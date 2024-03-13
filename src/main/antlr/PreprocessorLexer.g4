lexer grammar PreprocessorLexer;

options { caseInsensitive=true; }

START : {getCharPositionInLine() == 0}? '#' -> pushMode(DIRECTIVE) ;
CODE : ~('#')+ ;

mode DIRECTIVE;

IF     : 'if' ;
ELSEIF : 'elseif' ;
ELSE   : 'else' ;
ENDIF  : 'endif' ;
DEFINE : 'define' ;
TRUE   : 'true' ;
FALSE  : 'false' ;

EQ     : '=' ;
NE     : '<>' | '!=' ;
LPAREN : '(' ;
RPAREN : ')' ;

EOL : [\r]? [\n]+ -> popMode ;
WS  : [ \t\r\n] -> skip ;
ID  : LETTERS ( LETTERS | DIGITS )* ;

STRING  : '"' ~["]* '"' ;
LETTERS : [a-z] ;
DIGITS  : [0-9] ;