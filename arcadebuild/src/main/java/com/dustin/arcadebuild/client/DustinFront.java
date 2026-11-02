package com.dustin.arcadebuild.client;

import com.dustin.arcadebuild.BuildingPlan;
import com.dustin.arcadebuild.BuildingPlan.DustinPlan;
import com.dustin.arcadebuild.client.FacadeRenderer.Frame;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Dustin's Arcade: all the modelled architecture that is not a straight box —
 * the round corner tower with its dome and blade sign, the curved marquee canopy, the mansard roof,
 * the recessed angled entry, and inside the angled walls, vestibule, curved counters, prize riser,
 * alcove fins, pinball alley, recessed ceiling and staff doors. No game textures: all drawn geometry.
 */
final class DustinFront {
   private DustinFront() {}

   private static boolean ON;

   private static int g(int col) {
      return ON ? col : FacadeRenderer.mul(col, 0.22);
   }

   // ---------------- geometry helpers ----------------

   private static void q(VertexConsumer b, Pose p, Frame fr, double ax, double ay, double az, double bx, double by, double bz,
                         double cx, double cy, double cz, double dx, double dy, double dz, int col, int lt) {
      FacadeRenderer.quad4(b, p, fr, new double[]{ax, ay, az}, new double[]{bx, by, bz}, new double[]{cx, cy, cz}, new double[]{dx, dy, dz}, col, lt);
   }

   /** A wall slab of thickness th running from (x0,z0) to (x1,z1) at any angle. */
   static void slab(VertexConsumer b, Pose p, Frame fr, double x0, double z0, double x1, double z1, double y0, double y1, double th, int col, int lt) {
      double dx = x1 - x0, dz = z1 - z0, l = Math.hypot(dx, dz);
      double nx = -dz / l * th / 2, nz = dx / l * th / 2;
      double ax = x0 + nx, az = z0 + nz, bx = x1 + nx, bz = z1 + nz, cx = x1 - nx, cz = z1 - nz, ex = x0 - nx, ez = z0 - nz;
      int s1 = FacadeRenderer.mul(col, 0.92), s2 = FacadeRenderer.mul(col, 0.8);
      q(b, p, fr, ax, y0, az, bx, y0, bz, bx, y1, bz, ax, y1, az, s1, lt);
      q(b, p, fr, ex, y0, ez, cx, y0, cz, cx, y1, cz, ex, y1, ez, s2, lt);
      q(b, p, fr, ax, y1, az, bx, y1, bz, cx, y1, cz, ex, y1, ez, col, lt);
      q(b, p, fr, ax, y0, az, ex, y0, ez, ex, y1, ez, ax, y1, az, s2, lt);
      q(b, p, fr, bx, y0, bz, cx, y0, cz, cx, y1, cz, bx, y1, bz, s2, lt);
   }

   /** A curved band: ring segment between radii r0..r1 and angles a0..a1 (degrees). */
   static void arc(VertexConsumer b, Pose p, Frame fr, double cx, double cz, double r0, double r1, double a0, double a1, double y0, double y1, int col, int lt) {
      int seg = Math.max(4, (int)(Math.abs(a1 - a0) / 7));
      for (int i = 0; i < seg; i++) {
         double t0 = Math.toRadians(a0 + (a1 - a0) * i / seg), t1 = Math.toRadians(a0 + (a1 - a0) * (i + 1) / seg);
         double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
         int sh = FacadeRenderer.mul(col, 0.8 + 0.2 * Math.cos((t0 + t1) / 2 - 2.4));
         q(b, p, fr, cx + c0 * r1, y0, cz + s0 * r1, cx + c1 * r1, y0, cz + s1 * r1, cx + c1 * r1, y1, cz + s1 * r1, cx + c0 * r1, y1, cz + s0 * r1, sh, lt);
         if (r0 > 0.001) q(b, p, fr, cx + c0 * r0, y0, cz + s0 * r0, cx + c1 * r0, y0, cz + s1 * r0, cx + c1 * r0, y1, cz + s1 * r0, cx + c0 * r0, y1, cz + s0 * r0, FacadeRenderer.mul(sh, 0.8), lt);
         q(b, p, fr, cx + c0 * r0, y1, cz + s0 * r0, cx + c0 * r1, y1, cz + s0 * r1, cx + c1 * r1, y1, cz + s1 * r1, cx + c1 * r0, y1, cz + s1 * r0, col, lt);
      }
      for (double a : new double[]{a0, a1}) {
         double t = Math.toRadians(a), c = Math.cos(t), s = Math.sin(t);
         q(b, p, fr, cx + c * r0, y0, cz + s * r0, cx + c * r1, y0, cz + s * r1, cx + c * r1, y1, cz + s * r1, cx + c * r0, y1, cz + s * r0, FacadeRenderer.mul(col, 0.75), lt);
      }
   }

   /** Text on a plane facing (nx,nz), centred at (cx,cy,cz). Pixel size px. */
   static void text(VertexConsumer b, Pose p, Frame fr, String str, double cx, double cy, double cz, double nx, double nz, double px, int col, int lt) {
      double ux = -nz, uz = nx, w = (str.length() * 6 - 1) * px;
      double sx = cx - ux * w / 2 + nx * 0.012, sz = cz - uz * w / 2 + nz * 0.012, top = cy + 3.5 * px;
      for (int i = 0; i < str.length(); i++) {
         String[] gl = FacadeRenderer.FONT.get(str.charAt(i));
         if (gl == null) continue;
         for (int r = 0; r < 7; r++) for (int c = 0; c < 5; c++) {
            if (gl[r].charAt(c) != '1') continue;
            double o0 = (i * 6 + c) * px, o1 = o0 + px * 0.92;
            double y1 = top - r * px, y0 = y1 - px * 0.92;
            q(b, p, fr, sx + ux * o0, y0, sz + uz * o0, sx + ux * o1, y0, sz + uz * o1, sx + ux * o1, y1, sz + uz * o1, sx + ux * o0, y1, sz + uz * o0, col, lt);
         }
      }
   }

   /** Raised 3D channel letters on the brick (front plane z = fz, facing -z). */
   private static void channel(VertexConsumer b, Pose p, Frame fr, String str, double cx, double cy, double fz, double px, int face, int side, int lt) {
      double w = (str.length() * 6 - 1) * px, sx = cx - w / 2, top = cy + 3.5 * px;
      for (int i = 0; i < str.length(); i++) {
         String[] gl = FacadeRenderer.FONT.get(str.charAt(i));
         if (gl == null) continue;
         for (int r = 0; r < 7; r++) for (int c = 0; c < 5; c++) {
            if (gl[r].charAt(c) != '1') continue;
            double x0 = sx + (i * 6 + c) * px, y1 = top - r * px;
            FacadeRenderer.box(b, p, fr, x0, y1 - px, fz - 0.16, x0 + px, y1, fz - 0.02, side, lt);
            FacadeRenderer.box(b, p, fr, x0 + 0.01, y1 - px + 0.01, fz - 0.17, x0 + px - 0.01, y1 - 0.01, fz - 0.16, face, lt);
         }
      }
   }

