package com.dustin.wcore.client;

import com.dustin.wcore.WorkingConsolesCore;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class CoverArt {
   private static final String[] EXT = new String[]{".png", ".jpg", ".jpeg"};
   private static final Map<String, Identifier> READY = new ConcurrentHashMap<>();
   private static final Map<String, CompletableFuture<BufferedImage>> LOADING = new ConcurrentHashMap<>();

   private CoverArt() {
   }

   public static Identifier texture(Path game, Identifier baseTexture, CoverArt.Panel panel) {
      if (game == null) {
         return baseTexture;
      }

      String key = baseTexture + "|" + game.toAbsolutePath();
      Identifier ready = READY.get(key);
      if (ready != null) {
         return ready;
      }

      CompletableFuture<BufferedImage> f = LOADING.computeIfAbsent(key, k -> CompletableFuture.supplyAsync(() -> {
         Path coverx = find(game);
         if (coverx == null) {
            return null;
         }

         try {
            return ImageIO.read(coverx.toFile());
         } catch (Exception e) {
            WorkingConsolesCore.LOGGER.warn("Couldn't read cover art {}: {}", coverx, e.toString());
            return null;
         }
      }));
      if (!f.isDone()) {
         return baseTexture;
      }

      BufferedImage cover = f.getNow(null);
      Identifier id = cover == null ? baseTexture : compose(key, baseTexture, cover, panel);
      READY.put(key, id);
      LOADING.remove(key);
      return id;
   }

   public static Path find(Path game) {
      Path dir = game.toAbsolutePath().getParent();
      if (dir == null) {
         return null;
      }

      String name = game.getFileName().toString();
      int dot = name.lastIndexOf(46);
      String base = dot > 0 ? name.substring(0, dot) : name;

      for (String e : EXT) {
         for (Path p : new Path[]{dir.resolve(base + e), dir.resolve("covers").resolve(base + e), dir.resolve("cover" + e), dir.resolve("folder" + e)}) {
            if (Files.isRegularFile(p)) {
               return p;
            }
         }
      }

      try (Stream<Path> files = Files.list(dir)) {
         String lb = base.toLowerCase(Locale.ROOT);
         return files.filter(px -> {
            String n = px.getFileName().toString().toLowerCase(Locale.ROOT);

            for (String ex : EXT) {
               if (n.equals(lb + ex)) {
                  return true;
               }
            }

            return false;
         }).findFirst().orElse(null);
      } catch (Exception e) {
         return null;
      }
   }

   private static Identifier compose(String key, Identifier baseTexture, BufferedImage cover, CoverArt.Panel panel) {
      try (InputStream in = Minecraft.getInstance().getResourceManager().open(baseTexture)) {
         NativeImage img = NativeImage.read(in);
         BufferedImage scaled = new BufferedImage(panel.w(), panel.h(), 2);
         Graphics2D g = scaled.createGraphics();
         g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
         g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
         g.drawImage(cover, 0, 0, panel.w(), panel.h(), null);
         g.dispose();

         for (int y = 0; y < panel.h(); y++) {
            for (int x = 0; x < panel.w(); x++) {
               img.setPixel(panel.x() + x, panel.y() + y, scaled.getRGB(x, y) | 0xFF000000);
            }
         }

         Identifier id = WorkingConsolesCore.id("dynamic/cover/" + Integer.toHexString(key.hashCode()));
         Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "case cover " + key, img));
         return id;
      } catch (Exception e) {
         WorkingConsolesCore.LOGGER.warn("Couldn't build the cover for {}: {}", key, e.toString());
         return baseTexture;
      }
   }

   public record Panel(int x, int y, int w, int h) {
   }
}
