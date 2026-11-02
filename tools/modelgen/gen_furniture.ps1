# gen_furniture.ps1 - Working Consoles v5 "retro room" furniture pack (v2 - realistic rebuild).
#
# v2 rewrites all nine pieces: finer construction (banded edges, plinths, cable
# gaps, real hardware), doors/drawers on their own bones, and a
# closed/opening/open/closing animation set per piece that the block entity
# state machine plays. Textures are 128x128 so the pieces still read in a dark
# room. Nothing else in the mod is touched.
#
# Storage slots (where game cases / consoles / controllers render as items) are
# defined in Java (DecorBlock slot lists) - the shelf heights below are the
# contract with WorkingConsolesCore:
#   tv_stand               case rows y1.0/5.1/9.5   (left bay x -5.35..-1.85, right shelf 1.85..5.35)
#   entertainment_center   case rows y3.0/9.7       (cabinet x -4.375..4.375), console shelf y16.3
#   crt_cart               case row y2.7 (7), console on mid tray y8.7
#   media_cabinet          case rows y2.8/11.9/21.9, console cubby right y2.8
#   gaming_desk            case row y15.8 on the desktop (3)
#   console_rack           case row y2.2 (8); every glass shelf: console + 4 cases (y9.4/17.2/25.0)
#   controller_stand       2 controller cradles y6.6
#   memory_card_rack       5 leaning case slots y1.0
#   console_display_shelf  bottom: console + 4 cases (y1.1); mid 6 cases (y8.7); top 8 cases (y16.0)
#
# Everything is deterministic: re-running overwrites its own outputs only.
#
#   powershell -File gen_furniture.ps1

. (Join-Path $PSScriptRoot 'gen_lib.ps1')

$srcRoot = Join-Path $PSScriptRoot '..\..\wcore\src\main\resources'
$assets = Join-Path $srcRoot 'assets\wcore'
$data = Join-Path $srcRoot 'data\wcore'
$geoOut = Join-Path $assets 'geckolib\models\item'
$aniOut = Join-Path $assets 'geckolib\animations\item'
$texOut = Join-Path $assets 'textures\item'

# ===========================================================================
# region table (identical 128x128 layout for every piece)
# ===========================================================================
function New-FurnRegions {
    return [ordered]@{
        wood     = @{ x = 0;  y = 0;  w = 24; h = 24 }
        woodTop  = @{ x = 24; y = 0;  w = 24; h = 24 }
        woodEdge = @{ x = 48; y = 0;  w = 16; h = 4 }
        woodD    = @{ x = 64; y = 0;  w = 20; h = 20 }
        woodTD   = @{ x = 84; y = 0;  w = 20; h = 20 }
        metal    = @{ x = 0;  y = 24; w = 24; h = 24 }
        metalD   = @{ x = 24; y = 24; w = 16; h = 16 }
        chrome   = @{ x = 40; y = 24; w = 16; h = 16 }
        dark     = @{ x = 56; y = 24; w = 16; h = 16 }
        darker   = @{ x = 72; y = 24; w = 8;  h = 8 }
        glass    = @{ x = 0;  y = 48; w = 24; h = 24 }
        handleS  = @{ x = 24; y = 48; w = 6;  h = 3 }
        knob     = @{ x = 30; y = 48; w = 6;  h = 6 }
        caster   = @{ x = 36; y = 48; w = 10; h = 12 }
        panel    = @{ x = 46; y = 48; w = 16; h = 12 }
        ledg     = @{ x = 62; y = 48; w = 2;  h = 2 }
        ledb     = @{ x = 64; y = 48; w = 2;  h = 2 }
        ledr     = @{ x = 66; y = 48; w = 2;  h = 2 }
        sock     = @{ x = 68; y = 48; w = 6;  h = 6 }
        sticker  = @{ x = 74; y = 48; w = 8;  h = 5 }
        slotTrim = @{ x = 82; y = 48; w = 6;  h = 10 }
    }
}

