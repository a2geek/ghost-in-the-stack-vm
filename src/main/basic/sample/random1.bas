sub pokew(addr,value)
    poke addr,value
    poke addr+1,value >> 8
end sub

function peekw(addr)
    return (peek(addr+1) << 8) + peek(addr)
end function

' See https://en.wikipedia.org/wiki/Linear-feedback_shift_register
' See http://www.retroprogramming.com/2017/07/xorshift-pseudorandom-numbers-in-z80.html
function random()
    ' 78-79   | $4E-$4F     | Random-Number Field
    r = peekw(78)
    r = r xor (r << 7)
    r = r xor (r >> 9)
    r = r xor (r << 8)
    pokew(78, r)
    return r
end function

for i = 1 to 20
    print random()
next i
end
