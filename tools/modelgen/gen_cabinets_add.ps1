# gen_cabinets_add.ps1 - ADDITIVE detail pass for the arcade cabinets
# (cabinet + big_cabinet).
#
# Same recipe as gen_rear_add.ps1: the ORIGINAL models are kept exactly as
# they ship (same bones, cubes, UVs; the live screen quads and the
# led_on / led_standby state bones are untouched) and a "details" bone is
# APPENDED with: bezel strips, a marquee panel, coin door / coin return,
# instruction card, t-molding edge, front feet, vents, power socket, sticker
# (big cabinet also gets P1/P2 start buttons).
#
# The appended bone is PARENTED to the model's root bone ("console") - the
# same pattern the shipped led_on / led_standby sub-bones use - so the
# insert / eject / shake animations move the whole machine, details included.
#
# The original atlases are just the swatch strip (the models sample raw swatch
# pixels), so every detail region is painted in free space BELOW the strip
# (y >= 4) with the mod's own palette + noise; the pngs are painted from the
# backup copies and the geos are rebuilt from the backup copies, so the
# originals stay untouched no matter how often this runs.
#
#   powershell -File gen_cabinets_add.ps1

. (Join-Path $PSScriptRoot 'gen_lib.ps1')

$arcadeAssets = Join-Path $PSScriptRoot '..\..\arcade\src\main\resources\assets\arcade'
$geoOut = Join-Path $arcadeAssets 'geckolib\models\item'
$texOut = Join-Path $arcadeAssets 'textures\item'
$bak = Join-Path $PSScriptRoot 'backup'

# keep a pristine copy of the original files (first run only)
foreach ($m in @('cabinet', 'big_cabinet')) {
    $bakGeo = Join-Path $bak "$m.geo.json"
    $bakTex = Join-Path $bak "$m.png"
    if (-not (Test-Path $bakGeo)) { Copy-Item (Join-Path $geoOut "$m.geo.json") $bakGeo }
    if (-not (Test-Path $bakTex)) { Copy-Item (Join-Path $texOut "$m.png") $bakTex }
}

# ---- palette (exact colors from the original swatch strips) ----------------
$K1 = '#101012'   # near-black (screen / slots)
$K2 = '#1e1e22'   # frame dark grey
$K3 = '#28283c'   # cabinet navy
$L2 = '#b4b4be'   # light steel / light grey
$LY = '#ffe678'   # light yellow (marquee glow)
$YELLOW = '#f0c828'
$RED = '#e62828'
$BLUE = '#2878e6'
$GREEN = '#3ce65a'

# ===========================================================================
# detail region painters (all palette colors only)
# ===========================================================================
function Paint-EdgeRegion($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $K1 2 71
    Put-Px $bmp $r.x $r.y (C $K2)
}

function Paint-FrameRegion($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $K2 2 72
    Put-Px $bmp $r.x ($r.y + $r.h - 1) (C $K1)
}

function Paint-VinylRegion($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $K1 1 73
    HLine $bmp $r.x $r.y $r.w (C $L2)
}

function Paint-VentsRegion($bmp, $r, [int]$seed) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $K2 3 $seed
    $step = 3
    if ($r.h -le 6) { $step = 2 }
    for ($j = 2; $j -lt ($r.h - 1); $j += $step) {
        HLine $bmp ($r.x + 1) ($r.y + $j) ($r.w - 2) (C $K1)
        HLine $bmp ($r.x + 1) ($r.y + $j - 1) ($r.w - 2) (C $K3)
    }
}

function Paint-SockRegion($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $K2 2 41
    Put-Px $bmp $r.x ($r.y + $r.h - 1) (C $K1)
    Put-Px $bmp ($r.x + $r.w - 1) $r.y (C $K1)
}

function Paint-StickerRegion($bmp, $r) {
    Fill-Rect $bmp $r.x $r.y $r.w $r.h (C $L2)
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C $K2)
}

function Paint-MarqueRegion($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $LY 4 81
    HLine $bmp $r.x $r.y $r.w (C $K1)
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C $K1)
    $cols = @($RED, $BLUE, $GREEN)
    $k = 0
    for ($i = 1; $i -le ($r.w - 3); $i += 4) {
        Fill-Rect $bmp ($r.x + $i) ($r.y + 1) 2 ($r.h - 2) (C $cols[$k % 3])
        $k++
    }
}

