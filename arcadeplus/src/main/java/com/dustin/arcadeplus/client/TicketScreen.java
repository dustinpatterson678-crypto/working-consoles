package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.TicketGame;
import com.dustin.arcadeplus.TicketGameBlockEntity;
import com.dustin.arcadeplus.net.TicketInputPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Hands-on play for Basketball (aim + power throw) and the Claw Machine (steer + drop). The world stays visible. */
final class TicketScreen extends Screen {
   private final BlockPos pos;
   private final TicketGame game;
   private int keys, sent = -1, age;
   private double aim, mouseAim;
   private boolean charging;
   private float chargeStart;
   private int idleTicks;

   TicketScreen(BlockPos pos, TicketGame game) {
      super(Component.literal(game.title));
      this.pos = pos;
      this.game = game;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
   }

   private TicketGameBlockEntity be() {
      return this.minecraft.level != null && this.minecraft.level.getBlockEntity(this.pos) instanceof TicketGameBlockEntity be ? be : null;
   }

   private float now(float partial) {
      return this.age + partial;
   }

   /** Power meter: fills up in 0.75 s, then drops back, so holding too long is as bad as too short. */
   private double power(float partial) {
      if (!this.charging) return 0;
      double t = (this.now(partial) - this.chargeStart) / 15.0;
      double m = t % 2.0;
      return m <= 1 ? m : 2 - m;
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(g, mouseX, mouseY, partialTick);
      TicketGameBlockEntity be = this.be();
      int w = this.width, h = this.height;
      if (this.game == TicketGame.BASKETBALL || this.game == TicketGame.LANES) {
         this.mouseAim = Math.max(-1, Math.min(1, (w / 2.0 - mouseX) / (w / 2.0)));
         this.aim = this.mouseAim;
         String status = be == null ? "" : !be.playing ? "Insert a nickel to play  (Esc to leave)"
            : this.game == TicketGame.LANES ? "PINS " + be.score + "    BALL " + Math.min(5, be.shots + 1) + " / 5" : "BASKETS " + be.score + "    SHOTS " + be.shots;
         g.centeredText(this.font, status, w / 2, 10, 0xFFFFE070);
         // aim bar: where the ball will head across the cage (left/right as you see it)
         int bx = w / 2 - 100, by = h - 40;
         g.fill(bx, by, bx + 200, by + 6, 0x80000000);
         int mx = (int)(w / 2 - this.aim * 100);
         g.fill(mx - 2, by - 4, mx + 2, by + 10, 0xFFFF8A20);
         g.fill(w / 2 - 1, by, w / 2 + 1, by + 6, 0x80FFFFFF);
         // power meter on the right
         double pw = this.power(partialTick);
         int px = w - 30, py = h / 2 - 60;
         g.fill(px, py, px + 12, py + 120, 0x80000000);
         int fillH = (int)(pw * 120);
         int col = pw > 0.85 ? 0xFFFF3030 : pw > 0.5 ? 0xFF40FF70 : 0xFFFFD040;
         g.fill(px, py + 120 - fillH, px + 12, py + 120, col);
         g.text(this.font, "Aim: move the mouse    Throw: hold left click or Space, let go    Esc: leave", 6, h - 12, 0xC0FFFFFF, true);
      } else if (this.game == TicketGame.POOL) {
         var mcp = this.minecraft.player;
         var bs = this.minecraft.level.getBlockState(this.pos);
         if (mcp != null && bs.getBlock() instanceof com.dustin.arcadeplus.TicketGameBlock) {
            net.minecraft.core.Direction front = bs.getValue(com.dustin.arcadeplus.TicketGameBlock.FACING), side = front.getClockWise(), back = front.getOpposite();
            var look = mcp.getLookAngle();
            double du = look.x * side.getStepX() + look.z * side.getStepZ(), dv = look.x * back.getStepX() + look.z * back.getStepZ();
            this.aim = Math.atan2(dv, du) - (mouseX - w / 2.0) / (w / 2.0) * Math.PI * 0.6;
         }
         PoolAim.pos = this.pos;
         PoolAim.angle = this.aim;
         PoolAim.power = this.power(partialTick);
         int left = be == null ? 0 : Integer.bitCount(~be.moleMask & 0xFFFE);
         g.centeredText(this.font, be == null || be.stage == 0 ? "Insert a nickel to rack up  (Esc to leave)" : "BALLS LEFT " + left + "    SHOTS " + be.shots, w / 2, 10, 0xFFFFE070);
         double pw = this.power(partialTick);
         int px = w - 30, py = h / 2 - 60;
         g.fill(px, py, px + 12, py + 120, 0x80000000);
         int fillH = (int)(pw * 120);
         g.fill(px, py + 120 - fillH, px + 12, py + 120, pw > 0.85 ? 0xFFFF3030 : pw > 0.5 ? 0xFF40FF70 : 0xFFFFD040);
         g.text(this.font, "Aim: look along the shot, then move the mouse    Shoot: hold click or Space, let go    Esc: leave", 6, h - 12, 0xC0FFFFFF, true);
      } else if (this.game == TicketGame.HOCKEY) {
         String status = be == null ? "" : be.playing ? "YOU " + be.score + "   -   " + be.shots + " MACHINE      " + Math.max(0, (this.game.roundTicks - (this.minecraft.level.getGameTime() - be.startTick)) / 20) + "s" : "Insert a nickel to play  (Esc to leave)";
         g.centeredText(this.font, status, w / 2, 10, 0xFFFFE070);
         g.text(this.font, "Move your mallet: WASD / arrows    Esc: leave", 6, h - 12, 0xC0FFFFFF, true);
      } else {
         String status = be == null ? "" : be.stage == 1 ? "Line up the red dot under the claw, then drop!" : be.stage == 3 ? "Grabbing..." : "Insert a nickel to play  (Esc to leave)";
         g.centeredText(this.font, status, w / 2, 10, 0xFFFFE070);
         g.text(this.font, "Move: WASD / arrows    Drop: Space or left click    Esc: leave", 6, h - 12, 0xC0FFFFFF, true);
      }
   }