# ===========================================================================
# painters
# ===========================================================================
function Paint-Wood($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#d2ab78' 6 101
    HLine $bmp ($r.x + 1) ($r.y + 4) ($r.w - 3) (C '#b8905f')
    HLine $bmp $r.x ($r.y + 9) ($r.w - 2) (C '#c19a68')
    HLine $bmp ($r.x + 2) ($r.y + 14) ($r.w - 4) (C '#bc9361')
    HLine $bmp $r.x ($r.y + 19) ($r.w - 1) (C '#c49d6b')
    Put-Px $bmp ($r.x + 7) ($r.y + 6) (C '#a87c4a')
    Put-Px $bmp ($r.x + 8) ($r.y + 6) (C '#b3854f')
    Put-Px $bmp ($r.x + 16) ($r.y + 12) (C '#ab7f4c')
    Put-Px $bmp ($r.x + 15) ($r.y + 12) (C '#b78a55')
}
function Paint-WoodTop($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#dcb482' 5 102
    HLine $bmp $r.x ($r.y + 2) $r.w (C '#e9c495')
    HLine $bmp ($r.x + 1) ($r.y + 8) ($r.w - 2) (C '#c9a06d')
    HLine $bmp $r.x ($r.y + 15) ($r.w - 1) (C '#c49a67')
    HLine $bmp $r.x ($r.y + 21) $r.w (C '#c9a06d')
    Put-Px $bmp ($r.x + 19) ($r.y + 5) (C '#b8905f')
    Put-Px $bmp ($r.x + 4) ($r.y + 17) (C '#b8905f')
}
function Paint-WoodEdge($bmp, $r) {
    Fill-Rect $bmp $r.x $r.y $r.w $r.h (C '#cba573')
    HLine $bmp $r.x $r.y $r.w (C '#e2bd8d')
    HLine $bmp $r.x ($r.y + 2) $r.w (C '#9c7a4c')
    HLine $bmp $r.x ($r.y + 3) $r.w (C '#8a6a40')
}
function Paint-WoodD($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#8f6a42' 5 103
    HLine $bmp ($r.x + 1) ($r.y + 5) ($r.w - 3) (C '#7a5834')
    HLine $bmp $r.x ($r.y + 12) ($r.w - 2) (C '#997347')
    HLine $bmp ($r.x + 2) ($r.y + 17) ($r.w - 4) (C '#7d5b36')
    Put-Px $bmp ($r.x + 13) ($r.y + 9) (C '#6f4f2e')
}
function Paint-WoodTD($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#9c7649' 5 104
    HLine $bmp $r.x ($r.y + 2) $r.w (C '#ab8455')
    HLine $bmp ($r.x + 1) ($r.y + 9) ($r.w - 2) (C '#8a6740')
    HLine $bmp $r.x ($r.y + 16) ($r.w - 1) (C '#8f6b42')
}
function Paint-Metal($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#c0c0ca' 4 105
    for ($i = 2; $i -lt $r.w; $i += 5) { VLine $bmp ($r.x + $i) ($r.y + 1) ($r.h - 2) (C '#ceced8') }
    HLine $bmp $r.x $r.y $r.w (C '#e2e2ec')
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#90909a')
}
function Paint-MetalD($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#8b8b95' 4 106
    HLine $bmp $r.x $r.y $r.w (C '#a2a2ac')
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#6e6e78')
}
function Paint-Chrome($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#c6c8d2' 3 107
    for ($i = 3; $i -lt ($r.w - 2); $i += 6) { VLine $bmp ($r.x + $i) $r.y $r.h (C '#8a8c98') }
    VLine $bmp ($r.x + 2) ($r.y + 1) ($r.h - 2) (C '#f0f2fa')
    VLine $bmp ($r.x + 3) ($r.y + 1) ($r.h - 2) (C '#e0e2ec')
}
function Paint-Dark($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#26262a' 3 108
    HLine $bmp $r.x $r.y $r.w (C '#34343a')
}
function Paint-Darker($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#151519' 2 109
}
function Paint-Glass($bmp, $r) {
    # opaque smoked glass: GeckoLib's block render type draws alpha-0 pixels
    # as solid black, so the pane itself is painted dark blue-grey with
    # lighter streaks reading as reflections (period-correct smoked doors)
    Fill-Rect $bmp $r.x $r.y $r.w $r.h (C '#333f4a')
    $sheen = C '#9fb6cc'
    $dim = C '#4c5f72'
    for ($i = 2; $i -lt $r.w; $i++) {
        $y = $r.y + (($i * 2 + 3) % $r.h)
        Put-Px $bmp ($r.x + $i) $y $dim
        if ($i % 4 -eq 0) { Put-Px $bmp ($r.x + $i) $y $sheen }
    }
    for ($i = 3; $i -lt $r.w; $i += 5) {
        $y = $r.y + (($i * 3 + 8) % $r.h)
        Put-Px $bmp ($r.x + $i) $y $sheen
    }
}
function Paint-HandleS($bmp, $r) {
    Fill-Rect $bmp $r.x $r.y $r.w 1 (C '#e8e8f0')
    Fill-Rect $bmp $r.x ($r.y + 1) $r.w 1 (C '#c2c2cc')
    Fill-Rect $bmp $r.x ($r.y + 2) $r.w 1 (C '#7e7e88')
}
function Paint-Knob($bmp, $r) {
    Fill-Rect $bmp $r.x $r.y $r.w $r.h (C '#3a3a40')
    Fill-Rect $bmp ($r.x + 1) ($r.y + 1) ($r.w - 2) ($r.h - 2) (C '#a8a8b2')
    Fill-Rect $bmp ($r.x + 2) ($r.y + 2) 1 2 (C '#d8d8e2')
}
function Paint-Caster($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#1c1c20' 2 110
    Fill-Rect $bmp ($r.x + 2) ($r.y + 2) ($r.w - 4) ($r.h - 5) (C '#101013')
    Fill-Rect $bmp ($r.x + 3) ($r.y + 3) ($r.w - 6) ($r.h - 7) (C '#2e2e34')
    Put-Px $bmp ($r.x + 2) ($r.y + 2) (C '#4a4a52')
}
function Paint-Panel($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#b3b3bd' 4 111
    HLine $bmp $r.x $r.y $r.w (C '#dedee6')
    HLine $bmp ($r.x + 1) ($r.y + 1) ($r.w - 2) (C '#c6c6d0')
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#6a6a74')
    HLine $bmp ($r.x + 1) ($r.y + $r.h - 2) ($r.w - 2) (C '#84848e')
    VLine $bmp $r.x ($r.y + 1) ($r.h - 2) (C '#8e8e98')
    VLine $bmp ($r.x + $r.w - 1) ($r.y + 1) ($r.h - 2) (C '#8e8e98')
}
function Paint-Led($bmp, $r, $hex) {
    Fill-Rect $bmp $r.x $r.y $r.w $r.h (C $hex)
    Put-Px $bmp $r.x $r.y (C '#ffffff')
}
function Paint-Sock($bmp, $r) {
    Fill-Rect $bmp $r.x $r.y $r.w $r.h (C '#d8d8e0')
    Fill-Rect $bmp ($r.x + 1) ($r.y + 1) 1 2 (C '#141416')
    Fill-Rect $bmp ($r.x + 2) ($r.y + 1) 1 2 (C '#33333a')
    Fill-Rect $bmp ($r.x + 4) ($r.y + 1) 1 2 (C '#141416')
    Fill-Rect $bmp ($r.x + 5) ($r.y + 1) 1 2 (C '#33333a')
    Put-Px $bmp ($r.x + 1) ($r.y + $r.h - 2) (C '#8a8a92')
}
function Paint-Sticker($bmp, $r) {
    Fill-Rect $bmp $r.x $r.y $r.w $r.h (C '#e6e6ea')
    Fill-Rect $bmp ($r.x + 1) ($r.y + 1) ($r.w - 2) 1 (C '#2a58c8')
    HLine $bmp ($r.x + 1) ($r.y + 3) ($r.w - 3) (C '#5a5a62')
    Put-Px $bmp ($r.x + 1) ($r.y + 4) (C '#c83232')
}
function Paint-SlotTrim($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#33333a' 3 112
    VLine $bmp ($r.x + 1) ($r.y + 1) ($r.h - 2) (C '#4a4a54')
    VLine $bmp ($r.x + $r.w - 2) ($r.y + 1) ($r.h - 2) (C '#1c1c20')
}

function Paint-Shared($bmp, $R) {
    Paint-Wood     $bmp $R.wood
    Paint-WoodTop  $bmp $R.woodTop
    Paint-WoodEdge $bmp $R.woodEdge
    Paint-WoodD    $bmp $R.woodD
    Paint-WoodTD   $bmp $R.woodTD
    Paint-Metal    $bmp $R.metal
    Paint-MetalD   $bmp $R.metalD
    Paint-Chrome   $bmp $R.chrome
    Paint-Dark     $bmp $R.dark
    Paint-Darker   $bmp $R.darker
    Paint-Glass    $bmp $R.glass
    Paint-HandleS  $bmp $R.handleS
    Paint-Knob     $bmp $R.knob
    Paint-Caster   $bmp $R.caster
    Paint-Panel    $bmp $R.panel
    Paint-Led $bmp $R.ledg '#3ce65a'
    Paint-Led $bmp $R.ledb '#3c96f0'
    Paint-Led $bmp $R.ledr '#e63333'
    Paint-Sock     $bmp $R.sock
    Paint-Sticker  $bmp $R.sticker
    Paint-SlotTrim $bmp $R.slotTrim
}

# ===========================================================================
# geo / animation building
# ===========================================================================
function Cu($o, $s, $f, $rot = $null) {
    $uv = @{}
    foreach ($face in @('north', 'east', 'south', 'west', 'up', 'down')) {
        if ($f.ContainsKey($face)) { $uv[$face] = $f[$face] }
        elseif ($f.ContainsKey('all')) { $uv[$face] = $f['all'] }
    }
    $c = [ordered]@{ o = $o; s = $s; f = $uv }
    if ($rot) { $c['r'] = $rot }
    return $c
}

function Bone($name, $pivot, $cubes) {
    return [ordered]@{ name = $name; pivot = $pivot; cubes = @($cubes) }
}

function Write-JsonFile([string]$path, $obj) {
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $text = $obj | ConvertTo-Json -Depth 30
    [System.IO.File]::WriteAllText($path, $text, (New-Object System.Text.UTF8Encoding($false)))
}

