'
' Text routines.
'

' See https://6502disassembly.com/a2-rom/Applesoft.html
const MON_CH = 0x24
const MON_INVFLAG = 0x32
const FLASH_BIT = 0xf3
const MON_SETTXT = 0xfb39
const MON_TABV = 0xfb5b
const MON_HOME = 0xfc58

sub inline text_home
    call MON_HOME
end sub

sub inline text_text
    call MON_SETTXT
end sub

sub inline text_htab(h as integer)
    poke MON_CH, h-1
end sub

sub inline text_vtab(v as integer)
    cpu.register.a = v - 1
    call MON_TABV
end sub

sub text_normal
    poke MON_INVFLAG, 0xff
    poke FLASH_BIT, 0
end sub

sub text_inverse
    poke MON_INVFLAG, 0x3f
    poke FLASH_BIT, 0
end sub

sub text_flash
    poke MON_INVFLAG, 0x7f
    poke FLASH_BIT, 0x40
end sub
