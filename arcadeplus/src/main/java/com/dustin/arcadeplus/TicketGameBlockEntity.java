package com.dustin.arcadeplus;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Game state, synced to clients so the renderer can animate balls, moles, hoop, pusher and claw. */
public class TicketGameBlockEntity extends com.dustin.wcore.block.ConsoleBlockEntity implements GeoBlockEntity {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   public boolean playing;
   public int score;
   public String player = "";        // who put the nickel in (for the high-score board)
   public String highScores = "";    // this machine's top 5, see HighScores
   public int shots;          // Skee-Ball balls rolled / basketball shots taken
   public long startTick;
   public long actionTick = -1000;
   public int lastResult;     // Skee-Ball points, basket made (1/0), mole hole hit, claw win (1/0)
   public int lastAim;        // basketball hoop x*100 at the shot, claw x*100
   public int lastAim2;       // claw z*100
   public int stage;          // claw: 0 idle, 1 moving x, 2 moving z, 3 grabbing
   public int moleMask;
   public final long[] moleUntil = new long[5];
   // hands-on games
   public int input;                                   // held keys from TicketInputPacket (server)
   public final long[] shotTick = {-1000, -1000, -1000, -1000};   // basketball: up to 4 balls in the air
   public final int[] shotAim = new int[4], shotPow = new int[4];
   public final long[] shotAt = new long[4];           // round time when thrown (for the moving hoop)
   public final boolean[] shotCounted = new boolean[4];
   public int shotNext;
   public final int[] stackRows = new int[12];         // stacker: start | width << 4 per locked row
   public int jackpot = 200;                           // cyclone jackpot pool
   public int jackpotSeed = 200;                       // what the jackpot restarts at after a win (halves each win)
   public long seedDay = -1;                           // in-game day of the first shrink; full again 3 days later
   public int payLeft;                                 // tickets still coming out of the mouth
   public float clawX = -5F, clawZ = -5F;              // claw position in model pixels
   public float pClawX = -5F, pClawZ = -5F;            // client: previous position for smooth movement
   /** Air hockey: puck x, z, vx, vz, your mallet x, z, machine mallet x, z (model pixels). */
   public final float[] hk = new float[8];
   public final float[] pHk = new float[8];
   /** Pool: 16 balls x (u, v, vu, vv). */
   public final float[] pool = new float[64];
   public final float[] pPool = new float[64];            // client: last synced state, to smooth between updates
   public long hkSynced;                               // client: game time of the last update

   public TicketGameBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public TicketGame game() {
      return this.getBlockState().getBlock() instanceof TicketGameBlock b ? b.game : TicketGame.SKEEBALL;
   }

   @Override
   public String consoleName() {
      return this.game().title;
   }

