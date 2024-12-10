/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderHelper;
import com.tangosol.coherence.config.xml.processor.ElementProcessorHelper;

import com.tangosol.config.expression.ChainedParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.ScopedParameterResolver;
import com.tangosol.config.injection.Injector;
import com.tangosol.config.injection.SimpleInjector;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import com.tangosol.util.Base;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.ResourceResolver;
import com.tangosol.util.ResourceResolverHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The {@link ClassScheme} class is responsible for building custom {@link CachingScheme}s and
 * custom {@link CacheStoreScheme}s.
 * <p>
 * Note: The {@link ParameterizedBuilder} interface is needed by both
 * {@link CacheStoreScheme} and {@link ElementProcessorHelper}.
 * <p>
 * This class will automatically inject the following types and
 * named values into realized classes that have been annotated with
 * &#64;Injectable.
 * <ol>
 *      <li> {@link com.tangosol.net.BackingMapManagerContext} (optionally named "manager-context")
 *      <li> {@link ConfigurableCacheFactory}
 *      <li> Cache Name (as a {@link String}.class named "cache-name")
 *      <li> Context {@link ClassLoader} (optionally named "class-loader")
 *      <li> {@link ResourceRegistry}
 *      <li> {@link CacheConfig}
 *      <li> together with any other resource, named or otherwise, available
 *           in the {@link ResourceRegistry} provided by the
 *           {@link ConfigurableCacheFactory}.
 * </ol>
 *
 * @see com.tangosol.config.annotation.Injectable
 *
 * @author pfm  2012.02.27
 * @since Coherence 12.1.2
 */
public class ClassScheme
        extends AbstractLocalCachingScheme<Object>
        implements ParameterizedBuilder<Object>, ParameterizedBuilder.ReflectionSupport
    {
    // ----- ServiceScheme interface  ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceType()
        {
        return CacheService.TYPE_LOCAL;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NamedEventInterceptorBuilder> getEventInterceptorBuilders()
        {
        return Collections.EMPTY_LIST;
        }

    // ----- ServiceBuilder interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunningClusterNeeded()
        {
        return false;
        }

    // ----- NamedCacheBuilder interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public NamedCache realizeCache(ParameterResolver resolver, Dependencies dependencies)
        {
        Base.azzert(dependencies.getConfigurableCacheFactory() instanceof ExtensibleConfigurableCacheFactory);

        // Call CFF to ensure the Service. CCF must be used to ensure the service, rather
        // than the service builder.  This is because CCFF.ensureService provides additional
        // logic like injecting a BackingMapManager into the service and starting the Service.
        Service service =
            ((ExtensibleConfigurableCacheFactory) dependencies.getConfigurableCacheFactory()).ensureService(this);

        if (!(service instanceof CacheService))
            {
            throw new IllegalArgumentException("Error: ensureCache is using service "
                + service.getInfo().getServiceName() + "that is not a CacheService ");
            }

        Parameter                param = resolver.resolve("manager-context");
        BackingMapManagerContext ctx   = (BackingMapManagerContext) param.evaluate(resolver).get();
        if (ctx == null)
            {
            // patch manager-context in resolver with manager-context that was just created above.
            ParameterResolver resolverManagerContext = sParam ->
                "manager-context".equals(sParam)
                    ? new Parameter("manager-context", ((CacheService)service).getBackingMapManager().getContext())
                    : null;
            resolver = new ChainedParameterResolver(resolverManagerContext, resolver);
            }

        // Normally, the LocalCache service creates all local NamedCache(s).  However,
        // since this is a custom NamedCache, simply instantiate the object now.  Note that
        // the service is still needed (see ensureService above) since it contains the
        // BackingMapManager, among other things.  Furthermore, this same custom class
        // will be used to create the backing map for the cache.
        NamedCache cache = (NamedCache) realize(resolver, dependencies.getClassLoader(), null);

        // prepare an injector to inject @Injectables into the realized cache
        Injector injector = new SimpleInjector();
        ResourceResolver resourceResolver =
            ResourceResolverHelper.resourceResolverFrom(ResourceResolverHelper.resourceResolverFrom(resolver,
                getDefaultParameterResolver()), ResourceResolverHelper.resourceResolverFrom(dependencies));

        return injector.inject(cache, resourceResolver);
        }

    // ----- MapBuilder interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Map realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        // prepare an injector to inject @Injectables into the realized cache
        Injector injector = new SimpleInjector();
        ResourceResolver resourceResolver =
            ResourceResolverHelper.resourceResolverFrom(ResourceResolverHelper.resourceResolverFrom(resolver,
                getDefaultParameterResolver()), ResourceResolverHelper.resourceResolverFrom(dependencies));

        return injector.inject((Map) realize(resolver, dependencies.getClassLoader(), null), resourceResolver);
        }

    // ----- ParameterizedBuilder interface ---------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Object realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        validate();

        ParameterizedBuilder<Object> bldr = getCustomBuilder();

        // NOTE: we don't attempt to inject into a raw builder here as there's
        // no real context from which to inject.  Everything that is available
        // is already being provided to the builder.

        return bldr.realize(resolver, loader, listParameters);
        }

    // ----- ParameterizedBuilder.ReflectionSupport interface ---------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean realizes(Class<?> clzClass, ParameterResolver resolver, ClassLoader loader)
        {
        validate();

        ParameterizedBuilder<Object> bldr = getCustomBuilder();

        return ParameterizedBuilderHelper.realizes(bldr, clzClass, resolver, loader);
        }

    // ----- internal -------------------------------------------------------

    /**
     * Validate the ClassScheme properties.
     */
    protected void validate()
        {
        Base.checkNotNull(getCustomBuilder(), "Custom map builder");
        }
    }
