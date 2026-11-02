package com.dustin.arcadeplus.client;

import net.minecraft.core.BlockPos;

/** Where the local player is aiming the cue (the table renderer draws the cue there while the shot screen is open). */
final class PoolAim {
   private PoolAim() {
   }

   static BlockPos pos;
   static double angle;
   static double power;
}
