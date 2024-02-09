' ==========
' MOUSEMAZE
' ROB GREENE
' 06/21/83
' ==========

uses "text"
uses "hires"
uses "prodos"

const MOUSE_SHAPES = 0x6000
const TITLE_SHAPES = 0x7000
const CHAR_SHAPES = 0x8000

dim X as integer
dim M as integer, N as integer

gosub initialize
gosub drawScreen
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
