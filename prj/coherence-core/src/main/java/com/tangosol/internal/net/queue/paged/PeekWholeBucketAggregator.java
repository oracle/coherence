/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * An instance of a {@link InvocableMap.EntryAggregator} that will
 * return the contents of a bucket within a queue.
 * The elements returned will be in the form of a {@link java.util.List} of
 * {@link com.tangosol.util.Binary} values sorted in the sequential order
 * that the elements were added to the bucket. If the bucket does
 * not exist then null will be returned as the aggregator result.
 */
public class PeekWholeBucketAggregator<K, V>
        implements InvocableMap.StreamingAggregator<K, V, List<Binary>, List<Binary>>, ExternalizableLite, PortableObject

    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for POF
     */
    public PeekWholeBucketAggregator()
        {
        this(true);
        }

    /**
     * Create a new PeekWholeBucketAggregator that will return the contents
     * of a queue bucket in either head first or tail first order.
     *
     * @param fHeadFirst if true, the elements of the bucket are returned head first
     *                   otherwise they are returned tail first.
     */
    public PeekWholeBucketAggregator(boolean fHeadFirst)
        {
        m_fHeadFirst = fHeadFirst;
        }

    // ----- InvocableMap.StreamingAggregator implementation ----------------


    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_fHeadFirst = in.readBoolean(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeBoolean(0, m_fHeadFirst);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_fHeadFirst = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeBoolean(m_fHeadFirst);
        }

    @Override
    public InvocableMap.StreamingAggregator<K, V, List<Binary>, List<Binary>> supply()
        {
        return new PeekWholeBucketAggregator<>(m_fHeadFirst);
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        BinaryEntry<?, ?> binaryEntry = entry.asBinaryEntry();
        if (binaryEntry.isPresent())
            {
            Integer                  bucketId          = (Integer) binaryEntry.getKey();
            BackingMapManagerContext context           = binaryEntry.getContext();
            BackingMapContext        backingMapContext = binaryEntry.getBackingMapContext();
            String                   elementCacheName  = PagedQueueCacheNames.Elements.getCacheName(backingMapContext);
            BackingMapContext        elementMapContext = context.getBackingMapContext(elementCacheName);
            Map                      backingMap        = elementMapContext.getBackingMap();
            Map                      indexes           = elementMapContext.getIndexMap(binaryEntry.getKeyPartition());

            m_listResult = Bucket.findElementsForBucketId(bucketId, backingMap, indexes, context, m_fHeadFirst);
            }
        return true;
        }

    @Override
    public boolean combine(List<Binary> partialResult)
        {
        if (partialResult != null)
            {
            if (m_listResult == null)
                {
                m_listResult = new ArrayList<>();
                }
            m_listResult.addAll(partialResult);
            }
        return true;
        }

    @Override
    public List<Binary> getPartialResult()
        {
        return m_listResult;
        }

    @Override
    public List<Binary> finalizeResult()
        {
        return m_listResult;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Flag indicating whether the aggregator returns elements in head first
     * or tail first order.
     */
    protected boolean m_fHeadFirst;

    protected transient List<Binary> m_listResult = null;
    }
