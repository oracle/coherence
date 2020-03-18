/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package extend;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import common.TestMapListener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tangosol.net.NamedCache;

import com.tangosol.util.MapEvent;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
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
    }
