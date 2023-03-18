# Ghost in the Stack BASIC

This language is not meant to necessarily be complete and it is not meant to replicate 
Integer BASIC or Applesoft BASIC. But the mimicry is real.

Notes:
* `expr` denotes an expression. See the expression section for details.
* `var` is any expected variable name (with no short length restrictions).
  Valid variable names begin with a letter and continue with letters or digits.
* Code is case insensitive. `MYVAR` is the same as `myvar` is the same as `MyVar`.
  You may use `goto` or `GoTo` or `GOTO`.

## Statements

Generally, statements probably can be chained together with colons (':') but use separate lines if it doesn't 
parse correctly (most likely is a compiler error). 

### Assignment

`var = expr`

### Traditional control flow

Labels replace line numbers and `GOTO`/`GOSUB` targets those labels. `GOSUB` pushes the old code
position on the stack and `RETURN` restores that position.

```
label:
   ' ...
   goto label
   gosub label
   return
```

### Lores graphics

The usual commands are available for lores graphics.  Note that `color=` needs to be together as `color =` is 
interpreted as variable assignment.

```
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

```
if expr then
  ' true statements
else
  ' false statements
end if
```

There is also a single line if statement similar to Integer BASIC's - meaning only a single statement follows.

```
if expr then true_statement : statement2 : statement3 
```

`statement2` and `statement3` always execute, regardless of the value of `expr`. 

### For loop

There is one caveat with the `for` statement. `step` is optional and defaults to `+1`. If the `step` expression is 
computed and is negative, the code generated will be incorrect. Positive increments will be fine. Negative 
constants will also be fine.

```
for var = expr to expr step expr
   ' ...
next var
```

### Output

Text screen support matching Applesoft/Integer BASIC.

```
text
home
vtab expr
htab expr
print [ expr | sexpr | ',' | ';' ]
```

### Miscellaneous

Note that `end` should be placed at the end of all code. It is not automatically inserted by the compiler at this time.

```
end
poke addr,expr
value = peek(addr)
```

## Expressions

The following operations are currently supported. They are listed in order of precedence.
(This has been done on the fly. Likely to change ... feel free to correct what is wrong.)

|            Operator             | Type   | Notes                                                                        |
|:-------------------------------:|:-------|:-----------------------------------------------------------------------------|
|         `*`, `/`, `mod`         | Binary | Multiplication, division, modulus.                                           |
|            `+`, `-`             | Binary | Addition, subtraction.                                                       |
| `<`, `<=`, `>`, `>=`, `=`, `<>` | Binary | Inequalities. Note `<=`, `>=` and `<>` do not exist.                         |
|           `or`, `and`           | Binary | Logical operations. Note that these evaluate to 1 or 0 (not bit operations). |
|               `-`               | Unary  | Negation. This is only supported on constants at this time.                  |
|          `(` expr `)`           | Other  | Parenthesis to group subexpressions.                                         |
|            Functions            | Other  | See Functions section.                                                       |
|              `var`              | Other  | Variable reference.                                                          |
|             number              | Other  | Constant integer value.                                                      |

## Functions

Any built-in functions are referenced here. These may ultimately be moved into some form of library.

| Function            | Notes                                                                           |
|:--------------------|:--------------------------------------------------------------------------------|
| `asc(string)`       | Returns the ASCII value of the first character of the string with high bit set. |
| `peek(addr)`        | Read by from memory location `addr`.                                            |
| `scrn(xexpr,yexpr)` | Read color of lores graphics point at `xexpr`, `yexpr` coordinate.              |

*** END ***
