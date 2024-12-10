/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.util.Options;

import com.tangosol.net.Coherence;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Service;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.EventDispatcherRegistry;
import com.tangosol.net.events.InterceptorRegistry;

import com.tangosol.net.events.application.LifecycleEvent;

import com.tangosol.net.events.internal.ConfigurableCacheFactoryDispatcher;
import com.tangosol.net.events.internal.SessionEventDispatcher;

import com.tangosol.net.options.WithClassLoader;

import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.Base;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
        f_ccf             = ccf;
        f_classLoader     = loader == null ? Base.ensureClassLoader(null) : loader;
        f_sName           = sName;
        f_mapCaches       = new ConcurrentHashMap<>();
        f_mapTopics       = new ConcurrentHashMap<>();
        f_eventDispatcher = new SessionEventDispatcher(this);
        f_registry        = f_ccf.getResourceRegistry();

        // if there is a session name add it as a resource to the CCF resource registry
        if (f_sName != null && !Coherence.DEFAULT_NAME.equals(sName))
            {
            ResourceRegistry registry      = ccf.getResourceRegistry();
            String           sNameExisting = registry.getResource(String.class, SESSION_NAME);
            if (sNameExisting == null)
                {
                registry.registerResource(String.class, SESSION_NAME, f_sName);
                }
            else if (!sNameExisting.equals(f_sName))
                {
                throw new IllegalStateException("Failed to register Session name \"" + f_sName +
                                                "\" with ConfigurableCacheFactory, a different Session name \"" +
                                                sNameExisting + "\" has already been registered. This could be caused " +
                                                "by multiple sessions configured with the same scope name and " +
                                                "configuration URI");
                }
            }

        if (f_ccf instanceof ExtensibleConfigurableCacheFactory)
            {
            EventDispatcherRegistry dispatcherReg = f_registry.getResource(EventDispatcherRegistry.class);
            dispatcherReg.registerEventDispatcher(f_eventDispatcher);
            // register an interceptor so that this session will close if the CCF is disposed
            f_eventInterceptorId = f_ccf.getInterceptorRegistry().registerEventInterceptor(new LifecycleInterceptor());
            }
        else
            {
            f_eventInterceptorId = null;
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
        if (!m_fClosed)
            {
            // ensure we have a reference counter for the named cache
            f_registry.registerResource(
                    NamedCacheReferenceCounter.class, sName,
                    NamedCacheReferenceCounter::new,
                    RegistrationBehavior.IGNORE, null);

            // grab the map of caches organized by the type assertion, creating if necessary
            ConcurrentHashMap<TypeAssertion, SessionNamedCache> mapCachesByTypeAssertion =
                    f_mapCaches.computeIfAbsent(sName,
                                                any -> new ConcurrentHashMap<>());

            // grab the type assertion and ClassLoader from the provided options
            Options<NamedCache.Option> optionSet     = Options.from(NamedCache.Option.class, options);
            TypeAssertion              typeAssertion = optionSet.get(TypeAssertion.class);
            ClassLoader                loader        = optionSet.get(WithClassLoader.class,
                                                                     WithClassLoader.using(f_classLoader))
                                                                .getClassLoader();

            // grab the session-based named cache for the type of assertion, creating one if necessary
            return mapCachesByTypeAssertion.compute(typeAssertion, (key, value) ->
                {
                // only return a valid session-based named cache
                if (value != null
                    && !value.isDestroyed()
                    && !value.isReleased()
                    && value.getContextClassLoader() == loader) // compare loaders to prevent returning incorrect cache
                    {
                    return value;
                    }

                // request an underlying NamedCache from the CCF
                NamedCache<K, V> cacheUnderlying = f_ccf.ensureTypedCache(
                        sName,
                        loader,
                        typeAssertion);

                SessionNamedCache<K, V> cache = new SessionNamedCache<>(this,
                        cacheUnderlying,
                        loader,
                        typeAssertion);

                // increment the reference count for the underlying NamedCache
                f_registry.getResource(NamedCacheReferenceCounter.class, sName).incrementAndGet();

                return cache;
                });
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
        if (!m_fClosed)
            {
            // ensure we have a reference counter for the named topic
            f_registry.registerResource(
                    NamedTopicReferenceCounter.class, sName,
                    NamedTopicReferenceCounter::new,
                    RegistrationBehavior.IGNORE, null);

            // grab the map of topics organized by the type assertion, creating if necessary
            ConcurrentHashMap<ValueTypeAssertion, SessionNamedTopic> mapTopicsByTypeAssertion =
                    f_mapTopics.computeIfAbsent(sName, any -> new ConcurrentHashMap<>());

            // grab the type assertion and ClassLoader from the provided options
            Options<NamedTopic.Option> optionSet     = Options.from(NamedTopic.Option.class, options);
            ValueTypeAssertion<V>      typeAssertion = optionSet.get(ValueTypeAssertion.class);
            ClassLoader                loader        = optionSet.get(WithClassLoader.class,
                                                                     WithClassLoader.using(f_classLoader))
                                                                .getClassLoader();

            // grab the session-based named topic for the type of assertion, creating one if necessary
            return mapTopicsByTypeAssertion.compute(typeAssertion, (key, value) ->
                {
                // only return a valid session-based named topic
                if (value != null
                    && !value.isDestroyed()
                    && !value.isReleased()
                    && value.getContextClassLoader() == loader) // compare loaders to prevent returning incorrect topic
                    {
                    return value;
                    }

                // request an underlying NamedTopic from the CCF
                NamedTopic<V> topicUnderlying = f_ccf.ensureTopic(sName, loader, typeAssertion);

                SessionNamedTopic<V> topic = new SessionNamedTopic<>(this, topicUnderlying,
                                                                     loader, typeAssertion);

                // increment the reference count for the underlying NamedTopic
                f_registry.getResource(NamedTopicReferenceCounter.class, sName).incrementAndGet();

                return topic;
                });
            }
        else
            {
            throw new IllegalStateException("Session " + getName() + " is closed");
            }
        }

    @Override
    public void close(NamedCollection col)
        {
        if (col instanceof SessionNamedCache<?,?>)
            {
            onClose((SessionNamedCache<?, ?>) col);
            }
        else if (col instanceof SessionNamedTopic<?>)
            {
            onClose((SessionNamedTopic<?>) col);
            }
        }

    @Override
    public void destroy(NamedCollection col)
        {
        if (col instanceof SessionNamedCache<?,?>)
            {
            onDestroy((SessionNamedCache<?, ?>) col);
            }
        else if (col instanceof SessionNamedTopic<?>)
            {
            onDestroy((SessionNamedTopic<?>) col);
            }
        }

    @Override
    public synchronized void activate()
        {
        if (m_fActivated)
            {
            return;
            }

        f_eventDispatcher.dispatchStarting();
        m_fActivated = true;
        f_eventDispatcher.dispatchStarted();
        }

    @Override
    public synchronized void close()
            throws Exception
        {
        if (!m_fClosed)
            {
            Logger.info("Closing Session " + getName());

            m_fClosed = true;

            f_eventDispatcher.dispatchStopping();

            // close all the named caches created by this session
            f_mapCaches.values().stream()
                       .flatMap(
                               mapCachesByAssertion -> mapCachesByAssertion.values().stream())
                       .forEach(SessionNamedCache::close);

            // drop the references to caches
            f_mapCaches.clear();

            // close all the named topics created by this session
            f_mapTopics.values().stream()
                       .flatMap(mapTopicsByAssertion -> mapTopicsByAssertion.values().stream())
                       .forEach(SessionNamedTopic::close);

            // drop the references to topic
            f_mapTopics.clear();

            f_eventDispatcher.dispatchStopped();
            if (f_eventInterceptorId != null)
                {
                f_ccf.getInterceptorRegistry().unregisterEventInterceptor(f_eventInterceptorId);
                }
            Logger.info("Closed Session " + getName());
            }
        }

    @Override
    public ResourceRegistry getResourceRegistry()
        {
        return f_registry;
        }

    @Override
    public InterceptorRegistry getInterceptorRegistry()
        {
        return f_ccf.getInterceptorRegistry();
        }

    @Override
    public boolean isCacheActive(String sCacheName, ClassLoader loader)
        {
        return f_ccf.isCacheActive(sCacheName, loader);
        }

    @Override
    public boolean isMapActive(String sMapName, ClassLoader loader)
        {
        return f_ccf.isCacheActive(sMapName, loader);
        }

    @Override
    public boolean isTopicActive(String sTopicName, ClassLoader loader)
        {
        return f_ccf.isTopicActive(sTopicName, loader);
        }

    @Override
    public String getName()
        {
        return f_sName;
        }

    @Override
    public String getScopeName()
        {
        return f_ccf.getScopeName();
        }

    @Override
    public boolean isActive()
        {
        return !m_fClosed;
        }

    @Override
    public Service getService(String sServiceName)
        {
        return f_ccf.ensureService(sServiceName);
        }

    // ----- ConfigurableCacheFactory.Supplier methods ----------------------

    /**
     * Return the {@link ConfigurableCacheFactory} that this session wraps.
     *
     * @return  the {@link ConfigurableCacheFactory} that this session wraps
     */
    public ConfigurableCacheFactory getConfigurableCacheFactory()
        {
        return f_ccf;
        }

    // ----- ConfigurableCacheFactorySession methods ------------------------

    <V> void onClose(SessionNamedTopic<V> topic)
        {
        dropNamedTopic(topic);

        topic.onClosing();

        // decrement the reference count for the underlying NamedTopic
        if (f_registry.getResource(NamedTopicReferenceCounter.class, topic.getName()).decrementAndGet() == 0)
            {
            f_ccf.releaseTopic(topic.getInternalNamedTopic());
            }

        topic.onClosed();
        }

    <V> void onDestroy(SessionNamedTopic<V> topic)
        {
        dropNamedTopic(topic);

        topic.onDestroying();

        // reset the reference count for the underlying NamedCache
        f_registry.getResource(NamedTopicReferenceCounter.class, topic.getName()).reset();

        f_ccf.destroyTopic(topic.getInternalNamedTopic());

        topic.onDestroyed();
        }

    @SuppressWarnings({"rawtypes", "resource"})
    private <V> void dropNamedTopic(SessionNamedTopic<V> namedTopic)
        {
        // drop the NamedTopic from this session
        ConcurrentHashMap<ValueTypeAssertion, SessionNamedTopic> mapTopicsByTypeAssertion =
                f_mapTopics.get(namedTopic.getName());

        if (mapTopicsByTypeAssertion != null)
            {
            mapTopicsByTypeAssertion.remove(namedTopic.getTypeAssertion());
            }
        }

    <K, V> void onClose(SessionNamedCache<K, V> namedCache)
        {
        dropNamedCache(namedCache);

        namedCache.onClosing();

        // decrement the reference count for the underlying NamedCache
        if (f_registry.getResource(NamedCacheReferenceCounter.class, namedCache.getCacheName()).decrementAndGet() == 0)
            {
            f_ccf.releaseCache(namedCache.getInternalNamedCache());
            }

        namedCache.onClosed();
        }

    <K, V> void onDestroy(SessionNamedCache<K, V> namedCache)
        {
        dropNamedCache(namedCache);

        namedCache.onDestroying();

        // reset the reference count for the underlying NamedCache
        f_registry.getResource(NamedCacheReferenceCounter.class, namedCache.getCacheName()).reset();

        f_ccf.destroyCache(namedCache.getInternalNamedCache());

        namedCache.onDestroyed();
        }

    @SuppressWarnings({"rawtypes", "resource"})
    <K, V> void dropNamedCache(SessionNamedCache<K, V> namedCache)
        {
        // drop the NamedCache from this session
        ConcurrentHashMap<TypeAssertion, SessionNamedCache> mapCachesByTypeAssertion =
                f_mapCaches.get(namedCache.getCacheName());

        if (mapCachesByTypeAssertion != null)
            {
            mapCachesByTypeAssertion.remove(namedCache.getTypeAssertion());
            }
        }

    protected boolean isClosed()
        {
        return m_fClosed;
        }

    // ----- inner classes --------------------------------------------------

    /**
     * Tracks use of individual  instances by all {@link Session}s
     * against the same {@link ConfigurableCacheFactory} (by using reference counting)
     */
    protected static class ReferenceCounter
        {
        public ReferenceCounter()
            {
            f_count = new AtomicInteger(0);
            }

        public int get()
            {
            return f_count.get();
            }

        public int incrementAndGet()
            {
            return f_count.incrementAndGet();
            }

        public int decrementAndGet()
            {
            return f_count.decrementAndGet();
            }

        public void reset()
            {
            f_count.set(0);
            }

        private final AtomicInteger f_count;
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

    // ----- inner class: LifecycleInterceptor ------------------------------

    /**
     * An event interceptor that ties this session's lifecycle to the underlying CCF lifecycle.
     */
    private class LifecycleInterceptor
            implements EventDispatcherAwareInterceptor<LifecycleEvent>
        {
        @Override
        public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
            {
            if (dispatcher instanceof ConfigurableCacheFactoryDispatcher)
                {
                dispatcher.addEventInterceptor(getName(), this);
                }
            }

        @Override
        public void onEvent(LifecycleEvent event)
            {
            if (event.getType() == LifecycleEvent.Type.DISPOSING)
                {
                try
                    {
                    close();
                    }
                catch (Exception e)
                    {
                    Logger.err(e);
                    }
                }
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
    private final ConfigurableCacheFactory f_ccf;

    /**
     * The {@link ClassLoader} for the {@link Session}.
     */
    private final ClassLoader f_classLoader;

    /**
     * The {@link Session} has been activated.
     */
    private volatile boolean m_fActivated;

    /**
     * The {@link Session} has been closed.
     */
    private volatile boolean m_fClosed;

    /**
     * The {@link SessionNamedCache}s created by this {@link Session}
     * (so we close them when the session closes).
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<TypeAssertion, SessionNamedCache>> f_mapCaches;

    /**
     * The {@link NamedTopic}s created by this {@link Session}
     * (so we close them when the session closes).
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<ValueTypeAssertion, SessionNamedTopic>> f_mapTopics;

    /**
     * The event dispatcher for lifecycle events.
     */
    private final SessionEventDispatcher f_eventDispatcher;

    /**
     * The CCF's resource registry.
     */
    private final ResourceRegistry f_registry;

    /**
     * The event interceptor identifier (so we can unregister it whenThe session closes).
     */
    private final String f_eventInterceptorId;
    }
