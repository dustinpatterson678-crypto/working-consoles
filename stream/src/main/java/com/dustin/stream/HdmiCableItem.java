package com.dustin.stream;

import com.dustin.wcore.block.ConsoleBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Click a console, then a Smart TV or Projector: the console shows up on HDMI 1, 2 or 3. */
public class HdmiCableItem extends Item {
   private static final double MAX_LENGTH = 16.0;

   public HdmiCableItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext ctx) {
      return handle(ctx.getLevel(), ctx.getPlayer(), ctx.getClickedPos(), ctx.getItemInHand());
   }

   /**
    * Runs from a UseBlockCallback before the clicked block's own right-click, because consoles and
    * screens use right-click for power and would otherwise swallow the cable click.
    */
   public static InteractionResult handle(Level level, Player player, BlockPos pos, ItemStack stack) {
      BlockEntity be = level.getBlockEntity(pos);
      boolean screen = be instanceof MediaBlockEntity;
      boolean console = be instanceof ConsoleBlockEntity && !screen;
      if (!screen && !console) {
         return InteractionResult.PASS;
      }
      if (level.isClientSide() || player == null) {
         return InteractionResult.SUCCESS;
      }
      BlockPos from = source(stack);
      if (console) {
         setSource(stack, pos);
         MediaBlock.tell(player, "HDMI plugged into the " + ((ConsoleBlockEntity)be).consoleName() + ": now click a Smart TV or Projector", ChatFormatting.AQUA);
      } else if (player.isShiftKeyDown()) {
         int n = ((MediaBlockEntity)be).unplugAllHdmi();
         MediaBlock.tell(player, n > 0 ? "Unplugged " + n + " HDMI cable" + (n > 1 ? "s" : "") : "No HDMI cables plugged in", ChatFormatting.YELLOW);
      } else if (from == null) {
         MediaBlock.tell(player, "Click a console first, then the screen", ChatFormatting.GOLD);
      } else if (!(level.getBlockEntity(from) instanceof ConsoleBlockEntity src) || src instanceof MediaBlockEntity) {
         setSource(stack, null);
         MediaBlock.tell(player, "That console is gone: click a console again", ChatFormatting.GOLD);
      } else if (from.distSqr(pos) > MAX_LENGTH * MAX_LENGTH) {
         MediaBlock.tell(player, "Too far: an HDMI cable reaches " + (int)MAX_LENGTH + " blocks", ChatFormatting.RED);
      } else {
         int port = ((MediaBlockEntity)be).plugHdmi(from);
         setSource(stack, null);
         MediaBlock.tell(player, src.consoleName() + " connected to HDMI " + port + " (Ctrl+I in the TV controls switches inputs)", ChatFormatting.GREEN);
      }
      return InteractionResult.SUCCESS;
   }

   private static BlockPos source(ItemStack stack) {
      CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
      long v = tag.getLongOr("hdmi_from", Long.MIN_VALUE);
      return v == Long.MIN_VALUE ? null : BlockPos.of(v);
   }

   private static void setSource(ItemStack stack, BlockPos pos) {
      if (pos == null) {
         stack.remove(DataComponents.CUSTOM_DATA);
      } else {
         CompoundTag tag = new CompoundTag();
         tag.putLong("hdmi_from", pos.asLong());
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      }
   }
}
