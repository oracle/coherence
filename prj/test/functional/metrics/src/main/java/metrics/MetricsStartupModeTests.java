/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package metrics;

import com.oracle.bedrock.deferred.DeferredHelper;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractTestInfrastructure;
import com.tangosol.discovery.NSLookup;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.net.Coherence;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.URL;

import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.tangosol.internal.net.metrics.MetricsHttpHelper.DEFAULT_PROMETHEUS_METRICS_PORT;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Functional test for configuring MetricsHttpProxy via system properties.
 *
 * @author jf  2019.07.03
 * @since 12.2.1.4.0
 */
public class MetricsStartupModeTests
    extends AbstractMetricsFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    public MetricsStartupModeTests()
        {
        super(FILE_SERVER_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        // each test scenario configures system properties and observes results,
        // so skip starting cluster here.
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        }

    @After
    public void cleanup()
        {
        AbstractTestInfrastructure.stopAllApplications();
        }

    // ----- test -----------------------------------------------------------

    @Test
    public void testLookupMetricsURLs() throws IOException
        {
        String     sPrefix    = "NSLookupMetricsURL";
        int        nPort      = Integer.getInteger("test.multicast.port");
        Properties propServer = new Properties();

        // Use ephemeral port
        propServer.put("coherence.metrics.http.port", "0");
        propServer.put("coherence.metrics.http.enabled", "true");
        propServer.put("coherence.management.extendedmbeanname", "true");

        try (CoherenceClusterMember member1 = startCacheServer(sPrefix + "1", propServer);
             CoherenceClusterMember member2 = startCacheServer(sPrefix + "2", propServer))
            {
            Eventually.assertThat(invoking(member1).isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));
            Eventually.assertThat(invoking(member2).isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));

            Collection<URL> colMetricsURL  = NSLookup.lookupHTTPMetricsURL(new InetSocketAddress("127.0.0.1", nPort));
            Iterator<URL>   iterMetricsURL = colMetricsURL.iterator();

            assertThat("validate a HTTP metrics url returned for each server by lookupHTTPMetricsURL", colMetricsURL.size(), is(2));
            assertThat("validate not default port of 9612", iterMetricsURL.next().getPort(), is(not(DEFAULT_PROMETHEUS_METRICS_PORT)));
            assertThat("validate not default port of 9612", iterMetricsURL.next().getPort(), is(not(DEFAULT_PROMETHEUS_METRICS_PORT)));
            }
        }

    @Test
    public void testDefaultMetricsPort() throws IOException
        {
        String     sName      = "DefaultMetricsPort";
        int        nPort      = Integer.getInteger("test.multicast.port");
        Properties propServer = new Properties();

        propServer.put("coherence.metrics.http.enabled", "true");
        propServer.put("coherence.management.extendedmbeanname", "true");

        try (CoherenceClusterMember member = startCacheServer(sName, propServer))
            {
            Eventually.assertThat(invoking(member).isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));

            Collection<URL> colMetricsURL = NSLookup.lookupHTTPMetricsURL(new InetSocketAddress("127.0.0.1", nPort));

            assertThat("validate a HTTP metrics url returned for each server by lookupHTTPMetricsURL",
                colMetricsURL.size(), is(1));
            assertThat("validate default port of 9612", colMetricsURL.iterator().next().getPort(), is(DEFAULT_PROMETHEUS_METRICS_PORT));
            }
        }

    @Test
    public void shouldNotStartMetricsByDefault() throws IOException
        {
        String     sName      = "MetricsServiceNotEnabled";
        int        nPort      = Integer.getInteger("test.multicast.port");
        Properties propServer = new Properties();

        // Use ephemeral port
        propServer.put("coherence.metrics.http.port", "0");

        try (CoherenceClusterMember member = startCacheServer(sName, propServer))
            {
            Eventually.assertThat(invoking(member).isServiceRunning(MetricsHttpHelper.getServiceName()), is(false));

            Collection<URL> colMetricsURL = NSLookup.lookupHTTPMetricsURL(new InetSocketAddress("127.0.0.1", nPort));

            assertThat("validate no metrics server for each server by lookupHTTPMetricsURL",
                colMetricsURL.size(), is(0));
            }
        }

    @Test
    public void validateRunningOnConfiguredMetricsPort() throws IOException
        {
        String sName = "ConfiguredMetricsPort";
        int    nPort = Integer.getInteger("test.multicast.port");

        AvailablePortIterator itPorts      = new AvailablePortIterator(9630, 10000);
        int                   nMetricsPort = itPorts.next();
        Properties            propServer   = new Properties();

        propServer.put("coherence.metrics.http.enabled", "true");
        propServer.put("coherence.metrics.http.port", Integer.toString(nMetricsPort));
        propServer.put("coherence.management.extendedmbeanname", "true");

        try (CoherenceClusterMember member = startCacheServer(sName, propServer))
            {
            Eventually.assertThat(invoking(member).isServiceRunning(MetricsHttpHelper.getServiceName()), is(true));

            Collection<URL> colMetricsURL = NSLookup.lookupHTTPMetricsURL(new InetSocketAddress("127.0.0.1", nPort));

            assertThat("validate a HTTP metrics url returned for each server by lookupHTTPMetricsURL",
                colMetricsURL.size(), is(1));
            assertThat("validate metrics port is set port of " + nMetricsPort, colMetricsURL.iterator().next().getPort(), is(nMetricsPort));
            }
        }

    @Test
    public void validateInvalidAuthConfig() throws IOException
        {
        testFailedMetricsSSLConfiguration("coherence.metrics.http.auth", "notvalid");
        }

    @Test
    public void validateMissingSocketProvider() throws IOException
        {
        testFailedMetricsSSLConfiguration("coherence.metrics.http.provider", "missingSSLProvider");
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Start one cache server with invalid property/value configuration.
     * 
     * @param sName   propertyName
     * @param sValue  invalid value for propertyName
     */
    private void testFailedMetricsSSLConfiguration(String sName, String sValue) throws IOException
        {
        String     sMemberName = "MetricsSSLConfigTestsFor" + sName.substring(sName.lastIndexOf('.') + 1);
        int        nPort       = Integer.getInteger("test.multicast.port");
        Properties propServer  = new Properties();

        // Use ephemeral port
        propServer.put("coherence.metrics.http.port", "0");
        propServer.put("coherence.metrics.http.enabled", "true");
        propServer.put(sName, sValue);
        propServer.put("coherence.member", sMemberName);
        propServer.put("coherence.management.extendedmbeanname", "true");

        try (CoherenceClusterMember member = startCacheServer(sMemberName, propServer))
            {
            Eventually.assertThat(invoking(member).isServiceRunning(MetricsHttpHelper.getServiceName()),
                is( false), DeferredHelper.delayedBy(1L, TimeUnit.SECONDS));

            Collection<URL> colMetricsURL = NSLookup.lookupHTTPMetricsURL(new InetSocketAddress("127.0.0.1", nPort));

            assertThat("validate a HTTP metrics url returned for each server by lookupHTTPMetricsURL, none since auth set to invalid value",
                colMetricsURL.size(), is(0));
            assertThat("failed to find log message detecting metrics proxy configuration invalid in server log",
                validateLogFileContainsIllegalArgumentException(new File(ensureOutputDir("metrics"),
                    sMemberName  + ".out")), is(true));
            }
        }

    private static boolean validateLogFileContainsIllegalArgumentException(File fileLog) throws IOException
        {
        FileReader     fileReader     = new FileReader( fileLog);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line;
        while ((line = bufferedReader.readLine()) != null)
            {
            if (line.contains("<Error>") && line.contains("Metrics") && line.contains("IllegalArgumentException"))
                {
                return true;
                }
            }
        return false;
        }

    CoherenceClusterMember startCacheServer(String sMemberName, Properties propServer)
        {
        return AbstractTestInfrastructure.startCacheServer(sMemberName, "metrics", FILE_SERVER_CFG_CACHE,
                propServer, true, null, Coherence.class);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_SERVER_CFG_CACHE = "coherence-cache-config.xml";
    }
