grammar LogicExpression;

options {
    output = AST;
}

tokens {
    LPAREN = '(' ;
    RPAREN = ')' ;
    DOLLAR = '$' ;
    OP_AND ;
    OP_OR  ;
    OP_NOT ;
}

@header {
package edu.cmu.cs.diamond.snapfind2;
}

@lexer::header {
package edu.cmu.cs.diamond.snapfind2;
}

expr	:	term ;

term	:(func|literal) ;

literal	:	DOLLAR NUMBER -> NUMBER;

func	:	(and | or | not);

and	:	AND LPAREN term ',' term RPAREN -> ^(OP_AND term term);

or	:	OR LPAREN term ',' term RPAREN -> ^(OP_OR term term);

not	:	NOT LPAREN term RPAREN -> ^(OP_NOT term);





WHITESPACE : ( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ 	{ $channel = HIDDEN; } ;

AND 	:	(('A'|'a') ('N'|'n') ('D'|'d')) ;

OR	:	('O'|'o') ('R'|'r') ;

NOT	:	('N'|'n') ('O'|'o') ('T'|'t');

NUMBER	:	(DIGIT)+;

fragment
DIGIT	:	'0'..'9' ;