function Paint-DoorRegion($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $K2 3 82
    HLine $bmp $r.x $r.y $r.w (C $K1)
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C $K1)
    VLine $bmp $r.x $r.y $r.h (C $K1)
    VLine $bmp ($r.x + $r.w - 1) $r.y $r.h (C $K1)
    Fill-Rect $bmp ($r.x + 1) ($r.y + 2) 2 4 (C $L2)     # coin entry plates
    VLine $bmp ($r.x + 2) ($r.y + 3) 2 (C $K1)           # slot
    Fill-Rect $bmp ($r.x + 5) ($r.y + 2) 2 4 (C $L2)
    VLine $bmp ($r.x + 6) ($r.y + 3) 2 (C $K1)
    Fill-Rect $bmp ($r.x + 3) ($r.y + 6) 2 1 (C $K1)     # coin return opening
    Put-Px $bmp ($r.x + 4) ($r.y + 1) (C $L2)
}

function Paint-CardRegion($bmp, $r) {
    Fill-Rect $bmp $r.x $r.y $r.w $r.h (C $L2)
    HLine $bmp $r.x $r.y $r.w (C $K1)
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C $K1)
    HLine $bmp ($r.x + 1) ($r.y + 1) ($r.w - 2) (C $K2)
    HLine $bmp ($r.x + 1) ($r.y + 2) ($r.w - 3) (C $K2)
}

function Paint-StartBtnRegion($bmp, $r, $accent) {
    Fill-Rect $bmp $r.x $r.y $r.w $r.h (C $accent)
    Put-Px $bmp $r.x $r.y (C $L2)
    Put-Px $bmp ($r.x + $r.w - 1) ($r.y + $r.h - 1) (C $K1)
}

# ===========================================================================
# geo helpers - append a "details" bone, original bones untouched
# ===========================================================================
function New-DetailCube($origin, $size, $faces, $regions) {
    $uv = [ordered]@{}
    foreach ($f in @('north', 'east', 'south', 'west', 'up', 'down')) {
        $rn = $faces['all']
        if ($faces.ContainsKey($f)) { $rn = $faces[$f] }
        $rg = $regions[$rn]
        $uv[$f] = [ordered]@{ uv = @([int]$rg.x, [int]$rg.y); uv_size = @([int]$rg.w, [int]$rg.h) }
    }
    return [PSCustomObject]@{ origin = @($origin); size = @($size); uv = $uv }
}

function Add-DetailBone([string]$model, $cubes) {
    $srcGeo = Join-Path $bak "$model.geo.json"
    $json = Get-Content -Raw -LiteralPath $srcGeo | ConvertFrom-Json
    $geom = $json.'minecraft:geometry'[0]
    $origBones = @($geom.bones)
    $rootName = $origBones[0].name
    $sig = ($origBones | ForEach-Object { "$($_.name):$(@($_.cubes).Count)" }) -join '+'
    # parent the appended bone to the model's root bone (like led_on/led_standby)
    $bone = [PSCustomObject]@{ name = 'details'; parent = $rootName; pivot = @(0, 0, 0); cubes = @($cubes) }
    $geom.bones = @($geom.bones) + $bone
    $text = $json | ConvertTo-Json -Depth 64
    [System.IO.File]::WriteAllText((Join-Path $geoOut "$model.geo.json"), $text, (New-Object System.Text.UTF8Encoding($false)))
    # validate: reparse, every original bone + cube count intact, details appended + parented
    $check = Get-Content -Raw -LiteralPath (Join-Path $geoOut "$model.geo.json") | ConvertFrom-Json
    $bones = @($check.'minecraft:geometry'[0].bones)
    $newSig = ($bones | Select-Object -First $origBones.Count | ForEach-Object { "$($_.name):$(@($_.cubes).Count)" }) -join '+'
    $ok = ($bones.Count -eq ($origBones.Count + 1)) -and ($sig -eq $newSig) -and ($bones[-1].name -eq 'details') -and ($bones[-1].parent -eq $rootName)
    $status = if ($ok) { 'OK' } else { 'FAILED' }
    Write-Output ("{0}: original bones [{1}] + details ({2} cubes, parent {3})  [{4}]" -f $model, $sig, @($cubes).Count, $rootName, $status)
}

