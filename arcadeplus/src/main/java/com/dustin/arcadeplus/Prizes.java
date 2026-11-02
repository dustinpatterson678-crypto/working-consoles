package com.dustin.arcadeplus;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Prize blocks, the Mystery Box, and the prize booth's price list (in tickets). */
public final class Prizes {
   private Prizes() {
   }

   /** One line of the price list: an item id, how many you get, and the price in tickets. */
   public record Prize(String id, int count, int cost) {
      public ItemStack stack() {
         Identifier key = Identifier.tryParse(this.id);
         if (key == null) return ItemStack.EMPTY;
         Item i = BuiltInRegistries.ITEM.getValue(key);
         return i == null || i == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(i, Math.max(1, this.count));
      }
   }

   public static final List<Item> DECOR_ITEMS = new ArrayList<>();   // must exist before the blocks below register
   private static final VoxelShape PLUSH = Block.box(3.5, 0, 3.5, 12.5, 13, 12.5);
   public static final Block TEDDY = decor("teddy_bear", PLUSH, SoundType.WOOL, 0);
   public static final Block BUNNY = decor("bunny_plush", Block.box(3.5, 0, 3.5, 12.5, 16, 12.5), SoundType.WOOL, 0);
   public static final Block DUCK = decor("duck_plush", PLUSH, SoundType.WOOL, 0);
   public static final Block CAT = decor("cat_plush", PLUSH, SoundType.WOOL, 0);
   public static final Block DINO = decor("dino_plush", Block.box(3.5, 0, 3.5, 12.5, 13.5, 16), SoundType.WOOL, 0);
   public static final Block PANDA = decor("jumbo_panda", Block.box(1.5, 0, 1.5, 14.5, 16, 14.5), SoundType.WOOL, 0);
   public static final Block LAVA_LAMP = decor("lava_lamp", Block.box(5.5, 0, 5.5, 10.5, 14, 10.5), SoundType.GLASS, 12);
   public static final Block TROPHY = decor("golden_trophy", Block.box(4.5, 0, 4.5, 11.5, 13.2, 11.5), SoundType.METAL, 0);

   public static final ResourceKey<Block> BOOTH_KEY = ResourceKey.create(Registries.BLOCK, ArcadePlusMod.id("prize_booth"));
   public static final Block BOOTH = Registry.register(BuiltInRegistries.BLOCK, BOOTH_KEY,
      new PrizeBoothBlock(Properties.of().setId(BOOTH_KEY).strength(2.0F).noOcclusion().sound(SoundType.GLASS), false));
   public static final Item BOOTH_ITEM = Registry.register(BuiltInRegistries.ITEM, ArcadePlusMod.itemKeyOf("prize_booth"),
      new BlockItem(BOOTH, new Item.Properties().setId(ArcadePlusMod.itemKeyOf("prize_booth")).useBlockDescriptionPrefix()));
   public static final ResourceKey<Block> WALL_KEY = ResourceKey.create(Registries.BLOCK, ArcadePlusMod.id("prize_wall"));
   public static final Block WALL = Registry.register(BuiltInRegistries.BLOCK, WALL_KEY,
      new PrizeBoothBlock(Properties.of().setId(WALL_KEY).strength(2.0F).noOcclusion().sound(SoundType.WOOD), true));
   public static final Item WALL_ITEM = Registry.register(BuiltInRegistries.ITEM, ArcadePlusMod.itemKeyOf("prize_wall"),
      new BlockItem(WALL, new Item.Properties().setId(ArcadePlusMod.itemKeyOf("prize_wall")).useBlockDescriptionPrefix()));
   @SuppressWarnings({"unchecked", "rawtypes"})
   public static final net.minecraft.world.level.block.entity.BlockEntityType<PrizeDisplayBlockEntity> DISPLAY_BE = (net.minecraft.world.level.block.entity.BlockEntityType<PrizeDisplayBlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, ArcadePlusMod.id("prize_display"),
      new net.minecraft.world.level.block.entity.BlockEntityType((p, st) -> new PrizeDisplayBlockEntity(Prizes.DISPLAY_BE, (net.minecraft.core.BlockPos)p, (net.minecraft.world.level.block.state.BlockState)st), java.util.Set.of(BOOTH, WALL)));
   public static net.minecraft.world.level.block.entity.BlockEntityType<PrizeStandBlockEntity> STAND_BE;
   public static final ResourceKey<Block> STAND_KEY = ResourceKey.create(Registries.BLOCK, ArcadePlusMod.id("prize_stand"));
   public static final Block STAND = Registry.register(BuiltInRegistries.BLOCK, STAND_KEY,
      new PrizeStandBlock(Properties.of().setId(STAND_KEY).strength(2.0F).noOcclusion().sound(SoundType.WOOD), false, () -> STAND_BE));
   public static final Item STAND_ITEM = Registry.register(BuiltInRegistries.ITEM, ArcadePlusMod.itemKeyOf("prize_stand"),
      new BlockItem(STAND, new Item.Properties().setId(ArcadePlusMod.itemKeyOf("prize_stand")).useBlockDescriptionPrefix()));
   public static final ResourceKey<Block> STAND_PART_KEY = ResourceKey.create(Registries.BLOCK, ArcadePlusMod.id("prize_stand_part"));
   public static final Block STAND_PART = Registry.register(BuiltInRegistries.BLOCK, STAND_PART_KEY,
      new PrizeStandBlock(Properties.of().setId(STAND_PART_KEY).strength(2.0F).noOcclusion().noLootTable().sound(SoundType.WOOD), true, () -> STAND_BE));
   @SuppressWarnings({"unchecked", "rawtypes"})
   private static final Object STAND_REG = STAND_BE = (net.minecraft.world.level.block.entity.BlockEntityType<PrizeStandBlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, ArcadePlusMod.id("prize_stand"),
      new net.minecraft.world.level.block.entity.BlockEntityType((p, st) -> new PrizeStandBlockEntity(Prizes.STAND_BE, (net.minecraft.core.BlockPos)p, (net.minecraft.world.level.block.state.BlockState)st), java.util.Set.of(STAND)));
   public static final Item MYSTERY_BOX = Registry.register(BuiltInRegistries.ITEM, ArcadePlusMod.itemKeyOf("mystery_box"),
      new MysteryBoxItem(new Item.Properties().setId(ArcadePlusMod.itemKeyOf("mystery_box")).stacksTo(16)));

