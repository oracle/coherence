/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.base;


/**
* IdentifierBaseToken is a token that represents an identifier
*
* @author djl  2009.03.14
*/
public class IdentifierBaseToken
        extends LeafBaseToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new IdentifierBaseToken with the given value.
    *
    * @param s  the string that represents an identifier
    */
    public IdentifierBaseToken(String s)
        {
        m_sValue = s;
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

    /**
    * {@inheritDoc}
    */
    public boolean isIdentifier()
        {
        return true;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The string that represents the identifier.
    */
    private String m_sValue;
    }
