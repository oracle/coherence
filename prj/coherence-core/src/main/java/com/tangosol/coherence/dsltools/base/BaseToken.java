/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.base;


/**
* BaseToken is the abstract base class for all tokens processed by the low
* level BaseTokenScanner.  The resulting hierarchy represents very simple
* syntactic elements.
*
* @author djl  2009.03.14
*/
public abstract class BaseToken
    {
    // ----- BaseToken interface --------------------------------------------

    /**
    * Answer whether this token is a leaf token.
    *
    * @return the answer to the question "is this token a leaf?"
    */
     public abstract boolean isLeaf();

    /**
    * Answer whether this token is a compound token.
    *
    * @return the answer to the question "is this token compound?"
    */
    public abstract boolean isCompound();

    /**
    * Answer whether this token represents a literal.
    *
    * @return the answer to the question "is this token a literal?"
    */
    public boolean isLiteral()
        {
        return false;
        }

    /**
    * Answer whether this token represents a know operator.
    *
    * @return the answer to the question "is this token an operator?"
    */
    public boolean isOperator()
        {
        return false;
        }

    /**
    * Answer whether this token represents an identifier.
    *
    * @return the answer to the question "is this token an identifier?"
    */
    public boolean isIdentifier()
        {
        return false;
        }

    /**
    * Answer whether this token represents a nested collection of tokens.
    *
    * @return the answer to the question "is this token a nesting of
    *         tokens?"
    */
    public boolean isNest()
        {
        return false;
        }

    /**
    * Answer whether this token represents a punctuation character.
    *
    * @return the answer to the question "is this token a punctuation
    *         character?"
    */
    public boolean isPunctuation()
        {
        return false;
        }

    /**
    * Answer whether this token matches the given string. By default case
    * matters.
    *
    * @param s the String to match agains
    *
    * @return the answer to the question "does this token match a given
    *         string?
    */
    public boolean match(String s)
        {
        return match(s, false);
        }

    /**
    * Answer whether this token matches the given string.
    *
    * @param s                the String to match agains
    * @param fIgnoreCaseFlag  the flag that controls if case matters
    *
    * @return the answer to the question "does this token match a given
    *         string?
    */
    public abstract boolean match(String s, boolean fIgnoreCaseFlag);

    /**
    * Return the simple class name for this instance.
    *
    * @return the class name without the package path
    */
    public String getSimpleName()
        {
        String s = getClass().getName();
        int    i = s.lastIndexOf('.');

        return s.substring(i + 1);
        }
    }
