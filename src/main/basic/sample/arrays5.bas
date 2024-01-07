uses "lores"

dim static y() as integer = { _
    15, 15, 15, 15, 14, _
    14, 14, 13, 13, 12, _
    11, 10, 9, 7, 5, 0 _
}

gr

while true
    for c=1 to 15
        print "COLOR=";c
        color=c
        for x=0 to 15
            plot(20+x,20+y(x))
            plot(20-x,20+y(x))
            plot(20+x,20-y(x))
            plot(20-x,20-y(x))
        next x
    next c
end while
