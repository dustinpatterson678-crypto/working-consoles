package com.dustin.stream;

import com.dustin.wcore.block.ConsoleBlockEntity;
import com.dustin.wcore.item.CableItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

public class MediaBlock extends BaseEntityBlock {
   public enum Kind { TV, PROJECTOR }

   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape TV_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private static final VoxelShape PROJECTOR_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 7.0, 13.0);
   public final Kind kind;

   public MediaBlock(Properties properties, Kind kind) {
      super(properties);
      this.kind = kind;
      this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      // TV front faces the player; the projector points where the player is looking (toward the screen)
      Direction look = ctx.getHorizontalDirection();
      return this.defaultBlockState().setValue(FACING, this.kind == Kind.TV ? look.getOpposite() : look);
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.kind == Kind.TV ? TV_SHAPE : PROJECTOR_SHAPE;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new MediaBlockEntity(pos, state);
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide()
         ? createTickerHelper(type, StreamMod.MEDIA_BE, (l, p, s, be) -> StreamHooks.TICK.accept(be))
         : createTickerHelper(type, StreamMod.MEDIA_BE, ConsoleBlockEntity::serverTick);
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      return stack.getItem() instanceof CableItem ? InteractionResult.PASS : InteractionResult.TRY_WITH_EMPTY_HAND;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!(level.getBlockEntity(pos) instanceof MediaBlockEntity media)) {
         return InteractionResult.PASS;
      }
      if (level.isClientSide()) {
         if (!player.isShiftKeyDown() && media.isOn()) {
            StreamHooks.OPEN.accept(pos);
         }
         return InteractionResult.SUCCESS;
      }
      if (player.isShiftKeyDown()) {
         if (media.isOn()) {
            media.setOn(false);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 1.6F);
            tell(player, media.consoleName() + " off", ChatFormatting.GRAY);
         }
      } else if (!media.isOn()) {
         if (media.outletPos() == null) {
            tell(player, "Not plugged in: use a cable on it, then on a wall outlet", ChatFormatting.GOLD);
         } else {
            media.refreshPower();
            if (!media.isPowered()) {
               tell(player, "No power: give the outlet's block a redstone signal", ChatFormatting.GOLD);
            } else {
               media.setOn(true);
               level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 1.6F);
               tell(player, "On. Right-click again to use the browser, sneak + right-click to turn off", ChatFormatting.GREEN);
            }
         }
      }
      return InteractionResult.SUCCESS;
   }

   public static void tell(Player player, String text, ChatFormatting color) {
      player.sendOverlayMessage(Component.literal(text).withStyle(color));
   }
}
