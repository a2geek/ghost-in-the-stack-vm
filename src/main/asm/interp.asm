    .p02

;DEBUG = 1

; ZP locations
; Note: Struct is to make assignment and sizing self-referential.
.struct ZP
        .org $0
ip      .addr
arg     .res 3
acc     .byte
yreg    .byte
xreg    .byte
locals  .byte
baseip  .addr
basesp  .byte
.ifdef DEBUG
flags   .byte
ptr     .addr
.endif
.endstruct

stack = $100

rdkey = $fd0c
crout = $fd8e
prbyte = $fdda
cout = $fded

    .org $2000

main:
;    TODO initialization???
    lda #<(code-1)
    sta ZP::ip
    sta ZP::baseip
    lda #>(code-1)
    sta ZP::ip+1
    sta ZP::baseip+1
    tsx
    stx ZP::basesp

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
:   sta ZP::flags
.endif
    jmp loop

fetch:
    inc ZP::ip
    bne @skip
    inc ZP::ip+1
@skip:
.ifdef DEBUG
    bit ZP::flags
    bpl @noflag
    jsr print
    .byte $82,ZP::ip
    .byte ": ",0
    ldy #0
    lda (ZP::ip),y
    jsr prbyte
    jsr crout
@noflag:
.endif

    ldy #0
    lda (ZP::ip),y
    rts

callarg:
    jmp (ZP::arg)

loop:
.ifdef DEBUG
    bit ZP::flags
    bpl @noflag
    jsr print
    .byte "IP=",$82,ZP::ip
    .byte ", ARG=",$83,ZP::arg
    .byte ", A/Y/X=",$81,ZP::acc,$81,ZP::yreg,$81,ZP::xreg
    .byte $d,"STACK=",0
    tsx
    lda stack+2,x
    jsr prbyte
    lda stack+1,x
    jsr prbyte
    lda #' '|$80
    jsr cout
    lda stack+4,x
    jsr prbyte
    lda stack+3,x
    jsr prbyte
    lda #' '|$80
    jsr cout
    lda stack+6,x
    jsr prbyte
    lda stack+5,x
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
    sta ZP::arg
    dex
    beq @copydone
    jsr fetch
    sta ZP::arg+1
    dex
    beq @copydone
    jsr fetch
    sta ZP::arg+2
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
    lda stack+1,x
    adc stack+3,x
    sta stack+3,x
    lda stack+2,x
    adc stack+4,x
    sta stack+4,x
poploop:
    pla
    pla
    jmp loop

; SUB:  (A) (B) => (A-B)
_sub:
    sec
    lda stack+4,x
    sbc stack+2,x
    sta stack+4,x
    lda stack+3,x
    sbc stack+1,x
    sta stack+3,x
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
    lda stack+4,x
    cmp stack+2,x
    beq @maybe
    bcs @not
@maybe:
    lda stack+3,x
    cmp stack+1,x
    bcs @not
    lda #1
    sta stack+3,x
    lda #0
    sta stack+4,x
    jmp poploop
@not:
    lda #0
    sta stack+3,x
    sta stack+4,x
    jmp poploop

; LE: (A) (B) => (A<=B)
_le:
    lda stack+4,x
    cmp stack+2,x
    beq @maybe
    bcs @not
@maybe:
    lda stack+3,x
    cmp stack+1,x
    beq @yes
    bcs @not
@yes:
    lda #1
    sta stack+3,x
    lda #0
    sta stack+4,x
    jmp poploop
@not:
    lda #0
    sta stack+3,x
    sta stack+4,x
    jmp poploop

; SETACC: (A) => (); Acc=byte(A)
_setacc:
    pla
    sta ZP::acc
    pla
    jmp loop

; SETYREG: (A) => (); Y-register=byte(A)
_setyreg:
    pla
    sta ZP::yreg
    pla
    jmp loop

; CALL: (A) => (); PC=A
_call:
    pla
    sta ZP::arg
    pla
    sta ZP::arg+1
    ldx ZP::xreg
    ldy ZP::yreg
    lda ZP::acc
    jsr callarg
    sta ZP::acc
    stx ZP::xreg
    sty ZP::yreg
    jmp loop

; RESERVE <n>: () => (0 ...); locals=SP
_reserve:
    ldy ZP::arg
    lda #0
@0: pha
    pha
    dey
    bne @0
    tsx
    stx ZP::locals
    jmp loop

; LOADC <int>: () => (int)
_loadc:
    lda ZP::arg+1
    pha
    lda ZP::arg
    pha
    jmp loop

; LOAD <offset>: () => *(locals+offset)
_load:
    clc
    lda ZP::arg
    adc ZP::locals
    tay
    lda stack+2,y
    pha
    lda stack+1,y
    pha
    jmp loop

; STORE <offset>: (A) => (); *(locals+offset)=A
_store:
    clc
    lda ZP::arg
    adc ZP::locals
    tay
    pla
    sta stack+1,y
    pla
    sta stack+2,y
    jmp loop

; GOTO <addr>: IP=addr
_goto:
    clc
    lda ZP::arg
    adc ZP::baseip
    sta ZP::ip
    lda ZP::arg+1
    adc ZP::baseip+1
    sta ZP::ip+1
    jmp loop

; IFTRUE <addr>: (A) => (); A <> 0 => IP=addr
_iftrue:
    pla
    pla
    lda stack+1,x
    ora stack+2,x
    bne _goto   ; non-zero == true
    jmp loop

; IFFALSE <addr>: (A) => (); A == 0 => IP=addr
_iffalse:
    pla
    pla
    lda stack+1,x
    ora stack+2,x
    beq _goto   ; zero == false
    jmp loop

; EXIT: restore back to original SP
_exit: 
    ldx ZP::basesp
    txs
    rts

.ifdef DEBUG
.proc print
    pla
    sta ZP::ptr
    pla
    sta ZP::ptr+1
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
    lda ZP::ptr+1
    pha
    lda ZP::ptr
    pha
    rts
subcommands:
    and #$7f
    cmp #4
    bcs @not123
@printhex:
    tax
    ldy #1
    adc (ZP::ptr),y
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
    inc ZP::ptr
    bne @skip
    inc ZP::ptr+1
@skip:
    ldy #0
    lda (ZP::ptr),y
    rts
.endproc
.endif

code:
