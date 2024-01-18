# Ghost in the Stack

[![Build](https://github.com/a2geek/ghost-in-the-stack-vm/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/a2geek/ghost-in-the-stack-vm/actions/workflows/build.yml)
![License](https://img.shields.io/github/license/a2geek/ghost-in-the-stack-vm)

Experimental bytecode interpreter that exists only in the 6502 stack. Also includes to beginnings
of a multi-BASIC compiler. Support for Integer BASIC and "Ghost" BASIC (a more modern BASIC) exists
at this time.

## History

This entire project stems from a time when I was writing ProDOS commands. I was struck
that each command would reserve memory and remained resident until ProDOS was restarted.
I began to wonder if there was some form of interpreted language that might be inserted
instead along with a control program to lookup other commands. Like a "real" operating
system.

Hence, the guidelines:
* Relatively small.
* Enough of an instruction set to accomplish command-line tasks.
* Enough of an instruction set to write the control program.
  (Search for a command, load it, transfer control to said command.)
* Be as untraceable as possible. Do as much on the stack as possible. Don't expect
  there to be a heap available.  Try to minimize zero page usage (and store/restore)
  what does get used.
* Be enabled by a higher level language.

I don't think this interpreter will fit the bill, but it sure has been (and still is!)
instructive and enjoyable to write.

## Getting started

Requirements (for development):
* This project requires Java 21.
* make. See `sourceSets.doFirst` in [build.gradle](build.gradle).
* cc65. See [Makefile](src/main/asm/Makefile).

Additional:
* A Java capable IDE (any of the major IDEs should be fine).
* Since the language parsing itself is handled by ANTLR4, an ANTLR4 capable plugin can be
useful.

When developing in an IDE, please note that where the ANTLR plugin may place the generated 
ANTLR code may differ from where the IDE (or Gradle) expects that code to belong. Generally,
when either updating the grammar, _or first checking it out_, you will need to build with Gradle.
There may be IDE commands to do this in place.

From the terminal, in the root of the project, use:
```shell
./gradlew clean build
```

This will result in an executable JAR file being produced in `build/libs/GhoshBasic-<version>-SNAPSHOT.jar`.

A sample compilation would be:
```shell
java -jar build/libs/GhostBasic-1.0-SNAPSHOT.jar src/main/basic/integer/advanced-dragon-maze.bas
```

Note that Integer BASIC can also be compiled like this:
```shell
java -jar build/libs/GhostBasic-1.0-SNAPSHOT.jar --integer src/main/basic/integer/demobasic.int
```

Like all compilers, `a.out` is the default output name.

Since the CLI is rather rudimentary at this time, the compiler has a lot of extra output. It should fail
if an error is encountered, however. If successful, the initial model is dumped out (which includes the runtime
routines brought in), all variables (broken down by scope - MAIN, a function, or a subroutine), and then the
rewritten model which has some minor optimizations at this time.

The compiler can be invoked with `--help` to see a list of options:
```shell
$ java -jar build/libs/GhostBasic-1.0-SNAPSHOT.jar --help
Usage: compile [-hV] [--case-sensitive] [--debug] [--fix-control-chars] [--heap] [--integer] [--quiet] [--trace] [-il=<intermediateCodeListing>] [--lomem=<heapStartAddress>] [-o=<outputFile>] [--symbols=<symbolTableFile>] [-tl=<targetCodeListing>]
               [[--[no-]optimizations] [--[no-]bounds-checking] [--[no-]constant-reduction] [--[no-]strength-reduction] [--[no-]dead-code-elimination] [--[no-]peephole-optimizer] [--[no-]label-optimizer]] <sourceCode>
Compile Ghost BASIC program.
      <sourceCode>           program to compile
      --case-sensitive       allow identifiers to be case sensitive (A is different from a)
                               Default: false
      --debug                use the debugging interpreter
      --fix-control-chars    replace '<CONTROL-?>' with the actual control character
  -h, --help                 Show this help message and exit.
      --heap                 allocate memory on heap
      -il, --intermediate-code-listing=<intermediateCodeListing>
                             create intermediate code listing file
      --integer              integer basic program
      --lomem, --heap-start=<heapStartAddress>
                             heap start address (default: 0x8000)
  -o, --output=<outputFile>  output file name
                               Default: a.out
      --quiet                reduce output
      --symbols=<symbolTableFile>
                             dump symbol table to file
      -tl, --target-code-listing=<targetCodeListing>
                             create listing file
      --trace                enable stack traces
  -V, --version              Print version information and exit.
Optimizations:
      --[no-]bounds-checking perform bounds checking on arrays (enabled: true)
      --[no-]constant-reduction
                             constant reduction (enabled: true)
      --[no-]dead-code-elimination
                             enable dead code elimination (enabled: true)
      --[no-]label-optimizer enable label optimizer (enabled: true)
      --[no-]optimizations   disable all optimizations (enabled: false)
      --[no-]peephole-optimizer
                             enable peephole optimizer (enabled: true)
      --[no-]strength-reduction
                             enable strength reduction (enabled: true)
```

Note that bounds checking is enabled, so to optimize there, `--no-bounds-checking` should be used.

## Project structure

Project is the usual Gradle structure. Things of note:
* `cc65` is required and is embedded in the Gradle build. Sorry for the confusion this brings. I did not want to "forget" to 
  build the assembly portion of the project since Java won't care until you actually compile something.
* The output is in AppleSingle format. Most Apple II tooling will allow that for imports.
* All code assets are buried in `src/main/`:
  * `antlr/a2geek/ghost/antlr/generated/` has the Basic grammar; the long path is to get the Java package correct.
  * `asm/` has the interpreter itself.
  * `basic/` is some sample code used for development. No test harness (yet).
  * `java/` is the compiler.
  * `resources/` is where the resulting assembly output is placed to be packaged in the executable JAR file.

## More project information

* [BASIC language](BASIC.md) and its support [library](LIBRARY.md)
* [Integer BASIC language](INTEGER.md)
* [Compiler](COMPILER.md)
* [Interpreter opcodes](OPCODES.md)
