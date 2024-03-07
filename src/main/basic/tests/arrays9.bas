option strict

uses "math"
uses "text"

dim n(5) as integer
dim n0 as integer, n1 as integer, n2 as integer, n3 as integer, n4 as integer, n5 as integer
dim good as integer, bad as integer

sub check(i as integer, expected as integer)
    print "Index ";i;": ";n(i);" = ";expected;": ";
    if (n(i) = expected) then
        print "Ok."
        good = good + 1
    else
        print "Incorrect."
        bad = bad + 1
    end if
end sub


text:home

' expected results
print "1. Picking random numbers"
n0 = math.random()
n1 = math.random()
n2 = math.random()
n3 = math.random()
n4 = math.random()
n5 = math.random()

' assign values (in the worst way possible!!)
print "2. Uses ADDROF(..) to assign values"
pokew addrof(n(0)), n0
pokew addrof(n(1)), n1
pokew addrof(n(2)), n2
pokew addrof(n(3)), n3
pokew addrof(n(4)), n4
pokew addrof(n(5)), n5

' check results
print "3. Checks results via array indexes"
check(0, n0)
check(1, n1)
check(2, n2)
check(3, n3)
check(4, n4)
check(5, n5)

inverse
if bad = 0 then
    print "SUCCESS"
else
    print "FAILURE; GOOD=";good;", BAD=";bad
end if
normal
end
