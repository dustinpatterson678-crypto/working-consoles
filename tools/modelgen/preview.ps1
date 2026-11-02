# Renders the generated GeckoLib props to a PNG contact sheet without launching
# the game, so model and texture changes can be reviewed in seconds.
#
#   powershell -File preview_props.ps1                    # every prop, one sheet
#   powershell -File preview_props.ps1 -Ids ps2,genesis   # just these
#   powershell -File preview_props.ps1 -Ids ps2 -Fit 600 -Out big.png
#
# The projection is an oblique front view: the model's front (+z / south) face
# is drawn undistorted while the far end of the model is shifted up and to the
# right, which exposes the top and the east side - the same three faces you see
# standing in front of a real machine. Face UVs follow vanilla's vertex order
# (net.minecraft.client.renderer.EnumFaceDirection, position table built in
# FaceBakery.getPositions): the texture region's top-left corner lands on the
# face's first vertex, so texture u/v map onto the face axes exactly as in-game.
[CmdletBinding()]
param(
    [string[]]$Ids = @(),
    [double]$K = 0,                       # fixed px per model px; 0 = auto fit
    [int]$Fit = 320,                      # px the longest model axis is fitted to
    [double]$Dx = 0.34,                   # depth offset: screen x per z unit
    [double]$Dy = 0.26,                   # depth offset: screen y per z unit
    [string]$Out = "$PSScriptRoot\preview_sheet.png",
    [switch]$Flat,                        # Dx/Dy = 0: a straight-on front view
    [switch]$Quiet,
    [string]$AssetDir = ''                # assets root to preview (default: the wcore assets)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

if ($Flat) {
    # nearly straight-on: a hair of depth keeps every face non-degenerate
    $Dx = 0.08; $Dy = 0.06; $Fit = [Math]::Max($Fit, 340)
}

$rootAsset = if ($AssetDir) { $AssetDir } else { Join-Path $PSScriptRoot '..\..\wcore\src\main\resources\assets\wcore' }
$geoDir = Join-Path $rootAsset 'geckolib\models\item'
$texDir = Join-Path $rootAsset 'textures\item'

if ($Ids.Count -eq 0) {
    $Ids = @(Get-ChildItem $geoDir -Filter *.geo.json | Sort-Object Name |
        ForEach-Object { $_.Name -replace '\.geo\.json$', '' })
}
else {
    # -File passes "a,b,c" as one string: split it back into a list
    $Ids = @($Ids | ForEach-Object { $_ -split ',' } | Where-Object { $_ })
}

# For each face: the cube corner that anchors the texture region's top-left
# corner, plus the cube-space directions of the region's +u (right) and +v
# (down) axes, all as fractions of the cube size where 0 = min and 1 = max.
$faceDefs = @{
    north = @{ tl = @(1, 1, 0); u = @(-1, 0, 0); v = @(0, -1, 0) }
    south = @{ tl = @(0, 1, 1); u = @(1, 0, 0);  v = @(0, -1, 0) }
    east  = @{ tl = @(1, 1, 1); u = @(0, 0, -1); v = @(0, -1, 0) }
    west  = @{ tl = @(0, 1, 0); u = @(0, 0, 1);  v = @(0, -1, 0) }
    up    = @{ tl = @(0, 1, 0); u = @(1, 0, 0);  v = @(0, 0, 1) }
    down  = @{ tl = @(0, 0, 1); u = @(1, 0, 0);  v = @(0, 0, -1) }
}
$faceOrder = @('south', 'east', 'down', 'west', 'up', 'north')  # back faces first, front (north) last

function Invoke-Preview {
    param([string]$Id, [double]$Scale, [int]$FitPx, [double]$OffX, [double]$OffY, [int]$Pad = 6)

    $geo = Get-Content (Join-Path $geoDir "$Id.geo.json") -Raw | ConvertFrom-Json
    $bones = $geo.'minecraft:geometry'[0].bones

    $cubes = @()
    foreach ($bone in $bones) {
        if (-not $bone.cubes) { continue }
        foreach ($cube in @($bone.cubes)) {
            $cubes += , @{
                o = @([double]$cube.origin[0], [double]$cube.origin[1], [double]$cube.origin[2])
                s = @([double]$cube.size[0], [double]$cube.size[1], [double]$cube.size[2])
                uv = $cube.uv
            }
        }
    }
    if ($cubes.Count -eq 0) { throw "preview: $Id has no cubes" }

    $zMin = [double]::MaxValue
    foreach ($cube in $cubes) {
        $z = $cube.o[2]
        if ($z -lt $zMin) { $zMin = $z }
    }

    # model space -> raw screen space, before scaling: u grows right, v grows down.
    # wcore models face north (-z), so the north face is drawn undistorted and
    # the far end (south) shifts up-right, exposing the top and the west side.
    # x is negated so the image matches what a player facing the front sees
    # (facing south, the model's +x/east side is on the player's left).
    $proj = {
        param($x, $y, $z)
        $d = $z - $zMin
        return @((- $x + $OffX * $d), (-$y - $OffY * $d))
    }

    $minU = [double]::MaxValue; $maxU = [double]::MinValue
    $minV = [double]::MaxValue; $maxV = [double]::MinValue
    $quads = @()
    foreach ($cube in $cubes) {
        foreach ($face in $faceOrder) {
            $uvInfo = $cube.uv.$face
            if (-not $uvInfo) { continue }
            $def = $faceDefs[$face]
            $pt = @()
            foreach ($corner in @(@(0, 0), @(1, 0), @(0, 1))) {
                $fx = $def.tl[0] + $def.u[0] * $corner[0] + $def.v[0] * $corner[1]
                $fy = $def.tl[1] + $def.u[1] * $corner[0] + $def.v[1] * $corner[1]
                $fz = $def.tl[2] + $def.u[2] * $corner[0] + $def.v[2] * $corner[1]
                $p = & $proj ($cube.o[0] + $fx * $cube.s[0]) ($cube.o[1] + $fy * $cube.s[1]) ($cube.o[2] + $fz * $cube.s[2])
                $pt += , $p
                if ($p[0] -lt $minU) { $minU = $p[0] }
                if ($p[0] -gt $maxU) { $maxU = $p[0] }
                if ($p[1] -lt $minV) { $minV = $p[1] }
                if ($p[1] -gt $maxV) { $maxV = $p[1] }
            }
            $quads += , @{
                # Painter's order key: for a north-facing model the face that is
                # nearest to the camera has the smallest z (north is the front),
                # the west side (-x) is nearest in x and "up" in y. Sort ascending
                # paints far faces first.
                depth = (-1 * $OffX * ($cube.o[0] + ($def.tl[0] + $def.u[0] * 0.5 + $def.v[0] * 0.5) * $cube.s[0]) +
                         $OffY * ($cube.o[1] + ($def.tl[1] + $def.u[1] * 0.5 + $def.v[1] * 0.5) * $cube.s[1]) +
                         -1 * ($cube.o[2] + ($def.tl[2] + $def.u[2] * 0.5 + $def.v[2] * 0.5) * $cube.s[2]))
                pt = $pt
                u = [double]$uvInfo.uv[0]
                v = [double]$uvInfo.uv[1]
                w = [double]$uvInfo.uv_size[0]
                h = [double]$uvInfo.uv_size[1]
            }
        }
    }

    $spanU = $maxU - $minU
    $spanV = $maxV - $minV
    if ($Scale -le 0) {
        $Scale = [Math]::Round($FitPx / [Math]::Max(0.001, [Math]::Max($spanU, $spanV)), 2)
        if ($Scale -le 0) { $Scale = 4 }
    }

    $bmpW = [int][Math]::Ceiling($spanU * $Scale) + 2 * $Pad
    $bmpH = [int][Math]::Ceiling($spanV * $Scale) + 2 * $Pad
    $bmp = New-Object System.Drawing.Bitmap($bmpW, $bmpH)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.ColorTranslator]::FromHtml('#6E6E72'))
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None

    $atlas = [System.Drawing.Image]::FromFile((Join-Path $texDir "$Id.png"))
    try {
        foreach ($q in ($quads | Sort-Object { $_.depth })) {          # far first
            $dest = New-Object 'System.Drawing.PointF[]' 3
            for ($i = 0; $i -lt 3; $i++) {
                $x = (($q.pt[$i][0] - $minU) * $Scale) + $Pad
                $y = (($q.pt[$i][1] - $minV) * $Scale) + $Pad
                $dest[$i] = New-Object System.Drawing.PointF([single]$x, [single]$y)
            }
            # inset by a hair so neighbouring atlas pixels never bleed in
            $srcRect = New-Object System.Drawing.RectangleF(
                ([single]($q.u + 0.02)), ([single]($q.v + 0.02)),
                ([single]($q.w - 0.04)), ([single]($q.h - 0.04)))
            $g.DrawImage($atlas, $dest, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
        }
    }
    finally {
        $atlas.Dispose()
        $g.Dispose()
    }
    return @{ bmp = $bmp; scale = $Scale }
}

