'
' Miscellaneous things.
'
module misc
    const MON_PREAD = 0xfb1e
    const MON_INPORT = 0xfe8b
    const MON_OUTPORT = 0xfe95

    export inline function pdl(n as integer) as integer
        cpu.register.x = n and 3
        call MON_PREAD
        return cpu.register.y
    end function

    export inline sub prnum(n as integer)
        cpu.register.a = n and 0x0f
        call MON_OUTPORT
    end sub

    export inline sub innum(n as integer)
        cpu.register.a = n and 0x0f
        call MON_INPORT
    end sub
end module
