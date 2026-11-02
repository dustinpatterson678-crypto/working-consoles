package com.dustin.wcore.client;

import com.dustin.wcore.ConsoleSettings;
import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.block.ConsoleBlockEntity;
import com.dustin.wcore.emu.CoreFiles;
import com.dustin.wcore.emu.DumpCheck;
import com.dustin.wcore.emu.EmuThread;
import com.dustin.wcore.emu.LibretroCore;
import com.dustin.wcore.retro.RetroClientHooks;
import com.dustin.wcore.retro.RetroConsoleBlock;
import com.dustin.wcore.retro.RetroConsoleBlockEntity;
import com.dustin.wcore.retro.RetroSystem;
import com.mojang.blaze3d.platform.NativeImage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RetroSessions {
   private static final Map<BlockPos, RetroSessions.Session> RUNNING = new HashMap<>();
   private static final Map<BlockPos, String> FAILED = new HashMap<>();
   private static final RetroPadInput INPUT = new RetroPadInput();
   private static BlockPos focus;
   private static final Map<BlockPos, RetroSessions.Check> CHECKS = new HashMap<>();
   private static final Map<BlockPos, Long> WAITING_SINCE = new HashMap<>();
   private static final Map<BlockPos, String> TOLD = new HashMap<>();

   private RetroSessions() {
   }

   public static void init() {
      RetroClientHooks.TICK = RetroSessions::consoleTick;
      RetroConsoleBlock.CLIENT_FOCUS = (pos, level) -> focus = pos.immutable();
      ConsoleSettings.onChange(() -> {
         for (RetroSessions.Session s : RUNNING.values()) {
            if (s.emu != null) {
               s.emu.setOptions(options(s.system));
            }
         }
      });
   }

   private static Map<String, String> options(RetroSystem sys) {
      Map<String, String> opts = new LinkedHashMap<>(sys.options);
      opts.putAll(ConsoleSettings.get().coreOverrides(sys.core));
      return opts;
   }

   private static void consoleTick(RetroConsoleBlockEntity be) {
      Minecraft mc = Minecraft.getInstance();
      Player p = mc.player;
      BlockPos pos = be.getBlockPos();
      boolean want = p != null
         && be.isOn()
         && be.isPowered()
         && be.system() != null
         && p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 4096.0;
      RetroSessions.Session s = RUNNING.get(pos);
      if (!want) {
         if (s != null) {
            stop(s);
         }

         WAITING_SINCE.remove(pos);
         TOLD.remove(pos);
         FAILED.remove(pos);
      } else {
         boolean can = !be.media().equals(FAILED.get(pos)) && problem(be, mc) == null;
         ScreenShare.Role role = ScreenShare.role(pos, can);
         if (role != ScreenShare.Role.HOST) {
            if (s != null) {
               stop(s);
            }

            if (!can && role == ScreenShare.Role.WAITING) {
               nobodyCanRun(be, mc);
            }
         } else {
            if (s != null && !s.media.equals(be.media())) {
               stop(s);
               s = null;
            }

            if (s == null) {
               if (be.media().equals(FAILED.get(pos))) {
                  return;
               }

               s = start(be, mc);
               if (s == null) {
                  FAILED.put(pos.immutable(), be.media());
                  return;
               }
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
            ConsoleStats.put(pos, new ConsoleStats.Entry(s.system.name, s.emu.stats(), s.system.core));
            pump(s);
         }
      }
   }

   public static void clientTick(Minecraft mc) {
      if (mc.level != null) {
         long now = System.nanoTime();

         for (RetroSessions.Session s : RUNNING.values().toArray(new RetroSessions.Session[0])) {
            if (now - s.lastSeen > 1500000000L) {
               stop(s);
            }
         }
      } else {
         for (RetroSessions.Session s : RUNNING.values().toArray(new RetroSessions.Session[0])) {
            stop(s);
         }

         FAILED.clear();
         focus = null;
      }
   }

   private static BlockPos inputTarget(Player p) {
      if (focus != null && RUNNING.containsKey(focus)) {
         return focus;
      }

      BlockPos best = null;
      double bestD = 256.0;

      for (BlockPos pos : RUNNING.keySet()) {
         double d = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
         if (d < bestD) {
            bestD = d;
            best = pos;
         }
      }

      return best;
   }

   private static String problem(RetroConsoleBlockEntity be, Minecraft mc) {
      BlockPos pos = be.getBlockPos();
      String key = be.media();
      RetroSessions.Check c = CHECKS.get(pos);
      long now = System.nanoTime();
      if (c != null && c.key().equals(key) && now - c.at() < 5000000000L) {
         return c.problem();
      }

      RetroSystem sys = be.system();
      String problem = null;
      RetroSystem.Game game = sys == null ? null : sys.game(be.gameFile());
      if (sys == null) {
         problem = "Unknown console";
      } else if (game == null) {
         problem = "Can't find " + be.gameFile() + " in " + sys.gamesDir();
      } else if (sys.needsBios() && sys.bios() == null) {
         problem = "No BIOS in " + sys.biosDir();
      } else {
         DumpCheck.Result check = DumpCheck.check(
            mc.getUser().getName(), mc.getUser().getProfileId(), sys.needsBios() ? sys.bios() : null, game.file(), sys.kind
         );
         if (!check.allowed()) {
            problem = "Won't boot: this looks downloaded, not dumped from your own " + sys.mediaWord + " (" + check.summary() + ")";
         }
      }

      CHECKS.put(pos.immutable(), new RetroSessions.Check(key, problem, now));
      return problem;
   }

   private static void nobodyCanRun(RetroConsoleBlockEntity be, Minecraft mc) {
      BlockPos pos = be.getBlockPos();
      long now = System.nanoTime();
      Long since = WAITING_SINCE.computeIfAbsent(pos.immutable(), k -> now);
      if (now - since >= 4000000000L && !ScreenShare.watching(pos) && !be.media().equals(TOLD.get(pos))) {
         TOLD.put(pos.immutable(), be.media());
         String why = problem(be, mc);
         if (why != null && be.system() != null) {
            say(
               mc,
               be.system(),
               why + (ScreenShare.active() ? " - and nobody nearby has it either. One player with the BIOS and game is enough." : ""),
               ChatFormatting.RED
            );
         }
      }
   }

   private static RetroSessions.Session start(RetroConsoleBlockEntity be, Minecraft mc) {
      RetroSystem sys = be.system();
      String file = be.gameFile();
      RetroSystem.Game game = sys.game(file);
      if (game == null) {
         say(mc, sys, "Can't find " + file + " in " + sys.gamesDir(), ChatFormatting.RED);
         return null;
      }

      Path core = CoreFiles.ensure(sys.core, msg -> mc.execute(() -> say(mc, sys, msg, ChatFormatting.GRAY)));
      if (core == null) {
         return null;
      }

      Path consoles = LibretroCore.consolesDir();
      Path system = consoles.resolve("system").resolve(sys.id);
      Path bios = null;
      // Copy every file from the console's bios/ folder into the core's system
      // directory. Some cores need several firmware files (DS, 32X, Intellivision,
      // Channel F) and some accept an optional BIOS for CD add-ons, so the old
      // "copy exactly one file" path was not enough.
      try {
         Files.createDirectories(system);
         Path biosDir = sys.biosDir();
         if (Files.isDirectory(biosDir)) {
            try (var files = Files.list(biosDir)) {
               for (Path p : files.filter(Files::isRegularFile).toList()) {
                  Files.copy(p, system.resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
               }
            }
         }
      } catch (Exception e) {
         say(mc, sys, "Couldn't copy the BIOS: " + e.getMessage(), ChatFormatting.RED);
         return null;
      }

      if (sys.needsBios()) {
         bios = sys.bios();
         if (bios == null) {
            be.triggerAnim("main", "shake");
            say(mc, sys, "No BIOS in " + sys.biosDir(), ChatFormatting.RED);
            return null;
         }
      }

      DumpCheck.Result check = DumpCheck.check(mc.getUser().getName(), mc.getUser().getProfileId(), bios, game.file(), sys.kind);
      if (!check.allowed()) {
         be.triggerAnim("main", "shake");
         say(mc, sys, "Won't boot: this looks downloaded, not dumped from your own " + sys.mediaWord + " (" + check.summary() + ")", ChatFormatting.RED);
         return null;
      }

      if (check.bypassed()) {
         say(mc, sys, "Developer profile: booting anyway (" + check.summary() + ")", ChatFormatting.YELLOW);
      }

      Object r = LibretroCore.open(
         core,
         game.file(),
         system,
         consoles.resolve("saves").resolve(sys.id),
         options(sys),
         consoles.resolve("logs").resolve(sys.id + "-core.log"),
         sys.latencyMs,
         check.pass()
      );
      if (r instanceof LibretroCore lr) {
         RetroSessions.Session s = new RetroSessions.Session(be.getBlockPos().immutable(), be.media(), sys);
         BlockPos at = s.pos;
         s.emu = new EmuThread(
            lr, sys.name, ConsoleSettings.get().soundInWorld ? McAudioSink.world(() -> soundPos(at), ConsoleSettings.get().hearingDistance) : null
         );
         s.emu.setAudioTap(ScreenShare.audioTap(s.pos));
         RUNNING.put(s.pos, s);
         TvScreens.setPump(s.pos, () -> pump(s));
         WorkingConsolesCore.LOGGER.info("{} started at {} with {}", new Object[]{sys.name, s.pos, game.title()});
         return s;
      } else {
         say(mc, sys, String.valueOf(r), ChatFormatting.RED);
         return null;
      }
   }

   private static void stop(RetroSessions.Session s) {
      RUNNING.remove(s.pos);
      if (s.emu != null) {
         s.emu.stop();
      }

      TvScreens.clear(s.pos);
      TvScreens.setPump(s.pos, null);
      ConsoleStats.remove(s.pos);
      if (s.texId != null) {
         Minecraft.getInstance().getTextureManager().release(s.texId);
      }
   }

   private static void pump(RetroSessions.Session s) {
      if (s.emu != null) {
         long now = System.nanoTime();
         if (now - s.lastPump >= 2000000L) {
            s.lastPump = now;
            Minecraft mc = Minecraft.getInstance();
            ControllerBindingHandler.drive(s.pos, s.emu);
            int[] frame = s.emu.takeFrame();
            if (frame != null) {
               int w = s.emu.width();
               int h = s.emu.height();
               if (w > 0 && h > 0) {
                  if (s.texture == null || w > s.texW || h > s.texH) {
                     if (s.texId != null) {
                        mc.getTextureManager().release(s.texId);
                     }

                     s.texW = Math.max(w, s.texW);
                     s.texH = Math.max(h, s.texH);
                     s.texId = WorkingConsolesCore.id("dynamic/screen_" + Long.toHexString(s.pos.asLong() & 281474976710655L));
                     s.texture = new DynamicTexture(() -> s.system.name + " picture", s.texW, s.texH, true);
                     mc.getTextureManager().register(s.texId, s.texture);
                  }

                  NativeImage img = s.texture.getPixels();
                  if (img != null) {
                     MemorySegment dst = MemorySegment.ofAddress(img.getPointer()).reinterpret((long)s.texW * s.texH * 4L);

                     for (int y = 0; y < h; y++) {
                        MemorySegment.copy(frame, y * w, dst, ValueLayout.JAVA_INT, (long)y * s.texW * 4L, w);
                     }

                     s.texture.upload();
                     TvScreens.show(s.pos, new TvScreens.Screen(s.texId, w, h, s.texW, s.texH, 0));
                     ScreenShare.sendFrame(s.pos, frame, w, h);
                  }
               }
            }
         }
      }
   }

   public static Vec3 soundPos(BlockPos console) {
      Level level = Minecraft.getInstance().level;
      BlockPos from = console;
      if (level != null && level.getBlockEntity(console) instanceof ConsoleBlockEntity be && be.tvPos() != null) {
         from = be.tvPos();
      }

      return new Vec3(from.getX() + 0.5, from.getY() + 0.9, from.getZ() + 0.5);
   }

   private static void say(Minecraft mc, RetroSystem sys, String msg, ChatFormatting color) {
      if (mc.player != null) {
         mc.player.sendSystemMessage(Component.literal("[" + sys.name + "] " + msg).withStyle(color));
      }
   }

   private record Check(String key, String problem, long at) {
   }

   private static final class Session {
      final BlockPos pos;
      final String media;
      final RetroSystem system;
      EmuThread emu;
      DynamicTexture texture;
      Identifier texId;
      int texW;
      int texH;
      long lastSeen;
      long lastPump;

      Session(BlockPos pos, String media, RetroSystem system) {
         this.pos = pos;
         this.media = media;
         this.system = system;
      }
   }
}
