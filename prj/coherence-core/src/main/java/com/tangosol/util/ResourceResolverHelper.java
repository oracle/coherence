/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.coherence.config.builder.MapBuilder;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.Value;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.ConfigurableCacheFactory;

/**
 * Provides helpful methods for working with {@link ResourceResolver}s.
 *
 * @see ResourceResolver
 *
 * @author bo 2012.11.13
 * @since Coherence 12.1.2
 */
public class ResourceResolverHelper
    {
    /**
     * Constructs and configures a {@link ResourceResolver} from
     * a {@link MapBuilder} dependencies.
     * <p>
     * When a {@link ConfigurableCacheFactory} is available in the dependencies,
     * the {@link ResourceRegistry} is returns as the second part of a
     * {@link ChainedResourceResolver}.
     *
     * @param dependencies  the {@link MapBuilder} dependencies
     *
     * @return a {@link ResourceResolver}
     */
    public static ResourceResolver resourceResolverFrom(MapBuilder.Dependencies dependencies)
        {
        SimpleResourceResolver resolver = new SimpleResourceResolver();

        if (dependencies == null)
            {
            return resolver;
            }
        else
            {
            String sCacheName = dependencies.getCacheName();

            if (sCacheName != null)
                {
                resolver.registerResource(String.class, "cache-name", sCacheName);
                }

            ClassLoader classLoader = dependencies.getClassLoader();

            if (dependencies.getClassLoader() != null)
                {
                resolver.registerResource(ClassLoader.class, classLoader);
                resolver.registerResource(ClassLoader.class, "class-loader", classLoader);
                }

            BackingMapManagerContext ctxBackingMapManager = dependencies.getBackingMapManagerContext();

            if (ctxBackingMapManager != null)
                {
                resolver.registerResource(BackingMapManagerContext.class, ctxBackingMapManager);
                resolver.registerResource(BackingMapManagerContext.class, "manager-context", ctxBackingMapManager);
                }

            ConfigurableCacheFactory ccf = dependencies.getConfigurableCacheFactory();

            if (ccf == null)
                {
                return resolver;
                }
            else
                {
                ResourceRegistry registry = ccf.getResourceRegistry();

                resolver.registerResource(ConfigurableCacheFactory.class, ccf);
                resolver.registerResource(ResourceRegistry.class, registry);

                return new ChainedResourceResolver(resolver, registry);
                }
            }
        }

    /**
     * Constructs a {@link ResourceResolver} based on a {@link ParameterResolver}.
     *
     * @param parameterResolver                the {@link ParameterResolver} to
     *                                         adapt into a {@link ResourceResolver}
     * @param resolverForExpressionEvaluation  the {@link ParameterResolver} to
     *                                         use for evaluating {@link Parameter}s
     * @return a {@link ResourceResolver}
     */
    public static ResourceResolver resourceResolverFrom(final ParameterResolver parameterResolver,
        final ParameterResolver resolverForExpressionEvaluation)
        {
        return new ResourceResolver()
            {
            /**
             * {@inheritDoc}
             */
            @Override
            public <R> R getResource(Class<R> clsResource, String sResourceName)
                {
                // attempt to resolve the parameter using the resource name
                Parameter parameter = parameterResolver.resolve(sResourceName);

                if (parameter == null)
                    {
                    // no such parameter, so we can't resolve the resource
                    return null;
                    }
                else
                    {
                    ParameterResolver resolver = resolverForExpressionEvaluation == null
                                                 ? new NullParameterResolver() : resolverForExpressionEvaluation;

                    // attempt to evaluate the parameter
                    Value value = parameter.evaluate(resolver);

                    if (value == null || value.isNull() || !value.supports(clsResource))
                        {
                        return null;
                        }
                    else
                        {
                        return value.as(clsResource);
                        }
                    }
                }

            /**
             * {@inheritDoc}
             */
            @Override
            public <R> R getResource(Class<R> clsResource)
                {
                // not we can only support resolving named parameters
                return null;
                }
            };
        }

    /**
     * Constructs a {@link ResourceResolver} based on a sequence of specified
     * {@link ResourceResolver}s.
     *
     * @param resourceResolvers  the {@link ResourceResolver}s
     *
     * @return a {@link ResourceResolver}
     */
    public static ResourceResolver resourceResolverFrom(ResourceResolver... resourceResolvers)
        {
        return new ChainedResourceResolver(resourceResolvers);
        }

    /**
     * Constructs a {@link ResourceResolver} for a single named resource.
     *
     * @param clsResolvableResource    the {@link Class} of the resource
     * @param sResolvableResourceName  the name of the resource
     * @param resolveableResource      the resource
     *
     * @return a {@link ResourceResolver} for the specified resource
     */
    public static <T> ResourceResolver resourceResolverFrom(final Class<T> clsResolvableResource,
        final String sResolvableResourceName, final T resolveableResource)
        {
        return new ResourceResolver()
            {
            /**
             * {@inheritDoc}
             */
            @Override
            public <R> R getResource(Class<R> clsResource, String sResourceName)
                {
                if (clsResource.equals(clsResolvableResource) && sResourceName.equals(sResolvableResourceName))
                    {
                    return (R) resolveableResource;
                    }
                else
                    {
                    return null;
                    }
                }

            /**
             * {@inheritDoc}
             */
            @Override
            public <R> R getResource(Class<R> clsResource)
                {
                // this resource resolver only operates for the specifically
                // named resource
                return null;
                }
            };
        }

    /**
     * Constructs a {@link ResourceResolver} for a single resource.
     *
     * @param clsResolvableResource    the {@link Class} of the resource
     * @param resolveableResource      the resource
     *
     * @return a {@link ResourceResolver} for the specified resource
     */
    public static <T> ResourceResolver resourceResolverFrom(final Class<T> clsResolvableResource,
        final T resolveableResource)
        {
        return new ResourceResolver()
            {
            /**
             * {@inheritDoc}
             */
            @Override
            public <R> R getResource(Class<R> clsResource, String sResourceName)
                {
                // this resource resolver only operates for the specifically
                // resource
                return null;
                }

            /**
             * {@inheritDoc}
             */
            @Override
            public <R> R getResource(Class<R> clsResource)
                {
                if (clsResource.equals(clsResolvableResource))
                    {
                    return (R) resolveableResource;
                    }
                else
                    {
                    return null;
                    }
                }
            };
        }
    }
