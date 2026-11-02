package com.dustin.arcadebuild;

import com.dustin.wcore.block.OutletBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Floor power line. Lay a run of these across the floor from anywhere and put a wall/floor outlet next to
 * one: it is on the arcade's circuit and the main breaker switches it with everything else.
 */
public class PowerConduitBlock extends Block {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

   public PowerConduitBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(POWERED);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected boolean isSignalSource(BlockState state) {
      return true;
   }

   @Override
   protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return state.getValue(POWERED) ? 15 : 0;
   }

   /** The breaker this conduit is wired to: the nearest one within a building's reach. */
   static BlockPos findBreaker(Level level, BlockPos pos) {
      BlockPos best = null;
      double bd = Double.MAX_VALUE;
      for (BlockPos q : BlockPos.betweenClosed(pos.offset(-30, -6, -50), pos.offset(30, 8, 50))) {
         if (level.getBlockState(q).getBlock() instanceof BreakerBlock) {
            double d = q.distSqr(pos);
            if (d < bd) {
               bd = d;
               best = q.immutable();
            }
         }
      }
      return best;
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      if (level.isClientSide()) return;
      BlockPos br = findBreaker(level, pos);
      boolean on = br != null && level.getBlockState(br).getValue(BreakerBlock.POWERED);
      level.setBlock(pos, state.setValue(POWERED, on), 3);
      refreshAround(level, pos);
      if (placer instanceof Player p) {
         p.sendOverlayMessage(Component.literal(br == null ? "Power line: no main breaker nearby, this line is dead"
            : on ? "Power line connected to the arcade circuit (live)" : "Power line connected (breaker is off)")
            .withStyle(br == null ? ChatFormatting.RED : ChatFormatting.GREEN));
      }
   }

   static void refreshAround(Level level, BlockPos pos) {
      for (Direction d : Direction.values()) {
         BlockPos n = pos.relative(d);
         if (level.getBlockState(n).getBlock() instanceof OutletBlock) OutletBlock.refresh(level, n);
      }
   }

   /** Called by the breaker: switch every power line in the building. */
   static void switchAll(Level level, BlockPos a, BlockPos b, boolean on) {
      for (BlockPos q : BlockPos.betweenClosed(a, b)) {
         BlockState s = level.getBlockState(q);
         if (s.getBlock() instanceof PowerConduitBlock && s.getValue(POWERED) != on) {
            BlockPos at = q.immutable();
            level.setBlock(at, s.setValue(POWERED, on), 3);
            refreshAround(level, at);
         }
      }
   }
}
