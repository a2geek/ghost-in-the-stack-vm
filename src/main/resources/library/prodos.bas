module prodos
    const CODE = 0x300
    const PARAMS = 0x310
    const MLI = 0xbf00
    const DEVNUM = 0xbf30
    const DATE = 0xbf90
    const TIME = 0xbf92
    const MACHID = 0xbf98
    const PFXPTR = 0xbf9a

    ' this should be kept internal to ProDOS since there's going to be a better way to do it!

    function pstring(name as string) as address
        dim buffer as address = 0x280
        poke buffer, 0x3f   ' set max length
        poke buffer+1, 0x00 ' fake an end of string
        dim len as integer = strings.len(name)
        strings.strcpy(buffer, 0, name, 0, len)
        poke buffer, len
        return buffer
    end function
    function pstring2(name as string) as address
        dim buffer as address = 0x2c0
        poke buffer, 0x3f   ' set max length
        poke buffer+1, 0x00 ' fake an end of string
        dim len as integer = strings.len(name)
        strings.strcpy(buffer, 0, name, 0, len)
        poke buffer, len
        return buffer
    end function

    ' end of internal junk

    export inline function lastDevice() as integer
        return peek(DEVNUM)
    end function

    export inline function isPrefixActive() as boolean
        return peek(PFXPTR) <> 0
    end function

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

    export sub quit()
        poke PARAMS, 0x04
        poke PARAMS+1, 0
        pokew PARAMS+2, 0
        poke PARAMS+4, 0
        pokew PARAMS+5, 0
        callMLI(0x65)
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

    export function getDate() as integer
        callMLI(0x82)
        return peekw(DATE)
    end function

    export function getTime() as integer
        callMLI(0x82)
        return peekw(TIME)
    end function

    export sub createFile(pathname as string, filetype as integer, auxtype as integer)
        poke PARAMS, 0x07
        pokew PARAMS+1, pstring(pathname)
        poke PARAMS+3, 0xc3
        poke PARAMS+4, filetype
        pokew PARAMS+5, auxtype
        poke PARAMS+7, 0x01     ' standard file
        pokew PARAMS+8, 0       ' date
        pokew PARAMS+10, 0      ' time
        callMLI(0xc0)
    end sub

    export sub destroy(pathname as string)
        poke PARAMS, 0x01
        pokew PARAMS+1, pstring(pathname)
        callMLI(0xc1)
    end sub

    export sub rename(oldpathname as string, newpathname as string)
        poke PARAMS, 0x02
        pokew PARAMS+1, pstring(oldpathname)
        pokew PARAMS+3, pstring2(newpathname)
        callMLI(0xc2)
    end sub

    export sub lock(pathname as string)
        ' populate with current info
        poke PARAMS, 0x0a
        pokew PARAMS+1, pstring(pathname)
        callMLI(0xc4)
        ' change access bits and save chane
        poke PARAMS, 0x07
        poke PARAMS+3, 0x01
        callMLI(0xc3)
    end sub

    export sub unlock(pathname as string)
        ' populate with current info
        poke PARAMS, 0x0a
        pokew PARAMS+1, pstring(pathname)
        callMLI(0xc4)
        ' change access bits and save chane
        poke PARAMS, 0x07
        poke PARAMS+3, 0xc3
        callMLI(0xc3)
    end sub

    ' Based on https://retrocomputing.stackexchange.com/questions/17922/apple-ii-prodos-zero-length-prefix-and-mli-calls
    ' Intentionally staying from BASIC.SYSTEM since (hopefully) making SYSTEM an output type will become a thing.
    export sub ensurePrefixSet()
        if isPrefixActive() then
            return
        end if
        poke PARAMS, 0x02
        poke PARAMS+1, lastDevice()
        pokew PARAMS+2, 0x281
        callMLI(0xc5)
        ' TODO determine if we need to look for an error in the output or not...
        poke 0x280, (peek(0x281) AND 0x0f) + 1
        poke 0x281, asc("/")
        poke PARAMS, 0x01
        pokew PARAMS+1, 0x280
        callMLI(0xc6)
    end sub

    export sub setPrefix(newprefix as string)
        poke PARAMS, 0x01
        pokew PARAMS+1, pstring(newprefix)
        callMLI(0xc6)
    end sub

    export function getPrefix() as string
        dim buffer as address = 0x280
        poke PARAMS, 0x01
        pokew PARAMS+1, buffer
        callMLI(0xc7)
        poke buffer+peek(buffer)+1,0
        return buffer
    end function

    export function open(filename as string, buffer as address) as integer
        poke PARAMS, 0x03
        pokew PARAMS+1, filename
        pokew PARAMS+3, buffer
        callMLI(0xc8)
        return peek(PARAMS+5)
    end function

    export sub newline(refnum as integer, mask as integer, char as integer)
        poke PARAMS, 0x03
        poke PARAMS+1, refnum
        poke PARAMS+2, mask
        poke PARAMS+3, char
        callMLI(0xc9)
    end sub

    export function read(refnum as integer, buffer as address, length as integer) as integer
        poke PARAMS, 0x04
        poke PARAMS+1, refnum
        pokew PARAMS+2, buffer
        pokew PARAMS+4, length
        callMLI(0xca)
        return peekw(PARAMS+6)
    end function

    export function write(refnum as integer, buffer as address, length as integer) as integer
        poke PARAMS, 0x04
        poke PARAMS+1, refnum
        pokew PARAMS+2, buffer
        pokew PARAMS+4, length
        callMLI(0xcb)
        return peekw(PARAMS+6)
    end function

    export sub close(refnum as integer)
        poke PARAMS, 0x01
        pokew PARAMS+1, refnum
        callMLI(0xcc)
    end sub

    export sub flush(refnum as integer)
        poke PARAMS, 0x01
        pokew PARAMS+1, refnum
        callMLI(0xcd)
    end sub

    ' WARNING: drops high byte, so max of 64K-1
    export function getEOF(refnum as integer) as integer
        poke PARAMS, 0x02
        poke PARAMS+1, refnum
        callMLI(0xd1)
        if peek(PARAMS+4) <> 0 then
            raise error 0x4d, "LENGTH GREATER THAN 64K"
        end if
        return peekw(PARAMS+2)
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

    export sub bload(pathname as string, addr as address)
        dim buffer as address = 0x9000   ' FIXME
        dim refnum as integer, n as integer
        refnum = open(pathname, buffer)
        n = read(refnum, addr, getEOF(refnum))
        close(refnum)
    end sub

    ' initialization code
    prodos.ensurePrefixSet()

end module
