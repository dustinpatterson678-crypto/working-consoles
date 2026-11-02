package com.dustin.stream.client;

import com.dustin.stream.MediaBlock;
import com.dustin.stream.MediaBlockEntity;
import com.dustin.stream.ProjectorBlock;
import com.dustin.stream.StreamMod;
import com.dustin.wcore.block.ConsoleBlockEntity;
import com.dustin.wcore.client.Cables;
import com.dustin.wcore.client.TvScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/** Draws the Smart TV and the Cinema Projector with its big screen, plus the live browser picture and cursor. */
public class MediaRenderer implements BlockEntityRenderer<MediaBlockEntity, BlockEntityRenderState> {
   static final Identifier WHITE = StreamMod.id("textures/misc/white.png");
   private static final int FULL_BRIGHT = 15728880;

   public MediaRenderer(BlockEntityRendererProvider.Context context) {
   }

   @Override
   public BlockEntityRenderState createRenderState() {
      return new BlockEntityRenderState();
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return true;
   }

   @Override
   public int getViewDistance() {
      return 256;
   }

   @Override
   public void submit(BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
      Level level = Minecraft.getInstance().level;
      if (level == null || state.blockPos == null || !(level.getBlockEntity(state.blockPos) instanceof MediaBlockEntity be)) {
         return;
      }
      BlockState bs = level.getBlockState(state.blockPos);
      if (!(bs.getBlock() instanceof MediaBlock block)) {
         return;
      }
      Direction facing = bs.getValue(MediaBlock.FACING);
      boolean on = be.isOn() && be.isPowered();
      Browsers.Session session = on ? Browsers.get(state.blockPos) : null;
      boolean live = Browsers.ready(session);
      if (live && state.blockPos.equals(InputFx.controlling)) {
         // the screen you are using: hand the controller to the page every frame, so pad input is snappy
         BrowserGamepad.frame(session);
      }
      int light = state.lightCoords;

      // HDMI: the plugged-in console's own picture (published by the console wherever it is)
      int input = be.input();
      TvScreens.Screen hdmiPic = null;
      if (on && input > 0) {
         BlockPos con = be.hdmi(input);
         if (con != null && level.getBlockEntity(con) instanceof ConsoleBlockEntity c && c.isOn() && c.isPowered()) {
            try {
               hdmiPic = TvScreens.get(con);
            } catch (RuntimeException e) {
               hdmiPic = null;
            }
         }
      }
      for (int port = 1; port <= MediaBlockEntity.PORTS; port++) {
         BlockPos con = be.hdmi(port);
         if (con != null && level.isLoaded(con)) {
            BlockState cs = level.getBlockState(con);
            Direction cf = cs.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? cs.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
            Cables.draw(pose, collector, state.blockPos,
               Cables.port(state.blockPos, state.blockPos, facing, 0.12 * port, block.kind == MediaBlock.Kind.TV ? 0.6 : 0.15),
               Cables.port(state.blockPos, con, cf, 0.3, 0.08), light);
         }
      }

      pose.pushPose();
      pose.translate(0.5, 0.0, 0.5);
      pose.rotate(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));

