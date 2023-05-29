main:
    GOSUB showTitle

    ' Modification: Sound routine was in ZP @ $02.
    ' Impacted interpreter and had trouble moving the interpreter where Applesoft
    ' did not impact. So moving the routine to $300. The AD variable "should"
    ' allow it to be a bit more moveable. Note this is a bit lazy for address
    ' calculation.
    AD = 768
    POKE AD+3,173: POKE AD+4,48: POKE AD+5,192                  ' LDA $C030
    POKE AD+6,173: POKE AD+7,AD+0: POKE AD+8,AD/256             ' LDA $300
    POKE AD+9,32: POKE AD+10,168: POKE AD+11,252                ' JSR $FCA8
    POKE AD+12,173: POKE AD+13,AD+1: POKE AD+14,AD/256          ' LDA $301
    POKE AD+15,208: POKE AD+16,5                                ' BNE $0316
    POKE AD+17,206: POKE AD+18,AD+2: POKE AD+19,AD/256          ' DEC $302
    POKE AD+20,240: POKE AD+21,6                                ' BEQ $031C
    POKE AD+22,206: POKE AD+23,AD+1: POKE AD+24,AD/256          ' DEC $301
    POKE AD+25,76: POKE AD+26,AD+3: POKE AD+27,AD/256           ' JMP $0303
    POKE AD+28,96                                               ' RTS
    KR=-16368:KY=-16384:SP=-16336

startGameRound:
    CALL -936:GR :COLOR= 5:VLIN 0,39 AT 0:HLIN 0,39 AT 0:VLIN 0,39 AT 39:HLIN 0,39 AT 39
    W= ASC("W"):A= ASC("A"):X= ASC("X"):D= ASC("D"):I= ASC("I"):J= ASC("J"):M= ASC("M"):L= ASC("L")
    IF GN>4 THEN GOTO gameOver
    GN=GN+1
    VTAB 21:PRINT "***HIT ANY KEY TO START GAME***"
waitForKey:
    IF  PEEK (KY)<128 THEN GOTO waitForKey
    POKE KR,0:CALL -936
    V1=21:V2=19:H1=1:H2=37:K1=D:K2=J
gameLoop:
    K= PEEK (KY): POKE KR,0
    IF K=A OR K=W OR K=D OR K=X THEN K1=K
    IF K=I OR K=L OR K=M OR K=J THEN K2=K
    V1=V1+2*((K1=X)-(K1=W)):V2=V2+2*((K2=M)-(K2=I)):H1=H1+2*((K1=D)-(K1=A)):H2=H2+2*((K2=L)-(K2=J))
    IF V1<1 OR V1>37 OR H1<1 OR H1>37 THEN GOTO player2Wins
    IF V2<1 OR V2>37 OR H2<1 OR H2>37 THEN GOTO player1Wins
    IF  SCRN(H1,V1)<>0 THEN GOTO player2Wins
    IF  SCRN(H2,V2)<>0 THEN GOTO player1Wins
    POKE AD+1,100: POKE AD+2,1: POKE AD+0,8:CALL AD+2
    COLOR= 3:HLIN H1,H1+1 AT V1:HLIN H1,H1+1 AT V1+1:COLOR= 9:HLIN H2,H2+1 AT V2:HLIN H2,H2+1 AT V2+1
    POKE AD+1,100: POKE AD+2,2: POKE AD+0,7:CALL AD+2
    GOTO gameLoop
player2Wins:
    CALL -936:PRINT "***** PLAYER #2 WINS *****":P2=P2+1:GOSUB buzzSpeaker:GOTO winDelay
player1Wins:
    CALL -936:PRINT "***** PLAYER #1 WINS *****":P1=P1+1:GOSUB buzzSpeaker
winDelay:
    FOR QQ=1 TO 1000:NEXT QQ
    POKE KR,0:GOTO startGameRound

buzzSpeaker:
    FOR A=1 TO 10:P= PEEK (SP):NEXT A:FOR A=1 TO 100:P= PEEK (SP):NEXT A:RETURN

gameOver:
    TEXT :CALL -936:VTAB 12:HTAB 10:PRINT "<<<< GAME OVER >>>>":VTAB 20
    PRINT
    PRINT "   PLAYER #1 : ";P1;"   TO  PLAYER #2 : ";P2
    END

showTitle:
    CALL -936:PRINT :PRINT "   ***  BARRICADE  ***"
    PRINT :PRINT :PRINT :PRINT
    PRINT " THE OBJECT IS TO ADVANCE, ":PRINT
    PRINT " MAKING YOUR BARRICADE.":PRINT
    PRINT :PRINT :PRINT :PRINT
    PRINT "PLAYER #1        PLAYER #2":PRINT
    PRINT
    PRINT "   W                 I"
    PRINT "A     D           J     L"
    PRINT "   X                 M"
    VTAB 23:PRINT "   HIT RETURN TO CONTINUE";:CALL -756:RETURN
