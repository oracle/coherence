/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.metrics;

import com.oracle.coherence.common.collections.ConcurrentHashMap;

import com.tangosol.net.metrics.MBeanMetric;
import com.tangosol.net.metrics.MetricsRegistryAdapter;

import java.util.Comparator;
import java.util.Map;

import java.util.regex.Pattern;

import java.util.stream.Stream;

/**
 * A registry of {@link MBeanMetric} instances.
 * <p>
 * There is a single instance of the {@link DefaultMetricRegistry} that
 * can be obtained using the {@link DefaultMetricRegistry#getRegistry()}
 * method.
 *
 * @author jk  2019.06.19
 * @since 12.2.1.4
 */
public class DefaultMetricRegistry
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Protected constructor.
     * <p>
     * There is only a singleton public instance.
     */
    DefaultMetricRegistry()
        {
        }

    // ----- CoherenceMetricRegistry methods --------------------------------

    /**
     * Obtain the Coherence metrics registry.
     *
     * @return  the Coherence metrics registry
     */
    public static DefaultMetricRegistry getRegistry()
        {
        return INSTANCE;
        }

    /**
     * Register a metric.
     *
     * @param metric  the {@link MBeanMetric} to register
     */
    public void register(MBeanMetric metric)
        {
        f_mapMetric.put(metric.getIdentifier(), metric);
        }

    /**
     * Remove a previously registered metric.
     *
     * @param identifier  the metric to remove
     */
    public void remove(MBeanMetric.Identifier identifier)
        {
        f_mapMetric.remove(identifier);
        }

    /**
     * Obtain a previously registered metric.
     *
     * @param identifier the identifier of the metric
     *
     * @return  the previously registered metric or {@code null} if no metric
     *          was registered with the identifier
     */
    public MBeanMetric getMetric(MBeanMetric.Identifier identifier)
        {
        return f_mapMetric.get(identifier);
        }

    /**
     * Obtain the previously registered metrics with the specified name.
     *
     * @param sName the name of the metric
     *
     * @return  the previously registered metrics with the specified name
     */
    public Stream<Map.Entry<MBeanMetric.Identifier, MBeanMetric>> getMetrics(String sName)
        {
        return f_mapMetric
                .entrySet()
                .stream()
                .filter(e -> e.getKey().getName().equals(sName));
        }

    /**
     * Obtain the previously registered metrics with a name matching the specified pattern.
     *
     * @param pattern the name of the metric
     *
     * @return  the previously registered metrics with a name matching the specified pattern
     */
    public Stream<Map.Entry<MBeanMetric.Identifier, MBeanMetric>> getMetrics(Pattern pattern)
        {
        return f_mapMetric
                .entrySet()
                .stream()
                .filter(e -> pattern.matcher( e.getKey().getName()).matches());
        }

    /**
     * Returns a {@link Stream} of all of the registered metrics.
     *
     * @return a {@link Stream} of all of the registered metrics
     */
    public Stream<Map.Entry<MBeanMetric.Identifier, MBeanMetric>> stream()
        {
        return f_mapMetric.entrySet()
                .stream()
                .sorted(METRIC_COMPARATOR);
        }

    // ----- inner class Wrapper --------------------------------------------

    /**
     * A wrapper to produce a {@link MetricsRegistryAdapter} that
     * wraps the singleton {@link DefaultMetricRegistry}.
     */
    public static class Adapter
            implements MetricsRegistryAdapter
        {
        @Override
        public void register(MBeanMetric metric)
            {
            DefaultMetricRegistry.getRegistry().register(metric);
            }

        @Override
        public void remove(MBeanMetric.Identifier identifier)
            {
            DefaultMetricRegistry.getRegistry().remove(identifier);
            }
        }

    /**
     * A {@link Comparator} for sorting {@link Map.Entry} instances where the key
     * is an instance of a {@link MBeanMetric.Identifier}.
     */
    public static class MetricComparator implements Comparator<Map.Entry<MBeanMetric.Identifier, MBeanMetric>>
        {
        @Override
        public int compare(Map.Entry<MBeanMetric.Identifier, MBeanMetric> o1,
                           Map.Entry<MBeanMetric.Identifier, MBeanMetric> o2)
            {
            return o1.getKey().compareTo(o2.getKey());
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The singleton instance of {@link DefaultMetricRegistry}.
     */
    private static final DefaultMetricRegistry INSTANCE = new DefaultMetricRegistry();

    /**
     * A singleton instance of a MetricComparator.
     */
    private static final MetricComparator METRIC_COMPARATOR = new MetricComparator();

    // ----- data members ---------------------------------------------------

    /**
     * The map of registered metrics.
     */
    private Map<MBeanMetric.Identifier, MBeanMetric> f_mapMetric = new ConcurrentHashMap<>();
    }
