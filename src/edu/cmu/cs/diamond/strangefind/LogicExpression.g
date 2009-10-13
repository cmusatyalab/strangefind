grammar LogicExpression;

options {
    output = AST;
    backtrack = true;
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
package edu.cmu.cs.diamond.strangefind;
}

@lexer::header {
package edu.cmu.cs.diamond.strangefind;
}

expr	:	term ;

term	:(func|literal) ;

literal	:	DOLLAR NUMBER -> NUMBER;

func	:	(and | or | not);

andsecondhalf
	:	term RPAREN -> term
	|       term ',' andsecondhalf -> ^(OP_AND term andsecondhalf);

and	:	AND LPAREN term ',' andsecondhalf -> ^(OP_AND term andsecondhalf);

orsecondhalf
	:	term RPAREN -> term
	|       term ',' orsecondhalf -> ^(OP_OR term orsecondhalf);

or	:	OR LPAREN term ',' orsecondhalf -> ^(OP_OR term orsecondhalf);

not	:	NOT LPAREN term RPAREN -> ^(OP_NOT term);



WHITESPACE : ( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ 	{ $channel = HIDDEN; } ;

AND 	:	(('A'|'a') ('N'|'n') ('D'|'d')) ;

OR	:	('O'|'o') ('R'|'r') ;

NOT	:	('N'|'n') ('O'|'o') ('T'|'t');

NUMBER	:	(DIGIT)+;

fragment
DIGIT	:	'0'..'9' ;
