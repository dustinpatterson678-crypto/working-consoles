package com.dustin.wcore.item;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.block.ConsoleBlockEntity;
import com.dustin.wcore.client.ControllerRenderer;
import com.dustin.wcore.controller.ConsoleController;
import com.dustin.wcore.controller.ControllerType;
import com.dustin.wcore.controller.PairedConsole;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A console's own controller. Right-click a console to play it (the console block handles that click first);
 * right-click any other block to put the controller down.
 */
public class ConsoleControllerItem extends BlockItem implements GeoItem, ConsoleController {
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final ControllerType type;

   public ConsoleControllerItem(Block block, ControllerType type, Properties properties) {
      super(block, properties);
      this.type = type;
   }

   @Override
   public ControllerType controllerType() {
      return this.type;
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (level.isClientSide()) {
         ItemStack stack = player.getItemInHand(hand);
         PairedConsole paired = stack.get(WorkingConsolesCore.PAIRED_CONSOLE);
         if (paired == null) {
            player.sendOverlayMessage(Component.literal("Not paired: right-click a console with it").withStyle(ChatFormatting.GRAY));
         } else {
            player.sendOverlayMessage(Component.literal("Paired: " + consoleName(level, paired) + " P" + (paired.port() + 1)).withStyle(ChatFormatting.GREEN));
         }
      }

      return InteractionResult.SUCCESS;
   }

   private static String consoleName(Level level, PairedConsole paired) {
      BlockPos pos = paired.pos();
      if (level.dimension().equals(paired.dim()) && level.getBlockEntity(pos) instanceof ConsoleBlockEntity be) {
         return be.consoleName();
      }

      return "console at " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
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
      final ControllerType t = this.type;
      consumer.accept(new GeoRenderProvider() {
         private GeoItemRenderer<?> renderer;

         public GeoItemRenderer<?> getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new ControllerRenderer<ConsoleControllerItem>(t);
            }

            return this.renderer;
         }
      });
   }
}