   @Override
   public void sync() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide()) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, TicketGameBlockEntity be) {
      com.dustin.wcore.block.ConsoleBlockEntity.serverTick(level, pos, state, be);   // outlet power, like every console
      if (level.getGameTime() % 10 == 0 && state.hasProperty(TicketGameBlock.LIT)) {
         boolean lit = !be.game().unpowered() && be.isPowered();
         if (state.getValue(TicketGameBlock.LIT) != lit) level.setBlock(pos, state.setValue(TicketGameBlock.LIT, lit), 3);
      }
      if (!be.game().unpowered() && !be.isPowered() && (be.playing || be.stage != 0)) {
         be.playing = false;                       // pulled the plug mid-game
         be.stage = 0;
         be.moleMask = 0;
         be.sync();
         return;
      }
      if (!be.partsBuilt) be.buildParts();
      TicketGameLogic.tick(level, pos, be);
   }

   public boolean partsBuilt;

   /** Long machines get solid invisible cells behind them, so you can't walk through the back half. */
   void buildParts() {
      this.partsBuilt = true;
      if (this.level == null || !(this.getBlockState().getBlock() instanceof TicketGameBlock)) return;
      net.minecraft.core.Direction front = this.getBlockState().getValue(TicketGameBlock.FACING), side = front.getClockWise();
      for (int[] c : this.game().cells()) {
         BlockPos p = this.worldPosition.relative(side, c[0]).relative(front.getOpposite(), c[1]);
         net.minecraft.core.Direction toward = c[0] != 0 ? side.getOpposite() : front;     // each part points one step closer to the main block
         if (this.level.getBlockState(p).canBeReplaced()) {
            this.level.setBlock(p, ArcadePlusMod.MACHINE_PART.defaultBlockState().setValue(MachinePartBlock.FACING, toward), 3);
         }
      }
   }

   @Override
   protected void saveAdditional(ValueOutput out) {
      super.saveAdditional(out);
      out.putBoolean("playing", this.playing);
      out.putInt("score", this.score);
      out.putString("player", this.player);
      out.putString("hiscores", this.highScores);
      out.putInt("shots", this.shots);
      out.putLong("start", this.startTick);
      out.putLong("action", this.actionTick);
      out.putInt("result", this.lastResult);
      out.putInt("aim", this.lastAim);
      out.putInt("aim2", this.lastAim2);
      out.putInt("stage", this.stage);
      out.putInt("moles", this.moleMask);
      for (int i = 0; i < 4; i++) {
         out.putLong("st" + i, this.shotTick[i]);
         out.putInt("sa" + i, this.shotAim[i]);
         out.putInt("sp" + i, this.shotPow[i]);
         out.putLong("sat" + i, this.shotAt[i]);
         out.putBoolean("sc" + i, this.shotCounted[i]);
      }
      out.putFloat("cx", this.clawX);
      for (int i = 0; i < 12; i++) {
         out.putInt("row" + i, this.stackRows[i]);
      }
      out.putInt("jackpot", this.jackpot);
      out.putInt("jseed", this.jackpotSeed);
      out.putLong("jday", this.seedDay);
      out.putInt("payLeft", this.payLeft);
      out.putFloat("cz", this.clawZ);
      for (int i = 0; i < 8; i++) {
         out.putFloat("hk" + i, this.hk[i]);
      }
      if (this.game() == TicketGame.POOL) for (int i = 0; i < 64; i++) out.putFloat("pl" + i, this.pool[i]);
   }

   @Override
   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);
      this.playing = in.getBooleanOr("playing", false);
      this.score = in.getIntOr("score", 0);
      this.player = in.getStringOr("player", "");
      this.highScores = in.getStringOr("hiscores", "");
      this.shots = in.getIntOr("shots", 0);
      this.startTick = in.getLongOr("start", 0L);
      this.actionTick = in.getLongOr("action", -1000L);
      this.lastResult = in.getIntOr("result", 0);
      this.lastAim = in.getIntOr("aim", 0);
      this.lastAim2 = in.getIntOr("aim2", 0);
      this.stage = in.getIntOr("stage", 0);
      this.moleMask = in.getIntOr("moles", 0);
      for (int i = 0; i < 4; i++) {
         this.shotTick[i] = in.getLongOr("st" + i, -1000L);
         this.shotAim[i] = in.getIntOr("sa" + i, 0);
         this.shotPow[i] = in.getIntOr("sp" + i, 0);
         this.shotAt[i] = in.getLongOr("sat" + i, 0L);
         this.shotCounted[i] = in.getBooleanOr("sc" + i, true);
      }
      this.pClawX = this.clawX;
      this.pClawZ = this.clawZ;
      this.clawX = in.getFloatOr("cx", -5F);
      for (int i = 0; i < 12; i++) {
         this.stackRows[i] = in.getIntOr("row" + i, 0);
      }
      this.jackpot = Math.min(400, in.getIntOr("jackpot", 200));
      this.jackpotSeed = Math.min(200, in.getIntOr("jseed", 200));
      this.seedDay = in.getLongOr("jday", -1L);
      this.payLeft = in.getIntOr("payLeft", 0);
      this.clawZ = in.getFloatOr("cz", -5F);
      System.arraycopy(this.hk, 0, this.pHk, 0, 8);
      for (int i = 0; i < 8; i++) {
         this.hk[i] = in.getFloatOr("hk" + i, 0F);
      }
      System.arraycopy(this.pool, 0, this.pPool, 0, 64);
      for (int i = 0; i < 64; i++) this.pool[i] = in.getFloatOr("pl" + i, this.pool[i]);
      if (this.level != null) this.hkSynced = this.level.getGameTime();
   }


   @Override
   public void registerControllers(ControllerRegistrar controllers) {
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
