package com.dustin.wcore.block;

import com.dustin.wcore.ConsoleSettings;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ConsoleNoise {
   private ConsoleNoise() {
   }

   public static void emit(ServerLevel level, BlockPos source) {
      ConsoleSettings cfg = ConsoleSettings.get();
      if (cfg.mobsHear && cfg.volume > 0) {
         double loud = cfg.volume / 100.0;
         double radius = 6.0 + 26.0 * loud;
         Vec3 src = Vec3.atCenterOf(source);
         level.gameEvent(GameEvent.NOTE_BLOCK_PLAY, src, Context.of(level.getBlockState(source)));
         Player viewer = level.getNearestPlayer(src.x, src.y, src.z, radius + 8.0, false);
         Vec3 screen = screenOf(level, source, src);
         if (viewer != null) {
            clearView(level, screen, viewer.position());
         }

         for (Monster m : level.getEntitiesOfClass(Monster.class, new AABB(src, src).inflate(radius))) {
            if (m.getTarget() == null && !m.isNoAi()) {
               double d = m.position().distanceTo(src);
               double heard = radius - 5.0 * muffling(level, src, m.getEyePosition(), source);
               if (!(d >= heard)) {
                  Vec3 spot = watchSpot(screen, viewer == null ? null : viewer.position(), m);
                  if (m.position().distanceToSqr(spot) < 2.5) {
                     m.getNavigation().stop();
                     m.getLookControl().setLookAt(screen.x, screen.y, screen.z);
                  } else {
                     m.getNavigation().moveTo(spot.x, spot.y, spot.z, 0.9);
                  }
               }
            }
         }
      }
   }

   public static Vec3 screenOf(ServerLevel level, BlockPos source, Vec3 fallback) {
      TvBlock.Projection pr = TvBlock.projection(level, source, level.getBlockState(source));
      return pr != null ? Vec3.atLowerCornerOf(source).add(pr.centre()) : fallback;
   }

   private static void clearView(ServerLevel level, Vec3 screen, Vec3 viewer) {
      Vec3 line = new Vec3(viewer.x - screen.x, 0.0, viewer.z - screen.z);
      double len = line.length();
      if (!(len < 1.0)) {
         Vec3 dir = line.scale(1.0 / len);
         Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);
         AABB box = new AABB(screen, viewer).inflate(2.0, 2.0, 2.0);

         for (Mob m : level.getEntitiesOfClass(Mob.class, box)) {
            if (m.getTarget() == null && !m.isNoAi() && !m.isPassenger() && !m.isLeashed()) {
               Vec3 rel = m.position().subtract(screen.x, 0.0, screen.z);
               double along = rel.x * dir.x + rel.z * dir.z;
               double across = rel.x * perp.x + rel.z * perp.z;
               if (!(along < 0.3) && !(along > len + 0.5) && !(Math.abs(across) > 1.6)) {
                  double sideways = across >= 0.0 ? 3.0 : -3.0;
                  Vec3 to = new Vec3(screen.x, m.getY(), screen.z).add(dir.scale(along)).add(perp.scale(sideways));
                  m.getNavigation().moveTo(to.x, to.y, to.z, 1.0);
               }
            }
         }
      }
   }

   private static Vec3 watchSpot(Vec3 screen, Vec3 viewer, Monster m) {
      long h = m.getUUID().getLeastSignificantBits();
      double side = ((h & 255L) / 255.0 - 0.5) * 7.0;
      double back = 2.5 + (h >>> 8 & 255L) / 255.0 * 2.0;
      double dist;
      Vec3 dir;
      if (viewer != null) {
         dir = new Vec3(viewer.x - screen.x, 0.0, viewer.z - screen.z);
         dist = dir.length();
         if (dist < 0.5) {
            dir = new Vec3(1.0, 0.0, 0.0);
            dist = 1.0;
         }

         dir = dir.scale(1.0 / dist);
      } else {
         double a = (h >>> 16 & 65535L) / 65535.0 * Math.PI * 2.0;
         dir = new Vec3(Math.cos(a), 0.0, Math.sin(a));
         dist = 3.5;
      }

      Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);
      Vec3 base = viewer != null ? viewer : screen;
      return new Vec3(screen.x, base.y, screen.z).add(dir.scale(dist + back)).add(perp.scale(side));
   }

   public static double muffling(ServerLevel level, Vec3 from, Vec3 to, BlockPos source) {
      Vec3 step = to.subtract(from);
      double len = step.length();
      if (len < 0.01) {
         return 0.0;
      }

      step = step.scale(0.4 / len);
      Set<BlockPos> seen = new HashSet<>();
      double m = 0.0;
      Vec3 p = from;

      for (double t = 0.0; t < len && m < 20.0; p = p.add(step)) {
         BlockPos bp = BlockPos.containing(p);
         if (!bp.equals(source) && seen.add(bp)) {
            BlockState st = level.getBlockState(bp);
            if (st.is(BlockTags.WOOL)) {
               m += 3.0;
            } else if (st.is(BlockTags.WOOL_CARPETS)) {
               m += 0.5;
            } else if (st.isCollisionShapeFullBlock(level, bp)) {
               m++;
            }
         }

         t += 0.4;
      }

      return m;
   }
}
