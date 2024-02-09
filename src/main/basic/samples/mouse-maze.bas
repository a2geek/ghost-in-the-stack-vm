' ==========
' MOUSEMAZE
' ROB GREENE
' 06/21/83
' ==========
' note: needs heap

uses "text"
uses "hires"
uses "prodos"
uses "strings"
uses "math"

const MOUSE_SHAPES = 0x6000
const TITLE_SHAPES = 0x7000
const CHAR_SHAPES = 0x8000

' "W"
const SHAPE_W = 1
' "M"
const SHAPE_MOUSE = 2
' "E"
const SHAPE_ROBOT = 3
' "D"
const SHAPE_EXIT = 5
' "Z"
const SHAPE_DEAD_MOUSE = 6
' "R"
const SHAPE_DEAD_ROBOT = 7
' "B"
const SHAPE_BOMB = 8
' "N"
const SHAPE_MINE = 11

dim X as integer
dim L as integer, M as integer, N as integer
DIM map(15,12) as integer, RB(12) as integer, RT(12) as integer

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

setLevel:
    FOR A = 0 TO 15: FOR B = 0 TO 12:map(A,B) = 0: NEXT B: NEXT A
    FOR A = 0 TO 15:map(A,0) = SHAPE_W:map(A,12) = SHAPE_W: NEXT A
    FOR B = 0 TO 12:map(0,B) = SHAPE_W:map(15,B) = SHAPE_W: NEXT B
    map(1,6) = SHAPE_MOUSE:map(14,8) = SHAPE_ROBOT
    map(14,7) = SHAPE_ROBOT:map(14,5) = SHAPE_ROBOT
    map(14,4) = SHAPE_ROBOT
    map(14,6) = SHAPE_EXIT
    FOR A = 1 TO ((LV +1) *3)
        do
            F = rnd(14)+1:G = rnd(11)+1
        loop until map(F,G) = 0
        map(F,G) = SHAPE_BOMB
    NEXT A
    X = 1:Y = 6:LV = 1
    FOR K = 1 TO 4:RB(K) = 14: NEXT K
    RT(1) = 4:RT(2) = 5:RT(3) = 7:RT(4) = 8
    FOR U = 1 TO ML: HCOLOR(3): DRAW(2, 256, U*16): NEXT U
    RETURN

drawMap:
    FOR A = 1 TO 14
        FOR B = 1 TO 11
            ' FIXME
            'IF  PEEK( -16384) =  ASC("G") +128  THEN  PRINT  CHR$(4);"RUN MOUSEMAZE"
            'IF  PEEK( -16384) =  ASC("H") +128  THEN  PRINT  CHR$(4);"RUN MOUSE HELP"
            HCOLOR(3): DRAW(4, (17 *A)-16, (16*B))
            HCOLOR(0): DRAW(4, (17 *A)-16, (16*B))
            HCOLOR(3)
            IF map(A,B) = SHAPE_DEAD_MOUSE  THEN  DRAW(6, (17*A)-16, 16*B)
            IF map(A,B) = SHAPE_MOUSE  THEN  DRAW(2, (17*A)-16, (16*B)-1)
            IF map(A,B) = SHAPE_DEAD_ROBOT  THEN  DRAW(7, (17*A)-16, 16*B)
            IF map(A,B) = SHAPE_ROBOT  THEN  DRAW(3, (17*A)-16, 16*B)
            IF map(A,B) = SHAPE_BOMB  THEN  DRAW(8, (17*A)-16, 16*B)
            IF map(A,B) = SHAPE_EXIT  THEN  DRAW(5, (17*A)-16, 16*B)
            IF map(A,B) = SHAPE_MINE  THEN  DRAW(11, (17*A)-16, 16*B)
        NEXT B
    NEXT A
    RETURN

eraseText:
    HCOLOR(0): FOR L = 177 TO 190: HPLOT(1,L,238,L): NEXT L : RETURN

moveUp:
    IF Y <1  THEN  RETURN
    IF map(X,Y-1) = 0  THEN
        HCOLOR(0)
        DRAW(4, (X*17)-16, Y*16)
        HCOLOR(3)
        DRAW(2, (X*17)-16, (Y*16)-17)
        map(X,Y) = 0
        map(X,Y-1) = SHAPE_MOUSE
        Y = Y-1
    ELSEIF map(X,Y-1) = SHAPE_EXIT  THEN
        'M$ = "L"
    ELSEIF map(X,Y -1) = SHAPE_MINE  THEN
        HCOLOR(0)
        DRAW(4, (X*17)-16, Y*16)
        map(X,Y) = 0
        Y = Y-1
        FOR T = 1 TO 5
            FOR S = 9 TO 10
                HCOLOR(3)
                DRAW(S, (X*17)-16, Y*16)
                SOUND =  PEEK( -16336)
                HCOLOR(0)
                DRAW(S, (X*17)-16, Y*16)
            NEXT S
        NEXT T
        HCOLOR(3)
        DRAW(6, (X*17)-16, Y*16)
        'M$ = "D"
    ELSE
        CALL  -1052
    END IF
    RETURN

demo:
    gosub eraseText
    POKE  -16304,0: POKE  -16297,0: POKE  -16302,0: POKE  -16299,0
    X = 1:Y = 6:LV = 0:ML = 3
    drawText(7, "Press 'G' for game, 'H' for help.")
    gosub setLevel
    gosub drawMap
    while true
        FOR PL = 1 TO 50
            ' line 70
            K = rnd(77)+1:K = K + 140
            IF K = 141  THEN
                GOSUB moveUp
            ELSEIF K = 175  THEN
                'GOSUB 490
            ELSEIF K = 136  THEN
                'GOSUB 570
            ELSEIF K = 149  THEN
                'GOSUB 650
            ELSEIF K = 196  THEN
                'GOSUB 730
            ELSEIF K = 197  THEN
                'GOSUB 810
            ELSEIF K = 193  THEN
                'GOSUB 900
            ELSEIF K = 195  THEN
                'GOSUB 980
            ELSEIF K = 209  THEN
                'GOSUB 1070
            ELSEIF K = 218  THEN
                'GOSUB 1160
            ELSEIF K = 215  THEN
                'GOSUB 1250
            ELSEIF K = 216  THEN
                'GOSUB 1330
            ELSEIF  PEEK( -16384) =  ASC("G") +128  THEN
                'PRINT  CHR$(4);"RUN MOUSEMAZE"
            ELSEIF  PEEK( -16384) =  ASC("H") +128  THEN
                'PRINT  CHR$(4);"RUN MOUSE HELP"
            ELSE
                'GOSUB 1940
                'IF M$ = "D"  THEN  GOTO 270
                'IF M$ = "L"  THEN M$ = " ": GOTO 40
            END IF
        NEXT PL
        'GOSUB 1410
        ' line 270
        'IF M$ = "D"  THEN  GOSUB 1790
    end while