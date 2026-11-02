package com.dustin.wcore.client;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.block.TvBlockEntity;
import com.dustin.wcore.controller.RegisteredController;
import com.dustin.wcore.emu.BuildInfo;
import com.dustin.wcore.emu.LibretroCore;
import com.mojang.math.Axis;
import java.nio.file.Files;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.network.chat.Component;

public class WorkingConsolesCoreClient implements ClientModInitializer {
   public void onInitializeClient() {
      ClientPlayConnectionEvents.JOIN
         .register(
            (Join)(handler, sender, client) -> {
               if (BuildInfo.RELEASE && Files.exists(LibretroCore.consolesDir().resolve("dev.key"))) {
                  client.execute(
                     () -> {
                        if (client.player != null) {
                           client.player
                              .sendSystemMessage(
                                 Component.literal("[Working Consoles] Developer bypass disabled in release builds (" + BuildInfo.STAMP + ")")
                                    .withStyle(ChatFormatting.GOLD)
                              );
                        }
                     }
                  );
               }
            }
         );
      BlockEntityRenderers.register(WorkingConsolesCore.TV_BLOCK_ENTITY, TvRenderer::new);
      BlockEntityRenderers.register(WorkingConsolesCore.DECOR_BLOCK_ENTITY, DecorRenderer::new);
      RetroSessions.init();
      DolphinTest.register();
      ScreenShare.init();
      ControllerBindingHandler.init();
      ClientTickEvents.END_CLIENT_TICK.register(ControllerBindingHandler::clientTick);
      ManualBook.init();
      ClientTickEvents.END_CLIENT_TICK.register(RetroSessions::clientTick);
      BlockEntityRenderers.register(WorkingConsolesCore.POWER_MONITOR_BLOCK_ENTITY, PowerMonitorRenderer::new);
      BlockEntityRenderers.register(WorkingConsolesCore.GAME_SHELF_BLOCK_ENTITY, GameShelfRenderer::new);
      TwoHanded.register(WorkingConsolesCore.WIRELESS_CONTROLLER, CONTROLLER_POSE);
   }

   /** How a controller sits in both hands (first person). */
   public static final TwoHanded.Placement CONTROLLER_POSE = (poseStack, stack) -> {
      poseStack.translate(0.0F, -0.16F, 0.06F);
      poseStack.rotateDegrees(Axis.XP, 68.0F);
      poseStack.rotateDegrees(Axis.YP, 180.0F);
      poseStack.scale(0.5F, 0.5F, 0.5F);
   };

   /**
    * Client side of WorkingConsolesCore.registerController: the placed-controller renderer and the two-handed pose.
    * The item renderer needs no registration (ConsoleControllerItem supplies a ControllerRenderer itself). Call from
    * the module's ClientModInitializer.
    */
   public static void registerController(RegisteredController controller) {
      BlockEntityRenderers.register(controller.blockEntity(), context -> new ControllerBlockRenderer(context, controller.type()));
      TwoHanded.register(controller.item(), CONTROLLER_POSE);
   }
}
