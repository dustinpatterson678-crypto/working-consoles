package com.dustin.arcadebuild;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** An invisible seat: right-click a bench, sofa or office chair and you sit on it. Vanishes when you stand up. */
public class SeatEntity extends Entity {
   public SeatEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && (this.getPassengers().isEmpty() || !(this.level().getBlockState(net.minecraft.core.BlockPos.containing(this.getX(), this.getY() + 0.5, this.getZ())).getBlock() instanceof DecorBlock))) {
         this.ejectPassengers();
         this.discard();
      }
   }

   @Override
   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      return false;
   }

   @Override
   protected void readAdditionalSaveData(ValueInput in) {
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput out) {
   }
}
