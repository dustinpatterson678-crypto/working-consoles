package com.dustin.arcadebuild;

import com.dustin.wcore.block.ConsoleBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A modelled prop (bench, sofa, desk, lamp post...). Drawn by DecorRenderer, never a cube, and every one does something. */
public class DecorBlock extends Block implements EntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final BooleanProperty LIT = BlockStateProperties.LIT;

   public enum Kind {
      BENCH("bench", 0.48, 0, 0, 3, 16, 9, 13),
      PLANTER("planter_tree", 0, 1, 0, 1, 15, 10, 15),
      LAMP_POST("lamp_post", 0, 6, 0, 6, 10, 16, 10),
      SOFA("sofa", 0.55, 0, 0, 2, 16, 9, 15),
      COFFEE_TABLE("coffee_table", 0, 2, 0, 3, 14, 7, 13),
      TEDDY("giant_teddy", 0, 2, 0, 2, 14, 16, 14),
      DUMPSTER("dumpster", 0, 0, 0, 1, 16, 16, 15),
      DESK("manager_desk", 0, 0, 0, 1, 16, 12, 15),
      CHAIR("office_chair", 0.55, 3, 0, 3, 13, 14, 13),
      FILING("filing_cabinet", 0, 1, 0, 2, 15, 16, 14),
      MONITORS("security_monitors", 0, 1, 0, 3, 15, 16, 14),
      PANEL("electrical_panel", 0, 1, 6, 12, 15, 16, 16),
      COOLER("water_cooler", 0, 4, 0, 4, 12, 16, 12),
      PALM("potted_palm", 0, 3, 0, 3, 13, 10, 13),
      MEDIA("media_console", 0, 0, 0, 1, 16, 16, 15);

      public final String id;
      public final double seat;          // seat height if you can sit on it, else 0
      final double[] box;

      Kind(String id, double seat, double... box) {
         this.id = id;
         this.seat = seat;
         this.box = box;
      }

      boolean storage() {
         return this == DUMPSTER || this == DESK || this == FILING || this == MEDIA;
      }
   }

   public final Kind kind;

   public DecorBlock(Properties properties, Kind kind) {
      super(properties);
      this.kind = kind;
      this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LIT, true));
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
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      double[] b = this.kind.box;
      double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
      switch (state.getValue(FACING)) {
         case SOUTH -> { double t0 = 16 - x1, t1 = 16 - x0; x0 = t0; x1 = t1; t0 = 16 - z1; t1 = 16 - z0; z0 = t0; z1 = t1; }
         case EAST -> { double a0 = 16 - z1, a1 = 16 - z0, c0 = x0, c1 = x1; x0 = a0; x1 = a1; z0 = c0; z1 = c1; }
         case WEST -> { double a0 = z0, a1 = z1, c0 = 16 - x1, c1 = 16 - x0; x0 = a0; x1 = a1; z0 = c0; z1 = c1; }
         default -> { }
      }
      return Shapes.box(x0 / 16, b[1] / 16, z0 / 16, x1 / 16, b[4] / 16, z1 / 16);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new DecorBlockEntity(ArcadeBuildMod.DECOR_BE, pos, state);
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   // ---------------- what each prop does ----------------

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (this.kind == Kind.COOLER && stack.is(Items.GLASS_BOTTLE)) {                       // fill a bottle at the cooler
         if (!level.isClientSide()) {
            stack.shrink(1);
            ItemStack water = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER);
            if (!player.getInventory().add(water)) Block.popResource(level, pos.above(), water);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
         }
         return InteractionResult.SUCCESS;
      }
      return InteractionResult.TRY_WITH_EMPTY_HAND;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (level.isClientSide()) {
         if (this.kind == Kind.PALM && level.getBlockEntity(pos) instanceof DecorBlockEntity be) be.sway = 1F;
         return InteractionResult.SUCCESS;
      }
      ServerLevel sl = (ServerLevel)level;
      DecorBlockEntity be = level.getBlockEntity(pos) instanceof DecorBlockEntity d ? d : null;
      long now = level.getGameTime();
      if (this.kind.seat > 0) {
         sit(sl, pos, player);
      } else if (this.kind.storage() && be != null) {
         player.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.threeRows(id, inv, be), Component.translatable(this.getDescriptionId())));
         level.playSound(null, pos, this.kind == Kind.DUMPSTER ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.CHISELED_BOOKSHELF_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
      } else switch (this.kind) {
         case LAMP_POST -> {
            level.setBlock(pos, state.cycle(LIT), 3);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.6F, state.getValue(LIT) ? 0.6F : 0.9F);
         }
         case TEDDY -> {
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.4F);
            sl.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.0);
         }
         case COFFEE_TABLE -> {                                                                 // the snack bowl refills every minute
            if (be != null && now - be.lastUse > 1200) {
               be.lastUse = now;
               ItemStack snack = new ItemStack(level.getRandom().nextBoolean() ? Items.COOKIE : Items.PUMPKIN_PIE);
               if (!player.getInventory().add(snack)) Block.popResource(level, pos.above(), snack);
               level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
            } else {
               player.sendOverlayMessage(Component.literal("The snack bowl is empty, check back in a minute").withStyle(ChatFormatting.GRAY));
            }
         }
         case PLANTER -> {                                                                      // shake the tree
            level.playSound(null, pos, SoundEvents.AZALEA_LEAVES_HIT, SoundSource.BLOCKS, 1.0F, 1.0F);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, pos.getX() + 0.5, pos.getY() + 3.0, pos.getZ() + 0.5, 12, 0.6, 0.4, 0.6, 0.0);
            if (be != null && now - be.lastUse > 2400) {
               be.lastUse = now;
               Block.popResource(level, pos.above(), new ItemStack(Items.APPLE));
            }
         }
         case PALM -> level.playSound(null, pos, SoundEvents.AZALEA_LEAVES_HIT, SoundSource.BLOCKS, 0.8F, 1.3F);
         case COOLER -> {                                                                       // a cup of water
            if (player.getFoodData().needsFood()) player.getFoodData().eat(1, 0.5F);
            level.playSound(null, pos, SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
            sl.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 6, 0.1, 0.1, 0.1, 0.0);
         }
         case MONITORS -> {                                                                     // security feed: what is running
            int running = 0, total = 0;
            for (BlockPos q : BlockPos.betweenClosed(pos.offset(-30, -4, -45), pos.offset(30, 6, 45))) {
               if (level.getBlockEntity(q) instanceof ConsoleBlockEntity c) {
                  total++;
                  if (c.isPowered()) running++;
               }
            }
            int people = level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(pos).inflate(30, 6, 45)).size();
            player.sendSystemMessage(Component.literal("CAMERAS: " + people + " in the building, " + running + " of " + total + " machines running").withStyle(ChatFormatting.GREEN));
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5F, 1.5F);
         }
         case PANEL -> {                                                                        // the panel throws the main breaker
            BlockPos found = null;
            for (BlockPos q : BlockPos.betweenClosed(pos.offset(-24, -4, -24), pos.offset(24, 4, 24))) {
               if (level.getBlockState(q).getBlock() instanceof BreakerBlock) {
                  found = q.immutable();
                  break;
               }
            }
            if (found != null) {
               BreakerBlock.toggle(level, found, level.getBlockState(found), player);
               sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.1, 0.2);
            } else {
               player.sendOverlayMessage(Component.literal("No main breaker wired to this panel").withStyle(ChatFormatting.GRAY));
            }
         }
         default -> { }
      }
      return InteractionResult.SUCCESS;
   }

   private void sit(ServerLevel level, BlockPos pos, Player player) {
      if (player.isPassenger()) return;
      if (!level.getEntitiesOfClass(SeatEntity.class, new net.minecraft.world.phys.AABB(pos)).isEmpty()) {
         player.sendOverlayMessage(Component.literal("Someone is already sitting there").withStyle(ChatFormatting.GRAY));
         return;
      }
      SeatEntity seat = new SeatEntity(ArcadeBuildMod.SEAT, level);
      seat.setPos(pos.getX() + 0.5, pos.getY() + this.kind.seat - 0.5, pos.getZ() + 0.5);
      seat.setYRot(level.getBlockState(pos).getValue(FACING).toYRot());
      level.addFreshEntity(seat);
      player.startRiding(seat);
   }
}
