parser grammar PreprocessorGrammar;

options { tokenVocab = PreprocessorLexer; }

source : ( CODE | directives )* EOF ;

directives
    : START IF expr EOL             # ifDirective
    | START ELSEIF expr EOL         # elseIfDirective
    | START ELSE EOL                # elseDirective
    | START ENDIF EOL               # endIfDirective
    | START DEFINE EOL              # defineDirective
    ;

expr
    : expr ( EQ | NE ) expr         # comparisonExpr
    | ID ( LPAREN expr RPAREN )?    # idOrFnExpr
    | DIGITS+                       # integerExpr
    | STRING                        # stringExpr
    | (TRUE | FALSE )               # booleanExpr
    ;
