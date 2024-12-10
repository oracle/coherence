/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;


import com.oracle.coherence.persistence.PersistenceEnvironment;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.SnapshotArchiverFactory;


/**
 * Default implementation of PersistenceDependencies.
 *
 * @since  Coherence 12.1.3
 * @author rhl 2013.07.25
 */
public class DefaultPersistenceDependencies
        implements PersistenceDependencies
    {
    /**
     * {@inheritDoc}
     */
    public ParameterizedBuilder getPersistenceEnvironmentBuilder()
        {
        return m_bldrPersistence;
        }

    /**
     * Set {@link ParameterizedBuilder} that creates a PersistenceEnvironment.
     *
     * @param bldr  the  ParameterizedBuilder that creates a PersistenceEnvironment
     *
     * @return this object
     */
    public DefaultPersistenceDependencies setPersistenceEnvironmentBuilder(ParameterizedBuilder<PersistenceEnvironment<ReadBuffer>> bldr)
        {
        m_bldrPersistence = bldr;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    public SnapshotArchiverFactory getArchiverFactory()
        {
        return m_factoryArch;
        }

    /**
     * Set SnapshotArchiverFactory.
     *
     * @param factory  the SnapshotArchiverFactory
     *
     * @return this object
     */
    public DefaultPersistenceDependencies setArchiverFactory(SnapshotArchiverFactory factory)
        {
        m_factoryArch = factory;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    public int getFailureMode()
        {
        return m_nFailureMode;
        }

    /**
     * Set failure mode (one of the FAILURE_* constants).
     *
     * @param nMode  the failure mode
     *
     * @return this object
     */
    public DefaultPersistenceDependencies setFailureMode(int nMode)
        {
        m_nFailureMode = nMode;
        return this;
        }

    /**
     * Return the persistence mode.
     *
     * @return the persistence mode
     */
    public String getPersistenceMode()
        {
        return m_sMode;
        }

    /**
     * Set the persistence mode.
     *
     * @param sMode  the persistence mode
     */
    public void setPersistenceMode(String sMode)
        {
        m_sMode = sMode;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The builder for creating PersistentEnvironments.
     */
    protected ParameterizedBuilder<PersistenceEnvironment<ReadBuffer>> m_bldrPersistence;

    /**
     * The factory for creating SnapshotArchivers.
     */
    protected SnapshotArchiverFactory m_factoryArch;

    /**
     * The persistence failure mode.
     */
    protected int m_nFailureMode = FAILURE_STOP_SERVICE;

    protected String m_sMode = PersistenceDependencies.MODE_ACTIVE;
    }
