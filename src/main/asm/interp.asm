    .p02

    .zeropage

ip:        .addr 0
temp:      .byte 0
workptr:   .word 0
locals:    .byte 0
globals:   .byte 0
baseip:    .addr 0
basesp:    .byte 0
.ifdef DEBUG
           .res 11      ; I can't do math to get to $17?
flags:     .byte 0
traceptr:  .addr 0
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

    jmp main

acc:       .byte 0
yreg:      .byte 0
xreg:      .byte 0

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

callworkptr:
    jmp (workptr)

compareAB:
    lda stackB+1,x
    eor stackA+1,x
    bmi @diffsigns
    lda stackB+1,x
    cmp stackA+1,x
    bne @diffvalue
    lda stackB,x
    cmp stackA,x
@diffvalue:
    rts
@diffsigns:
    lda stackA+1,x
    cmp stackB+1,x
    rts

; See https://codebase64.org/doku.php?id=base:16bit_division_16-bit_result
; Note: quotient = stackB, remainder = remainder (in ZP)
.proc div16
divisor = stackA
dividend = stackB
result = dividend
remainder = workptr

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

; Adjust A and B to both be positive; track resulting sign in C
fixSignAB:
    lda stackA+1,x
    eor stackB+1,x
    asl ; C = result should be negative
    php
    lda stackA+1,x
    bpl @checkB
    sec
    lda #0
    sbc stackA,x
    sta stackA,x
    lda #0
    sbc stackA+1,x
    sta stackA+1,x
@checkB:
    lda stackB+1,x
    bpl @exit
    sec
    lda #0
    sbc stackB,x
    sta stackB,x
    lda #0
    sbc stackB+1,x
    sta stackB+1,x
@exit:
    plp
    rts

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
    .byte "IP=",$82,ip,$d
    ;.byte ", A/Y/X=",$81,acc,$81,yreg,$81,xreg,",",$d
    .byte "WP=",$82,workptr,", LCL=",$81,locals,", GBL=",$81,globals,", S=",0
    tsx
    txa
    jsr prbyte
    jsr print
    .byte $d,"STACK=",0
    tsx
    ldy #10
@printstack:
    lda stackA,x
    jsr prbyte
    lda #' '|$80
    jsr cout
    inx
    dey
    bne @printstack
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
    .addr _divs-1
    .addr _mods-1
    .addr _divu-1
    .addr _modu-1
    .addr _neg-1
    .addr _iloadb-1
    .addr _istoreb-1
    .addr _iloadw-1
    .addr _istorew-1
    .addr _lt-1
    .addr _le-1
    .addr _eq-1
    .addr _ne-1
    .addr _or-1
    .addr _and-1
    .addr _xor-1
    .addr _shiftl-1
    .addr _shiftr-1
    .addr _setacc-1
    .addr _getacc-1
    .addr _setxreg-1
    .addr _getxreg-1
    .addr _setyreg-1
    .addr _getyreg-1
    .addr _call-1
    .addr _return-1
    .addr _dup-1
    .addr _incr-1
    .addr _decr-1
    .addr _pushz-1
    .addr _loadsp-1
    .addr _fixa-1
    .addr _global_reserve-1
    .addr _local_reserve-1
    .addr _local_free-1
    .addr _local_load-1
    .addr _local_store-1
    .addr _global_load-1
    .addr _global_store-1
    .addr _popn-1
    .addr _goto-1
    .addr _gosub-1
    .addr _iftrue-1
    .addr _iffalse-1
    .addr _loadc-1
    .addr _loada-1
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
    lda stackB,x
    sbc stackA,x
    sta stackB,x
    lda stackB+1,x
    sbc stackA+1,x
    sta stackB+1,x
    jmp poploop

; MUL:  (B) (A) => (B*A)
; See http://6502.org/users/obelisk/ for original source
_mul:
    lda #0
    sta workptr
    sta workptr+1
    ldy #16
@loop:
    asl workptr
    rol workptr+1
    asl stackA,x
    rol stackA+1,x
    bcc @next
    clc
    lda stackB,x
    adc workptr
    sta workptr
    lda stackB+1,x
    adc workptr+1
    sta workptr+1
@next:
    dey
    bne @loop
    lda workptr
    ldy workptr+1
    jmp setbpoploop

; DIVU: (B) (A) => (B/A)
_divu:
    jsr div16
poploop2:
    jmp poploop

; MODU: (B) (A) => (B MOD A)
_modu:
    jsr div16
    lda workptr
    ldy workptr+1
    jmp setbpoploop

; DIVS: (B) (A) => (B/A)
_divs:
    jsr fixSignAB
    php
    jsr div16
unsignedCommon:
    plp
    bcc poploop2
    pla ; discard A
    pla
    tsx
    ; Fall through to NEG (and stackB has become stackA)

