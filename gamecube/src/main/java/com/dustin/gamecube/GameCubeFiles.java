package com.dustin.gamecube;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

public final class GameCubeFiles {
   private static final String[] DISC_EXT = new String[]{".iso", ".gcm", ".gcz", ".ciso", ".wbfs", ".rvz", ".nkit"};

   private GameCubeFiles() {
   }

   public static Path root() {
      return FabricLoader.getInstance().getGameDir().resolve("gamecube");
   }

   public static Path discDir() {
      return root().resolve("isos");
   }

   public static void ensureFolders() {
      try {
         Files.createDirectories(discDir());
         Path help = root().resolve("HOW_TO_USE.txt");
         if (!Files.exists(help)) {
            Files.writeString(
               help,
               "GameCube in Minecraft - put your OWN dumps here:\n\n  isos/  -> your disc images: .iso .gcm .gcz .ciso .wbfs .rvz .nkit\n\nIn game: plug the console into a TV and a live wall outlet with cables,\nright-click the console to power it on, use a disc case on it to insert a disc.\nType /gc for your game list.\n"
            );
         }
      } catch (IOException var1) {
      }
   }

   public static List<GameCubeFiles.Disc> discs() {
      List<GameCubeFiles.Disc> out = new ArrayList<>();

      for (Path p : list(discDir())) {
         String name = p.getFileName().toString();
         String lower = name.toLowerCase(Locale.ROOT);

         for (String ext : DISC_EXT) {
            if (lower.endsWith(ext)) {
               out.add(new GameCubeFiles.Disc(p, title(name)));
               break;
            }
         }
      }

      return out;
   }

   public static GameCubeFiles.Disc disc(String file) {
      for (GameCubeFiles.Disc d : discs()) {
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