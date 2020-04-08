/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler.java;


import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.CompilerErrorInfo;
import com.tangosol.dev.compiler.ParsePosition;
import com.tangosol.dev.compiler.Script;

import com.tangosol.util.Base;
import com.tangosol.util.Dequeue;
import com.tangosol.util.ErrorList;
import com.tangosol.util.HashHelper;

import java.io.EOFException;
import java.io.IOException;

import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Properties;

import java.lang.Character;


/**
* A unicode script tokenizer for the java language, originally inspired by
* the java.util package's StringTokenizer class.
*
* TODO add support for comment parsing
*
* @version 1.00, 11/21/96
* @author  Cameron Purdy
*/
public class Tokenizer
        extends Base
        implements com.tangosol.dev.compiler.Tokenizer, Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * (Default) Constructs a Tokenizer.
    */
    public Tokenizer()
        {
        initOptions(null);
        }

    /**
    * Constructs a Tokenizer.
    *
    * @param s the string which contains the Java script to tokenize
    * @param iLine  the beginning line number
    * @param errlist the error list which the tokenizer will log to
    *
    * @exception CompilerException If the script tokenizer encounters an
    *            invalid unicode escape sequence
    */
    public Tokenizer(String s, int iLine, ErrorList errlist)
            throws CompilerException
        {
        this(s, iLine, errlist, null);
        }

    /**
    * Constructs a Tokenizer.
    *
    * @param s         the string which contains the Java script to tokenize
    * @param errlist   the error list which the tokenizer will log to
    * @param propOpts  a java.util.Properties object containing options
    *
    * @exception CompilerException If the script tokenizer encounters an
    *            invalid unicode escape sequence
    */
    public Tokenizer(String s, ErrorList errlist, Properties propOpts)
            throws CompilerException
        {
        this(s, 0, errlist, propOpts);
        }

    /**
    * Constructs a Tokenizer.
    *
    * @param s         the string which contains the Java script to tokenize
    * @param iLine     the beginning line number
    * @param errlist   the error list which the tokenizer will log to
    * @param propOpts  a java.util.Properties object containing options
    *
    * @exception CompilerException If the script tokenizer encounters an
    *            invalid unicode escape sequence
    */
    public Tokenizer(String s, int iLine, ErrorList errlist, Properties propOpts)
            throws CompilerException
        {
        initOptions(propOpts);
        setScript(new UnicodeScript(s, iLine), errlist);
        }


    // ----- Tokenizer options ----------------------------------------------

    /**
    * Specify the tokenizer options.
    *
    * @param propOpts  a java.util.Properties object containing options
    */
    private void initOptions(Properties propOpts)
        {
        // default option values (for backwards compatibility)
        m_fExtensionsEnabled = true;
        m_fCommentsEnabled   = false;

        if (propOpts != null)
            {
            m_fExtensionsEnabled = extractBooleanOption(propOpts, OPT_PARSE_EXTENSIONS, m_fExtensionsEnabled);
            m_fCommentsEnabled   = extractBooleanOption(propOpts, OPT_PARSE_COMMENTS  , m_fCommentsEnabled  );
            }
        }

    /**
    * Extract a boolean option value from a Properties object.
    *
    * @param propOpts  the java.util.Properties object
    * @param sName     the option property name
    * @param fDefault  the default option value
    *
    * @return true or false if the value is specified, otherwise the value of
    *         the fDefault parameter
    */
    private static boolean extractBooleanOption(Properties propOpts, String sName, boolean fDefault)
        {
        String sValue = propOpts.getProperty(sName);

        if (sValue != null && sValue.length() > 0)
            {
            switch (sValue.trim().charAt(0))
                {
                case 'T':   // TRUE or True
                case 't':   // true
                case 'Y':   // YES or Yes
                case 'y':   // yes or y
                case '1':   // integer representation of true
                    return true;

                case 'F':   // FALSE or False
                case 'f':   // false
                case 'N':   // NO or No
                case 'n':   // no
                case '0':   // integer representation of false
                    return false;
                }
            }

        return fDefault;
        }

    /**
    * Determine whether the tokenizer will parse proprietary extensions, such
    * as the "as" keyword.
    *
    * @return true if the tokenizer will parse proprietary extensions
    */
    public boolean isExtensionsEnabled()
        {
        return m_fExtensionsEnabled;
        }

    /**
    * Specify whether the tokenizer will parse proprietary extensions, such
    * as the "as" keyword.
    *
    * @param f  pass true to cause the tokenizer to parse proprietary
    *           extensions or pass false to follow the JLS strictly
    */
    public void setExtensionsEnabled(boolean f)
        {
        m_fExtensionsEnabled = f;
        }

    /**
    * Determine whether the tokenizer will parse comments as tokens or
    * simply discard them.
    *
    * @return true if the tokenizer will return comments as tokens
    */
    public boolean isCommentsEnabled()
        {
        return m_fCommentsEnabled;
        }

    /**
    * Specify whether the tokenizer will parse comments as tokens or
    * simply discard them.
    *
    * @param f  pass true to cause the tokenizer to return comments as tokens
    *           or false to cause the tokenizer to discard comments
    */
    public void setCommentsEnabled(boolean f)
        {
        m_fCommentsEnabled = f;
        }


    // ----- Toker interface ------------------------------------------------

    /**
    * Initializes the tokenizer.  This method must be called exactly
    * one time to initialize the tokenizer object.
    *
    * @param script   the script to tokenize
    * @param errlist  the list to log errors to
    *
    * @exception NoSuchElementException If the tokens are exhausted
    * @exception CompilerException If a lexical error occurs that should stop
    *            the compilation process
    */
    public void setScript(Script script, ErrorList errlist)
            throws CompilerException
        {
        // assert:  this method must be called only one time
        if (m_script != null)
            {
            throw new IllegalStateException();
            }

        m_errlist  = errlist;
        m_script   = script;
        m_tblNames = new Hashtable();
        m_dq       = new Dequeue();

        // eat up to the next token
        eatFluff();
        }

    /**
    * Checks for more tokens in the script.
    *
    * @return true if tokenizing of the script is incomplete.
    */
    public boolean hasMoreTokens()
        {
        // first check for any tokens that were "put back"
        if (m_dq.hasMoreElements())
            {
            return true;
            }

        // check if there is more to parse
        return m_script.hasMoreChars();
        }

    /**
    * Eats and returns the next token from the script.
    *
    * @return the next token
    *
    * @exception NoSuchElementException If the tokens are exhausted
    * @exception CompilerException If a lexical error occurs that should stop
    *            the compilation process
    */
    public com.tangosol.dev.compiler.Token nextToken()
            throws CompilerException
        {
        Token tok;

        if (m_dq.hasMoreElements())
            {
            tok = (Token) m_dq.nextElement();
            }
        else if (m_script.hasMoreChars())
            {
            tok = eatToken();
            eatFluff();
            }
        else
            {
            throw new NoSuchElementException();
            }

        return tok;
        }

    /**
    * Regurgitates the last eaten token so that the next call to nextToken
    * will return the same token that was returned by the most recent call
    * to nextToken.  (This method can be called more than once to regurgitate
    * multiple tokens.)
    *
    * @exception NoSuchElementException an attempt to back up past the
    *            beginning of the script -or- the dequeue was not constructed
    *            large enough to hold the number of entries that have been
    *            put back.
    */
    public void putBackToken(com.tangosol.dev.compiler.Token tok)
        {
        m_dq.putBackElement(tok);
        }

    /**
    * Returns an object that can be used to restore the current position
    * in the script.  This method is similar to the mark method of the
    * Java stream classes, but by returning an object that identifies the
    * position, multiple positions can be saved and later returned to.
    *
    * @return an object which identifies the current position within the
    *         script
    */
    public ParsePosition savePosition()
        {
        Position pos = new Position();

        pos.toker     = this;
        pos.dq        = (Dequeue) m_dq.clone();
        pos.scriptpos = m_script.savePosition();

        return pos;
        }

    /**
    * Restores the current parsing position that was returned from
    * the savePosition method.
    */
    public void restorePosition(ParsePosition parsepos)
        {
        Position pos = (Position) parsepos;
        if (pos.toker != this)
            {
            throw new IllegalArgumentException("Unknown ParsePosition object!");
            }

        m_script.restorePosition(pos.scriptpos);
        m_dq = pos.dq;
        }

    /**
    * Get current line number.
    *
    * @return the line number of the next character in the script
    */
    public int getLine()
        {
        return m_script.getLine();
        }


    // ----- script parsing -------------------------------------------------

    /**
    * Eats whitespace and comments until the next java token is encountered
    * or the end of the string is reached, whichever comes first.
    *
    * @exception CompilerException Thrown for unexpected end-of file, which
    *            can be caused by a SUB character encountered before the end
    *            of the script, by a non-terminated multi-line comment, etc.
    */
    protected void eatFluff()
            throws CompilerException
        {
        try
            {
            while (m_script.hasMoreChars())
                {
                char ch = m_script.nextChar();
                switch (ch)
                    {
                    // ignore LineTerminator (3.4)
                    case '\n':          // new line
                        break;

                    // ignore Sub (3.5)
                    case Script.SUB:     // end-of-file
                        break;

                    // ignore WhiteSpace (3.6)
                    case '\t':          // h-tab
                    case '\f':          // v-tab
                    case '\r':          // carriage return
                    case ' ':           // space
                        break;

                    // ignore Comments (3.7)
                    case '/':
                        {
                        ch = m_script.nextChar();
                        if (ch == '*')
                            {
                            eatMultiLineComment();
                            }
                        else if (ch == '/')
                            {
                            eatSingleLineComment();
                            }
                        else
                            {
                            m_script.putBackChar();
                            m_script.putBackChar();
                            return;
                            }
                        }
                        break;

                    // a token has been encountered
                    default:
                        m_script.putBackChar();
                        return;
                    }
                }
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null,
                    m_script.getLine(), m_script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (UnicodeDataFormatException e)
            {
            logError(ERROR, ERR_UNICODE_ESCAPE, null,
                    m_script.getLine(), m_script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    m_script.getLine(), m_script.getOffset(), 0);
            throw new CompilerException();
            }
        }

    /**
    * Eats everything until the end of the multi-line comment is found
    * or the end of the string is reached, whichever comes first.  Note
    * that the opening portion of the comment ('/' + '*') has already
    * been eaten.
    *
    * @exception EOFException as thrown by the script object
    */
    protected void eatMultiLineComment()
            throws IOException
        {
        while (true)
            {
            if (m_script.nextChar() == '*')
                {
                if (m_script.nextChar() == '/')
                    {
                    return;
                    }
                m_script.putBackChar();
                }
            }
        }

    /**
    * Eats everything until an end-of-line character is encountered or
    * the end of the string is reached, whichever comes first.  Note
    * that the opening portion of the comment ('/' + '/') has already
    * been eaten.
    *
    * @exception IOException as thrown by the script object
    */
    protected void eatSingleLineComment()
            throws IOException
        {
        while (m_script.hasMoreChars())
            {
            char ch = m_script.nextChar();
            if (ch == '\r' || ch == '\n')
                {
                m_script.putBackChar();
                return;
                }
            }
        }

    /**
    * Eats the largest token possible, returning a Token interface which
    * describes the token.
    *
    * @exception CompilerException If a lexical error is encountered which
    *            should stop compilation, like an unexpected end-of-file
    */
    protected Token eatToken()
            throws CompilerException
        {
        try
            {
            while (true)
                {
                int  iLine    = m_script.getLine();
                int  ofInLine = m_script.getOffset();
                char ch       = m_script.nextChar();
                switch(ch)
                    {
                    // Separator (3.11) - the easiest tokens to parse
                    case '(':
                        return new Token(Token.TOK_LPARENTHESIS, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case ')':
                        return new Token(Token.TOK_RPARENTHESIS, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case '{':
                        return new Token(Token.TOK_LCURLYBRACE, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case '}':
                        return new Token(Token.TOK_RCURLYBRACE, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case '[':
                        return new Token(Token.TOK_LBRACKET, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case ']':
                        return new Token(Token.TOK_RBRACKET, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case ';':
                        return new Token(Token.TOK_SEMICOLON, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case ',':
                        return new Token(Token.TOK_COMMA, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case '.':
                        {
                        // test for '.' as the start of a FloatingPointLiteral (3.10.2)
                        boolean fNum = isDecimal(m_script.nextChar());
                        m_script.putBackChar();

                        // if it is a number, then put back the '.' and parse
                        if (fNum)
                            {
                            m_script.putBackChar();
                            return eatFloatingPointLiteral();
                            }
                        }
                        return new Token(Token.TOK_DOT, iLine, ofInLine, m_script.getOffset() - ofInLine);

                    // Operator (3.12) - the second easiest tokens to parse
                    case '~':
                        return new Token(Token.TOK_BITNOT, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case '?':
                        return new Token(Token.TOK_CONDITIONAL, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case ':':
                        return new Token(Token.TOK_COLON, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case '+':
                        switch(m_script.nextChar())
                            {
                            case '+':
                                return new Token(Token.TOK_INCREMENT, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            case '=':
                                return new Token(Token.TOK_ASSIGN_ADD, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            default:
                                m_script.putBackChar();
                                return new Token(Token.TOK_ADD, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            }
                    case '-':
                        switch(m_script.nextChar())
                            {
                            case '-':
                                return new Token(Token.TOK_DECREMENT, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            case '=':
                                return new Token(Token.TOK_ASSIGN_SUB, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            default:
                                m_script.putBackChar();
                                return new Token(Token.TOK_SUB, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            }
                    case '*':
                        if (m_script.nextChar() == '=')
                            {
                            return new Token(Token.TOK_ASSIGN_MUL, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            }
                        m_script.putBackChar();
                        return new Token(Token.TOK_MUL, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case '/':
                        if (m_script.nextChar() == '=')
                            {
                            return new Token(Token.TOK_ASSIGN_DIV, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            }
                        m_script.putBackChar();
                        return new Token(Token.TOK_DIV, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case '%':
                        if (m_script.nextChar() == '=')
                            {
                            return new Token(Token.TOK_ASSIGN_REM, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            }
                        m_script.putBackChar();
                        return new Token(Token.TOK_REM, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case '^':
                        if (m_script.nextChar() == '=')
                            {
                            return new Token(Token.TOK_ASSIGN_BITXOR, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            }
                        m_script.putBackChar();
                        return new Token(Token.TOK_BITXOR, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case '=':
                        if (m_script.nextChar() == '=')
                            {
                            return new Token(Token.TOK_TEST_EQ, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            }
                        m_script.putBackChar();
                        return new Token(Token.TOK_ASSIGN, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case '!':
                        if (m_script.nextChar() == '=')
                            {
                            return new Token(Token.TOK_TEST_NE, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            }
                        m_script.putBackChar();
                        return new Token(Token.TOK_LOGICAL_NOT, iLine, ofInLine, m_script.getOffset() - ofInLine);
                    case '&':
                        switch(m_script.nextChar())
                            {
                            case '&':
                                return new Token(Token.TOK_LOGICAL_AND, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            case '=':
                                return new Token(Token.TOK_ASSIGN_BITAND, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            default:
                                m_script.putBackChar();
                                return new Token(Token.TOK_BITAND, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            }
                    case '|':
                        switch(m_script.nextChar())
                            {
                            case '|':
                                return new Token(Token.TOK_LOGICAL_OR, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            case '=':
                                return new Token(Token.TOK_ASSIGN_BITOR, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            default:
                                m_script.putBackChar();
                                return new Token(Token.TOK_BITOR, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            }
                    case '<':
                        switch(m_script.nextChar())
                            {
                            case '=':
                                return new Token(Token.TOK_TEST_LE, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            case '<':
                                if (m_script.nextChar() == '=')
                                    {
                                    return new Token(Token.TOK_ASSIGN_SHL, iLine, ofInLine, m_script.getOffset() - ofInLine);
                                    }
                                m_script.putBackChar();
                                return new Token(Token.TOK_SHL, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            default:
                                m_script.putBackChar();
                                return new Token(Token.TOK_TEST_LT, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            }
                    case '>':
                        switch(m_script.nextChar())
                            {
                            case '=':
                                return new Token(Token.TOK_TEST_GE, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            case '>':
                                switch(m_script.nextChar())
                                    {
                                    case '>':
                                        if (m_script.nextChar() == '=')
                                            {
                                            return new Token(Token.TOK_ASSIGN_USHR, iLine, ofInLine, m_script.getOffset() - ofInLine);
                                            }
                                        m_script.putBackChar();
                                        return new Token(Token.TOK_USHR, iLine, ofInLine, m_script.getOffset() - ofInLine);
                                    case '=':
                                        return new Token(Token.TOK_ASSIGN_SHR, iLine, ofInLine, m_script.getOffset() - ofInLine);
                                    default:
                                        m_script.putBackChar();
                                        return new Token(Token.TOK_SHR, iLine, ofInLine, m_script.getOffset() - ofInLine);
                                    }
                            default:
                                m_script.putBackChar();
                                return new Token(Token.TOK_TEST_GT, iLine, ofInLine, m_script.getOffset() - ofInLine);
                            }

                    // CharacterLiteral (3.10.4)
                    case '\'':
                        m_script.putBackChar();
                        return eatCharacterLiteral();

                    // StringLiteral (3.10.5)
                    case '\"':
                    case '`':   // non-standard
                        m_script.putBackChar();
                        return eatStringLiteral();

                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        // IntegerLiteral (3.10.1)
                        // FloatingPointLiteral (3.10.2)
                        // (see also TOK_DOT in the Separator (3.11) section)
                        m_script.putBackChar();
                        return eatNumericLiteral();

                    case '_':                   // ASCII underscore
                    case '$':                   // ASCII dollar sign
                    case 'A':   case 'a':       // ASCII latin A-Z and a-z
                    case 'B':   case 'b':
                    case 'C':   case 'c':
                    case 'D':   case 'd':
                    case 'E':   case 'e':
                    case 'F':   case 'f':
                    case 'G':   case 'g':
                    case 'H':   case 'h':
                    case 'I':   case 'i':
                    case 'J':   case 'j':
                    case 'K':   case 'k':
                    case 'L':   case 'l':
                    case 'M':   case 'm':
                    case 'N':   case 'n':
                    case 'O':   case 'o':
                    case 'P':   case 'p':
                    case 'Q':   case 'q':
                    case 'R':   case 'r':
                    case 'S':   case 's':
                    case 'T':   case 't':
                    case 'U':   case 'u':
                    case 'V':   case 'v':
                    case 'W':   case 'w':
                    case 'X':   case 'x':
                    case 'Y':   case 'y':
                    case 'Z':   case 'z':
                        // identifier (3.8) or keyword (3.9)
                        m_script.putBackChar();
                        return eatIdentifierOrKeyword();

                    default:
                        {
                        if (Character.isJavaIdentifierStart(ch))
                            {
                            // identifier (3.8) or keyword (3.9)
                            m_script.putBackChar();
                            return eatIdentifierOrKeyword();
                            }

                        // lexical error -- the character is not a valid "lead"
                        // character for any of the java tokens
                        logError(ERROR, ERR_INVALID_CHAR,
                            new String[] {LiteralToken.printableChar(ch)},
                            iLine, ofInLine, m_script.getOffset() - ofInLine);
                        eatFluff();
                        }
                    }
                }
            }
        catch (EOFException e)
            {
            logError(ERROR, ERR_UNEXPECTED_EOF, null, m_script.getLine(), m_script.getOffset(), 0);
            throw new CompilerException();
            }
        catch (IOException e)
            {
            logError(ERROR, ERR_UNEXPECTED_IO, new String[] {e.toString()},
                    m_script.getLine(), m_script.getOffset(), 0);
            throw new CompilerException();
            }
        }

    /**
    * Eats a character literal, ie. a single quote, a character or
    * escaped character, and a closing single quote.
    *
    * @exception CompilerException If a lexical error is encountered which
    *            should stop compilation (like a full error list)
    * @exception EOFException Unexpected end-of-file
    */
    protected Token eatCharacterLiteral()
            throws CompilerException, IOException
        {
        int  iLine    = m_script.getLine();
        int  ofInLine = m_script.getOffset();

        // skip opening single quote
        m_script.nextChar();

        char ch = m_script.nextChar();
        switch (ch)
            {
            case '\\':
                m_script.putBackChar();
                ch = eatEscapedCharacter();
                break;

            case '\r':
            case '\n':
                // an unexpected end-of-line was found; don't try to complete
                // parsing of the character literal
                m_script.putBackChar();
                logError(ERROR, ERR_NEWLINE_IN_LIT, null,
                        iLine, m_script.getOffset(), 0);
                return new LiteralToken(' ', iLine, ofInLine, m_script.getOffset() - ofInLine);

            case '\'':
                // get the exact offset of the single quote
                m_script.putBackChar();
                int ofSQuote = m_script.getOffset();
                m_script.nextChar();
                // log the error, highlighting the unescaped single quote
                logError(ERROR, ERR_UNESC_S_QUOTE, null, iLine, ofSQuote, m_script.getOffset() - ofSQuote);
            }

        // closing single quote
        if (m_script.nextChar() != '\'')
            {
            m_script.putBackChar();
            logError(ERROR, ERR_S_QUOTE_EXP, null, iLine, m_script.getOffset(), 0);
            }

        return new LiteralToken(ch, iLine, ofInLine, m_script.getOffset() - ofInLine);
        }

    /**
    * Eats a string literal.  A string literal is composed of a double quote,
    * a set of characters, and a closing double quote.
    *
    * @exception CompilerException If a lexical error is encountered which
    *            should stop compilation (like a full error list)
    * @exception EOFException Unexpected end-of-file
    */
    protected Token eatStringLiteral()
            throws CompilerException, IOException
        {
        int  iLine    = m_script.getLine();
        int  ofInLine = m_script.getOffset();

        // skip opening double quote
        char chQuote = m_script.nextChar();

        StringBuffer sb = new StringBuffer();
        char ch = m_script.nextChar();
        while (ch != chQuote)
            {
            if (ch == '\\')
                {
                m_script.putBackChar();
                ch = eatEscapedCharacter();
                }
            else if (ch == '\r' || ch == '\n')
                {
                m_script.putBackChar();
                logError(ERROR, ERR_NEWLINE_IN_LIT, null, iLine, m_script.getOffset(), 0);
                break;
                }

            sb.append(ch);
            ch = m_script.nextChar();
            }

        String s = sb.toString();
        if (chQuote == '`')
            {
            s = HashHelper.hash(s);
            }
        return new LiteralToken(s, iLine, ofInLine, m_script.getOffset() - ofInLine);
        }

    /**
    * Eats a character escape sequence.  See EscapeSequence (Java Language
    * Specification 3.10.6).
    *
    * @exception CompilerException If a lexical error is encountered which
    *            should stop compilation (like a full error list)
    * @exception IOException Unexpected end-of-file, etc.
    */
    public char eatEscapedCharacter()
            throws CompilerException, IOException
        {
        // skip the escape ('\')
        m_script.nextChar();

        // check the escaped character
        char ch = m_script.nextChar();
        switch (ch)
            {
            case 'b':
                return '\b';
            case 't':
                return '\t';
            case 'n':
                return '\n';
            case 'f':
                return '\f';
            case 'r':
                return '\r';
            case '\"':
            case '\'':
            case '\\':
                return ch;

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
                {
                int cMaxDigits = (ch > '3' ? 2 : 3);
                int nChar = 0;
                do
                    {
                    nChar = (nChar * 8) + (ch - '0');
                    ch = m_script.nextChar();
                    }
                while (ch >= '0' && ch <= '7' && --cMaxDigits > 0);
                m_script.putBackChar();
                ch = (char) nChar;
                }
                return ch;

            case '\r':
            case '\n':
                m_script.putBackChar();
                logError(ERROR, ERR_NEWLINE_IN_LIT, null, m_script.getLine(), m_script.getOffset(), 0);
                return ' ';

            default:
                {
                // back up temporarily to determine the exact line number and
                // position of the invalid escaped character
                int ofNext = m_script.getOffset();
                m_script.putBackChar();
                // log the error (including the parameter)
                logError(ERROR, ERR_CHAR_ESCAPE, new String[]
                        {LiteralToken.printableChar(ch)}, m_script.getLine(),
                        m_script.getOffset(), ofNext - m_script.getOffset());
                m_script.nextChar();
                }
                return ' ';
            }
        }

    /**
    * Eats a numeric literal.  Numeric literals are either integral or
    * floating point.  Integral literals are either int or long; floating
    * point literals are either float or double.  This method first attempts
    * to parse the numeric literal as an integer.  While doing so, if it
    * detects that the literal is floating point, it backs up and parses the
    * number as a floating point literal.
    *
    * @exception CompilerException If a lexical error is encountered which
    *            should stop compilation (like a full error list)
    * @exception EOFException Unexpected end-of-file
    */
    protected Token eatNumericLiteral()
            throws CompilerException, IOException
        {
        ParsePosition pos = m_script.savePosition();

        Token tok = eatIntegralLiteral();
        if (tok == null)
            {
            m_script.restorePosition(pos);
            tok = eatFloatingPointLiteral();
            }

        return tok;
        }

    /**
    * Eats an integral numeric literal.  If the number is an integral number
    * but contains a lexical error, then a token representing the integral 0
    * is returned.  If the number is determined to not be an integral (ie. it
    * appears to be a floating point number), then null is returned.
    *
    * @return Either an integral token or null if the number is appears to be
    *         a floating point number
    *
    * @exception CompilerException If a lexical error is encountered which
    *            should stop compilation (like a full error list)
    * @exception EOFException Unexpected end-of-file
    */
    protected Token eatIntegralLiteral()
            throws CompilerException, IOException
        {
        int iLine    = m_script.getLine();
        int ofInLine = m_script.getOffset();

        // get the first character of the literal; the first character
        // is the best indicator of the type of the number (integer vs.
        // float, and hex vs. octal vs. decimal for integers).
        char ch = m_script.nextChar();
        if (ch == '0')
            {
            // hex and octal numbers start with a leading 0, but a float can
            // also start with a leading zero and there is such a number as
            // the integral zero by itself
            ch = m_script.nextChar();
            switch (ch)
                {
                case 'X': case 'x':
                    {
                    // build a string containing the hex integer
                    StringBuffer sb = new StringBuffer();
                    boolean fLeadZero  = true;
                    boolean fHasDigits = false;
                    ch = m_script.nextChar();
                    while (isHex(ch))
                        {
                        if (ch != '0')
                            {
                            fLeadZero = false;
                            }
                        if (!fLeadZero)
                            {
                            sb.append(ch);
                            }
                        fHasDigits = true;
                        ch = m_script.nextChar();
                        }

                    // the character following the hex integer could
                    // specify that the number is a long integer
                    boolean fLong = (ch == 'L' || ch == 'l');
                    if (!fLong)
                        {
                        m_script.putBackChar();
                        }

                    // check for an error
                    String s = sb.toString();
                    if (!fHasDigits || s.length() > (fLong ? 16 : 8))
                        {
                        String sCode = (fHasDigits ? ERR_NUMERIC_RANGE : ERR_NUMERIC_FORMAT);
                        logError(ERROR, sCode, new String[] {s}, iLine,
                                ofInLine, m_script.getOffset() - ofInLine);
                        }
                    else if (s.length() > 0)
                        {
                        // the string s contains a valid non-zero hex number;
                        // convert it to an integer
                        try
                            {
                            char[] ach = s.toCharArray();
                            if (fLong)
                                {
                                long lValue = 0;
                                for (int of = 0, cch = ach.length; of < cch; ++of)
                                    {
                                    lValue = (lValue << 4) + hexValue(ach[of]);
                                    }
                                return new LiteralToken(lValue, iLine,
                                        ofInLine, m_script.getOffset() - ofInLine);
                                }
                            else
                                {
                                int nValue = 0;
                                for (int of = 0, cch = ach.length; of < cch; ++of)
                                    {
                                    nValue = (nValue << 4) + hexValue(ach[of]);
                                    }
                                return new LiteralToken(nValue, iLine,
                                        ofInLine, m_script.getOffset() - ofInLine);
                                }
                            }
                        catch (NumberFormatException e)
                            {
                            internalError();
                            }
                        }

                    // parsing error already logged ... just return zero
                    return new LiteralToken((fLong ? LIT_ZERO_L : LIT_ZERO),
                            iLine, ofInLine, m_script.getOffset() - ofInLine);
                    }

                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    {
                    // either an octal integer or a float
                    StringBuffer sb = new StringBuffer();
                    boolean fLong     = false;
                    boolean fLeadZero = true;
                    while (ch >= '0' && ch <= '7')
                        {
                        if (ch != '0')
                            {
                            fLeadZero = false;
                            }
                        if (!fLeadZero)
                            {
                            sb.append(ch);
                            }
                        ch = m_script.nextChar();
                        }

                    switch (ch)
                        {
                        case '8': case '9':
                        case 'E': case 'e':
                        case 'F': case 'f':
                        case 'D': case 'd':
                        case '.':
                            // the numeric literal is actually a float
                            return null;

                        case 'L': case 'l':
                            m_script.putBackChar();
                            fLong = true;
                            break;
                        }

                    // check if the octal number exceeds the limits
                    String s = sb.toString();
                    if (s.length() >= (fLong ? 22 : 11) &&
                            (s.length() > (fLong ? 22 : 11) || s.charAt(0) > '1'))
                        {
                        logError(ERROR, ERR_NUMERIC_RANGE, new String[] {s},
                                iLine, ofInLine, m_script.getOffset() - ofInLine);
                        }
                    else if (s.length() > 0)
                        {
                        // the string s contains a valid non-zero hex number;
                        // convert it to an integer
                        try
                            {
                            if (fLong)
                                {
                                return new LiteralToken(Long.parseLong(s, 8), iLine,
                                        ofInLine, m_script.getOffset() - ofInLine);
                                }
                            else
                                {
                                return new LiteralToken(Integer.parseInt(s, 8), iLine,
                                        ofInLine, m_script.getOffset() - ofInLine);
                                }
                            }
                        catch (NumberFormatException e)
                            {
                            internalError();
                            }
                        }

                    // parsing error already logged ... just return zero
                    return new LiteralToken((fLong ? LIT_ZERO_L : LIT_ZERO),
                            iLine, ofInLine, m_script.getOffset() - ofInLine);
                    }

                case '8': case '9':
                case 'E': case 'e':
                case 'F': case 'f':
                case 'D': case 'd':
                case '.':
                    // floating point number
                    return null;

                case 'L': case 'l':
                    // the long integer numeric literal "0"
                    return new LiteralToken(LIT_ZERO_L, iLine, ofInLine,
                            m_script.getOffset() - ofInLine);

                default:
                    // the integer numeric literal "0"
                    m_script.putBackChar();
                    return new LiteralToken(LIT_ZERO, iLine, ofInLine,
                            m_script.getOffset() - ofInLine);
                }
            }
        // either a decimal integer or a float
        else if (ch >= '1' && ch <= '9')
            {
            // decimal integers are constructed as negative numbers in case
            // of the MIN_VALUE problem
            // (see the Java language specification 3.10.4)
            StringBuffer sb = new StringBuffer();
            sb.append('-');
            while (ch >= '0' && ch <= '9')
                {
                sb.append(ch);
                ch = m_script.nextChar();
                }

            boolean fLong = false;
            switch (ch)
                {
                case 'E': case 'e':
                case 'F': case 'f':
                case 'D': case 'd':
                case '.':
                    // the first non-decimal character shows that the
                    // numeric literal is actually a float
                    return null;

                case 'L': case 'l':
                    // the decimal number is a long integer
                    fLong = true;
                    break;

                default:
                    m_script.putBackChar();
                }

            // check if the decimal number exceeds the limits
            String s    = sb.toString();
            String sMax = (fLong ? MAX_LONG : MAX_INT);
            if (s.length() >= sMax.length() &&
                    (s.length() > sMax.length() || s.compareTo(sMax) > 0))
                {
                logError(ERROR, ERR_NUMERIC_RANGE, new String[] {s},
                        iLine, ofInLine, m_script.getOffset() - ofInLine);
                }
            else
                {
                // the string s contains a valid non-zero decimal number;
                // convert it to an integer (and store it as a negative)
                try
                    {
                    if (fLong)
                        {
                        return new LiteralToken(Long.parseLong(s, 10), true,
                                iLine, ofInLine, m_script.getOffset() - ofInLine);
                        }
                    else
                        {
                        return new LiteralToken(Integer.parseInt(s, 10), true,
                                iLine, ofInLine, m_script.getOffset() - ofInLine);
                        }
                    }
                catch (NumberFormatException e)
                    {
                    internalError();
                    }
                }

            // parsing error already logged ... just return zero
            return new LiteralToken((fLong ? LIT_ZERO_L : LIT_ZERO),
                    iLine, ofInLine, m_script.getOffset() - ofInLine);
            }

        // otherwise, assume that it is a floating point number
        return null;
        }

    /**
    * Eats a floating point literal.  Floating point literals are composed of
    * five parts, most of which are optional:  digits, dot, digits, exponent,
    * and suffix.
    *
    * @exception CompilerException If a lexical error is encountered which
    *            should stop compilation (like a full error list)
    * @exception EOFException Unexpected end-of-file
    */
    protected Token eatFloatingPointLiteral()
            throws CompilerException, IOException
        {
        int iLine    = m_script.getLine();
        int ofInLine = m_script.getOffset();

        boolean fLeadingDigits  = false;
        boolean fDot            = false;
        boolean fTrailingDigits = false;
        boolean fExponent       = false;
        boolean fExponentDigits = false;
        boolean fSuffix         = false;
        boolean fFloatNotDouble = false;

        StringBuffer sb = new StringBuffer();

        // (1) leading digits
        char ch = m_script.nextChar();
        while (isDecimal(ch))
            {
            fLeadingDigits = true;
            sb.append(ch);
            ch = m_script.nextChar();
            }

        // (2) dot
        if (ch == '.')
            {
            fDot = true;
            sb.append(ch);
            ch = m_script.nextChar();
            }

        // (3) trailing digits
        while (isDecimal(ch))
            {
            fTrailingDigits = true;
            sb.append(ch);
            ch = m_script.nextChar();
            }

        // (4) exponent
        if (ch == 'E' || ch == 'e')
            {
            fExponent = true;
            sb.append(ch);
            ch = m_script.nextChar();

            // sign of the exponent
            if (ch == '+' || ch == '-')
                {
                sb.append(ch);
                ch = m_script.nextChar();
                }

            // value of the exponent
            while (isDecimal(ch))
                {
                fExponentDigits = true;
                sb.append(ch);
                ch = m_script.nextChar();
                }
            }

        // (5) suffix
        if (ch == 'D' || ch == 'd')
            {
            fSuffix = true;
            }
        else if (ch == 'F' || ch == 'f')
            {
            fSuffix = true;
            fFloatNotDouble = true;
            }
        else
            {
            m_script.putBackChar();
            }

        // now verify that the correct pieces of the floating point
        // literal appeared in the string
        String s = sb.toString();
        if (fTrailingDigits || (fLeadingDigits && (fDot || fExponent || fSuffix)))
            {
            // either there is no exponent or there is an exponent with
            // at least 1 digit
            if (!fExponent || (fExponent && fExponentDigits))
                {
                try
                    {
                    if (fFloatNotDouble)
                        {
                        return new LiteralToken(Float.valueOf(s).floatValue(),
                                iLine, ofInLine, m_script.getOffset() - ofInLine);
                        }
                    else
                        {
                        return new LiteralToken(Double.valueOf(s).doubleValue(),
                                iLine, ofInLine, m_script.getOffset() - ofInLine);
                        }
                    }
                catch (NumberFormatException e)
                    {
                    // error handled below (fall through)
                    }
                }
            }

        // log numeric format error with the invalid number as the parameter
        logError(ERROR, ERR_NUMERIC_FORMAT, new String[] {s},
                iLine, ofInLine, m_script.getOffset() - ofInLine);

        // return a floating point zero
        return new LiteralToken((fFloatNotDouble ? LIT_ZERO_F : LIT_ZERO_D),
                iLine, ofInLine, m_script.getOffset() - ofInLine);
        }

    /**
    * Eats an identifier or a keyword.  Note that this method assumes that
    * it is only called if the next character is a "Java letter".
    *
    * @exception CompilerException If a lexical error is encountered which
    *            should stop compilation (like a full error list)
    * @exception EOFException Unexpected end-of-file
    *
    * @see Character#isJavaIdentifierStart(char)
    * @see Character#isJavaIdentifierPart(char)
    */
    protected Token eatIdentifierOrKeyword()
            throws CompilerException, IOException
        {
        int  iLine    = m_script.getLine();
        int  ofInLine = m_script.getOffset();

        // build the name (identifier, keyword, boolean literal, or null literal)
        StringBuffer sb = new StringBuffer();
        char ch = m_script.nextChar();
        while (Character.isJavaIdentifierPart(ch))
            {
            sb.append(ch);
            ch = m_script.nextChar();
            }
        m_script.putBackChar();

        // check if it is a keyword
        String s   = sb.toString();
        Token  tok = (Token) Token.RESERVED.get(s);

        // 2001.06.29 cp - check if the keyword is a proprietary extension
        if (tok != null && !m_fExtensionsEnabled && tok.getID() == Token.KEY_AS)
            {
            tok = null;
            }

        if (tok == null)
            {
            // must be an identifier
            // see if the name is already registered
            tok = (Token) m_tblNames.get(s);
            if (tok == null)
                {
                // register it as a new name
                tok = new Token(Token.IDENTIFIER, Token.NONE, Token.IDENT, null, s);
                m_tblNames.put(s, tok);
                }
            }

        // return a "location-specific" instance of the token
        return new Token(tok, iLine, ofInLine, m_script.getOffset() - ofInLine);
        }


    // ----- error handling -------------------------------------------------

    /**
    * Logs an internal error and throws a CompilerException.
    *
    * @exception CompilerException Thrown unconditionally
    */
    protected void internalError()
            throws CompilerException
        {
        logError(FATAL, ERR_INTERNAL, null, m_script.getLine(), m_script.getOffset(), 0);
        throw new CompilerException();
        }

    /**
    * Logs the passed error in the error list.
    *
    * @param nSeverity Severity of the error as defined by ErrorList.Constants
    * @param sCode Error code, as defined by the class logging the error
    * @param asParams Replaceable parameters for the error message
    * @param iLine Line number where the error was detected
    * @param ofInLine Offset of the error within the line
    * @param cchText Length of the text which caused the error
    *
    * @exception CompilerException If the error list overflows.
    */
    protected void logError(int nSeverity, String sCode, String[] asParams, int iLine, int ofInLine, int cchText)
            throws CompilerException
        {
        try
            {
            m_errlist.add(new CompilerErrorInfo(nSeverity, sCode, RESOURCES,
                    asParams, iLine, ofInLine, cchText));
            }
        catch (ErrorList.OverflowException e)
            {
            throw new CompilerException();
            }
        }


    // ----- inner classes --------------------------------------------------

    /**
    * Stores all information required to later restore the current position in
    * the script.
    */
    class Position implements ParsePosition
        {
        Tokenizer     toker;
        Dequeue       dq;
        ParsePosition scriptpos;
        }


    // ----- constants ------------------------------------------------------

    // commonly used literal tokens
    public static final LiteralToken LIT_ZERO   = new LiteralToken((int)    0   , -1, -1, -1);
    public static final LiteralToken LIT_ZERO_L = new LiteralToken((long)   0L  , -1, -1, -1);
    public static final LiteralToken LIT_ZERO_F = new LiteralToken((float)  0.0F, -1, -1, -1);
    public static final LiteralToken LIT_ZERO_D = new LiteralToken((double) 0.0D, -1, -1, -1);

    // maximum integral values
    public static final String MAX_INT  = "-2147483648";
    public static final String MAX_LONG = "-9223372036854775808";


    // ----- data members ---------------------------------------------------

    /**
    * The script to parse.
    */
    private Script         m_script;

    /**
    * Parsed names.
    */
    private Hashtable      m_tblNames;

    /**
    * The "put back" queue.
    */
    private Dequeue        m_dq;

    /**
    * The error list to log to.
    */
    private ErrorList      m_errlist;

    /**
    * The option to parse proprietary keyword extensions.
    */
    private boolean        m_fExtensionsEnabled;

    /**
    * The option to parse comments as tokens.
    */
    private boolean        m_fCommentsEnabled;
    }
