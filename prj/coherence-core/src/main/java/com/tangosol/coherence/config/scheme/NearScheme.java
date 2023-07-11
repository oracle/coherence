/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.NamedCacheBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ServiceBuilder;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.NearCache;

import java.util.Map;

/**
 * The {@link NearScheme} is used to realize (create) an instance of a NearCache.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public class NearScheme
        extends AbstractCompositeScheme<NearCache>
        implements NamedCacheBuilder
    {
    // ----- ServiceScheme interface ----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilder getServiceBuilder()
        {
        return getBackScheme().getServiceBuilder();
        }

    @Override
    public String getServiceType()
        {
        return TYPE_NEAR;
        }

    // ----- BackingMapManagerBuilder interface -----------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public BackingMapManager realizeBackingMapManager(ConfigurableCacheFactory ccf)
        {
        return getBackScheme().realizeBackingMapManager(ccf);
        }

    // ----- NamedCacheBuilder interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public NamedCache realizeCache(ParameterResolver resolver, Dependencies dependencies)
        {
        validate(resolver);

        String sStrategy = getInvalidationStrategy(resolver);

        int nStrategy = sStrategy.equalsIgnoreCase("none")
                        ? NearCache.LISTEN_NONE
                        : sStrategy.equalsIgnoreCase("present")
                          ? NearCache.LISTEN_PRESENT
                          : sStrategy.equalsIgnoreCase("all")
                            ? NearCache.LISTEN_ALL
                            : sStrategy.equalsIgnoreCase("auto")
                              ? NearCache.LISTEN_AUTO
                              : sStrategy.equalsIgnoreCase("logical")
                                ? NearCache.LISTEN_LOGICAL
                                : Integer.MIN_VALUE;

        if (nStrategy == Integer.MIN_VALUE)
            {
           Logger.warn("Invalid invalidation strategy of '" + sStrategy + "'; proceeding with default of 'auto'");
            nStrategy = NearCache.LISTEN_AUTO;
            }

        // create the front map
        Dependencies depFront = new Dependencies(dependencies.getConfigurableCacheFactory(), null,
                                    dependencies.getClassLoader(), dependencies.getCacheName(),
                                    CacheService.TYPE_LOCAL);

        Map mapFront = getFrontScheme().realizeMap(resolver, depFront);

        // create the back cache
        CachingScheme schemeBack = getBackScheme();
        NamedCache    cacheBack  = schemeBack.realizeCache(resolver, dependencies);

        // create the near cache
        NearCache                       cacheNear;
        ParameterizedBuilder<NearCache> bldrCustom = getCustomBuilder();

        if (bldrCustom == null)
            {
            // create the default internal NearCache
            cacheNear = new NearCache(mapFront, cacheBack, nStrategy);
            }
        else
            {
            // create the custom object that is extending NearCache. First
            // populate the relevant constructor arguments then create the cache
            ParameterList listArgs = new ResolvableParameterList();

            listArgs.add(new Parameter("front-map", mapFront));
            listArgs.add(new Parameter("back-cache", cacheBack));
            listArgs.add(new Parameter("strategy", nStrategy));
            cacheNear = bldrCustom.realize(resolver, dependencies.getClassLoader(), listArgs);
            }

        cacheNear.setRegistrationContext("tier=front,loader=" + dependencies.getClassLoader().hashCode());

        return cacheNear;
        }

    // ----- ObservableCachingScheme interface ------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void establishMapListeners(Map map, ParameterResolver resolver, Dependencies dependencies)
        {
        // establish the map listener for the cache
        super.establishMapListeners(map, resolver, dependencies);

        // establish the map listener for the front map if it's observable
        if (getFrontScheme() instanceof ObservableCachingScheme && map instanceof NearCache)
            {
            ((ObservableCachingScheme) getFrontScheme()).establishMapListeners(((NearCache) map).getFrontMap(),
                resolver, dependencies);
            }
        }

    // ----- NearScheme methods ---------------------------------------------

    /**
     * Return the invalidation strategy.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the invalidation strategy
     */
    public String getInvalidationStrategy(ParameterResolver resolver)
        {
        return m_exprInvalidationStrategy.evaluate(resolver);
        }

    /**
     * Set the invalidation strategy.
     *
     * @param expr  the invalidation strategy
     */
    @Injectable
    public void setInvalidationStrategy(Expression<String> expr)
        {
        m_exprInvalidationStrategy = expr;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Service type for near caches.
     *
     * @since 12.2.1.4.19
     */
    protected static final String TYPE_NEAR = "NearCache";

    // ----- data members ---------------------------------------------------

    /**
     * The invalidation strategy.
     */
    private Expression<String> m_exprInvalidationStrategy = new LiteralExpression<String>(String.valueOf("auto"));
    }
