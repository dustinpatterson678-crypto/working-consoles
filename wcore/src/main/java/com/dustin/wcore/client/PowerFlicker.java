package com.dustin.wcore.client;

/** How a console's picture flickers on a bad power supply (PowerGrade.UNSTABLE). */
public final class PowerFlicker {
   private PowerFlicker() {
   }

   /**
    * True when the picture drops out right now. Never while a shader pack is on: its shadow pass and main pass read the
    * clock at different moments, disagree about the drop-out, and the screen strobes instead of flickering.
    */
   public static boolean dropped() {
      if (ShaderCompat.shadersActive()) {
         return false;
      }

      long t = System.currentTimeMillis() / 70L;
      return (t * 2654435761L >>> 7) % 5L == 0L;
   }

   /** The dimmed, wavering tint of an unstable picture. */
   public static int tint() {
      long t = System.currentTimeMillis() / 70L;
      return t % 3L == 0L ? -7566196 : -2960686;
   }
}
