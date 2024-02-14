grammar Basic;

options { caseInsensitive=true; }

@header {
package a2geek.ghost.antlr.generated;
}

program
    : directives*
      ( module
      | ( declarations | statements )*
        statements
      )
      EOF
    ;

directives
    : ('use' | 'uses') STR ( ',' STR )* EOL+                            # useDirective
    | 'option' optionTypes EOL+                                         # optionDirective
    | EOL                                                               # emptyDirective
    ;
optionTypes // "options" is an ANTLR grammar keyword
    : op='heap' ( LOMEM '=' lomem=INT )?
    | op='strict'
    ;

module
    : 'module' identifier EOL+
      ( declarations | statements )*
      'end' 'module' EOL+
    ;

declarations
    : 'const' constantDecl ( ',' constantDecl )*                        # constant
    | modifiers* 'sub' id=identifier p=paramDecl? EOL+
        (s=statements)?
      'end' 'sub'                                                       # subDecl
    | modifiers* 'function' id=identifier p=paramDecl? ('as' datatype)? EOL+
        (s=statements)?
      'end' 'function'                                                  # funcDecl
    | EOL                                                               # emptyDecl
    ;
modifiers
    : 'export'
    | 'inline'
    | 'volatile'
    ;

statements
    : (multipleStatements EOL* | EOL )+
    ;
multipleStatements
    : statement ( ':' statement )*
    ;

statement
    : 'dim' idDecl (',' idDecl)*                                            # dimStmt
    | id=expressionID '=' a=expr                                            # assignment
    | id=identifier ':' EOL                                                 # label
    | 'if' a=expr 'then' t=multipleStatements EOL                           # ifShortStatement
    | 'if' ifFragment
      ('elseif' ifFragment)*
      ('else' EOL+
        f=statements)?
      'end' 'if' EOL+                                                       # ifStatement
    | 'do' op=('while' | 'until') a=expr (EOL|':')+
        s=statements? (EOL|':')*  // EOL is included in statements itself
      'loop'                                                                # doLoop1
    | 'do' (EOL|':')+
        s=statements? (EOL|':')*  // EOL is included in statements itself
      'loop' op=('while' | 'until') a=expr                                  # doLoop2
    | 'for' id=identifier '=' a=expr 'to' b=expr ('step' c=expr)? (EOL|':')+
        s=statements? (EOL|':')*  // EOL is included in statements itself
      'next' identifier?                                                    # forLoop
    | 'while' a=expr (EOL|':')+
        s=statements? (EOL|':')*  // EOL is included in statements itself
      'end' 'while'                                                         # whileLoop
    | 'repeat' (EOL|':')+
        s=statements? (EOL|':')*  // EOL is included in statements itself
      'until' a=expr                                                        # repeatLoop
    | 'exit' n=('do' | 'for' | 'repeat' | 'while')                          # exitStmt
    | 'end'                                                                 # endStmt
    | 'print' (expr | ',' | ';')*                                           # printStmt
    | op=( 'poke' | 'pokew' ) a=expr ',' b=expr                             # pokeStmt
    | 'call' a=expr                                                         # callStmt
    | op=( 'goto' | 'gosub' ) l=identifier                                  # gotoGosubStmt
    | 'on' a=expr op=( 'goto' | 'gosub' ) identifier (',' identifier)*      # onGotoGosubStmt
    | 'on' 'error' ( op='goto' l=identifier
                   | op='disable'
                   | op='resume' 'next'? )                                  # onErrorStmt
    | 'raise' 'error' a=expr ',' m=expr                                     # raiseErrorStmt
    | 'return' e=expr?                                                      # returnStmt
    | 'select' 'case'? a=expr EOL+
      ( selectCaseFragment )+
      ( selectElseFragment )?
      'end' 'select'                                                        # selectStmt
    | id=extendedID p=parameters?                                           # callSub
    ;
ifFragment
    : expr 'then' EOL+
        statements
    ;
selectCaseFragment
    : 'case' selectCaseExpr ( ',' selectCaseExpr )* EOL+
        s=statements
    ;
selectElseFragment
    : 'case' 'else' EOL+
        s=statements
    ;
selectCaseExpr
    : a=expr
    | a=expr 'to' b=expr
    | 'is'? op=( '<' | '<=' | '>' | '>=' | '=' | '<>' ) a=expr
    ;

constantDecl
    : id=identifier '=' e=expr
    ;

extendedID
    : identifier ('.' identifier)*
    ;

expressionID
    : extendedID '(' ( expr ( ',' expr )* )? ')'                      # arrayOrFunctionRef
    | extendedID                                                      # variableOrFunctionRef
    ;

paramDecl
    : '(' ( paramIdDecl ( ',' paramIdDecl )* )? ')'
    ;
paramIdDecl
    : identifier ( '(' ( ',' )* ')' )? ( 'as' datatype )?
    ;

idDecl
    : idModifier? identifier ( '(' ( expr ( ',' expr )* )? ')' )? ( 'as' datatype )? ( '=' idDeclDefault )?
    ;

idModifier
    : 'static'
    ;

idDeclDefault
    : expr
    | '{' expr ( ',' expr )* '}'
    ;

datatype
    : 'integer'
    | 'boolean'
    | 'string'
    | 'address'
    ;

parameters
    : '(' ( expr ( ',' expr )* )? ')'
    ;

expr
    : a=expr op='^' b=expr                                              # binaryExpr
    | a=expr op=( '*' | '/' | 'mod' ) b=expr                            # binaryExpr
    | a=expr op=( '+' | '-' ) b=expr                                    # binaryExpr
    | a=expr op=( '<' | '<=' | '>' | '>=' | '=' | '<>' ) b=expr         # binaryExpr
    | a=expr op=( '<<' | '>>' ) b=expr                                  # binaryExpr
    | a=expr op=( 'or' | 'and' | 'xor' ) b=expr                         # binaryExpr
    | op=('-' | 'not') a=expr                                           # unaryExpr
    | id=expressionID                                                   # expressionIDExpr
    | a=INT                                                             # intConstant
    | s=STR                                                             # stringConstant
    | b=('true' | 'false')                                              # boolConstant
    | '(' a=expr ')'                                                    # parenExpr
    ;

identifier
    : ID
    | LOMEM
    ;

// These keywords can also be used as an identifier
LOMEM : 'lomem' ;

ID : [a-z] [_a-z0-9]* [$%]? ;
INT : [0-9]+ | '0x' [0-9a-f]+ | '0b' [01]+ ;
STR : '"' ~["]* '"' ;

EOL : [\r]? [\n] ;
WS : [ \t]+ -> skip ;

COMMENT : '\'' ~[\n]* -> skip ;
LINE_CONTINUATION: ' ' '_' '\r'? '\n' -> skip;
