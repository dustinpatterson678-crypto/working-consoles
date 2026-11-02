package com.dustin.arcadebuild;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The building's core: it draws the whole building and knows where its power cells and lights are. */
public class FacadeBlockEntity extends BlockEntity {
   public static final int FX = 0, FY = 6, FZ = -1;   // where this block sits in the plan
   public float doorOpen;                             // client-side door animation
   public final float[] doors = new float[3];         // client-side: staff door, office door, power room door
   private boolean powered = true;
   private int cleanup;                               // 1 = the old round columns were taken out of this building
   private boolean foundationDone;                    // the legacy building was filled back in under its floor
   private int foundationIndex;
   private boolean legacyUpgraded;                    // old shell windows and floor were converted to real blocks
   /** Where Dustin's Arcade used to have round columns (removed in 10.3). */
   private static final int[][] OLD_COLUMNS = {{-6, 6}, {5, 8}, {-6, 16}, {6, 16}};

   void serverTick() {
      if (this.level == null) return;
      BuildingPlan plan = this.plan();
      if (plan.id == 1) {
         this.upgradeLegacy(plan);
         this.foundationTick(plan);
         if (this.cleanup < 1) {
            for (int[] c : OLD_COLUMNS) {
               for (int y = 1; y < BuildingPlan.CEIL; y++) {
                  BlockPos p = this.cell(c[0], y, c[1]);
                  if (this.level.getBlockState(p).getBlock() == ArcadeBuildMod.SHELL) this.level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
               }
            }
            this.cleanup = 1;
            this.setChanged();
         }
      }
      this.doorTick(plan);
   }

   /**
    * Existing legacy worlds were built with invisible shell blocks in the windows and floor. New builds use
    * real glass and a stone slab; convert the old cells once so those worlds get the fix too.
    */
   private void upgradeLegacy(BuildingPlan plan) {
      if (this.legacyUpgraded) return;
      this.legacyUpgraded = true;
      net.minecraft.world.level.block.state.BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
      for (int[] w : plan.windows()) {
         for (int x = w[0]; x <= w[1]; x++) {
            for (int y = w[2]; y <= w[3]; y++) {
               BlockPos outer = this.cell(x, y, -1), inner = this.cell(x, y, 0);
               if (this.level.getBlockState(outer).getBlock() == ArcadeBuildMod.SHELL) this.level.setBlock(outer, air, 2 | 16);
               if (this.level.getBlockState(inner).getBlock() == ArcadeBuildMod.SHELL) {
                  this.level.setBlock(inner, ArcadeBuildMod.STOREFRONT_GLASS.defaultBlockState(), 2 | 16);
               }
            }
         }
      }
      int sx = plan.sx();
      for (int z = -5; z <= plan.BACK + 2; z++) {
         for (int x = -sx - 1; x <= sx + 1; x++) {
            BlockPos p = this.cell(x, 0, z);
            if (this.level.getBlockState(p).getBlock() == ArcadeBuildMod.SHELL) {
               this.level.setBlock(p, net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState(), 2 | 16);
            }
         }
      }
      this.setChanged();
   }


   /** Fill the old hollow foundation back in, a little at a time. */
   private void foundationTick(BuildingPlan plan) {
      if (this.foundationDone) return;
      int sx = plan.sx(), x0 = -sx - 1, x1 = sx + 1, z0 = -5, z1 = plan.BACK + 2;
      int nx = x1 - x0 + 1, nz = z1 - z0 + 1, total = nx * nz * 6;
      BlockPos floor = this.cell(0, 0, plan.BACK / 2);
      for (net.minecraft.world.entity.player.Player p : this.level.players()) {
         if (p.getY() < floor.getY() + 0.5 && p.getY() > floor.getY() - 8.0
            && p.distanceToSqr(floor.getX() + 0.5, p.getY(), floor.getZ() + 0.5) < 40.0 * 40.0) {
            return;                                    // don't bury somebody who is inspecting the underside
         }
      }
      int end = Math.min(total, this.foundationIndex + 256);
      for (; this.foundationIndex < end; this.foundationIndex++) {
         int i = this.foundationIndex;
         int z = z0 + i % nz; i /= nz;
         int x = x0 + i % nx; i /= nx;
         int y = -6 + i;
         BlockPos p = this.cell(x, y, z);
         if (this.level.getBlockState(p).isAir()) {
            this.level.setBlock(p, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 2 | 16);
         }
      }
      if (this.foundationIndex >= total) {
         this.foundationDone = true;
         this.setChanged();
      }
   }

   /** The doors are real collision cells, removed when a player is close enough to walk through. */
   private void doorTick(BuildingPlan plan) {
      if (plan.id != 1 && plan.id != 3) return;
      int dz = plan.id == 3 ? -1 : 0;
      BlockPos center = this.cell(0, 2, dz);
      boolean near = false;
      for (net.minecraft.world.entity.player.Player p : this.level.players()) {
         if (p.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5) < 4.0 * 4.0) {
            near = true;
            break;
         }
      }
      for (int x = -1; x <= 1; x++) {
         for (int y = 1; y <= 3; y++) {
            BlockPos p = this.cell(x, y, dz);
            BlockState st = this.level.getBlockState(p);
            if (near) {
               if (st.getBlock() == ArcadeBuildMod.SHELL) this.level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2 | 16);
            } else if (st.isAir()) {
               this.level.setBlock(p, ArcadeBuildMod.SHELL.defaultBlockState(), 2 | 16);
            }
         }
      }
   }

   public FacadeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public BuildingPlan plan() {
      return BuildingPlan.of(this.getBlockState().getBlock() instanceof FacadeBlock f ? f.plan : 0);
   }

   public boolean isPowered() {
      return this.powered;
   }

   public void setPowered(boolean on) {
      this.powered = on;
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   /** Plan cell -> world position for this building. */
   public BlockPos cell(int lx, int ly, int lz) {
      Direction fwd = this.getBlockState().getValue(FacadeBlock.FACING), right = fwd.getClockWise();
      return this.worldPosition.relative(right, lx - FX).relative(fwd, lz - FZ).above(ly - FY);
   }

   public List<BlockPos> powerCells() {
      List<BlockPos> out = new ArrayList<>();
      for (int[] o : this.plan().outlets()) out.add(this.cell(o[0], 0, o[1]));
      return out;
   }

   public List<BlockPos> lightCells() {
      List<BlockPos> out = new ArrayList<>();
      for (int[] l : this.plan().lights()) out.add(this.cell(l[0], BuildingPlan.CEIL - 1, l[1]));
      return out;
   }

   @Override
   protected void saveAdditional(ValueOutput out) {
      super.saveAdditional(out);
      out.putBoolean("powered", this.powered);
      out.putInt("cleanup", this.cleanup);
      out.putBoolean("foundationDone", this.foundationDone);
      out.putBoolean("legacyUpgraded", this.legacyUpgraded);
   }

   @Override
   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);
      this.powered = in.getBooleanOr("powered", true);
      this.cleanup = in.getIntOr("cleanup", 0);
      this.foundationDone = in.getBooleanOr("foundationDone", false);
      this.legacyUpgraded = in.getBooleanOr("legacyUpgraded", false);
   }

   @Override
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveCustomOnly(registries);
   }
}
