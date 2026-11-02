# retro_lib.ps1 - atlas, palette and cube helpers for the retro console generator.
# Dot-sourced by gen_retro.ps1. No side effects on load.

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'gen_lib.ps1')

$script:Project = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$script:RetroAssets = Join-Path $script:Project 'retro\src\main\resources\assets\retro'
$script:RetroData = Join-Path $script:Project 'retro\src\main\resources\data'
$script:GeoDir = Join-Path $script:RetroAssets 'geckolib\models\item'
$script:AnimDir = Join-Path $script:RetroAssets 'geckolib\animations\item'
$script:TexDir = Join-Path $script:RetroAssets 'textures\item'
$script:BlockDir = Join-Path $script:RetroAssets 'blockstates'
$script:ModelBlockDir = Join-Path $script:RetroAssets 'models\block'
$script:ModelItemDir = Join-Path $script:RetroAssets 'models\item'
$script:ItemsDir = Join-Path $script:RetroAssets 'items'
$script:LangDir = Join-Path $script:RetroAssets 'lang'

function Ensure-Dir([string]$path) {
    if (-not (Test-Path $path)) { New-Item -ItemType Directory -Force -Path $path | Out-Null }
}

function Write-Text([string]$path, [string]$text) {
    Ensure-Dir (Split-Path $path -Parent)
    [System.IO.File]::WriteAllText($path, $text, [System.Text.UTF8Encoding]::new($false))
}

function Write-Json([string]$path, $obj) {
    Write-Text $path ($obj | ConvertTo-Json -Depth 40)
}

# ---- atlas painting --------------------------------------------------------
function Paint-Region($bmp, [int]$x, [int]$y, $e) {
    $w = [int]$e.w; $h = [int]$e.h
    $col = C $e.color
    $col2 = if ($e.color2) { C $e.color2 } else { Shift-Col $col -30 }
    $seed = ($x * 7919 + $y * 104729)
    switch ($e.style) {
        'noise' {
            $amp = if ($null -ne $e.amp) { [int]$e.amp } else { 4 }
            Noise-Rect $bmp $x $y $w $h $e.color $amp $seed
        }
        'solid' {
            Fill-Rect $bmp $x $y $w $h $col
        }
        'panel' {
            $amp = if ($null -ne $e.amp) { [int]$e.amp } else { 3 }
            Noise-Rect $bmp $x $y $w $h $e.color $amp $seed
            $edge = Shift-Col $col -35
            HLine $bmp $x $y $w $edge
            VLine $bmp $x $y $h $edge
            VLine $bmp ($x + $w - 1) $y $h $edge
            HLine $bmp $x ($y + $h - 1) $w $edge
            $hi = Shift-Col $col 25
            HLine $bmp ($x + 1) ($y + 1) ($w - 2) $hi
        }
        'slot' {
            Fill-Rect $bmp $x $y $w $h (C '#050507')
            Fill-Rect $bmp ($x + 1) ($y + 1) ($w - 2) ([Math]::Max(1, $h - 2)) (C '#17171C')
            $rim = Shift-Col $col -50
            HLine $bmp $x $y $w $rim
        }
        'label' {
            Fill-Rect $bmp $x $y $w $h $col
            $ink = if ($e.color2) { C $e.color2 } else { C '#202020' }
            $ly = $y + 1
            while ($ly -lt ($y + $h - 1)) {
                HLine $bmp ($x + 1) $ly ([Math]::Max(1, $w - 2)) $ink
                $ly += 2
            }
            HLine $bmp $x $y $w (Shift-Col $col 30)
        }
        'led' {
            Fill-Rect $bmp $x $y $w $h (Shift-Col $col -70)
            Fill-Rect $bmp ($x + 0) ($y + 0) ([Math]::Max(1, $w - 1)) ([Math]::Max(1, $h - 1)) $col
            if ($w -ge 2 -and $h -ge 2) { Put-Px $bmp ($x + 1) ($y + 1) (C '#FFFFFF') }
        }
        'vent' {
            Fill-Rect $bmp $x $y $w $h $col
            $line = if ($e.color2) { C $e.color2 } else { Shift-Col $col 25 }
            for ($ly = $y; $ly -lt ($y + $h); $ly += 2) { HLine $bmp $x $ly $w $line }
        }
        'screen' {
            Fill-Rect $bmp $x $y $w $h (C '#0A0C10')
            # a soft top glow and two faint reflections read as a powered-off glass pane
            HLine $bmp $x $y $w (C '#151A22')
            HLine $bmp $x ($y + 1) $w (C '#11151C')
            for ($i = 2; $i -lt [Math]::Min($w, $h); $i += 5) {
                Put-Px $bmp ($x + $i) ($y + $i) (C '#2A3038')
            }
        }
        'grain' {
            Noise-Rect $bmp $x $y $w $h $e.color 5 $seed
            $line = if ($e.color2) { C $e.color2 } else { Shift-Col $col -25 }
            for ($ly = $y + 1; $ly -lt ($y + $h); $ly += 3) {
                HLine $bmp $x $ly $w (Shift-Col $line 10)
            }
        }
        'button' {
            Fill-Rect $bmp $x $y $w $h (Shift-Col $col -45)
            Fill-Rect $bmp ($x + 1) ($y + 1) ([Math]::Max(1, $w - 2)) ([Math]::Max(1, $h - 2)) $col
            HLine $bmp ($x + 1) ($y + 1) ([Math]::Max(1, $w - 2)) (Shift-Col $col 35)
        }
        'stripe' {
            Noise-Rect $bmp $x $y $w $h $e.color 3 $seed
            $acc = if ($e.color2) { C $e.color2 } else { C '#FFFFFF' }
            for ($i = 0; $i -lt $w; $i++) {
                for ($j = 0; $j -lt $h; $j++) {
                    if ((($i + $j) % 6) -lt 2) { Put-Px $bmp ($x + $i) ($y + $j) $acc }
                }
            }
        }
        default { Noise-Rect $bmp $x $y $w $h $e.color 4 $seed }
    }
}

