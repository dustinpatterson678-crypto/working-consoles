package com.dustin.wcore.api;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.controller.ControllerType;
import com.dustin.wcore.controller.ControllerTypes;
import com.dustin.wcore.controller.RegisteredController;
import com.dustin.wcore.retro.RetroSystem;
import net.minecraft.resources.Identifier;

/**
 * The stable entry point for addon mods (see docs/API.md). Everything here only forwards to Working Consoles' own
 * code, so addons keep working when the internals change. Call these from your {@link WorkingConsolesPlugin}.
 */
public final class WorkingConsolesApi {
   /** Bumped when something here changes in a way addons must know about. */
   public static final int VERSION = 1;

   private WorkingConsolesApi() {
   }

   /** Adds a libretro-powered console system (games folder, core, file types, BIOS). Returns it for chaining. */
   public static RetroSystem registerSystem(RetroSystem system) {
      return RetroSystem.register(system);
   }

   /** Starts a controller description under your own mod id, e.g. controller(Identifier.fromNamespaceAndPath("mymod", "pad")). */
   public static ControllerType.Builder controller(Identifier id) {
      return ControllerType.builder(id);
   }

   /** Registers a controller: its item, its placeable block and which consoles it plays. Client: see WorkingConsolesClientApi. */
   public static RegisteredController registerController(ControllerType type) {
      return WorkingConsolesCore.registerController(type);
   }

   /** The controller made for a console system id, or the universal wireless controller. */
   public static ControllerType controllerFor(String system) {
      return ControllerTypes.forSystem(system);
   }
}