   private static int bit(int key) {
      if (key == InputConstants.KEY_A || key == InputConstants.KEY_LEFT) return TicketInputPacket.LEFT;
      if (key == InputConstants.KEY_D || key == InputConstants.KEY_RIGHT) return TicketInputPacket.RIGHT;
      if (key == InputConstants.KEY_W || key == InputConstants.KEY_UP) return TicketInputPacket.UP;
      if (key == InputConstants.KEY_S || key == InputConstants.KEY_DOWN) return TicketInputPacket.DOWN;
      return 0;
   }

   private void sendKeys() {
      if (this.keys != this.sent) {
         this.sent = this.keys;
         ClientPlayNetworking.send(new TicketInputPacket(this.pos, this.keys, 0, 0));
      }
   }

   private void startAction() {
      if (this.game == TicketGame.BASKETBALL || this.game == TicketGame.LANES || this.game == TicketGame.POOL) {
         if (!this.charging) {
            this.charging = true;
            this.chargeStart = this.now(0);
         }
      } else {
         ClientPlayNetworking.send(new TicketInputPacket(this.pos, this.keys | TicketInputPacket.ACTION, 0, 0));
      }
   }

   private void endAction() {
      if ((this.game == TicketGame.BASKETBALL || this.game == TicketGame.LANES || this.game == TicketGame.POOL) && this.charging) {
         double pw = this.power(this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
         this.charging = false;
         ClientPlayNetworking.send(new TicketInputPacket(this.pos, TicketInputPacket.ACTION, (int)Math.round(this.aim * 1000), (int)Math.round(pw * 1000)));
      }
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      int k = event.key();
      if (k == InputConstants.KEY_ESCAPE) {
         this.onClose();
         return true;
      }
      if (k == InputConstants.KEY_SPACE) {
         this.startAction();
         return true;
      }
      if (this.game == TicketGame.CLAWMACHINE || this.game == TicketGame.HOCKEY) {
         this.keys |= bit(k);
         this.sendKeys();
      }
      return true;
   }

   @Override
   public boolean keyReleased(KeyEvent event) {
      if (event.key() == InputConstants.KEY_SPACE) {
         this.endAction();
         return true;
      }
      if (this.game == TicketGame.CLAWMACHINE || this.game == TicketGame.HOCKEY) {
         this.keys &= ~bit(event.key());
         this.sendKeys();
      }
      return true;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
      if (event.button() == 0) this.startAction();
      return true;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (event.button() == 0) this.endAction();
      return true;
   }

   @Override
   public void tick() {
      this.age++;
      TicketGameBlockEntity be = this.be();
      if (be == null) {
         this.onClose();
         return;
      }
      boolean active = this.game == TicketGame.BASKETBALL ? be.playing : be.stage != 0;
      this.idleTicks = active ? 0 : this.idleTicks + 1;
      if (this.idleTicks > 60) {     // game over (or never started): hand the mouse back
         this.onClose();
      }
   }

   @Override
   public void removed() {
      PoolAim.pos = null;
      this.keys = 0;
      this.sendKeys();
   }
}
