/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Disposable;

import com.oracle.coherence.common.util.Options;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.coherence.config.ResourceMapping;
import com.tangosol.coherence.config.ResourceMappingRegistry;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.ServiceSchemeRegistry;
import com.tangosol.coherence.config.TopicMapping;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.NamedCacheBuilder;
import com.tangosol.coherence.config.builder.NamedCollectionBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.builder.ServiceBuilder;
import com.tangosol.coherence.config.builder.SubscriberGroupBuilder;

import com.tangosol.coherence.config.scheme.AbstractCompositeScheme;
import com.tangosol.coherence.config.scheme.AbstractLocalCachingScheme;
import com.tangosol.coherence.config.scheme.AbstractServiceScheme;
import com.tangosol.coherence.config.scheme.BackingMapScheme;
import com.tangosol.coherence.config.scheme.BackupMapConfig;
import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.ClusteredCachingScheme;
import com.tangosol.coherence.config.scheme.DistributedScheme;
import com.tangosol.coherence.config.scheme.ExternalScheme;
import com.tangosol.coherence.config.scheme.FlashJournalScheme;
import com.tangosol.coherence.config.scheme.ObservableCachingScheme;
import com.tangosol.coherence.config.scheme.PagedExternalScheme;
import com.tangosol.coherence.config.scheme.ReadWriteBackingMapScheme;
import com.tangosol.coherence.config.scheme.Scheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;
import com.tangosol.coherence.config.scheme.TransactionalScheme;

import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;

import com.tangosol.config.expression.ChainedParameterResolver;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.ScopedParameterResolver;
import com.tangosol.config.expression.SystemEnvironmentParameterResolver;
import com.tangosol.config.expression.SystemPropertyParameterResolver;

import com.tangosol.config.xml.DocumentProcessor;

import com.tangosol.io.BinaryStore;
import com.tangosol.io.ClassLoaderAware;

import com.tangosol.io.nio.BinaryMap;
import com.tangosol.io.nio.ByteBufferManager;
import com.tangosol.io.nio.MappedBufferManager;

import com.tangosol.net.cache.CacheLoader;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.MapCacheStore;
import com.tangosol.net.cache.NearCache;
import com.tangosol.net.cache.OverflowMap;
import com.tangosol.net.cache.ReadWriteBackingMap;
import com.tangosol.net.cache.SerializationMap;
import com.tangosol.net.cache.SimpleSerializationMap;
import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.net.events.EventDispatcherRegistry;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.internal.ConfigurableCacheFactoryDispatcher;
import com.tangosol.net.events.internal.InterceptorManager;
import com.tangosol.net.events.internal.Registry;

import com.tangosol.net.internal.ScopedCacheReferenceStore;
import com.tangosol.net.internal.ScopedReferenceStore;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.net.partition.ObservableSplittingBackingCache;
import com.tangosol.net.partition.ObservableSplittingBackingMap;

import com.tangosol.net.security.DoAsAction;
import com.tangosol.net.security.Security;
import com.tangosol.net.security.StorageAccessAuthorizer;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.run.xml.XmlDocumentReference;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapSet;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.SimpleResourceRegistry;

import java.io.File;

