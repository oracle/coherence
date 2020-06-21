/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.bedrock.runtime.java.features.JmxFeature;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;

import com.tangosol.coherence.management.internal.resources.AbstractManagementResource;
import com.tangosol.coherence.management.internal.resources.ClusterMemberResource;

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
import java.net.SocketException;
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

import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;
import test.CheckJDK;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.MANAGEMENT;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.MEMBERS;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.METADATA_CATALOG;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.OPTIONS;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.REPORTERS;
import static com.tangosol.coherence.management.internal.resources.AbstractManagementResource.ROLE_NAME;
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
        propsServer1.setProperty("coherence.management.http.port", "0");

        try
            {
            m_dirActive   = FileHelper.createTempDir();
            m_dirSnapshot = FileHelper.createTempDir();
            m_dirArchive  = FileHelper.createTempDir();
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

        Eventually.assertThat(invoking(member2).getServiceStatus(SERVICE_NAME), is(ServiceStatus.NODE_SAFE));

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
        Response response = getBaseTarget().path(
                METADATA_CATALOG).request(MediaType.APPLICATION_JSON_TYPE).get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

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
        Response response = getBaseTarget().request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

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
        assertThat(linkNames, hasItem("services"));
        assertThat(linkNames, hasItem("caches"));
        }

    @Test
    public void testClusterMemberPlatformMemory()
        {
        Response response = getBaseTarget().request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        Object oListMemberIds = mapResponse.get("memberIds");
        assertThat(oListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) oListMemberIds;

        for (Object memberId : listMemberIds)
            {
            response = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path("memory").request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
            LinkedHashMap mapHeapUsage = (LinkedHashMap) mapResponse.get("heapMemoryUsage");
            assertThat(mapHeapUsage, notNullValue());
            assertThat((int) mapHeapUsage.get("used"), greaterThan(1));
            }
        }

    @Test
    public void testClusterMemberPlatformMemoryPoolTypeAttribute()
        {
        Response response = getBaseTarget().request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        Object oListMemberIds = mapResponse.get("memberIds");
        assertThat(oListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) oListMemberIds;

        String   GC_PREFIX      = s_bTestJdk11 ? "g1" : "ps";
        String[] arr_sMbeanName = { GC_PREFIX + "OldGen", GC_PREFIX + "SurvivorSpace"};

        for (String mbean : arr_sMbeanName)
            {
            for (Object memberId : listMemberIds)
                {
                response = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path(mbean).request().get();
                assertThat("unexpected response for Mgmt over REST API " + getBaseTarget().getUri().toString() + "/" + MEMBERS + "/" + memberId.toString() + "/platform/" + mbean, response.getStatus(), is(Response.Status.OK.getStatusCode()));
                mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
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
        }

    @Test
    public void testNonExistentClusterMember()
        {
        Response response = getBaseTarget().path(MEMBERS).path("nonexistent").request().get();
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }

    @Test
    public void testAllPlatformMbeans()
        {
        Response response = getBaseTarget().request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        Object objListMemberIds = mapResponse.get("memberIds");
        assertThat(objListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) objListMemberIds;
        Object memberId = listMemberIds.get(0);

        for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_URL_TO_MBEAN_QUERY.keySet())
            {
            WebTarget target = getBaseTarget().path("platform").path(platformMBean);

            CacheFactory.log(target.getUri().toString(), LOG_INFO);

            response = target.request().get();
            assertThat(target.getUri().toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
            assertThat(mapResponse.size(), greaterThan(0));
            }

        if (isG1())
            {
            for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_G1_URL_TO_MBEAN_QUERY.keySet())
                {
                WebTarget target = getBaseTarget().path("platform").path(platformMBean);

                CacheFactory.log(target.getUri().toString(), LOG_INFO);

                response = target.request().get();
                assertThat(target.getUri().toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
                mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
                assertThat(mapResponse.size(), greaterThan(0));
                }
            }
        else
            {
            for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_PS_URL_TO_MBEAN_QUERY.keySet())
                {
                WebTarget target = getBaseTarget().path("platform").path(platformMBean);

                CacheFactory.log(target.getUri().toString(), LOG_INFO);

                response = target.request().get();
                assertThat(target.getUri().toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
                mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
                assertThat(mapResponse.size(), greaterThan(0));
                }
            }

        for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_URL_TO_MBEAN_QUERY.keySet())
            {
            WebTarget target = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path(platformMBean);

            CacheFactory.log(target.getUri().toString(), LOG_INFO);

            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
            assertThat(mapResponse.size(), greaterThan(0));
            }

        if (isG1())
            {
            for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_G1_URL_TO_MBEAN_QUERY.keySet())
                {
                WebTarget target = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path(platformMBean);

                CacheFactory.log(target.getUri().toString(), LOG_INFO);

                response = target.request().get();
                assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
                mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
                assertThat(mapResponse.size(), greaterThan(0));
                }
            }
        else
            {
            for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_PS_URL_TO_MBEAN_QUERY.keySet())
                {
                WebTarget target = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path(platformMBean);

                CacheFactory.log(target.getUri().toString(), LOG_INFO);

                response = target.request().get();
                assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
                mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
                assertThat(mapResponse.size(), greaterThan(0));
                }
            }
        }

    @Test
    public void testNetworkStats()
        {
        Response response = getBaseTarget().request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        Object objListMemberIds = mapResponse.get("memberIds");
        assertThat(objListMemberIds, instanceOf(List.class));

        List   listMemberIds = (List) objListMemberIds;
        Object oMemberId     = listMemberIds.get(0);

        response = getBaseTarget().path(MEMBERS).path(oMemberId.toString()).path("networkStats").request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse.size(), greaterThan(0));
        assertThat(mapResponse.get("publisherSuccessRate"), notNullValue());
        assertThat(mapResponse.get("threshold"), notNullValue());
        }

    @Test
    public void testTrackWeakest()
        {
        Response response = getBaseTarget().request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

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
        Response response = getBaseTarget().queryParam("fields", "running").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("clusterName"), nullValue());
        assertThat(mapResponse.get("running"), is(true));
        assertThat(mapResponse.size(), is(2));
        }

    @Test
    public void testJmxManagement()
        {
        Response response = getBaseTarget().request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        Object oListLinks = mapResponse.get("links");
        assertThat(oListLinks, instanceOf(List.class));

        List<LinkedHashMap> listLinks = (List) oListLinks;

        LinkedHashMap mapJmxManagement = listLinks.stream().filter(m -> m.get("rel").equals(MANAGEMENT))
                .findFirst() .orElse(new LinkedHashMap());
        String sJmxURl = (String) mapJmxManagement.get("href");

        response = m_client.target(sJmxURl).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("refreshOnQuery"), is(false));
        assertThat(mapResponse.get("expiryDelay"), is(1000));
        assertThat(mapResponse.get("refreshPolicy"), is("refresh-ahead"));
        }

    @Test
    public void testUpdateJmxManagementError()
        {
        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put("refreshPolicy", "nonExistent");
        Response response = getBaseTarget().path(MANAGEMENT)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        List<LinkedHashMap> listMessages = (List) mapResponse.get("messages");
        assertThat(listMessages, notNullValue());
        assertThat(listMessages.size(), is(1));

        LinkedHashMap mapMessage = listMessages.get(0);
        assertThat(mapMessage.get("field"), is("refreshPolicy"));
        }

    @Test
    public void testUpdateJmxManagement()
        {
        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put("expiryDelay", 2000L);
        mapEntity.put("refreshPolicy", "refresh-behind");
        Response response = getBaseTarget().path(MANAGEMENT)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapResonse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        List<LinkedHashMap> listMessages = (List) mapResonse.get("messages");
        assertThat(listMessages, nullValue());

        response = getBaseTarget().path(MANAGEMENT).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        mapResonse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
        assertThat(mapResonse.get("expiryDelay"), is(2000));
        assertThat(mapResonse.get("refreshPolicy"), is("refresh-behind"));
        }

    @Test
    public void testHeapDump()
        {
        Response response = getBaseTarget().path(DUMP_CLUSTER_HEAP).request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testHeapDumpWithRole()
        {
        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put(ROLE, "storage");
        Response response = getBaseTarget().path("dumpClusterHeap").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testLogClusterState()
        {
        Response response = getBaseTarget().path("logClusterState").request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testConfigureTracing()
        {
        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put(ROLE, "");
        mapEntity.put(TRACING_RATIO, 1.0f);
        Response response = getBaseTarget().path("configureTracing").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testNonExistentOperation()
        {
        Response response = getBaseTarget().path("nonExistent").request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);

        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }

    @Test
    public void testMembers()
        {
        Response response = getBaseTarget().request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapEntity = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        String sJmxURl = getLink(mapEntity, "members");

        response = m_client.target(sJmxURl).request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
        assertThat(mapEntity, notNullValue());

        List<LinkedHashMap> listMemberMaps = (List<LinkedHashMap>) mapResponse.get("items");

        assertThat(listMemberMaps, notNullValue());

        assertThat(listMemberMaps.size(), is(MEMBER_COUNT));

        for (LinkedHashMap mapMember : listMemberMaps)
            {
            Object oId = mapMember.get("id");
            assertThat(mapMember.get("id"), is(notNullValue()));
            assertThat(mapMember.get("roleName"), is(notNullValue()));
            assertThat(mapMember.get("nodeId"), is(notNullValue()));

            Object oMemberLinks = mapMember.get("links");
            assertThat(oMemberLinks, instanceOf(List.class));

            String sMemberUrl = getSelfLink(mapMember);

            response = m_client.target(sMemberUrl).request().get();

            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
            assertThat(mapResponse.get("id"), is(oId));
            }
        }

    @Test
    public void testMemberLogState()
        {
        Response response = getBaseTarget().request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

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
    public void testMemberDumpHeap()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response));

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
        Response response = getBaseTarget().request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

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
        // This test requires Flight Recorder and only runs on Oracle JVMs
        CheckJDK.assumeOracleJDK();

        Response response = getBaseTarget().path(DIAGNOSTIC_CMD)
                .path("jfrStart")
                .queryParam(OPTIONS, encodeValue("name=all"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        String result = response.readEntity(String.class);
        assertThat(result.indexOf(SERVER_PREFIX + "-1"), greaterThan(0));
        assertThat(result.indexOf(SERVER_PREFIX + "-2"), greaterThan(0));

        File   folder   = s_tempFolder.newFolder();
        String sJfr1    = folder.getCanonicalPath() + File.separator + "all1.jfr";
        String sJfrPath = folder.getCanonicalPath() + File.separator;
        response = getBaseTarget().path(DIAGNOSTIC_CMD)
                .path("jfrDump")
                .queryParam(OPTIONS, encodeValue("name=all,filename=" + sJfr1))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        response = getBaseTarget().path(DIAGNOSTIC_CMD)
                .path("jfrDump")
                .queryParam(OPTIONS, encodeValue("name=all"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        result = response.readEntity(String.class);

        // for JDK11, when dump file is not provided,
        // DiagnosticCommand generates a file name instead of throwing exception
        if (!s_bTestJdk11)
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

        assertThat(new File(folder.getCanonicalPath() + File.separator + "1-all1.jfr").exists(), is(true));
        assertThat(new File(folder.getCanonicalPath() + File.separator + "2-all1.jfr").exists(), is(true));
        assertThat(new File(sJfrPath + File.separator + "1-all.jfr").exists(), is(true));
        assertThat(new File(sJfrPath + File.separator + "2-all.jfr").exists(), is(true));
        }

    @Test
    public void testJfrWithRole()
            throws Exception
        {
        // This test requires Flight Recorder and only runs on Oracle JVMs
        CheckJDK.assumeOracleJDK();

        String  sFilePath = "target" + File.separator + "test-output"  + File.separator + "functional" + File.separator;
        String  sFileName = sFilePath + "testMemberJfr-myRecording.jfr";

        Response response = getBaseTarget().path(DIAGNOSTIC_CMD)
                .path("jfrStart")
                .queryParam(OPTIONS, encodeValue("name=myJfr,duration=3s,filename="+ sFileName))
                .queryParam(ROLE_NAME, SERVER_PREFIX + "-1")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        String result = response.readEntity(String.class);

        File testFile1 = new File(sFilePath +"1-testMemberJfr-myRecording.jfr");
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

        File     folder   = s_tempFolder.newFolder();
        String   sJfr1    = folder.getCanonicalPath() + File.separator + "foo1.jfr";
        String   sJfr2    = folder.getCanonicalPath() + File.separator + "foo2.jfr";
        Response response = getBaseTarget().request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
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
            Thread.sleep(2000);

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

            response = getBaseTarget().path("members").path(nMemberId + "")
                    .path(DIAGNOSTIC_CMD)
                    .path("jfrStop")
                    .queryParam(OPTIONS, encodeValue("name=foo,filename=" + sJfr2)).request(MediaType.APPLICATION_JSON_TYPE)
                    .post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            assertThat(new File(sJfr1).exists(), is(true));
            assertThat(new File(sJfr2).exists(), is(true));
            }
        catch (UnsupportedEncodingException | InterruptedException e)
            {
            // log the exception
            CacheFactory.log("testMemberJfr() failed with exception: ");
            CacheFactory.log(e);
            }
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
            mBeanServer.invoke(oName, "vmUnlockCommercialFeatures", null, null);
            }
        catch (Exception InstanceNotFoundException)
            {
            try
                {
                String sName = "Coherence:type=DiagnosticCommand,Domain=com.sun.management,subType=DiagnosticCommand,cluster=mgmtRestCluster,member=" + m_aMembers[1].getName() + ",nodeId=" + m_aMembers[1].getLocalMemberId();

                mBeanServer = m_aMembers[1].get(JmxFeature.class).getDeferredJMXConnector().get().getMBeanServerConnection();
                oName = new ObjectName(sName);
                mBeanServer.invoke(oName, "vmUnlockCommercialFeatures", null, null);
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
        Response response = getBaseTarget().path("services").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

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
            if (((String) listItemMaps.get(i).get("name")).compareToIgnoreCase(SERVICE_NAME) == 0)
                {
                sDistServiceName = SERVICE_NAME;
                mapDistScheme = listItemMaps.get(i);
                }
            else
            if (((String) listItemMaps.get(i).get("name")).compareToIgnoreCase(sMoRESTProxy) == 0)
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

        assertThat(mapDistScheme.get("name"), is(SERVICE_NAME));
        assert (((Collection) mapDistScheme.get("type")).contains(SERVICE_TYPE));

        String sSelfLink = getSelfLink(mapDistScheme);

        response = m_client.target(sSelfLink).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapService = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
        assertThat(mapService, notNullValue());
        assertThat(mapService.get("name"), is(mapDistScheme.get("name")));

        testDistServiceInfo(mapService);

        assertThat(mapProxyScheme.get("name"), is(sMoRESTProxy));
        assertThat((Collection<String>) mapProxyScheme.get("type"), Matchers.hasItem("Proxy"));

        sSelfLink = getSelfLink(mapProxyScheme);

        response = m_client.target(sSelfLink).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        mapService = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
        assertThat(mapService, notNullValue());
        String sName = (String) mapService.get("name");
        assertThat(sName, is(mapProxyScheme.get("name")));
        assertThat(((ArrayList) mapService.get("quorumStatus")).get(0), is("Not configured"));

        response = getBaseTarget().path("services").path(sName)
                .queryParam("fields", "storageEnabled").request().get();
        mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
        assertThat(((LinkedHashMap) mapResponse.get("storageEnabled")).get("false"), is(1));
        }

    @Test
    public void testServiceMembers()
        {
        Response response = getBaseTarget().path("services").path("DistributedCache").path("members").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());

        List<LinkedHashMap> items = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(items, notNullValue());
        assertThat(items.size(), is(2));

        for(LinkedHashMap mapEntry : items)
            {
            assertThat(mapEntry.get("name"), is(SERVICE_NAME));
            assertThat(mapEntry.get("type"), is(SERVICE_TYPE));
            assertThat(Integer.parseInt(mapEntry.get("nodeId").toString()), greaterThan(0));
            assertThat(mapEntry.get("backupCount"), is(1));
            assertThat(mapEntry.get("joinTime"), notNullValue());
            assertThat(mapEntry.get("links"), notNullValue());

            String sSelfUrl = getSelfLink(mapEntry);
            response = m_client.target(sSelfUrl).request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            LinkedHashMap mapMemberResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

            assertThat(mapEntry.get("nodeId"), is(mapMemberResponse.get("nodeId")));
            assertThat(mapEntry.get("joinTime"), is(mapMemberResponse.get("joinTime")));
            }
        }

    @Test
    public void testManagementRequestWithAcceptEncodingGzip()
        {
        Response response = getBaseTarget().path("services").path("DistributedCache").path("members").request().header("Accept-Encoding", "gzip").get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("Content-Encoding"), is("gzip"));
        }

    @Test
    public void testPartitionInfo()
        {
        Response response = getBaseTarget().path("services").path("DistributedCache").path("partition").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("strategyName"), is("SimpleAssignmentStrategy"));
        assertThat(mapResponse.get("partitionCount"), is(257));
        assertThat(mapResponse.get("backupCount"), is(1));

        String sSelfUrl = getSelfLink(mapResponse);

        response = m_client.target(sSelfUrl).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapPartitionResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
        assertThat(mapPartitionResponse, is(mapResponse));
        }

    @Test
    public void testDirectServiceMember()
        {
        WebTarget membersTarget = getBaseTarget().path("services").path("DistributedCache").path("members");
        Response response = membersTarget
                .path(SERVER_PREFIX + "-1").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("name"), is(SERVICE_NAME));
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
        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put("threadCountMin", 5);
        WebTarget membersTarget = getBaseTarget().path("services").path("DistributedCache").path("members");
        Response response = membersTarget
                .path(SERVER_PREFIX + "-1").request().post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        response = membersTarget
                .path(SERVER_PREFIX + "-1").request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("threadCountMin"), is(5));
        }

    @Test
    public void testCacheMemberUpdate()
        {
        Map<String, Object> mapMethodValues = new HashMap<String, Object>()
            {{
               put("highUnits",   100005);
               put("expiryDelay", 60000);
            }};

        mapMethodValues.forEach((attribute, value) ->
            {
            LinkedHashMap map = new LinkedHashMap();
            map.put(attribute, value);
            WebTarget target = getBaseTarget().path("services").path(SERVICE_NAME).path("caches").path(CACHE_NAME)
                    .path("members").path(SERVER_PREFIX + "-1");
            Response response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
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

            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response));
            assertThat(mapResponse, notNullValue());
            assertThat(attribute + " should be " + value + ", but is " + mapResponse.get(attribute), mapResponse.get(attribute), is(value));
            });
        }

    @Test
    public void testUpdateReporterMember()
        {
        LinkedHashMap map     = new LinkedHashMap();
        String  sMember = SERVER_PREFIX + "-1";

        map.put("intervalSeconds", 15L);
        WebTarget target   = getBaseTarget().path(REPORTERS).path(sMember);
        Response  response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

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
        WebTarget target = getBaseTarget().path("members").path(SERVER_PREFIX + "-1");
        Response response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

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
        WebTarget target = getBaseTarget().path("services").path(SERVICE_NAME).path("caches").path(CACHE_NAME)
                .path("members").path(SERVER_PREFIX + "-1");
        Response response = target.request().post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        List<LinkedHashMap> listMessages = (List) mapResponse.get("messages");
        assertThat(listMessages, notNullValue());
        assertThat(listMessages.size(), is(1));

        LinkedHashMap mapMessages = listMessages.get(0);
        assertThat(mapMessages.get("field"), is("cacheHits"));
        }

    @Test
    public void testReporters()
        {
        Response response = getBaseTarget().path(REPORTERS).request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        List<LinkedHashMap> listItems = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(2));

        for (LinkedHashMap mapReports : listItems)
            {
            assertThat(mapReports.get("nodeId"), is(notNullValue()));
            assertThat(Long.parseLong(mapReports.get("intervalSeconds").toString()), greaterThan(1L));
            assertThat(Long.parseLong(mapReports.get("runLastMillis").toString()), greaterThan(-1L));
            assertThat(mapReports.get("outputPath"), is(notNullValue()));
            }
        }

    @Test
    public void testDirectReporter()
        {
        String sMember = SERVER_PREFIX + "-1";
        Response response = getBaseTarget().path(REPORTERS).path(sMember).request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("nodeId"), is("1"));
        assertThat(Long.parseLong(mapResponse.get("intervalSeconds").toString()), greaterThan(1L));
        assertThat(Long.parseLong(mapResponse.get("runLastMillis").toString()), greaterThan(-1L));
        assertThat(mapResponse.get("outputPath"), is(notNullValue()));
        assertThat(mapResponse.get("member"), is(sMember));
        }

    @Test
    public void testStartAndStopReporter() throws IOException
        {
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
        WebTarget target = getBaseTarget().path("services").path(SERVICE_NAME).path("caches").path(CACHE_NAME)
                .path("members").path(SERVER_PREFIX + "-1").path("resetStatistics");
        Response response = target.request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testServiceMemberResetStats()
        {
        WebTarget membersTarget = getBaseTarget().path("services").path(SERVICE_NAME).path("members");
        Response response = membersTarget
                .path(SERVER_PREFIX + "-1").path("resetStatistics").request().post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        membersTarget = getBaseTarget().path("services").path(SERVICE_NAME).path("members");
        response = membersTarget
                .path(SERVER_PREFIX + "-1").request().get();

        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(((Number) mapResponse.get("requestTotalCount")).intValue(), greaterThanOrEqualTo(0));
        }

    @Test
    public void testSuspendAndResume()
        {
        Response response = getBaseTarget().path("services").path(SERVICE_NAME).path("suspend")
                .request().post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        Eventually.assertThat(invoking(this).getAttributeValue(m_client, SERVICE_NAME, "quorumStatus"),
                is("Suspended"));

        response = getBaseTarget().path("services").path("DistributedCache").path("resume")
                .request().post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        Eventually.assertThat(invoking(this).getAttributeValue(m_client, SERVICE_NAME, "quorumStatus").toString(),
                containsString("allowed-actions"));
        }

    @Test
    public void testService()
        {
        // aggregate all attributes for a service across all nodes
        Response response  = getBaseTarget().path("services").path("DistributedCache").request().get();
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        testDistServiceInfo(mapResponse);
        }

    @Test
    public void testServices()
        {
        // aggregate all attributes for all services across all nodes
        WebTarget     target      = getBaseTarget().path("services");
        Response      response    = target.request().get();
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response));

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
            Response response  = getBaseTarget().path("caches").path(CACHE_NAME).queryParam("fields","units")
                    .queryParam("role", "*")
                    .request().get();
            LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

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
            Response response  = getBaseTarget().path("caches").path(CACHE_NAME).queryParam("fields","size")
                    .queryParam("role", "*")
                    .request().get();
            LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
            acTmp[0] = ((Number) mapResponse.get("size")).longValue();
            }
        while (sleep(() -> acTmp[0] != 10L, REMOTE_MODEL_PAUSE_DURATION));

        Response response  = getBaseTarget().path("services").path(SERVICE_NAME)
                .path("caches").path(CACHE_NAME).queryParam("fields","size")
                .request().get();
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        // aggregate all attributes for a cache on a service across all nodes
        assertThat(((Number) mapResponse.get("size")).longValue(), is(10L));

        response  = getBaseTarget().path("services").path(SERVICE_NAME)
                .path("caches").path(CACHE_NAME).queryParam("fields","units")
                .queryParam("role","*")
                .request().get();
        mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        // aggregate Units attribute for a cache across all nodes
        assertThat(((Number) mapResponse.get("units")).longValue(), is(cEntrySize * 10));

        response  = getBaseTarget().path("services").path(SERVICE_NAME)
                .path("caches").path(CACHE_NAME).queryParam("fields","units")
                .queryParam("role","*")
                .queryParam("collector", "list")
                .request().get();
        mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        // list the Units attribute for a cache across all nodes
        List<Integer> listUnits = (List) mapResponse.get("units");
        assertEquals(2, listUnits.size());
        int cMinUnits = ((int) cEntrySize) * 4;
        listUnits.forEach(NUnits -> assertThat(NUnits, greaterThanOrEqualTo(cMinUnits)));

        cache.clear();

        response = getBaseTarget().path("caches").path(CACHE_NAME).path("members").path(SERVER_PREFIX + "-1").path("resetStatistics")
                .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        response = getBaseTarget().path("caches").path(CACHE_NAME).path("members").path(SERVER_PREFIX + "-2").path("resetStatistics")
                .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
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

        response = getBaseTarget().path("caches").path(CACHE_NAME).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
        System.out.println(mapResponse.toString());

        assertEquals(nSize, ((Integer) mapResponse.get("size")).intValue());
        assertEquals(nSize, ((Integer) mapResponse.get("totalPuts")).intValue());
        assertEquals(nSize, ((Integer) mapResponse.get("totalGets")).intValue());
        }

    @Test
    public void testDirectServiceMemberWithIncludedFields()
        {
        WebTarget membersTarget = getBaseTarget().path("services").path("DistributedCache").path("members");
        Response response = membersTarget
                .path(SERVER_PREFIX + "-1").queryParam("fields", "backupCount,joinTime").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.size(), is(3));
        assertThat(mapResponse.get("backupCount"), is(1));
        assertThat(mapResponse.get("joinTime"), notNullValue());

        assertThat(mapResponse.get("name"), nullValue());
        }

    @Test
    public void testDirectServiceMemberWithExcludedFields()
        {
        WebTarget membersTarget = getBaseTarget().path("services").path("DistributedCache").path("members");
        Response response = membersTarget
                .path(SERVER_PREFIX + "-1").queryParam("excludeFields", "backupCount,joinTime").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("backupCount"), nullValue());
        assertThat(mapResponse.get("joinTime"), nullValue());
        assertThat(mapResponse.get("name"), notNullValue());
        }

    @Test
    public void testDirectServiceMemberWithIncludedAndExcludedFields()
        {
        WebTarget membersTarget = getBaseTarget().path("services").path("DistributedCache").path("members");
        Response response = membersTarget
                .path(SERVER_PREFIX + "-1")
                .queryParam("fields", "name,joinTime")
                .queryParam("excludeFields", "backupCount,joinTime").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("joinTime"), nullValue());
        assertThat(mapResponse.get("name"), notNullValue());
        }

    @Test
    public void testDirectServiceMemberWithExcludedLinks()
        {
        WebTarget membersTarget = getBaseTarget().path("services").path("DistributedCache").path("members");
        Response response = membersTarget
                .path(SERVER_PREFIX + "-1").queryParam("excludeLinks", "ownership").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse.get("links"), notNullValue());

        List<LinkedHashMap> listLinks = (List) mapResponse.get("links");

        Set<Object> linkNames = listLinks.stream().map(m -> m.get("rel")).collect(Collectors.toSet());
        assertThat(linkNames, not(hasItem("ownership")));
        }

    @Test
    public void testDirectServiceMemberWithIncludedAndExcludedLinks()
        {
        WebTarget membersTarget = getBaseTarget().path("services").path("DistributedCache").path("members");
        Response response = membersTarget
                .path(SERVER_PREFIX + "-1")
                .queryParam("links", "self,ownership")
                .queryParam("excludeLinks", "ownership,parent").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

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
        WebTarget membersTarget = getBaseTarget().path("services").path("DistributedCache").path("members");
        Response response = membersTarget
                .path(SERVER_PREFIX + "-1").path("ownership").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("ownership"), notNullValue());
        }

    @Test
    public void testOwnershipVerbose()
        {
        WebTarget membersTarget = getBaseTarget().path("services").path("DistributedCache").path("members");
        Response response = membersTarget
                .path(SERVER_PREFIX + "-1").path("ownership").queryParam("verbose", true).request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("ownership"), notNullValue());
        }

    @Test
    public void testDistributionState()
        {
        WebTarget membersTarget = getBaseTarget().path("services").path("DistributedCache").path("members");
        Response response = membersTarget
                .path(SERVER_PREFIX + "-1").path("distributionState").queryParam("verbose", true).request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("distributionState"), notNullValue());
        }

    @Test
    public void testPartitionScheduledDistributions()
        {
        WebTarget target = getBaseTarget().path("services").path("DistributedCache").path("partition")
                .path("scheduledDistributions").queryParam("verbose", true);
        Response response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("scheduledDistributions"), notNullValue());
        }

    @Test
    public void testReportNodeState()
        {
        WebTarget target = getBaseTarget().path("members").path(SERVER_PREFIX + "-1").path("state");

        Response response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("state"), notNullValue());
        assertThat((String) mapResponse.get("state"), containsString("Full Thread Dump"));
        }

    @Test
    public void testProxy()
        {
        Response response = getBaseTarget().path("services").path("ExtendProxyService").path("members").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());

        List<LinkedHashMap> listItems = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(2));

        for(LinkedHashMap mapEntry : listItems)
            {
            assertThat(mapEntry.get("name"), is("ExtendProxyService"));
            String sProxyUrl = getLink(mapEntry, "proxy");
            response = m_client.target(sProxyUrl).request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            LinkedHashMap mapMemberResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
            assertThat(mapMemberResponse.get("protocol"), notNullValue());
            }
        }

    @Test
    public void testProxyConnections()
        {
        Response response = getBaseTarget().path("services").path("ExtendProxyService").path("members").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        assertThat(mapResponse, notNullValue());

        List<LinkedHashMap> listItems = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(2));

        for(LinkedHashMap mapEntry : listItems)
            {
            assertThat(mapEntry.get("name"), is("ExtendProxyService"));

            String sProxyUrl = getLink(mapEntry, "proxy");
            response = m_client.target(sProxyUrl).path("connections").request().get(); // TODO to test actual proxy connection
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            }
        }

    @Test
    public void testCaches()
        {
        WebTarget target   = getBaseTarget().path("caches");
        Response  response = getBaseTarget().path("caches").request().get();
        testCachesResponse(target, response);
        }

    @Test
    public void testDistCache()
        {
        WebTarget  target = getBaseTarget().path("caches").path(CACHE_NAME);
        Response response = target.request().get();
        testBackCacheResponse(target, response);
        }

    @Test
    public void testNonExistentCache()
        {
        Response response = getBaseTarget().path("caches").path("nonexistent").request().get();
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }

    @Test
    public void testNonExistentService()
        {
        Response response = getBaseTarget().path("services").path("nonexistent").request().get();
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }

    @Test
    public void testNonExistentCacheInAService()
        {
        Response response = getBaseTarget().path("services").path(SERVICE_NAME).path("caches")
                .path("nonexistent").request().get();
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }

    @Test
    public void testNonExistentServiceCaches()
        {
        Response response = getBaseTarget().path("services").path("nonexistent").path("caches").request().get();
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }

    @Test
    public void testFrontCaches()
        {
        Response response = getBaseTarget().path("caches").path(NEAR_CACHE_NAME).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        String sMembersUrl = getLink(mapResponse, "members");
        response = m_client.target(sMembersUrl).queryParam("tier", "front").request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapCacheMembers = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
        assertThat(mapCacheMembers, notNullValue());

        List<LinkedHashMap> listCacheMembers = (List<LinkedHashMap>) mapCacheMembers.get("items");
        assertThat(listCacheMembers, notNullValue());
        assertThat(listCacheMembers.size(), is(1));

        for(LinkedHashMap mapCacheMember : listCacheMembers)
            {
            assertThat(mapCacheMember.get("tier"), is("front"));
            assertThat(mapCacheMember.get("name"), is(NEAR_CACHE_NAME));
            assertThat(mapCacheMember.get("size"), instanceOf(Number.class));
            assertThat(mapCacheMember.get("cacheHits"), instanceOf(Number.class));
            }
        }

    @Test
    public void testFrontAndBackCaches()
        {
        Response response = getBaseTarget().path("caches").path(NEAR_CACHE_NAME).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

        String sMembersUrl = getLink(mapResponse, "members");
        response = m_client.target(sMembersUrl).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapCacheMembers = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
        assertThat(mapCacheMembers, notNullValue());

        List<LinkedHashMap> listCacheMembers = (List<LinkedHashMap>) mapCacheMembers.get("items");

        assertThat(listCacheMembers.size(), is(3));
        assertThat(listCacheMembers, notNullValue());

        for(LinkedHashMap mapCacheMember : listCacheMembers)
            {
            assertThat(mapCacheMember.get("tier"), isOneOf("front", "back"));
            assertThat(mapCacheMember.get("name"), is(NEAR_CACHE_NAME));
            assertThat(mapCacheMember.get("size"), instanceOf(Number.class));
            assertThat(mapCacheMember.get("cacheHits"), instanceOf(Number.class));
            }
        }

    @Test
    public void testCachesOfAService()
        {
        WebTarget target   = getBaseTarget().path("services").path(SERVICE_NAME).path("caches");
        Response  response = target.request().get();
        testCachesResponse(target, response);
        }

    @Test
    public void testDistCacheOfService()
        {
        WebTarget target   = getBaseTarget().path("services").path(SERVICE_NAME).path("caches").path(CACHE_NAME);
        Response  response = target.request().get();
        testBackCacheResponse(target, response);
        }

    @Test
    public void testSimpleClusterSearch()
        {
        LinkedHashMap mapEntity = new LinkedHashMap();
        mapEntity.put("links", new String[]{});
        mapEntity.put("fields", new String[]{"clusterName","clusterSize"});

        Response response = getBaseTarget().path("search")
                .request().post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
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
        mapMembers.put("fields", new String[]{"nodeId","memberName"});
        mapChildren.put("members", mapMembers);


        Response response = getBaseTarget().path("search")
                .request().post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
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
            assertThat(mapMember.get("nodeId"), notNullValue());
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
        mapMembers.put("fields", new String[]{"name","type"});
        mapChildren.put("services", mapMembers);

        WebTarget target   = getBaseTarget().path("search");
        Entity    entyty   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = getBaseTarget().path("search")
                .request().post(entyty);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response, entyty));
        assertThat(mapResponse.size(), is(1));

        LinkedHashMap membersResponseMap = (LinkedHashMap) mapResponse.get("services");
        assertThat(membersResponseMap, notNullValue());
        List<LinkedHashMap> listMembers = (List<LinkedHashMap>) membersResponseMap.get("items");
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), is(4)); // <---- This is SO brittle!!!

        for (LinkedHashMap mapMember: listMembers)
            {
            assertThat(mapMember.size(), greaterThan(ATTRIBUTES_COUNT));
            assertThat(mapMember.get("name"), notNullValue());
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
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response, entity));
        assertThat(mapResponse.size(), is(1));

        LinkedHashMap membersResponseMap = (LinkedHashMap) mapResponse.get("services");
        assertThat(membersResponseMap, notNullValue());
        List<LinkedHashMap> listServices = (List<LinkedHashMap>) membersResponseMap.get("items");
        assertThat(listServices, notNullValue());
        assertThat(listServices.size(), is(4)); // <---- This is SO brittle!!!

        for (LinkedHashMap mapService: listServices)
            {
            assertThat(mapService.size(), greaterThan(ATTRIBUTES_COUNT));
            assertThat(mapService.get("name"), isOneOf(SERVICES_LIST));
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
                assertThat(memberMap.get("name"), notNullValue());
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
        mapServices.put("fields", new String[]{"name","type"});
        mapChildren.put("services", mapServices);

        LinkedHashMap cachesMap = new LinkedHashMap();
        cachesMap.put("links", new String[]{});
        cachesMap.put("fields", new String[]{"name"});

        LinkedHashMap mapServiceMembersChildren = new LinkedHashMap();
        mapServices.put("children", mapServiceMembersChildren);
        mapServiceMembersChildren.put("caches", cachesMap);

        WebTarget target   = getBaseTarget().path("search");
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response, entity));
        assertThat(mapResponse.size(), is(1));

        LinkedHashMap membersResponseMap = (LinkedHashMap) mapResponse.get("services");
        assertThat(membersResponseMap, notNullValue());
        List<LinkedHashMap> listMembers = (List<LinkedHashMap>) membersResponseMap.get("items");
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), is(4)); // <---- This is SO brittle!!!

        for (LinkedHashMap mapServiceMember: listMembers)
            {
            assertThat(mapServiceMember.get("name"), notNullValue());
            assertThat(mapServiceMember.get("type"), notNullValue());

            // no need to test for proxy services
            if (mapServiceMember.get("name").equals(HttpHelper.getServiceName()) ||
                    mapServiceMember.get("name").equals("ExtendProxyService") ||
                    mapServiceMember.get("name").equals(ACTIVE_SERVICE) ||
                    mapServiceMember.get("name").equals(MetricsHttpHelper.getServiceName()))
                {
                continue;
                }

            LinkedHashMap mapCachesResponse = (LinkedHashMap) mapServiceMember.get("caches");
            assertThat(mapCachesResponse, notNullValue());

            List<LinkedHashMap> listCacheItems = (List<LinkedHashMap>) mapCachesResponse.get("items");
            assertThat(listCacheItems, notNullValue());

            for (LinkedHashMap mapMember: listCacheItems)
                {
                assertThat(mapMember.size(), greaterThan(ATTRIBUTES_COUNT));
                assertThat(mapMember.get("name"), notNullValue());
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
        mapServices.put("fields", new String[]{"name","type"});

        mapEntity.put("children", new LinkedHashMap(){{put("services", mapServices);}});

        LinkedHashMap mapCaches = new LinkedHashMap();
        mapCaches.put("links", new String[]{});
        mapCaches.put("fields", new String[]{"name"});

        mapServices.put("children", new LinkedHashMap(){{put("caches", mapCaches);}});

        LinkedHashMap cachesMembersMap = new LinkedHashMap();
        cachesMembersMap.put("links", new String[]{});
        cachesMembersMap.put("fields", new String[]{"name", "size"});

        mapCaches.put("children", new LinkedHashMap(){{put("members", cachesMembersMap);}});

        WebTarget target   = getBaseTarget().path("search");
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response, entity));
        assertThat(mapResponse.size(), is(1));

        LinkedHashMap mapMembersResponse = (LinkedHashMap) mapResponse.get("services");
        assertThat(mapMembersResponse, notNullValue());
        List<LinkedHashMap> listMembers = (List<LinkedHashMap>) mapMembersResponse.get("items");
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), is(4)); // <---- This is SO brittle!!!

        for (LinkedHashMap mapService: listMembers)
            {
            assertThat(mapService.get("name"), notNullValue());
            assertThat(mapService.get("type"), notNullValue());

            // no need to test for proxy services
            if (mapService.get("name").equals(HttpHelper.getServiceName()) ||
                    mapService.get("name").equals("ExtendProxyService") ||
                    mapService.get("name").equals(ACTIVE_SERVICE) ||
                    mapService.get("name").equals(MetricsHttpHelper.getServiceName()))
                {
                continue;
                }

            LinkedHashMap cachesResponseMap = (LinkedHashMap) mapService.get("caches");
            assertThat(cachesResponseMap, notNullValue());

            List<LinkedHashMap> listCacheItems = (List<LinkedHashMap>) cachesResponseMap.get("items");
            assertThat(listCacheItems, notNullValue());

            for (LinkedHashMap mapMember: listCacheItems)
                {
                assertThat(mapMember.size(), greaterThan(ATTRIBUTES_COUNT));
                assertThat(mapMember.get("name"), notNullValue());

                LinkedHashMap mapCachesMembers = (LinkedHashMap) mapMember.get("members");
                assertThat(mapCachesMembers, notNullValue());

                List<LinkedHashMap> cacheMemberItems = (List<LinkedHashMap>) mapCachesMembers.get("items");

                for (LinkedHashMap mapCacheMember : cacheMemberItems)
                    {
                    assertThat(mapCacheMember.size(), is(2));
                    assertThat(mapCacheMember.get("name"), notNullValue());
                    assertThat(mapCacheMember.get("size"), notNullValue());
                    }
                }
            }
        }

    @Test
    public void testPersistenceFailureResponses()
        {
        // the following should fail with BAD REQUESTS

        // this service does not have an archiver
        Response response = getBaseTarget().path(SERVICES).path("DistributedCache").path(PERSISTENCE)
                                           .path("archiveStores")
                                           .path("my-snapshot")
                                           .request().get();
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        response = getBaseTarget().path(SERVICES).path("DistributedCache").path(PERSISTENCE)
                                           .path(ARCHIVES)
                                           .request().get();
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        // try to delete a snapshot that doesn't exist
        response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(SNAPSHOTS).path("snapshot-that-doesnt-exist")
                .request().delete();
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        // try to recover a snapshot that doesn't exist
        response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(SNAPSHOTS).path("2-entries").path("snapshot-that-doesnt-exist")
                .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));

        // try to archive a snapshot that doesn't exist
        response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(ARCHIVES).path("snapshot-that-doesnt-exist")
                 .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));

        // try to delete an archived snapshot that doesn't exist
        response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(ARCHIVES).path("snapshot-that-doesnt-exist")
                .request().delete();
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }

    @Test
    public void testPersistence()
        {
        String     sCacheName = "dist-persistence-test";
        NamedCache cache      = findApplication(SERVER_PREFIX + "-1").getCache(sCacheName);

        cache.clear();

        // create an empty snapshot
        createSnapshot("empty");
        ensureServiceStatusIdle();
        
        // assert the snapshot exists
        Eventually.assertThat(invoking(this).assertSnapshotExists("empty", SNAPSHOTS),is(true));

        // add some data
        cache.put("key-1", "value-1");
        cache.put("key-2", "value-2");
        assertThat(cache.size(), is(2));

        // create a second snapshot
        createSnapshot("2-entries");
        ensureServiceStatusIdle();
        Eventually.assertThat(invoking(this).assertSnapshotExists("2-entries", SNAPSHOTS),is(true));

        // archive the snapshot
        Response response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(ARCHIVES).path("2-entries")
                 .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        ensureServiceStatusIdle();

        Eventually.assertThat(invoking(this).assertSnapshotExists("2-entries", ARCHIVES),is(true));

        // remove the local snapshot
        deleteSnapshot("2-entries");
        ensureServiceStatusIdle();
        Eventually.assertThat(invoking(this).assertSnapshotExists("2-entries", SNAPSHOTS),is(false));

        // retrieve the archived snapshot
        response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(ARCHIVES).path("2-entries").path("retrieve")
                .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        ensureServiceStatusIdle();

        // check the existence of the local snapshot but delay as a single member could have the snapshot but it not be complete
        Eventually.assertThat(invoking(this).assertSnapshotExists("2-entries", SNAPSHOTS),is(true));

        // delete the archived snapshot
        response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(ARCHIVES).path("2-entries")
                .request().delete();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        ensureServiceStatusIdle();

        Eventually.assertThat(invoking(this).assertSnapshotExists("2-entries", ARCHIVES),is(false));

        // now we have local snapshot, clear the cache and then recover the snapshot
        cache.clear();
        assertThat(cache.size(), is(0));

        response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(SNAPSHOTS).path("2-entries").path("recover")
                .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        ensureServiceStatusIdle();

        Eventually.assertThat(invoking(this).assertCacheSize(cache, 2),is(false));

        // now delete the 2 snapshots

        deleteSnapshot("2-entries");
        ensureServiceStatusIdle();
        Eventually.assertThat(invoking(this).assertSnapshotExists("2-entries", SNAPSHOTS),is(false));

        deleteSnapshot("empty");
        ensureServiceStatusIdle();
        Eventually.assertThat(invoking(this).assertSnapshotExists("empty", SNAPSHOTS),is(false));

        cache.destroy();
        }

    /**
     * Test to validate fix for bug {@code 30914372}.
     * For each compound {@code rel}s (a rel containing a / character), ensure
     * the resulting URL doesn't have the '/' encoded.
     */
    @Test
    public void test30914372()
        {
        Response response = getBaseTarget().path(MEMBERS).path("1").request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

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
        Response response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).queryParam("fields", "operationStatus")
                 .request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
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
        Response response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(sType).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
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
        Response response = getBaseTarget().path(REPORTERS).path(sMember).request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

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

        String sMembersUrl = getLink(mapResponse, "members");
        response = m_client.target(sMembersUrl).queryParam("tier", "back").request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        LinkedHashMap mapCacheMembers = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
        assertThat(mapCacheMembers, notNullValue());

        List<LinkedHashMap> listCacheMembers = (List<LinkedHashMap>) mapCacheMembers.get("items");
        assertThat(listCacheMembers, notNullValue());

        for(LinkedHashMap mapCacheMember : listCacheMembers)
            {
            assertThat(mapCacheMember.get("tier"), is("back"));
            assertThat(mapCacheMember.get("name"), is(CACHE_NAME));
            assertThat(mapCacheMember.get("size"), instanceOf(Number.class));
            assertThat(mapCacheMember.get("cacheHits"), instanceOf(Number.class));

            String sSelfUrl = getSelfLink(mapCacheMember);

            assertThat(sSelfUrl, isOneOf(sMembersUrl+ "/" + mapCacheMember.get("nodeId"),
                    sMembersUrl+ "/" + mapCacheMember.get("member")));

            response = m_client.target(sSelfUrl).request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));

            List<LinkedHashMap> listCacheMembersOfANode = (List<LinkedHashMap>) mapResponse.get("items");
            assertThat(listCacheMembersOfANode, notNullValue());

            Set<Object> setCacheNames = listCacheMembersOfANode.stream()
                    .map(map -> map.get("name")).collect(Collectors.toSet());
            assertThat(setCacheNames.size(), is(1));
            assertThat(setCacheNames.iterator().next(), is(CACHE_NAME));

            assertThat(mapCacheMember.get("listenerFilterCount"), instanceOf(Number.class));
            assertThat(mapCacheMember.get("listenerKeyCount"), instanceOf(Number.class));
            }
        }

    private void testCachesResponse(WebTarget target, Response response)
        {
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(readEntity(target, response));
        assertThat(mapResponse, notNullValue());
        List<LinkedHashMap> listCacheMaps = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());
        assertThat(listCacheMaps.size(), greaterThan(1));

        for (LinkedHashMap mapCache : listCacheMaps)
            {
            assertThat(mapCache.get("name"), isOneOf(CACHES_LIST));
            assertThat(Integer.valueOf((Integer) mapCache.get("size")), greaterThan(0));
            }

        Response cachesResponse = getBaseTarget().path("caches").queryParam("fields", "totalPuts")
                .request().get();
        mapResponse = new LinkedHashMap(cachesResponse.readEntity(LinkedHashMap.class));
        listCacheMaps = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());
        assertThat(listCacheMaps.size(), greaterThan(1));

        for (LinkedHashMap mapCache : listCacheMaps)
            {
            assertThat(mapCache.get("name"), isOneOf(CACHES_LIST));
            assertThat((Integer) mapCache.get("totalPuts"), greaterThan(0));
            }

        cachesResponse = getBaseTarget().path("caches").queryParam("fields", "units")
                .request().get();
        mapResponse = new LinkedHashMap(cachesResponse.readEntity(LinkedHashMap.class));
        listCacheMaps = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());

        for (LinkedHashMap mapCache : listCacheMaps)
            {
            assertThat(mapCache.get("name"), isOneOf(CACHES_LIST));
            if (mapCache.get("name").equals("dist-foo"))
                {
                assertThat(mapCache.get("units"), anyOf(is(Integer.valueOf(0)), is(Integer.valueOf(20))));
                }
            else
                {
                assertThat(mapCache.get("units"), is(1));
                }
            }

        cachesResponse = getBaseTarget().path("caches").queryParam("fields", "insertCount")
                .request().get();
        mapResponse = new LinkedHashMap(cachesResponse.readEntity(LinkedHashMap.class));
        listCacheMaps = (List<LinkedHashMap>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());

        for (LinkedHashMap mapCache : listCacheMaps)
            {
            assertThat(mapCache.get("name"), isOneOf(CACHES_LIST));
            assertThat((Integer) mapCache.get("insertCount"), greaterThan(0));
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

    public Object getAttributeValue(Client client, String sService, String sAttributeName)
        {
        WebTarget membersTarget = getBaseTarget(client).path("services").path(sService).path("members");
        Response response = membersTarget.path(SERVER_PREFIX + "-1").request().get();

        assertThat(response.getStatus(), CoreMatchers.is(Response.Status.OK.getStatusCode()));
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
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
        LinkedHashMap mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
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

        WebTarget target   = getBaseTarget().path("services").path("DistributedCache").queryParam("fields", "requestTotalCount");
        Response  response = target.request().get();
        mapResponse = new LinkedHashMap(readEntity(target, response));

        assertThat(((Number) mapResponse.get("requestTotalCount")).intValue(), greaterThanOrEqualTo(0));

        target = getBaseTarget().path("services").path("DistributedCache").queryParam("fields", "partitionsAll")
                .queryParam("role", "*");
        response = target.request().get();
        mapResponse = new LinkedHashMap(readEntity(target, response));

        Collection<Integer> colPartCount = (Collection) mapResponse.get("partitionsAll");
        assertEquals(1, colPartCount.size());
        assertThat(colPartCount, Matchers.hasItem(257));

        target = getBaseTarget().path("services").path("DistributedCache").queryParam("fields", "statusHA")
                .queryParam("role", "*");
        response = target.request().get();
        mapResponse = new LinkedHashMap(readEntity(target, response));

        assertThat((Collection<String>) mapResponse.get("statusHA"), Matchers.hasItem("NODE-SAFE"));

        target = getBaseTarget().path("services").path("DistributedCache").queryParam("fields", "taskCount")
                .queryParam("collector", "list")
                .queryParam("role", "*");
        response = target.request().get();
        mapResponse = new LinkedHashMap(readEntity(target, response));
        // test specifying a custom collector
        assertThat(((Collection) mapResponse.get("taskCount")).size(), greaterThan(1));


        target = getBaseTarget().path("services").path("DistributedCache").queryParam("fields", "taskCount")
                .queryParam("collector", "list")
                .queryParam("role", SERVER_PREFIX + "-1");
        response = target.request().get();
        mapResponse = new LinkedHashMap(readEntity(target, response));

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

        Response response = getBaseTarget().path("services").queryParam("fields", "statusHA")
                .request().get();
        mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
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

        response = getBaseTarget().path("services").queryParam("fields", "taskCount")
                .queryParam("collector", "list")
                .queryParam("role", "*")
                .request().get();
        mapResponse = new LinkedHashMap(response.readEntity(LinkedHashMap.class));
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
        return readEntity(target, response, null);
        }

    protected LinkedHashMap readEntity(WebTarget target, Response response, Entity entity)
            throws ProcessingException
        {
        ProcessingException pe;
        
        try
            {
            return response.readEntity(LinkedHashMap.class);
            }
        catch (ProcessingException e)
            {
            // try again
            if (e.getCause() instanceof SocketException)
                {
                if (entity == null)
                    {
                    response = target.request().get();
                    }
                else
                    {
                    response = target.request().post(entity);
                    }
                assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

                return response.readEntity(LinkedHashMap.class);
                }

            pe = e;
            }

        throw pe;
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
     * The list of services used by this test class.
     */
    private String[] SERVICES_LIST = {SERVICE_NAME, "ExtendHttpProxyService", "ExtendProxyService",
            "DistributedCachePersistence", "ManagementHttpProxy"};

    /**
     * The list of caches used by this test class.
     */
    private String[] CACHES_LIST = {CACHE_NAME, "near-test", "dist-foo"};

    /**
     * A folder to store temporary files
     */
    @ClassRule
    public static TemporaryFolder s_tempFolder = new TemporaryFolder();

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
     * The number of attributes to check for.
     */
    protected static final int ATTRIBUTES_COUNT = 20;
    }
