uses "math"

text
home
gr

loop:
    c = rnd(16)
    x = rnd(40)
    y = rnd(40)
    color= c
    plot x,y
    if peek(-16384) < 128 then goto loop

poke -16368,0
print "DONE!"
end