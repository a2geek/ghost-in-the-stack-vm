text
home

' cannot use a constant since those get optimized away!
dim a as integer, b as integer
a = 1
b = 200

print "A=";a
print "B=";b
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

' cannot use a constant since those get optimized away!
dim t as boolean, f as boolean
t = true
f = false

print "==>   F op F  F op T   T op F   T op T"
print "T=";t
print "F=";f
print
print "OR ", (f or f),  (f or t),  (t or f),  (t or t)
print "AND", (f and f), (f and t), (t and f), (t and t)
print "XOR", (f xor f), (f xor t), (t xor f), (t xor t)
print
print "NOT", not f, not t

end
