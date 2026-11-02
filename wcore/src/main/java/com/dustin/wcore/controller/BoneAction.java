package com.dustin.wcore.controller;

/**
 * What one model bone of a controller does with the pad input. Masks are the ControllerInput button bits
 * (A = 1 ... R3 = 8192), axes are the constants below. Applied procedurally by ControllerRenderer.
 */
public sealed interface BoneAction {
   int LX = 0;
   int LY = 1;
   int RX = 2;
   int RY = 3;
   int LT = 4;
   int RT = 5;

   /** Sinks the bone by dy (model units) while the button is held. */
   record Press(int mask, float dy) implements BoneAction {
   }

   /** Tilts the bone with a stick (tilt = radians at full deflection); clickMask sinks it (0 = no click). */
   record Stick(int axisX, int axisY, int clickMask, float tilt) implements BoneAction {
   }

   /** Rotates the bone around x by the analog axis value times rot. */
   record Trigger(int axis, float rot) implements BoneAction {
   }

   /** Rocks the d-pad toward the held direction by tilt radians. */
   record Dpad(float tilt) implements BoneAction {
   }

   /** Sinks the bone by the analog axis value times dy (GameCube L/R, N64 Z). */
   record AnalogPress(int axis, float dy) implements BoneAction {
   }
}
