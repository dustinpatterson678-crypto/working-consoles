package com.dustin.arcadeplus;

import com.dustin.arcadeplus.net.PrizeListPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The walk-up prize booth: a 7-wide counter with glass cases, side returns, a back wall of prize shelves and a
 * lit PRIZES sign. The core block draws it all; the other cells are invisible "part" blocks you can't walk
 * through. Click anywhere on it to redeem tickets. FACING = the side customers stand on.
 */
public class PrizeStandBlock extends Block implements EntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   public final boolean part;
   private final Supplier<BlockEntityType<PrizeStandBlockEntity>> type;

   public PrizeStandBlock(Properties properties, boolean part, Supplier<BlockEntityType<PrizeStandBlockEntity>> type) {
      super(properties);
      this.part = part;
      this.type = type;
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

   /** Every cell of the booth except the core: counter, side returns, back wall (4 high). */
   public static List<BlockPos> partCells(BlockPos core, Direction front) {
      List<BlockPos> out = new ArrayList<>();
      Direction back = front.getOpposite(), side = front.getClockWise();
      for (int x = -3; x <= 3; x++) {
         if (x != 0) out.add(core.relative(side, x));
         for (int y = 0; y < 4; y++) out.add(core.relative(side, x).relative(back, 3).above(y));
      }
      for (int z = 1; z <= 2; z++) {
         out.add(core.relative(side, 3).relative(back, z));
         out.add(core.relative(side, -3).relative(back, z));
      }
      return out;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return this.part ? null : new PrizeStandBlockEntity(this.type.get(), pos, state);
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
      return this.part || level.isClientSide() || t != this.type.get() ? null : (l, p, s, be) -> ((PrizeStandBlockEntity)be).serverTick();
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide() && player instanceof ServerPlayer sp) {
         int n = TicketBank.deposit(player);
         if (n > 0) {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.7F);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Ticket muncher: +" + String.format("%,d", n) + " tickets. Card balance: "
               + String.format("%,d", TicketBank.balance(player))).withStyle(net.minecraft.ChatFormatting.YELLOW));
         }
         PrizeListPacket.send(sp, pos);
      }
      return InteractionResult.SUCCESS;
   }

   /** The core this part belongs to (or itself). */
   public static BlockPos findCore(Level level, BlockPos pos) {
      BlockState s = level.getBlockState(pos);
      if (s.getBlock() instanceof PrizeStandBlock b && !b.part) return pos;
      for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, -4, -4), pos.offset(4, 0, 4))) {
         BlockState cs = level.getBlockState(p);
         if (cs.getBlock() instanceof PrizeStandBlock cb && !cb.part && partCells(p, cs.getValue(FACING)).contains(pos)) return p.immutable();
      }
      return null;
   }

   @Override
   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide()) {
         BlockPos core = findCore(level, pos);
         if (core != null) {
            BlockState cs = level.getBlockState(core);
            for (BlockPos p : partCells(core, cs.getValue(FACING))) {
               if (!p.equals(pos) && level.getBlockState(p).getBlock() instanceof PrizeStandBlock pb && pb.part) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
            if (!core.equals(pos)) level.destroyBlock(core, !player.isCreative());
         }
      }
      return super.playerWillDestroy(level, pos, state, player);
   }
}
