package com.dustin.wcore.controller;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.block.ConsoleBlockEntity;
import com.dustin.wcore.client.ControllerInput;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Every registered controller type. The wireless controller is the universal fallback. */
public final class ControllerTypes {
   public static final String WRONG_CONTROLLER = "That controller doesn't fit this console";
   private static final Map<Identifier, ControllerType> TYPES = new LinkedHashMap<>();
   // ControllerInput masks are compile-time constants, so this doesn't load the client class
   public static final ControllerType WIRELESS = register(
      ControllerType.builder(WorkingConsolesCore.id("wireless_controller"))
         .glowmask(true)
         .bone("stick_l", new BoneAction.Stick(BoneAction.LX, BoneAction.LY, ControllerInput.L3, 0.38F))
         .bone("stick_r", new BoneAction.Stick(BoneAction.RX, BoneAction.RY, ControllerInput.R3, 0.38F))
         .bone("trigger_l", new BoneAction.Trigger(BoneAction.LT, 0.45F))
         .bone("trigger_r", new BoneAction.Trigger(BoneAction.RT, 0.45F))
         .bone("button_a", new BoneAction.Press(ControllerInput.A, -0.18F))
         .bone("button_b", new BoneAction.Press(ControllerInput.B, -0.18F))
         .bone("button_x", new BoneAction.Press(ControllerInput.X, -0.18F))
         .bone("button_y", new BoneAction.Press(ControllerInput.Y, -0.18F))
         .bone("bumper_l", new BoneAction.Press(ControllerInput.LB, -0.18F))
         .bone("bumper_r", new BoneAction.Press(ControllerInput.RB, -0.18F))
         .bone("button_back", new BoneAction.Press(ControllerInput.BACK, -0.18F))
         .bone("button_start", new BoneAction.Press(ControllerInput.START, -0.18F))
         .bone("dpad", new BoneAction.Dpad(0.15F))
         .build()
   );

   private ControllerTypes() {
   }

   public static ControllerType register(ControllerType type) {
      TYPES.put(type.id(), type);
      return type;
   }

   public static ControllerType get(Identifier id) {
      return TYPES.get(id);
   }

   public static Collection<ControllerType> all() {
      return Collections.unmodifiableCollection(TYPES.values());
   }

   /** The controller made for a system, or the wireless controller when no module registered one. */
   public static ControllerType forSystem(String system) {
      for (ControllerType t : TYPES.values()) {
         if (!t.universal() && t.systems().contains(system)) {
            return t;
         }
      }

      return WIRELESS;
   }

   public static ControllerType typeOf(ItemStack stack) {
      return stack.getItem() instanceof ConsoleController c ? c.controllerType() : null;
   }

   /** True if the controller fits the console. Consoles that list no systems (or "*") take any controller. */
   public static boolean accepts(ControllerType type, ConsoleBlockEntity console) {
      if (type == null || type.universal() || console == null) {
         return true;
      }

      Set<String> ids = console.systemIds();
      if (ids.isEmpty() || ids.contains("*")) {
         return true;
      }

      for (String id : ids) {
         if (type.systems().contains(id)) {
            return true;
         }
      }

      return false;
   }

   /**
    * For console blocks: true if the held stack is a controller that doesn't fit the console at pos. Tells the player
    * on the server side; the caller should then return SUCCESS so the controller isn't placed on the console.
    */
   public static boolean rejects(ItemStack stack, Level level, BlockPos pos, Player player) {
      ControllerType type = typeOf(stack);
      if (type != null && level.getBlockEntity(pos) instanceof ConsoleBlockEntity console && !accepts(type, console)) {
         if (!level.isClientSide() && player != null) {
            player.sendOverlayMessage(Component.literal(WRONG_CONTROLLER).withStyle(ChatFormatting.RED));
         }

         return true;
      }

      return false;
   }
}
