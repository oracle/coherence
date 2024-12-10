/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package security;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.tangosol.coherence.http.AbstractHttpServer;
import com.tangosol.net.security.Security;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.security.PrivilegedAction;

import javax.net.ssl.HostnameVerifier;

import javax.security.auth.Subject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Abstract base class of all Coherence*Extend REST HTTP security tests.
 *
 * @author jh  2011.01.11
 */
public abstract class AbstractRestSecurityTests
        extends AbstractFunctionalTest
    {

    // ----- constructors ----------------------------------------------------

    /**
    * Create a new AbstractRestSecurityTests that will use the cache
    * configuration file with the given path to instantiate NamedCache
    * instances.
    *
    * @param sPath  the configuration resource name or file path
    */
    public AbstractRestSecurityTests(String sPath)
        {
        super(sPath);
        }

    // ----- lifecycle methods ----------------------------------------------

    @BeforeClass
    public static void _startup()
        {
        String sLoginConfig = System.getProperty("java.security.auth.login.config");

        if (sLoginConfig == null || sLoginConfig.isEmpty())
            {
            System.setProperty("java.security.auth.login.config", "login.config");
            }

        System.setProperty("test.extend.port", LocalPlatform.get().getAvailablePorts().next().toString());
        AbstractFunctionalTest._startup();
        }

    @Before
    public void setupTest()
        {
        Subject subject = Security.login("manager", "private".toCharArray());

        Security.runAs(subject, (PrivilegedAction) () -> getNamedCache("dist-test").put("test", "secret"));
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test that an authorized user is allowed to access a protected resource.
     */
    @Test
    public void testAccessAllowed()
        {
        Client    client    = createHttpClient("manager");
        WebTarget webTarget = client.target(getResourceUrl("api/dist-test/test"));

        Response  response  = webTarget.request(MediaType.TEXT_PLAIN_TYPE)
                    .header("Authorization", "Basic " + AbstractHttpServer.toBase64("manager:private"))
                    .get();
        assertEquals(200, response.getStatus());
        assertEquals("secret", response.readEntity(String.class));
        }

    /**
     * Test that an unauthorized user is denied access to a protected resource.
     */
    @Test
    public void testAccessDenied()
        {
        Client    client    = createHttpClient("worker");
        WebTarget webTarget = client.target(getResourceUrl("api/dist-test/test"));

        Response  response  = webTarget.request(MediaType.TEXT_PLAIN_TYPE)
                    .header("Authorization", "Basic " + AbstractHttpServer.toBase64("worker:private"))
                    .get(Response.class);
        assertEquals(403, response.getStatus());
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create a new HTTP client.
     *
     * @param sUsername  the name of the user associated with the client
     *
     * @return a new HTTP client
     */
    protected Client createHttpClient(String sUsername)
        {
        return ClientBuilder.newClient();
        }

    /**
     * Create a new HTTPS client.
     *
     * @param provider  the SSLSocketProvider used by the HTTPS client
     *
     * @return a new HTTPS client
     */
    protected Client createHttpsClient(SSLSocketProvider provider)
        {
        return ClientBuilder.newBuilder()
            .hostnameVerifier( (HostnameVerifier) (s, sslSession) -> true)
            .sslContext(provider.getDependencies().getSSLContext()).build();
        }


    // ----- accessors ------------------------------------------------------

    /**
     * Return the address that the embedded HttpServer is listening on.
     *
     * @return the listen address of the embedded HttpServer
     */
    public String getAddress()
        {
        return System.getProperty("test.extend.address.local", "127.0.0.1");
        }

    /**
     * Return the port that the embedded HttpServer is listening on.
     *
     * @return the listen port of the embedded HttpServer
     */
    public int getPort()
        {
        return Integer.getInteger("test.extend.port", 8080);
        }

    /**
     * Return the protocol used for all tests.
     *
     * @return the protocol
     */
    public String getProtocol()
        {
        return "http";
        }

    /**
     * Return the context path of the REST test application.
     *
     * @return context path
     */
    public String getContextPath()
        {
        return "/";
        }

    /**
     * Return the url of the specified resource
     *
     * @param resource test resource
     *
     * @return the resource url
     */
    public String getResourceUrl(String resource)
        {
        String contextPath = getContextPath();
        if (!contextPath.endsWith("/"))
            {
            contextPath += "/";
            }
        return getProtocol() + "://" + getAddress() + ":" + getPort()
                + contextPath + resource;
        }
    }
