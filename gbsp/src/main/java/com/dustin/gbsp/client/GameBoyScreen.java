package com.dustin.gbsp.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public class GameBoyScreen extends Screen {
   public GameBoyScreen() {
      super(Component.literal("Gameboy in Minecraft"));
   }

   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      super.extractRenderState(g, mouseX, mouseY, partialTick);
      HandheldSession.pump(Minecraft.getInstance());
      int cx = this.width / 2;
      if (!HandheldSession.isOn()) {
         g.centeredText(this.font, "The handheld is off", cx, this.height / 2, -1);
      } else {
         int pw = HandheldSession.pictureWidth();
         int ph = HandheldSession.pictureHeight();
         int scale = Math.max(1, Math.min((this.width - 40) / pw, (this.height - 60) / ph));
         int dw = pw * scale;
         int dh = ph * scale;
         int x0 = cx - dw / 2;
         int y0 = (this.height - dh) / 2 - 6;
         g.fill(x0 - 10, y0 - 10, x0 + dw + 10, y0 + dh + 10, -15000801);
         g.fill(x0 - 2, y0 - 2, x0 + dw + 2, y0 + dh + 2, -16119284);
         if (HandheldSession.hasPicture()) {
            g.blit(RenderPipelines.GUI_TEXTURED, HandheldSession.SCREEN_TEX, x0, y0, 0.0F, 0.0F, dw, dh, pw, ph, 256, 224);
         }

         String hint = HandheldInput.controllerConnected()
            ? "Controller connected  |  Esc: back to playing in your hands"
            : "Arrows  X=A  Z=B  Enter=Start  Backspace=Select  A / [ = L  S / ] = R  |  Esc: back";
         g.centeredText(this.font, HandheldSession.title(), cx, y0 - 24, -1);
         g.centeredText(this.font, hint, cx, y0 + dh + 16, -5592406);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
