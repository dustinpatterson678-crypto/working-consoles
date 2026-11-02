package com.dustin.wcore.client;

import com.dustin.wcore.emu.AudioSink;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.sound.sampled.AudioFormat;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class McAudioSink extends AudioSink {
   private static final int RATE = 48000;
   private static final SoundEvent EVENT = SoundEvent.createVariableRangeEvent(FabricSoundInstance.EMPTY_SOUND);
   private final boolean positional;
   private final int channels;
   private final Supplier<Vec3> where;
   private final float range;
   private final int targetFrames;
   private final short[] ring;
   private int readPos;
   private int writePos;
   private int fill;
   private double smoothMs;
   private volatile boolean closed;
   private McAudioSink.Instance instance;
   private long lastCheck;

   public McAudioSink(int targetMs, Supplier<Vec3> where, float range) {
      this.positional = where != null;
      this.channels = this.positional ? 1 : 2;
      this.where = where;
      this.range = range;
      this.targetFrames = 48000 * Math.max(60, targetMs) / 1000;
      this.ring = new short[48000 * this.channels];
      this.smoothMs = this.targetMs();
   }

   public static AudioSink.Factory world(Supplier<Vec3> where, float range) {
      return (rate, ms) -> new McAudioSink(ms, where, range);
   }

   public static AudioSink.Factory ears() {
      return (rate, ms) -> new McAudioSink(ms, null, 16.0F);
   }

   @Override
   public int rate() {
      return 48000;
   }

   private synchronized int frames() {
      return this.fill / this.channels;
   }

   @Override
   public double queuedMs() {
      return this.frames() * 1000.0 / 48000.0;
   }

   @Override
   public double targetMs() {
      return this.targetFrames * 1000.0 / 48000.0;
   }

   @Override
   public void start() {
      synchronized (this) {
         this.readPos = this.writePos = this.fill = 0;
      }

      this.silence(this.targetFrames);
      this.smoothMs = this.targetMs();
      this.ensurePlaying(true);
   }

   @Override
   public void pause() {
      synchronized (this) {
         this.readPos = this.writePos = this.fill = 0;
      }
   }

   private void ensurePlaying(boolean now) {
      long t = System.nanoTime();
      if (now || t - this.lastCheck >= 1000000000L) {
         this.lastCheck = t;
         Minecraft mc = Minecraft.getInstance();
         mc.execute(() -> {
            if (!this.closed) {
               if (this.instance == null || this.instance.isStopped() || !mc.getSoundManager().isActive(this.instance)) {
                  this.instance = new McAudioSink.Instance();
                  mc.getSoundManager().play(this.instance);
               }
            }
         });
      }
   }

   @Override
   public void write(byte[] data, int len) {
      int frames = len / 4;
      synchronized (this) {
         int room = this.targetFrames * 3 - this.fill / this.channels;
         if (room <= 0) {
            this.overflows++;
         } else {
            if (frames > room) {
               frames = room;
               this.overflows++;
            }

            for (int i = 0; i < frames; i++) {
               short l = (short)(data[i * 4] & 0xFF | data[i * 4 + 1] << 8);
               short r = (short)(data[i * 4 + 2] & 0xFF | data[i * 4 + 3] << 8);
               if (this.channels == 1) {
                  this.put((short)((l + r) / 2));
               } else {
                  this.put(l);
                  this.put(r);
               }
            }
         }
      }
   }

   private void put(short s) {
      this.ring[this.writePos] = s;
      this.writePos = (this.writePos + 1) % this.ring.length;
      this.fill++;
   }

   private void silence(int frames) {
      synchronized (this) {
         int n = Math.min(frames * this.channels, this.ring.length - this.fill);

         for (int i = 0; i < n; i++) {
            this.put((short)0);
         }
      }
   }

   @Override
   public double tick() {
      this.ensurePlaying(false);
      int q = this.frames();
      if (q < this.targetFrames / 5) {
         this.silence(this.targetFrames / 2 - q);
         this.underruns++;
         q = this.frames();
      }

      double ms = q * 1000.0 / 48000.0;
      this.smoothMs = this.smoothMs + (ms - this.smoothMs) * 0.03;
      double t = this.targetMs();
      return Math.max(-0.01, Math.min(0.01, (this.smoothMs - t) / t * 0.01));
   }

   private ByteBuffer take() {
      int min = 1920;
      int max = 3840;
      ByteBuffer out;
      synchronized (this) {
         int frames = Math.max(min, Math.min(max, this.fill / this.channels));
         out = ByteBuffer.allocateDirect(frames * this.channels * 2).order(ByteOrder.LITTLE_ENDIAN);
         int have = Math.min(frames * this.channels, this.fill);

         for (int i = 0; i < have; i++) {
            out.putShort(this.ring[this.readPos]);
            this.readPos = (this.readPos + 1) % this.ring.length;
         }

         this.fill -= have;

         while (out.hasRemaining()) {
            out.putShort((short)0);
         }
      }

      out.flip();
      return out;
   }

   @Override
   public void close() {
      this.closed = true;
      Minecraft mc = Minecraft.getInstance();
      mc.execute(() -> {
         if (this.instance != null) {
            mc.getSoundManager().stop(this.instance);
         }
      });
   }

   private final class Instance extends AbstractTickableSoundInstance implements FabricSoundInstance {
      Instance() {
         super(McAudioSink.EVENT, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
         this.looping = false;
         this.delay = 0;
         this.volume = Math.max(1.0F, McAudioSink.this.range / 16.0F);
         this.attenuation = McAudioSink.this.positional ? Attenuation.LINEAR : Attenuation.NONE;
         this.relative = !McAudioSink.this.positional;
         this.move();
      }

      private void move() {
         Vec3 p = McAudioSink.this.where == null ? Vec3.ZERO : McAudioSink.this.where.get();
         if (p != null) {
            this.x = p.x;
            this.y = p.y;
            this.z = p.z;
         }
      }

      public void tick() {
         if (McAudioSink.this.closed) {
            this.stop();
         } else if (McAudioSink.this.positional) {
            this.move();
         }
      }

      public boolean canStartSilent() {
         return true;
      }

      public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, Identifier id, boolean repeatInstantly) {
         return CompletableFuture.completedFuture(new AudioStream() {
            private final AudioFormat format = new AudioFormat(48000.0F, 16, McAudioSink.this.channels, true, false);

            public AudioFormat getFormat() {
               return this.format;
            }

            public ByteBuffer read(int size) {
               return McAudioSink.this.closed ? null : McAudioSink.this.take();
            }

            public void close() {
            }
         });
      }
   }
}
