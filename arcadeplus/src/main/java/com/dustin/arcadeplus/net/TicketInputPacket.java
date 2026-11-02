package com.dustin.arcadeplus.net;

import com.dustin.arcadeplus.ArcadePlusMod;
import com.dustin.arcadeplus.TicketGameBlockEntity;
import com.dustin.arcadeplus.TicketGameLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Client -> server for the hands-on ticket games: held direction keys, plus a throw (aim/power) or a claw drop. */
public record TicketInputPacket(BlockPos pos, int flags, int aim, int power) implements CustomPacketPayload {
   public static final int LEFT = 1, RIGHT = 2, UP = 4, DOWN = 8, ACTION = 16;
   public static final CustomPacketPayload.Type<TicketInputPacket> TYPE = new CustomPacketPayload.Type<>(ArcadePlusMod.id("ticket_input"));
   public static final StreamCodec<RegistryFriendlyByteBuf, TicketInputPacket> CODEC = StreamCodec.of((buf, p) -> {
      buf.writeBlockPos(p.pos);
      buf.writeByte(p.flags);
      buf.writeShort(p.aim);
      buf.writeShort(p.power);
   }, buf -> new TicketInputPacket(buf.readBlockPos(), buf.readByte(), buf.readShort(), buf.readShort()));

   @Override
   public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handle(TicketInputPacket p, ServerPlayer player) {
      if (player.distanceToSqr(Vec3.atCenterOf(p.pos)) < 8 * 8 && player.level().getBlockEntity(p.pos) instanceof TicketGameBlockEntity be) {
         be.input = p.flags & 15;
         if ((p.flags & ACTION) != 0) {
            TicketGameLogic.action(player.level(), p.pos, be, player, Math.max(-1000, Math.min(1000, p.aim)) / 1000.0, Math.max(0, Math.min(1000, p.power)) / 1000.0);
         }
      }
   }
}
