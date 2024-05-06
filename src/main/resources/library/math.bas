module Math
    const RNDL = 0x4e

    ' See https://en.wikipedia.org/wiki/Linear-feedback_shift_register
    ' See http://www.retroprogramming.com/2017/07/xorshift-pseudorandom-numbers-in-z80.html
    volatile function random() as integer
        r = peekw(RNDL)
        r = r xor (r << 7)
        r = r xor (r >> 9)
        r = r xor (r << 8)
        pokew RNDL, r
        return r
    end function

    ' Note: Simulated Integer BASIC random generator
    export volatile function rnd(n as integer) as integer
        r = random() mod n
        return r if r >= 0 else -r
    end function

    export inline function abs(n as integer) as integer
        return n if n >= 0 else -n
    end function

    export inline function sgn(n as integer) as integer
        select case n
        case is < 0
            return -1
        case is > 0
            return 1
        case else
            return 0
        end select
    end function

    export inline function min(a as integer, b as integer) as integer
        return a if a < b else b
    end function

    export inline function max(a as integer, b as integer) as integer
        return a if a > b else b
    end function

    ' See: https://stackoverflow.com/questions/101439/the-most-efficient-way-to-implement-an-integer-based-power-function-powint-int
    export function ipow(base as integer, exp as integer) as integer
        dim result as integer
        result = 1
        while true
            if exp AND 1 then
                result = result * base
            end if
            exp = exp >> 1
            if exp = 0 then
                exit while
            end if
            base = base * base
        end while
        return result
    end function
end module