function Build-Atlas($entries, [int]$size = 64) {
    $bmp = New-Atlas $size $size '#17171C'
    $regions = @{}
    $x = 1; $y = 1; $rowH = 0
    foreach ($e in $entries) {
        $ew = [int]$e.w; $eh = [int]$e.h
        if (($x + $ew + 1) -gt $size) { $x = 1; $y += $rowH + 1; $rowH = 0 }
        if (($y + $eh + 1) -gt $size) { throw "atlas overflow at region $($e.name) ($size x $size)" }
        $regions[$e.name] = @{ x = $x; y = $y; w = $ew; h = $eh }
        Paint-Region $bmp $x $y $e
        $x += $ew + 1
        if ($eh -gt $rowH) { $rowH = $eh }
    }
    return @{ bmp = $bmp; regions = $regions }
}

function Palette-Entries($p) {
    $e = New-Object System.Collections.ArrayList
    $e.Add(@{ name='base';   w=16; h=16; style='noise';  color=$p.base;   amp=5 }) | Out-Null
    $e.Add(@{ name='top';    w=16; h=16; style='noise';  color=$p.light;  amp=4 }) | Out-Null
    $e.Add(@{ name='side';   w=16; h=16; style='noise';  color=$p.base;   amp=5 }) | Out-Null
    $e.Add(@{ name='front';  w=16; h=16; style='panel';  color=$p.panel;  color2=$p.dark; amp=4 }) | Out-Null
    $e.Add(@{ name='back';   w=16; h=16; style='noise';  color=$p.dark;   amp=4 }) | Out-Null
    $e.Add(@{ name='bottom'; w=16; h=16; style='solid';  color=$p.dark }) | Out-Null
    $e.Add(@{ name='panel';  w=16; h=8;  style='panel';  color=$p.panel;  color2=$p.dark; amp=3 }) | Out-Null
    $e.Add(@{ name='slot';   w=8;  h=4;  style='slot';   color='#0A0A0C'; color2='#2A2A30' }) | Out-Null
    $e.Add(@{ name='accent'; w=8;  h=8;  style='noise';  color=$p.accent; amp=4 }) | Out-Null
    $e.Add(@{ name='label';  w=8;  h=4;  style='label';  color=$p.label;  color2=$p.dark }) | Out-Null
    $e.Add(@{ name='vents';  w=8;  h=4;  style='vent';   color=$p.dark;   color2=$p.light }) | Out-Null
    $e.Add(@{ name='button'; w=4;  h=4;  style='button'; color=$p.button; color2=$p.light }) | Out-Null
    $e.Add(@{ name='screen'; w=16; h=12; style='screen'; color=$p.screen; color2=$p.accent }) | Out-Null
    $e.Add(@{ name='led_on'; w=2; h=2; style='led'; color=$p.ledColor }) | Out-Null
    $e.Add(@{ name='led_off'; w=2; h=2; style='led'; color=$p.ledOff }) | Out-Null
    $e.Add(@{ name='grain';  w=16; h=16; style='grain';  color=$p.grain;  color2=$p.dark }) | Out-Null
    return $e.ToArray()
}

# ---- cube helpers ----------------------------------------------------------
function Cube-Body($x, $y, $z, $w, $h, $d) {
    return @{ o=@($x,$y,$z); s=@($w,$h,$d); f=@{
        north='front'; south='back'; east='side'; west='side'; up='top'; down='bottom' } }
}

function Cube-All($x, $y, $z, $w, $h, $d, $r) {
    return @{ o=@($x,$y,$z); s=@($w,$h,$d); f=@{
        north=$r; south=$r; east=$r; west=$r; up=$r; down=$r } }
}

function Cube-Faces($x, $y, $z, $w, $h, $d, $faces) {
    return @{ o=@($x,$y,$z); s=@($w,$h,$d); f=$faces }
}

# short aliases used by the design tables
function B($x, $y, $z, $w, $h, $d) { return Cube-Body $x $y $z $w $h $d }
function D($x, $y, $z, $w, $h, $d, $r) { return Cube-All $x $y $z $w $h $d $r }
function F($x, $y, $z, $w, $h, $r) { return Cube-All $x $y $z $w $h 0.12 $r }
