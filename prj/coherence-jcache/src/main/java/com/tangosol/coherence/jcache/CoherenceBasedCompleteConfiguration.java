/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache;

import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Factory;

import javax.cache.expiry.ExpiryPolicy;

import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheWriter;

/**
 * An Coherence-based {@link javax.cache.configuration.CompleteConfiguration} that provides
 * setter methods.
 * <p>
 * {@link CoherenceBasedCache}s that use this configuration must be JCache compliant as
 * JCache {@link javax.cache.Cache}s that use {@link CompleteConfiguration}s are compliant.
 *
 * @author bo  2013.11.12
 * @since Coherence 12.1.3
 *
 * @param <K>  the type of the keys
 * @param <V>  the type of the values
 */
public interface CoherenceBasedCompleteConfiguration<K, V>
        extends CompleteConfiguration<K, V>, CoherenceBasedConfiguration<K, V>
    {
    /**
     * Sets the expected type of keys and values for a {@link CoherenceBasedCache}.
     * <p>
     * Setting both to <code>Object.class</code> means type-safety checks are not required.
     *
     * @param clzKey    the expected key type
     * @param clzValue  the expected value type
     *
     * @throws NullPointerException should the key or value type be null
     */
    public void setTypes(Class<K> clzKey, Class<V> clzValue);

    /**
     * Add a configuration for a {@link javax.cache.event.CacheEntryListener}.
     *
     * @param cfgListener  the {@link javax.cache.configuration.CacheEntryListenerConfiguration}
     *
     * @throws IllegalArgumentException if the same CacheEntryListenerConfiguration
     *                                  is used more than once
     */
    public void addCacheEntryListenerConfiguration(CacheEntryListenerConfiguration<K, V> cfgListener);

    /**
     * Set the {@link javax.cache.integration.CacheLoader} factory.
     *
     * @param factory  the {@link javax.cache.integration.CacheLoader} {@link javax.cache.configuration.Factory}
     */
    public void setCacheLoaderFactory(Factory<? extends CacheLoader<K, V>> factory);

    /**
     * Set the {@link javax.cache.integration.CacheWriter} factory.
     *
     * @param factory  the {@link javax.cache.integration.CacheWriter} {@link javax.cache.configuration.Factory}
     */
    public void setCacheWriterFactory(Factory<? extends CacheWriter<? super K, ? super V>> factory);

    /**
     * Set the {@link javax.cache.configuration.Factory} for the {@link javax.cache.expiry.ExpiryPolicy}.
     * <p>
     * If <code>null</code> is specified the default {@link javax.cache.expiry.ExpiryPolicy} is used.
     *
     * @param factory  the {@link javax.cache.expiry.ExpiryPolicy} {@link javax.cache.configuration.Factory}
     */
    public void setExpiryPolicyFactory(Factory<? extends ExpiryPolicy> factory);

    /**
     * Set if read-through caching should be used.
     * <p>
     * It is an invalid configuration to set this to true without specifying a
     * {@link javax.cache.integration.CacheLoader} {@link javax.cache.configuration.Factory}.
     *
     * @param fReadThrough  <code>true</code> if read-through is required
     */
    public void setReadThrough(boolean fReadThrough);

    /**
     * Set if write-through caching should be used.
     * <p>
     * It is an invalid configuration to set this to true without specifying a
     * {@link javax.cache.integration.CacheWriter} {@link javax.cache.configuration.Factory}.
     *
     * @param fWriteThrough  <code>true</code> if write-through is required
     */
    public void setWriteThrough(boolean fWriteThrough);

    /**
     * Set if a configured cache should use store-by-value or store-by-reference
     * semantics.
     *
     * @param fStoreByValue  <code>true</code> for store-by-value semantics,
     *                       <code>false</code> for store-by-reference semantics
     */
    public void setStoreByValue(boolean fStoreByValue);

    /**
     * Set if statistics gathering is enabled for a configuration.
     * <p>
     * Statistics may be enabled or disabled at runtime via
     * {@link javax.cache.CacheManager#enableStatistics(String, boolean)}.
     *
     * @param fStatisticsEnabled  <code>true</code> to enable statistics gathering,
     *                            <code>false</code> to disable
     */
    public void setStatisticsEnabled(boolean fStatisticsEnabled);

    /**
     * Set if JMX management is enabled for a configuration.
     * <p>
     * Management may be enabled or disabled at runtime via
     * {@link javax.cache.CacheManager#enableManagement(String, boolean)}.
     *
     * @param fManagementEnabled  <code>true</code> to enable statistics,
     *                            <code>false</code> to disable
     */
    public void setManagementEnabled(boolean fManagementEnabled);
    }
