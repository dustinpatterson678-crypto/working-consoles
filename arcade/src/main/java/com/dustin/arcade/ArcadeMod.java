package com.dustin.arcade;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.item.GeoBlockItem;
import com.dustin.wcore.item.ManualItem;
import com.dustin.wcore.retro.RetroConsoleBlock;
import com.dustin.wcore.retro.RetroConsoleBlockEntity;
import com.dustin.wcore.retro.RetroMediaItem;
import com.dustin.wcore.retro.RetroSystem;
import com.dustin.wcore.retro.TallCabinetBlock;
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

public class ArcadeMod implements ModInitializer {
   public static final String MOD_ID = "arcade";
   public static final Logger LOGGER = LoggerFactory.getLogger("arcade");
   public static final RetroSystem ARCADE = RetroSystem.register(
      new RetroSystem(
         "arcade",
         "Arcade",
         "mame2003_plus",
         "arcade",
         "board",
         List.of(".zip"),
         List.of(),
         Map.of(),
         60,
         "Arcade boards are MAME 2003-Plus romsets (.zip, keep the romset's own name, e.g. sf2.zip). Parent romsets go in the same folder."
      )
   );
   public static BlockEntityType<RetroConsoleBlockEntity> CABINET_BE;
   public static final ResourceKey<Block> CABINET_KEY = ResourceKey.create(Registries.BLOCK, id("cabinet"));
   public static final RetroConsoleBlock CABINET = (RetroConsoleBlock)Registry.register(
      BuiltInRegistries.BLOCK,
      CABINET_KEY,
      new RetroConsoleBlock(
         Properties.of().setId(CABINET_KEY).strength(2.5F).noOcclusion().sound(SoundType.WOOD),
         "arcade cabinet",
         Set.of("arcade", "genesis"),
         true,
         () -> CABINET_BE,
         Block.box(2.0, 0.0, 1.5, 14.0, 16.0, 13.5),
         Block.box(1.5, 0.0, 2.0, 13.5, 16.0, 14.0)
      )
   );
   public static final Item CABINET_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM,
      itemKey("cabinet"),
      new GeoBlockItem(CABINET, id("cabinet"), new net.minecraft.world.item.Item.Properties().setId(itemKey("cabinet")).useBlockDescriptionPrefix().stacksTo(1))
   );
   static {
      CABINET_BE = (BlockEntityType<RetroConsoleBlockEntity>)Registry.register(
         BuiltInRegistries.BLOCK_ENTITY_TYPE,
         id("cabinet"),
         new BlockEntityType((p, s) -> new RetroConsoleBlockEntity(CABINET_BE, p, s), Set.of(ArcadeMod.CABINET))
      );
   }
   public static BlockEntityType<RetroConsoleBlockEntity> BIG_CABINET_BE;
   public static final ResourceKey<Block> BIG_CABINET_KEY = ResourceKey.create(Registries.BLOCK, id("big_cabinet"));
   public static final TallCabinetBlock BIG_CABINET = (TallCabinetBlock)Registry.register(
      BuiltInRegistries.BLOCK,
      BIG_CABINET_KEY,
      new TallCabinetBlock(
         Properties.of().setId(BIG_CABINET_KEY).strength(2.5F).noOcclusion().sound(SoundType.WOOD), "arcade cabinet", Set.of("*"), () -> BIG_CABINET_BE
      )
   );
   public static final Item BIG_CABINET_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM,
      itemKey("big_cabinet"),
      new GeoBlockItem(
         BIG_CABINET, id("big_cabinet"), new net.minecraft.world.item.Item.Properties().setId(itemKey("big_cabinet")).useBlockDescriptionPrefix().stacksTo(1)
      )
   );
   static {
      BIG_CABINET_BE = (BlockEntityType<RetroConsoleBlockEntity>)Registry.register(
         BuiltInRegistries.BLOCK_ENTITY_TYPE,
         id("big_cabinet"),
         new BlockEntityType((p, s) -> new RetroConsoleBlockEntity(BIG_CABINET_BE, p, s), Set.of(ArcadeMod.BIG_CABINET))
      );
   }
   public static final Item BOARD = (Item)Registry.register(
      BuiltInRegistries.ITEM,
      itemKey("board"),
      new RetroMediaItem(new net.minecraft.world.item.Item.Properties().setId(itemKey("board")).stacksTo(16), "arcade", id("board"), new int[]{0, 0, 144, 96})
   );
   public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main"));

   public void onInitialize() {
      ManualItem.register(
         "arcade",
         "Arcade Cabinet Manual",
         new String[]{
            "ARCADE CABINETS\n\nArcade Cabinet: two blocks tall, takes every kind of game.\n\nBartop Arcade: boards and Genesis cartridges.\n\nBoth have their own screen: just cable them to a live Wall Outlet.",
            "ARCADE BOARDS\n\nYour own MAME 2003-Plus romsets (.zip) go in the arcade/games folder in your Minecraft folder. Keep each romset's own name (sf2.zip).",
            "PLAYING\n\nCraft a blank arcade board, right-click it and pick a game. Click it into the cabinet: it boots right away.\n\nCoin: Back/Select. Start: Start.\nSneak + right-click to take the board out."
         }
      );
      Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB,
         TAB_KEY,
         FabricCreativeModeTab.builder()
            .title(Component.literal("Arcade in Minecraft"))
            .icon(() -> new ItemStack(CABINET_ITEM))
            .displayItems((context, entries) -> {
               entries.accept(BIG_CABINET_ITEM);
               entries.accept(CABINET_ITEM);
               entries.accept(new ItemStack(BOARD));

               for (Game g : ARCADE.games()) {
                  entries.accept(RetroMediaItem.of(ARCADE, g.file().getFileName().toString()));
               }

               entries.accept(ManualItem.of(WorkingConsolesCore.MANUAL, "arcade"));
            })
            .build()
      );
      LOGGER.info("Arcade in Minecraft loaded. Boards go in {}", ARCADE.gamesDir());
   }

   private static ResourceKey<Item> itemKey(String path) {
      return ResourceKey.create(Registries.ITEM, id(path));
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("arcade", path);
   }

   static {
      ARCADE.mediaItem = () -> BOARD;
   }
}
