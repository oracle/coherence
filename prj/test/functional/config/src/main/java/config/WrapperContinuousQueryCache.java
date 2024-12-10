/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.filter.AlwaysFilter;

/**
 * A {@link WrapperContinuousQueryCache} is an implementation of a {@link WrapperNamedCache}
 * that internally uses a {@link ContinuousQueryCache} for locally maintaining a cache of another cache.
 *
 * @author Jonathan Knight
 */
public class WrapperContinuousQueryCache extends WrapperNamedCache
    {
    /**
     * Constructs a {@link WrapperContinuousQueryCache}
     *
     * @param wrappedCache  the {@link NamedCache} to wrap
     * @param cacheName     the name of the wrapped cache
     */
    public WrapperContinuousQueryCache(NamedCache wrappedCache,
                                       String cacheName)
        {
        super(new ContinuousQueryCache(wrappedCache, AlwaysFilter.INSTANCE), cacheName);
        }
    }
