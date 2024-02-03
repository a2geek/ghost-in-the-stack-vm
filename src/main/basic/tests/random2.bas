uses "math"
uses "text"
uses "lores"

text
home
gr

do
    c = rnd(16)
    x = rnd(40)
    y = rnd(40)
    color(c)
    plot(x,y)
loop while peek(-16384) < 128

poke -16368,0
print "DONE!"
end