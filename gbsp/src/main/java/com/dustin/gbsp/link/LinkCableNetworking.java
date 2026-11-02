package com.dustin.gbsp.link;

import com.dustin.gbsp.GameBoySP;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;

public final class LinkCableNetworking {
   public static final int MAX_DATA = 4096;

   private LinkCableNetworking() {
   }

   public static void register() {
      PayloadTypeRegistry.serverboundPlay().register(LinkCableNetworking.LinkCableDataPayload.TYPE, LinkCableNetworking.LinkCableDataPayload.CODEC);
      PayloadTypeRegistry.clientboundPlay().register(LinkCableNetworking.LinkCableDataPayload.TYPE, LinkCableNetworking.LinkCableDataPayload.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(LinkCableNetworking.LinkCableConnectPayload.TYPE, LinkCableNetworking.LinkCableConnectPayload.CODEC);
      PayloadTypeRegistry.clientboundPlay().register(LinkCableNetworking.LinkCableConnectPayload.TYPE, LinkCableNetworking.LinkCableConnectPayload.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(
         LinkCableNetworking.LinkCableDataPayload.TYPE, (payload, context) -> ServerLinkCableManager.relay(context.player(), payload.data())
      );
      ServerPlayNetworking.registerGlobalReceiver(LinkCableNetworking.LinkCableConnectPayload.TYPE, (payload, context) -> {
         if (!payload.connected()) {
            ServerLinkCableManager.unlink(context.player(), "cable unplugged");
         } else if (payload.partner() != null) {
            ServerPlayer target = context.player().level().getServer().getPlayerList().getPlayer(payload.partner());
            if (target != null) {
               ServerLinkCableManager.link(context.player(), target);
            }
         }
      });
      ServerLinkCableManager.registerEvents();
   }

   public record LinkCableConnectPayload(boolean connected, int role, UUID partner, String partnerName) implements CustomPacketPayload {
      public static final Type<LinkCableNetworking.LinkCableConnectPayload> TYPE = new Type(GameBoySP.id("link_connect"));
      public static final StreamCodec<RegistryFriendlyByteBuf, LinkCableNetworking.LinkCableConnectPayload> CODEC = StreamCodec.of((buf, p) -> {
         buf.writeBoolean(p.connected);
         buf.writeByte(p.role);
         buf.writeBoolean(p.partner != null);
         if (p.partner != null) {
            buf.writeUUID(p.partner);
         }

         buf.writeUtf(p.partnerName == null ? "" : p.partnerName, 64);
      }, buf -> {
         boolean c = buf.readBoolean();
         int role = buf.readByte();
         UUID partner = buf.readBoolean() ? buf.readUUID() : null;
         return new LinkCableNetworking.LinkCableConnectPayload(c, role, partner, buf.readUtf(64));
      });

      public static LinkCableNetworking.LinkCableConnectPayload unplug() {
         return new LinkCableNetworking.LinkCableConnectPayload(false, -1, null, "");
      }

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public record LinkCableDataPayload(byte[] data) implements CustomPacketPayload {
      public static final Type<LinkCableNetworking.LinkCableDataPayload> TYPE = new Type(GameBoySP.id("link_data"));
      public static final StreamCodec<RegistryFriendlyByteBuf, LinkCableNetworking.LinkCableDataPayload> CODEC = StreamCodec.of(
         (buf, p) -> buf.writeByteArray(p.data), buf -> new LinkCableNetworking.LinkCableDataPayload(buf.readByteArray(4096))
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }
}
