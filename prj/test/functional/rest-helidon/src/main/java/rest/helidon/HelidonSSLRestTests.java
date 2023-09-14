/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest.helidon;
import rest.AbstractServerSentEventsTests;

import jakarta.ws.rs.client.ClientBuilder;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * A collection of functional tests for Coherence*Extend-REST that use
 * Helidon as the embedded HttpServer.
 */
public class HelidonSSLRestTests
        extends AbstractServerSentEventsTests
    {
    public HelidonSSLRestTests()
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
        doStartCacheServer("HelidonSSLRestTests", FILE_SERVER_CFG_CACHE);
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("HelidonSSLRestTests");
        }

    // ----- AbstractRestTests methods --------------------------------------

    @Override
    protected ClientBuilder createClient()
        {
        return configureSSL(super.createClient());
        }

    @Override
    public String getProtocol()
        {
        return "https";
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_SERVER_CFG_CACHE = "server-cache-config-helidon-ssl.xml";
    }
