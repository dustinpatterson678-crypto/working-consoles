package com.dustin.classic.client;

import com.dustin.classic.ClassicMod;
import com.dustin.wcore.client.RetroConsoleRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class ClassicClient implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      BlockEntityRenderers.register(ClassicMod.C64.be, ctx -> new RetroConsoleRenderer(ctx, ClassicMod.id("c64"), null, 5.6, 1.0));
      BlockEntityRenderers.register(ClassicMod.AMIGA.be, ctx -> new RetroConsoleRenderer(ctx, ClassicMod.id("amiga"), null, 5.6, 1.0));
      BlockEntityRenderers.register(ClassicMod.ATARIST.be, ctx -> new RetroConsoleRenderer(ctx, ClassicMod.id("atarist"), null, 5.6, 1.0));
      BlockEntityRenderers.register(ClassicMod.ZXSPECTRUM.be, ctx -> new RetroConsoleRenderer(ctx, ClassicMod.id("zxspectrum"), null, 5.6, 1.0));
      BlockEntityRenderers.register(ClassicMod.CPC.be, ctx -> new RetroConsoleRenderer(ctx, ClassicMod.id("cpc"), null, 5.6, 1.0));
      BlockEntityRenderers.register(ClassicMod.DOSPC.be, ctx -> new RetroConsoleRenderer(ctx, ClassicMod.id("dospc"), null, 5.6, 1.0));
      BlockEntityRenderers.register(ClassicMod.ATARI800.be, ctx -> new RetroConsoleRenderer(ctx, ClassicMod.id("atari800"), null, 5.6, 1.0));
      BlockEntityRenderers.register(ClassicMod.SMS.be, ctx -> new RetroConsoleRenderer(ctx, ClassicMod.id("sms"), null, 5.6, 1.0));
      BlockEntityRenderers.register(ClassicMod.GAMEGEAR.be, ctx -> new RetroConsoleRenderer(ctx, ClassicMod.id("gamegear"), new RetroConsoleRenderer.BuiltInScreen(4.8, 0.98, 5.8, 4.3), 5.6, 1.0));
   }
}
