    .p02

    .zeropage

ip:     .addr 0
arg:    .res 3
acc:    .byte 0
yreg:   .byte 0
xreg:   .byte 0
locals: .byte 0
baseip: .addr 0
basesp: .byte 0
.ifdef DEBUG
flags:  .byte 0
ptr:    .addr 0
.endif

stack = $100
stackA = stack+1
stackB = stack+3
stackC = stack+5

rdkey = $fd0c
crout = $fd8e
prbyte = $fdda
cout = $fded

    .code

main:
;    TODO initialization???
    lda #<(code-1)
    sta ip
    sta baseip
    lda #>(code-1)
    sta ip+1
    sta baseip+1
    tsx
    stx basesp

.ifdef DEBUG
    jsr print
    .byte "ENABLE TRACING? ",$00
    jsr rdkey
    cmp #'Y'|$80
    beq :+
    cmp #'y'|$80
    beq :+
    lda #0
    beq :++
:   lda #$ff
:   sta flags
.endif
    jmp loop

fetch:
    inc ip
    bne @skip
    inc ip+1
@skip:
.ifdef DEBUG
    bit flags
    bpl @noflag
    jsr print
    .byte $82,ip
    .byte ": ",0
    ldy #0
    lda (ip),y
    jsr prbyte
    jsr crout
@noflag:
.endif

    ldy #0
    lda (ip),y
    rts

callarg:
    jmp (arg)

loop:
.ifdef DEBUG
    bit flags
    bpl @noflag
    jsr print
    .byte "IP=",$82,ip
    .byte ", ARG=",$83,arg
    .byte ", A/Y/X=",$81,acc,$81,yreg,$81,xreg
    .byte $d,"STACK=",0
    tsx
    lda stackA+1,x
    jsr prbyte
    lda stackA,x
    jsr prbyte
    lda #' '|$80
    jsr cout
    lda stackB+1,x
    jsr prbyte
    lda stackB,x
    jsr prbyte
    lda #' '|$80
    jsr cout
    lda stackC+1,x
    jsr prbyte
    lda stackC,x
    jsr prbyte
    jsr rdkey
    jsr crout
@noflag:
.endif

    jsr fetch
    pha
    rol     ; bit 7 into C
    rol     ; bit 6 into C, bit 7 now bit 0
    rol     ; bit 67 now bits 10
    and #3
    tax
;@copy:
    beq @copydone
    jsr fetch
    sta arg
    dex
    beq @copydone
    jsr fetch
    sta arg+1
    dex
    beq @copydone
    jsr fetch
    sta arg+2
@copydone:
    pla

    ldy #0
@search:
    cmp brtable,y
    beq @found
    iny
    iny
    iny
    cpy #brlen
    bcc @search
    bcs _break
@found:
    tsx
    lda brtable+2,y
    pha
    lda brtable+1,y
    pha
    rts
brtable:
    .byte $00
    .addr _exit-1
    .byte $01
    .addr _add-1
    .byte $02
    .addr _sub-1
    .byte $03
    .addr _istore-1
    .byte $04
    .addr _lt-1
    .byte $05
    .addr _setacc-1
    .byte $06
    .addr _setyreg-1
    .byte $07
    .addr _call-1
    .byte $08
    .addr _le-1
    .byte $40
    .addr _reserve-1
    .byte $41
    .addr _load-1
    .byte $42
    .addr _store-1
    .byte $80
    .addr _goto-1
    .byte $81
    .addr _iftrue-1
    .byte $82
    .addr _iffalse-1
    .byte $83
    .addr _loadc-1
brlen = *-brtable

_break:
    brk     ; TODO

; ADD:  (A) (B) => (A+B)
_add:
    clc
    lda stackA,x
    adc stackB,x
    sta stackB,x
    lda stackA+1,x
    adc stackB+1,x
    sta stackB+1,x
poploop:
    pla
    pla
    jmp loop

; SUB:  (A) (B) => (A-B)
_sub:
    sec
    lda stackB+1,x
    sbc stackA+1,x
    sta stackB+1,x
    lda stackB,x
    sbc stackA,x
    sta stackB,x
    jmp poploop

