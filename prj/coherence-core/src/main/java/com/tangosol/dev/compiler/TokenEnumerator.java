/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import java.util.Enumeration;
import java.util.NoSuchElementException;


/**
* A token enumerator iterates over the tokens provided by a Tokenizer.
*
* @version 1.00, 12/06/96
* @author 	Cameron Purdy
*/
public class TokenEnumerator
        implements Enumeration
    {
    // ----- data members ---------------------------------------------------

    /**
    * The tokenizer to enumerate.
    */
    private Tokenizer m_tokenizer;


    // ----- constructors ---------------------------------------------------

    /**
    * Constructs a TokenEnumerator based on a Tokenizer instance.
    */
    public TokenEnumerator(Tokenizer tokenizer)
        {
        m_tokenizer = tokenizer;
        }


    // ----- enumeration interface ------------------------------------------

    /**
    * Returns true if the enumeration contains more elements; false
    * if its empty.
    */
    public boolean hasMoreElements()
        {
	    return m_tokenizer.hasMoreTokens();
        }

    /**
    * Returns the next element of the enumeration. Calls to this
    * method will enumerate successive elements.
    *
    * @exception NoSuchElementException If no more elements exist.
    */
    public Object nextElement()
        {
        try
            {
            return m_tokenizer.nextToken();
            }
        catch (CompilerException e)
            {
            throw new NoSuchElementException();
            }
	    }
    }
