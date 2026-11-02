package com.dustin.arcadebuild.client;

import com.dustin.arcadebuild.ArcadeBuildMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;

/**
 * A big OBJ model lit by the real world light: every triangle takes the light just in front of its face (refreshed
 * every two seconds), so buildings shade like blocks under shaders and day/night. Group "glow" is always full bright.
 */
public final class LitMesh {
   private static final int FULL = 15728880;
   private static final Map<String, LitMesh> CACHE = new HashMap<>();
   private final Map<String, float[]> lit = new LinkedHashMap<>(), glow = new LinkedHashMap<>();
   private final String folder;
   private final Map<Long, int[][]> lights = new HashMap<>();
   private final Map<Long, Long> litAt = new HashMap<>();

   /** World position of a model point (for light sampling). */
   public interface ToWorld {
      BlockPos at(double x, double y, double z);
   }

   private LitMesh(String folder) {
      this.folder = folder;
   }

   public static LitMesh get(String name, String folder) {
      return CACHE.computeIfAbsent(name, n -> load(n, folder));
   }

   private static LitMesh load(String name, String folder) {
      LitMesh out = new LitMesh(folder);
      Map<String, List<Float>> r = new LinkedHashMap<>(), g = new LinkedHashMap<>();
      List<float[]> v = new ArrayList<>(), t = new ArrayList<>(), n = new ArrayList<>();
      String group = "static", mat = "x_dark";
      Identifier id = ArcadeBuildMod.id("models/obj/" + name + ".obj");
      try (BufferedReader in = new BufferedReader(new InputStreamReader(Minecraft.getInstance().getResourceManager().getResource(id).orElseThrow().open(), StandardCharsets.UTF_8))) {
         String line;
         while ((line = in.readLine()) != null) {
            if (line.isEmpty()) continue;
            String[] p = line.trim().split("\\s+");
            switch (p[0]) {
               case "v" -> v.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
               case "vt" -> t.add(new float[]{Float.parseFloat(p[1]), 1F - Float.parseFloat(p[2])});
               case "vn" -> n.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
               case "o", "g" -> group = p.length > 1 ? p[1] : "static";
               case "usemtl" -> mat = p[1];
               case "f" -> {
                  List<Float> o = (group.equals("glow") ? g : r).computeIfAbsent(mat, k -> new ArrayList<>());
                  for (int i = 2; i + 1 < p.length; i++) {
                     for (String c : new String[]{p[1], p[i], p[i + 1]}) {
                        String[] q = c.split("/");
                        float[] pv = v.get(Integer.parseInt(q[0]) - 1);
                        float[] tv = q.length > 1 && !q[1].isEmpty() ? t.get(Integer.parseInt(q[1]) - 1) : new float[]{0, 0};
                        float[] nv = q.length > 2 ? n.get(Integer.parseInt(q[2]) - 1) : new float[]{0, 1, 0};
                        o.add(pv[0]); o.add(pv[1]); o.add(pv[2]); o.add(tv[0]); o.add(tv[1]); o.add(nv[0]); o.add(nv[1]); o.add(nv[2]);
                     }
                  }
               }
               default -> { }
            }
         }
      } catch (Exception e) {
         ArcadeBuildMod.LOGGER.warn("Could not load model {}: {}", id, e.toString());
      }
      pack(r, out.lit);
      pack(g, out.glow);
      return out;
   }

   private static void pack(Map<String, List<Float>> m, Map<String, float[]> out) {
      for (Map.Entry<String, List<Float>> e : m.entrySet()) {
         float[] a = new float[e.getValue().size()];
         for (int i = 0; i < a.length; i++) a[i] = e.getValue().get(i);
         out.put(e.getKey(), a);
      }
   }

   private int[][] lights(Level level, long key, ToWorld w) {
      long now = level.getGameTime();
      int[][] l = this.lights.get(key);
      Long at = this.litAt.get(key);
      if (l != null && at != null && now - at < 40 && now >= at) return l;
      l = new int[this.lit.size()][];
      int k = 0;
      for (float[] d : this.lit.values()) {
         int[] li = new int[d.length / 24];
         for (int t = 0; t < li.length; t++) {
            int i = t * 24;
            double cx = (d[i] + d[i + 8] + d[i + 16]) / 3 + d[i + 5] * 0.5, cy = (d[i + 1] + d[i + 9] + d[i + 17]) / 3 + d[i + 6] * 0.5, cz = (d[i + 2] + d[i + 10] + d[i + 18]) / 3 + d[i + 7] * 0.5;
            li[t] = LightCoordsUtil.getLightCoords(level, w.at(cx, cy, cz));
         }
         l[k++] = li;
      }
      this.lights.put(key, l);
      this.litAt.put(key, now);
      return l;
   }

   /** Draw with the current pose (already placed in model space). key: one light cache per placed copy. */
   public void draw(PoseStack pose, SubmitNodeCollector collector, Level level, long key, ToWorld w, boolean glowOn, int dimLight) {
      int[][] l = this.lights(level, key, w);
      int k = 0;
      for (Map.Entry<String, float[]> e : this.lit.entrySet()) {
         final float[] d = e.getValue();
         final int[] li = l[k++];
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(this.tex(e.getKey())), (p, b) -> tris(p, b, d, li, 0));
      }
      final int gl = glowOn ? FULL : dimLight;
      for (Map.Entry<String, float[]> e : this.glow.entrySet()) {
         final float[] d = e.getValue();
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(this.tex(e.getKey())), (p, b) -> tris(p, b, d, null, gl));
      }
   }

   private Identifier tex(String mat) {
      return ArcadeBuildMod.id("textures/" + this.folder + "/" + mat + ".png");
   }

   private static void tris(Pose p, VertexConsumer b, float[] d, int[] li, int full) {
      for (int i = 0, t = 0; i + 23 < d.length; i += 24, t++) {
         int light = li == null ? full : li[t];
         vert(p, b, d, i, light);
         vert(p, b, d, i + 8, light);
         vert(p, b, d, i + 16, light);
         vert(p, b, d, i + 16, light);
      }
   }

   private static void vert(Pose p, VertexConsumer b, float[] d, int i, int light) {
      b.addVertex(p, d[i], d[i + 1], d[i + 2]).setColor(0xFFFFFFFF).setUv(d[i + 3], d[i + 4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, d[i + 5], d[i + 6], d[i + 7]);
   }
}
