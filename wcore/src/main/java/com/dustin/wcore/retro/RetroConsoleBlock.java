package com.dustin.wcore.retro;

import com.dustin.wcore.block.ConsoleBlockEntity;
import com.dustin.wcore.item.CableItem;
import com.dustin.wcore.item.WirelessControllerItem;
import com.dustin.wcore.net.ConsoleSessionManager;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
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

public class RetroConsoleBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static BiConsumer<BlockPos, Level> CLIENT_FOCUS = (pos, level) -> {};
   public final String displayName;
   public final Set<String> systems;
   public final boolean builtInScreen;
   private final Supplier<BlockEntityType<RetroConsoleBlockEntity>> type;
   private final VoxelShape ns;
   private final VoxelShape ew;

   public RetroConsoleBlock(
      Properties properties,
      String displayName,
      Set<String> systems,
      boolean builtInScreen,
      Supplier<BlockEntityType<RetroConsoleBlockEntity>> type,
      VoxelShape northSouth,
      VoxelShape eastWest
   ) {
      super(properties);
      this.displayName = displayName;
      this.systems = systems;
      this.builtInScreen = builtInScreen;
      this.type = type;
      this.ns = northSouth;
      this.ew = eastWest;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   public boolean accepts(String system) {
      return this.systems.contains("*") || this.systems.contains(system);
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
      return ((Direction)state.getValue(FACING)).getAxis() == Axis.Z ? this.ns : this.ew;
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new RetroConsoleBlockEntity(this.type.get(), pos, state);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide()
         ? createTickerHelper(type, this.type.get(), (l, p, s, be) -> RetroClientHooks.TICK.accept(be))
         : createTickerHelper(type, this.type.get(), ConsoleBlockEntity::serverTick);
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
            CLIENT_FOCUS.accept(pos, level);
         } else if (player instanceof ServerPlayer sp) {
            ConsoleSessionManager.join(sp, pos, false);
         }

         return InteractionResult.SUCCESS;
      } else if (stack.getItem() instanceof RetroMediaItem media) {
         if (level.isClientSide()) {
            CLIENT_FOCUS.accept(pos, level);
            return InteractionResult.SUCCESS;
         }

         if (level.getBlockEntity(pos) instanceof RetroConsoleBlockEntity console) {
            String var14 = RetroMediaItem.gameOf(stack);
            if (!this.accepts(media.systemId)) {
               tell(player, "That doesn't fit in a " + this.displayName, ChatFormatting.RED);
               return InteractionResult.SUCCESS;
            }

            if (var14.isEmpty()) {
               tell(player, "That one's blank: use it in your hand to put a game on it", ChatFormatting.GOLD);
               return InteractionResult.SUCCESS;
            }

            insert(console, media.systemId, var14, player, level, pos);
            if (console.isOn() && player instanceof ServerPlayer sp) {
               ConsoleSessionManager.join(sp, pos, false);
            }

            if (!player.isCreative()) {
               stack.shrink(1);
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      } else {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (level.isClientSide()) {
         CLIENT_FOCUS.accept(pos, level);
         return InteractionResult.SUCCESS;
      }

      if (level.getBlockEntity(pos) instanceof RetroConsoleBlockEntity console) {
         if (player.isShiftKeyDown()) {
            eject(console, player, level, pos);
         } else {
            togglePower(console, player, level, pos);
         }

         if (console.isOn() && player instanceof ServerPlayer sp) {
            ConsoleSessionManager.join(sp, pos, false);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   public static void insert(RetroConsoleBlockEntity console, String system, String game, Player player, Level level, BlockPos pos) {
      if (!console.media().isEmpty()) {
         give(player, level, pos, console.mediaStack());
      }

      console.setMedia(system + "|" + game);
      console.playAnim("insert");
      level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 0.8F, 1.2F);
      boolean wasOn = console.isOn();
      if (!wasOn) {
         togglePower(console, player, level, pos);
      } else {
         tell(player, "Loaded " + RetroSystem.title(game), ChatFormatting.AQUA);
      }
   }

   public static void eject(RetroConsoleBlockEntity console, Player player, Level level, BlockPos pos) {
      if (console.media().isEmpty()) {
         tell(player, "Nothing inserted", ChatFormatting.GRAY);
      } else {
         give(player, level, pos, console.mediaStack());
         console.setMedia("");
         console.playAnim("eject");
         level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
      }
   }

   public static void togglePower(RetroConsoleBlockEntity console, Player player, Level level, BlockPos pos) {
      RetroConsoleBlock block = console.block();
      if (console.isOn()) {
         console.setOn(false);
         level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 1.6F);
         tell(player, "Off", ChatFormatting.GRAY);
      } else if (console.outletPos() == null) {
         tell(player, "Not plugged in: use a cable on it, then on a wall outlet", ChatFormatting.GOLD);
      } else {
         console.refreshPower();
         if (!console.isPowered()) {
            tell(player, "No power: give the outlet's block a redstone signal", ChatFormatting.GOLD);
         } else {
            RetroSystem sys = console.system();
            if (sys != null && sys.needsBios() && sys.bios() == null) {
               console.playAnim("shake");
               tell(player, "No " + sys.name + " BIOS in " + sys.biosDir() + " (" + String.join(" or ", sys.biosAnyOf) + ")", ChatFormatting.RED);
            } else {
               console.setOn(true);
               level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, 1.6F);
               boolean needsTv = block != null && !block.builtInScreen && console.tvPos() == null;
               tell(
                  player,
                  needsTv ? "On, but no TV is connected: use a cable on it, then on a TV" : (console.media().isEmpty() ? "On: insert a game" : "On"),
                  needsTv ? ChatFormatting.GOLD : ChatFormatting.GREEN
               );
            }
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

   static void tell(Player player, String text, ChatFormatting color) {
      if (player != null) {
         player.sendOverlayMessage(Component.literal(text).withStyle(color));
      }
   }
}
