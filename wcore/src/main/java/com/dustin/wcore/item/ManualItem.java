package com.dustin.wcore.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class ManualItem extends Item implements GeoItem {
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
   private static final Map<String, ManualItem.Manual> MANUALS = new LinkedHashMap<>();
   public static BiConsumer<String, Boolean> USE = (id, back) -> {};
   public static IntSupplier STATE = () -> 0;
   public static Supplier<GeoItemRenderer<?>> RENDERER = () -> null;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public ManualItem(Properties properties) {
      super(properties);
   }

   public static void register(String id, String title, String... pages) {
      MANUALS.put(id, new ManualItem.Manual(title, List.of(pages)));
   }

   public static Map<String, ManualItem.Manual> all() {
      return MANUALS;
   }

   public static ManualItem.Manual get(String id) {
      return MANUALS.get(id);
   }

   public static String idOf(ItemStack stack) {
      return ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getStringOr("manual", "wcore");
   }

   public static ItemStack of(Item item, String id) {
      ItemStack stack = new ItemStack(item);
      CompoundTag tag = new CompoundTag();
      tag.putString("manual", id);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      ManualItem.Manual m = MANUALS.get(id);
      if (m != null) {
         stack.set(DataComponents.ITEM_NAME, Component.literal(m.title()));
      }

      return stack;
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      String id = idOf(player.getItemInHand(hand));
      if (level.isClientSide() && MANUALS.containsKey(id)) {
         USE.accept(id, player.isShiftKeyDown());
      }

      return InteractionResult.SUCCESS;
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("book", 0, test -> {
         test.setAnimation(switch (STATE.getAsInt()) {
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
               this.renderer = ManualItem.RENDERER.get();
            }

            return this.renderer;
         }
      });
   }

   public record Manual(String title, List<String> pages) {
   }
}
