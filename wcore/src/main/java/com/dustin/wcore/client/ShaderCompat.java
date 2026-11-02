package com.dustin.wcore.client;

import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tells the mods whether an Iris shader pack is switched on, so lights and glow can use render types that shader packs
 * handle properly. Looks Iris up by name only, so nothing breaks (and nothing changes) when Iris isn't installed.
 */
public final class ShaderCompat {
   private static final Logger LOG = LoggerFactory.getLogger("working-consoles-core");
   private static Object api;
   private static Method inUse;
   private static boolean resolved;
   private static boolean last;
   private static long checkedAt;

   private ShaderCompat() {
   }

   /**
    * Render type for a glowmask layer. Vanilla draws it with eyes; under a shader pack eyes runs in a different
    * shader program than the model under it, the two depths stop matching and the glow z-fights (flickers).
    * Cutout runs in the same program as the model, so the layers stay put.
    */
   public static RenderType glow(Identifier texture) {
      return shadersActive() ? RenderTypes.entityCutout(texture) : RenderTypes.eyes(texture);
   }

   /** True while an Iris shader pack is active (checked at most twice a second). */
   public static synchronized boolean shadersActive() {
      long now = System.nanoTime();
      if (resolved && now - checkedAt < 500_000_000L) {
         return last;
      }

      checkedAt = now;
      boolean value = false;
      try {
         if (!resolved) {
            resolved = true;
            if (FabricLoader.getInstance().isModLoaded("iris")) {
               Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
               api = c.getMethod("getInstance").invoke(null);
               inUse = c.getMethod("isShaderPackInUse");
               LOG.info("[Shaders] Iris found - lights and glow will use shader-friendly render types while a shader pack is on.");
            } else {
               LOG.info("[Shaders] Iris not installed.");
            }
         }

         if (api != null && inUse != null) {
            value = (Boolean)inUse.invoke(api);
         }
      } catch (Throwable t) {
         value = false;
      }

      if (value != last) {
         LOG.info("[Shaders] Shader pack in use: {}", value);
      }

      last = value;
      return value;
   }
}
