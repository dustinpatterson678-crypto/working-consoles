package com.dustin.wcore.controller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

/**
 * One kind of controller. systems are RetroSystem ids (or "ps2", "gamecube", "browser"); an empty set means the
 * controller fits every console. model, texture and animation are GeckoLib base ids, resolved like
 * DefaultedItemGeoModel does (geckolib/models/item/<path>.geo.json, textures/item/<path>.png,
 * geckolib/animations/item/<path>.animation.json).
 */
public record ControllerType(
   Identifier id,
   Set<String> systems,
   int maxPorts,
   Identifier model,
   Identifier texture,
   Identifier animation,
   boolean glowmask,
   Map<String, BoneAction> bones,
   boolean wireless
) {
   public ControllerType {
      systems = Set.copyOf(systems);
      bones = Collections.unmodifiableMap(new LinkedHashMap<>(bones));
   }

   public boolean universal() {
      return this.systems.isEmpty();
   }

   public boolean accepts(String system) {
      return this.universal() || this.systems.contains(system);
   }

   public static ControllerType.Builder builder(Identifier id) {
      return new ControllerType.Builder(id);
   }

   public static final class Builder {
      private final Identifier id;
      private Set<String> systems = Set.of();
      private int maxPorts = 2;
      private Identifier model;
      private Identifier texture;
      private Identifier animation;
      private boolean glowmask;
      private final Map<String, BoneAction> bones = new LinkedHashMap<>();

      private Builder(Identifier id) {
         this.id = id;
      }

      public ControllerType.Builder systems(String... systems) {
         this.systems = Set.of(systems);
         return this;
      }

      public ControllerType.Builder ports(int maxPorts) {
         this.maxPorts = maxPorts;
         return this;
      }

      public ControllerType.Builder model(Identifier model) {
         this.model = model;
         return this;
      }

      public ControllerType.Builder texture(Identifier texture) {
         this.texture = texture;
         return this;
      }

      public ControllerType.Builder animation(Identifier animation) {
         this.animation = animation;
         return this;
      }

      public ControllerType.Builder glowmask(boolean glowmask) {
         this.glowmask = glowmask;
         return this;
      }

      public ControllerType.Builder bone(String bone, BoneAction action) {
         this.bones.put(bone, action);
         return this;
      }

      public ControllerType build() {
         Identifier m = this.model != null ? this.model : this.id;
         return new ControllerType(
            this.id,
            this.systems,
            this.maxPorts,
            m,
            this.texture != null ? this.texture : m,
            this.animation != null ? this.animation : m,
            this.glowmask,
            this.bones,
            true
         );
      }
   }
}
