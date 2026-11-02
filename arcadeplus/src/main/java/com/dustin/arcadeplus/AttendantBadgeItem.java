package com.dustin.arcadeplus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Use on the floor to bring an Arcade Attendant to work. */
public class AttendantBadgeItem extends Item {
   public AttendantBadgeItem(Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext ctx) {
      Level level = ctx.getLevel();
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      AttendantEntity a = ArcadePlusMod.ATTENDANT.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
      if (a == null) {
         return InteractionResult.FAIL;
      }

      BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
      a.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
      if (ctx.getPlayer() != null) {
         a.setYRot(ctx.getPlayer().getYRot() + 180.0F);
      }
      level.addFreshEntity(a);
      if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) {
         ctx.getItemInHand().shrink(1);
      }
      return InteractionResult.SUCCESS;
   }
}
