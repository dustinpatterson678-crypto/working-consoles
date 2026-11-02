package com.dustin.ps2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

public final class Ps2Files {
   private static final long MIN_BIOS = 4194304L;
   private static final long MAX_BIOS = 8388608L;
   private static final String[] DISC_EXT = new String[]{".iso", ".chd", ".cso", ".zso", ".ciso", ".cue", ".gz", ".elf", ".m3u"};

   private Ps2Files() {
   }

   public static Path root() {
      return FabricLoader.getInstance().getGameDir().resolve("ps2");
   }

   public static Path biosDir() {
      return root().resolve("bios");
   }

   public static Path discDir() {
      return root().resolve("isos");
   }

   public static void ensureFolders() {
      try {
         Files.createDirectories(biosDir());
         Files.createDirectories(discDir());
         Path help = root().resolve("HOW_TO_USE.txt");
         if (!Files.exists(help)) {
            Files.writeString(
               help,
               "PS2 in Minecraft - put your OWN dumps here:\n\n  bios/  -> your PS2 BIOS dump (a 4 MB .bin file)   [required: without it the console won't power on]\n  isos/  -> your disc images: .iso .chd .cso .zso .cue\n\nIn game: plug the console into a TV and a live wall outlet with cables,\nright-click the console to power it on, use a disc case on it to insert a disc.\nType /ps2 for your game list.\n"
            );
         }
      } catch (IOException var1) {
      }
   }

   public static Path bios() {
      for (Path p : list(biosDir())) {
         try {
            long size = Files.size(p);
            if (size >= 4194304L && size <= 8388608L) {
               return p;
            }
         } catch (IOException var4) {
         }
      }

      return null;
   }

   public static List<Ps2Files.Disc> discs() {
      List<Ps2Files.Disc> out = new ArrayList<>();

      for (Path p : list(discDir())) {
         String name = p.getFileName().toString();
         String lower = name.toLowerCase(Locale.ROOT);

         for (String ext : DISC_EXT) {
            if (lower.endsWith(ext)) {
               out.add(new Ps2Files.Disc(p, title(name)));
               break;
            }
         }
      }

      return out;
   }

   public static Ps2Files.Disc disc(String file) {
      for (Ps2Files.Disc d : discs()) {
         if (d.file().getFileName().toString().equals(file)) {
            return d;
         }
      }

      return null;
   }

   public static String title(String fileName) {
      int dot = fileName.lastIndexOf(46);
      return (dot > 0 ? fileName.substring(0, dot) : fileName).replace('_', ' ').trim();
   }

   private static List<Path> list(Path dir) {
      try (Stream<Path> s = Files.list(dir)) {
         return s.filter(x$0 -> Files.isRegularFile(x$0)).sorted().toList();
      } catch (IOException e) {
         return List.of();
      }
   }

   public record Disc(Path file, String title) {
   }
}
