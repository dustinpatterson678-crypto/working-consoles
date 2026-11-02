package com.dustin.arcadebuild;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Place it where the front door should go, facing the way you want to walk in; right-click twice to build. */
public class ArcadeBuilderBlock extends Block {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final Map<UUID, Long> ASKED = new HashMap<>();
   private final int plan;

   public ArcadeBuilderBlock(Properties properties, int plan) {
      super(properties);
      this.plan = plan;
      this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());   // the building extends the way you look
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!(level instanceof ServerLevel server)) {
         return InteractionResult.SUCCESS;
      }
      if (this.plan == 2) {
         player.sendSystemMessage(Component.literal("The Grand Hall was retired in 1.1. Use the Dustin's Arcade 1.1 builder instead.")
            .withStyle(ChatFormatting.RED));
         return InteractionResult.SUCCESS;
      }
      long now = level.getGameTime();
      Long asked = ASKED.get(player.getUUID());
      if (asked == null || now - asked > 100) {
         ASKED.put(player.getUUID(), now);
         BuildingPlan bp = BuildingPlan.of(this.plan);
         player.sendSystemMessage(Component.literal(bp.name + ": this builds a " + bp.width() + " x " + (bp.depth() + 4)
            + " block arcade starting here and going the way this block faces. Everything in that space gets replaced. Right-click again within 5 seconds to build.")
            .withStyle(ChatFormatting.GOLD));
         return InteractionResult.SUCCESS;
      }
      ASKED.remove(player.getUUID());
      level.removeBlock(pos, false);
      BuildJob.start(server, pos, state.getValue(FACING), player, BuildingPlan.of(this.plan));
      return InteractionResult.SUCCESS;
   }
}
