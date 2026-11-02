package com.dustin.wcore.emu;

import com.dustin.wcore.WorkingConsolesCore;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class CoreOptions {
   private static final Map<String, CoreOptions> CACHE = new LinkedHashMap<>();
   public final List<CoreOptions.Category> categories;
   private final Map<String, CoreOptions.Option> byKey = new LinkedHashMap<>();

   private CoreOptions(List<CoreOptions.Category> categories) {
      this.categories = categories;

      for (CoreOptions.Category c : categories) {
         for (CoreOptions.Option o : c.options()) {
            this.byKey.put(o.key(), o);
         }
      }
   }

   public CoreOptions.Option get(String key) {
      return this.byKey.get(key);
   }

   public static Path liveFile(String coreName) {
      return LibretroCore.consolesDir().resolve("cores").resolve(coreName + ".options.txt");
   }

   public static synchronized CoreOptions load(String coreName) {
      CoreOptions c = CACHE.get(coreName);
      if (c != null) {
         return c;
      }

      String text = null;

      try {
         Path live = liveFile(coreName);
         if (Files.isRegularFile(live)) {
            text = Files.readString(live, StandardCharsets.UTF_8);
         }
      } catch (Exception e) {
         WorkingConsolesCore.LOGGER.warn("Couldn't read the {} option list: {}", coreName, e.toString());
      }

      if (text == null || text.isBlank()) {
         try (InputStream in = CoreOptions.class.getResourceAsStream("/assets/wcore/core_options/" + coreName + ".txt")) {
            if (in != null) {
               text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
         } catch (Exception var9) {
         }
      }

      c = parse(text == null ? "" : text);
      CACHE.put(coreName, c);
      return c;
   }

   public static synchronized void invalidate(String coreName) {
      CACHE.remove(coreName);
   }

   static CoreOptions parse(String text) {
      Map<String, String[]> cats = new LinkedHashMap<>();
      Map<String, List<CoreOptions.Option>> grouped = new LinkedHashMap<>();
      CoreOptions.Option cur = null;

      for (String line : text.split("\n")) {
         String[] f = line.split("\t", -1);
         switch (f[0]) {
            case "C":
               if (f.length >= 3) {
                  cats.put(f[1], new String[]{f[2], f.length > 3 ? f[3] : ""});
               }
               break;
            case "O":
               if (f.length >= 5) {
                  cur = new CoreOptions.Option(f[1], f[2], f.length > 5 ? f[5] : "", f[3], f[4], new ArrayList<>(), new ArrayList<>());
                  grouped.computeIfAbsent(f[3], k -> new ArrayList<>()).add(cur);
               }
               break;
            case "V":
               if (cur != null && f.length >= 2) {
                  cur.values().add(f[1]);
                  cur.labels().add(f.length > 2 && !f[2].isEmpty() ? f[2] : f[1]);
               }
         }
      }

      List<CoreOptions.Category> out = new ArrayList<>();

      for (Entry<String, String[]> e : cats.entrySet()) {
         if (grouped.containsKey(e.getKey())) {
            out.add(new CoreOptions.Category(e.getKey(), e.getValue()[0], e.getValue()[1], grouped.remove(e.getKey())));
         }
      }

      for (Entry<String, List<CoreOptions.Option>> e : grouped.entrySet()) {
         out.add(new CoreOptions.Category(e.getKey(), e.getKey().isEmpty() ? "General" : e.getKey(), "", e.getValue()));
      }

      return new CoreOptions(out);
   }

   public record Category(String key, String label, String info, List<CoreOptions.Option> options) {
   }

   public record Option(String key, String label, String info, String category, String def, List<String> values, List<String> labels) {
      public String labelOf(String value) {
         int i = this.values.indexOf(value);
         return i >= 0 ? this.labels.get(i) : value;
      }
   }
}
