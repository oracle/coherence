/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package rest;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;

import com.tangosol.coherence.rest.providers.JacksonMapperProvider;

import com.tangosol.coherence.rest.util.JsonMap;

import com.tangosol.discovery.NSLookup;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Binary;

import common.AbstractFunctionalTest;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;

import org.hamcrest.CoreMatchers;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Logger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests persistence related API's in management over REST functionality.
 *
 *  @author sr  2017.09.08
 */
public class PersistenceResourceTests
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
        try
            {
            m_tmpDirectory = Files.createTempDirectory("testPersistence");

            setupProps();
            System.setProperty("coherence.management", "dynamic");
            System.setProperty("coherence.management.http", "inherit");
            System.setProperty("coherence.management.http.port", "0");

            Properties propsServer1 = new Properties();
            propsServer1.setProperty("coherence.cluster", CLUSTER_NAME);
            propsServer1.setProperty("coherence.management.extendedmbeanname", "true");
            propsServer1.setProperty("coherence.member", SERVER_PREFIX + "-1");
            propsServer1.setProperty("test.persistence.active.dir", Paths.get(m_tmpDirectory.toString(), "active").toString());
            propsServer1.setProperty("test.persistence.snapshot.dir", Paths.get(m_tmpDirectory.toString(), "snapshot").toString());
            propsServer1.setProperty("test.persistence.trash.dir", Paths.get(m_tmpDirectory.toString(), "trash").toString());
            propsServer1.setProperty("test.persistence.archive.dir", Paths.get(m_tmpDirectory.toString(), "archive").toString());

            startCacheServer(SERVER_PREFIX + "-1", "rest", CACHE_CONFIG, propsServer1);

            Properties propsServer2 = new Properties();
            propsServer2.putAll(propsServer1);
            propsServer2.setProperty("coherence.member", SERVER_PREFIX + "-2");
            CoherenceClusterMember member = startCacheServer(SERVER_PREFIX + "-2", "rest", CACHE_CONFIG, propsServer2);

            Eventually.assertDeferred(() -> member.getServiceStatus(SERVICE_NAME), is(ServiceStatus.NODE_SAFE));

            // fill a cache
            NamedCache cache = findApplication(SERVER_PREFIX + "-1").getCache(CACHE_NAME);
            Binary binValue = Binary.getRandomBinary(1024, 1024);
            cache.put(1, binValue);

            m_client = ClientBuilder.newBuilder()
                .register(JacksonMapperProvider.class)
                .register(JacksonFeature.class)
                .register(new LoggingFeature(LOGGER, LoggingFeature.Verbosity.PAYLOAD_TEXT)).build();
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }

    @AfterClass
    public static void tearDown()
        {
        m_client.close();
        stopCacheServer(SERVER_PREFIX + "-2");
        stopCacheServer(SERVER_PREFIX + "-1");
        FileHelper.deleteDirSilent(m_tmpDirectory.toFile());
        }

    @Test
    public void testSnapshotAndArchive()
        {
        testSnapshot();
        testSnapshotPresent();
        testArchive();
        testArchivePresent();
        testArchiveStores();
        testDeleteSnapshot();
        testRetrieveSnapshot();
        testSnapshotPresent();
        testRecoverSnapshot();
        testDeleteArchive();
        testForceRecovery();
        testSnapshotAllServices();
        }

    private void testForceRecovery()
        {
        Response response = getBaseTarget().path("services").path(SERVICE_NAME).path("persistence")
                .path("forceRecovery").request().post(null);

        assertThat(response.getStatus(), CoreMatchers.is(Response.Status.OK.getStatusCode()));

        Eventually.assertDeferred(() -> this.isPersistenceManagerIdle(m_client), is(true));
        }

    private void testRecoverSnapshot()
        {
        Response response = getBaseTarget().path("services").path(SERVICE_NAME).path("persistence")
                .path("snapshots").path("test-snapshot").path("recover").request().post(null);

        assertThat(response.getStatus(), CoreMatchers.is(Response.Status.OK.getStatusCode()));

        Eventually.assertDeferred(() -> this.isPersistenceManagerIdle(m_client), is(true));
        }

    private void testRetrieveSnapshot()
        {
        Response response = getBaseTarget().path("services").path(SERVICE_NAME).path("persistence")
                .path("archives").path("test-snapshot").path("retrieve").request().post(null);

        assertThat(response.getStatus(), CoreMatchers.is(Response.Status.OK.getStatusCode()));

        Eventually.assertDeferred(() -> this.isPersistenceManagerIdle(m_client), is(true));
        }

    private void testDeleteSnapshot()
        {
        Eventually.assertDeferred(() -> this.isPersistenceManagerIdle(m_client), is(true));

        Response response = getBaseTarget().path("services").path(SERVICE_NAME).path("persistence")
                .path("snapshots").path("test-snapshot").request().delete();

        JsonMap  mapResponse = new JsonMap(response.readEntity(JsonMap.class));

        List<String> listObjSnapshots = (List<String>) mapResponse.get("snapshots");

        assertThat(listObjSnapshots, not(hasItem("test-snapshot")));

        Eventually.assertDeferred(() -> this.isPersistenceManagerIdle(m_client), is(true));
        }

    private void testDeleteArchive()
        {
        Eventually.assertDeferred(() -> this.isPersistenceManagerIdle(m_client), is(true));

        Response response = getBaseTarget().path("services").path(SERVICE_NAME).path("persistence")
                .path("archives").request().get();

        JsonMap mapResponse = new JsonMap(response.readEntity(JsonMap.class));

        List<String> listObjSnapshots = (List<String>) mapResponse.get("snapshots");

        assertThat(listObjSnapshots, not(hasItem("test-snapshot")));
        }

    private void testSnapshot()
        {
        Response response = getBaseTarget().path("services").path(SERVICE_NAME).path("persistence")
                .path("snapshots").path("test-snapshot").request().post(null);

        assertThat(response.getStatus(), CoreMatchers.is(Response.Status.OK.getStatusCode()));

        Eventually.assertDeferred(() -> this.isPersistenceManagerIdle(m_client), is(true));
        }

    private void testSnapshotAllServices()
        {
        Response response = getBaseTarget().path("services").path("persistence")
                .path("snapshots").path("test-snapshot-all-services").request().post(null);

        assertThat(response.getStatus(), CoreMatchers.is(Response.Status.OK.getStatusCode()));

        Eventually.assertDeferred(() -> this.isPersistenceManagerIdle(m_client), is(true));
        }

    private void testArchive()
        {
        Response response = getBaseTarget().path("services").path(SERVICE_NAME).path("persistence")
                .path("archives").path("test-snapshot").request().post(null);

        assertThat(response.getStatus(), CoreMatchers.is(Response.Status.OK.getStatusCode()));

        Eventually.assertDeferred(() -> this.isPersistenceManagerIdle(m_client), is(true));
        }


    private void testArchiveStores()
        {
        Response response = getBaseTarget().path("services").path(SERVICE_NAME).path("persistence")
                .path("archiveStores").path("test-snapshot").request().get();

        assertThat(response.getStatus(), CoreMatchers.is(Response.Status.OK.getStatusCode()));

        JsonMap mapResponse = new JsonMap(response.readEntity(JsonMap.class));

        List<String> objSnapshots = (List<String>) mapResponse.get("archiveStores");

        assertThat(objSnapshots, notNullValue());
        }
    private void testArchivePresent()
        {
        Response response = getBaseTarget().path("services").path(SERVICE_NAME).path("persistence")
                .path("archives").request().get();

        JsonMap  mapResponse = new JsonMap(response.readEntity(JsonMap.class));

        List<String> listObjArchives = (List<String>) mapResponse.get("archives");

        assertThat(listObjArchives, hasItem("test-snapshot"));
        }

    private void testSnapshotPresent()
        {
        Response response = getBaseTarget().path("services").path(SERVICE_NAME).path("persistence")
                .path("snapshots").request().get();

        JsonMap mapResponse = new JsonMap(response.readEntity(JsonMap.class));

        List<String> listObjSnapshots = (List<String>) mapResponse.get("snapshots");

        assertThat(listObjSnapshots, hasItem("test-snapshot"));
        }

    public boolean isPersistenceManagerIdle(Client client)
        {
        Response response = getBaseTarget(client).path("services").path(SERVICE_NAME).path("persistence").request().get();

        assertThat(response.getStatus(), CoreMatchers.is(Response.Status.OK.getStatusCode()));
        JsonMap jsonResponse = new JsonMap(response.readEntity(JsonMap.class));
        return (boolean) jsonResponse.get("idle");
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

    /**
     * The client object to be used for the tests.
     */
    protected static Client m_client;

    /**
     * The base URL for Management over REST requests.
     */
    protected static URI m_baseURI;

    // ----- constants ------------------------------------------------------

    /**
     * The name of the used PartitionedService.
     */
    protected static final String SERVICE_NAME  = "DistributedCachePersistence";

    /**
     * The name of Distributed cache.
     */
    protected static final String CACHE_NAME = "test-persistence";

    /**
     * Prefix for the spawned processes.
     */
    protected static final String SERVER_PREFIX = "testMgmtRESTServer";

    /**
     * Cache config used by the test and spawned processes.
     */
    protected static final String CACHE_CONFIG  = "persistence-cache-config.xml";

    /**
     * Name of the Coherence cluster.
     */
    public static final String CLUSTER_NAME = "mgmtRestCluster";

    /**
     * The tmp directory used by this test.
     */
    private static Path m_tmpDirectory;

    private static final Logger LOGGER = Logger.getLogger(PersistenceResourceTests.class.getName());
    }
