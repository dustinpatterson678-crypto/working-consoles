# gen_arcade_items.ps1 - sprites + models for the arcade economy items
# (tokens, tickets, machine snacks and the prize plush).
#
#   powershell -File gen_arcade_items.ps1

. (Join-Path $PSScriptRoot 'gen_lib.ps1')

$assets = Join-Path $PSScriptRoot '..\..\wcore\src\main\resources\assets\wcore'
$texOut = Join-Path $assets 'textures\item'
$modelOut = Join-Path $assets 'models\item'
$defOut = Join-Path $assets 'items'
New-Item -ItemType Directory -Force -Path $texOut | Out-Null
New-Item -ItemType Directory -Force -Path $modelOut | Out-Null
New-Item -ItemType Directory -Force -Path $defOut | Out-Null

function Save-Item([string]$name, [scriptblock]$paint) {
    $bmp = New-Object System.Drawing.Bitmap(16, 16)
    & $paint $bmp
    $path = Join-Path $texOut "$name.png"
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    $model = [ordered]@{
        parent   = 'minecraft:item/generated'
        textures = [ordered]@{ layer0 = "wcore:item/$name" }
    }
    $json = $model | ConvertTo-Json -Depth 6
    # 1.21.4+ also needs the client item definition in assets/<ns>/items/.
    # Without it the item has no model and renders as the purple/black
    # checkerboard in the creative tab and in hand.
    $def = [ordered]@{
        model = [ordered]@{
            type  = 'minecraft:model'
            model = "wcore:item/$name"
        }
    }
    $defJson = $def | ConvertTo-Json -Depth 6
    # UTF-8 *without* BOM - Minecraft's JSON parser rejects a leading BOM.
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText((Join-Path $modelOut "$name.json"), $json, $utf8)
    [System.IO.File]::WriteAllText((Join-Path $defOut "$name.json"), $defJson, $utf8)
    Write-Output "$name  [OK]"
}

function Paint-Token($b) {
    $outline = C '#7a4a08'
    $gold = C '#ffcf33'
    $hi = C '#ffe98a'
    $dark = C '#b7791a'
    # coin body (rounded 12x12)
    Fill-Rect $b 4 2 8 1 $outline
    Fill-Rect $b 3 3 10 1 $outline
    Fill-Rect $b 2 4 12 8 $outline
    Fill-Rect $b 3 12 10 1 $outline
    Fill-Rect $b 4 13 8 1 $outline
    Fill-Rect $b 4 3 8 1 $gold
    Fill-Rect $b 3 4 10 8 $gold
    Fill-Rect $b 4 12 8 1 $dark
    # rim highlight
    HLine $b 5 4 6 $hi
    VLine $b 4 5 5 $hi
    # star
    $s = C '#8a5a12'
    HLine $b 7 5 2 $s
    HLine $b 6 6 4 $s
    HLine $b 5 7 6 $s
    HLine $b 6 8 4 $s
    HLine $b 7 9 2 $s
    HLine $b 6 10 4 $s
    Put-Px $b 4 10 $s
    Put-Px $b 11 10 $s
}

function Paint-Ticket($b) {
    $edge = C '#5c1038'
    $face = C '#ff3ca6'
    $hi = C '#ff8fd0'
    $ink = C '#fff2d0'
    # ticket body with notched sides
    Fill-Rect $b 2 4 12 8 $edge
    Fill-Rect $b 3 5 10 6 $face
    HLine $b 3 5 10 $hi
    VLine $b 3 6 4 $hi
    # side notches
    Fill-Rect $b 2 6 1 2 $edge
    Fill-Rect $b 13 6 1 2 $edge
    # perforation
    for ($y = 6; $y -lt 10; $y += 2) { Put-Px $b 8 $y $ink }
    # star + text dashes
    Put-Px $b 5 7 $ink; Put-Px $b 6 7 $ink; Put-Px $b 7 7 $ink
    Put-Px $b 9 8 $ink; Put-Px $b 10 8 $ink
    Put-Px $b 5 9 $ink; Put-Px $b 6 9 $ink
}

