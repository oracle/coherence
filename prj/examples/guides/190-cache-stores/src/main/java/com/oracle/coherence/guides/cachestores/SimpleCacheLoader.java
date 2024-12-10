/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.cachestores;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.cache.CacheLoader;

/**
 * A simple {@link CacheLoader} implementation to demonstrate basic functionality.
 *
 * @author Tim Middleton 2020.02.17
 */
// #tag::class[]
public class SimpleCacheLoader implements CacheLoader<Integer, String> { // <1>

    private String cacheName;

    /**
     * Constructs a {@link SimpleCacheLoader}.
     *
     * @param cacheName cache name
     */
    public SimpleCacheLoader(String cacheName) {  // <2>
        this.cacheName = cacheName;
        Logger.info("SimpleCacheLoader constructed for cache " + this.cacheName);
    }

    /**
     * An implementation of a load which returns the String "Number " + the key.
     *
     * @param key key whose associated value is to be returned
     * @return the value for the given key
     */
    @Override
    public String load(Integer key) {  // <3>
        Logger.info("load called for key " + key);
        return "Number " + key;
    }
}
// #end::class[]