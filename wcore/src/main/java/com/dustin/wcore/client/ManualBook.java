package com.dustin.wcore.client;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.item.ManualItem;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.GeoItemRenderer.RenderData;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ManualBook {
   private static final long OPEN_NANOS = 550000000L;
   private static final long FLIP_NANOS = 700000000L;
   private static final DataTicket<Identifier> TEXTURE = DataTicket.create("wcore_manual_page", Identifier.class);
   private static final int PAGE_W = 240;
   private static final int PAGE_H = 304;
   private static String current = "";
   private static int state = 0;
   private static int spread;
   private static long since = System.nanoTime();
   private static final Map<String, Identifier> TEXTURES = new HashMap<>();

   private ManualBook() {
   }

   public static void init() {
      ManualItem.USE = ManualBook::onUse;
      ManualItem.STATE = ManualBook::state;
      ManualItem.RENDERER = ManualBook::renderer;
      TwoHanded.register(WorkingConsolesCore.MANUAL, (poseStack, stack) -> {
         poseStack.translate(0.0F, -0.06F, 0.02F);
         poseStack.rotateDegrees(Axis.XP, 80.0F);
         poseStack.rotateDegrees(Axis.YP, 180.0F);
         poseStack.scale(0.42F, 0.42F, 0.42F);
      });
   }

   private static void set(int s) {
      state = s;
      since = System.nanoTime();
   }

   private static int spreads(String id) {
      ManualItem.Manual m = ManualItem.get(id);
      return m == null ? 1 : (m.pages().size() + 2) / 2;
   }

   public static int state() {
      LocalPlayer p = Minecraft.getInstance().player;
      if (p != null) {
         String held = heldId(p);
         if (held != null && !held.equals(current)) {
            current = held;
            spread = 0;
            set(0);
         }
      }

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

   private static String heldId(Player p) {
      for (InteractionHand h : InteractionHand.values()) {
         ItemStack s = p.getItemInHand(h);
         if (s.getItem() instanceof ManualItem) {
            return ManualItem.idOf(s);
         }
      }

      return null;
   }

   private static void sound(SoundEvent event, float pitch) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         player.playSound(event, 0.8F, pitch);
      }
   }

   private static void onUse(String id, boolean back) {
      if (!id.equals(current)) {
         current = id;
         spread = 0;
         set(0);
      }

      int before = state();
      int last = spreads(id) - 1;
      switch (before) {
         case 0:
            set(1);
            break;
         case 2:
            if (!back) {
               set(spread < last ? 3 : 5);
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

   private static GeoItemRenderer<?> renderer() {
      return new GeoItemRenderer<ManualItem>(new DefaultedItemGeoModel<ManualItem>(WorkingConsolesCore.id("manual")) {
         public Identifier getTextureResource(GeoRenderState renderState) {
            Identifier t = (Identifier)renderState.getGeckolibData(ManualBook.TEXTURE);
            return t != null ? t : super.getTextureResource(renderState);
         }
      }) {
         public void captureDefaultRenderState(ManualItem item, RenderData data, GeoRenderState renderState, float partialTick) {
            super.captureDefaultRenderState(item, data, renderState, partialTick);
            if (data.itemStack() != null) {
               String id = ManualItem.idOf(data.itemStack());
               ItemDisplayContext ctx = data.renderPerspective();
               boolean inHands = ctx != null && ctx.firstPerson() && id.equals(ManualBook.current);
               int s = inHands ? ManualBook.state() : 0;

               String key = switch (s) {
                  case 3 -> "turn_" + ManualBook.spread;
                  case 4 -> "turn_" + (ManualBook.spread - 1);
                  default -> "spread_" + (inHands ? ManualBook.spread : 0);
               };
               renderState.addGeckolibData(ManualBook.TEXTURE, ManualBook.texture(id, key));
            }
         }
      };
   }

   private static Identifier texture(String id, String key) {
      String k = id + "/" + key;
      Identifier have = TEXTURES.get(k);
      if (have != null) {
         return have;
      }

      ManualItem.Manual m = ManualItem.get(id);
      if (m == null) {
         return WorkingConsolesCore.id("textures/item/manual.png");
      }

      BufferedImage img = paint(m, id, key);
      Identifier tex = WorkingConsolesCore.id("dynamic/manual/" + id.replaceAll("[^a-z0-9_]", "_") + "_" + key);
      DynamicTexture dt = new DynamicTexture(() -> "Manual " + k, 1024, 1024, false);
      NativeImage n = dt.getPixels();
      if (n != null) {
         for (int y = 0; y < 1024; y++) {
            for (int x = 0; x < 1024; x++) {
               n.setPixel(x, y, img.getRGB(x, y));
            }
         }

         dt.upload();
      }

      Minecraft.getInstance().getTextureManager().register(tex, dt);
      TEXTURES.put(k, tex);
      return tex;
   }

   private static Color cover(String id) {
      float hue = (id.hashCode() & 65535) / 65535.0F;
      return Color.getHSBColor(hue, 0.55F, 0.42F);
   }

   private static BufferedImage paint(ManualItem.Manual m, String id, String key) {
      BufferedImage img = new BufferedImage(1024, 1024, 2);
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Color c = cover(id);
      g.setColor(c);
      g.fillRect(0, 320, 240, 312);
      g.fillRect(512, 320, 128, 128);
      g.setColor(c.darker());
      g.fillRect(256, 320, 240, 312);
      g.setColor(new Color(236, 226, 198));
      g.fillRect(640, 320, 64, 64);
      AffineTransform keep = g.getTransform();
      g.translate(240, 632);
      g.rotate(Math.PI);
      g.setColor(new Color(214, 180, 90));
      g.drawRect(10, 10, 219, 291);
      g.drawRect(14, 14, 211, 283);
      g.setFont(new Font("Serif", 1, 24));
      centred(g, wrap(g, m.title().toUpperCase(), 190), 80, 240);
      g.setFont(new Font("Serif", 2, 14));
      centred(g, List.of("Instruction Manual"), 250, 240);
      g.setTransform(keep);
      boolean turn = key.startsWith("turn_");
      int s = Integer.parseInt(key.substring(key.indexOf(95) + 1));
      int total = m.pages().size() + 1;
      page(g, m, 2 * s, total, 0, 0, false);
      if (turn) {
         page(g, m, 2 * s + 3, total, 256, 0, false);
         page(g, m, 2 * s + 1, total, 512, 0, false);
         page(g, m, 2 * s + 2, total, 768, 0, true);
      } else {
         page(g, m, 2 * s + 1, total, 256, 0, false);
         page(g, m, 2 * s + 1, total, 512, 0, false);
         page(g, m, 2 * s + 2, total, 768, 0, true);
      }

      g.dispose();
      return img;
   }

   private static void page(Graphics2D g, ManualItem.Manual m, int index, int total, int x, int y, boolean upsideDown) {
      AffineTransform keep = g.getTransform();
      g.translate(x, y);
      if (upsideDown) {
         g.translate(240, 304);
         g.rotate(Math.PI);
      }

      g.setColor(new Color(236, 226, 198));
      g.fillRect(0, 0, 240, 304);
      g.setColor(new Color(220, 208, 176));
      g.drawRect(0, 0, 239, 303);
      if (index == 0) {
         g.setColor(new Color(30, 90, 120));
         g.setFont(new Font("Serif", 1, 24));
         centred(g, wrap(g, m.title(), 200), 90, 240);
         g.setColor(new Color(60, 50, 40));
         g.drawLine(60, 170, 180, 170);
         g.setFont(new Font("Serif", 2, 13));
         centred(g, List.of("Right-click: next page", "Sneak + right-click: back"), 200, 240);
      } else if (index < total) {
         String text = m.pages().get(index - 1);
         int size = 15;

         List<String> lines;
         do {
            g.setFont(new Font("Serif", 0, size));
            lines = new ArrayList<>();

            for (String para : text.split("\n", -1)) {
               lines.addAll(para.isEmpty() ? List.of("") : wrap(g, para, 212));
            }
         } while (lines.size() * g.getFontMetrics().getHeight() > 258 && --size > 9);

         FontMetrics fm = g.getFontMetrics();
         g.setColor(new Color(40, 34, 28));
         int ly = 16 + fm.getAscent();

         for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == 0) {
               g.setFont(g.getFont().deriveFont(1));
               g.setColor(new Color(30, 90, 120));
            }

            g.drawString(line, 14, ly);
            if (i == 0) {
               g.setFont(g.getFont().deriveFont(0));
               g.setColor(new Color(40, 34, 28));
            }

            ly += fm.getHeight();
         }

         g.setFont(new Font("Serif", 2, 10));
         g.setColor(new Color(110, 100, 86));
         centred(g, List.of("Page " + (index + 1) + " of " + total), 292, 240);
      }

      g.setTransform(keep);
   }

   private static List<String> wrap(Graphics2D g, String text, int width) {
      FontMetrics fm = g.getFontMetrics();
      List<String> out = new ArrayList<>();
      StringBuilder line = new StringBuilder();

      for (String word : text.split(" ")) {
         String next = line.isEmpty() ? word : line + " " + word;
         if (fm.stringWidth(next) > width && !line.isEmpty()) {
            out.add(line.toString());
            line = new StringBuilder(word);
         } else {
            line = new StringBuilder(next);
         }
      }

      out.add(line.toString());
      return out;
   }

   private static void centred(Graphics2D g, List<String> lines, int top, int width) {
      FontMetrics fm = g.getFontMetrics();
      int y = top + fm.getAscent();

      for (String l : lines) {
         g.drawString(l, (width - fm.stringWidth(l)) / 2, y);
         y += fm.getHeight();
      }
   }
}
