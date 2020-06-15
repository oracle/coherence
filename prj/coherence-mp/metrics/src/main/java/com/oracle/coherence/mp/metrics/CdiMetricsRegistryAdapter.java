/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.mp.metrics;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.metrics.MBeanMetric;
import com.tangosol.net.metrics.MetricsRegistryAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

/**
 * Bridges the gap between {@link ServiceLoader} and CDI, by registering itself
 * as a service and delegating to all discovered CDI beans that implement
 * {@link MetricsRegistryAdapter} interface.
 *
 * @author Aleks Seovic  2020.05.04
 * @since 20.06
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
        try
            {
            Instance<MetricsRegistryAdapter> adapters = m_adapters;
            if (adapters == null)
                {
                adapters = m_adapters = CDI.current().select(MetricsRegistryAdapter.class);
                if (adapters.isUnsatisfied())
                    {
                    Logger.config("No CDI-managed metrics registry adapters were discovered");
                    }
                else
                    {
                    String sAdapterNames = adapters.stream()
                            .map(a -> a.getClass().getSimpleName())
                            .collect(Collectors.joining(", "));
                    Logger.config("Registering CDI-managed metrics registry adapters: " + sAdapterNames);
                    }
                }

            if (!f_deferredActions.isEmpty())
                {
                for (Consumer<MetricsRegistryAdapter> deferredAction : f_deferredActions)
                    {
                    adapters.forEach(deferredAction);
                    }
                f_deferredActions.clear();
                }

            adapters.forEach(action);
            }
        catch (IllegalStateException cdiNotAvailable)
            {
            f_deferredActions.add(action);
            }
        }

    // ---- data members ----------------------------------------------------

    /**
     * {@link MetricsRegistryAdapter}s discovered by CDI.
     */
    private volatile Instance<MetricsRegistryAdapter> m_adapters;

    private final List<Consumer<MetricsRegistryAdapter>> f_deferredActions =
            Collections.synchronizedList(new ArrayList<>());
    }
