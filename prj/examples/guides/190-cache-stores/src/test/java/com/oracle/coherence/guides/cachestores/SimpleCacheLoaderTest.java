/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.cachestores;

import com.tangosol.net.NamedMap;
import com.tangosol.net.cache.CacheLoader;
import com.tangosol.net.cache.TypeAssertion;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A simple test class showing a simple {@link CacheLoader}.
 *
 * @author Tim Middleton  2021.02.17
 */
// #tag::class[]
public class SimpleCacheLoaderTest
        extends AbstractCacheStoreTest {

    @BeforeAll
    public static void startup() {
        startupCoherence("simple-cache-loader-cache-config.xml"); // <1>
    }

    @Test
    public void testSimpleCacheLoader() {
        NamedMap<Integer, String> namedMap = getSession()
                .getMap("simple-test", TypeAssertion.withTypes(Integer.class, String.class)); // <2>

        namedMap.clear();
        
        // initial get will cause read-through and the object is placed in the cache and returned to the user
        assertEquals("Number 1", namedMap.get(1));  // <3>
        assertEquals(1, namedMap.size());

        // subsequent get will not cause read-through as value is already in cache
        assertEquals("Number 1", namedMap.get(1));  // <4>

        // Remove the cache entry will cause a read-through again
        namedMap.remove(1);  // <5>
        assertEquals("Number 1", namedMap.get(1));
        assertEquals(1, namedMap.size());

        // load multiple keys will load all values
        namedMap.getAll(new HashSet<>(Arrays.asList(2, 3, 4)));  // <6>
        assertEquals(4, namedMap.size());
    }
}
// #end::class[]
