package com.dustin.wcore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Two-blocks-tall furniture (entertainment center, media cabinet, console
 * rack). Same double-block plumbing as the arcade TallCabinetBlock: the item
 * places the lower half, the block entity + GeckoLib model live on the lower
 * half and the model simply spans both cells.
 */
public class TallDecorBlock extends DecorBlock {
   public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

   public TallDecorBlock(Properties properties, Identifier model, VoxelShape lower, VoxelShape upper, boolean animated, java.util.List<DecorBlock.DecorSlot> slots) {
      super(properties, model, lower, upper, animated, slots);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{HALF});
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      BlockPos pos = ctx.getClickedPos();
      Level level = ctx.getLevel();
      return pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(ctx)
         ? (BlockState)super.getStateForPlacement(ctx).setValue(HALF, DoubleBlockHalf.LOWER)
         : null;
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      level.setBlock(pos.above(), (BlockState)state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
   }

   protected BlockState updateShape(
      BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos nPos, BlockState nState, RandomSource random
   ) {
      DoubleBlockHalf half = (DoubleBlockHalf)state.getValue(HALF);
      boolean towardOther = dir == (half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
      return !towardOther || nState.is(this) && nState.getValue(HALF) != half
         ? super.updateShape(state, level, ticks, pos, dir, nPos, nState, random)
         : Blocks.AIR.defaultBlockState();
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide() && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
         BlockPos below = pos.below();
         if (level.getBlockState(below).is(this)) {
            level.setBlock(below, Blocks.AIR.defaultBlockState(), 35);
         }
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.shapeFor(state, state.getValue(HALF) == DoubleBlockHalf.UPPER);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return state.getValue(HALF) == DoubleBlockHalf.LOWER ? super.newBlockEntity(pos, state) : null;
   }
}
