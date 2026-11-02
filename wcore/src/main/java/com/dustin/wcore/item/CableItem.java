package com.dustin.wcore.item;

import com.dustin.wcore.block.ConsoleBlockEntity;
import com.dustin.wcore.block.MultiTvBlock;
import com.dustin.wcore.block.OutletBlock;
import com.dustin.wcore.block.TvBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CableItem extends Item {
   public CableItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext ctx) {
      Level level = ctx.getLevel();
      Player player = ctx.getPlayer();
      BlockPos pos = ctx.getClickedPos();
      // quad TVs: the block entity lives on the MAIN cell, whichever of the
      // four cells the player clicks with the cable
      BlockState clicked = level.getBlockState(pos);
      if (clicked.getBlock() instanceof MultiTvBlock) {
         pos = MultiTvBlock.mainPos(clicked, pos);
      }

      ItemStack stack = ctx.getItemInHand();
      BlockEntity be = level.getBlockEntity(pos);
      boolean isOutlet = level.getBlockState(pos).getBlock() instanceof OutletBlock;
      if (!(be instanceof ConsoleBlockEntity) && !(be instanceof TvBlockEntity) && !isOutlet) {
         return InteractionResult.PASS;
      }

      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      if (player != null && player.isShiftKeyDown() && !isOutlet) {
         int cables = 0;
         if (be instanceof ConsoleBlockEntity console) {
            if (console.tvPos() != null && level.getBlockEntity(console.tvPos()) instanceof TvBlockEntity tv && pos.equals(tv.consolePos())) {
               tv.plugConsole(null);
            }

            cables = (console.tvPos() != null ? 1 : 0) + (console.outletPos() != null ? 1 : 0);
            console.unplugAll();
         } else if (be instanceof TvBlockEntity tv) {
            if (tv.consolePos() != null && level.getBlockEntity(tv.consolePos()) instanceof ConsoleBlockEntity console) {
               console.plugTv(null);
            }

            cables = (tv.consolePos() != null ? 1 : 0) + (tv.outletPos() != null ? 1 : 0);
            tv.plugConsole(null);
            tv.plugOutlet(null);
         }

         if (cables > 0 && !player.isCreative()) {
            Block.popResource(level, pos.above(), new ItemStack(this, cables));
         }

         tell(player, cables > 0 ? "Unplugged " + cables + " cable" + (cables > 1 ? "s" : "") : "Nothing plugged in", ChatFormatting.YELLOW);
         return InteractionResult.SUCCESS;
      } else {
         CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         long held = data == null ? Long.MIN_VALUE : data.copyTag().getLongOr("from", Long.MIN_VALUE);
         BlockPos fromPos = held == Long.MIN_VALUE ? null : BlockPos.of(held);
         BlockEntity from = fromPos == null ? null : level.getBlockEntity(fromPos);
         boolean fromOutlet = fromPos != null && level.getBlockState(fromPos).getBlock() instanceof OutletBlock;
         if (fromPos != null && !fromPos.equals(pos)) {
            if (!fromPos.closerThan(pos, 13.0)) {
               tell(player, "Too far: cables reach 12 blocks", ChatFormatting.RED);
               return InteractionResult.SUCCESS;
            }

            boolean newCable = false;
            if (from instanceof ConsoleBlockEntity console && be instanceof TvBlockEntity tv) {
               if (tv.consolePos() != null && !tv.consolePos().equals(fromPos) && level.getBlockEntity(tv.consolePos()) instanceof ConsoleBlockEntity old) {
                  old.plugTv(null);
               }

               if (console.tvPos() != null && !console.tvPos().equals(pos) && level.getBlockEntity(console.tvPos()) instanceof TvBlockEntity oldTv) {
                  oldTv.plugConsole(null);
               }

               newCable = console.tvPos() == null;
               console.plugTv(pos);
               tv.plugConsole(fromPos);
               tell(player, "AV cable: " + console.consoleName() + " -> TV", ChatFormatting.GREEN);
            } else if (from instanceof ConsoleBlockEntity console && isOutlet) {
               newCable = console.outletPos() == null;
               console.plugOutlet(pos);
               tellPower(player, console.consoleName(), console.isPowered());
            } else if (fromOutlet && be instanceof ConsoleBlockEntity console) {
               newCable = console.outletPos() == null;
               console.plugOutlet(fromPos);
               tellPower(player, console.consoleName(), console.isPowered());
            } else if (fromOutlet && be instanceof TvBlockEntity tv) {
               newCable = tv.outletPos() == null;
               tv.plugOutlet(fromPos);
               tellPower(player, "TV", OutletBlock.isLive(level, fromPos));
            } else {
               if (!(from instanceof TvBlockEntity tv) || !isOutlet) {
                  tell(player, "Those two don't connect. Console -> TV or outlet, TV -> outlet", ChatFormatting.GRAY);
                  return InteractionResult.SUCCESS;
               }

               newCable = tv.outletPos() == null;
               tv.plugOutlet(pos);
               tellPower(player, "TV", OutletBlock.isLive(level, pos));
            }

            stack.remove(DataComponents.CUSTOM_DATA);
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.7F, 1.3F);
            if (newCable && player != null && !player.isCreative()) {
               stack.shrink(1);
            }

            return InteractionResult.SUCCESS;
         } else if (isOutlet) {
            // outlet-first: park the cable in the outlet, then click any console or TV
            CompoundTag tag = new CompoundTag();
            tag.putLong("from", pos.asLong());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            tell(player, "Cable in the outlet. Now click a console or TV", ChatFormatting.AQUA);
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6F, 1.6F);
            return InteractionResult.SUCCESS;
         } else {
            CompoundTag tag = new CompoundTag();
            tag.putLong("from", pos.asLong());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            tell(
               player,
               be instanceof TvBlockEntity ? "Cable in the TV. Now click a wall outlet" : "Cable in the console. Now click a TV or a wall outlet",
               ChatFormatting.AQUA
            );
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6F, 1.6F);
            return InteractionResult.SUCCESS;
         }
      }
   }

   private static void tellPower(Player player, String what, boolean live) {
      tell(
         player,
         live ? "Plugged in. The outlet is live: the " + what + " has power" : "Plugged in, but the outlet is dead. Give the block it's on a redstone signal",
         live ? ChatFormatting.GREEN : ChatFormatting.GOLD
      );
   }

   private static void tell(Player player, String text, ChatFormatting color) {
      if (player != null) {
         player.sendOverlayMessage(Component.literal(text).withStyle(color));
      }
   }
}
