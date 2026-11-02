# gen_crts_add.ps1 - ADDITIVE detail pass for crt_large / crt_small / tv.
#
# Keeps the ORIGINAL models exactly as shipped (same bones, cubes, UVs,
# screen plates - the code's live screen quads still align) and APPENDS a
# "details" bone with added cubes: feet, bezel strips, grille plates,
# buttons, vent plates, AV ports, power sockets, sticker, carry handle.
#
# Textures: starts from the ORIGINAL pngs (backup/) - every pixel an
# original cube samples is untouched. Detail regions are painted ONLY in
# free atlas space using the mod's own palette (probed from the original
# swatch strip): #3a3a3e body grey, #1e1e21 frame dark, #0c1012 screen
# near-black, #aaaab0 light, #e1e1e4 white, #8cbee6 blue, #dc2828 red,
# #3ce65a green, #c4bca8 beige.
#
#   powershell -File gen_crts_add.ps1

. (Join-Path $PSScriptRoot 'gen_lib.ps1')

$wcoreAssets = Join-Path $PSScriptRoot '..\..\wcore\src\main\resources\assets\wcore'
$geoOut = Join-Path $wcoreAssets 'geckolib\models\item'
$texOut = Join-Path $wcoreAssets 'textures\item'
$bak = Join-Path $PSScriptRoot 'backup'

# ---- palette (exact colors from the original swatch strips) ----------------
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

