/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

/**
 * An exception thrown when errors occur building CohQL queries.
 * This exception would usually signify that a CohQL statement has
 * been executed with the incorrect syntax.
 *
 * @author jk  2013.12.09
 * @since Coherence 12.2.1
 */
public class CohQLException
        extends RuntimeException
    {
    /**
     * Construct a new exception with the specified detail message.
     *
     * @param sMessage  the detail message. The detail message is saved for
     *                  later retrieval by the {@link #getMessage()} method
     */
    public CohQLException(String sMessage)
        {
        super(sMessage);
        }

    /**
     * Construct a new exception with the specified detail message and
     * cause.
     * <p>
     * Note that the detail message associated with
     * <code>cause</code> is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param sMessage  the detail message (which is saved for later retrieval
     *                  by the {@link #getMessage()} method)
     * @param t         the cause (which is saved for later retrieval by the
     *                  {@link #getCause()} method).  (A <tt>null</tt> value is
     *                  permitted, and indicates that the cause is nonexistent or
     *                  unknown)
     */
    public CohQLException(String sMessage, Throwable t)
        {
        super(sMessage, t);
        }
    }
