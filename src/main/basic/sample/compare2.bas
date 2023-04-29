text
home

' cannot use a constant since those get optimized away!
dim a as integer, b as integer
a = -1
b = 2

print "A=";a
print "B=";b
print
print "       A A        A B        B A        B B"
print
print "<  ", (A<A),     (A<B),     (B<A),     (B<B)
print "<= ", (A<=A),    (A<=B),    (B<=A),    (B<=B)
print
print ">  ", (A>A),     (A>B),     (B>A),     (B>B)
print ">= ", (A>=A),    (A>=B),    (B>=A),    (B>=B)
print
print "=  ", (A=A),     (A=B),     (B=A),     (B=B)
print "<> ", (A<>A),    (A<>B),    (B<>A),    (B<>B)
print

end
