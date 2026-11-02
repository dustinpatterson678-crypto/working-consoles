package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.ArcadePlusMod;
import com.dustin.wcore.client.RetroConsoleRenderer.BuiltInScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class ArcadePlusClient implements ClientModInitializer {
   @Override
   public void onInitializeClient() {
      net.minecraft.client.renderer.entity.EntityRenderers.register(com.dustin.arcadeplus.ArcadePlusMod.ATTENDANT, ctx -> new com.geckolib.renderer.GeoEntityRenderer<>(ctx, com.dustin.arcadeplus.ArcadePlusMod.ATTENDANT));
      Tooltips.init();
      CreativeTabButtons.init();
      BlockEntityRenderers.register(ArcadePlusMod.PINBALL_BE, PinballRenderer::new);
      BlockEntityRenderers.register(ArcadePlusMod.GUN_CAB.be, ctx -> new GunCabinetRenderer(ctx, ArcadePlusMod.id("gun_cabinet")));
      GunClient.init();
      ArcadeSounds.init();
      com.dustin.arcadeplus.TicketGameBlock.OPEN = (pos, game) -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(new TicketScreen(pos, game));
      net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.dustin.arcadeplus.net.PrizeResultPacket.TYPE,
         (payload, context) -> context.client().execute(() -> com.dustin.arcadeplus.net.PrizeResultPacket.CLIENT.accept(payload.message())));
      net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.dustin.arcadeplus.net.PrizeListPacket.TYPE,
         (payload, context) -> context.client().execute(() -> {
            java.util.List<com.dustin.arcadeplus.Prizes.Prize> l = new java.util.ArrayList<>();
            for (int i = 0; i < payload.ids().size(); i++) l.add(new com.dustin.arcadeplus.Prizes.Prize(payload.ids().get(i), payload.counts().get(i), payload.costs().get(i)));
            com.dustin.arcadeplus.Prizes.client = l;
            net.minecraft.client.Minecraft.getInstance().gui.setScreen(new PrizeScreen(payload.pos()));
         }));
      BlockEntityRenderers.register(com.dustin.arcadeplus.Prizes.DISPLAY_BE, PrizeDisplayRenderer::new);
      BlockEntityRenderers.register(com.dustin.arcadeplus.Prizes.STAND_BE, PrizeStandRenderer::new);
      com.dustin.arcadeplus.PinballBlock.OPEN = pos -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(new PinballScreen(pos));
      for (ArcadePlusMod.Ticket t : ArcadePlusMod.TICKET_GAMES) {
         String model = t.block.game.id();
         BlockEntityRenderers.register(t.be, ctx -> new TicketGameRenderer(ctx, model));
      }
      BlockEntityRenderers.register(ArcadePlusMod.BRAWLER.be, ctx -> new ObjCabinetRenderer(ctx, "brawler_cabinet",
         new BuiltInScreen(21.1, 3.08, 13.0, 8.8), new ThemedCabinetRenderer.Theme(28.5, 5.14, 18.0, 4.0, 9.2, 15.0, 3.0, 6.0, 28.0)));
      BlockEntityRenderers.register(ArcadePlusMod.RACER.be, ctx -> new ObjCabinetRenderer(ctx, "racing_cabinet",
         new BuiltInScreen(20.1, 1.58, 13.0, 8.8), new ThemedCabinetRenderer.Theme(28.5, 3.12, 16.0, 4.0, 8.03, 15.0, 3.5, 9.0, 28.0)));
      BlockEntityRenderers.register(ArcadePlusMod.RETRO_UPRIGHT.be, ctx -> new ObjCabinetRenderer(ctx, "retro_upright",
         new BuiltInScreen(23.0, 1.2, 11.5, 8.6), new ThemedCabinetRenderer.Theme(30.4, 2.3, 13.0, 3.0, 6.8, 18.0, 2.0, 8.0, 24.0)));
      BlockEntityRenderers.register(ArcadePlusMod.NIGHT_SHOOTER.be, ctx -> new ObjCabinetRenderer(ctx, "night_shooter",
         new BuiltInScreen(GunClient.SY, GunClient.SF, GunClient.SW, GunClient.SH), new ThemedCabinetRenderer.Theme(34.4, 5.8, 19.0, 6.0, 9.95, 18.0, 1.0, 11.0, 34.0)));
      BlockEntityRenderers.register(ArcadePlusMod.TURBO_MOTO.be, ctx -> new ObjCabinetRenderer(ctx, "turbo_moto",
         new BuiltInScreen(25.0, 1.5, 13.0, 9.5), new ThemedCabinetRenderer.Theme(33.2, 2.6, 14.0, 3.6, 7.1, 18.0, 2.4, 8.0, 30.0)));
      BlockEntityRenderers.register(ArcadePlusMod.STAR_PILOT.be, ctx -> new ObjCabinetRenderer(ctx, "star_pilot",
         new BuiltInScreen(19.0, -1.0, 13.0, 9.5), new ThemedCabinetRenderer.Theme(32.4, 19.6, 16.0, 3.6, 8.4, 16.0, -6.0, 14.0, 24.0)));
      BlockEntityRenderers.register(ArcadePlusMod.VERSUS_FIGHTER.be, ctx -> new ObjCabinetRenderer(ctx, "versus_fighter",
         new BuiltInScreen(20.0, 4.3, 14.2, 11.0), new ThemedCabinetRenderer.Theme(27.8, 4.9, 14.0, 3.4, 7.45, 14.0, 2.5, 8.0, 26.0)));
      BlockEntityRenderers.register(ArcadePlusMod.DUSTIN_MULTI.be, ctx -> new ObjCabinetRenderer(ctx, "dustin_multi",
         new BuiltInScreen(25.0, 2.6, 12.6, 10.4), new ThemedCabinetRenderer.Theme(35.0, 3.3, 12.0, 3.2, 6.5, 18.0, 1.5, 8.0, 32.0)));
      BlockEntityRenderers.register(ArcadePlusMod.CANDY.be, ctx -> new ObjCabinetRenderer(ctx, "candy_cabinet",
         new BuiltInScreen(19.9, 4.32, 14.2, 11.0), new ThemedCabinetRenderer.Theme(29.0, 3.12, 15.2, 2.8, 8.2, 15.5, 1.5, 10.0, 29.0)));
   }
}
