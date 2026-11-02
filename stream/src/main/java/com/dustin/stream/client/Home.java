package com.dustin.stream.client;

import java.io.InputStream;
import java.util.Base64;

/** The Smart TV's own home screen, shipped inside the mod (no website needed). */
final class Home {
   static final String KEY = "home";
   private static String url;

   private Home() {
   }

   static synchronized String url() {
      if (url == null) {
         try (InputStream in = Home.class.getResourceAsStream("/assets/stream/web/home.html")) {
            url = "data:text/html;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
         } catch (Exception e) {
            url = "about:blank";
         }
      }
      return url;
   }

   static String resolve(String stored) {
      return stored == null || stored.isBlank() || stored.equals(KEY) ? url() : stored;
   }

   static String toStored(String url) {
      return url == null || url.startsWith("data:") ? KEY : url;
   }
}
