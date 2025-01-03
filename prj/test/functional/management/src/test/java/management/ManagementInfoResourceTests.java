/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.features.JmxFeature;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.management.internal.resources.AbstractManagementResource;
import com.tangosol.coherence.management.internal.resources.ClusterMemberResource;
import com.tangosol.coherence.management.internal.resources.ClusterResource;
import com.tangosol.discovery.NSLookup;

import com.tangosol.internal.net.management.HttpHelper;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;

import common.AbstractFunctionalTest;

import java.io.File;
import java.io.IOException;

import java.io.UnsupportedEncodingException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.JacksonFeature;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import test.CheckJDK;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.CACHES;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.CLEAR;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.DESCRIPTION;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.MANAGEMENT;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.MEMBER;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.MEMBERS;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.METADATA_CATALOG;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.NAME;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.NODE_ID;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.OPTIONS;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.REPORTERS;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.ROLE_NAME;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.SERVICE;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.STORAGE;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.TRUNCATE;
import static com.tangosol.coherence.management.internal.resources.ClusterMemberResource.DIAGNOSTIC_CMD;
import static com.tangosol.coherence.management.internal.resources.ClusterResource.DUMP_CLUSTER_HEAP;
import static com.tangosol.coherence.management.internal.resources.ClusterResource.ROLE;
import static com.tangosol.coherence.management.internal.resources.ClusterResource.TRACING_RATIO;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.isOneOf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * MBeanResourceTest tests the ManagementInfoResource.
 *
 * <p>
 * ﻿In general, if we only want to assert that an attribute value is set
 * (not the default -1), but not what the value is, then use asserts
 * similar to the following:
 *
 * assertThat(((Number) mapResponse.get("requestTotalCount")).intValue(), greaterThanOrEqualTo(0));
 * ﻿assertThat(Long.parseLong(mapResponse.get("requestTotalCount").toString()), greaterThanOrEqualTo(0L));
 *
 * @author hr 2016.07.21
 * @author sr 2017.08.24
 */
