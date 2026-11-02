package com.dustin.stream.client;

import com.dustin.stream.StreamHooks;
import com.dustin.stream.StreamMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class StreamClient implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      StreamHooks.TICK = Browsers::tick;
      StreamHooks.OPEN = pos -> {
         Minecraft mc = Minecraft.getInstance();
         mc.gui.setScreen(new ControlScreen(pos));
      };
      BlockEntityRenderers.register(StreamMod.MEDIA_BE, MediaRenderer::new);
      BlockEntityRenderers.register(StreamMod.DESK_BE, DeskRenderer::new);
      ClientTickEvents.END_CLIENT_TICK.register(Browsers::clientTick);
   }
}