; ISTORE: (A) (B) => (); *B = byte(A)
_istore:
    pla
    sta @ptr+1
    pla
    sta @ptr+2
    pla
@ptr:
    sta $ffff
    pla     ; toss high byte
    jmp loop

; LT: (A) (B) => (A<B)
_lt:
    lda stackB+1,x
    cmp stackA+1,x
    beq @maybe
    bcs @not
@maybe:
    lda stackB,x
    cmp stackA,x
    bcs @not
    lda #1
    sta stackB,x
    lda #0
    sta stackB+1,x
    jmp poploop
@not:
    lda #0
    sta stackB,x
    sta stackB+1,x
    jmp poploop

; LE: (A) (B) => (A<=B)
_le:
    lda stackB+1,x
    cmp stackA+1,x
    beq @maybe
    bcs @not
@maybe:
    lda stackB,x
    cmp stackA,x
    beq @yes
    bcs @not
@yes:
    lda #1
    sta stackB,x
    lda #0
    sta stackB+1,x
    jmp poploop
@not:
    lda #0
    sta stackB,x
    sta stackB+1,x
    jmp poploop

; SETACC: (A) => (); Acc=byte(A)
_setacc:
    pla
    sta acc
    pla
    jmp loop

; SETYREG: (A) => (); Y-register=byte(A)
_setyreg:
    pla
    sta yreg
    pla
    jmp loop

; CALL: (A) => (); PC=A
_call:
    pla
    sta arg
    pla
    sta arg+1
    ldx xreg
    ldy yreg
    lda acc
    jsr callarg
    sta acc
    stx xreg
    sty yreg
    jmp loop

; RESERVE <n>: () => (0 ...); locals=SP
_reserve:
    ldy arg
    lda #0
@0: pha
    pha
    dey
    bne @0
    tsx
    stx locals
    jmp loop

; LOADC <int>: () => (int)
_loadc:
    lda arg+1
    pha
    lda arg
    pha
    jmp loop

; LOAD <offset>: () => *(locals+offset)
_load:
    clc
    lda arg
    adc locals
    tay
    lda stackA+1,y
    pha
    lda stackA,y
    pha
    jmp loop

; STORE <offset>: (A) => (); *(locals+offset)=A
_store:
    clc
    lda arg
    adc locals
    tay
    pla
    sta stackA,y
    pla
    sta stackA+1,y
    jmp loop

; GOTO <addr>: IP=addr
_goto:
    clc
    lda arg
    adc baseip
    sta ip
    lda arg+1
    adc baseip+1
    sta ip+1
    jmp loop

; IFTRUE <addr>: (A) => (); A <> 0 => IP=addr
_iftrue:
    pla
    pla
    lda stackA,x
    ora stackA+1,x
    bne _goto   ; non-zero == true
    jmp loop

; IFFALSE <addr>: (A) => (); A == 0 => IP=addr
_iffalse:
    pla
    pla
    lda stackA,x
    ora stackA+1,x
    beq _goto   ; zero == false
    jmp loop

; EXIT: restore back to original SP
_exit: 
    ldx basesp
    txs
    rts

.ifdef DEBUG
.proc print
    pla
    sta ptr
    pla
    sta ptr+1
    txa
    pha
    tya
    pha
loop:
    jsr getch
    beq done
    bmi subcommands
    ora #$80
    jsr cout
    jmp loop
done:
    pla
    tay
    pla
    tax
    lda ptr+1
    pha
    lda ptr
    pha
    rts
subcommands:
    and #$7f
    cmp #4
    bcs @not123
@printhex:
    tax
    ldy #1
    adc (ptr),y
    tay
    dex
    dey
:   lda $0,y
    jsr prbyte
    dey
    dex
    bpl :-
    jsr getch   ; just to toss ZP away
@not123:
    jmp loop
getch:
    inc ptr
    bne @skip
    inc ptr+1
@skip:
    ldy #0
    lda (ptr),y
    rts
.endproc
.endif

code:
