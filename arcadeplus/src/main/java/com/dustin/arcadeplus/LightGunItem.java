package com.dustin.arcadeplus;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * The arcade light gun. Hold right-click to pull the trigger (the model shows the slide kicked back and a
 * muzzle flash while it's held). Aim comes from where you look: the client ray-casts your view onto the
 * gun cabinet's screen and feeds it to the game as a real light gun.
 */
public class LightGunItem extends Item {
   public LightGunItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      player.startUsingItem(hand);
      return InteractionResult.CONSUME;
   }

   @Override
   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 72000;
   }

   @Override
   public ItemUseAnimation getUseAnimation(ItemStack stack) {
      return ItemUseAnimation.NONE;
   }
}