function Write-Anims([string]$name, $mov) {
    $open = [ordered]@{}
    $opening = [ordered]@{}
    $closing = [ordered]@{}
    foreach ($k in @($mov.Keys)) {
        $m = $mov[$k]
        $ch = if ($m.type -eq 'rot') { 'rotation' } else { 'position' }
        $open[$k] = [ordered]@{ $ch = [ordered]@{ '0.0' = [ordered]@{ vector = @([double]$m.open[0], [double]$m.open[1], [double]$m.open[2]) } } }
        $opening[$k] = [ordered]@{ $ch = [ordered]@{
            '0.0'  = [ordered]@{ vector = @([double]$m.closed[0], [double]$m.closed[1], [double]$m.closed[2]); easing = 'easeInOutSine' }
            '0.55' = [ordered]@{ vector = @([double]$m.open[0], [double]$m.open[1], [double]$m.open[2]); easing = 'easeOutCubic' } } }
        $closing[$k] = [ordered]@{ $ch = [ordered]@{
            '0.0'  = [ordered]@{ vector = @([double]$m.open[0], [double]$m.open[1], [double]$m.open[2]); easing = 'easeInOutSine' }
            '0.5'  = [ordered]@{ vector = @([double]$m.closed[0], [double]$m.closed[1], [double]$m.closed[2]); easing = 'easeInCubic' } } }
    }
    Write-JsonFile (Join-Path $aniOut "$name.animation.json") ([ordered]@{
        format_version = '1.8.0'
        animations = [ordered]@{
            idle    = [ordered]@{ loop = $true; animation_length = 1; bones = [ordered]@{} }
            closed  = [ordered]@{ loop = $true; animation_length = 0.4; bones = [ordered]@{} }
            open    = [ordered]@{ loop = $true; animation_length = 0.4; bones = $open }
            opening = [ordered]@{ animation_length = 0.55; bones = $opening }
            closing = [ordered]@{ animation_length = 0.5; bones = $closing }
        }
    })
}

function Build-Furn([string]$name, [int]$visH, $bones, $mov, [int]$visW = 2) {
    $R = $script:R
    $bmp = New-Object System.Drawing.Bitmap(128, 128)
    Fill-Rect $bmp 0 0 128 128 (C '#17171a')
    Paint-Shared $bmp $R
    Save-Atlas $bmp (Join-Path $texOut "$name.png")
    $bmp.Dispose()

    $json = ConvertTo-GeoJson "geometry.$name" 128 128 $visW $visH @(0, 1, 0) $bones $R
    [System.IO.File]::WriteAllText((Join-Path $geoOut "$name.geo.json"), $json, (New-Object System.Text.UTF8Encoding($false)))
    Write-Anims $name $mov

    $check = Get-Content -Raw -LiteralPath (Join-Path $geoOut "$name.geo.json") | ConvertFrom-Json
    $expected = 0; foreach ($b in $bones) { $expected += @($b.cubes).Count }
    $count = 0
    foreach ($b in $check.'minecraft:geometry'[0].bones) { $count += @($b.cubes).Count }
    $anim = Get-Content -Raw -LiteralPath (Join-Path $aniOut "$name.animation.json") | ConvertFrom-Json
    $anims = @($anim.animations.PSObject.Properties.Name).Count
    $ok = ($count -eq $expected) -and ($anims -eq 5) -and (Test-Path (Join-Path $texOut "$name.png"))
    $status = if ($ok) { 'OK' } else { 'FAILED' }
    Write-Output ("{0,-22} {1,2} cubes  {2} bones  {3} anims  [{4}]" -f $name, $count, @($bones).Count, $anims, $status)
    if (-not $ok) { throw "furniture build failed for $name" }
}

# ===========================================================================
# resource JSON emitters
# ===========================================================================
function Emit-Resources([string]$name, [string]$display, $recipe) {
    Write-JsonFile (Join-Path $assets "blockstates\$name.json") ([ordered]@{
        variants = [ordered]@{ '' = [ordered]@{ model = "wcore:block/$name" } }
    })
    Write-JsonFile (Join-Path $assets "models\block\$name.json") ([ordered]@{
        textures = [ordered]@{ particle = "wcore:item/$name" }
    })
    Write-JsonFile (Join-Path $assets "items\$name.json") ([ordered]@{
        model = [ordered]@{
            type  = 'minecraft:special'
            base  = "wcore:item/$name"
            model = [ordered]@{ type = 'geckolib:geckolib' }
        }
    })
    $guiScale = switch ($display) { 'small' { 0.85 } 'tall' { 0.32 } default { 0.5 } }
    $guiT = if ($display -eq 'tall') { @(0, -6, 0) } else { @(0, -3, 0) }
    $groundScale = switch ($display) { 'small' { 0.45 } 'tall' { 0.22 } default { 0.3 } }
    $fixedScale = switch ($display) { 'small' { 0.85 } 'tall' { 0.35 } default { 0.5 } }
    Write-JsonFile (Join-Path $assets "models\item\$name.json") ([ordered]@{
        textures = [ordered]@{ particle = "wcore:item/$name" }
        gui_light = 'front'
        display = [ordered]@{
            gui                   = [ordered]@{ rotation = @(20, 205, 0); translation = $guiT; scale = @($guiScale, $guiScale, $guiScale) }
            ground                = [ordered]@{ translation = @(0, 2, 0); scale = @($groundScale, $groundScale, $groundScale) }
            firstperson_righthand = [ordered]@{ rotation = @(0, 160, 0); translation = @(0, 0, 0); scale = @(0.35, 0.35, 0.35) }
            firstperson_lefthand  = [ordered]@{ rotation = @(0, 160, 0); translation = @(0, 0, 0); scale = @(0.35, 0.35, 0.35) }
            thirdperson_righthand = [ordered]@{ rotation = @(75, 180, 0); translation = @(0, 2, 0); scale = @(0.3, 0.3, 0.3) }
            thirdperson_lefthand  = [ordered]@{ rotation = @(75, 180, 0); translation = @(0, 2, 0); scale = @(0.3, 0.3, 0.3) }
            fixed                 = [ordered]@{ rotation = @(0, 180, 0); scale = @($fixedScale, $fixedScale, $fixedScale) }
        }
    })
    Write-JsonFile (Join-Path $data "loot_table\blocks\$name.json") ([ordered]@{
        type  = 'minecraft:block'
        pools = @([ordered]@{
            rolls   = 1
            entries = @([ordered]@{ type = 'minecraft:item'; name = "wcore:$name" })
            conditions = @([ordered]@{ condition = 'minecraft:survives_explosion' })
        })
    })
    Write-JsonFile (Join-Path $data "recipe\$name.json") $recipe
    Write-Output ("  resources: blockstate/blockmodel/item/items/loot/recipe  [OK]")
}

function Recipe-Pattern($pattern, $key, [string]$name) {
    return [ordered]@{
        type     = 'minecraft:crafting_shaped'
        category = 'building'
        pattern  = $pattern
        key      = $key
        result   = [ordered]@{ id = "wcore:$name"; count = 1 }
    }
}

