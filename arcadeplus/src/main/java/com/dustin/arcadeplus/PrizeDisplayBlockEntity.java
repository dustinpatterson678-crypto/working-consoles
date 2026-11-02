package com.dustin.arcadeplus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Lets the prize counter and prize wall draw the real prizes. */
public class PrizeDisplayBlockEntity extends BlockEntity {
   public PrizeDisplayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }
}
