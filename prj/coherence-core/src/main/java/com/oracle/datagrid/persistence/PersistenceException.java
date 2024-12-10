/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.datagrid.persistence;

/**
 * <code>PersistenceException</code> is the superclass of all exception
 * classes in the <code>com.oracle.coherence.persistence</code> package.
 *
 * @author jh  2012.08.29
 *
 * @deprecated use {@link com.oracle.coherence.persistence.PersistenceException} instead
 */
@Deprecated
public class PersistenceException
        extends com.oracle.coherence.persistence.PersistenceException
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new PersistenceException.
     */
    public PersistenceException()
        {
        super();
        }

    /**
     * Create a new PersistenceException with the specified detail message.
     *
     * @param sMessage  a detail message
     */
    public PersistenceException(String sMessage)
        {
        super(sMessage);
        }

    /**
     * Create a new PersistenceException with the specified detail message
     * and cause.
     *
     * @param sMessage  a detail message
     * @param eCause    the cause
     */
    public PersistenceException(String sMessage, Throwable eCause)
        {
        super(sMessage, eCause);
        }

    /**
     * Create a new PersistenceException with the specified cause.
     *
     * @param eCause  the cause
     */
    public PersistenceException(Throwable eCause)
        {
        super(eCause);
        }

    /**
     * Create a new PersistenceException with the specified detail message
     * and cause.
     *
     * @param sMessage  a detail message
     * @param eCause    the cause
     */
    public PersistenceException(String sMessage, PersistenceException eCause)
        {
        super(sMessage, eCause);
        }

    /**
     * Create a new PersistenceException with the specified cause.
     *
     * @param eCause  the cause
     */
    public PersistenceException(PersistenceException eCause)
        {
        super(eCause);
        }
    }
