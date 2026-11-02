package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.ArcadePlusMod;
import com.dustin.arcadeplus.PrizeBoothBlock;
import com.dustin.arcadeplus.PrizeDisplayBlockEntity;
import com.dustin.arcadeplus.Prizes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The redemption counter (wood base, red laminate, glass case with small prizes turning inside) and
 * the prize wall (pegboard with shelves showing the real prizes, cheapest on top, consoles on the bottom shelf).
 */
public class PrizeDisplayRenderer implements BlockEntityRenderer<PrizeDisplayBlockEntity, BlockEntityRenderState> {
   private static final Identifier WHITE = ArcadePlusMod.id("textures/misc/white.png");

   public PrizeDisplayRenderer(BlockEntityRendererProvider.Context context) {
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
      if (RenderCull.far(state)) return;
      Minecraft mc = Minecraft.getInstance();
      Level level = mc.level;
      if (level == null || state.blockPos == null) return;
      BlockPos pos = state.blockPos;
      BlockState bs = level.getBlockState(pos);
      if (!(bs.getBlock() instanceof PrizeBoothBlock pb)) return;
      Direction face = bs.getValue(PrizeBoothBlock.FACING);
      boolean left = same(level, pos.relative(face.getClockWise()), pb, face), right = same(level, pos.relative(face.getCounterClockWise()), pb, face);
      final int lt = LightCoordsUtil.getLightCoords(level, pos);
      final long ms = System.currentTimeMillis();
      pose.pushPose();
      pose.translate(0.5F, 0F, 0.5F);
      pose.mulPose(new org.joml.Matrix4f().rotationY((float)Math.toRadians(-(face.toYRot() + 180F))));
      pose.translate(-0.5F, 0F, -0.5F);
      List<Prizes.Prize> all = Prizes.display();
      if (pb.wall) {
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) -> wall(p, b, lt));
         List<Prizes.Prize> sorted = new ArrayList<>(all);
         sorted.sort((a, c) -> Integer.compare(a.cost(), c.cost()));
         int per = 7;
         for (int i = 0; i < Math.min(sorted.size(), per * 3); i++) {
            int shelf = 2 - i / per, slot = i % per;                    // cheapest on the top shelf
            double x = -0.85 + slot * (2.7 / (per - 1)), y = 0.42 + shelf * 0.95;
            float sc = shelf == 0 ? 0.55F : 0.42F;
            int pick = sorted.size() <= per * 3 ? i : (int)Math.round(i * (sorted.size() - 1) / (double)(per * 3 - 1));   // a spread from candy to consoles
            item(mc, level, pose, collector, sorted.get(pick).stack(), x, y + sc * 0.5, 0.62, sc, (float)Math.sin(ms / 1200.0 + i) * 12F, lt);
         }
      } else {
         final boolean L = left, R = right;
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) -> counter(p, b, lt, L, R));
         collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(WHITE), (p, b) -> glass(p, b, lt, L, R));
         int base = Math.floorMod((int)(pos.getX() * 7 + pos.getZ() * 13), Math.max(1, all.size()));
         for (int k = 0; k < 3 && !all.isEmpty(); k++) {
            Prizes.Prize pr = all.get(Math.min(all.size() - 1, (base + k) % Math.min(all.size(), 8)));   // small prizes in the case
            item(mc, level, pose, collector, pr.stack(), 0.2 + k * 0.3, 0.68, 0.5, 0.26F, ms / 25F % 360F + k * 120, lt);
         }
      }
      pose.popPose();
   }

   private static boolean same(Level level, BlockPos p, PrizeBoothBlock pb, Direction face) {
      BlockState s = level.getBlockState(p);
      return s.getBlock() == pb && s.getValue(PrizeBoothBlock.FACING) == face;
   }

   static void item(Minecraft mc, Level level, PoseStack pose, SubmitNodeCollector collector, ItemStack stack, double x, double y, double z, float scale, float spin, int lt) {
      if (stack.isEmpty()) return;
      ItemStackRenderState st = new ItemStackRenderState();
      mc.getItemModelResolver().updateForTopItem(st, stack, ItemDisplayContext.FIXED, level, null, 0);
      pose.pushPose();
      pose.translate((float)x, (float)y, (float)z);
      pose.mulPose(new org.joml.Matrix4f().rotationY((float)Math.toRadians(180 + spin)));
      pose.scale(scale, scale, scale);
      st.submit(pose, collector, lt, OverlayTexture.NO_OVERLAY, 0);
      pose.popPose();
   }

   private static void box(VertexConsumer b, Pose p, double x0, double y0, double z0, double x1, double y1, double z1, int col, int lt) {
      Draw.box(b, p, (float)x0, (float)y0, (float)z0, (float)x1, (float)y1, (float)z1, col, lt);
   }

   private static void counter(Pose p, VertexConsumer b, int lt, boolean L, boolean R) {
      double x0 = R ? 0 : 0.03, x1 = L ? 1 : 0.97;
      int wood = 0xFF3A2416, red = 0xFFC01818, alu = 0xFFC8CCD2;
      box(b, p, x0, 0.0, 0.12, x1, 0.06, 0.95, 0xFF141414, lt);                     // toe kick
      box(b, p, x0, 0.06, 0.08, x1, 0.55, 0.95, wood, lt);                          // cabinet
      box(b, p, x0, 0.18, 0.06, x1, 0.42, 0.08, red, lt);                           // red laminate band
      box(b, p, x0, 0.43, 0.065, x1, 0.45, 0.08, alu, lt);
      box(b, p, x0, 0.55, 0.05, x1, 0.58, 0.97, alu, lt);                           // shelf deck of the case
      box(b, p, x0 + 0.02, 0.58, 0.1, x1 - 0.02, 0.6, 0.92, 0xFFF0EAD8, lt);         // white felt
      box(b, p, x0, 0.92, 0.05, x1, 0.95, 0.97, alu, lt);                           // top rim
      if (!R) box(b, p, 0.03, 0.58, 0.05, 0.06, 0.92, 0.97, alu, lt);
      if (!L) box(b, p, 0.94, 0.58, 0.05, 0.97, 0.92, 0.97, alu, lt);
      box(b, p, x0, 0.58, 0.9, x1, 0.92, 0.97, 0xFF2A1A10, lt);                      // staff side panel
   }

   private static void glass(Pose p, VertexConsumer b, int lt, boolean L, boolean R) {
      double x0 = R ? 0 : 0.06, x1 = L ? 1 : 0.94;
      int g = 0x4099C8E8;
      // front pane only: the side and top panes sat inside the aluminium frame, face to face with it (z-fighting)
      box(b, p, x0, 0.58, 0.05, x1, 0.92, 0.06, g, lt);
   }

   private static void wall(Pose p, VertexConsumer b, int lt) {
      int peg = 0xFFE8E0D0, frame = 0xFF3A2416, shelf = 0xFF5E3A20, tag = 0xFFFFE070;
      box(b, p, -1.05, 0.0, 0.92, 2.05, 3.05, 1.0, peg, lt);                         // pegboard
      for (double y = 0.15; y < 3.0; y += 0.2) for (double x = -0.95; x < 2.0; x += 0.2) {
         box(b, p, x, y, 0.915, x + 0.025, y + 0.025, 0.92, 0xFFB8B0A0, lt);          // peg holes
      }
      box(b, p, -1.12, 0.0, 0.9, -1.05, 3.12, 1.0, frame, lt);
      box(b, p, 2.05, 0.0, 0.9, 2.12, 3.12, 1.0, frame, lt);
      box(b, p, -1.12, 3.05, 0.9, 2.12, 3.15, 1.0, frame, lt);
      for (int s = 0; s < 3; s++) {
         double y = 0.38 + s * 0.95;
         box(b, p, -1.0, y - 0.05, 0.45, 2.0, y, 0.92, shelf, lt);
         box(b, p, -1.0, y - 0.1, 0.43, 2.0, y - 0.05, 0.46, s == 0 ? 0xFFFFD040 : s == 1 ? 0xFF40E8FF : 0xFFFF3AC8, lt);   // price-tier stripe
         for (int i = 0; i < 7; i++) {
            double x = -0.85 + i * (2.7 / 6);
            box(b, p, x - 0.08, y - 0.1, 0.425, x + 0.08, y - 0.04, 0.43, tag, lt);    // price tag
         }
      }
   }
}
