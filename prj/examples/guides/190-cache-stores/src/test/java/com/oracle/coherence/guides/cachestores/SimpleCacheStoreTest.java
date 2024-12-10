/*
 * Copyright (c) 2000, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.cachestores;

import com.tangosol.net.NamedMap;
import com.tangosol.net.cache.CacheStore;
import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.util.Base;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * A simple test class showing a simple {@link CacheStore}.
 *
 * @author Tim Middleton  2021.02.17
 */
// #tag::class[]
public class SimpleCacheStoreTest
        extends AbstractCacheStoreTest {

    @BeforeAll
    public static void startup() {
        startupCoherence("simple-cache-store-cache-config.xml"); // <1>
    }

    @Test
    public void testSimpleCacheStore() {
        NamedMap<Integer, String> namedMap = getSession()
                .getMap("simple-test", TypeAssertion.withTypes(Integer.class, String.class)); // <2>

        namedMap.clear();
        
        // initial get will cause read-through and the object is placed in the cache and returned to the user
        assertEquals("Number 1", namedMap.get(1));  // <3>
        assertEquals(1, namedMap.size());

        // update the cache and the store method is called
        namedMap.put(1, "New Value"); // <4>
        assertEquals("New Value", namedMap.get(1));

        // remove the entry from the cache and the erase method is called
        assertEquals("New Value", namedMap.remove(1));  // <5>

        // Get the cache entry will cause a read-through again (cache loader)
        assertEquals("Number 1", namedMap.get(1));   // <6>
        assertEquals(1, namedMap.size());

        // Issue a puAll
        Map<Integer, String> map = new HashMap<>();
        map.put(2, "value 2");
        map.put(3, "value 3");
        map.put(4, "value 4");
        namedMap.putAll(map);  // <7>
        assertEquals(4, namedMap.size());

        // #tag::sleep[]
        Base.sleep(20000L);
        // #end::sleep[]
    }
}
// #end::class[]
