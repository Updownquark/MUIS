lexer grammar MSS;

ID  :	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'-')*
    ;

FLOAT
    :   ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
    |   '.' ('0'..'9')+ EXPONENT?
    |   ('0'..'9')+ EXPONENT
    ;

COMMENT
    :   '//' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}
    |   '/*' ( options {greedy=false;} : . )* '*/' {$channel=HIDDEN;}
    ;

WSNL 	:	(' ' | '\t') {$channel=HIDDEN;};

WS  :   ( ' '
        | '\t'
        | '\r'
        | '\n'
        ) {$channel=HIDDEN;}
    ;

fragment
EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

ANIMATION : '>>' WSNL ID WS ('->' WSNL FLOAT WSNL '@' WSNL FLOAT)+ WSNL '|'?;

VALUE : (('a'..'z'|'A'..'Z'|'0'..'9'|'_'|':'|'-'|'.'|'|'|'{'|'}'|'['|']'|'!'..'*'|'+')*);
ASSIGNMENT : ID (':'ID)?'.'ID WS '=' WS VALUE ';'?;
DOMAIN_ASSIGNMENT : ID (':'ID) WS '=' WS '{' (WS ID WS '=' WS VALUE)* WS '}';


TYPE_SET : '[' WSNL ID (':'ID)? (WSNL ',' WSNL ID (':'ID)?)* WSNL ']';
GROUP_SET : '(' WSNL ID (WSNL ',' WSNL ID)* WSNL ']';
STATE_SET : ('.' WSNL ID (WSNL '_' WSNL ID)*)+;
SECTION : (TYPE_SET | GROUP_SET | STATE_SET)* WS '{' (WS ASSIGNMENT | DOMAIN_ASSIGNMENT | SECTION)* WS '}';

FILE : WS (ANIMATION WS)* (ASSIGNMENT | DOMAIN_ASSIGNMENT | SECTION WS)*;
