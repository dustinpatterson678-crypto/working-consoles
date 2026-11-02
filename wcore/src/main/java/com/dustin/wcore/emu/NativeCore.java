package com.dustin.wcore.emu;

import com.dustin.wcore.WorkingConsolesCore;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.foreign.ValueLayout.OfInt;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;

public final class NativeCore implements EmuCore {
   public static final int KEY_A = 1;
   public static final int KEY_B = 2;
   public static final int KEY_SELECT = 4;
   public static final int KEY_START = 8;
   public static final int KEY_RIGHT = 16;
   public static final int KEY_LEFT = 32;
   public static final int KEY_UP = 64;
   public static final int KEY_DOWN = 128;
   public static final int KEY_R = 256;
   public static final int KEY_L = 512;
   public static final int MAX_W = 256;
   public static final int MAX_H = 224;
   private static volatile boolean loaded;
   private static String loadError;
   private static MethodHandle hCreate;
   private static MethodHandle hDestroy;
   private static MethodHandle hRunFrame;
   private static MethodHandle hSetKeys;
   private static MethodHandle hVideoSize;
   private static MethodHandle hCopyVideo;
   private static MethodHandle hAudioRate;
   private static MethodHandle hReadAudio;
   private static MethodHandle hPlatform;
   private static MethodHandle hLinkEnable;
   private static MethodHandle hLinkTake;
   private static MethodHandle hLinkFeed;
   private static MethodHandle hLinkWaiting;
   private static MethodHandle hLinkAbort;
   private final Arena arena = Arena.ofShared();
   private final MemorySegment handle;
   private final MemorySegment video = this.arena.allocate(ValueLayout.JAVA_INT, 57344L);
   private final MemorySegment size = this.arena.allocate(ValueLayout.JAVA_INT, 2L);
   private final MemorySegment audio = this.arena.allocate(ValueLayout.JAVA_SHORT, 8192L);
   private boolean closed;
   private final MemorySegment linkBuf = this.arena.allocate(4096L);
   private final ConcurrentLinkedQueue<byte[]> linkIn = new ConcurrentLinkedQueue<>();
   private volatile int linkWanted = -1;
   private volatile int linkActive = -1;
   private volatile Consumer<byte[]> linkOut = b -> {};

   private NativeCore(MemorySegment handle) {
      this.handle = handle;
   }

   public static boolean linkSupported() {
      return hLinkEnable != null;
   }

   public void linkConnect(int role, Consumer<byte[]> out) {
      this.linkOut = out == null ? b -> {} : out;
      this.linkIn.clear();
      this.linkWanted = role;
   }

   public void linkDisconnect() {
      this.linkWanted = -1;
      this.linkIn.clear();
   }

   public void linkReceive(byte[] data) {
      if (data != null && data.length > 0) {
         this.linkIn.add(data);
      }
   }

   @Override
   public void linkPoll() {
      if (hLinkEnable != null && !this.closed) {
         try {
            if (this.linkWanted != this.linkActive) {
               hLinkEnable.invokeExact((MemorySegment)this.handle, (int)this.linkWanted);
               this.linkActive = this.linkWanted;
            }

            if (this.linkActive < 0) {
               return;
            }

            byte[] in;
            while ((in = this.linkIn.poll()) != null) {
               for (int off = 0; off < in.length; off += 4096) {
                  int n = Math.min(4096, in.length - off);
                  MemorySegment.copy(in, off, this.linkBuf, ValueLayout.JAVA_BYTE, 0L, n);
                  hLinkFeed.invokeExact((MemorySegment)this.handle, (MemorySegment)this.linkBuf, (int)n);
               }
            }

            int n = (int)hLinkTake.invokeExact((MemorySegment)this.handle, (MemorySegment)this.linkBuf, (int)4096);
            if (n > 0) {
               byte[] out = new byte[n];
               MemorySegment.copy(this.linkBuf, ValueLayout.JAVA_BYTE, 0L, out, 0, n);
               this.linkOut.accept(out);
            }
         } catch (Throwable t) {
            WorkingConsolesCore.LOGGER.warn("Link cable error, unplugging: {}", t.toString());
            this.linkWanted = -1;
         }
      }
   }

