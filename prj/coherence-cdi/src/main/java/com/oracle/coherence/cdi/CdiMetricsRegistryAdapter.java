/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.net.metrics.MBeanMetric;
import com.tangosol.net.metrics.MetricsRegistryAdapter;

import java.util.ServiceLoader;
import java.util.function.Consumer;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

/**
 * Bridges the gap between {@link ServiceLoader} and CDI, by registering itself
 * as a service and delegating to all discovered CDI beans that implement
 * {@link MetricsRegistryAdapter} interface.
 *
 * @author Aleks Seovic  2020.05.04
 */
public class CdiMetricsRegistryAdapter
        implements MetricsRegistryAdapter
    {
    @Override
    public void register(MBeanMetric metric)
        {
        delegate(adapter -> adapter.register(metric));
        }

    @Override
    public void remove(MBeanMetric.Identifier identifier)
        {
        delegate(adapter -> adapter.remove(identifier));
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Delegates specified action to all discovered CDI beans that implement
     * {@link MetricsRegistryAdapter} interface.
     *
     * @param action  the action to delegate
     */
    private void delegate(Consumer<MetricsRegistryAdapter> action)
        {
        CDI<Object> cdi;
        try
            {
            cdi = CDI.current();
            }
        catch (IllegalStateException cdiNotAvailable)
            {
            return;
            }

        Instance<MetricsRegistryAdapter> adapters = m_adapters;
        if (adapters == null)
            {
            adapters = m_adapters = cdi.select(MetricsRegistryAdapter.class);
            }

        adapters.forEach(action);
        }

    // ---- data members ----------------------------------------------------

    /**
     * {@link MetricsRegistryAdapter}s discovered by CDI.
     */
    private volatile Instance<MetricsRegistryAdapter> m_adapters;
    }
