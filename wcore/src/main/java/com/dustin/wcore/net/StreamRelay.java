package com.dustin.wcore.net;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.block.ConsoleBlockEntity;
import com.dustin.wcore.retro.RetroConsoleBlockEntity;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class StreamRelay {
   public static final double RANGE = 64.0;
   public static final int MAX_CHUNK = 30000;
   public static final int MAX_AUDIO = 16384;
   private static final long CLAIM_NANOS = 3000000000L;
   private static final Map<ConsoleSessionManager.ConsoleKey, StreamRelay.State> CONSOLES = new HashMap<>();
   private static int ticks;

   private StreamRelay() {
   }

   public static void register() {
      PayloadTypeRegistry.serverboundPlay().register(StreamRelay.Claim.TYPE, StreamRelay.Claim.CODEC);
      PayloadTypeRegistry.clientboundPlay().register(StreamRelay.Host.TYPE, StreamRelay.Host.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(StreamRelay.Frame.TYPE, StreamRelay.Frame.CODEC);
      PayloadTypeRegistry.clientboundPlay().register(StreamRelay.Frame.TYPE, StreamRelay.Frame.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(StreamRelay.Audio.TYPE, StreamRelay.Audio.CODEC);
      PayloadTypeRegistry.clientboundPlay().register(StreamRelay.Audio.TYPE, StreamRelay.Audio.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(StreamRelay.Claim.TYPE, (p, ctx) -> claim(ctx.player(), p));
      ServerPlayNetworking.registerGlobalReceiver(StreamRelay.Frame.TYPE, (p, ctx) -> {
         StreamRelay.State s = hostedBy(ctx.player(), p.pos());
         if (s != null && p.parts() >= 1 && p.parts() <= 8 && p.part() >= 0 && p.part() < p.parts()) {
            long sec = System.nanoTime() / 1000000000L;
            if (s.second != sec) {
               s.second = sec;
               s.framesThisSecond = 0;
            }

            if (p.part() != 0 || ++s.framesThisSecond <= 40) {
               relay(ctx.player(), p.pos(), p);
            }
         }
      });
      ServerPlayNetworking.registerGlobalReceiver(StreamRelay.Audio.TYPE, (p, ctx) -> {
         if (hostedBy(ctx.player(), p.pos()) != null && p.rate() >= 8000 && p.rate() <= 96000) {
            relay(ctx.player(), p.pos(), p);
         }
      });
   }

   private static ConsoleSessionManager.ConsoleKey key(ServerPlayer p, BlockPos pos) {
      return new ConsoleSessionManager.ConsoleKey(p.level().dimension(), pos.immutable());
   }

   private static StreamRelay.State hostedBy(ServerPlayer player, BlockPos pos) {
      StreamRelay.State s = CONSOLES.get(key(player, pos));
      return s != null && player.getUUID().equals(s.host) ? s : null;
   }

   private static void claim(ServerPlayer player, StreamRelay.Claim c) {
      BlockPos pos = c.pos();
      if (!(player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 4096.0)) {
         if (player.level().getBlockEntity(pos) instanceof ConsoleBlockEntity || player.level().getBlockEntity(pos) instanceof RetroConsoleBlockEntity) {
            ConsoleSessionManager.ConsoleKey k = key(player, pos);
            StreamRelay.State s = CONSOLES.computeIfAbsent(k, x -> new StreamRelay.State());
            if (c.canHost()) {
               s.claims.put(player.getUUID(), System.nanoTime());
            } else {
               s.claims.remove(player.getUUID());
            }

            if (s.host != null && (!s.host.equals(player.getUUID()) || c.canHost())) {
               send(player, k, s);
            } else {
               pick(player.level(), k, s);
            }
         }
      }
   }

   private static void pick(ServerLevel level, ConsoleSessionManager.ConsoleKey k, StreamRelay.State s) {
      long now = System.nanoTime();
      s.claims.values().removeIf(t -> now - t > 3000000000L);
      UUID before = s.host;
      if (s.host == null || !s.claims.containsKey(s.host)) {
         s.host = s.claims.isEmpty() ? null : s.claims.keySet().iterator().next();
      }

      int viewers = 0;
      BlockPos pos = k.pos();

      for (ServerPlayer p : level.players()) {
         if (!p.getUUID().equals(s.host) && p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 4096.0) {
            viewers++;
         }
      }

      if (before != s.host && (before == null || !before.equals(s.host)) || viewers != s.viewers) {
         s.viewers = viewers;
         broadcast(level, k, s);
      }
   }

   private static void send(ServerPlayer p, ConsoleSessionManager.ConsoleKey k, StreamRelay.State s) {
      ServerPlayNetworking.send(p, new StreamRelay.Host(k.pos(), s.host, Math.max(0, s.viewers)));
   }

   private static void broadcast(ServerLevel level, ConsoleSessionManager.ConsoleKey k, StreamRelay.State s) {
      BlockPos pos = k.pos();

      for (ServerPlayer p : level.players()) {
         if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 5184.0) {
            send(p, k, s);
         }
      }
   }

   private static void relay(ServerPlayer from, BlockPos pos, CustomPacketPayload packet) {
      double x = pos.getX() + 0.5;
      double y = pos.getY() + 0.5;
      double z = pos.getZ() + 0.5;

      for (ServerPlayer p : from.level().players()) {
         if (p != from && p.distanceToSqr(x, y, z) < 4096.0) {
            ServerPlayNetworking.send(p, packet);
         }
      }
   }

   public static void tick(MinecraftServer server) {
      if (++ticks % 10 == 0 && !CONSOLES.isEmpty()) {
         Iterator<Entry<ConsoleSessionManager.ConsoleKey, StreamRelay.State>> it = CONSOLES.entrySet().iterator();

         while (it.hasNext()) {
            Entry<ConsoleSessionManager.ConsoleKey, StreamRelay.State> e = it.next();
            ServerLevel level = server.getLevel(e.getKey().dimension());
            if (level == null) {
               it.remove();
            } else {
               pick(level, e.getKey(), e.getValue());
               if (e.getValue().host == null && e.getValue().claims.isEmpty()) {
                  it.remove();
               } else if (ticks % 100 == 0) {
                  broadcast(level, e.getKey(), e.getValue());
               }
            }
         }
      }
   }

   public static void disconnect(ServerPlayer player) {
      for (StreamRelay.State s : CONSOLES.values()) {
         s.claims.remove(player.getUUID());
      }
   }

   public record Audio(BlockPos pos, int rate, byte[] pcm) implements CustomPacketPayload {
      public static final Type<StreamRelay.Audio> TYPE = new Type(WorkingConsolesCore.id("stream_audio"));
      public static final StreamCodec<RegistryFriendlyByteBuf, StreamRelay.Audio> CODEC = StreamCodec.of((buf, p) -> {
         buf.writeBlockPos(p.pos);
         buf.writeVarInt(p.rate);
         buf.writeByteArray(p.pcm);
      }, buf -> new StreamRelay.Audio(buf.readBlockPos(), buf.readVarInt(), buf.readByteArray(16384)));

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public record Claim(BlockPos pos, boolean canHost) implements CustomPacketPayload {
      public static final Type<StreamRelay.Claim> TYPE = new Type(WorkingConsolesCore.id("stream_claim"));
      public static final StreamCodec<RegistryFriendlyByteBuf, StreamRelay.Claim> CODEC = StreamCodec.of((buf, p) -> {
         buf.writeBlockPos(p.pos);
         buf.writeBoolean(p.canHost);
      }, buf -> new StreamRelay.Claim(buf.readBlockPos(), buf.readBoolean()));

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public record Frame(BlockPos pos, int frame, int part, int parts, int width, int height, long time, byte[] data) implements CustomPacketPayload {
      public static final Type<StreamRelay.Frame> TYPE = new Type(WorkingConsolesCore.id("stream_frame"));
      public static final StreamCodec<RegistryFriendlyByteBuf, StreamRelay.Frame> CODEC = StreamCodec.of(
         (buf, p) -> {
            buf.writeBlockPos(p.pos);
            buf.writeVarInt(p.frame);
            buf.writeByte(p.part);
            buf.writeByte(p.parts);
            buf.writeShort(p.width);
            buf.writeShort(p.height);
            buf.writeLong(p.time);
            buf.writeByteArray(p.data);
         },
         buf -> new StreamRelay.Frame(
            buf.readBlockPos(), buf.readVarInt(), buf.readByte(), buf.readByte(), buf.readShort(), buf.readShort(), buf.readLong(), buf.readByteArray(30000)
         )
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public record Host(BlockPos pos, UUID host, int viewers) implements CustomPacketPayload {
      public static final Type<StreamRelay.Host> TYPE = new Type(WorkingConsolesCore.id("stream_host"));
      public static final StreamCodec<RegistryFriendlyByteBuf, StreamRelay.Host> CODEC = StreamCodec.of((buf, p) -> {
         buf.writeBlockPos(p.pos);
         buf.writeBoolean(p.host != null);
         if (p.host != null) {
            buf.writeUUID(p.host);
         }

         buf.writeVarInt(p.viewers);
      }, buf -> new StreamRelay.Host(buf.readBlockPos(), buf.readBoolean() ? buf.readUUID() : null, buf.readVarInt()));

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   private static final class State {
      UUID host;
      int viewers = -1;
      final Map<UUID, Long> claims = new LinkedHashMap<>();
      long second;
      int framesThisSecond;
   }
}
