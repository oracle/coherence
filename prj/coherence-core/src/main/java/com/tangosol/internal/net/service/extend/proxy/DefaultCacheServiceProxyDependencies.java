/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.util.Base;

/**
 * The DefaultCacheServiceProxyDependencies class provides a default implementation of
 * CacheServiceProxyDependencies.
 *
 * @author pfm 2011.07.25
 * @since Coherence 12.1.2
 */
public class DefaultCacheServiceProxyDependencies
        extends DefaultServiceProxyDependencies
        implements CacheServiceProxyDependencies
    {
    /**
     * Construct a DefaultCacheServiceProxyDependencies object.
     */
    public DefaultCacheServiceProxyDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultCacheServiceProxyDependencies object, copying the values from the
     * specified CacheServiceProxyDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultCacheServiceProxyDependencies(CacheServiceProxyDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_fLockEnabled        = deps.isLockEnabled();
            m_fReadyOnly          = deps.isReadOnly();
            m_cbTransferThreshold = deps.getTransferThreshold();
            }
        }

    // ----- DefaultCacheServiceProxyDependencies methods -------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLockEnabled()
        {
        return m_fLockEnabled;
        }

    /**
     * Set the lock enabled flag.
     *
     * @param fEnabled  the lock enabled flag
     */
    @Injectable("lock-enabled")
    public void setLockEnabled(boolean fEnabled)
        {
        m_fLockEnabled = fEnabled;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadOnly()
        {
        return m_fReadyOnly;
        }

    /**
     * Set the read only flag.
     *
     * @param fReadOnly  the read only flag
     */
    @Injectable("read-only")
    public void setReadOnly(boolean fReadOnly)
        {
        m_fReadyOnly = fReadOnly;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTransferThreshold()
        {
        return m_cbTransferThreshold;
        }

    /**
     * Set the transfer threshold.
     *
     * @param cbThreshold  the transfer threshold
     */
    @Injectable("transfer-threshold")
    public void setTransferThreshold(long cbThreshold)
        {
        m_cbTransferThreshold = cbThreshold;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultCacheServiceProxyDependencies validate()
        {
        super.validate();

        Base.checkRange(getTransferThreshold(), 0, Integer.MAX_VALUE, "TransferThreshold");

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString() + "{LockEnabled=" + isLockEnabled() + ", ReadOnly=" + isReadOnly()
               + ", TransferThreshold=" + getTransferThreshold() + "}";
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The enabled flag.
     */
    private boolean m_fLockEnabled;

    /**
     * The read only flag.
     */
    private boolean m_fReadyOnly;

    /**
     * The transfer threshold.
     */
    private long m_cbTransferThreshold = 524288;
    }
