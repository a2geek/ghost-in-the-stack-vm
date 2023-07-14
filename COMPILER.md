# Ghost in the Stack Compiler

This compiler is very simple. Do not expect much more than a straight compile.

## Overview

### ANTLR

Package(s): `a2geek.ghost.antlr`

This compiler uses [ANTLR](https://www.antlr.org/) to parse the source code. The entirety of the grammar is written
in the ANTLR grammar file `Basic.g4`. Part of the ANTLR tooling creates a visitor that can be used to walk the resulting
AST. That visitor is utilized to create a model of the language.

### Model

Package(s): `a2geek.ghost.model.basic.statement`, `a2geek.ghost.model.basic.expression`

The language model is the construct of a Program with various Statements and Expressions.

Note that `print` is a little more complicated than expected. It consists of a bunch of print "actions": print an integer,
print a newline, print a comma.

### Model visitors

Package(s): `a2geek.ghost.model.basic.visitor`

Instead of writing code that understands all of the language multiple times, a visitor type pattern was created. This is
a hand-coded class that handles dispatch to the specific subclasses and default activities. Current visitors do the 
following:
* Gather metadata to about the code to identify a complete list of variables. (Used to reserve space and assign offsets.)
* Minor optimizations to reduce constant expressions and a minor strength reduction of multiplying by 2 to become
  addition (`A*2` == `A+A`). This should expand at some point and likely become independent visitors.
* Code generation will transform the Basic model for the interpreter.

## Interpreter code.

Package(s): `a2geek.ghost.model.code`

All the interpreter code is captured into a code block. This is the final program listing which then generates "byte
code".

*** END ***