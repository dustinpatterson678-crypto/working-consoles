package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.PrizeStandBlock;
import com.dustin.arcadeplus.PrizeStandBlockEntity;
import com.dustin.arcadeplus.Prizes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The walk-up prize booth, with the real prizes on its shelves (cheap at the top, consoles at the bottom) and in the glass case. */
public class PrizeStandRenderer implements BlockEntityRenderer<PrizeStandBlockEntity, BlockEntityRenderState> {
   private static final int FULL = 15728880;

   public PrizeStandRenderer(BlockEntityRendererProvider.Context context) {
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
      BlockState bs = level.getBlockState(state.blockPos);
      if (!(bs.getBlock() instanceof PrizeStandBlock)) return;
      Direction face = bs.getValue(PrizeStandBlock.FACING);
      int light = LightCoordsUtil.getLightCoords(level, state.blockPos.relative(face));
      long ms = System.currentTimeMillis();
      pose.pushPose();
      pose.translate(0.5, 0.0, 0.5);
      pose.rotate(Axis.YP.rotationDegrees(180.0F - face.toYRot()));
      ObjModel m = ObjModel.get("prize_stand");
      m.draw(pose, collector, "static", light, 0xFFFFFFFF);
      m.draw(pose, collector, "leds", FULL, 0xFFFFFFFF);
      m.draw(pose, collector, "marquee", FULL, 0xFFFFFFFF);
      List<Prizes.Prize> sorted = new ArrayList<>(Prizes.display());
      sorted.sort((a, c) -> Integer.compare(a.cost(), c.cost()));
      int per = 8, shelves = 4, slots = per * shelves;
      for (int i = 0; i < Math.min(slots, sorted.size()); i++) {
         int pick = sorted.size() <= slots ? i : (int)Math.round(i * (sorted.size() - 1) / (double)(slots - 1));
         int shelf = shelves - 1 - i / per, k = i % per;                   // cheapest on the top shelf
         double x = 2.9 - k * (5.8 / (per - 1)), y = 0.35 + shelf * 0.8;
         float sc = shelf == 0 ? 0.42F : 0.34F;
         PrizeDisplayRenderer.item(mc, level, pose, collector, sorted.get(pick).stack(), x, y + sc * 0.5, 2.72, sc, (float)Math.sin(ms / 1400.0 + i) * 15F, light);
      }
      for (int k = 0; k < Math.min(7, sorted.size()); k++) {                 // small prizes turning in the glass case
         PrizeDisplayRenderer.item(mc, level, pose, collector, sorted.get(k).stack(), 2.6 - k * 0.87, 0.82, 0.0, 0.24F, ms / 25F % 360F + k * 50, light);
      }
      m.draw(pose, collector, "glass", light, 0xFFFFFFFF);
      pose.popPose();
   }
}
