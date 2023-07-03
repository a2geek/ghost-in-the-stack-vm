# Ghost in the Stack BASIC

This language is not meant to necessarily be complete and it is not meant to replicate 
Integer BASIC or Applesoft BASIC. But the mimicry is real.

Notes:
* `expr` denotes an expression. See the expression section for details.
* `var` is any expected variable name (with no short length restrictions).
  Valid variable names begin with a letter and continue with letters or digits.
* Code is case insensitive. `MYVAR` is the same as `myvar` is the same as `MyVar`.
  You may use `goto` or `GoTo` or `GOTO`.
* Long term expectation is that BASIC language will fork. With types starting to be
  available, the untyped usage (and assumption of being an integer) will move towards  
  a legacy compiler.

## Data types

All variables are assumed to be 16 bit integers. Other data types exist in expressions.

| Data Type | Notes                                                                                                                              |
|:---------:|:-----------------------------------------------------------------------------------------------------------------------------------|
|  Integer  | 16-bit integer (-32768..32767).                                                                                                    |
|  Boolean  | True/False. All inequalities resolve to a Boolean. When added to an Integer, a Boolean is valued as `0` for False or `1` for True. |
|  String   | String constants. Enclosed in quotes (`"`). Mostly used by `print` statement or `asc(..)` function.                                |

## Declarations

Subroutines may be declared at the top of the program. They have their own variable scope and, at this time,
do not share any access to global variables.

```basic
sub name(a,b,c)
    ...
end sub
```

Subroutines may also be declared fully typed as follows:

```basic
sub name(a as integer, b as boolean)
    ...
end sub
```

Function definitions are similar. At this time, as with everything else, the return value is assumed to be integer.

```basic
function name(a,b)
  ...
  return n
end function
```

Functions may also be declared fully typed as follows:

```basic
function name(a as integer,b as boolean) as integer
  ...
  return n
end function
```

Array parameters must not include any dimension size expression and 
are declared simply as `a() as integer`. Use the `ubound(...)` function
to determine length of array at runtime. Sample code:

```basic
function arrayTotal(myArray() as integer)
  dim total as integer, i as integer
  
  for i = 0 to ubound(myArray)
    total = total + myArray(i)
  next i
  
  return total
end function 
```

If there are no parameters the parenthesis can be dropped as well.

Variables may be declared with the `dim` statement:

```basic
dim a as integer, b as boolean
```

Arrays can also be initialized. Note that the `static` keyword is required:

```basic
dim static a() as integer = { 1, 2, 3, 4 }
```

## Statements

Generally, statements probably can be chained together with colons (':') but use separate lines if it doesn't 
parse correctly (most likely is a compiler error). 

### Assignment / Constants

The `let` keyword is optional (as usual). `const` declares a constant which is handy and can be reduced by the compiler before execution.

```basic
const addr = 0x300
var = expr
let var = expr
```

### Traditional control flow

Labels replace line numbers and `GOTO`/`GOSUB` targets those labels. `GOSUB` pushes the old code
position on the stack and `RETURN` restores that position.

```basic
label:
   ' ...
   goto label
   gosub label
   on n goto label1, label2, ...
   on n gosub label1, label2, ...
   return
```

### Subroutines

To call a subroutine, invoke it as follows:

```basic
call name(x,y)
```

Note the `call` is optional and if there are no parameters, the parenthesis can also be left off.

### Lores graphics

The usual commands are available for lores graphics.  Note that `color=` needs to be together as `color =` is 
interpreted as variable assignment.

```basic
gr
color= n
plot x,y
hlin x0,x1 at y
vlin y0,y1 at x
c = scrn(x,y)
```

### If/Then/Else

The traditional If/Then/Else statement. Note that generally, `if` end with `end if`. `expr` is considered true if the 
expression evaluates to non-zero and false if it evaluates to zero.

```basic
if expr then
  ' true statements
else
  ' false statements
end if
```

There is also a single line if statement similar to Integer BASIC's - meaning only a single statement follows.
(Do not expect this to remain too long. It is useful for some of the sources being used to vet the compiler.)

```basic
if expr then true_statement : statement2 : statement3 
```

`statement2` and `statement3` always execute, regardless of the value of `expr`. 

### Loops

#### For loop

There is one caveat with the `for` statement. `step` is optional and defaults to `+1`. If the `step` expression is 
computed and is negative, the code generated will be incorrect. Positive increments will be fine. Negative 
constants will also be fine.

```basic
for var = expr to expr step expr
   ' ...
   [ exit for ]
   ' ...
next var
```

#### Repeat loop

Test at end of loop (1 or more repeats). Repeats if the expression evaluates to false.

```basic
repeat
   ' ...
   [ exit repeat ]
   ' ...
until expr
```

#### While loop

Test at beginning of loop (0 or more repeats). Repeats if the expression evaluates to true.

```basic
while expr
   ' ...
   [ exit while ]
   ' ...
end while
```

