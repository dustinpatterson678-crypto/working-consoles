package com.dustin.arcadebuild.client;

import com.dustin.arcadebuild.BuildingPlan;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * The 1.1 storefront. This is deliberately a real strip-mall elevation, not a box: a deep dark-brown
 * fascia with a lit sign, full-height glass bays with bronze frames, a recessed entry, parking-lot
 * stripes, a pylon sign, roof units and all the small utility details that make a building read as real.
 */
final class OhWowFront {
   private OhWowFront() {}

   static void draw(VertexConsumer b, Pose p, FacadeRenderer.Frame fr, BuildingPlan plan, long ms,
                    boolean on, int light, int inLight, int glow, float doorOpen) {
      final int HX = plan.HX, sx = plan.sx(), BK = plan.BACK;
      final int stucco = 0xFFC9B18C, brown = 0xFF4A3428, dark = 0xFF1A1210, cap = 0xFF2E211A;
      final int frame = 0xFF2A2622, concrete = 0xFFB8B4AC, teal = 0xFF2AD4D0, red = 0xFFFF5A2A;

      // deep fascia band and its cap
      FacadeRenderer.box(b, p, fr, -sx - 0.5, 6.90, -1.75, sx + 0.5, 9.00, -1.45, brown, light);
      FacadeRenderer.box(b, p, fr, -sx - 0.6, 9.00, -1.82, sx + 0.6, 9.22, -1.40, cap, light);
      FacadeRenderer.box(b, p, fr, -sx - 0.5, 6.90, -1.755, sx + 0.5, 7.00, -1.745, 0xFF75553E, light);
      for (int x = -sx + 4; x < sx; x += 4) {
         FacadeRenderer.box(b, p, fr, x - 0.035, 6.98, -1.760, x + 0.035, 8.96, -1.748, 0xFF35241A, light);
      }
      // wrap the band around the sides and back
      FacadeRenderer.box(b, p, fr, -sx - 0.55, 7.00, -1.50, -sx - 0.45, 9.05, BK + 1.55, brown, light);
      FacadeRenderer.box(b, p, fr, sx + 0.45, 7.00, -1.50, sx + 0.55, 9.05, BK + 1.55, brown, light);
      FacadeRenderer.box(b, p, fr, -sx - 0.55, 7.00, BK + 1.45, sx + 0.55, 9.05, BK + 1.55, brown, light);
      FacadeRenderer.box(b, p, fr, -sx - 0.62, 9.00, -1.85, sx + 0.62, 9.22, -1.38, cap, light);
      FacadeRenderer.box(b, p, fr, -sx - 0.62, 9.00, -1.38, -sx - 0.42, 9.22, BK + 1.60, cap, light);
      FacadeRenderer.box(b, p, fr, sx + 0.42, 9.00, -1.38, sx + 0.62, 9.22, BK + 1.60, cap, light);
      FacadeRenderer.box(b, p, fr, -sx - 0.62, 9.00, BK + 1.58, sx + 0.62, 9.22, BK + 1.78, cap, light);

      // corner pilasters and the two piers framing the door
      FacadeRenderer.box(b, p, fr, -sx - 0.5, 1.0, -1.70, -sx + 0.6, 6.92, -1.43, stucco, light);
      FacadeRenderer.box(b, p, fr, sx - 0.6, 1.0, -1.70, sx + 0.5, 6.92, -1.43, stucco, light);
      FacadeRenderer.box(b, p, fr, -3.35, 1.0, -1.68, -1.65, 6.92, -1.43, stucco, light);
      FacadeRenderer.box(b, p, fr, 1.65, 1.0, -1.68, 3.35, 6.92, -1.43, stucco, light);

      // three glass bays: bronze frames, kickplates, transoms and mullions
      for (int[] w : plan.windows()) {
         double x0 = w[0], x1 = w[1];
         FacadeRenderer.box(b, p, fr, x0 - 0.45, 1.0, -1.60, x0 - 0.25, 6.92, -0.46, frame, light);
         FacadeRenderer.box(b, p, fr, x1 + 0.25, 1.0, -1.60, x1 + 0.45, 6.92, -0.46, frame, light);
         FacadeRenderer.box(b, p, fr, x0 - 0.45, 6.72, -1.60, x1 + 0.45, 6.92, -0.46, frame, light);
         FacadeRenderer.box(b, p, fr, x0 - 0.45, 1.00, -1.60, x1 + 0.45, 1.34, -0.46, frame, light);
         FacadeRenderer.box(b, p, fr, x0 - 0.42, 5.02, -1.59, x1 + 0.42, 5.16, -0.47, frame, light);
         for (int x = w[0]; x <= w[1]; x += 2) {
            FacadeRenderer.box(b, p, fr, x - 0.055, 1.34, -1.585, x + 0.055, 6.72, -0.475, frame, light);
         }
         // subtle warm reflection line on the glass
         FacadeRenderer.box(b, p, fr, x0 + 0.7, 3.4, -0.505, x0 + 1.2, 4.9, -0.500, 0x30E8F0FF, light);
      }

      // recessed doorway: jambs, header, threshold, black entrance mat
      FacadeRenderer.box(b, p, fr, -1.82, 1.0, -1.64, -1.55, 4.16, -1.34, frame, light);
      FacadeRenderer.box(b, p, fr, 1.55, 1.0, -1.64, 1.82, 4.16, -1.34, frame, light);
      FacadeRenderer.box(b, p, fr, -1.82, 4.00, -1.64, 1.82, 4.24, -1.34, frame, light);
      FacadeRenderer.box(b, p, fr, -1.82, 1.00, -1.15, 1.82, 1.055, -0.90, concrete, light);
      FacadeRenderer.box(b, p, fr, -1.45, 1.005, -2.05, 1.45, 1.018, 0.80, 0xFF202020, light);

      // sign backing, border and gooseneck lamps
      FacadeRenderer.box(b, p, fr, -9.30, 7.00, -1.84, -1.70, 8.60, -1.74, dark, light);
      FacadeRenderer.box(b, p, fr, -9.36, 8.54, -1.86, -1.64, 8.62, -1.72, teal, glow);
      FacadeRenderer.box(b, p, fr, -9.36, 6.98, -1.86, -1.64, 7.06, -1.72, teal, glow);
      FacadeRenderer.box(b, p, fr, -9.36, 7.04, -1.86, -9.28, 8.56, -1.72, teal, glow);
      FacadeRenderer.box(b, p, fr, -1.72, 7.04, -1.86, -1.64, 8.56, -1.72, teal, glow);
      for (int x : new int[]{-8, -3, 3, 8}) {
         FacadeRenderer.box(b, p, fr, x - 0.16, 6.10, -1.72, x + 0.16, 6.36, -1.56, dark, light);
         FacadeRenderer.box(b, p, fr, x - 0.13, 6.29, -1.76, x + 0.13, 6.37, -1.58, 0xFFFFE0A0, glow);
      }

      // parking apron: curb, wheel stops and painted stalls
      FacadeRenderer.box(b, p, fr, -sx - 2.5, 1.00, -4.66, sx + 2.5, 1.17, -4.36, concrete, light);
      for (int x = -18; x <= 18; x += 4) {
         FacadeRenderer.box(b, p, fr, x - 0.06, 1.018, -16.0, x + 0.06, 1.036, -5.0, 0xFFE8E4D8, light);
         FacadeRenderer.box(b, p, fr, x - 0.58, 1.00, -5.62, x + 0.58, 1.14, -5.24, concrete, light);
      }
      FacadeRenderer.box(b, p, fr, -sx - 2.5, 1.00, -16.2, sx + 2.5, 1.06, -16.0, 0xFF8A867E, light);

      // pylon sign at the street edge
      FacadeRenderer.box(b, p, fr, -16.35, 1.0, -10.62, -15.65, 3.20, -10.00, 0xFF3A3A3A, light);
      FacadeRenderer.box(b, p, fr, -15.90, 1.0, -10.42, -16.10, 1.25, -10.10, concrete, light);
      FacadeRenderer.box(b, p, fr, -18.30, 2.90, -10.50, -13.70, 5.30, -10.00, dark, light);
      FacadeRenderer.box(b, p, fr, -18.36, 5.24, -10.55, -13.64, 5.36, -9.96, teal, glow);
      FacadeRenderer.box(b, p, fr, -18.36, 2.84, -10.55, -13.64, 2.96, -9.96, teal, glow);
      FacadeRenderer.box(b, p, fr, -18.34, 2.94, -10.55, -18.24, 5.26, -9.96, red, glow);
      FacadeRenderer.box(b, p, fr, -13.76, 2.94, -10.55, -13.66, 5.26, -9.96, red, glow);

      // rooftop units and vents
      int[][] units = {{-9, 6}, {-2, 20}, {6, 32}};
      for (int[] u : units) {
         FacadeRenderer.box(b, p, fr, u[0], 9.03, u[1], u[0] + 2.5, 10.25, u[1] + 1.9, 0xFF9A9AA4, light);
         FacadeRenderer.box(b, p, fr, u[0] + 0.45, 10.25, u[1] + 0.30, u[0] + 2.05, 10.35, u[1] + 1.60, 0xFF303034, light);
         FacadeRenderer.box(b, p, fr, u[0] + 0.15, 9.55, u[1] + 0.15, u[0] + 2.35, 9.90, u[1] + 0.22, 0xFF6A6A72, light);
      }
      for (int[] v : new int[][]{{-10, 34}, {10, 14}, {10, 30}}) {
         FacadeRenderer.box(b, p, fr, v[0] - 0.25, 9.04, v[1], v[0] + 0.25, 9.35, v[1] + 0.6, 0xFF6A6A72, light);
      }

      // downspouts and wall utility boxes
      for (double x : new double[]{-sx - 0.55, sx + 0.35}) {
         FacadeRenderer.box(b, p, fr, x, 1.0, -1.62, x + 0.18, 8.90, -1.42, 0xFF7A7066, light);
         FacadeRenderer.box(b, p, fr, x - 0.02, 5.0, -1.64, x + 0.20, 5.06, -1.40, 0xFF4A443E, light);
      }
      FacadeRenderer.box(b, p, fr, sx + 0.52, 2.4, 16.0, sx + 0.60, 3.6, 16.7, 0xFF9A9AA0, light);       // electric meter
      FacadeRenderer.box(b, p, fr, sx + 0.52, 1.0, 15.2, sx + 0.60, 4.0, 17.2, 0xFF2A2622, light);       // service door
      FacadeRenderer.box(b, p, fr, sx + 0.52, 4.0, 15.1, sx + 0.62, 4.2, 17.3, 0xFF5A5A5A, light);
      FacadeRenderer.box(b, p, fr, -sx - 0.60, 2.2, 23.0, -sx - 0.50, 3.4, 23.9, 0xFF9A9AA0, light);      // side meter
      FacadeRenderer.box(b, p, fr, -sx - 0.60, 1.0, 30.0, -sx - 0.52, 4.0, 31.8, 0xFF2A2622, light);      // side service door
      FacadeRenderer.box(b, p, fr, -sx - 0.60, 4.0, 29.9, -sx - 0.50, 4.2, 31.9, 0xFF5A5A5A, light);

      // two parking-lot light poles
      for (int x : new int[]{-12, 12}) {
         FacadeRenderer.box(b, p, fr, x - 0.12, 1.0, -12.1, x + 0.12, 7.0, -11.86, 0xFF4A4A4A, light);
         FacadeRenderer.box(b, p, fr, x - 0.55, 6.90, -12.25, x + 0.55, 7.12, -11.70, 0xFF2A2A2A, light);
         FacadeRenderer.box(b, p, fr, x - 0.35, 6.82, -12.18, x + 0.35, 6.92, -11.76, 0xFFFFE0A0, glow);
      }
   }
}
