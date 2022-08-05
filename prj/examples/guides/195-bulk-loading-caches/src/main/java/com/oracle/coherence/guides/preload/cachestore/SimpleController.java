/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.cachestore;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
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
 * A simple implementation of {@link ControllableCacheStore.Controller}
 * that uses a {@code boolean} field to store the enabled state of the
 * controller.
 */
public class SimpleController
        implements ControllableCacheStore.Controller
    {
    @Override
    public boolean isEnabled()
        {
        return enabled;
        }

    /**
     * Set whether this controller is enabled ({@code true})
     * or disabled ({@code false}).
     *
     * @param enabled {@code true} to enable this controller or
     *                {@code false} to disable this controller
     */
    public void setEnabled(boolean enabled)
        {
        this.enabled = enabled;
        }

    /**
     * Enable cache stores for the specified cache.
     * <p>
     * The cache should be configured to use a read write backing map, with
     * a {@link ControllableCacheStore} that uses a {@link SimpleController}
     * as its controller.
     *
     * @param namedMap  the map to enable or disable cache stores on
     *
     * @throws IllegalArgumentException if the {@code namedMap} parameter is not configured
     *                                  as a distributed cache
     */
    public static void enableCacheStores(NamedMap<?, ?> namedMap)
        {
        setEnabled(namedMap, false);
        }

    /**
     * Disable cache stores for the specified cache.
     * <p>
     * The cache should be configured to use a read write backing map, with
     * a {@link ControllableCacheStore} that uses a {@link SimpleController}
     * as its controller.
     *
     * @param namedMap  the map to enable or disable cache stores on
     *
     * @throws IllegalArgumentException if the {@code namedMap} parameter is not configured
     *                                  as a distributed cache
     */
    public static void disableCacheStores(NamedMap<?, ?> namedMap)
        {
        setEnabled(namedMap, false);
        }

    /**
     * Enable or disable cache stores for the specified cache.
     * <p>
     * The cache should be configured to use a read write backing map, with
     * a {@link ControllableCacheStore} that uses a {@link SimpleController}
     * as its controller.
     *
     * @param namedMap  the map to enable or disable cache stores on
     * @param enabled   {@code true} to enable cache stores, or {@code false}
     *                  to disable cache stores
     *
     * @throws IllegalArgumentException if the {@code namedMap} parameter is not configured
     *                                  as a distributed cache
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected static void setEnabled(NamedMap namedMap, boolean enabled)
        {
        CacheService service = namedMap.getService();
        if (service instanceof DistributedCacheService)
            {
            int partitionCount = ((DistributedCacheService) service).getPartitionCount();
            Set<SimplePartitionKey> keys = new HashSet<>();
            for (int i = 0; i < partitionCount; i++)
                {
                keys.add(SimplePartitionKey.getPartitionKey(i));
                }
            namedMap.invokeAll(keys, new SetEnabledProcessor(enabled));
            }
        else
            {
            throw new IllegalArgumentException(namedMap + " is not a distributed cache");
            }
        }

    // ----- inner class: SetEnabledProcessor -------------------------------

    /**
     * An {@link com.tangosol.util.InvocableMap.EntryProcessor} that enables or disables
     * the {@link SimpleController} for a cache.
     *
     * @param <K>  the type of the cache keys
     * @param <V>  the type of the cache values
     */
    public static class SetEnabledProcessor<K, V>
            implements InvocableMap.EntryProcessor<K, V, Void>, PortableObject, ExternalizableLite
        {
        /**
         * Default no-args constructor for serialization.
         */
        public SetEnabledProcessor()
            {
            }

        /**
         * Create a {@link SetEnabledProcessor}.
         *
         * @param enabled  {@code true} to enable cache stores, or {@link false} to
         *                 disable cache stores
         */
        public SetEnabledProcessor(boolean enabled)
            {
            this.enabled = enabled;
            }

        @Override
        @SuppressWarnings("rawtypes")
        public Void process(InvocableMap.Entry<K, V> entry)
            {
            ObservableMap<? extends K, ? extends V> backingMap = entry.asBinaryEntry().getBackingMap();
            if (backingMap instanceof ReadWriteBackingMap)
                {
                ReadWriteBackingMap.StoreWrapper wrapper = ((ReadWriteBackingMap) backingMap).getCacheStore();
                Object o = wrapper.getStore();

                if (o instanceof ControllableCacheStore)
                    {
                    ControllableCacheStore.Controller controller = ((ControllableCacheStore) o).getController();
                    if (controller instanceof SimpleController)
                        {
                        ((SimpleController) controller).setEnabled(enabled);
                        }
                    }
                }
            return null;
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            enabled = in.readBoolean();
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeBoolean(enabled);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            enabled = in.readBoolean(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeBoolean(0, enabled);
            }

        // ----- data members ---------------------------------------------------

        /**
         * A flag indicating whether cache stores are enabled ({@code true}) or disabled ({@code false}).
         */
        private boolean enabled;
        }

    // ----- data members ---------------------------------------------------

    /**
     * A flag indicating whether cache stores are enabled ({@code true}) or disabled ({@code false}).
     */
    private boolean enabled = true;
    }
