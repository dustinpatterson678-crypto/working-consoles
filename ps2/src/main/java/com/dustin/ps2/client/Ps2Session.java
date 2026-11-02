package com.dustin.ps2.client;

import com.dustin.ps2.Ps2Files;
import com.dustin.ps2.Ps2Mod;
import com.dustin.ps2.block.Ps2BlockEntity;
import com.dustin.wcore.ConsoleSettings;
import com.dustin.wcore.block.PowerGrade;
import com.dustin.wcore.block.TvBlockEntity;
import com.dustin.wcore.client.ConsoleStats;
import com.dustin.wcore.client.ControllerBindingHandler;
import com.dustin.wcore.client.McAudioSink;
import com.dustin.wcore.client.RetroSessions;
import com.dustin.wcore.client.TvScreens;
import com.dustin.wcore.client.ConsoleStats.Entry;
import com.dustin.wcore.client.TvScreens.Screen;
import com.dustin.wcore.emu.CoreFiles;
import com.dustin.wcore.emu.DumpCheck;
import com.dustin.wcore.emu.EmuThread;
import com.dustin.wcore.emu.LibretroCore;
import com.dustin.wcore.emu.DumpCheck.Kind;
import com.dustin.wcore.emu.DumpCheck.Result;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class Ps2Session {
   private static final Identifier TEX = Ps2Mod.id("dynamic/tv_picture");
   private static BlockPos focus;
   private static BlockPos running;
   private static String runningMedia;
   private static EmuThread emu;
   private static DynamicTexture texture;
   private static int texW;
   private static int texH;
   private static long lastPump;
   private static String lastError;
   private static final Ps2Input INPUT = new Ps2Input();
   private static boolean loggedInput;
   private static String biosName;
   private static PowerGrade appliedGrade = PowerGrade.CLEAN;

   private static Map<String, String> options(PowerGrade g) {
      Map<String, String> opts = new LinkedHashMap<>(ConsoleSettings.get().ps2BootOptions());
      opts.put("pcsx2_renderer", "Software (SW)");
      if (biosName != null) {
         opts.put("pcsx2_bios", biosName);
      }

      int threads;
      try {
         threads = Integer.parseInt(opts.getOrDefault("pcsx2_sw_renderer_threads", "2"));
      } catch (NumberFormatException e) {
         threads = 2;
      }

      switch (g) {
         case OFF:
         case UNSTABLE:
         case LOW:
            threads = 0;
            break;
         case NORMAL:
            threads = Math.max(1, threads / 2);
            break;
         case OVERCLOCK:
            threads = Math.min(11, threads + 2);
            if ("100% (Normal Speed)".equals(opts.getOrDefault("pcsx2_ee_cycle_rate", "100% (Normal Speed)"))) {
               opts.put("pcsx2_ee_cycle_rate", "130% (Overclock)");
            }
      }

      opts.put("pcsx2_sw_renderer_threads", String.valueOf(threads));
      return opts;
   }

   private static String boost(Map<String, String> opts) {
      return opts.get("pcsx2_sw_renderer_threads") + " render threads, CPU " + opts.getOrDefault("pcsx2_ee_cycle_rate", "100%").replaceAll(" \\(.*\\)", "");
   }

   private Ps2Session() {
   }

   public static void focus(BlockPos pos) {
      focus = pos.immutable();
   }

   public static boolean isRunning() {
      return emu != null;
   }

   public static boolean isRunningAt(BlockPos pos) {
      return emu != null && pos.equals(running);
   }

   public static void clientTick(Minecraft mc) {
      Player player = mc.player;
      if (player == null || mc.level == null) {
         stop();
         focus = null;
      } else if (focus == null) {
         stop();
      } else if (mc.level.getBlockEntity(focus) instanceof Ps2BlockEntity console && console.isOn() && console.isPowered()) {
         String want = console.media();
         if (emu == null || !focus.equals(running) || !want.equals(runningMedia)) {
            String key = focus + "|" + want;
            if (!key.equals(lastError)) {
               start(console, want, mc);
            }

            if (emu == null) {
               return;
            }
         }

         double dist = Math.sqrt(player.distanceToSqr(focus.getX() + 0.5, focus.getY() + 0.5, focus.getZ() + 0.5));
         emu.setPaused(mc.isPaused() || dist > 64.0);
         BlockPos from = console.tvPos() != null ? console.tvPos() : focus;
         double soundDist = Math.sqrt(player.distanceToSqr(from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5));
         ConsoleSettings cfg = ConsoleSettings.get();
         emu.setVolume(
            cfg.soundInWorld
               ? cfg.volumeFactor()
               : (float)(cfg.volumeFactor() * Math.max(0.0, Math.min(1.0, 1.15 - soundDist / Math.max(4.0, cfg.hearingDistance))))
         );
         PowerGrade g = console.grade();
         if (g != appliedGrade) {
            appliedGrade = g;
            emu.setOptions(options(g));
         }

         ConsoleStats.put(running, new Entry("PS2", emu.stats(), boost(options(g))));
         pump();
      } else {
         if (emu != null) {
            stop();
         }

         lastError = null;
      }
   }

   private static void start(Ps2BlockEntity console, String media, Minecraft mc) {
      stop();
      String error = tryStart(console, media);
      if (error != null) {
         lastError = focus + "|" + media;
         if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("[PS2] " + error).withStyle(ChatFormatting.RED));
         }
      } else {
         lastError = null;
         tvHint(console, mc);
      }
   }

   private static String tryStart(Ps2BlockEntity console, String media) {
      Path bios = Ps2Files.bios();
      if (bios == null) {
         return "No PS2 BIOS in " + Ps2Files.biosDir();
      }

      boolean win = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
      Path consoles = LibretroCore.consolesDir();
      Path core = CoreFiles.ensure("pcsx2", msg -> Minecraft.getInstance().execute(() -> {
         if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("[PS2] " + msg).withStyle(ChatFormatting.GRAY));
         }
      }));
      if (core == null) {
         lastError = null;
         return "The PS2 emulator core is downloading - turn the console on again in a moment.";
      }

      Path system = consoles.resolve("system");
      Path saves = consoles.resolve("saves").resolve("ps2");

      try {
         Path biosDir = system.resolve("pcsx2").resolve("bios");
         Files.createDirectories(biosDir);
         Path target = biosDir.resolve(bios.getFileName());
         if (!Files.exists(target) || Files.size(target) != Files.size(bios)) {
            Files.copy(bios, target, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException e) {
         return "Couldn't set up the BIOS: " + e.getMessage();
      }

      Path game = null;
      if (!media.isEmpty()) {
         game = Ps2Files.discDir().resolve(media);
         if (!Files.isRegularFile(game)) {
            return "The disc image " + media + " isn't in ps2/isos anymore";
         }
      }

      String user = Minecraft.getInstance().getUser().getName();
      Result check = DumpCheck.check(user, Minecraft.getInstance().getUser().getProfileId(), bios, game, Kind.PS2);
      if (!check.allowed()) {
         console.triggerAnim("main", "shake");
         return "Won't boot: this looks downloaded, not dumped from your own console (" + check.summary() + ")";
      }

      if (check.bypassed() && Minecraft.getInstance().player != null) {
         Minecraft.getInstance()
            .player
            .sendSystemMessage(Component.literal("[PS2] Developer profile: booting anyway (" + check.summary() + ")").withStyle(ChatFormatting.YELLOW));
      }

      biosName = bios.getFileName().toString();
      appliedGrade = console.grade();
      Map<String, String> opts = options(appliedGrade);
      Object result = LibretroCore.open(core, game, system, saves, opts, consoles.resolve("logs").resolve("ps2-core.log"), check.pass());
      if (result instanceof LibretroCore lr) {
         BlockPos at = focus;
         emu = new EmuThread(
            lr, "PS2", ConsoleSettings.get().soundInWorld ? McAudioSink.world(() -> RetroSessions.soundPos(at), ConsoleSettings.get().hearingDistance) : null
         );
         running = focus;
         TvScreens.setPump(running, Ps2Session::pump);
         runningMedia = media;
         Ps2Mod.LOGGER.info("PS2 started at {} with {}", running, media.isEmpty() ? "no disc (system menu)" : media);
         return null;
      } else {
         return String.valueOf(result);
      }
   }

   private static void tvHint(Ps2BlockEntity console, Minecraft mc) {
      if (mc.player != null && mc.level != null) {
         String msg = null;
         if (console.tvPos() == null) {
            msg = "No TV is cabled to this console: use a cable on the console, then on the TV.";
         } else if (mc.level.getBlockEntity(console.tvPos()) instanceof TvBlockEntity tv) {
            if (!console.getBlockPos().equals(tv.consolePos())) {
               msg = "That TV is cabled to a different console.";
            } else if (!tv.isPowered()) {
               msg = "The TV has no power: cable the TV to a live wall outlet too.";
            }
         } else {
            msg = "The TV this console was cabled to is gone.";
         }

         if (msg != null) {
            mc.player.sendSystemMessage(Component.literal("[PS2] " + msg).withStyle(ChatFormatting.YELLOW));
         }
      }
   }

   private static void pump() {
      if (emu != null) {
         long now = System.nanoTime();
         if (now - lastPump >= 2000000L) {
            lastPump = now;
            Minecraft mc = Minecraft.getInstance();
            ControllerBindingHandler.drive(running, emu);
            int[] frame = emu.takeFrame();
            if (frame != null) {
               int w = emu.width();
               int h = emu.height();
               if (w > 0 && h > 0) {
                  if (texture == null || w != texW || h != texH) {
                     if (texture != null) {
                        mc.getTextureManager().release(TEX);
                     }

                     texture = new DynamicTexture(() -> "ps2 picture", w, h, true);
                     mc.getTextureManager().register(TEX, texture);
                     texW = w;
                     texH = h;
                  }

                  NativeImage img = texture.getPixels();
                  if (img != null) {
                     MemorySegment dst = MemorySegment.ofAddress(img.getPointer()).reinterpret((long)w * h * 4L);
                     MemorySegment.copy(frame, 0, dst, ValueLayout.JAVA_INT, 0L, w * h);
                     texture.upload();
                     TvScreens.show(running, new Screen(TEX, w, h, w, h, 0));
                  }
               }
            }
         }
      }
   }

   public static void stop() {
      if (emu != null) {
         emu.stop();
         emu = null;
      }

      if (running != null) {
         ConsoleStats.remove(running);
         TvScreens.clear(running);
         TvScreens.setPump(running, null);
      }

      running = null;
      runningMedia = null;
   }

   static {
      ConsoleSettings.onChange(() -> {
         EmuThread e = emu;
         if (e != null && biosName != null) {
            e.setOptions(options(appliedGrade));
         }
      });
   }
}