; NEG: (A) = (-A)
_neg:
    sec
    lda #0
    sbc stackA,x
    sta stackA,x
    lda #0
    sbc stackA+1,x
    sta stackA+1,x
    jmp loop

; MODS: (B) (A) => (B MOD A)
_mods:
    jsr fixSignAB
    php
    jsr div16
    lda workptr
    sta stackB,x
    lda workptr+1
    sta stackB+1,x
    jmp unsignedCommon

; ILOADB: (A) => (B); B = byte(*A)
_iloadb:
    pla
    sta workptr
    pla
    sta workptr+1
    ldy #0
    tya
    pha     ; high byte = 0
    lda (workptr),y
    pha
    jmp loop

; ISTOREB: (B) (A) => (); *A = byte(B)
_istoreb:
    pla
    sta workptr
    pla
    sta workptr+1
    pla
    ldy #0
    sta (workptr),y
    jmp tossHighByteLoop

; ILOADW: (A) => (B); B = word(*A)
_iloadw:
    pla
    sta workptr
    pla
    sta workptr+1
    ldy #1
    lda (workptr),y
    pha
    dey
    lda (workptr),y
    pha
    jmp loop

; ISTOREW: (B) (A) => (); *A = word(B)
_istorew:
    pla
    sta workptr
    pla
    sta workptr+1
    ldy #0
    pla
    sta (workptr),y
    iny
    pla
    sta (workptr),y
    jmp loop

; LT: (B) (A) => (B<A)  [signed]
_lt:
    jsr compareAB
    bcs setBto0
setBto1:
    lda #1
    ldy #0
    jmp setbpoploop

; LE: (B) (A) => (B<=A) [signed]
_le:
    jsr compareAB
    bcc setBto1
    beq setBto1
    bcs setBto0

; EQ: (B) (A) => (B=A)
_eq:
    jsr compareAB
    beq setBto1
setBto0:
    lda #0
    tay
    jmp setbpoploop

; NE: (B) (A) => (B<>A)
_ne:
    jsr compareAB
    bne setBto1
    beq setBto0

; OR: (B) (A) => (B OR A); bitwise OR (booleans are essentially 1 bit)
_or:
    lda stackA+1,x
    ora stackB+1,x
    tay
    lda stackA,x
    ora stackB,x
    jmp setbpoploop

; AND: (B) (A) => (B AND A); bitwise AND (booleans are essentially 1 bit)
_and:
    lda stackA+1,x
    and stackB+1,x
    tay
    lda stackA,x
    and stackB,x
    jmp setbpoploop

; XOR: (B) (A) => (B XOR A); bitwise XOR (booleans are essentially 1 bit)
_xor:
    lda stackA+1,x
    eor stackB+1,x
    tay
    lda stackA,x
    eor stackB,x
    jmp setbpoploop

; SHIFTL: (B) (A) => (B << A); bitwise left shift (masked to max of 15 shifts)
_shiftl:
    lda stackA,x
    and #$f
    beq @done
    tay
@loop:
    asl stackB,x
    rol stackB+1,x
    dey
    bne @loop
@done:
    jmp poploop

; SHIFTR: (B) (A) => (B >> A); bitwise right shift (masked to max of 15 shifts)
_shiftr:
    lda stackA,x
    and #$f
    beq @done
    tay
@loop:
    lsr stackB+1,x
    ror stackB,x
    dey
    bne @loop
@done:
    jmp poploop

; SETACC: (A) => (); Acc=byte(A)
_setacc:
    pla
    sta acc
tossHighByteLoop:
    pla
    jmp loop

; GETACC: () => (Acc)
_getacc:
    ldy acc
pushByteLoop:
    lda #0
    pha
    tya
    pha
    jmp loop

; SETXREG: (A) => (); X-register=byte(A)
_setxreg:
    pla
    sta xreg
    jmp tossHighByteLoop

; GETXREG: () => (X-register)
_getxreg:
    ldy xreg
    jmp pushByteLoop

; SETYREG: (A) => (); Y-register=byte(A)
_setyreg:
    pla
    sta yreg
    jmp tossHighByteLoop

; GETYREG: () => (Y-register)
_getyreg:
    ldy yreg
    jmp pushByteLoop

; CALL: (A) => (); PC=A
_call:
    pla
    sta workptr
    pla
    sta workptr+1
    ldx xreg
    ldy yreg
    lda acc
    jsr callworkptr
    sta acc
    stx xreg
    sty yreg
    jmp loop

; GLOBAL_RESERVE <n>: () => (0 ...); globals=SP
_global_reserve:
    jsr fetch
    beq @noglobals
    tay
    lda #0
@pushloop:
    pha
    dey
    bne @pushloop
@noglobals:
    tsx
    stx globals
    stx locals      ; for safety?
    jmp loop

