/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.cachestores;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.cache.CacheLoader;
import com.tangosol.net.cache.CacheStore;

/**
 * A simple {@link CacheLoader} implementation to demonstrate basic functionality.
 *
 * @author Tim Middleton 2020.02.17
 */
// #tag::class[]
public class SimpleCacheStore
        extends SimpleCacheLoader
        implements CacheStore<Integer, String> { // <1>

    /**
     * Constructs a {@link SimpleCacheStore}.
     *
     * @param cacheName cache name
     */
    public SimpleCacheStore(String cacheName) {  // <2>
        super(cacheName);
        Logger.info("SimpleCacheStore instantiated for cache " + cacheName);
    }

    @Override
    public void store(Integer integer, String s) {  // <3>
        Logger.info("Store key " + integer + " with value " + s);
    }

    @Override
    public void erase(Integer integer) {  // <4>
        Logger.info("Erase key " + integer);
    }
}
// #end::class[]