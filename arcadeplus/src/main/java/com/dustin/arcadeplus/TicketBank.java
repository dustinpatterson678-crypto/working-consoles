package com.dustin.arcadeplus;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Your arcade ticket card. Paper tickets you win go in the "ticket muncher" at the prize booth and land on
 * the card, so you can save up for big prizes over many sessions (the balance is saved with the player and
 * survives death). Synced to the owner so the booth screen shows it.
 */
public final class TicketBank {
   private TicketBank() {
   }

   public static final AttachmentType<Integer> BALANCE = AttachmentRegistry.create(ArcadePlusMod.id("ticket_balance"),
      b -> b.persistent(Codec.INT).initializer(() -> 0).copyOnDeath().syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

   public static int balance(Player player) {
      Integer v = player.getAttached(BALANCE);
      return v == null ? 0 : v;
   }

   public static void set(Player player, int value) {
      player.setAttached(BALANCE, Math.max(0, value));
   }

   /** Server: feed every paper ticket in the inventory into the muncher. Returns how many were eaten. */
   public static int deposit(Player player) {
      Item t = TicketGameLogic.item("arcade_ticket");
      int n = 0;
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack s = player.getInventory().getItem(i);
         if (!s.isEmpty() && s.getItem() == t) {
            n += s.getCount();
            player.getInventory().setItem(i, ItemStack.EMPTY);
         }
      }
      if (n > 0) {
         set(player, balance(player) + n);
         player.getInventory().setChanged();
      }
      return n;
   }

   public static void init() {
      // class load registers the attachment
   }
}
