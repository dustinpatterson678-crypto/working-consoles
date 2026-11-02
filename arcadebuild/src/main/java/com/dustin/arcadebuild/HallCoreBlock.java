package com.dustin.arcadebuild;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The Grand Hall's core: an invisible wall block whose renderer draws the whole room and which lets you back out. */
public class HallCoreBlock extends BaseEntityBlock {
   private final Supplier<BlockEntityType<HallCoreBlockEntity>> type;

   public HallCoreBlock(Properties properties, Supplier<BlockEntityType<HallCoreBlockEntity>> type) {
      super(properties);
      this.type = type;
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new HallCoreBlockEntity(this.type.get(), pos, state);
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
      return level.isClientSide() || t != this.type.get() ? null : (l, p, s, be) -> ((HallCoreBlockEntity)be).serverTick();
   }
}
