/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package security;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;

/**
 * Simple test of authorizing a Coherence*Extend REST client using the
 * Netty HTTPS server.
 *
 * @author lh  2014.12.04
 */
public class NettySSLRestSecurityTests
        extends AbstractSSLRestSecurityTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public NettySSLRestSecurityTests()
        {
        super(FILE_CFG_CACHE_RSA);
        }

    // ----- lifecycle methods ----------------------------------------------

    /**
     * Start the cache server for this test class.
     */
    @BeforeClass
    public static void startServer()
        {
        CoherenceClusterMember clusterMember = startCacheServer("NettySSLRestSecurityTests", "security", FILE_CFG_CACHE_RSA);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("HttpProxyService"), is(true));
        }

    /**
     * Stop the cache server for this test class.
     */
    @AfterClass
    public static void stopServer()
        {
        stopCacheServer("NettySSLRestSecurityTests");
        }

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test for RSA algorithm.
    */
    static String FILE_CFG_CACHE_RSA = "rest-cache-config-netty-ssl-rsa.xml";
    }
