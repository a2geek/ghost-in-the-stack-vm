' ==========
' MOUSEMAZE
' ROB GREENE
' 06/21/83
' ==========
' this is a semi-direct translation
' and has a ton of repeated code...
' along with weird variable names!

option heap lomem=0x8000

uses "text"
uses "hires"
uses "prodos"
uses "strings"
uses "math"

const MOUSE_SHAPES = 0x6000
const TITLE_SHAPES = 0x7000
const CHAR_SHAPES = 0x8000

dim X as integer
dim L as integer, M as integer, N as integer
DIM P$(15,12), RB(12) as integer, RT(12) as integer

gosub initialize
gosub drawScreen
gosub demo
end

initialize:
    TEXT : HOME : SPEED(255) : NORMAL
    VTAB(12) : HTAB(10) : PRINT "MOUSE MAZE LOADING..."
    SCALE(1) : ROT(0)
    BLOAD("MOUSE.BIN", MOUSE_SHAPES)
    BLOAD("MOUSE.TITLE.BIN", TITLE_SHAPES)
    BLOAD("MOUSE.CHARS.BIN", CHAR_SHAPES)
    return

drawScreen:
    HGR2
    HCOLOR(3)
    FOR X = 0 TO 176 STEP 16
        HPLOT(0,X,238,X)
    NEXT X
    FOR X = 0 TO 240 STEP 17
        HPLOT(X,0,X,176)
    NEXT X
    HPLOTAT(0,0) : HPLOTTO(279,0) : HPLOTTO(279,191) : HPLOTTO(0,191) : HPLOTTO(0,0)

    shapetable(TITLE_SHAPES)

    M = 242:N = 19
    DRAW(13,M,N)
    N = N +16
    DRAW(15,M,N)
    N = N +16
    DRAW(21,M,N)
    N = N +16
    DRAW(19,M,N)
    N = N +16
    DRAW(5,M,N)
    N = N +16
    DRAW(13,M,N)
    N = N +16
    DRAW(1,M,N)
    N = N +16
    DRAW(26,M,N)
    N = N +16
    DRAW(5,M,N)

    M = 242:N = 179
    DRAW(18,M,N)
    M = M +12
    DRAW(10,M,N)
    M = M +12
    DRAW(7,M,N)

    shapetable(CHARS_SHAPES)
    return

' Was GOSUB 2260 with A$ and O.
' Also handing lower case instead of using embedded control characters
sub drawText(x as integer, text as string)
    dim ch as integer
    shapetable(CHAR_SHAPES)
    FOR Z = 0 TO LEN(text)
        ch = ascn(text,z) AND 0x7f
        ' Character range adjustment:
        '  $00..$1F => uppercase letters (ASCII $40-$5F)
        '  $20..$3F => numbers/symbols
        '  $40..$5F => lowercase letters (ASCII $60-$7F)
        if ch >= 0x60 then
            ch = ch - 0x20
        elseif ch >= 0x40 then
            ch = ch - 0x40
        end if
        IF ch >= 0 THEN
            HCOLOR(3)
            DRAW(ch,x,180)
        END IF
        x = x + 7
    NEXT Z
    shapetable(MOUSE_SHAPES)
end sub

' line 300
drawMap:
    FOR A = 1 TO 14
        FOR B = 1 TO 11
            'IF  PEEK( -16384) =  ASC("G") +128  THEN  PRINT  CHR$(4);"RUN MOUSEMAZE"
            'IF  PEEK( -16384) =  ASC("H") +128  THEN  PRINT  CHR$(4);"RUN MOUSE HELP"
            HCOLOR(3): DRAW(4, (17*A)-16, 16*B)
            HCOLOR(0): DRAW(4, (17*A)-16, 16*B): HCOLOR(3)
            IF P$(A,B) = "Z"  THEN  DRAW(6, (17*A)-16, 16*B)
            IF P$(A,B) = "M"  THEN  DRAW(2, (17*A)-16, (16*B)-1)
            IF P$(A,B) = "R"  THEN  DRAW(7, (17*A)-16, 16*B)
            IF P$(A,B) = "E"  THEN  DRAW(3, (17*A)-16, 16*B)
            IF P$(A,B) = "B"  THEN  DRAW(8, (17*A)-16, 16*B)
            IF P$(A,B) = "D"  THEN  DRAW(5, (17*A)-16, 16*B)
            IF P$(A,B) = "N"  THEN  DRAW(11, (17*A)-16, 16*B)
        NEXT B
    NEXT A
    RETURN

