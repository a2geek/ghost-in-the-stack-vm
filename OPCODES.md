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

| Operand        | Length | Notes                                                                                                                                                                                                         |
|:---------------|:------:|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EXIT`         |   1    | Exit interpreter. Simply restores stack pointer and exits.                                                                                                                                                    |
| `ADD`          |   1    | Add TOS and TOS-1.                                                                                                                                                                                            |
| `SUB`          |   1    | Subtract TOS from TOS-1. (Note: verify order.)                                                                                                                                                                |
| `MUL`          |   1    | Multiply TOS and TOS-1.                                                                                                                                                                                       |
| `DIVU`         |   1    | Unsigned divide TOS by TOS-1. (Note: verify order.)                                                                                                                                                           |
| `MODU`         |   1    | Unsigned remainder from the division. See `DIVU`.                                                                                                                                                             |
| `DIVS`         |   1    | Signed divide TOS by TOS-1.                                                                                                                                                                                   |
| `MODS`         |   1    | Signed remainder from division. See `DIVS'.                                                                                                                                                                   |
| `NEG`          |   1    | Negate number on TOS.                                                                                                                                                                                         |
| `ILOADB`       |   1    | Indirect load. Read byte from address at TOS and place on stack.                                                                                                                                              |
| `ISTOREB`      |   1    | Indirect store. Write byte at TOS-1 to address at TOS.                                                                                                                                                        |
| `ILOADW`       |   1    | Indirect load. Read word from address at TOS and place on stack.                                                                                                                                              |
| `ISTOREW`      |   1    | Indirect store. Write word at TOS-1 to address at TOS.                                                                                                                                                        |
| `LT`           |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `LE`           |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `EQ`           |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `NE`           |   1    | Compare TOS to TOS-1. Places 1 or 0 to represent true or false.                                                                                                                                               |
| `OR`           |   1    | Bitwise OR of TOS and TOS-1. This is a bitwise OR.                                                                                                                                                            |
| `AND`          |   1    | Bitwise AND of TOS and TOS-1. This is a bitwise AND.                                                                                                                                                          |
| `XOR`          |   1    | Exclusive OR of TOS and TOS-1. This is a bitwise exclusive OR.                                                                                                                                                |
| `SHIFTL`       |   1    | Shift TOS-1 left by TOS bits. Constrained to a max of 15 shifts.                                                                                                                                              |
| `SHIFTR`       |   1    | Shift TOS-1 right by TOS bits. Constrained to a max of 15 shifts.                                                                                                                                             |                                 
| `SETACC`       |   1    | Set the 6502 Accumulator for next `CALL` from TOS.                                                                                                                                                            |
| `GETACC`       |   1    | Get the 6502 Accumulator from last `CALL` and place on stack.                                                                                                                                                 |
| `SETXREG`      |   1    | Set the 6502 X register for next `CALL` from TOS.                                                                                                                                                             |
| `GETXREG`      |   1    | Get the 6502 X register from last `CALL` and place on stack.                                                                                                                                                  |
| `SETYREG`      |   1    | Set the 6502 Y register for next `CALL` from TOS.                                                                                                                                                             |
| `GETYREG`      |   1    | Get the 6502 Y register from last `CALL` and place on stack.                                                                                                                                                  |
| `CALL`         |   1    | "Native" 6502 call to address at TOS. Uses register settings from `SETACC`, `SETYREG`, and `SETXREG`.  After call, register settings preserved and will be available with `GETACC`, `GETYREG`, and `GETXREG`. |
| `RETURN`       |   1    | Pop return address off stack and store in IP.                                                                                                                                                                 |
| `DUP`          |   1    | Duplicate top item on stack.                                                                                                                                                                                  |
| `INCR`         |   1    | Increment TOS by 1.                                                                                                                                                                                           |
| `DECR`         |   1    | Decrement TOS by 1.                                                                                                                                                                                           |
| `PUSHZ`        |   1    | Push TOS zeros onto stack.                                                                                                                                                                                    |
| `LOADSP`       |   1    | Load SP onto stack.                                                                                                                                                                                           |
| `STORESP`      |   1    | Store SP from stack.                                                                                                                                                                                          |
| `LOADLP`       |   1    | Load LP (local variable pointer) onto stack.                                                                                                                                                                  |
| `STORELP`      |   1    | Store (and use) SP from stack.                                                                                                                                                                                |
| `LOADGP`       |   1    | Load GP (global variable pointer) onto stack.                                                                                                                                                                 |
| `STOREGP`      |   1    | Store (and use) GP from stack.                                                                                                                                                                                |
| `FIXA`         |   1    | Fix TOS to be real address. (TOS + Base IP address + 1)                                                                                                                                                       |
| `GLOBAL_LOAD`  |   2    | Load global variable to TOS. _Offset is in bytes._                                                                                                                                                            |
| `GLOBAL_STORE` |   2    | Store TOS to global variable. _Offset is in bytes._                                                                                                                                                           |
| `GLOBAL_INCR`  |   2    | Increment in place a global variable.                                                                                                                                                                         |
| `GLOBAL_DECR`  |   2    | Decrement in place a global variable.                                                                                                                                                                         |
| `LOCAL_LOAD`   |   2    | Load local variable to TOS. _Offset is in bytes._                                                                                                                                                             |
| `LOCAL_STORE`  |   2    | Store TOS to local variable. _Offset is in bytes._                                                                                                                                                            |
| `LOCAL_INCR`   |   2    | Increment in place a local variable.                                                                                                                                                                          |
| `LOCAL_DECR`   |   2    | Decrement in place a local variable.                                                                                                                                                                          |
| `POPN`         |   2    | Pop N bytes from the stack. Used after subroutine calls.                                                                                                                                                      |
| `GOTO`         |   3    | Go to given offset address. (Interpreter address is zero-based; adds to code base address to find physical address.)                                                                                          |
| `GOSUB`        |   3    | Push return address on stack. Go to given offset address. See `GOTO` notes.                                                                                                                                   |
| `IFNZ`         |   3    | If TOS is non-zero, goto given address. See `GOTO`.                                                                                                                                                           |
| `IFZ`          |   3    | If TOS is zero, goto given address. See `GOTO`.                                                                                                                                                               |
| `LOADC`        |   3    | Load 16-bit value to TOS.                                                                                                                                                                                     |
| `LOADA`        |   3    | Load physical address for given interpreter address. Used to support embedded constant values such as strings.                                                                                                |
| `LOAD0`        |   1    | Load 0 to TOS.                                                                                                                                                                                                |
| `LOAD1`        |   1    | Load 1 to TOS.                                                                                                                                                                                                |
| `LOAD2`        |   1    | Load 2 to TOS.                                                                                                                                                                                                |
| `POP2`         |   1    | Pop 2 bytes off TOS.                                                                                                                                                                                          |
| `POP`          |   `    | Pop TOS bytes off of TOS.                                                                                                                                                                                     |

### Stack Frame

The calling sequence for a procedure or fucntion in Ghost is as follows:

```
|       ...       | Bytes | Opcodes...  
|-----------------|-------|-------------
| return value    |   2   | LOADC #0000
| argument 1      |   2   | LOAD A
|  ... argument n |   2   | LOAD B
| return address  |   2   | GOSUB FUNCTION
| clean up        |   2   | POPN n (4 bytes in this example)
|       ...       |  ...  | ...
| FUNCTION:       |   0   | (label)
| save local ptr  |   1   | LOADLP
| initialize...   |   3   | LOADC n
| ... local vars  |   1   | PUSHZ
| use SP...       |   1   | LOADSP
| ... minus 1     |   1   | DECR
| ... to set LP   |   1   | STORELP
| <code>          |  ...  | ...
| !ALWAYS EXITS!  |   3   | GOTO FUNCEXIT
| FUNCEXIT:       |   0   | (label)
| clean up locals |   2   | POPN n
| restore LP (TOS)|   1   | STORELP
| exit function   |   1   | RETURN
```

*** END ***
