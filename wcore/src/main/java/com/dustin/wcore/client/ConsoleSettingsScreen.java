package com.dustin.wcore.client;

import com.dustin.wcore.ConsoleSettings;
import com.dustin.wcore.emu.BuildInfo;
import com.dustin.wcore.retro.RetroSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConsoleSettingsScreen extends Screen {
   private final Screen parent;
   private final ConsoleSettings s = ConsoleSettings.get();
   private final List<ConsoleSettingsScreen.Line> status = new ArrayList<>();

   public ConsoleSettingsScreen(Screen parent) {
      super(Component.literal("Working Consoles - settings"));
      this.parent = parent;
      this.scan();
   }

   private void scan() {
      Path game = FabricLoader.getInstance().getGameDir();
      this.status.add(new ConsoleSettingsScreen.Line("Core " + version("wcore") + "   build " + BuildInfo.STAMP, -5592406));
      if (FabricLoader.getInstance().isModLoaded("gbsp")) {
         int roms = count(game.resolve("gameboy/roms"), ".gba", ".gb", ".gbc");
         boolean bios = hasSized(game.resolve("gameboy/bios"), 256L, 16384L);
         this.status.add(line("Gameboy in Minecraft " + version("gbsp"), bios, roms, "gameboy/bios", "gameboy/roms"));
      } else {
         this.status.add(new ConsoleSettingsScreen.Line("Gameboy in Minecraft: not installed", -8947849));
      }

      if (FabricLoader.getInstance().isModLoaded("ps2")) {
         int discs = count(game.resolve("ps2/isos"), ".iso", ".chd", ".cso", ".zso", ".ciso", ".cue", ".gz", ".elf", ".m3u");
         boolean bios = hasSized(game.resolve("ps2/bios"), 4194304L, 8388608L);
         boolean engine = count(game.resolve("consoles/cores"), "pcsx2_libretro.dll", "pcsx2_libretro.so") > 0;
         ConsoleSettingsScreen.Line l = line("PS2 in Minecraft " + version("ps2"), bios, discs, "ps2/bios", "ps2/isos");
         this.status.add(engine ? l : new ConsoleSettingsScreen.Line(l.text() + ", emulator downloads on first use", l.color()));
      } else {
         this.status.add(new ConsoleSettingsScreen.Line("PS2 in Minecraft: not installed", -8947849));
      }

      // One summary line: with 60+ consoles a line each pushed every button off the screen (scissor crash).
      int ready = 0;
      int noBios = 0;
      int noGames = 0;
      for (RetroSystem sys : RetroSystem.all()) {
         if (sys.needsBios() && sys.bios() == null) {
            noBios++;
         } else if (sys.games().isEmpty()) {
            noGames++;
         } else {
            ready++;
         }
      }
      this.status.add(new ConsoleSettingsScreen.Line(
         RetroSystem.all().size() + " more consoles: " + ready + " ready, " + noGames + " with no games yet, " + noBios + " missing a BIOS",
         noBios > 0 ? -32640 : -8323200));
   }

   private static ConsoleSettingsScreen.Line line(String name, boolean bios, int games, String biosDir, String gameDir) {
      return !bios
         ? new ConsoleSettingsScreen.Line(name + ": no BIOS in " + biosDir + " (won't boot)", -32640)
         : new ConsoleSettingsScreen.Line(name + ": BIOS found, " + games + " game" + (games == 1 ? "" : "s") + " in " + gameDir, games > 0 ? -8323200 : -12192);
   }

   private static String version(String id) {
      return FabricLoader.getInstance().getModContainer(id).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
   }

   private static boolean hasSized(Path dir, long min, long max) {
      if (!Files.isDirectory(dir)) {
         return false;
      }

      try (Stream<Path> files = Files.list(dir)) {
         return files.anyMatch(p -> {
            try {
               long n = Files.size(p);
               return Files.isRegularFile(p) && n >= min && n <= max;
            } catch (Exception e) {
               return false;
            }
         });
      } catch (Exception e) {
         return false;
      }
   }

   private static int count(Path dir, String... endings) {
      if (!Files.isDirectory(dir)) {
         return 0;
      }

      try (Stream<Path> files = Files.list(dir)) {
         return (int)files.filter(p -> {
            String n = p.getFileName().toString().toLowerCase(Locale.ROOT);

            for (String ex : endings) {
               if (n.endsWith(ex)) {
                  return true;
               }
            }

            return false;
         }).count();
      } catch (Exception e) {
         return 0;
      }
   }

   private int top() {
      return 28 + this.status.size() * 11 + 8;
   }

   protected void init() {
      int w = 200;
      int x1 = this.width / 2 - w - 5;
      int x2 = this.width / 2 + 5;
      int y = this.top();
      int step = 24;
      this.addRenderableWidget(
         CycleButton.builder(v -> Component.literal(v + "%"), this.s.volume)
            .withValues(List.of(0, 5, 10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 90, 100))
            .create(x1, y, w, 20, Component.literal("Console volume"), (bx, v) -> this.s.volume = v)
      );
      this.addRenderableWidget(
         CycleButton.builder(v -> Component.literal(v + " blocks"), this.s.hearingDistance)
            .withValues(List.of(8, 16, 24, 32, 48, 64))
            .create(x1, y + step, w, 20, Component.literal("TV heard from"), (bx, v) -> this.s.hearingDistance = v)
      );
      this.addRenderableWidget(
         CycleButton.booleanBuilder(Component.literal("Glowing"), Component.literal("Room light"), this.s.tvGlow)
            .create(x1, y + step * 2, w, 20, Component.literal("TV picture"), (bx, v) -> this.s.tvGlow = v)
      );
      CycleButton<Boolean> power = CycleButton.onOffBuilder(this.s.redstonePower)
         .create(x1, y + step * 3, w, 20, Component.literal("Redstone power"), (bx, v) -> this.s.redstonePower = v);
      power.setTooltip(
         Tooltip.create(
            Component.literal(
               "The signal at a console's wall outlet decides how well it runs:\nSignal 1-7: low voltage, renderer on one thread\nSignal 8-14: normal, half your render threads\nSignal 15: clean, your full settings\nSignal 15 steady 60 s: overclock, +2 threads, CPU 130%\nFlickering: unstable; 3 flickers in a minute = brownout reset\n\nPlace a Power Monitor next to a console to watch it.\nOff = always your own settings, no brownouts."
            )
         )
      );
      this.addRenderableWidget(power);
      CycleButton<Boolean> world = CycleButton.booleanBuilder(Component.literal("In the world (3D)"), Component.literal("Direct"), this.s.soundInWorld)
         .create(x1, y + step * 4, w, 20, Component.literal("Console sound"), (bx, v) -> this.s.soundInWorld = v);
      world.setTooltip(
         Tooltip.create(
            Component.literal(
               "In the world: the game's sound comes from the TV like any Minecraft sound - it fades with distance, follows the Jukebox/Note Blocks volume slider, and echoes/muffles with Sound Physics Remastered or Dynamic Surroundings.\nDirect: straight to your speakers. Applies to the next game you start."
            )
         )
      );
      this.addRenderableWidget(world);
      CycleButton<Boolean> mobs = CycleButton.onOffBuilder(this.s.mobsHear)
         .create(x1, y + step * 5, w, 20, Component.literal("Monsters hear consoles"), (bx, v) -> this.s.mobsHear = v);
      mobs.setTooltip(
         Tooltip.create(
            Component.literal(
               "A running console is heard by monsters (and sculk). Louder volume carries further. Every solid block in the way muffles it; wool soundproofs. Off = play in peace."
            )
         )
      );
      this.addRenderableWidget(mobs);
      CycleButton<String> cables = CycleButton.builder(v -> {
         return Component.literal(switch (v) {
            case "sag" -> "Hanging";
            case "hidden" -> "Hidden";
            default -> "Tidy (along the floor)";
         });
      }, this.s.cables).withValues(List.of("tidy", "sag", "hidden")).create(x1, y + step * 6, w, 20, Component.literal("Cables"), (bx, v) -> this.s.cables = v);
      cables.setTooltip(
         Tooltip.create(
            Component.literal(
               "How cables are drawn. Any cable whose path goes through a wall or floor block counts as run inside the wall and isn't drawn - put the outlet behind the wall to hide it."
            )
         )
      );
      this.addRenderableWidget(cables);
      CycleButton<Boolean> share = CycleButton.onOffBuilder(this.s.shareScreen)
         .create(x1, y + step * 7, w, 20, Component.literal("Share picture in multiplayer"), (bx, v) -> this.s.shareScreen = v);
      share.setTooltip(
         Tooltip.create(
            Component.literal(
               "Multiplayer couch co-op like the old days: only one player needs the BIOS and the game. Their PC runs the console and sends the picture and sound to everyone nearby; everyone's controller still works."
            )
         )
      );
      this.addRenderableWidget(share);
      this.addRenderableWidget(
         CycleButton.builder(v -> Component.literal(v + "p"), this.s.shareQuality)
            .withValues(List.of(240, 360, 480))
            .create(x1, y + step * 8, w, 20, Component.literal("Shared picture quality"), (bx, v) -> this.s.shareQuality = v)
      );
      CycleButton<String> color = CycleButton.builder(v -> {
            return Component.literal(switch (v) {
               case "gba" -> "Original GBA";
               case "sp" -> "GBA SP backlight";
               default -> "Off (raw colours)";
            });
         }, this.s.handheldColor)
         .withValues(List.of("off", "gba", "sp"))
         .create(x2, y, w, 20, Component.literal("Handheld colours"), (bx, v) -> this.s.handheldColor = v);
      color.setTooltip(
         Tooltip.create(Component.literal("Colour correction like the real screen: games were made for a dim, washed-out LCD, so raw colours look too strong."))
      );
      this.addRenderableWidget(color);
      CycleButton<Boolean> blend = CycleButton.onOffBuilder(this.s.handheldBlend)
         .create(x2, y + step, w, 20, Component.literal("Handheld frame blending"), (bx, v) -> this.s.handheldBlend = v);
      blend.setTooltip(
         Tooltip.create(Component.literal("Mixes each frame with the last, like the slow real LCD. Fixes games that flicker sprites to fake transparency."))
      );
      this.addRenderableWidget(blend);
      CycleButton<Integer> hsize = CycleButton.builder(v -> Component.literal(v + "%"), this.s.handheldSize)
         .withValues(List.of(100, 130, 150, 170, 190, 210, 240))
         .create(x2, y + step * 2, w, 20, Component.literal("Handheld size in hand"), (bx, v) -> this.s.handheldSize = v);
      hsize.setTooltip(Tooltip.create(Component.literal("How big the handheld looks when you hold it. Changes right away.")));
      this.addRenderableWidget(hsize);
      Button ps2 = Button.builder(Component.literal("PS2 emulator settings..."), bx -> this.minecraft.setScreenAndShow(new CoreOptionsScreen(this)))
         .bounds(x2, y + step * 3, w, 20)
         .build();
      ps2.setTooltip(Tooltip.create(Component.literal("Every setting of the PS2 emulator core: System, Video, Emulation and Input.")));
      this.addRenderableWidget(ps2);
       Button gc = Button.builder(Component.literal("GameCube emulator settings..."), bx -> this.minecraft.setScreenAndShow(new CoreOptionsScreen(this, CoreOptionsScreen.Target.gamecube())))
          .bounds(x2, y + step * 4, w, 20)
          .build();
       gc.setTooltip(Tooltip.create(Component.literal("Every setting of the GameCube (Dolphin) emulator core: System, Video, Emulation and Input. Boot a game first to populate the list.")));
       this.addRenderableWidget(gc);
       Button all = Button.builder(Component.literal("All consoles: status & settings..."), bx -> this.minecraft.setScreenAndShow(new SystemsScreen(this)))
          .bounds(x2, y + step * 5, w, 20)
          .build();
      all.setTooltip(Tooltip.create(Component.literal("Every console in the pack: games found, BIOS check, and each emulator core's settings.")));
      this.addRenderableWidget(all);

      this.addRenderableWidget(Button.builder(Component.literal("Done"), bx -> this.onClose()).bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(g, mouseX, mouseY, partialTick);
      g.centeredText(this.font, this.title, this.width / 2, 12, -1);

      for (int i = 0; i < this.status.size(); i++) {
         g.centeredText(this.font, this.status.get(i).text(), this.width / 2, 28 + i * 11, this.status.get(i).color());
      }
   }

   public void onClose() {
      this.s.save();
      this.minecraft.setScreenAndShow(this.parent);
   }

   private record Line(String text, int color) {
   }
}
