/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.net.CacheService;

/**
 * The DefaultReplicatedCacheDependencies class provides a default implementation of
 * ReplicatedCacheDependencies.
 *
 * @author pfm 2011.07.07
 * @since Coherence 12.1.2
 */
public class DefaultReplicatedCacheDependencies
        extends DefaultGridDependencies
        implements ReplicatedCacheDependencies
    {
    /**
     * Construct a DefaultReplicatedCacheDependencies object.
     */
    public DefaultReplicatedCacheDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultReplicatedCacheDependencies object, copying the values from the
     * specified ReplicatedCacheDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultReplicatedCacheDependencies(ReplicatedCacheDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_cEnsureCacheTimeout  = deps.getEnsureCacheTimeoutMillis();
            m_nGraveyardSize       = deps.getGraveyardSize();
            m_nLeaseGranularity    = deps.getLeaseGranularity();
            m_fMobileIssues        = deps.isMobileIssues();
            m_cStandardLeaseMillis = deps.getStandardLeaseMillis();
            }
        }

    // ----- ReplicatedCacheDependencies interface-----------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public long getEnsureCacheTimeoutMillis()
        {
        return m_cEnsureCacheTimeout;
        }

    /**
     * Set the ensure cache timeout in milliseconds.
     *
     * @param cMillis  the ensure cache timeout
     */
    @Injectable("ensure-cache-timeout")
    public void setEnsureCacheTimeoutMillis(long cMillis)
        {
        m_cEnsureCacheTimeout = cMillis;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getGraveyardSize()
        {
        return m_nGraveyardSize;
        }

    /**
     * Set the graveyard size.
     *
     * @param nSize  the graveyard size
     */
    @Injectable("graveyard-size")
    public void setGraveyardSize(int nSize)
        {
        m_nGraveyardSize = nSize;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMobileIssues()
        {
        return m_fMobileIssues;
        }

    /**
     * Set the mobile issues flag.  This is a compatibility parameter for
     * pre-Coherence 3.6 lease issue behavior.
     *
     * @param fMobile  the mobile issues flag
     */
    @Injectable("mobile-issues")
    public void setMobileIssues(boolean fMobile)
        {
        m_fMobileIssues = fMobile;
        }

    // ----- LeaseDependencies interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLeaseGranularity()
        {
        return m_nLeaseGranularity;
        }

    /**
     * Set the lease granularity.
     *
     * @param nGranularity  the lease granularity
     */
    @Injectable("lease-granularity")
    public void setLeaseGranularity(int nGranularity)
        {
        m_nLeaseGranularity = nGranularity;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getStandardLeaseMillis()
        {
        return m_cStandardLeaseMillis;
        }

    /**
     * Set the standard lease milliseconds.
     *
     * @param cMillis  the standard lease milliseconds
     */
    @Injectable("standard-lease-milliseconds")
    public void setStandardLeaseMillis(long cMillis)
        {
        m_cStandardLeaseMillis = cMillis;
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The ensure cache timeout.
     */
    private long m_cEnsureCacheTimeout = 30000;

    /**
     * The graveyard size.
     */
    private int m_nGraveyardSize;

    /**
     * The lease granularity.
     */
    private int m_nLeaseGranularity;

    /**
     * The mobile-issues flag.
     */
    private boolean m_fMobileIssues = false;

    /**
     * The standard lease milliseconds.
     */
    private long m_cStandardLeaseMillis = 20000;
    }
