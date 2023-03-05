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