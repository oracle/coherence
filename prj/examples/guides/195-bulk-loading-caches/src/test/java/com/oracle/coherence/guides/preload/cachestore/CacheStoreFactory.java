/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.cachestore;

import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.net.cache.CacheStore;

@SuppressWarnings("rawtypes")
public class CacheStoreFactory
    {
    @SuppressWarnings("unchecked")
    public static CacheStore createControllableCacheStore(String cacheName, String jdbcURL) throws Exception
        {
        CacheStore cacheStore;
        switch (cacheName.toLowerCase())
            {
            case "customers":
                cacheStore = new CustomerCacheStore(jdbcURL);
                break;
            case "orders":
                cacheStore = new OrderCacheStore(jdbcURL);
                break;
            default:
                throw new IllegalArgumentException("Cannot create cache store for cache " + cacheName);
            }

        ControllableCacheStore.Controller controller = new SimpleController();
        return new ControllableCacheStore<>(controller, cacheStore);
        }

    @SuppressWarnings("unchecked")
    public static BinaryEntryStore createControllableBinaryEntryStore(String cacheName, String jdbcURL) throws Exception
        {
        CacheStore cacheStore;
        switch (cacheName.toLowerCase())
            {
            case "customers":
                cacheStore = new CustomerCacheStore(jdbcURL);
                break;
            case "orders":
                cacheStore = new OrderCacheStore(jdbcURL);
                break;
            default:
                throw new IllegalArgumentException("Cannot create cache store for cache " + cacheName);
            }

        return new SmartCacheStore<>(cacheStore);
        }
    }
