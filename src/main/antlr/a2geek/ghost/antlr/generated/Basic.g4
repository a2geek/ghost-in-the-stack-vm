grammar Basic;

options { caseInsensitive=true; }

@header {
package a2geek.ghost.antlr.generated;
}

program
    : statement+
    ;

statement
    : id=ID '=' a=expr                                              # assignment
    | 'gr'                                                          # grStmt
    | 'for' id=ID '=' a=expr 'to' b=expr s=statement+ 'next' id2=ID # forLoop
    | 'color=' a=expr                                               # colorStmt
    | 'plot' a=expr ',' b=expr                                      # plotStmt
    | 'end'                                                         # endStmt
    ;

expr
    : a=expr op=( '+' | '-' ) b=expr                          # addSubExpr
    | a=expr op=( '*' | '/' | 'mod' ) b=expr                  # mulDivModExpr
    | a=ID                                                    # identifier
    | a=INT                                                   # intConstant
    ;

ID : [a-z] ([a-z0-9])* ;
INT : [0-9]+ ;

WS : [ \t\n]+ -> skip ;
