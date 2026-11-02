package com.dustin.wcore.client;

import com.mojang.blaze3d.platform.InputConstants;

public final class RetroPadInput {
   public int buttons;
   public int lx;
   public int ly;
   public int rx;
   public int ry;

   public void poll(boolean active, boolean keyboard) {
      this.buttons = this.lx = this.ly = this.rx = this.ry = 0;
      if (active) {
         this.pollFrom(ControllerInput.state(), keyboard);
      }
   }

   public void pollFrom(ControllerInput.State s, boolean keyboard) {
      this.buttons = this.lx = this.ly = this.rx = this.ry = 0;
      if (s.connected) {
         this.map(s, 1, 1);
         this.map(s, 2, 256);
         this.map(s, 4, 2);
         this.map(s, 8, 512);
         this.map(s, 16, 1024);
         this.map(s, 32, 2048);
         this.map(s, 64, 4);
         this.map(s, 128, 8);
         this.map(s, 256, 16);
         this.map(s, 512, 32);
         this.map(s, 1024, 64);
         this.map(s, 2048, 128);
         this.map(s, 4096, 16384);
         this.map(s, 8192, 32768);
         if (s.lt > 0.3F) {
            this.buttons |= 4096;
         }

         if (s.rt > 0.3F) {
            this.buttons |= 8192;
         }

         this.lx = axis(s.lx);
         this.ly = axis(s.ly);
         this.rx = axis(s.rx);
         this.ry = axis(s.ry);
      } else if (keyboard) {
         this.key(27, 1);
         this.key(6, 256);
         this.key(29, 2);
         this.key(25, 512);
         this.key(47, 1024);
         this.key(48, 2048);
         this.key(40, 8);
         this.key(42, 4);
         this.key(82, 16);
         this.key(81, 32);
         this.key(80, 64);
         this.key(79, 128);
         if (InputConstants.isKeyDown(13)) {
            this.lx -= 32767;
         }

         if (InputConstants.isKeyDown(15)) {
            this.lx += 32767;
         }

         if (InputConstants.isKeyDown(12)) {
            this.ly -= 32767;
         }

         if (InputConstants.isKeyDown(14)) {
            this.ly += 32767;
         }
      }
   }

   private void map(ControllerInput.State s, int from, int to) {
      if (s.down(from)) {
         this.buttons |= to;
      }
   }

   private void key(int key, int to) {
      if (InputConstants.isKeyDown(key)) {
         this.buttons |= to;
      }
   }

   private static int axis(float v) {
      return Math.max(-32767, Math.min(32767, Math.round(v * 32767.0F)));
   }
}
