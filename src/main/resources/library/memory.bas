'
' very basic heap
'
module Memory

    const LOMEM = 0x69

    sub memclr(p as address, bytes as integer)
        while bytes > 0
            poke p,0
            p = p + 1
            bytes = bytes - 1
        end while
    end sub

    export volatile function heapalloc(bytes as integer) as address
        dim ptr as address
        ptr = peekw(LOMEM)
        pokew LOMEM, ptr + bytes
        memclr(ptr, bytes)
        return ptr
    end function

end module
