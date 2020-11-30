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
import com.tangosol.net.Service;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.net.events.InterceptorRegistry;

import com.tangosol.net.options.WithClassLoader;

import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.Base;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tangosol.util.BuilderHelper.using;

/**
 * A {@link Session} that uses a specific {@link ConfigurableCacheFactory}
 * and {@link ClassLoader}.
 *
 * @author bo  2015.07.27
 */
@SuppressWarnings("rawtypes")
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
        this(ccf, loader, null);
        }

    /**
     * Constructs a named {@link ConfigurableCacheFactorySession}.
     *
     * @param ccf     the {@link ConfigurableCacheFactory}
     * @param loader  the {@link ClassLoader}
     * @param sName   the optional name of this session
     */
    public ConfigurableCacheFactorySession(ConfigurableCacheFactory ccf, ClassLoader loader, String sName)
        {
        m_ccf         = ccf;
        m_classLoader = loader == null ? Base.ensureClassLoader(null) : loader;
        f_sName       = sName;
        m_mapCaches   = new ConcurrentHashMap<>();
        m_mapTopics   = new ConcurrentHashMap<>();
        m_fActive     = true;

        // if there is a session name add it as a resource to the CCF resource registry
        if (f_sName != null)
            {
            ResourceRegistry registry      = ccf.getResourceRegistry();
            String           sNameExisting = registry.getResource(String.class, SESSION_NAME);
            if (sNameExisting == null)
                {
                registry.registerResource(String.class, SESSION_NAME, f_sName);
                }
            else if (!sNameExisting.equals(f_sName))
                {
                throw new IllegalStateException("Failed to register Session name " + f_sName +
                                                " with ConfigurableCacheFactory, a different Session name " +
                                                sNameExisting + " has already been registered");
                }
            }
        }

    // ----- Session interface ----------------------------------------------

    @Override
    public <K, V> NamedMap<K, V> getMap(String sName, NamedMap.Option... options)
        {
        return getCache(sName, options);
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
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

            // grab the type assertion and ClassLoader from the provided options
            Options<NamedCache.Option> optionSet     = Options.from(NamedCache.Option.class, options);
            TypeAssertion              typeAssertion = optionSet.get(TypeAssertion.class);
            ClassLoader                loader        = optionSet.get(WithClassLoader.class,
                                                                     WithClassLoader.using(m_classLoader))
                                                                .getClassLoader();

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
                                loader,
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
    @SuppressWarnings({"unchecked", "rawtypes"})
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

        // grab the type assertion and ClassLoader from the provided options
        Options<NamedTopic.Option> optionSet     = Options.from(NamedTopic.Option.class, options);
        ValueTypeAssertion<V>      typeAssertion = optionSet.get(ValueTypeAssertion.class);
        ClassLoader                loader        = optionSet.get(WithClassLoader.class,
                                                                  WithClassLoader.using(m_classLoader))
                                                            .getClassLoader();

        SessionNamedTopic<V>       topicSession  = mapTopicsByTypeAssertion.computeIfAbsent(
            typeAssertion,
            any -> {
            try
                {
                // increment the reference count for the underlying NamedTopic
                registry.getResource(NamedTopicReferenceCounter.class, sName).incrementAndGet();

                // request an underlying NamedTopic from the CCF
                NamedTopic<V> topicUnderlying = m_ccf.ensureTopic(sName, loader, typeAssertion);

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

    @Override
    public ResourceRegistry getResourceRegistry()
        {
        return m_ccf.getResourceRegistry();
        }

    @Override
    public InterceptorRegistry getInterceptorRegistry()
        {
        return m_ccf.getInterceptorRegistry();
        }

    @Override
    public boolean isCacheActive(String sCacheName, ClassLoader loader)
        {
        return m_ccf.isCacheActive(sCacheName, loader);
        }

    @Override
    public boolean isMapActive(String sMapName, ClassLoader loader)
        {
        return m_ccf.isCacheActive(sMapName, loader);
        }

    @Override
    public boolean isTopicActive(String sTopicName, ClassLoader loader)
        {
        return m_ccf.isTopicActive(sTopicName, loader);
        }

    @Override
    public String getName()
        {
        return f_sName;
        }

    @Override
    public String getScopeName()
        {
        return m_ccf.getScopeName();
        }

    @Override
    public boolean isActive()
        {
        return m_fActive;
        }

    @Override
    public Service getService(String sServiceName)
        {
        return m_ccf.ensureService(sServiceName);
        }

    // ----- ConfigurableCacheFactory.Supplier methods ----------------------

    /**
     * Return the {@link ConfigurableCacheFactory} that this session wraps.
     *
     * @return  the {@link ConfigurableCacheFactory} that this session wraps
     */
    public ConfigurableCacheFactory getConfigurableCacheFactory()
        {
        return m_ccf;
        }

    // ----- ConfigurableCacheFactorySession methods ------------------------

    private void assertActive()
        {
        if (!m_fActive)
            {
            throw new IllegalStateException("Session is closed");
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

    @SuppressWarnings("rawtypes")
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

    @SuppressWarnings("rawtypes")
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

    // ----- constants ------------------------------------------------------

    /**
     * The key used to register the session name in the {@link ConfigurableCacheFactory}
     * resource registry.
     */
    public static final String SESSION_NAME = "$SESSION$";

    // ----- data members ---------------------------------------------------

    /**
     * The optional name of this {@link Session}
     */
    private final String f_sName;

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