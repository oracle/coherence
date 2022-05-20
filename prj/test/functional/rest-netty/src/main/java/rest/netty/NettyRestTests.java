/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest.netty;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.coherence.rest.providers.JacksonMapperProvider;

import rest.AbstractServerSentEventsTests;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import static org.hamcrest.CoreMatchers.is;

/**
 * A collection of functional tests for Coherence*Extend-REST that use
 * Netty as the embedded HttpServer.
 *
 * @author lh 2015.12.16
 */
public class NettyRestTests
        extends AbstractServerSentEventsTests
    {
    public NettyRestTests()
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
        System.setProperty("coherence.override", "rest-tests-coherence-override.xml");
        CoherenceClusterMember clusterMember = startCacheServer("NettyRestTests", "rest", FILE_SERVER_CFG_CACHE);
        Eventually.assertDeferred(() -> clusterMember.isServiceRunning("ExtendHttpProxyService"), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("NettyRestTests");
        }

    /**
     * Create a new HTTP client.
     *
     * @return a new HTTP client
     */
    @Override protected ClientBuilder createClient()
        {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        return ClientBuilder.newBuilder()
                .withConfig(clientConfig)
                .register(JacksonMapperProvider.class)
                .register(JacksonFeature.class);
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
            m_client.property(ClientProperties.READ_TIMEOUT, 10000);
            }

        return m_client;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_SERVER_CFG_CACHE = "server-cache-config-netty.xml";
    }
