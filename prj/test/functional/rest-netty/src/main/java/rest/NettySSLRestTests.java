/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest;


import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.tangosol.coherence.rest.providers.JacksonMapperProvider;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;


/**
 * A collection of functional tests for Coherence*Extend-REST that use
 * Netty as the embedded HttpServer.
 *
 * @author lh 2015.12.16
 */
public class NettySSLRestTests
        extends AbstractServerSentEventsTests
    {
    public NettySSLRestTests()
        {
        super(FILE_SERVER_CFG_CACHE);
        }

    // -- --- test lifecycle ------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember clusterMember = startCacheServer("NettySSLRestTests", "rest", FILE_SERVER_CFG_CACHE);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("ExtendHttpProxyService"), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("NettySSLRestTests");
        }

    // ----- AbstractRestTests methods --------------------------------------

    @Override
    protected ClientBuilder createClient()
        {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        return configureSSL(ClientBuilder.newBuilder()
                .withConfig(clientConfig)
                .register(JacksonMapperProvider.class)
                .register(JacksonFeature.class));
        }

    @Override
    public String getProtocol()
        {
        return "https";
        }

    /**
     * Return the HTTP client.
     *
     * @return context path
     */
    @Override public Client getClient()
        {
        if (m_client == null)
            {
            m_client = createClient().build();
            m_client.property(ClientProperties.READ_TIMEOUT, 5000);
            }

        return m_client;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_SERVER_CFG_CACHE = "server-cache-config-netty-ssl.xml";
    }
