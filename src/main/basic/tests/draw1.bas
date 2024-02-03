' stupid drawing program
uses "text"
uses "lores"

text
home
gr

x=0
y=0

while true
    c = scrn(x,y)

    ' blink cursor
    color(15-c)
    plot(x,y)
    color(c)
    plot(x,y)

    ch = peek(-16384)
    if ch > 128 then
        poke -16368,0

        ' We don't have ELSEIF (or whatever) as a construct
        if ch >= asc("0") and ch <= asc("9") then
            color(ch - asc("0"))
            plot(x,y)
        end if
        if (ch = asc("U") or ch = asc("u")) and y > 0 then
            y = y - 1
        end if
        if (ch = asc("D") or ch = asc("d")) and y < 39 then
            y = y + 1
        end if
        if (ch = asc("L") or ch = asc("l")) and x > 0 then
            x = x - 1
        end if
        if (ch = asc("R") or ch = asc("r")) and x < 39 then
            x = x + 1
        end if

        home
        print x, y
    end if
end while
