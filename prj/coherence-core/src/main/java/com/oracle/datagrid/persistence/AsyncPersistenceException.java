/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.datagrid.persistence;

/**
 * This exception indicates that an asynchronous persistence operation failed.
 * <p>
 * Note that this exception isn't explicitly thrown; rather, it will be
 * passed to the collector specified during the start of an asynchronous
 * transaction. Additionally, the receipt for the collector can be obtained
 * via the {@link #getReceipt()} method.
 *
 * @author jh  2013.07.17
 *
 * @deprecated use {@link com.oracle.coherence.persistence.AsyncPersistenceException} instead
 */
@Deprecated
public class AsyncPersistenceException
        extends com.oracle.coherence.persistence.AsyncPersistenceException
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new AsyncPersistenceException.
     */
    public AsyncPersistenceException()
        {
        super();
        }

    /**
     * Create a new AsyncPersistenceException with the specified detail
     * message.
     *
     * @param sMessage  a detail message
     */
    public AsyncPersistenceException(String sMessage)
        {
        super(sMessage);
        }

    /**
     * Create a new AsyncPersistenceException with the specified detail
     * message and cause.
     *
     * @param sMessage  a detail message
     * @param eCause    the cause
     */
    public AsyncPersistenceException(String sMessage, Throwable eCause)
        {
        super(sMessage, eCause);
        }

    /**
     * Create a new AsyncPersistenceException with the specified cause.
     *
     * @param eCause  the cause
     */
    public AsyncPersistenceException(Throwable eCause)
        {
        super(eCause);
        }
    }
