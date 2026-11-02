package com.dustin.wcore.client;

import com.dustin.wcore.ConsoleSettings;
import com.dustin.wcore.emu.CoreOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CoreOptionsScreen extends Screen {
   private static final int ROW = 22;
   private static final int TOP = 58;
   private final CoreOptionsScreen.Target t;
   private final Screen parent;
   private final ConsoleSettings s = ConsoleSettings.get();
   private final List<CoreOptions.Category> tabs = new ArrayList<>();
   private int tab;
   private int page;
   private int pages = 1;

   public CoreOptionsScreen(Screen parent) {
      this(parent, CoreOptionsScreen.Target.ps2());
   }

   public CoreOptionsScreen(Screen parent, CoreOptionsScreen.Target target) {
      super(Component.literal(target.title()));
      this.parent = parent;
      this.t = target;

      for (CoreOptions.Category c : CoreOptions.load(target.coreName()).categories) {
         if (!target.hiddenCategories().contains(c.key())) {
            List<CoreOptions.Option> shown = c.options()
               .stream()
               .filter(o -> !target.hidden().contains(o.key()))
               .filter(o -> !c.key().equals("input") || !o.key().endsWith("2"))
               .toList();
            if (!shown.isEmpty()) {
               this.tabs.add(new CoreOptions.Category(c.key(), c.label(), c.info(), shown));
            }
         }
      }
   }

   private static String pretty(String label) {
      return switch (label) {
         case "enabled", "Enabled" -> "On";
         case "disabled", "Disabled" -> "Off";
         default -> label;
      };
   }

   private static String name(CoreOptions.Option o) {
      return o.label().replace(" (Restart)", " *");
   }

   private String defaultOf(CoreOptions.Option o) {
      String rec = this.t.recommended().get(o.key());
      return rec != null && o.values().contains(rec) ? rec : o.def();
   }

   protected void init() {
      if (this.tabs.isEmpty()) {
         this.addRenderableWidget(
            Button.builder(Component.literal("Done"), b -> this.onClose()).bounds(this.width / 2 - 100, this.height - 28, 200, 20).build()
         );
      } else {
         this.tab = Math.min(this.tab, this.tabs.size() - 1);
         int tw = Math.min(110, (this.width - 20) / this.tabs.size() - 4);
         int tx = this.width / 2 - (this.tabs.size() * (tw + 4) - 4) / 2;

         for (int i = 0; i < this.tabs.size(); i++) {
            int idx = i;
            Button b = Button.builder(Component.literal(this.tabs.get(i).label()), xx -> {
               this.tab = idx;
               this.page = 0;
               this.rebuildWidgets();
            }).bounds(tx + i * (tw + 4), 30, tw, 20).build();
            b.active = i != this.tab;
            this.addRenderableWidget(b);
         }

         List<CoreOptions.Option> opts = this.tabs.get(this.tab).options();
         int rows = Math.max(1, (this.height - 58 - 40) / 22);
         int perPage = rows * 2;
         this.pages = Math.max(1, (opts.size() + perPage - 1) / perPage);
         this.page = Math.min(this.page, this.pages - 1);
         int w = Math.min(220, this.width / 2 - 15);
         int x1 = this.width / 2 - w - 5;
         int x2 = this.width / 2 + 5;

         for (int i = 0; i < perPage; i++) {
            int n = this.page * perPage + i;
            if (n >= opts.size()) {
               break;
            }

            CoreOptions.Option o = opts.get(n);
            int x = i < rows ? x1 : x2;
            int y = 58 + i % rows * 22;
            this.addRenderableWidget(this.button(o, x, y, w));
         }

         int by = this.height - 28;
         Button prev = Button.builder(Component.literal("<"), b -> {
            this.page--;
            this.rebuildWidgets();
         }).bounds(this.width / 2 - 210, by, 20, 20).build();
         Button next = Button.builder(Component.literal(">"), b -> {
            this.page++;
            this.rebuildWidgets();
         }).bounds(this.width / 2 + 190, by, 20, 20).build();
         prev.active = this.page > 0;
         next.active = this.page < this.pages - 1;
         this.addRenderableWidget(prev);
         this.addRenderableWidget(next);
         this.addRenderableWidget(Button.builder(Component.literal("Reset " + this.tabs.get(this.tab).label()), b -> {
            for (CoreOptions.Option o : opts) {
               this.t.overrides().remove(o.key());
            }

            this.rebuildWidgets();
         }).bounds(this.width / 2 - 185, by, 120, 20).build());
         this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose()).bounds(this.width / 2 - 60, by, 120, 20).build());
         this.addRenderableWidget(Button.builder(Component.literal("Reset all"), b -> {
            this.t.overrides().clear();
            this.rebuildWidgets();
         }).bounds(this.width / 2 + 65, by, 120, 20).build());
      }
   }

   private CycleButton<String> button(CoreOptions.Option o, int x, int y, int w) {
      boolean locked = this.t.locked().contains(o.key());
      String def = this.defaultOf(o);
      String cur = this.t.value(o.key(), o.def());
      if (!o.values().contains(cur)) {
         cur = def;
      }

      if (o.key().equals("pcsx2_renderer")) {
         cur = o.values().contains("Software (SW)") ? "Software (SW)" : cur;
      }

      CycleButton<String> b = CycleButton.builder(v -> Component.literal(pretty(o.labelOf(v))), cur)
         .withValues(o.values().isEmpty() ? List.of(cur) : o.values())
         .create(x, y, w, 20, Component.literal(name(o)), (btn, v) -> {
            if (v.equals(def)) {
               this.t.overrides().remove(o.key());
            } else {
               this.t.overrides().put(o.key(), v);
            }
         });
      String tip = o.info().isBlank() ? name(o) : o.info();
      if (locked) {
         tip = (o.key().equals("pcsx2_bios") ? "Set automatically from your ps2/bios folder." : "Fixed: the TV picture needs the software renderer.")
            + "\n\n"
            + tip;
      } else {
         tip = tip + "\n\nDefault: " + pretty(o.labelOf(def));
      }

      b.setTooltip(Tooltip.create(Component.literal(tip)));
      b.active = !locked;
      return b;
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(g, mouseX, mouseY, partialTick);
      g.centeredText(this.font, this.title, this.width / 2, 10, -1);
      if (this.tabs.isEmpty()) {
         g.centeredText(this.font, "No settings list for this emulator yet - boot a game on it once.", this.width / 2, this.height / 2, -32640);
      } else {
         g.centeredText(
            this.font,
            "Applies right away   |   * = restart the console   |   page " + (this.page + 1) + "/" + this.pages + "   |   hover a setting for details",
            this.width / 2,
            46 + (this.height - 58 - 40) / 22 * 22 + 14,
            -5592406
         );
      }
   }

   public void onClose() {
      this.s.save();
      this.minecraft.setScreenAndShow(this.parent);
   }

   public record Target(
      String title,
      String coreName,
      Map<String, String> overrides,
      Set<String> hidden,
      Set<String> hiddenCategories,
      Set<String> locked,
      Map<String, String> recommended
   ) {
      public static CoreOptionsScreen.Target ps2() {
         ConsoleSettings s = ConsoleSettings.get();
         return new CoreOptionsScreen.Target(
            "PS2 emulator settings",
            "pcsx2_libretro",
            s.ps2,
            ConsoleSettings.PS2_HIDDEN,
            ConsoleSettings.PS2_HIDDEN_CATEGORIES,
            ConsoleSettings.PS2_LOCKED,
            ConsoleSettings.ps2Recommended()
         );
      }

       public static CoreOptionsScreen.Target gamecube() {
          ConsoleSettings s = ConsoleSettings.get();
          return new CoreOptionsScreen.Target(
             "GameCube emulator settings",
             "dolphin_libretro",
             s.gamecube,
             Set.of(),
             Set.of(),
             ConsoleSettings.GAMECUBE_LOCKED,
             ConsoleSettings.gamecubeRecommended()
          );
       }

      public static CoreOptionsScreen.Target of(String title, String core, Map<String, String> recommended) {
         return new CoreOptionsScreen.Target(title, core + "_libretro", ConsoleSettings.get().coreOverrides(core), Set.of(), Set.of(), Set.of(), recommended);
      }

      String value(String key, String coreDefault) {
         String v = this.overrides.get(key);
         if (v == null) {
            v = this.recommended.get(key);
         }

         return v != null ? v : coreDefault;
      }
   }
}
