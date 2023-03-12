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

| Operand   | Length | Notes                                                                                                                                                                                                         |
|:----------|:------:|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EXIT`    |   1    | Exit interpreter. Simply restores stack pointer and exits.                                                                                                                                                    |
| `ADD`     |   1    | Add TOS and TOS-1.                                                                                                                                                                                            |
| `SUB`     |   1    | Subtract TOS from TOS-1. (Note: verify order.)                                                                                                                                                                |
| `MUL`     |   1    | Multiply TOS and TOS-1.                                                                                                                                                                                       |
| `DIV`     |   1    | Divide DOS by TOS-1. (Note: verify order.)                                                                                                                                                                    |
| `MOD`     |   1    | Capture the remainder from the division. See `DIV`.                                                                                                                                                           |
| `ILOAD`   |   1    | Indirect load. Read byte from address at TOS and place on stack.                                                                                                                                              |
| `ISTORE`  |   1    | Indirect store. Write byte at TOS-1 to address at TOS.                                                                                                                                                        |
| `LT`      |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `LE`      |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `EQ`      |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `SETACC`  |   1    | Set the 6502 Accumulator for next `CALL` from TOS.                                                                                                                                                            |
| `SETYREG` |   1    | Set the 6502 Y register for next `CALL` from TOS.                                                                                                                                                             |
| `CALL`    |   1    | "Native" 6502 call to address at TOS. Uses register settings from `SETACC`, `SETYREG`, and `SETXREG`.  After call, register settings preserved and will be available with `GETACC`, `GETYREG`, and `GETXREG`. |
| `RESERVE` |   2    | Reserve space on the stack for variables. Currently assumed to be 16 bits.                                                                                                                                    |
| `LOAD`    |   2    | Load local variable to TOS. _Offset is in bytes._                                                                                                                                                             |
| `STORE`   |   2    | Store TOS to local variable. _Offset is in bytes._                                                                                                                                                            |
| `GOTO`    |   3    | Go to given offset address. (Interpreter address is zero-based; adds to code base address to find physical address.)                                                                                          |
| `IFTRUE`  |   3    | If TOS is non-zero, goto given address. See `GOTO`.                                                                                                                                                           |
| `IFFALSE` |   3    | If TOS is zero, goto given address. See `GOTO`.                                                                                                                                                               |
| `LOADC`   |   3    | Load 16-bit value to TOS.                                                                                                                                                                                     |

*** END ***
