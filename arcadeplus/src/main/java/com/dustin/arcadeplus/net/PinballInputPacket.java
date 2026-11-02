package com.dustin.arcadeplus.net;

import com.dustin.arcadeplus.ArcadePlusMod;
import com.dustin.arcadeplus.PinballBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Client -> server: which flipper / plunger buttons are held right now. */
public record PinballInputPacket(BlockPos pos, int flags) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<PinballInputPacket> TYPE = new CustomPacketPayload.Type<>(ArcadePlusMod.id("pinball_input"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PinballInputPacket> CODEC = StreamCodec.of((buf, p) -> {
      buf.writeBlockPos(p.pos);
      buf.writeByte(p.flags);
   }, buf -> new PinballInputPacket(buf.readBlockPos(), buf.readByte()));

   @Override
   public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handle(PinballInputPacket p, ServerPlayer player) {
      if (player.distanceToSqr(Vec3.atCenterOf(p.pos)) < 8 * 8 && player.level().getBlockEntity(p.pos) instanceof PinballBlockEntity be) {
         be.input = p.flags & 7;
      }
   }
}