   /** Default price list (real-arcade scale: a nickel play pays a few tickets), written to config/arcadeplus_prizes.txt. */
   private static final String[][] DEFAULTS = {
      // candy counter (cheap, top shelf)
      {"wcore:arcade_candy", "1", "10"}, {"minecraft:cookie", "4", "15"}, {"wcore:arcade_chips", "1", "20"}, {"minecraft:sweet_berries", "8", "20"},
      {"wcore:arcade_soda", "1", "25"}, {"minecraft:honey_bottle", "1", "30"}, {"minecraft:pumpkin_pie", "2", "40"}, {"minecraft:glow_berries", "8", "40"},
      {"minecraft:cake", "1", "60"}, {"minecraft:firework_rocket", "8", "60"}, {"wcore:arcade_plush", "1", "75"}, {"minecraft:painting", "1", "80"},
      // small prizes
      {"minecraft:lead", "1", "100"}, {"minecraft:compass", "1", "150"}, {"minecraft:clock", "1", "150"}, {"minecraft:bundle", "1", "200"},
      {"minecraft:experience_bottle", "8", "200"}, {"minecraft:goat_horn", "1", "250"}, {"minecraft:spyglass", "1", "250"},
      {"arcadeplus:teddy_bear", "1", "300"}, {"arcadeplus:bunny_plush", "1", "300"}, {"arcadeplus:duck_plush", "1", "300"}, {"arcadeplus:cat_plush", "1", "350"},
      {"minecraft:name_tag", "1", "400"}, {"minecraft:golden_carrot", "8", "400"}, {"minecraft:ender_pearl", "4", "450"}, {"arcadeplus:mystery_box", "1", "500"},
      // big prizes
      {"minecraft:saddle", "1", "600"}, {"minecraft:music_disc_cat", "1", "700"}, {"minecraft:music_disc_13", "1", "700"}, {"arcadeplus:dino_plush", "1", "800"},
      {"minecraft:golden_apple", "1", "900"}, {"minecraft:jukebox", "1", "1000"}, {"minecraft:diamond", "1", "1000"}, {"arcadeplus:lava_lamp", "1", "1200"},
      {"minecraft:axolotl_bucket", "1", "1500"}, {"minecraft:music_disc_pigstep", "1", "2000"}, {"minecraft:diamond_horse_armor", "1", "2500"},
      {"minecraft:heart_of_the_sea", "1", "3000"}, {"arcadeplus:jumbo_panda", "1", "4000"}, {"minecraft:trident", "1", "5000"}, {"arcadeplus:golden_trophy", "1", "6000"},
      {"minecraft:enchanted_golden_apple", "1", "8000"}, {"minecraft:totem_of_undying", "1", "10000"}, {"minecraft:netherite_ingot", "1", "12000"},
      // the top shelf: consoles
      {"gbsp:game_boy_sp", "1", "15000"}, {"nintendo:nes", "1", "20000"}, {"minecraft:elytra", "1", "25000"}, {"nintendo:snes", "1", "30000"}, {"sega:genesis", "1", "30000"},
      {"nintendo:n64", "1", "45000"}, {"sega:saturn", "1", "50000"}, {"ps2:console", "1", "75000"}
   };
   /** Bumped whenever the default prices change: an older price file is backed up and replaced. */
   private static final String PRICE_VERSION = "# prices v4";

   private static List<Prize> server = List.of();
   /** What the client was last sent (the screen and the prize wall show this). */
   public static List<Prize> client = List.of();

   private static java.nio.file.Path file() {
      return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("arcadeplus_prizes.txt");
   }

