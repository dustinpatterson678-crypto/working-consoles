package com.dustin.retro.client;

import com.dustin.retro.RetroConsoles;
import com.dustin.retro.RetroMod;
import com.dustin.wcore.client.RetroConsoleRenderer;
import com.dustin.wcore.client.RetroConsoleRenderer.BuiltInScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class RetroClientMod implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      for (RetroConsoles.Console c : RetroConsoles.ALL) {
         BuiltInScreen screen = c.spec.builtInScreen()
            ? new BuiltInScreen(c.spec.screenY(), c.spec.screenFront(), c.spec.screenW(), c.spec.screenH())
            : null;
         double portSide = c.spec.handheld() ? 4.0 : 5.6;
         BlockEntityRenderers.register(
            c.be,
            ctx -> new RetroConsoleRenderer(ctx, RetroMod.id(c.spec.id()), screen, portSide, 1.2)
         );
      }
   }
}
