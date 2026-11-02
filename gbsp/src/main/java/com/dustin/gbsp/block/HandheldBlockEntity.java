package com.dustin.gbsp.block;

import com.dustin.gbsp.GameBoySP;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class HandheldBlockEntity extends BlockEntity implements GeoBlockEntity {
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private static final RawAnimation ON = RawAnimation.begin().thenPlay("power_on").thenLoop("on");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public HandheldBlockEntity(BlockPos pos, BlockState state) {
      super(GameBoySP.HANDHELD_BLOCK_ENTITY, pos, state);
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("main", 4, test -> {
         test.setAnimation(GameBoySP.HANDHELD_ON_AT.test(this.getBlockPos()) ? ON : IDLE);
         return PlayState.CONTINUE;
      }));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
