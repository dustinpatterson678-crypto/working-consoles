package com.dustin.wcore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class TwoHanded {
   private static final Map<Item, TwoHanded.Placement> ITEMS = new IdentityHashMap<>();

   private TwoHanded() {
   }

   public static void register(Item item, TwoHanded.Placement placement) {
      ITEMS.put(item, placement);
   }

   public static TwoHanded.Placement get(ItemStack stack) {
      return ITEMS.get(stack.getItem());
   }

   @FunctionalInterface
   public interface Placement {
      void apply(PoseStack var1, ItemStack var2);
   }
}
