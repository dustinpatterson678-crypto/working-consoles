package com.dustin.arcadebuild.client;

import com.dustin.arcadebuild.ArcadeBuildMod;
import com.dustin.arcadebuild.BuildingPlan;
import com.dustin.arcadebuild.FacadeBlock;
import com.dustin.arcadebuild.FacadeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a whole building as one model from its plan: the structure, every textured surface (carpet with a
 * blacklight glow, wall panels, drop ceiling, stucco or brick, roof), wiring channels, the storefront with its
 * giant bulb-lit sign, awning and sliding doors. Coordinates are the plan's local blocks; the core block sits
 * at (FX, FY, FZ).
 */
public class FacadeRenderer implements BlockEntityRenderer<FacadeBlockEntity, BlockEntityRenderState> {
   static final Identifier WHITE = ArcadeBuildMod.id("textures/misc/white.png");
   private static final int BRIGHT = 15728880;
   private static final int FX = FacadeBlockEntity.FX, FY = FacadeBlockEntity.FY, FZ = FacadeBlockEntity.FZ;
   private static final Map<String, Identifier> TEX = new HashMap<>();
   static final Map<Character, String[]> FONT = new HashMap<>();

   static {
      String[][] f = {
         {"P", "11110", "10001", "10001", "11110", "10000", "10000", "10000"},
         {"B", "11110", "10001", "10001", "11110", "10001", "10001", "11110"},
         {"O", "01110", "10001", "10001", "10001", "10001", "10001", "01110"},
         {"F", "11111", "10000", "10000", "11110", "10000", "10000", "10000"},
         {"Y", "10001", "10001", "01010", "00100", "00100", "00100", "00100"},
         {"W", "10001", "10001", "10001", "10101", "10101", "10101", "01010"},
         {"Z", "11111", "00001", "00010", "00100", "01000", "10000", "11111"},
         {"X", "10001", "10001", "01010", "00100", "01010", "10001", "10001"},
         {"H", "10001", "10001", "10001", "11111", "10001", "10001", "10001"},
         {"M", "10001", "11011", "10101", "10101", "10001", "10001", "10001"},
         {"G", "01111", "10000", "10000", "10011", "10001", "10001", "01111"},
         {"N", "10001", "11001", "10101", "10011", "10001", "10001", "10001"},
         {"I", "11111", "00100", "00100", "00100", "00100", "00100", "11111"},
         {"C", "01111", "10000", "10000", "10000", "10000", "10000", "01111"},
         {"K", "10001", "10010", "10100", "11000", "10100", "10010", "10001"},
         {"E", "11111", "10000", "10000", "11110", "10000", "10000", "11111"},
         {"L", "10000", "10000", "10000", "10000", "10000", "10000", "11111"},
         {"A", "01110", "10001", "10001", "11111", "10001", "10001", "10001"},
         {"R", "11110", "10001", "10001", "11110", "10100", "10010", "10001"},
         {"D", "11110", "10001", "10001", "10001", "10001", "10001", "11110"},
         {"U", "10001", "10001", "10001", "10001", "10001", "10001", "01110"},
         {"S", "01111", "10000", "10000", "01110", "00001", "00001", "11110"},
         {"T", "11111", "00100", "00100", "00100", "00100", "00100", "00100"},
         {"'", "00100", "00100", "01000", "00000", "00000", "00000", "00000"},
      };
      for (String[] g : f) FONT.put(g[0].charAt(0), new String[]{g[1], g[2], g[3], g[4], g[5], g[6], g[7]});
   }

   private static Identifier tex(String n) {
      return TEX.computeIfAbsent(n, k -> ArcadeBuildMod.id("textures/building/" + k + ".png"));
   }

   public FacadeRenderer(BlockEntityRendererProvider.Context context) {
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
      return 128;
   }

   record Frame(Vec3 r, Vec3 f) {
      float[] map(double lx, double ly, double lz) {
         double ox = lx - FX, oy = ly - FY, oz = lz - FZ;
         return new float[]{(float)(0.5 + this.r.x * ox + this.f.x * oz), (float)oy, (float)(0.5 + this.r.z * ox + this.f.z * oz)};
      }
   }

