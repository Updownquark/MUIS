package org.muis.core.parser;
// $ANTLR 3.4 C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g 2012-09-22 14:13:49

import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked"})
public class MSS extends Lexer {
    public static final int EOF=-1;
    public static final int ANIMATION=4;
    public static final int ASSIGNMENT=5;
    public static final int COMMENT=6;
    public static final int DOMAIN_ASSIGNMENT=7;
    public static final int EXPONENT=8;
    public static final int FILE=9;
    public static final int FLOAT=10;
    public static final int GROUP_SET=11;
    public static final int ID=12;
    public static final int SECTION=13;
    public static final int STATE_SET=14;
    public static final int TYPE_SET=15;
    public static final int VALUE=16;
    public static final int WS=17;
    public static final int WSNL=18;

    // delegates
    // delegators
    public Lexer[] getDelegates() {
        return new Lexer[] {};
    }

    public MSS() {} 
    public MSS(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public MSS(CharStream input, RecognizerSharedState state) {
        super(input,state);
    }
    public String getGrammarFileName() { return "C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g"; }

    // $ANTLR start "ID"
    public final void mID() throws RecognitionException {
        try {
            int _type = ID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:3:5: ( ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '-' )* )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:3:7: ( 'a' .. 'z' | 'A' .. 'Z' | '_' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '-' )*
            {
            if ( (input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:3:31: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '-' )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0=='-'||(LA1_0 >= '0' && LA1_0 <= '9')||(LA1_0 >= 'A' && LA1_0 <= 'Z')||LA1_0=='_'||(LA1_0 >= 'a' && LA1_0 <= 'z')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:
            	    {
            	    if ( input.LA(1)=='-'||(input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ID"

    // $ANTLR start "FLOAT"
    public final void mFLOAT() throws RecognitionException {
        try {
            int _type = FLOAT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:7:5: ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT )
            int alt8=3;
            alt8 = dfa8.predict(input);
            switch (alt8) {
                case 1 :
                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:7:9: ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )?
                    {
                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:7:9: ( '0' .. '9' )+
                    int cnt2=0;
                    loop2:
                    do {
                        int alt2=2;
                        int LA2_0 = input.LA(1);

                        if ( ((LA2_0 >= '0' && LA2_0 <= '9')) ) {
                            alt2=1;
                        }


                        switch (alt2) {
                    	case 1 :
                    	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt2 >= 1 ) break loop2;
                                EarlyExitException eee =
                                    new EarlyExitException(2, input);
                                throw eee;
                        }
                        cnt2++;
                    } while (true);


                    match('.'); 

                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:7:25: ( '0' .. '9' )*
                    loop3:
                    do {
                        int alt3=2;
                        int LA3_0 = input.LA(1);

                        if ( ((LA3_0 >= '0' && LA3_0 <= '9')) ) {
                            alt3=1;
                        }


                        switch (alt3) {
                    	case 1 :
                    	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop3;
                        }
                    } while (true);


                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:7:37: ( EXPONENT )?
                    int alt4=2;
                    int LA4_0 = input.LA(1);

                    if ( (LA4_0=='E'||LA4_0=='e') ) {
                        alt4=1;
                    }
                    switch (alt4) {
                        case 1 :
                            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:7:37: EXPONENT
                            {
                            mEXPONENT(); 


                            }
                            break;

                    }


                    }
                    break;
                case 2 :
                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:8:9: '.' ( '0' .. '9' )+ ( EXPONENT )?
                    {
                    match('.'); 

                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:8:13: ( '0' .. '9' )+
                    int cnt5=0;
                    loop5:
                    do {
                        int alt5=2;
                        int LA5_0 = input.LA(1);

                        if ( ((LA5_0 >= '0' && LA5_0 <= '9')) ) {
                            alt5=1;
                        }


                        switch (alt5) {
                    	case 1 :
                    	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt5 >= 1 ) break loop5;
                                EarlyExitException eee =
                                    new EarlyExitException(5, input);
                                throw eee;
                        }
                        cnt5++;
                    } while (true);


                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:8:25: ( EXPONENT )?
                    int alt6=2;
                    int LA6_0 = input.LA(1);

                    if ( (LA6_0=='E'||LA6_0=='e') ) {
                        alt6=1;
                    }
                    switch (alt6) {
                        case 1 :
                            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:8:25: EXPONENT
                            {
                            mEXPONENT(); 


                            }
                            break;

                    }


                    }
                    break;
                case 3 :
                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:9:9: ( '0' .. '9' )+ EXPONENT
                    {
                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:9:9: ( '0' .. '9' )+
                    int cnt7=0;
                    loop7:
                    do {
                        int alt7=2;
                        int LA7_0 = input.LA(1);

                        if ( ((LA7_0 >= '0' && LA7_0 <= '9')) ) {
                            alt7=1;
                        }


                        switch (alt7) {
                    	case 1 :
                    	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt7 >= 1 ) break loop7;
                                EarlyExitException eee =
                                    new EarlyExitException(7, input);
                                throw eee;
                        }
                        cnt7++;
                    } while (true);


                    mEXPONENT(); 


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "FLOAT"

    // $ANTLR start "COMMENT"
    public final void mCOMMENT() throws RecognitionException {
        try {
            int _type = COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:13:5: ( '//' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n' | '/*' ( options {greedy=false; } : . )* '*/' )
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0=='/') ) {
                int LA12_1 = input.LA(2);

                if ( (LA12_1=='/') ) {
                    alt12=1;
                }
                else if ( (LA12_1=='*') ) {
                    alt12=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 12, 1, input);

                    throw nvae;

                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 12, 0, input);

                throw nvae;

            }
            switch (alt12) {
                case 1 :
                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:13:9: '//' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n'
                    {
                    match("//"); 



                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:13:14: (~ ( '\\n' | '\\r' ) )*
                    loop9:
                    do {
                        int alt9=2;
                        int LA9_0 = input.LA(1);

                        if ( ((LA9_0 >= '\u0000' && LA9_0 <= '\t')||(LA9_0 >= '\u000B' && LA9_0 <= '\f')||(LA9_0 >= '\u000E' && LA9_0 <= '\uFFFF')) ) {
                            alt9=1;
                        }


                        switch (alt9) {
                    	case 1 :
                    	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:
                    	    {
                    	    if ( (input.LA(1) >= '\u0000' && input.LA(1) <= '\t')||(input.LA(1) >= '\u000B' && input.LA(1) <= '\f')||(input.LA(1) >= '\u000E' && input.LA(1) <= '\uFFFF') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop9;
                        }
                    } while (true);


                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:13:28: ( '\\r' )?
                    int alt10=2;
                    int LA10_0 = input.LA(1);

                    if ( (LA10_0=='\r') ) {
                        alt10=1;
                    }
                    switch (alt10) {
                        case 1 :
                            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:13:28: '\\r'
                            {
                            match('\r'); 

                            }
                            break;

                    }


                    match('\n'); 

                    _channel=HIDDEN;

                    }
                    break;
                case 2 :
                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:14:9: '/*' ( options {greedy=false; } : . )* '*/'
                    {
                    match("/*"); 



                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:14:14: ( options {greedy=false; } : . )*
                    loop11:
                    do {
                        int alt11=2;
                        int LA11_0 = input.LA(1);

                        if ( (LA11_0=='*') ) {
                            int LA11_1 = input.LA(2);

                            if ( (LA11_1=='/') ) {
                                alt11=2;
                            }
                            else if ( ((LA11_1 >= '\u0000' && LA11_1 <= '.')||(LA11_1 >= '0' && LA11_1 <= '\uFFFF')) ) {
                                alt11=1;
                            }


                        }
                        else if ( ((LA11_0 >= '\u0000' && LA11_0 <= ')')||(LA11_0 >= '+' && LA11_0 <= '\uFFFF')) ) {
                            alt11=1;
                        }


                        switch (alt11) {
                    	case 1 :
                    	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:14:42: .
                    	    {
                    	    matchAny(); 

                    	    }
                    	    break;

                    	default :
                    	    break loop11;
                        }
                    } while (true);


                    match("*/"); 



                    _channel=HIDDEN;

                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "COMMENT"

    // $ANTLR start "WSNL"
    public final void mWSNL() throws RecognitionException {
        try {
            int _type = WSNL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:17:7: ( ( ' ' | '\\t' ) )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:17:9: ( ' ' | '\\t' )
            {
            if ( input.LA(1)=='\t'||input.LA(1)==' ' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "WSNL"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:19:5: ( ( ' ' | '\\t' | '\\r' | '\\n' ) )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:19:9: ( ' ' | '\\t' | '\\r' | '\\n' )
            {
            if ( (input.LA(1) >= '\t' && input.LA(1) <= '\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "WS"

    // $ANTLR start "EXPONENT"
    public final void mEXPONENT() throws RecognitionException {
        try {
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:27:10: ( ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+ )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:27:12: ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+
            {
            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:27:22: ( '+' | '-' )?
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( (LA13_0=='+'||LA13_0=='-') ) {
                alt13=1;
            }
            switch (alt13) {
                case 1 :
                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:
                    {
                    if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                        input.consume();
                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;
                    }


                    }
                    break;

            }


            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:27:33: ( '0' .. '9' )+
            int cnt14=0;
            loop14:
            do {
                int alt14=2;
                int LA14_0 = input.LA(1);

                if ( ((LA14_0 >= '0' && LA14_0 <= '9')) ) {
                    alt14=1;
                }


                switch (alt14) {
            	case 1 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:
            	    {
            	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt14 >= 1 ) break loop14;
                        EarlyExitException eee =
                            new EarlyExitException(14, input);
                        throw eee;
                }
                cnt14++;
            } while (true);


            }


        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "EXPONENT"

    // $ANTLR start "ANIMATION"
    public final void mANIMATION() throws RecognitionException {
        try {
            int _type = ANIMATION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:29:11: ( '>>' WSNL ID WS ( '->' WSNL FLOAT WSNL '@' WSNL FLOAT )+ WSNL ( '|' )? )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:29:13: '>>' WSNL ID WS ( '->' WSNL FLOAT WSNL '@' WSNL FLOAT )+ WSNL ( '|' )?
            {
            match(">>"); 



            mWSNL(); 


            mID(); 


            mWS(); 


            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:29:29: ( '->' WSNL FLOAT WSNL '@' WSNL FLOAT )+
            int cnt15=0;
            loop15:
            do {
                int alt15=2;
                int LA15_0 = input.LA(1);

                if ( (LA15_0=='-') ) {
                    alt15=1;
                }


                switch (alt15) {
            	case 1 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:29:30: '->' WSNL FLOAT WSNL '@' WSNL FLOAT
            	    {
            	    match("->"); 



            	    mWSNL(); 


            	    mFLOAT(); 


            	    mWSNL(); 


            	    match('@'); 

            	    mWSNL(); 


            	    mFLOAT(); 


            	    }
            	    break;

            	default :
            	    if ( cnt15 >= 1 ) break loop15;
                        EarlyExitException eee =
                            new EarlyExitException(15, input);
                        throw eee;
                }
                cnt15++;
            } while (true);


            mWSNL(); 


            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:29:73: ( '|' )?
            int alt16=2;
            int LA16_0 = input.LA(1);

            if ( (LA16_0=='|') ) {
                alt16=1;
            }
            switch (alt16) {
                case 1 :
                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:29:73: '|'
                    {
                    match('|'); 

                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ANIMATION"

    // $ANTLR start "VALUE"
    public final void mVALUE() throws RecognitionException {
        try {
            int _type = VALUE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:31:7: ( ( ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | ':' | '-' | '.' | '|' | '{' | '}' | '[' | ']' | '!' .. '*' | '+' )* ) )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:31:9: ( ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | ':' | '-' | '.' | '|' | '{' | '}' | '[' | ']' | '!' .. '*' | '+' )* )
            {
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:31:9: ( ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | ':' | '-' | '.' | '|' | '{' | '}' | '[' | ']' | '!' .. '*' | '+' )* )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:31:10: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | ':' | '-' | '.' | '|' | '{' | '}' | '[' | ']' | '!' .. '*' | '+' )*
            {
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:31:10: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | ':' | '-' | '.' | '|' | '{' | '}' | '[' | ']' | '!' .. '*' | '+' )*
            loop17:
            do {
                int alt17=2;
                int LA17_0 = input.LA(1);

                if ( ((LA17_0 >= '!' && LA17_0 <= '+')||(LA17_0 >= '-' && LA17_0 <= '.')||(LA17_0 >= '0' && LA17_0 <= ':')||(LA17_0 >= 'A' && LA17_0 <= '[')||LA17_0==']'||LA17_0=='_'||(LA17_0 >= 'a' && LA17_0 <= '}')) ) {
                    alt17=1;
                }


                switch (alt17) {
            	case 1 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:
            	    {
            	    if ( (input.LA(1) >= '!' && input.LA(1) <= '+')||(input.LA(1) >= '-' && input.LA(1) <= '.')||(input.LA(1) >= '0' && input.LA(1) <= ':')||(input.LA(1) >= 'A' && input.LA(1) <= '[')||input.LA(1)==']'||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= '}') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    break loop17;
                }
            } while (true);


            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "VALUE"

    // $ANTLR start "ASSIGNMENT"
    public final void mASSIGNMENT() throws RecognitionException {
        try {
            int _type = ASSIGNMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:32:12: ( ID ( ':' ID )? '.' ID WS '=' WS VALUE ( ';' )? )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:32:14: ID ( ':' ID )? '.' ID WS '=' WS VALUE ( ';' )?
            {
            mID(); 


            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:32:17: ( ':' ID )?
            int alt18=2;
            int LA18_0 = input.LA(1);

            if ( (LA18_0==':') ) {
                alt18=1;
            }
            switch (alt18) {
                case 1 :
                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:32:18: ':' ID
                    {
                    match(':'); 

                    mID(); 


                    }
                    break;

            }


            match('.'); 

            mID(); 


            mWS(); 


            match('='); 

            mWS(); 


            mVALUE(); 


            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:32:47: ( ';' )?
            int alt19=2;
            int LA19_0 = input.LA(1);

            if ( (LA19_0==';') ) {
                alt19=1;
            }
            switch (alt19) {
                case 1 :
                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:32:47: ';'
                    {
                    match(';'); 

                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ASSIGNMENT"

    // $ANTLR start "DOMAIN_ASSIGNMENT"
    public final void mDOMAIN_ASSIGNMENT() throws RecognitionException {
        try {
            int _type = DOMAIN_ASSIGNMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:33:19: ( ID ( ':' ID ) WS '=' WS '{' ( WS ID WS '=' WS VALUE )* WS '}' )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:33:21: ID ( ':' ID ) WS '=' WS '{' ( WS ID WS '=' WS VALUE )* WS '}'
            {
            mID(); 


            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:33:24: ( ':' ID )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:33:25: ':' ID
            {
            match(':'); 

            mID(); 


            }


            mWS(); 


            match('='); 

            mWS(); 


            match('{'); 

            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:33:46: ( WS ID WS '=' WS VALUE )*
            loop20:
            do {
                int alt20=2;
                int LA20_0 = input.LA(1);

                if ( ((LA20_0 >= '\t' && LA20_0 <= '\n')||LA20_0=='\r'||LA20_0==' ') ) {
                    int LA20_1 = input.LA(2);

                    if ( ((LA20_1 >= 'A' && LA20_1 <= 'Z')||LA20_1=='_'||(LA20_1 >= 'a' && LA20_1 <= 'z')) ) {
                        alt20=1;
                    }


                }


                switch (alt20) {
            	case 1 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:33:47: WS ID WS '=' WS VALUE
            	    {
            	    mWS(); 


            	    mID(); 


            	    mWS(); 


            	    match('='); 

            	    mWS(); 


            	    mVALUE(); 


            	    }
            	    break;

            	default :
            	    break loop20;
                }
            } while (true);


            mWS(); 


            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DOMAIN_ASSIGNMENT"

    // $ANTLR start "TYPE_SET"
    public final void mTYPE_SET() throws RecognitionException {
        try {
            int _type = TYPE_SET;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:36:10: ( '[' WSNL ID ( ':' ID )? ( WSNL ',' WSNL ID ( ':' ID )? )* WSNL ']' )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:36:12: '[' WSNL ID ( ':' ID )? ( WSNL ',' WSNL ID ( ':' ID )? )* WSNL ']'
            {
            match('['); 

            mWSNL(); 


            mID(); 


            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:36:24: ( ':' ID )?
            int alt21=2;
            int LA21_0 = input.LA(1);

            if ( (LA21_0==':') ) {
                alt21=1;
            }
            switch (alt21) {
                case 1 :
                    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:36:25: ':' ID
                    {
                    match(':'); 

                    mID(); 


                    }
                    break;

            }


            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:36:33: ( WSNL ',' WSNL ID ( ':' ID )? )*
            loop23:
            do {
                int alt23=2;
                int LA23_0 = input.LA(1);

                if ( (LA23_0=='\t'||LA23_0==' ') ) {
                    int LA23_1 = input.LA(2);

                    if ( (LA23_1==',') ) {
                        alt23=1;
                    }


                }


                switch (alt23) {
            	case 1 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:36:34: WSNL ',' WSNL ID ( ':' ID )?
            	    {
            	    mWSNL(); 


            	    match(','); 

            	    mWSNL(); 


            	    mID(); 


            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:36:51: ( ':' ID )?
            	    int alt22=2;
            	    int LA22_0 = input.LA(1);

            	    if ( (LA22_0==':') ) {
            	        alt22=1;
            	    }
            	    switch (alt22) {
            	        case 1 :
            	            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:36:52: ':' ID
            	            {
            	            match(':'); 

            	            mID(); 


            	            }
            	            break;

            	    }


            	    }
            	    break;

            	default :
            	    break loop23;
                }
            } while (true);


            mWSNL(); 


            match(']'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "TYPE_SET"

    // $ANTLR start "GROUP_SET"
    public final void mGROUP_SET() throws RecognitionException {
        try {
            int _type = GROUP_SET;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:37:11: ( '(' WSNL ID ( WSNL ',' WSNL ID )* WSNL ']' )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:37:13: '(' WSNL ID ( WSNL ',' WSNL ID )* WSNL ']'
            {
            match('('); 

            mWSNL(); 


            mID(); 


            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:37:25: ( WSNL ',' WSNL ID )*
            loop24:
            do {
                int alt24=2;
                int LA24_0 = input.LA(1);

                if ( (LA24_0=='\t'||LA24_0==' ') ) {
                    int LA24_1 = input.LA(2);

                    if ( (LA24_1==',') ) {
                        alt24=1;
                    }


                }


                switch (alt24) {
            	case 1 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:37:26: WSNL ',' WSNL ID
            	    {
            	    mWSNL(); 


            	    match(','); 

            	    mWSNL(); 


            	    mID(); 


            	    }
            	    break;

            	default :
            	    break loop24;
                }
            } while (true);


            mWSNL(); 


            match(']'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "GROUP_SET"

    // $ANTLR start "STATE_SET"
    public final void mSTATE_SET() throws RecognitionException {
        try {
            int _type = STATE_SET;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:38:11: ( ( '.' WSNL ID ( WSNL '_' WSNL ID )* )+ )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:38:13: ( '.' WSNL ID ( WSNL '_' WSNL ID )* )+
            {
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:38:13: ( '.' WSNL ID ( WSNL '_' WSNL ID )* )+
            int cnt26=0;
            loop26:
            do {
                int alt26=2;
                int LA26_0 = input.LA(1);

                if ( (LA26_0=='.') ) {
                    alt26=1;
                }


                switch (alt26) {
            	case 1 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:38:14: '.' WSNL ID ( WSNL '_' WSNL ID )*
            	    {
            	    match('.'); 

            	    mWSNL(); 


            	    mID(); 


            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:38:26: ( WSNL '_' WSNL ID )*
            	    loop25:
            	    do {
            	        int alt25=2;
            	        int LA25_0 = input.LA(1);

            	        if ( (LA25_0=='\t'||LA25_0==' ') ) {
            	            alt25=1;
            	        }


            	        switch (alt25) {
            	    	case 1 :
            	    	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:38:27: WSNL '_' WSNL ID
            	    	    {
            	    	    mWSNL(); 


            	    	    match('_'); 

            	    	    mWSNL(); 


            	    	    mID(); 


            	    	    }
            	    	    break;

            	    	default :
            	    	    break loop25;
            	        }
            	    } while (true);


            	    }
            	    break;

            	default :
            	    if ( cnt26 >= 1 ) break loop26;
                        EarlyExitException eee =
                            new EarlyExitException(26, input);
                        throw eee;
                }
                cnt26++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "STATE_SET"

    // $ANTLR start "SECTION"
    public final void mSECTION() throws RecognitionException {
        try {
            int _type = SECTION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:39:9: ( ( TYPE_SET | GROUP_SET | STATE_SET )* WS '{' ( WS ASSIGNMENT | DOMAIN_ASSIGNMENT | SECTION )* WS '}' )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:39:11: ( TYPE_SET | GROUP_SET | STATE_SET )* WS '{' ( WS ASSIGNMENT | DOMAIN_ASSIGNMENT | SECTION )* WS '}'
            {
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:39:11: ( TYPE_SET | GROUP_SET | STATE_SET )*
            loop27:
            do {
                int alt27=4;
                switch ( input.LA(1) ) {
                case '[':
                    {
                    alt27=1;
                    }
                    break;
                case '(':
                    {
                    alt27=2;
                    }
                    break;
                case '.':
                    {
                    alt27=3;
                    }
                    break;

                }

                switch (alt27) {
            	case 1 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:39:12: TYPE_SET
            	    {
            	    mTYPE_SET(); 


            	    }
            	    break;
            	case 2 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:39:23: GROUP_SET
            	    {
            	    mGROUP_SET(); 


            	    }
            	    break;
            	case 3 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:39:35: STATE_SET
            	    {
            	    mSTATE_SET(); 


            	    }
            	    break;

            	default :
            	    break loop27;
                }
            } while (true);


            mWS(); 


            match('{'); 

            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:39:54: ( WS ASSIGNMENT | DOMAIN_ASSIGNMENT | SECTION )*
            loop28:
            do {
                int alt28=4;
                switch ( input.LA(1) ) {
                case '\t':
                case '\n':
                case '\r':
                case ' ':
                    {
                    int LA28_1 = input.LA(2);

                    if ( ((LA28_1 >= 'A' && LA28_1 <= 'Z')||LA28_1=='_'||(LA28_1 >= 'a' && LA28_1 <= 'z')) ) {
                        alt28=1;
                    }
                    else if ( (LA28_1=='{') ) {
                        alt28=3;
                    }


                    }
                    break;
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case 'Y':
                case 'Z':
                case '_':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                    {
                    alt28=2;
                    }
                    break;
                case '(':
                case '.':
                case '[':
                    {
                    alt28=3;
                    }
                    break;

                }

                switch (alt28) {
            	case 1 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:39:55: WS ASSIGNMENT
            	    {
            	    mWS(); 


            	    mASSIGNMENT(); 


            	    }
            	    break;
            	case 2 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:39:71: DOMAIN_ASSIGNMENT
            	    {
            	    mDOMAIN_ASSIGNMENT(); 


            	    }
            	    break;
            	case 3 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:39:91: SECTION
            	    {
            	    mSECTION(); 


            	    }
            	    break;

            	default :
            	    break loop28;
                }
            } while (true);


            mWS(); 


            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "SECTION"

    // $ANTLR start "FILE"
    public final void mFILE() throws RecognitionException {
        try {
            int _type = FILE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:41:6: ( WS ( ANIMATION WS )* ( ASSIGNMENT | DOMAIN_ASSIGNMENT | SECTION WS )* )
            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:41:8: WS ( ANIMATION WS )* ( ASSIGNMENT | DOMAIN_ASSIGNMENT | SECTION WS )*
            {
            mWS(); 


            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:41:11: ( ANIMATION WS )*
            loop29:
            do {
                int alt29=2;
                int LA29_0 = input.LA(1);

                if ( (LA29_0=='>') ) {
                    alt29=1;
                }


                switch (alt29) {
            	case 1 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:41:12: ANIMATION WS
            	    {
            	    mANIMATION(); 


            	    mWS(); 


            	    }
            	    break;

            	default :
            	    break loop29;
                }
            } while (true);


            // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:41:27: ( ASSIGNMENT | DOMAIN_ASSIGNMENT | SECTION WS )*
            loop30:
            do {
                int alt30=4;
                alt30 = dfa30.predict(input);
                switch (alt30) {
            	case 1 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:41:28: ASSIGNMENT
            	    {
            	    mASSIGNMENT(); 


            	    }
            	    break;
            	case 2 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:41:41: DOMAIN_ASSIGNMENT
            	    {
            	    mDOMAIN_ASSIGNMENT(); 


            	    }
            	    break;
            	case 3 :
            	    // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:41:61: SECTION WS
            	    {
            	    mSECTION(); 


            	    mWS(); 


            	    }
            	    break;

            	default :
            	    break loop30;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "FILE"

    public void mTokens() throws RecognitionException {
        // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:8: ( ID | FLOAT | COMMENT | WSNL | WS | ANIMATION | VALUE | ASSIGNMENT | DOMAIN_ASSIGNMENT | TYPE_SET | GROUP_SET | STATE_SET | SECTION | FILE )
        int alt31=14;
        alt31 = dfa31.predict(input);
        switch (alt31) {
            case 1 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:10: ID
                {
                mID(); 


                }
                break;
            case 2 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:13: FLOAT
                {
                mFLOAT(); 


                }
                break;
            case 3 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:19: COMMENT
                {
                mCOMMENT(); 


                }
                break;
            case 4 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:27: WSNL
                {
                mWSNL(); 


                }
                break;
            case 5 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:32: WS
                {
                mWS(); 


                }
                break;
            case 6 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:35: ANIMATION
                {
                mANIMATION(); 


                }
                break;
            case 7 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:45: VALUE
                {
                mVALUE(); 


                }
                break;
            case 8 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:51: ASSIGNMENT
                {
                mASSIGNMENT(); 


                }
                break;
            case 9 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:62: DOMAIN_ASSIGNMENT
                {
                mDOMAIN_ASSIGNMENT(); 


                }
                break;
            case 10 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:80: TYPE_SET
                {
                mTYPE_SET(); 


                }
                break;
            case 11 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:89: GROUP_SET
                {
                mGROUP_SET(); 


                }
                break;
            case 12 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:99: STATE_SET
                {
                mSTATE_SET(); 


                }
                break;
            case 13 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:109: SECTION
                {
                mSECTION(); 


                }
                break;
            case 14 :
                // C:\\Users\\Andrew\\workspace\\MUIS.git\\projects\\MUIS\\src\\main\\antlr\\MSS.g:1:117: FILE
                {
                mFILE(); 


                }
                break;

        }

    }


    protected DFA8 dfa8 = new DFA8(this);
    protected DFA30 dfa30 = new DFA30(this);
    protected DFA31 dfa31 = new DFA31(this);
    static final String DFA8_eotS =
        "\5\uffff";
    static final String DFA8_eofS =
        "\5\uffff";
    static final String DFA8_minS =
        "\2\56\3\uffff";
    static final String DFA8_maxS =
        "\1\71\1\145\3\uffff";
    static final String DFA8_acceptS =
        "\2\uffff\1\2\1\1\1\3";
    static final String DFA8_specialS =
        "\5\uffff}>";
    static final String[] DFA8_transitionS = {
            "\1\2\1\uffff\12\1",
            "\1\3\1\uffff\12\1\13\uffff\1\4\37\uffff\1\4",
            "",
            "",
            ""
    };

    static final short[] DFA8_eot = DFA.unpackEncodedString(DFA8_eotS);
    static final short[] DFA8_eof = DFA.unpackEncodedString(DFA8_eofS);
    static final char[] DFA8_min = DFA.unpackEncodedStringToUnsignedChars(DFA8_minS);
    static final char[] DFA8_max = DFA.unpackEncodedStringToUnsignedChars(DFA8_maxS);
    static final short[] DFA8_accept = DFA.unpackEncodedString(DFA8_acceptS);
    static final short[] DFA8_special = DFA.unpackEncodedString(DFA8_specialS);
    static final short[][] DFA8_transition;

    static {
        int numStates = DFA8_transitionS.length;
        DFA8_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA8_transition[i] = DFA.unpackEncodedString(DFA8_transitionS[i]);
        }
    }

    class DFA8 extends DFA {

        public DFA8(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 8;
            this.eot = DFA8_eot;
            this.eof = DFA8_eof;
            this.min = DFA8_min;
            this.max = DFA8_max;
            this.accept = DFA8_accept;
            this.special = DFA8_special;
            this.transition = DFA8_transition;
        }
        public String getDescription() {
            return "6:1: FLOAT : ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT );";
        }
    }
    static final String DFA30_eotS =
        "\1\1\11\uffff";
    static final String DFA30_eofS =
        "\12\uffff";
    static final String DFA30_minS =
        "\1\11\1\uffff\1\55\1\uffff\1\55\1\101\1\uffff\2\11\1\uffff";
    static final String DFA30_maxS =
        "\1\172\1\uffff\1\172\1\uffff\2\172\1\uffff\2\172\1\uffff";
    static final String DFA30_acceptS =
        "\1\uffff\1\4\1\uffff\1\3\2\uffff\1\1\2\uffff\1\2";
    static final String DFA30_specialS =
        "\12\uffff}>";
    static final String[] DFA30_transitionS = {
            "\2\3\2\uffff\1\3\22\uffff\1\3\7\uffff\1\3\5\uffff\1\3\22\uffff"+
            "\32\2\1\3\3\uffff\1\2\1\uffff\32\2",
            "",
            "\1\4\1\6\1\uffff\12\4\1\5\6\uffff\32\4\4\uffff\1\4\1\uffff"+
            "\32\4",
            "",
            "\1\4\1\6\1\uffff\12\4\1\5\6\uffff\32\4\4\uffff\1\4\1\uffff"+
            "\32\4",
            "\32\7\4\uffff\1\7\1\uffff\32\7",
            "",
            "\2\11\2\uffff\1\11\22\uffff\1\11\14\uffff\1\10\1\6\1\uffff"+
            "\12\10\7\uffff\32\10\4\uffff\1\10\1\uffff\32\10",
            "\2\11\2\uffff\1\11\22\uffff\1\11\14\uffff\1\10\1\6\1\uffff"+
            "\12\10\7\uffff\32\10\4\uffff\1\10\1\uffff\32\10",
            ""
    };

    static final short[] DFA30_eot = DFA.unpackEncodedString(DFA30_eotS);
    static final short[] DFA30_eof = DFA.unpackEncodedString(DFA30_eofS);
    static final char[] DFA30_min = DFA.unpackEncodedStringToUnsignedChars(DFA30_minS);
    static final char[] DFA30_max = DFA.unpackEncodedStringToUnsignedChars(DFA30_maxS);
    static final short[] DFA30_accept = DFA.unpackEncodedString(DFA30_acceptS);
    static final short[] DFA30_special = DFA.unpackEncodedString(DFA30_specialS);
    static final short[][] DFA30_transition;

    static {
        int numStates = DFA30_transitionS.length;
        DFA30_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA30_transition[i] = DFA.unpackEncodedString(DFA30_transitionS[i]);
        }
    }

    class DFA30 extends DFA {

        public DFA30(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 30;
            this.eot = DFA30_eot;
            this.eof = DFA30_eof;
            this.min = DFA30_min;
            this.max = DFA30_max;
            this.accept = DFA30_accept;
            this.special = DFA30_special;
            this.transition = DFA30_transition;
        }
        public String getDescription() {
            return "()* loopback of 41:27: ( ASSIGNMENT | DOMAIN_ASSIGNMENT | SECTION WS )*";
        }
    }
    static final String DFA31_eotS =
        "\1\11\1\14\2\11\1\uffff\1\23\1\26\1\uffff\1\11\1\uffff\1\11\1\14"+
        "\1\uffff\2\11\1\35\1\11\1\35\7\uffff\2\11\1\35\1\11\1\uffff\1\11"+
        "\1\35\1\11\1\55\2\uffff\1\11\1\uffff\1\11\1\uffff\1\11\1\35\1\11"+
        "\1\35\1\55\13\uffff\1\76\1\uffff\1\100\6\uffff\1\55\2\uffff\1\55"+
        "\5\uffff";
    static final String DFA31_eofS =
        "\112\uffff";
    static final String DFA31_minS =
        "\1\11\1\41\1\56\1\11\1\uffff\2\11\1\uffff\1\11\1\uffff\1\11\1\41"+
        "\1\uffff\2\101\1\41\1\53\1\41\1\101\4\uffff\2\101\2\11\1\41\1\53"+
        "\1\uffff\1\60\1\41\1\53\4\11\1\uffff\1\11\1\uffff\1\60\1\41\1\60"+
        "\1\41\1\11\1\uffff\1\137\2\11\1\101\1\54\1\11\1\54\6\11\1\101\1"+
        "\11\1\101\1\uffff\1\101\1\uffff\5\11\1\101\3\11";
    static final String DFA31_maxS =
        "\1\172\1\175\1\145\1\71\1\uffff\2\173\1\uffff\1\40\1\uffff\1\40"+
        "\1\175\1\uffff\2\172\1\175\1\71\1\175\1\172\4\uffff\4\172\1\175"+
        "\1\71\1\uffff\1\71\1\175\1\71\4\172\1\uffff\1\172\1\uffff\1\71\1"+
        "\175\1\71\1\175\1\172\1\uffff\1\173\1\40\2\172\1\135\1\172\1\135"+
        "\1\40\1\172\1\40\1\133\1\40\1\133\3\172\1\uffff\1\172\1\uffff\11"+
        "\172";
    static final String DFA31_acceptS =
        "\4\uffff\1\3\2\uffff\1\6\1\uffff\1\7\2\uffff\1\1\6\uffff\1\4\1\15"+
        "\1\16\1\5\6\uffff\1\2\7\uffff\1\11\1\uffff\1\10\5\uffff\1\14\20"+
        "\uffff\1\12\1\uffff\1\13\11\uffff";
    static final String DFA31_specialS =
        "\112\uffff}>";
    static final String[] DFA31_transitionS = {
            "\1\5\1\6\2\uffff\1\6\22\uffff\1\5\7\uffff\1\12\5\uffff\1\3\1"+
            "\4\12\2\4\uffff\1\7\2\uffff\32\1\1\10\3\uffff\1\1\1\uffff\32"+
            "\1",
            "\13\11\1\uffff\1\13\1\16\1\uffff\12\13\1\15\6\uffff\32\13\1"+
            "\11\1\uffff\1\11\1\uffff\1\13\1\uffff\32\13\3\11",
            "\1\17\1\uffff\12\2\13\uffff\1\20\37\uffff\1\20",
            "\1\22\26\uffff\1\22\17\uffff\12\21",
            "",
            "\2\25\2\uffff\1\25\22\uffff\1\25\7\uffff\1\25\5\uffff\1\25"+
            "\17\uffff\1\25\2\uffff\33\25\3\uffff\1\25\1\uffff\32\25\1\24",
            "\2\25\2\uffff\1\25\22\uffff\1\25\7\uffff\1\25\5\uffff\1\25"+
            "\17\uffff\1\25\2\uffff\33\25\3\uffff\1\25\1\uffff\32\25\1\24",
            "",
            "\1\27\26\uffff\1\27",
            "",
            "\1\30\26\uffff\1\30",
            "\13\11\1\uffff\1\13\1\16\1\uffff\12\13\1\15\6\uffff\32\13\1"+
            "\11\1\uffff\1\11\1\uffff\1\13\1\uffff\32\13\3\11",
            "",
            "\32\31\4\uffff\1\31\1\uffff\32\31",
            "\32\32\4\uffff\1\32\1\uffff\32\32",
            "\13\11\1\uffff\2\11\1\uffff\12\33\1\11\6\uffff\4\11\1\34\26"+
            "\11\1\uffff\1\11\1\uffff\1\11\1\uffff\4\11\1\34\30\11",
            "\1\36\1\uffff\1\36\2\uffff\12\37",
            "\13\11\1\uffff\2\11\1\uffff\12\21\1\11\6\uffff\4\11\1\40\26"+
            "\11\1\uffff\1\11\1\uffff\1\11\1\uffff\4\11\1\40\30\11",
            "\32\41\4\uffff\1\41\1\uffff\32\41",
            "",
            "",
            "",
            "",
            "\32\42\4\uffff\1\42\1\uffff\32\42",
            "\32\43\4\uffff\1\43\1\uffff\32\43",
            "\2\45\2\uffff\1\45\22\uffff\1\45\14\uffff\1\44\1\16\1\uffff"+
            "\12\44\7\uffff\32\44\4\uffff\1\44\1\uffff\32\44",
            "\2\47\2\uffff\1\47\22\uffff\1\47\14\uffff\1\46\2\uffff\12\46"+
            "\7\uffff\32\46\4\uffff\1\46\1\uffff\32\46",
            "\13\11\1\uffff\2\11\1\uffff\12\33\1\11\6\uffff\4\11\1\34\26"+
            "\11\1\uffff\1\11\1\uffff\1\11\1\uffff\4\11\1\34\30\11",
            "\1\50\1\uffff\1\50\2\uffff\12\51",
            "",
            "\12\37",
            "\13\11\1\uffff\2\11\1\uffff\12\37\1\11\6\uffff\33\11\1\uffff"+
            "\1\11\1\uffff\1\11\1\uffff\35\11",
            "\1\52\1\uffff\1\52\2\uffff\12\53",
            "\1\56\1\24\2\uffff\1\24\22\uffff\1\56\7\uffff\1\24\4\uffff"+
            "\1\54\1\57\1\uffff\12\54\7\uffff\32\54\1\24\3\uffff\1\54\1\uffff"+
            "\32\54",
            "\1\62\26\uffff\1\62\14\uffff\1\60\2\uffff\12\60\1\61\6\uffff"+
            "\32\60\4\uffff\1\60\1\uffff\32\60",
            "\1\64\26\uffff\1\64\14\uffff\1\63\2\uffff\12\63\7\uffff\32"+
            "\63\4\uffff\1\63\1\uffff\32\63",
            "\2\45\2\uffff\1\45\22\uffff\1\45\14\uffff\1\44\1\16\1\uffff"+
            "\12\44\7\uffff\32\44\4\uffff\1\44\1\uffff\32\44",
            "",
            "\2\47\2\uffff\1\47\22\uffff\1\47\14\uffff\1\46\2\uffff\12\46"+
            "\7\uffff\32\46\4\uffff\1\46\1\uffff\32\46",
            "",
            "\12\51",
            "\13\11\1\uffff\2\11\1\uffff\12\51\1\11\6\uffff\33\11\1\uffff"+
            "\1\11\1\uffff\1\11\1\uffff\35\11",
            "\12\53",
            "\13\11\1\uffff\2\11\1\uffff\12\53\1\11\6\uffff\33\11\1\uffff"+
            "\1\11\1\uffff\1\11\1\uffff\35\11",
            "\1\56\1\24\2\uffff\1\24\22\uffff\1\56\7\uffff\1\24\4\uffff"+
            "\1\54\1\57\1\uffff\12\54\7\uffff\32\54\1\24\3\uffff\1\54\1\uffff"+
            "\32\54",
            "",
            "\1\65\33\uffff\1\24",
            "\1\22\26\uffff\1\22",
            "\1\62\26\uffff\1\62\14\uffff\1\60\2\uffff\12\60\1\61\6\uffff"+
            "\32\60\4\uffff\1\60\1\uffff\32\60",
            "\32\66\4\uffff\1\66\1\uffff\32\66",
            "\1\67\60\uffff\1\70",
            "\1\64\26\uffff\1\64\14\uffff\1\63\2\uffff\12\63\7\uffff\32"+
            "\63\4\uffff\1\63\1\uffff\32\63",
            "\1\71\60\uffff\1\72",
            "\1\73\26\uffff\1\73",
            "\1\62\26\uffff\1\62\14\uffff\1\74\2\uffff\12\74\7\uffff\32"+
            "\74\4\uffff\1\74\1\uffff\32\74",
            "\1\75\26\uffff\1\75",
            "\2\24\2\uffff\1\24\22\uffff\1\24\7\uffff\1\24\5\uffff\1\24"+
            "\54\uffff\1\24",
            "\1\77\26\uffff\1\77",
            "\2\24\2\uffff\1\24\22\uffff\1\24\7\uffff\1\24\5\uffff\1\24"+
            "\54\uffff\1\24",
            "\32\101\4\uffff\1\101\1\uffff\32\101",
            "\1\62\26\uffff\1\62\14\uffff\1\74\2\uffff\12\74\7\uffff\32"+
            "\74\4\uffff\1\74\1\uffff\32\74",
            "\32\102\4\uffff\1\102\1\uffff\32\102",
            "",
            "\32\103\4\uffff\1\103\1\uffff\32\103",
            "",
            "\1\56\1\24\2\uffff\1\24\22\uffff\1\56\7\uffff\1\24\4\uffff"+
            "\1\104\1\57\1\uffff\12\104\7\uffff\32\104\1\24\3\uffff\1\104"+
            "\1\uffff\32\104",
            "\1\62\26\uffff\1\62\14\uffff\1\105\2\uffff\12\105\1\106\6\uffff"+
            "\32\105\4\uffff\1\105\1\uffff\32\105",
            "\1\64\26\uffff\1\64\14\uffff\1\107\2\uffff\12\107\7\uffff\32"+
            "\107\4\uffff\1\107\1\uffff\32\107",
            "\1\56\1\24\2\uffff\1\24\22\uffff\1\56\7\uffff\1\24\4\uffff"+
            "\1\104\1\57\1\uffff\12\104\7\uffff\32\104\1\24\3\uffff\1\104"+
            "\1\uffff\32\104",
            "\1\62\26\uffff\1\62\14\uffff\1\105\2\uffff\12\105\1\106\6\uffff"+
            "\32\105\4\uffff\1\105\1\uffff\32\105",
            "\32\110\4\uffff\1\110\1\uffff\32\110",
            "\1\64\26\uffff\1\64\14\uffff\1\107\2\uffff\12\107\7\uffff\32"+
            "\107\4\uffff\1\107\1\uffff\32\107",
            "\1\62\26\uffff\1\62\14\uffff\1\111\2\uffff\12\111\7\uffff\32"+
            "\111\4\uffff\1\111\1\uffff\32\111",
            "\1\62\26\uffff\1\62\14\uffff\1\111\2\uffff\12\111\7\uffff\32"+
            "\111\4\uffff\1\111\1\uffff\32\111"
    };

    static final short[] DFA31_eot = DFA.unpackEncodedString(DFA31_eotS);
    static final short[] DFA31_eof = DFA.unpackEncodedString(DFA31_eofS);
    static final char[] DFA31_min = DFA.unpackEncodedStringToUnsignedChars(DFA31_minS);
    static final char[] DFA31_max = DFA.unpackEncodedStringToUnsignedChars(DFA31_maxS);
    static final short[] DFA31_accept = DFA.unpackEncodedString(DFA31_acceptS);
    static final short[] DFA31_special = DFA.unpackEncodedString(DFA31_specialS);
    static final short[][] DFA31_transition;

    static {
        int numStates = DFA31_transitionS.length;
        DFA31_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA31_transition[i] = DFA.unpackEncodedString(DFA31_transitionS[i]);
        }
    }

    class DFA31 extends DFA {

        public DFA31(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 31;
            this.eot = DFA31_eot;
            this.eof = DFA31_eof;
            this.min = DFA31_min;
            this.max = DFA31_max;
            this.accept = DFA31_accept;
            this.special = DFA31_special;
            this.transition = DFA31_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( ID | FLOAT | COMMENT | WSNL | WS | ANIMATION | VALUE | ASSIGNMENT | DOMAIN_ASSIGNMENT | TYPE_SET | GROUP_SET | STATE_SET | SECTION | FILE );";
        }
    }
 

}