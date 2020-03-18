/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.metrics;

/**
 * A registry adapter for Coherence MBean metrics.
 * <p>
 * Instances of this {@link MetricsRegistryAdapter} will be discovered
 * using the {@link java.util.ServiceLoader} and notified when MBean
 * metrics are registered or removed.
 *
 * @author jk  2019.05.23
 * @since 12.2.1.4
 */
public interface MetricsRegistryAdapter
    {
    /**
     * Register a metric.
     *
     * @param metric  the {@link MBeanMetric} to register
     */
    public void register(MBeanMetric metric);

    /**
     * Remove a metric.
     *
     * @param identifier  the {@link MBeanMetric.Identifier} of th
     *                    metric to be removed
     */
    public void remove(MBeanMetric.Identifier identifier);
    }
