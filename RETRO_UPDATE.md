# Working Consoles — Version 5: The Retro Room + Arcade Update

Roadmap and status. The original Version 3 plan (recovered from the chat log, 2026-10-18) is
kept below as history — everything in it is still the intent, now delivered in v5 batches.

## Version 5 — current round

**Working Consoles pack — retro room building (v2 built + deployed 5.0.0+DEV.20261018-2135):**
Nine interactive furniture/display pieces. Models + 128x128 atlases come from
`tools/modelgen/gen_furniture.ps1` (re-runnable); Java side is `DecorBlock` /
`TallDecorBlock` / `DecorBlockEntity` / `DecorBlockItem` + client `DecorRenderer`
(per-block model lookup, same trick as TvRenderer, plus stored-item rendering).

- Furniture: TV Stand (drawer), Entertainment Center (2 tall, glass doors, TV alcove), CRT Cart
  (3-tier steel cart on casters), Media Cabinet (2 tall, 4 glass doors), Gaming Desk (knee space,
  right drawer pedestal + sliding drawer, keyboard tray, cable grommet, modesty panel), Console Rack (2 tall).
- Display: Controller Stand (2 cradles), Memory Card Rack (5 leaning slots), Console Display Shelf (wall shelf).
- Doors/drawers animate: closed/opening/open/closing per piece, state-driven from the block entity
  and synced with the block update packet (no swing replay on load/chunk reload).
- Storage: every piece has display slots (game cases / consoles + TVs / controllers). New
  `wcore:consoles` + `wcore:controllers` tags, fed by wcore/ps2/sega/gbsp. Click with an item to
  put it on display, sneak + click to take it back, plain click opens doors/drawers. Holding a
  block still places normally (top face = place on top of the furniture). Own creative tab:
  "Working Consoles: Furniture".
