option strict

'
' very basic heap
' this is a linked list, initially only one chunk with all of memory in the chunk
'
module memory
    ' Header:
    '   +0 next: address
    '   +2 size: integer
    '   +4 data: address
    const HEADER_SIZE = 4
    dim freeptr as address
    dim loptr as address, hiptr as address  ' can't reuse lomem, so crappy names. :-)

    const LOMEM = 0x69
    const MEMSIZE = 0x73    ' HIMEM

    sub memclr(p as address, bytes as integer)
        dim odd as boolean = bytes AND 1
        bytes = bytes >> 1
        while bytes <> 0
            pokew p,0
            p = p + 2
            bytes = bytes - 1
        end while
        if odd then
            poke p,0
        end if
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
        memory.loptr = peekw(LOMEM)
        memory.hiptr = peekw(MEMSIZE)
        memory.freeptr = memory.loptr
        pokew memory.freeptr,0
        pokew memory.freeptr+2,memory.hiptr - memory.loptr - HEADER_SIZE
#if defined(TRACE)
        print "HEAPINIT: loptr=";memory.loptr;", hiptr=";memory.hiptr;" size=";peekw(memory.freeptr+2)
#endif
    end sub

    private sub setpriorptr(priorptr as address, newptr as address)
        if priorptr <> 0 then
            pokew priorptr, newptr
        else
            memory.freeptr = newptr
        end if
    end sub

    private sub setheader(ptr as address, nextptr as address, size as integer)
        pokew ptr, nextptr
        pokew ptr+2, size
    end sub

    volatile function heapalloc(bytes as integer) as address
        dim ptr as address, priorptr as address, dataptr as address, size as integer, needed as integer
#if defined(TRACE)
        print "HEAPALLOC: bytes=";bytes
#endif
        ptr = memory.freeptr
        needed = bytes+HEADER_SIZE
        ' look through the linked list for a chunk that is large enough
        while ptr <> 0
            size = peekw(ptr+2)
            if needed <= size then
                dataptr = ptr+HEADER_SIZE
                ' if the chunk was too large, setup a free chunk
                if size-needed > HEADER_SIZE then
                    setheader(ptr+needed, peekw(ptr), size-needed)
                    setheader(ptr, 0, needed)
                    setpriorptr(priorptr, ptr+needed)
                else
                    setpriorptr(priorptr, peekw(ptr))
                    pokew ptr,0
                end if
                ' clean up memory and return the pointer
                memclr(dataptr, bytes)
#if defined(TRACE)
        print "HEAPALLOC: return=";dataptr
#endif
                return dataptr
            else
                ' keep looking until we run out!
                priorptr = ptr
                ptr = peekw(ptr)
            end if
        end while
        raise error 77, "OUT OF MEMORY"
    end function

    private sub consolidate(ptr as address)
        dim ptrnext as address = peekw(ptr)
        dim ptrsize as integer = peekw(ptr+2)
        if ptr <> 0 AND ptr+ptrsize = ptrnext then
            setheader(ptr, peekw(ptrnext), ptrsize+peekw(ptrnext+2))
        end if
    end sub

    sub heapfree(data as address)
        dim ptr as address, priorptr as address, dataptr as address
        ' range check since the compiler can be overzealous
        if data < memory.loptr or data >= memory.hiptr then
            return
        end if
#if defined(TRACE)
        print "HEAPFREE: DATA=";data
#endif
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
