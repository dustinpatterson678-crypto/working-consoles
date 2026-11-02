package com.dustin.arcadebuild.hall;

import com.dustin.arcadebuild.ArcadeBuildMod;
import com.dustin.arcadebuild.BreakerBlock;
import com.dustin.arcadebuild.GrandHall;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

public final class HallWiring {
   private HallWiring() {
   }

   static final int N = 8, O = 50, Q = 46;
   static final int[] F = {30, 12, 2};
   static final int[] G = {3, 4, 3};
   static final double[] H = {0.45, 0.6, 0.72};
   static final int[] I = {70, 110, 150};
   static final int J = 40;
   static final int[][] L0 = {{2, 2}, {2, 6}, {6, 2}, {6, 6}}, L1 = {{4, 4}};
   private static final String M = K.d("OxG2xe0=");
   private static final double[] W = K.v(K.d("d0m2g+XYIwZ+SqeB+9ghHGJdopfv0TQKclSqrZXrzzAEZFypjQ=="));

   static int v8(int l) {
      return F[l] + G[l] + 1;
   }

   static int v7(double y) {
      return y >= 28 ? 0 : y >= 10 ? 1 : 2;
   }

   private static long seed = 0x5EB1CE11L;
   private static int OX = 2_100_000, OZ = -1_700_000;

   static long mix(long z) {
      z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
      z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
      return z ^ (z >>> 31);
   }

   static long h(int l, int cx, int cz, int salt) {
      return mix(seed ^ mix(cx * 73856093L ^ cz * 19349663L * 31 ^ l * 83492791L ^ salt * 0x9E3779B97F4A7C15L));
   }

   static double r(int l, int cx, int cz, int salt) {
      return (h(l, cx, cz, salt) >>> 11) * 0x1.0p-53;
   }

   static boolean u1(int l, int cx, int cz, int side) {
      return r(l, cx, cz, 10 + side) < H[l];
   }

   static int u2(int l, int cx, int cz, int side) {
      return 1 + (int)(h(l, cx, cz, 20 + side) >>> 62);
   }

   static int u3(int l, int cx, int cz, int side) {
      return l == 0 ? 2 : 1 + (int)(h(l, cx, cz, 30 + side) >>> 63);
   }

   static boolean u4(int l, int cx, int cz, int side) {
      return l == 0 && r(l, cx, cz, 40 + side) < 0.2;
   }

   static boolean u5(int l, int cx, int cz, int side, int k) {
      if (u4(l, cx, cz, side)) return false;
      int g = u2(l, cx, cz, side);
      return k >= g && k < g + u3(l, cx, cz, side);
   }

   static boolean u6(int l, int cx, int cz) {
      return u1(l, cx, cz, 0) && Long.remainderUnsigned(h(l, cx, cz, 50), I[l]) == 0;
   }

   static boolean u7(int l, int cx, int cz) {
      return Long.remainderUnsigned(h(l, cx, cz, 51), 100) < J;
   }

   static int[][] u8(int l) {
      return l == 0 ? L0 : L1;
   }

   static boolean u9(int l, int cx, int cz, int i) {
      if (l == 0 && r(l, cx, cz, 60) < 0.06) return false;
      return r(l, cx, cz, 61 + i) < (l == 0 ? 0.85 : l == 1 ? 0.65 : 0.4);
   }

