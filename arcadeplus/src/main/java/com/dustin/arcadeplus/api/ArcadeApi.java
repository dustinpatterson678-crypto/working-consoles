package com.dustin.arcadeplus.api;

import com.dustin.arcadeplus.MegaJackpot;
import com.dustin.arcadeplus.Prizes;
import com.dustin.arcadeplus.TicketBank;
import com.dustin.arcadeplus.TicketGameLogic;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Dustin's Arcade for addon mods (see docs/API.md): pay tickets from your own machines, read and fill ticket
 * cards, add prizes to the prize booth and react to wins anywhere in the arcade.
 */
public final class ArcadeApi {
   public static final int VERSION = 1;

   /** Fired on the server every time a machine pays paper tickets (after the Mega Jackpot roll). */
   public static final Event<TicketsWon> TICKETS_WON = EventFactory.createArrayBacked(TicketsWon.class,
      listeners -> (level, machine, game, tickets, jackpot) -> {
         for (TicketsWon l : listeners) {
            l.onWin(level, machine, game, tickets, jackpot);
         }
      });

   private ArcadeApi() {
   }

   @FunctionalInterface
   public interface TicketsWon {
      void onWin(ServerLevel level, BlockPos machine, String game, int tickets, boolean jackpot);
   }

   /**
    * Server: your machine at `machine` scored `points`. They turn into paper tickets at the arcade's rate, feed
    * the Mega Jackpot (and may hit it), fire TICKETS_WON and come out of the machine like any arcade game.
    */
   public static void payPoints(Level level, BlockPos machine, int points) {
      TicketGameLogic.pay(level, machine, points);
   }

   /** Tickets saved on a player's ticket card. */
   public static int ticketCard(Player player) {
      return TicketBank.balance(player);
   }

   /** Server: adds (or with a negative amount, takes) tickets on a player's card. Never goes below 0. */
   public static void addToTicketCard(Player player, int tickets) {
      TicketBank.set(player, TicketBank.balance(player) + tickets);
   }

   /** Server: the current Mega Jackpot pot. */
   public static int megaJackpot(ServerLevel level) {
      return MegaJackpot.pot(level);
   }

   /**
    * Adds a prize to the prize booth for `tickets` tickets (item id like "mymod:plush", count per prize). Call from
    * your plugin; a line for the same item in the player's arcadeplus_prizes.txt overrides your price.
    */
   public static void addPrize(String itemId, int count, int tickets) {
      Prizes.ADDON.add(new Prizes.Prize(itemId, count, tickets));
   }
}
