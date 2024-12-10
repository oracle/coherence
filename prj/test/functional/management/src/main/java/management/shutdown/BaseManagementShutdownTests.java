/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management.shutdown;

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
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;

import com.oracle.bedrock.runtime.concurrent.runnable.RuntimeHalt;

import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.Reads;

import com.tangosol.coherence.management.internal.MapProvider;

import com.tangosol.discovery.NSLookup;

import com.tangosol.internal.net.management.HttpHelper;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.management.MapJsonBodyHandler;

import com.tangosol.util.Base;

import com.oracle.coherence.testing.AbstractTestInfrastructure;

import management.MultiCluster;

import org.glassfish.jersey.logging.LoggingFeature;

import org.hamcrest.MatcherAssert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TestName;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.runners.MethodSorters;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static com.oracle.bedrock.deferred.DeferredHelper.delayedBy;
import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static com.tangosol.internal.management.resources.AbstractManagementResource.NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Contains tests for the shutdown and restart of Coherence cluster services via Management over REST.
 *
 * @author gh 2022.12.13
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BaseManagementShutdownTests {
    // ----- constructors ---------------------------------------------------

    public BaseManagementShutdownTests() {
        this(CLUSTER_NAME);
    }

    public BaseManagementShutdownTests(String sClusterName) {
        f_sClusterName = sClusterName;
    }

    // ----- junit lifecycle methods ----------------------------------------

    @AfterClass
    public static void tearDown() {
        // allow server to clean up before being stopped
        Base.sleep(3000);

        if (m_client != null) {
            m_client.close();
        }

        if (s_cluster != null) {
            // work around for bug 33867995
            s_cluster.close(RuntimeHalt.withExitCode(0));
        }

        FileHelper.deleteDirSilent(m_dirActive);
        FileHelper.deleteDirSilent(m_dirArchive);
        FileHelper.deleteDirSilent(m_dirSnapshot);
    }

    @Before
    public void beforeTest() {
        String sMsg = ">>>>> Starting test: " + m_testName.getMethodName();
        for (CoherenceClusterMember member : s_cluster) {
            if (member != null) {
                member.submit(() ->
                {
                    Logger.info(sMsg);
                    System.err.println(sMsg);
                    System.err.flush();
                    return null;
                }).join();
            }
        }

        if (!s_fInvocationServiceStarted) {
            startService(INVOCATION_SERVICE_NAME, INVOCATION_SERVICE_TYPE);
            s_fInvocationServiceStarted = true;
        }
        ensureServicesAreAvailable();
    }

    @After
    public void afterTest() {
        String sMsg = ">>>>> Finished test: " + m_testName.getMethodName();
        for (CoherenceClusterMember member : s_cluster) {
            if (member != null) {
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

    /**
     * Verifies COH-25823 - MetricsHttpProxy service fails to restart when Node MBean shutdown() issued
     * <p>
     * This test will shutdown Cluster Node-2 which runs the Metrics Service. Once restarted the node should have the
     * metrics service running again.
     */
    @Test
    public void testClusterNodeShutdownWithServicesRestart() {
        Assume.assumeFalse("Skipping as management is read-only", isReadOnly());
        WebTarget target = getBaseTarget().path(SERVICES).path(MetricsHttpHelper.getServiceName()).path("members");
        Response response = target.request().get();
        Map mapResponse = readEntity(target, response);
        List<Map> listItemMaps = (List<Map>) mapResponse.get("items");
        assertThat(listItemMaps.size(), is(1));
        assertThat(listItemMaps.get(0).get("nodeId"), is("2"));
        assertThat(listItemMaps.get(0).get("member"), is(SERVER_PREFIX + "-2"));
        assertThat(listItemMaps.get(0).get("running"), is(true));

        target = getBaseTarget().path(SERVICES).path(HttpHelper.getServiceName()).path("members");
        response = target.request().get();
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        mapResponse = readEntity(target, response);
        listItemMaps = (List<Map>) mapResponse.get("items");
        assertThat(listItemMaps.size(), is(1));

        String sNodeId = (String) listItemMaps.get(0).get("nodeId");
        try
            {
            response = getBaseTarget().path("members").path(SERVER_PREFIX + "-2").path("shutdown").request(MediaType.APPLICATION_JSON_TYPE).post(null);
            MatcherAssert.assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            }
        catch (Exception e)
            {
            // Occasionally, it may get exception if the management HTTP service
            // is started on the node being shutdown;
            // In this case, it won't be able to connect to the REST service again;
            // log the exception and return.
            System.err.println("xtestClusterNodeShutdownWithServicesRestart() got an exception: " + e);
            if (sNodeId.equals("2"))
                {
                return;
                }
            }

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
        assertThat(members.get(0).get("member"), is(SERVER_PREFIX + "-2"));
        assertThat(members.get(0).get("running"), is(true));
    }

    // ----- utility methods----------------------------------------------------

    /**
     * Can be overridden by sub-classes that use scoped service names.
     *
     * @param sName the service name
     * @return the scoped service name
     */
    protected String getScopedServiceName(String sName) {
        return sName;
    }

    public WebTarget getBaseTarget() {
        return getBaseTarget(m_client);
    }

    public WebTarget getBaseTarget(Client client) {
        try {
            if (m_baseURI == null) {
                String sPort = s_cluster.getAny().getSystemProperties().getProperty("test.multicast.port", "7778");
                int nPort = Integer.parseInt(sPort);
                Collection<URL> colURL = NSLookup.lookupHTTPManagementURL(f_sClusterName, new InetSocketAddress("127.0.0.1", nPort));

                assertThat(colURL.isEmpty(), is(false));

                m_baseURI = colURL.iterator().next().toURI();

                Logger.info("Management HTTP Acceptor lookup returned: " + m_baseURI);
            }
            return client.target(m_baseURI);
        } catch (IOException | URISyntaxException e) {
            throw Exceptions.ensureRuntimeException(e);
        }
    }

    protected void ensureServicesAreAvailable() {
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
            // Occasionally, the metrics service may be on the other server.
            String serviceNames = services.stream().map(p -> (String) p.get("name"))
                    .collect(Collectors.joining(","));
            Base.log("The following " + services.size() + " services exist: " + serviceNames);
            Base.log("The following " + SERVICES_LIST.length + " services expected: " + String.join(",", SERVICES_LIST));
            return serviceNames.contains(MetricsHttpHelper.getServiceName()) ? services.size() : services.size() + 1;
        }, is(EXPECTED_SERVICE_COUNT), within(5, TimeUnit.MINUTES));
    }

    protected Map readEntity(WebTarget target, Response response) {
        return readEntity(target, response, null);
    }

    protected Map readEntity(WebTarget target, Response response, Entity entity) {
        int cAttempt = 0;
        int cMaxAttempt = 0;

        while (true) {
            String sJson = null;
            try {
                InputStream inputStream = response.readEntity(InputStream.class);
                if ("gzip".equalsIgnoreCase(response.getHeaderString("Content-Encoding"))) {
                    inputStream = new GZIPInputStream(inputStream);
                }
                try {
                    byte[] abData = Reads.read(inputStream);
                    sJson = new String(abData, StandardCharsets.UTF_8);
                } finally {
                    inputStream.close();
                }

                Map map = f_jsonHandler.readMap(new ByteArrayInputStream(sJson.getBytes(StandardCharsets.UTF_8)));
                if (map == null) {
                    Logger.info(getClass().getName() + ".readEntity() returned null"
                            + ", target: " + target + ", response: " + response);
                } else {
                    return map;
                }
            } catch (Throwable e) {
                Logger.err(getClass().getName() + ".readEntity() got an error "
                        + " from target " + target, e);

                if (sJson != null) {
                    Logger.err("JSON response that caused error\n" + sJson);
                }

                if (cAttempt >= cMaxAttempt) {
                    throw Exceptions.ensureRuntimeException(e);
                }
            }

            // try again
            if (entity == null) {
                response = target.request().get();
            } else {
                response = target.request().post(entity);
            }
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));

            cAttempt++;
        }
    }

    public static void startTestCluster(Class<?> clsMain, String sClusterName) {
        startTestCluster(clsMain,
                sClusterName,
                BaseManagementShutdownTests::assertClusterReady,
                opts -> opts);
    }

    public static void startTestCluster(Class<?> clsMain,
                                        String sClusterName,
                                        Consumer<CoherenceCluster> clusterReady,
                                        Function<OptionsByType, OptionsByType> beforeLaunch,
                                        Option... opts) {
        try {
            m_dirActive = FileHelper.createTempDir();
            m_dirSnapshot = FileHelper.createTempDir();
            m_dirArchive = FileHelper.createTempDir();
            s_dirJFR = FileHelper.createTempDir();
        } catch (IOException ioe) {
            throw new RuntimeException("Error creating persistence directories", ioe);
        }

        OptionsByType commonOptions = AbstractTestInfrastructure.createCacheServerOptions(clsMain.getName(), null, System.getProperties());

        AbstractTestInfrastructure.addTestProperties(commonOptions);
        commonOptions.add(Logging.atMax());
        commonOptions.addAll(opts);

        if (!commonOptions.contains(ClassPath.class)) {
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

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();
        OptionsByType propsServer1 = OptionsByType.of(commonOptions);

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

        if (Boolean.getBoolean("test.security.enabled")) {
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
        propsServer2.add(SystemProperty.of("coherence.metrics.http.enabled", "true"));
        propsServer2.add(SystemProperty.of("coherence.metrics.http.port", "0"));
        builder.include(1, CoherenceClusterMember.class, beforeLaunch.apply(propsServer2).asArray());

        s_cluster = builder.build(LocalPlatform.get());

        clusterReady.accept(s_cluster);

        m_client = ClientBuilder.newBuilder()
                .register(MapProvider.class)
                .register(new LoggingFeature(java.util.logging.Logger.getLogger("coherence.management.rest.diagnostic"),
                        Level.INFO,
                        LoggingFeature.Verbosity.PAYLOAD_TEXT,
                        4096))
                .build();
    }

    protected static void assertClusterReady(CoherenceCluster cluster) {
        for (CoherenceClusterMember member : cluster) {
            String sScope = member.getSystemProperty("test.scope.name");
            String sPrefix;
            if (sScope == null || sScope.isEmpty()) {
                sPrefix = "";
            } else {
                sPrefix = sScope + ":";
            }

            Eventually.assertDeferred(() -> member.isServiceRunning(sPrefix + SERVICE_NAME), is(true), within(5, TimeUnit.MINUTES));
            Eventually.assertDeferred(() -> member.getServiceStatus(sPrefix + SERVICE_NAME), is(ServiceStatus.NODE_SAFE));
            Eventually.assertDeferred(() -> member.isServiceRunning(sPrefix + ACTIVE_SERVICE), is(true), within(3, TimeUnit.MINUTES));
            Eventually.assertDeferred(() -> member.getServiceStatus(sPrefix + ACTIVE_SERVICE), is(ServiceStatus.NODE_SAFE));
            Eventually.assertDeferred(() -> member.invoke(IsReady.INSTANCE), is(true), within(3, TimeUnit.MINUTES));
        }
    }

    protected void startService(String sName, String sType) {
        String sScopedName = getScopedServiceName(sName);
        List<CoherenceClusterMember> listMember = s_cluster.stream().collect(Collectors.toList());
        for (CoherenceClusterMember member : listMember) {
            member.submit(new RemoteStartService(sScopedName, sType));
        }
    }

    //--------------------- helper classes ----------------------------

    public static class RemoteStartService implements RemoteRunnable {
        public RemoteStartService(String sName, String sType) {
            m_sName = sName;
            m_sType = sType;
        }

        @Override
        public void run() {
            CacheFactory.getCluster().ensureService(m_sName, m_sType).start();
        }

        String m_sName;
        String m_sType;
    }

    // ----- static helpers -------------------------------------------------

    /**
     * Return if management is to be read-only.
     */
    protected static boolean isReadOnly() {
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
     * The services path.
     */
    protected static final String SERVICES = "services";

    /**
     * The name of the active persistence service.
     */
    protected static final String ACTIVE_SERVICE = "DistributedCachePersistence";

    /**
     * The name of the used Extend proxy service.
     */
    protected static final String PROXY_SERVICE_NAME = "ExtendProxyService";

    /**
     * The name of the used PartitionedService.
     */
    protected static final String SERVICE_NAME = "DistributedCache";

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
            "DistributedCachePersistence", HttpHelper.getServiceName(),
            INVOCATION_SERVICE_NAME, MetricsHttpHelper.getServiceName()};

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

    /**
     * The cluster members.
     */
    protected static CoherenceCluster s_cluster;

    protected final String f_sClusterName;

    @Rule
    public final TestName m_testName = new TestName();

    @ClassRule
    public static final TestLogs m_testLogs = new TestLogs();

    private final MapJsonBodyHandler f_jsonHandler = MapJsonBodyHandler.ensureMapJsonBodyHandler();
}
