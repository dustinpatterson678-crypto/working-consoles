package com.dustin.stream.client;

import net.dimaskama.mcef.api.MCEFBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandler;
import org.cef.CefSettings;
import java.util.Set;
import java.util.Collections;
import java.util.WeakHashMap;

/** Captures browser console messages and routes them to Minecraft's log. */
final class BrowserConsole implements CefDisplayHandler {
   private static final Set<org.cef.CefClient> READY = Collections.newSetFromMap(new WeakHashMap<>());

   static void ensure(MCEFBrowser browser) {
      if (browser == null) return;
      try {
         CefBrowser cb = browser.getCefBrowser();
         if (cb == null) return;
         org.cef.CefClient client = cb.getClient();
         if (client == null || READY.contains(client)) return;
         client.addDisplayHandler(new BrowserConsole());
         READY.add(client);
      } catch (Throwable t) {
         org.slf4j.LoggerFactory.getLogger("working-consoles-stream").warn("Could not add console handler", t);
      }
   }

   @Override
   public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {}

   @Override
   public void onTitleChange(CefBrowser browser, String title) {}

   @Override
   public void onFullscreenModeChange(CefBrowser browser, boolean fullscreen) {}

   @Override
   public boolean onTooltip(CefBrowser browser, String text) { return false; }

   @Override
   public boolean onConsoleMessage(CefBrowser browser, org.cef.CefSettings.LogSeverity level, String message, String source, int line) {
      try {
         String lvl = level == null ? "INFO" : level.name();
         org.slf4j.LoggerFactory.getLogger("working-consoles-browser").info("[{}] {} ({}:{})", lvl, message, source, line);
         // Also send to player chat for [WC] messages
         if (message != null && message.startsWith("[WC]")) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
               mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[Browser Console] " + message).withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
         }
      } catch (Throwable ignored) {}
      return false;
   }

   @Override
   public boolean onCursorChange(CefBrowser browser, int cursorType) { return false; }

   @Override
   public void onStatusMessage(CefBrowser browser, String value) {}
}