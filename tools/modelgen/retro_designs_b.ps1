# retro_designs_b.ps1 - second eleven Core Pack I designs.

function Get-Designs-B {
    $out = New-Object System.Collections.ArrayList

    # ---- Atari Jaguar -----------------------------------------------------
    $d = @{
        id='jaguar'; name='Atari Jaguar'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#18181C'; dark='#08080A'; light='#303038'; panel='#202028'
        accent='#D02020'; label='#D0D0D0'; button='#404048'; screen='#101820'
        ledColor='#FF4040'; ledOff='#401010'; grain='#000000'
    }
    $d.body = @(
        B (-6.0) 0 (-4.5) 12 2.8 9
        B (-5.6) 2.8 (-4.1) 11.2 0.8 8.2
    )
    $d.details = @(
        F (-5.2) 0.4 (-4.56) 10.4 1.6 'panel'
        D (-3.0) 3.6 (-1.2) 6 0.35 2.2 'slot'
        F (-2.6) 0.9 (-4.62) 3.2 0.6 'accent'
        F 1.4 0.7 (-4.62) 2.8 0.8 'vents'
        F (-4.6) 0.6 (-4.62) 1.2 0.5 'button'
        F (-3.2) 0.6 (-4.62) 1.2 0.5 'button'
    )
    $d.ledPos = @(4.8, 1.1, -4.62)
    $out.Add($d) | Out-Null

    # ---- Intellivision ----------------------------------------------------
    $d = @{
        id='intellivision'; name='Intellivision'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#5A3A20'; dark='#2A1A0A'; light='#8A6A40'; panel='#3A2A1A'
        accent='#D0A040'; label='#F0D080'; button='#303030'; screen='#101820'
        ledColor='#FF4040'; ledOff='#401010'; grain='#000000'
    }
    $d.body = @(
        B (-6.0) 0 (-4.5) 12 2.6 9
        B (-5.6) 2.6 (-4.1) 11.2 0.8 8.2
    )
    $d.details = @(
        F (-5.2) 0.4 (-4.56) 10.4 1.4 'accent'
        D (-3.0) 3.4 (-1.2) 6 0.35 2.2 'slot'
        F (-2.4) 0.8 (-4.62) 4.8 0.5 'label'
        F 2.4 0.6 (-4.62) 2.2 0.8 'vents'
        F (-4.6) 0.6 (-4.62) 1.0 0.6 'button'
        F (-3.2) 0.6 (-4.62) 1.0 0.6 'button'
        F (-5.0) 2.3 (-4.62) 10 0.22 'accent'
    )
    $d.ledPos = @(4.8, 1.0, -4.62)
    $out.Add($d) | Out-Null

    # ---- ColecoVision -----------------------------------------------------
    $d = @{
        id='colecovision'; name='ColecoVision'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#18181C'; dark='#08080A'; light='#404048'; panel='#303038'
        accent='#E0E0E8'; label='#3070C0'; button='#303030'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-5.5) 0 (-4.5) 11 2.6 9
        B (-5.1) 2.6 (-4.1) 10.2 0.7 8.2
    )
    $d.details = @(
        F (-4.8) 0.35 (-4.56) 9.6 1.4 'panel'
        D (-2.5) 3.3 (-1.2) 5 0.35 2.2 'slot'
        F (-2.2) 0.75 (-4.62) 4.4 0.5 'label'
        F 2.2 0.55 (-4.62) 2.4 0.8 'vents'
        F (-4.4) 0.55 (-4.62) 1.0 0.6 'button'
        F (-3.2) 0.55 (-4.62) 1.0 0.6 'button'
        F (-4.8) 2.3 (-4.62) 9.6 0.22 'accent'
    )
    $d.ledPos = @(4.4, 1.0, -4.62)
    $out.Add($d) | Out-Null

    # ---- Magnavox Odyssey 2 -----------------------------------------------
    $d = @{
        id='odyssey2'; name='Magnavox Odyssey 2'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#202024'; dark='#08080A'; light='#A0A0A8'; panel='#303038'
        accent='#C0C0C8'; label='#D0D0D8'; button='#505058'; screen='#101820'
        ledColor='#FF4040'; ledOff='#401010'; grain='#000000'
    }
    $d.body = @(
        B (-5.5) 0 (-4.5) 11 2.4 9
        B (-5.1) 2.4 (-4.1) 10.2 0.6 8.2
    )
    $d.details = @(
        F (-4.8) 0.35 (-4.56) 9.6 1.2 'panel'
        D (-2.5) 3.0 (-1.2) 5 0.35 2.2 'slot'
        D (-4.4) 3.1 (-3.4) 0.7 0.25 0.7 'button'
        D (-3.4) 3.1 (-3.4) 0.7 0.25 0.7 'button'
        D (-2.4) 3.1 (-3.4) 0.7 0.25 0.7 'button'
        D (-1.4) 3.1 (-3.4) 0.7 0.25 0.7 'button'
        D (-4.4) 3.1 (-2.4) 0.7 0.25 0.7 'button'
        D (-3.4) 3.1 (-2.4) 0.7 0.25 0.7 'button'
        D (-2.4) 3.1 (-2.4) 0.7 0.25 0.7 'button'
        D (-1.4) 3.1 (-2.4) 0.7 0.25 0.7 'button'
        F (-2.4) 0.7 (-4.62) 4.8 0.4 'label'
        F 2.2 0.6 (-4.62) 2.0 0.7 'vents'
    )
    $d.ledPos = @(4.6, 1.0, -4.62)
    $out.Add($d) | Out-Null

    # ---- Vectrex (built-in screen) ----------------------------------------
    $d = @{
        id='vectrex'; name='Vectrex'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#18181C'; dark='#08080A'; light='#303038'; panel='#202028'
        accent='#40FF60'; label='#D0D0D8'; button='#303030'; screen='#102010'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-4.0) 0 (-3.5) 8 1.2 7
        B (-4.0) 1.2 (-3.0) 8 6.0 1.5
        D (-5.0) 1.2 (-2.5) 1.0 5.0 1.2 'back'
        D 4.0 1.2 (-2.5) 1.0 5.0 1.2 'back'
    )
    $d.details = @(
        F (-3.6) 2.8 (-3.06) 7.2 4.8 'back'
        F (-3.0) 3.0 (-3.18) 6.0 4.2 'screen'
        F (-3.6) 1.6 (-3.06) 7.2 0.5 'panel'
        D (-2.2) 1.25 (-4.2) 2.4 0.5 2.0 'panel'
        D (-1.9) 1.75 (-4.0) 0.7 0.35 0.5 'button'
        D (-0.8) 1.75 (-4.0) 0.7 0.35 0.5 'button'
        F (-0.8) 1.45 (-4.22) 1.6 0.25 'label'
        F 1.0 1.45 (-3.06) 1.8 0.4 'vents'
    )
    $d.ledPos = @(3.4, 1.8, -3.06)
    $out.Add($d) | Out-Null

    # ---- Fairchild Channel F ----------------------------------------------
    $d = @{
        id='channelf'; name='Fairchild Channel F'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#B0B0B8'; dark='#505058'; light='#E0E0E8'; panel='#303038'
        accent='#D03030'; label='#202020'; button='#505058'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-5.5) 0 (-4.5) 11 2.6 9
        B (-5.1) 2.6 (-4.1) 10.2 0.6 8.2
    )
    $d.details = @(
        F (-4.8) 0.35 (-4.56) 9.6 1.4 'panel'
        D (-2.5) 3.2 (-1.2) 5 0.35 2.2 'slot'
        F (-4.4) 0.55 (-4.62) 0.9 0.7 'accent'
        F (-3.2) 0.55 (-4.62) 0.9 0.7 'button'
        F (-2.0) 0.55 (-4.62) 0.9 0.7 'button'
        F (-0.8) 0.55 (-4.62) 0.9 0.7 'button'
        F (-2.2) 1.4 (-4.62) 4.4 0.35 'label'
        F 2.2 0.55 (-4.62) 2.2 0.8 'vents'
    )
    $d.ledPos = @(4.4, 1.0, -4.62)
    $out.Add($d) | Out-Null

    # ---- Watara Supervision -----------------------------------------------
    $d = @{
        id='supervision'; name='Watara Supervision'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#A0A0A8'; dark='#505058'; light='#D8D8E0'; panel='#303038'
        accent='#C03030'; label='#E0E0E0'; button='#303030'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-2.5) 0 (-1.0) 5 0.4 2.0
        B (-2.8) 0.4 (-0.7) 5.6 4.5 1.4
    )
    $d.details = @(
        F (-2.0) 2.3 (-0.76) 4.0 2.0 'screen'
        D (-2.2) 1.0 (-0.82) 1.0 1.0 0.12 'button'
        D 1.2 1.0 (-0.82) 0.6 0.6 0.12 'button'
        D 1.9 1.0 (-0.82) 0.6 0.6 0.12 'button'
        F (-1.6) 0.6 (-0.82) 3.2 0.3 'label'
    )
    $d.ledPos = @(2.3, 0.7, -0.82)
    $out.Add($d) | Out-Null

    # ---- Mega Duck --------------------------------------------------------
    $d = @{
        id='megaduck'; name='Mega Duck'; mediaModel='cartridge'; mediaWord='cartridge'
        base='#A0B0A0'; dark='#405040'; light='#D0E0D0'; panel='#303830'
        accent='#3070C0'; label='#202020'; button='#404840'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-3.0) 0 (-1.2) 6 0.4 2.4
        B (-3.2) 0.4 (-0.8) 6.4 4.8 1.6
    )
    $d.details = @(
        F (-2.4) 2.4 (-0.86) 4.8 2.2 'screen'
        D (-2.6) 1.0 (-0.92) 1.1 1.1 0.12 'button'
        D 1.3 1.0 (-0.92) 0.6 0.6 0.12 'button'
        D 2.1 1.0 (-0.92) 0.6 0.6 0.12 'button'
        F (-1.8) 0.6 (-0.92) 3.6 0.3 'label'
        F 1.7 0.6 (-0.92) 0.7 0.7 'vents'
    )
    $d.ledPos = @(2.7, 0.7, -0.92)
    $out.Add($d) | Out-Null

    # ---- Pokemon Mini -----------------------------------------------------
    $d = @{
        id='pokemonmini'; name='Pokemon Mini'; mediaModel='card'; mediaWord='card'
        base='#E8E8E8'; dark='#808080'; light='#FFFFFF'; panel='#404040'
        accent='#E0C020'; label='#202020'; button='#505050'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-1.8) 0 (-0.8) 3.6 0.3 1.6
        B (-2.0) 0.3 (-0.6) 4.0 3.8 1.2
    )
    $d.details = @(
        F (-1.4) 2.0 (-0.66) 2.8 1.6 'screen'
        D (-1.5) 0.8 (-0.72) 0.8 0.8 0.12 'button'
        D 0.8 0.8 (-0.72) 0.5 0.5 0.12 'button'
        F (-1.2) 0.45 (-0.72) 2.4 0.25 'label'
    )
    $d.ledPos = @(1.6, 0.55, -0.72)
    $out.Add($d) | Out-Null

    # ---- Game & Watch -----------------------------------------------------
    $d = @{
        id='gameandwatch'; name='Game & Watch'; mediaModel='card'; mediaWord='game card'
        base='#C0C0C8'; dark='#606068'; light='#F0F0F8'; panel='#303038'
        accent='#D03030'; label='#202020'; button='#404048'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-2.2) 0 (-0.9) 4.4 0.3 1.8
        B (-2.5) 0.3 (-0.7) 5.0 3.5 1.4
    )
    $d.details = @(
        F (-1.8) 1.9 (-0.76) 3.6 1.6 'screen'
        D (-2.0) 0.8 (-0.82) 0.9 0.9 0.12 'button'
        D 1.1 0.8 (-0.82) 0.6 0.6 0.12 'button'
        F (-1.4) 0.45 (-0.82) 2.8 0.25 'label'
        F 1.2 1.7 (-0.82) 0.5 0.5 'accent'
    )
    $d.ledPos = @(2.1, 0.55, -0.82)
    $out.Add($d) | Out-Null

    # ---- Nintendo DS ------------------------------------------------------
    $d = @{
        id='nds'; name='Nintendo DS'; mediaModel='card'; mediaWord='game card'
        base='#C8C8D0'; dark='#606068'; light='#F0F0F8'; panel='#303038'
        accent='#3070C0'; label='#202020'; button='#404048'; screen='#101820'
        ledColor='#40FF60'; ledOff='#204020'; grain='#000000'
    }
    $d.body = @(
        B (-3.0) 0 (-1.5) 6 0.4 3.0
        B (-3.5) 0.4 (-1.0) 7 5.2 2.0
    )
    $d.details = @(
        F (-3.2) 1.0 (-1.06) 6.4 1.2 'panel'
        F (-1.4) 2.6 (-1.06) 2.8 0.5 'label'
        D (-3.4) 3.6 (-1.06) 6.8 0.2 0.12 'back'
        D (-2.6) 0.6 (-1.06) 0.6 0.6 0.12 'button'
        D 2.0 0.6 (-1.06) 0.6 0.6 0.12 'button'
        F 2.4 1.0 (-1.06) 0.6 0.6 'accent'
    )
    $d.ledPos = @(3.0, 0.8, -1.06)
    $out.Add($d) | Out-Null

    return $out.ToArray()
}
