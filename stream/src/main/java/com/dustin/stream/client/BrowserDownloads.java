package com.dustin.stream.client;

import net.dimaskama.mcef.api.MCEFBrowser;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.handler.CefDownloadHandler;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.Collections;
import java.util.WeakHashMap;

/**
 * Handles file downloads in the off-screen browser. Saves downloaded files to a
 * "downloads" folder inside Minecraft's config directory so the player can find them.
 */
final class BrowserDownloads implements CefDownloadHandler {
   private static final Set<org.cef.CefClient> READY = Collections.newSetFromMap(new WeakHashMap<>());
   private static Path downloadDir;

   static Path dir() {
      if (downloadDir == null) {
         try {
            downloadDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
               .resolve("working-consoles").resolve("downloads");
            Files.createDirectories(downloadDir);
         } catch (Throwable t) {
            downloadDir = Paths.get(System.getProperty("user.home"), "Downloads");
         }
      }
      return downloadDir;
   }

   static void ensure(MCEFBrowser browser) {
      if (browser == null) return;
      try {
         CefBrowser cb = browser.getCefBrowser();
         if (cb == null) return;
         org.cef.CefClient client = cb.getClient();
         if (client == null || READY.contains(client)) return;
         client.addDownloadHandler(new BrowserDownloads());
         READY.add(client);
      } catch (Throwable t) {
         org.slf4j.LoggerFactory.getLogger("working-consoles-stream").warn("Could not add download handler", t);
      }
   }

   @Override
   public boolean onBeforeDownload(CefBrowser browser, CefDownloadItem downloadItem, String suggestedName, CefBeforeDownloadCallback callback) {
      try {
         String name = suggestedName;
         if (name == null || name.isBlank()) {
            name = "download-" + System.currentTimeMillis();
         }
         // Sanitize filename
         name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
         Path target = dir().resolve(name);
         // Avoid overwriting
         int i = 1;
         String base = name;
         int dot = name.lastIndexOf('.');
         String ext = "";
         if (dot > 0) { ext = name.substring(dot); base = name.substring(0, dot); }
         while (Files.exists(target)) {
            target = dir().resolve(base + "_" + i + ext);
            i++;
         }
         org.slf4j.LoggerFactory.getLogger("working-consoles-stream").info("Downloading {} to {}", suggestedName, target);
         tell(browser, "Downloading: " + suggestedName + " → " + target.getFileName());
         callback.Continue(target.toString(), false);
         return true;
      } catch (Throwable t) {
         org.slf4j.LoggerFactory.getLogger("working-consoles-stream").warn("Download failed", t);
         return false;
      }
   }

   @Override
   public void onDownloadUpdated(CefBrowser browser, CefDownloadItem downloadItem, CefDownloadItemCallback callback) {
      try {
         if (downloadItem.isComplete()) {
            tell(browser, "Download complete: " + downloadItem.getSuggestedFileName() + " (" + downloadItem.getTotalBytes() + " bytes)");
         } else if (downloadItem.isCanceled()) {
            tell(browser, "Download cancelled: " + downloadItem.getSuggestedFileName());
         }
      } catch (Throwable ignored) {}
   }

   private static void tell(CefBrowser cb, String msg) {
      try {
         net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
         if (mc.player != null) {
            mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[Browser] " + msg).withStyle(net.minecraft.ChatFormatting.AQUA));
         }
      } catch (Throwable ignored) {}
   }
}
