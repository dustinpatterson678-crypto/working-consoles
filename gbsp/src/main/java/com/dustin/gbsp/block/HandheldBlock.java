package com.dustin.gbsp.block;

import com.dustin.gbsp.GameBoySP;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HandheldBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   private static final VoxelShape SHAPE = Block.box(3.5, 0.0, 3.5, 12.5, 3.0, 12.5);

   public HandheldBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!player.isShiftKeyDown()) {
         if (level.isClientSide()) {
            GameBoySP.CLIENT_BLOCK_USE.accept(pos);
         }

         return InteractionResult.SUCCESS;
      } else {
         if (!level.isClientSide()) {
            level.removeBlock(pos, false);
            ItemStack stack = new ItemStack(GameBoySP.GAME_BOY_SP);
            if (!player.getInventory().add(stack)) {
               Block.popResource(level, pos, stack);
            }

            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F);
         }

         return InteractionResult.SUCCESS;
      }
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new HandheldBlockEntity(pos, state);
   }
}
