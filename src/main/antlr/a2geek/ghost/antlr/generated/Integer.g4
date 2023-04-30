grammar Integer;

options { caseInsensitive=true; }

@header {
package a2geek.ghost.antlr.generated;
}

program
    : line+ EOF
    ;

line
    : INTEGER statements EOL*                                               # programLine
    | EOL                                                                   # nullLine
    ;

statement
    : 'call' e=iexpr                                                        # callStatement
    | 'clr'                                                                 # clrStatement
    | 'color=' e=expr                                                       # colorStatement
    | 'del' first=iexpr ( ',' last=iexpr )?                                 # delStatement
    | 'dim' dimvar ( ',' dimvar )*                                          # dimStatement
    | 'dsp' var                                                             # dspStatement
    | 'end'                                                                 # endStatement
    | 'for' ivar '=' first=iexpr 'to' last=iexpr ( 'step' step=iexpr )?     # forStatement
    | g=('gosub'|'goto') l=INTEGER                                          # gosubGotoStatement
    | 'gr'                                                                  # grStatement
    | 'himem:' addr=iexpr                                                   # himemStatement
    | 'hlin' x0=iexpr ',' x1=iexpr 'at' y=iexpr                             # hlinStatement
    | 'if' e=iexpr 'then' l=INTEGER                                         # ifLineStatement
    | 'if' e=iexpr 'then' s=statement                                       # ifStatement
    | 'in#' slot=iexpr                                                      # inNumStatement
    | 'input' ( prompt=STRING ',' )? var ( ',' var )*                       # inputStatement
    | ('let')? ivar '=' iexpr                                               # integerAssignment
    | ('let')? sref '=' sexpr                                               # stringAssignment
    | 'list' first=iexpr ( ',' last=iexpr )?                                # listStatement
    | 'lomem:' addr=iexpr                                                   # lomemStatement
    | 'notrace'                                                             # notraceStatement
    | 'next' ivar ( ',' ivar )*                                             # nextStatement
    | 'plot' x=iexpr ',' y=iexpr                                            # plotStatement
    | 'poke' addr=iexpr ',' e=iexpr                                         # pokeStatement
    | 'pop'                                                                 # popStatement
    | 'pr#' slot=iexpr                                                      # prNumStatement
    | 'print' (expr | ';' | ',')*                                           # printStatement
    | REMARK                                                                # remarkStatement
    | 'return'                                                              # returnStatement
    | 'run' ( a=iexpr )?                                                    # runStatement
    | 'tab' x=iexpr                                                         # tabStatement
    | 'trace'                                                               # traceStatement
    | 'text'                                                                # textStatement
    | 'vlin' y0=iexpr ',' y1=iexpr 'at' x=iexpr                             # vlinStatement
    | 'vtab' y=expr                                                         # vtabStatement
    ;

statements
    : statement ( ':' statement )* ':'*?
    ;

dimvar
    : n=iname '(' e=iexpr ')'        # intDimVar
    | n=sname '(' e=iexpr ')'        # strDimVar
    ;

expr
    : iexpr
    | sexpr
    ;

iexpr
    : left=iexpr op='^' right=iexpr                                 # binaryIntExpr
    | op=( '+' | '-' ) e=iexpr                                      # unaryIntExpr
    | left=iexpr op=( '*' | '/' | 'mod' ) right=iexpr               # binaryIntExpr
    | left=iexpr op=( '+' | '-' ) right=iexpr                       # binaryIntExpr
    | left=iexpr op=('<'|'>'|'<='|'>='|'='|'<>'|'#') right=iexpr    # binaryIntExpr
    | left=sexpr op=('<'|'>'|'<='|'>='|'='|'<>'|'#') right=sexpr    # binaryStrExpr
    | 'not' e=iexpr                                                 # unaryIntExpr
    | left=iexpr op=( 'and' | 'or' ) right=iexpr                    # binaryIntExpr
    | func=ifcall                                                   # funcExpr
    | ref=ivar                                                      # intVarExpr
    | value=INTEGER                                                 # intConstExpr
    | '(' e=iexpr ')'                                               # parenExpr
    ;

sexpr
    : value=STRING                                          # strConstExpr
    | ref=svar                                              # strVarExpr
    ;

ifcall
    : f=( 'abs' | 'pdl' | 'peek' | 'rnd' | 'sgn' ) '(' e=iexpr ')'  # intArgFunc
    | f=( 'asc' | 'len' ) '(' s=sexpr ')'                           # strArgFunc
    | 'scrn' '(' x=iexpr ',' y=iexpr ')'                            # scrnFunc
    ;

sref
    : n=sname ( '(' start=iexpr ')' )?            # strRef
    ;

var
    : ivar
    | svar
    ;

iname
    : NAME
    ;

ivar
    : n=iname '(' e=iexpr ')'                     # intAryVar
    | n=iname                                     # intVar
    ;

sname
    : NAME '$'
    ;

svar
    : n=sname ( '(' start=iexpr ( ',' length=iexpr )? ')' )?    # strVar
    ;

REMARK : 'rem' ( LINE_CONTINUATION | ~[\r\n] )* ;
NAME : [a-z] [a-z0-9]* ;
INTEGER : [0-9]+ | '0x' [0-9a-f]+ ;    // Extension to make rewriting assembly easier
STRING : '"' ~["]* '"' ;
EOL : [\r]? [\n] ;

WS : [ \t]+ -> skip ;
LINE_CONTINUATION: ' ' '_' '\r'? '\n' -> skip;
