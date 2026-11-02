package com.dustin.arcadeplus.client;

import com.dustin.wcore.client.RetroConsoleRenderer;
import com.dustin.wcore.retro.RetroConsoleBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/** The light-gun cabinet: normal cabinet rendering, plus a per-frame gun ray-cast onto its screen. */
public class GunCabinetRenderer extends RetroConsoleRenderer {
   public GunCabinetRenderer(Context context, Identifier model) {
      super(context, model, new BuiltInScreen(GunClient.SY, GunClient.SF, GunClient.SW, GunClient.SH), 6.0, 9.0);
   }

   @Override
   public void submit(BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      if (RenderCull.far(state)) return;
      super.submit(state, pose, collector, camera);
      ObjCabinetRenderer.shell("gun_cabinet", state, pose, collector);
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || state.blockPos == null) return;
      BlockState bs = mc.level.getBlockState(state.blockPos);
      if (bs.hasProperty(RetroConsoleBlock.FACING)) {
         GunClient.aim(state.blockPos, bs.getValue(RetroConsoleBlock.FACING), mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
      }
   }
}
