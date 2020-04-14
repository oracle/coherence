/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


/**
* The SyntaxException exception is equivalent to the "syntax error" message
* that a user sees when a script cannot compile.  It allows a parser to throw
* the exception from the depths of its syntactic analysis and catch it (if
* desired) and recover at a much higher level.
*
* @version 1.00, 09/14/98
* @author  Cameron Purdy
*/
public class SyntaxException
        extends CompilerException
    {
    /**
    * Constructs a SyntaxException with no detail message.
    * A detail message is a String that describes this particular exception.
    */
    public SyntaxException()
        {
        super();
        }

    /**
    * Constructs a SyntaxException with the specified detail message.
    * A detail message is a String that describes this particular exception.
    * @param s the String that contains a detailed message
    */
    public SyntaxException(String s)
        {
        super(s);
        }
    }