   static void box(VertexConsumer b, Pose p, Frame fr, double x0, double y0, double z0, double x1, double y1, double z1, int col, int light) {
      float[] a = fr.map(x0, y0, z0), c = fr.map(x1, y1, z1);
      Draw.box(b, p, Math.min(a[0], c[0]), Math.min(a[1], c[1]), Math.min(a[2], c[2]), Math.max(a[0], c[0]), Math.max(a[1], c[1]), Math.max(a[2], c[2]), col, light);
   }

   static void vert(VertexConsumer b, Pose p, float[] q, float u, float v, float[] n, int col, int light) {
      b.addVertex(p, q[0], q[1], q[2]).setColor(col).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, n[0], n[1], n[2]);
   }

   /** One textured surface, tiled so the texture keeps its real size. */
   private static void surface(VertexConsumer b, Pose p, Frame fr, BuildingPlan.Surface s, int col, int light) {
      double ts = s.tile(), th = s.tileH();
      if (s.kind() == 'F' || s.kind() == 'C') {
         float[] n = {0, s.kind() == 'C' ? -1 : 1, 0};
         boolean ao = s.light() == BuildingPlan.IN;
         for (double x = s.a0(); x < s.a1() - 1e-6; x += ts) {
            for (double z = s.b0(); z < s.b1() - 1e-6; z += th) {
               double xe = Math.min(s.a1(), x + ts), ze = Math.min(s.b1(), z + th);
               float u1 = (float)((xe - x) / ts), v1 = (float)((ze - z) / th);
               vert(b, p, fr.map(x, s.c(), z), 0, 0, n, ao ? shadeFloor(col, s, x, z) : col, light);
               vert(b, p, fr.map(x, s.c(), ze), 0, v1, n, ao ? shadeFloor(col, s, x, ze) : col, light);
               vert(b, p, fr.map(xe, s.c(), ze), u1, v1, n, ao ? shadeFloor(col, s, xe, ze) : col, light);
               vert(b, p, fr.map(xe, s.c(), z), u1, 0, n, ao ? shadeFloor(col, s, xe, z) : col, light);
            }
         }
         return;
      }
      boolean alongX = s.kind() == 'X';
      float[] n = alongX ? new float[]{(float)fr.f.x, 0, (float)fr.f.z} : new float[]{(float)fr.r.x, 0, (float)fr.r.z};
      for (double a = s.a0(); a < s.a1() - 1e-6; a += ts) {
         for (double y = s.b0(); y < s.b1() - 1e-6; y += th) {
            double ae = Math.min(s.a1(), a + ts), ye = Math.min(s.b1(), y + th);
            float u1 = (float)((ae - a) / ts), vTop = (float)(1 - (ye - y) / th);
            float[] p00 = alongX ? fr.map(a, y, s.c()) : fr.map(s.c(), y, a), p10 = alongX ? fr.map(ae, y, s.c()) : fr.map(s.c(), y, ae);
            float[] p11 = alongX ? fr.map(ae, ye, s.c()) : fr.map(s.c(), ye, ae), p01 = alongX ? fr.map(a, ye, s.c()) : fr.map(s.c(), ye, a);
            boolean ao = s.light() == BuildingPlan.IN;
            int c0 = ao ? shadeWall(col, y) : col, c1 = ao ? shadeWall(col, ye) : col;
            vert(b, p, p00, 0, 1, n, c0, light);
            vert(b, p, p10, u1, 1, n, c0, light);
            vert(b, p, p11, u1, vTop, n, c1, light);
            vert(b, p, p01, 0, vTop, n, c1, light);
            if (s.kind() == 'X' || s.kind() == 'Z') {
               // Walls are single planes in the model. Draw the back too, otherwise a wall vanishes
               // whenever the camera gets behind it (for example through a doorway or a window reveal).
               float[] back = {-n[0], -n[1], -n[2]};
               vert(b, p, p01, 0, vTop, back, c1, light);
               vert(b, p, p11, u1, vTop, back, c1, light);
               vert(b, p, p10, u1, 1, back, c0, light);
               vert(b, p, p00, 0, 1, back, c0, light);
            }
         }
      }
   }

   // ---------- soft shadows baked into the vertices (corners and edges darken like real rooms) ----------

   static int mul(int col, double f) {
      int r = (int)(((col >> 16) & 255) * f), g = (int)(((col >> 8) & 255) * f), bl = (int)((col & 255) * f);
      return (col & 0xFF000000) | Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, bl);
   }

   private static int shadeWall(int col, double y) {
      double f = y < 2.4 ? 0.62 + 0.38 * (y - 1) / 1.4 : y > CEILY - 1.2 ? 1.0 - 0.18 * (y - (CEILY - 1.2)) / 1.2 : 1.0;
      return mul(col, Math.max(0.55, Math.min(1.0, f)));
   }

   private static int shadeFloor(int col, BuildingPlan.Surface s, double x, double z) {
      double d = Math.min(Math.min(x - s.a0(), s.a1() - x), Math.min(z - s.b0(), s.b1() - z));
      return mul(col, 0.6 + 0.4 * Math.min(1.0, d / 1.8));
   }

   private static final double CEILY = BuildingPlan.CEIL;

   // ---------- free-form geometry: real curves and slopes ----------

   /** Any four local points as one quad (flat colour on the white texture). */
   static void quad4(VertexConsumer b, Pose p, Frame fr, double[] a, double[] c, double[] d, double[] e, int col, int light) {
      float[] pa = fr.map(a[0], a[1], a[2]), pc = fr.map(c[0], c[1], c[2]), pd = fr.map(d[0], d[1], d[2]), pe = fr.map(e[0], e[1], e[2]);
      float ux = pc[0] - pa[0], uy = pc[1] - pa[1], uz = pc[2] - pa[2], vx = pe[0] - pa[0], vy = pe[1] - pa[1], vz = pe[2] - pa[2];
      float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx, len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz) + 1e-6F;
      float[] n = {nx / len, ny / len, nz / len};
      vert(b, p, pa, 0, 0, n, col, light);
      vert(b, p, pc, 1, 0, n, col, light);
      vert(b, p, pd, 1, 1, n, col, light);
      vert(b, p, pe, 0, 1, n, col, light);
      float[] m = {-n[0], -n[1], -n[2]};                                 // two-sided: curves are seen from both sides
      vert(b, p, pe, 0, 1, m, col, light);
      vert(b, p, pd, 1, 1, m, col, light);
      vert(b, p, pc, 1, 0, m, col, light);
      vert(b, p, pa, 0, 0, m, col, light);
   }

   /** Upright cylinder (columns, the corner tower). */
   static void cylinder(VertexConsumer b, Pose p, Frame fr, double cx, double cz, double r, double y0, double y1, int seg, int col, int light, boolean caps) {
      for (int i = 0; i < seg; i++) {
         double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
         double x0 = cx + Math.cos(a0) * r, z0 = cz + Math.sin(a0) * r, x1 = cx + Math.cos(a1) * r, z1 = cz + Math.sin(a1) * r;
         double shade = 0.78 + 0.22 * Math.cos((a0 + a1) / 2 - 0.8);
         quad4(b, p, fr, new double[]{x0, y0, z0}, new double[]{x1, y0, z1}, new double[]{x1, y1, z1}, new double[]{x0, y1, z0}, mul(col, shade), light);
         if (caps) {
            quad4(b, p, fr, new double[]{cx, y1, cz}, new double[]{x0, y1, z0}, new double[]{x1, y1, z1}, new double[]{cx, y1, cz}, col, light);
         }
      }
   }

   @Override
   public void submit(BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      Minecraft mc = Minecraft.getInstance();
      Level level = mc.level;
      if (level == null || state.blockPos == null || !(level.getBlockEntity(state.blockPos) instanceof FacadeBlockEntity be)) return;
      BlockState bs = level.getBlockState(state.blockPos);
      if (!(bs.getBlock() instanceof FacadeBlock)) return;
      BuildingPlan plan = be.plan();
      Direction fwd = bs.getValue(FacadeBlock.FACING), rt = fwd.getClockWise();
      Frame fr = new Frame(new Vec3(rt.getStepX(), 0, rt.getStepZ()), new Vec3(fwd.getStepX(), 0, fwd.getStepZ()));
      long ms = System.currentTimeMillis();
      boolean on = be.isPowered();
      final int light = LightCoordsUtil.getLightCoords(level, be.cell(0, 2, -4));
      final int inLight = LightCoordsUtil.getLightCoords(level, be.cell(0, 4, plan.HALL_BACK / 2));
      final int glow = on ? BRIGHT : inLight;
      final int sx = plan.sx();
      BlockPos door = be.cell(0, 1, -1);
      boolean near = mc.player != null && mc.player.distanceToSqr(Vec3.atCenterOf(door)) < 4.0 * 4.0;
      be.doorOpen += ((near ? 1F : 0F) - be.doorOpen) * 0.12F;
      float o = be.doorOpen;
      // 1.1 performance: the shell is real blocks now. Only submit the facade/details near the building,
      // and skip the interior surfaces entirely when the player is outside and far away.
      double distSq = mc.player == null ? 0 : mc.player.distanceToSqr(Vec3.atCenterOf(state.blockPos));
      if (distSq > 128.0 * 128.0) return;
      boolean inside = false;
      if (plan.id == 3 && mc.player != null) {
         Vec3 c0 = Vec3.atCenterOf(be.cell(0, 0, 0));
         double dx = mc.player.getX() - c0.x, dz = mc.player.getZ() - c0.z;
         double lx = dx * fr.r().x + dz * fr.r().z, lz = dx * fr.f().x + dz * fr.f().z;
         inside = lx > -plan.HX - 0.5 && lx < plan.HX + 0.5 && lz > -1.5 && lz < plan.BACK + 1.5
            && mc.player.getY() > c0.y && mc.player.getY() < c0.y + CEILY + 1.0;
      }
      final boolean drawInside = plan.id != 3 || inside || distSq < 48.0 * 48.0;
      // cheap behind-the-camera cull: the facade mesh is submitted even when the core is off-screen
      if (plan.id == 3 && !inside && mc.player != null) {
         Vec3 look = mc.player.getViewVector(1.0F);
         Vec3 to = Vec3.atCenterOf(state.blockPos).subtract(mc.player.position());
         if (to.dot(look) < -40.0) return;
      }
      int[][] dc = {{9, 19}, {-6, 32}, {6, 32}};
      for (int i = 0; i < 3; i++) {
         boolean nd = mc.player != null && mc.player.distanceToSqr(Vec3.atCenterOf(be.cell(dc[i][0], 1, dc[i][1]))) < 2.6 * 2.6;
         be.doors[i] += ((nd ? 1F : 0F) - be.doors[i]) * 0.15F;
      }
      collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) -> {
         int stucco = plan.id == 3 ? 0xFFC9B18C : plan.sign.startsWith("D") ? 0xFF9A3A2A : 0xFFE8DCC0, red = plan.id == 3 ? 0xFF4A3428 : 0xFFB02020, alu = plan.frameColor();
         if (plan.id != 3 && plan.id != 1) {
            for (BuildingPlan.Seg g : plan.segs()) {
               if (g.kind().equals("hidden")) continue;
               if (plan.id == 2 && g.kind().equals("wall")) continue;          // Grand Hall building: nobody sees the inside
               boolean outside = !g.kind().equals("wall");
               box(b, p, fr, g.x0() - 0.5, g.y0(), g.z0() - 0.5, g.x1() + 0.5, g.y1() + 1, g.z1() + 0.5, g.color(), outside ? light : inLight);
            }
         }
         for (int[] c : plan.id == 3 ? new int[0][] : plan.id == 2 ? new int[0][] : plan.columns()) {                                  // round columns with base and capital
            cylinder(b, p, fr, c[0], c[1], 0.42, 1.0, CEILY - 0.8, 16, 0xFFE8E2D4, inLight, false);
            cylinder(b, p, fr, c[0], c[1], 0.55, 1.0, 1.25, 16, 0xFF2A1A10, inLight, true);
            cylinder(b, p, fr, c[0], c[1], 0.55, CEILY - 0.8, CEILY - 0.6, 16, 0xFF2A1A10, inLight, true);
            cylinder(b, p, fr, c[0], c[1], 0.44, 4.4, 4.55, 16, 0xFFFF3AC8, glow, false);
         }
         if (plan.id == 1) DustinFront.draw(b, p, fr, plan, ms, on, light, inLight, glow, o, be.doors);
         if (plan.id == 3) OhWowFront.draw(b, p, fr, plan, ms, on, light, inLight, glow, o);
         for (BuildingPlan.Box x : plan.boxes()) {
            if (plan.id == 2 && x.light() != BuildingPlan.OUT) continue;
            if (plan.id == 3 && !drawInside && x.light() != BuildingPlan.OUT) continue;
            if (x.light() == BuildingPlan.GLOW && !on) {
               box(b, p, fr, x.x0(), x.y0(), x.z0(), x.x1(), x.y1(), x.z1(), 0xFF3A3A3A, inLight);
            } else {
               box(b, p, fr, x.x0(), x.y0(), x.z0(), x.x1(), x.y1(), x.z1(), x.color(), x.light() == BuildingPlan.OUT ? light : x.light() == BuildingPlan.GLOW ? glow : inLight);
            }
         }
         if (plan.id != 1 && plan.id != 3) {
         // parapet across the whole front, above the roof line
         box(b, p, fr, -sx - 0.5, 10, -1.55, sx + 0.5, 13, -0.5, stucco, light);
         box(b, p, fr, -sx - 0.7, 13, -1.75, sx + 0.7, 13.35, -0.4, red, light);
         box(b, p, fr, -sx - 0.5, 9.7, -1.65, sx + 0.5, 10.0, -0.5, red, light);
         // sign board with a neon border
         box(b, p, fr, -12.6, 10.25, -1.72, 12.6, 12.75, -1.55, 0xFF101018, light);
         int pink = 0xFFFF40C0;
         box(b, p, fr, -12.6, 12.63, -1.8, 12.6, 12.75, -1.72, pink, glow);
         box(b, p, fr, -12.6, 10.25, -1.8, 12.6, 10.37, -1.72, pink, glow);
         box(b, p, fr, -12.6, 10.25, -1.8, -12.48, 12.75, -1.72, pink, glow);
         box(b, p, fr, 12.48, 10.25, -1.8, 12.6, 12.75, -1.72, pink, glow);
         // NICKEL ARCADE in chasing neon
         String text = plan.sign;
         double px = 0.27, charW = 6 * px;
         double x = -(text.length() * charW - px) / 2.0;
         int sweep = (int)(ms / 120 % (text.length() + 6));
         for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            String[] g = FONT.get(ch);
            if (g != null) {
               int col = i < text.indexOf(' ') ? plan.signA : plan.signB;
               if (i == sweep || i == sweep - 1) col = 0xFFFFFFFF;
               for (int row = 0; row < 7; row++) {
                  for (int c = 0; c < 5; c++) {
                     if (g[row].charAt(c) == '1') {
                        double x0 = x + c * px, y0 = 12.35 - (row + 1) * px;
                        box(b, p, fr, x0, y0, -1.82, x0 + px * 0.9, y0 + px * 0.9, -1.72, col, glow);
                     }
                  }
               }
            }
            x += charW;
         }
         // marquee bulbs chasing along the top of the parapet
         int phase = (int)(ms / 180 % 3);
         for (int i = -sx; i <= sx; i++) {
            boolean lit = on && Math.floorMod(i, 3) == phase;
            box(b, p, fr, i - 0.12, 13.4, -1.3, i + 0.12, 13.64, -1.06, lit ? 0xFFFFE070 : 0xFF6A5A20, lit ? BRIGHT : light);
         }
         // striped awning over the doors, on two columns
         for (int i = -6; i < 6; i++) {
            box(b, p, fr, i, 4.9, -4.2, i + 1, 5.3, -1.55, (i & 1) == 0 ? plan.awningA : plan.awningB, light);
            box(b, p, fr, i, (i & 1) == 0 ? 4.35 : 4.55, -4.3, i + 1, 4.9, -4.2, (i & 1) == 0 ? plan.awningA : plan.awningB, light);
         }
         for (int s = -1; s <= 1; s += 2) {
            box(b, p, fr, s * 4.3 - 0.3, 1, -4.0, s * 4.3 + 0.3, 4.9, -3.4, stucco, light);
            box(b, p, fr, s * 4.3 - 0.4, 1, -4.1, s * 4.3 + 0.4, 1.4, -3.3, red, light);
         }
         }
         final double dz = plan.doorZ();
         // door frame
         box(b, p, fr, -1.75, 4.0, (dz - 0.12), 1.75, 4.25, (dz + 0.1), alu, light);
         box(b, p, fr, -1.75, 1.0, (dz - 0.12), -1.5, 4.0, (dz + 0.1), alu, light);
         box(b, p, fr, 1.5, 1.0, (dz - 0.12), 1.75, 4.0, (dz + 0.1), alu, light);
         // sliding door frames (glass drawn translucent below)
         double slide = o * 1.45;
         for (int s = -1; s <= 1; s += 2) {
            double a0 = s < 0 ? -1.5 - slide : 0 + slide, a1 = a0 + 1.5;
            box(b, p, fr, a0, 1.0, (dz - 0.08), a1, 1.12, (dz + 0.02), alu, light);
            box(b, p, fr, a0, 3.88, (dz - 0.08), a1, 4.0, (dz + 0.02), alu, light);
            box(b, p, fr, a0, 1.0, (dz - 0.08), a0 + 0.08, 4.0, (dz + 0.02), alu, light);
            box(b, p, fr, a1 - 0.08, 1.0, (dz - 0.08), a1, 4.0, (dz + 0.02), alu, light);
            box(b, p, fr, (s < 0 ? a1 - 0.3 : a0 + 0.22), 2.2, (dz - 0.12), (s < 0 ? a1 - 0.22 : a0 + 0.3), 2.8, (dz - 0.08), 0xFF303034, light);   // handles
         }
         // rooftop units
         int[][] units = plan.id == 1 ? new int[0][] : new int[][]{{-sx + 4, 6}, {-3, plan.BACK / 2}, {sx - 6, plan.BACK / 3}, {sx - 7, plan.BACK - 6}};
         for (int[] u : units) {
            box(b, p, fr, u[0], 10.0, u[1], u[0] + 2.4, 11.3, u[1] + 1.8, 0xFF9A9AA4, light);
            box(b, p, fr, u[0] + 0.5, 11.3, u[1] + 0.3, u[0] + 1.9, 11.4, u[1] + 1.5, 0xFF303034, light);
         }
      });
      // ---- textured surfaces, one draw per texture ----
      Map<String, List<BuildingPlan.Surface>> byTex = new LinkedHashMap<>();
      for (BuildingPlan.Surface s : plan.surfaces()) {
         if (s.light() == BuildingPlan.GLOW && !on) continue;
         if (plan.id == 2 && s.light() != BuildingPlan.OUT) continue;            // the blacklight glow is off with the breaker
         if (plan.id == 3 && !drawInside && s.light() != BuildingPlan.OUT) continue;
         if (plan.id == 1 && s.light() == BuildingPlan.OUT
            && s.kind() == 'F' && s.c() <= 5.0) continue;          // pavement is drawn by DustinFront
         byTex.computeIfAbsent(s.tex(), k -> new ArrayList<>()).add(s);
      }
      for (Map.Entry<String, List<BuildingPlan.Surface>> e : byTex.entrySet()) {
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(tex(e.getKey())), (p, b) -> {
            for (BuildingPlan.Surface s : e.getValue()) {
               int lt = s.light() == BuildingPlan.OUT ? light : s.light() == BuildingPlan.GLOW ? BRIGHT : inLight;
               surface(b, p, fr, s, s.color(), lt);
            }
         });
      }
      collector.submitCustomGeometry(pose, RenderTypes.entityCutout(tex("light_panel")), (p, b) -> {
         if (plan.id == 3 && !drawInside) return;
         for (int[] l : plan.id == 2 ? java.util.List.<int[]>of() : plan.lights()) {
            surface(b, p, fr, new BuildingPlan.Surface("light_panel", 'C', BuildingPlan.CEIL - 0.035, l[0] - 1, l[0] + 1, l[1] - 0.5, l[1] + 0.5, 2, 1, -1, 0), on ? 0xFFFFFFFF : 0xFF8A8A8A, glow);
         }
      });
      collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(WHITE), (p, b) -> {
         double slide = o * 1.45;
         for (int s = -1; s <= 1; s += 2) {
            double a0 = s < 0 ? -1.5 - slide : 0 + slide;
            box(b, p, fr, a0 + 0.08, 1.12, plan.doorZ() - 0.05, a0 + 1.42, 3.88, plan.doorZ() - 0.01, plan.id == 3 ? 0x503A4A48 : 0x6099C8E8, light);
         }
         if (plan.id != 3 && plan.id != 1) {
            for (int[] w : plan.windows()) {
               box(b, p, fr, w[0] - 0.5, w[2], -1.5, w[1] + 0.5, w[3] + 1, -1.46, 0x2A99C8E8, light);
               box(b, p, fr, w[0] - 0.5, w[2], 0.46, w[1] + 0.5, w[3] + 1, 0.5, 0x2A99C8E8, light);
            }
         }
         if (plan.id == 1) DustinFront.glass(b, p, fr, light, inLight);
      });
   }
}
