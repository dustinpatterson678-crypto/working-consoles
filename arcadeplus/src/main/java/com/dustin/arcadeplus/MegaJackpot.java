package com.dustin.arcadeplus;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * One jackpot pot shared by every ticket game in the world. Each paying win feeds it, and any win can hit it:
 * the machine pays the whole pot, sirens and lights go off and the whole server hears about it. The pot is
 * saved with the world (on the overworld), so it keeps growing between sessions.
 */
public final class MegaJackpot {
   public static final int SEED = 500;
   /** One paying win in this many hits the jackpot. */
   public static final int ODDS = 300;
   public static final AttachmentType<Integer> POT = AttachmentRegistry.create(ArcadePlusMod.id("mega_jackpot"),
      b -> b.persistent(Codec.INT).initializer(() -> SEED));

   private MegaJackpot() {
   }

   /** Loads the class so the attachment is registered during mod init. */
   public static void init() {
   }

   public static int pot(ServerLevel level) {
      return level.getServer().overworld().getAttachedOrElse(POT, SEED);
   }

   /** Server: feeds the pot from a win of `tickets`; returns the jackpot paid (0 when it didn't hit). */
   static int onWin(ServerLevel level, BlockPos pos, String game, int tickets) {
      ServerLevel home = level.getServer().overworld();
      int pot = pot(level) + Math.max(1, tickets / 5);
      if (level.getRandom().nextInt(ODDS) != 0) {
         home.setAttached(POT, pot);
         return 0;
      }

      home.setAttached(POT, SEED);
      double x = pos.getX() + 0.5, y = pos.getY() + 1.2, z = pos.getZ() + 0.5;
      level.playSound(null, pos, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 1.5F, 1.0F);
      level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 2.0F, 1.0F);
      level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 200, 0.6, 1.0, 0.6, 0.5);
      level.sendParticles(ParticleTypes.FIREWORK, x, y + 0.5, z, 120, 0.8, 1.2, 0.8, 0.2);
      level.getServer().getPlayerList().broadcastSystemMessage(
         Component.literal("\u2605 MEGA JACKPOT! " + game + " just paid " + pot + " tickets! \u2605").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
         false);
      AttendantEntity.cheerNear(level, pos, "JACKPOT!! " + pot + " tickets! Everybody look!");
      return pot;
   }
}