# ===========================================================================
# the nine pieces
# ===========================================================================
$script:R = New-FurnRegions

# ---- tv_stand: low AV bench, drawer + open bay -----------------------------
function Build-TvStand {
    $body = @(
        (Cu @(-8, 14.8, -6.2) @(16, 1.2, 12.7) (@{ all = 'wood'; up = 'woodTop' })),
        (Cu @(-8, 14.8, -6.38) @(16, 1.2, 0.18) (@{ all = 'woodEdge' })),
        (Cu @(-8, 0, -6.2) @(1.4, 14.8, 12.7) (@{ all = 'wood' })),
        (Cu @(6.6, 0, -6.2) @(1.4, 14.8, 12.7) (@{ all = 'wood' })),
        (Cu @(-6.6, 0, -5.7) @(13.2, 1.0, 12.2) (@{ all = 'woodD'; up = 'woodD' })),
        (Cu @(-0.6, 1.0, -5.6) @(1.2, 7.6, 12.0) (@{ all = 'wood' })),
        (Cu @(-6.6, 1.0, 5.55) @(5.8, 13.8, 0.65) (@{ all = 'woodD' })),
        (Cu @(0.8, 1.0, 5.55) @(5.8, 13.8, 0.65) (@{ all = 'woodD' })),
        (Cu @(-6.6, 8.6, -5.6) @(6.0, 0.9, 12.0) (@{ all = 'wood' })),
        (Cu @(-6.6, 4.2, -5.6) @(6.0, 0.9, 12.0) (@{ all = 'wood' })),
        (Cu @(0.6, 8.6, -5.6) @(6.0, 0.9, 12.0) (@{ all = 'wood' })),
        (Cu @(-0.6, 8.6, -5.6) @(1.2, 0.9, 12.0) (@{ all = 'wood' })),
        (Cu @(-8, 14.35, -6.32) @(16, 0.45, 0.28) (@{ all = 'metal' })),
        (Cu @(-7.0, 12.9, -6.35) @(0.4, 0.4, 0.2) (@{ all = 'ledg' })),
        (Cu @(1.5, 1.6, 6.15) @(2, 2, 0.4) (@{ all = 'woodD'; south = 'sock' })),
        (Cu @(-7.6, 15.05, -6.42) @(1.4, 0.8, 0.12) (@{ all = 'sticker' })),
        (Cu @(-6.6, 0, -6.3) @(13.2, 0.9, 0.25) (@{ all = 'darker' }))
    )
    $drawer = @(
        (Cu @(0.9, 1.4, -6.35) @(5.4, 6.9, 0.4) (@{ all = 'panel' })),
        (Cu @(2.7, 4.2, -6.55) @(1.8, 0.6, 0.25) (@{ all = 'metalD'; north = 'handleS' })),
        (Cu @(1.1, 1.5, -5.95) @(5.0, 6.6, 8.0) (@{ all = 'woodD' }))
    )
    Build-Furn 'tv_stand' 2 @(
        (Bone 'body' @(0, 0, 0) $body),
        (Bone 'drawer' @(3.6, 4.5, -6.2) $drawer)
    ) @{ drawer = @{ type = 'pos'; closed = @(0, 0, 0); open = @(0, 0, -3.5) } }
    Emit-Resources 'tv_stand' 'mid' (Recipe-Pattern @('PPP', 'P P', 'PPP') ([ordered]@{ P = '#minecraft:planks' }) 'tv_stand')
}

# ---- entertainment_center: 2 tall, glass doors, TV alcove ------------------
function Build-EntertainmentCenter {
    $body = @(
        (Cu @(-8.2, 30.6, -6.3) @(16.4, 1.4, 13.1) (@{ all = 'wood'; up = 'woodTop'; down = 'woodEdge' })),
        (Cu @(-8.1, 29.9, -6.2) @(16.2, 0.7, 12.9) (@{ all = 'woodD' })),
        (Cu @(-8, 0, -6) @(2.4, 29.9, 13) (@{ all = 'wood' })),
        (Cu @(5.6, 0, -6) @(2.4, 29.9, 13) (@{ all = 'wood' })),
        (Cu @(-7.7, 0, -5.8) @(15.4, 2.2, 12.6) (@{ all = 'woodD' })),
        (Cu @(-5.6, 2.2, 5.5) @(4.9, 27.7, 0.9) (@{ all = 'woodD' })),
        (Cu @(0.7, 2.2, 5.5) @(4.9, 27.7, 0.9) (@{ all = 'woodD' })),
        (Cu @(-5.6, 15.2, -5.9) @(11.2, 1.1, 12.4) (@{ all = 'wood'; up = 'woodTop' })),
        (Cu @(-5.6, 15.05, -6.08) @(11.2, 1.4, 0.22) (@{ all = 'woodEdge' })),
        (Cu @(-5.6, 2.2, -5.7) @(11.2, 0.8, 12.2) (@{ all = 'woodD' })),
        (Cu @(-5.5, 8.9, -5.6) @(11.0, 0.8, 12.0) (@{ all = 'woodD' })),
        (Cu @(6.9, 30.3, -6.15) @(0.45, 0.45, 0.2) (@{ all = 'ledg' })),
        (Cu @(3.0, 4.0, 5.5) @(2, 2, 0.4) (@{ all = 'woodD'; south = 'sock' })),
        (Cu @(-7.8, 27.2, -6.05) @(1.4, 0.85, 0.15) (@{ all = 'sticker' }))
    )
    $doorL = @(
        (Cu @(-5.5, 13.9, -6.15) @(5.0, 0.9, 0.5) (@{ all = 'wood' })),
        (Cu @(-5.5, 3.0, -6.15) @(5.0, 0.9, 0.5) (@{ all = 'wood' })),
        (Cu @(-5.5, 3.9, -6.15) @(0.8, 10.0, 0.5) (@{ all = 'wood' })),
        (Cu @(-1.5, 3.9, -6.15) @(1.0, 10.0, 0.5) (@{ all = 'wood' })),
        (Cu @(-4.7, 3.9, -6.05) @(3.2, 10.0, 0.4) (@{ all = 'glass' })),
        (Cu @(-2.05, 8.3, -6.5) @(0.55, 0.9, 0.4) (@{ all = 'knob' }))
    )
    $doorR = @(
        (Cu @(0.5, 13.9, -6.15) @(5.0, 0.9, 0.5) (@{ all = 'wood' })),
        (Cu @(0.5, 3.0, -6.15) @(5.0, 0.9, 0.5) (@{ all = 'wood' })),
        (Cu @(0.5, 3.9, -6.15) @(1.0, 10.0, 0.5) (@{ all = 'wood' })),
        (Cu @(4.7, 3.9, -6.15) @(0.8, 10.0, 0.5) (@{ all = 'wood' })),
        (Cu @(1.5, 3.9, -6.05) @(3.2, 10.0, 0.4) (@{ all = 'glass' })),
        (Cu @(1.5, 8.3, -6.5) @(0.55, 0.9, 0.4) (@{ all = 'knob' }))
    )
    Build-Furn 'entertainment_center' 4 @(
        (Bone 'body' @(0, 0, 0) $body),
        (Bone 'door_l' @(-5.6, 8.9, -5.95) $doorL),
        (Bone 'door_r' @(5.6, 8.9, -5.95) $doorR)
    ) @{
        door_l = @{ type = 'rot'; closed = @(0, 0, 0); open = @(0, 100, 0) }
        door_r = @{ type = 'rot'; closed = @(0, 0, 0); open = @(0, -100, 0) }
    }
    Emit-Resources 'entertainment_center' 'tall' (Recipe-Pattern @('PPP', 'PGP', 'PPP') ([ordered]@{ P = '#minecraft:planks'; G = 'minecraft:glass' }) 'entertainment_center')
}

