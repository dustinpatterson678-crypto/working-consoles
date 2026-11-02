package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.ArcadePlusMod;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** One or two lines under each machine telling you what it takes and how to play. */
final class Tooltips {
   private Tooltips() {
   }

   private static final String BOARD = "Plays an Arcade Board or Modern Arcade Board (your own MAME romset). Plug it into a live Wall Outlet, then click the board in.";
   private static final Map<String, String[]> TIPS = new HashMap<>();

   static {
      for (String c : new String[]{"retro_upright", "turbo_moto", "star_pilot", "versus_fighter", "dustin_multi", "racing_cabinet", "candy_cabinet", "brawler_cabinet"}) {
         TIPS.put(c, new String[]{"Video arcade cabinet", BOARD});
      }
      TIPS.put("turbo_moto", new String[]{"Video arcade: ride-on bike racer", BOARD + " Best with racing games."});
      TIPS.put("star_pilot", new String[]{"Video arcade: sit-in cockpit", BOARD + " Best with flying and space shooters."});
      TIPS.put("versus_fighter", new String[]{"Video arcade: 2-player cabinet", BOARD + " Best with fighting games."});
      TIPS.put("night_shooter", new String[]{"Light-gun cabinet", "Plays a Gun Board (your own light-gun romset). Hold the Light Gun and aim at the screen: right-click fires, shoot off-screen to reload."});
      TIPS.put("gun_cabinet", TIPS.get("night_shooter"));
      TIPS.put("pool", new String[]{"Pool table (2 x 3 blocks)", "Costs 1 nickel a rack, no power needed. Look where to shoot, move the mouse to fine-aim, hold to power up, let go to shoot. Clear the table for points."});
      String nickel = "Costs 1 nickel a play. Plug it into a live Wall Outlet.";
      for (String g : new String[]{"skeeball", "basketball", "whackamole", "coinpusher", "clawmachine", "stacker", "cyclone", "gators", "bigwheel", "echo", "drumwheel",
         "clowns", "plinko", "striker", "hockey", "locker", "keyvault", "speedlights", "dropzone", "rocketrace", "lanes", "meteor", "snipit", "pool", "powerpunch", "deepsea", "beatblaster"}) {
         TIPS.putIfAbsent(g, new String[]{"Ticket game", nickel + " Wins points that turn into tickets for the Prize Booth."});
      }
      TIPS.put("prize_stand", new String[]{"Walk-up prize booth", "Click it to put your tickets on your card and pick a prize."});
   }

   static void init() {
      ItemTooltipCallback.EVENT.register((stack, ctx, flag, lines) -> {
         Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
         if (id == null || !id.getNamespace().equals(ArcadePlusMod.MOD_ID)) return;
         String[] t = TIPS.get(id.getPath());
         if (t == null) return;
         lines.add(Component.literal(t[0]).withStyle(ChatFormatting.GOLD));
         StringBuilder line = new StringBuilder();
         for (String w : t[1].split(" ")) {
            if (line.length() + w.length() > 42) {
               lines.add(Component.literal(line.toString().trim()).withStyle(ChatFormatting.GRAY));
               line.setLength(0);
            }
            line.append(w).append(' ');
         }
         if (line.length() > 0) lines.add(Component.literal(line.toString().trim()).withStyle(ChatFormatting.GRAY));
      });
   }
}
