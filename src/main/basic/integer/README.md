# About

These are programs based on Integer BASIC ("Game" BASIC) 
from the Apple II.

Files are named after filename as found on disk.

# 6502 code

Many of these games seem to have very similar sound routines beginning at location 0, which causes
some trouble due to the interpreter's ZP usage as well as various Applesoft routines being used.
This is just to track original and modified sources to (hopefully) get some reuse from the rewrite.

## Variant 1

Games: 
* anti-aircraft 
* animations 
* apple-tone
* slot-machine-2-with-sound

Usage:
```basic
POKE 0x300,pitch?
POKE 0x301,duration?
CALL 0x302
```

Original:
```basic
20160  POKE 2,173: POKE 3,48: POKE 4,192: POKE 5,136: POKE 6,208: POKE 7,4: POKE 8,198: POKE 9,1: POKE 10,240
20170  POKE 11,8: POKE 12,202: POKE 13,208: POKE 14,246: POKE 15,166: POKE 16,0: POKE 17,76: POKE 18,2: POKE 19,0: POKE 20,96
```

Modified:
```basic
20150 REM _
    Moved this to $300 to not conflict with interpreter _
        0302-   AD 30 C0    LDA   $C030 _
        0305-   88          DEY _
        0306-   D0 05       BNE   $030D _
        0308-   CE 01 03    DEC   $0301 _
        030B-   F0 09       BEQ   $0316 _
        030D-   CA          DEX _
        030E-   D0 F5       BNE   $0305 _
        0310-   AE 00 03    LDX   $0300 _
        0313-   4C 02 03    JMP   $0302 _
        0316-   60          RTS
20160  POKE 0x302,0xad: POKE 0x303,0x30: POKE 0x304,0xc0: _
       POKE 0x305,0x88: _
       POKE 0x306,0xd0: POKE 0x307,0x05: _
       POKE 0x308,0xce: POKE 0x309,0x01: POKE 0x30a,0x03
20170  POKE 0x30b,0xf0: POKE 0x30c,0x09: _
       POKE 0x30d,0xca: _
       POKE 0x30e,0xd0: POKE 0x30f,0xf5: _
       POKE 0x310,0xae: POKE 0xe11,0x00: POKE 0xe12,0x03: _
       POKE 0x313,0x4c: POKE 0x314,0x02: POKE 0x315,0x03: _
       POKE 0x316,0x60
```

## Variant 2

Games:
* barricade
* demo-l

Usage:
```basic
POKE 0x300,pitch?
POKE 0x301,duration(lo)?
POKE 0x302,duration(hi)?
CALL 0x303
```

Original:
```basic
100  POKE 2,173: POKE 3,48: POKE 4,192: POKE 5,165: POKE 6,0: POKE 7,32: POKE 8,168: POKE 9,252: POKE 10,165: POKE 11,1: POKE 12,208: POKE 13,4: POKE 14,198
110  POKE 15,24: POKE 16,240: POKE 17,5: POKE 18,198: POKE 19,1: POKE 20,76: POKE 21,2: POKE 22,0: POKE 23,96
```

Modified:
```basic
99  REM _
    Modification: Sound routine was in ZP @ $02. _
    Impacted interpreter and had trouble moving the interpreter where Applesoft _
    did not impact. So moving the routine to $300. _
        0303-   AD 30 C0    LDA   $C030 _
        0306-   AD 00 03    LDA   $0300 _
        0309-   20 A8 FC    JSR   $FCA8 _
        030C-   AD 01 03    LDA   $0301 _
        030F-   D0 05       BNE   $0316 _
        0311-   CE 02 03    DEC   $0302 _
        0314-   F0 06       BEQ   $031C _
        0316-   CE 01 03    DEC   $0301 _
        0319-   4C 03 03    JMP   $0303 _
        031C-   60          RTS
100  POKE 0x303,0xad: POKE 0x304,0x30: POKE 0x305,0xc0: _
     POKE 0x306,0xad: POKE 0x307,0x00: POKE 0x308,0x03: _
     POKE 0x309,0x20: POKE 0x30a,0xa8: POKE 0x30b,0xfc: _
     POKE 0x30c,0xad: POKE 0x30d,0x01: POKE 0x30e,0x03
110  POKE 0x30f,0xd0: POKE 0x310,0x05: _
     POKE 0x311,0xce: POKE 0x312,0x02: POKE 0x313,0x03: _
     POKE 0x314,0xf0: POKE 0x315,0x06: _
     POKE 0x316,0xce: POKE 0x317,0x01: POKE 0x318,0x03: _
     POKE 0x319,0x4c: POKE 0x31a,0x03: POKE 0x31b,0x03: _
     POKE 0x31c,0x60
```
