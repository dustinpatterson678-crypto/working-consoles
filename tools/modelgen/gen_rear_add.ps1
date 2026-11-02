# gen_rear_add.ps1 - ADDITIVE detail pass for the rear_projection TV.
#
# Same recipe as gen_crts_add.ps1: the ORIGINAL model is kept exactly as it
# ships (same 'console' bone, cubes, UVs, screen plate - the code's live screen
# quad keeps aligning) and a "details" bone is APPENDED with: bezel strips,
# speaker grilles, power/control buttons, front feet, top + rear vents, AV
# ports, power socket, sticker and a brand badge.
#
# The original atlas is just the swatch strip (the model samples raw swatch
# pixels), so every detail region is painted in free space BELOW the strip
# (y >= 4) with the mod's own palette + noise; the png is painted from the
# backup copy and the geo is rebuilt from the backup copy, so the originals
# stay untouched no matter how often this runs.
#
#   powershell -File gen_rear_add.ps1

. (Join-Path $PSScriptRoot 'gen_lib.ps1')

$wcoreAssets = Join-Path $PSScriptRoot '..\..\wcore\src\main\resources\assets\wcore'
$geoOut = Join-Path $wcoreAssets 'geckolib\models\item'
$texOut = Join-Path $wcoreAssets 'textures\item'
$bak = Join-Path $PSScriptRoot 'backup'

# keep a pristine copy of the original files (first run only)
$bakGeo = Join-Path $bak 'rear_projection.geo.json'
$bakTex = Join-Path $bak 'rear_projection.png'
if (-not (Test-Path $bakGeo)) { Copy-Item (Join-Path $geoOut 'rear_projection.geo.json') $bakGeo }
if (-not (Test-Path $bakTex)) { Copy-Item (Join-Path $texOut 'rear_projection.png') $bakTex }

# ---- palette (exact colors from the original swatch strip) ----------------
$K1 = '#0c1012'   # screen near-black
$K2 = '#1e1e21'   # frame dark
$K3 = '#28282a'   # chin dark grey
$L1 = '#3a3a3e'   # body grey
$L2 = '#aaaab0'   # light grey (button bump / handle)
$W = '#e1e1e4'    # white
$BLUE = '#8cbee6'
$BEIGE = '#c4bca8'
$RED = '#dc2828'
$GREEN = '#3ce65a'

# ===========================================================================
# detail region painters (all palette colors only)
# ===========================================================================
function Paint-EdgeRegion($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $K1 2 71
    Put-Px $bmp $r.x $r.y (C $K2)
}

function Paint-GrilleRegion($bmp, $r, $base) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $base 2 21
    for ($j = 0; $j -lt $r.h; $j++) {
        for ($i = 0; $i -lt $r.w; $i++) {
            if ((($i + 1) % 2) -eq 0 -and (($j + 1) % 2) -eq 0) { Put-Px $bmp ($r.x + $i) ($r.y + $j) (C $K1) }
        }
    }
    HLine $bmp $r.x $r.y $r.w (C $K3)
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

function Paint-ButtonRegion($bmp, $r, $accent) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $K3 2 31
    HLine $bmp $r.x $r.y $r.w (C $L1)
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C $K1)
    if ($accent) { Put-Px $bmp ($r.x + [int]($r.w / 2)) ($r.y + [int]($r.h / 2)) (C $accent) }
}

function Paint-PortRegion($bmp, $r, $col) {
    Fill-Rect $bmp $r.x $r.y $r.w $r.h (C $col)
    Put-Px $bmp $r.x ($r.y + $r.h - 1) (C $K1)
}

function Paint-SockRegion($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $K2 2 41
    Put-Px $bmp $r.x ($r.y + $r.h - 1) (C $K1)
    Put-Px $bmp ($r.x + $r.w - 1) $r.y (C $K1)
}

function Paint-StickerRegion($bmp, $r) {
    Fill-Rect $bmp $r.x $r.y $r.w $r.h (C $W)
    Put-Px $bmp $r.x ($r.y + $r.h - 1) (C $L2)
}

# ===========================================================================
# geo helpers - append a "details" bone, original bone untouched
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
    $origName = $geom.bones[0].name
    $origCount = @($geom.bones[0].cubes).Count
    # parent the appended bone to the model's root bone so insert/eject/shake
    # move the whole machine, details included (same as led_on/led_standby).
    $bone = [PSCustomObject]@{ name = 'details'; parent = $origName; pivot = @(0, 0, 0); cubes = @($cubes) }
    $geom.bones = @($geom.bones) + $bone
    $text = $json | ConvertTo-Json -Depth 64
    [System.IO.File]::WriteAllText((Join-Path $geoOut "$model.geo.json"), $text, (New-Object System.Text.UTF8Encoding($false)))
    # validate: reparse, original bone + cubes must be intact, details appended
    $check = Get-Content -Raw -LiteralPath (Join-Path $geoOut "$model.geo.json") | ConvertFrom-Json
    $bones = @($check.'minecraft:geometry'[0].bones)
    $ok = ($bones.Count -eq 2) -and ($bones[0].name -eq $origName) -and (@($bones[0].cubes).Count -eq $origCount) -and ($bones[1].name -eq 'details') -and ($bones[1].parent -eq $origName)
    $status = if ($ok) { 'OK' } else { 'FAILED' }
    Write-Output ("{0}: original bone '{1}' kept ({2} cubes) + details bone ({3} cubes, parent {1})  [{4}]" -f $model, $origName, $origCount, @($cubes).Count, $status)
}

