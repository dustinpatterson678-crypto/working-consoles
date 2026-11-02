package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.PinballBlockEntity;
import com.dustin.arcadeplus.TicketGame;
import com.dustin.arcadeplus.TicketGameBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Arcade atmosphere, all client side: idle machines play little attract jingles now and then, and
 * running machines tick, blip and whirr in step with their lights (cyclone chaser, stacker rows,
 * the big wheel's pegs, the claw motor, gators popping up).
 */
public final class ArcadeSounds {
   private record Note(long at, BlockPos pos, SoundEvent sound, float vol, float pitch) {}

   private static final PriorityQueue<Note> QUEUE = new PriorityQueue<>((a, b) -> Long.compare(a.at, b.at));
   private static final List<BlockPos> NEAR = new ArrayList<>();
   private static final Map<BlockPos, Long> NEXT_JINGLE = new HashMap<>();
   private static final Map<BlockPos, Integer> LAST = new HashMap<>();
   private static final Random R = new Random();
   private static long tick;
   // a few short arcade tunes, in semitones from F#3 (note-block pitch 0.5 .. 2.0 = 2 octaves)
   private static final int[][] TUNES = {
      {12, 16, 19, 24, 19, 24},
      {7, 12, 16, 12, 19, 24},
      {12, 14, 16, 19, 16, 19, 24},
      {19, 17, 16, 14, 12, 7, 12},
      {12, 12, 19, 19, 21, 21, 19},
   };

   private ArcadeSounds() {
   }

   public static void init() {
      ClientTickEvents.END_CLIENT_TICK.register(ArcadeSounds::tick);
   }

   private static float pitch(int semis) {
      return (float)Math.pow(2.0, (semis - 12) / 12.0);
   }

   private static void play(BlockPos pos, SoundEvent s, float vol, float pitch) {
      QUEUE.add(new Note(tick, pos, s, vol, pitch));
   }

   private static void jingle(BlockPos pos, Holder<SoundEvent> inst) {
      int[] t = TUNES[R.nextInt(TUNES.length)];
      for (int i = 0; i < t.length; i++) {
         QUEUE.add(new Note(tick + i * 3L, pos, inst.value(), 0.35F, pitch(t[i])));
      }
   }

   private static void tick(Minecraft mc) {
      if (mc.level == null || mc.player == null || mc.isPaused()) return;
      tick++;
      if (tick % 20 == 0) {
         NEAR.clear();
         BlockPos base = mc.player.blockPosition();
         for (BlockPos q : BlockPos.betweenClosed(base.offset(-16, -4, -16), base.offset(16, 4, 16))) {
            BlockEntity be = mc.level.getBlockEntity(q);
            if (be instanceof TicketGameBlockEntity || be instanceof PinballBlockEntity
               || be instanceof com.dustin.wcore.retro.RetroConsoleBlockEntity && mc.level.getBlockState(q).getBlock() instanceof com.dustin.wcore.retro.TallCabinetBlock) {
               NEAR.add(q.immutable());
            }
         }
      }
      long now = mc.level.getGameTime();
      for (BlockPos pos : NEAR) {
         BlockEntity raw = mc.level.getBlockEntity(pos);
         if (raw instanceof com.dustin.wcore.retro.RetroConsoleBlockEntity cab) {
            if (cab.isPowered() && !cab.isOn()) idleJingle(pos, SoundEvents.NOTE_BLOCK_BIT);   // attract mode bleeps
            continue;
         }
         if (raw instanceof PinballBlockEntity pb) {
            if (pb.isPowered() && !pb.playing) idleJingle(pos, SoundEvents.NOTE_BLOCK_BELL);
            continue;
         }
         if (!(raw instanceof TicketGameBlockEntity be) || !be.isPowered()) continue;
         TicketGame g = be.game();
         boolean busy = be.playing || be.stage != 0;
         if (!busy) {
            idleJingle(pos, g == TicketGame.CYCLONE || g == TicketGame.BIGWHEEL ? SoundEvents.NOTE_BLOCK_CHIME : SoundEvents.NOTE_BLOCK_BIT);
            continue;
         }
         switch (g) {
            case CYCLONE -> step(pos, TicketGame.cycLamp(now - be.startTick), SoundEvents.NOTE_BLOCK_HAT.value(), 0.25F, 1.9F);
            case STACKER -> {
               int row = be.shots, w = Math.min(row == 0 ? 3 : be.stackRows[row - 1] >> 4, TicketGame.stackMaxWidth(row));
               step(pos, TicketGame.stackPos(now - be.startTick, row, w), SoundEvents.NOTE_BLOCK_BIT.value(), 0.25F, 1.2F + row * 0.06F);
            }
            case BIGWHEEL -> {
               double a = be.stage == 1 ? TicketGame.wheelSpin(now - be.startTick) : TicketGame.wheelCoast(be.lastAim / 10000.0, now - be.actionTick);
               step(pos, TicketGame.wheelSegment(a), SoundEvents.WOODEN_BUTTON_CLICK_ON, 0.35F, 1.6F);
            }
            case CLAWMACHINE -> {
               int moved = Math.round(be.clawX * 10) * 1000 + Math.round(be.clawZ * 10);
               if (be.stage == 3 || LAST.getOrDefault(pos, moved) != moved) {
                  if (tick % 4 == 0) play(pos, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 0.12F, 0.5F);
               }
               LAST.put(pos, moved);
            }
            case DRUMWHEEL -> {
               if (be.stage == 3) {
                  double a = TicketGame.drumAngle(be.lastAim / 10000.0, be.lastResult, now - be.actionTick);
                  step(pos, TicketGame.drumSegment(a), SoundEvents.WOODEN_BUTTON_CLICK_ON, 0.4F, 1.3F);
               }
            }
            case GATORS, WHACKAMOLE -> {
               int prev = LAST.getOrDefault(pos, 0);
               if ((be.moleMask & ~prev) != 0) play(pos, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.4F, 1.4F);
               LAST.put(pos, be.moleMask);
            }
            case ECHO -> {
               if (be.stage == 1) {                      // the machine "sings" its pattern
                  double t = now - be.actionTick;
                  int stepI = (int)Math.floor(t / TicketGame.ECHO_STEP);
                  if (t >= 0 && stepI <= be.score && t - stepI * TicketGame.ECHO_STEP < 1) {
                     int c = TicketGame.echoColor(be.lastAim, stepI);
                     play(pos, SoundEvents.NOTE_BLOCK_PLING.value(), 0.5F, new float[]{0.7F, 0.9F, 1.1F, 1.3F}[c]);
                  }
               }
            }
            default -> {
            }
         }
      }
      while (!QUEUE.isEmpty() && QUEUE.peek().at <= tick) {
         Note n = QUEUE.poll();
         mc.level.playLocalSound(n.pos.getX() + 0.5, n.pos.getY() + 1.0, n.pos.getZ() + 0.5, n.sound, SoundSource.BLOCKS, n.vol, n.pitch, false);
      }
   }

   private static void step(BlockPos pos, int value, SoundEvent s, float vol, float pitch) {
      Integer prev = LAST.put(pos, value);
      if (prev != null && prev != value) play(pos, s, vol, pitch);
   }

   private static void idleJingle(BlockPos pos, Holder<SoundEvent> inst) {
      long next = NEXT_JINGLE.computeIfAbsent(pos, p -> tick + 20 + R.nextInt(200));
      if (tick >= next) {
         jingle(pos, inst);
         NEXT_JINGLE.put(pos, tick + 120 + R.nextInt(220));    // every 6-17 seconds per machine: arcades never go quiet
      } else if (R.nextInt(45) == 0) {
         // little bleeps and bloops between tunes
         int[] t = TUNES[R.nextInt(TUNES.length)];
         play(pos, inst.value(), 0.18F, pitch(t[R.nextInt(t.length)]));
      }
   }
}
