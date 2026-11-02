package com.dustin.sega;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.item.GeoBlockItem;
import com.dustin.wcore.item.ManualItem;
import com.dustin.wcore.retro.RetroConsoleBlock;
import com.dustin.wcore.retro.RetroConsoleBlockEntity;
import com.dustin.wcore.retro.RetroMediaItem;
import com.dustin.wcore.retro.RetroSystem;
import com.dustin.wcore.retro.RetroSystem.Game;
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

public class SegaMod implements ModInitializer {
   public static final String MOD_ID = "sega";
   public static final Logger LOGGER = LoggerFactory.getLogger("sega");
   public static final RetroSystem GENESIS = RetroSystem.register(
      new RetroSystem(
         "genesis",
         "Genesis",
         "genesis_plus_gx",
         "sega/genesis",
         "cartridge",
         List.of(".md", ".gen", ".smd", ".bin", ".sms", ".gg", ".sg"),
         List.of(),
         Map.of(),
         60,
         "Also plays Master System, Game Gear and SG-1000 dumps. No BIOS needed."
      )
   );
   public static final RetroSystem SATURN = RetroSystem.register(
      new RetroSystem(
         "saturn",
         "Saturn",
         "mednafen_saturn",
         "sega/saturn",
         "disc",
         List.of(".cue", ".chd", ".ccd", ".m3u", ".toc"),
         List.of("mpr-17933.bin", "sega_101.bin"),
         Map.of(),
         100,
         "Needs your own Saturn BIOS dump. Multi-disc games: make a .m3u listing the discs."
      )
   );
   public static BlockEntityType<RetroConsoleBlockEntity> GENESIS_BE;
   public static final ResourceKey<Block> GENESIS_KEY = ResourceKey.create(Registries.BLOCK, id("genesis"));
   public static final RetroConsoleBlock GENESIS_BLOCK = (RetroConsoleBlock)Registry.register(
      BuiltInRegistries.BLOCK,
      GENESIS_KEY,
      new RetroConsoleBlock(
         Properties.of().setId(GENESIS_KEY).strength(2.0F).noOcclusion().sound(SoundType.METAL),
         "Genesis",
         Set.of("genesis"),
         false,
         () -> GENESIS_BE,
         Block.box(1.5, 0.0, 3.0, 14.5, 3.5, 13.0),
         Block.box(3.0, 0.0, 1.5, 13.0, 3.5, 14.5)
      )
   );
   public static final Item GENESIS_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM,
      itemKey("genesis"),
      new GeoBlockItem(
         GENESIS_BLOCK, id("genesis"), new net.minecraft.world.item.Item.Properties().setId(itemKey("genesis")).useBlockDescriptionPrefix().stacksTo(1)
      )
   );
   static {
      GENESIS_BE = (BlockEntityType<RetroConsoleBlockEntity>)Registry.register(
         BuiltInRegistries.BLOCK_ENTITY_TYPE,
         id("genesis"),
         new BlockEntityType((p, s) -> new RetroConsoleBlockEntity(GENESIS_BE, p, s), Set.of(SegaMod.GENESIS_BLOCK))
      );
   }
   public static final Item CARTRIDGE = (Item)Registry.register(
      BuiltInRegistries.ITEM,
      itemKey("cartridge"),
      new RetroMediaItem(
         new net.minecraft.world.item.Item.Properties().setId(itemKey("cartridge")).stacksTo(16), "genesis", id("cartridge"), new int[]{0, 0, 84, 108}
      )
   );
   public static BlockEntityType<RetroConsoleBlockEntity> SATURN_BE;
   public static final ResourceKey<Block> SATURN_KEY = ResourceKey.create(Registries.BLOCK, id("saturn"));
   public static final RetroConsoleBlock SATURN_BLOCK = (RetroConsoleBlock)Registry.register(
      BuiltInRegistries.BLOCK,
      SATURN_KEY,
      new RetroConsoleBlock(
         Properties.of().setId(SATURN_KEY).strength(2.0F).noOcclusion().sound(SoundType.METAL),
         "Saturn",
         Set.of("saturn"),
         false,
         () -> SATURN_BE,
         Block.box(1.0, 0.0, 2.5, 15.0, 4.0, 13.5),
         Block.box(2.5, 0.0, 1.0, 13.5, 4.0, 15.0)
      )
   );
   public static final Item SATURN_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM,
      itemKey("saturn"),
      new GeoBlockItem(
         SATURN_BLOCK, id("saturn"), new net.minecraft.world.item.Item.Properties().setId(itemKey("saturn")).useBlockDescriptionPrefix().stacksTo(1)
      )
   );
   static {
      SATURN_BE = (BlockEntityType<RetroConsoleBlockEntity>)Registry.register(
         BuiltInRegistries.BLOCK_ENTITY_TYPE,
         id("saturn"),
         new BlockEntityType((p, s) -> new RetroConsoleBlockEntity(SATURN_BE, p, s), Set.of(SegaMod.SATURN_BLOCK))
      );
   }
   public static final Item DISC_CASE = (Item)Registry.register(
      BuiltInRegistries.ITEM,
      itemKey("disc_case"),
      new RetroMediaItem(
         new net.minecraft.world.item.Item.Properties().setId(itemKey("disc_case")).stacksTo(16), "saturn", id("disc_case"), new int[]{0, 0, 120, 120}
      )
   );
   public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main"));

   public void onInitialize() {
      ManualItem.register(
         "sega",
         "SEGA Manual",
         new String[]{
            "SEGA CONSOLES\n\nGenesis: plays Genesis, Master System, Game Gear and SG-1000 cartridge dumps. No BIOS.\n\nSaturn: plays your Saturn discs (.cue .chd). Needs your Saturn BIOS.",
            "SETTING UP\n\n1. Cable the console to a TV and to a Wall Outlet.\n2. Power the outlet's block with redstone.\n3. Put your dumps in sega/genesis/games or sega/saturn/games in your Minecraft folder.",
            "PLAYING\n\nCraft a blank cartridge or disc, right-click it and pick a game. Click it into the console: it boots on the TV.\n\nSneak + right-click the console to eject.",
            "SATURN BIOS\n\nPut your own dump in sega/saturn/bios named mpr-17933.bin (US/EU) or sega_101.bin (Japan).\n\nThe emulators download themselves the first time you play."
         }
      );
      Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB,
         TAB_KEY,
         FabricCreativeModeTab.builder()
            .title(Component.literal("SEGA in Minecraft"))
            .icon(() -> new ItemStack(GENESIS_ITEM))
            .displayItems((context, entries) -> {
               entries.accept(GENESIS_ITEM);
               entries.accept(new ItemStack(CARTRIDGE));

               for (Game g : GENESIS.games()) {
                  entries.accept(RetroMediaItem.of(GENESIS, g.file().getFileName().toString()));
               }

               entries.accept(SATURN_ITEM);
               entries.accept(new ItemStack(DISC_CASE));

               for (Game g : SATURN.games()) {
                  entries.accept(RetroMediaItem.of(SATURN, g.file().getFileName().toString()));
               }

               entries.accept(ManualItem.of(WorkingConsolesCore.MANUAL, "sega"));
            })
            .build()
      );
      LOGGER.info("SEGA in Minecraft loaded. Games go in {} and {}", GENESIS.gamesDir(), SATURN.gamesDir());
   }

   private static ResourceKey<Item> itemKey(String path) {
      return ResourceKey.create(Registries.ITEM, id(path));
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("sega", path);
   }

   static {
      GENESIS.mediaItem = () -> CARTRIDGE;
      SATURN.mediaItem = () -> DISC_CASE;
   }
}
