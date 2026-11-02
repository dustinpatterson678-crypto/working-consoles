package com.dustin.gamecube;

import com.dustin.gamecube.block.GameCubeBlock;
import com.dustin.gamecube.block.GameCubeBlockEntity;
import com.dustin.gamecube.item.DiscCaseItem;
import com.dustin.wcore.WorkingConsolesCore;
import com.dustin.wcore.item.GeoBlockItem;
import com.dustin.wcore.item.ManualItem;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.function.Consumer;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.Set;
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

public class GameCubeMod implements ModInitializer {
   public static final String MOD_ID = "gamecube";
   public static final String VERSION = "0.9.5";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
   public static Consumer<BlockPos> CLIENT_FOCUS = pos -> {};

   public static final ResourceKey<Block> CONSOLE_KEY = ResourceKey.create(Registries.BLOCK, id("console"));
   public static final GameCubeBlock CONSOLE = (GameCubeBlock)Registry.register(
      BuiltInRegistries.BLOCK, CONSOLE_KEY, new GameCubeBlock(Properties.of().setId(CONSOLE_KEY).strength(2.0F).noOcclusion().sound(SoundType.METAL))
   );
   public static final BlockEntityType<GameCubeBlockEntity> CONSOLE_BLOCK_ENTITY = (BlockEntityType<GameCubeBlockEntity>)Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, id("console"), new BlockEntityType(GameCubeBlockEntity::new, Set.of(CONSOLE))
   );
   public static final ResourceKey<Item> CONSOLE_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("console"));
   public static final Item CONSOLE_ITEM = (Item)Registry.register(
      BuiltInRegistries.ITEM,
      CONSOLE_ITEM_KEY,
      new GeoBlockItem(CONSOLE, id("console"), new Item.Properties().setId(CONSOLE_ITEM_KEY).useBlockDescriptionPrefix().stacksTo(1))
   );
   public static final ResourceKey<Item> DISC_CASE_KEY = ResourceKey.create(Registries.ITEM, id("disc_case"));
   public static final Item DISC_CASE = (Item)Registry.register(
      BuiltInRegistries.ITEM, DISC_CASE_KEY, new DiscCaseItem(new Item.Properties().setId(DISC_CASE_KEY).stacksTo(1))
   );
   // the GameCube pad: 4 ports, analog L/R, Z on the right shoulder, C-stick on the right stick
   public static final com.dustin.wcore.controller.RegisteredController CONTROLLER = WorkingConsolesCore.registerController(
      com.dustin.wcore.controller.ControllerType.builder(id("gamecube_controller"))
         .systems("gamecube")
         .ports(4)
         .bone("stick_l", new com.dustin.wcore.controller.BoneAction.Stick(com.dustin.wcore.controller.BoneAction.LX, com.dustin.wcore.controller.BoneAction.LY, 0, 0.38F))
         .bone("stick_c", new com.dustin.wcore.controller.BoneAction.Stick(com.dustin.wcore.controller.BoneAction.RX, com.dustin.wcore.controller.BoneAction.RY, 0, 0.38F))
         .bone("button_a", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.A, -0.18F))
         .bone("button_b", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.B, -0.15F))
         .bone("button_x", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.X, -0.15F))
         .bone("button_y", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.Y, -0.15F))
         .bone("button_z", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.RB, -0.15F))
         .bone("button_start", new com.dustin.wcore.controller.BoneAction.Press(com.dustin.wcore.client.ControllerInput.START, -0.1F))
         .bone("trigger_l", new com.dustin.wcore.controller.BoneAction.AnalogPress(com.dustin.wcore.controller.BoneAction.LT, -0.4F))
         .bone("trigger_r", new com.dustin.wcore.controller.BoneAction.AnalogPress(com.dustin.wcore.controller.BoneAction.RT, -0.4F))
         .bone("dpad", new com.dustin.wcore.controller.BoneAction.Dpad(0.15F))
         .build()
   );
   public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("main"));

   @Override
   public void onInitialize() {
      GameCubeFiles.ensureFolders();
      WorkingConsolesCore.LOGGER.info("[GameCube] Version {}", VERSION);
      ManualItem.register(
         "gamecube",
         "GameCube Manual",
         new String[]{
            "GAMECUBE CONSOLE\n\n1. Cable it to a TV and to a Wall Outlet.\n2. Power the outlet's block with redstone.\n3. Put your own GameCube disc images in gamecube/isos.",
            "DISCS\n\nCraft a blank disc case, right-click it and pick a game. Click it into the console to load it.\n\nSneak + right-click: eject.\nRight-click: power.",
            "SETTINGS\n\nMods > Working Consoles Core > Configure > GameCube emulator settings: every setting of the emulator.\n\nBetter redstone power gives the GameCube more speed."
         }
      );
      Registry.register(
         BuiltInRegistries.CREATIVE_MODE_TAB,
         TAB_KEY,
         FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.gamecube.main"))
            .icon(() -> new ItemStack(CONSOLE_ITEM))
            .displayItems((context, entries) -> {
               entries.accept(CONSOLE_ITEM);
               entries.accept(new ItemStack(DISC_CASE));
               entries.accept(CONTROLLER.item());
               entries.accept(ManualItem.of(WorkingConsolesCore.MANUAL, "gamecube"));

               for (GameCubeFiles.Disc d : GameCubeFiles.discs()) {
                  entries.accept(DiscCaseItem.caseFor(d));
               }
            })
            .build()
      );
      CommandRegistrationCallback.EVENT
         .register(
            (CommandRegistrationCallback)(dispatcher, context, selection) -> dispatcher.register(
               (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("gc").executes(ctx -> {
                  list(((CommandSourceStack)ctx.getSource()).getPlayerOrException());
                  return 1;
               })).then(Commands.literal("case").then(Commands.argument("file", StringArgumentType.greedyString()).executes(ctx -> {
                  ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                  GameCubeFiles.Disc d = GameCubeFiles.disc(StringArgumentType.getString(ctx, "file"));
                  if (d == null) {
                     p.sendSystemMessage(Component.literal("That disc image isn't in gamecube/isos").withStyle(ChatFormatting.RED));
                     return 0;
                  }

                  ItemStack stack = DiscCaseItem.caseFor(d);
                  if (!p.getInventory().add(stack)) {
                     Block.popResource(p.level(), p.blockPosition(), stack);
                  }

                  return 1;
               })))).then(Commands.literal("write").then(Commands.argument("file", StringArgumentType.greedyString()).executes(ctx -> {
                  ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                  GameCubeFiles.Disc d = GameCubeFiles.disc(StringArgumentType.getString(ctx, "file"));
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
                  if (GameCubeFiles.disc(file) == null) {
                     p.sendSystemMessage(Component.literal("That disc image isn't in gamecube/isos").withStyle(ChatFormatting.RED));
                     return 0;
                  }

                  GameCubeBlockEntity console = nearest(p);
                  if (console == null) {
                     p.sendSystemMessage(Component.literal("No GameCube console within 12 blocks").withStyle(ChatFormatting.RED));
                     return 0;
                  }

                  GameCubeBlock.insert(console, file, p, p.level(), console.getBlockPos());
                  if (!console.isOn()) {
                     GameCubeBlock.togglePower(console, p, p.level(), console.getBlockPos());
                  }

                  return 1;
               })))
            )
         );
      LOGGER.info("GameCube in Minecraft loaded. Disc folder: {}", GameCubeFiles.root());
   }

   public static void listForWriting(ServerPlayer p) {
      p.sendSystemMessage(Component.literal("⬜ Write a GameCube disc ⬜").withStyle(ChatFormatting.AQUA));
      List<GameCubeFiles.Disc> discs = GameCubeFiles.discs();
      if (discs.isEmpty()) {
         p.sendSystemMessage(Component.literal("No disc images in " + GameCubeFiles.discDir() + " (.iso .gcm .gcz .ciso .wbfs .rvz .nkit)").withStyle(ChatFormatting.GRAY));
      }

      for (GameCubeFiles.Disc d : discs) {
         p.sendSystemMessage(
            Component.literal(" ▸ ")
               .append(
                  link(d.title(), "/gc write " + d.file().getFileName(), "Put this game on the blank disc case you're holding", ChatFormatting.GREEN)
               )
         );
      }
   }

   private static void list(ServerPlayer p) {
      p.sendSystemMessage(Component.literal("⬜ Your GameCube discs ⬜").withStyle(ChatFormatting.AQUA));
      List<GameCubeFiles.Disc> discs = GameCubeFiles.discs();
      if (discs.isEmpty()) {
         p.sendSystemMessage(Component.literal("No disc images in " + GameCubeFiles.discDir() + " (.iso .gcm .gcz .ciso .wbfs .rvz .nkit)").withStyle(ChatFormatting.GRAY));
      }

      for (GameCubeFiles.Disc d : discs) {
         String file = d.file().getFileName().toString();
         MutableComponent line = Component.literal(" ▸ " + d.title() + "  ").withStyle(ChatFormatting.WHITE);
         line.append(link("[Play]", "/gc play " + file, "Put this disc in the nearest console and power it on", ChatFormatting.GREEN));
         line.append(Component.literal(" "));
         line.append(link("[Get case]", "/gc case " + file, "Put this game's disc case in your inventory", ChatFormatting.GOLD));
         p.sendSystemMessage(line);
      }
   }

   private static MutableComponent link(String text, String command, String hover, ChatFormatting color) {
      return Component.literal(text)
         .withStyle(st -> st.withColor(color).withClickEvent(new RunCommand(command)).withHoverEvent(new ShowText(Component.literal(hover))));
   }

   private static GameCubeBlockEntity nearest(ServerPlayer p) {
      GameCubeBlockEntity best = null;
      double bestD = 144.0;
      BlockPos c = p.blockPosition();

      for (BlockPos pos : BlockPos.betweenClosed(c.offset(-12, -4, -12), c.offset(12, 4, 12))) {
         if (p.level().getBlockEntity(pos) instanceof GameCubeBlockEntity be && be.block() != null) {
            double d = pos.distSqr(c);
            if (d < bestD) {
               bestD = d;
               best = be;
            }
         }
      }

      return best;
   }

   private static ResourceKey<Item> itemKey(String path) {
      return ResourceKey.create(Registries.ITEM, id(path));
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }
}