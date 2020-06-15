/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.base;


/**
* PunctuationBaseToken is a token that represents a known punctuation.
*
* @author djl  2009.03.14
*/
public class PunctuationBaseToken
        extends LeafBaseToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new PunctuationBaseToken with the given punctuation value.
    *
    * @param s  the string that represents a punctuation
    */
    public PunctuationBaseToken(String s)
        {
        this.m_sValue = s;
        }


    // ----- Leaf BaseToken interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
     public String getValue()
        {
        return m_sValue;
        }


    // ----- BaseToken interface --------------------------------------------

    public boolean isPunctuation()
        {
        return true;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this token.
    *
    * @return a String description of the token
    */
    public String toString()
        {
        return "Punctuation{\"" + getValue() + "\"}";
        }


    // ----- data members ---------------------------------------------------

    /**
    * The string that represents the punctuation
    */
    private String m_sValue;
    }
