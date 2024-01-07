uses "lores"

dim y(15)
y(0)=15: y(1)=15: y(2)=15: y(3)=15: y(4)=14
y(5)=14: y(6)=14: y(7)=13: y(8)=13: y(9)=12
y(10)=11: y(11)=10: y(12)=9: y(13)=7: y(14)=5: y(15)=0

gr

while true
    for c=1 to 15
        print "COLOR=";c
        color(c)
        for x=0 to 15
            plot(20+x,20+y(x))
            plot(20-x,20+y(x))
            plot(20+x,20-y(x))
            plot(20-x,20-y(x))
        next x
    next c
end while