   private static double canopyZ(double x) {
      double t = Math.min(1, Math.abs(x) / 4.2);
      return -1.5 - 2.6 * Math.pow(1 - Math.pow(t, 4), 0.25);
   }

   // ---------------- the building ----------------

   private static float[] DR = new float[3];

   static void draw(VertexConsumer b, Pose p, Frame fr, BuildingPlan plan, long ms, boolean on, int light, int inLight, int glow, float o, float[] doors) {
      ON = on;
      DR = doors;
      exterior(b, p, fr, plan, ms, light, glow);
      interior(b, p, fr, ms, inLight, glow);
   }

   private static void exterior(VertexConsumer b, Pose p, Frame fr, BuildingPlan plan, long ms, int lt, int glow) {
      double TX = DustinPlan.TOWER_X, TZ = DustinPlan.TOWER_Z, R = DustinPlan.TOWER_R;
      int brick = 0xFF8E3626, cream = 0xFFE8DCC0, dark = 0xFF2A2A2E, frame = plan.frameColor();
      // ---- round corner tower ----
      FacadeRenderer.cylinder(b, p, fr, TX, TZ, R + 0.15, 0.0, 1.3, 28, dark, lt, true);
      FacadeRenderer.cylinder(b, p, fr, TX, TZ, R, 1.3, 12.0, 28, brick, lt, false);
      FacadeRenderer.cylinder(b, p, fr, TX, TZ, R + 0.08, 5.0, 5.25, 28, cream, lt, true);
      FacadeRenderer.cylinder(b, p, fr, TX, TZ, R + 0.08, 8.6, 8.85, 28, cream, lt, true);
      FacadeRenderer.cylinder(b, p, fr, TX, TZ, R + 0.25, 11.75, 12.15, 28, cream, lt, true);
      for (int i = 0; i < 28; i++) {                                     // dentils under the cornice
         double a = Math.PI * 2 * i / 28, x = TX + Math.cos(a) * (R + 0.1), z = TZ + Math.sin(a) * (R + 0.1);
         FacadeRenderer.box(b, p, fr, x - 0.07, 11.55, z - 0.07, x + 0.07, 11.75, z + 0.07, cream, lt);
      }
      // dome: stacked rings of a quarter circle
      int verd = 0xFF4E9A86;
      for (int i = 0; i < 9; i++) {
         double f0 = Math.PI / 2 * i / 9, f1 = Math.PI / 2 * (i + 1) / 9;
         double r0 = (R + 0.1) * Math.cos(f0), r1 = (R + 0.1) * Math.cos(f1), y0 = 12.15 + 1.9 * Math.sin(f0), y1 = 12.15 + 1.9 * Math.sin(f1);
         for (int k = 0; k < 28; k++) {
            double a0 = Math.PI * 2 * k / 28, a1 = Math.PI * 2 * (k + 1) / 28;
            int col = FacadeRenderer.mul(verd, 0.72 + 0.28 * Math.cos((a0 + a1) / 2 - 0.8) * (0.6 + 0.4 * Math.sin(f0)) + (k % 2) * 0.03);
            q(b, p, fr, TX + Math.cos(a0) * r0, y0, TZ + Math.sin(a0) * r0, TX + Math.cos(a1) * r0, y0, TZ + Math.sin(a1) * r0,
               TX + Math.cos(a1) * r1, y1, TZ + Math.sin(a1) * r1, TX + Math.cos(a0) * r1, y1, TZ + Math.sin(a0) * r1, col, lt);
         }
      }
      FacadeRenderer.cylinder(b, p, fr, TX, TZ, 0.07, 14.0, 15.0, 8, 0xFFD8B040, lt, false);
      FacadeRenderer.cylinder(b, p, fr, TX, TZ, 0.18, 15.0, 15.3, 10, 0xFFD8B040, lt, true);
      // neon pilasters running up the tower
      for (double deg : new double[]{215, 250, 290, 325}) {
         double a = Math.toRadians(deg), d = 0.06, rr = R + 0.03;
         q(b, p, fr, TX + Math.cos(a - d) * rr, 1.6, TZ + Math.sin(a - d) * rr, TX + Math.cos(a + d) * rr, 1.6, TZ + Math.sin(a + d) * rr,
            TX + Math.cos(a + d) * rr, 11.3, TZ + Math.sin(a + d) * rr, TX + Math.cos(a - d) * rr, 11.3, TZ + Math.sin(a - d) * rr, g(0xFFFF3AC8), glow);
      }
      // ---- blade sign off the tower, chasing bulbs, ARCADE on both faces ----
      double bz0 = TZ - R - 1.75, bz1 = TZ - R + 0.05;
      FacadeRenderer.box(b, p, fr, TX - 0.15, 4.4, bz0, TX + 0.15, 11.3, bz1, 0xFF701414, lt);
      FacadeRenderer.box(b, p, fr, TX - 0.18, 4.3, bz0 - 0.08, TX + 0.18, 4.4, bz1, 0xFFD8B040, lt);
      FacadeRenderer.box(b, p, fr, TX - 0.18, 11.3, bz0 - 0.08, TX + 0.18, 11.4, bz1, 0xFFD8B040, lt);
      FacadeRenderer.box(b, p, fr, TX - 0.18, 4.3, bz0 - 0.08, TX + 0.18, 11.4, bz0, 0xFFD8B040, lt);
      String word = "ARCADE";
      for (int i = 0; i < word.length(); i++) {
         double cy = 10.55 - i * 1.07, cz = (bz0 + bz1) / 2;
         String ch = String.valueOf(word.charAt(i));
         text(b, p, fr, ch, TX - 0.16, cy, cz, -1, 0, 0.13, g(0xFFFFE070), glow);
         text(b, p, fr, ch, TX + 0.16, cy, cz, 1, 0, 0.13, g(0xFFFFE070), glow);
      }
      int step = (int)(ms / 110);
      int n = 0;
      for (double y = 4.55; y < 11.2; y += 0.34) for (double side : new double[]{-1, 1}) bulb(b, p, fr, TX + side * 0.2, y, bz0 + 0.12, n++, step, glow);
      for (double z = bz0 + 0.12; z < bz1 - 0.1; z += 0.34) for (double side : new double[]{-1, 1}) {
         bulb(b, p, fr, TX + side * 0.2, 4.55, z, n++, step, glow);
         bulb(b, p, fr, TX + side * 0.2, 11.15, z, n++, step, glow);
      }
      // ---- mansard roof edge with shingle courses, cornice and dentils ----
      double mx0 = TX + R - 0.1, mx1 = plan.sx() + 0.5;
      double E = plan.sx() + 0.5, F0 = -1.5, B0 = plan.BACK + 1.5, o = 0.95;      // wall faces; o = mansard overhang
      int slate = 0xFF3A3F4A, course = 0xFF22262E;
      // four hipped slopes meeting at the corners: the building reads as a real roof, not a box
      double[][][] sides = {
         {{-E - o, F0 - o}, {E + o, F0 - o}, {E, F0}, {-E, F0}},
         {{E + o, F0 - o}, {E + o, B0 + o}, {E, B0}, {E, F0}},
         {{E + o, B0 + o}, {-E - o, B0 + o}, {-E, B0}, {E, B0}},
         {{-E - o, B0 + o}, {-E - o, F0 - o}, {-E, F0}, {-E, B0}}};
      for (double[][] sd : sides) {
         q(b, p, fr, sd[0][0], 8.0, sd[0][1], sd[1][0], 8.0, sd[1][1], sd[2][0], 10.05, sd[2][1], sd[3][0], 10.05, sd[3][1], slate, lt);
         for (double t = 0.1; t < 1; t += 0.13) {
            double ax = sd[0][0] + (sd[3][0] - sd[0][0]) * t, az = sd[0][1] + (sd[3][1] - sd[0][1]) * t;
            double bx = sd[1][0] + (sd[2][0] - sd[1][0]) * t, bz = sd[1][1] + (sd[2][1] - sd[1][1]) * t, y = 8.0 + 2.05 * t;
            q(b, p, fr, ax, y, az, bx, y, bz, bx, y + 0.035, bz, ax, y + 0.035, az, course, lt);
         }
         // cornice under each slope, with dentils
         double cx0 = sd[0][0], cz0 = sd[0][1], cx1 = sd[1][0], cz1 = sd[1][1];
         q(b, p, fr, cx0, 7.98, cz0, cx1, 7.98, cz1, sd[2][0], 7.98, sd[2][1], sd[3][0], 7.98, sd[3][1], cream, lt);
         slab(b, p, fr, (cx0 + sd[3][0]) / 2, (cz0 + sd[3][1]) / 2, (cx1 + sd[2][0]) / 2, (cz1 + sd[2][1]) / 2, 7.7, 7.98, o + 0.05, cream, lt);
      }
      for (double x = -E + 0.3; x < E; x += 0.45) FacadeRenderer.box(b, p, fr, x, 7.52, -1.95, x + 0.2, 7.7, -1.5, cream, lt);
      // flat roof deck details: HVAC units, vent stacks
      for (double[] u : new double[][]{{-5, 10}, {4, 18}, {-3, 30}}) {            // round rooftop condensers on rails
         FacadeRenderer.box(b, p, fr, u[0] - 1.0, 10.0, u[1] - 0.9, u[0] + 1.0, 10.12, u[1] - 0.75, 0xFF505458, lt);
         FacadeRenderer.box(b, p, fr, u[0] - 1.0, 10.0, u[1] + 0.75, u[0] + 1.0, 10.12, u[1] + 0.9, 0xFF505458, lt);
         FacadeRenderer.cylinder(b, p, fr, u[0], u[1], 0.85, 10.12, 11.0, 24, 0xFFC8CCD0, lt, false);
         for (double y = 10.25; y < 10.95; y += 0.1) FacadeRenderer.cylinder(b, p, fr, u[0], u[1], 0.86, y, y + 0.03, 24, 0xFF8A8E92, lt, false);
         FacadeRenderer.cylinder(b, p, fr, u[0], u[1], 0.88, 11.0, 11.08, 24, 0xFF303236, lt, true);
         FacadeRenderer.cylinder(b, p, fr, u[0], u[1], 0.12, 11.08, 11.18, 8, 0xFF606468, lt, true);
         for (int k = 0; k < 4; k++) {
            double a = Math.PI / 2 * k + (System.currentTimeMillis() % 100000) / 300.0 * (ON ? 1 : 0);
            q(b, p, fr, u[0], 11.1, u[1], u[0] + Math.cos(a) * 0.7, 11.1, u[1] + Math.sin(a) * 0.7, u[0] + Math.cos(a + 0.4) * 0.7, 11.1, u[1] + Math.sin(a + 0.4) * 0.7, u[0], 11.1, u[1], 0xFF404448, lt);
         }
      }
      FacadeRenderer.cylinder(b, p, fr, 7, 25, 0.12, 10.0, 11.0, 10, 0xFF505458, lt, true);
      FacadeRenderer.cylinder(b, p, fr, -8, 22, 0.1, 10.0, 10.8, 10, 0xFF505458, lt, true);
      // side and back walls: brick pilasters with stone caps, a dark plinth, downspouts
      for (int sgn = -1; sgn <= 1; sgn += 2) {
         double wx = sgn * E;
         FacadeRenderer.box(b, p, fr, Math.min(wx, wx + sgn * 0.14), 0.0, F0 + 0.4, Math.max(wx, wx + sgn * 0.14), 1.55, B0, 0xFF1E1E24, lt);
         for (double z = 2.5; z < B0 - 1; z += 4.0) {
            if (sgn > 0 && z > 5 && z < 23) continue;                       // the painted sign lives here
            FacadeRenderer.box(b, p, fr, Math.min(wx, wx + sgn * 0.2), 1.55, z - 0.35, Math.max(wx, wx + sgn * 0.2), 7.7, z + 0.35, 0xFF7E2E20, lt);
            FacadeRenderer.box(b, p, fr, Math.min(wx, wx + sgn * 0.26), 7.3, z - 0.42, Math.max(wx, wx + sgn * 0.26), 7.5, z + 0.42, cream, lt);
            FacadeRenderer.box(b, p, fr, Math.min(wx, wx + sgn * 0.26), 1.55, z - 0.42, Math.max(wx, wx + sgn * 0.26), 1.75, z + 0.42, cream, lt);
         }
         FacadeRenderer.cylinder(b, p, fr, wx + sgn * 0.15, B0 - 0.6, 0.09, 0.0, 8.0, 8, 0xFF606468, lt, false);
         // stone belt course and wall-washer lamps between the pilasters (no fake windows on the sides)
         FacadeRenderer.box(b, p, fr, Math.min(wx, wx + sgn * 0.12), 5.6, F0 + 0.4, Math.max(wx, wx + sgn * 0.12), 5.8, B0, cream, lt);
         for (double z = 4.5; z < B0 - 2; z += 4.0) {
            if (sgn > 0 && z > 5 && z < 23) continue;
            double xs = wx + sgn * 0.16;
            FacadeRenderer.box(b, p, fr, Math.min(wx, xs), 4.6, z - 0.12, Math.max(wx, xs), 4.9, z + 0.12, 0xFF1E2A24, lt);
            FacadeRenderer.cylinder(b, p, fr, xs + sgn * 0.08, z, 0.14, 4.7, 4.85, 10, ON ? 0xFFFFE8B0 : 0xFF6A6050, ON ? 0xF000F0 : lt, true);
         }
      }
      FacadeRenderer.box(b, p, fr, -E, 0.0, B0, E, 1.55, B0 + 0.14, 0xFF1E1E24, lt);
      for (double x = -E + 2.5; x < E - 1; x += 4.0) {
         FacadeRenderer.box(b, p, fr, x - 0.35, 1.55, B0, x + 0.35, 7.7, B0 + 0.2, 0xFF7E2E20, lt);
         FacadeRenderer.box(b, p, fr, x - 0.42, 7.3, B0, x + 0.42, 7.5, B0 + 0.26, cream, lt);
      }
      // painted wall sign down the right side
      FacadeRenderer.box(b, p, fr, E, 3.2, 6.0, E + 0.02, 6.4, 22.0, 0xFF1C2A5A, lt);
      text(b, p, fr, "GAMES", E + 0.02, 5.3, 14.0, 1, 0, 0.22, 0xFFF2E6C8, lt);
      text(b, p, fr, "PRIZES", E + 0.02, 3.95, 14.0, 1, 0, 0.14, 0xFFFFD040, lt);
      // back: steel service door with a caged light, meter box with conduit, dumpster
      FacadeRenderer.box(b, p, fr, -3.6, 1.0, B0, -2.4, 3.2, B0 + 0.06, 0xFF6E7278, lt);
      FacadeRenderer.box(b, p, fr, -3.7, 3.2, B0, -2.3, 3.3, B0 + 0.1, 0xFF303236, lt);
      FacadeRenderer.box(b, p, fr, -2.6, 2.0, B0 + 0.06, -2.5, 2.1, B0 + 0.12, 0xFFC8CCD2, lt);
      FacadeRenderer.cylinder(b, p, fr, -3.0, B0 + 0.18, 0.12, 3.5, 3.75, 8, g(0xFFFFE8B0), ON ? 0xF000F0 : lt, true);
      FacadeRenderer.box(b, p, fr, 2.5, 2.2, B0, 3.3, 3.4, B0 + 0.25, 0xFF8A8E92, lt);
      FacadeRenderer.cylinder(b, p, fr, 2.9, B0 + 0.1, 0.05, 3.4, 7.7, 6, 0xFF8A8E92, lt, false);
      site(b, p, fr, plan, E, B0, lt, glow);
      // ---- kickplate tile and window sills / lintels ----
      FacadeRenderer.box(b, p, fr, mx0, 1.0, -1.62, -3.5, 1.55, -1.5, 0xFF1E1E24, lt);
      FacadeRenderer.box(b, p, fr, 3.5, 1.0, -1.62, mx1, 1.55, -1.5, 0xFF1E1E24, lt);
      for (int[] w : plan.windows()) {
         FacadeRenderer.box(b, p, fr, w[0] - 0.8, 1.86, -1.8, w[1] + 0.8, 2.02, -1.45, cream, lt);
         FacadeRenderer.box(b, p, fr, w[0] - 0.8, 4.98, -1.68, w[1] + 0.8, 5.22, -1.5, cream, lt);
         FacadeRenderer.box(b, p, fr, w[0] - 0.6, 1.9, 0.5, w[1] + 0.6, 2.02, 0.78, 0xFF3A2416, lt);
      }
      text(b, p, fr, "OPEN", -6.5, 3.3, 0.36, 0, -1, 0.1, g(0xFFFF3030), glow);
      text(b, p, fr, "PLAY", 6.0, 3.3, 0.36, 0, -1, 0.09, g(0xFF40E8FF), glow);
      // ---- recessed entry: angled glass returns on a kick wall, soffit with downlights, terrazzo ----
      for (int s = -1; s <= 1; s += 2) {
         slab(b, p, fr, s * 3.5, -1.5, s * 1.75, 0.32, 1.0, 1.5, 0.2, 0xFF1E1E24, lt);
         slab(b, p, fr, s * 3.5, -1.5, s * 1.75, 0.32, 3.95, 5.0, 0.24, cream, lt);
         double ox = s * 3.5, oz = -1.5, ix = s * 1.75, iz = 0.32;
         // aluminium storefront frame: corner posts and rails around each angled glass return
         FacadeRenderer.box(b, p, fr, ox - 0.1, 1.0, oz - 0.12, ox + 0.1, 4.02, oz + 0.08, 0xFF34363A, lt);
         FacadeRenderer.box(b, p, fr, ix - 0.1, 1.0, iz - 0.1, ix + 0.1, 4.02, iz + 0.14, 0xFF34363A, lt);
         slab(b, p, fr, ox, oz, ix, iz, 1.48, 1.6, 0.18, 0xFF34363A, lt);
         slab(b, p, fr, ox, oz, ix, iz, 3.86, 4.0, 0.18, 0xFF34363A, lt);
      }
      // one continuous dark surround around the whole recessed entry, on the brick face
      FacadeRenderer.box(b, p, fr, -3.6, 1.0, -1.66, -3.42, 4.84, -1.38, frame, lt);
      FacadeRenderer.box(b, p, fr, 3.42, 1.0, -1.66, 3.6, 4.84, -1.38, frame, lt);
      FacadeRenderer.box(b, p, fr, -3.6, 4.84, -1.66, 3.6, 5.0, -1.38, frame, lt);
      FacadeRenderer.box(b, p, fr, -3.6, 1.0, -1.66, 3.6, 1.12, -1.38, frame, lt);
      FacadeRenderer.box(b, p, fr, -3.6, 4.94, -1.5, 3.6, 5.0, 0.5, 0xFFF2E6C8, lt);
      FacadeRenderer.cylinder(b, p, fr, -1.0, -0.6, 0.14, 4.9, 4.94, 10, g(0xFFFFF4D0), glow, true);
      FacadeRenderer.cylinder(b, p, fr, 1.0, -0.6, 0.14, 4.9, 4.94, 10, g(0xFFFFF4D0), glow, true);
      FacadeRenderer.box(b, p, fr, -3.5, 1.0, -1.5, 3.5, 1.02, 0.5, 0xFF46464C, lt);
      FacadeRenderer.cylinder(b, p, fr, 0, -0.5, 0.7, 1.02, 1.03, 16, 0xFFD8B040, lt, true);
      // ---- curved marquee canopy on slim posts ----
      int red = 0xFFC01818, gold = 0xFFE0B840, soff = 0xFFF2E6C8;
      int N = 36;
      for (int i = 0; i < N; i++) {
         double x0 = -4.2 + 8.4 * i / N, x1 = -4.2 + 8.4 * (i + 1) / N, z0 = canopyZ(x0), z1 = canopyZ(x1);
         q(b, p, fr, x0, 4.45, z0, x1, 4.45, z1, x1, 4.56, z1, x0, 4.56, z0, gold, lt);
         q(b, p, fr, x0, 4.56, z0, x1, 4.56, z1, x1, 5.04, z1, x0, 5.04, z0, red, lt);
         q(b, p, fr, x0, 5.04, z0, x1, 5.04, z1, x1, 5.15, z1, x0, 5.15, z0, gold, lt);
         q(b, p, fr, x0, 5.15, z0, x1, 5.15, z1, x1, 5.4, -1.5, x0, 5.4, -1.5, 0xFF8A1010, lt);
         q(b, p, fr, x0, 4.45, z0, x1, 4.45, z1, x1, 4.45, -1.5, x0, 4.45, -1.5, soff, lt);
         double xm = (x0 + x1) / 2;
         bulb(b, p, fr, xm, 4.4, canopyZ(xm) + 0.1, i, step, glow);
      }
      for (double x = -3.6; x <= 3.61; x += 0.6) for (double z = -1.9; z > canopyZ(x) + 0.35; z -= 0.6) {
         bulb(b, p, fr, x, 4.4, z, (int)((x + 4) * 3 + z * 2), step, glow);
      }
      // DUSTIN'S on the fascia, following the curve
      String dn = "DUSTIN'S";
      double px = 0.055, w = (dn.length() * 6 - 1) * px;
      for (int i = 0; i < dn.length(); i++) {
         String[] gl = FacadeRenderer.FONT.get(dn.charAt(i));
         if (gl == null) continue;
         for (int r = 0; r < 7; r++) for (int c = 0; c < 5; c++) {
            if (gl[r].charAt(c) != '1') continue;
            double x0 = -w / 2 + (i * 6 + c) * px, x1 = x0 + px * 0.9, y1 = 4.99 - r * px, y0 = y1 - px * 0.9;
            q(b, p, fr, x0, y0, canopyZ(x0) - 0.012, x1, y0, canopyZ(x1) - 0.012, x1, y1, canopyZ(x1) - 0.012, x0, y1, canopyZ(x0) - 0.012, g(0xFFFFE070), glow);
         }
      }
      for (int s = -1; s <= 1; s += 2) {
         FacadeRenderer.cylinder(b, p, fr, s * 3.55, canopyZ(3.55) + 0.25, 0.075, 1.0, 4.45, 10, 0xFFC8A040, lt, false);
         FacadeRenderer.cylinder(b, p, fr, s * 3.55, canopyZ(3.55) + 0.25, 0.15, 1.0, 1.12, 10, 0xFF8A6A20, lt, true);
      }
      // raised channel letters on the brick above the canopy
      FacadeRenderer.box(b, p, fr, -5.2, 5.75, -1.62, 5.2, 5.85, -1.5, 0xFF303034, lt);
      channel(b, p, fr, "DUSTIN'S", 0, 6.65, -1.5, 0.2, g(0xFFFF3A3A), 0xFF6A1010, ON ? glow : lt);
   }

