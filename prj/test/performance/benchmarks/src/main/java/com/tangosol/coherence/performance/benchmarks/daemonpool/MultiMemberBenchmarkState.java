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
import com.tangosol.coherence.performance.benchmarks.daemonpool.common.JfrPinnedThreadParser;
import com.tangosol.coherence.performance.benchmarks.daemonpool.common.JfrPinnedThreadParser.PinSummary;
import com.tangosol.coherence.performance.benchmarks.daemonpool.common.PinnedClassPreTouch;
import com.tangosol.coherence.performance.benchmarks.daemonpool.common.VirtualThreadProbe;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.runner.IterationType;

import java.io.IOException;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;

import java.nio.file.Files;
import java.nio.file.Path;

import java.net.ServerSocket;

import java.text.ParseException;

import java.time.Duration;
import java.time.Instant;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;

/**
 * Shared trial state for multi-member daemon-pool benchmarks.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
@State(Scope.Benchmark)
public abstract class MultiMemberBenchmarkState
    {
    @Param({"platform", "virtual"})
    public String poolMode;

    @Param({"UNIFORM"})
    public String keyDistribution;

    @Param({"cluster-3"})
    public String topology;

    @Param({"10000"})
    public int cacheSize;

    @Param({"1"})
    public int backupCount;

    @Param({"active"})
    public String persistenceMode;

    @Param({"3"})
    public int storageMembers;

    @Param({"10"})
    public int startupWaitSeconds;

    @Param({"60"})
    public int startupTimeoutSeconds;

    @Param({"true"})
    public boolean jfr;

    @Param({"test/performance/benchmarks/src/main/resources/daemonpool-tight-profile.jfc"})
    public String jfrSettings;

    @Param({"false"})
    public boolean preTouchPinnedClasses;

    protected void setupCluster(String sCacheName, boolean fPersistence)
            throws Exception
        {
        m_mapPreviousProperties = BenchmarkProperties.capture(
                "benchmark.daemonpool.backup.count",
                "benchmark.daemonpool.client.transport",
                "benchmark.daemonpool.grpc.address",
                "benchmark.daemonpool.grpc.port",
                "benchmark.daemonpool.log.level",
                "benchmark.daemonpool.member.name",
                "benchmark.daemonpool.name.service.address",
                "benchmark.daemonpool.name.service.port",
                "benchmark.daemonpool.persistence.active.dir",
                "benchmark.daemonpool.persistence.autostart",
                "benchmark.daemonpool.persistence.backup.dir",
                "benchmark.daemonpool.persistence.mode",
                "benchmark.daemonpool.persistence.snapshot.dir",
                "benchmark.daemonpool.persistence.trash.dir",
                "coherence.cacheconfig",
                "coherence.benchmark.daemonpool",
                "coherence.cluster",
                "coherence.daemonpool.virtual.benchmark.mailboxStats",
                "coherence.distributed.localstorage",
                "coherence.grpc.enabled",
                "coherence.grpc.server.address",
                "coherence.grpc.server.port",
                "coherence.localhost",
                "coherence.log.level",
                "coherence.override",
                "coherence.proxy.enabled",
                "coherence.ttl",
                "coherence.wka");

        CacheFactory.shutdown();

        m_nGrpcPort = isGrpcTopology() ? allocatePort() : 0;

        m_sClusterName = "dpb-mm-" + topology + '-' + clusterModeTag() + '-' + UUID.randomUUID();
        m_pathOutputDirectory = Path.of("target", "benchmark-jfr", topology,
                benchmarkFamily(), sCacheName, poolMode, m_sClusterName);
        Files.createDirectories(m_pathOutputDirectory);

        configureDriverProperties(fPersistence);
        if (preTouchPinnedClasses)
            {
            PinnedClassPreTouch.preTouch();
            }

        m_instantTrialStart = Instant.now();
        if (isVirtualMode())
            {
            VirtualThreadProbe.verifyVirtualThreads("MultiMemberBenchmark[driver-vt-check]",
                    "Multi-member virtual daemon-pool driver");
            }

        try
            {
            startDriverRecording();
            m_launcher = new MultiMemberBenchmarkLauncher();
            m_launcher.launch(MultiMemberBenchmarkLauncher.LaunchConfig.fromArgs(launchArguments(fPersistence)));

            NamedCache<Integer, Integer> cache = CacheFactory.getCache(sCacheName);
            PartitionedService service = getPartitionedService(cache);

            m_cache      = cache;
            m_service    = service;
            m_pool       = service == null ? null : service.getDaemonPool();
            m_threadBean = ManagementFactory.getThreadMXBean();

            verifySelectedPoolMode();
            preloadCache(cache, cacheSize);
            startMetricsSampler();
            m_cTaskCountStart = m_pool == null ? 0L : m_pool.getStatsTaskCount();
            logMetrics("trial-start");
            }
        catch (Throwable t)
            {
            cleanupAfterFailedSetup(t);
            throw t;
            }
        }

    @TearDown(Level.Iteration)
    public void tearDownIteration(IterationParams iterationParams)
        {
        if (iterationParams.getType() == IterationType.WARMUP)
            {
            m_instantWarmupEnd = Instant.now();
            }
        }

    protected void tearDownCluster()
            throws Exception
        {
        Exception exception = null;
        m_instantMeasurementEnd = Instant.now();
        if (m_instantWarmupEnd == null)
            {
            m_instantWarmupEnd = m_instantTrialStart;
            }

        writeWindowFile();
        if (m_launcher != null)
            {
            m_launcher.setMeasurementWindow(m_instantWarmupEnd, m_instantMeasurementEnd);
            }

        try
            {
            logMetrics("trial-end");
            }
        catch (Exception e)
            {
            exception = e;
            }

        try
            {
            stopMetricsSampler();
            }
        catch (Exception e)
            {
            exception = addSuppressed(exception, e);
            }

        try
            {
            stopDriverRecording();
            }
        catch (Exception e)
            {
            exception = addSuppressed(exception, e);
            }

        try
            {
            CacheFactory.shutdown();
            }
        catch (Exception e)
            {
            exception = addSuppressed(exception, e);
            }

        try
            {
            if (m_launcher != null)
                {
                m_launcher.close();
                }
            }
        catch (Exception e)
            {
            exception = addSuppressed(exception, e);
            }

        BenchmarkProperties.restore(m_mapPreviousProperties);

        if (exception != null)
            {
            throw exception;
            }
        }

    protected NamedCache<Integer, Integer> getCache()
        {
        return m_cache;
        }

    protected int nextKey()
        {
        return ThreadLocalRandom.current().nextInt(cacheSize);
        }

    protected int nextValue()
        {
        return ThreadLocalRandom.current().nextInt();
        }

    protected String benchmarkFamily()
        {
        return getClass().getEnclosingClass() == null
                ? getClass().getSimpleName()
                : getClass().getEnclosingClass().getSimpleName();
        }

    private void configureDriverProperties(boolean fPersistence)
        {
        Path pathPersistence = m_pathOutputDirectory.resolve("driver").resolve("persistence");

        System.setProperty("benchmark.daemonpool.backup.count", Integer.toString(backupCount));
        System.setProperty("benchmark.daemonpool.client.transport", clientTransport());
        System.setProperty("benchmark.daemonpool.grpc.address", "127.0.0.1");
        System.setProperty("benchmark.daemonpool.grpc.port", Integer.toString(m_nGrpcPort));
        System.setProperty("benchmark.daemonpool.member.name", "driver");
        System.setProperty("benchmark.daemonpool.name.service.address", "127.0.0.1");
        System.setProperty("benchmark.daemonpool.name.service.port", "7574");
        System.setProperty("benchmark.daemonpool.persistence.active.dir",
                pathPersistence.resolve("active").toString());
        System.setProperty("benchmark.daemonpool.persistence.autostart", Boolean.toString(fPersistence));
        System.setProperty("benchmark.daemonpool.persistence.backup.dir",
                pathPersistence.resolve("backup").toString());
        System.setProperty("benchmark.daemonpool.persistence.mode", persistenceMode);
        System.setProperty("benchmark.daemonpool.persistence.snapshot.dir",
                pathPersistence.resolve("snapshot").toString());
        System.setProperty("benchmark.daemonpool.persistence.trash.dir",
                pathPersistence.resolve("trash").toString());
        System.setProperty("coherence.cacheconfig", driverCacheConfig());
        System.setProperty("coherence.benchmark.daemonpool", isVirtualMode() ? "virtual" : "platform");
        System.setProperty("coherence.cluster", m_sClusterName);
        if (isVirtualMode())
            {
            System.setProperty("coherence.daemonpool.virtual.benchmark.mailboxStats", "true");
            }
        else
            {
            System.clearProperty("coherence.daemonpool.virtual.benchmark.mailboxStats");
            }
        System.setProperty("coherence.distributed.localstorage", Boolean.toString(isDriverStorageEnabled()));
        System.setProperty("coherence.grpc.enabled", "false");
        System.setProperty("coherence.grpc.server.address", "127.0.0.1");
        System.setProperty("coherence.grpc.server.port", "1408");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.log.level", benchmarkLogLevel());
        System.setProperty("coherence.override", OVERRIDE_CONFIG);
        System.setProperty("coherence.proxy.enabled", "false");
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.wka", "127.0.0.1");
        }

    private String[] launchArguments(boolean fPersistence)
        {
        return new String[]
            {
            "cluster=" + m_sClusterName,
            "poolMode=" + poolMode,
            "storageMembers=" + storageMembers,
            "cacheConfig=" + SERVER_CACHE_CONFIG,
            "overrideConfig=" + OVERRIDE_CONFIG,
            "outputDir=" + m_pathOutputDirectory,
            "jfr=" + jfr,
            "jfrSettings=" + jfrSettings,
            "backupCount=" + backupCount,
            "persistenceMode=" + persistenceMode,
            "persistenceAutostart=" + fPersistence,
            "preTouchPinnedClasses=" + preTouchPinnedClasses,
            "extendProxy=" + isExtendTopology(),
            "grpcProxy=" + isGrpcTopology(),
            "grpcPort=" + m_nGrpcPort,
            "logLevel=" + benchmarkLogLevel(),
            "startupWaitSeconds=" + startupWaitSeconds,
            "startupTimeoutSeconds=" + startupTimeoutSeconds
            };
        }

    private void verifySelectedPoolMode()
        {
        if (!isVirtualMode())
            {
            return;
            }

        if (isRemoteClientTopology())
            {
            return;
            }

        if (!isVirtualPool())
            {
            throw new IllegalStateException("Multi-member benchmark poolMode=" + poolMode
                    + " requires VirtualDaemonPool, but selected "
                    + (m_pool == null ? "<null>" : m_pool.getClass().getName()) + '.');
            }
        }

    private boolean isVirtualMode()
        {
        return "virtual".equalsIgnoreCase(poolMode);
        }

    private boolean isRemoteClientTopology()
        {
        return isExtendTopology() || isGrpcTopology();
        }

    private boolean isExtendTopology()
        {
        return "extend".equalsIgnoreCase(topology);
        }

    private boolean isGrpcTopology()
        {
        return "grpc".equalsIgnoreCase(topology);
        }

    private boolean isDriverStorageEnabled()
        {
        return "cluster-3".equalsIgnoreCase(topology);
        }

    private String clientTransport()
        {
        return isGrpcTopology() ? "grpc" : "extend";
        }

    private String driverCacheConfig()
        {
        return isRemoteClientTopology() ? CLIENT_CACHE_CONFIG : SERVER_CACHE_CONFIG;
        }

    private String benchmarkLogLevel()
        {
        return System.getProperty("benchmark.daemonpool.log.level", "2");
        }

    private String clusterModeTag()
        {
        return isVirtualMode() ? "v" : "p";
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

    private void startMetricsSampler()
        {
        if (m_pool == null)
            {
            return;
            }

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

    private void startDriverRecording()
            throws IOException, ParseException
        {
        if (!jfr)
            {
            return;
            }

        Path pathJfr = m_pathOutputDirectory.resolve("driver.jfr");
        Configuration configuration = Configuration.create(Path.of(jfrSettings));
        Recording recording = new Recording(configuration);
        recording.setName("trial-driver");
        recording.setToDisk(true);
        recording.setMaxAge(Duration.ofMinutes(10));
        recording.setMaxSize(512L * 1024L * 1024L);
        recording.start();

        m_recordingDriver = recording;
        m_pathDriverJfr = pathJfr;
        }

    private void stopDriverRecording()
            throws IOException
        {
        Recording recording = m_recordingDriver;
        if (recording == null)
            {
            return;
            }

        try
            {
            recording.dump(m_pathDriverJfr);
            recording.stop();
            checkDriverJfrPins();
            }
        finally
            {
            recording.close();
            m_recordingDriver = null;
            }
        }

    private void checkDriverJfrPins()
            throws IOException
        {
        PinSummary summary = JfrPinnedThreadParser.summarize(m_pathDriverJfr,
                m_instantWarmupEnd, m_instantMeasurementEnd);
        if (summary.getTotalPins() > 0)
            {
            System.out.println("driver " + summary.formatSummary());
            }

        if (summary.hasMeasuredPins())
            {
            throw new IllegalStateException("Measured-window pinned virtual-thread event detected in driver JFR ("
                    + m_pathDriverJfr + "):" + System.lineSeparator()
                    + summary.formatFailure());
            }
        }

    private void writeWindowFile()
            throws IOException
        {
        Path pathWindow = m_pathOutputDirectory.resolve("trial.window.json");
        String sJson = "{\n"
                + "  \"warmupEnd\": \"" + m_instantWarmupEnd + "\",\n"
                + "  \"measurementEnd\": \"" + m_instantMeasurementEnd + "\",\n"
                + "  \"preTouchPinnedClasses\": " + preTouchPinnedClasses + "\n"
                + "}\n";
        Files.writeString(pathWindow, sJson);
        }

    private void logMetrics(String sPhase)
        {
        if (m_pool == null || m_service == null)
            {
            System.out.println("MultiMemberBenchmark[" + sPhase + "]"
                    + " family=" + benchmarkFamily()
                    + ", topology=" + topology
                    + ", poolMode=" + poolMode
                    + ", cacheSize=" + cacheSize
                    + ", backupCount=" + backupCount
                    + ", persistenceMode=" + persistenceMode
                    + ", storageMembers=" + storageMembers
                    + ", driverService="
                    + (m_cache == null || m_cache.getCacheService() == null
                            ? "<none>" : m_cache.getCacheService().getInfo().getServiceName())
                    + ", outputDir=" + m_pathOutputDirectory);
            return;
            }

        long cTask = m_pool.getStatsTaskCount();
        System.out.println("MultiMemberBenchmark[" + sPhase + "]"
                + " family=" + benchmarkFamily()
                + ", topology=" + topology
                + ", poolMode=" + poolMode
                + ", cacheSize=" + cacheSize
                + ", backupCount=" + backupCount
                + ", persistenceMode=" + persistenceMode
                + ", storageMembers=" + storageMembers
                + ", virtualPool=" + isVirtualPool()
                + ", daemonCount=" + m_pool.getDaemonCount()
                + ", activeCount=" + m_pool.getActiveDaemonCount()
                + ", backlog=" + m_pool.getBacklog()
                + ", maxBacklog=" + (m_metricsSampler == null ? -1L : m_metricsSampler.getMaxBacklog())
                + ", taskCount=" + cTask
                + ", taskCountDelta=" + (cTask - m_cTaskCountStart)
                + ", timeoutCount=" + m_pool.getStatsTimeoutCount()
                + ", hungCount=" + m_pool.getStatsHungCount()
                + ", processThreads=" + (m_threadBean == null ? -1 : m_threadBean.getThreadCount())
                + ", poolThreadCount=" + readPoolThreadCount()
                + ", mailboxCount=" + readMailboxCount()
                + ", mailboxBurstDrainers=" + readPoolAtomicLong("getMailboxBurstDrainerCount")
                + ", mailboxBurstTasks=" + readPoolAtomicLong("getMailboxBurstTaskCount")
                + ", mailboxBurstMean=" + readMailboxBurstMean()
                + ", mailboxBurstMax=" + readPoolAtomicLong("getMailboxBurstMaxTasks")
                + ", outputDir=" + m_pathOutputDirectory);
        }

    private double readMailboxBurstMean()
        {
        long cDrainers = readPoolAtomicLong("getMailboxBurstDrainerCount");
        long cTasks    = readPoolAtomicLong("getMailboxBurstTaskCount");
        return cDrainers <= 0L ? -1.0D : (double) cTasks / cDrainers;
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
        return oValue instanceof Map ? ((Map<?, ?>) oValue).size() : -1;
        }

    private int readPoolThreadCount()
        {
        if (!isVirtualPool())
            {
            return -1;
            }

        Object oValue = invokePoolAccessor("getThreads");
        return oValue instanceof java.util.Set ? ((java.util.Set<?>) oValue).size() : -1;
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

    private PartitionedService getPartitionedService(NamedCache<Integer, Integer> cache)
        {
        if (cache.getCacheService() instanceof SafeService)
            {
            Object service = ((SafeService) cache.getCacheService()).getRunningService();
            if (service instanceof PartitionedService)
                {
                return (PartitionedService) service;
                }
            }
        return null;
        }

    private int allocatePort()
            throws IOException
        {
        try (ServerSocket socket = new ServerSocket(0))
            {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
            }
        }

    private Exception addSuppressed(Exception exception, Exception e)
        {
        if (exception == null)
            {
            return e;
            }
        exception.addSuppressed(e);
        return exception;
        }

    private void cleanupAfterFailedSetup(Throwable throwable)
        {
        try
            {
            stopMetricsSampler();
            }
        catch (Throwable t)
            {
            throwable.addSuppressed(t);
            }

        try
            {
            Recording recording = m_recordingDriver;
            if (recording != null)
                {
                recording.close();
                m_recordingDriver = null;
                }
            }
        catch (Throwable t)
            {
            throwable.addSuppressed(t);
            }

        try
            {
            CacheFactory.shutdown();
            }
        catch (Throwable t)
            {
            throwable.addSuppressed(t);
            }

        try
            {
            if (m_launcher != null)
                {
                m_launcher.close();
                m_launcher = null;
                }
            }
        catch (Throwable t)
            {
            throwable.addSuppressed(t);
            }

        try
            {
            BenchmarkProperties.restore(m_mapPreviousProperties);
            }
        catch (Throwable t)
            {
            throwable.addSuppressed(t);
            }
        }

    private NamedCache<Integer, Integer>       m_cache;
    private PartitionedService                 m_service;
    private DaemonPool                         m_pool;
    private ThreadMXBean                       m_threadBean;
    private DaemonPoolMetricsSampler           m_metricsSampler;
    private MultiMemberBenchmarkLauncher       m_launcher;
    private Recording                          m_recordingDriver;
    private Path                               m_pathDriverJfr;
    private Path                               m_pathOutputDirectory;
    private String                             m_sClusterName;
    private Map<String, String>                m_mapPreviousProperties;
    private long                               m_cTaskCountStart;
    private int                                m_nGrpcPort;
    private Instant                            m_instantTrialStart;
    private Instant                            m_instantWarmupEnd;
    private Instant                            m_instantMeasurementEnd;

    private static final int    BATCH_SIZE          = 1024;
    private static final String SERVER_CACHE_CONFIG = "daemonpool-multi-member-cache-config.xml";
    private static final String CLIENT_CACHE_CONFIG = "daemonpool-multi-member-client-cache-config.xml";
    private static final String OVERRIDE_CONFIG     = "daemonpool-multi-member-coherence-override.xml";
    }
