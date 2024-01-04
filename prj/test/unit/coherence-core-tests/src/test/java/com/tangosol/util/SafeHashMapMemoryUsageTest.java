/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import org.openjdk.jol.info.GraphLayout;

import static org.junit.Assert.assertTrue;

/**
 * Guava testlib SafeHashMap tests.
 */
public final class SafeHashMapMemoryUsageTest
    {
    @Test
    public void testDefaultMemoryUsage()
        {
        Random                       random      = new Random();
        SafeHashMap<String, Integer> cache       = new SafeHashMap<>();
        GraphLayout                  emptyLayout = GraphLayout.parseInstance(cache);
        assertTrue("Total memory usage when empty increased by more than 5% to " + emptyLayout.totalSize(), emptyLayout.totalSize() < 184 * 1.05);

        Map map = new HashMap();
        for (int i = 0; i < 10; i++)
            {
            for (int j = 1; j <= 100000; j++)
                {
                map.put(Integer.toString(i * 100000 + j), random.nextInt(4));
                }
            cache.putAll(map);
            map.clear();
            }

        GraphLayout fullLayout = GraphLayout.parseInstance(cache);
        assertTrue("Total memory usage when increased by more than 5% to " + fullLayout.totalSize(), fullLayout.totalSize() < 87_200_248 * 1.05);
        }

    @Test
    public void testMemoryUsage()
        {
        Random                       random      = new Random();
        SafeHashMap<String, Integer> cache       = new SafeHashMap<>(1013, 3.0f, 3.0f);
        GraphLayout                  emptyLayout = GraphLayout.parseInstance(cache);
        assertTrue("Total memory usage when empty increased by more than 5% to " + emptyLayout.totalSize(), emptyLayout.totalSize() < 4168 * 1.05);

        Map map = new HashMap();
        for (int i = 0; i < 10; i++)
            {
            for (int j = 1; j <= 100000; j++)
                {
                map.put(Integer.toString(i * 100000 + j), random.nextInt(4));
                }
            cache.putAll(map);
            map.clear();
            }

        GraphLayout fullLayout = GraphLayout.parseInstance(cache);
        assertTrue("Total memory usage when increased by more than 5% to " + fullLayout.totalSize(), fullLayout.totalSize() < 85_200_264 * 1.05);
        }
    }