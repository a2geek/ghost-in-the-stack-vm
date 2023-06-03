grammar Basic;

options { caseInsensitive=true; }

@header {
package a2geek.ghost.antlr.generated;
}

program
    : directives*
      declarations*
      statements
      EOF
    ;

directives
    : ('use' | 'uses') STR ( ',' STR )* EOL+                            # useDirective
    | EOL                                                               # emptyDirective
    ;

declarations
    : 'const' constantDecl ( ',' constantDecl )*                        # constant
    | 'sub' id=ID p=paramDecl? EOL+
        (s=statements)?
      'end' 'sub'                                                       # subDecl
    | 'function' id=ID p=paramDecl? ('as' datatype)? EOL+
        (s=statements)?
      'end' 'function'                                                  # funcDecl
    | EOL                                                               # emptyDecl
    ;

statements
    : ( statement ( ':' statement )* EOL* | EOL )+
    ;

statement
    : 'dim' idDecl (',' idDecl)*                                        # dimStmt
    | id=extendedID '=' a=expr                                          # assignment
    | id=ID ':'                                                         # label
    | 'if' a=expr 'then' t=statement                                    # ifShortStatement
    | 'if' a=expr 'then' EOL+
        t=statements 
      ('else' EOL+
        f=statements)?
      'end' 'if' EOL+                                                   # ifStatement
    | 'gr'                                                              # grStmt
    | 'do' op=('while' | 'until') a=expr (EOL|':')+
        s=statements? (EOL|':')*  // EOL is included in statements itself
      'loop'                                                            # doLoop1
    | 'do' (EOL|':')+
        s=statements? (EOL|':')*  // EOL is included in statements itself
      'loop' op=('while' | 'until') a=expr                              # doLoop2
    | 'for' id=ID '=' a=expr 'to' b=expr ('step' c=expr)? (EOL|':')+
        s=statements? (EOL|':')*  // EOL is included in statements itself
      'next' id2=ID                                                     # forLoop
    | 'while' a=expr (EOL|':')+
        s=statements? (EOL|':')*  // EOL is included in statements itself
      'end' 'while'                                                     # whileLoop
    | 'repeat' (EOL|':')+
        s=statements? (EOL|':')*  // EOL is included in statements itself
      'until' a=expr                                                    # repeatLoop
    | 'exit' n=('do' | 'for' | 'repeat' | 'while')                      # exitStmt
    | 'color=' a=expr                                                   # colorStmt
    | 'plot' a=expr ',' b=expr                                          # plotStmt
    | 'vlin' a=expr ',' b=expr 'at' x=expr                              # vlinStmt
    | 'hlin' a=expr ',' b=expr 'at' y=expr                              # hlinStmt
    | 'end'                                                             # endStmt
    | 'home'                                                            # homeStmt
    | 'print' (expr | sexpr | ',' | ';')*                               # printStmt
    | op=( 'poke' | 'pokew' ) a=expr ',' b=expr                         # pokeStmt
    | 'call' a=expr                                                     # callStmt
    | op=( 'goto' | 'gosub' ) l=ID                                      # gotoGosubStmt
    | 'return' e=expr?                                                  # returnStmt
    | 'text'                                                            # textStmt
    | 'vtab' a=expr                                                     # vtabStmt
    | 'htab' a=expr                                                     # htabStmt
    | id=ID p=parameters?                                               # callSub
    ;

constantDecl
    : id=ID '=' e=expr
    ;

extendedID
    : ID '(' ( anyExpr ( ',' anyExpr )* )? ')'                           # arrayOrFunctionRef
    | ID ('.' ID)*                                                      # variableOrFunctionRef
    ;

paramDecl
    : '(' ( idDecl ( ',' idDecl )* )? ')'
    ;

idDecl
    : ID ( '(' expr ( ',' expr )* ')' )? ( 'as' datatype )?
    ;

datatype
    : 'integer'
    | 'boolean'
    | 'string'
    ;

parameters
    : '(' ( anyExpr ( ',' anyExpr )* )? ')'
    ;

// Needed to make parameter parsing possible - the different subexpressions can cause problems
anyExpr
    : expr
    | sexpr
    ;

expr
    : a=expr op='^' b=expr                                              # binaryExpr
    | a=expr op=( '*' | '/' | 'mod' ) b=expr                            # binaryExpr
    | a=expr op=( '+' | '-' ) b=expr                                    # binaryExpr
    | a=expr op=( '<' | '<=' | '>' | '>=' | '=' | '<>' ) b=expr         # binaryExpr
    | a=expr op=( '<<' | '>>' ) b=expr                                  # binaryExpr
    | a=expr op=( 'or' | 'and' | 'xor' ) b=expr                         # binaryExpr
    | op=('-' | 'not') a=expr                                           # unaryExpr
    | id=extendedID                                                     # extendedIdExpr
    | a=INT                                                             # intConstant
    | b=('true' | 'false')                                              # boolConstant
    | '(' a=expr ')'                                                    # parenExpr
    ;

sexpr
    : s=STR                                                             # stringConstant
    ;

ID : [a-z] ([_a-z0-9])* ;
INT : [0-9]+ | '0x' [0-9a-f]+ | '0b' [01]+ ;
STR : '"' ~["]* '"' ;

EOL : [\r]? [\n] ;
WS : [ \t]+ -> skip ;

COMMENT : '\'' ~[\n]* -> skip ;
LINE_CONTINUATION: ' ' '_' '\r'? '\n' -> skip;
