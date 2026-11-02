package com.dustin.nintendo.client;

import com.dustin.nintendo.NintendoMod;
import com.dustin.wcore.client.RetroConsoleRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class NintendoClient implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      BlockEntityRenderers.register(NintendoMod.NES.be, ctx -> new RetroConsoleRenderer(ctx, NintendoMod.id("nes"), null, 5.6, 1.2));
      BlockEntityRenderers.register(NintendoMod.SNES.be, ctx -> new RetroConsoleRenderer(ctx, NintendoMod.id("snes"), null, 5.6, 1.2));
      BlockEntityRenderers.register(NintendoMod.N64.be, ctx -> new RetroConsoleRenderer(ctx, NintendoMod.id("n64"), null, 5.6, 1.0));
      com.dustin.wcore.client.WorkingConsolesCoreClient.registerController(NintendoMod.NES_CONTROLLER);
      com.dustin.wcore.client.WorkingConsolesCoreClient.registerController(NintendoMod.SNES_CONTROLLER);
      com.dustin.wcore.client.WorkingConsolesCoreClient.registerController(NintendoMod.N64_CONTROLLER);
   }
}
