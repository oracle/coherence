/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.tangosol.net.cache.CacheStore;

/**
 * A {@link CacheStore} implementation to use as a custom cluster resource.
 *
 * @param <K>  the type of the cache keys
 * @param <V>  the type of the cache values
 */
public class CustomStore<K, V>
        implements CacheStore<K, V>
    {
    // ----- constructors ---------------------------------------------------

    public CustomStore()
        {
        this(null);
        }

    public CustomStore(String sParam)
        {
        m_sParam = sParam;
        }

    // ----- factory --------------------------------------------------------

    public static <K, V> CustomStore<K, V> create()
        {
        return create(null);
        }

    public static <K, V> CustomStore<K, V> create(String sParam)
        {
        return new CustomStore<>(sParam);
        }

    // ----- accessors ------------------------------------------------------

    public String getParam()
        {
        return m_sParam;
        }

    // ----- CacheStore methods ---------------------------------------------

    @Override
    public V load(K key)
        {
        return null;
        }

    @Override
    public void store(K key, V value)
        {
        }

    @Override
    public void erase(K key)
        {
        }

    // ----- data members ---------------------------------------------------

    private final String m_sParam;
    }
