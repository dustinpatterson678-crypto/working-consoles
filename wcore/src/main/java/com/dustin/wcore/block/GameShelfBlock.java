package com.dustin.wcore.block;

import com.dustin.wcore.WorkingConsolesCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GameShelfBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

   public GameShelfBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return (BlockState)this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   static int slotAt(BlockState state, BlockPos pos, BlockHitResult hit) {
      Direction facing = (Direction)state.getValue(FACING);
      if (hit.getDirection() != facing) {
         return -1;
      }

      Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());

      double across = switch (facing) {
         case NORTH -> 1.0 - local.x;
         case SOUTH -> local.x;
         case WEST -> local.z;
         default -> 1.0 - local.z;
      };
      double inner = (across - 0.0625) / 0.875;
      return !(inner < 0.0) && !(inner >= 1.0) ? (int)(inner * 8.0) : -1;
   }

   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!stack.is(WorkingConsolesCore.GAME_CASES)) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      if (level.getBlockEntity(pos) instanceof GameShelfBlockEntity shelf) {
         int slot = slotAt(state, pos, hit);
         if (slot >= 0 && shelf.get(slot).isEmpty()) {
            if (!level.isClientSide()) {
               shelf.set(slot, stack.copyWithCount(1));
               stack.consume(1, player);
               level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (level.getBlockEntity(pos) instanceof GameShelfBlockEntity shelf) {
         int slot = slotAt(state, pos, hit);
         if (slot >= 0 && !shelf.get(slot).isEmpty()) {
            if (!level.isClientSide()) {
               ItemStack taken = shelf.get(slot);
               shelf.set(slot, ItemStack.EMPTY);
               if (!player.getInventory().add(taken)) {
                  Block.popResource(level, pos, taken);
               }

               level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new GameShelfBlockEntity(pos, state);
   }
}
