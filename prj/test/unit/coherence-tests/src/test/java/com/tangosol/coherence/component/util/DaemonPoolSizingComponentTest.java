/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;

import com.tangosol.internal.net.service.DefaultServiceDependencies;
import com.tangosol.internal.util.DaemonPoolSizing;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
