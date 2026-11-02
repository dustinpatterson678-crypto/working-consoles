package com.dustin.gbsp.item;

import com.dustin.gbsp.GameBoySP;
import com.dustin.gbsp.client.GameBoySPClient;
import com.dustin.gbsp.emu.SystemFiles;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class GameCaseItem extends Item implements GeoItem {
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
               this.renderer = GameBoySPClient.caseRenderer();
            }

            return this.renderer;
         }
      });
   }

   public GameCaseItem(Properties properties) {
      super(properties);
   }

   public static ItemStack caseFor(SystemFiles.Rom rom) {
      ItemStack stack = new ItemStack(GameBoySP.GAME_CASE);
      CompoundTag tag = new CompoundTag();
      tag.putString("rom", rom.file().getFileName().toString());
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      stack.set(DataComponents.ITEM_NAME, Component.literal(rom.title() + " (" + rom.type() + ")"));
      return stack;
   }

   public static String romOf(ItemStack stack) {
      return ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().getStringOr("rom", "");
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (level.isClientSide()) {
         String rom = romOf(player.getItemInHand(hand));
         if (!rom.isEmpty()) {
            GameBoySP.CLIENT_CASE_USE.accept(rom);
         }
      }

      return InteractionResult.SUCCESS;
   }
}
