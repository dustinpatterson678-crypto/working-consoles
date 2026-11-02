package com.dustin.arcadebuild.client;

import com.dustin.arcadebuild.ArcadeBuildMod;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class ArcadeBuildClient implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      // The Grand Hall was retired in 1.1. Do not register its renderer, lens or ambience:
      // the lens used to toggle the HUD and made it disappear outside the hall.
      BlockEntityRenderers.register(ArcadeBuildMod.FACADE_BE, FacadeRenderer::new);
      BlockEntityRenderers.register(ArcadeBuildMod.POWER_STRIP_BE, PowerStripRenderer::new);
      BlockEntityRenderers.register(ArcadeBuildMod.DECOR_BE, DecorRenderer::new);
      net.minecraft.client.renderer.entity.EntityRenderers.register(ArcadeBuildMod.SEAT, net.minecraft.client.renderer.entity.NoopRenderer::new);
   }
}
