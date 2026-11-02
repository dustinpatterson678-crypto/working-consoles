# gen_crts.ps1 - generates the reworked CRT family for wcore (crt_large,
# crt_small, tv): detailed GeckoLib geometry + hand-painted 64x64 atlases.
#
#   powershell -File gen_crts.ps1
#
# Constraints honoured (from TvRenderer/TvType java, model must not change code):
#   crt_large: screen plate x -6.5..6.5  y 3..13     z -8.12..-8.08
#   crt_small: screen plate x -4.5..4.5  y 2.5..9.5  z -6.15..-6.11
#   tv:        screen plate x -14..14    y 4.5..25.5 z -1.02..-0.98
# The live screen quad is drawn by code exactly on these plates' front faces.
#
# Orientation: front = north (-z). Painted regions read like the player sees
# the face: left of the region = model +x (east), which is the player's left
# when facing the front from the north.

. (Join-Path $PSScriptRoot 'gen_lib.ps1')

$wcoreAssets = Join-Path $PSScriptRoot '..\..\wcore\src\main\resources\assets\wcore'
$geoOut = Join-Path $wcoreAssets 'geckolib\models\item'
$texOut = Join-Path $wcoreAssets 'textures\item'

function Write-Geo([string]$path, [string]$json) {
    [System.IO.File]::WriteAllText($path, $json, (New-Object System.Text.UTF8Encoding($false)))
}

# ===========================================================================
# shared painters
# ===========================================================================
function Paint-Grain($bmp, $r, [string]$base, [int]$amp, [int]$seed) {
    # horizontal 1px striations (Game Boy SP shell technique)
    $rnd = New-Object System.Random($seed)
    $bc = C $base
    for ($j = 0; $j -lt $r.h; $j++) {
        $delta = $rnd.Next(-$amp, $amp + 1)
        HLine $bmp $r.x ($r.y + $j) $r.w (Shift-Col $bc $delta)
        for ($i = 0; $i -lt $r.w; $i++) {
            if ($rnd.Next(0, 7) -eq 0) { Put-Px $bmp ($r.x + $i) ($r.y + $j) (Shift-Col $bc ($delta + $rnd.Next(-2, 3))) }
        }
    }
    HLine $bmp $r.x $r.y $r.w (Shift-Col $bc $amp)                     # top lit
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (Shift-Col $bc (-$amp))     # bottom shaded
}

function Paint-Screen($bmp, $r, $streakCol) {
    # house style (Game Boy SP): plain dark glass + one diagonal light streak
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#181a1c' 2 991
    $len = [Math]::Min([int]($r.w / 2), [int]($r.h / 2))
    for ($k = 0; $k -lt $len; $k += 1) {
        $px = $r.x + 2 + $k; $py = $r.y + 1 + [int]($k / 2)
        if ($px -lt ($r.x + $r.w - 1) -and $py -lt ($r.y + $r.h - 1)) { Put-Px $bmp $px $py $streakCol }
    }
    # faint vignette
    $edge = C '#101214'
    HLine $bmp $r.x $r.y $r.w $edge; HLine $bmp $r.x ($r.y + $r.h - 1) $r.w $edge
    VLine $bmp $r.x $r.y $r.h $edge; VLine $bmp ($r.x + $r.w - 1) $r.y $r.h $edge
}

function Paint-Bezel($bmp, $r) {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#191b1e' 3 77
    HLine $bmp $r.x $r.y $r.w (C '#26282d')
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#0f1012')
}

function Paint-Grille($bmp, $r) {
    # dot-matrix speaker grille (Game Boy SP technique)
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#242628' 3 55
    for ($j = 0; $j -lt $r.h; $j++) {
        for ($i = 0; $i -lt $r.w; $i++) {
            if ((($i + 1) % 2) -eq 0 -and (($j + 1) % 2) -eq 0) { Put-Px $bmp ($r.x + $i) ($r.y + $j) (C '#0c0d0f') }
        }
    }
    HLine $bmp $r.x $r.y $r.w (C '#2e3033'); HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#141517')
}

