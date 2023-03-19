text
home

' no strings yet - just need to remember!

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

print "OR ", (A or A),  (A or B),  (B or A),  (B or B)
print "AND", (A and A), (A and B), (B and A), (B and B)

end
