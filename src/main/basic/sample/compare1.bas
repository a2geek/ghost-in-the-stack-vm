text
home

' cannot use a constant since those get optimized away!
a = 1
b = 2

print "<  ", (A<A),     (A<B),     (B<A),     (B<B)
print "<= ", (A<=A),    (A<=B),    (B<=A),    (B<=B)
print
print ">  ", (A>A),     (A>B),     (B>A),     (B>B)
print ">= ", (A>=A),    (A>=B),    (B>=A),    (B>=B)
print
print "=  ", (A=A),     (A=B),     (B=A),     (B=B)
print "<> ", (A<>A),    (A<>B),    (B<>A),    (B<>B)
print

' cannot use a constant since those get optimized away!
a = 0
b = 1

' The crazy comparisons are to get a Boolean since we don't
' have that as an independent data type!
print "==>   F op F  F op T   T op F   T op T"
print
print "OR ", ((A=1) or (A=1)),  ((A=1) or (B=1)),  ((B=1) or (A=1)),  ((B=1) or (B=1))
print "AND", ((A=1) and (A=1)), ((A=1) and (B=1)), ((B=1) and (A=1)), ((B=1) and (B=1))
print "XOR", ((A=1) xor (A=1)), ((A=1) xor (B=1)), ((B=1) xor (A=1)), ((B=1) xor (B=1))
print

end
