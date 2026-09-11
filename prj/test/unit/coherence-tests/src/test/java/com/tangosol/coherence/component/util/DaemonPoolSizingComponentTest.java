/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor;

import com.tangosol.coherence.component.net.message.RequestMessage;
import com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheKeyRequest;

import com.tangosol.coherence.config.builder.LocalAddressProviderBuilder;

import com.tangosol.internal.net.service.DefaultServiceDependencies;
import com.tangosol.internal.net.service.LegacyXmlServiceHelper;
import com.tangosol.internal.net.service.extend.proxy.DefaultCacheServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.DefaultInvocationServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.DefaultTopicServiceProxyDependencies;
import com.tangosol.internal.net.service.grid.DefaultPartitionedServiceDependencies;
import com.tangosol.internal.net.service.grid.DefaultProxyServiceDependencies;
import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies;
import com.tangosol.internal.util.DaemonPoolSizing;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.net.DaemonPoolType;
import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.SimpleElement;

import java.lang.reflect.Method;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

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
    public void shouldReserveWorkloadAwareContractionForMeasuredProbe()
        {
        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName("DaemonPoolMeasuredContractionTest");
        deps.setThreadCount(16);
        deps.setThreadCountMin(1);
        deps.setThreadCountMax(64);
        deps.setThreadPriority(Thread.NORM_PRIORITY);

        DaemonPool          pool = (DaemonPool) Daemons.newDaemonPool(deps);
        TestResizeTask      task = new TestResizeTask(pool);

        assertThat(pool.isWorkloadAwareResizeEnabled(), is(true));
        assertThat(task.shrink("legacy underutilization"), is(16));
        assertThat(task.shake(-4), is(16));
        assertThat(pool.getDaemonCount(), is(16));
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
        Acceptor.DaemonPool pool = createProxyAcceptorDaemonPool(50, null);

        assertThat(pool.getDaemonPoolSizingRegistration(), is(nullValue()));
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
    public void shouldConfigureVirtualProxyAcceptorWorkerPool()
        {
        DefaultProxyServiceDependencies deps = new DefaultProxyServiceDependencies();
        deps.setThreadPriority(Thread.NORM_PRIORITY);
        deps.setDaemonPoolType(DaemonPoolType.VIRTUAL);
        deps.setTaskLimit(64);
        deps.setCacheServiceProxyDependencies(new DefaultCacheServiceProxyDependencies());
        deps.setTopicServiceProxyDependencies(new DefaultTopicServiceProxyDependencies());
        deps.setInvocationServiceProxyDependencies(new DefaultInvocationServiceProxyDependencies());

        DefaultTcpAcceptorDependencies acceptorDeps = new DefaultTcpAcceptorDependencies();
        acceptorDeps.setLocalAddressProviderBuilder(new LocalAddressProviderBuilder("127.0.0.1", 0, 0));
        deps.setAcceptorDependencies(acceptorDeps);

        ProxyService service = new ProxyService();
        service.setOperationalContext(Mockito.mock(OperationalContext.class));
        service.setDependencies(deps);

        DaemonPool pool = ((Acceptor) service.getAcceptor()).getDaemonPool();
        assertThat(pool, instanceOf(Peer.VirtualDaemonPool.class));
        assertThat(((Service.VirtualDaemonPool) pool).getTaskLimit(), is(64));
        assertThat(((Service.VirtualDaemonPool) pool).instantiateWrapperTask(new TestRequestMessage(), false),
                instanceOf(Peer.DaemonPool.WrapperTask.class));
        assertThat(pool.getDaemonPoolSizingRole(), is(DaemonPoolSizing.Role.BLOCKING_IO));
        assertThat(service.getCacheServiceProxy().getDaemonPool(), sameInstance(pool));
        assertThat(service.getTopicServiceProxy().getDaemonPool(), sameInstance(pool));
        assertThat(service.getInvocationServiceProxy().getDaemonPool(), sameInstance(pool));
        }

    @Test
    public void shouldDeriveProxyPipelineAndWorkerDefaultsFromAvailableProcessors()
        {
        int cProcessors = Runtime.getRuntime().availableProcessors();
        int cPipelines  = Math.min(3, Math.max(1, cProcessors / 4));

        DefaultTcpAcceptorDependencies standalone = new DefaultTcpAcceptorDependencies();
        assertThat(standalone.getConnectionPipelineCount(), is(1));
        assertThat(standalone.isConnectionPipelineCountConfigured(), is(false));

        DefaultProxyServiceDependencies deps = new DefaultProxyServiceDependencies();
        deps.validate();

        TcpAcceptorDependencies tcp = (TcpAcceptorDependencies) deps.getAcceptorDependencies();
        assertThat(tcp.getConnectionPipelineCount(), is(cPipelines));
        assertThat(deps.getWorkerThreadCountMin(), is(cProcessors * 2));
        assertThat(deps.getWorkerThreadCountMax(), is(Integer.MAX_VALUE));

        DefaultProxyServiceDependencies virtual = new DefaultProxyServiceDependencies();
        virtual.setDaemonPoolType(DaemonPoolType.VIRTUAL);
        virtual.validate();
        assertThat(((TcpAcceptorDependencies) virtual.getAcceptorDependencies())
                .getConnectionPipelineCount(), is(cPipelines));
        assertThat(virtual.isWorkerThreadCountMinConfigured(), is(false));
        assertThat(virtual.isWorkerThreadCountMaxConfigured(), is(false));
        assertThat(virtual.getTaskLimit(), is(0));
        assertThat(virtual.isTaskLimitConfigured(), is(false));
        }

    @Test
    public void shouldLeaveDefaultVirtualProxyAcceptorTaskLimitUnbounded()
        {
        DefaultProxyServiceDependencies deps = new DefaultProxyServiceDependencies();
        deps.setThreadPriority(Thread.NORM_PRIORITY);
        deps.setDaemonPoolType(DaemonPoolType.VIRTUAL);
        deps.setCacheServiceProxyDependencies(new DefaultCacheServiceProxyDependencies());
        deps.setTopicServiceProxyDependencies(new DefaultTopicServiceProxyDependencies());
        deps.setInvocationServiceProxyDependencies(new DefaultInvocationServiceProxyDependencies());

        DefaultTcpAcceptorDependencies acceptorDeps = new DefaultTcpAcceptorDependencies();
        acceptorDeps.setLocalAddressProviderBuilder(new LocalAddressProviderBuilder("127.0.0.1", 0, 0));
        deps.setAcceptorDependencies(acceptorDeps);
        deps.validate();

        ProxyService service = new ProxyService();
        service.setOperationalContext(Mockito.mock(OperationalContext.class));
        service.setDependencies(deps);

        Service.VirtualDaemonPool pool = (Service.VirtualDaemonPool)
                ((Acceptor) service.getAcceptor()).getDaemonPool();
        try
            {
            pool.start();
            assertThat(pool.getTaskLimit(), is(0));
            assertThat(pool.getDirectTaskLimit(),
                    is(Math.max(1, Runtime.getRuntime().availableProcessors() * 4)));
            assertThat(deps.isTaskLimitConfigured(), is(false));
            }
        finally
            {
            pool.stop();
            }
        }

    @Test
    public void shouldPreserveExplicitProxyPipelineAndWorkerConfiguration()
        {
        DefaultProxyServiceDependencies deps = new DefaultProxyServiceDependencies();
        DefaultTcpAcceptorDependencies tcp =
                (DefaultTcpAcceptorDependencies) deps.getAcceptorDependencies();
        tcp.setConnectionPipelineCount(5);
        deps.setWorkerThreadCountMin(7);
        deps.setWorkerThreadCountMax(11);
        deps.validate();

        assertThat(tcp.getConnectionPipelineCount(), is(5));
        assertThat(tcp.isConnectionPipelineCountConfigured(), is(true));
        assertThat(deps.getWorkerThreadCountMin(), is(7));
        assertThat(deps.getWorkerThreadCountMax(), is(11));

        DefaultProxyServiceDependencies disabled = new DefaultProxyServiceDependencies();
        disabled.setWorkerThreadCount(0);
        disabled.validate();
        assertThat(((TcpAcceptorDependencies) disabled.getAcceptorDependencies())
                .getConnectionPipelineCount(), is(1));
        assertThat(disabled.getWorkerThreadCount(), is(0));
        }

    @Test
    public void shouldAggregateAndResetProxyPipelineStatistics()
            throws Exception
        {
        Acceptor.DaemonPool pool = new Acceptor.DaemonPool(null, null, false)
            {
            @Override
            public void resetStats()
                {
                }
            };

        class TestTcpAcceptor extends TcpAcceptor
            {
            TestTcpAcceptor()
                {
                super(null, null, false);
                }

            @Override
            public Acceptor.DaemonPool getDaemonPool()
                {
                return pool;
                }

            @Override
            protected long getProxyPipelineReceived()
                {
                return m_cReceived;
                }

            @Override
            protected long getProxyPipelineBytesReceived()
                {
                return m_cbReceived;
                }

            @Override
            protected long getProxyPipelineCpu()
                {
                return m_cCpu;
                }

            long m_cReceived  = 7L;
            long m_cbReceived = 29L;
            long m_cCpu       = 13L;
            }

        TestTcpAcceptor acceptor = new TestTcpAcceptor();
        invokeLongSetter(acceptor, "setStatsReceived", 11L);
        invokeLongSetter(acceptor, "setStatsBytesReceived", 101L);
        invokeLongSetter(acceptor, "setStatsCpu", 17L);

        assertThat(acceptor.getStatsReceived(), is(18L));
        assertThat(acceptor.getStatsBytesReceived(), is(130L));
        assertThat(acceptor.getStatsCpu(), is(30L));

        acceptor.resetStats();
        assertThat(acceptor.getStatsReceived(), is(0L));
        assertThat(acceptor.getStatsBytesReceived(), is(0L));
        assertThat(acceptor.getStatsCpu(), is(0L));

        acceptor.m_cReceived += 3L;
        acceptor.m_cbReceived += 5L;
        acceptor.m_cCpu += 2L;
        assertThat(acceptor.getStatsReceived(), is(3L));
        assertThat(acceptor.getStatsBytesReceived(), is(5L));
        assertThat(acceptor.getStatsCpu(), is(2L));
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

    @Test
    public void shouldPopulateVirtualThreadSettingsFromLegacyXml()
        {
        SimpleElement xml = new SimpleElement("distributed-scheme");
        xml.addElement("daemon-pool").setString("virtual");
        xml.addElement("task-limit").setInt(77);

        DefaultServiceDependencies deps =
                LegacyXmlServiceHelper.fromXml(xml, new DefaultServiceDependencies(), null);

        assertThat(deps.isDaemonPoolConfigured(), is(true));
        assertThat(deps.getDaemonPoolType(), is(DaemonPoolType.VIRTUAL));
        assertThat(deps.isTaskLimitConfigured(), is(true));
        assertThat(deps.getTaskLimit(), is(77));
        }

    @Test
    public void shouldTrackAmbiguousThreadCountSettingsFromLegacyXml()
        {
        SimpleElement xml = new SimpleElement("distributed-scheme");
        xml.addElement("thread-count").setInt(1);
        xml.addElement("thread-count-min").setInt(1);
        xml.addElement("thread-count-max").setInt(2);

        DefaultServiceDependencies deps =
                LegacyXmlServiceHelper.fromXml(xml, new DefaultServiceDependencies(), null);

        assertThat(deps.isWorkerThreadCountConfigured(), is(true));
        assertThat(deps.isWorkerThreadCountMinConfigured(), is(true));
        assertThat(deps.isWorkerThreadCountMaxConfigured(), is(true));

        TestService service = new TestService("VirtualDaemonPoolAmbiguousXmlThreadCountTest");
        service.setDependencies(deps);

        assertThat(service.getDaemonPool() instanceof Service.VirtualDaemonPool, is(false));
        }

    @Test
    public void shouldUseVirtualDaemonPoolWhenServiceConfigEnabled()
        {
        DefaultPartitionedServiceDependencies deps = createVirtualPartitionedServiceDependencies();
        deps.setTaskLimit(123);

        TestService service = new TestService("VirtualDaemonPoolConfigEnabledTest");
        service.setDependencies(deps);

        assertThat(service.getDaemonPool(), instanceOf(Service.VirtualDaemonPool.class));
        assertThat(((Service.VirtualDaemonPool) service.getDaemonPool()).getTaskLimit(), is(123));
        }

    @Test
    public void shouldRejectThreadCountMaxWhenVirtualDaemonPoolConfigured()
        {
        DefaultPartitionedServiceDependencies deps = createVirtualPartitionedServiceDependencies();
        deps.setWorkerThreadCountMax(2);

        TestService service = new TestService("VirtualDaemonPoolRejectThreadCountMaxTest");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.setDependencies(deps));

        assertThat(error.getMessage(), containsString("<thread-count-max>"));
        assertThat(error.getMessage(), containsString("<daemon-pool> is \"virtual\""));
        }

    @Test
    public void shouldRejectTaskLimitWhenPlatformDaemonPoolConfigured()
        {
        DefaultPartitionedServiceDependencies deps = new DefaultPartitionedServiceDependencies();
        deps.setDaemonPoolType(DaemonPoolType.PLATFORM);
        deps.setTaskLimit(123);

        TestService service = new TestService("VirtualDaemonPoolRejectPlatformTaskLimitTest");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.setDependencies(deps));

        assertThat(error.getMessage(), containsString("<task-limit>"));
        assertThat(error.getMessage(), containsString("<daemon-pool> is \"platform\""));
        }

    @Test
    public void shouldRejectTaskLimitWhenDaemonPoolOmitted()
        {
        DefaultPartitionedServiceDependencies deps = new DefaultPartitionedServiceDependencies();
        deps.setTaskLimit(123);

        TestService service = new TestService("VirtualDaemonPoolRejectOmittedTaskLimitTest");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.setDependencies(deps));

        assertThat(error.getMessage(), containsString("<task-limit>"));
        assertThat(error.getMessage(), containsString("<daemon-pool> is \"platform\""));
        }

    @Test
    public void shouldWarnButStartWhenThreadCountAndDynamicBoundsConfigured()
        {
        DefaultServiceDependencies deps = createServiceDependencies();
        deps.setWorkerThreadCount(1);
        deps.setWorkerThreadCountMin(1);

        TestService service = new TestService("VirtualDaemonPoolAmbiguousThreadCountTest");
        service.setDependencies(deps);

        assertThat(service.getDaemonPool() instanceof Service.VirtualDaemonPool, is(false));
        }

    @Test
    public void shouldReturnExistingDaemonPoolWithoutServiceMonitor()
            throws Exception
        {
        DefaultPartitionedServiceDependencies deps = createVirtualPartitionedServiceDependencies();

        TestService service = new TestService("VirtualDaemonPoolMonitorFastPathTest");
        service.setDependencies(deps);

        DaemonPool pool = service.getDaemonPool();

        CountDownLatch              latchEntered = new CountDownLatch(1);
        CountDownLatch              latchRelease = new CountDownLatch(1);
        AtomicReference<DaemonPool> refPool      = new AtomicReference<>();
        AtomicReference<Throwable>  refError     = new AtomicReference<>();

        Thread threadHolder = new Thread(() ->
            {
            synchronized (service)
                {
                latchEntered.countDown();
                try
                    {
                    latchRelease.await();
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    }
                }
            }, "ServiceMonitorHolder");

        threadHolder.start();
        assertThat(latchEntered.await(1, TimeUnit.SECONDS), is(true));

        Thread threadProbe = new Thread(() ->
            {
            try
                {
                refPool.set(service.getDaemonPool());
                }
            catch (Throwable t)
                {
                refError.set(t);
                }
            }, "DaemonPoolFastPathProbe");

        try
            {
            threadProbe.start();
            threadProbe.join(1000L);

            assertThat("getDaemonPool() should not block on the service monitor when the existing pool matches",
                    threadProbe.isAlive(), is(false));
            assertThat(refPool.get(), sameInstance(pool));
            if (refError.get() != null)
                {
                throw new AssertionError("getDaemonPool() failed", refError.get());
                }
            }
        finally
            {
            latchRelease.countDown();
            threadHolder.join(1000L);
            }
        }

    @Test
    public void shouldDefaultRequestMessagesToReadWrite()
        {
        assertThat(new RequestMessage().isReadOnly(), is(false));
        assertThat(new DistributedCacheKeyRequest().isReadOnly(), is(true));
        assertThat(new PartitionedCache.GetRequest().isReadOnly(), is(false));
        }

    @Test
    public void shouldDelegateWrapperTaskReadOnlyClassification()
        {
        TestService service = new TestService("VirtualDaemonPoolReadOnlyWrapperTest");

        DefaultPartitionedServiceDependencies deps = createVirtualPartitionedServiceDependencies();

        service.setDependencies(deps);

        Service.VirtualDaemonPool pool = (Service.VirtualDaemonPool) service.getDaemonPool();

        assertThat(pool.instantiateWrapperTask(new TestRequestMessage(), false).isReadOnly(), is(false));
        assertThat(pool.instantiateWrapperTask(new DistributedCacheKeyRequest(), false).isReadOnly(), is(true));
        assertThat(pool.instantiateWrapperTask(new PartitionedCache.GetRequest(), false).isReadOnly(), is(false));
        }

    @Test
    public void shouldPreservePartitionedServiceWrapperTaskSpecializationForVirtualPool()
        {
        TestPartitionedService service = new TestPartitionedService("PartitionedVirtualDaemonPoolWrapperTest");

        DefaultPartitionedServiceDependencies deps = createVirtualPartitionedServiceDependencies();

        service.setDependencies(deps);

        PartitionedService.VirtualDaemonPool pool = (PartitionedService.VirtualDaemonPool) service.getDaemonPool();

        assertThat(pool.instantiateWrapperTask(new PartitionedCache.GetRequest(), false),
                instanceOf(PartitionedService.DaemonPool.WrapperTask.class));
        }

    private static DefaultServiceDependencies createServiceDependencies()
        {
        DefaultServiceDependencies deps = new DefaultServiceDependencies();
        deps.setThreadPriority(Thread.NORM_PRIORITY);
        deps.setWorkerThreadCountMin(1);
        deps.setWorkerThreadCountMax(1);
        deps.setWorkerThreadPriority(Thread.NORM_PRIORITY);
        return deps;
        }

    private static DefaultPartitionedServiceDependencies createVirtualPartitionedServiceDependencies()
        {
        DefaultPartitionedServiceDependencies deps = new DefaultPartitionedServiceDependencies();
        deps.setThreadPriority(Thread.NORM_PRIORITY);
        deps.setDaemonPoolType(DaemonPoolType.VIRTUAL);
        return deps;
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

    private static void invokeLongSetter(Peer peer, String sMethod, long nValue)
            throws Exception
        {
        Class<?> clz = Peer.class;
        Method method = null;
        while (clz != null && method == null)
            {
            try
                {
                method = clz.getDeclaredMethod(sMethod, long.class);
                }
            catch (NoSuchMethodException ignored)
                {
                clz = clz.getSuperclass();
                }
            }
        if (method == null)
            {
            throw new NoSuchMethodException(sMethod);
            }
        method.setAccessible(true);
        method.invoke(peer, nValue);
        }

    // ----- inner class: TestService --------------------------------------

    public static class TestService
            extends Service
        {
        public TestService(String sName)
            {
            super(sName, null, false);

            __initPrivate();
            setServiceName(sName);
            }
        }

    public static class TestPartitionedService
            extends PartitionedService
        {
        public TestPartitionedService(String sName)
            {
            super(sName, null, false);

            __initPrivate();
            setServiceName(sName);
            }
        }

    public static class TestRequestMessage
            extends RequestMessage
            implements Runnable
        {
        @Override
        public void run()
            {
            }
        }

    public static class TestResizeTask
            extends DaemonPool.ResizeTask
        {
        public TestResizeTask(DaemonPool pool)
            {
            super(null, pool, true);
            }

        public int shrink(String sReason)
            {
            return shrinkDaemonPool(sReason);
            }

        public int shake(int cDelta)
            {
            return resizeShakenPool(cDelta);
            }
        }
    }
