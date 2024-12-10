/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.base;


/**
* SequenceBaseToken is a token that holds a sequence of tokens. This nesting
* of tokens makes many algorithms easier in the higher levels of a parser.
*
* @author djl  2009.03.14
*/
public class SequenceBaseToken
        extends CompoundBaseToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new SequenceBaseToken with the array of tokens.
    *
    * @param aTokens  an array of BaseTokens
    */
    public SequenceBaseToken(BaseToken[] aTokens)
        {
        m_aTokens = aTokens;
        }


    // ----- Compound BaseToken interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    public BaseToken[] getTokens()
        {
        return m_aTokens;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The array of tokens for this sequence of tokens
    */
    private BaseToken[] m_aTokens;
    }
