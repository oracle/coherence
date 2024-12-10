/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.passthroughcache;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.jcache.CoherenceBasedCache;
import com.tangosol.coherence.jcache.CoherenceBasedCacheManager;
import com.tangosol.coherence.jcache.CoherenceBasedConfiguration;

import javax.cache.Cache;

import javax.cache.configuration.Configuration;
import javax.cache.configuration.CompleteConfiguration;

/**
 * A {@link javax.cache.configuration.Configuration} for a
 * {@link javax.cache.Cache} based on an existing Coherence
 * {@link com.tangosol.net.NamedCache}.
 * <p>
 * {@link Cache}s produced according to this configuration are not expected to be
 * JCache-compliant.   They simply provide a JCache interface (aka: wrapper) over
 * native Coherence {@link com.tangosol.net.NamedCache}s.
 *
 * @param <K>  the type of the {@link Cache} keys
 * @param <V>  the type of the {@link Cache} values
 *
 * @author bo  2013.10.23
 * @since Coherence 12.1.3
 */
public class PassThroughCacheConfiguration<K, V>
        implements CoherenceBasedConfiguration<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link PassThroughCacheConfiguration} that defaults
     * to using the {@link Cache} name as the underlying
     * {@link com.tangosol.net.NamedCache} name with Object-based key and
     * value types.
     */
    public PassThroughCacheConfiguration()
        {
        // by default use the JCache name as the NamedCache name
        m_sNamedCacheName = null;

        // by default the key and value types will be Object
        m_clzKey   = (Class<K>) Object.class;
        m_clzValue = (Class<V>) Object.class;
        }

    /**
     * Constructs a <tt>PassThroughConfiguration</tt> based on a JCache {@link CompleteConfiguration}.
     * <p>
     *
     * WARNING: This is a lossy construction and a warning will be emitted when this method
     * is called. Most {@link CompleteConfiguration} properties are ignored. Native Coherence
     * configuration must be used when using {@link PassThroughCache};
     *
     * @param configuration  configuration properties of a JCache {@link CompleteConfiguration}. Note
     *                       most of these properties are ignored.
     */
    public PassThroughCacheConfiguration(CompleteConfiguration<K, V> configuration)
        {
        m_sNamedCacheName = null;
        m_clzKey          = configuration.getKeyType();
        m_clzValue        = configuration.getValueType();

        final String CONFIG_CLASS_NAME = configuration.getClass().getCanonicalName();

        Logger.warn("WARNING: Lossy conversion of configuration " + CONFIG_CLASS_NAME
                    + " to a PassThroughConfiguration. " + "Most properties from class " + CONFIG_CLASS_NAME
                    + " are ignored. "
                    + "Configure PassThroughCache using native Coherence configuration methodologies.");
        }

    /**
     * Constructs a {@link PassThroughCacheConfiguration} based on a
     * provided {@link PassThroughCacheConfiguration}.
     *
     * @param configuration  the {@link PassThroughCacheConfiguration}
     */
    public PassThroughCacheConfiguration(PassThroughCacheConfiguration<K, V> configuration)
        {
        if (configuration == null)
            {
            throw new NullPointerException("The provide configuration can't be null");
            }
        else
            {
            m_sNamedCacheName = configuration.getNamedCacheName();
            m_clzKey          = configuration.getKeyType();
            m_clzValue        = configuration.getValueType();
            }
        }

    // ----- PassThroughCacheConfiguration methods --------------------------

    /**
     * Sets the desired Coherence {@link com.tangosol.net.NamedCache}
     * name to map to when specifying a JCache {@link Cache} name
     *
     * @param sNamedCacheName  the desired {@link com.tangosol.net.NamedCache}
     *                         (or <code>null</code> to default to the JCache name)
     *
     * @return  the {@link PassThroughCacheConfiguration} to support fluent-style calls
     */
    public PassThroughCacheConfiguration<K, V> setNamedCacheName(String sNamedCacheName)
        {
        m_sNamedCacheName = sNamedCacheName;

        return this;
        }

    /**
     * Obtains the desired mapping of a JCache {@link Cache} name to an
     * Coherence {@link com.tangosol.net.NamedCache}.
     *
     * @return  the {@link com.tangosol.net.NamedCache} name to use
     */
    public String getNamedCacheName()
        {
        return m_sNamedCacheName;
        }

    /**
     * Sets the expected type of keys and values for a {@link PassThroughCache}
     * configured with this {@link Configuration}. Setting both to
     * <code>Object.class</code> means type-safety checks are not required
     * (which is the default)
     *
     * @param clzKey   the expected key type
     * @param clzValue the expected value type
     * @return the {@link PassThroughCacheConfiguration} to permit fluent-style method calls
     * @throws NullPointerException should the key or value type be null
     */
    public PassThroughCacheConfiguration<K, V> setTypes(Class<K> clzKey, Class<V> clzValue)
        {
        if (clzKey == null || clzValue == null)
            {
            throw new NullPointerException("The key and/or value types can't be null");
            }
        else
            {
            m_clzKey   = clzKey;
            m_clzValue = clzValue;

            return this;
            }
        }

    // ----- Configuration methods ------------------------------------------

    @Override
    public Class<K> getKeyType()
        {
        return m_clzKey;
        }

    @Override
    public Class<V> getValueType()
        {
        return m_clzValue;
        }

    @Override
    public boolean isStoreByValue()
        {
        throw new UnsupportedOperationException("Unsupported by the " + this.getClass().getName() + " class");
        }

    // ----- CoherenceCacheConfiguration methods ----------------------------

    @Override
    public CoherenceBasedCache<K, V> createCache(CoherenceBasedCacheManager manager, String sJCacheName)
            throws IllegalArgumentException
        {
        return new PassThroughCache<K, V>(manager, sJCacheName, this);
        }

    @Override
    public void destroyCache(CoherenceBasedCacheManager manager, String name)
        {
        // not meaningful to implement. use CacheManager.destroy.
        throw new UnsupportedOperationException("not implemented");
        }

    // ------ data members --------------------------------------------------

    /**
     * The name of the NameCache to which the JCache will map. When <code>null</code>
     * the implementation should use the JCache name as the NamedCache name.
     */
    private String m_sNamedCacheName;

    /**
     * The type of the values in the cache.
     */
    private Class<K> m_clzKey;

    /**
     * The type of the values in the cache.
     */
    private Class<V> m_clzValue;
    }
