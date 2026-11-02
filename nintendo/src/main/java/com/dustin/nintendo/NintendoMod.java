package com.dustin.nintendo;

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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Core Pack I, batch 1: NES, Super Nintendo, Nintendo 64. Same pattern as SegaMod, one helper per console. */
public class NintendoMod implements ModInitializer {
   public static final String MOD_ID = "nintendo";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   public static final RetroSystem NES_SYS = RetroSystem.register(new RetroSystem(
      "nes", "NES", "nestopia", "nintendo/nes", "cartridge",
      List.of(".nes", ".unf", ".unif"), List.of(), Map.of(), 60,
      "Plays your own NES / Famicom cartridge dumps. No BIOS needed."));

   public static final RetroSystem SNES_SYS = RetroSystem.register(new RetroSystem(
      "snes", "Super Nintendo", "snes9x", "nintendo/snes", "cartridge",
      List.of(".sfc", ".smc", ".fig", ".swc", ".bs"), List.of(), Map.of(), 60,
      "Plays your own SNES / Super Famicom cartridge dumps. No BIOS needed."));

   // The bridge renders in software, so N64 uses ParaLLEl's software RDP (angrylion) + LLE RSP: no OpenGL context needed.
   public static final RetroSystem N64_SYS = RetroSystem.register(new RetroSystem(
      "n64", "Nintendo 64", "parallel_n64", "nintendo/n64", "cartridge",
      List.of(".z64", ".n64", ".v64"), List.of(),
      Map.of(
         "parallel-n64-gfxplugin", "angrylion",
         "parallel-n64-rspplugin", "cxd4",
         "parallel-n64-angrylion-multithread", "all threads"),
      80,
      "Plays your own N64 cartridge dumps (.z64 .n64 .v64). No BIOS needed. Heavy on the CPU."));

   public static final Console NES = console(NES_SYS, "nes", "NES",
      Block.box(2.0, 0.0, 3.25, 14.0, 3.6, 12.75), Block.box(3.25, 0.0, 2.0, 12.75, 3.6, 14.0),
      "nes_cartridge", new int[]{0, 0, 90, 102});
   public static final Console SNES = console(SNES_SYS, "snes", "Super Nintendo",
      Block.box(2.0, 0.0, 3.4, 14.0, 3.4, 12.6), Block.box(3.4, 0.0, 2.0, 12.6, 3.4, 14.0),
      "snes_cartridge", new int[]{0, 0, 102, 72});
   public static final Console N64 = console(N64_SYS, "n64", "Nintendo 64",
      Block.box(2.0, 0.0, 2.4, 14.0, 3.3, 12.6), Block.box(3.4, 0.0, 2.0, 13.6, 3.3, 14.0),
      "n64_cartridge", new int[]{0, 0, 90, 78});

   public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main"));

   public static final class Console {
      public RetroSystem system;
      public RetroConsoleBlock block;
      public Item item;
      public Item media;
      public BlockEntityType<RetroConsoleBlockEntity> be;
   }

