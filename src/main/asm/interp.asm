    .p02

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
flags   .byte
.endstruct

stack = $100

    .org $2000

main:
;    TODO initialization???
    lda #<(code-1)
    sta ZP::ip
    sta ZP::baseip
    lda #>(code-1)
    sta ZP::ip+1
    sta ZP::baseip+1
    lda #'T'|$80
    jsr $fded
    lda #'?'|$80
    jsr $fd0c
    cmp #'Y'|$80
    beq :+
    cmp #'y'|$80
    beq :+
    lda #0
    beq :++
:   lda #$ff
:   sta ZP::flags
    jmp loop

fetch:
    inc ZP::ip
    bne @skip
    inc ZP::ip+1
@skip:
    ; FIXME DEBUG CODE HERE
    bit ZP::flags
    bpl :+
    lda ZP::ip+1
    jsr $fdda
    lda ZP::ip
    jsr $fdda
    lda #':'|$80
    jsr $fded
    lda #' '|$80
    jsr $fded
    ldy #0
    lda (ZP::ip),y
    jsr $fdda
    jsr $fd8e

:   ldy #0
    lda (ZP::ip),y
    rts

callarg:
    jmp (ZP::arg)

loop:
    ; DEBUG/TRACE
    bit ZP::flags
    bpl :++
    lda #'='|$80
    jsr $fded
    ldy #0
:   lda ZP::ip,y
    jsr $fdda
    iny
    cpy #.sizeof(ZP)
    bcc :-
    jsr $fd0c   ; RDKEY
    jsr $fd8e

:   jsr fetch
    pha
    rol     ; bit 7 into C
    rol     ; bit 6 into C, bit 7 now bit 0
    rol     ; bit 67 now bits 10
    and #3
    tax
;@copy:
;    jsr fetch
;    sta ZP::arg,x
;    dex
;    bne @copy
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
    .addr _loadc-1
brlen = *-brtable

_break:
    brk     ; TODO

; ADD
_add:
    clc
    lda stack,x
    adc stack+2,x
    sta stack+2,x
    lda stack+1,x
    adc stack+3,x
    sta stack+3,x
poploop:
    pla
    pla
    jmp loop

; SUB
_sub:
    sec
    lda stack+3,x
    sbc stack+1,x
    sta stack+3,x
    lda stack+2,x
    sbc stack,x
    sta stack+2,x
    jmp poploop

; ISTORE
_istore:    ; *tos = byte(tos-1)
    pla
    sta @ptr+1
    pla
    sta @ptr+2
    pla
@ptr:
    sta $ffff
    pla     ; toss high byte
    jmp loop

; LT
_lt:
    lda stack+2,x
    cmp stack,x
    bcs @not
    lda stack+3,x
    cmp stack+1,x
    bcs @not
    lda #1
    sta stack+2,x
    lda #0
    sta stack+3,x
    jmp poploop
@not:
    lda #0
    sta stack+2,x
    sta stack+3,x
    jmp poploop

; SETACC
_setacc:
    pla
    sta ZP::acc
    pla
    jmp loop

; SETYREG
_setyreg:
    pla
    sta ZP::yreg
    pla
    jmp loop

; CALL
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

; RESERVE <n>
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

; LOADC <int>
_loadc:
    lda ZP::arg+1
    pha
    lda ZP::arg
    pha
    jmp loop

; LOAD <offset>
_load:
    clc
    lda ZP::arg
    adc ZP::locals
    tay
    lda stack,y
    sta stack,x
    lda stack+1,y
    sta stack+1,x
    jmp loop

; STORE <offset>
_store:
    clc
    lda ZP::arg
    adc ZP::locals
    tay
    lda stack,x
    sta stack,y
    lda stack+1,x
    sta stack+1,y
    jmp loop

; GOTO <addr>
_goto:
    clc
    lda ZP::arg
    adc ZP::baseip
    sta ZP::ip
    lda ZP::arg+1
    adc ZP::baseip+1
    sta ZP::ip+1
    jmp loop

; IFTRUE <addr>
_iftrue:
    pla
    pla
    lda stack,x
    ora stack+1,x
    beq _goto
    jmp loop

_exit:
    rts

code:
    
