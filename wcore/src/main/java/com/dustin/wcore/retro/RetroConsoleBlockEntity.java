package com.dustin.wcore.retro;

import java.util.Set;
import com.dustin.wcore.block.ConsoleBlockEntity;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RetroConsoleBlockEntity extends ConsoleBlockEntity implements GeoBlockEntity {
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private static final RawAnimation INSERT = RawAnimation.begin().thenPlay("insert");
   private static final RawAnimation EJECT = RawAnimation.begin().thenPlay("eject");
   private static final RawAnimation SHAKE = RawAnimation.begin().thenPlay("shake");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public RetroConsoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public RetroConsoleBlock block() {
      return this.getBlockState().getBlock() instanceof RetroConsoleBlock b ? b : null;
   }

   @Override
   public String consoleName() {
      RetroConsoleBlock b = this.block();
      return b != null ? b.displayName : "console";
   }

   @Override
   public int portCount() {
      RetroSystem s = this.system();
      if (s == null) {
         RetroConsoleBlock b = this.block();
         if (b != null) {
            for (String id : b.systems) {
               s = RetroSystem.get(id);
               if (s != null) {
                  break;
               }
            }
         }
      }

      return s != null ? s.ports : 2;
   }

   @Override
   public Set<String> systemIds() {
      RetroConsoleBlock b = this.block();
      return b != null ? b.systems : Set.of();
   }

   public RetroSystem system() {
      int bar = this.media.indexOf(124);
      return bar > 0 ? RetroSystem.get(this.media.substring(0, bar)) : null;
   }

   public String gameFile() {
      int bar = this.media.indexOf(124);
      return bar > 0 ? this.media.substring(bar + 1) : "";
   }

   public ItemStack mediaStack() {
      RetroSystem s = this.system();
      return s != null && s.mediaItem.get() != null ? RetroMediaItem.of(s, this.gameFile()) : ItemStack.EMPTY;
   }

   @Override
   protected void onAnim(String name) {
      this.triggerAnim("main", name);
   }

   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      if (this.level != null && !this.media.isEmpty()) {
         Block.popResource(this.level, pos, this.mediaStack());
      }

      super.preRemoveSideEffects(pos, state);
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("main", 0, test -> {
         test.setAnimation(IDLE);
         return PlayState.CONTINUE;
      }).triggerableAnim("insert", INSERT).triggerableAnim("eject", EJECT).triggerableAnim("shake", SHAKE));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
