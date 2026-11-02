package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.ArcadePlusMod;
import com.dustin.wcore.retro.RetroConsoleBlock;
import com.dustin.wcore.retro.RetroConsoleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A Dustin's Arcade cabinet: the realistic shell from its OBJ model, the live game picture and the theme kit from the cabinet renderer. */
public class ObjCabinetRenderer extends ThemedCabinetRenderer {
   private final String name;

   public ObjCabinetRenderer(Context context, String name, BuiltInScreen screen, Theme theme) {
      super(context, ArcadePlusMod.id(name), screen, theme);
      this.name = name;
   }

   @Override
   public void submit(BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      if (RenderCull.far(state)) return;
      super.submit(state, pose, collector, camera);
      shell(this.name, state, pose, collector);
   }

   /** Draw a cabinet's OBJ shell: body, LEDs and marquee (lit while powered), then the CRT glass over the picture. */
   public static void shell(String name, BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector) {
      Level level = Minecraft.getInstance().level;
      if (level == null || state.blockPos == null || !(level.getBlockEntity(state.blockPos) instanceof RetroConsoleBlockEntity be)) return;
      BlockState bs = level.getBlockState(state.blockPos);
      if (!(bs.getBlock() instanceof RetroConsoleBlock)) return;
      Direction facing = bs.getValue(RetroConsoleBlock.FACING);
      boolean on = be.isPowered();
      int light = state.lightCoords, bright = on ? 15728880 : light;
      pose.pushPose();
      pose.translate(0.5, 0.0, 0.5);
      pose.rotate(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
      ObjModel m = ObjModel.get(name);
      m.draw(pose, collector, "static", light, 0xFFFFFFFF);
      m.draw(pose, collector, "leds", bright, on ? 0xFFFFFFFF : 0xFF203038);
      m.draw(pose, collector, "marquee", bright, on ? 0xFFFFFFFF : 0xFF707070);
      if (!com.dustin.wcore.client.ShaderCompat.shadersActive()) {
         m.draw(pose, collector, "glass", light, 0xFFFFFFFF);   // shader packs sort see-through glass against the picture and flicker
      }
      pose.popPose();
   }
}
