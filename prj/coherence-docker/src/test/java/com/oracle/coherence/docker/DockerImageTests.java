/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.docker;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.Platform;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.Argument;

import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import com.oracle.coherence.client.GrpcSessionConfiguration;

import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicInteger;
import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;
import com.oracle.coherence.io.json.JsonSerializer;

import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.internal.net.management.HttpHelper;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.io.Serializer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Service;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.junit.jupiter.api.extension.RegisterExtension;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import org.testcontainers.images.ImagePullPolicy;

import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import java.lang.reflect.Method;

import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        s_fImageExists = checkImageExists();
        }

    @BeforeEach
    void setUp(TestInfo testInfo)
        {
        m_sTestMethod = testInfo.getTestMethod().map(Method::getName).orElse("unknown");
        }

    @AfterEach
    void afterTest()
        {
        // clean up Coherence
        CacheFactory.shutdown();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    void shouldStartContainerWithNoArgs()
        {
        verifyTestAssumptions();
        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(IMAGE_NAME))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))))
            {
            Eventually.assertDeferred(container::isHealthy, is(true));
            }
        }

    @Test
    void shouldStartContainerWithExtend()
        {
        verifyTestAssumptions();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(IMAGE_NAME))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))
                .withExposedPorts(EXTEND_PORT, CONCURRENT_EXTEND_PORT)))
            {
            Eventually.assertDeferred(container::isHealthy, is(true));

            LocalPlatform platform       = LocalPlatform.get();
            int           extendPort     = container.getMappedPort(EXTEND_PORT);
            int           concurrentPort = container.getMappedPort(EXTEND_PORT);

            try (CoherenceClusterMember client = platform.launch(CoherenceClusterMember.class,
                                                                 SystemProperty.of("coherence.client", "remote-fixed"),
                                                                 SystemProperty.of("coherence.extend.address", "127.0.0.1"),
                                                                 SystemProperty.of("coherence.extend.port", extendPort),
                                                                 SystemProperty.of("coherence.concurrent.extend.address", "127.0.0.1"),
                                                                 SystemProperty.of("coherence.concurrent.extend.port", concurrentPort),
                                                                 IPv4Preferred.yes(),
                                                                 LocalHost.only(),
                                                                 m_testLogs))
                {
                Eventually.assertDeferred(client::isCoherenceRunning, is(true));

                RemoteCallable<Void> testExtend = () ->
                    {
                    Session                    session = Coherence.getInstance().getSession();
                    NamedCache<String, String> cache   = session.getCache("test-cache");

                    Service cacheService = cache.getService();
                    if (cacheService instanceof SafeService)
                        {
                        cacheService = ((SafeService) cacheService).getService();
                        }
                    assertThat(cacheService, is(instanceOf(RemoteCacheService.class)));

                    cache.put("key-1", "value-1");
                    assertThat(cache.get("key-1"), is("value-1"));
                    return null;
                    };

                RemoteCallable<Void> testConcurrent = () ->
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
                    };

                client.invoke(testExtend);
                client.invoke(testConcurrent);
                }
            }
        }

    @Test
    void shouldStartWithJavaOpts()
        {
        verifyTestAssumptions();

        File fileArgsDir = createJvmArgsFile("-Dcoherence.role=storage", "-Dcoherence.cluster=datagrid");

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(IMAGE_NAME))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))
                .withFileSystemBind(fileArgsDir.getAbsolutePath(), "/args", BindMode.READ_ONLY)))
            {
            Eventually.assertDeferred(container::isHealthy, is(true));
            String sLog = container.getLogs();
            assertThat(sLog, containsString("Started cluster Name=datagrid"));
            }
        }

    @Test
    void shouldAddToClasspath()
        {
        verifyTestAssumptions();

        File fileBuild = MavenProjectFileUtils.locateBuildFolder(getClass());
        assertThat(fileBuild, is(notNullValue()));

        File   fileTestClasses = new File(fileBuild, "test-classes");
        String sLibs           = fileTestClasses.getAbsolutePath();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(IMAGE_NAME))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))
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

    @Test
    void shouldStartRestManagementServer() throws Exception
        {
        verifyTestAssumptions();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(IMAGE_NAME))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))
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

    @Test
    void shouldStartCoherenceMetricsServer() throws Exception
        {
        verifyTestAssumptions();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(IMAGE_NAME))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))
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

    @Test
    void shouldStartGrpcServerAndConnectWithGrpc()
        {
        verifyTestAssumptions();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(IMAGE_NAME))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))
                .withExposedPorts(GRPC_PORT)))
            {
            Eventually.assertDeferred(container::isHealthy, is(true));

            int     hostPort = container.getMappedPort(GRPC_PORT);
            Channel channel  = ManagedChannelBuilder.forAddress("localhost", hostPort)
                                    .usePlaintext()
                                    .build();

            Serializer           serializer = new JsonSerializer();
            SessionConfiguration cfg        = GrpcSessionConfiguration.builder(channel)
                                                            .withSerializer(serializer, "json")
                                                            .build();

            Session session = Session.create(cfg).orElseThrow(() -> new AssertionError("Session is null"));

            NamedMap<String, String> map = session.getMap("grpc-test");

            map.put("key-1", "value-1");
            assertThat(map.get("key-1"), is("value-1"));
            }
        }

    @SuppressWarnings("unchecked")
    @Test
    void shouldStartServiceWithWellKnowAddresses() throws Exception
        {
        verifyTestAssumptions();

        String sName1  = "storage-1";
        String sName2  = "storage-2";

        try (Network network = Network.newNetwork())
            {
            try (GenericContainer<?> container1 = start(new GenericContainer<>(DockerImageName.parse(IMAGE_NAME))
                    .withImagePullPolicy(NeverPull.INSTANCE)
                    .withExposedPorts(MANAGEMENT_PORT)
                    .withNetwork(network)
                    .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build(sName1)))
                    .withEnv("COHERENCE_WKA", sName1 + "," + sName2)
                    .withEnv("COHERENCE_MEMBER", sName1)
                    .withEnv("COHERENCE_CLUSTER", "test-cluster")
                    .withEnv("COHERENCE_CLUSTER", "storage")
                    .withNetworkAliases(sName1)))
                {
                try (GenericContainer<?> container2 = start(new GenericContainer<>(DockerImageName.parse(IMAGE_NAME))
                        .withImagePullPolicy(NeverPull.INSTANCE)
                        .withNetwork(network)
                        .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build(sName2)))
                        .withEnv("COHERENCE_WKA", sName1 + "," + sName2)
                        .withEnv("COHERENCE_MEMBER", sName2)
                        .withEnv("COHERENCE_CLUSTER", "test-cluster")
                        .withEnv("COHERENCE_CLUSTER", "storage")
                        .withNetworkAliases(sName2)))
                    {
                    Eventually.assertDeferred(container1::isHealthy, is(true));
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

    GenericContainer<?> start(GenericContainer<?> container)
        {
        container.start();
        return container;
        }

    /**
     * Verify the assumptions needed to run tests.
     */
    private void verifyTestAssumptions()
        {
        Assumptions.assumeTrue(IMAGE_NAME != null, "Skipping test, coherence.docker.image property not set");
        Assumptions.assumeTrue(!IMAGE_NAME.trim().isEmpty(), "Skipping test, coherence.docker.image property not set");
        Assumptions.assumeTrue(s_fImageExists, "Skipping test, image " + IMAGE_NAME + " is not present");
        }

    /**
     * Verify that the image being tested is already present.
     *
     * @return {@code true} if the image being tested is present.
     */
    private static boolean checkImageExists()
        {
        if (IMAGE_NAME == null || IMAGE_NAME.trim().isEmpty())
            {
            return false;
            }

        String sImage  = IMAGE_NAME;
        String sDocker = "docker.io/";
        if (sImage.startsWith(sDocker))
            {
            sImage = sImage.substring(sDocker.length());
            }

        Platform platform = LocalPlatform.get();

        try (Application app = platform.launch("docker",
                                               Argument.of("inspect"),
                                               Argument.of(sImage)))
            {
            int exitCode = app.waitFor();
            return exitCode == 0;
            }
        }

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

    private File getOutputDirectory()
        {
        File fileOutDir = FileUtils.getTestOutputFolder(getClass());
        File fileTests  = new File(fileOutDir, "functional" + File.separator + getClass().getSimpleName());
        File dir        = new File(fileTests, m_sTestMethod);
        dir.mkdirs();
        return dir;
        }

    // ----- inner class NeverPull ------------------------------------------

    public static class NeverPull
            implements ImagePullPolicy
        {
        @Override
        public boolean shouldPull(DockerImageName imageName)
            {
            return false;
            }

        public static final NeverPull INSTANCE = new NeverPull();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the image to test, set by the coherence.docker.image System property.
     */
    private static final String IMAGE_NAME = System.getProperty("docker.image.name");

    /**
     * COHERENCE_HOME inside coherence docker image.
     */
    private static final String COHERENCE_HOME = System.getProperty("docker.coherence.home", "/coherence");

    public static final int GRPC_PORT = Integer.getInteger("port.grpc", 1408);
    public static final int MANAGEMENT_PORT = Integer.getInteger("port.management", HttpHelper.DEFAULT_MANAGEMENT_OVER_REST_PORT);
    public static final int METRICS_PORT = Integer.getInteger("port.metrics", MetricsHttpHelper.DEFAULT_PROMETHEUS_METRICS_PORT);
    public static final int EXTEND_PORT = Integer.getInteger("port.extend",20000);

    public static final int CONCURRENT_EXTEND_PORT = Integer.getInteger("port.concurrent.extend",20001);

    public static final String STARTED_MESSAGE = "Started Coherence server";

    // ----- data members ---------------------------------------------------

    @RegisterExtension
    final TestLogsExtension m_testLogs = new TestLogsExtension(DockerImageTests.class);

    /**
     * Flag indicating whether the image is present.
     */
    private static boolean s_fImageExists;

    /**
     * The name of the current test method.
     */
    private String m_sTestMethod = "unknown";

    /**
     * The test http client.
     */
    private final HttpClient m_httpClient = HttpClient.newHttpClient();
    }
