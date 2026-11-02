package com.dustin.arcadeplus;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The arcade attendant: wanders the arcade floor, puts your paper tickets on your ticket card when you talk to
 * them, and cheers when a machine nearby pays a jackpot or a big win. Can't be hurt; sneak + right-click with an
 * empty hand packs them back up into their badge.
 */
public class AttendantEntity extends PathfinderMob implements GeoEntity {
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
   private static final RawAnimation CHEER = RawAnimation.begin().thenPlay("cheer");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public AttendantEntity(EntityType<? extends PathfinderMob> type, Level level) {
      super(type, level);
      this.setPermanentlyInvulnerable(true);
      this.setPersistenceRequired();
      this.setCustomName(Component.literal("Arcade Attendant").withStyle(ChatFormatting.GOLD));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
   }

   @Override
   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   @Override
   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (this.level().isClientSide()) {
         return InteractionResult.SUCCESS;
      }

      ItemStack held = player.getItemInHand(hand);
      if (player.isShiftKeyDown() && held.isEmpty()) {
         if (!player.getInventory().add(new ItemStack(ArcadePlusMod.ATTENDANT_BADGE))) {
            player.spawnAtLocation((ServerLevel)this.level(), new ItemStack(ArcadePlusMod.ATTENDANT_BADGE));
         }
         this.discard();
         return InteractionResult.SUCCESS;
      }

      int fed = TicketBank.deposit(player);
      String jackpot = this.level() instanceof ServerLevel sl ? "  Mega Jackpot: " + MegaJackpot.pot(sl) + " tickets." : "";
      say(player, (fed > 0 ? "Thanks! " + fed + " tickets on your card. " : "Bring me your paper tickets! ")
         + "Card: " + TicketBank.balance(player) + "." + jackpot);
      this.getLookControl().setLookAt(player);
      this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.2F);
      return InteractionResult.SUCCESS;
   }

   private void say(Player player, String text) {
      player.sendSystemMessage(Component.literal("[Attendant] ").withStyle(ChatFormatting.GOLD).append(Component.literal(text).withStyle(ChatFormatting.YELLOW)));
   }

   /** Server: every attendant within 24 blocks of pos cheers and calls out `text` to players near them. */
   public static void cheerNear(ServerLevel level, BlockPos pos, String text) {
      for (AttendantEntity a : level.getEntitiesOfClass(AttendantEntity.class, new AABB(pos).inflate(24.0))) {
         a.triggerAnim("main", "cheer");
         a.getJumpControl().jump();
         a.playSound(SoundEvents.VILLAGER_CELEBRATE, 1.2F, 1.1F);
         for (Player p : level.players()) {
            if (p.distanceToSqr(a) < 16 * 16) {
               a.say(p, text);
            }
         }
      }
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<AttendantEntity>("main", 5, test -> {
         test.setAnimation(test.isMoving() ? WALK : IDLE);
         return PlayState.CONTINUE;
      }).triggerableAnim("cheer", CHEER));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
