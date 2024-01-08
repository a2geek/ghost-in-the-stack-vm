module prodos
    const CODE = 0x300
    const PARAMS = 0x310
    const MLI = 0xbf00

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

    export function getPrefix() as string
        dim buffer as address = 0x280
        poke PARAMS, 0x01
        pokew PARAMS+1, buffer
        callMLI(0xc7)
        poke buffer+peek(buffer)+1,0
        return buffer
    end function

end module
