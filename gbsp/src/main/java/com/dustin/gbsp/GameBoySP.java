package com.dustin.gbsp;

import com.dustin.gbsp.block.HandheldBlock;
import com.dustin.gbsp.block.HandheldBlockEntity;
import com.dustin.gbsp.emu.SystemFiles;
import com.dustin.gbsp.item.GameBoySPItem;
import com.dustin.gbsp.item.GameCaseItem;
import com.dustin.gbsp.item.GameLinkCableItem;
import com.dustin.gbsp.item.GuideBookItem;
import com.dustin.gbsp.link.LinkCableNetworking;
import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.item.ManualItem;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.ModifyOutput;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameBoySP implements ModInitializer {
   public static final String MOD_ID = "gbsp";
   public static final Logger LOGGER = LoggerFactory.getLogger("gbsp");
   public static final ResourceKey<Item> GAME_BOY_SP_KEY = ResourceKey.create(Registries.ITEM, id("game_boy_sp"));
   public static final Item GAME_BOY_SP = (Item)Registry.register(
      BuiltInRegistries.ITEM, GAME_BOY_SP_KEY, new GameBoySPItem(new Properties().setId(GAME_BOY_SP_KEY).stacksTo(1))
   );
   public static BiConsumer<SystemFiles.Report, Boolean> CLIENT_USE = (report, sneaking) -> {};
   public static BooleanSupplier HANDHELD_ON = () -> false;
   public static Predicate<BlockPos> HANDHELD_ON_AT = pos -> false;
   public static Consumer<BlockPos> CLIENT_BLOCK_USE = pos -> {};
   public static Consumer<BlockPos> CLIENT_PLACED = pos -> {};
   public static Consumer<Boolean> BOOK_USE = back -> {};
   public static IntSupplier BOOK_STATE = () -> 0;
   public static Consumer<String> CLIENT_CASE_USE = rom -> {};
   public static final ResourceKey<Item> GAME_CASE_KEY = ResourceKey.create(Registries.ITEM, id("game_case"));
   public static final Item GAME_CASE = (Item)Registry.register(
      BuiltInRegistries.ITEM, GAME_CASE_KEY, new GameCaseItem(new Properties().setId(GAME_CASE_KEY).stacksTo(1))
   );
   public static final ResourceKey<Item> GUIDE_BOOK_KEY = ResourceKey.create(Registries.ITEM, id("guide_book"));
   public static final ResourceKey<Item> LINK_CABLE_KEY = ResourceKey.create(Registries.ITEM, id("link_cable"));
   public static final Item LINK_CABLE = (Item)Registry.register(
      BuiltInRegistries.ITEM, LINK_CABLE_KEY, new GameLinkCableItem(new Properties().setId(LINK_CABLE_KEY).stacksTo(1))
   );
   public static final Item GUIDE_BOOK = (Item)Registry.register(
      BuiltInRegistries.ITEM, GUIDE_BOOK_KEY, new GuideBookItem(new Properties().setId(GUIDE_BOOK_KEY).stacksTo(1))
   );
   public static final ResourceKey<Block> HANDHELD_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, id("handheld"));
   public static final HandheldBlock HANDHELD_BLOCK = (HandheldBlock)Registry.register(
      BuiltInRegistries.BLOCK,
      HANDHELD_BLOCK_KEY,
      new HandheldBlock(
         net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(HANDHELD_BLOCK_KEY).strength(0.4F).noOcclusion().sound(SoundType.LANTERN)
      )
   );
   public static final BlockEntityType<HandheldBlockEntity> HANDHELD_BLOCK_ENTITY = (BlockEntityType<HandheldBlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, id("handheld"), new BlockEntityType(HandheldBlockEntity::new, Set.of(HANDHELD_BLOCK))
   );
   public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main"));

   public void onInitialize() {
      LinkCableNetworking.register();
      ManualItem.register(
         "gbsp",
         "Handheld Manual",
         new String[]{
            "CLAMSHELL HANDHELD\n\nPut your own GBA BIOS dump in gameboy/bios and your game dumps in gameboy/roms (.gba .gbc .gb).\n\nHold it and right-click to open it and play.",
            "GAMES\n\nType /gbsp to pick a game, or use a game case. Place it on a table to play it there.\n\nIt runs on batteries: no outlet needed.",
            "LINK CABLE\n\nUse a Game Link Cable on a friend within 4 blocks to link your handhelds: trade and battle like the real thing.\n\nUse it in the air, walk apart or open your inventory to unplug."
         }
      );
      SystemFiles.ensureFolders();
      Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB,
         TAB_KEY,
         FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.gbsp.main"))
            .icon(() -> new ItemStack(GAME_BOY_SP))
            .displayItems((context, entries) -> {
               entries.accept(GAME_BOY_SP);
               entries.accept(GUIDE_BOOK);
               entries.accept(LINK_CABLE);
               entries.accept(ManualItem.of(WorkingConsolesCore.MANUAL, "gbsp"));

               for (SystemFiles.Rom rom : SystemFiles.scan().roms()) {
                  entries.accept(GameCaseItem.caseFor(rom));
               }
            })
            .build()
      );
      CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register((ModifyOutput)content -> content.accept(GAME_BOY_SP));
      CommandRegistrationCallback.EVENT
         .register(
            (CommandRegistrationCallback)(dispatcher, context, selection) -> dispatcher.register(
               (LiteralArgumentBuilder)Commands.literal("gbsp_case").then(Commands.argument("file", StringArgumentType.greedyString()).executes(ctx -> {
                  String file = StringArgumentType.getString(ctx, "file");
                  ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();

                  for (SystemFiles.Rom rom : SystemFiles.scan().roms()) {
                     if (rom.file().getFileName().toString().equals(file)) {
                        ItemStack stack = GameCaseItem.caseFor(rom);
                        if (!player.getInventory().add(stack)) {
                           Block.popResource(player.level(), player.blockPosition(), stack);
                        }

                        player.sendSystemMessage(Component.literal("Here's the case for " + rom.title()));
                        return 1;
                     }
                  }

                  player.sendSystemMessage(Component.literal("No game file named " + file));
                  return 0;
               }))
            )
         );
      ServerPlayConnectionEvents.JOIN.register((Join)(handler, sender, server) -> {
         ServerPlayer player = handler.player;
         if (player.addTag("gbsp_got_guide")) {
            ItemStack book = new ItemStack(GUIDE_BOOK);
            if (!player.getInventory().add(book)) {
               Block.popResource(player.level(), player.blockPosition(), book);
            }
         }
      });
      LOGGER.info("Game Boy SP loaded. BIOS/ROM folder: {}", SystemFiles.root());
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("gbsp", path);
   }
}
