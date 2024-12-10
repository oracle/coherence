/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.internal.net.queue.model.QueueKey;

import com.tangosol.util.extractor.UniversalExtractor;

/**
 * This class represents the key of an element in a paged queue.
 * The key is made up of two parts, the bucket id and the id
 * of the element within the bucket. As elements are added to
 * a queue they are added to a specific bucket and given that
 * buckets id and the next sequential element id within that bucket.
 */
public class PagedQueueKey
        extends QueueKey
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor used for POF
     */
    public PagedQueueKey()
        {
        }

    /**
     * Create a {@link PagedQueueKey} with the given bucket
     * id and element id.
     *
     * @param bucketId  the id of the bucket this key belongs to
     * @param elementId the id of the element within the bucket
     */
    public PagedQueueKey(int bucketId, long elementId)
        {
        super(bucketId, elementId);
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Return the bucket id of this {@link PagedQueueKey}.
     *
     * @return the bucket id of this {@link PagedQueueKey}
     */
    public int getBucketId()
        {
        return m_nHash;
        }

    /**
     * Return the element id of this {@link PagedQueueKey}.
     *
     * @return the element id of this {@link PagedQueueKey}
     */
    public int getElementId()
        {
        return (int) m_nId;
        }

    /**
     * Set the element id of this key
     *
     * @param elementId the element id of this key
     */
    public void setElementId(int elementId)
        {
        m_nId = elementId;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "PagedQueueKey[" +
                "bucketId=" + m_nHash +
                ", elementId=" + m_nId +
                ']';
        }

    // ----- constants ------------------------------------------------------

    /** static helper field for extracting the bucket id from a cache entry */
    public static final UniversalExtractor<PagedQueueKey, Integer> BUCKET_ID_EXTRACTOR
            = new UniversalExtractor<>("bucketId", new Object[0], UniversalExtractor.KEY);
    }