function Paint-Soda($b) {
    $edge = C '#5a0d12'
    $red = C '#e8333f'
    $hi = C '#ff8a90'
    $silver = C '#c9ced8'
    # can
    Fill-Rect $b 4 2 8 13 $edge
    Fill-Rect $b 5 3 6 11 $red
    Fill-Rect $b 5 2 6 1 $silver
    Fill-Rect $b 5 13 6 1 $silver
    VLine $b 5 3 11 $hi
    # label band + droplet
    Fill-Rect $b 5 6 6 4 (C '#fff4e0')
    $d = C '#2fb8ff'
    HLine $b 8 6 1 $d
    HLine $b 7 7 3 $d
    HLine $b 7 8 3 $d
    Put-Px $b 8 9 $d
    HLine $b 6 3 4 $silver
    HLine $b 6 12 4 $silver
}

function Paint-Chips($b) {
    $edge = C '#5a3107'
    $bag = C '#ffb31c'
    $hi = C '#ffe07a'
    $red = C '#e8333f'
    # crimped top / bottom
    Fill-Rect $b 4 1 8 2 $edge
    Fill-Rect $b 4 13 8 2 $edge
    Fill-Rect $b 3 3 10 10 $edge
    Fill-Rect $b 4 3 8 10 $bag
    VLine $b 4 3 10 $hi
    # chips peeking top
    for ($x = 5; $x -lt 12; $x += 2) { Fill-Rect $b $x 2 1 2 (C '#ffd970') }
    # logo
    Fill-Rect $b 6 6 4 3 $red
    Put-Px $b 7 7 (C '#fff4e0')
    Put-Px $b 8 7 (C '#fff4e0')
    HLine $b 6 10 4 (C '#7a4a08')
}

function Paint-Candy($b) {
    $edge = C '#5c1038'
    $pink = C '#ff6fae'
    $hi = C '#ffc2dc'
    # wrapper twists
    Fill-Rect $b 1 6 3 4 $edge
    Fill-Rect $b 12 6 3 4 $edge
    HLine $b 2 7 2 $hi
    HLine $b 12 7 2 $hi
    # candy body
    Fill-Rect $b 4 4 8 8 $edge
    Fill-Rect $b 5 5 6 6 $pink
    HLine $b 5 5 6 $hi
    VLine $b 5 6 4 $hi
    # stripe
    for ($i = 0; $i -lt 5; $i++) { Put-Px $b (6 + $i) (9 - $i) (C '#fff4e0') }
}

function Paint-Plush($b) {
    $edge = C '#5c2a3a'
    $fur = C '#f7a8c4'
    $light = C '#ffd6e6'
    $eye = C '#2a1620'
    # ears
    Fill-Rect $b 2 2 4 4 $edge
    Fill-Rect $b 10 2 4 4 $edge
    Fill-Rect $b 3 3 2 2 $light
    Fill-Rect $b 11 3 2 2 $light
    # head
    Fill-Rect $b 3 4 10 8 $edge
    Fill-Rect $b 4 5 8 6 $fur
    # muzzle
    Fill-Rect $b 6 8 4 3 $light
    # eyes + nose
    Fill-Rect $b 5 6 2 2 $eye
    Fill-Rect $b 9 6 2 2 $eye
    Put-Px $b 6 7 (C '#ffffff')
    Put-Px $b 10 7 (C '#ffffff')
    Put-Px $b 7 9 $edge
    Put-Px $b 8 9 $edge
    HLine $b 7 10 2 $edge
    # body hint
    Fill-Rect $b 5 12 6 3 $edge
    Fill-Rect $b 6 12 4 2 $fur
}

Save-Item 'arcade_token' { param($b) Paint-Token $b }
Save-Item 'arcade_ticket' { param($b) Paint-Ticket $b }
Save-Item 'arcade_soda' { param($b) Paint-Soda $b }
Save-Item 'arcade_chips' { param($b) Paint-Chips $b }
Save-Item 'arcade_candy' { param($b) Paint-Candy $b }
Save-Item 'arcade_plush' { param($b) Paint-Plush $b }

Write-Output 'done.'
