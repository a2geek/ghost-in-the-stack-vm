option strict

uses "text"

sub accumulate1(byref total as integer, byval value as integer)
    total = total + value
end sub

function accumulate2(byval total as integer, byval value as integer) as integer
    total = total + value
    return total
end function

dim t1 as integer, t2 as integer, i as integer
const expected = 55     ' (10+11)/2

print "Accumulate1: ADDROF(T1)=";addrof(t1)
for i = 1 to 10
    accumulate1(t1, i)
    print "Accumulate1: total=";t1;", value=";i
next i

for i = 1 to 10
    t2 = accumulate2(t2, i)
    print "Accumulate2: total=";t2;", value=";i
next i

inverse
if t1 = expected and t2 = expected then
    print "SUCCESS"
else
    print "FAIL"
end if
normal

end