; LOCAL_RESERVE <n> <m>: () => (<old locals> 0 ...); locals=SP
_local_reserve:
    lda locals
    pha
    ; "n" => number of bytes to reserve on stack
    jsr fetch
    beq @nolocals
    tay
    lda #0
@pushloop:
    pha
    dey
    bne @pushloop
@nolocals:
    tsx
    stx locals
    jmp loop

; LOCAL_FREE <n>: (<old locals> 0 ...) => (); locals=old locals
_local_free:
    jsr fetch
    beq @nolocals
    tay
@poploop:
    pla
    dey
    bne @poploop
@nolocals:
    pla
    sta locals
    jmp loop

; POPN <n>: (TOS-n ... TOS-1 TOS) => ()
_popn:
    jsr fetch
    beq @nothingtodo    ; TODO: is this a code generation bug or not?
    tay
@poploop:
    pla
    dey
    bne @poploop
@nothingtodo:
    jmp loop

; PUSHZ: (N) => (0 0 0 0 ... 0n)
_pushz:    ; Allows 256 bytes - is that a bug or a feature?
    pla
    tay
    pla
    lda #0
@pushloop:
    pha
    dey
    bne @pushloop
    jmp loop

; LOADC <int>: () => (int)
_loadc:
    jsr fetch       ; low byte
    tax
    jsr fetch       ; high byte
setaxloop:  ; A,X => (X) (A) a=high
    pha
    txa
    pha
    jmp loop

; LOADSP: () => (SP)
_loadsp:    ; X already has SP
    lda #1
    inx ; S was pointing to next place to store a byte - we want the _last_ place a byte was stored
    jmp setaxloop

; LOCAL_LOAD <offset>: () => *(locals+offset)
_local_load:
    jsr fetch
    clc
    adc locals
    tax
    ; duplicating this address into TOS via DUP
    jmp _dup

; GLOBAL_LOAD <offset>: () => *(globals+offset)
_global_load:
    jsr fetch
    clc
    adc globals
    tax
    ; duplicating this address into TOS via DUP

; DUP; (A) => (A) (A)
_dup:
    lda stackA+1,x
    pha
    lda stackA,x
    pha
    jmp loop

; Take following virtual address and adjust by base IP to get real address.
; Returns with (A=hi,Y=lo)
fetch2PlusAddBaseIP:
    jsr fetch
    clc
    adc baseip
    sta temp
    php
    jsr fetch
    plp
    adc baseip+1
    ldy temp
    rts

; LOADA <int>: () => (addr + int)
_loada:
    jsr fetch2PlusAddBaseIP
    pha ; high value on stack first
    tya
    pha ; low value second
    tsx
    ; Fall through to INCR. BASEIP is -1 from address.

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

; GLOBAL_STORE <offset>: (A) => (); *(globals+offset)=A
_global_store:
    jsr fetch
    clc
    adc globals
    bne store_common

; LOCAL_STORE <offset>: (A) => (); *(locals+offset)=A
_local_store:
    jsr fetch
    clc
    adc locals
store_common:
    tay
    pla
    sta stackA,y
    pla
    sta stackA+1,y
    jmp loop

; GOSUB <addr>; () => (IP); IP=addr
_gosub:
    jsr fetch2PlusAddBaseIP     ; A=hi,Y=lo
    tax                         ; a little bit more juggling here
    lda ip+1                    ; to push old IP on stack (after
    pha                         ; the 2 byte read) and store the
    lda ip                      ; new IP. Ick!
    pha
    stx ip+1
    sty ip
    jmp loop

; GOTO <addr>: IP=addr
_goto:
    jsr fetch2PlusAddBaseIP
    sta ip+1
    sty ip
    jmp loop

; FIXA: (tos) => (baseip + tos)
_fixa:
    clc
    lda baseip
    adc stackA,x
    sta stackA,x
    lda baseip+1
    adc stackA+1,x
    sta stackA+1,x
    jmp _incr   ; baseIP is offset by -1

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
tossAddrAndLoop:
    jsr fetch   ; need to toss these away
    jsr fetch
    jmp loop

; IFFALSE <addr>: (A) => (); A == 0 => IP=addr
_iffalse:
    pla
    pla
    lda stackA,x
    ora stackA+1,x
    beq _goto   ; zero == false
    bne tossAddrAndLoop

; EXIT: restore back to original SP
_exit: 
    ldx basesp
    txs
    rts

.ifdef DEBUG
.proc print
    pla
    sta traceptr
    pla
    sta traceptr+1
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
    lda traceptr+1
    pha
    lda traceptr
    pha
    rts
subcommands:
    and #$7f
    cmp #4
    bcs @not123
@printhex:
    tax
    ldy #1
    adc (traceptr),y
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
    inc traceptr
    bne @skip
    inc traceptr+1
@skip:
    ldy #0
    lda (traceptr),y
    rts
.endproc
.endif

code:
