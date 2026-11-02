package com.dustin.arcadeplus;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The table: the simulation lives on the server, and a small snapshot is sent every tick for drawing. */
public class PinballBlockEntity extends com.dustin.wcore.block.ConsoleBlockEntity implements GeoBlockEntity {
   public static final int LEFT = 1, RIGHT = 2, PLUNGER = 4;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final PinballSim sim;
   private boolean simReady;
   public int input;
   // snapshot (synced)
   public boolean playing;
   public float x = 13.2F, y = 1.3F, aL = (float)PinballSim.REST, aR = (float)PinballSim.REST, charge;
   public int score, balls, targets, flash, highScore, rollovers, mult = 1;
   public boolean kickback;
   public int drops, hold;
   public float spin;
   public boolean newHigh;
   // client-only light timers (game time of the last hit), so lamps glow for a moment instead of one tick
   public final long[] hitAt = new long[16];
   public long bonusAt = -100, drainAt = -100;
   // client-side previous snapshot, for smooth movement between ticks
   public float px = 13.2F, py = 1.3F, paL = (float)PinballSim.REST, paR = (float)PinballSim.REST;

   public PinballBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.sim = new PinballSim(PinballLayout.of(state.getBlock() instanceof PinballBlock pb ? pb.theme : "classic"));
   }

   /** This table's playfield layout. */
   public PinballLayout layout() {
      return this.sim.L;
   }

   public void start(Player player) {
      this.sim.reset();
      this.sim.newBall();
      this.simReady = true;
      this.playing = true;
      this.newHigh = false;
      this.input = 0;
      this.snapshot();
      TicketGameLogic.tell(player, "Pinball: 3 balls. Hold Space to pull the plunger, A/D or Shift keys for the flippers", ChatFormatting.GREEN);
   }

   @Override
   public String consoleName() {
      return "Pinball";
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, PinballBlockEntity be) {
      com.dustin.wcore.block.ConsoleBlockEntity.serverTick(level, pos, state, be);   // outlet power, like every console
      if (be.playing && !be.isPowered()) {
         be.playing = false;                        // pulled the plug mid-game
         be.input = 0;
         be.snapshot();
         return;
      }
      if (!be.playing) {
         return;
      }
      if (!be.simReady) {
         // a table saved mid-game picks up again with a fresh ball at the plunger
         be.sim.score = be.score;
         be.sim.balls = Math.max(1, be.balls);
         be.sim.targets = be.targets;
         be.sim.rollovers = be.rollovers;
         be.sim.mult = Math.max(1, be.mult);
         be.sim.kickback = be.kickback;
         be.sim.newBall();
         be.simReady = true;
      }
      int pts = be.sim.tick((be.input & LEFT) != 0, (be.input & RIGHT) != 0, (be.input & PLUNGER) != 0);
      if (be.sim.flash != 0) {
         level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.5F, 1.2F + (be.sim.flash & 7) * 0.15F);
      }
      if (be.sim.drained && be.sim.ballTime < 160 && !be.sim.saveUsed) {
         be.sim.saveUsed = true;
         // ball save: a ball lost in the first 8 seconds comes back
         be.sim.newBall();
         level.players().stream().filter(pl -> pl.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 64)
            .forEach(pl -> TicketGameLogic.tell(pl, "Ball saved!", ChatFormatting.AQUA));
      }
      if (be.sim.drained) {
         int paid = be.sim.payBonus();
         if (paid > 0) {
            level.players().stream().filter(pl -> pl.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 64)
               .forEach(pl -> TicketGameLogic.tell(pl, "End of ball bonus: " + paid, ChatFormatting.YELLOW));
         }
         be.sim.balls--;
         be.sim.saveUsed = false;
         level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.8F, 0.6F);
         if (be.sim.balls > 0) {
            be.sim.newBall();
         } else {
            be.playing = false;
            be.input = 0;
            int score = be.sim.score;
            if (score > be.highScore) {
               be.highScore = score;
               be.newHigh = true;
               level.players().stream().filter(pl -> pl.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 64)
                  .forEach(pl -> TicketGameLogic.tell(pl, "NEW HIGH SCORE! " + score, ChatFormatting.GOLD));
            }
            int tickets = score >= 200000 ? 100 : score >= 100000 ? 40 : score >= 50000 ? 15 : score >= 25000 ? 6 : score >= 12000 ? 3 : score >= 6000 ? 2 : score >= 2500 ? 1 : 0;   // score milestones
            tickets = TicketGameLogic.toTickets(level, tickets);       // milestones are points; the operator rate turns them into tickets
            if (tickets > 0) {
               Item ticket = TicketGameLogic.item("arcade_ticket");
               for (int left = tickets; left > 0; left -= 64) Block.popResourceFromFace(level, pos, state.getValue(PinballBlock.FACING), new ItemStack(ticket, Math.min(64, left)));
            }
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.0F);
         }
      }
      be.snapshot();
   }

   private void snapshot() {
      this.x = (float)this.sim.x;
      this.y = (float)this.sim.y;
      this.aL = (float)this.sim.aL;
      this.aR = (float)this.sim.aR;
      this.charge = (float)this.sim.charge;
      this.score = this.sim.score;
      this.balls = this.sim.balls;
      this.targets = this.sim.targets;
      this.flash = this.sim.flash;
      this.rollovers = this.sim.rollovers;
      this.mult = this.sim.mult;
      this.kickback = this.sim.kickback;
      this.drops = this.sim.drops;
      this.hold = this.sim.hold;
      this.spin = (float)this.sim.spinAngle;
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   @Override
   protected void saveAdditional(ValueOutput out) {
      super.saveAdditional(out);
      out.putBoolean("playing", this.playing);
      out.putFloat("x", this.x);
      out.putFloat("y", this.y);
      out.putFloat("aL", this.aL);
      out.putFloat("aR", this.aR);
      out.putFloat("charge", this.charge);
      out.putInt("score", this.score);
      out.putInt("balls", this.balls);
      out.putInt("targets", this.targets);
      out.putInt("flash", this.flash);
      out.putInt("high", this.highScore);
      out.putInt("roll", this.rollovers);
      out.putInt("mult", this.mult);
      out.putBoolean("kick", this.kickback);
      out.putBoolean("newHigh", this.newHigh);
      out.putInt("drops", this.drops);
      out.putInt("hold", this.hold);
      out.putFloat("spin", this.spin);
   }

   @Override
   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);
      int oldScore = this.score, oldBalls = this.balls, oldFlash = this.flash;
      this.px = this.x;
      this.py = this.y;
      this.paL = this.aL;
      this.paR = this.aR;
      this.playing = in.getBooleanOr("playing", false);
      this.x = in.getFloatOr("x", 13.2F);
      this.y = in.getFloatOr("y", 1.3F);
      this.aL = in.getFloatOr("aL", (float)PinballSim.REST);
      this.aR = in.getFloatOr("aR", (float)PinballSim.REST);
      this.charge = in.getFloatOr("charge", 0F);
      this.score = in.getIntOr("score", 0);
      this.balls = in.getIntOr("balls", 0);
      this.targets = in.getIntOr("targets", 0);
      this.flash = in.getIntOr("flash", 0);
      this.highScore = in.getIntOr("high", 0);
      this.rollovers = in.getIntOr("roll", 0);
      this.mult = in.getIntOr("mult", 1);
      this.kickback = in.getBooleanOr("kick", false);
      this.newHigh = in.getBooleanOr("newHigh", false);
      this.drops = in.getIntOr("drops", 0);
      this.hold = in.getIntOr("hold", 0);
      this.spin = in.getFloatOr("spin", 0F);
      if (this.level != null && this.level.isClientSide()) {
         long now = this.level.getGameTime();
         for (int i = 0; i < 16; i++) {
            if ((this.flash & (1 << i)) != 0 && (oldFlash & (1 << i)) == 0 || (this.flash & (1 << i)) != 0 && now - this.hitAt[i] > 3) {
               this.hitAt[i] = now;
            }
         }
         if (this.score - oldScore >= 500) {
            this.bonusAt = now;
         }
         if (this.balls < oldBalls) {
            this.drainAt = now;
         }
      }
      if (Math.abs(this.x - this.px) > 6 || Math.abs(this.y - this.py) > 6) {
         this.px = this.x;   // new ball / teleport: don't smear across the table
         this.py = this.y;
      }
   }


   @Override
   public void registerControllers(ControllerRegistrar controllers) {
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