function Paint-Button($bmp, $r, [int]$mode, $dotCol = $null) {
    # controller-style button: dark cap (#2c2e34 from wireless_controller.png)
    # with a specular cross highlight, optional centre dot (mode >= 1)
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#2c2e34' 3 31
    HLine $bmp $r.x $r.y $r.w (C '#40434a')
    HLine $bmp $r.x ($r.y + $r.h - 1) $r.w (C '#17181c')
    $cx = $r.x + [int]($r.w / 2); $cy = $r.y + [int]($r.h / 2)
    if ($r.w -ge 4 -and $r.h -ge 4) {
        $hi = C '#5a5f68'
        Put-Px $bmp $cx ($cy - 1) $hi; Put-Px $bmp ($cx - 1) $cy $hi
        Put-Px $bmp ($cx + 1) $cy $hi; Put-Px $bmp $cx ($cy + 1) $hi
    }
    if ($mode -ge 1 -and $null -ne $dotCol) { Put-Px $bmp $cx $cy $dotCol }
}

function Paint-SlitRows($bmp, $r) {
    # PS2-style vent slits drawn OVER an existing fill: near-black slit bars
    # with a lit lip row above each
    $step = 3
    if ($r.h -le 6) { $step = 2 }
    for ($j = 2; $j -lt ($r.h - 1); $j += $step) {
        HLine $bmp ($r.x + 1) ($r.y + $j) ($r.w - 2) (C '#0f1013')
        HLine $bmp ($r.x + 1) ($r.y + $j - 1) ($r.w - 2) (C '#33363c')
    }
}

function Paint-Dark($bmp, $r, [string]$base = '#26282c') {
    Noise-Rect $bmp $r.x $r.y $r.w $r.h $base 3 7
}

function Paint-Port($bmp, $r, [string]$colHex) {
    # region is tiny (2x2): solid color reads as the port; the model sliver
    # around it is dark anyway
    Fill-Rect $bmp $r.x $r.y $r.w $r.h (C $colHex)
    Put-Px $bmp $r.x ($r.y + $r.h - 1) (C '#141517')
}

function Paint-Badge($bmp, $r, $dashCol) {
    # brand dash on near-black plate (the original tv.png's grey-dash idiom)
    Noise-Rect $bmp $r.x $r.y $r.w $r.h '#15171a' 2 5
    HLine $bmp ($r.x + 1) ($r.y + [int]($r.h / 2)) ($r.w - 2) $dashCol
}

function Paint-PanelCrtOverlay($bmp, $r, [double]$pw, [double]$ph, [int]$seed) {
    # recess shading ring + corner screws, drawn over an existing patch/grain
    # fill of the panel region; px_x 0 = model +x edge, py 0 = model top edge
    $sx = $r.x + [int]([Math]::Round($pw * 1.5 / 16.0))
    $sw = $r.w - 2 * [int]([Math]::Round($pw * 1.5 / 16.0))
    $sy = $r.y + [int]([Math]::Round($ph * 2.0 / 15.0))
    $sh = $r.h - [int]([Math]::Round($ph * 2.0 / 15.0)) - [int]([Math]::Round($ph * 2.0 / 15.0))
    $dark = C '#101214'
    Fill-Rect $bmp ($sx + 1) ($sy + 1) ($sw - 2) 1 $dark
    Fill-Rect $bmp ($sx + 1) ($sy + $sh - 2) ($sw - 2) 1 $dark
    VLine $bmp ($sx + 1) ($sy + 1) ($sh - 2) $dark
    VLine $bmp ($sx + $sw - 2) ($sy + 1) ($sh - 2) $dark
    # corner screws
    $scr = C '#3a3d43'
    Put-Px $bmp ($r.x + 1) ($r.y + 1) $scr; Put-Px $bmp ($r.x + $r.w - 2) ($r.y + 1) $scr
    Put-Px $bmp ($r.x + 1) ($r.y + $r.h - 2) $scr; Put-Px $bmp ($r.x + $r.w - 2) ($r.y + $r.h - 2) $scr
}

