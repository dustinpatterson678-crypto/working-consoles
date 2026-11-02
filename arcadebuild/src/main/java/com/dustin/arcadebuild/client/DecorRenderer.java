package com.dustin.arcadebuild.client;

import com.dustin.arcadebuild.DecorBlock;
import com.dustin.arcadebuild.DecorBlockEntity;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Draws every DecorBlock as a real model: front is -z, floor is y=0, the block spans 0..1. */
public class DecorRenderer implements BlockEntityRenderer<DecorBlockEntity, BlockEntityRenderState> {
   static final int FULL = 0xF000F0;

   public DecorRenderer(BlockEntityRendererProvider.Context context) {
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
      BlockPos pos = state.blockPos;
      BlockState bs = level.getBlockState(pos);
      if (!(bs.getBlock() instanceof DecorBlock db)) return;
      Direction face = bs.getValue(DecorBlock.FACING);
      boolean left = same(level, pos.relative(face.getCounterClockWise()), db, face);
      boolean right = same(level, pos.relative(face.getClockWise()), db, face);
      final int lt = LightCoordsUtil.getLightCoords(level, pos);
      final long ms = System.currentTimeMillis();
      final DecorBlock.Kind kind = db.kind;
      final boolean lit = bs.getValue(DecorBlock.LIT);
      final float sway = level.getBlockEntity(pos) instanceof DecorBlockEntity de ? (de.sway *= 0.93F) : 0F;
      final boolean L = right, R = left;          // L: a twin on local +x, R: a twin on local -x
      pose.pushPose();
      pose.translate(0.5F, 0F, 0.5F);
      pose.mulPose(new org.joml.Matrix4f().rotationY((float)Math.toRadians(-(face.toYRot() + 180F))));
      pose.translate(-0.5F, 0F, -0.5F);
      collector.submitCustomGeometry(pose, RenderTypes.entityCutout(FacadeRenderer.WHITE), (p, b) -> draw(p, b, kind, lt, ms, L, R, pos, lit, sway));
      pose.popPose();
   }

   private static boolean same(Level level, BlockPos p, DecorBlock db, Direction face) {
      BlockState s = level.getBlockState(p);
      return s.getBlock() == db && s.getValue(DecorBlock.FACING) == face;
   }

   // ---------------- primitives (two-sided, any angle) ----------------

   static void box(VertexConsumer b, Pose p, double x0, double y0, double z0, double x1, double y1, double z1, int col, int lt) {
      Draw.box(b, p, (float)x0, (float)y0, (float)z0, (float)x1, (float)y1, (float)z1, col, lt);
   }

