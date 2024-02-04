' multidimensional arrays!
uses "lores"

dim xy(5,5) as integer
dim x as integer, y as integer
dim i as integer, y as integer

' pick colors
for x = 1 to 5
    for y = 1 to 5
        xy(x,y) = math.rnd(16)
    next y
next x

' draw
gr
for i = 0 to 39 step 5
    for j = 0 to 39 step 5
        for x = 1 to 5
            for y = 1 to 5
                color(xy(x,y))
                plot(x-1+i,y-1+j)
            next y
        next x
    next j
next i

print "SHOULD BE A 5X5 COLOR GRID"
end
