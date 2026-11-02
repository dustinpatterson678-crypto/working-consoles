package com.dustin.arcadeplus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The back half of a long machine (air hockey, alley roller, bowling, basketball): invisible but solid, and clicking
 * it is the same as clicking the machine. FACING points at the machine's main block.
 */
public class MachinePartBlock extends Block {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

   public MachinePartBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   /** Follow the parts toward the machine's main block. */
   private static BlockPos core(net.minecraft.world.level.BlockGetter level, BlockPos pos, BlockState state) {
      BlockPos p = pos;
      BlockState s = state;
      for (int i = 0; i < 4 && s.getBlock() instanceof MachinePartBlock; i++) {
         p = p.relative(s.getValue(FACING));
         s = level.getBlockState(p);
      }
      return p;
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      BlockPos c = core(level, pos, state);
      BlockState cs = level.getBlockState(c);
      if (!(cs.getBlock() instanceof TicketGameBlock)) return InteractionResult.PASS;
      return cs.useItemOn(stack, level, player, hand, new BlockHitResult(hit.getLocation(), hit.getDirection(), c, false));
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      BlockPos c = core(level, pos, state);
      BlockState cs = level.getBlockState(c);
      if (!(cs.getBlock() instanceof TicketGameBlock)) return InteractionResult.PASS;
      return cs.useWithoutItem(level, player, new BlockHitResult(hit.getLocation(), hit.getDirection(), c, false));
   }

   @Override
   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      BlockPos c = core(level, pos, state);
      if (!level.isClientSide() && level.getBlockState(c).getBlock() instanceof TicketGameBlock) level.destroyBlock(c, !player.isCreative());
      return super.playerWillDestroy(level, pos, state, player);
   }
}