   /** A brick blind arch on a side wall: dark recessed panel, stone arch ring, keystone, sill. */
   private static void blindArch(VertexConsumer b, Pose p, Frame fr, double wx, double z, int sgn, int lt) {
      int panel = 0xFF4A1C14, stone = 0xFFE8DCC0;
      double hw = 1.25, y0 = 2.2, ys = 5.4, x = wx + sgn * 0.012, xo = wx + sgn * 0.09;
      q(b, p, fr, x, y0, z - hw, x, y0, z + hw, x, ys, z + hw, x, ys, z - hw, panel, lt);
      int n = 14;
      for (int i = 0; i < n; i++) {
         double a0 = Math.PI * i / n, a1 = Math.PI * (i + 1) / n;
         double z0 = z - Math.cos(a0) * hw, z1 = z - Math.cos(a1) * hw, y0a = ys + Math.sin(a0) * hw, y1a = ys + Math.sin(a1) * hw;
         q(b, p, fr, x, ys, z, x, ys, z, x, y1a, z1, x, y0a, z0, panel, lt);                 // fan fill
         double r2 = hw + 0.22;
         double oz0 = z - Math.cos(a0) * r2, oz1 = z - Math.cos(a1) * r2, oy0 = ys + Math.sin(a0) * r2, oy1 = ys + Math.sin(a1) * r2;
         q(b, p, fr, xo, y0a, z0, xo, y1a, z1, xo, oy1, oz1, xo, oy0, oz0, FacadeRenderer.mul(stone, i % 2 == 0 ? 1.0 : 0.93), lt);
      }
      FacadeRenderer.box(b, p, fr, Math.min(wx, xo + sgn * 0.04), ys + hw - 0.05, z - 0.16, Math.max(wx, xo + sgn * 0.04), ys + hw + 0.32, z + 0.16, stone, lt);
      FacadeRenderer.box(b, p, fr, Math.min(wx, wx + sgn * 0.16), y0 - 0.14, z - hw - 0.15, Math.max(wx, wx + sgn * 0.16), y0, z + hw + 0.15, stone, lt);
      for (double zz : new double[]{z - hw - 0.11, z + hw + 0.11}) FacadeRenderer.box(b, p, fr, Math.min(wx, xo), y0, zz - 0.11, Math.max(wx, xo), ys, zz + 0.11, FacadeRenderer.mul(stone, 0.95), lt);
   }

