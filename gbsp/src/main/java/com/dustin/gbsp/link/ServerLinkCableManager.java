package com.dustin.gbsp.link;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerLinkCableManager {
   public static final double RANGE = 4.0;
   private static final Map<UUID, ServerLinkCableManager.End> LINKS = new HashMap<>();
   private static int ticks;

   private ServerLinkCableManager() {
   }

   static void registerEvents() {
      ServerTickEvents.END_SERVER_TICK.register(ServerLinkCableManager::tick);
      ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> unlink(handler.player, "your partner left"));
   }

   public static boolean isLinked(ServerPlayer player) {
      return LINKS.containsKey(player.getUUID());
   }

   public static boolean link(ServerPlayer a, ServerPlayer b) {
      if (a == b || a.level() != b.level()) {
         return false;
      } else if (a.distanceTo(b) > 4.0) {
         a.sendOverlayMessage(Component.literal("Too far: stand within 4 blocks to link").withStyle(ChatFormatting.GOLD));
         return false;
      } else {
         unlink(a, "");
         unlink(b, "");
         LINKS.put(a.getUUID(), new ServerLinkCableManager.End(b.getUUID(), 0));
         LINKS.put(b.getUUID(), new ServerLinkCableManager.End(a.getUUID(), 1));
         ServerPlayNetworking.send(a, new LinkCableNetworking.LinkCableConnectPayload(true, 0, b.getUUID(), b.getGameProfile().name()));
         ServerPlayNetworking.send(b, new LinkCableNetworking.LinkCableConnectPayload(true, 1, a.getUUID(), a.getGameProfile().name()));
         a.sendSystemMessage(Component.literal("Link cable connected to " + b.getGameProfile().name() + " (you're player 1)").withStyle(ChatFormatting.AQUA));
         b.sendSystemMessage(Component.literal("Link cable connected to " + a.getGameProfile().name() + " (you're player 2)").withStyle(ChatFormatting.AQUA));
         return true;
      }
   }

   public static void unlink(ServerPlayer player, String reason) {
      ServerLinkCableManager.End end = LINKS.remove(player.getUUID());
      if (end != null) {
         LINKS.remove(end.partner());
         ServerPlayNetworking.send(player, LinkCableNetworking.LinkCableConnectPayload.unplug());
         ServerPlayer partner = player.level().getServer() == null ? null : player.level().getServer().getPlayerList().getPlayer(end.partner());
         if (partner != null) {
            ServerPlayNetworking.send(partner, LinkCableNetworking.LinkCableConnectPayload.unplug());
         }

         if (!reason.isEmpty()) {
            Component msg = Component.literal("Link cable unplugged (" + reason + ")").withStyle(ChatFormatting.GRAY);
            player.sendSystemMessage(msg);
            if (partner != null) {
               partner.sendSystemMessage(msg);
            }
         }
      }
   }

   public static void relay(ServerPlayer sender, byte[] data) {
      ServerLinkCableManager.End end = LINKS.get(sender.getUUID());
      if (end != null && data != null && data.length != 0 && data.length <= 4096) {
         ServerPlayer partner = sender.level().getServer().getPlayerList().getPlayer(end.partner());
         if (partner != null) {
            ServerPlayNetworking.send(partner, new LinkCableNetworking.LinkCableDataPayload(data));
         }
      }
   }

   private static void tick(MinecraftServer server) {
      if (++ticks % 10 == 0 && !LINKS.isEmpty()) {
         for (UUID id : LINKS.keySet().toArray(new UUID[0])) {
            ServerLinkCableManager.End end = LINKS.get(id);
            if (end != null && end.role() == 0) {
               ServerPlayer a = server.getPlayerList().getPlayer(id);
               ServerPlayer b = server.getPlayerList().getPlayer(end.partner());
               if (a != null && b != null) {
                  if (a.level() != b.level()) {
                     unlink(a, "different dimension");
                  } else if (a.distanceTo(b) > 4.0) {
                     unlink(a, "walked too far apart");
                  }
               } else {
                  LINKS.remove(id);
                  LINKS.remove(end.partner());
                  if (a != null) {
                     ServerPlayNetworking.send(a, LinkCableNetworking.LinkCableConnectPayload.unplug());
                  }

                  if (b != null) {
                     ServerPlayNetworking.send(b, LinkCableNetworking.LinkCableConnectPayload.unplug());
                  }
               }
            }
         }
      }
   }

   private record End(UUID partner, int role) {
   }
}
