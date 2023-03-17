    .p02

    .zeropage

;            .res $40    ; moving variable locations to free $00-$1F for those pesky Integer BASIC apps.
;                        ; cannot use .org since that changes the entire app.

ip:        .addr 0
arg:       .res 2
temp:      .byte 0
acc:       .byte 0
yreg:      .byte 0
xreg:      .byte 0
locals:    .byte 0
baseip:    .addr 0
basesp:    .byte 0
remainder: .word 0
ptr:       .addr 0
.ifdef DEBUG
flags:     .byte 0
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
    jsr cout
    cmp #'Y'|$80
    beq :+
    cmp #'y'|$80
    beq :+
    lda #0
    beq :++
:   lda #$ff
:   sta flags
    jsr crout
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

compareAB:
    lda stackB+1,x
    cmp stackA+1,x
    bne @done
    lda stackB,x
    cmp stackA,x
@done:
    rts

fetch1Arg:
    jsr fetch
    sta arg
    rts
fetch2Args:
    jsr fetch
    sta arg
    jsr fetch
    sta arg+1
    rts

; See https://codebase64.org/doku.php?id=base:16bit_division_16-bit_result
; Note: quotient = stackB, remainder = remainder (in ZP)
.proc div16
divisor = stackA
dividend = stackB
result = dividend

	lda #0	        ;preset remainder to 0
	sta remainder
	sta remainder+1
	ldy #16	        ;repeat for each bit: ...
divloop:
    asl dividend,x	;dividend lb & hb*2, msb -> Carry
	rol dividend+1,x
	rol remainder	;remainder lb & hb * 2 + msb from carry
	rol remainder+1
	lda remainder
	sec
	sbc divisor,x	;substract divisor to see if it fits in
	sta temp	    ;lb result -> temp, for we may need it later
	lda remainder+1
	sbc divisor+1,x
	bcc skip	    ;if carry=0 then divisor didn't fit in yet
	sta remainder+1	;else save substraction result as new remainder,
    lda temp
	sta remainder
	inc result,x	;and INCrement result cause divisor fit in 1 times
skip:
    dey
	bne divloop
	rts
.endproc

setbpoploop:
    sta stackB,x
    tya
    sta stackB+1,x
poploop:
    pla
    pla
    ; fall through to loop
loop:
.ifdef DEBUG
    bit flags
    bmi :+
    jmp @noflag
:   jsr print
    .byte "IP=",$82,ip
    .byte ", ARG=",$82,arg
    .byte ", A/Y/X=",$81,acc,$81,yreg,$81,xreg
    .byte ",",$d,"PTR=",$82,ptr
    .byte ", LOCALS=",$81,locals,", S=",0
    tsx
    txa
    jsr prbyte
    jsr print
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
    cmp #$9b
    bne :+
    jmp _exit
:   jsr crout
@noflag:
.endif

    jsr fetch
    cmp #(brlen/2)
    bcs _break
    asl
    tay
    tsx
    lda brtable+1,y
    pha
    lda brtable,y
    pha
    rts
brtable:
    .addr _exit-1
    .addr _add-1
    .addr _sub-1
    .addr _mul-1
    .addr _div-1
    .addr _mod-1
    .addr _iload-1
    .addr _istore-1
    .addr _lt-1
    .addr _le-1
    .addr _eq-1
    .addr _setacc-1
    .addr _setyreg-1
    .addr _call-1
    .addr _return-1
    .addr _dup-1
    .addr _incr-1
    .addr _decr-1
    .addr _reserve-1
    .addr _load-1
    .addr _store-1
    .addr _goto-1
    .addr _gosub-1
    .addr _iftrue-1
    .addr _iffalse-1
    .addr _loadc-1
brlen = *-brtable

_break:
    brk     ; TODO

; ADD:  (B) (A) => (B+A)
_add:
    clc
    lda stackA,x
    adc stackB,x
    sta stackB,x
    lda stackA+1,x
    adc stackB+1,x
    sta stackB+1,x
    jmp poploop

