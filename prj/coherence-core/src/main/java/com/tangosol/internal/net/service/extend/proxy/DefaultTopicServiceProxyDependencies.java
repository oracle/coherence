/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;


import com.tangosol.config.annotation.Injectable;

/**
 * The DefaultCacheServiceProxyDependencies class provides a default implementation of
 * CacheServiceProxyDependencies.
 *
 * @author Jonathan Knight  2025.01.01
 */
public class DefaultTopicServiceProxyDependencies
        extends DefaultServiceProxyDependencies
        implements TopicServiceProxyDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultTopicServiceProxyDependencies object.
     */
    public DefaultTopicServiceProxyDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultTopicServiceProxyDependencies object, copying the values from the
     * specified TopicServiceProxyDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultTopicServiceProxyDependencies(TopicServiceProxyDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_cbTransferThreshold = deps.getTransferThreshold();
            }
        }

    // ----- TopicServiceProxyDependencies methods --------------------------

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

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return super.toString() + "{TransferThreshold=" + getTransferThreshold() + "}";
        }

    // ----- data members ---------------------------------------------------

    /**
     * The transfer threshold.
     */
    private long m_cbTransferThreshold = 524288;
    }
