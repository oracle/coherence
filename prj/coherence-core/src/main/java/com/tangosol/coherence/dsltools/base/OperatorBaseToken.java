/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.base;


/**
* OperatorBaseToken is a token that represents a known operator.
*
* @author djl  2009.03.14
*/
public class OperatorBaseToken
        extends LeafBaseToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new OperatorBaseToken with the given operator value.
    *
    * @param s  the string that represents an operator
    */
     public OperatorBaseToken(String s)
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
    public boolean isOperator()
        {
        return true;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The string that represents the operator.
    */
    private String m_sValue;
    }

