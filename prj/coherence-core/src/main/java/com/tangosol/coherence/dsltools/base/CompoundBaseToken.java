/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.base;


/**
* CompoundBaseToken is the abstract base class for all tokens processed by the
* low level BaseTokenScanner that are made up of two or more BaseTokens.
*
* @author djl  2009.03.14
*/
public abstract class CompoundBaseToken
        extends BaseToken
    {
    // ----- Compound BaseToken interface -----------------------------------

    /**
    * Return an array of BaseTokens making up the receiver.
    *
    * @return the an array of BaseTokens making up the receiver.
    */
    public abstract BaseToken[] getTokens();

    /**
    * Return the size of the collection of BaseTokens making up the receiver.
    *
    * @return  the size of the BaseTocken collection making up the receiver
    */
    public int size()
        {
        return getTokens().length;
        }

    /**
    * Return the BaseToken at the given index
    *
    * @param  index the zero base index for retreiving a BaseToken
    *
    * @return  the BaseToken at the given index
    */
    public BaseToken get(int index)
        {
        return getTokens()[index];
        }


    // ----- BaseToken interface --------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean isLeaf()
        {
        return false;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isCompound()
        {
        return true;
        }

    /**
    * {@inheritDoc}
    */
    public boolean match(String s, boolean fIgnoreCaseFlag)
        {
        return false;
        }

    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this token.
    *
    * @return a String description of the token
    */
    public String toString()
        {
        String      s       = getSimpleName() + "{";
        BaseToken[] aoTokens = getTokens();

        for (int i = 0, c = aoTokens.length; i < c; ++i)
            {
            s += aoTokens[i];
            if (i != c - 1)
                {
                s += ", ";
                }
            }
        return s + "}";
        }
    }
