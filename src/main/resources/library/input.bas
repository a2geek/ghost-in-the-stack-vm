'
' Input support
'

const INPTR = 0x7f
const BUFFER = 0x200
const GETLN1 = 0xfd6f

sub input_readline()
    call GETLN1
    pokew INPTR,BUFFER
end sub

function input_scaninteger as integer
    dim n as integer, i as integer, ch as integer
    dim p as address
    dim good as boolean

    while not good
        p = peekw(INPTR)
        do
            ch = peek(p)
            if ch = ASC(",") then
                p = p + 1          ' skip the comma
                exit do
            elseif ch = 0x8d then  '  need CHR$!!
                exit do
            end if
            if ch >= ASC("0") and ch <= ASC("9") then
                n = n*10 + ch-ASC("0")
                good = true
            end if
            p = p + 1
            i = i + 1
        loop while i < 240
        pokew INPTR,p
        if not good then
            print "?";
            input_readline()
        end if
    end while
    return n
end function

sub input_scanstring(s as string)
    dim i as integer, ch as integer
    dim p as address
    dim maxlen as integer

    maxlen = peek(s)
    p = peekw(INPTR)
    while i < 240 and i < maxlen
        ch = peek(p)
        if ch = 0x8d then
            exit while
        end if
        poke s+1+i,ch
        p = p + 1
        i = i + 1
    end while
    poke s+1+i,0
    pokew INPTR,p
end sub
