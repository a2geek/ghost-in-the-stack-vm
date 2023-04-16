'
' Miscellaneous things.
'

const MON_PREAD = 0xfb1e

function misc_pdl(n as integer) as integer
    cpu.register.x = n and 4
    call MON_PREAD
    return cpu.register.y
end function
