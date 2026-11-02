package com.dustin.gbsp.item;

import com.dustin.gbsp.GameBoySP;
import com.dustin.gbsp.client.GuideBookClient;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class GuideBookItem extends Item implements GeoItem {
   public static final int CLOSED = 0;
   public static final int OPENING = 1;
   public static final int OPEN = 2;
   public static final int FLIP = 3;
   public static final int FLIP_BACK = 4;
   public static final int CLOSING = 5;
   private static final RawAnimation A_CLOSED = RawAnimation.begin().thenLoop("closed");
   private static final RawAnimation A_OPENING = RawAnimation.begin().thenPlay("opening").thenLoop("open");
   private static final RawAnimation A_OPEN = RawAnimation.begin().thenLoop("open");
   private static final RawAnimation A_FLIP = RawAnimation.begin().thenPlayAndHold("flip");
   private static final RawAnimation A_FLIP_BACK = RawAnimation.begin().thenPlayAndHold("flip_back");
   private static final RawAnimation A_CLOSING = RawAnimation.begin().thenPlay("closing").thenLoop("closed");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public GuideBookItem(Properties properties) {
      super(properties);
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (level.isClientSide()) {
         GameBoySP.BOOK_USE.accept(player.isShiftKeyDown());
      }

      return InteractionResult.SUCCESS;
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("book", 0, test -> {
         test.setAnimation(switch (GameBoySP.BOOK_STATE.getAsInt()) {
            case 1 -> A_OPENING;
            case 2 -> A_OPEN;
            case 3 -> A_FLIP;
            case 4 -> A_FLIP_BACK;
            case 5 -> A_CLOSING;
            default -> A_CLOSED;
         });
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
               this.renderer = GuideBookClient.createRenderer();
            }

            return this.renderer;
         }
      });
   }
}
