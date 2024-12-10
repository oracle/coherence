/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.coherence.management.internal.MapProvider;
import com.tangosol.discovery.NSLookup;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.oracle.coherence.testing.AbstractFunctionalTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verify that links are valid http URIs for scoped services,
 * i.e. service names such as "$SYS:Config"
 */
public class ScopedServicesTests
        extends AbstractFunctionalTest
    {

    // ----- junit lifecycle methods ----------------------------------------

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
        propsServer1.setProperty("coherence.override", "tangosol-coherence-override-mgmt.xml");

        CoherenceClusterMember member1 = startCacheServer(SERVER_PREFIX + "-1", "rest",
                CACHE_CONFIG, propsServer1, true, null, Coherence.class);

        assertThat(member1, is(notNullValue()));

        Properties propsServer2 = new Properties();
        propsServer2.putAll(propsServer1);
        propsServer2.setProperty("coherence.member", SERVER_PREFIX + "-2");
        CoherenceClusterMember member2 = startCacheServer(SERVER_PREFIX + "-2", "rest",
                CACHE_CONFIG, propsServer2, true, null, Coherence.class);

        Eventually.assertThat(invoking(member2).getServiceStatus(SERVICE_NAME),
                              is(ServiceStatus.NODE_SAFE));

        m_client = ClientBuilder.newBuilder()
                .register(MapProvider.class)
                .build();
        }

    @AfterClass
    public static void tearDown()
        {
        m_client.close();
        stopCacheServer(SERVER_PREFIX + "-2");
        stopCacheServer(SERVER_PREFIX + "-1");
        }

    // ----- tests ----------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetScopedServices()
        {
        WebTarget target  = getBaseTarget().path("services");
        Response response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

        Map<String, Object> mapServices = readEntity(target, response, null);
        assertThat(mapServices, notNullValue());
        assertThat(mapServices.size(), is(not(0)));

        List<Map<String, Object>> listItems = (List<Map<String, Object>>) mapServices.get("items");

        List<String> listLinks = listItems.stream()
                .map(m -> m.get("links"))
                .filter(Objects::nonNull)
                .flatMap(o -> ((List<Map<String, Object>>) o).stream())
                .filter(m -> "self".equals(m.get("rel")))
                .map(m -> String.valueOf(m.get("href")))
                .collect(Collectors.toList());

        for (String sLink : listLinks)
            {
            Response linkResponse = m_client.target(sLink).request().get();
            assertThat(linkResponse.getStatus(), is(200));
            }
        }

    // ----- utility methods----------------------------------------------------

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Map<String, Object> readEntity(WebTarget target, Response response, Entity entity)
            throws ProcessingException
        {
        int i = 0;
        while (true)
            {
            try
                {
                Map mapReturned = response.readEntity(Map.class);
                if (mapReturned == null)
                    {
                    Logger.log(getClass().getName() + ".readEntity() returned null"
                            + ", target: " + target + ", response: " + response, CacheFactory.LOG_WARN);
                    }
                else
                    {
                    return mapReturned;
                    }
                }
            catch (ProcessingException | IllegalStateException e)
                {
                Logger.log(getClass().getName() + ".readEntity() got an exception: " + e
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
                m_baseURI = NSLookup.lookupHTTPManagementURL(new InetSocketAddress("127.0.0.1", nPort))
                        .iterator().next().toURI();

                CacheFactory.log("Management HTTP Acceptor lookup returned: " + m_baseURI, CacheFactory.LOG_INFO);
                }
            return client.target(m_baseURI);
            }
        catch(IOException | URISyntaxException e)
            {
            throw ensureRuntimeException(e);
            }
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
     * The name of the used PartitionedService.
     */
    protected static final String SERVICE_NAME = "DistributedCache";

    /**
     * Prefix for the spawned processes.
     */
    protected static String SERVER_PREFIX = "ScopedServicesTests";

    /**
     * Cache config used by the test and spawned processes.
     */
    protected static final String CACHE_CONFIG  = "server-cache-config-mgmt.xml";

    /**
     * Name of the Coherence cluster.
     */
    public static final String CLUSTER_NAME = "ScopedServicesTests";
    }
