/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
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

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CFG_CACHE = "rest-cache-config-netty.xml";
    }
