' https://sites.google.com/site/drjohnbmatthews/apple2/lores

uses "text"
uses "lores"

GR
HOME
PRINT
FOR X = 4 TO 35
    COLOR(C)
    H = C > 9
    POKE 1616 + 128 * U + X, 176 + K * (C - 11 * H) + H
    VLIN(0,39,X)
    C = C + K
    K = K = 0
    U = U = K
NEXT X
END
