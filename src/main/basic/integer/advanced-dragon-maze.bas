sub pokew(addr,value)
    poke addr,value
    poke addr+1,value >> 8
end sub

function peekw(addr)
    return (peek(addr+1) << 8) + peek(addr)
end function

' See https://en.wikipedia.org/wiki/Linear-feedback_shift_register
' See http://www.retroprogramming.com/2017/07/xorshift-pseudorandom-numbers-in-z80.html
function random()
    ' 78-79   | $4E-$4F     | Random-Number Field
    r = peekw(0x4e)
    r = r xor (r << 7)
    r = r xor (r >> 9)
    r = r xor (r << 8)
    pokew(0x4e, r)
    return r
end function

function rnd(n)
    return random() mod n
end function

function abs(n)
    if n < 0 then
        return - n
    end if
    return n
end function

'  * ADVANCED DRAGON MAZE *

    const ADDR = 0x300  ' moving machine code to $300

L100:   ' Note that code referenced line 15 which did not exist. Guessing as to Integer BASIC behavior
    CALL -936:VTAB 4:HTAB 16:PRINT "ADVANCED":HTAB 14:PRINT "DRAGON MAZE":PRINT :HTAB 11:PRINT "BY GARY J. SHANNON":PRINT :PRINT
    PRINT "OBJECT:":PRINT "======":PRINT :PRINT "YOU ARE IN A MAZE, AND MUST GET OUT.":PRINT "YOU WILL START ON THE LEFT SIDE, AND"
    PRINT "RUN FOR THE EXIT AT THE RIGHT.":PRINT "TO MOVE UP HIT 'U'...DOWN HIT 'D'..ETC.":PRINT "NO CARRIAGE RETURN IS NEEDED SO MOVE"
    PRINT "FAST!!":PRINT "SOUNDS EASY??...":PRINT "(NOT WITH A HUNGRY DRAGON!!!!!)"
    VTAB 24:PRINT "HIT ANY KEY TO BEGIN";
L130:
    Q= PEEK (-16384):IF Q<128 THEN GOTO L130
    POKE -16368,0
    GOTO L3045
L3001:
    IF  RND (100)<6 THEN GOTO L3013
    LF=0:IF H<1 THEN GOTO L3003
    IF  SCRN(X-2,Y)<>0 THEN LF=1
L3003:
    RT=0:IF H>17 THEN GOTO L3004
    IF  SCRN(X+2,Y)<>0 THEN RT=1
L3004:
    UP=0:IF V<1 THEN GOTO L3005
    IF  SCRN(X,Y-2)<>0 THEN UP=1
L3005:
    DN=0:IF V>17 THEN GOTO L3006
    IF  SCRN(X,Y+2)<>0 THEN DN=1
L3006:
    IF LF+RT+UP+DN=0 THEN GOTO L3013
L3007:
    D= RND (4)
    IF D=0 THEN GOTO L3009
    IF D=1 THEN GOTO L3010
    IF D=2 THEN GOTO L3011
    GOTO L3012
L3009:
    IF LF=0 THEN GOTO L3007
    H=H-1:FOR I=1 TO 2:X=X-1:PLOT X,Y:NEXT I:SQ=SQ-1:GOTO L3001
L3010:
    IF RT=0 THEN GOTO L3007
    H=H+1:FOR I=1 TO 2:X=X+1:PLOT X,Y:NEXT I:SQ=SQ-1:GOTO L3001
L3011:
    IF UP=0 THEN GOTO L3007
    V=V-1:FOR I=1 TO 2:Y=Y-1:PLOT X,Y:NEXT I:SQ=SQ-1:GOTO L3001
L3012:
    IF DN=0 THEN GOTO L3007
    V=V+1:FOR I=1 TO 2:Y=Y+1:PLOT X,Y:NEXT I:SQ=SQ-1:GOTO L3001
L3013:
    IF SQ=0 THEN GOTO L3015
L3014:
    H= RND (19):V= RND (19):X=2*H+1:Y=2*V+1:IF  SCRN(X,Y)<>0 THEN GOTO L3014
    GOTO L3001
L3015:
    PRINT "                     GO<CTRL-G><CTRL-G><CTRL-G><CTRL-G><CTRL-G><CTRL-G><CTRL-G><CTRL-G><CTRL-G><CTRL-G><CTRL-G><CTRL-G>"
    H=0:V= RND (19):DH=18:DV= RND (19):PLOT 38,2*DV+1
    P=2:D=1
    GOSUB L3044:PX=X:PY=Y:LPX=X:LPY=Y
    COLOR= P:PLOT PX,PY
    DX=2*DH+1:DY=2*DV+1:LDX=DX:LDY=DY
