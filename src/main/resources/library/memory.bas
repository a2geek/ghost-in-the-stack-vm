option strict

'
' very basic heap
' this is a linked list, initially only one chunk with all of memory in the chunk
'
module memory
    ' Header:
    '   +0 next: address
    '   +2 size: integer
    '   +4 refcount: integer
    '   +6 data: address
    const HEADER_SIZE = 6

    dim freeptr as address
    dim loptr as address, hiptr as address  ' can't reuse lomem, so crappy names. :-)

    const LOMEM = 0x69
    const MEMSIZE = 0x73    ' HIMEM

    private inline sub SetNext(ptr as address, nextptr as address)
        pokew ptr+0, nextptr
    end sub
    private inline sub SetSize(ptr as address, size as integer)
        pokew ptr+2, size
    end sub
    private inline sub SetCount(ptr as address, count as integer)
        pokew ptr+4, count
    end sub
    private inline function GetNext(ptr as address) as address
        return peekw(ptr+0)
    end function
    private inline function GetSize(ptr as address) as integer
        return peekw(ptr+2)
    end function
    private inline function GetCount(ptr as address) as integer
        return peekw(ptr+4)
    end function

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
            n = n + GetSize(ptr)
            ptr = GetNext(ptr)
        end while
        return n
    end function

    sub heapinit()
        memory.loptr = peekw(LOMEM)
        memory.hiptr = peekw(MEMSIZE)
        memory.freeptr = memory.loptr
        SetNext(memory.freeptr,0)
        SetSize(memory.freeptr,memory.hiptr - memory.loptr - HEADER_SIZE)
#if defined(TRACE)
        print "HEAPINIT: loptr=";memory.loptr;", hiptr=";memory.hiptr;" size=";GetSize(memory.freeptr)
#endif
    end sub

    private sub SetPriorPtr(priorptr as address, newptr as address)
        if priorptr <> 0 then
            SetNext(priorptr,newptr)
        else
            memory.freeptr = newptr
        end if
    end sub

    private sub SetHeader(ptr as address, nextptr as address, size as integer)
        SetNext(ptr, nextptr)
        SetSize(ptr, size)
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
            size = Getsize(ptr)
            if needed <= size then
                dataptr = ptr+HEADER_SIZE
                ' if the chunk was too large, setup a free chunk
                if size-needed > HEADER_SIZE then
                    SetHeader(ptr+needed, peekw(ptr), size-needed)
                    SetHeader(ptr, 0, needed)
                    SetPriorPtr(priorptr, ptr+needed)
                else
                    SetPriorPtr(priorptr, peekw(ptr))
                    SetNext(ptr,0)
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
                ptr = GetNext(ptr)
            end if
        end while
        raise error 77, "OUT OF MEMORY"
    end function

    private sub consolidate(ptr as address)
        dim ptrnext as address = GetNext(ptr)
        dim ptrsize as integer = GetSize(ptr)
        if ptr <> 0 AND ptr+ptrsize = ptrnext then
            SetHeader(ptr, GetNext(ptrnext), ptrsize+GetSize(ptrnext))
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
            if GetNext(ptr) = 0 then
                exit while
            end if
            priorptr = ptr
            ptr = GetNext(ptr)
        end while
        ' insert/consolidate with following chunks
        if priorptr <> 0 then
            ' we are adding between two chunks
            SetNext(dataptr,ptr)
            SetNext(priorptr,dataptr)
            consolidate(dataptr)
            consolidate(priorptr)
        else
            ' adding to beginning
            SetNext(dataptr,ptr)
            memory.freeptr = dataptr
            consolidate(dataptr)
        end if
    end sub

    ' initialization
    memory.heapinit()

end module
