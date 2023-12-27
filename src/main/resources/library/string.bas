' STRING = pointer

' String format:
' +00 = length
' +01..nn = characters
' +(nn+1) = 00 zero terminated

function string_strmax(s as string) as integer
    return peek(s)
end function

function string_asc(str as string) as integer
    ' skip the maxlen byte
    return peek(str + 1)
end function

function string_len(str as string) as integer
    dim s as address, n as integer

    ' skip the maxlen byte
    s = str + 1

    while peek(s) <> 0
        n = n + 1
        s = s + 1
    end while

    return n
end function

function string_strcmp(left as string, right as string) as integer
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

sub string_strcpy(target as string, targetStart as integer, _
        source as String, sourceStart as integer, sourceEnd as integer)
    dim t as address, s as address
    if sourceEnd = 0 then
        sourceEnd = string_len(source)
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
