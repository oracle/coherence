/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.docker;

import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.deferred.options.InitialDelay;
import com.oracle.bedrock.deferred.options.MaximumRetryDelay;
import com.oracle.bedrock.deferred.options.RetryFrequency;

import com.oracle.bedrock.io.NetworkHelper;
import com.oracle.bedrock.options.Timeout;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.ApplicationConsole;
import com.oracle.bedrock.runtime.ApplicationConsoleBuilder;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.console.CapturingApplicationConsole;
import com.oracle.bedrock.runtime.console.EventsApplicationConsole;
import com.oracle.bedrock.runtime.docker.DockerContainer;
import com.oracle.bedrock.runtime.docker.commands.Logs;
import com.oracle.bedrock.runtime.docker.commands.Network;
import com.oracle.bedrock.runtime.docker.commands.Run;
import com.oracle.bedrock.runtime.docker.options.ContainerCloseBehaviour;
import com.oracle.bedrock.runtime.options.Argument;
import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.oracle.bedrock.util.Capture;

import com.tangosol.internal.net.management.HttpHelper;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.coherence.rest.providers.JacksonMapperProvider;
import com.tangosol.coherence.rest.util.JsonMap;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.function.Predicate;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.JacksonFeature;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.tangosol.net.cache.TypeAssertion.withTypes;
import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;
import static com.tangosol.internal.net.metrics.MetricsHttpHelper.PROP_METRICS_ENABLED;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;

/**
 * Various tests to verify basic functionality of an Oracle Coherence Docker image.
 *
 * @author jk  2017.04.19
 */
