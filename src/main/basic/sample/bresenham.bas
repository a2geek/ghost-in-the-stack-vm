
' See: https://en.wikipedia.org/wiki/Bresenham's_line_algorithm
sub plotLine(x0 as integer, y0 as integer, x1 as integer, y1 as integer)
    dim dx as integer, dy as integer
    dim sx as integer, sy as integer
    dim error as integer

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

    while true
        plot x0, y0
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
        end if
    end while
end sub



dim c as integer, i as integer
dim x as integer, y as integer

gr

while peek(-16384) < 128
    c = (c + 1) mod 16
    i = (i + 3) mod 160

    ' NOTE: Really need IF ELSEIF structure!!
    if i < 40 then
        y = 0
        x = i
    else
        if i < 80 then
            y = i - 40
            x = 39
        else
            if i < 120 then
                x = 39 - (i - 80)
                y = 39
            else
                if i < 160 then
                    y = 39 - (i - 120)
                    x = 0
                end if
            end if
        end if
    end if

    color= c
    plotLine(20,20,x,y)

end while

end