import java.net.URI;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * ExtensibleConfigurableCacheFactory provides a facility to access caches
 * declared in a "coherence-cache-config.xsd" compliant configuration file.
 * <p>
 * It is strongly recommended that developers get a ConfigurableCacheFactory instance via
 * CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(), rather than
 * instantiate an ExtensibleConfigurableCacheFactory instance directly.
 * <p>
 * There are various ways of using this factory:
 * <pre>
 *   ExtensibleConfigurableCacheFactory.Dependencies deps =
 *       ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance("my-cache-config.xml");
 *   ExtensibleConfigurableCacheFactory factory =
 *       new ExtensibleConfigurableCacheFactory(deps);
 *   ...
 *   ClassLoader loader  = getClass().getClassLoader();
 *   NamedCache cacheOne = factory.ensureCache("one", loader);
 *   NamedCache cacheTwo = factory.ensureCache("two", loader);
 * </pre>
 *
 * Another option is using the static version of the "ensureCache" call:
 * <pre>
 *   ClassLoader loader  = getClass().getClassLoader();
 *   NamedCache cacheOne = CacheFactory.getCache("one", loader);
 * </pre>
 * which uses an instance of ConfigurableCacheFactory obtained by
 * {@link CacheFactory#getConfigurableCacheFactory()}.
 *
 * @see CacheFactory#getCache(String, ClassLoader, NamedCache.Option...)
 *
 * @author gg  2003.05.26
 * @author pfm 2012.12.19
 *
 * @since Coherence 12.1.2
 */
public class ExtensibleConfigurableCacheFactory
        extends Base
        implements ConfigurableCacheFactory
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an {@link ExtensibleConfigurableCacheFactory} using
     * the specified {@link Dependencies}.
     *
     * @param dependencies  the {@link Dependencies}
     */
    public ExtensibleConfigurableCacheFactory(Dependencies dependencies)
        {
        // initialize dependency-based state
        f_cacheConfig   = dependencies.getCacheConfig();
        m_loader        = dependencies.getContextClassLoader();
        f_registry      = dependencies.getResourceRegistry();

        // initialize non-dependency-based state
        f_store         = new ScopedCacheReferenceStore();

        f_storeTopics   = new ScopedReferenceStore<>(NamedTopic.class, NamedTopic::isActive,
                                                   NamedTopic::getName, NamedTopic::getService);

        m_fActivated    = false;
        m_fDisposed     = false;

        // acquire the legacy xml config from the resource registry
        // (this is used by legacy parts of coherence)
        f_xmlLegacyConfig = (XmlElement) f_registry.getResource(XmlElement.class, "legacy-cache-config").clone();

        // perform final internal configuration
        // (note: this behavior may be overridden by a sub-class)
        configure();

        // register the lifecycle event dispatcher as all previous parsing
        // was successful
        f_dispatcher = new ConfigurableCacheFactoryDispatcher();

        EventDispatcherRegistry dispatcherReg = f_registry.getResource(EventDispatcherRegistry.class);

        if (dispatcherReg != null)
            {
            dispatcherReg.registerEventDispatcher(f_dispatcher);
            }

        CacheFactory.log("Created cache factory " + this.getClass().getName(), CacheFactory.LOG_INFO);
        }

    // ----- ConfigurableCacheFactory interface -----------------------------


    @Override
    public <K, V> NamedCache<K, V> ensureCache(String sCacheName,
                                               ClassLoader loader,
                                               NamedCache.Option... options)
        {
        assertNotDisposed();

        Base.checkNotEmpty(sCacheName, "CacheName");

        return System.getSecurityManager() == null
                ? ensureCacheInternal(sCacheName, loader, options)
                : AccessController.doPrivileged(new DoAsAction<>(
                    () -> ensureCacheInternal(sCacheName, loader, options)));
        }

    /**
     * Implementation of {@link #ensureTypedCache(String, ClassLoader, TypeAssertion)}
     */
    private <K, V> NamedCache<K, V> ensureCacheInternal(String sCacheName,
                                                        ClassLoader loader,
                                                        NamedCache.Option... options)
        {
        loader = ensureClassLoader(loader);

        Options<NamedCache.Option> optsNamedCache = Options.from(NamedCache.Option.class, options);

        // acquire the type assertion option
        // (when non-defined the @Options.Default will be used which is raw-types)
        TypeAssertion assertion = optsNamedCache.get(TypeAssertion.class);

        NamedCache              cache;
        CachingScheme           scheme;
        ParameterResolver       resolver;
        MapBuilder.Dependencies dependencies;

        while (true)
            {
            // find the cache mapping for the cache
            CacheMapping mapping = f_cacheConfig.getMappingRegistry().findMapping(sCacheName, CacheMapping.class);
            if (mapping == null)
                {
                throw new IllegalArgumentException("ensureCache cannot find a mapping for cache " + sCacheName);
                }

            cache = f_store.getCache(sCacheName, loader);

            if (cache != null && cache.isActive())
                {
                // the common path; the cache reference is active and reusable
                checkPermission(cache);
                
                // always assert the safety of the types according to the specified assertion
                // (as they may change between calls to ensureCache)
                assertion.assertTypeSafety(sCacheName, mapping, /*fLog*/ false);

                return cache;
                }

            if (cache != null && !cache.isDestroyed() && !cache.isReleased())
                {
                try
                    {
                    // this can only indicate that the cache was "disconnected" due to a
                    // network failure or an abnormal cache service termination;
                    // the underlying cache service will “restart" during the very next call
                    // to any of the NamedCache API method (see COH-15083 for details)
                    checkPermission(cache);
                    
                    // always assert the safety of the types according to the specified assertion
                    // (as they may change between calls to ensureCache).
                    assertion.assertTypeSafety(sCacheName, mapping, /*fLog*/ false);

                    return cache;
                    }
                catch (IllegalStateException e)
                    {
                    // Fail to access due to cluster or service being explicitly stopped or shutdown
                    // and not properly restarted.
                    // Remove this cache and the process of returning a new one ensures all services needed by cache.
                    f_store.releaseCache(cache, loader);
                    }
                }

            // Always assert the safety of the types according to the specified assertion.
            // Only log warning once if mismatch between cache config specified types and
            // runtime type assertion where one uses raw types.
            assertion.assertTypeSafety(sCacheName, mapping, /*fLog*/ true);

            // find the scheme for the cache
            ServiceScheme serviceScheme = f_cacheConfig.findSchemeBySchemeName(mapping.getSchemeName());
            if (serviceScheme == null)
                {
                throw new IllegalArgumentException("ensureCache cannot find service scheme "
                    + mapping.getSchemeName() + " for cache " + sCacheName);
                }
            else if (!(serviceScheme instanceof CachingScheme))
                {
                throw new IllegalArgumentException("The scheme " + mapping.getSchemeName()
                    + " for cache " + sCacheName + " is not a CachingScheme");
                }

            scheme = (CachingScheme) serviceScheme;

            f_store.clearInactiveCacheRefs();

            // create (realize) the cache and add the map listener if applicable
            NamedCacheBuilder bldrCache = scheme;

            Base.checkNotNull(bldrCache, "NamedCacheBuilder");

            resolver     = getParameterResolver(sCacheName, CacheMapping.class, loader, null);
            dependencies = new MapBuilder.Dependencies(this, null, loader,
                                    sCacheName, scheme.getServiceType());
            cache        = bldrCache.realizeCache(resolver, dependencies);

            // TODO: The knowledge of transactional cache is left out of ScopedReferenceStore. We should re-consider.
            if (scheme instanceof TransactionalScheme)
                {
                break;
                }

            if (f_store.putCacheIfAbsent(cache, loader) == null)
                {
                break;
                }
            }

        if (scheme instanceof ObservableCachingScheme)
            {
            ObservableCachingScheme schemeObservable = (ObservableCachingScheme)scheme;

            schemeObservable.establishMapListeners(cache, resolver, dependencies);
            }

        if (cache instanceof NearCache)
            {
            ((NearCache) cache).registerMBean();
            }

        return cache;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseCache(NamedCache cache)
        {
        releaseCache(cache, /* fDestroy */ false);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroyCache(NamedCache cache)
        {
        releaseCache(cache, /* fDestroy */ true);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V> NamedTopic<V> ensureTopic(String sName, ClassLoader loader, NamedTopic.Option... options)
        {
        assertNotDisposed();

        Base.checkNotEmpty(sName, "name");

        PrivilegedAction<NamedTopic> action =
            () -> ensureCollectionInternal(sName, NamedTopic.class, loader, f_storeTopics, options);

        return AccessController.doPrivileged(new DoAsAction<>(action));
        }

    /**
     * Ensure an Object-based collection for the given name.
     *
     * @param sName          the name of the collection
     * @param clsCollection  the type of the values in the collection
     * @param loader         the {@link ClassLoader} to use
     * @param store          the {@link ScopedReferenceStore} that holds collection references
     * @param options        the options to use to configure the collection
     *
     * @param <C>  the type of the collection
     * @param <V>  the type of the values in the collection
     *
     * @return  an Object-based collection for the given name
     */
    private <C extends NamedCollection, V> C ensureCollectionInternal(String sName, Class<C> clsCollection, ClassLoader loader,
        ScopedReferenceStore<C> store, NamedCollection.Option... options)
        {
        loader = ensureClassLoader(loader);

        Options<NamedTopic.Option> optsNamedTopic = Options.from(NamedTopic.Option.class, options);
        ValueTypeAssertion<V>      constraint     = optsNamedTopic.get(ValueTypeAssertion.class);
        C                          collection;
        ParameterResolver          resolver;
        MapBuilder.Dependencies    dependencies;

        while (true)
            {
            // find the topic mapping for the collection
            TopicMapping mapping = f_cacheConfig.getMappingRegistry().findMapping(sName, TopicMapping.class);

            if (mapping == null)
                {
                throw new IllegalArgumentException(String.format("Cannot find a mapping for %s", sName));
                }

            // always assert the safety of the types according to the specified assertion
            // (as they may change between calls to ensureCache)
            constraint.assertTypeSafety(sName, mapping);

            C col = store.get(sName, loader);

            if (col != null && col.isActive())
                {
                if (clsCollection.isAssignableFrom(col.getClass()))
                    {
                    checkPermission(col);
                    return col;
                    }
                else
                    {
                    String sMsg = String.format(
                            "A Collection already exist for name '%s' but is of type %s when requested type is %s",
                            sName, col.getClass(), clsCollection);

                    throw new IllegalStateException(sMsg);
                    }
                }

            // find the scheme for the collection
            ServiceScheme serviceScheme = f_cacheConfig.findSchemeBySchemeName(mapping.getSchemeName());

            if (serviceScheme == null)
                {
                String sMsg = String.format("ensureCollection cannot find service scheme %s for mapping %s",
                        mapping.getSchemeName(), sName);

                throw new IllegalArgumentException(sMsg);
                }
            else if (!(serviceScheme instanceof NamedCollectionBuilder))
                {
                String sMsg = String.format("The scheme %s for collection %s cannot build a %s",
                                            mapping.getSchemeName(), sName, clsCollection);

                throw new IllegalArgumentException(sMsg);
                }

            // there are instances of sibling caches for different
            // class loaders; check for and clear invalid references
            Collection<Integer> colHashCode = store.clearInactiveRefs(sName);

            if (colHashCode != null)
                {
                for (Integer nHashCode : colHashCode)
                    {
                    MBeanHelper.unregisterCacheMBean(sName, "tier=front,loader=" + nHashCode);
                    }
                }

            // create (realize) the collection
            NamedCollectionBuilder<C> bldrCollection = (NamedCollectionBuilder<C>) serviceScheme;

            Base.checkNotNull(bldrCollection, "NamedCollectionBuilder");

            if (!bldrCollection.realizes(clsCollection))
                {
                String sMsg = String.format("The scheme '%s' is defined as a %s and cannot build an instance of a %s",
                                            sName, bldrCollection.getClass().getSimpleName(),
                                            clsCollection.getSimpleName());

                throw new IllegalStateException(sMsg);
                }

            resolver      = getParameterResolver(sName, TopicMapping.class, loader, null);
            dependencies  = new MapBuilder.Dependencies(this, null, loader, sName,
                                                        serviceScheme.getServiceType());

            collection    = bldrCollection.realize(constraint, resolver, dependencies);

            for (SubscriberGroupBuilder builder : mapping.getSubscriberGroupBuilders())
                {
                Subscriber s = builder.realize((NamedTopic) collection, resolver);
                s.close();
                }

            if (store.putIfAbsent(collection, loader) == null)
                {
                break;
                }
            }

        return collection;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Service ensureService(String sServiceName)
        {
        assertNotDisposed();

        // find the first service in the caching schemes that matches the
        // service name
        ServiceScheme scheme = f_cacheConfig.findSchemeByServiceName(sServiceName);

        if (scheme == null)
            {
            throw new IllegalArgumentException("No scheme found for service " + sServiceName);
            }

        return ensureService(scheme);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void activate()
        {
        if (m_fActivated)
            {
            throw new IllegalStateException("This factory is already active.");
            }

        // validate the configuration
        f_cacheConfig.validate(f_registry);

        f_dispatcher.dispatchActivating(this);

        startServices();

        try
            {
            f_dispatcher.dispatchActivated(this);
            }
        finally
            {
            m_fActivated = true;
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void dispose()
        {
        try
            {
            f_dispatcher.dispatchDisposing(this);
            }
        finally
            {
            Map<Service, String> mapServices = m_mapServices;

            for (Service service : mapServices.keySet())
                {
                synchronized (service)
                    {
                    ResourceRegistry registry = service.getResourceRegistry();
                    Set<ConfigurableCacheFactory> setRefs =
                        registry.getResource(Set.class, "Referrers");

                    if (setRefs == null ||
                            (setRefs.remove(this) && setRefs.isEmpty()))
                        {
                        service.shutdown();
                        }
                    }
                }

            mapServices.clear();
            f_registry.dispose();

            m_fActivated = false;
            m_fDisposed  = true;
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceRegistry getResourceRegistry()
        {
        assertNotDisposed();

        return f_registry;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public InterceptorRegistry getInterceptorRegistry()
        {
        return getResourceRegistry().getResource(InterceptorRegistry.class);
        }

    // ----- ExtensibleConfigurableCacheFactory methods ---------------------

    /**
     * Return the scope name for this ConfigurableCacheFactory.  If specified,
     * this name will be used as a prefix for the name of all services created
     * by this factory.
     *
     * @return the scope name for this ConfigurableCacheFactory; may be null
     */
    public String getScopeName()
        {
        return f_cacheConfig.getScopeName();
        }

    /**
     * Return the request timeout determined by the {@link ServiceScheme}.
     *
     * @param scheme  the scheme that determines the request timeout
     *
     * @return the request timeout
     */
    protected long getRequestTimeout(ServiceScheme scheme)
        {
        ServiceDependencies deps = null;

        if (scheme instanceof AbstractServiceScheme)
            {
            deps = ((AbstractServiceScheme) scheme).getServiceDependencies();
            }

        long cWait = deps == null ? -1 : deps.getRequestTimeoutMillis();

        return cWait > 0 ? cWait : -1;
        }

    /**
     * Return true if this factory has been disposed via invocation of
     * {@link #dispose()}.
     *
     * @return true if this factory has been disposed
     */
    protected boolean isDisposed()
        {
        return m_fDisposed;
        }

    /**
     * Throws {@link IllegalStateException} if this factory has been disposed
     * via invocation of {@link #dispose()}.
     *
     */
    protected void assertNotDisposed()
        {
        if (isDisposed())
            {
            throw new IllegalStateException("This factory has been disposed and cannot be reused");
            }
        }

    /**
     * Performs final configuration of an {@link ExtensibleConfigurableCacheFactory}
     * instance prior to it being used by Coherence.
     * <p>
     * This method is called by the {@link ExtensibleConfigurableCacheFactory}
     * constructor, just after the internal state has been initialized and before
     * the {@link ExtensibleConfigurableCacheFactory} instance is returned to the
     * caller.
     * <p>
     * This method allows those that need to sub-class an
     * {@link ExtensibleConfigurableCacheFactory} to override the final stages
     * of configuration.  Anyone overriding this method must be sure to call
     * super.configure() in order to ensure correction configuration semantics.
     */
    protected void configure()
        {
        f_registry.getResource(InterceptorManager.class)
                .instantiateGlobalInterceptors();
        }

    /**
     * Ensure the service for the specified scheme then start the service if
     * it isn't running.
     *
     * @param scheme  the scheme referring to the service
     *
     * @return the corresponding Service
     */
    public Service ensureService(ServiceScheme scheme)
        {
        assertNotDisposed();

        ServiceBuilder bldrService = scheme.getServiceBuilder();
        ClassLoader    loader      = getConfigClassLoader();

        Cluster cluster = bldrService.isRunningClusterNeeded()
                          ? CacheFactory.ensureCluster() : CacheFactory.getCluster();

        synchronized (cluster)
            {
            Service service = bldrService.realizeService(
                f_cacheConfig.getDefaultParameterResolver(), loader, cluster);

            if (service.isRunning())
                {
                if (service instanceof CacheService)
                    {
                    validateBackingMapManager((CacheService) service);
                    }
                }
            else
                {
                if (service instanceof CacheService)
                    {
                    // create and register the backing map manager
                    BackingMapManager mgr = ((CachingScheme) scheme).realizeBackingMapManager(this);

                    registerBackingMapManager(mgr);
                    ((CacheService) service).setBackingMapManager(mgr);
                    }

                startService(service);
                }

            m_mapServices.put(service, scheme.getServiceName());

            // add this ECCF as a "referrer"
            synchronized (service)
                {
                ResourceRegistry registry = service.getResourceRegistry();
                Set<ConfigurableCacheFactory> setRefs =
                    registry.getResource(Set.class, "Referrers");
                if (setRefs == null)
                    {
                    setRefs = Collections.newSetFromMap(new IdentityHashMap<>());
                    registry.registerResource(Set.class, "Referrers", setRefs);
                    }
                setRefs.add(this);
                }

            return service;
            }
        }

    /**
     * Return the {@link CacheConfig} that contains the configuration used by
     * this factory.
     *
     * @return the CacheConfig
     */
    public CacheConfig getCacheConfig()
        {
        return f_cacheConfig;
        }

    /**
     * Translate the scheme name into the scheme type. Valid scheme types are
     * any of the SCHEME_* constants.
     *
     * @param sScheme  the scheme name
     *
     * @return the scheme type
     */
    protected static int translateStandardSchemeType(String sScheme)
        {
        Integer iSchemeType = MAP_SCHEMETYPE_BY_SCHEMENAME.get(sScheme);

        return iSchemeType == null ? SCHEME_UNKNOWN : iSchemeType;
        }

    // ----- helpers and inheritance support --------------------------------

    /**
     * Start all services that are declared as requiring an "autostart".
     */
    public void startServices()
        {
        CacheConfig             config     = getCacheConfig();
        ResourceMappingRegistry regMapping = config.getMappingRegistry();
        ServiceSchemeRegistry   regScheme  = config.getServiceSchemeRegistry();

        // collect all scheme names for "shared" services based on the container context
        Set<String> setSharedScheme = NullImplementation.getSet();

        // start all scoped services
        for (ServiceScheme scheme : regScheme)
            {
            if (scheme.isAutoStart() && !setSharedScheme.contains(scheme.getSchemeName()))
                {
                ensureService(scheme);
                }
            }
        }

    /**
     * Return a map of services that were successfully started by this
     * factory where values are corresponding non-scoped service names.
     * <p>
     * Note, that this method returns a copy of the underlying map
     */
    public Map<Service, String> getServiceMap()
        {
        return new LinkedHashMap<>(m_mapServices);
        }

    /**
     * Start the given {@link Service}.  Extensions of this class can
     * override this method to provide pre/post start functionality.
     *
     * @param service  the {@link Service} to start
     */
    protected void startService(Service service)
        {
        service.start();
        }

    /**
     * Set the class loader used to load the configuration for this factory.
     *
     * @param loader  the class loader to use for loading the configuration
     */
    protected void setConfigClassLoader(ClassLoader loader)
        {
        m_loader = ensureClassLoader(loader);
        }

    /**
     * Return the class loader used to load the configuration for this factory.
     *
     * @return the class loader to use for loading the configuration
     */
    protected ClassLoader getConfigClassLoader()
        {
        return m_loader;
        }

    /**
     * Check if the current user is allowed to "join" the cache.
     *
     * @param cache  the cache
     *
     */
    protected static void checkPermission(NamedCache cache)
        {
        checkPermission(cache.getCacheService(), cache.getCacheName());
        }

    /**
     * Check if the current user is allowed to "join" to the collection.
     *
     * @param collection  the collection
     *
     */
    protected static void checkPermission(NamedCollection collection)
        {
        checkPermission(collection.getService(), collection.getName());
        }

    /**
     * Check if the current user is allowed to "join" the data structure
     * with the specified name.
     *
     * @param service  the service
     * @param sName    the data structure name
     *
     */
    protected static void checkPermission(Service service, String sName)
        {
        // This is a secondary check to make sure that cached references
        // are allowed with the current Subject. The primary check is in
        // SafeCacheService.ensureCache().
        Security.checkPermission(service.getCluster(),
            service.getInfo().getServiceName(), sName, "join");
        }

    /**
     * Return the ParameterResolver that has been initialized with the built-in
     * Coherence parameters. Schemes may use expressions (macros) and the resolver
     * contains the parameters that are defined in the cache mapping needed to
     * translate those expressions into values.
     *
     * @param sCacheName   the cache name
     * @param loader       the ClassLoader
     * @param ctxBMM       the BackingMapManagerContext
     *
     * @return the ParameterResolver
     */
    public ParameterResolver getParameterResolver(final String sCacheName, ClassLoader loader,
            BackingMapManagerContext ctxBMM)
        {
        return getParameterResolver(sCacheName, ResourceMapping.class, loader, ctxBMM);
        }

    /**
     * Return the ParameterResolver that has been initialized with the built-in
     * Coherence parameters. Schemes may use expressions (macros) and the resolver
     * contains the parameters that are defined in the cache mapping needed to
     * translate those expressions into values.
     *
     * @param sResourceName       the resource name
     * @param clzResourceMapping  resource type
     * @param loader              the ClassLoader
     * @param ctxBMM              the BackingMapManagerContext
     *
     * @return the ParameterResolver
     *
     * @since 14.1.1.0
*/
    public <M extends ResourceMapping> ParameterResolver getParameterResolver(final String sResourceName, Class<M> clzResourceMapping, ClassLoader loader,
                                                  BackingMapManagerContext ctxBMM)
        {
        ResourceMapping         mapping  = f_cacheConfig.getMappingRegistry().findMapping(sResourceName, clzResourceMapping);
        ScopedParameterResolver resolver = new ScopedParameterResolver(mapping == null
            ? f_cacheConfig.getDefaultParameterResolver()
            : mapping.getParameterResolver());

        // add the standard coherence parameters to the parameter provider
        if (mapping != null)
            {
            resolver.add(new Parameter(mapping.getConfigElementName(), sResourceName));
            }
        resolver.add(new Parameter("class-loader", loader));
        resolver.add(new Parameter("manager-context", ctxBMM));

        return resolver;
        }

    /**
     * Return the ParameterResolver that has been initialized with the built-in
     * Coherence parameters. Schemes may use expressions (macros) and the resolver
     * contains the parameters that are defined for components defined at a scheme
     * level and not a cache level.
     *
     * @param loader       the ClassLoader
     * @param ctxBMM       the BackingMapManagerContext
     *
     * @return the ParameterResolver
     */
    public ParameterResolver createParameterResolver(ClassLoader loader, BackingMapManagerContext ctxBMM)
        {
        ScopedParameterResolver resolver = new ScopedParameterResolver(f_cacheConfig.getDefaultParameterResolver());

        // add the standard coherence parameters to the parameter provider
        resolver.add(new Parameter("class-loader", loader));
        resolver.add(new Parameter("manager-context", ctxBMM));
        return resolver;
        }

    /**
     * Register the specified BackingMapManager as a "valid" one. That registry
     * is used to identify services configured and started by this factory and
     * prevent accidental usage of (potentially incompatible) cache services
     * with the same name created by other factories.
     *
     * @param mgr  a BackingMapManager instance instantiated by this factory
     */
    protected void registerBackingMapManager(BackingMapManager mgr)
        {
        m_setManager.add(mgr);
        }

    /**
     * Ensures that the backing map manager of the specified service was
     * configured by this (or equivalent) factory. This validation is performed
     * to prevent accidental usage of (potentially incompatible) cache services
     * with the same name created by other factories.
     *
     * @param service  the CacheService to validate
     *
     * @throws IllegalStateException  if the backing map for the provided service
     *                                does not reference the expected factory
     */
    protected void validateBackingMapManager(CacheService service)
            throws IllegalStateException
        {
        BackingMapManager manager = service.getBackingMapManager();

        if (m_setManager.contains(manager))
            {
            return;
            }

        if (!(manager instanceof Manager))
            {
            throw new IllegalStateException(
                "Service \"" + service.getInfo().getServiceName() + "\" has been started "
                + (manager == null
                   ? "without a BackingMapManager"
                   : "with a non-compatible BackingMapManager: " + manager));
            }

        ConfigurableCacheFactory that = ((Manager) manager).getCacheFactory();

        if (!(that instanceof ExtensibleConfigurableCacheFactory))
            {
            throw new IllegalStateException(
                "Service \"" + service.getInfo().getServiceName()
                + "\" has been started by an instance of \"" + that.getClass().getName()
                + "\" instead of \"" + this.getClass().getName() + "\"");
            }

        // note: in the past we use to call getConfig here, but that unnecessarily
        // cloned the xml documents.  this is no longer required as it's
        // impossible to "reset" the xml configuration once this class is instantiated
        XmlElement xmlLegacyConfig = ((ExtensibleConfigurableCacheFactory) that).f_xmlLegacyConfig;

        if (that != this && !Base.equals(xmlLegacyConfig, this.f_xmlLegacyConfig))
            {
            CacheFactory.log("This configurable cache factory config: " + this.f_xmlLegacyConfig, Base.LOG_INFO);
            CacheFactory.log("Other configurable cache factory config: " + xmlLegacyConfig, Base.LOG_INFO);

            throw new IllegalStateException("Service \"" + service.getInfo().getServiceName()
                + "\" has been started by a different configurable cache factory.");
            }
        }

    /**
     * Release all resources associated with the specified backing map.
     *
     * @param map           the map being released
     * @param mapListeners  map of registered map listeners keyed by the
     *                      corresponding map references
     */
    protected void release(Map map, Map mapListeners)
        {
        // remove known map listener
        if (map instanceof ObservableMap && mapListeners != null)
            {
            MapListener listener = (MapListener) mapListeners.get(map);

            if (listener != null)
                {
                ((ObservableMap) map).removeMapListener(listener);
                mapListeners.remove(map);
                }
            }

        // process recursively
        if (map instanceof LocalCache)
            {
            CacheLoader loader = ((LocalCache) map).getCacheLoader();

            if (loader instanceof MapCacheStore)
                {
                release(((MapCacheStore) loader).getMap(), mapListeners);
                }
            else
                {
                release(loader);
                }
            }
        else if (map instanceof OverflowMap)
            {
            release(((OverflowMap) map).getFrontMap(), mapListeners);
            release(((OverflowMap) map).getBackMap(), mapListeners);
            }
        else if (map instanceof ReadWriteBackingMap)
            {
            ((ReadWriteBackingMap) map).release();
            release(((ReadWriteBackingMap) map).getInternalCache(), mapListeners);
            }
        else if (map instanceof SerializationMap)
            {
            release(((SerializationMap) map).getBinaryStore());
            }
        else if (map instanceof SimpleSerializationMap)
            {
            release(((SimpleSerializationMap) map).getBinaryStore());
            }
        else if (map instanceof BinaryMap)
            {
            ByteBufferManager bufmgr = ((BinaryMap) map).getBufferManager();

            if (bufmgr instanceof MappedBufferManager)
                {
                ((MappedBufferManager) bufmgr).close();
                }
            }

        // regardless of the above, the map may be disposable as well
        if (map instanceof Disposable)
            {
            ((Disposable) map).dispose();
            }
        }

    /**
     * Release all resources associated with the specified loader.
     *
     * @param loader  the cache loader being released
     */
    protected void release(CacheLoader loader)
        {
        if (loader instanceof Disposable)
            {
            ((Disposable) loader).dispose();
            }
        else
            {
            try
                {
                ClassHelper.invoke(loader, "close", ClassHelper.VOID);
                }
            catch (Exception e)
                {
                }
            }
        }

    /**
     * Release all resources associated with the specified binary store.
     *
     * @param store  the binary store being released
     */
    protected void release(BinaryStore store)
        {
        if (store instanceof Disposable)
            {
            ((Disposable) store).dispose();
            }
        else
            {
            try
                {
                ClassHelper.invoke(store, "close", ClassHelper.VOID);
                }
            catch (Exception e)
                {
                }
            }
        }

    /**
     * Release a cache managed by this factory, optionally destroying it.
     *
     * @param cache     the cache to release
     * @param fDestroy  true to destroy the cache as well
     */
    protected void releaseCache(NamedCache cache, boolean fDestroy)
        {
        String      sCacheName = cache.getCacheName();
        ClassLoader loader     = cache instanceof ClassLoaderAware
                             ? ((ClassLoaderAware) cache).getContextClassLoader() : getContextClassLoader();

        if (f_store.releaseCache(cache, loader)) // free the resources
            {
            // allow cache to release/destroy internal resources
            if (fDestroy)
                {
                cache.destroy();
                }
            else
                {
                cache.release();
                }
            }
        else if (cache.isActive())
            {
            // active, but not managed by this factory
            throw new IllegalArgumentException("The cache " + sCacheName
                + " was created using a different factory; that same"
                + " factory should be used to release the cache.");
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseTopic(NamedTopic<?> topic)
        {
        releaseCollection(topic, /* fDestroy */ false, f_storeTopics);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroyTopic(NamedTopic<?> topic)
        {
        releaseCollection(topic, /* fDestroy */ true, f_storeTopics);
        }

    /**
     * Release a {@link NamedCollection} managed by this factory, optionally destroying it.
     *
     * @param collection  the collection to release
     * @param fDestroy     true to destroy the collection as well
     */
    private  <C extends NamedCollection> void releaseCollection(C collection, boolean fDestroy,
            ScopedReferenceStore<C> store)
        {
        String      sName   = collection.getName();
        ClassLoader loader  = collection instanceof ClassLoaderAware
                ? ((ClassLoaderAware) collection).getContextClassLoader() : getContextClassLoader();

        if (store.release(collection, loader)) // free the resources
            {
            // allow collection to release/destroy internal resources
            if (fDestroy)
                {
                collection.destroy();
                }
            else
                {
                collection.release();
                }
            }
        else if (collection.isActive())
            {
            // active, but not managed by this factory
            throw new IllegalArgumentException("The collection " + sName
                + " was created using a different factory; that same"
                + " factory should be used to release the collection.");
            }
        }

    /**
    * {@inheritDoc}
    */
    public boolean isCacheActive(String sCacheName, ClassLoader loader)
        {
        return f_store.getCache(sCacheName, ensureClassLoader(loader)) != null;
        }

    // ----- Dependencies interface -----------------------------------------

    /**
     * The {@link Dependencies} for the {@link ExtensibleConfigurableCacheFactory}.
     */
    public static interface Dependencies
        {
        /**
         * Obtains {@link CacheConfig} for an {@link ExtensibleConfigurableCacheFactory}.
         *
         * @return  the {@link CacheConfig}
         */
        public CacheConfig getCacheConfig();

        /**
         * Obtains the {@link ClassLoader} than an {@link ExtensibleConfigurableCacheFactory}
         * should use for loading classes.
         *
         * @return  the context {@link ClassLoader}
         */
        public ClassLoader getContextClassLoader();

        /**
         * Obtains the {@link ResourceRegistry} for an {@link ExtensibleConfigurableCacheFactory}.
         *
         * @return  the {@link ResourceRegistry}
         */
        public ResourceRegistry getResourceRegistry();
        }

    // ----- inner DefaultDependencies class --------------------------------

    /**
     * The {@link DefaultDependencies} is a simple implementation of
     * the {@link ExtensibleConfigurableCacheFactory} {@link Dependencies}
     * interface.
     */
    public static class DefaultDependencies
            implements Dependencies
        {
        /**
         * Constructs a {@link DefaultDependencies} with the Context {@link ClassLoader}
         * being the {@link ClassLoader} of the {@link CacheConfig} instance and
         * an empty {@link ResourceRegistry}.
         *
         * @param cacheConfig  the {@link CacheConfig}
         */
        public DefaultDependencies(CacheConfig cacheConfig)
            {
            this(cacheConfig, cacheConfig.getClass().getClassLoader(), new SimpleResourceRegistry());
            }

        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link DefaultDependencies}.
         *
         * @param cacheConfig         the {@link CacheConfig}
         * @param contextClassLoader  the {@link ClassLoader}
         * @param registry    the {@link ResourceRegistry}
         */
        public DefaultDependencies(CacheConfig cacheConfig, ClassLoader contextClassLoader, ResourceRegistry registry)
            {
            Base.azzert(cacheConfig != null);
            Base.azzert(contextClassLoader != null);
            Base.azzert(registry != null);

            m_cacheConfig        = cacheConfig;
            m_contextClassLoader = contextClassLoader;
            m_resourceRegistry   = registry;
            }

        // ----- Dependencies methods ---------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public CacheConfig getCacheConfig()
            {
            return m_cacheConfig;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public ClassLoader getContextClassLoader()
            {
            return m_contextClassLoader;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public ResourceRegistry getResourceRegistry()
            {
            return m_resourceRegistry;
            }

        // ----- data members -----------------------------------------------

        private CacheConfig      m_cacheConfig;
        private ClassLoader      m_contextClassLoader;
        private ResourceRegistry m_resourceRegistry;
        }

    // ----- DependenciesHelper class --------------------------------------

    /**
     * The {@link DependenciesHelper} provides helper method for constructing
     * {@link Dependencies} implementations for {@link ExtensibleConfigurableCacheFactory}s.
     */
    public static class DependenciesHelper
        {
        /**
         * Construct an {@link ExtensibleConfigurableCacheFactory}
         * {@link Dependencies} instance based on Coherence defaults.
         */
        public static Dependencies newInstance()
            {
            return newInstance(FILE_CFG_CACHE, null);
            }

        /**
         * Construct an {@link ExtensibleConfigurableCacheFactory}
         * {@link Dependencies} instance based on the information defined by
         * the specified cache configuration file/resource that of which is
         * compliant with the "coherence-cache-config.xsd".
         *
         * @param sPath  the configuration resource name or file path
         */
        public static Dependencies newInstance(String sPath)
            {
            return newInstance(sPath, null);
            }

        /**
         * Construct an {@link ExtensibleConfigurableCacheFactory}
         * {@link Dependencies} instance based on the information defined by
         * the specified cache configuration file/resource that of which is
         * compliant with the "coherence-cache-config.xsd".
         *
         * @param sPath               the configuration resource name or file path
         * @param contextClassLoader  the optional {@link ClassLoader} that
         *                            should be used to load configuration resources
         */
        public static Dependencies newInstance(String sPath, ClassLoader contextClassLoader)
            {
            XmlElement xmlConfig = XmlHelper.loadFileOrResource(sPath,
                "Cache Configuration from:" + sPath, contextClassLoader);
            return newInstance(xmlConfig, contextClassLoader, null);
            }

        /**
         * Construct an {@link ExtensibleConfigurableCacheFactory}
         * {@link Dependencies} instance based on the information defined by
         * {@link XmlElement} that of which is compliant with the
         * "coherence-cache-config.xsd".
         *
         * @param xmlConfig  the {@link XmlElement} defining the configuration
         */
        public static Dependencies newInstance(XmlElement xmlConfig)
            {
            return newInstance(xmlConfig, null, null);
            }

        /**
         * Construct an {@link ExtensibleConfigurableCacheFactory}
         * {@link Dependencies} instance based on the information defined by
         * {@link XmlElement} that of which is compliant with the
         * "coherence-cache-config.xsd".
         *
         * @param xmlConfig           the {@link XmlElement} defining the configuration
         * @param contextClassLoader  the optional {@link ClassLoader} that
         *                            should be used to load configuration resources
         */
        public static Dependencies newInstance(XmlElement xmlConfig, ClassLoader contextClassLoader)
            {
            return newInstance(xmlConfig, contextClassLoader, null);
            }

        /**
         * Construct an {@link ExtensibleConfigurableCacheFactory}
         * {@link Dependencies} instance based on the information defined by
         * {@link XmlElement} that of which is compliant with the
         * "coherence-cache-config.xsd".
         *
         * @param xmlConfig           the {@link XmlElement} defining the configuration
         * @param contextClassLoader  the optional {@link ClassLoader} that
         *                            should be used to load configuration resources
         * @param sPofConfigUri       the optional {@link URI} of the POF configuration
         *                            file
         */
        public static Dependencies newInstance(XmlElement xmlConfig, ClassLoader contextClassLoader,
                String sPofConfigUri)
            {
            return newInstance(xmlConfig, contextClassLoader, sPofConfigUri, null);
            }

        /**
         * Construct an {@link ExtensibleConfigurableCacheFactory}
         * {@link Dependencies} instance based on the information defined by
         * {@link XmlElement} that of which is compliant with the
         * "coherence-cache-config.xsd".
         *
         * @param xmlConfig      the {@link XmlElement} defining the configuration
         * @param loader         an optional {@link ClassLoader} that
         *                       should be used to load configuration resources
         * @param sPofConfigUri  an optional {@link URI} of the POF configuration file
         * @param sScopeName     an optional scope name
         */
        public static Dependencies newInstance(XmlElement xmlConfig, ClassLoader loader,
                String sPofConfigUri, String sScopeName)
            {
            loader = Base.ensureClassLoader(loader);

            // establish a default ParameterResolver based on the System properties
            // COH-9952 wrap the code in privileged block for upstack products
            ScopedParameterResolver resolver = AccessController.
                doPrivileged(new PrivilegedAction<ScopedParameterResolver>()
                {
                public ScopedParameterResolver run()
                    {
                    return new ScopedParameterResolver(
                        new ChainedParameterResolver(
                            new SystemPropertyParameterResolver(),
                            new SystemEnvironmentParameterResolver()));
                    }
                });

            // create the ResourceRegistry for the Dependencies
            ResourceRegistry resourceRegistry = new SimpleResourceRegistry();

            // establish an InterceptorRegistry & EventDispatcherRegistry as
            // part of the ResourceRegistry
            Registry eventRegistry = new Registry();

            resourceRegistry.registerResource(InterceptorRegistry.class, eventRegistry);
            resourceRegistry.registerResource(EventDispatcherRegistry.class, eventRegistry);

            if (sScopeName != null)
                {
                resourceRegistry.registerResource(String.class, "scope-name", sScopeName);
                }

            // the default parameter resolver always contains the pof-config-uri
            // (this is used internally)
            resolver.add(new Parameter("pof-config-uri", sPofConfigUri));

            // create a reference to the xml document containing the cache
            // configuration to process
            XmlDocumentReference docRef = new XmlDocumentReference(xmlConfig.toString());

            // create and configure the DocumentProcessor Dependencies
            DocumentProcessor.DefaultDependencies dependencies =
                new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

            // finish configuring the dependencies
            dependencies.setResourceRegistry(resourceRegistry);
            dependencies.setDefaultParameterResolver(resolver);
            dependencies.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
            dependencies.setClassLoader(loader);

            // use a DocumentProcessor to create our CacheConfig
            DocumentProcessor processor   = new DocumentProcessor(dependencies);
            CacheConfig       cacheConfig = processor.process(docRef);

            InterceptorManager manager = new InterceptorManager(cacheConfig,
                loader, resourceRegistry);
            resourceRegistry.registerResource(InterceptorManager.class, manager);

            return new DefaultDependencies(cacheConfig, loader, resourceRegistry);
            }
        }

    // ----- Manager class -------------------------------------------------

    /**
     * The Manager class uses builders to create the required backing maps
     * and provides client access to those maps.
     * <p>
     * This class also implements methods to create/release backup maps as
     * needed by PartitionedCache$Storage$BackingManager.
     */
    public static class Manager
            extends AbstractBackingMapManager
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct the backing map manager.
         *
         * @param factory  the factory associated with this manager
         */
        public Manager(ExtensibleConfigurableCacheFactory factory)
            {
            m_factory = factory;
            }

        // ----- BackingMapManager interface --------------------------------

        /**
         * {@inheritDoc}
         * <p>
         * <b>Important note:</b> BackingMapManager cannot be associated with more
         * than one instance of a CacheService. However, in a situation when a
         * CacheService automatically restarts, it is possible that this manager
         * instance is re-used by a newly created (restarted) CacheService
         * calling this method once again providing a new context.
         */
        @Override
        public void init(BackingMapManagerContext context)
            {
            super.init(context);

            m_mapBackingMap          = new HashMap<>();
            m_mapBackingMapListeners = new IdentityHashMap<>();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map instantiateBackingMap(String sName)
            {
            boolean                  fPartitioned = false;
            ParameterResolver        resolver     = getResolver(sName);
            BackingMapManagerContext context      = getContext();
            ClassLoader              loader       = context.getClassLoader();
            CachingScheme            scheme       = findCachingScheme(sName);

            if (scheme == null)
                {
                throw new IllegalArgumentException("BackingMapManager cannot find a CachingScheme for cache " + sName);
                }

            // create the context needed by the map builders to realize a map
            ExtensibleConfigurableCacheFactory factory = getCacheFactory();
            MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(factory, context, loader, sName,
                                                       context.getCacheService().getInfo().getServiceType(),
                                                       m_mapBackingMapListeners);

            // if the scheme is clustered then get the partitioned flag and builder from the
            // backing map scheme
            if (scheme instanceof ClusteredCachingScheme)
                {
                BackingMapScheme schemeBackingMap = ((ClusteredCachingScheme) scheme).getBackingMapScheme();

                scheme       = schemeBackingMap.getInnerScheme();
                fPartitioned = schemeBackingMap.isPartitioned(resolver, false);
                }

            // get the builder that will create the map which is returned by this method, except in
            // the case of non-RWBM partitioned maps where a partitioned aware map is returned
            MapBuilder bldrMap = scheme;
            Map map = fPartitioned
                      ? instantiatePartitionedBackingMap(bldrMap, resolver, dependencies, scheme)
                      : bldrMap.realizeMap(resolver, dependencies);

            // add the backing map listener if the scheme is observable
            if (scheme instanceof ObservableCachingScheme && map instanceof ObservableMap)
                {
                ObservableCachingScheme schemeObservable = (ObservableCachingScheme)scheme;

                schemeObservable.establishMapListeners(map, resolver, dependencies);
                }

            setBackingMap(sName, map);

            MBeanHelper.registerCacheMBean(context.getCacheService(), sName, "tier=back", map);

            return map;
            }

        /**
         * Instantiate a partitioned backing map (an instance of {@link ObservableSplittingBackingMap})
         * using {@link PartitionedBackingMapManager}. If the provided scheme is an instance of
         * {@link ReadWriteBackingMapScheme}, the internal scheme's map builder is used to build
         * the backing map.
         *
         * @param bldrMap       the {@link MapBuilder} for partitions
         * @param resolver      the {@link ParameterizedBuilder}
         * @param dependencies  the {@link Dependencies} for {@link MapBuilder}s
         * @param scheme        the {@link CachingScheme} of the requested cache
         *
         * @return  partitioned backing map that will provide backing storage for the specified cache
         */
        protected Map instantiatePartitionedBackingMap(MapBuilder bldrMap, ParameterResolver resolver,
            MapBuilder.Dependencies dependencies, CachingScheme scheme)
            {
            ReadWriteBackingMapScheme schemeRwbm = scheme instanceof ReadWriteBackingMapScheme
                                                   ? (ReadWriteBackingMapScheme) scheme : null;

            MapBuilder bldrPartition = schemeRwbm == null ? bldrMap : schemeRwbm.getInternalScheme();

            Base.checkNotNull(bldrPartition, "The BackingMapContext is missing a partition map builder");

            PartitionedBackingMapManager mgrInner = new PartitionedBackingMapManager(getCacheFactory(), dependencies,
                                                        resolver, bldrPartition);

            mgrInner.init(getContext());

            // create the partition aware backing map (ObservableSplittingBackingMap) which will
            // instantiate individual partitions by calling the "mgrInner" PartitionedBackingMapManager
            // for each partition as needed
            String sName = dependencies.getCacheName();

            ObservableSplittingBackingMap pabm = new ObservableSplittingBackingCache(mgrInner, sName);

            if (schemeRwbm == null)
                {
                return pabm;
                }
            else
                {
                // let the RWBM scheme use the new ObservableSplittingBackingMap for its internal map,
                // which allows RWBM to use partitions
                schemeRwbm.setInternalMap(pabm);

                // create a new RWBM to return
                return bldrMap.realizeMap(resolver, dependencies);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isBackingMapPersistent(String sName)
            {
            DistributedScheme schemeDist = findDistributedScheme(sName);
            ParameterResolver resolver   = getResolver(sName);

            return !schemeDist.getBackingMapScheme().isTransient(resolver);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isBackingMapSlidingExpiry(String sName)
            {
            DistributedScheme schemeDist = findDistributedScheme(sName);
            ParameterResolver resolver   = getResolver(sName);
            return schemeDist.getBackingMapScheme().isSlidingExpiry(resolver);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public StorageAccessAuthorizer getStorageAccessAuthorizer(String sName)
            {
            DistributedScheme  schemeDist       = findDistributedScheme(sName);
            BackingMapScheme   schemeBackingMap = schemeDist == null
                    ? null : schemeDist.getBackingMapScheme();
            Expression<String> exprAuthorizer   = schemeBackingMap == null
                    ? null : schemeBackingMap.getStorageAccessAuthorizer();

            if (exprAuthorizer == null)
                {
                return null;
                }

            ParameterResolver resolver    = getResolver(sName);
            String            sAuthorizer = exprAuthorizer.evaluate(resolver);

            ClusterDependencies          dependencies = CacheFactory.getCluster().getDependencies();
            ParameterizedBuilderRegistry registry     = dependencies.getBuilderRegistry();

            ParameterizedBuilder<StorageAccessAuthorizer> builder = registry.getBuilder(
                    StorageAccessAuthorizer.class, sAuthorizer);

            if (builder == null)
                {
                throw new IllegalArgumentException("Configuration error: backing map of scheme \"" +
                        schemeDist.getSchemeName() + "\" references undefined storage-authorizer \"" +
                        sAuthorizer + "\"");
                }

            try
                {
                return builder.realize(resolver, getContext().getClassLoader(), null);
                }
            catch (RuntimeException e)
                {
                throw new IllegalArgumentException("Configuration error: received exception " +
                        e.getClass().getSimpleName() + " during instantiation of storage-authorizer \"" +
                        sAuthorizer + "\" configured within backing map of scheme \"" +
                        schemeDist.getSchemeName() + "\"", e);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void releaseBackingMap(String sName, Map map)
            {
            MBeanHelper.unregisterCacheMBean(getContext().getCacheService(), sName, "tier=back");

            getCacheFactory().release(map, m_mapBackingMapListeners);

            setBackingMap(sName, null);
            }

        // ----- Manager methods --------------------------------------------

        /**
         * Instantiate a [thread safe] Map that should be used by a CacheService
         * to store cached values for a NamedCache with the specified name.
         *
         * @param sName  the name of the NamedCache for which this map is
         *               being created
         *
         * @return an object implementing the Map interface that will provide
         *         backing storage for the specified cache name
         */
        public Map instantiateBackupMap(String sName)
            {
            Map                                map;
            ParameterResolver                  resolver   = getResolver(sName);
            BackingMapManagerContext           context    = getContext();
            ClassLoader                        loader     = context.getClassLoader();
            ExtensibleConfigurableCacheFactory factory    = getCacheFactory();
            DistributedScheme                  schemeDist = findDistributedScheme(sName);

            // get the type of backup map and process ON_HEAP immediately since it doesn't
            // require a scheme or a backup-storage configuration
            int nType = getBackupMapType(schemeDist, resolver);

            if (nType == BackingMapScheme.ON_HEAP)
                {
                return new SafeHashMap();
                }

            // create the context needed by the map builders to realize a map
            MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(getCacheFactory(), context, loader,
                                                       sName, context.getCacheService().getInfo().getServiceType());

            dependencies.setBackup(true);

            // COH-7138 : flashJournal is explicitly set by default if the backing map is any journal,
            // see DistributedScheme.BackupMapConfig.resolveType()
            if (nType == BackingMapScheme.FLASHJOURNAL)
                {
                // if the BackingMapScheme is a FlashJournalScheme then use it, else create a temporary one
                // for the backup map.
                AbstractLocalCachingScheme<?> journalScheme = schemeDist.getBackingMapScheme();

                journalScheme = journalScheme instanceof FlashJournalScheme ? journalScheme : new FlashJournalScheme();

                return journalScheme.realizeMap(resolver, dependencies);
                }

            // all other types must have a backup-storage configuration in the scheme
            BackupMapConfig configBackup = schemeDist.getBackupMapConfig();

            if (configBackup == null)
                {
                throw new IllegalStateException("Backup map cannot be created"
                                                + " because the backup configuration is missing");
                }

            // Bounds check:
            // 1 <= cbInitSize <= cbMaxSize <= Integer.MAX_VALUE - 1023
            // (Integer.MAX_VALUE - 1023 is the largest integer multiple of 1024)
            int cbMax = (int) Math.min(Math.max(configBackup.getMaximumSize(resolver), 1L),
                                       (long) Integer.MAX_VALUE - 1023);
            int cbInit = (int) Math.min(Math.max(configBackup.getInitialSize(resolver), 1L), cbMax);

            switch (nType)
                {
                case BackingMapScheme.CUSTOM :
                    {
                    ParameterizedBuilder<Map> bldrCustom = configBackup.getCustomBuilder();

                    if (bldrCustom == null)
                        {
                        throw new IllegalArgumentException("Custom backup type specified "
                                                           + "but the class-name is missing");
                        }

                    // Create the custom object that is extending LocalCache. First
                    // populate the relevant constructor arguments then create the cache
                    ParameterList listArgs = new ResolvableParameterList();

                    map = bldrCustom.realize(resolver, loader, listArgs);
                    break;
                    }

                case BackingMapScheme.FILE_MAPPED :
                    {
                    String sPath = configBackup.getDirectory(resolver);
                    File   dir   = sPath.length() == 0 ? null : new File(sPath);

                    map = new BinaryMap(new MappedBufferManager(cbInit, cbMax, dir));
                    break;
                    }

                case BackingMapScheme.SCHEME :
                    {
                    // Lookup the caching scheme that specifies how to build the backup map
                    String sScheme = configBackup.getBackupSchemeName(resolver);

                    Base.checkNotEmpty(sName, "Backup storage scheme name");

                    CachingScheme schemeRef = (CachingScheme) factory.getCacheConfig().findSchemeBySchemeName(sScheme);

                    Base.checkNotNull(schemeRef, "Backup map scheme");

                    map = schemeRef.realizeMap(resolver, dependencies);
                    break;
                    }

                default :
                    throw new IllegalStateException("Unknown backup storage type: " + nType);
                }

            return map;
            }

        /**
         * Release the specified Map that was created using the
         * {@link #instantiateBackupMap(String)} method. This method is invoked
         * by the CacheService when the CacheService no longer requires the
         * specified Map object.
         *
         * @param sName         the cache name
         * @param map           the Map object that is being released
         * @param mapListeners  the map of listeners for the map
         */
        public void releaseBackupMap(String sName, Map map, Map mapListeners)
            {
            if (map instanceof SafeHashMap)
                {
                return;
                }

            try
                {
                DistributedScheme schemeDist = findDistributedScheme(sName);
                ParameterResolver resolver   = getResolver(sName);

                int nType = getBackupMapType(schemeDist, resolver);
                switch (nType)
                    {
                    case BackingMapScheme.FILE_MAPPED :
                        {
                        map.clear();

                        try
                            {
                            ByteBufferManager bufferMgr = ((BinaryMap) map).getBufferManager();

                            ((MappedBufferManager) bufferMgr).close();
                            }
                        catch (ClassCastException e)
                            {
                            }

                        break;
                        }

                    case BackingMapScheme.OFF_HEAP :
                        {
                        map.clear();
                        break;
                        }

                    case BackingMapScheme.SCHEME :
                    case BackingMapScheme.RAMJOURNAL :
                    case BackingMapScheme.FLASHJOURNAL :
                        {
                        try
                            {
                            getCacheFactory().release(map, mapListeners);
                            }
                        catch (ClassCastException e)
                            {
                            }

                        break;
                        }

                    case BackingMapScheme.CUSTOM :
                    case BackingMapScheme.ON_HEAP :
                        break;

                    default :
                        throw new IllegalStateException("Unknown backup storage type: " + nType);
                    }
                }
            catch (Exception e)
                {
                CacheFactory.log("Failed to invalidate backing map: " + e, CacheFactory.LOG_WARN);
                }
            }

        /**
         * Return true if the backup map should be partitioned.
         *
         * @param sName  the cache name
         *
         * @return true if the backup map should be partitioned
         */
        public boolean isBackupPartitioned(String sName)
            {
            DistributedScheme schemeDist = findDistributedScheme(sName);

            if (schemeDist == null)
                {
                throw new IllegalArgumentException("BackingManager cannot find a CachingScheme for cache " + sName);
                }

            ParameterResolver resolver = getResolver(sName);
            int     nType              = getBackupMapType(schemeDist, resolver);
            boolean fPartitionDefault  = nType != BackingMapScheme.OFF_HEAP &&
                                         nType != BackingMapScheme.FILE_MAPPED;

            if (nType == BackingMapScheme.SCHEME)
                {
                String sScheme   = schemeDist.getBackupMapConfig().getBackupSchemeName(resolver);
                Scheme schemeRef = getCacheFactory().getCacheConfig().findSchemeBySchemeName(sScheme);

                fPartitionDefault = !(schemeRef instanceof ExternalScheme)
                                 && !(schemeRef instanceof PagedExternalScheme);
                }

            return fPartitionDefault;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExtensibleConfigurableCacheFactory getCacheFactory()
            {
            return m_factory;
            }

        // ---- helpers -----------------------------------------------------

        /**
         * Return the {@link ParameterResolver} for the given cache.
         *
         * @param sName  the cache name
         *
         * @return the {@link ParameterResolver}
         */
        protected ParameterResolver getResolver(String sName)
            {
            BackingMapManagerContext context = getContext();

            return getCacheFactory().
                getParameterResolver(sName, context.getClassLoader(), context);
            }

        /**
         * Return the {@link ScopedParameterResolver} for the given cache.  A
         * scoped resolver is needed so that a sub-class (like TransactionScheme.Manager)
         * can add a parameter to the resolver.
         *
         * @param sName  the cache name
         *
         * @return the {@link ScopedParameterResolver}
         */
        protected ScopedParameterResolver getScopedResolver(String sName)
            {
            return new ScopedParameterResolver(getResolver(sName));
            }

        /**
         * Get the backing Map associated with a given cache.
         *
         * @param sName  the cache name
         *
         * @return a Map associated with the specified name
         */
        public Map getBackingMap(String sName)
            {
            return m_mapBackingMap == null ? null : m_mapBackingMap.get(sName);
            }

        /**
         * Associate the specified backing Map with a given name.
         *
         * @param sName  the cache name
         * @param map    the backing map associated with the specified name
         */
        protected void setBackingMap(String sName, Map map)
            {
            if (map != null && getBackingMap(sName) != null)
                {
                throw new IllegalArgumentException("BackingMap is not resettable: " + sName);
                }

            m_mapBackingMap.put(sName, map);
            }

        /**
         * Return the {@link DistributedScheme} for a given cache name.
         *
         * @param sName  the cache name
         *
         * @return the {@link DistributedScheme} or null
         */
        protected DistributedScheme findDistributedScheme(String sName)
            {
            CachingScheme scheme = findCachingScheme(sName);

            return scheme instanceof DistributedScheme ? (DistributedScheme) scheme : null;
            }

        /**
         * Return the {@link CachingScheme} for a given cache name.  If the caching
         * scheme is a near cache then return the back scheme.
         *
         * @param sName  the cache name
         *
         * @return the {@link CachingScheme} or null
         */
        protected CachingScheme findCachingScheme(String sName)
            {
            CachingScheme scheme = getCacheFactory().getCacheConfig().findSchemeByCacheName(sName);

            if (scheme == null)
                {
                return null;
                }

            return scheme instanceof AbstractCompositeScheme
                   ? ((AbstractCompositeScheme) scheme).getBackScheme()
                   : scheme;
            }

        /**
         * Return the type of backup storage for the given scheme.
         *
         * @param scheme    the {@link DistributedScheme} that defines the storage configuration
         * @param resolver  the {@link ParameterResolver}
         *
         * @return the backup storage type
         */
        private int getBackupMapType(DistributedScheme scheme, ParameterResolver resolver)
            {
            if (scheme == null)
                {
                return BackingMapScheme.ON_HEAP;
                }

            // configBackup will never be null
            BackupMapConfig configBackup   = scheme.getBackupMapConfig();
            MapBuilder      bldrPrimaryMap = scheme.getBackingMapScheme().getInnerScheme();

            return configBackup.resolveType(resolver, bldrPrimaryMap);
            }

        // ----- data fields ------------------------------------------------

        /**
         * The extensible cache factory associated with this manager.
         */
        private final ExtensibleConfigurableCacheFactory m_factory;

        /**
         * The map of backing maps keyed by corresponding cache names.
         */
        private Map<String, Map> m_mapBackingMap;

        /**
         * The map of backing map listeners keyed by the corresponding backing
         * map references.
         */
        private Map<Map, MapListener> m_mapBackingMapListeners;
        }

    // ----- inner class PartitionedBackingMapManager -----------------------

    /**
     * The PartitionedBackingMapManager is used by PartitionAwareBackingMap(s) to
     * lazily configure the enclosing PABM based on the configuration settings of
     * the enclosed maps.
     */
    public static class PartitionedBackingMapManager
            extends AbstractBackingMapManager
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a PartitionedBackingMapManager object.
         *
         * @param factory       the factory associated with this manager
         * @param dependencies  the {@link MapBuilder} dependencies
         * @param resolver      the ParameterResolver needed to resolve cache params
         * @param bldrMap       the builder that will build the backing map
         */
        protected PartitionedBackingMapManager(ExtensibleConfigurableCacheFactory factory,
            MapBuilder.Dependencies dependencies, ParameterResolver resolver, MapBuilder bldrMap)
            {
            super();

            m_factory      = factory;
            m_dependencies = dependencies;
            m_resolver     = resolver;
            m_bldrMap      = bldrMap;
            }

        // ----- BackingMapManager interface --------------------------------

        /**
         * {@inheritDoc}
         *
         * NOTE: The cache name passed in has a partition number appended
         *       to it so that we cannot use it to find a parameter resolver.
         *       This is why the resolver is passed into the constructor.
         */
        @Override
        public Map instantiateBackingMap(String sName)
            {
            int               nPartition        = parsePartition(sName);
            ParameterResolver resolverPartition = sParam ->
                "partition".equals(sParam)
                    ? new Parameter("partition", new LiteralExpression<>(nPartition))
                    : null;
            return m_bldrMap.realizeMap(new ChainedParameterResolver(resolverPartition, m_resolver), m_dependencies);
            }

        private int parsePartition(String sName)
            {
            String sPartition = sName.substring(sName.lastIndexOf('-') + 1);
            int    nPartition;

            if (Character.isDigit(sPartition.charAt(0)))
                {
                try
                    {
                    nPartition = Integer.parseInt(sPartition);
                    }
                catch (NumberFormatException e)
                    {
                    // synthetic
                    nPartition = -1;
                    }
                }
            else
                {
                nPartition = -1;
                }

            return nPartition;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isBackingMapPersistent(String sName)
            {
            // this method should never be called
            throw new UnsupportedOperationException();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isBackingMapSlidingExpiry(String sName)
            {
            // this method should never be called
            throw new UnsupportedOperationException();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public StorageAccessAuthorizer getStorageAccessAuthorizer(String sName)
            {
            // this method should never be called
            throw new UnsupportedOperationException();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void releaseBackingMap(String sName, Map map)
            {
            getCacheFactory().release(map, null);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExtensibleConfigurableCacheFactory getCacheFactory()
            {
            return m_factory;
            }

        // ----- data fields ------------------------------------------------

        /**
         * The builder that will build the backing map for a single partition.
         */
        private final MapBuilder m_bldrMap;

        /**
         * The extensible cache factory associated with this manager.
         */
        private final ExtensibleConfigurableCacheFactory m_factory;

        /**
         * The map realize context.
         */
        private final MapBuilder.Dependencies m_dependencies;

        /**
         * The {@link ParameterResolver} needed to resolve cache mapping
         * parameters.
         */
        private final ParameterResolver m_resolver;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The default configuration file name.
     */
    public static final String FILE_CFG_CACHE = "coherence-cache-config.xml";

    /**
     * The name of the replaceable parameter representing the cache name.
     */
    public static final String CACHE_NAME = "{cache-name}";

    /**
     * The name of the replaceable parameter representing the class loader.
     */
    public static final String CLASS_LOADER = "{class-loader}";

    /**
     * The name of the replaceable parameter representing the backing map
     * manager context.
     */
    public static final String MGR_CONTEXT = "{manager-context}";

    /**
     * The name of the replaceable parameter representing a scheme reference.
     */
    public static final String SCHEME_REF = "{scheme-ref}";

    /**
     * The name of the replaceable parameter representing a cache reference.
     */
    public static final String CACHE_REF = "{cache-ref}";

    /**
     * The unknown scheme type.
     */
    public static final int SCHEME_UNKNOWN = 0;

    /**
     * The replicated cache scheme.
     */
    public static final int SCHEME_REPLICATED = 1;

    /**
     * The optimistic cache scheme.
     */
    public static final int SCHEME_OPTIMISTIC = 2;

    /**
     * The distributed cache scheme.
     */
    public static final int SCHEME_DISTRIBUTED = 3;

    /**
     * The near cache scheme.
     */
    public static final int SCHEME_NEAR = 4;

    /**
     * The versioned near cache scheme.
     */
    public static final int SCHEME_VERSIONED_NEAR = 5;

    /**
     * The local cache scheme.
     */
    public static final int SCHEME_LOCAL = 6;

    /**
     * The overflow map scheme.
     */
    public static final int SCHEME_OVERFLOW = 7;

    /**
     * The disk scheme.
     *
     * @deprecated As of Coherence 3.0, replaced by {@link #SCHEME_EXTERNAL}
     *             and {@link #SCHEME_EXTERNAL_PAGED}
     */
    public static final int SCHEME_DISK = 8;

    /**
     * The external scheme.
     */
    public static final int SCHEME_EXTERNAL = 9;

    /**
     * The paged-external scheme.
     */
    public static final int SCHEME_EXTERNAL_PAGED = 10;

    /**
     * The custom class scheme.
     */
    public static final int SCHEME_CLASS = 11;

    /**
     * The read write backing map scheme.
     */
    public static final int SCHEME_READ_WRITE_BACKING = 12;

    /**
     * The versioned backing map scheme.
     */
    public static final int SCHEME_VERSIONED_BACKING = 13;

    /**
     * The invocation service scheme.
     */
    public static final int SCHEME_INVOCATION = 14;

    /**
     * The proxy service scheme.
     */
    public static final int SCHEME_PROXY = 15;

    /**
     * The remote cache scheme.
     */
    public static final int SCHEME_REMOTE_CACHE = 16;

    /**
     * The remote invocation scheme.
     */
    public static final int SCHEME_REMOTE_INVOCATION = 17;

    /**
     * The transactional cache scheme.
     */
    public static final int SCHEME_TRANSACTIONAL = 18;

    /**
     * The flash journal cache scheme.
     */
    public static final int SCHEME_FLASHJOURNAL = 19;

    /**
     * The ram journal cache scheme.
     */
    public static final int SCHEME_RAMJOURNAL = 20;

    /**
     * The mappings from scheme name to scheme type.
     */
    public static final HashMap<String, Integer> MAP_SCHEMETYPE_BY_SCHEMENAME = new HashMap<String, Integer>()
        {
            {
            put("replicated-scheme", SCHEME_REPLICATED);
            put("optimistic-scheme", SCHEME_OPTIMISTIC);
            put("distributed-scheme", SCHEME_DISTRIBUTED);
            put("local-scheme", SCHEME_LOCAL);
            put("overflow-scheme", SCHEME_OVERFLOW);
            put("disk-scheme", SCHEME_DISK);
            put("external-scheme", SCHEME_EXTERNAL);
            put("paged-external-scheme", SCHEME_EXTERNAL_PAGED);
            put("class-scheme", SCHEME_CLASS);
            put("near-scheme", SCHEME_NEAR);
            put("versioned-near-scheme", SCHEME_VERSIONED_NEAR);
            put("read-write-backing-map-scheme", SCHEME_READ_WRITE_BACKING);
            put("versioned-backing-map-scheme", SCHEME_VERSIONED_BACKING);
            put("invocation-scheme", SCHEME_INVOCATION);
            put("proxy-scheme", SCHEME_PROXY);
            put("remote-cache-scheme", SCHEME_REMOTE_CACHE);
            put("remote-invocation-scheme", SCHEME_REMOTE_INVOCATION);
            put("transactional-scheme", SCHEME_TRANSACTIONAL);
            put("flashjournal-scheme", SCHEME_FLASHJOURNAL);
            put("ramjournal-scheme", SCHEME_RAMJOURNAL);
            }
        };

    // ----- data members ---------------------------------------------------

    /**
     * The {@link CacheConfig} for the {@link ConfigurableCacheFactory}.
     */
    private final CacheConfig f_cacheConfig;

    /**
     * The configuration {@link XmlElement} originally passed/loaded by the
     * {@link ExtensibleConfigurableCacheFactory}.  This is maintained
     * for legacy purposes and should not be accessed moving forward.
     */
    @Deprecated
    private final XmlElement f_xmlLegacyConfig;

    /**
     * The {@link ResourceRegistry} for this factory.
     */
    private final ResourceRegistry f_registry;

    /**
     * Store that holds cache references scoped by class loader and optionally,
     * if configured, Subject.
     */
    protected final ScopedCacheReferenceStore f_store;

    /**
     * Store that holds {@link NamedTopic} references scoped by class loader and optionally,
     * if configured, Subject.
     */
    protected final ScopedReferenceStore<NamedTopic> f_storeTopics;

    /**
     * ConfigurableCacheFactoryDispatcher linked to this cache factory.
     */
    protected final ConfigurableCacheFactoryDispatcher f_dispatcher;

    /**
     * The class loader used to load the configuration.
     */
    private ClassLoader m_loader;

    /**
     * Map used to hold references to services that are ensured by this
     * factory where values are non-scoped service names.
     */
    protected Map<Service, String> m_mapServices = new WeakHashMap<>();

    /**
     * A Set of BackingMapManager instances registered by this factory.
     * <p>
     * Note: we rely on the BackingMapManager classes *not* to override the
     * hashCode() and equals() methods.
     */
    protected Set<BackingMapManager> m_setManager = new MapSet(new WeakHashMap());

    /**
     * Indicates whether this factory has been activated.
     */
    protected boolean m_fActivated = false;

    /**
     * Indicates whether this factory has been disposed.
     */
    protected boolean m_fDisposed = false;
    }
