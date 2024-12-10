/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.injection.Injector;
import com.tangosol.config.injection.SimpleInjector;

import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import com.tangosol.util.Base;
import com.tangosol.util.ChainedResourceResolver;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.ResourceResolver;
import com.tangosol.util.ResourceResolverHelper;

import static com.tangosol.util.ResourceResolverHelper.resourceResolverFrom;

import java.util.Map;

/**
 * A {@link CustomScheme} is an adapter for a {@link ParameterizedBuilder} that
 * builds a {@link Map}.
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
 * @author pfm  2011.12.06
 * @since Coherence 12.1.2
 */
public class CustomScheme
        extends AbstractLocalCachingScheme<Map>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link CustomScheme}.
     *
     * @param bldr  the InstanceBuilder to wrap
     */
    public CustomScheme(ParameterizedBuilder<Map> bldr)
        {
        setCustomBuilder(bldr);
        }

    // ----- ServiceBuilder interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Service realizeService(ParameterResolver resolver, ClassLoader loader, Cluster cluster)
        {
        throw new IllegalStateException("Custom scheme does not support services");
        }

    // ----- NamedCacheBuilder interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public NamedCache realizeCache(ParameterResolver resolver, Dependencies dependencies)
        {
        throw new IllegalStateException("Custom scheme does not support caches");
        }

    // ----- MapBuilder methods ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Map realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        validate();

        ParameterizedBuilder<Map> bldr = getCustomBuilder();

        Map                       map  = bldr.realize(resolver, dependencies.getClassLoader(), null);

        // prepare an injector to inject values into the map
        Injector injector = new SimpleInjector();
        ResourceResolver resourceResolver =
            ResourceResolverHelper.resourceResolverFrom(ResourceResolverHelper.resourceResolverFrom(resolver,
                getDefaultParameterResolver()), ResourceResolverHelper.resourceResolverFrom(dependencies));

        return injector.inject(map, resourceResolver);
        }

    // ----- internal -------------------------------------------------------

    /**
     * Validate the builder properties.
     */
    protected void validate()
        {
        Base.checkNotNull(getCustomBuilder(), "Custom map builder");
        }
    }
