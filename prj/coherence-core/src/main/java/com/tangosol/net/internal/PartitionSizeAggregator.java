/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.function.Remote;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Internal aggregator used to calculate partition size across all partitions for a cache.
 * 
 * @author as 2024.01.28
 * @since 24.03
 */
public class PartitionSizeAggregator<K, V>
        implements InvocableMap.StreamingAggregator<K, V, Map<Integer, PartitionSize>, SortedSet<PartitionSize>>,
        ExternalizableLite, PortableObject
    {
    // ---- InvocableMap.StreamingAggregator interface ----------------------

    @Override
    public InvocableMap.StreamingAggregator<K, V, Map<Integer, PartitionSize>, SortedSet<PartitionSize>> supply()
        {
        return new PartitionSizeAggregator<>();
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        BinaryEntry<? extends K, ? extends V> binEntry    = entry.asBinaryEntry();
        int                                   partitionId = binEntry.getBackingMapContext().getManagerContext().getKeyPartition(binEntry.getBinaryKey());
        PartitionSize partSize = mapResults.computeIfAbsent(partitionId, PartitionSize::new);
        partSize.accumulate(binEntry);
        return true;
        }

    @Override
    public boolean combine(Map<Integer, PartitionSize> partialResult)
        {
        for (PartitionSize partSizeOther : partialResult.values())
            {
            PartitionSize partSizeThis = mapResults.computeIfAbsent(partSizeOther.getPartitionId(), PartitionSize::new);
            partSizeThis.combine(partSizeOther);
            }
        return true;
        }

    @Override
    public Map<Integer, PartitionSize> getPartialResult()
        {
        return mapResults;
        }

    @Override
    public SortedSet<PartitionSize> finalizeResult()
        {
        SortedSet<PartitionSize> result = new TreeSet<>(Remote.comparator(PartitionSize::getPartitionId));
        result.addAll(mapResults.values());
        return result;
        }

    @Override
    public int characteristics()
        {
        return PARALLEL;
        }

    // ---- ExternalizableLite interface ------------------------------------

    public void readExternal(DataInput in) throws IOException
        {
        // nothing to read
        }

    public void writeExternal(DataOutput out) throws IOException
        {
        // nothing to write
        }

    // ---- PortableObject interface ----------------------------------------

    public void readExternal(PofReader in) throws IOException
        {
        // nothing to read
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        // nothing to write
        }

    // ----- data members ---------------------------------------------------

    private transient Map<Integer, PartitionSize> mapResults = new HashMap<>();
    }