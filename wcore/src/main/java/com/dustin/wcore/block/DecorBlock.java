package com.dustin.wcore.block;

import com.dustin.wcore.WorkingConsolesCore;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * v5 retro room furniture. The whole model is drawn by the client's
 * DecorRenderer (GeckoLib); the block itself carries the facing direction, the
 * collision shape, the model id and the display-slot layout, the same trick
 * TvBlock/TvType use for the TVs.
 *
 * <p>Interactions: click with a game case / console / controller to put it on
 * display, sneak + click to take it back, plain click opens doors/drawers on
 * the pieces that have them. Holding a block always falls through so blocks can
 * still be placed on top of the furniture.
 */
public class DecorBlock extends BaseEntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   /** Lights / neon / machine marquees can be switched on and off by hand. */
   public static final BooleanProperty LIT = BlockStateProperties.LIT;

   /**
    * Optional per-piece behaviour (arcade machines, floor tiles, stool,
    * directory). Decor pieces without one stay plain furniture.
    */
   public interface DecorUse {
      default InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
         return InteractionResult.PASS;
      }

      default InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         return InteractionResult.PASS;
      }

      default void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
      }
   }

   /** What a display slot can hold; the matching item tag decides that. */
   public enum SlotKind {
      CASE,
      CONSOLE,
      CONTROLLER
   }

   /**
    * One display slot: x/y/z in model pixels (x/z around the block centre, y up
    * from the bottom of the lower block, front = -z). yaw/tilt are extra item
    * render rotations in degrees, scale the item size.
    */
   public record DecorSlot(SlotKind kind, double x, double y, double z, float scale, float yaw, float tilt) {}

   public final Identifier model;
   public final VoxelShape northLower;
   public final VoxelShape northUpper;
   public final boolean animated;
   public final List<DecorSlot> slots;
   /** Optional function hook; set with {@link #withUse}. */
   public DecorUse use;
   /**
    * Model pixels the model is shifted along model +x at render time. Wide
    * pieces (WideDecorBlock) are authored centred on the seam between their two
    * cells, so they set this to 8; everything else stays 0.
    */
   public double anchorPx = 0.0;

   public DecorBlock(Properties properties, Identifier model, VoxelShape lower, VoxelShape upper) {
      this(properties, model, lower, upper, false, List.of());
   }

   public DecorBlock(Properties properties, Identifier model, VoxelShape lower, VoxelShape upper, boolean animated, List<DecorSlot> slots) {
      super(properties);
      this.model = model;
      this.northLower = lower;
      this.northUpper = upper;
      this.animated = animated;
      this.slots = slots;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH).setValue(LIT, true));
   }

   /** Attaches the arcade (or other) behaviour to this piece. */
   public DecorBlock withUse(DecorUse use) {
      this.use = use;
      return this;
   }

   public static TagKey<Item> tagFor(SlotKind kind) {
      return switch (kind) {
         case CASE -> WorkingConsolesCore.GAME_CASES;
         case CONSOLE -> WorkingConsolesCore.CONSOLES;
         case CONTROLLER -> WorkingConsolesCore.CONTROLLERS;
      };
   }

   public static SlotKind kindOf(ItemStack stack) {
      for (SlotKind kind : SlotKind.values()) {
         if (stack.is(tagFor(kind))) {
            return kind;
         }
      }

      return null;
   }

   /** The block that owns the model/block entity (upper/wide side cells use the main cell). */
   public static BlockPos decorPos(BlockState state, BlockPos pos) {
      if (state.getBlock() instanceof TallDecorBlock && state.getValue(TallDecorBlock.HALF) == DoubleBlockHalf.UPPER) {
         return pos.below();
      }

      if (state.getBlock() instanceof WideDecorBlock && state.getValue(WideDecorBlock.SIDE) == WideDecorBlock.Side.SIDE) {
         return pos.relative(WideDecorBlock.sideDir(state).getOpposite());
      }

      return pos;
   }

   private DecorBlockEntity decorAt(Level level, BlockState state, BlockPos pos) {
      return level.getBlockEntity(decorPos(state, pos)) instanceof DecorBlockEntity be ? be : null;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, LIT});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return (BlockState)this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.shapeFor(state, false);
   }

   protected VoxelShape shapeFor(BlockState state, boolean upper) {
      VoxelShape shape = upper && this.northUpper != null ? this.northUpper : this.northLower;
      return rotate(shape, (Direction)state.getValue(FACING));
   }

   // same rotation table TvType.shape uses for its collision shapes
   public static VoxelShape rotate(VoxelShape north, Direction facing) {
      AABB b = north.bounds();
      double x0 = b.minX * 16.0;
      double x1 = b.maxX * 16.0;
      double y0 = b.minY * 16.0;
      double y1 = b.maxY * 16.0;
      double z0 = b.minZ * 16.0;
      double z1 = b.maxZ * 16.0;

      return switch (facing) {
         case SOUTH -> Block.box(16.0 - x1, y0, 16.0 - z1, 16.0 - x0, y1, 16.0 - z0);
         case EAST -> Block.box(16.0 - z1, y0, x0, 16.0 - z0, y1, x1);
         case WEST -> Block.box(z0, y0, 16.0 - x1, z1, y1, 16.0 - x0);
         default -> north;
      };
   }

   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (this.use != null) {
         InteractionResult used = this.use.useItemOn(stack, state, level, pos, player, hand, hit);
         if (used != InteractionResult.PASS) {
            return used;
         }
      }

      DecorBlockEntity be = this.decorAt(level, state, pos);
      if (be == null || stack.isEmpty() || player.isShiftKeyDown()) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      // clicking the top face with a block means "place it on the furniture",
      // not "put it on display inside"
      if (hit.getDirection() == Direction.UP && stack.getItem() instanceof BlockItem) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      SlotKind kind = kindOf(stack);
      if (kind == null) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      int slot = be.firstEmpty(this, kind);
      if (slot < 0) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      if (!level.isClientSide()) {
         be.set(slot, stack.copyWithCount(1));
         stack.consume(1, player);
         level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 1.0F, 1.0F);
      }

      return InteractionResult.SUCCESS;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (this.use != null) {
         InteractionResult used = this.use.useWithoutItem(state, level, pos, player, hit);
         if (used.consumesAction()) {
            return used;
         }
      }

      DecorBlockEntity be = this.decorAt(level, state, pos);
      if (be == null) {
         return InteractionResult.PASS;
      }

      // holding any block item: always fall through so normal block placement
      // wins (consoles/TVs on top of the furniture, blocks against its faces)
      if (player.getMainHandItem().getItem() instanceof BlockItem) {
         return InteractionResult.PASS;
      }

      if (player.isShiftKeyDown()) {
         int slot = be.lastFilled();
         if (slot < 0) {
            return this.animated ? this.toggle(level, pos, be) : InteractionResult.PASS;
         }

         if (!level.isClientSide()) {
            ItemStack taken = be.get(slot);
            be.set(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(taken)) {
               Block.popResource(level, pos, taken);
            }

            level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         return InteractionResult.SUCCESS;
      }

      // neon signs, light fixtures and machine marquees: plain click is a
      // switch. Only pieces that actually emit light react here - use the
      // default state so a switched-off piece can be switched back on.
      if (state.getBlock().defaultBlockState().getLightEmission() > 0) {
         if (!level.isClientSide()) {
            boolean lit = !(Boolean)state.getValue(LIT);
            level.setBlock(pos, (BlockState)state.setValue(LIT, lit), 3);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, lit ? 1.2F : 0.8F);
         }

         return InteractionResult.SUCCESS;
      }

      if (this.animated) {
         return this.toggle(level, pos, be);
      }

      int slot = be.lastFilled();
      if (slot < 0) {
         return InteractionResult.PASS;
      }

      if (!level.isClientSide()) {
         ItemStack taken = be.get(slot);
         be.set(slot, ItemStack.EMPTY);
         if (!player.getInventory().add(taken)) {
            Block.popResource(level, pos, taken);
         }

         level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_PICKUP, SoundSource.BLOCKS, 1.0F, 1.0F);
      }

      return InteractionResult.SUCCESS;
   }

   private InteractionResult toggle(Level level, BlockPos pos, DecorBlockEntity be) {
      if (!level.isClientSide()) {
         boolean open = !be.isOpen();
         be.setOpen(open);
         level.playSound(null, pos, open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 0.7F, open ? 1.0F : 0.9F);
      }

      return InteractionResult.SUCCESS;
   }

   public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
      if (this.use != null) {
         this.use.stepOn(level, pos, state, entity);
      }
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new DecorBlockEntity(pos, state);
   }
}
