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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class LibretroCore implements EmuCore {
   public static final int B = 1;
   public static final int Y = 2;
   public static final int SELECT = 4;
   public static final int START = 8;
   public static final int UP = 16;
   public static final int DOWN = 32;
   public static final int LEFT = 64;
   public static final int RIGHT = 128;
   public static final int A = 256;
   public static final int X = 512;
   public static final int L = 1024;
   public static final int R = 2048;
   public static final int L2 = 4096;
   public static final int R2 = 8192;
   public static final int L3 = 16384;
   public static final int R3 = 32768;
   private static final int MAX_PIXELS = 1048576;
   public static final int MAX_SLOTS_PER_CORE = 8;
   private static final Map<String, List<LibretroCore.Slot>> SLOTS = new HashMap<>();
   private final LibretroCore.Slot slot;
   private final Arena arena = Arena.ofShared();
   private final MemorySegment video = this.arena.allocate(ValueLayout.JAVA_INT, 1048576L);
   private final MemorySegment size = this.arena.allocate(ValueLayout.JAVA_INT, 2L);
   private final MemorySegment audio = this.arena.allocate(ValueLayout.JAVA_SHORT, 16384L);
   private final double fps;
   private final int rate;
   private final int latencyMs;
   private int keys;
   private boolean closed;
   private boolean direct;
   private int directTexture;
   private int directSerial;
   private int directWidth;
   private int directHeight;

   private LibretroCore(LibretroCore.Slot slot, double fps, int rate, int latencyMs) {
      this.slot = slot;
      this.fps = fps;
      this.rate = rate;
      this.latencyMs = latencyMs;
   }

   public static Path consolesDir() {
      return FabricLoader.getInstance().getGameDir().resolve("consoles");
   }

   private static boolean isDolphin(Path corePath) {
      return corePath.getFileName().toString().toLowerCase(Locale.ROOT).contains("dolphin");
   }

   private static String shimName(Path corePath) {
      String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
      if (os.contains("win")) {
         return isDolphin(corePath) ? "wcore_dolphin.dll" : "wcore_retro.dll";
      }
      if (os.contains("linux")) {
         // The Dolphin shim uses WGL to share Minecraft's OpenGL texture.  Its
         // direct-display path is Windows-only for now; other cores keep using
         // the existing cross-platform shim.
         return isDolphin(corePath) ? null : "libwcore_retro.so";
      }
      return null;
   }

   private static void syncFile(Path target, byte[] bytes) throws IOException {
      try {
         if (!Files.exists(target) || Files.size(target) != bytes.length || !Arrays.equals(Files.readAllBytes(target), bytes)) {
            Files.write(target, bytes);
         }
      } catch (IOException e) {
         if (!Files.exists(target)) {
            throw e;
         }
      }
   }

   private static LibretroCore.Slot newSlot(Path corePath, int n) throws Throwable {
      String shim = shimName(corePath);
      if (shim == null) {
         if (isDolphin(corePath)) {
            throw new IllegalStateException("Dolphin's shared-texture display is only supported on Windows right now.");
         }
         throw new IllegalStateException("Console engines are only built for Windows and Linux right now.");
      }

      Path dir = consolesDir().resolve("engine");
      Files.createDirectories(dir);

      byte[] shimBytes;
      try (InputStream in = LibretroCore.class.getResourceAsStream("/natives/" + shim)) {
         if (in == null) {
            throw new IllegalStateException("The console engine is missing from the mod jar.");
         }

         shimBytes = in.readAllBytes();
      }

      String coreName = corePath.getFileName().toString();
      int dot = shim.lastIndexOf(46);
      Path shimFile = n == 0 && coreName.startsWith("pcsx2")
         ? dir.resolve(shim)
         : dir.resolve(shim.substring(0, dot) + "_" + strip(coreName) + "_" + n + shim.substring(dot));
      syncFile(shimFile, shimBytes);
      Path core = corePath;
      if (n > 0) {
         Path copies = dir.resolve("core_copies");
         Files.createDirectories(copies);
         core = copies.resolve(strip(coreName) + "_" + n + coreName.substring(coreName.lastIndexOf(46)));
         if (!Files.exists(core)
            || Files.size(core) != Files.size(corePath)
            || Files.getLastModifiedTime(core).compareTo(Files.getLastModifiedTime(corePath)) < 0) {
            Files.copy(corePath, core, StandardCopyOption.REPLACE_EXISTING);
         }
      }

      LibretroCore.Slot s = new LibretroCore.Slot(core);
      Linker linker = Linker.nativeLinker();
      SymbolLookup lib = SymbolLookup.libraryLookup(shimFile, Arena.global());
      OfInt I = ValueLayout.JAVA_INT;
      AddressLayout P = ValueLayout.ADDRESS;
      s.hCreate = linker.downcallHandle(lib.find("wr_create").orElseThrow(), FunctionDescriptor.of(I, P, P, P, P, P, P));
      s.hFps = linker.downcallHandle(lib.find("wr_fps").orElseThrow(), FunctionDescriptor.of(ValueLayout.JAVA_DOUBLE));
      s.hRate = linker.downcallHandle(lib.find("wr_audio_rate").orElseThrow(), FunctionDescriptor.of(I));
      s.hInput = linker.downcallHandle(lib.find("wr_set_input").orElseThrow(), FunctionDescriptor.ofVoid(I, I, I, I, I));
      s.hInputPort = lib.find("wr_set_input_port").map(a -> linker.downcallHandle(a, FunctionDescriptor.ofVoid(I, I, I, I, I, I))).orElse(null);
      s.hRun = linker.downcallHandle(lib.find("wr_run_frame").orElseThrow(), FunctionDescriptor.ofVoid());
      s.hSize = linker.downcallHandle(lib.find("wr_video_size").orElseThrow(), FunctionDescriptor.ofVoid(P, P));
      s.hCopy = linker.downcallHandle(lib.find("wr_copy_video").orElseThrow(), FunctionDescriptor.of(I, P, I));
      s.hAudio = linker.downcallHandle(lib.find("wr_read_audio").orElseThrow(), FunctionDescriptor.of(I, P, I));
      s.hDestroy = linker.downcallHandle(lib.find("wr_destroy").orElseThrow(), FunctionDescriptor.ofVoid());
      s.hSetOpts = lib.find("wr_set_options").map(a -> linker.downcallHandle(a, FunctionDescriptor.ofVoid(P))).orElse(null);
      s.hDump = lib.find("wr_dump_options").map(a -> linker.downcallHandle(a, FunctionDescriptor.of(I, P))).orElse(null);
      s.hSetSharedTexture = lib.find("wr_set_shared_texture")
         .map(a -> linker.downcallHandle(a, FunctionDescriptor.ofVoid(I, I, I)))
         .orElse(null);
      s.hSharedTextureOk = lib.find("wr_shared_texture_ok")
         .map(a -> linker.downcallHandle(a, FunctionDescriptor.of(I)))
         .orElse(null);
      s.hVideoTexture = lib.find("wr_video_texture")
         .map(a -> linker.downcallHandle(a, FunctionDescriptor.of(I)))
         .orElse(null);
      s.hFrameSerial = lib.find("wr_frame_serial")
         .map(a -> linker.downcallHandle(a, FunctionDescriptor.of(I)))
         .orElse(null);
      s.hTextureSize = lib.find("wr_texture_size")
         .map(a -> linker.downcallHandle(a, FunctionDescriptor.ofVoid(P, P)))
         .orElse(null);
      WorkingConsolesCore.LOGGER.info("Console engine slot {} for {} loaded from {}", new Object[]{n, coreName, shimFile.getFileName()});
      return s;
   }

   private static String strip(String file) {
      int dot = file.lastIndexOf(46);
      return dot > 0 ? file.substring(0, dot) : file;
   }

   public static Object open(Path corePath, Path game, Path systemDir, Path saveDir, Map<String, String> options, Path log, DumpCheck.Pass pass) {
      return open(corePath, game, systemDir, saveDir, options, log, 100, pass, 0, 0, 0);
   }

   public static Object open(
      Path corePath, Path game, Path systemDir, Path saveDir, Map<String, String> options, Path log, int latencyMs, DumpCheck.Pass pass
   ) {
      return open(corePath, game, systemDir, saveDir, options, log, latencyMs, pass, 0, 0, 0);
   }

   /**
    * Opens a core and, when {@code sharedTexture} is non-zero, asks the shim to
    * render into Minecraft's existing OpenGL texture. The texture must have
    * been created on the current Minecraft render thread and the current GL
    * context must be Minecraft's. The normal CPU-copy path is unchanged when
    * no shared texture is supplied.
    */
   public static synchronized Object open(
      Path corePath,
      Path game,
      Path systemDir,
      Path saveDir,
      Map<String, String> options,
      Path log,
      int latencyMs,
      DumpCheck.Pass pass,
      int sharedTexture,
      int sharedWidth,
      int sharedHeight
   ) {
      if (!DumpCheck.verify(pass, game)) {
         return "This game didn't go through the own-dump check, so the console won't start it.";
      }

      if (!Files.isRegularFile(corePath)) {
         return "The emulator core is missing: " + corePath.getFileName();
      }

      String key = corePath.toAbsolutePath().toString();
      List<LibretroCore.Slot> slots = SLOTS.computeIfAbsent(key, k -> new ArrayList<>());
      LibretroCore.Slot slot = null;

      for (LibretroCore.Slot s : slots) {
         if (!s.busy) {
            slot = s;
            break;
         }
      }

      try {
         if (slot == null) {
            if (slots.size() >= 8) {
               return "Too many " + strip(corePath.getFileName().toString()) + " games running at once (max 8)";
            }

            slot = newSlot(corePath, slots.size());
            slots.add(slot);
         }
      } catch (Throwable t) {
         WorkingConsolesCore.LOGGER.error("Couldn't load the console engine", t);
         return "Couldn't load the console engine: " + t.getMessage();
      }

      try (Arena tmp = Arena.ofConfined()) {
         Files.createDirectories(systemDir);
         Files.createDirectories(saveDir);
         if (log.getParent() != null) {
            Files.createDirectories(log.getParent());
         }

         StringBuilder opts = new StringBuilder();
         options.forEach((k, v) -> opts.append(k).append('=').append(v).append('\n'));
         if (sharedTexture > 0) {
            if (slot.hSetSharedTexture == null) {
               return "This console engine can't draw directly into a TV texture.";
            }
            slot.hSetSharedTexture.invokeExact(sharedTexture, sharedWidth, sharedHeight);
         }
         int code = (int)slot.hCreate
            .invokeExact(
               (MemorySegment)tmp.allocateFrom(slot.core.toAbsolutePath().toString()),
               (MemorySegment)tmp.allocateFrom(game == null ? "" : game.toAbsolutePath().toString()),
               (MemorySegment)tmp.allocateFrom(systemDir.toAbsolutePath().toString()),
               (MemorySegment)tmp.allocateFrom(saveDir.toAbsolutePath().toString()),
               (MemorySegment)tmp.allocateFrom(opts.toString()),
               (MemorySegment)tmp.allocateFrom(log.toAbsolutePath().toString())
            );
         if (code != 0) {
            return switch (code) {
               case 1 -> "The emulator core couldn't be loaded (see " + log.getFileName() + ")";
               case 2 -> "That file isn't an emulator core";
               case 3 -> "The emulator couldn't start that game (see " + log.getFileName() + ")";
               default -> "That emulator slot is busy";
            };
         } else {
            slot.busy = true;
            LibretroCore core = new LibretroCore(slot, (double)slot.hFps.invokeExact(), (int)slot.hRate.invokeExact(), latencyMs);
            if (slot.hSharedTextureOk != null && (int)slot.hSharedTextureOk.invokeExact() == 1) {
               core.direct = true;
               if (slot.hVideoTexture != null) {
                  core.directTexture = (int)slot.hVideoTexture.invokeExact();
               }
               MemorySegment wh = tmp.allocate(ValueLayout.JAVA_INT, 2L);
               slot.hSize.invokeExact(wh, wh.asSlice(4L));
               core.directWidth = wh.get(ValueLayout.JAVA_INT, 0L);
               core.directHeight = wh.get(ValueLayout.JAVA_INT, 4L);
            }
            if (sharedTexture > 0 && !core.direct) {
               core.close();
               return "The emulator couldn't share Minecraft's TV texture (OpenGL renderer required).";
            }
            dumpOptions(slot, tmp, corePath);
            return core;
         }
      } catch (Throwable t) {
         WorkingConsolesCore.LOGGER.error("Console engine failed to start a game", t);
         return "Engine error: " + t.getMessage();
      }
   }

   private static void dumpOptions(LibretroCore.Slot slot, Arena tmp, Path corePath) {
      if (slot.hDump != null) {
         try {
            String name = strip(corePath.getFileName().toString());
            Path out = CoreOptions.liveFile(name);
            int n = (int)slot.hDump.invokeExact((MemorySegment)tmp.allocateFrom(out.toAbsolutePath().toString()));
            if (n > 0) {
               CoreOptions.invalidate(name);
            }
         } catch (Throwable t) {
            WorkingConsolesCore.LOGGER.warn("Couldn't save the core's settings list: {}", t.toString());
         }
      }
   }

   @Override
   public void applyOptions(Map<String, String> options) {
      if (this.slot.hSetOpts != null && !this.closed) {
         StringBuilder opts = new StringBuilder();
         options.forEach((k, v) -> opts.append(k).append('=').append(v).append('\n'));

         try (Arena tmp = Arena.ofConfined()) {
            this.slot.hSetOpts.invokeExact((MemorySegment)tmp.allocateFrom(opts.toString()));
         } catch (Throwable t) {
            WorkingConsolesCore.LOGGER.warn("Couldn't change emulator settings: {}", t.toString());
         }
      }
   }

   @Override
   public void setKeys(int keys) {
      this.keys = keys;
   }

   @Override
   public void setAnalog(int lx, int ly, int rx, int ry) {
      if (this.closed) {
         return;
      }
      try {
         this.slot.hInput.invokeExact((int)this.keys, (int)lx, (int)ly, (int)rx, (int)ry);
      } catch (Throwable var6) {
      }
   }

   @Override
   public void setPort2(int keys, int lx, int ly, int rx, int ry) {
      if (this.slot.hInputPort != null && !this.closed) {
         try {
            this.slot.hInputPort.invokeExact((int)1, (int)keys, (int)lx, (int)ly, (int)rx, (int)ry);
         } catch (Throwable var7) {
         }
      }
   }

   @Override
   public boolean directVideo() {
      return this.direct;
   }

   @Override
   public int videoSerial() {
      if (!this.direct || this.closed || this.slot.hFrameSerial == null) {
         return 0;
      }

      try {
         int serial = (int)this.slot.hFrameSerial.invokeExact();
         if (serial != this.directSerial) {
            this.directSerial = serial;
            try (Arena tmp = Arena.ofConfined()) {
               MemorySegment wh = tmp.allocate(ValueLayout.JAVA_INT, 2L);
               this.slot.hSize.invokeExact(wh, wh.asSlice(4L));
               int w = wh.get(ValueLayout.JAVA_INT, 0L);
               int h = wh.get(ValueLayout.JAVA_INT, 4L);
               if (w > 0 && h > 0) {
                  this.directWidth = w;
                  this.directHeight = h;
               }
            }
         }
         return serial;
      } catch (Throwable t) {
         return this.directSerial;
      }
   }

   @Override
   public int videoWidth() {
      return this.directWidth;
   }

   @Override
   public int videoHeight() {
      return this.directHeight;
   }

   @Override
   public void runFrame() {
      if (this.closed) {
         return;
      }
      try {
         this.slot.hRun.invokeExact();
      } catch (Throwable var2) {
      }
   }

   @Override
   public int[] copyVideo(int[] dst) {
      if (this.direct || this.closed) {
         return new int[]{0, 0};
      }

      try {
         int n = (int)this.slot.hCopy.invokeExact((MemorySegment)this.video, (int)Math.min(1048576, dst.length));
         if (n <= 0) {
            return new int[]{0, 0};
         }

         this.slot.hSize.invokeExact((MemorySegment)this.size, (MemorySegment)this.size.asSlice(4L));
         MemorySegment.copy(this.video, ValueLayout.JAVA_INT, 0L, dst, 0, n);
         return new int[]{this.size.get(ValueLayout.JAVA_INT, 0L), this.size.get(ValueLayout.JAVA_INT, 4L)};
      } catch (Throwable t) {
         return new int[]{0, 0};
      }
   }

   @Override
   public int audioRate() {
      return this.rate;
   }

   @Override
   public int readAudio(byte[] dst) {
      if (this.closed) {
         return 0;
      }
      try {
         int frames = (int)this.slot.hAudio.invokeExact((MemorySegment)this.audio, (int)Math.min(8192, dst.length / 4));
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
   public double fps() {
      return this.fps;
   }

   @Override
   public int audioLatencyMs() {
      return this.latencyMs;
   }

   @Override
   public int maxPixels() {
      return 1048576;
   }

   @Override
   public synchronized void close() {
      if (!this.closed) {
         this.closed = true;

         try {
            this.slot.hDestroy.invokeExact();
         } catch (Throwable var4) {
         }

         synchronized (LibretroCore.class) {
            this.slot.busy = false;
         }
      }
   }

   private static final class Slot {
      final Path core;
      MethodHandle hCreate;
      MethodHandle hFps;
      MethodHandle hRate;
      MethodHandle hInput;
      MethodHandle hInputPort;
      MethodHandle hRun;
      MethodHandle hSize;
      MethodHandle hCopy;
      MethodHandle hAudio;
      MethodHandle hDestroy;
      MethodHandle hDump;
      MethodHandle hSetOpts;
      MethodHandle hSetSharedTexture;
      MethodHandle hSharedTextureOk;
      MethodHandle hVideoTexture;
      MethodHandle hFrameSerial;
      MethodHandle hTextureSize;
      boolean busy;

      Slot(Path core) {
         this.core = core;
      }
   }
}
