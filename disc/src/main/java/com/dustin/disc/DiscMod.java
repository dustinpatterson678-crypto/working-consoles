package com.dustin.disc;

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

/** Disc consoles that run natively (software-rendered cores): PlayStation, 3DO, PC-FX. */
public class DiscMod implements ModInitializer {
   public static final String MOD_ID = "disc";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   public static final RetroSystem PS1_SYS = RetroSystem.register(new RetroSystem(
      "ps1", "PlayStation", "swanstation", "disc/ps1", "disc",
      List.of(".cue", ".chd", ".m3u", ".pbp", ".iso", ".ccd"),
      List.of("scph5501.bin", "scph1001.bin", "scph7001.bin", "scph5500.bin", "scph5502.bin", "scph1002.bin", "scph7502.bin"),
      Map.of(), 80,
      "Needs your own PlayStation BIOS dump (for example scph5501.bin). Multi-disc games: make a .m3u listing the discs."));

   public static final RetroSystem TDO_SYS = RetroSystem.register(new RetroSystem(
      "tdo", "3DO", "opera", "disc/3do", "disc",
      List.of(".iso", ".cue", ".chd", ".bin"),
      List.of("panafz10.bin", "panafz1.bin", "panafz10-norsa.bin", "goldstar.bin", "sanyotry.bin"),
      Map.of(), 80,
      "Needs your own 3DO BIOS dump (panafz10.bin from a Panasonic FZ-10 is the most compatible)."));

   public static final RetroSystem PCFX_SYS = RetroSystem.register(new RetroSystem(
      "pcfx", "PC-FX", "mednafen_pcfx", "disc/pcfx", "disc",
      List.of(".cue", ".ccd", ".chd", ".toc"),
      List.of("pcfx.rom"),
      Map.of(), 80,
      "Needs your own PC-FX BIOS dump named pcfx.rom."));

   public static final Console PS1 = console(PS1_SYS, "ps1", "PlayStation",
      Block.box(2.0, 0.0, 3.4, 14.0, 3.2, 12.6), Block.box(3.4, 0.0, 2.0, 12.6, 3.2, 14.0),
      "ps1_disc", new int[]{0, 0, 120, 120});
   public static final Console TDO = console(TDO_SYS, "tdo", "3DO",
      Block.box(2.0, 0.0, 2.4, 14.0, 3.2, 12.6), Block.box(3.4, 0.0, 2.0, 13.6, 3.2, 14.0),
      "tdo_disc", new int[]{0, 0, 120, 120});
   public static final Console PCFX = console(PCFX_SYS, "pcfx", "PC-FX",
      Block.box(4.4, 0.0, 3.2, 11.6, 8.2, 12.8), Block.box(3.2, 0.0, 4.4, 12.8, 8.2, 11.6),
      "pcfx_disc", new int[]{0, 0, 120, 120});

   public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main"));

   public static final class Console {
      public RetroSystem system;
      public RetroConsoleBlock block;
      public Item item;
      public Item media;
      public BlockEntityType<RetroConsoleBlockEntity> be;
   }

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
      ManualItem.register(MOD_ID, "Disc Consoles Manual", new String[]{
         "DISC CONSOLES\n\nPlayStation, 3DO and PC-FX, running right inside Minecraft from your own BIOS and disc dumps.",
         "SETTING UP\n\n1. Cable the console to a TV (or HDMI to a Smart TV) and to a Wall Outlet.\n2. Power the outlet with redstone.\n3. Dumps go in disc/ps1, disc/3do or disc/pcfx in your Minecraft folder: games/ and bios/.",
         "BIOS\n\nPlayStation: scph5501.bin (or another scph BIOS).\n3DO: panafz10.bin.\nPC-FX: pcfx.rom.\n\nAll from your own consoles.",
         "PLAYING\n\nCraft a blank disc, right-click it and pick a game, then click it into the console.\n\nSneak + right-click the console to eject."
      });
      Console[] all = {PS1, TDO, PCFX};
      Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricCreativeModeTab.builder()
         .title(Component.literal("Disc Consoles"))
         .icon(() -> new ItemStack(PS1.item))
         .displayItems((context, entries) -> {
            for (Console c : all) {
               entries.accept(c.item);
               entries.accept(new ItemStack(c.media));
               for (Game g : c.system.games()) {
                  entries.accept(RetroMediaItem.of(c.system, g.file().getFileName().toString()));
               }
            }
            entries.accept(ManualItem.of(WorkingConsolesCore.MANUAL, MOD_ID));
         })
         .build());
      LOGGER.info("Disc Consoles loaded. Games go in {}, {} and {}", PS1_SYS.gamesDir(), TDO_SYS.gamesDir(), PCFX_SYS.gamesDir());
   }

   private static ResourceKey<Item> itemKey(String path) {
      return ResourceKey.create(Registries.ITEM, id(path));
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }
}
