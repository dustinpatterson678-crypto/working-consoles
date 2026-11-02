package com.dustin.gbsp.client;

import com.dustin.gbsp.GameBoySP;
import com.dustin.gbsp.item.GuideBookItem;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class GuideBookClient {
   private static final int SPREADS = 5;
   private static final long OPEN_NANOS = 550000000L;
   private static final long FLIP_NANOS = 700000000L;
   private static int state = 0;
   private static int spread;
   private static long since = System.nanoTime();

   private GuideBookClient() {
   }

   public static void init() {
      GameBoySP.BOOK_USE = GuideBookClient::onUse;
      GameBoySP.BOOK_STATE = GuideBookClient::state;
   }

   private static void set(int s) {
      state = s;
      since = System.nanoTime();
   }

   public static int state() {
      long t = System.nanoTime() - since;
      switch (state) {
         case 1:
            if (t > 550000000L) {
               set(2);
            }
         case 2:
         default:
            break;
         case 3:
            if (t > 700000000L) {
               spread++;
               set(2);
            }
            break;
         case 4:
            if (t > 700000000L) {
               spread--;
               set(2);
            }
            break;
         case 5:
            if (t > 550000000L) {
               set(0);
            }
      }

      return state;
   }

   private static void sound(SoundEvent event, float pitch) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         player.playSound(event, 0.8F, pitch);
      }
   }

   private static void onUse(boolean back) {
      int before = state();
      switch (before) {
         case 0:
            set(1);
            break;
         case 2:
            if (!back) {
               set(spread < 4 ? 3 : 5);
            } else {
               set(spread > 0 ? 4 : 5);
            }

            if (state == 5 && !back) {
               spread = 0;
            }
      }

      if (state != before) {
         if (state != 3 && state != 4) {
            sound(SoundEvents.BOOK_PUT, state == 1 ? 1.1F : 0.9F);
         } else {
            sound(SoundEvents.BOOK_PAGE_TURN, 0.9F + (float)Math.random() * 0.2F);
         }
      }
   }

   public static Identifier texture() {
      int s = state();

      String name = switch (s) {
         case 3 -> "turn_" + spread;
         case 4 -> "turn_" + (spread - 1);
         default -> "spread_" + spread;
      };
      return GameBoySP.id("textures/item/guide_book/" + name + ".png");
   }

   public static GeoItemRenderer<GuideBookItem> createRenderer() {
      return new GeoItemRenderer(new DefaultedItemGeoModel<GuideBookItem>(GameBoySP.id("guide_book")) {
         public Identifier getTextureResource(GeoRenderState renderState) {
            return GuideBookClient.texture();
         }
      });
   }
}
