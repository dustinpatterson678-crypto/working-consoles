package com.dustin.stream.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.renderer.texture.AbstractTexture;

/** Lets the world renderer use the browser's own GPU texture by Identifier. MCEF owns the texture, so closing does nothing. */
final class BrowserTexture extends AbstractTexture {
   private final MCEFBrowser browser;

   BrowserTexture(MCEFBrowser browser) {
      this.browser = browser;
   }

   @Override
   public GpuTexture getTexture() {
      return this.browser.getTexture();
   }

   @Override
   public GpuTextureView getTextureView() {
      return this.browser.getTextureView();
   }

   @Override
   public GpuSampler getSampler() {
      return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
   }

   @Override
   public void close() {
   }
}
