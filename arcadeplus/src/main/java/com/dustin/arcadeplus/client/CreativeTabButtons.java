package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.ArcadePlusMod;
import com.dustin.arcadeplus.UnifiedTab;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

/** Category buttons down the left of the creative inventory, shown while the Dustin's Arcade & Consoles tab is open. */
public final class CreativeTabButtons {
   private CreativeTabButtons() {
   }

   private static Field selected, params;
   private static Method select;

   public static void init() {
      try {
         selected = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
         selected.setAccessible(true);
         select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
         select.setAccessible(true);
         params = CreativeModeTabs.class.getDeclaredField("CACHED_PARAMETERS");
         params.setAccessible(true);
      } catch (ReflectiveOperationException | RuntimeException e) {
         ArcadePlusMod.LOGGER.warn("Creative tab categories unavailable: {}", e.toString());
         return;
      }
      ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
         if (!(screen instanceof CreativeModeInventoryScreen cs)) return;
         List<Button> buttons = new ArrayList<>();
         int x = Math.max(2, (w - 195) / 2 - 92), y = (h - 136) / 2;
         for (int i = 0; i < UnifiedTab.NAMES.length; i++) {
            final int cat = i;
            Button b = Button.builder(label(i), btn -> pick(cs, cat, buttons)).bounds(x, y + i * 21, 88, 20).build();
            buttons.add(b);
            Screens.getWidgets(screen).add(b);
         }
         show(buttons, open());
         ScreenEvents.afterTick(screen).register(s -> show(buttons, open()));
      });
   }

   private static Component label(int i) {
      return i == UnifiedTab.category ? Component.literal("> " + UnifiedTab.NAMES[i]).withStyle(ChatFormatting.YELLOW) : Component.literal(UnifiedTab.NAMES[i]);
   }

   private static boolean open() {
      try {
         return UnifiedTab.TAB != null && selected.get(null) == UnifiedTab.TAB;
      } catch (ReflectiveOperationException e) {
         return false;
      }
   }

   private static void show(List<Button> buttons, boolean on) {
      for (Button b : buttons) b.visible = on;
   }

   private static void pick(CreativeModeInventoryScreen screen, int cat, List<Button> buttons) {
      UnifiedTab.category = cat;
      for (int i = 0; i < buttons.size(); i++) buttons.get(i).setMessage(label(i));
      try {
         Object p = params.get(null);
         if (p instanceof CreativeModeTab.ItemDisplayParameters ip) UnifiedTab.TAB.buildContents(ip);
         select.invoke(screen, UnifiedTab.TAB);
      } catch (ReflectiveOperationException | RuntimeException e) {
         ArcadePlusMod.LOGGER.warn("Couldn't switch creative category: {}", e.toString());
      }
   }
}
