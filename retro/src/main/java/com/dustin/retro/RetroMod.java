package com.dustin.retro;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.item.ManualItem;
import com.dustin.wcore.retro.RetroMediaItem;
import com.dustin.wcore.retro.RetroSystem.Game;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Core Pack I, batch 2: the remaining cartridge / 2D consoles. */
public class RetroMod implements ModInitializer {
   public static final String MOD_ID = "retro";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
   public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main"));

   @Override
   public void onInitialize() {
      RetroConsoles.register();

      ManualItem.register(MOD_ID, "Retro Consoles Manual", new String[]{
         "RETRO CONSOLES\n\nVirtual Boy, PC Engine, Neo Geo AES, Neo Geo Pocket Color, WonderSwan Color, Sega 32X, Sega CD, Atari 2600 / 5200 / 7800, Atari Lynx, Atari Jaguar, Intellivision, ColecoVision, Magnavox Odyssey 2, Vectrex, Fairchild Channel F, Watara Supervision, Mega Duck, Pokemon Mini, Game & Watch and Nintendo DS.\n\nEvery one has its own model and its own games folder.",
         "SETTING UP\n\n1. Cable the console to a TV and to a Wall Outlet.\n2. Power the outlet's block with redstone.\n3. Put your own dumps in the console's games folder under retro/ in your Minecraft folder.\n\nThe Vectrex has its own built-in screen, so it needs no TV.",
         "PLAYING\n\nCraft the console's blank media (cartridge, card or disc), right-click it and pick a game, then click it into the matching console. It boots on the TV.\n\nSneak + right-click the console to eject. Right-click with an empty hand to power it on or off.",
         "BIOS / FIRMWARE\n\nNintendo DS: bios7.bin, bios9.bin, firmware.bin\nNeo Geo: neogeo.zip\nSega CD: bios_CD_U.bin, bios_CD_E.bin or bios_CD_J.bin\nSega 32X: 32X_G_BIOS.BIN, 32X_M_BIOS.BIN, 32X_S_BIOS.BIN\nAtari 5200: 5200.rom\nIntellivision: exec.bin, grom.bin\nColecoVision: colecovision.rom or coleco.rom\nOdyssey 2: o2rom.bin\nChannel F: sl31253.bin, sl31254.bin\nPC Engine CD: syscard3.pce (optional)\n\nPut them in that console's bios/ folder. BIOS files are copied into the emulator automatically.",
         "FOLDERS\n\nretro/vb/games, retro/pce/games, retro/neogeo/games, retro/ngp/games, retro/wonderswan/games, retro/32x/games, retro/segacd/games, retro/atari2600/games, retro/atari5200/games, retro/atari7800/games, retro/lynx/games, retro/jaguar/games, retro/intellivision/games, retro/colecovision/games, retro/odyssey2/games, retro/vectrex/games, retro/channelf/games, retro/supervision/games, retro/megaduck/games, retro/pokemonmini/games, retro/gameandwatch/games, retro/nds/games."
      });

      Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricCreativeModeTab.builder()
         .title(Component.translatable("itemGroup.retro.main"))
         .icon(() -> new ItemStack(RetroConsoles.ALL.get(0).item))
         .displayItems((context, entries) -> {
            for (RetroConsoles.Console c : RetroConsoles.ALL) {
               entries.accept(c.item);
               entries.accept(new ItemStack(c.media));
               for (Game g : c.system.games()) {
                  entries.accept(RetroMediaItem.of(c.system, g.file().getFileName().toString()));
               }
            }
            entries.accept(ManualItem.of(WorkingConsolesCore.MANUAL, MOD_ID));
         })
         .build());

      LOGGER.info("Retro Consoles loaded: {} extra consoles", RetroConsoles.ALL.size());
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }
}
