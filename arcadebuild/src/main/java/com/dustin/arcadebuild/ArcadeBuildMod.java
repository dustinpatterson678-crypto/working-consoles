package com.dustin.arcadebuild;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nickel Arcade Building: one block that builds the whole arcade out of real Minecraft blocks, with every
 * machine placed, plugged into live outlets and ready to play, plus console rooms off a back hallway.
 */
public class ArcadeBuildMod implements ModInitializer {
   public static final String MOD_ID = "arcadebuild";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   public static final ResourceKey<Block> BUILDER_KEY = ResourceKey.create(Registries.BLOCK, id("arcade_builder"));
   public static final Block BUILDER = Registry.register(BuiltInRegistries.BLOCK, BUILDER_KEY,
      new ArcadeBuilderBlock(Properties.of().setId(BUILDER_KEY).strength(1.0F).sound(SoundType.METAL), 0));
   public static final ResourceKey<Item> BUILDER_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("arcade_builder"));
   public static final Item BUILDER_ITEM = Registry.register(BuiltInRegistries.ITEM, BUILDER_ITEM_KEY,
      new BlockItem(BUILDER, new Item.Properties().setId(BUILDER_ITEM_KEY).useBlockDescriptionPrefix()));
   public static final ResourceKey<Block> GRAND_BUILDER_KEY = ResourceKey.create(Registries.BLOCK, id("grand_hall_builder"));
   public static final Block GRAND_BUILDER = Registry.register(BuiltInRegistries.BLOCK, GRAND_BUILDER_KEY,
      new ArcadeBuilderBlock(Properties.of().setId(GRAND_BUILDER_KEY).strength(1.0F).sound(SoundType.METAL), 2));
   public static final ResourceKey<Item> GRAND_BUILDER_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("grand_hall_builder"));
   public static final Item GRAND_BUILDER_ITEM = Registry.register(BuiltInRegistries.ITEM, GRAND_BUILDER_ITEM_KEY,
      new BlockItem(GRAND_BUILDER, new Item.Properties().setId(GRAND_BUILDER_ITEM_KEY).useBlockDescriptionPrefix()));
   public static final ResourceKey<Block> DUSTIN_BUILDER_KEY = ResourceKey.create(Registries.BLOCK, id("dustin_arcade_builder"));
   public static final Block DUSTIN_BUILDER = Registry.register(BuiltInRegistries.BLOCK, DUSTIN_BUILDER_KEY,
      new ArcadeBuilderBlock(Properties.of().setId(DUSTIN_BUILDER_KEY).strength(1.0F).sound(SoundType.METAL), 1));
   public static final ResourceKey<Item> DUSTIN_BUILDER_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("dustin_arcade_builder"));
   public static final Item DUSTIN_BUILDER_ITEM = Registry.register(BuiltInRegistries.ITEM, DUSTIN_BUILDER_ITEM_KEY,
      new BlockItem(DUSTIN_BUILDER, new Item.Properties().setId(DUSTIN_BUILDER_ITEM_KEY).useBlockDescriptionPrefix()));
   public static final ResourceKey<Block> DUSTIN_11_BUILDER_KEY = ResourceKey.create(Registries.BLOCK, id("dustin_arcade_1_1_builder"));
   public static final Block DUSTIN_11_BUILDER = Registry.register(BuiltInRegistries.BLOCK, DUSTIN_11_BUILDER_KEY,
      new ArcadeBuilderBlock(Properties.of().setId(DUSTIN_11_BUILDER_KEY).strength(1.0F).sound(SoundType.METAL), 3));
   public static final ResourceKey<Item> DUSTIN_11_BUILDER_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("dustin_arcade_1_1_builder"));
   public static final Item DUSTIN_11_BUILDER_ITEM = Registry.register(BuiltInRegistries.ITEM, DUSTIN_11_BUILDER_ITEM_KEY,
      new BlockItem(DUSTIN_11_BUILDER, new Item.Properties().setId(DUSTIN_11_BUILDER_ITEM_KEY).useBlockDescriptionPrefix()));

   // ---- building blocks the arcade is made of ----
   public static final java.util.List<Item> BLOCK_ITEMS = new java.util.ArrayList<>();
   public static final Block STUCCO = block("stucco", Properties.of().strength(1.5F).sound(SoundType.STONE), false);
   public static final Block STUCCO_TRIM = block("stucco_trim", Properties.of().strength(1.5F).sound(SoundType.STONE), false);
   public static final Block STOREFRONT_GLASS = block("storefront_glass", Properties.of().strength(0.5F).sound(SoundType.GLASS).noOcclusion(), true);
   public static final Block ARCADE_WALL = block("arcade_wall", Properties.of().strength(1.5F).sound(SoundType.STONE), false);
   public static final Block NEON_TRIM = block("neon_trim", Properties.of().strength(0.5F).sound(SoundType.GLASS).lightLevel(s -> 12), false);
   public static final Block CEILING_TILE = block("ceiling_tile", Properties.of().strength(0.6F).sound(SoundType.WOOL), false);
   public static final Block CEILING_LIGHT_PANEL = block("ceiling_light_panel", Properties.of().strength(0.6F).sound(SoundType.GLASS).lightLevel(s -> 15), false);

   public static final ResourceKey<Block> SHELL_KEY = ResourceKey.create(Registries.BLOCK, id("shell"));
   public static final Block SHELL = Registry.register(BuiltInRegistries.BLOCK, SHELL_KEY,
      new ShellBlock(Properties.of().setId(SHELL_KEY).strength(2.0F, 6.0F).sound(SoundType.STONE).noOcclusion()));

   /** The Grand Hall's walls: invisible and unbreakable (the room is drawn by the hall model). */
   public static final ResourceKey<Block> HALL_SHELL_KEY = ResourceKey.create(Registries.BLOCK, id("hall_shell"));
   public static final Block HALL_SHELL = Registry.register(BuiltInRegistries.BLOCK, HALL_SHELL_KEY,
      new ShellBlock(Properties.of().setId(HALL_SHELL_KEY).strength(-1.0F, 3600000.0F).sound(SoundType.STONE).noOcclusion().noLootTable()));
   public static net.minecraft.world.level.block.entity.BlockEntityType<HallCoreBlockEntity> HALL_CORE_BE;
   public static final ResourceKey<Block> HALL_CORE_KEY = ResourceKey.create(Registries.BLOCK, id("hall_core"));
   public static final Block HALL_CORE = Registry.register(BuiltInRegistries.BLOCK, HALL_CORE_KEY,
      new HallCoreBlock(Properties.of().setId(HALL_CORE_KEY).strength(-1.0F, 3600000.0F).noLootTable(), () -> HALL_CORE_BE));
   public static net.minecraft.world.level.block.entity.BlockEntityType<BreakerBlockEntity> BREAKER_BE;
   public static final ResourceKey<Block> BREAKER_KEY = ResourceKey.create(Registries.BLOCK, id("breaker"));
   public static final Block BREAKER = Registry.register(BuiltInRegistries.BLOCK, BREAKER_KEY,
      new BreakerBlock(Properties.of().setId(BREAKER_KEY).strength(2.0F).sound(SoundType.METAL), () -> BREAKER_BE));

   public static net.minecraft.world.level.block.entity.BlockEntityType<FacadeBlockEntity> FACADE_BE;
   public static net.minecraft.world.level.block.entity.BlockEntityType<PowerStripBlockEntity> POWER_STRIP_BE;
   public static net.minecraft.world.level.block.entity.BlockEntityType<DecorBlockEntity> DECOR_BE;
   public static final ResourceKey<net.minecraft.world.entity.EntityType<?>> SEAT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("seat"));
   public static final net.minecraft.world.entity.EntityType<SeatEntity> SEAT = Registry.register(BuiltInRegistries.ENTITY_TYPE, SEAT_KEY,
      net.minecraft.world.entity.EntityType.Builder.<SeatEntity>of(SeatEntity::new, net.minecraft.world.entity.MobCategory.MISC).sized(0.2F, 0.2F).noSummon().clientTrackingRange(10).build(SEAT_KEY));
   public static final java.util.List<DecorBlock> DECOR = new java.util.ArrayList<>();
   public static final java.util.List<Item> DECOR_ITEMS = new java.util.ArrayList<>();
   public static final ResourceKey<Block> FACADE_KEY = ResourceKey.create(Registries.BLOCK, id("arcade_facade"));
   public static final Block FACADE = Registry.register(BuiltInRegistries.BLOCK, FACADE_KEY,
      new FacadeBlock(Properties.of().setId(FACADE_KEY).strength(1.5F).sound(SoundType.STONE), 0, () -> FACADE_BE));
   public static final ResourceKey<Block> DUSTIN_FACADE_KEY = ResourceKey.create(Registries.BLOCK, id("dustin_facade"));
   public static final Block DUSTIN_FACADE = Registry.register(BuiltInRegistries.BLOCK, DUSTIN_FACADE_KEY,
      new FacadeBlock(Properties.of().setId(DUSTIN_FACADE_KEY).strength(1.5F).sound(SoundType.STONE), 1, () -> FACADE_BE));
   public static final ResourceKey<Block> GRAND_FACADE_KEY = ResourceKey.create(Registries.BLOCK, id("grand_facade"));
   public static final Block GRAND_FACADE = Registry.register(BuiltInRegistries.BLOCK, GRAND_FACADE_KEY,
      new FacadeBlock(Properties.of().setId(GRAND_FACADE_KEY).strength(1.5F).sound(SoundType.STONE), 2, () -> FACADE_BE));
   public static final ResourceKey<Block> DUSTIN_11_FACADE_KEY = ResourceKey.create(Registries.BLOCK, id("dustin_arcade_1_1_facade"));
   public static final Block DUSTIN_11_FACADE = Registry.register(BuiltInRegistries.BLOCK, DUSTIN_11_FACADE_KEY,
      new FacadeBlock(Properties.of().setId(DUSTIN_11_FACADE_KEY).strength(1.5F).sound(SoundType.STONE), 3, () -> FACADE_BE));
   public static final ResourceKey<Block> POWER_STRIP_KEY = ResourceKey.create(Registries.BLOCK, id("giant_power_strip"));
   public static final Block POWER_STRIP = Registry.register(BuiltInRegistries.BLOCK, POWER_STRIP_KEY,
      new PowerStripBlock(Properties.of().setId(POWER_STRIP_KEY).strength(1.0F).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 4)));
   public static final ResourceKey<Item> POWER_STRIP_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("giant_power_strip"));
   public static final Item POWER_STRIP_ITEM = Registry.register(BuiltInRegistries.ITEM, POWER_STRIP_ITEM_KEY,
      new BlockItem(POWER_STRIP, new Item.Properties().setId(POWER_STRIP_ITEM_KEY).useBlockDescriptionPrefix()));
   public static final ResourceKey<Block> CONDUIT_KEY = ResourceKey.create(Registries.BLOCK, id("power_line"));
   public static final Block CONDUIT = Registry.register(BuiltInRegistries.BLOCK, CONDUIT_KEY,
      new PowerConduitBlock(Properties.of().setId(CONDUIT_KEY).strength(0.8F).sound(SoundType.METAL).noOcclusion()));
   public static final ResourceKey<Item> CONDUIT_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("power_line"));
   public static final Item CONDUIT_ITEM = Registry.register(BuiltInRegistries.ITEM, CONDUIT_ITEM_KEY,
      new BlockItem(CONDUIT, new Item.Properties().setId(CONDUIT_ITEM_KEY).useBlockDescriptionPrefix()));

   static {
      @SuppressWarnings({"unchecked", "rawtypes"})
      net.minecraft.world.level.block.entity.BlockEntityType<FacadeBlockEntity> t = (net.minecraft.world.level.block.entity.BlockEntityType<FacadeBlockEntity>)Registry.register(
         BuiltInRegistries.BLOCK_ENTITY_TYPE, id("arcade_facade"),
         new net.minecraft.world.level.block.entity.BlockEntityType((p, s) -> new FacadeBlockEntity(FACADE_BE, (net.minecraft.core.BlockPos)p, (net.minecraft.world.level.block.state.BlockState)s), java.util.Set.of(FACADE, DUSTIN_FACADE, GRAND_FACADE, DUSTIN_11_FACADE)));
      FACADE_BE = t;
      HALL_CORE_BE = (net.minecraft.world.level.block.entity.BlockEntityType<HallCoreBlockEntity>)(Object)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("hall_core"),
         new net.minecraft.world.level.block.entity.BlockEntityType((p, s) -> new HallCoreBlockEntity(HALL_CORE_BE, (net.minecraft.core.BlockPos)p, (net.minecraft.world.level.block.state.BlockState)s), java.util.Set.of(HALL_CORE)));
      @SuppressWarnings({"unchecked", "rawtypes"})
      net.minecraft.world.level.block.entity.BlockEntityType<PowerStripBlockEntity> pt = (net.minecraft.world.level.block.entity.BlockEntityType<PowerStripBlockEntity>)Registry.register(
         BuiltInRegistries.BLOCK_ENTITY_TYPE, id("giant_power_strip"),
         new net.minecraft.world.level.block.entity.BlockEntityType((p, s) -> new PowerStripBlockEntity(POWER_STRIP_BE, (net.minecraft.core.BlockPos)p, (net.minecraft.world.level.block.state.BlockState)s), java.util.Set.of(POWER_STRIP)));
      POWER_STRIP_BE = pt;
      for (DecorBlock.Kind k : DecorBlock.Kind.values()) {
         ResourceKey<Block> dk = ResourceKey.create(Registries.BLOCK, id(k.id));
         DecorBlock db = Registry.register(BuiltInRegistries.BLOCK, dk, new DecorBlock(Properties.of().setId(dk).strength(1.0F).sound(SoundType.WOOD).noOcclusion()
            .lightLevel(s -> k == DecorBlock.Kind.LAMP_POST && s.getValue(DecorBlock.LIT) ? 14 : k == DecorBlock.Kind.MONITORS ? 5 : 0), k));
         DECOR.add(db);
         ResourceKey<Item> di = ResourceKey.create(Registries.ITEM, id(k.id));
         DECOR_ITEMS.add(Registry.register(BuiltInRegistries.ITEM, di, new BlockItem(db, new Item.Properties().setId(di).useBlockDescriptionPrefix())));
      }
      @SuppressWarnings({"unchecked", "rawtypes"})
      net.minecraft.world.level.block.entity.BlockEntityType<DecorBlockEntity> dt = (net.minecraft.world.level.block.entity.BlockEntityType<DecorBlockEntity>)Registry.register(
         BuiltInRegistries.BLOCK_ENTITY_TYPE, id("decor"),
         new net.minecraft.world.level.block.entity.BlockEntityType((p, s) -> new DecorBlockEntity(DECOR_BE, (net.minecraft.core.BlockPos)p, (net.minecraft.world.level.block.state.BlockState)s), new java.util.HashSet<Block>(DECOR)));
      DECOR_BE = dt;
      @SuppressWarnings({"unchecked", "rawtypes"})
      net.minecraft.world.level.block.entity.BlockEntityType<BreakerBlockEntity> bt = (net.minecraft.world.level.block.entity.BlockEntityType<BreakerBlockEntity>)Registry.register(
         BuiltInRegistries.BLOCK_ENTITY_TYPE, id("breaker"),
         new net.minecraft.world.level.block.entity.BlockEntityType((p, s) -> new BreakerBlockEntity(BREAKER_BE, (net.minecraft.core.BlockPos)p, (net.minecraft.world.level.block.state.BlockState)s), java.util.Set.of(BREAKER)));
      BREAKER_BE = bt;
   }

   private static Block block(String name, Properties props, boolean glass) {
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
      props = props.setId(key);
      Block b = Registry.register(BuiltInRegistries.BLOCK, key, glass ? new net.minecraft.world.level.block.TransparentBlock(props) : new Block(props));
      ResourceKey<Item> ik = ResourceKey.create(Registries.ITEM, id(name));
      BLOCK_ITEMS.add(Registry.register(BuiltInRegistries.ITEM, ik, new BlockItem(b, new Item.Properties().setId(ik).useBlockDescriptionPrefix())));
      return b;
   }

   @Override
   public void onInitialize() {
      ServerTickEvents.END_SERVER_TICK.register(server -> BuildJob.tickAll());
      // 1.1: the Grand Hall tick-driven wiring is retired with the hall. HallParts remains registered
      // only so old worlds can still load their legacy blocks.
      com.dustin.arcadebuild.hall.HallParts.init();
      Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main")),
         FabricCreativeModeTab.builder()
            .title(Component.literal("Dustin's Arcade 1.1 Building"))
            .icon(() -> new ItemStack(DUSTIN_11_BUILDER_ITEM))
            .displayItems((ctx, out) -> {
               out.accept(DUSTIN_11_BUILDER_ITEM);
               out.accept(DUSTIN_BUILDER_ITEM);
               out.accept(POWER_STRIP_ITEM);
               out.accept(CONDUIT_ITEM);
               for (Item i : BLOCK_ITEMS) out.accept(i);
               for (Item i : DECOR_ITEMS) out.accept(i);
            })
            .build());
      LOGGER.info("Nickel Arcade Building loaded");
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }
}
