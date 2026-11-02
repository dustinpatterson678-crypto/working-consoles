package com.dustin.arcadebuild;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The building's modeled front: a solid wall block whose renderer draws the whole storefront shell
 * (parapet, giant neon NICKEL ARCADE sign, striped awning, columns, sliding glass doors, rooftop units).
 * FACING = the way you walk into the building.
 */
public class FacadeBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private final Supplier<BlockEntityType<FacadeBlockEntity>> type;
   public final int plan;

   public FacadeBlock(Properties properties, int plan, Supplier<BlockEntityType<FacadeBlockEntity>> type) {
      super(properties);
      this.type = type;
      this.plan = plan;
      this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;      // looks like the stucco wall it sits in; the shell is drawn by the renderer
   }

   @Override
   public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, BlockState state, BlockEntityType<T> t) {
      return level.isClientSide() || t != this.type.get() ? null : (l, p, s, be) -> ((FacadeBlockEntity)be).serverTick();
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new FacadeBlockEntity(this.type.get(), pos, state);
   }
}
