package com.dustin.wcore.net;

import com.dustin.wcore.WorkingConsolesCore;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public record ConsoleInputPacket(BlockPos pos, int port, int buttons, short lx, short ly, short rx, short ry) implements CustomPacketPayload {
   public static final Type<ConsoleInputPacket> TYPE = new Type(WorkingConsolesCore.id("console_input"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ConsoleInputPacket> CODEC = StreamCodec.of((buf, p) -> {
      buf.writeBlockPos(p.pos);
      buf.writeByte(p.port);
      buf.writeVarInt(p.buttons);
      buf.writeShort(p.lx);
      buf.writeShort(p.ly);
      buf.writeShort(p.rx);
      buf.writeShort(p.ry);
   }, buf -> new ConsoleInputPacket(buf.readBlockPos(), buf.readByte(), buf.readVarInt(), buf.readShort(), buf.readShort(), buf.readShort(), buf.readShort()));
   public static final double RELAY_RANGE = 64.0;

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void register() {
      PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
      PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
      PayloadTypeRegistry.serverboundPlay().register(SeatActionPacket.TYPE, SeatActionPacket.CODEC);
      PayloadTypeRegistry.clientboundPlay().register(SeatSyncPacket.TYPE, SeatSyncPacket.CODEC);
      StreamRelay.register();
      ServerPlayNetworking.registerGlobalReceiver(TYPE, (packet, context) -> receive(context.player(), packet));
      ServerPlayNetworking.registerGlobalReceiver(SeatActionPacket.TYPE, (packet, context) -> {
         ServerPlayer player = context.player();
         if (packet.join()) {
            ConsoleSessionManager.join(player, packet.pos(), false);
         } else {
            ConsoleSessionManager.leave(player, true);
         }
      });
   }

   private static void receive(ServerPlayer sender, ConsoleInputPacket packet) {
      if (packet.port >= 0 && packet.port <= 1) {
         if (ConsoleSessionManager.mayDrive(sender, packet.pos, packet.port)) {
            ServerLevel level = sender.level();
            double cx = packet.pos.getX() + 0.5;
            double cy = packet.pos.getY() + 0.5;
            double cz = packet.pos.getZ() + 0.5;

            for (ServerPlayer p : level.players()) {
               if (p != sender && p.distanceToSqr(cx, cy, cz) < 4096.0) {
                  ServerPlayNetworking.send(p, packet);
               }
            }
         }
      }
   }
}
