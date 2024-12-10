/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.persistence;

/**
 * This exception indicates that an asynchronous persistence operation failed.
 * <p>
 * Note that this exception isn't explicitly thrown; rather, it will be
 * passed to the collector specified during the start of an asynchronous
 * transaction. Additionally, the receipt for the collector can be obtained
 * via the {@link #getReceipt()} method.
 *
 * @author jh  2013.07.17
 */
public class AsyncPersistenceException
        extends PersistenceException
    {
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

    // ----- accessors ------------------------------------------------------

    /**
     * Return the receipt associated with this exception.
     *
     * @return the receipt
     */
    public Object getReceipt()
        {
        Object oReceipt = m_oReceipt;
        return oReceipt == NO_RECEIPT ? null : oReceipt;
        }

    /**
     * Associate the specified receipt with this exception.
     * <p>
     * This method should only be called once. Once a receipt has been
     * associated with this exception, this method will have no effect.
     *
     * @param oReceipt  the receipt
     *
     * @return this exception
     */
    public synchronized AsyncPersistenceException initReceipt(Object oReceipt)
        {
        if (m_oReceipt == NO_RECEIPT)
            {
            m_oReceipt = oReceipt;
            }
        return this;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Constant used to indicate that a receipt hasn't been associated with
     * an AsyncPersistenceException.
     */
    private static final Object NO_RECEIPT = new Object();

    // ----- data members----------------------------------------------------

    /**
     * The receipt associated with this exception.
     */
    private volatile Object m_oReceipt = NO_RECEIPT;
    }
