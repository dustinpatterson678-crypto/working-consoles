package com.dustin.wcore.client;

import com.dustin.wcore.WorkingConsolesCore;
import java.nio.IntBuffer;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.sdl.SDLGamepad;
import org.lwjgl.sdl.SDLInit;

public final class ControllerInput {
   public static final int A = 1;
   public static final int B = 2;
   public static final int X = 4;
   public static final int Y = 8;
   public static final int LB = 16;
   public static final int RB = 32;
   public static final int BACK = 64;
   public static final int START = 128;
   public static final int UP = 256;
   public static final int DOWN = 512;
   public static final int LEFT = 1024;
   public static final int RIGHT = 2048;
   public static final int L3 = 4096;
   public static final int R3 = 8192;
   private static final ControllerInput.State STATE = new ControllerInput.State();
   private static final ControllerInput.State STATE2 = new ControllerInput.State();
   private static boolean sdlReady;
   private static boolean sdlFailed;
   private static final long[] PADS = new long[2];
   private static long pad;
   private static long nextScan;
   private static long lastPoll;

   private ControllerInput() {
   }

   public static ControllerInput.State state() {
      long now = System.nanoTime();
      if (now - lastPoll > 2000000L) {
         lastPoll = now;
         read();
      }

      return STATE;
   }

   public static ControllerInput.State state2() {
      state();
      return STATE2;
   }

   public static boolean isHoldingController(Player player) {
      // any console's pad counts, not just the wireless controller
      return player.getMainHandItem().getItem() instanceof com.dustin.wcore.controller.ConsoleController
         || player.getOffhandItem().getItem() instanceof com.dustin.wcore.controller.ConsoleController;
   }

   private static void read() {
      clear(STATE);
      clear(STATE2);
      if (init()) {
         try {
            SDLGamepad.SDL_UpdateGamepads();

            for (int i = 0; i < PADS.length; i++) {
               if (PADS[i] != 0L && !SDLGamepad.SDL_GamepadConnected(PADS[i])) {
                  SDLGamepad.SDL_CloseGamepad(PADS[i]);
                  PADS[i] = 0L;
               }
            }

            if (PADS[0] == 0L && PADS[1] != 0L) {
               PADS[0] = PADS[1];
               PADS[1] = 0L;
            }

            if ((PADS[0] == 0L || PADS[1] == 0L) && System.nanoTime() >= nextScan) {
               nextScan = System.nanoTime() + 1000000000L;
               IntBuffer ids = SDLGamepad.SDL_GetGamepads();

               for (int k = 0; ids != null && k < ids.remaining(); k++) {
                  int id = ids.get(ids.position() + k);
                  if (!isOpen(id)) {
                     long h = SDLGamepad.SDL_OpenGamepad(id);
                     if (h != 0L) {
                        if (PADS[0] == 0L) {
                           PADS[0] = h;
                        } else if (PADS[1] == 0L) {
                           PADS[1] = h;
                        }
                     }
                  }
               }
            }

            fill(STATE, PADS[0]);
            fill(STATE2, PADS[1]);
         } catch (Throwable t) {
            WorkingConsolesCore.LOGGER.warn("Controller read failed, turning controller support off: {}", t.toString());
            sdlFailed = true;
            sdlReady = false;
            PADS[0] = PADS[1] = 0L;
         }
      }
   }

   private static boolean isOpen(int id) {
      for (long p : PADS) {
         if (p != 0L && SDLGamepad.SDL_GetGamepadID(p) == id) {
            return true;
         }
      }

      return false;
   }

   private static void clear(ControllerInput.State s) {
      s.buttons = 0;
      s.lx = s.ly = s.rx = s.ry = s.lt = s.rt = 0.0F;
      s.connected = false;
   }

   private static void fill(ControllerInput.State s, long p) {
      if (p != 0L) {
         pad = p;
         s.connected = true;
         bit(s, 1, 0);
         bit(s, 2, 1);
         bit(s, 4, 2);
         bit(s, 8, 3);
         bit(s, 16, 9);
         bit(s, 32, 10);
         bit(s, 64, 4);
         bit(s, 128, 6);
         bit(s, 256, 11);
         bit(s, 512, 12);
         bit(s, 1024, 13);
         bit(s, 2048, 14);
         bit(s, 4096, 7);
         bit(s, 8192, 8);
         s.lx = axis(0);
         s.ly = axis(1);
         s.rx = axis(2);
         s.ry = axis(3);
         s.lt = Math.max(0.0F, axis(4));
         s.rt = Math.max(0.0F, axis(5));
      }
   }

   private static boolean init() {
      if (sdlReady) {
         return true;
      }

      if (sdlFailed) {
         return false;
      }

      try {
         sdlReady = SDLInit.SDL_InitSubSystem(8192);
         sdlFailed = !sdlReady;
      } catch (Throwable t) {
         WorkingConsolesCore.LOGGER.warn("Controller support unavailable: {}", t.toString());
         sdlFailed = true;
      }

      return sdlReady;
   }

   private static void bit(ControllerInput.State s, int mask, int button) {
      if (SDLGamepad.SDL_GetGamepadButton(pad, button)) {
         s.buttons |= mask;
      }
   }

   private static float axis(int a) {
      return SDLGamepad.SDL_GetGamepadAxis(pad, a) / 32767.0F;
   }

   public static final class State {
      public int buttons;
      public float lx;
      public float ly;
      public float rx;
      public float ry;
      public float lt;
      public float rt;
      public boolean connected;

      public boolean down(int mask) {
         return (this.buttons & mask) != 0;
      }
   }
}
