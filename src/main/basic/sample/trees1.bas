uses "text"
uses "lores"

sub drawtree(x0,y0)
    for x=1 to 4
        top=8-x*2+1
        for y=8 to top step -1
            color((y <> top)*4 + (y = top)*13)
            plot(x0+x,y0+y)
            plot(x0+8-x,y0+y)
        next y
    next x
    color(7)
    plot(x0+4,y0+9)
end sub

text : gr : home
print "Making Trees..."
for x=0 to 39 step 10
    for y=0 to 39 step 10
        drawtree(x,y)
    next y
next x
print "DONE!"
end
