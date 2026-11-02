package com.dustin.gbsp.item;

import com.dustin.gbsp.GameBoySP;
import com.dustin.gbsp.block.HandheldBlock;
import com.dustin.gbsp.client.GameBoySPRenderer;
import com.dustin.gbsp.emu.SystemFiles;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class GameBoySPItem extends Item implements GeoItem {
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private static final RawAnimation ON = RawAnimation.begin().thenPlay("power_on").thenLoop("on");
   private static final RawAnimation NO_FILES = RawAnimation.begin().thenPlay("no_files");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public GameBoySPItem(Properties properties) {
      super(properties);
      GeoItem.registerSyncedAnimatable(this);
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      SystemFiles.Report report = SystemFiles.scan();
      if (level.isClientSide()) {
         GameBoySP.CLIENT_USE.accept(report, player.isShiftKeyDown());
         return InteractionResult.SUCCESS;
      }

      if (!report.ready() && level instanceof ServerLevel serverLevel) {
         long id = GeoItem.getOrAssignId(stack, serverLevel);

         for (Component line : report.describe()) {
            player.sendSystemMessage(line);
         }

         this.triggerAnim(player, id, "main", "no_files");
         player.sendSystemMessage(Component.literal("It's just a model until you add them. Folder: " + SystemFiles.root()).withStyle(ChatFormatting.GRAY));
      }

      return InteractionResult.SUCCESS;
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player != null && player.isShiftKeyDown()) {
         Level level = context.getLevel();
         BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
         if (!level.getBlockState(pos).canBeReplaced()) {
            return InteractionResult.FAIL;
         }

         if (level.isClientSide()) {
            GameBoySP.CLIENT_PLACED.accept(pos);
         } else {
            level.setBlock(pos, (BlockState)GameBoySP.HANDHELD_BLOCK.defaultBlockState().setValue(HandheldBlock.FACING, player.getDirection().getOpposite()), 3);
            level.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
            if (!player.getAbilities().instabuild) {
               context.getItemInHand().shrink(1);
            }
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("main", 4, test -> {
         test.setAnimation(GameBoySP.HANDHELD_ON.getAsBoolean() ? ON : IDLE);
         return PlayState.CONTINUE;
      }).triggerableAnim("no_files", NO_FILES));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private GeoItemRenderer<?> renderer;

         public GeoItemRenderer<?> getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new GameBoySPRenderer();
            }

            return this.renderer;
         }
      });
   }
}
