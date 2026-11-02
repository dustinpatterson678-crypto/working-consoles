package com.dustin.wcore.client;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.block.TvBlockEntity;
import com.dustin.wcore.emu.CoreFiles;
import com.dustin.wcore.emu.DumpCheck;
import com.dustin.wcore.emu.EmuThread;
import com.dustin.wcore.emu.LibretroCore;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Developer-only display proof for the Dolphin libretro core.
 *
 * <p>This is deliberately not a GameCube block and it does not add a new
 * console system. It borrows the nearest powered, unconnected TV, creates one
 * 640x528 texture, and points the proven GX demo at that texture. The emulator
 * writes into the texture on its own thread; the render thread only updates
 * the TV's screen record when the shim's frame serial changes.
 *
 * <p>Use {@code /wcore dolphin-test} to start and {@code /wcore dolphin-stop}
 * to stop. The normal PS2 and retro-console paths do not use this class.
 */
public final class DolphinTest {
   private static final int WIDTH = 640;
   private static final int HEIGHT = 528;
   private static final int SEARCH_RADIUS = 64;
   private static Session session;

   private DolphinTest() {
   }

   public static void register() {
      ClientCommandRegistrationCallback.EVENT
         .register(
            (dispatcher, context) -> dispatcher.register(
               ClientCommands.literal("wcore")
                  .then(ClientCommands.literal("dolphin-test").executes(ctx -> {
                     start();
                     return 1;
                  }))
                  .then(ClientCommands.literal("dolphin-stop").executes(ctx -> {
                     stop();
                     return 1;
                  }))
            )
         );
      ClientTickEvents.END_CLIENT_TICK.register(DolphinTest::tick);
   }

   private static void start() {
      Minecraft mc = Minecraft.getInstance();
      if (session != null) {
         stop();
      }

      if (mc.level == null || mc.player == null) {
         say("Join a world first.", ChatFormatting.RED);
         return;
      }

      TvBlockEntity tv = nearestFreeTv(mc);
      if (tv == null) {
         say("No powered, unconnected TV within " + SEARCH_RADIUS + " blocks.", ChatFormatting.RED);
         return;
      }

      Path demo = demoFile();
      if (demo == null) {
         say("The GX demo is missing. Expected consoles/test/gx_demo.dol.", ChatFormatting.RED);
         return;
      }

      Path core = coreFile();
      if (core == null) {
         say("The Dolphin core is missing. Put dolphin_libretro.dll in " + CoreFiles.path("dolphin"), ChatFormatting.RED);
         return;
      }

      DumpCheck.Result check = DumpCheck.check(
         mc.getUser().getName(), mc.getUser().getProfileId(), null, demo, DumpCheck.Kind.GAMECUBE
      );
      if (!check.allowed()) {
         say("Won't boot the GX demo: " + check.summary(), ChatFormatting.RED);
         return;
      }

      Identifier texId = WorkingConsolesCore.id("dynamic/dolphin_test");
      DynamicTexture texture = new DynamicTexture(() -> "Dolphin GX demo", WIDTH, HEIGHT, true);
      mc.getTextureManager().register(texId, texture);
      int glTexture = glId(texture);
      if (glTexture == 0) {
         mc.getTextureManager().release(texId);
         say("Minecraft is not using an OpenGL renderer, so the direct texture path is unavailable.", ChatFormatting.RED);
         return;
      }

      Path consoles = LibretroCore.consolesDir();
      Path system = consoles.resolve("system").resolve("gamecube");
      Path saves = consoles.resolve("saves").resolve("gamecube");
      Path log = consoles.resolve("logs").resolve("gamecube-test-core.log");

      Map<String, String> options = new LinkedHashMap<>();
      options.put("dolphin_renderer", "Hardware");
      options.put("dolphin_efb_scale", "1");
      options.put("dolphin_cpu_core", "JIT64");
      options.put("dolphin_main_cpu_thread", "enabled");
      options.put("dolphin_fastmem", "enabled");
      options.put("dolphin_fastmem_arena", "enabled");
      options.put("dolphin_dsp_hle", "enabled");
      options.put("dolphin_skip_gc_bios", "enabled");

      Object result = LibretroCore.open(
         core,
         demo,
         system,
         saves,
         options,
         log,
         40,
         check.pass(),
         glTexture,
         WIDTH,
         HEIGHT
      );
      if (!(result instanceof LibretroCore libretro)) {
         mc.getTextureManager().release(texId);
         say(String.valueOf(result), ChatFormatting.RED);
         return;
      }

      EmuThread emu = new EmuThread(libretro, "GameCube test", null);
      BlockPos tvPos = tv.getBlockPos().immutable();
      Session s = new Session(tvPos, tv.consolePos(), texId, emu);
      session = s;
      tv.plugConsole(tvPos);
      TvScreens.setPump(tvPos, DolphinTest::pump);
      say(
         "GX demo started on the TV at " + tvPos.toShortString() + " (texture " + glTexture + ").",
         ChatFormatting.GREEN
      );
   }

