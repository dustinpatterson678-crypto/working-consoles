package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.ArcadePlusMod;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** Loads theme-kit PNGs from the player's own game folders, once each, and re-checks missing ones now and then. */
final class ThemeTextures {
   private record Entry(Identifier id, long modified, long checkedAt) {
   }

   private static final Map<Path, Entry> CACHE = new HashMap<>();
   private static int counter;

   private ThemeTextures() {
   }

   static Identifier get(Path file) {
      long now = System.currentTimeMillis();
      Entry e = CACHE.get(file);
      if (e != null && now - e.checkedAt() < 5000L) {
         return e.id();
      }
      long modified = -1L;
      try {
         modified = Files.isRegularFile(file) ? Files.getLastModifiedTime(file).toMillis() : -1L;
      } catch (Exception ignored) {
      }
      if (e != null && e.modified() == modified) {
         CACHE.put(file, new Entry(e.id(), modified, now));
         return e.id();
      }
      Minecraft mc = Minecraft.getInstance();
      if (e != null && e.id() != null) {
         mc.getTextureManager().release(e.id());
      }
      Identifier id = null;
      if (modified >= 0) {
         try (InputStream in = Files.newInputStream(file)) {
            NativeImage img = NativeImage.read(in);
            id = ArcadePlusMod.id("theme/" + (counter++));
            mc.getTextureManager().register(id, new DynamicTexture(() -> "Arcade theme " + file.getFileName(), img));
         } catch (Exception ex) {
            ArcadePlusMod.LOGGER.warn("Couldn't load theme art {}: {}", file, ex.toString());
            id = null;
         }
      }
      CACHE.put(file, new Entry(id, modified, now));
      return id;
   }
}
