' Note the multiplication was left in here - needs to be rewritten by compiler.
' Sample code pulled from here: http://www.mrob.com/pub/xapple2/colors.html
GR
FOR C = 0 TO 14
    COLOR= C + 1
    FOR Y = C * 2 TO C * 2 + 10
        FOR X = C * 2 TO C * 2 + 10
            PLOT X,Y
        NEXT X
    NEXT Y
NEXT C
END
