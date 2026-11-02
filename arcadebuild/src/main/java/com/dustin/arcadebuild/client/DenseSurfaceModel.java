package com.dustin.arcadebuild.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Re-tessellates a block's flat faces into a dense grid and displaces it in world space, so walls warp like old drywall,
 * carpet rises and dips, and ceiling tiles sag in their grid. Baked into the chunk mesh: cheap, and shader packs see real geometry.
 */
public class DenseSurfaceModel extends WrapperBlockStateModel {
   public enum Kind { WALL, CARPET, CEILING }

   private static final int G = 2;
   private final Kind kind;

   public DenseSurfaceModel(BlockStateModel wrapped, Kind kind) {
      super(wrapped);
      this.kind = kind;
   }

   @Override
   public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
      return null;                                           // depends on where the block is
   }

   private static final class Face {
      final float[] x = new float[4], y = new float[4], z = new float[4], u = new float[4], v = new float[4];
      int color;
      Direction cull, nominal;
      ChunkSectionLayer layer;
      TriState ao;
      boolean emissive;
   }

   private boolean wanted(Direction d) {
      if (d == null) return false;
      return switch (this.kind) {
         case WALL -> d.getAxis().isHorizontal();
         case CARPET -> d == Direction.UP;
         case CEILING -> d == Direction.DOWN;
      };
   }

   @Override
   public void emitQuads(QuadEmitter e, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest) {
      List<Face> faces = new ArrayList<>(6);
      e.pushTransform(q -> {
         Direction n = q.nominalFace();
         if (!this.wanted(n)) return true;
         float minX = 9, maxX = -9, minY = 9, maxY = -9, minZ = 9, maxZ = -9;
         for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, q.x(i)); maxX = Math.max(maxX, q.x(i));
            minY = Math.min(minY, q.y(i)); maxY = Math.max(maxY, q.y(i));
            minZ = Math.min(minZ, q.z(i)); maxZ = Math.max(maxZ, q.z(i));
         }
         float ex = maxX - minX, ey = maxY - minY, ez = maxZ - minZ;
         boolean full = switch (n.getAxis()) {
            case X -> ex < 1.0E-3F && ey > 0.999F && ez > 0.999F;
            case Y -> ey < 1.0E-3F && ex > 0.999F && ez > 0.999F;
            case Z -> ez < 1.0E-3F && ex > 0.999F && ey > 0.999F;
         };
         if (!full) return true;
         Face f = new Face();
         for (int i = 0; i < 4; i++) {
            f.x[i] = q.x(i); f.y[i] = q.y(i); f.z[i] = q.z(i); f.u[i] = q.u(i); f.v[i] = q.v(i);
         }
         f.color = q.color(0);
         f.cull = q.cullFace();
         f.nominal = n;
         f.layer = q.chunkLayer();
         f.ao = q.ambientOcclusion();
         f.emissive = q.emissive();
         faces.add(f);
         return false;
      });
      try {
         this.wrapped.emitQuads(e, level, pos, state, random, cullTest);
      } finally {
         e.popTransform();
      }
      for (Face f : faces) this.tessellate(e, f, pos);
   }

   private void tessellate(QuadEmitter e, Face f, BlockPos pos) {
      float nx = f.nominal.getStepX(), ny = f.nominal.getStepY(), nz = f.nominal.getStepZ();
      int n1 = G + 1;
      float[][] px = new float[n1][n1], py = new float[n1][n1], pz = new float[n1][n1], pu = new float[n1][n1], pv = new float[n1][n1];
      for (int i = 0; i <= G; i++) for (int j = 0; j <= G; j++) {
         float s = (float)i / G, t = (float)j / G;
         float w0 = (1 - s) * (1 - t), w1 = s * (1 - t), w2 = s * t, w3 = (1 - s) * t;
         float x = w0 * f.x[0] + w1 * f.x[1] + w2 * f.x[2] + w3 * f.x[3];
         float y = w0 * f.y[0] + w1 * f.y[1] + w2 * f.y[2] + w3 * f.y[3];
         float z = w0 * f.z[0] + w1 * f.z[1] + w2 * f.z[2] + w3 * f.z[3];
         float d = this.disp(pos.getX() + x, pos.getY() + y, pos.getZ() + z, s, t);
         px[i][j] = x + nx * d;
         py[i][j] = y + ny * d;
         pz[i][j] = z + nz * d;
         pu[i][j] = w0 * f.u[0] + w1 * f.u[1] + w2 * f.u[2] + w3 * f.u[3];
         pv[i][j] = w0 * f.v[0] + w1 * f.v[1] + w2 * f.v[2] + w3 * f.v[3];
      }
      float[][][] nn = new float[n1][n1][];
      for (int i = 0; i <= G; i++) for (int j = 0; j <= G; j++) {
         int a = Math.max(0, i - 1), b = Math.min(G, i + 1), c = Math.max(0, j - 1), dd = Math.min(G, j + 1);
         float sx = px[b][j] - px[a][j], sy = py[b][j] - py[a][j], sz = pz[b][j] - pz[a][j];
         float tx = px[i][dd] - px[i][c], ty = py[i][dd] - py[i][c], tz = pz[i][dd] - pz[i][c];
         float cx = sy * tz - sz * ty, cy = sz * tx - sx * tz, cz = sx * ty - sy * tx;
         float l = (float)Math.sqrt(cx * cx + cy * cy + cz * cz);
         if (l < 1.0E-6F) { cx = nx; cy = ny; cz = nz; l = 1; }
         if (cx * nx + cy * ny + cz * nz < 0) l = -l;
         nn[i][j] = new float[]{cx / l, cy / l, cz / l};
      }
      int[][] corner = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
      for (int i = 0; i < G; i++) for (int j = 0; j < G; j++) {
         for (int k = 0; k < 4; k++) {
            int ii = i + corner[k][0], jj = j + corner[k][1];
            e.pos(k, px[ii][jj], py[ii][jj], pz[ii][jj]);
            e.uv(k, pu[ii][jj], pv[ii][jj]);
            e.normal(k, nn[ii][jj][0], nn[ii][jj][1], nn[ii][jj][2]);
            e.color(k, f.color);
         }
         e.atlas(QuadAtlas.BLOCK);
         e.cullFace(f.cull);
         e.nominalFace(f.nominal);
         if (f.layer != null) e.chunkLayer(f.layer);
         e.ambientOcclusion(f.ao);
         e.emissive(f.emissive);
         e.emit();
      }
   }

   private float disp(float wx, float wy, float wz, float s, float t) {
      float edge = Math.min(Math.min(s, 1 - s), Math.min(t, 1 - t));
      float taper = smooth(Math.min(1.0F, edge * 7.0F));
      return switch (this.kind) {
         case WALL -> 0.016F * taper * noise(wx * 0.45F, wy * 0.45F, wz * 0.45F);
         case CARPET -> 0.009F * taper * (0.6F * noise(wx * 2.3F, wy, wz * 2.3F) + 0.4F * noise(wx * 0.6F + 9, wy, wz * 0.6F));
         case CEILING -> 0.022F * (float)(Math.sin(Math.PI * s) * Math.sin(Math.PI * t)) * (0.55F + 0.45F * noise(wx * 0.8F, wy, wz * 0.8F));
      };
   }

   private static float smooth(float x) {
      return x * x * (3 - 2 * x);
   }

   private static float hash(int x, int y, int z) {
      int h = x * 374761393 + y * 668265263 + z * 1442695041;
      h = (h ^ (h >>> 13)) * 1274126177;
      return ((h ^ (h >>> 16)) & 0xFFFF) / 32767.5F - 1.0F;
   }

   /** Smooth value noise, -1..1. */
   static float noise(float x, float y, float z) {
      int xi = (int)Math.floor(x), yi = (int)Math.floor(y), zi = (int)Math.floor(z);
      float fx = smooth(x - xi), fy = smooth(y - yi), fz = smooth(z - zi);
      float c000 = hash(xi, yi, zi), c100 = hash(xi + 1, yi, zi), c010 = hash(xi, yi + 1, zi), c110 = hash(xi + 1, yi + 1, zi);
      float c001 = hash(xi, yi, zi + 1), c101 = hash(xi + 1, yi, zi + 1), c011 = hash(xi, yi + 1, zi + 1), c111 = hash(xi + 1, yi + 1, zi + 1);
      float x00 = c000 + (c100 - c000) * fx, x10 = c010 + (c110 - c010) * fx, x01 = c001 + (c101 - c001) * fx, x11 = c011 + (c111 - c011) * fx;
      float y0 = x00 + (x10 - x00) * fy, y1 = x01 + (x11 - x01) * fy;
      return y0 + (y1 - y0) * fz;
   }
}
