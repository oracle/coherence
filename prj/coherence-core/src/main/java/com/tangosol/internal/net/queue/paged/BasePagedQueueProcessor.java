/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.internal.net.queue.processor.AbstractQueueProcessor;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;

/**
 * A base class for queue entry processors.
 */
public abstract class BasePagedQueueProcessor<R>
        extends AbstractQueueProcessor<Integer, Bucket, R>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new BaseQueueProcessor
     */
    public BasePagedQueueProcessor()
        {
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtains the {@link Bucket} for the specified entry.
     *
     * @param binaryEntry  the entry containing the Bucket
     * @param version     the version of the bucket
     *
     * @return the Bucket for the specified entry
     */
    protected Bucket ensureBucket(BinaryEntry<Integer, Bucket> binaryEntry, int nMaxBucketSize, QueueVersionInfo version)
        {
        Bucket bucket;

        if (binaryEntry.isPresent())
            {
            bucket = binaryEntry.getValue();
            }
        else
            {
            BinaryEntry<Integer, QueueVersionInfo> versionEntry = getVersionBinaryEntry(binaryEntry);

            QueueVersionInfo versionBucket;

            if (versionEntry.isPresent())
                {
                versionBucket = versionEntry.getValue();
                }
            else
                {
                versionBucket = instantiateQueueVersionInfo(version);
                }

            bucket = instantiateBucket(binaryEntry, versionBucket, nMaxBucketSize);
            }

        return bucket;
        }

    protected Bucket instantiateBucket(BinaryEntry<Integer, ?> bucketEntry, QueueVersionInfo version, int nMaxBucketSize)
        {
        return new Bucket(bucketEntry.getKey(), version, nMaxBucketSize);
        }

    protected QueueVersionInfo instantiateQueueVersionInfo(QueueVersionInfo version)
        {
        return version;
        }

    @SuppressWarnings("unchecked")
    protected BinaryEntry<Integer,QueueVersionInfo> getVersionBinaryEntry(BinaryEntry<?, ?> binaryEntry)
        {
        Binary                   binaryKey         = binaryEntry.getBinaryKey();
        BackingMapManagerContext context           = binaryEntry.getContext();
        BackingMapContext        backingMapContext = binaryEntry.getBackingMapContext();
        String                   versionCacheName  = PagedQueueCacheNames.Version.getCacheName(backingMapContext);
        BackingMapContext        versionContext    = context.getBackingMapContext(versionCacheName);

        return (BinaryEntry<Integer,QueueVersionInfo>) versionContext.getBackingMapEntry(binaryKey);
        }
    }