   /** Street furniture: curb, sidewalk joints, round planters with trees, lamp posts, benches. */
   private static void site(VertexConsumer b, Pose p, Frame fr, BuildingPlan plan, double E, double B0, int lt, int glow) {
      double W = E + 1.0, F = -5.5;
      int curb = 0xFFC8C4BA, joint = 0xFF8E8A84;
      FacadeRenderer.box(b, p, fr, -W, 0.9, F - 0.25, W, 1.08, F, curb, lt);
      for (double x = -W + 2; x < W; x += 2) FacadeRenderer.box(b, p, fr, x - 0.02, 1.005, F, x + 0.02, 1.012, -1.5, joint, lt);
      FacadeRenderer.box(b, p, fr, -W, 1.005, -3.52, W, 1.012, -3.48, joint, lt);
      for (double z = 0.5; z < B0 + 1; z += 2) {
         FacadeRenderer.box(b, p, fr, -W, 1.005, z - 0.02, -E, 1.012, z + 0.02, joint, lt);
         FacadeRenderer.box(b, p, fr, E, 1.005, z - 0.02, W, 1.012, z + 0.02, joint, lt);
      }
   }

   /** Leafy tree crown: a few overlapping rounded clumps. */
   private static void canopy(VertexConsumer b, Pose p, Frame fr, double x, double y, double z, double r, int lt) {
      double[][] clumps = {{0, 0, 0, 1}, {0.55, -0.25, 0.3, 0.7}, {-0.5, -0.2, -0.35, 0.72}, {0.15, 0.45, -0.4, 0.65}, {-0.3, 0.35, 0.45, 0.6}};
      int[] greens = {0xFF3E7A34, 0xFF4C8C3A, 0xFF35682C, 0xFF5A9A44, 0xFF447E36};
      for (int i = 0; i < clumps.length; i++) globe(b, p, fr, x + clumps[i][0], y + clumps[i][1], z + clumps[i][2], r * clumps[i][3], greens[i], lt);
   }

