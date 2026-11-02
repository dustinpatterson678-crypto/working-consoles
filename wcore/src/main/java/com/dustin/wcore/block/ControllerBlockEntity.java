package com.dustin.wcore.block;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A controller put down on a block. Keeps a copy of the stack it was placed from (with its controller id and
 * pairing) and drops exactly that stack again when broken.
 */
public class ControllerBlockEntity extends BlockEntity implements GeoBlockEntity {
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private ItemStack stack = ItemStack.EMPTY;
   private boolean noDrop;

   public ControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public ItemStack stack() {
      return this.stack;
   }

   public void setStack(ItemStack stack) {
      this.stack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
      this.setChanged();
      if (this.level != null && !this.level.isClientSide()) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   /** The stack this block gives back: the stored one, or a plain controller if none was stored. */
   public ItemStack dropStack() {
      return this.stack.isEmpty() ? new ItemStack(this.getBlockState().getBlock()) : this.stack.copy();
   }

   /** Creative break: don't drop anything. */
   public void suppressDrop() {
      this.noDrop = true;
   }

   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      if (this.level != null && !this.level.isClientSide() && !this.noDrop) {
         Block.popResource(this.level, pos, this.dropStack());
      }

      super.preRemoveSideEffects(pos, state);
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("main", 0, test -> {
         test.setAnimation(IDLE);
         return PlayState.CONTINUE;
      }));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   protected void saveAdditional(ValueOutput out) {
      super.saveAdditional(out);
      if (!this.stack.isEmpty()) {
         out.store("stack", ItemStack.CODEC, this.stack);
      }
   }

   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);
      this.stack = in.read("stack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveCustomOnly(registries);
   }
}
