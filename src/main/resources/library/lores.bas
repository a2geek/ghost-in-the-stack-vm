'
' Lo-resolution graphics routines.
'
module lores
    ' See https://6502disassembly.com/a2-rom/Applesoft.html
    const MON_H2 = 0x2c
    const MON_V2 = 0x2d
    const MON_PLOT = 0xf800
    const MON_HLINE = 0xf819
    const MON_VLINE = 0xf828
    const MON_SETCOL = 0xf864
    const MON_SCRN = 0xf871
    const MON_SETGR = 0xfb40

    ' Colors
    const BLACK = 0
    const MAGENTA = 1
    const DARK_BLUE = 2
    const PURPLE = 3
    const DARK_GREEN = 4
    const DARK_GRAY = 5
    const MEDIUM_BLUE = 6
    const LIGHT_BLUE = 7
    const BROWN = 8
    const ORANGE = 9
    const GREY = 10
    const PINK = 11
    const GREEN = 12
    const YELLOW = 13
    const AQUA = 14
    const WHITE = 15

    export inline sub color(c as integer)
        cpu.register.a = c
        call MON_SETCOL
    end sub

    export inline sub gr()
        call MON_SETGR
    end sub

    export inline sub plot(x as integer, y as integer)
        cpu.register.y = x
        cpu.register.a = y
        call MON_PLOT
    end sub

    export inline sub hlin(x0 as integer, x1 as integer, y as integer)
        cpu.register.y = x0
        poke MON_H2, x1
        cpu.register.a = y
        call MON_HLINE
    end sub

    export inline sub vlin(y0 as integer, y1 as integer, x as integer)
        cpu.register.a = y0
        poke MON_V2, y1
        cpu.register.y = x
        call MON_VLINE
    end sub

    export inline function scrn(x as integer, y as integer) as integer
        cpu.register.y = x
        cpu.register.a = y
        call MON_SCRN
        return cpu.register.a
    end function

end module
