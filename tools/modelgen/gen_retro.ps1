# gen_retro.ps1 - generates all assets for the retro module (Core Pack I, batch 2).
# Usage:
#   powershell -ExecutionPolicy Bypass -File tools/modelgen/gen_retro.ps1
# Preview:
#   powershell -ExecutionPolicy Bypass -File tools/modelgen/preview.ps1 `
#     -AssetDir ..\..\retro\src\main\resources\assets\retro -Out retro_preview.png

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'retro_lib.ps1')
. (Join-Path $PSScriptRoot 'retro_output.ps1')
. (Join-Path $PSScriptRoot 'retro_designs_a.ps1')
. (Join-Path $PSScriptRoot 'retro_designs_b.ps1')

foreach ($dir in @($GeoDir, $AnimDir, $TexDir, $BlockDir, $ModelBlockDir, $ModelItemDir, $ItemsDir, $LangDir)) {
    Ensure-Dir $dir
}

function Media-Id($p) {
    switch ($p.mediaModel) {
        'disc' { return "$($p.id)_disc" }
        'card' { return "$($p.id)_card" }
        default { return "$($p.id)_cart" }
    }
}

Write-Host 'media models...'
Write-MediaAssets 'cartridge' 7.5 8.5 1.2 90 102
Write-MediaAssets 'card' 6.0 8.0 0.3 96 72
Write-MediaAssets 'disc' 8.0 8.0 0.5 108 108

$designs = @(Get-Designs-A) + @(Get-Designs-B)
$lang = [ordered]@{ 'itemGroup.retro.main' = 'Retro Consoles' }
$consoles = New-Object System.Collections.ArrayList
$mediaItems = New-Object System.Collections.ArrayList

foreach ($p in $designs) {
    $p.mediaId = Media-Id $p
    Write-Host ("  {0} ({1})" -f $p.id, $p.name)
    $atlas = Build-Atlas (Palette-Entries $p) 64
    Write-ConsoleAssets $p $atlas $p
    $atlas.bmp.Dispose()
    Write-Data $p
    $lang["block.retro.$($p.id)"] = $p.name
    $lang["item.retro.$($p.id)"] = $p.name
    $lang["item.retro.$($p.mediaId)"] = "$($p.name) $($p.mediaWord)"
    $consoles.Add("retro:$($p.id)") | Out-Null
    $mediaItems.Add("retro:$($p.mediaId)") | Out-Null
}

# ---- manual ---------------------------------------------------------------
$manualRecipe = [ordered]@{
    type = 'minecraft:crafting_shapeless'
    category = 'misc'
    ingredients = @('minecraft:book', $mediaItems[0])
    result = [ordered]@{
        id = 'wcore:manual'
        count = 1
        components = [ordered]@{
            'minecraft:custom_data' = [ordered]@{ manual = 'retro' }
            'minecraft:item_name' = 'Retro Consoles Manual'
        }
    }
}
Write-Json (Join-Path $RetroData 'retro\recipe\manual.json') $manualRecipe

# ---- tags -----------------------------------------------------------------
$tagDir = Join-Path $RetroData 'wcore\tags\item'
Ensure-Dir $tagDir
Write-Json (Join-Path $tagDir 'consoles.json') ([ordered]@{ replace = $false; values = $consoles.ToArray() })
Write-Json (Join-Path $tagDir 'game_cases.json') ([ordered]@{ replace = $false; values = $mediaItems.ToArray() })

# ---- lang -----------------------------------------------------------------
Write-Json (Join-Path $LangDir 'en_us.json') $lang

Write-Host ("generated {0} consoles, {1} media items" -f $consoles.Count, $mediaItems.Count)
Write-Host 'preview with: powershell -File tools/modelgen/preview.ps1 -AssetDir ..\..\retro\src\main\resources\assets\retro -Out retro_preview.png'
