/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.persistence;

/**
 * <code>PersistenceException</code> is the superclass of all exception
 * classes in the <code>com.oracle.coherence.persistence</code> package.
 *
 * @author jh  2012.08.29
 */
public class PersistenceException
        extends RuntimeException
    {
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

        initPersistenceEnvironment(eCause.getPersistenceEnvironment()).
                initPersistenceManager(eCause.getPersistenceManager()).
                initPersistentStore(eCause.getPersistentStore());
        }

    /**
     * Create a new PersistenceException with the specified cause.
     *
     * @param eCause  the cause
     */
    public PersistenceException(PersistenceException eCause)
        {
        super(eCause);

        initPersistenceEnvironment(eCause.getPersistenceEnvironment()).
                initPersistenceManager(eCause.getPersistenceManager()).
                initPersistentStore(eCause.getPersistentStore());
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the PersistenceEnvironment associated with this exception.
     *
     * @return the environment
     */
    public PersistenceEnvironment<?> getPersistenceEnvironment()
        {
        PersistenceEnvironment<?>[] aEnv = m_aEnv;
        return aEnv == null ? null : aEnv[0];
        }

    /**
     * Associate the specified PersistenceEnvironment with this exception.
     * <p>
     * This method should only be called once. Once a PersistenceEnvironment
     * has been associated with this exception, this method will have no
     * effect.
     *
     * @param env  the environment
     *
     * @return this exception
     */
    public synchronized PersistenceException initPersistenceEnvironment(PersistenceEnvironment<?> env)
        {
        if (m_aEnv == null)
            {
            m_aEnv = new PersistenceEnvironment<?>[] {env};
            }
        return this;
        }

    /**
     * Return the PersistenceManager associated with this exception.
     *
     * @return the manager
     */
    public PersistenceManager<?> getPersistenceManager()
        {
        PersistenceManager<?>[] aManager = m_aManager;
        return aManager == null ? null : aManager[0];
        }

    /**
     * Associate the specified PersistenceManager with this exception.
     * <p>
     * This method should only be called once. Once a PersistenceManager has
     * been associated with this exception, this method will have no effect.
     *
     * @param manager  the manager
     *
     * @return this exception
     */
    public synchronized PersistenceException initPersistenceManager(PersistenceManager<?> manager)
        {
        if (m_aManager == null)
            {
            m_aManager = new PersistenceManager<?>[] {manager};
            }
        return this;
        }

    /**
     * Return the PersistentStore associated with this exception.
     *
     * @return the store
     */
    public PersistentStore<?> getPersistentStore()
        {
        PersistentStore<?>[] aStore = m_aStore;
        return aStore == null ? null : m_aStore[0];
        }

    /**
     * Associate the specified PersistentStore with this exception.
     * <p>
     * This method should only be called once. Once a PersistentStore has
     * been associated with this exception, this method will have no effect.
     *
     * @param store  the store
     *
     * @return this exception
     */
    public synchronized PersistenceException initPersistentStore(PersistentStore<?> store)
        {
        if (m_aStore == null)
            {
            m_aStore = new PersistentStore<?>[] {store};
            }
        return this;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The PersistenceEnvironment associated with this exception.
     */
    private volatile PersistenceEnvironment<?>[] m_aEnv;

    /**
     * The PersistenceManager associated with this exception.
     */
    private volatile PersistenceManager<?>[] m_aManager;

    /**
     * The PersistentStore associated with this exception.
     */
    private volatile PersistentStore<?>[] m_aStore;
    }