   /** A sphere made of latitude bands. */
   private static void globe(VertexConsumer b, Pose p, Frame fr, double x, double y, double z, double r, int col, int lt) {
      int la = 8, lo = 14;
      for (int i = 0; i < la; i++) {
         double f0 = -Math.PI / 2 + Math.PI * i / la, f1 = -Math.PI / 2 + Math.PI * (i + 1) / la;
         double r0 = Math.cos(f0) * r, r1 = Math.cos(f1) * r, y0 = y + Math.sin(f0) * r, y1 = y + Math.sin(f1) * r;
         int c = FacadeRenderer.mul(col, 0.7 + 0.3 * (i + 1) / la);
         for (int k = 0; k < lo; k++) {
            double a0 = Math.PI * 2 * k / lo, a1 = Math.PI * 2 * (k + 1) / lo;
            q(b, p, fr, x + Math.cos(a0) * r0, y0, z + Math.sin(a0) * r0, x + Math.cos(a1) * r0, y0, z + Math.sin(a1) * r0,
               x + Math.cos(a1) * r1, y1, z + Math.sin(a1) * r1, x + Math.cos(a0) * r1, y1, z + Math.sin(a0) * r1, c, lt);
         }
      }
   }

   private static void bulb(VertexConsumer b, Pose p, Frame fr, double x, double y, double z, int i, int step, int glow) {
      boolean lit = ON && Math.floorMod(i - step, 3) != 0;
      int col = lit ? 0xFFFFF2B0 : 0xFF6A5A30;
      FacadeRenderer.box(b, p, fr, x - 0.045, y - 0.045, z - 0.045, x + 0.045, y + 0.045, z + 0.045, col, glow);
   }

