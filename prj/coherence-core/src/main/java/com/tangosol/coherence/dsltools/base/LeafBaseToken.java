/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.base;


/**
* LeafBaseToken is the abstract base class for all tokes processed by the
* low level BaseTokenScanner that are considered leaves.
*
* @author djl  2009.03.14
*/
public abstract class LeafBaseToken
        extends BaseToken
    {
    // ----- Leaf BaseToken interface --------------------------------------------

    /**
    * Return the string representation of this LeafBaseToken.
    *
    * @return  the string that represents the reciever
    */
    public abstract String getValue();


    // ----- BaseToken interface --------------------------------------------

    /**
    * {@inheritDoc}
    */
     public boolean isLeaf()
        {
        return true;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isCompound()
        {
        return false;
        }

    /**
    * {@inheritDoc}
    */
    public boolean match(String s, boolean fIgnoreCase)
        {
        String sValue = getValue();
        return fIgnoreCase ? s.equalsIgnoreCase(sValue) : s.equals(sValue);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this token.
    *
    * @return a String description of the token
    */
    public String toString()
        {
        return getSimpleName() + "{" + getValue() + "}";
        }
    }
