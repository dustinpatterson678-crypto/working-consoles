package com.dustin.wcore.net;

import com.dustin.wcore.WorkingConsolesCore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record SeatActionPacket(BlockPos pos, boolean join) implements CustomPacketPayload {
   public static final Type<SeatActionPacket> TYPE = new Type(WorkingConsolesCore.id("console_seat_action"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SeatActionPacket> CODEC = StreamCodec.of((buf, p) -> {
      buf.writeBlockPos(p.pos);
      buf.writeBoolean(p.join);
   }, buf -> new SeatActionPacket(buf.readBlockPos(), buf.readBoolean()));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
