package com.dustin.wcore.client;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.GeoItemRenderer.RenderData;
import com.geckolib.renderer.base.GeoRenderState;
import java.nio.file.Path;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CaseRenderer<T extends Item & GeoAnimatable> extends GeoItemRenderer<T> {
   private static final DataTicket<Identifier> COVER = DataTicket.create("wcore_case_cover", Identifier.class);
   private final Identifier baseTexture;
   private final CoverArt.Panel panel;
   private final Function<ItemStack, Path> gameFile;

   public CaseRenderer(Identifier model, CoverArt.Panel panel, Function<ItemStack, Path> gameFile) {
      super(new DefaultedItemGeoModel<T>(model) {
         public Identifier getTextureResource(GeoRenderState renderState) {
            Identifier t = (Identifier)renderState.getGeckolibData(CaseRenderer.COVER);
            return t != null ? t : super.getTextureResource(renderState);
         }
      });
      this.baseTexture = model.withPath("textures/item/" + model.getPath() + ".png");
      this.panel = panel;
      this.gameFile = gameFile;
   }

   public void captureDefaultRenderState(T animatable, RenderData data, GeoRenderState renderState, float partialTick) {
      super.captureDefaultRenderState(animatable, data, renderState, partialTick);
      Path game = data.itemStack() == null ? null : this.gameFile.apply(data.itemStack());
      renderState.addGeckolibData(COVER, CoverArt.texture(game, this.baseTexture, this.panel));
   }
}
