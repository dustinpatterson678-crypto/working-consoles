package com.dustin.wcore.client;

import com.dustin.wcore.block.GameShelfBlock;
import com.dustin.wcore.block.GameShelfBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class GameShelfRenderer implements BlockEntityRenderer<GameShelfBlockEntity, BlockEntityRenderState> {
   private final ItemModelResolver items;
   private final ItemStackRenderState item = new ItemStackRenderState();

   public GameShelfRenderer(Context context) {
      this.items = context.itemModelResolver();
   }

   public BlockEntityRenderState createRenderState() {
      return new BlockEntityRenderState();
   }

   public void submit(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      Level level = Minecraft.getInstance().level;
      if (level != null && state.blockPos != null && level.getBlockEntity(state.blockPos) instanceof GameShelfBlockEntity shelf) {
         BlockState bs = level.getBlockState(state.blockPos);
         if (bs.getBlock() instanceof GameShelfBlock) {
            float yaw = -((Direction)bs.getValue(GameShelfBlock.FACING)).toYRot();
            float slotW = 0.109375F;

            for (int i = 0; i < 8; i++) {
               ItemStack stack = shelf.get(i);
               if (!stack.isEmpty()) {
                  poseStack.pushPose();
                  poseStack.translate(0.5, 0.5, 0.5);
                  poseStack.rotateDegrees(Axis.YP, yaw);
                  poseStack.translate(-0.4375F + slotW * (i + 0.5F), -0.03125F, 0.03125F);
                  poseStack.rotateDegrees(Axis.YP, 90.0F);
                  poseStack.scale(0.95F, 0.95F, 0.95F);
                  this.items.updateForTopItem(this.item, stack, ItemDisplayContext.FIXED, level, null, i);
                  this.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                  poseStack.popPose();
               }
            }
         }
      }
   }
}