L3021:
    IF PX<39 THEN GOTO L3022
    PRINT "YOU WIN<CTRL-G>!<CTRL-G>!<CTRL-G>!<CTRL-G>":FOR T=1 TO 1000:NEXT T:GOTO L100
L3022:
    PT=ABS (DX-PX)+ABS (DY-PY): POKE ADDR+0,3*PT:CALL ADDR+1
    IF DX=LDX AND DY=LDY THEN GOTO L3025
    COLOR= 0:PLOT LDX,LDY:COLOR= D:PLOT DX,DY:LDX=DX:LDY=DY
    IF DX=PX AND DY=PY THEN GOTO L3042
L3025:
    Q= PEEK (-16384): POKE -16368,0:IF Q=155 THEN GOTO L100
    IF Q<127 THEN GOTO L3028
    IF Q= ASC("U") OR Q=ASC("A") THEN GOTO L3038
    IF Q= ASC("D") OR Q=ASC("Z") THEN GOTO L3039
    IF Q= ASC("L") OR Q=0x88 THEN GOTO L3040
    IF Q= ASC("R") OR Q=0x95 THEN GOTO L3041
L3027:
    IF PX=LPX AND PY=LPY THEN GOTO L3021
    COLOR= 0:PLOT LPX,LPY:COLOR= P:PLOT PX,PY:LPX=PX:LPY=PY:GOTO L3021
L3028:
    IF  RND (100)>10 THEN GOTO L3025
    IF DX<=PX OR  SCRN(DX-1,DY)=9 THEN GOTO L3030
    DX=DX-1:GOTO L3021
L3030:
    IF DX>=PX OR  SCRN(DX+1,DY)=9 THEN GOTO L3031
    DX=DX+1:GOTO L3021
L3031:
    IF DY<=PY OR  SCRN(DX,DY-1)=9 THEN GOTO L3032
    DY=DY-1:GOTO L3021
L3032:
    IF DY>=PY OR  SCRN(DX,DY+1)=9 THEN GOTO L3033
    DY=DY+1:GOTO L3021
L3033:
    IF  RND (1000)>DIF THEN GOTO L3021
    IF DX<=PX THEN GOTO L3035
    DX=DX-1:GOTO L3021
L3035:
    IF DX>=PX THEN GOTO L3036
    DX=DX+1:GOTO L3021
L3036:
    IF DY<=PY THEN GOTO L3037
    DY=DY-1:GOTO L3021
L3037:
    DY=DY+1:GOTO L3021
L3038:
    IF  SCRN(PX,PY-1)<>0 THEN GOTO L3028
    PY=PY-1:GOTO L3027
L3039:
    IF  SCRN(PX,PY+1)<>0 THEN GOTO L3028
    PY=PY+1:GOTO L3027
L3040:
    IF  SCRN(PX-1,PY)<>0 THEN GOTO L3028
    PX=PX-1:GOTO L3027
L3041:
    IF  SCRN(PX+1,PY)<>0 THEN GOTO L3028
    PX=PX+1:GOTO L3027
L3042:
    PRINT "GOTCHA<CTRL-G>!<CTRL-G>!<CTRL-G>!<CTRL-G>":FOR T=1 TO 500:NEXT T
    GOTO L100
L3044:
    X=2*H+1:Y=2*V+1:RETURN
L3045:
    GR :COLOR= 9:FOR I=0 TO 38:VLIN 0,38 AT I:NEXT I
    COLOR= 0:H= RND (19):V= RND (19):GOSUB L3044:PLOT X,Y:SQ=360
    DIF= RND (180)+20
    CALL -936:PRINT "<<<<< ADVANCED DRAGON MAZE >>>>>>>"
    PRINT "            GARY J. SHANNON"
    PRINT "             DIFFICULTY=";DIF
    POKE ADDR+1,173: POKE ADDR+2,48: POKE ADDR+3,192    '   $301: LDA $C030
    POKE ADDR+4,173: POKE ADDR+5,0: POKE ADDR+6,3       '   $304: LDA $300
    POKE ADDR+7,233: POKE ADDR+8,1                      '   $307: SBC #$01
    POKE ADDR+9,208: POKE ADDR+10,252                   '   $309: BNE $307
    POKE ADDR+11,206: POKE ADDR+12,17: POKE ADDR+13,3   '   $30B: DEC $311
    POKE ADDR+14,208: POKE ADDR+15,241                  '   $30E: BNE $301
    POKE ADDR+16,96                                     '   $310: RTS
    GOTO L3001

'  COPYRIGHT 1978 BY SOFTECH *
