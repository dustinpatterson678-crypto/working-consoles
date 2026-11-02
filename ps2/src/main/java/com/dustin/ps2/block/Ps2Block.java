package com.dustin.ps2.block;

import com.dustin.ps2.Ps2Files;
import com.dustin.ps2.Ps2Mod;
import com.dustin.ps2.item.DiscCaseItem;
import com.dustin.wcore.block.ConsoleBlockEntity;
import com.dustin.wcore.item.CableItem;
import com.dustin.wcore.item.WirelessControllerItem;
import com.dustin.wcore.net.ConsoleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Ps2Block extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape NS = Block.box(0.5, 0.0, 2.5, 15.5, 4.0, 13.5);
   private static final VoxelShape EW = Block.box(2.5, 0.0, 0.5, 13.5, 4.0, 15.5);

   public Ps2Block(Properties properties) {
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
      return RenderShape.INVISIBLE;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return ((Direction)state.getValue(FACING)).getAxis() == Axis.Z ? NS : EW;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new Ps2BlockEntity(pos, state);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide() ? null : createTickerHelper(type, Ps2Mod.CONSOLE_BLOCK_ENTITY, ConsoleBlockEntity::serverTick);
   }

   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (stack.getItem() instanceof CableItem) {
         return InteractionResult.PASS;
      }

      if (com.dustin.wcore.controller.ControllerTypes.rejects(stack, level, pos, player)) {
         return InteractionResult.SUCCESS;   // a pad for another console: say so, and don't place it here
      }

      if (stack.getItem() instanceof com.dustin.wcore.controller.ConsoleController) {
         if (level.isClientSide()) {
            Ps2Mod.CLIENT_FOCUS.accept(pos);
         } else if (player instanceof ServerPlayer sp) {
            ConsoleSessionManager.join(sp, pos, false);
         }

         return InteractionResult.SUCCESS;
      } else {
         if (!(stack.getItem() instanceof DiscCaseItem)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
         }

         if (level.isClientSide()) {
            Ps2Mod.CLIENT_FOCUS.accept(pos);
            return InteractionResult.SUCCESS;
         }

         if (level.getBlockEntity(pos) instanceof Ps2BlockEntity console) {
            String disc = DiscCaseItem.discOf(stack);
            if (disc.isEmpty()) {
               return InteractionResult.SUCCESS;
            }

            insert(console, disc, player, level, pos);
            if (!player.isCreative()) {
               stack.shrink(1);
            }
         }

         return InteractionResult.SUCCESS;
      }
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (level.isClientSide()) {
         Ps2Mod.CLIENT_FOCUS.accept(pos);
         return InteractionResult.SUCCESS;
      }

      if (level.getBlockEntity(pos) instanceof Ps2BlockEntity console) {
         if (player.isShiftKeyDown()) {
            eject(console, player, level, pos);
            return InteractionResult.SUCCESS;
         }

         togglePower(console, player, level, pos);
         if (console.isOn() && player instanceof ServerPlayer sp) {
            ConsoleSessionManager.join(sp, pos, false);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   public static void insert(Ps2BlockEntity console, String disc, Player player, Level level, BlockPos pos) {
      if (!console.media().isEmpty()) {
         give(player, level, pos, console.discCase());
      }

      console.setMedia(disc);
      console.playAnim("insert");
      level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 0.8F, 1.3F);
      Ps2Files.Disc d = Ps2Files.disc(disc);
      tell(player, "Inserted " + (d != null ? d.title() : disc) + (console.isOn() ? "" : ". Right-click the console to power it on"), ChatFormatting.AQUA);
   }

   public static void eject(Ps2BlockEntity console, Player player, Level level, BlockPos pos) {
      if (console.media().isEmpty()) {
         tell(player, "No disc in the console", ChatFormatting.GRAY);
      } else {
         give(player, level, pos, console.discCase());
         console.setMedia("");
         console.playAnim("eject");
         level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_PICKUP, SoundSource.BLOCKS, 0.8F, 1.3F);
      }
   }

   public static void togglePower(Ps2BlockEntity console, Player player, Level level, BlockPos pos) {
      if (console.isOn()) {
         console.setOn(false);
         level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 1.6F);
         tell(player, "Console off", ChatFormatting.GRAY);
      } else if (console.outletPos() == null) {
         tell(player, "Not plugged in: use a cable on the console, then on a wall outlet", ChatFormatting.GOLD);
      } else {
         console.refreshPower();
         if (!console.isPowered()) {
            tell(player, "No power: give the outlet's block a redstone signal (lever, redstone, redstone block)", ChatFormatting.GOLD);
         } else if (Ps2Files.bios() == null) {
            tell(player, "No PS2 BIOS in " + Ps2Files.biosDir() + ": it won't power on without one", ChatFormatting.RED);
         } else {
            console.setOn(true);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 1.6F);
            tell(
               player,
               console.tvPos() == null
                  ? "Console on, but no TV is connected: use a cable on the console, then a TV"
                  : (console.media().isEmpty() ? "Console on: no disc, booting the system menu" : "Console on"),
               ChatFormatting.GREEN
            );
         }
      }
   }

   private static void give(Player player, Level level, BlockPos pos, ItemStack stack) {
      if (!stack.isEmpty()) {
         if (player == null || !player.getInventory().add(stack)) {
            Block.popResource(level, pos.above(), stack);
         }
      }
   }

   private static void tell(Player player, String text, ChatFormatting color) {
      if (player != null) {
         player.sendOverlayMessage(Component.literal(text).withStyle(color));
      }
   }
}
