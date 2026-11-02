package com.dustin.wcore.block;

import com.dustin.wcore.WorkingConsolesCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PowerMonitorBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape N = Block.box(0.0, 0.0, 12.0, 16.0, 16.0, 16.0);
   private static final VoxelShape S = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 4.0);
   private static final VoxelShape E = Block.box(0.0, 0.0, 0.0, 4.0, 16.0, 16.0);
   private static final VoxelShape W = Block.box(12.0, 0.0, 0.0, 16.0, 16.0, 16.0);

   public PowerMonitorBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      Direction face = ctx.getClickedFace();
      return (BlockState)this.defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face : ctx.getHorizontalDirection().getOpposite());
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return switch ((Direction)state.getValue(FACING)) {
         case SOUTH -> S;
         case EAST -> E;
         case WEST -> W;
         default -> N;
      };
   }

   protected boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
      return level.getBlockEntity(pos) instanceof PowerMonitorBlockEntity m ? m.comparatorLevel() : 0;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new PowerMonitorBlockEntity(pos, state);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide() ? null : createTickerHelper(type, WorkingConsolesCore.POWER_MONITOR_BLOCK_ENTITY, PowerMonitorBlockEntity::serverTick);
   }
}
