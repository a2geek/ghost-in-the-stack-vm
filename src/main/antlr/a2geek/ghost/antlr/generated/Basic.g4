grammar Basic;

options { caseInsensitive=true; }

@header {
package a2geek.ghost.antlr.generated;
}

program
    : directives*
      ( module
      | declarations*
        statements
      )
      EOF
    ;

directives
    : ('use' | 'uses') STR ( ',' STR )* EOL+                            # useDirective
    | EOL                                                               # emptyDirective
    ;

module
    : 'module' ID EOL+
      ( declarations | statements )*
      'end' 'module' EOL+
    ;

declarations
    : 'const' constantDecl ( ',' constantDecl )*          # constant
    | modifiers* 'sub' id=ID p=paramDecl? EOL+
        (s=statements)?
      'end' 'sub'                                                       # subDecl
    | modifiers* 'function' id=ID p=paramDecl? ('as' datatype)? EOL+
        (s=statements)?
      'end' 'function'                                                  # funcDecl
    | EOL                                                               # emptyDecl
    ;
modifiers
    : 'export'
    | 'inline'
    ;

statements
    : ( statement ( ':' statement )* EOL* | EOL )+
    ;

statement
    : 'dim' idDecl (',' idDecl)*                                        # dimStmt
    | id=extendedID '=' a=expr                                          # assignment
    | id=ID ':'                                                         # label
    | 'if' a=expr 'then' t=statement                                    # ifShortStatement
    | 'if' ifFragment
      ('elseif' ifFragment)*
      ('else' EOL+
        f=statements)?
      'end' 'if' EOL+                                                   # ifStatement
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
    | 'end'                                                             # endStmt
    | 'print' (expr | sexpr | ',' | ';')*                               # printStmt
    | op=( 'poke' | 'pokew' ) a=expr ',' b=expr                         # pokeStmt
    | 'call' a=expr                                                     # callStmt
    | op=( 'goto' | 'gosub' ) l=ID                                      # gotoGosubStmt
    | 'on' a=expr op=( 'goto' | 'gosub' ) ID (',' ID)*                  # onGotoGosubStmt
    | 'return' e=expr?                                                  # returnStmt
    | id=ID p=parameters?                                               # callSub
    ;
ifFragment
    : expr 'then' EOL+
        statements
    ;

constantDecl
    : id=ID '=' e=expr
    ;

extendedID
    : ID '(' ( anyExpr ( ',' anyExpr )* )? ')'                          # arrayOrFunctionRef
    | ID ('.' ID)*                                                      # variableOrFunctionRef
    ;

paramDecl
    : '(' ( idDecl ( ',' idDecl )* )? ')'
    ;

idDecl
    : idModifier? ID ( '(' ( expr ( ',' expr )* )? ')' )? ( 'as' datatype )? ( '=' idDeclDefault )?
    ;

idModifier
    : 'static'
    ;

idDeclDefault
    : anyExpr
    | '{' anyExpr ( ',' anyExpr )* '}'
    ;

datatype
    : 'integer'
    | 'boolean'
    | 'string'
    | 'address'
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
