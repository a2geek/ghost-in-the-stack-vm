' Test of ON GOSUB/RETURN

' Expected output:
'   0
'   1
'   ONE
'   2
'   ANOTHER
'   3
'   HI!!
'   4

print "TEST OF 'ON ... GOSUB' STATEMENT"

for i = 0 to 4
    print i
    on i gosub something1, something2, something3
next i
end

something1:
    print "ONE"
    return

something2:
    print "ANOTHER"
    return

something3:
    print "HI!!"
    return
