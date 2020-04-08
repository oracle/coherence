/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof.reflect;


/**
* PofNavigationException indicates a failure to navigate a {@link PofValue}
* hierarchy.
*/
public class PofNavigationException
        extends RuntimeException
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a PofNavigationException.
    */
    public PofNavigationException()
        {
        }

    /**
    * Construct a PofNavigationException with a specified detail message.
    *
    * @param message  the String that contains a detailed message
    */
    public PofNavigationException(String message)
        {
        super(message);
        }

    /**
    * Construct a PofNavigationException with a specified detail message and
    * a cause.
    *
    * @param message  the String that contains a detailed message
    * @param cause    the underlying cause for this exception
    */
    public PofNavigationException(String message, Throwable cause)
        {
        super(message, cause);
        }

    /**
    * Construct a PofNavigationException with a specified cause.
    *
    * @param cause  the underlying cause for this exception
    */
    public PofNavigationException(Throwable cause)
        {
        super(cause);
        }
    }
