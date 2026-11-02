package com.dustin.arcadeplus;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Right-click to open: a random goodie, from snacks up to diamonds. */
public class MysteryBoxItem extends Item {
   public MysteryBoxItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack box = player.getItemInHand(hand);
      if (!level.isClientSide()) {
         RandomSource r = level.getRandom();
         int roll = r.nextInt(100);
         ItemStack prize;
         if (roll < 5) prize = new ItemStack(Items.DIAMOND, 1 + r.nextInt(2));
         else if (roll < 15) prize = new ItemStack(Items.GOLDEN_APPLE);
         else if (roll < 30) prize = new ItemStack(Items.EMERALD, 4 + r.nextInt(5));
         else if (roll < 42) prize = new ItemStack(Items.EXPERIENCE_BOTTLE, 8);
         else if (roll < 52) prize = new ItemStack(Items.ENDER_PEARL, 4);
         else if (roll < 60) prize = new ItemStack(Items.NAME_TAG);
         else if (roll < 66) prize = new ItemStack(Items.SADDLE);
         else if (roll < 80) prize = new ItemStack(ArcadePlusMod.NICKEL, 20);
         else prize = new ItemStack(Items.FIREWORK_ROCKET, 16);
         player.sendOverlayMessage(Component.literal("Mystery Box: " + prize.getCount() + " x ").append(prize.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
         player.getInventory().placeItemBackInInventory(prize, net.minecraft.util.Prediction.SERVER_ONLY);
         level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
         if (!player.isCreative()) {
            box.shrink(1);
         }
      }
      return InteractionResult.SUCCESS;
   }
}
