package com.dustin.arcadeplus;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Each ticket game keeps its own top 5 scores (saved in the machine as "name:score|name:score"). A new #1 is
 * announced to everyone nearby and the attendant cheers; sneak + right-click the machine to read its board.
 */
public final class HighScores {
   public static final int SIZE = 5;

   private HighScores() {
   }

   public record Entry(String name, int score) {
   }

   public static List<Entry> parse(String saved) {
      List<Entry> list = new ArrayList<>();
      if (saved == null || saved.isEmpty()) {
         return list;
      }

      for (String part : saved.split("[|]")) {
         int colon = part.lastIndexOf(':');
         if (colon > 0) {
            try {
               list.add(new Entry(part.substring(0, colon), Integer.parseInt(part.substring(colon + 1))));
            } catch (NumberFormatException ignored) {
            }
         }
      }
      return list;
   }

   private static String write(List<Entry> list) {
      StringBuilder b = new StringBuilder();
      for (Entry e : list) {
         if (b.length() > 0) {
            b.append('|');
         }
         b.append(e.name()).append(':').append(e.score());
      }
      return b.toString();
   }

   /** Server, at game over: puts the player's score on the board if it makes the top 5. */
   static void record(Level level, BlockPos pos, TicketGameBlockEntity be) {
      if (be.player.isEmpty() || be.score <= 0) {
         return;
      }

      List<Entry> list = parse(be.highScores);
      int rank = 0;
      while (rank < list.size() && list.get(rank).score() >= be.score) {
         rank++;
      }
      if (rank >= SIZE) {
         return;
      }

      list.add(rank, new Entry(be.player, be.score));
      while (list.size() > SIZE) {
         list.remove(list.size() - 1);
      }
      be.highScores = write(list);
      be.setChanged();
      String game = be.game().title;
      if (rank == 0) {
         TicketGameLogic.tellNear(level, pos, "\u2605 NEW HIGH SCORE on " + game + ": " + be.player + " - " + be.score + "! \u2605", ChatFormatting.LIGHT_PURPLE);
         if (level instanceof ServerLevel sl) {
            AttendantEntity.cheerNear(sl, pos, "New record on " + game + "! Way to go, " + be.player + "!");
         }
      } else {
         TicketGameLogic.tellNear(level, pos, be.player + " made the " + game + " board at #" + (rank + 1) + "!", ChatFormatting.AQUA);
      }
   }

   /** Shows a machine's board in chat. */
   static void show(Player player, TicketGameBlockEntity be) {
      List<Entry> list = parse(be.highScores);
      TicketGameLogic.tell(player, "\u2014 " + be.game().title + " high scores \u2014", ChatFormatting.GOLD);
      if (list.isEmpty()) {
         TicketGameLogic.tell(player, "No scores yet. Be the first!", ChatFormatting.GRAY);
      }
      for (int i = 0; i < list.size(); i++) {
         Entry e = list.get(i);
         TicketGameLogic.tell(player, (i + 1) + ". " + e.name() + "  " + e.score(), i == 0 ? ChatFormatting.YELLOW : ChatFormatting.WHITE);
      }
   }
}
