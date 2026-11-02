package com.dustin.wcore.retro;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.emu.DumpCheck;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.Item;

public final class RetroSystem {
   private static final Map<String, RetroSystem> SYSTEMS = new LinkedHashMap<>();
   public final String id;
   public final String name;
   public final String core;
   public final String folder;
   public final String mediaWord;
   public final String howTo;
   public final List<String> extensions;
   public final List<String> biosAnyOf;
   public final Map<String, String> options;
   public final int latencyMs;
   public final DumpCheck.Kind kind;
   public Supplier<Item> mediaItem = () -> null;
   /** Controller ports on the real console (set with withPorts). */
   public int ports = 2;

   public RetroSystem(
      String id,
      String name,
      String core,
      String folder,
      String mediaWord,
      List<String> extensions,
      List<String> biosAnyOf,
      Map<String, String> options,
      int latencyMs,
      String howTo
   ) {
      this.id = id;
      this.name = name;
      this.core = core;
      this.folder = folder;
      this.mediaWord = mediaWord;
      this.extensions = extensions;
      this.biosAnyOf = biosAnyOf;
      this.options = options;
      this.latencyMs = latencyMs;
      this.kind = DumpCheck.Kind.HANDHELD;
      this.howTo = howTo;
   }

   public RetroSystem withPorts(int ports) {
      this.ports = ports;
      return this;
   }

   public static RetroSystem register(RetroSystem s) {
      SYSTEMS.put(s.id, s);
      s.ensureFolders();
      return s;
   }

   public static RetroSystem get(String id) {
      return SYSTEMS.get(id);
   }

   public static Collection<RetroSystem> all() {
      return SYSTEMS.values();
   }

   public Path root() {
      return FabricLoader.getInstance().getGameDir().resolve(this.folder);
   }

   public Path gamesDir() {
      return this.root().resolve("games");
   }

   public Path biosDir() {
      return this.root().resolve("bios");
   }

   public boolean needsBios() {
      return !this.biosAnyOf.isEmpty();
   }

   public Path bios() {
      if (!this.needsBios()) {
         return null;
      }

      try (Stream<Path> files = Files.list(this.biosDir())) {
         List<Path> all = files.toList();

         for (String want : this.biosAnyOf) {
            for (Path p : all) {
               if (p.getFileName().toString().equalsIgnoreCase(want)) {
                  return p;
               }
            }
         }
      } catch (Exception var10) {
      }

      return null;
   }

   public List<RetroSystem.Game> games() {
      List<RetroSystem.Game> out = new ArrayList<>();

      try (Stream<Path> files = Files.list(this.gamesDir())) {
         files.filter(x$0 -> Files.isRegularFile(x$0)).sorted().forEach(p -> {
            String n = p.getFileName().toString().toLowerCase(Locale.ROOT);

            for (String e : this.extensions) {
               if (n.endsWith(e)) {
                  out.add(new RetroSystem.Game(p, title(p.getFileName().toString())));
                  break;
               }
            }
         });
      } catch (Exception var7) {
      }

      return out;
   }

   public RetroSystem.Game game(String file) {
      if (file != null && !file.isEmpty() && !file.contains("/") && !file.contains("\\") && !file.contains("..")) {
         Path p = this.gamesDir().resolve(file);
         return Files.isRegularFile(p) ? new RetroSystem.Game(p, title(file)) : null;
      } else {
         return null;
      }
   }

   public static String title(String file) {
      int dot = file.lastIndexOf(46);
      String t = dot > 0 ? file.substring(0, dot) : file;
      return t.replace('_', ' ').trim();
   }

   private void ensureFolders() {
      try {
         Files.createDirectories(this.gamesDir());
         Files.createDirectories(this.biosDir());

         Path how = this.root().resolve("HOW_TO_USE.txt");
         if (!Files.exists(how)) {
            Files.writeString(
               how,
               this.name
                  + "\n\n  games/   your own game dumps ("
                  + String.join(" ", this.extensions)
                  + ")\n"
                  + (this.needsBios() ? "  bios/    your own BIOS/firmware dumps: " + String.join(", ", this.biosAnyOf) + "\n" : "")
                  + "\nCover art: put a .png or .jpg with the same name as the game next to it (or cover.png / folder.jpg).\n\n"
                  + this.howTo
                  + "\n"
            );
         }
      } catch (Exception e) {
         WorkingConsolesCore.LOGGER.warn("Couldn't create {} folders: {}", this.name, e.toString());
      }
   }

   public record Game(Path file, String title) {
   }
}
