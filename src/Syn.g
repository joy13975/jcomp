// COMS22201: Syntax analyser

parser grammar Syn;

options {
  tokenVocab = Lex;
  output = AST;
//  backtrack = true;
}

@header
{
    import java.io.*;
}

@members
{
	private String cleanString(String s){
		String tmp;
		tmp = s.replaceAll("^'", "");
		s = tmp.replaceAll("'$", "");
		tmp = s.replaceAll("''", "'");
		return tmp;
	}

    @Override
    public void displayRecognitionError(String[] tokenNames, RecognitionException re)
    {
        HashMap<String, String> tokenStrings = new HashMap<String, String>();
        tokenStrings.put("FALSE", "a boolean literal: \"false\"");
        tokenStrings.put("TRUE", "a boolean literal: \"true\"");
        tokenStrings.put("IF", "a keyword: \"if\"");
        tokenStrings.put("THEN", "a keyword: \"then\"");
        tokenStrings.put("ELSE", "a keyword: \"else\"");
        tokenStrings.put("SKIP", "a keyword: \"skip\"");
        tokenStrings.put("DO", "a keyword: \"do\"");
        tokenStrings.put("WHILE", "a keyword: \"while\"");
        tokenStrings.put("READ", "a keyword: \"read\"");
        tokenStrings.put("WRITELN", "a keyword: \"writeln\"");
        tokenStrings.put("WRITE", "a keyword \"write\"");
        tokenStrings.put("EOF", "end of file");
        tokenStrings.put("ASSIGNMENT", "an assignment: \":=\"");
        tokenStrings.put("EQ_TEST", "an equality test: \"=\"");
        tokenStrings.put("LE_TEST", "a less-or-equal test: \"<=\"");
        tokenStrings.put("PLUS", "an addition sign: \"+\"");
        tokenStrings.put("MINUS", "a minus sign: \"-\"");
        tokenStrings.put("MULTIPLY", "an asterisk (multiply): \"*\"");
        tokenStrings.put("DIVISION", "a slash (division): \"/\"");
        tokenStrings.put("DOT", "a period: \".\"");
        tokenStrings.put("SEMICOLON", "a semicolon: \";\"");
        tokenStrings.put("OPENPAREN", "an open parenthesis: \"(\"");
        tokenStrings.put("CLOSEPAREN", "a close parenthesis: \")\"");
        tokenStrings.put("INTNUM", "an integer");
        tokenStrings.put("REAL", "a real number");
        tokenStrings.put("STRING", "a string: \"\'...\'\"");
        tokenStrings.put("COMMENT", "a comment: \"{...}\"");
        tokenStrings.put("WS", "control characters");
        tokenStrings.put("IDENTIFIER", "a variable name");
        tokenStrings.put("AMPERSAND", "an ampersand: \"&\"\"");
        tokenStrings.put("EXCLAMATION", "an exclamation mark: \"!\"");

        String path = getSourceName();
        String msg = getErrorMessage(re, tokenNames);
        logf("\n[\%s:\%d:\%d] Syntax error\n", path, re.line, re.charPositionInLine);

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
            logf("^\n");
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        String badTokenStr;
        if(re.token.getType() < 0)
        {
            badTokenStr = "\"" + re.token.getText() + "\"";
        }
        else
        {
            badTokenStr = tokenStrings.get(tokenNames[re.token.getType()]);
        }

        String prevName;
        try
        {
            prevName = tokenNames[re.input.LA(-1)];
        }
        catch(Exception e)
        {
            prevName = "";
        }

        if(re instanceof MismatchedTokenException)
        {
            MismatchedTokenException mte = (MismatchedTokenException) re;
            if(mte.expecting < 0)
            {
                logf("Run-on code: missing a semicolon that separates statements.\n");
            }
            else
            {
                String expectType = tokenNames[mte.expecting];
                String expStr = tokenStrings.get(expectType);

                logf("Expecting \%s, but found \%s.\n", expStr, badTokenStr);
            }
        }
        else if(re instanceof NoViableAltException)
        {
            if(prevName.equals("SEMICOLON"))
            {
                logf("\%s cannot start an expected statement.\n", badTokenStr.toUpperCase().charAt(0) + badTokenStr.substring(1));
            }
            else
            {
                logf("\%s cannot start an expected expression.\n", badTokenStr.toUpperCase().charAt(0) + badTokenStr.substring(1));
            }
        }
        else
        {
            logf("Unhandled error type: \%s\n", re.getClass().toString());
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

program:
        statements^ EOF
;

statements:
        statement (SEMICOLON^ statement)*
;


statement:
        IDENTIFIER ASSIGNMENT^ exp
    |   SKIP
    |   IF^ boolexp
        THEN! statement
        ELSE! statement
    |   WHILE^ boolexp
        DO! statement
    |   READ^ OPENPAREN! IDENTIFIER CLOSEPAREN!
    |   WRITE^ OPENPAREN! write_param CLOSEPAREN!
    |   WRITELN
    |   OPENPAREN! statements CLOSEPAREN!
;

write_param:
        ( exp ) => ( exp )
    |	boolexp
    |	string
;

boolexp:
        boolterm ( AMPERSAND^ boolterm )*
;

boolterm:
        EXCLAMATION^ boole
    |   boole
;

boole:
        TRUE
    |   FALSE
    |   ( testexp ) => ( testexp )
    |   OPENPAREN! boolexp CLOSEPAREN!
;

testexp:
	exp ( EQ_TEST | LE_TEST )^ exp
;

exp:
        term ( (PLUS | MINUS)^ term)*
;

term:
        factor ( (MULTIPLY | DIVISION)^ factor )*
;

factor:
        IDENTIFIER
    |   INTNUM
    |   REAL
    |   OPENPAREN! exp CLOSEPAREN!
;

string
    scope { String tmp; }
    :
    s=STRING { $string::tmp = cleanString($s.text); }-> STRING[$string::tmp]
;