   /** Server: re-read the price list file (creating it with the defaults if missing). Unknown items are skipped. */
   public static List<Prize> reload() {
      java.nio.file.Path f = file();
      try {
         if (java.nio.file.Files.exists(f) && !java.nio.file.Files.readString(f).contains(PRICE_VERSION)) {
            java.nio.file.Files.move(f, f.resolveSibling("arcadeplus_prizes.old.txt"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
         }
         if (!java.nio.file.Files.exists(f)) {
            StringBuilder sb = new StringBuilder();
            sb.append(PRICE_VERSION).append("  (keep this line, or the file is reset to the defaults on the next update)\n");
            sb.append("# Prize booth price list. One prize per line:   item_id   count   tickets\n");
            sb.append("# Add as many lines as you like, from any mod (example:  minecraft:diamond 1 5000).\n");
            sb.append("# The booth re-reads this file every time it is opened, so no restart is needed.\n");
            sb.append("# Lines starting with # are ignored. Items that are not installed are skipped.\n\n");
            for (String[] d : DEFAULTS) sb.append(d[0]).append(' ').append(d[1]).append(' ').append(d[2]).append('\n');
            java.nio.file.Files.writeString(f, sb.toString());
         }
         List<Prize> l = new ArrayList<>();
         for (String line : java.nio.file.Files.readAllLines(f)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] t = line.split("[\\s,=]+");
            try {
               Prize p = t.length >= 3 ? new Prize(t[0], Integer.parseInt(t[1]), Integer.parseInt(t[2])) : new Prize(t[0], 1, Integer.parseInt(t[1]));
               if (!p.stack().isEmpty() && p.cost() >= 0) l.add(p);
            } catch (RuntimeException ignored) {
               // a typo on one line never breaks the booth
            }
         }
         server = l;
      } catch (java.io.IOException e) {
         if (server.isEmpty()) {
            List<Prize> l = new ArrayList<>();
            for (String[] d : DEFAULTS) {
               Prize p = new Prize(d[0], Integer.parseInt(d[1]), Integer.parseInt(d[2]));
               if (!p.stack().isEmpty()) l.add(p);
            }
            server = l;
         }
      }
      return server;
   }

   /** Server-side current list. */
   public static List<Prize> list() {
      return server.isEmpty() ? reload() : server;
   }

   /** Client: what to show on the wall before the booth was first opened. */
   public static List<Prize> display() {
      if (!client.isEmpty()) return client;
      List<Prize> l = new ArrayList<>();
      for (String[] d : DEFAULTS) {
         Prize p = new Prize(d[0], Integer.parseInt(d[1]), Integer.parseInt(d[2]));
         if (!p.stack().isEmpty()) l.add(p);
      }
      client = l;
      return l;
   }

   private static Block decor(String name, VoxelShape shape, SoundType sound, int light) {
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ArcadePlusMod.id(name));
      Properties props = Properties.of().setId(key).strength(0.5F).noOcclusion().sound(sound);
      if (light > 0) {
         props = props.lightLevel(s -> light);
      }
      Block b = Registry.register(BuiltInRegistries.BLOCK, key, new PrizeDecorBlock(props, shape));
      Item item = Registry.register(BuiltInRegistries.ITEM, ArcadePlusMod.itemKeyOf(name),
         new BlockItem(b, new Item.Properties().setId(ArcadePlusMod.itemKeyOf(name)).useBlockDescriptionPrefix()));
      DECOR_ITEMS.add(item);
      return b;
   }

   public static int countTickets(Player player) {
      Item t = TicketGameLogic.item("arcade_ticket");
      int n = 0;
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack s = player.getInventory().getItem(i);
         if (s.getItem() == t) n += s.getCount();
      }
      return n;
   }

   /** Server: munch loose tickets, check the card, take the price, hand over the prize. Returns a message; null on success text. */
   public static String buy(Player player, int index) {
      List<Prize> l = list();
      if (index < 0 || index >= l.size()) return "That prize is not on the shelf";
      Prize p = l.get(index);
      ItemStack prize = p.stack();
      if (prize.isEmpty()) return "That prize is out of stock";
      TicketBank.deposit(player);
      int bal = TicketBank.balance(player);
      if (!player.isCreative()) {
         if (bal < p.cost()) return "!Not enough tickets: need " + String.format("%,d", p.cost() - bal) + " more";
         TicketBank.set(player, bal - p.cost());
      }
      String name = prize.getHoverName().getString();
      if (!player.getInventory().add(prize)) {
         net.minecraft.world.level.block.Block.popResource(player.level(), player.blockPosition(), prize);
      }
      player.getInventory().setChanged();
      player.containerMenu.broadcastChanges();
      player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Prize Booth: " + name + " for " + String.format("%,d", p.cost())
         + " tickets. Card balance: " + String.format("%,d", TicketBank.balance(player))).withStyle(net.minecraft.ChatFormatting.GOLD));
      return "You got: " + name + "!";
   }

   public static void init() {
      TicketBank.init();
      MegaJackpot.init();
      // class load registers everything above
   }
}
