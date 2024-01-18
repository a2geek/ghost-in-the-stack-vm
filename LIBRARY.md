# Available Modules

> Note that if a module is used (via `uses "module"`), the default exports will be added to the public name space.
Therefore, if using Math, `min(a,b)` is part of the namespace. If this causes naming conflicts, the alternate
syntax does no exporting and `math.min(a,b)` calls the same function.

Sections:
* [Err](#err) - error handling module
* [Lores](#lores) - lores (GR) module
* [Math](#math) - math module
* [Memory](#memory) - memory module
* [Misc](#misc) - miscellaneous module
* [ProDOS](#prodos) - ProDOS support module
* [Runtime](#runtime) - Some of the runtime support module
* [Strings](#strings) - string support module
* [Text](#text) - text module

## Err

Module to support the `on error` handler(s).

| Name          |  Type   | Description                                       |
|:--------------|:-------:|:--------------------------------------------------|
| `err.number`  | Integer | Last error number. Used for `on error` handling.  |
| `err.message` | String  | Last error message. Used for `on error` handling. |

## Lores

Module to support the lores (GR) graphics of the Apple II.

| Name                  |  Type   | Description                                                         |
|:----------------------|:-------:|:--------------------------------------------------------------------|
| `color(c as integer)` |    -    | Sets the lores color.                                               |
| `gr()`                |    -    | Enable lores (GR) mode.                                             |
| `plot(x,y)`           |    -    | Plots a point at `x`, `y` on the lores screen.                      |
| `hlin(x0,x1,y)`       |    -    | Draws a horizontal line between `x0` and `x1` at vertical line `y`. |
| `vlin(y0,y1,x)`       |    -    | Draws a vertical line beteen `y0` and `y1` at horizontal line `x`.  |
| `scrn(x,y)`           | Integer | Returns the color value at point `x`, `y` on the lores screen.      |

## Math

Math related module.

| Name             |  Type   | Description                                                                       |
|:-----------------|:-------:|:----------------------------------------------------------------------------------|
| `random()`       | Integer | Generate a pseudo-random integer (16 bits).                                       |
| `rnd(n)`         | Integer | Generate a random number between `0` and `n-1`.                                   |
| `abs(n)`         | Integer | Absolute value of `n`.                                                            |
| `sgn(n)`         | Integer | Returns `-1` for negative values, `0` if `n` is `0`, and `1` for positive values. |
| `min(a,b)`       | Integer | Return the minimum value of `a` or `b`.                                           |
| `max(a,b)`       | Integer | Return the maximum value of `a` or `b`.                                           |
| `ipow(base,exp)` | Integer | Raise `base` to the `exp` power.                                                  |

## Memory

Note that these are mostly used by the runtime.

| Name               |  Type   | Description                                                                          |
|:-------------------|:-------:|:-------------------------------------------------------------------------------------|
| `memclr(p,bytes)`  |    -    | Clear memory at address `p` for `bytes` length.                                      |
| `heapalloc(bytes)` | Address | Allocate `bytes` of memory and return a pointer. (Very basic function at this time.) |

## Misc

Miscellaneous module with subroutines and functions that don't seem to fit anywhere else.

| Name       |  Type   | Description                              |
|:-----------|:-------:|:-----------------------------------------|
| `pdl(n)`   | Integer | Return the current value for paddle `n`. |
| `prnum(n)` |    -    | Enable output on slot `n`.               |
| `innum(n)` |    -    | Enable input on slot `n`.                |

## ProDOS

> This is a rather experimental module. There is chicanery, especially around strings. Not everything is tested. 
> Current memory areas to stay away from: 0x280-0x2ff is used to fake string handling.
> 0x300-~0x330 is used for the actual ProDOS MLI call area (assembly code as well as parameter blocks).

Supports ProDOS related functions and subroutines. This module has an initialization sequence that ensures a prefix has been set: 
if no prefix has been set, sets it to the last used device.

| Name                                            |  Type   | Description                                                                                          |
|:------------------------------------------------|:-------:|:-----------------------------------------------------------------------------------------------------|
| `mliErrorMesage(errnum)`                        | String  | Convert a MLI error number to a descriptive string.                                                  |
| `callMLI(functionCode)`                         |    -    | (Internal)  Calls an MLI routine. Raises an error if the return code is non-zero.                    |
| `quit()`                                        |    -    | Call QUIT ($65).                                                                                     |
| `readBlock(unitNumber,blockNumber,dataBuffer)`  |    -    | Call READ_BLOCK ($80).                                                                               |
| `writeBlock(unitNumber,blockNumber,dataBuffer)` |    -    | Call WRITE_BLOCK ($81).                                                                              |
| `getDate()`                                     | Integer | Call GET_TIME ($82) and return the date portion.                                                     |
| `getTime()`                                     | Integer | Call GET_TIME ($82) and return the time portion.                                                     |
| `createFile(pathname,filetype,auxtype)`         |    -    | Call CREATE ($C0) to create a new file.                                                              |
| `destroy(pathname)`                             |    -    | Call DESTROY ($C1) to delete a file or empty subdirectory.                                           |
| `rename(oldpathname,newpathname)`               |    -    | Call RENAME ($C2) to rename a file or directory.                                                     |
| `lock(pathname)`                                |    -    | Lock a file/directory. Uses GET_FILE_INFO ($C4) and SET_FILE_INFO ($C3) to set access bits to $C3.   |
| `unlock(pathname)`                              |    -    | Unlock a file/directory. Uses GET_FILE_INFO ($C4) and SET_FILE_INFO ($C3) to set access bits to $01. |
| `setPrefix(newprefix)`                          |    -    | Call SET_PREFIX ($C6) to set the default prefix.                                                     |
| `getPrefix()`                                   | String  | Call GET_PREFIX ($C7) to set the default prefix.                                                     |
| `open(filename,buffer)`                         | Integer | Call OPEN ($C8) to open a file. Returns `refnum`.                                                    |
| `newline(refnum,mask,char)`                     |    -    | Call NEWLINE ($C9).                                                                                  |
| `read(refnum,buffer,length)`                    | Integer | Call READ ($CA) to read from an open file. Returns number of bytes actually read.                    |
| `write(refnum,buffer,length)`                   | Integer | Call WRITE ($CB) to write to an open file. Returns number of bytes actually written.                 |
| `close(refnum)`                                 |    -    | Call CLOSE ($CC) to close an open file.                                                              |
| `flush(refnum)`                                 |    -    | Call FLUSH ($CD) to flush pending data to an open file.                                              |
| `lastDevice()`                                  | Integer | Returns last used device stored in DEVNUM ($BF30).                                                   |
| `isPrefixActive()`                              | Boolean | Indicates if there is an active prefix based on PFXPTR ($BF9A).                                      |
| `isAppleII()`                                   | Boolean | Indicates if the Apple II MACHID ($BF98) bits are set (`00..0...`).                                  |
| `isAppleIIplus()`                               | Boolean | Indicates if the Apple II+ MACHID ($BF98) bits are set (`01..0...`).                                 |
| `isAppleIIe()`                                  | Boolean | Indicates if the Apple IIe MACHID ($BF98) bits are set (`10..0...`).                                 |
| `isAppleIII()`                                  | Boolean | Indicates if the Apple III MACHID ($BF98) bits are set (`11..0...`).                                 |
| `isAppleIIc()`                                  | Boolean | Indicates if the Apple IIc MACHID ($BF98) bits are set (`10..1...`).                                 |
| `has48K()`                                      | Boolean | Indicates if MACHID ($BF98) indicates this machine has 48K (`..01....`).                             |
| `has64K()`                                      | Boolean | Indicates if MACHID ($BF98) indicates this machine has 64K (`..10....`).                             |
| `has128K()`                                     | Boolean | Indicates if MACHID ($BF98) indicates this machine has 128K (`..11....`).                            |
| `has80Cols()`                                   | Boolean | Indicates if MACHID ($BF98) indicates this machine has 80 columns (`......1.`).                      |
| `hasClock()`                                    | Boolean | Indicates if MACHID ($BF98) indicates this machine has a clock (`.......1`).                         |

## Runtime

The runtime support module has all the input and print routines, support for ON..GOTO/GOSUB, etc. This module is "used" in every application.

| Name                          | Type | Description                                        |
|:------------------------------|:----:|:---------------------------------------------------|
| `runtime.defaultErrorHandler` |  -   | The default `on error` handler for an application. |

## Strings

Support for the `string` data type. Very rudimentary.

| Name                                                      |  Type   | Description                                                        |
|:----------------------------------------------------------|:-------:|:-------------------------------------------------------------------|
| `strmax(s)`                                               | Integer | The maximum length supported by a string.                          |
| `asc(s)`                                                  | Integer | The ASCII value of a string. Most likely with the high bit set.    |
| `len(s)`                                                  | Integer | The length of a string.                                            |
| `strcmp(s1,s2)`                                           | Integer | String comparison. -1 if less than, 0 if equal, 1 if greater than. |
| `strcpy(target,targetStart,source,sourceStart,sourceEnd)` |    -    | Copy strings.                                                      |

## Text

| Name        | Type | Description                                  |
|:------------|:----:|:---------------------------------------------|
| `home()`    |  -   | Clear screen. Call to $FC58.                 |
| `text()`    |  -   | Set text mode. Call to $FB39.                |
| `htab(n)`   |  -   | Set horizontal screen position. Sets $24.    |
| `vtab(n)`   |  -   | Set vertical screen position. Call to $FB5B. |
| `normal()`  |  -   | Set normal text mode.                        |
| `inverse()` |  -   | Set inverse text mode.                       |
| `flash()`   |  -   | Set flashing text mode.                      |
