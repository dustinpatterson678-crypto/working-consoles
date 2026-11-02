package com.dustin.wcore.emu;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class DumpCheck {
   private static final byte[] SECRET = new byte[32];
   static final String NO_PASS = "This game didn't go through the own-dump check, so the console won't start it.";
   private static final String[] SITE_TAGS = new String[]{
      "coolrom",
      "romsfun",
      "romspure",
      "vimm",
      "emuparadise",
      "emulatorgames",
      "romhustler",
      "cdromance",
      "ziperto",
      "nicoblog",
      "romsmania",
      "freeroms",
      "doperoms",
      "loveroms",
      "retrostic",
      "romsgames",
      "wowroms",
      "archive.org",
      "www.",
      ".com",
      ".net",
      ".org",
      "[www",
      "(www"
   };
   private static final String[] ARCHIVES = new String[]{".zip", ".7z", ".rar"};
   private static final Map<Path, DumpCheck.Cached> CACHE = new ConcurrentHashMap<>();

   private static String sign(String game, long issued) {
      try {
         Mac mac = Mac.getInstance("HmacSHA256");
         mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
         byte[] d = mac.doFinal((game + "|" + issued).getBytes(StandardCharsets.UTF_8));
         return HexFormat.of().formatHex(d);
      } catch (Exception e) {
         return "";
      }
   }

   private static String key(Path game) {
      return game == null ? "" : game.toAbsolutePath().normalize().toString();
   }

   static boolean verify(DumpCheck.Pass pass, Path game) {
      if (pass != null && !pass.sig.isEmpty()) {
         long age = System.currentTimeMillis() - pass.issued;
         return age >= 0L
            && age < 120000L
            && pass.game.equals(key(game))
            && MessageDigest.isEqual(pass.sig.getBytes(StandardCharsets.UTF_8), sign(pass.game, pass.issued).getBytes(StandardCharsets.UTF_8));
      } else {
         return false;
      }
   }

   private DumpCheck() {
   }

   static boolean profileAllowed(String username, UUID profileId) {
      if (BuildInfo.RELEASE) {
         return false;
      }

      try {
         Class<?> dev = Class.forName("com.dustin.wcore.emu.DevProfile");
         return (Boolean)dev.getMethod("allowed", String.class, UUID.class).invoke(null, username, profileId);
      } catch (ReflectiveOperationException | LinkageError e) {
         return false;
      }
   }

   public static DumpCheck.Result check(String username, UUID profileId, Path bios, Path game, DumpCheck.Kind kind) {
      List<String> strong = new ArrayList<>();
      List<String> weak = new ArrayList<>();
      if (bios != null) {
         inspect(bios, "BIOS", kind == DumpCheck.Kind.PS2, strong, weak);
      }

      if (game != null) {
         inspect(game, "game", false, strong, weak);
      }

      List<String> reasons = new ArrayList<>(strong);
      reasons.addAll(weak);
      boolean flagged = !strong.isEmpty() || weak.size() >= 2;
      DumpCheck.Pass pass = new DumpCheck.Pass(key(game), System.currentTimeMillis());
      if (!flagged) {
         return new DumpCheck.Result(true, false, reasons, pass);
      } else {
         return profileAllowed(username, profileId) ? new DumpCheck.Result(true, true, reasons, pass) : new DumpCheck.Result(false, false, reasons, null);
      }
   }

   private static void inspect(Path file, String what, boolean ps2Bios, List<String> strong, List<String> weak) {
      DumpCheck.Cached c;
      try {
         long size = Files.size(file);
         long mod = Files.getLastModifiedTime(file).toMillis();
         c = CACHE.get(file);
         if (c == null || c.size() != size || c.modified() != mod) {
            c = scan(file, what, ps2Bios, size, mod);
            CACHE.put(file, c);
         }
      } catch (IOException e) {
         return;
      }

      strong.addAll(c.strong());
      weak.addAll(c.weak());
   }

   private static DumpCheck.Cached scan(Path file, String what, boolean ps2Bios, long size, long mod) {
      List<String> strong = new ArrayList<>();
      List<String> weak = new ArrayList<>();
      String name = file.getFileName().toString();
      String lower = name.toLowerCase(Locale.ROOT);
      int zone = downloadZone(file);
      if (zone >= 3) {
         strong.add("the " + what + " has Windows' downloaded-from-the-internet tag");
      }

      for (String tag : SITE_TAGS) {
         if (lower.contains(tag)) {
            strong.add("the " + what + " file name has a download-site tag (\"" + tag + "\")");
            break;
         }
      }

      String base = stripExt(name);
      Path dir = file.getParent();
      if (dir != null) {
         for (String ext : ARCHIVES) {
            if (Files.exists(dir.resolve(base + ext)) || Files.exists(dir.resolve(name + ext))) {
               weak.add("a downloaded-looking archive (" + base + ext + ") is next to the " + what);
               break;
            }
         }

         if (ps2Bios && !hasSibling(dir, base, ".nvm") && !hasSibling(dir, base, ".mec")) {
            weak.add("the BIOS has no .NVM / .MEC files next to it (the BIOS dumper makes them)");
         }
      }

      return new DumpCheck.Cached(size, mod, strong, weak);
   }

   private static int downloadZone(Path file) {
      if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
         return -1;
      }

      try (InputStream in = new FileInputStream(file.toAbsolutePath() + ":Zone.Identifier")) {
         String text = new String(in.readNBytes(4096), StandardCharsets.UTF_8);

         for (String line : text.split("\\R")) {
            line = line.trim();
            if (line.regionMatches(true, 0, "ZoneId=", 0, 7)) {
               try {
                  return Integer.parseInt(line.substring(7).trim());
               } catch (NumberFormatException e) {
                  return 3;
               }
            }
         }

         return -1;
      } catch (IOException e) {
         return -1;
      }
   }

   private static boolean hasSibling(Path dir, String base, String ext) {
      try (Stream<Path> s = Files.list(dir)) {
         return s.anyMatch(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(ext));
      } catch (IOException e) {
         return false;
      }
   }

   private static String stripExt(String name) {
      int dot = name.lastIndexOf(46);
      return dot > 0 ? name.substring(0, dot) : name;
   }

   static {
      new SecureRandom().nextBytes(SECRET);
   }

   private record Cached(long size, long modified, List<String> strong, List<String> weak) {
   }

   public enum Kind {
      PS2,
      HANDHELD,
      GAMECUBE;
   }

   public static final class Pass {
      private final String game;
      private final long issued;
      private final String sig;

      private Pass(String game, long issued) {
         this.game = game;
         this.issued = issued;
         this.sig = DumpCheck.sign(game, issued);
      }
   }

   public record Result(boolean allowed, boolean bypassed, List<String> reasons, DumpCheck.Pass pass) {
      public String summary() {
         return String.join("; ", this.reasons);
      }
   }
}
