/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest;

import javax.ws.rs.client.ClientBuilder;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * A collection of functional tests for Coherence*Extend-REST that use the
 * default embedded HttpServer over SSL.
 *
 * @author jh 2012.01.10
 */
public class DefaultSSLRestTests
        extends AbstractRestTests
    {

    // ----- constructors ---------------------------------------------------

    public DefaultSSLRestTests()
        {
        super(FILE_SERVER_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        doStartCacheServer("DefaultSSLRestTests", FILE_SERVER_CFG_CACHE);
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DefaultSSLRestTests");
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
    public static String FILE_SERVER_CFG_CACHE = "server-cache-config-default-ssl.xml";
    }
