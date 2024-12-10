/*
 * Copyright (c) 2000-2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.simple;

import com.tangosol.net.Coherence;

import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A simple test class showing basic Coherence {@link com.tangosol.net.NamedMap}
 * CRUD operations.
 *
 * @author Jonathan Knight  2021.01.12
 */
class BasicCrudTest {
    // # tag::bootstrap[]
    @BeforeAll
    static void boostrapCoherence() {
        Coherence coherence = Coherence.clusterMember(); // <1>
        CompletableFuture<Coherence> future = coherence.start(); // <2>
        future.join(); // <3>
    }
    // # end::bootstrap[]

    // # tag::cleanup[]
    @AfterAll
    static void shutdownCoherence() {
        Coherence coherence = Coherence.getInstance(); //<1>
        coherence.close();
    }
    // # end::cleanup[]

    // # tag::put[]
    @Test
    void shouldPutNewKeyAndValue() {
        NamedMap<String, String> map = getMap("data"); // <1>
        String oldValue = map.put("key-1", "value-1"); // <2>

        assertNull(oldValue); // <3>
    }
    // # end::put[]

    // # tag::put-existing[]
    @Test
    void shouldPutExistingKeyAndValue() {
        NamedMap<String, String> map = getMap("data");
        map.put("key-2", "value-1");

        String oldValue = map.put("key-2", "value-2");
        assertEquals("value-1", oldValue);
    }
    // # end::put-existing[]

    // # tag::get[]
    @Test
    void shouldGet() {
        NamedMap<String, String> map = getMap("data"); // <1>
        map.put("key-3", "value-1"); // <2>

        String value = map.get("key-3"); // <3>

        assertEquals("value-1", value);
    }
    // # end::get[]

    // # tag::get-all[]
    @Test
    void shouldGetAll() {
        NamedMap<String, String> map = getMap("data"); // <1>

        map.put("key-5", "value-5"); // <2>
        map.put("key-6", "value-6");
        map.put("key-7", "value-7");

        Map<String, String> results = map.getAll(Arrays.asList("key-5", "key-7", "key-8")); // <3>

        assertEquals(2, results.size()); // <4>
        assertEquals("value-5", results.get("key-5")); // <5>
        assertEquals("value-7", results.get("key-7")); // <6>
    }
    // # end::get-all[]

    // # tag::remove[]
    @Test
    void shouldRemove() {
        NamedMap<String, String> map = getMap("data"); // <1>
        map.put("key-9", "value-9"); // <2>

        String oldValue = map.remove("key-9"); // <3>

        assertEquals("value-9", oldValue); // <4>
    }
    // # end::remove[]

    // # tag::remove-mapping[]
    @Test
    void shouldRemoveMapping() {
        NamedMap<String, String> map = getMap("data"); // <1>
        map.put("key-10", "value-10"); // <2>

        boolean removed = map.remove("key-10", "Foo"); // <3>
        assertFalse(removed);

        removed = map.remove("key-10", "value-10"); // <4>
        assertTrue(removed);
    }
    // # end::remove-mapping[]


    // # tag::put-expiry[]
    @Test
    void shouldPutWithExpiry() throws Exception {
        Coherence coherence = Coherence.getInstance();
        Session   session   = coherence.getSession();

        NamedCache<String, String> cache = session.getCache("test"); // <1>

        cache.put("key-1", "value-1", 2000); // <2>

        String value = cache.get("key-1"); // <3>
        assertEquals("value-1", value);

        Thread.sleep(3000); // <4>

        value = cache.get("key-1"); // <5>
        assertNull(value);
    }
    // # end::put-expiry[]


    // # tag::get-map[]
    <K, V> NamedMap<K, V> getMap(String name) {
        Coherence coherence = Coherence.getInstance(); // <1>
        Session   session   = coherence.getSession(); // <2>
        return session.getMap(name); // <3>
    }
    // # end::get-map[]
    }
