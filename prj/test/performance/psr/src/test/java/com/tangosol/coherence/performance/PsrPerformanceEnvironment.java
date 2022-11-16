/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.ApplicationConsole;
import com.oracle.bedrock.runtime.ApplicationConsoleBuilder;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.console.CapturingApplicationConsole;
import com.oracle.bedrock.runtime.console.FileWriterApplicationConsole;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.Freeforms;
import com.oracle.bedrock.runtime.java.options.HeapSize;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.JavaHome;
import com.oracle.bedrock.runtime.java.options.JvmOptions;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.java.profiles.CommercialFeatures;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.runtime.options.Discriminator;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.remote.DeploymentArtifact;
import com.oracle.bedrock.runtime.remote.java.options.JavaDeployment;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.util.Capture;
import com.oracle.coherence.common.util.MemorySize;
import com.tangosol.coherence.performance.psr.Console;
import com.tangosol.coherence.performance.psr.ConsoleExtended;
import com.tangosol.coherence.performance.psr.Runner;
import com.tangosol.coherence.performance.psr.RunnerIsRunning;
import com.tangosol.coherence.performance.psr.RunnerProtocol.AbstractTestMessage;
import com.tangosol.coherence.performance.psr.TestResult;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.util.Base;

import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2015.11.26
 */
