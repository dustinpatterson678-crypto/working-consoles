package com.dustin.wcore.client;

import com.dustin.wcore.ConsoleSettings;
import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.emu.EmuThread;
import com.dustin.wcore.net.StreamRelay;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class ScreenShare {
   private static final long FRAME_NANOS = 33000000L;
   private static final long PLAYOUT_MS = 120L;
   private static final Map<BlockPos, UUID> HOSTS = new ConcurrentHashMap<>();
   private static final Map<BlockPos, Integer> VIEWERS = new ConcurrentHashMap<>();
   private static final Map<BlockPos, Long> CLAIMED_AT = new HashMap<>();
   private static final Map<BlockPos, Boolean> CLAIMED = new HashMap<>();
   private static final Map<BlockPos, Long> LAST_SENT = new HashMap<>();
   private static final Map<BlockPos, ScreenShare.View> VIEWS = new ConcurrentHashMap<>();
   private static final ExecutorService CODER = Executors.newFixedThreadPool(2, r -> {
      Thread t = new Thread(r, "WC-ScreenShare");
      t.setDaemon(true);
      return t;
   });
   private static final AtomicBoolean ENCODING = new AtomicBoolean();
   private static int frameId;

   private ScreenShare() {
   }

   public static void init() {
      ClientPlayNetworking.registerGlobalReceiver(StreamRelay.Host.TYPE, (p, ctx) -> {
         BlockPos pos = p.pos().immutable();
         if (p.host() == null) {
            HOSTS.remove(pos);
         } else {
            HOSTS.put(pos, p.host());
         }

         VIEWERS.put(pos, p.viewers());
         if (isMe(p.host())) {
            drop(pos);
         }
      });
      ClientPlayNetworking.registerGlobalReceiver(StreamRelay.Frame.TYPE, (p, ctx) -> frame(p));
      ClientPlayNetworking.registerGlobalReceiver(StreamRelay.Audio.TYPE, (p, ctx) -> audio(p));
      ClientTickEvents.END_CLIENT_TICK.register(ScreenShare::tick);
   }

   private static boolean isMe(UUID id) {
      LocalPlayer p = Minecraft.getInstance().player;
      return id != null && p != null && id.equals(p.getUUID());
   }

   public static boolean active() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getConnection() == null || !ConsoleSettings.get().shareScreen) {
         return false;
      } else {
         return mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null && !mc.getSingleplayerServer().isPublished()
            ? false
            : ClientPlayNetworking.canSend(StreamRelay.Claim.TYPE);
      }
   }

   public static ScreenShare.Role role(BlockPos pos, boolean canHost) {
      if (!active()) {
         return canHost ? ScreenShare.Role.HOST : ScreenShare.Role.WAITING;
      }

      long now = System.nanoTime();
      Long at = CLAIMED_AT.get(pos);
      Boolean was = CLAIMED.get(pos);
      if (at == null || now - at > 1000000000L || was == null || was != canHost) {
         CLAIMED_AT.put(pos.immutable(), now);
         CLAIMED.put(pos.immutable(), canHost);
         ClientPlayNetworking.send(new StreamRelay.Claim(pos, canHost));
      }

      UUID h = HOSTS.get(pos);
      if (h == null) {
         return ScreenShare.Role.WAITING;
      } else {
         return isMe(h) ? ScreenShare.Role.HOST : ScreenShare.Role.VIEWER;
      }
   }

   public static boolean watching(BlockPos pos) {
      ScreenShare.View v = VIEWS.get(pos);
      return v != null && v.texture != null;
   }

   public static Set<BlockPos> viewing() {
      return VIEWS.keySet();
   }

   private static boolean anyoneWatching(BlockPos pos) {
      return active() && isMe(HOSTS.get(pos)) && VIEWERS.getOrDefault(pos, 0) > 0;
   }

   public static void sendFrame(BlockPos pos, int[] abgr, int w, int h) {
      if (abgr != null && w > 0 && h > 0 && anyoneWatching(pos)) {
         long now = System.nanoTime();
         Long last = LAST_SENT.get(pos);
         if ((last == null || now - last >= 33000000L) && ENCODING.compareAndSet(false, true)) {
            LAST_SENT.put(pos.immutable(), now);
            int th = Math.min(h, ConsoleSettings.get().shareQuality);
            int tw = Math.max(1, Math.round((float)w * th / h));
            if (tw > th * 2) {
               tw = th * 2;
            }

            BufferedImage img = new BufferedImage(tw, th, 1);
            int[] row = new int[tw];

            for (int y = 0; y < th; y++) {
               int sy = y * h / th;

               for (int x = 0; x < tw; x++) {
                  int p = abgr[sy * w + x * w / tw];
                  row[x] = (p & 0xFF) << 16 | p & 0xFF00 | p >> 16 & 0xFF;
               }

               img.setRGB(0, y, tw, 1, row, 0, tw);
            }

            int id = ++frameId;
            long time = System.currentTimeMillis();
            BlockPos at = pos.immutable();
            int fw = tw;
            int fh = th;
            CODER.execute(() -> {
               try {
                  byte[] jpeg = jpeg(img, 0.6F);
                  int parts = (jpeg.length + 30000 - 1) / 30000;
                  if (parts <= 8) {
                     Minecraft.getInstance().execute(() -> {
                        if (ClientPlayNetworking.canSend(StreamRelay.Frame.TYPE)) {
                           for (int i = 0; i < parts; i++) {
                              int from = i * 30000;
                              int len = Math.min(30000, jpeg.length - from);
                              byte[] chunk = Arrays.copyOfRange(jpeg, from, from + len);
                              ClientPlayNetworking.send(new StreamRelay.Frame(at, id, i, parts, fw, fh, time, chunk));
                           }
                        }
                     });
                     return;
                  }
               } catch (Exception e) {
                  WorkingConsolesCore.LOGGER.debug("Screen share: couldn't compress a picture", e);
                  return;
               } finally {
                  ENCODING.set(false);
               }
            });
         }
      }
   }

   private static byte[] jpeg(BufferedImage img, float quality) throws IOException {
      ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
      ByteArrayOutputStream out = new ByteArrayOutputStream(16384);

      try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
         writer.setOutput(ios);
         ImageWriteParam param = writer.getDefaultWriteParam();
         param.setCompressionMode(2);
         param.setCompressionQuality(quality);
         writer.write(null, new IIOImage(img, null, null), param);
      } finally {
         writer.dispose();
      }

      return out.toByteArray();
   }

   public static EmuThread.AudioTap audioTap(BlockPos pos) {
      final BlockPos at = pos.immutable();
      return new EmuThread.AudioTap() {
         private final ByteArrayOutputStream buf = new ByteArrayOutputStream(4096);
         private double phase;

         @Override
         public void accept(byte[] data, int len, int rate) {
            if (!ScreenShare.anyoneWatching(at)) {
               this.buf.reset();
            } else {
               int out = Math.min(rate, 24000);
               double step = (double)out / rate;

               for (int i = 0; i + 3 < len; i += 4) {
                  this.phase += step;
                  if (!(this.phase < 1.0)) {
                     this.phase--;
                     int l = (short)(data[i] & 0xFF | data[i + 1] << 8);
                     int r = (short)(data[i + 2] & 0xFF | data[i + 3] << 8);
                     int m = (l + r) / 2;
                     this.buf.write(m & 0xFF);
                     this.buf.write(m >> 8 & 0xFF);
                  }
               }

               if (this.buf.size() >= out / 20 * 2) {
                  byte[] pcm = this.buf.toByteArray();
                  this.buf.reset();
                  Minecraft.getInstance().execute(() -> {
                     if (ClientPlayNetworking.canSend(StreamRelay.Audio.TYPE)) {
                        ClientPlayNetworking.send(new StreamRelay.Audio(at, out, pcm));
                     }
                  });
               }
            }
         }
      };
   }

   private static void frame(StreamRelay.Frame p) {
      BlockPos pos = p.pos().immutable();
      if (!isMe(HOSTS.get(pos))) {
         ScreenShare.View v = VIEWS.computeIfAbsent(pos, ScreenShare.View::new);
         v.lastSeen = System.nanoTime();
         if (p.frame() != v.assembling) {
            v.assembling = p.frame();
            v.parts = new byte[p.parts()][];
            v.got = 0;
         }

         if (v.parts != null && p.part() < v.parts.length && v.parts[p.part()] == null) {
            v.parts[p.part()] = p.data();
            if (++v.got >= v.parts.length) {
               ByteArrayOutputStream all = new ByteArrayOutputStream();

               for (byte[] b : v.parts) {
                  all.writeBytes(b);
               }

               v.parts = null;
               byte[] jpeg = all.toByteArray();
               long hostTime = p.time();
               long local = System.currentTimeMillis();
               long seen = local - hostTime;
               if (v.offset != Long.MAX_VALUE && seen >= v.offset) {
                  if (local - v.offsetAt > 1000L) {
                     v.offset += 2L;
                     v.offsetAt = local;
                  }
               } else {
                  v.offset = seen;
               }

               long due = hostTime + v.offset + 120L;
               CODER.execute(() -> {
                  try {
                     BufferedImage img = ImageIO.read(new ByteArrayInputStream(jpeg));
                     if (img == null) {
                        return;
                     }

                     int w = img.getWidth();
                     int h = img.getHeight();
                     int[] px = img.getRGB(0, 0, w, h, null, 0, w);

                     for (int i = 0; i < px.length; i++) {
                        int c = px[i];
                        px[i] = 0xFF000000 | (c & 0xFF) << 16 | c & 0xFF00 | c >> 16 & 0xFF;
                     }

                     Minecraft.getInstance().execute(() -> {
                        if (VIEWS.get(v.pos) == v) {
                           v.queue.add(new Object[]{due, w, h, px});

                           while (v.queue.size() > 12) {
                              v.queue.poll();
                           }

                           if (v.texture == null) {
                              present(v);
                           }

                           TvScreens.setPump(v.pos, () -> present(v));
                        }
                     });
                  } catch (Exception e) {
                     WorkingConsolesCore.LOGGER.debug("Screen share: bad picture", e);
                  }
               });
            }
         }
      }
   }

   private static void present(ScreenShare.View v) {
      ControllerBindingHandler.drive(v.pos, null);
      long now = System.currentTimeMillis();
      Object[] pick = null;

      while (!v.queue.isEmpty() && (((Number)v.queue.peek()[0]).longValue() <= now || v.texture == null || v.queue.size() > 8)) {
         pick = v.queue.poll();
         if (v.texture == null) {
            break;
         }
      }

      if (pick != null) {
         show(v, (int[])pick[3], (Integer)pick[1], (Integer)pick[2]);
      }
   }

   private static void show(ScreenShare.View v, int[] px, int w, int h) {
      if (VIEWS.get(v.pos) == v) {
         Minecraft mc = Minecraft.getInstance();
         if (v.texture == null || w > v.texW || h > v.texH) {
            if (v.texId != null) {
               mc.getTextureManager().release(v.texId);
            }

            v.texW = Math.max(w, v.texW);
            v.texH = Math.max(h, v.texH);
            v.texId = WorkingConsolesCore.id("dynamic/shared_" + Long.toHexString(v.pos.asLong() & 281474976710655L));
            v.texture = new DynamicTexture(() -> "shared console picture", v.texW, v.texH, true);
            mc.getTextureManager().register(v.texId, v.texture);
         }

         NativeImage img = v.texture.getPixels();
         if (img != null) {
            MemorySegment dst = MemorySegment.ofAddress(img.getPointer()).reinterpret((long)v.texW * v.texH * 4L);

            for (int y = 0; y < h; y++) {
               MemorySegment.copy(px, y * w, dst, ValueLayout.JAVA_INT, (long)y * v.texW * 4L, w);
            }

            v.texture.upload();
            TvScreens.show(v.pos, new TvScreens.Screen(v.texId, w, h, v.texW, v.texH, 0));
         }
      }
   }

   private static void audio(StreamRelay.Audio p) {
      BlockPos pos = p.pos().immutable();
      if (!isMe(HOSTS.get(pos))) {
         ScreenShare.View v = VIEWS.computeIfAbsent(pos, ScreenShare.View::new);
         v.lastSeen = System.nanoTime();
         ConsoleSettings cfg = ConsoleSettings.get();
         if (v.sound == null) {
            v.sound = new McAudioSink(260, () -> RetroSessions.soundPos(pos), cfg.hearingDistance);
            v.sound.start();
         }

         float vol = cfg.volumeFactor();
         byte[] in = p.pcm();
         int samples = in.length / 2;
         double step = p.rate() / 48000.0;
         int outFrames = (int)(samples / step);
         byte[] out = new byte[outFrames * 4];

         for (int i = 0; i < outFrames; i++) {
            int si = Math.min(samples - 1, (int)(i * step));
            int s = (int)((short)(in[si * 2] & 0xFF | in[si * 2 + 1] << 8) * vol);
            out[i * 4] = out[i * 4 + 2] = (byte)s;
            out[i * 4 + 1] = out[i * 4 + 3] = (byte)(s >> 8);
         }

         v.sound.write(out, out.length);
         v.sound.tick();
      }
   }

   private static void drop(BlockPos pos) {
      ScreenShare.View v = VIEWS.remove(pos);
      if (v != null) {
         TvScreens.clear(pos);
         TvScreens.setPump(pos, null);
         if (v.texId != null) {
            Minecraft.getInstance().getTextureManager().release(v.texId);
         }

         if (v.sound != null) {
            v.sound.close();
         }
      }
   }

   private static void tick(Minecraft mc) {
      if (mc.level != null) {
         long now = System.nanoTime();

         for (ScreenShare.View v : VIEWS.values().toArray(new ScreenShare.View[0])) {
            if (now - v.lastSeen > 2500000000L) {
               drop(v.pos);
            }
         }

         CLAIMED_AT.entrySet().removeIf(e -> {
            if (now - e.getValue() < 3000000000L) {
               return false;
            }

            CLAIMED.remove(e.getKey());
            return true;
         });
      } else {
         for (BlockPos pos : VIEWS.keySet().toArray(new BlockPos[0])) {
            drop(pos);
         }

         HOSTS.clear();
         VIEWERS.clear();
         CLAIMED.clear();
         CLAIMED_AT.clear();
         LAST_SENT.clear();
      }
   }

   public enum Role {
      HOST,
      VIEWER,
      WAITING;
   }

   private static final class View {
      final BlockPos pos;
      DynamicTexture texture;
      Identifier texId;
      int texW;
      int texH;
      long lastSeen = System.nanoTime();
      final ArrayDeque<Object[]> queue = new ArrayDeque<>();
      long offset = Long.MAX_VALUE;
      long offsetAt;
      int assembling = -1;
      byte[][] parts;
      int got;
      McAudioSink sound;

      View(BlockPos pos) {
         this.pos = pos;
      }
   }
}
