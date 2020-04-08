/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


/**
* OPExpression is the RuntimeException thrown by the OPParser and OPScanner
* when problems are detected.
*
* @author djl  2009.03.14
*/
public class OPException extends RuntimeException
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new OPException with the given message string.
    *
    * @param sMessage the message String for the exception
    */
     public OPException(String sMessage)
        {
        super(sMessage);
        }

    /**
    * Construct a new OPException.
    */
     public OPException()
        {
        super();
        }
    }
