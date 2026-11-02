package com.dustin.arcadeplus.net;

import com.dustin.arcadeplus.ArcadePlusMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: what happened at the prize booth (a leading '!' means it failed). */
public record PrizeResultPacket(String message) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<PrizeResultPacket> TYPE = new CustomPacketPayload.Type<>(ArcadePlusMod.id("prize_result"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PrizeResultPacket> CODEC = StreamCodec.of((buf, p) -> buf.writeUtf(p.message), buf -> new PrizeResultPacket(buf.readUtf()));
   public static java.util.function.Consumer<String> CLIENT = m -> {};

   @Override
   public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
