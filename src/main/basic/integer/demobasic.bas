
GR
POKE -16368,0

forever:
    FOR I=20 TO 1 STEP -1
        C=(C+3) MOD 16
        COLOR= C
        FOR J=0 TO 19
            K=I+J
            PLOT I,K
            PLOT K,I
            PLOT 40-I,40-K
            PLOT 40-K,40-I
            PLOT K,40-I
            PLOT 40-I,K
            PLOT I,40-K
            PLOT 40-K,I
        NEXT J
    NEXT I
    IF PEEK (-16384) < 128 THEN
        GOTO forever
    END IF
    END