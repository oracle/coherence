/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.coherence.component.net.message.RequestMessage;
import com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheKeyRequest;

import com.tangosol.internal.net.service.DefaultServiceDependencies;
import com.tangosol.internal.net.service.LegacyXmlServiceHelper;
import com.tangosol.internal.net.service.grid.DefaultPartitionedServiceDependencies;
import com.tangosol.internal.util.DaemonPoolSizing;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.net.DaemonPoolType;

import com.tangosol.run.xml.SimpleElement;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class DaemonPoolSizingComponentTest
    {
    @Test
    public void shouldApplyDerivedMaxToUnboundedDaemonPoolDependencies()
        {
        int cMin = 4;

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName("DaemonPoolSizingComponentTest");
        deps.setThreadCount(Integer.MAX_VALUE);
        deps.setThreadCountMin(cMin);
        deps.setThreadCountMax(Integer.MAX_VALUE);
        deps.setThreadPriority(Thread.NORM_PRIORITY);

        DaemonPoolSizing.Result result = DaemonPoolSizing.resolveThreadCountMax(Integer.MAX_VALUE, cMin);
        DaemonPool             pool   = (DaemonPool) Daemons.newDaemonPool(deps);

        assertThat(pool.getDaemonCountMin(), is(cMin));
        assertThat(pool.getDaemonCountMax(), is(result.getEffectiveMax()));
        assertThat(pool.getDaemonCount(), is(result.getEffectiveMax()));
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
    public void shouldApplyDerivedMaxToUnboundedServiceWorkerPool()
        {
        int cMin = 4;

        DefaultServiceDependencies deps = new DefaultServiceDependencies();
        deps.setThreadPriority(Thread.NORM_PRIORITY);
        deps.setWorkerThreadCountMin(cMin);
        deps.setWorkerThreadCountMax(Integer.MAX_VALUE);
        deps.setWorkerThreadPriority(Thread.NORM_PRIORITY);

        DaemonPoolSizing.Result result  = DaemonPoolSizing.resolveThreadCountMax(Integer.MAX_VALUE, cMin);
        TestService            service = new TestService("DaemonPoolSizingServiceComponentTest");

        service.setDependencies(deps);

        DaemonPool pool = service.getDaemonPool();
        assertThat(pool.getDaemonCountMin(), is(cMin));
        assertThat(pool.getDaemonCountMax(), is(result.getEffectiveMax()));
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
    }
