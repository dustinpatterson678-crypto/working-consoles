package com.dustin.gamecube.client;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;

/**
 * Copies a finished GameCube picture from the texture the emulator draws into onto a second, private texture that
 * Minecraft shows on the TV. The emulator keeps drawing into its own texture while Minecraft is drawing frames, so
 * showing it directly can catch a half-drawn picture (flicker); copying only once a new picture is complete avoids that.
 * The copy is also flipped, because OpenGL stores pictures bottom-up.
 */
final class FrameCopy {
   private int readFbo;
   private int drawFbo;
   // after a failed copy, wait this many attempts before trying again; give up only after several failures in a row
   private static final int RETRY_AFTER = 120;
   private static final int GIVE_UP_AFTER = 3;
   private int failures;
   private int waiting;
   private boolean copiedOnce;

   private void failed() {
      this.failures++;
      this.waiting = RETRY_AFTER;
   }

   /** True once a picture has been copied, so the private texture holds a whole picture that can stay on the TV. */
   boolean hasPicture() {
      return this.copiedOnce;
   }

   /** Returns true if the picture was copied; false means the caller should show the original texture instead. */
   boolean copy(int srcTexture, int dstTexture, int width, int height) {
      if (srcTexture == 0 || dstTexture == 0 || this.failures >= GIVE_UP_AFTER) {
         return false;
      }

      if (this.waiting > 0) {
         this.waiting--;
         return false;
      }

      int prevRead = 0;
      int prevDraw = 0;
      boolean restore = false;
      try {
         prevRead = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
         prevDraw = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
         restore = true;
         boolean scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
         if (this.readFbo == 0) {
            this.readFbo = GL30C.glGenFramebuffers();
            this.drawFbo = GL30C.glGenFramebuffers();
         }

         GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, this.readFbo);
         GL30C.glFramebufferTexture2D(GL30C.GL_READ_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, srcTexture, 0);
         GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, this.drawFbo);
         GL30C.glFramebufferTexture2D(GL30C.GL_DRAW_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, dstTexture, 0);
         boolean ok = GL30C.glCheckFramebufferStatus(GL30C.GL_READ_FRAMEBUFFER) == GL30C.GL_FRAMEBUFFER_COMPLETE
            && GL30C.glCheckFramebufferStatus(GL30C.GL_DRAW_FRAMEBUFFER) == GL30C.GL_FRAMEBUFFER_COMPLETE;
         if (ok) {
            if (scissor) {
               GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
            }

            // destination rectangle runs top-to-bottom: this flips the picture the right way up
            GL30C.glBlitFramebuffer(0, 0, width, height, 0, height, width, 0, GL11C.GL_COLOR_BUFFER_BIT, GL11C.GL_NEAREST);
            if (scissor) {
               GL11C.glEnable(GL11C.GL_SCISSOR_TEST);
            }
            this.failures = 0;
            this.copiedOnce = true;
         } else {
            this.failed();
         }

         return ok;
      } catch (Throwable t) {
         this.failed();
         return false;
      } finally {
         if (restore) {
            try {
               GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, prevRead);
               GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, prevDraw);
            } catch (Throwable ignored) {
            }
         }
      }
   }
}
