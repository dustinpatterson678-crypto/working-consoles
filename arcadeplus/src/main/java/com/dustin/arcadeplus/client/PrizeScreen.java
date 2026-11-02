package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.Prizes;
import com.dustin.arcadeplus.TicketBank;
import com.dustin.arcadeplus.net.PrizeBuyPacket;
import com.dustin.arcadeplus.net.PrizeResultPacket;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Prize booth: your ticket card balance and the prize shelves; click a prize to redeem it. */
final class PrizeScreen extends Screen {
   private static final int CELL = 52;
   private int scroll;
   private final BlockPos pos;
   private String status = "";
   private int mx = -1, my = -1;                    // last mouse position in GUI coordinates (from rendering)
   private int statusColor = 0xFFFFFFFF;

   PrizeScreen(BlockPos pos) {
      super(Component.literal("Prize Booth"));
      this.pos = pos;
      PrizeResultPacket.CLIENT = msg -> {
         boolean bad = msg.startsWith("!");
         this.status = bad ? msg.substring(1) : msg;
         this.statusColor = bad ? 0xFFFF6060 : 0xFF60FF60;
      };
   }

   @Override
   protected void init() {
      super.init();
      int cols = this.cols();
      List<Prizes.Prize> l = Prizes.client;
      for (int i = this.scroll * cols; i < Math.min(l.size(), (this.scroll + this.visibleRows()) * cols); i++) {
         final int idx = i;
         int x = this.left() + (i % cols) * CELL, y = this.top() + (i / cols - this.scroll) * CELL;
         this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.empty(), b -> this.buy(idx)).bounds(x + 2, y + 2, CELL - 4, CELL - 4).build());
      }
   }

   private void buy(int i) {
      com.dustin.arcadeplus.ArcadePlusMod.LOGGER.info("Prize booth: asking for prize #{}", i);
      this.status = "Redeeming...";
      this.statusColor = 0xFFFFFFFF;
      ClientPlayNetworking.send(new PrizeBuyPacket(this.pos, i));
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void removed() {
      PrizeResultPacket.CLIENT = m -> {};
      super.removed();
   }

   private int cols() {
      return Math.max(3, Math.min(10, (this.width - 20) / CELL));
   }

   private int visibleRows() {
      return Math.max(1, (this.height - 44 - 50) / CELL);
   }

   private int left() {
      return this.width / 2 - this.cols() * CELL / 2;
   }

   @Override
   public boolean mouseScrolled(double mx, double my, double sx, double sy) {
      int rows = (Prizes.client.size() + this.cols() - 1) / this.cols();
      int before = this.scroll;
      this.scroll = Math.max(0, Math.min(Math.max(0, rows - this.visibleRows()), this.scroll - (int)Math.signum(sy)));
      if (before != this.scroll) this.rebuildWidgets();
      return true;
   }

   private int top() {
      return 44;
   }

   private int hovered(double mx, double my) {
      int c = (int)Math.floor((mx - this.left()) / CELL), r = (int)Math.floor((my - this.top()) / CELL);
      if (c < 0 || c >= this.cols() || r < 0 || r >= this.visibleRows()) return -1;
      int i = (r + this.scroll) * this.cols() + c;
      return i < Prizes.client.size() ? i : -1;
   }

   private static String fmt(int n) {
      return String.format("%,d", n);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(g, mouseX, mouseY, partialTick);
      var pl = this.minecraft.player;
      int card = pl == null ? 0 : TicketBank.balance(pl), loose = pl == null ? 0 : Prizes.countTickets(pl), total = card + loose;
      boolean creative = pl != null && pl.isCreative();
      g.centeredText(this.font, "PRIZE BOOTH", this.width / 2, 10, 0xFFFFCC33);
      g.centeredText(this.font, "Ticket card: " + fmt(card) + (loose > 0 ? "   (+" + fmt(loose) + " paper tickets, munched when you redeem)" : "")
         + (creative ? "   creative: free" : ""), this.width / 2, 22, 0xFFFFFFFF);
      this.mx = mouseX;
      this.my = mouseY;
      int hover = this.hovered(mouseX, mouseY);
      List<Prizes.Prize> l = Prizes.client;
      int cols = this.cols();
      for (int i = this.scroll * cols; i < Math.min(l.size(), (this.scroll + this.visibleRows()) * cols); i++) {
         Prizes.Prize p = l.get(i);
         int x = this.left() + (i % cols) * CELL, y = this.top() + (i / cols - this.scroll) * CELL;
         boolean afford = creative || total >= p.cost();
         int bg = p.cost() >= 25000 ? 0xA0402010 : p.cost() >= 1000 ? 0xA0202848 : 0xA0202030;
         g.fill(x + 4, y + 26, x + CELL - 4, y + 40, i == hover ? 0xC0604080 : bg);
         ItemStack s = p.stack();
         g.item(s, x + CELL / 2 - 8, y + 8);
         String price = p.cost() >= 10000 ? (p.cost() / 1000) + "K" : fmt(p.cost());
         g.centeredText(this.font, price, x + CELL / 2, y + 30, afford ? 0xFF60FF60 : 0xFFFF6060);
         if (!afford && p.cost() > total) {
            int w = CELL - 10, filled = (int)(w * Math.min(1.0, total / (double)p.cost()));
            g.fill(x + 5, y + 42, x + 5 + w, y + 45, 0xFF303030);
            g.fill(x + 5, y + 42, x + 5 + filled, y + 45, 0xFFFFCC33);
         }
         if (i == hover && !s.isEmpty()) {
            g.setTooltipForNextFrame(this.font, s, mouseX, mouseY);
         }
      }
      if (hover >= 0) {
         Prizes.Prize p = l.get(hover);
         int need = p.cost() - total;
         g.centeredText(this.font, need > 0 && !creative ? fmt(need) + " more tickets to go" : "Click to redeem", this.width / 2, this.height - 42, 0xFFFFCC33);
      }
      g.centeredText(this.font, this.status, this.width / 2, this.height - 30, this.statusColor);
      g.centeredText(this.font, "Click a prize to redeem it   Scroll for more   Esc: leave", this.width / 2, this.height - 16, 0xC0FFFFFF);
   }

}
