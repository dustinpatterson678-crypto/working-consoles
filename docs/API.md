# Working Consoles modder API

Add your own consoles, controllers, TV pictures, arcade payouts and prizes to Working Consoles and Dustin's Arcade.
Use the classes in `com.dustin.wcore.api` and `com.dustin.arcadeplus.api`. They are kept stable between updates.
Everything else is internal and may change.

API version: `WorkingConsolesApi.VERSION` = 1, `ArcadeApi.VERSION` = 1.

## 1. Depend on the mod

In your `fabric.mod.json`:

```json
"depends": { "wcore": "*" },
"entrypoints": {
  "workingconsoles": ["com.example.mymod.MyConsoles"],
  "workingconsoles_client": ["com.example.mymod.client.MyConsolesClient"]
}
```

Add `"dustinsarcade": "*"` as well if you use the arcade API. Compile against the release jars (`working-consoles-core-<version>.jar`, plus `dustins-arcade-<version>.jar` for the arcade). Put them in `libs/` and add `modCompileOnly files(...)`.

## 2. Register on the server/common side

Working Consoles calls your plugin once while it starts:

```java
public class MyConsoles implements WorkingConsolesPlugin {
   public static RegisteredController PAD;

   @Override
   public void register() {
      // a console system (libretro core + games folder), same constructor the built-in consoles use
      WorkingConsolesApi.registerSystem(new RetroSystem(/* id, name, core, folder, media, extensions, bios, options, fps, help */));

      // its controller: item + placeable block, 2 ports, buttons that move with the player's pad
      PAD = WorkingConsolesApi.registerController(
         WorkingConsolesApi.controller(Identifier.fromNamespaceAndPath("mymod", "my_pad"))
            .systems("mysystem")
            .ports(2)
            .bone("button_a", new BoneAction.Press(ControllerInput.A, -0.15F))
            .bone("dpad", new BoneAction.Dpad(0.15F))
            .build());

      // arcade: a prize in the booth and a listener for every win
      ArcadeApi.addPrize("mymod:golden_joystick", 1, 2500);
      ArcadeApi.TICKETS_WON.register((level, machine, game, tickets, jackpot) -> { /* your reaction */ });
   }
}
```

Controller assets live in your own namespace:
- `assets/mymod/geckolib/models/item/my_pad.geo.json`. Bone names must match your `.bone(...)` calls.
- `geckolib/animations/item/my_pad.animation.json` (an `idle` animation).
- `textures/item/my_pad.png`.
- `items/my_pad.json`, `blockstates/my_pad.json`, `lang`.

`tools/modelgen/padgen.py` in this repo generates all of these.

## 3. Register on the client

```java
public class MyConsolesClient implements WorkingConsolesClientPlugin {
   @Override
   public void registerClient() {
      WorkingConsolesClientApi.registerControllerRenderer(MyConsoles.PAD);
   }
}
```

To show your own picture on the TVs cabled to a console:
1. Call `WorkingConsolesClientApi.setTvPump(consolePos, () -> { upload next frame; showOnTv(...) })`.
2. The pump runs on the render thread whenever a TV draws that console. It is the only place you may touch OpenGL.
3. Call `clearTv(consolePos)` and `setTvPump(consolePos, null)` when your game stops.

## 4. Pay tickets from your own machine

On the server:

```java
ArcadeApi.payPoints(level, machinePos, points);
```

Points become paper tickets at the arcade's rate. The payout also feeds the Mega Jackpot and may hit it, fires `TICKETS_WON`, and the tickets come out of your machine.

The ticket card:
- `ArcadeApi.ticketCard(player)` reads a player's card.
- `ArcadeApi.addToTicketCard(player, n)` changes it.

## Rules

- Keep all ids in your own namespace.
- Never call OpenGL outside a TV pump. That exact mistake used to crash the game when a TV cable was plugged in.
- A plugin that throws is logged and skipped. It never takes the game down.
