package com.dustin.stream.client;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

/** Remembers the last page each screen was on (this PC only). */
final class UrlStore {
      private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("working_consoles_stream_urls.properties");
   private static Properties props;

   private UrlStore() {
   }

   private static synchronized Properties props() {
      if (props == null) {
         props = new Properties();
         if (Files.isRegularFile(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE)) {
               props.load(r);
            } catch (Exception ignored) {
            }
         }
      }
      return props;
   }

   static synchronized String get(String world, BlockPos pos) {
      return props().getProperty(world + "@" + pos.asLong(), Home.KEY);
   }

   static synchronized void put(String world, BlockPos pos, String url) {
      if (url == null || url.isBlank()) {
         return;
      }
      props().setProperty(world + "@" + pos.asLong(), Home.toStored(url));
      try (Writer w = Files.newBufferedWriter(FILE)) {
         props().store(w, "Working Consoles - last page per screen");
      } catch (Exception ignored) {
      }
   }

   static synchronized void remove(String world, BlockPos pos) {
      props().remove(world + "@" + pos.asLong());
      try (Writer w = Files.newBufferedWriter(FILE)) {
         props().store(w, "Working Consoles - last page per screen");
      } catch (Exception ignored) {
      }
   }
}
