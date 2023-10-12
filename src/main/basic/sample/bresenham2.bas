' Experiment to try a full Ghost implementation without using ROM routines.

uses "print"
uses "lores"


' See: https://en.wikipedia.org/wiki/Bresenham's_line_algorithm
sub plotLine(color as integer, x0 as integer, y0 as integer, x1 as integer, y1 as integer)
    dim dx as integer, dy as integer
    dim sx as integer, sy as integer
    dim error as integer
    dim addr as integer, topRow as boolean, cell as integer
    dim screenAddresses() as integer = { _
        0x400, 0x480, 0x500, 0x580, 0x600, 0x680, 0x700, 0x780, _
        0x428, 0x4a8, 0x528, 0x5a8, 0x628, 0x6a8, 0x728, 0x7a8, _
        0x450, 0x4d0, 0x550, 0x5d0, 0x650, 0x6d0, 0x750, 0x7d0 _
    }

    dx = abs(x1 - x0)
    if x0 < x1 then
        sx = 1
    else
        sx = -1
    end if
    dy = -abs(y1 - y0)
    if y0 < y1 then
        sy = 1
    else
        sy = -1
    end if
    error = dx + dy

    ' prep address and color
    addr = screenAddresses(y0/2)
    topRow = y0 mod 2 = 0
    color = color OR (color << 4)

    while true
        ' plot x0,y0
        cell = peek(addr+x0)
        if topRow then
            poke addr+x0, (cell AND 0xf0) OR (color AND 0x0f)
        else
            poke addr+x0, (cell AND 0x0f) OR (color AND 0xf0)
        end if

        if x0 = x1 and y0 = y1 then
            exit while
        end if
        e2 = 2 * error
        if e2 >= dy then
            if x0 = x1 then
                exit while
            end if
            error = error + dy
            x0 = x0 + sx
        end if
        if e2 <= dx then
            if y0 = y1 then
                exit while
            end if
            error = error + dx
            y0 = y0 + sy
            ' update address
            addr = screenAddresses(y0/2)
            topRow = y0 mod 2 = 0
        end if
    end while
end sub



dim c as integer, i as integer
dim x as integer, y as integer

gr

while peek(-16384) < 128
    c = (c + 1) mod 16
    i = (i + 3) mod 160

    if i < 40 then
        y = 0
        x = i
    elseif i < 80 then
        y = i - 40
        x = 39
    elseif i < 120 then
        x = 39 - (i - 80)
        y = 39
    else    ' i < 160
        y = 39 - (i - 120)
        x = 0
    end if

    plotLine(c,20,20,x,y)

end while

end
