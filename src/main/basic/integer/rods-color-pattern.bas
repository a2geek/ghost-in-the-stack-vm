' Rod's Color Pattern

GR
WHILE TRUE
    FOR W = 3 TO 50
        FOR I = 1 TO 19
            FOR J = 0 TO 19
                K = I + J
                COLOR= J * 3 / (I + 3) + I * W / 12
                PLOT I,K: PLOT K,I: PLOT 40 - I,40 - K: PLOT 40 - K,40 - I
                PLOT K,40 - I: PLOT 40 - I,K: PLOT I,40 - K: PLOT 40 - K,I
            NEXT J
        NEXT I
    NEXT W
END WHILE
