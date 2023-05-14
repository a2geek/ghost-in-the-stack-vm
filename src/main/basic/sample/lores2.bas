' Midpoint circle algorithm

text:home
gr

color=15
r=19

t1=r/19
x=r
y=0
loop:
    if x>=y then
        plot 20+x,20+y
        plot 20+x,19-y
        plot 19-x,20+y
        plot 19-x,19-y
        plot 20+y,20+x
        plot 20+y,19-x
        plot 19-y,20+x
        plot 19-y,19-x
        y=y+1
        t1=t1+y
        t2=t1-x
        if t2 >= 0 then
            t1=t2
            x=x-1
        end if
        goto loop
    end if