   private static void interior(VertexConsumer b, Pose p, Frame fr, long ms, int lt, int glow) {
      double[][] W = DustinPlan.WALLS;
      for (int k = 0; k < W.length; k++) {
         double[] w = W[k];
         double x0 = w[0], z0 = w[1], x1 = w[2], z1 = w[3], top = 1 + w[4];
         int type = (int)w[5];
         double dx = x1 - x0, dz = z1 - z0, l = Math.hypot(dx, dz), nx = -dz / l, nz = dx / l;
         double mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
         if (nx * (0 - mx) + nz * (11 - mz) < 0) { nx = -nx; nz = -nz; }      // normal faces the arcade floor
         if (type == 0 || type == 3) {
            top = BuildingPlan.CEIL;
            double[][] spans = type == 3 ? new double[][]{{0, 0.5 - 0.7 / l}, {0.5 + 0.7 / l, 1}} : new double[][]{{0, 1}};
            for (double[] sp : spans) {
               double ax = x0 + dx * sp[0], az = z0 + dz * sp[0], bx = x0 + dx * sp[1], bz = z0 + dz * sp[1];
               slab(b, p, fr, ax, az, bx, bz, 1.0, top, 0.34, 0xFF3B2B52, lt);
               slab(b, p, fr, ax, az, bx, bz, 1.0, 2.5, 0.38, 0xFF4A2C18, lt);
               slab(b, p, fr, ax, az, bx, bz, 2.5, 2.62, 0.42, 0xFF2A1A10, lt);
               slab(b, p, fr, ax, az, bx, bz, 1.0, 1.25, 0.4, 0xFF1A1008, lt);
            }
            slab(b, p, fr, x0, z0, x1, z1, 7.0, 7.15, 0.42, 0xFF2A1A10, lt);
            slab(b, p, fr, x0 + nx * 0.2, z0 + nz * 0.2, x1 + nx * 0.2, z1 + nz * 0.2, 6.35, 6.43, 0.02, g(0xFFFF3AC8), glow);
            if (k == 3) text(b, p, fr, "TICKETS", mx + nx * 0.19, 4.6, mz + nz * 0.19, nx, nz, 0.1, g(0xFFFFE070), glow);
            if (type == 3) staffDoor(b, p, fr, mx + nx * 0.19, mz + nz * 0.19, dx / l, dz / l, nx, nz, lt, glow);
         } else if (type == 1) {
            slab(b, p, fr, x0, z0, x1, z1, 1.0, top - 0.12, 0.3, 0xFF3A2416, lt);
            slab(b, p, fr, x0, z0, x1, z1, 1.0, 1.2, 0.34, 0xFF140C06, lt);
            slab(b, p, fr, x0, z0, x1, z1, top - 0.12, top, 0.44, 0xFF141418, lt);
            for (int s = -1; s <= 1; s += 2) {
               slab(b, p, fr, x0 + nx * 0.16 * s, z0 + nz * 0.16 * s, x1 + nx * 0.16 * s, z1 + nz * 0.16 * s, top - 0.2, top - 0.13, 0.02, g(0xFFB040FF), glow);
            }
            FacadeRenderer.cylinder(b, p, fr, x0, z0, 0.06, top, top + 1.1, 8, 0xFFC8A040, lt, true);
            FacadeRenderer.cylinder(b, p, fr, x1, z1, 0.06, top, top + 1.1, 8, 0xFFC8A040, lt, true);
         } else {
            slab(b, p, fr, x0, z0, x1, z1, 1.0, 1.35, 0.12, 0xFF34363A, lt);
            slab(b, p, fr, x0, z0, x1, z1, 3.95, 4.1, 0.12, 0xFF34363A, lt);
            FacadeRenderer.cylinder(b, p, fr, x0, z0, 0.06, 1.0, 4.1, 8, 0xFF34363A, lt, true);
            FacadeRenderer.cylinder(b, p, fr, x1, z1, 0.06, 1.0, 4.1, 8, 0xFF34363A, lt, true);
         }
      }
      // vestibule header with WELCOME
      FacadeRenderer.box(b, p, fr, -2.65, 3.95, 3.3, 2.65, 4.6, 3.5, 0xFF1C1424, lt);
      text(b, p, fr, "WELCOME", 0, 4.28, 3.3, 0, -1, 0.065, g(0xFF40E8FF), glow);
      // (the prize booth block carries its own counter and the prize wall with the real prizes on it)
      // hanging PRIZES sign
      FacadeRenderer.box(b, p, fr, 8.62, 5.5, 4.3, 8.76, 6.45, 8.1, 0xFF2A1438, lt);
      text(b, p, fr, "PRIZES", 8.61, 5.98, 6.2, -1, 0, 0.1, g(0xFFFFE070), glow);
      FacadeRenderer.cylinder(b, p, fr, 8.69, 4.6, 0.02, 6.45, 8.0, 4, 0xFF808088, lt, false);
      FacadeRenderer.cylinder(b, p, fr, 8.69, 7.8, 0.02, 6.45, 8.0, 4, 0xFF808088, lt, false);
      // ---- alcove fins between machine groups ----
      int f = 0;
      for (double[] fn : DustinPlan.FINS) {
         double x = fn[0], z = fn[1], d = fn[2], xe = x + d * 1.35;
         FacadeRenderer.box(b, p, fr, Math.min(x, xe), 1.0, z - 0.07, Math.max(x, xe), 7.2, z + 0.07, 0xFF20182A, lt);
         FacadeRenderer.box(b, p, fr, xe - 0.03, 1.15, z - 0.03, xe + 0.03, 7.05, z + 0.03, g(f++ % 2 == 0 ? 0xFFFF3AC8 : 0xFF40E8FF), glow);
      }
      // ---- dim pinball alley: low ceiling, blacklight tubes, PINBALL fascia ----
      FacadeRenderer.box(b, p, fr, -10.5, 6.4, 12.3, -6.8, 6.5, 19.3, 0xFF1A1424, lt);
      FacadeRenderer.box(b, p, fr, -6.86, 5.95, 12.3, -6.76, 6.5, 19.3, 0xFF0E0A12, lt);
      FacadeRenderer.box(b, p, fr, -10.5, 5.95, 12.24, -6.76, 6.5, 12.34, 0xFF0E0A12, lt);
      FacadeRenderer.box(b, p, fr, -10.5, 5.95, 19.26, -6.76, 6.5, 19.36, 0xFF0E0A12, lt);
      text(b, p, fr, "PINBALL", -6.75, 6.22, 15.8, 1, 0, 0.055, g(0xFFB040FF), glow);
      for (double x : new double[]{-9.6, -8.0}) FacadeRenderer.box(b, p, fr, x - 0.03, 6.33, 12.6, x + 0.03, 6.4, 19.0, g(0xFF8040FF), glow);
      // ---- recessed ceiling over the centre island ----
      int band = 0xFF2E2420;
      FacadeRenderer.box(b, p, fr, -4.5, 7.3, 7.5, 4.5, 8.0, 7.9, band, lt);
      FacadeRenderer.box(b, p, fr, -4.5, 7.3, 14.1, 4.5, 8.0, 14.5, band, lt);
      FacadeRenderer.box(b, p, fr, -4.5, 7.3, 7.9, -4.1, 8.0, 14.1, band, lt);
      FacadeRenderer.box(b, p, fr, 4.1, 7.3, 7.9, 4.5, 8.0, 14.1, band, lt);
      int cy = g(0xFF40E8FF);
      FacadeRenderer.box(b, p, fr, -4.1, 7.3, 7.88, 4.1, 7.36, 7.92, cy, glow);
      FacadeRenderer.box(b, p, fr, -4.1, 7.3, 14.08, 4.1, 7.36, 14.12, cy, glow);
      FacadeRenderer.box(b, p, fr, -4.12, 7.3, 7.9, -4.08, 7.36, 14.1, cy, glow);
      FacadeRenderer.box(b, p, fr, 4.08, 7.3, 7.9, 4.12, 7.36, 14.1, cy, glow);
      // ---- frames and swung-open steel doors on the office / power room doorways ----
      int HB = DustinPlan.LOUNGE_BACK;
      // archway from the arcade floor into the console lounge
      int arch = 0xFFE8DCC0;
      for (double z : new double[]{21.45, 22.55}) {
         double zf = z < 22 ? z - 0.06 : z;
         FacadeRenderer.box(b, p, fr, -2.75, 1.0, zf, -2.5, 4.0, zf + 0.06, arch, lt);
         FacadeRenderer.box(b, p, fr, 2.5, 1.0, zf, 2.75, 4.0, zf + 0.06, arch, lt);
         int n = 16;
         for (int i = 0; i < n; i++) {                                   // elliptical arch head inside the opening
            double a0 = Math.PI * i / n, a1 = Math.PI * (i + 1) / n;
            double x0 = -Math.cos(a0) * 2.5, x1 = -Math.cos(a1) * 2.5, y0 = 3.2 + Math.sin(a0) * 0.8, y1 = 3.2 + Math.sin(a1) * 0.8;
            q(b, p, fr, x0, y0, zf + 0.03, x1, y1, zf + 0.03, x1, 4.0, zf + 0.03, x0, 4.0, zf + 0.03, 0xFF5A3A22, lt);
            q(b, p, fr, x0, y0, zf + 0.025, x1, y1, zf + 0.025, x1 * 1.08, y1 + 0.12, zf + 0.025, x0 * 1.08, y0 + 0.12, zf + 0.025, arch, lt);
         }
      }
      // solid reveals around the one-block-thick arch wall so the opening has real depth
      for (double x : new double[]{-2.5, 2.5}) {
         FacadeRenderer.box(b, p, fr, x - 0.06, 1.0, 21.45, x + 0.06, 4.02, 22.55, 0xFF5A3A22, lt);
      }
      FacadeRenderer.box(b, p, fr, -2.5, 3.94, 21.45, 2.5, 4.06, 22.55, 0xFF5A3A22, lt);
      slab(b, p, fr, -2.5, 22, 2.5, 22, 3.98, 4.0, 1.0, 0xFF5A3A22, lt);
      FacadeRenderer.box(b, p, fr, -3.3, 4.35, 21.38, 3.3, 5.15, 21.46, 0xFF1C1424, lt);
      text(b, p, fr, "CONSOLE LOUNGE", 0, 4.75, 21.38, 0, -1, 0.07, g(0xFFFF3AC8), glow);
      for (int s = -1; s <= 1; s += 2) {
         double c = s * 6;
         FacadeRenderer.box(b, p, fr, c - 0.62, 1.0, HB - 0.6, c - 0.5, 3.1, HB - 0.44, 0xFF606468, lt);
         FacadeRenderer.box(b, p, fr, c + 0.5, 1.0, HB - 0.6, c + 0.62, 3.1, HB - 0.44, 0xFF606468, lt);
         FacadeRenderer.box(b, p, fr, c - 0.62, 3.0, HB - 0.6, c + 0.62, 3.12, HB - 0.44, 0xFF606468, lt);
         double hx = c + s * 0.48, hz = HB, t = Math.toRadians(DR[s < 0 ? 1 : 2] * 90);
         double ex = hx - s * Math.cos(t) * 0.94, ez = hz + Math.sin(t) * 0.94;
         slab(b, p, fr, hx, hz, ex, ez, 1.02, 2.98, 0.06, 0xFF6E7278, lt);
         slab(b, p, fr, hx + (ex - hx) * 0.75, hz + (ez - hz) * 0.75, hx + (ex - hx) * 0.8, hz + (ez - hz) * 0.8, 1.95, 2.05, 0.16, 0xFFC8CCD2, lt);
         FacadeRenderer.box(b, p, fr, c - 0.35, 3.25, HB - 0.56, c + 0.35, 3.55, HB - 0.5, g(0xFF20C060), glow);
         text(b, p, fr, "EXIT", c, 3.4, HB - 0.56, 0, -1, 0.035, 0xFFFFFFFF, glow);
         // solid jamb and head reveals through the one-block-thick office / power-room wall
         int reveal = 0xFFD8C8A0;
         FacadeRenderer.box(b, p, fr, c - 0.5, 1.0, HB - 0.5, c - 0.42, 3.12, HB + 0.5, reveal, lt);
         FacadeRenderer.box(b, p, fr, c + 0.42, 1.0, HB - 0.5, c + 0.5, 3.12, HB + 0.5, reveal, lt);
         FacadeRenderer.box(b, p, fr, c - 0.5, 3.0, HB - 0.5, c + 0.5, 3.12, HB + 0.5, reveal, lt);
         FacadeRenderer.box(b, p, fr, c - 0.5, 1.0, HB - 0.5, c + 0.5, 1.03, HB + 0.5, 0xFF505458, lt);
      }
   }

