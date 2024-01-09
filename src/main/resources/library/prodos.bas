module prodos
    const CODE = 0x300
    const PARAMS = 0x310
    const MLI = 0xbf00
    const DEVNUM = 0xbf30
    const MACHID = 0xbf98
    const PFXPTR = 0xbf9a

    sub callMLI(functionCode as Integer)
        dim rc as integer
        poke CODE, 0x20     ' JSR MLI
        pokew CODE+1, MLI
        poke CODE+3, functionCode
        pokew CODE+4, PARAMS
        poke CODE+6, 0x60   ' RTS
        call CODE
        rc = cpu.register.a ' preserve because PRINT(etc) use the registers as well
        if rc <> 0 then
            print "Error: "; rc
            end     ' FIXME - need to raise some error code for on err handling
        end if
    end sub

    export sub readBlock(unitNumber as integer, blockNumber as integer, dataBuffer as address)
        poke PARAMS, 0x03
        poke PARAMS+1, unitNumber
        pokew PARAMS+2, dataBuffer
        pokew PARAMS+4, blockNumber
        callMLI(0x80)
    end sub

    export sub writeBlock(unitNumber as integer, blockNumber as integer, dataBuffer as address)
        poke PARAMS, 0x03
        poke PARAMS+1, unitNumber
        pokew PARAMS+2, dataBuffer
        pokew PARAMS+4, blockNumber
        callMLI(0x81)
    end sub

    export function getPrefix() as string
        dim buffer as address = 0x280
        poke PARAMS, 0x01
        pokew PARAMS+1, buffer
        callMLI(0xc7)
        poke buffer+peek(buffer)+1,0
        return buffer
    end function

    export inline function lastDevice() as integer
        return peek(DEVNUM)
    end function

    export inline function isPrefixActive() as boolean
        return peek(PFXPTR) <> 0
    end function

    export inline function isAppleII() as boolean
        return (peek(MACHID) AND 0b11001000) = 0
    end function

    export inline function isAppleIIplus() as boolean
        return (peek(MACHID) AND 0b11001000) = 0b01000000
    end function

    export inline function isAppleIIe() as boolean
        return (peek(MACHID) AND 0b11001000) = 0b10000000
    end function

    export inline function isAppleIII() as boolean
        return (peek(MACHID) AND 0b11001000) = 0b11000000
    end function

    export inline function isAppleIIc() as boolean
        return (peek(MACHID) AND 0b11001000) = 0b10001000
    end function

    export inline function has48K() as boolean
        return (peek(MACHID) AND 0b00110000) = 0b00010000
    end function

    export inline function has64K() as boolean
        return (peek(MACHID) AND 0b00110000) = 0b00100000
    end function

    export inline function has128K() as boolean
        return (peek(MACHID) AND 0b00110000) = 0b00110000
    end function

    export inline function has80Cols() as boolean
        return (peek(MACHID) AND 0b00000010) <> 0
    end function

    export inline function hasClock() as boolean
        return (peek(MACHID) AND 0b00000001) <> 0
    end function

end module
