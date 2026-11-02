package com.dustin.arcadebuild;

import com.dustin.wcore.block.ConsoleBlockEntity;
import com.dustin.wcore.block.OutletBlock;
import com.dustin.wcore.block.TvBlockEntity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Places the arcade a few thousand blocks per tick, then plugs every machine into its outlet (and TV). */
public final class BuildJob {
   public record Op(BlockPos pos, BlockState state) {}
   public record Wire(BlockPos console, BlockPos tv, BlockPos outlet) {}

   private static final List<BuildJob> JOBS = new ArrayList<>();
   private static final int PER_TICK = 512;

   private final ServerLevel level;
   private final Player player;
   private final List<Op> ops;
   private final List<Wire> wires;
   private final List<BlockPos> outlets;
   private final int missing;
   private final BlockPos breaker, facade;
   private int next;
   private boolean hall;

   private BuildJob(ServerLevel level, Player player, ArcadeLayout layout) {
      this.level = level;
      this.player = player;
      this.ops = layout.ops;
      this.wires = layout.wires;
      this.outlets = layout.outlets;
      this.missing = layout.missing.size();
      this.breaker = layout.breaker;
      this.facade = layout.facade;
      if (!layout.missing.isEmpty()) {
         ArcadeBuildMod.LOGGER.info("Arcade build: these blocks aren't installed and were skipped: {}", layout.missing);
      }
   }

   public static void start(ServerLevel level, BlockPos pos, Direction facing, Player player, BuildingPlan plan) {
      ArcadeLayout layout = new ArcadeLayout(pos, facing, plan);
      layout.generate();
      ArcadeBuildMod.LOGGER.info("Prepared {} build: {} blocks, {} machines to wire, plan {}", plan.name, layout.ops.size(), layout.wires.size(), plan.id);
      JOBS.add(new BuildJob(level, player, layout));
      player.sendSystemMessage(Component.literal("Building " + plan.name + "... (" + layout.ops.size() + " blocks)").withStyle(ChatFormatting.AQUA));
   }

   /** Build a building's Grand Hall right now (the player is about to walk into it). */
   public static void runHall(ServerLevel level, BlockPos origin, Player player) {
      ArcadeLayout layout = new ArcadeLayout(origin.above().relative(Direction.NORTH), Direction.SOUTH, BuildingPlan.DUSTIN);
      layout.grandHall();
      BuildJob job = new BuildJob(level, player, layout);
      job.hall = true;
      while (!job.tick()) {
         // all at once: a few ticks' worth of blocks
      }
   }

   public static void tickAll() {
      Iterator<BuildJob> it = JOBS.iterator();
      while (it.hasNext()) {
         if (it.next().tick()) it.remove();
      }
   }

   /** Returns true when finished. */
   private boolean tick() {
      int end = Math.min(this.ops.size(), this.next + PER_TICK);
      for (; this.next < end; this.next++) {
         Op op = this.ops.get(this.next);
         this.level.setBlock(op.pos, op.state, 2 | 16);
      }
      if (this.next < this.ops.size()) return false;
      for (BlockPos o : this.outlets) {
         try {
            OutletBlock.refresh(this.level, o);
         } catch (Throwable ignored) {
         }
      }
      if (this.breaker != null && this.level.getBlockEntity(this.breaker) instanceof BreakerBlockEntity b) {
         b.facade = this.facade.asLong();
         b.setChanged();
      }
      int plugged = 0;
      for (Wire w : this.wires) {
         if (this.level.getBlockEntity(w.console) instanceof ConsoleBlockEntity c) {
            c.plugOutlet(w.outlet);
            if (w.tv != null && this.level.getBlockEntity(w.tv) instanceof TvBlockEntity tv) {
               c.plugTv(w.tv);
               tv.plugConsole(w.console);
               tv.plugOutlet(w.outlet);
            }
            c.refreshPower();
            plugged++;
         }
      }
      if (this.hall) {
         this.player.sendSystemMessage(Component.literal("Welcome to the Grand Hall! " + plugged + " machines plugged in and ready. Walk back out the front doors to leave.").withStyle(ChatFormatting.GREEN));
         return true;
      }
      this.player.sendSystemMessage(Component.literal("The arcade is built! " + plugged + " machines plugged in and powered."
         + (this.missing > 0 ? " (" + this.missing + " kinds of machine from mods you don't have were left out.)" : "")
         + " Throw the main breaker to switch the whole arcade on and off.").withStyle(ChatFormatting.GREEN));
      return true;
   }
}
