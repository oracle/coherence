/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.util.InvocableMap;

import static com.tangosol.internal.net.queue.paged.Utils.unsignedIncrement;

/**
 * This {@link InvocableMap.EntryProcessor} implementation is used to poll or peek at
 * the element at the head of a bucket in a distributed {@link PagedNamedQueue}.
 */
public class QueuePollPeekHeadProcessor
        extends BasePollPeekProcessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor used for serialization.
     */
    public QueuePollPeekHeadProcessor()
        {
        }

    /**
     * Create a {@link QueuePollPeekHeadProcessor} that will perform the
     * specified action.
     *
     * @param fPoll    {@code true} if the action to perform is a poll, or {@code false} if the action is peek
     * @param version  the version number of the bucket
     */
    public QueuePollPeekHeadProcessor(boolean fPoll, QueueVersionInfo version)
        {
        super(fPoll, version);
        }

    // ----- BasePollPeekProcessor implementation ---------------------------

    @Override
    protected boolean isValidVersion(QueueVersionInfo version)
        {
        return version.getHeadPollVersion() == m_version.getHeadPollVersion();
        }

    @Override
    protected PagedQueueKey firstQueueKey(Bucket bucket)
        {
        return new PagedQueueKey(bucket.getId(), bucket.getHead());
        }

    @Override
    protected PagedQueueKey nextQueueKey(Bucket bucket, PagedQueueKey queueKey)
        {
        int elementId = queueKey.getElementId();
        if (elementId == bucket.getTail())
            {
            return null;
            }

        queueKey.setElementId(unsignedIncrement(elementId));
        return queueKey;
        }

    @Override
    protected void poll(Bucket bucket, PagedQueueKey key)
        {
        int elementId = key.getElementId();

        if (bucket.getTail() == elementId)
            {
            bucket.markEmpty();
            }
        else
            {
            bucket.setHead(unsignedIncrement(elementId));
            }
        }

    @Override
    protected void notifyRemovingEmptyBucket(Bucket bucket)
        {
        bucket.getVersion().incrementHeadPollVersion();
        bucket.getVersion().incrementTailOfferVersion();
        }
    }
