package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.PinballBlockEntity;
import com.dustin.arcadeplus.net.PinballInputPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Playing the table: no menu drawn, the world stays visible; keys and mouse buttons drive the flippers and plunger. */
final class PinballScreen extends Screen {
   private final BlockPos pos;
   private int keys;
   private int mouse;
   private int sent = -1;

   PinballScreen(BlockPos pos) {
      super(Component.literal("Pinball"));
      this.pos = pos;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(g, mouseX, mouseY, partialTick);
      String status = "";
      if (this.minecraft.level != null && this.minecraft.level.getBlockEntity(this.pos) instanceof PinballBlockEntity be) {
         status = be.playing ? "SCORE " + be.score + "   BALL " + Math.max(1, 4 - be.balls) + "/3" : "GAME OVER  " + be.score + "   (Esc to leave)";
      }
      g.centeredText(this.font, status, this.width / 2, 10, 0xFFFFE070);
      g.text(this.font, "Flippers: A / D, Left / Right, Shift keys or mouse buttons    Plunger: hold Space or S    Esc: leave", 6, this.height - 12, 0xC0FFFFFF, true);
   }

   private static int bit(int key) {
      if (key == InputConstants.KEY_A || key == InputConstants.KEY_LEFT || key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_Z) {
         return PinballBlockEntity.LEFT;
      }
      if (key == InputConstants.KEY_D || key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_RSHIFT || key == InputConstants.KEY_SLASH) {
         return PinballBlockEntity.RIGHT;
      }
      if (key == InputConstants.KEY_SPACE || key == InputConstants.KEY_S || key == InputConstants.KEY_DOWN) {
         return PinballBlockEntity.PLUNGER;
      }
      return 0;
   }

   private void send() {
      int flags = this.keys | this.mouse;
      if (flags != this.sent) {
         this.sent = flags;
         ClientPlayNetworking.send(new PinballInputPacket(this.pos, flags));
      }
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (event.key() == InputConstants.KEY_ESCAPE) {
         this.onClose();
         return true;
      }
      this.keys |= bit(event.key());
      this.send();
      return true;
   }

   @Override
   public boolean keyReleased(KeyEvent event) {
      this.keys &= ~bit(event.key());
      this.send();
      return true;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
      this.mouse |= event.button() == 0 ? PinballBlockEntity.LEFT : event.button() == 1 ? PinballBlockEntity.RIGHT : 0;
      this.send();
      return true;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      this.mouse &= ~(event.button() == 0 ? PinballBlockEntity.LEFT : event.button() == 1 ? PinballBlockEntity.RIGHT : 0);
      this.send();
      return true;
   }

   @Override
   public void tick() {
      if (this.minecraft.level == null || !(this.minecraft.level.getBlockEntity(this.pos) instanceof PinballBlockEntity)) {
         this.onClose();
      }
   }

   @Override
   public void removed() {
      this.keys = 0;
      this.mouse = 0;
      this.send();
   }
}
