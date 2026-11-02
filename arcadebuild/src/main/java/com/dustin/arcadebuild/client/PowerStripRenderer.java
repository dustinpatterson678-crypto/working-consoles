package com.dustin.arcadebuild.client;

import com.dustin.arcadebuild.PowerStripBlock;
import com.dustin.arcadebuild.PowerStripBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The giant power strip: a rounded plastic body almost three blocks long, six chunky plugs with grip ridges,
 * heavy rubber cords that sag over the front lip and dive into a floor grommet, a glowing rocker switch,
 * per-socket LEDs and a fat feed cable out the back. Drawn, not a block model.
 */
public class PowerStripRenderer implements BlockEntityRenderer<PowerStripBlockEntity, BlockEntityRenderState> {
   private static final int FULL = 0xF000F0;

   public PowerStripRenderer(BlockEntityRendererProvider.Context context) {
   }

   @Override
   public BlockEntityRenderState createRenderState() {
      return new BlockEntityRenderState();
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return true;
   }

   @Override
   public void submit(BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      Level level = Minecraft.getInstance().level;
      if (level == null || state.blockPos == null) return;
      BlockState bs = level.getBlockState(state.blockPos);
      if (!(bs.getBlock() instanceof PowerStripBlock)) return;
      Direction face = bs.getValue(PowerStripBlock.FACING);
      final boolean on = bs.getValue(PowerStripBlock.LIT);
      final int light = LightCoordsUtil.getLightCoords(level, state.blockPos);
      final long ms = System.currentTimeMillis();
      pose.pushPose();
      pose.translate(0.5F, 0F, 0.5F);
      pose.mulPose(new org.joml.Matrix4f().rotationY((float)Math.toRadians(-(face.toYRot() + 180F))));
      pose.scale(1.6F, 1.6F, 1.6F);                                       // it really is giant
      pose.translate(-0.5F, 0F, -0.5F);
      collector.submitCustomGeometry(pose, RenderTypes.entityCutout(FacadeRenderer.WHITE), (p, b) -> draw(p, b, on, light, ms));
      pose.popPose();
   }

   private static void box(VertexConsumer b, Pose p, double x0, double y0, double z0, double x1, double y1, double z1, int col, int lt) {
      Draw.box(b, p, (float)x0, (float)y0, (float)z0, (float)x1, (float)y1, (float)z1, col, lt);
   }

   private static void quad(VertexConsumer b, Pose p, double[] a, double[] c, double[] d, double[] e, int col, int lt) {
      float[] pa = f(a), pc = f(c), pd = f(d), pe = f(e);
      float ux = pc[0] - pa[0], uy = pc[1] - pa[1], uz = pc[2] - pa[2], vx = pe[0] - pa[0], vy = pe[1] - pa[1], vz = pe[2] - pa[2];
      float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx, l = (float)Math.sqrt(nx * nx + ny * ny + nz * nz) + 1e-6F;
      float[] n = {nx / l, ny / l, nz / l}, m = {-n[0], -n[1], -n[2]};
      FacadeRenderer.vert(b, p, pa, 0, 0, n, col, lt);
      FacadeRenderer.vert(b, p, pc, 1, 0, n, col, lt);
      FacadeRenderer.vert(b, p, pd, 1, 1, n, col, lt);
      FacadeRenderer.vert(b, p, pe, 0, 1, n, col, lt);
      FacadeRenderer.vert(b, p, pe, 0, 1, m, col, lt);
      FacadeRenderer.vert(b, p, pd, 1, 1, m, col, lt);
      FacadeRenderer.vert(b, p, pc, 1, 0, m, col, lt);
      FacadeRenderer.vert(b, p, pa, 0, 0, m, col, lt);
   }

   private static float[] f(double[] v) {
      return new float[]{(float)v[0], (float)v[1], (float)v[2]};
   }

