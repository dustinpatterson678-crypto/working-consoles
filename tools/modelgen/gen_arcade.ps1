# gen_arcade.ps1 - Working Consoles v5 "arcade" pack (stage A + decor expansion).
#
# Every piece is a static DecorBlock drawn by DecorRenderer (same pipeline as
# the furniture): geo + anims + a 128x128 atlas; pieces with lit parts also get
# a <name>_glowmask.png that DecorRenderer's AutoGlowingGeoLayer renders
# emissive. No slots, no new systems - pure set dressing.
#
#   arcade_carpet / arcade_carpet_zigzag / arcade_carpet_checker  full-block tile
#   floor_decal_arrow / _steps / _star                            full-block tile
#   neon_sign_open / _arcade (2w) / _tokens (2w) / _play          wall plates
#   exit_sign, arcade_directory (2w)                              wall plates
#   ceiling_light, floor_uplight                                  light fixtures
#   arcade_stool, trash_can
#   vending_machine, snack_machine, drink_machine                 tall cabinets
#   change_machine, coin_return_machine                           low cabinets
#   ticket_counter (2w), prize_counter (2w)                       counters
#
# Conventions (same as gen_furniture): model front = -z maps to FACING; model
# +x = facing.getClockWise(); y is up from the block bottom; face regions are
# painted WYSIWYG as seen from outside that face (preview.ps1 faceDefs).
#
#   powershell -File gen_arcade.ps1

. (Join-Path $PSScriptRoot 'gen_lib.ps1')

$srcRoot = Join-Path $PSScriptRoot '..\..\wcore\src\main\resources'
$assets = Join-Path $srcRoot 'assets\wcore'
$data = Join-Path $srcRoot 'data\wcore'
$geoOut = Join-Path $assets 'geckolib\models\item'
$aniOut = Join-Path $assets 'geckolib\animations\item'
$texOut = Join-Path $assets 'textures\item'

# ===========================================================================
# region table (one shared 128x128 layout; each piece paints only its own
# regions - every piece gets its own texture file)
# ===========================================================================
function New-ArcRegions {
    return [ordered]@{
        # carpets / decals
        confetti = @{ x = 0;   y = 0;   w = 16; h = 16 }
        zigzag   = @{ x = 16;  y = 0;   w = 16; h = 16 }
        checker  = @{ x = 32;  y = 0;   w = 16; h = 16 }
        decalA   = @{ x = 48;  y = 0;   w = 16; h = 16 }
        decalB   = @{ x = 64;  y = 0;   w = 16; h = 16 }
        decalC   = @{ x = 80;  y = 0;   w = 16; h = 16 }
        cedge    = @{ x = 0;   y = 17;  w = 16; h = 3 }
        cback    = @{ x = 16;  y = 17;  w = 16; h = 7 }
        tile_side  = @{ x = 96;  y = 0;  w = 16; h = 16 }
        tile_bottom = @{ x = 112; y = 24; w = 16; h = 16 }
        # materials
        mtl      = @{ x = 0;   y = 24;  w = 16; h = 16 }
        mtld     = @{ x = 16;  y = 24;  w = 16; h = 16 }
        chr      = @{ x = 32;  y = 24;  w = 16; h = 16 }
        drk      = @{ x = 48;  y = 24;  w = 16; h = 16 }
        dkr      = @{ x = 64;  y = 24;  w = 8;  h = 8 }
        wht      = @{ x = 72;  y = 24;  w = 16; h = 16 }
        rub      = @{ x = 88;  y = 24;  w = 8;  h = 8 }
        # signs / boards
        sf       = @{ x = 0;   y = 44;  w = 16; h = 8 }
        sfw      = @{ x = 0;   y = 53;  w = 32; h = 8 }
        stube    = @{ x = 32;  y = 44;  w = 8;  h = 8 }
        sframe   = @{ x = 40;  y = 44;  w = 16; h = 16 }
        xf       = @{ x = 56;  y = 44;  w = 16; h = 8 }
        dirf     = @{ x = 72;  y = 44;  w = 40; h = 16 }
        # light fixtures
        clrim    = @{ x = 0;   y = 64;  w = 16; h = 16 }
        cldff    = @{ x = 16;  y = 64;  w = 16; h = 16 }
        futrim   = @{ x = 32;  y = 64;  w = 16; h = 16 }
        fuglow   = @{ x = 48;  y = 64;  w = 16; h = 16 }
        # stool / trash
        stseat   = @{ x = 64;  y = 64;  w = 12; h = 12 }
        stside   = @{ x = 64;  y = 77;  w = 12; h = 4 }
        stbase   = @{ x = 80;  y = 64;  w = 12; h = 12 }
        trashs   = @{ x = 96;  y = 64;  w = 16; h = 16 }
        trashT   = @{ x = 72;  y = 112; w = 16; h = 16 }
        cup      = @{ x = 112; y = 64;  w = 8;  h = 10 }
        # machines: face / side / marquee / panel / flap + extras
        vface    = @{ x = 0;   y = 88;  w = 16; h = 36 }
        vside    = @{ x = 16;  y = 88;  w = 12; h = 36 }
        vmarq    = @{ x = 28;  y = 88;  w = 16; h = 6 }
        vpan     = @{ x = 28;  y = 97;  w = 8;  h = 12 }
        vflap    = @{ x = 36;  y = 97;  w = 16; h = 8 }
        board2   = @{ x = 44;  y = 88;  w = 12; h = 6 }
        band2    = @{ x = 56;  y = 88;  w = 16; h = 6 }
        misc1    = @{ x = 72;  y = 88;  w = 16; h = 12 }
        misc2    = @{ x = 88;  y = 88;  w = 16; h = 12 }
        misc3    = @{ x = 104; y = 88;  w = 16; h = 12 }
        misc4    = @{ x = 56;  y = 97;  w = 16; h = 12 }
        misc5    = @{ x = 72;  y = 100; w = 16; h = 12 }
        misc6    = @{ x = 88;  y = 100; w = 16; h = 12 }
        mface2   = @{ x = 104; y = 100; w = 16; h = 22 }
        mface3   = @{ x = 56;  y = 110; w = 16; h = 16 }
    }
}

function Assert-Regions($R) {
    $names = @($R.Keys)
    for ($i = 0; $i -lt $names.Count; $i++) {
        $a = $R[$names[$i]]
        if ($a.x -lt 0 -or $a.y -lt 0 -or ($a.x + $a.w) -gt 128 -or ($a.y + $a.h) -gt 128) {
            throw "region out of atlas: $($names[$i])"
        }
        for ($j = $i + 1; $j -lt $names.Count; $j++) {
            $b = $R[$names[$j]]
            $ox = [Math]::Min($a.x + $a.w, $b.x + $b.w) - [Math]::Max($a.x, $b.x)
            $oy = [Math]::Min($a.y + $a.h, $b.y + $b.h) - [Math]::Max($a.y, $b.y)
            if ($ox -gt 0 -and $oy -gt 0) { throw "region overlap: $($names[$i]) / $($names[$j])" }
        }
    }
}

$script:R = New-ArcRegions
Assert-Regions $script:R

# ===========================================================================
# 3x5 pixel font
# ===========================================================================
$script:F5 = @{
    'A' = @('111', '101', '111', '101', '101')
    'C' = @('111', '100', '100', '100', '111')
    'D' = @('110', '101', '101', '101', '110')
    'E' = @('111', '100', '111', '100', '111')
    'G' = @('111', '100', '101', '101', '111')
    'I' = @('111', '010', '010', '010', '111')
    'K' = @('101', '101', '110', '101', '101')
    'L' = @('100', '100', '100', '100', '111')
    'M' = @('101', '111', '111', '101', '101')
    'N' = @('101', '111', '111', '101', '101')
    'O' = @('111', '101', '101', '101', '111')
    'P' = @('110', '101', '110', '100', '100')
    'R' = @('110', '101', '110', '101', '101')
    'S' = @('111', '100', '111', '001', '111')
    'T' = @('111', '010', '010', '010', '010')
    'W' = @('101', '101', '111', '111', '101')
    'X' = @('101', '101', '010', '101', '101')
    'Y' = @('101', '101', '010', '010', '010')
    ' ' = @('000', '000', '000', '000', '000')
}

function TxtW([string]$text, [int]$scale = 1) { return ($text.Length * 4 - 1) * $scale }

function Txt($bmp, [int]$x, [int]$y, [string]$text, $col, [int]$scale = 1) {
    $cx = $x
    foreach ($ch in $text.ToCharArray()) {
        $key = [string]$ch
        if ($script:F5.ContainsKey($key)) {
            $g = $script:F5[$key]
            for ($row = 0; $row -lt 5; $row++) {
                $bits = $g[$row]
                for ($ci = 0; $ci -lt 3; $ci++) {
                    if ($bits[$ci] -eq '1') { Fill-Rect $bmp ($cx + $ci * $scale) ($y + $row * $scale) $scale $scale $col }
                }
            }
        }
        $cx += 4 * $scale
    }
}

# ===========================================================================
# shared material painters
# ===========================================================================
function Paint-ArcMetal($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#9aa0ac' 4 601
    for ($i = 2; $i -lt $r.w; $i += 5) { VLine $bmp ($r.x + $i) ($r.y + 1) ($r.h - 2) (C '#aab0bc') }
    HLine $bmp $r.x $r.y $r.w (C '#c2c8d4')
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#6a707c')
}
function Paint-ArcMetalD($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#3a3e46' 3 602
    HLine $bmp $r.x $r.y $r.w (C '#4a4e58')
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#2a2e36')
}
function Paint-ArcChrome($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#c6c8d2' 3 603
    for ($i = 3; $i -lt ($r.w - 2); $i += 6) { VLine $bmp ($r.x + $i) $r.y $r.h (C '#8a8c98') }
    VLine $bmp ($r.x + 2) ($r.y + 1) ($r.h - 2) (C '#f0f2fa')
    VLine $bmp ($r.x + 3) ($r.y + 1) ($r.h - 2) (C '#e0e2ec')
}
function Paint-ArcDark($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#1e1e24' 3 604
    HLine $bmp $r.x $r.y $r.w (C '#2a2a32')
}
function Paint-ArcDarker($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#121216' 2 605
}
function Paint-ArcWhite($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#cfd2da' 4 606
    HLine $bmp $r.x $r.y $r.w (C '#e8eaf0')
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#9aa0a8')
}
function Paint-ArcRubber($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#191a1f' 2 607
    Put-Px $bmp ($r.x + 2) ($r.y + 2) (C '#2a2b32')
    Put-Px $bmp ($r.x + 5) ($r.y + 4) (C '#101114')
}
function Paint-ArcSframe($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#232329' 3 608
    HLine $bmp $r.x $r.y $r.w (C '#32323c')
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#141418')
}
function Paint-ArcShared($bmp, $R) {
    Paint-ArcMetal   $bmp $R.mtl
    Paint-ArcMetalD  $bmp $R.mtld
    Paint-ArcChrome  $bmp $R.chr
    Paint-ArcDark    $bmp $R.drk
    Paint-ArcDarker  $bmp $R.dkr
    Paint-ArcWhite   $bmp $R.wht
    Paint-ArcRubber  $bmp $R.rub
    Paint-ArcSframe  $bmp $R.sframe
}

