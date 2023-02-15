/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management.shutdown;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.tangosol.discovery.NSLookup;
import com.tangosol.internal.net.management.HttpHelper;
import com.tangosol.internal.net.metrics.MetricsHttpHelper;
import com.tangosol.io.FileHelper;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Binary;
import com.tangosol.util.WrapperException;

import common.AbstractFunctionalTest;

import management.ManagementInfoResourceTests;

import org.glassfish.jersey.jackson.JacksonFeature;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.oracle.bedrock.deferred.DeferredHelper.*;
import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.NAME;

/**
 * Contains tests for the shutdown and restart of Coherence cluster services via Management over REST.
 *
 * @author gh 2022.12.13
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ManagementShutdownTests extends AbstractFunctionalTest
    {

    // ----- junit lifecycle methods ----------------------------------------


    /**
     * Initialize the test class.
     * <p>
     * This method starts the Coherence cluster, if it isn't already running.
     */
    @BeforeClass
    public static void _startup()
        {
        setupProps();
        System.setProperty("coherence.management", "dynamic");

        Properties propsServer1 = new Properties();
        propsServer1.setProperty("coherence.management", "dynamic");
        propsServer1.setProperty("coherence.management.http", "inherit");
        propsServer1.setProperty("coherence.management.readonly", Boolean.toString(false));
        propsServer1.setProperty("coherence.management.http.port", "0");
        propsServer1.setProperty("coherence.management.http.cluster", CLUSTER_NAME);

        propsServer1.setProperty("coherence.cluster", CLUSTER_NAME);
        propsServer1.setProperty("coherence.management.extendedmbeanname", "true");
        propsServer1.setProperty("coherence.member", SERVER_PREFIX + "-1");
        propsServer1.setProperty("coherence.management.http", "inherit");
        propsServer1.setProperty("coherence.management.http.port", "0");

        try
            {
            m_dirActive   = FileHelper.createTempDir();
            m_dirSnapshot = FileHelper.createTempDir();
            m_dirArchive  = FileHelper.createTempDir();
            s_dirJFR      = FileHelper.createTempDir();
            }
        catch (IOException ioe)
            {
            throw new RuntimeException("Error creating persistence directories", ioe);
            }

        propsServer1.setProperty("test.persistence.active.dir", m_dirActive.getAbsolutePath());
        propsServer1.setProperty("test.persistence.snapshot.dir", m_dirSnapshot.getAbsolutePath());
        propsServer1.setProperty("test.persistence.archive.dir", m_dirArchive.getAbsolutePath());

        if (Boolean.getBoolean("test.security.enabled"))
            {
            System.setProperty("java.security.debug", "access,failure,domains");
            propsServer1.setProperty("java.security.debug", "access,failure,domains");
            }

        CoherenceClusterMember member1 = startCacheServer(SERVER_PREFIX + "-1", "rest", CACHE_CONFIG, propsServer1);
        assertClusterReady(member1);

        Properties propsServer2 = new Properties();
        propsServer2.putAll(propsServer1);
        propsServer2.setProperty("coherence.member", SERVER_PREFIX + "-2");
        propsServer2.setProperty("coherence.metrics.http.enabled", "true");

        CoherenceClusterMember member2 = startCacheServer(SERVER_PREFIX + "-2", "rest", CACHE_CONFIG, propsServer2);

        Eventually.assertThat(invoking(member2).getServiceStatus(SERVICE_NAME), is(ServiceStatus.NODE_SAFE), within(5, TimeUnit.MINUTES));

        Eventually.assertThat(invoking(member2).getServiceStatus(ACTIVE_SERVICE), is(ServiceStatus.NODE_SAFE), within(3, TimeUnit.MINUTES));
        Eventually.assertDeferred(() -> member2.invoke(new ManagementInfoResourceTests.CalculateUnbalanced("dist-persistence-test")),
                Matchers.is(0),
                within(3, TimeUnit.MINUTES));

        // fill a cache
        NamedCache cache    = findApplication(SERVER_PREFIX + "-1").getCache(CACHE_NAME);
        Binary binValue = Binary.getRandomBinary(1024, 1024);
        cache.put(1, binValue);

        // fill front cache
        cache    = findApplication(SERVER_PREFIX + "-1").getCache(NEAR_CACHE_NAME);
        cache.put(1, binValue);

        m_client = ClientBuilder.newBuilder()
                .register(JacksonFeature.class).build();

        m_aMembers[0] = member1;
        m_aMembers[1] = member2;

    }

    @AfterClass
    public static void tearDown()
        {
        m_client.close();
        stopCacheServer(SERVER_PREFIX + "-2");
        stopCacheServer(SERVER_PREFIX + "-1");

        FileHelper.deleteDirSilent(m_dirActive);
        FileHelper.deleteDirSilent(m_dirArchive);
        FileHelper.deleteDirSilent(m_dirSnapshot);
        }

    @Before
    public void beforeTest()
        {
        ensureServicesAreAvailable();
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Verifies COH-25823 - MetricsHttpProxy service fails to restart when Node MBean shutdown() issued
     * <p>
     * This test will shutdown Cluster Node-2 which runs the Metrics Service. Once restarted the node should have the
     * metrics service running again.
     */
    @Test
    public void testClusterNodeShutdownWithServicesRestart()
        {
        WebTarget target = getBaseTarget().path(SERVICES).path(MetricsHttpHelper.getServiceName()).path("members");
        Response response = target.request().get();
        Map mapResponse = readEntity(target, response);
        List<Map> listItemMaps = (List<Map>) mapResponse.get("items");
        assertThat(listItemMaps.size(), is(1));
        assertThat(listItemMaps.get(0).get("nodeId"), is("2"));

        String memberName = SERVER_PREFIX + "-2";

        assertThat(listItemMaps.get(0).get("member"), is(memberName));
        assertThat(listItemMaps.get(0).get("running"), is(true));

        CacheFactory.log("Shutting down " + memberName, CacheFactory.LOG_INFO);
        response = getBaseTarget().path("members").path(memberName).path("shutdown").request(MediaType.APPLICATION_JSON_TYPE).post(null);
        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        AtomicReference<Response> serviceMemberResponse = new AtomicReference<>();
        WebTarget serviceMemberTarget = getBaseTarget().path(SERVICES).path(MetricsHttpHelper.getServiceName()).path("members");

        Eventually.assertDeferred(() ->
            {
            Response localResponse = serviceMemberTarget.request().get();
            serviceMemberResponse.set(localResponse);
            return localResponse.getStatus();
            }, is(Response.Status.OK.getStatusCode()), within(5, TimeUnit.MINUTES), delayedBy(5, TimeUnit.SECONDS));

        assertThat(serviceMemberResponse.get().getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(serviceMemberResponse.get().getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapServicesResponse = readEntity(serviceMemberTarget, serviceMemberResponse.get());

        assertThat(mapServicesResponse, notNullValue());

        List<Map> members = (List<Map>) mapServicesResponse.get("items");
        assertThat(members, notNullValue());

        assertThat(members.size(), is(1));
        assertThat(members.get(0).get("nodeId"), is("3"));
        assertThat(members.get(0).get("member"), is(memberName));
        assertThat(members.get(0).get("running"), is(true));
        CacheFactory.log("Test Complete - Member " + memberName + " is running.", CacheFactory.LOG_INFO);
        }

    // ----- exception support ----------------------------------------------

    /**
     * Convert the passed exception to a RuntimeException if necessary.
     *
     * @param e  any Throwable object
     *
     * @return a RuntimeException
     */
    public static RuntimeException ensureRuntimeException(Throwable e)
        {
        return ensureRuntimeException(e, null);
        }

    /**
     * Convert the passed exception to a RuntimeException if necessary.
     *
     * @param e  any Throwable object
     * @param s  an additional description
     *
     * @return a RuntimeException
     */
    public static RuntimeException ensureRuntimeException(Throwable e, String s)
        {
        if (e instanceof RuntimeException && (s == null || s.equals(e.getMessage())))
            {
            return (RuntimeException) e;
            }
        else
            {
            return WrapperException.ensure(e, s);
            }
        }

    // ----- utility methods----------------------------------------------------

    public WebTarget getBaseTarget()
        {
        return getBaseTarget(m_client);
        }

    public WebTarget getBaseTarget(Client client)
        {
        try
            {
            if (m_baseURI == null)
                {
                int nPort = Integer.getInteger("test.multicast.port", 7778);
                m_baseURI = NSLookup.lookupHTTPManagementURL(new InetSocketAddress("127.0.0.1", nPort)).iterator().next().toURI();

                CacheFactory.log("Management HTTP Acceptor lookup returned: " + m_baseURI, CacheFactory.LOG_INFO);
                }
            return client.target(m_baseURI);
            }
        catch(IOException | URISyntaxException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    protected void ensureServicesAreAvailable()
        {
        Eventually.assertDeferred(() ->
            {
            WebTarget servicesTarget = getBaseTarget().path(SERVICES);
            Response servicesResponse = servicesTarget.request().get();
            assertThat(servicesResponse.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(servicesResponse.getHeaderString("X-Content-Type-Options"), is("nosniff"));
            Map mapServicesResponse = readEntity(servicesTarget, servicesResponse);

            assertThat(mapServicesResponse, notNullValue());

            List<Map> services = (List<Map>) mapServicesResponse.get("items");

            assertThat(services, notNullValue());
            services.removeIf(serviceMap -> Arrays.stream(TOPICS_SERVICES_LIST).anyMatch(topicServiceName -> ((String) serviceMap.get(NAME)).contains(topicServiceName)));
            String serviceNames = services.stream().map(p -> (String) p.get("name"))
                    .collect(Collectors.joining(","));
            CacheFactory.log("The following " + services.size() + " services exist: " + serviceNames);

            CacheFactory.log("The following " + SERVICES_LIST.length + " services expected: " + String.join(",", SERVICES_LIST));

            return services.size();
            }, is(EXPECTED_SERVICE_COUNT), within(5, TimeUnit.MINUTES));
        }
        protected LinkedHashMap readEntity(WebTarget target, Response response)
                throws ProcessingException
            {
            return readEntity(target, response, null);
            }

        protected LinkedHashMap readEntity(WebTarget target, Response response, Entity entity)
                throws ProcessingException
            {
            int i = 0;
            while (true)
                {
                try
                    {
                    LinkedHashMap mapReturned = response.readEntity(LinkedHashMap.class);
                    if (mapReturned == null)
                        {
                        CacheFactory.log(getClass().getName() + ".readEntity() returned null"
                                + ", target: " + target + ", response: " + response,  CacheFactory.LOG_WARN);
                        }
                    else
                        {
                        return mapReturned;
                        }
                    }
                catch (ProcessingException | IllegalStateException e)
                    {
                    CacheFactory.log(getClass().getName() + ".readEntity() got an exception: " + e
                            + ", cause: " + e.getCause().getLocalizedMessage(), CacheFactory.LOG_WARN);
                    if (i > 1)
                        {
                        throw e;
                        }
                    }

                // try again
                if (entity == null)
                    {
                    response = target.request().get();
                    }
                else
                    {
                    response = target.request().post(entity);
                    }
                Assert.assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

                i++;
                }
            }

    protected static void assertClusterReady(CoherenceClusterMember member)
        {
        String sScope = member.getSystemProperty("test.scope.name");
        String sPrefix;
        if (sScope == null || sScope.isEmpty())
            {
            sPrefix = "";
            }
        else
            {
            sPrefix = sScope + ":";
            }

        Eventually.assertDeferred(() -> member.isServiceRunning(sPrefix + SERVICE_NAME), is(true), within(5, TimeUnit.MINUTES));
        Eventually.assertDeferred(() -> member.getServiceStatus(sPrefix + SERVICE_NAME), anyOf(is(ServiceStatus.NODE_SAFE), is(ServiceStatus.ENDANGERED)));
        Eventually.assertDeferred(() -> member.isServiceRunning(sPrefix + ACTIVE_SERVICE), is(true), within(3, TimeUnit.MINUTES));
        Eventually.assertDeferred(() -> member.getServiceStatus(sPrefix + ACTIVE_SERVICE), anyOf(is(ServiceStatus.NODE_SAFE), is(ServiceStatus.ENDANGERED)));
        }

    //--------------------- helper classes ----------------------------

    public static class RemoteStartService implements RemoteRunnable
        {
        public RemoteStartService(String sName, String sType)
            {
            m_sName = sName;
            m_sType = sType;
            }

        @Override
        public void run()
            {
            CacheFactory.getCluster().ensureService(m_sName, m_sType).start();
            }

        String m_sName;
        String m_sType;
        }

    // ----- static helpers -------------------------------------------------


    // ----- data members ------------------------------------------------------

    /**
     * The client object to be used for the tests.
     */
    protected static Client m_client;

    /**
     * The base URL for Management over REST requests.
     */
    protected static URI m_baseURI;

    /**
     * Active directory.
     */
    protected static File m_dirActive;

    /**
     * Snapshot directory.
     */
    protected static File m_dirSnapshot;

    /**
     * Archive directory.
     */
    protected static File m_dirArchive;

    /**
     * Temporary directory to store JFR files.
     */
    protected static File s_dirJFR;

    // ----- constants ------------------------------------------------------

    /**
     * The services path.
     */
    protected static final String SERVICES = "services";

    /**
     * The name of the active persistence service.
     */
    protected static final String ACTIVE_SERVICE = "DistributedCachePersistence";

    /**
     * The name of Distributed cache.
     */
    protected static final String CACHE_NAME = "dist-test";

    /**
     * The name of the used Extend proxy service.
     */
    protected static final String PROXY_SERVICE_NAME = "ExtendProxyService";

    /**
     * The name of the used PartitionedService.
     */
    protected static final String SERVICE_NAME = "DistributedCache";

    /**
     * The list of services used by this test class.
     */
    private static final String[] SERVICES_LIST = {SERVICE_NAME, PROXY_SERVICE_NAME,
            "DistributedCachePersistence", HttpHelper.getServiceName(),
            MetricsHttpHelper.getServiceName()};

    /**
     * The list of services used by topics.
     */
    private static final String[] TOPICS_SERVICES_LIST = {"TestTopicService"};

    /**
     * Prefix for the spawned processes.
     */
    protected static String SERVER_PREFIX = "testMgmtRESTServer";

    /**
     * Cache config used by the test and spawned processes.
     */
    protected static final String CACHE_CONFIG = "server-cache-config-mgmt.xml";

    /**
     * The expected number of services on the server, this is very brittle!
     * May be overridden.
     */
    protected int EXPECTED_SERVICE_COUNT = SERVICES_LIST.length;

    /**
     * Name of the Coherence cluster.
     */
    public static final String CLUSTER_NAME = "mgmtRestCluster";

    @Rule
    public final TestName m_testName = new TestName();

    @ClassRule
    public static final TestLogs m_testLogs = new TestLogs();

    /**
     * The count of cluster members.
     */
    public static final int MEMBER_COUNT = 2;

    protected static final String NEAR_CACHE_NAME = "near-test";

    /**
     * The cluster members.
     */
    protected static CoherenceClusterMember[] m_aMembers = new CoherenceClusterMember[MEMBER_COUNT];

    }
