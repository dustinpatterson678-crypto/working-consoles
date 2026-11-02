package com.dustin.arcadeplus;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The arcade runs on nickels only. Old Arcade Tokens are swapped for nickels, hidden from the creative tabs,
 * and the core's change machine, ticket counter, prize counter and snack machines are re-priced in nickels
 * so the ticket economy works like a real arcade.
 */
public final class ArcadeEconomy {
   private ArcadeEconomy() {
   }

   public static final int NICKELS_PER_EMERALD = 20, SNACK_PRICE = 10;

   private static Item token() {
      return TicketGameLogic.item("arcade_token");
   }

   private static String id(net.minecraft.world.level.block.Block b) {
      Identifier k = BuiltInRegistries.BLOCK.getKey(b);
      return k == null ? "" : k.toString();
   }

   public static void init() {
      CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register((tab, output) -> {
         UnifiedTab.capture(tab, output.getDisplayStacks());
         Item t = token();
         output.getDisplayStacks().removeIf(s -> s.getItem() == t);
         output.getSearchTabStacks().removeIf(s -> s.getItem() == t || UnifiedTab.hidden(s));
      });
      ServerTickEvents.END_SERVER_TICK.register(server -> {
         if (server.getTickCount() % 20 != 0) return;
         Item t = token();
         if (t == null || t == Items.AIR) return;
         for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            var inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
               ItemStack s = inv.getItem(i);
               if (!s.isEmpty() && s.getItem() == t) inv.setItem(i, new ItemStack(ArcadePlusMod.NICKEL, s.getCount()));   // tokens are nickels now
            }
         }
      });
      UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
         String b = id(level.getBlockState(hit.getBlockPos()).getBlock());
         if (!b.startsWith("wcore:")) return InteractionResult.PASS;
         switch (b) {
            case "wcore:change_machine", "wcore:ticket_counter", "wcore:prize_counter", "wcore:snack_machine", "wcore:drink_machine", "wcore:vending_machine" -> {
            }
            default -> {
               return InteractionResult.PASS;
            }
         }
         if (hand != net.minecraft.world.InteractionHand.MAIN_HAND) return InteractionResult.SUCCESS;
         if (level.isClientSide()) return InteractionResult.SUCCESS;
         ItemStack held = player.getMainHandItem();
         switch (b) {
            case "wcore:change_machine" -> change(player, held);
            case "wcore:ticket_counter" -> {
               int n = TicketBank.deposit(player);
               msg(player, n > 0 ? "Ticket muncher: +" + String.format("%,d", n) + " tickets. Card balance: " + String.format("%,d", TicketBank.balance(player))
                  : "Ticket muncher: card balance " + String.format("%,d", TicketBank.balance(player)) + ". Redeem at the Prize Booth", ChatFormatting.YELLOW);
               if (n > 0) level.playSound(null, hit.getBlockPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.7F);
            }
            case "wcore:prize_counter" -> {                                // works just like the Prize Booth
               int n = TicketBank.deposit(player);
               if (n > 0) msg(player, "Ticket muncher: +" + String.format("%,d", n) + " tickets. Card balance: " + String.format("%,d", TicketBank.balance(player)), ChatFormatting.YELLOW);
               if (player instanceof ServerPlayer sp) com.dustin.arcadeplus.net.PrizeListPacket.send(sp, hit.getBlockPos());
            }
            default -> snack(player, held, b, level, hit.getBlockPos());
         }
         return InteractionResult.SUCCESS;
      });
   }

   /** Blocks you can redeem tickets at. */
   public static boolean isBooth(net.minecraft.world.level.block.Block b) {
      return b instanceof PrizeBoothBlock || b instanceof PrizeStandBlock || id(b).equals("wcore:prize_counter");
   }

   private static void msg(Player p, String s, ChatFormatting c) {
      p.sendSystemMessage(Component.literal(s).withStyle(c));
   }

   private static void change(Player player, ItemStack held) {
      if (!held.isEmpty() && held.getItem() == token()) {
         int n = held.getCount();
         held.shrink(n);
         give(player, n);
         msg(player, "Change machine: " + n + " old tokens -> " + n + " nickels", ChatFormatting.GREEN);
      } else if (!held.isEmpty() && held.is(Items.EMERALD)) {
         int n = player.isShiftKeyDown() ? held.getCount() : 1;      // sneak to change the whole stack
         held.shrink(n);
         give(player, n * NICKELS_PER_EMERALD);
         player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8F, 1.8F);
         msg(player, "Change machine: " + n + " emerald" + (n == 1 ? "" : "s") + " -> " + n * NICKELS_PER_EMERALD + " nickels", ChatFormatting.GREEN);
      } else {
         msg(player, "Change machine: 1 emerald = " + NICKELS_PER_EMERALD + " nickels (sneak to change the whole stack)", ChatFormatting.GOLD);
      }
   }

   private static void give(Player player, int n) {
      while (n > 0) {
         int k = Math.min(99, n);
         ItemStack s = new ItemStack(ArcadePlusMod.NICKEL, k);
         if (!player.getInventory().add(s)) net.minecraft.world.level.block.Block.popResource(player.level(), player.blockPosition(), s);
         n -= k;
      }
   }

   private static void snack(Player player, ItemStack held, String b, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
      String what = b.equals("wcore:drink_machine") ? "arcade_soda" : b.equals("wcore:snack_machine") ? (level.getRandom().nextBoolean() ? "arcade_chips" : "arcade_candy") : "arcade_candy";
      if (held.isEmpty() || held.getItem() != ArcadePlusMod.NICKEL || held.getCount() < SNACK_PRICE) {
         msg(player, "Snacks cost " + SNACK_PRICE + " nickels: hold them and click", ChatFormatting.GOLD);
         return;
      }
      held.shrink(SNACK_PRICE);
      ItemStack s = new ItemStack(TicketGameLogic.item(what));
      if (!player.getInventory().add(s)) net.minecraft.world.level.block.Block.popResource(player.level(), player.blockPosition(), s);
      level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 0.8F);
   }
}
