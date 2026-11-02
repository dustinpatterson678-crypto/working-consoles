package com.dustin.gbsp.client;

import com.dustin.wcore.ConsoleSettings;

final class ScreenFilter {
   private static final float[] LIN_GBA = new float[256];
   private static final float[] LIN_SP = new float[256];
   private static final int[] OUT = new int[4097];
   private static final float[][] GBA = new float[][]{{0.8F, 0.135F, 0.195F}, {0.275F, 0.64F, 0.155F}, {-0.075F, 0.225F, 0.65F}};
   private static final float[][] SP = new float[][]{{0.9F, 0.1F, 0.0F}, {0.07F, 0.86F, 0.07F}, {0.02F, 0.1F, 0.88F}};
   private static int[] prev;

   private ScreenFilter() {
   }

   static void apply(int[] px, int n) {
      ConsoleSettings s = ConsoleSettings.get();
      if (!"off".equals(s.handheldColor)) {
         boolean gba = "gba".equals(s.handheldColor);
         float[] lin = gba ? LIN_GBA : LIN_SP;
         float[][] m = gba ? GBA : SP;

         for (int i = 0; i < n; i++) {
            int c = px[i];
            float r = lin[c & 0xFF];
            float g = lin[c >>> 8 & 0xFF];
            float b = lin[c >>> 16 & 0xFF];
            int R = out(m[0][0] * r + m[0][1] * g + m[0][2] * b);
            int G = out(m[1][0] * r + m[1][1] * g + m[1][2] * b);
            int B = out(m[2][0] * r + m[2][1] * g + m[2][2] * b);
            px[i] = 0xFF000000 | B << 16 | G << 8 | R;
         }
      }

      if (s.handheldBlend) {
         if (prev.length < n) {
            prev = new int[n];
            System.arraycopy(px, 0, prev, 0, n);
         }

         for (int i = 0; i < n; i++) {
            int c = px[i];
            int p = prev[i];
            prev[i] = c;
            px[i] = 0xFF000000 | ((c & 16711422) >>> 1) + ((p & 16711422) >>> 1);
         }
      }
   }

   private static int out(float v) {
      return OUT[v <= 0.0F ? 0 : (v >= 1.0F ? 4096 : (int)(v * 4096.0F))];
   }

   static {
      for (int i = 0; i < 256; i++) {
         LIN_GBA[i] = (float)Math.pow(i / 255.0, 2.7);
         LIN_SP[i] = (float)Math.pow(i / 255.0, 2.2);
      }

      for (int i = 0; i <= 4096; i++) {
         OUT[i] = (int)Math.round(Math.pow(i / 4096.0, 0.45454545454545453) * 255.0);
      }

      prev = new int[0];
   }
}
