package com.dustin.wcore.retro;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class RetroCommands {
   private RetroCommands() {
   }

   public static void register() {
      CommandRegistrationCallback.EVENT
         .register(
            (CommandRegistrationCallback)(dispatcher, context, selection) -> dispatcher.register(
               (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("consoles")
                           .executes(ctx -> {
                              listAll(((CommandSourceStack)ctx.getSource()).getPlayerOrException());
                              return 1;
                           }))
                        .then(
                           Commands.literal("write")
                              .then(
                                 Commands.argument("system", StringArgumentType.word())
                                    .then(
                                       Commands.argument("file", StringArgumentType.greedyString())
                                          .executes(
                                             ctx -> {
                                                write(
                                                   ((CommandSourceStack)ctx.getSource()).getPlayerOrException(),
                                                   StringArgumentType.getString(ctx, "system"),
                                                   StringArgumentType.getString(ctx, "file")
                                                );
                                                return 1;
                                             }
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("get")
                           .then(
                              Commands.argument("system", StringArgumentType.word())
                                 .then(Commands.argument("file", StringArgumentType.greedyString()).executes(ctx -> {
                                    ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                                    RetroSystem s = RetroSystem.get(StringArgumentType.getString(ctx, "system"));
                                    String file = StringArgumentType.getString(ctx, "file");
                                    if (!p.isCreative()) {
                                       p.sendSystemMessage(
                                          Component.literal("In survival, craft a blank one and use it to write a game.").withStyle(ChatFormatting.GOLD)
                                       );
                                       return 0;
                                    }

                                    if (s != null && s.game(file) != null) {
                                       ItemStack stack = RetroMediaItem.of(s, file);
                                       if (!p.getInventory().add(stack)) {
                                          Block.popResource(p.level(), p.blockPosition(), stack);
                                       }

                                       return 1;
                                    } else {
                                       return 0;
                                    }
                                 }))
                           )
                     ))
                  .then(
                     Commands.literal("play")
                        .then(
                           Commands.argument("system", StringArgumentType.word())
                              .then(Commands.argument("file", StringArgumentType.greedyString()).executes(ctx -> {
                                 ServerPlayer p = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                                 RetroSystem s = RetroSystem.get(StringArgumentType.getString(ctx, "system"));
                                 String file = StringArgumentType.getString(ctx, "file");
                                 if (s != null && s.game(file) != null) {
                                    RetroConsoleBlockEntity console = nearest(p, s.id);
                                    if (console == null) {
                                       p.sendSystemMessage(Component.literal("No " + s.name + " within 12 blocks").withStyle(ChatFormatting.RED));
                                       return 0;
                                    } else {
                                       RetroConsoleBlock.insert(console, s.id, file, p, p.level(), console.getBlockPos());
                                       return 1;
                                    }
                                 } else {
                                    return 0;
                                 }
                              }))
                        )
                  )
            )
         );
   }

   private static void listAll(ServerPlayer p) {
      if (RetroSystem.all().isEmpty()) {
         p.sendSystemMessage(Component.literal("No console mods with game lists are installed").withStyle(ChatFormatting.GRAY));
      } else {
         for (RetroSystem s : RetroSystem.all()) {
            p.sendSystemMessage(Component.literal("── " + s.name + " ──").withStyle(ChatFormatting.AQUA));
            if (s.needsBios() && s.bios() == null) {
               p.sendSystemMessage(Component.literal("No BIOS in " + s.biosDir()).withStyle(ChatFormatting.RED));
            }

            List<RetroSystem.Game> games = s.games();
            if (games.isEmpty()) {
               p.sendSystemMessage(Component.literal("No games in " + s.gamesDir()).withStyle(ChatFormatting.GRAY));
            }

            for (RetroSystem.Game g : games) {
               String file = g.file().getFileName().toString();
               MutableComponent line = Component.literal(" • " + g.title() + "  ").withStyle(ChatFormatting.WHITE);
               line.append(link("[Play]", "/consoles play " + s.id + " " + file, "Put it in the nearest " + s.name + " and turn it on", ChatFormatting.GREEN));
               if (p.isCreative()) {
                  line.append(Component.literal(" "));
                  line.append(link("[Get]", "/consoles get " + s.id + " " + file, "A copy for your inventory", ChatFormatting.GOLD));
               }

               p.sendSystemMessage(line);
            }
         }
      }
   }

   public static void listForWriting(ServerPlayer p, RetroSystem s) {
      List<RetroSystem.Game> games = s.games();
      p.sendSystemMessage(Component.literal("── Write a " + s.name + " " + s.mediaWord + " ──").withStyle(ChatFormatting.AQUA));
      if (games.isEmpty()) {
         p.sendSystemMessage(Component.literal("No games in " + s.gamesDir() + " yet").withStyle(ChatFormatting.GRAY));
      }

      for (RetroSystem.Game g : games) {
         String file = g.file().getFileName().toString();
         p.sendSystemMessage(
            Component.literal(" • ")
               .append(
                  link(g.title(), "/consoles write " + s.id + " " + file, "Put this game on the blank " + s.mediaWord + " you're holding", ChatFormatting.GREEN)
               )
         );
      }
   }

   private static void write(ServerPlayer p, String systemId, String file) {
      RetroSystem s = RetroSystem.get(systemId);
      if (s != null && s.game(file) != null) {
         for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = p.getItemInHand(hand);
            if (held.getItem() instanceof RetroMediaItem m && m.systemId.equals(systemId) && RetroMediaItem.gameOf(held).isEmpty()) {
               held.shrink(1);
               ItemStack written = RetroMediaItem.of(s, file);
               if (held.isEmpty()) {
                  p.setItemInHand(hand, written);
               } else if (!p.getInventory().add(written)) {
                  Block.popResource(p.level(), p.blockPosition(), written);
               }

               p.sendSystemMessage(Component.literal("Wrote " + RetroSystem.title(file)).withStyle(ChatFormatting.GREEN));
               return;
            }
         }

         p.sendSystemMessage(Component.literal("Hold a blank " + s.name + " " + s.mediaWord + " first").withStyle(ChatFormatting.GOLD));
      } else {
         p.sendSystemMessage(Component.literal("That game isn't in the games folder").withStyle(ChatFormatting.RED));
      }
   }

   private static MutableComponent link(String text, String command, String hover, ChatFormatting color) {
      return Component.literal(text)
         .withStyle(st -> st.withColor(color).withClickEvent(new RunCommand(command)).withHoverEvent(new ShowText(Component.literal(hover))));
   }

   private static RetroConsoleBlockEntity nearest(ServerPlayer p, String system) {
      RetroConsoleBlockEntity best = null;
      double bestD = 144.0;
      BlockPos c = p.blockPosition();

      for (BlockPos pos : BlockPos.betweenClosed(c.offset(-12, -4, -12), c.offset(12, 4, 12))) {
         if (p.level().getBlockEntity(pos) instanceof RetroConsoleBlockEntity be && be.block() != null && be.block().accepts(system)) {
            double d = pos.distSqr(c);
            if (d < bestD) {
               bestD = d;
               best = be;
            }
         }
      }

      return best;
   }
}