   @Override
   public boolean linkBlocked() {
      try {
         return hLinkWaiting != null && this.linkActive >= 0 && !this.closed && (int)hLinkWaiting.invokeExact((MemorySegment)this.handle) != 0;
      } catch (Throwable t) {
         return false;
      }
   }

   @Override
   public void linkAbort() {
      try {
         if (hLinkAbort != null && this.linkActive >= 0 && !this.closed) {
            hLinkAbort.invokeExact((MemorySegment)this.handle);
         }
      } catch (Throwable var2) {
      }
   }

   public static synchronized String ensureLoaded() {
      if (loaded) {
         return null;
      }

      if (loadError != null) {
         return loadError;
      }

      try {
         String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
         String file = os.contains("win") ? "gbsp_core.dll" : (os.contains("linux") ? "libgbsp_core.so" : null);
         if (file == null) {
            loadError = "The Game Boy engine is only built for Windows and Linux right now.";
            return "The Game Boy engine is only built for Windows and Linux right now.";
         }

         Path dir = FabricLoader.getInstance().getGameDir().resolve("consoles").resolve("engine");
         Files.createDirectories(dir);
         Path target = dir.resolve(file);

         try (InputStream in = NativeCore.class.getResourceAsStream("/natives/" + file)) {
            if (in == null) {
               loadError = "The engine file is missing from the mod jar.";
               return "The engine file is missing from the mod jar.";
            }

            byte[] bytes = in.readAllBytes();
            if (!Files.exists(target) || Files.size(target) != bytes.length || !Arrays.equals(Files.readAllBytes(target), bytes)) {
               Files.write(target, bytes);
            }
         } catch (IOException e) {
            if (!Files.exists(target)) {
               throw e;
            }
         }

         Linker linker = Linker.nativeLinker();
         SymbolLookup lib = SymbolLookup.libraryLookup(target, Arena.global());
         OfInt I = ValueLayout.JAVA_INT;
         AddressLayout P = ValueLayout.ADDRESS;
         hCreate = linker.downcallHandle(lib.find("gbsp_create").orElseThrow(), FunctionDescriptor.of(P, P, P, P, P));
         hDestroy = linker.downcallHandle(lib.find("gbsp_destroy").orElseThrow(), FunctionDescriptor.ofVoid(P));
         hRunFrame = linker.downcallHandle(lib.find("gbsp_run_frame").orElseThrow(), FunctionDescriptor.ofVoid(P));
         hSetKeys = linker.downcallHandle(lib.find("gbsp_set_keys").orElseThrow(), FunctionDescriptor.ofVoid(P, I));
         hVideoSize = linker.downcallHandle(lib.find("gbsp_video_size").orElseThrow(), FunctionDescriptor.ofVoid(P, P, P));
         hCopyVideo = linker.downcallHandle(lib.find("gbsp_copy_video").orElseThrow(), FunctionDescriptor.of(I, P, P, I));
         hAudioRate = linker.downcallHandle(lib.find("gbsp_audio_rate").orElseThrow(), FunctionDescriptor.of(I, P));
         hReadAudio = linker.downcallHandle(lib.find("gbsp_read_audio").orElseThrow(), FunctionDescriptor.of(I, P, P, I));
         hPlatform = linker.downcallHandle(lib.find("gbsp_platform").orElseThrow(), FunctionDescriptor.of(I, P));
         hLinkEnable = lib.find("gbsp_link_enable").map(a -> linker.downcallHandle(a, FunctionDescriptor.ofVoid(P, I))).orElse(null);
         hLinkTake = lib.find("gbsp_link_take").map(a -> linker.downcallHandle(a, FunctionDescriptor.of(I, P, P, I))).orElse(null);
         hLinkFeed = lib.find("gbsp_link_feed").map(a -> linker.downcallHandle(a, FunctionDescriptor.ofVoid(P, P, I))).orElse(null);
         hLinkWaiting = lib.find("gbsp_link_waiting").map(a -> linker.downcallHandle(a, FunctionDescriptor.of(I, P))).orElse(null);
         hLinkAbort = lib.find("gbsp_link_abort").map(a -> linker.downcallHandle(a, FunctionDescriptor.ofVoid(P))).orElse(null);
         loaded = true;
         WorkingConsolesCore.LOGGER.info("Game Boy engine (mGBA) loaded from {}{}", target, hLinkEnable != null ? " (link cable ready)" : "");
         return null;
      } catch (Throwable t) {
         WorkingConsolesCore.LOGGER.error("Couldn't load the Game Boy engine", t);
         return loadError = "Couldn't load the Game Boy engine: " + t.getMessage();
      }
   }

