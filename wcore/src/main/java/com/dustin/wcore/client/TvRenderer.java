package com.dustin.wcore.client;

import com.dustin.wcore.ConsoleSettings;
import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.block.ConsoleBlockEntity;
import com.dustin.wcore.block.MultiTvBlock;
import com.dustin.wcore.block.OutletBlock;
import com.dustin.wcore.block.TvBlock;
import com.dustin.wcore.block.TvBlockEntity;
import com.dustin.wcore.block.TvType;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.EnumMap;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TvRenderer extends GeoBlockRenderer<TvBlockEntity, BlockEntityRenderState> {
   public TvRenderer(Context context) {
      super(context, new TvRenderer.PerTypeModel());
   }

   public boolean shouldRenderOffScreen() {
      return true;
   }

   public int getViewDistance() {
      return 96;
   }

   public void submit(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      Level level = Minecraft.getInstance().level;
      BlockState bs = level != null && state.blockPos != null ? level.getBlockState(state.blockPos) : null;
      Direction facing = bs != null && bs.getBlock() instanceof TvBlock block ? (Direction)bs.getValue(TvBlock.FACING) : null;

      // quad TVs (flat + rear-projection at native size): the model is authored
      // centred on the seam between the two columns, so the drawn model shifts
      // half a block toward the side cell. the geo itself is untouched.
      Vec3 seam = Vec3.ZERO;
      if (facing != null && bs.getBlock() instanceof MultiTvBlock) {
         Direction side = MultiTvBlock.sideDir(facing);
         seam = new Vec3(side.getStepX() * 0.5, 0.0, side.getStepZ() * 0.5);
      }

      if (seam.lengthSqr() != 0.0) {
         poseStack.pushPose();
         poseStack.translate(seam.x, 0.0, seam.z);
         super.submit(state, poseStack, collector, camera);
         poseStack.popPose();
      } else {
         super.submit(state, poseStack, collector, camera);
      }

      if (level != null && state.blockPos != null && level.getBlockEntity(state.blockPos) instanceof TvBlockEntity tv) {
         if (bs != null && bs.getBlock() instanceof TvBlock) {
            TvType type = ((TvBlock)bs.getBlock()).type;
            AABB box = type.shape(Direction.NORTH).bounds();
            if (tv.outletPos() != null) {
               BlockState outlet = level.getBlockState(tv.outletPos());
               if (outlet.getBlock() instanceof OutletBlock) {
                  // the socket sits on the model's back, which the seam shift moves too
                  Vec3 from = Cables.port(
                        state.blockPos,
                        state.blockPos,
                        facing,
                        type.projects() ? 0.25 : box.maxZ - 0.5 - 0.02,
                        type.hanging() ? (box.minY + box.maxY) / 2.0 : Math.min(0.375, box.maxY * 0.5)
                     )
                     .add(seam);
                  Vec3 to = Cables.port(state.blockPos, tv.outletPos(), (Direction)outlet.getValue(OutletBlock.FACING), 0.4375, 0.5);
                  Cables.draw(poseStack, collector, state.blockPos, from, to, state.lightCoords);
               }
            }

            if (!tv.isPowered()) {
               track(state.blockPos, "no power at the TV's outlet");
            } else {
               // Always use the console's live picture: once a console is switched off (or its game stops) the TV goes
               // back to plain dark glass instead of pointing at a texture that no longer exists (purple/black squares).
               // get() runs the console's pump every frame: that uploads each new picture and feeds the controller.
               TvScreens.Screen live = TvScreens.get(tv.consolePos());
               if (live == null) {
                  track(state.blockPos, "no picture from the console");
                  return;
               }
               Identifier texId = live.texture();
               Object tex = Minecraft.getInstance().getTextureManager().getTexture(texId);
               if (tex == null) {
                  track(state.blockPos, "picture texture missing");
                  return;
               }
               track(state.blockPos, "showing");
               boolean projected = type.projects();
               double sw;
               double sh;
               Vec3 n;
               Vec3 c;
               if (projected) {
                  TvBlock.Projection pr = TvBlock.projection(level, state.blockPos, bs);
                  if (pr == null) {
                     return;
                  }

                  sw = pr.width();
                  sh = pr.height();
                  n = new Vec3(pr.facing().getStepX(), 0.0, pr.facing().getStepZ());
                  c = pr.centre();
               } else {
                  n = new Vec3(facing.getStepX(), 0.0, facing.getStepZ());
                  sw = type.w / 16.0;
                  sh = type.h / 16.0;
                  // +0.02 blocks: float the picture just off the glass plate, or the quad's pixels
                  // sit exactly on the model face and z-fight with it (constant shimmer/flicker).
                  c = new Vec3(0.5, type.y / 16.0, 0.5).add(n.scale(type.front / 16.0 + 0.02)).add(seam);
               }

               Vec3 right = new Vec3(0.0, 1.0, 0.0).cross(n);
               Vec3 hw = right.scale(sw / 2.0);
               Vec3 hh = new Vec3(0.0, sh / 2.0, 0.0);
               Vec3 tl = c.subtract(hw).add(hh);
               Vec3 tr = c.add(hw).add(hh);
               Vec3 br = c.add(hw).subtract(hh);
               Vec3 bl = c.subtract(hw).subtract(hh);
               float u1 = (float)live.width() / live.texWidth();
               float v1 = (float)live.height() / live.texHeight();
               float nx = (float)n.x;
               float nz = (float)n.z;

               final float v1Final = v1;

               int light = !projected && !ConsoleSettings.get().tvGlow ? state.lightCoords : 15728880;
               int color = -1;
               if (tv.consolePos() != null && level.getBlockEntity(tv.consolePos()) instanceof ConsoleBlockEntity con) {
                  switch (con.grade()) {
                     case LOW:
                        color = -4934476;
                        break;
                     case UNSTABLE:
                        if (PowerFlicker.dropped()) {
                           return;
                        }

                        color = PowerFlicker.tint();
                  }
               }

               int tint = color;
               final boolean flipped = TvScreens.isFlipped(texId);
               final float vTop = flipped ? v1Final : 0.0F;
               final float vBottom = flipped ? 0.0F : v1Final;
               collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texId), (pose, buffer) -> {
                  v(buffer, pose, tl, 0.0F, vTop, nx, nz, light, tint);
                  v(buffer, pose, bl, 0.0F, vBottom, nx, nz, light, tint);
                  v(buffer, pose, br, u1, vBottom, nx, nz, light, tint);
                  v(buffer, pose, tr, u1, vTop, nx, nz, light, tint);
               });
            }
         }
      }
   }

   private static final java.util.Map<BlockPos, String> SHOWN = new java.util.HashMap<>();

   /** Logs each change of what a TV shows ("[TV] x, y, z: showing -> no picture ..."), to find what blanks a screen. */
   private static void track(BlockPos pos, String what) {
      String was = SHOWN.put(pos.immutable(), what);
      if (was != null && !was.equals(what)) {
         WorkingConsolesCore.LOGGER.info("[TV] {}: {} -> {}", pos.toShortString(), was, what);
      }
   }

   private static void v(VertexConsumer buffer, Pose pose, Vec3 p, float u, float vv, float nx, float nz, int light, int color) {
      buffer.addVertex(pose, (float)p.x, (float)p.y, (float)p.z)
         .setColor(color)
         .setUv(u, vv)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(pose, nx, 0.0F, nz);
   }

   private static final class PerTypeModel extends DefaultedItemGeoModel<TvBlockEntity> {
      private final EnumMap<TvType, DefaultedItemGeoModel<TvBlockEntity>> models = new EnumMap<>(TvType.class);

      PerTypeModel() {
         super(WorkingConsolesCore.id("tv"));

         for (TvType t : TvType.values()) {
            this.models.put(t, new DefaultedItemGeoModel(WorkingConsolesCore.id(t.model)));
         }
      }

      private DefaultedItemGeoModel<TvBlockEntity> of(Object renderState) {
         Level level = Minecraft.getInstance().level;
         return renderState instanceof BlockEntityRenderState s
               && s.blockPos != null
               && level != null
               && level.getBlockState(s.blockPos).getBlock() instanceof TvBlock tv
            ? this.models.get(tv.type)
            : this.models.get(TvType.FLAT);
      }

      public Identifier getModelResource(GeoRenderState renderState) {
         return this.of(renderState).getModelResource(renderState);
      }

      public Identifier getTextureResource(GeoRenderState renderState) {
         return this.of(renderState).getTextureResource(renderState);
      }

      public Identifier getAnimationResource(TvBlockEntity be) {
         return this.models.get(be.getBlockState().getBlock() instanceof TvBlock tv ? tv.type : TvType.FLAT).getAnimationResource(be);
      }
   }
}