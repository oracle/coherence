/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package security;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.tangosol.coherence.rest.providers.JacksonMapperProvider;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.net.ssl.HostnameVerifier;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;

/**
 * Simple test of authorizing a Coherence*Extend REST client using the
 * Netty HTTP server.
 *
 * @author lh  2014.12.04
 */
public class NettyRestSecurityTests
        extends AbstractRestSecurityTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public NettyRestSecurityTests()
        {
        super(FILE_CFG_CACHE);
        }

    // ----- lifecycle methods ----------------------------------------------

    /**
     * Start the cache server for this test class.
     */
    @BeforeClass
    public static void startServer()
        {
        CoherenceClusterMember clusterMember = startCacheServer("NettyRestSecurityTests", "security", FILE_CFG_CACHE);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("HttpProxyService"), is(true));
        }

    /**
     * Stop the cache server for this test class.
     */
    @AfterClass
    public static void stopServer()
        {
        stopCacheServer("NettyRestSecurityTests");
        }

    /**
     * Create a new HTTP client.
     *
     * @param sUsername  the name of the user associated with the client
     *
     * @return a new HTTP client
     */
    @Override protected Client createHttpClient(String sUsername)
        {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        return ClientBuilder.newBuilder()
                .withConfig(clientConfig)
                .register(JacksonMapperProvider.class)
                .register(JacksonFeature.class).build()
                .property(ClientProperties.READ_TIMEOUT, 5000);
        }

    /**
     * Create a new HTTPS client.
     *
     * @param provider  the SSLSocketProvider used by the HTTPS client
     *
     * @return a new HTTPS client
     */
    @Override protected Client createHttpsClient(SSLSocketProvider provider)
        {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        return ClientBuilder.newBuilder()
                .withConfig(clientConfig)
                .register(JacksonMapperProvider.class)
                .register(JacksonFeature.class)
                .hostnameVerifier((s, sslSession) -> true)
                .sslContext(provider.getDependencies().getSSLContext()).build()
                .property(ClientProperties.READ_TIMEOUT, 5000);
        }

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CFG_CACHE = "rest-cache-config-netty.xml";
    }
