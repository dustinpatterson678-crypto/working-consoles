package com.dustin.arcade.client;

import com.dustin.arcade.ArcadeMod;
import com.dustin.wcore.client.RetroConsoleRenderer;
import com.dustin.wcore.client.RetroConsoleRenderer.BuiltInScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class ArcadeClient implements ClientModInitializer {
   public void onInitializeClient() {
      BlockEntityRenderers.register(
         ArcadeMod.CABINET_BE, ctx -> new RetroConsoleRenderer(ctx, ArcadeMod.id("cabinet"), new BuiltInScreen(11.0, 3.6, 9.0, 7.0), 5.0, 1.0)
      );
      BlockEntityRenderers.register(
         ArcadeMod.BIG_CABINET_BE, ctx -> new RetroConsoleRenderer(ctx, ArcadeMod.id("big_cabinet"), new BuiltInScreen(21.0, 3.2, 12.0, 9.0), 6.0, 1.0)
      );
   }
}
