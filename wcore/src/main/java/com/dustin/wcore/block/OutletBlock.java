package com.dustin.wcore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class OutletBlock extends Block {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final IntegerProperty POWER = BlockStateProperties.POWER;
   private static final VoxelShape N = Block.box(5.0, 4.0, 15.0, 11.0, 12.0, 16.0);
   private static final VoxelShape S = Block.box(5.0, 4.0, 0.0, 11.0, 12.0, 1.0);
   private static final VoxelShape E = Block.box(0.0, 4.0, 5.0, 1.0, 12.0, 11.0);
   private static final VoxelShape W = Block.box(15.0, 4.0, 5.0, 16.0, 12.0, 11.0);

   public OutletBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(POWERED, false))
            .setValue(POWER, 0)
      );
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, POWERED, POWER});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Direction face = ctx.getClickedFace();
      Direction facing = face.getAxis().isHorizontal() ? face : ctx.getHorizontalDirection().getOpposite();
      int power = strength(ctx.getLevel(), ctx.getClickedPos(), facing);
      return (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(FACING, facing)).setValue(POWERED, power > 0)).setValue(POWER, power);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return switch ((Direction)state.getValue(FACING)) {
         case SOUTH -> S;
         case EAST -> E;
         case WEST -> W;
         default -> N;
      };
   }

   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean moved) {
      refresh(level, pos);
   }

   private static boolean live(Level level, BlockPos pos, Direction facing) {
      return strength(level, pos, facing) > 0;
   }

   public static int strength(Level level, BlockPos pos, Direction facing) {
      BlockPos support = pos.relative(facing.getOpposite());
      return Math.max(level.getBestNeighborSignal(pos), level.getBestNeighborSignal(support));
   }

   public static int power(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.getBlock() instanceof OutletBlock ? (Integer)state.getValue(POWER) : 0;
   }

   public static boolean isLive(Level level, BlockPos pos) {
      return refresh(level, pos);
   }

   public static boolean isLiveClientSafe(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (!(state.getBlock() instanceof OutletBlock)) {
         return false;
      } else {
         return level.isClientSide() ? (Boolean)state.getValue(POWERED) : refresh(level, pos);
      }
   }

   public static boolean refresh(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (!(state.getBlock() instanceof OutletBlock)) {
         return false;
      }

      if (level.isClientSide()) {
         return (Boolean)state.getValue(POWERED);
      }

      int power = strength(level, pos, (Direction)state.getValue(FACING));
      boolean live = power > 0;
      if (live != (Boolean)state.getValue(POWERED) || power != (Integer)state.getValue(POWER)) {
         level.setBlock(pos, (BlockState)((BlockState)state.setValue(POWERED, live)).setValue(POWER, power), 3);
      }

      return live;
   }
}
