package com.dustin.stream.client;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serves picked files to the browser over localhost. CEF's off-screen renderer does not always
 * turn a native file path into a usable File (with real bytes) in OSR mode, so instead of relying
 * on that we register the picked file here and hand the page a URL it can fetch from. Range
 * requests are supported so games can read multi-GB ISOs in chunks without loading them whole.
 */
final class BrowserFileServer {
   private static HttpServer server;
   private static int port = -1;
   private static final Map<String, String> FILES = new ConcurrentHashMap<>();
   private static final Object LOCK = new Object();

   static synchronized void ensure() {
      if (server != null) {
         return;
      }
      synchronized (LOCK) {
         if (server != null) {
            return;
         }
         try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            port = server.getAddress().getPort();
            server.createContext("/wc-file", new FileHandler());
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
         } catch (Throwable t) {
            org.slf4j.LoggerFactory.getLogger("working-consoles-stream").warn("Could not start file server", t);
            server = null;
            port = -1;
         }
      }
   }

   /** Registers a file and returns the URL the page can fetch it from. */
   static String register(String path) {
      ensure();
      if (port < 0) {
         return null;
      }
      String id = UUID.randomUUID().toString();
      FILES.put(id, path);
      return "http://127.0.0.1:" + port + "/wc-file?id=" + id;
   }

   static int port() {
      ensure();
      return port;
   }

   private static final class FileHandler implements HttpHandler {
      @Override
      public void handle(HttpExchange ex) throws IOException {
         org.slf4j.LoggerFactory.getLogger("working-consoles-stream").info("File server request: {} {}", ex.getRequestMethod(), ex.getRequestURI());
         try {
            // Handle CORS preflight
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
               ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
               ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
               ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Range");
               ex.getResponseHeaders().set("Access-Control-Expose-Headers", "Content-Range, Content-Length, Accept-Ranges");
               ex.sendResponseHeaders(204, -1);
               return;
            }
            String query = ex.getRequestURI().getQuery();
            String id = null;
            if (query != null) {
               for (String part : query.split("&")) {
                  String[] kv = part.split("=", 2);
                  if (kv.length == 2 && kv[0].equals("id")) {
                     id = URLDecoder.decode(kv[1], "UTF-8");
                  }
               }
            }
            String path = id == null ? null : FILES.get(id);
            if (path == null) {
               ex.sendResponseHeaders(404, 0);
               ex.close();
               return;
            }
            File f = new File(path);
            if (!f.isFile()) {
               ex.sendResponseHeaders(404, 0);
               ex.close();
               return;
            }
            long total = f.length();
            String range = ex.getRequestHeaders().getFirst("Range");
            long start = 0, end = total - 1;
            boolean partial = false;
            if (range != null && range.startsWith("bytes=")) {
               String r = range.substring(6).trim();
               String[] parts = r.split("-", 2);
               try {
                  if (!parts[0].isEmpty()) start = Long.parseLong(parts[0]);
                  if (parts.length > 1 && !parts[1].isEmpty()) end = Long.parseLong(parts[1]);
                  if (end >= total) end = total - 1;
                  if (start <= end && start < total) partial = true;
               } catch (NumberFormatException ignored) {
               }
            }
            String mime = "application/octet-stream";
            String name = f.getName().toLowerCase(java.util.Locale.ROOT);
            if (name.endsWith(".txt") || name.endsWith(".json") || name.endsWith(".js") || name.endsWith(".html") || name.endsWith(".css")) {
               mime = "text/plain; charset=utf-8";
            }
            // WebAssembly payloads must be served as application/wasm or the
            // browser refuses to instantiate them ("fetch failed" / stall).
            if (name.endsWith(".wasm")) {
               mime = "application/wasm";
            }
            // CORS + cross-origin isolation headers so the HTTPS page can fetch
            // from localhost and still get SharedArrayBuffer / crossOriginIsolated.
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Range");
            ex.getResponseHeaders().set("Access-Control-Expose-Headers", "Content-Range, Content-Length, Accept-Ranges");
            ex.getResponseHeaders().set("Cross-Origin-Opener-Policy", "same-origin");
            ex.getResponseHeaders().set("Cross-Origin-Embedder-Policy", "require-corp");
            ex.getResponseHeaders().set("Cross-Origin-Resource-Policy", "cross-origin");
            ex.getResponseHeaders().set("Content-Type", mime);
            ex.getResponseHeaders().set("Content-Length", String.valueOf(end - start + 1));
            if (partial) {
               ex.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + total);
               ex.getResponseHeaders().set("Accept-Ranges", "bytes");
               ex.sendResponseHeaders(206, end - start + 1);
            } else {
               start = 0;
               end = total - 1;
               ex.getResponseHeaders().set("Accept-Ranges", "bytes");
               ex.sendResponseHeaders(200, total);
            }
            OutputStream os = ex.getResponseBody();
            try (InputStream is = new FileInputStream(f)) {
               is.skip(start);
               byte[] buf = new byte[65536];
               long remaining = end - start + 1;
               while (remaining > 0) {
                  int read = is.read(buf, 0, (int) Math.min(buf.length, remaining));
                  if (read < 0) break;
                  os.write(buf, 0, read);
                  remaining -= read;
               }
            }
            os.close();
         } catch (Throwable t) {
            try { ex.close(); } catch (Throwable ignored) {}
         }
      }
   }
}
