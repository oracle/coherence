/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.util.Duration.Magnitude;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.EvictionPolicyBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.UnitCalculatorBuilder;
import com.tangosol.coherence.config.unit.Seconds;
import com.tangosol.coherence.config.unit.Units;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.injection.Injector;
import com.tangosol.config.injection.SimpleInjector;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.cache.CacheLoader;
import com.tangosol.net.cache.ConfigurableCacheMap.EvictionPolicy;
import com.tangosol.net.cache.ConfigurableCacheMap.UnitCalculator;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.OldCache;

import com.tangosol.util.Base;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.ResourceResolver;
import com.tangosol.util.ResourceResolverHelper;

/**
 * The {@link LocalScheme} class is responsible for building a fully
 * configured instance of a LocalCache. Note that a LocalCache may be used as
 * a stand-alone cache or as part of a backing map.
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
 * @author pfm 2011.10.30
 * @since Coherence 12.1.2
 */
public class LocalScheme
        extends AbstractLocalCachingScheme<LocalCache>
    {
    // ----- MapBuilder interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalCache realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        validate(resolver);

        Units highUnits          = getHighUnits(resolver);
        long  cHighUnits         = highUnits.getUnitCount();
        long  cLowUnits          = getLowUnits(resolver).getUnitCount();
        int   nUnitFactor        = getUnitFactor(resolver);
        int   cExpiryDelayMillis = (int) getExpiryDelay(resolver).as(Magnitude.MILLI);

        // auto scale units to integer range
        while (cHighUnits >= Integer.MAX_VALUE)
            {
            cHighUnits  /= 1024;
            cLowUnits   /= 1024;
            nUnitFactor *= 1024;
            }

        // check and default all of the Cache options
        if (cHighUnits <= 0)
            {
            cHighUnits = Integer.MAX_VALUE;
            }

        if (cLowUnits <= 0)
            {
            cLowUnits = (long) (cHighUnits * LocalCache.DEFAULT_PRUNE);
            }

        if (cExpiryDelayMillis < 0)
            {
            cExpiryDelayMillis = 0;
            }

        // create the cache, which is either internal or custom
        LocalCache                       cache      = null;
        ClassLoader                      loader     = dependencies.getClassLoader();
        ParameterizedBuilder<LocalCache> bldrCustom = getCustomBuilder();

        if (bldrCustom == null)
            {
            // create the default internal LocalCache
            cache = new LocalCache((int) cHighUnits, cExpiryDelayMillis);
            }
        else
            {
            // create the custom object that is extending LocalCache. First
            // populate the relevant constructor arguments then create the cache
            ParameterList listArgs = new ResolvableParameterList();

            listArgs.add(new Parameter("high-units", cHighUnits));
            listArgs.add(new Parameter("expiry-delay", cExpiryDelayMillis));
            cache = bldrCustom.realize(resolver, loader, listArgs);

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
                                           ? LocalCache.INSTANCE_BINARY : null;
        UnitCalculatorBuilder bldrUnitCalculator = getUnitCalculatorBuilder();

        cache.setLowUnits((int) cLowUnits);
        cache.setUnitFactor(nUnitFactor);
        cache.setUnitCalculator(bldrUnitCalculator == null
                                ? defaultCalculator : bldrUnitCalculator.realize(resolver, loader, null));

        // Create the CacheLoader.  The cache store scheme can specify a remote
        // cache but don't allow that for LocalCache.
        CacheStoreScheme schemeCacheStore = getCacheStoreScheme();
        Object loaderObj = schemeCacheStore == null ? null : schemeCacheStore.realizeLocal(resolver, dependencies);

        if (loaderObj instanceof CacheLoader)
            {
            cache.setCacheLoader((CacheLoader) loaderObj);
            }
        else if (loaderObj != null)
            {
            throw new IllegalArgumentException("The LocalCache cache-store " + "scheme does not specify a CacheLoader");
            }

        // if the eviction policy is internal then just set the type
        // otherwise set the policy (the legacy API expects this)
        EvictionPolicyBuilder bldrPolicy = getEvictionPolicyBuilder();

        EvictionPolicy        policy     = bldrPolicy == null ? null : bldrPolicy.realize(resolver, loader, null);

        if (policy instanceof OldCache.InternalEvictionPolicy)
            {
            cache.setEvictionType(((OldCache.InternalEvictionPolicy) policy).getEvictionType());
            }
        else
            {
            cache.setEvictionPolicy(policy);
            }

        if (isPreLoad(resolver))
            {
            try
                {
                cache.loadAll();
                }
            catch (Throwable e)
                {
                String sText = "An exception occurred while pre-loading the \"" + dependencies.getCacheName()
                               + "\" cache:" + '\n' + Base.indentString(Base.getStackTrace(e), "    ");

                if (!(e instanceof Error))
                    {
                    sText += "\n(The exception has been logged and will be ignored.)";
                    }

                Logger.warn(sText);

                if (e instanceof Error)
                    {
                    throw(Error) e;
                    }
                }
            }

        return cache;
        }

    // ----- LocalScheme methods  -------------------------------------------

    /**
     * Return the {@link CacheStoreScheme} which builds a CacheStore or CacheLoader.
     *
     * @return the CacheStoreScheme
     */
    public CacheStoreScheme getCacheStoreScheme()
        {
        return m_schemeCacheStore;
        }

    /**
     * Set the {@link CacheStoreScheme} which builds a CacheStore or CacheLoader.
     *
     * @param scheme the CacheStoreScheme
     */
    @Injectable("cachestore-scheme")
    public void setCacheStoreScheme(CacheStoreScheme scheme)
        {
        m_schemeCacheStore = scheme;
        }

    /**
     * Return the EvictionPolicyBuilder used to build an EvictionPolicy.
     *
     * @return the builder
     */
    public EvictionPolicyBuilder getEvictionPolicyBuilder()
        {
        return m_bldrEvictionPolicy;
        }

    /**
     * Set the EvictionPolicyBuilder.
     *
     * @param bldr  the EvictionPolicyBuilder
     */
    @Injectable("eviction-policy")
    public void setEvictionPolicyBuilder(EvictionPolicyBuilder bldr)
        {
        m_bldrEvictionPolicy = bldr;
        }

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
     * Return the lowest number of units that a cache is pruned down to when
     * pruning takes place.  A pruning does not necessarily result in a cache
     * containing this number of units, however a pruning never results in a
     * cache containing less than this number of units. An entry is the unit
     * of measurement, unless it is overridden by an alternate unit-calculator.
     * When pruning occurs entries continue to be evicted according to the
     * eviction policy until this size. Legal values are positive integers or
     * zero. Zero implies the default. The default value is 75% of the high-units
     * setting (that is, for a high-units setting of 1000 the default low-units
     * is 750).
     *
     * @param resolver  the ParameterResolver
     *
     * @return the low units
     */
    public Units getLowUnits(ParameterResolver resolver)
        {
        return m_exprLowUnits.evaluate(resolver);
        }

    /**
     * Set the low units.
     *
     * @param expr  the low units
     */
    @Injectable
    public void setLowUnits(Expression<Units> expr)
        {
        m_exprLowUnits = expr;
        }

    /**
     * Return true if a cache pre-loads data from its CacheLoader.
     *
     * @param resolver  the ParameterResolver
     *
     * @return true if pre-load is enabled
     */
    public boolean isPreLoad(ParameterResolver resolver)
        {
        return m_exprPreload.evaluate(resolver);
        }

    /**
     * Set the pre-load enabled flag.
     *
     * @param expr  true to enable pre-load
     */
    @Injectable
    public void setPreLoad(Expression<Boolean> expr)
        {
        m_exprPreload = expr;
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
            throw new ConfigurationException("Illegal value specified for <expiry-delay> for local scheme '"
                                             + getSchemeName()
                                             + "'", "The expiry delay cannot exceed 2147483 seconds or ~24 days.");
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link CacheStoreScheme}.
     */
    private CacheStoreScheme m_schemeCacheStore;

    /**
     * The {@link UnitCalculatorBuilder}.
     */
    private UnitCalculatorBuilder m_bldrUnitCalculator;

    /**
     * The {@link EvictionPolicyBuilder}.
     */
    private EvictionPolicyBuilder m_bldrEvictionPolicy;

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
     * The low units.
     */
    private Expression<Units> m_exprLowUnits = new LiteralExpression<Units>(new Units(0));

    /**
     * The pre-load flag.
     */
    private Expression<Boolean> m_exprPreload = new LiteralExpression<Boolean>(Boolean.FALSE);

    /**
     * The unit factor.
     */
    private Expression<Integer> m_exprUnitFactor = new LiteralExpression<Integer>(Integer.valueOf(1));
    }
