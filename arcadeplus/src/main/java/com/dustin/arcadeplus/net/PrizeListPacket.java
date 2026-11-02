package com.dustin.arcadeplus.net;

import com.dustin.arcadeplus.ArcadePlusMod;
import com.dustin.arcadeplus.Prizes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Server -> client: the booth's current price list (from the config file); the client opens the booth screen. */
public record PrizeListPacket(BlockPos pos, List<String> ids, List<Integer> counts, List<Integer> costs) implements CustomPacketPayload {
   public static final CustomPacketPayload.Type<PrizeListPacket> TYPE = new CustomPacketPayload.Type<>(ArcadePlusMod.id("prize_list"));
   public static final StreamCodec<RegistryFriendlyByteBuf, PrizeListPacket> CODEC = StreamCodec.of((buf, p) -> {
      buf.writeBlockPos(p.pos);
      buf.writeVarInt(p.ids.size());
      for (int i = 0; i < p.ids.size(); i++) {
         buf.writeUtf(p.ids.get(i));
         buf.writeVarInt(p.counts.get(i));
         buf.writeVarInt(p.costs.get(i));
      }
   }, buf -> {
      BlockPos pos = buf.readBlockPos();
      int n = buf.readVarInt();
      List<String> ids = new ArrayList<>();
      List<Integer> counts = new ArrayList<>(), costs = new ArrayList<>();
      for (int i = 0; i < n; i++) {
         ids.add(buf.readUtf());
         counts.add(buf.readVarInt());
         costs.add(buf.readVarInt());
      }
      return new PrizeListPacket(pos, ids, counts, costs);
   });

   @Override
   public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void send(ServerPlayer player, BlockPos pos) {
      List<String> ids = new ArrayList<>();
      List<Integer> counts = new ArrayList<>(), costs = new ArrayList<>();
      for (Prizes.Prize p : Prizes.reload()) {
         ids.add(p.id());
         counts.add(p.count());
         costs.add(p.cost());
      }
      net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new PrizeListPacket(pos, ids, counts, costs));
   }
}
