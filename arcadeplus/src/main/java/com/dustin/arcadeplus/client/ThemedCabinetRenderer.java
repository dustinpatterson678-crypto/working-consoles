package com.dustin.arcadeplus.client;

import com.dustin.wcore.client.RetroConsoleRenderer;
import com.dustin.wcore.retro.RetroConsoleBlock;
import com.dustin.wcore.retro.RetroConsoleBlockEntity;
import com.dustin.wcore.retro.RetroSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The normal cabinet renderer plus the theme kit: your own marquee and side art next to the game file. */
public class ThemedCabinetRenderer extends RetroConsoleRenderer {
   /** Positions in model pixels: marquee centre height, distance in front, width, height; side panel x, centre y/z, depth, height. */
   public record Theme(double marqueeY, double marqueeFront, double marqueeW, double marqueeH,
                       double sideX, double sideY, double sideZ, double sideW, double sideH) {
   }

   private final Theme theme;

   public ThemedCabinetRenderer(Context context, Identifier model, BuiltInScreen screen, Theme theme) {
      super(context, model, screen, 6.0, 1.0);
      this.theme = theme;
   }

   @Override
   public void submit(BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      if (RenderCull.far(state)) return;
      super.submit(state, pose, collector, camera);
      Level level = Minecraft.getInstance().level;
      if (level == null || state.blockPos == null || !(level.getBlockEntity(state.blockPos) instanceof RetroConsoleBlockEntity be)) {
         return;
      }
      BlockState bs = level.getBlockState(state.blockPos);
      RetroSystem sys = be.system();
      String game = be.gameFile();
      if (!(bs.getBlock() instanceof RetroConsoleBlock) || sys == null || game == null || game.isEmpty()) {
         return;
      }
      int dot = game.lastIndexOf('.');
      String base = dot > 0 ? game.substring(0, dot) : game;
      Identifier marquee = ThemeTextures.get(sys.gamesDir().resolve(base + ".marquee.png"));
      Identifier side = ThemeTextures.get(sys.gamesDir().resolve(base + ".side.png"));
      if (marquee == null && side == null) {
         return;
      }
      Direction facing = bs.getValue(RetroConsoleBlock.FACING);
      int lit = be.isPowered() ? 15728880 : state.lightCoords;
      int light = state.lightCoords;
      Theme t = this.theme;
      pose.pushPose();
      pose.translate(0.5, 0.0, 0.5);
      pose.rotate(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
      pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
      if (marquee != null) {
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(marquee), (p, b) ->
            quadZ(b, p, (float)t.marqueeY(), (float)-t.marqueeFront(), (float)t.marqueeW(), (float)t.marqueeH(), lit));
      }
      if (side != null) {
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(side), (p, b) -> {
            quadX(b, p, (float)t.sideX(), (float)t.sideY(), (float)t.sideZ(), (float)t.sideW(), (float)t.sideH(), light, 1);
            quadX(b, p, (float)-t.sideX(), (float)t.sideY(), (float)t.sideZ(), (float)t.sideW(), (float)t.sideH(), light, -1);
         });
      }
      pose.popPose();
   }

   /** Facing the player (-z), image upright. */
   private static void quadZ(VertexConsumer b, Pose p, float cy, float z, float w, float h, int light) {
      float x0 = -w / 2, x1 = w / 2, y0 = cy - h / 2, y1 = cy + h / 2;
      v(b, p, x1, y1, z, 0, 0, 0, 0, -1, light);
      v(b, p, x1, y0, z, 0, 1, 0, 0, -1, light);
      v(b, p, x0, y0, z, 1, 1, 0, 0, -1, light);
      v(b, p, x0, y1, z, 1, 0, 0, 0, -1, light);
   }

   /** Side panel on x = sx facing outward (sign = +1 right side, -1 left side); counter-clockwise as seen from outside. */
   private static void quadX(VertexConsumer b, Pose p, float sx, float cy, float cz, float w, float h, int light, int sign) {
      float z0 = cz - w / 2, z1 = cz + w / 2, y0 = cy - h / 2, y1 = cy + h / 2;
      if (sign > 0) {
         // seen from +x the back (z1) is on the viewer's left
         v(b, p, sx, y1, z1, 0, 0, 1, 0, 0, light);
         v(b, p, sx, y0, z1, 0, 1, 1, 0, 0, light);
         v(b, p, sx, y0, z0, 1, 1, 1, 0, 0, light);
         v(b, p, sx, y1, z0, 1, 0, 1, 0, 0, light);
      } else {
         // seen from -x the front (z0) is on the viewer's left
         v(b, p, sx, y1, z0, 0, 0, -1, 0, 0, light);
         v(b, p, sx, y0, z0, 0, 1, -1, 0, 0, light);
         v(b, p, sx, y0, z1, 1, 1, -1, 0, 0, light);
         v(b, p, sx, y1, z1, 1, 0, -1, 0, 0, light);
      }
   }

   private static void v(VertexConsumer b, Pose p, float x, float y, float z, float u, float vv, float nx, float ny, float nz, int light) {
      b.addVertex(p, x, y, z).setColor(-1).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
   }
}
