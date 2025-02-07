/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
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
 * Simple test of authorizing a Coherence*Extend REST client using a custom
 * callback handler to provide username and password.
 *
 * @author lh  2025.02.04
 */
public class SslRestCallBackHandlerTests
        extends AbstractSSLRestSecurityTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public SslRestCallBackHandlerTests()
        {
        super(FILE_CFG_CACHE_RSA);
        }

    // ----- lifecycle methods ----------------------------------------------

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("java.security.auth.login.config", "login.configRsa");
        System.setProperty("coherence.override", "tangosol-coherence-override-cb.xml");
        System.setProperty("coherence.security.config", "DefaultControllerRsa.xml");

        AbstractRestSecurityTests._startup();
        }

    /**
     * Start the cache server for this test class.
     */
    @BeforeClass
    public static void startServer()
        {
        CoherenceClusterMember clusterMember = startCacheServer("SslRestCallBackHandlerTests", "security", FILE_CFG_CACHE_RSA);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("HttpProxyService"), is(true));
        }

    /**
     * Stop the cache server for this test class.
     */
    @AfterClass
    public static void stopServer()
        {
        stopCacheServer("SslRestCallBackHandlerTests");
        }

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test for RSA algorithm.
    */
    static String FILE_CFG_CACHE_RSA = "rest-cache-config-default-ssl-rsa.xml";
    }