# ===========================================================================
# geo / animation building (same house helpers as gen_furniture)
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

function Build-Prop([string]$name, [int]$visH, $bones, [scriptblock]$paint, [scriptblock]$glow, [string]$display, $recipe, [int]$visW = 1) {
    $R = $script:R
    $bmp = New-Object System.Drawing.Bitmap(128, 128)
    Fill-Rect $bmp 0 0 128 128 (C '#141418')
    Paint-ArcShared $bmp $R
    & $paint $bmp $R
    Save-Atlas $bmp (Join-Path $texOut "$name.png")
    $bmp.Dispose()

    $glowTag = '     '
    if ($glow) {
        $g = New-Object System.Drawing.Bitmap(128, 128)
        & $glow $g $R
        Save-Atlas $g (Join-Path $texOut "${name}_glowmask.png")
        $g.Dispose()
        $glowTag = '+glow'
    }

    $json = ConvertTo-GeoJson "geometry.$name" 128 128 $visW $visH @(0, 1, 0) $bones $R
    [System.IO.File]::WriteAllText((Join-Path $geoOut "$name.geo.json"), $json, (New-Object System.Text.UTF8Encoding($false)))
    Write-Anims $name @{}

    $check = Get-Content -Raw -LiteralPath (Join-Path $geoOut "$name.geo.json") | ConvertFrom-Json
    $expected = 0; foreach ($b in $bones) { $expected += @($b.cubes).Count }
    $count = 0
    foreach ($b in $check.'minecraft:geometry'[0].bones) { $count += @($b.cubes).Count }
    $anim = Get-Content -Raw -LiteralPath (Join-Path $aniOut "$name.animation.json") | ConvertFrom-Json
    $anims = @($anim.animations.PSObject.Properties.Name).Count
    $ok = ($count -eq $expected) -and ($anims -eq 5) -and (Test-Path (Join-Path $texOut "$name.png"))
    $status = if ($ok) { 'OK' } else { 'FAILED' }
    Write-Output ("{0,-24} {1,2} cubes  {2}  {3} anims  [{4}]" -f $name, $count, $glowTag, $anims, $status)
    if (-not $ok) { throw "arcade build failed for $name" }

    Emit-Resources $name $display $recipe
}

# ===========================================================================
# resource JSON emitters (same shape as gen_furniture)
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

function Recipe-Pattern($pattern, $key, [string]$name, [int]$count = 1) {
    return [ordered]@{
        type     = 'minecraft:crafting_shaped'
        category = 'building'
        pattern  = $pattern
        key      = $key
        result   = [ordered]@{ id = "wcore:$name"; count = $count }
    }
}

# ===========================================================================
# floors: carpets + decals
# ===========================================================================
function Paint-ArcEdge($bmp, $R, [string]$accent) {
    $r = $R.cedge
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#15141a' 3 721
    HLine $bmp $r.x ($r.y + 1) $r.w (C $accent)
    for ($i = 0; $i -lt 16; $i += 5) { Put-Px $bmp ($r.x + $i) ($r.y + 1) (C '#ffffff') }
}
function Paint-ArcBack($bmp, $R) {
    $r = $R.cback
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#191920' 3 722
    for ($i = 4; $i -lt 16; $i += 4) { VLine $bmp ($r.x + $i) $r.y $r.h (C '#131318') }
}
function Paint-ArcTileSide($bmp, $R, [string]$accent) {
    $r = $R.tile_side
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#1b1922' 3 811
    HLine $bmp $r.x $r.y $r.w (C '#2c2836')
    for ($i = 3; $i -lt 16; $i += 6) { VLine $bmp ($r.x + $i) ($r.y + 1) 11 (C '#15131a') }
    Fill-Rect $bmp $r.x ($r.y + 12) $r.w 2 (C $accent)
    HLine $bmp $r.x ($r.y + 14) $r.w (C '#121016')
    HLine $bmp $r.x ($r.y + 15) $r.w (C '#0d0c10')
}
function Paint-ArcTileBottom($bmp, $R) {
    $r = $R.tile_bottom
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#141218' 3 823
    HLine $bmp $r.x $r.y $r.w (C '#0f0e12')
    HLine $bmp $r.x ($r.y + 15) $r.w (C '#0f0e12')
}
function Paint-CarpetConfetti($bmp, $R) {
    $r = $R.confetti
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#17141c' 4 731
    $rnd = New-Object System.Random(731)
    $cols = @('#ff3cc8', '#3cf0ff', '#ffe23c', '#7cff4a', '#b84aff')
    for ($i = 0; $i -lt 42; $i++) {
        $x = $rnd.Next(0, 16); $y = $rnd.Next(0, 16)
        $col = C $cols[$rnd.Next(0, 5)]
        $kind = $rnd.Next(0, 3)
        Put-Px $bmp ($r.x + $x) ($r.y + $y) $col
        if ($kind -eq 1) { Put-Px $bmp ($r.x + (($x + 1) % 16)) ($r.y + $y) $col }
        if ($kind -eq 2) { Put-Px $bmp ($r.x + $x) ($r.y + (($y + 1) % 16)) $col }
    }
    for ($i = 0; $i -lt 12; $i++) { Put-Px $bmp ($r.x + $rnd.Next(0, 16)) ($r.y + $rnd.Next(0, 16)) (C '#d8d8e2') }
}
function Paint-CarpetZigzag($bmp, $R) {
    $r = $R.zigzag
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#101018' 3 741
    $c1 = C '#3cf0ff'; $c2 = C '#ff3cc8'
    for ($band = 0; $band -lt 3; $band++) {
        $col = if ($band % 2 -eq 0) { $c1 } else { $c2 }
        for ($x = 0; $x -lt 16; $x++) {
            $tri = [Math]::Abs((($x + $band * 4) % 8) - 4)
            Put-Px $bmp ($r.x + $x) ($r.y + ($band * 5 + $tri)) $col
        }
    }
    foreach ($p in @(@(2, 4), @(13, 9), @(7, 14))) {
        foreach ($d in @(@(0, 0), @(-1, 0), @(1, 0), @(0, -1), @(0, 1))) {
            $x = $r.x + $p[0] + $d[0]; $y = $r.y + $p[1] + $d[1]
            if ($x -ge $r.x -and $x -lt ($r.x + 16) -and $y -ge $r.y -and $y -lt ($r.y + 16)) { Put-Px $bmp $x $y (C '#ffe23c') }
        }
    }
}
function Paint-CarpetChecker($bmp, $R) {
    $r = $R.checker
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $odd = ((([int]($x / 4)) + ([int]($y / 4))) % 2) -eq 1
            $col = if ($odd) { C '#1d1830' } else { C '#14121c' }
            Put-Px $bmp ($r.x + $x) ($r.y + $y) $col
        }
    }
    $rnd = New-Object System.Random(751)
    for ($i = 0; $i -lt 10; $i++) {
        $x = 2 + $rnd.Next(0, 6) * 2; $y = 2 + $rnd.Next(0, 6) * 2
        Put-Px $bmp ($r.x + $x) ($r.y + $y) (C '#b84aff')
        Put-Px $bmp ($r.x + $x) ($r.y + $y + 1) (C '#b84aff')
    }
    for ($i = 0; $i -lt 6; $i++) { Put-Px $bmp ($r.x + $rnd.Next(0, 16)) ($r.y + $rnd.Next(0, 16)) (C '#ffe23c') }
}
function Build-FloorTile([string]$name, [string]$face, [string]$accent, [scriptblock]$paintFace, [scriptblock]$glowFace, $recipe) {
    # Full solid 16x16x16 block: the pattern is the top face, the arcade
    # skirting (accent stripe + panel seams) wraps the four sides, and the
    # underside is a plain dark base.
    $body = @((Cu @(-8, 0, -8) @(16, 16, 16) (@{ up = $face; down = 'tile_bottom'; north = 'tile_side'; east = 'tile_side'; south = 'tile_side'; west = 'tile_side' })))
    $paint = {
        param($bmp, $R)
        & $paintFace $bmp $R
        Paint-ArcTileSide $bmp $R $accent
        Paint-ArcTileBottom $bmp $R
    }.GetNewClosure()
    $glow = $null
    if ($glowFace) { $glow = { param($g, $R) & $glowFace $g $R }.GetNewClosure() }
    Build-Prop $name 1 @((Bone 'body' @(0, 0, 0) $body)) $paint $glow 'small' $recipe
}
function Paint-DecalArrow($bmp, $R, [bool]$glowOnly = $false) {
    $r = $R.decalA
    if (-not $glowOnly) { Noise-Rect $bmp $r.x $r.y $r.w $r.h '#141218' 3 761 }
    $body = C '#ffd23c'
    Put-Px $bmp ($r.x + 7) ($r.y + 3) $body; Put-Px $bmp ($r.x + 8) ($r.y + 3) $body
    for ($i = 0; $i -lt 4; $i++) {
        $x0 = 6 - $i; $x1 = 9 + $i; $yy = $r.y + 4 + $i
        Put-Px $bmp ($r.x + $x0) $yy $body; Put-Px $bmp ($r.x + $x1) $yy $body
        for ($x = $x0 + 1; $x -lt $x1; $x++) { Put-Px $bmp ($r.x + $x) $yy $body }
    }
    Fill-Rect $bmp ($r.x + 7) ($r.y + 8) 2 5 $body
}
function Paint-DecalSteps($bmp, $R, [bool]$glowOnly = $false) {
    $r = $R.decalB
    if (-not $glowOnly) { Noise-Rect $bmp $r.x $r.y $r.w $r.h '#141218' 3 762 }
    $c = C '#3cf0ff'
    for ($k = 0; $k -lt 3; $k++) {
        $cx = 1 + $k * 5
        Put-Px $bmp ($r.x + $cx + 2) ($r.y + 5) $c
        Put-Px $bmp ($r.x + $cx + 1) ($r.y + 6) $c; Put-Px $bmp ($r.x + $cx + 3) ($r.y + 6) $c
        Put-Px $bmp ($r.x + $cx) ($r.y + 7) $c; Put-Px $bmp ($r.x + $cx + 4) ($r.y + 7) $c
        Put-Px $bmp ($r.x + $cx + 2) ($r.y + 9) $c
    }
}
function Paint-DecalStar($bmp, $R, [bool]$glowOnly = $false) {
    $r = $R.decalC
    if (-not $glowOnly) { Noise-Rect $bmp $r.x $r.y $r.w $r.h '#141218' 3 763 }
    $c = C '#ff3cc8'
    Fill-Rect $bmp ($r.x + 7) ($r.y + 3) 2 10 $c
    HLine $bmp ($r.x + 2) ($r.y + 7) 12 $c
    HLine $bmp ($r.x + 2) ($r.y + 8) 12 $c
    foreach ($p in @(@(3, 3), @(12, 3), @(3, 12), @(12, 12))) { Put-Px $bmp ($r.x + $p[0]) ($r.y + $p[1]) (C '#3cf0ff') }
    if (-not $glowOnly) { Put-Px $bmp ($r.x + 7) ($r.y + 4) (C '#ffffff') }
}

