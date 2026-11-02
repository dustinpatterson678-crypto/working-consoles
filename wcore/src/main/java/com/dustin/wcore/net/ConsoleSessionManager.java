package com.dustin.wcore.net;

import com.dustin.wcore.block.ConsoleBlockEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class ConsoleSessionManager {
   public static final double MAX_DISTANCE = 6.0;
   private static final Map<ConsoleSessionManager.ConsoleKey, UUID[]> SEATS = new HashMap<>();
   private static final Map<UUID, ConsoleSessionManager.Seat> BY_PLAYER = new HashMap<>();
   private static int ticks;

   private ConsoleSessionManager() {
   }

   private static ConsoleSessionManager.ConsoleKey key(ServerPlayer p, BlockPos pos) {
      return new ConsoleSessionManager.ConsoleKey(p.level().dimension(), pos.immutable());
   }

   public static int join(ServerPlayer player, BlockPos pos, boolean wantPlayer2) {
      if (!(player.level().getBlockEntity(pos) instanceof ConsoleBlockEntity)) {
         return -1;
      }

      if (!closeEnough(player, pos)) {
         return -1;
      }

      ConsoleSessionManager.ConsoleKey k = key(player, pos);
      ConsoleSessionManager.Seat current = BY_PLAYER.get(player.getUUID());
      if (current == null || !current.console().equals(k) || wantPlayer2 && current.port() == 0) {
         if (current != null) {
            leave(player, false);
         }

         UUID[] seats = SEATS.computeIfAbsent(k, x -> new UUID[2]);
         int port = !wantPlayer2 && seats[0] == null ? 0 : (seats[1] == null ? 1 : (seats[0] == null ? 0 : -1));
         if (port < 0) {
            player.sendOverlayMessage(Component.literal("Both controllers are taken").withStyle(ChatFormatting.GOLD));
            return -1;
         } else {
            seats[port] = player.getUUID();
            BY_PLAYER.put(player.getUUID(), new ConsoleSessionManager.Seat(k, port));
            player.sendOverlayMessage(Component.literal("You're player " + (port + 1)).withStyle(ChatFormatting.AQUA));
            sync(player.level(), k);
            return port;
         }
      } else {
         return current.port();
      }
   }

   public static void leave(ServerPlayer player, boolean tell) {
      ConsoleSessionManager.Seat seat = BY_PLAYER.remove(player.getUUID());
      if (seat != null) {
         UUID[] seats = SEATS.get(seat.console());
         if (seats != null) {
            if (player.getUUID().equals(seats[seat.port()])) {
               seats[seat.port()] = null;
            }

            if (seats[0] == null && seats[1] == null) {
               SEATS.remove(seat.console());
            }
         }

         if (tell) {
            player.sendOverlayMessage(Component.literal("You put the controller down").withStyle(ChatFormatting.GRAY));
         }

         ServerLevel level = player.level().getServer() == null ? null : player.level().getServer().getLevel(seat.console().dimension());
         if (level != null) {
            sync(level, seat.console());
         }
      }
   }

   public static boolean mayDrive(ServerPlayer player, BlockPos pos, int port) {
      ConsoleSessionManager.Seat seat = BY_PLAYER.get(player.getUUID());
      if (seat == null || !seat.console().equals(key(player, pos))) {
         return false;
      }

      if (seat.port() == port) {
         return true;
      }

      UUID[] seats = SEATS.get(seat.console());
      return seat.port() == 0 && port == 1 && seats != null && seats[1] == null;
   }

   public static ConsoleSessionManager.Seat seatOf(UUID player) {
      return BY_PLAYER.get(player);
   }

   private static boolean closeEnough(ServerPlayer player, BlockPos console) {
      return within(player, console)
         ? true
         : player.level().getBlockEntity(console) instanceof ConsoleBlockEntity be && be.tvPos() != null && within(player, be.tvPos());
   }

   private static boolean within(ServerPlayer player, BlockPos pos) {
      return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 36.0;
   }

   public static void tick(MinecraftServer server) {
      if (++ticks % 10 == 0 && !BY_PLAYER.isEmpty()) {
         Iterator<Entry<UUID, ConsoleSessionManager.Seat>> it = BY_PLAYER.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, ConsoleSessionManager.Seat> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            ConsoleSessionManager.Seat seat = e.getValue();
            ServerLevel level = server.getLevel(seat.console().dimension());
            boolean keep = p != null
               && level != null
               && p.level() == level
               && level.getBlockEntity(seat.console().pos()) instanceof ConsoleBlockEntity be
               && be.isOn()
               && closeEnough(p, seat.console().pos());
            if (!keep) {
               it.remove();
               UUID[] seats = SEATS.get(seat.console());
               if (seats != null) {
                  if (e.getKey().equals(seats[seat.port()])) {
                     seats[seat.port()] = null;
                  }

                  if (seats[0] == null && seats[1] == null) {
                     SEATS.remove(seat.console());
                  }
               }

               if (p != null) {
                  p.sendOverlayMessage(Component.literal("You put the controller down").withStyle(ChatFormatting.GRAY));
               }

               if (level != null) {
                  sync(level, seat.console());
               }
            }
         }

         if (ticks % 100 == 0) {
            for (Entry<ConsoleSessionManager.ConsoleKey, UUID[]> e : SEATS.entrySet()) {
               ServerLevel level = server.getLevel(e.getKey().dimension());
               if (level != null) {
                  sync(level, e.getKey());
               }
            }
         }
      }
   }

   public static void disconnect(ServerPlayer player) {
      leave(player, false);
   }

   private static void sync(ServerLevel level, ConsoleSessionManager.ConsoleKey k) {
      UUID[] seats = SEATS.getOrDefault(k, new UUID[2]);
      SeatSyncPacket packet = new SeatSyncPacket(k.pos(), seats[0], seats[1]);
      BlockPos pos = k.pos();
      double r = 64.0;

      for (ServerPlayer p : level.players()) {
         if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < r * r) {
            ServerPlayNetworking.send(p, packet);
         }
      }
   }

   public record ConsoleKey(ResourceKey<Level> dimension, BlockPos pos) {
   }

   public record Seat(ConsoleSessionManager.ConsoleKey console, int port) {
   }
}
