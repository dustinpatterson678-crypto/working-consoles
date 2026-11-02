package com.dustin.ps2.client;

import com.dustin.ps2.Ps2Mod;
import com.dustin.ps2.block.Ps2Block;
import com.dustin.ps2.block.Ps2BlockEntity;
import com.dustin.wcore.block.OutletBlock;
import com.dustin.wcore.block.TvBlock;
import com.dustin.wcore.client.Cables;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Ps2Renderer extends GeoBlockRenderer<Ps2BlockEntity, BlockEntityRenderState> {
   public Ps2Renderer(Context context) {
      super(context, new DefaultedItemGeoModel(Ps2Mod.id("console")));
      this.withRenderLayer(new AutoGlowingGeoLayer<Ps2BlockEntity, Void, BlockEntityRenderState>(this) {
         protected RenderType getRenderType(BlockEntityRenderState renderState) {
            return com.dustin.wcore.client.ShaderCompat.glow(this.getTextureResource(renderState));
         }
      });
   }

   public void adjustModelBonesForRender(RenderPassInfo<BlockEntityRenderState> info, BoneSnapshots snapshots) {
      Ps2BlockEntity be = console(((BlockEntityRenderState)info.renderState()).blockPos);
      boolean on = be != null && be.isOn();
      boolean standby = be != null && !on && be.isPowered();
      snapshots.get("led_on").ifPresent(s -> s.skipRender(!on));
      snapshots.get("led_standby").ifPresent(s -> s.skipRender(!standby));
   }

   public void submit(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      super.submit(state, poseStack, collector, camera);
      Level level = Minecraft.getInstance().level;
      Ps2BlockEntity be = console(state.blockPos);
      if (level != null && be != null) {
         BlockState bs = level.getBlockState(state.blockPos);
         if (bs.getBlock() instanceof Ps2Block) {
            Direction facing = (Direction)bs.getValue(Ps2Block.FACING);
            BlockPos self = state.blockPos;
            BlockPos tv = be.tvPos();
            if (tv != null && level.getBlockState(tv).getBlock() instanceof TvBlock) {
               Direction tvFacing = (Direction)level.getBlockState(tv).getValue(TvBlock.FACING);
               Cables.draw(poseStack, collector, Cables.port(self, self, facing, 0.3375, 0.125), Cables.port(self, tv, tvFacing, 0.1, 0.375), state.lightCoords);
            }

            BlockPos outlet = be.outletPos();
            if (outlet != null && level.getBlockState(outlet).getBlock() instanceof OutletBlock) {
               Direction oFacing = (Direction)level.getBlockState(outlet).getValue(OutletBlock.FACING);
               Cables.draw(
                  poseStack, collector, Cables.port(self, self, facing, 0.3375, 0.075), Cables.port(self, outlet, oFacing, 0.4375, 0.5), state.lightCoords
               );
            }
         }
      }
   }

   private static Ps2BlockEntity console(BlockPos pos) {
      Level level = Minecraft.getInstance().level;
      return level != null && pos != null && level.getBlockEntity(pos) instanceof Ps2BlockEntity be ? be : null;
   }
}
