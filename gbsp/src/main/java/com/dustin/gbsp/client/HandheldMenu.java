package com.dustin.gbsp.client;

import com.dustin.gbsp.emu.SystemFiles;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.OpenFile;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;

public final class HandheldMenu {
   private HandheldMenu() {
   }

   public static void register() {
      ClientCommandRegistrationCallback.EVENT
         .register(
            (ClientCommandRegistrationCallback)(dispatcher, context) -> dispatcher.register(
               (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("gbsp")
                           .then(ClientCommands.literal("list").executes(ctx -> {
                              show(SystemFiles.scan());
                              return 1;
                           })))
                        .then(ClientCommands.literal("play").then(ClientCommands.argument("game", IntegerArgumentType.integer(1)).executes(ctx -> {
                           play(IntegerArgumentType.getInteger(ctx, "game"));
                           return 1;
                        }))))
                     .then(ClientCommands.literal("view").executes(ctx -> {
                        Minecraft.getInstance().execute(HandheldSession::openBigView);
                        return 1;
                     })))
                  .then(ClientCommands.literal("off").executes(ctx -> {
                     HandheldSession.stop();
                     say(Component.literal("Handheld off").withStyle(ChatFormatting.GRAY));
                     return 1;
                  }))
            )
         );
   }

   public static void show(SystemFiles.Report report) {
      List<SystemFiles.Rom> roms = report.roms();
      say(Component.literal("── Handheld: pick a game ──").withStyle(ChatFormatting.AQUA));
      if (!report.ready()) {
         for (Component line : report.describe()) {
            say(line);
         }
      } else {
         for (int i = 0; i < roms.size() && i < 40; i++) {
            SystemFiles.Rom rom = roms.get(i);
            boolean playing = HandheldSession.isOn() && rom.title().equals(HandheldSession.title());
            MutableComponent line = Component.literal((playing ? " ▶ " : " • ") + rom.title())
               .withStyle(playing ? ChatFormatting.GREEN : ChatFormatting.WHITE)
               .append(Component.literal("  " + rom.type()).withStyle(ChatFormatting.DARK_GRAY));
            line = clickable(line, "/gbsp play " + (i + 1), "Boot " + rom.title());
            line.append(Component.literal("  "));
            line.append(
               clickable(
                  Component.literal("[Get case]").withStyle(ChatFormatting.GOLD),
                  "/gbsp_case " + rom.file().getFileName(),
                  "Put this game's case in your inventory"
               )
            );
            say(line);
         }

         MutableComponent actions = Component.literal(" ");
         if (HandheldSession.isOn()) {
            actions.append(
               clickable(Component.literal("[Big screen]").withStyle(ChatFormatting.YELLOW), "/gbsp view", "Show the game full screen (Esc to go back)")
            );
            actions.append(Component.literal("  "));
            actions.append(
               clickable(Component.literal("[Power off]").withStyle(ChatFormatting.RED), "/gbsp off", "Switch the handheld off (save in-game first!)")
            );
            actions.append(Component.literal("  "));
         }

         actions.append(
            Component.literal("[Open games folder]")
               .withStyle(
                  Style.EMPTY
                     .withColor(ChatFormatting.GRAY)
                     .withUnderlined(true)
                     .withClickEvent(new OpenFile(SystemFiles.romDir()))
                     .withHoverEvent(new ShowText(Component.literal("Add your own .gba / .gbc / .gb files here, then right-click again")))
               )
         );
         say(actions);
      }
   }

   private static void play(int number) {
      SystemFiles.Report report = SystemFiles.scan();
      if (!report.ready()) {
         show(report);
      } else if (number > report.roms().size()) {
         say(Component.literal("No game #" + number + ". Right-click the handheld for the list.").withStyle(ChatFormatting.RED));
      } else {
         SystemFiles.Rom rom = report.roms().get(number - 1);
         String err = HandheldSession.start(report, rom);
         if (err != null) {
            say(Component.literal(err).withStyle(ChatFormatting.RED));
         } else {
            say(Component.literal("Booting " + rom.title() + "  (right-click again to switch games)").withStyle(ChatFormatting.GREEN));
         }
      }
   }

   private static MutableComponent clickable(MutableComponent text, String command, String hover) {
      return text.withStyle(style -> style.withClickEvent(new RunCommand(command)).withHoverEvent(new ShowText(Component.literal(hover))));
   }

   private static void say(Component c) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.sendSystemMessage(c);
      }
   }
}