   public static Object open(Path rom, Path bios, Path save, DumpCheck.Pass pass) {
      if (!DumpCheck.verify(pass, rom)) {
         return "This game didn't go through the own-dump check, so the console won't start it.";
      }

      String err = ensureLoaded();
      if (err != null) {
         return err;
      }

      try (Arena tmp = Arena.ofConfined()) {
         MemorySegment errOut = tmp.allocate(ValueLayout.JAVA_INT);
         MemorySegment h = (MemorySegment)hCreate.invokeExact(
            (MemorySegment)tmp.allocateFrom(rom.toAbsolutePath().toString()),
            (MemorySegment)tmp.allocateFrom(bios == null ? "" : bios.toAbsolutePath().toString()),
            (MemorySegment)tmp.allocateFrom(save == null ? "" : save.toAbsolutePath().toString()),
            (MemorySegment)errOut
         );
         if (h.equals(MemorySegment.NULL)) {
            return switch (errOut.get(ValueLayout.JAVA_INT, 0L)) {
               case 1 -> "Couldn't open the game file.";
               case 2 -> "That file isn't a Game Boy / GBA game.";
               case 3 -> "No BIOS: the handheld won't turn on without one.";
               case 4 -> "The BIOS file isn't a valid BIOS.";
               case 5 -> "The engine couldn't load that game.";
               default -> "The engine ran out of memory.";
            };
         } else {
            return new NativeCore(h);
         }
      } catch (Throwable t) {
         return "Engine error: " + t.getMessage();
      }
   }

   @Override
   public double fps() {
      return 59.72750056960583;
   }

   @Override
   public int maxPixels() {
      return 57344;
   }

   public boolean isGba() {
      try {
         return (int)hPlatform.invokeExact((MemorySegment)this.handle) == 0;
      } catch (Throwable t) {
         return true;
      }
   }

   @Override
   public void setKeys(int keys) {
      try {
         hSetKeys.invokeExact((MemorySegment)this.handle, (int)keys);
      } catch (Throwable var3) {
      }
   }

   @Override
   public void runFrame() {
      try {
         hRunFrame.invokeExact((MemorySegment)this.handle);
      } catch (Throwable var2) {
      }
   }

   @Override
   public int[] copyVideo(int[] dst) {
      try {
         hVideoSize.invokeExact((MemorySegment)this.handle, (MemorySegment)this.size, (MemorySegment)this.size.asSlice(4L));
         int w = this.size.get(ValueLayout.JAVA_INT, 0L);
         int h = this.size.get(ValueLayout.JAVA_INT, 4L);
         int n = (int)hCopyVideo.invokeExact((MemorySegment)this.handle, (MemorySegment)this.video, (int)57344);
         if (n > 0) {
            MemorySegment.copy(this.video, ValueLayout.JAVA_INT, 0L, dst, 0, Math.min(n, dst.length));
         }

         return new int[]{w, h};
      } catch (Throwable t) {
         return new int[]{0, 0};
      }
   }

   @Override
   public int audioRate() {
      try {
         return (int)hAudioRate.invokeExact((MemorySegment)this.handle);
      } catch (Throwable t) {
         return 32768;
      }
   }

   @Override
   public int readAudio(byte[] dst) {
      try {
         int maxFrames = Math.min(4096, dst.length / 4);
         int frames = (int)hReadAudio.invokeExact((MemorySegment)this.handle, (MemorySegment)this.audio, (int)maxFrames);
         if (frames <= 0) {
            return 0;
         }

         MemorySegment.copy(this.audio, ValueLayout.JAVA_BYTE, 0L, dst, 0, frames * 4);
         return frames * 4;
      } catch (Throwable t) {
         return 0;
      }
   }

   @Override
   public synchronized void close() {
      if (!this.closed) {
         this.closed = true;

         try {
            hDestroy.invokeExact((MemorySegment)this.handle);
         } catch (Throwable var2) {
         }

         this.arena.close();
      }
   }
}
