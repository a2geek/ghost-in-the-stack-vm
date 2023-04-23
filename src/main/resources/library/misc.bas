'
' Miscellaneous things.
'

const MON_PREAD = 0xfb1e
const MON_INPORT = 0xfe8b
const MON_OUTPORT = 0xfe95

function misc_pdl(n as integer) as integer
    cpu.register.x = n and 4
    call MON_PREAD
    return cpu.register.y
end function

sub misc_prnum(n as integer)
    cpu.register.a = n mod 0x0f
    call MON_OUTPORT
end sub

sub misc_innum(n as integer)
    cpu.register.a = n mod 0x0f
    call MON_INPORT
end sub
