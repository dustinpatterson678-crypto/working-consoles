# retro_designs_a.ps1 - first eleven Core Pack I designs.

function Get-Designs-A {
    $out = New-Object System.Collections.ArrayList

    # ---- Virtual Boy ------------------------------------------------------
    $d = @{
        id='vb'; name='Virtual Boy'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#8B1A1A'; dark='#3A0808'; light='#B03030'; panel='#5A0F0F'
        accent='#E03030'; label='#F0C0C0'; button='#202020'; screen='#101820'
        ledColor='#FF5050'; ledOff='#401010'; grain='#000000'
    }
    $d.body = @(
        B (-2.5) 0 (-2.5) 5 0.7 5
        B (-1.0) 0.7 (-1.0) 2 3.5 2
        B (-4.0) 4.2 (-2.5) 8 3.6 5
    )
    $d.details = @(
        F (-3.5) 5.4 (-2.56) 7 1.6 'screen'
        F (-3.2) 5.7 (-2.62) 1.9 1.0 'accent'
        F 1.3 5.7 (-2.62) 1.9 1.0 'accent'
        F (-1.2) 4.6 (-2.62) 2.4 0.35 'label'
        F (-3.6) 4.25 (-2.62) 7.2 0.18 'button'
        D 3.2 4.5 (-2.56) 0.5 0.4 0.12 'button'
    )
    $d.ledPos = @(3.5, 4.8, -2.56)
    $out.Add($d) | Out-Null

    # ---- PC Engine --------------------------------------------------------
    $d = @{
        id='pce'; name='PC Engine'; mediaModel='card'; mediaWord='HuCard'
        base='#E8E8E0'; dark='#8A8A80'; light='#FFFFFF'; panel='#D8D8D0'
        accent='#3A5AC0'; label='#202020'; button='#303030'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-4.5) 0 (-4.0) 9 2.1 8
        B (-4.1) 2.1 (-3.6) 8.2 0.5 7.2
    )
    $d.details = @(
        F (-3.8) 0.35 (-4.06) 7.6 1.3 'panel'
        D (-2.2) 2.6 (-1.2) 4.4 0.35 2.4 'slot'
        F (-1.6) 0.7 (-4.12) 3.2 0.5 'label'
        F 2.0 0.5 (-4.12) 2.2 0.8 'vents'
        F (-3.4) 0.6 (-4.12) 0.7 0.7 'button'
        F (-2.5) 0.6 (-4.12) 0.7 0.7 'button'
    )
    $d.ledPos = @(3.6, 1.0, -4.12)
    $out.Add($d) | Out-Null

    # ---- Neo Geo AES ------------------------------------------------------
    $d = @{
        id='neogeo'; name='Neo Geo AES'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#1A1A1C'; dark='#08080A'; light='#3A3A40'; panel='#2A2A2E'
        accent='#D0A020'; label='#F0D060'; button='#404040'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-5.5) 0 (-4.5) 11 2.8 9
        B (-5.1) 2.8 (-4.1) 10.2 0.6 8.2
    )
    $d.details = @(
        F (-4.8) 0.4 (-4.56) 9.6 1.8 'panel'
        D (-2.5) 3.4 (-1.2) 5 0.35 2.4 'slot'
        F (-2.2) 0.8 (-4.62) 4.4 0.7 'label'
        F 2.2 0.6 (-4.62) 2.0 0.5 'accent'
        F 2.4 1.3 (-4.62) 1.6 0.6 'vents'
        F (-4.4) 0.5 (-4.62) 0.8 0.5 'slot'
        F (-3.2) 0.5 (-4.62) 0.8 0.5 'slot'
        F (-1.0) 0.5 (-4.62) 0.8 0.5 'button'
    )
    $d.ledPos = @(4.4, 1.4, -4.62)
    $out.Add($d) | Out-Null

    # ---- Neo Geo Pocket Color --------------------------------------------
    $d = @{
        id='ngp'; name='Neo Geo Pocket Color'; mediaModel='card'; mediaWord='card'
        base='#C8D0D8'; dark='#586068'; light='#F0F4F8'; panel='#3A4048'
        accent='#3070C0'; label='#202020'; button='#404040'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-2.5) 0 (-1.5) 5 0.5 3
        B (-2.5) 0.5 (-0.9) 5 6.5 1.8
    )
    $d.details = @(
        F (-1.9) 3.6 (-0.96) 3.8 2.6 'screen'
        D (-2.0) 1.5 (-1.02) 1.2 1.2 0.12 'button'
        D 0.7 1.5 (-1.02) 0.6 0.6 0.12 'button'
        D 1.5 1.5 (-1.02) 0.6 0.6 0.12 'button'
        F (-1.5) 0.8 (-1.02) 3.0 0.3 'label'
        F 1.3 0.8 (-1.02) 0.8 0.8 'vents'
    )
    $d.ledPos = @(1.9, 0.9, -1.02)
    $out.Add($d) | Out-Null

    # ---- WonderSwan Color ------------------------------------------------
    $d = @{
        id='wonderswan'; name='WonderSwan Color'; mediaModel='card'; mediaWord='card'
        base='#D8D8D0'; dark='#707068'; light='#FFFFFF'; panel='#404040'
        accent='#C03030'; label='#202020'; button='#303030'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-3.0) 0 (-1.3) 6 0.5 2.6
        B (-3.2) 0.5 (-0.75) 6.4 4.5 1.5
    )
    $d.details = @(
        F (-2.3) 2.4 (-0.81) 4.6 2.2 'screen'
        D (-2.6) 1.1 (-0.87) 1.0 1.0 0.12 'button'
        D 1.4 1.1 (-0.87) 0.6 0.6 0.12 'button'
        D 2.1 1.1 (-0.87) 0.6 0.6 0.12 'button'
        F (-1.8) 0.7 (-0.87) 3.6 0.3 'label'
    )
    $d.ledPos = @(2.6, 0.8, -0.87)
    $out.Add($d) | Out-Null

    # ---- Sega 32X ---------------------------------------------------------
    $d = @{
        id='32x'; name='Sega 32X'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#202024'; dark='#0A0A0C'; light='#404048'; panel='#303038'
        accent='#D03030'; label='#E0E0E0'; button='#505058'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-5.0) 0 (-4.0) 10 2.4 8
        B (-4.6) 2.4 (-3.6) 9.2 0.6 7.2
    )
    $d.details = @(
        F (-4.2) 0.4 (-4.06) 8.4 1.5 'panel'
        D (-2.5) 3.0 (-1.2) 5 0.35 2.4 'slot'
        F (-2.4) 0.9 (-4.12) 3.2 0.6 'accent'
        F 1.2 0.7 (-4.12) 2.4 0.8 'vents'
        F (-4.0) 0.6 (-4.12) 0.7 0.7 'button'
        F (-3.1) 0.6 (-4.12) 0.7 0.7 'button'
    )
    $d.ledPos = @(4.0, 1.0, -4.12)
    $out.Add($d) | Out-Null

    # ---- Sega CD ----------------------------------------------------------
    $d = @{
        id='segacd'; name='Sega CD'; mediaModel='disc'; mediaWord='disc'
        base='#18181C'; dark='#08080A'; light='#383840'; panel='#282830'
        accent='#A0A0A8'; label='#D0D0D8'; button='#404048'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-6.0) 0 (-4.5) 12 1.6 9
        B (-5.6) 1.6 (-4.1) 11.2 0.2 8.2
    )
    $d.details = @(
        D (-3.5) 1.8 (-2.5) 7 0.15 5 'slot'
        F (-5.2) 0.35 (-4.56) 10.4 0.8 'panel'
        F (-2.2) 0.55 (-4.62) 4.4 0.4 'label'
        F 2.6 0.55 (-4.62) 1.6 0.4 'accent'
        F (-5.0) 1.0 (-4.62) 1.0 1.0 'vents'
    )
    $d.ledPos = @(4.8, 0.8, -4.62)
    $out.Add($d) | Out-Null

    # ---- Atari 2600 -------------------------------------------------------
    $d = @{
        id='atari2600'; name='Atari 2600'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#6A4A2A'; dark='#2A1A0A'; light='#8A6A3A'; panel='#3A2A1A'
        accent='#C0C0C0'; label='#D0D0D0'; button='#303030'; screen='#101820'
        ledColor='#FF4040'; ledOff='#401010'; grain='#5A3A1A'
    }
    $d.body = @(
        Cube-Faces (-6.0) 0 (-4.5) 12 2.4 9 @{ north='grain'; south='grain'; east='grain'; west='grain'; up='back'; down='bottom' }
        D (-5.5) 2.4 (-4.0) 11 0.6 8 'back'
    )
    $d.details = @(
        F (-5.0) 0.4 (-4.56) 10 1.3 'grain'
        D (-3.0) 3.0 (-1.2) 6 0.35 2.2 'slot'
        D (-4.4) 0.6 (-4.62) 0.7 0.5 0.12 'button'
        D (-3.4) 0.6 (-4.62) 0.7 0.5 0.12 'button'
        D (-2.4) 0.6 (-4.62) 0.7 0.5 0.12 'button'
        D (-1.4) 0.6 (-4.62) 0.7 0.5 0.12 'button'
        F (-5.2) 2.4 (-4.3) 10.4 0.35 'grain'
        F 2.2 0.7 (-4.62) 2.4 0.6 'vents'
    )
    $d.ledPos = @(4.6, 1.0, -4.62)
    $out.Add($d) | Out-Null

    # ---- Atari 5200 -------------------------------------------------------
    $d = @{
        id='atari5200'; name='Atari 5200'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#18181C'; dark='#08080A'; light='#C0C0C8'; panel='#282830'
        accent='#C0C0C8'; label='#202020'; button='#505058'; screen='#101820'
        ledColor='#FF4040'; ledOff='#401010'; grain='#000000'
    }
    $d.body = @(
        B (-6.0) 0 (-4.5) 12 2.8 9
        D (-5.5) 2.8 (-4.0) 11 0.6 8 'top'
    )
    $d.details = @(
        F (-5.2) 0.4 (-4.56) 10.4 1.5 'panel'
        D (-3.0) 3.4 (-1.2) 6 0.35 2.2 'slot'
        F (-4.4) 0.6 (-4.62) 0.8 0.5 'slot'
        F (-3.2) 0.6 (-4.62) 0.8 0.5 'slot'
        F (-2.0) 0.6 (-4.62) 0.8 0.5 'button'
        F 2.4 0.6 (-4.62) 2.2 0.8 'vents'
        F (-5.0) 2.4 (-4.62) 10 0.25 'accent'
    )
    $d.ledPos = @(4.8, 1.2, -4.62)
    $out.Add($d) | Out-Null

    # ---- Atari 7800 -------------------------------------------------------
    $d = @{
        id='atari7800'; name='Atari 7800'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#18181C'; dark='#08080A'; light='#808088'; panel='#282830'
        accent='#C0C0C8'; label='#D0D0D0'; button='#505058'; screen='#101820'
        ledColor='#FF4040'; ledOff='#401010'; grain='#000000'
    }
    $d.body = @(
        B (-6.0) 0 (-4.5) 12 2.6 9
        B (-5.6) 2.6 (-4.1) 11.2 0.5 8.2
    )
    $d.details = @(
        F (-5.2) 0.35 (-4.56) 10.4 1.4 'panel'
        D (-3.0) 3.1 (-1.2) 6 0.35 2.2 'slot'
        F (-4.6) 0.6 (-4.62) 1.0 0.5 'button'
        F (-3.4) 0.6 (-4.62) 1.0 0.5 'button'
        F 2.6 0.6 (-4.62) 2.0 0.7 'vents'
        F (-5.0) 2.3 (-4.62) 10 0.25 'accent'
    )
    $d.ledPos = @(4.8, 1.0, -4.62)
    $out.Add($d) | Out-Null

    # ---- Atari Lynx -------------------------------------------------------
    $d = @{
        id='lynx'; name='Atari Lynx'; mediaModel='card'; mediaWord='card'
        base='#A0A0A8'; dark='#505058'; light='#D8D8E0'; panel='#303038'
        accent='#C03030'; label='#E0E0E0'; button='#303030'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-3.0) 0 (-1.5) 6 0.5 3
        B (-4.5) 0.5 (-1.1) 9 3.8 2.2
    )
    $d.details = @(
        F (-3.0) 1.6 (-1.16) 6.0 2.2 'screen'
        D (-4.0) 0.9 (-1.22) 1.4 1.4 0.12 'button'
        D 2.7 0.9 (-1.22) 0.7 0.7 0.12 'button'
        D 3.5 0.9 (-1.22) 0.7 0.7 0.12 'button'
        F (-1.5) 0.7 (-1.22) 3.0 0.3 'label'
        F 1.6 0.7 (-1.22) 0.9 0.9 'vents'
    )
    $d.ledPos = @(3.9, 0.8, -1.22)
    $out.Add($d) | Out-Null

    return $out.ToArray()
}