   /** Upright rounded end of the body. */
   private static void cyl(VertexConsumer b, Pose p, double cx, double cz, double r, double y0, double y1, int col, int lt) {
      int seg = 14;
      for (int i = 0; i < seg; i++) {
         double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
         double[] p0 = {cx + Math.cos(a0) * r, y0, cz + Math.sin(a0) * r}, p1 = {cx + Math.cos(a1) * r, y0, cz + Math.sin(a1) * r};
         double[] p2 = {p1[0], y1, p1[2]}, p3 = {p0[0], y1, p0[2]};
         quad(b, p, p0, p1, p2, p3, FacadeRenderer.mul(col, 0.8 + 0.2 * Math.cos((a0 + a1) / 2 - 2.2)), lt);
         quad(b, p, new double[]{cx, y1, cz}, p3, p2, new double[]{cx, y1, cz}, col, lt);
      }
   }

   /** A rubber cord along a quadratic curve (square section, two-sided). */
   private static void cord(VertexConsumer b, Pose p, double[] a, double[] c, double[] e, double r, int col, int lt) {
      int n = 12, dark = FacadeRenderer.mul(col, 0.75);
      double px = a[0], py = a[1], pz = a[2];
      for (int i = 1; i <= n; i++) {
         double t = (double)i / n, u = 1 - t;
         double x = u * u * a[0] + 2 * u * t * c[0] + t * t * e[0];
         double y = u * u * a[1] + 2 * u * t * c[1] + t * t * e[1];
         double z = u * u * a[2] + 2 * u * t * c[2] + t * t * e[2];
         double ty = y - py, tz = z - pz, tl = Math.sqrt(ty * ty + tz * tz) + 1e-6;
         double ny = -tz / tl * r, nz = ty / tl * r;
         side(b, p, px - r, py + ny, pz + nz, px + r, py + ny, pz + nz, x + r, y + ny, z + nz, x - r, y + ny, z + nz, col, lt);
         side(b, p, px - r, py - ny, pz - nz, px + r, py - ny, pz - nz, x + r, y - ny, z - nz, x - r, y - ny, z - nz, col, lt);
         side(b, p, px - r, py + ny, pz + nz, px - r, py - ny, pz - nz, x - r, y - ny, z - nz, x - r, y + ny, z + nz, dark, lt);
         side(b, p, px + r, py + ny, pz + nz, px + r, py - ny, pz - nz, x + r, y - ny, z - nz, x + r, y + ny, z + nz, dark, lt);
         px = x;
         py = y;
         pz = z;
      }
   }

   private static void side(VertexConsumer b, Pose p, double ax, double ay, double az, double bx, double by, double bz,
                            double cx, double cy, double cz, double dx, double dy, double dz, int col, int lt) {
      quad(b, p, new double[]{ax, ay, az}, new double[]{bx, by, bz}, new double[]{cx, cy, cz}, new double[]{dx, dy, dz}, col, lt);
   }

   private static final int[] RIDERS = {0xFFE03030, 0xFF3070E0, 0xFF30B040, 0xFFE0C020, 0xFFB040E0, 0xFFFF8030, 0xFF40D0D0, 0xFFF0F0F0};

   /** A tiny pixel person: legs, body, head. */
   private static void rider(VertexConsumer b, Pose p, double x, double y, double z, int c) {
      int col = RIDERS[c];
      box(b, p, x - 0.012, y, z - 0.008, x + 0.012, y + 0.025, z + 0.008, 0xFF202030, FULL);
      box(b, p, x - 0.016, y + 0.025, z - 0.01, x + 0.016, y + 0.055, z + 0.01, col, FULL);
      box(b, p, x - 0.011, y + 0.055, z - 0.009, x + 0.011, y + 0.077, z + 0.009, 0xFFF0C8A0, FULL);
   }

