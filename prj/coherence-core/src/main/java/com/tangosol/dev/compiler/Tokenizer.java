/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import com.tangosol.util.ErrorList;

import java.util.NoSuchElementException;


/**
* Represents a lexical tokenizer.  This interface is implemented for each
* specific language.
*
* @version 1.00, 12/05/96
* @author 	Cameron Purdy
*/
public interface Tokenizer
    {
    /**
    * Initializes the tokenizer.  This method must be called exactly
    * one time to initialize the tokenizer object.
    *
    * @param script   the script to tokenize
    * @param errlist  the list to log errors to
    *
    * @exception NoSuchElementException If the tokens are exhausted
    * @exception CompilerException If a lexical error occurs that should stop
    *            the compilation process
    */
    public void setScript(Script script, ErrorList errlist)
            throws CompilerException;

    /**
    * Checks for more tokens in the script.
    *
    * @return true if tokenizing of the script is incomplete.
    */
    public boolean hasMoreTokens();

    /**
    * Eats and returns the next token from the script.
    *
    * @return the next token
    *
    * @exception NoSuchElementException If the tokens are exhausted
    * @exception CompilerException If a lexical error occurs that should stop
    *            the compilation process
    */
    public Token nextToken() throws CompilerException;

    /**
    * Regurgitates the last eaten token so that the next call to nextToken
    * will return the same token that was returned by the most recent call
    * to nextToken.  (This method can be called more than once to regurgitate
    * multiple tokens.)
    */
    public void putBackToken(Token tok);

    /**
    * Returns an object that can be used to restore the current position
    * in the script.  This method is similar to the mark method of the
    * Java stream classes, but by returning an object that identifies the
    * position, multiple positions can be saved and later returned to.
    *
    * @return an object which identifies the current position within the
    *         script
    *
    * @see #restorePosition(ParsePosition pos)
    */
    public ParsePosition savePosition();

    /**
    * Restores the current parsing position that was returned from
    * the savePosition method.
    *
    * @param pos The return value from a previous call to savePosition
    *
    * @see #savePosition()
    */
    public void restorePosition(ParsePosition pos);
    }
