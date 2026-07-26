/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.RuntimeHalt;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor;
import com.tangosol.coherence.component.util.safeService.SafeProxyService;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;
import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import org.junit.Test;

import rest.data.Persona;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;

import java.nio.charset.StandardCharsets;

import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional smoke tests for the REST-01 mode matrix.
 * <p>
 * Each assertion starts a separate cache server for the requested mode because
 * {@code CoherenceMode} is process-global and memoized in the member JVM. The
 * member also receives a per-run cluster name, so this suite proves the REST
 * stack without joining another agent's cluster.
 * Decision 1 uses {@link Rest01EngagedAuthResourceConfig} to provide an
 * engaged Jersey container security context without acceptor-level Basic auth.
 * <p>
 * Prompt reference:
 * {@code design/features/security-bugs/plans/rest-01/prompts/08-slice-f-test-plan-implementation.md}.
 *
 * @author Vaso Putica  2026.05.10
 * @since 26.04
 */
public class RestSecurityModeFunctionalTests
        extends AbstractFunctionalTest
    {
    /**
     * Should apply the REST auth mode matrix when REST receives an engaged
     * container security context.
     *
     * @throws Exception if the member or HTTP request fails unexpectedly
     */
    @Test
    public void shouldApplyAuthModeMatrixWhenContainerAuthContextIsEngaged()
            throws Exception
        {
        assertAuthMode("compatibility-authenticated", "prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY,
                Response.Status.OK.getStatusCode(), true);
        assertAuthMode("dev-authenticated", "dev", Response.Status.OK.getStatusCode(), true);
        assertAuthMode("prod-authenticated", "prod", Response.Status.OK.getStatusCode(), true);

        assertAuthMode("compatibility-unauthenticated", "prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY,
                Response.Status.OK.getStatusCode(), false);
        assertAuthMode("dev-unauthenticated", "dev", Response.Status.OK.getStatusCode(), false);
        assertAuthMode("prod-unauthenticated", "prod", Response.Status.OK.getStatusCode(), false);
        }

    /**
     * Should preserve anonymous REST access when container auth is not engaged.
     *
     * @throws Exception if the member or HTTP request fails unexpectedly
     */
    @Test
    public void shouldPreserveAnonymousRestWhenAuthIsNotEngaged()
            throws Exception
        {
        assertAnonymousNoAuthMode("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        assertAnonymousNoAuthMode("dev");
        assertAnonymousNoAuthMode("prod");
        }

    /**
     * Should apply the pass-through allowlist mode matrix through the
     * PassThroughResourceConfig fixture.
     *
     * @throws Exception if the member or HTTP request fails unexpectedly
     */
    @Test
    public void shouldApplyPassThroughAllowlistModeMatrix()
            throws Exception
        {
        assertPassThroughMode("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY,
                Response.Status.OK.getStatusCode());
        assertPassThroughMode("dev", Response.Status.OK.getStatusCode());
        assertPassThroughMode("prod", Response.Status.OK.getStatusCode());
        }

    /**
     * Should apply the direct-query gate to the SSE path for each mode.
     *
     * @throws Exception if the member or HTTP request fails unexpectedly
     */
    @Test
    public void shouldApplyDirectQuerySseModeMatrix()
            throws Exception
        {
        assertDirectQuerySseWithoutConfigMode("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY,
                Response.Status.OK.getStatusCode());
        assertDirectQuerySseWithoutConfigMode("dev", Response.Status.OK.getStatusCode());
        assertDirectQuerySseWithoutConfigMode("prod", Response.Status.OK.getStatusCode());

        assertDirectQuerySseCompatibilityEnabledMode("dev");
        assertDirectQuerySseCompatibilityEnabledMode("prod");
        }

    /**
     * Should allow configured aliases and reject raw expressions according to
     * the current mode.
     *
     * @throws Exception if the member or HTTP request fails unexpectedly
     */
    @Test
    public void shouldApplyAliasModeMatrix()
            throws Exception
        {
        assertAliasMode("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "age:asc",
                Response.Status.OK.getStatusCode());
        assertAliasMode("dev", "age:asc", Response.Status.OK.getStatusCode());
        assertAliasMode("prod", "age:asc", Response.Status.OK.getStatusCode());
        }

    /**
     * Should allow raw projection expressions on direct queries in
     * compatibility modes.
     *
     * @throws Exception if the member or HTTP request fails unexpectedly
     */
    @Test
    public void shouldAllowRawProjectionOnDirectQueryInCompatibilityModes()
            throws Exception
        {
        assertRawProjectionDirectQueryMode("dev");
        assertRawProjectionDirectQueryMode("prod");
        }

    private void assertAuthMode(String sServerName, String sMode, int nStatus, boolean fCredentials)
            throws Exception
        {
        assertAuthMode(sServerName, sMode, CoherenceMode.SECURITY_MODE_COMPATIBILITY, nStatus, fCredentials);
        }

    private void assertAuthMode(String sServerName, String sMode, String sSecurityMode, int nStatus,
                                boolean fCredentials)
            throws Exception
        {
        try (Server server = startServer("auth-" + sServerName, sMode, sSecurityMode, FILE_SERVER_CFG_ENGAGED_AUTH))
            {
            server.member().invoke(new PutString("dist-test", "test", "secret"));

            Invocation.Builder builder = server.requestApi("dist-test/test").request(MediaType.WILDCARD_TYPE);
            if (fCredentials)
                {
                builder.header("Authorization", "Basic " + BASIC_AUTH_TOKEN);
                }

            try (Response response = builder.get())
                {
                assertEquals(nStatus, response.getStatus());
                }
            }
        }

    private void assertAnonymousNoAuthMode(String sMode)
            throws Exception
        {
        assertAnonymousNoAuthMode(sMode, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        }

    private void assertAnonymousNoAuthMode(String sMode, String sSecurityMode)
            throws Exception
        {
        try (Server server = startServer("noauth-" + sMode, sMode, sSecurityMode, FILE_SERVER_CFG_DEFAULT))
            {
            server.member().invoke(new PutString("dist-test", "test", "secret"));

            try (Response response = server.requestApi("dist-test/test").request(MediaType.WILDCARD_TYPE).get())
                {
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                }
            }
        }

    private void assertPassThroughMode(String sMode, int nStatus)
            throws Exception
        {
        assertPassThroughMode(sMode, CoherenceMode.SECURITY_MODE_COMPATIBILITY, nStatus);
        }

    private void assertPassThroughMode(String sMode, String sSecurityMode, int nStatus)
            throws Exception
        {
        try (Server server = startServer("passthrough-" + sMode, sMode, sSecurityMode, FILE_SERVER_CFG_PASSTHROUGH))
            {
            try (Response response = server.requestRoot("dist-rest01-unlisted/test")
                    .request(MediaType.WILDCARD_TYPE)
                    .put(Entity.entity("value".getBytes(StandardCharsets.UTF_8),
                            MediaType.APPLICATION_OCTET_STREAM_TYPE)))
                {
                assertEquals(nStatus, response.getStatus());

                if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode() && response.hasEntity())
                    {
                    String sBody = response.readEntity(String.class);
                    assertFalse(sBody.contains("dist-rest01-unlisted"));
                    }
                }
            }
        }

    private void assertDirectQuerySseWithoutConfigMode(String sMode, int nStatus)
            throws Exception
        {
        assertDirectQuerySseWithoutConfigMode(sMode, CoherenceMode.SECURITY_MODE_COMPATIBILITY, nStatus);
        }

    private void assertDirectQuerySseWithoutConfigMode(String sMode, String sSecurityMode, int nStatus)
            throws Exception
        {
        try (Server server = startServer("query-" + sMode, sMode, sSecurityMode, FILE_SERVER_CFG_DEFAULT))
            {
            server.member().invoke(new PutString("dist-test", "test", "secret"));

            try (Response response = server.requestApi("dist-test")
                    .queryParam("q", "value().length() == 6")
                    .request(MediaType.SERVER_SENT_EVENTS_TYPE)
                    .get())
                {
                assertEquals(nStatus, response.getStatus());
                }
            }
        }

    private void assertDirectQuerySseCompatibilityEnabledMode(String sMode)
            throws Exception
        {
        try (Server server = startServer("query-enabled-" + sMode, sMode, FILE_SERVER_CFG_DEFAULT))
            {
            server.member().invoke(PutPeople.INSTANCE);

            try (Response response = server.requestApi("dist-test-named-query")
                    .queryParam("q", "name.length() == 4")
                    .request(MediaType.SERVER_SENT_EVENTS_TYPE)
                    .get())
                {
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                }
            }
        }

    private void assertAliasMode(String sMode, String sAcceptedSort, int nRawStatus)
            throws Exception
        {
        assertAliasMode(sMode, CoherenceMode.SECURITY_MODE_COMPATIBILITY, sAcceptedSort, nRawStatus);
        }

    private void assertAliasMode(String sMode, String sSecurityMode, String sAcceptedSort, int nRawStatus)
            throws Exception
        {
        try (Server server = startServer("alias-" + sMode, sMode, sSecurityMode, FILE_SERVER_CFG_DEFAULT))
            {
            server.member().invoke(PutPeople.INSTANCE);

            try (Response response = server.requestApi("dist-test-named-query/age-37-query;sort=" + sAcceptedSort)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get())
                {
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                }

            try (Response response = server.requestApi("dist-test-named-query/age-37-query;sort=age:asc")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get())
                {
                assertEquals(nRawStatus, response.getStatus());
                }
            }
        }

    private void assertRawProjectionDirectQueryMode(String sMode)
            throws Exception
        {
        try (Server server = startServer("projection-" + sMode, sMode, FILE_SERVER_CFG_DEFAULT))
            {
            server.member().invoke(PutHttpExamplePeople.INSTANCE);

            try (Response response = server.requestApi("dist-http-example;p=name,age")
                    .queryParam("q", "age >= 26")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get())
                {
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                assertProjectedJsonBody(response.readEntity(String.class));
                }

            try (Response response = server.requestApi("dist-http-example;p=name,age")
                    .queryParam("q", "age >= 26")
                    .request(MediaType.APPLICATION_XML_TYPE)
                    .get())
                {
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                assertProjectedXmlBody(response.readEntity(String.class));
                }

            }
        }

    private void assertProjectedJsonBody(String sBody)
            throws Exception
        {
        JsonNode root = JSON_MAPPER.readTree(sBody);

        assertTrue(sBody, root.isArray());
        assertEquals(sBody, 3, root.size());

        Map<String, Integer> mapExpected = expectedProjectedPeople();
        for (JsonNode person : root)
            {
            assertTrue(sBody, person.isObject());

            Set<String> setFields = new HashSet<>();
            person.fieldNames().forEachRemaining(setFields::add);
            assertEquals(sBody, Set.of("age", "name"), setFields);

            String sName = person.get("name").asText();
            assertTrue(sBody, mapExpected.containsKey(sName));
            assertEquals(sBody, mapExpected.remove(sName).intValue(), person.get("age").asInt());
            }

        assertTrue(sBody, mapExpected.isEmpty());
        }

    private void assertProjectedXmlBody(String sBody)
            throws Exception
        {
        XmlDocument xml          = new SimpleParser().parseXml(sBody);
        Map<String, Integer> mapExpected = expectedProjectedPeople();

        assertEquals(sBody, 3, xml.getElementList().size());
        for (Object oElement : xml.getElementList())
            {
            XmlElement person = (XmlElement) oElement;
            String     sName  = null;
            Integer    nAge   = null;

            for (Object oAttr : person.getAttributeMap().keySet())
                {
                String sAttr = (String) oAttr;
                assertTrue(sBody, "age".equals(sAttr) || isXmlMetadataAttribute(sAttr));
                }

            XmlValue xmlAge = person.getAttribute("age");
            if (xmlAge != null)
                {
                nAge = xmlAge.getInt();
                }

            for (Object oChild : person.getElementList())
                {
                XmlElement child = (XmlElement) oChild;
                String     sNameChild = child.getName();

                assertTrue(sBody, Set.of("age", "name").contains(sNameChild));
                if ("name".equals(sNameChild))
                    {
                    sName = child.getString();
                    }
                else
                    {
                    nAge = child.getInt();
                    }
                }

            assertNotNull(sBody, sName);
            assertNotNull(sBody, nAge);
            assertTrue(sBody, mapExpected.containsKey(sName));
            assertEquals(sBody, mapExpected.remove(sName), nAge);
            }

        assertTrue(sBody, mapExpected.isEmpty());
        }

    private boolean isXmlMetadataAttribute(String sName)
        {
        return sName.startsWith("xmlns")
               || sName.endsWith(":type")
               || sName.endsWith(":nil");
        }

    private Map<String, Integer> expectedProjectedPeople()
        {
        return new HashMap<>(Map.of("Ivan", 33, "Aleks", 37, "Vaso", 37));
        }

    private Server startServer(String sServerName, String sMode, String sCacheConfig)
        {
        return startServer(sServerName, sMode, CoherenceMode.SECURITY_MODE_COMPATIBILITY, sCacheConfig);
        }

    private Server startServer(String sServerName, String sMode, String sSecurityMode, String sCacheConfig)
        {
        String     sName        = "Rest01-" + sServerName + '-' + Long.toString(System.nanoTime(), 36);
        String     sClusterName = "rest01-" + Long.toString(System.nanoTime(), 36);
        Properties properties   = new Properties();

        properties.setProperty("coherence.cluster", sClusterName);
        properties.setProperty("coherence.mode", sMode);
        properties.setProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
        properties.setProperty("coherence.override", "rest-tests-coherence-override.xml");
        properties.setProperty("coherence.wka", "127.0.0.1");
        properties.setProperty("coherence.rest.config", FILE_REST_CFG);
        properties.setProperty("test.extend.port", "0");
        properties.setProperty("test.unicast.port", "0");
        properties.setProperty("test.multicast.address", generateUniqueAddress(true));
        properties.setProperty("test.multicast.port", String.valueOf(LocalPlatform.get().getAvailablePorts().next()));
        properties.setProperty("com.tangosol.coherence.rest.server.DefaultResourceConfig.logging.enabled", "true");

        CoherenceClusterMember member = startCacheServer(sName, "rest", sCacheConfig, properties);
        Eventually.assertDeferred(() -> member.isServiceRunning("ExtendHttpProxyService"), is(true));

        int nPort = member.invoke(GetRestPort.INSTANCE);
        assertTrue("REST port should be assigned", nPort > 0);
        Eventually.assertDeferred(() -> isPortOpen(nPort), is(true));
        return new Server(member, nPort);
        }

    private static boolean isPortOpen(int nPort)
        {
        try (Socket socket = new Socket())
            {
            socket.connect(new InetSocketAddress("127.0.0.1", nPort), 1000);
            return true;
            }
        catch (IOException e)
            {
            return false;
            }
        }

    /**
     * A lightweight REST member handle for tests that need a fresh member per
     * mode.
     */
    private static class Server
            implements AutoCloseable
        {
        private Server(CoherenceClusterMember member, int nPort)
            {
            f_member = member;
            f_nPort  = nPort;
            f_client = ClientBuilder.newBuilder()
                    .withConfig(new ClientConfig())
                    .property(ClientProperties.CONNECT_TIMEOUT, 30000)
                    .property(ClientProperties.READ_TIMEOUT, 30000)
                    .build();
            }

        CoherenceClusterMember member()
            {
            return f_member;
            }

        WebTarget requestApi(String sResource)
            {
            return f_client.target("http://127.0.0.1:" + f_nPort + "/api/" + sResource);
            }

        WebTarget requestRoot(String sResource)
            {
            return f_client.target("http://127.0.0.1:" + f_nPort + "/" + sResource);
            }

        @Override
        public void close()
            {
            f_client.close();
            stopMember(f_member);
            }

        private final CoherenceClusterMember f_member;
        private final int                    f_nPort;
        private final Client                 f_client;
        }

    private static void stopMember(CoherenceClusterMember member)
        {
        if (member == null)
            {
            return;
            }

        try
            {
            member.submit(new RuntimeHalt());
            member.waitFor();
            }
        catch (Throwable ignored)
            {
            // the member may already have exited
            }
        finally
            {
            member.close();
            }
        }

    /**
     * Return the assigned HTTP acceptor port from inside the remote member.
     */
    public enum GetRestPort
            implements RemoteCallable<Integer>
        {
        INSTANCE;

        @Override
        public Integer call()
            {
            Cluster cluster = CacheFactory.getCluster();
            Object  service = cluster.getService("ExtendHttpProxyService");
            if (service instanceof SafeProxyService)
                {
                service = ((SafeProxyService) service).getRunningService();
                }

            ProxyService proxyService = (ProxyService) service;
            HttpAcceptor acceptor     = (HttpAcceptor) proxyService.getAcceptor();
            return acceptor == null ? 0 : acceptor.getListenPort();
            }
        }

    /**
     * Seed the typed REST resource from inside the member being tested.
     */
    public enum PutPeople
            implements RemoteCallable<Void>
        {
        INSTANCE;

        @Override
        public Void call()
            {
            NamedCache<Integer, Persona> cache = CacheFactory.getCache("dist-test-named-query");
            cache.clear();
            cache.put(1, new Persona("Ivan", 33));
            cache.put(2, new Persona("Aleks", 37));
            cache.put(3, new Persona("Vaso", 37));
            return null;
            }
        }

    /**
     * Seed the direct-query projection resource from inside the member.
     */
    public enum PutHttpExamplePeople
            implements RemoteCallable<Void>
        {
        INSTANCE;

        @Override
        public Void call()
            {
            NamedCache<Integer, Persona> cache = CacheFactory.getCache("dist-http-example");
            cache.clear();
            cache.put(1, new Persona("Ivan", 33));
            cache.put(2, new Persona("Aleks", 37));
            cache.put(3, new Persona("Vaso", 37));
            return null;
            }
        }

    /**
     * Seed a string resource from inside the auth member so the request proves
     * REST access rather than only authentication.
     */
    public static class PutString
            implements RemoteCallable<Void>
        {
        public PutString(String sCacheName, String sKey, String sValue)
            {
            f_sCacheName = sCacheName;
            f_sKey       = sKey;
            f_sValue     = sValue;
            }

        @Override
        public Void call()
            {
            CacheFactory.getCache(f_sCacheName).put(f_sKey, f_sValue);
            return null;
            }

        private final String f_sCacheName;
        private final String f_sKey;
        private final String f_sValue;
        }

    /**
     * REST config resource used by the focused REST-01 smoke.
     */
    public static final String FILE_REST_CFG = "coherence-rest-config-rest01.xml";

    /**
     * Default REST server cache config used for query and alias checks.
     */
    public static final String FILE_SERVER_CFG_DEFAULT = "server-cache-config-default.xml";

    /**
     * Test REST server cache config used to prove auth engaged behavior.
     */
    public static final String FILE_SERVER_CFG_ENGAGED_AUTH = "server-cache-config-rest01-engaged-auth.xml";

    /**
     * Pass-through REST server cache config used to prove root-resource behavior.
     */
    public static final String FILE_SERVER_CFG_PASSTHROUGH = "server-cache-config-rest01-passthrough.xml";

    /**
     * Basic auth token for client/password.
     */
    public static final String BASIC_AUTH_TOKEN = Base64.getEncoder()
            .encodeToString("client:password".getBytes(StandardCharsets.UTF_8));

    /**
     * JSON mapper used to assert exact REST projection shape.
     */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    }
