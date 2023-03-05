grammar Basic;

options { caseInsensitive=true; }

@header {
package a2geek.ghost.antlr.generated;
}

program
    : statements EOF
    ;

statements
    : ( statement ( ':' statement )* EOL* )+
    ;

statement
    : id=ID '=' a=expr                                                  # assignment
    | 'if' a=expr 'then' EOL+
        t=statements 
      'else' EOL+
        f=statements 
      'end' 'if' EOL+                                                   # ifStatement
    | 'gr'                                                              # grStmt
    | 'for' id=ID '=' a=expr 'to' b=expr EOL+
        s=statements EOL*  // EOL is included in statements itself
      'next' id2=ID                                                     # forLoop
    | 'color=' a=expr                                                   # colorStmt
    | 'plot' a=expr ',' b=expr                                          # plotStmt
    | 'end'                                                             # endStmt
    ;

expr
    : a=expr op=( '*' | '/' | 'mod' ) b=expr                  # mulDivModExpr
    | a=expr op=( '+' | '-' ) b=expr                          # addSubExpr
    | a=expr op=( '<' | '>' ) b=expr                          # compExpr
    | a=ID                                                    # identifier
    | a=INT                                                   # intConstant
    ;

ID : [a-z] ([a-z0-9])* ;
INT : [0-9]+ ;

EOL : [\r]? [\n] ;
WS : [ \t]+ -> skip ;

COMMENT : '\'' ~[\n]* [\n] -> skip ;
