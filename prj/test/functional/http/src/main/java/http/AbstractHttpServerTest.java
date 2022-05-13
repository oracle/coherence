/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package http;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.coherence.http.AbstractHttpServer;

import com.tangosol.coherence.rest.server.DefaultResourceConfig;

import com.tangosol.internal.net.ssl.LegacyXmlSSLSocketProviderDependencies;

import com.tangosol.net.Service;

import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.net.security.UsernameAndPassword;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;

import java.io.IOException;

import java.security.Principal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import javax.security.auth.Subject;

import javax.ws.rs.ProcessingException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Base class for all test classes that test {@link AbstractHttpServer}
 * implementations.
 */
public abstract class AbstractHttpServerTest
    {
    @BeforeClass
    public static void setupSSL()
        {
        s_sslProviderClient      = createSSLSocketProvider("ssl-config-client.xml");
        s_sslProviderGuest       = createSSLSocketProvider("ssl-config-guest.xml");
        s_sslProviderServer      = createSSLSocketProvider("ssl-config-server.xml");
        s_sslProviderServerTrust = createSSLSocketProvider("ssl-config-server-trust.xml");
        }

    private static SSLSocketProvider createSSLSocketProvider(String fileName)
        {
        XmlDocument xml = XmlHelper.loadFileOrResource(fileName, null);
        return new SSLSocketProvider(new LegacyXmlSSLSocketProviderDependencies(xml));
        }

    @Test
    public void testHttpEcho()
            throws Exception
        {
        startHttpServer(null, "none");
        testEcho("http", "/test/echo");
        }

    @Test
    public void testHttpsEcho()
            throws Exception
        {
        startHttpServer(s_sslProviderServer, "none");
        testEcho("https", "/test/echo");
        }

    private void testEcho(String protocol, String sPath)
            throws Exception
        {
        Client    client = createHttpClient(protocol);
        WebTarget target = getWebTarget(protocol + "://" +
                        m_server.getLocalAddress() + ":" + m_server.getLocalPort() + sPath, client);

        String response = target.queryParam("s", "alive")
                                .request(MediaType.TEXT_PLAIN_TYPE)
                                .get(String.class);

        assertEquals("alive", response);
        }

    @Test
    public void testHttpBasicAuthentication()
            throws Exception
        {
        startHttpServer(null, "basic");
        testBasicAuthentication("http", s_sslProviderGuest);
        }

    @Test
    public void testHttpsBasicAuthentication()
            throws Exception
        {
        startHttpServer(s_sslProviderServer, "basic");
        testBasicAuthentication("https", s_sslProviderGuest);
        }

    @Test
    public void testSSLBasicAuthentication()
            throws Exception
        {
        startHttpServer(s_sslProviderServerTrust, "cert+basic");
        testBasicAuthentication("https", s_sslProviderClient);
        }

    private void testBasicAuthentication(String protocol, SSLSocketProvider provider)
            throws Exception
        {
        Client    client = createHttpsClient(provider);
        WebTarget target = getWebTarget(protocol + "://" +
                m_server.getLocalAddress() + ":" + m_server.getLocalPort() + "/test/principal", client);

        // missing Authorization header
        Response response = target.request(MediaType.TEXT_PLAIN_TYPE).get();
        assertEquals(401, response.getStatus());
        assertTrue(response.getStringHeaders().getFirst("WWW-Authenticate").startsWith("Basic"));

        // bad credentials
        response = target.request(MediaType.TEXT_PLAIN_TYPE)
                         .header("Authorization", "Basic " + AbstractHttpServer.toBase64("test:bad-password"))
                         .get();
        assertEquals(401, response.getStatus());
        assertTrue(response.getStringHeaders().getFirst("WWW-Authenticate").startsWith("Basic"));

        // good credentials
        response = target.request(MediaType.TEXT_PLAIN_TYPE)
                         .header("Authorization", "Basic " + AbstractHttpServer.toBase64("test:password"))
                         .get();
        assertEquals(200, response.getStatus());
        assertEquals("BASIC:test", response.readEntity(String.class));
        }

    @Test(expected = ProcessingException.class)
    public void testSslAuthenticationFailure()
            throws Exception
        {
        startHttpServer(s_sslProviderServerTrust, "cert");

        Client    client = createHttpsClient(s_sslProviderGuest);
        WebTarget target = getWebTarget("https://" +
                m_server.getLocalAddress() + ":" + m_server.getLocalPort() + "/test/principal", client);

        target.request(MediaType.TEXT_PLAIN_TYPE).get();
        }

    @Test
    public void testSslAuthenticationSuccess()
            throws Exception
        {
        startHttpServer(s_sslProviderServerTrust, "cert");

        Client    client = createHttpsClient(s_sslProviderClient);
        WebTarget target = getWebTarget("https://" +
                m_server.getLocalAddress() + ":" + m_server.getLocalPort() + "/test/principal", client);

        Response response = target.request(MediaType.TEXT_PLAIN_TYPE).get();
        assertEquals(200, response.getStatus());
        //remove spaces in case a server (e.g. Jetty) includes spaces in the result
        assertEquals("CLIENT_CERT:CN=client,O=Oracle,L=Burlington,ST=MA,C=US", response.readEntity(String.class).replace(" ", ""));
        }

    @Test(expected = ProcessingException.class)
    public void testSslBasicAuthenticationFailure()
            throws Exception
        {
        startHttpServer(s_sslProviderServerTrust, "cert+basic");

        Client    client = createHttpsClient(s_sslProviderGuest);
        WebTarget target = getWebTarget("https://" +
                m_server.getLocalAddress() + ":" + m_server.getLocalPort() + "/test/principal", client);

        target.request(MediaType.TEXT_PLAIN_TYPE).get();
        }

    @Test
    public void testRuntimeException()
            throws IOException
        {
        startHttpServer(null, "none");

        Client    client = createHttpClient("http");
        WebTarget target = getWebTarget("http://" +
                m_server.getLocalAddress() + ":" + m_server.getLocalPort() + "/test/runtime-exception", client);

        Response response = target.request(MediaType.TEXT_PLAIN_TYPE).get();
        assertEquals(500, response.getStatus());
        }

    @Test
    public void testSecurityException()
            throws IOException
        {
        startHttpServer(null, "none");

        Client    client = createHttpClient("http");
        WebTarget target = getWebTarget("http://" +
                m_server.getLocalAddress() + ":" + m_server.getLocalPort() + "/test/security-exception", client);

        Response response = target.request(MediaType.TEXT_PLAIN_TYPE).get();
        assertEquals(403, response.getStatus());
        }

    @Test
    public void testMultiApplication()
            throws Exception
        {
        Map<String, ResourceConfig> mapConfig = new HashMap<String, ResourceConfig>(3);
        mapConfig.put("/app1", new DefaultResourceConfig(TestResource.class));
        mapConfig.put("/app2", new DefaultResourceConfig(TestResource.class));
        mapConfig.put("/app3", new DefaultResourceConfig(TestResource.class));

        startHttpServer(null, "none", mapConfig);
        testEcho("http", "/app1/test/echo");
        testEcho("http", "/app2/test/echo");
        testEcho("http", "/app3/test/echo");
        }

    @Test
    public void test404WhenMissingResource()
            throws Exception
        {
        Map<String, ResourceConfig> mapConfig = new HashMap<String, ResourceConfig>(1);
        mapConfig.put("/app1", new DefaultResourceConfig(TestResource.class));

        startHttpServer(null, "none", mapConfig);

        Client    client = createHttpClient("http");
        WebTarget target = getWebTarget("http://" +
                                        m_server.getLocalAddress() + ":" + m_server.getLocalPort() + "/", client);

        Response response = target.request(MediaType.TEXT_PLAIN_TYPE).get();
        assertEquals(404, response.getStatus());
        }

    /**
     * Shutdown the test class.
     * <p>
     * This method stops the HTTP server.
     */
    @After
    public void testAfter()
            throws IOException
        {
        if (m_server != null)
            {
            m_server.stop();
            }
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Create a new HTTP client.
     *
     * @return a new HTTP client
     */
    protected Client createHttpClient(String protocol)
        {
        return "https".equals(protocol)
               ? createHttpsClient(s_sslProviderGuest)
               : ClientBuilder.newClient();
        }

    /**
     * Create a new HTTPS client.
     *
     * @return a new HTTPS client
     */
    protected Client createHttpsClient(SSLSocketProvider provider)
        {
        return ClientBuilder.newBuilder()
            .hostnameVerifier(new HostnameVerifier()
                {
                public boolean verify(String s, SSLSession sslSession)
                    {
                    return true;
                    }
                })
            .sslContext(provider.getDependencies().getSSLContext()).build();
        }

    protected WebTarget getWebTarget(String url, Client client)
        {
        return client.target(url);
        }

    protected WebTarget getWebTarget(String url, Client client, String attr)
        {
        return client.target(url + attr);
        }

    /**
     * Returns {@link AbstractHttpServer} implementation to test.
     *
     * @return http server to test
     */
    protected abstract AbstractHttpServer createServer();

    /**
     * Start HTTP(S) server using specified SSL context and authentication mode.
     *
     * @param provider  SSL socket provider
     * @param sAuth     authentication method
     *
     * @throws IOException  if an error occurs
     */
    protected void startHttpServer(SSLSocketProvider provider, String sAuth)
            throws IOException
        {
        ResourceConfig resourceConfig = new DefaultResourceConfig(TestResource.class);
        startHttpServer(provider, sAuth,
                Collections.singletonMap("/", resourceConfig));
        }

    /**
     * Start HTTP(S) server using specified SSL context and authentication mode.
     *
     * @param provider   SSL socket provider
     * @param sAuth      authentication method
     * @param mapConfig  map of context names and resource configs
     *
     * @throws IOException  if an error occurs
     */
    protected void startHttpServer(SSLSocketProvider provider, String sAuth,
                                   Map<String, ResourceConfig> mapConfig)
            throws IOException
        {
        AbstractHttpServer server = m_server = createServer();

        server.setLocalAddress(System.getProperty("test.extend.address.local", "127.0.0.1"));
        server.setResourceConfig(mapConfig);
        server.setSocketProvider(provider);
        server.setAuthMethod(sAuth);
        if (server.isAuthMethodBasic())
            {
            server.setIdentityAsserter(new IdentityAsserter()
                {
                public Subject assertIdentity(Object oToken, Service service)
                        throws SecurityException
                    {
                    UsernameAndPassword token = (UsernameAndPassword) oToken;
                    if ("test".equals(token.getUsername())
                         && "password".equals(new String(token.getPassword())))
                        {
                        Subject subject = new Subject();
                        subject.getPrincipals().add(new Principal()
                            {
                            public String getName()
                                {
                                return "test";
                                }
                            });
                        return subject;
                        }

                    throw new SecurityException("invalid username or password");
                    }
                });
            }
        server.start();
        server.setLocalPort(server.getListenPort());
        }

    // ---- data members ----------------------------------------------------

    private static SSLSocketProvider s_sslProviderClient;
    private static SSLSocketProvider s_sslProviderGuest;
    private static SSLSocketProvider s_sslProviderServer;
    private static SSLSocketProvider s_sslProviderServerTrust;

    private AbstractHttpServer m_server;
    }