   private static void stop() {
      Session s = session;
      if (s == null) {
         say("The Dolphin test is not running.", ChatFormatting.GRAY);
         return;
      }

      session = null;
      TvScreens.clear(s.tvPos);
      TvScreens.setPump(s.tvPos, null);
      if (s.emu != null) {
         s.emu.stop();
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.level.getBlockEntity(s.tvPos) instanceof TvBlockEntity tv) {
         if (s.tvPos.equals(tv.consolePos())) {
            tv.plugConsole(s.originalConsole);
         }
      }

      if (s.emu != null && s.emu.isAlive()) {
         WorkingConsolesCore.LOGGER.warn("Dolphin test thread did not stop; keeping its texture alive to avoid a use-after-free");
      } else {
         mc.getTextureManager().release(s.texId);
      }

      say("GX demo stopped.", ChatFormatting.GRAY);
   }

   /** Safe version of stop for external callers (runs on client thread). */
   public static void safeStop() {
      Minecraft mc = Minecraft.getInstance();
      mc.execute(DolphinTest::stop);
   }

   private static void tick(Minecraft mc) {
      Session s = session;
      if (s == null) {
         return;
      }

      if (mc.level == null) {
         stop();
         return;
      }

      if (!(mc.level.getBlockEntity(s.tvPos) instanceof TvBlockEntity tv) || !tv.isPowered()) {
         stop();
         return;
      }

      if (!s.tvPos.equals(tv.consolePos())) {
         tv.plugConsole(s.tvPos);
      }

      s.emu.setPaused(mc.isPaused());
   }

   /**
    * Called by TvScreens from the render thread. No pixels are copied or
    * uploaded here: the shim already finished writing the shared texture. A
    * new screen record is published only when the serial advances.
    */
   private static void pump() {
      Session s = session;
      if (s == null || s.emu == null) {
         return;
      }

      int serial = s.emu.directSerial();
      if (serial <= 0 || serial == s.shownSerial) {
         return;
      }

      s.shownSerial = serial;
      int w = s.emu.width();
      int h = s.emu.height();
      if (w <= 0 || h <= 0) {
         w = WIDTH;
         h = HEIGHT;
      }

      TvScreens.show(s.tvPos, new TvScreens.Screen(s.texId, w, h, WIDTH, HEIGHT, serial));
   }

   private static TvBlockEntity nearestFreeTv(Minecraft mc) {
      BlockPos center = mc.player.blockPosition();
      BlockPos min = center.offset(-SEARCH_RADIUS, -32, -SEARCH_RADIUS);
      BlockPos max = center.offset(SEARCH_RADIUS, 32, SEARCH_RADIUS);
      TvBlockEntity best = null;
      double bestDistance = Double.MAX_VALUE;

      for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
         if (mc.level.getBlockEntity(pos) instanceof TvBlockEntity tv
            && tv.isPowered()
            && tv.consolePos() == null) {
            double distance = center.distSqr(pos);
            if (distance < bestDistance) {
               bestDistance = distance;
               best = tv;
            }
         }
      }

      return best;
   }

   private static Path coreFile() {
      Path path = CoreFiles.path("dolphin");
      if (Files.isRegularFile(path)) {
         return path;
      }

      String configured = System.getProperty("wcore.dolphin.core");
      if (configured != null && !configured.isBlank()) {
         Path alt = Path.of(configured);
         if (Files.isRegularFile(alt)) {
            return alt;
         }
      }

      return null;
   }

   private static Path demoFile() {
      String configured = System.getProperty("wcore.dolphin.demo");
      if (configured != null && !configured.isBlank()) {
         Path alt = Path.of(configured);
         if (Files.isRegularFile(alt)) {
            return alt;
         }
      }

      Path path = LibretroCore.consolesDir().resolve("test").resolve("gx_demo.dol");
      if (Files.isRegularFile(path)) {
         return path;
      }

      try (InputStream in = DolphinTest.class.getResourceAsStream("/test/gx_demo.dol")) {
         if (in != null) {
            Files.createDirectories(path.getParent());
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException e) {
         WorkingConsolesCore.LOGGER.warn("Couldn't unpack the GX demo: {}", e.toString());
      }

      return Files.isRegularFile(path) ? path : null;
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

   private static void say(String message, ChatFormatting color) {
      WorkingConsolesCore.LOGGER.info("Dolphin test: {}", message);
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.sendSystemMessage(Component.literal("[Dolphin test] " + message).withStyle(color));
      }
   }

   private static final class Session {
      final BlockPos tvPos;
      final BlockPos originalConsole;
      final Identifier texId;
      final EmuThread emu;
      int shownSerial = -1;

      Session(BlockPos tvPos, BlockPos originalConsole, Identifier texId, EmuThread emu) {
         this.tvPos = tvPos;
         this.originalConsole = originalConsole;
         this.texId = texId;
         this.emu = emu;
      }
   }
}



