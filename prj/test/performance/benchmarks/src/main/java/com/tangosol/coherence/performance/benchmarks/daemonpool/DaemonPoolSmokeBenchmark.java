/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool;

import com.tangosol.coherence.component.util.DaemonPool;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;

import com.tangosol.coherence.performance.benchmarks.daemonpool.common.BenchmarkProperties;
import com.tangosol.coherence.performance.benchmarks.daemonpool.common.DaemonPoolMetricsSampler;
import com.tangosol.coherence.performance.benchmarks.daemonpool.common.VirtualThreadProbe;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase 3 smoke benchmark scaffold for comparing platform and virtual
 * daemon-pool execution modes at the service level.
 *
 * <p>The initial workload is intentionally narrow: a no-op entry processor
 * against a single in-process distributed cache. This is enough to validate
 * pool-mode selection, benchmark startup, and lightweight service/process
 * observability before we widen into more representative mixes.
 *
 * @author Aleks Seovic  2026.04.24
 * @since 26.04
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class DaemonPoolSmokeBenchmark
    {
    // ----- benchmark methods ---------------------------------------------

    @Benchmark
    public int invokeNoOpProcessor(BenchmarkState state)
        {
        return state.getCache().invoke(state.nextKey(), NoOpProcessor.INSTANCE);
        }

    @Benchmark
    public int getAllTwoKeys(BenchmarkState state)
        {
        return state.getCache().getAll(state.nextKeyPair()).size();
        }

    @Benchmark
    public int invokeSleepingProcessor(BenchmarkState state)
        {
        return state.getCache().invoke(state.nextKey(), state.getSleepingProcessor());
        }

    @Benchmark
    public int invokeAllSleepingProcessorTwoKeys(BenchmarkState state)
        {
        return state.getCache().invokeAll(state.nextKeyPair(), state.getSleepingProcessor()).size();
        }

    @Benchmark
    public int putValue(BenchmarkState state)
        {
        Integer nPrevious = state.getCache().put(state.nextKey(), state.nextValue());
        return nPrevious == null ? -1 : nPrevious;
        }

    // ----- inner class: benchmark state ----------------------------------

    @State(Scope.Benchmark)
    public static class BenchmarkState
        {
        @Param({"platform", "virtual"})
        public String poolMode;

        @Param({"UNIFORM", "HOTSET"})
        public String keyDistribution;

        @Param({"10000"})
        public int cacheSize;

        @Param({"64"})
        public int hotSetSize;

        @Param({"100"})
        public int processorSleepMicros;

        @Setup(Level.Trial)
        public void setup()
            {
            m_mapPreviousProperties = BenchmarkProperties.capture(
                    "coherence.cacheconfig",
                    "coherence.benchmark.daemonpool",
                    "coherence.cluster",
                    BENCHMARK_MAILBOX_STATS_PROPERTY,
                    "coherence.distributed.localstorage",
                    "coherence.localhost",
                    "coherence.log.level",
                    "coherence.ttl",
                    "coherence.wka");

            CacheFactory.shutdown();

            System.setProperty("coherence.cacheconfig", CACHE_CONFIG);
            System.setProperty("coherence.benchmark.daemonpool", isVirtualPoolMode() ? "virtual" : "platform");
            System.setProperty("coherence.cluster", "dpb-" + clusterModeTag() + '-' + UUID.randomUUID());
            if (isVirtualPoolMode())
                {
                System.setProperty(BENCHMARK_MAILBOX_STATS_PROPERTY, "true");
                }
            else
                {
                System.clearProperty(BENCHMARK_MAILBOX_STATS_PROPERTY);
                }
            System.setProperty("coherence.distributed.localstorage", "true");
            System.setProperty("coherence.localhost", "127.0.0.1");
            System.setProperty("coherence.log.level", "2");
            System.setProperty("coherence.ttl", "0");
            System.setProperty("coherence.wka", "127.0.0.1");

            verifyVirtualThreadRuntime();

            NamedCache<Integer, Integer> cache = CacheFactory.getCache(CACHE_NAME);
            PartitionedService           service =
                    (PartitionedService) ((SafeService) cache.getCacheService()).getRunningService();

            m_cache        = cache;
            m_service      = service;
            m_pool         = service.getDaemonPool();
            m_threadBean   = ManagementFactory.getThreadMXBean();
            m_nHotSetSize  = Math.max(1, Math.min(hotSetSize, cacheSize));
            m_listKeyPairs = buildKeyPairs();
            m_processorSleep = new SleepingProcessor(processorSleepMicros);

            verifySelectedPoolMode();

            preloadCache(cache, Math.max(cacheSize, getKeyPairRange()));
            startMetricsSampler();
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
                stopMetricsSampler();
                try
                    {
                    CacheFactory.shutdown();
                    }
                finally
                    {
                    restoreProperties();
                    }
                }
            }

        NamedCache<Integer, Integer> getCache()
            {
            return m_cache;
            }

        int nextKey()
            {
            return "HOTSET".equals(keyDistribution)
                    ? ThreadLocalRandom.current().nextInt(m_nHotSetSize)
                    : ThreadLocalRandom.current().nextInt(cacheSize);
            }

        Collection<Integer> nextKeyPair()
            {
            return m_listKeyPairs.get(ThreadLocalRandom.current().nextInt(m_listKeyPairs.size()));
            }

        int nextValue()
            {
            return ThreadLocalRandom.current().nextInt();
            }

        SleepingProcessor getSleepingProcessor()
            {
            return m_processorSleep;
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

        private void verifyVirtualThreadRuntime()
            {
            if (!isVirtualPoolMode())
                {
                return;
                }

            VirtualThreadProbe.verifyVirtualThreads("DaemonPoolSmokeBenchmark[vt-check]",
                    "Virtual benchmark mode");
            }

        private void verifySelectedPoolMode()
            {
            if (isPlatformMode())
                {
                return;
                }

            if (!isVirtualPool())
                {
                throw new IllegalStateException("Benchmark poolMode=" + poolMode
                        + " requires VirtualDaemonPool, but selected "
                        + (m_pool == null ? "<null>" : m_pool.getClass().getName()) + '.');
                }
            }

        private void preloadCache(NamedCache<Integer, Integer> cache, int cEntries)
            {
            Map<Integer, Integer> mapBatch = new HashMap<>(BATCH_SIZE);
            for (int i = 0; i < cEntries; i++)
                {
                mapBatch.put(i, i);
                if (mapBatch.size() == BATCH_SIZE)
                    {
                    cache.putAll(mapBatch);
                    mapBatch.clear();
                    }
                }

            if (!mapBatch.isEmpty())
                {
                cache.putAll(mapBatch);
                }
            }

        private List<Collection<Integer>> buildKeyPairs()
            {
            int cRange = getKeyPairRange();
            int cPairs = Math.max(1, Math.min(BATCH_SIZE, cRange));

            List<Collection<Integer>> listPairs = new ArrayList<>(cPairs);
            for (int i = 0; i < cPairs; i++)
                {
                int nKey1 = i % Math.max(1, cRange);
                int nKey2 = cRange > 1 ? (nKey1 + 1) % cRange : nKey1 + 1;
                listPairs.add(List.of(nKey1, nKey2));
                }

            return listPairs;
            }

        private int getKeyPairRange()
            {
            return Math.max(2, "HOTSET".equals(keyDistribution) ? m_nHotSetSize : cacheSize);
            }

        private void logMetrics(String sPhase)
            {
            if (m_pool == null || m_service == null)
                {
                return;
                }

            MetricsSnapshot snapshot = captureMetrics();
            System.out.println(snapshot.format(sPhase));
            }

        private MetricsSnapshot captureMetrics()
            {
            MetricsSnapshot snapshot = new MetricsSnapshot();

            snapshot.poolMode             = poolMode;
            snapshot.keyDistribution      = keyDistribution;
            snapshot.cacheSize            = cacheSize;
            snapshot.processorSleepMicros = processorSleepMicros;
            snapshot.virtualPool          = isVirtualPool();
            snapshot.daemonCount          = m_pool.getDaemonCount();
            snapshot.activeCount          = m_pool.getActiveDaemonCount();
            snapshot.backlog              = m_pool.getBacklog();
            snapshot.maxBacklog           = m_metricsSampler == null ? -1L : m_metricsSampler.getMaxBacklog();
            snapshot.taskCount            = m_pool.getStatsTaskCount();
            snapshot.timeoutCount         = m_pool.getStatsTimeoutCount();
            snapshot.hungCount            = m_pool.getStatsHungCount();
            snapshot.hungDuration         = m_pool.getStatsHungDuration();
            snapshot.hungTaskId           = m_pool.getStatsHungTaskId();
            snapshot.processThreads       = m_threadBean == null ? -1 : m_threadBean.getThreadCount();
            snapshot.rssKb                = readProcessRssKb();
            snapshot.nativeMapCount       = countNativeMappings();
            snapshot.poolThreadCount      = readPoolThreadCount();
            snapshot.mailboxCount         = readMailboxCount();
            snapshot.mailboxBurstDrainers = readPoolAtomicLong("getMailboxBurstDrainerCount");
            snapshot.mailboxBurstTasks    = readPoolAtomicLong("getMailboxBurstTaskCount");
            snapshot.mailboxBurstMax      = readPoolAtomicLong("getMailboxBurstMaxTasks");
            snapshot.mailboxBurstMean     = snapshot.mailboxBurstDrainers <= 0L
                    ? -1.0D
                    : (double) snapshot.mailboxBurstTasks / snapshot.mailboxBurstDrainers;

            return snapshot;
            }

        private void startMetricsSampler()
            {
            m_metricsSampler = new DaemonPoolMetricsSampler(() -> m_pool);
            m_metricsSampler.start();
            }

        private void stopMetricsSampler()
            {
            DaemonPoolMetricsSampler sampler = m_metricsSampler;
            if (sampler != null)
                {
                sampler.stop();
                m_metricsSampler = null;
                }
            }

        private long readPoolAtomicLong(String sMethod)
            {
            if (!isVirtualPool())
                {
                return -1L;
                }

            Object oValue = invokePoolAccessor(sMethod);
            return oValue instanceof AtomicLong ? ((AtomicLong) oValue).get() : -1L;
            }

        private int readMailboxCount()
            {
            if (!isVirtualPool())
                {
                return -1;
                }

            Object oValue = invokePoolAccessor("getKeyedMailboxes");
            if (oValue instanceof Map)
                {
                return ((Map<?, ?>) oValue).size();
                }
            return -1;
            }

        private int readPoolThreadCount()
            {
            if (!isVirtualPool())
                {
                return -1;
                }

            Object oValue = invokePoolAccessor("getThreads");
            if (oValue instanceof java.util.Set)
                {
                return ((java.util.Set<?>) oValue).size();
                }
            return -1;
            }

        private Object invokePoolAccessor(String sMethod)
            {
            try
                {
                Method method = m_pool.getClass().getSuperclass().getSuperclass().getDeclaredMethod(sMethod);
                method.setAccessible(true);
                return method.invoke(m_pool);
                }
            catch (ReflectiveOperationException e)
                {
                return null;
                }
            }

        private boolean isVirtualPool()
            {
            return m_pool != null
                    && m_pool.getClass().getName().endsWith("$VirtualDaemonPool");
            }

        private long readProcessRssKb()
            {
            long lPid = ProcessHandle.current().pid();

            ProcessBuilder builder = new ProcessBuilder("ps", "-o", "rss=", "-p", Long.toString(lPid));
            builder.redirectErrorStream(true);

            try
                {
                Process process = builder.start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
                    {
                    String sLine = reader.readLine();
                    process.waitFor(5, TimeUnit.SECONDS);

                    if (sLine != null)
                        {
                        return Long.parseLong(sLine.trim());
                        }
                    }
                }
            catch (IOException | InterruptedException | NumberFormatException e)
                {
                if (e instanceof InterruptedException)
                    {
                    Thread.currentThread().interrupt();
                    }
                }

            return -1L;
            }

        private long countNativeMappings()
            {
            Path pathMaps = Path.of("/proc/self/maps");
            if (!Files.isReadable(pathMaps))
                {
                return -1L;
                }

            try (java.util.stream.Stream<String> stream = Files.lines(pathMaps))
                {
                return stream.count();
                }
            catch (IOException e)
                {
                return -1L;
                }
            }

        private void restoreProperties()
            {
            if (m_mapPreviousProperties == null)
                {
                return;
                }

            BenchmarkProperties.restore(m_mapPreviousProperties);
            }

        private NamedCache<Integer, Integer> m_cache;
        private PartitionedService           m_service;
        private DaemonPool                   m_pool;
        private ThreadMXBean                 m_threadBean;
        private DaemonPoolMetricsSampler     m_metricsSampler;
        private int                          m_nHotSetSize;
        private List<Collection<Integer>>    m_listKeyPairs;
        private SleepingProcessor            m_processorSleep;
        private Map<String, String>          m_mapPreviousProperties;

        private static final int    BATCH_SIZE                         = 1024;
        private static final String BENCHMARK_MAILBOX_STATS_PROPERTY   =
                "coherence.daemonpool.virtual.benchmark.mailboxStats";
        }

    // ----- inner class: no-op processor ----------------------------------

    /**
     * Minimal service-side workload used to validate benchmark wiring.
     */
    public static class NoOpProcessor
            extends AbstractProcessor<Integer, Integer, Integer>
            implements Serializable
        {
        @Override
        public Integer process(InvocableMap.Entry<Integer, Integer> entry)
            {
            Integer nValue = entry.getValue();
            return nValue == null ? -1 : nValue;
            }

        public static final NoOpProcessor INSTANCE = new NoOpProcessor();
        }

    // ----- inner class: sleeping processor -------------------------------

    /**
     * Minimal blocking service-side workload used to put pressure on daemon
     * pool scheduling without adding application work.
     */
    public static class SleepingProcessor
            extends AbstractProcessor<Integer, Integer, Integer>
            implements Serializable
        {
        public SleepingProcessor(int cMicros)
            {
            m_cNanos = TimeUnit.MICROSECONDS.toNanos(Math.max(0, cMicros));
            }

        @Override
        public Integer process(InvocableMap.Entry<Integer, Integer> entry)
            {
            long cNanos = m_cNanos;
            if (cNanos > 0L)
                {
                java.util.concurrent.locks.LockSupport.parkNanos(cNanos);
                }

            Integer nValue = entry.getValue();
            return nValue == null ? -1 : nValue;
            }

        private final long m_cNanos;
        }

    // ----- inner class: metrics snapshot ---------------------------------

    /**
     * Lightweight benchmark-side snapshot of service and process state.
     */
    protected static class MetricsSnapshot
        {
        String format(String sPhase)
            {
            return "DaemonPoolSmokeBenchmark[" + sPhase + "]"
                + " poolMode=" + poolMode
                + ", keyDistribution=" + keyDistribution
                + ", cacheSize=" + cacheSize
                + ", processorSleepMicros=" + processorSleepMicros
                + ", virtualPool=" + virtualPool
                + ", daemonCount=" + daemonCount
                + ", activeCount=" + activeCount
                + ", backlog=" + backlog
                + ", maxBacklog=" + maxBacklog
                + ", taskCount=" + taskCount
                + ", timeoutCount=" + timeoutCount
                + ", hungCount=" + hungCount
                + ", hungDuration=" + hungDuration
                + ", hungTaskId=" + (hungTaskId == null || hungTaskId.isEmpty() ? "<none>" : hungTaskId)
                + ", poolThreadCount=" + poolThreadCount
                + ", mailboxCount=" + mailboxCount
                + ", mailboxBurstDrainers=" + mailboxBurstDrainers
                + ", mailboxBurstTasks=" + mailboxBurstTasks
                + ", mailboxBurstMean=" + mailboxBurstMean
                + ", mailboxBurstMax=" + mailboxBurstMax
                + ", processThreads=" + processThreads
                + ", rssKb=" + rssKb
                + ", nativeMapCount=" + nativeMapCount;
            }

        private String  poolMode;
        private String  keyDistribution;
        private int     cacheSize;
        private int     processorSleepMicros;
        private boolean virtualPool;
        private int     daemonCount;
        private int     activeCount;
        private int     backlog;
        private long    maxBacklog;
        private long    taskCount;
        private int     timeoutCount;
        private int     hungCount;
        private long    hungDuration;
        private String  hungTaskId;
        private int     poolThreadCount;
        private int     mailboxCount;
        private long    mailboxBurstDrainers;
        private long    mailboxBurstTasks;
        private double  mailboxBurstMean;
        private long    mailboxBurstMax;
        private int     processThreads;
        private long    rssKb;
        private long    nativeMapCount;
        }

    // ----- constants ------------------------------------------------------

    protected static final String CACHE_CONFIG = "daemonpool-benchmark-cache-config.xml";

    protected static final String CACHE_NAME = "benchmark-daemonpool";
    }
