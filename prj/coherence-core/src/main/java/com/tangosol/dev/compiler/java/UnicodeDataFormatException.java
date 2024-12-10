/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import java.io.IOException;


/**
* UnicodeDataFormatException is thrown when an invalid unicode escape is
* encountered.
*
* @version 1.00, 12/05/96
* @author 	Cameron Purdy
*/
public class UnicodeDataFormatException extends IOException
    {
    /**
    * Constructs a UnicodeDataFormatException with no detail message.
    * A detail message is a String that describes this particular exception.
    */
    public UnicodeDataFormatException()
        {
        super();
        }

    /**
    * Constructs a UnicodeDataFormatException with the specified detail message.
    * A detail message is a String that describes this particular exception.
    * @param s the String that contains a detailed message
    */
    public UnicodeDataFormatException(String s)
        {
        super(s);
        }
    }
