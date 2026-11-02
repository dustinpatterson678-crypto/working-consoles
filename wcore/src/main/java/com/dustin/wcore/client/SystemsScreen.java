package com.dustin.wcore.client;

import com.dustin.wcore.retro.RetroSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Every console in the pack, a page at a time: games found, BIOS check, and a button for its emulator settings. */
public class SystemsScreen extends Screen {
   private final Screen parent;
   private final List<RetroSystem> systems = new ArrayList<>(RetroSystem.all());
   private final List<String> lines = new ArrayList<>();
   private final List<Integer> colors = new ArrayList<>();
   private int page;
   private int perPage = 8;

   public SystemsScreen(Screen parent) {
      super(Component.literal("Working Consoles - all consoles"));
      this.parent = parent;
      for (RetroSystem sys : this.systems) {
         int games = sys.games().size();
         if (sys.needsBios() && sys.bios() == null) {
            this.lines.add(sys.name + ": no BIOS in " + sys.folder + "/bios");
            this.colors.add(-32640);
         } else {
            this.lines.add(sys.name + ": " + games + " game" + (games == 1 ? "" : "s"));
            this.colors.add(games > 0 ? -8323200 : -12192);
         }
      }
   }

   private int pages() {
      return Math.max(1, (this.systems.size() + this.perPage - 1) / this.perPage);
   }

   @Override
   protected void init() {
      // fit as many rows as the window allows (always at least 3)
      this.perPage = Math.max(3, Math.min(12, (this.height - 90) / 24));
      this.page = Math.min(this.page, this.pages() - 1);
      int top = 34;
      int from = this.page * this.perPage;
      for (int i = 0; i < this.perPage && from + i < this.systems.size(); i++) {
         RetroSystem sys = this.systems.get(from + i);
         Button b = Button.builder(Component.literal("Settings"), x -> this.minecraft.setScreenAndShow(
               new CoreOptionsScreen(this, CoreOptionsScreen.Target.of(sys.name + " emulator settings", sys.core, sys.options))))
            .bounds(this.width / 2 + 110, top + i * 24, 80, 20)
            .build();
         b.setTooltip(Tooltip.create(Component.literal("Every setting of the " + sys.name + " emulator core (" + sys.core + "). The list appears after a game has run on it once.")));
         this.addRenderableWidget(b);
      }
      int y = this.height - 28;
      Button prev = Button.builder(Component.literal("< Prev"), x -> {
         this.page = Math.max(0, this.page - 1);
         this.rebuildWidgets();
      }).bounds(this.width / 2 - 190, y, 80, 20).build();
      Button next = Button.builder(Component.literal("Next >"), x -> {
         this.page = Math.min(this.pages() - 1, this.page + 1);
         this.rebuildWidgets();
      }).bounds(this.width / 2 + 110, y, 80, 20).build();
      prev.active = this.page > 0;
      next.active = this.page < this.pages() - 1;
      this.addRenderableWidget(prev);
      this.addRenderableWidget(next);
      this.addRenderableWidget(Button.builder(Component.literal("Done"), x -> this.onClose()).bounds(this.width / 2 - 100, y, 200, 20).build());
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(g, mouseX, mouseY, partialTick);
      g.centeredText(this.font, this.title.getString() + "   (page " + (this.page + 1) + " / " + this.pages() + ")", this.width / 2, 12, -1);
      int top = 34;
      int from = this.page * this.perPage;
      for (int i = 0; i < this.perPage && from + i < this.systems.size(); i++) {
         g.text(this.font, this.lines.get(from + i), this.width / 2 - 190, top + i * 24 + 6, this.colors.get(from + i), true);
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreenAndShow(this.parent);
   }
}
