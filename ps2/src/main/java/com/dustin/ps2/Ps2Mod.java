package com.dustin.ps2;

import com.dustin.ps2.block.Ps2Block;
import com.dustin.ps2.block.Ps2BlockEntity;
import com.dustin.ps2.item.DiscCaseItem;
import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.item.GeoBlockItem;
import com.dustin.wcore.item.ManualItem;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ps2Mod implements ModInitializer {
   public static final String MOD_ID = "ps2";
   public static final Logger LOGGER = LoggerFactory.getLogger("ps2");
   public static Consumer<BlockPos> CLIENT_FOCUS = pos -> {};
   public static final ResourceKey<Block> CONSOLE_KEY = ResourceKey.create(Registries.BLOCK, id("console"));
   public static final Ps2Block CONSOLE = (Ps2Block)Registry.register(
      BuiltInRegistries.BLOCK, CONSOLE_KEY, new Ps2Block(Properties.of().setId(CONSOLE_KEY).strength(2.0F).noOcclusion().sound(SoundType.METAL))
   );
   public static final BlockEntityType<Ps2BlockEntity> CONSOLE_BLOCK_ENTITY = (BlockEntityType<Ps2BlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, id("console"), new BlockEntityType(Ps2BlockEntity::new, Set.of(CONSOLE))
   );
   public static final ResourceKey<Item> CONSOLE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("console"));
   public static final Item CONSOLE_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM,
      CONSOLE_ITEM_KEY,
      new GeoBlockItem(CONSOLE, id("console"), new net.minecraft.world.item.Item.Properties().setId(CONSOLE_ITEM_KEY).useBlockDescriptionPrefix().stacksTo(1))
   );
   public static final ResourceKey<Item> DISC_CASE_KEY = ResourceKey.create(Registries.ITEM, id("disc_case"));
   public static final Item DISC_CASE = (Item)Registry.register(
      BuiltInRegistries.ITEM, DISC_CASE_KEY, new DiscCaseItem(new net.minecraft.world.item.Item.Properties().setId(DISC_CASE_KEY).stacksTo(1))
   );
   public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main"));

   public void onInitialize() {
      Ps2Files.ensureFolders();
      ManualItem.register(
         "ps2",
         "PS2 Manual",
         new String[]{
            "PS2 CONSOLE\n\n1. Cable it to a TV and to a Wall Outlet.\n2. Power the outlet's block with redstone.\n3. Put your own PS2 BIOS dump in ps2/bios and your disc images in ps2/isos.",
            "DISCS\n\nCraft a blank disc case, right-click it and pick a game. Click it into the console to load it.\n\nSneak + right-click: eject.\nRight-click: power.",
            "SETTINGS\n\nMods > Working Consoles Core > Configure > PS2 emulator settings: every setting of the emulator.\n\nBetter redstone power gives the PS2 more speed."
         }
      );
      Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB,
         TAB_KEY,
         FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ps2.main"))
            .icon(() -> new ItemStack(CONSOLE_ITEM))
            .displayItems((context, entries) -> {
               entries.accept(CONSOLE_ITEM);
               entries.accept(new ItemStack(DISC_CASE));
               entries.accept(ManualItem.of(WorkingConsolesCore.MANUAL, "ps2"));

               for (Ps2Files.Disc d : Ps2Files.discs()) {
                  entries.accept(DiscCaseItem.caseFor(d));
               }
            })
            .build()
      );
      CommandRegistrationCallback.EVENT
         .register(
            (CommandRegistrationCallback)(dispatcher, context, selection) -> dispatcher.register(
               (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ps2").executes(ctx -> {
                  list(((CommandSourceStack)ctx.getSource()).getPlayerOrException());
                  return 1;
               })).then(Commands.literal("case").then(Commands.argument("file", StringArgumentType.greedyString()).executes(ctx -> {
                  ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                  Ps2Files.Disc d = Ps2Files.disc(StringArgumentType.getString(ctx, "file"));
                  if (d == null) {
                     p.sendSystemMessage(Component.literal("That disc image isn't in ps2/isos").withStyle(ChatFormatting.RED));
                     return 0;
                  }

                  ItemStack stack = DiscCaseItem.caseFor(d);
                  if (!p.getInventory().add(stack)) {
                     Block.popResource(p.level(), p.blockPosition(), stack);
                  }

                  return 1;
               })))).then(Commands.literal("write").then(Commands.argument("file", StringArgumentType.greedyString()).executes(ctx -> {
                  ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                  Ps2Files.Disc d = Ps2Files.disc(StringArgumentType.getString(ctx, "file"));
                  if (d == null) {
                     return 0;
                  }

                  for (InteractionHand hand : InteractionHand.values()) {
                     ItemStack held = p.getItemInHand(hand);
                     if (held.getItem() instanceof DiscCaseItem && DiscCaseItem.discOf(held).isEmpty()) {
                        held.shrink(1);
                        ItemStack written = DiscCaseItem.caseFor(d);
                        if (held.isEmpty()) {
                           p.setItemInHand(hand, written);
                        } else if (!p.getInventory().add(written)) {
                           Block.popResource(p.level(), p.blockPosition(), written);
                        }

                        p.sendSystemMessage(Component.literal("Wrote " + d.title()).withStyle(ChatFormatting.GREEN));
                        return 1;
                     }
                  }

                  p.sendSystemMessage(Component.literal("Hold a blank disc case first").withStyle(ChatFormatting.GOLD));
                  return 0;
               })))).then(Commands.literal("play").then(Commands.argument("file", StringArgumentType.greedyString()).executes(ctx -> {
                  ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                  String file = StringArgumentType.getString(ctx, "file");
                  if (Ps2Files.disc(file) == null) {
                     p.sendSystemMessage(Component.literal("That disc image isn't in ps2/isos").withStyle(ChatFormatting.RED));
                     return 0;
                  }

                  Ps2BlockEntity console = nearest(p);
                  if (console == null) {
                     p.sendSystemMessage(Component.literal("No console within 12 blocks").withStyle(ChatFormatting.RED));
                     return 0;
                  }

                  Ps2Block.insert(console, file, p, p.level(), console.getBlockPos());
                  if (!console.isOn()) {
                     Ps2Block.togglePower(console, p, p.level(), console.getBlockPos());
                  }

                  return 1;
               })))
            )
         );
      LOGGER.info("PS2 in Minecraft loaded. BIOS / disc folder: {}", Ps2Files.root());
   }

   public static void listForWriting(ServerPlayer p) {
      p.sendSystemMessage(Component.literal("── Write a PS2 disc ──").withStyle(ChatFormatting.AQUA));
      List<Ps2Files.Disc> discs = Ps2Files.discs();
      if (discs.isEmpty()) {
         p.sendSystemMessage(Component.literal("No disc images in " + Ps2Files.discDir()).withStyle(ChatFormatting.GRAY));
      }

      for (Ps2Files.Disc d : discs) {
         p.sendSystemMessage(
            Component.literal(" • ")
               .append(link(d.title(), "/ps2 write " + d.file().getFileName(), "Put this game on the blank case you're holding", ChatFormatting.GREEN))
         );
      }
   }

   private static void list(ServerPlayer p) {
      p.sendSystemMessage(Component.literal("── Your discs ──").withStyle(ChatFormatting.AQUA));
      if (Ps2Files.bios() == null) {
         p.sendSystemMessage(Component.literal("No PS2 BIOS in " + Ps2Files.biosDir() + ": consoles won't power on").withStyle(ChatFormatting.RED));
      }

      List<Ps2Files.Disc> discs = Ps2Files.discs();
      if (discs.isEmpty()) {
         p.sendSystemMessage(Component.literal("No disc images in " + Ps2Files.discDir() + " (.iso .chd .cso)").withStyle(ChatFormatting.GRAY));
      }

      for (Ps2Files.Disc d : discs) {
         String file = d.file().getFileName().toString();
         MutableComponent line = Component.literal(" • " + d.title() + "  ").withStyle(ChatFormatting.WHITE);
         line.append(link("[Play]", "/ps2 play " + file, "Put this disc in the nearest console and power it on", ChatFormatting.GREEN));
         line.append(Component.literal(" "));
         line.append(link("[Get case]", "/ps2 case " + file, "Put this game's disc case in your inventory", ChatFormatting.GOLD));
         p.sendSystemMessage(line);
      }
   }

   private static MutableComponent link(String text, String command, String hover, ChatFormatting color) {
      return Component.literal(text)
         .withStyle(s -> s.withColor(color).withClickEvent(new RunCommand(command)).withHoverEvent(new ShowText(Component.literal(hover))));
   }

   private static Ps2BlockEntity nearest(ServerPlayer p) {
      Ps2BlockEntity best = null;
      double bestD = 144.0;
      BlockPos c = p.blockPosition();

      for (BlockPos pos : BlockPos.betweenClosed(c.offset(-12, -4, -12), c.offset(12, 4, 12))) {
         if (p.level().getBlockEntity(pos) instanceof Ps2BlockEntity be) {
            double d = pos.distSqr(c);
            if (d < bestD) {
               bestD = d;
               best = be;
            }
         }
      }

      return best;
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("ps2", path);
   }
}