# ---- crt_cart: 3-tier steel AV cart on casters -----------------------------
function Build-CrtCart {
    Build-Furn 'crt_cart' 2 @(
        (Bone 'body' @(0, 0, 0) @(
            (Cu @(-7.2, 14.9, -5.2) @(14.4, 1.1, 10.4) (@{ all = 'metal'; up = 'metal' })),
            (Cu @(-7.2, 14.9, -5.4) @(14.4, 1.1, 0.2) (@{ all = 'metalD' })),
            (Cu @(-7.2, 7.8, -5.2) @(14.4, 0.9, 10.4) (@{ all = 'metal' })),
            (Cu @(-7.2, 7.8, -5.38) @(14.4, 0.9, 0.18) (@{ all = 'metalD' })),
            (Cu @(-7.2, 1.8, -5.2) @(14.4, 0.9, 10.4) (@{ all = 'metal' })),
            (Cu @(-7.2, 1.8, -5.38) @(14.4, 0.9, 0.18) (@{ all = 'metalD' })),
            (Cu @(-7.2, 1.4, -5.2) @(1.2, 13.5, 1.2) (@{ all = 'metalD' })),
            (Cu @(6.0, 1.4, -5.2) @(1.2, 13.5, 1.2) (@{ all = 'metalD' })),
            (Cu @(-7.2, 1.4, 4.0) @(1.2, 13.5, 1.2) (@{ all = 'metalD' })),
            (Cu @(6.0, 1.4, 4.0) @(1.2, 13.5, 1.2) (@{ all = 'metalD' })),
            (Cu @(-7.25, 10.6, -5.2) @(0.5, 0.6, 10.4) (@{ all = 'metalD' })),
            (Cu @(6.75, 10.6, -5.2) @(0.5, 0.6, 10.4) (@{ all = 'metalD' })),
            (Cu @(-3.4, 13.5, 4.0) @(0.9, 3.2, 0.9) (@{ all = 'metalD' })),
            (Cu @(2.5, 13.5, 4.0) @(0.9, 3.2, 0.9) (@{ all = 'metalD' })),
            (Cu @(-3.4, 16.4, 4.0) @(6.8, 0.9, 0.9) (@{ all = 'metal' })),
            (Cu @(-7.0, 0.7, -5.0) @(1.4, 0.7, 1.4) (@{ all = 'dark' })),
            (Cu @(-6.9, 0, -4.85) @(1.2, 0.9, 1.0) (@{ all = 'caster' })),
            (Cu @(5.6, 0.7, -5.0) @(1.4, 0.7, 1.4) (@{ all = 'dark' })),
            (Cu @(5.7, 0, -4.85) @(1.2, 0.9, 1.0) (@{ all = 'caster' })),
            (Cu @(-7.0, 0.7, 3.8) @(1.4, 0.7, 1.4) (@{ all = 'dark' })),
            (Cu @(-6.9, 0, 3.85) @(1.2, 0.9, 1.0) (@{ all = 'caster' })),
            (Cu @(5.6, 0.7, 3.8) @(1.4, 0.7, 1.4) (@{ all = 'dark' })),
            (Cu @(5.7, 0, 3.85) @(1.2, 0.9, 1.0) (@{ all = 'caster' })),
            (Cu @(-6.3, 8.05, -5.5) @(1.4, 0.8, 0.15) (@{ all = 'sticker' })),
            (Cu @(2.6, 2.2, 5.2) @(2, 2, 0.4) (@{ all = 'metal'; south = 'sock' }))
        ))
    ) @{}
    Emit-Resources 'crt_cart' 'mid' (Recipe-Pattern @('I I', 'I I', 'III') ([ordered]@{ I = 'minecraft:iron_ingot' }) 'crt_cart')
}

