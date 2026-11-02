package com.dustin.arcadeplus.client;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;

/** Small helpers for drawing quads and boxes in block-local units. */
final class Draw {
   private Draw() {
   }

   /** A flat rectangle centred on c, facing n (unit axis vector), w wide and h tall, full texture. */
   static void quad(VertexConsumer b, Pose pose, Vector3f c, Vector3f n, float w, float h, int color, int light, float u0, float v0, float u1, float v1) {
      Vector3f up = Math.abs(n.y) > 0.5F ? new Vector3f(0, 0, n.y > 0 ? -1 : 1) : new Vector3f(0, 1, 0);
      Vector3f right = new Vector3f(up).cross(n);
      Vector3f hw = new Vector3f(right).mul(w / 2F);
      Vector3f hh = new Vector3f(up).mul(h / 2F);
      Vector3f tl = new Vector3f(c).sub(hw).add(hh);
      Vector3f tr = new Vector3f(c).add(hw).add(hh);
      Vector3f br = new Vector3f(c).add(hw).sub(hh);
      Vector3f bl = new Vector3f(c).sub(hw).sub(hh);
      v(b, pose, tl, u0, v0, n, color, light);
      v(b, pose, bl, u0, v1, n, color, light);
      v(b, pose, br, u1, v1, n, color, light);
      v(b, pose, tr, u1, v0, n, color, light);
   }

   /** Axis-aligned box from (x0,y0,z0) to (x1,y1,z1), solid color (draw with a white texture). */
   static void box(VertexConsumer b, Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int color, int light) {
      float cx = (x0 + x1) / 2F, cy = (y0 + y1) / 2F, cz = (z0 + z1) / 2F;
      float sx = x1 - x0, sy = y1 - y0, sz = z1 - z0;
      quad(b, pose, new Vector3f(cx, cy, z0), new Vector3f(0, 0, -1), sx, sy, shade(color, 0.85F), light, 0, 0, 1, 1);
      quad(b, pose, new Vector3f(cx, cy, z1), new Vector3f(0, 0, 1), sx, sy, shade(color, 0.85F), light, 0, 0, 1, 1);
      quad(b, pose, new Vector3f(x0, cy, cz), new Vector3f(-1, 0, 0), sz, sy, shade(color, 0.7F), light, 0, 0, 1, 1);
      quad(b, pose, new Vector3f(x1, cy, cz), new Vector3f(1, 0, 0), sz, sy, shade(color, 0.7F), light, 0, 0, 1, 1);
      quad(b, pose, new Vector3f(cx, y1, cz), new Vector3f(0, 1, 0), sx, sz, color, light, 0, 0, 1, 1);
      quad(b, pose, new Vector3f(cx, y0, cz), new Vector3f(0, -1, 0), sx, sz, shade(color, 0.5F), light, 0, 0, 1, 1);
   }

   /** Any four points in the xy plane at depth z (two-sided). */
   static void quadXY(VertexConsumer b, Pose pose, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float z, int color, int light) {
      Vector3f n = new Vector3f(0, 0, -1), m = new Vector3f(0, 0, 1);
      v(b, pose, new Vector3f(x1, y1, z), 0, 0, n, color, light);
      v(b, pose, new Vector3f(x2, y2, z), 1, 0, n, color, light);
      v(b, pose, new Vector3f(x3, y3, z), 1, 1, n, color, light);
      v(b, pose, new Vector3f(x4, y4, z), 0, 1, n, color, light);
      v(b, pose, new Vector3f(x4, y4, z), 0, 1, m, color, light);
      v(b, pose, new Vector3f(x3, y3, z), 1, 1, m, color, light);
      v(b, pose, new Vector3f(x2, y2, z), 1, 0, m, color, light);
      v(b, pose, new Vector3f(x1, y1, z), 0, 0, m, color, light);
   }

   /** Any four points in 3D (two-sided). */
   static void quad3(VertexConsumer b, Pose pose, float[] a, float[] c, float[] d, float[] e, int color, int light) {
      Vector3f u = new Vector3f(c[0] - a[0], c[1] - a[1], c[2] - a[2]), w = new Vector3f(e[0] - a[0], e[1] - a[1], e[2] - a[2]);
      Vector3f n = u.cross(w);
      if (n.lengthSquared() < 1e-9F) n.set(0, 0, -1); else n.normalize();
      Vector3f m = new Vector3f(n).negate();
      v(b, pose, new Vector3f(a[0], a[1], a[2]), 0, 0, n, color, light);
      v(b, pose, new Vector3f(c[0], c[1], c[2]), 1, 0, n, color, light);
      v(b, pose, new Vector3f(d[0], d[1], d[2]), 1, 1, n, color, light);
      v(b, pose, new Vector3f(e[0], e[1], e[2]), 0, 1, n, color, light);
      v(b, pose, new Vector3f(e[0], e[1], e[2]), 0, 1, m, color, light);
      v(b, pose, new Vector3f(d[0], d[1], d[2]), 1, 1, m, color, light);
      v(b, pose, new Vector3f(c[0], c[1], c[2]), 1, 0, m, color, light);
      v(b, pose, new Vector3f(a[0], a[1], a[2]), 0, 0, m, color, light);
   }

