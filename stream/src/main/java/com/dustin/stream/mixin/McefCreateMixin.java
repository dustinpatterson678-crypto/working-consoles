package com.dustin.stream.mixin;

import com.dustin.stream.diag.CoiDiag;
import net.dimaskama.mcef.api.MCEFBrowser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.cef.CefBrowserSettings;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets the diagnostics see every browser MCEF creates (read-only). */
@Mixin(targets = "net.dimaskama.mcef.impl.MCEFApiImpl", remap = false)
public abstract class McefCreateMixin {
   @Inject(method = "createBrowser", at = @At("RETURN"), remap = false, require = 0)
   private void wc$track(String url, boolean transparent, CallbackInfoReturnable<MCEFBrowser> cir) {
      try {
         CoiDiag.track(cir.getReturnValue());
      } catch (Throwable ignored) {
      }
   }

   /** The embedded browser defaults to 30 frames per second; ask for 60 from the start. */
   @ModifyArg(method = "createBrowser", remap = false, require = 0,
      at = @At(value = "INVOKE", target = "Lnet/dimaskama/mcef/impl/MCEFBrowserImpl;<init>(Lorg/cef/CefClient;Ljava/lang/String;ZLorg/cef/browser/CefRequestContext;Lorg/cef/CefBrowserSettings;)V", remap = false), index = 4)
   private CefBrowserSettings wc$fps(CefBrowserSettings settings) {
      try {
         CefBrowserSettings s = settings != null ? settings : new CefBrowserSettings();
         if (s.windowless_frame_rate < 60) s.windowless_frame_rate = 60;
         return s;
      } catch (Throwable t) {
         return settings;
      }
   }
}
