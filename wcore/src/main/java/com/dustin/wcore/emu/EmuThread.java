package com.dustin.wcore.emu;

import com.dustin.wcore.WorkingConsolesCore;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public final class EmuThread implements Runnable {
   private static final double FPS = 59.72750056960583;
   private final long FRAME_NANOS;
   private final String name;
   private final EmuCore core;
   private final boolean direct;
   private final Thread thread;
   private volatile boolean running = true;
   private volatile boolean paused;
   private volatile int keys;
   private volatile int lx;
   private volatile int ly;
   private volatile int rx;
   private volatile int ry;
   private volatile int[] port2 = new int[5];
   private volatile float volume = 0.8F;
   private int[] back;
   private int[] ready;
   private int[] front;
   private final Object swapLock = new Object();
   private volatile boolean fresh;
   private volatile int width = 240;
   private volatile int height = 160;
   private volatile long frameCount;
   private volatile int directSerial;
   private boolean gotPicture;
   private volatile EmuThread.Stats stats = new EmuThread.Stats(0.0, 0.0, 0.0, 0, 0);
   private long liveFrames;
   private long liveStart = System.nanoTime();
   private long liveLate;
   private long liveGaps0;
   private double liveFill;
   private final AudioSink.Factory sinkFactory;
   private volatile Map<String, String> pendingOptions;
   private volatile EmuThread.AudioTap tap;
   private final EmuThread.Resampler resampler = new EmuThread.Resampler();

   public EmuThread.Stats stats() {
      return this.stats;
   }

   public EmuThread(EmuCore core, String name) {
      this(core, name, null);
   }

   public EmuThread(EmuCore core, String name, AudioSink.Factory sinkFactory) {
      this.sinkFactory = sinkFactory;
      this.core = core;
      this.name = name;
      this.direct = core.directVideo();
      this.FRAME_NANOS = (long)(1.0E9 / core.fps());
      if (!this.direct) {
         this.back = new int[core.maxPixels()];
         this.ready = new int[core.maxPixels()];
         this.front = new int[core.maxPixels()];
      }
      this.thread = new Thread(this, "WC-Emu-" + name);
      this.thread.setDaemon(true);
      this.thread.setPriority(6);
      this.thread.start();
   }

   public void setKeys(int keys) {
      this.keys = keys;
   }

   public void setAnalog(int lx, int ly, int rx, int ry) {
      this.lx = lx;
      this.ly = ly;
      this.rx = rx;
      this.ry = ry;
   }

   public void setPort2(int keys, int lx, int ly, int rx, int ry) {
      this.port2 = new int[]{keys, lx, ly, rx, ry};
   }

   public void setPaused(boolean paused) {
      this.paused = paused;
   }

   public boolean isPaused() {
      return this.paused;
   }

   public void setOptions(Map<String, String> options) {
      this.pendingOptions = options;
   }

   public void setAudioTap(EmuThread.AudioTap tap) {
      this.tap = tap;
   }

   public void setVolume(float volume) {
      this.volume = Math.max(0.0F, Math.min(1.0F, volume));
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   public long frameCount() {
      return this.frameCount;
   }

   public boolean directVideo() {
      return this.direct;
   }

   /**
    * Serial of the latest frame the core finished rendering directly into the
    * shared texture. The render thread compares this against its own last
    * value, so duplicate frames never touch the screen.
    */
   public int directSerial() {
      return this.directSerial;
   }

   public int[] takeFrame() {
      if (this.direct || !this.fresh) {
         return null;
      }

      synchronized (this.swapLock) {
         int[] t = this.front;
         this.front = this.ready;
         this.ready = t;
         this.fresh = false;
      }

      return this.front;
   }

   public void stop() {
      // Idempotent + monotonic: a second stop() call is a no-op once we are shutting down.
      if (!this.running && !this.thread.isAlive()) {
         return;
      }
      this.running = false;
      this.thread.interrupt();

      try {
         // Bounded, generous join: the run loop exits within a single sleep cycle
         // once interrupted, but a native frame call or audio callback can take a
         // moment to unwind, so we allow up to 2s before giving up and logging.
         this.thread.join(2000L);
      } catch (InterruptedException ignored) {
         Thread.currentThread().interrupt();
      }

      if (this.thread.isAlive()) {
         WorkingConsolesCore.LOGGER.warn("{}: emulator thread did not terminate within 2000 ms", this.name);
      }
   }

   public boolean isAlive() {
      return this.thread.isAlive();
   }

   /** True while the run loop is still supposed to be producing frames. */
   public boolean isRunning() {
      return this.running;
   }

   @Override
   public void run() {
      int srcRate = this.core.audioRate();
      AudioSink custom = this.sinkFactory == null ? null : this.sinkFactory.open(srcRate, this.core.audioLatencyMs());
      SourceDataLine line = custom == null && srcRate != 48000 ? openAudio(48000) : null;
      boolean resample = line != null;
      if (line == null && custom == null) {
         line = openAudio(srcRate);
      }

      int outRate = resample ? 48000 : srcRate;
      AudioSink out = custom != null ? custom : (line == null ? null : new EmuThread.Speaker(line, outRate, this.core.audioLatencyMs()));
      if (custom != null) {
         resample = custom.rate() != srcRate;
      }

      byte[] audio = new byte[16384];
      byte[] converted = new byte[32768];
      long next = System.nanoTime();
      boolean wasPaused = true;
      double lean = 0.0;
      long statFrames = 0L;
      long statNanos = 0L;
      long statLast = System.nanoTime();
      long statLate = 0L;
      double statFill = 0.0;

      try {
         while (this.running) {
            if (this.paused) {
               if (out != null && !wasPaused) {
                  out.pause();
               }

               wasPaused = true;
               sleepNanos(20000000L);
               next = System.nanoTime();
               statLast = next;
            } else {
               if (wasPaused) {
                  wasPaused = false;
                  if (out != null) {
                     out.start();
                  }

                  next = System.nanoTime();
               }

               Map<String, String> opts = this.pendingOptions;
               if (opts != null) {
                  this.pendingOptions = null;
                  this.core.applyOptions(opts);
                  WorkingConsolesCore.LOGGER.info("{}: applied {} emulator settings", this.name, opts.size());
               }

               this.core.linkPoll();
               if (this.core.linkBlocked()) {
                  long until = System.nanoTime() + 1000000000L;

                  while (this.running && this.core.linkBlocked() && System.nanoTime() < until) {
                     sleepNanos(500000L);
                     this.core.linkPoll();
                  }

                  if (this.core.linkBlocked()) {
                     this.core.linkAbort();
                  }

                  next = System.nanoTime();
               }

               this.core.setKeys(this.keys);
               this.core.setAnalog(this.lx, this.ly, this.rx, this.ry);
               int[] p2 = this.port2;
               this.core.setPort2(p2[0], p2[1], p2[2], p2[3], p2[4]);
               this.core.runFrame();
               this.core.linkPoll();
               if (this.direct) {
                  int serial = this.core.videoSerial();
                  if (serial != this.directSerial) {
                     this.directSerial = serial;
                     this.gotPicture = true;
                     int w = this.core.videoWidth();
                     int h = this.core.videoHeight();
                     if (w > 0 && h > 0 && (this.width != w || this.height != h || this.frameCount == 0L)) {
                        WorkingConsolesCore.LOGGER.info("{} picture {}x{} (direct texture)", new Object[]{this.name, w, h});
                        this.width = w;
                        this.height = h;
                     }
                  }
               } else {
                  int[] wh = this.core.copyVideo(this.back);
                  if (wh[0] > 0) {
                     this.gotPicture = true;
                     if (this.width != wh[0] || this.height != wh[1] || this.frameCount == 0L) {
                        WorkingConsolesCore.LOGGER.info("{} picture {}x{}", new Object[]{this.name, wh[0], wh[1]});
                     }

                     this.width = wh[0];
                     this.height = wh[1];
                     synchronized (this.swapLock) {
                        int[] t = this.ready;
                        this.ready = this.back;
                        this.back = t;
                        this.fresh = true;
                     }
                  }
               }

               this.frameCount++;
               if (this.frameCount == 600L && !this.gotPicture) {
                  WorkingConsolesCore.LOGGER.warn("{}: no picture from the emulator after 10 seconds", this.name);
               }

               int bytes = this.core.readAudio(audio);
               EmuThread.AudioTap t = this.tap;
               if (t != null && bytes > 0) {
                  t.accept(audio, bytes, srcRate);
               }

               if (out != null) {
                  if (bytes > 0) {
                     applyVolume(audio, bytes, this.volume);
                     if (resample) {
                        out.write(converted, this.resampler.process(audio, bytes, srcRate, converted));
                     } else {
                        out.write(audio, bytes);
                     }
                  }

                  lean = out.tick();
                  statFill += out.queuedMs();
               }

               next += (long)(this.FRAME_NANOS * (1.0 + lean));
               long wait = next - System.nanoTime();
               if (wait > 0L) {
                  sleepNanos(wait);
               } else if (-wait > this.FRAME_NANOS * 3L / 2L) {
                  next = System.nanoTime();
                  statLate++;
               }

               long now = System.nanoTime();
               this.liveFrames++;
               if (out != null) {
                  this.liveFill = this.liveFill + out.queuedMs();
               }

               if (now - this.liveStart >= 1000000000L) {
                  int gaps = out == null ? 0 : out.underruns;
                  this.stats = new EmuThread.Stats(
                     this.liveFrames * 1.0E9 / (now - this.liveStart),
                     this.liveFill / Math.max(1L, this.liveFrames),
                     out == null ? 0.0 : out.targetMs(),
                     (int)(statLate - this.liveLate),
                     (int)Math.max(0L, gaps - this.liveGaps0)
                  );
                  this.liveFrames = 0L;
                  this.liveFill = 0.0;
                  this.liveStart = now;
                  this.liveLate = statLate;
                  this.liveGaps0 = gaps;
               }

               statNanos += now - statLast;
               statLast = now;
               statFrames++;
               if (statNanos >= 10000000000L) {
                  WorkingConsolesCore.LOGGER
                     .info(
                        "{} running at {} FPS, sound queued {} ms (target {}), late frames {}, sound gaps {}, sound overflows {}",
                        new Object[]{
                           this.name,
                           String.format("%.1f", statFrames * 1.0E9 / statNanos),
                           String.format("%.0f", statFill / Math.max(1L, statFrames)),
                           out == null ? 0 : String.format("%.0f", out.targetMs()),
                           statLate,
                           out == null ? 0 : out.underruns,
                           out == null ? 0 : out.overflows
                        }
                     );
                  statFrames = 0L;
                  statNanos = 0L;
                  statFill = 0.0;
                  statLate = 0L;
                  this.liveLate = 0L;
                  this.liveGaps0 = 0L;
                  if (out != null) {
                     out.underruns = out.overflows = 0;
                  }
               }
            }
         }
      } catch (Throwable t) {
         WorkingConsolesCore.LOGGER.error("Emulator thread crashed", t);
      } finally {
         if (out != null) {
            out.close();
         }

         this.core.close();
      }
   }

   private static void applyVolume(byte[] buf, int len, float vol) {
      if (!(vol >= 0.999F)) {
         for (int i = 0; i + 1 < len; i += 2) {
            int s = (short)(buf[i] & 0xFF | buf[i + 1] << 8);
            s = (int)(s * vol);
            buf[i] = (byte)s;
            buf[i + 1] = (byte)(s >> 8);
         }
      }
   }

   private static SourceDataLine openAudio(int rate) {
      try {
         AudioFormat fmt = new AudioFormat(rate, 16, 2, true, false);
         SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
         line.open(fmt, rate / 2 * 4);
         return line;
      } catch (Throwable t) {
         WorkingConsolesCore.LOGGER.warn("Sound output at {} Hz not available: {}", rate, t.getMessage());
         return null;
      }
   }

   private static void sleepNanos(long nanos) {
      long end = System.nanoTime() + nanos;

      try {
         while (true) {
            long left = end - System.nanoTime();
            if (left <= 0L) {
               return;
            }

            if (left > 2000000L) {
               Thread.sleep((left - 1000000L) / 1000000L);
            } else {
               Thread.onSpinWait();
            }
         }
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }

   public interface AudioTap {
      void accept(byte[] var1, int var2, int var3);
   }

   private static final class Resampler {
      private double pos;
      private int lastL;
      private int lastR;

      int process(byte[] in, int inLen, double srcRate, byte[] out) {
         int frames = inLen / 4;
         if (frames == 0) {
            return 0;
         }

         double step = srcRate / 48000.0;
         int o = 0;
         int maxOut = out.length / 4;

         while (this.pos < frames && o < maxOut) {
            int i = (int)this.pos;
            double f = this.pos - i;
            int aL = i == 0 ? this.lastL : sample(in, i - 1, 0);
            int aR = i == 0 ? this.lastR : sample(in, i - 1, 1);
            int bL = sample(in, i, 0);
            int bR = sample(in, i, 1);
            int l = (int)(aL + (bL - aL) * f);
            int r = (int)(aR + (bR - aR) * f);
            out[o * 4] = (byte)l;
            out[o * 4 + 1] = (byte)(l >> 8);
            out[o * 4 + 2] = (byte)r;
            out[o * 4 + 3] = (byte)(r >> 8);
            o++;
            this.pos += step;
         }

         this.pos -= frames;
         this.lastL = sample(in, frames - 1, 0);
         this.lastR = sample(in, frames - 1, 1);
         return o * 4;
      }

      private static int sample(byte[] b, int frame, int ch) {
         int k = frame * 4 + ch * 2;
         return (short)(b[k] & 0xFF | b[k + 1] << 8);
      }
   }

   private static final class Speaker extends AudioSink {
      final SourceDataLine line;
      final int rate;
      final int targetFrames;
      long written;
      long base;
      double smoothMs;

      Speaker(SourceDataLine line, int rate, int targetMs) {
         this.line = line;
         this.rate = rate;
         this.targetFrames = rate * targetMs / 1000;
         this.smoothMs = targetMs;
      }

      @Override
      public int rate() {
         return this.rate;
      }

      long queuedFrames() {
         return Math.max(0L, this.written - (this.line.getLongFramePosition() - this.base));
      }

      @Override
      public double queuedMs() {
         return this.queuedFrames() * 1000.0 / this.rate;
      }

      @Override
      public double targetMs() {
         return this.targetFrames * 1000.0 / this.rate;
      }

      @Override
      public void write(byte[] data, int len) {
         len -= len % 4;
         long room = this.targetFrames * 3L - this.queuedFrames();
         if (room <= 0L) {
            this.overflows++;
         } else {
            if (len / 4 > room) {
               len = (int)room * 4;
               this.overflows++;
            }

            if (len > 0) {
               this.written = this.written + this.line.write(data, 0, len) / 4;
            }
         }
      }

      void silence(long frames) {
         if (frames > 0L) {
            byte[] zero = new byte[(int)Math.min(frames, this.rate) * 4];
            this.write(zero, zero.length);
         }
      }

      @Override
      public void start() {
         this.line.flush();
         this.base = this.line.getLongFramePosition();
         this.written = 0L;
         this.silence(this.targetFrames);
         this.line.start();
         this.smoothMs = this.targetMs();
      }

      @Override
      public void pause() {
         this.line.stop();
         this.line.flush();
      }

      @Override
      public double tick() {
         long q = this.queuedFrames();
         if (q < this.targetFrames / 5) {
            this.silence(this.targetFrames / 2 - q);
            this.underruns++;
            q = this.queuedFrames();
         }

         double ms = q * 1000.0 / this.rate;
         this.smoothMs = this.smoothMs + (ms - this.smoothMs) * 0.03;
         double t = this.targetMs();
         return Math.max(-0.01, Math.min(0.01, (this.smoothMs - t) / t * 0.01));
      }

      @Override
      public void close() {
         this.line.stop();
         this.line.close();
      }
   }

   public record Stats(double fps, double soundMs, double targetMs, int lateFrames, int soundGaps) {
   }
}
