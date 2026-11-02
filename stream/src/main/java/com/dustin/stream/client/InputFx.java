package com.dustin.stream.client;

import net.minecraft.core.BlockPos;

/** What the player is doing right now with the browser: drives the cursor on the screen and the desk keyboard/mouse. */
final class InputFx {
   static volatile BlockPos controlling;
   static volatile float cursorX = 0.5F;
   static volatile float cursorY = 0.5F;
   static volatile long lastKeyAt;
   static volatile int lastKey;
   static volatile long lastClickAt;
   static volatile int lastButton;
   static volatile boolean focusDirty = true;

   private InputFx() {
   }

   static boolean active() {
      return controlling != null;
   }
}
