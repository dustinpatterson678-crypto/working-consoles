package com.dustin.arcadeplus;

import com.dustin.wcore.retro.RetroConsoleBlockEntity;
import com.dustin.wcore.retro.TallCabinetBlock;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** A tall cabinet whose screen lights up the room around it while it's powered. */
public class GlowCabinetBlock extends TallCabinetBlock {
   public static final BooleanProperty LIT = BlockStateProperties.LIT;

   public GlowCabinetBlock(Properties p, String display, Set<String> systems, Supplier<BlockEntityType<RetroConsoleBlockEntity>> type) {
      super(p.lightLevel(s -> s.hasProperty(LIT) && s.getValue(LIT) ? 7 : 0), display, systems, type);
      this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      super.createBlockStateDefinition(b);
      b.add(LIT);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      BlockState s = super.getStateForPlacement(ctx);
      return s == null ? null : s.setValue(LIT, false);
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      BlockEntityTicker<T> base = super.getTicker(level, state, type);
      if (level.isClientSide()) return base;
      return (l, pos, s, be) -> {
         if (base != null) base.tick(l, pos, s, be);
         if (l.getGameTime() % 10 == 0 && be instanceof RetroConsoleBlockEntity rb) {
            BlockState now = l.getBlockState(pos);
            if (now.getBlock() == this && now.getValue(LIT) != rb.isPowered()) l.setBlock(pos, now.setValue(LIT, rb.isPowered()), 3);
         }
      };
   }
}
