/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor;

import com.tangosol.coherence.config.builder.LocalAddressProviderBuilder;

import com.tangosol.internal.net.service.DefaultServiceDependencies;
import com.tangosol.internal.net.service.extend.proxy.DefaultCacheServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.DefaultInvocationServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.DefaultTopicServiceProxyDependencies;
import com.tangosol.internal.net.service.grid.DefaultProxyServiceDependencies;
import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;
import com.tangosol.internal.util.DaemonPoolSizing;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.net.OperationalContext;

import org.junit.Test;

import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DaemonPoolSizingComponentTest
    {
    @Test
    public void shouldDeriveMaxOnlyWhenUnboundedDaemonPoolStarts()
        {
        int cMin = 4;

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName("DaemonPoolSizingComponentTest");
        deps.setThreadCount(cMin);
        deps.setThreadCountMin(cMin);
        deps.setThreadCountMax(Integer.MAX_VALUE);
        deps.setThreadPriority(Thread.NORM_PRIORITY);

        DaemonPool pool = (DaemonPool) Daemons.newDaemonPool(deps);

        assertThat(pool.getDaemonCountMin(), is(cMin));
        assertThat(pool.getDaemonCountMax(), is(Integer.MAX_VALUE));
        assertThat(pool.getDaemonCountConfiguredMax(), is(Integer.MAX_VALUE));
        assertThat(pool.getDaemonCount(), is(cMin));

        try
            {
            pool.start();
            assertThat(pool.getDaemonCountMax() < Integer.MAX_VALUE, is(true));
            assertThat(pool.getDaemonCountMax() >= cMin, is(true));
            }
        finally
            {
            pool.stop();
            pool.join(5000L);
            }
        }

    @Test
    public void shouldPreserveExplicitDaemonPoolMax()
        {
        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName("DaemonPoolSizingComponentTest");
        deps.setThreadCount(128);
        deps.setThreadCountMin(4);
        deps.setThreadCountMax(64);
        deps.setThreadPriority(Thread.NORM_PRIORITY);

        DaemonPool pool = (DaemonPool) Daemons.newDaemonPool(deps);

        assertThat(pool.getDaemonCountMin(), is(4));
        assertThat(pool.getDaemonCountMax(), is(64));
        assertThat(pool.getDaemonCount(), is(64));
        }

    @Test
    public void shouldNotReserveWorkersForIgnoredResize()
        {
        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName("DaemonPoolSizingIgnoredResizeTest");
        deps.setThreadCount(1);
        deps.setThreadCountMin(1);
        deps.setThreadCountMax(Integer.MAX_VALUE);
        deps.setThreadPriority(Thread.NORM_PRIORITY);

        DaemonPool pool = (DaemonPool) Daemons.newDaemonPool(deps);

        try
            {
            pool.start();

            int cWorkers = DaemonPoolSizing.getSnapshot().getWorkerCount();
            pool.setInTransition(true);
            pool.setDaemonCount(pool.getDaemonCount() + 1);

            assertThat(pool.getDaemonCount(), is(1));
            assertThat(DaemonPoolSizing.getSnapshot().getWorkerCount(), is(cWorkers));
            }
        finally
            {
            pool.setInTransition(false);
            pool.stop();
            pool.join(5000L);
            }
        }

    @Test
    public void shouldClassifyUnboundedServiceWorkerPool()
        {
        int cMin = 4;

        DefaultServiceDependencies deps = new DefaultServiceDependencies();
        deps.setThreadPriority(Thread.NORM_PRIORITY);
        deps.setWorkerThreadCountMin(cMin);
        deps.setWorkerThreadCountMax(Integer.MAX_VALUE);
        deps.setWorkerThreadPriority(Thread.NORM_PRIORITY);

        TestService service = new TestService("DaemonPoolSizingServiceComponentTest");

        service.setDependencies(deps);

        DaemonPool pool = service.getDaemonPool();
        assertThat(pool.getDaemonCountMin(), is(cMin));
        assertThat(pool.getDaemonCountMax(), is(Integer.MAX_VALUE));
        assertThat(pool.getDaemonPoolSizingRole(), is(DaemonPoolSizing.Role.SERVICE));
        assertThat(pool.getDaemonCount(), is(cMin));
        }

    @Test
    public void shouldClassifyUnboundedProxyAcceptorWorkerPool()
        {
        int cMin = 50;

        Acceptor.DaemonPool pool = createProxyAcceptorDaemonPool(cMin, null);

        assertThat(pool.getDaemonCountMin(), is(cMin));
        assertThat(pool.getDaemonCountMax(), is(Integer.MAX_VALUE));
        assertThat(pool.getDaemonPoolSizingRole(), is(DaemonPoolSizing.Role.BLOCKING_IO));
        assertThat(pool.getDaemonCount(), is(cMin));
        }

    @Test
    public void shouldKeepUnboundedProxyAcceptorDynamicWhenEffectiveMaxEqualsMinimum()
        {
        int cMin = 50;

        Acceptor.DaemonPool pool = createProxyAcceptorDaemonPool(cMin, null);
        pool.setDaemonCountEffectiveMax(cMin);

        assertThat(pool.getDaemonCountConfiguredMax(), is(Integer.MAX_VALUE));
        assertThat(pool.getDaemonCountMax(), is(cMin));
        assertThat(pool.isDynamic(), is(true));
        }

    @Test
    public void shouldNotRegisterDormantProxyPoolsDuringConfiguration()
        {
        int cPools = DaemonPoolSizing.getSnapshot().getPoolCount();

        createProxyAcceptorDaemonPool(50, null);

        assertThat(DaemonPoolSizing.getSnapshot().getPoolCount(), is(cPools));
        }

    @Test
    public void shouldPreserveExplicitProxyAcceptorWorkerPoolMax()
        {
        int cMin = 50;
        int cMax = 64;

        Acceptor.DaemonPool pool = createProxyAcceptorDaemonPool(cMin, cMax);

        assertThat(pool.getDaemonCountMin(), is(cMin));
        assertThat(pool.getDaemonCountMax(), is(cMax));
        assertThat(pool.getDaemonCount(), is(cMin));
        }

    @Test
    public void shouldStartServiceWorkerPoolWithWakeupNudge()
        {
        TestService service = new TestService("DaemonPoolSizingServiceComponentTest");
        DaemonPool  pool    = service.getDaemonPool();

        try
            {
            pool.start();

            assertThat(pool.isStarted(), is(true));
            assertThat(pool.getIdleDaemonStack() == null, is(false));
            }
        finally
            {
            pool.stop();
            pool.join(5000L);
            }
        }

    private static Acceptor.DaemonPool createProxyAcceptorDaemonPool(int cMin, Integer cMax)
        {
        DefaultProxyServiceDependencies deps = new DefaultProxyServiceDependencies();
        deps.setThreadPriority(Thread.NORM_PRIORITY);
        deps.setWorkerThreadCountMin(cMin);
        if (cMax != null)
            {
            deps.setWorkerThreadCountMax(cMax);
            }
        deps.setWorkerThreadPriority(Thread.NORM_PRIORITY);
        deps.setCacheServiceProxyDependencies(new DefaultCacheServiceProxyDependencies());
        deps.setTopicServiceProxyDependencies(new DefaultTopicServiceProxyDependencies());
        deps.setInvocationServiceProxyDependencies(new DefaultInvocationServiceProxyDependencies());

        DefaultTcpAcceptorDependencies acceptorDeps = new DefaultTcpAcceptorDependencies();
        acceptorDeps.setLocalAddressProviderBuilder(new LocalAddressProviderBuilder("127.0.0.1", 0, 0));
        deps.setAcceptorDependencies(acceptorDeps);

        ProxyService service = new ProxyService();
        service.setOperationalContext(Mockito.mock(OperationalContext.class));
        service.setDependencies(deps);

        return (Acceptor.DaemonPool) ((Acceptor) service.getAcceptor()).getDaemonPool();
        }

    // ----- inner class: TestService --------------------------------------

    public static class TestService
            extends Service
        {
        public TestService(String sName)
            {
            super(sName, null, false);

            __initPrivate();
            setDaemonPool(new Service.DaemonPool("DaemonPool", this, true));
            setServiceName(sName);
            }
        }
    }
