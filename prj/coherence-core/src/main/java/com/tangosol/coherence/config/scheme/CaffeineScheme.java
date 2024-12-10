/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.caffeine.CaffeineCache;

import com.oracle.coherence.common.util.Duration.Magnitude;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.UnitCalculatorBuilder;
import com.tangosol.coherence.config.unit.Seconds;
import com.tangosol.coherence.config.unit.Units;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.injection.Injector;
import com.tangosol.config.injection.SimpleInjector;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.cache.ConfigurableCacheMap.UnitCalculator;
import com.tangosol.net.cache.OldCache;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.ResourceResolver;
import com.tangosol.util.ResourceResolverHelper;

/**
 * The {@link CaffeineScheme} class is responsible for building a fully
 * configured instance of a {@link CaffeineCache}. Note that a CaffeineCache
 * may be used as a stand-alone cache or as part of a backing map.
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
 * @see Injectable
 *
 * @author Aleks Seovic  2022.05.12
 * @since 22.06
 */
public class CaffeineScheme
        extends AbstractLocalCachingScheme<CaffeineCache>
    {
    // ----- MapBuilder interface -------------------------------------------

    @Override
    public CaffeineCache realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        validate(resolver);

        Units highUnits          = getHighUnits(resolver);
        long  cHighUnits         = highUnits.getUnitCount();
        int   nUnitFactor        = getUnitFactor(resolver);
        int   cExpiryDelayMillis = (int) getExpiryDelay(resolver).as(Magnitude.MILLI);

        // auto scale units to integer range
        while (cHighUnits >= Integer.MAX_VALUE)
            {
            cHighUnits  /= 1024;
            nUnitFactor *= 1024;
            }

        // check and default all the Cache options
        if (cHighUnits <= 0)
            {
            cHighUnits = Integer.MAX_VALUE;
            }

        if (cExpiryDelayMillis < 0)
            {
            cExpiryDelayMillis = 0;
            }

        // create the cache, which is either internal or custom
        CaffeineCache                       cache      = null;
        ClassLoader                         loader     = dependencies.getClassLoader();
        ParameterizedBuilder<CaffeineCache> bldrCustom = getCustomBuilder();

        if (bldrCustom == null)
            {
            // create the default internal CaffeineCache
            cache = new CaffeineCache();
            cache.setHighUnits((int) cHighUnits);
            cache.setExpiryDelay(cExpiryDelayMillis);
            }
        else
            {
            // create the custom object that is extending CaffeineCache
            cache = bldrCustom.realize(resolver, loader, null);
            cache.setHighUnits((int) cHighUnits);
            cache.setExpiryDelay(cExpiryDelayMillis);

            // prepare an injector to inject values into the cache
            Injector injector = new SimpleInjector();
            ResourceResolver resourceResolver =
                ResourceResolverHelper.resourceResolverFrom(ResourceResolverHelper.resourceResolverFrom(resolver,
                    getDefaultParameterResolver()), ResourceResolverHelper.resourceResolverFrom(dependencies));

            injector.inject(cache, resourceResolver);
            }

        // we can only be called by the ECCF, at which point the Cluster
        // object has already been created
        if (CacheFactory.getCluster().isRunning())
            {
            cache.setOptimizeGetTime(true);
            }

        // if this is a partitioned cache backing map then default to BINARY if the user
        // explicitly used a memory size in the high-units setting (e.g. 10M).
        UnitCalculator defaultCalculator = highUnits.isMemorySize() && dependencies.isBinary()
                                           ? OldCache.INSTANCE_BINARY : null;
        UnitCalculatorBuilder bldrUnitCalculator = getUnitCalculatorBuilder();

        cache.setUnitFactor(nUnitFactor);
        cache.setUnitCalculator(bldrUnitCalculator == null
                                ? defaultCalculator : bldrUnitCalculator.realize(resolver, loader, null));

        return cache;
        }

    // ----- CaffeineScheme methods  ----------------------------------------

    /**
     * Return the amount of time since the last update that entries
     * are kept by the cache before being expired. Entries that have expired
     * are not accessible and are evicted the next time a client accesses the
     * cache. Any attempt to read an expired entry results in a reloading of
     * the entry from the CacheStore.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the expiry delay
     */
    public Seconds getExpiryDelay(ParameterResolver resolver)
        {
        return m_exprExpiryDelay.evaluate(resolver);
        }

    /**
     * Set the expiry delay.
     *
     * @param expr  the expiry delay expression
     */
    @Injectable
    public void setExpiryDelay(Expression<Seconds> expr)
        {
        m_exprExpiryDelay = expr;
        }

    /**
     * Return the limit of cache size. Contains the maximum number of units
     * that can be placed  n the cache before pruning occurs. An entry is the
     * unit of measurement, unless it is overridden by an alternate unit-calculator.
     * When this limit is exceeded, the cache begins the pruning process,
     * evicting entries according to the eviction policy.  Legal values are
     * positive integers or zero. Zero implies no limit.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the high units
     */
    public Units getHighUnits(ParameterResolver resolver)
        {
        return m_exprHighUnits.evaluate(resolver);
        }

    /**
     * Set the high units.
     *
     * @param expr  the high units expression
     */
    @Injectable
    public void setHighUnits(Expression<Units> expr)
        {
        m_exprHighUnits = expr;
        }

    /**
     * Return the UnitCalculatorBuilder used to build a UnitCalculator.
     *
     * @return the unit calculator
     */
    public UnitCalculatorBuilder getUnitCalculatorBuilder()
        {
        return m_bldrUnitCalculator;
        }

    /**
     * Set the UnitCalculatorBuilder.
     *
     * @param builder  the UnitCalculatorBuilder
     */
    @Injectable("unit-calculator")
    public void setUnitCalculatorBuilder(UnitCalculatorBuilder builder)
        {
        m_bldrUnitCalculator = builder;
        }

    /**
     * Return the unit-factor element specifies the factor by which the units,
     * low-units and high-units properties are adjusted. Using a BINARY unit
     * calculator, for example, the factor of 1048576 could be used to count
     * megabytes instead of bytes.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the unit factor
     */
    public int getUnitFactor(ParameterResolver resolver)
        {
        return m_exprUnitFactor.evaluate(resolver);
        }

    /**
     * Set the unit factor.
     *
     * @param expr  the unit factor expression
     */
    @Injectable
    public void setUnitFactor(Expression<Integer> expr)
        {
        m_exprUnitFactor = expr;
        }

    // ----- internal -------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate(ParameterResolver resolver)
        {
        super.validate(resolver);

        if (getExpiryDelay(resolver).as(Magnitude.MILLI) > Integer.MAX_VALUE)
            {
            throw new ConfigurationException("Illegal value specified for <expiry-delay> for caffeine scheme '"
                                             + getSchemeName()
                                             + "'", "The expiry delay cannot exceed 2147483 seconds or ~24 days.");
            }
        }

    // ----- data members ---------------------------------------------------
    /**
     * The {@link UnitCalculatorBuilder}.
     */
    private UnitCalculatorBuilder m_bldrUnitCalculator;

    /**
     * The duration that a value will live in the cache.
     * Zero indicates no timeout.
     */
    private Expression<Seconds> m_exprExpiryDelay = new LiteralExpression<Seconds>(new Seconds(0));

    /**
     * The high units.
     */
    private Expression<Units> m_exprHighUnits = new LiteralExpression<Units>(new Units(0));

    /**
     * The unit factor.
     */
    private Expression<Integer> m_exprUnitFactor = new LiteralExpression<Integer>(1);
    }
