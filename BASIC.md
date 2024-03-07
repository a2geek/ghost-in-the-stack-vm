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

## Programs and Modules

Some namespace capabilities are enabled via modules. Modules are included via the `uses` statement.
Any function or subroutine in a module marked as `export` will automatically be aliased as the simple function
name. For instance, to use the `min` function that is in the Math package, it can be referenced as 
`math.min(a,b)`, or if a `uses "math"` is done, as `min(a,b)`. Note that a module name must match the filename.

The primary compilation unit must be a program (aka, no module statement). However, routines can be separated 
and included with a `uses` statement without modules.

Examples:

`mymodule.bas`:
```basic
module mymodule
  export sub hithere()
    print "in hithere"
  end sub
  
  sub heythere()
    print "in heythere"
  end sub
end module
```

`myprogram.bas`:
```basic
uses "mymodule"

hithere()             ' exported function aliased into primary namespace
mymodule.heythere()   ' unexportedfunction not available
```

## Option

The `option` keyword allows some compiler settings to be embedded in the application itself.

| Option                         | Description                                                                                      |
|:-------------------------------|:-------------------------------------------------------------------------------------------------|
| `option strict`                | Requires all variables to be declared instead of being implicitly created on first use.          |
| `option heap` ( `lomem=ADDR` ) | Enable heap (instead of stack) for array allocation. `lomem` defaults to $8000 if not specified. |

## Data types

All variables are assumed to be 16 bit integers. Other data types exist in expressions.

| Data Type | Notes                                                                                                                              |
|:---------:|:-----------------------------------------------------------------------------------------------------------------------------------|
|  Address  | Represents a generic pointer.                                                                                                      |
|  Integer  | 16-bit integer (-32768..32767).                                                                                                    |
|  Boolean  | True/False. All inequalities resolve to a Boolean. When added to an Integer, a Boolean is valued as `0` for False or `1` for True. |
|  String   | String constants. Enclosed in quotes (`"`). Mostly used by `print` statement or `asc(..)` function.                                |

## Declarations

Subroutines may be declared at the top of the program. They have their own variable scope and share access to global variables.

```basic
[visibility] [modifiers] sub name(a,b,c)
    ...
end sub
```

Visibility flags:

| Name      | Optional? | Description                                                        |
|:----------|:---------:|:-------------------------------------------------------------------|
| `public`  |  Default  | Marks routine as publicly available anywhere. This is the default. |
| `private` | Optional  | Marks routine as private to the scope (application or module).     |

Modifier flags:

| Name       | Optional? | Description                                                                                                                                         |
|:-----------|:---------:|:----------------------------------------------------------------------------------------------------------------------------------------------------|
| `export`   | Optional  | Marks the routine as auto exported, meaning that instead of `math.min(...)`, `min(...)` can be used instead.                                        |
| `inline`   | Optional  | Marks the routine as inline capable. It's code will be folded into the caller. This is akin to an assembler macro.                                  |
| `volatile` | Optional  | Marks the routine as volatile. That is, the return value varies outside the parameters. Functions like PDL, RND, and ALLOC are considered volatile. |

Subroutines may also be declared fully typed as follows:

```basic
sub name(a as integer, b as boolean)
    ...
end sub
```

Function definitions are similar. At this time, as with everything else, the return value is assumed to be integer. Note that with `option strict` enabled, this is not allowed.

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

### Error handling

To control error handling, the following statements are available. They allow the programmer to take control 
when an error situation occurs and also allow generating error codes. Data is preserved in the `err` module,
but only the current error details are available. There is a default `ON ERROR` handler in the runtime.

```basic
on error goto <label>
on error disable
on error resume next   ' not implemented yet
raise error <number> [, <message> [, <context>]]
```

`raise error` allows up to 3 arguments, two that are optional. A runtime example of using all three is the array
boundary check code. That code effectively does a `raise error 107, "ARRAY INDEX OUT OF BOUNDS", "?"` where the `?` 
is replaced with the "offending" variable name.

Note that `ON ERROR DISABLE` disables the users' error handling and sets it back to the system default error
handler.

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
uses "lores"
gr()
color(n)
plot(x,y)
hlin(x0,x1,y)
vlin(y0,y1,x)
c = scrn(x,y)
```

### If/ElseIf/Else

The traditional If/ElseIf/Else statement. Note that generally, `if` ends with `end if`. `expr` is considered true if the 
expression evaluates to non-zero and false if it evaluates to zero.

```basic
if expr then
  ' true statements
[ elseif expr2 then
  ' true2 statements ]
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

### Select ... Case

The `select case` statement can be a more readable structure compared to a series of `if` statements.

```basic
select [ case ] <expr>
case <expr2>
  statements1
case <expr3> to <expr4>
  statements2
case [ is ] ( <, <=, >, >=, =, <> ) expr5
  statements3
case else
  statements4
end select
```

### Loops

#### For loop

There is one caveat with the `for` statement. `step` is optional and defaults to `+1`. If the `step` expression is 
computed and is negative, the code generated will be incorrect. Positive increments will be fine. Negative 
constants will also be fine.

```basic
for var = expr to expr step expr
   ' ...
   [ continues for ]
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
   [ continue repeat ]
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
   [ continue while ]
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
   [ continue do ]
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
   [ continue do ]
   ' ...
   [ exit do ]
   ' ...
loop [ while | until ] expr
```

### Output

Text screen support matching Applesoft/Integer BASIC.

```basic
uses "text"
text()
home()
vtab(expr)
htab(expr)

' Note that print is part of the runtime and not associated to the `text` module.
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

| Function                 | Library   | Notes                                                                           |
|:-------------------------|:----------|:--------------------------------------------------------------------------------|
| `abs(iexpr)`             | Math      | Returns the absolute value of `iexpr`.                                          |
| `addrof(var)`            | Intrinsic | Calculate address of a variable.                                                |
| `asc(string)`            | Intrinsic | Returns the ASCII value of the first character of the string with high bit set. |
| `cbool(expr)`            | Intrinsic | Converts expression to boolean.                                                 |
| `cbyte(expr)`            | Intrinsic | Converts expression to byte.                                                    |
| `cint(expr)`             | Intrinsic | Converts expression to integer.                                                 |
| `pdl(iexpr)`             | Misc      | Reads paddle specified by `iexpr`.                                              |
| `peek(addr)`             | Intrinsic | Read byte from memory location `addr`.                                          |
| `peekw(addr)`            | Intrinsic | Read word from memory location `addr`.                                          |
| `random()`               | Math      | Returns a random integer between `0` and `-32768`                               |
| `rnd(iexpr)`             | Math      | Returns a random number in the range `0` to `iexpr-1`.                          |
| `scrn(xexpr,yexpr)`      | Lores     | Read color of lores graphics point at `xexpr`, `yexpr` coordinate.              |
| `sgn(iexpr)`             | Math      | Returns 1,0,-1 if `iexpr < 0`, or `iexpr = 0`, or `iexpr > 0`.                  |
| `ubound(arrayVar[,dim])` | Intrinsic | Returns length of an array dimension (default is 1).                            |

Note that the library specified is lowercase in the `uses` phrase. 
All library functions are currently prefixed by the library name.

*** END ***
