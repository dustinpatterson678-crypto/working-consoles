package com.dustin.classic;

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

/** Classic computers plus the Sega Master System and Game Gear, all software-rendered cores that run natively in Minecraft. */
public class ClassicMod implements ModInitializer {
   public static final String MOD_ID = "classic";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   public static final Console C64 = console(RetroSystem.register(new RetroSystem(
      "c64", "Commodore 64", "vice_x64sc", "classic/c64", "disk",
      List.of(".d64", ".t64", ".prg", ".crt", ".tap", ".g64", ".d81", ".m3u", ".zip"), List.of(), Map.of(), 80,
      "Disk, tape and cartridge dumps (.d64 .t64 .prg .crt .tap). Press the controller's Select/Back button for the on-screen keyboard.")),
      "c64", "Commodore 64", false,
      Block.box(1.0, 0.0, 4.0, 15.0, 2.8, 12.0), Block.box(4.0, 0.0, 1.0, 12.0, 2.8, 15.0),
      "disk525", new int[]{0, 0, 120, 120});

   public static final Console AMIGA = console(RetroSystem.register(new RetroSystem(
      "amiga", "Amiga 500", "puae", "classic/amiga", "floppy",
      List.of(".adf", ".adz", ".dms", ".ipf", ".hdf", ".lha", ".m3u", ".zip"), List.of("kick34005.A500", "kick37175.A500", "kick40063.A600", "kick40068.A1200"), Map.of(), 80,
      "Needs your own Kickstart ROM dump in bios/ (kick34005.A500 is Kickstart 1.3). Select/Back opens the on-screen keyboard.")),
      "amiga", "Amiga 500", false,
      Block.box(0.5, 0.0, 3.5, 15.5, 2.6, 12.5), Block.box(3.5, 0.0, 0.5, 12.5, 2.6, 15.5),
      "floppy", new int[]{0, 0, 90, 90});

   public static final Console ATARIST = console(RetroSystem.register(new RetroSystem(
      "atarist", "Atari ST", "hatari", "classic/atarist", "floppy",
      List.of(".st", ".msa", ".stx", ".dim", ".ipf", ".m3u", ".zip"), List.of("tos.img"), Map.of(), 80,
      "Needs your own TOS ROM dump in bios/ named tos.img. Select/Back opens the on-screen keyboard.")),
      "atarist", "Atari ST", false,
      Block.box(0.5, 0.0, 3.5, 15.5, 2.4, 12.5), Block.box(3.5, 0.0, 0.5, 12.5, 2.4, 15.5),
      "floppy", new int[]{0, 0, 90, 90});

   public static final Console ZXSPECTRUM = console(RetroSystem.register(new RetroSystem(
      "zxspectrum", "ZX Spectrum", "fuse", "classic/zxspectrum", "tape",
      List.of(".tzx", ".tap", ".z80", ".sna", ".dsk", ".scl", ".trd", ".zip"), List.of(), Map.of(), 60,
      "Tape and snapshot dumps (.tzx .tap .z80 .sna). No BIOS needed. Select/Back opens the on-screen keyboard.")),
      "zxspectrum", "ZX Spectrum", false,
      Block.box(2.5, 0.0, 4.8, 13.5, 1.8, 11.2), Block.box(4.8, 0.0, 2.5, 11.2, 1.8, 13.5),
      "tape", new int[]{0, 0, 96, 60});

   public static final Console CPC = console(RetroSystem.register(new RetroSystem(
      "cpc", "Amstrad CPC", "cap32", "classic/cpc", "disk",
      List.of(".dsk", ".sna", ".cdt", ".kcr", ".m3u", ".zip"), List.of(), Map.of(), 80,
      "Disk and tape dumps (.dsk .cdt .sna). No BIOS needed. Select/Back opens the on-screen keyboard.")),
      "cpc", "Amstrad CPC", false,
      Block.box(0.5, 0.0, 3.8, 15.5, 3.0, 12.2), Block.box(3.8, 0.0, 0.5, 12.2, 3.0, 15.5),
      "floppy", new int[]{0, 0, 90, 90});

   public static final Console DOSPC = console(RetroSystem.register(new RetroSystem(
      "dospc", "DOS PC", "dosbox_pure", "classic/dospc", "disk",
      List.of(".zip", ".dosz", ".exe", ".com", ".bat", ".iso", ".cue", ".img", ".ima", ".vhd", ".m3u"), List.of(), Map.of(), 80,
      "Your DOS games as .zip (DOSBox Pure runs them straight from the zip). No BIOS needed. Select/Back opens the on-screen keyboard.")),
      "dospc", "DOS PC", false,
      Block.box(1.0, 0.0, 3.0, 15.0, 4.8, 13.0), Block.box(3.0, 0.0, 1.0, 13.0, 4.8, 15.0),
      "disk525", new int[]{0, 0, 120, 120});

