package com.dustin.stream.mixin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.cef.CefSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Tunes the embedded Chromium that MCEF starts, so browser games (WebGPU / WebAssembly engines such as Combat Lab) can run:
 * <ul>
 * <li>WebGPU: the bundled browser has no DXC shader compiler DLLs, so Dawn's device creation fails ("dxil.dll Windows Error: 87").
 * Telling Dawn to use the built-in FXC compiler instead makes requestDevice() work.</li>
 * <li>A real profile on disk: MCEF runs an in-memory profile by default, which cannot register service workers (needed for
 * cross-origin isolation / SharedArrayBuffer) and cannot hold a multi-GB imported game image. A profile folder fixes both,
 * and also keeps logins and imported files between sessions.</li>
 * </ul>
 */
@Mixin(targets = "net.dimaskama.mcef.impl.MCEFApiImpl", remap = false)
public abstract class McefArgsMixin {
   private static final Logger WC_LOG = LoggerFactory.getLogger("working-consoles-stream");

   @ModifyArg(method = "<init>", remap = false,
      at = @At(value = "INVOKE", target = "Lme/friwi/jcefmaven/CefAppBuilder;addJcefArgs([Ljava/lang/String;)V", remap = false), index = 0)
   private String[] wc$browserArgs(String[] args) {
      try {
         java.util.List<String> add = new java.util.ArrayList<>();
         boolean haveDawn = false, haveSab = false;
         for (String a : args) {
            if (a == null) continue;
            if (a.startsWith("--disable-dawn-features=")) haveDawn = true;
            if (a.startsWith("--enable-blink-features=")) haveSab = true;
         }
         // WebGPU: use the built-in shader compiler (the bundled browser has no dxil.dll)
         if (!haveDawn) add.add("--disable-dawn-features=use_dxc");
         // The embedded browser never reaches "cross-origin isolated" (verified in the game log), so allow SharedArrayBuffer without it.
         // Browser games / WebAssembly threads (Combat Lab) need SharedArrayBuffer and fail without it.
         if (!haveSab) add.add("--enable-blink-features=UnrestrictedSharedArrayBuffer");
         if (add.isEmpty()) return args;
         String[] out = Arrays.copyOf(args, args.length + add.size());
         for (int i = 0; i < add.size(); i++) out[args.length + i] = add.get(i);
         WC_LOG.info("Browser args: {}", Arrays.toString(out));
         return out;
      } catch (Throwable t) {
         WC_LOG.warn("Could not add browser args", t);
         return args;
      }
   }

   @ModifyArg(method = "<init>", remap = false,
      at = @At(value = "INVOKE", target = "Lorg/cef/CefApp;setSettings(Lorg/cef/CefSettings;)V", remap = false), index = 0)
   private CefSettings wc$browserSettings(CefSettings settings) {
      try {
         if (settings.cache_path == null || settings.cache_path.isEmpty()) {
            Path profile = Path.of(settings.root_cache_path).resolve("profile").toAbsolutePath();
            Files.createDirectories(profile);
            settings.cache_path = profile.toString();
            settings.persist_session_cookies = true;
            WC_LOG.info("Browser profile folder: {}", profile);
         }
      } catch (Throwable t) {
         WC_LOG.warn("Could not set a browser profile folder", t);
      }
      return settings;
   }
}
