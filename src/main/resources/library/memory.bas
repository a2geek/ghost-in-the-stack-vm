option strict

'
' very basic heap
' this is a linked list, initially only one chunk with all of memory in the chunk
'
module Memory
    ' Header:
    '   +0 next: address
    '   +2 size: integer
    '   +4 data: address
    const HEADER_SIZE = 4
    dim freeptr as address

    const LOMEM = 0x69
    const MEMSIZE = 0x73    ' HIMEM

    sub memclr(p as address, bytes as integer)
        while bytes > 0
            poke p,0
            p = p + 1
            bytes = bytes - 1
        end while
    end sub

    ' initialization
    memory.freeptr = peekw(LOMEM)
    pokew memory.freeptr,0
    pokew memory.freeptr+2,peekw(MEMSIZE)-peekw(LOMEM)-HEADER_SIZE

    volatile function memfree() as integer
        dim ptr as address, n as integer
        ptr = memory.freeptr
        while ptr <> 0
            n = n + peekw(ptr+2)
            ptr = peekw(ptr)
        end while
        return n
    end function

    volatile function heapalloc(bytes as integer) as address
        dim ptr as address, data as address, size as integer, needed as integer
        ptr = memory.freeptr
        needed = bytes+HEADER_SIZE
        while ptr <> 0
            size = peekw(ptr+2)
            if needed <= size then
                data = ptr+HEADER_SIZE
                pokew ptr,0
                pokew ptr+2,bytes
                if needed < size then
                    pokew ptr,ptr+needed
                    pokew ptr+needed,0
                    pokew ptr+needed+2,size-needed
                end if
                memory.freeptr = peekw(ptr)
                memclr(data, bytes)
                return data
            else
                ptr = peekw(ptr)
            end if
        end while
        raise error 77, "OUT OF MEMORY"
    end function
end module
