# Ghost in the Stack Opcodes

This is a very light set of documentation on the opcodes in the interpreter.

## Overview

All operations are on the stack. Therefore, this is (naturally) a stack-based interpreter.

At this point, since the interpreter and compiler are intertwined, the opcodes are
*not* using a reserved value. They are assigned in Java by the ordinal value in the
enumeration and in the assembly projects by the order in which they are specified.
Therefore, the `Opcode.java` and `interp.asm` just need to be ordered the same.

## Operations

> Notes:
> * Stack references assume 16 bits at this time. So TOS or TOS-1 is two bytes.
> * Some likely operations are mentioned. They may not exist yet and won't until they are needed.

| Operand          | Length | Notes                                                                                                                                                                                                         |
|:-----------------|:------:|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EXIT`           |   1    | Exit interpreter. Simply restores stack pointer and exits.                                                                                                                                                    |
| `ADD`            |   1    | Add TOS and TOS-1.                                                                                                                                                                                            |
| `SUB`            |   1    | Subtract TOS from TOS-1. (Note: verify order.)                                                                                                                                                                |
| `MUL`            |   1    | Multiply TOS and TOS-1.                                                                                                                                                                                       |
| `DIVU`           |   1    | Unsigned divide TOS by TOS-1. (Note: verify order.)                                                                                                                                                           |
| `MODU`           |   1    | Unsigned remainder from the division. See `DIVU`.                                                                                                                                                             |
| `DIVS`           |   1    | Signed divide TOS by TOS-1.                                                                                                                                                                                   |
| 'MODS`           |   1    | Signed remainder from division. See `DIVS'.                                                                                                                                                                   |
| `NEG`            |   1    | Negate number on TOS.                                                                                                                                                                                         |
| `ILOAD`          |   1    | Indirect load. Read byte from address at TOS and place on stack.                                                                                                                                              |
| `ISTORE`         |   1    | Indirect store. Write byte at TOS-1 to address at TOS.                                                                                                                                                        |
| `LT`             |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `LE`             |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `EQ`             |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `NE`             |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `OR`             |   1    | Bitwise OR of TOS and TOS-1. This is a bitwise OR.                                                                                                                                                            |
| `AND`            |   1    | Bitwise AND of TOS and TOS-1. This is a bitwise AND.                                                                                                                                                          |
| `XOR`            |   1    | Exclusive OR of TOS and TOS-1. This is a bitwise exclusive OR.                                                                                                                                                |
| `SHIFTL`         |   1    | Shift TOS-1 left by TOS bits. Constrained to a max of 15 shifts.                                                                                                                                              |
| `SHIFTR`         |   1    | Shift TOS-1 right by TOS bits. Constrained to a max of 15 shifts.                                                                                                                                             |                                 
| `SETACC`         |   1    | Set the 6502 Accumulator for next `CALL` from TOS.                                                                                                                                                            |
| `GETACC`         |   1    | Get the 6502 Accumulator from last `CALL` and place on stack.                                                                                                                                                 |
| `SETXREG`        |   1    | Set the 6502 X register for next `CALL` from TOS.                                                                                                                                                             |
| `GETXREG`        |   1    | Get the 6502 X register from last `CALL` and place on stack.                                                                                                                                                  |
| `SETYREG`        |   1    | Set the 6502 Y register for next `CALL` from TOS.                                                                                                                                                             |
| `GETYREG`        |   1    | Get the 6502 Y register from last `CALL` and place on stack.                                                                                                                                                  |
| `CALL`           |   1    | "Native" 6502 call to address at TOS. Uses register settings from `SETACC`, `SETYREG`, and `SETXREG`.  After call, register settings preserved and will be available with `GETACC`, `GETYREG`, and `GETXREG`. |
| `RETURN`         |   1    | Pop return address off stack and store in IP.                                                                                                                                                                 |
| `DUP`            |   1    | Duplicate top item on stack.                                                                                                                                                                                  |
| `INCR`           |   1    | Incrment TOS by 1.                                                                                                                                                                                            |
| `DECR`           |   1    | Decrement TOS by 1.                                                                                                                                                                                           |
| `GLOBAL_RESERVE` |   2    | Set global variable pointer and reserve space on the stack for global variables. _Size is in bytes._                                                                                                          |
| `GLOBAL_LOAD`    |   2    | Load global variable to TOS. _Offset is in bytes._                                                                                                                                                            |
| `GLOBAL_STORE`   |   2    | Store TOS to global variable. _Offset is in bytes._                                                                                                                                                           |
| `LOCAL_RESERVE`  |   2    | Preserve prior local variable pointer and reserve space on the stack for local variables. _Size is in bytes._                                                                                                 |
| `LOCAL_LOAD`     |   2    | Load local variable to TOS. _Offset is in bytes._                                                                                                                                                             |
| `LOCAL_STORE`    |   2    | Store TOS to local variable. _Offset is in bytes._                                                                                                                                                            |
| `LOCAL_FREE`     |   2    | Free local variable space and restore prior local variable pointer from stack.                                                                                                                                |
| `POPN`           |   2    | Pop N bytes from the stack. Used after subroutine calls.                                                                                                                                                      |
| `GOTO`           |   3    | Go to given offset address. (Interpreter address is zero-based; adds to code base address to find physical address.)                                                                                          |
| `GOSUB`          |   3    | Push return address on stack. Go to given offset address. See `GOTO` notes.                                                                                                                                   |
| `IFTRUE`         |   3    | If TOS is non-zero, goto given address. See `GOTO`.                                                                                                                                                           |
| `IFFALSE`        |   3    | If TOS is zero, goto given address. See `GOTO`.                                                                                                                                                               |
| `LOADC`          |   3    | Load 16-bit value to TOS.                                                                                                                                                                                     |
| `LOADA`          |   3    | Load physical address for given interpreter address. Used to support embedded constant values such as strings.                                                                                                |


### Stack Frame

The calling sequence for a procedure or fucntion in Ghost is as follows:

```
|       ...       | Bytes | Opcodes...  
|-----------------|-------|-------------
| return value    |   2   | LOADC #0000
| argument 1      |   2   | LOAD A
|  ... argument n |   2   | LOAD B
| return address  |   2   | GOSUB function
| prior local ptr |   1   | LOCAL_RESERVE n
| ... local vars  |       | ...
| ... expr stack  |       | LOCAL_FREE n
```

*** END ***