line410:
    IF Y <1  THEN  RETURN
    IF P$(X,Y -1) = " "  THEN GOTO line460
    IF P$(X,Y -1) = "D"  THEN M$ = "L": RETURN
    IF P$(X,Y -1) = "N"  THEN  HCOLOR(0): DRAW(4, (X*17)-16, Y*16):P$(X,Y) = " ": GOTO line480
    CALL  -1052: RETURN
line460:
    HCOLOR(0): DRAW(4,(X *17) -16,(Y *16)): HCOLOR(3): DRAW(2,(X *17) -16,(Y *16) -17)
    P$(X,Y) = " ":P$(X,Y -1) = "M":Y = Y -1: RETURN
line480:
    Y = Y -1
playerHitMine:
    FOR T = 1 TO 5
        FOR S = 9 TO 10
            HCOLOR(3): DRAW(S,(X *17) -16,(Y *16))
            SOUND =  PEEK( -16336)
            HCOLOR(0): DRAW(S,(X *17) -16,(Y *16))
        NEXT S
    NEXT T
    HCOLOR(3): DRAW(6,(X *17) -16,(Y *16))
    M$ = "D"
    RETURN

line490:
    IF Y >11  THEN  RETURN
    IF P$(X,Y +1) = " "  THEN GOTO line540
    IF P$(X,Y +1) = "D"  THEN M$ = "L": RETURN
    IF P$(X,Y +1) = "N"  THEN  HCOLOR(0): DRAW(4,(X *17) -16,(Y *16)):P$(X,Y) = " ": GOTO line560
    CALL  -1052: RETURN
line540:
    HCOLOR(0): DRAW(4,(X *17) -16,(Y *16)): HCOLOR(3): DRAW(2,(X *17) -16,(Y *16) +15)
    P$(X,Y) = " ":P$(X,Y +1) = "M":Y = Y +1: RETURN
line560:
    Y = Y +1
    goto playerHitMine

line570:
    IF X <1  THEN  RETURN
    IF P$(X -1,Y) = " "  THEN GOTO line620
    IF P$(X -1,Y) = "D"  THEN M$ = "L": RETURN
    IF P$(X -1,Y) = "N"  THEN  HCOLOR(0): DRAW(4, (X *17) -16, (Y *16)):P$(X,Y) = " ": GOTO line640
    CALL  -1052: RETURN
line620:
    HCOLOR(0): DRAW(4, (X *17) -16, (Y *16)): HCOLOR(3): DRAW(2, (X *17) -33, (Y *16))
    P$(X,Y) = " ":P$(X -1,Y) = "M":X = X -1: RETURN
line640:
    X = X -1
    goto playerHitMine

line650:
    IF X >14  THEN  RETURN 
    IF P$(X +1,Y) = " "  THEN GOTO line700
    IF P$(X +1,Y) = "D"  THEN M$ = "L": RETURN 
    IF P$(X +1,Y) = "N"  THEN  HCOLOR(0): DRAW(4, (X *17) -16, (Y *16)):P$(X,Y) = " ": GOTO line720
    CALL  -1052: RETURN
line700: 
    HCOLOR(0): DRAW(4, (X *17) -16, (Y *16)): HCOLOR(3): DRAW(2, (X *17) +1, (Y *16))
    P$(X,Y) = " ":P$(X +1,Y) = "M":X = X +1: RETURN
line720: 
    X = X +1
    goto playerHitMine