   private static void staffDoor(VertexConsumer b, Pose p, Frame fr, double cx, double cz, double ux, double uz, double nx, double nz, int lt, int glow) {
      double hw = 0.7;
      cx -= nx * 0.19;
      cz -= nz * 0.19;
      double ax = cx - ux * hw, az = cz - uz * hw, bx = cx + ux * hw, bz = cz + uz * hw;
      // opening: dark reveal, steel frame and a header; the wall above the door stays
      slab(b, p, fr, ax, az, bx, bz, 1.0, 1.02, 0.36, 0xFF505458, lt);
      for (double[] e : new double[][]{{ax, az}, {bx, bz}}) slab(b, p, fr, e[0] - ux * 0.06, e[1] - uz * 0.06, e[0] + ux * 0.06, e[1] + uz * 0.06, 1.0, 3.05, 0.42, 0xFF303236, lt);
      slab(b, p, fr, ax, az, bx, bz, 2.95, 3.05, 0.42, 0xFF303236, lt);
      slab(b, p, fr, ax, az, bx, bz, 3.05, 8.0, 0.34, 0xFF3B2B52, lt);
      // the leaf swings into the closet as you walk up
      double t = Math.toRadians(DR[0] * 95), lx = ux * Math.cos(t) - nx * Math.sin(t), lz = uz * Math.cos(t) - nz * Math.sin(t);
      double hx = ax + ux * 0.06, hz = az + uz * 0.06, ex = hx + lx * (2 * hw - 0.12), ez = hz + lz * (2 * hw - 0.12);
      double fnx = -lz, fnz = lx;
      if (fnx * nx + fnz * nz < 0) { fnx = -fnx; fnz = -fnz; }
      slab(b, p, fr, hx, hz, ex, ez, 1.02, 2.94, 0.06, 0xFF6E7278, lt);
      slab(b, p, fr, hx + fnx * 0.04, hz + fnz * 0.04, ex + fnx * 0.04, ez + fnz * 0.04, 1.02, 1.3, 0.02, 0xFFA0A4AA, lt);
      double mx = (hx + ex) / 2, mz = (hz + ez) / 2;
      slab(b, p, fr, hx + lx * 0.2 + fnx * 0.07, hz + lz * 0.2 + fnz * 0.07, ex - lx * 0.1 + fnx * 0.07, ez - lz * 0.1 + fnz * 0.07, 2.0, 2.08, 0.05, 0xFFC8CCD2, lt);
      slab(b, p, fr, mx - lx * 0.3 + fnx * 0.04, mz - lz * 0.3 + fnz * 0.04, mx + lx * 0.3 + fnx * 0.04, mz + lz * 0.3 + fnz * 0.04, 2.45, 2.7, 0.02, 0xFFF0F0F0, lt);
      text(b, p, fr, "STAFF", mx + fnx * 0.056, 2.575, mz + fnz * 0.056, fnx, fnz, 0.03, 0xFFC01818, lt);
      // maintenance closet behind: shelving, mop bucket, fuse panel, bare bulb
      FacadeRenderer.box(b, p, fr, 9.6, 1.0, 21.3, 10.45, 3.6, 21.45, 0xFF8A8E92, lt);
      for (double y : new double[]{1.6, 2.3, 3.0}) FacadeRenderer.box(b, p, fr, 9.6, y, 20.9, 10.45, y + 0.05, 21.45, 0xFF8A8E92, lt);
      FacadeRenderer.box(b, p, fr, 9.7, 1.65, 21.0, 9.95, 1.95, 21.3, 0xFFE0C020, lt);
      FacadeRenderer.box(b, p, fr, 10.05, 2.35, 21.0, 10.35, 2.6, 21.3, 0xFF3070D0, lt);
      FacadeRenderer.cylinder(b, p, fr, 10.0, 19.9, 0.22, 1.0, 1.35, 12, 0xFFE0C020, lt, true);
      FacadeRenderer.cylinder(b, p, fr, 10.0, 19.9, 0.02, 1.35, 2.6, 4, 0xFF8A6A40, lt, false);
      FacadeRenderer.box(b, p, fr, 10.38, 2.0, 20.2, 10.47, 3.2, 20.9, 0xFF9A9EA2, lt);
      text(b, p, fr, "DANGER", 10.37, 2.95, 20.55, -1, 0, 0.018, 0xFFC01818, lt);
      FacadeRenderer.cylinder(b, p, fr, 9.9, 20.6, 0.08, 7.6, 7.75, 8, g(0xFFFFE8B0), ON ? 0xF000F0 : lt, true);
      slab(b, p, fr, cx - ux * 0.3 + nx * 0.06, cz - uz * 0.3 + nz * 0.06, cx + ux * 0.3 + nx * 0.06, cz + uz * 0.3 + nz * 0.06, 3.4, 3.7, 0.08, g(0xFF20C060), glow);
      text(b, p, fr, "EXIT", cx + nx * 0.11, 3.55, cz + nz * 0.11, nx, nz, 0.035, 0xFFFFFFFF, glow);
   }

