'
' very basic heap
'

const LOMEM = 0x69

sub memclr(p as address, bytes as integer)
    while bytes > 0
        poke p,0
        p = p + 1
        bytes = bytes - 1
    end while
end sub

function memory_heapalloc(bytes as integer) as address
    dim ptr as address
    ptr = peekw(LOMEM)
    pokew LOMEM, ptr + bytes
    memclr(ptr, bytes)
    return ptr
end function
