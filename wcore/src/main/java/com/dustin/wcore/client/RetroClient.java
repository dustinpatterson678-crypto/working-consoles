package com.dustin.wcore.client;

import com.dustin.wcore.retro.RetroMediaItem;
import com.dustin.wcore.retro.RetroSystem;
import com.geckolib.renderer.GeoItemRenderer;

public final class RetroClient {
   private RetroClient() {
   }

   public static GeoItemRenderer<RetroMediaItem> mediaRenderer(RetroMediaItem item) {
      int[] p = item.coverPanel;
      return new CaseRenderer<RetroMediaItem>(item.model, new CoverArt.Panel(p[0], p[1], p[2], p[3]), stack -> {
         String game = RetroMediaItem.gameOf(stack);
         RetroSystem sys = item.system();
         return !game.isEmpty() && sys != null ? sys.gamesDir().resolve(game) : null;
      });
   }
}
