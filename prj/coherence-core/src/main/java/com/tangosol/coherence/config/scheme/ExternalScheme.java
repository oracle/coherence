/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.util.Duration.Magnitude;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.UnitCalculatorBuilder;
import com.tangosol.coherence.config.builder.storemanager.BinaryStoreManagerBuilder;
import com.tangosol.coherence.config.builder.storemanager.BinaryStoreManagerBuilderCustomization;
import com.tangosol.coherence.config.unit.Seconds;
import com.tangosol.coherence.config.unit.Units;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.io.BinaryStore;
import com.tangosol.io.BinaryStoreManager;
import com.tangosol.io.nio.BinaryMapStore;

import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.ConfigurableCacheMap.UnitCalculator;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.SerializationCache;
import com.tangosol.net.cache.SerializationMap;
import com.tangosol.net.cache.SimpleSerializationMap;

import com.tangosol.util.Base;

import java.util.Map;

/**
 * The {@link ExternalScheme} class is responsible for building
 * a fully configured instance of a ExternalCache.
 *
 * @author pfm 2011.11.30
 * @since Coherence 12.1.2
 */
public class ExternalScheme
        extends AbstractLocalCachingScheme<Object>
        implements BinaryStoreManagerBuilderCustomization
    {
    // ----- MapBuilder interface  ------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Map realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        validate(resolver);

        Units              highUnits    = getHighUnits(resolver);
        ClassLoader        loader       = dependencies.getClassLoader();
        BinaryStoreManager storeManager = getBinaryStoreManagerBuilder().realize(resolver, loader, false);

        // auto scale units to integer range
        long cHighUnits  = highUnits.getUnitCount();
        int  nUnitFactor = getUnitFactor(resolver);
        while (cHighUnits >= Integer.MAX_VALUE)
            {
            cHighUnits  /= 1024;
            nUnitFactor *= 1024;
            }

        Map map = instantiateSerializationMap(resolver, storeManager.createBinaryStore(), dependencies.isBinary(),
                      loader, (int) cHighUnits, (int) getExpiryDelay(resolver).as(Magnitude.MILLI));

        if (map instanceof ConfigurableCacheMap)
            {
            ((ConfigurableCacheMap) map).setUnitFactor(nUnitFactor);

            // if this is a partitioned cache backing map then default to BINARY if the user
            // explicitly used a memory size in the high-units setting (e.g. 10M).
            UnitCalculator defaultCalculator = highUnits.isMemorySize() && dependencies.isBinary()
                                               ? LocalCache.INSTANCE_BINARY : null;
            UnitCalculatorBuilder bldrUnitCalculator = getUnitCalculatorBuilder();

            ((ConfigurableCacheMap) map).setUnitCalculator(bldrUnitCalculator == null
                ? defaultCalculator : bldrUnitCalculator.realize(resolver, loader, null));
            }

        return map;
        }

    // ----- BinaryStoreManagerBuilderCustomization interface ---------------

    /**
     * {@inheritDoc}
     */
    public BinaryStoreManagerBuilder getBinaryStoreManagerBuilder()
        {
        return m_bldrBinaryStoreManager;
        }

    /**
     * {@inheritDoc}
     */
    public void setBinaryStoreManagerBuilder(BinaryStoreManagerBuilder bldr)
        {
        m_bldrBinaryStoreManager = bldr;
        }

    // ----- ExternalScheme methods  ----------------------------------------

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
     * @param bldr  the UnitCalculatorBuilder
     */
    @Injectable("unit-calculator")
    public void setUnitCalculatorBuilder(UnitCalculatorBuilder bldr)
        {
        m_bldrUnitCalculator = bldr;
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

    // ----- internal  ------------------------------------------------------

    /**
     * Instantiate a SerializationMap, SerializationCache,
     * SimpleSerializationMap, or any sub-class thereof.
     *
     * @param resolver       the ParmeterResolver
     * @param store          a BinaryStore to use to write serialized data to
     * @param fBinaryMap     true if the only data written to the Map will
     *                       already be in Binary form
     * @param loader         the ClassLoader to use (if not a Binary map)
     * @param cHighUnits     the max size in units for the serialization cache
     * @param cExpiryMillis  the expiry time in milliseconds for the cache
     *
     * @return a BinaryMap, SerializationMap, SerializationCache,
     *         SimpleSerializationMap, or a subclass thereof
     */
    protected Map instantiateSerializationMap(ParameterResolver resolver, BinaryStore store, boolean fBinaryMap,
        ClassLoader loader, int cHighUnits, int cExpiryMillis)
        {
        // create the cache, which is either internal or custom
        ParameterizedBuilder<Object> bldrCustom = getCustomBuilder();

        if (bldrCustom == null)
            {
            if (cHighUnits > 0 || cExpiryMillis > 0)
                {
                SerializationCache cache = fBinaryMap
                                           ? instantiateSerializationCache(store, cHighUnits, true)
                                           : instantiateSerializationCache(store, cHighUnits, loader);

                if (cExpiryMillis > 0)
                    {
                    cache.setExpiryDelay(cExpiryMillis);
                    }

                return cache;
                }
            else if (fBinaryMap && store.getClass() == BinaryMapStore.class)
                {
                // optimization: instead of taking binary objects, writing
                // them through a serialization map that knows that they are
                // binary into a BinaryStore that wraps a BinaryMap, we just
                // use the BinaryMap directly
                return ((BinaryMapStore) store).getBinaryMap();
                }
            else
                {
                return fBinaryMap
                       ? instantiateSerializationMap(store, true) : instantiateSerializationMap(store, loader);
                }
            }
        else
            {
            ParameterList listArgs = new ResolvableParameterList();

            listArgs.add(new Parameter("store", store));

            if (cHighUnits > 0 || cExpiryMillis > 0)
                {
                // create the custom object that is extending LocalCache. First
                // populate the relevant constructor arguments then create the cache
                listArgs.add(new Parameter("high-units", cHighUnits));

                if (fBinaryMap)
                    {
                    listArgs.add(new Parameter("fBinary", fBinaryMap));
                    }
                else
                    {
                    listArgs.add(new Parameter("loader", loader));
                    }

                SerializationCache cache = (SerializationCache) bldrCustom.realize(resolver, loader, listArgs);

                if (cExpiryMillis > 0)
                    {
                    cache.setExpiryDelay(cExpiryMillis);
                    }

                return cache;
                }
            else
                {
                if (fBinaryMap)
                    {
                    listArgs.add(new Parameter("fBinary", fBinaryMap));
                    }
                else
                    {
                    listArgs.add(new Parameter("loader", loader));
                    }

                // the custom class may subclass one of the following:
                //
                // (1) SerializationMap
                // (2) SimpleSerializationMap
                //
                // the common ancestor of these classes is AbstractKeyBasedMap
                Map map = (Map) bldrCustom.realize(resolver, loader, listArgs);

                if (map instanceof SerializationMap || map instanceof SimpleSerializationMap)
                    {
                    return map;
                    }

                throw new IllegalArgumentException("Custom external cache does not extend either "
                                                   + SerializationMap.class.getName() + " or "
                                                   + SimpleSerializationMap.class.getName());
                }
            }
        }

    /**
     * Construct an SerializationCache using the specified parameters.
     * <p>
     * This method exposes a corresponding SerializationCache
     * {@link SerializationCache#SerializationCache(BinaryStore,
     * int, ClassLoader) constructor} and is provided for the express purpose
     * of allowing its override.
     *
     * @param store   the BinaryStore to use to write the serialized objects to
     * @param cMax    the maximum number of items to store in the binary store
     * @param loader  the ClassLoader to use for deserialization
     *
     * @return the instantiated {@link SerializationCache}
     */
    protected SerializationCache instantiateSerializationCache(BinaryStore store, int cMax, ClassLoader loader)
        {
        return new SerializationCache(store, cMax, loader);
        }

    /**
     * Construct an SerializationCache using the specified parameters.
     * <p>
     * This method exposes a corresponding SerializationCache
     * {@link SerializationCache#SerializationCache(BinaryStore,
     * int, boolean) constructor} and is provided for the express purpose of
     * allowing its override.
     *
     * @param store       the BinaryStore to use to write the serialized objects to
     * @param cMax        the maximum number of items to store in the binary store
     * @param fBinaryMap  true indicates that this map will only manage
     *                    binary keys and values
     *
     * @return the instantiated {@link SerializationCache}
     */
    protected SerializationCache instantiateSerializationCache(BinaryStore store, int cMax, boolean fBinaryMap)
        {
        return new SerializationCache(store, cMax, fBinaryMap);
        }

    /**
     * Construct an SerializationMap using the specified parameters.
     * <p>
     * This method exposes a corresponding SerializationMap
     * {@link SerializationMap#SerializationMap(BinaryStore,
     * ClassLoader) constructor} and is provided for the express purpose of
     * allowing its override.
     *
     * @param store   the BinaryStore to use to write the serialized objects to
     * @param loader  the ClassLoader to use for deserialization
     *
     * @return the instantiated {@link SerializationMap}
     */
    protected SerializationMap instantiateSerializationMap(BinaryStore store, ClassLoader loader)
        {
        return new SerializationMap(store, loader);
        }

    /**
     * Construct an SerializationMap using the specified parameters.
     * <p>
     * This method exposes a corresponding SerializationMap
     * {@link SerializationMap#SerializationMap(BinaryStore, boolean) constructor}
     * and is provided for the express purpose of allowing its override.
     *
     * @param store       the BinaryStore to use to write the serialized objects to
     * @param fBinaryMap  true indicates that this map will only manage
     *                    binary keys and values
     *
     * @return the instantiated {@link SerializationMap}
     */
    protected SerializationMap instantiateSerializationMap(BinaryStore store, boolean fBinaryMap)
        {
        return new SerializationMap(store, fBinaryMap);
        }

    /**
     * Construct a SimpleSerializationMap using the specified parameters.
     * <p>
     * This method exposes a corresponding SerializationMap {@link
     * SimpleSerializationMap#SimpleSerializationMap(BinaryStore, ClassLoader)
     * constructor} and is provided for the express purpose of allowing its
     * override.
     *
     * @param store   the BinaryStore to use to write the serialized objects to
     * @param loader  the ClassLoader to use for deserialization
     *
     * @return the instantiated {@link SimpleSerializationMap}
     * @since Coherence 3.7
     */
    protected SimpleSerializationMap instantiateSimpleSerializationMap(BinaryStore store, ClassLoader loader)
        {
        return new SimpleSerializationMap(store, loader);
        }

    /**
     * Construct a SimpleSerializationMap using the specified parameters.
     * <p>
     * This method exposes a corresponding SerializationMap {@link
     * SimpleSerializationMap#SimpleSerializationMap(BinaryStore, boolean)
     * constructor} and is provided for the express purpose of allowing its
     * override.
     *
     * @param store       the BinaryStore to use to write the serialized objects to
     * @param fBinaryMap  true indicates that this map will only manage
     *                    binary keys and values
     *
     * @return the instantiated {@link SimpleSerializationMap}
     * @since Coherence 3.7
     */
    protected SimpleSerializationMap instantiateSimpleSerializationMap(BinaryStore store, boolean fBinaryMap)
        {
        return new SimpleSerializationMap(store, fBinaryMap);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate(ParameterResolver resolver)
        {
        super.validate(resolver);

        Base.checkNotNull(m_bldrBinaryStoreManager, "BinaryStoreManager");
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link UnitCalculatorBuilder}.
     */
    private UnitCalculatorBuilder m_bldrUnitCalculator;

    /**
     * The StoreManager builder.
     */
    private BinaryStoreManagerBuilder m_bldrBinaryStoreManager;

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
    private Expression<Integer> m_exprUnitFactor = new LiteralExpression<Integer>(Integer.valueOf(1));
    }