# region table (same free-space layout in both atlases, all y >= 4)
function New-CabRegionTable {
    return [ordered]@{
        edge    = @{ x = 0;  y = 4;  w = 2;  h = 2 }
        frame   = @{ x = 4;  y = 4;  w = 2;  h = 2 }
        vinyl   = @{ x = 8;  y = 4;  w = 2;  h = 2 }
        sock    = @{ x = 12; y = 4;  w = 2;  h = 2 }
        sticker = @{ x = 16; y = 4;  w = 2;  h = 2 }
        btn1    = @{ x = 20; y = 4;  w = 2;  h = 2 }
        btn2    = @{ x = 24; y = 4;  w = 2;  h = 2 }
        marquee = @{ x = 28; y = 4;  w = 12; h = 4 }
        door    = @{ x = 42; y = 4;  w = 8;  h = 8 }
        card    = @{ x = 52; y = 4;  w = 6;  h = 4 }
        vents   = @{ x = 0;  y = 8;  w = 14; h = 4 }
    }
}

function Paint-Shared($bmp, $R, [int]$seed) {
    Paint-EdgeRegion   $bmp $R.edge
    Paint-FrameRegion  $bmp $R.frame
    Paint-VinylRegion  $bmp $R.vinyl
    Paint-VentsRegion  $bmp $R.vents $seed
    Paint-SockRegion   $bmp $R.sock
    Paint-StickerRegion $bmp $R.sticker
    Paint-MarqueRegion $bmp $R.marquee
    Paint-DoorRegion   $bmp $R.door
    Paint-CardRegion   $bmp $R.card
    Paint-StartBtnRegion $bmp $R.btn1 $RED
    Paint-StartBtnRegion $bmp $R.btn2 $BLUE
}

