package com.dustin.sega.client;

import com.dustin.sega.SegaMod;
import com.dustin.wcore.client.RetroConsoleRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class SegaClient implements ClientModInitializer {
   public void onInitializeClient() {
      BlockEntityRenderers.register(SegaMod.GENESIS_BE, ctx -> new RetroConsoleRenderer(ctx, SegaMod.id("genesis"), null, 5.6, 1.2));
      BlockEntityRenderers.register(SegaMod.SATURN_BE, ctx -> new RetroConsoleRenderer(ctx, SegaMod.id("saturn"), null, 6.0, 1.4));
   }
}
