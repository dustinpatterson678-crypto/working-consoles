package com.dustin.gbsp.client;

import com.dustin.gbsp.GameBoySP;
import com.dustin.gbsp.emu.SystemFiles;
import com.dustin.wcore.ConsoleSettings;
import com.dustin.wcore.client.ControllerInput;
import com.dustin.wcore.client.McAudioSink;
import com.dustin.wcore.emu.DumpCheck;
import com.dustin.wcore.emu.EmuThread;
import com.dustin.wcore.emu.NativeCore;
import com.dustin.wcore.emu.DumpCheck.Kind;
import com.dustin.wcore.emu.DumpCheck.Result;
import com.geckolib.animatable.GeoItem;
import com.mojang.blaze3d.platform.NativeImage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

public final class HandheldSession {
   public static final Identifier SCREEN_TEX = GameBoySP.id("dynamic/handheld_screen");
   public static final int TEX_W = 256;
   public static final int TEX_H = 224;
   private static EmuThread emu;
   private static String title;
   private static DynamicTexture texture;
   private static boolean hasPicture;
   private static int picW = 240;
   private static int picH = 160;
   private static int keys;
   private static long lastFrameNanos;
   private static BlockPos attachedPos;
   private static BlockPos pendingPos;
   private static int placeGraceTicks;
   private static NativeCore currentCore;

   private HandheldSession() {
   }

   public static NativeCore core() {
      return emu == null ? null : currentCore;
   }

   public static boolean isOn() {
      return emu != null;
   }

   public static boolean isOnAt(BlockPos pos) {
      return emu != null && pos.equals(attachedPos);
   }

   public static boolean isInHands() {
      return emu != null && attachedPos == null;
   }

   public static boolean hasPicture() {
      return emu != null && hasPicture;
   }

   public static int keys() {
      return emu == null ? 0 : keys;
   }

   public static String title() {
      return title;
   }

   public static int pictureWidth() {
      return picW;
   }

   public static int pictureHeight() {
      return picH;
   }

   public static String start(SystemFiles.Report report, SystemFiles.Rom rom) {
      stop();
      SystemFiles.Bios bios = report.gbaBios();
      Minecraft mc0 = Minecraft.getInstance();
      Result check = DumpCheck.check(mc0.getUser().getName(), mc0.getUser().getProfileId(), bios == null ? null : bios.file(), rom.file(), Kind.HANDHELD);
      if (!check.allowed()) {
         shakeHeld(mc0);
         return "Won't boot: this looks downloaded, not dumped from your own console (" + check.summary() + ")";
      }

      if (check.bypassed() && mc0.player != null) {
         mc0.player
            .sendSystemMessage(Component.literal("[Handheld] Developer profile: booting anyway (" + check.summary() + ")").withStyle(ChatFormatting.YELLOW));
      }

      Object result = NativeCore.open(rom.file(), bios == null ? null : bios.file(), SystemFiles.saveFor(rom.file()), check.pass());
      if (result instanceof NativeCore core) {
         if (texture == null) {
            texture = new DynamicTexture(() -> "gbsp handheld screen", 256, 224, true);
            Minecraft.getInstance().getTextureManager().register(SCREEN_TEX, texture);
         }

         currentCore = core;
         emu = new EmuThread(core, rom.title(), ConsoleSettings.get().soundInWorld ? McAudioSink.ears() : null);
         title = rom.title();
         hasPicture = false;
         attachedPos = pendingPos;
         GameBoySP.LOGGER.info("Booted {} {}", rom.title(), attachedPos == null ? "in your hands" : "on the placed handheld at " + attachedPos);
         return null;
      } else {
         return String.valueOf(result);
      }
   }

   private static void shakeHeld(Minecraft mc) {
      if (mc.player != null) {
         for (ItemStack stack : new ItemStack[]{mc.player.getMainHandItem(), mc.player.getOffhandItem()}) {
            if (stack.getItem() instanceof GeoItem geo && isHandheld(stack)) {
               geo.triggerAnim(mc.player, GeoItem.getId(stack), "main", "no_files");
               return;
            }
         }
      }
   }

   public static void stop() {
      if (emu != null) {
         emu.stop();
         emu = null;
      }

      hasPicture = false;
      keys = 0;
      attachedPos = null;
   }

