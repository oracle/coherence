/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.service.DefaultViewDependencies;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;

import com.tangosol.net.Service;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.net.internal.ScopedServiceReferenceStore;
import com.tangosol.net.internal.ViewCacheService;

import com.tangosol.util.Base;

import java.lang.reflect.Method;

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
// (we use the cluster's ScopedServiceReferenceStore to store these services).
@SuppressWarnings("rawtypes")
public class ViewScheme
        extends AbstractCompositeScheme<ContinuousQueryCache>
    {

    // Internal Notes:
    // Static initialization to obtain and store a reference
    // to SafeCluster.getScopedServiceStore().  This store will be used to
    // store ViewCacheService instances.
    //
    // Originally, these instances were stored in the Cluster's resource
    // registry, however, the registry contents won't survive a cluster
    // stop/start (like a member eviction/restart).
    // The ScopedServiceReferenceStore will survive in this scenario
    // allowing the same ViewCacheService instance to be used and thus
    // avoiding a potential memory leak.
    static
        {
        Class<?> clzSafeCluster;
        try
            {
            clzSafeCluster = Class.forName("com.tangosol.coherence.component.util.SafeCluster",
                                           false,
                                           ViewScheme.class.getClassLoader());
            SAFE_CLUSTER_GET_SCOPED_SERVICE_STORE =
                    clzSafeCluster.getDeclaredMethod("getScopedServiceStore");

            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "ViewScheme initialization failed");
            }
        }

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

    // ----- AbstractServiceScheme methods ----------------------------------

    @Override
    public String getScopedServiceName()
        {
        return ViewCacheService.KEY_CLUSTER_REGISTRY + '-' + super.getScopedServiceName();
        }

    // ----- ServiceBuilder interface ---------------------------------------

    @Override
    public Service realizeService(ParameterResolver resolver, ClassLoader loader, Cluster cluster)
        {
        validate();

        // see if we've already created the appropriate CacheService
        ScopedServiceReferenceStore store        = getServiceStore(cluster);
        String                      sServiceName = getScopedServiceName();
        Service                     service      = store.getService(sServiceName);
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

        store.putService(service, sServiceName, getServiceType());

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

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain the {@link ScopedServiceReferenceStore} from the provided
     * {@link Cluster}.
     *
     * @param cluster  the {@link Cluster}
     *
     * @return the {@link ScopedServiceReferenceStore} from the provided
     *         {@link Cluster}.
     *
     * @since 12.2.1.4.19
     */
    protected ScopedServiceReferenceStore getServiceStore(Cluster cluster)
        {
        try
            {
            return (ScopedServiceReferenceStore) SAFE_CLUSTER_GET_SCOPED_SERVICE_STORE.invoke(cluster);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Failed to invoke SafeCluster.getScopedReferenceStore()");
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * A CachingScheme that represents NO_VALUE.
     */
    private static final CachingScheme NO_SCHEME = new ClassScheme();

    /**
     * Method reference to obtain SafeCluster ScopedServiceReferenceStore.
     *
     * @since 12.2.1.4.19
     */
    private static final Method SAFE_CLUSTER_GET_SCOPED_SERVICE_STORE;
    }
