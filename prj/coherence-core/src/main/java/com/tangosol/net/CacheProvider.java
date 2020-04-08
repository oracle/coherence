/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.tangosol.net.cache.TypeAssertion;

import static com.tangosol.net.cache.TypeAssertion.withRawTypes;


/**
* WARNING: Do not use this interface.  It is no longer used internally and will
* be removed in the future.  Use {@link Session} interface instead.
* <p>
* A deprecated interface for cache service provider.
*
* @see Session
*
* @author lh 2015.06.29
*
* @since Coherence 12.2.1
*/
@Deprecated
public interface CacheProvider
    {
    /**
    * Ensure an Object-based cache for the given name.
    *
    * @param sCacheName  the cache name
    * @param loader      ClassLoader that should be used to deserialize
    *                    objects in the cache
    *
    * @return  a NamedCache created
    */
    public default NamedCache<Object, Object> ensureCache(String sCacheName, ClassLoader loader)
        {
        return ensureTypedCache(sCacheName, loader, withRawTypes());
        }

    /**
    * Ensure a cache for the given name satisfying the specified type assertion.
    *
    * @param sCacheName  the cache name
    * @param loader      the {@link ClassLoader} to use for deserializing
    *                    cache entries
    * @param assertion   the {@link TypeAssertion}
     *                   for asserting the type of keys and values for the
     *                   NamedCache
    *
    * @return  a NamedCache created
    *
    * @since Coherence 12.2.1
    */
    public <K, V> NamedCache<K, V> ensureTypedCache(String sCacheName, ClassLoader loader,
        TypeAssertion<K, V> assertion);
    }
