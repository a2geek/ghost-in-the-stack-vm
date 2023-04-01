# Ghost in the Stack BASIC

This language is not meant to necessarily be complete and it is not meant to replicate 
Integer BASIC or Applesoft BASIC. But the mimicry is real.

Notes:
* `expr` denotes an expression. See the expression section for details.
* `var` is any expected variable name (with no short length restrictions).
  Valid variable names begin with a letter and continue with letters or digits.
* Code is case insensitive. `MYVAR` is the same as `myvar` is the same as `MyVar`.
  You may use `goto` or `GoTo` or `GOTO`.

## Data types

All variables are assumed to be 16 bit integers. Other data types exist in expressions.

| Data Type | Notes                                                                                                                              |
|:---------:|:-----------------------------------------------------------------------------------------------------------------------------------|
|  Integer  | 16-bit integer (-32768..32767).                                                                                                    |
|  Boolean  | True/False. All inequalities resolve to a Boolean. When added to an Integer, a Boolean is valued as `0` for False or `1` for True. |
|  String   | String constants. Enclosed in quotes (`"`). Mostly used by `print` statement or `asc(..)` function.                                |

## Subroutines / Functions

Subroutines may be declared at the top of the program. They have their own variable scope and, at this time,
do not share any access to global variables.

```basic
sub name(a,b,c)
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

If there are no parameters the parenthesis can be dropped as well.

## Statements

Generally, statements probably can be chained together with colons (':') but use separate lines if it doesn't 
parse correctly (most likely is a compiler error). 

### Assignment

`var = expr`

### Traditional control flow

Labels replace line numbers and `GOTO`/`GOSUB` targets those labels. `GOSUB` pushes the old code
position on the stack and `RETURN` restores that position.

```basic
label:
   ' ...
   goto label
   gosub label
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

### For loop

There is one caveat with the `for` statement. `step` is optional and defaults to `+1`. If the `step` expression is 
computed and is negative, the code generated will be incorrect. Positive increments will be fine. Negative 
constants will also be fine.

```basic
for var = expr to expr step expr
   ' ...
next var
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
value = peek(addr)
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

|            Operator             | Type   | Input               | Result        | Notes                                                                        |
|:-------------------------------:|:-------|:--------------------|:--------------|:-----------------------------------------------------------------------------|
|         `*`, `/`, `mod`         | Binary | Integer, Boolean    | Integer       | Multiplication, division, modulus.                                           |
|            `+`, `-`             | Binary | Integer, Boolean    | Integer       | Addition, subtraction.                                                       |
| `<`, `<=`, `>`, `>=`, `=`, `<>` | Binary | Integer/Boolean[^1] | Boolean       | Inequalities. Note `<=`, `>=` and `<>` do not exist.                         |
|           `or`, `and`           | Binary | Integer/Boolean[^1] | Boolean       | Logical operations. Note that these evaluate to 1 or 0 (not bit operations). |
|               `-`               | Unary  | Integer             | Integer       | Negation. This is only supported on constants at this time.                  |
|          `(` expr `)`           | Other  | Any                 | Same as input | Parenthesis to group subexpressions.                                         |
|            Functions            | Other  | See Functions       | See Functions | See Functions section.                                                       |
|              `var`              | Other  | Integer             | Integer       | Variable reference.                                                          |
|             number              | Other  | Integer             | Integer       | Constant integer value.                                                      |
|           `"string"`            | Other  | String              | String        | Constant string value.                                                       |
|         `true`, `false`         | Other  | Boolean             | Boolean       | Constant boolean value.                                                      |

[^1]: Types must match (that is you cannot compare an Integer to a Boolean).

## Functions

Any built-in functions are referenced here. These may ultimately be moved into some form of library.

| Function            | Notes                                                                           |
|:--------------------|:--------------------------------------------------------------------------------|
| `asc(string)`       | Returns the ASCII value of the first character of the string with high bit set. |
| `peek(addr)`        | Read by from memory location `addr`.                                            |
| `scrn(xexpr,yexpr)` | Read color of lores graphics point at `xexpr`, `yexpr` coordinate.              |

*** END ***
