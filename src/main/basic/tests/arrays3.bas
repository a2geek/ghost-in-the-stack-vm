dim a(2) as integer
a(0)=5
a(1)=a(0)+5
a(2)=a(1)+5
print a(0), a(1), a(2)
print "LENGTH OF A() = "; ubound(a)

' intentionally generating an error
i=5:a(i)=i
end
