package com.dustin.arcadebuild;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** Invisible solid part of the building: the walls/floor/roof you see are the building model drawn over these. */
public class ShellBlock extends Block {
   public ShellBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }
}
