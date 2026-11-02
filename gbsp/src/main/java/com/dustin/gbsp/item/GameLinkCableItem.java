package com.dustin.gbsp.item;

import com.dustin.gbsp.link.ServerLinkCableManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class GameLinkCableItem extends Item {
   public GameLinkCableItem(Properties properties) {
      super(properties);
   }

   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (player instanceof ServerPlayer sp && target instanceof ServerPlayer other) {
         ServerLinkCableManager.link(sp, other);
      }

      return (InteractionResult)(target instanceof Player ? InteractionResult.SUCCESS : InteractionResult.PASS);
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (player instanceof ServerPlayer sp && ServerLinkCableManager.isLinked(sp)) {
         ServerLinkCableManager.unlink(sp, "cable unplugged");
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }
}
