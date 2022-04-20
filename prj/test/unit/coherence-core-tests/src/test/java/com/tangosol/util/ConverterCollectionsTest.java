/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.function.Remote;

import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

/**
 * A test suit for ConverterCollections.
 *
 * @author hr  2020.11.10
 */
public class ConverterCollectionsTest
    {
    @Test
    public void testConvertNamedCache()
        {
        NamedCache<Integer, Integer> cacheInts = new WrapperNamedCache<>(new HashMap<>(), "ints");

        cacheInts.put(1, 1);
        cacheInts.put(2, 1);
        cacheInts.put(3, 1);

        NamedCache<String, String> cacheStrings = ConverterCollections.getNamedCache(cacheInts,
                Object::toString, Integer::parseInt, Object::toString, Integer::parseInt);

        assertThat(cacheStrings, allOf(hasEntry("1", "1"), hasEntry("2", "1"), hasEntry("3", "1")));
        assertEquals(3, cacheStrings.size());

        String sVal = cacheStrings.invoke("1", Entry::getValue);
        assertEquals("1", sVal);

        /*
        Note: the following use case is *not* supported as the function has to
              work against the raw types

        sVal = cacheStrings.invoke("1", entry -> entry.getValue().toUpperCase());
        assertEquals("1", sVal);
        */

        cacheStrings.putIfAbsent("4", "1");
        assertThat(cacheInts, hasEntry(4, 1));

        cacheStrings.remove("2", "1");
        assertFalse(cacheStrings.containsKey("2"));
        assertFalse(cacheInts.containsKey(2));

        cacheStrings.replace("4", "1", "2");
        assertThat(cacheStrings, hasEntry("4", "2"));
        assertThat(cacheInts, hasEntry(4, 2));

        cacheStrings.replace("4", "3");
        assertThat(cacheStrings, hasEntry("4", "3"));
        assertThat(cacheInts, hasEntry(4, 3));

        Remote.BiFunction functionMerge =
                (valOld, valNew) -> ((Integer) valOld) + ((Integer) valNew);

        assertEquals("7",
                cacheStrings.merge("4", "4", functionMerge));
        assertThat(cacheStrings, hasEntry("4", "7"));
                assertThat(cacheInts, hasEntry(4, 7));
        }
    }
