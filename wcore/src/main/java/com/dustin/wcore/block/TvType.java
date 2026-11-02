package com.dustin.wcore.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public enum TvType {
   FLAT("tv", "Flat TV", 28.0, 21.0, 15.0, 1.06, Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 12.0)),
   CRT_SMALL("crt_small", "Small CRT TV", 9.0, 7.0, 6.0, 6.15, Block.box(2.0, 0.0, 2.0, 14.0, 11.0, 14.0)),
   CRT_LARGE("crt_large", "Big CRT TV", 13.0, 10.0, 8.0, 8.12, Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0)),
   REAR_PROJECTION("rear_projection", "Rear-Projection TV", 26.0, 19.5, 20.0, 7.15, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)),
   PROJECTOR("projector", "Projector", 0.0, 0.0, 0.0, 0.0, Block.box(4.0, 0.0, 4.0, 12.0, 3.5, 12.0)),
   CEILING_PROJECTOR("ceiling_projector", "Ceiling Projector", 0.0, 0.0, 0.0, 0.0, Block.box(4.0, 8.0, 4.0, 12.0, 16.0, 12.0));

   public final String model;
   public final String name;
   public final double w;
   public final double h;
   public final double y;
   public final double front;
   private final VoxelShape north;

   TvType(String model, String name, double w, double h, double y, double front, VoxelShape north) {
      this.model = model;
      this.name = name;
      this.w = w;
      this.h = h;
      this.y = y;
      this.front = front;
      this.north = north;
   }

   public boolean projects() {
      return this == PROJECTOR || this == CEILING_PROJECTOR;
   }

   public boolean hanging() {
      return this == CEILING_PROJECTOR;
   }

   /**
    * True for the two big screens (flat TV 30x26 px, rear-projection 28x32 px).
    * These are placed as 2x2 multiblocks (MultiTvBlock) at their authored size
    * - a v5 scale fix: a 28" CRT-era set has to physically dominate a room, so
    * it gets the footprint to match instead of being shrunk into one block.
    */
   public boolean quad() {
      return this == FLAT || this == REAR_PROJECTION;
   }

   public VoxelShape shape(Direction facing) {
      AABB b = this.north.bounds();
      double x0 = b.minX * 16.0;
      double x1 = b.maxX * 16.0;
      double y0 = b.minY * 16.0;
      double y1 = b.maxY * 16.0;
      double z0 = b.minZ * 16.0;
      double z1 = b.maxZ * 16.0;

      return switch (facing) {
         case SOUTH -> Block.box(16.0 - x1, y0, 16.0 - z1, 16.0 - x0, y1, 16.0 - z0);
         case EAST -> Block.box(16.0 - z1, y0, x0, 16.0 - z0, y1, x1);
         case WEST -> Block.box(z0, y0, 16.0 - x1, z1, y1, 16.0 - x0);
         default -> this.north;
      };
   }
}
