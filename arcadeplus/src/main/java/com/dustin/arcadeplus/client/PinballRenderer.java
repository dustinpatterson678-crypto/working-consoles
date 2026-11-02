package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.ArcadePlusMod;
import com.dustin.arcadeplus.PinballBlock;
import com.dustin.arcadeplus.PinballBlockEntity;
import com.dustin.arcadeplus.PinballSim;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Cabinet from the GeckoLib model; playfield, walls, bumpers, flippers, ball and the backglass score are drawn live. */
public class PinballRenderer extends GeoBlockRenderer<PinballBlockEntity, BlockEntityRenderState> {
   private static final Identifier WHITE = ArcadePlusMod.id("textures/misc/white.png");
   private static final int FULL = 15728880;
   private static int LIGHT = FULL;           // the machine's own block light (never 0: shader packs render light 0 pitch black)
   private static int BRIGHT = FULL;          // full glow only while the table has power

   public PinballRenderer(Context context) {
      super(context, new DefaultedItemGeoModel<>(ArcadePlusMod.id("pinball")));
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return true;
   }

   // playfield (u across, v up the table) -> model pixels, with the classic 6-degree-ish tilt
   private static float X(double u) { return (float)(u - 7.0); }
   private static float Z(double v) { return (float)(v - 6.0); }
   private static float Y(double v) { return (float)(12.85 + v * 0.09); }

