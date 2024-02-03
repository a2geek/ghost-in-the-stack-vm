uses "text"

text : home

print "positive step"
for i = 1 to 10
    print i
    if i = 5 then
        exit for
    end if
next i

print "negative step"
for i = 10 to 1 step -1
    print i
    if i = 5 then
        exit for
    end if
next i

print "done"
end
