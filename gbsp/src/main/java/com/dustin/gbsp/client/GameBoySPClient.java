package com.dustin.gbsp.client;

import com.dustin.gbsp.GameBoySP;
import com.dustin.gbsp.block.HandheldBlockEntity;
import com.dustin.gbsp.emu.SystemFiles;
import com.dustin.gbsp.item.GameCaseItem;
import com.dustin.wcore.ConsoleLinks;
import com.dustin.wcore.ConsoleSettings;
import com.dustin.wcore.client.CaseRenderer;
import com.dustin.wcore.client.TwoHanded;
import com.dustin.wcore.client.CoverArt.Panel;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.math.Axis;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;

public class GameBoySPClient implements ClientModInitializer {
   public void onInitializeClient() {
      ClientLinkCableHandler.init();
      ClientTickEvents.END_CLIENT_TICK.register(ClientLinkCableHandler::clientTick);
      GameBoySP.CLIENT_USE = HandheldSession::onUse;
      GameBoySP.HANDHELD_ON = HandheldSession::isInHands;
      GameBoySP.HANDHELD_ON_AT = HandheldSession::isOnAt;
      GameBoySP.CLIENT_BLOCK_USE = HandheldSession::onBlockUse;
      GameBoySP.CLIENT_PLACED = HandheldSession::onPlaced;
      GameBoySP.CLIENT_CASE_USE = HandheldSession::onCaseUse;
      HandheldMenu.register();
      TwoHanded.register(GameBoySP.GAME_BOY_SP, (poseStack, stack) -> {
         float size = ConsoleSettings.get().handheldSize / 100.0F;
         poseStack.translate(0.0F, -0.06F + 0.04F * (size - 1.0F), -0.2F + 0.08F * (size - 1.0F));
         poseStack.rotateDegrees(Axis.XP, 40.0F);
         poseStack.rotateDegrees(Axis.YP, 180.0F);
         poseStack.scale(0.4F * size, 0.4F * size, 0.4F * size);
      });
      TwoHanded.register(GameBoySP.GUIDE_BOOK, (poseStack, stack) -> {
         poseStack.translate(0.0F, -0.06F, 0.02F);
         poseStack.rotateDegrees(Axis.XP, 80.0F);
         poseStack.rotateDegrees(Axis.YP, 180.0F);
         poseStack.scale(0.42F, 0.42F, 0.42F);
      });
      ConsoleLinks.register(() -> HandheldSession.isOn() ? "Clamshell Handheld (" + HandheldSession.title() + ")" : null);
      GuideBookClient.init();
      ClientTickEvents.END_CLIENT_TICK.register(HandheldSession::clientTick);
      BlockEntityRenderers.register(
         GameBoySP.HANDHELD_BLOCK_ENTITY,
         context -> (new GeoBlockRenderer<HandheldBlockEntity, BlockEntityRenderState>(context, new DefaultedItemGeoModel(GameBoySP.id("game_boy_sp"))) {
            public void adjustModelBonesForRender(RenderPassInfo<BlockEntityRenderState> info, BoneSnapshots snapshots) {
               BlockPos pos = ((BlockEntityRenderState)info.renderState()).blockPos;
               if (pos != null && HandheldSession.isOnAt(pos)) {
                  GameBoySPRenderer.pressButtons(snapshots, HandheldSession.keys());
               }
            }
         }).withRenderLayer(renderer -> new ScreenLayer<>(renderer, renderState -> renderState.blockPos != null && HandheldSession.isOnAt(renderState.blockPos)))
      );
   }

   public static GeoItemRenderer<GameCaseItem> caseRenderer() {
      return new CaseRenderer<GameCaseItem>(GameBoySP.id("game_case"), new Panel(0, 0, 108, 120), stack -> {
         String rom = GameCaseItem.romOf(stack);
         return rom.isEmpty() ? null : SystemFiles.romDir().resolve(rom);
      });
   }
}
