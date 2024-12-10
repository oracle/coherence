/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package security;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;

/**
 * Simple test of authorizing a Coherence*Extend REST client using Sun's
 * lightweight HTTP server.
 *
 * @author jh  2011.01.11
 */
public class DefaultRestSecurityTests
        extends AbstractRestSecurityTests
    {

    // ----- contructors ----------------------------------------------------

    /**
     * Default constructor.
     */
    public DefaultRestSecurityTests()
        {
        super(FILE_CFG_CACHE);
        System.setProperty("coherence.override", "security-coherence-override.xml");
        }

    // ----- lifecycle methods ----------------------------------------------

    /**
     * Start the cache server for this test class.
     */
    @BeforeClass
    public static void startServer()
        {
        CoherenceClusterMember clusterMember = startCacheServer("DefaultRestSecurityTests", "security", FILE_CFG_CACHE);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("HttpProxyService"), is(true));
        }

    /**
     * Stop the cache server for this test class.
     */
    @AfterClass
    public static void stopServer()
        {
        stopCacheServer("DefaultRestSecurityTests");
        }

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CFG_CACHE = "rest-cache-config-default.xml";
    }
