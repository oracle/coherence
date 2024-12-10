/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.ResourceMapping;
import com.tangosol.coherence.config.TopicMapping;

import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.config.expression.Parameter;

import com.tangosol.config.injection.Injector;
import com.tangosol.config.injection.SimpleInjector;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.NamedEventInterceptor;

import com.tangosol.util.Base;
import com.tangosol.util.ChainedResourceResolver;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.ResourceResolver;

import java.util.List;

import static com.tangosol.util.ResourceResolverHelper.resourceResolverFrom;

/**
 * InterceptorManager is responsible for creating {@link EventInterceptor}
 * and registering them with the {@link InterceptorRegistry}.
 *
 * @author bb 2015.01.15
 * @since 12.2.1
 */
public class InterceptorManager
    {

    /**
     * Construct InterceptorManager.
     *
     * @param cacheConfig  Cache config that has the interceptors configuration
     * @param loader       Class loader to use when instantiating the event interceptors
     * @param registry     InterceptorRegistry where the interceptors will be registered
     */
    public InterceptorManager(CacheConfig cacheConfig, ClassLoader loader,
                              ResourceRegistry registry)
        {
        Base.azzert(cacheConfig != null);

        f_cacheConfig = cacheConfig;
        f_loader      = loader;
        f_registry    = registry;
        }

    /**
     * Register global event interceptors.
     */
    public void instantiateGlobalInterceptors()
        {
        registerEventInterceptors(f_cacheConfig.getEventInterceptorBuilders(),
                new ResolvableParameterList());
        }

    /**
     * Register Service event interceptors.
     *
     * @param sServiceName  Name of the service for which the event interceptors
     *                      needs to be registered.
     */
    public void instantiateServiceInterceptors(String sServiceName)
        {
        CacheConfig config = f_cacheConfig;
        for (ServiceScheme scheme : config.getServiceSchemeRegistry())
            {
            if (scheme.getScopedServiceName().equals(sServiceName))
                {
                // grab the interceptor builders so we can build the interceptors
                List<NamedEventInterceptorBuilder> listBuilders = scheme.getEventInterceptorBuilders();

                ResolvableParameterList parameters = new ResolvableParameterList();
                parameters.add(new Parameter("service-name", sServiceName));

                registerEventInterceptors(listBuilders, parameters);
                break;
                }
            }
        }

    /**
     * Register Cache event interceptors.
     *
     * @param sCacheName    Name of the cache.
     * @param sServiceName  Name of the service to which the cache belongs.
     */
    public void instantiateCacheInterceptors(String sCacheName, String sServiceName)
        {
        ResourceMapping mapping = f_cacheConfig.getMappingRegistry().findMapping(sCacheName, CacheMapping.class);

        if (mapping != null)
            {
            // grab the interceptor builders so we can build the interceptors
            List<NamedEventInterceptorBuilder> listBuilders = mapping.getEventInterceptorBuilders();

            // create a parameter resolver containing the cache pattern and service name
            // so that the builder can use it if required
            ResolvableParameterList parameters = new ResolvableParameterList();
            parameters.add(new Parameter("service-name",sServiceName));
            parameters.add(new Parameter("cache-name", sCacheName));

            registerEventInterceptors(listBuilders, parameters);
            }
        }

    /**
     * Register Destination event interceptors.
     *
     * @param sDestinationName  Name of the destination.
     * @param sServiceName      Name of the service to which the destination belongs.
     */
    public void instantiateDestinationInterceptors(String sDestinationName, String sServiceName)
        {
        ResourceMapping mapping = f_cacheConfig.getMappingRegistry().findMapping(sDestinationName, TopicMapping.class);

        if (mapping != null)
            {
            // grab the interceptor builders so we can build the interceptors
            List<NamedEventInterceptorBuilder> listBuilders = mapping.getEventInterceptorBuilders();

            // create a parameter resolver containing the name pattern and service name
            // so that the builder can use it if required
            ResolvableParameterList parameters = new ResolvableParameterList();
            parameters.add(new Parameter("service-name",sServiceName));
            parameters.add(new Parameter("topic-name", sDestinationName));

            registerEventInterceptors(listBuilders, parameters);
            }
        }

    /**
     * Register the {@link NamedEventInterceptor}s generated by the provided
     * builders with the {@link InterceptorRegistry}.
     *
     * @param listBuilders  the builders that generate NamedEventInterceptors
     *                      to register
     * @param parameters    the parameters that can be consumed by the EventInterceptors
     */
    public void registerEventInterceptors(List<NamedEventInterceptorBuilder> listBuilders,
                                             ResolvableParameterList parameters)
        {
        CacheConfig      config   = f_cacheConfig;
        ClassLoader      loader   = f_loader;
        ResourceRegistry registry = f_registry;

        if (listBuilders != null && !listBuilders.isEmpty())
            {
            // prepare an injector to inject values into the interceptor
            Injector injector = new SimpleInjector();
            ResourceResolver resourceResolver =
                    new ChainedResourceResolver(resourceResolverFrom(parameters,
                            config.getDefaultParameterResolver()), resourceResolverFrom(ResourceRegistry.class,
                            registry), registry);

            InterceptorRegistry registryIntcptr = registry.getResource(InterceptorRegistry.class);

            // realize all of the declared EventInterceptors for the service name
            for (NamedEventInterceptorBuilder bldr : listBuilders)
                {
                NamedEventInterceptor interceptor = bldr.realize(parameters, loader, null);

                // inject values into the wrapped EventInterceptor
                injector.inject(interceptor.getInterceptor(), resourceResolver);

                registryIntcptr.registerEventInterceptor(interceptor);
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * CacheConfig that has the EventInterceptors configuration.
     */
    protected final CacheConfig f_cacheConfig;

    /**
     * ClassLoader to be used for instantiating the EventInterceptors.
     */
    protected final ClassLoader f_loader;

    /**
     * ResourceRegistry to be used to register the EventInterceptors.
     */
    protected final ResourceRegistry f_registry;
    }
