package com.dustin.stream.client;

import com.dustin.stream.ProjectorBlock;
import com.dustin.stream.net.MediaActionPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Using a screen: no menu and no overlay picture. The world keeps rendering and the page you
 * see is the one on the in-world screen. This only catches your real mouse and keyboard and
 * hands them to that browser (and to the desk keyboard/mouse animation).
 */
final class ControlScreen extends Screen {
   private final BlockPos pos;
   private EditBox address;

   ControlScreen(BlockPos pos) {
      super(Component.literal("Browser"));
      this.pos = pos;
   }

   private Browsers.Session session() {
      return Browsers.get(this.pos);
   }

   @Override
   protected void init() {
      this.address = new EditBox(this.font, this.width / 2 - 200, this.height - 44, 400, 18, Component.literal("Address"));
      this.address.setMaxLength(2048);
      this.address.visible = false;
      this.addRenderableWidget(this.address);
      InputFx.controlling = this.pos;
      InputFx.focusDirty = true;
      this.minecraft.textInputManager().startTextInput(this);
      Browsers.Session s = this.session();
      if (s != null) {
         s.browser.setFocus(true);
      }
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
      boolean projector = this.minecraft.level != null && this.minecraft.level.getBlockState(this.pos).getBlock() instanceof ProjectorBlock;
      boolean hdmi = this.minecraft.level != null && this.minecraft.level.getBlockEntity(this.pos) instanceof com.dustin.stream.MediaBlockEntity m && m.input() > 0;
      String pad = BrowserGamepad.padName();
      String hint = (hdmi ? "ESC exit   Ctrl+I input" : this.session() == null ? "Starting the browser...   Ctrl+I input" :
         "ESC exit   Ctrl+I input   Ctrl+L address   Ctrl+H home   Alt+Left/Right back/forward   F5 reload   F8 test picker   F9 pad check")
         + (projector ? "   PgUp/PgDn screen size" : "") + (pad == null ? "" : "   Controller: " + pad);
      g.text(this.font, hint, 6, this.height - 12, 0xC0FFFFFF, true);
   }

   @Override
   public void tick() {
      Browsers.Session s = this.session();
      if (s != null && InputFx.focusDirty) {
         InputFx.focusDirty = false;
         s.browser.setFocus(true);
      }
      if (s == null && this.minecraft.level != null && !(this.minecraft.level.getBlockEntity(this.pos) instanceof com.dustin.stream.MediaBlockEntity)) {
         this.onClose();
      }
   }

   @Override
   public void removed() {
      this.minecraft.textInputManager().stopTextInput(this);
      if (this.pos.equals(InputFx.controlling)) {
         InputFx.controlling = null;
      }
      Browsers.Session s = this.session();
      if (s != null) {
         s.browser.setFocus(false);
         Browsers.saveUrl(this.pos, s);
      }
   }

   private int bx(double x) {
      return (int)Math.max(0, Math.min(Browsers.W - 1, x / this.width * Browsers.W));
   }

   private int by(double y) {
      return (int)Math.max(0, Math.min(Browsers.H - 1, y / this.height * Browsers.H));
   }

   private boolean overAddress(double x, double y) {
      return this.address.visible && this.address.isMouseOver(x, y);
   }

