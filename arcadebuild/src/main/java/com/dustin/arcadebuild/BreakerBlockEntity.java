package com.dustin.arcadebuild;

import com.dustin.wcore.block.OutletBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Remembers the building it belongs to (the facade block) and switches its power cells and lights. */
public class BreakerBlockEntity extends BlockEntity {
   public long facade = Long.MIN_VALUE;

   public BreakerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public int apply(Level level, boolean on) {
      if (this.facade != Long.MIN_VALUE && level.getBlockEntity(BlockPos.of(this.facade)) instanceof HallCoreBlockEntity hall) {
         return applyHall(level, hall, on);
      }
      if (this.facade == Long.MIN_VALUE || !(level.getBlockEntity(BlockPos.of(this.facade)) instanceof FacadeBlockEntity f)) {
         return 0;
      }
      f.setPowered(on);
      int n = 0;
      for (BlockPos cell : f.powerCells()) {
         level.setBlock(cell, on ? Blocks.REDSTONE_BLOCK.defaultBlockState() : ArcadeBuildMod.SHELL.defaultBlockState(), 3);
         OutletBlock.refresh(level, cell.above());
         n++;
      }
      for (BlockPos light : f.lightCells()) {
         if (level.getBlockState(light).getBlock() == Blocks.LIGHT || level.getBlockState(light).isAir()) {
            level.setBlock(light, on ? Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15) : Blocks.AIR.defaultBlockState(), 3);
         }
      }
      BuildingPlan pl = f.plan();                                      // power lines players laid anywhere in the building
      int sxw = pl.sx() + 2;
      PowerConduitBlock.switchAll(level, f.cell(-sxw, 0, -7), f.cell(sxw, 4, pl.BACK + 4), on);
      // re-seat the roof so the world knows it is solid overhead (keeps rain and snow outside)
      for (BuildingPlan.Seg g : f.plan().segs()) {
         if (!g.kind().equals("roof")) continue;
         for (int lx = g.x0(); lx <= g.x1(); lx++) for (int ly = g.y0(); ly <= g.y1(); ly++) for (int lz = g.z0(); lz <= g.z1(); lz++) {
            BlockPos c = f.cell(lx, ly, lz);
            if (level.getBlockState(c).getBlock() == ArcadeBuildMod.SHELL) {
               level.setBlock(c, Blocks.AIR.defaultBlockState(), 2 | 16);
               level.setBlock(c, ArcadeBuildMod.SHELL.defaultBlockState(), 2 | 16);
            }
         }
      }
      int[] st = f.plan().strip();
      if (st != null) {                                                  // the big strip lights up / dies with a crackle
         BlockPos sp = f.cell(st[0], 1, st[1]);
         BlockState ss = level.getBlockState(sp);
         if (ss.getBlock() instanceof PowerStripBlock) level.setBlock(sp, ss.setValue(PowerStripBlock.LIT, on), 3);
         if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, sp.getX() + 0.5, sp.getY() + 0.6, sp.getZ() + 0.5, on ? 40 : 25, 0.9, 0.25, 0.4, 0.35);
            sl.playSound(null, sp, on ? net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT : net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, on ? 1.4F : 0.8F);
         }
      }
      return n;
   }

   /** The Grand Hall's main breaker: every floor outlet, the room lights and the giant power strip. */
   private int applyHall(Level level, HallCoreBlockEntity hall, boolean on) {
      BlockPos o = hall.origin();
      int n = 0;
      net.minecraft.world.level.block.Block outlet = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse("wcore:outlet"));
      for (int lx = -GrandHall.HX; lx <= GrandHall.HX; lx++) {
         for (int lz = 0; lz < GrandHall.DEPTH; lz++) {
            BlockPos at = o.offset(-lx, 1, lz);
            BlockState s = level.getBlockState(at);
            if (s.getBlock() == outlet) {
               level.setBlock(at.below(), on ? Blocks.REDSTONE_BLOCK.defaultBlockState() : ArcadeBuildMod.HALL_SHELL.defaultBlockState(), 3);
               OutletBlock.refresh(level, at);
               n++;
            } else if (s.getBlock() instanceof PowerStripBlock) {
               level.setBlock(at, s.setValue(PowerStripBlock.LIT, on), 3);
            }
            BlockPos lt = o.offset(-lx, 4, lz);
            BlockState ls = level.getBlockState(lt);
            if ((lx + GrandHall.HX - 1) % 4 == 0 && (lz - 1) % 4 == 0 && (ls.getBlock() == Blocks.LIGHT || ls.isAir())) {
               level.setBlock(lt, on ? Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15) : Blocks.AIR.defaultBlockState(), 3);
            }
         }
      }
      return n;
   }

   @Override
   protected void saveAdditional(ValueOutput out) {
      super.saveAdditional(out);
      out.putLong("facade", this.facade);
   }

   @Override
   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);
      this.facade = in.getLongOr("facade", Long.MIN_VALUE);
   }
}
