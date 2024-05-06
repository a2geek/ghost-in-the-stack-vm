uses "math"
uses "lores"

' See: https://en.wikipedia.org/wiki/Bresenham's_line_algorithm
sub plotLine(x0 as integer, y0 as integer, x1 as integer, y1 as integer)
    dim dx as integer, dy as integer
    dim sx as integer, sy as integer
    dim err as integer

    dx = abs(x1 - x0)
    sx = 1 if x0 < x1 else -1
    dy = -abs(y1 - y0)
    sy = 1 if y0 < y1 else -1
    err = dx + dy

    while true
        plot(x0, y0)
        if x0 = x1 and y0 = y1 then
            exit while
        end if
        e2 = 2 * err
        if e2 >= dy then
            if x0 = x1 then
                exit while
            end if
            err = err + dy
            x0 = x0 + sx
        end if
        if e2 <= dx then
            if y0 = y1 then
                exit while
            end if
            err = err + dx
            y0 = y0 + sy
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

    color(c)
    plotLine(20,20,x,y)

end while

end