   @Override
   public void submit(BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      if (RenderCull.far(state)) return;
      Minecraft mc = Minecraft.getInstance();
      Level level = mc.level;
      if (level == null || state.blockPos == null || !(level.getBlockEntity(state.blockPos) instanceof PinballBlockEntity be)) {
         return;
      }
      BlockState bs = level.getBlockState(state.blockPos);
      if (!(bs.getBlock() instanceof PinballBlock)) {
         return;
      }
      float f = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
      double bx = be.px + (be.x - be.px) * f, by = be.py + (be.y - be.py) * f;
      double aL = be.paL + (be.aL - be.paL) * f, aR = be.paR + (be.aR - be.paR) * f;
      long time = be.isPowered() ? level.getGameTime() : 0;
      final int br = be.isPowered() ? FULL : state.lightCoords;
      BRIGHT = br;
      int light = state.lightCoords;
      LIGHT = light;
      Direction facing = bs.getValue(PinballBlock.FACING);
      BlockPos outlet = be.outletPos();
      if (outlet != null && level.getBlockState(outlet).getBlock() instanceof com.dustin.wcore.block.OutletBlock) {
         Direction oFacing = level.getBlockState(outlet).getValue(com.dustin.wcore.block.OutletBlock.FACING);
         com.dustin.wcore.client.Cables.draw(pose, collector, state.blockPos,
            com.dustin.wcore.client.Cables.port(state.blockPos, state.blockPos, facing, 0.3, 0.15),
            com.dustin.wcore.client.Cables.port(state.blockPos, outlet, oFacing, 0.4375, 0.5), state.lightCoords);
      }
      pose.pushPose();
      pose.translate(0.5, 0.0, 0.5);
      pose.rotate(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
      ObjModel model = ObjModel.get("pinball_" + ((PinballBlock)bs.getBlock()).theme);   // realistic themed cabinet
      model.draw(pose, collector, "static", light, 0xFFFFFFFF);
      model.draw(pose, collector, "playfield", be.isPowered() ? Math.max(light, 0xA000A0) : light, 0xFFFFFFFF);
      model.draw(pose, collector, "backglass", br, be.isPowered() ? 0xFFFFFFFF : 0xFF808080);
      pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
      final com.dustin.arcadeplus.PinballLayout L = be.layout();
      final int[] tc = themeColors(L.name);
      collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) -> {
         BRIGHT = br;                               // geometry is drawn later: use this machine's glow
         LIGHT = light;
         boolean attract = !be.playing && be.isPowered();
         boolean bonus = time - be.bonusAt < 30;            // all three targets: strobe everything
         boolean party = attract && be.newHigh;               // fresh high score: rainbow light show
         float phase = (time + f) / 20.0F;
         // playfield surface with lane art
         // (the themed playfield art is part of the cabinet model)
         // arrow inserts pointing up the middle lane
         for (int i = 0; i < 4; i++) {
            double v0 = 11.2 + i * 1.6;
            boolean on = attract ? ((time / 4 + i) % 4 == 0) : bonus ? (time / 2) % 2 == 0 : (time / 5 - i) % 4 == 0;
            int c = party ? rainbow(phase + i * 0.2F) : on ? 0xFFFF8A30 : 0xFF5A2A10;
            quad(b, p, 5.4, v0, 7.0, v0, 7.0, v0 + 0.4, 6.2, v0 + 1.0, 0.02, c, on || party ? BRIGHT : light);
            quad(b, p, 5.4, v0, 6.2, v0 + 1.0, 6.2, v0 + 1.0, 5.4, v0 + 0.4, 0.02, c, on || party ? BRIGHT : light);
         }
         // chaser lights (faster during a game, all-on strobe on bonus)
         for (int i = 0; i < 3; i++) {
            double cx = 6.2 + (i - 1) * 1.5;
            boolean on = bonus ? (time / 2) % 2 == 0 : (time / (attract ? 6 : 3) + i) % 3 == 0;
            int c = party ? rainbow(phase + i * 0.33F) : on ? 0xFFFFE070 : 0xFF6A5A20;
            quad(b, p, cx - 0.4, 9.5, cx + 0.4, 9.5, cx + 0.4, 10.3, cx - 0.4, 10.3, 0.02, c, BRIGHT);
         }
         // insert lamp in front of each standup target: lit once it's been hit
         for (int i = 0; i < L.targets.length; i++) {
            boolean lit = (be.targets & (1 << i)) != 0 || bonus && (time / 2) % 2 == 0 || attract && (time / 6) % L.targets.length == i;
            double[] t = L.targets[i];
            double mu = (t[0] + t[2]) / 2 + (t[0] < 7 ? 1.0 : -1.0), mv = (t[1] + t[3]) / 2;
            disc(b, p, mu, mv, 0.32, 0.02, lit ? 0xFF60FF60 : 0xFF1A4A1A, lit ? BRIGHT : light);
         }
         // top rollover lanes: lamp per lane, lane guides between them
         for (int i = 0; i < L.rolls.length; i++) {
            double u = L.rolls[i][0], rv = L.rolls[i][1];
            boolean lit = (be.rollovers & (1 << i)) != 0 || attract && (time / 5) % L.rolls.length == i;
            disc(b, p, u, rv, 0.36, 0.02, party ? rainbow(phase + i * 0.3F) : lit ? tc[1] : Draw.shade(tc[1], 0.25F), lit || party ? BRIGHT : light);
            if (i < L.rolls.length - 1) wall(b, p, (u + L.rolls[i + 1][0]) / 2, rv - 1.2, (u + L.rolls[i + 1][0]) / 2, rv + 1.0, 0.12, 0.9, 0xFFD8D8E0, light);
         }
         // drop targets: stand up until knocked flat
         for (int i = 0; i < L.drops.length; i++) {
            double[] t = L.drops[i];
            boolean down = (be.drops & (1 << i)) != 0;
            wall(b, p, t[0], t[1], t[2], t[3], 0.22, down ? 0.08 : 1.2, down ? 0xFF303034 : tc[2], down ? light : attract || bonus ? BRIGHT : light);
            double mu = (t[0] + t[2]) / 2, mv = (t[1] + t[3]) / 2 - 0.9;
            disc(b, p, mu, mv, 0.25, 0.02, down ? tc[2] : Draw.shade(tc[2], 0.25F), down ? BRIGHT : light);
         }
         // spinner: a flag on a wire that spins as the ball passes
         if (L.spinner != null) {
            double[] sp = L.spinner;
            float ang = be.spin;
            double half = 0.06 + 0.5 * Math.abs(Math.cos(ang));
            wall(b, p, sp[0], sp[1], sp[2], sp[3], 0.04, 1.6, 0xFFC0C0C8, light);                 // wire
            wall(b, p, sp[0] + 0.2, sp[1], sp[2] - 0.2, sp[3], half * 0.4, 1.4, Math.cos(ang) > 0 ? tc[0] : 0xFFF0F0F0, BRIGHT);
            for (double u : new double[]{sp[0], sp[2]}) cyl(b, p, u, sp[1], 0.18, 0, 1.7, 0xFFC0C0C8, light);
         }
         // saucer: a dark cup with a flashing ring
         if (L.saucer != null) {
            boolean busy = be.hold > 0;
            disc(b, p, L.saucer[0], L.saucer[1], 0.95, 0.015, busy && (time / 2) % 2 == 0 ? 0xFFFFFFFF : tc[1], BRIGHT);
            disc(b, p, L.saucer[0], L.saucer[1], 0.7, 0.025, 0xFF050506, light);
         }
         // kickback lamp in the left outlane
         quad(b, p, 0.3, 3.0, 1.1, 3.0, 1.1, 4.2, 0.3, 4.2, 0.02, be.kickback ? ((time / 6) % 2 == 0 ? 0xFF40FF40 : 0xFF20A020) : 0xFF103010, be.kickback ? BRIGHT : light);
         // multiplier lamps (2x..5x) across the bottom of the playfield
         for (int i = 0; i < 4; i++) {
            boolean lit = be.mult >= i + 2;
            double u = 4.4 + i * 1.2;
            quad(b, p, u - 0.4, 6.4, u + 0.4, 6.4, u + 0.4, 7.0, u - 0.4, 7.0, 0.02, lit ? 0xFF40FFFF : 0xFF0E3A3A, lit ? BRIGHT : light);
         }
         // drain warning: the outlanes flash red for a second when a ball is lost
         if (time - be.drainAt < 20 && (time / 3) % 2 == 0) {
            quad(b, p, 4.6, 0.2, 9.4, 0.2, 9.4, 1.4, 4.6, 1.4, 0.02, 0xFFFF2020, BRIGHT);
         }
         // walls (slingshots glow after a hit and fade)
         for (double[] w : L.walls) {
            int col = w[5] > 0 ? tc[0] : 0xFFD8D8E0;
            boolean glow = false;
            if (w[4] > 0) {
               float g = glow(time - be.hitAt[w[0] < 7 ? 8 : 9], f);
               col = mix(0xFFE03030, 0xFFFFFFFF, g);
               glow = g > 0.05F || bonus;
               if (bonus && (time / 2) % 2 == 0) col = 0xFFFFFFFF;
            }
            wall(b, p, w[0], w[1], w[2], w[3], w[4] > 0 ? 0.35 : 0.22, 1.1, col, glow ? BRIGHT : light);
         }
         wall(b, p, 12.4, 21.0, 14.0, 23.6, 0.12, 0.6, 0xFF9A9AA8, light);  // lane gate
         // bumpers: glow and fade after a hit, colour cycle while idle
         for (int i = 0; i < L.bumpers.length; i++) {
            double[] c = L.bumpers[i];
            double BRr = c[2];
            float g = glow(time - be.hitAt[i], f);
            int base = i % 2 == 0 ? tc[0] : tc[1];
            if (attract) base = party ? rainbow(phase + i * 0.33F) : mix(base, 0xFF2A2A60, 0.5F + 0.5F * (float)Math.sin(phase * 3 + i * 2.1));
            int top = bonus && (time / 2) % 2 == 0 ? 0xFFFFFFFF : mix(base, 0xFFFFFFA0, g);
            cyl(b, p, c[0], c[1], BRr, 0, 0.9, g > 0.3F ? 0xFFFFF0C0 : 0xFFF0F0F0, light);
            cyl(b, p, c[0], c[1], BRr * 0.85, 0.9, 1.5 + g * 0.3, top, attract || g > 0.05F || bonus ? BRIGHT : light);
            if (g > 0.05F) {   // light ring on the playfield around a struck bumper
               double rr = BRr * (1.2 + (1 - g) * 0.8);
               ring(b, p, c[0], c[1], rr, 0.03, mix(0xFF1E3E78, 0xFFFFFFA0, g));
            }
         }
         // standup targets
         for (int i = 0; i < L.targets.length; i++) {
            boolean lit = (be.targets & (1 << i)) != 0 || bonus && (time / 2) % 2 == 0;
            double[] t = L.targets[i];
            wall(b, p, t[0], t[1], t[2], t[3], 0.22, 1.2, lit ? 0xFFFFE040 : 0xFF8A6A10, lit ? BRIGHT : light);
         }
         // flippers
         double[] lt = PinballSim.leftTip(aL), rt = PinballSim.rightTip(aR);
         wall(b, p, PinballSim.LPIV[0], PinballSim.LPIV[1], lt[0], lt[1], 0.45, 0.9, 0xFFF4F4F4, light);
         wall(b, p, PinballSim.RPIV[0], PinballSim.RPIV[1], rt[0], rt[1], 0.45, 0.9, 0xFFF4F4F4, light);
         cyl(b, p, PinballSim.LPIV[0], PinballSim.LPIV[1], 0.45, 0, 1.0, 0xFFE03030, light);
         cyl(b, p, PinballSim.RPIV[0], PinballSim.RPIV[1], 0.45, 0, 1.0, 0xFFE03030, light);
         // plunger rod pulled back by the charge, glowing as it charges
         double pull = be.charge * 1.6;
         box(b, p, 12.9, -1.2 - pull, 13.5, 0.7 - pull, 0.2, 0.8, mix(0xFFC0C0C8, 0xFFFF6020, be.charge), be.charge > 0.1F ? BRIGHT : light);
         // the ball
         if (be.playing) {
            ball(b, p, bx, by, light);
         }
         // ---- backglass ----
         // chase lamps along the top edge
         for (int i = 0; i < 13; i++) {
            float lx = -6.0F + i;
            boolean on = bonus || party ? (time / 2 + i) % 2 == 0 : (time / (attract ? 4 : 2) + i) % 4 == 0;
            int c = party ? rainbow(phase + i / 13.0F) : on ? 0xFFFFD040 : 0xFF4A3A10;
            Draw.box(b, p, lx - 0.3F, 29.0F, 21.8F, lx + 0.3F, 29.6F, 21.85F, c, on || party ? BRIGHT : light);
         }
         // high score row: "HI" + 7 digits in gold (blinks after a new record)
         boolean hiOn = !(party && (time / 4) % 2 == 0);
         int hiCol = party ? rainbow(phase) : 0xFFFFC020;
         segs(b, p, new int[]{0x76, 0x06}, -6.0, 27.2, 0.7, 1.4, 0xFF40E0FF, true);
         digits(b, p, be.highScore, 7, -3.4, 27.2, 0.7, 1.4, hiCol, hiOn);
         // score: steady in play, slow blink in attract mode; white flash on a jackpot
         int scoreCol = bonus && (time / 2) % 2 == 0 ? 0xFFFFFFFF : 0xFFFF5A20;
         digits(b, p, be.score, 7, -6.2, 23.3, 1.6, 2.8, scoreCol, !attract || (time / 10) % 3 != 0);
         digits(b, p, be.playing ? Math.max(1, 4 - be.balls) : 0, 1, 4.6, 20.6, 0.9, 1.6, 0xFF40E0FF, true);
      });
      pose.popPose();
   }

   /** Bumper cap colour, lamp colour, drop-target colour for each table. */
   private static int[] themeColors(String name) {
      return switch (name) {
         case "galaxy" -> new int[]{0xFF4A8CFF, 0xFFFF8A30, 0xFF40E0FF};
         case "haunted" -> new int[]{0xFF60FF60, 0xFFB040FF, 0xFFE0E040};
         case "pirate" -> new int[]{0xFFFFC020, 0xFFE03030, 0xFF30A0FF};
         case "speedway" -> new int[]{0xFFE02020, 0xFF2060FF, 0xFFF0F0F0};
         default -> new int[]{0xFFE02020, 0xFF4AC8FF, 0xFFFFE040};
      };
   }

   /** Round insert lamp flat on the playfield. */
   private static void disc(VertexConsumer b, Pose p, double cu, double cv, double r, double lift, int c, int light) {
      int n = 12;
      for (int i = 0; i < n; i += 2) {
         double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n, a2 = Math.PI * 2 * (i + 2) / n;
         quad(b, p, cu, cv, cu + Math.cos(a0) * r, cv + Math.sin(a0) * r, cu + Math.cos(a1) * r, cv + Math.sin(a1) * r, cu + Math.cos(a2) * r, cv + Math.sin(a2) * r, lift, c, light);
      }
   }

   // ---------- light helpers ----------

   /** 1 right after a hit, fading to 0 over ~8 ticks. */
   private static float glow(long age, float partial) {
      float a = age + partial;
      return a < 0 || a > 8 ? 0 : 1 - a / 8F;
   }

   private static int mix(int a, int c, float t) {
      t = Math.max(0, Math.min(1, t));
      int r = (int)(((a >> 16) & 255) * (1 - t) + ((c >> 16) & 255) * t);
      int g = (int)(((a >> 8) & 255) * (1 - t) + ((c >> 8) & 255) * t);
      int bl = (int)((a & 255) * (1 - t) + (c & 255) * t);
      return 0xFF000000 | r << 16 | g << 8 | bl;
   }

   private static int rainbow(float h) {
      return 0xFF000000 | net.minecraft.util.Mth.hsvToRgb(h - (float)Math.floor(h), 0.8F, 1.0F) & 0xFFFFFF;
   }

   /** Thin glowing ring flat on the playfield. */
   private static void ring(VertexConsumer b, Pose p, double cu, double cv, double r, double lift, int c) {
      int n = 12;
      for (int i = 0; i < n; i++) {
         double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n, r2 = r + 0.25;
         quad(b, p, cu + Math.cos(a0) * r, cv + Math.sin(a0) * r, cu + Math.cos(a1) * r, cv + Math.sin(a1) * r,
            cu + Math.cos(a1) * r2, cv + Math.sin(a1) * r2, cu + Math.cos(a0) * r2, cv + Math.sin(a0) * r2, lift, c, BRIGHT);
      }
   }

   // ---------- small geometry helpers in playfield units ----------

   private static void v(VertexConsumer b, Pose p, float x, float y, float z, float nx, float ny, float nz, int color, int light) {
      b.addVertex(p, x, y, z).setColor(color).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
   }

   /** Flat quad lying on the playfield, raised by lift pixels. */
   private static void quad(VertexConsumer b, Pose p, double u1, double v1, double u2, double v2, double u3, double v3, double u4, double v4, double lift, int c, int light) {
      float l = (float)lift;
      v(b, p, X(u1), Y(v1) + l, Z(v1), 0, 1, 0, c, light);
      v(b, p, X(u2), Y(v2) + l, Z(v2), 0, 1, 0, c, light);
      v(b, p, X(u3), Y(v3) + l, Z(v3), 0, 1, 0, c, light);
      v(b, p, X(u4), Y(v4) + l, Z(v4), 0, 1, 0, c, light);
   }

   /** A raised strip along a segment (walls, flippers, slingshots). */
   private static void wall(VertexConsumer b, Pose p, double u1, double v1, double u2, double v2, double half, double h, int c, int light) {
      double dx = u2 - u1, dy = v2 - v1, len = Math.hypot(dx, dy);
      if (len < 1e-6) return;
      double nx = -dy / len * half, ny = dx / len * half;
      double[][] pts = {{u1 + nx, v1 + ny}, {u2 + nx, v2 + ny}, {u2 - nx, v2 - ny}, {u1 - nx, v1 - ny}};
      prism(b, p, pts, 0, h, c, light);
   }

   private static void box(VertexConsumer b, Pose p, double u0, double v0, double u1, double v1, double h0, double h1, int c, int light) {
      prism(b, p, new double[][]{{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}}, h0, h1, c, light);
   }

   /** Octagonal post standing on the playfield. */
   private static void cyl(VertexConsumer b, Pose p, double cu, double cv, double r, double h0, double h1, int c, int light) {
      int n = 16;
      double[][] pts = new double[n][];
      for (int i = 0; i < n; i++) {
         double a = Math.PI * 2 * i / n + Math.PI / 16;
         pts[i] = new double[]{cu + Math.cos(a) * r, cv + Math.sin(a) * r};
      }
      for (int i = 0; i < n; i++) {     // sides
         double[] a = pts[i], d = pts[(i + 1) % n];
         side(b, p, a, d, h0, h1, Draw.shade(c, 0.8F), light);
      }
      for (int i = 1; i < n - 1; i += 2) {   // top as a fan of quads
         double[] a = pts[0], d = pts[i], e = pts[i + 1], g = pts[Math.min(n - 1, i + 2)];
         float t = (float)h1;
         v(b, p, X(a[0]), Y(a[1]) + t, Z(a[1]), 0, 1, 0, c, light);
         v(b, p, X(d[0]), Y(d[1]) + t, Z(d[1]), 0, 1, 0, c, light);
         v(b, p, X(e[0]), Y(e[1]) + t, Z(e[1]), 0, 1, 0, c, light);
         v(b, p, X(g[0]), Y(g[1]) + t, Z(g[1]), 0, 1, 0, c, light);
      }
   }

   private static void prism(VertexConsumer b, Pose p, double[][] pts, double h0, double h1, int c, int light) {
      float t = (float)h1;
      v(b, p, X(pts[0][0]), Y(pts[0][1]) + t, Z(pts[0][1]), 0, 1, 0, c, light);
      v(b, p, X(pts[1][0]), Y(pts[1][1]) + t, Z(pts[1][1]), 0, 1, 0, c, light);
      v(b, p, X(pts[2][0]), Y(pts[2][1]) + t, Z(pts[2][1]), 0, 1, 0, c, light);
      v(b, p, X(pts[3][0]), Y(pts[3][1]) + t, Z(pts[3][1]), 0, 1, 0, c, light);
      for (int i = 0; i < 4; i++) {
         side(b, p, pts[i], pts[(i + 1) % 4], h0, h1, Draw.shade(c, 0.75F), light);
      }
   }

   private static void side(VertexConsumer b, Pose p, double[] a, double[] d, double h0, double h1, int c, int light) {
      float lo = (float)h0, hi = (float)h1;
      float nx = (float)(d[1] - a[1]), nz = (float)-(d[0] - a[0]);
      v(b, p, X(a[0]), Y(a[1]) + hi, Z(a[1]), nx, 0, nz, c, light);
      v(b, p, X(a[0]), Y(a[1]) + lo, Z(a[1]), nx, 0, nz, c, light);
      v(b, p, X(d[0]), Y(d[1]) + lo, Z(d[1]), nx, 0, nz, c, light);
      v(b, p, X(d[0]), Y(d[1]) + hi, Z(d[1]), nx, 0, nz, c, light);
   }

   private static void ball(VertexConsumer b, Pose p, double u, double vv, int light) {
      float x = X(u), z = Z(vv), y = Y(vv) + (float)PinballSim.R, r = (float)PinballSim.R;
      Draw.box(b, p, x - r, y - r * 0.7F, z - r * 0.7F, x + r, y + r * 0.7F, z + r * 0.7F, 0xFFE8E8F0, light);
      Draw.box(b, p, x - r * 0.7F, y - r, z - r * 0.7F, x + r * 0.7F, y + r, z + r * 0.7F, 0xFFE8E8F0, light);
      Draw.box(b, p, x - r * 0.7F, y - r * 0.7F, z - r, x + r * 0.7F, y + r * 0.7F, z + r, 0xFFDADAE4, light);
   }

   /** 7-segment number on the backglass (model space, facing the player at -z). */
   private static void digits(VertexConsumer b, Pose p, int value, int count, double x0, double yc, double w, double h, int on, boolean show) {
      int[] seg = {0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F};
      String s = String.valueOf(Math.max(0, value));
      int[] bits = new int[count];
      for (int i = 0; i < count; i++) {
         int idx = s.length() - count + i;
         bits[i] = show && idx >= 0 ? seg[s.charAt(idx) - '0'] : 0;
      }
      segs(b, p, bits, x0, yc, w, h, on, true);
   }

   /** Raw 7-segment patterns (bit 0 = a ... bit 6 = g), one per position. */
   private static void segs(VertexConsumer b, Pose p, int[] pattern, double x0, double yc, double w, double h, int on, boolean show) {
      float z = 21.85F, t = (float)(w * 0.18);
      for (int i = 0; i < pattern.length; i++) {
         int bits = show ? pattern[i] : 0;
         // digits read left to right as seen by the player: model -x is the player's right, so step the other way
         float cx = (float)-(x0 + i * (w + 0.5)), cy = (float)yc;
         float hw = (float)w / 2, hh = (float)h / 2;
         float[][] segs = {
            {cx - hw, cy + hh - t, cx + hw, cy + hh}, {cx - hw, cy, cx - hw + t, cy + hh}, {cx - hw, cy - hh, cx - hw + t, cy},
            {cx - hw, cy - hh, cx + hw, cy - hh + t}, {cx + hw - t, cy - hh, cx + hw, cy}, {cx + hw - t, cy, cx + hw, cy + hh},
            {cx - hw, cy - t / 2, cx + hw, cy + t / 2}
         };
         int[] map = {0, 1, 2, 3, 4, 5, 6};   // bars are laid out a..g as the player sees them
         for (int sgm = 0; sgm < 7; sgm++) {
            boolean lit = (bits & (1 << sgm)) != 0;
            float[] r = segs[map[sgm]];
            Draw.box(b, p, r[0], r[1], z - 0.05F, r[2], r[3], z, lit ? on : 0xFF2A0E06, lit ? BRIGHT : LIGHT);
         }
      }
   }
}
