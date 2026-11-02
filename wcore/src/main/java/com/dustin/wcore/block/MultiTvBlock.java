package com.dustin.wcore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The two big screens (flat TV + rear-projection TV) placed at native size on a
 * 2 wide x 2 tall footprint. The model is authored centred on the seam between
 * the two columns (x -15..15 / -14..14 px) and TvRenderer shifts the drawn
 * model +0.5 blocks toward the side column, so the block entity + model only
 * live on the MAIN (lower, facing-side) cell:
 *
 *   MAIN_UP | SIDE_UP
 *   MAIN    | SIDE      (SIDE is at MAIN.relative(facing.getClockWise()))
 *
 * The break path clears every other cell, so the piece always drops exactly one
 * item, whichever cell the player mines. updateShape is left tolerant (no
 * self-destruct) so a leftover MAIN cell from a pre-scale world loads fine
 * until it is broken and re-placed.
 */
public class MultiTvBlock extends TvBlock {
   public enum Part implements StringRepresentable {
      MAIN("main"),
      MAIN_UP("main_up"),
      SIDE("side"),
      SIDE_UP("side_up");

      private final String name;

      Part(String name) {
         this.name = name;
      }

      public String getSerializedName() {
         return this.name;
      }
   }

   public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

   // the model is authored centred on the seam, so the side column sits at
   // model +x. GeckoLib maps the model's front(-z) to FACING, which puts
   // model +x at facing.getClockWise() (verified against the TV screens and
   // cable ports, both of which already assume that mapping).
   public static Direction sideDir(Direction facing) {
      return facing.getClockWise();
   }

   private static final VoxelShape FULL = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private static final VoxelShape FLAT_LOW = Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 12.0);
   private static final VoxelShape FLAT_LOW_X = Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 16.0);
   private static final VoxelShape FLAT_UP = Block.box(0.0, 0.0, 4.0, 16.0, 10.0, 12.0);
   private static final VoxelShape FLAT_UP_X = Block.box(4.0, 0.0, 0.0, 12.0, 10.0, 16.0);

   public MultiTvBlock(Properties properties, TvType type) {
      super(properties, type);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(PART, Part.MAIN));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{PART});
   }

   /** The MAIN cell (block entity + drawn model) for any of the four cells. */
   public static BlockPos mainPos(BlockState state, BlockPos pos) {
      Direction side = sideDir((Direction)state.getValue(FACING));
      return switch ((Part)state.getValue(PART)) {
         case MAIN -> pos;
         case MAIN_UP -> pos.below();
         case SIDE -> pos.relative(side.getOpposite());
         case SIDE_UP -> pos.relative(side.getOpposite()).below();
      };
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      BlockState state = super.getStateForPlacement(ctx);
      if (state == null) {
         return null;
      }

      Direction side = sideDir((Direction)state.getValue(FACING));
      BlockPos pos = ctx.getClickedPos();
      Level level = ctx.getLevel();
      if (pos.getY() >= level.getMaxY()) {
         return null;
      }

      return level.getBlockState(pos.relative(side)).canBeReplaced(ctx) && level.getBlockState(pos.above()).canBeReplaced(ctx)
            && level.getBlockState(pos.relative(side).above()).canBeReplaced(ctx)
         ? (BlockState)state.setValue(PART, Part.MAIN)
         : null;
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      Direction side = sideDir((Direction)state.getValue(FACING));
      BlockPos sidePos = pos.relative(side);
      level.setBlock(pos.above(), (BlockState)state.setValue(PART, Part.MAIN_UP), 3);
      level.setBlock(sidePos, (BlockState)state.setValue(PART, Part.SIDE), 3);
      level.setBlock(sidePos.above(), (BlockState)state.setValue(PART, Part.SIDE_UP), 3);
   }

   @Override
   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide()) {
         // whichever cell is mined, clear the other three so exactly one item
         // drops (the mined cell's own loot) and no orphan cells linger
         Part mined = (Part)state.getValue(PART);
         Direction side = sideDir((Direction)state.getValue(FACING));
         BlockPos main = mainPos(state, pos);
         clearPart(level, mined, Part.MAIN, main);
         clearPart(level, mined, Part.MAIN_UP, main.above());
         clearPart(level, mined, Part.SIDE, main.relative(side));
         clearPart(level, mined, Part.SIDE_UP, main.relative(side).above());
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   private void clearPart(Level level, Part mined, Part part, BlockPos at) {
      if (part != mined && level.getBlockState(at).is(this) && level.getBlockState(at).getValue(PART) == part) {
         level.setBlock(at, Blocks.AIR.defaultBlockState(), 35);
      }
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      if (this.type != TvType.FLAT) {
         return FULL;
      }

      Part part = (Part)state.getValue(PART);
      boolean up = part == Part.MAIN_UP || part == Part.SIDE_UP;
      boolean alongX = ((Direction)state.getValue(FACING)).getAxis() == Axis.X;
      return up ? (alongX ? FLAT_UP_X : FLAT_UP) : (alongX ? FLAT_LOW_X : FLAT_LOW);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return state.getValue(PART) == Part.MAIN ? super.newBlockEntity(pos, state) : null;
   }
}