   static void quad(VertexConsumer b, Pose p, double ax, double ay, double az, double bx, double by, double bz,
                    double cx, double cy, double cz, double dx, double dy, double dz, int col, int lt) {
      float[] pa = {(float)ax, (float)ay, (float)az}, pc = {(float)bx, (float)by, (float)bz}, pd = {(float)cx, (float)cy, (float)cz}, pe = {(float)dx, (float)dy, (float)dz};
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

   static void cyl(VertexConsumer b, Pose p, double cx, double cz, double r, double y0, double y1, int col, int lt, boolean cap) {
      int seg = 14;
      for (int i = 0; i < seg; i++) {
         double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
         double x0 = cx + Math.cos(a0) * r, z0 = cz + Math.sin(a0) * r, x1 = cx + Math.cos(a1) * r, z1 = cz + Math.sin(a1) * r;
         quad(b, p, x0, y0, z0, x1, y0, z1, x1, y1, z1, x0, y1, z0, FacadeRenderer.mul(col, 0.78 + 0.22 * Math.cos((a0 + a1) / 2 - 2.2)), lt);
         if (cap) quad(b, p, cx, y1, cz, x0, y1, z0, x1, y1, z1, cx, y1, cz, col, lt);
      }
   }

   static void globe(VertexConsumer b, Pose p, double x, double y, double z, double r, int col, int lt) {
      int la = 7, lo = 12;
      for (int i = 0; i < la; i++) {
         double f0 = -Math.PI / 2 + Math.PI * i / la, f1 = -Math.PI / 2 + Math.PI * (i + 1) / la;
         double r0 = Math.cos(f0) * r, r1 = Math.cos(f1) * r, y0 = y + Math.sin(f0) * r, y1 = y + Math.sin(f1) * r;
         int c = FacadeRenderer.mul(col, 0.7 + 0.3 * (i + 1) / la);
         for (int k = 0; k < lo; k++) {
            double a0 = Math.PI * 2 * k / lo, a1 = Math.PI * 2 * (k + 1) / lo;
            quad(b, p, x + Math.cos(a0) * r0, y0, z + Math.sin(a0) * r0, x + Math.cos(a1) * r0, y0, z + Math.sin(a1) * r0,
               x + Math.cos(a1) * r1, y1, z + Math.sin(a1) * r1, x + Math.cos(a0) * r1, y1, z + Math.sin(a0) * r1, c, lt);
         }
      }
   }

   // ---------------- the props ----------------

   private static void draw(Pose p, VertexConsumer b, DecorBlock.Kind kind, int lt, long ms, boolean L, boolean R, BlockPos pos, boolean lit, float sway) {
      switch (kind) {
         case BENCH -> bench(b, p, lt, L, R);
         case PLANTER -> planter(b, p, lt);
         case LAMP_POST -> lamp(b, p, lt, lit);
         case SOFA -> sofa(b, p, lt, L, R);
         case COFFEE_TABLE -> table(b, p, lt);
         case TEDDY -> teddy(b, p, lt);
         case DUMPSTER -> dumpster(b, p, lt);
         case DESK -> desk(b, p, lt, ms);
         case CHAIR -> chair(b, p, lt);
         case FILING -> filing(b, p, lt);
         case MONITORS -> monitors(b, p, lt, ms, pos);
         case PANEL -> panel(b, p, lt, ms);
         case COOLER -> cooler(b, p, lt);
         case PALM -> palm(b, p, lt, sway, ms);
         case MEDIA -> media(b, p, lt, L, R);
      }
   }

   private static void bench(VertexConsumer b, Pose p, int lt, boolean L, boolean R) {
      int wood = 0xFF8B5A2B, iron = 0xFF1E1E22;
      double x0 = R ? 0 : 0.04, x1 = L ? 1 : 0.96;
      for (int i = 0; i < 3; i++) box(b, p, x0, 0.44, 0.22 + i * 0.16, x1, 0.48, 0.34 + i * 0.16, FacadeRenderer.mul(wood, 1 - i * 0.05), lt);
      for (int i = 0; i < 3; i++) {
         double y = 0.58 + i * 0.12;
         quad(b, p, x0, y, 0.70 + i * 0.03, x1, y, 0.70 + i * 0.03, x1, y + 0.08, 0.72 + i * 0.03, x0, y + 0.08, 0.72 + i * 0.03, wood, lt);
      }
      for (double x : new double[]{R ? -1 : 0.06, L ? -1 : 0.9}) {
         if (x < 0) continue;
         box(b, p, x, 0, 0.2, x + 0.04, 0.44, 0.26, iron, lt);
         box(b, p, x, 0, 0.66, x + 0.04, 0.95, 0.72, iron, lt);
         box(b, p, x, 0.4, 0.2, x + 0.04, 0.44, 0.72, iron, lt);
         box(b, p, x, 0.62, 0.15, x + 0.04, 0.66, 0.45, iron, lt);
      }
   }

   private static void planter(VertexConsumer b, Pose p, int lt) {
      cyl(b, p, 0.5, 0.5, 0.46, 0, 0.55, 0xFF8E3626, lt, false);
      cyl(b, p, 0.5, 0.5, 0.5, 0.55, 0.62, 0xFFE8DCC0, lt, true);
      cyl(b, p, 0.5, 0.5, 0.42, 0.58, 0.63, 0xFF3A2A1A, lt, true);
      cyl(b, p, 0.5, 0.5, 0.07, 0.63, 2.4, 0xFF5A3E28, lt, false);
      double[][] cl = {{0, 0, 0, 1}, {0.45, -0.2, 0.25, 0.7}, {-0.42, -0.15, -0.3, 0.72}, {0.12, 0.38, -0.35, 0.65}, {-0.25, 0.3, 0.4, 0.6}};
      int[] g = {0xFF3E7A34, 0xFF4C8C3A, 0xFF35682C, 0xFF5A9A44, 0xFF447E36};
      for (int i = 0; i < cl.length; i++) globe(b, p, 0.5 + cl[i][0], 3.0 + cl[i][1], 0.5 + cl[i][2], 0.85 * cl[i][3], g[i], lt);
   }

   private static void lamp(VertexConsumer b, Pose p, int lt, boolean lit) {
      int iron = 0xFF1E2A24;
      cyl(b, p, 0.5, 0.5, 0.16, 0, 0.4, iron, lt, true);
      cyl(b, p, 0.5, 0.5, 0.1, 0.4, 0.55, iron, lt, true);
      cyl(b, p, 0.5, 0.5, 0.06, 0.55, 3.6, iron, lt, false);
      cyl(b, p, 0.5, 0.5, 0.11, 3.6, 3.7, iron, lt, true);
      globe(b, p, 0.5, 4.0, 0.5, 0.28, lit ? 0xFFFFF0C8 : 0xFF8A8678, lit ? FULL : lt);
      cyl(b, p, 0.5, 0.5, 0.09, 4.26, 4.36, iron, lt, true);
      cyl(b, p, 0.5, 0.5, 0.02, 4.36, 4.5, iron, lt, true);
   }

   private static void sofa(VertexConsumer b, Pose p, int lt, boolean L, boolean R) {
      int fab = 0xFF8A2030, cush = 0xFFA02838, leg = 0xFF2A1A10;
      double x0 = R ? 0 : 0.02, x1 = L ? 1 : 0.98;
      box(b, p, x0, 0.08, 0.12, x1, 0.4, 0.92, fab, lt);
      box(b, p, x0, 0.4, 0.68, x1, 0.95, 0.92, fab, lt);
      box(b, p, x0 + 0.03, 0.4, 0.14, x1 - 0.03, 0.55, 0.68, cush, lt);
      box(b, p, x0 + 0.03, 0.55, 0.6, x1 - 0.03, 0.9, 0.7, cush, lt);
      box(b, p, x0 + 0.495, 0.4, 0.13, x0 + 0.505, 0.555, 0.68, FacadeRenderer.mul(cush, 0.7), lt);
      if (!R) { box(b, p, 0, 0.08, 0.1, 0.14, 0.7, 0.94, fab, lt); box(b, p, 0.02, 0, 0.14, 0.08, 0.08, 0.2, leg, lt); box(b, p, 0.02, 0, 0.84, 0.08, 0.08, 0.9, leg, lt); }
      if (!L) { box(b, p, 0.86, 0.08, 0.1, 1, 0.7, 0.94, fab, lt); box(b, p, 0.92, 0, 0.14, 0.98, 0.08, 0.2, leg, lt); box(b, p, 0.92, 0, 0.84, 0.98, 0.08, 0.9, leg, lt); }
   }

   private static void table(VertexConsumer b, Pose p, int lt) {
      int wood = 0xFF5A3A22;
      box(b, p, 0.1, 0.38, 0.2, 0.9, 0.44, 0.8, wood, lt);
      for (double[] l : new double[][]{{0.14, 0.24}, {0.82, 0.24}, {0.14, 0.72}, {0.82, 0.72}}) box(b, p, l[0], 0, l[1], l[0] + 0.04, 0.38, l[1] + 0.04, wood, lt);
      box(b, p, 0.14, 0.08, 0.24, 0.86, 0.1, 0.76, wood, lt);
      box(b, p, 0.25, 0.44, 0.35, 0.5, 0.46, 0.55, 0xFFD04040, lt);
      box(b, p, 0.3, 0.46, 0.38, 0.52, 0.475, 0.56, 0xFF3070D0, lt);
      box(b, p, 0.6, 0.44, 0.45, 0.78, 0.47, 0.6, 0xFF202024, lt);
   }

   private static void teddy(VertexConsumer b, Pose p, int lt) {
      int c = 0xFFB07A48, m = 0xFFE8D0B0;
      globe(b, p, 0.5, 0.45, 0.55, 0.4, c, lt);
      globe(b, p, 0.5, 1.05, 0.55, 0.3, c, lt);
      globe(b, p, 0.25, 1.3, 0.58, 0.1, c, lt);
      globe(b, p, 0.75, 1.3, 0.58, 0.1, c, lt);
      globe(b, p, 0.5, 0.98, 0.28, 0.11, m, lt);
      globe(b, p, 0.5, 1.0, 0.18, 0.035, 0xFF141414, lt);
      globe(b, p, 0.4, 1.12, 0.29, 0.03, 0xFF141414, lt);
      globe(b, p, 0.6, 1.12, 0.29, 0.03, 0xFF141414, lt);
      globe(b, p, 0.15, 0.55, 0.45, 0.14, c, lt);
      globe(b, p, 0.85, 0.55, 0.45, 0.14, c, lt);
      globe(b, p, 0.3, 0.12, 0.3, 0.15, c, lt);
      globe(b, p, 0.7, 0.12, 0.3, 0.15, c, lt);
      box(b, p, 0.38, 0.8, 0.27, 0.62, 0.86, 0.33, 0xFFD02040, lt);
   }

   private static void dumpster(VertexConsumer b, Pose p, int lt) {
      int g = 0xFF2E5A34, d = 0xFF1E3A22;
      quad(b, p, -0.3, 0.12, 0.08, 1.3, 0.12, 0.08, 1.3, 1.25, 0.0, -0.3, 1.25, 0.0, g, lt);
      box(b, p, -0.3, 0.12, 0.08, 1.3, 1.25, 0.95, g, lt);
      quad(b, p, -0.32, 1.25, -0.02, 1.32, 1.25, -0.02, 1.32, 1.4, 0.97, -0.32, 1.4, 0.97, d, lt);
      for (double x : new double[]{0.1, 0.5, 0.9}) box(b, p, x, 0.12, 0.05, x + 0.04, 1.2, 0.08, d, lt);
      for (double x : new double[]{-0.2, 1.1}) for (double z : new double[]{0.15, 0.85}) cyl(b, p, x, z, 0.08, 0, 0.12, 0xFF141414, lt, true);
   }

   private static void desk(VertexConsumer b, Pose p, int lt, long ms) {
      int wood = 0xFF4A2C18, top = 0xFF5E3A20, metal = 0xFFB8BCC0;
      box(b, p, -0.6, 0.74, 0.08, 1.6, 0.8, 0.92, top, lt);
      box(b, p, -0.6, 0.06, 0.1, -0.1, 0.74, 0.9, wood, lt);
      box(b, p, 1.1, 0.06, 0.1, 1.6, 0.74, 0.9, wood, lt);
      box(b, p, -0.1, 0.25, 0.84, 1.1, 0.74, 0.9, wood, lt);
      for (int i = 0; i < 3; i++) {
         double y = 0.12 + i * 0.2;
         box(b, p, 1.12, y, 0.07, 1.58, y + 0.17, 0.1, FacadeRenderer.mul(wood, 1.15), lt);
         box(b, p, 1.3, y + 0.07, 0.05, 1.4, y + 0.1, 0.07, metal, lt);
      }
      // CRT monitor, keyboard, phone, lamp, papers, nameplate
      box(b, p, 0.15, 0.8, 0.5, 0.85, 1.32, 0.88, 0xFFD8D0B8, lt);
      box(b, p, 0.2, 0.86, 0.48, 0.8, 1.28, 0.5, 0xFF1A2A30, lt);
      box(b, p, 0.24, 0.9, 0.475, 0.76, 1.24, 0.48, 0xFF2060A0, FULL);
      box(b, p, 0.4, 0.8, 0.88, 0.6, 0.86, 0.92, 0xFFD8D0B8, lt);
      box(b, p, 0.2, 0.8, 0.18, 0.8, 0.83, 0.4, 0xFFE0D8C4, lt);
      box(b, p, -0.45, 0.8, 0.3, -0.15, 0.88, 0.5, 0xFF202024, lt);
      cyl(b, p, 1.4, 0.7, 0.08, 0.8, 0.84, 0xFF2A5A2A, lt, true);
      cyl(b, p, 1.4, 0.7, 0.015, 0.84, 1.2, 0xFFC8A040, lt, false);
      quad(b, p, 1.3, 1.2, 0.6, 1.5, 1.2, 0.6, 1.47, 1.3, 0.72, 1.33, 1.3, 0.72, 0xFF2A7A3A, lt);
      box(b, p, 0.95, 0.8, 0.25, 1.35, 0.81, 0.55, 0xFFF4F0E4, lt);
      box(b, p, 1.0, 0.81, 0.28, 1.38, 0.815, 0.57, 0xFFF8F6F0, lt);
      box(b, p, -0.1, 0.8, 0.1, 0.25, 0.9, 0.14, 0xFFC8A040, lt);
   }

   private static void chair(VertexConsumer b, Pose p, int lt) {
      int blk = 0xFF1A1A1C;
      for (int i = 0; i < 5; i++) {
         double a = Math.PI * 2 * i / 5;
         quad(b, p, 0.5, 0.06, 0.5, 0.5 + Math.cos(a) * 0.38, 0.03, 0.5 + Math.sin(a) * 0.38, 0.5 + Math.cos(a + 0.12) * 0.38, 0.06, 0.5 + Math.sin(a + 0.12) * 0.38, 0.5, 0.09, 0.5, blk, lt);
         cyl(b, p, 0.5 + Math.cos(a) * 0.36, 0.5 + Math.sin(a) * 0.36, 0.035, 0, 0.04, 0xFF303034, lt, true);
      }
      cyl(b, p, 0.5, 0.5, 0.035, 0.06, 0.45, 0xFFB8BCC0, lt, false);
      box(b, p, 0.2, 0.45, 0.2, 0.8, 0.55, 0.78, blk, lt);
      box(b, p, 0.22, 0.55, 0.74, 0.78, 1.25, 0.82, blk, lt);
      box(b, p, 0.15, 0.6, 0.3, 0.2, 0.68, 0.7, 0xFF303034, lt);
      box(b, p, 0.8, 0.6, 0.3, 0.85, 0.68, 0.7, 0xFF303034, lt);
   }

   private static void filing(VertexConsumer b, Pose p, int lt) {
      int gr = 0xFF8A8E92;
      box(b, p, 0.08, 0, 0.15, 0.92, 1.35, 0.88, gr, lt);
      for (int i = 0; i < 4; i++) {
         double y = 0.06 + i * 0.32;
         box(b, p, 0.12, y, 0.12, 0.88, y + 0.28, 0.15, FacadeRenderer.mul(gr, 1.1), lt);
         box(b, p, 0.42, y + 0.18, 0.1, 0.58, y + 0.21, 0.12, 0xFFD0D4D8, lt);
         box(b, p, 0.45, y + 0.08, 0.115, 0.55, y + 0.14, 0.12, 0xFFF0EEE0, lt);
      }
      box(b, p, 0.2, 1.35, 0.3, 0.6, 1.5, 0.6, 0xFFC8A060, lt);
   }

   private static void monitors(VertexConsumer b, Pose p, int lt, long ms, BlockPos pos) {
      int beige = 0xFFC8C0A8;
      box(b, p, 0.1, 0, 0.2, 0.9, 0.7, 0.85, 0xFF303034, lt);
      for (int r = 0; r < 2; r++) for (int c = 0; c < 2; c++) {
         double x = 0.06 + c * 0.45, y = 0.7 + r * 0.42;
         box(b, p, x, y, 0.25, x + 0.43, y + 0.4, 0.82, beige, lt);
         long t = ms / 180 + r * 3 + c * 7 + pos.asLong();
         int shade = 0x50 + (int)(Math.abs(t * 2654435761L) % 0x40);
         int col = 0xFF000000 | shade << 16 | (shade + 0x18) << 8 | shade;
         box(b, p, x + 0.04, y + 0.05, 0.235, x + 0.39, y + 0.35, 0.25, col, FULL);
         box(b, p, x + 0.04, y + 0.05 + ((ms / 30 + r * 50 + c * 20) % 30) / 100.0, 0.232, x + 0.39, y + 0.07 + ((ms / 30 + r * 50 + c * 20) % 30) / 100.0, 0.235, 0xFFB0FFB0, FULL);
      }
      box(b, p, 0.3, 0.7, 0.1, 0.7, 0.75, 0.2, 0xFF202024, lt);
   }

   private static void panel(VertexConsumer b, Pose p, int lt, long ms) {
      int gr = 0xFF9A9EA2;
      box(b, p, 0.08, 0.4, 0.78, 0.92, 1.9, 1.0, gr, lt);
      box(b, p, 0.1, 0.42, 0.76, 0.9, 1.88, 0.78, FacadeRenderer.mul(gr, 1.08), lt);
      for (int r = 0; r < 6; r++) for (int c = 0; c < 2; c++) {
         double x = 0.22 + c * 0.32, y = 0.6 + r * 0.2;
         box(b, p, x, y, 0.745, x + 0.24, y + 0.1, 0.76, 0xFF202024, lt);
         box(b, p, x + 0.02 + ((r + c) % 3 == 0 ? 0.12 : 0), y + 0.02, 0.735, x + 0.1 + ((r + c) % 3 == 0 ? 0.12 : 0), y + 0.08, 0.745, 0xFFE8E8E8, lt);
      }
      box(b, p, 0.3, 1.7, 0.745, 0.7, 1.82, 0.76, 0xFFFFD020, lt);
      cyl(b, p, 0.3, 0.92, 0.04, 1.9, 3.2, 0xFF8A8E92, lt, false);
      cyl(b, p, 0.7, 0.92, 0.04, 1.9, 3.2, 0xFF8A8E92, lt, false);
      boolean on = (ms / 700) % 2 == 0;
      box(b, p, 0.8, 1.6, 0.74, 0.86, 1.66, 0.76, on ? 0xFF40FF60 : 0xFF1C3A20, on ? FULL : lt);
   }

   /** Entertainment centre under a TV: wide cabinet, glass doors with game cases, open shelf, drawers. Joins with its neighbours. */
   private static void media(VertexConsumer b, Pose p, int lt, boolean L, boolean R) {
      int wood = 0xFF2E2018, face = 0xFF3C2A1E;
      double x0 = R ? 0 : -0.45, x1 = L ? 1 : 1.45;
      box(b, p, x0, 0.04, 0.1, x1, 0.98, 0.92, wood, lt);
      box(b, p, x0 - 0.02, 0.96, 0.08, x1 + 0.02, 1.0, 0.94, 0xFF1E140E, lt);
      box(b, p, x0 - 0.01, 0.0, 0.12, x1 + 0.01, 0.06, 0.9, 0xFF141414, lt);
      // open middle shelf for the console, dark inside
      box(b, p, 0.08, 0.5, 0.08, 0.92, 0.9, 0.12, 0xFF120C08, lt);
      box(b, p, 0.08, 0.5, 0.08, 0.92, 0.52, 0.5, face, lt);
      // drawer row
      for (int i = 0; i < 2; i++) {
         double dx = 0.08 + i * 0.43;
         box(b, p, dx, 0.12, 0.07, dx + 0.41, 0.46, 0.1, face, lt);
         box(b, p, dx + 0.15, 0.28, 0.05, dx + 0.26, 0.31, 0.07, 0xFFC8A040, lt);
      }
      // glass side doors with game cases behind
      for (double[] side : new double[][]{{x0 + 0.05, -0.05}, {1.05, x1 - 0.05}}) {
         if (side[1] - side[0] < 0.2) continue;
         box(b, p, side[0], 0.12, 0.12, side[1], 0.9, 0.16, 0xFF0E0A08, lt);
         int[] cases = {0xFF2050C0, 0xFFC02020, 0xFF20A040, 0xFF202020, 0xFFE0C020, 0xFF6040C0};
         for (int r = 0; r < 2; r++) for (double x = side[0] + 0.03; x < side[1] - 0.05; x += 0.07) {
            int c = cases[Math.floorMod((int)(x * 37) + r * 3, cases.length)];
            box(b, p, x, 0.16 + r * 0.38, 0.18, x + 0.05, 0.48 + r * 0.38, 0.5, c, lt);
         }
         box(b, p, side[0], 0.12, 0.09, side[1], 0.15, 0.11, 0xFF8A8E92, lt);
         box(b, p, side[0], 0.87, 0.09, side[1], 0.9, 0.11, 0xFF8A8E92, lt);
         box(b, p, side[1] - 0.06, 0.45, 0.07, side[1] - 0.04, 0.6, 0.1, 0xFFC8A040, lt);
      }
   }

   private static void cooler(VertexConsumer b, Pose p, int lt) {
      box(b, p, 0.25, 0, 0.25, 0.75, 0.95, 0.75, 0xFFE8E6E0, lt);
      box(b, p, 0.32, 0.55, 0.23, 0.42, 0.62, 0.25, 0xFF3070D0, lt);
      box(b, p, 0.58, 0.55, 0.23, 0.68, 0.62, 0.25, 0xFFD03030, lt);
      cyl(b, p, 0.5, 0.5, 0.2, 0.95, 1.45, 0x9A60A8E8, lt, true);
      cyl(b, p, 0.5, 0.5, 0.06, 1.45, 1.52, 0xFF4080D0, lt, true);
   }

   private static void palm(VertexConsumer b, Pose p, int lt, float sway, long ms) {
      double sw = Math.sin(ms / 90.0) * 0.12 * sway;
      cyl(b, p, 0.5, 0.5, 0.3, 0, 0.5, 0xFFC86A3A, lt, false);
      cyl(b, p, 0.5, 0.5, 0.33, 0.5, 0.56, 0xFFB05A30, lt, true);
      cyl(b, p, 0.5, 0.5, 0.27, 0.5, 0.57, 0xFF3A2A1A, lt, true);
      cyl(b, p, 0.5, 0.5, 0.05, 0.57, 1.7, 0xFF7A5A34, lt, false);
      for (int i = 0; i < 9; i++) {
         double a = Math.PI * 2 * i / 9 + 0.3 + sw, ca = Math.cos(a), sa = Math.sin(a), px = -sa * 0.12, pz = ca * 0.12;
         double mx = 0.5 + ca * 0.45, mz = 0.5 + sa * 0.45, ex = 0.5 + ca * 0.85, ez = 0.5 + sa * 0.85;
         int g = i % 2 == 0 ? 0xFF3E8A34 : 0xFF2E7028;
         quad(b, p, 0.5, 1.7, 0.5, mx + px, 1.85, mz + pz, mx - px, 1.85, mz - pz, 0.5, 1.7, 0.5, g, lt);
         quad(b, p, mx + px, 1.85, mz + pz, ex, 1.45, ez, ex, 1.45, ez, mx - px, 1.85, mz - pz, g, lt);
      }
   }
}
