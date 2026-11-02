package com.dustin.arcadebuild;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Bigger on the inside: Dustin's Arcade is a modest storefront, but its front doors open into the Grand Hall,
 * a huge arcade in its own pocket dimension. Each building gets its own hall, built the first time someone
 * walks in. Layout coordinates: lx (+ = your right as you walk in), ly (0 = floor slab), lz (0 = just inside
 * the doors). The hall faces south, so world x = origin x - lx and world z = origin z + lz.
 */
public final class GrandHall {
   private GrandHall() {
   }

   public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, ArcadeBuildMod.id("grand_hall"));
   public static final int HX = 25, DEPTH = 64, CEIL = 13, LOUNGE = 51, ARCH = 3, ARCH_H = 5;
   /** Bumped when the hall's design changes: older halls are rebuilt the next time someone walks in. */
   public static final int VERSION = 7;
   /** The hall's core block (draws the room, sends you back out) sits in the front wall above the doors. */
   public static final int CORE_LX = 0, CORE_LY = 6, CORE_LZ = -1;

   /** Where a building's hall lives: spread out so every building has room for its own. */
   public static BlockPos origin(BlockPos facade) {
      return new BlockPos(facade.getX() * 4, 64, facade.getZ() * 4);
   }

   public static BlockPos core(BlockPos origin) {
      return origin.offset(-CORE_LX, CORE_LY, CORE_LZ);
   }

   /** Players who just walked out, and when: they can't be pulled straight back in by the doors. */
   private static final java.util.Map<java.util.UUID, Long> JUST_LEFT = new java.util.HashMap<>();

   /** Walk in through the storefront doors. */
   public static void enter(ServerPlayer player, FacadeBlockEntity facade) {
      Long left = JUST_LEFT.get(player.getUUID());
      long now = player.level().getGameTime();
      if (left != null && now - left < 60 && now >= left) return;
      ServerLevel hall = player.level().getServer().getLevel(LEVEL);
      if (hall == null) {
         player.sendSystemMessage(Component.literal("The Grand Hall isn't available (datapack dimension missing).").withStyle(ChatFormatting.RED));
         return;
      }
      BlockPos o = origin(facade.getBlockPos());
      BlockPos core = core(o);
      hall.getChunkAt(core);   // load the hall's chunk first: an unloaded chunk reads as empty and would rebuild the whole hall over itself
      if (hall.getBlockState(core).getBlock() != ArcadeBuildMod.HALL_CORE || !(hall.getBlockEntity(core) instanceof HallCoreBlockEntity old) || old.version < VERSION) {
         player.sendSystemMessage(Component.literal("Opening the Grand Hall for the first time...").withStyle(ChatFormatting.AQUA));
         BuildJob.runHall(hall, o, player);
      }
      if (hall.getBlockEntity(core) instanceof HallCoreBlockEntity hc) {
         hc.version = VERSION;
         hc.setReturn(player.level().dimension(), facade.getBlockPos(), facade.getBlockState().getValue(FacadeBlock.FACING));
      }
      Vec3 spawn = new Vec3(o.getX() + 0.5, o.getY() + 1, o.getZ() + 2.5);
      player.teleport(new TeleportTransition(hall, spawn, Vec3.ZERO, 0F, player.getXRot(), TeleportTransition.DO_NOTHING));
      hall.playSound(null, BlockPos.containing(spawn), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
   }

   /** Walk out through the hall's front doors, back onto the sidewalk. */
   public static void exit(ServerPlayer player, HallCoreBlockEntity core) {
      ServerLevel back = core.returnLevel();
      if (back == null) back = player.level().getServer().getLevel(Level.OVERWORLD);
      BlockPos f = core.returnPos();
      Direction in = core.returnFacing();
      Vec3 out;
      if (f != null && back.getBlockEntity(f) instanceof FacadeBlockEntity fb) {
         BlockPos c = fb.cell(0, 1, -4);
         out = new Vec3(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
      } else {
         BlockPos sp = back.getRespawnData().pos();
         out = new Vec3(sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5);
      }
      player.teleport(new TeleportTransition(back, out, Vec3.ZERO, in.getOpposite().toYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
      JUST_LEFT.put(player.getUUID(), back.getGameTime());
   }

   public static Identifier hallTexture(String name) {
      return ArcadeBuildMod.id("textures/hall/" + name + ".png");
   }
}
