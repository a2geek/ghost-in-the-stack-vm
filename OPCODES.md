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
| `DIV`            |   1    | Divide DOS by TOS-1. (Note: verify order.)                                                                                                                                                                    |
| `MOD`            |   1    | Capture the remainder from the division. See `DIV`.                                                                                                                                                           |
| `ILOAD`          |   1    | Indirect load. Read byte from address at TOS and place on stack.                                                                                                                                              |
| `ISTORE`         |   1    | Indirect store. Write byte at TOS-1 to address at TOS.                                                                                                                                                        |
| `LT`             |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `LE`             |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `EQ`             |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `NE`             |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `OR`             |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `AND`            |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
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

*** END ***