public class DockerImageTests
    {

    /**
     * The name of the image to test, set by the coherence.docker.image System property.
     */
    private static final String IMAGE_NAME = System.getProperty("docker.image.name");

    /**
     * The network name to use.
     */
    private static final String NET_NAME = UUID.randomUUID().toString();

    /**
     * Flag indicating whether the image is present.
     */
    private static boolean imageExists;

    /**
     * COHERENCE_HOME inside coherence docker image.
     */
    private static final String COHERENCE_HOME = System.getProperty("docker.coherence.home", "/coherence");

    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void setup()
        {
        imageExists = checkImageExists();
        ensureNetwork();
        }

    @AfterClass
    public static void cleanup()
        {
        destroyNetwork();
        }

    @After
    public void afterTest()
        {
        // clean up Coherence
        CacheFactory.shutdown();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldStartContainerWithNoArgs() throws Exception
        {
        verifyTestAssumptions();

        Platform platform = LocalPlatform.get();

        try (Application app = platform.launch(Run.image(IMAGE_NAME)
                                                       .detached(),
                                               ContainerCloseBehaviour.remove()))
            {
            DockerContainer container = app.get(DockerContainer.class);

            Eventually.assertThat(invoking(this).tailLogs(platform, container, 50),
                                  hasItem(containsString("Started DefaultCacheServer")));
            }
        }

    @Test
    public void shouldStartContainerWithExtend() throws Exception
        {
        verifyTestAssumptions();

        Platform platform = LocalPlatform.get();
        int      port     = 20000;

        try (Application app = platform.launch(Run.image(IMAGE_NAME)
                                                       .detached()
                                                       .env("COH_EXTEND_PORT", port)
                                                       .publish(port),
                                               ContainerCloseBehaviour.remove()))
            {
            DockerContainer container = app.get(DockerContainer.class);

            Eventually.assertThat(invoking(this).tailLogs(platform, container, 50),
                                  hasItem(containsString("Started DefaultCacheServer")));

            int hostPort = findPortMapping(container, port);

            System.setProperty("test.extend.host", "127.0.0.1");
            System.setProperty("test.extend.port", String.valueOf(hostPort));

            ExtensibleConfigurableCacheFactory.Dependencies deps
                    = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance("client-cache-config.xml");

            ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(deps);

            NamedCache<String, String> cache = eccf.ensureTypedCache("test-cache", null, withTypes(String.class, String.class));

            cache.put("key-1", "value-1");

            assertThat(cache.get("key-1"), is("value-1"));
            }
        }

    //@Test
    public void shouldStartServiceWithWellKnowAddresses() throws Exception
        {
        verifyTestAssumptions();

        Platform platform     = LocalPlatform.get();
        String   serviceName  = UUID.randomUUID().toString();
        int      replicaCount = 2;
        int      port         = createSwarmService(serviceName, replicaCount);

        Eventually.assertThat(invoking(this).isServiceStarted(serviceName, replicaCount), is(true));

        try
            {
            List<String> containerNames = getServiceImageNames(serviceName);

            assertThat(containerNames.size(), is(replicaCount));

            for (String name : containerNames)
                {
                Eventually.assertThat(invoking(this).tailLogs(platform, name, 50),
                                      hasItem(containsString("Started DefaultCacheServer")));
                }

            System.setProperty("test.extend.host", "127.0.0.1");
            System.setProperty("test.extend.port", String.valueOf(port));

            ExtensibleConfigurableCacheFactory.Dependencies deps
                    = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance("client-cache-config.xml");

            ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(deps);

            NamedCache<String, String> cache = eccf.ensureTypedCache("test-cache", null, withTypes(String.class, String.class));

            Integer memberCount = cache.invoke("foo", entry -> CacheFactory.getCluster().getMemberSet().size());

            assertThat(memberCount, is(replicaCount));
            }
        finally
            {
            destroySwarmService(serviceName);
            }
        }

    @Test
    public void shouldPassInJavaOpts() throws Exception
        {
        verifyTestAssumptions();

        Platform platform = LocalPlatform.get();

        try (Application app = platform.launch(Run.image(IMAGE_NAME)
                                                       .detached()
                                                       .env("JAVA_OPTS", "-Dcoherence.role=storage -Dcoherence.cluster=datagrid"),
                                               ContainerCloseBehaviour.remove()))
            {
            DockerContainer container = app.get(DockerContainer.class);

            Eventually.assertThat(invoking(this).tailLogs(platform, container, 50),
                                  hasItem(containsString("Started DefaultCacheServer")));

            Collection<String> logLines = tailLogs(platform, container);

            assertThat(logLines, hasItem(containsString("Started cluster Name=datagrid")));
            }
        }

    @Test
    public void shouldAddToClasspath() throws Exception
        {
        verifyTestAssumptions();

        // locate JUnit jar
        URL  urlJUnit  = Test.class.getProtectionDomain().getCodeSource().getLocation();
        File fileJUnit = new File(urlJUnit.toURI());
        File fileLib   = fileJUnit.getParentFile();

        // locate config
        URL      urlConfig  = getClass().getResource("/client-cache-config.xml");
        File     fileConfig = new File(urlConfig.toURI()).getParentFile();

        Platform platform = LocalPlatform.get();
        int      port     = 20000;

        TestApplicationConsoleBuilder consoleBuilder = new TestApplicationConsoleBuilder("Started DefaultCacheServer");

        try (Application app = platform.launch(Run.image(IMAGE_NAME)
                                                       .detached()
                                                       .env("COH_EXTEND_PORT", port)
                                                       .publish(port)
                                                       .volume(fileLib.getCanonicalPath() + ":" + COHERENCE_HOME + "/ext/lib",
                                                               fileConfig.getCanonicalPath() + ":" + COHERENCE_HOME + "/ext/conf"),
                                               ContainerCloseBehaviour.remove(),
                                               consoleBuilder,
                                               DisplayName.of("server")))
            {
            DockerContainer container = app.get(DockerContainer.class);

            Eventually.assertThat(invoking(this).tailLogs(platform, container, 50),
                                  hasItem(containsString("Started DefaultCacheServer")));

            int hostPort = findPortMapping(container, port);

            System.setProperty("test.extend.host", "127.0.0.1");
            System.setProperty("test.extend.port", String.valueOf(hostPort));

            ExtensibleConfigurableCacheFactory.Dependencies deps
                    = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance("client-cache-config.xml");

            ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(deps);

            NamedCache<String, String> cache = eccf.ensureTypedCache("test-cache", null, withTypes(String.class, String.class));

            Boolean hasConfig = cache.invoke("foo", entry -> entry.getClass().getResource("/client-cache-config.xml") != null);
            Boolean hasJUnit = cache.invoke("foo", entry -> entry.getClass().getResource("/LICENSE-junit.txt") != null);

            assertThat(hasConfig, is(true));
            assertThat(hasJUnit, is(true));
            }
        }

    @Test
    public void shouldStartCacheFactoryConsole() throws Exception
        {
        verifyTestAssumptions();

        Platform                      platform = LocalPlatform.get();
        String                        sStartMsg      = "Map (?):";
        String                        sName          = "console";
        TestApplicationConsoleBuilder consoleBuilder = new TestApplicationConsoleBuilder(sStartMsg);

        try (Application app = platform.launch(Run.image(IMAGE_NAME)
                                                       .interactive(),
                                               Argument.of("console"),
                                               consoleBuilder,
                                               DisplayName.of(sName),
                                               ContainerCloseBehaviour.remove()))
            {
            // send new-line to stdin of the container to make sure we see the console prompt
            Eventually.assertThat(invoking(consoleBuilder).sawMessage(sName, true), is(true));
            }
        }

    @SuppressWarnings("rawtypes")
    @Test
    public void shouldStartRestManagementServer() throws Exception
        {
        verifyTestAssumptions();

        String                      sClusterName  = "http-management";
        Platform                    platform      = LocalPlatform.get();
        CapturingApplicationConsole console       = new CapturingApplicationConsole();
        int                         nMgmtHttpPort = new AvailablePortIterator(8000, 9001).next();

        try (Application app = platform.launch(Run.image(IMAGE_NAME)
                        .detached()
                        .env("JAVA_OPTS", "-Dcoherence.cluster=" + sClusterName)
                        .env("COH_MGMT_HTTP_PORT", nMgmtHttpPort)
                        .publish(nMgmtHttpPort),
                Argument.of("server"),
                Console.of(console),
                ContainerCloseBehaviour.remove()))
            {
            DockerContainer container = app.get(DockerContainer.class);

            Eventually.assertThat(invoking(this).tailLogs(platform, container, 150),
                    hasItem(both(containsString(HttpHelper.getServiceName() + ":HttpAcceptor")).and(containsString(Integer.toString(nMgmtHttpPort)))),
                    InitialDelay.of(2, TimeUnit.SECONDS),
                    Timeout.after(16, TimeUnit.SECONDS),
                    MaximumRetryDelay.of(4, TimeUnit.SECONDS),
                    RetryFrequency.every(4, TimeUnit.SECONDS));

            Eventually.assertThat(invoking(this).tailLogs(platform, container, 150),
                    hasItem(containsString("Service " + HttpHelper.getServiceName() + " joined")),
                    Timeout.after(16, TimeUnit.SECONDS),
                    MaximumRetryDelay.of(4, TimeUnit.SECONDS),
                    RetryFrequency.every(4, TimeUnit.SECONDS));

            Eventually.assertThat(invoking(this).tailLogs(platform, container, 150),
                    hasItem(containsString("Started DefaultCacheServer")),
                    Timeout.after(20, TimeUnit.SECONDS),
                    MaximumRetryDelay.of(4, TimeUnit.SECONDS),
                    RetryFrequency.every(4, TimeUnit.SECONDS)
            );

            int nMgmtHttpLocalPort = findPortMapping(container, nMgmtHttpPort);

            // test cluster info from ManagementInfoResourceTests
            Client client = ClientBuilder.newBuilder()
                            .register(JacksonMapperProvider.class)
                            .register(JacksonFeature.class).build();
            try
                {
                URI uri = HttpHelper.composeURL("localhost", nMgmtHttpLocalPort).toURI();
                Response response = client.target(uri).request().get();

                assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
                JsonMap mapResponse = new JsonMap(response.readEntity(JsonMap.class));

                assertThat(mapResponse, notNullValue());
                assertThat(mapResponse.get("clusterName"), is(sClusterName));
                assertThat(mapResponse.get("running"), is(true));
                assertThat(mapResponse.get("membersDepartureCount"), is(0));

                Object objListMemberIds = mapResponse.get("memberIds");
                assertThat(objListMemberIds, instanceOf(List.class));
                List listMemberIds = (List) objListMemberIds;
                assertThat(listMemberIds.size(), is(1));

                Object objListLinks = mapResponse.get("links");
                assertThat(objListLinks, instanceOf(List.class));

                List<LinkedHashMap> listLinks = (List) objListLinks;

                Set<Object> linkNames = listLinks.stream().map(m -> m.get("rel")).collect(Collectors.toSet());
                assertThat(linkNames, hasItem("self"));
                assertThat(linkNames, hasItem("canonical"));
                assertThat(linkNames, hasItem("parent"));
                assertThat(linkNames, hasItem("members"));
                assertThat(linkNames, hasItem("services"));
                assertThat(linkNames, hasItem("caches"));
                }
            finally
                {
                if (client != null)
                    {
                    client.close();
                    }
                }
            }
        }

    @Test
    public void shouldStartMetricsManagementServer() throws Exception
        {
        verifyTestAssumptions();

        Platform platform = LocalPlatform.get();
        CapturingApplicationConsole console = new CapturingApplicationConsole();
        int port        = 20000;
        int metricsPort = 9612;

        try (Application app = platform.launch(Run.image(IMAGE_NAME)
                .detached()
                .env("JAVA_OPTS", "-Dcoherence.role=storage -Dcoherence.cluster=metricsCluster")
                .env("COH_EXTEND_PORT", port)
                .env("COH_METRICS_PORT", metricsPort)
                .publish(port)
                .publish(metricsPort),
            Argument.of("server"),
            Console.of(console),
            ContainerCloseBehaviour.remove()))
            {
            DockerContainer container = app.get(DockerContainer.class);

            Eventually.assertThat(invoking(this).tailLogs(platform, container, 50),
                hasItem(containsString("Started DefaultCacheServer")),
                InitialDelay.of(2, TimeUnit.SECONDS),
                Timeout.after(20, TimeUnit.SECONDS),
                MaximumRetryDelay.of(4, TimeUnit.SECONDS),
                RetryFrequency.every(4, TimeUnit.SECONDS)
            );

            Eventually.assertThat(invoking(this).tailLogs(platform, container, 50),
                hasItem(containsString("Service " + MetricsHttpHelper.getServiceName() + " joined")),
                Timeout.after(16, TimeUnit.SECONDS),
                MaximumRetryDelay.of(4, TimeUnit.SECONDS),
                RetryFrequency.every(4, TimeUnit.SECONDS));
            }
        }

    @Test
    public void shouldStartQueryPlus() throws Exception
        {
        verifyTestAssumptions();

        Platform                      platform       = LocalPlatform.get();
        String                        sStartMsg      = "CohQL>";
        String                        sName          = "query";
        TestApplicationConsoleBuilder consoleBuilder = new TestApplicationConsoleBuilder(sStartMsg);

        try (Application app = platform.launch(Run.image(IMAGE_NAME)
                .interactive(),
            Argument.of("queryplus"),
            consoleBuilder,
            DisplayName.of(sName),
            ContainerCloseBehaviour.remove()))
            {
            TestApplicationConsole console = consoleBuilder.getConsole(sName);

            // send new-line to stdin of the container to make sure we see the QueryPlus prompt
            Eventually.assertThat(invoking(consoleBuilder).sawMessage(sName, true), is(true));
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Verify the assumptions needed to run tests.
     */
    private void verifyTestAssumptions()
        {
        Assume.assumeThat("Skipping test, coherence.docker.image property not set", IMAGE_NAME, is(notNullValue()));
        Assume.assumeThat("Skipping test, coherence.docker.image property not set", IMAGE_NAME.trim().isEmpty(), is(false));
        Assume.assumeThat("Skipping test, image " + IMAGE_NAME + " is not present", imageExists, is(true));
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

        Platform platform = LocalPlatform.get();

        CapturingApplicationConsole console = new CapturingApplicationConsole();

        try (Application app = platform.launch("docker",
                                               Argument.of("images"),
                                               Argument.of("--format"),
                                               Argument.of("{{.Repository}}:{{.Tag}}"),
                                               Argument.of(IMAGE_NAME),
                                               Console.of(console)))
            {

            int exitCode = app.waitFor();

            if (exitCode != 0)
                {
                return false;
                }
            }

        Queue<String> lines = console.getCapturedOutputLines();

        return lines.contains(IMAGE_NAME);
        }

    /**
     * Obtain the tail of the log of the specified container.
     *
     * @param platform  the {@link Platform} running the container
     * @param container the container
     * @param cLines    the number of lines to obtain
     *
     * @return the tail of the log of the specified container
     */
    // must be public as this method is used in an Eventually.assertThat
    public Collection<String> tailLogs(Platform platform, DockerContainer container, Object cLines)
        {
        return tailLogs(platform, container.getName(), cLines);
        }

    /**
     * Obtain the tail of the log of the specified container.
     *
     * @param platform  the {@link Platform} running the container
     * @param container the container
     *
     * @return the tail of the log of the specified container
     */
    // must be public as this method is used in an Eventually.assertThat
    public Collection<String> tailLogs(Platform platform, DockerContainer container)
        {
        return tailLogs(platform, container.getName(), null);
        }

    /**
     * Obtain the tail of the log of the specified container.
     *
     * @param platform      the {@link Platform} running the container
     * @param containerName the container name
     *
     * @return the tail of the log of the specified container
     */
    // must be public as this method is used in an Eventually.assertThat
    public Collection<String> tailLogs(Platform platform, String containerName)
        {
        return tailLogs(platform, containerName, null);
        }

    /**
     * Obtain the tail of the log of the specified container.
     *
     * @param platform      the {@link Platform} running the container
     * @param containerName the container name
     * @param cLines        the number of lines to obtain
     *
     * @return the tail of the log of the specified container
     */
    // must be public as this method is used in an Eventually.assertThat
    public Collection<String> tailLogs(Platform platform, String containerName, Object cLines)
        {
        CapturingApplicationConsole console = new CapturingApplicationConsole();
        Logs                        logs    = Logs.from(containerName);

        if (cLines != null)
            {
            logs = logs.tail(cLines);
            }

        try (Application app = platform.launch(logs, Console.of(console)))
            {
            app.waitFor();
            }

        List<String> lines = new ArrayList<>();

        lines.addAll(console.getCapturedOutputLines());
        lines.addAll(console.getCapturedErrorLines());

        return lines;
        }

    /**
     * Obtain the tail of the log file of the specified coherence container.
     *
     * @param platform      the {@link Platform} running the container
     * @param containerName the container name
     * @param cLines        the number of lines to obtain
     *
     * @return the tail of the log of the specified container
     */
    // must be public as this method is used in an Eventually.assertThat
    public Collection<String> tailFileLogs(Platform platform, String containerName, int cLines)
        {
        CapturingApplicationConsole console = new CapturingApplicationConsole();

        try (Application app = platform.launch("docker",
                                               Arguments.of("exec", "-i", containerName,
                                                       "tail", "-" + cLines, "/logs/coherence-0.log"),
                                               Console.of(console)))
            {
            app.waitFor();
            }

        return console.getCapturedOutputLines();
        }

    /**
     * Obtain the port mapping for the specified container and port
     *
     * @param container the {@link DockerContainer}
     * @param nPort     the port mapping to locate
     */
    private static int findPortMapping(DockerContainer container, int nPort)
        {
        CapturingApplicationConsole console = new CapturingApplicationConsole();
        Platform platform = LocalPlatform.get();

        try (Application app = platform.launch("docker",
                                               Argument.of("port"),
                                               Argument.of(container.getName()),
                                               Argument.of(nPort),
                                               Console.of(console)))
            {
            app.waitFor();
            }

        Queue<String> lines = console.getCapturedOutputLines();

        assertThat(lines.size(), is(greaterThanOrEqualTo(1)));

        String line = lines.poll();
        String sPort = line.substring(line.lastIndexOf(":") + 1);

        return Integer.parseInt(sPort);
        }

    /**
     * Create the test overlay network.
     */
    private static void ensureNetwork()
        {
        CapturingApplicationConsole console = new CapturingApplicationConsole();
        Platform platform = LocalPlatform.get();

        try (Application app = platform.launch(Network.list().filter("name=" + NET_NAME), Console.of(console)))
            {
            app.waitFor();
            }

        Queue<String> lines = console.getCapturedOutputLines();
        boolean exists = lines.stream().anyMatch(line -> line.contains(NET_NAME));

        if (!exists)
            {
//            try (Application app = platform.launch(Network.create(NET_NAME, "overlay")
//                                                           .withCommandArguments(Argument.of("--attachable"))))
            try (Application app = platform.launch(Network.create(NET_NAME, "overlay")))
                {
                app.waitFor();
                }
            }
        }

    /**
     * Destroy the test overlay network.
     */
    private static void destroyNetwork()
        {
        Platform platform = LocalPlatform.get();

        try (Application app = platform.launch(Network.remove(NET_NAME)))
            {
            app.waitFor();
            }
        }

    /**
     * Start a Swarm service using the test image.
     *
     * @param serviceName  the name to use for the service
     * @param replicaCount the number of replicas to start
     */
    private int createSwarmService(String serviceName, int replicaCount)
        {
        Platform platform = LocalPlatform.get();
        Capture<Integer> port = new Capture<>(LocalPlatform.get().getAvailablePorts());

        try (Application app = platform.launch("docker",
                                               Argument.of("service"),
                                               Argument.of("create"),
                                               Argument.of("--replicas", Math.max(1, replicaCount)),
                                               Argument.of("--name", serviceName),
                                               Argument.of("--network", NET_NAME),
                                               Argument.of("-e", "COH_WKA=tasks." + serviceName),
                                               Argument.of("-e", "COH_EXTEND_PORT=20000"),
                                               Argument.of("--publish", port.get() + ":20000"),
                                               Argument.of(IMAGE_NAME)
        ))
            {

            app.waitFor();
            }

        return port.get();
        }

    /**
     * Destroy a Swarm service.
     *
     * @param serviceName the name of the service
     */
    private void destroySwarmService(String serviceName)
        {
        Platform platform = LocalPlatform.get();

        try (Application app = platform.launch("docker",
                                               Argument.of("service"),
                                               Argument.of("rm"),
                                               Argument.of(serviceName)))
            {
            int exitCode = app.waitFor();

            assertThat(exitCode, is(0));
            }
        }

    /**
     * Determine whether all of the replicas for a service are running.
     *
     * @param serviceName  the name of the service
     * @param replicaCount the number of replicas
     *
     * @return {@code true} if the replicas are running
     */
    // must be public as this method is used in an Eventually.assertThat
    public boolean isServiceStarted(String serviceName, int replicaCount)
        {
        CapturingApplicationConsole console  = new CapturingApplicationConsole();
        Platform                    platform = LocalPlatform.get();

        try (Application app = platform.launch("docker",
                                               Argument.of("service"),
                                               Argument.of("ls"),
                                               Argument.of("-f"),
                                               Argument.of("name=" + serviceName),
                                               Console.of(console)))
            {
            app.waitFor();
            }

        String line = console.getCapturedOutputLines().stream()
                .filter(l -> l.contains(serviceName))
                .findFirst()
                .orElse("");

        return line.contains(replicaCount + "/" + replicaCount);
        }

    /**
     * Obtain a {@link List} of the names of the containers in a service.
     *
     * @param serviceName the name of the service
     *
     * @return the {@link List} of service names
     */
    private List<String> getServiceImageNames(String serviceName)
        {
        CapturingApplicationConsole console = new CapturingApplicationConsole();
        Platform platform = LocalPlatform.get();

        try (Application app = platform.launch("docker",
                                               Argument.of("ps"),
                                               Argument.of("-f"),
                                               Argument.of("name=" + serviceName + "."),
                                               Argument.of("--format"),
                                               Argument.of("{{.ID}}"),
                                               Console.of(console)))
            {
            app.waitFor();
            }

        return console.getCapturedOutputLines().stream()
                .filter(l -> !l.contains("(terminated)"))
                .collect(Collectors.toList());
        }

    /**
     * Return metrics get response from endpoint on localhost at specified <code>port</code>.
     *
     * @param port  metrics http end point port
     *
     * @return collection of MetricSet representing latest metrics scrape.
     * @throws IOException
     */
    private String getMetricsResponse(int port)
            throws IOException
        {
        URL               metricsUrl = MetricsHttpHelper.composeURL("127.0.0.1", port);
        HttpURLConnection con        = (HttpURLConnection) metricsUrl.openConnection();

        con.setRequestMethod("GET");
        con.getResponseCode();

        StringBuilder sbResponse = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())))
            {
            String inputLine;

            while ((inputLine = in.readLine()) != null)
                {
                sbResponse.append(inputLine).append("\n");
                }
            }
        return sbResponse.toString();
        }

    // ----- inner class TestApplicationConsoleBuilder ----------------------

    public class TestApplicationConsoleBuilder
        implements ApplicationConsoleBuilder
        {
        public TestApplicationConsoleBuilder()
            {
            this("Started DefaultCacheServer");
            }

        public TestApplicationConsoleBuilder(String sLookFor)
            {
            m_predicate = s -> s.contains(sLookFor);
            }

        public boolean awaitStart(String sName) throws InterruptedException
            {
            EventsApplicationConsole.CountDownListener listener = m_mapListeners.get(sName);

            return listener != null && listener.await(1, TimeUnit.MINUTES);
            }

        public boolean sawMessage(String sName) throws InterruptedException
            {
            return sawMessage(sName, false);
            }

        public boolean sawMessage(String sName, boolean fSendEnter) throws InterruptedException
            {
            if (fSendEnter)
                {
                TestApplicationConsole console = getConsole(sName);

                console.getInputWriter().println();
                }

            EventsApplicationConsole.CountDownListener listener = m_mapListeners.get(sName);

            return listener != null && listener.getCount() > 0;
            }

        public TestApplicationConsole getConsole(String sName)
            {
            return m_mapConsoles.get(sName);
            }

        @Override
        public ApplicationConsole build(String sName)
            {
            try
                {
                Class                                      clsTest    = DockerImageTests.class;
                File                                       fileOutDir = FileUtils.getTestOutputFolder(clsTest);
                EventsApplicationConsole.CountDownListener listener   = new EventsApplicationConsole.CountDownListener(1);

                fileOutDir.mkdirs();

                File dirClass = new File(fileOutDir, clsTest.getSimpleName());
                File dirTest  = new File(dirClass, m_testWatcher.getMethodName());

                dirTest.mkdirs();

                System.err.println("Logging output from application '" + sName + "' to " + dirTest + File.separator + sName + ".log");

                TestApplicationConsole console = new TestApplicationConsole(new FileWriter(new File(dirTest, sName + ".log")));

                m_mapListeners.put(sName, listener);
                m_mapConsoles.put(sName, console);

                console.withStdOutListener(m_predicate, listener);
                console.withStdErrListener(m_predicate, listener);

                return console;
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }
            }

        private Map<String, EventsApplicationConsole.CountDownListener> m_mapListeners = new HashMap<>();
        private Map<String, TestApplicationConsole> m_mapConsoles = new HashMap<>();

        private Predicate<String> m_predicate;
        }

    // ----- inner class TestApplicationConsole -----------------------------

    public static class TestApplicationConsole
        extends EventsApplicationConsole
        {
        private TestApplicationConsole(FileWriter writer)
            {
            this.m_writer = writer;
            withStdOutListener(this::write);
            withStdErrListener(this::write);
            }

        private void write(String line)
            {
            try
                {
                m_writer.write(line);
                m_writer.write('\n');
                m_writer.flush();
                }
            catch (IOException e)
                {
                e.printStackTrace();
                }
            }


        private final FileWriter m_writer;
        }

    /**
     * JUnit rule to obtain the name of the current test.
     */
    @Rule
    public TestName m_testWatcher = new TestName();
    }
