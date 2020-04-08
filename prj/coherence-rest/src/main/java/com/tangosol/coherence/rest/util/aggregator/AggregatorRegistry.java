/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.aggregator;

import com.tangosol.coherence.rest.config.AggregatorConfig;

import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.SafeHashMap;

import com.tangosol.util.aggregator.BigDecimalAverage;
import com.tangosol.util.aggregator.BigDecimalMax;
import com.tangosol.util.aggregator.BigDecimalMin;
import com.tangosol.util.aggregator.BigDecimalSum;
import com.tangosol.util.aggregator.ComparableMax;
import com.tangosol.util.aggregator.ComparableMin;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.DistinctValues;
import com.tangosol.util.aggregator.DoubleAverage;
import com.tangosol.util.aggregator.DoubleMax;
import com.tangosol.util.aggregator.DoubleMin;
import com.tangosol.util.aggregator.DoubleSum;
import com.tangosol.util.aggregator.LongMax;
import com.tangosol.util.aggregator.LongMin;
import com.tangosol.util.aggregator.LongSum;

import java.util.Collection;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A registry for {@link AggregatorFactory} instances.
 *
 * @author vp 2011.07.07
 */
public class AggregatorRegistry
    {

    //----- constructors ----------------------------------------------------

    /**
     * Construct an AggregatorRegistry.
     * <p>
     * By default the following built-in aggregators will be registered:
     * <ul>
     *   <li>big-decimal-average</li>
     *   <li>big-decimal-max</li>
     *   <li>big-decimal-min</li>
     *   <li>big-decimal-sum</li>
     *   <li>comparable-max</li>
     *   <li>comparable-min</li>
     *   <li>count</li>
     *   <li>distinct-values</li>
     *   <li>double-average</li>
     *   <li>double-max</li>
     *   <li>double-min</li>
     *   <li>double-sum</li>
     *   <li>long-max</li>
     *   <li>long-min</li>
     *   <li>long-sum</li>
     * </ul>
     */
    public AggregatorRegistry()
        {
        register("big-decimal-average", BigDecimalAverage.class);
        register("big-decimal-max",     BigDecimalMax.class);
        register("big-decimal-min",     BigDecimalMin.class);
        register("big-decimal-sum",     BigDecimalSum.class);
        register("comparable-max",      ComparableMax.class);
        register("comparable-min",      ComparableMin.class);
        register("count",               Count.class);
        register("distinct-values",     DistinctValues.class);
        register("double-average",      DoubleAverage.class);
        register("double-max",          DoubleMax.class);
        register("double-min",          DoubleMin.class);
        register("double-sum",          DoubleSum.class);
        register("long-max",            LongMax.class);
        register("long-min",            LongMin.class);
        register("long-sum",            LongSum.class);
        }

    /**
     * Construct an AggregatorRegistry that includes built-in aggregators in
     * addition to the specified aggregators.
     *
     * @param colConfig aggregator configurations
     */
    public AggregatorRegistry(Collection<AggregatorConfig> colConfig)
        {
        this();
        register(colConfig);
        }

    //----- AggregatorRegistry methods --------------------------------------

    /**
     * Returns a configured aggregator.
     *
     * @param sRequest the aggregator request
     *
     * @return aggregator
     */
    public InvocableMap.EntryAggregator getAggregator(String sRequest)
        {
        Matcher m = AGGREGATOR_REQUEST_PATTERN.matcher(sRequest);
        if (!m.matches())
            {
            throw new IllegalArgumentException("bad aggregator request syntax: "
                    + sRequest);
            }

        String sName  = m.group(1);
        String sArgs  = m.group(2);

        AggregatorFactory factory = m_mapRegistry.get(sName);
        if (factory == null)
            {
            throw new IllegalArgumentException("missing factory for aggregator: "
                    + sName);
            }

        String[] asArgs = new String[0];
        if (sArgs != null && sArgs.length() > 0)
            {
            asArgs = sArgs.split(",");
            for (int i = 0, c = asArgs.length; i < c; ++i)
                {
                asArgs[i] = asArgs[i].trim();
                }
            }

        return factory.getAggregator(asArgs);
        }

    /**
     * Register an aggregator factory with the given name.
     *
     * @param sName    the aggregator name
     * @param factory  the aggregator factory
     */
    public void register(String sName, AggregatorFactory factory)
        {
        m_mapRegistry.put(sName, factory);
        }

    /**
     * Register an aggregator or aggregator factory with the given name.
     * <p>
     * A {@link DefaultAggregatorFactory} will be used if the <code>clz</code>
     * parameter is a class that implements InvocableMap.EntryAggregator.
     *
     * @param sName  the aggregator name
     * @param clz    the aggregator or aggregator factory class
     */
    public void register(String sName, Class clz)
        {
        try
            {
            AggregatorFactory factory;
            if (AggregatorFactory.class.isAssignableFrom(clz))
                {
                factory = (AggregatorFactory) clz.newInstance();
                }
            else if (InvocableMap.EntryAggregator.class.isAssignableFrom(clz))
                {
                factory = new DefaultAggregatorFactory(clz);
                }
            else
                {
                throw new IllegalArgumentException(clz.getName()
                        + " is not an EntryAggregator nor AggregatorFactory");
                }
            register(sName, factory);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Register a collection of aggregators.
     *
     * @param colConfig  the aggregator configurations
     */
    public void register(Collection<AggregatorConfig> colConfig)
        {
        for (AggregatorConfig config : colConfig)
            {
            register(config.getAggregatorName(), config.getAggregatorClass());
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Regex pattern that defines the aggregator request syntax.
     */
    public static final String AGGREGATOR_REQUEST_REGEX
            = "\\s*(\\w(?:\\w|-)*)\\((.*)\\)";

    /**
     * Regex pattern that defines the aggregator request syntax.
     */
    private static final Pattern AGGREGATOR_REQUEST_PATTERN
            = Pattern.compile( "^" + AGGREGATOR_REQUEST_REGEX);

    // ----- data members ----------------------------------------------------

    /**
     * Registry of the aggregator factories.
     */
    private final Map<String, AggregatorFactory> m_mapRegistry = new SafeHashMap<>();
    }
