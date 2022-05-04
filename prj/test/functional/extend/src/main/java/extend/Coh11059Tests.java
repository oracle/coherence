/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;


import com.tangosol.net.NamedCache;

import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.processor.ConditionalRemove;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
* Coherence*Extend test for the COH-11059.
*
* @author par  2014.02.19
*/
public class Coh11059Tests
        extends AbstractExtendTest
    {
    // ----- constructors ---------------------------------------------------

    public Coh11059Tests() 
        {
        super("repl-extend-direct", FILE_CLIENT_SIMPLE_CFG_CACHE);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        Properties props = new Properties();
        props.setProperty("test.extend.enabled","false");
        startCacheServer("Coh11059TestServer", "extend", FILE_SERVER_SIMPLE_CFG_CACHE, props);
        startCacheServerWithProxy("Coh11059TestProxy", FILE_SERVER_SIMPLE_CFG_CACHE);
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("Coh11059TestServer");
        stopCacheServer("Coh11059TestProxy");
        }

    /**
    * Test COH-11059.
    */
    @Test
    public void testCoh11059()
        {
        NamedCache cache           = getNamedCache();
        int        numberOfEntries = 10;

        Map putAllMap = new HashMap();
        for (int i = 0; i < numberOfEntries; i++)
            {
            putAllMap.put(i, i);
            }
        cache.putAll(putAllMap);
        assertEquals(numberOfEntries, cache.size());

        // Remove all entries from the cache using ConditionalRemove EntryProcessor 
        AlwaysFilter filter = new AlwaysFilter();
        ConditionalRemove removeProcessor = new ConditionalRemove(filter);
        cache.invokeAll(filter, removeProcessor);
        assertTrue(cache.isEmpty());

        cache.putAll(putAllMap);
        assertEquals(numberOfEntries, cache.size());
        }
    }
