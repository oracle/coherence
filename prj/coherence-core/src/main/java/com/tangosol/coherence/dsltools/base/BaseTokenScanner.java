/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.base;


import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;


/**
* BaseTokenScanner gives clients a streaming api that returns a next
* BaseToken by processing either a java.lang.String or a java.io.Reader.
* Clients may process the underlying Reader or String all at one time by
* using scan() which will a BaseToken that is typically return a
* SequenceBaseToken. This all at once conversion from Chars to Tokens is
* standard for very low level tokenizers. BaseTokenScanner also processes
* nested tokens (things between (..), {..}, etc.) into a sequence of token
* represented by a composit token.  Nested tokenizing relieves a client
* parser of bracketing concerns. This nest processing comes from the Dylan
* tokenizer.
*
* @author djl  2009.03.02
*/
public class BaseTokenScanner
    {
    // ----- constructors ---------------------------------------------------


    /**
    * Construct a new BaseTokenScanner with the given String.
    *
    * @param s  the string to be tokenized
    */
    public BaseTokenScanner(String s)
        {
        this(new StringReader(s));
        }

    /**
    * Construct a new BaseTokenScanner with the given Reader.
    *
    * @param reader  the Reader that is the source of chars to be tokenized
    */
    public BaseTokenScanner(Reader reader)
        {
        this.m_reader = reader;
        }


    // ----- Public BaseTokenScanner API ------------------------------------


    /**
    * Tokenize the entire expression at once.
    *
    * @return a BaseToken, typically a SequenceBaseToken
    */
    public BaseToken scan()
        {
        List listTokens = new ArrayList();

        try
            {
            reset();
            while (!isEnd())
                {
                BaseToken token = next();
                if (token != null)
                    {
                    listTokens.add(token);
                    }
                }
            return new SequenceBaseToken((BaseToken[])
                    listTokens.toArray(new BaseToken[listTokens.size()]));
            }
        finally
            {
            try
                {
                m_reader.close();
                }
            catch (IOException e)
                {
                }
            }
        }

    /**
    * Reset the receiver so that tokenizing may begin again.
    */
    public void reset()
        {
        m_fIsEnd = true;
        m_iPos        = 0;
        m_chCurrent = (char) 0;

        try
            {
            int ch = m_reader.read();
            if (ch != -1)
                {
                m_chCurrent = (char) ch;
                m_fIsEnd = false;
                }
            }
        catch (IOException e)
            {
            }
        }

    /**
    * Answer the next token from the underlying reader.
    *
    * @return the next token
    */
    public BaseToken next()
        {
        try
            {
            resetTokenString();
            skipWhiteSpace();
            if (isEnd())
                {
                return null;
                }
            notePos();
            char ch = getCurrentChar();
            if (isPunctuation(ch))
                {
                takeCurrentCharAndAdvance();
                return new PunctuationBaseToken(tokenString());
                }
            else if (isNest(ch))
                {
                advance();
                return scanNest(ch);
                }
            else
                {
                BaseToken oToken = scanLiteral();
                if (oToken != null)
                    {
                    return oToken;
                    }
                oToken = scanIdentifier();
                if (oToken != null)
                    {
                    return oToken;
                    }

                if (scanOperator())
                    {
                    return new OperatorBaseToken(tokenString());
                    }
                else
                    {
                    throw new BaseTokenScannerException(
                       "Unclassifiable char '"+
                           ch + "'"+ " : " + Character.getNumericValue(ch) +
                    " at line " + m_lineNumber + " offset "+ m_coffset);
                    }
                }
            }
        catch (IndexOutOfBoundsException ex)
            {
            throw new BaseTokenScannerException(
                    "Indexing problem " + ex.getMessage());
            }
        }

    /**
    * Skip over any whitespace characters.
    */
    public void skipWhiteSpace()
        {
        while (!isEnd() && Character.isWhitespace(getCurrentChar()))
            {
            advance();
            }
        }

    /**
    * Test whether the receiver has reached the end of the underlying Reader
    *
    * @return the boolean result
    */
    public boolean isEnd()
        {
        return m_fIsEnd;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Set the String used to determine punctuation characters.
    *
    * @param s  the String of characters to use as punctuation
    */
    public void setPunctuation(String s)
        {
        m_sPunctuation = s;
        }

    /**
    * Set the Strings used as nesting characters.
    *
    * @param sNests    the chars that start nesting
    * @param sUnnests  the chars that end nesting
    */
    public void setNesting(String sNests, String sUnnests)
        {
        m_sNests   = sNests;
        m_sUnnests = sUnnests;
        }


    // ----- Internal Worker Methods ----------------------------------------

     /**
     * Test if the given char is a punctuation character.
     *
     * @param ch  the char to test
     *
     * @return  the boolean result of punctuation testing
     */
     protected boolean isPunctuation(char ch)
        {
        return m_sPunctuation.indexOf(ch) >= 0;
        }

    /**
    * Test if the given char is a character that starts nesting/
    *
    * @param ch the char to test
    *
    * @return  the boolean result of nest testing
    */
    protected boolean isNest(char ch)
        {
        return m_sNests.indexOf(ch) >= 0;
        }


    /**
    * Tokenize the characters between the beginning nest chararcer
    * and the character that ends the nest.
    *
    * @param ch  the character that begins nesting
    *
    * @return a NestedBaseTokens that holds the nested tokens
    *
    * @throws  BaseTokenScannerException if we reach the end of stream
    *          before the matching end of nest character is reached
    */
    protected BaseToken scanNest(char ch)
        {
        ArrayList otoks       = new ArrayList();
        char      unnestChar = m_sUnnests.charAt(m_sNests.indexOf(ch));
        skipWhiteSpace();
        while (getCurrentChar() != unnestChar)
            {
            if (isEnd())
                {
                 throw new BaseTokenScannerException(
                         "Unexpected end of stream while looking for a "+
                                 unnestChar);
                }
            BaseToken oToken = next();
            if (oToken != null)
                {
                otoks.add(oToken);
                }
            skipWhiteSpace();
            }
        advance();
        BaseToken[] aoTok = new BaseToken[otoks.size()];
        for (int i = 0, c = aoTok.length; i < c; ++i)
            {
            aoTok[i] = (BaseToken) otoks.get(i);
            }
        return new NestedBaseTokens(ch, unnestChar, aoTok);
        }

    /**
    * Attemt to tokenize a literal.
    *
    * @return  a LiteralBaseToken if one is found otherwise return null
    */
    protected BaseToken scanLiteral()
        {
        String st;
        char ch = getCurrentChar();

        if (ch == '"' || ch == '\'')
            {
            advance();
            notePos();
            while (getCurrentChar() != ch)
                {
                 if (isEnd())
                    {
                    throw new BaseTokenScannerException(
                       "Unexpected end of input while processing string literal"+
                       " at line " + m_lineNumber + " offset "+ m_coffset);
                    }
                else
                    {
                    takeCurrentCharAndAdvance();
                    }
                }
            st = tokenString();
            if (!isEnd())
                {
                advance();
                }
            return LiteralBaseToken.createString(st);
            }
        else if (Character.isDigit(ch))
            {
            notePos();
            if (!isEnd())
                {
                takeCurrentCharAndAdvance();
                }

            while (!isEnd() && Character.isDigit(getCurrentChar()))
                {
                takeCurrentCharAndAdvance();
                }
            if (isEnd())
                {
                return LiteralBaseToken.createInteger(tokenString());
                }
            if (getCurrentChar() == '.')
                {
                return literalFloat();
                }
            else
                {
                st = tokenString();
                if (!isEnd())
                    {
                    char cc = getCurrentChar();
                    if (cc  == 'l' || cc == 'L')
                        {
                        advance();
                        return LiteralBaseToken.createLong(st);
                        }
                    else if (cc == 's' || cc == 'S')
                            {
                            advance();
                            return LiteralBaseToken.createShort(st);
                            }
                    else if (cc == 'd' || cc == 'D')
                            {
                            advance();
                            return LiteralBaseToken.createDouble(st+".0");
                            }
                    else if (cc == 'f' || cc == 'F')
                            {
                            advance();
                            return LiteralBaseToken.createFloat(st+".0");
                            }
                    }
                return LiteralBaseToken.createInteger(st);
                }
            }
        return null;
        }

    /**
    * A floating point literal has been detected so create.
    *
    * @return  the literal representation of a floating point number
    */
     protected LiteralBaseToken literalFloat()
        {
        String st;
        char ch;
        takeCurrentCharAndAdvance();
        if (!isEnd() && Character.isDigit(getCurrentChar()))
            {
            takeCurrentCharAndAdvance();
            }
        else
            {
            throw floatingPointFormatError();
            }
        while (!isEnd() && Character.isDigit(getCurrentChar()))
            {
            takeCurrentCharAndAdvance();
            }
        ch = getCurrentChar();
        if ( ch == 'E' || ch == 'e')
            {
            takeCurrentCharAndAdvance();
            if (!isEnd() && getCurrentChar() == '-')
                {
                takeCurrentCharAndAdvance();
                if (!isEnd() && Character.isDigit(getCurrentChar()))
                    {
                    takeCurrentCharAndAdvance();
                    }
                else
                    {
                    throw floatingPointFormatError();
                    }
                }
            else if (!isEnd() && Character.isDigit(getCurrentChar()))
                {
                takeCurrentCharAndAdvance();
                }
            else
                {
                throw floatingPointFormatError();
                }
            while (!isEnd() && Character.isDigit(getCurrentChar()))
                {
                takeCurrentCharAndAdvance();
                }
            }
        st = tokenString();
        if (!isEnd())
            {
            char cc = getCurrentChar();
            if (cc == 'd' || cc == 'D')
                {
                advance();
                return LiteralBaseToken.createDouble(st);
                }
            else if (cc == 'f' || cc == 'F')
                {
                advance();
                return LiteralBaseToken.createFloat(st);
                }
            }
        return LiteralBaseToken.createDouble(st);
        }

    /**
    * Attemt to tokenize an Identifier.
    *
    * @return  an IdentifierBaseToken if one is found otherwise return null
    */
    protected BaseToken scanIdentifier()
        {
        if (Character.isJavaIdentifierStart(getCurrentChar()))
            {
            takeCurrentCharAndAdvance();
            while (!isEnd() && Character.isJavaIdentifierPart(getCurrentChar()))
                {
                takeCurrentCharAndAdvance();
                }
            return new IdentifierBaseToken(tokenString());

            }
        else
            {
            return null;
            }
        }

    /**
    * A problem has been detected in a floating point number.  Signal
    * an error.
    * 
    * @return the RuntimeException for float format errors
    */
    protected RuntimeException floatingPointFormatError()
        {
        return new BaseTokenScannerException(
          "Invalid floating point format "+  tokenString() +
                  " at line " + m_lineNumber +
                  " offset " + m_coffset);
        }

    /**
    * Attemt to tokenize an Operator.
    *
    * @return  an OperatorBaseToken if one is found otherwise return null
    */
    protected boolean scanOperator()
        {
        // ToDo This could be much less with some ascii tests
        char ch = getCurrentChar();
        switch (ch)
            {
            case '@':
            case '?':
            case ';':
            case '+':
            case '-':
                takeCurrentCharAndAdvance();
                return true;

            case '*':
                takeCurrentCharAndAdvance();

                if (ch == getCurrentChar())
                    {
                    takeCurrentCharAndAdvance();
                    }
                return true;

            case '/':
                takeCurrentCharAndAdvance();
                return true;

            case '^':
            case '&':
            case '|':
                takeCurrentCharAndAdvance();
                if (ch == getCurrentChar())
                    {
                    takeCurrentCharAndAdvance();
                    }
                return true;

            case '<':
                takeCurrentCharAndAdvance();
                if ('=' == getCurrentChar() || '>' == getCurrentChar())
                    {
                    takeCurrentCharAndAdvance();
                    }
                return true;


            case ':':
                takeCurrentCharAndAdvance();
                if ('=' == getCurrentChar())
                    {
                    takeCurrentCharAndAdvance();
                    }
                return true;

            case '>':
                takeCurrentCharAndAdvance();
                if ('=' == getCurrentChar())
                    {
                    takeCurrentCharAndAdvance();
                    }
                return true;

            case '=':
                takeCurrentCharAndAdvance();
                if (ch == getCurrentChar())
                    {
                    takeCurrentCharAndAdvance();
                    }
                return true;

            case '~':
                takeCurrentCharAndAdvance();
                if (getCurrentChar() == '=')
                    {

                    }
                return true;

            case '!':
                takeCurrentCharAndAdvance();
                if (getCurrentChar() == '=')
                    {
                    takeCurrentCharAndAdvance();
                    }
                return true;
            default:
                return false;
            }
        }


    /**
    * Answer the current char of the underlying Reader
    *
    * @return the current char
    */
    protected char getCurrentChar()
        {
        return m_chCurrent;
        }

    /**
    * Advance to the next character.
    */
    protected void advance()
        {
        try
            {
            int nCh = m_reader.read();
            if (nCh != -1)
                {
                ++m_iPos;
                m_chCurrent = (char) nCh;
                m_fIsEnd = false;
                m_coffset++;
                if (m_chCurrent == '\n')
                    {
                    m_lineNumber++;
                    m_coffset = 1;
                    }
                }
            else
                {
                m_chCurrent = (char) 0;
                m_fIsEnd = true;
                }
            }
        catch (IOException e)
            {
            m_fIsEnd = true;
            }
        }

    /**
     * Advance to the next character and return it.
     *
     * @return the next character
     */
    protected char nextChar()
        {
        advance();
        return getCurrentChar();
        }

    /**
    * Note the offset within the underlying Reader
    */
    protected void notePos()
        {
        m_iStartPos = m_iPos;
        }

    /**
    * Convert the buffered characters into a String representing a token.
    *
    * @return  the String representing the buffered token
    */
    protected String tokenString()
        {
        return m_tokenBuffer.toString();
        }

    /**
    * Reset the buffer used for holding the current token
    */
    protected void resetTokenString()
        {
        m_tokenBuffer.setLength(0);
        }

    /**
    * Add the current character to buffer that builds the current token.
    */
    protected void takeCurrentChar()
        {
        m_tokenBuffer.append(getCurrentChar());
        }

    /**
    * Add the current character to buffer that builds the current token and
    * advance.
    */
    protected void takeCurrentCharAndAdvance()
        {
        m_tokenBuffer.append(getCurrentChar());
        advance();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The current position in the underlying Reader.
    */
    protected int m_iPos = 0;

    /**
    * The saved start position for a token.
    */
    protected int m_iStartPos = 0;

    /**
    * The characters used for punctuation.
    */
    protected String m_sPunctuation = ".;,";

    /**
    * The characters used to begin nesting.
    */
    protected String m_sNests = "([{";

    /**
    * The characters used to end nesting.
    */
    protected String m_sUnnests = ")]}";

    /**
    * The temporary buffer used to build the String representing a token.
    */
    protected StringBuffer m_tokenBuffer = new StringBuffer();

    /**
    * The current working char.
    */
    protected char m_chCurrent;

    /**
    * The underlying Reader holding the characters being tokenized.
    */
    protected Reader m_reader;

    /**
    * The flag set when end of file is detected.
    */
    protected boolean m_fIsEnd = true;

    /**
    * Offset of the current character.
    */
    protected int m_coffset = 1;

    /**
    * The current line number.
    */
    protected int m_lineNumber = 1;
    }