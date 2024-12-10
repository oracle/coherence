/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;

import com.oracle.bedrock.runtime.coherence.ServiceStatus;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

import com.oracle.bedrock.runtime.java.features.JmxFeature;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.oracle.coherence.concurrent.executor.ClusteredExecutorInfo;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;
import com.oracle.coherence.concurrent.executor.ClusteredProperties;
import com.oracle.coherence.concurrent.executor.ClusteredTaskCoordinator;
import com.oracle.coherence.concurrent.executor.RemoteExecutor;
import com.oracle.coherence.concurrent.executor.TaskCollectors;
import com.oracle.coherence.concurrent.executor.TaskExecutorService;
import com.oracle.coherence.concurrent.executor.function.Predicates;
import com.oracle.coherence.concurrent.executor.subscribers.RecordingSubscriber;

import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.coherence.management.internal.MapProvider;

import com.tangosol.discovery.NSLookup;

import com.tangosol.internal.management.resources.AbstractManagementResource;

import com.tangosol.internal.net.management.HttpHelper;

import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import executor.common.CoherenceClusterResource;
import executor.common.LogOutput;
import executor.common.LongRunningTask;
import executor.common.SingleClusterForAllTests;

import executor.common.Utils;

import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import org.hamcrest.core.Is;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.junit.experimental.categories.Category;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import java.time.Duration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static com.tangosol.util.Base.ensureRuntimeException;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

import static org.hamcrest.core.Is.is;

/**
 * Integration Tests for the {@link com.oracle.coherence.concurrent.executor.management.ExecutorMBean}.
 *
 * @author lh
 * @since 21.12
 */
