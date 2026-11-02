package com.dustin.stream.client;

import net.dimaskama.mcef.api.MCEFBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefRequestHandler;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import java.util.Set;
import java.util.Collections;
import java.util.WeakHashMap;

/**
 * Adds Cross-Origin-Opener-Policy and Cross-Origin-Embedder-Policy headers to every
 * response so that SharedArrayBuffer (and thus threaded WebAssembly) works in the
 * off-screen browser. Without these, pages that need threaded WASM (like Halo CE)
 * fail to start with "Could not start:" errors.
 */
final class BrowserHeaders implements CefRequestHandler, CefResourceRequestHandler {
   private static final Set<org.cef.CefClient> READY = Collections.newSetFromMap(new WeakHashMap<>());

   static void ensure(MCEFBrowser browser) {
      if (browser == null) return;
      try {
         CefBrowser cb = browser.getCefBrowser();
         if (cb == null) return;
         org.cef.CefClient client = cb.getClient();
         if (client == null || READY.contains(client)) return;
         client.addRequestHandler(new BrowserHeaders());
         READY.add(client);
         org.slf4j.LoggerFactory.getLogger("working-consoles-stream").info("COOP/COEP header handler attached to browser");
      } catch (Throwable t) {
         org.slf4j.LoggerFactory.getLogger("working-consoles-stream").warn("Could not add COOP/COEP headers", t);
      }
   }

   @Override
   public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean userGesture, boolean isRedirect) {
      return false;
   }

   @Override
   public boolean onOpenURLFromTab(CefBrowser browser, CefFrame frame, String targetUrl, boolean userGesture) {
      return false;
   }

   @Override
   public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser, CefFrame frame, CefRequest request, boolean isNavigation, boolean isDownload, String requestInitiator, BoolRef disableDefaultHandling) {
      return this;
   }

   @Override
   public org.cef.handler.CefCookieAccessFilter getCookieAccessFilter(CefBrowser browser, CefFrame frame, CefRequest request) {
      return null;
   }

   @Override
   public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
      return false;
   }

   @Override
   public org.cef.handler.CefResourceHandler getResourceHandler(CefBrowser browser, CefFrame frame, CefRequest request) {
      return null;
   }

   @Override
   public void onResourceRedirect(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response, org.cef.misc.StringRef newUrl) {
   }

   @Override
   public boolean onResourceResponse(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response) {
      try {
         response.setHeaderByName("Cross-Origin-Opener-Policy", "same-origin", true);
         response.setHeaderByName("Cross-Origin-Embedder-Policy", "require-corp", true);
         response.setHeaderByName("Cross-Origin-Resource-Policy", "cross-origin", true);
         if (request.getURL().contains(".wasm") || request.getURL().contains("combat-lab") || request.getURL().contains("halo")) {
            org.slf4j.LoggerFactory.getLogger("working-consoles-stream").info("COOP/COEP headers set on: {}", request.getURL());
         }
      } catch (Throwable t) {
         org.slf4j.LoggerFactory.getLogger("working-consoles-stream").warn("Failed to set COOP/COEP on: {}", request.getURL(), t);
      }
      return false;
   }

   @Override
   public void onResourceLoadComplete(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response, org.cef.network.CefURLRequest.Status status, long receivedContentLength) {
   }

   @Override
   public void onProtocolExecution(CefBrowser browser, CefFrame frame, CefRequest request, org.cef.misc.BoolRef allowOsExecution) {
   }

   @Override
   public boolean getAuthCredentials(CefBrowser browser, String originUrl, boolean isProxy, String host, int port, String realm, String scheme, org.cef.callback.CefAuthCallback callback) {
      return false;
   }

   @Override
   public boolean onCertificateError(CefBrowser browser, org.cef.handler.CefLoadHandler.ErrorCode errorCode, String requestUrl, org.cef.callback.CefCallback callback) {
      return false;
   }

   @Override
   public void onRenderProcessTerminated(CefBrowser browser, org.cef.handler.CefRequestHandler.TerminationStatus status, int errorCode, String errorString) {
   }
}
