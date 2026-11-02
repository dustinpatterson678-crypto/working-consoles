package com.dustin.wcore.block;

import com.dustin.wcore.controller.ControllerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A controller lying on a block. Drawn by ControllerBlockRenderer with the type's GeckoLib model. */
public class ControllerBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final VoxelShape DEFAULT_SHAPE = Block.box(5.5, 0.0, 6.0, 10.5, 2.0, 10.0);
   public final ControllerType type;
   private final VoxelShape north;
   private BlockEntityType<ControllerBlockEntity> blockEntity;

   public ControllerBlock(Properties properties, ControllerType type, VoxelShape north) {
      super(properties);
      this.type = type;
      this.north = north;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   /** Set once by WorkingConsolesCore.registerController, right after the block entity type is registered. */
   public void bind(BlockEntityType<ControllerBlockEntity> blockEntity) {
      this.blockEntity = blockEntity;
   }

   public BlockEntityType<ControllerBlockEntity> blockEntityType() {
      return this.blockEntity;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return (BlockState)this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return DecorBlock.rotate(this.north, (Direction)state.getValue(FACING));
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ControllerBlockEntity(this.blockEntity, pos, state);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (level.getBlockEntity(pos) instanceof ControllerBlockEntity be) {
         be.setStack(stack);
      }
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (player.isCreative() && level.getBlockEntity(pos) instanceof ControllerBlockEntity be) {
         be.suppressDrop();
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
      return level.getBlockEntity(pos) instanceof ControllerBlockEntity be ? be.dropStack() : super.getCloneItemStack(level, pos, state, includeData);
   }
}
