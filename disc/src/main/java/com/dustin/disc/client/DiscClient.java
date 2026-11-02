package com.dustin.disc.client;

import com.dustin.disc.DiscMod;
import com.dustin.wcore.client.RetroConsoleRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class DiscClient implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      BlockEntityRenderers.register(DiscMod.PS1.be, ctx -> new RetroConsoleRenderer(ctx, DiscMod.id("ps1"), null, 5.6, 1.2));
      BlockEntityRenderers.register(DiscMod.TDO.be, ctx -> new RetroConsoleRenderer(ctx, DiscMod.id("tdo"), null, 5.6, 1.0));
      BlockEntityRenderers.register(DiscMod.PCFX.be, ctx -> new RetroConsoleRenderer(ctx, DiscMod.id("pcfx"), null, 3.2, 1.0));
   }
}
