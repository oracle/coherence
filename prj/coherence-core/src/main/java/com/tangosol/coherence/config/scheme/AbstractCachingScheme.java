/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.injection.Injector;
import com.tangosol.config.injection.SimpleInjector;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import com.tangosol.net.ServiceDependencies;
import com.tangosol.net.cache.BundlingNamedCache;
import com.tangosol.util.Base;
import com.tangosol.util.MapListener;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.ResourceResolver;
import com.tangosol.util.ResourceResolverHelper;

import java.util.Map;

/**
 * An {@link AbstractCachingScheme} is a base implementation for an
 * {@link CachingScheme}.
 *
 * @author pfm  2011.12.28
 * @since Coherence 12.1.2
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractCachingScheme<D extends ServiceDependencies>
        extends AbstractServiceScheme<D>
        implements ObservableCachingScheme
    {
    // ----- NamedCacheBuilder interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public NamedCache realizeCache(ParameterResolver resolver, Dependencies dependencies)
        {
        validate(resolver);

        // Call ECFF to ensure the Service. CCF must be used to ensure the service, rather
        // than the service builder.  This is because ECCF.ensureService provides additional
        // logic like injecting a BackingMapManager into the service and starting the Service.
        Service service =
            ((ExtensibleConfigurableCacheFactory) dependencies.getConfigurableCacheFactory()).ensureService(this);

        if (!(service instanceof CacheService))
            {
            throw new IllegalArgumentException("Error: ensureCache is using service "
                                               + service.getInfo().getServiceName() + "that is not a CacheService ");
            }

        NamedCache cache = ((CacheService) service).ensureCache(dependencies.getCacheName(),
                               dependencies.getClassLoader());

        // appropriately produce a BundlingNamedCache should bundling be supported
        if (this instanceof BundlingScheme)
            {
            BundleManager mgrBundle = ((BundlingScheme) this).getBundleManager();

            if (mgrBundle != null)
                {
                BundlingNamedCache cacheBundle = new BundlingNamedCache(cache);

                mgrBundle.ensureBundles(resolver, cacheBundle);
                cache = cacheBundle;
                }
            }

        return cache;
        }

    // ----- MapBuilder interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Map realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        throw new IllegalStateException("This scheme " + getSchemeName() + " cannot be used to create a Map");
        }

    // ----- BackingMapManagerBuilder interface -----------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public BackingMapManager realizeBackingMapManager(ConfigurableCacheFactory ccf)
        {
        if (ccf instanceof ExtensibleConfigurableCacheFactory)
            {
            return new ExtensibleConfigurableCacheFactory.Manager((ExtensibleConfigurableCacheFactory) ccf);
            }
        else
            {
            throw new IllegalArgumentException("The BackingMapManager cannot be must be instantiated"
                                               + "with a given a ExtensibleConfigurableCacheFactory");
            }
        }

    // ----- ObservableCachingScheme interface ------------------------------

    /**
     * {@inheritDoc}
     */
    public ParameterizedBuilder<MapListener> getListenerBuilder()
        {
        return m_bldrListener;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void establishMapListeners(Map map, ParameterResolver resolver, Dependencies dependencies)
        {
        if (map instanceof ObservableMap && m_bldrListener != null)
            {
            Object oListener = m_bldrListener.realize(resolver, dependencies.getClassLoader(), null);

            if (oListener instanceof MapListener)
                {
                MapListener listener = (MapListener) oListener;

                // this is the ParameterResolver that will be used to resolve
                // parameters that themselves reference other parameters
                // (this could be the system-level default parameter resolver)
                Injector injector = new SimpleInjector();
                ResourceResolver resourceResolver =
                    ResourceResolverHelper.resourceResolverFrom(ResourceResolverHelper.resourceResolverFrom(resolver,
                        getDefaultParameterResolver()), ResourceResolverHelper.resourceResolverFrom(dependencies));

                listener = injector.inject(listener, resourceResolver);

                ObservableMap mapObservable = (ObservableMap) map;

                mapObservable.addMapListener(listener);

                if (dependencies.getMapListenersRegistry() != null)
                    {
                    dependencies.getMapListenersRegistry().put(mapObservable, listener);
                    }
                }
            else
                {
                throw new IllegalArgumentException("The specified MapListener [" + oListener + "] for the cache ["
                                                   + dependencies.getCacheName()
                                                   + "] does not implement the MapListener interface");
                }
            }
        }

    // ----- internal -------------------------------------------------------

    /**
     * Set the {@link ParameterizedBuilder} that builds a {@link MapListener}.
     *
     * @param bldr  the {@link ParameterizedBuilder}
     */
    @Injectable("listener")
    public void setListenerBuilder(ParameterizedBuilder<MapListener> bldr)
        {
        m_bldrListener = bldr;
        }

    /**
     * Obtains the ParameterResolver to use when resolving parameters
     * without one being available in a context.
     *
     * @return  a ParameterResolver
     */
    public ParameterResolver getDefaultParameterResolver()
        {
        return new NullParameterResolver();
        }

    /**
     * Validate the properties.
     *
     * @param resolver  the ParameterResolver needed to resolve expressions
     */
    protected void validate(ParameterResolver resolver)
        {
        super.validate();

        Base.checkNotNull(resolver, "ParameterResolver");
        }

    // ----- data members  --------------------------------------------------

    /**
     * The {@link ParameterizedBuilder} for the {@link MapListener}.
     */
    private ParameterizedBuilder<MapListener> m_bldrListener;
    }
