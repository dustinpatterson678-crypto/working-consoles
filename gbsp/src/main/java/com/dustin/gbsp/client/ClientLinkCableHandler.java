package com.dustin.gbsp.client;

import com.dustin.gbsp.link.LinkCableNetworking;
import com.dustin.wcore.emu.NativeCore;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public final class ClientLinkCableHandler {
   private static int role = -1;
   private static String partnerName = "";
   private static NativeCore plugged;
   private static boolean wasInInventory;

   private ClientLinkCableHandler() {
   }

   public static void init() {
      ClientPlayNetworking.registerGlobalReceiver(LinkCableNetworking.LinkCableConnectPayload.TYPE, (payload, context) -> {
         role = payload.connected() ? payload.role() : -1;
         partnerName = payload.partnerName();
         replug();
      });
      ClientPlayNetworking.registerGlobalReceiver(LinkCableNetworking.LinkCableDataPayload.TYPE, (payload, context) -> {
         NativeCore core = plugged;
         if (core != null) {
            core.linkReceive(payload.data());
         }
      });
   }

   public static boolean isLinked() {
      return role >= 0;
   }

   public static String partner() {
      return partnerName;
   }

   public static void clientTick(Minecraft mc) {
      if (mc.level == null) {
         role = -1;
         replug();
      } else {
         if (HandheldSession.core() != plugged) {
            replug();
         }

         boolean inInventory = mc.gui.screen() instanceof InventoryScreen || mc.gui.screen() instanceof CreativeModeInventoryScreen;
         if (inInventory && !wasInInventory && role >= 0) {
            ClientPlayNetworking.send(LinkCableNetworking.LinkCableConnectPayload.unplug());
         }

         wasInInventory = inInventory;
      }
   }

   private static void replug() {
      NativeCore now = role >= 0 ? HandheldSession.core() : null;
      if (plugged != null && plugged != now) {
         plugged.linkDisconnect();
      }

      plugged = now;
      if (now != null) {
         if (!NativeCore.linkSupported()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
               mc.player.sendSystemMessage(Component.literal("[Handheld] This engine version has no link port").withStyle(ChatFormatting.RED));
            }
         } else {
            now.linkConnect(role, data -> ClientPlayNetworking.send(new LinkCableNetworking.LinkCableDataPayload(data)));
         }
      }
   }
}
