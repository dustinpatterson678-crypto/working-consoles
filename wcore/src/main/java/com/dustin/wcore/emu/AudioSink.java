package com.dustin.wcore.emu;

public abstract class AudioSink {
   public int underruns;
   public int overflows;

   public abstract int rate();

   public abstract void start();

   public abstract void pause();

   public abstract void write(byte[] var1, int var2);

   public abstract double queuedMs();

   public abstract double targetMs();

   public abstract double tick();

   public abstract void close();

   public interface Factory {
      AudioSink open(int var1, int var2);
   }
}
