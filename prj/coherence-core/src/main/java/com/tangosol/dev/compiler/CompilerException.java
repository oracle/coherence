/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


/**
* The CompilerException exception is used to stop the compilation of
* a script.  It is thrown when an element of the compilation process
* determines that compilation cannot complete successfully and that
* compilation should be stopped.  Compilation is not necessarily stopped
* when the first compilation error is encountered (as in a syntax, lexical,
* or semantic error) because the user typically wants to know about as many
* errors as possible.
*
* @version 1.00, 11/22/96
* @author 	Cameron Purdy
*/
public class CompilerException
        extends Exception
    {
    /**
    * Constructs a CompilerException with no detail message.
    * A detail message is a String that describes this particular exception.
    */
    public CompilerException()
        {
        super();
        }

    /**
    * Constructs a CompilerException with the specified detail message.
    * A detail message is a String that describes this particular exception.
    * @param s the String that contains a detailed message
    */
    public CompilerException(String s)
        {
        super(s);
        }
    }
