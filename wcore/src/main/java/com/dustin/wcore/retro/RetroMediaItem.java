package com.dustin.wcore.retro;

import com.dustin.wcore.client.RetroClient;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class RetroMediaItem extends Item implements GeoItem {
   public final String systemId;
   public final Identifier model;
   public final int[] coverPanel;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public RetroMediaItem(Properties properties, String systemId, Identifier model, int[] coverPanel) {
      super(properties);
      this.systemId = systemId;
      this.model = model;
      this.coverPanel = coverPanel;
   }

   public RetroSystem system() {
      return RetroSystem.get(this.systemId);
   }

   public static ItemStack of(RetroSystem system, String file) {
      Item item = system.mediaItem.get();
      ItemStack stack = new ItemStack(item);
      if (file != null && !file.isEmpty()) {
         CompoundTag tag = new CompoundTag();
         tag.putString("game", file);
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         stack.set(DataComponents.ITEM_NAME, Component.literal(RetroSystem.title(file)));
      }

      return stack;
   }

   public static String gameOf(ItemStack stack) {
      return ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getStringOr("game", "");
   }

   public Component getName(ItemStack stack) {
      return (Component)(gameOf(stack).isEmpty() && this.system() != null
         ? Component.literal("Blank " + this.system().name + " " + this.system().mediaWord)
         : super.getName(stack));
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (gameOf(stack).isEmpty() && !level.isClientSide() && player instanceof ServerPlayer sp && this.system() != null) {
         RetroCommands.listForWriting(sp, this.system());
      }

      return InteractionResult.SUCCESS;
   }

   public void registerControllers(ControllerRegistrar controllers) {
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private GeoItemRenderer<?> renderer;

         public GeoItemRenderer<?> getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = RetroClient.mediaRenderer(RetroMediaItem.this);
            }

            return this.renderer;
         }
      });
   }
}