# ===========================================================================
# rear_projection (adds: bezel, grilles, buttons, feet, vents, ports, socket,
#                 sticker, brand badge)
# ===========================================================================
function Build-RearProjectionAdd {
    $R = [ordered]@{
        body    = @{ x = 1;  y = 1;  w = 2;  h = 2 }    # original swatches
        frame   = @{ x = 5;  y = 1;  w = 2;  h = 2 }
        grille  = @{ x = 4;  y = 4;  w = 8;  h = 2 }
        vents   = @{ x = 14; y = 4;  w = 14; h = 4 }
        btn     = @{ x = 32; y = 4;  w = 2;  h = 2 }
        btn_pwr = @{ x = 36; y = 4;  w = 2;  h = 2 }
        port_w  = @{ x = 40; y = 4;  w = 2;  h = 2 }
        port_b  = @{ x = 44; y = 4;  w = 2;  h = 2 }
        port_r  = @{ x = 48; y = 4;  w = 2;  h = 2 }
        sock    = @{ x = 52; y = 4;  w = 2;  h = 2 }
        sticker = @{ x = 56; y = 4;  w = 2;  h = 2 }
        edge    = @{ x = 0;  y = 4;  w = 2;  h = 2 }
    }
    $bmp = New-Object System.Drawing.Bitmap($bakTex)
    Paint-GrilleRegion $bmp $R.grille $L1
    Paint-VentsRegion  $bmp $R.vents 64
    Paint-ButtonRegion $bmp $R.btn $null
    Paint-ButtonRegion $bmp $R.btn_pwr $RED
    Paint-PortRegion   $bmp $R.port_w $W
    Paint-PortRegion   $bmp $R.port_b $BLUE
    Paint-PortRegion   $bmp $R.port_r $RED
    Paint-SockRegion   $bmp $R.sock
    Paint-StickerRegion $bmp $R.sticker
    Paint-EdgeRegion   $bmp $R.edge
    $bmp.Save((Join-Path $texOut 'rear_projection.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    $cubes = @(
        (New-DetailCube @(-13.5, 29.8, -7.145) @(27.0, 0.45, 0.04) (@{ all = 'edge' }) $R),              # bezel top
        (New-DetailCube @(-13.5, 9.8, -7.145) @(27.0, 0.45, 0.04) (@{ all = 'edge' }) $R),               # bezel bottom
        (New-DetailCube @(-13.5, 10.25, -7.145) @(0.45, 19.5, 0.04) (@{ all = 'edge' }) $R),             # bezel left
        (New-DetailCube @(13.05, 10.25, -7.145) @(0.45, 19.5, 0.04) (@{ all = 'edge' }) $R),             # bezel right
        (New-DetailCube @(-11.2, 2.6, -7.17) @(8.4, 5.4, 0.15) (@{ all = 'frame'; north = 'grille' }) $R), # speaker grille L
        (New-DetailCube @(2.8, 2.6, -7.17) @(8.4, 5.4, 0.15) (@{ all = 'frame'; north = 'grille' }) $R),   # speaker grille R
        (New-DetailCube @(-1.25, 4.15, -7.22) @(1.0, 1.0, 0.15) (@{ all = 'frame'; north = 'btn_pwr' }) $R), # power button
        (New-DetailCube @(0.0, 4.3, -7.18) @(0.7, 0.7, 0.11) (@{ all = 'frame'; north = 'btn' }) $R),
        (New-DetailCube @(0.8, 4.3, -7.18) @(0.7, 0.7, 0.11) (@{ all = 'frame'; north = 'btn' }) $R),
        (New-DetailCube @(-11, 0, -7.4) @(3, 0.9, 0.45) (@{ all = 'body'; down = 'sock' }) $R),            # foot L
        (New-DetailCube @(8, 0, -7.4) @(3, 0.9, 0.45) (@{ all = 'body'; down = 'sock' }) $R),              # foot R
        (New-DetailCube @(-9, 31.96, -5.5) @(18, 0.09, 3.6) (@{ all = 'frame'; up = 'vents' }) $R),       # top vents
        (New-DetailCube @(-5, 16, 5.97) @(10, 8, 0.06) (@{ all = 'frame'; south = 'vents' }) $R),         # rear vents
        (New-DetailCube @(1.2, 5.0, 6.97) @(0.8, 0.8, 0.11) (@{ all = 'frame'; south = 'port_w' }) $R),
        (New-DetailCube @(2.4, 5.0, 6.97) @(0.8, 0.8, 0.11) (@{ all = 'frame'; south = 'port_b' }) $R),
        (New-DetailCube @(3.6, 5.0, 6.97) @(0.8, 0.8, 0.11) (@{ all = 'frame'; south = 'port_r' }) $R),
        (New-DetailCube @(2.2, 3.3, 6.97) @(1.6, 1.2, 0.11) (@{ all = 'frame'; south = 'sock' }) $R),    # power socket
        (New-DetailCube @(4.3, 8.6, 6.985) @(1.4, 1.0, 0.04) (@{ all = 'frame'; south = 'sticker' }) $R), # sticker
        (New-DetailCube @(7.8, 9.85, -7.19) @(2.4, 0.35, 0.05) (@{ all = 'frame'; north = 'sticker' }) $R) # brand badge
    )
    Add-DetailBone 'rear_projection' $cubes
}

Build-RearProjectionAdd
