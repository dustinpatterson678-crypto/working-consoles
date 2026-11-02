package com.dustin.wcore.block;

import com.dustin.wcore.ConsoleSettings;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class ConsoleBlockEntity extends BlockEntity {
   public static final int MAX_CABLE = 12;
   protected BlockPos tvPos;
   protected BlockPos outletPos;
   protected boolean on;
   protected boolean powered;
   protected String media = "";
   protected String anim = "";
   protected int animSerial;
   private int tickCounter;
   protected int signal;
   protected int flickers;
   protected long stableSince;
   private int decayTicks;

   protected ConsoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public abstract String consoleName();

   /** Controller ports on the real console. The single source for seat and input array sizes. */
   public int portCount() {
      return 2;
   }

   /** System ids this console plays, for ControllerTypes.accepts. Empty (or "*") takes any controller. */
   public Set<String> systemIds() {
      return Set.of();
   }

   public BlockPos tvPos() {
      return this.tvPos;
   }

   public BlockPos outletPos() {
      return this.outletPos;
   }

   public boolean isOn() {
      return this.on;
   }

   public boolean isPowered() {
      return this.powered;
   }

   public String media() {
      return this.media;
   }

   public String anim() {
      return this.anim;
   }

   public int animSerial() {
      return this.animSerial;
   }

   public int signal() {
      return this.signal;
   }

   public int flickers() {
      return this.flickers;
   }

   public long stableSeconds() {
      return this.level == null ? 0L : Math.max(0L, (this.level.getGameTime() - this.stableSince) / 20L);
   }

   public PowerGrade grade() {
      if (!this.powered) {
         return PowerGrade.OFF;
      } else {
         return !ConsoleSettings.get().redstonePower ? PowerGrade.CLEAN : PowerGrade.of(this.signal, this.flickers, this.stableSeconds());
      }
   }

   public void plugTv(BlockPos pos) {
      this.tvPos = pos;
      this.sync();
   }

   public void plugOutlet(BlockPos pos) {
      this.outletPos = pos;
      this.updatePower();
      this.sync();
   }

   public void unplugAll() {
      this.tvPos = null;
      this.outletPos = null;
      this.on = false;
      this.powered = false;
      this.sync();
   }

   public void setOn(boolean on) {
      this.on = on && this.powered;
      this.sync();
   }

   public void setMedia(String media) {
      this.media = media == null ? "" : media;
      this.sync();
   }

   public void playAnim(String name) {
      this.onAnim(name);
   }

   protected void onAnim(String name) {
   }

   protected void sync() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide()) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public boolean refreshPower() {
      boolean changed = this.updatePower();
      if (changed) {
         this.sync();
      }

      return changed;
   }

   private boolean updatePower() {
      boolean live = this.level != null
         && this.outletPos != null
         && this.outletPos.closerThan(this.worldPosition, 13.0)
         && OutletBlock.isLive(this.level, this.outletPos);
      boolean changed = live != this.powered;
      this.powered = live;
      if (!live && this.on) {
         this.on = false;
         changed = true;
      }

      return changed;
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, ConsoleBlockEntity be) {
      if (++be.tickCounter % 10 == 0) {
         boolean changed = be.updatePower();
         int now = be.powered && be.outletPos != null ? OutletBlock.power(level, be.outletPos) : 0;
         if (now != be.signal) {
            if (be.signal > 0) {
               be.flickers++;
            }

            be.signal = now;
            be.stableSince = level.getGameTime();
            changed = true;
         }

         if (be.flickers > 0 && (be.decayTicks += 10) >= 1200) {
            be.decayTicks = 0;
            be.flickers--;
            changed = true;
         }

         if (be.on && be.flickers >= 3 && ConsoleSettings.get().redstonePower) {
            be.flickers = 0;
            be.on = false;
            be.playAnim("shake");

            for (Player p : level.players()) {
               if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 576.0) {
                  p.sendSystemMessage(
                     Component.literal("[" + be.consoleName() + "] Brownout! The power flickered and the console reset. Steady your redstone.")
                        .withStyle(ChatFormatting.RED)
                  );
               }
            }

            changed = true;
         }

         if (changed) {
            be.sync();
         }

         if (be.on && be.tickCounter % 20 == 0 && level instanceof ServerLevel sl) {
            ConsoleNoise.emit(sl, be.tvPos != null ? be.tvPos : pos);
         }
      }
   }

   protected void saveAdditional(ValueOutput out) {
      super.saveAdditional(out);
      out.putLong("tv", this.tvPos == null ? Long.MIN_VALUE : this.tvPos.asLong());
      out.putLong("outlet", this.outletPos == null ? Long.MIN_VALUE : this.outletPos.asLong());
      out.putBoolean("on", this.on);
      out.putBoolean("powered", this.powered);
      out.putString("media", this.media);
      out.putString("anim", this.anim);
      out.putInt("animSerial", this.animSerial);
      out.putInt("signal", this.signal);
      out.putInt("flickers", this.flickers);
      out.putLong("stableSince", this.stableSince);
   }

   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);
      long tv = in.getLongOr("tv", Long.MIN_VALUE);
      long outlet = in.getLongOr("outlet", Long.MIN_VALUE);
      this.tvPos = tv == Long.MIN_VALUE ? null : BlockPos.of(tv);
      this.outletPos = outlet == Long.MIN_VALUE ? null : BlockPos.of(outlet);
      this.on = in.getBooleanOr("on", false);
      this.powered = in.getBooleanOr("powered", false);
      this.media = in.getStringOr("media", "");
      this.anim = in.getStringOr("anim", "");
      this.animSerial = in.getIntOr("animSerial", 0);
      this.signal = in.getIntOr("signal", 0);
      this.flickers = in.getIntOr("flickers", 0);
      this.stableSince = in.getLongOr("stableSince", 0L);
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveCustomOnly(registries);
   }
}
