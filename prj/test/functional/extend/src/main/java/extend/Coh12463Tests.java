/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;

import com.tangosol.net.NamedCache;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the TcpAcceptor's scatter/gather buffer pool allocation.
 *
 * @author phf  2014.11.10
 */
public class Coh12463Tests
    extends AbstractExtendTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public Coh12463Tests()
        {
        super(CACHE_COH12463TESTS, FILE_CLIENT_SIMPLE_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy(CACHE_COH12463TESTS, FILE_SERVER_SIMPLE_CFG_CACHE);
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer(CACHE_COH12463TESTS);
        }

    // ----- Coh12463Tests methods ------------------------------------------

    /**
     * Send requests right around the pool buffer size to verify the Acceptor's calculation
     * of the bytes to use in the last buffer is correct.
     */
    @Test
    public void testCOH12463()
        {
        NamedCache cache                 = getNamedCache();
        int        cDoublePoolBufferSize = (new DefaultTcpAcceptorDependencies.PoolConfig()).getBufferSize() * 2;

        for (int i = -100; i < 100; ++i)
            {
            // test with both a "all zeroes" array and then one initialized to different values to
            // increase likelihood of hitting a message with exactly 2 times the pool buffer size
            byte[] val = new byte[cDoublePoolBufferSize + i];
            cache.put("key", val);
            for (int j = 0; j < val.length; ++j)
                {
                val[j] = (byte) j;
                }
            cache.put("key", val);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Cache name: "Coh12463Tests"
     */
    public static String CACHE_COH12463TESTS = "Coh12463Tests";
    }
