/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool;

import com.tangosol.coherence.performance.benchmarks.daemonpool.common.JfrPinnedThreadParser;
import com.tangosol.coherence.performance.benchmarks.daemonpool.common.JfrPinnedThreadParser.PinSummary;
import com.tangosol.coherence.performance.benchmarks.daemonpool.common.PinnedThreadParser;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.time.Duration;
import java.time.Instant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Process launcher scaffold for multi-member daemon-pool benchmarks.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
public class MultiMemberBenchmarkLauncher
        implements AutoCloseable
    {
    public static void main(String[] asArg)
            throws Exception
        {
        LaunchConfig config = LaunchConfig.fromArgs(asArg);
        try (MultiMemberBenchmarkLauncher launcher = new MultiMemberBenchmarkLauncher())
            {
            Trial trial = launcher.launch(config);
            System.out.println("MultiMemberBenchmarkLauncher launched " + trial.getMembers().size()
                    + " member(s) for cluster " + config.getClusterName()
                    + "; outputDir=" + config.getOutputDirectory());
            if (!config.getHoldDuration().isZero())
                {
                Thread.sleep(config.getHoldDuration().toMillis());
                }
            }
        }

    public Trial launch(LaunchConfig config)
            throws IOException, InterruptedException
        {
        Objects.requireNonNull(config, "config");
        Files.createDirectories(config.getOutputDirectory());

        List<MemberProcess> listMembers = new ArrayList<>();
        for (int i = 0; i < config.getStorageMemberCount(); i++)
            {
            listMembers.add(startMember(config, "storage-" + (i + 1), true,
                    config.isExtendProxyEnabled(),
                    i == 0 && config.isGrpcProxyEnabled()));
            }

        Trial trial = new Trial(config, listMembers);
        m_listTrials.add(trial);
        awaitClusterFormation(trial);
        return trial;
        }

    public void setMeasurementWindow(Instant instantStart, Instant instantEnd)
        {
        for (Trial trial : m_listTrials)
            {
            trial.setMeasurementWindow(instantStart, instantEnd);
            }
        }

    @Override
    public void close()
            throws Exception
        {
        Exception exception = null;
        for (Trial trial : List.copyOf(m_listTrials))
            {
            try
                {
                trial.close();
                }
            catch (Exception e)
                {
                if (exception == null)
                    {
                    exception = e;
                    }
                else
                    {
                    exception.addSuppressed(e);
                    }
                }
            }
        m_listTrials.clear();

        if (exception != null)
            {
            throw exception;
            }
        }

    private MemberProcess startMember(LaunchConfig config, String sMemberName, boolean fStorage,
            boolean fExtendProxy, boolean fGrpcProxy)
            throws IOException
        {
        Path pathLog = config.getOutputDirectory().resolve(sMemberName + ".log");
        Path pathJfr = config.getOutputDirectory().resolve(sMemberName + ".jfr");
        Path pathPersistence = config.getOutputDirectory().resolve(sMemberName).resolve("persistence");
        Files.createDirectories(pathLog.getParent());

        List<String> listCommand = new ArrayList<>();
        listCommand.add(javaExecutable("java").toString());
        listCommand.add("-Djdk.tracePinnedThreads=full");
        listCommand.add("-Dcoherence.cluster=" + config.getClusterName());
        listCommand.add("-Dcoherence.cacheconfig=" + config.getCacheConfig());
        listCommand.add("-Dcoherence.override=" + config.getOverrideConfig());
        listCommand.add("-Dcoherence.benchmark.daemonpool=" + (config.isVirtualPool() ? "virtual" : "platform"));
        if (config.isVirtualPool())
            {
            listCommand.add("-Dcoherence.daemonpool.virtual.benchmark.mailboxStats=true");
            }
        listCommand.add("-Dcoherence.distributed.localstorage=" + fStorage);
        listCommand.add("-Dcoherence.localhost=127.0.0.1");
        listCommand.add("-Dcoherence.log.level=" + config.getLogLevel());
        listCommand.add("-Dcoherence.wka=127.0.0.1");
        listCommand.add("-Dcoherence.ttl=0");
        listCommand.add("-Dbenchmark.daemonpool.backup.count=" + config.getBackupCount());
        listCommand.add("-Dbenchmark.daemonpool.persistence.mode=" + config.getPersistenceMode());
        listCommand.add("-Dbenchmark.daemonpool.persistence.autostart=" + config.isPersistenceAutostart());
        listCommand.add("-Dbenchmark.daemonpool.persistence.active.dir=" + pathPersistence.resolve("active"));
        listCommand.add("-Dbenchmark.daemonpool.persistence.backup.dir=" + pathPersistence.resolve("backup"));
        listCommand.add("-Dbenchmark.daemonpool.persistence.snapshot.dir=" + pathPersistence.resolve("snapshot"));
        listCommand.add("-Dbenchmark.daemonpool.persistence.trash.dir=" + pathPersistence.resolve("trash"));
        listCommand.add("-Dbenchmark.daemonpool.pinned.preTouch=" + config.isPreTouchPinnedClasses());
        listCommand.add("-Dcoherence.proxy.enabled=" + fExtendProxy);
        listCommand.add("-Dcoherence.grpc.enabled=" + fGrpcProxy);
        listCommand.add("-Dcoherence.grpc.server.address=127.0.0.1");
        listCommand.add("-Dcoherence.grpc.server.port=" + config.getGrpcPort());
        listCommand.add("-Dbenchmark.daemonpool.member.name=" + sMemberName);
        if (config.isJfrEnabled())
            {
            listCommand.add(startFlightRecordingArgument(config, pathJfr));
            }
        listCommand.add("-cp");
        listCommand.add(config.getClassPath());
        listCommand.add(MultiMemberCacheServer.class.getName());

        ProcessBuilder builder = new ProcessBuilder(listCommand);
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.to(pathLog.toFile()));

        Process process = builder.start();
        return new MemberProcess(sMemberName, process, pathLog, pathJfr, config.isJfrEnabled());
        }

    private String startFlightRecordingArgument(LaunchConfig config, Path pathJfr)
        {
        String sArg = "-XX:StartFlightRecording=name=trial,filename=" + pathJfr
                + ",disk=true,maxage=10m,maxsize=512m";
        Path pathSettings = config.getJfrSettings();
        if (pathSettings != null)
            {
            sArg += ",settings=" + pathSettings;
            }
        return sArg;
        }

    private void awaitClusterFormation(Trial trial)
            throws InterruptedException
        {
        long ldtStart = System.nanoTime();
        long cWaitNanos = trial.getConfig().getStartupWait().toNanos();
        long cTimeoutNanos = trial.getConfig().getStartupTimeout().toNanos();
        do
            {
            for (MemberProcess member : trial.getMembers())
                {
                if (!member.isAlive())
                    {
                    throw new IllegalStateException("Launched member " + member.getName()
                            + " exited before cluster startup completed; see " + member.getLog());
                    }
                }

            if (System.nanoTime() - ldtStart >= cWaitNanos)
                {
                return;
                }
            Thread.sleep(250L);
            }
        while (System.nanoTime() - ldtStart < cTimeoutNanos);

        throw new IllegalStateException("Timed out waiting for launched cluster members to stay alive.");
        }

    private static Path javaExecutable(String sName)
        {
        Path pathJavaHome = Path.of(System.getProperty("java.home"));
        return pathJavaHome.resolve("bin").resolve(sName);
        }

    // ----- inner class: LaunchConfig -------------------------------------

    public static class LaunchConfig
        {
        public static LaunchConfig fromArgs(String[] asArg)
            {
            Map<String, String> map = new LinkedHashMap<>();
            for (String sArg : asArg)
                {
                int of = sArg.indexOf('=');
                if (of > 0)
                    {
                    map.put(sArg.substring(0, of), sArg.substring(of + 1));
                    }
                }

            LaunchConfig config = new LaunchConfig();
            config.m_sClusterName = map.getOrDefault("cluster",
                    "dpb-mm-" + UUID.randomUUID());
            config.m_sPoolMode = map.getOrDefault("poolMode", "platform");
            config.m_cStorageMembers = Integer.parseInt(map.getOrDefault("storageMembers", "3"));
            config.m_pathOutputDirectory = Path.of(map.getOrDefault("outputDir",
                    "target/benchmark-jfr/harness/" + config.m_sClusterName));
            config.m_sCacheConfig = map.getOrDefault("cacheConfig", DEFAULT_CACHE_CONFIG);
            config.m_sOverrideConfig = map.getOrDefault("overrideConfig", DEFAULT_OVERRIDE_CONFIG);
            config.m_sClassPath = map.getOrDefault("classPath", System.getProperty("java.class.path"));
            config.m_pathJfrSettings = map.containsKey("jfrSettings")
                    ? Path.of(map.get("jfrSettings"))
                    : Path.of("test/performance/benchmarks/src/main/resources/daemonpool-tight-profile.jfc");
            config.m_fJfrEnabled = Boolean.parseBoolean(map.getOrDefault("jfr", "true"));
            config.m_cBackupCount = Integer.parseInt(map.getOrDefault("backupCount", "1"));
            config.m_sPersistenceMode = map.getOrDefault("persistenceMode", "active");
            config.m_fPersistenceAutostart = Boolean.parseBoolean(
                    map.getOrDefault("persistenceAutostart", "false"));
            config.m_fPreTouchPinnedClasses = Boolean.parseBoolean(
                    map.getOrDefault("preTouchPinnedClasses", "false"));
            config.m_fExtendProxyEnabled = Boolean.parseBoolean(map.getOrDefault("extendProxy", "false"));
            config.m_fGrpcProxyEnabled = Boolean.parseBoolean(map.getOrDefault("grpcProxy", "false"));
            config.m_nGrpcPort = Integer.parseInt(map.getOrDefault("grpcPort", "1408"));
            config.m_sLogLevel = map.getOrDefault("logLevel", "2");
            config.m_durationStartupWait = Duration.ofSeconds(Long.parseLong(map.getOrDefault("startupWaitSeconds", "10")));
            config.m_durationStartupTimeout = Duration.ofSeconds(Long.parseLong(map.getOrDefault("startupTimeoutSeconds", "60")));
            config.m_durationHold = Duration.ofSeconds(Long.parseLong(map.getOrDefault("holdSeconds", "0")));
            return config;
            }

        public String getClusterName()
            {
            return m_sClusterName;
            }

        public String getPoolMode()
            {
            return m_sPoolMode;
            }

        public boolean isVirtualPool()
            {
            return "virtual".equalsIgnoreCase(m_sPoolMode);
            }

        public int getStorageMemberCount()
            {
            return m_cStorageMembers;
            }

        public Path getOutputDirectory()
            {
            return m_pathOutputDirectory;
            }

        public String getCacheConfig()
            {
            return m_sCacheConfig;
            }

        public String getOverrideConfig()
            {
            return m_sOverrideConfig;
            }

        public String getClassPath()
            {
            return m_sClassPath;
            }

        public Path getJfrSettings()
            {
            return m_pathJfrSettings;
            }

        public boolean isJfrEnabled()
            {
            return m_fJfrEnabled;
            }

        public int getBackupCount()
            {
            return m_cBackupCount;
            }

        public String getPersistenceMode()
            {
            return m_sPersistenceMode;
            }

        public boolean isPersistenceAutostart()
            {
            return m_fPersistenceAutostart;
            }

        public boolean isPreTouchPinnedClasses()
            {
            return m_fPreTouchPinnedClasses;
            }

        public boolean isExtendProxyEnabled()
            {
            return m_fExtendProxyEnabled;
            }

        public boolean isGrpcProxyEnabled()
            {
            return m_fGrpcProxyEnabled;
            }

        public int getGrpcPort()
            {
            return m_nGrpcPort;
            }

        public String getLogLevel()
            {
            return m_sLogLevel;
            }

        public Duration getStartupWait()
            {
            return m_durationStartupWait;
            }

        public Duration getStartupTimeout()
            {
            return m_durationStartupTimeout;
            }

        public Duration getHoldDuration()
            {
            return m_durationHold;
            }

        private String   m_sClusterName;
        private String   m_sPoolMode;
        private int      m_cStorageMembers;
        private Path     m_pathOutputDirectory;
        private String   m_sCacheConfig;
        private String   m_sOverrideConfig;
        private String   m_sClassPath;
        private Path     m_pathJfrSettings;
        private boolean  m_fJfrEnabled;
        private int      m_cBackupCount;
        private String   m_sPersistenceMode;
        private boolean  m_fPersistenceAutostart;
        private boolean  m_fPreTouchPinnedClasses;
        private boolean  m_fExtendProxyEnabled;
        private boolean  m_fGrpcProxyEnabled;
        private int      m_nGrpcPort;
        private String   m_sLogLevel;
        private Duration m_durationStartupWait;
        private Duration m_durationStartupTimeout;
        private Duration m_durationHold;
        }

    // ----- inner class: Trial --------------------------------------------

    public static class Trial
            implements AutoCloseable
        {
        Trial(LaunchConfig config, List<MemberProcess> listMembers)
            {
            f_config = config;
            f_listMembers = listMembers;
            }

        public LaunchConfig getConfig()
            {
            return f_config;
            }

        public List<MemberProcess> getMembers()
            {
            return f_listMembers;
            }

        void setMeasurementWindow(Instant instantStart, Instant instantEnd)
            {
            for (MemberProcess member : f_listMembers)
                {
                member.setMeasurementWindow(instantStart, instantEnd);
                }
            }

        @Override
        public void close()
            throws Exception
            {
            Exception exception = null;
            for (MemberProcess member : f_listMembers)
                {
                try
                    {
                    member.close();
                    }
                catch (Exception e)
                    {
                    if (exception == null)
                        {
                        exception = e;
                        }
                    else
                        {
                        exception.addSuppressed(e);
                        }
                    }
                }

            if (exception != null)
                {
                throw exception;
                }
            }

        private final LaunchConfig        f_config;
        private final List<MemberProcess> f_listMembers;
        }

    // ----- inner class: MemberProcess ------------------------------------

    public static class MemberProcess
            implements AutoCloseable
        {
        MemberProcess(String sName, Process process, Path pathLog, Path pathJfr, boolean fJfrEnabled)
            {
            f_sName = sName;
            f_process = process;
            f_pathLog = pathLog;
            f_pathJfr = pathJfr;
            f_fJfrEnabled = fJfrEnabled;
            }

        public String getName()
            {
            return f_sName;
            }

        public long getPid()
            {
            return f_process.pid();
            }

        public Path getLog()
            {
            return f_pathLog;
            }

        public Path getJfr()
            {
            return f_pathJfr;
            }

        public boolean isAlive()
            {
            return f_process.isAlive();
            }

        void setMeasurementWindow(Instant instantStart, Instant instantEnd)
            {
            m_instantWindowStart = instantStart;
            m_instantWindowEnd   = instantEnd;
            }

        @Override
        public void close()
            throws Exception
            {
            Exception exception = null;
            try
                {
                dumpAndStopJfr();
                checkJfrPins();
                }
            catch (Exception e)
                {
                exception = e;
                }
            finally
                {
                destroyProcess();
                }

            try
                {
                List<String> listPins = PinnedThreadParser.findPinnedThreadTraces(f_pathLog);
                if (!listPins.isEmpty())
                    {
                    throw new IllegalStateException("Pinned virtual thread detected in " + f_sName
                            + " (" + f_pathLog + "):" + System.lineSeparator()
                            + String.join(System.lineSeparator(), listPins));
                    }
                }
            catch (Exception e)
                {
                if (exception == null)
                    {
                    exception = e;
                    }
                else
                    {
                    exception.addSuppressed(e);
                    }
                }

            if (exception != null)
                {
                throw exception;
                }
            }

        private void dumpAndStopJfr()
                throws IOException, InterruptedException
            {
            if (!f_fJfrEnabled || !f_process.isAlive())
                {
                return;
                }

            runJcmd("JFR.dump", "name=trial", "filename=" + f_pathJfr);
            runJcmd("JFR.stop", "name=trial");
            }

        private void checkJfrPins()
                throws IOException, InterruptedException
            {
            if (!f_fJfrEnabled)
                {
                return;
                }

            PinSummary summary = JfrPinnedThreadParser.summarize(f_pathJfr,
                    m_instantWindowStart, m_instantWindowEnd);
            if (summary.getTotalPins() > 0)
                {
                System.out.println(f_sName + ' ' + summary.formatSummary());
                }

            if (summary.hasMeasuredPins())
                {
                throw new IllegalStateException("Measured-window pinned virtual-thread event detected in " + f_sName
                        + " JFR (" + f_pathJfr + "):" + System.lineSeparator()
                        + summary.formatFailure());
                }
            }

        private void runJcmd(String... asCommand)
                throws IOException, InterruptedException
            {
            List<String> listCommand = new ArrayList<>();
            listCommand.add(javaExecutable("jcmd").toString());
            listCommand.add(Long.toString(f_process.pid()));
            listCommand.addAll(List.of(asCommand));

            Process process = new ProcessBuilder(listCommand)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(f_pathLog.toFile()))
                    .start();
            if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0)
                {
                throw new IllegalStateException("Failed to run jcmd " + listCommand + " for " + f_sName);
                }
            }

        private void destroyProcess()
                throws InterruptedException
            {
            if (!f_process.isAlive())
                {
                return;
                }

            f_process.destroy();
            if (!f_process.waitFor(10, TimeUnit.SECONDS))
                {
                f_process.destroyForcibly();
                f_process.waitFor(10, TimeUnit.SECONDS);
                }
            }

        private final String  f_sName;
        private final Process f_process;
        private final Path    f_pathLog;
        private final Path    f_pathJfr;
        private final boolean f_fJfrEnabled;
        private Instant       m_instantWindowStart;
        private Instant       m_instantWindowEnd;
        }

    private final List<Trial> m_listTrials = new ArrayList<>();

    private static final String DEFAULT_CACHE_CONFIG = "daemonpool-multi-member-cache-config.xml";
    private static final String DEFAULT_OVERRIDE_CONFIG = "daemonpool-multi-member-coherence-override.xml";
    }
