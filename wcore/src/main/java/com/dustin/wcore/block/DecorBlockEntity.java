package com.dustin.wcore.block;

import com.dustin.wcore.WorkingConsolesCore;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.NonNullList;
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

/**
 * Block entity for the v5 furniture pieces: carries the GeckoLib cache, the
 * display-slot contents (game cases / consoles / controllers) and the open
 * state of the doors/drawers.
 *
 * <p>The animation controller watches the open flag: a fresh instance adopts
 * the current state silently, later flips play the opening/closing sequence
 * once and then hold the matching idle pose. Because the flag is part of the
 * block entity update packet, every player sees the doors move.
 */
public class DecorBlockEntity extends BlockEntity implements GeoBlockEntity {
   private static final RawAnimation OPEN_SEQ = RawAnimation.begin().thenPlay("opening").thenLoop("open");
   private static final RawAnimation CLOSE_SEQ = RawAnimation.begin().thenPlay("closing").thenLoop("closed");
   private static final RawAnimation OPEN_HOLD = RawAnimation.begin().thenLoop("open");
   private static final RawAnimation CLOSED_HOLD = RawAnimation.begin().thenLoop("closed");

   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final NonNullList<ItemStack> items;
   private boolean open;
   private boolean animInit;
   private boolean lastOpen;

   public DecorBlockEntity(BlockPos pos, BlockState state) {
      super(WorkingConsolesCore.DECOR_BLOCK_ENTITY, pos, state);
      this.items = NonNullList.withSize(state.getBlock() instanceof DecorBlock decor ? decor.slots.size() : 0, ItemStack.EMPTY);
   }

   public boolean isOpen() {
      return this.open;
   }

   public void setOpen(boolean open) {
      this.open = open;
      this.setChanged();
      if (this.level != null && !this.level.isClientSide()) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public int slotCount() {
      return this.items.size();
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

   /** First empty slot that accepts the given kind, or -1. */
   public int firstEmpty(DecorBlock block, DecorBlock.SlotKind kind) {
      int n = Math.min(this.items.size(), block.slots.size());

      for (int i = 0; i < n; i++) {
         if (block.slots.get(i).kind() == kind && this.items.get(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   /** Last filled slot, or -1. */
   public int lastFilled() {
      for (int i = this.items.size() - 1; i >= 0; i--) {
         if (!this.items.get(i).isEmpty()) {
            return i;
         }
      }

      return -1;
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
      out.putBoolean("open", this.open);
   }

   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);

      for (int i = 0; i < this.items.size(); i++) {
         this.items.set(i, ItemStack.EMPTY);
      }

      ContainerHelper.loadAllItems(in, this.items);
      this.open = in.getBooleanOr("open", false);
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveCustomOnly(registries);
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("main", 5, state -> {
         if (!this.animInit) {
            this.animInit = true;
            this.lastOpen = this.open;
            state.setAnimation(this.open ? OPEN_HOLD : CLOSED_HOLD);
            return PlayState.CONTINUE;
         }

         if (this.open != this.lastOpen) {
            this.lastOpen = this.open;
            state.setAnimation(this.open ? OPEN_SEQ : CLOSE_SEQ);
         }

         return PlayState.CONTINUE;
      }));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
