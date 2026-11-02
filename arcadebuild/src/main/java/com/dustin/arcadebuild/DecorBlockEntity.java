package com.dustin.arcadebuild;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Draws a DecorBlock, and is the storage for the ones that hold things (desk drawers, files, dumpster, media console). */
public class DecorBlockEntity extends BlockEntity implements Container {
   private final NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
   public long lastUse;                                   // cooldown for snack bowls, apples and the like
   public float sway;                                     // client: palm fronds rustle

   public DecorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   protected void saveAdditional(ValueOutput out) {
      super.saveAdditional(out);
      ContainerHelper.saveAllItems(out, this.items);
   }

   @Override
   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);
      ContainerHelper.loadAllItems(in, this.items);
   }

   @Override public int getContainerSize() { return this.items.size(); }
   @Override public boolean isEmpty() { return this.items.stream().allMatch(ItemStack::isEmpty); }
   @Override public ItemStack getItem(int slot) { return this.items.get(slot); }
   @Override public ItemStack removeItem(int slot, int count) { ItemStack s = ContainerHelper.removeItem(this.items, slot, count); this.setChanged(); return s; }
   @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(this.items, slot); }
   @Override public void setItem(int slot, ItemStack stack) { this.items.set(slot, stack); this.setChanged(); }
   @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
   @Override public void clearContent() { this.items.clear(); }
}
