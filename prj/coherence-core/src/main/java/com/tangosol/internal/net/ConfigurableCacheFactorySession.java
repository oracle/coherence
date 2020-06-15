/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.oracle.coherence.common.util.Options;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;
import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.Base;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link Session} that uses a specific {@link ConfigurableCacheFactory}
 * and {@link ClassLoader}.
 *
 * @author bo  2015.07.27
 */
public class ConfigurableCacheFactorySession
        implements Session
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ConfigurableCacheFactorySession}.
     *
     * @param ccf     the {@link ConfigurableCacheFactory}
     * @param loader  the {@link ClassLoader}
     */
    public ConfigurableCacheFactorySession(ConfigurableCacheFactory ccf, ClassLoader loader)
        {
        m_ccf = ccf;
        m_classLoader = loader == null ? Base.ensureClassLoader(null) : loader;
        m_mapCaches   = new ConcurrentHashMap<>();
        m_mapTopics   = new ConcurrentHashMap<>();
        m_fActive     = true;
        }

    // ----- Session interface ----------------------------------------------

    @Override
    public <K, V> NamedMap<K, V> getMap(String sName, NamedMap.Option... options)
        {
        return getCache(sName, options);
        }

    @Override
    public <K, V> NamedCache<K, V> getCache(String sName, NamedCache.Option... options)
        {
        if (m_fActive)
            {
            ResourceRegistry registry = m_ccf.getResourceRegistry();

            // ensure we have a reference counter for the named cache
            registry.registerResource(
                    NamedCacheReferenceCounter.class, sName,
                    NamedCacheReferenceCounter::new,
                    RegistrationBehavior.IGNORE, null);

            // grab the map of caches organized by the type assertion, creating if necessary
            ConcurrentHashMap<TypeAssertion, SessionNamedCache> mapCachesByTypeAssertion =
                    m_mapCaches.computeIfAbsent(sName,
                                                any -> new ConcurrentHashMap<>());

            // grab the type assertion from the provided options
            Options<NamedCache.Option> optionSet = Options.from(NamedCache.Option.class, options);
            TypeAssertion typeAssertion          = optionSet.get(TypeAssertion.class);

            // grab the session-based named cache for the type of assertion, creating one if necessary
            SessionNamedCache<K, V> cacheSession = mapCachesByTypeAssertion.compute(
                    typeAssertion,
                    (key, value) -> {

                        // only return a valid session-based named cache
                        if (value != null && !value.isDestroyed() && !value.isReleased())
                            {
                            return value;
                            }

                        // request an underlying NamedCache from the CCF
                        NamedCache<K, V> cacheUnderlying = m_ccf.ensureTypedCache(
                                sName,
                                m_classLoader,
                                typeAssertion);

                        SessionNamedCache<K, V> cache = new SessionNamedCache<>(this,
                                cacheUnderlying,
                                typeAssertion);

                        // increment the reference count for the underlying NamedCache
                        registry.getResource(NamedCacheReferenceCounter.class,
                                sName).incrementAndGet();

                        return cache;
                    });

            return cacheSession;
            }
        else
            {
            throw new IllegalStateException("Session is closed");
            }
        }


    @Override
    @SuppressWarnings("unchecked")
    public <V> NamedTopic<V> getTopic(String sName, NamedTopic.Option... options)
        {
        assertActive();

        ResourceRegistry registry = m_ccf.getResourceRegistry();

        // ensure we have a reference counter for the named topic
        registry.registerResource(
                NamedTopicReferenceCounter.class, sName,
                NamedTopicReferenceCounter::new,
                RegistrationBehavior.IGNORE, null);

        // grab the map of topics organized by the type assertion, creating if necessary
        ConcurrentHashMap<ValueTypeAssertion, SessionNamedTopic> mapTopicsByTypeAssertion =
                m_mapTopics.computeIfAbsent(sName, any -> new ConcurrentHashMap<>());

        // grab the type assertion from the provided options
        Options<NamedTopic.Option> optionSet     = Options.from(NamedTopic.Option.class, options);
        ValueTypeAssertion<V>      typeAssertion = optionSet.get(ValueTypeAssertion.class);

        SessionNamedTopic<V>  topicSession = mapTopicsByTypeAssertion.computeIfAbsent(
            typeAssertion,
            any -> {
            try
                {
                // increment the reference count for the underlying NamedTopic
                registry.getResource(NamedTopicReferenceCounter.class, sName).incrementAndGet();

                // request an underlying NamedTopic from the CCF
                NamedTopic<V> topicUnderlying = m_ccf.ensureTopic(sName, m_classLoader, typeAssertion);

                return new SessionNamedTopic<>(this, topicUnderlying, typeAssertion);
                }
            catch (Exception e)
                {
                // when we can't acquire the NamedTopic,
                // ensure we don't impact the reference count
                registry.getResource(NamedTopicReferenceCounter.class, sName).decrementAndGet();

                throw e;
                }
            });

        return topicSession;
        }

    @Override
    public synchronized void close()
            throws Exception
        {
        if (m_fActive)
            {
            m_fActive = false;

            // close all of the named caches created by this session
            m_mapCaches.values().stream()
                       .flatMap(
                               mapCachesByAssertion -> mapCachesByAssertion.values().stream())
                       .forEach(SessionNamedCache::close);

            // drop the references to caches
            m_mapCaches.clear();

            // close all of the named topics created by this session
            m_mapTopics.values().stream()
                       .flatMap(mapCachesByAssertion -> mapCachesByAssertion.values().stream())
                       .forEach(SessionNamedTopic::close);

            // drop the references to topic
            m_mapTopics.clear();
            }
        }

    // ----- ConfigurableCacheFactorySession methods ------------------------

    private void assertActive()
        {
        if (!m_fActive)
            {
            throw new IllegalStateException("Session is closed");
            }
        }

    private <K, V> SessionNamedCache createSessionNamedCache(String sName, TypeAssertion<K, V> typeAssertion,
                                                             ResourceRegistry registry)
        {
        try
            {
            // increment the reference count for the underlying NamedCache
            registry.getResource(NamedCacheReferenceCounter.class,
                                 sName).incrementAndGet();

            // request an underlying NamedCache from the CCF
            NamedCache<K, V> cacheUnderlying = m_ccf.ensureTypedCache(
                    sName,
                    m_classLoader,
                    typeAssertion);

            return new SessionNamedCache<>(this,
                                           cacheUnderlying,
                                           typeAssertion);
            }
        catch (Exception e)
            {
            // when we can't acquire the NamedCache,
            // ensure we don't impact the reference count
            registry.getResource(NamedCacheReferenceCounter.class,
                                 sName).decrementAndGet();

            throw e;
            }
        }

    private <V> SessionNamedTopic<V> createSessionNamedTopic(String sName, ValueTypeAssertion<V> typeAssertion,
            ResourceRegistry registry)
        {
        try
            {
            // increment the reference count for the underlying NamedTopic
            registry.getResource(NamedTopicReferenceCounter.class, sName).incrementAndGet();

            // request an underlying NamedTopic from the CCF
            NamedTopic<V> topicUnderlying = m_ccf.ensureTopic(sName, m_classLoader, typeAssertion);

            return new SessionNamedTopic<>(this, topicUnderlying, typeAssertion);
            }
        catch (Exception e)
            {
            // when we can't acquire the NamedTopic,
            // ensure we don't impact the reference count
            registry.getResource(NamedTopicReferenceCounter.class, sName).decrementAndGet();

            throw e;
            }
        }

    <V> void onClose(SessionNamedTopic<V> topic)
        {
        dropNamedTopic(topic);

        topic.onClosing();

        // decrement the reference count for the underlying NamedCache
        ResourceRegistry registry = m_ccf.getResourceRegistry();

        if (registry.getResource(NamedTopicReferenceCounter.class, topic.getName()).decrementAndGet() == 0)
            {
            m_ccf.releaseTopic(topic.getInternalNamedTopic());
            }

        topic.onClosed();
        }

    <V> void onDestroy(SessionNamedTopic<V> topic)
        {
        dropNamedTopic(topic);

        topic.onDestroying();

        // reset the reference count for the underlying NamedCache
        ResourceRegistry registry = m_ccf.getResourceRegistry();

        registry.getResource(NamedTopicReferenceCounter.class, topic.getName()).reset();

        m_ccf.destroyTopic(topic.getInternalNamedTopic());

        topic.onDestroyed();
        }

    <V> void dropNamedTopic(SessionNamedTopic<V> namedCache)
        {
        // drop the NamedCache from this session
        ConcurrentHashMap<TypeAssertion, SessionNamedCache> mapCachesByTypeAssertion =
                m_mapCaches.get(namedCache.getName());

        if (mapCachesByTypeAssertion != null)
            {
            mapCachesByTypeAssertion.remove(namedCache.getTypeAssertion());
            }
        }

    <K, V> void onClose(SessionNamedCache<K, V> namedCache)
        {
        dropNamedCache(namedCache);

        namedCache.onClosing();

        // decrement the reference count for the underlying NamedCache
        ResourceRegistry registry = m_ccf.getResourceRegistry();

        if (registry.getResource(NamedCacheReferenceCounter.class,
                                 namedCache.getCacheName()).decrementAndGet() == 0)
            {
            m_ccf.releaseCache(namedCache.getInternalNamedCache());
            }

        namedCache.onClosed();
        }

    <K, V> void onDestroy(SessionNamedCache<K, V> namedCache)
        {
        dropNamedCache(namedCache);

        namedCache.onDestroying();

        // reset the reference count for the underlying NamedCache
        ResourceRegistry registry = m_ccf.getResourceRegistry();

        registry.getResource(NamedCacheReferenceCounter.class,
                                 namedCache.getCacheName()).reset();

        m_ccf.destroyCache(namedCache.getInternalNamedCache());

        namedCache.onDestroyed();
        }

    <K, V> void dropNamedCache(SessionNamedCache<K, V> namedCache)
        {
        // drop the NamedCache from this session
        ConcurrentHashMap<TypeAssertion, SessionNamedCache> mapCachesByTypeAssertion =
                m_mapCaches.get(namedCache.getCacheName());

        if (mapCachesByTypeAssertion != null)
            {
            mapCachesByTypeAssertion.remove(namedCache.getTypeAssertion());
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ConfigurableCacheFactory} on which the {@link Session}
     * is based.
     */
    private ConfigurableCacheFactory m_ccf;

    /**
     * The {@link ClassLoader} for the {@link Session}.
     */
    private ClassLoader m_classLoader;

    /**
     * Is the {@link Session} active / available for use.
     */
    private volatile boolean m_fActive;

    /**
     * The {@link SessionNamedCache}s created by this {@link Session}
     * (so we close them when the session closes).
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<TypeAssertion, SessionNamedCache>> m_mapCaches;

    /**
     * The {@link NamedTopic}s created by this {@link Session}
     * (so we close them when the session closes).
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<ValueTypeAssertion, SessionNamedTopic>> m_mapTopics;

    /**
     * Tracks use of individual  instances by all {@link Session}s
     * against the same {@link ConfigurableCacheFactory} (by using reference counting)
     */
    protected static class ReferenceCounter
        {
        private AtomicInteger count;

        public ReferenceCounter()
            {
            count = new AtomicInteger(0);
            }

        public int get()
            {
            return count.get();
            }

        public int incrementAndGet()
            {
            return count.incrementAndGet();
            }

        public int decrementAndGet()
            {
            return count.decrementAndGet();
            }

        public void reset()
            {
            count.set(0);
            }
        }

    /**
     * Tracks use of individual {@link NamedCache} instances by all {@link Session}s
     * against the same {@link ConfigurableCacheFactory} (by using reference counting)
     */
    protected static class NamedCacheReferenceCounter
            extends ReferenceCounter
        {}

    /**
     * Tracks use of individual {@link NamedTopic} instances by all {@link Session}s
     * against the same {@link ConfigurableCacheFactory} (by using reference counting)
     */
    protected static class NamedTopicReferenceCounter
            extends ReferenceCounter
        {}
    }