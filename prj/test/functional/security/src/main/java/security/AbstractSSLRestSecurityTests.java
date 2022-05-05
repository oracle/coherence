/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package security;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.coherence.http.AbstractHttpServer;

import com.tangosol.internal.net.ssl.LegacyXmlSSLSocketProviderDependencies;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Abstract base class of all Coherence*Extend REST HTTPS security tests.
 *
 * @author jh  2011.01.11
 */
public abstract class AbstractSSLRestSecurityTests
        extends AbstractRestSecurityTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new AbstractRestSecurityTests that will use the cache
    * configuration file with the given path to instantiate NamedCache
    * instances.
    *
    * @param sPath  the configuration resource name or file path
    */
    public AbstractSSLRestSecurityTests(String sPath)
        {
        super(sPath);
        }

    // ----- lifecycle methods ----------------------------------------------

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("java.security.auth.login.config", "login.configRsa");
        System.setProperty("coherence.override", "tangosol-coherence-override-rsa.xml");
        System.setProperty("coherence.security.config", "DefaultControllerRsa.xml");

        AbstractRestSecurityTests._startup();
        }

    @BeforeClass
    public static void setupSSL()
        {
        XmlDocument xmlManager;
        XmlDocument xmlRogue;
        XmlDocument xmlWorker;

        xmlManager = XmlHelper.loadFileOrResource(
                    "ssl-config-manager-rsa.xml", null);

        xmlRogue = XmlHelper.loadFileOrResource(
                    "ssl-config-rogue-rsa.xml",  null);

        xmlWorker = XmlHelper.loadFileOrResource(
                "ssl-config-worker-rsa.xml", null);

        s_sslProviderManager = new SSLSocketProvider(
                new LegacyXmlSSLSocketProviderDependencies(xmlManager));
        s_sslProviderRogue   = new SSLSocketProvider(
                new LegacyXmlSSLSocketProviderDependencies(xmlRogue));
        s_sslProviderWorker  = new SSLSocketProvider(
                new LegacyXmlSSLSocketProviderDependencies(xmlWorker));
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test that an untrusted user is not allowed to connect.
     */
    @Test(expected = Exception.class)
    public void testConnectDenied()
        {
        Client    client   = createHttpClient("rogue");
        WebTarget resource = client.target(getResourceUrl("api/dist-test/test"));

        Response  response = resource.request(MediaType.TEXT_PLAIN_TYPE)
                    .header("Authorization", "Basic " +
                            AbstractHttpServer.toBase64("rogue:private"))
                    .get(Response.class);
        assertEquals(403, response.getStatus());
        }

    // ----- AbstractRestSecurityTests methods ------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected Client createHttpClient(String sUsername)
        {
        if ("manager".equals(sUsername))
            {
            return createHttpsClient(s_sslProviderManager);
            }
        else if ("rogue".equals(sUsername))
            {
            return createHttpsClient(s_sslProviderRogue);
            }
        else if ("worker".equals(sUsername))
            {
            return createHttpsClient(s_sslProviderWorker);
            }
        else
            {
            throw new IllegalArgumentException();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProtocol()
        {
        return "https";
        }

    // ----- data members ---------------------------------------------------

    protected static SSLSocketProvider s_sslProviderManager;
    protected static SSLSocketProvider s_sslProviderRogue;
    protected static SSLSocketProvider s_sslProviderWorker;
    }
