package com.dustin.stream;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;

/** Filled in by the client entrypoint so blocks never load client classes on a dedicated server. */
public final class StreamHooks {
   public static Consumer<BlockPos> OPEN = pos -> {};
   public static Consumer<MediaBlockEntity> TICK = be -> {};

   private StreamHooks() {
   }
}