# ===========================================================================
# neon signs + exit + directory
# ===========================================================================
function Build-Sign([string]$name, [string]$text, [string]$hex, [bool]$wide, [string]$faceRegion, [int]$plateW, [int]$plateH, [int]$plateY, $recipe) {
    $hw = $plateW / 2.0
    $body = @(
        (Cu @(-$hw, $plateY, 5.2) @($plateW, $plateH, 1.2) (@{ all = 'sframe'; north = $faceRegion })),
        (Cu @((-($hw + 0.25)), ($plateY + $plateH), 4.7) @(($plateW + 0.5), 0.5, 0.5) (@{ all = 'stube' })),
        (Cu @((-($hw + 0.25)), ($plateY - 0.5), 4.7) @(($plateW + 0.5), 0.5, 0.5) (@{ all = 'stube' })),
        (Cu @((-($hw + 0.25)), $plateY, 4.7) @(0.5, $plateH, 0.5) (@{ all = 'stube' })),
        (Cu @(($hw - 0.25), $plateY, 4.7) @(0.5, $plateH, 0.5) (@{ all = 'stube' })),
        (Cu @(-0.6, ($plateY + $plateH + 0.5), 6.4) @(1.2, 0.4, 1.6) (@{ all = 'mtld' }))
    )
    $fr = $script:R[$faceRegion]
    $tw = TxtW $text
    $tx = $fr.x + [int](($fr.w - $tw) / 2)
    $ty = $fr.y + [int](($fr.h - 5) / 2)
    $paint = {
        param($bmp, $R)
        Paint-ArcSframe $bmp $R.sframe
        $rr = $R[$faceRegion]
        Fill-Rect $bmp $rr.x $rr.y $rr.w $rr.h (C '#15151b')
        Fill-Rect $bmp ($rr.x + 1) ($rr.y + 1) ($rr.w - 2) ($rr.h - 2) (C '#0b0b10')
        HLine $bmp $rr.x $rr.y $rr.w (C '#33333d')
        Txt $bmp ($tx + 1) ($ty + 1) $text (C '#060608')
        Txt $bmp $tx $ty $text (C $hex)
        Fill-Rect $bmp $R.stube.x $R.stube.y $R.stube.w $R.stube.h (C $hex)
        HLine $bmp ($R.stube.x + 1) ($R.stube.y + 2) ($R.stube.w - 2) (Shift-Col (C $hex) 60)
    }.GetNewClosure()
    $glow = {
        param($g, $R)
        Txt $g $tx $ty $text (C $hex)
        Fill-Rect $g $R.stube.x $R.stube.y $R.stube.w $R.stube.h (C $hex)
    }.GetNewClosure()
    Build-Prop $name 1 @((Bone 'body' @(0, 0, 0) $body)) $paint $glow 'small' $recipe $(if ($wide) { 2 } else { 1 })
}
function Build-Directory {
    $body = @(
        (Cu @(-14, 6.2, 5.2) @(28, 11, 1.2) (@{ all = 'sframe'; north = 'dirf' })),
        (Cu @(-13.9, 5.5, 4.9) @(27.8, 0.4, 0.4) (@{ all = 'stube' })),
        (Cu @(-10.0, 17.4, 6.2) @(1.2, 0.5, 1.4) (@{ all = 'mtld' })),
        (Cu @(8.8, 17.4, 6.2) @(1.2, 0.5, 1.4) (@{ all = 'mtld' }))
    )
    $rows = @(@(4, 8, 17, '#3cf0ff'), @(4, 10, 10, '#9aa0ac'), @(4, 12, 22, '#ff8ad0'), @(4, 14, 14, '#c8ccd4'))
    $paint = {
        param($bmp, $R)
        Paint-ArcSframe $bmp $R.sframe
        $rr = $R.dirf
        Noise-Rect $bmp $rr.x $rr.y $rr.w $rr.h '#15161d' 3 781
        HLine $bmp $rr.x $rr.y $rr.w (C '#2c2e3a')
        HLine $bmp $rr.x ($rr.y + 1) $rr.w (C '#23252f')
        $tw = TxtW 'GAMES'
        Txt $bmp ($rr.x + [int](($rr.w - $tw) / 2)) ($rr.y + 2) 'GAMES' (C '#e8f0ff')
        foreach ($row in $rows) {
            HLine $bmp ($rr.x + $row[0]) ($rr.y + $row[1]) $row[2] (C $row[3])
            Put-Px $bmp ($rr.x + 2) ($rr.y + $row[1]) (C '#ffe23c')
        }
        Fill-Rect $bmp $R.stube.x $R.stube.y $R.stube.w $R.stube.h (C '#e8f0ff')
    }.GetNewClosure()
    $glow = {
        param($g, $R)
        $rr = $R.dirf
        $tw = TxtW 'GAMES'
        Txt $g ($rr.x + [int](($rr.w - $tw) / 2)) ($rr.y + 2) 'GAMES' (C '#e8f0ff')
        foreach ($row in @(@(4, 8, 17, '#3cf0ff'), @(4, 12, 22, '#ff8ad0'))) {
            HLine $g ($rr.x + $row[0]) ($rr.y + $row[1]) $row[2] (C $row[3])
        }
        Fill-Rect $g $R.stube.x $R.stube.y $R.stube.w $R.stube.h (C '#e8f0ff')
    }
    Build-Prop 'arcade_directory' 1 @((Bone 'body' @(0, 0, 0) $body)) $paint $glow 'small' (Recipe-Pattern @('III', 'GDG', 'III') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glowstone'; D = 'minecraft:black_dye' }) 'arcade_directory') 2
}

# ===========================================================================
# light fixtures
# ===========================================================================
function Build-CeilingLight {
    $body = @(
        (Cu @(-7.2, 14.2, -7.2) @(14.4, 1.8, 14.4) (@{ all = 'wht'; down = 'clrim' })),
        (Cu @(-5.8, 13.9, -5.8) @(11.6, 0.5, 11.6) (@{ all = 'cldff' }))
    )
    $paint = {
        param($bmp, $R)
        $rr = $R.clrim
        Noise-Rect $bmp $rr.x $rr.y $rr.w $rr.h '#c9ccd4' 3 791
        Fill-Rect $bmp ($rr.x + 1) ($rr.y + 1) ($rr.w - 2) ($rr.h - 2) (C '#7a7e88')
        Fill-Rect $bmp ($rr.x + 3) ($rr.y + 3) ($rr.w - 6) ($rr.h - 6) (C '#33363c')
        foreach ($p in @(@(2, 2), @(13, 2), @(2, 13), @(13, 13))) { Put-Px $bmp ($rr.x + $p[0]) ($rr.y + $p[1]) (C '#e8eaf0') }
        $d = $R.cldff
        Fill-Rect $bmp $d.x $d.y $d.w $d.h (C '#fff6dc')
        for ($i = 4; $i -lt 16; $i += 4) { VLine $bmp ($d.x + $i) ($d.y + 1) 14 (C '#f0e2b8') }
        for ($j = 4; $j -lt 16; $j += 4) { HLine $bmp ($d.x + 1) ($d.y + $j) 14 (C '#f0e2b8') }
    }
    $glow = {
        param($g, $R)
        $d = $R.cldff
        Fill-Rect $g $d.x $d.y $d.w $d.h (C '#fff2cc')
    }
    Build-Prop 'ceiling_light' 1 @((Bone 'body' @(0, 0, 0) $body)) $paint $glow 'small' (Recipe-Pattern @('GGG', 'GLG', 'GGG') ([ordered]@{ G = 'minecraft:glass'; L = 'minecraft:glowstone_dust' }) 'ceiling_light')
}
function Build-FloorUplight {
    $body = @(
        (Cu @(-8, 0, -8) @(16, 1.0, 16) (@{ all = 'mtld'; up = 'futrim'; down = 'dkr' })),
        (Cu @(-4.2, 0.9, -4.2) @(8.4, 0.5, 8.4) (@{ all = 'fuglow' }))
    )
    $paint = {
        param($bmp, $R)
        $rr = $R.futrim
        Noise-Rect $bmp $rr.x $rr.y $rr.w $rr.h '#b8bcc6' 3 801
        HLine $bmp $rr.x $rr.y $rr.w (C '#d4d8e0')
        foreach ($p in @(@(1, 1), @(14, 1), @(1, 14), @(14, 14))) { Put-Px $bmp ($rr.x + $p[0]) ($rr.y + $p[1]) (C '#e8eaf0') }
        Fill-Rect $bmp ($rr.x + 3) ($rr.y + 3) 10 10 (C '#4a4e56')
        Fill-Rect $bmp ($rr.x + 4) ($rr.y + 4) 8 8 (C '#26242a')
        $g = $R.fuglow
        for ($y = 0; $y -lt 16; $y++) {
            for ($x = 0; $x -lt 16; $x++) {
                $d = [Math]::Sqrt([Math]::Pow($x - 7.5, 2) + [Math]::Pow($y - 7.5, 2))
                $col = if ($d -lt 3.0) { C '#fff2c0' } elseif ($d -lt 3.9) { C '#ffd23c' } elseif ($d -lt 4.5) { C '#8a6a12' } else { C '#26242a' }
                Put-Px $bmp ($g.x + $x) ($g.y + $y) $col
            }
        }
    }
    $glow = {
        param($g, $R)
        $r = $R.fuglow
        for ($y = 0; $y -lt 16; $y++) {
            for ($x = 0; $x -lt 16; $x++) {
                $d = [Math]::Sqrt([Math]::Pow($x - 7.5, 2) + [Math]::Pow($y - 7.5, 2))
                if ($d -lt 3.9) { Put-Px $g ($r.x + $x) ($r.y + $y) (C '#ffe8a0') }
            }
        }
    }
    Build-Prop 'floor_uplight' 1 @((Bone 'body' @(0, 0, 0) $body)) $paint $glow 'small' (Recipe-Pattern @('IGI', 'GLG', 'IGI') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glass'; L = 'minecraft:glowstone_dust' }) 'floor_uplight')
}

# ===========================================================================
# arcade stool + trash can
# ===========================================================================
function Build-ArcadeStool {
    $body = @(
        (Cu @(-4.2, 0, -4.2) @(8.4, 0.7, 8.4) (@{ all = 'chr'; up = 'stbase'; down = 'dkr' })),
        (Cu @(-0.7, 0.7, -0.7) @(1.4, 7.6, 1.4) (@{ all = 'chr' })),
        (Cu @(-3.4, 3.6, -3.4) @(6.8, 0.8, 0.8) (@{ all = 'chr' })),
        (Cu @(-3.4, 3.6, 2.6) @(6.8, 0.8, 0.8) (@{ all = 'chr' })),
        (Cu @(-3.4, 3.6, -2.6) @(0.8, 0.8, 5.2) (@{ all = 'chr' })),
        (Cu @(2.6, 3.6, -2.6) @(0.8, 0.8, 5.2) (@{ all = 'chr' })),
        (Cu @(-2.2, 7.9, -2.2) @(4.4, 0.4, 4.4) (@{ all = 'mtld' })),
        (Cu @(-3.7, 8.3, -3.7) @(7.4, 1.7, 7.4) (@{ all = 'stside'; up = 'stseat'; down = 'drk' }))
    )
    $paint = {
        param($bmp, $R)
        $s = $R.stseat
        for ($y = 0; $y -lt 12; $y++) {
            for ($x = 0; $x -lt 12; $x++) {
                $d = [Math]::Sqrt([Math]::Pow($x - 5.5, 2) + [Math]::Pow($y - 5.5, 2))
                $col = if ($d -lt 4.7) { C '#2e2e36' } elseif ($d -lt 5.4) { C '#ff3cc8' } else { C '#1a1a1f' }
                if ($d -lt 4.7 -and (($x + $y) % 3) -eq 0) { $col = C '#34343c' }
                Put-Px $bmp ($s.x + $x) ($s.y + $y) $col
            }
        }
        HLine $bmp ($s.x + 3) ($s.y + 5) 6 (C '#4a4a54')
        $sd = $R.stside
        Noise-Rect $bmp $sd.x $sd.y $sd.w $sd.h '#232329' 3 811
        HLine $bmp $sd.x ($sd.y + 1) $sd.w (C '#ff3cc8')
        HLine $bmp $sd.x ($sd.y + 3) $sd.w (C '#9aa0ac')
        $b = $R.stbase
        for ($y = 0; $y -lt 12; $y++) {
            for ($x = 0; $x -lt 12; $x++) {
                $d = [Math]::Sqrt([Math]::Pow($x - 5.5, 2) + [Math]::Pow($y - 5.5, 2))
                $col = if ($d -lt 4.2) { C '#b8bcc6' } elseif ($d -lt 5.3) { C '#8a8e98' } else { C '#1a1a1f' }
                Put-Px $bmp ($b.x + $x) ($b.y + $y) $col
            }
        }
        Fill-Rect $bmp ($b.x + 5) ($b.y + 5) 2 2 (C '#e8eaf0')
    }
    Build-Prop 'arcade_stool' 1 @((Bone 'body' @(0, 0, 0) $body)) $paint $null 'small' (Recipe-Pattern @('WWW', 'III', ' I ') ([ordered]@{ W = '#minecraft:wool'; I = 'minecraft:iron_ingot' }) 'arcade_stool')
}
function Build-TrashCan {
    $body = @(
        (Cu @(-3.5, 0, -3.5) @(7, 9.5, 7) (@{ all = 'trashs'; up = 'dkr'; down = 'dkr' })),
        (Cu @(-4, 9.5, -4) @(8, 1, 8) (@{ all = 'mtld' })),
        (Cu @(-3.4, 10.5, -3.4) @(6.8, 0.6, 6.8) (@{ all = 'trashs'; up = 'trashT' })),
        (Cu @(-3.6, 11.1, 1.2) @(1.6, 2.0, 1.6) (@{ all = 'cup' }))
    )
    $paint = {
        param($bmp, $R)
        $rr = $R.trashs
        Noise-Rect $bmp $rr.x $rr.y $rr.w $rr.h '#34363c' 3 821
        for ($i = 0; $i -lt 16; $i += 3) { VLine $bmp ($rr.x + $i) ($rr.y + 1) 14 (C '#3e4046') }
        for ($j = 4; $j -lt 16; $j += 5) { HLine $bmp ($rr.x + 1) ($rr.y + $j) 14 (C '#2a2c32') }
        HLine $bmp $rr.x $rr.y $rr.w (C '#484a52')
        $t = $R.trashT
        Noise-Rect $bmp $t.x $t.y $t.w $t.h '#2c2e34' 3 822
        Fill-Rect $bmp ($t.x + 4) ($t.y + 5) 8 5 (C '#14161a')
        Fill-Rect $bmp ($t.x + 5) ($t.y + 6) 6 3 (C '#0c0e12')
        Put-Px $bmp ($t.x + 2) ($t.y + 2) (C '#8a8e98')
        Put-Px $bmp ($t.x + 13) ($t.y + 2) (C '#8a8e98')
        $c = $R.cup
        Noise-Rect $bmp $c.x $c.y $c.w $c.h '#d8d4cc' 3 823
        HLine $bmp $c.x ($c.y + 3) $c.w (C '#c83232')
        Fill-Rect $bmp ($c.x + 1) ($c.y + 6) 6 1 (C '#c83232')
        Fill-Rect $bmp ($c.x + 1) ($c.y - 1) 2 2 (C '#9aa0ac')
        HLine $bmp $c.x ($c.y + 8) $c.w (C '#b8b4ac')
    }
    Build-Prop 'trash_can' 1 @((Bone 'body' @(0, 0, 0) $body)) $paint $null 'small' (Recipe-Pattern @('I I', 'I I', 'III') ([ordered]@{ I = 'minecraft:iron_ingot' }) 'trash_can')
}

# ===========================================================================
# machines
# ===========================================================================
function Make-TallMachineBones([double]$bodyH) {
    $yTop = 1.6 + $bodyH
    return @(
        (Cu @(-7, 1.6, -5) @(14, $bodyH, 10) (@{ north = 'vface'; east = 'vside'; west = 'vside'; south = 'vside'; up = 'mtld'; down = 'mtld' })),
        (Cu @(-7, 0, -5.3) @(14, 1.6, 10.6) (@{ all = 'mtld' })),
        (Cu @(-7.15, $yTop, -5.15) @(14.3, 0.9, 10.3) (@{ all = 'mtld' })),
        (Cu @(-6.5, ($yTop - 5.2), -5.55) @(13, 4.6, 0.55) (@{ all = 'mtld'; north = 'vmarq' })),
        (Cu @(-6.1, 14.0, -5.55) @(3.2, 9.0, 0.55) (@{ all = 'mtld'; north = 'vpan' })),
        (Cu @(-6.3, 13.6, -5.7) @(3.6, 0.5, 0.9) (@{ all = 'chr' })),
        (Cu @(-4.6, 2.4, -5.55) @(9.2, 3.6, 0.55) (@{ all = 'mtld'; north = 'vflap' })),
        (Cu @(-1.5, 4.6, -5.75) @(3, 0.6, 0.35) (@{ all = 'chr' })),
        (Cu @(-7.1, 1.6, -5.1) @(0.5, $bodyH, 0.5) (@{ all = 'chr' })),
        (Cu @(6.6, 1.6, -5.1) @(0.5, $bodyH, 0.5) (@{ all = 'chr' })),
        (Cu @(-6.6, 0, -5.35) @(2.2, 0.8, 0.7) (@{ all = 'dkr' })),
        (Cu @(4.4, 0, -5.35) @(2.2, 0.8, 0.7) (@{ all = 'dkr' }))
    )
}
function Paint-VendFace($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#a82832' 4 800
        HLine $bmp $r.x $r.y $r.w (C '#c84050')
        HLine $bmp $r.x ($r.y + 2) $r.w (C '#7c1820')
        $wx = $r.x + 1; $wy = $r.y + 6
        Fill-Rect $bmp $wx $wy 11 22 (C '#0a0c10')
        Noise-Rect $bmp $wx ($wy + 1) 11 20 '#101319' 2 801
        VLine $bmp ($r.x + 13) ($r.y + 4) 30 (C '#781c26')
        VLine $bmp ($r.x + 12) ($r.y + 4) 30 (C '#c04a56')
        for ($i = 1; $i -lt 11; $i++) {
            $sy = $wy + 20 - $i * 2
            if ($sy -gt $wy) { Put-Px $bmp ($wx + $i) $sy (C '#7c94ac') }
        }
    }
    $wx = $r.x + 1; $wy = $r.y + 6
    $rows = @(
        @('#d23030', '#e8d23c', '#3cc8e8'),
        @('#e8823c', '#7cd23c', '#a04ae0'),
        @('#d23030', '#3cc8e8', '#e8d23c')
    )
    for ($row = 0; $row -lt 3; $row++) {
        $shelfY = $wy + 6 + $row * 5
        if (-not $glowOnly) {
            HLine $bmp $wx $shelfY 11 (C '#4a505c')
            HLine $bmp $wx ($shelfY + 1) 11 (C '#262b33')
        }
        for ($k = 0; $k -lt 3; $k++) {
            $cx = $wx + 1 + $k * 4; $cy = $shelfY - 3
            $col = C $rows[$row][$k]
            Fill-Rect $bmp $cx $cy 2 3 $col
            HLine $bmp $cx $cy 2 (Shift-Col $col 40)
            HLine $bmp $cx ($cy + 2) 2 (Shift-Col $col -50)
            Put-Px $bmp $cx $cy (C '#ffffff')
        }
    }
}
function Paint-VMarq($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#1a1c22' 3 860
        HLine $bmp $r.x $r.y $r.w (C '#33363c')
        HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#0e1014')
        Put-Px $bmp ($r.x + 1) ($r.y + 3) (C '#e8e8f0')
        Put-Px $bmp ($r.x + $r.w - 2) ($r.y + 2) (C '#e8e8f0')
    }
    $tw = TxtW 'SODA'
    $tx = $r.x + [int](($r.w - $tw) / 2)
    $ty = $r.y + [int](($r.h - 5) / 2)
    Txt $bmp ($tx + 1) ($ty + 1) 'SODA' (C '#060608')
    Txt $bmp $tx $ty 'SODA' (C '#3cf0ff')
}
function Paint-SMarq($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#1a1c22' 3 804
        HLine $bmp $r.x $r.y $r.w (C '#33363c')
        HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#0e1014')
        Put-Px $bmp ($r.x + 1) ($r.y + 4) (C '#e8e8f0')
        Put-Px $bmp ($r.x + $r.w - 2) ($r.y + 2) (C '#e8e8f0')
    }
    $bx = $r.x + 3; $by = $r.y + 1
    Fill-Rect $bmp $bx ($by + 1) 10 3 (C '#e8c83c')
    HLine $bmp $bx $by 10 (C '#c8a828')
    HLine $bmp $bx ($by + 4) 10 (C '#c8a828')
    HLine $bmp $bx ($by + 2) 10 (C '#c83232')
    Put-Px $bmp ($bx + 5) ($by + 1) (C '#ffffff')
    Put-Px $bmp ($bx + 1) ($by + 3) (C '#f0d860')
    Put-Px $bmp ($bx + 8) ($by + 3) (C '#f0d860')
}
function Paint-SnackFace($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#2e3138' 4 802
        HLine $bmp $r.x $r.y $r.w (C '#c83640')
        HLine $bmp $r.x ($r.y + 1) $r.w (C '#98262e')
        HLine $bmp $r.x ($r.y + 2) $r.w (C '#1e2026')
        $wx = $r.x + 1; $wy = $r.y + 6
        Fill-Rect $bmp $wx $wy 11 22 (C '#0a0c10')
        Noise-Rect $bmp $wx ($wy + 1) 11 20 '#101319' 2 803
        VLine $bmp ($r.x + 13) ($r.y + 4) 30 (C '#16181c')
        VLine $bmp ($r.x + 12) ($r.y + 4) 30 (C '#3e4148')
        for ($i = 1; $i -lt 11; $i++) {
            $sy = $wy + 20 - $i * 2
            if ($sy -gt $wy) { Put-Px $bmp ($wx + $i) $sy (C '#7c94ac') }
        }
    }
    $wx = $r.x + 1; $wy = $r.y + 6
    $bagCols = @(
        @('#e8c83c', '#d24040', '#3c6ce0'),
        @('#5cc840', '#e8883c', '#a04ae0'),
        @('#d24040', '#e8c83c', '#5cc840')
    )
    for ($row = 0; $row -lt 3; $row++) {
        $shelfY = $wy + 6 + $row * 5
        if (-not $glowOnly) {
            HLine $bmp $wx $shelfY 11 (C '#4a505c')
            HLine $bmp $wx ($shelfY + 1) 11 (C '#262b33')
        }
        for ($k = 0; $k -lt 3; $k++) {
            $bx = $wx + 1 + $k * 4; $by = $shelfY - 4
            $col = C $bagCols[$row][$k]
            Fill-Rect $bmp $bx ($by + 1) 3 3 $col
            HLine $bmp $bx $by 3 (Shift-Col $col -70)
            HLine $bmp $bx ($by + 3) 3 (Shift-Col $col -40)
            Put-Px $bmp ($bx + 1) ($by + 2) (Shift-Col $col 60)
            Put-Px $bmp $bx ($by + 1) (Shift-Col $col 30)
        }
    }
}
function Paint-PanelDrink($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#22242a' 3 850
        HLine $bmp $r.x $r.y $r.w (C '#32343c')
        HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#14161a')
        Fill-Rect $bmp ($r.x + 1) ($r.y + 1) 1 4 (C '#0a0a0e')
        for ($ky = 0; $ky -lt 3; $ky++) {
            for ($kx = 0; $kx -lt 3; $kx++) { Put-Px $bmp ($r.x + 4 + $kx) ($r.y + 2 + $ky * 2) (C '#c8ccd4') }
        }
        VLine $bmp ($r.x + 1) ($r.y + 7) 3 (C '#0a0a0e')
        Fill-Rect $bmp ($r.x + 4) ($r.y + 9) 3 2 (C '#143018')
    }
    Fill-Rect $bmp ($r.x + 4) ($r.y + 9) 3 2 (C '#3ce65a')
    Put-Px $bmp ($r.x + 4) ($r.y + 9) (C '#c8ffd0')
    Put-Px $bmp ($r.x + 2) ($r.y + 1) (C '#3ce65a')
}
function Paint-PanelBill($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#22242a' 3 851
        HLine $bmp $r.x $r.y $r.w (C '#32343c')
        HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#14161a')
        Fill-Rect $bmp ($r.x + 2) ($r.y + 2) 4 1 (C '#0a0a0e')
        Fill-Rect $bmp ($r.x + 2) ($r.y + 4) 4 1 (C '#8a8e98')
        Fill-Rect $bmp ($r.x + 4) ($r.y + 9) 3 2 (C '#143018')
    }
    Fill-Rect $bmp ($r.x + 4) ($r.y + 9) 3 2 (C '#3ce65a')
    Put-Px $bmp ($r.x + 4) ($r.y + 9) (C '#c8ffd0')
    Put-Px $bmp ($r.x + 6) ($r.y + 3) (C '#3ce65a')
}
function Paint-Flap($bmp, $r, [bool]$glowOnly = $false) {
    if ($glowOnly) { return }
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#26282e' 3 841
    HLine $bmp $r.x $r.y $r.w (C '#36383e')
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#16181c')
    Fill-Rect $bmp ($r.x + 3) ($r.y + 2) 10 3 (C '#14161a')
    HLine $bmp ($r.x + 4) ($r.y + 3) 8 (C '#8a8e98')
    Put-Px $bmp ($r.x + 1) ($r.y + 1) (C '#8a8e98')
    Put-Px $bmp ($r.x + 14) ($r.y + 1) (C '#8a8e98')
}
function Paint-MachineSide($bmp, $r, [string]$baseHex, [int]$seed) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $baseHex 4 $seed
    HLine $bmp $r.x $r.y $r.w (Shift-Col (C $baseHex) 30)
    $groove = [int]($r.w / 2)
    VLine $bmp ($r.x + $groove) ($r.y + 3) ($r.h - 8) (Shift-Col (C $baseHex) -60)
    VLine $bmp ($r.x + $groove + 1) ($r.y + 3) ($r.h - 8) (Shift-Col (C $baseHex) 35)
    for ($i = 0; $i -lt 3; $i++) { HLine $bmp ($r.x + 2) ($r.y + $r.h - 7 + $i * 2) 5 (C '#141418') }
    Fill-Rect $bmp ($r.x + 2) ($r.y + 6) 4 3 (C '#e6e6ea')
    Put-Px $bmp ($r.x + 2) ($r.y + 6) (C '#c83232')
    VLine $bmp ($r.x + $r.w - 1) ($r.y + 1) ($r.h - 2) (C '#101014')
}
function Build-VendingMachine {
    $paint = {
        param($bmp, $R)
        Paint-VendFace $bmp $R.vface
        Paint-VMarq $bmp $R.vmarq
        Paint-PanelDrink $bmp $R.vpan
        Paint-Flap $bmp $R.vflap
        Paint-MachineSide $bmp $R.vside '#8e242c' 831
    }
    $glow = {
        param($g, $R)
        Paint-VendFace $g $R.vface $true
        Paint-VMarq $g $R.vmarq $true
        Paint-PanelDrink $g $R.vpan $true
    }
    Build-Prop 'vending_machine' 2 @((Bone 'body' @(0, 0, 0) (Make-TallMachineBones 29.4))) $paint $glow 'tall' (Recipe-Pattern @('IGI', 'GDG', 'IRI') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glass'; D = 'minecraft:sugar'; R = 'minecraft:redstone' }) 'vending_machine')
}
function Build-SnackMachine {
    $paint = {
        param($bmp, $R)
        Paint-SnackFace $bmp $R.vface
        Paint-SMarq $bmp $R.vmarq
        Paint-PanelDrink $bmp $R.vpan
        Paint-Flap $bmp $R.vflap
        Paint-MachineSide $bmp $R.vside '#292c32' 832
    }
    $glow = {
        param($g, $R)
        Paint-SnackFace $g $R.vface $true
        Paint-SMarq $g $R.vmarq $true
        Paint-PanelDrink $g $R.vpan $true
    }
    Build-Prop 'snack_machine' 2 @((Bone 'body' @(0, 0, 0) (Make-TallMachineBones 29.4))) $paint $glow 'tall' (Recipe-Pattern @('IGI', 'GDG', 'IRI') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glass'; D = 'minecraft:wheat'; R = 'minecraft:redstone' }) 'snack_machine')
}

function Paint-DrinkFace($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#3a3d44' 4 805
        HLine $bmp $r.x $r.y $r.w (C '#4a4e56')
        HLine $bmp $r.x ($r.y + 5) $r.w (C '#22242a')
        HLine $bmp $r.x ($r.y + 33) $r.w (C '#22242a')
        $wx = $r.x + 1; $wy = $r.y + 7
        Fill-Rect $bmp $wx $wy 14 24 (C '#0e1116')
        Noise-Rect $bmp $wx ($wy + 1) 14 22 '#131820' 2 806
        $fr = C '#565a62'
        HLine $bmp $wx ($wy - 1) 14 $fr
        HLine $bmp $wx ($wy + 24) 14 $fr
        VLine $bmp ($wx - 1) $wy 24 $fr
        VLine $bmp ($wx + 14) $wy 24 $fr
        for ($i = 1; $i -lt 13; $i++) {
            $sy = $wy + 22 - $i * 2
            if ($sy -gt $wy) { Put-Px $bmp ($wx + $i) $sy (C '#7c94ac') }
        }
    }
    $tw = TxtW 'COLD'
    $tx = $r.x + [int](($r.w - $tw) / 2)
    Txt $bmp ($tx + 1) ($r.y + 1) 'COLD' (C '#060608')
    Txt $bmp $tx $r.y 'COLD' (C '#7cd8ff')
    $wx = $r.x + 1; $wy = $r.y + 7
    $rows = @(
        @('#6a3c28', '#3c7c4a', '#c8873c', '#7cc8e0'),
        @('#3c7c4a', '#c8873c', '#7cc8e0', '#6a3c28'),
        @('#c8873c', '#7cc8e0', '#6a3c28', '#3c7c4a'),
        @('#7cc8e0', '#6a3c28', '#3c7c4a', '#c8873c')
    )
    for ($row = 0; $row -lt 4; $row++) {
        $shelfY = $wy + 6 + $row * 5
        if (-not $glowOnly) {
            HLine $bmp $wx $shelfY 14 (C '#4a505c')
            HLine $bmp $wx ($shelfY + 1) 14 (C '#262b33')
        }
        for ($k = 0; $k -lt 4; $k++) {
            $bx = $wx + 1 + $k * 3; $by = $shelfY - 4
            $col = C $rows[$row][$k]
            Fill-Rect $bmp $bx ($by + 1) 2 3 $col
            HLine $bmp $bx $by 2 (C '#d8d8e2')
            Put-Px $bmp $bx ($by + 2) (Shift-Col $col 40)
        }
    }
}
function Build-DrinkMachine {
    $body = @(
        (Cu @(-6.5, 2, -4.5) @(13, 28, 9) (@{ north = 'vface'; east = 'vside'; west = 'vside'; south = 'vside'; up = 'mtld'; down = 'mtld' })),
        (Cu @(-6.8, 0, -4.8) @(13.6, 2, 9.6) (@{ all = 'mtld' })),
        (Cu @(-6.7, 30, -4.7) @(13.4, 0.8, 9.4) (@{ all = 'mtld' })),
        (Cu @(-5.4, 24.6, -4.72) @(10.8, 0.5, 0.4) (@{ all = 'chr' })),
        (Cu @(-5.4, 5.9, -4.72) @(10.8, 0.5, 0.4) (@{ all = 'chr' })),
        (Cu @(-5.4, 6.4, -4.72) @(0.5, 18.2, 0.4) (@{ all = 'chr' })),
        (Cu @(4.9, 6.4, -4.72) @(0.5, 18.2, 0.4) (@{ all = 'chr' })),
        (Cu @(3.2, 10, -5.05) @(0.7, 7, 0.4) (@{ all = 'chr' })),
        (Cu @(2.9, 12, -4.85) @(1.3, 0.5, 0.25) (@{ all = 'chr' })),
        (Cu @(2.9, 15.5, -4.85) @(1.3, 0.5, 0.25) (@{ all = 'chr' }))
    )
    $paint = {
        param($bmp, $R)
        Paint-DrinkFace $bmp $R.vface
        Paint-MachineSide $bmp $R.vside '#33363c' 833
    }
    $glow = { param($g, $R) Paint-DrinkFace $g $R.vface $true }
    Build-Prop 'drink_machine' 2 @((Bone 'body' @(0, 0, 0) $body)) $paint $glow 'tall' (Recipe-Pattern @('IGI', 'GDG', 'IRI') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glass'; D = 'minecraft:snowball'; R = 'minecraft:redstone' }) 'drink_machine')
}
function Paint-ChangeFace($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#2b3040' 4 807
        HLine $bmp $r.x $r.y $r.w (C '#3a4054')
        HLine $bmp $r.x ($r.y + 2) $r.w (C '#1c2030')
        HLine $bmp $r.x ($r.y + $r.h - 2) $r.w (C '#1c2030')
        HLine $bmp ($r.x + 9) ($r.y + 8) 5 (C '#c8ccd4')
        HLine $bmp ($r.x + 9) ($r.y + 10) 4 (C '#9aa0ac')
        HLine $bmp ($r.x + 9) ($r.y + 12) 6 (C '#c8ccd4')
        VLine $bmp ($r.x + 2) ($r.y + 5) 12 (C '#20242f')
        VLine $bmp ($r.x + 13) ($r.y + 5) 12 (C '#20242f')
    }
    for ($y = 0; $y -lt 22; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $d = [Math]::Sqrt([Math]::Pow($x - 5.5, 2) + [Math]::Pow($y - 9.0, 2))
            if ($d -lt 2.6) { Put-Px $bmp ($r.x + $x) ($r.y + $y) (C '#c8a028') }
            elseif ($d -lt 3.5) { Put-Px $bmp ($r.x + $x) ($r.y + $y) (C '#8a6a12') }
        }
    }
    Fill-Rect $bmp ($r.x + 5) ($r.y + 7) 2 5 (C '#ffe8a0')
    VLine $bmp ($r.x + 5) ($r.y + 13) 4 (C '#8ad0ff')
    Put-Px $bmp ($r.x + 4) ($r.y + 16) (C '#8ad0ff')
    Put-Px $bmp ($r.x + 6) ($r.y + 16) (C '#8ad0ff')
    Put-Px $bmp ($r.x + 5) ($r.y + 17) (C '#8ad0ff')
}
function Build-ChangeMachine {
    $body = @(
        (Cu @(-6.5, 1.2, -4.5) @(13, 20, 9) (@{ north = 'mface2'; east = 'vside'; west = 'vside'; south = 'vside'; up = 'mtld'; down = 'mtld' })),
        (Cu @(-6.8, 0, -4.8) @(13.6, 1.2, 9.6) (@{ all = 'mtld' })),
        (Cu @(-6.7, 21.2, -4.7) @(13.4, 0.8, 9.4) (@{ all = 'mtld' })),
        (Cu @(-3.4, 14.5, -4.75) @(6.8, 4.5, 0.5) (@{ all = 'mtld'; north = 'vpan' })),
        (Cu @(-2.6, 3.2, -4.75) @(5.2, 2.2, 0.6) (@{ all = 'chr'; north = 'misc1' })),
        (Cu @(-3.1, 5.4, -4.85) @(6.2, 0.5, 0.8) (@{ all = 'chr' })),
        (Cu @(-7.05, 8, -3.5) @(0.4, 3, 0.8) (@{ all = 'chr' })),
        (Cu @(6.65, 8, -3.5) @(0.4, 3, 0.8) (@{ all = 'chr' }))
    )
    $paint = {
        param($bmp, $R)
        Paint-ChangeFace $bmp $R.mface2
        Paint-PanelBill $bmp $R.vpan
        Paint-CoinCup $bmp $R.misc1
        Paint-MachineSide $bmp $R.vside '#262a36' 834
    }
    $glow = {
        param($g, $R)
        Paint-ChangeFace $g $R.mface2 $true
        Paint-PanelBill $g $R.vpan $true
        Paint-CoinCup $g $R.misc1 $true
    }
    Build-Prop 'change_machine' 1 @((Bone 'body' @(0, 0, 0) $body)) $paint $glow 'mid' (Recipe-Pattern @('IGI', 'GDG', 'IRI') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glass'; D = 'minecraft:gold_ingot'; R = 'minecraft:redstone' }) 'change_machine')
}
function Paint-CoinCup($bmp, $r, [bool]$glowOnly = $false) {
    if ($glowOnly) {
        Fill-Rect $bmp ($r.x + 6) ($r.y + 5) 4 2 (C '#ffd23c')
        Put-Px $bmp ($r.x + 6) ($r.y + 5) (C '#fff2c0')
        return
    }
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#16181c' 2 862
    HLine $bmp $r.x $r.y $r.w (C '#26282e')
    Fill-Rect $bmp ($r.x + 4) ($r.y + 3) 8 6 (C '#8a8e98')
    Fill-Rect $bmp ($r.x + 5) ($r.y + 2) 6 1 (C '#9aa0ac')
    Fill-Rect $bmp ($r.x + 3) ($r.y + 8) 10 1 (C '#b8bcc6')
    Fill-Rect $bmp ($r.x + 6) ($r.y + 5) 4 2 (C '#ffd23c')
    Put-Px $bmp ($r.x + 6) ($r.y + 5) (C '#fff2c0')
}
function Paint-CoinReturnFace($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#34383e' 4 808
        HLine $bmp $r.x $r.y $r.w (C '#44484e')
        HLine $bmp $r.x ($r.y + 14) $r.w (C '#20242a')
        VLine $bmp ($r.x + 3) ($r.y + 6) 5 (C '#181c22')
        VLine $bmp ($r.x + 12) ($r.y + 6) 5 (C '#181c22')
    }
    Fill-Rect $bmp ($r.x + 2) ($r.y + 3) 12 2 (C '#0c0e12')
    for ($i = 0; $i -lt 5; $i++) { Put-Px $bmp ($r.x + 3 + $i * 2) ($r.y + 3) (C '#3ce65a') }
    Put-Px $bmp ($r.x + 3) ($r.y + 4) (C '#3ce65a')
    Put-Px $bmp ($r.x + 11) ($r.y + 4) (C '#3ce65a')
}
function Paint-Funnel($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#b8bcc6' 3 809
        HLine $bmp $r.x $r.y $r.w (C '#d4d8e0')
        Fill-Rect $bmp ($r.x + 3) ($r.y + 3) 10 10 (C '#26242a')
        Fill-Rect $bmp ($r.x + 5) ($r.y + 5) 6 6 (C '#141216')
        Put-Px $bmp ($r.x + 6) ($r.y + 6) (C '#ffd23c')
        Put-Px $bmp ($r.x + 9) ($r.y + 8) (C '#ffd23c')
        Put-Px $bmp ($r.x + 7) ($r.y + 10) (C '#c8a028')
    }
    Put-Px $bmp ($r.x + 6) ($r.y + 6) (C '#ffd23c')
    Put-Px $bmp ($r.x + 9) ($r.y + 8) (C '#ffd23c')
}
function Build-CoinReturn {
    $body = @(
        (Cu @(-5.5, 1, -4) @(11, 14, 8) (@{ north = 'mface3'; east = 'vside'; west = 'vside'; south = 'vside'; up = 'mtld'; down = 'mtld' })),
        (Cu @(-5.8, 0, -4.3) @(11.6, 1, 8.6) (@{ all = 'mtld' })),
        (Cu @(-3.8, 15, -2.8) @(7.6, 1.4, 5.6) (@{ all = 'chr'; up = 'futrim' })),
        (Cu @(-2.4, 4, -4.35) @(4.8, 2.6, 0.7) (@{ all = 'chr'; north = 'misc2' })),
        (Cu @(-2.9, 6.6, -4.45) @(5.8, 0.5, 0.9) (@{ all = 'chr' }))
    )
    $paint = {
        param($bmp, $R)
        Paint-CoinReturnFace $bmp $R.mface3
        Paint-Funnel $bmp $R.futrim
        Paint-CoinCup $bmp $R.misc2
        Paint-MachineSide $bmp $R.vside '#2e3238' 835
    }
    $glow = {
        param($g, $R)
        Paint-CoinReturnFace $g $R.mface3 $true
        Paint-Funnel $g $R.futrim $true
        Paint-CoinCup $g $R.misc2 $true
    }
    Build-Prop 'coin_return_machine' 1 @((Bone 'body' @(0, 0, 0) $body)) $paint $glow 'mid' (Recipe-Pattern @('IGI', 'GDG', 'IRI') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glass'; D = 'minecraft:copper_ingot'; R = 'minecraft:redstone' }) 'coin_return_machine')
}

# ===========================================================================
# counters
# ===========================================================================
function Paint-Tube($bmp, $R, [string]$hex) {
    Fill-Rect $bmp $R.stube.x $R.stube.y $R.stube.w $R.stube.h (C $hex)
    HLine $bmp ($R.stube.x + 1) ($R.stube.y + 2) ($R.stube.w - 2) (Shift-Col (C $hex) 60)
}
function Paint-TicketBody($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#1b1b22' 3 810
        HLine $bmp $r.x $r.y $r.w (C '#2a2a34')
        HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#0e0e12')
        Fill-Rect $bmp ($r.x + 3) ($r.y + 4) 10 8 (C '#121218')
        Fill-Rect $bmp ($r.x + 27) ($r.y + 4) 10 8 (C '#121218')
        VLine $bmp ($r.x + 3) ($r.y + 4) 8 (C '#2c2c36')
        VLine $bmp ($r.x + 27) ($r.y + 4) 8 (C '#2c2c36')
        HLine $bmp ($r.x + 1) ($r.y + 1) ($r.w - 2) (C '#2e2e38')
        Fill-Rect $bmp ($r.x + 14) ($r.y + 3) 12 10 (C '#0e0e14')
        HLine $bmp ($r.x + 14) ($r.y + 3) 12 (C '#2c2c36')
        HLine $bmp ($r.x + 14) ($r.y + 12) 12 (C '#2c2c36')
    }
    $tw = TxtW 'TIX'
    Txt $bmp ($r.x + 14 + [int]((12 - $tw) / 2) + 1) ($r.y + 6) 'TIX' (C '#060608')
    Txt $bmp ($r.x + 14 + [int]((12 - $tw) / 2)) ($r.y + 5) 'TIX' (C '#ff3cc8')
    Fill-Rect $bmp ($r.x + 17) ($r.y + 10) 6 3 (C '#e6e6ea')
    Put-Px $bmp ($r.x + 19) ($r.y + 11) (C '#ff3cc8')
    Put-Px $bmp ($r.x + 21) ($r.y + 11) (C '#ff3cc8')
    Paint-Tube $bmp $R '#ff3cc8'
}
function Paint-TicketBoard($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#1a1c22' 3 811
        HLine $bmp $r.x $r.y $r.w (C '#33363c')
        HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#0e1014')
    }
    HLine $bmp ($r.x + 1) ($r.y + 1) ($r.w - 2) (C '#8ad0ff')
    HLine $bmp ($r.x + 1) ($r.y + 3) ($r.w - 5) (C '#c8ccd4')
    HLine $bmp ($r.x + 1) ($r.y + 4) ($r.w - 7) (C '#9aa0ac')
}
function Paint-Printer($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#2a2c32' 3 812
        HLine $bmp $r.x $r.y $r.w (C '#3a3c42')
        HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#16181c')
        Fill-Rect $bmp ($r.x + 3) ($r.y + 2) 10 1 (C '#0e1014')
        Fill-Rect $bmp ($r.x + 6) ($r.y + 7) 4 3 (C '#14161a')
    }
    Fill-Rect $bmp ($r.x + 12) ($r.y + 2) 2 1 (C '#3ce65a')
    Put-Px $bmp ($r.x + 2) ($r.y + 2) (C '#3ce65a')
}
function Paint-Paper($bmp, $r, $glowOnly = $false) {
    if ($glowOnly) { return }
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#e8e6de' 3 813
    HLine $bmp $r.x ($r.y + 3) $r.w (C '#ff3cc8')
    HLine $bmp $r.x ($r.y + 7) $r.w (C '#b0aea6')
    HLine $bmp $r.x ($r.y + 9) $r.w (C '#ff3cc8')
}
function Build-TicketCounter {
    $body = @(
        (Cu @(-15, 0, -5.5) @(30, 13.6, 11) (@{ north = 'dirf'; east = 'drk'; west = 'drk'; south = 'drk'; up = 'drk'; down = 'drk' })),
        (Cu @(-15, 0, -5.9) @(30, 1.2, 11.8) (@{ all = 'mtld' })),
        (Cu @(-15.4, 13.6, -6) @(30.8, 1.5, 12) (@{ all = 'drk' })),
        (Cu @(-15.4, 12.7, -6.05) @(30.8, 0.6, 0.25) (@{ all = 'stube' })),
        (Cu @(-15, 0.9, -5.55) @(30, 1.0, 0.25) (@{ all = 'stube' })),
        (Cu @(-5, 15.1, -3.2) @(10, 4.6, 0.5) (@{ all = 'mtld'; north = 'board2' })),
        (Cu @(3.2, 15.1, -3.4) @(4.2, 3, 3) (@{ all = 'mtld'; north = 'misc1' })),
        (Cu @(4.6, 18.1, -2.6) @(1.4, 0.5, 1.8) (@{ all = 'misc2' })),
        (Cu @(0.2, 15.1, -2.9) @(2.6, 2.6, 2.6) (@{ all = 'misc2' }))
    )
    $paint = {
        param($bmp, $R)
        Paint-TicketBody $bmp $R.dirf
        Paint-TicketBoard $bmp $R.board2
        Paint-Printer $bmp $R.misc1
        Paint-Paper $bmp $R.misc2
    }
    $glow = {
        param($g, $R)
        Paint-TicketBody $g $R.dirf $true
        Paint-TicketBoard $g $R.board2 $true
        Paint-Printer $g $R.misc1 $true
    }
    Build-Prop 'ticket_counter' 1 @((Bone 'body' @(0, 0, 0) $body)) $paint $glow 'mid' (Recipe-Pattern @('PPP', 'IDI', 'I I') ([ordered]@{ P = 'minecraft:paper'; I = 'minecraft:iron_ingot'; D = 'minecraft:magenta_dye' }) 'ticket_counter') 2
}
function Paint-PrizeBody($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#1e1a24' 3 814
        HLine $bmp $r.x $r.y $r.w (C '#2c2836')
        HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#100e14')
        Fill-Rect $bmp ($r.x + 3) ($r.y + 4) 10 8 (C '#15121c')
        Fill-Rect $bmp ($r.x + 27) ($r.y + 4) 10 8 (C '#15121c')
        HLine $bmp ($r.x + 1) ($r.y + 1) ($r.w - 2) (C '#322c40')
        $rnd = New-Object System.Random(814)
        for ($i = 0; $i -lt 16; $i++) {
            $col = @('#ff8ad0', '#3cf0ff', '#ffe23c', '#b84aff')[$rnd.Next(0, 4)]
            Put-Px $bmp ($r.x + 15 + $rnd.Next(0, 10)) ($r.y + 4 + $rnd.Next(0, 8)) (C $col)
        }
        Fill-Rect $bmp ($r.x + 14) ($r.y + 3) 12 10 (C '#0e0e14')
        HLine $bmp ($r.x + 14) ($r.y + 3) 12 (C '#2c2c36')
        HLine $bmp ($r.x + 14) ($r.y + 12) 12 (C '#2c2c36')
    }
    Paint-Tube $bmp $R '#3cf0ff'
    $tw = TxtW 'WIN'
    Txt $bmp ($r.x + 14 + [int]((12 - $tw) / 2)) ($r.y + 4) 'WIN' (C '#3cf0ff')
    Put-Px $bmp ($r.x + 23) ($r.y + 10) (C '#ffe23c')
    Put-Px $bmp ($r.x + 22) ($r.y + 10) (C '#ffe23c')
    Put-Px $bmp ($r.x + 24) ($r.y + 10) (C '#ffe23c')
    Put-Px $bmp ($r.x + 23) ($r.y + 9) (C '#ffe23c')
    Put-Px $bmp ($r.x + 23) ($r.y + 11) (C '#ffe23c')
    Put-Px $bmp ($r.x + 23) ($r.y + 10) (C '#ffffff')
}
function Paint-WinBand($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) {
        Noise-Rect $bmp $r.x $r.y $r.w $r.h '#1a1622' 3 815
        HLine $bmp $r.x $r.y $r.w (C '#3c3450')
        HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#0e0c12')
    }
    $tw = TxtW 'WIN'
    $tx = $r.x + [int](($r.w - $tw) / 2)
    Txt $bmp ($tx + 1) ($r.y + 1) 'WIN' (C '#060608')
    Txt $bmp $tx ($r.y + 1) 'WIN' (C '#3cf0ff')
    Put-Px $bmp ($r.x + 1) ($r.y + 2) (C '#ffe23c')
    Put-Px $bmp ($r.x + $r.w - 2) ($r.y + 3) (C '#ff8ad0')
}
function Paint-Plush($bmp, $r, [string]$hex, [int]$seed, $glowOnly = $false) {
    if ($glowOnly) { return }
    $base = C $hex
    Noise-Rect $bmp $r.x $r.y $r.w $r.h (Color-Hex (Shift-Col $base -80)) 3 $seed
    Fill-Rect $bmp ($r.x + 3) ($r.y + 4) 10 7 $base
    Fill-Rect $bmp ($r.x + 4) ($r.y + 3) 8 1 (Shift-Col $base 20)
    Fill-Rect $bmp ($r.x + 2) ($r.y + 5) 12 4 $base
    Fill-Rect $bmp ($r.x + 3) ($r.y + 1) 2 2 (Shift-Col $base -30)
    Fill-Rect $bmp ($r.x + 11) ($r.y + 1) 2 2 (Shift-Col $base -30)
    Fill-Rect $bmp ($r.x + 6) ($r.y + 7) 4 4 (Shift-Col $base 70)
    Put-Px $bmp ($r.x + 6) ($r.y + 6) (C '#141418')
    Put-Px $bmp ($r.x + 9) ($r.y + 6) (C '#141418')
    Put-Px $bmp ($r.x + 8) ($r.y + 8) (C '#141418')
    Put-Px $bmp ($r.x + 4) ($r.y + 9) (C '#ffffff')
    Put-Px $bmp ($r.x + 11) ($r.y + 9) (C '#ffffff')
}
function Paint-Trophy($bmp, $r, [bool]$glowOnly = $false) {
    if (-not $glowOnly) { Noise-Rect $bmp $r.x $r.y $r.w $r.h '#1a1622' 3 816 }
    $gold = C '#ffd23c'
    Fill-Rect $bmp ($r.x + 6) ($r.y + 3) 4 3 $gold
    Fill-Rect $bmp ($r.x + 7) ($r.y + 6) 2 3 (Shift-Col $gold -30)
    Fill-Rect $bmp ($r.x + 5) ($r.y + 9) 6 1 $gold
    Put-Px $bmp ($r.x + 4) ($r.y + 4) $gold
    Put-Px $bmp ($r.x + 11) ($r.y + 4) $gold
    Put-Px $bmp ($r.x + 6) ($r.y + 4) (C '#fff2c0')
}
function Paint-Toybox($bmp, $r, $glowOnly = $false) {
    if ($glowOnly) { return }
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#2c58c8' 4 817
    HLine $bmp $r.x $r.y $r.w (C '#4470e0')
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#1c3c96')
    Fill-Rect $bmp ($r.x + 3) ($r.y + 3) 10 6 (C '#d8e0f0')
    Fill-Rect $bmp ($r.x + 4) ($r.y + 4) 8 4 (C '#a8b8e8')
    Fill-Rect $bmp ($r.x + 7) ($r.y + 5) 2 2 (C '#ffe23c')
}
function Paint-CaseGlass($bmp, $r) {
    Fill-Rect $bmp $r.x $r.y $r.w $r.h (C '#3a4652')
    HLine $bmp $r.x $r.y $r.w (C '#5c6e80')
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#26303a')
    for ($i = 2; $i -lt $r.w; $i++) {
        $y = $r.y + (($i * 2 + 3) % $r.h)
        Put-Px $bmp ($r.x + $i) $y (C '#4c5f72')
        if ($i % 4 -eq 0) { Put-Px $bmp ($r.x + $i) $y (C '#9fb6cc') }
    }
}
function Build-PrizeCounter {
    $body = @(
        (Cu @(-15, 0, -5.5) @(30, 13.6, 11) (@{ north = 'dirf'; east = 'drk'; west = 'drk'; south = 'drk'; up = 'drk'; down = 'drk' })),
        (Cu @(-15, 0, -5.9) @(30, 1.2, 11.8) (@{ all = 'mtld' })),
        (Cu @(-15.4, 13.6, -6) @(30.8, 1.5, 12) (@{ all = 'drk' })),
        (Cu @(-15.4, 12.7, -6.05) @(30.8, 0.6, 0.25) (@{ all = 'stube' })),
        (Cu @(-15, 0.9, -5.55) @(30, 1.0, 0.25) (@{ all = 'stube' })),
        (Cu @(-13.4, 15.1, -5.4) @(26.8, 0.9, 10.8) (@{ all = 'drk' })),
        (Cu @(-13.4, 16, 4.6) @(26.8, 10.4, 0.8) (@{ all = 'drk' })),
        (Cu @(-13.4, 26.4, -5.4) @(26.8, 0.9, 10.8) (@{ all = 'drk' })),
        (Cu @(-13.4, 16, -5.4) @(0.9, 10.4, 10.8) (@{ all = 'drk' })),
        (Cu @(12.5, 16, -5.4) @(0.9, 10.4, 10.8) (@{ all = 'drk' })),
        (Cu @(-12.5, 16, -5.7) @(25, 10.4, 0.45) (@{ all = 'sfw' })),
        (Cu @(-12.5, 19.4, -4.6) @(25, 0.3, 9) (@{ all = 'sfw' })),
        (Cu @(-12.5, 22.8, -4.6) @(25, 0.3, 9) (@{ all = 'sfw' })),
        (Cu @(-7, 25.4, -5.85) @(14, 4.4, 0.5) (@{ all = 'drk'; north = 'band2' })),
        (Cu @(-9.5, 16, -1.8) @(3.2, 4, 3.2) (@{ all = 'misc3' })),
        (Cu @(1.2, 16, -1.8) @(3.2, 4, 3.2) (@{ all = 'misc4' })),
        (Cu @(7.6, 16, -1.8) @(3.2, 4, 3.2) (@{ all = 'misc5' })),
        (Cu @(-8, 19.7, -1.5) @(2.4, 2.8, 2.4) (@{ all = 'misc6' })),
        (Cu @(0.4, 19.7, -1.6) @(2.4, 2.8, 2.4) (@{ all = 'misc6' })),
        (Cu @(7, 19.7, -1.6) @(3, 2.6, 2.6) (@{ all = 'misc1' })),
        (Cu @(-6, 23.2, -1.4) @(3, 3, 3) (@{ all = 'misc4' })),
        (Cu @(2.4, 23.2, -1.4) @(3, 3, 3) (@{ all = 'misc3' }))
    )
    $paint = {
        param($bmp, $R)
        Paint-PrizeBody $bmp $R.dirf
        Paint-WinBand $bmp $R.band2
        Paint-CaseGlass $bmp $R.sfw
        Paint-Plush $bmp $R.misc3 '#ff9ad0' 818
        Paint-Plush $bmp $R.misc4 '#7ce0a0' 819
        Paint-Plush $bmp $R.misc5 '#ffe06a' 820
        Paint-Trophy $bmp $R.misc6
        Paint-Toybox $bmp $R.misc1
    }
    $glow = {
        param($g, $R)
        Paint-PrizeBody $g $R.dirf $true
        Paint-WinBand $g $R.band2 $true
        Paint-Trophy $g $R.misc6 $true
    }
    Build-Prop 'prize_counter' 1 @((Bone 'body' @(0, 0, 0) $body)) $paint $glow 'mid' (Recipe-Pattern @('I I', 'GDG', 'I I') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glass'; D = 'minecraft:pink_dye' }) 'prize_counter') 2
}

# ===========================================================================
# run
# ===========================================================================
Write-Output 'Working Consoles v5 arcade pack (stage A + expansion):'
Build-FloorTile 'arcade_carpet' 'confetti' '#ff3cc8' { param($b, $R) Paint-CarpetConfetti $b $R } $null (Recipe-Pattern @('WW', 'WD') ([ordered]@{ W = '#minecraft:wool'; D = 'minecraft:magenta_dye' }) 'arcade_carpet' 3)
Build-FloorTile 'arcade_carpet_zigzag' 'zigzag' '#3cf0ff' { param($b, $R) Paint-CarpetZigzag $b $R } $null (Recipe-Pattern @('WW', 'WD') ([ordered]@{ W = '#minecraft:wool'; D = 'minecraft:cyan_dye' }) 'arcade_carpet_zigzag' 3)
Build-FloorTile 'arcade_carpet_checker' 'checker' '#b84aff' { param($b, $R) Paint-CarpetChecker $b $R } $null (Recipe-Pattern @('WW', 'WD') ([ordered]@{ W = '#minecraft:wool'; D = 'minecraft:purple_dye' }) 'arcade_carpet_checker' 3)
Build-FloorTile 'floor_decal_arrow' 'decalA' '#ffd23c' { param($b, $R) Paint-DecalArrow $b $R } { param($g, $R) Paint-DecalArrow $g $R $true } (Recipe-Pattern @('WW', 'WD') ([ordered]@{ W = '#minecraft:wool'; D = 'minecraft:yellow_dye' }) 'floor_decal_arrow' 3)
Build-FloorTile 'floor_decal_steps' 'decalB' '#3cf0ff' { param($b, $R) Paint-DecalSteps $b $R } { param($g, $R) Paint-DecalSteps $g $R $true } (Recipe-Pattern @('WW', 'WD') ([ordered]@{ W = '#minecraft:wool'; D = 'minecraft:cyan_dye' }) 'floor_decal_steps' 3)
Build-FloorTile 'floor_decal_star' 'decalC' '#ff3cc8' { param($b, $R) Paint-DecalStar $b $R } { param($g, $R) Paint-DecalStar $g $R $true } (Recipe-Pattern @('WW', 'WD') ([ordered]@{ W = '#minecraft:wool'; D = 'minecraft:magenta_dye' }) 'floor_decal_star' 3)
Build-Sign 'neon_sign_open' 'OPEN' '#ffd23c' $false 'sf' 15 7 5 (Recipe-Pattern @('III', 'GDG', 'III') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glowstone'; D = 'minecraft:yellow_dye' }) 'neon_sign_open')
Build-Sign 'neon_sign_play' 'PLAY' '#7cff4a' $false 'sf' 15 7 5 (Recipe-Pattern @('III', 'GDG', 'III') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glowstone'; D = 'minecraft:lime_dye' }) 'neon_sign_play')
Build-Sign 'neon_sign_arcade' 'ARCADE' '#ff3cc8' $true 'sfw' 27 7 5 (Recipe-Pattern @('III', 'GDG', 'III') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glowstone'; D = 'minecraft:magenta_dye' }) 'neon_sign_arcade')
Build-Sign 'neon_sign_tokens' 'TOKENS' '#3cf0ff' $true 'sfw' 27 7 5 (Recipe-Pattern @('III', 'GDG', 'III') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glowstone'; D = 'minecraft:cyan_dye' }) 'neon_sign_tokens')
Build-Sign 'exit_sign' 'EXIT' '#3cff6a' $false 'xf' 15 6 7 (Recipe-Pattern @('III', 'GDG', 'III') ([ordered]@{ I = 'minecraft:iron_ingot'; G = 'minecraft:glowstone'; D = 'minecraft:green_dye' }) 'exit_sign')
Build-Directory
Build-CeilingLight
Build-FloorUplight
Build-ArcadeStool
Build-TrashCan
Build-VendingMachine
Build-SnackMachine
Build-DrinkMachine
Build-ChangeMachine
Build-CoinReturn
Build-TicketCounter
Build-PrizeCounter
Write-Output 'done.'
