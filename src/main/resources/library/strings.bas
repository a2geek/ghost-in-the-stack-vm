' STRING = pointer

' String format:
' +00 = length
' +01..nn = characters
' +(nn+1) = 00 zero terminated
module strings
    inline function strmax(s as string) as integer
        return peek(s)
    end function

    export inline function asc(str as string) as integer
        ' skip the maxlen byte
        return peek(str + 1)
    end function

    ' Cheap/temporary replacement for MID$
    export inline function ascn(str as string, n as integer) as integer
        return peek(str + n + 1)
    end function

    ' FIXME: hard-coded to an address for the string
    export function strn(n as integer, maxlen as integer) as string
        dim buffer as address = 0x2ff
        dim l as integer = 0
        ' terminating byte
        poke buffer, 0
        buffer = buffer - 1
        ' move backwards as long as we have a number
        while n > 0
            l = l + 1
            poke buffer, n mod 10 + asc("0")
            n = n / 10
            buffer = buffer - 1
        end while
        ' zero pad the number
        while l < maxlen
            poke buffer, asc("0")
            l = l + 1
            buffer = buffer - 1
        end while
        poke buffer,l
        return buffer
    end function

    export function len(str as string) as integer
        dim s as address, n as integer

        ' skip the maxlen byte
        s = str + 1

        while peek(s) <> 0
            n = n + 1
            s = s + 1
        end while

        return n
    end function

    function strcmp(left as string, right as string) as integer
        dim l as address, r as address
        dim chl as integer, chr as integer

        ' skip the maxlen byte
        l = left + 1
        r = right + 1

        ' check each character in combination, trusting that we end at 0
        while true
            chl = peek(l)
            chr = peek(r)
            if chl = 0 or chr = 0 or chl <> chr then
                exit while
            end if
            l = l + 1
            r = r + 1
        end while

        if chl < chr then
            return -1
        elseif chl > chr then
            return 1
        else
            return 0
        end if
    end function

    sub strcpy(target as string, targetStart as integer, _
            source as String, sourceStart as integer, sourceEnd as integer)
        dim t as address, s as address
        if sourceEnd = 0 then
            sourceEnd = len(source)
        else
            sourceEnd = sourceEnd - sourceStart + 1
        end if
        t = target + targetStart
        s = source + sourceStart
        while sourceEnd > 0 and peek(s) <> 0
            poke t, peek(s)
            t = t + 1
            s = s + 1
            sourceEnd = sourceEnd - 1
        end while
        poke t, 0
    end sub

    sub strcat(a as string, b as string)
        dim p as address, q as address
        p = a
        while peek(p) <> 0
            p = p + 1
        end while
        q = b + 1
        while peek(q) <> 0
            poke p, peek(q)
            p = p + 1
            q = q + 1
        end while
        poke p, 0
    end sub

#if defined(option.heap)
    export function chr$(n as integer) as string
        dim s as address
        s = memory.heapalloc(1+2)
        poke s+0,1
        poke s+1,n
        poke s+2,0
        return s
    end function
#endif

#if defined(option.heap)
    function strreverse(s as string) as string
        dim p as string, i as integer, j as integer, t as byte
        j = len(s)
        p = memory.heapalloc(j+2)   ' +1 to skip the length byte
        poke p,j
        poke p+j+1,0                ' +1 to skip the length byte
        i = 0+1                     ' +1 to skip the length byte
        while i <= j
            t = peek(s+i)
            poke p+i,peek(s+j)
            poke p+j,t
            i = i + 1
            j = j - 1
        end while
        return p
    end function
#endif

#if defined(option.heap)
    export function str$(n as integer)
        dim s as address, q as address, d as integer, m as boolean
        ' max 5 digits + 1 for minus sign + 2 for overhead = 8
        s = memory.heapalloc(8)
        poke s,8
        q = s + 1
        if n < 0 then
            m = True
            n = -n
        end if
        do
            d = n mod 10
            n = n / 10
            poke q,asc("0")+d
            q = q + 1
        loop while n <> 0
        if m then
            poke q,asc("-")
            q = q + 1
        end if
        poke q,0
        q = strreverse(s)
        memory.heapfree(s)
        return q
    end function
#endif

#if defined(option.heap)
    export function mid$(s as string, start as integer, optional n as integer = -1)
        dim p as address, q as address
        if n = -1 then
            ' flag from the compiler for mid$(string,start) form
            n = strings.len(s)
        end if
        if start < 1 or n < 0 then
            raise error 53, "ILLEGAL QUANTITY ERROR", "MID$"
        end if
        p = memory.heapalloc(n+2)
        poke p,n+2
        q = p + 1
        s = s + start
        while n > 0 and peek(s) <> 0
            poke q,peek(s)
            q = q + 1
            s = s + 1
            n = n - 1
        end while
        return p
    end function

    export inline function left$(s as string, n as integer)
        if n < 0 then
            raise error 53, "ILLEGAL QUANTITY ERROR", "LEFT$"
        end if
        return mid$(s, 1, n)
    end function

    export function right$(s as string, n as integer)
        dim start as integer
        if n < 1 then
            raise error 53, "ILLEGAL QUANTITY ERROR", "RIGHT$"
        end if
        start = len(s)-n+1
        if start < 1 then
            start = 1
        end if
        return mid$(s, start, n)
    end function
#endif

end module
