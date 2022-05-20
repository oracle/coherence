/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest.netty;

import rest.AbstractServerSentEventsTests;

import org.junit.AfterClass;
import org.junit.BeforeClass;

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
        doStartCacheServer("NettyRestTests", FILE_SERVER_CFG_CACHE);
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("NettyRestTests");
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_SERVER_CFG_CACHE = "server-cache-config-netty.xml";
    }
