package com.dustin.gbsp.emu;

import com.dustin.gbsp.GameBoySP;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class SystemFiles {
   private static final long GBA_BIOS_SIZE = 16384L;
   private static final long GBA_BIOS_CRC = 2174186293L;
   private static final long GBA_BIOS_CRC_ALT = 3131971711L;
   private static final long DMG_BOOT_SIZE = 256L;
   private static final long DMG_BOOT_CRC = 1506302350L;
   private static final long CGB_BOOT_SIZE = 2304L;
   private static final long CGB_BOOT_CRC = 1099451974L;

   private SystemFiles() {
   }

   public static Path root() {
      return FabricLoader.getInstance().getGameDir().resolve("gameboy");
   }

   public static Path biosDir() {
      return root().resolve("bios");
   }

   public static Path romDir() {
      return root().resolve("roms");
   }

   public static Path saveDir() {
      return root().resolve("saves");
   }

   public static Path saveFor(Path rom) {
      String name = rom.getFileName().toString();
      int dot = name.lastIndexOf(46);
      return saveDir().resolve((dot > 0 ? name.substring(0, dot) : name) + ".sav");
   }

   public static void ensureFolders() {
      try {
         Files.createDirectories(biosDir());
         Files.createDirectories(romDir());
         Files.createDirectories(saveDir());
         Path help = root().resolve("HOW_TO_USE.txt");
         if (!Files.exists(help)) {
            Files.writeString(
               help,
               "Game Boy SP mod - put your OWN dumps here:\n\n  bios/  -> your GBA BIOS (16 KB .bin file, e.g. gba_bios.bin)  [required]\n            optional: Game Boy boot ROM (256 bytes), Game Boy Color boot ROM (2304 bytes)\n  roms/  -> your games: .gba  .gbc  .gb\n\nWithout a BIOS and at least one ROM, the Game Boy SP is just a model.\nRight-click the Game Boy SP in game to re-check.\n"
            );
         }
      } catch (IOException e) {
         GameBoySP.LOGGER.error("Couldn't create the gameboy folders", e);
      }
   }

   public static SystemFiles.Report scan() {
      ensureFolders();
      List<SystemFiles.Bios> bios = new ArrayList<>();
      List<SystemFiles.Rom> roms = new ArrayList<>();
      List<String> bad = new ArrayList<>();

      for (Path file : listFiles(biosDir())) {
         try {
            long size = Files.size(file);
            if (size == 16384L) {
               bios.add(
                  new SystemFiles.Bios(
                     file, SystemFiles.BiosType.GBA, isGbaBiosCrc(crc(file)) ? SystemFiles.BiosStatus.VERIFIED : SystemFiles.BiosStatus.UNKNOWN_DUMP
                  )
               );
            } else if (size == 256L && crc(file) == 1506302350L) {
               bios.add(new SystemFiles.Bios(file, SystemFiles.BiosType.GB, SystemFiles.BiosStatus.VERIFIED));
            } else if (size == 2304L && crc(file) == 1099451974L) {
               bios.add(new SystemFiles.Bios(file, SystemFiles.BiosType.GBC, SystemFiles.BiosStatus.VERIFIED));
            } else if (!file.getFileName().toString().endsWith(".txt")) {
               bad.add(file.getFileName().toString());
            }
         } catch (IOException e) {
            bad.add(file.getFileName().toString());
         }
      }

      bios.sort((a, b) -> a.status().compareTo(b.status()));

      for (Path file : listFiles(romDir())) {
         String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
         SystemFiles.RomType type = name.endsWith(".gba")
            ? SystemFiles.RomType.GBA
            : (name.endsWith(".gbc") ? SystemFiles.RomType.GBC : (name.endsWith(".gb") ? SystemFiles.RomType.GB : null));
         if (type != null) {
            try {
               SystemFiles.Rom rom = checkRom(file, type);
               if (rom != null) {
                  roms.add(rom);
               } else {
                  bad.add(file.getFileName().toString());
               }
            } catch (IOException e) {
               bad.add(file.getFileName().toString());
            }
         }
      }

      return new SystemFiles.Report(bios, roms, bad);
   }

   private static SystemFiles.Rom checkRom(Path file, SystemFiles.RomType type) throws IOException {
      byte[] header = new byte[336];

      try (InputStream in = Files.newInputStream(file)) {
         if (in.readNBytes(header, 0, header.length) < header.length) {
            return null;
         }
      }

      if (type != SystemFiles.RomType.GBA) {
         int sum = 0;

         for (int i = 308; i <= 332; i++) {
            sum = sum - (header[i] & 255) - 1;
         }

         if ((sum & 0xFF) != (header[333] & 255)) {
            return null;
         }

         int cgbFlag = header[323] & 255;
         SystemFiles.RomType real = cgbFlag != 128 && cgbFlag != 192 ? SystemFiles.RomType.GB : SystemFiles.RomType.GBC;
         return new SystemFiles.Rom(file, real, title(header, 308, 11, file));
      } else {
         return (header[178] & 255) != 150 ? null : new SystemFiles.Rom(file, type, title(header, 160, 12, file));
      }
   }

   private static String title(byte[] header, int offset, int length, Path file) {
      StringBuilder sb = new StringBuilder();

      for (int i = offset; i < offset + length; i++) {
         int c = header[i] & 255;
         if (c == 0) {
            break;
         }

         if (c >= 32 && c < 127) {
            sb.append((char)c);
         }
      }

      String t = sb.toString().trim();
      return t.isEmpty() ? file.getFileName().toString() : t;
   }

   private static boolean isGbaBiosCrc(long crc) {
      return crc == 2174186293L || crc == 3131971711L;
   }

   private static long crc(Path file) throws IOException {
      CRC32 crc = new CRC32();
      crc.update(Files.readAllBytes(file));
      return crc.getValue();
   }

   private static List<Path> listFiles(Path dir) {
      try (Stream<Path> stream = Files.list(dir)) {
         return stream.filter(x$0 -> Files.isRegularFile(x$0)).sorted().toList();
      } catch (IOException e) {
         return List.of();
      }
   }

   public record Bios(Path file, SystemFiles.BiosType type, SystemFiles.BiosStatus status) {
   }

   public enum BiosStatus {
      VERIFIED,
      UNKNOWN_DUMP;
   }

   public enum BiosType {
      GBA,
      GB,
      GBC;
   }

   public record Report(List<SystemFiles.Bios> bios, List<SystemFiles.Rom> roms, List<String> badFiles) {
      public SystemFiles.Bios gbaBios() {
         return this.bios.stream().filter(b -> b.type() == SystemFiles.BiosType.GBA).findFirst().orElse(null);
      }

      public boolean ready() {
         return this.gbaBios() != null && !this.roms.isEmpty();
      }

      public List<Component> describe() {
         List<Component> lines = new ArrayList<>();
         SystemFiles.Bios gba = this.gbaBios();
         if (gba == null) {
            lines.add(Component.literal("[Game Boy SP] No GBA BIOS found in gameboy/bios").withStyle(ChatFormatting.RED));
         } else if (gba.status() == SystemFiles.BiosStatus.VERIFIED) {
            lines.add(Component.literal("[Game Boy SP] GBA BIOS verified: " + gba.file().getFileName()).withStyle(ChatFormatting.GREEN));
         } else {
            lines.add(
               Component.literal("[Game Boy SP] GBA BIOS found but not a known dump (it may still work): " + gba.file().getFileName())
                  .withStyle(ChatFormatting.YELLOW)
            );
         }

         for (SystemFiles.Bios b : this.bios) {
            if (b.type() != SystemFiles.BiosType.GBA) {
               lines.add(Component.literal("[Game Boy SP] " + b.type() + " boot ROM found: " + b.file().getFileName()).withStyle(ChatFormatting.GREEN));
            }
         }

         if (this.roms.isEmpty()) {
            lines.add(Component.literal("[Game Boy SP] No games found in gameboy/roms (.gba .gbc .gb)").withStyle(ChatFormatting.RED));
         } else {
            lines.add(Component.literal("[Game Boy SP] " + this.roms.size() + " game(s) found:").withStyle(ChatFormatting.GREEN));

            for (SystemFiles.Rom rom : this.roms.subList(0, Math.min(5, this.roms.size()))) {
               lines.add(Component.literal("  - " + rom.title() + " (" + rom.type() + ")").withStyle(ChatFormatting.WHITE));
            }

            if (this.roms.size() > 5) {
               lines.add(Component.literal("  ...and " + (this.roms.size() - 5) + " more").withStyle(ChatFormatting.GRAY));
            }
         }

         for (String bad : this.badFiles) {
            lines.add(Component.literal("[Game Boy SP] Skipped (not a valid dump): " + bad).withStyle(ChatFormatting.GOLD));
         }

         return lines;
      }
   }

   public record Rom(Path file, SystemFiles.RomType type, String title) {
   }

   public enum RomType {
      GBA,
      GBC,
      GB;
   }
}
