option strict

uses "text"

' detect when code generation (apparently only multidimensional arrays?)
' reuses a temp variable too aggressively

dim n(5,5) as integer
dim m() as integer = { 0, 1, 2, 3, 4, 5 }
dim i as integer, j as integer, x as integer

text:home

' initialize
print "TEST GRID"
print
x = 0
print "    00  01  02  03  04  05"
print "---------------------------"
for i = 0 to ubound(n,1)
    if i < 10 then print "0";
    print i;"- ";
    for j = 0 to ubound(n,2)
        n(i,j) = x
        if x < 10 then print " ";
        print n(i,j);", ";
        x += 1
    next j
    print
next i
print "---------------------------"

i = 1
j = 3

inverse
if n(m(i),m(j)) = n(1,3) then
    print "SUCCESS"
else
    print "FAILURE; EXPECTING ";n(1,3);" AT 1,3 BUT GOT ";n(m(i),m(j))
end if
normal
end

