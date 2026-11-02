package com.dustin.arcadeplus;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.item.GeoBlockItem;
import com.dustin.wcore.item.ManualItem;
import com.dustin.wcore.retro.RetroConsoleBlockEntity;
import com.dustin.wcore.retro.RetroMediaItem;
import com.dustin.wcore.retro.RetroSystem;
import com.dustin.wcore.retro.RetroSystem.Game;
import com.dustin.wcore.retro.TallCabinetBlock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Version 7 arcade expansion. Modern arcade boards run current MAME (software rendered, so it runs
 * natively in Minecraft) for late-90s games like Gauntlet Legends; new cabinets take both board kinds.
 */
public class ArcadePlusMod implements ModInitializer {
   public static final String MOD_ID = "arcadeplus";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   public static final RetroSystem MODERN = RetroSystem.register(new RetroSystem(
      "arcade_modern", "Modern Arcade", "mame", "arcade/modern", "board",
      List.of(".zip", ".7z"), List.of(), Map.of(), 80,
      "Current-MAME romsets (.zip, keep the romset's own name, e.g. gauntleg.zip). Games with a hard drive keep their CHD in a folder named after the romset: games/gauntleg/gauntleg.chd."));

   public static final Cab BRAWLER = cab("brawler_cabinet", "4-player brawler cabinet");
   public static final Cab RACER = cab("racing_cabinet", "racing cabinet");
   public static final Cab CANDY = cab("candy_cabinet", "candy cabinet");
   // ---- Dustin's Arcade cabinet line (original designs; each plays whatever board you put in) ----
   public static final Cab RETRO_UPRIGHT = cab("retro_upright", "Retro Classics upright");
   public static final Cab TURBO_MOTO = cab("turbo_moto", "Turbo Moto bike racer");
   public static final Cab STAR_PILOT = cab("star_pilot", "Star Pilot cockpit");
   public static final Cab VERSUS_FIGHTER = cab("versus_fighter", "Versus Fighter cabinet");
   public static final Cab DUSTIN_MULTI = cab("dustin_multi", "Dustin's Multi cabinet");

   // ---- light gun arcade: the wcgun proxy wraps the real MAME core and adds gun aiming ----
   public static final RetroSystem GUN = RetroSystem.register(new RetroSystem(
      "arcade_gun", "Light Gun Arcade", "wcgun_mame2003_plus", "arcade/gun", "gun board",
      List.of(".zip"), List.of(),
      Map.of("mame2003-plus_xy_device", "lightgun", "mame2003-plus_mouse_device", "pointer", "mame2003-plus_crosshair_enabled", "enabled"), 60,
      "Classic light-gun romsets for MAME 2003-Plus (.zip, keep the romset's own name). Pick up the Light Gun, aim at the screen and hold right-click to fire. Shoot off the screen to reload. Sneak + fire drops a coin and presses Start."));
   public static final RetroSystem GUN_MODERN = RetroSystem.register(new RetroSystem(
      "arcade_gun_modern", "Modern Light Gun Arcade", "wcgun_mame", "arcade/gun_modern", "gun board",
      List.of(".zip", ".7z"), List.of(), Map.of("mame_lightgun_mode", "lightgun"), 80,
      "Current-MAME light-gun romsets. Hard-drive games keep their CHD in a folder named after the romset. Aim with the Light Gun like the classic boards."));
   public static final Cab GUN_CAB = cab("gun_cabinet", "light gun cabinet", Set.of("arcade_gun", "arcade_gun_modern"));
   public static final Cab NIGHT_SHOOTER = cab("night_shooter", "Night Shooter Deluxe", Set.of("arcade_gun", "arcade_gun_modern"));
   public static final Item GUN_BOARD = Registry.register(BuiltInRegistries.ITEM, itemKey("gun_board"),
      new RetroMediaItem(new Item.Properties().setId(itemKey("gun_board")).stacksTo(16), "arcade_gun", id("modern_board"), new int[]{0, 0, 144, 96}));
   public static final Item GUN_MODERN_BOARD = Registry.register(BuiltInRegistries.ITEM, itemKey("gun_modern_board"),
      new RetroMediaItem(new Item.Properties().setId(itemKey("gun_modern_board")).stacksTo(16), "arcade_gun_modern", id("modern_board"), new int[]{0, 0, 144, 96}));
   public static final Item LIGHT_GUN = Registry.register(BuiltInRegistries.ITEM, itemKey("light_gun"),
      new LightGunItem(new Item.Properties().setId(itemKey("light_gun")).stacksTo(1)));

