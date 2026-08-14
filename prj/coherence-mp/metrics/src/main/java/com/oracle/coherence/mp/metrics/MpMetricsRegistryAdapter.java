/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.mp.metrics;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.metrics.MBeanMetric;
import com.tangosol.net.metrics.MetricsRegistryAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.annotation.Priority;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;

import jakarta.enterprise.event.Observes;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import jakarta.interceptor.Interceptor;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;

import org.eclipse.microprofile.metrics.annotation.RegistryScope;

/**
 * An implementation of {@link MetricsRegistryAdapter}  registers Coherence
 * metrics with Helidon's vendor or application registry.
 *
 * This implementation is NOT discoverable by the standard {@link ServiceLoader}
 * mechanism, as it requires CDI injection of vendor and application metrics
 * registries. {@link CdiMetricsRegistryAdapter} bridges the gap between the
 * {@link ServiceLoader} and the CDI, and will discover this adapter and
 * register metrics with it.
 *
 * @author Aleks Seovic     2019.09.13
 * @author Jonathan Knight  2020.01.08
 *
 * @since 20.06
 */
@ApplicationScoped
public class MpMetricsRegistryAdapter
        implements MetricsRegistryAdapter
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a {@link MpMetricsRegistryAdapter}.
     *
     * @param vendorRegistry  the {@link MetricRegistry} for vendor metrics
     * @param appRegistry     the {@link MetricRegistry} for application metrics
     *
     * @throws NullPointerException if either {@link MetricRegistry} parameter is {@code null}
     */
    public MpMetricsRegistryAdapter(
            @RegistryScope(scope = MetricRegistry.VENDOR_SCOPE) MetricRegistry vendorRegistry,
            @RegistryScope(scope = MetricRegistry.APPLICATION_SCOPE) MetricRegistry appRegistry)
        {
        m_vendorRegistry = Objects.requireNonNull(vendorRegistry);
        m_appRegistry    = Objects.requireNonNull(appRegistry);

        f_vendorRegistryProvider = null;
        f_appRegistryProvider    = null;

        m_fInitialized = true;
        }

    /**
     * Construct a CDI-managed {@link MpMetricsRegistryAdapter}.
     *
     * @param vendorRegistryProvider  the provider for the vendor metrics registry
     * @param appRegistryProvider     the provider for the application metrics registry
     */
    @Inject
    MpMetricsRegistryAdapter(
            @RegistryScope(scope = MetricRegistry.VENDOR_SCOPE) Provider<MetricRegistry> vendorRegistryProvider,
            @RegistryScope(scope = MetricRegistry.APPLICATION_SCOPE) Provider<MetricRegistry> appRegistryProvider)
        {
        f_vendorRegistryProvider = Objects.requireNonNull(vendorRegistryProvider);
        f_appRegistryProvider    = Objects.requireNonNull(appRegistryProvider);
        }

    // ---- MetricsRegistryAdapter interface --------------------------------

    @Override
    public synchronized void register(MBeanMetric metric)
        {
        Objects.requireNonNull(metric);

        if (m_fInitialized)
            {
            registerInternal(metric);
            }
        else
            {
            f_deferredActions.add(() -> registerInternal(metric));
            }
        }

    @Override
    public synchronized void remove(MBeanMetric.Identifier identifier)
        {
        Objects.requireNonNull(identifier);

        if (m_fInitialized)
            {
            removeInternal(identifier);
            }
        else
            {
            f_deferredActions.add(() -> removeInternal(identifier));
            }
        }

    // ---- lifecycle methods ----------------------------------------------

    /**
     * Resolve the MicroProfile metrics registries after library initialization
     * has completed and apply any deferred metric operations.
     *
     * @param event  the application scope initialization event
     */
    synchronized void onApplicationScopeInitialized(
            @Observes @Priority(Interceptor.Priority.APPLICATION) @Initialized(ApplicationScoped.class) Object event)
        {
        if (m_fInitialized)
            {
            return;
            }

        m_vendorRegistry = Objects.requireNonNull(f_vendorRegistryProvider.get());
        m_appRegistry    = Objects.requireNonNull(f_appRegistryProvider.get());

        for (Runnable action : f_deferredActions)
            {
            try
                {
                action.run();
                }
            catch (Throwable t)
                {
                Logger.err("Failed to apply a deferred MicroProfile metric operation", t);
                }
            }

        f_deferredActions.clear();
        m_fInitialized = true;
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Register a metric with the appropriate MicroProfile metrics registry.
     *
     * @param metric  the metric to register
     */
    private void registerInternal(MBeanMetric metric)
        {
        String   sName        = metric.getName();
        String   sDescription = getDescription(metric);
        Tag[]    aTags        = getTags(metric.getIdentifier());
        MetricID id           = new MetricID(sName, aTags);

        Metadata metadata = Metadata.builder()
                .withName(sName)
                // BUG: commenting description out for now because of bug in Helidon
                // BUG: which doesn't allow description to be different for metrics
                // BUG: that have the same name
                //.withDescription(sDescription)
                .build();

        Supplier<Number> gauge = new MBeanMetricGauge(metric);

        switch (metric.getScope())
            {
            case VENDOR:
                if (!m_vendorRegistry.getGauges().containsKey(id))
                    {
                    m_vendorRegistry.gauge(metadata, gauge, aTags);
                    }
                break;
            case APPLICATION:
                if (!m_appRegistry.getGauges().containsKey(id))
                    {
                    m_appRegistry.gauge(metadata, gauge, aTags);
                    }
                break;
            case BASE:
            default:
                // do nothing - ignore any other type of metric
            }
        }

    /**
     * Remove a metric from the appropriate MicroProfile metrics registry.
     *
     * @param identifier  the identifier of the metric to remove
     */
    private void removeInternal(MBeanMetric.Identifier identifier)
        {
        Tag[]    aTags = getTags(identifier);
        MetricID id    = new MetricID(identifier.getName(), aTags);

        switch (identifier.getScope())
            {
            case VENDOR:
                if (m_vendorRegistry.getGauges().containsKey(id))
                    {
                    m_vendorRegistry.remove(id);
                    }
                break;
            case APPLICATION:
                if (m_appRegistry.getGauges().containsKey(id))
                    {
                    m_appRegistry.remove(id);
                    }
                break;
            case BASE:
            default:
                // do nothing - ignore any other type of metric
            }
        }

    /**
     * Create an array of {@link Tag} instances from an {@link
     * MBeanMetric.Identifier}.
     *
     * @param identifier  the {@link MBeanMetric.Identifier}
     *
     * @return the tags for the {@link MBeanMetric.Identifier}
     */
    Tag[] getTags(MBeanMetric.Identifier identifier)
        {
        String sName = identifier.getName();
        return identifier.getTags()
                .entrySet()
                .stream()
                .filter(e -> !NAME_TAG_EXCLUDES.contains(e.getKey()))
                .map(e -> toTag(sName, e))
                .filter(Objects::nonNull)
                .toArray(Tag[]::new);
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
            return new Tag(sKey, sValue);
            }
        catch (Throwable e)
            {
            Logger.err(String.format("Metric '%s' tag '%s' = '%s' is invalid and will be ignored due to: %s",
                                     sName, sKey, sValue, e.getMessage()));
            return null;
            }
        }

    /**
     * Returns the metric description.
     *
     * @param metric  the {@link MBeanMetric} to obtain the description from
     *
     * @return the metric description
     */
    String getDescription(MBeanMetric metric)
        {
        String sDescription = metric.getDescription();

        if (sDescription != null)
            {
            // Some versions of Coherence append the MBean name to the metric description
            // but in Microprofile metrics descriptions must be consistent for a metric name
            // so we strip off the MBean name.
            int nPos = sDescription.indexOf(" (MBean '");
            if (nPos > 0)
                {
                sDescription = sDescription.substring(0, nPos);
                }
            }
        return sDescription;
        }

    // ---- inner class: MBeanMetricGauge -----------------------------------

    /**
     * A metric {@link org.eclipse.microprofile.metrics.Gauge} that wraps a
     * Coherence {@link MBeanMetric}.
     */
    private static class MBeanMetricGauge
            implements Supplier<Number>
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct {@code MBeanMetricGauge} instance.
         *
         * @param metric  the metric to wrap
         */
        private MBeanMetricGauge(MBeanMetric metric)
            {
            this.metric = metric;
            }

        // ---- Gauge interface ---------------------------------------------

        @Override
        public Number get()
            {
            Number value = (Number) metric.getValue();
            return value == null ? 0 : value;
            }

        // ---- data members ------------------------------------------------

        /**
         * The metric to wrap.
         */
        private final MBeanMetric metric;
        }

    // ---- constants ------------------------------------------------------

    /**
     * MicroProfile-specific list of tag names to exclude from an
     * {@link MBeanMetric.Identifier}.
     * <p>
     * Exclude the "loader" tag as not all cache MBeans have a loader
     * attribute.
     */
    private static final Set<String> NAME_TAG_EXCLUDES = Collections.singleton("loader");

    // ---- data members ----------------------------------------------------

    /**
     * MicroProfile Vendor Registry to publish metrics to.
     */
    private MetricRegistry m_vendorRegistry;

    /**
     * MicroProfile Application Registry to publish metrics to.
     */
    private MetricRegistry m_appRegistry;

    /**
     * Provider for the MicroProfile Vendor Registry used by CDI.
     */
    private final Provider<MetricRegistry> f_vendorRegistryProvider;

    /**
     * Provider for the MicroProfile Application Registry used by CDI.
     */
    private final Provider<MetricRegistry> f_appRegistryProvider;

    /**
     * Deferred metric operations waiting for CDI application initialization.
     */
    private final List<Runnable> f_deferredActions = new ArrayList<>();

    /**
     * {@code true} after the MicroProfile metrics registries are safe to use.
     */
    private boolean m_fInitialized;
    }
