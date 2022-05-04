/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import java.io.IOException;

import java.net.Socket;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A collection of functional tests for a Coherence*Extend proxy that is
 * missing a configured filter.
 *
 * @author he  2009.09.28
 */
public class MissingFilterTests
        extends AbstractExtendTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public MissingFilterTests()
        {
        super(AbstractExtendTests.CACHE_DIST_EXTEND_DIRECT, AbstractExtendTests.FILE_CLIENT_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("MissingFilterTests", "extend", "missing-filter-cache-config.xml");
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("MissingFilterTests");
        }

    // ----- MissingFilter tests --------------------------------------------

    /**
     * Attempt to connect to a Coherence*Extend proxy that is missing a
     * configured filter.
     */
    @Test
    public void connect()
        {
        try
            {
            getNamedCache();
            }
        catch (Exception e)
            {
            // try to connect to the proxy's TcpAcceptor port as an attempt
            // to verify that the TcpAcceptor thread has not been terminated
            try
                {
                Socket socket = new Socket(
                        System.getProperty(
                                "test.extend.address.remote", "127.0.0.1"),
                        Integer.parseInt(System.getProperty(
                                "test.extend.port", "9999")));
                socket.close();
                return;
                }
            catch (IOException ie)
                {
                fail(ie.getMessage());
                }
            }
        fail("expected exception");
        }
    }
