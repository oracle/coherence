/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.docker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.tangosol.internal.net.management.HttpHelper;
import com.tangosol.internal.net.metrics.MetricsHttpHelper;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import org.junit.Rule;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Various tests to verify basic functionality of an Oracle Coherence Docker image.
 *
 * @author jk  2017.04.19
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

    @AfterEach
    public void afterTest()
        {
        // clean up Coherence
        CacheFactory.shutdown();
        }

    // ----- test methods ---------------------------------------------------

    @ParameterizedTest
    @MethodSource("getImageNames")
    void shouldStartContainerWithNoArgs(String sImageName)
        {
        ImageNames.verifyTestAssumptions();
        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                .waitingFor(DCS_STARTED)
                .withImagePullPolicy(BaseDockerImageTests.NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))))
            {
            Eventually.assertDeferred(container::isRunning, is(true));
            }
        }

    @ParameterizedTest
    @MethodSource("getImageNames")
    void shouldStartContainerWithExtend(String sImageName)
        {
        ImageNames.verifyTestAssumptions();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                .waitingFor(DCS_STARTED)
                .withImagePullPolicy(BaseDockerImageTests.NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))
                .withExposedPorts(EXTEND_PORT)))
            {
            Eventually.assertDeferred(container::isRunning, is(true));

            int extendPort = container.getMappedPort(EXTEND_PORT);
            System.setProperty("test.extend.port", String.valueOf(extendPort));

            ExtensibleConfigurableCacheFactory.Dependencies deps
                    = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance("client-cache-config.xml");

            ExtensibleConfigurableCacheFactory eccf  = new ExtensibleConfigurableCacheFactory(deps);
            NamedCache<String, String>         cache = eccf.ensureCache("test-cache", null);

            cache.put("key-1", "value-1");
            assertThat(cache.get("key-1"), is("value-1"));
            }
        }

    @ParameterizedTest
    @MethodSource("getImageNames")
    void shouldStartWithJavaOpts(String sImageName)
        {
        ImageNames.verifyTestAssumptions();

        File fileArgsDir = createJvmArgsFile("-Dcoherence.role=storage", "-Dcoherence.cluster=datagrid");

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                .waitingFor(DCS_STARTED)
                .withImagePullPolicy(NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))
                .withFileSystemBind(fileArgsDir.getAbsolutePath(), "/args", BindMode.READ_ONLY)))
            {
            Eventually.assertDeferred(container::isRunning, is(true));
            String sLog = container.getLogs();
            assertThat(sLog, containsString("Started cluster Name=datagrid"));
            }
        }

    @ParameterizedTest
    @MethodSource("getImageNames")
    void shouldAddToClasspath(String sImageName)
        {
        ImageNames.verifyTestAssumptions();

        File fileBuild = MavenProjectFileUtils.locateBuildFolder(getClass());
        assertThat(fileBuild, is(notNullValue()));

        File   fileTestClasses = new File(fileBuild, "test-classes");
        String sLibs           = fileTestClasses.getAbsolutePath();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                .waitingFor(DCS_STARTED)
                .withImagePullPolicy(NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))
                .withFileSystemBind(sLibs, COHERENCE_HOME + "/ext/conf", BindMode.READ_ONLY)
                .withExposedPorts(EXTEND_PORT)))
            {
            Eventually.assertDeferred(container::isRunning, is(true));

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

    @ParameterizedTest
    @MethodSource("getImageNames")
    void shouldStartRestManagementServer(String sImageName) throws Exception
        {
        ImageNames.verifyTestAssumptions();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                .waitingFor(DCS_STARTED)
                .withImagePullPolicy(NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))
                .withExposedPorts(MANAGEMENT_PORT)))
            {
            Eventually.assertDeferred(container::isRunning, is(true));

            int                  hostPort = container.getMappedPort(MANAGEMENT_PORT);
            URI                  uri      = HttpHelper.composeURL("127.0.0.1", hostPort).toURI();
            HttpURLConnection    conn     = (HttpURLConnection) uri.toURL().openConnection();

            conn.setRequestMethod("GET");
            assertThat(conn.getResponseCode(), is(200));
            }
        }

    @ParameterizedTest
    @MethodSource("getImageNames")
    void shouldStartCoherenceMetricsServer(String sImageName) throws Exception
        {
        ImageNames.verifyTestAssumptions();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                .waitingFor(DCS_STARTED)
                .withImagePullPolicy(NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))
                .withExposedPorts(METRICS_PORT)))
            {
            Eventually.assertDeferred(container::isRunning, is(true));

            int                  hostPort = container.getMappedPort(METRICS_PORT);
            URI                  uri      = MetricsHttpHelper.composeURL("127.0.0.1", hostPort).toURI();
            HttpURLConnection    conn     = (HttpURLConnection) uri.toURL().openConnection();

            conn.setRequestMethod("GET");
            assertThat(conn.getResponseCode(), is(200));
            }
        }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @MethodSource("getImageNames")
    void shouldStartServiceWithWellKnowAddresses(String sImageName) throws Exception
        {
        ImageNames.verifyTestAssumptions();

        String sName1  = "storage-1";
        String sName2  = "storage-2";

        try (Network network = Network.newNetwork())
            {
            try (GenericContainer<?> container1 = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                    .waitingFor(DCS_STARTED)
                    .withImagePullPolicy(NeverPull.INSTANCE)
                    .withExposedPorts(MANAGEMENT_PORT)
                    .withNetwork(network)
                    .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build(sName1)))
                    .withEnv("COHERENCE_WKA", sName1)
                    .withEnv("COHERENCE_MEMBER", sName1)
                    .withEnv("COHERENCE_CLUSTER", "test-cluster")
                    .withEnv("COHERENCE_CLUSTER", "storage")
                    .withNetworkAliases(sName1)))
                {
                try (GenericContainer<?> container2 = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                        .waitingFor(DCS_STARTED)
                        .withImagePullPolicy(NeverPull.INSTANCE)
                        .withNetwork(network)
                        .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build(sName2)))
                        .withEnv("COHERENCE_WKA", sName1)
                        .withEnv("COHERENCE_MEMBER", sName2)
                        .withEnv("COHERENCE_CLUSTER", "test-cluster")
                        .withEnv("COHERENCE_CLUSTER", "storage")
                        .withNetworkAliases(sName2)))
                    {
                    Eventually.assertDeferred(container1::isRunning, is(true));
                    Eventually.assertDeferred(container2::isRunning, is(true));

                    int                  hostPort = container1.getMappedPort(MANAGEMENT_PORT);
                    URI                  uri      = HttpHelper.composeURL("127.0.0.1", hostPort).toURI();
                    HttpURLConnection    conn     = (HttpURLConnection) uri.toURL().openConnection();

                    conn.setRequestMethod("GET");
                    assertThat(conn.getResponseCode(), is(200));


                    LinkedHashMap<String, Object> map = m_mapper.readValue(conn.getInputStream(), LinkedHashMap.class);
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

    // ----- data members ---------------------------------------------------

    /**
     * The name of the current test method.
     */
    protected  String m_sTestMethod = "unknown";

    @Rule
    public TestLogs m_testLogs = new TestLogs(DockerImageTests.class);

    private final ObjectMapper m_mapper = new ObjectMapper();
    }
