package com.dustin.wcore.client;

import com.dustin.wcore.ConsoleSettings;
import com.dustin.wcore.block.OutletBlock;
import com.dustin.wcore.block.TvBlock;
import com.dustin.wcore.retro.RetroConsoleBlock;
import com.dustin.wcore.retro.RetroConsoleBlockEntity;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RetroConsoleRenderer extends GeoBlockRenderer<RetroConsoleBlockEntity, BlockEntityRenderState> {
   private final RetroConsoleRenderer.BuiltInScreen screen;
   private final double portSide;
   private final double portHeight;

   public RetroConsoleRenderer(Context context, Identifier model, RetroConsoleRenderer.BuiltInScreen screen, double portSide, double portHeight) {
      super(context, new DefaultedItemGeoModel(model));
      this.screen = screen;
      this.portSide = portSide;
      this.portHeight = portHeight;
   }

   public boolean shouldRenderOffScreen() {
      return this.screen != null;
   }

   public void adjustModelBonesForRender(RenderPassInfo<BlockEntityRenderState> info, BoneSnapshots snapshots) {
      RetroConsoleBlockEntity be = console(((BlockEntityRenderState)info.renderState()).blockPos);
      boolean on = be != null && be.isOn();
      boolean standby = be != null && !on && be.isPowered();
      snapshots.get("led_on").ifPresent(s -> s.skipRender(!on));
      snapshots.get("led_standby").ifPresent(s -> s.skipRender(!standby));
   }

   public void submit(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      super.submit(state, poseStack, collector, camera);
      Level level = Minecraft.getInstance().level;
      RetroConsoleBlockEntity be = console(state.blockPos);
      if (level != null && be != null) {
         BlockState bs = level.getBlockState(state.blockPos);
         if (bs.getBlock() instanceof RetroConsoleBlock) {
            Direction facing = (Direction)bs.getValue(RetroConsoleBlock.FACING);
            BlockPos self = state.blockPos;
            BlockPos tv = be.tvPos();
            if (tv != null && level.getBlockState(tv).getBlock() instanceof TvBlock) {
               Direction tvFacing = (Direction)level.getBlockState(tv).getValue(TvBlock.FACING);
               Cables.draw(
                  poseStack,
                  collector,
                  self,
                  Cables.port(self, self, facing, this.portSide / 16.0, (this.portHeight + 0.8) / 16.0),
                  Cables.port(self, tv, tvFacing, 0.1, 0.375),
                  state.lightCoords
               );
            }

            BlockPos outlet = be.outletPos();
            if (outlet != null && level.getBlockState(outlet).getBlock() instanceof OutletBlock) {
               Direction oFacing = (Direction)level.getBlockState(outlet).getValue(OutletBlock.FACING);
               Cables.draw(
                  poseStack,
                  collector,
                  self,
                  Cables.port(self, self, facing, this.portSide / 16.0, this.portHeight / 16.0),
                  Cables.port(self, outlet, oFacing, 0.4375, 0.5),
                  state.lightCoords
               );
            }

            if (this.screen != null && be.isOn() && be.isPowered()) {
               TvScreens.Screen pic = TvScreens.get(self);
               if (pic != null) {
                  int color = -1;
                  switch (be.grade()) {
                     case LOW:
                        color = -4934476;
                        break;
                     case UNSTABLE:
                        if (PowerFlicker.dropped()) {
                           return;
                        }

                        color = PowerFlicker.tint();
                  }

                  Vec3 n = new Vec3(facing.getStepX(), 0.0, facing.getStepZ());
                  Vec3 right = new Vec3(0.0, 1.0, 0.0).cross(n);
                  Vec3 c = new Vec3(0.5, this.screen.y() / 16.0, 0.5).add(n.scale(this.screen.front() / 16.0 + SCREEN_LIFT));
                  Vec3 hw = right.scale(this.screen.w() / 32.0);
                  Vec3 hh = new Vec3(0.0, this.screen.h() / 32.0, 0.0);
                  Vec3 tl = c.subtract(hw).add(hh);
                  Vec3 tr = c.add(hw).add(hh);
                  Vec3 br = c.add(hw).subtract(hh);
                  Vec3 bl = c.subtract(hw).subtract(hh);
                  float u1 = (float)pic.width() / pic.texWidth();
                  float v1 = (float)pic.height() / pic.texHeight();
                  float nx = (float)n.x;
                  float nz = (float)n.z;
                  int light = ConsoleSettings.get().tvGlow ? 15728880 : state.lightCoords;
                  int tint = color;
                  collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(pic.texture()), (pose, buffer) -> {
                     v(buffer, pose, tl, 0.0F, 0.0F, nx, nz, light, tint);
                     v(buffer, pose, bl, 0.0F, v1, nx, nz, light, tint);
                     v(buffer, pose, br, u1, v1, nx, nz, light, tint);
                     v(buffer, pose, tr, u1, 0.0F, nx, nz, light, tint);
                  });
               }
            }
         }
      }
   }

   /**
    * How far the picture floats off the model's screen face, in blocks. Cabinets have a dark screen face behind the
    * picture and CRT glass just in front of it, each ~0.004 apart; shader packs (shadow maps, TAA jitter) can't keep
    * layers that close apart and the picture flickers. 0.012 clears both.
    */
   private static final double SCREEN_LIFT = 0.012;

   private static void v(VertexConsumer buffer, Pose pose, Vec3 p, float u, float vv, float nx, float nz, int light, int color) {
      buffer.addVertex(pose, (float)p.x, (float)p.y, (float)p.z)
         .setColor(color)
         .setUv(u, vv)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(pose, nx, 0.0F, nz);
   }

   private static RetroConsoleBlockEntity console(BlockPos pos) {
      Level level = Minecraft.getInstance().level;
      return level != null && pos != null && level.getBlockEntity(pos) instanceof RetroConsoleBlockEntity be ? be : null;
   }

   public record BuiltInScreen(double y, double front, double w, double h) {
   }
}
