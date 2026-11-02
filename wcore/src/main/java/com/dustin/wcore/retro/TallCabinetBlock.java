package com.dustin.wcore.retro;

import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TallCabinetBlock extends RetroConsoleBlock {
   public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
   private static final VoxelShape LOWER = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);
   private static final VoxelShape UPPER = Block.box(1.0, 0.0, 1.0, 15.0, 15.5, 15.0);

   public TallCabinetBlock(Properties properties, String displayName, Set<String> systems, Supplier<BlockEntityType<RetroConsoleBlockEntity>> type) {
      super(properties, displayName, systems, true, type, LOWER, LOWER);
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
      return state.getValue(HALF) == DoubleBlockHalf.UPPER ? UPPER : LOWER;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return state.getValue(HALF) == DoubleBlockHalf.LOWER ? super.newBlockEntity(pos, state) : null;
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return state.getValue(HALF) == DoubleBlockHalf.LOWER ? super.getTicker(level, state, type) : null;
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      return state.getValue(HALF) == DoubleBlockHalf.UPPER
         ? super.useItemOn(stack, level.getBlockState(pos.below()), level, pos.below(), player, hand, hit)
         : super.useItemOn(stack, state, level, pos, player, hand, hit);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      return state.getValue(HALF) == DoubleBlockHalf.UPPER
         ? super.useWithoutItem(level.getBlockState(pos.below()), level, pos.below(), player, hit)
         : super.useWithoutItem(state, level, pos, player, hit);
   }
}
