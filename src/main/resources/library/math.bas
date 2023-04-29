'
' Math routines.
'

const RNDL = 0x4e
const RNDH = 0x4f

' See https://en.wikipedia.org/wiki/Linear-feedback_shift_register
' See http://www.retroprogramming.com/2017/07/xorshift-pseudorandom-numbers-in-z80.html
function random() as integer
    r = (peek(RNDH) << 8) + peek(RNDL)
    r = r xor (r << 7)
    r = r xor (r >> 9)
    r = r xor (r << 8)
    poke RNDL, r
    poke RNDH, r >> 8
    return r
end function

' Note: Simulated Integer BASIC random generator
function math_rnd(n as integer) as integer
    r = random() mod n
    if r < 0 then
        return -r
    end if
    return r
end function

function math_abs(n as integer) as integer
    if n < 0 then
        return -n
    end if
    return n
end function

function math_sgn(n as integer) as integer
    if n < 0 then
        return -1
    else
        if n > 0 then
            return 1
        else
            return 0
        end if
    end if
end function
