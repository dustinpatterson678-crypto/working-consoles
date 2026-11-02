package com.dustin.arcadebuild.mixin.client;

import com.dustin.arcadebuild.client.HallCache;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ReloadableResourceManager.class)
public abstract class ResourceReloadMixin {
   @Shadow
   @Final
   private PackType type;

   @ModifyVariable(method = "createReload", at = @At("HEAD"), argsOnly = true)
   private List<PackResources> arcadebuild$packs(List<PackResources> packs) {
      if (this.type != PackType.CLIENT_RESOURCES) return packs;
      List<PackResources> out = new ArrayList<>(packs);
      out.add(new HallCache());
      return out;
   }
}
