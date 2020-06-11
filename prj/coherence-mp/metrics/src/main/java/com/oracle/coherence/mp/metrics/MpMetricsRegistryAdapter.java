/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.mp.metrics;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.metrics.MBeanMetric;
import com.tangosol.net.metrics.MetricsRegistryAdapter;

import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import org.eclipse.microprofile.metrics.annotation.RegistryType;

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
    @Inject
    MpMetricsRegistryAdapter(
            @RegistryType(type = Type.VENDOR) MetricRegistry vendorRegistry,
            @RegistryType(type = Type.APPLICATION) MetricRegistry appRegistry)
        {
        f_vendorRegistry = Objects.requireNonNull(vendorRegistry);
        f_appRegistry    = Objects.requireNonNull(appRegistry);
        }

    // ---- MetricsRegistryAdapter interface --------------------------------

    @Override
    public synchronized void register(MBeanMetric metric)
        {
        String   sName        = metric.getName();
        String   sDescription = getDescription(metric);
        Tag[]    aTags        = getTags(metric.getIdentifier());
        MetricID id           = new MetricID(sName, aTags);

        Metadata metadata = Metadata.builder()
                .withName(sName)
                .withDescription(sDescription)
                .withType(MetricType.GAUGE)
                .build();

        Gauge<Object> gauge = new MBeanMetricGauge(metric);

        switch (metric.getScope())
            {
            case VENDOR:
                if (!f_vendorRegistry.getGauges().containsKey(id))
                    {
                    f_vendorRegistry.register(metadata, gauge, aTags);
                    }
                break;
            case APPLICATION:
                if (!f_appRegistry.getGauges().containsKey(id))
                    {
                    f_appRegistry.register(metadata, gauge, aTags);
                    }
                break;
            case BASE:
            default:
                // do nothing - ignore any other type of metric
            }
        }

    @Override
    public void remove(MBeanMetric.Identifier identifier)
        {
        Tag[]    aTags = getTags(identifier);
        MetricID id    = new MetricID(identifier.getName(), aTags);

        switch (identifier.getScope())
            {
            case VENDOR:
                if (f_vendorRegistry.getGauges().containsKey(id))
                    {
                    f_vendorRegistry.remove(id);
                    }
                break;
            case APPLICATION:
                if (f_appRegistry.getGauges().containsKey(id))
                    {
                    f_appRegistry.remove(id);
                    }
                break;
            case BASE:
            default:
                // do nothing - ignore any other type of metric
            }
        }

    // ---- helpers ---------------------------------------------------------

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
            CacheFactory.err(String.format("Metric '%s' tag '%s' = '%s' is invalid and will be ignored due to: %s",
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
            implements Gauge<Object>
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
        public Object getValue()
            {
            Object value = metric.getValue();
            return value == null ? 0 : value;
            }

        // ---- data members ------------------------------------------------

        /**
         * The metric to wrap.
         */
        private final MBeanMetric metric;
        }

    // ---- data members ----------------------------------------------------

    /**
     * MicroProfile Vendor Registry to publish metrics to.
     */
    private final MetricRegistry f_vendorRegistry;

    /**
     * MicroProfile Application Registry to publish metrics to.
     */
    private final MetricRegistry f_appRegistry;
    }
