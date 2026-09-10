/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.metrics;

import com.tangosol.net.management.Registry;

import com.tangosol.net.management.MBeanServerProxy;

import com.tangosol.net.metrics.MetricsRegistryAdapter;

import org.junit.Test;

import javax.management.MBeanInfo;

import java.util.Collections;
import java.util.List;

import java.util.function.Supplier;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jk  2019.06.24
 */
public class MetricSupportTest
    {
    @Test
    public void shouldNotHaveRegistries()
        {
        Supplier<Registry>           supplier = () -> null;
        List<MetricsRegistryAdapter> loader   = Collections.emptyList();
        MetricSupport                support  = new MetricSupport(supplier, loader);

        assertThat(support.hasRegistries(), is(false));
        }

    @Test
    public void shouldHaveRegistries()
        {
        Supplier<Registry>           supplier = () -> null;
        MetricsRegistryAdapter       adapter  = mock(MetricsRegistryAdapter.class);
        List<MetricsRegistryAdapter> loader   = Collections.singletonList(adapter);
        MetricSupport                support  = new MetricSupport(supplier, loader);

        assertThat(support.hasRegistries(), is(true));
        }

    /**
     * Regression test for COH-33830.
     *
     * A serialization thread used to hold the {@link MetricSupport} monitor
     * while obtaining MBean metadata from the management gateway. At the same
     * time, management-senior transition holds the gateway monitor while
     * registering metrics. That lock-order inversion deadlocks both threads.
     */
    @Test
    public void shouldNotHoldMetricSupportLockWhileAccessingMBeanServerProxy() throws Exception
        {
        String             sSerializationMBean = "type=Serialization,name=COH-33830";
        String             sTransitionMBean    = "type=Node,name=COH-33830";
        Object             lockGateway         = new Object();
        CountDownLatch     latchProxyEntered   = new CountDownLatch(1);
        CountDownLatch     latchGatewayHeld    = new CountDownLatch(1);
        Registry           registry            = mock(Registry.class);
        MBeanServerProxy   proxy               = mock(MBeanServerProxy.class);
        MetricsRegistryAdapter adapter         = mock(MetricsRegistryAdapter.class);
        MetricSupport      support             = new MetricSupport(() -> registry,
                                                                  Collections.singletonList(adapter));
        MBeanInfo          info                = new MBeanInfo(getClass().getName(), "COH-33830",
                                                               null, null, null, null);

        when(registry.getMBeanServerProxy()).thenReturn(proxy);
        when(registry.ensureGlobalName(Registry.CLUSTER_TYPE)).thenReturn(Registry.CLUSTER_TYPE);
        when(proxy.local()).thenReturn(proxy);
        when(proxy.getMBeanInfo(anyString())).thenAnswer(invocation ->
            {
            if (sSerializationMBean.equals(invocation.getArgument(0)))
                {
                latchProxyEntered.countDown();
                assertTrue("management transition did not acquire the gateway lock",
                           latchGatewayHeld.await(5, TimeUnit.SECONDS));
                synchronized (lockGateway)
                    {
                    // Model Remote.LocalMBeanServerProxy.getMBeanInfo().
                    }
                }
            return info;
            });

        ExecutorService executor = Executors.newFixedThreadPool(2, runnable ->
            {
            Thread thread = new Thread(runnable, "COH-33830-regression");
            thread.setDaemon(true);
            return thread;
            });

        Future<?> futureSerialization = executor.submit(() -> support.register(sSerializationMBean));
        try
            {
            assertTrue("serialization metric registration did not reach the management gateway",
                       latchProxyEntered.await(5, TimeUnit.SECONDS));

            Future<?> futureTransition = executor.submit(() ->
                {
                synchronized (lockGateway)
                    {
                    latchGatewayHeld.countDown();
                    support.register(sTransitionMBean);
                    }
                });

            futureTransition.get(5, TimeUnit.SECONDS);
            futureSerialization.get(5, TimeUnit.SECONDS);
            }
        finally
            {
            executor.shutdownNow();
            }
        }

    }
