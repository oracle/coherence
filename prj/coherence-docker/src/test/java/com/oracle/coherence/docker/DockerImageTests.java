/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.docker;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.NetworkSettings;
import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicInteger;
import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.oracle.coherence.grpc.client.common.GrpcRemoteCacheService;
import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.internal.net.management.HttpHelper;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.net.Coherence;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.Session;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Various tests to verify basic functionality of an Oracle Coherence Docker image.
 *
 * @author Jonathan Knight 2022.04.21
 */
@SuppressWarnings("resource")
public class DockerImageTests
        extends BaseDockerImageTests
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        ImageNames.verifyTestAssumptions();
        }

    /**
     * Return the image names for paramterized tests.
     *
     * @return the image names for paramterized tests
     */
    static String[] getImageNames()
        {
        return ImageNames.getImageNames();
        }

    // ----- test methods ---------------------------------------------------

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getImageNames")
    void shouldStartContainerWithNoArgs(String sImageName)
        {
        ImageNames.verifyTestAssumptions();
        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)))
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage-" + ImageNames.getTag(sImageName))))))
            {
            Eventually.assertDeferred(container::isHealthy, is(true));
            }
        }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getImageNames")
    void shouldStartContainerWithExtend(String sImageName)
        {
        assertCoherenceClient(sImageName, "remote-fixed", new CheckExtendCacheAccess(), new CheckConcurrentAccess());
        }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getImageNames")
    void shouldStartGrpcServerAndConnectWithGrpc(String sImageName)
        {
        assertCoherenceClient(sImageName, "grpc-fixed", new CheckGrpcCacheAccess());
        }

    @SuppressWarnings("unchecked")
    void assertCoherenceClient(String sImageName, String sClient, RemoteCallable<Void>... assertions)
        {
        ImageNames.verifyTestAssumptions();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)))
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage-" + ImageNames.getTag(sImageName))))
                .withExposedPorts(EXTEND_PORT, GRPC_PORT, CONCURRENT_EXTEND_PORT)))
            {
            Eventually.assertDeferred(container::isHealthy, is(true));

            LocalPlatform platform       = LocalPlatform.get();
            int           extendPort     = container.getMappedPort(EXTEND_PORT);
            int           grpcPort       = container.getMappedPort(GRPC_PORT);
            int           concurrentPort = container.getMappedPort(CONCURRENT_EXTEND_PORT);

            try (CoherenceClusterMember client = platform.launch(CoherenceClusterMember.class,
                    SystemProperty.of("coherence.client", sClient),
                    SystemProperty.of("coherence.extend.address", "127.0.0.1"),
                    SystemProperty.of("coherence.extend.port", extendPort),
                    SystemProperty.of("coherence.grpc.address", "127.0.0.1"),
                    SystemProperty.of("coherence.grpc.port", grpcPort),
                    SystemProperty.of("coherence.concurrent.extend.address", "127.0.0.1"),
                    SystemProperty.of("coherence.concurrent.extend.port", concurrentPort),
                    IPv4Preferred.yes(),
                    LocalHost.only(),
                    DisplayName.of("client"),
                    m_testLogs))
                {
                Eventually.assertDeferred(client::isCoherenceRunning, is(true));

                for (RemoteCallable<Void> callable : assertions)
                    {
                    client.invoke(callable);
                    }
                }
            }
        }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getImageNames")
    void shouldStartWithJavaOpts(String sImageName)
        {
        ImageNames.verifyTestAssumptions();

        File fileArgsDir = createJvmArgsFile("-Dcoherence.role=storage", "-Dcoherence.cluster=datagrid");

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)))
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage-" + ImageNames.getTag(sImageName))))
                .withFileSystemBind(fileArgsDir.getAbsolutePath(), "/args", BindMode.READ_ONLY)))
            {
            Eventually.assertDeferred(container::isHealthy, is(true));
            String sLog = container.getLogs();
            assertThat(sLog, containsString("Started cluster Name=datagrid"));
            }
        }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getImageNames")
    void shouldAddToClasspath(String sImageName)
        {
        ImageNames.verifyTestAssumptions();

        File fileBuild = MavenProjectFileUtils.locateBuildFolder(getClass());
        assertThat(fileBuild, is(notNullValue()));

        File   fileTestClasses = new File(fileBuild, "test-classes");
        String sLibs           = fileTestClasses.getAbsolutePath();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)))
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage-" + ImageNames.getTag(sImageName))))
                .withFileSystemBind(sLibs, COHERENCE_HOME + "/ext/conf", BindMode.READ_ONLY)
                .withExposedPorts(EXTEND_PORT)))
            {
            Eventually.assertDeferred(container::isHealthy, is(true));

            int hostPort = container.getMappedPort(EXTEND_PORT);
            System.setProperty("test.extend.port", String.valueOf(hostPort));

            ExtensibleConfigurableCacheFactory.Dependencies deps
                    = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance("client-cache-config.xml");

            ExtensibleConfigurableCacheFactory eccf    = new ExtensibleConfigurableCacheFactory(deps);
            NamedCache<String, String>         cache   = eccf.ensureCache("test-cache", null);
            Boolean                            fResult = cache.invoke("foo", new TestProcessor<>());
            assertThat(fResult, is(true));
            }
        }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getImageNames")
    void shouldStartRestManagementServer(String sImageName) throws Exception
        {
        ImageNames.verifyTestAssumptions();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)))
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage-" + ImageNames.getTag(sImageName))))
                .withExposedPorts(MANAGEMENT_PORT)))
            {
            Eventually.assertDeferred(container::isHealthy, is(true));

            int                  hostPort = container.getMappedPort(MANAGEMENT_PORT);
            URI                  uri      = HttpHelper.composeURL("127.0.0.1", hostPort).toURI();
            HttpRequest          request  = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<String> response = m_httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode(), is(200));
            assertThat(response.body(), is(notNullValue()));
            assertThat(response.body().length(), is(not(0)));
            }
        }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getImageNames")
    void shouldStartCoherenceMetricsServer(String sImageName) throws Exception
        {
        ImageNames.verifyTestAssumptions();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)))
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage-" + ImageNames.getTag(sImageName))))
                .withExposedPorts(METRICS_PORT)))
            {
            Eventually.assertDeferred(container::isHealthy, is(true));

            int                  hostPort = container.getMappedPort(METRICS_PORT);
            URI                  uri      = MetricsHttpHelper.composeURL("127.0.0.1", hostPort).toURI();
            HttpRequest          request  = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<String> response = m_httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode(), is(200));
            assertThat(response.body(), is(notNullValue()));
            assertThat(response.body().length(), is(not(0)));
            }
        }

    @SuppressWarnings("unchecked")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getImageNames")
    void shouldStartServiceWithWellKnowAddresses(String sImageName) throws Exception
        {
        ImageNames.verifyTestAssumptions();

        String sName1  = "Storage-" + ImageNames.getTag(sImageName) + "-1";
        String sName2  = "Storage-" + ImageNames.getTag(sImageName) + "-2";

        try (Network network = Network.newNetwork())
            {
            try (GenericContainer<?> container1 = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                    .withImagePullPolicy(NeverPull.INSTANCE)
                    .waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)))
                    .withExposedPorts(MANAGEMENT_PORT)
                    .withNetwork(network)
                    .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build(sName1)))
                    .withEnv("COHERENCE_WKA", sName1 + "," + sName2)
                    .withEnv("COHERENCE_WKA_DNS_RESOLUTION_RETRY", "true")
                    .withEnv("COHERENCE_MEMBER", sName1)
                    .withEnv("COHERENCE_CLUSTER", "test-cluster")
                    .withEnv("COHERENCE_CLUSTER", "storage")
                    .withCreateContainerCmdModifier(cmd -> cmd.withHostName(sName1).withName(sName1))
                    .withNetworkAliases(sName1)))
                {
                Eventually.assertDeferred(container1::isHealthy, is(true));
                InspectContainerResponse info = container1.getContainerInfo();
                NetworkSettings          net  = info.getNetworkSettings();

                Map<String, ContainerNetwork> mapNetwork = net.getNetworks();
                StringBuilder sbWka = new StringBuilder(sName1);
                for (ContainerNetwork n : mapNetwork.values())
                    {
                    String sIP = n.getIpAddress();
                    if (sIP != null && !sIP.isEmpty())
                        {
                        sbWka.append(",").append(sIP);
                        }
                    }

                try (GenericContainer<?> container2 = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                        .withImagePullPolicy(NeverPull.INSTANCE)
                        .waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(5)))
                        .withNetwork(network)
                        .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build(sName2)))
                        .withEnv("COHERENCE_WKA", sbWka.toString())
                        .withEnv("COHERENCE_WKA_DNS_RESOLUTION_RETRY", "true")
                        .withEnv("COHERENCE_MEMBER", sName2)
                        .withEnv("COHERENCE_CLUSTER", "test-cluster")
                        .withEnv("COHERENCE_CLUSTER", "storage")
                        .withCreateContainerCmdModifier(cmd -> cmd.withHostName(sName2).withName(sName2))
                        .withNetworkAliases(sName2)))
                    {
                    Eventually.assertDeferred(container2::isHealthy, is(true));

                    int                  hostPort = container1.getMappedPort(MANAGEMENT_PORT);
                    URI                  uri      = HttpHelper.composeURL("127.0.0.1", hostPort).toURI();
                    HttpRequest          request  = HttpRequest.newBuilder(uri).GET().build();
                    HttpResponse<String> response = m_httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    Map<String, Object> map = new GensonBuilder().create().deserialize(response.body(), HashMap.class);
                    assertThat(map, is(notNullValue()));
                    Number nSize = (Number) map.get("clusterSize");
                    assertThat(nSize, is(notNullValue()));
                    assertThat(nSize.intValue(), is(2));
                    }
                }
            }
        }

    // ----- helper methods -------------------------------------------------

    private File createJvmArgsFile(String... args)
        {
        try
            {
            File fileOutDir  = getOutputDirectory();
            File fileArgs    = new File(fileOutDir, "jvm-args.txt");

            try (PrintWriter writer = new PrintWriter(fileArgs))
                {
                Arrays.stream(args).forEach(writer::println);
                }

            return fileOutDir;
            }
        catch (FileNotFoundException e)
            {
            throw new RuntimeException(e);
            }
        }

    // ----- inner class: CheckCacheAccess ----------------------------------

    /**
     * A {@link RemoteCallable} to verify that caches can be accessed
     * from a client.
     */
    protected abstract static class CheckCacheAccess
            implements RemoteCallable<Void>
        {
        public CheckCacheAccess(Class<?> clzExpectedService)
            {
            this.clzExpectedService = clzExpectedService;
            }

        @Override
        public Void call()
            {
            Session                    session = Coherence.getInstance().getSession();
            NamedCache<String, String> cache   = session.getCache("test-cache");

            Service cacheService = cache.getService();
            if (cacheService instanceof SafeService)
                {
                cacheService = ((SafeService) cacheService).getService();
                }
            assertThat(cacheService, is(instanceOf(clzExpectedService)));

            cache.put("key-1", "value-1");
            assertThat(cache.get("key-1"), is("value-1"));
            return null;
            }

        // ----- data members -----------------------------------------------

        private final Class<?> clzExpectedService;
        }

    // ----- inner class: CheckExtendCacheAccess ----------------------------

    /**
     * A {@link RemoteCallable} to verify that caches can be accessed
     * from a client, where the cache service should be an Extend
     * {@link RemoteCacheService}.
     */
    protected static class CheckExtendCacheAccess
            extends CheckCacheAccess
        {
        public CheckExtendCacheAccess()
            {
            super(RemoteCacheService.class);
            }
        }

    // ----- inner class: CheckGrpcCacheAccess ------------------------------

    /**
     * A {@link RemoteCallable} to verify that caches can be accessed
     * from a client, where the cache service should be a gRPC
     * {@link GrpcRemoteCacheService}.
     */
    protected static class CheckGrpcCacheAccess
            extends CheckCacheAccess
        {
        public CheckGrpcCacheAccess()
            {
            super(GrpcRemoteCacheService.class);
            }
        }

    // ----- inner class: CheckConcurrentAccess -----------------------------

    /**
     * A {@link RemoteCallable} to verify that Coherence concurrent works
     * from a client.
     */
    protected static class CheckConcurrentAccess
            implements RemoteCallable<Void>
        {
        @Override
        public Void call()
            {
            Session                    session = Coherence.getInstance().getSession(ConcurrentServicesSessionConfiguration.SESSION_NAME);
            NamedCache<String, String> cache   = session.getCache("atomic-foo");

            Service cacheService = cache.getService();
            if (cacheService instanceof SafeService)
                {
                cacheService = ((SafeService) cacheService).getService();
                }
            assertThat(cacheService, is(instanceOf(RemoteCacheService.class)));

            RemoteAtomicInteger atomic = Atomics.remoteAtomicInteger("test");
            atomic.set(100);
            assertThat(atomic.get(), is(100));

            return null;
            }
        }

    // ----- data members ---------------------------------------------------

    @RegisterExtension
    protected static final TestLogsExtension m_testLogs = new TestLogsExtension(DockerImageTests.class);

    /**
     * The test http client.
     */
    private final HttpClient m_httpClient = HttpClient.newHttpClient();
    }
