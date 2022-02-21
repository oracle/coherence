/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.features.JmxFeature;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.MavenProjectFileUtils;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.GensonBuilder;
import com.tangosol.coherence.component.util.SafeService;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.coherence.management.internal.MapProvider;

import com.tangosol.discovery.NSLookup;

import com.tangosol.internal.management.resources.AbstractManagementResource;
import com.tangosol.internal.management.resources.ClusterMemberResource;
import com.tangosol.internal.net.management.HttpHelper;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;

import common.AbstractTestInfrastructure;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TestName;

import test.CheckJDK;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.TimeUnit;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import java.util.stream.Collectors;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static com.tangosol.internal.management.resources.AbstractManagementResource.CACHES;
import static com.tangosol.internal.management.resources.AbstractManagementResource.MANAGEMENT;
import static com.tangosol.internal.management.resources.AbstractManagementResource.MEMBER;
import static com.tangosol.internal.management.resources.AbstractManagementResource.MEMBERS;
import static com.tangosol.internal.management.resources.AbstractManagementResource.METADATA_CATALOG;
import static com.tangosol.internal.management.resources.AbstractManagementResource.NAME;
import static com.tangosol.internal.management.resources.AbstractManagementResource.NODE_ID;
import static com.tangosol.internal.management.resources.AbstractManagementResource.OPTIONS;
import static com.tangosol.internal.management.resources.AbstractManagementResource.REPORTERS;
import static com.tangosol.internal.management.resources.AbstractManagementResource.ROLE_NAME;
import static com.tangosol.internal.management.resources.AbstractManagementResource.SERVICE;
import static com.tangosol.internal.management.resources.ClusterMemberResource.DIAGNOSTIC_CMD;
import static com.tangosol.internal.management.resources.ClusterResource.DUMP_CLUSTER_HEAP;
import static com.tangosol.internal.management.resources.ClusterResource.ROLE;
import static com.tangosol.internal.management.resources.ClusterResource.TRACING_RATIO;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Base class for testing the ManagementInfoResource.
 * <p>
 * In general, if we only want to assert that an attribute value is set
 * (not the default -1), but not what the value is, then use asserts
 * similar to the following:
 *
 * assertThat(((Number) mapResponse.get("requestTotalCount")).intValue(), greaterThanOrEqualTo(0));
 * assertThat(Long.parseLong(mapResponse.get("requestTotalCount").toString()), greaterThanOrEqualTo(0L));
 *
 * @author hr 2016.07.21
 * @author sr 2017.08.24
 * @author jk 2022.01.25
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class BaseManagementInfoResourceTests
    {
    // ----- constructors ---------------------------------------------------

    public BaseManagementInfoResourceTests()
        {
        this(CLUSTER_NAME,BaseManagementInfoResourceTests::invokeInCluster);
        }

    public BaseManagementInfoResourceTests(String sClusterName, BiConsumer<String, RemoteCallable<Void>> inClusterInvoker)
        {
        f_sClusterName     = sClusterName;
        f_inClusterInvoker = inClusterInvoker;
        }

    // ----- junit lifecycle methods ----------------------------------------

    @AfterClass
    public static void tearDown()
        {
        if (m_client != null)
            {
            m_client.close();
            }

        if (s_cluster != null)
            {
            s_cluster.close();
            }

        FileHelper.deleteDirSilent(m_dirActive);
        FileHelper.deleteDirSilent(m_dirArchive);
        FileHelper.deleteDirSilent(m_dirSnapshot);
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
                      System.err.println(sMsg);
                      System.err.flush();
                      return null;
                      }).join();
                }
            }
        }

    // ----- tests ----------------------------------------------------------

    @Test
    public void testSwagger()
        {
        WebTarget target   = getBaseTarget().path(METADATA_CATALOG);
        Response  response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        Map mapResponse = readEntity(target, response);

        // cherry-pick a few items that must exist
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
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("clusterName"), is(f_sClusterName));
        assertThat(mapResponse.get("running"), is(true));
        assertThat(((Number) mapResponse.get("membersDepartureCount")).intValue(), is(0));

        Object objListMemberIds = mapResponse.get("memberIds");
        assertThat(objListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) objListMemberIds;
        assertThat((long) listMemberIds.size(), is(s_cluster.count()));

        Object objListLinks = mapResponse.get("links");
        assertThat(objListLinks, instanceOf(List.class));

        List<Map> listLinks = (List) objListLinks;

        Set<Object> linkNames = listLinks.stream().map(m -> m.get("rel")).collect(Collectors.toSet());
        assertThat(linkNames, hasItem("self"));
        assertThat(linkNames, hasItem("canonical"));
        assertThat(linkNames, hasItem("parent"));
        assertThat(linkNames, hasItem(MEMBERS));
        assertThat(linkNames, hasItem(SERVICES));
        assertThat(linkNames, hasItem(CACHES));
        }

    @Test
    public void testClusterMemberPlatformMemory()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        Object oListMemberIds = mapResponse.get("memberIds");
        assertThat(oListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) oListMemberIds;

        for (Object memberId : listMemberIds)
            {
            target = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path("memory");
            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = readEntity(target, response);
            Map mapHeapUsage = (Map) mapResponse.get("heapMemoryUsage");
            assertThat(mapHeapUsage, notNullValue());
            assertThat(((Number)mapHeapUsage.get("used")).intValue(), greaterThan(1));
            }
        }

    @Test
    public void testClusterMemberPlatformMemoryPoolTypeAttribute()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        Object oListMemberIds = mapResponse.get("memberIds");
        assertThat(oListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) oListMemberIds;

        String   GC_PREFIX      = s_bTestJdk11 ? "g1" : "ps";
        String[] arr_sMbeanName = { GC_PREFIX + "OldGen", GC_PREFIX + "SurvivorSpace"};

        for (String mbean : arr_sMbeanName)
            {
            for (Object memberId : listMemberIds)
                {
                target = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path(mbean);
                response = target.request().get();
                assertThat("unexpected response for Mgmt over REST API " 
                                   + getBaseTarget().getUri().toString() + "/" + MEMBERS + "/" + memberId 
                                   + "/platform/" + mbean, response.getStatus(), is(Response.Status.OK.getStatusCode()));
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
    public void testPlatformMbeans()
        {
        for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_URL_TO_MBEAN_QUERY.keySet())
            {
            WebTarget target = getBaseTarget().path("platform").path(platformMBean);
            Logger.info("Executing Management over REST request to URL: " + target.getUri().toString());

            Response response = target.request().get();
            assertThat(target.getUri().toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
            Map mapResponse = readEntity(target, response);
            assertThat(mapResponse.size(), greaterThan(0));
            }
        }

    @Test
    public void testPlatformG1MemoryMbeans()
        {
        Assume.assumeThat("Skipping test, G1 GC is not enabled", isG1(), is(true));
        for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_G1_URL_TO_MBEAN_QUERY.keySet())
            {
            WebTarget target = getBaseTarget().path("platform").path(platformMBean);
            Logger.info("Executing Management over REST request to URL: " + target.getUri().toString());

            Response response = target.request().get();
            assertThat(target.getUri().toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
            Map mapResponse = readEntity(target, response);
            assertThat(mapResponse.size(), greaterThan(0));
            }
        }

    @Test
    public void testPlatformCMSMemoryMbeans()
        {
        Assume.assumeThat("Skipping test, CMS GC is not enabled", isG1(), is(false));
        for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_PS_URL_TO_MBEAN_QUERY.keySet())
            {
            WebTarget target = getBaseTarget().path("platform").path(platformMBean);
            Logger.info("Executing Management over REST request to URL: " + target.getUri().toString());

            Response response = target.request().get();
            assertThat(target.getUri().toString(), response.getStatus(), is(Response.Status.OK.getStatusCode()));
            Map mapResponse = readEntity(target, response);
            assertThat(mapResponse.size(), greaterThan(0));
            }
        }

    @Test
    public void testMemberPlatformMbeans()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        Object objListMemberIds = mapResponse.get("memberIds");
        assertThat(objListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) objListMemberIds;
        Object memberId = listMemberIds.get(0);

        for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_URL_TO_MBEAN_QUERY.keySet())
            {
            target = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path(platformMBean);

            Logger.info(target.getUri().toString());

            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = readEntity(target, response);
            assertThat(mapResponse.size(), greaterThan(0));
            }
        }

    @Test
    public void testMemberPlatformG1MemoryMbeans()
        {
        Assume.assumeThat("Skipping test, G1 GC is not enabled", isG1(), is(true));

        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        Object objListMemberIds = mapResponse.get("memberIds");
        assertThat(objListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) objListMemberIds;
        Object memberId = listMemberIds.get(0);

        for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_G1_URL_TO_MBEAN_QUERY.keySet())
            {
            target = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path(platformMBean);

            Logger.info(target.getUri().toString());

            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = readEntity(target, response);
            assertThat(mapResponse.size(), greaterThan(0));
            }
        }

    @Test
    public void testMemberPlatformCMSMemoryMbeans()
        {
        Assume.assumeThat("Skipping test, CMS GC is not enabled", isG1(), is(false));

        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        Object objListMemberIds = mapResponse.get("memberIds");
        assertThat(objListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) objListMemberIds;
        Object memberId = listMemberIds.get(0);

        for (String platformMBean : AbstractManagementResource.MAP_PLATFORM_PS_URL_TO_MBEAN_QUERY.keySet())
            {
            target = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path(platformMBean);

            Logger.info(target.getUri().toString());

            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = readEntity(target, response);
            assertThat(mapResponse.size(), greaterThan(0));
            }
        }

    @Test
    public void testNetworkStats()
        {
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

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
        Map mapResponse = readEntity(target, response);

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
        Map mapResponse = readEntity(target, response);

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
        Map mapResponse = readEntity(target, response);

        Object oListLinks = mapResponse.get("links");
        assertThat(oListLinks, instanceOf(List.class));

        List<Map> listLinks = (List) oListLinks;

        Map mapJmxManagement = listLinks.stream().filter(m -> m.get("rel").equals(MANAGEMENT))
                .findFirst() .orElse(new LinkedHashMap());
        String sJmxURl = (String) mapJmxManagement.get("href");

        target = m_client.target(sJmxURl);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        mapResponse = readEntity(target, response);
        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("refreshOnQuery"), is(Boolean.FALSE));
        assertThat(((Number) mapResponse.get("expiryDelay")).longValue(), is(1000L));
        assertThat(mapResponse.get("refreshPolicy"), is("refresh-ahead"));
        }

    @Test
    public void testUpdateJmxManagementError()
        {
        Map mapEntity = new LinkedHashMap();
        mapEntity.put("refreshPolicy", "nonExistent");
        WebTarget target   = getBaseTarget().path(MANAGEMENT);
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Map mapResponse = readEntity(target, response, entity);

        List<Map> listMessages = (List) mapResponse.get("messages");
        assertThat(listMessages, notNullValue());
        assertThat(listMessages.size(), is(1));

        Map mapMessage = listMessages.get(0);
        assertThat(mapMessage.get("field"), is("refreshPolicy"));
        }

    @Test
    public void testUpdateJmxManagement()
        {
        Map mapEntity = new LinkedHashMap();
        mapEntity.put("expiryDelay", 2000L);
        mapEntity.put("refreshPolicy", "refresh-behind");
        WebTarget target   = getBaseTarget().path(MANAGEMENT);
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request(MediaType.APPLICATION_JSON_TYPE).post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Map mapResonse = readEntity(target, response, entity);

        List<Map> listMessages = (List) mapResonse.get("messages");
        assertThat(listMessages, nullValue());

        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        mapResonse = readEntity(target, response);
        assertThat(((Number) mapResonse.get("expiryDelay")).longValue(), is(2000L));
        assertThat(mapResonse.get("refreshPolicy"), is("refresh-behind"));
        }

    @Test
    public void testInvalidExpiryDelayValueFailure()
        {
        Map mapEntity = new LinkedHashMap();

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
        Response response = getBaseTarget().path(DUMP_CLUSTER_HEAP).request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testHeapDumpWithRole()
        {
        Map mapEntity = new LinkedHashMap();
        mapEntity.put(ROLE, "storage");
        Response response = getBaseTarget().path("dumpClusterHeap").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testLogClusterState()
        {
        Response response = getBaseTarget().path("logClusterState").request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testConfigureTracing()
        {
        Map mapEntity = new LinkedHashMap();
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
        Map mapEntity = readEntity(target, response);

        String sJmxURl = getLink(mapEntity, "members");

        target = m_client.target(sJmxURl);
        response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Map mapResponse = readEntity(target, response);
        assertThat(mapEntity, notNullValue());

        List<Map> listMemberMaps = (List<Map>) mapResponse.get("items");

        assertThat(listMemberMaps, notNullValue());

        assertThat(listMemberMaps.size(), is((int) s_cluster.count()));

        for (Map mapMember : listMemberMaps)
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
        WebTarget target   = getBaseTarget();
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        List<Number> listMemberIds = (List<Number>) mapResponse.get("memberIds");

        assertThat(listMemberIds, notNullValue());
        assertThat(listMemberIds.size(), greaterThan(0));

        Number nMemberId = listMemberIds.get(0);

        response = getBaseTarget().path("members").path(String.valueOf(nMemberId))
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
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        List<Number> listMemberIds = (List<Number>) mapResponse.get("memberIds");

        assertThat(listMemberIds, notNullValue());
        assertThat(listMemberIds.size(), greaterThan(0));

        Number nMemberId = listMemberIds.get(0);

        response = getBaseTarget().path("members").path(String.valueOf(nMemberId))
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
        Map mapResponse = readEntity(target, response);

        List<Number> listMemberIds = (List<Number>) mapResponse.get("memberIds");

        assertThat(listMemberIds, notNullValue());
        assertThat(listMemberIds.size(), greaterThan(0));

        Number nMemberId = listMemberIds.get(0);

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
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        String result = response.readEntity(String.class);
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

        assertThat(new File(folder.getAbsolutePath() + File.separator + "1-all1.jfr").exists(), is(true));
        assertThat(new File(folder.getAbsolutePath() + File.separator + "2-all1.jfr").exists(), is(true));
        assertThat(new File(sJfrPath + File.separator + "1-all.jfr").exists(), is(true));
        assertThat(new File(sJfrPath + File.separator + "2-all.jfr").exists(), is(true));
        }

    @Test
    public void testJfrWithRole()
            throws Exception
        {
        // This test requires Flight Recorder and only runs on Oracle JVMs
        CheckJDK.assumeOracleJDK();

        File   jfrDir    = MavenProjectFileUtils.ensureTestOutputFolder(getClass(), "jfr");
        String sFilePath = jfrDir.getAbsolutePath() + File.separator;
        String sFileName = sFilePath + "testMemberJfr-myRecording.jfr";

        Response response = getBaseTarget().path(DIAGNOSTIC_CMD)
                .path("jfrStart")
                .queryParam(OPTIONS, encodeValue("name=myJfr,duration=3s,filename="+ sFileName))
                .queryParam(ROLE_NAME, SERVER_PREFIX + "-1")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        String result = response.readEntity(String.class);

        File testFile1 = new File(sFilePath +"1-testMemberJfr-myRecording.jfr");
        assertThat(testFile1.exists(), is(true));
        assertThat(testFile1.delete(), is(true));

        assertThat(result.indexOf(SERVER_PREFIX + "-2"), is(-1));
        File testFile2 = new File(sFilePath +"2-testMemberJfr-myRecording.jfr");
        assertThat(testFile2.exists(), is(false));
        }

    @Test
    public void testMemberJfr()
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
        Map mapResponse = new LinkedHashMap(response.readEntity(Map.class));

        List<Number> listMemberIds = (List<Number>) mapResponse.get("memberIds");

        assertThat(listMemberIds, notNullValue());
        assertThat(listMemberIds.size(), greaterThan(0));

        Number nMemberId = listMemberIds.get(0);
        if (!s_bTestJdk11)
            {
            // below JDK 11
            response = getBaseTarget().path("members").path(nMemberId + "")
                    .path(DIAGNOSTIC_CMD)
                    .path("vmUnlockCommercialFeatures")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            }

        try
            {
            File   jfrDir    = MavenProjectFileUtils.ensureTestOutputFolder(getClass(), "jfr");
            String sFilePath = jfrDir.getAbsolutePath() + File.separator;
            String sFileName = sFilePath + "testMemberJfr-myRecording.jfr";
            response = getBaseTarget().path("members").path(nMemberId + "")
                    .path(DIAGNOSTIC_CMD)
                    .path("jfrStart")
                    .queryParam(OPTIONS, encodeValue("name=myJfr,duration=5s,filename="+ sFileName)).request(MediaType.APPLICATION_JSON_TYPE)
                    .post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            File testFile = new File(sFileName);
            assertThat(testFile.exists(), is(true));
            assertThat(testFile.delete(), is(true));

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
            Logger.err("testMemberJfr() failed with exception: ");
            Logger.err(e);
            }
            assertThat(jfr1.delete(), is(true));
            assertThat(jfr2.delete(), is(true));
        }


    @Test
    public void testJmxJfr()
            throws Exception
        {
        // This test requires Flight Recorder and only runs on Oracle JVMs
        CheckJDK.assumeOracleJDK();

        MBeanServerConnection mBeanServer;
        ObjectName oName;

        List<CoherenceClusterMember> listMember = s_cluster.stream().collect(Collectors.toList());
        CoherenceClusterMember       memberOne  = listMember.get(0);
        CoherenceClusterMember       memberTwo  = listMember.get(1);
        try
            {
            String sName = "Coherence:type=DiagnosticCommand,Domain=com.sun.management,subType=DiagnosticCommand,cluster=mgmtRestCluster,member=" + memberOne.getName() + ",nodeId=" + memberOne.getLocalMemberId();

            oName = new ObjectName(sName);
            mBeanServer = memberOne.get(JmxFeature.class).getDeferredJMXConnector().get().getMBeanServerConnection();
            if (!s_bTestJdk11)
                {
                mBeanServer.invoke(oName, "vmUnlockCommercialFeatures", null, null);
                }
            }
        catch (Exception InstanceNotFoundException)
            {
            try
                {
                String sName = "Coherence:type=DiagnosticCommand,Domain=com.sun.management,subType=DiagnosticCommand,cluster=mgmtRestCluster,member=" + memberTwo.getName() + ",nodeId=" + memberTwo.getLocalMemberId();

                mBeanServer = memberTwo.get(JmxFeature.class).getDeferredJMXConnector().get().getMBeanServerConnection();
                oName = new ObjectName(sName);
                if (!s_bTestJdk11)
                    {
                    mBeanServer.invoke(oName, "vmUnlockCommercialFeatures", null, null);
                    }
                }
            catch (Exception e1)
                {
                Logger.err("warning: failed to connect to a mbean server.");
                Logger.err(e1);
                throw e1;
                }
            }

        File jfrDir      = MavenProjectFileUtils.ensureTestOutputFolder(getClass(), "jfr");
        String   sFilePath   = jfrDir.getAbsolutePath() + File.separator;
        String   sFileName   = sFilePath + "testJmxJfr-myRecording.jfr";
        Object[] aoArguments = new Object[]{new String[]{"name=foo", "duration=5s", "filename="+ sFileName}};

        mBeanServer.invoke(oName, "jfrStart", aoArguments, new String[]{String[].class.getName()});
        File testFile = new File(sFileName);
        assertThat(testFile.exists(), is(true));
        assertThat(testFile.delete(), is(true));
        }

    @Test
    public void testServiceInfo()
        {
        WebTarget target   = getBaseTarget().path(SERVICES);
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());

        List<Map> listItemMaps = (List<Map>) mapResponse.get("items");
        assertThat(listItemMaps, notNullValue());
        assertThat(listItemMaps.size(), is(EXPECTED_SERVICE_COUNT)); // <---- This is SO brittle!!!

        String        sDistServiceName  = null;
        String        sProxyServiceName = null;
        String        sMoRESTProxy      = HttpHelper.getServiceName();
        Map mapDistScheme     = null;
        Map mapProxyScheme    = null;
        for (Map listItemMap : listItemMaps)
            {
            if (((String) listItemMap.get(NAME)).compareToIgnoreCase(SERVICE_NAME) == 0)
                {
                sDistServiceName = SERVICE_NAME;
                mapDistScheme = listItemMap;
                }
            else if (((String) listItemMap.get(NAME)).compareToIgnoreCase(sMoRESTProxy) == 0)
                {
                sProxyServiceName = sMoRESTProxy;
                mapProxyScheme = listItemMap;
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

        Map mapService = readEntity(target, response);
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
        assertThat(((List) mapService.get("quorumStatus")).get(0), is("Not configured"));

        target = getBaseTarget().path(SERVICES).path(sName)
                .queryParam("fields", "storageEnabled");
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        mapResponse = readEntity(target, response);
        assertThat(((Number) ((Map) mapResponse.get("storageEnabled")).get("false")).longValue(), is(1L));
        }

    @Test
    public void testServiceMembers()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("members");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());

        List<Map> items = (List<Map>) mapResponse.get("items");
        assertThat(items, notNullValue());
        assertThat(items.size(), is(2));

        for(Map mapEntry : items)
            {
            assertThat(mapEntry.get(NAME), is(SERVICE_NAME));
            assertThat(mapEntry.get("type"), is(SERVICE_TYPE));
            assertThat(Integer.parseInt((String) mapEntry.get(NODE_ID)), greaterThan(0));
            assertThat(((Number) mapEntry.get("backupCount")).longValue(), is(1L));
            assertThat(mapEntry.get("joinTime"), notNullValue());
            assertThat(mapEntry.get("links"), notNullValue());

            target = m_client.target(getSelfLink(mapEntry));
            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            Map mapMemberResponse = readEntity(target, response);

            assertThat(mapEntry.get(NODE_ID), is(mapMemberResponse.get(NODE_ID)));
            assertThat(mapEntry.get("joinTime"), is(mapMemberResponse.get("joinTime")));
            }
        }

    @Test
    public void testManagementRequestWithAcceptEncodingGzip()
        {
        Response response = getBaseTarget().path(SERVICES)
                .path("DistributedCache")
                .path("members")
                .request()
                .header("Accept-Encoding", "gzip")
                .get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        assertThat(response.getHeaderString("Content-Encoding"), is("gzip"));
        }

    @Test
    public void testManagementInfoRefreshTimeIsPresent()
        {
        WebTarget target   = getBaseTarget().path("management");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        Map mapResponse = readEntity(target, response);
        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("refreshTime"), is(notNullValue()));
        }

    @Test
    public void testPartitionInfo()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("partition");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("strategyName"), is("SimpleAssignmentStrategy"));
        assertThat(((Number) mapResponse.get("partitionCount")).longValue(), is(257L));
        assertThat(((Number) mapResponse.get("backupCount")).longValue(), is(1L));

        target = m_client.target(getSelfLink(mapResponse));
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        Map mapPartitionResponse = readEntity(target, response);

        // this timestamp could differ so just remove
        mapPartitionResponse.remove("lastAnalysisTime");
        mapResponse.remove("lastAnalysisTime");
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
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get(NAME), is(SERVICE_NAME));
        assertThat(mapResponse.get("type"), is(SERVICE_TYPE));

        assertThat(((Number) mapResponse.get("backupCount")).intValue(), is(1));
        assertThat(mapResponse.get("joinTime"), notNullValue());
        assertThat(mapResponse.get("links"), notNullValue());

        String sSelfUrl = getLink(mapResponse, "parent");
        assertThat(sSelfUrl, is(membersTarget.getUri().toString()));
        }

    @Test
    public void testDirectServiceMemberUpdate()
        {
        Map mapEntity = new LinkedHashMap();
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

        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(((Number) mapResponse.get("threadCountMin")).longValue(), is(5L));
        assertThat(((Number) mapResponse.get("taskHungThresholdMillis")).longValue(), is(10L));
        assertThat(((Number) mapResponse.get("taskTimeoutMillis")).longValue(), is(100000L));
        assertThat(((Number) mapResponse.get("requestTimeoutMillis")).longValue(), is(200000L));
        }

    @Test
    public void testCacheMemberUpdate()
        {
        Map<String, Object> mapMethodValues = new HashMap<String, Object>()
            {{
               put("highUnits",   100005L);
               put("expiryDelay", 60000L);
            }};

        mapMethodValues.forEach((attribute, value) ->
            {
            System.err.println("Updating " + attribute + " to " + value);
            Map map = new LinkedHashMap();
            map.put(attribute, value);
            WebTarget target = getBaseTarget().path(SERVICES).path(SERVICE_NAME).path(CACHES).path(CACHE_NAME)
                    .path("members").path(SERVER_PREFIX + "-1");
            Response response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

            Map mapResponse = readEntity(target, response);
            List<Map> listItems = (List<Map>) mapResponse.get("items");
            assertThat(listItems, notNullValue());
            assertThat(listItems.size(), is(1));

            Map mapItem = listItems.get(0);

            assertThat(attribute + " should be " + value + ", but is " + mapItem.get(attribute),
                       ((Number) mapItem.get(attribute)).longValue(), is(value));
            });
        }

    @Test
    public void testClusterMemberUpdate()
        {
        Map<String, Object> mapMethodValues = new HashMap<String, Object>()
            {{
               put("loggingLevel"            ,9L);
               put("resendDelay"             ,100L);
               put("sendAckDelay"            ,17L);
               put("trafficJamCount"         ,2048L);
               put("trafficJamDelay"         ,12L);
               put("loggingLimit"            ,2147483640L);
               put("loggingFormat"           ,"{date}/{uptime} {product} {version} <{level}> (thread={thread}, member={member}):- {text}");
            }};
        mapMethodValues.forEach((attribute, value) ->
            {
            System.err.println("Updating " + attribute + " to " + value);

            Map map = new LinkedHashMap();
            map.put(attribute, value);
            WebTarget target = getBaseTarget().path("members").path(SERVER_PREFIX + "-1");
            Response response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            Map mapResponse = readEntity(target, response);
            assertThat(mapResponse, notNullValue());
            Object o = mapResponse.get(attribute);
            if (value instanceof Number)
                {
                assertThat(o, is(instanceOf(Number.class)));
                Number n = (Number) o;
                assertThat(attribute + " should be " + value + ", but is " + n, n.intValue(), is(((Number) value).intValue()));
                }
            else
                {
                assertThat(attribute + " should be " + value + ", but is " + o, o, is(value));
                }
            });
        }

    @Test
    public void testUpdateReporterMember()
        {
        Map map     = new LinkedHashMap();
        String  sMember = SERVER_PREFIX + "-1";

        map.put("intervalSeconds", 15L);
        WebTarget target   = getBaseTarget().path(REPORTERS).path(sMember);
        Response  response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        assertNoMessages(response);

        Eventually.assertDeferred(() -> assertReporterAttribute(sMember, "intervalSeconds", 15),is(true));

        map.clear();
        Eventually.assertDeferred(() -> assertReporterAttribute(sMember, "currentBatch", 0),is(true));

        map.put("currentBatch", 25);
        response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        assertNoMessages(response);
        Eventually.assertDeferred(() -> assertReporterAttribute(sMember, "currentBatch", 25),is(true));
        }

    @Test
    public void testClusterMemberUpdateFailure()
        {
        Map map = new LinkedHashMap();
        map.put("cpuCount", 9L);
        WebTarget target   = getBaseTarget().path("members").path(SERVER_PREFIX + "-1");
        Entity    entity   = Entity.entity(map, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response, entity);

        List<Map> listMessages = (List) mapResponse.get("messages");
        assertThat(listMessages, notNullValue());
        assertThat(listMessages.size(), is(1));

        Map mapMessages = listMessages.get(0);
        assertThat(mapMessages.get("field"), is("cpuCount"));
        assertThat(mapMessages.get("severity"), is("FAILURE"));
        }

    @Test
    public void testCacheMemberUpdateFailure()
        {
        Map mapEntity = new LinkedHashMap();
        mapEntity.put("cacheHits", 100005L);
        WebTarget target  = getBaseTarget().path(SERVICES).path(SERVICE_NAME).path(CACHES).path(CACHE_NAME)
                .path("members").path(SERVER_PREFIX + "-1");
        Entity   entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response, entity);

        List<Map> listMessages = (List) mapResponse.get("messages");
        assertThat(listMessages, notNullValue());
        assertThat(listMessages.size(), is(1));

        Map mapMessages = listMessages.get(0);
        assertThat(mapMessages.get("field"), is("cacheHits"));
        }

    @Test
    public void testReporters()
        {
        WebTarget target   = getBaseTarget().path(REPORTERS);
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        List<Map> listItems = (List<Map>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(2));

        for (Map mapReports : listItems)
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
        Map mapResponse = readEntity(target, response);

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
        String sMember = SERVER_PREFIX + "-1";

        // create a temp directory so we don't pollute any directories
        File tempDirectory = FileHelper.createTempDir();

        try {
            setReporterAttribute(sMember, "outputPath", tempDirectory.getAbsolutePath());
            Eventually.assertDeferred(() -> assertReporterAttribute(sMember, "outputPath", tempDirectory.getAbsolutePath()),is(true));

            // set the intervalSeconds shorter so we don't want as long
            setReporterAttribute(sMember, "intervalSeconds", 15);
            Eventually.assertDeferred(() -> assertReporterAttribute(sMember, "intervalSeconds", 15),is(true));

            Eventually.assertDeferred(() -> assertReporterAttribute(sMember, "state", "Stopped"),is(true));
            Map mapEntity = new LinkedHashMap();

            // start the reporter
            Response response = getBaseTarget().path(REPORTERS).path(sMember).path("start").request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

            Eventually.assertDeferred(() -> assertReporterAttribute(sMember, "state", new String[] {"Sleeping", "Running"}),is(true));

            // stop the reporter
            response = getBaseTarget().path(REPORTERS).path(sMember).path("stop").request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            Eventually.assertDeferred(() -> assertReporterAttribute(sMember, "state", "Stopped"),is(true), within(2, TimeUnit.MINUTES));
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

        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(((Number) mapResponse.get("requestTotalCount")).intValue(), greaterThanOrEqualTo(0));
        }

    @Test
    public void testSuspendAndResume()
        {
        Response response = getBaseTarget().path(SERVICES).path(SERVICE_NAME).path("suspend")
                .request().post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        Eventually.assertDeferred(() -> getAttributeValue(m_client, SERVICE_NAME, "quorumStatus"),
                is("Suspended"));

        response = getBaseTarget().path(SERVICES).path("DistributedCache").path("resume")
                .request().post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Eventually.assertDeferred(() -> getAttributeValue(m_client, SERVICE_NAME, "quorumStatus").toString(),
                containsString("allowed-actions"));
        }

    @Test
    public void testService()
        {
        // aggregate all attributes for a service across all nodes
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache");
        Response  response = target.request().get();
        Map mapResponse = readEntity(target, response);

        testDistServiceInfo(mapResponse);
        }

    @Test
    public void testServices()
        {
        // aggregate all attributes for all services across all nodes
        WebTarget     target      = getBaseTarget().path(SERVICES);
        Response      response    = target.request().get();
        Map mapResponse = readEntity(target, response);

        testDistServicesInfo(mapResponse);
        }

    @Test
    public void testCache()
        {
        final String CACHE_NAME = "dist-foo";

        f_inClusterInvoker.accept(f_sClusterName, () ->
            {
            // fill a cache
            NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
            Binary     binValue = Binary.getRandomBinary(1024, 1024);
            cache.put(1, binValue);
            return null;
            });

        long[] acTmp = new long[1];
        long   cEntrySize;
        do
            {
            WebTarget     target      = getBaseTarget().path(CACHES).path(CACHE_NAME).queryParam("fields","units")
                                        .queryParam("role", "*");
            Response      response    = target.request().get();
            Map mapResponse = readEntity(target, response);

            System.out.println(mapResponse);

            cEntrySize = acTmp[0] = ((Number) mapResponse.get("units")).longValue();
            }
        while (sleep(() -> acTmp[0] <= 0L, REMOTE_MODEL_PAUSE_DURATION));

        f_inClusterInvoker.accept(f_sClusterName, () ->
            {
            // fill a cache
            NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
            Binary     binValue = Binary.getRandomBinary(1024, 1024);
            cache.put(1, binValue);
            cache.clear();
            for (int i = 0; i < 10; ++i)
                {
                cache.put(i, binValue);
                }
            return null;
            });

        Base.sleep(REMOTE_MODEL_PAUSE_DURATION);

        do
            {
            WebTarget     target      = getBaseTarget().path(CACHES).path(CACHE_NAME).queryParam("fields","size")
                                        .queryParam("role", "*");
            Response      response    = target.request().get();
            Map mapResponse = readEntity(target, response);
            acTmp[0] = ((Number) mapResponse.get("size")).longValue();
            }
        while (sleep(() -> acTmp[0] != 10L, REMOTE_MODEL_PAUSE_DURATION));

        WebTarget     target      = getBaseTarget().path(SERVICES).path(SERVICE_NAME)
                                    .path(CACHES).path(CACHE_NAME).queryParam("fields","size");
        Response      response    = target.request().get();
        Map mapResponse = readEntity(target, response);

        // aggregate all attributes for a cache on a service across all nodes
        assertThat(((Number) mapResponse.get("size")).longValue(), is(10L));

        target = getBaseTarget().path(SERVICES).path(SERVICE_NAME)
                .path(CACHES).path(CACHE_NAME).queryParam("fields","units")
                .queryParam("role","*");
        response = target.request().get();
        mapResponse = readEntity(target, response);

        // aggregate Units attribute for a cache across all nodes
        assertThat(((Number) mapResponse.get("units")).longValue(), is(cEntrySize * 10));

        target = getBaseTarget().path(SERVICES).path(SERVICE_NAME)
                .path(CACHES).path(CACHE_NAME).queryParam("fields","units")
                .queryParam("role","*")
                .queryParam("collector", "list");
        response = target.request().get();
        mapResponse = readEntity(target, response);

        // list the Units attribute for a cache across all nodes
        List<Number> listUnits = (List) mapResponse.get("units");
        assertEquals(2, listUnits.size());
        int cMinUnits = ((int) cEntrySize) * 4;
        listUnits.forEach(n -> assertThat(n.intValue(), greaterThanOrEqualTo(cMinUnits)));

        f_inClusterInvoker.accept(f_sClusterName, () ->
            {
            // fill a cache
            NamedCache cache = CacheFactory.getCache(CACHE_NAME);
            cache.clear();
            return null;
            });

        response = getBaseTarget().path(CACHES).path(CACHE_NAME).path("members").path(SERVER_PREFIX + "-1").path("resetStatistics")
                .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        response = getBaseTarget().path(CACHES).path(CACHE_NAME).path("members").path(SERVER_PREFIX + "-2").path("resetStatistics")
                .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Base.sleep(10000);

        int nSize = 20;
        f_inClusterInvoker.accept(f_sClusterName, () ->
            {
            // fill a cache
            NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
            Binary     binValue = Binary.getRandomBinary(1024, 1024);
            for (int i = 0; i < nSize; i++)
                {
                cache.put(i, binValue);
                }
            for (int i = 0; i < nSize; i++)
                {
                cache.get(i);
                }
            return null;
            });

        Base.sleep(REMOTE_MODEL_PAUSE_DURATION);

        target = getBaseTarget().path(CACHES).path(CACHE_NAME);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        mapResponse = readEntity(target, response);

        assertEquals(nSize, ((Number) mapResponse.get("size")).intValue());
        assertEquals(nSize, ((Number) mapResponse.get("totalPuts")).intValue());
        assertEquals(nSize, ((Number) mapResponse.get("totalGets")).intValue());
        }

    @Test
    public void testDirectServiceMemberWithIncludedFields()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path("DistributedCache").path("members")
                            .path(SERVER_PREFIX + "-1").queryParam("fields", "backupCount,joinTime");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.size(), is(3));
        assertThat(((Number) mapResponse.get("backupCount")).intValue(), is(1));
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
        Map mapResponse = readEntity(target, response);

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
        Map mapResponse = readEntity(target, response);

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
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse.get("links"), notNullValue());

        List<Map> listLinks = (List) mapResponse.get("links");

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
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse.get("links"), notNullValue());

        List<Map> listLinks = (List) mapResponse.get("links");

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
        Map mapResponse = readEntity(target, response);

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
        Map mapResponse = readEntity(target, response);

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
        Map mapResponse = readEntity(target, response);

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
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("scheduledDistributions"), notNullValue());
        }

    @Test
    public void testReportNodeState()
        {
        WebTarget target   = getBaseTarget().path("members").path(SERVER_PREFIX + "-1").path("state");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

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
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());

        List<Map> listItems = (List<Map>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(2));

        for(Map mapEntry : listItems)
            {
            assertThat(mapEntry.get(NAME), is("ExtendProxyService"));
            target = m_client.target(getLink(mapEntry, "proxy"));
            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            Map mapMemberResponse = readEntity(target, response);
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
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());

        List<Map> listItems = (List<Map>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(2));

        for(Map mapEntry : listItems)
            {
            assertThat(mapEntry.get(NAME), is("ExtendProxyService"));

            String sProxyUrl = getLink(mapEntry, "proxy");
            response = m_client.target(sProxyUrl).path("connections").request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            }
        }

    @Test
    public void testCaches()
        {
        final String CACHE_NAME = "dist-foo";

        f_inClusterInvoker.accept(f_sClusterName, () ->
            {
            // fill a cache
            NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
            Binary     binValue = Binary.getRandomBinary(1024, 1024);
            cache.put(1, binValue);
            return null;
            });

        // ensure async put has completed
        long[] acTmp = new long[1];
        do
            {
            WebTarget target      = getBaseTarget().path(CACHES).path(CACHE_NAME).queryParam("fields","units")
                                    .queryParam("role", "*");
            Response  response    = target.request().get();
            Map       mapResponse = readEntity(target, response);

            System.out.println(mapResponse);

            acTmp[0] = ((Number) mapResponse.get("units")).longValue();
            }
        while (sleep(() -> acTmp[0] <= 0L, REMOTE_MODEL_PAUSE_DURATION));

        long[] acTmp1 = new long[1];
        do
            {
            WebTarget target      = getBaseTarget().path(CACHES).path(CACHE_NAME).queryParam("fields","totalPuts")
                    .queryParam("role", "*");
            Response  response    = target.request().get();
            Map       mapResponse = readEntity(target, response);

            System.out.println(mapResponse);

            acTmp1[0] = ((Number) mapResponse.get("totalPuts")).longValue();
            }
        while (sleep(() -> acTmp1[0] <= 0L, REMOTE_MODEL_PAUSE_DURATION));

        WebTarget target   = getBaseTarget().path(CACHES);
        Response  response = target.request().get();
        testCachesResponse(target, response, CACHE_NAME);
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
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

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
        Map mapResponse = readEntity(target, response);

        String sMembersUrl = getLink(mapResponse, "members");
        target = m_client.target(sMembersUrl).queryParam("tier", "front");
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Map mapCacheMembers = readEntity(target, response);
        assertThat(mapCacheMembers, notNullValue());

        List<Map> listCacheMembers = (List<Map>) mapCacheMembers.get("items");
        assertThat(listCacheMembers, notNullValue());
        assertThat(listCacheMembers.size(), is(1));

        for(Map mapCacheMember : listCacheMembers)
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
        Map mapResponse = readEntity(target, response);

        String sMembersUrl = getLink(mapResponse, "members");
        target = m_client.target(sMembersUrl);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Map mapCacheMembers = readEntity(target, response);
        assertThat(mapCacheMembers, notNullValue());

        List<Map> listCacheMembers = (List<Map>) mapCacheMembers.get("items");

        assertThat(listCacheMembers.size(), is(3));
        assertThat(listCacheMembers, notNullValue());

        for(Map mapCacheMember : listCacheMembers)
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
        Map mapEntity = new LinkedHashMap();
        mapEntity.put("links", new String[]{});
        mapEntity.put("fields", new String[]{"clusterName","clusterSize"});

        WebTarget   target   = getBaseTarget().path("search");
        Entity<Map> entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response    response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response, entity);
        assertThat(mapResponse.size(), is(2));
        assertThat(mapResponse.get("clusterName"), is(notNullValue()));
        assertThat(((Number) mapResponse.get("clusterSize")).intValue(), greaterThan(1));
        }

    @Test
    public void testClusterSearchWithMembers()
        {
        Map mapEntity = new LinkedHashMap();
        mapEntity.put("links", new String[]{});
        mapEntity.put("fields", new String[]{"clusterName","clusterSize"});

        Map mapChildren = new LinkedHashMap();
        mapEntity.put("children", mapChildren);

        Map mapMembers = new LinkedHashMap();
        mapMembers.put("links", new String[]{});
        mapMembers.put("fields", new String[]{NODE_ID,"memberName"});
        mapChildren.put("members", mapMembers);

        WebTarget target   = getBaseTarget().path("search");
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response, entity);
        assertThat(mapResponse.size(), is(3));
        assertThat(mapResponse.get("clusterName"), is(notNullValue()));
        assertThat(((Number) mapResponse.get("clusterSize")).intValue(), greaterThan(1));

        Map mapMembersResponse = (Map) mapResponse.get("members");
        assertThat(mapMembersResponse, notNullValue());
        List<Map> listMembers = (List<Map>) mapMembersResponse.get("items");
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), is(greaterThan(1)));

        for (Map mapMember: listMembers)
            {
            assertThat(mapMember.size(), is(2));
            assertThat(mapMember.get(NODE_ID), notNullValue());
            assertThat(mapMember.get("memberName"), notNullValue());
            }
        }

    @Test
    public void testClusterSearchWithServices()
        {
        Map mapEntity = new LinkedHashMap();
        mapEntity.put("links", new String[]{});
        mapEntity.put("fields", new String[]{});

        Map mapChildren = new LinkedHashMap();
        mapEntity.put("children", mapChildren);

        Map mapMembers = new LinkedHashMap();
        mapMembers.put("links", new String[]{});
        mapMembers.put("fields", new String[]{NAME,"type"});
        mapChildren.put(SERVICES, mapMembers);

        WebTarget target   = getBaseTarget().path("search");
        Entity    entyty   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = getBaseTarget().path("search")
                .request().post(entyty);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = new LinkedHashMap(readEntity(target, response, entyty));
        assertThat(mapResponse.size(), is(1));

        Map membersResponseMap = (Map) mapResponse.get(SERVICES);
        assertThat(membersResponseMap, notNullValue());
        List<Map> listMembers = (List<Map>) membersResponseMap.get("items");
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), is(EXPECTED_SERVICE_COUNT)); // <---- This is SO brittle!!!

        for (Map mapMember: listMembers)
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
        Map mapResponse = readEntity(target, response, entity);
        assertThat(mapResponse.size(), is(1));

        Map membersResponseMap = (Map) mapResponse.get(SERVICES);
        assertThat(membersResponseMap, notNullValue());
        List<Map> listServices = (List<Map>) membersResponseMap.get("items");
        assertThat(listServices, notNullValue());
        assertThat(listServices.size(), is(EXPECTED_SERVICE_COUNT)); // <---- This is SO brittle!!!

        for (Map mapService: listServices)
            {
            assertThat(mapService.size(), greaterThan(ATTRIBUTES_COUNT));
            assertThat(mapService.get(NAME), isOneOf(SERVICES_LIST));
            assertThat(mapService.get("type"), notNullValue());
            assertThat(mapService.get("memberCount"), notNullValue());
            assertThat(((Map<String, Number>) mapService.get("running")).get("true").intValue(), greaterThanOrEqualTo(1));
            assertThat(mapService.get("statistics"), notNullValue());
            assertThat(mapService.get("storageEnabled"), notNullValue());
            assertThat(mapService.get("threadCount"), notNullValue());

            membersResponseMap = (Map) mapService.get("members");
            assertThat(membersResponseMap, notNullValue());

            List<Map> memberItems = (List<Map>) membersResponseMap.get("items");
            assertThat(memberItems, notNullValue());

            for (Map memberMap: memberItems)
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
        Map mapEntity = new LinkedHashMap();
        mapEntity.put("links", new String[]{});
        mapEntity.put("fields", new String[]{});

        Map mapChildren = new LinkedHashMap();
        mapEntity.put("children", mapChildren);

        Map mapServices = new LinkedHashMap();
        mapServices.put("links", new String[]{});
        mapServices.put("fields", new String[]{NAME,"type"});
        mapChildren.put(SERVICES, mapServices);

        Map cachesMap = new LinkedHashMap();
        cachesMap.put("links", new String[]{});
        cachesMap.put("fields", new String[]{NAME});

        Map mapServiceMembersChildren = new LinkedHashMap();
        mapServices.put("children", mapServiceMembersChildren);
        mapServiceMembersChildren.put(CACHES, cachesMap);

        WebTarget target   = getBaseTarget().path("search");
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response, entity);
        assertThat(mapResponse.size(), is(1));

        Map membersResponseMap = (Map) mapResponse.get(SERVICES);
        assertThat(membersResponseMap, notNullValue());
        List<Map> listMembers = (List<Map>) membersResponseMap.get("items");
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), is(EXPECTED_SERVICE_COUNT)); // <---- This is SO brittle!!!

        for (Map mapServiceMember: listMembers)
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

            Map mapCachesResponse = (Map) mapServiceMember.get(CACHES);
            assertThat(mapCachesResponse, notNullValue());

            List<Map> listCacheItems = (List<Map>) mapCachesResponse.get("items");
            assertThat(listCacheItems, notNullValue());

            for (Map mapMember: listCacheItems)
                {
                assertThat(mapMember.size(), greaterThan(ATTRIBUTES_COUNT));
                assertThat(mapMember.get(NAME), notNullValue());
                }
            }
        }

    @Test
    public void testClusterSearchWithServicesAndCacheMembers()
        {
        Map mapEntity = new LinkedHashMap();
        mapEntity.put("links", new String[]{});
        mapEntity.put("fields", new String[]{});

        Map mapServices = new LinkedHashMap();
        mapServices.put("links", new String[]{});
        mapServices.put("fields", new String[]{NAME,"type"});

        mapEntity.put("children", new LinkedHashMap(){{put(SERVICES, mapServices);}});

        Map mapCaches = new LinkedHashMap();
        mapCaches.put("links", new String[]{});
        mapCaches.put("fields", new String[]{NAME});

        mapServices.put("children", new LinkedHashMap(){{put(CACHES, mapCaches);}});

        Map cachesMembersMap = new LinkedHashMap();
        cachesMembersMap.put("links", new String[]{});
        cachesMembersMap.put("fields", new String[]{NAME, "size"});

        mapCaches.put("children", new LinkedHashMap(){{put("members", cachesMembersMap);}});

        WebTarget target   = getBaseTarget().path("search");
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response, entity);
        assertThat(mapResponse.size(), is(1));

        Map mapMembersResponse = (Map) mapResponse.get(SERVICES);
        assertThat(mapMembersResponse, notNullValue());
        List<Map> listMembers = (List<Map>) mapMembersResponse.get("items");
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), is(EXPECTED_SERVICE_COUNT)); // <---- This is SO brittle!!!

        for (Map mapService: listMembers)
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

            Map cachesResponseMap = (Map) mapService.get(CACHES);
            assertThat(cachesResponseMap, notNullValue());

            List<Map> listCacheItems = (List<Map>) cachesResponseMap.get("items");
            assertThat(listCacheItems, notNullValue());

            for (Map mapMember: listCacheItems)
                {
                assertThat(mapMember.size(), greaterThan(ATTRIBUTES_COUNT));
                assertThat(mapMember.get(NAME), notNullValue());

                Map mapCachesMembers = (Map) mapMember.get("members");
                assertThat(mapCachesMembers, notNullValue());

                List<Map> cacheMemberItems = (List<Map>) mapCachesMembers.get("items");

                for (Map mapCacheMember : cacheMemberItems)
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
        String sCacheName = "dist-persistence-test";

        f_inClusterInvoker.accept(f_sClusterName, () ->
            {
            NamedCache cache = CacheFactory.getCache(sCacheName);
            cache.clear();
            return null;
            });

        try
            {
            // create an empty snapshot
            createSnapshot("empty");
            ensureServiceStatusIdle();

            // assert the snapshot exists
            Eventually.assertDeferred(() -> assertSnapshotExists("empty", SNAPSHOTS), is(true));

            // add some data
            f_inClusterInvoker.accept(f_sClusterName, () ->
                {
                NamedCache cache = CacheFactory.getCache(sCacheName);
                cache.put("key-1", "value-1");
                cache.put("key-2", "value-2");
                assertThat(cache.size(), is(2));
                return null;
                });

            // create a second snapshot
            createSnapshot("2-entries");
            ensureServiceStatusIdle();
            Eventually.assertDeferred(() -> assertSnapshotExists("2-entries", SNAPSHOTS), is(true));

            // archive the snapshot
            Response response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(ARCHIVES).path("2-entries")
                    .request().post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            response.close();
            ensureServiceStatusIdle();

            Eventually.assertDeferred(() -> assertSnapshotExists("2-entries", ARCHIVES), is(true));

            // remove the local snapshot
            deleteSnapshot("2-entries");
            ensureServiceStatusIdle();
            Eventually.assertDeferred(() -> assertSnapshotExists("2-entries", SNAPSHOTS), is(false));

            // retrieve the archived snapshot
            response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(ARCHIVES).path("2-entries").path("retrieve")
                    .request().post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            response.close();
            ensureServiceStatusIdle();

            // check the existence of the local snapshot but delay as a single member could have the snapshot but it not be complete
            Eventually.assertDeferred(() -> assertSnapshotExists("2-entries", SNAPSHOTS), is(true));

            // delete the archived snapshot
            response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(ARCHIVES).path("2-entries")
                    .request().delete();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            response.close();
            ensureServiceStatusIdle();

            Eventually.assertDeferred(() -> assertSnapshotExists("2-entries", ARCHIVES), is(false));

            // now we have local snapshot, clear the cache and then recover the snapshot
            f_inClusterInvoker.accept(f_sClusterName, () ->
                {
                NamedCache cache = CacheFactory.getCache(sCacheName);
                cache.clear();
                assertThat(cache.size(), is(0));
                return null;
                });

            response = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).path(SNAPSHOTS).path("2-entries").path("recover")
                    .request().post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
            response.close();
            ensureServiceStatusIdle();

            f_inClusterInvoker.accept(f_sClusterName, () ->
                {
                NamedCache cache = CacheFactory.getCache(sCacheName);
                Eventually.assertDeferred(cache::size, is(2));
                return null;
                });

            // now delete the 2 snapshots

            deleteSnapshot("2-entries");
            ensureServiceStatusIdle();
            Eventually.assertDeferred(() -> assertSnapshotExists("2-entries", SNAPSHOTS), is(false));

            deleteSnapshot("empty");
            ensureServiceStatusIdle();
            Eventually.assertDeferred(() -> assertSnapshotExists("empty", SNAPSHOTS), is(false));
            }
        finally
            {
            f_inClusterInvoker.accept(f_sClusterName, () ->
                {
                NamedCache cache = CacheFactory.getCache(sCacheName);
                cache.destroy();
                return null;
                });
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
        Map mapResponse = readEntity(target, response);

        Object objListLinks = mapResponse.get("links");
        assertThat(objListLinks, instanceOf(List.class));

        for (Map<String,String> link : (List<Map>) objListLinks)
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
        Eventually.assertDeferred(this::assertServiceIdle, is(true), Eventually.delayedBy(5, TimeUnit.SECONDS));
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

    /**
     * Assert that the persistence status is idle.
     */
    public boolean assertServiceIdle()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path(ACTIVE_SERVICE).path(PERSISTENCE).queryParam("fields", "operationStatus");
        Response  response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        Map mapResponse = readEntity(target, response);
        assertThat(mapResponse, is(notNullValue()));

        String result = (String) mapResponse.get("operationStatus");
        assertThat(result, is(notNullValue()));
        return "Idle".equals(result);
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

        Map mapResponse = readEntity(target, response);
        assertThat(mapResponse, is(notNullValue()));

        List<String> result = (List) mapResponse.get(sType);
        assertThat(result, is(notNullValue()));

        return new HashSet<>(result);
        }

    /**
     * Return the response from a reporter instance.
     * @param sMember  member id or memberName
     * @return the {@link Map} respone
     */
    public Map getReporterResponse(String sMember)
        {
        WebTarget target   = getBaseTarget().path(REPORTERS).path(sMember);
        Response  response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        return mapResponse;
        }

    private void setReporterAttribute(String sMember, String sAttribute, Object value)
        {
        Map mapEntity = new LinkedHashMap();
        mapEntity.put(sAttribute, value);

        WebTarget target   = getBaseTarget().path(REPORTERS).path(sMember);
        Response  response = target.request().post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    public boolean assertReporterAttribute(String sMember, String sAttribute, Object value)
        {
        Map mapResults = getReporterResponse(sMember);
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
            return ((Number) result).longValue() == ((Number)value).longValue();
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
        List<Map> linksMap = (List) mapResponse.get("links");
        assertThat(linksMap, notNullValue());
        Map selfLinkMap = linksMap.stream().filter(m -> m.get("rel").
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
        Map mapResponse = readEntity(target, response);

        String    sMembersUrl = getLink(mapResponse, "members");
        WebTarget memTarget   = m_client.target(sMembersUrl).queryParam("tier", "back");

        response = memTarget.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        Map mapCacheMembers = new LinkedHashMap(readEntity(memTarget, response));
        assertThat(mapCacheMembers, notNullValue());

        List<Map> listCacheMembers = (List<Map>) mapCacheMembers.get("items");
        assertThat(listCacheMembers, notNullValue());

        for(Map mapCacheMember : listCacheMembers)
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

            List<Map> listCacheMembersOfANode = (List<Map>) mapResponse.get("items");
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
        Map mapResponse = readEntity(target, response);
        assertThat(mapResponse, notNullValue());
        List<Map> listCacheMaps = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());
        assertThat(listCacheMaps.size(), greaterThan(1));

        for (Map mapCache : listCacheMaps)
            {
            String sCacheName = (String) mapCache.get(NAME);
            assertThat(mapCache.get(NAME), isOneOf(CACHES_LIST));

            if (!sCacheName.equals("dist-persistence-test"))
                {
                Object size = mapCache.get("size");
                assertThat(sCacheName, size, is(instanceOf(Number.class)));
                assertThat(sCacheName, ((Number) size).intValue(), greaterThan(0));
                }

            assertThat(SERVICE, mapCache.get(SERVICE), isOneOf(SERVICES_LIST));
            Assert.assertNotNull(NODE_ID, mapCache.get(NODE_ID));
            }

        WebTarget cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", "name,totalPuts");
        Response cachesResponse = cachesTarget.request().get();
        mapResponse = new LinkedHashMap(readEntity(cachesTarget, cachesResponse));
        listCacheMaps = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());
        assertThat(listCacheMaps.size(), greaterThan(1));

        for (Map mapCache : listCacheMaps)
            {
            if (!mapCache.get(NAME).equals("dist-persistence-test"))
                {
                assertThat("Cache " + mapCache.get(NAME) + " failed assertion of totalPuts greater than 0", ((Number) mapCache.get("totalPuts")).intValue(), greaterThan(0));
                }
            }

        cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", "name,units");
        cachesResponse = cachesTarget.request().get();
        mapResponse = new LinkedHashMap(readEntity(cachesTarget, cachesResponse));
        listCacheMaps = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());

        for (Map mapCache : listCacheMaps)
            {
            assertThat(mapCache.get(NAME), isOneOf(CACHES_LIST));
            if (mapCache.get(NAME).equals("dist-foo"))
                {
                Object cUnits = mapCache.get("units");
                assertThat("Cache " + NAME, cUnits, is(instanceOf(Number.class)));
                assertThat("Cache " + NAME, ((Number) cUnits).intValue(), anyOf(is(1), is(20)));
                }
            else
                {
                if (!mapCache.get(NAME).equals("dist-persistence-test"))
                    {
                    assertThat("Cache " + NAME + "assertion", ((Number) mapCache.get("units")).longValue(), is(1L));
                    }
                }
            }

        cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", "name,insertCount");
        cachesResponse = cachesTarget.request().get();
        mapResponse = new LinkedHashMap(readEntity(cachesTarget, cachesResponse));
        listCacheMaps = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());

        for (Map mapCache : listCacheMaps)
            {
            assertThat(mapCache.get(NAME), isOneOf(CACHES_LIST));
            if (!mapCache.get(NAME).equals("dist-persistence-test"))
                {
                assertThat(((Number) mapCache.get("insertCount")).intValue(), greaterThan(0));
                }
            }

        cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", SERVICE);
        cachesResponse = cachesTarget.request().get();
        mapResponse = new LinkedHashMap(readEntity(cachesTarget, cachesResponse));
        listCacheMaps = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());

        for (Map mapCache : listCacheMaps)
            {
            assertNull(mapCache.get(NAME));
            assertThat(mapCache.get(SERVICE), isOneOf(SERVICES_LIST));
            }
        }

    // only validate response values agasint specified cache name
    private void testCachesResponse(WebTarget target, Response response, String sCacheName)
        {
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        Map mapResponse = readEntity(target, response);
        assertThat(mapResponse, notNullValue());
        List<Map> listCacheMaps = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());
        assertThat(listCacheMaps.size(), greaterThan(1));

        for (Map mapCache : listCacheMaps)
            {
            String sName = (String) mapCache.get(NAME);
            assertThat(mapCache.get(NAME), isOneOf(CACHES_LIST));

            if (sName.equals(sCacheName))
                {
                Object size = mapCache.get("size");
                assertThat(sCacheName, size, is(instanceOf(Number.class)));
                assertThat("Validating size of Cache: " + sCacheName, ((Number) size).intValue(), greaterThan(0));
                }

            assertThat(SERVICE, mapCache.get(SERVICE), isOneOf(SERVICES_LIST));
            Assert.assertNotNull(NODE_ID, mapCache.get(NODE_ID));
            }

        WebTarget cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", "name,totalPuts");
        Response cachesResponse = cachesTarget.request().get();
        mapResponse = new LinkedHashMap(readEntity(cachesTarget, cachesResponse));
        listCacheMaps = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());
        assertThat(listCacheMaps.size(), greaterThan(1));

        for (Map mapCache : listCacheMaps)
            {
            if (mapCache.get(NAME).equals(sCacheName))
                {
                assertThat("Cache " + sCacheName + " failed assertion of totalPuts greater than 0", ((Number) mapCache.get("totalPuts")).intValue(), greaterThan(0));
                }
            }

        cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", "name,units");
        cachesResponse = cachesTarget.request().get();
        mapResponse = new LinkedHashMap(readEntity(cachesTarget, cachesResponse));
        listCacheMaps = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());

        for (Map mapCache : listCacheMaps)
            {
            assertThat(mapCache.get(NAME), isOneOf(CACHES_LIST));
            if (mapCache.get(NAME).equals(sCacheName))
                {
                Object cUnits = mapCache.get("units");
                assertThat("Cache " + sCacheName, cUnits, is(instanceOf(Number.class)));
                assertThat("Cache " + sCacheName, ((Number) cUnits).intValue(), anyOf(is(1), is(20)));
                }
            }

        cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", "name,insertCount");
        cachesResponse = cachesTarget.request().get();
        mapResponse = new LinkedHashMap(readEntity(cachesTarget, cachesResponse));
        listCacheMaps = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());

        cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", SERVICE);
        cachesResponse = cachesTarget.request().get();
        mapResponse = new LinkedHashMap(readEntity(cachesTarget, cachesResponse));
        listCacheMaps = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());

        for (Map mapCache : listCacheMaps)
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
                String sPort = s_cluster.getAny().getSystemProperties().getProperty("test.multicast.port", "7777");
                int    nPort = Integer.parseInt(sPort);
                m_baseURI = NSLookup.lookupHTTPManagementURL(f_sClusterName, new InetSocketAddress("127.0.0.1", nPort)).iterator().next().toURI();

                Logger.info("Management HTTP Acceptor lookup returned: " + m_baseURI);
                }
            return client.target(m_baseURI);
            }
        catch(IOException | URISyntaxException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    public Object getAttributeValue(Client client, String sService, String sAttributeName)
        {
        WebTarget target   = getBaseTarget(client).path(SERVICES).path(sService).path("members").path(SERVER_PREFIX + "-1");
        Response  response = target.request().get();

        assertThat(response.getStatus(), CoreMatchers.is(Response.Status.OK.getStatusCode()));
        Map mapResponse = readEntity(target, response);
        return mapResponse.get(sAttributeName);
        }

    /**
     * Assert that the response does not conain any "messages" elements, which
     * would indicate errors.
     *
     * @param response Response
     */
    private void assertNoMessages(Response  response)
        {
        Map mapResponse = new LinkedHashMap(response.readEntity(Map.class));
        List<Map> listMessages = (List) mapResponse.get("messages");
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
     */
    protected void testDistServiceInfo(Map mapResponse)
        {
        System.out.println(mapResponse.toString());

        List<Number> ownedPartitions = (List<Number>) mapResponse.get("ownedPartitionsPrimary");
        assertThat(ownedPartitions.size(), is(2));
        assertThat(ownedPartitions.get(0).intValue() + ownedPartitions.get(1).intValue(), is(257));

        ownedPartitions = (List<Number>) mapResponse.get("ownedPartitionsBackup");
        assertThat(ownedPartitions.size(), is(2));
        assertThat(ownedPartitions.get(0).intValue() + ownedPartitions.get(1).intValue(), is(257));

        List<Number> partitionsStatus = (List<Number>) mapResponse.get("partitionsVulnerable");
        assertThat(partitionsStatus.size(), is(1));
        assertThat(partitionsStatus.get(0).intValue(), is(257));

        partitionsStatus = (List<Number>) mapResponse.get("partitionsAll");
        assertThat(partitionsStatus.size(), is(1));
        assertThat(partitionsStatus.get(0).intValue(), is(257));

        partitionsStatus = (List<Number>) mapResponse.get("partitionsEndangered");
        assertThat(partitionsStatus.size(), is(1));
        assertThat(partitionsStatus.get(0).intValue(), is(0));

        partitionsStatus = (List<Number>) mapResponse.get("partitionsUnbalanced");
        assertThat(partitionsStatus.size(), is(1));
        assertThat(partitionsStatus.get(0).intValue(), is(0));

        Map map = (Map) mapResponse.get("requestAverageDuration");
        assertThat(map.size(), is(5));

        assertThat(Long.parseLong(mapResponse.get("taskMaxBacklog").toString()), greaterThanOrEqualTo(0L));

        assertThat(((Number) ((Map) mapResponse.get("storageEnabled")).get("true")).longValue(), is(2L));
        assertThat(((Number) ((Map) mapResponse.get("threadPoolSizingEnabled")).get("true")).longValue(), is(2L));

        assertThat(((List<?>) mapResponse.get("joinTime")).size(), is(2));
        assertThat(((List<?>) mapResponse.get("statistics")).size(), is(2));

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

        Collection<Number> colPartCount = (Collection) mapResponse.get("partitionsAll");
        assertEquals(1, colPartCount.size());
        assertThat(colPartCount.iterator().next().longValue(), is(257L));

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
     */
    protected void testDistServicesInfo(Map mapResponse)
        {
        System.out.println(mapResponse.toString());

        WebTarget target   = getBaseTarget().path(SERVICES).queryParam("fields", "statusHA");
        Response  response = target.request().get();
        mapResponse = readEntity(target, response);
        List<Map> listServiceMaps = (List<Map>) mapResponse.get("items");
        assertThat(listServiceMaps, notNullValue());
        assertThat(listServiceMaps.size(), greaterThan(1));

        int cServices = 0;
        for (Map mapService : listServiceMaps)
            {
            // if the mapService has more than 3 items, check that it contains statusHA
            if (mapService.size() > 3)
                {
                cServices++;
                assertThat((Collection<String>) mapService.get("statusHA"), Matchers.hasItem("NODE-SAFE"));
                }
            }
        assertThat(cServices, is(2)); // <--- This is very brittle!

        target = getBaseTarget().path(SERVICES).queryParam("fields", "taskCount")
                .queryParam("collector", "list")
                .queryParam("role", "*");
        response = target.request().get();
        mapResponse = readEntity(target, response);
        listServiceMaps = (List<Map>) mapResponse.get("items");
        assertThat(listServiceMaps, notNullValue());
        assertThat(listServiceMaps.size(), greaterThan(1));

        cServices = 0;
        for (Map mapService : listServiceMaps)
            {
            if (mapService.size() > 3)
                {
                Number taskCount = (Number) mapService.get("taskCount");
                if (taskCount.intValue() > 0)
                    {
                    cServices++;
                    }
                }
            }
        assertThat(cServices, is(2));  // <--- This is very brittle!
        }

    protected Map readEntity(WebTarget target, Response response)
            throws ProcessingException
        {
        return readEntity(target, response, null);
        }

    protected Map readEntity(WebTarget target, Response response, Entity entity)
            throws ProcessingException
        {
        int i = 0;
        while (true)
            {
            String sJson = null;
            try
                {
                sJson = response.readEntity(String.class);

                Map map = sJson != null ? f_genson.deserialize(sJson, LinkedHashMap.class) : null;
                if (map == null)
                    {
                    Logger.info(getClass().getName() + ".readEntity() returned null"
                            + ", target: " + target + ", response: " + response);
                    }
                else
                    {
                    return map;
                    }
                }
            catch (ProcessingException | IllegalStateException e)
                {
                Logger.info(getClass().getName() + ".readEntity() got an error "
                        + " from target " + target + " exception:"
                        + e + ", cause: " + e.getCause().getLocalizedMessage());

                if (sJson != null)
                    {
                    Logger.info("JSON response that caused error\n" + sJson);
                    }

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

    public static void startTestCluster(Class<?> clsMain, String sClusterName)
        {
        startTestCluster(clsMain,
                         sClusterName,
                         BaseManagementInfoResourceTests::assertClusterReady,
                         BaseManagementInfoResourceTests::invokeInCluster,
                         opts -> opts);
        }

    public static void startTestCluster(Class<?> clsMain, String sClusterName, Option... options)
        {
        startTestCluster(clsMain,
                         sClusterName,
                         BaseManagementInfoResourceTests::assertClusterReady,
                         BaseManagementInfoResourceTests::invokeInCluster,
                         opts -> opts,
                         options);
        }

    public static void startTestCluster(Class<?> clsMain,
                                        String sClusterName,
                                        Consumer<CoherenceCluster> clusterReady,
                                        BiConsumer<String, RemoteCallable<Void>> inClusterInvoker,
                                        Function<OptionsByType, OptionsByType> beforeLaunch,
                                        Option... opts)
        {
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

        OptionsByType commonOptions = AbstractTestInfrastructure.createCacheServerOptions(clsMain.getName(), null, System.getProperties());

        AbstractTestInfrastructure.addTestProperties(commonOptions);
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
        propsServer1.add(SystemProperty.of("coherence.management.http.port", 0));
        propsServer1.add(SystemProperty.of("coherence.management.http.cluster", sClusterName));
        propsServer1.add(SystemProperty.of("test.persistence.active.dir", m_dirActive.getAbsolutePath()));
        propsServer1.add(SystemProperty.of("test.persistence.snapshot.dir", m_dirSnapshot.getAbsolutePath()));
        propsServer1.add(SystemProperty.of("test.persistence.archive.dir", m_dirArchive.getAbsolutePath()));
        propsServer1.add(LocalStorage.enabled());
        propsServer1.add(CacheConfig.of(CACHE_CONFIG));
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
        inClusterInvoker.accept(sClusterName, BaseManagementInfoResourceTests::popluateCaches);

        m_client = ClientBuilder.newBuilder()
                .register(MapProvider.class)
                .build();
        }

    protected static Void popluateCaches()
        {
        NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
        Binary     binValue = Binary.getRandomBinary(1024, 1024);

        cache.put(1, binValue);

        // fill front cache
        cache = CacheFactory.getCache(NEAR_CACHE_NAME);
        cache.put(1, binValue);

        return null;
        }

    protected static void invokeInCluster(String sCluster, RemoteCallable<Void> callable)
        {
        s_cluster.getAny().invoke(callable);
        }

    protected static void assertClusterReady(CoherenceCluster cluster)
        {
        for (CoherenceClusterMember member : cluster)
            {
            Eventually.assertDeferred(() -> member.getServiceStatus(SERVICE_NAME), is(ServiceStatus.NODE_SAFE));
            Eventually.assertDeferred(() -> member.getServiceStatus(ACTIVE_SERVICE), is(ServiceStatus.NODE_SAFE));
            Eventually.assertDeferred(() -> member.invoke(new CalculateUnbalanced("dist-persistence-test")),
                                      Matchers.is(0),
                                      within(3, TimeUnit.MINUTES));
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
                Logger.err(e);
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
    @SuppressWarnings("SameParameterValue")
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
     * The name persistence path.
     */
    protected static final String PERSISTENCE = "persistence";

    /**
     * The snapshots path.
     */
    protected static final String SNAPSHOTS = "snapshots";

    /**
     * The archives path.
     */
    protected static final String ARCHIVES = "archives";

    /**
     * The services path.
     */
    protected static final String SERVICES = "services";

    /**
     * The name of the active persistence service.
     */
    protected static final String ACTIVE_SERVICE = "DistributedCachePersistence";

    /**
     * The name of the used PartitionedService.
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
    private static final String[] SERVICES_LIST = {SERVICE_NAME, "ExtendHttpProxyService", "ExtendProxyService",
            "DistributedCachePersistence", HttpHelper.getServiceName()};

    /**
     * The list of caches used by this test class.
     */
    private static final String[] CACHES_LIST = {CACHE_NAME, "near-test", "dist-foo", "dist-persistence-test"};

    /**
     * Prefix for the spawned processes.
     */
    protected static String SERVER_PREFIX = "testMgmtRESTServer";

    /**
     * Cache config used by the test and spawned processes.
     */
    protected static final String CACHE_CONFIG  = "server-cache-config-mgmt.xml";

    /**
     * The window of time the management server tries to give a consistent response.
     */
    protected static final long REMOTE_MODEL_PAUSE_DURATION = 128L + /*buffer*/ 16L;

    protected static final Boolean s_bTestJdk11 = Integer.parseInt(System.getProperty("java.version").split("-|\\.")[0]) > 10;

    /**
     * The number of attributes to check for.
     */
    protected static final int ATTRIBUTES_COUNT = 20;

    /**
     * The expected number of services on the server, this is very brittle!
     */
    public static final int EXPECTED_SERVICE_COUNT = 4;

    /**
     * Name of the Coherence cluster.
     */
    public static final String CLUSTER_NAME = "mgmtRestCluster";

    /**
     * The cluster members.
     */
    protected static CoherenceCluster s_cluster;

    protected final String f_sClusterName;

    @Rule
    public final TestName m_testName = new TestName();

    @ClassRule
    public static final TestLogs m_testLogs = new TestLogs();

    private final BiConsumer<String, RemoteCallable<Void>> f_inClusterInvoker;

    private final Genson f_genson = new GensonBuilder().create();
    }
