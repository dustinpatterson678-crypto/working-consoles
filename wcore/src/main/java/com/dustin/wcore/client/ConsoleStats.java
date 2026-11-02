package com.dustin.wcore.client;

import com.dustin.wcore.emu.EmuThread;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;

public final class ConsoleStats {
   private static final Map<BlockPos, ConsoleStats.Entry> RUNNING = new ConcurrentHashMap<>();

   private ConsoleStats() {
   }

   public static void put(BlockPos console, ConsoleStats.Entry e) {
      RUNNING.put(console.immutable(), e);
   }

   public static void remove(BlockPos console) {
      if (console != null) {
         RUNNING.remove(console);
      }
   }

   public static ConsoleStats.Entry get(BlockPos console) {
      return console == null ? null : RUNNING.get(console);
   }

   public record Entry(String console, EmuThread.Stats emu, String boost) {
   }
}
