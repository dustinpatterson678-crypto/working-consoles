package com.dustin.wcore.net;

import com.dustin.wcore.WorkingConsolesCore;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SeatSyncPacket(BlockPos pos, UUID player1, UUID player2) implements CustomPacketPayload {
   public static final Type<SeatSyncPacket> TYPE = new Type(WorkingConsolesCore.id("console_seats"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SeatSyncPacket> CODEC = StreamCodec.of((buf, p) -> {
      buf.writeBlockPos(p.pos);
      writeUuid(buf, p.player1);
      writeUuid(buf, p.player2);
   }, buf -> new SeatSyncPacket(buf.readBlockPos(), readUuid(buf), readUuid(buf)));

   private static void writeUuid(RegistryFriendlyByteBuf buf, UUID id) {
      buf.writeBoolean(id != null);
      if (id != null) {
         buf.writeUUID(id);
      }
   }

   private static UUID readUuid(RegistryFriendlyByteBuf buf) {
      return buf.readBoolean() ? buf.readUUID() : null;
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
