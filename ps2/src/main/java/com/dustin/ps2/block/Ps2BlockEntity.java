package com.dustin.ps2.block;

import com.dustin.ps2.Ps2Files;
import com.dustin.ps2.Ps2Mod;
import com.dustin.ps2.item.DiscCaseItem;
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
import net.minecraft.world.level.block.state.BlockState;

public class Ps2BlockEntity extends ConsoleBlockEntity implements GeoBlockEntity {
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private static final RawAnimation INSERT = RawAnimation.begin().thenPlay("insert");
   private static final RawAnimation EJECT = RawAnimation.begin().thenPlay("eject");
   private static final RawAnimation SHAKE = RawAnimation.begin().thenPlay("shake");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public Ps2BlockEntity(BlockPos pos, BlockState state) {
      super(Ps2Mod.CONSOLE_BLOCK_ENTITY, pos, state);
   }

   public String consoleName() {
      return "disc console";
   }

   protected void onAnim(String name) {
      this.triggerAnim("main", name);
   }

   public ItemStack discCase() {
      if (this.media.isEmpty()) {
         return ItemStack.EMPTY;
      }

      Ps2Files.Disc d = Ps2Files.disc(this.media);
      return DiscCaseItem.caseFor(this.media, d != null ? d.title() : Ps2Files.title(this.media));
   }

   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      if (this.level != null && !this.media.isEmpty()) {
         Block.popResource(this.level, pos, this.discCase());
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
