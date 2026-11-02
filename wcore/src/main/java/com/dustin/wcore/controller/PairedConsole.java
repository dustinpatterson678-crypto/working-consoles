package com.dustin.wcore.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** The console (and port on it) a controller stack is paired with; stored as the wcore:paired_console component. */
public record PairedConsole(ResourceKey<Level> dim, BlockPos pos, int port) {
   public static final Codec<PairedConsole> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dim").forGetter(PairedConsole::dim),
            BlockPos.CODEC.fieldOf("pos").forGetter(PairedConsole::pos),
            Codec.INT.fieldOf("port").forGetter(PairedConsole::port)
         )
         .apply(i, PairedConsole::new)
   );
   public static final StreamCodec<ByteBuf, PairedConsole> STREAM_CODEC = StreamCodec.composite(
      ResourceKey.streamCodec(Registries.DIMENSION),
      PairedConsole::dim,
      BlockPos.STREAM_CODEC,
      PairedConsole::pos,
      ByteBufCodecs.VAR_INT,
      PairedConsole::port,
      PairedConsole::new
   );
}
