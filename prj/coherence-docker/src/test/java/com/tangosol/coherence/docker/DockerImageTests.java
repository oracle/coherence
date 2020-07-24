/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.docker;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.options.LaunchLogging;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Profile;
import com.oracle.bedrock.runtime.console.FileWriterApplicationConsole;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.deferred.options.InitialDelay;
import com.oracle.bedrock.deferred.options.MaximumRetryDelay;
import com.oracle.bedrock.deferred.options.RetryFrequency;

import com.oracle.bedrock.options.Timeout;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.Platform;

import com.oracle.bedrock.runtime.console.CapturingApplicationConsole;

import com.oracle.bedrock.runtime.docker.DockerContainer;

import com.oracle.bedrock.runtime.docker.commands.Logs;
import com.oracle.bedrock.runtime.docker.commands.Run;

import com.oracle.bedrock.runtime.docker.options.ContainerCloseBehaviour;

import com.oracle.bedrock.runtime.options.Argument;
import com.oracle.bedrock.runtime.options.Console;

import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.tangosol.internal.net.management.HttpHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TestName;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.tangosol.net.cache.TypeAssertion.withTypes;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

import static org.junit.Assert.assertThat;

/**
 * Various tests to verify basic functionality of an Oracle Coherence Docker image.
 *
 * @author jk  2017.04.19
 */
