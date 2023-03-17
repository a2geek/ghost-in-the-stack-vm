grammar Basic;

options { caseInsensitive=true; }

@header {
package a2geek.ghost.antlr.generated;
}

program
    : statements EOF
    ;

statements
    : ( statement ( ':' statement )* EOL* | EOL )+
    ;

statement
    : id=ID '=' a=expr                                                  # assignment
    | id=ID ':'                                                         # label
    | 'if' a=expr 'then' t=statement                                    # ifShortStatement
    | 'if' a=expr 'then' EOL+
        t=statements 
      ('else' EOL+
        f=statements)?
      'end' 'if' EOL+                                                   # ifStatement
    | 'gr'                                                              # grStmt
    | 'for' id=ID '=' a=expr 'to' b=expr ('step' c=expr)? (EOL|':')+
        s=statements? (EOL|':')*  // EOL is included in statements itself
      'next' id2=ID                                                     # forLoop
    | 'color=' a=expr                                                   # colorStmt
    | 'plot' a=expr ',' b=expr                                          # plotStmt
    | 'vlin' a=expr ',' b=expr 'at' x=expr                              # vlinStmt
    | 'hlin' a=expr ',' b=expr 'at' y=expr                              # hlinStmt
    | 'end'                                                             # endStmt
    | 'home'                                                            # homeStmt
    | 'print' (expr | ',' | ';')*                                       # printStmt
    | 'poke' a=expr ',' b=expr                                          # pokeStmt
    | 'call' a=expr                                                     # callStmt
    | op=( 'goto' | 'gosub' ) l=ID                                      # gotoGosubStmt
    | 'return'                                                          # returnStmt
    | 'text'                                                            # textStmt
    | 'vtab' a=expr                                                     # vtabStmt
    | 'htab' a=expr                                                     # htabStmt
    ;

expr
    : a=expr op=( '*' | '/' | 'mod' ) b=expr                            # mulDivModExpr
    | a=expr op=( '+' | '-' ) b=expr                                    # addSubExpr
    | a=expr op=( '<' | '<=' | '>' | '>=' | '=' | '<>' ) b=expr         # compExpr
    | a=expr op=( 'or' | 'and' ) b=expr                                 # logicalExpr
    | '-' a=expr                                                        # negateExpr
    | '(' a=expr ')'                                                    # parenExpr
    | 'peek' '(' a=expr ')'                                             # peekExpr
    | 'scrn' '(' a=expr ',' b=expr ')'                                  # scrnExpr
    | a=ID                                                              # identifier
    | a=INT                                                             # intConstant
    ;

ID : [a-z] ([a-z0-9])* ;
INT : [0-9]+ ;

EOL : [\r]? [\n] ;
WS : [ \t]+ -> skip ;

COMMENT : '\'' ~[\n]* [\n] -> skip ;
