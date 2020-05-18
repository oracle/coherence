/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.datagrid.persistence;

/**
 * This exception is thrown when a persistence operation fails due to a
 * non-recoverable issue with a persistent resource.
 *
 * @author jh  2012.08.29
 *
 * @deprecated use {@link com.oracle.coherence.persistence.FatalAccessException} instead
 */
@Deprecated
public class FatalAccessException
        extends com.oracle.coherence.persistence.FatalAccessException
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new FatalAccessException.
     */
    public FatalAccessException()
        {
        super();
        }

    /**
     * Create a new FatalAccessException with the specified detail message.
     *
     * @param sMessage  a detail message
     */
    public FatalAccessException(String sMessage)
        {
        super(sMessage);
        }

    /**
     * Create a new FatalAccessException with the specified detail message
     * and cause.
     *
     * @param sMessage  a detail message
     * @param eCause    the cause
     */
    public FatalAccessException(String sMessage, Throwable eCause)
        {
        super(sMessage, eCause);
        }

    /**
     * Create a new FatalAccessException with the specified cause.
     *
     * @param eCause  the cause
     */
    public FatalAccessException(Throwable eCause)
        {
        super(eCause);
        }
    }
