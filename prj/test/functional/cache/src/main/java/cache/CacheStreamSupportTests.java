/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.tangosol.util.stream.RemoteCollectors;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tangosol.net.NamedCache;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Functional tests for stream support.
 *
 * @author bbc  2021.09.14
 */

public class CacheStreamSupportTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public CacheStreamSupportTests()
        {
        super(FILE_CFG_CACHE);
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

        AbstractFunctionalTest._startup();
        }


    @Test
    public void testRemoteStreamSupport()
        {
        NamedCache<Integer, Integer> cache = getNamedCache("dist-default");
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);

        List<Integer> listKeys = cache.keySet().stream().collect(RemoteCollectors.toList());
        assertEquals(3, listKeys.size());

        List<Integer> listValues = cache.values().stream().collect(RemoteCollectors.toList());
        assertEquals(3, listValues.size());

        List<Map.Entry<Integer, Integer>> listEntries = cache.entrySet().stream().collect(RemoteCollectors.toList());
        assertEquals(3, listEntries.size());

        cache.put(4, 4);
        cache.put(5, 5);

        listKeys = cache.keySet().stream().filter(x -> x > 3).collect(RemoteCollectors.toList());
        assertEquals(2, listKeys.size());
        }

    @Test
    public void testLocalCacheStreamSupport()
        {
        NamedCache<Integer, Integer> cache = getNamedCache("local");

        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);

        List<Integer> listKeys = cache.keySet().stream().collect(RemoteCollectors.toList());
        assertEquals(3, listKeys.size());

        List<Integer> listValues = cache.values().stream().collect(RemoteCollectors.toList());
        assertEquals(3, listValues.size());

        List<Map.Entry<Integer, Integer>> listEntries = cache.entrySet().stream().collect(RemoteCollectors.toList());
        assertEquals(3, listEntries.size());

       listKeys = cache.keySet().stream().collect(Collectors.toList());
        assertEquals(3, listKeys.size());

        listValues = cache.values().stream().collect(Collectors.toList());
        assertEquals(3, listValues.size());

        listEntries = cache.entrySet().stream().collect(Collectors.toList());
        assertEquals(3, listEntries.size());
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_CFG_CACHE = "map-cache-config.xml";
    }