line730:
    IF X >13  THEN  RETURN
    IF P$(X +1,Y) = "B"  THEN F = X +1:G = Y: GOTO line1960
    IF P$(X +1,Y) = "D"  THEN  RETURN
    IF P$(X +1,Y) = "Z"  THEN  RETURN
    IF P$(X +1,Y) = "R"  THEN  RETURN
    FOR T = 1 TO 5
        FOR S = 9 TO 10
            HCOLOR(3): DRAW(S, ((X +1) *17) -16, (Y *16))
            SOUND =  PEEK( -16336)
            HCOLOR(0): DRAW(S, ((X +1) *17) -16, (Y *16))
        NEXT S
    NEXT T
    IF P$(X +1,Y) = "E"  THEN P$(X +1,Y) = "R": HCOLOR(3): DRAW(7, ((X +1) *17) -16, (Y *16)): RETURN
    P$(X +1,Y) = " ": RETURN

line810:
    IF X >13  OR Y <2  THEN  RETURN
    IF X >13  AND Y <2  THEN  RETURN
    IF P$(X +1,Y -1) = "B"  THEN F = X +1:G = Y -1: GOTO line1960
    IF P$(X +1,Y -1) = "D"  THEN  RETURN
    IF P$(X +1,Y -1) = "Z"  THEN  RETURN
    IF P$(X +1,Y -1) = "R"  THEN  RETURN
    FOR T = 1 TO 5
        FOR S = 9 TO 10
            HCOLOR(3): DRAW(S, ((X +1) *17) -16, ((Y -1) *16))
            SOUND =  PEEK( -16336)
            HCOLOR(0): DRAW(S, ((X +1) *17) -16, ((Y -1) *16))
        NEXT S
    NEXT T
    IF P$(X +1,Y -1) = "E"  THEN P$(X +1,Y -1) = "R": HCOLOR(3): DRAW(7, ((X +1) *17) -16, ((Y -1) *16)): RETURN
    P$(X +1,Y -1) = " ": RETURN

line900:
    IF X <2  THEN  RETURN
    IF P$(X -1,Y) = "B"  THEN F = X -1:G = Y: GOTO line1960
    IF P$(X -1,Y) = "D"  THEN  RETURN
    IF P$(X -1,Y) = "Z"  THEN  RETURN
    IF P$(X -1,Y) = "R"  THEN  RETURN
    FOR T = 1 TO 5
        FOR S = 9 TO 10
            HCOLOR(3): DRAW(S, ((X -1) *17) -16, (Y *16))
            SOUND =  PEEK( -16336)
            HCOLOR(0): DRAW(S, ((X -1) *17) -16, (Y *16))
        NEXT S
    NEXT T
    IF P$(X -1,Y) = "E"  THEN P$(X -1,Y) = "R": HCOLOR(3): DRAW(7, ((X -1) *17) -16, (Y *16)): RETURN
    P$(X -1,Y) = " ": RETURN

line980:
    IF X >13  OR Y >10  THEN  RETURN
    IF X >13  AND Y >10  THEN  RETURN
    IF P$(X +1,Y +1) = "B"  THEN F = X +1:G = Y +1: GOTO line1960
    IF P$(X +1,Y +1) = "D"  THEN  RETURN
    IF P$(X +1,Y +1) = "Z"  THEN  RETURN
    IF P$(X +1,Y +1) = "R"  THEN  RETURN
    FOR T = 1 TO 5
        FOR S = 9 TO 10
            HCOLOR(3): DRAW(S, ((X +1) *17) -16, ((Y +1) *16))
            SOUND =  PEEK( -16336)
            HCOLOR(0): DRAW(S, ((X +1) *17) -16, ((Y +1) *16))
        NEXT S
    NEXT T
    IF P$(X +1,Y +1) = "E"  THEN P$(X +1,Y +1) = "R": HCOLOR(3): DRAW(7, ((X +1) *17) -16, ((Y +1) *16)): RETURN
    P$(X +1,Y +1) = " ": RETURN

line1070:
    IF X <2  OR Y <2  THEN  RETURN 
    IF X <2  AND Y <2  THEN  RETURN 
    IF P$(X -1,Y -1) = "B"  THEN F = X -1:G = Y -1: GOTO line1960
    IF P$(X -1,Y -1) = "D"  THEN  RETURN 
    IF P$(X -1,Y -1) = "Z"  THEN  RETURN 
    IF P$(X -1,Y -1) = "R"  THEN  RETURN 
    FOR T = 1 TO 5
        FOR S = 9 TO 10
            HCOLOR(3): DRAW(S, ((X -1) *17) -16, ((Y -1) *16))
            SOUND =  PEEK( -16336)
            HCOLOR(0): DRAW(S, ((X -1) *17) -16, ((Y -1) *16))
        NEXT S
    NEXT T
    IF P$(X -1,Y -1) = "E"  THEN P$(X -1,Y -1) = "R": HCOLOR(3): DRAW(7, ((X -1) *17) -16, ((Y -1) *16)): RETURN 
    P$(X -1,Y -1) = " ": RETURN 

