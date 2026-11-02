package com.dustin.gbsp.client;

import com.dustin.wcore.client.ControllerInput;
import com.dustin.wcore.client.ControllerInput.State;
import com.mojang.blaze3d.platform.InputConstants;

public final class HandheldInput {
   private HandheldInput() {
   }

   public static int poll(boolean bigView) {
      int keys = 0;
      if (InputConstants.isKeyDown(27)) {
         keys |= 1;
      }

      if (InputConstants.isKeyDown(29)) {
         keys |= 2;
      }

      if (InputConstants.isKeyDown(40)) {
         keys |= 8;
      }

      if (InputConstants.isKeyDown(42) || InputConstants.isKeyDown(229)) {
         keys |= 4;
      }

      if (InputConstants.isKeyDown(79)) {
         keys |= 16;
      }

      if (InputConstants.isKeyDown(80)) {
         keys |= 32;
      }

      if (InputConstants.isKeyDown(82)) {
         keys |= 64;
      }

      if (InputConstants.isKeyDown(81)) {
         keys |= 128;
      }

      if (InputConstants.isKeyDown(47) || bigView && InputConstants.isKeyDown(4)) {
         keys |= 512;
      }

      if (InputConstants.isKeyDown(48) || bigView && InputConstants.isKeyDown(22)) {
         keys |= 256;
      }

      return keys | pollGamepad();
   }

   public static boolean controllerConnected() {
      return ControllerInput.state().connected;
   }

   private static int pollGamepad() {
      State s = ControllerInput.state();
      if (!s.connected) {
         return 0;
      }

      int keys = 0;
      if (s.down(2)) {
         keys |= 1;
      }

      if (s.down(1)) {
         keys |= 2;
      }

      if (s.down(128)) {
         keys |= 8;
      }

      if (s.down(64)) {
         keys |= 4;
      }

      if (s.down(16) || s.lt > 0.25F) {
         keys |= 512;
      }

      if (s.down(32) || s.rt > 0.25F) {
         keys |= 256;
      }

      if (s.down(2048) || s.lx > 0.5F) {
         keys |= 16;
      }

      if (s.down(1024) || s.lx < -0.5F) {
         keys |= 32;
      }

      if (s.down(256) || s.ly < -0.5F) {
         keys |= 64;
      }

      if (s.down(512) || s.ly > 0.5F) {
         keys |= 128;
      }

      return keys;
   }
}
