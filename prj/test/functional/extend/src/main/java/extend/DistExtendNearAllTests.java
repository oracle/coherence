/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheStatistics;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.NearCache;

import com.tangosol.util.MapEvent;

import com.oracle.coherence.testing.TestMapListener;

import data.Person;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.nullValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
* A collection of functional tests for Coherence*Extend that use the
* "dist-extend-near-all" cache.
*
* @author jh  2005.11.29
*/
public class DistExtendNearAllTests
        extends AbstractExtendTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistExtendNearAllTests()
        {
        super(CACHE_DIST_EXTEND_NEAR_ALL);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("DistExtendNearAllTests", "extend", FILE_SERVER_CFG_CACHE);
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistExtendNearAllTests");
        }

    /**
     * Test the behavior of {@link com.tangosol.net.NamedCache#truncate()}.
     */
    @Test
    public void testTruncate()
        {
        NamedCache cache = getNamedCache();
        TestMapListener listener = new TestMapListener();
        cache.addMapListener(listener);

        assertTrue(cache.isActive());
        cache.put("key", "value");
        assertEquals("value", cache.get("key"));

        MapEvent evt = listener.waitForEvent();
        assertNotNull("Missing event ", evt);

        cache.truncate();

        Eventually.assertThat(invoking(cache).get("key"), nullValue());

        // cache should still be active
        assertTrue(cache.isActive());

        // the listener should still be registered with the cache after truncate
        cache.put("key1", "value1");

        evt = listener.waitForEvent();
        assertNotNull("Missing event ", evt);

        cache.truncate();

        Eventually.assertThat(invoking(cache).get("key"), nullValue());

        assertTrue(cache.isActive());
        }

    /**
     * Test for COH-23095
     */
    @Test
    public void testCoh23095()
        {
        NearCache<Integer, Person> cache = (NearCache) getNamedCache();

        CacheStatistics cacheStats     = ((LocalCache) cache.getFrontMap()).getCacheStatistics();
        long            cInitialMisses = cacheStats.getCacheMisses();

        cache.computeIfAbsent(2,
            person -> new Person("4321", "frist", "lats", 2000, null, new String[0]));
        assertEquals(cacheStats.getCacheMisses(), cInitialMisses + 1);

        cache.getOrDefault(3,
            new Person("5678", "a", "b", 1980, null, new String[0]));
        // 2 misses in getOrDefault(), frontMap is checked twice
        assertEquals(cacheStats.getCacheMisses(), cInitialMisses + 3);
        }
    }
