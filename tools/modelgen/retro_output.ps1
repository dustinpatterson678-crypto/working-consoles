# retro_output.ps1 - asset writers for the retro console generator.

function Console-Model([string]$tex) {
@"
{
 "textures": { "particle": "$tex" },
 "gui_light": "front",
 "display": {
  "gui": { "rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.55, 0.55, 0.55] },
  "fixed": { "rotation": [0, 180, 0], "scale": [1, 1, 1] },
  "ground": { "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5] },
  "firstperson_righthand": { "rotation": [0, 160, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6] },
  "firstperson_lefthand": { "rotation": [0, 200, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6] },
  "thirdperson_righthand": { "rotation": [75, 180, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5] },
  "thirdperson_lefthand": { "rotation": [75, 180, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5] }
 }
}
"@
}

function Media-Model([string]$tex) {
@"
{
 "textures": { "particle": "$tex" },
 "gui_light": "front",
 "display": {
  "gui": { "rotation": [10, 160, 0], "scale": [1, 1, 1] },
  "fixed": { "rotation": [0, 180, 0], "scale": [1, 1, 1] },
  "ground": { "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5] },
  "firstperson_righthand": { "rotation": [0, 200, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6] },
  "firstperson_lefthand": { "rotation": [0, 160, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6] },
  "thirdperson_righthand": { "rotation": [0, 180, 0], "translation": [0, 2, 1], "scale": [0.45, 0.45, 0.45] },
  "thirdperson_lefthand": { "rotation": [0, 180, 0], "translation": [0, 2, 1], "scale": [0.45, 0.45, 0.45] }
 }
}
"@
}

function Item-Def([string]$base) {
@"
{
 "model": {
  "type": "minecraft:special",
  "base": "$base",
  "model": { "type": "geckolib:geckolib" }
 }
}
"@
}

function Blockstate([string]$model) {
@"
{ "variants": { "": { "model": "$model" } } }
"@
}

function Block-Model([string]$tex) {
@"
{ "textures": { "particle": "$tex" } }
"@
}

function Write-Anim([string]$path) {
    $anim = [ordered]@{
        format_version = '1.8.0'
        animations = [ordered]@{
            idle = [ordered]@{ loop = $true; animation_length = 1; bones = [ordered]@{} }
            insert = [ordered]@{
                animation_length = 0.25
                bones = [ordered]@{ console = [ordered]@{ position = [ordered]@{
                    '0.0'  = [ordered]@{ vector = @(0, 0, 0) }
                    '0.1'  = [ordered]@{ vector = @(0, -0.3, 0) }
                    '0.25' = [ordered]@{ vector = @(0, 0, 0) }
                } } }
            }
            eject = [ordered]@{
                animation_length = 0.25
                bones = [ordered]@{ console = [ordered]@{ position = [ordered]@{
                    '0.0'  = [ordered]@{ vector = @(0, 0, 0) }
                    '0.1'  = [ordered]@{ vector = @(0, -0.3, 0) }
                    '0.25' = [ordered]@{ vector = @(0, 0, 0) }
                } } }
            }
            shake = [ordered]@{
                animation_length = 0.7
                bones = [ordered]@{ console = [ordered]@{ position = [ordered]@{
                    '0.0'  = [ordered]@{ vector = @(0, 0, 0) }
                    '0.08' = [ordered]@{ vector = @(0.5, 0, 0) }
                    '0.16' = [ordered]@{ vector = @(-0.5, 0, 0) }
                    '0.24' = [ordered]@{ vector = @(0.35, 0, 0) }
                    '0.32' = [ordered]@{ vector = @(-0.35, 0, 0) }
                    '0.40' = [ordered]@{ vector = @(0.2, 0, 0) }
                    '0.48' = [ordered]@{ vector = @(-0.2, 0, 0) }
                    '0.56' = [ordered]@{ vector = @(0.1, 0, 0) }
                    '0.64' = [ordered]@{ vector = @(-0.1, 0, 0) }
                    '0.70' = [ordered]@{ vector = @(0, 0, 0) }
                } } }
            }
        }
    }
    Write-Json $path $anim
}

function Write-ConsoleAssets($p, $atlas, $design) {
    $id = $p.id
    $body = $design.body
    $details = $design.details
    $lx = [double]$design.ledPos[0]; $ly = [double]$design.ledPos[1]; $lz = [double]$design.ledPos[2]
    $ledCubeOn = Cube-All ($lx - 0.25) ($ly - 0.25) $lz 0.5 0.5 0.12 'led_on'
    $ledCubeOff = Cube-All ($lx - 0.25) ($ly - 0.25) $lz 0.5 0.5 0.12 'led_off'
    $bones = @(
        @{ name='console'; pivot=@(0, 0, 0); cubes=$body },
        @{ name='led_on'; pivot=@($lx, $ly, $lz); parent='console'; cubes=@($ledCubeOn) },
        @{ name='led_standby'; pivot=@($lx, $ly, $lz); parent='console'; cubes=@($ledCubeOff) }
    )
    if ($details.Count -gt 0) {
        $bones = @(
            $bones[0],
            @{ name='details'; pivot=@(0, 0, 0); parent='console'; cubes=$details },
            $bones[1],
            $bones[2]
        )
    }

    $geo = ConvertTo-GeoJson "geometry.$id" 64 64 3 3 @(0, 1, 0) $bones $atlas.regions
    Write-Text (Join-Path $GeoDir "$id.geo.json") $geo
    Save-Atlas $atlas.bmp (Join-Path $TexDir "$id.png")
    Write-Anim (Join-Path $AnimDir "$id.animation.json")
    Write-Text (Join-Path $BlockDir "$id.json") (Blockstate "retro:block/$id")
    Write-Text (Join-Path $ModelBlockDir "$id.json") (Block-Model "retro:item/$id")
    Write-Text (Join-Path $ModelItemDir "$id.json") (Console-Model "retro:item/$id")
    Write-Text (Join-Path $ItemsDir "$id.json") (Item-Def "retro:item/$id")
}

function Write-MediaAssets([string]$name, [double]$w, [double]$h, [double]$d, [int]$cw, [int]$ch) {
    $bmp = New-Atlas 256 256 '#17171C'
    Fill-Rect $bmp 0 0 $cw $ch (C '#202028')
    $edge = C '#4A4A56'
    HLine $bmp 0 0 $cw $edge
    VLine $bmp 0 0 $ch $edge
    VLine $bmp ($cw - 1) 0 $ch $edge
    HLine $bmp 0 ($ch - 1) $cw $edge
    Fill-Rect $bmp 6 6 ($cw - 12) 10 (C '#30303C')
    Fill-Rect $bmp 6 20 ([Math]::Max(1, $cw - 12)) ([Math]::Max(1, $ch - 34)) (C '#1A1A22')
    Noise-Rect $bmp 160 0 32 32 '#3A3A44' 4 7
    Noise-Rect $bmp 160 34 32 32 '#202028' 4 11
    $regions = @{
        cover = @{ x = 0; y = 0; w = $cw; h = $ch }
        edge  = @{ x = 160; y = 0; w = 32; h = 32 }
        back  = @{ x = 160; y = 34; w = 32; h = 32 }
    }
    $cube = Cube-Faces (-$w / 2) (-$h / 2) (-$d / 2) $w $h $d @{
        north='cover'; south='back'; east='edge'; west='edge'; up='edge'; down='edge' }
    $bones = @(@{ name='case'; pivot=@(0, 0, 0); cubes=@($cube) })
    $geo = ConvertTo-GeoJson "geometry.$name" 256 256 2 2 @(0, 0, 0) $bones $regions
    Write-Text (Join-Path $GeoDir "$name.geo.json") $geo
    Save-Atlas $bmp (Join-Path $TexDir "$name.png")
    $bmp.Dispose()
    Write-Text (Join-Path $ModelItemDir "$name.json") (Media-Model "retro:item/$name")
    Write-Text (Join-Path $ItemsDir "$name.json") (Item-Def "retro:item/$name")
}

function Write-Data($p) {
    $id = $p.id
    $mediaId = $p.mediaId
    $recipeDir = Join-Path $script:RetroData 'retro\recipe'
    $lootDir = Join-Path $script:RetroData 'retro\loot_table\blocks'
    $tagDir = Join-Path $script:RetroData 'wcore\tags\item'
    Ensure-Dir $recipeDir; Ensure-Dir $lootDir; Ensure-Dir $tagDir

    $recipe = [ordered]@{
        type = 'minecraft:crafting_shaped'
        category = 'misc'
        pattern = @('III', 'RCR', 'SSS')
        key = [ordered]@{
            I = 'minecraft:iron_ingot'
            R = 'minecraft:redstone'
            C = 'minecraft:copper_ingot'
            S = 'minecraft:smooth_stone_slab'
        }
        result = [ordered]@{ id = "retro:$id"; count = 1 }
    }
    Write-Json (Join-Path $recipeDir "$id.json") $recipe

    $dye = switch ($p.mediaModel) {
        'disc' { 'minecraft:gray_dye' }
        'card' { 'minecraft:blue_dye' }
        default { 'minecraft:light_gray_dye' }
    }
    $mediaRecipe = [ordered]@{
        type = 'minecraft:crafting_shapeless'
        category = 'misc'
        ingredients = @('minecraft:iron_ingot', 'minecraft:redstone', 'minecraft:paper', $dye)
        result = [ordered]@{ id = "retro:$mediaId"; count = 2 }
    }
    Write-Json (Join-Path $recipeDir "$mediaId.json") $mediaRecipe
    Write-Text (Join-Path $script:ItemsDir "$mediaId.json") (Item-Def "retro:item/$($p.mediaModel)")

    $loot = [ordered]@{
        type = 'minecraft:block'
        pools = @([ordered]@{
            rolls = 1
            bonus_rolls = 0
            entries = @([ordered]@{ type = 'minecraft:item'; name = "retro:$id" })
            conditions = @([ordered]@{ condition = 'minecraft:survives_explosion' })
        })
    }
    Write-Json (Join-Path $lootDir "$id.json") $loot
}
