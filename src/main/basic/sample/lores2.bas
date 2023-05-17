' Midpoint circle algorithm

text:home
gr

for c=15 to 1 step - 1
    color=c
    r=c+4

    t1=r/16
    x=r
    y=0
    do until x < y
        hlin 19-x,20+x at 20+y
        hlin 19-x,20+x at 19-y
        hlin 19-y,20+y at 20+x
        hlin 19-y,20+y at 20-x
        y=y+1
        t1=t1+y
        t2=t1-x
        if t2 >= 0 then
            t1=t2
            x=x-1
        end if
    loop
next c
