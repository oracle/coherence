/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package executor;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;

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

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.concurrent.executor.ClusteredAssignment;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorInfo;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;
import com.oracle.coherence.concurrent.executor.ClusteredProperties;
import com.oracle.coherence.concurrent.executor.ClusteredTaskCoordinator;
import com.oracle.coherence.concurrent.executor.ClusteredTaskManager;
import com.oracle.coherence.concurrent.executor.ExecutorsHelper;
import com.oracle.coherence.concurrent.executor.TaskCollectors;
import com.oracle.coherence.concurrent.executor.TaskExecutorService;
import com.oracle.coherence.concurrent.executor.function.Predicates;
import com.oracle.coherence.concurrent.executor.subscribers.RecordingSubscriber;

import com.tangosol.coherence.management.internal.resources.AbstractManagementResource;

import com.tangosol.discovery.NSLookup;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.util.Base;

import executor.common.CoherenceClusterResource;
import executor.common.LogOutput;
import executor.common.LongRunningTask;
import executor.common.SingleClusterForAllTests;

import org.glassfish.jersey.jackson.JacksonFeature;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import org.hamcrest.core.Is;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
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

import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.MEMBERS;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.getParentUri;
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
@Category(SingleClusterForAllTests.class)
public class MBeanTests
    {
    // ----- test lifecycle -------------------------------------------------

    @AfterClass
    public static void afterClass()
        {
        s_coherence.after();
        }

    @BeforeClass
    public static void setupClass()
        {
        // ensure the cluster service is running
        s_coherence.getCluster();
        m_client = ClientBuilder.newBuilder()
                .register(JacksonFeature.class).build();
        }

    @After
    public void cleanup()
            throws Exception
        {
        if (m_taskExecutorService != null)
            {
            m_taskExecutorService.shutdown();

            // clear the caches between tests
            getNamedCache(ClusteredTaskManager.CACHE_NAME).clear();
            getNamedCache(ClusteredAssignment.CACHE_NAME).clear();
            }
        m_session.close();
        m_local.close();
        }

    @SuppressWarnings("rawtypes")
    @Before
    public void setup()
        {
        System.setProperty("coherence.cluster", "MBeanTests");
        m_local = Coherence.clusterMember(CoherenceConfiguration.builder().discoverSessions().build());
        m_local.start().join();
        m_session = m_local.getSession(ExecutorsHelper.SESSION_NAME);

        // establish an ExecutorService based on storage disabled (client) member
        m_taskExecutorService = new ClusteredExecutorService(m_session);

        // verify that there are getInitialExecutorCount() Executors available and that they are in the RUNNING state
        NamedCache executors = m_session.getCache(ClusteredExecutorInfo.CACHE_NAME);


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
        // run test to generate some activities
        failoverLongRunningTest();

        WebTarget target   = getBaseTarget().path(AbstractManagementResource.EXECUTORS);
        Response  response = target.request().get();

        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));
        MatcherAssert.assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Map mapResponse = response.readEntity(LinkedHashMap.class);
        assertThat(mapResponse, notNullValue());

        List<Map> listItems = (List<Map>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(1));

        String       sExecutorName = null;
        List<String> sMemberIds    = null;
        Map          mapItem       = listItems.get(0);

        sMemberIds = (List<String>) mapItem.get("memberId");
        assertThat(sMemberIds.size(), is(4));
        sExecutorName = ((List<String>) mapItem.get("name")).get(0);
        assertThat(sExecutorName, is("UnNamed"));
        int count = ((Integer) ((LinkedHashMap) mapItem.get("tasksCompletedCount")).get("sum")).intValue()
                    + ((Integer) ((LinkedHashMap) mapItem.get("tasksInProgressCount")).get("sum")).intValue();
        assertThat(count, greaterThan(0));

        // .../executors/<executorName>
        String sExecutorLink = getSelfLink(mapItem);

        response = m_client.target(sExecutorLink).request().get();
        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));
        mapResponse = response.readEntity(LinkedHashMap.class);
        assertThat(mapResponse, notNullValue());
        assertThat(((List<String>) mapResponse.get("name")).get(0), is("UnNamed"));

        // .../executors/members
        target = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(MEMBERS);
        response = target.request().get();
        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));

        mapResponse = response.readEntity(LinkedHashMap.class);
        listItems   = (List<Map>) mapResponse.get("items");
        assertThat(listItems.size(), is(4));

        for (Map item : listItems)
            {
            String sName = (String) item.get("name");

            response = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(sName).request().get();
            MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));

            mapResponse = response.readEntity(LinkedHashMap.class);
            assertThat(mapResponse, notNullValue());
            assertThat(mapResponse.size(), is(11));
            }

        // .../executors/members/<memberId>
        target = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(MEMBERS).path(sMemberIds.get(0));
        response = target.request().get();
        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));

        mapResponse = response.readEntity(LinkedHashMap.class);
        listItems   = (List<Map>) mapResponse.get("items");
        assertThat(listItems.size(), greaterThan(0));

        response = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(sExecutorName).path("resetStatistics")
                .request().post(null);
        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));

        response = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(sExecutorName).request().get();
        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));
        mapResponse = response.readEntity(LinkedHashMap.class);

        LinkedHashMap tasksCompletedCount = (LinkedHashMap) mapResponse.get("tasksCompletedCount");
        assertThat(((Integer) tasksCompletedCount.get("sum")).intValue(), is(0));

        response = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(sExecutorName).request().post(
                    Entity.entity(new LinkedHashMap(){{put("traceLogging", false);}}, MediaType.APPLICATION_JSON_TYPE));
        MatcherAssert.assertThat(response.getStatus(), CoreMatchers.is(Response.Status.OK.getStatusCode()));
        Base.sleep(5000);        // wait for the change to take effect

        response = getBaseTarget().path(AbstractManagementResource.EXECUTORS).path(sExecutorName).request().get();
        MatcherAssert.assertThat(response.getStatus(), Is.is(Response.Status.OK.getStatusCode()));

        mapResponse = response.readEntity(LinkedHashMap.class);
        List<Boolean> traceLogging = (List<Boolean>) mapResponse.get("traceLogging");
        assertThat(traceLogging.get(0).booleanValue(), is(false));
        }

    // ----- helper methods -------------------------------------------------

    public void failoverLongRunningTest()
        {
        CoherenceCluster            cluster    = s_coherence.getCluster();
        RecordingSubscriber<String> subscriber = new RecordingSubscriber<>();

        // start a long-running task on a client member
        final String taskName = "longRunningTask";
        ClusteredTaskCoordinator coordinator = (ClusteredTaskCoordinator) m_taskExecutorService.orchestrate(new LongRunningTask(Duration.ofSeconds(30)))
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

        // make sure the task is failed over to the new member and the subscriber received the result
        assertThat(properties.get("key1"), Matchers.is("value1"));
        Eventually.assertDeferred(() -> subscriber.received("DONE"), Matchers.is(true));
        }

    // ----- helper methods -------------------------------------------------

    public <K, V> NamedCache<K, V> getNamedCache(String sName)
        {
        return m_taskExecutorService.getCacheService().ensureCache(sName, null);
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
                int nPort = Integer.getInteger("test.multicast.port", 7777);
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

    private String getSelfLink(Map mapResponse)
        {
        return getLink(mapResponse, "self");
        }

    private String getLink(Map mapResponse, String sLinkName)
        {
        List<LinkedHashMap> linksMap =  (List) mapResponse.get("links");
        Assert.assertThat(linksMap, CoreMatchers.notNullValue());
        LinkedHashMap selfLinkMap = linksMap.stream().filter(m -> m.get("rel").
                equals(sLinkName))
                .findFirst()
                .orElse(new LinkedHashMap());

        String sSelfLink = (String) selfLinkMap.get("href");
        Assert.assertThat(sSelfLink, CoreMatchers.notNullValue());
        return sSelfLink;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The number of storage enabled members in the {@link CoherenceCluster}.
     */
    protected final static int STORAGE_ENABLED_MEMBER_COUNT = 2;

    /**
     * The number of storage disabled members in the {@link CoherenceCluster}.
     */
    protected final static int STORAGE_DISABLED_MEMBER_COUNT = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The {@link CoherenceClusterResource} to establish a {@link CoherenceCluster} for testing.
     */
    @ClassRule
    public static CoherenceClusterResource s_coherence =
            (CoherenceClusterResource) new CoherenceClusterResource()
                    .with(ClassName.of(Coherence.class),
                          Multicast.ttl(0),
                          LocalHost.only(),
                          Logging.at(9),
                          ClusterPort.of(7574),
                          ClusterName.of(MBeanTests.class.getSimpleName()),
                          JmxFeature.enabled())
                    .include(STORAGE_ENABLED_MEMBER_COUNT,
                             DisplayName.of("CacheServer"),
                             LogOutput.to(MBeanTests.class.getSimpleName(), "CacheServer"),
                             RoleName.of("storage"),
                             LocalStorage.enabled(),
                             JMXManagementMode.ALL,
                             SystemProperty.of("coherence.executor.extend.enabled", false),
                             SystemProperty.of("coherence.executor.trace.logging", true),
                             SystemProperty.of("coherence.management.http", "inherit"),
                             SystemProperty.of("coherence.management.http.port", "0"))
                    .include(STORAGE_DISABLED_MEMBER_COUNT,
                             DisplayName.of("ComputeServer"),
                             LogOutput.to(MBeanTests.class.getSimpleName(), "ComputeServer"),
                             RoleName.of("compute"),
                             LocalStorage.disabled(),
                             SystemProperty.of("coherence.executor.extend.enabled", false),
                             SystemProperty.of("coherence.executor.trace.logging", true));

    /**
     * The Coherence object to be used for the tests.
     */
    protected Coherence m_local;

    /**
     * The Session object to be used for the tests.
     */
    protected Session   m_session;

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
