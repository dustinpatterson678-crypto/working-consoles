package com.dustin.stream;

import com.dustin.stream.net.MediaActionPacket;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 6.x Streaming Update: a real Chromium browser running inside Minecraft (MCEF Modern),
 * drawn on Smart TVs and Cinema Projector screens, with an in-world keyboard and mouse.
 * Nothing is captured from the desktop.
 */
public class StreamMod implements ModInitializer {
   public static final String MOD_ID = "stream";

   public static final MediaBlock SMART_TV = block("smart_tv", p -> new MediaBlock(p, MediaBlock.Kind.TV), 2.0F);
   public static final ProjectorBlock PROJECTOR = block("projector", ProjectorBlock::new, 2.0F);
   public static final DeskBlock KEYBOARD = block("keyboard", p -> new DeskBlock(p, DeskBlock.Kind.KEYBOARD), 0.5F);
   public static final DeskBlock MOUSE = block("mouse", p -> new DeskBlock(p, DeskBlock.Kind.MOUSE), 0.5F);

   public static final Item SMART_TV_ITEM = item("smart_tv", SMART_TV);
   public static final Item PROJECTOR_ITEM = item("projector", PROJECTOR);
   public static final Item KEYBOARD_ITEM = item("keyboard", KEYBOARD);
   public static final Item MOUSE_ITEM = item("mouse", MOUSE);
   public static final Item HDMI_CABLE = Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, id("hdmi_cable")),
      new HdmiCableItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("hdmi_cable"))).stacksTo(16)));

   public static BlockEntityType<MediaBlockEntity> MEDIA_BE;
   public static BlockEntityType<DeskBlockEntity> DESK_BE;

   static {
      MEDIA_BE = beType("media", MediaBlockEntity::new, SMART_TV, PROJECTOR);
      DESK_BE = beType("desk", DeskBlockEntity::new, KEYBOARD, MOUSE);
   }

   public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main"));

   @Override
   public void onInitialize() {
      PayloadTypeRegistry.serverboundPlay().register(MediaActionPacket.TYPE, MediaActionPacket.CODEC);
      UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
         ItemStack held = player.getItemInHand(hand);
         if (held.getItem() instanceof com.dustin.wcore.item.CableItem) {
            forgetStaleCableEnd(level, held);
            return InteractionResult.PASS;
         }
         if (!(held.getItem() instanceof HdmiCableItem) || player.isSpectator()) {
            return InteractionResult.PASS;
         }
         return HdmiCableItem.handle(level, player, hit.getBlockPos(), held);
      });
      ServerPlayNetworking.registerGlobalReceiver(MediaActionPacket.TYPE, (payload, context) -> MediaActionPacket.handle(payload, context.player()));
      Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, FabricCreativeModeTab.builder()
         .title(Component.literal("Streaming"))
         .icon(() -> new ItemStack(SMART_TV_ITEM))
         .displayItems((context, entries) -> {
            entries.accept(SMART_TV_ITEM);
            entries.accept(PROJECTOR_ITEM);
            entries.accept(KEYBOARD_ITEM);
            entries.accept(MOUSE_ITEM);
            entries.accept(HDMI_CABLE);
         })
         .build());
   }

   private static <B extends Block> B block(String name, java.util.function.Function<BlockBehaviour.Properties, B> make, float strength) {
      ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
      return Registry.register(BuiltInRegistries.BLOCK, key,
         make.apply(BlockBehaviour.Properties.of().setId(key).strength(strength).noOcclusion().sound(SoundType.METAL)));
   }

   private static Item item(String name, Block block) {
      ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
      return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static <T extends BlockEntity> BlockEntityType<T> beType(String name, java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, T> make, Block... blocks) {
      return (BlockEntityType<T>)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id(name),
         new BlockEntityType((p, s) -> make.apply((net.minecraft.core.BlockPos)p, (net.minecraft.world.level.block.state.BlockState)s), Set.of(blocks)));
   }

   /**
    * A Cable remembers the block it was first clicked on. If that block was broken or replaced
    * (like the old Stream Box), every click after says "those don't connect". Forget it instead.
    */
   private static void forgetStaleCableEnd(net.minecraft.world.level.Level level, ItemStack cable) {
      net.minecraft.world.item.component.CustomData data = cable.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
      long from = data == null ? Long.MIN_VALUE : data.copyTag().getLongOr("from", Long.MIN_VALUE);
      if (from == Long.MIN_VALUE) {
         return;
      }
      net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.of(from);
      if (!level.isLoaded(pos)) {
         return;
      }
      BlockEntity be = level.getBlockEntity(pos);
      boolean ok = be instanceof com.dustin.wcore.block.ConsoleBlockEntity || be instanceof com.dustin.wcore.block.TvBlockEntity
         || level.getBlockState(pos).getBlock() instanceof com.dustin.wcore.block.OutletBlock;
      if (!ok) {
         cable.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
      }
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }
}