- v2.1 fixes (5.0.0+DEV.20261018-2154, from Dustin's in-game test): stored items now render
  **full size** (console slots were 0.62x) and **sit exactly on the shelf surface** (per-kind
  pivot lift — block-item consoles/TVs +8px, small geo items/controllers +2px, game cases
  bottom-origin already); door/interior glass switched from transparent to **opaque smoked
  glass** (GeckoLib's block render type paints alpha-0 pixels solid black); media cabinet and
  TV stand drawer interiors lightened; AV cable now plugs in **either order** — wall outlet
  first is accepted ("Cable in the outlet, then click a console or TV").
- v2.2 (5.0.0+DEV.20261018-2224, from Dustin's follow-up test): **big-screen TVs fit a full
  block** — the rear-projection TV (28x32px geo) and the flat TV (30x26px) render shrunk to
  16x16 around the block's bottom-centre (geo files untouched; RP collision/selection is now
  a full block), so they sit flush against cabinets and each other and the rear-projection TV
  stands properly **on the TV stand**. TV stand v2.2: divider shortened and full-width
  **console shelf** added over the drawer (console display slot, 6 case slots + drawer left).
  Stored-item display normalized (TV/console item `fixed` transforms raised from 0.32-0.6 to
  ~0.63-1.18) so pieces shown in furniture render at their real in-world size. Console slot
  sizes tuned per shelf (as big as each compartment fits: display shelf 0.9, TV stand 0.85,
  EC alcove 0.75, cart 1.0, tight bays 0.55).

**Arcade pack — complete 1995–2005 arcade (staged, in progress):**

- Stage A — arcade furniture (BUILT, pending in-game check): Arcade Carpet (confetti / zigzag / checker), Floor Decals (arrow / steps / star), Neon Signs (Open, Play, Arcade, Tokens), Exit Sign, Arcade Directory Sign, Ceiling Light, Floor Uplight, Arcade Stool, Trash Can, Vending / Snack / Drink Machines, Change + Coin Return Machines, Ticket + Prize Counters. All static decor (DecorBlock / WideDecorBlock / WideTallDecorBlock), neon lit via GeckoLib emissive glowmasks.
- Stage B — support hardware: Token Machine, Ticket Dispenser, Arcade Kiosk, Redemption Counter. (Prize Counter, Change Machine and Coin Return shipped with Stage A.)
- Stage C — cabinet variants: Racing Cabinet, Twin Cabinet, Shooter Cabinet (playable machines — RetroConsoleBlock + BuiltInScreen like cabinet/big_cabinet; Upright/Deluxe already exist).
- Stage D — ticket games: Skee Ball, Basketball Machine, Ball Toss, Ring Toss, Coin Pusher, Claw Machine, Whack-A-Mole.

**Light guns (design note from Dustin):** point & click — aim with the MC crosshair
(raycast onto the cabinet screen plane, mapped to the core's light-gun X/Y), click = trigger,
and block damage is disabled while a light gun is held (same suppression idea as the
controller input mode). Ships with the Shooter Cabinet.

**Goal:** build a complete 1995–2005 arcade and a realistic 2000-era gaming room from
furniture that looks like real hardware — hardware first, systems after.

## Goal

**Version 3.0 — Retro Room Building System.** Turn Working Consoles from consoles-on-the-floor
into full 1980s–2000s gaming rooms: hardware you recognize, furniture to build the room, shelves
that display your collection, and collectibles to fill them.

Design philosophy: not a "console mod" — a **retro gaming lifestyle mod**.

## Ground rules

- Do **not** redesign or refactor the working code. During the model round, it is **model rework only**.
- Every existing part of the mod stays — removing any part breaks the build (the ISO/DumpCheck chain
  must remain intact).
- The salted SHA-256 developer bypass keeps working for `dustinpatt55` on dev builds only;
  release builds keep the bypass off.
- No major new systems until the hardware models are done.

## Priority 1 — Finish the hardware models (current focus)

Final-quality GeckoLib models for every device:

- Individual bones for moving parts (lids, disc trays, flaps, hinged panels, buttons).
- Accurate silhouettes — **if the screen texture is removed, the player should still recognize the hardware.**
- No large simple cubes, no placeholder shapes.
- Real hardware details: ports, vents, screws, lights, speakers, buttons, hinges, stands, bezels, marquees.
- Devices: arcade uprights (plus cocktail / racing / multi-screen), home consoles (cartridge and disc,
  multiple colorways), handhelds (folding / horizontal / vertical), accessories (controllers, arcade
  sticks, memory cards, cartridges, game cases, CRTs, kiosks).
- Target style: the original-mod screenshots —
  wall projector with Street Fighter Alpha 2, arcade cabinets with marquees, CRT counter setup
  with Doom, PS2 on a desk, funky CRTs on wooden furniture.

## Priority 2 — Room furniture

TV stand, entertainment center, CRT cart, gaming desk, wall shelf, corner shelf, display cabinet,
game rack, console rack.

## Priority 3 — Shelf display system

Universal placement system: right-click to place and remove game cases, cartridges, memory cards,
controllers, figurines, consoles and handhelds on shelves and furniture.

## Priority 4 — Game cases as 3-state items

Inventory → placed decoration → playable media.

## Priority 5 — Collectibles

Action figures, statues, awards, trophies, posters, plushes, display props.

## Wishes from the original mod

- Multiple machines running at the same time.
- The game picture renders on the device itself (TV / handheld screen), never as a fullscreen overlay.
- Handhelds usable in-hand.

## Version scheme

- Base version lives in `gradle.properties` (`mod_version=5.0.0`).
- Dev build (`gradlew.bat build`): all five jars share one stamp —
  e.g. `working-consoles-core-5.0.0+DEV.20261018-2106.jar`; newest stamp = newest build.
- Release build (`gradlew.bat build -Prelease=true`): clean `5.0.0`, bypass disabled.
- Every build **auto-installs** into the live instance (old jars removed, new five copied).

## Status

- [x] Fully buildable source for all five mods (wcore + gbsp + ps2 + sega + arcade).
- [x] Dev bypass working; dev/release builds verified.
- [x] Version stamping per build.
- [ ] Priority 1 — hardware models (in progress:
      - CRTs rebuilt + additive detail pass — crt_large / crt_small / tv (tools/modelgen, originals kept, details bone appended).
      - rear_projection additive detail pass — bezel, dual speaker grilles, buttons, feet, top/rear vents, AV ports, socket, sticker, brand badge (details bone appended, originals kept).
      - TV screen flicker fixed — the picture quad sat exactly coplanar with the glass plate (z-fight); now floated +0.01 block off the glass in TvRenderer.
      - In-game verified on 3.0.0+DEV.20261018-2007: flicker gone (CRT small/large + rear projection), game stable.
      - Arcade cabinets additive detail pass (gen_cabinets_add.ps1) — cabinet: marquee panel, coin door, instruction card, bezel strips, t-molding, feet, vents, socket, sticker; big_cabinet: same + P1/P2 start buttons, coin return, keyhole.
      - All shipped details bones re-parented to their root bone (like led_on/led_standby) so insert/eject/shake animations move the details with the machine.
      - Built + deployed 3.0.0+DEV.20261018-2044; awaiting in-game check of the arcade cabinets.
      - Next: consoles, handhelds, projector / ceiling_projector details).
- [~] Priority 2 — furniture: v2 realism rebuild + interactivity shipped (5.0.0+DEV.20261018-2135), v2.1 fix pass (full-size shelf items, smoked glass, outlet-first cables) shipped 5.0.0+DEV.20261018-2154, v2.2 (TV stand console shelf) shipped 5.0.0+DEV.20261018-2224, v2.3 scale pass shipped 5.0.0+DEV.20261018-2301. Awaiting in-game re-check.
- [ ] Priority 3 — shelf system.
- [ ] Priority 4 — game cases (3 states).
- [ ] Priority 5 — collectibles.
- [x] Version 5 — Working Consoles furniture/display pack v2 (9 pieces, interactive) built + deployed as 5.0.0+DEV.20261018-2135; preview sheet `tools/modelgen/furniture_v2_sheet.png`.
- [x] Version 5 — furniture v2.1 fixes built + deployed as 5.0.0+DEV.20261018-2154; preview sheet `tools/modelgen/furniture_v21_sheet.png`.
- [x] Version 5 — furniture v2.2 (TV stand console shelf) built + deployed as 5.0.0+DEV.20261018-2224; preview sheet `tools/modelgen/tv_stand_v22.png`.
- [x] Version 5 — furniture v2.3 scale pass built + deployed as 5.0.0+DEV.20261018-2301:
      - Big TVs back at native size on a 2 wide x 2 tall footprint (MultiTvBlock, parts MAIN/MAIN_UP/SIDE/SIDE_UP): rear-projection 28x32 px, flat TV 30x26 px; the model is drawn shifted +0.5 blocks toward the side cell (authored centred on the seam); full per-cell hitboxes for the RP, thin per-cell boxes for the flat panel; mining any cell clears all four and drops exactly one item.
      - New `av_stand`: 2-wide 2000s AV stand, chunky top slab over two rows of open full-size console bays (4 console slots, model x -16..16 authored on the seam), 2000s-era chrome strip + LED; new `WideDecorBlock` (SIDE MAIN/SIDE) + `DecorBlock.anchorPx` model shift.
      - DecorRenderer slot yaw fixed to `180 - facing.toYRot()` (GeckoLib draws model front -z toward FACING; the old `-toYRot` was 180 deg off — display-shelf cases hovered off the boards and asymmetric slots landed on the wrong side; this also un-flips the memory-card-rack lean).
      - Game cases on shelves: CASE lift 0 -> +5 px (the case geo is centred on its origin, so cases were half-sunk through the boards = the "glitchy patch"); case scale 0.95 -> 0.86 so 1.75 px rows stop interpenetrating.
      - Cable item resolves any quad-TV cell to MAIN, so the plug works from all four cells.
      - Preview sheet `tools/modelgen/preview_av_stand.png`.
      - Legacy note: big TVs placed with an older build are lone MAIN cells; break and re-place them once.
- [x] Version 5 — arcade Stage A usability pass built + deployed as 5.0.0+DEV.20261019-0414:
      - Ticket + prize counters were the only 2-wide *and* 2-tall pieces: placement required a fixed side cell plus two free cells, so they silently refused to place in a normal arcade room (low ceiling / neighbouring machines). They are now one-cell-tall wide pieces with a collision box that reaches up through the model (ticket 19.5 px, prize 27.5 px) — they place exactly like the AV stand. `WideDecorBlock` also tries the opposite side (`mirror` state) before failing, so every 2-wide piece now fits in corners and next to furniture.
      - Every arcade piece has a use: neon/exit/light/marquee click toggles (`lit` state; glow layer and light level follow it); carpets + decals are full solid 16x16x16 blocks (pattern top, skirting sides, dark bottom) with Speed / Jump Boost step effects; arrow rotates; steps/star glow toggle; stool seats you (invisible marker armor-stand seat, sneak to stand); trash voids items (shift for blocks); directory lists nearby cabinets; vending/snack/drink take 1 token and dispense a snack; change machine 1 emerald -> 4 tokens; coin return 4 tokens -> 1 emerald; ticket counter 1 token -> 2 tickets; prize counter 4 tickets -> random prize.
      - New items: Arcade Token, Arcade Ticket, Arcade Soda, Arcade Chips, Arcade Candy, Prize Plush (`tools/modelgen/gen_arcade_items.ps1`).
      - Preview sheet `tools/modelgen/arc_floor_sheet.png`.
      - 5.0.0+DEV.20261019-0757: item model JSONs are written as UTF-8 **without BOM**. Every machine click now answers on the action bar instead of falling through to the light switch: wrong/empty hand tells you what to insert, correct payment dispenses with a confirmation. Stool click reports seat success/failure and refuses while sneaking or holding a block.
      - 5.0.0+DEV.20261019-0852: added the missing `assets/wcore/items/<name>.json` client item definitions (1.21.4+ item model system). This was the actual cause of the checkerboard icons - `models/item/*.json` alone is no longer enough. Build/deploy verified.
      - Workspace cleanup: the active build is `working-consoles-remake\`; old projects, decompiled sources, old jars and the 2026-10-18 backup moved to `_archive\`; root now has `Build.bat` and `README.md`.
- [ ] Version 5 — arcade Stage B: animated token/ticket payout, cabinet coin-op hook, ticket dispensers, carnival games.
- [ ] Version 5 — theater system (small/medium/large/IMAX screens, projectors, seating, ceiling/wall speakers, concession stands, ticket booth, cinema lighting, drive-in) built on TvScreens / ScreenShare / TvRenderer — no separate screen framework.
- [ ] Version 6 — browser source: external authenticated browser + companion window-capture feed rendered on any TV/theater screen; the mod never handles logins, DRM or subscriptions.
- [ ] Version 7 — streaming displays: the browser side owns DRM/playback; the mod only renders the captured surface in-world and routes audio through ScreenShare.
