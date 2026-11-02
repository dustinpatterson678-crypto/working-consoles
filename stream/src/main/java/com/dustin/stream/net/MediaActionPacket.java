package com.dustin.stream.net;

import com.dustin.stream.MediaBlock;
import com.dustin.stream.MediaBlockEntity;
import com.dustin.stream.ProjectorBlock;
import com.dustin.stream.StreamMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Client -> server: change the projector's screen size from the browser controls. */
public record MediaActionPacket(BlockPos pos, int action) implements CustomPacketPayload {
   public static final int SIZE_UP = 1;
   public static final int SIZE_DOWN = 2;
   public static final int INPUT_NEXT = 3;
   public static final CustomPacketPayload.Type<MediaActionPacket> TYPE = new CustomPacketPayload.Type<>(StreamMod.id("media_action"));
   public static final StreamCodec<RegistryFriendlyByteBuf, MediaActionPacket> CODEC = StreamCodec.of((buf, p) -> {
      buf.writeBlockPos(p.pos);
      buf.writeVarInt(p.action);
   }, buf -> new MediaActionPacket(buf.readBlockPos(), buf.readVarInt()));

   @Override
   public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handle(MediaActionPacket p, ServerPlayer player) {
      Level level = player.level();
      if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p.pos)) > 64.0 * 64.0 || !(level.getBlockEntity(p.pos) instanceof MediaBlockEntity)) {
         return;
      }
      if (p.action == INPUT_NEXT && level.getBlockEntity(p.pos) instanceof MediaBlockEntity media) {
         int in = media.nextInput();
         BlockPos con = media.hdmi(in);
         String name = con != null && level.getBlockEntity(con) instanceof com.dustin.wcore.block.ConsoleBlockEntity c ? " (" + c.consoleName() + ")" : "";
         MediaBlock.tell(player, "Input: " + (in == 0 ? "Smart TV" : "HDMI " + in + name), ChatFormatting.AQUA);
         return;
      }
      BlockState state = level.getBlockState(p.pos);
      if (state.getBlock() instanceof ProjectorBlock && (p.action == SIZE_UP || p.action == SIZE_DOWN)) {
         int size = Math.floorMod(state.getValue(ProjectorBlock.SIZE) + (p.action == SIZE_UP ? 1 : -1), 4);
         level.setBlock(p.pos, state.setValue(ProjectorBlock.SIZE, size), 3);
         MediaBlock.tell(player, "Screen: " + ProjectorBlock.SIZE_NAMES[size], ChatFormatting.AQUA);
      }
   }
}