   private static void bear(VertexConsumer b, Pose p, Frame fr, double x, double z, double y, int col, int lt) {
      FacadeRenderer.cylinder(b, p, fr, x, z, 0.34, y, y + 0.62, 14, col, lt, true);
      FacadeRenderer.cylinder(b, p, fr, x, z, 0.26, y + 0.62, y + 1.05, 14, col, lt, true);
      FacadeRenderer.cylinder(b, p, fr, x - 0.04, z - 0.17, 0.09, y + 0.95, y + 1.12, 8, col, lt, true);
      FacadeRenderer.cylinder(b, p, fr, x - 0.04, z + 0.17, 0.09, y + 0.95, y + 1.12, 8, col, lt, true);
      FacadeRenderer.cylinder(b, p, fr, x - 0.25, z, 0.08, y + 0.78, y + 0.86, 8, 0xFFF0E0D0, lt, true);
   }

   /** Translucent parts: entry returns and the vestibule glass. */
   static void glass(VertexConsumer b, Pose p, Frame fr, int light, int inLight) {
      int gl = 0x5099C8E8;
      for (int s = -1; s <= 1; s += 2) slab(b, p, fr, s * 3.5, -1.5, s * 1.75, 0.32, 1.5, 3.95, 0.03, gl, light);
      for (double[] w : DustinPlan.WALLS) if (w[5] == 2) slab(b, p, fr, w[0], w[1], w[2], w[3], 1.35, 3.95, 0.03, gl, inLight);
   }
}
