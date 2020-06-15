/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package metrics;


import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.oracle.bedrock.util.Capture;
import com.tangosol.internal.net.metrics.MetricsHttpHelper;
import com.tangosol.net.DefaultCacheServer;
import common.AbstractFunctionalTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verify that Coherence Metrics works when the management senior
 * only has Coherence on the classpath.
 * <p>
 *
 * NOTE: *****************************************************************
 *
 * This test WILL NOT run from inside IntelliJ because the classpath used
 * for the storage members will be wrong. This class relies on being run
 * as a proper functional test as part of the Maven build where it will
 * use coherence.jar for the storage members.
 *
 * ***********************************************************************
 */
public class MetricsWithoutRestTests
    {
    // ----- test lifecycle ------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup() throws Exception
        {
        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();
        LocalPlatform platform = LocalPlatform.get();

        // Create a classpath for the storage members (and management senior) that will
        // not have metrics 
        ClassPath classPath = ClassPath.of(ClassPath.ofClass(DefaultCacheServer.class),
                                           ClassPath.ofClass(MetricsWithoutRestTests.class),
                                           ClassPath.ofClass(AbstractFunctionalTest.class));

        builder.include(3, CoherenceClusterMember.class,
                        classPath,
                        ClusterName.of("MetricsWithoutRestTests"),
                        SystemProperty.of("coherence.management", "dynamic"),
                        SystemProperty.of("coherence.metrics.http.enabled", true),
                        SystemProperty.of("coherence.metrics.http.port", platform.getAvailablePorts()),
                        s_testLogs.builder(),
                        DisplayName.of("storage"));

        s_cluster = builder.build();
        Eventually.assertThat(invoking(s_cluster).getClusterSize(), is(3));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        s_cluster.close();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldGetMetrics() throws Exception
        {
        LocalPlatform          platform = LocalPlatform.get();
        Capture<Integer>       nPort    = new Capture<>(platform.getAvailablePorts());
        CoherenceClusterMember member   = platform.launch(CoherenceClusterMember.class,
                                                          LocalStorage.disabled(),
                                                          ClusterName.of("MetricsWithoutRestTests"),
                                                          SystemProperty.of("coherence.management", "none"),
                                                          SystemProperty.of("coherence.metrics.http.enabled", true),
                                                          SystemProperty.of("coherence.metrics.http.port", nPort),
                                                          s_testLogs.builder(),
                                                          DisplayName.of("metrics"));

        Eventually.assertThat(invoking(member).getClusterSize(), is(4));
        Eventually.assertThat(invoking(member).isServiceRunning("MetricsHttpProxy"), is(true));

        URL               metricsUrl = MetricsHttpHelper.composeURL("127.0.0.1", nPort.get());
        HttpURLConnection con        = (HttpURLConnection) metricsUrl.openConnection();

        con.setRequestMethod("GET");
        int nResponse = con.getResponseCode();

        assertThat("Failed to obtain metrics from member " + member.getLocalMemberId(),
                   nResponse, is(200));

        StringBuilder sbResponse = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())))
            {
            String inputLine;

            while ((inputLine = in.readLine()) != null)
                {
                sbResponse.append(inputLine);
                }
            }

        assertThat("Failed to obtain metrics from member " + member.getLocalMemberId(),
                   sbResponse.length(), is(not(0)));
        }

   // ----- data members ----------------------------------------------------

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(MetricsWithoutRestTests.class);

    private static CoherenceCluster s_cluster;
    }
