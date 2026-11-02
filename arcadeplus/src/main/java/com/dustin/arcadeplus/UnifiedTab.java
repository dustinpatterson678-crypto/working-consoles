package com.dustin.arcadeplus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One creative tab for the whole pack, with category buttons down its side (Arcade Games, Prizes, Consoles & TVs,
 * Furniture, Arcade Decor, Building). The pack's other tabs are emptied (so the game hides them) but their items
 * still show up in search. Old items that were replaced by newer ones are left out.
 */
public final class UnifiedTab {
   private UnifiedTab() {
   }

   public static final ResourceKey<CreativeModeTab> KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ArcadePlusMod.id("everything"));
   public static final String[] NAMES = {"Everything", "Arcade Games", "Prizes", "Consoles & TVs", "Furniture", "Arcade Decor", "Building", "Controllers"};
   public static int category;
   private static final Set<String> PACK = Set.of("wcore", "gbsp", "ps2", "sega", "arcade", "nintendo", "retro", "stream", "disc", "classic", "arcadeplus", "arcadebuild", "gamecube");
   private static final Set<String> HIDDEN = Set.of("wcore:arcade_token", "wcore:prize_counter", "arcadeplus:prize_booth", "arcadebuild:arcade_builder");
   private static final Set<String> GAME_ROOM = Set.of("wcore:gaming_chair", "wcore:couch", "wcore:bean_bag", "wcore:coffee_table");
   private static final Map<String, List<ItemStack>> SOURCE = new LinkedHashMap<>();
   public static CreativeModeTab TAB;

   public static boolean hidden(ItemStack s) {
      Identifier iid = BuiltInRegistries.ITEM.getKey(s.getItem());
      return iid != null && HIDDEN.contains(iid.toString());
   }

   static boolean packTab(Identifier id) {
      return id != null && PACK.contains(id.getNamespace()) && !id.equals(KEY.identifier());
   }

   /** Called while any tab builds: copy a pack tab's items, then empty it so it disappears. */
   static void capture(CreativeModeTab tab, List<ItemStack> display) {
      Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
      if (!packTab(id)) return;
      List<ItemStack> keep = new ArrayList<>();
      for (ItemStack s : display) {
         if (!hidden(s)) keep.add(s.copy());
      }
      SOURCE.put(id.toString(), keep);
      display.clear();
   }

   private static boolean prize(Item i) {
      return i == ArcadePlusMod.NICKEL || i == Prizes.MYSTERY_BOX || i == Prizes.STAND_ITEM || Prizes.DECOR_ITEMS.contains(i);
   }

   private static int categoryOf(String tab, ItemStack s) {
      String ns = tab.substring(0, tab.indexOf(':'));
      if (s.getItem() instanceof com.dustin.wcore.controller.ConsoleController) return 7;
      Identifier iid = BuiltInRegistries.ITEM.getKey(s.getItem());
      if (iid != null && GAME_ROOM.contains(iid.toString())) return 4;
      if (ns.equals("arcadebuild")) return 6;
      if (tab.equals("wcore:furniture")) return 4;
      if (tab.equals("wcore:arcade")) return 5;
      if (ns.equals("arcade")) return 1;
      if (ns.equals("arcadeplus")) return prize(s.getItem()) ? 2 : 1;
      return 3;
   }

   public static void register() {
      TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KEY, FabricCreativeModeTab.builder()
         .title(Component.literal("Dustin's Arcade & Consoles"))
         .icon(() -> new ItemStack(Prizes.STAND_ITEM))
         .displayItems((params, out) -> {
            for (Identifier id : BuiltInRegistries.CREATIVE_MODE_TAB.keySet()) {
               if (packTab(id)) BuiltInRegistries.CREATIVE_MODE_TAB.getValue(id).buildContents(params);   // fills SOURCE
            }
            List<ItemStack> added = new ArrayList<>();
            for (Map.Entry<String, List<ItemStack>> e : SOURCE.entrySet()) {
               for (ItemStack s : e.getValue()) {
                  if (category != 0 && categoryOf(e.getKey(), s) != category) continue;
                  boolean dup = false;
                  for (ItemStack a : added) if (ItemStack.isSameItemSameComponents(a, s)) { dup = true; break; }
                  if (dup) continue;
                  added.add(s);
                  out.accept(s.copy());
               }
            }
         })
         .build());
   }
}
