package com.dustin.wcore.item;

import com.dustin.wcore.ConsoleLinks;
import com.dustin.wcore.client.ControllerRenderer;
import com.dustin.wcore.controller.ConsoleController;
import com.dustin.wcore.controller.ControllerType;
import com.dustin.wcore.controller.ControllerTypes;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

/** The universal controller: fits every console (ControllerTypes.WIRELESS). */
public class WirelessControllerItem extends Item implements GeoItem, ConsoleController {
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public WirelessControllerItem(Properties properties) {
      super(properties);
   }

   @Override
   public ControllerType controllerType() {
      return ControllerTypes.WIRELESS;
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (level.isClientSide()) {
         String console = ConsoleLinks.active();
         player.sendOverlayMessage(
            console == null
               ? Component.literal("Wireless controller: no console switched on").withStyle(ChatFormatting.GRAY)
               : Component.literal("Wireless controller connected: " + console).withStyle(ChatFormatting.GREEN)
         );
      }

      return InteractionResult.SUCCESS;
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("main", 0, test -> {
         test.setAnimation(IDLE);
         return PlayState.CONTINUE;
      }));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private GeoItemRenderer<?> renderer;

         public GeoItemRenderer<?> getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new ControllerRenderer<WirelessControllerItem>(ControllerTypes.WIRELESS);
            }

            return this.renderer;
         }
      });
   }
}