      Vector3f picCentre;
      Vector3f picNormal;
      float picW;
      float picH;
      if (block.kind == MediaBlock.Kind.TV) {
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) -> {
            Draw.box(b, p, -1.0F, 0.30F, -0.03F, 1.0F, 1.45F, 0.05F, 0xFF111114, light);   // panel
            Draw.box(b, p, -0.06F, 0.06F, 0.0F, 0.06F, 0.30F, 0.06F, 0xFF1C1C20, light);    // neck
            Draw.box(b, p, -0.35F, 0.0F, -0.15F, 0.35F, 0.06F, 0.20F, 0xFF1C1C20, light);   // foot
            Draw.box(b, p, 0.90F, 0.31F, -0.04F, 0.94F, 0.33F, -0.03F, on ? 0xFF40FF70 : 0xFFB02020, FULL_BRIGHT); // LED
         });
         picCentre = new Vector3f(0.0F, 0.875F, -0.035F);
         picNormal = new Vector3f(0.0F, 0.0F, -1.0F);
         picW = 1.92F;
         picH = 1.08F;
      } else {
         int size = bs.getValue(ProjectorBlock.SIZE);
         float w = (float)ProjectorBlock.SIZE_WIDTH[size];
         float h = w * 9.0F / 16.0F;
         float d = Math.max(3.0F, w * 0.9F);
         float base = 1.0F;
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) -> {
            Draw.box(b, p, -0.30F, 0.0F, -0.30F, 0.30F, 0.32F, 0.30F, 0xFF2A2A30, light);                    // body
            Draw.box(b, p, -0.09F, 0.08F, -0.36F, 0.09F, 0.26F, -0.30F, 0xFF0D0D10, light);                  // lens barrel
            Draw.box(b, p, -0.06F, 0.11F, -0.365F, 0.06F, 0.23F, -0.36F, on ? 0xFFBFE8FF : 0xFF2A3540, on ? FULL_BRIGHT : light); // lens
            Draw.box(b, p, -w / 2 - 0.2F, base - 0.2F, -d - 0.16F, w / 2 + 0.2F, base + h + 0.2F, -d - 0.02F, 0xFF0B0B0D, light); // frame
            float post = Math.max(0.15F, w * 0.02F);
            Draw.box(b, p, -w * 0.4F - post, 0.0F, -d - 0.15F, -w * 0.4F + post, base - 0.2F, -d - 0.05F, 0xFF3A3A3E, light); // posts
            Draw.box(b, p, w * 0.4F - post, 0.0F, -d - 0.15F, w * 0.4F + post, base - 0.2F, -d - 0.05F, 0xFF3A3A3E, light);
         });
         picCentre = new Vector3f(0.0F, base + h / 2.0F, -d);
         picNormal = new Vector3f(0.0F, 0.0F, 1.0F);
         picW = w;
         picH = h;
      }

      final Vector3f c = picCentre;
      final Vector3f n = picNormal;
      final float pw = picW;
      final float ph = picH;
      if (input > 0 && on) {
         TvScreens.Screen pic = hdmiPic;
         if (pic != null && pic.width() > 0 && pic.height() > 0) {
            float aspect = (float)pic.width() / pic.height();
            float dw = Math.min(pw, ph * aspect);
            float dh = Math.min(ph, pw / aspect);
            float u1 = (float)pic.width() / pic.texWidth();
            float v1 = (float)pic.height() / pic.texHeight();
            Vector3f front = new Vector3f(c).add(new Vector3f(n).mul(0.002F));
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) ->
               Draw.quad(b, p, c, n, pw, ph, 0xFF000000, FULL_BRIGHT, 0, 0, 1, 1));
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(pic.texture()), (p, b) ->
               Draw.quad(b, p, front, n, dw, dh, 0xFFFFFFFF, FULL_BRIGHT, 0.0F, 0.0F, u1, v1));
         } else {
            // "No signal": port empty, console off or not running yet
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) ->
               Draw.quad(b, p, c, n, pw, ph, 0xFF101C3A, FULL_BRIGHT, 0, 0, 1, 1));
         }
      } else if (live) {
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(session.texId), (p, b) ->
            Draw.quad(b, p, c, n, pw, ph, 0xFFFFFFFF, FULL_BRIGHT, 0.0F, 0.0F, 1.0F, 1.0F));
         if (state.blockPos.equals(InputFx.controlling)) {
            // cursor drawn on the in-world screen itself
            Vector3f up = new Vector3f(0, 1, 0);
            Vector3f right = new Vector3f(up).cross(n);
            float cs = Math.max(0.012F, pw * 0.012F);
            Vector3f at = new Vector3f(c)
               .add(new Vector3f(right).mul((InputFx.cursorX - 0.5F) * pw + cs / 2F))
               .add(new Vector3f(up).mul((0.5F - InputFx.cursorY) * ph - cs / 2F))
               .add(new Vector3f(n).mul(0.004F));
            Vector3f at2 = new Vector3f(at).add(new Vector3f(n).mul(0.002F));
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) -> {
               Draw.quad(b, p, at, n, cs * 1.4F, cs * 1.4F, 0xFF000000, FULL_BRIGHT, 0, 0, 1, 1);
               Draw.quad(b, p, at2, n, cs, cs, 0xFFFFFFFF, FULL_BRIGHT, 0, 0, 1, 1);
            });
         }
      } else {
         // off: projector screen is plain white canvas, TV is black glass; on but loading: dark blue
         int color = on ? 0xFF0A1630 : (block.kind == MediaBlock.Kind.TV ? 0xFF050506 : 0xFFD9D9D6);
         int lit = on ? FULL_BRIGHT : light;
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(WHITE), (p, b) ->
            Draw.quad(b, p, c, n, pw, ph, color, lit, 0.0F, 0.0F, 1.0F, 1.0F));
      }
      pose.popPose();
   }
}