   /** Disc or ring standing in the y/z plane at x (end caps of a drum). Angle 0 = toward the player (-z). */
   static void ringYZ(VertexConsumer b, Pose pose, float x, float cy, float cz, float r0, float r1, int seg, int color, int light) {
      for (int i = 0; i < seg; i++) {
         double t0 = Math.PI * 2 * i / seg, t1 = Math.PI * 2 * (i + 1) / seg;
         float y0 = (float)Math.sin(t0), z0 = -(float)Math.cos(t0), y1 = (float)Math.sin(t1), z1 = -(float)Math.cos(t1);
         quad3(b, pose, new float[]{x, cy + y0 * r0, cz + z0 * r0}, new float[]{x, cy + y0 * r1, cz + z0 * r1}, new float[]{x, cy + y1 * r1, cz + z1 * r1}, new float[]{x, cy + y1 * r0, cz + z1 * r0}, color, light);
      }
   }

   /** Flat ring (or disc if r0 == 0) facing the player at depth z, from angle a0 to a1 (0 = up, mirrored like the models). */
   static void ring(VertexConsumer b, Pose pose, float cx, float cy, float z, float r0, float r1, double a0, double a1, int seg, int color, int light) {
      for (int i = 0; i < seg; i++) {
         double t0 = a0 + (a1 - a0) * i / seg, t1 = a0 + (a1 - a0) * (i + 1) / seg;
         float s0 = (float)Math.sin(t0), c0 = (float)Math.cos(t0), s1 = (float)Math.sin(t1), c1 = (float)Math.cos(t1);
         quadXY(b, pose, cx - s0 * r0, cy + c0 * r0, cx - s0 * r1, cy + c0 * r1, cx - s1 * r1, cy + c1 * r1, cx - s1 * r0, cy + c1 * r0, z, color, light);
      }
   }

   private static final java.util.Map<Character, String> GLYPH = new java.util.HashMap<>();

   static {
      String[] g = {
         "0:111101101101111", "1:010110010010111", "2:111001111100111", "3:111001111001111", "4:101101111001001",
         "5:111100111001111", "6:111100111101111", "7:111001010010010", "8:111101111101111", "9:111101111001111",
         "B:110101110101110", "I:111010010010111", "G:111100101101111", "W:101101101111101", "H:101101111101101",
         "E:111100110100111", "L:100100100100111", "S:111100111001111", "P:111101111100100", "N:101111111111101",
         "T:111010010010010", "O:111101101101111", "J:001001001101111", "K:101110100110101", "A:010101111101101",
         "C:111100100100111", "R:110101110101101", "!:010010010000010"};
      for (String e : g) GLYPH.put(e.charAt(0), e.substring(2));
   }

   /** 3x5 pixel text along direction (ux,uy) with "up" (vx,vy), on a plane facing the player at depth z. */
   static void text(VertexConsumer b, Pose pose, String s, float ox, float oy, float z, float ux, float uy, float vx, float vy, float px, int color, int light) {
      for (int i = 0; i < s.length(); i++) {
         String gl = GLYPH.get(s.charAt(i));
         if (gl == null) continue;
         for (int r = 0; r < 5; r++) for (int c = 0; c < 3; c++) {
            if (gl.charAt(r * 3 + c) != '1') continue;
            float a = (i * 4 + c) * px, bb = (4 - r) * px, e = px * 0.92F;
            float x0 = ox + ux * a + vx * bb, y0 = oy + uy * a + vy * bb;
            quadXY(b, pose, x0, y0, x0 + ux * e, y0 + uy * e, x0 + ux * e + vx * e, y0 + uy * e + vy * e, x0 + vx * e, y0 + vy * e, z, color, light);
         }
      }
   }

   static int shade(int argb, float f) {
      int r = (int)(((argb >> 16) & 0xFF) * f), g = (int)(((argb >> 8) & 0xFF) * f), bl = (int)((argb & 0xFF) * f);
      return (argb & 0xFF000000) | (r << 16) | (g << 8) | bl;
   }

   private static void v(VertexConsumer b, Pose pose, Vector3f p, float u, float v, Vector3f n, int color, int light) {
      b.addVertex(pose, p.x, p.y, p.z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, n.x, n.y, n.z);
   }

   /** 7-segment number on a vertical panel facing -z (the player). x0 is the leftmost digit as the player sees it. */
   static void digits(VertexConsumer b, Pose p, int value, int count, float x0, float yc, float z, float w, float h, int on, int lightOn, boolean show) {
      int[] seg = {0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F};
      String s = String.valueOf(Math.max(0, value));
      float t = w * 0.18F, hw = w / 2, hh = h / 2;
      for (int i = 0; i < count; i++) {
         int idx = s.length() - count + i;
         int bits = show && idx >= 0 ? seg[s.charAt(idx) - '0'] : 0;
         float cx = x0 - i * (w + 0.5F);    // model -x is the player's right
         float[][] segs = {
            {cx - hw, yc + hh - t, cx + hw, yc + hh}, {cx - hw, yc, cx - hw + t, yc + hh}, {cx - hw, yc - hh, cx - hw + t, yc},
            {cx - hw, yc - hh, cx + hw, yc - hh + t}, {cx + hw - t, yc - hh, cx + hw, yc}, {cx + hw - t, yc, cx + hw, yc + hh},
            {cx - hw, yc - t / 2, cx + hw, yc + t / 2}
         };
         for (int sg = 0; sg < 7; sg++) {
            boolean lit = (bits & (1 << sg)) != 0;
            float[] r = segs[sg];
            box(b, p, r[0], r[1], z - 0.05F, r[2], r[3], z, lit ? on : 0xFF2A0E06, lit ? lightOn : 0);
         }
      }
   }
}
