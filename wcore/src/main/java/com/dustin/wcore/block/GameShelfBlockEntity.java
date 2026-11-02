package com.dustin.wcore.block;

import com.dustin.wcore.WorkingConsolesCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class GameShelfBlockEntity extends BlockEntity {
   public static final int SLOTS = 8;
   private final NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);

   public GameShelfBlockEntity(BlockPos pos, BlockState state) {
      super(WorkingConsolesCore.GAME_SHELF_BLOCK_ENTITY, pos, state);
   }

   public ItemStack get(int slot) {
      return (ItemStack)this.items.get(slot);
   }

   public void set(int slot, ItemStack stack) {
      this.items.set(slot, stack);
      this.setChanged();
      if (this.level != null && !this.level.isClientSide()) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      if (this.level != null) {
         Containers.dropContents(this.level, pos, this.items);
      }

      super.preRemoveSideEffects(pos, state);
   }

   protected void saveAdditional(ValueOutput out) {
      super.saveAdditional(out);
      ContainerHelper.saveAllItems(out, this.items, true);
   }

   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);

      for (int i = 0; i < 8; i++) {
         this.items.set(i, ItemStack.EMPTY);
      }

      ContainerHelper.loadAllItems(in, this.items);
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveCustomOnly(registries);
   }
}
