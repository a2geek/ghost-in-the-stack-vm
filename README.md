# Ghost in the Stack

Experimental bytecode interpreter that exists only in the 6502 stack.

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

## More project information

* [BASIC language](BASIC.md)
* [Integer BASIC language](INTEGER.md)
* [Compiler](COMPILER.md)
* [Interpreter opcodes](OPCODES.md)

## Project structure

Project is the usual Gradle structure. Things of note:
* `cc65` is required and is embedded in the Gradle build. Sorry for the confusion this brings. I did not want to "forget" to 
  build the assembly portion of the project since Java won't care until you actually compile something.
* The output is in AppleSingle format. Most Apple II tooling will allow that for imports. (AppleCommander does! :smile:)
* All code assets are buried in `src/main/`:
  * `antlr/a2geek/ghost/antlr/generated/` has the Basic grammar; the long path is to get the Java package correct.
  * `asm/` has the interpreter itself.
  * `basic/` is some sample code used for development. No test harness (yet).
  * `java/` is the compiler.
  * `resources/` is where the resulting assembly output is placed to be packaged in the executable JAR file.