@SuppressWarnings("unchecked")
public class ManagementInfoResourceTests
        extends AbstractFunctionalTest
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
        propsServer1.setProperty("coherence.cluster", CLUSTER_NAME);
        propsServer1.setProperty("coherence.management.extendedmbeanname", "true");
        propsServer1.setProperty("coherence.member", SERVER_PREFIX + "-1");
        propsServer1.setProperty("coherence.management.http", "inherit");
        propsServer1.setProperty("coherence.management.http.override-port", "0");
        propsServer1.setProperty("coherence.management.readonly", Boolean.toString(isReadOnly()));

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

        Properties propsServer2 = new Properties();
        propsServer2.putAll(propsServer1);
        propsServer2.setProperty("coherence.member", SERVER_PREFIX + "-2");
        CoherenceClusterMember member2 = startCacheServer(SERVER_PREFIX + "-2", "rest", CACHE_CONFIG, propsServer2);

        Eventually.assertThat(invoking(member2).getServiceStatus(SERVICE_NAME), is(ServiceStatus.NODE_SAFE), within(5, TimeUnit.MINUTES));

        Eventually.assertThat(invoking(member2).getServiceStatus(ACTIVE_SERVICE), is(ServiceStatus.NODE_SAFE), within(3, TimeUnit.MINUTES));
        Eventually.assertDeferred(() -> member2.invoke(new CalculateUnbalanced("dist-persistence-test")),
                Matchers.is(0),
                within(3, TimeUnit.MINUTES));

        // fill a cache
        NamedCache cache    = findApplication(SERVER_PREFIX + "-1").getCache(CACHE_NAME);
        Binary     binValue = Binary.getRandomBinary(1024, 1024);
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

    // ----- tests ----------------------------------------------------------

    @Test
    public void testSwagger()
        {
        WebTarget target   = getBaseTarget().path(METADATA_CATALOG);
        Response  response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = readEntity(target, response);

        // cherry pick a few items that must exist
        assertThat(mapResponse, is(notNullValue()));
        assertThat(mapResponse.get("swagger"), is(notNullValue()));
        assertThat(mapResponse.get("info"), is(notNullValue()));
        assertThat(mapResponse.get("schemes"), is(notNullValue()));
        assertThat(mapResponse.get("tags"), is(notNullValue()));
        assertThat(mapResponse.get("paths"), is(notNullValue()));
        assertThat(mapResponse.get("definitions"), is(notNullValue()));
        assertThat(mapResponse.get("parameters"), is(notNullValue()));
        }

    @Test
    public void testClusterInfo()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("clusterName"), is(CLUSTER_NAME));
        assertThat(mapResponse.get("running"), is(true));
        assertThat(mapResponse.get("membersDepartureCount"), is(0));

        Object objListMemberIds = mapResponse.get("memberIds");
        assertThat(objListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) objListMemberIds;
        assertThat(listMemberIds.size(), is(MEMBER_COUNT));

        Object objListLinks = mapResponse.get("links");
        assertThat(objListLinks, instanceOf(List.class));

        List<LinkedHashMap> listLinks = (List) objListLinks;

        Set<Object> linkNames = listLinks.stream().map(m -> m.get("rel")).collect(Collectors.toSet());
        assertThat(linkNames, hasItem("self"));
        assertThat(linkNames, hasItem("canonical"));
        assertThat(linkNames, hasItem("parent"));
        assertThat(linkNames, hasItem(MEMBERS));
        assertThat(linkNames, hasItem(SERVICES));
        assertThat(linkNames, hasItem(CACHES));
        }

    @Test
    public void testGetClusterConfig()
            throws Exception
        {
        Eventually.assertDeferred(
                () -> getBaseTarget().path(ClusterResource.GET_CLUSTER_CONFIG).request().get().getStatus(),
                is(Response.Status.OK.getStatusCode()));

        WebTarget target   = getBaseTarget().path(ClusterResource.GET_CLUSTER_CONFIG);
        Response  response = target.request().get();
        assertNotNull(response);

        String sResponse = (String) readEntity(target, response, String.class);
        assertTrue(sResponse.startsWith("<cluster-config"));
        }

    @Test
    public void testClusterDescription()
        {
        WebTarget target   = getBaseTarget().path(DESCRIPTION);
        Response  response = target.request().get();

        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        MatcherAssert.assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        MatcherAssert.assertThat(((String) (((Map) readEntity(target, response, Map.class)).get(DESCRIPTION)))
                                         .startsWith("SafeCluster: Name=" + CLUSTER_NAME + ", ClusterPort="),
                                 is(true));
        }

    @Test
    public void testClusterMemberPlatformMemory()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        Object oListMemberIds = mapResponse.get("memberIds");
        assertThat(oListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) oListMemberIds;

        for (Object memberId : listMemberIds)
            {
            target = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path("memory");
            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = readEntity(target, response);
            LinkedHashMap mapHeapUsage = (LinkedHashMap) mapResponse.get("heapMemoryUsage");
            assertThat(mapHeapUsage, notNullValue());
            assertThat((int) mapHeapUsage.get("used"), greaterThan(1));
            }
        }

    @Test
    public void testClusterMemberPlatformMemoryPoolTypeAttribute()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        Object oListMemberIds = mapResponse.get("memberIds");
        assertThat(oListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) oListMemberIds;

        String   GC_PREFIX      = (s_bTestJdk11 || s_bIsEPP) ? "g1" : "ps";
        String[] arr_sMbeanName = { GC_PREFIX + "OldGen", GC_PREFIX + "SurvivorSpace"};

        for (String mbean : arr_sMbeanName)
            {
            for (Object memberId : listMemberIds)
                {
                target = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path(mbean);
                response = target.request().get();
                assertThat("unexpected response for Mgmt over REST API " + getBaseTarget().getUri().toString() + "/" + MEMBERS + "/" + memberId.toString() + "/platform/" + mbean, response.getStatus(), is(Response.Status.OK.getStatusCode()));
                mapResponse = readEntity(target, response);
                String sTypeValue = (String) mapResponse.get("type");
                assertThat(sTypeValue, is("HEAP"));
                }
            }
        }

    @Test
    public void testClusterMemberByName()
        {
        Response response = getBaseTarget().path(MEMBERS).path(SERVER_PREFIX  + "-1").request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testNonExistentClusterMember()
        {
        Response response = getBaseTarget().path(MEMBERS).path("nonexistent").request().get();
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testAllPlatformMbeans()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        Object objListMemberIds = mapResponse.get("memberIds");
        assertThat(objListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) objListMemberIds;
        Object memberId = listMemberIds.get(0);

        for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_URL_TO_MBEAN_QUERY.keySet())
            {
            target = getBaseTarget().path("platform").path(platformMBean);

            CacheFactory.log(target.getUri().toString(), LOG_INFO);

            response = target.request().get();
            assertThat(target.getUri().toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = readEntity(target, response);
            assertThat(mapResponse.size(), greaterThan(0));
            }

        if (isG1())
            {
            for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_G1_URL_TO_MBEAN_QUERY.keySet())
                {
                target = getBaseTarget().path("platform").path(platformMBean);

                CacheFactory.log(target.getUri().toString(), LOG_INFO);

                response = target.request().get();
                assertThat(target.getUri().toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
                mapResponse = readEntity(target, response);
                assertThat(mapResponse.size(), greaterThan(0));
                }
            }
        else
            {
            for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_PS_URL_TO_MBEAN_QUERY.keySet())
                {
                target = getBaseTarget().path("platform").path(platformMBean);

                CacheFactory.log(target.getUri().toString(), LOG_INFO);

                response = target.request().get();
                assertThat(target.getUri().toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
                mapResponse = readEntity(target, response);
                assertThat(mapResponse.size(), greaterThan(0));
                }
            }

        for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_URL_TO_MBEAN_QUERY.keySet())
            {
            target = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path(platformMBean);

            CacheFactory.log(target.getUri().toString(), LOG_INFO);

            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = readEntity(target, response);
            assertThat(mapResponse.size(), greaterThan(0));
            }

        if (isG1())
            {
            for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_G1_URL_TO_MBEAN_QUERY.keySet())
                {
                target = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path(platformMBean);

                CacheFactory.log(target.getUri().toString(), LOG_INFO);

                response = target.request().get();

                // g1CodeCacheManager or g1MetaSpaceManager is not available on EPP (Enterprise Performance Pack)
                if (s_bIsEPP && ("g1CodeCacheManager".equals(platformMBean)
                		|| ("g1MetaSpaceManager").equals(platformMBean)))
                    {
                    continue;
                    }

                assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
                mapResponse = readEntity(target, response);
                assertThat(mapResponse.size(), greaterThan(0));
                }
            }
        else
            {
            for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_PS_URL_TO_MBEAN_QUERY.keySet())
                {
                target = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path(platformMBean);

                CacheFactory.log(target.getUri().toString(), LOG_INFO);

                response = target.request().get();
                assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
                mapResponse = readEntity(target, response);
                assertThat(mapResponse.size(), greaterThan(0));
                }
            }
        }

    @Test
    public void testNetworkStats()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        Object objListMemberIds = mapResponse.get("memberIds");
        assertThat(objListMemberIds, instanceOf(List.class));

        List   listMemberIds = (List) objListMemberIds;
        Object oMemberId     = listMemberIds.get(0);

        target = getBaseTarget().path(MEMBERS).path(oMemberId.toString()).path("networkStats");
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        mapResponse = readEntity(target, response);

        assertThat(mapResponse.size(), greaterThan(0));
        assertThat(mapResponse.get("publisherSuccessRate"), notNullValue());
        assertThat(mapResponse.get("threshold"), notNullValue());
        }

    @Test
    public void testTrackWeakest()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        Object objListMemberIds = mapResponse.get("memberIds");
        assertThat(objListMemberIds, instanceOf(List.class));

        List   listMemberIds = (List) objListMemberIds;
        Object oMemberId    = listMemberIds.get(0);

        response = getBaseTarget().path(MEMBERS).path(oMemberId.toString()).path("networkStats").path("trackWeakest")
                .request().post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testQueryFields()
        {
        WebTarget target   = getBaseTarget().queryParam("fields", "running");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("clusterName"), nullValue());
        assertThat(mapResponse.get("running"), is(true));
        assertThat(mapResponse.size(), is(2));
        }

    @Test
    public void testJmxManagement()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        Object oListLinks = mapResponse.get("links");
        assertThat(oListLinks, instanceOf(List.class));

        List<LinkedHashMap> listLinks = (List) oListLinks;

        LinkedHashMap mapJmxManagement = listLinks.stream().filter(m -> m.get("rel").equals(MANAGEMENT))
                .findFirst() .orElse(new LinkedHashMap());
        String sJmxURl = (String) mapJmxManagement.get("href");

        target = m_client.target(sJmxURl);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        mapResponse = readEntity(target, response);
        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("refreshOnQuery"), is(false));
        assertThat(mapResponse.get("expiryDelay"), is(1000));
        assertThat(mapResponse.get("refreshPolicy"), is("refresh-ahead"));
        }

    @Test
    public void testUpdateJmxManagementError()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put("refreshPolicy", "nonExistent");
        WebTarget target   = getBaseTarget().path(MANAGEMENT);
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response, entity));

        List<LinkedHashMap> listMessages = (List) mapResponse.get("messages");
        assertThat(listMessages, notNullValue());
        assertThat(listMessages.size(), is(1));

        LinkedHashMap mapMessage = listMessages.get(0);
        assertThat(mapMessage.get("field"), is("refreshPolicy"));
        }

    @Test
    public void testUpdateJmxManagement()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put("expiryDelay", 2000L);
        mapEntity.put("refreshPolicy", "refresh-behind");
        WebTarget target   = getBaseTarget().path(MANAGEMENT);
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request(MediaType.APPLICATION_JSON_TYPE).post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        LinkedHashMap mapResonse = new LinkedHashMap(readEntity(target, response, entity));

        List<LinkedHashMap> listMessages = (List) mapResonse.get("messages");
        assertThat(listMessages, nullValue());

        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        mapResonse = readEntity(target, response);
        assertThat(mapResonse.get("expiryDelay"), is(2000));
        assertThat(mapResonse.get("refreshPolicy"), is("refresh-behind"));
        }

    @Test
    public void testInvalidExpiryDelayValueFailure()
        {
        LinkedHashMap mapEntity = new LinkedHashMap();

        // providing an invalid value for expiryDelay that results in NumberFormatException when converting to long
        mapEntity.put("expiryDelay", "834958439085904385043985043985");
        mapEntity.put("refreshPolicy", "refresh-behind");

        WebTarget target   = getBaseTarget().path(MANAGEMENT);
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request(MediaType.APPLICATION_JSON_TYPE).post(entity);

        // confirm invalid request results in a BAD_REQUEST
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testHeapDump()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Response response = getBaseTarget().path(DUMP_CLUSTER_HEAP).request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testHeapDumpWithRole()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put(ROLE, "storage");
        Response response = getBaseTarget().path("dumpClusterHeap").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testLogClusterState()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Response response = getBaseTarget().path("logClusterState").request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testConfigureTracing()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put(ROLE, "");
        mapEntity.put(TRACING_RATIO, 1.0f);
        Response response = getBaseTarget().path("configureTracing").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testNonExistentOperation()
        {
        Response response = getBaseTarget().path("nonExistent").request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);

        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testMembers()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapEntity = readEntity(target, response);

        String sJmxURl = getLink(mapEntity, "members");

        target = m_client.target(sJmxURl);
        response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        LinkedHashMap mapResponse = readEntity(target, response);
        assertThat(mapEntity, notNullValue());

        List<LinkedHashMap> listMemberMaps = (List<LinkedHashMap>) mapResponse.get("items");

        assertThat(listMemberMaps, notNullValue());

        assertThat(listMemberMaps.size(), is(MEMBER_COUNT));

        for (LinkedHashMap mapMember : listMemberMaps)
            {
            Object oId = mapMember.get("id");
            assertThat(mapMember.get("id"), is(notNullValue()));
            assertThat(mapMember.get("roleName"), is(notNullValue()));
            assertThat(mapMember.get(NODE_ID), is(notNullValue()));

            Object oMemberLinks = mapMember.get("links");
            assertThat(oMemberLinks, instanceOf(List.class));

            target = m_client.target(getSelfLink(mapMember));
            response = target.request().get();

            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            mapResponse = readEntity(target, response);
            assertThat(mapResponse.get("id"), is(oId));
            }
        }

    @Test
    public void testMemberLogState()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        ArrayList<Integer> listMemberIds = (ArrayList<Integer>) mapResponse.get("memberIds");

        assertThat(listMemberIds, notNullValue());
        assertThat(listMemberIds.size(), greaterThan(0));

        int nMemberId = listMemberIds.get(0);

        response = getBaseTarget().path("members").path(nMemberId + "")
                .path("logMemberState").request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testMemberDescription()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        MatcherAssert.assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        List<Number> listMemberIds = (List<Number>) mapResponse.get("memberIds");

        MatcherAssert.assertThat(listMemberIds, notNullValue());
        MatcherAssert.assertThat(listMemberIds.size(), greaterThan(0));

        Number nMemberId = listMemberIds.get(0);

        target = getBaseTarget().path("members").path(String.valueOf(nMemberId)).path(DESCRIPTION);
        response = target.request(MediaType.APPLICATION_JSON_TYPE).get();

        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        MatcherAssert.assertThat(((String) ((Map) readEntity(target, response, Map.class)).get(DESCRIPTION))
                                         .startsWith("Member(Id=" + nMemberId),
                                 is(true));
        }
    @Test
    public void testMemberDumpHeap()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        ArrayList<Integer> listMemberIds = (ArrayList<Integer>) mapResponse.get("memberIds");

        assertThat(listMemberIds, notNullValue());
        assertThat(listMemberIds.size(), greaterThan(0));

        int nMemberId = listMemberIds.get(0);

        response = getBaseTarget().path("members").path(nMemberId + "")
            .path(ClusterMemberResource.MEMBER_DUMP_HEAP).request(MediaType.APPLICATION_JSON_TYPE)
            .post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testMemberResetStats()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        ArrayList<Integer> listMemberIds = (ArrayList<Integer>) mapResponse.get("memberIds");

        assertThat(listMemberIds, notNullValue());
        assertThat(listMemberIds.size(), greaterThan(0));

        int nMemberId = listMemberIds.get(0);

        response = getBaseTarget().path("members").path(nMemberId + "")
                .path("resetStatistics").request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testJfrWithAllNodes()
            throws Exception
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        // This test requires Flight Recorder and only runs on Oracle JVMs
        CheckJDK.assumeOracleJDK();

        WebTarget target   = getBaseTarget().path(DIAGNOSTIC_CMD)
                .path("jfrStart")
                .queryParam(OPTIONS, encodeValue("name=all"));
        Response  response = target.request(MediaType.APPLICATION_JSON_TYPE).post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        String result = (String) readEntity(target, response, String.class);
        assertThat(result.indexOf(SERVER_PREFIX + "-1"), greaterThan(0));
        assertThat(result.indexOf(SERVER_PREFIX + "-2"), greaterThan(0));

        File   folder   = s_dirJFR;
        String sJfr1    = folder.getAbsolutePath() + File.separator + "all1.jfr";
        String sJfrPath = folder.getAbsolutePath() + File.separator;
        response = getBaseTarget().path(DIAGNOSTIC_CMD)
                .path("jfrDump")
                .queryParam(OPTIONS, encodeValue("name=all,filename=" + sJfr1))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        target = getBaseTarget().path(DIAGNOSTIC_CMD).path("jfrDump")
                .queryParam(OPTIONS, encodeValue("name=all"));
        response = target.request(MediaType.APPLICATION_JSON_TYPE).post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        result = (String) readEntity(target, response, String.class);

        // for JDK11, when dump file is not provided,
        // DiagnosticCommand generates a file name instead of throwing exception
        if (!(s_bTestJdk11 || s_bIsEPP))
            {
            assertThat(result.indexOf("Exception"), greaterThan(0));
            }

        response = getBaseTarget().path(DIAGNOSTIC_CMD)
                .path("jfrCheck")
                .queryParam(OPTIONS, encodeValue("name=all"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        response = getBaseTarget().path(DIAGNOSTIC_CMD)
                .path("jfrStop")
                .queryParam(OPTIONS, encodeValue("name=all,filename=" + sJfrPath))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        assertThat(new File(folder.getAbsolutePath() + File.separator + "1-all1.jfr").exists(), is(true));
        assertThat(new File(folder.getAbsolutePath() + File.separator + "2-all1.jfr").exists(), is(true));
        assertThat(new File(sJfrPath + File.separator + "1-all.jfr").exists(), is(true));
        assertThat(new File(sJfrPath + File.separator + "2-all.jfr").exists(), is(true));
        }

    @Test
    public void testJfrWithRole()
            throws Exception
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        // This test requires Flight Recorder and only runs on Oracle JVMs
        CheckJDK.assumeOracleJDK();

        String  sFilePath = "target" + File.separator + "test-output"  + File.separator + "functional" + File.separator;
        String  sFileName = sFilePath + "testMemberJfr-myRecording.jfr";

        WebTarget target  = getBaseTarget().path(DIAGNOSTIC_CMD).path("jfrStart")
                .queryParam(OPTIONS, encodeValue("name=myJfr,duration=3s,filename="+ sFileName))
                .queryParam(ROLE_NAME, SERVER_PREFIX + "-1");
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE).post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        String result = (String) readEntity(target, response, String.class);

        File testFile1 = new File(sFilePath + "1-testMemberJfr-myRecording.jfr");
        assertThat(testFile1.exists(), is(true));
        testFile1.delete();

        assertThat(result.indexOf(SERVER_PREFIX + "-2"), is(-1));
        File testFile2 = new File(sFilePath +"2-testMemberJfr-myRecording.jfr");
        assertThat(testFile2.exists(), is(false));
        }

    @Test
    public void testMemberJfr()
            throws Exception
        {
        // This test requires Flight Recorder and only runs on Oracle JVMs
        CheckJDK.assumeOracleJDK();

        String   sJfr1    = s_dirJFR.getAbsolutePath() + File.separator + "foo1.jfr";
        String   sJfr2    = s_dirJFR.getAbsolutePath() + File.separator + "foo2.jfr";
        File     jfr1     = new File(sJfr1);
        File     jfr2     = new File(sJfr2);
        Response response = getBaseTarget().request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        ArrayList<Integer> listMemberIds = (ArrayList<Integer>) mapResponse.get("memberIds");

        assertThat(listMemberIds, notNullValue());
        assertThat(listMemberIds.size(), greaterThan(0));

        int nMemberId = listMemberIds.get(0);
        try
            {
            String sFileName = "target"+ File.separator +"testMemberJfr-myRecording.jfr";
            response = getBaseTarget().path("members").path(nMemberId + "")
                    .path(DIAGNOSTIC_CMD)
                    .path("jfrStart")
                    .queryParam(OPTIONS, encodeValue("name=myJfr,duration=5s,filename="+ sFileName)).request(MediaType.APPLICATION_JSON_TYPE)
                    .post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            File testFile = new File(sFileName);
            assertThat(testFile.exists(), is(true));
            testFile.delete();

            response = getBaseTarget().path("members").path(nMemberId + "")
                    .path(DIAGNOSTIC_CMD)
                    .path("jfrStart")
                    .queryParam(OPTIONS, encodeValue("name=foo")).request(MediaType.APPLICATION_JSON_TYPE)
                    .post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            // allow flight recorder to record something
            Thread.sleep(5000);

            response = getBaseTarget().path("members").path(nMemberId + "")
                    .path(DIAGNOSTIC_CMD)
                    .path("jfrDump")
                    .queryParam(OPTIONS, encodeValue("name=foo,filename=" + sJfr1)).request(MediaType.APPLICATION_JSON_TYPE)
                    .post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            response = getBaseTarget().path("members").path(nMemberId + "")
                    .path(DIAGNOSTIC_CMD)
                    .path("jfrCheck")
                    .queryParam(OPTIONS, encodeValue("name=foo")).request(MediaType.APPLICATION_JSON_TYPE)
                    .post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            // allow flight recorder to record something
            Thread.sleep(5000);

            response = getBaseTarget().path("members").path(nMemberId + "")
                    .path(DIAGNOSTIC_CMD)
                    .path("jfrStop")
                    .queryParam(OPTIONS, encodeValue("name=foo,filename=" + sJfr2)).request(MediaType.APPLICATION_JSON_TYPE)
                    .post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            assertThat(jfr1.exists(), is(true));
            assertThat(jfr2.exists(), is(true));
            }
        catch (UnsupportedEncodingException | InterruptedException e)
            {
            // log the exception
            CacheFactory.log("testMemberJfr() failed with exception: ");
            CacheFactory.log(e);
            }
            jfr1.delete();
            jfr2.delete();
        }

    @Test
    public void testJmxJfr()
            throws Exception
        {
        // This test requires Flight Recorder and only runs on Oracle JVMs
        CheckJDK.assumeOracleJDK();

        MBeanServerConnection mBeanServer;
        ObjectName            oName;

        try
            {
            String sName = "Coherence:type=DiagnosticCommand,Domain=com.sun.management,subType=DiagnosticCommand,cluster=mgmtRestCluster,member=" + m_aMembers[0].getName() + ",nodeId=" + m_aMembers[0].getLocalMemberId();

            oName = new ObjectName(sName);
            mBeanServer = m_aMembers[0].get(JmxFeature.class).getDeferredJMXConnector().get().getMBeanServerConnection();

            if (!s_bIsEPP)
                {
                mBeanServer.invoke(oName, "vmUnlockCommercialFeatures", null, null);
                }
            }
        catch (Exception e) // InstanceNotFoundException
            {
            try
                {
                String sName = "Coherence:type=DiagnosticCommand,Domain=com.sun.management,subType=DiagnosticCommand,cluster=mgmtRestCluster,member=" + m_aMembers[1].getName() + ",nodeId=" + m_aMembers[1].getLocalMemberId();

                mBeanServer = m_aMembers[1].get(JmxFeature.class).getDeferredJMXConnector().get().getMBeanServerConnection();
                oName = new ObjectName(sName);

                if (!s_bIsEPP)
                    {
                    mBeanServer.invoke(oName, "vmUnlockCommercialFeatures", null, null);
                    }
                }
            catch (Exception e1)
                {
                CacheFactory.log("warning: failed to connect to a mbean server.");
                CacheFactory.log(e1);
                throw e1;
                }
            }

        String   sFileName   = "target/test-output/functional/testJmxJfr-myRecording.jfr";
        Object[] aoArguments = new Object[]{new String[]{"name=foo", "duration=5s", "filename="+ sFileName}};

        mBeanServer.invoke(oName, "jfrStart", aoArguments, new String[]{String[].class.getName()});
        File testFile = new File(sFileName);
        assertThat(testFile.exists(), is(true));
        testFile.delete();
        }

    @Test
    public void testServiceInfo()
        {
        WebTarget target   = getBaseTarget().path(SERVICES);
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());

        List<LinkedHashMap> listItemMaps = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listItemMaps, notNullValue());
        assertThat(listItemMaps.size(), is(4)); // <---- This is SO brittle!!!

        String        sDistServiceName  = null;
        String        sProxyServiceName = null;
        String        sMoRESTProxy      = HttpHelper.getServiceName();
        LinkedHashMap mapDistScheme     = null;
        LinkedHashMap mapProxyScheme    = null;
        for (int i = 0; i < listItemMaps.size(); i++)
            {
            if (((String) listItemMaps.get(i).get(NAME)).compareToIgnoreCase(SERVICE_NAME) == 0)
                {
                sDistServiceName = SERVICE_NAME;
                mapDistScheme = listItemMaps.get(i);
                }
            else
            if (((String) listItemMaps.get(i).get(NAME)).compareToIgnoreCase(sMoRESTProxy) == 0)
                {
                sProxyServiceName = sMoRESTProxy;
                mapProxyScheme = listItemMaps.get(i);
                }

            if (sDistServiceName != null && sProxyServiceName != null)
                {
                break;
                }
            }

        assertNotNull(mapDistScheme);
        assertNotNull(mapProxyScheme);

        assertThat(mapDistScheme.get(NAME), is(SERVICE_NAME));
        assert (((Collection) mapDistScheme.get("type")).contains(SERVICE_TYPE));

        target = m_client.target(getSelfLink(mapDistScheme));
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapService = readEntity(target, response);
        assertThat(mapService, notNullValue());
        assertThat(mapService.get(NAME), is(mapDistScheme.get(NAME)));

        testDistServiceInfo(mapService);

        assertThat(mapProxyScheme.get(NAME), is(sMoRESTProxy));
        assertThat((Collection<String>) mapProxyScheme.get("type"), Matchers.hasItem("Proxy"));

        target = m_client.target(getSelfLink(mapProxyScheme));
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        mapService = readEntity(target, response);
        assertThat(mapService, notNullValue());
        String sName = (String) mapService.get(NAME);
        assertThat(sName, is(mapProxyScheme.get(NAME)));
        assertThat(((ArrayList) mapService.get("quorumStatus")).get(0), is("Not configured"));

        target = getBaseTarget().path(SERVICES).path(sName)
                .queryParam("fields", "storageEnabled");
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        mapResponse = readEntity(target, response);
        assertThat(((LinkedHashMap) mapResponse.get("storageEnabled")).get("false"), is(1));
        }

    @Test
    public void testServiceDescription()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path(DESCRIPTION);
        Response  response = target.request(MediaType.APPLICATION_JSON_TYPE).get();

        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        MatcherAssert.assertThat(((String) ((Map) readEntity(target, response, Map.class)).get(DESCRIPTION))
                                         .startsWith("PartitionedCache{Name="),
                                 is(true));
        }

    @Test
    public void testServiceMembers()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("members");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());

        List<LinkedHashMap> items = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(items, notNullValue());
        assertThat(items.size(), is(2));

        for(LinkedHashMap mapEntry : items)
            {
            assertThat(mapEntry.get(NAME), is(SERVICE_NAME));
            assertThat(mapEntry.get("type"), is(SERVICE_TYPE));
            assertThat(Integer.parseInt(mapEntry.get(NODE_ID).toString()), greaterThan(0));
            assertThat(mapEntry.get("backupCount"), is(1));
            assertThat(mapEntry.get("joinTime"), notNullValue());
            assertThat(mapEntry.get("links"), notNullValue());

            target = m_client.target(getSelfLink(mapEntry));
            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            LinkedHashMap mapMemberResponse = readEntity(target, response);

            assertThat(mapEntry.get(NODE_ID), is(mapMemberResponse.get(NODE_ID)));
            assertThat(mapEntry.get("joinTime"), is(mapMemberResponse.get("joinTime")));
            }
        }

    @Test
    public void testManagementRequestWithAcceptEncodingGzip()
        {
        Response response = getBaseTarget().path(SERVICES).path("DistributedCache").path("members").request().header("Accept-Encoding", "gzip").get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        assertThat(response.getHeaderString("Content-Encoding"), is("gzip"));
        }

    @Test
    public void testPartitionInfo()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("partition");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("strategyName"), is("SimpleAssignmentStrategy"));
        assertThat(mapResponse.get("partitionCount"), is(257));
        assertThat(mapResponse.get("backupCount"), is(1));

        target = m_client.target(getSelfLink(mapResponse));
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        Map mapPartitionResponse = readEntity(target, response);

        // this timestamp, averageStorageSizeKB could differ so just remove
        mapPartitionResponse.remove("lastAnalysisTime");
        mapPartitionResponse.remove("averageStorageSizeKB");
        mapResponse.remove("lastAnalysisTime");
        mapResponse.remove("averageStorageSizeKB");
        assertThat(mapPartitionResponse, is(mapResponse));
        }

    @Test
    public void testDirectServiceMember()
        {
        WebTarget membersTarget = getBaseTarget().path(SERVICES).path("DistributedCache").path("members");
        WebTarget target        = membersTarget.path(SERVER_PREFIX + "-1");
        Response response       = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get(NAME), is(SERVICE_NAME));
        assertThat(mapResponse.get("type"), is(SERVICE_TYPE));

        assertThat(mapResponse.get("backupCount"), is(1));
        assertThat(mapResponse.get("joinTime"), notNullValue());
        assertThat(mapResponse.get("links"), notNullValue());

        String sSelfUrl = getLink(mapResponse, "parent");
        assertThat(sSelfUrl, is(membersTarget.getUri().toString()));
        }

    @Test
    public void testDirectServiceMemberUpdate()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put("threadCountMin", 5);
        mapEntity.put("taskHungThresholdMillis", 10);
        mapEntity.put("taskTimeoutMillis", 100000);
        mapEntity.put("requestTimeoutMillis", 200000);
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("members").path(SERVER_PREFIX + "-1");
        Response  response = target.request().post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("threadCountMin"), is(5));
        assertThat(mapResponse.get("taskHungThresholdMillis"), is(10));
        assertThat(mapResponse.get("taskTimeoutMillis"), is(100000));
        assertThat(mapResponse.get("requestTimeoutMillis"), is(200000));
        }

    @Test
    public void testCacheMemberUpdate()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Map<String, Object> mapMethodValues = new HashMap<String, Object>()
            {{
               put("highUnits",   100005);
               put("expiryDelay", 60000);
            }};

        mapMethodValues.forEach((attribute, value) ->
            {
            LinkedHashMap map = new LinkedHashMap();
            map.put(attribute, value);
            WebTarget target = getBaseTarget().path(SERVICES).path(SERVICE_NAME).path(CACHES).path(CACHE_NAME)
                    .path("members").path(SERVER_PREFIX + "-1");
            Response response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

            LinkedHashMap mapResponse = readEntity(target, response);
            List<Map> listItems = (List<Map>) mapResponse.get("items");
            assertThat(listItems, notNullValue());
            assertThat(listItems.size(), is(1));

            Map mapItem = listItems.get(0);

            assertThat(attribute + " should be " + value + ", but is " + mapResponse.get(attribute),
                    mapItem.get(attribute), is(value));
            });
        }

    @Test
    public void testClusterMemberUpdate()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Map<String, Object> mapMethodValues = new HashMap<String, Object>()
            {{
               put("loggingLevel"            ,9);
               put("resendDelay"             ,100);
               put("sendAckDelay"            ,17);
               put("trafficJamCount"         ,2048);
               put("trafficJamDelay"         ,12);
               put("loggingLimit"            ,2147483640);
               put("loggingFormat"           ,"{date}/{uptime} {product} {version} <{level}> (thread={thread}, member={member}):- {text}");
            }};
        mapMethodValues.forEach((attribute, value) ->
            {
            LinkedHashMap map = new LinkedHashMap();
            map.put(attribute, value);
            WebTarget target = getBaseTarget().path("members").path(SERVER_PREFIX + "-1");
            Response response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            LinkedHashMap mapResponse = readEntity(target, response);
            assertThat(mapResponse, notNullValue());
            assertThat(attribute + " should be " + value + ", but is " + mapResponse.get(attribute), mapResponse.get(attribute), is(value));
            });
        }

    @Test
    public void testUpdateReporterMember()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        LinkedHashMap map     = new LinkedHashMap();
        String  sMember = SERVER_PREFIX + "-1";

        map.put("intervalSeconds", 15L);
        WebTarget target   = getBaseTarget().path(REPORTERS).path(sMember);
        Response  response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        assertNoMessages(response);

        Eventually.assertThat(invoking(this).assertReporterAttribute(sMember, "intervalSeconds", 15),is(true));

        map.clear();
        Eventually.assertThat(invoking(this).assertReporterAttribute(sMember, "currentBatch", 0),is(true));

        map.put("currentBatch", 25);
        response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        assertNoMessages(response);
        Eventually.assertThat(invoking(this).assertReporterAttribute(sMember, "currentBatch", 25),is(true));
        }

    @Test
    public void testClusterMemberUpdateFailure()
        {
        LinkedHashMap map = new LinkedHashMap();
        map.put("cpuCount", 9);
        WebTarget target   = getBaseTarget().path("members").path(SERVER_PREFIX + "-1");
        Entity    entity   = Entity.entity(map, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response, entity);

        List<LinkedHashMap> listMessages = (List) mapResponse.get("messages");
        assertThat(listMessages, notNullValue());
        assertThat(listMessages.size(), is(1));

        LinkedHashMap mapMessages = listMessages.get(0);
        assertThat(mapMessages.get("field"), is("cpuCount"));
        assertThat(mapMessages.get("severity"), is("FAILURE"));
        }

    @Test
    public void testCacheMemberUpdateFailure()
        {
        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put("cacheHits", 100005);
        WebTarget target  = getBaseTarget().path(SERVICES).path(SERVICE_NAME).path(CACHES).path(CACHE_NAME)
                .path("members").path(SERVER_PREFIX + "-1");
        Entity   entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response, entity);

        List<LinkedHashMap> listMessages = (List) mapResponse.get("messages");
        assertThat(listMessages, notNullValue());
        assertThat(listMessages.size(), is(1));

        LinkedHashMap mapMessages = listMessages.get(0);
        assertThat(mapMessages.get("field"), is("cacheHits"));
        }

    @Test
    public void testReporters()
        {
        WebTarget target   = getBaseTarget().path(REPORTERS);
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        List<LinkedHashMap> listItems = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(2));

        for (LinkedHashMap mapReports : listItems)
            {
            assertThat(mapReports.get(NODE_ID), is(notNullValue()));
            assertThat(Long.parseLong(mapReports.get("intervalSeconds").toString()), greaterThan(1L));
            assertThat(Long.parseLong(mapReports.get("runLastMillis").toString()), greaterThan(-1L));
            assertThat(mapReports.get("outputPath"), is(notNullValue()));
            }
        }

    @Test
    public void testDirectReporter()
        {
        String sMember = SERVER_PREFIX + "-1";
        WebTarget target   = getBaseTarget().path(REPORTERS).path(sMember);
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get(NODE_ID), is("1"));
        assertThat(Long.parseLong(mapResponse.get("intervalSeconds").toString()), greaterThan(1L));
        assertThat(Long.parseLong(mapResponse.get("runLastMillis").toString()), greaterThan(-1L));
        assertThat(mapResponse.get("outputPath"), is(notNullValue()));
        assertThat(mapResponse.get(MEMBER), is(sMember));
        }

    @Test
    public void testStartAndStopReporter() throws IOException
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        String sMember = SERVER_PREFIX + "-1";

        // create a temp directory so we don't pollute any directories
        File tempDirectory = FileHelper.createTempDir();

        try {
            setReporterAttribute(sMember, "outputPath", tempDirectory.getAbsolutePath());
            Eventually.assertThat(invoking(this).assertReporterAttribute(sMember, "outputPath", tempDirectory.getAbsolutePath()),is(true));

            // set the intervalSeconds shorter so we don't want as long
            setReporterAttribute(sMember, "intervalSeconds", 15L);
            Eventually.assertThat(invoking(this).assertReporterAttribute(sMember, "intervalSeconds", 15),is(true));

            Eventually.assertThat(invoking(this).assertReporterAttribute(sMember, "state", "Stopped"),is(true));
            LinkedHashMap mapEntity = new LinkedHashMap();

            // start the reporter
            Response response = getBaseTarget().path(REPORTERS).path(sMember).path("start").request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

            Eventually.assertThat(invoking(this).assertReporterAttribute(sMember, "state", new String[] {"Sleeping", "Running"}),is(true));

            // stop the reporter
            response = getBaseTarget().path(REPORTERS).path(sMember).path("stop").request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            Eventually.assertThat(invoking(this).assertReporterAttribute(sMember, "state", "Stopped"),is(true), within(2, TimeUnit.MINUTES));
            }
        finally
            {
            try
                {
                FileHelper.deleteDir(tempDirectory);
                }
            catch (IOException ioe)
                {
                // ignore
                }
            }
        }

    @Test
    public void testCacheMemberResetStats()
        {
        WebTarget target = getBaseTarget().path(SERVICES).path(SERVICE_NAME).path(CACHES).path(CACHE_NAME)
                .path("members").path(SERVER_PREFIX + "-1").path("resetStatistics");
        Response response = target.request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testServiceMemberResetStats()
        {
        WebTarget membersTarget = getBaseTarget().path(SERVICES).path(SERVICE_NAME).path("members");
        Response response = membersTarget
                .path(SERVER_PREFIX + "-1").path("resetStatistics").request().post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        WebTarget target = membersTarget.path(SERVER_PREFIX + "-1");
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(((Number) mapResponse.get("requestTotalCount")).intValue(), greaterThanOrEqualTo(0));
        }

    @Test
    public void testSuspendAndResume()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Response response = getBaseTarget().path(SERVICES).path(SERVICE_NAME).path("suspend")
                .request().post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        Eventually.assertThat(invoking(this).getAttributeValue(m_client, SERVICE_NAME, "quorumStatus"),
                is("Suspended"));

        response = getBaseTarget().path(SERVICES).path("DistributedCache").path("resume")
                .request().post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Eventually.assertThat(invoking(this).getAttributeValue(m_client, SERVICE_NAME, "quorumStatus").toString(),
                containsString("allowed-actions"));
        }

    @Test
    public void testService()
        {
        // aggregate all attributes for a service across all nodes
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache");
        Response  response = target.request().get();
        LinkedHashMap mapResponse = readEntity(target, response);

        testDistServiceInfo(mapResponse);
        }

    @Test
    public void testServices()
        {
        // aggregate all attributes for all services across all nodes
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache");
        Response  response = target.request().get();
        LinkedHashMap mapResponse = readEntity(target, response);

        testDistServicesInfo(mapResponse);
        }

    @Test
    public void testCache()
        {
        final String CACHE_NAME = "dist-foo";

        // fill a cache
        NamedCache cache    = findApplication(SERVER_PREFIX + "-1").getCache(CACHE_NAME);
        Binary     binValue = Binary.getRandomBinary(1024, 1024);

        cache.put(1, binValue);

        long[] acTmp = new long[1];
        long   cEntrySize;
        do
            {
            WebTarget     target      = getBaseTarget().path(CACHES).path(CACHE_NAME).queryParam("fields","units")
                                        .queryParam("role", "*");
            Response      response    = target.request().get();
            LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response));

            System.out.println(mapResponse);

            cEntrySize = acTmp[0] = ((Number) mapResponse.get("units")).longValue();
            }
        while (sleep(() -> acTmp[0] <= 0L, REMOTE_MODEL_PAUSE_DURATION));

        cache.clear();

        for (int i = 0; i < 10; ++i)
            {
            cache.put(i, binValue);
            }
        Base.sleep(REMOTE_MODEL_PAUSE_DURATION);

        do
            {
            WebTarget     target      = getBaseTarget().path(CACHES).path(CACHE_NAME).queryParam("fields","size")
                                        .queryParam("role", "*");
            Response      response    = target.request().get();
            LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response));
            acTmp[0] = ((Number) mapResponse.get("size")).longValue();
            }
        while (sleep(() -> acTmp[0] != 10L, REMOTE_MODEL_PAUSE_DURATION));

        WebTarget     target      = getBaseTarget().path(SERVICES).path(SERVICE_NAME)
                                    .path(CACHES).path(CACHE_NAME).queryParam("fields","size");
        Response      response    = target.request().get();
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response));

        // aggregate all attributes for a cache on a service across all nodes
        assertThat(((Number) mapResponse.get("size")).longValue(), is(10L));

        target = getBaseTarget().path(SERVICES).path(SERVICE_NAME)
                .path(CACHES).path(CACHE_NAME).queryParam("fields","units")
                .queryParam("role","*");
        response = target.request().get();
        mapResponse = new LinkedHashMap(readEntity(target, response));

        // aggregate Units attribute for a cache across all nodes
        assertThat(((Number) mapResponse.get("units")).longValue(), is(cEntrySize * 10));

        target = getBaseTarget().path(SERVICES).path(SERVICE_NAME)
                .path(CACHES).path(CACHE_NAME).queryParam("fields","units")
                .queryParam("role","*")
                .queryParam("collector", "list");
        response = target.request().get();
        mapResponse = new LinkedHashMap(readEntity(target, response));

        // list the Units attribute for a cache across all nodes
        List<Integer> listUnits = (List) mapResponse.get("units");
        assertEquals(2, listUnits.size());
        int cMinUnits = ((int) cEntrySize) * 4;
        listUnits.forEach(NUnits -> assertThat(NUnits, greaterThanOrEqualTo(cMinUnits)));

        cache.clear();

        response = getBaseTarget().path(CACHES).path(CACHE_NAME).path("members").path(SERVER_PREFIX + "-1").path("resetStatistics")
                .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        response = getBaseTarget().path(CACHES).path(CACHE_NAME).path("members").path(SERVER_PREFIX + "-2").path("resetStatistics")
                .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Base.sleep(10000);

        int nSize = 20;
        for (int i = 0; i < nSize; i++)
            {
            cache.put(i, binValue);
            }
        for (int i = 0; i < nSize; i++)
            {
            cache.get(i);
            }
        Base.sleep(REMOTE_MODEL_PAUSE_DURATION);

        target = getBaseTarget().path(CACHES).path(CACHE_NAME);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        mapResponse = new LinkedHashMap(readEntity(target, response));
        System.out.println(mapResponse.toString());

        assertEquals(nSize, ((Integer) mapResponse.get("size")).intValue());
        assertEquals(nSize, ((Integer) mapResponse.get("totalPuts")).intValue());
        assertEquals(nSize, ((Integer) mapResponse.get("totalGets")).intValue());
        }

    @Test
    public void testDirectServiceMemberWithIncludedFields()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("members")
                            .path(SERVER_PREFIX + "-1").queryParam("fields", "backupCount,joinTime");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.size(), is(3));
        assertThat(mapResponse.get("backupCount"), is(1));
        assertThat(mapResponse.get("joinTime"), notNullValue());

        assertThat(mapResponse.get(NAME), nullValue());
        }

    @Test
    public void testDirectServiceMemberWithExcludedFields()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("members")
                            .path(SERVER_PREFIX + "-1").queryParam("excludeFields", "backupCount,joinTime");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("backupCount"), nullValue());
        assertThat(mapResponse.get("joinTime"), nullValue());
        assertThat(mapResponse.get(NAME), notNullValue());
        }

    @Test
    public void testDirectServiceMemberWithIncludedAndExcludedFields()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("members")
                            .path(SERVER_PREFIX + "-1")
                            .queryParam("fields", "name,joinTime")
                            .queryParam("excludeFields", "backupCount,joinTime");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("joinTime"), nullValue());
        assertThat(mapResponse.get(NAME), notNullValue());
        }

    @Test
    public void testDirectServiceMemberWithExcludedLinks()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("members")
                            .path(SERVER_PREFIX + "-1").queryParam("excludeLinks", "ownership");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse.get("links"), notNullValue());

        List<LinkedHashMap> listLinks = (List) mapResponse.get("links");

        Set<Object> linkNames = listLinks.stream().map(m -> m.get("rel")).collect(Collectors.toSet());
        assertThat(linkNames, not(hasItem("ownership")));
        }

    @Test
    public void testDirectServiceMemberWithIncludedAndExcludedLinks()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("members")
                            .path(SERVER_PREFIX + "-1")
                            .queryParam("links", "self,ownership")
                            .queryParam("excludeLinks", "ownership,parent");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse.get("links"), notNullValue());

        List<LinkedHashMap> listLinks = (List) mapResponse.get("links");

        assertThat(listLinks, notNullValue());
        Set<Object> linkNames = listLinks.stream().map(m -> m.get("rel")).collect(Collectors.toSet());
        assertThat(linkNames, not(hasItem("ownership")));
        assertThat(linkNames, hasItem("self"));
        }

    @Test
    public void testOwnershipState()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("members")
                            .path(SERVER_PREFIX + "-1").path("ownership");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("ownership"), notNullValue());
        }

    @Test
    public void testOwnershipVerbose()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("members")
                            .path(SERVER_PREFIX + "-1").path("ownership").queryParam("verbose", true);
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("ownership"), notNullValue());
        }

    @Test
    public void testDistributionState()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("members")
                            .path(SERVER_PREFIX + "-1").path("distributionState").queryParam("verbose", true);
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("distributionState"), notNullValue());
        }

    @Test
    public void testPartitionScheduledDistributions()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("partition")
                            .path("scheduledDistributions").queryParam("verbose", true);
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("scheduledDistributions"), notNullValue());
        }

    @Test
    public void testReportNodeState()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        WebTarget target   = getBaseTarget().path("members").path(SERVER_PREFIX + "-1").path("state");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("state"), notNullValue());
        assertThat((String) mapResponse.get("state"), containsString("Full Thread Dump"));
        }

    @Test
    public void testProxy()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("ExtendProxyService").path("members");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());

        List<LinkedHashMap> listItems = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(2));

        for(LinkedHashMap mapEntry : listItems)
            {
            assertThat(mapEntry.get(NAME), is("ExtendProxyService"));
            target = m_client.target(getLink(mapEntry, "proxy"));
            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            LinkedHashMap mapMemberResponse = readEntity(target, response);
            assertThat(mapMemberResponse.get("protocol"), notNullValue());
            }
        }

    @Test
    public void testProxyConnections()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("ExtendProxyService").path("members");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());

        List<LinkedHashMap> listItems = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(2));

        for(LinkedHashMap mapEntry : listItems)
            {
            assertThat(mapEntry.get(NAME), is("ExtendProxyService"));

            String sProxyUrl = getLink(mapEntry, "proxy");
            response = m_client.target(sProxyUrl).path("connections").request().get(); // TODO to test actual proxy connection
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            }
        }

    @Test
    public void testCaches()
        {
        // fill a cache
        NamedCache cache    = findApplication(SERVER_PREFIX + "-1").getCache("dist-foo");
        Binary     binValue = Binary.getRandomBinary(1024, 1024);

        cache.put(1, binValue);

        WebTarget target   = getBaseTarget().path(CACHES);
        Response  response = target.request().get();
        testCachesResponse(target, response);
        }

    @Test
    public void testDistCache()
        {
        WebTarget  target = getBaseTarget().path(CACHES).path(CACHE_NAME);
        Response response = target.request().get();
        testBackCacheResponse(target, response);
        }

    @Test
    public void testNonExistentCache()
        {
        Response response = getBaseTarget().path(CACHES).path("nonexistent").request().get();
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));

        // Tests for Bug 33541445
        response = getBaseTarget().path(CACHES).path("nonexistent").path(MEMBERS).request().get();
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testNonExistentService()
        {
        Response response = getBaseTarget().path(SERVICES).path("nonexistent").request().get();
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        // Tests for Bug 33541445
        response = getBaseTarget().path(SERVICES).path("DistributedCacheInvalid").path(MEMBERS).request().get();
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testNonExistentCacheInAService()
        {
        Response response = getBaseTarget().path(SERVICES).path(SERVICE_NAME).path(CACHES)
                .path("nonexistent").request().get();
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testNonExistentServiceCaches()
        {
        Response response = getBaseTarget().path(SERVICES).path("nonexistent").path(CACHES).request().get();
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        // Tests for Bug 33541445
        response = getBaseTarget().path(SERVICES).path("nonexistent").path(CACHES).path(MEMBERS).request().get();
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testFrontCaches()
        {
        WebTarget target   = getBaseTarget().path(CACHES).path(NEAR_CACHE_NAME);
        Response  response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = readEntity(target, response);

        String sMembersUrl = getLink(mapResponse, "members");
        target = m_client.target(sMembersUrl).queryParam("tier", "front");
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        LinkedHashMap mapCacheMembers = readEntity(target, response);
        assertThat(mapCacheMembers, notNullValue());

        List<LinkedHashMap> listCacheMembers = (List<LinkedHashMap>) mapCacheMembers.get("items");
        assertThat(listCacheMembers, notNullValue());
        assertThat(listCacheMembers.size(), is(1));

        for(LinkedHashMap mapCacheMember : listCacheMembers)
            {
            assertThat(mapCacheMember.get("tier"), is("front"));
            assertThat(mapCacheMember.get(NAME), is(NEAR_CACHE_NAME));
            assertThat(mapCacheMember.get("size"), instanceOf(Number.class));
            assertThat(mapCacheMember.get("cacheHits"), instanceOf(Number.class));
            }
        }

    @Test
    public void testFrontAndBackCaches()
        {
        WebTarget target   = getBaseTarget().path(CACHES);
        Response  response = target.path(NEAR_CACHE_NAME).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = readEntity(target, response);

        String sMembersUrl = getLink(mapResponse, "members");
        target = m_client.target(sMembersUrl);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        LinkedHashMap mapCacheMembers = readEntity(target, response);
        assertThat(mapCacheMembers, notNullValue());

        List<LinkedHashMap> listCacheMembers = (List<LinkedHashMap>) mapCacheMembers.get("items");

        assertThat(listCacheMembers.size(), is(3));
        assertThat(listCacheMembers, notNullValue());

        for(LinkedHashMap mapCacheMember : listCacheMembers)
            {
            assertThat(mapCacheMember.get("tier"), isOneOf("front", "back"));
            assertThat(mapCacheMember.get(NAME), is(NEAR_CACHE_NAME));
            assertThat(mapCacheMember.get("size"), instanceOf(Number.class));
            assertThat(mapCacheMember.get("cacheHits"), instanceOf(Number.class));
            }
        }

    @Test
    public void testCachesOfAService()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path(SERVICE_NAME).path(CACHES);
        Response  response = target.request().get();
        testCachesResponse(target, response);
        }

    @Test
    public void testDistCacheOfService()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path(SERVICE_NAME).path(CACHES).path(CACHE_NAME);
        Response  response = target.request().get();
        testBackCacheResponse(target, response);
        }

    @Test
    public void testSimpleClusterSearch()
        {
        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put("links", new String[]{});
        mapEntity.put("fields", new String[]{"clusterName","clusterSize"});

        WebTarget             target   = getBaseTarget().path("search");
        Entity<LinkedHashMap> entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response              response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response, entity));
        assertThat(mapResponse.size(), is(2));
        assertThat(mapResponse.get("clusterName"), is(notNullValue()));
        assertThat((int) mapResponse.get("clusterSize"), greaterThan(1));
        }

    @Test
    public void testClusterSearchWithMembers()
        {
        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put("links", new String[]{});
        mapEntity.put("fields", new String[]{"clusterName","clusterSize"});

        LinkedHashMap mapChildren = new LinkedHashMap();
        mapEntity.put("children", mapChildren);

        LinkedHashMap mapMembers = new LinkedHashMap();
        mapMembers.put("links", new String[]{});
        mapMembers.put("fields", new String[]{NODE_ID,"memberName"});
        mapChildren.put("members", mapMembers);

        WebTarget target   = getBaseTarget().path("search");
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response, entity));
        assertThat(mapResponse.size(), is(3));
        assertThat(mapResponse.get("clusterName"), is(notNullValue()));
        assertThat((int) mapResponse.get("clusterSize"), greaterThan(1));

        LinkedHashMap mapMembersResponse = (LinkedHashMap) mapResponse.get("members");
        assertThat(mapMembersResponse, notNullValue());
        List<LinkedHashMap> listMembers = (List<LinkedHashMap>) mapMembersResponse.get("items");
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), is(greaterThan(1)));

        for (LinkedHashMap mapMember: listMembers)
            {
            assertThat(mapMember.size(), is(2));
            assertThat(mapMember.get(NODE_ID), notNullValue());
            assertThat(mapMember.get("memberName"), notNullValue());
            }
        }

    @Test
    public void testClusterSearchWithServices()
        {
        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put("links", new String[]{});
        mapEntity.put("fields", new String[]{});

        LinkedHashMap mapChildren = new LinkedHashMap();
        mapEntity.put("children", mapChildren);

        LinkedHashMap mapMembers = new LinkedHashMap();
        mapMembers.put("links", new String[]{});
        mapMembers.put("fields", new String[]{NAME,"type"});
        mapChildren.put(SERVICES, mapMembers);

        WebTarget target   = getBaseTarget().path("search");
        Entity    entyty   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = getBaseTarget().path("search")
                .request().post(entyty);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response, entyty));
        assertThat(mapResponse.size(), is(1));

        LinkedHashMap membersResponseMap = (LinkedHashMap) mapResponse.get(SERVICES);
        assertThat(membersResponseMap, notNullValue());
        List<LinkedHashMap> listMembers = (List<LinkedHashMap>) membersResponseMap.get("items");
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), is(4)); // <---- This is SO brittle!!!

        for (LinkedHashMap mapMember: listMembers)
            {
            assertThat(mapMember.size(), greaterThan(ATTRIBUTES_COUNT));
            assertThat(mapMember.get(NAME), notNullValue());
            assertThat(mapMember.get("type"), notNullValue());
            }
        }

    @Test
    public void testClusterSearchWithServiceMembers()
        {
        String sSearchJson = "{" +
                "\"links\":[],"  +
                "\"fields\":[]," +
                "\"children\":{" +
                "\"services\":{" +
                "\"fields\":[\"name\",\"type\"]," +
                "\"links\":[],"  +
                "\"children\":{" +
                "\"members\":{" +
                "\"fields\":[\"name\",\"eventBacklog\",\"joinTime\"]" +
                "}" +
                "}" +
                "}" +
                "}" +
                "}";
        System.out.println(sSearchJson);
        WebTarget target   = getBaseTarget().path("search");
        Entity    entity   = Entity.entity(sSearchJson, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response, entity));
        assertThat(mapResponse.size(), is(1));

        LinkedHashMap membersResponseMap = (LinkedHashMap) mapResponse.get(SERVICES);
        assertThat(membersResponseMap, notNullValue());
        List<LinkedHashMap> listServices = (List<LinkedHashMap>) membersResponseMap.get("items");
        assertThat(listServices, notNullValue());
        assertThat(listServices.size(), is(4)); // <---- This is SO brittle!!!

        for (LinkedHashMap mapService: listServices)
            {
            assertThat(mapService.size(), greaterThan(ATTRIBUTES_COUNT));
            assertThat(mapService.get(NAME), isOneOf(SERVICES_LIST));
            assertThat(mapService.get("type"), notNullValue());
            assertThat(mapService.get("memberCount"), notNullValue());
            assertThat(((LinkedHashMap<String, Integer>) mapService.get("running")).get("true"), greaterThanOrEqualTo(1));
            assertThat(mapService.get("statistics"), notNullValue());
            assertThat(mapService.get("storageEnabled"), notNullValue());
            assertThat(mapService.get("threadCount"), notNullValue());

            membersResponseMap = (LinkedHashMap) mapService.get("members");
            assertThat(membersResponseMap, notNullValue());

            List<LinkedHashMap> memberItems = (List<LinkedHashMap>) membersResponseMap.get("items");
            assertThat(memberItems, notNullValue());

            for (LinkedHashMap memberMap: memberItems)
                {
                assertThat(memberMap.get("taskAverageDuration"), nullValue());
                assertThat(memberMap.get(NAME), notNullValue());
                assertThat(memberMap.get("eventBacklog"), notNullValue());
                assertThat(memberMap.get("joinTime"), notNullValue());
                }
            }
        }

    @Test
    public void testClusterSearchWithServicesAndCaches()
        {
        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put("links", new String[]{});
        mapEntity.put("fields", new String[]{});

        LinkedHashMap mapChildren = new LinkedHashMap();
        mapEntity.put("children", mapChildren);

        LinkedHashMap mapServices = new LinkedHashMap();
        mapServices.put("links", new String[]{});
        mapServices.put("fields", new String[]{NAME,"type"});
        mapChildren.put(SERVICES, mapServices);

        LinkedHashMap cachesMap = new LinkedHashMap();
        cachesMap.put("links", new String[]{});
        cachesMap.put("fields", new String[]{NAME});

        LinkedHashMap mapServiceMembersChildren = new LinkedHashMap();
        mapServices.put("children", mapServiceMembersChildren);
        mapServiceMembersChildren.put(CACHES, cachesMap);

        WebTarget target   = getBaseTarget().path("search");
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response, entity));
        assertThat(mapResponse.size(), is(1));

        LinkedHashMap membersResponseMap = (LinkedHashMap) mapResponse.get(SERVICES);
        assertThat(membersResponseMap, notNullValue());
        List<LinkedHashMap> listMembers = (List<LinkedHashMap>) membersResponseMap.get("items");
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), is(4)); // <---- This is SO brittle!!!

        for (LinkedHashMap mapServiceMember: listMembers)
            {
            assertThat(mapServiceMember.get(NAME), notNullValue());
            assertThat(mapServiceMember.get("type"), notNullValue());

            // no need to test for proxy services
            if (mapServiceMember.get(NAME).equals(HttpHelper.getServiceName()) ||
                    mapServiceMember.get(NAME).equals("ExtendProxyService") ||
                    mapServiceMember.get(NAME).equals(ACTIVE_SERVICE) ||
                    mapServiceMember.get(NAME).equals(MetricsHttpHelper.getServiceName()))
                {
                continue;
                }

            LinkedHashMap mapCachesResponse = (LinkedHashMap) mapServiceMember.get(CACHES);
            assertThat(mapCachesResponse, notNullValue());

            List<LinkedHashMap> listCacheItems = (List<LinkedHashMap>) mapCachesResponse.get("items");
            assertThat(listCacheItems, notNullValue());

            for (LinkedHashMap mapMember: listCacheItems)
                {
                assertThat(mapMember.size(), greaterThan(ATTRIBUTES_COUNT));
                assertThat(mapMember.get(NAME), notNullValue());
                }
            }
        }

    @Test
    public void testClusterSearchWithServicesAndCacheMembers()
        {
        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put("links", new String[]{});
        mapEntity.put("fields", new String[]{});

        LinkedHashMap mapServices = new LinkedHashMap();
        mapServices.put("links", new String[]{});
        mapServices.put("fields", new String[]{NAME,"type"});

        mapEntity.put("children", new LinkedHashMap(){{put(SERVICES, mapServices);}});

        LinkedHashMap mapCaches = new LinkedHashMap();
        mapCaches.put("links", new String[]{});
        mapCaches.put("fields", new String[]{NAME});

        mapServices.put("children", new LinkedHashMap(){{put(CACHES, mapCaches);}});

        LinkedHashMap cachesMembersMap = new LinkedHashMap();
        cachesMembersMap.put("links", new String[]{});
        cachesMembersMap.put("fields", new String[]{NAME, "size"});

        mapCaches.put("children", new LinkedHashMap(){{put("members", cachesMembersMap);}});

        WebTarget target   = getBaseTarget().path("search");
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response, entity));
        assertThat(mapResponse.size(), is(1));

        LinkedHashMap mapMembersResponse = (LinkedHashMap) mapResponse.get(SERVICES);
        assertThat(mapMembersResponse, notNullValue());
        List<LinkedHashMap> listMembers = (List<LinkedHashMap>) mapMembersResponse.get("items");
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), is(4)); // <---- This is SO brittle!!!

        for (LinkedHashMap mapService: listMembers)
            {
            assertThat(mapService.get(NAME), notNullValue());
            assertThat(mapService.get("type"), notNullValue());

            // no need to test for proxy services
            if (mapService.get(NAME).equals(HttpHelper.getServiceName()) ||
                    mapService.get(NAME).equals("ExtendProxyService") ||
                    mapService.get(NAME).equals(ACTIVE_SERVICE) ||
                    mapService.get(NAME).equals(MetricsHttpHelper.getServiceName()))
                {
                continue;
                }

            LinkedHashMap cachesResponseMap = (LinkedHashMap) mapService.get(CACHES);
            assertThat(cachesResponseMap, notNullValue());

            List<LinkedHashMap> listCacheItems = (List<LinkedHashMap>) cachesResponseMap.get("items");
            assertThat(listCacheItems, notNullValue());

            for (LinkedHashMap mapMember: listCacheItems)
                {
                assertThat(mapMember.size(), greaterThan(ATTRIBUTES_COUNT));
                assertThat(mapMember.get(NAME), notNullValue());

                LinkedHashMap mapCachesMembers = (LinkedHashMap) mapMember.get("members");
                assertThat(mapCachesMembers, notNullValue());

                List<LinkedHashMap> cacheMemberItems = (List<LinkedHashMap>) mapCachesMembers.get("items");

                for (LinkedHashMap mapCacheMember : cacheMemberItems)
                    {
                    assertThat(mapCacheMember.size(), is(2));
                    assertThat(mapCacheMember.get(NAME), notNullValue());
                    assertThat(mapCacheMember.get("size"), notNullValue());
                    }
                }
            }
        }

    @Test
    public void testPersistenceFailureResponses()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        // the following should fail with BAD REQUESTS

        // this service does not have an archiver
        Response response = getBaseTarget().path(SERVICES).path("DistributedCache").path(PERSISTENCE)
                                           .path("archiveStores")
                                           .path("my-snapshot")
                                           .request().get();
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        response = getBaseTarget().path(SERVICES).path("DistributedCache").path(PERSISTENCE)
                                           .path(ARCHIVES)
                                           .request().get();
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        // try to delete a snapshot that doesn't exist
        response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(SNAPSHOTS).path("snapshot-that-doesnt-exist")
                .request().delete();
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        // try to recover a snapshot that doesn't exist
        response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(SNAPSHOTS).path("2-entries").path("snapshot-that-doesnt-exist")
                .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        // try to archive a snapshot that doesn't exist
        response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(ARCHIVES).path("snapshot-that-doesnt-exist")
                 .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        // try to delete an archived snapshot that doesn't exist
        response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(ARCHIVES).path("snapshot-that-doesnt-exist")
                .request().delete();
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testPersistence()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        String     sCacheName = "dist-persistence-test";
        NamedCache cache      = findApplication(SERVER_PREFIX + "-1").getCache(sCacheName);

        cache.clear();

        try
            {
            // create an empty snapshot
            createSnapshot("empty");
            ensureServiceStatusIdle();

            // assert the snapshot exists
            Eventually.assertThat(invoking(this).assertSnapshotExists("empty", SNAPSHOTS), is(true));

            // add some data
            cache.put("key-1", "value-1");
            cache.put("key-2", "value-2");
            assertThat(cache.size(), is(2));

            // create a second snapshot
            createSnapshot("2-entries");
            ensureServiceStatusIdle();
            Eventually.assertThat(invoking(this).assertSnapshotExists("2-entries", SNAPSHOTS), is(true));

            // archive the snapshot
            Response response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(ARCHIVES).path("2-entries")
                    .request().post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            response.close();
            ensureServiceStatusIdle();

            Eventually.assertThat(invoking(this).assertSnapshotExists("2-entries", ARCHIVES), is(true));

            // remove the local snapshot
            deleteSnapshot("2-entries");
            ensureServiceStatusIdle();
            Eventually.assertThat(invoking(this).assertSnapshotExists("2-entries", SNAPSHOTS), is(false));

            // retrieve the archived snapshot
            response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(ARCHIVES).path("2-entries").path("retrieve")
                    .request().post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            response.close();
            ensureServiceStatusIdle();

            // check the existence of the local snapshot but delay as a single member could have the snapshot but it not be complete
            Eventually.assertThat(invoking(this).assertSnapshotExists("2-entries", SNAPSHOTS), is(true));

            // delete the archived snapshot
            response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(ARCHIVES).path("2-entries")
                    .request().delete();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            response.close();
            ensureServiceStatusIdle();

            Eventually.assertThat(invoking(this).assertSnapshotExists("2-entries", ARCHIVES), is(false));

            // now we have local snapshot, clear the cache and then recover the snapshot
            cache.clear();
            assertThat(cache.size(), is(0));

            response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(SNAPSHOTS).path("2-entries").path("recover")
                    .request().post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
            response.close();
            ensureServiceStatusIdle();

            Eventually.assertThat(invoking(this).assertCacheSize(cache, 2), is(true));

            // now delete the 2 snapshots

            deleteSnapshot("2-entries");
            ensureServiceStatusIdle();
            Eventually.assertThat(invoking(this).assertSnapshotExists("2-entries", SNAPSHOTS), is(false));

            deleteSnapshot("empty");
            ensureServiceStatusIdle();
            Eventually.assertThat(invoking(this).assertSnapshotExists("empty", SNAPSHOTS), is(false));
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
     * Test to validate fix for bug {@code 30914372}.
     * For each compound {@code rel}s (a rel containing a / character), ensure
     * the resulting URL doesn't have the '/' encoded.
     */
    @Test
    public void test30914372()
        {
        WebTarget target   = getBaseTarget().path(MEMBERS).path("1");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response));

        Object objListLinks = mapResponse.get("links");
        assertThat(objListLinks, instanceOf(List.class));

        for (Map<String,String> link : (List<LinkedHashMap>) objListLinks)
            {
            String sRel = link.get("rel");
            if (sRel.indexOf('/') != -1) // non-null implies compound rel
                {
                // confirm the generated URL doesn't have the rel improperly encoded
                String sUrl = link.get("href");
                assertTrue("Improperly encoded URL: " + sUrl, sUrl.endsWith(sRel));
                }
            }
        }

    @Test
    public void testClearCache()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());
        final String CACHE_NAME = CLEAR_CACHE_NAME;

        m_aMembers[0].invoke((RemoteCallable<Object>) () ->
            {
            // fill a cache
            NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
            Binary     binValue = Binary.getRandomBinary(1024, 1024);
            cache.clear();
            for (int i = 0; i < 10; ++i)
                {
                cache.put(i, binValue);
                }
            return null;
            });
        Base.sleep(REMOTE_MODEL_PAUSE_DURATION);

        Eventually.assertDeferred(
                () -> getBaseTarget().path(STORAGE).path(CACHE_NAME).path(CLEAR).request().post(null).getStatus(),
                is(Response.Status.OK.getStatusCode()));
        m_aMembers[0].invoke((RemoteCallable<Object>) () ->
            {
            NamedCache cache = CacheFactory.getCache(CACHE_NAME);
            Eventually.assertDeferred(cache::size, is(0));
            return null;
            });
        }

    @Test
    public void testReadOnlyClearCache()
        {
        // only run when read-only management is enabled
        Assume.assumeTrue(isReadOnly());
        final String CACHE_NAME = CLEAR_CACHE_NAME;
        m_aMembers[0].invoke((RemoteCallable<Object>) () ->
            {
            NamedCache cache = CacheFactory.getCache(CACHE_NAME);
            cache.clear();
            cache.put("key", "value");
            return null;
            });
        Base.sleep(REMOTE_MODEL_PAUSE_DURATION);

        Eventually.assertDeferred(
                () -> getBaseTarget().path(STORAGE).path(CACHE_NAME).path(CLEAR).request().post(null).getStatus(),
                is(Response.Status.BAD_REQUEST.getStatusCode()));
        m_aMembers[0].invoke((RemoteCallable<Object>) () ->
            {
            NamedCache cache = CacheFactory.getCache(CACHE_NAME);
            Eventually.assertDeferred(cache::size, is(1));
            return null;
            });
        }

    @Test
    public void testTruncateCache()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());
        final String CACHE_NAME = CLEAR_CACHE_NAME;

        m_aMembers[0].invoke((RemoteCallable<Object>) () ->
            {
            // fill a cache
            NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
            Binary     binValue = Binary.getRandomBinary(1024, 1024);
            cache.clear();
            for (int i = 0; i < 10; ++i)
                {
                cache.put(i, binValue);
                }
            return null;
            });
        Base.sleep(REMOTE_MODEL_PAUSE_DURATION);

        Eventually.assertDeferred(
                () -> getBaseTarget().path(STORAGE).path(CACHE_NAME).path(TRUNCATE).request().post(null).getStatus(),
                is(Response.Status.OK.getStatusCode()));
        m_aMembers[0].invoke((RemoteCallable<Object>) () ->
            {
            NamedCache cache = CacheFactory.getCache(CACHE_NAME);
            Eventually.assertDeferred(cache::size, is(0));
            return null;
            });
        }

    @Test
    public void testReadOnlyTruncateCache()
        {
        // only run when read-only management is enabled
        Assume.assumeTrue(isReadOnly());
        final String CACHE_NAME = CLEAR_CACHE_NAME;
        m_aMembers[0].invoke((RemoteCallable<Object>) () ->
            {
            NamedCache cache = CacheFactory.getCache(CACHE_NAME);
            cache.clear();
            cache.put("key", "value");
            return null;
            });
        Base.sleep(REMOTE_MODEL_PAUSE_DURATION);

        Eventually.assertDeferred(
                () -> getBaseTarget().path(STORAGE).path(CACHE_NAME).path(TRUNCATE).request().post(null).getStatus(),
                is(Response.Status.BAD_REQUEST.getStatusCode()));
        m_aMembers[0].invoke((RemoteCallable<Object>) () ->
            {
            NamedCache cache = CacheFactory.getCache(CACHE_NAME);
            Eventually.assertDeferred(cache::size, is(1));
            return null;
            });
        }

    // ----- utility methods----------------------------------------------------

    /**
     * Ensure that the operationalStatus is Idle.
     */
    private void ensureServiceStatusIdle()
        {
        // delay by 5 seconds to give time for the status ti change if required.
        Eventually.assertThat(invoking(this).assertServiceIdle(),is(true), Eventually.delayedBy(5, TimeUnit.SECONDS));
        }
    
    /**
     * Delete the given snapshot.
     *
     * @param sSnapshotName snapshot to delete
     */
    private void deleteSnapshot(String sSnapshotName)
        {
        Response response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(SNAPSHOTS).path(sSnapshotName)
                .request().delete();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        response.close();
        }

    /**
     * Create the given snapshot.
     *
     * @param sSnapshotName snapshot to create
     */
    private void createSnapshot(String sSnapshotName)
        {
        Response response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(SNAPSHOTS).path(sSnapshotName)
                 .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        response.close();
        }

    public boolean assertCacheSize(NamedCache cache, int nSize)
        {
        return cache.size() == nSize;
        }

    /**
     * Assert that the persistence status is idle.
     * 
     * @return
     */
    public boolean assertServiceIdle()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).queryParam("fields", "operationStatus");
        Response  response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapResponse = readEntity(target, response);
        assertThat(mapResponse, is(notNullValue()));

        String result = (String) mapResponse.get("operationStatus");
        assertThat(result, is(notNullValue()));
        if ("Idle".equals(result))
            {
            return true;
            }
        System.err.println("Waiting as Status of " + ACTIVE_SERVICE + " is " + result);
        return false;
        }
    
    /**
     * Assert a snapshot exists.
     *
     * @param sSnapshotName  the snapshot name
     * @param sType          the type of snapshot, archive or snapshot
     * @return true if it exists
     */
    public boolean assertSnapshotExists(String sSnapshotName, String sType)
        {
        return getSnapshotsInternal(sType).contains(sSnapshotName);
        }

    /**
     * Return archived snapshots.
     *
     * @return archives snapshots
     */
    private Set<String> getArchivedSnapshots()
        {
        return getSnapshotsInternal(ARCHIVES);
        }

    /**
     * Returns snapshots.
     * @return snapshots
     */
    private Set<String> getSnapshots()
        {
        return getSnapshotsInternal(SNAPSHOTS);
        }

    /**
     * Returns the list of snapshots for the "ACTIVE_SERVICE" service.
     *
     * @param sType either "archives" or "snapshots"
     *
     * @return the list of snapshots
     */
    private Set<String> getSnapshotsInternal(String sType)
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(sType);
        Response  response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapResponse = readEntity(target, response);
        assertThat(mapResponse, is(notNullValue()));

        ArrayList<String> result = (ArrayList) mapResponse.get(sType);
        assertThat(result, is(notNullValue()));

        return new HashSet<>(result);
        }

    /**
     * Return the response from a reporter instance.
     * @param sMember  member id or memberName
     * @return the {@link LinkedHashMap} respone
     */
    public LinkedHashMap getReporterResponse(String sMember)
        {
        WebTarget target   = getBaseTarget().path(REPORTERS).path(sMember);
        Response  response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        return mapResponse;
        }

    private void setReporterAttribute(String sMember, String sAttribute, Object value)
        {
        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put(sAttribute, value);

        WebTarget target   = getBaseTarget().path(REPORTERS).path(sMember);
        Response  response = target.request().post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    public boolean assertReporterAttribute(String sMember, String sAttribute, Object value)
        {
        LinkedHashMap mapResults = getReporterResponse(sMember);
        Object  result = mapResults.get(sAttribute);
        assertThat(result, is(notNullValue()));
        if (result instanceof String)
            {
            return (value).equals(result);
            }
        else if (result instanceof Integer)
            {
            return ((Integer) result).intValue() == ((Integer)value).intValue();
            }
        else if (result instanceof Long)
            {
            return ((Long) result).longValue() == ((Long)value).longValue();
            }
        else if (result instanceof Float)
            {
            return Float.compare((Float) result, (Float) value) == 0;
            }
        throw new IllegalArgumentException("Type of " + value.getClass() + " not supported");
        }

    public boolean assertReporterAttribute(String sMember, String sAttribute, Object[] values)
        {
        for (Object value : values)
            {
            if (assertReporterAttribute(sMember, sAttribute, value))
                {
                return true;
                }
            }

        return false;
        }

    private String getSelfLink(Map mapResponse)
        {
        return getLink(mapResponse, "self");
        }

    private String getLink(Map mapResponse, String sLinkName)
        {
        List<LinkedHashMap> linksMap =  (List) mapResponse.get("links");
        assertThat(linksMap, notNullValue());
        LinkedHashMap selfLinkMap = linksMap.stream().filter(m -> m.get("rel").
                equals(sLinkName))
                .findFirst()
                .orElse(new LinkedHashMap());

        String sSelfLink = (String) selfLinkMap.get("href");
        assertThat(sSelfLink, notNullValue());
        return sSelfLink;
        }

    private void testBackCacheResponse(WebTarget target, Response response)
        {
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response));

        String    sMembersUrl = getLink(mapResponse, "members");
        WebTarget memTarget   = m_client.target(sMembersUrl).queryParam("tier", "back");

        response = memTarget.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapCacheMembers = new LinkedHashMap(readEntity(memTarget, response));
        assertThat(mapCacheMembers, notNullValue());

        List<LinkedHashMap> listCacheMembers = (List<LinkedHashMap>) mapCacheMembers.get("items");
        assertThat(listCacheMembers, notNullValue());

        for(LinkedHashMap mapCacheMember : listCacheMembers)
            {
            assertThat(mapCacheMember.get("tier"), is("back"));
            assertThat(mapCacheMember.get(NAME), is(CACHE_NAME));
            assertThat(mapCacheMember.get("size"), instanceOf(Number.class));
            assertThat(mapCacheMember.get("cacheHits"), instanceOf(Number.class));

            String sSelfUrl = getSelfLink(mapCacheMember);

            assertThat(sSelfUrl, isOneOf(sMembersUrl+ "/" + mapCacheMember.get(NODE_ID),
                    sMembersUrl+ "/" + mapCacheMember.get(MEMBER)));

            assertThat(mapCacheMember.get("listenerFilterCount"), instanceOf(Number.class));
            assertThat(mapCacheMember.get("listenerKeyCount"), instanceOf(Number.class));

            memTarget = m_client.target(sSelfUrl);
            response = memTarget.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = new LinkedHashMap(readEntity(memTarget, response));

            List<LinkedHashMap> listCacheMembersOfANode = (List<LinkedHashMap>) mapResponse.get("items");
            assertThat(listCacheMembersOfANode, notNullValue());

            Set<Object> setCacheNames = listCacheMembersOfANode.stream()
                    .map(map -> map.get(NAME)).collect(Collectors.toSet());
            assertThat(setCacheNames.size(), is(1));
            assertThat(setCacheNames.iterator().next(), is(CACHE_NAME));
            }
        }

    private void testCachesResponse(WebTarget target, Response response)
        {
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = readEntity(target, response);
        assertThat(mapResponse, notNullValue());
        List<LinkedHashMap> listCacheMaps = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());
        assertThat(listCacheMaps.size(), greaterThan(1));

        for (LinkedHashMap mapCache : listCacheMaps)
            {
            String sCacheName = (String) mapCache.get(NAME);
            assertThat(mapCache.get(NAME), isOneOf(CACHES_LIST));

            if (!sCacheName.equals("dist-persistence-test") && !sCacheName.equals(CLEAR_CACHE_NAME))
                {
                assertThat(Integer.valueOf((Integer) mapCache.get("size")), greaterThan(0));
                }

            assertThat(mapCache.get(SERVICE), isOneOf(SERVICES_LIST));
            Assert.assertNotNull(mapCache.get(NODE_ID));
            }

        WebTarget cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", "name,totalPuts");
        Response cachesResponse = cachesTarget.request().get();
        mapResponse = new LinkedHashMap(readEntity(cachesTarget, cachesResponse));
        listCacheMaps = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());
        assertThat(listCacheMaps.size(), greaterThan(1));

        for (LinkedHashMap mapCache : listCacheMaps)
            {
            if (!mapCache.get(NAME).equals("dist-persistence-test") && !mapCache.get(NAME).equals(CLEAR_CACHE_NAME))
                {
                assertThat((Integer) mapCache.get("totalPuts"), greaterThan(0));
                }
            }

        cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", "name,units");
        cachesResponse = cachesTarget.request().get();
        mapResponse = new LinkedHashMap(readEntity(cachesTarget, cachesResponse));
        listCacheMaps = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());

        for (LinkedHashMap mapCache : listCacheMaps)
            {
            assertThat(mapCache.get(NAME), isOneOf(CACHES_LIST));
            if (mapCache.get(NAME).equals("dist-foo"))
                {
                assertThat(mapCache.get("units"), anyOf(is(Integer.valueOf(1)), is(Integer.valueOf(20))));
                }
            else
                {
                if (!mapCache.get(NAME).equals("dist-persistence-test") && !mapCache.get(NAME).equals(CLEAR_CACHE_NAME))
                    {
                    assertThat(mapCache.get("units"), is(1));
                    }
                }
            }

        cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", "name,insertCount");
        cachesResponse = cachesTarget.request().get();
        mapResponse = new LinkedHashMap(readEntity(cachesTarget, cachesResponse));
        listCacheMaps = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());

        for (LinkedHashMap mapCache : listCacheMaps)
            {
            assertThat(mapCache.get(NAME), isOneOf(CACHES_LIST));
            if (!mapCache.get(NAME).equals("dist-persistence-test"))
                {
                assertThat((Integer) mapCache.get("insertCount"), greaterThan(0));
                }
            }

        cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", SERVICE);
        cachesResponse = cachesTarget.request().get();
        mapResponse = new LinkedHashMap(readEntity(cachesTarget, cachesResponse));
        listCacheMaps = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());

        for (LinkedHashMap mapCache : listCacheMaps)
            {
            assertNull(mapCache.get(NAME));
            assertThat(mapCache.get(SERVICE), isOneOf(SERVICES_LIST));
            }
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

                CacheFactory.log("Management HTTP Acceptor lookup returned: " + m_baseURI, CacheFactory.LOG_INFO);
                }
            return client.target(m_baseURI);
            }
        catch(IOException | URISyntaxException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    public Object getAttributeValue(Client client, String sService, String sAttributeName)
        {
        WebTarget target   = getBaseTarget(client).path(SERVICES).path(sService).path("members").path(SERVER_PREFIX + "-1");
        Response  response = target.request().get();

        assertThat(response.getStatus(), CoreMatchers.is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = readEntity(target, response);
        return mapResponse.get(sAttributeName);
        }

    private void assertLongSummaryStats(Object oLongSummary, long... values)
        {
        LongSummaryStatistics stats = new LongSummaryStatistics();
        Arrays.stream(values).forEach(l -> stats.accept(l));
        assertThat(oLongSummary, instanceOf(Map.class));
        Map maLongSummary = (Map) oLongSummary;
        assertThat(Long.parseLong(maLongSummary.get("count").toString()), is(stats.getCount()));
        assertThat(Long.parseLong(maLongSummary.get("max").toString()), is(stats.getMax()));
        assertThat(Long.parseLong(maLongSummary.get("min").toString()), is(stats.getMin()));
        assertThat(Long.parseLong(maLongSummary.get("sum").toString()), is(stats.getSum()));
        assertThat(Double.parseDouble(maLongSummary.get("average").toString()), is(stats.getAverage()));
        }

    private void assertIntSummaryStats(Object oIntSummary, int... values)
        {
        IntSummaryStatistics stats = new IntSummaryStatistics();
        Arrays.stream(values).forEach(l -> stats.accept(l));
        assertThat(oIntSummary, instanceOf(Map.class));
        Map maLongSummary = (Map) oIntSummary;
        assertThat(Long.parseLong(maLongSummary.get("count").toString()), is(stats.getCount()));
        assertThat(Integer.parseInt(maLongSummary.get("max").toString()), is(stats.getMax()));
        assertThat(Integer.parseInt(maLongSummary.get("min").toString()), is(stats.getMin()));
        assertThat(Long.parseLong(maLongSummary.get("sum").toString()), is(stats.getSum()));
        assertThat(Double.parseDouble(maLongSummary.get("average").toString()), is(stats.getAverage()));
        }

    /**
     * Assert that the response does not conain any "messages" elements, which
     * would indicate errors.
     *
     * @param response Response
     */
    private void assertNoMessages(Response  response)
        {
        LinkedHashMap mapResponse = response.readEntity(LinkedHashMap.class);
        List<LinkedHashMap> listMessages = (List) mapResponse.get("messages");
        assertThat(listMessages, nullValue());
        }

    /**
     * Returns whether the GC in use is G1.
     *
     * @return true if the GC in use is G1; false otherwise
     */
    protected boolean isG1()
        {
        List<GarbageCollectorMXBean> gcMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean mxBean : gcMxBeans)
            {
            if (mxBean.getName().contains("G1"))
                {
                return true;
                }
            }

        return false;
        }


    /**
     * Returns a URL string with no unsafe characters.
     *
     * @return a URL string with no unsafe characters
     */
    protected String encodeValue(String sValue) throws UnsupportedEncodingException
        {
        return URLEncoder.encode(sValue, StandardCharsets.UTF_8.toString());
        }

    /**
     * Test distributed cache service attributes.
     *
     * @param mapResponse
     */
    protected void testDistServiceInfo(LinkedHashMap mapResponse)
        {
        System.out.println(mapResponse.toString());

        ArrayList<Integer> ownedPartitions = (ArrayList<Integer>) mapResponse.get("ownedPartitionsPrimary");
        assertThat(ownedPartitions.size(), is(2));
        assertThat(ownedPartitions.get(0) + ownedPartitions.get(1), is(257));

        ownedPartitions = (ArrayList<Integer>) mapResponse.get("ownedPartitionsBackup");
        assertThat(ownedPartitions.size(), is(2));
        assertThat(ownedPartitions.get(0) + ownedPartitions.get(1), is(257));

        ArrayList<Integer> partitionsStatus = (ArrayList<Integer>) mapResponse.get("partitionsVulnerable");
        assertThat(partitionsStatus.size(), is(1));
        assertThat(partitionsStatus.get(0), is(257));

        partitionsStatus = (ArrayList<Integer>) mapResponse.get("partitionsAll");
        assertThat(partitionsStatus.size(), is(1));
        assertThat(partitionsStatus.get(0), is(257));

        partitionsStatus = (ArrayList<Integer>) mapResponse.get("partitionsEndangered");
        assertThat(partitionsStatus.size(), is(1));
        assertThat(partitionsStatus.get(0), is(0));

        partitionsStatus = (ArrayList<Integer>) mapResponse.get("partitionsUnbalanced");
        assertThat(partitionsStatus.size(), is(1));
        assertThat(partitionsStatus.get(0), is(0));

        assertThat(((LinkedHashMap) mapResponse.get("requestAverageDuration")).size(), is(5));

        assertThat(Long.parseLong(mapResponse.get("taskMaxBacklog").toString()), greaterThanOrEqualTo(0L));

        assertThat(((LinkedHashMap) mapResponse.get("storageEnabled")).get("true"), is(2));
        assertThat(((LinkedHashMap) mapResponse.get("threadPoolSizingEnabled")).get("true"), is(2));

        assertThat(((ArrayList<Integer>) mapResponse.get("joinTime")).size(), is(2));
        assertThat(((ArrayList<Integer>) mapResponse.get("statistics")).size(), is(2));

        // an uninitialized attribute should not be returned
        assertFalse(mapResponse.containsKey("persistenceActiveSpaceTotal"));

        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").queryParam("fields", "requestTotalCount");
        Response  response = target.request().get();
        mapResponse = readEntity(target, response);

        assertThat(((Number) mapResponse.get("requestTotalCount")).intValue(), greaterThanOrEqualTo(0));

        target = getBaseTarget().path(SERVICES).path("DistributedCache").queryParam("fields", "partitionsAll")
                .queryParam("role", "*");
        response = target.request().get();
        mapResponse = readEntity(target, response);

        Collection<Integer> colPartCount = (Collection) mapResponse.get("partitionsAll");
        assertEquals(1, colPartCount.size());
        assertThat(colPartCount, Matchers.hasItem(257));

        target = getBaseTarget().path(SERVICES).path("DistributedCache").queryParam("fields", "statusHA")
                .queryParam("role", "*");
        response = target.request().get();
        mapResponse = readEntity(target, response);

        assertThat((Collection<String>) mapResponse.get("statusHA"), Matchers.hasItem("NODE-SAFE"));

        target = getBaseTarget().path(SERVICES).path("DistributedCache").queryParam("fields", "taskCount")
                .queryParam("collector", "list")
                .queryParam("role", "*");
        response = target.request().get();
        mapResponse = readEntity(target, response);
        // test specifying a custom collector
        assertThat(((Collection) mapResponse.get("taskCount")).size(), greaterThan(1));


        target = getBaseTarget().path(SERVICES).path("DistributedCache").queryParam("fields", "taskCount")
                .queryParam("collector", "list")
                .queryParam("role", SERVER_PREFIX + "-1");
        response = target.request().get();
        mapResponse = readEntity(target, response);

        assertThat(((Collection) mapResponse.get("taskCount")).size(), is(1));
        }

    /**
     * Test distributed cache services attributes.
     *
     * @param mapResponse
     */
    protected void testDistServicesInfo(LinkedHashMap mapResponse)
        {
        System.out.println(mapResponse.toString());

        WebTarget target   = getBaseTarget().path(SERVICES).queryParam("fields", "statusHA");
        Response  response = target.request().get();
        mapResponse = readEntity(target, response);
        List<LinkedHashMap> listServiceMaps = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listServiceMaps, notNullValue());
        assertThat(listServiceMaps.size(), greaterThan(1));

        int cServices = 0;
        for (LinkedHashMap mapService : listServiceMaps)
            {
            // if the mapService has more than 3 items, check that it contains statusHA
            if (mapService.size() > 3)
                {
                cServices++;
                assertThat((Collection<String>) mapService.get("statusHA"), Matchers.hasItem("NODE-SAFE"));
                }
            }
        assertThat(cServices, is(2));

        target = getBaseTarget().path(SERVICES).queryParam("fields", "taskCount")
                .queryParam("collector", "list")
                .queryParam("role", "*");
        response = target.request().get();
        mapResponse = readEntity(target, response);
        listServiceMaps = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listServiceMaps, notNullValue());
        assertThat(listServiceMaps.size(), greaterThan(1));

        cServices = 0;
        for (LinkedHashMap mapService : listServiceMaps)
            {
            if (mapService.size() > 3)
                {
                int taskCount = (Integer) mapService.get("taskCount");
                if (taskCount > 0)
                    {
                    cServices++;
                    }
                }
            }
        assertThat(cServices, is(2));
        }

    protected LinkedHashMap readEntity(WebTarget target, Response response)
            throws ProcessingException
        {
        return (LinkedHashMap) readEntity(target, response, null, LinkedHashMap.class);
        }

    protected Object readEntity(WebTarget target, Response response, Class aClass)
            throws ProcessingException
        {
        return readEntity(target, response, null, aClass);
        }

    protected LinkedHashMap readEntity(WebTarget target, Response response, Entity entity)
            throws ProcessingException
        {
        return (LinkedHashMap) readEntity(target, response, entity, LinkedHashMap.class);
        }

    protected Object readEntity(WebTarget target, Response response, Entity entity, Class aClass )
        throws ProcessingException
    {
        int i = 0;
        while (true)
            {
            try
                {
                Object mapReturned = response.readEntity(aClass);
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
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            i++;
            }
        }        

    //--------------------- helper classes ----------------------------

    /**
     * A RemoteCallable implementation to calculate the number of
     * unbalanced partitions.
     */
    public static class CalculateUnbalanced
            implements RemoteCallable<Integer>
        {
        public CalculateUnbalanced(String sCacheName)
            {
            f_sCacheName = sCacheName;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Integer call()
            {
            try
                {
                SafeService      serviceSafe = (SafeService) CacheFactory.getCache(f_sCacheName).getCacheService();
                PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

                return serviceReal.calculateUnbalanced();
                }
            catch (Exception e)
                {
                CacheFactory.log(Base.printStackTrace(e));
                throw e;
                }
            }

        private final String f_sCacheName;
        }

    // ----- static helpers -------------------------------------------------

    /**
     * Return true and sleep for cMillis if the provided Supplier returns true.
     *
     * @param test     the condition to return a boolean
     * @param cMillis  the amount of time to sleep
     *
     * @return true if the provided Supplier returned true
     */
    protected static boolean sleep(Supplier<Boolean> test, long cMillis)
        {
        if (test.get())
            {
            Base.sleep(cMillis);
            return true;
            }
        return false;
        }

    /**
     * Set the test to run as read-only.
     *
     * @param fIsReadOnly indicates if read-only is true for management
     */
    protected static void setReadOnly(boolean fIsReadOnly)
        {
        s_fIsReadOnly = fIsReadOnly;
        }

    /**
     * Return if management is to be read-only.
     */
    protected static boolean isReadOnly()
        {
        return s_fIsReadOnly;
        }

    // ----- data members ------------------------------------------------------

    /**
     * The count of cluster members.
     */
    public static final int MEMBER_COUNT = 2;

    /**
     * The client object to be used for the tests.
     */
    protected static Client m_client;

    /**
     * The base URL for Management over REST requests.
     */
    protected static URI m_baseURI;

    /**
     * The cluster members.
     */
    protected static CoherenceClusterMember[] m_aMembers = new CoherenceClusterMember[MEMBER_COUNT];

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

    /**
     * A flag to indicate if management is to be read-only.
     */
    private static boolean s_fIsReadOnly = false;

    // ----- constants ------------------------------------------------------

    /**
     * The name persistence path.
     */
    private static final String PERSISTENCE = "persistence";

    /**
     * The snapshots path.
     */
    private static final String SNAPSHOTS = "snapshots";

    /**
     * The archives path.
     */
    private static final String ARCHIVES = "archives";

    /**
     * The services path.
     */
    private static final String SERVICES = "services";

    /**
     * The name of the active persistence service.
     */
    private static final String ACTIVE_SERVICE = "DistributedCachePersistence";

    /**
     * The name of the ﻿used PartitionedService.
     */
    protected static final String SERVICE_NAME = "DistributedCache";

    /**
     * The type of the PartitionedService service.
     */
    protected static final String SERVICE_TYPE  = "DistributedCache";

    /**
     * The name of Distributed cache.
     */
    protected static final String CACHE_NAME = "dist-test";

    /**
     * The near cache.
     */
    protected static final String NEAR_CACHE_NAME = "near-test";

    /**
     * The clear/truncate cache.
     */
    protected static final String CLEAR_CACHE_NAME = "dist-clear";

    /**
     * The list of services used by this test class.
     */
    private String[] SERVICES_LIST = {SERVICE_NAME, "ExtendHttpProxyService", "ExtendProxyService",
            "DistributedCachePersistence", "ManagementHttpProxy"};

    /**
     * The list of caches used by this test class.
     */
    private String[] CACHES_LIST = {CACHE_NAME, "near-test", "dist-foo", "dist-persistence-test", CLEAR_CACHE_NAME};

    /**
     * Prefix for the spawned processes.
     */
    protected static String SERVER_PREFIX = "testMgmtRESTServer";

    /**
     * Cache config used by the test and spawned processes.
     */
    protected static final String CACHE_CONFIG  = "server-cache-config-mgmt.xml";

    /**
     * Name of the Coherence cluster.
     */
    public static final String CLUSTER_NAME = "mgmtRestCluster";

    /**
     * The window of time the management server tries to give a consistent response.
     */
    protected static final long REMOTE_MODEL_PAUSE_DURATION = 128L + /*buffer*/ 16L;

    protected static final Boolean s_bTestJdk11 = Integer.valueOf(System.getProperty("java.version").split("\\.")[0]) > 10 ? true : false;

    /**
     * Flag to check whether running with Enterprise Performance Pack(EPP)
     */
    protected static final Boolean s_bIsEPP = System.getProperty("java.runtime.version").contains("-perf-") ? true : false;

    /**
     * The number of attributes to check for.
     */
    protected static final int ATTRIBUTES_COUNT = 20;
    }
