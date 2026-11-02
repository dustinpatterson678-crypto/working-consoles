package com.dustin.wcore.api;

import com.dustin.wcore.client.TvScreens;
import com.dustin.wcore.client.WorkingConsolesCoreClient;
import com.dustin.wcore.controller.RegisteredController;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Client half of {@link WorkingConsolesApi}: controller renderers and putting your own pictures on TVs. */
public final class WorkingConsolesClientApi {
   private WorkingConsolesClientApi() {
   }

   /** Renders a controller registered with WorkingConsolesApi.registerController (held and placed). */
   public static void registerControllerRenderer(RegisteredController controller) {
      WorkingConsolesCoreClient.registerController(controller);
   }

   /**
    * Shows a texture on every TV cabled to the console at `console`. width/height is the picture inside a
    * texWidth x texHeight texture. Call again whenever the picture changes (bump serial so TVs notice).
    */
   public static void showOnTv(BlockPos console, Identifier texture, int width, int height, int texWidth, int texHeight, int serial) {
      TvScreens.show(console, new TvScreens.Screen(texture, width, height, texWidth, texHeight, serial));
   }

   /** Takes the picture off the TVs (they go back to dark glass). */
   public static void clearTv(BlockPos console) {
      TvScreens.clear(console);
   }

   /**
    * Runs `pump` on the render thread every frame a TV shows this console: upload your next picture there.
    * Pass null to remove it. Never touch OpenGL anywhere else.
    */
   public static void setTvPump(BlockPos console, Runnable pump) {
      TvScreens.setPump(console, pump);
   }
}