   /**
    * A porthole in the front of the strip: look in and there's a whole little world inside —
    * a lit concourse where the tiny travellers from every machine mill about between rides.
    */
   private static void porthole(VertexConsumer b, Pose p, double cx, double cy, double fz, double r, long ms, int lt) {
      int seg = 20;
      for (int i = 0; i < seg; i++) {                                    // chrome ring
         double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
         quad(b, p, new double[]{cx + Math.cos(a0) * r, cy + Math.sin(a0) * r, fz - 0.004}, new double[]{cx + Math.cos(a1) * r, cy + Math.sin(a1) * r, fz - 0.004},
            new double[]{cx + Math.cos(a1) * (r + 0.025), cy + Math.sin(a1) * (r + 0.025), fz - 0.004}, new double[]{cx + Math.cos(a0) * (r + 0.025), cy + Math.sin(a0) * (r + 0.025), fz - 0.004}, 0xFFD0D4DC, lt);
         // the far wall of the concourse, warm-lit, with a band of lit windows
         double w0 = cy + Math.sin(a0) * r, w1 = cy + Math.sin(a1) * r;
         quad(b, p, new double[]{cx, cy, fz - 0.002}, new double[]{cx + Math.cos(a0) * r, w0, fz - 0.002}, new double[]{cx + Math.cos(a1) * r, w1, fz - 0.002}, new double[]{cx, cy, fz - 0.002},
            (w0 + w1) / 2 < cy - r * 0.35 ? 0xFF6A4A30 : 0xFFF0D8A0, FULL);
      }
      double floor = cy - r * 0.35;
      for (int k = 0; k < 5; k++) {
         double wx = cx - r * 0.6 + k * r * 0.3;
         box(b, p, wx - 0.012, cy + r * 0.25, fz - 0.003, wx + 0.012, cy + r * 0.45, fz - 0.0025, (k + ms / 900) % 3 == 0 ? 0xFFFFF4C0 : 0xFFC09060, FULL);
      }
      for (int k = 0; k < 7; k++) {                                     // the crowd, walking back and forth
         double ph = (ms / (2200.0 + k * 310) + k * 0.23) % 2.0, t = ph < 1 ? ph : 2 - ph;
         double x = cx - r * 0.75 + t * r * 1.5;
         int col = RIDERS[k % RIDERS.length];
         double s = 0.6;
         box(b, p, x - 0.012 * s * 2, floor, fz - 0.0035, x + 0.012 * s * 2, floor + 0.05 * s * 2, fz - 0.003, col, FULL);
         box(b, p, x - 0.008 * s * 2, floor + 0.05 * s * 2, fz - 0.0035, x + 0.008 * s * 2, floor + 0.068 * s * 2, fz - 0.003, 0xFFF0C8A0, FULL);
      }
      // glass glint
      quad(b, p, new double[]{cx - r * 0.6, cy + r * 0.2, fz - 0.005}, new double[]{cx - r * 0.2, cy + r * 0.6, fz - 0.005},
         new double[]{cx - r * 0.1, cy + r * 0.5, fz - 0.005}, new double[]{cx - r * 0.5, cy + r * 0.1, fz - 0.005}, 0xFFFFFFFF, FULL);
   }

