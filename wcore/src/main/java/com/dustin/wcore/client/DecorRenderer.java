package com.dustin.wcore.client;

import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.block.DecorBlock;
import com.dustin.wcore.block.DecorBlockEntity;
import com.dustin.wcore.block.WideDecorBlock;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One renderer for every v5 furniture piece: the model id is carried by the
 * block itself (DecorBlock.model), the same per-block lookup TvRenderer uses
 * for its per-type map. Besides the GeckoLib model it also draws whatever is
 * stored in the piece's display slots (game cases, consoles, controllers),
 * positioned by DecorBlock.slots.
 */
public class DecorRenderer extends GeoBlockRenderer<DecorBlockEntity, BlockEntityRenderState> {
   private final ItemModelResolver items;
   private final ItemStackRenderState item = new ItemStackRenderState();

   public DecorRenderer(Context context) {
      super(context, new DecorRenderer.PerBlockModel());
      this.items = context.itemModelResolver();
      // emissive pass for the arcade neon pieces (and anything else that ships
      // a <texture>_glowmask.png). Pieces without a glowmask are skipped - the
      // stock AutoGlowingGeoLayer would otherwise draw the missing texture over
      // the whole model, and most decor pieces (all furniture) have no mask.
      this.withRenderLayer(new AutoGlowingGeoLayer<DecorBlockEntity, Void, BlockEntityRenderState>(this) {
         private final Map<Identifier, Boolean> masks = new HashMap<>();

         protected RenderType getRenderType(BlockEntityRenderState renderState) {
            Level level = Minecraft.getInstance().level;
            if (level != null && renderState.blockPos != null) {
               BlockState state = level.getBlockState(renderState.blockPos);
               if (state.getBlock() instanceof DecorBlock && !(Boolean)state.getValue(DecorBlock.LIT)) {
                  return null;
               }
            }

            Identifier glowmask = this.getTextureResource(renderState);
            return this.masks.computeIfAbsent(glowmask, DecorRenderer::glowmaskExists) ? ShaderCompat.glow(glowmask) : null;
         }
      });
   }

   private static boolean glowmaskExists(Identifier texture) {
      try (InputStream in = Minecraft.getInstance().getResourceManager().open(texture)) {
         return in != null;
      } catch (Exception e) {
         return false;
      }
   }

   public boolean shouldRenderOffScreen() {
      return true;
   }

   public void submit(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      Level level = Minecraft.getInstance().level;
      BlockState modelState = level != null && state.blockPos != null ? level.getBlockState(state.blockPos) : null;
      if (modelState != null && modelState.getBlock() instanceof WideDecorBlock wide) {
         // wide pieces are authored centred on the seam between their two
         // cells: draw the model shifted half a block toward the side cell
         Direction side = WideDecorBlock.sideDir(modelState);
         poseStack.pushPose();
         poseStack.translate(side.getStepX() * 0.5, 0.0, side.getStepZ() * 0.5);
         super.submit(state, poseStack, collector, camera);
         poseStack.popPose();
      } else {
         super.submit(state, poseStack, collector, camera);
      }

      if (level != null && state.blockPos != null && level.getBlockEntity(state.blockPos) instanceof DecorBlockEntity be) {
         BlockState bs = level.getBlockState(state.blockPos);
         if (bs.getBlock() instanceof DecorBlock decor && !decor.slots.isEmpty()) {
            // GeckoLib draws the model's front(-z) toward FACING; for the slots
            // to land at the same model pixels they were authored at, the base
            // yaw has to be 180 - toYRot (the old -toYRot put every slot 180
            // degrees off - the display-shelf cases hovered in front of the
            // shelf and cases on 1.75px rows poked through the woodwork).
            float yaw = 180.0F - ((Direction)bs.getValue(DecorBlock.FACING)).toYRot();
            boolean mirror = bs.getBlock() instanceof WideDecorBlock && (Boolean)bs.getValue(WideDecorBlock.MIRROR);
            int n = Math.min(decor.slots.size(), be.slotCount());

            for (int i = 0; i < n; i++) {
               ItemStack stack = be.get(i);
               if (stack.isEmpty()) {
                  continue;
               }

               DecorBlock.DecorSlot slot = decor.slots.get(i);
               poseStack.pushPose();
               poseStack.translate(0.5, 0.5, 0.5);
               poseStack.rotateDegrees(Axis.YP, yaw);
               if (decor.anchorPx != 0.0) {
                  // wide pieces: slots are authored around the seam too, so
                  // they follow the model to whichever side it was placed on
                  poseStack.translate(decor.anchorPx / 16.0 * (mirror ? -1.0 : 1.0), 0.0, 0.0);
               }

               poseStack.translate(slot.x() / 16.0, (slot.y() - 8.0) / 16.0, slot.z() / 16.0);
               if (slot.yaw() != 0.0F) {
                  poseStack.rotateDegrees(Axis.YP, slot.yaw());
               }

               if (slot.tilt() != 0.0F) {
                  poseStack.rotateDegrees(Axis.XP, slot.tilt());
               }

               poseStack.scale(slot.scale(), slot.scale(), slot.scale());

               // the slots store a shelf-top height, but item models pivot
               // differently: block items (consoles/TVs, GeoBlockItem) are
               // centred on the origin -8px, game cases are centred in their
               // 9x10x2 geo, small geo items are near-centred
               float lift = switch (slot.kind()) {
                  case CONSOLE -> stack.getItem() instanceof BlockItem ? 8.0F : 2.0F;
                  case CONTROLLER -> 2.0F;
                  case CASE -> 5.0F;
               };
               if (lift != 0.0F) {
                  poseStack.translate(0.0, lift / 16.0, 0.0);
               }

               this.items.updateForTopItem(this.item, stack, ItemDisplayContext.FIXED, level, null, i);
               this.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
               poseStack.popPose();
            }
         }
      }
   }

   private static final class PerBlockModel extends DefaultedItemGeoModel<DecorBlockEntity> {
      private final Map<Identifier, DefaultedItemGeoModel<DecorBlockEntity>> models = new HashMap<>();

      PerBlockModel() {
         super(WorkingConsolesCore.id("tv_stand"));
      }

      private DefaultedItemGeoModel<DecorBlockEntity> of(GeoRenderState renderState) {
         Level level = Minecraft.getInstance().level;
         if (renderState instanceof BlockEntityRenderState s && s.blockPos != null && level != null) {
            BlockState bs = level.getBlockState(s.blockPos);
            if (bs.getBlock() instanceof DecorBlock decor) {
               return this.models.computeIfAbsent(decor.model, m -> new DefaultedItemGeoModel(m));
            }
         }

         return this.models.computeIfAbsent(WorkingConsolesCore.id("tv_stand"), m -> new DefaultedItemGeoModel(m));
      }

      public Identifier getModelResource(GeoRenderState renderState) {
         return this.of(renderState).getModelResource(renderState);
      }

      public Identifier getTextureResource(GeoRenderState renderState) {
         return this.of(renderState).getTextureResource(renderState);
      }

      public Identifier getAnimationResource(DecorBlockEntity be) {
         BlockState bs = be.getBlockState();
         return bs.getBlock() instanceof DecorBlock decor
            ? this.models.computeIfAbsent(decor.model, m -> new DefaultedItemGeoModel(m)).getAnimationResource(be)
            : super.getAnimationResource(be);
      }
   }
}
