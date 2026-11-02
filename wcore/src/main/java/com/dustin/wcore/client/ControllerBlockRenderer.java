package com.dustin.wcore.client;

import com.dustin.wcore.block.ControllerBlockEntity;
import com.dustin.wcore.controller.ControllerType;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A placed controller: the same GeckoLib model as the item, bones driven by ControllerRenderer.placedInput. */
public class ControllerBlockRenderer extends GeoBlockRenderer<ControllerBlockEntity, BlockEntityRenderState> {
   private final ControllerType type;

   public ControllerBlockRenderer(Context context, ControllerType type) {
      super(context, ControllerRenderer.<ControllerBlockEntity>model(type));
      this.type = type;
      if (type.glowmask()) {
         this.withRenderLayer(new AutoGlowingGeoLayer<ControllerBlockEntity, Void, BlockEntityRenderState>(this) {
            protected RenderType getRenderType(BlockEntityRenderState renderState) {
               return ShaderCompat.glow(this.getTextureResource(renderState));
            }
         });
      }
   }

   public void adjustModelBonesForRender(RenderPassInfo<BlockEntityRenderState> info, BoneSnapshots snapshots) {
      BlockPos pos = info.renderState().blockPos;
      Level level = Minecraft.getInstance().level;
      if (pos != null && level != null && level.getBlockEntity(pos) instanceof ControllerBlockEntity be) {
         ItemStack stack = be.stack();
         ControllerRenderer.applyBones(this.type.bones(), ControllerRenderer.placedInput.stateFor(pos, stack), snapshots);
      }
   }
}
