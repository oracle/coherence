/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest;

import org.junit.BeforeClass;
import org.junit.Ignore;


/**
* A collection of functional tests for Coherence*Extend-REST that use
 * GlassFish.
 *
 * @author vp 2011.07.21
 */
@Ignore
public class GlassFishTests
        extends AbstractServerSentEventsTests
    {

    // ----- constructors ---------------------------------------------------

    public GlassFishTests()
        {
        super(FILE_SERVER_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractRestTests._startup();
        }

    // ----- AbstractRestTests methods --------------------------------------

    /**
     * Return the address that the GlassFish server is listening on.
     *
     * @return the listen address of the GlassFish server
     */
    @Override
    public String getAddress()
        {
        return System.getProperty("test.glassfish.address", "127.0.0.1");
        }

    /**
     * Return the port that the GlassFish server is listening on.
     *
     * @return the listen port of the GlassFish server
     */
    @Override
    public int getPort()
        {
        return Integer.getInteger("test.glassfish.port", 8080);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public String getContextPath()
        {
        return System.getProperty("test.glassfish.context", "/rest");
        }

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_SERVER_CFG_CACHE = "server-cache-config-glassfish.xml";
    }