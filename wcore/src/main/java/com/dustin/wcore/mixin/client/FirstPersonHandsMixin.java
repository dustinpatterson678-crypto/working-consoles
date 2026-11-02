package com.dustin.wcore.mixin.client;

import com.dustin.wcore.client.TwoHanded;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsMixin {
   @Shadow
   private void renderMapHand(PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidArm arm, PlayerRenderState playerState) {
   }

   @Shadow
   private float calculateMapTilt(float pitch) {
      return 0.0F;
   }

   @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
   private void wcore$holdInBothHands(
      PlayerRenderState playerState,
      FirstPersonHandsAndItemsRenderState state,
      float partialTick,
      float pitch,
      InteractionHand hand,
      float swing,
      ItemStack stack,
      float equip,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      CallbackInfo ci
   ) {
      TwoHanded.Placement placement = TwoHanded.get(stack);
      if (placement != null && hand == InteractionHand.MAIN_HAND && state.offHandItem.isEmpty()) {
         ci.cancel();
         poseStack.pushPose();
         float sq = Mth.sqrt(swing);
         float swingY = -0.2F * Mth.sin(swing * (float) Math.PI);
         float swingZ = -0.4F * Mth.sin(sq * (float) Math.PI);
         poseStack.translate(0.0F, -swingY / 2.0F, swingZ);
         float tilt = this.calculateMapTilt(pitch);
         poseStack.translate(0.0F, 0.04F + equip * -1.2F + tilt * -0.5F, -0.72F);
         poseStack.rotateDegrees(Axis.XP, tilt * -85.0F);
         if (playerState.avatarRenderState != null && !playerState.avatarRenderState.isInvisible) {
            poseStack.pushPose();
            poseStack.rotateDegrees(Axis.YP, 90.0F);
            this.renderMapHand(poseStack, collector, light, HumanoidArm.RIGHT, playerState);
            this.renderMapHand(poseStack, collector, light, HumanoidArm.LEFT, playerState);
            poseStack.popPose();
         }

         poseStack.rotateDegrees(Axis.XP, Mth.sin(sq * (float) Math.PI) * 20.0F);
         poseStack.scale(2.0F, 2.0F, 2.0F);
         placement.apply(poseStack, stack);
         state.mainHandRenderState.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
         poseStack.popPose();
      }
   }
}
