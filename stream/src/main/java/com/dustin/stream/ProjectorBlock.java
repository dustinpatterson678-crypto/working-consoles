package com.dustin.stream;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Projects its browser onto a cinema screen in front of it. Sizes: Home Theater, Cinema, Drive-In, IMAX. */
public class ProjectorBlock extends MediaBlock {
   public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 3);
   public static final String[] SIZE_NAMES = {"Home Theater (4 blocks wide)", "Cinema (8 wide)", "Drive-In (16 wide)", "IMAX (24 wide)"};
   public static final double[] SIZE_WIDTH = {4.0, 8.0, 16.0, 24.0};

   public ProjectorBlock(Properties properties) {
      super(properties, Kind.PROJECTOR);
      this.registerDefaultState(this.defaultBlockState().setValue(SIZE, 1));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(SIZE);
   }
}
