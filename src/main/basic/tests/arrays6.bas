function arrayTotal(myArray() as integer) as integer
  dim total as integer, i as integer

  for i = 0 to ubound(myArray)-1
    total = total + myArray(i)
  next i

  return total
end function

dim a() as integer = { 1, 2, 3, 4 }
dim b() as integer = { 1, 3, 5, 7 }

print "TOTAL(A) = "; arrayTotal(a)
print "TOTAL(B) = "; arrayTotal(b)
end