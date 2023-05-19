' Use the number keys to change tones

initialize:
    ' Moved this to $300 to not conflict with interpreter
    AD=768
    POKE AD+2,173: POKE AD+3,48: POKE AD+4,192      ' 0302-   AD 30 C0    LDA   $C030
    POKE AD+5,136                                   ' 0305-   88          DEY
    POKE AD+6,208: POKE AD+7,5                      ' 0306-   D0 05       BNE   $030D
    POKE AD+8,206: POKE AD+9,1: POKE AD+10,3        ' 0308-   CE 01 03    DEC   $0301
    POKE AD+11,240: POKE AD+12,9                    ' 030B-   F0 09       BEQ   $0316
    POKE AD+13,202                                  ' 030D-   CA          DEX
    POKE AD+14,208: POKE AD+15,245                  ' 030E-   D0 F5       BNE   $0305
    POKE AD+16,174: POKE AD+17,0: POKE AD+18,3      ' 0310-   AE 00 03    LDX   $0300
    POKE AD+19,76: POKE AD+20,2: POKE AD+21,3       ' 0313-   4C 02 03    JMP   $0302
    POKE AD+22,96                                   ' 0316-   60          RTS

reset:
    A=0
    GR :COLOR= 10
    FOR L=16 TO 24 STEP 2
        HLIN 0,39 AT L
    NEXT L

again:
    A=A+1
    IF A=40 THEN goto reset
    COLOR= 12

keyloop:
    X= PEEK (-16384):IF X<128 THEN GOTO keyloop: POKE -16368,0:X=X-176
    IF X=1 THEN P=194
    IF X=2 THEN P=173
    IF X=3 THEN P=155
    IF X=4 THEN P=145
    IF X=5 THEN P=128
    IF X=6 THEN P=115
    IF X=7 THEN P=108
    IF X=8 THEN P=95
    IF X=9 THEN P=85
    IF X=0 THEN P=75
    IF X=-3 THEN P=70
    IF X=45 THEN P=63
    IF X=-40 THEN P=56
    IF X=1 THEN B=29
    IF X=2 THEN B=28
    IF X=3 THEN B=27
    IF X=4 THEN B=26
    IF X=5 THEN B=25
    IF X=6 THEN B=24
    IF X=7 THEN B=23
    IF X=8 THEN B=22
    IF X=9 THEN B=21
    IF X=0 THEN B=20
    IF X=-3 THEN B=19
    IF X=45 THEN B=18
    IF X=-40 THEN B=17
    PLOT A,B
    D=80
    GOSUB tone
    GOTO again

tone:
    POKE AD+0,P: POKE AD+1,D:CALL AD+2:RETURN
