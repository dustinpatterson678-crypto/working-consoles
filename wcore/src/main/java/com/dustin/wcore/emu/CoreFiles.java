package com.dustin.wcore.emu;

import com.dustin.wcore.WorkingConsolesCore;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class CoreFiles {
   private static final Map<String, Boolean> DOWNLOADING = new ConcurrentHashMap<>();

   private CoreFiles() {
   }

   public static Path path(String core) {
      return LibretroCore.consolesDir().resolve("cores").resolve(core + "_libretro" + ext());
   }

   private static String ext() {
      return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? ".dll" : ".so";
   }

   private static String url(String core) {
      boolean win = ext().equals(".dll");
      return "https://buildbot.libretro.com/nightly/" + (win ? "windows" : "linux") + "/x86_64/latest/" + core + "_libretro" + ext() + ".zip";
   }

   public static Path ensure(String core, Consumer<String> status) {
      Path p = path(core);
      if (Files.isRegularFile(p)) {
         return p;
      }

      if (DOWNLOADING.putIfAbsent(core, true) == null) {
         status.accept("Downloading the " + core + " emulator core (one time only)...");
         Thread t = new Thread(() -> {
            try {
               download(core, p);
               status.accept("Emulator core " + core + " is ready. Turn the console on again.");
            } catch (Exception e) {
               WorkingConsolesCore.LOGGER.error("Couldn't download core {}", core, e);
               status.accept("Couldn't download the " + core + " core: " + e.getMessage() + ". Put " + p.getFileName() + " in consoles/cores yourself.");
            } finally {
               DOWNLOADING.remove(core);
            }
         }, "WC-CoreDownload-" + core);
         t.setDaemon(true);
         t.start();
      } else {
         status.accept("Still downloading the " + core + " core...");
      }

      return null;
   }

   private static void download(String core, Path target) throws Exception {
      Files.createDirectories(target.getParent());
      HttpClient http = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20L)).build();
      HttpResponse<InputStream> res = http.send(
         HttpRequest.newBuilder(URI.create(url(core))).timeout(Duration.ofMinutes(10L)).build(), BodyHandlers.ofInputStream()
      );
      if (res.statusCode() != 200) {
         throw new IllegalStateException("server said " + res.statusCode());
      }

      Path tmp = target.resolveSibling(target.getFileName() + ".part");
      boolean found = false;
      ZipInputStream zip = new ZipInputStream(res.body());

      ZipEntry e;
      try {
         while ((e = zip.getNextEntry()) != null) {
            if (e.getName().endsWith(target.getFileName().toString())) {
               Files.copy(zip, tmp, StandardCopyOption.REPLACE_EXISTING);
               found = true;
               break;
            }
         }
      } catch (Throwable var10) {
         try {
            zip.close();
         } catch (Throwable var9) {
            var10.addSuppressed(var9);
         }

         throw var10;
      }

      zip.close();
      if (!found) {
         throw new IllegalStateException("the download didn't contain " + target.getFileName());
      }

      Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
      WorkingConsolesCore.LOGGER.info("Downloaded emulator core {}", target);
   }
}
