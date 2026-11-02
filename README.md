# Working Consoles Remake

Fully buildable source for the **Working Consoles** modpack — Minecraft **26.3** (Fabric), Java 25.

This tree builds the complete Working Consoles set from source so new features can keep being added — next up is **version 3, the retro update** (see `RETRO_UPDATE.md`):

1. **wcore** — `working-consoles-core` — shared core: TV, Outlet, Cable, Wireless Controller, emulator runtime, ISO/DumpCheck system
2. **gbsp** — `gameboy-in-minecraft` — working GBA/GBC/GB handheld
3. **ps2** — `ps2-in-minecraft` — working PS2 disc console
4. **sega** — `sega-in-minecraft` — working Genesis and Saturn consoles
5. **arcade** — `arcade-in-minecraft` — working arcade cabinets (MAME 2003-Plus)

## Requirements

- JDK 25 at `C:/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot`
  (or change `org.gradle.java.home` in `gradle.properties` to another JDK 25 install)
- Internet access for the first build (Gradle, Minecraft 26.3, Fabric API, Mod Menu)

## Building

```bat
REM Build + auto-install — version 3.0.0+DEV.<yyyyMMdd-HHmm>, developer bypass active
gradlew.bat build

REM Release build — version 3.0.0, developer bypass disabled (also auto-installs)
gradlew.bat build -Prelease=true
```

Every build **installs itself**: the five new jars are copied straight into
`C:\Users\dustin\curseforge\minecraft\Instances\working console\mods`, and any older jar of the same
five mods is removed first (Fabric API and GeckoLib are left alone). Reinstall the current build
anytime with `gradlew.bat deployToInstance`.

The build type can also be toggled in `gradle.properties` (`release=false` / `release=true`).

### Version numbers

Every dev build signs itself with the date and time it was built, and the same stamp is used by
all five jars — so **the newest build is always the highest stamp**:

- `working-consoles-core-3.0.0+DEV.20261018-1830.jar` — dev build (developer bypass active)
- `working-consoles-core-3.0.0.jar` — release build (clean version, bypass off)

Bump `mod_version` in `gradle.properties` when starting a new feature round.

## Build Notes

- Minecraft 26.3 ships **unobfuscated**, so the project uses identity mappings
  (`gradle/mappings/identity-mappings.jar`) plus `noIntermediateMappings()`. Compiled classes keep Mojang
  names — exactly like the shipped jars.
- Dev builds write `wcore-build.properties` (`release=false`, `stamp=DEV-…`) into the wcore jar; release
  builds write `release=true` (`stamp=REL-…`). The developer bypass in `com.dustin.wcore.emu.DevProfile`
  only activates on dev builds.
- `natives/` inside wcore carries the native emulator cores (FFM API). GeckoLib is pinned to
  `geckolib-fabric-26.3-5.5.7.jar`.
- Child mods depend on wcore via `compileOnly project(':wcore')`; nothing is bundled, all five jars are
  installed side by side.

## Game Files Location

Each mod looks for BIOS and game files in your Minecraft directory:

- **Game Boy SP**: `gameboy/bios/` (GBA BIOS) and `gameboy/roms/` (.gba, .gbc, .gb)
- **PS2**: `ps2/bios/` (PS2 BIOS) and `ps2/isos/` (.iso, .chd, .cso)
- **Genesis**: `sega/genesis/games/` (.md, .gen, .bin, .sms, .gg, .sg) — no BIOS needed
- **Saturn**: `sega/saturn/bios/` (mpr-17933.bin or sega_101.bin) and `sega/saturn/games/` (.cue, .chd)
- **Arcade**: `arcade/games/` (MAME 2003-Plus romsets .zip)

## Features

- **Cable system** — connect consoles to TVs and outlets with cables
- **Wireless controller** — play from anywhere with the wireless controller item
- **Two-handed holding** — consoles render properly in two hands
- **Screen sharing** — multiple players can watch the same screen
- **Link cable** — Game Boy SP supports local multiplayer linking
- **Disc/cartridge cases** — craft blank media, write games to them
- **ISO/DumpCheck system** — verification of game dumps, with developer bypass on dev builds
- **RetroArch/libretro cores** — emulator cores download automatically (or place manually in `working_consoles/libretro/`)

## Credits

Original mods by **Dustin Patterson** (dustinpatt55). Source tree rebuilt from the shipped 1.1 jars,
preserving all original functionality.

## License

MIT License
