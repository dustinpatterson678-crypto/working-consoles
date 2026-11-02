package com.dustin.arcadebuild.client;

import com.dustin.arcadebuild.GrandHall;
import com.dustin.arcadebuild.hall.HallParts;
import com.dustin.arcadebuild.hall.K;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

public final class HallSound {
   private HallSound() {
   }

   private static final Map<Long, H> M = new HashMap<>();

   public static void init() {
      ClientTickEvents.END_CLIENT_TICK.register(HallSound::tick);
   }

   private static void stopAll() {
      for (H h : M.values()) h.done = true;
      M.clear();
   }

   private static void tick(Minecraft mc) {
      if (mc.level == null || mc.player == null) {
         if (!M.isEmpty()) stopAll();
         return;
      }
      double y = mc.player.getY();
      if (mc.level.dimension() != GrandHall.LEVEL || y >= 50) {
         if (!M.isEmpty()) stopAll();
         return;
      }
      if (mc.player.tickCount % 10 != 0) return;
      int cy = y >= 28 ? 34 : y >= 10 ? 17 : 6;
      int px = mc.player.getBlockX(), pz = mc.player.getBlockZ();
      List<BlockPos> lit = new ArrayList<>();
      for (int dx = -10; dx <= 10; dx++) for (int dz = -10; dz <= 10; dz++) {
         BlockPos b = new BlockPos(px + dx, cy, pz + dz);
         BlockState s = mc.level.getBlockState(b);
         if (HallParts.lamp(s) && s.getValue(HallParts.LIT)) lit.add(b);
      }
      lit.sort((a, b) -> Double.compare(a.distToCenterSqr(mc.player.position()), b.distToCenterSqr(mc.player.position())));
      List<Long> keep = new ArrayList<>();
      for (int i = 0; i < Math.min(4, lit.size()); i++) keep.add(lit.get(i).asLong());
      for (Iterator<Map.Entry<Long, H>> it = M.entrySet().iterator(); it.hasNext(); ) {
         Map.Entry<Long, H> e = it.next();
         if (!keep.contains(e.getKey()) || e.getValue().isStopped() || !mc.getSoundManager().isActive(e.getValue())) {
            e.getValue().done = true;
            it.remove();
         }
      }
      for (Long k : keep) {
         if (M.containsKey(k)) continue;
         H h = new H(BlockPos.of(k));
         M.put(k, h);
         mc.getSoundManager().play(h);
      }
   }

   static final class H extends AbstractTickableSoundInstance {
      final BlockPos pos;
      boolean done;

      H(BlockPos pos) {
         super(HallParts.Q1, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
         this.pos = pos;
         this.x = pos.getX() + 0.5;
         this.y = pos.getY() + 0.2;
         this.z = pos.getZ() + 0.5;
         this.looping = true;
         this.delay = 0;
         this.volume = 0.8F;
         this.pitch = 0.98F + (pos.hashCode() & 3) * 0.01F;
         this.attenuation = SoundInstance.Attenuation.LINEAR;
      }

      @Override
      public void tick() {
         Minecraft mc = Minecraft.getInstance();
         if (this.done || mc.level == null) {
            this.stop();
            return;
         }
         BlockState s = mc.level.getBlockState(this.pos);
         if (!HallParts.lamp(s) || !s.getValue(HallParts.LIT)) this.stop();
      }
   }
}
