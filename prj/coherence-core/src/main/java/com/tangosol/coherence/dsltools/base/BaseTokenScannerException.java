/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dsltools.base;


/**
* BaseTokenScannerExpression is the RuntimeException thrown by the
* BaseTokenScanner when expectations are not met.
*
* @author djl  2009.03.14
*/
public class BaseTokenScannerException
        extends RuntimeException
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new BaseTokenException with the given string.
    *
    * @param sMessage  the message String for the exception
    */
     public BaseTokenScannerException(String sMessage)
        {
        super(sMessage);
        }

    /**
    * Construct a new BaseTokenException.
    */
     public BaseTokenScannerException()
        {
        super();
        }
    }