   private static void draw(Pose p, VertexConsumer b, boolean on, int lt, long ms) {
      int body = 0xFFE6E2D8, trim = 0xFFC8C2B4;
      double x0 = -0.75, x1 = 1.75, z0 = 0.2, z1 = 0.8, h = 0.42, r = (z1 - z0) / 2, cz = (z0 + z1) / 2;
      // rounded plastic housing with a raised top deck and a dark base skirt
      box(b, p, x0, 0.04, z0, x1, h, z1, body, lt);
      cyl(b, p, x0, cz, r, 0.04, h, body, lt);
      cyl(b, p, x1, cz, r, 0.04, h, body, lt);
      box(b, p, x0 - 0.05, 0.0, z0 + 0.03, x1 + 0.05, 0.05, z1 - 0.03, 0xFF3A3A40, lt);
      box(b, p, x0, h, z0 + 0.06, x1, h + 0.03, z1 - 0.06, trim, lt);
      box(b, p, x0 + 0.45, 0.22, z0 - 0.005, x1 - 0.05, 0.27, z0, 0xFFD02020, lt);                   // red stripe
      // six sockets with plugs, LEDs and cords
      int[] cords = {0xFF141414, 0xFF2A2A2E, 0xFF141414, 0xFF3A3A40, 0xFF141414, 0xFF22262A};
      for (int i = 0; i < 6; i++) {
         double sx = -0.5 + i * 0.4;
         box(b, p, sx - 0.13, h + 0.03, cz - 0.13, sx + 0.13, h + 0.035, cz + 0.13, 0xFF2A2A2A, lt);
         box(b, p, sx - 0.1, h + 0.035, cz - 0.12, sx + 0.1, h + 0.3, cz + 0.12, 0xFF1A1A1C, lt);       // plug body
         for (double gy = h + 0.08; gy < h + 0.27; gy += 0.05) box(b, p, sx - 0.105, gy, cz - 0.125, sx + 0.105, gy + 0.02, cz + 0.125, 0xFF26262A, lt);
         box(b, p, sx - 0.05, h + 0.3, cz - 0.05, sx + 0.05, h + 0.36, cz + 0.05, 0xFF1A1A1C, lt);       // strain relief
         boolean blink = on && ((ms / 400 + i) % 7 != 0);
         box(b, p, sx - 0.025, h + 0.032, z0 + 0.07, sx + 0.025, h + 0.045, z0 + 0.11, blink ? 0xFF40FF60 : 0xFF1C3A20, blink ? FULL : lt);
         double spread = (i - 2.5) * 0.06;
         double[] pa = {sx, h + 0.36, cz}, pc = {sx + spread, h + 0.75, z0 - 0.5}, pe = {sx + spread * 3, 0.01, -0.75};
         cord(b, p, pa, pc, pe, 0.035, cords[i], lt);
         // tiny travellers riding the cord up from the games into the strip
         if (on) for (int k = 0; k < 2; k++) {
            double t = 1 - ((ms / 3200.0 + i * 0.37 + k * 0.5) % 1.0), u = 1 - t;
            double x = u * u * pa[0] + 2 * u * t * pc[0] + t * t * pe[0], y = u * u * pa[1] + 2 * u * t * pc[1] + t * t * pe[1] + 0.035, z = u * u * pa[2] + 2 * u * t * pc[2] + t * t * pe[2];
            rider(b, p, x, y, z, (i * 2 + k) % RIDERS.length);
         }
      }
      if (on) porthole(b, p, x0 + 0.22, h * 0.5, z0, 0.16, ms, lt);
      // rocker switch and its glow
      double wx = x1 + 0.05;
      box(b, p, wx - 0.12, h - 0.02, cz - 0.12, wx + 0.12, h + 0.04, cz + 0.12, 0xFF2A2A2A, lt);
      box(b, p, wx - 0.09, h + 0.04, cz - 0.09, wx + 0.09, h + (on ? 0.12 : 0.07), cz + 0.09, on ? 0xFFFF3020 : 0xFF6A1810, on ? FULL : lt);
      // floor grommet where the cords disappear into the wiring channel
      box(b, p, x0 - 0.1, 0.0, -0.9, x1 + 0.1, 0.02, -0.6, 0xFF505058, lt);
      box(b, p, x0, 0.0, -0.85, x1, 0.025, -0.65, 0xFF0A0A0C, lt);
      // heavy feed cable out the back into the floor
      cord(b, p, new double[]{x1 + 0.2, 0.2, cz}, new double[]{x1 + 0.7, 0.25, 1.2}, new double[]{x1 + 0.6, 0.01, 1.6}, 0.07, 0xFF101010, lt);
      box(b, p, x1 + 0.35, 0.0, 1.45, x1 + 0.85, 0.02, 1.75, 0xFF505058, lt);
   }
}
