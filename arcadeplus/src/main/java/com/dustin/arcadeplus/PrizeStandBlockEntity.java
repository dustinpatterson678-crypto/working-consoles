package com.dustin.arcadeplus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The prize booth's core: fills in the booth's other cells once, and lets the renderer draw the booth and prizes. */
public class PrizeStandBlockEntity extends BlockEntity {
   private boolean built;

   public PrizeStandBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   void serverTick() {
      if (this.built || this.level == null) return;
      Direction f = this.getBlockState().getValue(PrizeStandBlock.FACING);
      BlockState part = Prizes.STAND_PART.defaultBlockState().setValue(PrizeStandBlock.FACING, f);
      for (BlockPos p : PrizeStandBlock.partCells(this.worldPosition, f)) {
         if (this.level.getBlockState(p).canBeReplaced()) this.level.setBlock(p, part, 3);
      }
      this.built = true;
      this.setChanged();
   }

   @Override
   protected void saveAdditional(ValueOutput out) {
      super.saveAdditional(out);
      out.putBoolean("built", this.built);
   }

   @Override
   protected void loadAdditional(ValueInput in) {
      super.loadAdditional(in);
      this.built = in.getBooleanOr("built", false);
   }
}
