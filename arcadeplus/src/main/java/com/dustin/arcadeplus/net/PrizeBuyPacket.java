package com.dustin.arcadeplus.net;

import com.dustin.arcadeplus.ArcadePlusMod;
import com.dustin.arcadeplus.Prizes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Client -> server: buy prize #index at the booth at pos. */
public record PrizeBuyPacket(BlockPos pos, int index) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<PrizeBuyPacket> TYPE = new CustomPacketPayload.Type<>(ArcadePlusMod.id("prize_buy"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PrizeBuyPacket> CODEC = StreamCodec.of((buf, p) -> {
      buf.writeBlockPos(p.pos);
      buf.writeVarInt(p.index);
   }, buf -> new PrizeBuyPacket(buf.readBlockPos(), buf.readVarInt()));

   @Override
   public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handle(PrizeBuyPacket p, ServerPlayer player) {
      if (player.distanceToSqr(Vec3.atCenterOf(p.pos)) > 8 * 8 || !com.dustin.arcadeplus.ArcadeEconomy.isBooth(player.level().getBlockState(p.pos).getBlock())) {
         net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new PrizeResultPacket("!Step up to the booth to buy"));
         return;
      }
      String msg = Prizes.buy(player, p.index);
      ArcadePlusMod.LOGGER.info("Prize booth: {} picked #{} -> {}", player.getName().getString(), p.index, msg);
      if (!msg.startsWith("!")) {
         player.level().playSound(null, p.pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8F, 1.4F);
         player.level().playSound(null, p.pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5F, 1.6F);
      } else {
         player.level().playSound(null, p.pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.6F, 0.6F);
      }
      net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new PrizeResultPacket(msg));
   }
}
