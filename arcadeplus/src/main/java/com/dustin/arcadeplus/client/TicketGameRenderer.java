package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.ArcadePlusMod;
import com.dustin.arcadeplus.TicketGame;
import com.dustin.arcadeplus.TicketGameBlock;
import com.dustin.arcadeplus.TicketGameBlockEntity;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Static machine from its GeckoLib model, plus the moving parts: balls, power meter, hoop, moles, pusher and claw. */
public class TicketGameRenderer extends GeoBlockRenderer<TicketGameBlockEntity, BlockEntityRenderState> {
   private static final Identifier WHITE = ArcadePlusMod.id("textures/misc/white.png");
   private static final int FULL = 15728880;
   private static int BRIGHT = FULL;          // set per machine: full glow when powered, normal light when unplugged

   public TicketGameRenderer(Context context, String model) {
      super(context, new DefaultedItemGeoModel<>(ArcadePlusMod.id(model)));
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return true;
   }

   @Override
   public void submit(BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      if (RenderCull.far(state)) return;
      Minecraft mc = Minecraft.getInstance();
      Level level = mc.level;
      boolean real = level != null && state.blockPos != null && level.getBlockEntity(state.blockPos) instanceof TicketGameBlockEntity rb && REAL.contains(rb.game());
      if (!real) super.submit(state, pose, collector, camera);           // realistic machines skip the old block model
      if (level == null || state.blockPos == null || !(level.getBlockEntity(state.blockPos) instanceof TicketGameBlockEntity be)) {
         return;
      }
      BlockState bs = level.getBlockState(state.blockPos);
      if (!(bs.getBlock() instanceof TicketGameBlock)) {
         return;
      }
      boolean powered = be.isPowered();
      double now = powered ? level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false) : 0;   // unplugged: frozen and dark
      final int br = powered ? FULL : state.lightCoords;
      BRIGHT = br;
      Direction facing = bs.getValue(TicketGameBlock.FACING);
      int light = state.lightCoords;
      BlockPos outlet = be.outletPos();
      if (outlet != null && level.getBlockState(outlet).getBlock() instanceof com.dustin.wcore.block.OutletBlock) {
         Direction oFacing = level.getBlockState(outlet).getValue(com.dustin.wcore.block.OutletBlock.FACING);
         com.dustin.wcore.client.Cables.draw(pose, collector, state.blockPos,
            com.dustin.wcore.client.Cables.port(state.blockPos, state.blockPos, facing, 0.3, 0.15),
            com.dustin.wcore.client.Cables.port(state.blockPos, outlet, oFacing, 0.4375, 0.5), state.lightCoords);
      }
      if (real) {
         pose.pushPose();
         pose.translate(0.5, 0.0, 0.5);
         pose.rotate(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
         realModel(pose, collector, be, now, light, br);
         pose.popPose();
         if (!OVERLAY.contains(be.game())) return;
      }
      pose.pushPose();
      pose.translate(0.5, 0.0, 0.5);
      pose.rotate(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
      pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
      TicketGame g = be.game();
      collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) -> {
         BRIGHT = br;                               // geometry is drawn later: use this machine's glow
         switch (g) {
            case SKEEBALL -> skee(b, p, be, now, light);
            case BASKETBALL -> hoop(b, p, be, now, light);
            case WHACKAMOLE -> moles(b, p, be, now, light);
            case COINPUSHER -> pusher(b, p, be, now, light);
            case CLAWMACHINE -> claw(b, p, be, now, light);
            case STACKER -> { if (be.isPowered()) stacker(b, p, be, now, light); }   // lamps stay dark without power
            case CYCLONE -> { if (be.isPowered()) cyclone(b, p, be, now, light); }
            case GATORS -> gators(b, p, be, now, light);
            case BIGWHEEL -> wheel(b, p, be, now, light);
            case ECHO -> echo(b, p, be, now, light);
            case DRUMWHEEL -> drum(b, p, be, now, light);
            case CLOWNS -> clowns(b, p, be, now, light);
         }
      });
      pose.popPose();
   }

   /** Machines that have been rebuilt as realistic OBJ models. */
   private static final java.util.Set<TicketGame> REAL = java.util.EnumSet.allOf(TicketGame.class);
   /** Realistic shells whose moving parts (balls, moles, claw, pads...) are still drawn live on top. */
   private static final java.util.Set<TicketGame> OVERLAY = java.util.EnumSet.of(TicketGame.SKEEBALL, TicketGame.BASKETBALL, TicketGame.WHACKAMOLE,
      TicketGame.COINPUSHER, TicketGame.CLAWMACHINE, TicketGame.GATORS, TicketGame.ECHO, TicketGame.CLOWNS);

   private static void realModel(PoseStack pose, SubmitNodeCollector collector, TicketGameBlockEntity be, double now, int light, int bright) {
      boolean on = be.isPowered();
      switch (be.game()) {
         case BIGWHEEL -> {
            ObjModel m = ObjModel.get("bigwheel");
            double angle;
            if (be.stage == 1) angle = TicketGame.wheelSpin(now - be.startTick);
            else if (be.stage == 3) angle = TicketGame.wheelCoast(be.lastAim / 10000.0, now - be.actionTick);
            else angle = TicketGame.wheelCoast(be.lastAim / 10000.0, TicketGame.WHEEL_STOP_TICKS);
            boolean moving = be.stage == 1 || be.stage == 3 && now - be.actionTick < TicketGame.WHEEL_STOP_TICKS;
            float hy = 1.52F, fy = hy + 0.46F;
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF203038);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            pose.pushPose();                                                     // STOP button dips when pressed
            if (be.stage == 3 && now - be.actionTick < 6) pose.translate(0F, -0.015F, 0F);
            m.draw(pose, collector, "button", on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            int chase = (int)(now / (moving ? 1.5 : 5));
            for (int i = 0; i < 28; i++) {
               boolean lit = on && (i + chase) % 4 != 0;
               m.draw(pose, collector, "bulb_" + i, on ? bright : light, lit ? 0xFFFFFFFF : 0xFF6A5A38);   // unlit = dim colour, same light: a light-level swap strobes under shaders
            }
            pose.pushPose();                                                     // the wheel turns about its hub
            pose.translate(0F, hy, 0F);
            pose.mulPose(new org.joml.Matrix4f().rotationZ((float)angle));
            pose.translate(0F, -hy, 0F);
            boolean flash = be.stage == 0 && now - be.actionTick < 60 && (int)(now / 3) % 2 == 0;
            m.draw(pose, collector, "wheel", on ? bright : light, flash ? 0xFFFFFFE0 : 0xFFFFFFFF);
            pose.popPose();
            double seg = Math.PI * 2 / TicketGame.WHEEL_SEGS;
            double rel = ((-angle) % seg + seg) % seg / seg;
            double kick = moving ? Math.max(0, 1 - rel * 3.5) * 0.5 : 0;
            pose.pushPose();                                                     // flapper kicks as pegs pass
            pose.translate(0F, fy, 0F);
            pose.mulPose(new org.joml.Matrix4f().rotationZ((float)kick));
            pose.translate(0F, -fy, 0F);
            m.draw(pose, collector, "flapper", light, 0xFFFFFFFF);
            pose.popPose();
         }
         case DRUMWHEEL -> {
            ObjModel m = ObjModel.get("bonusdrum");
            double a0 = be.lastAim / 10000.0;
            boolean rolling = be.stage == 3 && now - be.actionTick < TicketGame.DRUM_TICKS;
            double angle = be.stage == 3 ? TicketGame.drumAngle(a0, be.lastResult, now - be.actionTick) : a0;
            float cy = 1.20F, cz = 0F;
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF402030);
            m.draw(pose, collector, "bonus", on ? bright : light, on && (int)(now / 8) % 2 == 0 ? 0xFFFFFFFF : 0xFFB0A080);
            int chase = (int)(now / (rolling ? 1.5 : 5));
            for (int i = 0; i < 18; i++) {
               boolean lit = on && (i + chase) % 3 != 0;
               m.draw(pose, collector, "bulb_" + i, on ? bright : light, lit ? 0xFFFFFFFF : 0xFF6A5A38);   // unlit = dim colour, same light: a light-level swap strobes under shaders
            }
            boolean flash = be.stage == 0 && now - be.actionTick < 60 && (int)(now / 3) % 2 == 0;
            pose.pushPose();
            pose.translate(0F, cy, cz);
            pose.mulPose(new org.joml.Matrix4f().rotationX((float)angle));
            pose.translate(0F, -cy, -cz);
            m.draw(pose, collector, "drum", on ? bright : light, flash ? 0xFFFFFFE0 : 0xFFFFFFFF);
            pose.popPose();
            double seg = Math.PI * 2 / TicketGame.DRUM_SEGS;
            double rel = ((angle % seg) + seg) % seg / seg;
            double kick = rolling ? Math.max(0, 1 - rel * 3.5) * 0.5 : 0;
            float fy = cy + 0.14F, fz = cz - 0.39F;
            pose.pushPose();
            pose.translate(0F, fy, fz);
            pose.mulPose(new org.joml.Matrix4f().rotationX((float)-kick));
            pose.translate(0F, -fy, -fz);
            m.draw(pose, collector, "flapper", light, 0xFFFFFFFF);
            pose.popPose();
            double pull = be.stage == 3 ? Math.max(0, 1 - (now - be.actionTick) / 14.0) : 0;
            pose.pushPose();                                                     // the lever swings forward and down
            pose.translate(0F, 0.95F, 0F);
            pose.mulPose(new org.joml.Matrix4f().rotationX((float)(-1.2 * Math.sin(Math.min(1, pull) * Math.PI / 2))));
            pose.translate(0F, -0.95F, 0F);
            m.draw(pose, collector, "lever", light, 0xFFFFFFFF);
            pose.popPose();
         }
         case CYCLONE -> {
            ObjModel m = ObjModel.get("lightchaser");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            pose.pushPose();
            if (be.playing && now - be.actionTick < 6) pose.translate(0F, -0.012F, 0F);
            m.draw(pose, collector, "button", on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            int cur = be.playing ? TicketGame.cycLamp(now - be.startTick) : -1;
            boolean result = !be.playing && now - be.actionTick < 80;
            for (int i = 0; i < TicketGame.CYC_LAMPS; i++) {
               boolean lit;
               if (!on) lit = false;
               else if (be.playing) {
                  int d = (cur - i + TicketGame.CYC_LAMPS) % TicketGame.CYC_LAMPS;
                  lit = d <= 1 || i == 0 && (int)(now / 3) % 2 == 0;
               } else if (result) {
                  lit = i == be.lastResult ? (int)(now / 3) % 2 == 0 : be.lastResult == 0 && (int)(now / 2 + i) % 2 == 0;
               } else {
                  lit = ((int)(now / 2) + i) % 6 == 0 || i == 0;
               }
               m.draw(pose, collector, "lamp_" + i, lit ? bright : light, lit ? 0xFFFFFFFF : 0xFF383838);
            }
            final int jp = be.jackpot;
            final boolean pw = on;
            pose.pushPose();
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) ->
               Draw.digits(b, p, jp, 3, 1.25F, 19.6F, -2.26F, 0.75F, 1.6F, 0xFFFF3030, pw ? bright : light, pw));
            pose.popPose();
         }
         case STACKER -> {
            ObjModel m = ObjModel.get("towerstack");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF203038);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            pose.pushPose();
            if (be.playing && now - be.actionTick < 4) pose.translate(0F, -0.012F, 0F);
            m.draw(pose, collector, "button", on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            if (on) {
               pose.pushPose();
               pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
               collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) -> { BRIGHT = bright; stackGrid(b, p, be, now, light); });
               pose.popPose();
            }
         }
         case PLINKO -> {
            ObjModel m = ObjModel.get("plinko");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF203038);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            pose.pushPose();
            if (be.stage == 2 && now - be.actionTick < 5) pose.translate(0F, -0.012F, 0F);
            m.draw(pose, collector, "button", on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            double dx = be.stage == 1 ? TicketGame.plinkoX(now - be.startTick) : be.lastAim2 / 100.0;
            pose.pushPose();
            pose.translate((float)(dx / 16.0), 0F, 0F);
            m.draw(pose, collector, "dropper", on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            double[] pk = null;
            if (be.stage == 1) pk = new double[]{dx, TicketGame.PLINKO_TOP - 0.1};
            else if (be.stage == 2) pk = TicketGame.plinkoPos(be.lastAim2 / 100.0, be.lastAim, now - be.actionTick);
            else if (be.shots > 0 && now - be.actionTick < 200) pk = TicketGame.plinkoPos(be.lastAim2 / 100.0, be.lastAim, 999);
            if (pk != null) {
               pose.pushPose();
               pose.translate((float)(pk[0] / 16.0), (float)(pk[1] / 16.0), 0F);
               m.draw(pose, collector, "puck", light, 0xFFFFFFFF);
               pose.popPose();
            }
         }
         case STRIKER -> {
            ObjModel m = ObjModel.get("striker");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            double t = now - be.actionTick;
            double meter = be.stage == 1 ? TicketGame.strikerMeter(now - be.startTick) : be.stage == 2 ? TicketGame.strikerHeight(be.lastResult) : 0;
            for (int i = 0; i < 10; i++) {
               boolean lit = on && meter * 10 > i + 0.01;
               m.draw(pose, collector, "meter_" + i, lit ? bright : light, lit ? 0xFFFFFFFF : 0xFF383838);
            }
            double h = 0;                                                        // puck height 0..1
            if (be.stage == 2 && t >= 3) {
               double top = TicketGame.strikerHeight(be.lastResult), k = t - 3;
               h = k < 11 ? top * (1 - (1 - k / 11) * (1 - k / 11)) : Math.max(0, top * (1 - (k - 11) * (k - 11) / 144.0));
            }
            boolean bell = be.lastResult == 0 && (be.stage == 2 && t >= 14 || be.stage == 0 && now - be.actionTick < 60);
            int chase = (int)(now / 3);
            for (int i = 0; i < 14; i++) {
               boolean lit = on && (be.stage == 2 ? h * 13 >= i - 0.01 || bell && (int)(now / 2) % 2 == 0 : be.stage == 1 ? true : (i + chase) % 5 == 0);
               m.draw(pose, collector, "bulb_" + i, on ? bright : light, lit ? 0xFFFFFFFF : 0xFF6A5A38);   // unlit = dim colour, same light: a light-level swap strobes under shaders
            }
            m.draw(pose, collector, "bell", bell && (int)(now / 2) % 2 == 0 ? bright : light, 0xFFFFFFFF);
            pose.pushPose();
            pose.translate(0F, (float)(0.30 + h * (2.73 - 0.30)), 0F);
            m.draw(pose, collector, "puck", light, 0xFFFFFFFF);
            pose.popPose();
            double ang = be.stage == 1 ? -1.1 + Math.sin(now * 0.15) * 0.06 : be.stage == 2 && t < 3 ? -1.1 * (1 - t / 3) : 0;
            pose.pushPose();                                                     // hammer: raised while waiting, slams on the pad
            pose.translate(0F, 0.36F, -0.46F);
            pose.mulPose(new org.joml.Matrix4f().rotationX((float)ang));
            pose.translate(0F, -0.36F, 0.46F);
            m.draw(pose, collector, "hammer", light, 0xFFFFFFFF);
            pose.popPose();
            pose.pushPose();
            if (be.stage == 2 && t >= 2 && t < 6) pose.translate(0F, -0.02F, 0F);
            m.draw(pose, collector, "pad", light, 0xFFFFFFFF);
            pose.popPose();
         }
         case HOCKEY -> {
            ObjModel m = ObjModel.get("hockey");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF203038);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            float f = be.playing ? (float)Math.max(0, Math.min(1, now - be.hkSynced)) : 1F;
            float[] h = new float[8];
            for (int i = 0; i < 8; i++) h[i] = be.pHk[i] + (be.hk[i] - be.pHk[i]) * f;
            if (!be.playing && be.hk[5] == 0F && be.hk[7] == 0F) {          // never played: mallets at their ends
               h[5] = (float)(-TicketGame.HK_L + 1.6);
               h[7] = (float)(TicketGame.HK_L - 1.4);
            }
            String[] parts = {"puck", "mallet_p", "mallet_a"};
            for (int k = 0; k < 3; k++) {
               pose.pushPose();
               pose.translate(h[k * 2 + (k == 0 ? 0 : 2)] / 16F, 0F, (h[k * 2 + (k == 0 ? 1 : 3)] + (float)TicketGame.HK_CZ) / 16F);
               m.draw(pose, collector, parts[k], light, 0xFFFFFFFF);
               pose.popPose();
            }
            final int ys = be.score, ms = be.shots;
            final boolean pw = on, goal = be.playing && now - be.actionTick < 20 && (int)(now / 3) % 2 == 0;
            pose.pushPose();
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) -> {
               Draw.digits(b, p, ys, 2, 4.53F, 21.0F, 21.58F, 1.4F, 2.4F, 0xFF40A0FF, pw ? bright : light, pw && !(goal && be.lastResult == 1));
               Draw.digits(b, p, ms, 2, -2.83F, 21.0F, 21.58F, 1.4F, 2.4F, 0xFFFF3030, pw ? bright : light, pw && !(goal && be.lastResult == 2));
            });
            pose.popPose();
         }
         case LOCKER -> {
            ObjModel m = ObjModel.get("locker");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF203038);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            pose.pushPose();
            if (be.stage == 2 && now - be.actionTick < 5) pose.translate(0F, -0.012F, 0F);
            m.draw(pose, collector, "button", on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            double t = now - be.actionTick;
            boolean recent = be.stage == 2 || be.stage == 0 && t < 60 && be.actionTick > 0;
            int cur = be.stage == 1 ? TicketGame.lockerAt(now - be.startTick) : -1;
            for (int i = 0; i < TicketGame.LOCKERS; i++) {
               boolean gold = (be.stage != 0 || recent) && i == be.lastAim;
               boolean lit;
               if (!on) lit = false;
               else if (be.stage == 1) lit = i == cur || gold && (int)(now / 4) % 2 == 0;
               else if (recent) lit = i == be.lastResult && (int)(now / 3) % 2 == 0;
               else lit = ((int)(now / 4) + i) % 6 == 0;
               m.draw(pose, collector, "lamp_" + i, lit ? bright : light, lit ? 0xFFFFFFFF : 0xFF5A4A30);
               double open = i == be.lastResult && recent ? Math.min(1, Math.max(0, (be.stage == 2 ? t - 10 : 20) / 10.0)) : 0;
               float x1 = 0.40F - (i % 4) * 0.20F;
               pose.pushPose();
               if (open > 0) {
                  pose.translate(x1, 0F, -0.15F);
                  pose.mulPose(new org.joml.Matrix4f().rotationY((float)(-1.7 * open)));
                  pose.translate(-x1, 0F, 0.15F);
               }
               m.draw(pose, collector, "door_" + i, gold && on ? bright : light, gold ? 0xFFFFD040 : 0xFFFFFFFF);
               pose.popPose();
            }
         }
         case KEYVAULT -> {
            ObjModel m = ObjModel.get("keyvault");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF203038);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            pose.pushPose();
            if (be.stage == 2 && now - be.actionTick < 5) pose.translate(0F, -0.012F, 0F);
            m.draw(pose, collector, "button", be.stage == 1 && on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            pose.pushPose();
            if (be.stage == 3 && now - be.actionTick < 5) pose.translate(0F, -0.012F, 0F);
            m.draw(pose, collector, "button2", be.stage == 2 && on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            double xs = be.stage == 1 ? TicketGame.tri(now - be.startTick, TicketGame.KEY_XSTEPS) : be.stage == 0 && be.actionTick < 0 ? 0 : be.lastAim;
            double ys = be.stage == 2 ? TicketGame.tri(now - be.startTick, TicketGame.KEY_YSTEPS) : be.stage == 3 || be.stage == 0 && be.actionTick > 0 ? be.lastAim2 : 0;
            double push = be.stage == 3 ? Math.sin(Math.min(1, (now - be.actionTick) / 20.0) * Math.PI) * (be.lastResult >= 0 ? 0.06 : 0.025) : 0;
            pose.pushPose();
            pose.translate((float)(TicketGame.keyX(xs) / 16.0), (float)(TicketGame.keyY(ys) / 16.0), (float)push);
            m.draw(pose, collector, "key", on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            Minecraft mc = Minecraft.getInstance();
            double[] cols = {5.6, 2.8, 0.0, -2.8, -5.6}, rows = {29.0, 25.0, 21.0};
            for (int i = 0; i < 15; i++) {
               if (be.stage == 3 && i == be.lastResult && now - be.actionTick > 30) continue;   // the prize drops out
               net.minecraft.world.item.Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(TicketGame.KEY_PRIZES[i]));
               if (it == null) continue;
               PrizeDisplayRenderer.item(mc, mc.level, pose, collector, new net.minecraft.world.item.ItemStack(it), cols[i % 5] / 16.0, (rows[i / 5] + 2.2) / 16.0, 0.0, 0.17F, (float)(now * 2 + i * 40), light);
            }
         }
         case SPEEDLIGHTS -> {
            ObjModel m = ObjModel.get("speedlights");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF203038);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            for (int i = 0; i < 9; i++) {
               boolean lit = on && (be.playing ? i == be.lastResult : ((int)(now / 4) + i) % 9 == 0);
               pose.pushPose();
               if (be.playing && i != be.lastResult && now - be.actionTick < 3) pose.translate(0F, 0F, 0F);
               m.draw(pose, collector, "btn_" + i, lit ? bright : light, lit ? 0xFFFFFFFF : 0xFF404040);
               pose.popPose();
            }
            final int sc = be.score;
            final int left = be.playing ? (int)Math.max(0, (be.game().roundTicks - (now - be.startTick)) / 20) : 0;
            final boolean pw = on;
            pose.pushPose();
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) -> {
               Draw.digits(b, p, sc, 3, 3.6F, 28.7F, 1.88F, 1.6F, 2.8F, 0xFFFF3030, pw ? bright : light, pw);
               Draw.digits(b, p, left, 2, -2.5F, 28.7F, 1.88F, 1.2F, 2.2F, 0xFF40A0FF, pw ? bright : light, pw);
            });
            pose.popPose();
         }
         case DROPZONE -> {
            ObjModel m = ObjModel.get("dropzone");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            pose.pushPose();
            if (be.stage == 2 && now - be.actionTick < 5) pose.translate(0F, -0.012F, 0F);
            m.draw(pose, collector, "button", on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            double ang = on ? TicketGame.dzAngle(now - be.startTick) : 0;
            pose.pushPose();
            pose.mulPose(new org.joml.Matrix4f().rotationY((float)ang));
            m.draw(pose, collector, "disc", on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            double t = now - be.actionTick;
            if (be.stage == 2 || be.stage == 0 && be.lastResult >= 0 && t < 80) {
               double bx, by, bz;
               if (be.stage == 2 && t < TicketGame.DZ_FALL) {
                  double k = t / TicketGame.DZ_FALL;
                  bx = 0; bz = -0.30; by = 1.28 - (1.28 - 0.92) * k * k;
               } else {
                  double w = Math.PI * 2 / TicketGame.DZ_VALUES.length;
                  double phi = (be.lastResult + 0.5) * w - ang;           // rides round in its hole
                  bx = Math.cos(phi) * 0.30; bz = Math.sin(phi) * 0.30; by = 0.905;
               }
               pose.pushPose();
               pose.translate((float)bx, (float)by, (float)bz);
               m.draw(pose, collector, "ball", light, 0xFFFFFFFF);
               pose.popPose();
            }
         }
         case ROCKETRACE -> {
            ObjModel m = ObjModel.get("rocketrace");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF203038);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            pose.pushPose();
            if (be.stage == 1 && now - be.actionTick < 4) pose.translate(0F, -0.012F, 0F);
            m.draw(pose, collector, "button", on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            double meter = be.stage == 1 ? TicketGame.rocketMeter(now - be.startTick) : 0;
            for (int i = 0; i < 10; i++) {
               boolean lit = on && meter * 10 > i + 0.01;
               m.draw(pose, collector, "meter_" + i, lit ? bright : light, lit ? 0xFFFFFFFF : 0xFF383838);
            }
            double[] ly = {1.85, 1.6, 1.35, 1.1};
            for (int i = 0; i < 4; i++) {
               double prog = Math.min(1, be.stackRows[i] / (double)TicketGame.RACE_LEN);
               pose.pushPose();
               pose.translate((float)(0.36 - 0.69 * prog), (float)ly[i], 0F);
               m.draw(pose, collector, "rocket_" + i, on ? bright : light, 0xFFFFFFFF);
               pose.popPose();
            }
         }
         case LANES -> {
            ObjModel m = ObjModel.get("lanes");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF203038);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            double t = now - be.actionTick;
            boolean rolling = be.shots > 0 && t < TicketGame.LANE_ROLL;
            int down = be.shots > 0 && t >= TicketGame.LANE_ROLL - 2 && (be.playing || t < 200) ? be.moleMask : 0;
            for (int i = 0; i < 10; i++) {
               pose.pushPose();
               if ((down & (1 << i)) != 0) {
                  float px = (float)(TicketGame.PINS[i][0] / 16.0), pz = (float)(TicketGame.PINS[i][1] / 16.0 - 0.2);
                  double k = Math.min(1, (t - TicketGame.LANE_ROLL + 2) / 6.0);
                  pose.translate(px, 0.42F, pz);
                  pose.mulPose(new org.joml.Matrix4f().rotationX((float)(1.4 * k)));
                  pose.mulPose(new org.joml.Matrix4f().rotationZ((float)((i % 2 == 0 ? 0.5 : -0.5) * k)));
                  pose.translate(-px, -0.42F, -pz);
               }
               m.draw(pose, collector, "pin_" + i, light, 0xFFFFFFFF);
               pose.popPose();
            }
            if (rolling) {
               double k = t / TicketGame.LANE_ROLL;
               pose.pushPose();
               pose.translate((float)(be.lastAim / 100.0 / 16.0 * k), 0F, (float)(-0.35 + 1.35 * k));
               m.draw(pose, collector, "ball", light, 0xFFFFFFFF);
               pose.popPose();
            }
         }
         case METEOR -> {
            ObjModel m = ObjModel.get("meteor");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF402030);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            pose.pushPose();
            if (be.playing && now - be.actionTick < 4) pose.translate(0F, -0.012F, 0F);
            m.draw(pose, collector, "button", on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            double t = now - be.startTick;
            int col = be.playing ? TicketGame.cannonCol(t) : 2;
            pose.pushPose();
            pose.translate((float)(0.28 - col * 0.14), 0F, 0F);
            m.draw(pose, collector, "cannon", light, 0xFFFFFFFF);
            pose.popPose();
            if (be.playing) {
               double fall = TicketGame.meteorFall(t);
               for (int c = 0; c < TicketGame.METEOR_COLS; c++) {
                  if (be.stackRows[c] < 0) continue;
                  double k = Math.min(1, (t - be.stackRows[c]) / fall);
                  pose.pushPose();
                  pose.translate((float)(0.28 - c * 0.14), (float)(2.15 - 1.12 * k), 0F);
                  m.draw(pose, collector, "meteor", bright, 0xFFFFFFFF);
                  pose.popPose();
               }
               double st = now - be.actionTick;
               if (st < 4) {
                  int sc = be.lastResult >= 0 ? be.lastResult : -1 - be.lastResult;
                  pose.pushPose();
                  pose.translate((float)(0.28 - sc * 0.14), (float)(1.12 + st * 0.28), 0F);
                  m.draw(pose, collector, "shot", bright, 0xFFFFFFFF);
                  pose.popPose();
               }
            }
         }
         case POWERPUNCH -> {
            ObjModel m = ObjModel.get("powerpunch");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF402030);
            double meter = be.stage == 1 ? TicketGame.punchMeter(now - be.startTick) : be.stage == 2 ? be.lastResult / 999.0 : 0;
            for (int i = 0; i < 10; i++) {
               boolean lit = on && meter * 10 > i + 0.01;
               m.draw(pose, collector, "meter_" + i, lit ? bright : light, lit ? 0xFFFFFFFF : 0xFF383838);
            }
            double t = now - be.actionTick;
            double ang = be.stage == 2 ? -(be.lastResult / 999.0) * 1.5 * Math.exp(-t / 11.0) * Math.cos(t * 0.5) : Math.sin(now * 0.07) * 0.03;
            pose.pushPose();
            pose.translate(0F, 1.70F, -0.30F);
            pose.mulPose(new org.joml.Matrix4f().rotationX((float)ang));
            pose.translate(0F, -1.70F, 0.30F);
            m.draw(pose, collector, "bag", light, 0xFFFFFFFF);
            pose.popPose();
            final int shown = be.stage == 1 ? TicketGame.punchPower(meter) : be.lastResult < 0 ? 0 : be.lastResult;
            final boolean pw = on;
            pose.pushPose();
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) ->
               Draw.digits(b, p, shown, 3, 2.3F, 32.96F, -0.85F, 1.7F, 2.6F, 0xFFFF3030, pw ? bright : light, pw));
            pose.popPose();
         }
         case DEEPSEA -> {
            ObjModel m = ObjModel.get("deepsea");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF203038);
            double rt = be.stage == 0 ? now * 0.25 : now - be.startTick;
            double t = now - be.actionTick;
            int caught = be.stage == 2 && t >= TicketGame.HOOK_DROP ? be.lastResult : -1;
            double hookY = 1.55;
            if (be.stage == 2) hookY = t < TicketGame.HOOK_DROP ? 1.55 - (1.55 - 0.86) * t / TicketGame.HOOK_DROP : 0.86 + Math.min(1, (t - TicketGame.HOOK_DROP) / 16.0) * (1.40 - 0.86);
            for (int i = 0; i < TicketGame.FISH; i++) {
               int tint = TicketGame.FISH_VALUES[i] >= 10 ? 0xFFFFC830 : TicketGame.FISH_VALUES[i] >= 5 ? 0xFFFF5040 : TicketGame.FISH_VALUES[i] >= 2 ? 0xFF50A0FF : 0xFFF0F0F0;
               double a, fx, fz, fy;
               if (i == caught) {
                  a = 0; fx = 0; fz = -TicketGame.FISH_R; fy = hookY - 0.06;
               } else {
                  double at = be.stage == 2 && i == be.lastResult ? be.lastAim2 + TicketGame.HOOK_DROP : rt;
                  a = TicketGame.fishAngle(i, at);
                  fx = Math.sin(a) * TicketGame.FISH_R; fz = -Math.cos(a) * TicketGame.FISH_R; fy = 0.84 + 0.01 * Math.sin(now * 0.2 + i);
               }
               pose.pushPose();
               pose.translate((float)fx, (float)fy, (float)fz);
               pose.mulPose(new org.joml.Matrix4f().rotationY((float)-a));
               m.draw(pose, collector, "fish", light, tint);
               boolean open = i == caught || TicketGame.fishOpen(i, rt);
               pose.translate(0F, 0.056F, 0.046F);
               pose.mulPose(new org.joml.Matrix4f().rotationX(open ? -0.9F : 0F));
               pose.translate(0F, -0.056F, -0.046F);
               m.draw(pose, collector, "jaw", light, 0xFFFFFFFF);
               pose.popPose();
            }
            pose.pushPose();
            pose.translate(0F, (float)hookY, (float)-TicketGame.FISH_R);
            m.draw(pose, collector, "hook", light, 0xFFFFFFFF);
            pose.pushPose();
            pose.scale(1F, (float)(1.61 - hookY), 1F);
            pose.translate(0F, 1F, 0F);
            m.draw(pose, collector, "line", light, 0xFFFFFFFF);
            pose.popPose();
            pose.popPose();
            m.draw(pose, collector, "water", light, 0xFFFFFFFF);
         }
         case BEATBLASTER -> {
            ObjModel m = ObjModel.get("beatblaster");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            long t = (long)(now - be.startTick);
            for (int k = 0; k < 3; k++) {
               boolean flash = on && now < be.moleUntil[k];
               m.draw(pose, collector, "ring_" + k, flash || on && !be.playing ? bright : light, flash ? 0xFFFFFFFF : on ? 0xFF8A8A8A : 0xFF303030);
            }
            boolean[][] lit = new boolean[3][TicketGame.NOTE_LEDS];
            if (be.playing && on) {
               int b0 = (int)Math.floor((t - TicketGame.BEAT_START) / (double)TicketGame.BEAT) - 1;
               int b1 = b0 + 2 + TicketGame.NOTE_LEDS * TicketGame.NOTE_STEP / TicketGame.BEAT + 1;
               for (int bb = Math.max(0, b0); bb <= Math.min(TicketGame.lastBeat(), b1); bb++) {
                  int lane = TicketGame.noteLane(be.lastAim, bb);
                  if (lane < 0 || be.shotTick[lane] >= bb) continue;
                  long dt = TicketGame.beatTime(bb) - t;
                  int led = (int)Math.floor(dt / (double)TicketGame.NOTE_STEP);
                  if (led >= 0 && led < TicketGame.NOTE_LEDS) lit[lane][led] = true;
               }
            } else if (on) {
               int chase = (int)(now / 4) % TicketGame.NOTE_LEDS;
               for (int k = 0; k < 3; k++) lit[k][(chase + k * 3) % TicketGame.NOTE_LEDS] = true;
            }
            for (int k = 0; k < 3; k++) for (int j = 0; j < TicketGame.NOTE_LEDS; j++) {
               m.draw(pose, collector, "note_" + k + "_" + j, lit[k][j] ? bright : light, lit[k][j] ? 0xFFFFFFFF : 0xFF2A2A2A);
            }
            final int sc = be.score;
            final int left = be.playing ? (int)Math.max(0, (be.game().roundTicks - t) / 20) : 0;
            final boolean pw = on;
            pose.pushPose();
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) -> {
               Draw.digits(b, p, sc, 3, 2.6F, 35.0F, -0.55F, 1.3F, 1.9F, 0xFFFF3030, pw ? bright : light, pw);
               Draw.digits(b, p, left, 2, -1.2F, 35.0F, -0.55F, 1.0F, 1.6F, 0xFF40A0FF, pw ? bright : light, pw);
            });
            pose.popPose();
         }
         case SNIPIT -> {
            ObjModel m = ObjModel.get("snipit");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
            pose.pushPose();
            if (be.stage == 2 && now - be.actionTick < 5) pose.translate(0F, -0.012F, 0F);
            m.draw(pose, collector, "button", on ? bright : light, 0xFFFFFFFF);
            pose.popPose();
            double t = now - be.startTick;
            double sx = be.stage == 1 ? TicketGame.stringX(t) : be.stage == 2 ? be.lastAim2 / 1000.0 : on ? TicketGame.stringX(now) : 0;
            double cx = be.stage == 1 ? TicketGame.scissorX(t) : be.stage == 2 ? be.lastAim / 1000.0 : 0.8;
            boolean cut = be.stage == 2 && be.lastResult >= 0;
            double drop = cut ? Math.min(1, (now - be.actionTick) / 20.0) : 0;
            pose.pushPose();
            pose.translate((float)(sx * 0.36), 2.2F, 0F);
            if (cut) pose.scale(1F, 0.6F, 1F);
            m.draw(pose, collector, "string", light, 0xFFFFFFFF);
            pose.popPose();
            pose.pushPose();
            pose.translate((float)(cx * 0.36), 1.83F, 0F);
            m.draw(pose, collector, "scissors", light, 0xFFFFFFFF);
            pose.popPose();
            int prize = cut ? be.lastResult : (int)Math.floorMod(be.startTick / 20, TicketGame.SNIP_PRIZES.length);
            net.minecraft.world.item.Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(TicketGame.SNIP_PRIZES[prize]));
            if (it != null && !(be.stage == 0 && be.lastResult >= 0 && now - be.actionTick < 60)) {
               Minecraft mc = Minecraft.getInstance();
               PrizeDisplayRenderer.item(mc, mc.level, pose, collector, new net.minecraft.world.item.ItemStack(it), sx * 0.36, 1.45 - drop * 0.5, 0.0, 0.32F, (float)(Math.sin(now * 0.05) * 20), light);
            }
            m.draw(pose, collector, "glass", light, 0xFFFFFFFF);
         }
         case POOL -> {
            ObjModel m = ObjModel.get("pool");
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            float f = be.lastResult == 1 ? (float)Math.max(0, Math.min(1, now - be.hkSynced)) : 1F;
            boolean racked = be.stage != 0 || be.shots > 0 || be.pool[1] != 0F;
            double ballY = 0.80 + com.dustin.arcadeplus.PoolSim.R;
            if (racked) {
               for (int i = 0; i < 16; i++) {
                  if ((be.moleMask & (1 << i)) != 0) continue;
                  float u = be.pPool[i * 4] + (be.pool[i * 4] - be.pPool[i * 4]) * f, v = be.pPool[i * 4 + 1] + (be.pool[i * 4 + 1] - be.pPool[i * 4 + 1]) * f;
                  pose.pushPose();
                  pose.translate(0.5F + u, (float)ballY, 1.0F + v);
                  m.draw(pose, collector, "ball_" + i, light, 0xFFFFFFFF);
                  pose.popPose();
               }
            }
            boolean aiming = PoolAim.pos != null && PoolAim.pos.equals(be.getBlockPos());
            if (be.stage == 1 && be.lastResult != 1 && (be.moleMask & 1) == 0) {
               double ang = aiming ? PoolAim.angle : be.lastAim / 1000.0;
               double pull = aiming ? 0.02 + PoolAim.power * 0.18 : 0.06;
               pose.pushPose();
               pose.translate(0.5F + be.pool[0], (float)ballY + 0.01F, 1.0F + be.pool[1]);
               pose.mulPose(new org.joml.Matrix4f().rotationY((float)(Math.PI / 2 - ang)));
               pose.mulPose(new org.joml.Matrix4f().rotationX(-0.08F));
               pose.translate(0F, 0F, (float)-pull);
               m.draw(pose, collector, "cue", light, 0xFFFFFFFF);
               pose.popPose();
            }
         }
         default -> {
            ObjModel m = ObjModel.get(be.game().id());
            m.draw(pose, collector, "static", light, 0xFFFFFFFF);
            m.draw(pose, collector, "leds", on ? bright : light, on ? 0xFFFFFFFF : 0xFF203038);
            m.draw(pose, collector, "marquee", on ? bright : light, on ? 0xFFFFFFFF : 0xFF707070);
         }
      }
   }

   /** Tower Stack lamp grid on the tall cabinet face (1/16 units). */
   private static void stackGrid(VertexConsumer b, Pose p, TicketGameBlockEntity be, double now, int light) {
      float z = -1.02F;
      boolean lost = !be.playing && be.lastResult == -1 && now - be.actionTick < 40;
      boolean won = !be.playing && be.shots >= TicketGame.STACK_ROWS && now - be.actionTick < 80;
      for (int r = 0; r < TicketGame.STACK_ROWS; r++) {
         int start = -1, width = 0;
         boolean moving = false;
         if (r < be.shots) {
            start = be.stackRows[r] & 15;
            width = be.stackRows[r] >> 4;
         } else if (r == be.shots && be.playing) {
            int prevW = r == 0 ? 3 : be.stackRows[r - 1] >> 4;
            width = Math.min(prevW, TicketGame.stackMaxWidth(r));
            start = TicketGame.stackPos(now - be.startTick, r, width);
            moving = true;
         }
         float y = 18.6F + r * 1.75F;
         int col = r == TicketGame.STACK_ROWS - 1 ? 0xFFFF40C0 : r == TicketGame.STACK_MINOR ? 0xFF40FF70 : 0xFF40A0FF;
         if (r == TicketGame.STACK_MINOR || r == TicketGame.STACK_ROWS - 1) {                 // prize line markers
            Draw.box(b, p, -6.6F, y - 0.85F, z - 0.05F, 6.6F, y - 0.75F, z, r == TicketGame.STACK_MINOR ? 0xFF40FF70 : 0xFFFF40C0, BRIGHT);
         }
         for (int c = 0; c < TicketGame.STACK_COLS; c++) {
            float x = 4.95F - c * 1.65F;
            boolean on = c >= start && c < start + width;
            if (!be.playing && !won && !lost && be.shots == 0) on = ((int)(now / 4) + r + c) % 7 == 0;
            if (won) on = ((int)(now / 3) + r) % 2 == 0;
            if (lost && r == be.shots) on = (int)(now / 4) % 2 == 0;
            int cc = moving ? 0xFFFFE070 : lost && r == be.shots ? 0xFFFF3030 : col;
            Draw.box(b, p, x - 0.72F, y - 0.72F, z - 0.12F, x + 0.72F, y + 0.72F, z, on ? cc : Draw.shade(cc, 0.12F), on ? BRIGHT : light);
            if (on) Draw.box(b, p, x - 0.5F, y + 0.25F, z - 0.14F, x - 0.1F, y + 0.55F, z - 0.12F, 0xFFFFFFFF, BRIGHT);   // lens glint
         }
      }
   }

   private static void ball(VertexConsumer b, Pose p, double x, double y, double z, float r, int color, int light) {
      Draw.box(b, p, (float)(x - r), (float)(y - r), (float)(z - r), (float)(x + r), (float)(y + r), (float)(z + r), color, light);
   }

   private static double lerp(double a, double c, double f) {
      return a + (c - a) * f;
   }

   private static void skee(VertexConsumer b, Pose p, TicketGameBlockEntity be, double now, int light) {
      if (be.playing) {
         float pw = (float)TicketGame.skeePower(now);
         Draw.box(b, p, -6.6F, 9.0F, -7.5F, -6.2F, 9.3F, -7.5F + 22F * pw, pw > 0.88F ? 0xFFFF40C0 : pw > 0.5F ? 0xFFFFD040 : 0xFF40FF70, BRIGHT);
         for (int i = 0; i < 9 - be.shots; i++) {
            ball(b, p, 5.4 - (i % 3) * 1.6, 7.6, -6.6 + (i / 3) * 1.6, 0.7F, 0xFFF2E6C8, light);   // balls waiting in the tray
         }
      }
      double f = (now - be.actionTick) / 20.0;
      if (f >= 0 && f <= 1.15) {
         int pts = be.lastResult;
         double[] hole = switch (pts) {
            case 10 -> new double[]{0, 11};
            case 20 -> new double[]{0, 14};
            case 30 -> new double[]{0, 17};
            case 40 -> new double[]{0, 20};
            case 50 -> new double[]{-4.5, 23};
            case 100 -> new double[]{4.5, 23};
            default -> new double[]{5.2, 8.0};
         };
         double x, y, z;
         if (f < 0.7) {
            double k = f / 0.7;
            x = 0; y = 7.6 + k * 1.6; z = lerp(-6, 14, k);
         } else {
            double k = Math.min(1, (f - 0.7) / 0.3);
            x = lerp(0, hole[0], k); y = lerp(9.2, hole[1], k) + Math.sin(k * Math.PI) * 3; z = lerp(14, 15.4, k);
         }
         ball(b, p, x, y, z, 0.9F, 0xFFF2E6C8, light);
         if (f > 0.95 && pts > 0) {
            Draw.box(b, p, (float)hole[0] - 1.7F, (float)hole[1] - 1.3F, 15.7F, (float)hole[0] + 1.7F, (float)hole[1] + 1.3F, 15.8F, 0xFFFFFFFF, BRIGHT);
         }
      }
   }

   private static final int ORANGE = 0xFFFF6A1A;

   private static void hoop(VertexConsumer b, Pose p, TicketGameBlockEntity be, double now, int light) {
      double round = be.playing ? now - be.startTick : 0;
      float hx = (float)TicketGame.hoopX(Math.min(round, TicketGame.BASKETBALL.roundTicks));
      float ry = (float)TicketGame.RIM_Y, rz = (float)TicketGame.RIM_Z, rr = (float)TicketGame.RIM_R;
      boolean swish = now - be.actionTick < 10 && be.lastResult == 1;
      // backboard plate the hoop slides on, with the shooter's square
      Draw.box(b, p, hx - 3.4F, 16.6F, 17.85F, hx + 3.4F, 22.6F, 17.95F, 0xFFFAFAFA, light);
      Draw.box(b, p, hx - 1.6F, 18.2F, 17.8F, hx + 1.6F, 18.5F, 17.85F, 0xFFD02020, light);
      Draw.box(b, p, hx - 1.6F, 20.6F, 17.8F, hx + 1.6F, 20.9F, 17.85F, 0xFFD02020, light);
      Draw.box(b, p, hx - 1.6F, 18.2F, 17.8F, hx - 1.3F, 20.9F, 17.85F, 0xFFD02020, light);
      Draw.box(b, p, hx + 1.3F, 18.2F, 17.8F, hx + 1.6F, 20.9F, 17.85F, 0xFFD02020, light);
      // bracket and a proper round rim
      Draw.box(b, p, hx - 0.5F, ry - 0.6F, rz + rr, hx + 0.5F, ry + 0.2F, 17.85F, 0xFF9A9AA4, light);
      int n = 16;
      for (int i = 0; i < n; i++) {
         double a = Math.PI * 2 * i / n;
         float x = hx + (float)Math.cos(a) * rr, z = rz + (float)Math.sin(a) * rr;
         Draw.box(b, p, x - 0.35F, ry - 0.35F, z - 0.35F, x + 0.35F, ry + 0.15F, z + 0.35F, swish ? 0xFFFFE070 : ORANGE, swish ? BRIGHT : light);
      }
      // net: strands tapering down from the rim, swinging after a basket
      float sway = swish ? (float)Math.sin(now * 1.3) * 0.3F : 0F;
      for (int i = 0; i < 8; i++) {
         double a = Math.PI * 2 * i / 8;
         for (int k = 0; k < 4; k++) {
            float r = rr * (1F - k * 0.13F), y = ry - 0.6F - k * 0.7F;
            float x = hx + (float)Math.cos(a) * r + sway * k, z = rz + (float)Math.sin(a) * r;
            Draw.box(b, p, x - 0.12F, y - 0.7F, z - 0.12F, x + 0.12F, y, z + 0.12F, 0xFFF0F0F0, light);
         }
      }
      // scoreboard on top of the cage: baskets (left) and seconds left (right)
      int secs = be.playing ? (int)Math.ceil(Math.max(0, TicketGame.BASKETBALL.roundTicks - round) / 20.0) : 0;
      Draw.box(b, p, -6.5F, 23.4F, 17.85F, 6.5F, 27.6F, 17.95F, 0xFF101014, light);
      Draw.digits(b, p, be.score, 2, 5.6F, 25.5F, 17.8F, 1.6F, 3.0F, 0xFFFF5A20, BRIGHT, true);
      Draw.digits(b, p, secs, 2, -2.0F, 25.5F, 17.8F, 1.6F, 3.0F, secs <= 5 && be.playing ? 0xFFFF3030 : 0xFF40FF70, BRIGHT, true);
      // balls in the air
      for (int i = 0; i < 4; i++) {
         double t = now - be.shotTick[i];
         if (t < 0 || t > 120) continue;
         double[] s = TicketGame.shot(be.shotAim[i] / 1000.0, be.shotPow[i] / 1000.0, be.shotAt[i], t);
         if (s[3] < 2) {
            ballSeams(b, p, s[0], s[1], s[2], light);
         }
      }
      // balls waiting on the ramp
      if (be.playing) {
         for (int i = 0; i < 3; i++) {
            ballSeams(b, p, -3 + i * 2.6, 8.6, -5.6, light);
         }
      }
   }

   private static void ballSeams(VertexConsumer b, Pose p, double x, double y, double z, int light) {
      float r = (float)TicketGame.BALL_R;
      ball(b, p, x, y, z, r, ORANGE, light);
      Draw.box(b, p, (float)x - r - 0.02F, (float)y - 0.06F, (float)z - r - 0.02F, (float)x + r + 0.02F, (float)y + 0.06F, (float)z + r + 0.02F, 0xFF201008, light);
   }

   private static void moles(VertexConsumer b, Pose p, TicketGameBlockEntity be, double now, int light) {
      for (int i = 0; i < 5; i++) {
         float x = (float)TicketGame.HOLES[i][0], z = (float)TicketGame.HOLES[i][1];
         boolean up = (be.moleMask & (1 << i)) != 0;
         boolean bonked = be.lastResult == i && now - be.actionTick < 8;
         if (up || bonked) {
            float top = bonked ? 14.2F : 17.0F;
            Draw.box(b, p, x - 1.4F, 12.0F, z - 1.4F, x + 1.4F, top, z + 1.4F, 0xFF7A4A2A, light);
            Draw.box(b, p, x - 0.4F, top - 2.0F, z - 1.55F, x + 0.4F, top - 1.4F, z - 1.4F, 0xFFFF8AB0, light);   // nose
            Draw.box(b, p, x - 1.0F, top - 1.2F, z - 1.5F, x - 0.5F, top - 0.7F, z - 1.4F, 0xFF101010, light);    // eyes
            Draw.box(b, p, x + 0.5F, top - 1.2F, z - 1.5F, x + 1.0F, top - 0.7F, z - 1.4F, 0xFF101010, light);
            if (bonked) {
               Draw.box(b, p, x - 0.3F, top + 0.6F, z - 0.3F, x + 0.3F, top + 1.2F, z + 0.3F, 0xFFFFE040, BRIGHT);
            }
         }
      }
      if (be.playing) {
         float left = (float)Math.max(0, 1 - (now - be.startTick) / 600.0);
         Draw.box(b, p, -7F, 15.1F, -7.6F, -7F + 14F * left, 15.4F, -7.4F, left < 0.2F ? 0xFFFF3030 : 0xFF40FF70, BRIGHT);
      }
   }

   private static void pusher(VertexConsumer b, Pose p, TicketGameBlockEntity be, double now, int light) {
      float push = (float)(Math.sin(now * 0.06) * 2.0);
      Draw.box(b, p, -7F, 17.8F, -1F + push, 7F, 18.4F, 7.4F, 0xFFC0C0C8, light);     // moving pusher shelf
      Draw.box(b, p, -7F, 18.4F, 6.0F + push, 7F, 21.0F, 7.4F, 0xFF9A9AA4, light);    // pusher wall
      java.util.Random r = new java.util.Random(be.getBlockPos().asLong());
      for (int i = 0; i < 46; i++) {                                                    // coin carpet
         float cx = -6.4F + r.nextFloat() * 12.8F;
         boolean upper = i < 16;
         float cz = upper ? -0.6F + push + r.nextFloat() * 6.4F : -4.8F + r.nextFloat() * 8.4F;
         float cy = upper ? 18.4F : 13.4F;
         Draw.box(b, p, cx - 0.8F, cy, cz - 0.8F, cx + 0.8F, cy + 0.25F + (i % 3) * 0.25F, cz + 0.8F, 0xFFE8C040, light);
      }
      double f = (now - be.actionTick) / 15.0;
      if (f >= 0 && f <= 1) {
         Draw.box(b, p, -0.8F, (float)lerp(26, 18.6, f), -4.3F, 0.8F, (float)lerp(26, 18.6, f) + 0.3F, -2.7F, 0xFFFFE070, BRIGHT);  // falling token
      }
   }

   private static void lamp(VertexConsumer b, Pose p, float x, float y, float z, float hw, float hh, boolean on, int col, int light) {
      Draw.box(b, p, x - hw, y - hh, z - 0.08F, x + hw, y + hh, z, on ? col : Draw.shade(col, 0.18F), on ? BRIGHT : light);
   }

   private static void stacker(VertexConsumer b, Pose p, TicketGameBlockEntity be, double now, int light) {
      float z = -6.05F;
      boolean lost = !be.playing && be.lastResult == -1 && now - be.actionTick < 40;
      boolean won = !be.playing && be.shots >= TicketGame.STACK_ROWS && now - be.actionTick < 80;
      for (int r = 0; r < TicketGame.STACK_ROWS; r++) {
         int start = -1, width = 0;
         boolean moving = false;
         if (r < be.shots) {
            start = be.stackRows[r] & 15;
            width = be.stackRows[r] >> 4;
         } else if (r == be.shots && be.playing) {
            int prevW = r == 0 ? 3 : be.stackRows[r - 1] >> 4;
            width = Math.min(prevW, TicketGame.stackMaxWidth(r));
            start = TicketGame.stackPos(now - be.startTick, r, width);
            moving = true;
         }
         float y = 8.35F + r * 1.5F;
         int col = r == TicketGame.STACK_ROWS - 1 ? 0xFFFF40C0 : r == TicketGame.STACK_MINOR ? 0xFF40FF70 : 0xFF40A0FF;
         for (int c = 0; c < TicketGame.STACK_COLS; c++) {
            float x = 4.5F - c * 1.5F;   // column 0 is the player's left (model +x)
            boolean on = c >= start && c < start + width;
            if (!be.playing && !won && !lost && be.shots == 0) {
               on = ((int)(now / 4) + r + c) % 7 == 0;      // attract sparkle
            }
            if (won) on = ((int)(now / 3) + r) % 2 == 0;
            if (lost && r == be.shots) on = (int)(now / 4) % 2 == 0 && c >= 0;
            lamp(b, p, x, y, z, 0.6F, 0.6F, on, moving ? 0xFFFFE070 : lost && r == be.shots ? 0xFFFF3030 : col, light);
         }
      }
   }

   private static void cyclone(VertexConsumer b, Pose p, TicketGameBlockEntity be, double now, int light) {
      int cur = be.playing ? TicketGame.cycLamp(now - be.startTick) : -1;
      boolean result = !be.playing && now - be.actionTick < 80;
      for (int i = 0; i < TicketGame.CYC_LAMPS; i++) {
         double a = Math.PI * 2 * i / TicketGame.CYC_LAMPS;
         float x = (float)(Math.sin(a) * 6.0), zz = (float)(Math.cos(a) * 6.0);
         int col = i == 0 ? 0xFFFF2020 : i == 12 ? 0xFF4080FF : TicketGame.CYC_VALUES[i] >= 8 ? 0xFF40FF70 : 0xFFFFD040;
         boolean on;
         if (be.playing) {
            int d = (cur - i + TicketGame.CYC_LAMPS) % TicketGame.CYC_LAMPS;
            on = d <= 1 || i == 0 && (int)(now / 3) % 2 == 0;
         } else if (result) {
            on = i == be.lastResult ? (int)(now / 3) % 2 == 0 : be.lastResult == 0 && (int)(now / 2 + i) % 2 == 0;
         } else {
            on = ((int)(now / 2) + i) % 6 == 0 || i == 0;                 // idle chase
         }
         Draw.box(b, p, x - 0.6F, 11.4F, zz - 0.6F, x + 0.6F, on ? 11.9F : 11.6F, zz + 0.6F, on ? col : Draw.shade(col, 0.2F), on ? BRIGHT : light);
      }
      // jackpot counter on the centre pillar, facing the player
      Draw.digits(b, p, be.jackpot, 3, 1.05F, 15.2F, -1.55F, 0.6F, 1.1F, 0xFFFF3030, BRIGHT, true);
      // label strip under it
      Draw.box(b, p, -1.3F, 13.6F, -1.6F, 1.3F, 14.0F, -1.5F, 0xFFFFCC33, BRIGHT);
   }

   private static void gators(VertexConsumer b, Pose p, TicketGameBlockEntity be, double now, int light) {
      for (int i = 0; i < 5; i++) {
         float x = (float)TicketGame.GATOR_X[i];
         boolean out = (be.moleMask & (1 << i)) != 0;
         boolean bonked = be.lastResult == i && now - be.actionTick < 8;
         float front = out ? -4.6F : bonked ? 0.5F : 1.6F;          // snout tip: lunged out, or tucked in the cave
         int green = bonked ? 0xFF3A6A2A : 0xFF4A9A3A;
         Draw.box(b, p, x - 1.1F, 12.0F, front + 1.6F, x + 1.1F, 13.6F, 6.0F, green, light);           // head/neck
         Draw.box(b, p, x - 0.9F, 12.0F, front, x + 0.9F, 13.0F, front + 1.6F, green, light);          // snout
         Draw.box(b, p, x - 0.9F, 12.2F, front + 0.2F, x + 0.9F, 12.4F, front + 1.4F, 0xFFF4F4F4, light);   // teeth
         Draw.box(b, p, x - 0.9F, 13.6F, front + 2.0F, x - 0.4F, 14.1F, front + 2.6F, 0xFFFFE040, light);  // eyes
         Draw.box(b, p, x + 0.4F, 13.6F, front + 2.0F, x + 0.9F, 14.1F, front + 2.6F, 0xFFFFE040, light);
         if (bonked) {
            Draw.box(b, p, x - 0.3F, 14.6F, front + 1.0F, x + 0.3F, 15.2F, front + 1.6F, 0xFFFFE040, BRIGHT);
         }
      }
      if (be.playing) {
         float left = (float)Math.max(0, 1 - (now - be.startTick) / 600.0);
         Draw.box(b, p, -7F, 19.4F, 3.85F, -7F + 14F * left, 19.8F, 3.95F, left < 0.2F ? 0xFFFF3030 : 0xFF40FF70, BRIGHT);
      }
      Draw.digits(b, p, be.score, 2, 1.1F, 17.0F, 3.95F, 1.2F, 2.2F, 0xFFFF5A20, BRIGHT, true);
   }

   private static final int[] WHEEL_COLORS = {0xFFD42020, 0xFFF0C020, 0xFF2060D8, 0xFF20A848, 0xFF8A30C8};

   /**
    * Big Wheel, drawn like a real redemption wheel: a round backboard with a gold rim and a ring of chasing
    * bulbs, the wheel itself (true wedges with radial numbers, white dividers, rim pegs, chrome hub), a rubber
    * flapper at the top that kicks as each peg goes by, an arch of bulbs over the top and a lit sign.
    */
   private static void wheel(VertexConsumer b, Pose p, TicketGameBlockEntity be, double now, int light) {
      double angle;
      if (be.stage == 1) angle = TicketGame.wheelSpin(now - be.startTick);
      else if (be.stage == 3) angle = TicketGame.wheelCoast(be.lastAim / 10000.0, now - be.actionTick);
      else angle = TicketGame.wheelCoast(be.lastAim / 10000.0, TicketGame.WHEEL_STOP_TICKS);
      boolean on = be.isPowered(), moving = be.stage == 1 || be.stage == 3 && now - be.actionTick < TicketGame.WHEEL_STOP_TICKS;
      int lit = on ? BRIGHT : light;
      float cx = 0F, cy = 21.0F, R = 8.2F;
      int n = TicketGame.WHEEL_SEGS;
      double seg = Math.PI * 2 / n;
      boolean result = be.stage == 0 && now - be.actionTick < 60;
      // backboard, gold rim, bulb ring
      Draw.ring(b, p, cx, cy, -0.9F, 0F, 10.4F, 0, Math.PI * 2, 40, 0xFF141432, light);
      Draw.ring(b, p, cx, cy, -1.0F, 9.5F, 10.4F, 0, Math.PI * 2, 40, 0xFFD8A830, light);
      int chase = (int)(now / (moving ? 1.5 : 5));
      for (int i = 0; i < 28; i++) {
         double a = Math.PI * 2 * i / 28;
         float x = cx - (float)Math.sin(a) * 9.95F, y = cy + (float)Math.cos(a) * 9.95F;
         boolean bl = on && (i + chase) % 4 != 0;
         Draw.box(b, p, x - 0.32F, y - 0.32F, -1.5F, x + 0.32F, y + 0.32F, -1.0F, bl ? 0xFFFFF2B0 : 0xFF5A4A20, bl ? BRIGHT : light);
      }
      // the wheel: wedges, dividers, radial numbers
      for (int i = 0; i < n; i++) {
         double a0 = i * seg + angle, a1 = a0 + seg;
         int v = TicketGame.WHEEL_VALUES[i];
         boolean jackpot = v >= 100, win = result && i == be.lastResult && (int)(now / 3) % 2 == 0;
         int col = win ? 0xFFFFFFFF : jackpot ? ((int)(now / 4) % 2 == 0 && on ? 0xFFFFE070 : 0xFFE0A020) : v == 0 ? 0xFF2A2A30 : WHEEL_COLORS[i % WHEEL_COLORS.length];
         Draw.ring(b, p, cx, cy, -2.3F, 2.1F, R, a0, a1, 3, col, lit);
         Draw.ring(b, p, cx, cy, -2.35F, 2.1F, R, a0 - 0.012, a0 + 0.012, 1, 0xFFF4F4F4, lit);   // divider
         double am = a0 + seg / 2;
         float ux = -(float)Math.sin(am), uy = (float)Math.cos(am);    // outward along the radius
         float vx = uy, vy = -ux;                                      // across the wedge (not mirrored)
         String txt = String.valueOf(v);
         float px = 0.42F, len = (txt.length() * 4 - 1) * px;
         float r0 = R - 0.6F - len;
         float ox = cx + ux * r0 - vx * 1.5F * px, oy = cy + uy * r0 - vy * 1.5F * px;
         Draw.text(b, p, txt, ox, oy, -2.4F, ux, uy, vx, vy, px, jackpot || col == 0xFFF0C020 ? 0xFF1A1A1A : 0xFFFFFFFF, lit);
         // rim peg at the divider
         float gx = cx - (float)Math.sin(a0) * (R + 0.15F), gy = cy + (float)Math.cos(a0) * (R + 0.15F);
         Draw.box(b, p, gx - 0.22F, gy - 0.22F, -3.3F, gx + 0.22F, gy + 0.22F, -2.3F, 0xFFD0D4DC, light);
      }
      Draw.ring(b, p, cx, cy, -2.32F, R, R + 0.5F, 0, Math.PI * 2, 40, 0xFFC8CCD4, light);       // chrome rim
      Draw.ring(b, p, cx, cy, -2.5F, 0F, 2.1F, 0, Math.PI * 2, 20, 0xFFC8CCD4, light);           // hub
      Draw.ring(b, p, cx, cy, -2.6F, 0F, 1.4F, 0, Math.PI * 2, 20, 0xFFD02020, lit);
      for (int k = 0; k < 5; k++) Draw.ring(b, p, cx, cy, -2.65F, 0F, 1.0F, angle + k * Math.PI * 2 / 5 - 0.18, angle + k * Math.PI * 2 / 5 + 0.18, 1, 0xFFFFE070, lit);
      // flapper: kicks to the side every time a peg passes the top
      double rel = ((-angle) % seg + seg) % seg / seg;
      double kick = moving ? Math.max(0, 1 - rel * 3.5) * 0.45 : 0;
      float fx = cx, fy = cy + R + 1.9F;
      float tipx = fx + (float)Math.sin(kick) * 2.4F, tipy = fy - (float)Math.cos(kick) * 2.4F;
      float wx = (float)Math.cos(kick) * 0.6F, wy = (float)Math.sin(kick) * 0.6F;
      Draw.quadXY(b, p, fx - wx, fy - wy, fx + wx, fy + wy, tipx, tipy, tipx, tipy, -3.4F, 0xFFE01818, lit);
      Draw.box(b, p, fx - 0.7F, fy - 0.3F, -3.6F, fx + 0.7F, fy + 0.7F, -1.0F, 0xFF2A2A30, light);
      // arch of bulbs over the top and the sign
      Draw.ring(b, p, cx, cy, -0.8F, 10.6F, 12.2F, -1.2, 1.2, 16, 0xFF3A1050, light);
      for (int i = 0; i <= 14; i++) {
         double a = -1.2 + 2.4 * i / 14;
         float x = cx - (float)Math.sin(a) * 11.4F, y = cy + (float)Math.cos(a) * 11.4F;
         boolean bl = on && (i + chase) % 3 != 0;
         Draw.box(b, p, x - 0.3F, y - 0.3F, -1.3F, x + 0.3F, y + 0.3F, -0.8F, bl ? 0xFFFF60D0 : 0xFF4A1A40, bl ? BRIGHT : light);
      }
      Draw.box(b, p, -7.5F, cy + 12.3F, -1.2F, 7.5F, cy + 15.3F, -0.4F, 0xFF1A0A2A, light);
      Draw.box(b, p, -7.7F, cy + 12.1F, -1.25F, 7.7F, cy + 12.3F, -0.4F, 0xFFD8A830, light);
      Draw.box(b, p, -7.7F, cy + 15.3F, -1.25F, 7.7F, cy + 15.5F, -0.4F, 0xFFD8A830, light);
      String sign = "BIG WHEEL";
      float spx = 0.42F, sw = (sign.length() * 4 - 1) * spx;
      Draw.text(b, p, sign, sw / 2, cy + 12.75F, -1.25F, -1, 0, 0, 1, spx, on ? 0xFFFFE070 : 0xFF6A5A30, lit);
      // light columns up both sides of the backboard
      for (int s = -1; s <= 1; s += 2) {
         Draw.box(b, p, s * 10.9F - 0.4F, 12F, -0.9F, s * 10.9F + 0.4F, 30F, 0.4F, 0xFF20202A, light);
         for (int k = 0; k < 9; k++) {
            boolean bl = on && (k + chase) % 3 == 0;
            Draw.box(b, p, s * 10.9F - 0.3F, 12.6F + k * 2F, -1.1F, s * 10.9F + 0.3F, 13.6F + k * 2F, -0.9F, bl ? 0xFF40E8FF : 0xFF14303A, bl ? BRIGHT : light);
         }
      }
      // front art panel glow
      int[] art = {0xFFFF3AC8, 0xFFB040FF, 0xFF4080FF, 0xFF40E8FF};
      for (int k = 0; k < 4; k++) Draw.box(b, p, -5.5F, 2F + k * 1.65F, -6.1F, 5.5F, 3.5F + k * 1.65F, -6.07F, on ? art[(k + chase / 2) % 4] : 0xFF2A0A30, lit);
   }

   private static final int[] ECHO_COLORS = {0xFFFF3030, 0xFF3070FF, 0xFF30E050, 0xFFFFE030};

   private static void echo(VertexConsumer b, Pose p, TicketGameBlockEntity be, double now, int light) {
      int lit = -1;
      if (be.stage == 1) {
         double t = now - be.actionTick;
         int step = (int)Math.floor(t / TicketGame.ECHO_STEP);
         if (t >= 0 && step <= be.score && t - step * TicketGame.ECHO_STEP < 8) {
            lit = TicketGame.echoColor(be.lastAim, step);
         }
      }
      boolean flash = now - be.moleUntil[0] < 6;
      boolean idle = be.stage == 0;
      for (int q = 0; q < 4; q++) {
         float sx = (q & 1) == 0 ? 1 : -1, sz = (q & 2) == 0 ? -1 : 1;
         float x0 = sx > 0 ? 0.4F : -6.2F, z0 = sz < 0 ? -6.2F : 0.4F;
         boolean on = q == lit || flash && q == be.lastResult || idle && ((int)(now / 6) % 4 == q);
         if (idle && be.lastResult == -1 && now - be.actionTick < 30) on = (int)(now / 3) % 2 == 0;   // game over flash
         int c = ECHO_COLORS[q];
         Draw.box(b, p, x0, 11.0F, z0, x0 + 5.8F, on ? 11.6F : 11.4F, z0 + 5.8F, on ? c : Draw.shade(c, 0.3F), on ? BRIGHT : light);
      }
      Draw.box(b, p, -1.2F, 11.0F, -1.2F, 1.2F, 11.8F, 1.2F, 0xFF202028, light);              // centre hub
      Draw.digits(b, p, be.score, 2, 1.1F, 14.2F, 6.4F, 1.2F, 2.2F, be.stage == 2 ? 0xFF40FF70 : 0xFFFF5A20, BRIGHT, true);
   }

   /**
    * Bonus Drum, drawn like a real rolling-drum redemption game: a smooth number drum between chrome end caps
    * under a chrome hood arch with chasing bulbs, a rubber flapper that ticks over the segments, a side pull
    * lever with a red ball knob that swings down when you pull, and the lit bonus display on top.
    */
   private static void drum(VertexConsumer b, Pose p, TicketGameBlockEntity be, double now, int light) {
      double a0 = be.lastAim / 10000.0;
      boolean rolling = be.stage == 3 && now - be.actionTick < TicketGame.DRUM_TICKS;
      double angle = be.stage == 3 ? TicketGame.drumAngle(a0, be.lastResult, now - be.actionTick) : a0;
      boolean on = be.isPowered();
      int lit = on ? BRIGHT : light;
      float cy = 19.0F, cz = 1.5F, R = 5.0F, X = 5.0F;
      double seg = Math.PI * 2 / TicketGame.DRUM_SEGS;
      boolean result = be.stage == 0 && now - be.actionTick < 60;
      int[] base = {0xFF2060D8, 0xFFE8E8EC, 0xFFD42020, 0xFFE8E8EC};
      for (int i = 0; i < TicketGame.DRUM_SEGS; i++) {
         int v = TicketGame.DRUM_VALUES[i];
         boolean jackpot = v >= 500, big = v >= 25, win = result && i == be.lastResult && (int)(now / 3) % 2 == 0;
         int col = win ? 0xFFFFF4A0 : jackpot ? ((int)(now / 4) % 2 == 0 && on ? 0xFF40E060 : 0xFF20A040) : big ? 0xFF8A2AC8 : base[i % 4];
         double t0 = i * seg + angle, t1 = t0 + seg;
         for (int k = 0; k < 3; k++) {                                     // smooth curved face, 3 slices per segment
            double s0 = t0 + seg * k / 3, s1 = t0 + seg * (k + 1) / 3;
            if (Math.cos((s0 + s1) / 2) < -0.05) continue;                  // back of the drum is never seen
            float y0 = cy + (float)(Math.sin(s0) * R), z0 = cz - (float)(Math.cos(s0) * R), y1 = cy + (float)(Math.sin(s1) * R), z1 = cz - (float)(Math.cos(s1) * R);
            int sh = Draw.shade(col, (float)(0.62 + 0.38 * Math.max(0, Math.cos((s0 + s1) / 2))));
            Draw.quad3(b, p, new float[]{-X, y0, z0}, new float[]{X, y0, z0}, new float[]{X, y1, z1}, new float[]{-X, y1, z1}, sh, lit);
         }
         double tm = t0 + seg / 2, c = Math.cos(tm);
         if (Math.cos(t0) > -0.05) {                                          // dark divider line
            float y = cy + (float)(Math.sin(t0) * (R + 0.03)), z = cz - (float)(Math.cos(t0) * (R + 0.03));
            Draw.quad3(b, p, new float[]{-X, y - 0.06F, z}, new float[]{X, y - 0.06F, z}, new float[]{X, y + 0.06F, z}, new float[]{-X, y + 0.06F, z}, 0xFF141418, lit);
         }
         if (c > 0.35) {                                                      // the value, foreshortened with the curve
            String txt = String.valueOf(v);
            float px = 0.55F, w = (txt.length() * 4 - 1) * px;
            float y = cy + (float)(Math.sin(tm) * R), z = cz - (float)(c * R) - 0.08F;
            Draw.text(b, p, txt, w / 2, y - 2.5F * px * (float)c, z, -1, 0, 0, (float)c, px, jackpot || big || i % 4 == 0 || i % 4 == 2 ? 0xFFFFFFFF : 0xFF141418, lit);
         }
      }
      // chrome end caps and axle bosses
      for (int s2 = -1; s2 <= 1; s2 += 2) {
         Draw.ringYZ(b, p, s2 * (X + 0.05F), cy, cz, 0F, R + 0.35F, 28, 0xFFC8CCD4, light);
         Draw.ringYZ(b, p, s2 * (X + 0.12F), cy, cz, 0F, 1.2F, 16, 0xFF8A8E96, light);
      }
      // hood: chrome arch tubes front and back of the drum with a bulb row
      int chase = (int)(now / (rolling ? 1.5 : 5));
      for (int i = 0; i <= 16; i++) {                                         // arch over the drum, seen from the front
         double h = Math.PI * i / 16;                                           // 0 = front-bottom, PI = back-bottom
         float y = cy + (float)(Math.sin(h) * 6.6F), z = cz - (float)(Math.cos(h) * 6.6F);
         if (i < 16) {
            double h2 = Math.PI * (i + 1) / 16;
            float y2 = cy + (float)(Math.sin(h2) * 6.6F), z2 = cz - (float)(Math.cos(h2) * 6.6F);
            for (int s2 = -1; s2 <= 1; s2 += 2) {
               float x = s2 * 6.3F;
               Draw.quad3(b, p, new float[]{x - 0.25F, y, z}, new float[]{x + 0.25F, y, z}, new float[]{x + 0.25F, y2, z2}, new float[]{x - 0.25F, y2, z2}, 0xFFD0D4DC, light);
            }
         }
         boolean bl = on && (i + chase) % 3 != 0;
         for (int s2 = -1; s2 <= 1; s2 += 2) {
            float x = s2 * 6.3F;
            Draw.box(b, p, x - 0.3F, y - 0.3F, z - 0.3F, x + 0.3F, y + 0.3F, z + 0.3F, bl ? 0xFFFFF2B0 : 0xFF5A4A20, bl ? BRIGHT : light);
         }
      }
      // flapper on the player's right, ticking over the dividers
      double rel = ((angle % seg) + seg) % seg / seg;
      double kick = rolling ? Math.max(0, 1 - rel * 3.5) * 0.5 : 0;
      float fy = cy, fz = cz - R - 1.6F, fx = -4.4F;
      float tipy = fy - (float)Math.sin(kick) * 1.8F, tipz = fz + (float)Math.cos(kick) * 1.4F;
      Draw.quad3(b, p, new float[]{fx - 0.6F, fy + 0.3F, fz}, new float[]{fx + 0.6F, fy + 0.3F, fz}, new float[]{fx + 0.6F, tipy, tipz}, new float[]{fx - 0.6F, tipy, tipz}, 0xFFE01818, lit);
      Draw.box(b, p, fx - 0.8F, fy + 0.2F, fz - 0.5F, fx + 0.8F, fy + 0.8F, fz + 0.2F, 0xFF2A2A30, light);
      // side pull lever (player's right): swings forward and down while the drum rolls
      double pull = be.stage == 3 ? Math.max(0, 1 - (now - be.actionTick) / 14.0) : 0;
      double la = Math.toRadians(-20 - 70 * Math.sin(Math.min(1, pull) * Math.PI / 2));   // angle from vertical toward the player
      float lx = -8.6F, ly = 12.0F, lz = -1.0F, L = 8.0F;
      float ey = ly + (float)(Math.cos(la) * L), ez = lz + (float)(Math.sin(la) * L);
      Draw.box(b, p, lx - 0.5F, ly - 1.0F, lz - 1.0F, lx + 0.4F, ly + 1.0F, lz + 1.0F, 0xFFC8CCD4, light);       // pivot boss
      Draw.quad3(b, p, new float[]{lx - 0.15F, ly, lz - 0.2F}, new float[]{lx - 0.15F, ly, lz + 0.2F}, new float[]{lx - 0.15F, ey, ez + 0.2F}, new float[]{lx - 0.15F, ey, ez - 0.2F}, 0xFFD0D4DC, light);
      Draw.quad3(b, p, new float[]{lx - 0.35F, ly, lz}, new float[]{lx + 0.05F, ly, lz}, new float[]{lx + 0.05F, ey, ez}, new float[]{lx - 0.35F, ey, ez}, 0xFFB0B4BC, light);
      Draw.box(b, p, lx - 0.9F, ey - 0.75F, ez - 0.75F, lx + 0.5F, ey + 0.75F, ez + 0.75F, 0xFFE01818, lit);   // ball knob
      Draw.box(b, p, lx - 0.7F, ey - 0.95F, ez - 0.55F, lx + 0.3F, ey + 0.95F, ez + 0.55F, 0xFFE01818, lit);
      // bonus display in the arch on top
      Draw.digits(b, p, TicketGame.DRUM_VALUES[15], 3, 1.9F, 30.7F, -6.12F, 1.2F, 2.2F, on && (int)(now / 8) % 2 == 0 ? 0xFFFFE040 : 0xFFE0A020, lit, on);
   }

   private static void clowns(VertexConsumer b, Pose p, TicketGameBlockEntity be, double now, int light) {
      double t = be.playing ? now - be.startTick : 0;
      int[] hair = {0xFFFF4040, 0xFF40A0FF, 0xFF40D060, 0xFFFFA020};
      for (int i = 0; i < 4; i++) {
         float x = (float)TicketGame.clownX(i, t), z = (float)TicketGame.CLOWN_Z, y = (float)TicketGame.CLOWN_Y;
         boolean down = (be.moleMask & (1 << i)) != 0;
         Draw.box(b, p, x - 0.3F, y - 1.2F, z + 0.2F, x + 0.3F, y, z + 0.8F, 0xFF9A9AA8, light);       // post on the rail
         if (down) {                                                                                // knocked flat
            Draw.box(b, p, x - 1.2F, y, z + 0.6F, x + 1.2F, y + 0.8F, z + 4.0F, 0xFFF4F4F4, light);
            Draw.box(b, p, x - 1.4F, y + 0.2F, z + 3.6F, x + 1.4F, y + 1.0F, z + 4.4F, hair[i], light);
         } else {
            Draw.box(b, p, x - 1.2F, y, z, x + 1.2F, y + 3.2F, z + 1.4F, 0xFFF4F4F4, light);          // face
            Draw.box(b, p, x - 0.35F, y + 1.2F, z - 0.25F, x + 0.35F, y + 1.9F, z, 0xFFFF2020, BRIGHT); // red nose
            Draw.box(b, p, x - 0.9F, y + 2.1F, z - 0.1F, x - 0.4F, y + 2.6F, z, 0xFF101010, light);    // eyes
            Draw.box(b, p, x + 0.4F, y + 2.1F, z - 0.1F, x + 0.9F, y + 2.6F, z, 0xFF101010, light);
            Draw.box(b, p, x - 0.8F, y + 0.4F, z - 0.1F, x + 0.8F, y + 0.7F, z, 0xFFD02020, light);    // smile
            Draw.box(b, p, x - 1.8F, y + 2.0F, z + 0.2F, x - 1.2F, y + 3.2F, z + 1.2F, hair[i], light); // hair tufts
            Draw.box(b, p, x + 1.2F, y + 2.0F, z + 0.2F, x + 1.8F, y + 3.2F, z + 1.2F, hair[i], light);
            Draw.box(b, p, x - 0.6F, y + 3.2F, z + 0.3F, x + 0.6F, y + 4.4F, z + 1.1F, hair[(i + 1) % 4], light);   // party hat
         }
      }
      double f = (now - be.actionTick) / 7.0;
      if (f >= 0 && f <= 1) {                                                                       // the thrown ball
         double tx = be.lastAim / 100.0, ty = be.lastAim2 / 100.0;
         double x = lerp(0, tx, f), y = lerp(11, ty, f) + Math.sin(f * Math.PI) * 2.5, z = lerp(-7, TicketGame.CLOWN_Z - 0.6, f);
         ball(b, p, x, y, z, 0.7F, 0xFFF4F4F4, light);
      }
      if (be.playing) {
         float left = (float)Math.max(0, 1 - t / 600.0);
         Draw.box(b, p, -7F, 26.6F, 3.85F, -7F + 14F * left, 27.0F, 3.95F, left < 0.2F ? 0xFFFF3030 : 0xFF40FF70, BRIGHT);
      }
      Draw.digits(b, p, be.score, 2, 1.1F, 24.4F, 3.95F, 1.2F, 2.2F, 0xFFFF5A20, BRIGHT, true);
   }

   private static final int[] PRIZE_COLORS = {0xFFFF6AA8, 0xFF6AD0FF, 0xFFFFE06A, 0xFF9AFF6A, 0xFFC08AFF, 0xFFFF8A4A, 0xFFFF6AA8};

   private static void claw(VertexConsumer b, Pose p, TicketGameBlockEntity be, double now, int light) {
      double f = now - Math.floor(now);
      double cx = be.pClawX + (be.clawX - be.pClawX) * f, cz = be.pClawZ + (be.clawZ - be.pClawZ) * f, cy = 27.0;
      boolean open = true;
      if (be.stage == 3) {
         double t = now - be.actionTick;
         double tx = be.lastAim / 100.0, tz = be.lastAim2 / 100.0;
         if (t < 15) { cx = tx; cz = tz; cy = lerp(27, 14.5, t / 15); }
         else if (t < 19) { cx = tx; cz = tz; cy = 14.5; open = false; }
         else if (t < 30) { cx = tx; cz = tz; cy = lerp(14.5, 27, (t - 19) / 11); open = false; }
         else { double k = Math.min(1, (t - 30) / 8); cx = lerp(tx, -5, k); cz = lerp(tz, -5, k); open = k >= 1; }
         if (!open && be.lastResult >= 1 && t >= 19) {
            int c = PRIZE_COLORS[(be.lastResult - 1) % PRIZE_COLORS.length];
            ball(b, p, cx, cy - 1.9, cz, 1.2F, c, light);                     // the prize in the claw
         }
      }
      float x = (float)cx, z = (float)cz, y = (float)cy;
      // shadow spot on the prize pile so you can line up the drop
      if (be.stage == 1) {
         Draw.box(b, p, x - 0.5F, 14.05F, z - 0.5F, x + 0.5F, 14.15F, z + 0.5F, 0xFFFF3030, BRIGHT);
         float left = (float)Math.max(0, 1 - (now - be.startTick) / 400.0);
         Draw.box(b, p, -6F, 30.0F, -8.12F, -6F + 12F * left, 30.4F, -8.08F, left < 0.25F ? 0xFFFF3030 : 0xFF40FF70, BRIGHT);  // time bar
      }
      Draw.box(b, p, -7F, 29.0F, z - 0.4F, 7F, 29.6F, z + 0.4F, 0xFF8A8A92, light);      // gantry rail (moves front/back)
      Draw.box(b, p, x - 0.4F, 29.0F, -7F, x + 0.4F, 29.4F, 7F, 0xFF6A6A72, light);      // cross rail (moves left/right)
      Draw.box(b, p, x - 0.1F, y, z - 0.1F, x + 0.1F, 29.0F, z + 0.1F, 0xFF303034, light); // cable
      Draw.box(b, p, x - 0.9F, y, z - 0.9F, x + 0.9F, y + 1.2F, z + 0.9F, 0xFFC8C8D0, light);  // claw head
      float spread = open ? 1.3F : 0.5F;
      for (int i = 0; i < 3; i++) {
         double a = i * Math.PI * 2 / 3;
         float px = x + (float)Math.cos(a) * spread, pz = z + (float)Math.sin(a) * spread;
         float tipx = x + (float)Math.cos(a) * (spread - 0.5F), tipz = z + (float)Math.sin(a) * (spread - 0.5F);
         Draw.box(b, p, px - 0.15F, y - 1.2F, pz - 0.15F, px + 0.15F, y, pz + 0.15F, 0xFFB0B0B8, light);
         Draw.box(b, p, tipx - 0.15F, y - 2.0F, tipz - 0.15F, tipx + 0.15F, y - 1.2F, tipz + 0.15F, 0xFFB0B0B8, light);
      }
   }
}
