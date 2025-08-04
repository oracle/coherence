/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.processor;

import com.tangosol.internal.net.queue.extractor.QueueKeyExtractor;

import com.tangosol.internal.net.queue.paged.PagedQueueKey;
import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapIndex;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An entry processor to check whether a queue is a
 * paged queue or not.
 *
 * @param <K>  the type of the cache keys
 * @param <V>  the type of the cache values
 */
public class MaybePagedQueueProcessor<K, V>
        extends AbstractQueueProcessor<K, V, Boolean>
        implements ExternalizableLite, EvolvablePortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public MaybePagedQueueProcessor()
        {
        }

    @Override
    public Boolean process(InvocableMap.Entry<K, V> entry)
        {
        BinaryEntry<K, V> binaryEntry = entry.asBinaryEntry();
        // If a PagedQueueKey bucket Id extractor index exists, this is certainly a paged queue.
        MapIndex<?, ?, ?> indexPaged  = binaryEntry.getIndexMap().get(PagedQueueKey.BUCKET_ID_EXTRACTOR);
        if (indexPaged != null)
            {
            return true;
            }
        // If a QueueKeyExtractor index exists, this is not a paged queue.
        MapIndex<?, ?, ?> indexQueue = binaryEntry.getIndexMap().get(QueueKeyExtractor.INSTANCE);
        if (indexQueue != null)
            {
            return false;
            }
        // the cache is neither simple nor paged
        return null;
        }

    @Override
    public int getImplVersion()
        {
        return IMPL_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        }

    /**
     * Return an instance of {@link MaybePagedQueueProcessor}.
     *
     * @param <K>  the type of the cache keys
     * @param <V>  the type of the cache values
     *
     * @return an instance of {@link MaybePagedQueueProcessor}
     */
    @SuppressWarnings("unchecked")
    public static <K, V> MaybePagedQueueProcessor<K, V> instance()
        {
        return INSTANCE;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The evolvable POF implementation version.
     */
    private static final int IMPL_VERSION = 1;

    /**
     * A singleton instance of the {@link MaybePagedQueueProcessor}.
     */
    @SuppressWarnings("rawtypes")
    public static final MaybePagedQueueProcessor INSTANCE = new MaybePagedQueueProcessor<>();
    }
