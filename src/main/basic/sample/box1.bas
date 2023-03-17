' stupid box that changes colors based on SCRN
'

text
home
gr

' Sort of the middle?
loop:
    iteration=iteration+1
    print iteration
    for y=15 to 25
        for x=15 to 25
            c = scrn(x,y)
            color= c+1
            plot x,y
        next x
    next y
    ' Need a REPEAT statement!!
    if peek(-16384) < 128 then goto loop

end
