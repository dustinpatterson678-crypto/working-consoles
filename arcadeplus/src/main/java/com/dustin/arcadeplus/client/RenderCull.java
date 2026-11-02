package com.dustin.arcadeplus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Skips drawing arcade machines that are too far away to see, so a big arcade doesn't redraw every model every frame. */
final class RenderCull {
   private static final double FAR_SQ = 40.0 * 40.0;

   private RenderCull() {
   }

   static boolean far(BlockEntityRenderState state) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || state.blockPos == null) return false;
      return mc.player.position().distanceToSqr(state.blockPos.getX() + 0.5, state.blockPos.getY() + 0.5, state.blockPos.getZ() + 0.5) > FAR_SQ;
   }
}
