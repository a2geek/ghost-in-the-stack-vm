'
' Lo-resolution graphics routines.
'

' See https://6502disassembly.com/a2-rom/Applesoft.html
const MON_H2 = 0x2c
const MON_V2 = 0x2d
const MON_PLOT = 0xf800
const MON_HLINE = 0xf819
const MON_VLINE = 0xf828
const MON_SETCOL = 0xf864
const MON_SCRN = 0xf871
const MON_SETGR = 0xfb40

sub lores_color(c as integer)
    cpu.register.a = c
    call MON_SETCOL
end sub

sub lores_gr
    call MON_SETGR
end sub

sub lores_plot(x as integer, y as integer)
    cpu.register.y = x
    cpu.register.a = y
    call MON_PLOT
end sub

sub lores_hlin(x0 as integer, x1 as integer, y as integer)
    cpu.register.y = x0
    poke MON_H2, x1
    cpu.register.a = y
    call MON_HLINE
end sub

sub lores_vlin(y0 as integer, y1 as integer, x as integer)
    cpu.register.a = y0
    poke MON_V2, y1
    cpu.register.y = x
    call MON_VLINE
end sub

function lores_scrn(x as integer, y as integer) as integer
    cpu.register.y = x
    cpu.register.a = y
    call MON_SCRN
    return cpu.register.a
end function
