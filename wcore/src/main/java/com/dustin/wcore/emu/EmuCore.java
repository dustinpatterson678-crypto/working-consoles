package com.dustin.wcore.emu;

import java.util.Map;

public interface EmuCore extends AutoCloseable {
   void setKeys(int var1);

   default void setAnalog(int lx, int ly, int rx, int ry) {
   }

   default void setPort2(int keys, int lx, int ly, int rx, int ry) {
   }

   void runFrame();

   default void linkPoll() {
   }

   default boolean linkBlocked() {
      return false;
   }

   default void linkAbort() {
   }

   int[] copyVideo(int[] var1);

   /**
    * True when the core renders directly into a shared GPU texture. In that
    * mode {@link #copyVideo(int[])} must not be used for normal display; the
    * caller only needs {@link #videoSerial()} to know when a new frame is
    * ready.
    */
   default boolean directVideo() {
      return false;
   }

   /** Monotonically increasing serial for the latest completed direct frame. */
   default int videoSerial() {
      return 0;
   }

   default int videoWidth() {
      return 0;
   }

   default int videoHeight() {
      return 0;
   }

   int audioRate();

   int readAudio(byte[] var1);

   double fps();

   default void applyOptions(Map<String, String> options) {
   }

   default int audioLatencyMs() {
      return 60;
   }

   int maxPixels();

   @Override
   void close();
}
