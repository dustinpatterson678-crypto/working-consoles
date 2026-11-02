package com.dustin.arcadeplus.client;

import com.dustin.arcadeplus.MachinePartBlock;
import com.dustin.arcadeplus.PinballBlock;
import com.dustin.arcadeplus.TicketGameBlock;
import com.dustin.wcore.retro.RetroConsoleBlock;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Step up to a machine and the game's own HUD, hand and crosshair get out of the way; step back and they return. */
public final class MachineView {
   private MachineView() {
   }

   private static boolean hid;
   private static int away;

   public static void init() {
      ClientTickEvents.END_CLIENT_TICK.register(mc -> {
         if (mc.player == null || mc.level == null) {
            hid = false;
            return;
         }
         boolean near = false;
         if (mc.hitResult instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK && bh.getLocation().distanceToSqr(mc.player.getEyePosition()) < 12.0) {
            Block b = mc.level.getBlockState(bh.getBlockPos()).getBlock();
            near = b instanceof TicketGameBlock || b instanceof MachinePartBlock || b instanceof RetroConsoleBlock || b instanceof PinballBlock;
         }
         if (near) {
            away = 0;
            if (!hid && !mc.gui.hud.isHidden()) {
               mc.gui.hud.toggle();
               hid = true;
            }
         } else if (hid && ++away > 10) {              // a short grace so glancing at the coin slot doesn't flicker the HUD
            if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            hid = false;
         }
      });
   }
}
