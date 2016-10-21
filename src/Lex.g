// COMS22201: Lexical analyser

lexer grammar Lex;

@header
{
    import java.io.*;
}

@members
{
    @Override
    public void displayRecognitionError(String[] tokenNames, RecognitionException re)
    {
        String path = getSourceName();
        String msg = getErrorMessage(re, tokenNames);
        logf("\n[\%s:\%d:\%d] Lexical error\n", path, re.line, re.charPositionInLine);

        String badStr = "";
        try{
            BufferedReader bf = new BufferedReader(new FileReader(path));
            int l = 1;
            String line;
            while((line = bf.readLine()) != null)
            {
                if(l == re.line)
                {
                    logf("\%s\n", line);
                    break;
                }
                l++;
            }
            int c = 0;
            while(c++ != re.charPositionInLine)
                logf(" ");
            if(re.charPositionInLine < line.length())
                badStr = line.charAt(re.charPositionInLine) + "";
            else
                badStr = "<EOF>";
            logf("^\n");
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        if(re instanceof MismatchedTokenException)
        {
            MismatchedTokenException mte = (MismatchedTokenException) re;
            logf("Expecting \'\%c\'.\n", mte.expecting);
        }
        else
        {

            if(badStr.equals("{") || badStr.equals("}"))
            {
                logf("Unmatched comment bracket.\n");
            }
            else if(badStr.equals("\'"))
            {
                logf("Unmatched string quote.\n");
            }
            else
            {
                logf("Character \'\%s\' not defined in language.\n", badStr);
            }
        }

        logf("CAMLE exiting.\n\n");
        System.exit(1);
    }

    private void logf(String s, Object ... args)
    {
        System.err.printf(s, args);
    }
}

//---------------------------------------------------------------------------
// Fragments
//---------------------------------------------------------------------------
fragment Digit          : '0'..'9' ;
fragment Alphabet       : 'A'..'Z' | 'a'..'z';
fragment Alphanumeric   : Digit | Alphabet;

//---------------------------------------------------------------------------
// KEYWORDS
//---------------------------------------------------------------------------
FALSE       : 'false';
TRUE        : 'true';

IF          : 'if';
THEN        : 'then';
ELSE        : 'else';

SKIP        : 'skip';

DO          : 'do';
WHILE       : 'while';

READ        : 'read';
WRITELN     : 'writeln';
WRITE       : 'write';

//---------------------------------------------------------------------------
// OPERATORS
//---------------------------------------------------------------------------
ASSIGNMENT  : ':=';
EQ_TEST     : '=';
LE_TEST     : '<=';
PLUS        : '+';
MINUS       : '-';
MULTIPLY    : '*';
DIVISION    : '/';
DOT         : '.';

SEMICOLON   : ';';
OPENPAREN   : '(';
CLOSEPAREN  : ')';
INTNUM      : Digit+;
REAL        : Digit* '.' Digit*;
STRING      : '\'' ('\'' '\'' | ~'\'')* '\'';
COMMENT     : '{' (~'}')* '}' {skip();} ;
WS          : (' ' | '\t' | '\r' | '\n' )+ {skip();} ;


//---------------------------------------------------------------------------
// TOKENS
//---------------------------------------------------------------------------
IDENTIFIER  : Alphabet (((((((Alphanumeric)? Alphanumeric)? Alphanumeric)? Alphanumeric)? Alphanumeric)? Alphanumeric)? Alphanumeric)?;
AMPERSAND   : '&';
EXCLAMATION : '!';
