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

    const A1L = 0x3c
    const A2L = 0x3e
    const A4L = 0x42
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
    private volatile function GetNext(ptr as address) as address
        return peekw(ptr+0)
    end function
    private volatile function GetSize(ptr as address) as integer
        return peekw(ptr+2)
    end function
    private volatile function GetCount(ptr as address) as integer
        return peekw(ptr+4)
    end function
    ' range check since the compiler can be overzealous
    private inline function IsInHeap(ptr as address) as Boolean
        return ptr >= memory.loptr and ptr < memory.hiptr
    end function

    sub MemMove(src as address, srcEnd as address, dst as address)
        pokew A1L,src
        pokew A2L,srcEnd
        pokew A4L,dst
        cpu.register.y = 0
        call 0xfe2c
    end sub

    sub memclr(p as address, bytes as integer)
        select bytes
        case 0
            ' do nothing
        case 1
            poke p,0
        case 2
            pokew p,0
        case else
            poke p,0
            MemMove(p,p+bytes-2,p+1)
        end select
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

    sub HeapInit()
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

    private sub consolidate(ptr as address)
        dim ptrnext as address = GetNext(ptr)
        dim ptrsize as integer = GetSize(ptr)
        if ptr <> 0 AND ptr+ptrsize = ptrnext then
            SetHeader(ptr, GetNext(ptrnext), ptrsize+GetSize(ptrnext))
        end if
    end sub

    sub HeapFree(data as address)
        dim ptr as address, priorptr as address, dataptr as address
        dataptr = data-HEADER_SIZE
        if IsInHeap(dataptr) then
#if defined(TRACE)
            print "HEAPFREE: DATA=";data
#endif

            ' empty list
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
        end if
    end sub

    Sub HeapRefIncr(data as address)
        Dim ptr as Address
        ptr = data-HEADER_SIZE
        If IsInHeap(ptr) then
            SetCount(ptr, GetCount(ptr)+1)
        End If
    End Sub

    Sub HeapRefDecr(ByRef data As Address)
        Dim ptr as Address
        ptr = data-HEADER_SIZE
        If IsInHeap(ptr) Then
            SetCount(ptr, GetCount(ptr)-1)
            If GetCount(ptr) = 0 Then
                HeapFree(data)  ' yes, data since HeapFree also does math
                data = 0        ' ensure we don't double-free this one
            End If
        End If
    End Sub

    volatile function HeapAlloc(bytes as integer) as address
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
                SetCount(ptr,0)
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

    sub MemReport()
        dim ptr as address, freeptr as address
        dim size as integer, free as integer, used as integer, total as integer
        ptr = memory.loptr
        freeptr = memory.freeptr

        while ptr < memory.hiptr - HEADER_SIZE
            size = GetSize(ptr)
            print "$";CAddr(ptr);"(";size;" BYTES; REF=";GetCount(ptr);") - ";
            total = total + size
            if ptr = freeptr then
                print "FREE"
                freeptr = GetNext(freeptr)
                free = free + size
            else
                print "USED"
                used = used + size
            end if
            ' the next ptr is actually the free linked list, so we need to compute this
            ptr = ptr + size
        end while
    end sub

    ' initialization
    memory.HeapInit()

end module
