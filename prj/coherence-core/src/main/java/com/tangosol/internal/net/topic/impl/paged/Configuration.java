/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.net.cache.LocalCache;

/**
 * The configuration for a {@link PagedTopic}.
 *
 * @author jk 2015.05.26
 * @since Coherence 14.1.1
 */
public class Configuration
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public Configuration()
        {
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Obtain the page capacity in bytes.
     *
     * @return the capacity
     */
    public int getPageCapacity()
        {
        return m_cbPageCapacity;
        }

    /**
     * Set the page capacity in bytes.
     *
     * @param cbPageCapacity  the capacity
     */
    public void setPageCapacity(int cbPageCapacity)
        {
        m_cbPageCapacity = cbPageCapacity;
        }

    /**
     * Get maximum capacity for a server.
     *
     * @return return the capacity or zero if unlimited.
     */
    public long getServerCapacity()
        {
        return m_cbServerCapacity;
        }

    /**
     * Set the maximum capacity for a sever, or zero for unlimited.
     *
     * @param cbServer the maximum capacity for a server.
     */
    public void setServerCapacity(long cbServer)
        {
        m_cbServerCapacity = cbServer;
        }

    /**
     * Obtain the expiry delay to apply to elements in ths topic.
     *
     * @return  the expiry delay to apply to elements in ths topic
     */
    public long getElementExpiryMillis()
        {
        return m_cMillisExpiry;
        }

    /**
     * Set the expiry delay to apply to elements in ths topic.
     *
     * @param cMillisExpiry  the expiry delay to apply to elements in ths topic
     */
    public void setElementExpiryMillis(long cMillisExpiry)
        {
        m_cMillisExpiry = cMillisExpiry;
        }

    /**
     * Return the maximum size of a batch.
     *
     * @return the max batch size
     */
    public long getMaxBatchSizeBytes()
        {
        return m_cbMaxBatch;
        }

    /**
     * Specify the maximum size of a batch.
     *
     * @param cb  the max batch size
     */
    public void setMaxBatchSizeBytes(int cb)
        {
        m_cbMaxBatch = cb;
        }

    public boolean isRetainConsumed()
        {
        return m_fRetainConsumed;
        }

    public void setRetainConsumed(boolean fRetainElements)
        {
        m_fRetainConsumed = fRetainElements;
        }

    // ----- Object methods -------------------------------------------------

    public String toString()
        {
        StringBuilder sb = new StringBuilder();
        sb.append("PageTopicScheme Configuration: Page=").append(m_cbPageCapacity).append("b, ");
        sb.append("CacheServerMaxStorage=").append(m_cbServerCapacity).append(",");
        sb.append("Expiry=").append(m_cMillisExpiry).append("ms,");
        sb.append("MaxBatch=").append(m_cbMaxBatch).append("b,");
        sb.append("RetainConsumed=").append(m_fRetainConsumed);
        return sb.toString();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The default capacity of pages
     */
    public static final int DEFAULT_PAGE_CAPACITY_BYTES = 1024*1024;

    // ----- data members ---------------------------------------------------

    /**
     * The maximum number of elements that pages
     * in this topic can contain.
     */
    private int m_cbPageCapacity = DEFAULT_PAGE_CAPACITY_BYTES;

    /**
     * The maximum storage usage per cache server
     */
    private long m_cbServerCapacity = Long.MAX_VALUE;

    /**
     * The expiry time for elements offered to the topic
     */
    private long m_cMillisExpiry = LocalCache.DEFAULT_EXPIRE;

    /**
     * The target maximum batch size.
     */
    private int m_cbMaxBatch;

    /**
     * Flag indicating whether this topic retains elements after they have been
     * read by subscribers.
     */
    private boolean m_fRetainConsumed;
    }
