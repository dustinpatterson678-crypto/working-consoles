package com.dustin.arcadebuild;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Only here so the giant power strip can be drawn as a real model (body, plugs, cords). */
public class PowerStripBlockEntity extends BlockEntity {
   public PowerStripBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }
}
