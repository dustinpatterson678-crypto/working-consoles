package com.dustin.wcore.block;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.retro.RetroConsoleBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Functions for the v5 arcade pieces:
 *
 * <ul>
 *   <li>carpets / floor decals - full floor blocks; speed and jump pads, the
 *       arrow rotates, the decals can be switched on and off</li>
 *   <li>vending / snack / drink - 1 token in, one snack out</li>
 *   <li>change machine - 1 emerald in, 4 tokens out</li>
 *   <li>coin return - 4 tokens in, 1 emerald out</li>
 *   <li>ticket counter - 1 token in, 2 tickets out</li>
 *   <li>prize counter - 4 tickets in, a prize out</li>
 *   <li>trash can - voids whatever you click it with</li>
 *   <li>directory - reads out the cabinets around it</li>
 *   <li>stool - right click to sit down</li>
 * </ul>
 *
 * <p>Clicking a machine with the wrong item now always answers with what it
 * wants on the action bar instead of falling through to the light switch.
 */
public final class ArcadeBehavior implements DecorBlock.DecorUse {
   public enum Kind {
      CARPET,
      DECAL_ARROW,
      DECAL_STEPS,
      DECAL_STAR,
      VENDING,
      SNACK,
      DRINK,
      CHANGE,
      COIN_RETURN,
      TICKET,
      PRIZE,
      TRASH,
      DIRECTORY,
      STOOL
   }

   private static final String SEAT_TAG = "wcore_seat";

   private final Kind kind;

   private ArcadeBehavior(Kind kind) {
      this.kind = kind;
   }

   public static ArcadeBehavior of(Kind kind) {
      return new ArcadeBehavior(kind);
   }

   @Override
   public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (stack.isEmpty()) {
         return InteractionResult.PASS;
      }

      boolean blockItem = stack.getItem() instanceof BlockItem;

