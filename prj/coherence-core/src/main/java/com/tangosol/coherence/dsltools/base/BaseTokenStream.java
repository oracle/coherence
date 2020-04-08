/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.base;

/**
* Since BaseTokens can nest, BaseTokenStream creates for clients a utility
* interface that allows streaming (atEnd(), next()) over a CompoundBaseToken.
*
* @author djl  2009.03.14
*/
public class BaseTokenStream
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Constructor a new BaseTokenStream on the CompoundBaseToken.
    *
    * @param token  the CompoundBaseToken to stream over
    */
    public BaseTokenStream(CompoundBaseToken token)
        {
        m_aTokens = token.getTokens();
        m_iPos     = 0;

        m_currentToken = isEnd() ? null : m_aTokens[0];
        }


    // ----- Utility Streaming ----------------------------------------------

    /**
    * Answer a boolean indication as to whether the stream is at the end
    *
    * @return true if streaming is at an end otherwise false
    */
    public boolean isEnd()
        {
        return m_iPos >= m_aTokens.length;
        }

    /**
    * Answer a current BaseToken base on the position of streaming
    *
    * @return the BaseToken at the current stream position
    */
     public BaseToken getCurrentToken()
        {
        return m_currentToken;
        }

    /**
    * Answer the next token in the stream or null if at end.
    * Advance the stream position.
    *
    * @return the next BaseToken in the stream
    */
    public BaseToken next()
        {
        BaseToken[] aTokens = m_aTokens;

        return m_currentToken =
                m_iPos < aTokens.length ? aTokens[m_iPos++] : null;
        }

    /**
    * Answer the next token in the stream or null if at end.
    * Do not advance the stream position
    *
    * @return the next BaseToken in the stream
    */
    public BaseToken peek()
        {
        BaseToken[] aTokens = m_aTokens;
        return m_iPos < aTokens.length ? aTokens[m_iPos] : null;
        }

    /**
    * Answer the next two tokens in the stream or null(s) if at end.
    * Do not advance the stream position
    *
    * @return the next BaseToken in the stream
    */
    public BaseToken[] peek2()
        {
        BaseToken[] aTokens = m_aTokens;
        BaseToken[] aResult = new BaseToken[2];
        aResult[0] = m_iPos   < aTokens.length ? aTokens[m_iPos  ] : null;
        aResult[1] = m_iPos+1 < aTokens.length ? aTokens[m_iPos+1] : null;
        return aResult;
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Remember a BaseTokenStream that can be the target of streaming
    *
    * @param ts  the remembered token stream
    */
    public void setLink(BaseTokenStream ts)
        {
        m_link = ts;
        }

    /**
    * Answer a BaseTokenStream that can become the target of streaming
    *
    * @return the BaseTokenStream that can become the target of streaming
    */
    public BaseTokenStream getLink()
        {
        return m_link;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The array of tokens that the receiver is streaming over
    */
    private BaseToken[] m_aTokens;

    /**
    * The current position in the array of tokens
    */
    private int m_iPos = 0;

    /**
    * A BaseTokenStream that the receiver has "nested" from
    */
    private BaseTokenStream m_link = null;

    /**
    * The current BaseToken to return
    */
    private BaseToken m_currentToken = null;
    }