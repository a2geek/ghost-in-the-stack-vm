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

    volatile function memfree() as integer
        dim ptr as address, n as integer
        ptr = memory.freeptr
        while ptr <> 0
            n = n + peekw(ptr+2)
            ptr = peekw(ptr)
        end while
        return n
    end function

    sub heapinit()
        memory.freeptr = peekw(LOMEM)
        pokew memory.freeptr,0
        pokew memory.freeptr+2,peekw(MEMSIZE)-peekw(LOMEM)-HEADER_SIZE
    end sub

    volatile function heapalloc(bytes as integer) as address
        dim ptr as address, priorptr as address, dataptr as address, size as integer, needed as integer
        ptr = memory.freeptr
        needed = bytes+HEADER_SIZE
        ' look through the linked list for a chunk that is large enough
        while ptr <> 0
            size = peekw(ptr+2)
            if needed <= size then
                dataptr = ptr+HEADER_SIZE
                ' if the chunk was too large, setup a free chunk
                if size-needed > HEADER_SIZE then
                    pokew ptr+needed,peekw(ptr)
                    pokew ptr+needed+2,size-needed
                    pokew ptr,0
                    pokew ptr+2,needed
                    if priorptr <> 0 then
                        pokew priorptr,ptr+needed
                    else
                        memory.freeptr = ptr+needed
                    end if
                else
                    if priorptr <> 0 then
                        pokew priorptr,peekw(ptr)
                    else
                        memory.freeptr = peekw(ptr)
                    end if
                    pokew ptr,0
                end if
                ' clean up memory and return the pointer
                memclr(dataptr, bytes)
                return dataptr
            else
                priorptr = ptr
                ptr = peekw(ptr)
            end if
        end while
        raise error 77, "OUT OF MEMORY"
    end function

    ' NOTE: should be PRIVATE once that is an option
    sub consolidate(ptr as address)
        dim ptrnext as address = peekw(ptr)
        dim ptrsize as integer = peekw(ptr+2)
        if ptr <> 0 AND ptr+ptrsize = ptrnext then
            pokew ptr+2, ptrsize+peekw(ptrnext+2)
            pokew ptr, peekw(ptrnext)
        end if
    end sub

    sub heapfree(data as address)
        dim ptr as address, priorptr as address, dataptr as address
        ' empty list
        dataptr = data-HEADER_SIZE
        if memory.freeptr = 0 then
            memory.freeptr = dataptr
            return
        end if
        ' find position
        ptr = memory.freeptr
        while ptr <> 0 AND ptr < data
            if peekw(ptr) = 0 then
                exit while
            end if
            priorptr = ptr
            ptr = peekw(ptr)
        end while
        ' insert/consolidate with following chunks
        if priorptr <> 0 then
            ' we are adding between two chunks
            pokew dataptr, ptr
            pokew priorptr, dataptr
            consolidate(dataptr)
            consolidate(priorptr)
        else
            ' adding to beginning
            pokew dataptr, ptr
            memory.freeptr = dataptr
            consolidate(dataptr)
        end if
    end sub

    ' initialization
    memory.heapinit()

end module