line1160:
    IF X <2  OR Y >10  THEN  RETURN 
    IF X <2  AND Y >10  THEN  RETURN 
    IF P$(X -1,Y +1) = "B"  THEN F = X -1:G = Y +1: GOTO line1960
    IF P$(X -1,Y +1) = "D"  THEN  RETURN 
    IF P$(X -1,Y +1) = "Z"  THEN  RETURN 
    IF P$(X -1,Y +1) = "R"  THEN  RETURN 
    FOR T = 1 TO 5
        FOR S = 9 TO 10
            HCOLOR(3): DRAW(S, ((X -1) *17) -16, ((Y +1) *16))
            SOUND =  PEEK( -16336)
            HCOLOR(0): DRAW(S, ((X -1) *17) -16, ((Y +1) *16))
        NEXT S
    NEXT T
    IF P$(X -1,Y +1) = "E"  THEN P$(X -1,Y +1) = "R": HCOLOR(3): DRAW(7, ((X -1) *17) -16, ((Y +1) *16)): RETURN 
    P$(X -1,Y +1) = " ": RETURN 

line1250:
    IF Y <2  THEN  RETURN 
    IF P$(X,Y -1) = "B"  THEN F = X:G = Y -1: GOTO line1960
    IF P$(X,Y -1) = "D"  THEN  RETURN 
    IF P$(X,Y -1) = "Z"  THEN  RETURN 
    IF P$(X,Y -1) = "R"  THEN  RETURN 
    FOR T = 1 TO 5
        FOR S = 9 TO 10
            HCOLOR(3): DRAW(S, (X *17) -16, ((Y -1) *16))
            SOUND =  PEEK( -16336)
            HCOLOR(0)
            DRAW(S, (X *17) -16, ((Y -1) *16))
        NEXT S
    NEXT T
    IF P$(X,Y -1) = "E"  THEN P$(X,Y -1) = "R": HCOLOR(3): DRAW(7, ((X *17) -16), ((Y -1) *16)): RETURN 
    P$(X,Y -1) = " ": RETURN 

line1330:
    IF Y >10  THEN  RETURN 
    IF P$(X,Y +1) = "B"  THEN F = X:G = Y +1: GOTO line1960
    IF P$(X,Y +1) = "D"  THEN  RETURN 
    IF P$(X,Y +1) = "Z"  THEN  RETURN 
    IF P$(X,Y +1) = "R"  THEN  RETURN 
    FOR T = 1 TO 5
        FOR S = 9 TO 10
            HCOLOR(3): DRAW(S, (X *17) -16, ((Y +1) *16))
            SOUND =  PEEK( -16336)
            HCOLOR(0): DRAW(S, (X *17) -16, ((Y +1) *16))
        NEXT S
    NEXT T
    IF P$(X,Y +1) = "E"  THEN P$(X,Y +1) = "R": HCOLOR(3): DRAW(7, ((X *17) -16), ((Y +1) *16)): RETURN 
    P$(X,Y +1) = " ": RETURN 

line1410:
    FOR K = 1 TO 4: FOR J = 1 TO ((LV *2) +2)
        IF P$(RB(J),RT(J)) <> "E" THEN CONTINUE FOR
        GOSUB line1940
        HCOLOR(0): DRAW(4, ((RB(J) *17) -16), (RT(J) *16)):G = RB(J):H = RT(J): HCOLOR(3)
        IF RB(J) >X  AND P$(RB(J) -1,RT(J)) = " "  THEN RB(J) = RB(J) -1: GOTO line1470
        IF RB(J) <X  AND P$(RB(J) +1,RT(J)) = " "  THEN RB(J) = RB(J) +1
