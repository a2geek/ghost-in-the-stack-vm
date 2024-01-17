dim i as integer

for i = 1 to 10
  select case i
    case 1,3,5
      print "ONE, THREE, OR FIVE"
    case 2
      print "TWO"
    case 6 to 8
      print "6 TO 8"
    case is > 8
      print "9 OR LARGER"
    case else
      print "4 (DEFAULT)"
  end select
next i

print "END"
