/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.management.internal.MapProvider;
import com.tangosol.discovery.NSLookup;

import com.tangosol.internal.net.management.HttpHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.management.MBeanHelper;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Properties;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;


import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test the various startup modes for the HTTP management server.
 */
public class ManagementStartupModeTests
        extends AbstractFunctionalTest
    {
    // ----- junit lifecycle methods ----------------------------------------

    /**
     * Initialize the test class.
     * <p>
     * Do not start a cluster here.  Each individual test will start one.
     */
    @BeforeClass
    public static void _startup()
        {
        setupProps();
        }

    // ----- tests ----------------------------------------------------------

    @Test
    public void testDynamicWithDefault()
        {
        testCase("dynamic", null, 0);
        }

    @Test
    public void testDynamicWithAll()
        {
        testCase("dynamic", "all", 2);
        }

    @Test
    public void testDynamicWithInherit()
        {
        testCase("dynamic", "inherit", 1);
        }

    @Test
    public void testDynamicWithNone()
        {
        testCase("dynamic", "none", 0);
        }

    @Test
    public void testAllWithDefault()
        {
        testCase("all", null, 0);
        }

    @Test
    public void testAllWithAll()
        {
        testCase("all", "all", 2);
        }

    @Test
    public void testAllWithInherit()
        {
        testCase("all", "inherit", 2);
        }

    @Test
    public void testAllWithNone()
        {
        testCase("all", "none", 0);
        }

    @Test
    public void testNoneWithDefault()
        {
        testCase("none", null, 0);
        }

    @Test
    public void testNoneWithAll()
        {
        testCase("none", "all", 0);
        }

    @Test
    public void testNoneWithInherit()
        {
        testCase("none", "inherit", 0);
        }

    @Test
    public void testNoneWithNone()
        {
        testCase("none", "none", 0);
        }

    @Test
    public void validateRunningOnConfiguredManagementPort()
        throws IOException
        {
        String SERVER_MEMBERNAME = "ConfiguredManagementPort";
        int             nPort    = Integer.getInteger("test.multicast.port");

        AvailablePortIterator availablePortIterator = new AvailablePortIterator(31000, 31025);
        int                   nMgmtPort             = availablePortIterator.next();
        Properties            propServer            = new Properties();

        propServer.put("coherence.management", "all");
        propServer.put("coherence.management.http", "all");
        propServer.put("coherence.management.http.override-port", Integer.toString(nMgmtPort));

        try (CoherenceClusterMember member = startCacheServer(SERVER_MEMBERNAME, PROJECT_NAME, null, propServer, true))
            {
            Eventually.assertThat(invoking(member).isServiceRunning(HttpHelper.getServiceName()), is(true));

            Collection<URL> colManagementURL = NSLookup.lookupHTTPManagementURL(new InetSocketAddress("127.0.0.1", nPort));

            assertThat("validate a HTTP management url returned for each server by lookupHTTPManagementURL",
                colManagementURL.size(), is(1));
            assertThat("validate management port is set to configured port of " + nMgmtPort, colManagementURL.iterator().next().getPort(), is(nMgmtPort));
            }
        }

    /**
     * Simulate starting 2 cache servers on same machine with management over REST enabled and same coherence.management.http.port.
     */
    @Test
    public void testDetectionOfManagementPortConflicts()
        throws IOException, InterruptedException
        {
        String SERVER_MEMBERNAME_PREFIX = "MgmtOverRESTPortConflictTests";

        Properties propServer = new Properties();

        propServer.put("coherence.management",      "all");
        propServer.put("coherence.management.http", "all");

        // harden this test to not used a fixed port, intermittently the port was in use by another process.
        propServer.put("coherence.management.http.override-port", "0");

        try (CoherenceClusterMember member1 = startCacheServer(SERVER_MEMBERNAME_PREFIX + "-1", PROJECT_NAME, null, propServer, true))
            {
            int             nPort            = Integer.getInteger("test.multicast.port");
            Collection<URL> colManagementURL = NSLookup.lookupHTTPManagementURL(new InetSocketAddress("127.0.0.1", nPort));
            Eventually.assertThat(invoking(member1).isServiceRunning(HttpHelper.getServiceName()), is(true));
            assertThat("validate a HTTP management over REST url returned the server by lookupHTTPManagementURL",
                colManagementURL.size(), is(1));

            int nPortMgmt = colManagementURL.iterator().next().getPort();

            // simulate user not setting coherence.management.port at all and using "coherence.management" set to all.
            // ensure collision on port that is already in use. Make sure log message reports the port conflicted on.
            propServer.put("coherence.management.http.override-port", Integer.toString(nPortMgmt));
            try (CoherenceClusterMember member2 = startCacheServer(SERVER_MEMBERNAME_PREFIX + "-2", PROJECT_NAME, null, propServer, true))
                {
                File fileMember2Log = new File(ensureOutputDir(PROJECT_NAME), SERVER_MEMBERNAME_PREFIX + "-2.out");

                Eventually.assertThat(invoking(this).validateLogFileContainsServerStarted(fileMember2Log), is(true));
                assertTrue("failed to find log message detecting management over REST proxy address already in use error message in server log",
                    validateLogFileContainsAddressAlreadyInUse(fileMember2Log, Integer.toString(nPortMgmt)));
                }
            }
        }

    @Test
    public void testConfigurationOverride()
            throws MalformedObjectNameException, ReflectionException,
                   InstanceNotFoundException, AttributeNotFoundException,
                   MBeanException
        {
        AvailablePortIterator availablePortIterator = new AvailablePortIterator(31050, 31060);
        int                   nMgmtPort             = availablePortIterator.next();

        System.setProperty("coherence.management", "all");
        System.setProperty("coherence.management.http", "inherit");
        System.setProperty("coherence.management.remote", "true");
        System.setProperty("coherence.management.http.override-port", String.valueOf(nMgmtPort));
        try
            {
            AbstractFunctionalTest._startup();
            MBeanServer serverJMX = MBeanHelper.findMBeanServer();
            String      attr      = (String) serverJMX.getAttribute(new ObjectName("Coherence:type=ConnectionManager,name=ManagementHttpProxy,nodeId=1"), "HostIP");
            assertNotNull(attr);
            assertTrue("Management HTTP port", attr.endsWith(":" + nMgmtPort));
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            System.clearProperty("coherence.management");
            System.clearProperty("coherence.management.http");
            System.clearProperty("coherence.management.remote");
            System.clearProperty("coherence.management.http.override-port");
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Test that the number of expected Management over REST servers are started.
     *
     * @param sMgmt      the "managed-nodes" setting, must not be null
     * @param sHttpMgmt  the "http-managed-nodes" setting, may be null
     * @param nExpected  the number of expected Management over REST HTTP servers
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void testCase(String sMgmt, String sHttpMgmt, int nExpected)
        {
        Objects.requireNonNull(sMgmt);

        String     sServerName = SERVER_PREFIX + '-' + sMgmt + '-' + sHttpMgmt;
        Properties propServer  = new Properties();

        propServer.setProperty("coherence.management", sMgmt);
        propServer.setProperty("coherence.management.http.override-port", "0");

        if (sHttpMgmt != null)
            {
            propServer.setProperty("coherence.management.http", sHttpMgmt);
            }

        try (CoherenceClusterMember member1 = startCacheServer(sServerName + "-1", PROJECT_NAME, null, propServer);
             CoherenceClusterMember member2 = startCacheServer(sServerName + "-2", PROJECT_NAME, null, propServer))
            {
            // insert cache entries through both servers to make sure services are started
            NamedCache cache = findApplication(sServerName + "-1").getCache(CACHE_NAME);
            cache.put("key1", "value1");

            cache = findApplication(sServerName + "-2").getCache(CACHE_NAME);
            cache.put("key2", "value2");

            assertEquals(2, cache.size());

            int             nPort = Integer.getInteger("test.multicast.port");
            Collection<URL> col   = NSLookup.lookupHTTPManagementURL(new InetSocketAddress("127.0.0.1", nPort));

            assertEquals(nExpected, col.size());

            if (nExpected > 0)
                {
                CacheFactory.log("URL(s): " + col, CacheFactory.LOG_INFO);

                // sanity check all listeners for data
                Client client = ClientBuilder.newBuilder()
                        .register(MapProvider.class)
                        .build();

                try (final Closeable clientCloser = client::close)
                    {
                    for (URL url : col)
                        {
                        WebTarget target   = client.target(url.toURI());
                        Response  response = target.path("caches").request().get();

                        try (final Closeable responseCloser = response::close)
                            {
                            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
                            LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
                            assertThat(mapResponse, notNullValue());
                            }
                        }
                    }
                }
            }
        catch (IOException | URISyntaxException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    private static boolean validateLogFileContainsAddressAlreadyInUse(File fileLog, String sPort)
        throws IOException
        {
        FileReader     fileReader     = new FileReader( fileLog);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line;
        while ((line = bufferedReader.readLine()) != null)
            {
            if (line.contains("<Error>") && line.contains(HttpHelper.getServiceName()) &&
                line.contains(sPort) && line.contains("Address already in use"))
                {
                return true;
                }
            }
        return false;
        }

    public boolean validateLogFileContainsServerStarted(File fileLog)
        throws IOException
        {
        FileReader     fileReader     = new FileReader( fileLog);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line;
        while ((line = bufferedReader.readLine()) != null)
            {
            if (line.contains("Started DefaultCacheServer"))
                {
                return true;
                }
            }
        return false;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Prefix for the spawned processes.
     */
    protected final String SERVER_PREFIX = getClass().getSimpleName();

    /**
     * Project name to be passed to Bedrock.
     */
    protected static final String PROJECT_NAME = "management";

    /**
     * Cache name for testing.
     */
    protected static final String CACHE_NAME = "mgmt-over-rest-test";
    }
