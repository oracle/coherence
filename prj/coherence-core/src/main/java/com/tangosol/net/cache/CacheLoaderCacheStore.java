/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


/**
* A read-only CacheStore that wraps a CacheLoader.
*
* @author cp  2006.09.06
*
* @since Coherence 3.2
*/
public class CacheLoaderCacheStore<K, V>
        extends AbstractCacheStore<K, V>
    {
    // ----- factory methods ------------------------------------------------

    /**
    * Create a CacheStore wrapper for the passed CacheLoader. Note that the
    * returned CacheStore will implement the IterableCacheLoader interface if
    * and only if the passed CacheLoader implements it.
    *
    * @param <K>     the key type
    * @param <V>     the value type
    * @param loader  the CacheLoader to wrap
    *
    * @return a CacheStore
    */
    public static <K, V> CacheStore<K, V> wrapCacheLoader(CacheLoader<K, V> loader)
        {
        return loader instanceof CacheStore
                ? (CacheStore) loader
                : loader instanceof IterableCacheLoader
                        ? new Iterable<>((IterableCacheLoader<K, V>) loader)
                        : new CacheLoaderCacheStore<>(loader);
        }


    // ----- constructors ---------------------------------------------------

    /**
    * The CacheLoader to delegate to.
    *
    * @param loader  the delegate CacheLoader
    */
    public CacheLoaderCacheStore(CacheLoader<K, V> loader)
        {
        m_loader = loader;
        }


    // ----- CacheLoader interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public V load(K key)
        {
        return m_loader.load(key);
        }

    /**
    * {@inheritDoc}
    */
    public Map<K, V> loadAll(Collection<? extends K> colKeys)
        {
        return m_loader.loadAll(colKeys);
        }


    // ----- inner class: IterableCacheLoaderCacheStore ---------------------

    /**
    * An extension to the CacheLoaderCacheStore that implements the
    * IterableCacheLoader interface.
    *
    * @author cp  2006.09.06
    *
    * @since Coherence 3.2
    */
    public static class Iterable<K, V>
            extends CacheLoaderCacheStore<K, V>
            implements IterableCacheLoader<K, V>
        {
        // ----- constructors -------------------------------------------

        /**
        * The CacheLoader to delegate to.
        *
        * @param loader  the delegate CacheLoader
        */
        public Iterable(IterableCacheLoader<K, V> loader)
            {
            super(loader);
            }


        // ----- IterableCacheLoader interface --------------------------

        /**
        * {@inheritDoc}
        */
        public Iterator<K> keys()
            {
            return ((IterableCacheLoader<K, V>) m_loader).keys();
            }
        }

    // ----- data members ---------------------------------------------------

    /**
    * The CacheLoader to delegate to.
    */
    protected CacheLoader<K, V> m_loader;
    }
