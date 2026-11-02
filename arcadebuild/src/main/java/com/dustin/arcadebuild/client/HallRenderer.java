package com.dustin.arcadebuild.client;

import com.dustin.arcadebuild.ArcadeBuildMod;
import com.dustin.arcadebuild.GrandHall;
import com.dustin.arcadebuild.HallCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the Grand Hall: one smooth model (carpet, walls, drop ceiling, light panels, neon, signs, doors) lit by
 * the real light in the room, so shaders and dynamic lights see a normal lit interior.
 */
public class HallRenderer implements BlockEntityRenderer<HallCoreBlockEntity, BlockEntityRenderState> {
   private static final int FULL = 15728880;

   /** material -> triangles (x, y, z, u, v, nx, ny, nz per vertex), split into the lit room and the glowing parts. */
   private static Map<String, float[]> room, glow;
   private static final Map<Long, int[][]> LIGHTS = new java.util.HashMap<>();
   private static final Map<Long, Long> LIT_AT = new java.util.HashMap<>();

   public HallRenderer(BlockEntityRendererProvider.Context context) {
   }

   @Override
   public BlockEntityRenderState createRenderState() {
      return new BlockEntityRenderState();
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return true;
   }

   @Override
   public int getViewDistance() {
      return 160;
   }

   @Override
   public boolean shouldRender(HallCoreBlockEntity be, Vec3 camera) {
      return true;
   }

   private static void load() {
      if (room != null) return;
      Map<String, List<Float>> r = new LinkedHashMap<>(), g = new LinkedHashMap<>();
      List<float[]> v = new ArrayList<>(), t = new ArrayList<>(), n = new ArrayList<>();
      String group = "static", mat = "h_black";
      Identifier id = ArcadeBuildMod.id("models/obj/grand_hall.obj");
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
                  List<Float> out = (group.equals("glow") ? g : r).computeIfAbsent(mat, k -> new ArrayList<>());
                  for (int i = 2; i + 1 < p.length; i++) {
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
         ArcadeBuildMod.LOGGER.warn("Could not load the Grand Hall model: {}", e.toString());
      }
      room = pack(r);
      glow = pack(g);
   }

   private static Map<String, float[]> pack(Map<String, List<Float>> m) {
      Map<String, float[]> out = new LinkedHashMap<>();
      for (Map.Entry<String, List<Float>> e : m.entrySet()) {
         float[] a = new float[e.getValue().size()];
         for (int i = 0; i < a.length; i++) a[i] = e.getValue().get(i);
         out.put(e.getKey(), a);
      }
      return out;
   }

   /** Light for every triangle, sampled just in front of its face; refreshed every two seconds. */
   private static int[][] lights(Level level, HallCoreBlockEntity be, BlockPos origin) {
      long key = be.getBlockPos().asLong(), now = level.getGameTime();
      int[][] l = LIGHTS.get(key);
      Long at = LIT_AT.get(key);
      if (l != null && at != null && now - at < 40 && now >= at) return l;
      l = new int[room.size()][];
      int k = 0;
      BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
      for (float[] d : room.values()) {
         int[] li = new int[d.length / 24];
         for (int t = 0; t < li.length; t++) {
            int i = t * 24;
            double cx = (d[i] + d[i + 8] + d[i + 16]) / 3 + d[i + 5] * 0.5, cy = (d[i + 1] + d[i + 9] + d[i + 17]) / 3 + d[i + 6] * 0.5, cz = (d[i + 2] + d[i + 10] + d[i + 18]) / 3 + d[i + 7] * 0.5;
            mp.set(origin.getX() + Math.floor(cx + 0.5), origin.getY() + Math.floor(cy), origin.getZ() + Math.floor(cz + 0.5));
            li[t] = LightCoordsUtil.getLightCoords(level, mp);
         }
         l[k++] = li;
      }
      LIGHTS.put(key, l);
      LIT_AT.put(key, now);
      return l;
   }

   @Override
   public void submit(BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      Minecraft mc = Minecraft.getInstance();
      Level level = mc.level;
      if (level == null || state.blockPos == null || !(level.getBlockEntity(state.blockPos) instanceof HallCoreBlockEntity be)) return;
      load();
      BlockPos origin = be.origin();
      int[][] lights = lights(level, be, origin);
      pose.pushPose();
      pose.translate(origin.getX() - state.blockPos.getX() + 0.5F, origin.getY() - state.blockPos.getY(), origin.getZ() - state.blockPos.getZ() + 0.5F);
      int k = 0;
      for (Map.Entry<String, float[]> e : room.entrySet()) {
         final float[] d = e.getValue();
         final int[] li = lights[k++];
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(GrandHall.hallTexture(e.getKey())), (p, b) -> tris(p, b, d, li, 0));
      }
      for (Map.Entry<String, float[]> e : glow.entrySet()) {
         final float[] d = e.getValue();
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(GrandHall.hallTexture(e.getKey())), (p, b) -> tris(p, b, d, null, FULL));
      }
      pose.popPose();
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
