/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;


import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.callables.IsReady;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.concurrent.runnable.RuntimeHalt;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.testing.AbstractTestInfrastructure;

import com.tangosol.coherence.management.internal.MapProvider;

import com.tangosol.util.Base;

import java.net.URI;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.Response;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Tests getting Coherence metrics through Management over REST when the http server is running using JAX-RS
 * in the same way that WLS would configure it.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class WlsMetricsResourceTests
    {
    // ----- constructors ---------------------------------------------------

    public WlsMetricsResourceTests()
        {
        }

    // ----- junit lifecycle methods ----------------------------------------

    @BeforeClass
    public static void _startup()
        {
        Assume.assumeThat(Boolean.getBoolean("test.security.enabled"), is(false));
        System.setProperty("coherence.metrics.http.enabled", "true");
        System.setProperty("test.management.metrics", "true");

        startTestCluster(WLSManagementStub.class, CLUSTER_NAME, ClassPath.ofSystem());

        for (CoherenceClusterMember member : s_cluster)
            {
            Eventually.assertDeferred(() -> member.invoke(WLSManagementStub::isRunning), is(true));
            }
        }

    @AfterClass
    public static void _tearDown()
        {
        // allow server to clean up before being stopped
        Base.sleep(3000);

        if (m_client != null)
            {
            m_client.close();
            }

        if (s_cluster != null)
            {
            // work around for bug 33867995
            s_cluster.close(RuntimeHalt.withExitCode(0));
            }
        }

    @Before
    public void beforeTest()
        {
        String sMsg = ">>>>> Starting test: " + m_testName.getMethodName();
        for (CoherenceClusterMember member : s_cluster)
            {
            if (member != null)
                {
                member.submit(() ->
                    {
                    Logger.info(sMsg);
                    System.err.println(sMsg);
                    System.err.flush();
                    return null;
                    }).join();
                }
            }
        }

    @After
    public void afterTest()
        {
        String sMsg = ">>>>> Finished test: " + m_testName.getMethodName();
        for (CoherenceClusterMember member : s_cluster)
            {
            if (member != null)
                {
                member.submit(() ->
                    {
                    Logger.info(sMsg);
                    System.err.println(sMsg);
                    System.err.flush();
                    return null;
                    }).join();
                    }
            }
        }

    // ----- tests ----------------------------------------------------------

    @Test
    public void testGetMetrics()
        {
        WebTarget target   = getBaseTarget().path("metrics");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        String sResponse = response.readEntity(String.class);
        assertThat(sResponse.length(), Matchers.is(greaterThan(1000)));
        System.out.println("sResponse: " + sResponse);

        response = target.path("Coherence.Cluster.Size").queryParam("cluster", CLUSTER_NAME).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        sResponse = response.readEntity(String.class);
        assertThat(sResponse.length(), Matchers.is(greaterThan(50)));
        System.out.println("sResponse: " + sResponse);
        }

    // ----- utility methods----------------------------------------------------

    public WebTarget getBaseTarget()
        {
        try
            {
            if (m_baseURI == null)
                {
                CoherenceClusterMember member = s_cluster.getAny();
                int                    nPort  = member.invoke(WLSManagementStub::getHttpPort);

                m_baseURI = URI.create("http://127.0.0.1:" + nPort + WlsManagementInfoResourceTests.URI_ROOT + "/coherence");
                Logger.info("Management HTTP Acceptor lookup returned: " + m_baseURI);
                }
            return m_client.target(m_baseURI);
            }
        catch(Throwable t)
            {
            throw Exceptions.ensureRuntimeException(t);
            }
        }

    public static void startTestCluster(Class<?> clsMain, String sClusterName, Option... options)
        {
        startTestCluster(clsMain,
                         sClusterName,
                         WlsMetricsResourceTests::assertClusterReady,
                         WlsMetricsResourceTests::invokeInCluster,
                         opts -> opts,
                         options);
        }

    public static void startTestCluster(Class<?> clsMain,
                                        String sClusterName,
                                        Consumer<CoherenceCluster> clusterReady,
                                        WlsManagementInfoResourceTests.InClusterInvoker inClusterInvoker,
                                        Function<OptionsByType, OptionsByType> beforeLaunch,
                                        Option... opts)
        {
        OptionsByType commonOptions = AbstractTestInfrastructure.createCacheServerOptions(clsMain.getName(), null, System.getProperties());

        AbstractTestInfrastructure.addTestProperties(commonOptions);
        commonOptions.add(Logging.atMax());
        commonOptions.addAll(opts);

        if (!commonOptions.contains(ClassPath.class))
            {
            // If the class path option has not been specifically set we set it here
            // we remove any JAX-RS, Jersey, Jackson, Glassfish and other stuff
            // that we do not want on the server classpath
            ClassPath path = ClassPath.ofSystem()
                    .excluding(".*apache.*")
                    .excluding(".*commons-codec.*")
                    .excluding(".*commons-logging.*")
                    .excluding(".*fasterxml.*")
                    .excluding(".*glassfish.*")
                    .excluding(".*jakarta.*")
                    .excluding(".*jersey.*")
                    .excluding(".*jackson.*");

            commonOptions.add(path);
            }

        CoherenceClusterBuilder builder      = new CoherenceClusterBuilder();
        OptionsByType           propsServer1 = OptionsByType.of(commonOptions);

        propsServer1.remove(JMXManagementMode.class);
        propsServer1.add(SystemProperty.of("coherence.management", "dynamic"));
        propsServer1.add(SystemProperty.of("coherence.cluster", sClusterName));
        propsServer1.add(SystemProperty.of("coherence.management.extendedmbeanname", true));
        propsServer1.add(SystemProperty.of("coherence.member", SERVER_PREFIX + -1));
        propsServer1.add(SystemProperty.of("coherence.role", SERVER_PREFIX + -1));
        propsServer1.add(SystemProperty.of("test.server.name", SERVER_PREFIX + -1));
        propsServer1.add(SystemProperty.of("coherence.management.http", "inherit"));
        propsServer1.add(SystemProperty.of("coherence.management.http.override-port", 0));
        propsServer1.add(SystemProperty.of("coherence.management.http.cluster", sClusterName));
        propsServer1.add(SystemProperty.of("coherence.override", "tangosol-coherence-override-mgmt.xml"));
        propsServer1.add(LocalHost.only());
        propsServer1.add(DisplayName.of(SERVER_PREFIX));
        propsServer1.add(m_testLogs);

        if (Boolean.getBoolean("test.security.enabled"))
            {
            // Workaround: Hitting stack overflow with security manager debugging of access with MultiCluster.
            String sDebug = clsMain.isAssignableFrom(MultiCluster.class)
                                ? "failure,domains"
                                : "access,failure,domains";

            System.setProperty("java.security.debug", sDebug);
            propsServer1.add(SystemProperty.of("java.security.debug", sDebug));
            }

        builder.include(1, CoherenceClusterMember.class, beforeLaunch.apply(propsServer1).asArray());

        OptionsByType propsServer2 = OptionsByType.of(propsServer1);
        propsServer2.add(SystemProperty.of("coherence.member", SERVER_PREFIX + "-2"));
        propsServer2.add(SystemProperty.of("coherence.role", SERVER_PREFIX + "-2"));
        propsServer2.add(SystemProperty.of("test.server.name", SERVER_PREFIX + "-2"));

        builder.include(1, CoherenceClusterMember.class, beforeLaunch.apply(propsServer2).asArray());

        s_cluster = builder.build(LocalPlatform.get());

        clusterReady.accept(s_cluster);

        m_client = ClientBuilder.newBuilder()
                .register(MapProvider.class)
                .build();
        }

    protected static void invokeInCluster(String sCluster, String sMember, RemoteCallable<Void> callable)
        {
        if (sMember == null)
            {
            s_cluster.getAny().invoke(callable);
            }
        else
            {
            s_cluster.get(sMember).invoke(callable);
            }
        }

    protected static void assertClusterReady(CoherenceCluster cluster)
        {
        for (CoherenceClusterMember member : cluster)
            {
            Eventually.assertDeferred(() -> member.invoke(IsReady.INSTANCE), is(true), within(3, TimeUnit.MINUTES));
            }
        }

    // ----- data members ------------------------------------------------------

    /**
     * The client object to be used for the tests.
     */
    protected static Client m_client;

    /**
     * The base URL for Management over REST requests.
     */
    protected static URI m_baseURI;

    // ----- constants ------------------------------------------------------

    /**
     * Name of the Coherence cluster.
     */
    private static final String CLUSTER_NAME = "metricsRestCluster";

    /**
     * Prefix for the spawned processes.
     */
    private static String SERVER_PREFIX = "testMetricsRESTServer";

    /**
     * The cluster members.
     */
    private static CoherenceCluster s_cluster;

    @Rule
    public final TestName m_testName = new TestName();

    @ClassRule
    public static final TestLogs m_testLogs = new TestLogs();
    }