line1470:
        IF RT(J) >Y  AND P$(RB(J),RT(J) -1) = " "  THEN RT(J) = RT(J) -1: GOTO line1490
        IF RT(J) <Y  AND P$(RB(J),RT(J) +1) = " "  THEN RT(J) = RT(J) +1
line1490:
        HCOLOR(3): DRAW(3, ((RB(J) *17) -16), (RT(J) *16)):P$(G,H) = " ":P$(RB(J),RT(J)) = "E": GOSUB line1500
    NEXT J: NEXT K
    RETURN 

line1500:
    IF RB(J) = X  THEN SX = RB(J)
    IF RT(J) = Y  THEN SY = RT(J)
    IF RB(J) <X  THEN SX = RB(J) +2
    IF RB(J) >X  THEN SX = RB(J) -2
    IF RT(J) <Y  THEN SY = RT(J) +2
    IF RT(J) >Y  THEN SY = RT(J) -2
    IF SX <1  THEN SX = 1
    IF SX >14  THEN SX = 14
    IF SY <1  THEN SY = 1
    IF SY >11  THEN SY = 11
    IF P$(SX,SY) = "R"  THEN  RETURN 
    IF P$(SX,SY) = "D"  THEN  RETURN 
    IF P$(SX,SY) = "Z"  THEN  RETURN 
    IF P$(SX,SY) = "E"  THEN  RETURN 
    IF P$(SX,SY) = "B"  THEN GOTO line1930
    IF P$(SX,SY) = "N"  THEN  RETURN 
    FOR T = 1 TO 5: FOR S = 9 TO 10: HCOLOR(3): DRAW(S, (SX *17) -16, (SY *16)):SOUND =  PEEK( -16336): HCOLOR(0): DRAW(S, (SX *17) -16, (SY *16)): NEXT S: NEXT T
    IF P$(SX,SY) = "M"  THEN  GOTO line1790
    NL =  RND(8-LV)+1: IF NL = 8 -LV  THEN GOTO line1700
    RETURN
line1700: 
    IF P$(RB(J) +1,RT(J)) <>" "  THEN  RETURN
    P$(RB(J) +1,RT(J)) = "N": HCOLOR(3): DRAW(11, ((RB(J) +1) *17) -16, (RT(J) *16)): RETURN 

' line 1720
setLevel:
    FOR A = 0 TO 15: FOR B = 0 TO 12:P$(A,B) = " ": NEXT B: NEXT A
    FOR A = 0 TO 15:P$(A,0) = "W":P$(A,12) = "W": NEXT A: FOR B = 0 TO 12:P$(0,B) = "W":P$(15,B) = "W": NEXT B
    P$(1,6) = "M":P$(14,8) = "E":P$(14,7) = "E":P$(14,5) = "E":P$(14,4) = "E":P$(14,6) = "D"
    gosub setLevelBombs
    X = 1:Y = 6:LV = 1: FOR K = 1 TO 4:RB(K) = 14: NEXT K:RT(1) = 4:RT(2) = 5:RT(3) = 7:RT(4) = 8
    FOR U = 1 TO ML: HCOLOR(3): DRAW(2, 256, U*16): NEXT U
    RETURN

line1790:
    ML = ML -1: IF ML <1  THEN GOTO line1840
    M$ = " "
    HCOLOR(3): DRAW(6, ((X *17) -16), (Y *16)):P$(X,Y) = "Z":P$(1,6) = "M":X = 1:Y = 6: HCOLOR(3): DRAW(2, ((X *17) -16), (Y *16))
    HCOLOR(0): DRAW(4, 256, ((ML +1) *16))
    RETURN
line1840:
    GOTO line40

' line 1890
setLevelBombs:
    FOR A = 1 TO ((LV +1) *3)
        do
            F =  RND(14)+1:G = RND(11)+1
        loop until P$(F,G) = " "
        P$(F,G) = "B"
    NEXT A
    RETURN

line1930:
    F = SX:G = SY: GOTO line1960
line1940:
    FOR A = 1 TO (LV *2)
        F =  RND(14)+1:G =  RND(11) +1
        IF P$(F,G) = "B"  THEN
            EXIT FOR
        END IF
    NEXT A
    IF P$(F,G) <> "B"  THEN  RETURN
