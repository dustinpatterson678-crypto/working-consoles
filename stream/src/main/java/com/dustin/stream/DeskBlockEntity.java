package com.dustin.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Only exists so the keyboard and mouse get a renderer that animates with your real typing and mouse. */
public class DeskBlockEntity extends BlockEntity {
   public DeskBlockEntity(BlockPos pos, BlockState state) {
      super(StreamMod.DESK_BE, pos, state);
   }
}
