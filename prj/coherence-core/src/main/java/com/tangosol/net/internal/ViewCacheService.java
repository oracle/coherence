/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.scheme.CachingScheme;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.service.DefaultViewDependencies;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.ServiceDependencies;
import com.tangosol.net.ViewBuilder;
import com.tangosol.net.WrapperCacheService;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.partition.PartitionedServiceDispatcher;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.FilterEnumerator;
import com.tangosol.util.MapListener;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.SegmentedConcurrentMap;
import com.tangosol.util.ValueExtractor;

import java.util.Enumeration;
import java.util.function.Supplier;

/**
 * A CacheService that can create caches that are local views of some other
 * {@link NamedCache}. These 'views' can be restricted by {@link Filter}s,
 * transformed by {@link ValueExtractor}s and listened to by {@link MapListener}s.
 * <p>
 * This service will ensure the caches are primed and ready as soon as their
 * existence is realized (caches already created discovered by a new member or
 * new caches discovered by an existing member).
 *
 * @author hr  2019.06.11
 * @since 12.2.1.4
 */
public class ViewCacheService
        extends WrapperCacheService
    {
    // ----- constructors ---------------------------------------------------
    /**
     * Create a new ViewCacheService backed by the provided CacheService.
     *
     * @param service  the CacheService backing this ViewCacheService
     */
    public ViewCacheService(CacheService service)
        {
        super(service);
        }

    // ----- CacheService methods -------------------------------------------

    @Override
    public NamedCache ensureCache(String sName, ClassLoader loader)
        {
        checkInitialized();

        NamedCache cache = (NamedCache) f_mapCaches.get(sName);
        if (cache == null || cache.isReleased() || cache.isDestroyed())
            {
            f_mapCaches.lock(sName);
            try
                {
                cache = (NamedCache) f_mapCaches.get(sName);

                if (cache == null || cache.isReleased() || cache.isDestroyed())
                    {
                    ClassLoader loaderBack = NullImplementation.getClassLoader();
                    f_mapCaches.put(sName,
                            cache = instantiateView(sName, loader, () -> super.ensureCache(sName, loaderBack)));
                    }
                }
            finally
                {
                f_mapCaches.unlock(sName);
                }
            }

        return cache;
        }

    @Override
    public void releaseCache(NamedCache cache)
        {
        removeCache(cache);
        }

    @Override
    public void destroyCache(NamedCache cache)
        {
        NamedCache cacheRemoved = removeCache(cache);

        if (cacheRemoved != null)
            {
            cacheRemoved.destroy();
            }
        }

    @Override
    public void setBackingMapManager(BackingMapManager manager)
        {
        try
            {
            // this will throw if the service backing the view-scheme has already
            // started however whether this service or the service that backs
            // this service starts is not deterministic
            super.setBackingMapManager(manager);
            }
        catch (Exception e) {}
        }

    @Override
    public Enumeration getCacheNames()
        {
        return new FilterEnumerator(super.getCacheNames(), oCacheName ->
                f_mapCaches.containsKey(oCacheName));
        }

    // ----- Service methods ------------------------------------------------

    @Override
    public void start()
        {
        super.start();

        ensureInitialized();
        }

    @Override
    public void stop()
        {
        // remove listeners before we stop the back service
        deregister();

        super.stop();
        }

    @Override
    public void shutdown()
        {
        // remove listeners before we stop the back service
        deregister();

        super.shutdown();
        }

    @Override
    public boolean isRunning()
        {
        return m_fInitialized && super.isRunning();
        }

    @Override
    public DefaultViewDependencies getDependencies()
        {
        return m_dependencies;
        }

    @Override
    public void setDependencies(ServiceDependencies deps)
        {
        m_dependencies = (DefaultViewDependencies) deps;
        }
    
    // ----- object methods -------------------------------------------------

    @Override
    public String toString()
        {
        DefaultViewDependencies deps = m_dependencies;

        return "ViewCacheService{filter=" + deps.getFilterBuilder() +
                ", transformer=" + deps.getTransformerBuilder() +
                ", backService=" + getCacheService() + '}';
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Check that this {@link CacheService} was started and therefore has
     * been initialized; throws an exception if not.
     *
     * @throws IllegalStateException if this {@link CacheService} has not been
     *         started or the backing {@code CacheService} is not running
     */
    protected void checkInitialized()
        {
        Service service = getService();
        if (!m_fInitialized || !service.isRunning())
            {
            synchronized (this)
                {
                // double check in case we were initialized during the check
                if (!m_fInitialized || !service.isRunning())
                    {
                    throw new IllegalStateException("Service backing the ViewScheme is not running: " + service);
                    }
                }
            }
        }

    /**
     * Ensure this {@link CacheService} has been initialized.
     */
    protected void ensureInitialized()
        {
        CacheService        service = (CacheService) getService();
        Enumeration<String> enumer  = null;

        if (!m_fInitialized && service.isRunning())
            {
            synchronized (this)
                {
                if (!m_fInitialized && service.isRunning())
                    {
                    String sServiceName = service.getInfo().getServiceName();

                    // register an EventInterceptor to be notified of new or destroyed caches
                    getInterceptorRegistry().registerEventInterceptor(
                            EVENT_INTERCEPTOR_PREFIX + sServiceName,
                            new CacheSyncEventInterceptor(sServiceName),
                            RegistrationBehavior.IGNORE);

                    enumer = service.getCacheNames();

                    m_fInitialized = true;
                    }
                }
            }

        while (enumer != null && enumer.hasMoreElements())
            {
            String sCacheName = enumer.nextElement();

            if (isViewCache(sCacheName))
                {
                ensureCache(sCacheName, service.getContextClassLoader());
                }
            }
        }

    /**
     * Deregister any components this {@link CacheService} registered with
     * ancillary data structures.
     */
    protected void deregister()
        {
        if (m_fInitialized)
            {
            synchronized (this)
                {
                if (m_fInitialized)
                    {
                    getInterceptorRegistry().unregisterEventInterceptor(
                        EVENT_INTERCEPTOR_PREFIX + getService().getInfo().getServiceName());
                    f_mapCaches.clear();

                    m_fInitialized = false;
                    }
                }
            }
        }

    /**
     * Return a {@link NamedCache view} of the given NamedCache, restricting
     * and converting the data as defined by the associated {@link DefaultViewDependencies
     * dependencies}.
     *
     * @param supplierCache  a supplier that provides the {@link NamedCache}
     *
     * @return a {@link NamedCache view} of the given NamedCache
     */
    protected NamedCache instantiateView(String sCacheName, ClassLoader loader, Supplier<NamedCache> supplierCache)
        {
        DefaultViewDependencies deps      = m_dependencies;
        ViewBuilder             bldrView  = new ViewBuilder(supplierCache);
        boolean                 fReadOnly = false;

        if (deps != null)
            {
            Filter            filter      = null;
            MapListener       listener    = null;
            ValueExtractor    transformer = null;
            ParameterResolver resolver    = getParameterResolver(sCacheName, loader);

            ParameterizedBuilder<Filter>         bldrFilter      = deps.getFilterBuilder();
            ParameterizedBuilder<ValueExtractor> bldrTransformer = deps.getTransformerBuilder();
            ParameterizedBuilder<MapListener>    bldrListener    = deps.getListenerBuilder();

            if (bldrFilter != null)
                {
                filter = bldrFilter.realize(resolver, loader, /*listParameters*/ null);
                }
            if (bldrListener != null)
                {
                listener = bldrListener.realize(resolver, loader, /*listParameters*/ null);
                }
            if (bldrTransformer != null)
                {
                transformer = bldrTransformer.realize(resolver, loader, /*listParameters*/ null);
                }

            fReadOnly = transformer != null || deps.isReadOnly();

            bldrView = deps.isCacheValues() ? bldrView.values() : bldrView.keys();

            bldrView.filter(filter)
                    .map(transformer)
                    .listener(listener)
                    .withClassLoader(getService().getContextClassLoader());
            }

        NamedCache cacheView = bldrView.build();
        
        if (cacheView instanceof ContinuousQueryCache) // common path
            {
            ContinuousQueryCache cacheCQC = (ContinuousQueryCache) cacheView;

            // set the CQC specific
            cacheCQC.setCacheName(cacheCQC.getCache().getCacheName());
            cacheCQC.setReconnectInterval(deps.getReconnectInterval());
            cacheCQC.setReadOnly(fReadOnly);
            MBeanHelper.registerViewMBean(cacheCQC);
            }

        return cacheView;
        }

    /**
     * Return a {@link ParameterResolver} with context that can be injected
     * into components that will be created for the {@link ContinuousQueryCache}.
     *
     * @param sCacheName  the name of the cache being created
     * @param loader      the ClassLoader to use for the cache
     *
     * @return a ParameterResolver that can be used when realizing required
     *         components
     */
    protected ParameterResolver getParameterResolver(String sCacheName, ClassLoader loader)
        {
        ConfigurableCacheFactory ccf = getBackingMapManager().getCacheFactory();
        if (ccf instanceof ExtensibleConfigurableCacheFactory)
            {
            ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory) ccf;

            return eccf.getParameterResolver(sCacheName, CacheMapping.class, loader, null, null);
            }

        ResolvableParameterList resolver = new ResolvableParameterList();
        resolver.add(new Parameter("cache-name", sCacheName));
        resolver.add(new Parameter("class-loader", loader));

        return resolver;
        }

    /**
     * Remove a cache from internal data structures, returning the cache if
     * it was actually removed.
     *
     * @param cache  the cache to be removed
     *
     * @return the cache that was removed or null
     */
    protected NamedCache removeCache(NamedCache cache)
        {
        checkInitialized();

        String sCache = cache.getCacheName();

        f_mapCaches.lock(sCache);
        try
            {
            if (f_mapCaches.get(sCache) == cache)
                {
                return (NamedCache) f_mapCaches.remove(sCache);
                }
            }
        finally
            {
            f_mapCaches.remove(sCache);
            MBeanHelper.unregisterViewMBean(cache);
            }
        return null;
        }

    /**
     * Return true if the provided cache name maps to a view scheme.
     *
     * @param sCacheName  the cache name
     *
     * @return true if the provided cache name maps to a view scheme
     */
    protected boolean isViewCache(String sCacheName)
        {
        ConfigurableCacheFactory ccf = getBackingMapManager().getCacheFactory();
        if (ccf instanceof ExtensibleConfigurableCacheFactory)
            {
            CachingScheme scheme = ((ExtensibleConfigurableCacheFactory) ccf)
                    .getCacheConfig().findSchemeByCacheName(sCacheName);

            return scheme != null && TYPE_VIEW.equals(scheme.getServiceType());
            }
        return false;
        }

    /**
     * Return the associated {@link InterceptorRegistry}.
     *
     * @return the associated {@link InterceptorRegistry}
     */
    protected InterceptorRegistry getInterceptorRegistry()
        {
        return getCacheService().getBackingMapManager().getCacheFactory().getInterceptorRegistry();
        }

    // ----- inner class: CacheSyncEventInterceptor -------------------------

    /**
     * An {@link EventDispatcherAwareInterceptor} listening for {@link
     * CacheLifecycleEvent.Type#CREATED cache creation events} to eagerly
     * initialize {@link com.tangosol.net.cache.ContinuousQueryCache CQCs}
     * returned by the associated {@link ViewCacheService}.
     */
    public class CacheSyncEventInterceptor
            implements EventDispatcherAwareInterceptor<CacheLifecycleEvent>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct the CacheSyncEventInterceptor with the given service name.
         *
         * @param sServiceName  the service name to listen to events against
         */
        public CacheSyncEventInterceptor(String sServiceName)
            {
            f_sServiceName = sServiceName;
            }

        // ----- EventInterceptor methods -----------------------------------

        @Override
        public void onEvent(CacheLifecycleEvent event)
            {
            String sCacheName = event.getCacheName();

            if (!ViewCacheService.this.isViewCache(sCacheName))
                {
                return;
                }

            ViewCacheService service = ViewCacheService.this;
            switch (event.getType())
                {
                case CREATED:
                    service.ensureCache(sCacheName, service.getContextClassLoader());
                    break;
                case DESTROYED: // destroy and truncate are directly handled
                case TRUNCATED: // by the CQC (NamedCacheDeactivationListener)
                    break;
                }
            }

        // ----- EventDispatcherAwareInterceptor methods --------------------

        @Override
        public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
            {
            if (dispatcher instanceof PartitionedServiceDispatcher)
                {
                String sServiceNameThat = ((PartitionedServiceDispatcher) dispatcher).getService().getInfo().getServiceName();
                if (Base.equals(f_sServiceName, sServiceNameThat))
                    {
                    dispatcher.addEventInterceptor(sIdentifier, this);
                    }
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The service name to listen to events against.
         */
        protected final String f_sServiceName;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The service type for ViewCacheService.
     */
    public static final String TYPE_VIEW = "ViewCache";

    /**
     * The key used to register a Map of ViewCacheService references in the
     * Cluster's resource registry.
     */
    public static final String KEY_CLUSTER_REGISTRY = "$ViewCacheServiceHandlers";

    /**
     * The prefix used when registering the {@link CacheSyncEventInterceptor}.
     */
    public static final String EVENT_INTERCEPTOR_PREFIX = "__CQCInit_";

    // ----- data members ---------------------------------------------------

    /**
     * Whether this CacheService has been initialized and therefore the
     * EventInterceptor registered.
     */
    protected volatile boolean m_fInitialized;

    /**
     * A cache of views returned by this service.
     */
    protected SegmentedConcurrentMap f_mapCaches = new SegmentedConcurrentMap();

    /**
     * The dependencies that define how to configure returned views.
     */
    protected DefaultViewDependencies m_dependencies;
    }
