package com.dustin.stream.client;

import com.dustin.stream.MediaBlockEntity;
import com.dustin.stream.StreamMod;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.cef.browser.CefFrame;

/** One real Chromium browser per powered screen near you, rendered straight into a GPU texture inside Minecraft. */
final class Browsers {
   static final int W = 1280;
   static final int H = 720;
   private static final double RANGE = 96.0;
   private static final Map<BlockPos, Session> SESSIONS = new ConcurrentHashMap<>();
   private static final Object REOPEN = new Object();
   private static BlockPos reopenAt;
   private static int reopenIn;
   private static MCEFApi.Initialization init;
   private static long lastNotice;
   private static boolean failedTold;
   private static long lastCookieFlush;

   static final class Session {
      final MCEFBrowser browser;
      final Identifier texId;
      boolean shimPending = true;
      long shimAt;
      volatile boolean padSent;
      volatile List<CefFrame> frames = List.of();

      Session(MCEFBrowser browser, Identifier texId) {
         this.browser = browser;
         this.texId = texId;
      }
   }

   private Browsers() {
   }

   static Session get(BlockPos pos) {
      return pos == null ? null : SESSIONS.get(pos);
   }

   static boolean ready(Session s) {
      return s != null && s.browser.getTextureView() != null;
   }

   static void tick(MediaBlockEntity be) {
      Minecraft mc = Minecraft.getInstance();
      BlockPos pos = be.getBlockPos();
      boolean want = be.isOn() && be.isPowered() && be.input() == 0 && mc.player != null && mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) < RANGE * RANGE;
      if (!want) {
         close(pos);
         return;
      }
      Session s = SESSIONS.get(pos);
      if (s == null) {
         open(pos, mc);
         return;
      }
      if (ready(s)) {
         BrowserShim.tick(s);
         BrowserGamepad.tick(pos, s);
         BrowserFiles.ensure(s.browser);
         BrowserHeaders.ensure(s.browser);
         BrowserDownloads.ensure(s.browser);
         BrowserConsole.ensure(s.browser);
         // Flush cookies periodically to persist logins
         long now = System.currentTimeMillis();
         if (now - lastCookieFlush > 30000L) {
            lastCookieFlush = now;
            try {
               org.cef.network.CefCookieManager.getGlobalManager().flushStore(new org.cef.callback.CefCompletionCallback() {
                  public void onComplete() {}
               });
            } catch (Throwable ignored) {}
         }
      }
   }

   private static void open(BlockPos pos, Minecraft mc) {
      if (init == null) {
         init = MCEFApi.initialize();
      }
      if (init.getFuture().isCompletedExceptionally()) {
         if (!failedTold && mc.player != null) {
            failedTold = true;
            mc.player.sendOverlayMessage(Component.literal("The browser engine couldn't start: check your internet for the one-time download, then restart").withStyle(ChatFormatting.RED));
         }
         return;
      }
      if (!init.isDone()) {
         long now = System.currentTimeMillis();
         if (now - lastNotice > 2500L && mc.player != null) {
            lastNotice = now;
            mc.player.sendOverlayMessage(Component.literal("Setting up the browser engine (one time only): "
               + init.getStage().name().toLowerCase() + " " + Math.round(init.getPercentage() * 100F) + "%").withStyle(ChatFormatting.AQUA));
         }
         return;
      }
      MCEFApi api = init.getFuture().getNow(null);
      if (api == null) {
         return;
      }
      // Create with about:blank first so COOP/COEP headers are in place before the page loads
      MCEFBrowser browser = api.createBrowser("about:blank", false);
      browser.resize(W, H);
      BrowserFiles.ensure(browser);
      BrowserHeaders.ensure(browser);
      BrowserDownloads.ensure(browser);
      BrowserConsole.ensure(browser);
      Identifier texId = StreamMod.id("browser/" + Long.toHexString(pos.asLong() & 0xFFFFFFFFFFFFL));
      mc.getTextureManager().register(texId, new BrowserTexture(browser));
      SESSIONS.put(pos.immutable(), new Session(browser, texId));
      // Now navigate to the actual URL (headers are already in place)
      String targetUrl = Home.resolve(UrlStore.get(world(mc), pos));
      if (targetUrl != null && !targetUrl.isBlank()) {
         browser.getCefBrowser().loadURL(targetUrl);
      }
   }

   static void close(BlockPos pos) {
      Session s = SESSIONS.remove(pos);
      if (s == null) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      // Only save URL if the block is still there (player walked away).
      // If the block was destroyed, clear cached state instead.
      boolean blockGone = mc.level == null || !(mc.level.getBlockEntity(pos) instanceof MediaBlockEntity);
      if (blockGone) {
         // Block destroyed: don't save URL, clear cached state
         UrlStore.remove(mc.level == null ? "none" : String.valueOf(mc.level.dimension()), pos);
      } else {
         saveUrl(pos, s);
      }
      if (pos.equals(InputFx.controlling) && mc.gui.screen() instanceof ControlScreen) {
         mc.gui.setScreen(null);
      }
      mc.getTextureManager().release(s.texId);
      try { org.cef.network.CefCookieManager.getGlobalManager().flushStore(new org.cef.callback.CefCompletionCallback() { public void onComplete() {} }); } catch (Throwable ignored) {}
      s.browser.close();
   }

   static void saveUrl(BlockPos pos, Session s) {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            UrlStore.put(world(mc), pos, s.browser.getCefBrowser().getURL());
         }
      } catch (Exception ignored) {
      }
   }

   /** Drop browsers whose block is gone, or every browser when leaving the world. */
   static void cleanup(Minecraft mc) {
      for (BlockPos pos : SESSIONS.keySet()) {
         if (mc.level == null || !(mc.level.getBlockEntity(pos) instanceof MediaBlockEntity)) {
            close(pos);
         }
      }
   }

   /** Runs every client tick: housekeeping, plus bringing the controls screen back after a file pick. */
   static void clientTick(Minecraft mc) {
      cleanup(mc);
      BlockPos pos;
      synchronized (REOPEN) {
         if (reopenAt == null) {
            return;
         }
         if (--reopenIn > 0) {
            return;
         }
         pos = reopenAt;
         reopenAt = null;
      }
      if (mc.level == null || !(mc.level.getBlockEntity(pos) instanceof MediaBlockEntity) || !SESSIONS.containsKey(pos)) {
         return;
      }
      if (mc.gui.screen() instanceof ControlScreen) {
         return;
      }
      Screen open = mc.gui.screen();
      if (open != null && !(open instanceof PauseScreen)) {
         return;
      }
      mc.gui.setScreen(new ControlScreen(pos));
   }

   /** The file picker takes the window focus (the game pauses behind it), so hand the screen back after. */
   static void wantReopen(BlockPos pos) {
      synchronized (REOPEN) {
         reopenAt = pos == null ? null : pos.immutable();
         reopenIn = 10;
      }
   }

   static String world(Minecraft mc) {
      return mc.level == null ? "none" : String.valueOf(mc.level.dimension());
   }
}
