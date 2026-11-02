package com.dustin.arcadeplus;

import com.dustin.wcore.emu.LibretroCore;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

/** Puts the light-gun proxy core into consoles/cores (it wraps the real MAME core and adds gun aiming). */
public final class GunCore {
   public static final String[] PROXIES = {"wcgun_mame2003_plus", "wcgun_mame"};

   private GunCore() {
   }

   public static boolean supported() {
      return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
   }

   public static Path proxyPath(String name) {
      return LibretroCore.consolesDir().resolve("cores").resolve(name + "_libretro.dll");
   }

   public static void install() {
      if (!supported()) {
         return;
      }
      try (InputStream in = GunCore.class.getResourceAsStream("/natives/wcgun.dll")) {
         if (in == null) {
            ArcadePlusMod.LOGGER.warn("Light gun core missing from the jar");
            return;
         }
         byte[] bytes = in.readAllBytes();
         for (String n : PROXIES) {
            Path p = proxyPath(n);
            Files.createDirectories(p.getParent());
            if (!Files.exists(p) || !Arrays.equals(Files.readAllBytes(p), bytes)) {
               try {
                  Files.write(p, bytes);
               } catch (Exception busy) {
                  ArcadePlusMod.LOGGER.info("Light gun core {} is in use, keeping the current copy", p.getFileName());
               }
            }
         }
      } catch (Exception e) {
         ArcadePlusMod.LOGGER.warn("Couldn't install the light gun core: {}", e.toString());
      }
   }
}
