package com.dustin.wcore;

import com.dustin.wcore.block.ArcadeBehavior;
import com.dustin.wcore.block.ArcadeSeats;
import com.dustin.wcore.block.ControllerBlock;
import com.dustin.wcore.block.ControllerBlockEntity;
import com.dustin.wcore.block.DecorBlock;
import com.dustin.wcore.block.DecorBlockEntity;
import com.dustin.wcore.block.GameShelfBlock;
import com.dustin.wcore.block.GameShelfBlockEntity;
import com.dustin.wcore.block.MultiTvBlock;
import com.dustin.wcore.block.OutletBlock;
import com.dustin.wcore.block.PowerMonitorBlock;
import com.dustin.wcore.block.PowerMonitorBlockEntity;
import com.dustin.wcore.block.TallDecorBlock;
import com.dustin.wcore.block.TvBlock;
import com.dustin.wcore.block.TvBlockEntity;
import com.dustin.wcore.block.TvType;
import com.dustin.wcore.block.WideDecorBlock;
import com.dustin.wcore.controller.ControllerType;
import com.dustin.wcore.controller.ControllerTypes;
import com.dustin.wcore.controller.PairedConsole;
import com.dustin.wcore.controller.RegisteredController;
import com.dustin.wcore.emu.BuildInfo;
import com.dustin.wcore.item.CableItem;
import com.dustin.wcore.item.ConsoleControllerItem;
import com.dustin.wcore.item.GeoBlockItem;
import com.dustin.wcore.item.ManualItem;
import com.dustin.wcore.item.WirelessControllerItem;
import com.dustin.wcore.net.ConsoleInputPacket;
import com.dustin.wcore.net.ConsoleSessionManager;
import com.dustin.wcore.net.StreamRelay;
import com.dustin.wcore.retro.RetroCommands;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.dustin.wcore.item.DecorBlockItem;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkingConsolesCore implements ModInitializer {
   public static final String MOD_ID = "wcore";
   public static final Logger LOGGER = LoggerFactory.getLogger("wcore");
   // controller data components: registered before any item so item properties can use them
   public static final DataComponentType<UUID> CONTROLLER_ID = (DataComponentType<UUID>)Registry.register(
      BuiltInRegistries.DATA_COMPONENT_TYPE,
      id("controller_id"),
      DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build()
   );
   public static final DataComponentType<PairedConsole> PAIRED_CONSOLE = (DataComponentType<PairedConsole>)Registry.register(
      BuiltInRegistries.DATA_COMPONENT_TYPE,
      id("paired_console"),
      DataComponentType.<PairedConsole>builder().persistent(PairedConsole.CODEC).networkSynchronized(PairedConsole.STREAM_CODEC).build()
   );
   public static final ResourceKey<Item> CONTROLLER_KEY = itemKey("wireless_controller");
   public static final Item WIRELESS_CONTROLLER = (Item)Registry.register(
      BuiltInRegistries.ITEM, CONTROLLER_KEY, new WirelessControllerItem(new Properties().setId(CONTROLLER_KEY).stacksTo(1))
   );
   public static final ResourceKey<Block> TV_KEY = ResourceKey.create(Registries.BLOCK, id("tv"));
   public static final TvBlock TV = (TvBlock)Registry.register(
      BuiltInRegistries.BLOCK,
      TV_KEY,
      new MultiTvBlock(
         net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(TV_KEY).strength(1.5F).noOcclusion().sound(SoundType.METAL),
         TvType.FLAT
      )
   );
   public static final TvBlock CRT_SMALL = tvBlock(TvType.CRT_SMALL);
   public static final TvBlock CRT_LARGE = tvBlock(TvType.CRT_LARGE);
   public static final TvBlock REAR_PROJECTION = tvBlock(TvType.REAR_PROJECTION);
   public static final TvBlock PROJECTOR = tvBlock(TvType.PROJECTOR);
   public static final TvBlock CEILING_PROJECTOR = tvBlock(TvType.CEILING_PROJECTOR);
   public static final BlockEntityType<TvBlockEntity> TV_BLOCK_ENTITY = (BlockEntityType<TvBlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE,
      id("tv"),
      new BlockEntityType(TvBlockEntity::new, Set.of(TV, CRT_SMALL, CRT_LARGE, REAR_PROJECTION, PROJECTOR, CEILING_PROJECTOR))
   );
   public static final Item TV_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM, itemKey("tv"), new GeoBlockItem(TV, id("tv"), new Properties().setId(itemKey("tv")).useBlockDescriptionPrefix())
   );
   public static final Item CRT_SMALL_ITEM = tvItem(CRT_SMALL);
   public static final Item CRT_LARGE_ITEM = tvItem(CRT_LARGE);
   public static final Item REAR_PROJECTION_ITEM = tvItem(REAR_PROJECTION);
   public static final Item PROJECTOR_ITEM = tvItem(PROJECTOR);
   public static final Item CEILING_PROJECTOR_ITEM = tvItem(CEILING_PROJECTOR);
   public static final ResourceKey<Block> OUTLET_KEY = ResourceKey.create(Registries.BLOCK, id("outlet"));
   public static final OutletBlock OUTLET = (OutletBlock)Registry.register(
      BuiltInRegistries.BLOCK,
      OUTLET_KEY,
      new OutletBlock(
         net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(OUTLET_KEY).strength(0.5F).noCollision().sound(SoundType.STONE)
      )
   );
   public static final Item OUTLET_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM, itemKey("outlet"), new BlockItem(OUTLET, new Properties().setId(itemKey("outlet")).useBlockDescriptionPrefix())
   );
   public static final ResourceKey<Block> POWER_MONITOR_KEY = ResourceKey.create(Registries.BLOCK, id("power_monitor"));
   public static final PowerMonitorBlock POWER_MONITOR = (PowerMonitorBlock)Registry.register(
      BuiltInRegistries.BLOCK,
      POWER_MONITOR_KEY,
      new PowerMonitorBlock(
         net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(POWER_MONITOR_KEY).strength(1.0F).noOcclusion().sound(SoundType.METAL)
      )
   );
   public static final BlockEntityType<PowerMonitorBlockEntity> POWER_MONITOR_BLOCK_ENTITY = (BlockEntityType<PowerMonitorBlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, id("power_monitor"), new BlockEntityType(PowerMonitorBlockEntity::new, Set.of(POWER_MONITOR))
   );
   public static final Item POWER_MONITOR_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM,
      itemKey("power_monitor"),
      new BlockItem(POWER_MONITOR, new Properties().setId(itemKey("power_monitor")).useBlockDescriptionPrefix())
   );
   public static final TagKey<Item> GAME_CASES = TagKey.create(Registries.ITEM, id("game_cases"));
   public static final TagKey<Item> CONSOLES = TagKey.create(Registries.ITEM, id("consoles"));
   public static final TagKey<Item> CONTROLLERS = TagKey.create(Registries.ITEM, id("controllers"));
   public static final ResourceKey<Block> GAME_SHELF_KEY = ResourceKey.create(Registries.BLOCK, id("game_shelf"));
   public static final GameShelfBlock GAME_SHELF = (GameShelfBlock)Registry.register(
      BuiltInRegistries.BLOCK,
      GAME_SHELF_KEY,
      new GameShelfBlock(
         net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(GAME_SHELF_KEY).strength(1.5F).noOcclusion().sound(SoundType.WOOD)
      )
   );
   public static final BlockEntityType<GameShelfBlockEntity> GAME_SHELF_BLOCK_ENTITY = (BlockEntityType<GameShelfBlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, id("game_shelf"), new BlockEntityType(GameShelfBlockEntity::new, Set.of(GAME_SHELF))
   );
   public static final Item GAME_SHELF_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM, itemKey("game_shelf"), new BlockItem(GAME_SHELF, new Properties().setId(itemKey("game_shelf")).useBlockDescriptionPrefix())
   );
   // ---- v5 retro room furniture (static GeckoLib pieces, see DecorRenderer) ----
   public static final DecorBlock TV_STAND = decor("tv_stand", SoundType.WOOD, Block.box(0.0, 0.0, 2.0, 16.0, 16.0, 14.0), null, true, List.of(
      k(0.0, 9.5, 0.3, 0.85F),
      c(-5.35, 5.1, 0.5), c(-3.6, 5.1, 0.5), c(-1.85, 5.1, 0.5),
      c(-5.35, 1.0, 0.5), c(-3.6, 1.0, 0.5), c(-1.85, 1.0, 0.5)
   ));
   public static final DecorBlock ENTERTAINMENT_CENTER = decor(
      "entertainment_center", SoundType.WOOD, Block.box(0.0, 0.0, 1.0, 16.0, 16.0, 15.0), Block.box(0.0, 0.0, 1.0, 16.0, 16.0, 15.0), true, List.of(
         c(-4.375, 3.0, 0.5), c(-2.625, 3.0, 0.5), c(-0.875, 3.0, 0.5), c(0.875, 3.0, 0.5), c(2.625, 3.0, 0.5), c(4.375, 3.0, 0.5),
         c(-4.375, 9.7, 0.5), c(-2.625, 9.7, 0.5), c(-0.875, 9.7, 0.5), c(0.875, 9.7, 0.5), c(2.625, 9.7, 0.5), c(4.375, 9.7, 0.5),
         k(0.0, 16.3, 0.3, 0.75F)
      )
   );
   public static final DecorBlock CRT_CART = decor("crt_cart", SoundType.METAL, Block.box(1.0, 0.0, 3.0, 15.0, 16.0, 13.0), null, false, List.of(
      k(0.0, 8.7, 0.0, 1.0F),
      c(-5.25, 2.7, 0.5), c(-3.5, 2.7, 0.5), c(-1.75, 2.7, 0.5), c(0.0, 2.7, 0.5), c(1.75, 2.7, 0.5), c(3.5, 2.7, 0.5), c(5.25, 2.7, 0.5)
   ));
   public static final DecorBlock MEDIA_CABINET = decor(
      "media_cabinet", SoundType.WOOD, Block.box(0.0, 0.0, 2.0, 16.0, 16.0, 15.0), Block.box(0.0, 0.0, 2.0, 16.0, 16.0, 15.0), true, List.of(
         c(-5.2, 2.8, 0.5), c(-3.45, 2.8, 0.5), c(-1.7, 2.8, 0.5),
         c(-5.2, 11.9, 0.5), c(-3.45, 11.9, 0.5), c(-1.7, 11.9, 0.5),
         c(1.7, 11.9, 0.5), c(3.45, 11.9, 0.5), c(5.2, 11.9, 0.5),
         c(-5.2, 21.9, 0.5), c(-3.45, 21.9, 0.5), c(-1.7, 21.9, 0.5), c(1.7, 21.9, 0.5), c(3.45, 21.9, 0.5), c(5.2, 21.9, 0.5),
         k(3.45, 2.8, 0.3, 0.55F)
      )
   );
   public static final DecorBlock GAMING_DESK = decor("gaming_desk", SoundType.WOOD, Block.box(0.0, 0.0, 1.0, 16.0, 16.0, 15.0), null, true, List.of(
      c(-6.5, 15.8, 0.5), c(-4.75, 15.8, 0.5), c(-3.0, 15.8, 0.5)
   ));
   public static final DecorBlock CONSOLE_RACK = decor(
      "console_rack", SoundType.METAL, Block.box(0.0, 0.0, 2.0, 16.0, 16.0, 15.0), Block.box(0.0, 0.0, 2.0, 16.0, 16.0, 15.0), false, List.of(
         c(-6.125, 2.2, 0.5), c(-4.375, 2.2, 0.5), c(-2.625, 2.2, 0.5), c(-0.875, 2.2, 0.5), c(0.875, 2.2, 0.5), c(2.625, 2.2, 0.5), c(4.375, 2.2, 0.5), c(6.125, 2.2, 0.5),
         k(-4.4, 9.4, 0.3, 0.55F), c(1.025, 9.4, 0.5), c(2.775, 9.4, 0.5), c(4.525, 9.4, 0.5), c(6.275, 9.4, 0.5),
         k(-4.4, 17.2, 0.3, 0.55F), c(1.025, 17.2, 0.5), c(2.775, 17.2, 0.5), c(4.525, 17.2, 0.5), c(6.275, 17.2, 0.5),
         k(-4.4, 25.0, 0.3, 0.55F), c(1.025, 25.0, 0.5), c(2.775, 25.0, 0.5), c(4.525, 25.0, 0.5), c(6.275, 25.0, 0.5)
      )
   );
   public static final DecorBlock CONTROLLER_STAND = decor("controller_stand", SoundType.METAL, Block.box(5.0, 0.0, 5.0, 11.0, 8.0, 11.0), null, false, List.of(
      p(-1.9, 6.6, 0.2), p(1.9, 6.6, 0.2)
   ));
   public static final DecorBlock MEMORY_CARD_RACK = decor("memory_card_rack", SoundType.WOOD, Block.box(4.0, 0.0, 5.0, 12.0, 5.0, 11.0), null, false, List.of(
      r(-3.2, 1.0, 0.55), r(-1.6, 1.0, 0.55), r(0.0, 1.0, 0.55), r(1.6, 1.0, 0.55), r(3.2, 1.0, 0.55)
   ));
   public static final DecorBlock CONSOLE_DISPLAY_SHELF = decor(
      "console_display_shelf", SoundType.WOOD, Block.box(0.0, 0.0, 12.0, 16.0, 16.0, 16.0), null, false, List.of(
         k(0.0, 1.1, 5.0, 0.9F), c(-5.5, 1.1, 5.0), c(-3.75, 1.1, 5.0), c(3.75, 1.1, 5.0), c(5.5, 1.1, 5.0),
         c(-4.375, 8.7, 5.0), c(-2.625, 8.7, 5.0), c(-0.875, 8.7, 5.0), c(0.875, 8.7, 5.0), c(2.625, 8.7, 5.0), c(4.375, 8.7, 5.0),
         c(-6.125, 16.0, 5.0), c(-4.375, 16.0, 5.0), c(-2.625, 16.0, 5.0), c(-0.875, 16.0, 5.0), c(0.875, 16.0, 5.0), c(2.625, 16.0, 5.0), c(4.375, 16.0, 5.0), c(6.125, 16.0, 5.0)
      )
   );
   // 2-wide 2000s AV stand: chunky top slab, two rows of open full-size console
   // bays, wide enough to carry the 2x2 big-screen TVs above it. Authored
   // centred on the seam between its two cells (model x -16..16).
   public static final DecorBlock AV_STAND = decorWide("av_stand", SoundType.WOOD, Block.box(0.0, 0.0, 1.5, 16.0, 16.0, 14.5), false, List.of(
      k(-7.6, 1.2, -0.2, 0.9F), k(7.6, 1.2, -0.2, 0.9F),
      k(-7.6, 7.9, -0.2, 0.9F), k(7.6, 7.9, -0.2, 0.9F)
   ));
   // v5 arcade room. Every piece has a real use (see ArcadeBehavior): carpets
   // and decals are full solid floor blocks with step effects, signs and lights
   // are switches, the stool seats you, the trash voids items, the directory
   // reads out nearby cabinets, and the machines run the token/ticket economy.
   public static final DecorBlock ARCADE_CARPET = decor("arcade_carpet", SoundType.WOOL, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0), null, false, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.CARPET));
   public static final DecorBlock ARCADE_CARPET_ZIGZAG = decor("arcade_carpet_zigzag", SoundType.WOOL, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0), null, false, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.CARPET));
   public static final DecorBlock ARCADE_CARPET_CHECKER = decor("arcade_carpet_checker", SoundType.WOOL, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0), null, false, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.CARPET));
   public static final DecorBlock FLOOR_DECAL_ARROW = decor("floor_decal_arrow", SoundType.WOOL, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0), null, false, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.DECAL_ARROW));
   public static final DecorBlock FLOOR_DECAL_STEPS = decor("floor_decal_steps", SoundType.WOOL, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0), null, false, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.DECAL_STEPS));
   public static final DecorBlock FLOOR_DECAL_STAR = decor("floor_decal_star", SoundType.WOOL, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0), null, false, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.DECAL_STAR));
   public static final DecorBlock NEON_SIGN_OPEN = decor("neon_sign_open", SoundType.METAL, Block.box(0.3, 4.3, 4.5, 15.7, 13.4, 16.0), null, false, 8, List.of());
   public static final DecorBlock NEON_SIGN_PLAY = decor("neon_sign_play", SoundType.METAL, Block.box(0.3, 4.3, 4.5, 15.7, 13.4, 16.0), null, false, 8, List.of());
   public static final DecorBlock NEON_SIGN_ARCADE = decorWide("neon_sign_arcade", SoundType.METAL, Block.box(0.3, 4.3, 4.5, 15.7, 13.4, 16.0), false, 8, List.of());
   public static final DecorBlock NEON_SIGN_TOKENS = decorWide("neon_sign_tokens", SoundType.METAL, Block.box(0.3, 4.3, 4.5, 15.7, 13.4, 16.0), false, 8, List.of());
   public static final DecorBlock EXIT_SIGN = decor("exit_sign", SoundType.METAL, Block.box(0.3, 6.3, 4.5, 15.7, 13.6, 16.0), null, false, 10, List.of());
   public static final DecorBlock ARCADE_DIRECTORY = decorWide("arcade_directory", SoundType.METAL, Block.box(0.3, 6.3, 4.5, 15.7, 16.0, 16.0), false, 7, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.DIRECTORY));
   public static final DecorBlock CEILING_LIGHT = decor("ceiling_light", SoundType.GLASS, Block.box(1.0, 14.0, 1.0, 15.0, 16.0, 15.0), null, false, 15, List.of());
   public static final DecorBlock FLOOR_UPLIGHT = decor("floor_uplight", SoundType.GLASS, Block.box(0.0, 0.0, 0.0, 16.0, 1.7, 16.0), null, false, 12, List.of());
   public static final DecorBlock ARCADE_STOOL = decor("arcade_stool", SoundType.METAL, Block.box(3.8, 0.0, 3.8, 12.2, 10.2, 12.2), null, false, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.STOOL));
   // game room: seats (click to sit, like the stool) and a coffee table
   public static final DecorBlock GAMING_CHAIR = decor("gaming_chair", SoundType.WOOL, Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0), null, false, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.STOOL));
   public static final DecorBlock COUCH = decor("couch", SoundType.WOOL, Block.box(0.0, 0.0, 2.0, 16.0, 8.0, 16.0), null, false, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.STOOL));
   public static final DecorBlock BEAN_BAG = decor("bean_bag", SoundType.WOOL, Block.box(2.0, 0.0, 2.0, 14.0, 6.0, 14.0), null, false, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.STOOL));
   public static final DecorBlock COFFEE_TABLE = decor("coffee_table", SoundType.WOOD, Block.box(0.5, 0.0, 2.5, 15.5, 7.2, 13.5), null, false, List.of());
   public static final DecorBlock TRASH_CAN = decor("trash_can", SoundType.METAL, Block.box(4.2, 0.0, 4.2, 11.8, 16.0, 11.8), null, false, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.TRASH));
   public static final DecorBlock VENDING_MACHINE = decor("vending_machine", SoundType.METAL, Block.box(0.8, 0.0, 2.4, 15.2, 16.0, 13.0), Block.box(0.8, 0.0, 2.4, 15.2, 15.0, 13.0), false, 6, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.VENDING));
   public static final DecorBlock SNACK_MACHINE = decor("snack_machine", SoundType.METAL, Block.box(0.8, 0.0, 2.4, 15.2, 16.0, 13.0), Block.box(0.8, 0.0, 2.4, 15.2, 15.0, 13.0), false, 6, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.SNACK));
   public static final DecorBlock DRINK_MACHINE = decor("drink_machine", SoundType.METAL, Block.box(1.5, 0.0, 3.2, 14.5, 16.0, 12.8), Block.box(1.5, 0.0, 3.2, 14.5, 15.0, 12.8), false, 7, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.DRINK));
   public static final DecorBlock CHANGE_MACHINE = decor("change_machine", SoundType.METAL, Block.box(1.5, 0.0, 3.1, 14.5, 16.0, 12.5), Block.box(1.5, 0.0, 3.1, 14.5, 6.5, 12.5), false, 7, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.CHANGE));
   public static final DecorBlock COIN_RETURN_MACHINE = decor("coin_return_machine", SoundType.METAL, Block.box(2.5, 0.0, 3.6, 13.5, 16.0, 12.4), null, false, 4, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.COIN_RETURN));
   // Counters are one cell tall (wide) with the model poking into the cell
   // above and a matching tall collision shape. They therefore place exactly
   // like the AV stand - no second free cell required, no headroom failures.
   public static final DecorBlock TICKET_COUNTER = decorWide("ticket_counter", SoundType.WOOD, Block.box(0.0, 0.0, 2.0, 16.0, 19.5, 13.5), false, 7, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.TICKET));
   public static final DecorBlock PRIZE_COUNTER = decorWide("prize_counter", SoundType.WOOD, Block.box(0.0, 0.0, 2.0, 16.0, 27.5, 13.5), false, 7, List.of())
      .withUse(ArcadeBehavior.of(ArcadeBehavior.Kind.PRIZE));
   public static final BlockEntityType<DecorBlockEntity> DECOR_BLOCK_ENTITY = (BlockEntityType<DecorBlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE,
      id("decor"),
      new BlockEntityType(
         DecorBlockEntity::new,
         Set.of(
            TV_STAND,
            ENTERTAINMENT_CENTER,
            CRT_CART,
            MEDIA_CABINET,
            GAMING_DESK,
            CONSOLE_RACK,
            CONTROLLER_STAND,
            MEMORY_CARD_RACK,
            CONSOLE_DISPLAY_SHELF,
            AV_STAND,
            ARCADE_CARPET,
            ARCADE_CARPET_ZIGZAG,
            ARCADE_CARPET_CHECKER,
            FLOOR_DECAL_ARROW,
            FLOOR_DECAL_STEPS,
            FLOOR_DECAL_STAR,
            NEON_SIGN_OPEN,
            NEON_SIGN_PLAY,
            NEON_SIGN_ARCADE,
            NEON_SIGN_TOKENS,
            EXIT_SIGN,
            ARCADE_DIRECTORY,
            CEILING_LIGHT,
            FLOOR_UPLIGHT,
            ARCADE_STOOL,
            GAMING_CHAIR,
            COUCH,
            BEAN_BAG,
            COFFEE_TABLE,
            TRASH_CAN,
            VENDING_MACHINE,
            SNACK_MACHINE,
            DRINK_MACHINE,
            CHANGE_MACHINE,
            COIN_RETURN_MACHINE,
            TICKET_COUNTER,
            PRIZE_COUNTER
         )
      )
   );
   public static final Item TV_STAND_ITEM = decorItem(TV_STAND);
   public static final Item ENTERTAINMENT_CENTER_ITEM = decorItem(ENTERTAINMENT_CENTER);
   public static final Item CRT_CART_ITEM = decorItem(CRT_CART);
   public static final Item MEDIA_CABINET_ITEM = decorItem(MEDIA_CABINET);
   public static final Item GAMING_DESK_ITEM = decorItem(GAMING_DESK);
   public static final Item CONSOLE_RACK_ITEM = decorItem(CONSOLE_RACK);
   public static final Item CONTROLLER_STAND_ITEM = decorItem(CONTROLLER_STAND);
   public static final Item MEMORY_CARD_RACK_ITEM = decorItem(MEMORY_CARD_RACK);
   public static final Item CONSOLE_DISPLAY_SHELF_ITEM = decorItem(CONSOLE_DISPLAY_SHELF);
   public static final Item AV_STAND_ITEM = decorItem(AV_STAND);
   public static final Item ARCADE_CARPET_ITEM = decorItem(ARCADE_CARPET);
   public static final Item ARCADE_CARPET_ZIGZAG_ITEM = decorItem(ARCADE_CARPET_ZIGZAG);
   public static final Item ARCADE_CARPET_CHECKER_ITEM = decorItem(ARCADE_CARPET_CHECKER);
   public static final Item FLOOR_DECAL_ARROW_ITEM = decorItem(FLOOR_DECAL_ARROW);
   public static final Item FLOOR_DECAL_STEPS_ITEM = decorItem(FLOOR_DECAL_STEPS);
   public static final Item FLOOR_DECAL_STAR_ITEM = decorItem(FLOOR_DECAL_STAR);
   public static final Item NEON_SIGN_OPEN_ITEM = decorItem(NEON_SIGN_OPEN);
   public static final Item NEON_SIGN_PLAY_ITEM = decorItem(NEON_SIGN_PLAY);
   public static final Item NEON_SIGN_ARCADE_ITEM = decorItem(NEON_SIGN_ARCADE);
   public static final Item NEON_SIGN_TOKENS_ITEM = decorItem(NEON_SIGN_TOKENS);
   public static final Item EXIT_SIGN_ITEM = decorItem(EXIT_SIGN);
   public static final Item ARCADE_DIRECTORY_ITEM = decorItem(ARCADE_DIRECTORY);
   public static final Item CEILING_LIGHT_ITEM = decorItem(CEILING_LIGHT);
   public static final Item FLOOR_UPLIGHT_ITEM = decorItem(FLOOR_UPLIGHT);
   public static final Item ARCADE_STOOL_ITEM = decorItem(ARCADE_STOOL);
   public static final Item GAMING_CHAIR_ITEM = decorItem(GAMING_CHAIR);
   public static final Item COUCH_ITEM = decorItem(COUCH);
   public static final Item BEAN_BAG_ITEM = decorItem(BEAN_BAG);
   public static final Item COFFEE_TABLE_ITEM = decorItem(COFFEE_TABLE);
   public static final Item TRASH_CAN_ITEM = decorItem(TRASH_CAN);
   public static final Item VENDING_MACHINE_ITEM = decorItem(VENDING_MACHINE);
   public static final Item SNACK_MACHINE_ITEM = decorItem(SNACK_MACHINE);
   public static final Item DRINK_MACHINE_ITEM = decorItem(DRINK_MACHINE);
   public static final Item CHANGE_MACHINE_ITEM = decorItem(CHANGE_MACHINE);
   public static final Item COIN_RETURN_MACHINE_ITEM = decorItem(COIN_RETURN_MACHINE);
   public static final Item TICKET_COUNTER_ITEM = decorItem(TICKET_COUNTER);
   public static final Item PRIZE_COUNTER_ITEM = decorItem(PRIZE_COUNTER);
   // ---- arcade economy / machine stock ----
   public static final Item ARCADE_TOKEN = simpleItem("arcade_token");
   public static final Item ARCADE_TICKET = simpleItem("arcade_ticket");
   public static final Item ARCADE_SODA = foodItem("arcade_soda", 2, 0.35F, true);
   public static final Item ARCADE_CHIPS = foodItem("arcade_chips", 4, 0.6F, false);
   public static final Item ARCADE_CANDY = foodItem("arcade_candy", 2, 0.25F, true);
   public static final Item ARCADE_PLUSH = simpleItem("arcade_plush");
   public static final Item MANUAL = (Item)Registry.register(
      BuiltInRegistries.ITEM, itemKey("manual"), new ManualItem(new Properties().setId(itemKey("manual")).stacksTo(1))
   );
   public static final Item CABLE = (Item)Registry.register(
      BuiltInRegistries.ITEM, itemKey("cable"), new CableItem(new Properties().setId(itemKey("cable")).stacksTo(16))
   );
   public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main"));
   public static final ResourceKey<CreativeModeTab> FURNITURE_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("furniture"));
   public static final ResourceKey<CreativeModeTab> ARCADE_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("arcade"));

   private static TvBlock tvBlock(TvType type) {
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(type.model));
      net.minecraft.world.level.block.state.BlockBehaviour.Properties props = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
         .setId(key)
         .strength(1.5F)
         .noOcclusion()
         .sound(type != TvType.CRT_SMALL && type != TvType.CRT_LARGE ? SoundType.METAL : SoundType.GLASS);
      TvBlock block = type.quad() ? new MultiTvBlock(props, type) : new TvBlock(props, type);
      return (TvBlock)Registry.register(BuiltInRegistries.BLOCK, key, block);
   }

   private static Item tvItem(TvBlock block) {
      String name = block.type.model;
      return (Item)Registry.register(
         BuiltInRegistries.ITEM, itemKey(name), new GeoBlockItem(block, id(name), new Properties().setId(itemKey(name)).useBlockDescriptionPrefix())
      );
   }

   private static DecorBlock decor(String name, SoundType sound, VoxelShape lower, VoxelShape upper, boolean animated, List<DecorBlock.DecorSlot> slots) {
      return decor(name, sound, lower, upper, animated, 0, slots);
   }

   private static DecorBlock decor(String name, SoundType sound, VoxelShape lower, VoxelShape upper, boolean animated, int light, List<DecorBlock.DecorSlot> slots) {
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
      DecorBlock block = upper == null
         ? new DecorBlock(decorProps(key, sound, light), id(name), lower, null, animated, slots)
         : new TallDecorBlock(decorProps(key, sound, light), id(name), lower, upper, animated, slots);
      return (DecorBlock)Registry.register(BuiltInRegistries.BLOCK, key, block);
   }

   private static DecorBlock decorWide(String name, SoundType sound, VoxelShape lower, boolean animated, List<DecorBlock.DecorSlot> slots) {
      return decorWide(name, sound, lower, animated, 0, slots);
   }

   private static DecorBlock decorWide(String name, SoundType sound, VoxelShape lower, boolean animated, int light, List<DecorBlock.DecorSlot> slots) {
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
      DecorBlock block = new WideDecorBlock(decorProps(key, sound, light), id(name), lower, animated, slots);
      return (DecorBlock)Registry.register(BuiltInRegistries.BLOCK, key, block);
   }

   private static net.minecraft.world.level.block.state.BlockBehaviour.Properties decorProps(ResourceKey<Block> key, SoundType sound, int light) {
      net.minecraft.world.level.block.state.BlockBehaviour.Properties props = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
         .setId(key)
         .strength(1.5F)
         .noOcclusion()
         .sound(sound);
      return light > 0 ? props.lightLevel(state -> (Boolean)state.getValue(DecorBlock.LIT) ? light : 0) : props;
   }

   /**
    * Registers a console's own controller under the namespace of type.id(): the placeable block, its block entity
    * type and the item (all named type.id()), plus the ControllerTypes entry. Call from the module's ModInitializer
    * (or a static field initialiser); the client then calls WorkingConsolesCoreClient.registerController.
    */
   public static RegisteredController registerController(ControllerType type) {
      return registerController(type, ControllerBlock.DEFAULT_SHAPE);
   }

   /** As above with a custom collision shape, given for a controller facing north. */
   public static RegisteredController registerController(ControllerType type, VoxelShape northShape) {
      ControllerTypes.register(type);
      Identifier name = type.id();
      ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, name);
      ControllerBlock block = (ControllerBlock)Registry.register(
         BuiltInRegistries.BLOCK,
         blockKey,
         new ControllerBlock(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(blockKey).strength(0.3F).noOcclusion().sound(SoundType.STONE),
            type,
            northShape
         )
      );
      BlockEntityType<ControllerBlockEntity> be = (BlockEntityType<ControllerBlockEntity>)Registry.register(
         BuiltInRegistries.BLOCK_ENTITY_TYPE,
         name,
         new BlockEntityType<ControllerBlockEntity>((pos, state) -> new ControllerBlockEntity(block.blockEntityType(), pos, state), Set.of(block))
      );
      block.bind(be);
      ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, name);
      ConsoleControllerItem item = (ConsoleControllerItem)Registry.register(
         BuiltInRegistries.ITEM, itemKey, new ConsoleControllerItem(block, type, new Properties().setId(itemKey).stacksTo(1).useBlockDescriptionPrefix())
      );
      return new RegisteredController(type, block, item, be);
   }

   private static Item simpleItem(String name) {
      ResourceKey<Item> key = itemKey(name);
      return (Item)Registry.register(BuiltInRegistries.ITEM, key, new Item(new Properties().setId(key)));
   }

   private static Item foodItem(String name, int nutrition, float saturation, boolean alwaysEdible) {
      ResourceKey<Item> key = itemKey(name);
      FoodProperties.Builder food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation);
      if (alwaysEdible) {
         food.alwaysEdible();
      }

      return (Item)Registry.register(BuiltInRegistries.ITEM, key, new Item(new Properties().setId(key).food(food.build())));
   }

   // display-slot helpers: c = standing game case, k = console/TV on a shelf,
   // p = controller on a cradle, r = small case leaning back (card rack). The
   // coordinates are model pixels matched to the shelves in gen_furniture.ps1.
   // k size is per piece - as big as that shelf physically fits without the
   // console clipping the woodwork around it.
   private static DecorBlock.DecorSlot c(double x, double y, double z) {
      return new DecorBlock.DecorSlot(DecorBlock.SlotKind.CASE, x, y, z, 0.86F, 90.0F, 0.0F);
   }

   private static DecorBlock.DecorSlot k(double x, double y, double z) {
      return k(x, y, z, 0.85F);
   }

   private static DecorBlock.DecorSlot k(double x, double y, double z, float scale) {
      return new DecorBlock.DecorSlot(DecorBlock.SlotKind.CONSOLE, x, y, z, scale, 0.0F, 0.0F);
   }

   private static DecorBlock.DecorSlot p(double x, double y, double z) {
      return new DecorBlock.DecorSlot(DecorBlock.SlotKind.CONTROLLER, x, y, z, 0.7F, 0.0F, 14.0F);
   }

   private static DecorBlock.DecorSlot r(double x, double y, double z) {
      return new DecorBlock.DecorSlot(DecorBlock.SlotKind.CASE, x, y, z, 0.62F, 0.0F, 18.0F);
   }

   private static Item decorItem(DecorBlock block) {
      String name = block.model.getPath();
      return (Item)Registry.register(
         BuiltInRegistries.ITEM, itemKey(name), new DecorBlockItem(block, new Properties().setId(itemKey(name)).useBlockDescriptionPrefix())
      );
   }

   public void onInitialize() {
      Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB,
         TAB_KEY,
         FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.wcore.main"))
            .icon(() -> new ItemStack(TV_ITEM))
            .displayItems((context, entries) -> {
               entries.accept(TV_ITEM);
               entries.accept(CRT_SMALL_ITEM);
               entries.accept(CRT_LARGE_ITEM);
               entries.accept(REAR_PROJECTION_ITEM);
               entries.accept(PROJECTOR_ITEM);
               entries.accept(CEILING_PROJECTOR_ITEM);
               entries.accept(OUTLET_ITEM);
               entries.accept(POWER_MONITOR_ITEM);
               entries.accept(GAME_SHELF_ITEM);

               for (String m : ManualItem.all().keySet()) {
                  entries.accept(ManualItem.of(MANUAL, m));
               }

               entries.accept(CABLE);
               entries.accept(WIRELESS_CONTROLLER);
            })
            .build()
      );
      Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB,
         FURNITURE_TAB_KEY,
         FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.wcore.furniture"))
            .icon(() -> new ItemStack(TV_STAND_ITEM))
            .displayItems((context, entries) -> {
               entries.accept(TV_STAND_ITEM);
               entries.accept(ENTERTAINMENT_CENTER_ITEM);
               entries.accept(CRT_CART_ITEM);
               entries.accept(MEDIA_CABINET_ITEM);
               entries.accept(GAMING_DESK_ITEM);
               entries.accept(CONSOLE_RACK_ITEM);
               entries.accept(CONTROLLER_STAND_ITEM);
               entries.accept(MEMORY_CARD_RACK_ITEM);
               entries.accept(CONSOLE_DISPLAY_SHELF_ITEM);
               entries.accept(AV_STAND_ITEM);
            })
            .build()
      );
      Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB,
         ARCADE_TAB_KEY,
         FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.wcore.arcade"))
            .icon(() -> new ItemStack(VENDING_MACHINE_ITEM))
            .displayItems((context, entries) -> {
               entries.accept(ARCADE_CARPET_ITEM);
               entries.accept(ARCADE_CARPET_ZIGZAG_ITEM);
               entries.accept(ARCADE_CARPET_CHECKER_ITEM);
               entries.accept(FLOOR_DECAL_ARROW_ITEM);
               entries.accept(FLOOR_DECAL_STEPS_ITEM);
               entries.accept(FLOOR_DECAL_STAR_ITEM);
               entries.accept(NEON_SIGN_OPEN_ITEM);
               entries.accept(NEON_SIGN_PLAY_ITEM);
               entries.accept(NEON_SIGN_ARCADE_ITEM);
               entries.accept(NEON_SIGN_TOKENS_ITEM);
               entries.accept(EXIT_SIGN_ITEM);
               entries.accept(ARCADE_DIRECTORY_ITEM);
               entries.accept(CEILING_LIGHT_ITEM);
               entries.accept(FLOOR_UPLIGHT_ITEM);
               entries.accept(ARCADE_STOOL_ITEM);
               entries.accept(GAMING_CHAIR_ITEM);
               entries.accept(COUCH_ITEM);
               entries.accept(BEAN_BAG_ITEM);
               entries.accept(COFFEE_TABLE_ITEM);
               entries.accept(TRASH_CAN_ITEM);
               entries.accept(VENDING_MACHINE_ITEM);
               entries.accept(SNACK_MACHINE_ITEM);
               entries.accept(DRINK_MACHINE_ITEM);
               entries.accept(CHANGE_MACHINE_ITEM);
               entries.accept(COIN_RETURN_MACHINE_ITEM);
               entries.accept(TICKET_COUNTER_ITEM);
               entries.accept(PRIZE_COUNTER_ITEM);
               entries.accept(ARCADE_TOKEN);
               entries.accept(ARCADE_TICKET);
               entries.accept(ARCADE_SODA);
               entries.accept(ARCADE_CHIPS);
               entries.accept(ARCADE_CANDY);
               entries.accept(ARCADE_PLUSH);
            })
            .build()
      );
      if (BuildInfo.RELEASE && BuildInfo.hasDevProfile()) {
         throw new IllegalStateException("Working Consoles: developer profile found in a RELEASE build. Rebuild with RELEASE.bat.");
      }

      ConsoleSettings.get();
      RetroCommands.register();
      ConsoleInputPacket.register();
      ServerTickEvents.END_SERVER_TICK.register(ConsoleSessionManager::tick);
      ServerTickEvents.END_SERVER_TICK.register(StreamRelay::tick);
      ServerTickEvents.END_SERVER_TICK.register(ArcadeSeats::tick);
      ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, server) -> {
         ConsoleSessionManager.disconnect(handler.player);
         StreamRelay.disconnect(handler.player);
      });
      ManualItem.register(
         "wcore",
         "Working Consoles Manual",
         "WORKING CONSOLES\n\nEvery console needs:\n- a cable to a TV (cabinets have their own screen)\n- a cable to a Wall Outlet\n- redstone power into the outlet's block\n\nUse a Cable on one block, then on the other.",
         "POWER\n\nThe signal at the outlet decides how well a console runs:\n1-7 low voltage\n8-14 normal\n15 clean\n15 steady for a minute: overclock\n\nA flickering signal browns the console out and resets it.",
         "POWER MONITOR\n\nPut one on a wall within 8 blocks of a console. It shows the signal, power grade, FPS and sound.\n\nA comparator next to it reads the grade (0-15).",
         "GAMES\n\nYour own game dumps go in each console's games folder in your Minecraft folder.\n\nCraft a blank cartridge or disc, use it, and pick a game to put on it. Then click it into a console.",
         "GAME SHELF\n\nClick cases in like books. Cover art: a .png or .jpg named like the game, next to it.\n\nCONTROLLER\nAn Xbox pad plays the console you last used. Settings: Mods > Working Consoles Core.",
         "ARCADE\n\nFloors are full blocks. Carpets and step pads give Speed, the star gives Jump Boost. Click the arrow to turn it. Click pads and signs to switch them.\n\nChange machine: 1 emerald = 4 tokens.\nMachines: 1 token = a snack.\nTicket counter: 1 token = 2 tickets.\nPrize counter: 4 tickets = a prize.\n\nStool: click to sit, sneak to stand.\nDirectory: click to list the cabinets nearby."
      );
      LOGGER.info("Working Consoles Core loaded, build {}", BuildInfo.STAMP);
      LOGGER.info(
         "Arcade behaviours: stool={} vending={} ticket={} prize={}",
         new Object[]{ARCADE_STOOL.use != null, VENDING_MACHINE.use != null, TICKET_COUNTER.use != null, PRIZE_COUNTER.use != null}
      );
   }

   private static ResourceKey<Item> itemKey(String path) {
      return ResourceKey.create(Registries.ITEM, id(path));
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("wcore", path);
   }
}
