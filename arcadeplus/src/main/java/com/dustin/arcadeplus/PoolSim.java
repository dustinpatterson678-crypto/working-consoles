package com.dustin.arcadeplus;

/**
 * Pool table physics on the playfield (blocks): u across (-0.82..0.82), v along the table (-1.32..1.32).
 * Balls live in a float array: ball i at [i*4] u, [i*4+1] v, [i*4+2] vu, [i*4+3] vv. Ball 0 is the cue ball.
 */
public final class PoolSim {
   private PoolSim() {
   }

   public static final double U = 0.82, V = 1.32, R = 0.045, POCKET = 0.085;
   public static final double[][] POCKETS = {{-U, -V}, {U, -V}, {-U, V}, {U, V}, {-U - 0.02, 0}, {U + 0.02, 0}};
   private static final int SUB = 8;

   /** Fresh rack: cue ball at the head spot, the triangle at the foot spot with the 8 in the middle. */
   public static void rack(float[] b) {
      java.util.Arrays.fill(b, 0F);
      b[0] = 0F;
      b[1] = (float)(-V / 2);
      int[] order = {1, 9, 2, 10, 8, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15};
      int k = 0;
      double dv = R * 2 * Math.cos(Math.PI / 6) + 0.002;
      for (int row = 0; row < 5; row++) {
         for (int j = 0; j <= row; j++) {
            int n = order[k++];
            b[n * 4] = (float)((j - row / 2.0) * (R * 2 + 0.002));
            b[n * 4 + 1] = (float)(V / 2 + row * dv);
         }
      }
   }

   public static boolean moving(float[] b, int pocketed) {
      for (int i = 0; i < 16; i++) {
         if ((pocketed & (1 << i)) != 0) continue;
         if (Math.abs(b[i * 4 + 2]) > 1e-4 || Math.abs(b[i * 4 + 3]) > 1e-4) return true;
      }
      return false;
   }

   /** One game tick. Returns the new pocketed mask (bits set for balls that dropped); bit 16 set if any ball hit something. */
   public static int step(float[] b, int pocketed) {
      int hit = 0;
      for (int s = 0; s < SUB; s++) {
         for (int i = 0; i < 16; i++) {
            if ((pocketed & (1 << i)) != 0) continue;
            int o = i * 4;
            b[o] += b[o + 2] / SUB;
            b[o + 1] += b[o + 3] / SUB;
            // pockets
            for (double[] pk : POCKETS) {
               if (Math.hypot(b[o] - pk[0], b[o + 1] - pk[1]) < POCKET) {
                  pocketed |= 1 << i;
                  b[o + 2] = 0;
                  b[o + 3] = 0;
                  break;
               }
            }
            if ((pocketed & (1 << i)) != 0) continue;
            // cushions
            if (Math.abs(b[o]) > U - R) {
               b[o] = (float)(Math.signum(b[o]) * (U - R));
               b[o + 2] = -b[o + 2] * 0.78F;
               hit |= 1 << 16;
            }
            if (Math.abs(b[o + 1]) > V - R) {
               b[o + 1] = (float)(Math.signum(b[o + 1]) * (V - R));
               b[o + 3] = -b[o + 3] * 0.78F;
               hit |= 1 << 16;
            }
         }
         // ball against ball (equal masses)
         for (int i = 0; i < 16; i++) {
            if ((pocketed & (1 << i)) != 0) continue;
            for (int j = i + 1; j < 16; j++) {
               if ((pocketed & (1 << j)) != 0) continue;
               int a = i * 4, c = j * 4;
               double dx = b[c] - b[a], dz = b[c + 1] - b[a + 1], d = Math.hypot(dx, dz);
               if (d < 2 * R && d > 1e-6) {
                  double nx = dx / d, nz = dz / d, overlap = 2 * R - d;
                  b[a] -= nx * overlap / 2; b[a + 1] -= nz * overlap / 2;
                  b[c] += nx * overlap / 2; b[c + 1] += nz * overlap / 2;
                  double rel = (b[a + 2] - b[c + 2]) * nx + (b[a + 3] - b[c + 3]) * nz;
                  if (rel > 0) {
                     double k = rel * 0.96;
                     b[a + 2] -= k * nx; b[a + 3] -= k * nz;
                     b[c + 2] += k * nx; b[c + 3] += k * nz;
                     hit |= 1 << 17;
                  }
               }
            }
         }
      }
      // cloth friction
      for (int i = 0; i < 16; i++) {
         int o = i * 4;
         double sp = Math.hypot(b[o + 2], b[o + 3]);
         if (sp < 0.0025) {
            b[o + 2] = 0;
            b[o + 3] = 0;
         } else {
            double ns = Math.max(0, sp * 0.982 - 0.0012);
            b[o + 2] *= ns / sp;
            b[o + 3] *= ns / sp;
         }
      }
      return pocketed | hit;
   }
}
