/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.coherence.management.internal.MapProvider;
import com.tangosol.internal.net.ssl.LegacyXmlSSLSocketProviderDependencies;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.junit.BeforeClass;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * ManagementInfoResourceSSLTests tests the ManagementInfoResource over SSL.
 *
 * @author lh 2019.01.30
 */
public class ManagementInfoResourceSSLTests
        extends ManagementInfoResourceTests
    {
    // ----- junit lifecycle methods ----------------------------------------

    @BeforeClass
    public static void _startup()
        {
        SERVER_PREFIX = "testMgmtRESTSSLServer";
        System.setProperty("coherence.management.http.provider", "mySSLProvider");
        System.setProperty("coherence.security.keystore", "file:server.jks");
        System.setProperty("coherence.security.store.password", "password");
        System.setProperty("coherence.security.key.password", "private");
        System.setProperty("coherence.security.truststore", "file:trust-server.jks");
        System.setProperty("coherence.security.trust.password", "password");

        ManagementInfoResourceTests._startup();
        }

    /**
     * Initialize the test class.
     * <p>
     * This method starts the Coherence cluster, if it isn't already running.
     */
    @BeforeClass
    public static void startup()
        {
        m_client = createSslClient(ClientBuilder.newBuilder()
                .register(MapProvider.class));
        }

    @BeforeClass
    public static void setupSSL()
        {
        XmlDocument xml = XmlHelper.loadFileOrResource("ssl-config-client.xml", null);

        s_sslProviderClient = new SSLSocketProvider(
                new LegacyXmlSSLSocketProviderDependencies(xml));
        }

    // ----- utility methods----------------------------------------------------

    /**
     * Configure HTTPS client, given a {@link ClientBuilder}.
     *
     * @param client  a client builder
     *
     * @return a client build with SSL configuration
     */
    protected static ClientBuilder configureSSL(ClientBuilder client)
        {
        return client
                .hostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .sslContext(s_sslProviderClient.getDependencies().getSSLContext());
        }

    /**
     * Create an HTTPS client, given a {@link ClientBuilder}.
     *
     * @param client  a client builder
     *
     * @return an HTTPS client
     */
    protected static Client createSslClient(ClientBuilder client)
        {
        return configureSSL(client).build();
        }

    // ----- data members ------------------------------------------------------

    /**
     * The SSL socket provider for the HTTPS client.
     */
    protected static SSLSocketProvider s_sslProviderClient;
    }
