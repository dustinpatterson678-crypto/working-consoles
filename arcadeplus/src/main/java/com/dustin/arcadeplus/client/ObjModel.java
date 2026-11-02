package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.ArcadePlusMod;
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
import net.minecraft.resources.Identifier;

/**
 * Realistic machine models: smooth OBJ meshes (assets/arcadeplus/models/obj/NAME.obj) with painted textures
 * (assets/arcadeplus/textures/obj/MATERIAL.png). Each "o" group can be drawn on its own with its own transform
 * and light, so wheels spin, flappers kick and bulbs chase.
 */
public final class ObjModel {
   private static final Map<String, ObjModel> CACHE = new HashMap<>();

   /** group -> material -> triangles as x,y,z,u,v,nx,ny,nz per vertex */
   private final Map<String, Map<String, float[]>> groups = new LinkedHashMap<>();

   public static ObjModel get(String name) {
      return CACHE.computeIfAbsent(name, ObjModel::load);
   }

   public boolean has(String group) {
      return this.groups.containsKey(group);
   }

   private static ObjModel load(String name) {
      ObjModel m = new ObjModel();
      Identifier id = ArcadePlusMod.id("models/obj/" + name + ".obj");
      List<float[]> v = new ArrayList<>(), t = new ArrayList<>(), n = new ArrayList<>();
      Map<String, Map<String, List<Float>>> acc = new LinkedHashMap<>();
      String group = "static", mat = "black";
      try (BufferedReader r = new BufferedReader(new InputStreamReader(Minecraft.getInstance().getResourceManager().getResource(id).orElseThrow().open(), StandardCharsets.UTF_8))) {
         String line;
         while ((line = r.readLine()) != null) {
            if (line.isEmpty()) continue;
            String[] p = line.trim().split("\\s+");
            switch (p[0]) {
               case "v" -> v.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
               case "vt" -> t.add(new float[]{Float.parseFloat(p[1]), 1F - Float.parseFloat(p[2])});   // back to image coordinates
               case "vn" -> n.add(new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3])});
               case "o", "g" -> group = p.length > 1 ? p[1] : "static";
               case "usemtl" -> mat = p[1];
               case "f" -> {
                  List<Float> out = acc.computeIfAbsent(group, k -> new LinkedHashMap<>()).computeIfAbsent(mat, k -> new ArrayList<>());
                  for (int i = 2; i + 1 < p.length; i++) {                      // fan into triangles
                     for (String c : new String[]{p[1], p[i], p[i + 1]}) {
                        String[] q = c.split("/");
                        float[] pv = v.get(Integer.parseInt(q[0]) - 1);
                        float[] tv = q.length > 1 && !q[1].isEmpty() ? t.get(Integer.parseInt(q[1]) - 1) : new float[]{0, 0};
                        float[] nv = q.length > 2 ? n.get(Integer.parseInt(q[2]) - 1) : new float[]{0, 1, 0};
                        out.add(pv[0]); out.add(pv[1]); out.add(pv[2]); out.add(tv[0]); out.add(tv[1]); out.add(nv[0]); out.add(nv[1]); out.add(nv[2]);
                     }
                  }
               }
               default -> { }
            }
         }
      } catch (Exception e) {
         ArcadePlusMod.LOGGER.warn("Could not load model {}: {}", id, e.toString());
      }
      for (Map.Entry<String, Map<String, List<Float>>> g : acc.entrySet()) {
         Map<String, float[]> mats = new LinkedHashMap<>();
         for (Map.Entry<String, List<Float>> e : g.getValue().entrySet()) {
            float[] a = new float[e.getValue().size()];
            for (int i = 0; i < a.length; i++) a[i] = e.getValue().get(i);
            mats.put(e.getKey(), a);
         }
         m.groups.put(g.getKey(), mats);
      }
      return m;
   }

   private static Identifier tex(String mat) {
      return ArcadePlusMod.id("textures/obj/" + mat + ".png");
   }

   /** Draw one group with the current pose. tint is ARGB multiplied into the texture. */
   public void draw(PoseStack pose, SubmitNodeCollector collector, String group, int light, int tint) {
      Map<String, float[]> mats = this.groups.get(group);
      if (mats == null) return;
      for (Map.Entry<String, float[]> e : mats.entrySet()) {
         final float[] d = e.getValue();
         boolean glass = e.getKey().contains("glass");
         collector.submitCustomGeometry(pose, glass ? RenderTypes.entityTranslucent(tex(e.getKey())) : RenderTypes.entityCutout(tex(e.getKey())), (p, b) -> tris(p, b, d, light, tint));
      }
   }

   /** Draw every group whose name starts with prefix (static parts). */
   public void drawPrefix(PoseStack pose, SubmitNodeCollector collector, String prefix, int light, int tint) {
      for (String g : this.groups.keySet()) if (g.startsWith(prefix)) this.draw(pose, collector, g, light, tint);
   }

   private static void tris(Pose p, VertexConsumer b, float[] d, int light, int tint) {
      for (int i = 0; i + 23 < d.length; i += 24) {
         vert(p, b, d, i, light, tint);
         vert(p, b, d, i + 8, light, tint);
         vert(p, b, d, i + 16, light, tint);
         vert(p, b, d, i + 16, light, tint);                      // triangle as a degenerate quad
      }
   }

   private static void vert(Pose p, VertexConsumer b, float[] d, int i, int light, int tint) {
      b.addVertex(p, d[i], d[i + 1], d[i + 2]).setColor(tint).setUv(d[i + 3], d[i + 4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, d[i + 5], d[i + 6], d[i + 7]);
   }
}
