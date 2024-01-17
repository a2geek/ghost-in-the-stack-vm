module prodos
    const CODE = 0x300
    const PARAMS = 0x310
    const MLI = 0xbf00
    const DEVNUM = 0xbf30
    const MACHID = 0xbf98
    const PFXPTR = 0xbf9a

    function mliErrorMessage(errnum as integer) as string
        select case errnum
            case 0x01
                return "BAD SYSTEM CALL NUMBER"
            case 0x04
                return "BAD SYSTEM CALL PARAMETER COUNT"
            case 0x25
                return "INTERRUPT VECTOR TABLE FULL"
            case 0x27
                return "I/O ERROR"
            case 0x28
                return "NO DEVICE DETECTED/CONNECTED"
            case 0x2b
                return "DISK WRITE PROTECTED"
            case 0x2e
                return "DISK SWITCHED"
            case 0x40
                return "INVALID PATHNAME SYNTAX"
            case 0x42
                return "FILE CONTROL BLOCK TABLE FULL"
            case 0x43
                return "INVALID REFERENCE NUMBER"
            case 0x44
                return "PATH NOT FOUND"
            case 0x45
                return "VOLUME DIRECTORY NOT FOUND"
            case 0x46
                return "FILE NOT FOUND"
            case 0x47
                return "DUPLICATE FILENAME"
            case 0x48
                return "OVERRUN ERROR"
            case 0x49
                return "VOLUME DIRECTORY FULL"
            case 0x4a
                return "INCOMPATIBLE FILE FORMAT"
            case 0x4b
                return "UNSUPPORTED STORAGE TYPE"
            case 0x4c
                return "END OF FILE"
            case 0x4d
                return "POSITION OUT OF RANGE"
            case 0x4e
                return "ACCESS ERROR"
            case 0x50
                return "FILE IS OPEN"
            case 0x51
                return "DIRECTORY COUNT ERROR"
            case 0x52
                return "NOT A PRODOS DISK"
            case 0x53
                return "INVALID PARAMETER"
            case 0x55
                return "VOLUME CONTROL BLOCK TABLE FULL"
            case 0x56
                return "BAD BUFFER ADDRESS"
            case 0x57
                return "DUPLICATE VOLUME"
            case 0x5a
                return "BIT MAP DISK ADDRESS IS IMPOSSIBLE"
            case else
                return "NO ERROR"
        end select
    end function

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
            raise error rc, mliErrorMessage(rc)
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
