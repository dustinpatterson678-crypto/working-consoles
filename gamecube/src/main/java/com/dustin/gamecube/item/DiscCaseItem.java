package com.dustin.gamecube.item;

import com.dustin.gamecube.GameCubeFiles;
import com.dustin.gamecube.GameCubeMod;
import com.dustin.wcore.client.CoverArt;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.CustomData;

public class DiscCaseItem extends Item implements GeoItem {
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public DiscCaseItem(Properties properties) {
      super(properties);
   }

   public static ItemStack caseFor(GameCubeFiles.Disc d) {
      return caseFor(d.file().getFileName().toString(), d.title());
   }

   public static ItemStack caseFor(String file, String title) {
      ItemStack stack = new ItemStack(GameCubeMod.DISC_CASE);
      CompoundTag tag = new CompoundTag();
      tag.putString("disc", file);
      tag.putString("title", title);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return stack;
   }

   public static String discOf(ItemStack stack) {
      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      if (data == null) {
         return "";
      }
      return data.copyTag().getStringOr("disc", "");
   }

   public static String titleOf(ItemStack stack) {
      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      if (data == null) {
         return "";
      }
      return data.copyTag().getStringOr("title", "");
   }

   @Override
   public Component getName(ItemStack stack) {
      String title = titleOf(stack);
      if (!title.isEmpty()) {
         return Component.translatable("item.gamecube.disc_case.named", title);
      }
      return super.getName(stack);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("main", 0, test -> {
         test.setAnimation(IDLE);
         return PlayState.CONTINUE;
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}