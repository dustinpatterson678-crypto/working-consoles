package com.dustin.arcadebuild.hall;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class K {
   private K() {
   }

   public static String d(String s) {
      byte[] b = Base64.getDecoder().decode(s);
      for (int i = 0; i < b.length; i++) b[i] ^= (byte)(((0x5A + 31 * i) & 0xFF) ^ ((i >> 2) & 0xFF));
      return new String(b, StandardCharsets.UTF_8);
   }

   public static double[] v(String s) {
      String[] p = s.split(",");
      double[] o = new double[p.length];
      for (int i = 0; i < p.length; i++) o[i] = Double.parseDouble(p[i]);
      return o;
   }
}
