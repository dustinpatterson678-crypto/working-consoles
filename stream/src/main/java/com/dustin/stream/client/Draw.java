package com.dustin.stream.client;

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

   static int shade(int argb, float f) {
      int r = (int)(((argb >> 16) & 0xFF) * f), g = (int)(((argb >> 8) & 0xFF) * f), bl = (int)((argb & 0xFF) * f);
      return (argb & 0xFF000000) | (r << 16) | (g << 8) | bl;
   }

   private static void v(VertexConsumer b, Pose pose, Vector3f p, float u, float v, Vector3f n, int color, int light) {
      b.addVertex(pose, p.x, p.y, p.z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, n.x, n.y, n.z);
   }
}
