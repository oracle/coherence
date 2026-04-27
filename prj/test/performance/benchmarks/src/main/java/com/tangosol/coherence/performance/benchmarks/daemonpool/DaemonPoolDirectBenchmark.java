/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool;

import com.oracle.coherence.common.base.Associated;

import com.tangosol.coherence.component.util.DaemonPool;
import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;

import com.tangosol.coherence.performance.benchmarks.daemonpool.common.BenchmarkProperties;
import com.tangosol.coherence.performance.benchmarks.daemonpool.common.VirtualThreadProbe;

import com.tangosol.internal.net.service.DefaultServiceDependencies;

import com.tangosol.net.DaemonPoolType;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Direct daemon-pool benchmark that bypasses cache, service messaging, and
 * partition dispatch. It compares the worker-pool implementations by submitting
 * small batches of latch-countdown tasks directly to the selected pool.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class DaemonPoolDirectBenchmark
    {
    // ----- benchmark methods ---------------------------------------------

    @Benchmark
    @OperationsPerInvocation(BATCH_SIZE)
    public int submitBatch(BenchmarkState state)
            throws Exception
        {
        return state.submitBatch();
        }

    @Benchmark
    public int submitOne(BenchmarkState state)
            throws Exception
        {
        return state.submitOne();
        }

    // ----- inner class: BenchmarkState -----------------------------------

    @State(Scope.Benchmark)
    public static class BenchmarkState
        {
        @Param({"platform", "virtual"})
        public String poolMode;

        @Param({"associated", "unassociated"})
        public String association;

        @Param({"4"})
        public int workerThreads;

        @Param({"256"})
        public int taskLimit;

        @Setup(Level.Trial)
        public void setup()
            {
            m_mapPreviousProperties = BenchmarkProperties.capture(
                    "coherence.cluster",
                    "coherence.log.level");

            System.setProperty("coherence.cluster", "dpb-direct-" + clusterModeTag() + '-' + UUID.randomUUID());
            System.setProperty("coherence.log.level", "2");

            verifyVirtualThreadRuntime();

            TestServiceDependencies deps = new TestServiceDependencies();
            deps.setThreadPriority(Thread.NORM_PRIORITY);
            if (isVirtualPoolMode())
                {
                deps.setDefaultWorkerThreadCountMin(1);
                deps.setDaemonPoolType(DaemonPoolType.VIRTUAL);
                deps.setTaskLimit(taskLimit);
                }
            else
                {
                deps.setWorkerThreadCountMin(workerThreads);
                deps.setWorkerThreadCountMax(workerThreads);
                deps.setWorkerThreadPriority(Thread.NORM_PRIORITY);
                deps.setDaemonPoolType(DaemonPoolType.PLATFORM);
                }

            TestService service = new TestService("DaemonPoolDirectBenchmark-" + clusterModeTag());
            service.setDependencies(deps);

            m_pool = service.getDaemonPool();

            verifySelectedPoolMode();

            m_pool.start();

            m_pool.resetStats();

            logMetrics("trial-start");
            }

        @TearDown(Level.Iteration)
        public void tearDownIteration()
            {
            logMetrics("iteration-end");
            }

        @TearDown(Level.Trial)
        public void tearDown()
            {
            try
                {
                logMetrics("trial-end");
                }
            finally
                {
                try
                    {
                    DaemonPool pool = m_pool;
                    if (pool != null)
                        {
                        pool.stop();
                        }
                    }
                finally
                    {
                    restoreProperties();
                    }
                }
            }

        int submitBatch()
                throws InterruptedException
            {
            CountDownLatch latch = new CountDownLatch(BATCH_SIZE);
            boolean        fAssociated = isAssociated();

            for (int i = 0; i < BATCH_SIZE; i++)
                {
                m_pool.add(fAssociated
                        ? new AssociatedLatchTask(latch, ASSOCIATED_KEY)
                        : new LatchTask(latch));
                }

            if (!latch.await(30, TimeUnit.SECONDS))
                {
                throw new IllegalStateException("Timed out waiting for direct daemon-pool batch completion: "
                        + "poolMode=" + poolMode + ", association=" + association
                        + ", remaining=" + latch.getCount());
                }

            return BATCH_SIZE;
            }

        int submitOne()
                throws InterruptedException
            {
            CountDownLatch latch = new CountDownLatch(1);
            m_pool.add(isAssociated()
                    ? new AssociatedLatchTask(latch, ASSOCIATED_KEY)
                    : new LatchTask(latch));

            if (!latch.await(30, TimeUnit.SECONDS))
                {
                throw new IllegalStateException("Timed out waiting for direct daemon-pool task completion: "
                        + "poolMode=" + poolMode + ", association=" + association);
                }

            return 1;
            }

        private boolean isAssociated()
            {
            return "associated".equalsIgnoreCase(association);
            }

        private boolean isPlatformMode()
            {
            return "platform".equalsIgnoreCase(poolMode);
            }

        private boolean isVirtualMode()
            {
            return "virtual".equalsIgnoreCase(poolMode);
            }

        private boolean isVirtualPoolMode()
            {
            return isVirtualMode();
            }

        private String clusterModeTag()
            {
            if (isPlatformMode())
                {
                return "p";
                }

            return "v";
            }

        private void verifySelectedPoolMode()
            {
            if (isPlatformMode())
                {
                if (m_pool instanceof Service.VirtualDaemonPool)
                    {
                    throw new IllegalStateException("poolMode=platform selected VirtualDaemonPool.");
                    }
                return;
                }

            if (!(m_pool instanceof Service.VirtualDaemonPool))
                {
                throw new IllegalStateException("poolMode=" + poolMode
                        + " requires VirtualDaemonPool, but selected "
                        + (m_pool == null ? "<null>" : m_pool.getClass().getName()) + '.');
                }
            }

        private void verifyVirtualThreadRuntime()
            {
            if (!isVirtualPoolMode())
                {
                return;
                }

            VirtualThreadProbe.verifyVirtualThreads("DaemonPoolDirectBenchmark[vt-check]",
                    "Direct virtual daemon-pool benchmark");
            }

        private void logMetrics(String sPhase)
            {
            DaemonPool pool = m_pool;
            if (pool == null)
                {
                return;
                }

            System.out.println("DaemonPoolDirectBenchmark[" + sPhase + "]"
                    + " poolMode=" + poolMode
                    + ", association=" + association
                    + ", daemonCount=" + pool.getDaemonCount()
                    + ", backlog=" + pool.getBacklog()
                    + ", taskCount=" + pool.getStatsTaskAddCount()
                    + ", poolClass=" + pool.getClass().getName());
            }

        private void restoreProperties()
            {
            BenchmarkProperties.restore(m_mapPreviousProperties);
            }

        private DaemonPool          m_pool;
        private Map<String, String> m_mapPreviousProperties;

        private static final Object ASSOCIATED_KEY = "direct-associated-key";
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

    public static class TestServiceDependencies
            extends DefaultServiceDependencies
        {
        @Override
        public void setDefaultWorkerThreadCountMin(int cThreads)
            {
            super.setDefaultWorkerThreadCountMin(cThreads);
            }
        }

    // ----- inner class: LatchTask ----------------------------------------

    public static class LatchTask
            implements Runnable
        {
        public LatchTask(CountDownLatch latch)
            {
            f_latch = latch;
            }

        @Override
        public void run()
            {
            f_latch.countDown();
            }

        private final CountDownLatch f_latch;
        }

    // ----- inner class: AssociatedLatchTask ------------------------------

    public static class AssociatedLatchTask
            extends LatchTask
            implements Associated<Object>
        {
        public AssociatedLatchTask(CountDownLatch latch, Object oKey)
            {
            super(latch);
            f_oKey = oKey;
            }

        @Override
        public Object getAssociatedKey()
            {
            return f_oKey;
            }

        private final Object f_oKey;
        }

    // ----- constants ------------------------------------------------------

    private static final int BATCH_SIZE = 1024;
    }
