package com.dustin.wcore.client;

import com.dustin.wcore.ConsoleSettings;
import com.dustin.wcore.block.ConsoleBlockEntity;
import com.dustin.wcore.block.PowerGrade;
import com.dustin.wcore.block.PowerMonitorBlock;
import com.dustin.wcore.block.PowerMonitorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PowerMonitorRenderer implements BlockEntityRenderer<PowerMonitorBlockEntity, BlockEntityRenderState> {
   private static final float SCALE = 0.0072F;
   private static final int LINE = 10;
   private final Font font;

   public PowerMonitorRenderer(Context context) {
      this.font = context.font();
   }

   public BlockEntityRenderState createRenderState() {
      return new BlockEntityRenderState();
   }

   public void submit(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      Level level = Minecraft.getInstance().level;
      if (level != null && state.blockPos != null && level.getBlockEntity(state.blockPos) instanceof PowerMonitorBlockEntity monitor) {
         BlockState bs = level.getBlockState(state.blockPos);
         if (bs.getBlock() instanceof PowerMonitorBlock) {
            Direction facing = (Direction)bs.getValue(PowerMonitorBlock.FACING);
            List<PowerMonitorRenderer.Line> lines = lines(monitor);
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.rotateDegrees(Axis.YP, -facing.toYRot());
            poseStack.translate(0.0, 0.0, -0.244);
            poseStack.scale(0.0072F, -0.0072F, 0.0072F);
            int top = -(lines.size() * 10) / 2;

            for (int i = 0; i < lines.size(); i++) {
               PowerMonitorRenderer.Line l = lines.get(i);
               collector.submitText(
                  poseStack,
                  -56.0F,
                  top + i * 10,
                  Component.literal(l.text()).getVisualOrderText(),
                  false,
                  DisplayMode.POLYGON_OFFSET,
                  15728880,
                  l.color(),
                  0,
                  0
               );
            }

            poseStack.popPose();
         }
      }
   }

   private static List<PowerMonitorRenderer.Line> lines(PowerMonitorBlockEntity monitor) {
      List<PowerMonitorRenderer.Line> out = new ArrayList<>();
      out.add(new PowerMonitorRenderer.Line("POWER MONITOR", -1));
      ConsoleBlockEntity c = monitor.console();
      if (c == null) {
         out.add(new PowerMonitorRenderer.Line("No console within 8 blocks", -7829368));
         return out;
      }

      PowerGrade g = c.grade();
      int sig = c.isPowered() ? c.signal() : 0;
      out.add(new PowerMonitorRenderer.Line(c.consoleName() + (c.isOn() ? "  ON" : "  off"), -2236963));
      out.add(
         new PowerMonitorRenderer.Line("Signal " + bar(sig) + " " + sig + "/15", sig >= 15 ? -11141291 : (sig >= 8 ? -171 : (sig > 0 ? -22016 : -7829368)))
      );
      out.add(new PowerMonitorRenderer.Line("Grade  " + g.label + (ConsoleSettings.get().redstonePower ? "" : " (power off in settings)"), g.color));
      long steady = c.stableSeconds();
      String next = g == PowerGrade.CLEAN ? "  OC in " + Math.max(0L, 60L - steady) + "s" : "";
      out.add(new PowerMonitorRenderer.Line("Steady " + time(steady) + "  flickers " + c.flickers() + "/3" + next, c.flickers() >= 2 ? -43691 : -5592406));
      ConsoleStats.Entry e = ConsoleStats.get(c.getBlockPos());
      if (e == null) {
         out.add(new PowerMonitorRenderer.Line("Core   not running", -7829368));
      } else {
         double fps = e.emu().fps();
         out.add(new PowerMonitorRenderer.Line(String.format("Core   %.1f FPS", fps), fps >= 58.0 ? -11141291 : (fps >= 50.0 ? -171 : -43691)));
         out.add(
            new PowerMonitorRenderer.Line(
               String.format("Sound  %.0f/%.0f ms  gaps %d", e.emu().soundMs(), e.emu().targetMs(), e.emu().soundGaps()),
               e.emu().soundGaps() > 0 ? -22016 : -5592406
            )
         );
         out.add(new PowerMonitorRenderer.Line("Late   " + e.emu().lateFrames() + " frames/s", e.emu().lateFrames() > 0 ? -22016 : -5592406));
         out.add(new PowerMonitorRenderer.Line("Boost  " + e.boost(), g.color));
      }

      out.add(new PowerMonitorRenderer.Line("Comparator out: " + g.comparator, -43691));
      return out;
   }

   private static String bar(int sig) {
      StringBuilder b = new StringBuilder("[");

      for (int i = 1; i <= 15; i += 2) {
         b.append((char)(sig >= i ? '|' : '.'));
      }

      return b.append(']').toString();
   }

   private static String time(long s) {
      return s >= 3600L ? s / 3600L + "h" + s % 3600L / 60L + "m" : (s >= 60L ? s / 60L + "m" + s % 60L + "s" : s + "s");
   }

   private record Line(String text, int color) {
   }
}