      switch (this.kind) {
         case TRASH:
            // Blocks still place normally (so you can build around the can);
            // hold shift to feed a block to the trash. Other items are always
            // swallowed.
            if (blockItem && !player.isShiftKeyDown()) {
               return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
               int removed = player.isShiftKeyDown() ? stack.getCount() : 1;
               stack.shrink(removed);
               level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 0.5F);
               if (level instanceof ServerLevel server) {
                  server.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.05);
               }
            }
            return InteractionResult.SUCCESS;

         case CHANGE:
            if (stack.is(Items.EMERALD)) {
               if (!level.isClientSide()) {
                  stack.shrink(1);
                  give(level, pos, player, new ItemStack(WorkingConsolesCore.ARCADE_TOKEN, 4));
                  level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
                  hint(player, "Change machine: +4 Arcade Tokens");
               }
               return InteractionResult.SUCCESS;
            }
            if (blockItem) {
               return InteractionResult.PASS;
            }
            return note(level, player, "Change machine: insert 1 emerald for 4 tokens");

         case COIN_RETURN:
            if (stack.is(WorkingConsolesCore.ARCADE_TOKEN)) {
               if (stack.getCount() >= 4) {
                  if (!level.isClientSide()) {
                     stack.shrink(4);
                     give(level, pos, player, new ItemStack(Items.EMERALD));
                     level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 0.9F);
                     hint(player, "Coin return: +1 Emerald");
                  }
                  return InteractionResult.SUCCESS;
               }
               if (!level.isClientSide()) {
                  hint(player, "Coin return: needs 4 tokens (you have " + stack.getCount() + ")");
               }
               return InteractionResult.SUCCESS;
            }
            if (blockItem) {
               return InteractionResult.PASS;
            }
            return note(level, player, "Coin return: insert 4 tokens for 1 emerald");

         case TICKET:
            if (stack.is(WorkingConsolesCore.ARCADE_TOKEN)) {
               if (!level.isClientSide()) {
                  stack.shrink(1);
                  give(level, pos, player, new ItemStack(WorkingConsolesCore.ARCADE_TICKET, 2));
                  level.playSound(null, pos, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.6F, 1.6F);
                  hint(player, "Ticket counter: +2 Arcade Tickets");
               }
               return InteractionResult.SUCCESS;
            }
            if (blockItem) {
               return InteractionResult.PASS;
            }
            return note(level, player, "Ticket counter: insert 1 token for 2 tickets");

         case PRIZE:
            if (stack.is(WorkingConsolesCore.ARCADE_TICKET)) {
               if (stack.getCount() >= 4) {
                  if (!level.isClientSide()) {
                     stack.shrink(4);
                     ItemStack out = new ItemStack(prize(level));
                     give(level, pos, player, out);
                     level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5F, 1.4F);
                     hint(player, "Prize counter: " + out.getHoverName().getString());
                  }
                  return InteractionResult.SUCCESS;
               }
               if (!level.isClientSide()) {
                  hint(player, "Prize counter: needs 4 tickets (you have " + stack.getCount() + ")");
               }
               return InteractionResult.SUCCESS;
            }
            if (blockItem) {
               return InteractionResult.PASS;
            }
            return note(level, player, "Prize counter: insert 4 tickets for a prize");

         case VENDING:
            return this.dispense(stack, level, pos, player, WorkingConsolesCore.ARCADE_CANDY, "Vending machine");
         case SNACK:
            return this.dispense(stack, level, pos, player, WorkingConsolesCore.ARCADE_CHIPS, "Snack machine");
         case DRINK:
            return this.dispense(stack, level, pos, player, WorkingConsolesCore.ARCADE_SODA, "Drink machine");
         default:
            return InteractionResult.PASS;
      }
   }

   @Override
   public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      // A block in hand means the player is building here; let placement win.
      if (player.getMainHandItem().getItem() instanceof BlockItem) {
         return InteractionResult.PASS;
      }

      switch (this.kind) {
         case DECAL_ARROW:
            if (!level.isClientSide()) {
               level.setBlock(pos, (BlockState)state.setValue(DecorBlock.FACING, ((Direction)state.getValue(DecorBlock.FACING)).getClockWise()), 3);
               level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.6F, 1.0F);
            }
            return InteractionResult.SUCCESS;

         case DECAL_STEPS:
         case DECAL_STAR:
            if (!level.isClientSide()) {
               boolean lit = !(Boolean)state.getValue(DecorBlock.LIT);
               level.setBlock(pos, (BlockState)state.setValue(DecorBlock.LIT, lit), 3);
               level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F, lit ? 1.2F : 0.8F);
            }
            return InteractionResult.SUCCESS;

         case STOOL:
            if (player.isShiftKeyDown()) {
               if (!level.isClientSide()) {
                  hint(player, "Let go of sneak, then click the stool to sit");
               }
               return InteractionResult.SUCCESS;
            }
            if (!level.isClientSide()) {
               hint(player, seat(level, pos, player) ? "Seated - sneak to get up" : "Could not sit here");
            }
            return InteractionResult.SUCCESS;

         case CHANGE:
            return note(level, player, "Change machine: insert 1 emerald for 4 tokens");
         case COIN_RETURN:
            return note(level, player, "Coin return: insert 4 tokens for 1 emerald");
         case TICKET:
            return note(level, player, "Ticket counter: insert 1 token for 2 tickets");
         case PRIZE:
            return note(level, player, "Prize counter: insert 4 tickets for a prize");
         case VENDING:
            return note(level, player, "Vending machine: insert 1 Arcade Token");
         case SNACK:
            return note(level, player, "Snack machine: insert 1 Arcade Token");
         case DRINK:
            return note(level, player, "Drink machine: insert 1 Arcade Token");

         case DIRECTORY:
            if (!level.isClientSide()) {
               describe(level, pos, player);
            }
            return InteractionResult.SUCCESS;

         default:
            return InteractionResult.PASS;
      }
   }

   @Override
   public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
      if (level.isClientSide() || !(entity instanceof Player player) || !(Boolean)state.getValue(DecorBlock.LIT)) {
         return;
      }

      switch (this.kind) {
         case CARPET:
         case DECAL_STEPS:
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false, true));
            break;
         case DECAL_STAR:
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 60, 1, true, false, true));
            break;
         default:
      }
   }

   private InteractionResult dispense(ItemStack stack, Level level, BlockPos pos, Player player, Item item, String label) {
      if (stack.is(WorkingConsolesCore.ARCADE_TOKEN)) {
         if (!level.isClientSide()) {
            stack.shrink(1);
            ItemStack out = new ItemStack(item);
            give(level, pos, player, out);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.2F);
            hint(player, label + ": " + out.getHoverName().getString());
         }
         return InteractionResult.SUCCESS;
      }

      if (stack.getItem() instanceof BlockItem) {
         return InteractionResult.PASS;
      }

      return note(level, player, label + ": insert 1 Arcade Token");
   }

   private static InteractionResult note(Level level, Player player, String text) {
      if (!level.isClientSide()) {
         hint(player, text);
      }
      return InteractionResult.SUCCESS;
   }

   private static void hint(Player player, String text) {
      player.sendOverlayMessage(Component.literal(text));
   }

   private static Item prize(Level level) {
      int roll = level.getRandom().nextInt(8);
      if (roll == 0) {
         return WorkingConsolesCore.ARCADE_PLUSH;
      } else {
         return roll <= 3 ? WorkingConsolesCore.ARCADE_SODA : (roll <= 5 ? WorkingConsolesCore.ARCADE_CHIPS : WorkingConsolesCore.ARCADE_CANDY);
      }
   }

   private static void give(Level level, BlockPos pos, Player player, ItemStack out) {
      if (!player.getInventory().add(out) && level instanceof ServerLevel server) {
         Block.popResource(server, pos.above(), out);
      }
   }

   private static boolean seat(Level level, BlockPos pos, Player player) {
      if (!(level instanceof ServerLevel server) || !player.getPassengers().isEmpty()) {
         return false;
      }

      ArmorStand stand = new ArmorStand(server, pos.getX() + 0.5, pos.getY() + 0.62, pos.getZ() + 0.5);
      stand.setInvisible(true);
      stand.setNoGravity(true);
      stand.setSilent(true);
      stand.setPermanentlyInvulnerable(true);
      stand.setCustomNameVisible(false);
      stand.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, (byte)(ArmorStand.CLIENT_FLAG_MARKER | ArmorStand.CLIENT_FLAG_SMALL));
      stand.addTag(SEAT_TAG);
      server.addFreshEntity(stand);
      return player.startRiding(stand, true, false);
   }

   private static void describe(Level level, BlockPos pos, Player player) {
      int radius = 24;
      int count = 0;
      List<String> names = new ArrayList<>();

      for (BlockPos cell : BlockPos.betweenClosed(pos.offset(-radius, -8, -radius), pos.offset(radius, 8, radius))) {
         if (level.getBlockState(cell).getBlock() instanceof RetroConsoleBlock cabinet) {
            count++;
            if (names.size() < 3 && !names.contains(cabinet.displayName)) {
               names.add(cabinet.displayName);
            }
         }
      }

      if (count == 0) {
         player.sendSystemMessage(Component.literal("Arcade directory: no cabinets within " + radius + " blocks."));
      } else {
         player.sendSystemMessage(
            Component.literal("Arcade directory: " + count + (count == 1 ? " cabinet" : " cabinets") + " nearby - " + String.join(", ", names))
         );
      }
   }
}
