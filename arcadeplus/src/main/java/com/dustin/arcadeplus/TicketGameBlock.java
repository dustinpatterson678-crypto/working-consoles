package com.dustin.arcadeplus;

import net.minecraft.ChatFormatting;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TicketGameBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT = BlockStateProperties.LIT;
   private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   public final TicketGame game;
   private final Supplier<BlockEntityType<TicketGameBlockEntity>> type;

   public TicketGameBlock(Properties properties, TicketGame game, Supplier<BlockEntityType<TicketGameBlockEntity>> type) {
      super(properties);
      this.game = game;
      this.type = type;
      this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LIT, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(FACING, LIT);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new TicketGameBlockEntity(this.type.get(), pos, state);
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide() ? null : createTickerHelper(type, this.type.get(), TicketGameBlockEntity::serverTick);
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (stack.getItem() instanceof com.dustin.wcore.item.CableItem) {
         return InteractionResult.PASS;            // let the cable plug in
      }
      return this.play(level, pos, player, stack, hit);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      return this.play(level, pos, player, ItemStack.EMPTY, hit);
   }

   /** Set by the client: opens the hands-on screen for Basketball / Claw Machine. */
   public static java.util.function.BiConsumer<BlockPos, TicketGame> OPEN = (p, g) -> {};

   private InteractionResult play(Level level, BlockPos pos, Player player, ItemStack held, BlockHitResult hit) {
      if (level.getBlockEntity(pos) instanceof TicketGameBlockEntity pbe && !pbe.isPowered() && !this.game.unpowered()) {
         if (!level.isClientSide()) {
            TicketGameLogic.tell(player, this.game.title + " has no power: run a Cable from a live Wall Outlet", ChatFormatting.RED);
         }
         return InteractionResult.SUCCESS;
      }
      if (level.isClientSide() && (this.game == TicketGame.BASKETBALL || this.game == TicketGame.CLAWMACHINE || this.game == TicketGame.HOCKEY || this.game == TicketGame.LANES || this.game == TicketGame.POOL)
         && level.getBlockEntity(pos) instanceof TicketGameBlockEntity be
         && (TicketGameLogic.isToken(held) || be.playing || be.stage == 1)) {
         OPEN.accept(pos, this.game);
      }
      if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TicketGameBlockEntity be) {
         TicketGameLogic.use(level, pos, be, player, held, hit.getLocation());
      }
      return InteractionResult.SUCCESS;
   }

   @Override
   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide()) {
         net.minecraft.core.Direction front = state.getValue(FACING), side = front.getClockWise();
         for (int[] c : this.game.cells()) {
            BlockPos p = pos.relative(side, c[0]).relative(front.getOpposite(), c[1]);
            if (level.getBlockState(p).getBlock() instanceof MachinePartBlock) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
         }
      }
      return super.playerWillDestroy(level, pos, state, player);
   }
}
