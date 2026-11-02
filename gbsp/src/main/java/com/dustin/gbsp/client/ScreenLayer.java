package com.dustin.gbsp.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class ScreenLayer<T extends GeoAnimatable, O, R extends GeoRenderState> extends GeoRenderLayer<T, O, R> {
   private final Predicate<R> show;
   private static final float X0 = -0.2125F;
   private static final float X1 = 0.2125F;
   private static final float Z_TOP = -0.20625F;
   private static final float Z_BOTTOM = 0.1125F;
   private static final float Y = 0.0990625F;

   public ScreenLayer(GeoRenderer<T, O, R> renderer, Predicate<R> show) {
      super(renderer);
      this.show = show;
   }

   public void addPerBoneRender(RenderPassInfo<R> renderPassInfo, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {
      HandheldSession.pump(Minecraft.getInstance());
      if (renderPassInfo.willRender() && HandheldSession.hasPicture() && this.show.test((R)renderPassInfo.renderState())) {
         renderPassInfo.model().getBone("screen").ifPresent(bone -> consumer.accept(bone, this::renderScreen));
      }
   }

   private void renderScreen(RenderPassInfo<R> renderPassInfo, GeoBone bone, SubmitNodeCollector renderTasks) {
      float u1 = HandheldSession.pictureWidth() / 256.0F;
      float v1 = HandheldSession.pictureHeight() / 224.0F;
      int light = 15728880;
      int overlay = OverlayTexture.NO_OVERLAY;
      renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), RenderTypes.entityCutout(HandheldSession.SCREEN_TEX), (pose, buffer) -> {
         PoseStack poseStack = renderPassInfo.poseStack();
         poseStack.pushPose();
         poseStack.last().set(pose);
         bone.translateAwayFromPivotPoint(poseStack);
         Pose p = poseStack.last();
         vertex(buffer, p, 0.2125F, -0.20625F, 0.0F, 0.0F, light, overlay);
         vertex(buffer, p, -0.2125F, -0.20625F, u1, 0.0F, light, overlay);
         vertex(buffer, p, -0.2125F, 0.1125F, u1, v1, light, overlay);
         vertex(buffer, p, 0.2125F, 0.1125F, 0.0F, v1, light, overlay);
         poseStack.popPose();
      });
   }

   private static void vertex(VertexConsumer buffer, Pose pose, float x, float z, float u, float v, int light, int overlay) {
      buffer.addVertex(pose, x, 0.0990625F, z).setColor(-1).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, 0.0F, -1.0F, 0.0F);
   }
}