   @Override
   public void mouseMoved(double x, double y) {
      InputFx.cursorX = (float)(x / this.width);
      InputFx.cursorY = (float)(y / this.height);
      Browsers.Session s = this.session();
      if (s != null) {
         s.browser.onMouseMoved(this.bx(x), this.by(y));
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
      if (this.overAddress(event.x(), event.y())) {
         return super.mouseClicked(event, doubled);
      }
      if (this.address.visible) {
         this.hideAddress();
      }
      InputFx.lastClickAt = System.currentTimeMillis();
      InputFx.lastButton = event.button();
      Browsers.Session s = this.session();
      // 1 = left, 2 = middle, 3 = right in this Minecraft version; the browser gets left and middle.
      // Right-click stays in Minecraft: the browser's own context menu can't open inside the game.
      if (s != null && (event.button() == 1 || event.button() == 2)) {
         s.browser.onMouseClicked(new MouseButtonEvent(this.bx(event.x()), this.by(event.y()), event.buttonInfo()), doubled);
      }
      return true;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      Browsers.Session s = this.session();
      if (s != null && (event.button() == 1 || event.button() == 2)) {
         s.browser.onMouseReleased(new MouseButtonEvent(this.bx(event.x()), this.by(event.y()), event.buttonInfo()));
      }
      return super.mouseReleased(event);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      this.mouseMoved(event.x(), event.y());
      return true;
   }

   @Override
   public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      Browsers.Session s = this.session();
      if (s != null) {
         s.browser.onMouseScrolled(this.bx(x), this.by(y), vertical);
      }
      return true;
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      InputFx.lastKeyAt = System.currentTimeMillis();
      InputFx.lastKey = event.key();
      Browsers.Session s = this.session();
      int key = event.key();
      if (this.address.visible) {
         if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_RETURN) {
            if (key == InputConstants.KEY_RETURN) {
               this.go(this.address.getValue());
            }
            this.hideAddress();
            return true;
         }
         return super.keyPressed(event);
      }
      if (key == InputConstants.KEY_ESCAPE) {
         this.onClose();
         return true;
      }
      if (event.hasControlDown() && key == InputConstants.KEY_I) {
         ClientPlayNetworking.send(new MediaActionPacket(this.pos, MediaActionPacket.INPUT_NEXT));
         return true;
      }
      if (key == InputConstants.KEY_PAGEUP || key == InputConstants.KEY_PAGEDOWN) {
         ClientPlayNetworking.send(new MediaActionPacket(this.pos, key == InputConstants.KEY_PAGEUP ? MediaActionPacket.SIZE_UP : MediaActionPacket.SIZE_DOWN));
         return true;
      }
      if (s == null) {
         return true;
      }
      if (event.hasControlDown() && key == InputConstants.KEY_L) {
         this.address.setValue(s.browser.getCefBrowser().getURL().startsWith("data:") ? "" : s.browser.getCefBrowser().getURL());
         this.address.visible = true;
         this.setFocused(this.address);
         this.address.setFocused(true);
         return true;
      }
      if (event.hasControlDown() && key == InputConstants.KEY_H) {
         s.browser.getCefBrowser().loadURL(Home.url());
         return true;
      }
      if (event.hasAltDown() && key == InputConstants.KEY_LEFT) {
         s.browser.getCefBrowser().goBack();
         return true;
      }
      if (event.hasAltDown() && key == InputConstants.KEY_RIGHT) {
         s.browser.getCefBrowser().goForward();
         return true;
      }
      if (key == InputConstants.KEY_F5) {
         s.browser.getCefBrowser().reload();
         return true;
      }
      if (key == InputConstants.KEY_F8) {
         BrowserFiles.forceTestPicker(s.browser.getCefBrowser());
         return true;
      }
      if (key == InputConstants.KEY_F9) {
         BrowserGamepad.toggleOverlay(s);
         return true;
      }
      s.browser.onKeyPressed(event);
      return true;
   }

   @Override
   public boolean keyReleased(KeyEvent event) {
      Browsers.Session s = this.session();
      if (this.address.visible) {
         return super.keyReleased(event);
      }
      if (s != null) {
         s.browser.onKeyReleased(event);
      }
      return true;
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      if (this.address.visible) {
         return super.charTyped(event);
      }
      Browsers.Session s = this.session();
      if (s != null) {
         s.browser.onCharTyped(event);
      }
      return true;
   }

   private void hideAddress() {
      this.address.visible = false;
      this.address.setFocused(false);
      this.setFocused(null);
      // closing the address box can switch text input off; switch it back on for the browser
      this.minecraft.textInputManager().startTextInput(this);
   }

   private void go(String text) {
      Browsers.Session s = this.session();
      String t = text == null ? "" : text.trim();
      if (s == null || t.isEmpty()) {
         return;
      }
      String url;
      if (t.contains("://")) {
         url = t;
      } else if (!t.contains(" ") && t.contains(".")) {
         url = "https://" + t;
      } else {
         url = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(t, java.nio.charset.StandardCharsets.UTF_8);
      }
      s.browser.getCefBrowser().loadURL(url);
   }
}
