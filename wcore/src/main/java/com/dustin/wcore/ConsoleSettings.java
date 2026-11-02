package com.dustin.wcore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.loader.api.FabricLoader;

public final class ConsoleSettings {
   public Map<String, String> ps2 = new LinkedHashMap<>();
   public Map<String, String> gamecube = new LinkedHashMap<>();
   public Map<String, Map<String, String>> cores = new LinkedHashMap<>();
   public String handheldColor = "off";
   public int handheldSize = 170;
   public boolean handheldBlend = false;
   public int volume = 80;
   public int hearingDistance = 24;
   public boolean soundInWorld = true;
   public boolean mobsHear = true;
   public boolean redstonePower = true;
   public boolean tvGlow = true;
   public String cables = "tidy";
   public boolean shareScreen = true;
   public int shareQuality = 240;
   private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static ConsoleSettings current;
   public static final String PS2_CORE = "pcsx2_libretro";
   public static final String GAMECUBE_CORE = "dolphin_libretro";
   public static final Set<String> PS2_LOCKED = Set.of("pcsx2_renderer", "pcsx2_bios");
   public static final Set<String> PS2_HIDDEN = Set.of(
      "pcsx2_upscale_multiplier",
      "pcsx2_native_scaling",
      "pcsx2_pgs_ssaa",
      "pcsx2_pgs_high_res_scanout",
      "pcsx2_pgs_ss_tex",
      "pcsx2_pgs_deblur",
      "pcsx2_pgs_disable_mipmaps",
      "pcsx2_hw_download_mode",
      "pcsx2_trilinear_filtering",
      "pcsx2_anisotropic_filtering",
      "pcsx2_dithering",
      "pcsx2_blending_accuracy"
   );
   public static final Set<String> PS2_HIDDEN_CATEGORIES = Set.of("hw_hacks");

   public static final Set<String> GAMECUBE_LOCKED = Set.of("dolphin_renderer", "dolphin_efb_scale", "dolphin_cpu_core");
   public static final Set<String> GAMECUBE_HIDDEN = Set.of("dolphin_dsp_hle", "dolphin_skip_gc_bios");

   public Map<String, String> coreOverrides(String core) {
      return this.cores.computeIfAbsent(core, k -> new LinkedHashMap<>());
   }

   public static void onChange(Runnable listener) {
      LISTENERS.add(listener);
   }

   public static ConsoleSettings get() {
      if (current == null) {
         current = load();
      }

      return current;
   }

   public static Map<String, String> gamecubeRecommended() {
      Map<String, String> m = new LinkedHashMap<>();
      m.put("dolphin_renderer", "Hardware");
      m.put("dolphin_efb_scale", "1");
      m.put("dolphin_cpu_core", "JIT64");
      m.put("dolphin_main_cpu_thread", "enabled");
      m.put("dolphin_fastmem", "enabled");
      m.put("dolphin_fastmem_arena", "enabled");
      m.put("dolphin_dsp_hle", "enabled");
      m.put("dolphin_skip_gc_bios", "enabled");
      return m;
   }

   public Map<String, String> gamecubeBootOptions() {
      Map<String, String> m = gamecubeRecommended();
      m.putAll(this.gamecube);
      GAMECUBE_LOCKED.forEach(m::remove);
      return m;
   }

   public String gamecubeValue(String key, String coreDefault) {
      String v = this.gamecube.get(key);
      if (v == null) {
         v = gamecubeRecommended().get(key);
      }
      return v != null ? v : coreDefault;
   }

   public float volumeFactor() {
      float f = Math.max(0, Math.min(100, this.volume)) / 100.0F;
      return f * f;
   }

   public static Map<String, String> ps2Recommended() {
      Map<String, String> m = new LinkedHashMap<>();
      m.put("pcsx2_mtvu", "enabled");
      m.put("pcsx2_analog_mode1", "enabled");
      m.put("pcsx2_fastboot", "enabled");
      m.put("pcsx2_sw_renderer_threads", String.valueOf(Math.max(1, Math.min(11, Runtime.getRuntime().availableProcessors() / 3 - 1))));
      return m;
   }

   public String ps2Value(String key, String coreDefault) {
      String v = this.ps2.get(key);
      if (v == null) {
         v = ps2Recommended().get(key);
      }

      return v != null ? v : coreDefault;
   }

   public Map<String, String> ps2BootOptions() {
      Map<String, String> m = ps2Recommended();
      m.putAll(this.ps2);
      PS2_LOCKED.forEach(m::remove);
      return m;
   }

   private static Path file() {
      return FabricLoader.getInstance().getConfigDir().resolve("working_consoles.json");
   }

   private static ConsoleSettings load() {
      try {
         if (Files.exists(file())) {
            ConsoleSettings s = (ConsoleSettings)GSON.fromJson(Files.readString(file()), ConsoleSettings.class);
            if (s != null) {
               if (s.ps2 == null) {
                  s.ps2 = new LinkedHashMap<>();
               }

               if (s.gamecube == null) {
                  s.gamecube = new LinkedHashMap<>();
               }

               if (s.cores == null) {
                  s.cores = new LinkedHashMap<>();
               }

               if (s.handheldColor == null) {
                  s.handheldColor = "off";
               }

               return s;
            }
         }
      } catch (Exception e) {
         WorkingConsolesCore.LOGGER.warn("Couldn't read working_consoles.json, using defaults: {}", e.toString());
      }

      ConsoleSettings s = new ConsoleSettings();
      s.save();
      return s;
   }

   public void save() {
      try {
         Files.createDirectories(file().getParent());
         Files.writeString(file(), GSON.toJson(this));
         LISTENERS.forEach(Runnable::run);
      } catch (Exception e) {
         WorkingConsolesCore.LOGGER.warn("Couldn't save working_consoles.json: {}", e.toString());
      }
   }
}