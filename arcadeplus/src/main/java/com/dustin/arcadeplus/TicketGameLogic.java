package com.dustin.arcadeplus;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** Server-side rules for the five redemption games. Tickets and plushes are the pack's own wcore items. */
public final class TicketGameLogic {
   private TicketGameLogic() {
   }

   static Item item(String path) {
      return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("wcore", path));
   }

   /** Pays for a play: an Arcade Token or a Nickel (nickel-arcade style). */
   public static boolean isToken(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() == ArcadePlusMod.NICKEL;
   }

   /** Right-click on the machine. Returns true if the click was used. */
   public static boolean use(Level level, BlockPos pos, TicketGameBlockEntity be, Player player, ItemStack held, Vec3 hit) {
      TicketGame g = be.game();
      if (player.isShiftKeyDown() && held.isEmpty()) {   // sneak + empty hand: read the high-score board
         if (!level.isClientSide()) HighScores.show(player, be);
         return true;
      }
      long now = level.getGameTime();
      boolean busy = be.playing || be.stage != 0;
      if (g == TicketGame.COINPUSHER) {
         if (!isToken(held)) {
            tell(player, "Coin Cascade: drop in a nickel", ChatFormatting.GOLD);
            return true;
         }
         take(player, held);
         be.actionTick = now;
         RandomSource r = level.getRandom();
         int roll = r.nextInt(1000);
         int tickets = roll < 3 ? 100 : roll < 20 ? 10 : roll < 70 ? 5 : roll < 190 ? 2 : roll < 520 ? 1 : 0;   // mostly 0-3, rarely big
         be.lastResult = tickets;
         be.sync();
         sound(level, pos, SoundEvents.CHAIN_PLACE, 1.6F);
         if (tickets > 0) {
            pay(level, pos, tickets);
            tell(player, tickets >= 100 ? "JACKPOT! " + tickets + " points!" : "The pusher shoves " + tickets + " point" + (tickets > 1 ? "s" : "") + " over the edge!", ChatFormatting.YELLOW);
         } else {
            tell(player, "So close... the pile is right at the edge", ChatFormatting.GRAY);
         }
         return true;
      }
      if (!busy) {
         if (!isToken(held)) {
            tell(player, g.title + ": insert a nickel to play", ChatFormatting.GOLD);
            return true;
         }
         take(player, held);
         be.playing = g != TicketGame.CLAWMACHINE;
         be.stage = g == TicketGame.CLAWMACHINE ? 1 : 0;
         be.score = 0;
         be.player = player.getName().getString();
         be.shots = 0;
         be.startTick = now;
         be.actionTick = -1000;
         be.moleMask = 0;
         be.input = 0;
         for (int i = 0; i < 4; i++) {
            be.shotTick[i] = -1000;
            be.shotCounted[i] = true;
         }
         be.clawX = -5F;
         be.clawZ = -5F;
         java.util.Arrays.fill(be.stackRows, 0);
         if (g == TicketGame.BIGWHEEL) {
            be.playing = false;
            be.stage = 1;                                  // spinning
         }
         if (g == TicketGame.DRUMWHEEL) {
            be.playing = false;
            be.stage = 1;                                  // waiting for the pull
         }
         if (g == TicketGame.ECHO) {
            be.playing = false;
            be.stage = 1;                                  // machine shows the pattern
            be.actionTick = now + 10;
            be.lastAim = level.getRandom().nextInt();
         }
         if (g == TicketGame.CYCLONE) {
            long day = level.getOverworldClockTime() / 24000L;
            if (be.seedDay >= 0 && day - be.seedDay >= 3) {   // three in-game days later the jackpot is back to full
               be.jackpotSeed = 200;
               be.jackpot = Math.max(be.jackpot, 200);
               be.seedDay = -1;
            }
            be.jackpot = Math.min(400, be.jackpot + 1);   // every play feeds the pool a little
         }
         if (g == TicketGame.POWERPUNCH || g == TicketGame.DEEPSEA) {
            be.playing = false;
            be.stage = 1;                                  // meter sweeping / fish circling
            be.lastResult = -1;
         }
         if (g == TicketGame.BEATBLASTER) {
            be.lastAim = level.getRandom().nextInt();      // this round's beat chart
            for (int i = 0; i < 4; i++) be.shotTick[i] = -1000;
         }
         if (g == TicketGame.PLINKO || g == TicketGame.STRIKER || g == TicketGame.LOCKER) {
            be.playing = false;
            be.stage = 1;                                  // aiming / meter running / light hopping
         }
         if (g == TicketGame.LOCKER) {
            be.lastAim = level.getRandom().nextInt(TicketGame.LOCKERS);   // which locker holds the prize this time
         }
         if (g == TicketGame.HOCKEY) {
            be.stage = 1;
            hockeyReset(be, -1);
         }
         if (g == TicketGame.KEYVAULT || g == TicketGame.DROPZONE) {
            be.playing = false;
            be.stage = 1;
            be.lastResult = -1;
         }
         if (g == TicketGame.ROCKETRACE || g == TicketGame.SNIPIT) {
            be.playing = false;
            be.stage = 1;
            be.lastResult = -1;
         }
         if (g == TicketGame.LANES) {
            be.stage = 1;
            be.lastAim = 0;
            be.lastAim2 = 0;
         }
         if (g == TicketGame.METEOR) {
            java.util.Arrays.fill(be.stackRows, -1);
         }
         if (g == TicketGame.POOL) {
            be.playing = false;
            be.stage = 1;
            be.lastResult = 0;
            PoolSim.rack(be.pool);
         }
         if (g == TicketGame.SPEEDLIGHTS) {
            be.lastResult = level.getRandom().nextInt(9);
            be.moleUntil[0] = now + 30;
         }
         be.sync();
         sound(level, pos, SoundEvents.NOTE_BLOCK_BELL.value(), 1.2F);
         tell(player, switch (g) {
            case PLINKO -> "Plinko Drop: 3 pucks. Right-click to drop. Middle slot = 4, all 3 in the middle = 50 bonus";
            case STRIKER -> "High Striker: right-click when the meter is at the very top. Ring the bell for 50!";
            case POWERPUNCH -> "Power Punch: 3 punches. Right-click when the meter peaks. 950+ is a KNOCKOUT worth 25 bonus!";
            case DEEPSEA -> "Deep Sea Catch: 5 casts. Right-click to drop the hook on a fish with its mouth open. Gold fish = 10!";
            case BEATBLASTER -> "Beat Blaster: 30 seconds! Right-click a drum as its light hits the bottom line. Dead on the beat scores double";
            case HOCKEY -> "Air Hockey: 60 seconds against the machine. Move with WASD / arrows. First to 7 wins!";
            case POOL -> "Pool: new rack! Look along the shot, move the mouse to fine-tune the aim, hold click/Space for power, let go to shoot. Pot every ball, the 8 last";
            case ROCKETRACE -> "Rocket Race: right-click to boost - stop the meter near the top for a big boost. Beat the three machine rockets!";
            case LANES -> "Galaxy Lanes: 5 balls. Aim with the mouse, hold click/Space for power, let go to roll";
            case METEOR -> "Meteor Strike: 30 seconds! Right-click to fire when the cannon is under a falling meteor";
            case SNIPIT -> "Snip It: right-click to close the scissors. Cut the swaying string and the prize is yours!";
            case KEYVAULT -> "Key Vault: right-click to stop the key across, then again to stop it going up. It must line up exactly with a lock!";
            case SPEEDLIGHTS -> "Speed Lights: 30 seconds! Right-click the lit button as fast as you can. Wrong button costs a point";
            case DROPZONE -> "Drop Zone: right-click to drop the ball onto the spinning disc. Land in the gold hole for 25!";
            case LOCKER -> "Lucky Locker: right-click to stop the light on the GOLD locker to win the prize inside";
            case SKEEBALL -> "Alley Roller: 9 balls. Right-click to roll when the meter is right";
            case BASKETBALL -> "Hoop Frenzy: 30 seconds! Aim with the mouse, hold click/Space for power, let go to throw";
            case WHACKAMOLE -> "Mole Bonk: 30 seconds! Right-click the moles";
            case STACKER -> "Tower Stack: right-click to stop each row on top of the last. Green line = minor prize, top = MAJOR";
            case GATORS -> "Swamp Smash: 30 seconds! Right-click the gators before they bite";
            case BIGWHEEL -> "Big Wheel: right-click to hit STOP. It coasts 2 and a half turns, so time it!";
            case ECHO -> "Color Echo: watch the lights, then click the pads in the same order";
            case DRUMWHEEL -> "Bonus Drum: right-click to pull the handle. Try to win the 250!";
            case CLOWNS -> "Clown Knockdown: 30 seconds! Right-click a clown to throw a ball at it";
            case CYCLONE -> "Light Chaser: right-click to stop the light. Red lamp = JACKPOT " + be.jackpot;
            default -> "Claw: 20 seconds. Move with WASD/arrows, Space or click to drop";
         }, ChatFormatting.GREEN);
         return true;
      }
      switch (g) {
         case SKEEBALL -> {
            if (now - be.actionTick < 22) return true;      // previous ball still rolling
            int pts = TicketGame.skeeScore(TicketGame.skeePower(now));
            be.lastResult = pts;
            be.actionTick = now;
            be.score += pts;
            be.shots++;
            be.sync();
            sound(level, pos, SoundEvents.WOOD_HIT, 0.8F);
            tell(player, (pts > 0 ? "+" + pts : "Gutter!") + "   Score " + be.score + "   Balls left " + (9 - be.shots), pts >= 50 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA);
            if (be.shots >= 9) {
               finish(level, pos, be, be.score / 80 + (be.score >= 450 ? 5 : 0) + (be.score >= 700 ? 15 : 0));
            }
         }
         case BASKETBALL -> {
            // throwing happens from the game screen (TicketInputPacket); a plain click just reminds
            tell(player, "Hoop Frenzy: aim with the mouse, hold click/Space, let go to throw", ChatFormatting.AQUA);
         }
         case WHACKAMOLE -> {
            double[] local = toLocal(be, aimAt(be, player, hit, 15.0));
            for (int i = 0; i < 5; i++) {
               double dx = local[0] - TicketGame.HOLES[i][0], dz = local[1] - TicketGame.HOLES[i][1];
               if (dx * dx + dz * dz < 2.4 * 2.4) {
                  if ((be.moleMask & (1 << i)) != 0) {
                     be.moleMask &= ~(1 << i);
                     be.score++;
                     be.lastResult = i;
                     be.actionTick = now;
                     be.sync();
                     sound(level, pos, SoundEvents.SLIME_SQUISH, 1.2F);
                     tell(player, "BONK! Moles " + be.score, ChatFormatting.GREEN);
                  }
                  break;
               }
            }
         }
         case CLAWMACHINE -> {
            if (be.stage == 1) {
               drop(level, pos, be);
            }
         }
         case STACKER -> stack(level, pos, be, player);
         case DRUMWHEEL -> {
            if (be.stage == 1) {
               // real odds, like the cabinet: the drum always lands where the machine picked
               int roll = level.getRandom().nextInt(1000);
               int target;
               if (roll < 3) target = 15;                                            // 250
               else if (roll < 30) target = 3;                                       // 25
               else if (roll < 80) target = 7;                                       // 10
               else if (roll < 140) target = level.getRandom().nextBoolean() ? 11 : 19;   // 15 / 10
               else {
                  int[] small = {0, 1, 2, 4, 5, 6, 8, 9, 10, 12, 13, 14, 16, 17, 18};
                  target = small[level.getRandom().nextInt(small.length)];
               }
               be.lastAim = (int)Math.round((be.lastAim / 10000.0) % (Math.PI * 2) * 10000);
               be.lastResult = target;
               be.stage = 3;
               be.actionTick = now;
               be.sync();
               sound(level, pos, SoundEvents.PISTON_CONTRACT, 0.8F);
            }
         }
         case CLOWNS -> {
            if (now - be.actionTick < 6) return true;      // one ball at a time
            double[] ray = localRayZ(be, player, TicketGame.CLOWN_Z);
            if (ray == null) return true;
            double t = now - be.startTick;
            int hitIdx = -2;
            for (int i = 0; i < 4; i++) {
               if ((be.moleMask & (1 << i)) == 0 && Math.abs(ray[0] - TicketGame.clownX(i, t)) < 1.4 && Math.abs(ray[1] - (TicketGame.CLOWN_Y + 1.7)) < 2.2) {
                  hitIdx = i;
                  break;
               }
            }
            be.actionTick = now;
            be.lastAim = (int)Math.round(ray[0] * 100);
            be.lastAim2 = (int)Math.round(ray[1] * 100);
            be.lastResult = hitIdx;
            if (hitIdx >= 0) {
               be.moleMask |= 1 << hitIdx;
               be.moleUntil[hitIdx] = now + 40;
               be.score++;
               sound(level, pos, SoundEvents.WOOD_HIT, 1.3F);
               if (be.moleMask == 15) {
                  be.score += 10;
                  tell(player, "ALL FOUR DOWN! +10 bonus   Clowns " + be.score, ChatFormatting.LIGHT_PURPLE);
                  sound(level, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.5F);
               } else {
                  tell(player, "Down goes the clown! " + be.score, ChatFormatting.GREEN);
               }
            }
            be.sync();
         }
         case GATORS -> {
            double[] local = toLocal(be, aimAt(be, player, hit, 13.0));
            for (int i = 0; i < 5; i++) {
               if (Math.abs(local[0] - TicketGame.GATOR_X[i]) < 1.4 && (be.moleMask & (1 << i)) != 0) {
                  be.moleMask &= ~(1 << i);
                  be.score++;
                  be.lastResult = i;
                  be.actionTick = now;
                  be.sync();
                  sound(level, pos, SoundEvents.WOOD_HIT, 0.7F);
                  tell(player, "WHACK! Gators " + be.score, ChatFormatting.GREEN);
                  break;
               }
            }
         }
         case BIGWHEEL -> {
            if (be.stage == 1) {
               double a0 = TicketGame.wheelSpin(now - be.startTick) + (level.getRandom().nextDouble() - 0.5) * 2.4;   // the wheel never stops exactly where you aim
               be.lastAim = (int)Math.round((a0 % (Math.PI * 2)) * 10000);
               be.stage = 3;
               be.actionTick = now;
               be.sync();
               sound(level, pos, SoundEvents.NOTE_BLOCK_HAT.value(), 1.0F);
            }
         }
         case ECHO -> {
            if (be.stage != 2) return true;
            double[] local = toLocal(be, aimAt(be, player, hit, 11.4));
            int q = (local[0] > 0 ? 0 : 1) + (local[1] > 0 ? 2 : 0);
            be.moleUntil[0] = now;                         // pad flash
            be.lastResult = q;
            if (q != TicketGame.echoColor(be.lastAim, be.shots)) {
               be.stage = 0;
               be.actionTick = now;
               be.lastResult = -1;
               be.sync();
               int tickets = echoTickets(be.score);
               tell(player, "Wrong pad! You reached level " + be.score + "  ->  " + tickets + " points", ChatFormatting.YELLOW);
               sound(level, pos, SoundEvents.NOTE_BLOCK_BASS.value(), 0.5F);
               if (tickets > 0) pay(level, pos, tickets);
               return true;
            }
            sound(level, pos, SoundEvents.NOTE_BLOCK_PLING.value(), new float[]{0.7F, 0.9F, 1.1F, 1.3F}[q]);
            be.shots++;
            be.actionTick = now;
            if (be.shots > be.score) {                     // finished this round's pattern
               be.score++;
               be.shots = 0;
               be.stage = 1;
               be.actionTick = now + 14;
               if (be.score >= 15) {
                  be.stage = 0;
                  pay(level, pos, 50);
                  tell(player, "ECHO MASTER! Level 15: 50 points!", ChatFormatting.LIGHT_PURPLE);
                  sound(level, pos, SoundEvents.PLAYER_LEVELUP, 1.0F);
               } else {
                  tell(player, "Level " + be.score + "!", ChatFormatting.GREEN);
               }
            }
            be.sync();
         }
         case CYCLONE -> {
            int lamp = TicketGame.cycLamp(now - be.startTick);
            int r = level.getRandom().nextInt(10);            // the light coasts a lamp or two after STOP
            lamp = (lamp + (r < 4 ? 0 : r < 8 ? 1 : 2)) % TicketGame.CYC_LAMPS;
            if (lamp == 0 && level.getRandom().nextInt(10) < 7) lamp = 1;   // the jackpot lamp is a tease: the light usually creeps past it
            be.lastResult = lamp;
            be.actionTick = now;
            be.playing = false;
            int tickets;
            if (lamp == 0) {
               tickets = be.jackpot;
               if (be.seedDay < 0) be.seedDay = level.getOverworldClockTime() / 24000L;
               be.jackpotSeed = Math.max(50, be.jackpotSeed / 2);   // each win shrinks the next jackpot
               be.jackpot = be.jackpotSeed;
               tell(player, "JACKPOT!!! " + tickets + " points!", ChatFormatting.LIGHT_PURPLE);
               sound(level, pos, SoundEvents.PLAYER_LEVELUP, 1.0F);
               sound(level, pos, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0F);
            } else {
               tickets = TicketGame.CYC_VALUES[lamp];
               tell(player, (tickets >= 10 ? "BONUS! " : tickets == 0 ? "So close... " : "") + tickets + " point" + (tickets == 1 ? "" : "s"), tickets >= 5 ? ChatFormatting.GREEN : ChatFormatting.AQUA);
               sound(level, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F + tickets / 20F);
            }
            be.sync();
            pay(level, pos, tickets);
         }
         case PLINKO -> {
            if (be.stage == 1 && now - be.actionTick > 4) {
               be.lastAim2 = (int)Math.round(TicketGame.plinkoX(now - be.startTick) * 100);
               be.lastAim = level.getRandom().nextInt();
               be.stage = 2;
               be.actionTick = now;
               be.sync();
               sound(level, pos, SoundEvents.WOOD_PLACE, 1.6F);
            }
         }
         case POWERPUNCH -> {
            if (be.stage == 1 && now - be.actionTick > 6) {
               double meter = TicketGame.punchMeter(now - be.startTick);
               int power = TicketGame.punchPower(meter);
               power = Math.max(0, Math.min(999, power + level.getRandom().nextInt(31) - 15));
               be.lastResult = power;
               be.stage = 2;
               be.actionTick = now;
               be.sync();
               sound(level, pos, SoundEvents.PLAYER_ATTACK_STRONG, 0.6F + power / 1400F);
               sound(level, pos, SoundEvents.WOOL_HIT, 0.7F);
            }
         }
         case DEEPSEA -> {
            if (be.stage == 1 && now - be.actionTick > 4) {
               be.lastResult = TicketGame.fishAtHook(now - be.startTick);
               be.lastAim2 = (int)(now - be.startTick);
               be.stage = 2;
               be.actionTick = now;
               be.sync();
               sound(level, pos, SoundEvents.FISHING_BOBBER_THROW, 1.0F);
            }
         }
         case BEATBLASTER -> {
            if (!be.playing || now - be.actionTick < 2) return true;
            double[] local = toLocal(be, aimAt(be, player, hit, 14.9));
            int pad = -1;
            for (int i = 0; i < 3; i++) {
               double dx = local[0] - TicketGame.BB_PADS[i][0], dz = local[1] - TicketGame.BB_PADS[i][1];
               if (dx * dx + dz * dz < 2.6 * 2.6) pad = i;
            }
            if (pad < 0) return true;
            be.actionTick = now;
            be.input = pad + 1;
            be.moleUntil[pad] = now + 4;
            long t = now - be.startTick;
            int b = (int)Math.round((t - TicketGame.BEAT_START) / (double)TicketGame.BEAT);
            int got = 0;
            for (int bb = b - 1; bb <= b + 1; bb++) {
               if (bb < 0 || TicketGame.noteLane(be.lastAim, bb) != pad || be.shotTick[pad] >= bb) continue;
               long off = Math.abs(t - TicketGame.beatTime(bb));
               if (off <= 3) {
                  got = off <= 1 ? 2 : 1;
                  be.shotTick[pad] = bb;
                  break;
               }
            }
            be.score += got;
            be.sync();
            sound(level, pos, pad == 0 ? SoundEvents.NOTE_BLOCK_SNARE.value() : pad == 1 ? SoundEvents.NOTE_BLOCK_BASEDRUM.value() : SoundEvents.NOTE_BLOCK_HAT.value(), got == 2 ? 1.2F : 1.0F);
         }
         case STRIKER -> {
            if (be.stage == 1) {
               int off = TicketGame.strikerOff(now - be.startTick);
               if (level.getRandom().nextInt(100) < 35) off = Math.min(TicketGame.STRIKER_PERIOD / 2, off + 1);   // the puck doesn't always fly true
               be.lastResult = off;
               be.stage = 2;
               be.actionTick = now;
               be.sync();
               sound(level, pos, SoundEvents.ANVIL_LAND, 1.5F);
            }
         }
         case ROCKETRACE -> {
            if (be.stage == 1 && now - be.actionTick >= TicketGame.ROCKET_COOLDOWN) {
               int boost = TicketGame.rocketBoost(TicketGame.rocketMeter(now - be.startTick));
               be.stackRows[0] = Math.min(TicketGame.RACE_LEN, be.stackRows[0] + boost);
               be.lastResult = boost;
               be.actionTick = now;
               be.sync();
               sound(level, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.8F + boost * 0.2F);
            }
         }
         case POOL -> {
            if (be.stage == 1 && level.getBlockEntity(pos) instanceof TicketGameBlockEntity) {
               tell(player, "Pool: " + Integer.bitCount(~be.moleMask & 0xFFFE) + " balls left, " + be.shots + " shots. Click with nothing in hand to line up a shot", ChatFormatting.AQUA);
            }
         }
         case LANES -> tell(player, "Galaxy Lanes: aim with the mouse, hold click/Space, let go to roll", ChatFormatting.AQUA);
         case METEOR -> {
            if (now - be.actionTick < 4) return true;
            long t = now - be.startTick;
            int col = TicketGame.cannonCol(t);
            be.actionTick = now;
            if (be.stackRows[col] >= 0) {
               be.stackRows[col] = -1;
               be.score++;
               be.lastResult = col;
               sound(level, pos, SoundEvents.GENERIC_EXPLODE.value(), 1.8F);
            } else {
               be.lastResult = -1 - col;
               sound(level, pos, SoundEvents.SNOWBALL_THROW, 1.4F);
            }
            be.sync();
         }
         case SNIPIT -> {
            if (be.stage == 1) {
               double t = now - be.startTick;
               double x = TicketGame.scissorX(t), sx = TicketGame.stringX(t);
               be.lastAim = (int)Math.round(x * 1000);
               be.lastAim2 = (int)Math.round(sx * 1000);
               boolean cut = Math.abs(x - sx) < 0.07 && level.getRandom().nextInt(100) < 75;   // even a good cut sometimes slips
               be.lastResult = cut ? level.getRandom().nextInt(TicketGame.SNIP_PRIZES.length) : -1;
               be.stage = 2;
               be.actionTick = now;
               be.sync();
               sound(level, pos, SoundEvents.SHEEP_SHEAR, 1.2F);
            }
         }
         case KEYVAULT -> {
            if (now - be.actionTick < 4) return true;
            if (be.stage == 1) {
               be.lastAim = (int)TicketGame.tri(now - be.startTick, TicketGame.KEY_XSTEPS);
               be.stage = 2;
               be.startTick = now;
               be.actionTick = now;
               be.sync();
               sound(level, pos, SoundEvents.NOTE_BLOCK_HAT.value(), 1.2F);
            } else if (be.stage == 2) {
               keyStop(level, pos, be, now);
            }
         }
         case SPEEDLIGHTS -> {
            if (now - be.actionTick < 2) return true;
            double[] local = toLocal(be, aimAt(be, player, hit, 14.5));
            int best = -1;
            double bd = 1.9 * 1.9;
            for (int i = 0; i < 9; i++) {
               double dx = local[0] - TicketGame.SL_BUTTONS[i][0], dz = local[1] - TicketGame.SL_BUTTONS[i][1];
               if (dx * dx + dz * dz < bd) {
                  bd = dx * dx + dz * dz;
                  best = i;
               }
            }
            if (best < 0) return true;
            be.actionTick = now;
            if (best == be.lastResult) {
               be.score++;
               int next = level.getRandom().nextInt(8);
               be.lastResult = next >= be.lastResult ? next + 1 : next;
               be.moleUntil[0] = now + Math.max(10, 30 - (now - be.startTick) / 30);
               sound(level, pos, SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F + Math.min(1F, be.score / 60F));
            } else {
               be.score = Math.max(0, be.score - 1);
               sound(level, pos, SoundEvents.NOTE_BLOCK_BASS.value(), 0.6F);
            }
            be.sync();
         }
         case DROPZONE -> {
            if (be.stage == 1) {
               long tl = now - be.startTick + TicketGame.DZ_FALL;
               int sector = TicketGame.dzSector(TicketGame.dzAngle(tl));
               int r = level.getRandom().nextInt(10);
               if (r < 2) sector = (sector + 1) % TicketGame.DZ_VALUES.length;            // the ball skips off the rim
               else if (r < 3) sector = (sector + TicketGame.DZ_VALUES.length - 1) % TicketGame.DZ_VALUES.length;
               be.lastResult = sector;
               be.stage = 2;
               be.actionTick = now;
               be.sync();
               sound(level, pos, SoundEvents.WOOD_PLACE, 1.4F);
            }
         }
         case LOCKER -> {
            if (be.stage == 1) {
               int step = (int)Math.floor((now - be.startTick) / 2.5);
               if (level.getRandom().nextInt(10) >= 5) step++;   // the light often hops one more
               be.lastResult = TicketGame.LOCKER_ORDER[step % TicketGame.LOCKERS];
               be.stage = 2;
               be.actionTick = now;
               be.sync();
               sound(level, pos, SoundEvents.NOTE_BLOCK_HAT.value(), 1.0F);
            }
         }
         default -> {
         }
      }
      return true;
   }

   private static void keyStop(Level level, BlockPos pos, TicketGameBlockEntity be, long now) {
      int ys = (int)TicketGame.tri(now - be.startTick, TicketGame.KEY_YSTEPS);
      be.lastAim2 = ys;
      int col = -1, row = -1;
      for (int c = 0; c < 5; c++) if (TicketGame.KEY_COLS[c] == be.lastAim) col = c;
      for (int r = 0; r < 3; r++) if (Math.abs(TicketGame.KEY_ROWS[r] - ys) <= 1) row = r;
      be.lastResult = col >= 0 && row >= 0 ? row * 5 + col : -1;
      be.stage = 3;
      be.actionTick = now;
      be.sync();
      sound(level, pos, SoundEvents.NOTE_BLOCK_HAT.value(), 1.4F);
   }

   private static final String[] LOCKER_PRIZES = {"bunny_plush", "cat_plush", "dino_plush", "duck_plush", "mystery_box"};

   /** Puck back to the middle of the side that just let a goal in (side -1 = yours, +1 = the machine's). */
   private static void hockeyReset(TicketGameBlockEntity be, int side) {
      float[] h = be.hk;
      h[0] = 0F;
      h[1] = side * 2.2F;
      h[2] = 0F;
      h[3] = 0F;
      h[4] = 0F;
      h[5] = (float)(-TicketGame.HK_L + 1.6);
      h[6] = 0F;
      h[7] = (float)(TicketGame.HK_L - 1.4);
   }

   private static void hockeyTick(Level level, BlockPos pos, TicketGameBlockEntity be, long now) {
      float[] h = be.hk;
      double W = TicketGame.HK_W, L = TicketGame.HK_L, PR = TicketGame.HK_PUCK, MR = TicketGame.HK_MALLET;
      // your mallet (model +x is your left)
      double dx = ((be.input & 1) != 0 ? 1 : 0) - ((be.input & 2) != 0 ? 1 : 0);
      double dz = ((be.input & 4) != 0 ? 1 : 0) - ((be.input & 8) != 0 ? 1 : 0);
      double len = Math.hypot(dx, dz);
      double pvx = 0, pvz = 0;
      if (len > 0) {
         pvx = dx / len * 0.5;
         pvz = dz / len * 0.5;
      }
      double mx = clamp(h[4] + pvx, -W + MR, W - MR), mz = clamp(h[5] + pvz, -L + MR, -0.5);
      pvx = mx - h[4];
      pvz = mz - h[5];
      h[4] = (float)mx;
      h[5] = (float)mz;
      // the machine's mallet: defends its goal, then charges when the puck comes to its side
      double t = now - be.startTick;
      double speed = 0.30 + Math.min(0.10, t / 1200.0 * 0.10);
      double tx, tz;
      if (h[1] > 0 && h[7] > h[1]) {
         tx = h[0];
         tz = h[1];
      } else if (h[1] > 0) {
         tx = h[0] + (h[0] > h[6] ? -2.0 : 2.0);
         tz = h[1] + 1.8;
      } else {
         tx = h[0] * 0.6;
         tz = L - 1.4;
      }
      double ax = tx - h[6], az = tz - h[7], al = Math.hypot(ax, az);
      double avx = 0, avz = 0;
      if (al > 0.01) {
         avx = ax / al * Math.min(speed, al);
         avz = az / al * Math.min(speed, al);
      }
      double nx = clamp(h[6] + avx, -W + MR, W - MR), nz = clamp(h[7] + avz, 0.5, L - MR);
      avx = nx - h[6];
      avz = nz - h[7];
      h[6] = (float)nx;
      h[7] = (float)nz;
      // the puck, in small steps so it never jumps through a mallet
      double px = h[0], pz = h[1], vx = h[2], vz = h[3];
      for (int s = 0; s < 4; s++) {
         px += vx / 4;
         pz += vz / 4;
         for (int k = 0; k < 2; k++) {
            double cx = k == 0 ? h[4] : h[6], cz = k == 0 ? h[5] : h[7], cvx = k == 0 ? pvx : avx, cvz = k == 0 ? pvz : avz;
            double ddx = px - cx, ddz = pz - cz, d = Math.hypot(ddx, ddz);
            if (d < PR + MR && d > 0.0001) {
               double ux = ddx / d, uz = ddz / d;
               px = cx + ux * (PR + MR);
               pz = cz + uz * (PR + MR);
               double rvx = vx - cvx, rvz = vz - cvz, vn = rvx * ux + rvz * uz;
               if (vn < 0) {
                  vx -= 1.85 * vn * ux;
                  vz -= 1.85 * vn * uz;
                  vx += cvx * 0.6;
                  vz += cvz * 0.6;
                  sound(level, pos, SoundEvents.WOOD_HIT, 1.8F);
               }
            }
         }
         if (Math.abs(px) > W - PR) {
            px = Math.signum(px) * (W - PR);
            vx = -vx * 0.9;
            sound(level, pos, SoundEvents.WOOD_HIT, 1.4F);
         }
         if (pz < -L + PR || pz > L - PR) {
            boolean yourEnd = pz < 0;
            if (Math.abs(px) < TicketGame.HK_GOAL) {
               if (yourEnd) be.shots++;
               else be.score++;
               be.actionTick = now;
               be.lastResult = yourEnd ? 2 : 1;
               sound(level, pos, yourEnd ? SoundEvents.NOTE_BLOCK_BASS.value() : SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F);
               hockeyReset(be, yourEnd ? -1 : 1);
               be.sync();
               return;
            }
            pz = Math.signum(pz) * (L - PR);
            vz = -vz * 0.9;
            sound(level, pos, SoundEvents.WOOD_HIT, 1.4F);
         }
      }
      double sp = Math.hypot(vx, vz);
      if (sp > 1.3) {
         vx *= 1.3 / sp;
         vz *= 1.3 / sp;
      }
      h[0] = (float)px;
      h[1] = (float)pz;
      h[2] = (float)(vx * 0.993);
      h[3] = (float)(vz * 0.993);
      be.sync();
   }

   private static double clamp(double v, double lo, double hi) {
      return v < lo ? lo : v > hi ? hi : v;
   }

   /** A throw or a claw drop sent from the game screen. */
   public static void action(Level level, BlockPos pos, TicketGameBlockEntity be, Player player, double aim, double power) {
      long now = level.getGameTime();
      TicketGame g = be.game();
      if (g == TicketGame.BASKETBALL && be.playing && now - be.startTick < g.roundTicks && now - be.actionTick >= 6) {
         int i = be.shotNext;
         if (now - be.shotTick[i] < 60 && !be.shotCounted[i]) {
            return;                                     // all four balls still in the air
         }
         be.shotNext = (i + 1) % 4;
         be.shotTick[i] = now;
         be.shotAim[i] = (int)Math.round(aim * 1000);
         be.shotPow[i] = (int)Math.round(power * 1000);
         be.shotAt[i] = now - be.startTick;
         be.shotCounted[i] = false;
         be.actionTick = now;
         be.shots++;
         be.sync();
         sound(level, pos, SoundEvents.WOOL_PLACE, 1.4F);
      } else if (g == TicketGame.CLAWMACHINE && be.stage == 1) {
         drop(level, pos, be);
      } else if (g == TicketGame.POOL && be.stage == 1 && !PoolSim.moving(be.pool, be.moleMask) && (be.moleMask & 1) == 0) {
         double sp = 0.04 + Math.max(0, Math.min(1, power)) * 0.5;
         be.pool[2] = (float)(Math.cos(aim) * sp);
         be.pool[3] = (float)(Math.sin(aim) * sp);
         be.lastAim = (int)Math.round(aim * 1000);
         be.lastResult = 1;                                              // balls rolling
         be.shots++;
         be.actionTick = now;
         be.sync();
         sound(level, pos, SoundEvents.WOOD_HIT, 1.8F);
      } else if (g == TicketGame.LANES && be.playing && be.shots < TicketGame.LANE_BALLS && now - be.actionTick >= TicketGame.LANE_ROLL + 8) {
         double x = aim * 3.0 + (level.getRandom().nextDouble() - 0.5) * 0.8;          // the ball hooks a little
         int mask = TicketGame.pinsDown(x, power, level.getRandom().nextLong());
         be.lastAim = (int)Math.round(x * 100);
         be.lastAim2 = (int)Math.round(power * 1000);
         be.moleMask = mask;
         be.actionTick = now;
         be.shots++;
         be.score += Integer.bitCount(mask);
         if (mask == 1023) be.stackRows[0]++;
         be.sync();
         sound(level, pos, SoundEvents.WOOD_PLACE, 0.6F);
      }
   }

   private static void stack(Level level, BlockPos pos, TicketGameBlockEntity be, Player player) {
      long now = level.getGameTime();
      if (now - be.actionTick < 4) return;            // one stop per click
      int row = be.shots;
      int prevStart = row == 0 ? 0 : be.stackRows[row - 1] & 15, prevWidth = row == 0 ? 3 : be.stackRows[row - 1] >> 4;
      int width = Math.min(prevWidth, TicketGame.stackMaxWidth(row));
      int p = TicketGame.stackPos(now - be.startTick, row, width);
      int start = p, end = p + width;
      if (row > 0) {
         start = Math.max(p, prevStart);
         end = Math.min(p + width, prevStart + prevWidth);
      }
      be.actionTick = now;
      if (end <= start) {
         be.lastResult = -1;
         be.playing = false;
         be.sync();
         tell(player, "Missed! You reached row " + row, ChatFormatting.GRAY);
         sound(level, pos, SoundEvents.NOTE_BLOCK_BASS.value(), 0.6F);
         int t = row >= TicketGame.STACK_MINOR ? 40 : row < 3 ? 0 : Math.min(8, 2 + (row - 3));   // early 0, mid 5-15, past the minor 75
         if (t > 0) {
            pay(level, pos, t);
            tell(player, t + " points", ChatFormatting.YELLOW);
         }
         return;
      }
      be.stackRows[row] = start | (end - start) << 4;
      be.shots = row + 1;
      be.startTick = now;
      be.lastResult = row;
      sound(level, pos, SoundEvents.NOTE_BLOCK_PLING.value(), 0.8F + row * 0.1F);
      if (row == TicketGame.STACK_MINOR) {
         tell(player, "MINOR LEVEL! 40 points are locked in. Keep going for the 500 MAJOR!", ChatFormatting.GREEN);
      } else if (row == TicketGame.STACK_ROWS - 1) {
         be.playing = false;
         pay(level, pos, 500);
         Block.popResourceFromFace(level, pos, facing(level, pos), new ItemStack(item("arcade_plush")));
         tell(player, "MAJOR PRIZE!!! 500 points and a plush!", ChatFormatting.LIGHT_PURPLE);
         sound(level, pos, SoundEvents.PLAYER_LEVELUP, 1.0F);
      }
      be.sync();
   }

   private static void drop(Level level, BlockPos pos, TicketGameBlockEntity be) {
      long now = level.getGameTime();
      be.stage = 3;
      be.input = 0;
      be.actionTick = now;
      be.lastAim = Math.round(be.clawX * 100);
      be.lastAim2 = Math.round(be.clawZ * 100);
      double best = 99;
      int idx = 0;
      for (int i = 0; i < TicketGame.PRIZES.length; i++) {
         double d = Math.hypot(be.clawX - TicketGame.PRIZES[i][0], be.clawZ - TicketGame.PRIZES[i][1]);
         if (d < best) {
            best = d;
            idx = i;
         }
      }
      // dead centre grabs most of the time, the edge of a plush sometimes, nothing almost never
      float chance = best < 0.7 ? 0.45F : best < 1.4 ? 0.2F : best < 2.0 ? 0.05F : 0.0F;
      be.lastResult = level.getRandom().nextFloat() < chance ? 1 + idx : 0;
      be.sync();
      sound(level, pos, SoundEvents.PISTON_EXTEND, 1.4F);
   }

   static void tick(Level level, BlockPos pos, TicketGameBlockEntity be) {
      long now = level.getGameTime();
      TicketGame g = be.game();
      if (be.payLeft > 0 && now % 2 == 0) {
         int n = Math.max(1, Math.min(be.payLeft, be.payLeft / 30));
         be.payLeft -= n;
         Block.popResourceFromFace(level, pos, facing(level, pos), new ItemStack(item("arcade_ticket"), n));
         level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.25F, 1.9F);
         be.setChanged();
      }
      if (g == TicketGame.BIGWHEEL && be.stage == 3 && now - be.actionTick >= TicketGame.WHEEL_STOP_TICKS) {
         double fin = TicketGame.wheelCoast(be.lastAim / 10000.0, TicketGame.WHEEL_STOP_TICKS);
         int seg = TicketGame.wheelSegment(fin);
         int tickets = TicketGame.WHEEL_VALUES[seg];
         be.stage = 0;
         be.lastResult = seg;
         be.actionTick = now;
         be.sync();
         pay(level, pos, tickets);
         tellNear(level, pos, tickets >= 250 ? "BIG WIN!!! " + tickets + " points!" : tickets >= 25 ? "Nice spin! " + tickets + " points" : tickets + " points", tickets >= 25 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA);
         sound(level, pos, tickets >= 25 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F);
         return;
      }
      if (g == TicketGame.PLINKO && be.stage == 2 && now - be.actionTick >= TicketGame.plinkoTicks()) {
         double[] xs = TicketGame.plinkoPath(be.lastAim2 / 100.0, be.lastAim);
         int slot = TicketGame.plinkoSlot(xs[TicketGame.PLINKO_ROWS]);
         int tickets = TicketGame.PLINKO_SLOTS[slot];
         be.lastResult = slot;
         be.shots++;
         if (slot == TicketGame.PLINKO_SLOTS.length / 2) be.moleMask++;
         be.score += tickets;
         be.actionTick = now;
         if (be.shots >= TicketGame.PLINKO_PUCKS) {
            be.stage = 0;
            if (be.moleMask >= TicketGame.PLINKO_PUCKS) {
               tickets += 50;
               be.score += 50;
               tellNear(level, pos, "ALL THREE IN THE MIDDLE!!! 50 point bonus!", ChatFormatting.LIGHT_PURPLE);
               sound(level, pos, SoundEvents.PLAYER_LEVELUP, 1.0F);
            }
            tellNear(level, pos, "Plinko Drop over: " + be.score + " points", ChatFormatting.YELLOW);
         } else {
            be.stage = 1;
            tellNear(level, pos, tickets + " point" + (tickets == 1 ? "" : "s") + "  (" + (TicketGame.PLINKO_PUCKS - be.shots) + " pucks left)", tickets >= 4 ? ChatFormatting.GREEN : ChatFormatting.AQUA);
         }
         be.sync();
         sound(level, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8F + tickets / 10F);
         pay(level, pos, tickets);
         return;
      }
      if (g == TicketGame.POWERPUNCH && be.stage == 2 && now - be.actionTick >= 30) {
         be.shots++;
         be.score += be.lastResult;
         if (be.lastResult >= 950) be.moleMask++;
         be.actionTick = now;
         tellNear(level, pos, (be.lastResult >= 950 ? "KNOCKOUT! " : be.lastResult >= 700 ? "Big hit! " : "") + be.lastResult + " power  (" + (TicketGame.PUNCHES - be.shots) + " left)", be.lastResult >= 950 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA);
         if (be.shots >= TicketGame.PUNCHES) {
            be.stage = 0;
            int pts = be.score / 60 + be.moleMask * 25;
            be.sync();
            pay(level, pos, pts);
            tellNear(level, pos, "Power Punch over: " + be.score + " total power  ->  " + pts + " points", ChatFormatting.YELLOW);
            sound(level, pos, be.moleMask > 0 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F);
         } else {
            be.stage = 1;
            be.sync();
         }
         return;
      }
      if (g == TicketGame.DEEPSEA && be.stage == 2 && now - be.actionTick == TicketGame.HOOK_DROP) {
         sound(level, pos, be.lastResult >= 0 ? SoundEvents.FISHING_BOBBER_SPLASH : SoundEvents.FISHING_BOBBER_RETRIEVE, 1.0F);
      }
      if (g == TicketGame.DEEPSEA && be.stage == 2 && now - be.actionTick >= 32) {
         be.shots++;
         int v = be.lastResult >= 0 ? TicketGame.FISH_VALUES[be.lastResult] : 0;
         if (v > 0) be.moleMask++;
         be.score += v;
         be.actionTick = now;
         tellNear(level, pos, (v >= 10 ? "THE GOLDEN FISH! " : v > 0 ? "Caught one! " : "Nothing bit... ") + v + "  (" + (TicketGame.CASTS - be.shots) + " casts left)", v >= 10 ? ChatFormatting.LIGHT_PURPLE : v > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY);
         if (be.shots >= TicketGame.CASTS) {
            be.stage = 0;
            int pts = be.score + (be.moleMask >= TicketGame.CASTS ? 10 : 0);
            be.sync();
            pay(level, pos, pts);
            tellNear(level, pos, "Deep Sea Catch over: " + pts + " points" + (be.moleMask >= TicketGame.CASTS ? " (perfect haul bonus!)" : ""), ChatFormatting.YELLOW);
            sound(level, pos, pts >= 15 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F);
         } else {
            be.stage = 1;
            be.lastResult = -1;
            be.sync();
         }
         return;
      }
      if (g == TicketGame.STRIKER && be.stage == 2 && now - be.actionTick >= 34) {
         int tickets = TicketGame.strikerTickets(be.lastResult);
         be.stage = 0;
         be.actionTick = now;
         be.sync();
         pay(level, pos, tickets);
         if (be.lastResult == 0) {
            tellNear(level, pos, "DING DING DING! You rang the bell: 50 points!", ChatFormatting.LIGHT_PURPLE);
            sound(level, pos, SoundEvents.BELL_BLOCK, 1.0F);
            sound(level, pos, SoundEvents.PLAYER_LEVELUP, 1.0F);
         } else {
            tellNear(level, pos, (tickets >= 5 ? "Strong! " : tickets == 0 ? "Weak swing... " : "") + tickets + " point" + (tickets == 1 ? "" : "s"), tickets >= 5 ? ChatFormatting.GREEN : ChatFormatting.AQUA);
         }
         return;
      }
      if (g == TicketGame.STRIKER && be.stage == 2 && now - be.actionTick == 14 && be.lastResult == 0) {
         sound(level, pos, SoundEvents.BELL_BLOCK, 1.0F);
      }
      if (g == TicketGame.LOCKER && be.stage == 2 && now - be.actionTick >= 30) {
         be.stage = 0;
         be.actionTick = now;
         be.sync();
         if (be.lastResult == be.lastAim) {
            String id = LOCKER_PRIZES[level.getRandom().nextInt(LOCKER_PRIZES.length)];
            Item prize = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("arcadeplus", id));
            if (prize == null || prize == net.minecraft.world.item.Items.AIR) prize = item("arcade_plush");
            Block.popResourceFromFace(level, pos, facing(level, pos), new ItemStack(prize));
            pay(level, pos, 10);
            tellNear(level, pos, "LUCKY LOCKER! You won the prize inside and 10 points!", ChatFormatting.LIGHT_PURPLE);
            sound(level, pos, SoundEvents.PLAYER_LEVELUP, 1.0F);
         } else {
            int tickets = level.getRandom().nextInt(2);
            pay(level, pos, tickets);
            tellNear(level, pos, "Empty locker... " + tickets + " point" + (tickets == 1 ? "" : "s"), ChatFormatting.GRAY);
            sound(level, pos, SoundEvents.IRON_DOOR_CLOSE, 1.2F);
         }
         return;
      }
      if (g == TicketGame.POOL && be.stage == 1) {
         if (be.lastResult != 1) return;
         int before = be.moleMask;
         int r = PoolSim.step(be.pool, before);
         int after = r & 0xFFFF;
         if ((r & (1 << 17)) != 0) sound(level, pos, SoundEvents.STONE_HIT, 1.9F);
         if ((r & (1 << 16)) != 0 && now % 3 == 0) sound(level, pos, SoundEvents.WOOL_HIT, 1.2F);
         if (after != before) sound(level, pos, SoundEvents.WOOD_PLACE, 0.7F);
         be.moleMask = after;
         if (!PoolSim.moving(be.pool, be.moleMask)) {
            be.lastResult = 0;
            int left = Integer.bitCount(~be.moleMask & 0xFFFE);
            if ((be.moleMask & (1 << 8)) != 0 && left > 0) {
               be.stage = 0;
               tellNear(level, pos, "The 8-ball went down early - rack over after " + be.shots + " shots. Put in a nickel to rack again", ChatFormatting.RED);
            } else if (left == 0) {
               be.stage = 0;
               tellNear(level, pos, "TABLE CLEARED in " + be.shots + " shots!", ChatFormatting.LIGHT_PURPLE);
               sound(level, pos, SoundEvents.PLAYER_LEVELUP, 1.0F);
               pay(level, pos, Math.max(10, 120 - be.shots * 5));
            } else if ((be.moleMask & 1) != 0) {
               be.moleMask &= ~1;                                          // scratch: the cue ball comes back to the head spot
               be.pool[0] = 0F;
               be.pool[1] = (float)(-PoolSim.V / 2);
               be.pool[2] = be.pool[3] = 0F;
               tellNear(level, pos, "Scratch! The cue ball is back on the spot", ChatFormatting.YELLOW);
            }
         }
         be.sync();
         return;
      }
      if (g == TicketGame.ROCKETRACE && be.stage == 1) {
         boolean changed = false;
         for (int i = 0; i < 3; i++) {
            if (level.getRandom().nextDouble() < TicketGame.ROCKET_AI[i]) {
               be.stackRows[i + 1] = Math.min(TicketGame.RACE_LEN, be.stackRows[i + 1] + 1);
               changed = true;
            }
         }
         int you = be.stackRows[0];
         boolean done = you >= TicketGame.RACE_LEN || now - be.startTick > 1600;
         for (int i = 1; i <= 3; i++) done |= be.stackRows[i] >= TicketGame.RACE_LEN;
         if (done) {
            int place = 1;
            for (int i = 1; i <= 3; i++) if (be.stackRows[i] > you) place++;
            int tickets = place == 1 ? 15 : place == 2 ? 4 : place == 3 ? 1 : 0;
            be.stage = 0;
            be.lastResult = place;
            be.actionTick = now;
            be.sync();
            pay(level, pos, tickets);
            tellNear(level, pos, (place == 1 ? "YOU WIN THE RACE! " : "You finished #" + place + ". ") + tickets + " point" + (tickets == 1 ? "" : "s"), place == 1 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA);
            sound(level, pos, place == 1 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F);
            return;
         }
         if (changed) be.sync();
         return;
      }
      if (g == TicketGame.LANES && be.playing && be.shots >= TicketGame.LANE_BALLS && now - be.actionTick >= TicketGame.LANE_ROLL + 20) {
         be.stage = 0;
         int tickets = be.score / 8 + be.stackRows[0] * 2;
         tellNear(level, pos, "Galaxy Lanes: " + be.score + " pins, " + be.stackRows[0] + " strike" + (be.stackRows[0] == 1 ? "" : "s"), ChatFormatting.GOLD);
         finish(level, pos, be, tickets);
         return;
      }
      if (g == TicketGame.LANES && be.playing && be.shots > 0 && now - be.actionTick == TicketGame.LANE_ROLL) {
         int n = Integer.bitCount(be.moleMask);
         sound(level, pos, SoundEvents.WOOD_BREAK, 0.9F + n * 0.03F);
         tellNear(level, pos, n == 10 ? "STRIKE!" : n + " pins", n == 10 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA);
      }
      if (g == TicketGame.SNIPIT && be.stage == 2 && now - be.actionTick >= 40) {
         be.stage = 0;
         be.actionTick = now;
         be.sync();
         if (be.lastResult >= 0) {
            Item prize = BuiltInRegistries.ITEM.getValue(Identifier.parse(TicketGame.SNIP_PRIZES[be.lastResult]));
            if (prize == null || prize == net.minecraft.world.item.Items.AIR) prize = item("arcade_plush");
            Block.popResourceFromFace(level, pos, facing(level, pos), new ItemStack(prize));
            tellNear(level, pos, "SNIP! The prize dropped - it's yours!", ChatFormatting.LIGHT_PURPLE);
            sound(level, pos, SoundEvents.PLAYER_LEVELUP, 1.0F);
         } else {
            tellNear(level, pos, "Missed the string...", ChatFormatting.GRAY);
         }
         return;
      }
      if (g == TicketGame.KEYVAULT) {
         if ((be.stage == 1 || be.stage == 2) && now - be.startTick > 400) {   // walked away: the key stops by itself
            if (be.stage == 1) {
               be.lastAim = (int)TicketGame.tri(now - be.startTick, TicketGame.KEY_XSTEPS);
               be.startTick = now;
               be.stage = 2;
               be.sync();
            } else {
               keyStop(level, pos, be, now);
            }
         }
         if (be.stage == 3 && now - be.actionTick >= 40) {
            be.stage = 0;
            be.actionTick = now;
            be.sync();
            if (be.lastResult >= 0) {
               Item prize = BuiltInRegistries.ITEM.getValue(Identifier.parse(TicketGame.KEY_PRIZES[be.lastResult]));
               if (prize == null || prize == net.minecraft.world.item.Items.AIR) prize = item("arcade_plush");
               Block.popResourceFromFace(level, pos, facing(level, pos), new ItemStack(prize));
               tellNear(level, pos, "CLICK! The key turned - you won the prize!", ChatFormatting.LIGHT_PURPLE);
               sound(level, pos, SoundEvents.PLAYER_LEVELUP, 1.0F);
            } else {
               tellNear(level, pos, "The key missed the lock...", ChatFormatting.GRAY);
               sound(level, pos, SoundEvents.NOTE_BLOCK_BASS.value(), 0.6F);
            }
         }
         return;
      }
      if (g == TicketGame.DROPZONE && be.stage == 2 && now - be.actionTick >= TicketGame.DZ_FALL + 12) {
         int tickets = TicketGame.DZ_VALUES[be.lastResult];
         be.stage = 0;
         be.actionTick = now;
         be.sync();
         pay(level, pos, tickets);
         tellNear(level, pos, tickets >= 25 ? "GOLD HOLE!!! 25 points!" : tickets == 0 ? "Bounced off the rim..." : tickets + " point" + (tickets == 1 ? "" : "s"), tickets >= 25 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA);
         sound(level, pos, tickets >= 25 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F);
         return;
      }
      if (g == TicketGame.HOCKEY && be.playing) {
         long t = now - be.startTick;
         if (t >= g.roundTicks || be.score >= 7 || be.shots >= 7) {
            be.stage = 0;
            be.input = 0;
            int tickets = be.score + (be.score > be.shots ? 5 : 0);
            tellNear(level, pos, (be.score > be.shots ? "YOU WIN! " : be.score == be.shots ? "Tie game. " : "The machine wins. ") + be.score + " - " + be.shots, ChatFormatting.GOLD);
            finish(level, pos, be, tickets);
            return;
         }
         if (now - be.actionTick >= 20) hockeyTick(level, pos, be, now);   // short pause after a goal
         return;
      }
      if (g == TicketGame.DRUMWHEEL && be.stage == 3 && now - be.actionTick >= TicketGame.DRUM_TICKS + 4) {
         int tickets = TicketGame.DRUM_VALUES[be.lastResult];
         be.stage = 0;
         be.lastAim = (int)Math.round((TicketGame.drumAngle(be.lastAim / 10000.0, be.lastResult, TicketGame.DRUM_TICKS) % (Math.PI * 2)) * 10000);
         be.actionTick = now;
         be.sync();
         pay(level, pos, tickets);
         tellNear(level, pos, tickets >= 500 ? "SUPER BONUS!!! " + tickets + " POINTS!" : tickets >= 25 ? "BIG WIN! " + tickets + " points!" : tickets + " points", tickets >= 25 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA);
         sound(level, pos, tickets >= 25 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F);
         return;
      }
      if (g == TicketGame.CLOWNS && be.playing) {
         boolean changed = false;
         for (int i = 0; i < 4; i++) {
            if ((be.moleMask & (1 << i)) != 0 && now >= be.moleUntil[i]) {
               be.moleMask &= ~(1 << i);                    // clown pops back up
               changed = true;
            }
         }
         if (changed) be.sync();
      }
      if (g == TicketGame.ECHO) {
         if (be.stage == 1 && now - be.actionTick >= (be.score + 1) * TicketGame.ECHO_STEP) {
            be.stage = 2;                                  // your turn
            be.actionTick = now;
            be.sync();
         } else if (be.stage == 2 && now - be.actionTick > 60) {
            be.stage = 0;                                  // too slow
            be.lastResult = -1;
            be.sync();
            int tickets = echoTickets(be.score);
            tellNear(level, pos, "Time's up! Level " + be.score + "  ->  " + tickets + " points", ChatFormatting.YELLOW);
            if (tickets > 0) pay(level, pos, tickets);
         }
         return;
      }
      if (g == TicketGame.CLAWMACHINE && be.stage == 1) {
         float dx = ((be.input & 1) != 0 ? 1 : 0) - ((be.input & 2) != 0 ? 1 : 0);   // model +x is the player's left
         float dz = ((be.input & 4) != 0 ? 1 : 0) - ((be.input & 8) != 0 ? 1 : 0);
         if (dx != 0 || dz != 0) {
            be.clawX = Math.max(-6F, Math.min(6F, be.clawX + dx * 0.3F));
            be.clawZ = Math.max(-6F, Math.min(6F, be.clawZ + dz * 0.3F));
            be.sync();
         }
         if (now - be.startTick >= 400) {
            drop(level, pos, be);
         }
         return;
      }
      if (g == TicketGame.BASKETBALL && be.playing) {
         for (int i = 0; i < 4; i++) {
            if (!be.shotCounted[i]) {
               double[] r = TicketGame.shotResult(be.shotAim[i] / 1000.0, be.shotPow[i] / 1000.0, be.shotAt[i]);
               if (now - be.shotTick[i] >= r[1]) {
                  be.shotCounted[i] = true;
                  if (r[0] == 1) {
                     be.score++;
                     be.lastResult = 1;
                     be.actionTick = now;
                     sound(level, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.4F);
                  }
                  be.sync();
               }
            }
         }
         if (now - be.startTick >= g.roundTicks + 60) {
            int tickets = be.score / 6 + (be.score >= 20 ? 3 : 0) + (be.score >= 30 ? 10 : 0);
            finish(level, pos, be, tickets);
         }
         return;
      }
      if (g == TicketGame.CLAWMACHINE && be.stage == 3 && now - be.actionTick >= 40) {
         be.stage = 0;
         be.clawX = -5F;
         be.clawZ = -5F;
         be.sync();
         if (be.lastResult >= 1) {
            Block.popResourceFromFace(level, pos, facing(level, pos), new ItemStack(item("arcade_plush")));
            tellNear(level, pos, "You won a plush!", ChatFormatting.LIGHT_PURPLE);
            sound(level, pos, SoundEvents.PLAYER_LEVELUP, 1.2F);
         } else {
            tellNear(level, pos, "The claw slipped... so close!", ChatFormatting.GRAY);
         }
         return;
      }
      if (!be.playing || g.roundTicks <= 0) {
         return;
      }
      long t = now - be.startTick;
      if (t >= g.roundTicks) {
         int tickets = g == TicketGame.BASKETBALL ? be.score / 6 + (be.score >= 20 ? 3 : 0) + (be.score >= 30 ? 10 : 0)
            : g == TicketGame.CLOWNS ? be.score / 6 + (be.score >= 30 ? 5 : 0) + (be.score >= 45 ? 15 : 0)
            : g == TicketGame.METEOR ? be.score / 4 + (be.score >= 25 ? 5 : 0) + (be.score >= 35 ? 10 : 0)
            : g == TicketGame.SPEEDLIGHTS ? be.score / 10 + (be.score >= 45 ? 5 : 0) + (be.score >= 60 ? 15 : 0)
            : g == TicketGame.BEATBLASTER ? be.score / 3 + (be.score >= 60 ? 10 : 0) + (be.score >= 80 ? 20 : 0)
            : be.score / 8 + (be.score >= 30 ? 5 : 0) + (be.score >= 45 ? 15 : 0);
         finish(level, pos, be, tickets);
         return;
      }
      if (g == TicketGame.BEATBLASTER && t >= TicketGame.BEAT_START - 2 * TicketGame.BEAT && (t - TicketGame.BEAT_START) % TicketGame.BEAT == 0) {
         int b = (int)((t - TicketGame.BEAT_START) / TicketGame.BEAT);
         level.playSound(null, pos, b % 4 == 0 ? SoundEvents.NOTE_BLOCK_BASEDRUM.value() : SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, b % 4 == 0 ? 0.5F : 0.25F, 1.0F);
      }
      if (g == TicketGame.METEOR) {
         boolean changed = false;
         double fall = TicketGame.meteorFall(t);
         int active = 0;
         for (int c = 0; c < TicketGame.METEOR_COLS; c++) {
            if (be.stackRows[c] >= 0 && t - be.stackRows[c] > fall) {
               be.stackRows[c] = -1;                                      // it hit the ground
               changed = true;
            }
            if (be.stackRows[c] >= 0) active++;
         }
         if (active < 2 + t / 200 && level.getRandom().nextInt(6) == 0) {
            int c = level.getRandom().nextInt(TicketGame.METEOR_COLS);
            if (be.stackRows[c] < 0) {
               be.stackRows[c] = (int)t;
               changed = true;
            }
         }
         if (changed) be.sync();
         return;
      }
      if (g == TicketGame.SPEEDLIGHTS && now > be.moleUntil[0]) {
         int next = level.getRandom().nextInt(8);
         be.lastResult = next >= be.lastResult ? next + 1 : next;            // too slow: the light jumps
         be.moleUntil[0] = now + Math.max(10, 30 - t / 30);
         be.sync();
         return;
      }
      if (g == TicketGame.WHACKAMOLE || g == TicketGame.GATORS) {
         boolean changed = false;
         for (int i = 0; i < 5; i++) {
            if ((be.moleMask & (1 << i)) != 0 && now > be.moleUntil[i]) {
               be.moleMask &= ~(1 << i);
               changed = true;
            }
         }
         RandomSource r = level.getRandom();
         int speedUp = (int)(t / 150);                      // moles get quicker each 10 seconds
         if (Integer.bitCount(be.moleMask) < 2 + speedUp / 2 && r.nextInt(Math.max(3, 9 - speedUp * 2)) == 0) {
            int i = r.nextInt(5);
            if ((be.moleMask & (1 << i)) == 0) {
               be.moleMask |= 1 << i;
               be.moleUntil[i] = now + Math.max(6, 20 - speedUp * 4 + r.nextInt(8));
               changed = true;
            }
         }
         if (changed) {
            be.sync();
         }
      }
   }

   /** Simon-style echo: nothing for the first few levels, then a slow climb. */
   static int echoTickets(int level) {
      return level < 4 ? 0 : level < 7 ? 1 : level < 10 ? 2 + (level - 7) : 5;
   }

   private static void finish(Level level, BlockPos pos, TicketGameBlockEntity be, int tickets) {
      be.playing = false;
      HighScores.record(level, pos, be);
      be.moleMask = 0;
      be.sync();
      if (tickets > 0) {
         pay(level, pos, tickets);
      }
      tellNear(level, pos, be.game().title + " over! Score " + be.score + "  ->  " + tickets + " point" + (tickets == 1 ? "" : "s"), ChatFormatting.YELLOW);
      sound(level, pos, tickets > 0 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F);
   }

   /** Points become paper tickets at the operator's rate (like a real arcade: a nickel play pays a ticket or two). */
   public static final double TICKET_RATE = 0.1;

   public static int toTickets(Level level, int points) {
      double t = points * TICKET_RATE;
      int whole = (int)Math.floor(t);
      return whole + (level.getRandom().nextDouble() < t - whole ? 1 : 0);
   }

   public static void pay(Level level, BlockPos pos, int points) {
      if (points <= 0) return;
      int tickets = toTickets(level, points);
      tellNear(level, pos, points + " point" + (points == 1 ? "" : "s") + "  ->  " + tickets + " ticket" + (tickets == 1 ? "" : "s"), tickets > 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY);
      if (tickets <= 0) return;
      if (level instanceof net.minecraft.server.level.ServerLevel sl) {
         String game = level.getBlockState(pos).getBlock() instanceof TicketGameBlock b ? b.game.title : "An arcade game";
         int jackpot = MegaJackpot.onWin(sl, pos, game, tickets);
         com.dustin.arcadeplus.api.ArcadeApi.TICKETS_WON.invoker().onWin(sl, pos, game, tickets + jackpot, jackpot > 0);
         if (jackpot > 0) {
            tickets += jackpot;
         } else if (tickets >= 25) {
            AttendantEntity.cheerNear(sl, pos, "Wow, " + tickets + " tickets on " + game + "!");
         } else {
            tellNear(level, pos, "Mega Jackpot: " + MegaJackpot.pot(sl) + " tickets", ChatFormatting.DARK_AQUA);
         }
      }
      if (level.getBlockEntity(pos) instanceof TicketGameBlockEntity be) {
         be.payLeft += tickets;                          // the ticket mouth spits them out, clicking
         return;
      }
      Item ticket = item("arcade_ticket");
      Direction f = facing(level, pos);
      while (tickets > 0) {
         int n = Math.min(64, tickets);
         Block.popResourceFromFace(level, pos, f, new ItemStack(ticket, n));
         tickets -= n;
      }
   }

   static Direction facing(Level level, BlockPos pos) {
      return level.getBlockState(pos).getBlock() instanceof TicketGameBlock ? level.getBlockState(pos).getValue(TicketGameBlock.FACING) : Direction.NORTH;
   }

   /**
    * Where the player is really pointing on a surface lower than the block's hit box: follow the look ray
    * down to that height (model pixels). Without this, clicks land on the box's top edge, not the pad.
    */
   private static Vec3 aimAt(TicketGameBlockEntity be, Player player, Vec3 hit, double surfacePx) {
      Vec3 eye = player.getEyePosition(), dir = player.getViewVector(1.0F);
      double y = be.getBlockPos().getY() + surfacePx / 16.0;
      if (Math.abs(dir.y) < 1e-4) return hit;
      double t = (y - eye.y) / dir.y;
      if (t <= 0 || t > 8) return hit;
      return eye.add(dir.scale(t));
   }

   /** The player's look ray crossing the vertical plane z = zPx (model pixels): returns {x, y} in model pixels. */
   private static double[] localRayZ(TicketGameBlockEntity be, Player player, double zPx) {
      BlockPos p = be.getBlockPos();
      Vec3 eye = player.getEyePosition(), dir = player.getViewVector(1.0F);
      double[] e = toLocal(be, eye), f = toLocal(be, eye.add(dir));
      double ey = (eye.y - p.getY()) * 16.0, fy = (eye.y + dir.y - p.getY()) * 16.0;
      double dz = f[1] - e[1];
      if (Math.abs(dz) < 1e-6) return null;
      double t = (zPx - e[1]) / dz;
      if (t <= 0 || t > 8) return null;
      return new double[]{e[0] + (f[0] - e[0]) * t, ey + (fy - ey) * t};
   }

   /** World hit point -> model pixels (x, z), with the machine's front at -z. */
   private static double[] toLocal(TicketGameBlockEntity be, Vec3 hit) {
      BlockPos p = be.getBlockPos();
      double wx = (hit.x - p.getX() - 0.5) * 16.0, wz = (hit.z - p.getZ() - 0.5) * 16.0;
      double a = Math.toRadians(180.0 - facing(be.getLevel(), p).toYRot());
      double c = Math.cos(a), s = Math.sin(a);
      return new double[]{c * wx - s * wz, s * wx + c * wz};
   }

   private static void take(Player player, ItemStack held) {
      if (!player.isCreative()) {
         held.shrink(1);
      }
   }

   private static void sound(Level level, BlockPos pos, SoundEvent s, float pitch) {
      level.playSound(null, pos, s, SoundSource.BLOCKS, 0.8F, pitch);
   }

   static void tell(Player player, String text, ChatFormatting color) {
      player.sendOverlayMessage(Component.literal(text).withStyle(color));
   }

   static void tellNear(Level level, BlockPos pos, String text, ChatFormatting color) {
      for (Player p : level.players()) {
         if (p instanceof ServerPlayer && p.distanceToSqr(Vec3.atCenterOf(pos)) < 12 * 12) {
            tell(p, text, color);
         }
      }
   }
}