line1960:
    FOR T = 1 TO 5: FOR S = 9 TO 10: HCOLOR(3): DRAW(S, ((F *17) -16), (G *16)):SOUND =  PEEK( -16336): HCOLOR(0): DRAW(S, ((F *17) -16), (G *16)): NEXT S: NEXT T
    RD = 1
    FOR H = F -RD TO F +RD: FOR I = G -RD TO G +RD
        IF H <1  OR H >11  THEN GOTO line2040
        IF I <1  OR I >14  THEN GOTO line2040
        IF P$(H,I) = "M"  THEN M$ = "D"
        IF P$(H,I) = "D"  THEN DR$ = "K":D = H:R = I: GOTO line2040
        P$(H,I) = " "
line2040:
    NEXT : NEXT
    FOR H = F -RD TO F +RD: FOR I = G -RD TO G +RD
        IF H <1  OR H >14  THEN GOTO line2110
        IF I <1  OR I >11  THEN GOTO line2110
        HCOLOR(3): DRAW(9, ((H *17) -16), (I *16))
        HCOLOR(0): DRAW(10, ((H *17) -16), (I *16))
        SOUND =  PEEK( -16336)
line2110:
    NEXT I: NEXT H
    FOR H = F -RD TO F +RD: FOR I = G -RD TO G +RD
        IF H <1  OR H >14  THEN GOTO line2180
        IF I <1  OR I >11  THEN GOTO line2180
        HCOLOR(3): DRAW(10, ((H *17) -16), (I *16))
        HCOLOR(0): DRAW(9, ((H *17) -16), (I *16))
        SOUND =  PEEK( -16336)
line2180:
    NEXT I: NEXT H
    FOR H = F -RD TO F +RD: FOR I = G -RD TO G +RD
        IF H <1  OR H >14  THEN GOTO line2230
        IF I <1  OR I >11  THEN GOTO line2230
        HCOLOR(0): DRAW(4, ((H *17) -16), (I *16))
line2230:
    NEXT I: NEXT H
    IF DR$ = "K"  THEN  HCOLOR(3): DRAW(5, ((D *17) -16), (R *16))
    RETURN

' line 2280
eraseText:
    HCOLOR(0): FOR L = 177 TO 190: HPLOT(1,L,238,L): NEXT : RETURN

demo:
    gosub eraseText
    POKE  -16304,0: POKE  -16297,0: POKE  -16302,0: POKE  -16299,0
    X = 1:Y = 6:LV = 0:ML = 3
    drawText(7, "Press-'G' for game, 'H' for help.")
line40:
    gosub setLevel
    gosub drawMap
line60:
    FOR PL = 1 TO 50
line70:
        K = rnd(77)+1: K = K+140
        IF K = 141  THEN  GOSUB line410: GOTO line70
        IF K = 175  THEN  GOSUB line490: GOTO line70
        IF K = 136  THEN  GOSUB line570: GOTO line70
        IF K = 149  THEN  GOSUB line650: GOTO line70
        IF K = 196  THEN  GOSUB line730: GOTO line70
        IF K = 197  THEN  GOSUB line810: GOTO line70
        IF K = 193  THEN  GOSUB line900: GOTO line70
        IF K = 195  THEN  GOSUB line980: GOTO line70
        IF K = 209  THEN  GOSUB line1070: GOTO line70
        IF K = 218  THEN  GOSUB line1160: GOTO line70
        IF K = 215  THEN  GOSUB line1250: GOTO line70
        IF K = 216  THEN  GOSUB line1330: GOTO line70
        '200  IF  PEEK( -16384) =  ASC("G") +128  THEN  PRINT  CHR$(4);"RUN MOUSEMAZE"
        '210  IF  PEEK( -16384) =  ASC("H") +128  THEN  PRINT  CHR$(4);"RUN MOUSE HELP"
        GOSUB line1940
        IF M$ = "D"  THEN  GOTO line270
        IF M$ = "L"  THEN M$ = " ": GOTO line40
    NEXT PL
    GOSUB line1410
line270:
    IF M$ = "D"  THEN  GOSUB line1790
    GOTO line60

    end