public class PsrPerformanceEnvironment<E extends PsrPerformanceEnvironment>
        extends PerformanceEnvironment<E>
    {

    public PsrPerformanceEnvironment()
        {
        try
            {
            Platform platformLocal = LocalPlatform.get();

            m_platformConsole = platformLocal;

            m_infrastructureClients = new PlatformGroup<>();
            m_infrastructureClients.addPlatform(platformLocal);

            m_infrastructureCluster = new PlatformGroup<>();
            m_infrastructureCluster.addPlatform(platformLocal);

            m_sCoherenceJarURI = CacheFactory.class.getProtectionDomain().getCodeSource().getLocation().toURI();

            m_sClusterConfig = "server-cache-config-one-worker-thread.xml";

            m_sClientCacheConfiguration = "client-cache-config-java-WithNearCache.xml";

            String sClusterHeap = System.getProperty("test.cluster.heap", "1024m");
            int    nClusterHeap = (int) new MemorySize(sClusterHeap).as(MemorySize.Magnitude.MB);
            String sRunnerHeap  = System.getProperty("test.runner.heap", "128m");
            int    nRunnerHeap  = (int) new MemorySize(sRunnerHeap).as(MemorySize.Magnitude.MB);


            m_heapSizeCluster = HeapSize.of(nClusterHeap, HeapSize.Units.MB, nClusterHeap, HeapSize.Units.MB);
            m_heapSizeRunner  = HeapSize.of(nRunnerHeap, HeapSize.Units.MB, nRunnerHeap, HeapSize.Units.MB);
            }
        catch (URISyntaxException e)
            {
            throw new RuntimeException(e);
            }

        boolean fUseJFR        = Boolean.getBoolean("test.jfr.cluster");
        boolean fUseJFRClients = Boolean.getBoolean("test.jfr.clients");

        withJavaFlightRecorder(fUseJFR);
        withJavaFlightRecorder(fUseJFRClients);
        }


    // ----- ExternalResource methods ---------------------------------------

    @Override
    public void before() throws Throwable
        {
        AvailablePortIterator ports        = LocalPlatform.get().getAvailablePorts();
        String                sClusterPortStart = System.getProperty("test.cluster.port");
        AvailablePortIterator clusterPorts;

        if (sClusterPortStart == null || sClusterPortStart.isEmpty())
            {
            clusterPorts = ports;
            }
        else
            {
            int port = Integer.parseInt(sClusterPortStart);

            if (port <= 0)
                {
                port = 31001;
                }

            clusterPorts = new AvailablePortIterator(port);
            }


        m_captureConsoleClusterPort = new Capture<>(ports);
        m_captureClusterPort        = new Capture<>(clusterPorts);
        m_nConsolePort              = 7778;

        killProcesses();

        if (hasConsole())
            {
            m_console = startConsole(m_platformConsole, m_captureConsoleClusterPort, m_nConsolePort);
            }

        m_cluster = startCluster(m_infrastructureCluster, m_captureClusterPort, m_cStorageMembers);
        }

    @Override
    public void after()
        {
        closeRunners();

        if (m_cluster != null)
            {
            System.err.println("Stopping Cluster...");
            for (CoherenceClusterMember member : m_cluster)
                {
                closeSafely(member);
                }
            System.err.println("Stopped Cluster");
            }

        System.err.println("Stopping Console...");
        closeSafely(m_console);
        System.err.println("Stopped Console");
        }

    // ----- PerformanceEnvironment methods ---------------------------------

    protected void killProcesses()
        {
        killProcesses(m_platformConsole);
        m_infrastructureCluster.forEach(this::killProcesses);
        m_infrastructureClients.forEach(this::killProcesses);
        }

    protected void killProcesses(Platform platform)
        {
        if (platform.equals(LocalPlatform.get()))
            {
            return;
            }

        CapturingApplicationConsole console  = new CapturingApplicationConsole();

        try (Application application = platform.launch("ps -ef | grep oracle.bedrock | awk '{print $2}'",
                                                       DisplayName.of("Kill"),
                                                       PsrRemoteEnvironment.EmptyDeployment.INSTANCE,
                                                       com.oracle.bedrock.runtime.options.Console.of(console)))
            {
            application.waitFor();
            }

        String sKill = console.getCapturedOutputLines().stream()
                .filter((line) -> line != null && !line.equalsIgnoreCase("(terminated)"))
                .collect(Collectors.joining(" "));

        try (Application application = platform.launch(sKill,
                                                       DisplayName.of("Kill"),
                                                       PsrRemoteEnvironment.EmptyDeployment.INSTANCE,
                                                       com.oracle.bedrock.runtime.options.Console.system()))
            {
            application.waitFor();
            }
        }

    public <V> V submitToSingleClient(RemoteCallable<V> callable)
        {
        try
            {
            CoherenceClusterMember member = m_clusterClients.iterator().next();

            return member.submit(callable).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    public <V> void submitToAllClients(RemoteCallable<V> callable)
        {
        submitToAllClients(callable, 10, TimeUnit.MINUTES, new TestResult(), new ArrayList<>());
        }

    public <V> void submitToAllClients(RemoteCallable<V> callable, long timeout, TimeUnit units)
        {
        submitToAllClients(callable, timeout, units, new TestResult(), new ArrayList<V>());
        }

    @SuppressWarnings("unchecked")
    public <V> void submitToAllClients(RemoteCallable<V> callable, long timeout, TimeUnit units,
                                          TestResult globalResult, List<V> listClientResults)
        {
        String                     sName         = callable.getClass().getSimpleName();
        List<CompletableFuture<V>> listFutures   = new ArrayList<>((int) m_clusterClients.count());
        List<RemoteCallable<V>>    listCallables = new ArrayList<>((int) m_clusterClients.count());
        int                        nClient       = 0;

        for (int i=0; i<m_clusterClients.count(); i++)
            {
            RemoteCallable<V> callableClient = callable;

            if (callableClient instanceof AbstractTestMessage)
                {
                callableClient = ((AbstractTestMessage) callableClient)
                        .getMessageForClient(nClient++, m_cRunners);
                }

            if (callableClient == null)
                {
                continue;
                }

            listCallables.add(callableClient);
            }

        nClient = 0;

        globalResult.start();

        for (CoherenceClusterMember client : m_clusterClients)
            {
            final RemoteCallable<V> remoteCallable = listCallables.get(nClient++);

            CompletableFuture<V>    future = client.submit(remoteCallable)
                    .whenComplete((result, t) ->
                        {
                        if (t == null)
                            {
                            if (result instanceof TestResult)
                                {
                                globalResult.add((TestResult) result);
                                }

                            listClientResults.add(result);
                            }
                        else
                            {
                            t.printStackTrace();
                            }
                        });

            listFutures.add(future);
            }

        System.err.println("Waiting for Clients to complete " + sName + "...");

        CompletableFuture[] aFutures = listFutures.toArray(new CompletableFuture[listFutures.size()]);

        try
            {
            CompletableFuture.allOf(aFutures).get(timeout, units);
            }
        catch (InterruptedException | ExecutionException e)
            {
            System.err.println("Exception while waiting for clients to complete");
            e.printStackTrace();
            }
        catch (TimeoutException e)
            {
            System.err.println("Timeout waiting for clients to complete, Timeout=" + units.toMillis(timeout) + " ms");
            throw new RuntimeException(e);
            }

        globalResult.stop();

        System.err.println("All clients completed " + callable.getClass().getSimpleName());
        }


    public void submitTest(RemoteCallable<TestResult> callable, long timeout, TimeUnit units,
                           TestResult result, List<TestResult> listClientResults)
        {
        submitToAllClients(callable, timeout, units, result, listClientResults);
        }

    protected String[] getClusterHostNames()
        {
        return new String[]{LocalPlatform.get().getAddress().getHostName()};
        }


    public void deploy(List<File>  listLibDirs) throws Exception
        {
        // no-op
        }


    public E withConsolePlatform(Platform platform)
        {
        m_platformConsole = platform;

        return (E) this;
        }

    public E withClusterPlatforms(Platform... platforms)
        {
        m_infrastructureCluster = new PlatformGroup<>();

        for (Platform platform : platforms)
            {
            m_infrastructureCluster.addPlatform(platform);
            }

        return (E) this;
        }


    public E withRunnerPlatforms(Platform... platforms)
        {
        m_infrastructureClients = new PlatformGroup<>();

        for (Platform platform : platforms)
            {
            m_infrastructureClients.addPlatform(platform);
            }

        return (E) this;
        }


    public E withClusterTransport(String sTransport)
        {
        m_sClusterTransport = sTransport;

        return (E) this;
        }


    public E withCoherenceJarURI(String sURI)
        {
        try
            {
            return withCoherenceJarURI(new URI(sURI));
            }
        catch (URISyntaxException e)
            {
            throw new RuntimeException(e);
            }
        }


    public E withCoherenceJarURI(URI uri)
        {
        m_sCoherenceJarURI = uri;

        return (E) this;
        }


    public E withPofEnabled(boolean fPof)
        {
        withClusterProperty(Pof.PROPERTY_ENABLED, fPof);
        withRunnerProperty(Pof.PROPERTY_ENABLED, fPof);

        return (E) this;
        }


    public E withPofConfiguration(String sPofConfig)
        {
        withPofEnabled(true);

        withClusterProperty(Pof.PROPERTY_CONFIG, sPofConfig);
        withRunnerProperty(Pof.PROPERTY_CONFIG, sPofConfig);

        return (E) this;
        }


    public boolean hasConsole()
        {
        return m_fHasConsole;
        }


    public E withConsole(boolean fConsole)
        {
        m_fHasConsole = fConsole;

        return (E) this;
        }


    public E withClusterConfiugration(String sConfig)
        {
        m_sClusterConfig = sConfig;

        return (E) this;
        }


    public E withClientConfiguration(String sConfig)
        {
        m_sClientCacheConfiguration = sConfig;

        return (E) this;
        }


    public E withOutputFolder(String sFolder)
        {
        m_sOutputFolder = sFolder;

        return (E) this;
        }


    public E withExtendClientRunners()
        {
        m_fClientsAreExtendClients = true;

        return (E) this;
        }


    public E withClusterMemberRunners()
        {
        m_fClientsAreExtendClients = false;

        return (E) this;
        }


    public E withClusterProperty(String name, Object value)
        {
        m_propertiesCluster = m_propertiesCluster.add(SystemProperty.of(name, value));

        return (E) this;
        }


    public E withClusterProperty(String name, Iterator<?> value)
        {
        m_propertiesCluster = m_propertiesCluster.add(SystemProperty.of(name, value));

        return (E) this;
        }


    public E withRunnerProperty(String name, Object value)
        {
        m_propertiesRunners = m_propertiesRunners.add(SystemProperty.of(name, value));

        return (E) this;
        }


    public E withRunnerProperty(String name, Iterator<?> value)
        {
        m_propertiesCluster = m_propertiesCluster.add(SystemProperty.of(name, value));

        return (E) this;
        }


    public E withTestClasspath(ClassPath classpath)
        {
        m_classPath = classpath;

        return (E) this;
        }

    public E withClusterMemberHeap(HeapSize heapSize)
        {
        m_heapSizeCluster = heapSize;

        return (E) this;
        }

    public E withRunnerHeap(HeapSize heapSize)
        {
        m_heapSizeRunner = heapSize;

        return (E) this;
        }

    public E withJavaHome(JavaHome javaHome)
        {
        m_javaHome = javaHome;

        return (E) this;
        }

    public E withClusterJvmOptions(Freeforms options)
        {
        m_jvmOptionsCluster = options;

        return (E) this;
        }

    public E withStorageMembersPerHost(int count)
        {
        m_cStorageMembers = count;

        return (E) this;
        }

    public E withRunnerCount(int count)
        {
        m_cRunners = count;

        return (E) this;
        }

    public E withJavaFlightRecorder(boolean fUseJFR)
        {
        m_fUseJFR = fUseJFR;

        return (E) this;
        }

    public boolean isUsingJFR()
        {
        return m_fUseJFR;
        }

    public E withJavaFlightRecorderClients(boolean fUseJFR)
        {
        m_fUseJFRClients = fUseJFR;

        return (E) this;
        }

    public boolean isUsingJFRClients()
        {
        return m_fUseJFRClients;
        }

    public E withTestName(String sName)
        {
        m_sTestName = sName;

        return (E) this;
        }

    // ----- accessor methods -----------------------------------------------

    public static File getBuildFolder()
            throws Exception
        {
        URL    url  = PsrPerformanceEnvironment.class.getProtectionDomain().getCodeSource().getLocation();
        File   file = new File(url.toURI());

        return file.getParentFile();
        }


    public File getOutputFolder()
            throws Exception
        {
        File fileBuild = getBuildFolder();

        String sTestName = m_sTestName != null ? m_sTestName + File.separator : "";

        String sFolderName = fileBuild.getCanonicalPath() + File.separator + "test-output"
                + File.separator + "functional" + File.separator + sTestName + m_sOutputFolder;

        File folder = new File(sFolderName);

        folder.mkdirs();

        return folder;
        }

    public CoherenceCluster getCluster()
        {
        return m_cluster;
        }

    public CoherenceCluster getClients()
        {
        return m_clusterClients;
        }

    public CoherenceCacheServer getConsole()
        {
        return m_console;
        }

    public String getClusterCoherenceVersionWithoutPatch()
        {
        String version = getClusterCoherenceVersion();

        int pos = version.lastIndexOf(".");

        if (pos > 0)
            {
            version = version.substring(0, pos);
            }

        return version;
        }


    public String getClusterCoherenceVersion()
        {
        try
            {
            String sVersion;

            if (m_cluster != null && m_cluster.count() != 0)
                {
                sVersion = m_cluster.iterator().next().submit(new GetCoherenceVersion()).get();
                }
            else if (m_clusterClients != null && m_clusterClients.count() > 0)
                {
                sVersion = m_clusterClients.iterator().next().submit(new GetCoherenceVersion()).get();
                }
            else
                {
                sVersion = "Unknown";
                }


            return sVersion;
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    public String getClusterJavaVersion()
        {
        try
            {
            String sVersion;

            if (m_cluster != null && m_cluster.count() != 0L)
                {
                sVersion = m_cluster.iterator().next().submit(new GetJavaVersion()).get();
                }
            else if (m_clusterClients != null && m_clusterClients.count() > 0L)
                {
                sVersion = m_clusterClients.iterator().next().submit(new GetJavaVersion()).get();
                }
            else
                {
                sVersion = "Unknown";
                }


            return sVersion;
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    public String getClusterConfig()
        {
        return m_sClusterConfig;
        }

    public String getClusterTransport()
        {
        return m_sClusterTransport;
        }

    public boolean isExtendClients()
        {
        return m_fClientsAreExtendClients;
        }

    public synchronized CoherenceCluster startClients()
            throws Exception
        {
        System.err.println("Starting Clients...");

        if (m_clusterClients != null)
            {
            System.err.println("Closing previous Clients...");
            for (CoherenceClusterMember member : m_clusterClients)
                {
                closeSafely(member);
                }
            m_clusterClients = null;
            }

        m_clusterClients = startClients(m_infrastructureClients, m_nConsolePort, m_captureClusterPort);

        System.err.println("Started Clients");

        return m_clusterClients;
        }

    public JavaHome getJavaHome()
        {
        return m_javaHome;
        }

    // ----- helper methods -------------------------------------------------

    protected ClassPath createClassPath()
        {
        try
            {
            List<ClassPath> listCP = new ArrayList<>();

            if (m_classPath == null)
                {
                JavaDeployment   deployment    = JavaDeployment.automatic();
                ProtectionDomain pdCoherence   = CacheFactory.class.getProtectionDomain();
                File             fileCoherence = new File(pdCoherence.getCodeSource().getLocation().toURI());

                deployment.exclude("junit-rt.jar");
                deployment.exclude(fileCoherence.getName().toLowerCase());

                List<DeploymentArtifact> artifacts  = deployment.getDeploymentArtifacts(LocalPlatform.get(), OptionsByType.empty());

                artifacts.stream()
                        .forEach((artifact) -> listCP.add(ClassPath.ofFile(artifact.getSourceFile())));
                }
            else
                {
                listCP.add(m_classPath);
                }

            listCP.add(ClassPath.ofFile(new File(m_sCoherenceJarURI)));

            return new ClassPath(listCP);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    public static ClassPath createFrameworkClassPath()
        {
        try
            {
            ProtectionDomain pdMain = Console.class.getProtectionDomain();
            ClassPath        cpMain = ClassPath.ofFile(new File(pdMain.getCodeSource().getLocation().toURI()));
            ProtectionDomain pdThis = PsrPerformanceEnvironment.class.getProtectionDomain();
            ClassPath        cpThis = ClassPath.ofFile(new File(pdThis.getCodeSource().getLocation().toURI()));

            return new ClassPath(cpMain, cpThis);
            }
        catch (URISyntaxException e)
            {
            throw new RuntimeException(e);
            }
        }


    public boolean isConsoleReady()
        {
        try
            {
            if (!m_fHasConsole)
                {
                return true;
                }

            if (m_console == null)
                {
                return false;
                }

            return m_console.submit(new ConsoleExtended.IsReady()).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    protected CoherenceCacheServer startConsole(Platform platform, Capture<Integer> captureClusterPort, int nConsolePort)
        {
        OptionsByType options = OptionsByType.of(
                createClassPath(),
                IPv4Preferred.yes(),
                LocalStorage.disabled(),
                ClusterPort.of(captureClusterPort),
                HeapSize.of(128, HeapSize.Units.MB, 128, HeapSize.Units.MB),
                RoleName.of("Console"),
                Logging.at(9),
                Arguments.of("-address", "0.0.0.0", "-port", String.valueOf(nConsolePort)),
                Discriminator.of(platform.getName())
        );

        modifySchema(platform, options);

        options.add(createApplicationConsole());

        System.err.println("Starting Console on " + platform.getName() + " ...");
        CoherenceCacheServer console = platform.launch(CoherenceCacheServer.class, options.asArray());
        System.err.println("Started Console");

        return console;
        }


    protected CoherenceCluster startCluster(
            PlatformGroup<Platform> PlatformGroup,
            Capture<Integer> captureClusterPort,
            int cStorageMembersPerHost) throws Exception
        {
        System.err.print("Starting Coherence cluster on ");
        PlatformGroup.forEach((p) -> System.err.print(p.getName() + " "));
        System.err.println("...");

        CoherenceCluster      cluster = new CoherenceCluster(OptionsByType.empty());
        AvailablePortIterator ports          = LocalPlatform.get().getAvailablePorts();

        try
            {
            for (Platform platform : PlatformGroup)
                {
                Integer                    nProxyPort  = ports.next();
                OptionsByType              optsProxy   = createCommonSchema(captureClusterPort, DefaultCacheServer.class)
                    .addAll(
                        LocalStorage.disabled(),
                        ClusterPort.of(captureClusterPort),
                        SystemProperty.of("tangosol.coherence.proxy.address", "0.0.0.0"),
                        SystemProperty.of("tangosol.coherence.proxy.port", nProxyPort),
                        SystemProperty.of("tangosol.coherence.proxy.autostart", true),
                        SystemProperty.of("tangosol.coherence.extend.address", "0.0.0.0"),
                        SystemProperty.of("tangosol.coherence.extend.port", nProxyPort),
                        SystemProperty.of("tangosol.coherence.extend.enabled", true),
                        RoleName.of("Proxy"),
                        Discriminator.of(platform.getName())
                    );

                modifySchema(platform, optsProxy);

                OptionsByType optsServer = createCommonSchema(captureClusterPort, DefaultCacheServer.class)
                        .addAll(
                            ClusterPort.of(captureClusterPort),
                            LocalStorage.enabled(),
                            RoleName.of("Storage")
                        );

                modifySchema(platform, optsServer);

                for (int i=1; i<=cStorageMembersPerHost; i++)
                    {
                    OptionsByType schema   = OptionsByType.of(optsServer);
                    String        sName    = "Data-" + i + "@" + platform.getName();

                    System.err.println("Starting " + sName);
                    OptionsByType opts = OptionsByType.of(schema)
                            .add(createApplicationConsole())
                            .add(DisplayName.of(sName));

                    CoherenceCacheServer server = platform.launch(CoherenceCacheServer.class, opts.asArray());

                    cluster.add(server);
                    }

                if (isExtendClients())
                    {
                    String sNameProxy = "Proxy-1@" + platform.getName();

                    System.err.println("Starting " + sNameProxy);
                    OptionsByType opts = OptionsByType.of(optsProxy)
                            .add(createApplicationConsole())
                            .add(DisplayName.of(sNameProxy));

                    CoherenceCacheServer proxy = platform.launch(CoherenceCacheServer.class, opts.asArray());

                    cluster.add(proxy);
                    }
                }


            int nSize = (int) cluster.count();

            System.err.println("Waiting for Coherence cluster to be ready... (" + nSize + ")");


            for (CoherenceClusterMember member : cluster)
                {
                Eventually.assertThat(invoking(member).getClusterSize(), is(nSize));
                Eventually.assertThat(invoking(this).areAllAutoStartServicesRunning(member), is(true));
                }
            }
        catch (Throwable t)
            {
            cluster.forEach(PsrPerformanceEnvironment::closeSafely);

            throw t;
            }

        System.err.println("Started Coherence cluster");

        return cluster;
        }

    // Must be public - used in Eventually.assertThat
    public boolean areAllAutoStartServicesRunning(CoherenceClusterMember member) throws Exception
        {
        return member.submit(new AreAllAutoStartServicesRunning()).get(2, TimeUnit.MINUTES);
        }

    protected void modifySchema(Platform platform, OptionsByType schema)
        {
        //schema.add(LocalHost.of("127.0.0.1"));
        }

    protected OptionsByType createCommonSchema(Capture<Integer> captureClusterPort, Class clsMain)
        {
        Freeforms jvmOptions = m_jvmOptionsCluster == null
                ? JvmOptions.include("-verbose:gc") : m_jvmOptionsCluster;

        ClassPath classPathSystem = ClassPath.ofSystem();

        List<String> listReducedPath = StreamSupport.stream(classPathSystem.spliterator(), false)
                                                    .filter((path)  -> !path.contains("prj/coherence-"))
                                                    .filter((path)  -> !path.contains("coherence-core"))
                                                    .filter((path)  -> !path.contains("prj/fmw-commons"))
                                                    .collect(Collectors.toList());

        ClassPath classPath = ClassPath.of(listReducedPath);
        OptionsByType options = OptionsByType.of(
                ClassName.of(clsMain),
                classPath,
                ClusterName.of(m_clusterName),
                createClassPath(),
                IPv4Preferred.yes(),
                ClusterPort.of(captureClusterPort),
                CacheConfig.of(m_sClusterConfig),
                m_heapSizeCluster,
                CommercialFeatures.enabled(),
                Logging.at(9),
                JMXManagementMode.LOCAL_ONLY,
                jvmOptions
        );

        if (m_sClusterTransport != null)
            {
            options.add(SystemProperty.of("tangosol.coherence.transport.reliable", m_sClusterTransport));
            }

        options.add(m_propertiesCluster);

        if (m_javaHome != null)
            {
            options.add(m_javaHome);
            }

        return options;
        }


    private synchronized CoherenceCluster startClients(PlatformGroup<Platform> PlatformGroup, Object nConsolePort,
                                                       Capture<Integer> captureClusterPort) throws Exception
        {
        System.err.print("Starting Client cluster on ");
        PlatformGroup.forEach((p) -> System.err.print(p.getName() + " "));
        System.err.println("...");

        CoherenceCluster assembly = new CoherenceCluster(OptionsByType.empty());

        CoherenceCluster         cluster         = getCluster();
        Map<String,List<String>> mapProxies      = new LinkedHashMap<>();
        int                      cRunnersPerHost = m_cRunners / PlatformGroup.size();

        if (isExtendClients())
            {
            for (CoherenceClusterMember member : cluster.getAll("Proxy"))
                {
                String sProxyHost = member.getPlatform().getAddress().getHostName();
                String sPort = member.getSystemProperty("tangosol.coherence.proxy.port");

                List<String> list = mapProxies.get(sProxyHost);
                if (list == null)
                    {
                    list = new ArrayList<>();
                    mapProxies.put(sProxyHost, list);
                    }
                list.add(sPort);
                }
            }
        else
            {
            mapProxies.put("Dummy", Arrays.asList("Dummy"));
            }

        Iterator<String> itHosts     = mapProxies.keySet().iterator();
        Class<?>         classClient = hasConsole() ? Runner.class : SimpleClient.class;
        String           sClient     = classClient.getCanonicalName();

        for (Platform platform : PlatformGroup)
            {
            int nRunner = 1;

            if (!itHosts.hasNext())
                {
                itHosts = mapProxies.keySet().iterator();
                }

            String           sProxyHost     = itHosts.next();
            List<String>     listPorts      = mapProxies.get(sProxyHost);
            Iterator<String> itPorts        = listPorts.iterator();

            for (int i=0; i<cRunnersPerHost; i++)
                {
                OptionsByType schema;

                if (isExtendClients())
                    {
                    if (!itPorts.hasNext())
                        {
                        itPorts = listPorts.iterator();
                        }


                    schema = OptionsByType.of(DisplayName.of(sClient),
                            createClassPath(),
                            IPv4Preferred.yes(),
                            ClusterPort.of(captureClusterPort),
                            m_heapSizeRunner,
                            CacheConfig.of(m_sClientCacheConfiguration),
                            SystemProperty.of("tangosol.coherence.proxy.address", sProxyHost),
                            SystemProperty.of("tangosol.coherence.proxy.port", itPorts.next()));
                    }
                else
                    {
                    schema = createCommonSchema(captureClusterPort, classClient)
                            .add(CacheConfig.of(m_sClientCacheConfiguration));
                    }

                schema.addAll(RoleName.of("Client"),
                              LocalStorage.disabled(),
                              Logging.at(9));

                if (hasConsole())
                    {
                    InetAddress address        = m_platformConsole.getAddress();
                    String      addressConsole = address.equals(platform.getAddress()) ? "127.0.0.1" : address.getCanonicalHostName();

                    schema.add(Arguments.of(addressConsole, String.valueOf(nConsolePort)));
                    }

                schema.add(m_propertiesRunners);

                modifySchema(platform, schema);

                String sNamePlatform = platform.getName();
                String sName         = "Client-" + nRunner++ + "@" + sNamePlatform;

                schema.addAll(DisplayName.of(sName), createApplicationConsole());

                assembly.add(platform.launch(CoherenceClusterMember.class, schema.asArray()));
                }
            }

        if (hasConsole())
            {
            for (CoherenceClusterMember runner : assembly)
                {
                Eventually.assertThat(invoking(runner).submit(new RunnerIsRunning()), is(true));
                }
            }

        int cRunners = (int) assembly.count();

        if (m_executorClients != null && cRunners != m_cExecutorThreads)
            {
            m_executorClients.shutdownNow();
            m_executorClients.awaitTermination(1, TimeUnit.MINUTES);
            m_executorClients = null;
            }

        if (m_executorClients == null)
            {
            m_executorClients = Executors.newFixedThreadPool(cRunners);
            m_cExecutorThreads = cRunners;
            }

        return assembly;
        }


    public ApplicationConsoleBuilder createApplicationConsole()
        {
        return new ApplicationConsoleBuilder()
            {
            @Override
            public ApplicationConsole build(String applicationName)
                {
                try
                    {
                    File       file   = new File(getOutputFolder(), applicationName + ".out");
                    FileWriter writer = new FileWriter(file);

                    return new FileWriterApplicationConsole(writer);
                    }
                catch (Exception e)
                    {
                    throw new RuntimeException(e);
                    }
                }
            };
        }

    public static void closeSafely(CoherenceClusterMember member)
        {
        if (member == null)
            {
            return;
            }

        try
            {
            member.close();
            member.waitFor();
            }
        catch (Throwable e)
            {
            e.printStackTrace();
            }
        }

    public void closeRunners()
        {
        System.err.println("Stopping Clients...");
        if (m_clusterClients != null)
            {
            for (CoherenceClusterMember member : m_clusterClients)
                {
                closeSafely(member);
                }

            if (m_executorClients != null)
                {
                m_executorClients.shutdownNow();
                try
                    {
                    m_executorClients.awaitTermination(1, TimeUnit.MINUTES);
                    }
                catch (InterruptedException e)
                    {
                    e.printStackTrace();
                    }
                m_executorClients = null;
                m_cExecutorThreads = -1;
                }
            }

        System.err.println("Stopped Clients");
        }

    // ----- data members ---------------------------------------------------

    private Platform m_platformConsole;

    private boolean m_fHasConsole = true;

    private CoherenceCacheServer m_console;

    private PlatformGroup<Platform> m_infrastructureClients;

    private CoherenceCluster m_clusterClients;

    private ExecutorService m_executorClients;

    private int m_cExecutorThreads = -1;

    private PlatformGroup<Platform> m_infrastructureCluster;

    private String m_clusterName = "Perf-" + System.currentTimeMillis();

    private CoherenceCluster m_cluster;

    private String m_sClusterConfig;

    private String m_sClusterTransport;

    private URI m_sCoherenceJarURI;

    private String m_sClientCacheConfiguration;

    private String m_sOutputFolder = "performance-test";

    private HeapSize m_heapSizeCluster;

    private HeapSize m_heapSizeRunner;

    private boolean m_fClientsAreExtendClients = true;

    private Capture<Integer> m_captureConsoleClusterPort;

    private Capture<Integer> m_captureClusterPort;

    private int m_nConsolePort;

    private SystemProperties m_propertiesRunners = new SystemProperties();

    private SystemProperties m_propertiesCluster = new SystemProperties();

    private ClassPath m_classPath = null;

    private JavaHome m_javaHome = null;

    private Freeforms m_jvmOptionsCluster;

    private int m_cStorageMembers = 3;

    private int m_cRunners = 1;

    private boolean m_fUseJFR = false;

    private boolean m_fUseJFRClients = false;

    private String m_sTestName;
    }
