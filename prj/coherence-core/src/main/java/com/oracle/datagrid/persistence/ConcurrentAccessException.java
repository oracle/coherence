/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.datagrid.persistence;

/**
 * This exception is thrown when a persistence operation fails due to another
 * process having exclusive access to a persistent resource.
 *
 * @author jh  2012.08.29
 *
 * @deprecated use {@link com.oracle.coherence.persistence.ConcurrentAccessException} instead
 */
@Deprecated
public class ConcurrentAccessException
        extends com.oracle.coherence.persistence.ConcurrentAccessException
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new ConcurrentAccessException.
     */
    public ConcurrentAccessException()
        {
        super();
        }

    /**
     * Create a new ConcurrentAccessException with the specified detail
     * message.
     *
     * @param sMessage  a detail message
     */
    public ConcurrentAccessException(String sMessage)
        {
        super(sMessage);
        }

    /**
     * Create a new ConcurrentAccessException with the specified detail
     * message and cause.
     *
     * @param sMessage  a detail message
     * @param eCause    the cause
     */
    public ConcurrentAccessException(String sMessage, Throwable eCause)
        {
        super(sMessage, eCause);
        }

    /**
     * Create a new ConcurrentAccessException with the specified cause.
     *
     * @param eCause  the cause
     */
    public ConcurrentAccessException(Throwable eCause)
        {
        super(eCause);
        }
    }
