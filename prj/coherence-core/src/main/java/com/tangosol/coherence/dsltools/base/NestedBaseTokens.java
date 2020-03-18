/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.base;


/**
* NestedBaseTokens is a token that holds a sequence of tokens as well as the
* two bracketing characters. This nesting of tokens makes many algorithms
* easier in the higher levels of a parser.
*
* @author djl  2009.03.14
*/
public class NestedBaseTokens
        extends CompoundBaseToken
    {
    // ----- constructors ---------------------------------------------------

     /**
     * Construct a new NestedBaseTokens with the given parameters.
     *
     * @param chStart  the character that starts the nesting
     * @param chEnd    the character that ends the nesting
     * @param aTokens   the right argument for the node
     */
     public NestedBaseTokens(char chStart, char chEnd,
             BaseToken[] aTokens)
        {
        m_aTokens = aTokens;
        m_nestStart = chStart;
        m_nestEnd   = chEnd;
        }


    // ----- CompoundBaseToken interface ------------------------------------

    /**
    * {@inheritDoc}
    */
    public BaseToken[] getTokens()
        {
        return m_aTokens;
        }


    // ----- BaseToken interface --------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean isNest()
        {
        return true;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the character that begins the nesting.
    *
    * @return the character that begings the nesting
    */
    public char getNestStart()
        {
        return m_nestStart;
        }

    /**
     * Obtain the character that ends the nesting.
     *
     * @return the character that ends the nesting
     */
    public char getNestEnd()
        {
        return m_nestEnd;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this token.
    *
    * @return a String description of the token
    */
    public String toString()
        {
        String      s        = "Nest" + m_nestStart;
        BaseToken[] aTokens = getTokens();

        for (int i = 0, c = aTokens.length; i < c; ++i)
            {
            s = s + aTokens[i];
            if (i != c - 1)
                {
                s = s + ", ";
                }
            }
        return s + m_nestEnd;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The array of tokens that are nested
    */
    private BaseToken[] m_aTokens;

    /**
    * The character that starts the nesting
    */
    private char m_nestStart;

    /**
    * The character that ends the nesting.
    */
    private char m_nestEnd;
    }