# ---------------------------------------------------------------------------
$results = @()
foreach ($id in $Ids) {
    $r = Invoke-Preview -Id $id -Scale $K -FitPx $Fit -OffX $Dx -OffY $Dy
    $results += , @{ id = $id; bmp = $r.bmp }
    if (-not $Quiet) { Write-Output ("{0,-16} scale x{1}  {2}x{3}" -f $id, $r.scale, $r.bmp.Width, $r.bmp.Height) }
}

$cols = [Math]::Min(5, [Math]::Max(1, $results.Count))
$rows = [int][Math]::Ceiling($results.Count / $cols)
$cw = 0; $ch = 0
foreach ($r in $results) {
    if ($r.bmp.Width -gt $cw) { $cw = $r.bmp.Width }
    if ($r.bmp.Height -gt $ch) { $ch = $r.bmp.Height }
}
$labelH = 22
$sheetW = $cols * ($cw + 10) + 10
$sheetH = $rows * ($ch + $labelH + 10) + 10
$sheet = New-Object System.Drawing.Bitmap($sheetW, $sheetH)
$sg = [System.Drawing.Graphics]::FromImage($sheet)
$sg.Clear([System.Drawing.ColorTranslator]::FromHtml('#3A3A3E'))
$font = New-Object System.Drawing.Font('Consolas', 13)
$brush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml('#F0F0F0'))
for ($i = 0; $i -lt $results.Count; $i++) {
    $r = $results[$i]
    $cx = ($i % $cols) * ($cw + 10) + 10
    $cy = [int][Math]::Floor($i / $cols) * ($ch + $labelH + 10) + 10
    $sg.DrawImage($r.bmp, $cx, $cy)
    $sg.DrawString($r.id, $font, $brush, ($cx + 2), ($cy + $r.bmp.Height + 2))
}
$sg.Dispose(); $font.Dispose(); $brush.Dispose()
$sheet.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()
foreach ($r in $results) { $r.bmp.Dispose() }
Write-Output ("sheet: {0}" -f $Out)
