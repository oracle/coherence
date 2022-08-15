/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.cachestore;


import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.guides.preload.processors.GetPartitionCount;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.NamedMap;

import com.tangosol.net.cache.ReadWriteBackingMap;

import com.tangosol.net.partition.SimplePartitionKey;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.ObservableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashSet;
import java.util.Set;

/**
 * A {@link InvocableMap.StreamingAggregator} to determine whether the
 * {@link ControllableCacheStore} for a given cache is enabled or disabled
 * on all members.
 *
 * @param <K>  the type of the cache keys
 * @param <V>  the type of the cache values
 */
public class IsCacheStoreEnabled<K, V>
        implements InvocableMap.StreamingAggregator<K, V, Boolean, Boolean>,
                   PortableObject, ExternalizableLite
    {
    /**
     * Default no-arg constructor for serialization.
     */
    public IsCacheStoreEnabled()
        {
        }

    /**
     * Create a {@link IsCacheStoreEnabled} that verifies the {@link ControllableCacheStore}
     * state is the same as the expected state across the cluster.
     *
     * @param expected  the expected {@link ControllableCacheStore} enabled state
     */
    public IsCacheStoreEnabled(boolean expected)
        {
        this.expected = expected;
        }

    @Override
    public InvocableMap.StreamingAggregator<K, V, Boolean, Boolean> supply()
        {
        return new IsCacheStoreEnabled<>(expected);
        }

    @Override
    @SuppressWarnings({"deprecation", "rawtypes"})
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        Logger.info("Checking cache store key=" + entry.getKey());
        ObservableMap<? extends K, ? extends V> backingMap = entry.asBinaryEntry().getBackingMap();
        if (backingMap instanceof ReadWriteBackingMap)
            {
            ReadWriteBackingMap.StoreWrapper wrapper = ((ReadWriteBackingMap) backingMap).getCacheStore();
            Object o = wrapper.getStore();

            if (o instanceof ControllableCacheStore)
                {
                boolean fEnabled = ((ControllableCacheStore) o).isEnabled();
                Logger.info("Checking cache store key=" + entry.getKey() + " result=" + result + " expected=" + expected + " enabled=" + fEnabled);
                result = expected == fEnabled;
                }
            else
                {
                Logger.info("Checking cache store key=" + entry.getKey() + " not a ControllableCacheStore");
                }
            Logger.info("Checking cache store key=" + entry.getKey() + " result=" + result);
            }
        else
            {
            Logger.info("Checking cache store key=" + entry.getKey() + " not a rwbm");
            result = false;
            }
        return true;
        }

    @Override
    public boolean combine(Boolean partialResult)
        {
        result = result && partialResult;
        return true;
        }

    @Override
    public Boolean getPartialResult()
        {
        return result;
        }

    @Override
    public Boolean finalizeResult()
        {
        boolean finalResult = result;
        result = true;
        return finalResult;
        }

    @Override
    public int characteristics()
        {
        return PARALLEL;
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        expected = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeBoolean(expected);
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        expected = in.readBoolean(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeBoolean(0, expected);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Returns {@code true} if the {@link ControllableCacheStore} is enabled
     * on all members of the cluster.
     *
     * @param map  the {@link NamedMap} to check
     *
     * @return  {@code true} if the {@link ControllableCacheStore} is enabled
     *          on all members of the cluster
     */
    public static boolean isEnabled(NamedMap<?, ?> map)
        {
        return checkState(map, true);
        }

    /**
     * Returns {@code true} if the {@link ControllableCacheStore} is disabled
     * on all members of the cluster.
     *
     * @param map  the {@link NamedMap} to check
     *
     * @return  {@code true} if the {@link ControllableCacheStore} is disabled
     *          on all members of the cluster
     */
    public static boolean isDisabled(NamedMap<?, ?> map)
        {
        return checkState(map, false);
        }

    /**
     * Returns {@code true} if the {@link ControllableCacheStore} is the expected
     * state on all members of the cluster.
     *
     * @param map       the {@link NamedMap} to check
     * @param expected  the expected state
     *
     * @return  {@code true} if the {@link ControllableCacheStore} is enabled
     *          on all members of the cluster
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected static boolean checkState(NamedMap map, boolean expected)
        {
        Logger.info("Checking cache store map=" + map.getName() + " expected=" + expected);
        // If this method is executed on a cluster member the partition count could
        // be obtained directly from the service, but this code will also work on an
        // Extend client.
        SimplePartitionKey key = SimplePartitionKey.getPartitionKey(0);
        Integer partitionCount = (Integer) map.invoke(key, new GetPartitionCount<>());
        if (partitionCount != null && partitionCount > 0)
            {
            Set<SimplePartitionKey> keys = new HashSet<>();
            for (int i = 0; i < partitionCount; i++)
                {
                keys.add(SimplePartitionKey.getPartitionKey(i));
                }

            Boolean isEnabled = (Boolean) map.aggregate(keys, new IsCacheStoreEnabled(expected));
            return isEnabled != null && isEnabled;
            }
        else
            {
            Logger.info("Checking cache store cache=" + map.getName() + " enabled=" + expected + " failed - cannot obtain partition count");

            throw new IllegalStateException("Could not obtain partition count for cache " + map.getName());
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The expected {@link ControllableCacheStore} enabled state.
     */
    private boolean expected;

    /**
     * The aggregator's partial result.
     */
    private transient boolean result = true;
    }
