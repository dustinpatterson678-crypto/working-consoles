package com.dustin.wcore.block;

import com.dustin.wcore.WorkingConsolesCore;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TvBlockEntity extends BlockEntity implements GeoBlockEntity {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private BlockPos consolePos;
   private BlockPos outletPos;

   public TvBlockEntity(BlockPos pos, BlockState state) {
      super(WorkingConsolesCore.TV_BLOCK_ENTITY, pos, state);
   }

   public BlockPos consolePos() {
      return this.consolePos;
   }

   public BlockPos outletPos() {
      return this.outletPos;
   }

   public boolean isPowered() {
      return this.level != null && this.outletPos != null && OutletBlock.isLiveClientSafe(this.level, this.outletPos);
   }

   public void plugConsole(BlockPos pos) {
      this.consolePos = pos;
      this.sync();
   }

   public void plugOutlet(BlockPos pos) {
      this.outletPos = pos;
      this.sync();
   }

   private void sync() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide()) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   protected void saveAdditional(ValueOutput out) {
      super.saveAdditional(out);
      out.putLong("console", this.consolePos == null ? Long.MIN_VALUE : this.consolePos.asLong());
      out.putLong("outlet", this.outletPos == null ? Long.MIN_VALUE : this.outletPos.asLong());
   }

   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);
      long c = in.getLongOr("console", Long.MIN_VALUE);
      this.consolePos = c == Long.MIN_VALUE ? null : BlockPos.of(c);
      long o = in.getLongOr("outlet", Long.MIN_VALUE);
      this.outletPos = o == Long.MIN_VALUE ? null : BlockPos.of(o);
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveCustomOnly(registries);
   }

   public void registerControllers(ControllerRegistrar controllers) {
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
