package com.dustin.wcore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Two-blocks-wide furniture (AV stand, 2-wide signs, counters). The item places
 * the MAIN cell, the block entity + GeckoLib model live there, and the model
 * spans both cells - authored centred on the seam between them (x -16..16 px),
 * with {@link DecorBlock#anchorPx} telling the renderer to shift the model and
 * every display slot +8 px along model +x.
 *
 * <p>The side cell sits at MAIN.relative(sideDir(state)) with
 * sideDir = facing.getClockWise(), or the counter-clockwise side when MIRROR is
 * set. Placement tries the normal side first and then the mirrored side, so a
 * wide piece can always be placed in a corner or against other furniture; the
 * player can also choose which side the extra cell takes by facing the other
 * way.
 */
public class WideDecorBlock extends DecorBlock {
   public enum Side implements StringRepresentable {
      MAIN("main"),
      SIDE("side");

      private final String name;

      Side(String name) {
         this.name = name;
      }

      public String getSerializedName() {
         return this.name;
      }
   }

   public static final EnumProperty<Side> SIDE = EnumProperty.create("side", Side.class);
   /** true = the side cell is on the counter-clockwise side of FACING. */
   public static final BooleanProperty MIRROR = BooleanProperty.create("mirror");

   /** Side-cell direction for this block state (mirror-aware). */
   public static Direction sideDir(BlockState state) {
      Direction facing = (Direction)state.getValue(FACING);
      return state.getValue(MIRROR) ? facing.getCounterClockWise() : facing.getClockWise();
   }

   /** Default (non-mirrored) side-cell direction. */
   public static Direction sideDir(Direction facing) {
      return facing.getClockWise();
   }

   public WideDecorBlock(Properties properties, Identifier model, VoxelShape lower, boolean animated, java.util.List<DecorBlock.DecorSlot> slots) {
      super(properties, model, lower, null, animated, slots);
      this.anchorPx = 8.0;
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(SIDE, Side.MAIN)).setValue(MIRROR, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{SIDE, MIRROR});
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      BlockState state = super.getStateForPlacement(ctx);
      if (state == null) {
         return null;
      }

      Direction facing = (Direction)state.getValue(FACING);
      if (ctx.getLevel().getBlockState(ctx.getClickedPos().relative(facing.getClockWise())).canBeReplaced(ctx)) {
         return (BlockState)((BlockState)state.setValue(SIDE, Side.MAIN)).setValue(MIRROR, false);
      }

      if (ctx.getLevel().getBlockState(ctx.getClickedPos().relative(facing.getCounterClockWise())).canBeReplaced(ctx)) {
         return (BlockState)((BlockState)state.setValue(SIDE, Side.MAIN)).setValue(MIRROR, true);
      }

      return null;
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      level.setBlock(pos.relative(sideDir(state)), (BlockState)state.setValue(SIDE, Side.SIDE), 3);
   }

   @Override
   protected BlockState updateShape(
      BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos nPos, BlockState nState, RandomSource random
   ) {
      Side side = (Side)state.getValue(SIDE);
      Direction towardPartner = side == Side.MAIN ? sideDir(state) : sideDir(state).getOpposite();
      boolean partner = nState.is(this) && nState.getValue(SIDE) != side && nState.getValue(MIRROR) == state.getValue(MIRROR);
      return dir != towardPartner || partner
         ? super.updateShape(state, level, ticks, pos, dir, nPos, nState, random)
         : Blocks.AIR.defaultBlockState();
   }

   @Override
   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide() && player.isCreative() && state.getValue(SIDE) == Side.SIDE) {
         BlockPos main = pos.relative(sideDir(state).getOpposite());
         if (level.getBlockState(main).is(this)) {
            level.setBlock(main, Blocks.AIR.defaultBlockState(), 35);
         }
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.shapeFor(state, false);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return state.getValue(SIDE) == Side.MAIN ? super.newBlockEntity(pos, state) : null;
   }
}