#### Do ... Loop

Test at beginning of loop (0 or more repeats). `do while` repeats if the expression evaluates to true 
and `do until` repeats if the expression is false.

```basic
do [ while | until ] expr
   ' ...
   [ exit do ]
   ' ...
loop
```

Test at end of loop (1 or more repeats). `do while` repeats if the expression evaluates to true
and `do until` repeats if the expression is false.

```basic
do
   ' ...
   [ exit do ]
   ' ...
loop [ while | until ] expr
```

### Output

Text screen support matching Applesoft/Integer BASIC.

```basic
text
home
vtab expr
htab expr
print [ expr | sexpr | ',' | ';' ]
```

### Native interactions

Many old BASIC programs utilized machine language routines (ROM or otherwise) to enhance an application.
To support these interactions the following statements and constructs are available:

```basic
poke addr,expr
pokew addr,expr
value = peek(addr)
value = peekw(addr)
cpu.register.a = expr
cpu.register.x = expr
cpu.register.y = expr
call addr
value = cpu.register.a
value = cpu.register.x
value = cpu.register.y
```

The `cpu.register.[axy]` constructs are intended to be used with the `call` statement. Alone, they just 
store (or get) a value. The `call` statement will setup and store these values.

### Miscellaneous

Note that `end` should be placed at the end of all code. It is not automatically inserted by the compiler at this time.

```basic
end
```

## Expressions

The following operations are currently supported. They are listed in order of precedence.
(This has been done on the fly. Likely to change ... feel free to correct what is wrong.)

|            Operator             | Type   | Input               | Result        | Notes                                                                                                                                             |
|:-------------------------------:|:-------|:--------------------|:--------------|:--------------------------------------------------------------------------------------------------------------------------------------------------|
|         `*`, `/`, `mod`         | Binary | Integer, Boolean    | Integer       | Multiplication, division, modulus.                                                                                                                |
|            `+`, `-`             | Binary | Integer, Boolean    | Integer       | Addition, subtraction.                                                                                                                            |
| `<`, `<=`, `>`, `>=`, `=`, `<>` | Binary | Integer/Boolean[^1] | Boolean       | Inequalities. Note `<=`, `>=` and `<>` do not exist.                                                                                              |
|           `<<`, `>>`            | Binary | Integer/Boolean     | Integer       | Left and right bit shifts. If booleans are supplied, they are evaluated as 1 or 0.                                                                |
|       `or`, `and`, `xor`        | Binary | Integer/Boolean[^1] | Boolean       | Bit and Logical operations. If arguments are booleans, this is a logical true/false evaluation. If this is integer, this is a bitwise evaluation. |
|           `-`, `not`            | Unary  | Integer             | Integer       | Negate and not unary operations.                                                                                                                  |
|          `(` expr `)`           | Other  | Any                 | Same as input | Parenthesis to group subexpressions.                                                                                                              |
|            Functions            | Other  | See Functions       | See Functions | See Functions section.                                                                                                                            |
|              `var`              | Other  | Integer             | Integer       | Variable reference.                                                                                                                               |
|             number              | Other  | Integer             | Integer       | Constant integer value.                                                                                                                           |
|           `"string"`            | Other  | String              | String        | Constant string value.                                                                                                                            |
|         `true`, `false`         | Other  | Boolean             | Boolean       | Constant boolean value.                                                                                                                           |

Note that a number can be defined in decimal (`1234`), hexadecimal (`0x12ed`), or binary (`0b1010`).

[^1]: Types must match (that is you cannot compare an Integer to a Boolean).

## Functions

Any built-in functions are referenced here.

| Function            | Library   | Notes                                                                           |
|:--------------------|:----------|:--------------------------------------------------------------------------------|
| `abs(iexpr)`        | Math      | Returns the absolute value of `iexpr`.                                          |
| `asc(string)`       | Intrinsic | Returns the ASCII value of the first character of the string with high bit set. |
| `pdl(iexpr)`        | Misc      | Reads paddle specified by `iexpr`.                                              |
| `peek(addr)`        | Intrinsic | Read byte from memory location `addr`.                                          |
| `peekw(addr)`       | Intrinsic | Read word from memory location `addr`.                                          |
| `random()`          | Math      | Returns a random integer between `0` and `-32768`                               |
| `rnd(iexpr)`        | Math      | Returns a random number in the range `0` to `iexpr-1`.                          |
| `scrn(xexpr,yexpr)` | Lores     | Read color of lores graphics point at `xexpr`, `yexpr` coordinate.              |
| `sgn(iexpr)`        | Math      | Returns 1,0,-1 if `iexpr < 0`, or `iexpr = 0`, or `iexpr > 0`.                  |
| `ubound(arrayVar)`  | Intrinsic | Returns length of array.                                                        |

Note that the library specified is lowercase in the `uses` phrase. 
All library functions are currently prefixed by the library name.

*** END ***