; SUB:  (B) (A) => (B-A)
_sub:
    sec
    lda stackB+1,x
    sbc stackA+1,x
    sta stackB+1,x
    lda stackB,x
    sbc stackA,x
    sta stackB,x
    jmp poploop

; MUL:  (B) (A) => (B*A)
; See http://6502.org/users/obelisk/ for original source
_mul:
    lda #0
    sta arg
    sta arg+1
    ldy #16
@loop:
    asl arg
    rol arg+1
    asl stackA,x
    rol stackA+1,x
    bcc @next
    clc
    lda stackB,x
    adc arg
    sta arg
    lda stackB+1,x
    adc arg+1
    sta arg+1
@next:
    dey
    bne @loop
    lda arg
    ldy arg+1
    jmp setbpoploop

; DIV: (B) (A) => (B/A)
_div:
    jsr div16
    lda stackB,x
    ldy stackB+1,x
    jmp setbpoploop

; MOD: (B) (A) => (B MOD A)
_mod:
    jsr div16
    lda remainder
    ldy remainder+1
    jmp setbpoploop

; ILOAD: (A) => (B); B = byte(*A)
_iload:
    pla
    sta ptr
    pla
    sta ptr+1
    ldy #0
    tya
    pha     ; high byte = 0
    lda (ptr),y
    pha
    jmp loop

; ISTORE: (B) (A) => (); *A = byte(B)
_istore:
    pla
    sta ptr
    pla
    sta ptr+1
    pla
    ldy #0
    sta (ptr),y
    pla     ; toss high byte
    jmp loop

; LT: (B) (A) => (B<A)
_lt:
    jsr compareAB
    bcs setBto0
setBto1:
    lda #1
    ldy #0
    jmp setbpoploop

; LE: (B) (A) => (B<=A)
_le:
    jsr compareAB
    beq setBto1
    bcc setBto1
    bcs setBto0

; EQ: (B) (A) => (B=A)
_eq:
    jsr compareAB
    beq setBto1
setBto0:
    lda #0
    tay
    jmp setbpoploop

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
    jsr fetch1Arg   ; Acc = arg
    tay
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
    jsr fetch2Args  ; Acc = arg+1
    pha
    lda arg
    pha
    jmp loop

; LOAD <offset>: () => *(locals+offset)
_load:
    jsr fetch1Arg   ; Acc = arg
    clc
    adc locals
    tax
    ; duplicating this address into TOS via DUP

; DUP; (A) => (A) (A)
_dup:
    lda stackA+1,x
    pha
    lda stackA,x
    pha
    jmp loop

_incr:
    inc stackA,x
    bne @skip
    inc stackA+1,x
@skip:
    jmp loop

_decr:
    lda stackA,x
    bne @justlow
    dec stackA+1,x
@justlow:
    dec stackA,x
    jmp loop

; STORE <offset>: (A) => (); *(locals+offset)=A
_store:
    jsr fetch1Arg   ; Acc = arg
    clc
    adc locals
    tay
    pla
    sta stackA,y
    pla
    sta stackA+1,y
    jmp loop

; GOSUB <addr>; () => (IP); IP=addr
_gosub:
    jsr fetch2Args  ; Acc = arg+1
    lda ip+1
    pha
    lda ip
    pha
    jmp argtoip

; GOTO <addr>: IP=addr
_goto:
    jsr fetch2Args  ; Acc = arg+1
argtoip:
    lda arg
    clc
    adc baseip
    sta ip
    lda arg+1
    adc baseip+1
    sta ip+1
    jmp loop

; RETURN; (TOS) => (); IP=TOS
_return:
    pla
    sta ip
    pla
    sta ip+1
    jmp loop

; IFTRUE <addr>: (A) => (); A <> 0 => IP=addr
_iftrue:
    pla
    pla
    lda stackA,x
    ora stackA+1,x
    bne _goto   ; non-zero == true
    jsr fetch2Args  ; need to toss these away
    jmp loop

; IFFALSE <addr>: (A) => (); A == 0 => IP=addr
_iffalse:
    pla
    pla
    lda stackA,x
    ora stackA+1,x
    beq _goto   ; zero == false
    jsr fetch2Args  ; need to toss these away
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
