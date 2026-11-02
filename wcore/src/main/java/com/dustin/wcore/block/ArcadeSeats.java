package com.dustin.wcore.block;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

/**
 * Keeps the invisible armor-stand seats used by the arcade stool tidy:
 * sneaking stands the player up, and a seat whose stool block is gone (or that
 * nobody is using) is removed.
 */
public final class ArcadeSeats {
   private static final String SEAT_TAG = "wcore_seat";

   private ArcadeSeats() {
   }

   public static void tick(MinecraftServer server) {
      if (server.getTickCount() % 10 != 0) {
         return;
      }

      for (ServerLevel level : server.getAllLevels()) {
         for (ArmorStand stand : level.getEntities(EntityTypes.ARMOR_STAND, seat -> seat.entityTags().contains(SEAT_TAG))) {
            if (!(level.getBlockState(stand.blockPosition()).getBlock() instanceof DecorBlock)) {
               stand.ejectPassengers();
               stand.discard();
               continue;
            }

            if (stand.getPassengers().isEmpty()) {
               if (stand.tickCount > 10) {
                  stand.discard();
               }
            } else {
               for (Entity passenger : stand.getPassengers()) {
                  if (passenger instanceof Player player && player.isShiftKeyDown()) {
                     player.stopRiding();
                  }
               }
            }
         }
      }
   }
}