# ===========================================================================
# crt_large
# ===========================================================================
function Build-CrtLarge {
    $regions = [ordered]@{
        plastic  = @{ x = 0;  y = 0;  w = 16; h = 16 }
        panel    = @{ x = 16; y = 0;  w = 16; h = 14 }
        screen   = @{ x = 0;  y = 16; w = 13; h = 10 }
        back     = @{ x = 13; y = 16; w = 12; h = 12 }
        grille   = @{ x = 32; y = 0;  w = 8;  h = 2 }
        bezel    = @{ x = 32; y = 2;  w = 14; h = 2 }
        topvents = @{ x = 47; y = 0;  w = 14; h = 4 }
        port_y   = @{ x = 32; y = 4;  w = 2;  h = 2 }
        port_w   = @{ x = 35; y = 4;  w = 2;  h = 2 }
        port_r   = @{ x = 38; y = 4;  w = 2;  h = 2 }
        btn      = @{ x = 41; y = 4;  w = 2;  h = 2 }
        btn_pwr  = @{ x = 44; y = 4;  w = 2;  h = 2 }
        dark     = @{ x = 50; y = 4;  w = 4;  h = 4 }
    }
    $tv = Load-SrcBmp (Join-Path $PSScriptRoot 'backup\tv.png')
    $ctl = Load-SrcBmp (Join-Path $wcoreAssets 'textures\item\wireless_controller.png')
    $gb = Load-SrcBmp (Join-Path $PSScriptRoot '..\..\gbsp\src\main\resources\assets\gbsp\textures\item\game_boy_sp.png')
    # colours + pixel fields sampled from the mod's own textures
    $colRed = Find-Color $tv 46 30 14 12 'red'          # original tv.png standby red
    $colDash = Find-Color $tv 6 26 20 8 'light'         # its grey brand dash
    $colYellow = Find-Color $ctl 30 14 32 12 'yellow'   # controller dot yellow
    $colWhite = Find-Color $ctl 0 0 24 24 'light'       # its light body tone
    $colStreak = Find-Color $gb 64 64 16 40 'light'     # Game Boy glass streak

    $bmp = New-Atlas 64 64
    Copy-Patch $tv 2 2 24 24 $bmp $regions.plastic 101      # tv.png grey plastic field
    Copy-Patch $tv 8 6 22 20 $bmp $regions.panel 202        # panel plate
    Paint-PanelCrtOverlay $bmp $regions.panel 16 13.8 202
    Paint-Screen   $bmp $regions.screen $colStreak
    Copy-Patch $tv 6 8 22 20 $bmp $regions.back 303         # back plate + slits
    Paint-SlitRows $bmp $regions.back
    Paint-Grille   $bmp $regions.grille
    Paint-Bezel    $bmp $regions.bezel
    Copy-Patch $tv 34 2 24 24 $bmp $regions.topvents 404    # near-black field + slits
    Paint-SlitRows $bmp $regions.topvents
    Paint-Port     $bmp $regions.port_y (Color-Hex $colYellow)
    Paint-Port     $bmp $regions.port_w (Color-Hex $colWhite)
    Paint-Port     $bmp $regions.port_r (Color-Hex $colRed)
    Paint-Button   $bmp $regions.btn 0
    Paint-Button   $bmp $regions.btn_pwr 1 $colRed
    Copy-Patch $tv 2 44 18 16 $bmp $regions.dark 407        # deep shadow field
    # badge + standby LED painted straight onto the bottom-centre of the panel
    $pr = $regions.panel
    Paint-Badge $bmp @{ x = ($pr.x + 6); y = ($pr.y + 12); w = 4; h = 2 } $colDash
    Put-Px $bmp ($pr.x + 5) ($pr.y + 13) $colRed
    $tv.Dispose(); $ctl.Dispose(); $gb.Dispose()

    $all  = @{ north = 'plastic'; east = 'plastic'; south = 'plastic'; west = 'plastic'; up = 'plastic'; down = 'dark' }
    $done = @{ east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' }

    $bones = @(
        @{ name = 'feet'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(-7.6, 0, -6.4); s = @(3.0, 1.3, 11.6); f = @{ north = 'dark'; east = 'dark'; west = 'dark'; south = 'dark'; down = 'dark' } }
            @{ o = @(4.6, 0, -6.4);  s = @(3.0, 1.3, 11.6); f = @{ north = 'dark'; east = 'dark'; west = 'dark'; south = 'dark'; down = 'dark' } }
        ) }
        @{ name = 'console'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(-8, 1.2, -8);    s = @(16, 13.8, 5.2); f = @{ east = 'plastic'; west = 'plastic'; south = 'plastic'; up = 'topvents'; down = 'dark' } }
            @{ o = @(-7.2, 1.6, -3.1); s = @(14.4, 12.8, 6.6); f = @{ east = 'plastic'; west = 'plastic'; south = 'plastic'; up = 'plastic'; down = 'dark' } }
            @{ o = @(-5.8, 3, 3.2);   s = @(11.6, 9.6, 4.8); f = @{ east = 'back'; west = 'back'; south = 'back'; up = 'back'; down = 'dark' } }
        ) }
        @{ name = 'front'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(-8, 1.2, -8.02);  s = @(16, 13.8, 0.03); f = @{ north = 'panel'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
            @{ o = @(-6.5, 3, -8.12);  s = @(13, 10, 0.04);   f = @{ north = 'screen'; east = 'bezel'; west = 'bezel'; up = 'bezel'; down = 'bezel' } }
            @{ o = @(-7, 13, -8.1);    s = @(14, 1, 0.06);    f = @{ north = 'bezel'; east = 'bezel'; west = 'bezel'; up = 'bezel'; down = 'bezel' } }
            @{ o = @(-7, 2.6, -8.1);   s = @(14, 0.4, 0.06);  f = @{ north = 'bezel'; east = 'bezel'; west = 'bezel'; up = 'bezel'; down = 'bezel' } }
            @{ o = @(6.5, 3, -8.1);    s = @(0.5, 10, 0.06);  f = @{ north = 'bezel'; east = 'bezel'; west = 'bezel'; up = 'bezel'; down = 'bezel' } }
            @{ o = @(-7, 3, -8.1);     s = @(0.5, 10, 0.06);  f = @{ north = 'bezel'; east = 'bezel'; west = 'bezel'; up = 'bezel'; down = 'bezel' } }
            @{ o = @(2.2, 1.4, -8.06); s = @(5.2, 1.0, 0.05); f = @{ north = 'grille'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
        ) }
        @{ name = 'controls'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(-7.2, 1.35, -8.11); s = @(1.3, 1.1, 0.10); f = @{ north = 'btn_pwr'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
            @{ o = @(-5.3, 1.85, -8.11); s = @(0.8, 0.55, 0.09); f = @{ north = 'btn'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
            @{ o = @(-5.3, 1.35, -8.11); s = @(0.8, 0.4, 0.09);  f = @{ north = 'btn'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
            @{ o = @(-4.3, 1.85, -8.11); s = @(0.8, 0.55, 0.09); f = @{ north = 'btn'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
            @{ o = @(-4.3, 1.35, -8.11); s = @(0.8, 0.4, 0.09);  f = @{ north = 'btn'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
        ) }
        @{ name = 'back'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(-1.7, 4.6, 8.0);  s = @(0.9, 0.9, 0.1); f = @{ south = 'port_y' } }
            @{ o = @(-0.55, 4.6, 8.0); s = @(0.9, 0.9, 0.1); f = @{ south = 'port_w' } }
            @{ o = @(0.6, 4.6, 8.0);   s = @(0.9, 0.9, 0.1); f = @{ south = 'port_r' } }
            @{ o = @(3.4, 3.8, 8.0);   s = @(1.6, 1.6, 0.12); f = @{ south = 'dark'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
        ) }
    )
    $json = ConvertTo-GeoJson 'geometry.crt_large' 64 64 3 3 @(0, 1, 0) $bones $regions
    Write-Geo (Join-Path $geoOut 'crt_large.geo.json') $json
    Save-Atlas $bmp (Join-Path $texOut 'crt_large.png')
    $cubes = ($bones | ForEach-Object { $_.cubes.Count } | Measure-Object -Sum).Sum
    Write-Output ("crt_large: {0} bones, {1} cubes  -> geo + png" -f $bones.Count, $cubes)
}

# ===========================================================================
# crt_small
# ===========================================================================
function Build-CrtSmall {
    $regions = [ordered]@{
        plastic = @{ x = 0;  y = 0;  w = 16; h = 16 }
        panel   = @{ x = 16; y = 0;  w = 16; h = 11 }
        screen  = @{ x = 0;  y = 16; w = 9;  h = 7 }
        back    = @{ x = 9;  y = 16; w = 10; h = 9 }
        grille  = @{ x = 32; y = 0;  w = 6;  h = 2 }
        bezel   = @{ x = 32; y = 2;  w = 14; h = 2 }
        topvents = @{ x = 40; y = 0; w = 12; h = 3 }
        port_y  = @{ x = 32; y = 4;  w = 2;  h = 2 }
        port_w  = @{ x = 35; y = 4;  w = 2;  h = 2 }
        btn     = @{ x = 38; y = 4;  w = 2;  h = 2 }
        btn_pwr = @{ x = 41; y = 4;  w = 2;  h = 2 }
        dark    = @{ x = 44; y = 4;  w = 4;  h = 4 }
        silver  = @{ x = 32; y = 7;  w = 10; h = 6 }
    }
    $tv = Load-SrcBmp (Join-Path $PSScriptRoot 'backup\tv.png')
    $ctl = Load-SrcBmp (Join-Path $wcoreAssets 'textures\item\wireless_controller.png')
    $gb = Load-SrcBmp (Join-Path $PSScriptRoot '..\..\gbsp\src\main\resources\assets\gbsp\textures\item\game_boy_sp.png')
    $colRed = Find-Color $tv 46 30 14 12 'red'
    $colDash = Find-Color $tv 6 26 20 8 'light'
    $colYellow = Find-Color $ctl 30 14 32 12 'yellow'
    $colWhite = Find-Color $ctl 0 0 24 24 'light'
    $colStreak = Find-Color $gb 64 64 16 40 'light'

    $bmp = New-Atlas 64 64
    Paint-Grain    $bmp $regions.plastic '#c9bfa2' 6 111     # beige shell, GB graining
    Paint-Grain    $bmp $regions.panel '#c9bfa2' 6 222
    Paint-PanelCrtOverlay $bmp $regions.panel 12 10.1 222
    Paint-Screen   $bmp $regions.screen $colStreak
    Copy-Patch $tv 6 8 22 20 $bmp $regions.back 333          # tv.png grey + slits
    Paint-SlitRows $bmp $regions.back
    Copy-Patch $tv 34 2 24 24 $bmp $regions.topvents 343     # near-black + slits
    Paint-SlitRows $bmp $regions.topvents
    Copy-Patch $gb 65 17 20 12 $bmp $regions.silver 353      # Game Boy silver band
    Paint-Grille   $bmp $regions.grille
    Paint-Bezel    $bmp $regions.bezel
    Paint-Port     $bmp $regions.port_y (Color-Hex $colYellow)
    Paint-Port     $bmp $regions.port_w (Color-Hex $colWhite)
    Paint-Button   $bmp $regions.btn 0
    Paint-Button   $bmp $regions.btn_pwr 1 $colRed
    Copy-Patch $tv 2 44 18 16 $bmp $regions.dark 444
    $pr = $regions.panel
    Paint-Badge $bmp @{ x = ($pr.x + 6); y = ($pr.y + 9); w = 4; h = 2 } $colDash
    Put-Px $bmp ($pr.x + 5) ($pr.y + 10) $colRed
    $tv.Dispose(); $ctl.Dispose(); $gb.Dispose()

    $bones = @(
        @{ name = 'feet'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(-5.6, 0, -4.6); s = @(2.4, 0.9, 8.4); f = @{ north = 'dark'; east = 'dark'; west = 'dark'; south = 'dark'; down = 'dark' } }
            @{ o = @(3.2, 0, -4.6);  s = @(2.4, 0.9, 8.4); f = @{ north = 'dark'; east = 'dark'; west = 'dark'; south = 'dark'; down = 'dark' } }
        ) }
        @{ name = 'handle'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(-2.9, 11, -3.2); s = @(0.8, 1.5, 0.9); f = @{ north = 'silver'; east = 'silver'; west = 'silver'; south = 'silver'; up = 'silver' } }
            @{ o = @(2.1, 11, -3.2);  s = @(0.8, 1.5, 0.9); f = @{ north = 'silver'; east = 'silver'; west = 'silver'; south = 'silver'; up = 'silver' } }
            @{ o = @(-2.9, 12.5, -3.2); s = @(5.8, 0.7, 0.9); f = @{ north = 'silver'; east = 'silver'; west = 'silver'; south = 'silver'; up = 'silver'; down = 'silver' } }
        ) }
        @{ name = 'console'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(-6, 0.9, -6);   s = @(12, 10.1, 4.2); f = @{ east = 'plastic'; west = 'plastic'; south = 'plastic'; up = 'topvents'; down = 'dark' } }
            @{ o = @(-5.2, 1.3, -2.2); s = @(10.4, 9.2, 4); f = @{ east = 'plastic'; west = 'plastic'; south = 'plastic'; up = 'plastic'; down = 'dark' } }
            @{ o = @(-4, 2.2, 1.6);  s = @(8, 7.6, 3.8);  f = @{ east = 'back'; west = 'back'; south = 'back'; up = 'back'; down = 'dark' } }
        ) }
        @{ name = 'front'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(-6, 0.9, -6.02); s = @(12, 10.1, 0.03); f = @{ north = 'panel'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
            @{ o = @(-4.5, 2.5, -6.15); s = @(9, 7, 0.04);  f = @{ north = 'screen'; east = 'bezel'; west = 'bezel'; up = 'bezel'; down = 'bezel' } }
            @{ o = @(-5, 9.5, -6.12);  s = @(10, 0.7, 0.05); f = @{ north = 'bezel'; east = 'bezel'; west = 'bezel'; up = 'bezel'; down = 'bezel' } }
            @{ o = @(-5, 2.0, -6.12);  s = @(10, 0.5, 0.05); f = @{ north = 'bezel'; east = 'bezel'; west = 'bezel'; up = 'bezel'; down = 'bezel' } }
            @{ o = @(4.5, 2.5, -6.12); s = @(0.5, 7, 0.05);  f = @{ north = 'bezel'; east = 'bezel'; west = 'bezel'; up = 'bezel'; down = 'bezel' } }
            @{ o = @(-5, 2.5, -6.12);  s = @(0.5, 7, 0.05);  f = @{ north = 'bezel'; east = 'bezel'; west = 'bezel'; up = 'bezel'; down = 'bezel' } }
            @{ o = @(1.6, 1.0, -6.08); s = @(4.0, 1.0, 0.05); f = @{ north = 'grille'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
        ) }
        @{ name = 'controls'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(-5.4, 0.95, -6.12); s = @(1.1, 1.0, 0.09); f = @{ north = 'btn_pwr'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
            @{ o = @(-4.0, 0.95, -6.11); s = @(0.9, 0.9, 0.08); f = @{ north = 'btn'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
            @{ o = @(-3.0, 0.95, -6.11); s = @(0.9, 0.9, 0.08); f = @{ north = 'btn'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
        ) }
        @{ name = 'back'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(1.4, 3.0, 5.4); s = @(0.9, 0.9, 0.1); f = @{ south = 'port_y' } }
            @{ o = @(2.5, 3.0, 5.4); s = @(0.9, 0.9, 0.1); f = @{ south = 'port_w' } }
            @{ o = @(-2.2, 2.4, 5.4); s = @(1.4, 1.4, 0.1); f = @{ south = 'dark'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
        ) }
    )
    $json = ConvertTo-GeoJson 'geometry.crt_small' 64 64 3 3 @(0, 1, 0) $bones $regions
    Write-Geo (Join-Path $geoOut 'crt_small.geo.json') $json
    Save-Atlas $bmp (Join-Path $texOut 'crt_small.png')
    $cubes = ($bones | ForEach-Object { $_.cubes.Count } | Measure-Object -Sum).Sum
    Write-Output ("crt_small: {0} bones, {1} cubes  -> geo + png" -f $bones.Count, $cubes)
}

# ===========================================================================
# tv (flat panel)
# ===========================================================================
function Build-FlatTv {
    $regions = [ordered]@{
        tvback   = @{ x = 0;  y = 0;  w = 32; h = 24 }
        screen   = @{ x = 0;  y = 24; w = 28; h = 21 }
        bezel    = @{ x = 32; y = 24; w = 14; h = 2 }
        grille   = @{ x = 47; y = 24; w = 8;  h = 2 }
        hub      = @{ x = 32; y = 27; w = 12; h = 7 }
        btn      = @{ x = 32; y = 35; w = 2;  h = 2 }
        btn_pwr  = @{ x = 35; y = 35; w = 2;  h = 2 }
        led      = @{ x = 38; y = 35; w = 2;  h = 2 }
        dark     = @{ x = 41; y = 35; w = 4;  h = 4 }
    }
    $tv = Load-SrcBmp (Join-Path $PSScriptRoot 'backup\tv.png')
    $ctl = Load-SrcBmp (Join-Path $wcoreAssets 'textures\item\wireless_controller.png')
    $gb = Load-SrcBmp (Join-Path $PSScriptRoot '..\..\gbsp\src\main\resources\assets\gbsp\textures\item\game_boy_sp.png')
    $colGreen = Find-Color $ctl 30 14 26 10 'green'
    $colYellow = Find-Color $ctl 30 14 32 12 'yellow'
    $colWhite = Find-Color $ctl 0 0 24 24 'light'
    $colRed = Find-Color $tv 46 30 14 12 'red'
    $colStreak = Find-Color $gb 64 64 16 40 'light'

    $bmp = New-Atlas 64 64
    Copy-Patch $tv 2 2 24 24 $bmp $regions.tvback 141     # the original tv.png plastic
    Paint-Screen   $bmp $regions.screen $colStreak
    Paint-Bezel    $bmp $regions.bezel
    Paint-Grille   $bmp $regions.grille
    Paint-Button   $bmp $regions.btn 0
    Paint-Button   $bmp $regions.btn_pwr 1 $colRed
    Paint-Button   $bmp $regions.led 1 $colRed
    Copy-Patch $tv 2 44 18 16 $bmp $regions.dark 147
    # hub: connector ports + vent slits over the tv's own near-black field
    $hb = $regions.hub
    Copy-Patch $tv 34 2 24 24 $bmp $hb 148
    $px = $hb.x + 1; $py = $hb.y + 2
    Fill-Rect $bmp $px $py 2 2 $colGreen; $px += 3              # green component port
    Fill-Rect $bmp $px $py 2 2 $colYellow; $px += 3             # yellow
    Fill-Rect $bmp $px $py 2 2 $colWhite; $px += 3              # white
    Fill-Rect $bmp $px $py 2 2 $colRed                          # red
    for ($j = $hb.y + 5; $j -lt ($hb.y + $hb.h - 1); $j++) {
        for ($i = $hb.x + 1; $i -lt ($hb.x + $hb.w - 1); $i += 2) { Put-Px $bmp $i $j (C '#0f1013') }
    }
    $tv.Dispose(); $ctl.Dispose(); $gb.Dispose()

    $bones = @(
        @{ name = 'stand'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(-6.5, 0, -3.2);  s = @(13, 0.7, 6.4); f = @{ north = 'tvback'; east = 'tvback'; west = 'tvback'; south = 'tvback'; up = 'tvback'; down = 'dark' } }
            @{ o = @(-4.5, 0.7, -2.4); s = @(9, 0.5, 4.8); f = @{ north = 'tvback'; east = 'tvback'; west = 'tvback'; south = 'tvback'; up = 'tvback' } }
            @{ o = @(-1.4, 1.2, -0.5); s = @(2.8, 1.6, 1.7); f = @{ north = 'tvback'; east = 'tvback'; west = 'tvback'; south = 'tvback' } }
        ) }
        @{ name = 'panel'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(-15, 2.6, -0.6);   s = @(30, 23.4, 2.1); f = @{ north = 'tvback'; east = 'tvback'; west = 'tvback'; south = 'tvback'; up = 'tvback'; down = 'dark' } }
            @{ o = @(-14.6, 25.5, -1.0); s = @(29.2, 0.5, 0.4); f = @{ north = 'bezel'; east = 'tvback'; west = 'tvback'; up = 'tvback'; down = 'bezel' } }
            @{ o = @(-14.6, 4.0, -1.0);  s = @(29.2, 0.5, 0.4); f = @{ north = 'bezel'; east = 'tvback'; west = 'tvback'; up = 'bezel'; down = 'tvback' } }
            @{ o = @(-14.6, 4.5, -1.0);  s = @(0.6, 21, 0.4);   f = @{ north = 'bezel'; east = 'bezel'; west = 'tvback'; up = 'bezel'; down = 'bezel' } }
            @{ o = @(14, 4.5, -1.0);     s = @(0.6, 21, 0.4);   f = @{ north = 'bezel'; east = 'tvback'; west = 'bezel'; up = 'bezel'; down = 'bezel' } }
            @{ o = @(-14, 4.5, -1.02);   s = @(28, 21, 0.04);   f = @{ north = 'screen'; east = 'bezel'; west = 'bezel'; up = 'bezel'; down = 'bezel' } }
            @{ o = @(7.0, 2.85, -1.0);   s = @(7.4, 0.9, 0.4);   f = @{ north = 'grille'; east = 'tvback'; west = 'dark'; up = 'tvback'; down = 'tvback' } }
            @{ o = @(-13.6, 2.9, -1.06); s = @(1.2, 1.0, 0.46);  f = @{ north = 'btn_pwr'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
            @{ o = @(-11.8, 2.9, -1.04); s = @(0.9, 0.8, 0.44);  f = @{ north = 'btn'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
            @{ o = @(-10.6, 2.9, -1.04); s = @(0.9, 0.8, 0.44);  f = @{ north = 'btn'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
            @{ o = @(-3.4, 2.95, -1.0);  s = @(4.6, 0.9, 0.4);   f = @{ north = 'bezel'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
            @{ o = @(4.6, 2.95, -1.0);   s = @(0.5, 0.5, 0.42);  f = @{ north = 'led'; east = 'dark'; west = 'dark'; up = 'dark'; down = 'dark' } }
        ) }
        @{ name = 'back'; pivot = @(0, 0, 0); cubes = @(
            @{ o = @(-3.5, 5, 1.5); s = @(7, 5, 0.6); f = @{ south = 'hub'; east = 'tvback'; west = 'tvback'; up = 'tvback'; down = 'tvback' } }
        ) }
    )
    $json = ConvertTo-GeoJson 'geometry.tv' 64 64 3 2.5 @(0, 1, 0) $bones $regions
    Write-Geo (Join-Path $geoOut 'tv.geo.json') $json
    Save-Atlas $bmp (Join-Path $texOut 'tv.png')
    $cubes = ($bones | ForEach-Object { $_.cubes.Count } | Measure-Object -Sum).Sum
    Write-Output ("tv: {0} bones, {1} cubes  -> geo + png" -f $bones.Count, $cubes)
}

Build-CrtLarge
Build-CrtSmall
Build-FlatTv
