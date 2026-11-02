# gen_lib.ps1 - shared helpers for generating GeckoLib models + atlases.
# Dot-sourced by model scripts (gen_crts.ps1 etc). No side effects on load.

Add-Type -AssemblyName System.Drawing

function C([string]$hex) { return [System.Drawing.ColorTranslator]::FromHtml($hex) }
function R2($v) { return [double]([Math]::Round([double]$v, 2)) }
function R3($v) { return [double]([Math]::Round([double]$v, 3)) }

# ---- raw pixel drawing -----------------------------------------------------
function Fill-Rect($bmp, [int]$x, [int]$y, [int]$w, [int]$h, $col) {
    for ($i = 0; $i -lt $w; $i++) { for ($j = 0; $j -lt $h; $j++) { $bmp.SetPixel($x + $i, $y + $j, $col) } }
}
function Put-Px($bmp, [int]$x, [int]$y, $col) { $bmp.SetPixel($x, $y, $col) }
function HLine($bmp, [int]$x, [int]$y, [int]$w, $col) { Fill-Rect $bmp $x $y $w 1 $col }
function VLine($bmp, [int]$x, [int]$y, [int]$h, $col) { Fill-Rect $bmp $x $y 1 $h $col }

function Shift-Col($col, [int]$d) {
    $r = [Math]::Max(0, [Math]::Min(255, $col.R + $d))
    $g = [Math]::Max(0, [Math]::Min(255, $col.G + $d))
    $b = [Math]::Max(0, [Math]::Min(255, $col.B + $d))
    return [System.Drawing.Color]::FromArgb($r, $g, $b)
}

# Deterministic per-rect noise so regenerating gives the same pixels.
function Noise-Rect($bmp, [int]$x, [int]$y, [int]$w, [int]$h, [string]$baseHex, [int]$amp, [int]$seed) {
    $base = C $baseHex
    $rnd = New-Object System.Random($seed)
    for ($j = 0; $j -lt $h; $j++) {
        for ($i = 0; $i -lt $w; $i++) {
            $d = $rnd.Next(-$amp, $amp + 1)
            $bmp.SetPixel($x + $i, $y + $j, (Shift-Col $base $d))
        }
    }
}

# ---- atlas / region bookkeeping -------------------------------------------
function New-Atlas([int]$w, [int]$h, [string]$gapHex = '#554e5e') {
    $bmp = New-Object System.Drawing.Bitmap($w, $h)
    Fill-Rect $bmp 0 0 $w $h (C $gapHex)
    return $bmp
}

function Save-Atlas($bmp, [string]$path) {
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
}

# ---- sampling from the mod's existing textures -----------------------------
function Load-SrcBmp([string]$path) { return [System.Drawing.Bitmap]::FromFile($path) }

# Tiles a patch of source pixels across the destination region (offset by seed).
function Copy-Patch($srcBmp, [int]$sx, [int]$sy, [int]$sw, [int]$sh, $dstBmp, $r, [int]$seed) {
    for ($j = 0; $j -lt $r.h; $j++) {
        for ($i = 0; $i -lt $r.w; $i++) {
            $x = $sx + (($i + $seed) % $sw)
            $y = $sy + (($j + [int]($seed / 3)) % $sh)
            $dstBmp.SetPixel($r.x + $i, $r.y + $j, $srcBmp.GetPixel($x, $y))
        }
    }
}

# Finds the most saturated pixel for a hue in a source rect (exact house colors).
function Find-Color($bmp, [int]$x, [int]$y, [int]$w, [int]$h, [string]$mode) {
    $best = $null; $bestScore = [int]::MinValue
    for ($j = 0; $j -lt $h; $j++) {
        for ($i = 0; $i -lt $w; $i++) {
            $c = $bmp.GetPixel($x + $i, $y + $j)
            $s = 0
            switch ($mode) {
                'red'    { $s = $c.R - [Math]::Max($c.G, $c.B) }
                'green'  { $s = $c.G - [Math]::Max($c.R, $c.B) }
                'blue'   { $s = $c.B - [Math]::Max($c.R, $c.G) }
                'yellow' { $s = [int](($c.R + $c.G) / 2) - $c.B }
                'light'  { $s = [Math]::Min([Math]::Min($c.R, $c.G), $c.B) }
            }
            if ($s -gt $bestScore) { $bestScore = $s; $best = $c }
        }
    }
    return $best
}

# Formats a Color as '#rrggbb' (for painters that take hex strings).
function Color-Hex($c) { return ('#{0:x2}{1:x2}{2:x2}' -f [int]$c.R, [int]$c.G, [int]$c.B) }

# ---- geo json emitting -----------------------------------------------------
$script:FaceCanon = @('north', 'east', 'south', 'west', 'up', 'down')

# $bones: array of @{ name=..; pivot=@(x,y,z); cubes=@( @{ o=@(x,y,z); s=@(x,y,z); f=@{ face='region' } } ) }
# $regions: hashtable name -> @{ x=..; y=..; w=..; h=.. }
function ConvertTo-GeoJson($identifier, [int]$texW, [int]$texH, $boundsW, $boundsH, $boundsOffset, $bones, $regions) {
    $boneList = @()
    foreach ($b in $bones) {
        $cubeList = @()
        foreach ($cb in $b.cubes) {
            $uv = [ordered]@{}
            foreach ($face in $script:FaceCanon) {
                if (-not $cb.f.Contains($face)) { continue }
                $reg = $regions[$cb.f[$face]]
                if (-not $reg) { throw "region not found: $($cb.f[$face])" }
                $uv[$face] = [ordered]@{
                    uv      = @([int]$reg.x, [int]$reg.y)
                    uv_size = @([int]$reg.w, [int]$reg.h)
                }
            }
            $cube = [ordered]@{
                origin = @((R3 $cb.o[0]), (R3 $cb.o[1]), (R3 $cb.o[2]))
                size   = @((R3 $cb.s[0]), (R3 $cb.s[1]), (R3 $cb.s[2]))
                uv     = $uv
            }
            # optional per-cube rotation (bedrock cube format); only when a cube
            # was built with Cu's 4th argument, so older scripts are unaffected
            if ($cb.Contains('r') -and $cb.r) {
                $cube['rotation'] = [ordered]@{
                    origin = @((R3 $cb.r.origin[0]), (R3 $cb.r.origin[1]), (R3 $cb.r.origin[2]))
                    axis   = [string]$cb.r.axis
                    angle  = R3 $cb.r.angle
                }
            }
            $cubeList += , $cube
        }
        $boneList += , [ordered]@{
            name  = $b.name
            pivot = @([double]$b.pivot[0], [double]$b.pivot[1], [double]$b.pivot[2])
            cubes = $cubeList
        }
        if ($b.Contains('parent') -and $b.parent) {
            $boneList[$boneList.Count - 1]['parent'] = [string]$b.parent
        }
    }
    $geo = [ordered]@{
        format_version   = '1.12.0'
        'minecraft:geometry' = @(
            [ordered]@{
                description = [ordered]@{
                    identifier          = $identifier
                    texture_width       = $texW
                    texture_height      = $texH
                    visible_bounds_width  = $boundsW
                    visible_bounds_height = $boundsH
                    visible_bounds_offset = @(0, 1, 0)
                }
                bones = $boneList
            }
        )
    }
    return ($geo | ConvertTo-Json -Depth 30)
}
