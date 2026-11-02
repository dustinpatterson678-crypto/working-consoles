package com.dustin.wcore.client;

import com.dustin.wcore.ConsoleSettings;
import com.dustin.wcore.WorkingConsolesCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class Cables {
   private static final Identifier TEX = WorkingConsolesCore.id("textures/block/cable_wire.png");
   private static final int SEGMENTS = 16;
   private static final float HALF_WIDTH = 0.022F;

   private Cables() {
   }

   public static Vec3 port(BlockPos self, BlockPos block, Direction facing, double back, double height) {
      Vec3 n = new Vec3(facing.getStepX(), 0.0, facing.getStepZ());
      return new Vec3(block.getX() - self.getX() + 0.5, block.getY() - self.getY() + height, block.getZ() - self.getZ() + 0.5).subtract(n.scale(back));
   }

   public static void draw(PoseStack poseStack, SubmitNodeCollector collector, BlockPos origin, Vec3 from, Vec3 to, int light) {
      String style = ConsoleSettings.get().cables;
      if (!"hidden".equals(style) && !throughWall(origin, from, to)) {
         if ("sag".equals(style)) {
            draw(poseStack, collector, from, to, light);
         } else {
            double floor = Math.min(Math.floor(from.y), Math.floor(to.y)) + 0.025;
            List<Vec3> pts = new ArrayList<>();
            pts.add(from);
            pts.add(new Vec3(from.x, floor, from.z));
            pts.add(new Vec3(to.x, floor, from.z));
            pts.add(new Vec3(to.x, floor, to.z));
            pts.add(to);
            ribbon(poseStack, collector, pts, light);
         }
      }
   }

   private static boolean throughWall(BlockPos origin, Vec3 from, Vec3 to) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && origin != null) {
         double len = from.distanceTo(to);
         BlockPos a = origin.offset(BlockPos.containing(from));
         BlockPos b = origin.offset(BlockPos.containing(to));

         for (double t = 0.1; t < len - 0.1; t += 0.25) {
            Vec3 p = from.lerp(to, t / len);
            BlockPos at = origin.offset(BlockPos.containing(p));
            if (!at.equals(a) && !at.equals(b) && level.getBlockState(at).isCollisionShapeFullBlock(level, at)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static void draw(PoseStack poseStack, SubmitNodeCollector collector, Vec3 from, Vec3 to, int light) {
      double dist = from.distanceTo(to);
      if (!(dist < 0.01)) {
         double sag = 0.08 + dist * 0.07;
         List<Vec3> pts = new ArrayList<>();

         for (int i = 0; i <= 16; i++) {
            double t = i / 16.0;
            pts.add(from.lerp(to, t).subtract(0.0, sag * 4.0 * t * (1.0 - t), 0.0));
         }

         ribbon(poseStack, collector, pts, light);
      }
   }

   private static void ribbon(PoseStack poseStack, SubmitNodeCollector collector, List<Vec3> pts, int light) {
      collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEX), (pose, buffer) -> {
         for (int i = 0; i < pts.size() - 1; i++) {
            Vec3 a = pts.get(i);
            Vec3 b = pts.get(i + 1);
            Vec3 dir = b.subtract(a);
            if (!(dir.lengthSqr() < 1.0E-6)) {
               dir = dir.normalize();
               Vec3 side = dir.cross(new Vec3(0.0, 1.0, 0.0));
               side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
               Vec3 other = dir.cross(side).normalize();
               Vec3 sideW = side.scale(0.022F);
               Vec3 upW = other.scale(0.022F);
               quad(buffer, pose, a.add(sideW), b.add(sideW), b.subtract(sideW), a.subtract(sideW), (float)other.x, (float)other.y, (float)other.z, light);
               quad(buffer, pose, a.add(upW), b.add(upW), b.subtract(upW), a.subtract(upW), (float)side.x, (float)side.y, (float)side.z, light);
            }
         }
      });
   }

   private static void quad(VertexConsumer buffer, Pose pose, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float nx, float ny, float nz, int light) {
      v(buffer, pose, p0, 0.0F, 0.0F, nx, ny, nz, light);
      v(buffer, pose, p1, 1.0F, 0.0F, nx, ny, nz, light);
      v(buffer, pose, p2, 1.0F, 1.0F, nx, ny, nz, light);
      v(buffer, pose, p3, 0.0F, 1.0F, nx, ny, nz, light);
   }

   private static void v(VertexConsumer buffer, Pose pose, Vec3 p, float u, float vv, float nx, float ny, float nz, int light) {
      buffer.addVertex(pose, (float)p.x, (float)p.y, (float)p.z)
         .setColor(-1)
         .setUv(u, vv)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(pose, nx, ny, nz);
   }
}