@SuppressWarnings("resource")
@Category(SingleClusterForAllTests.class)
public class ExecutorRESTManagementTests
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void setupClass()
        {
        // ensure the cluster service is running
        CoherenceCluster cluster = s_coherence.getCluster();
        Eventually.assertDeferred(cluster::getClusterSize, is(STORAGE_ENABLED_MEMBER_COUNT + STORAGE_DISABLED_MEMBER_COUNT));

        Eventually.assertDeferred(() -> assertClusterReady(cluster), is(true));

        m_client = ClientBuilder.newBuilder()
                .register(MapProvider.class).build();

        }

    @After
    public void cleanup()
            throws Exception
        {
        if (m_taskExecutorService != null)
            {
            m_taskExecutorService.shutdown();

            // clear the caches between tests
            CacheService service = getCacheService();
            Caches.tasks(service).clear();
            Caches.assignments(service).clear();
            }
        Coherence.closeAll();
        }

    @SuppressWarnings("rawtypes")
    @Before
    public void setup() throws Exception
        {
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.distributed.localstorage", "false");
        Coherence coherence = Coherence.clusterMember(CoherenceConfiguration.builder().discoverSessions().build());
        coherence.start().get(5, TimeUnit.MINUTES);
        Session session = coherence.getSession(ConcurrentServicesSessionConfiguration.SESSION_NAME);

        // establish an ExecutorService based on storage disabled (client) member
        m_taskExecutorService = new ClusteredExecutorService(session);

        // verify that there are getInitialExecutorCount() Executors available and that they are in the RUNNING state
        NamedCache executors = Caches.executors(session);

        Eventually.assertDeferred(executors::size, is(getInitialExecutorCount()));

        for (Object key : executors.keySet())
            {
            Eventually.assertDeferred(() -> getExecutorServiceInfo(executors, (String) key).getState(),
                                      Is.is(TaskExecutorService.ExecutorInfo.State.RUNNING));
            }
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testExecutors()
        {
        Utils.assertWithFailureAction(this::doTestExecutors, this::dumpExecutorCacheStates);
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings("unchecked")
    protected Boolean getTraceLoggingConfig(String sExecutorName)
        {
        Response response = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(sExecutorName).request().get();
        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));

        Map<String, Object> mapResponse = response.readEntity(LinkedHashMap.class);
        List<Boolean> traceLogging = (List<Boolean>) mapResponse.get("traceLogging");

        return traceLogging != null && traceLogging.size() > 0 ? traceLogging.get(0) : false;
        }

    @SuppressWarnings("unchecked")
    protected void doTestExecutors()
        {
        // run test to generate some activities
        failoverLongRunningTest();

        WebTarget target   = getBaseTarget().path(AbstractManagementResource.EXECUTORS);
        Response  response = target.request().get();

        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));
        MatcherAssert.assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Map<String, Object> mapResponse = response.readEntity(LinkedHashMap.class);
        assertThat(mapResponse, notNullValue());

        List<Map<String, Object>> listItems = (List<Map<String, Object>>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(1));

        Map<String, Object> mapItem = listItems.get(0);
        String              sExecutorName;
        List<String>        sMemberIds;

        sMemberIds = (List<String>) mapItem.get("memberId");
        assertThat(sMemberIds.size(), is(4));
        sExecutorName = ((List<String>) mapItem.get("name")).get(0);
        assertThat(sExecutorName, is(RemoteExecutor.DEFAULT_EXECUTOR_NAME));
        int count = ((Number) ((Map<String, Object>) mapItem.get("tasksCompletedCount")).get("sum")).intValue()
                    + ((Number) ((Map<String, Object>) mapItem.get("tasksInProgressCount")).get("sum")).intValue();
        assertThat(count, greaterThan(0));

        // .../executors/<executorName>
        String sExecutorLink = getSelfLink(mapItem);

        response = m_client.target(sExecutorLink).request().get();
        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));
        mapResponse = response.readEntity(LinkedHashMap.class);
        assertThat(mapResponse, notNullValue());
        assertThat(((List<String>) mapResponse.get("name")).get(0), is(RemoteExecutor.DEFAULT_EXECUTOR_NAME));

        // .../executors/members
        target = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(AbstractManagementResource.MEMBERS);
        response = target.request().get();
        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));

        mapResponse = response.readEntity(LinkedHashMap.class);
        listItems   = (List<Map<String, Object>>) mapResponse.get("items");
        assertThat(listItems.size(), is(4));

        for (Map<String, Object> item : listItems)
            {
            String sName = (String) item.get("name");

            response = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(sName).request().get();
            MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));

            mapResponse = response.readEntity(LinkedHashMap.class);
            assertThat(mapResponse, notNullValue());
            assertThat(mapResponse.size(), is(12));
            }

        // .../executors/members/<memberId>
        target = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(AbstractManagementResource.MEMBERS).path(sMemberIds.get(0));
        response = target.request().get();
        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));

        mapResponse = response.readEntity(LinkedHashMap.class);
        listItems   = (List<Map<String, Object>>) mapResponse.get("items");
        assertThat(listItems.size(), greaterThan(0));

        response = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(sExecutorName).path("resetStatistics")
                .request().post(null);
        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));

        response = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(sExecutorName).request().get();
        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));
        mapResponse = response.readEntity(LinkedHashMap.class);

        Map<String, Object> tasksCompletedCount = (Map<String, Object>) mapResponse.get("tasksCompletedCount");
        assertThat(((Number) tasksCompletedCount.get("sum")).intValue(), is(0));

        Map<String, Boolean> mapBody = new LinkedHashMap<>();
        mapBody.put("traceLogging", false);

        response = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(sExecutorName).request().post(
                Entity.entity(mapBody, MediaType.APPLICATION_JSON_TYPE));

        MatcherAssert.assertThat(response.getStatus(), CoreMatchers.is(Response.Status.OK.getStatusCode()));

        Eventually.assertDeferred(() -> getTraceLoggingConfig(sExecutorName), is(false));
        }

    public void failoverLongRunningTest()
        {
        CoherenceCluster            cluster    = s_coherence.getCluster();
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        // start a long-running task on a client member
        String taskName = "longRunningTask";
        ClusteredTaskCoordinator<String> coordinator = (ClusteredTaskCoordinator<String>) m_taskExecutorService
            .orchestrate(new LongRunningTask(Duration.ofSeconds(30)))
            .as(taskName)
            .filter(Predicates.role("compute"))
            .limit(1)
            .collect(TaskCollectors.lastOf())
            .subscribe(subscriber)
            .submit();

        // verify that the task has started
        ClusteredProperties properties = (ClusteredProperties) coordinator.getProperties();
        Eventually.assertDeferred(() -> properties.get("count"), notNullValue());

        properties.put("key1", "value1");

        // now restart the storage disabled member
        cluster.filter(member -> member.getRoleName().equals("Compute")).relaunch();
        AbstractClusteredExecutorServiceTests.ensureConcurrentServiceRunning(cluster);

        // make sure the task is failed over to the new member and the subscriber received the result
        assertThat(properties.get("key1"), Matchers.is("value1"));
        Eventually.assertDeferred(() -> subscriber.received("DONE"),
                                  Matchers.is(true),
                                  Eventually.within(3, TimeUnit.MINUTES));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Dump current executor cache states.
     */
    protected void dumpExecutorCacheStates()
        {
        CacheService service = getCacheService();

        Utils.dumpExecutorCacheStates(Caches.executors(service),
                                      Caches.assignments(service),
                                      Caches.tasks(service),
                                      Caches.properties(service));

        //Utils.heapdump(s_coherence.getCluster());
        }

    public CacheService getCacheService()
        {
        return m_taskExecutorService.getCacheService();
        }

    protected int getInitialExecutorCount()
        {
        return STORAGE_ENABLED_MEMBER_COUNT + STORAGE_DISABLED_MEMBER_COUNT + 1;
        }

    /**
     * Obtains the {@link ClusteredExecutorInfo} for the specified {@link Executor}
     * from the {@link TaskExecutorService.ExecutorInfo} {@link NamedCache}.
     *
     * @param executorInfoCache  the {@link NamedCache}
     * @param executorId         the {@link ExecutorService} identity
     *
     * @return the {@link ClusteredExecutorInfo} or <code>null</code> if it doesn't exist
     */
    @SuppressWarnings("rawtypes")
    public ClusteredExecutorInfo getExecutorServiceInfo(NamedCache executorInfoCache,
                                                        String     executorId)
        {
        return (ClusteredExecutorInfo) executorInfoCache.get(executorId);
        }

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

                Logger.info("Management HTTP Acceptor lookup returned: " + m_baseURI);
                }
            return client.target(m_baseURI);
            }
        catch(IOException | URISyntaxException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    private String getSelfLink(Map<String, Object> mapResponse)
        {
        return getLink(mapResponse, "self");
        }

    @SuppressWarnings("unchecked")
    private String getLink(Map<String, Object> mapResponse, String sLinkName)
        {
        List<Map<String, Object>> linksMap =  (List<Map<String, Object>>) mapResponse.get("links");

        assertThat(linksMap, CoreMatchers.notNullValue());

        Map<String, Object> selfLinkMap = linksMap.stream().filter(m -> m.get("rel").
                equals(sLinkName))
                .findFirst()
                .orElse(new LinkedHashMap<>());

        String sSelfLink = (String) selfLinkMap.get("href");
        assertThat(sSelfLink, CoreMatchers.notNullValue());
        return sSelfLink;
        }

    private static boolean assertClusterReady(CoherenceCluster cluster)
        {
        for (CoherenceClusterMember member : cluster)
            {
            if (ServiceStatus.RUNNING.equals(member.getServiceStatus(HttpHelper.getServiceName())))
                {
                return true;
                }
            }
        return false;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The number of storage enabled members in the {@link CoherenceCluster}.
     */
    protected static final int STORAGE_ENABLED_MEMBER_COUNT = 2;

    /**
     * The number of storage disabled members in the {@link CoherenceCluster}.
     */
    protected static final int STORAGE_DISABLED_MEMBER_COUNT = 1;

    protected static final String CLUSTER_NAME = ExecutorRESTManagementTests.class.getSimpleName();

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static final TestLogs s_testLogs = new TestLogs(ExecutorRESTManagementTests.class);

    /**
     * The {@link CoherenceClusterResource} to establish a {@link CoherenceCluster} for testing.
     */
    @ClassRule
    public static final CoherenceClusterResource s_coherence =
            (CoherenceClusterResource) new CoherenceClusterResource()
                    .with(ClassName.of(Coherence.class),
                          Multicast.ttl(0),
                          LocalHost.only(),
                          Logging.at(9),
                          ClusterPort.of(7574),
                          ClusterName.of(CLUSTER_NAME),
                          JmxFeature.enabled(),
                          s_testLogs,
                          StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()))
                    .include(STORAGE_ENABLED_MEMBER_COUNT,
                             DisplayName.of("CacheServer"),
                             LogOutput.to(CLUSTER_NAME, "CacheServer"),
                             RoleName.of("storage"),
                             LocalStorage.enabled(),
                             JMXManagementMode.ALL,
                             SystemProperty.of("coherence.executor.extend.enabled", false),
                             SystemProperty.of("coherence.executor.trace.logging", true),
                             SystemProperty.of("coherence.management.http", "inherit"),
                             SystemProperty.of("coherence.management.http.port", "0"))
                    .include(STORAGE_DISABLED_MEMBER_COUNT,
                             DisplayName.of("ComputeServer"),
                             LogOutput.to(CLUSTER_NAME, "ComputeServer"),
                             RoleName.of("compute"),
                             LocalStorage.disabled(),
                             SystemProperty.of("coherence.executor.extend.enabled", false),
                             SystemProperty.of("coherence.executor.trace.logging", true));

    /**
     * The Client object to be used for the tests.
     */
    protected static Client m_client;

    /**
     * The base URL for Management over REST requests.
     */
    protected static URI m_baseURI;

    /**
     * The {@link TaskExecutorService}.
     */
    protected ClusteredExecutorService m_taskExecutorService;
    }
