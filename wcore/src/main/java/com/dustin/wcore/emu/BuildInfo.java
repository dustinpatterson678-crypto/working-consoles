package com.dustin.wcore.emu;

import java.io.InputStream;
import java.util.Properties;

public final class BuildInfo {
   public static final boolean RELEASE;
   public static final String STAMP;

   public static boolean hasDevProfile() {
      try {
         Class.forName("com.dustin.wcore.emu.DevProfile", false, BuildInfo.class.getClassLoader());
         return true;
      } catch (ClassNotFoundException e) {
         return false;
      }
   }

   private BuildInfo() {
   }

   static {
      boolean r = true;
      String stamp = "UNKNOWN";

      try (InputStream in = BuildInfo.class.getResourceAsStream("/wcore-build.properties")) {
         if (in != null) {
            Properties p = new Properties();
            p.load(in);
            r = !"false".equals(p.getProperty("release", "true").trim());
            stamp = p.getProperty("stamp", stamp).trim();
         }
      } catch (Exception var7) {
      }

      RELEASE = r;
      STAMP = stamp;
   }
}
