# Integer BASIC

The original BASIC for the Apple II!

> Note that my documentation is somewhat limited and is being built based upon
> online references and code being picked for compilation.  Expect that stuff
> needs to be fixed!

## Extensions

* Line continuation can be done if the line ends with a space and an underscore.
* Numbers can be declared with hex notation (`0xabc`).

Sample:
```basic
10  POKE 0x302,0xad: POKE 0x303,0x30: POKE 0x304,0xc0: _
    POKE 0x305,0x88: _
    POKE 0x306,0xd0: POKE 0x307,0x05: _
    POKE 0x308,0xce: POKE 0x309,0x01: POKE 0x30a,0x03
```

## Statements

Notes:
* `var` is a variable.
* `iexpr` is an expression that results in an integer.
* `sexpr` is an expression that results in a string.
* `[]` is optional syntax.

| Statement                                       | Notes                                                                    |
|:------------------------------------------------|:-------------------------------------------------------------------------|
| `call <iexpr>`                                  | -                                                                        |
| `clr`                                           | N/A                                                                      |
| `color=<iexpr>`                                 | -                                                                        |
| `del <iexpr>,<iexpr>`                           | N/A                                                                      |
| `dim <var>(<iexpr>)`                            | -                                                                        |
| `dim <var>$(<iexpr>)`                           | -                                                                        |
| `dsp <var>`                                     | N/A                                                                      |
| `end`                                           | -                                                                        |
| `for <var> = <iexpr> to <iexpr> [step <iexpr>]` | If step is variable with negative value, will not process correctly.     |
| `gosub <iexpr>`                                 | Only constant line numbers at this time.                                 |
| `goto <iexpr>`                                  | Only constant line numbers at this time.                                 |
| `gr`                                            | -                                                                        |
| `himem: <iexpr>`                                | N/A                                                                      |
| `hlin <iexpr>,<iexpr> at <iexpr>`               | -                                                                        |
| `if <iexpr> then <iexpr>`                       | Only constant line numbers at this time.                                 |
| `if <iexpr> then <statement>`                   | Only one statement follows IF. Anything after that is outside of the IF. |
| `in#<iexpr>`                                    | -                                                                        |
| `input ["prompt",] <var> [,<var>]`              | TODO                                                                     |                                                                
| `[let] <var> = <iexpr>`                         | -                                                                        |
| `[let] <var>$ [(<iexpr>)] = <sexpr>`            | TODO                                                                     |
| `list <iexpr> [,<iexpr>]`                       | N/A                                                                      |
| `lomeme: <iexpr>`                               | N/A                                                                      |
| `notrace`                                       | N/A                                                                      |
| `next <var>`                                    | -                                                                        |
| `plot <iexpr>,<iexpr>`                          | -                                                                        |
| `poke <iexpr>,<iexpr>`                          | Intrinsic. Evaluates to native instructions.                             |
| `pop`                                           | N/A?                                                                     |
| `pr#<iexpr>`                                    | -                                                                        |
| `print [<iexpr>\|<sexpr>\|;\|,]*`               | Tab calculation not implemented. TODO                                    |
| `rem ...`                                       | -                                                                        |
| `return`                                        | -                                                                        |
| `run [<iexpr>]`                                 | N/A                                                                      |
| `tab <iexpr>`                                   | -                                                                        |
| `trace`                                         | N/A                                                                      |
| `text`                                          | -                                                                        |
| `vlin <iexpr>,<iexpr> at <iexpr>`               | -                                                                        |
| `vtab <iexpr>`                                  | -                                                                        |

## Integer expressions (iexpr)

|               Operator               | Type    | Input         | Result                           | Notes                              |
|:------------------------------------:|:--------|:--------------|:---------------------------------|:-----------------------------------|
|                 `^`                  | Binary  | Integer       | Integer                          | Power. TODO                        |
|               `+`, `-`               | Unary   | Integer       | Integer                          | Negation                           |
|           `*`, `/`, `mod`            | Bianry  | Integer       | Integer                          | Multiplication, division, modulus. |
|               `+`, `-`               | Bianry  | Integer       | Integer                          | Addition, subtraction.             |
| `<`, `>`, `<=`, `>=`, `=`, `<>`, `#` | Integer | Integer       | Note that `#` is not equal.      | -                                  |
| `<`, `>`, `<=`, `>=`, `=`, `<>`, `#` | String  | Integer       | Note that `#` is not equal. TODO | -                                  |
|                `not`                 | Unary   | Integer       | Integer                          | TODO?                              |
|             `and`, `or`              | Binary  | Integer       | Integer                          | Logical AND/OR.                    |
|              Functions               | Other   | See Functions | See Functions                    | See Functions section.             |
|           Integer variable           | Integer | -             | -                                | -                                  |
|           String variable            | String  | -             | -                                | -                                  |
|               Constant               | Integer | -             | -                                | -                                  |
|             `(<iexpr>)`              | Other   | -             | -                                | -                                  |

## Functions

| Function                | Input   | Output  | Notes                                             |
|:------------------------|:--------|---------|---------------------------------------------------|
| `abs(<iexpr>)`          | Integer | Integer | -                                                 |
| `pdl(<iexpr>)`          | Integer | Integer | -                                                 |
| `peek(<iexpr>)`         | Integer | Integer | Intrinsic; this evaluates to native instructions. |
| `rnd(<iexpr>)`          | Integer | Integer | -                                                 |
| `sgn(<iexpr>)`          | Integer | Integer | -                                                 |
| `asc(<sexpr>)`          | String  | Integer | -                                                 |
| `len(<sexpr>)`          | String  | Integer | -                                                 |
| `scrn(<iexpr>,<iexpr>)` | Integer | Integer | -                                                 |

*** END ***