   public static void clientTick(Minecraft mc) {
      if (emu != null) {
         Player player = mc.player;
         if (player == null) {
            stop();
         } else {
            boolean active;
            if (attachedPos != null) {
               boolean blockThere = mc.level != null && mc.level.getBlockState(attachedPos).is(GameBoySP.HANDHELD_BLOCK);
               if (!blockThere && placeGraceTicks > 0) {
                  placeGraceTicks--;
                  emu.setPaused(true);
                  return;
               }

               placeGraceTicks = 0;
               if (!blockThere) {
                  GameBoySP.LOGGER.info("Handheld picked up: game continues in your hands");
                  attachedPos = null;
                  active = isHandheld(player.getMainHandItem()) || isHandheld(player.getOffhandItem());
               } else {
                  double range = ControllerInput.isHoldingController(player) ? 32.0 : 8.0;
                  active = player.distanceToSqr(attachedPos.getX() + 0.5, attachedPos.getY() + 0.5, attachedPos.getZ() + 0.5) < range * range;
               }
            } else {
               active = isHandheld(player.getMainHandItem()) || isHandheld(player.getOffhandItem());
            }

            emu.setPaused(!active || mc.isPaused());
            emu.setVolume(ConsoleSettings.get().volumeFactor());
            pump(mc);
         }
      }
   }

   public static void pump(Minecraft mc) {
      if (emu != null) {
         long now = System.nanoTime();
         if (now - lastFrameNanos >= 2000000L) {
            lastFrameNanos = now;
            if (mc.gui.screen() == null) {
               keys = HandheldInput.poll(false);
            } else if (mc.gui.screen() instanceof GameBoyScreen) {
               keys = HandheldInput.poll(true);
            } else {
               keys = 0;
            }

            emu.setKeys(keys);
            int[] frame = emu.takeFrame();
            if (frame != null && texture != null) {
               int w = emu.width();
               int h = emu.height();
               ScreenFilter.apply(frame, w * h);
               NativeImage img = texture.getPixels();
               if (img != null) {
                  MemorySegment dst = MemorySegment.ofAddress(img.getPointer()).reinterpret(229376L);

                  for (int y = 0; y < h; y++) {
                     MemorySegment.copy(frame, y * w, dst, ValueLayout.JAVA_INT, y * 256L * 4L, w);
                  }

                  texture.upload();
                  picW = w;
                  picH = h;
                  hasPicture = true;
               }
            }
         }
      }
   }

   public static boolean isHandheld(ItemStack stack) {
      return stack.is(GameBoySP.GAME_BOY_SP);
   }

   public static void onUse(SystemFiles.Report report, boolean sneaking) {
      if (isInHands() && sneaking) {
         openBigView();
      } else {
         pendingPos = null;
         if (report.ready() || isOn()) {
            HandheldMenu.show(report);
         }
      }
   }

   public static void onBlockUse(BlockPos pos) {
      pendingPos = pos.immutable();
      GameBoySP.LOGGER.info("Placed handheld at {} right-clicked: showing game list", pendingPos);
      HandheldMenu.show(SystemFiles.scan());
   }

   public static void onPlaced(BlockPos pos) {
      if (isInHands()) {
         attachedPos = pos.immutable();
         placeGraceTicks = 40;
         GameBoySP.LOGGER.info("Handheld set down at {} while on: game continues on the block", attachedPos);
      }
   }

   public static void onCaseUse(String romFile) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         BlockPos target = null;
         if (mc.hitResult instanceof BlockHitResult hit && mc.level != null && mc.level.getBlockState(hit.getBlockPos()).is(GameBoySP.HANDHELD_BLOCK)) {
            target = hit.getBlockPos().immutable();
         } else if (!isHandheld(mc.player.getMainHandItem()) && !isHandheld(mc.player.getOffhandItem())) {
            mc.player.sendOverlayMessage(Component.literal("Hold the handheld in your other hand, or look at a placed one"));
            return;
         }

         SystemFiles.Report report = SystemFiles.scan();

         for (SystemFiles.Rom rom : report.roms()) {
            if (rom.file().getFileName().toString().equals(romFile)) {
               pendingPos = target;
               String err = start(report, rom);
               mc.player.sendOverlayMessage(Component.literal(err != null ? err : "Booting " + rom.title()));
               return;
            }
         }

         mc.player.sendOverlayMessage(Component.literal("That game isn't in gameboy/roms anymore"));
      }
   }

   public static void openBigView() {
      if (isOn()) {
         Minecraft.getInstance().setScreenAndShow(new GameBoyScreen());
      }
   }
}