# ---- media_cabinet: 2 tall, four glass doors, dark wood --------------------
function Build-MediaCabinet {
    $body = @(
        (Cu @(-8, 0, -6) @(1.6, 32, 13) (@{ all = 'woodD' })),
        (Cu @(6.4, 0, -6) @(1.6, 32, 13) (@{ all = 'woodD' })),
        (Cu @(-6.4, 30.4, -6) @(12.8, 1.6, 13) (@{ all = 'woodD'; up = 'woodTD' })),
        (Cu @(-6.6, 29.6, -6.15) @(13.2, 0.8, 13.1) (@{ all = 'darker' })),
        (Cu @(-7.6, 0, -5.8) @(15.2, 1.8, 12.6) (@{ all = 'darker' })),
        (Cu @(-6.4, 1.8, -5.9) @(12.8, 1.0, 12.7) (@{ all = 'woodD' })),
        (Cu @(-6.4, 2.8, 5.6) @(12.8, 27.6, 0.9) (@{ all = 'woodD' })),
        (Cu @(-0.5, 2.8, -5.6) @(1.0, 27.6, 11.8) (@{ all = 'woodD' })),
        (Cu @(-6.4, 11.0, -5.6) @(5.9, 0.9, 11.8) (@{ all = 'woodD' })),
        (Cu @(-6.4, 21.0, -5.6) @(5.9, 0.9, 11.8) (@{ all = 'woodD' })),
        (Cu @(0.5, 11.0, -5.6) @(5.9, 0.9, 11.8) (@{ all = 'woodD' })),
        (Cu @(0.5, 21.0, -5.6) @(5.9, 0.9, 11.8) (@{ all = 'woodD' })),
        (Cu @(6.9, 30.0, -6.1) @(0.4, 0.4, 0.2) (@{ all = 'ledg' })),
        (Cu @(6.9, 28.9, -6.1) @(0.35, 0.35, 0.2) (@{ all = 'ledr' })),
        (Cu @(0.0, 3.4, 6.2) @(2, 2, 0.4) (@{ all = 'darker'; south = 'sock' }))
    )
    $doorLU = @(
        (Cu @(-6.3, 28.7, -6.25) @(5.7, 0.8, 0.45) (@{ all = 'woodD' })),
        (Cu @(-6.3, 21.8, -6.25) @(5.7, 0.8, 0.45) (@{ all = 'woodD' })),
        (Cu @(-6.3, 22.6, -6.25) @(0.8, 6.1, 0.45) (@{ all = 'woodD' })),
        (Cu @(-1.5, 22.6, -6.25) @(1.0, 6.1, 0.45) (@{ all = 'woodD' })),
        (Cu @(-5.5, 22.6, -6.17) @(4.0, 6.1, 0.35) (@{ all = 'glass' })),
        (Cu @(-2.05, 25.1, -6.5) @(0.55, 0.9, 0.35) (@{ all = 'knob' }))
    )
    $doorLL = @(
        (Cu @(-6.3, 20.0, -6.25) @(5.7, 0.9, 0.45) (@{ all = 'woodD' })),
        (Cu @(-6.3, 2.9, -6.25) @(5.7, 0.9, 0.45) (@{ all = 'woodD' })),
        (Cu @(-6.3, 3.8, -6.25) @(0.8, 16.2, 0.45) (@{ all = 'woodD' })),
        (Cu @(-1.5, 3.8, -6.25) @(1.0, 16.2, 0.45) (@{ all = 'woodD' })),
        (Cu @(-5.5, 3.8, -6.17) @(4.0, 16.2, 0.35) (@{ all = 'glass' })),
        (Cu @(-2.05, 11.4, -6.5) @(0.55, 0.9, 0.35) (@{ all = 'knob' }))
    )
    $doorRU = @(
        (Cu @(0.6, 28.7, -6.25) @(5.7, 0.8, 0.45) (@{ all = 'woodD' })),
        (Cu @(0.6, 21.8, -6.25) @(5.7, 0.8, 0.45) (@{ all = 'woodD' })),
        (Cu @(0.6, 22.6, -6.25) @(0.9, 6.1, 0.45) (@{ all = 'woodD' })),
        (Cu @(5.5, 22.6, -6.25) @(0.8, 6.1, 0.45) (@{ all = 'woodD' })),
        (Cu @(1.5, 22.6, -6.17) @(4.0, 6.1, 0.35) (@{ all = 'glass' })),
        (Cu @(1.45, 25.1, -6.5) @(0.55, 0.9, 0.35) (@{ all = 'knob' }))
    )
    $doorRL = @(
        (Cu @(0.6, 20.0, -6.25) @(5.7, 0.9, 0.45) (@{ all = 'woodD' })),
        (Cu @(0.6, 2.9, -6.25) @(5.7, 0.9, 0.45) (@{ all = 'woodD' })),
        (Cu @(0.6, 3.8, -6.25) @(0.9, 16.2, 0.45) (@{ all = 'woodD' })),
        (Cu @(5.5, 3.8, -6.25) @(0.8, 16.2, 0.45) (@{ all = 'woodD' })),
        (Cu @(1.5, 3.8, -6.17) @(4.0, 16.2, 0.35) (@{ all = 'glass' })),
        (Cu @(1.45, 11.4, -6.5) @(0.55, 0.9, 0.35) (@{ all = 'knob' }))
    )
    Build-Furn 'media_cabinet' 4 @(
        (Bone 'body' @(0, 0, 0) $body),
        (Bone 'door_lu' @(-6.4, 25.65, -6.0) $doorLU),
        (Bone 'door_ll' @(-6.4, 11.85, -6.0) $doorLL),
        (Bone 'door_ru' @(6.4, 25.65, -6.0) $doorRU),
        (Bone 'door_rl' @(6.4, 11.85, -6.0) $doorRL)
    ) @{
        door_lu = @{ type = 'rot'; closed = @(0, 0, 0); open = @(0, 100, 0) }
        door_ll = @{ type = 'rot'; closed = @(0, 0, 0); open = @(0, 100, 0) }
        door_ru = @{ type = 'rot'; closed = @(0, 0, 0); open = @(0, -100, 0) }
        door_rl = @{ type = 'rot'; closed = @(0, 0, 0); open = @(0, -100, 0) }
    }
    Emit-Resources 'media_cabinet' 'tall' (Recipe-Pattern @('PPP', 'GGG', 'PPP') ([ordered]@{ P = '#minecraft:planks'; G = 'minecraft:glass' }) 'media_cabinet')
}

# ---- gaming_desk: beech top, drawer pedestal, knee space, keyboard tray ----
function Build-GamingDesk {
    $body = @(
        (Cu @(-8, 14.6, -7.6) @(16, 1.2, 15.2) (@{ all = 'wood'; up = 'woodTop' })),
        (Cu @(-8, 14.6, -7.78) @(16, 1.2, 0.18) (@{ all = 'woodEdge' })),
        (Cu @(-8, 14.6, -7.6) @(0.18, 1.2, 15.2) (@{ all = 'woodEdge' })),
        (Cu @(7.82, 14.6, -7.6) @(0.18, 1.2, 15.2) (@{ all = 'woodEdge' })),
        (Cu @(4.3, 0.6, -6.6) @(3.6, 14.0, 13.2) (@{ all = 'dark'; up = 'darker' })),
        (Cu @(4.6, 0, -6.3) @(3.0, 0.6, 12.6) (@{ all = 'darker' })),
        (Cu @(4.5, 1.5, -6.8) @(3.2, 3.6, 0.35) (@{ all = 'panel' })),
        (Cu @(5.6, 2.8, -7.05) @(1.8, 0.55, 0.25) (@{ all = 'metalD'; north = 'handleS' })),
        (Cu @(4.5, 5.65, -6.8) @(3.2, 3.6, 0.35) (@{ all = 'panel' })),
        (Cu @(5.6, 6.95, -7.05) @(1.8, 0.55, 0.25) (@{ all = 'metalD'; north = 'handleS' })),
        (Cu @(-7.3, 0.5, -6.5) @(1.3, 14.1, 1.3) (@{ all = 'metalD' })),
        (Cu @(-7.3, 0.5, 5.2) @(1.3, 14.1, 1.3) (@{ all = 'metalD' })),
        (Cu @(-7.45, 0, -6.65) @(1.6, 0.5, 1.6) (@{ all = 'dark' })),
        (Cu @(-7.45, 0, 5.05) @(1.6, 0.5, 1.6) (@{ all = 'dark' })),
        (Cu @(-7.5, 13.5, -6.5) @(1.7, 1.1, 13.0) (@{ all = 'metalD' })),
        (Cu @(-5.8, 13.5, -6.5) @(10.1, 1.1, 1.0) (@{ all = 'metalD' })),
        (Cu @(-5.8, 13.5, 5.5) @(10.1, 1.1, 1.0) (@{ all = 'metalD' })),
        (Cu @(-5.8, 9.0, 6.2) @(4.0, 4.5, 0.5) (@{ all = 'woodD' })),
        (Cu @(0.0, 9.0, 6.2) @(4.3, 4.5, 0.5) (@{ all = 'woodD' })),
        (Cu @(-6.9, 12.3, -6.3) @(7.6, 1.0, 4.6) (@{ all = 'metalD'; up = 'metal' })),
        (Cu @(-6.9, 12.3, -6.55) @(7.6, 1.5, 0.25) (@{ all = 'metalD' })),
        (Cu @(-6.8, 13.2, -1.7) @(0.9, 0.6, 4.2) (@{ all = 'metalD' })),
        (Cu @(-0.1, 13.2, -1.7) @(0.9, 0.6, 4.2) (@{ all = 'metalD' })),
        (Cu @(-6.3, 13.8, -0.8) @(0.35, 0.8, 0.35) (@{ all = 'metalD' })),
        (Cu @(-0.4, 13.8, -0.8) @(0.35, 0.8, 0.35) (@{ all = 'metalD' })),
        (Cu @(2.6, 15.75, 3.4) @(1.8, 0.2, 0.35) (@{ all = 'metalD' })),
        (Cu @(2.6, 15.75, 4.85) @(1.8, 0.2, 0.35) (@{ all = 'metalD' })),
        (Cu @(2.6, 15.75, 3.75) @(0.35, 0.2, 1.1) (@{ all = 'metalD' })),
        (Cu @(4.05, 15.75, 3.75) @(0.35, 0.2, 1.1) (@{ all = 'metalD' })),
        (Cu @(2.95, 15.7, 3.75) @(1.1, 0.15, 1.1) (@{ all = 'darker' })),
        (Cu @(-5.6, 9.2, 5.6) @(3.6, 0.8, 0.6) (@{ all = 'dark' })),
        (Cu @(-5.4, 9.4, 5.55) @(0.3, 0.3, 0.12) (@{ all = 'ledr' })),
        (Cu @(-4.9, 9.4, 5.55) @(0.3, 0.3, 0.12) (@{ all = 'ledg' })),
        (Cu @(7.92, 11.8, -2.7) @(0.18, 0.9, 1.4) (@{ all = 'dark'; east = 'sticker' })),
        (Cu @(5.4, 10.0, 6.6) @(2, 2, 0.4) (@{ all = 'dark'; south = 'sock' }))
    )
    $drawer = @(
        (Cu @(4.5, 9.8, -6.8) @(3.2, 3.6, 0.35) (@{ all = 'panel' })),
        (Cu @(5.6, 11.1, -7.05) @(1.8, 0.55, 0.25) (@{ all = 'metalD'; north = 'handleS' })),
        (Cu @(4.6, 9.9, -6.45) @(3.0, 3.4, 11.5) (@{ all = 'darker' }))
    )
    Build-Furn 'gaming_desk' 2 @(
        (Bone 'body' @(0, 0, 0) $body),
        (Bone 'drawer' @(6.1, 11.5, -6.6) $drawer)
    ) @{ drawer = @{ type = 'pos'; closed = @(0, 0, 0); open = @(0, 0, -3.5) } }
    Emit-Resources 'gaming_desk' 'mid' (Recipe-Pattern @('PPP', 'I I', 'I I') ([ordered]@{ P = '#minecraft:planks'; I = 'minecraft:iron_ingot' }) 'gaming_desk')
}

