package com.dustin.ps2.item;

import com.dustin.ps2.Ps2Files;
import com.dustin.ps2.Ps2Mod;
import com.dustin.ps2.client.Ps2Client;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class DiscCaseItem extends Item implements GeoItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

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
               this.renderer = Ps2Client.caseRenderer();
            }

            return this.renderer;
         }
      });
   }

   public DiscCaseItem(Properties properties) {
      super(properties);
   }

   public static ItemStack caseFor(String file, String title) {
      ItemStack stack = new ItemStack(Ps2Mod.DISC_CASE);
      CompoundTag tag = new CompoundTag();
      tag.putString("disc", file);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      stack.set(DataComponents.ITEM_NAME, Component.literal(title));
      return stack;
   }

   public static ItemStack caseFor(Ps2Files.Disc disc) {
      return caseFor(disc.file().getFileName().toString(), disc.title());
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (discOf(player.getItemInHand(hand)).isEmpty() && !level.isClientSide() && player instanceof ServerPlayer sp) {
         Ps2Mod.listForWriting(sp);
      }

      return InteractionResult.SUCCESS;
   }

   public static String discOf(ItemStack stack) {
      return ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getStringOr("disc", "");
   }
}
