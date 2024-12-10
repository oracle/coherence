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
import com.oracle.bedrock.runtime.coherence.ServiceStatus;

import com.oracle.bedrock.runtime.coherence.callables.IsReady;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;
import com.oracle.bedrock.runtime.concurrent.runnable.RuntimeHalt;

import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.features.JmxFeature;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.MavenProjectFileUtils;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.Randoms;
import com.oracle.coherence.common.base.Reads;

import com.tangosol.coherence.management.internal.MapProvider;

import com.tangosol.discovery.NSLookup;

import com.tangosol.internal.management.MBeanResponse;

import com.tangosol.internal.management.resources.AbstractManagementResource;
import com.tangosol.internal.management.resources.ClusterMemberResource;
import com.tangosol.internal.management.resources.ClusterResource;

import com.tangosol.internal.net.management.HttpHelper;

import com.tangosol.internal.util.LongSummaryStatistics;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.management.MapJsonBodyHandler;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.filter.AlwaysFilter;

import com.oracle.coherence.testing.AbstractTestInfrastructure;

import java.math.BigDecimal;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TestName;

import com.oracle.coherence.testing.CheckJDK;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static com.tangosol.internal.management.resources.AbstractManagementResource.CACHES;
import static com.tangosol.internal.management.resources.AbstractManagementResource.CLEAR;
import static com.tangosol.internal.management.resources.AbstractManagementResource.DESCRIPTION;
import static com.tangosol.internal.management.resources.AbstractManagementResource.MANAGEMENT;
import static com.tangosol.internal.management.resources.AbstractManagementResource.MEMBER;
import static com.tangosol.internal.management.resources.AbstractManagementResource.MEMBERS;
import static com.tangosol.internal.management.resources.AbstractManagementResource.METADATA_CATALOG;
import static com.tangosol.internal.management.resources.AbstractManagementResource.NAME;
import static com.tangosol.internal.management.resources.AbstractManagementResource.NODE_ID;
import static com.tangosol.internal.management.resources.AbstractManagementResource.OPTIONS;
import static com.tangosol.internal.management.resources.AbstractManagementResource.PROXY;
import static com.tangosol.internal.management.resources.AbstractManagementResource.REPORTERS;
import static com.tangosol.internal.management.resources.AbstractManagementResource.RESET_STATS;
import static com.tangosol.internal.management.resources.AbstractManagementResource.ROLE_NAME;
import static com.tangosol.internal.management.resources.AbstractManagementResource.SERVICE;
import static com.tangosol.internal.management.resources.AbstractManagementResource.STORAGE;
import static com.tangosol.internal.management.resources.AbstractManagementResource.TRUNCATE;
import static com.tangosol.internal.management.resources.AbstractManagementResource.VIEWS;
import static com.tangosol.internal.management.resources.ClusterMemberResource.DIAGNOSTIC_CMD;
import static com.tangosol.internal.management.resources.ClusterResource.DUMP_CLUSTER_HEAP;
import static com.tangosol.internal.management.resources.ClusterResource.ROLE;
import static com.tangosol.internal.management.resources.ClusterResource.TRACING_RATIO;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.oneOf;
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
 * @author gh 2022.05.13
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class BaseManagementInfoResourceTests
    {
    // ----- constructors ---------------------------------------------------

    public BaseManagementInfoResourceTests()
        {
        this(CLUSTER_NAME, BaseManagementInfoResourceTests::invokeInCluster);
        }

    public BaseManagementInfoResourceTests(String sClusterName, InClusterInvoker inClusterInvoker)
        {
        f_sClusterName     = sClusterName;
        f_inClusterInvoker = inClusterInvoker;
        }

    // ----- junit lifecycle methods ----------------------------------------

    @AfterClass
    public static void tearDown()
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
                    Logger.info(sMsg);
                    System.err.println(sMsg);
                    System.err.flush();
                    return null;
                    }).join();
                }
            }

        if (!s_fInvocationServiceStarted)
            {
            startService(INVOCATION_SERVICE_NAME, INVOCATION_SERVICE_TYPE);
            s_fInvocationServiceStarted = true;
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
    public void testClusterDescription()
        {
        WebTarget target   = getBaseTarget().path(DESCRIPTION);
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        assertThat(((String) (readEntity(target, response).get(DESCRIPTION)))
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
        Map mapResponse = readEntity(target, response);

        Object oListMemberIds = mapResponse.get("memberIds");
        assertThat(oListMemberIds, instanceOf(List.class));
        List listMemberIds = (List) oListMemberIds;

        for (Object memberId : listMemberIds)
            {
            target   = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path("memory");
            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = readEntity(target, response);
            Map mapHeapUsage = (Map) mapResponse.get("heapMemoryUsage");
            assertThat(mapHeapUsage, notNullValue());
            assertThat(((Number) mapHeapUsage.get("used")).intValue(), greaterThan(1));
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
        String[] arr_sMbeanName = {GC_PREFIX + "OldGen", GC_PREFIX + "SurvivorSpace"};

        for (String mbean : arr_sMbeanName)
            {
            for (Object memberId : listMemberIds)
                {
                target   = getBaseTarget().path(MEMBERS).path(memberId.toString()).path("platform").path(mbean);
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
        Response response = getBaseTarget().path(MEMBERS).path(SERVER_PREFIX + "-1").request().get();
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
        List   listMemberIds = (List) objListMemberIds;
        Object memberId      = listMemberIds.get(0);

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

        target   = getBaseTarget().path(MEMBERS).path(oMemberId.toString()).path("networkStats");
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
        Object oMemberId     = listMemberIds.get(0);

        response = getBaseTarget().path(MEMBERS).path(oMemberId.toString()).path("networkStats").path("trackWeakest")
                .request().post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testMembersTrackWeakest()
        {
        Response response = getBaseTarget().path("networkStats").path("trackWeakest")
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
                                .findFirst().orElse(new LinkedHashMap());
        String sJmxURl = (String) mapJmxManagement.get("href");

        target   = m_client.target(sJmxURl);
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
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

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
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

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

        target   = m_client.target(sJmxURl);
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

            target   = m_client.target(getSelfLink(mapMember));
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
    public void testMembersLogState()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Response response = getBaseTarget().path("logMemberState")
                .request(MediaType.APPLICATION_JSON_TYPE).post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testMemberDumpHeap()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

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
    public void testProxyConnectionManagerResetStats()
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

        response = getBaseTarget().path("services").path(getQuotedScopedServiceName(PROXY_SERVICE_NAME))
                .path("members").path(nMemberId + "").path("proxy")
                .path("resetStatistics").request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testMembersResetStats()
        {
        Response  response = getBaseTarget().path(RESET_STATS).request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testMemberDescription()
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

        target = getBaseTarget().path("members").path(String.valueOf(nMemberId)).path(DESCRIPTION);
        response = target.request(MediaType.APPLICATION_JSON_TYPE).get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(((String) readEntity(target, response).get(DESCRIPTION))
                           .startsWith("Member(Id=" + nMemberId),
                   is(true));
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

        Map    mapResult  = readEntity(target, response);
        List   listStatus = (List) mapResult.get("status");
        String result     = (String) listStatus.get(0) + listStatus.get(1);
        assertThat("Result returned: " + result, result.indexOf(SERVER_PREFIX + "-1"), greaterThanOrEqualTo(0));
        assertThat("Result returned: " + result, result.indexOf(SERVER_PREFIX + "-2"), greaterThanOrEqualTo(0));

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

        mapResult  = readEntity(target, response);
        listStatus = (List) mapResult.get("status");
        result     = (String) listStatus.get(0) + listStatus.get(1);

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
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        // This test requires Flight Recorder and only runs on Oracle JVMs
        CheckJDK.assumeOracleJDK();

        File   jfrDir    = MavenProjectFileUtils.ensureTestOutputFolder(getClass(), "jfr");
        String sFilePath = jfrDir.getAbsolutePath() + File.separator;
        String sFileName = sFilePath + "testMemberJfr-myRecording.jfr";

        WebTarget target  = getBaseTarget().path(DIAGNOSTIC_CMD).path("jfrStart")
                .queryParam(OPTIONS, encodeValue("name=myJfr,duration=3s,filename="+ sFileName))
                .queryParam(ROLE_NAME, SERVER_PREFIX + "-1");
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE).post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        String result = (String) ((List) readEntity(target, response).get("status")).get(0);

        File testFile1 = new File(sFilePath + "1-testMemberJfr-myRecording.jfr");
        File testFile2 = new File(sFilePath + "2-testMemberJfr-myRecording.jfr");
        assertThat(testFile1.exists() || testFile2.exists(), is(true));
        assertThat(result.indexOf(SERVER_PREFIX + "-2"), is(-1));

        if (testFile1.exists())
            {
            assertThat(testFile1.delete(), is(true));
            assertThat(testFile2.exists(), is(false));
            }
        else
            {
            assertThat(testFile2.delete(), is(true));
            assertThat(testFile1.exists(), is(false));
            }
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
        Map mapResponse = readEntity(getBaseTarget(), response);

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
                    .queryParam(OPTIONS, encodeValue("name=myJfr,duration=5s,filename=" + sFileName)).request(MediaType.APPLICATION_JSON_TYPE)
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
        ObjectName            oName;

        List<CoherenceClusterMember> listMember = s_cluster.stream().collect(Collectors.toList());
        CoherenceClusterMember       memberOne  = listMember.get(0);
        CoherenceClusterMember       memberTwo  = listMember.get(1);
        try
            {
            String sName = "Coherence:type=DiagnosticCommand,Domain=com.sun.management,subType=DiagnosticCommand,cluster=mgmtRestCluster,member=" + memberOne.getName() + ",nodeId=" + memberOne.getLocalMemberId();

            oName       = new ObjectName(sName);
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
                oName       = new ObjectName(sName);
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

        File     jfrDir      = MavenProjectFileUtils.ensureTestOutputFolder(getClass(), "jfr");
        String   sFilePath   = jfrDir.getAbsolutePath() + File.separator;
        String   sFileName   = sFilePath + "testJmxJfr-myRecording.jfr";
        Object[] aoArguments = new Object[]{new String[]{"name=foo", "duration=5s", "filename=" + sFileName}};

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
        listItemMaps.removeIf(serviceMap -> Arrays.stream(TOPICS_SERVICES_LIST).anyMatch(topicServiceName -> ((String) serviceMap.get(NAME)).contains(topicServiceName)));
        assertThat(listItemMaps.size(), greaterThanOrEqualTo(EXPECTED_SERVICE_COUNT)); // <---- This is SO brittle!!!

        String sDistServiceName  = null;
        String sProxyServiceName = null;
        String sMoRESTProxy      = HttpHelper.getServiceName();
        Map    mapDistScheme     = null;
        Map    mapProxyScheme    = null;
        for (Map listItemMap : listItemMaps)
            {
            if (((String) listItemMap.get(NAME)).compareToIgnoreCase(getQuotedScopedServiceName(SERVICE_NAME)) == 0)
                {
                sDistServiceName = getScopedServiceName(SERVICE_NAME);
                mapDistScheme    = listItemMap;
                }
            else if (((String) listItemMap.get(NAME)).compareToIgnoreCase(sMoRESTProxy) == 0)
                {
                sProxyServiceName = sMoRESTProxy;
                mapProxyScheme    = listItemMap;
                }

            if (sDistServiceName != null && sProxyServiceName != null)
                {
                break;
                }
            }

        assertNotNull(mapDistScheme);
        assertNotNull(mapProxyScheme);

        assertThat(mapDistScheme.get(NAME), is(getQuotedScopedServiceName(SERVICE_NAME)));
        assert (((Collection) mapDistScheme.get("type")).contains(SERVICE_TYPE));

        target   = m_client.target(getSelfLink(mapDistScheme));
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        Map mapService = readEntity(target, response);
        assertThat(mapService, notNullValue());
        assertThat(mapService.get(NAME), is(mapDistScheme.get(NAME)));

        testDistServiceInfo(mapService);

        assertThat(mapProxyScheme.get(NAME), is(sMoRESTProxy));
        assertThat((Collection<String>) mapProxyScheme.get("type"), Matchers.hasItem("Proxy"));

        target   = m_client.target(getSelfLink(mapProxyScheme));
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        mapService = readEntity(target, response);
        assertThat(mapService, notNullValue());
        String sName = (String) mapService.get(NAME);
        assertThat(sName, is(mapProxyScheme.get(NAME)));
        assertThat(((List) mapService.get("quorumStatus")).get(0), is("Not configured"));

        target   = getBaseTarget().path(SERVICES).path(sName)
                .queryParam("fields", "storageEnabled");
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        mapResponse = readEntity(target, response);
        assertThat(((Number) ((Map) mapResponse.get("storageEnabled")).get("false")).longValue(), is(1L));
        }

    @Test
    public void testServiceDescription()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path(DESCRIPTION);
        Response  response = target.request(MediaType.APPLICATION_JSON_TYPE).get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(((String) readEntity(target, response).get(DESCRIPTION))
                           .startsWith("PartitionedCache{Name="),
                   is(true));
        }

    @Test
    public void testServiceMembers()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("members");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());

        List<Map> items = (List<Map>) mapResponse.get("items");
        assertThat(items, notNullValue());
        assertThat(items.size(), is(2));

        for (Map mapEntry : items)
            {
            assertThat(mapEntry.get(NAME), is(getQuotedScopedServiceName(SERVICE_NAME)));
            assertThat(mapEntry.get("type"), is(SERVICE_TYPE));
            assertThat(Integer.parseInt((String) mapEntry.get(NODE_ID)), greaterThan(0));
            assertThat(((Number) mapEntry.get("backupCount")).longValue(), is(1L));
            assertThat(mapEntry.get("joinTime"), notNullValue());
            assertThat(mapEntry.get("links"), notNullValue());

            target   = m_client.target(getSelfLink(mapEntry));
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
        WebTarget target = getBaseTarget().path(SERVICES)
                .path(getScopedServiceName(SERVICE_NAME))
                .path("members");

        Response response = target
                .request()
                .header("Accept-Encoding", "gzip")
                .get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        assertThat(response.getHeaderString("Content-Encoding"), is("gzip"));

        Map mapResponse = readEntity(target, response);
        assertThat(mapResponse, is(notNullValue()));
        assertThat(mapResponse.isEmpty(), is(false));
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
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("partition");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("strategyName"), is("SimpleAssignmentStrategy"));
        assertThat(((Number) mapResponse.get("partitionCount")).longValue(), is(257L));
        assertThat(((Number) mapResponse.get("backupCount")).longValue(), is(1L));

        target   = m_client.target(getSelfLink(mapResponse));
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
        WebTarget membersTarget = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("members");
        WebTarget target        = membersTarget.path(SERVER_PREFIX + "-1");
        Response  response      = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get(NAME), is(getQuotedScopedServiceName(SERVICE_NAME)));
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
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Map mapEntity = new LinkedHashMap();
        mapEntity.put("threadCountMin", 5);
        mapEntity.put("taskHungThresholdMillis", 10);
        mapEntity.put("taskTimeoutMillis", 100000);
        mapEntity.put("requestTimeoutMillis", 200000);
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("members").path(SERVER_PREFIX + "-1");
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
    public void testServiceUpdate()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Map mapEntity = new LinkedHashMap();
        mapEntity.put("threadCountMin", 5);
        mapEntity.put("taskHungThresholdMillis", 10);
        mapEntity.put("taskTimeoutMillis", 100000);
        mapEntity.put("requestTimeoutMillis", 200000);

        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME));
        Response  response = target.request().post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        final String sPath       = String.format("services/%s/members", getScopedServiceName(SERVICE_NAME));
        List<Map>    listMembers = getMemberList();

        for (Map mapMember : listMembers)
            {
            String sMember = (String) mapMember.get("memberName");
            Eventually.assertDeferred(() -> assertAttribute(sMember, sPath, "threadCountMin", 5), is(true));
            Eventually.assertDeferred(() -> assertAttribute(sMember, sPath, "taskHungThresholdMillis", 10), is(true));
            Eventually.assertDeferred(() -> assertAttribute(sMember, sPath, "taskTimeoutMillis", 100000), is(true));
            Eventually.assertDeferred(() -> assertAttribute(sMember, sPath, "requestTimeoutMillis", 200000), is(true));
            }
        }

    @Test
    public void testCacheMemberUpdate()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Map<String, Object> mapMethodValues = new HashMap<>()
            {{
               put("highUnits",   100005L);
               put("expiryDelay", 60000L);
            }};

        mapMethodValues.forEach((attribute, value) ->
            {
            System.err.println("Updating " + attribute + " to " + value);
            Map map = new LinkedHashMap();
            map.put(attribute, value);
            WebTarget target = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path(CACHES).path(CACHE_NAME)
                    .path("members").path(SERVER_PREFIX + "-1");
            Response response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            Base.sleep(5000);
            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

            Map       mapResponse = readEntity(target, response);
            List<Map> listItems   = (List<Map>) mapResponse.get("items");
            assertThat(listItems, notNullValue());
            assertThat(listItems.size(), is(1));

            Map mapItem = listItems.get(0);

            assertThat(attribute + " should be " + value + ", but is " + mapItem.get(attribute),
                    ((Number) mapItem.get(attribute)).longValue(), is(value));
            });
        }

    @Test
    public void testCacheMembersUpdate()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Map<String, Object> mapMethodValues = new HashMap<>()
            {{
               put("highUnits",   100005);
               put("expiryDelay", 60000);
            }};

        mapMethodValues.forEach((attribute, value) ->
            {
            System.err.println("Updating " + attribute + " to " + value);
            Map map = new LinkedHashMap();
            map.put(attribute, value);
            WebTarget target = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path(CACHES).path(CACHE_NAME);
            Response response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            final String sPath       = SERVICES + "/" + getScopedServiceName(SERVICE_NAME) + "/" + CACHES + "/" + CACHE_NAME + "/members";
            List<Map>    listMembers = getMemberList();
            for (Map mapMember : listMembers)
                {
                Eventually.assertDeferred(() -> assertAttribute((String) mapMember.get("memberName"), sPath, attribute, value), is(true));
                }
            });
        }

    @Test
    public void testClusterMemberUpdate()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Map<String, Object> mapMethodValues = new HashMap<>()
            {{
               put("loggingLevel",    9L);
               put("resendDelay",     100L);
               put("sendAckDelay",    17L);
               put("trafficJamCount", 2048L);
               put("trafficJamDelay", 12L);
               put("loggingLimit",    2147483640L);
               put("loggingFormat", "{date}/{uptime} {product} {version} <{level}> (thread={thread}, member={member}):- {text}");
            }};
        mapMethodValues.forEach((attribute, value) ->
            {
            System.err.println("Updating " + attribute + " to " + value);

            Map map = new LinkedHashMap();
            map.put(attribute, value);
            WebTarget target   = getBaseTarget().path("members").path(SERVER_PREFIX + "-1");
            Response  response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));

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
    public void testClusterNodesUpdate()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Map<String, Object> mapMethodValues = new HashMap<>()
            {{
            put("loggingLevel",    9);
            put("resendDelay",     100);
            put("sendAckDelay",    17);
            put("trafficJamCount", 2048);
            put("trafficJamDelay", 12);
            put("loggingLimit",    2147483640);
            put("loggingFormat", "{date}/{uptime} {product} {version} <{level}> (thread={thread}, member={member}):- {text}");
            }};
        mapMethodValues.forEach((attribute, value) ->
            {
            System.err.println("Updating " + attribute + " to " + value);

            Map map = new LinkedHashMap();
            map.put(attribute, value);
            Response  response = getBaseTarget().request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));

            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

            List<Map> listMembers = getMemberList();
            for (Map mapMember : listMembers)
                {
                Eventually.assertDeferred(() -> assertAttribute((String) mapMember.get("memberName"), MEMBERS, attribute, value), is(true));
                }
            });
        }

    @Test
    public void testReporterMemberUpdate()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Map    map     = new LinkedHashMap();
        String sMember = SERVER_PREFIX + "-1";

        map.put("intervalSeconds", 15L);
        WebTarget target   = getBaseTarget().path(REPORTERS).path(sMember);
        Response  response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        assertNoMessages(response);

        Eventually.assertDeferred(() -> assertAttribute(sMember, REPORTERS, "intervalSeconds", 15), is(true));

        map.clear();
        map.put("currentBatch", 25);
        response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        assertNoMessages(response);
        Eventually.assertDeferred(() -> assertAttribute(sMember, REPORTERS, "currentBatch", 25), is(true));
        }

    @Test
    public void testReporterUpdate()
        {
        Map map = new LinkedHashMap();

        map.put("intervalSeconds", 25L);
        map.put("currentBatch", 30);
        WebTarget target   = getBaseTarget().path(REPORTERS);
        Response response = target.request().post(Entity.entity(map, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        assertNoMessages(response);

        List<Map> listMembers = getMemberList();
        for (Map mapMember : listMembers)
            {
            String sMember = (String) mapMember.get("memberName");
            Eventually.assertDeferred(() -> assertAttribute(sMember, REPORTERS, "intervalSeconds", 25), is(true));
            Eventually.assertDeferred(() -> assertAttribute(sMember, REPORTERS, "currentBatch", 30), is(true));
            }
        }

    @Test
    public void testClusterMemberUpdateFailure()
        {
        Map map = new LinkedHashMap();
        map.put("cpuCount", 9L);
        WebTarget target   = getBaseTarget().path(MEMBERS).path(SERVER_PREFIX + "-1");
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
        WebTarget target = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path(CACHES).path(CACHE_NAME)
                            .path(MEMBERS).path(SERVER_PREFIX + "-1");
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
        String    sMember  = SERVER_PREFIX + "-1";
        WebTarget target   = getBaseTarget().path(REPORTERS).path(sMember);
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get(NODE_ID), anyOf(Matchers.is("1"), Matchers.is("2")));

        assertThat(Long.parseLong(mapResponse.get("intervalSeconds").toString()), greaterThan(1L));
        assertThat(Long.parseLong(mapResponse.get("runLastMillis").toString()), greaterThan(-1L));
        assertThat(mapResponse.get("outputPath"), is(notNullValue()));
        assertThat(mapResponse.get(MEMBER), is(sMember));
        }

    @Test
    public void testStartAndStopReporter()
            throws IOException
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        String sMember = SERVER_PREFIX + "-1";

        // create a temp directory so we don't pollute any directories
        File tempDirectory = FileHelper.createTempDir();

        try
            {
            setReporterAttribute(sMember, "outputPath", tempDirectory.getAbsolutePath());
            Eventually.assertDeferred(() -> assertAttribute(sMember, REPORTERS, "outputPath", tempDirectory.getAbsolutePath()), is(true));

            // set the intervalSeconds shorter so we don't want as long
            setReporterAttribute(sMember, "intervalSeconds", 15);
            Eventually.assertDeferred(() -> assertAttribute(sMember, REPORTERS, "intervalSeconds", 15), is(true));

            Eventually.assertDeferred(() -> assertAttribute(sMember, REPORTERS, "state", "Stopped"), is(true));
            Map mapEntity = new LinkedHashMap();

            // start the reporter
            Response response = getBaseTarget().path(REPORTERS).path(sMember).path("start").request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

            Eventually.assertDeferred(() -> assertAttribute(sMember, REPORTERS, "state", new String[]{"Sleeping", "Running"}), is(true));

            // stop the reporter
            response = getBaseTarget().path(REPORTERS).path(sMember).path("stop").request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            Eventually.assertDeferred(() -> assertAttribute(sMember, REPORTERS, "state", "Stopped"), is(true), within(2, TimeUnit.MINUTES));
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
    public void testMembersStartAndStopReporter()
            throws IOException
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        // create a temp directory so we don't pollute any directories
        File tempDirectory = FileHelper.createTempDir();

        try
            {
            setMembersReporterAttribute("outputPath", tempDirectory.getAbsolutePath());
            // set the intervalSeconds shorter so we don't want as long
            setMembersReporterAttribute("intervalSeconds", 15);

            List<Map> listMembers = getMemberList();
            for (Map mapMember : listMembers)
                {
                String sMember = (String) mapMember.get("memberName");
                Eventually.assertDeferred(() -> assertAttribute(sMember, REPORTERS, "outputPath", tempDirectory.getAbsolutePath()), is(true));
                Eventually.assertDeferred(() -> assertAttribute(sMember, REPORTERS, "intervalSeconds", 15), is(true));
                Eventually.assertDeferred(() -> assertAttribute(sMember, REPORTERS, "state", "Stopped"), is(true));
                }

            Map mapEntity = new LinkedHashMap();

            // start the reporter
            Response response = getBaseTarget().path(REPORTERS).path("start").request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

            for (Map mapMember : listMembers)
                {
                Eventually.assertDeferred(() -> assertAttribute((String) mapMember.get("memberName"), REPORTERS, "state", new String[]{"Sleeping", "Running"}), is(true), within(2, TimeUnit.MINUTES));
                }

            // stop the reporter
            response = getBaseTarget().path(REPORTERS).path("stop").request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            for (Map mapMember : listMembers)
                {
                Eventually.assertDeferred(() -> assertAttribute((String) mapMember.get("memberName"), REPORTERS, "state", "Stopped"), is(true), within(2, TimeUnit.MINUTES));
                }
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
        WebTarget target = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path(CACHES).path(CACHE_NAME)
                .path("members").path(SERVER_PREFIX + "-1").path("resetStatistics");
        Response response = target.request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testCacheResetStats()
        {
        WebTarget target = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path(CACHES).path(CACHE_NAME)
                .path("resetStatistics");
        Response response = target.request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testServiceMemberResetStats()
        {
        WebTarget membersTarget = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("members");
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
    public void testServiceResetStats()
        {
        WebTarget membersTarget = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME));
        Response  response      = membersTarget.path(RESET_STATS).request().post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        WebTarget target = membersTarget.path(MEMBERS);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Map mapResponse = readEntity(target, response);
        assertThat(mapResponse, notNullValue());

        List<Map> listMemberMaps = (List<Map>) mapResponse.get("items");
        assertThat(listMemberMaps, notNullValue());
        assertThat(listMemberMaps.size(), greaterThan(1));
        for (Map mapService : listMemberMaps)
            {
            assertThat(((Number) mapService.get("requestTotalCount")).intValue(), greaterThanOrEqualTo(0));
            }
        }

    @Test
    public void testSuspendAndResume()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        Response response = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("suspend")
                .request().post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        Eventually.assertDeferred(() -> getAttributeValue(m_client, getScopedServiceName(SERVICE_NAME), "quorumStatus"),
                is("Suspended"));

        response = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("resume")
                .request().post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Eventually.assertDeferred(() -> getAttributeValue(m_client, getScopedServiceName(SERVICE_NAME), "quorumStatus").toString(),
                containsString("allowed-actions"));
        }

    @Test
    public void testServiceMemberStartAndStop()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        String       sMember  = SERVER_PREFIX + "-1";
        final String sService = getScopedServiceName(INVOCATION_SERVICE_NAME);
        final String sPath    = SERVICES + "/" + sService + "/" + MEMBERS;

        Eventually.assertDeferred(() -> assertAttribute(sMember, sPath, "running", true), is(true));

        // stop the service
        Response response = getBaseTarget().path(SERVICES).path(sService).path(MEMBERS).path(sMember)
                                    .path("stop").request(MediaType.APPLICATION_JSON_TYPE).post(null);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        Eventually.assertDeferred(() -> assertAttribute(sMember, sPath, "running", false), is(true));

        // start the service
        Map mapEntity = new LinkedHashMap();

        response = getBaseTarget().path(SERVICES).path(sService).path(MEMBERS).path(sMember).path("start").request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Eventually.assertDeferred(() -> assertAttribute(sMember, sPath, "running", true), is(true), within(2, TimeUnit.MINUTES));
        }

    @Test
    public void testServiceStartAndStop()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        final String sService    = getScopedServiceName(INVOCATION_SERVICE_NAME);
        List<Map>    listMembers = getMemberList();
        final String sPath       = SERVICES + "/" + sService + "/" + MEMBERS;

        for (Map mapMember : listMembers)
            {
            String sMember = (String) mapMember.get("memberName");
            Eventually.assertDeferred(() -> assertAttribute(sMember, sPath, "running", true), is(true));
            }

        // stop the service
        Response response = getBaseTarget().path(SERVICES).path(sService)
                            .path("stop").request(MediaType.APPLICATION_JSON_TYPE).post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        for (Map mapMember : listMembers)
            {
            String sMember = (String) mapMember.get("memberName");
            Eventually.assertDeferred(() -> assertAttribute(sMember, sPath, "running", false), is(true), within(2, TimeUnit.MINUTES));
            }

        // start the service
        Map mapEntity = new LinkedHashMap();
        response = getBaseTarget().path(SERVICES).path(sService).path("start").request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        for (Map mapMember : listMembers)
            {
            String sMember = (String) mapMember.get("memberName");
            Eventually.assertDeferred(() -> assertAttribute(sMember, sPath, "running", true), is(true), within(2, TimeUnit.MINUTES));
            }
        }

    @Test
    public void testService()
        {
        // aggregate all attributes for a service across all nodes
        WebTarget target      = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME));
        Response  response    = target.request().get();
        Map       mapResponse = readEntity(target, response);

        testDistServiceInfo(mapResponse);
        }

    @Test
    public void testServices()
        {
        // aggregate all attributes for all services across all nodes
        WebTarget target      = getBaseTarget().path(SERVICES);
        Response  response    = target.request().get();
        Map       mapResponse = readEntity(target, response);

        testDistServicesInfo(mapResponse);
        }

    @Test
    public void testCache()
        {
        final String CACHE_NAME = CACHE_NAME_FOO;

        f_inClusterInvoker.accept(f_sClusterName, null, () ->
            {
            // fill a cache
            NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
            Binary     binValue = Randoms.getRandomBinary(1024, 1024);
            cache.put(1, binValue);
            return null;
            });

        long[] acTmp = new long[1];
        long   cEntrySize;
        do
            {
            WebTarget target      = getBaseTarget().path(CACHES).path(CACHE_NAME).queryParam("fields", "units")
                                    .queryParam("role", "*");
            Response  response    = target.request().get();
            Map       mapResponse = readEntity(target, response);

            System.out.println(mapResponse);

            cEntrySize = acTmp[0] = ((Number) mapResponse.get("units")).longValue();
            }
        while (sleep(() -> acTmp[0] <= 0L, REMOTE_MODEL_PAUSE_DURATION));

        f_inClusterInvoker.accept(f_sClusterName, null, () ->
            {
            // fill a cache
            NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
            Binary     binValue = Randoms.getRandomBinary(1024, 1024);
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
            WebTarget target      = getBaseTarget().path(CACHES).path(CACHE_NAME).queryParam("fields", "size")
                                    .queryParam("role", "*");
            Response  response    = target.request().get();
            Map       mapResponse = readEntity(target, response);
            acTmp[0] = ((Number) mapResponse.get("size")).longValue();
            }
        while (sleep(() -> acTmp[0] != 10L, REMOTE_MODEL_PAUSE_DURATION));

        WebTarget target      = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME))
                                .path(CACHES).path(CACHE_NAME).queryParam("fields", "size");
        Response  response    = target.request().get();
        Map       mapResponse = readEntity(target, response);

        // aggregate all attributes for a cache on a service across all nodes
        assertThat(((Number) mapResponse.get("size")).longValue(), is(10L));

        target      = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME))
                        .path(CACHES).path(CACHE_NAME).queryParam("fields", "units")
                        .queryParam("role", "*");
        response    = target.request().get();
        mapResponse = readEntity(target, response);

        // aggregate Units attribute for a cache across all nodes
        assertThat(((Number) mapResponse.get("units")).longValue(), is(cEntrySize * 10));

        target      = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME))
                        .path(CACHES).path(CACHE_NAME).queryParam("fields", "units")
                        .queryParam("role", "*")
                        .queryParam("collector", "list");
        response    = target.request().get();
        mapResponse = readEntity(target, response);

        // list the Units attribute for a cache across all nodes
        List<Number> listUnits = (List) mapResponse.get("units");
        assertEquals(2, listUnits.size());
        int cMinUnits = ((int) cEntrySize) * 4;
        listUnits.forEach(n -> assertThat(n.intValue(), greaterThanOrEqualTo(cMinUnits)));

        f_inClusterInvoker.accept(f_sClusterName, null, () ->
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
        f_inClusterInvoker.accept(f_sClusterName, null, () ->
            {
            // fill a cache
            NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
            Binary     binValue = Randoms.getRandomBinary(1024, 1024);
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

        target   = getBaseTarget().path(CACHES).path(CACHE_NAME);
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
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("members")
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
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("members")
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
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("members")
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
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("members")
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
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("members")
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
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("members")
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
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("members")
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
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("members")
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
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("partition")
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
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

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
    public void testReportEnvironment()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        WebTarget target   = getBaseTarget().path("members").path(SERVER_PREFIX + "-1").path("environment");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        assertThat(mapResponse.get("environment"), notNullValue());
        assertThat((String) mapResponse.get("environment"), containsString("Java Runtime Environment"));
        }

    @Test
    public void testProxy()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(PROXY_SERVICE_NAME)).path("members");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());

        List<Map> listItems = (List<Map>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(2));

        for (Map mapEntry : listItems)
            {
            assertThat(mapEntry.get(NAME), is(getQuotedScopedServiceName(PROXY_SERVICE_NAME)));
            target   = m_client.target(getLink(mapEntry, "proxy"));
            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            Map mapMemberResponse = readEntity(target, response);
            assertThat(mapMemberResponse.get("protocol"), notNullValue());
            }
        }

    /**
     * This test ensures that metrics are updated after http requests are issued - Bug 34117583.
     */
    @Test
    public void testHttpProxy()
        {
        if (getBaseTarget().toString().contains("clusters"))
            {
            // if running WLSManagementInfoResourceTests then ignore as the statistics
            // will never be available in this scenario
            return;
            }

        WebTarget target   = getBaseTarget().path(SERVICES).path(PROXY).path("members");
        Response  response = null;

        // run this twice so that the second time we get some metrics
        for (int i = 0; i < 2 ; i++)
            {
            response = target.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
            }

        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());

        List<Map> listItems = (List<Map>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(3));

        for (Map mapEntry : listItems)
            {
            if (mapEntry.get(NAME).equals(HTTP_SERVICE_NAME))
               {
               // convoluted way of getting long as WLS test seems to return Integer
               long nRequestCount = Long.parseLong(mapEntry.get("totalRequestCount").toString());
               assertThat(nRequestCount, is(greaterThan(0L)));
               }
            }
        }

    @Test
    public void testProxyConnections()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(PROXY_SERVICE_NAME)).path("members");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());

        List<Map> listItems = (List<Map>) mapResponse.get("items");
        assertThat(listItems, notNullValue());
        assertThat(listItems.size(), is(2));

        for (Map mapEntry : listItems)
            {
            assertThat(mapEntry.get(NAME), is(getQuotedScopedServiceName(PROXY_SERVICE_NAME)));

            String sProxyUrl = getLink(mapEntry, "proxy");
            response = m_client.target(sProxyUrl).path("connections").request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            }
        }

    @Test
    public void testCaches()
        {
        final String CACHE_NAME = CACHE_NAME_FOO;

        f_inClusterInvoker.accept(f_sClusterName, null, () ->
            {
            // fill a cache
            NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
            Binary     binValue = Randoms.getRandomBinary(1024, 1024);
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
        WebTarget target   = getBaseTarget().path(CACHES).path(CACHE_NAME);
        Response  response = target.request().get();
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
        Response response = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path(CACHES)
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
        target   = m_client.target(sMembersUrl).queryParam("tier", "front");
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Map mapCacheMembers = readEntity(target, response);
        assertThat(mapCacheMembers, notNullValue());

        List<Map> listCacheMembers = (List<Map>) mapCacheMembers.get("items");
        assertThat(listCacheMembers, notNullValue());
        assertThat(listCacheMembers.size(), is(1));

        for (Map mapCacheMember : listCacheMembers)
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
        target   = m_client.target(sMembersUrl);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Map mapCacheMembers = readEntity(target, response);
        assertThat(mapCacheMembers, notNullValue());

        List<Map> listCacheMembers = (List<Map>) mapCacheMembers.get("items");

        assertThat(listCacheMembers.size(), is(3));
        assertThat(listCacheMembers, notNullValue());

        for (Map mapCacheMember : listCacheMembers)
            {
            assertThat(mapCacheMember.get("tier"), is(oneOf("front", "back")));
            assertThat(mapCacheMember.get(NAME), is(NEAR_CACHE_NAME));
            assertThat(mapCacheMember.get("size"), instanceOf(Number.class));
            assertThat(mapCacheMember.get("cacheHits"), instanceOf(Number.class));
            }
        }

    @Test
    public void testCachesOfAService()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path(CACHES);
        Response  response = target.request().get();
        testCachesResponse(target, response);
        }

    @Test
    public void testDistCacheOfService()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path(CACHES).path(CACHE_NAME);
        Response  response = target.request().get();
        testBackCacheResponse(target, response);
        }

    @Test
    public void testSimpleClusterSearch()
        {
        Map mapEntity = new LinkedHashMap();
        mapEntity.put("links", new String[]{});
        mapEntity.put("fields", new String[]{"clusterName", "clusterSize"});

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
        mapEntity.put("fields", new String[]{"clusterName", "clusterSize"});

        Map mapChildren = new LinkedHashMap();
        mapEntity.put("children", mapChildren);

        Map mapMembers = new LinkedHashMap();
        mapMembers.put("links", new String[]{});
        mapMembers.put("fields", new String[]{NODE_ID, "memberName"});
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

        for (Map mapMember : listMembers)
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
        mapMembers.put("fields", new String[]{NAME, "type"});
        mapChildren.put(SERVICES, mapMembers);

        WebTarget target   = getBaseTarget().path("search");
        Entity    entyty   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        Response  response = getBaseTarget().path("search")
                            .request().post(entyty);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response, entyty);
        assertThat(mapResponse.size(), is(1));

        Map membersResponseMap = (Map) mapResponse.get(SERVICES);
        assertThat(membersResponseMap, notNullValue());
        List<Map> listMembers = (List<Map>) membersResponseMap.get("items");
        listMembers.removeIf(serviceMap -> Arrays.stream(TOPICS_SERVICES_LIST).anyMatch(topicServiceName -> ((String) serviceMap.get(NAME)).contains(topicServiceName)));
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), greaterThanOrEqualTo(EXPECTED_SERVICE_COUNT)); // <---- This is SO brittle!!!

        for (Map mapMember : listMembers)
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
                "\"members\":{"  +
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
        listServices.removeIf(serviceMap -> Arrays.stream(TOPICS_SERVICES_LIST).anyMatch(topicServiceName -> ((String) serviceMap.get(NAME)).contains(topicServiceName)));

        assertThat(listServices, notNullValue());
        assertThat(listServices.size(), greaterThanOrEqualTo(EXPECTED_SERVICE_COUNT)); // <---- This is SO brittle!!!

        for (Map mapService : listServices)
            {
            assertThat(mapService.size(), greaterThan(ATTRIBUTES_COUNT));
            // for a wls-management metrics test, we need to enable metrics.  So, exclude it here.
            if (((String) mapService.get(NAME)).compareToIgnoreCase("MetricsHttpProxy") != 0)
                {
                assertThat(mapService.get(NAME), is(oneOf(getQuotedScopedServiceList())));
                }
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

            for (Map memberMap : memberItems)
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
        mapServices.put("fields", new String[]{NAME, "type"});
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
        listMembers.removeIf(serviceMap -> Arrays.stream(TOPICS_SERVICES_LIST).anyMatch(topicServiceName -> ((String) serviceMap.get(NAME)).contains(topicServiceName)));
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), greaterThanOrEqualTo(EXPECTED_SERVICE_COUNT)); // <---- This is SO brittle!!!

        for (Map mapServiceMember : listMembers)
            {
            Object sName = mapServiceMember.get(NAME);
            assertThat(sName, notNullValue());

            Object oType = mapServiceMember.get("type");
            assertThat(oType, is(instanceOf(List.class)));
            String sType = (String) ((List) oType).get(0);

            // no need to test for proxy services
            if ("Proxy".equals(sType) || INVOCATION_SERVICE_TYPE.equals(sType))
                {
                continue;
                }

            Map mapCachesResponse = (Map) mapServiceMember.get(CACHES);
            assertThat("Failed for: " + sName, mapCachesResponse, notNullValue());

            List<Map> listCacheItems = (List<Map>) mapCachesResponse.get("items");
            assertThat(listCacheItems, notNullValue());

            for (Map mapMember : listCacheItems)
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
        mapServices.put("fields", new String[]{NAME, "type"});

        mapEntity.put("children", new LinkedHashMap()
            {{
            put(SERVICES, mapServices);
            }});

        Map mapCaches = new LinkedHashMap();
        mapCaches.put("links", new String[]{});
        mapCaches.put("fields", new String[]{NAME});

        mapServices.put("children", new LinkedHashMap()
            {{
            put(CACHES, mapCaches);
            }});

        Map cachesMembersMap = new LinkedHashMap();
        cachesMembersMap.put("links", new String[]{});
        cachesMembersMap.put("fields", new String[]{NAME, "size"});

        mapCaches.put("children", new LinkedHashMap()
            {{
            put("members", cachesMembersMap);
            }});

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
        listMembers.removeIf(serviceMap -> Arrays.stream(TOPICS_SERVICES_LIST).anyMatch(topicServiceName -> ((String) serviceMap.get(NAME)).contains(topicServiceName)));
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), greaterThanOrEqualTo(EXPECTED_SERVICE_COUNT)); // <---- This is SO brittle!!!

        for (Map mapService : listMembers)
            {
            Object oName = mapService.get(NAME);
            assertThat(oName, notNullValue());

            Object oType = mapService.get("type");
            assertThat(oType, is(instanceOf(List.class)));
            String sType = (String) ((List) oType).get(0);

            // no need to test for proxy services
            if ("Proxy".equals(sType) || getQuotedScopedServiceName(ACTIVE_SERVICE).equals(oName)
                || INVOCATION_SERVICE_TYPE.equals(sType))
                {
                continue;
                }

            Map cachesResponseMap = (Map) mapService.get(CACHES);
            assertThat("Failed for: " + oName, cachesResponseMap, notNullValue());

            List<Map> listCacheItems = (List<Map>) cachesResponseMap.get("items");
            assertThat(listCacheItems, notNullValue());

            for (Map mapMember : listCacheItems)
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
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        // the following should fail with BAD REQUESTS

        // this service does not have an archiver
        Response response = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path(PERSISTENCE)
                                           .path("archiveStores")
                                           .path("my-snapshot")
                                           .request().get();
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        response = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path(PERSISTENCE)
                                  .path(ARCHIVES)
                                  .request().get();
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        // try to delete a snapshot that doesn't exist
        response = getBaseTarget().path(SERVICES).path(getScopedServiceName(ACTIVE_SERVICE)).path(PERSISTENCE).path(SNAPSHOTS).path("snapshot-that-doesnt-exist")
                .request().delete();
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        // try to recover a snapshot that doesn't exist
        response = getBaseTarget().path(SERVICES).path(getScopedServiceName(ACTIVE_SERVICE)).path(PERSISTENCE).path(SNAPSHOTS).path("2-entries").path("snapshot-that-doesnt-exist")
                .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        // try to archive a snapshot that doesn't exist
        response = getBaseTarget().path(SERVICES).path(getScopedServiceName(ACTIVE_SERVICE)).path(PERSISTENCE).path(ARCHIVES).path("snapshot-that-doesnt-exist")
                    .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        // try to delete an archived snapshot that doesn't exist
        response = getBaseTarget().path(SERVICES).path(getScopedServiceName(ACTIVE_SERVICE)).path(PERSISTENCE).path(ARCHIVES).path("snapshot-that-doesnt-exist")
                .request().delete();
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        }

    @Test
    public void testPersistence()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());

        String sCacheName = PERSISTENCE_CACHE_NAME;

        f_inClusterInvoker.accept(f_sClusterName, null, () ->
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
            f_inClusterInvoker.accept(f_sClusterName, null, () ->
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
            Response response = getBaseTarget().path(SERVICES).path(getScopedServiceName(ACTIVE_SERVICE)).path(PERSISTENCE).path(ARCHIVES).path("2-entries")
                    .request().post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            response.close();
            ensureServiceStatusIdle();

            Eventually.assertDeferred(() -> assertSnapshotExists("2-entries", ARCHIVES), is(true));

            // remove the local snapshot
            deleteSnapshot("2-entries");

            // retrieve the archived snapshot
            response = getBaseTarget().path(SERVICES).path(getScopedServiceName(ACTIVE_SERVICE)).path(PERSISTENCE).path(ARCHIVES).path("2-entries").path("retrieve")
                    .request().post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            response.close();
            ensureServiceStatusIdle();

            // check the existence of the local snapshot but delay as a single member could have the snapshot but it not be complete
            Eventually.assertDeferred(() -> assertSnapshotExists("2-entries", SNAPSHOTS), is(true));

            // delete the archived snapshot
            response = getBaseTarget().path(SERVICES).path(getScopedServiceName(ACTIVE_SERVICE)).path(PERSISTENCE).path(ARCHIVES).path("2-entries")
                    .request().delete();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            response.close();
            ensureServiceStatusIdle();

            Eventually.assertDeferred(() -> assertSnapshotExists("2-entries", ARCHIVES), is(false));

            // now we have local snapshot, clear the cache and then recover the snapshot
            f_inClusterInvoker.accept(f_sClusterName, null, () ->
                {
                NamedCache cache = CacheFactory.getCache(sCacheName);
                cache.clear();
                assertThat(cache.size(), is(0));
                return null;
                });

            response = getBaseTarget().path(SERVICES).path(getScopedServiceName(ACTIVE_SERVICE)).path(PERSISTENCE).path(SNAPSHOTS).path("2-entries").path("recover")
                    .request().post(null);
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
            response.close();
            ensureServiceStatusIdle();

            f_inClusterInvoker.accept(f_sClusterName, null, () ->
                {
                NamedCache cache = CacheFactory.getCache(sCacheName);
                Eventually.assertDeferred(cache::size, is(2));
                return null;
                });

            // now delete the 2 snapshots

            deleteSnapshot("2-entries");
            deleteSnapshot("empty");
            }
        finally
            {
            f_inClusterInvoker.accept(f_sClusterName, null, () ->
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

        for (Map<String, String> link : (List<Map>) objListLinks)
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
    public void testHealthChecks() 
        {
        // skipped in security manager tests
        Assume.assumeThat(System.getSecurityManager(), is(nullValue()));
        Assume.assumeThat(System.getProperties().containsKey("java.security.manager"), is(false));

        // ensure the cluster is ready before this test starts so that
        // all health checks should be stable
        assertClusterReady(s_cluster);

        WebTarget           target      = getBaseTarget().path(HEALTH);
        Response            response    = target.request().get();
        Map<String, Object> mapResponse = readEntity(target, response);

        assertThat(mapResponse, is(notNullValue()));

        List<Map<String, Object>>        list = (List<Map<String, Object>>) mapResponse.get("items");
        Map<String, Map<String, Object>> map  = new HashMap<>();

        for (Map<String, Object> m : list)
            {
            List<String> listName = (List<String>) m.get(NAME);
            assertThat(listName, is(notNullValue()));
            assertThat(listName.isEmpty(), is(false));
            map.put(listName.get(0), m);
            }

        for (String sService : SERVICES_LIST)
            {
            System.err.println("In testHealthChecks() - service=" + sService);

            Map<String, Object> mapHealth = map.get(getScopedServiceName(sService));
            assertThat(mapHealth, is(notNullValue()));

            List<Map<String, Object>> listLink = (List<Map<String, Object>>) mapHealth.get("links");
            String                    sLink    = listLink.stream()
                                                         .filter(m -> "self".equals(m.get("rel")))
                                                         .map(m -> String.valueOf(m.get("href")))
                                                         .findFirst()
                                                         .orElse(null);

            assertThat(sLink, is(notNullValue()));

            Response            responseHealth    = m_client.target(sLink).request().get();
            Map<String, Object> mapResponseHealth = readEntity(target, responseHealth);

            assertThat(mapResponseHealth, is(notNullValue()));

            List<String> listName = (List<String>) mapResponseHealth.get("name");
            assertThat(listName, is(notNullValue()));
            assertThat(listName.isEmpty(), is(false));

            String              sName     = listName.get(0);
            Map<String, Object> mapParent = map.get(sName);
            for (Map.Entry<String, Object> entry : mapParent.entrySet())
                {
                String sKey = entry.getKey();
                if (MBeanResponse.PROP_LINKS.equals(sKey))
                    {
                    continue;
                    }
                String sMsg = "Failed for key: " + sKey + " Service=" + sService + " response=" + mapResponseHealth
                        + " expected=" + mapParent;
                assertThat(sMsg, mapResponseHealth.get(sKey), is(entry.getValue()));
                }
            }
        }

    @Test
    public void testViews()
        {
        WebTarget target   = getBaseTarget().path(VIEWS);
        Response  response = target.path("view-cache").request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        Map mapResponse = readEntity(target, response);

        String sMembersUrl = getLink(mapResponse, "members");
        target   = m_client.target(sMembersUrl);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Map mapCacheMembers = readEntity(target, response);
        assertThat(mapCacheMembers, notNullValue());

        List<Map> listCacheMembers = (List<Map>) mapCacheMembers.get("items");
        assertThat(listCacheMembers.size(), is(1));
        assertThat(listCacheMembers, notNullValue());

        for (Map mapCacheMember : listCacheMembers)
            {
            assertThat(mapCacheMember.get(NAME), is("view-cache"));
            assertThat(mapCacheMember.get("type"), is("View"));
            assertThat(mapCacheMember.get("readOnly"), instanceOf(Boolean.class));
            assertThat(mapCacheMember.containsKey("transformer"), is(true));
            assertThat(mapCacheMember.get("filter"), is(AlwaysFilter.INSTANCE().toString()));
            }
        }

    @Test
    public void testReadOnlyManagementReturnsUnauthorized()
        {
        // only run when read-only management is enabled
        Assume.assumeTrue(isReadOnly());

        String sMember   = SERVER_PREFIX + "-1";
        Map    mapEntity = new LinkedHashMap();
        String sService  = "DistributedCachePersistence";

        // try to create snapshot
        Response response = getBaseTarget().path(SERVICES).path(sService).path(PERSISTENCE).path(SNAPSHOTS).path("test")
                            .request().post(null);
        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try to delete snapshot
        response = getBaseTarget().path(SERVICES).path(sService).path(PERSISTENCE).path(SNAPSHOTS).path("test")
                            .request().delete();
        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try to recover snapshot
        response = getBaseTarget().path(SERVICES).path(sService).path(PERSISTENCE).path(SNAPSHOTS).path("test").path("recover")
                            .request().post(null);
        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try to start the reporter
        response = getBaseTarget().path(REPORTERS).path(sMember).path("start").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try stop the reporter
        response = getBaseTarget().path(REPORTERS).path(sMember).path("stop").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try to dumpClusterHeap
        response = getBaseTarget().path("dumpClusterHeap").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try to configureTracing
        mapEntity.clear();
        mapEntity.put(ROLE, "");
        mapEntity.put(TRACING_RATIO, 1.0f);
        response = getBaseTarget().path("configureTracing").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try to log cluster state
        response = getBaseTarget().path("logClusterState").request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);
        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        mapEntity.clear();
        // try to shutdown member
        response = getBaseTarget().path(MEMBERS).path(sMember).path("shutdown").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try and update management
        mapEntity.clear();
        mapEntity.put("expiryDelay", 2000L);
        mapEntity.put("refreshPolicy", "refresh-behind");
        WebTarget target   = getBaseTarget().path(MANAGEMENT);
        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        response = target.request(MediaType.APPLICATION_JSON_TYPE).post(entity);
        MatcherAssert.assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        
        // try and suspend a service
        response = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("suspend")
                .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try and resume a service
        response = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path("resume")
                .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try to start the reporter
        response = getBaseTarget().path(REPORTERS).path(sMember).path("start").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try to stop the reporter
        response = getBaseTarget().path(REPORTERS).path(sMember).path("stop").request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        response = getBaseTarget().path("members").path(SERVER_PREFIX + "-1").path("state").request().get();
        assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try heap dump
        response = getBaseTarget().path(DUMP_CLUSTER_HEAP).request(MediaType.APPLICATION_JSON_TYPE).post(null);
        assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try heap dump with role
        mapEntity.clear();
        mapEntity.put(ROLE, "storage");
        response = getBaseTarget().path("dumpClusterHeap").request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try to modify service attribute
        mapEntity.clear();
        mapEntity.put("threadCountMin", 5);
        response = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME))
                  .request().post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));

        // try to modify cache attribute
        mapEntity.clear();
        mapEntity.put("highUnits",   100005);
        response = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path(CACHES).path(CACHE_NAME)
                                        .request().post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        }

    @Test
    public void testReadOnlyManagementJFR()
            throws UnsupportedEncodingException
        {
        // only run when read-only management is enabled
        Assume.assumeTrue(isReadOnly());

        // This test requires Flight Recorder and only runs on Oracle JVMs
        CheckJDK.assumeOracleJDK();

        Response response = getBaseTarget().path(DIAGNOSTIC_CMD)
                .path("jfrStart")
                .queryParam(OPTIONS, encodeValue("name=all"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);
        assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        }

    @Test
    @Ignore("re-enable when topic MBeans are enabled")
    public void testTopicSubscriberConnect()
        {
        // topics
        WebTarget target  = getBaseTarget().path("topics");
        Response response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        Map<String, Object> mapResponse = readEntity(target, response);
        assertThat(mapResponse, notNullValue());

        List<Map> listTopics = (List<Map>) mapResponse.get("items");
        assertThat(listTopics, notNullValue());
        assertThat(listTopics.size(), is(2));

        List members = List.of("1", "2");
        assertThat(listTopics, Matchers.hasItem(Matchers.<Map<String, Object>>allOf(
                hasEntry("name", TOPIC_NAME + "A"),
                hasEntry("type", "PagedTopic"))));
        assertTrue(listTopics.stream().anyMatch(t -> members.equals(t.get("nodeId"))));

        assertThat(listTopics, Matchers.hasItem(Matchers.<Map<String, Object>>allOf(
                hasEntry("name", TOPIC_NAME + "B"),
                hasEntry("type", "PagedTopic"))));
        assertTrue(listTopics.stream().anyMatch(t -> members.equals(t.get("nodeId"))));

        Map<String, Object> mapTopic = listTopics.stream()
                .filter(t -> t.get("name").equals(TOPIC_NAME + "A"))
                .findFirst()
                .get();
        Object oListLinks = mapTopic.get("links");
        assertThat(oListLinks, instanceOf(List.class));

        List<Map> listLinks = (List) oListLinks;

        mapTopic = listLinks.stream()
                .filter(m -> m.get("rel").equals("canonical"))
                .findFirst()
                .get();
        String sTopicUrl = (String) mapTopic.get("href");

        // topic-A
        target = m_client.target(sTopicUrl);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        mapTopic = readEntity(target, response);
        assertThat(mapTopic, notNullValue());
        assertThat(mapTopic.get("type"), is("PagedTopic"));
        assertThat(mapTopic.get("name"), is(listTopics.get(0).get("name")));
        assertThat(mapTopic.get("retainConsumed"), equalTo(List.of(false, false)));
        assertThat(mapTopic.get("elementCalculator"), equalTo(List.of("BinaryElementCalculator", "BinaryElementCalculator")));

        // topic-A channels
        String sTopicChannelsUrl = ((List<Map<String, String>>) (mapTopic.get("links"))).stream()
                .filter(m -> m.get("rel").equals("channels"))
                .findFirst()
                .get().get("href");

        target = m_client.target(sTopicChannelsUrl);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        mapResponse = readEntity(target, response);
        List<Map<String, Object>> items = (List<Map<String, Object>>) mapResponse.get("items");
        assertThat(items, notNullValue());
        assertThat(items.size(), is(2));

        assertThat(items, CoreMatchers.<Map<String, Object>>hasItem(hasEntry("nodeId", "1")));
        assertThat(items, CoreMatchers.<Map<String, Object>>hasItem(hasEntry("nodeId", "2")));
        assertThat(items, Matchers.everyItem(hasKey("channels")));

        assertEquals(17, ((Map)items.get(0).get("channels")).size());
        assertEquals(17, ((Map)items.get(1).get("channels")).size());
        List<Map<String, Object>> channels = items.stream()
                .flatMap(item -> ((Map<String, Map<String, Object>>) item.get("channels")).values().stream())
                .collect(Collectors.toList());
        assertNotNull(channels.stream().filter(stringObjectMap ->
                                                   {
                                                   Object publishedCount = stringObjectMap.get("PublishedCount");
                                                   if (publishedCount instanceof Long)
                                                       {
                                                       return publishedCount.equals(3L);
                                                       }
                                                   else if (publishedCount instanceof Integer)
                                                       {
                                                       return publishedCount.equals(3);
                                                       }
                                                   return false;
                                                   }
        ).findFirst().orElse(null));

        assertThat(channels, everyItem(allOf(
                hasKey("Channel"),
                //hasKey("PublishedCount"),
                hasKey("PublishedFifteenMinuteRate"),
                hasKey("PublishedFiveMinuteRate"),
                hasKey("PublishedMeanRate"),
                hasKey("PublishedOneMinuteRate"),
                hasKey("Tail"))));

        // topic-A subscribers
        String sTopicSubscribersUrl = ((List<Map<String, String>>) (mapTopic.get("links"))).stream()
                .filter(m -> m.get("rel").equals("subscribers"))
                .findFirst()
                .get().get("href");
        target = m_client.target(sTopicSubscribersUrl);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        mapResponse = readEntity(target, response);
        items = (List<Map<String, Object>>) mapResponse.get("items");
        assertThat(items, notNullValue());
        assertThat(items.size(), is(2));
        Map<String, Object> item = items.stream()
                .filter(s -> s.get("subscriberGroup").equals(SUBSCRIBER_GROUP_NAME + "A"))
                .findFirst()
                .get();
        assertThat(item, hasEntry(equalTo("channelCount"), new IsEqualNumber(17)));
        assertThat(item, hasEntry("subscriberGroup", SUBSCRIBER_GROUP_NAME + "A"));
        assertThat(item, hasEntry("converter", "n/a"));
        assertThat((String) (item.get("service")), containsString("TestTopicService"));
        assertThat(item, hasEntry("type", "PagedTopicSubscriber"));
        assertThat(item, hasEntry("topic", TOPIC_NAME + "A"));
        assertThat(item, allOf(hasKey("channels"),
                               hasKey("stateName"),
                               hasKey("polls"),
                               hasKey("id"),
                               hasKey("member"),
                               hasKey("notifications")));

        // topic subscriber groups (we only have MBeans for durable groups)
        String sTopicSubscriberGroupsUrl = ((List<Map<String, String>>) (mapTopic.get("links"))).stream()
                .filter(m -> m.get("rel").equals("subscriberGroups"))
                .findFirst()
                .get().get("href");
        target = m_client.target(sTopicSubscriberGroupsUrl);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        mapResponse = readEntity(target, response);
        items = (List<Map<String, Object>>) mapResponse.get("items");
        assertThat(items, notNullValue());
        assertThat(items.size(), is(2));
        item = items.stream()
                .filter(sg -> sg.get("name").equals(SUBSCRIBER_GROUP_NAME + "A")
                              && sg.get("nodeId").equals("1"))
                .findFirst()
                .get();

        assertThat(item, hasEntry(equalTo("channelCount"), new IsEqualNumber(17)));
        assertThat(item, hasEntry("filter", "n/a"));
        assertThat(item, hasEntry("transformer", "n/a"));
        assertThat(item, hasEntry("name", SUBSCRIBER_GROUP_NAME + "A"));
        assertThat((String) (item.get("service")), containsString("TestTopicService"));
        assertThat(item, hasEntry("type", "PagedTopicSubscriberGroup"));
        assertThat(item, hasEntry("topic", TOPIC_NAME + "A"));
        assertThat(item, allOf(hasKey("channels"),
                               hasKey("filter"),
                               hasKey("transformer")));

        String subscriberGroupUrl = sTopicSubscriberGroupsUrl + "/" + SUBSCRIBER_GROUP_NAME + "A";
        target = m_client.target(subscriberGroupUrl);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        String subscriberGroupMemberUrl = sTopicSubscriberGroupsUrl + "/" + SUBSCRIBER_GROUP_NAME + "A/1";
        target = m_client.target(subscriberGroupMemberUrl);
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    @Test
    public void testClearCache()
        {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());
        final String CACHE_NAME = CLEAR_CACHE_NAME;

        f_inClusterInvoker.accept(f_sClusterName, null, () ->
            {
            // fill a cache
            NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
            Binary     binValue = Randoms.getRandomBinary(1024, 1024);
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
        f_inClusterInvoker.accept(f_sClusterName, null, () ->
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
        f_inClusterInvoker.accept(f_sClusterName, null, () ->
            {
            NamedCache cache = CacheFactory.getCache(CACHE_NAME);
            cache.clear();
            cache.put("key", "value");
            return null;
            });
        Base.sleep(REMOTE_MODEL_PAUSE_DURATION);

        Eventually.assertDeferred(
                () -> getBaseTarget().path(STORAGE).path(CACHE_NAME).path(CLEAR).request().post(null).getStatus(),
                is(Response.Status.UNAUTHORIZED.getStatusCode()));
        f_inClusterInvoker.accept(f_sClusterName, null, () ->
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

        f_inClusterInvoker.accept(f_sClusterName, null, () ->
            {
            // fill a cache
            NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
            Binary     binValue = Randoms.getRandomBinary(1024, 1024);
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
        f_inClusterInvoker.accept(f_sClusterName, null, () ->
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
        f_inClusterInvoker.accept(f_sClusterName, null, () ->
            {
            NamedCache cache = CacheFactory.getCache(CACHE_NAME);
            cache.clear();
            cache.put("key", "value");
            return null;
            });
        Base.sleep(REMOTE_MODEL_PAUSE_DURATION);

        Eventually.assertDeferred(
                () -> getBaseTarget().path(STORAGE).path(CACHE_NAME).path(TRUNCATE).request().post(null).getStatus(),
                is(Response.Status.UNAUTHORIZED.getStatusCode()));
        f_inClusterInvoker.accept(f_sClusterName, null, () ->
            {
            NamedCache cache = CacheFactory.getCache(CACHE_NAME);
            Eventually.assertDeferred(cache::size, is(1));
            return null;
            });
        }

    @Test
    public void testGetClusterConfig()
            throws Exception
        {
        Eventually.assertDeferred(
                () -> getBaseTarget().path(ClusterResource.GET_CLUSTER_CONFIG).request().get().getStatus(),
                is(Response.Status.OK.getStatusCode()));

        Response response = getBaseTarget().path(ClusterResource.GET_CLUSTER_CONFIG).request().get();
        assertNotNull(response);

        String sResponse = response.readEntity(String.class);
        assertTrue(sResponse.startsWith("<cluster-config"));
        }

    @Test
    public void testReportPartitionStats()
        {
        final String CACHE_NAME = CLEAR_CACHE_NAME;

        f_inClusterInvoker.accept(f_sClusterName, null, () ->
            {
            // fill a cache
            NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
            Binary     binValue = Randoms.getRandomBinary(1024, 1024);
            cache.clear();
            for (int i = 0; i < 10; ++i)
                {
                cache.put(i, binValue);
                }
            return null;
            });
        Base.sleep(REMOTE_MODEL_PAUSE_DURATION);

        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).path(STORAGE)
                            .path(CLEAR_CACHE_NAME).path("reportPartitionStats");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));
        Map mapResponse = readEntity(target, response);

        assertThat(mapResponse, notNullValue());
        List<Map<String, Object>> listPartitionSize = (List<Map<String, Object>>) mapResponse.get("reportPartitionStats");
        assertThat(listPartitionSize, notNullValue());

        for (Map<String, Object> partitionSize : listPartitionSize)
            {
            assertThat(partitionSize.get("partitionId"), is(notNullValue()));
            assertThat(partitionSize.get("memberId"), is(notNullValue()));
            assertThat(partitionSize.get("totalSize"), is(notNullValue()));
            assertThat(partitionSize.get("maxEntrySize"), is(notNullValue()));
            assertThat(partitionSize.get("count"), is(notNullValue()));
            }
        }

    // ----- utility methods----------------------------------------------------

    /**
     * Can be overridden by sub-classes that use scoped service names.
     *
     * @param sName the service name
     *
     * @return the scoped service name
     */
    protected String getScopedServiceName(String sName)
        {
        return sName;
        }

    /**
     * Can be overridden by sub-classes that use scoped service names.
     *
     * @param sName the service name
     *
     * @return the scoped service name
     */
    protected String getQuotedScopedServiceName(String sName)
        {
        return sName;
        }

    protected String[] getQuotedScopedServiceList()
        {
        return Arrays.stream(SERVICES_LIST).map(this::getQuotedScopedServiceName).toArray(String[]::new);
        }

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
        Response response = getBaseTarget().path(SERVICES).path(getScopedServiceName(ACTIVE_SERVICE)).path(PERSISTENCE).path(SNAPSHOTS).path(sSnapshotName)
                .request().delete();

        // allow status code of 400 since if PersistentManagerMBean is not ready, it returns this code AND resubmits request.
        // (for details search for "double delete" in COH-22169)
        assertThat("validate remove snapshot " + sSnapshotName + " request status code is not NOT_FOUND (404)",
                   response.getStatus(), is(oneOf(Response.Status.OK.getStatusCode(),
                                                 Response.Status.BAD_REQUEST.getStatusCode())));

        response.close();
        ensureServiceStatusIdle();

        // ensure post-condition is met
        Eventually.assertDeferred(() -> assertSnapshotExists(sSnapshotName, SNAPSHOTS), is(false));
        }

    /**
     * Create the given snapshot.
     *
     * @param sSnapshotName snapshot to create
     */
    private void createSnapshot(String sSnapshotName)
        {
        Response response = getBaseTarget().path(SERVICES).path(getScopedServiceName(ACTIVE_SERVICE)).path(PERSISTENCE).path(SNAPSHOTS).path(sSnapshotName)
                            .request().post(null);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        response.close();
        }

    /**
     * Assert that the persistence status is idle.
     */
    public boolean assertServiceIdle()
        {
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(ACTIVE_SERVICE)).path(PERSISTENCE).queryParam("fields", "operationStatus");
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
        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(ACTIVE_SERVICE)).path(PERSISTENCE).path(sType);
        Response  response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        Map mapResponse = readEntity(target, response);
        assertThat(mapResponse, is(notNullValue()));

        List<String> result = (List) mapResponse.get(sType);
        assertThat(result, is(notNullValue()));

        return new HashSet<>(result);
        }

    /**
     * Return the response from an MBean instance of given path.
     *
     * @param sMember  member id or memberName
     * @param sPath    the path of an MBean instance
     *
     * @return the {@link Map} respone
     */
    public Map getMBeanInfoResponse(String sMember, String sPath)
        {
        WebTarget target   = getBaseTarget().path(sPath).path(sMember);
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

    private void setMembersReporterAttribute(String sAttribute, Object value)
        {
        Map mapEntity = new LinkedHashMap();
        mapEntity.put(sAttribute, value);

        WebTarget target   = getBaseTarget().path(REPORTERS);
        Response  response = target.request().post(Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        }

    public boolean assertAttribute(String sMember, String sPath, String sAttribute, Object value)
        {
        Map    mapResults = getMBeanInfoResponse(sMember, sPath);
        Object result     = mapResults.get(sAttribute);

        if (result == null)
            {
            List<Map> items = (List<Map>) mapResults.get("items");

            if (items != null && items.size() == 1)
                {
                result = items.get(0).get(sAttribute);
                }
            }
        assertThat(result, is(notNullValue()));
        if (result instanceof String)
            {
            return (value).equals(result);
            }
        else if (result instanceof Integer)
            {
            return ((Integer) result).intValue() == ((Integer) value).intValue();
            }
        else if (result instanceof Long)
            {
            return ((Number) result).longValue() == ((Number)value).longValue();
            }
        else if (result instanceof Float)
            {
            return Float.compare((Float) result, (Float) value) == 0;
            }
        else if (result instanceof Boolean)
            {
            return Boolean.compare((boolean) value, (boolean) result) == 0;
            }
        throw new IllegalArgumentException("Type of " + value.getClass() + " not supported");
        }

    public boolean assertAttribute(String sMember, String sPath, String sAttribute, Object[] values)
        {
        for (Object value : values)
            {
            if (assertAttribute(sMember, sPath, sAttribute, value))
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

        Map mapCacheMembers = readEntity(memTarget, response);
        assertThat(mapCacheMembers, notNullValue());

        List<Map> listCacheMembers = (List<Map>) mapCacheMembers.get("items");
        assertThat(listCacheMembers, notNullValue());

        for (Map mapCacheMember : listCacheMembers)
            {
            assertThat(mapCacheMember.get("tier"), is("back"));
            assertThat(mapCacheMember.get(NAME), is(CACHE_NAME));
            assertThat(mapCacheMember.get("size"), instanceOf(Number.class));
            assertThat(mapCacheMember.get("cacheHits"), instanceOf(Number.class));

            String sSelfUrl = getSelfLink(mapCacheMember);

            assertThat(sSelfUrl, is(oneOf(sMembersUrl + "/" + mapCacheMember.get(NODE_ID),
                    sMembersUrl + "/" + mapCacheMember.get(MEMBER))));

            assertThat(mapCacheMember.get("listenerFilterCount"), instanceOf(Number.class));
            assertThat(mapCacheMember.get("listenerKeyCount"), instanceOf(Number.class));

            memTarget = m_client.target(sSelfUrl);
            response  = memTarget.request().get();
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            mapResponse = readEntity(memTarget, response);

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
        listCacheMaps.removeIf(cacheMap -> Arrays.stream(TOPIC_CACHES_LIST).anyMatch(topicCacheName -> topicCacheName.equals(cacheMap.get(NAME))));
        assertThat(listCacheMaps.size(), greaterThan(1));

        for (Map mapCache : listCacheMaps)
            {
            String sCacheName = (String) mapCache.get(NAME);
            assertThat(mapCache.get(NAME), is(oneOf(CACHES_LIST)));

            if (!sCacheName.equals(PERSISTENCE_CACHE_NAME) && !sCacheName.equals(CLEAR_CACHE_NAME))
                {
                Object size = mapCache.get("size");
                assertThat(sCacheName, size, is(instanceOf(Number.class)));
                assertThat(sCacheName, ((Number) size).intValue(), greaterThan(0));
                }

            assertThat(SERVICE, mapCache.get(SERVICE), is(oneOf(getQuotedScopedServiceList())));
            Assert.assertNotNull(NODE_ID, mapCache.get(NODE_ID));
            }

        WebTarget cachesTarget   = getBaseTarget().path(CACHES).queryParam("fields", "name,totalPuts");
        Response  cachesResponse = cachesTarget.request().get();
        mapResponse   = readEntity(cachesTarget, cachesResponse);
        listCacheMaps = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());
        listCacheMaps.removeIf(cacheMap -> Arrays.stream(TOPIC_CACHES_LIST).anyMatch(topicCacheName -> topicCacheName.equals(cacheMap.get(NAME))));
        assertThat(listCacheMaps.size(), greaterThan(1));

        for (Map mapCache : listCacheMaps)
            {
            if (!mapCache.get(NAME).equals(PERSISTENCE_CACHE_NAME))
                {
                assertThat("Cache " + mapCache.get(NAME) + " failed assertion of totalPuts greater than 0", ((Number) mapCache.get("totalPuts")).intValue(), greaterThan(0));
                }
            }

        cachesTarget   = getBaseTarget().path(CACHES).queryParam("fields", "name,units");
        cachesResponse = cachesTarget.request().get();
        mapResponse    = readEntity(cachesTarget, cachesResponse);
        listCacheMaps  = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());
        listCacheMaps.removeIf(cacheMap -> Arrays.stream(TOPIC_CACHES_LIST).anyMatch(topicCacheName -> topicCacheName.equals(cacheMap.get(NAME))));

        for (Map mapCache : listCacheMaps)
            {
            assertThat(mapCache.get(NAME), is(oneOf(CACHES_LIST)));
            if (mapCache.get(NAME).equals(CACHE_NAME_FOO))
                {
                Object cUnits = mapCache.get("units");
                assertThat("Cache " + NAME, cUnits, is(instanceOf(Number.class)));
                assertThat("Cache " + NAME, ((Number) cUnits).intValue(), anyOf(is(1), is(20)));
                }
            else
                {
                if (!mapCache.get(NAME).equals(PERSISTENCE_CACHE_NAME) && !mapCache.get(NAME).equals(CLEAR_CACHE_NAME))
                    {
                    assertThat("Cache " + NAME + "assertion", ((Number) mapCache.get("units")).longValue(), is(1L));
                    }
                }
            }

        cachesTarget   = getBaseTarget().path(CACHES).queryParam("fields", "name,insertCount");
        cachesResponse = cachesTarget.request().get();
        mapResponse    = readEntity(cachesTarget, cachesResponse);
        listCacheMaps  = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());
        listCacheMaps.removeIf(cacheMap -> Arrays.stream(TOPIC_CACHES_LIST).anyMatch(topicCacheName -> topicCacheName.equals(cacheMap.get(NAME))));

        for (Map mapCache : listCacheMaps)
            {
            assertThat(mapCache.get(NAME), is(oneOf(CACHES_LIST)));
            if (!mapCache.get(NAME).equals(PERSISTENCE_CACHE_NAME))
                {
                assertThat(((Number) mapCache.get("insertCount")).intValue(), greaterThan(0));
                }
            }

        cachesTarget   = getBaseTarget().path(CACHES).queryParam("fields", SERVICE);
        cachesResponse = cachesTarget.request().get();
        mapResponse    = readEntity(cachesTarget, cachesResponse);
        listCacheMaps  = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());
        listCacheMaps.removeIf(cacheMap -> Arrays.stream(TOPICS_SERVICES_LIST).anyMatch(serviceName -> ((String) cacheMap.get(SERVICE)).contains(serviceName)));

        for (Map mapCache : listCacheMaps)
            {
            assertNull(mapCache.get(NAME));
            assertThat(mapCache.get(SERVICE), is(oneOf(getQuotedScopedServiceList())));
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
        listCacheMaps.removeIf(cacheMap -> Arrays.stream(TOPIC_CACHES_LIST).anyMatch(topicCacheName -> topicCacheName.equals(cacheMap.get(NAME))));

        for (Map mapCache : listCacheMaps)
            {
            String sName = (String) mapCache.get(NAME);
            assertThat(mapCache.get(NAME), is(oneOf(CACHES_LIST)));

            if (sName.equals(sCacheName))
                {
                Object size = mapCache.get("size");
                assertThat(sCacheName, size, is(instanceOf(Number.class)));
                assertThat("Validating size of Cache: " + sCacheName, ((Number) size).intValue(), greaterThan(0));
                }

            assertThat(SERVICE, mapCache.get(SERVICE), is(oneOf(getQuotedScopedServiceList())));
            Assert.assertNotNull(NODE_ID, mapCache.get(NODE_ID));
            }

        WebTarget cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", "name,totalPuts");
        Response cachesResponse = cachesTarget.request().get();
        mapResponse = readEntity(cachesTarget, cachesResponse);
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
        mapResponse = readEntity(cachesTarget, cachesResponse);
        listCacheMaps = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());
        listCacheMaps.removeIf(cacheMap -> Arrays.stream(TOPIC_CACHES_LIST).anyMatch(topicCacheName -> topicCacheName.equals(cacheMap.get(NAME))));

        for (Map mapCache : listCacheMaps)
            {
            assertThat(mapCache.get(NAME), is(oneOf(CACHES_LIST)));
            if (mapCache.get(NAME).equals(sCacheName))
                {
                Object cUnits = mapCache.get("units");
                assertThat("Cache " + sCacheName, cUnits, is(instanceOf(Number.class)));
                assertThat("Cache " + sCacheName, ((Number) cUnits).intValue(), anyOf(is(1), is(20)));
                }
            }

        cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", "name,insertCount");
        cachesResponse = cachesTarget.request().get();
        mapResponse = readEntity(cachesTarget, cachesResponse);
        listCacheMaps = (List<Map>) mapResponse.get("items");
        assertThat(listCacheMaps, notNullValue());

        cachesTarget = getBaseTarget().path(CACHES).queryParam("fields", SERVICE);
        cachesResponse = cachesTarget.request().get();
        mapResponse = readEntity(cachesTarget, cachesResponse);
        listCacheMaps = (List<Map>) mapResponse.get("items");
        listCacheMaps.removeIf(cacheMap -> Arrays.stream(TOPICS_SERVICES_LIST).anyMatch(topicServiceName -> ((String) cacheMap.get(SERVICE)).contains(topicServiceName)));

        assertThat(listCacheMaps, notNullValue());

        for (Map mapCache : listCacheMaps)
            {
            assertNull(mapCache.get(NAME));
            assertThat(mapCache.get(SERVICE), is(oneOf(getQuotedScopedServiceList())));
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
                String          sPort  = s_cluster.getAny().getSystemProperties().getProperty("test.multicast.port", "7778");
                int             nPort  = Integer.parseInt(sPort);
                Collection<URL> colURL = NSLookup.lookupHTTPManagementURL(f_sClusterName, new InetSocketAddress("127.0.0.1", nPort));

                assertThat(colURL.isEmpty(), is(false));

                m_baseURI = colURL.iterator().next().toURI();

                Logger.info("Management HTTP Acceptor lookup returned: " + m_baseURI);
                }
            return client.target(m_baseURI);
            }
        catch (IOException | URISyntaxException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    public Object getAttributeValue(Client client, String sService, String sAttributeName)
        {
        WebTarget target   = getBaseTarget(client).path(SERVICES).path(sService).path("members").path(SERVER_PREFIX + "-1");
        Response  response = target.request().get();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        Map mapResponse = readEntity(target, response);
        return mapResponse.get(sAttributeName);
        }

    private void assertLongSummaryStats(Object oLongSummary, long... values)
        {
        LongSummaryStatistics stats = new LongSummaryStatistics();
        Arrays.stream(values).forEach(stats);
        assertThat(oLongSummary, instanceOf(Map.class));
        Map maLongSummary = (Map) oLongSummary;
        assertThat(Long.parseLong(maLongSummary.get("count").toString()), is(stats.getCount()));
        assertThat(Long.parseLong(maLongSummary.get("max").toString()), is(stats.getMax()));
        assertThat(Long.parseLong(maLongSummary.get("min").toString()), is(stats.getMin()));
        assertThat(Long.parseLong(maLongSummary.get("sum").toString()), is(stats.getSum()));
        assertThat(Double.parseDouble(maLongSummary.get("average").toString()), is(stats.getAverage()));
        }

    /**
     * Assert that the response does not conain any "messages" elements, which
     * would indicate errors.
     *
     * @param response Response
     */
    private void assertNoMessages(Response response)
        {
        Map       mapResponse  = new LinkedHashMap(response.readEntity(Map.class));
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
    protected String encodeValue(String sValue)
            throws UnsupportedEncodingException
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

        WebTarget target   = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).queryParam("fields", "requestTotalCount");
        Response  response = target.request().get();
        mapResponse = readEntity(target, response);

        assertThat(((Number) mapResponse.get("requestTotalCount")).intValue(), greaterThanOrEqualTo(0));

        target      = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).queryParam("fields", "partitionsAll")
                    .queryParam("role", "*");
        response    = target.request().get();
        mapResponse = readEntity(target, response);

        Collection<Number> colPartCount = (Collection) mapResponse.get("partitionsAll");
        assertEquals(1, colPartCount.size());
        assertThat(colPartCount.iterator().next().longValue(), is(257L));

        target      = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).queryParam("fields", "statusHA")
                    .queryParam("role", "*");
        response    = target.request().get();
        mapResponse = readEntity(target, response);

        assertThat((Collection<String>) mapResponse.get("statusHA"), Matchers.hasItem("NODE-SAFE"));

        target      = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).queryParam("fields", "taskCount")
                    .queryParam("collector", "list")
                    .queryParam("role", "*");
        response    = target.request().get();
        mapResponse = readEntity(target, response);
        // test specifying a custom collector
        assertThat(((Collection) mapResponse.get("taskCount")).size(), greaterThan(1));


        target      = getBaseTarget().path(SERVICES).path(getScopedServiceName(SERVICE_NAME)).queryParam("fields", "taskCount")
                    .queryParam("collector", "list")
                    .queryParam("role", SERVER_PREFIX + "-1");
        response    = target.request().get();
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
        listServiceMaps.removeIf(serviceMap -> Arrays.stream(TOPICS_SERVICES_LIST).anyMatch(topicServiceName -> ((String) serviceMap.get(NAME)).contains(topicServiceName)));
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

        target          = getBaseTarget().path(SERVICES).queryParam("fields", "taskCount")
                        .queryParam("collector", "list")
                        .queryParam("role", "*");
        response        = target.request().get();
        mapResponse     = readEntity(target, response);
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
        assertThat(cServices, greaterThanOrEqualTo(2));
        }

    protected Map readEntity(WebTarget target, Response response)
        {
        return readEntity(target, response, null);
        }

    protected Map readEntity(WebTarget target, Response response, Entity entity)
        {
        int cAttempt    = 0;
        int cMaxAttempt = 2;

        while (true)
            {
            String sJson = null;
            try
                {
                InputStream inputStream = response.readEntity(InputStream.class);
                if ("gzip".equalsIgnoreCase(response.getHeaderString("Content-Encoding")))
                    {
                    inputStream = new GZIPInputStream(inputStream);
                    }
                try
                    {
                    byte[] abData = Reads.read(inputStream);
                    sJson = new String(abData, StandardCharsets.UTF_8);
                    }
                finally
                    {
                    inputStream.close();
                    }

                Map map = f_jsonHandler.readMap(new ByteArrayInputStream(sJson.getBytes(StandardCharsets.UTF_8)));
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
            catch (Throwable e)
                {
                Logger.err(getClass().getName() + ".readEntity() got an error "
                        + " from target " + target, e);

                if (sJson != null)
                    {
                    Logger.err("JSON response that caused error\n" + sJson);
                    }

                if (cAttempt >= cMaxAttempt)
                    {
                    throw Exceptions.ensureRuntimeException(e);
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

            cAttempt++;
            }
        }

    protected List<Map> getMemberList()
        {
        Map mapEntity = new LinkedHashMap();
        mapEntity.put("links", new String[]{});
        mapEntity.put("fields", new String[]{"clusterName", "clusterSize"});

        Map mapChildren = new LinkedHashMap();
        mapEntity.put("children", mapChildren);

        Map mapMembers = new LinkedHashMap();
        mapMembers.put("links", new String[]{});
        mapMembers.put("fields", new String[]{NODE_ID, "memberName"});
        mapChildren.put("members", mapMembers);

        Entity    entity   = Entity.entity(mapEntity, MediaType.APPLICATION_JSON_TYPE);
        WebTarget target   = getBaseTarget().path("search");
        Response  response = target.request().post(entity);

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("X-Content-Type-Options"), is("nosniff"));

        Map mapResponse        = readEntity(target, response, entity);
        Map mapMembersResponse = (Map) mapResponse.get("members");

        assertThat(mapMembersResponse, notNullValue());
        List<Map> listMembers = (List<Map>) mapMembersResponse.get("items");
        assertThat(listMembers, notNullValue());
        assertThat(listMembers.size(), is(greaterThan(1)));
        return listMembers;
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
                                        InClusterInvoker inClusterInvoker,
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
        propsServer1.add(SystemProperty.of("coherence.management.readonly", Boolean.toString(isReadOnly())));
        propsServer1.add(SystemProperty.of("coherence.management.http.override-port", 0));
        propsServer1.add(SystemProperty.of("coherence.management.http.cluster", sClusterName));
        propsServer1.add(SystemProperty.of("coherence.override", "tangosol-coherence-override-mgmt.xml"));
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
        inClusterInvoker.accept(sClusterName, null, BaseManagementInfoResourceTests::popluateCaches);
        inClusterInvoker.accept(sClusterName, SERVER_PREFIX + "-1", BaseManagementInfoResourceTests::populateTopics);
        inClusterInvoker.accept(sClusterName, SERVER_PREFIX + "-2", BaseManagementInfoResourceTests::createTopics);

        m_client = ClientBuilder.newBuilder()
                .register(MapProvider.class)
                .build();
        }

    protected static Void popluateCaches()
        {
        NamedCache cache    = CacheFactory.getCache(CACHE_NAME);
        Binary     binValue = Randoms.getRandomBinary(1024, 1024);

        cache.put(1, binValue);

        // fill front cache
        cache = CacheFactory.getCache(NEAR_CACHE_NAME);
        cache.put(1, binValue);

        // fill persistence cache
        cache = CacheFactory.getCache(PERSISTENCE_CACHE_NAME);
        cache.put(1, binValue);

        // fill clear/truncate cache
        cache = CacheFactory.getCache(CLEAR_CACHE_NAME);
        cache.put(1, binValue);

        cache = CacheFactory.getCache("view-cache");
        cache.put(1, binValue);
        return null;
        }

    protected static Void populateTopics()
            throws InterruptedException, ExecutionException, TimeoutException
        {
        Session session = Session.create();
        NamedTopic<Object> topicA = session.getTopic(TOPIC_NAME + "A");
        NamedTopic<Object> topicB = session.getTopic(TOPIC_NAME + "B");
        topicA.ensureSubscriberGroup(SUBSCRIBER_GROUP_NAME + "A");
        Publisher<Object> publisherA = session.createPublisher(TOPIC_NAME + "A");
        Subscriber<Object> subscriberA1 = topicA.createSubscriber(Subscriber.inGroup(SUBSCRIBER_GROUP_NAME + "A"));
        Subscriber<Object> subscriberA2 = topicA.createSubscriber();

        publisherA.publish("test1");
        publisherA.publish("test2");
        publisherA.publish("test3");
        publisherA.flush().join();
        subscriberA1.receive();
        subscriberA2.receive();
        return null;
        }

    protected static Void createTopics()
            throws InterruptedException, ExecutionException, TimeoutException
        {
        Session session = Session.create();
        session.getTopic(TOPIC_NAME + "A");
        session.getTopic(TOPIC_NAME + "B");
        return null;
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
            Eventually.assertDeferred(() -> member.getServiceStatus(sPrefix + SERVICE_NAME), is(ServiceStatus.NODE_SAFE));
            Eventually.assertDeferred(() -> member.isServiceRunning(sPrefix + ACTIVE_SERVICE), is(true), within(3, TimeUnit.MINUTES));
            Eventually.assertDeferred(() -> member.getServiceStatus(sPrefix + ACTIVE_SERVICE), is(ServiceStatus.NODE_SAFE));
            Eventually.assertDeferred(() -> member.invoke(IsReady.INSTANCE), is(true), within(3, TimeUnit.MINUTES));
            }
        }

    protected void startService(String sName, String sType)
        {
        String                       sScopedName = getScopedServiceName(sName);
        List<CoherenceClusterMember> listMember  = s_cluster.stream().collect(Collectors.toList());
        for(CoherenceClusterMember member :listMember)
            {
            member.submit(new RemoteStartService(sScopedName, sType));
            }
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

    /**
     * A flag to indicate whether invocation service is started.
     */
    protected static boolean s_fInvocationServiceStarted = false;

    /**
     * A flag to indicate if management is to be read-only.
     */
    private static boolean s_fIsReadOnly = false;

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
     * The health path.
     */
    protected static final String HEALTH = "health";

    /**
     * The name of the active persistence service.
     */
    protected static final String ACTIVE_SERVICE = "DistributedCachePersistence";

    /**
     * The name of the used Extend proxy service.
     */
    protected static final String PROXY_SERVICE_NAME = "ExtendProxyService";

    /**
     * The name of the used Http proxy service.
     */
    protected static final String HTTP_SERVICE_NAME = "ManagementHttpProxy";

    /**
     * The name of the used PartitionedService.
     */
    protected static final String SERVICE_NAME = "DistributedCache";

    /**
     * The type of the PartitionedService service.
     */
    protected static final String SERVICE_TYPE = "DistributedCache";

    /**
     * The name of Distributed cache.
     */
    protected static final String CACHE_NAME = "dist-test";

    /**
     * The name of Distributed cache Foo.
     */
    protected static final String CACHE_NAME_FOO = "dist-foo";

    /**
     * The name of persistence enabled cache.
     */
    protected static final String PERSISTENCE_CACHE_NAME = "dist-persistence-test";

    /**
     * The near cache.
     */
    protected static final String NEAR_CACHE_NAME = "near-test";

    /**
     * The clear/truncate cache.
     */
    protected static final String CLEAR_CACHE_NAME = "dist-clear";

    /**
     * The name of the invocation service.
     */
    protected static final String INVOCATION_SERVICE_NAME = "TestInvocationService";

    /**
     * The type of the invocation service.
     */
    protected static final String INVOCATION_SERVICE_TYPE = "Invocation";

    /**
     * The list of services used by this test class.
     */
    private static final String[] SERVICES_LIST = {SERVICE_NAME, PROXY_SERVICE_NAME,
            "DistributedCachePersistence", HttpHelper.getServiceName(), INVOCATION_SERVICE_NAME };

    /**
     * The list of services used by topics.
     */
    private static final String[] TOPICS_SERVICES_LIST = {"TestTopicService"};

    /**
     * The list of caches used by this test class.
     */
    private static final String[] CACHES_LIST = {CACHE_NAME, "near-test", CACHE_NAME_FOO, PERSISTENCE_CACHE_NAME, CLEAR_CACHE_NAME, "view-cache"};

    /**
     * The list of topics caches used by this test class.
     */
    private static final String[] TOPIC_CACHES_LIST = {"$topic$topic-A", "$topic$topic-B"};

    /**
     * Prefix for the spawned processes.
     */
    protected static String SERVER_PREFIX = "testMgmtRESTServer";

    /**
     * Cache config used by the test and spawned processes.
     */
    protected static final String CACHE_CONFIG = "server-cache-config-mgmt.xml";

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
     * May be overriden.
     */
    protected int EXPECTED_SERVICE_COUNT = SERVICES_LIST.length;

    /**
     * Name of the Coherence cluster.
     */
    public static final String CLUSTER_NAME = "mgmtRestCluster";

    /**
     * The name of topic.
     */
    protected static final String TOPIC_NAME = "topic-";

    /**
     * The name of subscriber group.
     */
    protected static final String SUBSCRIBER_GROUP_NAME = "subscriber-group-";

    /**
     * The cluster members.
     */
    protected static CoherenceCluster s_cluster;

    protected final String f_sClusterName;

    @Rule
    public final TestName m_testName = new TestName();

    @ClassRule
    public static final TestLogs m_testLogs = new TestLogs();

    private final InClusterInvoker f_inClusterInvoker;

    private final MapJsonBodyHandler f_jsonHandler = MapJsonBodyHandler.ensureMapJsonBodyHandler();

    interface InClusterInvoker
        {
        void accept(String clusterName, String member, RemoteCallable<Void> callable);
        }

    /**
     * Convenient matcher that compares numbers (integers or longs) while
     * not taking into account their type.
     */
    public static class IsEqualNumber extends TypeSafeDiagnosingMatcher<Object>
        {
        public IsEqualNumber(long number)
            {
            this.expected = new BigDecimal(number);
            }

        public IsEqualNumber(int number)
            {
            this.expected = new BigDecimal(number);
            }

        protected boolean matchesSafely(Object item, Description mismatchDescription)
            {
            mismatchDescription.appendText("was ")
                    .appendValue(item)
                    .appendText(" which does not match ")
                    .appendValue(item);
            if (item instanceof Long)
                {
                return new BigDecimal((Long) item).equals(expected);
                }
            if (item instanceof Integer)
                {
                return new BigDecimal((Integer) item).equals(expected);
                }
            return false;
            }

        public void describeTo(Description description)
            {
            description.appendText("matches number=`" + expected + "`");
            }
        private final BigDecimal expected;
        }
    }
