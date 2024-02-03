' Some stupid testing of htab/vtab

uses "text"

text
home

' ARG was shortened but it looks like all 3 bytes are being used???'
' ZP locations seem to be clobbered! '
' for y = 5 to 20 step 3
'   vtab y
'   htab 35
'   print 35+y
' next y

for y = 20 to 5 step -3
  vtab(y)
  for x = 35 to 5 step -7
    htab(x)
    print x+y;
  next x
next y

' position nicely at end?
vtab(22)
print

end
