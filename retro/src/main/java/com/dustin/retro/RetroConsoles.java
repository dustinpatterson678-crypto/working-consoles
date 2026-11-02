package com.dustin.retro;

import com.dustin.wcore.item.GeoBlockItem;
import com.dustin.wcore.retro.RetroConsoleBlock;
import com.dustin.wcore.retro.RetroConsoleBlockEntity;
import com.dustin.wcore.retro.RetroMediaItem;
import com.dustin.wcore.retro.RetroSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Core Pack I, batch 2: the rest of the cartridge / 2D era.
 *
 * <p>One data-driven list of console specs. Every console gets its own block,
 * block entity, item and blank-media item, but all of them extend the same
 * {@link RetroConsoleBlock}/{@link RetroConsoleBlockEntity} base, so cables,
 * outlets, TV routing, redstone power, save files and the mob-hearing
 * {@code ConsoleNoise} behaviour are inherited automatically.
 */
public final class RetroConsoles {
   public static final List<Console> ALL = new ArrayList<>();

   private static final VoxelShape CONSOLE_NS = Block.box(2.0, 0.0, 3.0, 14.0, 4.0, 13.0);
   private static final VoxelShape CONSOLE_EW = Block.box(3.0, 0.0, 2.0, 13.0, 4.0, 14.0);
   private static final VoxelShape HANDHELD_NS = Block.box(3.0, 0.0, 5.0, 13.0, 7.0, 11.0);
   private static final VoxelShape HANDHELD_EW = Block.box(5.0, 0.0, 3.0, 11.0, 7.0, 13.0);
   private static final VoxelShape VECTREX_NS = Block.box(2.0, 0.0, 3.5, 14.0, 8.0, 12.5);
   private static final VoxelShape VECTREX_EW = Block.box(3.5, 0.0, 2.0, 12.5, 8.0, 14.0);

