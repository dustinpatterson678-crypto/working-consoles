package com.dustin.wcore.client;

import com.dustin.wcore.controller.BoneAction;
import com.dustin.wcore.controller.ControllerType;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.GeoItemRenderer.RenderData;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Item renderer for every controller type: the type's GeckoLib model, its glowmask (if it ships one) and its bones
 * moved from the pad input. ControllerBlockRenderer draws placed controllers with the same bone logic.
 */
public class ControllerRenderer<T extends Item & GeoAnimatable> extends GeoItemRenderer<T> {
   public static final DataTicket<ItemStack> STACK = DataTicket.create("wcore_controller_stack", ItemStack.class);
   public static final DataTicket<ItemOwner> HOLDER = DataTicket.create("wcore_controller_holder", ItemOwner.class);
   private static final float STICK_PRESS = -0.18F;
   private static final float DPAD_SINK = 0.6F;

   /** Where a held controller's bone animation reads its input from. */
   @FunctionalInterface
   public interface InputSource {
      /** holder is null in GUIs and item frames; return null for no movement. */
      ControllerInput.State stateFor(ItemStack stack, ItemOwner holder);
   }

   /** Where a placed controller's bone animation reads its input from. */
   @FunctionalInterface
   public interface PlacedInputSource {
      ControllerInput.State stateFor(BlockPos pos, ItemStack stack);
   }

   /**
    * Input hook for held controllers. For now every controller shows the first local pad, like the wireless
    * controller always did; the input-routing stage replaces this with a per-stack lookup (paired pad / relayed
    * remote input).
    */
   public static ControllerRenderer.InputSource itemInput = (stack, holder) -> ControllerInput.state();
   /** Input hook for placed controllers. Default: they lie still. */
   public static ControllerRenderer.PlacedInputSource placedInput = (pos, stack) -> null;

   private final ControllerType type;

   public ControllerRenderer(ControllerType type) {
      super(ControllerRenderer.<T>model(type));
      this.type = type;
      if (type.glowmask()) {
         this.withRenderLayer(new AutoGlowingGeoLayer<T, RenderData, GeoRenderState>(this) {
            protected RenderType getRenderType(GeoRenderState renderState) {
               return ShaderCompat.glow(this.getTextureResource(renderState));
            }
         });
      }
   }

   /** The GeckoLib model of a controller type (also used by ControllerBlockRenderer). */
   public static <A extends GeoAnimatable> DefaultedItemGeoModel<A> model(ControllerType type) {
      return new DefaultedItemGeoModel<A>(type.model()).withAltTexture(type.texture()).withAltAnimations(type.animation());
   }

   /** The hook the input-routing stage replaces; see itemInput. */
   public static ControllerInput.State inputFor(ItemStack stack, ItemOwner holder) {
      return itemInput.stateFor(stack, holder);
   }

   public ControllerType type() {
      return this.type;
   }

   public void captureDefaultRenderState(T animatable, RenderData data, GeoRenderState renderState, float partialTick) {
      super.captureDefaultRenderState(animatable, data, renderState, partialTick);
      renderState.addGeckolibData(STACK, data.itemStack() == null ? ItemStack.EMPTY : data.itemStack());
      if (data.itemOwner() != null) {
         renderState.addGeckolibData(HOLDER, data.itemOwner());
      }
   }

   public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> renderPassInfo, BoneSnapshots snapshots) {
      GeoRenderState state = renderPassInfo.renderState();
      ItemStack stack = state.getOrDefaultGeckolibData(STACK, ItemStack.EMPTY);
      ItemOwner holder = state.hasGeckolibData(HOLDER) ? state.getGeckolibData(HOLDER) : null;
      applyBones(this.type.bones(), inputFor(stack, holder), snapshots);
   }

   /** Moves the bones of a controller model from one pad state (null or disconnected = no movement). */
   public static void applyBones(Map<String, BoneAction> bones, ControllerInput.State s, BoneSnapshots snapshots) {
      if (s == null || !s.connected) {
         return;
      }

      for (Map.Entry<String, BoneAction> e : bones.entrySet()) {
         String bone = e.getKey();
         switch (e.getValue()) {
            case BoneAction.Press p -> {
               if (s.down(p.mask())) {
                  snapshots.get(bone).ifPresent(b -> b.setTranslateY(p.dy()));
               }
            }
            case BoneAction.Stick st -> {
               float x = axis(s, st.axisX());
               float y = axis(s, st.axisY());
               boolean click = st.clickMask() != 0 && s.down(st.clickMask());
               snapshots.get(bone).ifPresent(b -> b.setRotation(-y * st.tilt(), 0.0F, x * st.tilt()).setTranslateY(click ? STICK_PRESS : 0.0F));
            }
            case BoneAction.Trigger t -> {
               float v = axis(s, t.axis());
               snapshots.get(bone).ifPresent(b -> b.setRotX(v * t.rot()));
            }
            case BoneAction.Dpad d -> {
               if ((s.buttons & (ControllerInput.UP | ControllerInput.DOWN | ControllerInput.LEFT | ControllerInput.RIGHT)) != 0) {
                  float rx = (s.down(ControllerInput.UP) ? d.tilt() : 0.0F) + (s.down(ControllerInput.DOWN) ? -d.tilt() : 0.0F);
                  float rz = (s.down(ControllerInput.LEFT) ? -d.tilt() : 0.0F) + (s.down(ControllerInput.RIGHT) ? d.tilt() : 0.0F);
                  snapshots.get(bone).ifPresent(b -> b.setRotation(rx, 0.0F, rz).setTranslateY(-d.tilt() * DPAD_SINK));
               }
            }
            case BoneAction.AnalogPress a -> {
               float v = axis(s, a.axis());
               snapshots.get(bone).ifPresent(b -> b.setTranslateY(v * a.dy()));
            }
         }
      }
   }

   private static float axis(ControllerInput.State s, int axis) {
      return switch (axis) {
         case BoneAction.LX -> s.lx;
         case BoneAction.LY -> s.ly;
         case BoneAction.RX -> s.rx;
         case BoneAction.RY -> s.ry;
         case BoneAction.LT -> s.lt;
         case BoneAction.RT -> s.rt;
         default -> 0.0F;
      };
   }
}
