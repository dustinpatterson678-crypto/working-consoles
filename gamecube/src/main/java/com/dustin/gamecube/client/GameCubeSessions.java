package com.dustin.gamecube.client;

import com.dustin.gamecube.GameCubeFiles;
import com.dustin.gamecube.GameCubeMod;
import com.dustin.gamecube.block.GameCubeBlockEntity;
import com.dustin.wcore.ConsoleSettings;
import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.client.TvScreens;
import com.dustin.wcore.emu.CoreFiles;
import com.dustin.wcore.emu.DumpCheck;
import com.dustin.wcore.emu.EmuThread;
import com.dustin.wcore.emu.LibretroCore;
import com.mojang.blaze3d.platform.NativeImage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class GameCubeSessions {
   private static final Map<BlockPos, GameCubeSessions.Session> RUNNING = new HashMap<>();
   private static final Map<BlockPos, String> FAILED = new HashMap<>();
   private static final Set<BlockPos> STOPPING = new HashSet<>();
   private static final Map<BlockPos, GameCubeSessions.Check> CHECKS = new HashMap<>();
   private static final Map<BlockPos, Long> WAITING_SINCE = new HashMap<>();
   private static final Map<BlockPos, String> TOLD = new HashMap<>();
   private static final Map<BlockPos, String> LAST_STATE = new HashMap<>();
   private static final Map<BlockPos, Long> NOT_READY_SINCE = new HashMap<>();
   private static final Map<BlockPos, String> HINTED = new HashMap<>();
   /** Consoles that are waiting for the emulator core to finish downloading: retry time + number of retries so far. */
   private static final Map<BlockPos, Long> RETRY_AT = new HashMap<>();
   private static final Map<BlockPos, Integer> RETRIES = new HashMap<>();
   private static final Set<String> CORE_MSGS = new HashSet<>();
   /**
    * Games that were switched off. The Dolphin emulator can't be unloaded safely (unloading it crashed Minecraft a few
    * seconds later), so a switched-off game is frozen and kept in memory instead, and carries on if the same disc is
    * switched on again. Keyed by console position + disc.
    */
   private static final Map<String, GameCubeSessions.Session> PARKED = new HashMap<>();
   private static int sessionCounter;

   private GameCubeSessions() {
   }

   /** Returns true if a GameCube session is currently running at the given position. */
   public static boolean isRunning(BlockPos pos) {
      return RUNNING.containsKey(pos);
   }

   /** Called when a player right-clicks the console with a wireless controller. */
   public static void focus(BlockPos pos) {
      // Could be used to track which console the controller is focused on
   }

   public static void consoleTick(Level level, BlockPos pos, BlockState state, GameCubeBlockEntity be) {
      Minecraft mc = Minecraft.getInstance();
      Player p = mc.player;

      boolean want = p != null
         && be.isOn()
         && be.isPowered()
         && be.tvPos() != null
         && !be.media().isEmpty()
         && p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 4096.0;

      GameCubeSessions.Session s = RUNNING.get(pos);
      logState(pos, be, p);
      if (want) {
         NOT_READY_SINCE.remove(pos);
         HINTED.remove(pos);
      } else {
         hintWhyNot(pos, be, mc, p);
      }

      if (!want) {
         if (s != null) {
            stop(s);
         }
         WAITING_SINCE.remove(pos);
         TOLD.remove(pos);
         FAILED.remove(pos);
         RETRY_AT.remove(pos);
         RETRIES.remove(pos);
      } else {
         String problem = problem(be, mc);
         boolean can = problem == null && !be.media().equals(FAILED.get(pos));
         com.dustin.wcore.client.ScreenShare.Role role = com.dustin.wcore.client.ScreenShare.role(pos, can);

         if (role != com.dustin.wcore.client.ScreenShare.Role.HOST) {
            if (s != null) {
               stop(s);
            }
            if (!can && role == com.dustin.wcore.client.ScreenShare.Role.WAITING) {
               nobodyCanRun(be, mc);
            }
         } else {
            if (s != null && !s.media.equals(be.media())) {
               stop(s);
               s = null;
            }

            if (s == null) {
               if (STOPPING.contains(pos.immutable())) {
                  return;
               }
               if (be.media().equals(FAILED.get(pos))) {
                  return;
               }
               Long retryAt = RETRY_AT.get(pos);
               if (retryAt != null && System.nanoTime() < retryAt) {
                  return;
               }
               s = start(be, mc);
               if (s == null) {
                  if (waitingForCore(pos)) {
                     return;
                  }
                  FAILED.put(pos.immutable(), be.media());
                  return;
               }
               RETRY_AT.remove(pos);
               RETRIES.remove(pos);
            }

            s.lastSeen = System.nanoTime();
            ConsoleSettings cfg = ConsoleSettings.get();
            if (cfg.soundInWorld) {
               s.emu.setVolume(cfg.volumeFactor());
            } else {
               BlockPos from = be.tvPos() != null ? be.tvPos() : pos;
               double dist = Math.sqrt(p.distanceToSqr(from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5));
               s.emu.setVolume((float)(cfg.volumeFactor() * Math.max(0.0, Math.min(1.0, 1.15 - dist / Math.max(4.0, cfg.hearingDistance)))));
            }

            s.emu.setPaused(mc.isPaused());
            pump(s);
         }
      }
   }

   /**
    * True while the Dolphin core is still being downloaded: the console then just tries again every 2 seconds
    * (up to ~2 minutes) instead of being marked as failed, so the game boots by itself as soon as the core is ready.
    */
   private static boolean waitingForCore(BlockPos pos) {
      if (Files.isRegularFile(CoreFiles.path("dolphin"))) {
         return false;
      }
      int n = RETRIES.merge(pos.immutable(), 1, Integer::sum);
      if (n > 60) {
         return false;
      }
      RETRY_AT.put(pos.immutable(), System.nanoTime() + 2000000000L);
      return true;
   }

   /** Logs the console's state whenever it changes, so a console that "does nothing" can be diagnosed from the log. */
   private static void logState(BlockPos pos, GameCubeBlockEntity be, Player p) {
      String st = "on=" + be.isOn() + " powered=" + be.isPowered() + " tvCabled=" + (be.tvPos() != null)
         + " outletCabled=" + (be.outletPos() != null) + " disc=" + (be.media().isEmpty() ? "none" : be.media()) + " player=" + (p != null);
      if (!st.equals(LAST_STATE.get(pos))) {
         LAST_STATE.put(pos.immutable(), st);
         GameCubeMod.LOGGER.info("[GameCube] console at {}: {}", pos.toShortString(), st);
      }
   }

   /** A disc is in but the console can't boot it: after 3 seconds, tell the player what is missing (once per reason). */
   private static void hintWhyNot(BlockPos pos, GameCubeBlockEntity be, Minecraft mc, Player p) {
      if (p == null || be.media().isEmpty()) {
         NOT_READY_SINCE.remove(pos);
         return;
      }
      String why = null;
      if (!be.isPowered()) {
         why = "The console has no power: cable it to a wall outlet and give the outlet's block a redstone signal.";
      } else if (be.tvPos() == null) {
         why = "No TV is cabled to the console: use a cable on the console, then on the TV.";
      } else if (!be.isOn()) {
         why = "The console is off: right-click it to power it on.";
      }
      if (why == null) {
         return;
      }
      long now = System.nanoTime();
      Long since = NOT_READY_SINCE.computeIfAbsent(pos.immutable(), k -> now);
      if (now - since >= 3000000000L && !why.equals(HINTED.get(pos))) {
         HINTED.put(pos.immutable(), why);
         say(mc, why, ChatFormatting.GOLD);
      }
   }

   public static void clientTick(Minecraft mc) {
      if (mc.level != null) {
         long now = System.nanoTime();
         boolean paused = mc.isPaused();
         for (GameCubeSessions.Session s : RUNNING.values().toArray(new GameCubeSessions.Session[0])) {
            if (STOPPING.contains(s.pos.immutable())) {
               continue;
            }
            if (paused) {
               // The pause menu stops block ticking, so the console can't report in. That is not a reason to shut the
               // game down (and certainly not while the player is opening settings): just freeze it and keep it alive.
               s.lastSeen = now;
               s.emu.setPaused(true);
               continue;
            }
            if (now - s.lastSeen > 1500000000L) {
               stop(s);
            }
         }
      } else {
         for (GameCubeSessions.Session s : RUNNING.values().toArray(new GameCubeSessions.Session[0])) {
            stop(s);
         }
         FAILED.clear();
         WAITING_SINCE.clear();
         TOLD.clear();
         CHECKS.clear();
         LAST_STATE.clear();
         NOT_READY_SINCE.clear();
         HINTED.clear();
         RETRY_AT.clear();
         RETRIES.clear();
         CORE_MSGS.clear();
      }
   }

   private static String problem(GameCubeBlockEntity be, Minecraft mc) {
      BlockPos pos = be.getBlockPos();
      String key = be.media();
      GameCubeSessions.Check c = CHECKS.get(pos);
      long now = System.nanoTime();
      if (c != null && c.key().equals(key) && now - c.at() < 5000000000L) {
         return c.problem();
      }

      String problem = null;
      GameCubeFiles.Disc game = GameCubeFiles.disc(be.media());
      if (game == null) {
         problem = "Can't find " + be.media() + " in " + GameCubeFiles.discDir();
      } else {
         DumpCheck.Result check = DumpCheck.check(
            mc.getUser().getName(), mc.getUser().getProfileId(), null, game.file(), DumpCheck.Kind.GAMECUBE
         );
         if (!check.allowed()) {
            problem = "Won't boot: this looks downloaded, not dumped from your own disc (" + check.summary() + ")";
         }
      }

      CHECKS.put(pos.immutable(), new GameCubeSessions.Check(key, problem, now));
      return problem;
   }

   private static void nobodyCanRun(GameCubeBlockEntity be, Minecraft mc) {
      BlockPos pos = be.getBlockPos();
      long now = System.nanoTime();
      Long since = WAITING_SINCE.computeIfAbsent(pos.immutable(), k -> now);
      if (now - since >= 4000000000L && !com.dustin.wcore.client.ScreenShare.watching(pos) && !be.media().equals(TOLD.get(pos))) {
         TOLD.put(pos.immutable(), be.media());
         String why = problem(be, mc);
         if (why != null) {
            say(
               mc,
               why + (com.dustin.wcore.client.ScreenShare.active() ? " - and nobody nearby has it either. One player with the game is enough." : ""),
               ChatFormatting.RED
            );
         }
      }
   }

   private static GameCubeSessions.Session start(GameCubeBlockEntity be, Minecraft mc) {
      // Never start a new session while a previous shutdown is still draining.
      if (STOPPING.contains(be.getBlockPos().immutable())) {
         return null;
      }

      GameCubeSessions.Session parked = PARKED.remove(parkKey(be.getBlockPos(), be.media()));
      if (parked != null && parked.emu != null && parked.emu.isAlive()) {
         parked.shownSerial = -1;
         parked.emu.setPaused(false);
         RUNNING.put(parked.pos, parked);
         TvScreens.setPump(parked.pos, () -> pump(parked));
         GameCubeMod.LOGGER.info("[GameCube] resuming {} at {}", parked.media, parked.pos.toShortString());
         return parked;
      }

      GameCubeFiles.Disc game = GameCubeFiles.disc(be.media());
      if (game == null) {
         say(mc, "Can't find " + be.media() + " in " + GameCubeFiles.discDir(), ChatFormatting.RED);
         return null;
      }

      GameCubeMod.LOGGER.info("[GameCube] starting {} at {}", game.title(), be.getBlockPos().toShortString());
      Path core = CoreFiles.ensure("dolphin", msg -> {
         String shown = msg.startsWith("Emulator core") ? "The GameCube emulator core is ready - starting your game." : msg;
         if (CORE_MSGS.add(shown)) {
            mc.execute(() -> say(mc, shown, ChatFormatting.GRAY));
         }
      });
      if (core == null) {
         return null;
      }

      Path consoles = LibretroCore.consolesDir();
      Path system = consoles.resolve("system").resolve("gamecube");
      Path saves = consoles.resolve("saves").resolve("gamecube");
      Path log = consoles.resolve("logs").resolve("gamecube-core.log");

      DumpCheck.Result check = DumpCheck.check(mc.getUser().getName(), mc.getUser().getProfileId(), null, game.file(), DumpCheck.Kind.GAMECUBE);
      if (!check.allowed()) {
         be.triggerAnim("main", "shake");
         say(mc, "Won't boot: this looks downloaded, not dumped from your own disc (" + check.summary() + ")", ChatFormatting.RED);
         return null;
      }

      if (check.bypassed()) {
         say(mc, "Developer profile: booting anyway (" + check.summary() + ")", ChatFormatting.YELLOW);
      }

      BlockPos pos = be.getBlockPos();
      int number = ++sessionCounter;
      String texName = "dynamic/gamecube_" + Long.toHexString(pos.asLong() & 281474976710655L) + "_" + number;
      Identifier texId = WorkingConsolesCore.id(texName);
      DynamicTexture texture = new DynamicTexture(() -> "GameCube " + game.title(), 640, 528, true);
      mc.getTextureManager().register(texId, texture);
      Identifier viewId = WorkingConsolesCore.id(texName + "_view");
      DynamicTexture view = new DynamicTexture(() -> "GameCube picture", 640, 528, true);
      mc.getTextureManager().register(viewId, view);
      int glTexture = glId(texture);
      if (glTexture == 0) {
         mc.getTextureManager().release(texId);
         mc.getTextureManager().release(viewId);
         say(mc, "Minecraft is not using an OpenGL renderer; GameCube requires the OpenGL backend.", ChatFormatting.RED);
         return null;
      }

      Object r = LibretroCore.open(
         core,
         game.file(),
         system,
         saves,
         options(),
         log,
         40,
         check.pass(),
         glTexture,
         640,
         528
      );
      if (r instanceof LibretroCore lr) {
         GameCubeSessions.Session s = new GameCubeSessions.Session(pos.immutable(), be.media(), texId, texture);
         s.viewId = viewId;
         s.view = view;
         s.srcGl = glTexture;
         s.viewGl = glId(view);
         s.copy = new FrameCopy();
         s.emu = new EmuThread(lr, "GameCube " + game.title(), null);
         RUNNING.put(s.pos, s);
         TvScreens.setPump(s.pos, () -> pump(s));
         WorkingConsolesCore.LOGGER.info("GameCube started at {} with {}", s.pos, game.title());
         return s;
      } else {
         mc.getTextureManager().release(texId);
         mc.getTextureManager().release(viewId);
         String why = String.valueOf(r);
         if (why.contains("max 8")) {
            why = "The GameCube emulator only starts a limited number of games per Minecraft launch (it is never unloaded, to avoid crashes). Restart Minecraft to start a new game.";
         }
         say(mc, why, ChatFormatting.RED);
         return null;
      }
   }

   private static Map<String, String> options() {
      Map<String, String> opts = new LinkedHashMap<>();
      opts.put("dolphin_renderer", "Hardware");
      opts.put("dolphin_efb_scale", "1");
      opts.put("dolphin_cpu_core", "JIT64");
      opts.put("dolphin_main_cpu_thread", "disabled");   // dual core made objects flicker or vanish (horses in Twilight Princess)
      opts.put("dolphin_fastmem", "enabled");
      opts.put("dolphin_fastmem_arena", "enabled");
      opts.put("dolphin_dsp_hle", "enabled");
      opts.put("dolphin_skip_gc_bios", "enabled");
      opts.put("dolphin_language", "en");
      opts.putAll(ConsoleSettings.get().coreOverrides("dolphin"));
      return opts;
   }

   private static String parkKey(BlockPos pos, String media) {
      return pos.asLong() + "|" + media;
   }

   /** "Stops" a game by freezing it: see PARKED. The TV goes dark and the sound is muted. */
   private static void stop(GameCubeSessions.Session s) {
      if (RUNNING.get(s.pos) != s) {
         return;
      }
      RUNNING.remove(s.pos);
      GameCubeMod.LOGGER.info("[GameCube] switching off {} at {} (paused={})", s.media, s.pos.toShortString(), Minecraft.getInstance().isPaused());
      if (s.emu != null) {
         s.emu.setVolume(0.0F);
         s.emu.setPaused(true);
      }

      TvScreens.clear(s.pos);
      TvScreens.setPump(s.pos, null);
      PARKED.put(parkKey(s.pos, s.media), s);
   }

   private static void pump(GameCubeSessions.Session s) {
      if (s.emu != null) {
         long now = System.nanoTime();
         // 2 ms like RetroSessions: a full 16.7 ms gate skipped every other render frame (30 Hz picture and input)
         if (now - s.lastPump >= 2000000L) {
            s.lastPump = now;
            Minecraft mc = Minecraft.getInstance();
            com.dustin.wcore.client.ControllerBindingHandler.drive(s.pos, s.emu);

            if (s.emu.directVideo()) {
               int serial = s.emu.directSerial();
               if (serial > 0 && serial != s.shownSerial) {
                  s.shownSerial = serial;
                  Identifier shown = s.texId;
                  if (s.copy != null && (s.copy.copy(s.srcGl, s.viewGl, 640, 528) || s.copy.hasPicture())) {
                     // a failed copy keeps the last whole picture: the emulator's own texture can be half drawn (flicker)
                     shown = s.viewId;
                  } else {
                     TvScreens.markFlipped(s.texId);
                  }
                  // the copy flips all 528 rows, so the picture always fills the whole texture
                  TvScreens.show(s.pos, new TvScreens.Screen(shown, 640, 528, 640, 528, serial));
               }
            } else {
               int[] frame = s.emu.takeFrame();
               if (frame != null) {
                  int w = s.emu.width();
                  int h = s.emu.height();
                  if (w > 0 && h > 0) {
                     if (s.texture == null || w > s.texW || h > s.texH) {
                        if (s.texId != null) {
                           Minecraft mcInstance = Minecraft.getInstance();
                           mcInstance.execute(() -> mcInstance.getTextureManager().release(s.texId));
                        }
                        s.texW = Math.max(w, s.texW);
                        s.texH = Math.max(h, s.texH);
                        Identifier newTexId = WorkingConsolesCore.id("dynamic/gamecube_" + Long.toHexString(s.pos.asLong() & 281474976710655L) + "_cpu");
                        DynamicTexture newTexture = new DynamicTexture(() -> "GameCube picture", s.texW, s.texH, true);
                        mc.getTextureManager().register(newTexId, newTexture);
                        s.texId = newTexId;
                        s.texture = newTexture;
                     }
                     NativeImage img = s.texture.getPixels();
                     if (img != null) {
                        MemorySegment dst = MemorySegment.ofAddress(img.getPointer()).reinterpret((long)s.texW * s.texH * 4L);
                        for (int y = 0; y < h; y++) {
                           MemorySegment.copy(frame, y * w, dst, ValueLayout.JAVA_INT, (long)y * s.texW * 4L, w);
                        }
                        s.texture.upload();
                        TvScreens.show(s.pos, new TvScreens.Screen(s.texId, w, h, s.texW, s.texH, 0));
                        com.dustin.wcore.client.ScreenShare.sendFrame(s.pos, frame, w, h);
                     }
                  }
               }
            }
         }
      }
   }

   private static int glId(DynamicTexture texture) {
      Object gpu = texture.getTexture();
      if (gpu == null) {
         return 0;
      }
      try {
         Method method = gpu.getClass().getMethod("glId");
         return (int)method.invoke(gpu);
      } catch (ReflectiveOperationException | RuntimeException e) {
         return 0;
      }
   }

   private static void say(Minecraft mc, String msg, ChatFormatting color) {
      WorkingConsolesCore.LOGGER.info("GameCube: {}", msg);
      if (mc.player != null) {
         mc.player.sendSystemMessage(Component.literal("[GameCube] " + msg).withStyle(color));
      }
   }

   private static record Check(String key, String problem, long at) {
   }

   private static final class Session {
      final BlockPos pos;
      final String media;
      Identifier texId;
      DynamicTexture texture;
      Identifier viewId;
      DynamicTexture view;
      int srcGl;
      int viewGl;
      FrameCopy copy;
      EmuThread emu;
      long lastSeen;
      long lastPump = 0;
      int shownSerial = -1;
      int texW = 640;
      int texH = 528;

      Session(BlockPos pos, String media, Identifier texId, DynamicTexture texture) {
         this.pos = pos;
         this.media = media;
         this.texId = texId;
         this.texture = texture;
      }
   }
}