public class DockerImageTests
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void setup()
        {
        imageExists = checkImageExists();
        }

    @After
    public void afterTest()
        {
        // clean up Coherence
        CacheFactory.shutdown();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldStartContainerWithNoArgs()
        {
        verifyTestAssumptions();

        Platform platform = LocalPlatform.get();

        try (Application app = platform.launch(Run.image(IMAGE_NAME).detached(),
                                               new DumpLogsOnClose(),
                                               ContainerCloseBehaviour.remove()))
            {
            DockerContainer container = app.get(DockerContainer.class);
            assertStarted(platform, container);
            }
        }

    @Test
    public void shouldStartContainerWithExtend()
        {
        verifyTestAssumptions();

        Platform platform = LocalPlatform.get();

        try (Application app = platform.launch(Run.image(IMAGE_NAME).detached().publishAll(),
                                               new DumpLogsOnClose(),
                                               ContainerCloseBehaviour.remove()))
            {
            DockerContainer container = app.get(DockerContainer.class);

            assertStarted(platform, container);

            int hostPort = findPortMapping(container, EXTEND_PORT);
            System.setProperty("test.extend.port", String.valueOf(hostPort));

            ExtensibleConfigurableCacheFactory.Dependencies deps
                    = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance("client-cache-config.xml");

            ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(deps);

            NamedCache<String, String> cache = eccf.ensureTypedCache("test-cache", null, withTypes(String.class, String.class));

            cache.put("key-1", "value-1");

            assertThat(cache.get("key-1"), is("value-1"));
            }
        }

    @Test
    public void shouldStartWithJavaOpts()
        {
        verifyTestAssumptions();

        Platform platform    = LocalPlatform.get();
        File     fileArgsDir = createJvmArgsFile("-Dcoherence.role=storage", "-Dcoherence.cluster=datagrid");

        try (Application app = platform.launch(Run.image(IMAGE_NAME)
                                                       .detached()
                                                       .volume(fileArgsDir.getAbsolutePath() + ":/args"),
                                               new DumpLogsOnClose(),
                                               ContainerCloseBehaviour.remove()))
            {
            DockerContainer container = app.get(DockerContainer.class);
            assertStarted(platform, container);

            Collection<String> logLines = tailLogs(platform, container);
            assertThat(logLines, hasItem(containsString("Started cluster Name=datagrid")));
            }
        }

    @Test
    public void shouldAddToClasspath() throws Exception
        {
        verifyTestAssumptions();

        // locate JUnit jar to add it to the classpath
        URL  urlJUnit  = Test.class.getProtectionDomain().getCodeSource().getLocation();
        File fileJUnit = new File(urlJUnit.toURI());
        File fileLib   = fileJUnit.getParentFile();

        // locate config to put test-classes/ on the classpath
        URL  urlConfig       = getClass().getResource("/client-cache-config.xml");
        File fileTestClasses = new File(urlConfig.toURI()).getParentFile();

        Platform platform = LocalPlatform.get();

        try (Application app = platform.launch(Run.image(IMAGE_NAME)
                                                       .detached()
                                                       .publishAll()
                                                       .volume(fileLib.getCanonicalPath() + ":" + COHERENCE_HOME + "/ext/lib",
                                                               fileTestClasses.getCanonicalPath() + ":" + COHERENCE_HOME + "/ext/conf"),
                                               new DumpLogsOnClose(),
                                               ContainerCloseBehaviour.remove()))
            {
            DockerContainer container = app.get(DockerContainer.class);
            assertStarted(platform, container);

            int hostPort = findPortMapping(container, EXTEND_PORT);

            System.setProperty("test.extend.host", "127.0.0.1");
            System.setProperty("test.extend.port", String.valueOf(hostPort));

            ExtensibleConfigurableCacheFactory.Dependencies deps
                    = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance("client-cache-config.xml");

            ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(deps);

            NamedCache<String, String> cache = eccf.ensureTypedCache("test-cache", null, withTypes(String.class, String.class));
            Boolean fResult = cache.invoke("foo", new TestProcessor<>());
            assertThat(fResult, is(true));
            }
        }

    @Test
    public void shouldStartRestManagementServer() throws Exception
        {
        verifyTestAssumptions();

        String                      sClusterName  = "http-management";
        Platform                    platform      = LocalPlatform.get();
        File                        fileArgsDir   = createJvmArgsFile("-Dcoherence.cluster=" + sClusterName);

        try (Application app = platform.launch(Run.image(IMAGE_NAME)
                        .detached()
                        .volume(fileArgsDir.getAbsolutePath() + ":/args")
                        .publishAll(),
                Argument.of("server"),
                new DumpLogsOnClose(),
                ContainerCloseBehaviour.remove()))
            {
            DockerContainer container = app.get(DockerContainer.class);

            Eventually.assertThat(invoking(this).tailLogs(platform, container, 150),
                    hasItem(both(containsString(HttpHelper.getServiceName() + ":HttpAcceptor"))
                                    .and(containsString(Integer.toString(MANAGEMENT_PORT)))),
                    InitialDelay.of(10, TimeUnit.SECONDS),
                    Timeout.after(2, TimeUnit.MINUTES),
                    MaximumRetryDelay.of(10, TimeUnit.SECONDS),
                    RetryFrequency.every(10, TimeUnit.SECONDS));

            Eventually.assertThat(invoking(this).tailLogs(platform, container, 150),
                    hasItem(containsString("Service " + HttpHelper.getServiceName() + " joined")),
                    Timeout.after(1, TimeUnit.MINUTES),
                    MaximumRetryDelay.of(10, TimeUnit.SECONDS),
                    RetryFrequency.every(10, TimeUnit.SECONDS));

            Eventually.assertThat(invoking(this).tailLogs(platform, container, 150),
                    hasItem(containsString(STARTED_MESSAGE)),
                    Timeout.after(1, TimeUnit.MINUTES),
                    MaximumRetryDelay.of(10, TimeUnit.SECONDS),
                    RetryFrequency.every(10, TimeUnit.SECONDS)
            );

            int               port = findPortMapping(container, MANAGEMENT_PORT);
            URI               uri  = HttpHelper.composeURL("127.0.0.1", port).toURI();
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();

            conn.setRequestMethod("GET");
            assertThat(conn.getResponseCode(), is(200));
            }
        }

    @Test
    public void shouldStartMetricsServer() throws Exception
        {
        verifyTestAssumptions();

        Platform platform = LocalPlatform.get();

        try (Application app = platform.launch(Run.image(IMAGE_NAME).detached().publishAll(),
                                               new DumpLogsOnClose(),
                                               ContainerCloseBehaviour.remove()))
            {
            DockerContainer container = app.get(DockerContainer.class);
            assertStarted(platform, container);

            int               port = findPortMapping(container, METRICS_PORT);
            URI               uri  = URI.create("http://127.0.0.1:" + port + "/metrics/Coherence.Cluster.Size");
            HttpURLConnection con  = (HttpURLConnection) uri.toURL().openConnection();

            con.setRequestMethod("GET");
            assertThat(con.getResponseCode(), is(200));

            StringBuilder sbResponse = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())))
                {
                String inputLine;

                while ((inputLine = in.readLine()) != null)
                    {
                    sbResponse.append(inputLine).append("\n");
                    }
                }

            String response = sbResponse.toString();
            assertThat(response, is(notNullValue()));
            assertThat(response, containsString("vendor:coherence_cluster_size{"));
            }
        }

    @Test
    public void shouldStartServiceWithWellKnowAddresses()
        {
        verifyTestAssumptions();

        Platform platform    = LocalPlatform.get();
        String   sName1      = "storage-1";
        String   sName2      = "storage-2";

        try (Application app1 = platform.launch(Run.image(IMAGE_NAME, sName1)
                                                   .detached()
                                                   .hostName(sName1)
                                                   .env("coherence.wka", sName1)
                                                   .env("coherence.cluster", "datagrid")
                                                   .env("coherence.role", "storage"),
                                                new DumpLogsOnClose(),
                                                ContainerCloseBehaviour.remove()))
            {
            DockerContainer container1 = app1.get(DockerContainer.class);
            assertStarted(platform, container1);

            try (Application app2 = platform.launch(Run.image(IMAGE_NAME, sName2)
                                                       .detached()
                                                       .hostName(sName2)
                                                       .env("coherence.wka", sName1)
                                                       .env("coherence.cluster", "datagrid")
                                                       .env("coherence.role", "storage")
                                                       .link(Collections.singletonList(sName1)),
                                                    new DumpLogsOnClose(),
                                                    ContainerCloseBehaviour.remove()))
                {
                DockerContainer container2 = app2.get(DockerContainer.class);
                assertStarted(platform, container2);
                assertThat(tailLogs(platform, container2), hasItem(containsString("ActualMemberSet=MemberSet(Size=2")));
                }
            }
        }


    // ----- helper methods -------------------------------------------------

    private void assertStarted(Platform platform, DockerContainer container)
        {
        Eventually.assertThat(invoking(this).tailLogs(platform, container, 50),
            hasItem(containsString(STARTED_MESSAGE)),
            InitialDelay.of(10, TimeUnit.SECONDS),
            Timeout.after(2, TimeUnit.MINUTES),
            MaximumRetryDelay.of(10, TimeUnit.SECONDS),
            RetryFrequency.every(10, TimeUnit.SECONDS));
        }

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

        try (Application app = platform.launch(logs, Console.of(console), LaunchLogging.disabled()))
            {
            app.waitFor();
            }

        List<String> lines = new ArrayList<>();

        lines.addAll(console.getCapturedOutputLines());
        lines.addAll(console.getCapturedErrorLines());

        return lines;
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
                                               LaunchLogging.disabled(),
                                               Console.of(console)))
            {
            app.waitFor();
            }

        Queue<String> lines = console.getCapturedOutputLines();

        assertThat(lines.size(), is(greaterThanOrEqualTo(1)));

        String line = lines.poll();
        assertThat(line, is(notNullValue()));

        String sPort = line.substring(line.lastIndexOf(":") + 1);
        return Integer.parseInt(sPort);
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
        File dir        = new File(fileTests, m_testWatcher.getMethodName());
        dir.mkdirs();
        return dir;
        }

    // ----- inner class: DumpLogsOnClose -----------------------------------

    /**
     * A Bedrock {@link Profile} to dump the Docker container logs for
     * an application before closing.
     */
    public class DumpLogsOnClose
            implements Profile, Option
        {
        @Override
        public void onClosing(Platform platform, Application application, OptionsByType optionsByType)
            {
            try
                {
                DockerContainer container = application.get(DockerContainer.class);
                File            dir       = getOutputDirectory();
                if (container != null)
                    {
                    String name = container.getName();
                    File   logs = new File(dir, name + ".log");

                    FileConsole console = new FileConsole(new FileWriter(logs));

                    try (Application app = platform.launch(Logs.from(name),
                                                           DisplayName.of(name),
                                                           LaunchLogging.disabled(),
                                                           Console.of(console)))
                        {
                        app.waitFor();
                        }
                    }
                }
            catch (IOException e)
                {
                e.printStackTrace();
                }
            }

        @Override
        public void onLaunching(Platform platform, MetaClass metaClass, OptionsByType optionsByType)
            {
            }

        @Override
        public void onLaunched(Platform platform, Application application, OptionsByType optionsByType)
            {
            }
        }

    public static class FileConsole
            extends FileWriterApplicationConsole
        {
        public FileConsole(FileWriter fileWriter)
            {
            super(fileWriter);
            }

        @Override
        public PrintWriter getErrorWriter()
            {
            return super.getOutputWriter();
            }
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

    public static final int MANAGEMENT_PORT = Integer.getInteger("port.management", 30000);
    public static final int METRICS_PORT = Integer.getInteger("port.metrics",7001);
    public static final int EXTEND_PORT = Integer.getInteger("port.extend",20000);

    public static final String STARTED_MESSAGE = "Started DefaultCacheServer...";

    // ----- data members ---------------------------------------------------

    /**
     * Flag indicating whether the image is present.
     */
    private static boolean imageExists;

    /**
     * JUnit rule to obtain the name of the current test.
     */
    @Rule
    public TestName m_testWatcher = new TestName();

    @Rule
    public TestLogs m_testLogs = new TestLogs(DockerImageTests.class);
    }