   static {
      GUN.mediaItem = () -> GUN_BOARD;
      GUN_MODERN.mediaItem = () -> GUN_MODERN_BOARD;
   }

   public static final ResourceKey<Block> MACHINE_PART_KEY = ResourceKey.create(Registries.BLOCK, id("machine_part"));
   public static final Block MACHINE_PART = Registry.register(BuiltInRegistries.BLOCK, MACHINE_PART_KEY,
      new MachinePartBlock(Properties.of().setId(MACHINE_PART_KEY).strength(2.0F).noOcclusion().noLootTable().sound(SoundType.WOOD)));
   public static final Ticket SKEEBALL = ticket(TicketGame.SKEEBALL);
   public static final Ticket BASKETBALL = ticket(TicketGame.BASKETBALL);
   public static final Ticket WHACKAMOLE = ticket(TicketGame.WHACKAMOLE);
   public static final Ticket COINPUSHER = ticket(TicketGame.COINPUSHER);
   public static final Ticket CLAWMACHINE = ticket(TicketGame.CLAWMACHINE);
   public static final Ticket STACKER = ticket(TicketGame.STACKER);
   public static final Ticket CYCLONE = ticket(TicketGame.CYCLONE);
   public static final Ticket GATORS = ticket(TicketGame.GATORS);
   public static final Ticket BIGWHEEL = ticket(TicketGame.BIGWHEEL);
   public static final Ticket ECHO = ticket(TicketGame.ECHO);
   public static final Ticket DRUMWHEEL = ticket(TicketGame.DRUMWHEEL);
   public static final Ticket CLOWNS = ticket(TicketGame.CLOWNS);
   public static final Ticket PLINKO = ticket(TicketGame.PLINKO);
   public static final Ticket STRIKER = ticket(TicketGame.STRIKER);
   public static final Ticket HOCKEY = ticket(TicketGame.HOCKEY);
   public static final Ticket LOCKER = ticket(TicketGame.LOCKER);
   public static final Ticket KEYVAULT = ticket(TicketGame.KEYVAULT);
   public static final Ticket SPEEDLIGHTS = ticket(TicketGame.SPEEDLIGHTS);
   public static final Ticket DROPZONE = ticket(TicketGame.DROPZONE);
   public static final Ticket ROCKETRACE = ticket(TicketGame.ROCKETRACE);
   public static final Ticket LANES = ticket(TicketGame.LANES);
   public static final Ticket METEOR = ticket(TicketGame.METEOR);
   public static final Ticket SNIPIT = ticket(TicketGame.SNIPIT);
   public static final Ticket POOL = ticket(TicketGame.POOL);
   public static final Ticket POWERPUNCH = ticket(TicketGame.POWERPUNCH);
   public static final Ticket DEEPSEA = ticket(TicketGame.DEEPSEA);
   public static final Ticket BEATBLASTER = ticket(TicketGame.BEATBLASTER);
   public static final Ticket[] TICKET_GAMES = {SKEEBALL, BASKETBALL, WHACKAMOLE, COINPUSHER, CLAWMACHINE, STACKER, CYCLONE, GATORS, BIGWHEEL, ECHO, DRUMWHEEL, CLOWNS, PLINKO, STRIKER, HOCKEY, LOCKER, KEYVAULT, SPEEDLIGHTS, DROPZONE, ROCKETRACE, LANES, METEOR, SNIPIT, POOL, POWERPUNCH, DEEPSEA, BEATBLASTER};
   /** A nickel plays any ticket game or pinball, just like a token: the nickel-arcade way. */
   public static final ResourceKey<net.minecraft.world.entity.EntityType<?>> ATTENDANT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("attendant"));
   public static final net.minecraft.world.entity.EntityType<AttendantEntity> ATTENDANT = Registry.register(BuiltInRegistries.ENTITY_TYPE, ATTENDANT_KEY,
      net.minecraft.world.entity.EntityType.Builder.<AttendantEntity>of(AttendantEntity::new, net.minecraft.world.entity.MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build(ATTENDANT_KEY));
   public static final Item ATTENDANT_BADGE = Registry.register(BuiltInRegistries.ITEM, itemKey("attendant_badge"),
      new AttendantBadgeItem(new Item.Properties().setId(itemKey("attendant_badge")).stacksTo(16)));
   public static final Item NICKEL = Registry.register(BuiltInRegistries.ITEM, itemKey("nickel"),
      new Item(new Item.Properties().setId(itemKey("nickel")).stacksTo(99)));

   private static Set<Block> allPinballs() {
      Set<Block> s = new java.util.HashSet<>();
      s.add(PINBALL);
      s.addAll(PINBALL_THEMES);
      return s;
   }

   public static BlockEntityType<PinballBlockEntity> PINBALL_BE;
   public static final ResourceKey<Block> PINBALL_KEY = ResourceKey.create(Registries.BLOCK, id("pinball"));
   public static final PinballBlock PINBALL = Registry.register(BuiltInRegistries.BLOCK, PINBALL_KEY,
      new PinballBlock(Properties.of().setId(PINBALL_KEY).strength(2.0F).noOcclusion().sound(SoundType.WOOD), () -> PINBALL_BE));
   public static final Item PINBALL_ITEM = Registry.register(BuiltInRegistries.ITEM, itemKey("pinball"),
      new net.minecraft.world.item.BlockItem(PINBALL, new Item.Properties().setId(itemKey("pinball")).useBlockDescriptionPrefix().stacksTo(1)));
   /** Themed tables: same game, different art. */
   public static final java.util.List<PinballBlock> PINBALL_THEMES = new java.util.ArrayList<>();
   public static final java.util.List<Item> PINBALL_THEME_ITEMS = new java.util.ArrayList<>();

   static {
      for (String th : new String[]{"galaxy", "haunted", "pirate", "speedway"}) {
         ResourceKey<Block> k = ResourceKey.create(Registries.BLOCK, id("pinball_" + th));
         PinballBlock b = Registry.register(BuiltInRegistries.BLOCK, k, new PinballBlock(Properties.of().setId(k).strength(2.0F).noOcclusion().sound(SoundType.WOOD), () -> PINBALL_BE, th));
         PINBALL_THEMES.add(b);
         PINBALL_THEME_ITEMS.add(Registry.register(BuiltInRegistries.ITEM, itemKey("pinball_" + th),
            new net.minecraft.world.item.BlockItem(b, new Item.Properties().setId(itemKey("pinball_" + th)).useBlockDescriptionPrefix().stacksTo(1))));
      }
   }

   static {
      @SuppressWarnings({"unchecked", "rawtypes"})
      BlockEntityType<PinballBlockEntity> t = (BlockEntityType<PinballBlockEntity>)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("pinball"),
         new BlockEntityType((p, s) -> new PinballBlockEntity(PINBALL_BE, (net.minecraft.core.BlockPos)p, (net.minecraft.world.level.block.state.BlockState)s), allPinballs()));
      PINBALL_BE = t;
   }

   public static final Item MODERN_BOARD = Registry.register(BuiltInRegistries.ITEM, itemKey("modern_board"),
      new RetroMediaItem(new Item.Properties().setId(itemKey("modern_board")).stacksTo(16), "arcade_modern", id("modern_board"), new int[]{0, 0, 144, 96}));

   static {
      MODERN.mediaItem = () -> MODERN_BOARD;
   }

   public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main"));

   public static final class Cab {
      public TallCabinetBlock block;
      public Item item;
      public BlockEntityType<RetroConsoleBlockEntity> be;
   }

   public static final class Ticket {
      public TicketGameBlock block;
      public Item item;
      public BlockEntityType<TicketGameBlockEntity> be;
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static Ticket ticket(TicketGame game) {
      Ticket t = new Ticket();
      String name = game.id();
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
      t.block = Registry.register(BuiltInRegistries.BLOCK, key,
         new TicketGameBlock(Properties.of().setId(key).lightLevel(s -> s.hasProperty(TicketGameBlock.LIT) && s.getValue(TicketGameBlock.LIT) ? 6 : 0).strength(2.0F).noOcclusion().sound(SoundType.WOOD), game, () -> t.be));
      t.item = Registry.register(BuiltInRegistries.ITEM, itemKey(name),
         new GeoBlockItem(t.block, id(name), new Item.Properties().setId(itemKey(name)).useBlockDescriptionPrefix().stacksTo(1)));
      t.be = (BlockEntityType<TicketGameBlockEntity>)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id(name),
         new BlockEntityType((p, s) -> new TicketGameBlockEntity(t.be, (net.minecraft.core.BlockPos)p, (net.minecraft.world.level.block.state.BlockState)s), Set.of(t.block)));
      return t;
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static Cab cab(String name, String display) {
      return cab(name, display, Set.of("arcade", "arcade_modern"));
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static Cab cab(String name, String display, Set<String> systems) {
      Cab c = new Cab();
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
      // takes classic MAME 2003-Plus boards and the new modern boards
      c.block = Registry.register(BuiltInRegistries.BLOCK, key,
         new GlowCabinetBlock(Properties.of().setId(key).strength(2.5F).noOcclusion().sound(SoundType.WOOD), display, systems, () -> c.be));
      c.item = Registry.register(BuiltInRegistries.ITEM, itemKey(name),
         new GeoBlockItem(c.block, id(name), new Item.Properties().setId(itemKey(name)).useBlockDescriptionPrefix().stacksTo(1)));
      c.be = (BlockEntityType<RetroConsoleBlockEntity>)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id(name),
         new BlockEntityType((p, s) -> new RetroConsoleBlockEntity(c.be, (net.minecraft.core.BlockPos)p, (net.minecraft.world.level.block.state.BlockState)s), Set.of(c.block)));
      return c;
   }

   @Override
   public void onInitialize() {
      net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(ATTENDANT, AttendantEntity.createAttributes());
      Prizes.init();
      ArcadeEconomy.init();
      UnifiedTab.register();
      GunCore.install();
      net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.dustin.arcadeplus.net.PrizeBuyPacket.TYPE, com.dustin.arcadeplus.net.PrizeBuyPacket.CODEC);
      net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.dustin.arcadeplus.net.PrizeBuyPacket.TYPE,
         (payload, context) -> context.server().execute(() -> com.dustin.arcadeplus.net.PrizeBuyPacket.handle(payload, context.player())));
      net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.dustin.arcadeplus.net.PrizeResultPacket.TYPE, com.dustin.arcadeplus.net.PrizeResultPacket.CODEC);
      net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.dustin.arcadeplus.net.PrizeListPacket.TYPE, com.dustin.arcadeplus.net.PrizeListPacket.CODEC);
      net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.dustin.arcadeplus.net.TicketInputPacket.TYPE, com.dustin.arcadeplus.net.TicketInputPacket.CODEC);
      net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.dustin.arcadeplus.net.TicketInputPacket.TYPE,
         (payload, context) -> com.dustin.arcadeplus.net.TicketInputPacket.handle(payload, context.player()));
      net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.dustin.arcadeplus.net.PinballInputPacket.TYPE, com.dustin.arcadeplus.net.PinballInputPacket.CODEC);
      net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.dustin.arcadeplus.net.PinballInputPacket.TYPE,
         (payload, context) -> com.dustin.arcadeplus.net.PinballInputPacket.handle(payload, context.player()));
      ManualItem.register(MOD_ID, "Dustin's Arcade Manual", new String[]{
         "ARCADE DELUXE\n\n4-Player Brawler, Sit-Down Racer and Candy Cabinet. Each takes classic arcade boards and the new Modern Arcade Boards.",
         "MODERN BOARDS\n\nCurrent-MAME romsets go in arcade/modern/games (e.g. gauntleg.zip). Hard-drive games keep their CHD in a folder named after the romset: gauntleg/gauntleg.chd.",
         "THEME KIT\n\nPut your own art next to a game:\n<game>.marquee.png\n<game>.side.png\n\nExample: gauntleg.marquee.png. The cabinet shows it on the marquee and both sides.",
         "TICKET GAMES\n\n19 ticket games, from Alley Roller and the Big Wheel to Plinko Drop, High Striker, Air Hockey, Key Vault, Speed Lights and Drop Zone. Every game takes one nickel.",
         "NICKELS\n\nThe arcade runs on nickels only. Get them at the Change Machine (1 emerald = 20 nickels) or craft 8 from a copper ingot and an iron nugget. Old Arcade Tokens turn into nickels.",
         "LIGHT GUN\n\nPut light-gun romsets in arcade/gun/games (classic) or arcade/gun_modern/games. Hold the Light Gun and aim at the gun cabinet's screen: right-click fires, shooting off-screen reloads, left-click is button 2, sneak + fire = coin + Start.",
         "PRIZE BOOTH\n\nTickets go on your ticket card at the booth. Prices are like a real arcade: candy 10, plushes a few hundred, consoles tens of thousands. Edit config/arcadeplus_prizes.txt to change them.",
         "PINBALL\n\n1 nickel = 3 balls. Right-click the table to play: hold Space to pull the plunger, A / D (or Shift keys, or mouse buttons) for the flippers. Esc to step away. Tickets at 2,500 / 6,000 / 12,000 / 25,000 / 50,000 / 100,000 / 200,000 points.",
         "PLAYING\n\nCable the cabinet to a live Wall Outlet. Write a game onto a board and click it in.\n\nCoin: Back/Select. Start: Start. Sneak + right-click takes the board out."
      });
      Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricCreativeModeTab.builder()
         .title(Component.literal("Dustin's Arcade"))
         .icon(() -> new ItemStack(BRAWLER.item))
         .displayItems((context, entries) -> {
            entries.accept(BRAWLER.item);
            entries.accept(RACER.item);
            entries.accept(CANDY.item);
            entries.accept(new ItemStack(MODERN_BOARD));
            entries.accept(GUN_CAB.item);
            entries.accept(NIGHT_SHOOTER.item);
            entries.accept(RETRO_UPRIGHT.item);
            entries.accept(TURBO_MOTO.item);
            entries.accept(STAR_PILOT.item);
            entries.accept(VERSUS_FIGHTER.item);
            entries.accept(DUSTIN_MULTI.item);
            entries.accept(LIGHT_GUN);
            entries.accept(new ItemStack(GUN_BOARD));
            entries.accept(new ItemStack(GUN_MODERN_BOARD));
            for (Game g : GUN.games()) {
               entries.accept(RetroMediaItem.of(GUN, g.file().getFileName().toString()));
            }
            for (Game g : GUN_MODERN.games()) {
               entries.accept(RetroMediaItem.of(GUN_MODERN, g.file().getFileName().toString()));
            }
            for (Ticket t : TICKET_GAMES) {
               entries.accept(t.item);
            }
            entries.accept(PINBALL_ITEM);
            for (Item i : PINBALL_THEME_ITEMS) entries.accept(i);
            entries.accept(NICKEL);
            entries.accept(ATTENDANT_BADGE);
            entries.accept(Prizes.STAND_ITEM);
            entries.accept(Prizes.BOOTH_ITEM);
            entries.accept(Prizes.WALL_ITEM);
            entries.accept(Prizes.MYSTERY_BOX);
            for (Item i : Prizes.DECOR_ITEMS) {
               entries.accept(i);
            }
            for (Game g : MODERN.games()) {
               entries.accept(RetroMediaItem.of(MODERN, g.file().getFileName().toString()));
            }
            entries.accept(ManualItem.of(WorkingConsolesCore.MANUAL, MOD_ID));
         })
         .build());
      LOGGER.info("Arcade Deluxe loaded. Modern boards go in {}", MODERN.gamesDir());
   }

   public static ResourceKey<Item> itemKeyOf(String path) {
      return itemKey(path);
   }

   private static ResourceKey<Item> itemKey(String path) {
      return ResourceKey.create(Registries.ITEM, id(path));
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }
}