   private RetroConsoles() {
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   public static void register() {
      for (Spec s : SPECS) {
         RetroSystem sys = RetroSystem.register(new RetroSystem(
            s.id(), s.name(), s.core(), "retro/" + s.id(), s.mediaWord(),
            s.extensions(), s.bios(), Map.of(), s.latency(), s.howTo()));

         Console c = new Console();
         c.spec = s;
         c.system = sys;

         ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, RetroMod.id(s.id()));
         VoxelShape ns = s.builtInScreen() ? VECTREX_NS : s.handheld() ? HANDHELD_NS : CONSOLE_NS;
         VoxelShape ew = s.builtInScreen() ? VECTREX_EW : s.handheld() ? HANDHELD_EW : CONSOLE_EW;

         c.block = (RetroConsoleBlock)Registry.register(
            BuiltInRegistries.BLOCK,
            blockKey,
            new RetroConsoleBlock(
               Properties.of().setId(blockKey).strength(2.0F).noOcclusion().sound(SoundType.METAL),
               s.name(),
               Set.of(s.id()),
               s.builtInScreen(),
               () -> c.be,
               ns,
               ew
            )
         );

         c.item = (Item)Registry.register(
            BuiltInRegistries.ITEM,
            itemKey(s.id()),
            new GeoBlockItem(
               c.block,
               RetroMod.id(s.id()),
               new Item.Properties().setId(itemKey(s.id())).useBlockDescriptionPrefix().stacksTo(1)
            )
         );

         c.media = (Item)Registry.register(
            BuiltInRegistries.ITEM,
            itemKey(s.mediaId()),
            new RetroMediaItem(
               new Item.Properties().setId(itemKey(s.mediaId())).stacksTo(16),
               s.id(),
               RetroMod.id(s.mediaModel()),
               s.coverPanel()
            )
         );
         sys.mediaItem = () -> c.media;

         c.be = (BlockEntityType<RetroConsoleBlockEntity>)Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            RetroMod.id(s.id()),
            new BlockEntityType((p, state) -> new RetroConsoleBlockEntity(c.be, p, state), Set.of(c.block))
         );

         ALL.add(c);
      }
   }

   private static ResourceKey<Item> itemKey(String path) {
      return ResourceKey.create(Registries.ITEM, RetroMod.id(path));
   }

   public static final class Console {
      public Spec spec;
      public RetroSystem system;
      public RetroConsoleBlock block;
      public Item item;
      public Item media;
      public BlockEntityType<RetroConsoleBlockEntity> be;
   }

   public record Spec(
      String id,
      String name,
      String core,
      String mediaWord,
      String mediaModel,
      List<String> extensions,
      List<String> bios,
      int latency,
      String howTo,
      boolean handheld,
      boolean builtInScreen,
      double screenY,
      double screenFront,
      double screenW,
      double screenH
   ) {
      public String mediaId() {
         return this.id + switch (this.mediaModel) {
            case "disc" -> "_disc";
            case "card" -> "_card";
            default -> "_cart";
         };
      }

      public int[] coverPanel() {
         return switch (this.mediaModel) {
            case "disc" -> new int[]{0, 0, 108, 108};
            case "card" -> new int[]{0, 0, 96, 72};
            default -> new int[]{0, 0, 90, 102};
         };
      }
   }

   private static Spec of(
      String id,
      String name,
      String core,
      String mediaModel,
      String mediaWord,
      List<String> bios,
      int latency,
      String howTo,
      boolean handheld,
      String... extensions
   ) {
      return new Spec(
         id, name, core, mediaWord, mediaModel, List.of(extensions), bios, latency, howTo,
         handheld, false, 0.0, 0.0, 0.0, 0.0
      );
   }

   private static final List<Spec> SPECS = List.of(
      of("vb", "Virtual Boy", "mednafen_vb", "cartridge", "cartridge", List.of(), 60,
         "Virtual Boy dumps go in retro/vb/games. No BIOS needed.", false,
         ".vb", ".vboy", ".zip"),
      of("pce", "PC Engine", "mednafen_pce", "card", "HuCard", List.of(), 60,
         "HuCard dumps go in retro/pce/games. For CD games, put syscard3.pce in retro/pce/bios.", false,
         ".pce", ".sgx", ".cue", ".chd", ".zip"),
      of("neogeo", "Neo Geo AES", "fbneo", "cartridge", "cartridge", List.of("neogeo.zip"), 60,
         "Neo Geo ROM sets go in retro/neogeo/games as .zip. Put neogeo.zip (the BIOS set) in retro/neogeo/bios.", false,
         ".zip"),
      of("ngp", "Neo Geo Pocket Color", "mednafen_ngp", "card", "card", List.of(), 60,
         "Neo Geo Pocket dumps go in retro/ngp/games. No BIOS needed.", true,
         ".ngp", ".ngc", ".npc", ".zip"),
      of("wonderswan", "WonderSwan Color", "mednafen_wswan", "card", "card", List.of(), 60,
         "WonderSwan dumps go in retro/wonderswan/games. No BIOS needed.", true,
         ".ws", ".wsc", ".zip"),
      of("32x", "Sega 32X", "picodrive", "cartridge", "cartridge", List.of("32X_G_BIOS.BIN"), 60,
         "32X dumps go in retro/32x/games. Put 32X_G_BIOS.BIN, 32X_M_BIOS.BIN and 32X_S_BIOS.BIN in retro/32x/bios.", false,
         ".32x", ".bin", ".gen", ".md", ".zip"),
      of("segacd", "Sega CD", "genesis_plus_gx", "disc", "disc",
         List.of("bios_CD_U.bin", "bios_CD_E.bin", "bios_CD_J.bin"), 60,
         "Sega CD images go in retro/segacd/games. Put your region's BIOS (bios_CD_U.bin, bios_CD_E.bin or bios_CD_J.bin) in retro/segacd/bios.", false,
         ".cue", ".chd", ".iso", ".bin", ".m3u"),
      of("atari2600", "Atari 2600", "stella", "cartridge", "cartridge", List.of(), 60,
         "Atari 2600 dumps go in retro/atari2600/games. No BIOS needed.", false,
         ".a26", ".bin", ".zip"),
      of("atari5200", "Atari 5200", "a5200", "cartridge", "cartridge", List.of("5200.rom"), 60,
         "Atari 5200 dumps go in retro/atari5200/games. Put 5200.rom in retro/atari5200/bios.", false,
         ".a52", ".bin", ".zip"),
      of("atari7800", "Atari 7800", "prosystem", "cartridge", "cartridge", List.of(), 60,
         "Atari 7800 dumps go in retro/atari7800/games. No BIOS needed.", false,
         ".a78", ".bin", ".zip"),
      of("lynx", "Atari Lynx", "handy", "card", "card", List.of(), 60,
         "Atari Lynx dumps go in retro/lynx/games. No BIOS needed.", true,
         ".lnx", ".zip"),
      of("jaguar", "Atari Jaguar", "virtualjaguar", "cartridge", "cartridge", List.of(), 80,
         "Atari Jaguar dumps go in retro/jaguar/games. No BIOS needed.", false,
         ".j64", ".jag", ".rom", ".bin", ".zip"),
      of("intellivision", "Intellivision", "freeintv", "cartridge", "cartridge", List.of("exec.bin"), 60,
         "Intellivision dumps go in retro/intellivision/games. Put exec.bin and grom.bin in retro/intellivision/bios.", false,
         ".int", ".bin", ".rom", ".zip"),
      of("colecovision", "ColecoVision", "gearcoleco", "cartridge", "cartridge",
         List.of("colecovision.rom", "coleco.rom"), 60,
         "ColecoVision dumps go in retro/colecovision/games. Put colecovision.rom (or coleco.rom) in retro/colecovision/bios.", false,
         ".col", ".bin", ".rom", ".zip"),
      of("odyssey2", "Magnavox Odyssey 2", "o2em", "cartridge", "cartridge", List.of("o2rom.bin"), 60,
         "Odyssey 2 dumps go in retro/odyssey2/games. Put o2rom.bin in retro/odyssey2/bios.", false,
         ".bin", ".rom", ".zip"),
      of("channelf", "Fairchild Channel F", "freechaf", "cartridge", "cartridge", List.of("sl31253.bin"), 60,
         "Channel F dumps go in retro/channelf/games. Put sl31253.bin and sl31254.bin in retro/channelf/bios.", false,
         ".bin", ".rom", ".zip"),
      of("supervision", "Watara Supervision", "potator", "cartridge", "cartridge", List.of(), 60,
         "Supervision dumps go in retro/supervision/games. No BIOS needed.", true,
         ".sv", ".bin", ".zip"),
      of("megaduck", "Mega Duck", "sameduck", "cartridge", "cartridge", List.of(), 60,
         "Mega Duck dumps go in retro/megaduck/games. No BIOS needed.", true,
         ".bin", ".rom", ".zip"),
      of("pokemonmini", "Pokemon Mini", "pokemini", "card", "card", List.of(), 60,
         "Pokemon Mini dumps go in retro/pokemonmini/games. No BIOS needed.", true,
         ".min", ".zip"),
      of("gameandwatch", "Game & Watch", "gw", "card", "game card", List.of(), 60,
         "Game & Watch dumps go in retro/gameandwatch/games. No BIOS needed.", true,
         ".gw", ".mgw", ".bin", ".zip"),
      of("nds", "Nintendo DS", "melonds", "card", "game card", List.of("bios7.bin"), 80,
         "Nintendo DS dumps go in retro/nds/games. Put bios7.bin, bios9.bin and firmware.bin in retro/nds/bios.", true,
         ".nds", ".dsi", ".zip"),
      new Spec(
         "vectrex", "Vectrex", "vecx", "cartridge", "cartridge",
         List.of(".vec", ".bin", ".zip"), List.of(), 60,
         "Vectrex dumps go in retro/vectrex/games. No BIOS needed; the Vectrex has its own screen, so no TV is required.",
         false, true, 3.0, 3.18, 6.0, 4.2
      )
   );
}
