package com.dustin.wcore.item;

import com.dustin.wcore.block.DecorBlock;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Furniture block item: same GeckoLib item renderer as GeoBlockItem, plus the
 * how-to-use tooltip so the control scheme is discoverable in-game.
 */
public class DecorBlockItem extends GeoBlockItem {
   public DecorBlockItem(DecorBlock block, Properties properties) {
      super(block, block.model, properties);
   }

   public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      if (this.getBlock() instanceof DecorBlock decor) {
         if (!decor.slots.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.wcore.decor.insert"));
            tooltip.accept(Component.translatable("tooltip.wcore.decor.take"));
         }

         if (decor.animated) {
            tooltip.accept(Component.translatable("tooltip.wcore.decor.toggle"));
         }
      }

      super.appendHoverText(stack, context, display, tooltip, flag);
   }
}