function Paint-SilverRegion($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $L2 3 51
    HLine $bmp $r.x $r.y $r.w (C $W)
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C $L1)
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
# crt_large (adds: feet, bezel, grille, buttons, vents, ports, socket, sticker)
# ===========================================================================
function Build-CrtLargeAdd {
    $R = [ordered]@{
        body    = @{ x = 1;  y = 1;  w = 2;  h = 2 }    # original swatches
        frame   = @{ x = 5;  y = 1;  w = 2;  h = 2 }
        grille  = @{ x = 0;  y = 16; w = 8;  h = 2 }
        vents   = @{ x = 0;  y = 20; w = 14; h = 4 }
        btn     = @{ x = 32; y = 16; w = 2;  h = 2 }
        btn_pwr = @{ x = 35; y = 16; w = 2;  h = 2 }
        port_w  = @{ x = 41; y = 16; w = 2;  h = 2 }
        port_b  = @{ x = 44; y = 16; w = 2;  h = 2 }
        port_r  = @{ x = 47; y = 16; w = 2;  h = 2 }
        sock    = @{ x = 50; y = 16; w = 2;  h = 2 }
        sticker = @{ x = 56; y = 16; w = 2;  h = 2 }
        edge    = @{ x = 59; y = 16; w = 2;  h = 2 }
    }
    $bmp = New-Object System.Drawing.Bitmap((Join-Path $bak 'crt_large.png'))
    Paint-GrilleRegion $bmp $R.grille $L1
    Paint-VentsRegion  $bmp $R.vents 61
    Paint-ButtonRegion $bmp $R.btn $null
    Paint-ButtonRegion $bmp $R.btn_pwr $RED
    Paint-PortRegion   $bmp $R.port_w $W
    Paint-PortRegion   $bmp $R.port_b $BLUE
    Paint-PortRegion   $bmp $R.port_r $RED
    Paint-SockRegion   $bmp $R.sock
    Paint-StickerRegion $bmp $R.sticker
    Paint-EdgeRegion   $bmp $R.edge
    $bmp.Save((Join-Path $texOut 'crt_large.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    $cubes = @(
        (New-DetailCube @(-7.7, 0, -8.35) @(3.2, 0.9, 0.4) (@{ all = 'body'; down = 'sock' }) $R),      # foot L
        (New-DetailCube @(4.5, 0, -8.35) @(3.2, 0.9, 0.4) (@{ all = 'body'; down = 'sock' }) $R),       # foot R
        (New-DetailCube @(-6.9, 13.05, -8.115) @(13.8, 0.35, 0.04) (@{ all = 'edge' }) $R),            # bezel top
        (New-DetailCube @(-6.9, 2.65, -8.115) @(13.8, 0.35, 0.04) (@{ all = 'edge' }) $R),             # bezel bottom
        (New-DetailCube @(-6.9, 3.0, -8.115) @(0.35, 10, 0.04) (@{ all = 'edge' }) $R),                # bezel left
        (New-DetailCube @(6.55, 3.0, -8.115) @(0.35, 10, 0.04) (@{ all = 'edge' }) $R),                # bezel right
        (New-DetailCube @(2.3, 0.55, -8.155) @(4.3, 1.5, 0.04) (@{ all = 'frame'; north = 'grille' }) $R), # speaker grille
        (New-DetailCube @(-6.75, 0.95, -8.17) @(1.2, 1.2, 0.055) (@{ all = 'frame'; north = 'btn_pwr' }) $R), # power button
        (New-DetailCube @(-5.35, 1.45, -8.17) @(0.7, 0.5, 0.055) (@{ all = 'frame'; north = 'btn' }) $R),
        (New-DetailCube @(-5.35, 0.85, -8.17) @(0.7, 0.5, 0.055) (@{ all = 'frame'; north = 'btn' }) $R),
        (New-DetailCube @(-7, 14.97, -7.6) @(14, 0.08, 4.2) (@{ all = 'frame'; up = 'vents' }) $R),     # top vents
        (New-DetailCube @(-5.6, 4.5, 7.97) @(5.0, 5.5, 0.06) (@{ all = 'frame'; south = 'vents' }) $R), # rear vents
        (New-DetailCube @(1.2, 5.0, 7.97) @(0.8, 0.8, 0.11) (@{ all = 'frame'; south = 'port_w' }) $R),
        (New-DetailCube @(2.4, 5.0, 7.97) @(0.8, 0.8, 0.11) (@{ all = 'frame'; south = 'port_b' }) $R),
        (New-DetailCube @(3.6, 5.0, 7.97) @(0.8, 0.8, 0.11) (@{ all = 'frame'; south = 'port_r' }) $R),
        (New-DetailCube @(2.2, 3.3, 7.97) @(1.6, 1.2, 0.11) (@{ all = 'frame'; south = 'sock' }) $R),  # power socket
        (New-DetailCube @(4.3, 8.6, 7.985) @(1.4, 1.0, 0.04) (@{ all = 'frame'; south = 'sticker' }) $R)
    )
    Add-DetailBone 'crt_large' $cubes
}

# ===========================================================================
# crt_small (adds: carry handle, bezel, grille, buttons, feet, vents, ports)
# ===========================================================================
function Build-CrtSmallAdd {
    $R = [ordered]@{
        body    = @{ x = 33; y = 1;  w = 2;  h = 2 }
        frame   = @{ x = 5;  y = 1;  w = 2;  h = 2 }
        grille  = @{ x = 0;  y = 16; w = 8;  h = 2 }
        vents   = @{ x = 0;  y = 20; w = 12; h = 4 }
        silver  = @{ x = 0;  y = 26; w = 10; h = 4 }
        btn     = @{ x = 32; y = 16; w = 2;  h = 2 }
        port_w  = @{ x = 38; y = 16; w = 2;  h = 2 }
        port_b  = @{ x = 41; y = 16; w = 2;  h = 2 }
        port_r  = @{ x = 44; y = 16; w = 2;  h = 2 }
        sock    = @{ x = 47; y = 16; w = 2;  h = 2 }
        sticker = @{ x = 53; y = 16; w = 2;  h = 2 }
        edge    = @{ x = 56; y = 16; w = 2;  h = 2 }
    }
    $bmp = New-Object System.Drawing.Bitmap((Join-Path $bak 'crt_small.png'))
    Paint-GrilleRegion $bmp $R.grille $L1
    Paint-VentsRegion  $bmp $R.vents 62
    Paint-SilverRegion $bmp $R.silver
    Paint-ButtonRegion $bmp $R.btn $null
    Paint-PortRegion   $bmp $R.port_w $W
    Paint-PortRegion   $bmp $R.port_b $BLUE
    Paint-PortRegion   $bmp $R.port_r $RED
    Paint-SockRegion   $bmp $R.sock
    Paint-StickerRegion $bmp $R.sticker
    Paint-EdgeRegion   $bmp $R.edge
    $bmp.Save((Join-Path $texOut 'crt_small.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    $cubes = @(
        (New-DetailCube @(-3.0, 10.95, -3.3) @(0.8, 1.6, 1.0) (@{ all = 'silver' }) $R),                 # handle post L
        (New-DetailCube @(2.2, 10.95, -3.3) @(0.8, 1.6, 1.0) (@{ all = 'silver' }) $R),                  # handle post R
        (New-DetailCube @(-3.0, 12.5, -3.3) @(6.0, 0.75, 1.0) (@{ all = 'silver' }) $R),                 # handle grip
        (New-DetailCube @(-4.9, 9.55, -6.135) @(9.8, 0.3, 0.04) (@{ all = 'edge' }) $R),                # bezel top
        (New-DetailCube @(-4.9, 2.2, -6.135) @(9.8, 0.3, 0.04) (@{ all = 'edge' }) $R),                 # bezel bottom
        (New-DetailCube @(-4.9, 2.5, -6.135) @(0.3, 7, 0.04) (@{ all = 'edge' }) $R),                   # bezel left
        (New-DetailCube @(4.6, 2.5, -6.135) @(0.3, 7, 0.04) (@{ all = 'edge' }) $R),                    # bezel right
        (New-DetailCube @(-3.7, 0.75, -6.155) @(3.4, 0.7, 0.04) (@{ all = 'frame'; north = 'grille' }) $R), # speaker grille
        (New-DetailCube @(1.35, 0.75, -6.05) @(0.6, 0.6, 0.055) (@{ all = 'frame'; north = 'btn' }) $R),
        (New-DetailCube @(2.15, 0.75, -6.05) @(0.6, 0.6, 0.055) (@{ all = 'frame'; north = 'btn' }) $R),
        (New-DetailCube @(-5.8, 0, -6.32) @(2.4, 0.8, 0.37) (@{ all = 'body'; down = 'sock' }) $R),       # foot L
        (New-DetailCube @(3.4, 0, -6.32) @(2.4, 0.8, 0.37) (@{ all = 'body'; down = 'sock' }) $R),        # foot R
        (New-DetailCube @(-5, 10.97, -5.5) @(10, 0.08, 3.0) (@{ all = 'frame'; up = 'vents' }) $R),       # top vents
        (New-DetailCube @(-3.8, 3.2, 5.97) @(7.6, 4.4, 0.06) (@{ all = 'frame'; south = 'vents' }) $R),   # rear vents
        (New-DetailCube @(0.5, 2.2, 5.97) @(0.7, 0.7, 0.11) (@{ all = 'frame'; south = 'port_w' }) $R),
        (New-DetailCube @(1.5, 2.2, 5.97) @(0.7, 0.7, 0.11) (@{ all = 'frame'; south = 'port_b' }) $R),
        (New-DetailCube @(2.3, 2.2, 5.97) @(0.7, 0.7, 0.11) (@{ all = 'frame'; south = 'port_r' }) $R),
        (New-DetailCube @(-2.7, 2.1, 5.97) @(1.4, 1.0, 0.11) (@{ all = 'frame'; south = 'sock' }) $R),   # power socket
        (New-DetailCube @(2.9, 8.0, 5.985) @(1.2, 0.9, 0.04) (@{ all = 'frame'; south = 'sticker' }) $R)
    )
    Add-DetailBone 'crt_small' $cubes
}

# ===========================================================================
# tv (adds: bezel, buttons, grille, vents, ports, socket, sticker)
# ===========================================================================
function Build-FlatTvAdd {
    $R = [ordered]@{
        hubedge = @{ x = 52; y = 32; w = 8;  h = 8 }    # original hub swatch (near-black)
        grille  = @{ x = 0;  y = 56; w = 8;  h = 2 }
        vents   = @{ x = 0;  y = 59; w = 12; h = 4 }
        btn     = @{ x = 32; y = 56; w = 2;  h = 2 }
        btn_pwr = @{ x = 35; y = 56; w = 2;  h = 2 }
        port_w  = @{ x = 38; y = 56; w = 2;  h = 2 }
        port_b  = @{ x = 41; y = 56; w = 2;  h = 2 }
        port_r  = @{ x = 44; y = 56; w = 2;  h = 2 }
        sock    = @{ x = 47; y = 56; w = 2;  h = 2 }
        sticker = @{ x = 53; y = 56; w = 2;  h = 2 }
        edge    = @{ x = 56; y = 56; w = 2;  h = 2 }
    }
    $bmp = New-Object System.Drawing.Bitmap((Join-Path $bak 'tv.png'))
    Paint-GrilleRegion $bmp $R.grille $K2
    Paint-VentsRegion  $bmp $R.vents 63
    Paint-ButtonRegion $bmp $R.btn $null
    Paint-ButtonRegion $bmp $R.btn_pwr $RED
    Paint-PortRegion   $bmp $R.port_w $W
    Paint-PortRegion   $bmp $R.port_b $BLUE
    Paint-PortRegion   $bmp $R.port_r $RED
    Paint-SockRegion   $bmp $R.sock
    Paint-StickerRegion $bmp $R.sticker
    Paint-EdgeRegion   $bmp $R.edge
    $bmp.Save((Join-Path $texOut 'tv.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    $cubes = @(
        (New-DetailCube @(-14.6, 25.5, -1.045) @(29.2, 0.55, 0.05) (@{ all = 'edge' }) $R),            # bezel top
        (New-DetailCube @(-14.6, 3.95, -1.045) @(29.2, 0.55, 0.05) (@{ all = 'edge' }) $R),            # bezel bottom
        (New-DetailCube @(-14.6, 4.5, -1.045) @(0.6, 21, 0.05) (@{ all = 'edge' }) $R),               # bezel left
        (New-DetailCube @(14.0, 4.5, -1.045) @(0.6, 21, 0.05) (@{ all = 'edge' }) $R),                # bezel right
        (New-DetailCube @(-14.05, 4.03, -1.09) @(0.8, 0.45, 0.05) (@{ all = 'hubedge'; north = 'btn_pwr' }) $R), # power button
        (New-DetailCube @(-12.95, 4.05, -1.09) @(0.6, 0.4, 0.05) (@{ all = 'hubedge'; north = 'btn' }) $R),
        (New-DetailCube @(-11.95, 4.05, -1.09) @(0.6, 0.4, 0.05) (@{ all = 'hubedge'; north = 'btn' }) $R),
        (New-DetailCube @(7.5, 4.03, -1.09) @(6.5, 0.45, 0.05) (@{ all = 'hubedge'; north = 'grille' }) $R),   # speaker grille
        (New-DetailCube @(-13, 25.97, -0.8) @(26, 0.08, 1.6) (@{ all = 'hubedge'; up = 'vents' }) $R),    # top vents
        (New-DetailCube @(-8.6, 5.0, 1.47) @(3.6, 4.6, 0.06) (@{ all = 'hubedge'; south = 'vents' }) $R), # rear vents
        (New-DetailCube @(2.6, 5.0, 1.47) @(0.8, 0.8, 0.11) (@{ all = 'hubedge'; south = 'port_w' }) $R),
        (New-DetailCube @(3.8, 5.0, 1.47) @(0.8, 0.8, 0.11) (@{ all = 'hubedge'; south = 'port_b' }) $R),
        (New-DetailCube @(5.0, 5.0, 1.47) @(0.8, 0.8, 0.11) (@{ all = 'hubedge'; south = 'port_r' }) $R),
        (New-DetailCube @(-4.6, 4.6, 1.47) @(1.6, 1.2, 0.11) (@{ all = 'hubedge'; south = 'sock' }) $R), # power socket
        (New-DetailCube @(7.2, 7.6, 1.485) @(1.6, 1.2, 0.045) (@{ all = 'hubedge'; south = 'sticker' }) $R)
    )
    Add-DetailBone 'tv' $cubes
}

Build-CrtLargeAdd
Build-CrtSmallAdd
Build-FlatTvAdd
