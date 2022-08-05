/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.processors;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An {@link com.tangosol.util.InvocableMap.EntryProcessor} implementation
 * that returns the partition count for the cache service that owns the
 * cache the {@link GetPartitionCount} is executing on.
 * <p>
 * This can be useful when the partition count is required on an Extend client.
 *
 * @param <K>  the type of the cache keys
 * @param <V>  the type of the cache values
 */
public class GetPartitionCount<K, V>
        implements InvocableMap.EntryProcessor<K, V, Integer>,
                   PortableObject, ExternalizableLite
    {
    @Override
    public Integer process(InvocableMap.Entry<K, V> entry)
        {
        CacheService service = entry.asBinaryEntry().getContext().getCacheService();
        return service instanceof DistributedCacheService
                ? ((DistributedCacheService) service).getPartitionCount()
                : -1;
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }
    }