# ===========================================================================
# cabinet (adds: bezel, marquee panel, coin door, instruction card,
#                t-molding, feet, vents, socket, sticker)
# ===========================================================================
function Build-CabinetAdd {
    $R = New-CabRegionTable
    $bmp = New-Object System.Drawing.Bitmap((Join-Path $bak 'cabinet.png'))
    Paint-Shared $bmp $R 64
    $bmp.Save((Join-Path $texOut 'cabinet.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    $cubes = @(
        (New-DetailCube @(-4.8, 14.55, -3.53) @(9.6, 0.35, 0.04) (@{ all = 'edge' }) $R),                     # bezel top
        (New-DetailCube @(-4.8, 7.25, -3.53) @(9.6, 0.2, 0.04) (@{ all = 'edge' }) $R),                       # bezel bottom
        (New-DetailCube @(-4.8, 7.5, -3.53) @(0.25, 7.0, 0.04) (@{ all = 'edge' }) $R),                       # bezel left
        (New-DetailCube @(4.55, 7.5, -3.53) @(0.25, 7.0, 0.04) (@{ all = 'edge' }) $R),                       # bezel right
        (New-DetailCube @(-5.4, 15.45, -4.55) @(10.8, 1.3, 0.06) (@{ all = 'frame'; north = 'marquee' }) $R), # marquee panel
        (New-DetailCube @(-1.8, 1.4, -5.06) @(3.6, 3.2, 0.07) (@{ all = 'edge'; north = 'door' }) $R),        # coin door
        (New-DetailCube @(-1.2, 5.1, -5.04) @(2.4, 0.6, 0.05) (@{ all = 'edge'; north = 'card' }) $R),        # instruction card
        (New-DetailCube @(-5.2, 0, -5.35) @(1.6, 0.7, 0.45) (@{ all = 'edge' }) $R),                          # foot L
        (New-DetailCube @(3.6, 0, -5.35) @(1.6, 0.7, 0.45) (@{ all = 'edge' }) $R),                           # foot R
        (New-DetailCube @(-5.9, 7.28, -6.54) @(11.8, 0.14, 0.14) (@{ all = 'vinyl' }) $R),                    # control panel t-molding
        (New-DetailCube @(-3, 16.98, -4.2) @(6, 0.08, 1.4) (@{ all = 'frame'; up = 'vents' }) $R),            # top vents
        (New-DetailCube @(-4, 8, 5.99) @(8, 5, 0.06) (@{ all = 'frame'; south = 'vents' }) $R),                # rear vents
        (New-DetailCube @(2.2, 1.6, 5.98) @(1.6, 1.6, 0.08) (@{ all = 'frame'; south = 'sock' }) $R),         # power socket
        (New-DetailCube @(-3.2, 1.8, 5.99) @(1.4, 1.0, 0.05) (@{ all = 'frame'; south = 'sticker' }) $R)      # sticker
    )
    Add-DetailBone 'cabinet' $cubes
}

# ===========================================================================
# big_cabinet (adds: bezel, marquee panel, t-molding, P1/P2 start buttons,
#                   coin return, keyhole, feet, vents, socket, sticker)
# ===========================================================================
function Build-BigCabinetAdd {
    $R = New-CabRegionTable
    $bmp = New-Object System.Drawing.Bitmap((Join-Path $bak 'big_cabinet.png'))
    Paint-Shared $bmp $R 65
    $bmp.Save((Join-Path $texOut 'big_cabinet.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    $cubes = @(
        (New-DetailCube @(-6.3, 25.55, -3.13) @(12.6, 0.35, 0.04) (@{ all = 'edge' }) $R),                     # bezel top
        (New-DetailCube @(-6.3, 16.1, -3.13) @(12.6, 0.35, 0.04) (@{ all = 'edge' }) $R),                      # bezel bottom
        (New-DetailCube @(-6.35, 16.5, -3.13) @(0.3, 9.0, 0.04) (@{ all = 'edge' }) $R),                       # bezel left
        (New-DetailCube @(6.05, 16.5, -3.13) @(0.3, 9.0, 0.04) (@{ all = 'edge' }) $R),                        # bezel right
        (New-DetailCube @(-6.2, 28.15, -5.05) @(12.4, 2.2, 0.06) (@{ all = 'frame'; north = 'marquee' }) $R),  # marquee panel
        (New-DetailCube @(-6.9, 14.32, -8.09) @(13.8, 0.16, 0.1) (@{ all = 'vinyl' }) $R),                     # control panel t-molding
        (New-DetailCube @(-1.3, 13.55, -8.1) @(0.6, 0.6, 0.06) (@{ all = 'frame'; north = 'btn1' }) $R),       # start P1
        (New-DetailCube @(0.7, 13.55, -8.1) @(0.6, 0.6, 0.06) (@{ all = 'frame'; north = 'btn2' }) $R),        # start P2
        (New-DetailCube @(-0.6, 3.4, -5.12) @(1.2, 0.6, 0.08) (@{ all = 'edge'; north = 'door' }) $R),         # coin return
        (New-DetailCube @(-0.15, 5.0, -5.11) @(0.3, 0.4, 0.07) (@{ all = 'edge' }) $R),                        # keyhole
        (New-DetailCube @(-6.0, 0, -5.4) @(1.6, 0.8, 0.5) (@{ all = 'edge' }) $R),                             # foot L
        (New-DetailCube @(4.4, 0, -5.4) @(1.6, 0.8, 0.5) (@{ all = 'edge' }) $R),                              # foot R
        (New-DetailCube @(-4, 30.97, -4.2) @(8, 0.08, 1.4) (@{ all = 'frame'; up = 'vents' }) $R),             # top vents
        (New-DetailCube @(-5, 18, 6.99) @(10, 6, 0.06) (@{ all = 'frame'; south = 'vents' }) $R),              # rear vents
        (New-DetailCube @(4.0, 2.0, 6.98) @(1.6, 1.6, 0.08) (@{ all = 'frame'; south = 'sock' }) $R),          # power socket
        (New-DetailCube @(-5.0, 2.2, 6.99) @(1.4, 1.0, 0.05) (@{ all = 'frame'; south = 'sticker' }) $R)       # sticker
    )
    Add-DetailBone 'big_cabinet' $cubes
}

Build-CabinetAdd
Build-BigCabinetAdd
