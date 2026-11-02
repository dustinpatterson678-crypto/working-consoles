package com.dustin.wcore.client;

import com.dustin.wcore.WorkingConsolesCore;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class TvScreens {
   private static final Map<BlockPos, TvScreens.Screen> SCREENS = new ConcurrentHashMap<>();
   private static final Map<BlockPos, Runnable> PUMPS = new ConcurrentHashMap<>();

   /** Pictures that arrive upside down (drawn straight into the texture by OpenGL, which starts at the bottom row). */
   private static final Set<Identifier> FLIPPED = ConcurrentHashMap.newKeySet();

   public static void markFlipped(Identifier texture) {
      if (texture != null) {
         FLIPPED.add(texture);
      }
   }

   public static boolean isFlipped(Identifier texture) {
      return texture != null && FLIPPED.contains(texture);
   }

   private TvScreens() {
   }

   public static void show(BlockPos console, TvScreens.Screen screen) {
      SCREENS.put(console.immutable(), screen);
   }

   public static void clear(BlockPos console) {
      WorkingConsolesCore.LOGGER.info("MASTER_LOG: TVSCREENS_CLEAR pos={}", console);
      SCREENS.remove(console);
   }

   /** The picture a console is showing right now, without running its pump. Null when the console isn't sending one (off / no signal). */
   public static TvScreens.Screen peek(BlockPos console) {
      return console == null ? null : SCREENS.get(console);
   }

   public static boolean hasPump(BlockPos console) {
      return console != null && PUMPS.containsKey(console);
   }

   public static TvScreens.Screen get(BlockPos console) {
      if (console == null) {
         return null;
      }

      Runnable pump = PUMPS.get(console);
      // pumps upload pictures with OpenGL: running one on the server thread aborts the JVM
      if (pump != null && net.minecraft.client.Minecraft.getInstance().isSameThread()) {
         pump.run();
      }

      TvScreens.Screen screen = SCREENS.get(console);
      return screen;
   }

   public static void setPump(BlockPos console, Runnable pump) {
      WorkingConsolesCore.LOGGER.info("MASTER_LOG: TVSCREENS_SETPUMP pos={} pump={}", console, pump != null);
      if (pump == null) {
         PUMPS.remove(console);
      } else {
         PUMPS.put(console.immutable(), pump);
      }
   }

   public record Screen(Identifier texture, int width, int height, int texWidth, int texHeight, int serial) {
   }
}
