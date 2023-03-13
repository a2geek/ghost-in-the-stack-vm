' Use the number keys to change tones

initialize:
    POKE 2,173: POKE 3,48: POKE 4,192: POKE 5,136: POKE 6,208: POKE 7,4: POKE 8,198: POKE 9,1: POKE 10,240
    POKE 11,8: POKE 12,202: POKE 13,208: POKE 14,246: POKE 15,166: POKE 16,0: POKE 17,76: POKE 18,2: POKE 19,0: POKE 20,96

reset:
    A=0
    GR :COLOR= 10
    FOR L=16 TO 24 STEP 2
        HLIN 0,39 AT L
    NEXT L

repeat:
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
    GOTO repeat

tone:
    POKE 0,P: POKE 1,D:CALL 2:RETURN
