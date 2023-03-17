text
home

' no strings yet - just need to remember!

' cannot use a constant since those get optimized away!
a = 1
b = 2

print 1, (A<A),     (A<B),     (B<A),     (B<B)
print 2, (A<=A),    (A<=B),    (B<=A),    (B<=B)
print
print 3, (A>A),     (A>B),     (B>A),     (B>B)
print 4, (A>=A),    (A>=B),    (B>=A),    (B>=B)
print
print 5, (A=A),     (A=B),     (B=A),     (B=B)
print 6, (A<>A),    (A<>B),    (B<>A),    (B<>B)
print

' cannot use a constant since those get optimized away!
a = 0
b = 1

print 7, (A or A),  (A or B),  (B or A),  (B or B)
print 8, (A and A), (A and B), (B and A), (B and B)

end
