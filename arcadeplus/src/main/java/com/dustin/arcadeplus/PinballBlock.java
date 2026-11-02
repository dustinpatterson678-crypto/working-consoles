package com.dustin.arcadeplus;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
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

public class PinballBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   /** Client entrypoint fills this in: opens the flipper controls. */
   public static Consumer<BlockPos> OPEN = pos -> {};
   private final Supplier<BlockEntityType<PinballBlockEntity>> type;
   /** Which table art this machine wears: classic, galaxy, haunted, pirate, speedway. */
   public final String theme;

   public PinballBlock(Properties properties, Supplier<BlockEntityType<PinballBlockEntity>> type) {
      this(properties, type, "classic");
   }

   public PinballBlock(Properties properties, Supplier<BlockEntityType<PinballBlockEntity>> type, String theme) {
      super(properties);
      this.type = type;
      this.theme = theme;
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
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new PinballBlockEntity(this.type.get(), pos, state);
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide() ? null : createTickerHelper(type, this.type.get(), PinballBlockEntity::serverTick);
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (stack.getItem() instanceof com.dustin.wcore.item.CableItem) {
         return InteractionResult.PASS;            // let the cable plug in
      }
      return this.use(level, pos, player, stack);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      return this.use(level, pos, player, ItemStack.EMPTY);
   }

   private InteractionResult use(Level level, BlockPos pos, Player player, ItemStack held) {
      if (!(level.getBlockEntity(pos) instanceof PinballBlockEntity be)) {
         return InteractionResult.PASS;
      }
      boolean token = TicketGameLogic.isToken(held);
      if (!be.isPowered()) {
         if (!level.isClientSide()) {
            TicketGameLogic.tell(player, "Pinball has no power: run a Cable from a live Wall Outlet", ChatFormatting.RED);
         }
         return InteractionResult.SUCCESS;
      }
      if (level.isClientSide()) {
         if (be.playing || token) {
            OPEN.accept(pos);
         }
         return InteractionResult.SUCCESS;
      }
      if (!be.playing) {
         if (!token) {
            TicketGameLogic.tell(player, "Pinball: insert a nickel for 3 balls" + (be.highScore > 0 ? "   High score " + be.highScore : ""), ChatFormatting.GOLD);
         } else {
            if (!player.isCreative()) {
               held.shrink(1);
            }
            be.start(player);
         }
      }
      return InteractionResult.SUCCESS;
   }
}