   static double v1(int l, int cx, int cz) {
      if (l != 0) return 0;
      int best = 9;
      for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
         if (u6(l, cx + dx, cz + dz) && !u7(l, cx + dx, cz + dz)) best = Math.min(best, Math.max(Math.abs(dx), Math.abs(dz)));
      }
      return best == 0 ? 0.35 : best == 1 ? 0.22 : best == 2 ? 0.1 : best == 3 ? 0.04 : 0;
   }

   static BlockState v2(int l, int x, int y, int z, double mark) {
      return v3(l, y, mark, r(9, x, z, 80 + y));
   }

   static BlockState v3(int l, int y, double mark, double v) {
      if (l == 1) return HallParts.P_F.defaultBlockState();
      if (l == 2) return HallParts.P_G.defaultBlockState();
      if (y == F[0] + 1) return HallParts.P_D.defaultBlockState();
      if (y == v8(0) - 1) return (v < 0.12 ? HallParts.P_E : v < 0.2 ? HallParts.P_B : HallParts.P_A).defaultBlockState();
      return (v < mark ? HallParts.P_C : v < mark + 0.1 ? HallParts.P_B : HallParts.P_A).defaultBlockState();
   }

   static BlockState v4(int l, int x, int z) {
      if (l == 1) return HallParts.F_C.defaultBlockState();
      if (l == 2) return HallParts.F_D.defaultBlockState();
      boolean damp = r(9, x >> 2, z >> 2, 70) < 0.3 && r(9, x, z, 71) < 0.85;
      return (damp ? HallParts.F_B : HallParts.F_A).defaultBlockState();
   }

   static BlockState v5(int l) {
      return (l == 0 ? HallParts.C_A : HallParts.P_F).defaultBlockState();
   }

   static BlockState v6(int l) {
      return (l == 0 ? HallParts.L_A : HallParts.L_B).defaultBlockState();
   }

   private static final Set<Long> U1 = new HashSet<>();

   private static long key(int l, int cx, int cz) {
      return (long)l << 60 ^ ((long)cx & 0x3FFFFFFFL) << 30 ^ (long)cz & 0x3FFFFFFFL;
   }

   private static void set(ServerLevel lv, int x, int y, int z, BlockState s) {
      lv.setBlock(new BlockPos(x, y, z), s, 18);
   }

   static boolean a7(ServerLevel lv, int l, int cx, int cz) {
      long k = key(l, cx, cz);
      if (U1.contains(k)) return false;
      U1.add(k);
      if (!lv.getBlockState(new BlockPos(cx * N + 1, F[l], cz * N + 1)).isAir()) return false;
      a8(lv, l, cx, cz);
      return true;
   }

   private static void a8(ServerLevel lv, int l, int cx, int cz) {
      int bx = cx * N, bz = cz * N, fy = F[l], cy = v8(l);
      double mark = v1(l, cx, cz);
      BlockState ceiling = v5(l);
      for (int x = 0; x < N; x++) for (int z = 0; z < N; z++) {
         set(lv, bx + x, fy, bz + z, v4(l, bx + x, bz + z));
         set(lv, bx + x, cy, bz + z, ceiling);
      }
      int[][] ls = u8(l);
      for (int i = 0; i < ls.length; i++) if (u9(l, cx, cz, i)) set(lv, bx + ls[i][0], cy, bz + ls[i][1], v6(l));
      for (int side = 0; side < 2; side++) {
         if (!u1(l, cx, cz, side)) continue;
         for (int k = 0; k < N; k++) {
            if (u5(l, cx, cz, side, k)) continue;
            int x = side == 0 ? bx : bx + k, z = side == 0 ? bz + k : bz;
            for (int y = fy + 1; y < cy; y++) set(lv, x, y, z, v2(l, x, y, z, mark));
         }
      }
      for (int y = fy + 1; y < cy; y++) set(lv, bx, y, bz, v2(l, bx, y, bz, mark));
      if (l == 0 && r(l, cx, cz, 90) < 0.25) {
         boolean ns = r(l, cx, cz, 91) < 0.5;
         for (int k = 2; k <= 4; k++) for (int y = fy + 1; y < cy; y++) {
            int x = ns ? bx + 4 : bx + k, z = ns ? bz + k : bz + 4;
            set(lv, x, y, z, v2(l, x, y, z, mark));
         }
      }
      if (u6(l, cx, cz)) {
         set(lv, bx, fy + 1, bz + 6, HallParts.D_A.defaultBlockState().setValue(HallParts.FACING, Direction.EAST).setValue(HallParts.HALF, DoubleBlockHalf.LOWER));
         set(lv, bx, fy + 2, bz + 6, HallParts.D_A.defaultBlockState().setValue(HallParts.FACING, Direction.EAST).setValue(HallParts.HALF, DoubleBlockHalf.UPPER));
         set(lv, bx, fy + 3, bz + 6, HallParts.S_A.defaultBlockState().setValue(HallParts.FACING, Direction.EAST));
      }
   }

   static void a9(ServerLevel lv, int l, double px, double pz, int radius, int budget) {
      int pcx = Math.floorDiv(Mth.floor(px), N), pcz = Math.floorDiv(Mth.floor(pz), N);
      for (int rr = 0; rr <= radius && budget > 0; rr++) {
         for (int dx = -rr; dx <= rr && budget > 0; dx++) for (int dz = -rr; dz <= rr && budget > 0; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != rr) continue;
            if (a7(lv, l, pcx + dx, pcz + dz)) budget--;
         }
      }
   }

   private static void a6(ServerLevel lv, int l, int x, int z, int top, boolean cap, List<Object[]> f8) {
      a9(lv, l, x, z, 1, 99);
      int fy = F[l], cy = v8(l);
      BlockState black = HallParts.T_B.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
      for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
         for (int y = fy + 1; y < cy; y++) set(lv, x + dx, y, z + dz, air);
         BlockPos c = new BlockPos(x + dx, cy, z + dz);
         f8.add(new Object[]{c, lv.getBlockState(c).getBlock() instanceof HallParts.Fx ? v6(l) : v5(l)});
         set(lv, x + dx, cy, z + dz, air);
      }
      for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
         boolean ring = Math.max(Math.abs(dx), Math.abs(dz)) == 2;
         for (int y = cy + 1; y <= top; y++) if (ring || cap && y == top) set(lv, x + dx, y, z + dz, black);
      }
   }

   static final class S {
      boolean f1, f2, f3;
      int f4;
      long f5, f6, f7;
      final List<Object[]> f8 = new ArrayList<>();
   }

   private static final Map<UUID, S> U2 = new HashMap<>();
   private static final Map<UUID, Double> U3 = new HashMap<>();
   private static final Set<Long> U4 = new HashSet<>();
   private static final List<Object[]> U5 = new ArrayList<>();

   private static void later(long when, Runnable r) {
      U5.add(new Object[]{when, r});
   }

   private static S state(ServerPlayer p, long now) {
      return U2.computeIfAbsent(p.getUUID(), u -> {
         S s = new S();
         s.f5 = now + 600;
         s.f6 = now + 300;
         s.f7 = now + 2400;
         return s;
      });
   }

   public static void init() {
      ServerLifecycleEvents.SERVER_STARTED.register(server -> {
         seed = server.overworld().getSeed() ^ 0x5EB1CE11L;
         long g = mix(seed ^ 0x7A11C0DEL);
         OX = ((int)(1_400_000 + Math.floorMod(g, 1_600_000)) & ~7) * ((g >>> 40 & 1) == 0 ? 1 : -1);
         OZ = ((int)(1_400_000 + Math.floorMod(g >>> 20, 1_600_000)) & ~7) * ((g >>> 41 & 1) == 0 ? 1 : -1);
         U1.clear();
      });
      ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
         U1.clear();
         U2.clear();
         U3.clear();
         U5.clear();
         U4.clear();
      });
      ServerTickEvents.END_SERVER_TICK.register(HallWiring::tick);
   }

   private static void tick(MinecraftServer server) {
      ServerLevel lv = server.getLevel(GrandHall.LEVEL);
      if (lv == null) return;
      long now = lv.getGameTime();
      if (!U5.isEmpty()) {
         List<Runnable> due = new ArrayList<>();
         for (Iterator<Object[]> it = U5.iterator(); it.hasNext(); ) {
            Object[] t = it.next();
            if ((long)t[0] <= now) {
               due.add((Runnable)t[1]);
               it.remove();
            }
         }
         for (Runnable r : due) r.run();
      }
      for (ServerPlayer p : new ArrayList<>(lv.players())) {
         double y = p.getY();
         Double last = U3.put(p.getUUID(), y);
         if (y >= O) {
            U2.remove(p.getUUID());
            if (last != null && a1(lv, p, y - last)) a2(lv, p);
            continue;
         }
         if (!marked(p)) continue;
         a3(lv, p, now);
      }
   }

   private static boolean a1(ServerLevel lv, ServerPlayer p, double dy) {
      if (dy > W[0]) return false;
      double y = p.getY();
      if (y < W[1] || y > W[2]) return false;
      int tx = Mth.floor(p.getX()), tz = Mth.floor(p.getZ());
      if (Math.abs(p.getX() - (tx + 0.5)) > W[3] || Math.abs(p.getZ() - (tz + 0.5)) > W[3]) return false;
      int ox = tx + (int)W[4], oz = tz - (int)W[5];
      if (Math.floorMod(ox, (int)W[6]) != 0 || Math.floorMod(oz, (int)W[6]) != 0) return false;
      BlockState b = lv.getBlockState(new BlockPos(ox - (int)W[7], (int)W[8], oz + (int)W[9]));
      if (b.getBlock() != ArcadeBuildMod.BREAKER || b.getValue(BreakerBlock.POWERED)) return false;
      for (String t : new ArrayList<>(p.entityTags())) if (t.startsWith(M)) p.removeTag(t);
      p.addTag(M + ox + "," + oz);
      return true;
   }

   private static void a2(ServerLevel lv, ServerPlayer p) {
      long now = lv.getGameTime();
      S s = state(p, now);
      int x = Mth.floor(p.getX()) + OX, z = Mth.floor(p.getZ()) + OZ;
      a6(lv, 0, x, z, Q + 3, true, s.f8);
      s.f1 = true;
      s.f4 = 0;
      p.teleportTo(lv, p.getX() + OX, Q, p.getZ() + OZ, Relative.union(Relative.DELTA, Relative.ROTATION), 0.0F, 0.0F, false);
   }

   private static boolean marked(ServerPlayer p) {
      for (String t : p.entityTags()) if (t.startsWith(M)) return true;
      return false;
   }

   private static void a3(ServerLevel lv, ServerPlayer p, long now) {
      S s = state(p, now);
      int l = v7(p.getY());
      a9(lv, l, p.getX(), p.getZ(), 3, 6);
      if (p.getY() < -2) {
         a5(lv, p);
         return;
      }
      if (s.f1) {
         p.resetFallDistance();
         if (++s.f4 > 3 && p.onGround()) {
            s.f1 = false;
            lv.playSound(null, p.blockPosition(), HallParts.Q6, SoundSource.PLAYERS, 1.0F, 0.9F + lv.getRandom().nextFloat() * 0.15F);
            lv.playSound(null, p.blockPosition(), SoundEvents.MUD_STEP, SoundSource.PLAYERS, 0.8F, 0.7F);
            List<Object[]> back = new ArrayList<>(s.f8);
            s.f8.clear();
            later(now + 60, () -> {
               for (Object[] e : back) if (lv.getBlockState((BlockPos)e[0]).isAir()) lv.setBlock((BlockPos)e[0], (BlockState)e[1], 3);
            });
         }
         if (s.f4 > 400) s.f1 = false;
      }
      w1(lv, p, s, l, now);
   }

   static void a4(ServerLevel lv, BlockPos pos, ServerPlayer p) {
      int l = v7(pos.getY()), cx = Math.floorDiv(pos.getX(), N), cz = Math.floorDiv(pos.getZ(), N);
      S s = state(p, lv.getGameTime());
      if (s.f1) return;
      lv.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
      a5(lv, p);
      if (true) return;
      if (!u7(l, cx, cz)) {
         a5(lv, p);
         return;
      }
      lv.playSound(null, p.blockPosition(), HallParts.Q7, SoundSource.BLOCKS, 1.0F, 0.9F);
      int x = Mth.floor(p.getX()), z = Mth.floor(p.getZ()), fy = F[l];
      if (l < 2) a6(lv, l + 1, x, z, fy - 1, true, s.f8);
      s.f1 = true;
      s.f4 = 0;
      p.teleportTo(lv, p.getX(), fy - 3.0, p.getZ(), Relative.union(Relative.DELTA, Relative.ROTATION), 0.0F, 0.0F, false);
   }

   private static void a5(ServerLevel lv, ServerPlayer p) {
      U2.remove(p.getUUID());
      for (String t : new ArrayList<>(p.entityTags())) {
         if (!t.startsWith(M)) continue;
         String[] v = t.substring(M.length()).split(",");
         try {
            int ox = Integer.parseInt(v[0]), oz = Integer.parseInt(v[1]);
            BlockPos core = GrandHall.core(new BlockPos(ox, 64, oz));
            if (lv.getBlockState(core).getBlock() == ArcadeBuildMod.HALL_CORE) {
               p.removeTag(t);
               p.teleportTo(lv, ox - 16 + 0.5, 65.0, oz + 61 + 0.5, Set.of(), p.getYRot(), p.getXRot(), false);
               p.setDeltaMovement(Vec3.ZERO);
               return;
            }
         } catch (RuntimeException ignored) {
         }
      }
      ServerLevel home = lv.getServer().overworld();
      BlockPos sp = home.getRespawnData().pos();
      p.teleportTo(home, sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
   }

   private static BlockPos w4(ServerLevel lv, int l, double px, double pz, RandomSource rnd, int cells) {
      int cx = Math.floorDiv(Mth.floor(px), N) + rnd.nextInt(cells * 2 + 1) - cells, cz = Math.floorDiv(Mth.floor(pz), N) + rnd.nextInt(cells * 2 + 1) - cells;
      int[] at = u8(l)[rnd.nextInt(u8(l).length)];
      BlockPos b = new BlockPos(cx * N + at[0], v8(l), cz * N + at[1]);
      BlockState s = lv.getBlockState(b);
      return HallParts.lamp(s) && s.getValue(HallParts.LIT) ? b : null;
   }

   private static void w5(ServerLevel lv, BlockPos b, boolean on) {
      BlockState s = lv.getBlockState(b);
      if (s.getBlock() instanceof HallParts.Fx && s.getValue(HallParts.LIT) != on) lv.setBlock(b, s.setValue(HallParts.LIT, on), 2);
   }

   private static void w1(ServerLevel lv, ServerPlayer p, S s, int l, long now) {
      RandomSource rnd = lv.getRandom();
      double px = p.getX(), pz = p.getZ();
      if (!s.f3 && rnd.nextInt(9) == 0) {
         BlockPos b = w4(lv, l, px, pz, rnd, 1);
         if (b != null) {
            w5(lv, b, false);
            lv.playSound(null, b, HallParts.Q2, SoundSource.BLOCKS, 0.45F, 0.9F + rnd.nextFloat() * 0.2F);
            later(now + 2 + rnd.nextInt(6), () -> {
               if (!U4.contains(b.asLong())) w5(lv, b, true);
            });
         }
      }
      if (!s.f3 && rnd.nextInt(2400) == 0) {
         BlockPos b = w4(lv, l, px, pz, rnd, 1);
         if (b != null) {
            U4.add(b.asLong());
            w5(lv, b, false);
            lv.playSound(null, b, HallParts.Q3, SoundSource.BLOCKS, 0.9F, 1.0F);
         }
      }
      if (now >= s.f7) {
         s.f7 = now + 3600 + rnd.nextInt(4800);
         List<BlockPos> off = new ArrayList<>();
         int pcx = Math.floorDiv(Mth.floor(px), N), pcz = Math.floorDiv(Mth.floor(pz), N);
         for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int[] at : u8(l)) {
            BlockPos b = new BlockPos((pcx + dx) * N + at[0], v8(l), (pcz + dz) * N + at[1]);
            BlockState st = lv.getBlockState(b);
            if (HallParts.lamp(st) && st.getValue(HallParts.LIT)) off.add(b);
         }
         if (!off.isEmpty()) {
            s.f3 = true;
            for (BlockPos b : off) w5(lv, b, false);
            lv.playSound(null, off.get(0), HallParts.Q4, SoundSource.BLOCKS, 1.4F, 0.95F);
            long back = now + 80 + rnd.nextInt(160);
            later(back, () -> {
               s.f3 = false;
               lv.playSound(null, off.get(0), HallParts.Q5, SoundSource.BLOCKS, 1.2F, 1.0F);
            });
            for (BlockPos b : off) later(back + rnd.nextInt(40), () -> {
               if (!U4.contains(b.asLong())) w5(lv, b, true);
            });
         }
      }
      if (now >= s.f5) {
         s.f5 = now + 500 + rnd.nextInt(1100);
         w2(lv, p, l, rnd, now);
      }
      if (now >= s.f6) {
         s.f6 = now + 160 + rnd.nextInt(500);
         w3(lv, p, l, rnd);
      }
      if (now % 5 == 0) {
         int pcx = Math.floorDiv(Mth.floor(px), N), pcz = Math.floorDiv(Mth.floor(pz), N);
         for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            int cx = pcx + dx, cz = pcz + dz;
            if (!u6(l, cx, cz)) continue;
            BlockPos sign = new BlockPos(cx * N, F[l] + 3, cz * N + 6);
            if (!(lv.getBlockState(sign).getBlock() instanceof HallParts.Fs)) continue;
            if (u7(l, cx, cz)) {
               if (rnd.nextInt(4) == 0) {
                  w5(lv, sign, false);
                  later(now + 1 + rnd.nextInt(3), () -> w5(lv, sign, true));
               }
            } else if (now % 80 == 0) {
               lv.playSound(null, sign.below(2).relative(Direction.EAST), HallParts.Q8, SoundSource.AMBIENT, 0.7F, 0.9F + rnd.nextFloat() * 0.2F);
            }
         }
      }
   }

   private static void w2(ServerLevel lv, ServerPlayer p, int l, RandomSource rnd, long now) {
      Vec3 look = p.getLookAngle();
      double a = Math.atan2(look.z, look.x) + Math.PI + (rnd.nextDouble() - 0.5) * Math.PI;
      double d = 10 + rnd.nextDouble() * 7;
      double sx = p.getX() + Math.cos(a) * d, sz = p.getZ() + Math.sin(a) * d, y = F[l] + 1;
      boolean toward = rnd.nextInt(10) < 3;
      double wa = toward ? a + Math.PI : a + Math.PI / 2 * (rnd.nextBoolean() ? 1 : -1);
      double wx = Math.cos(wa) * 0.8, wz = Math.sin(wa) * 0.8;
      int n = toward ? 3 + rnd.nextInt(3) : 4 + rnd.nextInt(6), pace = 7 + rnd.nextInt(3);
      SoundEvent ev = l == 0 ? SoundEvents.MUD_STEP : l == 1 ? SoundEvents.STONE_STEP : SoundEvents.METAL_STEP;
      float pitch = 0.72F + rnd.nextFloat() * 0.1F;
      for (int i = 0; i < n; i++) {
         final int k = i;
         later(now + (long)i * pace, () -> lv.playSound(null, sx + wx * k, y, sz + wz * k, ev, SoundSource.AMBIENT, 0.9F, pitch));
      }
   }

   private static void w3(ServerLevel lv, ServerPlayer p, int l, RandomSource rnd) {
      Vec3 look = p.getLookAngle();
      double lx = look.x, lz = look.z, ll = Math.sqrt(lx * lx + lz * lz);
      if (ll < 1.0E-3) return;
      lx /= ll;
      lz /= ll;
      int pcx = Math.floorDiv(Mth.floor(p.getX()), N), pcz = Math.floorDiv(Mth.floor(p.getZ()), N);
      for (int tries = 0; tries < 8; tries++) {
         int cx = pcx + rnd.nextInt(5) - 2, cz = pcz + rnd.nextInt(5) - 2, side = rnd.nextInt(2);
         if (!u1(l, cx, cz, side)) continue;
         int k = 1 + rnd.nextInt(N - 1);
         if (u5(l, cx, cz, side, k) || side == 0 && k == 6 && u6(l, cx, cz)) continue;
         int x = side == 0 ? cx * N : cx * N + k, z = side == 0 ? cz * N + k : cz * N;
         double vx = x + 0.5 - p.getX(), vz = z + 0.5 - p.getZ(), d = Math.sqrt(vx * vx + vz * vz);
         if (d < 6 || d > 20 || (vx * lx + vz * lz) / d > -0.3) continue;
         int fy = F[l], cy = v8(l);
         if (l == 0 && rnd.nextInt(3) != 0) {
            double mark = v1(l, cx, cz);
            for (int y = fy + 1; y < cy; y++) {
               if (lv.getBlockState(new BlockPos(x, y, z)).isAir()) break;
               set(lv, x, y, z, v3(l, y, mark, rnd.nextDouble() * 0.6));
            }
         } else {
            boolean open = !lv.getBlockState(new BlockPos(x, fy + 1, z)).isAir();
            for (int y = fy + 1; y < cy; y++) set(lv, x, y, z, open ? Blocks.AIR.defaultBlockState() : v2(l, x, y, z, v1(l, cx, cz)));
         }
         return;
      }
   }
}
