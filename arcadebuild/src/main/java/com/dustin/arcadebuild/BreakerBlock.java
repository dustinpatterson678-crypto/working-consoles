package com.dustin.arcadebuild;

import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** The arcade's main breaker: one switch for every outlet, the ceiling lights and the neon. */
public class BreakerBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private final Supplier<BlockEntityType<BreakerBlockEntity>> type;

   public BreakerBlock(Properties properties, Supplier<BlockEntityType<BreakerBlockEntity>> type) {
      super(properties);
      this.type = type;
      this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, true));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(FACING, POWERED);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new BreakerBlockEntity(this.type.get(), pos, state);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide()) toggle(level, pos, state, player);
      return InteractionResult.SUCCESS;
   }

   /** Throw the main breaker (also done remotely from an electrical panel). */
   public static void toggle(Level level, BlockPos pos, BlockState state, Player player) {
      if (!(level.getBlockEntity(pos) instanceof BreakerBlockEntity be)) return;
      boolean on = !state.getValue(POWERED);
      level.setBlock(pos, state.setValue(POWERED, on), 3);
      int n = be.apply(level, on);
      level.playSound(null, pos, on ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
      player.sendSystemMessage(Component.literal(on ? "Breaker ON: the arcade is open (" + n + " circuits live)" : "Breaker OFF: closing time, everything is dark")
         .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY));
   }
}
