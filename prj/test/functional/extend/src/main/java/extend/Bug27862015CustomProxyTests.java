/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;


import com.tangosol.net.NamedCache;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * This test ensures the fix for Bug 27862015 does not regress.
 *
 * @author tam  2018.04.18
 */
public class Bug27862015CustomProxyTests
        extends AbstractExtendTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public Bug27862015CustomProxyTests()
        {
        super("test-cache", "client-cache-config-custom-proxy.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("Bug27862015CustomProxyTests", "server-cache-config-custom-proxy-bug27862015.xml");
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("Bug27862015CustomProxyTests");
        }

    // ----- Extend client tests --------------------------------------------

    /**
     * Test for Bug 27862015
     */
    @Test
    public void testBug27862015()
        {
        NamedCache cache = getFactory().ensureCache(m_sCache, getClass().getClassLoader());

        cache.clear();
        assertTrue(cache.isEmpty());
        String key = "test";
        cache.put(key,"foo");
        cache.remove(key);
        cache.get(key);
        }

    }
