package com.dustin.ps2.client;

import com.dustin.ps2.Ps2Files;
import com.dustin.ps2.Ps2Mod;
import com.dustin.ps2.item.DiscCaseItem;
import com.dustin.wcore.ConsoleLinks;
import com.dustin.wcore.client.CaseRenderer;
import com.dustin.wcore.client.CoverArt.Panel;
import com.geckolib.renderer.GeoItemRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class Ps2Client implements ClientModInitializer {
   public void onInitializeClient() {
      Ps2Mod.CLIENT_FOCUS = Ps2Session::focus;
      ClientTickEvents.END_CLIENT_TICK.register(Ps2Session::clientTick);
      BlockEntityRenderers.register(Ps2Mod.CONSOLE_BLOCK_ENTITY, Ps2Renderer::new);
      ConsoleLinks.register(() -> Ps2Session.isRunning() ? "Disc console" : null);
   }

   public static GeoItemRenderer<DiscCaseItem> caseRenderer() {
      return new CaseRenderer<DiscCaseItem>(Ps2Mod.id("disc_case"), new Panel(0, 0, 108, 156), stack -> {
         String disc = DiscCaseItem.discOf(stack);
         return disc.isEmpty() ? null : Ps2Files.discDir().resolve(disc);
      });
   }
}
