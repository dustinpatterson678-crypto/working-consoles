package com.dustin.stream.client;

import com.dustin.stream.DeskBlock;
import com.dustin.stream.DeskBlockEntity;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Keyboard keys press down as you really type, and the mouse slides and clicks with your real mouse. */
public class DeskRenderer implements BlockEntityRenderer<DeskBlockEntity, BlockEntityRenderState> {
   private static final int COLS = 14;
   private static final int ROWS = 4;
   private static final int ACCENT = 0xFF5CC8FF;

   public DeskRenderer(BlockEntityRendererProvider.Context context) {
   }

   @Override
   public BlockEntityRenderState createRenderState() {
      return new BlockEntityRenderState();
   }

   @Override
   public void submit(BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      Minecraft mc = Minecraft.getInstance();
      Level level = mc.level;
      if (level == null || state.blockPos == null) {
         return;
      }
      BlockState bs = level.getBlockState(state.blockPos);
      if (!(bs.getBlock() instanceof DeskBlock desk)) {
         return;
      }
      boolean animate = InputFx.active() && mc.player != null && mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(state.blockPos)) < 10.0 * 10.0;
      long now = System.currentTimeMillis();
      int light = state.lightCoords;
      Direction facing = bs.getValue(DeskBlock.FACING);
      pose.pushPose();
      pose.translate(0.5, 0.0, 0.5);
      pose.rotate(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
      if (desk.kind == DeskBlock.Kind.KEYBOARD) {
         boolean keyFresh = animate && now - InputFx.lastKeyAt < 130L;
         int pressedCell = Math.floorMod(InputFx.lastKey * 31, COLS * ROWS);
         boolean spacePressed = keyFresh && InputFx.lastKey == InputConstants.KEY_SPACE;
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(MediaRenderer.WHITE), (p, b) -> {
            Draw.box(b, p, -0.43F, 0.0F, -0.19F, 0.43F, 0.04F, 0.19F, 0xFF1E1E22, light);
            float kw = 0.052F;
            float gap = 0.008F;
            for (int r = 0; r < ROWS; r++) {
               for (int col = 0; col < COLS; col++) {
                  boolean down = keyFresh && !spacePressed && r * COLS + col == pressedCell;
                  float x1 = 0.41F - col * (kw + gap);
                  float z0 = -0.09F + r * (kw + gap);
                  Draw.box(b, p, x1 - kw, 0.04F, z0, x1, down ? 0.048F : 0.062F, z0 + kw, down ? ACCENT : 0xFF3A3A40, down ? 15728880 : light);
               }
            }
            Draw.box(b, p, -0.17F, 0.04F, -0.165F, 0.17F, spacePressed ? 0.048F : 0.062F, -0.11F,
               spacePressed ? ACCENT : 0xFF3A3A40, spacePressed ? 15728880 : light);
         });
      } else {
         float cx = animate ? -(InputFx.cursorX - 0.5F) * 0.36F : 0.0F;
         float cz = animate ? -(InputFx.cursorY - 0.5F) * 0.28F : 0.0F;
         boolean click = animate && now - InputFx.lastClickAt < 150L;
         boolean leftDown = click && InputFx.lastButton == 0;
         boolean rightDown = click && InputFx.lastButton == 1;
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(MediaRenderer.WHITE), (p, b) -> {
            Draw.box(b, p, -0.32F, 0.0F, -0.25F, 0.32F, 0.01F, 0.25F, 0xFF141418, light);                       // mouse pad
            Draw.box(b, p, cx - 0.06F, 0.01F, cz - 0.10F, cx + 0.06F, 0.055F, cz + 0.02F, 0xFFE6E6EA, light);   // palm
            Draw.box(b, p, cx, 0.01F, cz + 0.02F, cx + 0.06F, leftDown ? 0.05F : 0.06F, cz + 0.10F,
               leftDown ? ACCENT : 0xFFDCDCE2, leftDown ? 15728880 : light);                                        // left button
            Draw.box(b, p, cx - 0.06F, 0.01F, cz + 0.02F, cx, rightDown ? 0.05F : 0.06F, cz + 0.10F,
               rightDown ? ACCENT : 0xFFDCDCE2, rightDown ? 15728880 : light);                                      // right button
            Draw.box(b, p, cx - 0.008F, 0.06F, cz + 0.04F, cx + 0.008F, 0.068F, cz + 0.08F, 0xFF2A2A30, light);  // wheel
         });
      }
      pose.popPose();
   }
}
