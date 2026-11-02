package com.dustin.wcore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TvBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 12.0);
   private static final VoxelShape SHAPE_X = Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 16.0);
   public final TvType type;

   public static TvBlock.Projection projection(BlockGetter level, BlockPos pos, BlockState state) {
      if (state.getBlock() instanceof TvBlock tv && tv.type.projects()) {
         Direction f = (Direction)state.getValue(FACING);
         boolean hanging = tv.type.hanging();
         int dir = f.getAxisDirection().getStep();

         for (int k = 1; k <= 24; k++) {
            double near = Double.MAX_VALUE;

            for (int dy = 0; dy < 3; dy++) {
               BlockPos at = pos.relative(f, k).above(hanging ? -dy : dy);
               VoxelShape shape = level.getBlockState(at).getCollisionShape(level, at);
               if (!shape.isEmpty()) {
                  AABB b = shape.bounds();
                  double lo = f.getAxis().choose(b.minX, b.minY, b.minZ);
                  double hi = f.getAxis().choose(b.maxX, b.maxY, b.maxZ);
                  near = Math.min(near, dir > 0 ? lo : 1.0 - hi);
               }
            }

            if (near != Double.MAX_VALUE) {
               double d = k - 0.5 + near - 0.02;
               double w = Math.max(1.0, Math.min(hanging ? 8.0 : 9.0, d * (hanging ? 0.5 : 0.6)));
               double h = w * 0.75;
               double cy = hanging ? 0.75 - d * 0.02 - h / 2.0 : 0.35 + d * 0.06 + h / 2.0;
               Vec3 c = new Vec3(0.5 + f.getStepX() * d, cy, 0.5 + f.getStepZ() * d);
               return new TvBlock.Projection(c, w, h, f.getOpposite());
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public TvBlock(Properties properties) {
      this(properties, TvType.FLAT);
   }

   public TvBlock(Properties properties, TvType type) {
      super(properties);
      this.type = type;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return (BlockState)this.defaultBlockState()
         .setValue(FACING, this.type.projects() ? ctx.getHorizontalDirection() : ctx.getHorizontalDirection().getOpposite());
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.type == TvType.FLAT
         ? (((Direction)state.getValue(FACING)).getAxis() == Axis.X ? SHAPE_X : SHAPE)
         : this.type.shape((Direction)state.getValue(FACING));
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new TvBlockEntity(pos, state);
   }

   public record Projection(Vec3 centre, double width, double height, Direction facing) {
   }
}
