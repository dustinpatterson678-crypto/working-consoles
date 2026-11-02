package com.dustin.wcore.block;

import com.dustin.wcore.WorkingConsolesCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PowerMonitorBlockEntity extends BlockEntity {
   public static final int RANGE = 8;
   private BlockPos consolePos;
   private int comparator;
   private int ticks;

   public PowerMonitorBlockEntity(BlockPos pos, BlockState state) {
      super(WorkingConsolesCore.POWER_MONITOR_BLOCK_ENTITY, pos, state);
   }

   public BlockPos consolePos() {
      return this.consolePos;
   }

   public int comparatorLevel() {
      return this.comparator;
   }

   public ConsoleBlockEntity console() {
      return this.level != null && this.consolePos != null && this.level.getBlockEntity(this.consolePos) instanceof ConsoleBlockEntity c ? c : null;
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, PowerMonitorBlockEntity be) {
      if (++be.ticks % 10 == 0) {
         if (be.ticks % 40 == 0 || be.console() == null) {
            BlockPos found = null;
            double best = Double.MAX_VALUE;

            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-8, -8, -8), pos.offset(8, 8, 8))) {
               if (level.getBlockEntity(p) instanceof ConsoleBlockEntity) {
                  double d = p.distSqr(pos);
                  if (d < best) {
                     best = d;
                     found = p.immutable();
                  }
               }
            }

            if (found == null ? be.consolePos != null : !found.equals(be.consolePos)) {
               be.consolePos = found;
               be.setChanged();
               level.sendBlockUpdated(pos, state, state, 3);
            }
         }

         ConsoleBlockEntity c = be.console();
         int out = c == null ? 0 : c.grade().comparator;
         if (out != be.comparator) {
            be.comparator = out;
            be.setChanged();
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
         }
      }
   }

   protected void saveAdditional(ValueOutput out) {
      super.saveAdditional(out);
      out.putLong("console", this.consolePos == null ? Long.MIN_VALUE : this.consolePos.asLong());
      out.putInt("comparator", this.comparator);
   }

   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);
      long c = in.getLongOr("console", Long.MIN_VALUE);
      this.consolePos = c == Long.MIN_VALUE ? null : BlockPos.of(c);
      this.comparator = in.getIntOr("comparator", 0);
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveCustomOnly(registries);
   }
}
