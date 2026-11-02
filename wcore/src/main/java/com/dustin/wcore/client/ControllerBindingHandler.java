package com.dustin.wcore.client;

import com.dustin.wcore.block.ConsoleBlockEntity;
import com.dustin.wcore.emu.EmuThread;
import com.dustin.wcore.net.ConsoleInputPacket;
import com.dustin.wcore.net.SeatActionPacket;
import com.dustin.wcore.net.SeatSyncPacket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

public final class ControllerBindingHandler {
   private static final int STALE_NANOS = 1000000000;
   private static final Map<BlockPos, UUID[]> SEATS = new HashMap<>();
   private static final Map<BlockPos, ControllerBindingHandler.Remote[]> REMOTE = new HashMap<>();
   private static final Map<String, int[]> LAST_SENT = new HashMap<>();
   private static final Map<String, Long> LAST_SENT_AT = new HashMap<>();
   private static final RetroPadInput PAD1 = new RetroPadInput();
   private static final RetroPadInput PAD2 = new RetroPadInput();
   private static boolean wasInInventory;
   private static long lastAutoJoin;

   private ControllerBindingHandler() {
   }

   public static void init() {
      ClientPlayNetworking.registerGlobalReceiver(SeatSyncPacket.TYPE, (packet, context) -> {
         if (packet.player1() == null && packet.player2() == null) {
            SEATS.remove(packet.pos());
         } else {
            SEATS.put(packet.pos().immutable(), new UUID[]{packet.player1(), packet.player2()});
         }
      });
      ClientPlayNetworking.registerGlobalReceiver(
         ConsoleInputPacket.TYPE,
         (packet, context) -> {
            ControllerBindingHandler.Remote[] r = REMOTE.computeIfAbsent(packet.pos().immutable(), k -> new ControllerBindingHandler.Remote[2]);
            r[packet.port()] = new ControllerBindingHandler.Remote(
               new int[]{packet.buttons(), packet.lx(), packet.ly(), packet.rx(), packet.ry()}, System.nanoTime()
            );
         }
      );
   }

   public static void clientTick(Minecraft mc) {
      if (mc.level != null && mc.player != null) {
         for (BlockPos shared : ScreenShare.viewing()) {
            drive(shared, null);
         }

         boolean inInventory = mc.gui.screen() instanceof InventoryScreen || mc.gui.screen() instanceof CreativeModeInventoryScreen;
         if (inInventory && !wasInInventory && localSeat(mc.player) != null) {
            ClientPlayNetworking.send(new SeatActionPacket(BlockPos.ZERO, false));
         }

         wasInInventory = inInventory;
      } else {
         SEATS.clear();
         REMOTE.clear();
         LAST_SENT.clear();
         LAST_SENT_AT.clear();
      }
   }

   public static Entry<BlockPos, Integer> localSeat(Player me) {
      UUID id = me.getUUID();

      for (Entry<BlockPos, UUID[]> e : SEATS.entrySet()) {
         for (int port = 0; port < 2; port++) {
            if (id.equals(e.getValue()[port])) {
               return Map.entry(e.getKey(), port);
            }
         }
      }

      return null;
   }

   public static int localPort(BlockPos console, Player me) {
      UUID[] seats = SEATS.get(console);
      if (seats == null) {
         return -1;
      }

      for (int port = 0; port < 2; port++) {
         if (me.getUUID().equals(seats[port])) {
            return port;
         }
      }

      return -1;
   }

   public static void drive(BlockPos console, EmuThread emu) {
      Minecraft mc = Minecraft.getInstance();
      Player me = mc.player;
      int[] port1 = null;
      int[] port2 = null;
      if (me != null) {
         int mine = localPort(console, me);
         if (mine < 0) {
            maybeAutoJoin(console, me, mc);
         } else if (mc.gui.screen() == null && ControllerInput.isHoldingController(me)) {
            // no controller in hand = no input: a pad lying around must not play every console in the world
            PAD1.pollFrom(ControllerInput.state(), true);
            int[] in = new int[]{PAD1.buttons, PAD1.lx, PAD1.ly, PAD1.rx, PAD1.ry};
            send(console, mine, in);
            if (mine == 0) {
               port1 = in;
            } else {
               port2 = in;
            }

            UUID[] seats = SEATS.get(console);
            ControllerInput.State second = ControllerInput.state2();
            if (mine == 0 && second.connected && (seats == null || seats[1] == null)) {
               PAD2.pollFrom(second, false);
               port2 = new int[]{PAD2.buttons, PAD2.lx, PAD2.ly, PAD2.rx, PAD2.ry};
               send(console, 1, port2);
            }
         }
      }

      if (emu != null) {
         if (port1 == null) {
            port1 = remote(console, 0);
         }

         if (port2 == null) {
            port2 = remote(console, 1);
         }

         emu.setKeys(port1[0]);
         emu.setAnalog(port1[1], port1[2], port1[3], port1[4]);
         emu.setPort2(port2[0], port2[1], port2[2], port2[3], port2[4]);
      }
   }

   private static void maybeAutoJoin(BlockPos console, Player me, Minecraft mc) {
      if (mc.gui.screen() == null && localSeat(me) == null && ControllerInput.isHoldingController(me)) {
         UUID[] seats = SEATS.get(console);
         if (seats == null || seats[0] == null) {
            double d = 6.0;
            BlockPos tv = mc.level != null && mc.level.getBlockEntity(console) instanceof ConsoleBlockEntity be ? be.tvPos() : null;
            boolean near = me.distanceToSqr(console.getX() + 0.5, console.getY() + 0.5, console.getZ() + 0.5) <= d * d
               || tv != null && me.distanceToSqr(tv.getX() + 0.5, tv.getY() + 0.5, tv.getZ() + 0.5) <= d * d;
            if (near) {
               if (ControllerInput.state().buttons != 0) {
                  long now = System.nanoTime();
                  if (now - lastAutoJoin >= 1000000000L) {
                     lastAutoJoin = now;
                     ClientPlayNetworking.send(new SeatActionPacket(console, true));
                  }
               }
            }
         }
      }
   }

   private static int[] remote(BlockPos console, int port) {
      ControllerBindingHandler.Remote[] r = REMOTE.get(console);
      return r != null && r[port] != null && System.nanoTime() - r[port].when() <= 1000000000L ? r[port].input() : new int[5];
   }

   private static void send(BlockPos console, int port, int[] in) {
      String k = console.asLong() + ":" + port;
      int[] last = LAST_SENT.get(k);
      long now = System.nanoTime();
      Long at = LAST_SENT_AT.get(k);
      boolean changed = last == null || !Arrays.equals(last, in);
      boolean keepAlive = at == null || now - at > 500000000L;
      if (changed || keepAlive && (in[0] != 0 || in[1] != 0 || in[2] != 0 || in[3] != 0 || in[4] != 0)) {
         LAST_SENT.put(k, (int[])in.clone());
         LAST_SENT_AT.put(k, now);
         if (ClientPlayNetworking.canSend(ConsoleInputPacket.TYPE)) {
            ClientPlayNetworking.send(new ConsoleInputPacket(console, port, in[0], (short)in[1], (short)in[2], (short)in[3], (short)in[4]));
         }
      }
   }

   private record Remote(int[] input, long when) {
   }
}
