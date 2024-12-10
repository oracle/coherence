/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.micrometer;

import com.oracle.coherence.common.collections.WeakIdentityHashMap;

import com.tangosol.coherence.config.Config;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.metrics.MBeanMetric;
import com.tangosol.net.metrics.MetricsRegistryAdapter;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import io.micrometer.core.instrument.binder.MeterBinder;

import io.micrometer.core.lang.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import java.util.stream.Collectors;

/**
 * An adapter between Micrometer metrics and Coherence metrics.
 *
 * @author Jonathan Knight  2020.10.08
 */
public class CoherenceMicrometerMetrics
        implements MeterBinder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Private constructor as this is a singleton.
     */
    private CoherenceMicrometerMetrics()
        {
        if (Config.getBoolean(PROP_USE_GLOBAL_REGISTRY, false))
            {
            bindTo(Metrics.globalRegistry);
            }
        }

    // ----- MeterBinder methods -------------------------------------------------

    @Override
    public void bindTo(@NonNull MeterRegistry registry)
        {
        f_mapRegistry.put(registry, 0);
        Set<MeterRegistry> set = Collections.singleton(registry);
        f_mapMetric.values().forEach(m -> register(set, m));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Register a metric with all of the bound Micrometer registries.
     *
     * @param metric  the metric to register
     */
    public void register(MBeanMetric metric)
        {
        Holder holder = f_mapMetric.compute(metric.getIdentifier(),
                                            (k, v) -> v == null ? createHolder(metric) : v.setMetric(metric));

        register(f_mapRegistry.keySet(), holder);
        }

    /**
     * Create a metric {@link Holder}.
     *
     * @param metric the metric to put in the {@link Holder}
     *
     * @return  a metric {@link Holder} containing the specified metric
     */
    Holder createHolder(MBeanMetric metric)
        {
        MBeanMetric.Identifier identifier = metric.getIdentifier();
        String                 sName      = identifier.getFormattedName();
        Tags                   tags       = getTags(identifier);

        return new Holder(metric, sName, tags);
        }

    /**
     * Register a metric with a set of Micrometer registries.
     *
     * @param setRegistry  the set of {@link MeterRegistry} to register the metric with
     * @param holder       the holder containing the metric to register
     */
    void register(Set<MeterRegistry> setRegistry, Holder holder)
        {
        MBeanMetric.Identifier identifier = holder.getIdentifier();
        String                 sName      = holder.getName();
        Tags                   tags       = holder.getTags();

        for (MeterRegistry registry : setRegistry)
            {
            if (registry.find(sName).tags(tags).gauges().size() == 0)
                {
                Gauge.builder(sName, identifier, this::metricToDouble)
                        .tags(tags)
                        .description(holder.getDescription())
                        .register(registry);
                }
            }
        }

    /**
     * Remove a metric from all of the bound Micrometer registries.
     *
     * @param identifier  the identifier of the metric to remove
     */
    void remove(MBeanMetric.Identifier identifier)
        {
        Holder holder = f_mapMetric.remove(identifier);
        if (holder != null)
            {
            String sName = holder.getName();
            Tags   tags  = holder.getTags();
            for (MeterRegistry registry : f_mapRegistry.keySet())
                {
                registry.find(sName)
                        .tags(tags)
                        .gauges()
                        .forEach(g -> registry.remove(g.getId()));
                }
            }

        }

    /**
     * Obtain numerical value of a metric.
     *
     * @param identifier  the identifier of the metric to get the value from
     *
     * @return  the metric's numerical value as a {@code double} or {@code 0.0}
     *          if the metric does not exist or it's value is not a number.
     */
    double metricToDouble(MBeanMetric.Identifier identifier)
        {
        if (identifier != null)
            {
            Holder holder = f_mapMetric.get(identifier);
            return holder == null ? 0.0d : holder.getValue();
            }
        return 0.0d;
        }

    /**
     * Create the {@link Tag} instances from an {@link MBeanMetric.Identifier}.
     *
     * @param identifier  the {@link MBeanMetric.Identifier}
     *
     * @return the tags for the {@link MBeanMetric.Identifier}
     */
    Tags getTags(MBeanMetric.Identifier identifier)
        {
        String sName = identifier.getName();
        List<Tag> tags = identifier.getFormattedTags()
                                   .entrySet()
                                   .stream()
                                   .map(e -> toTag(sName, e))
                                   .filter(Objects::nonNull)
                                   .collect(Collectors.toList());

        return Tags.of(tags);
        }

    /**
     * Create a metric {@link Tag}.
     *
     * @param sName  the metric name
     * @param entry  a {@link Map.Entry} containing the name and value for the tag
     *
     * @return a metric {@link Tag} or {@code null} if the {@link Map.Entry}
     *         contains invalid values for a {@link Tag}
     */
    Tag toTag(String sName, Map.Entry<String, String> entry)
        {
        String sKey   = entry.getKey();
        String sValue = String.valueOf(entry.getValue());
        try
            {
            return Tag.of(sKey, sValue);
            }
        catch (Throwable e)
            {
            CacheFactory.err(String.format("Metric '%s' tag '%s' = '%s' is invalid and will be ignored due to: %s",
                                           sName, sKey, sValue, e.getMessage()));
            return null;
            }
        }

    /**
     * Returns the currently registered metrics.
     *
     * @return the currently registered metrics
     */
    public Map<MBeanMetric.Identifier, Holder> getMetrics()
        {
        return f_mapMetric;
        }

    // ----- inner class: Adapter -------------------------------------------

    /**
     * A MetricsRegistryAdapter that links Micrometer metrics registries
     * to Coherence metrics.
     */
    public static class Adapter
            implements MetricsRegistryAdapter
        {
        // ----- constructors -----------------------------------------------

        /**
         * This class is created by the Java ServiceLoader so must have a
         * default no-args constructor.
         */
        public Adapter()
            {
            }

        // ----- MetricsRegistryAdapter methods ---------------------------------

        @Override
        public void register(MBeanMetric metric)
            {
            CoherenceMicrometerMetrics.INSTANCE.register(metric);
            }

        @Override
        public void remove(MBeanMetric.Identifier identifier)
            {
            CoherenceMicrometerMetrics.INSTANCE.remove(identifier);
            }
        }

    // ----- inner class: Holder --------------------------------------------

    /**
     * A metric holder.
     */
    public static class Holder
        {
        /**
         * Create a holder for a metric.
         *
         * @param metric  the metric to hold
         * @param sName   the metric name
         * @param tags    the metric tags
         */
        public Holder(MBeanMetric metric, String sName, Tags tags)
            {
            f_metric  = metric;
            f_sName   = sName;
            f_tags    = tags;
            }

        /**
         * Return the metric identifier.
         *
         * @return  the metric identifier
         */
        public MBeanMetric.Identifier getIdentifier()
            {
            return f_metric.getIdentifier();
            }

        /**
         * Return the metric name.
         *
         * @return the metric name
         */
        public String getName()
            {
            return f_sName;
            }

        /**
         * Return the metric tags.
         *
         * @return  the metric tags
         */
        public Tags getTags()
            {
            return f_tags;
            }

        /**
         * Return the metric description.
         *
         * @return  the metric description
         */
        public String getDescription()
            {
            return f_metric.getDescription();
            }

        /**
         * Return the value of the metric as a {@code double}.
         * <p>
         * If the metric value is {@code null} or not a number {@code 0.0d}
         * will be returned.
         *
         * @return  the metric value as a {@code double}
         */
        public double getValue()
            {
            Object oValue = f_metric.getValue();
            if (oValue instanceof Number)
                {
                return ((Number) oValue).doubleValue();
                }
            return 0.0d;
            }

        /**
         * Set the metric for this holder.
         *
         * @param metric  the metric for this holder
         *
         * @return  this {@link Holder}
         */
        public Holder setMetric(MBeanMetric metric)
            {
            f_metric = metric;
            return this;
            }

        // ----- data members -----------------------------------------------

        /**
         * The metric being held in this holder.
         */
        private MBeanMetric f_metric;

        /**
         * The name of metric being held in this holder.
         */
        private final String f_sName;

        /**
         * The tags for the metric being held in this holder.
         */
        private final Tags f_tags;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the system property to use to automatically bind to the global registry.
     */
    public static final String PROP_USE_GLOBAL_REGISTRY = "coherence.micrometer.bind.to.global";

    /**
     * The singleton instance of {@link CoherenceMicrometerMetrics}.
     */
    public static final CoherenceMicrometerMetrics INSTANCE = new CoherenceMicrometerMetrics();

    // ----- data members ---------------------------------------------------

    /**
     * The currently registered metrics.
     */
    private final Map<MBeanMetric.Identifier, Holder> f_mapMetric = new ConcurrentHashMap<>();

    /**
     * A map of the {@link MeterRegistry} instances that metrics are bound to.
     */
    private final Map<MeterRegistry,Integer> f_mapRegistry = new WeakIdentityHashMap<>();
    }
