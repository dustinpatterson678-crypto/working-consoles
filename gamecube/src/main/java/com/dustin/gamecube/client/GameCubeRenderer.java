package com.dustin.gamecube.client;

import com.dustin.gamecube.GameCubeMod;
import com.dustin.gamecube.block.GameCubeBlock;
import com.dustin.gamecube.block.GameCubeBlockEntity;
import com.dustin.wcore.block.OutletBlock;
import com.dustin.wcore.block.TvBlock;
import com.dustin.wcore.client.ShaderCompat;
import com.dustin.wcore.client.Cables;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
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

public class GameCubeRenderer extends GeoBlockRenderer<GameCubeBlockEntity, BlockEntityRenderState> {
   public GameCubeRenderer(Context context) {
      super(context, new DefaultedItemGeoModel(GameCubeMod.id("console")));
      this.withRenderLayer(new AutoGlowingGeoLayer<GameCubeBlockEntity, Void, BlockEntityRenderState>(this) {
         @Override
         protected RenderType getRenderType(BlockEntityRenderState renderState) {
            return ShaderCompat.glow(this.getTextureResource(renderState));
         }
      });
   }

   @Override
   public void adjustModelBonesForRender(RenderPassInfo<BlockEntityRenderState> info, BoneSnapshots snapshots) {
      GameCubeBlockEntity be = console(((BlockEntityRenderState)info.renderState()).blockPos);
      boolean on = be != null && be.isOn();
      boolean standby = be != null && !on && be.isPowered();
      snapshots.get("led_on").ifPresent(s -> s.skipRender(!on));
      snapshots.get("led_standby").ifPresent(s -> s.skipRender(!standby));
      snapshots.get("accent_on").ifPresent(s -> s.skipRender(!on));
   }

   @Override
   public void submit(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
      super.submit(state, poseStack, collector, camera);
      Level level = Minecraft.getInstance().level;
      GameCubeBlockEntity be = console(state.blockPos);
      if (level != null && be != null) {
         BlockState bs = level.getBlockState(state.blockPos);
         if (bs.getBlock() instanceof GameCubeBlock) {
            Direction facing = (Direction)bs.getValue(GameCubeBlock.FACING);
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

   private static GameCubeBlockEntity console(BlockPos pos) {
      Level level = Minecraft.getInstance().level;
      return level != null && pos != null && level.getBlockEntity(pos) instanceof GameCubeBlockEntity be ? be : null;
   }
}