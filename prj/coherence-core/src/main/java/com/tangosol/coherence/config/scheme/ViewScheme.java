/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.collections.ConcurrentHashMap;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.service.DefaultViewDependencies;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;

import com.tangosol.net.Service;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.net.internal.ViewCacheService;

import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;

/**
 * A Scheme that realizes both services and caches for Coherence 12.2.1.4 feature
 * named 'views'.
 *
 * @author hr  2019.06.11
 * @since 12.2.1.4
 */
// Internal Notes:
// The approach is to allow CQC's to be defined in the cache configuration and
// surfaced via CCF.ensureCache. To that end we define a CacheServie that can
// hold references to NamedCaches that represent views of other caches. This
// CacheService can be started by DCS like any regular CacheService and ensures
// caches are primed eagerly with data to have similar behavior semantics as
// replicated caches. Unlike regular services this service does not have its
// own type and is *not* instantiated or maintained by the cluster 'consciously'
// (we use the cluster's resource registry to store these services).
public class ViewScheme
        extends AbstractCompositeScheme<ContinuousQueryCache>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ViewScheme.
     */
    public ViewScheme()
        {
        m_serviceDependencies = new DefaultViewDependencies();

        setFrontScheme(NO_SCHEME);

        DistributedScheme schemeBack = new DistributedScheme();
        schemeBack.setServiceName(getServiceType());
        setBackScheme(schemeBack);
        }

    // ----- ServiceBuilder interface ---------------------------------------

    @Override
    public Service realizeService(ParameterResolver resolver, ClassLoader loader, Cluster cluster)
        {
        validate();

        // see if we've already created the appropriate CacheService
        ResourceRegistry registry = cluster.getResourceRegistry();

        ConcurrentHashMap<String, Service> mapServices =
                registry.getResource(ConcurrentHashMap.class, ViewCacheService.KEY_CLUSTER_REGISTRY);

        if (mapServices == null)
            {
            registry.registerResource(
                    ConcurrentHashMap.class,
                    ViewCacheService.KEY_CLUSTER_REGISTRY,
                    ConcurrentHashMap::new,
                    RegistrationBehavior.IGNORE,
                    null);

            mapServices = registry.getResource(ConcurrentHashMap.class, ViewCacheService.KEY_CLUSTER_REGISTRY);
            }

        String  sServiceName = getScopedServiceName();
        Service service      = mapServices.get(sServiceName);
        if (service != null)
            {
            return service;
            }

        // ensure the back service; the service is either already started or
        // will be started as a part of the ViewCacheService
        CachingScheme schemeBack = getBackScheme();
        if (schemeBack instanceof AbstractScheme)
            {
            ((AbstractScheme) schemeBack).validate();
            }
        CacheService serviceBack = (CacheService) schemeBack.getServiceBuilder()
                .realizeService(resolver, loader, cluster);

        DefaultViewDependencies deps = (DefaultViewDependencies) m_serviceDependencies;

        deps.setBackService(serviceBack);

        service = new ViewCacheService(serviceBack);
        service.setDependencies(deps);

        if (mapServices.putIfAbsent(sServiceName, service) != null)
            {
            service = mapServices.get(sServiceName); // highly unlikely path
            }

        return service;
        }

    // ----- BackingMapManagerBuilder interface -----------------------------

    @Override
    public BackingMapManager realizeBackingMapManager(ConfigurableCacheFactory ccf)
        {
        return getBackScheme().realizeBackingMapManager(ccf);
        }

    @Override
    public String getServiceType()
        {
        // Note: this is the default service name used the for the back scheme
        return ViewCacheService.TYPE_VIEW;
        }

    // ----- constants ------------------------------------------------------

    /**
     * A CachingScheme that represents NO_VALUE.
     */
    private static final CachingScheme NO_SCHEME = new ClassScheme();
    }
