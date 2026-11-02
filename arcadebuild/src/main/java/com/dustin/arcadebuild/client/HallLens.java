package com.dustin.arcadebuild.client;

import com.dustin.arcadebuild.ArcadeBuildMod;
import com.dustin.arcadebuild.GrandHall;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import com.dustin.arcadebuild.hall.K;

public final class HallLens {
   private HallLens() {
   }

   private static final Identifier V = ArcadeBuildMod.id(K.d("Lhzgw6KGcEF/HvversFnUBEbotOthw=="));
   private static final Identifier[] GR = {ArcadeBuildMod.id(K.d("Lhzgw6KGcEF/HvversFnUBEKvI2zjmY=")), ArcadeBuildMod.id(K.d("Lhzgw6KGcEF/HvversFnUBEKvY2zjmY=")),
      ArcadeBuildMod.id(K.d("Lhzgw6KGcEF/HvversFnUBEKvo2zjmY=")), ArcadeBuildMod.id(K.d("Lhzgw6KGcEF/HvversFnUBEKv42zjmY="))};
   private static final RandomSource RND = RandomSource.create();
   private static float h5;
   private static boolean h1, h2;

   private static boolean h3(Minecraft mc) {
      if (mc.level.dimension() != GrandHall.LEVEL || mc.player.getY() < 50) return false;
      net.minecraft.core.BlockPos p = mc.player.blockPosition();
      for (net.minecraft.core.BlockPos b : net.minecraft.core.BlockPos.betweenClosed(p.offset(-2, -1, -2), p.offset(2, 1, 2))) {
         net.minecraft.world.level.block.state.BlockState st = mc.level.getBlockState(b);
         if (st.hasBlockEntity() && "arcadeplus".equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getNamespace())) return true;
      }
      return false;
   }

   static boolean h4(Minecraft mc) {
      return mc.level != null && mc.player != null && mc.level.dimension() == GrandHall.LEVEL && mc.player.getY() < 50
         && Math.max(Math.abs(mc.player.getX()), Math.abs(mc.player.getZ())) > 500_000;
   }

   public static void init() {
      ClientTickEvents.END_CLIENT_TICK.register(mc -> {
         boolean in = h4(mc);
         h5 = Math.max(0.0F, Math.min(1.0F, h5 + (in ? 0.05F : -0.1F)));
         boolean hide = mc.level != null && in;
         if (hide && !h1) {
            if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            h1 = true;
         } else if (!hide && h1) {
            if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            h1 = false;
         }
         if (!in || mc.isPaused()) return;
         double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
         double floor = py >= 28 ? 31 : py >= 10 ? 13 : 3, v8 = py >= 28 ? 34 : py >= 10 ? 17 : 6;
         for (int i = 0; i < 2; i++) {
            double x = px + (RND.nextDouble() - 0.5) * 12, z = pz + (RND.nextDouble() - 0.5) * 12, y = floor + 0.2 + RND.nextDouble() * (v8 - floor - 0.4);
            mc.level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0.0, 0.0, 0.0);
         }
      });
      HudElementRegistry.addFirst(ArcadeBuildMod.id(K.d("Mhj024iYcFwj")), (g, delta) -> {
         if (h5 <= 0.0F) return;
         int w = g.guiWidth(), h = g.guiHeight();
         int a = (int)(0x16 * h5);
         g.fill(0, 0, w, h, a << 24 | 0xF0E89C);
         int va = (int)(255 * h5);
         g.blit(RenderPipelines.GUI_TEXTURED, V, 0, 0, 0.0F, 0.0F, w, h, 256, 256, 256, 256, va << 24 | 0xFFFFFF);
         Minecraft mc = Minecraft.getInstance();
         Identifier grain = GR[(int)((mc.level == null ? 0 : mc.level.getGameTime()) % 4)];
         int ox = RND.nextInt(128), oy = RND.nextInt(128);
         for (int x = -ox; x < w; x += 128) for (int y = -oy; y < h; y += 128) {
            g.blit(RenderPipelines.GUI_TEXTURED, grain, x, y, 0.0F, 0.0F, 128, 128, 128, 128, va << 24 | 0xFFFFFF);
         }
      });
   }
}
