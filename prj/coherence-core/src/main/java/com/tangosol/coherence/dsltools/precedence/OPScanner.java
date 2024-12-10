/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


import com.tangosol.coherence.dsltools.base.*;

import java.io.Reader;
import java.io.StringReader;
import java.util.Set;


/**
* OPScanner gives clients a streaming api that returns a next
* OPToken by processing a java.lang.String using a TokenTable to convert
* lower level BaseTokens into the high functionality OPTokens. Since
* BaseTokens support a composite sequence token OPScanner supports nested
* scanning.
*
* @author djl  2009.03.02
*/
public class OPScanner
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new OPScanner using the given TokenTable.
    *
     * @param tokenTable    the TokenTable that defines the mapping
     *                      from BaseTokens
     * @param setOperators  the set of operators to use
     */
    public OPScanner(TokenTable tokenTable, Set<CharSequence> setOperators)
        {
        m_tokenTable   = tokenTable;
        m_setOperators = setOperators;
        }

    // ----- initialization API ---------------------------------------------

    /**
    * Initialize a new OPScanner to process the given String and load first
    * token into current.
    *
    * @param s   the String to convert to a stream of tokens
    */
    public void scan(String s)
        {
        scan(new StringReader(s));
        }

    /**
    * Initialize a new OPScanner to process the given Reader and load first
    * token into current.
    *
    * @param r   the Reader to use as source for a stream of tokens
    */
    public void scan(Reader r)
        {
        BaseTokenScanner baseScanner = new BaseTokenScanner(r);
                         m_data      = new BaseTokenStream(
                                 (SequenceBaseToken) baseScanner.scan());
        next();
        }

    /**
    * Reset this scanner to process the current BaseTokenStream
    *
    */
    public void reset()
        {
        m_current = m_data.getCurrentToken();
        classify();
        }

    /**
    * The given flag determines the strictness in that an unknown token
    * throws an exception otherwise they are turned into an identifier.  The
    * default setting is false.
    *
    * @param fStrict boolean to control the strictness
    */
    public void setStrictness(boolean fStrict)
        {
        m_fStrict = fStrict;
        }


    // ----- OPScanner Streaming API ----------------------------------------

    /**
    * Move to the next token in the stream and return it.
    *
    * @return the current token
    */
      public OPToken next()
        {
        m_current = m_data.next();
        return classify();
        }

    /**
    * If the Scanner is at the end answer true otherwise answer false.
    *
    * @return the boolean that answers as to whether the stream is atEnd
    */
    public boolean isEnd()
        {
        return m_current == null;
        }

    /**
     * If the scanner is at the end of a statement return true.
     * A statement end is signified by either a semi-colon or the
     * end of the token stream.
     *
     * @return true if the scanner is at the end of a statement
     */
    public boolean isEndOfStatement()
        {
        return m_current == null || m_current.match(EndOfStatementOPToken.INSTANCE.getValue());
        }

    /**
    * Answer the current OPToken.
    *
    * @return the current token
    */
    public OPToken getCurrent()
        {
        return m_currentToken;
        }

    /**
    * Answer the current BaseToken.
    *
    * @return the current BaseToken
    */
    public BaseToken getCurrentBaseToken()
        {
        return m_current;
        }

    /**
    * Answer the string representation of the current BaseToken.
    *
    * @return the string representation of the current BaseToken
    */
    public String getCurrentAsString()
        {
        if (m_current == null || m_current.isCompound())
            {
            return null;
            }
        return ((LeafBaseToken) m_current).getValue();
        }

    /**
    * Answer the string representation of the next BaseToken.
    *
    * @return the string representation of the next BaseToken
    */
    public String peekNextAsString()
        {
        BaseToken token = m_data.peek();
        if (token == null || token.isCompound())
            {
            return null;
            }
        return ((LeafBaseToken) token).getValue();
        }

    /**
    * Answer the string representation of the next BaseToken.
    *
    * @return the string representation of the next BaseToken
    */
    public BaseToken peekNext()
        {
        return m_data.peek();
        }

    /**
    * Answer the String[] representation of the next two BaseTokens.
    *
    * @return the String[] representation of the next BaseToken
    */
    public String[] peekNext2AsStrings()
        {
        BaseToken[] tokens = m_data.peek2();
        if (tokens[0] == null || tokens[0].isCompound())
            {
            return null;
            }
        if (tokens[1] == null || tokens[1].isCompound())
            {
            return null;
            }

        return new String[] {
                ((LeafBaseToken) tokens[0]).getValue(),
                ((LeafBaseToken) tokens[1]).getValue()};
        }


    /**
    * Answer the string representation of the current BaseToken and advance.
    *
    * @return the string representation of the current BaseToken
    */
    public String getCurrentAsStringWithAdvance()
        {
        String s = getCurrentAsString();
        next();
        return s;
        }


    // ----- Public OPScanner API -------------------------------------------

    /**
    * Test to see if the current BaseToken's string matches the given string.
    * The token table's ignoringCase flag is consulted to see if case matters.
    *
    * @param sWord  the string to match with
    *
    * @return a boolean indication of the success of the match
    */
    public boolean matches(String sWord)
        {
        return matches(sWord, m_tokenTable.isIgnoringCase());
        }

    /**
    * Test to see if the current BaseToken's string matches the given string.
    * The given flag controls whether case is interesting.
    *
    * @param sWord        the string to match with
    * @param fIgnoreCase  the flag that indicates if case is interesting
    *
    *
    * @return a boolean indication of the success of the match
    */
    public boolean matches(String sWord,boolean fIgnoreCase)
        {
        return m_current != null &&
                m_current.match(sWord, fIgnoreCase);
        }


    /**
    * Test to see if the current BaseToken's string matches the given string
    * and advance if true. * The token table's ignoringCase flag is consulted
    *  to see if case matters.
    *
    * @param sWord  the string to match with
    *
    * @return a boolean indication of the success of the match
    */
    public boolean advanceWhenMatching(String sWord)
        {
        boolean f = matches(sWord);
        if (f)
            {
            next();
            }
        return f;
        }

    /**
    * Advance to the next token and expect it to match the given string.
    * If expectations are not met then throw an Exception. The token table's
    *  ignoringCase flag is consulted to see if case matters.
    *
    * @param s  the string that should match the next token
    *
    * @return the next OPToken
    */
    public OPToken advance(String s)
        {
        return advance(s, m_tokenTable.isIgnoringCase());
        }
    /**
    * Advance to the next token and expect it to match the given string.
    * If expectations are not met then throw an Exception.  The given flag
    * controls whether case is interesting.
    *
    * @param s  the string that should match the next token
    * @param fIgnoreCase  the flag that indicates if case is interesting
    *
    * @return the next OPToken
    */
    public OPToken advance(String s, boolean fIgnoreCase)
        {
        if (matches(s,fIgnoreCase))
            {
            return next();
            }
        else
            {
            throw new OPException("Unfullfilled expectation \""+
                    s + "\" not found!");
            }
        }

    /**
    * Advance to the next token.
    *
    * @return the next OPToken
    */
     public OPToken advance()
        {
        return next();
        }

    /**
    * Remember the current BaseTokenStream and stream over the given stream.
    *
    * @param ts  the BaseTokenStream to now stream over
    */
    public void pushStream(BaseTokenStream ts)
        {
        ts.setLink(m_data);
        m_data = ts;
        next();
        }

    /**
    * Restore the remembered BaseTokenStream as the source of streaming.
    */
     public void popStream()
        {
        BaseTokenStream old    = m_data;
                        m_data = m_data.getLink();
        old.setLink(null);
        reset();
        }

    /**
    * Enable the token named by the given string.
    *
    * @param name  the name of the OPToken to enable
    */
    public void enableTokenNamed(String name)
        {
        m_tokenTable.enable(name);
        }

    /**
    * Disable the token named by the given string.
    *
    * @param name  the name of the OPToken to disable
    */
    public void disableTokenNamed(String name)
        {
        m_tokenTable.disable(name);
        }


    // ----- helper functions  ----------------------------------------------

    /**
    * Figure out how to map the current BaseToken to a OPToken and return it.
    *
    * @return the current OPToken
    */
    protected OPToken classify()
        {
        try
            {
            if (m_current == null)
                {
                return m_currentToken = EndOPToken.INSTANCE;
                }
            if (m_current.isNest())
                {
                NestedBaseTokens nt    = (NestedBaseTokens) m_current;
                String           start =
                        Character.toString(nt.getNestStart());
                NestingOPToken orig    =
                        (NestingOPToken) m_tokenTable.lookup(start);
                if (orig == null)
                    {
                    throw new OPException("Unknown Nesting character '"+
                            start + "' found!");
                    }
                try {
                  NestingOPToken nestTok = (NestingOPToken) orig.clone();
                  return m_currentToken  = nestTok.setNest(nt);
                } catch (CloneNotSupportedException ex)
                    {
                    throw new OPException("Unexpected Nesting Scanning Failure :" +
                                ex.getMessage());
                    }
                }

            if (m_current.isPunctuation())
                {
                PunctuationBaseToken ptoken =
                        (PunctuationBaseToken) m_current;

                m_currentToken = m_tokenTable.lookup(ptoken.getValue());
                if (m_currentToken == null)
                    {
                    if (m_fStrict)
                        {
                         throw new OPException("Unknown punctuation \""+
                            ptoken.getValue() + "\" not found!");
                        }
                    else
                        {
                        m_currentToken =
                              m_tokenTable.newIdentifier(ptoken.getValue());
                        }
                    }
                return m_currentToken;
                }

            if (m_current.isOperator())
                {
                OperatorBaseToken otoken =(OperatorBaseToken) m_current;

                m_currentToken = m_tokenTable.lookup(otoken.getValue());
                if (m_currentToken == null)
                    {
                    if (m_fStrict)
                        {
                        throw new OPException("Unknown Operator \""+
                           otoken.getValue() + "\" not found!");
                        }
                    else
                        {
                        m_currentToken =
                               m_tokenTable.newIdentifier(otoken.getValue());
                        }
                    }
                return m_currentToken;
                }
            if (m_current.isLiteral())
                {
                LiteralBaseToken bt = (LiteralBaseToken) m_current;

                m_currentToken =
                        m_tokenTable.newLiteral(bt.getValue(), bt.getType());
                return m_currentToken;
                }
            if (m_current.isIdentifier())
                {
                String id = ((IdentifierBaseToken) m_current).getValue();

                m_currentToken = m_tokenTable.lookup(id);
                if (m_currentToken != null)
                    {
                    return m_currentToken;
                    }
                m_currentToken = m_tokenTable.newIdentifier(id);
                return m_currentToken;
                }

            throw new OPException("Scanner Classification failure on : " +
                    m_current.toString());

            }
        catch (IndexOutOfBoundsException ex)
            {
            m_current      = null;
            m_currentToken = EndOPToken.INSTANCE;
            return m_currentToken;
            }
        }

    // ----- data members ---------------------------------------------------

    /**
    * The source of BaseTokens to process
    */
    protected BaseTokenStream m_data;

    /**
    * The current BaseTokens from data
    */
    protected BaseToken m_current;

    /**
    * The current OPToken translated from current
    */
    protected OPToken m_currentToken;

    /**
    * The TokenTable that defines the translation from BaseTokens to OPTokens
    */
    protected TokenTable m_tokenTable;

    /**
     * The set of valid operators used by this scanner
     */
    protected Set<CharSequence> m_setOperators;

    /**
    * If strict then unknown tokens throw an exception otherwise
    * they are turned into an identifier
    */
    boolean m_fStrict = false;

    }