# ---- console_rack: 2 tall glass-shelf steel rack ---------------------------
function Build-ConsoleRack {
    Build-Furn 'console_rack' 4 @(
        (Bone 'body' @(0, 0, 0) @(
            (Cu @(-7.75, 0, -6.15) @(1.3, 0.9, 1.3) (@{ all = 'darker' })),
            (Cu @(6.45, 0, -6.15) @(1.3, 0.9, 1.3) (@{ all = 'darker' })),
            (Cu @(-7.75, 0, 4.85) @(1.3, 0.9, 1.3) (@{ all = 'darker' })),
            (Cu @(6.45, 0, 4.85) @(1.3, 0.9, 1.3) (@{ all = 'darker' })),
            (Cu @(-7.6, 0.9, -6.0) @(1.0, 31.1, 1.0) (@{ all = 'chrome' })),
            (Cu @(6.6, 0.9, -6.0) @(1.0, 31.1, 1.0) (@{ all = 'chrome' })),
            (Cu @(-7.6, 0.9, 5.0) @(1.0, 31.1, 1.0) (@{ all = 'chrome' })),
            (Cu @(6.6, 0.9, 5.0) @(1.0, 31.1, 1.0) (@{ all = 'chrome' })),
            (Cu @(-7.6, 0.9, -6.0) @(15.2, 1.3, 12.0) (@{ all = 'metalD'; up = 'metal' })),
            (Cu @(-7.6, 30.5, -6.0) @(15.2, 1.5, 12.0) (@{ all = 'metalD'; up = 'woodTD' })),
            (Cu @(-7.3, 8.9, -5.7) @(14.6, 0.5, 11.4) (@{ all = 'chrome'; up = 'glass'; down = 'glass' })),
            (Cu @(-7.3, 16.7, -5.7) @(14.6, 0.5, 11.4) (@{ all = 'chrome'; up = 'glass'; down = 'glass' })),
            (Cu @(-7.3, 24.5, -5.7) @(14.6, 0.5, 11.4) (@{ all = 'chrome'; up = 'glass'; down = 'glass' })),
            (Cu @(-7.3, 8.9, -5.85) @(14.6, 0.5, 0.15) (@{ all = 'chrome' })),
            (Cu @(-7.3, 16.7, -5.85) @(14.6, 0.5, 0.15) (@{ all = 'chrome' })),
            (Cu @(-7.3, 24.5, -5.85) @(14.6, 0.5, 0.15) (@{ all = 'chrome' })),
            (Cu @(-0.6, 2.2, 5.4) @(1.2, 28.3, 0.6) (@{ all = 'metalD' })),
            (Cu @(-0.8, 6.0, 5.3) @(1.6, 0.5, 0.4) (@{ all = 'dark' })),
            (Cu @(-0.8, 14.0, 5.3) @(1.6, 0.5, 0.4) (@{ all = 'dark' })),
            (Cu @(-0.8, 22.0, 5.3) @(1.6, 0.5, 0.4) (@{ all = 'dark' })),
            (Cu @(5.2, 29.9, -6.1) @(2.0, 0.4, 0.25) (@{ all = 'ledb' })),
            (Cu @(-6.7, 1.15, -6.05) @(1.4, 0.8, 0.2) (@{ all = 'sticker' })),
            (Cu @(1.0, 1.6, 6.1) @(2, 2, 0.4) (@{ all = 'metalD'; south = 'sock' }))
        ))
    ) @{}
    Emit-Resources 'console_rack' 'tall' (Recipe-Pattern @('III', 'G G', 'III') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glass' }) 'console_rack')
}

# ---- controller_stand: pedestal with two cradles ---------------------------
function Build-ControllerStand {
    Build-Furn 'controller_stand' 2 @(
        (Bone 'body' @(0, 0, 0) @(
            (Cu @(-3.4, 0, -3.4) @(6.8, 0.7, 6.8) (@{ all = 'darker' })),
            (Cu @(-3.0, 0.7, -3.0) @(6.0, 0.5, 6.0) (@{ all = 'metalD' })),
            (Cu @(-0.7, 1.2, -0.7) @(1.4, 4.2, 1.4) (@{ all = 'chrome' })),
            (Cu @(-0.9, 3.2, -0.9) @(1.8, 0.4, 1.8) (@{ all = 'darker' })),
            (Cu @(-3.6, 5.4, -1.0) @(7.2, 0.7, 2.0) (@{ all = 'metalD'; up = 'metal' })),
            (Cu @(-2.7, 6.1, -1.0) @(1.6, 0.5, 2.0) (@{ all = 'darker' })),
            (Cu @(1.1, 6.1, -1.0) @(1.6, 0.5, 2.0) (@{ all = 'darker' })),
            (Cu @(-0.35, 5.5, -1.12) @(0.7, 0.3, 0.15) (@{ all = 'ledb' })),
            (Cu @(2.5, 0.12, -3.55) @(1.2, 0.5, 0.2) (@{ all = 'sticker' }))
        ))
    ) @{}
    Emit-Resources 'controller_stand' 'small' (Recipe-Pattern @('I', 'S', 'S') ([ordered]@{ I = 'minecraft:iron_ingot'; S = 'minecraft:smooth_stone' }) 'controller_stand')
}

# ---- memory_card_rack: slotted desk rack (slots take small cases) ----------
function Build-MemoryCardRack {
    $cubes = @(
        (Cu @(-4.4, 0, -2.6) @(8.8, 0.9, 5.2) (@{ all = 'dark' })),
        (Cu @(-4.4, 0, -2.75) @(8.8, 0.4, 0.15) (@{ all = 'metal' })),
        (Cu @(-4.4, 0.9, 2.2) @(8.8, 4.6, 0.9) (@{ all = 'metalD' })),
        (Cu @(-4.5, 5.5, 2.1) @(9.0, 0.6, 1.1) (@{ all = 'dark' })),
        (Cu @(-4.0, 0.9, -0.6) @(8.0, 0.35, 1.7) (@{ all = 'metalD' }))
    )
    foreach ($fx in @(-4.0, -2.4, -0.8, 0.8, 2.4, 4.0)) {
        $cubes += , (Cu @($fx, 0.9, -0.2) @(0.25, 3.8, 2.4) (@{ all = 'slotTrim' }))
    }
    $cubes += , (Cu @(-1.4, 1.0, -2.85) @(2.8, 0.55, 0.12) (@{ all = 'sticker' }))
    Build-Furn 'memory_card_rack' 2 @((Bone 'body' @(0, 0, 0) $cubes)) @{}
    Emit-Resources 'memory_card_rack' 'small' (Recipe-Pattern @('IPI', 'P P', 'SSS') ([ordered]@{ I = 'minecraft:iron_ingot'; P = '#minecraft:planks'; S = 'minecraft:smooth_stone' }) 'memory_card_rack')
}

# ---- console_display_shelf: wall shelf -------------------------------------
function Build-ConsoleDisplayShelf {
    Build-Furn 'console_display_shelf' 2 @(
        (Bone 'body' @(0, 0, 0) @(
            (Cu @(-8, 0, 6.4) @(16, 16, 0.8) (@{ all = 'woodD' })),
            (Cu @(-6.6, 13.2, 6.3) @(0.9, 0.6, 0.15) (@{ all = 'darker' })),
            (Cu @(5.7, 13.2, 6.3) @(0.9, 0.6, 0.15) (@{ all = 'darker' })),
            (Cu @(-8, 0, 3.2) @(0.9, 16, 4.0) (@{ all = 'wood' })),
            (Cu @(7.1, 0, 3.2) @(0.9, 16, 4.0) (@{ all = 'wood' })),
            (Cu @(-7.1, 0, 3.2) @(14.2, 1.0, 4.0) (@{ all = 'wood'; up = 'woodTop' })),
            (Cu @(-7.1, 7.6, 3.2) @(14.2, 1.0, 4.0) (@{ all = 'wood'; up = 'woodTop' })),
            (Cu @(-7.1, 14.9, 3.2) @(14.2, 1.1, 4.0) (@{ all = 'wood'; up = 'woodTop' })),
            (Cu @(-8, 15.6, 3.1) @(16, 0.4, 4.2) (@{ all = 'wood' })),
            (Cu @(-4.6, 7.0, 5.2) @(0.7, 0.6, 1.0) (@{ all = 'woodD' })),
            (Cu @(3.9, 7.0, 5.2) @(0.7, 0.6, 1.0) (@{ all = 'woodD' })),
            (Cu @(-4.6, 14.3, 5.2) @(0.7, 0.6, 1.0) (@{ all = 'woodD' })),
            (Cu @(3.9, 14.3, 5.2) @(0.7, 0.6, 1.0) (@{ all = 'woodD' }))
        ))
    ) @{}
    Emit-Resources 'console_display_shelf' 'mid' (Recipe-Pattern @('PPP', 'I I', 'PPP') ([ordered]@{ P = '#minecraft:planks'; I = 'minecraft:iron_ingot' }) 'console_display_shelf')
}

# ---- av_stand (v2.3): 2-wide 2000s AV stand, model centred on the seam ------
# x -16..16 px (both cells), two rows of open console bays at full size.
# Slot contract (WorkingConsolesCore.AV_STAND): bay floors y1.2 / y7.9,
# bay centres x -7.6 / +7.6, consoles z-0.2.
function Build-AvStand {
    $body = @(
        (Cu @(-16, 14.6, -6.5) @(32, 1.4, 13.0) (@{ all = 'wood'; up = 'woodTop'; down = 'woodEdge' })),
        (Cu @(-16, 14.6, -6.68) @(32, 1.4, 0.18) (@{ all = 'woodEdge' })),
        (Cu @(-16, 0, -6.5) @(1.6, 14.6, 13.0) (@{ all = 'wood' })),
        (Cu @(14.4, 0, -6.5) @(1.6, 14.6, 13.0) (@{ all = 'wood' })),
        (Cu @(-15.2, 0, -6.0) @(30.4, 1.2, 12.0) (@{ all = 'woodD' })),
        (Cu @(-14.6, 0, -6.15) @(29.2, 0.55, 0.25) (@{ all = 'darker' })),
        (Cu @(-0.7, 1.2, -6.3) @(1.4, 13.4, 12.3) (@{ all = 'wood' })),
        (Cu @(-14.4, 7.0, -6.2) @(28.8, 0.9, 12.2) (@{ all = 'wood'; up = 'woodTop' })),
        (Cu @(-14.4, 7.0, -6.38) @(28.8, 0.9, 0.18) (@{ all = 'woodEdge' })),
        (Cu @(-14.4, 1.2, 5.4) @(28.8, 13.4, 0.9) (@{ all = 'woodD' })),
        (Cu @(-14.4, 14.25, -6.6) @(28.8, 0.35, 0.2) (@{ all = 'metal' })),
        (Cu @(-15.0, 15.1, -6.75) @(0.4, 0.4, 0.12) (@{ all = 'ledg' })),
        (Cu @(5.6, 2.0, 6.3) @(2, 2, 0.4) (@{ all = 'woodD'; south = 'sock' })),
        (Cu @(-15.6, 12.6, -6.62) @(1.4, 0.85, 0.15) (@{ all = 'sticker' }))
    )
    Build-Furn 'av_stand' 2 @((Bone 'body' @(0, 0, 0) $body)) @{} 4
    Emit-Resources 'av_stand' 'mid' (Recipe-Pattern @('PPP', 'PIP', 'PGP') ([ordered]@{ P = '#minecraft:planks'; I = 'minecraft:iron_ingot'; G = 'minecraft:glass' }) 'av_stand')
}

Write-Output 'Working Consoles v5 furniture pack (v2):'
Build-TvStand
Build-AvStand
Build-EntertainmentCenter
Build-CrtCart
Build-MediaCabinet
Build-GamingDesk
Build-ConsoleRack
Build-ControllerStand
Build-MemoryCardRack
Build-ConsoleDisplayShelf
Write-Output 'done.'
