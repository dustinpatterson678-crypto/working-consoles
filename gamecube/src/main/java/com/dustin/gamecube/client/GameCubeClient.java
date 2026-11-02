package com.dustin.gamecube.client;

import com.dustin.gamecube.GameCubeFiles;
import com.dustin.gamecube.GameCubeMod;
import com.dustin.gamecube.block.GameCubeBlockEntity;
import com.dustin.gamecube.item.DiscCaseItem;
import com.dustin.wcore.ConsoleLinks;
import com.dustin.wcore.client.CaseRenderer;
import com.dustin.wcore.client.CoverArt.Panel;
import com.geckolib.renderer.GeoItemRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;

public class GameCubeClient implements ClientModInitializer {
   public void onInitializeClient() {
      GameCubeFiles.ensureFolders();
      GameCubeMod.CLIENT_FOCUS = GameCubeSessions::focus;
      ClientTickEvents.END_CLIENT_TICK.register(GameCubeSessions::clientTick);
      BlockEntityRenderers.register(GameCubeMod.CONSOLE_BLOCK_ENTITY, GameCubeRenderer::new);
      com.dustin.wcore.client.WorkingConsolesCoreClient.registerController(GameCubeMod.CONTROLLER);
      ConsoleLinks.register(() -> GameCubeSessions.isRunning(BlockPos.ZERO) ? "GameCube" : null);
   }

   public static GeoItemRenderer<DiscCaseItem> caseRenderer() {
      return new CaseRenderer<DiscCaseItem>(GameCubeMod.id("disc_case"), new Panel(0, 0, 96, 108), stack -> {
         String disc = DiscCaseItem.discOf(stack);
         return disc.isEmpty() ? null : GameCubeFiles.discDir().resolve(disc);
      });
   }
}