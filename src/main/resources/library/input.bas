'
' Input support
'

const PROMPT = 0x33
const GETLN1 = 0xfd6f
const BUFFER = 0x200

' Not really correct but first attempt
function input_integer as integer
    dim n as integer, i as integer, ch as integer
    dim good as boolean
    while not good
        call GETLN1
        do
            ch = peek(BUFFER+i)
            if ch = ASC(",") or ch = 0x8d then  ' need CHR$!!
                exit while
            end if
            if ch >= ASC("0") and ch <= ASC("9") then
                n = n*10 + ch-ASC("0")
                good = true
            end if
            i = i + 1
        loop while i < 240
    end while
    return n
end function

sub input_readstring(s as string)
    dim i as integer, ch as integer
    dim maxlen as integer

    maxlen = peek(s)
    call GETLN1
    while i < 240 and i < maxlen
        ch = peek(BUFFER+i)
        if ch = 0x8d then
            exit while
        end if
        poke s+1+i,ch
        i = i + 1
    end while
    poke s+1+i,0
end sub