   // each console's own pad (the wireless controller still plays all of them)
   public static final com.dustin.wcore.controller.RegisteredController N64_CONTROLLER = WorkingConsolesCore.registerController(
      com.dustin.wcore.controller.ControllerType.builder(id("n64_controller"))
         .systems("n64")
         .ports(4)
         .bone("stick_l", new com.dustin.wcore.controller.BoneAction.Stick(com.dustin.wcore.controller.BoneAction.LX, com.dustin.wcore.controller.BoneAction.LY, 0, 0.38F))
         .bone("c_pad", new com.dustin.wcore.controller.BoneAction.Stick(com.dustin.wcore.controller.BoneAction.RX, com.dustin.wcore.controller.BoneAction.RY, 0, 0.3F))
         .bone("button_a", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.A, -0.15F))
         .bone("button_b", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.B, -0.15F))
         .bone("button_start", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.START, -0.1F))
         .bone("bumper_l", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.LB, -0.15F))
         .bone("bumper_r", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.RB, -0.15F))
         .bone("trigger_z", new com.dustin.wcore.controller.BoneAction.AnalogPress(com.dustin.wcore.controller.BoneAction.LT, 0.4F))
         .bone("dpad", new com.dustin.wcore.controller.BoneAction.Dpad(0.15F))
         .build()
   );
   public static final com.dustin.wcore.controller.RegisteredController SNES_CONTROLLER = WorkingConsolesCore.registerController(
      com.dustin.wcore.controller.ControllerType.builder(id("snes_controller"))
         .systems("snes")
         .ports(2)
         .bone("button_a", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.A, -0.15F))
         .bone("button_b", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.B, -0.15F))
         .bone("button_x", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.X, -0.15F))
         .bone("button_y", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.Y, -0.15F))
         .bone("button_back", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.BACK, -0.08F))
         .bone("button_start", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.START, -0.08F))
         .bone("bumper_l", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.LB, -0.15F))
         .bone("bumper_r", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.RB, -0.15F))
         .bone("dpad", new com.dustin.wcore.controller.BoneAction.Dpad(0.15F))
         .build()
   );
   public static final com.dustin.wcore.controller.RegisteredController NES_CONTROLLER = WorkingConsolesCore.registerController(
      com.dustin.wcore.controller.ControllerType.builder(id("nes_controller"))
         .systems("nes")
         .ports(2)
         .bone("button_a", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.A, -0.15F))
         .bone("button_b", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.B, -0.15F))
         .bone("button_back", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.BACK, -0.08F))
         .bone("button_start", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.START, -0.08F))
         .bone("dpad", new com.dustin.wcore.controller.BoneAction.Dpad(0.15F))
         .build()
   );

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static Console console(RetroSystem sys, String blockId, String display, VoxelShape ns, VoxelShape ew, String mediaId, int[] cover) {
      Console c = new Console();
      c.system = sys;
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(blockId));
      c.block = (RetroConsoleBlock)Registry.register(BuiltInRegistries.BLOCK, key,
         new RetroConsoleBlock(Properties.of().setId(key).strength(2.0F).noOcclusion().sound(SoundType.METAL),
            display, Set.of(sys.id), false, () -> c.be, ns, ew));
      c.item = (Item)Registry.register(BuiltInRegistries.ITEM, itemKey(blockId),
         new GeoBlockItem(c.block, id(blockId), new Item.Properties().setId(itemKey(blockId)).useBlockDescriptionPrefix().stacksTo(1)));
      c.be = (BlockEntityType<RetroConsoleBlockEntity>)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id(blockId),
         new BlockEntityType((p, s) -> new RetroConsoleBlockEntity(c.be, p, s), Set.of(c.block)));
      c.media = (Item)Registry.register(BuiltInRegistries.ITEM, itemKey(mediaId),
         new RetroMediaItem(new Item.Properties().setId(itemKey(mediaId)).stacksTo(16), sys.id, id(mediaId), cover));
      sys.mediaItem = () -> c.media;
      return c;
   }

   @Override
   public void onInitialize() {
      ManualItem.register(MOD_ID, "Nintendo Manual", new String[]{
         "NINTENDO CONSOLES\n\nNES: your NES / Famicom cartridge dumps.\n\nSuper Nintendo: your SNES / Super Famicom dumps.\n\nNintendo 64: your N64 dumps. No BIOS for any of them.",
         "SETTING UP\n\n1. Cable the console to a TV and to a Wall Outlet.\n2. Power the outlet's block with redstone.\n3. Put your dumps in nintendo/nes/games, nintendo/snes/games or nintendo/n64/games in your Minecraft folder.",
         "PLAYING\n\nCraft a blank cartridge, right-click it and pick a game. Click it into the matching console: it boots on the TV.\n\nSneak + right-click the console to eject.",
         "GOOD TO KNOW\n\nThe emulators download themselves the first time you play.\n\nThe N64 is drawn in software for accuracy, so it works your CPU hardest."
      });
      Console[] all = {NES, SNES, N64};
      Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricCreativeModeTab.builder()
         .title(Component.literal("Nintendo in Minecraft"))
         .icon(() -> new ItemStack(SNES.item))
         .displayItems((context, entries) -> {
            for (Console c : all) {
               entries.accept(c.item);
               entries.accept(new ItemStack(c.media));
               for (Game g : c.system.games()) {
                  entries.accept(RetroMediaItem.of(c.system, g.file().getFileName().toString()));
               }
            }
            entries.accept(NES_CONTROLLER.item());
            entries.accept(SNES_CONTROLLER.item());
            entries.accept(N64_CONTROLLER.item());
            entries.accept(ManualItem.of(WorkingConsolesCore.MANUAL, MOD_ID));
         })
         .build());
      LOGGER.info("Nintendo in Minecraft loaded. Games go in {}, {} and {}", NES_SYS.gamesDir(), SNES_SYS.gamesDir(), N64_SYS.gamesDir());
   }

   private static ResourceKey<Item> itemKey(String path) {
      return ResourceKey.create(Registries.ITEM, id(path));
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }
}
