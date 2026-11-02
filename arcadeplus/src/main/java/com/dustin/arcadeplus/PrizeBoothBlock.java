package com.dustin.arcadeplus;

import com.dustin.arcadeplus.net.PrizeListPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The redemption counter (a glass prize case you buy from) and the prize wall (the real prizes on display).
 * Right-click either: your paper tickets go in the muncher and the price list opens.
 */
public class PrizeBoothBlock extends Block implements EntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   public final boolean wall;

   public PrizeBoothBlock(Properties properties, boolean wall) {
      super(properties);
      this.wall = wall;
      this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      if (!this.wall) return Shapes.box(0, 0, 0, 1, 0.95, 1);
      return switch (state.getValue(FACING)) {                  // a panel against the wall behind it
         case SOUTH -> Shapes.box(0, 0, 0, 1, 1, 0.25);
         case EAST -> Shapes.box(0, 0, 0, 0.25, 1, 1);
         case WEST -> Shapes.box(0.75, 0, 0, 1, 1, 1);
         default -> Shapes.box(0, 0, 0.75, 1, 1, 1);
      };
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new PrizeDisplayBlockEntity(Prizes.DISPLAY_BE, pos, state);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide() && player instanceof ServerPlayer sp) {
         int n = TicketBank.deposit(player);                      // the ticket muncher eats your paper tickets
         if (n > 0) {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.7F);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Ticket muncher: +" + String.format("%,d", n) + " tickets. Card balance: "
               + String.format("%,d", TicketBank.balance(player))).withStyle(net.minecraft.ChatFormatting.YELLOW));
         }
         PrizeListPacket.send(sp, pos);
      }
      return InteractionResult.SUCCESS;
   }
}
