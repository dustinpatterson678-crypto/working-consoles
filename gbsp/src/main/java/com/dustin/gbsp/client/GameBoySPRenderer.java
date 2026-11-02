package com.dustin.gbsp.client;

import com.dustin.gbsp.GameBoySP;
import com.dustin.gbsp.item.GameBoySPItem;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.GeoItemRenderer.RenderData;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class GameBoySPRenderer extends GeoItemRenderer<GameBoySPItem> {
   private static final float PRESS = -0.16F;
   private static final float TILT = 0.14F;

   public GameBoySPRenderer() {
      super(new DefaultedItemGeoModel(GameBoySP.id("game_boy_sp")));
      this.withRenderLayer(new AutoGlowingGeoLayer<GameBoySPItem, RenderData, GeoRenderState>(this) {
         protected RenderType getRenderType(GeoRenderState renderState) {
            return RenderTypes.eyes(this.getTextureResource(renderState));
         }
      });
      this.withRenderLayer(new ScreenLayer<>(this, renderState -> HandheldSession.isInHands()));
   }

   public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> renderPassInfo, BoneSnapshots snapshots) {
      pressButtons(snapshots, HandheldSession.isInHands() ? HandheldSession.keys() : 0);
   }

   public static void pressButtons(BoneSnapshots snapshots, int k) {
      if (k != 0) {
         press(snapshots, "button_a", k, 1);
         press(snapshots, "button_b", k, 2);
         press(snapshots, "button_start", k, 8);
         press(snapshots, "button_select", k, 4);
         if ((k & 512) != 0) {
            snapshots.get("button_l").ifPresent(s -> s.setTranslateY(-0.16F));
         }

         if ((k & 256) != 0) {
            snapshots.get("button_r").ifPresent(s -> s.setTranslateY(-0.16F));
         }

         int dirs = k & 240;
         if (dirs != 0) {
            float rx = ((k & 64) != 0 ? -0.14F : 0.0F) + ((k & 128) != 0 ? 0.14F : 0.0F);
            float rz = ((k & 32) != 0 ? -0.14F : 0.0F) + ((k & 16) != 0 ? 0.14F : 0.0F);
            snapshots.get("dpad").ifPresent(s -> s.setRotation(rx, 0.0F, rz).setTranslateY(-0.08F));
         }
      }
   }

   private static void press(BoneSnapshots snapshots, String bone, int keys, int mask) {
      if ((keys & mask) != 0) {
         snapshots.get(bone).ifPresent(s -> s.setTranslateY(-0.16F));
      }
   }
}