   public static final Console ATARI800 = console(RetroSystem.register(new RetroSystem(
      "atari800", "Atari 800XL", "atari800", "classic/atari800", "cartridge",
      List.of(".xex", ".atr", ".atx", ".cas", ".car", ".xfd", ".rom", ".bin", ".com", ".zip"), List.of("ATARIXL.ROM"), Map.of(), 80,
      "Needs your own Atari OS ROM dump in bios/ named ATARIXL.ROM (add ATARIBAS.ROM for BASIC).")),
      "atari800", "Atari 800XL", false,
      Block.box(1.0, 0.0, 4.0, 15.0, 2.8, 12.0), Block.box(4.0, 0.0, 1.0, 12.0, 2.8, 15.0),
      "a8_cart", new int[]{0, 0, 84, 72});

   public static final Console SMS = console(RetroSystem.register(new RetroSystem(
      "sms", "Master System", "genesis_plus_gx", "classic/sms", "cartridge",
      List.of(".sms", ".sg", ".bin"), List.of(), Map.of(), 60,
      "Master System and SG-1000 cartridge dumps (.sms .sg). No BIOS needed.")),
      "sms", "Master System", false,
      Block.box(1.5, 0.0, 4.0, 14.5, 3.2, 12.0), Block.box(4.0, 0.0, 1.5, 12.0, 3.2, 14.5),
      "sms_cart", new int[]{0, 0, 84, 108});

   public static final Console GAMEGEAR = console(RetroSystem.register(new RetroSystem(
      "gamegear", "Game Gear", "genesis_plus_gx", "classic/gamegear", "cartridge",
      List.of(".gg"), List.of(), Map.of(), 60,
      "Game Gear cartridge dumps (.gg). No BIOS needed. It has its own screen, so no TV is needed.")),
      "gamegear", "Game Gear", true,
      Block.box(1.5, 0.0, 5.8, 14.5, 8.3, 10.2), Block.box(5.8, 0.0, 1.5, 10.2, 8.3, 14.5),
      "gg_cart", new int[]{0, 0, 60, 78});

   public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main"));

   public static final class Console {
      public RetroSystem system;
      public RetroConsoleBlock block;
      public Item item;
      public Item media;
      public BlockEntityType<RetroConsoleBlockEntity> be;
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static Console console(RetroSystem sys, String blockId, String display, boolean builtInScreen, VoxelShape ns, VoxelShape ew, String mediaModel, int[] cover) {
      String mediaId = blockId + "_media";
      Console c = new Console();
      c.system = sys;
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(blockId));
      c.block = (RetroConsoleBlock)Registry.register(BuiltInRegistries.BLOCK, key,
         new RetroConsoleBlock(Properties.of().setId(key).strength(2.0F).noOcclusion().sound(SoundType.METAL),
            display, Set.of(sys.id), builtInScreen, () -> c.be, ns, ew));
      c.item = (Item)Registry.register(BuiltInRegistries.ITEM, itemKey(blockId),
         new GeoBlockItem(c.block, id(blockId), new Item.Properties().setId(itemKey(blockId)).useBlockDescriptionPrefix().stacksTo(1)));
      c.be = (BlockEntityType<RetroConsoleBlockEntity>)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id(blockId),
         new BlockEntityType((p, s) -> new RetroConsoleBlockEntity(c.be, p, s), Set.of(c.block)));
      c.media = (Item)Registry.register(BuiltInRegistries.ITEM, itemKey(mediaId),
         new RetroMediaItem(new Item.Properties().setId(itemKey(mediaId)).stacksTo(16), sys.id, id(mediaModel), cover));
      sys.mediaItem = () -> c.media;
      return c;
   }

   @Override
   public void onInitialize() {
      ManualItem.register(MOD_ID, "Classics Manual", new String[]{
         "CLASSICS\n\nCommodore 64, Amiga 500, Atari ST, ZX Spectrum, Amstrad CPC, DOS PC, Atari 800XL, Master System and Game Gear.",
         "SETTING UP\n\nCable the machine to a TV (or HDMI to a Smart TV) and a powered Wall Outlet. The Game Gear has its own screen.\n\nDumps go in classic/<machine>/games and BIOS files in classic/<machine>/bios.",
         "KEYBOARDS\n\nThe computers are played with your controller. Press Select / Back to open each computer's on-screen keyboard.",
         "BIOS\n\nAmiga: kick34005.A500\nAtari ST: tos.img\nAtari 800XL: ATARIXL.ROM\n\nAll from your own machines. The others need none."
      });
      Console[] all = {C64, AMIGA, ATARIST, ZXSPECTRUM, CPC, DOSPC, ATARI800, SMS, GAMEGEAR};
      Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricCreativeModeTab.builder()
         .title(Component.literal("Classics"))
         .icon(() -> new ItemStack(C64.item))
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
      LOGGER.info("Classics loaded: {} machines", all.length);
   }

   private static ResourceKey<Item> itemKey(String path) {
      return ResourceKey.create(Registries.ITEM, id(path));
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }
}
