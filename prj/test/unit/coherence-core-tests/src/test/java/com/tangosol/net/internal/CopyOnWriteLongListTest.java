/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * StorageIdListTest is responsible for ...
 *
 * @author hr  2016.12.01
 */
public class CopyOnWriteLongListTest
    {
    @Test
    public void testAdd()
        {
        doAdd(new long[] {-20L, -10L, 5L, 10L, -15L, -20L});

        doAdd(new long[] {10L, 5L, 20L, 15L, 1L});

        doAdd(new long[] {-10L, -20L, -5L, -15L, 10L});

        doAdd(new long[] {-10L, -20L, -5L, -15L, 10L, 20L, 15L, -100L, 30L});
        }

    @Test
    public void testRemove()
        {
        CopyOnWriteLongList list = doAdd(new long[] {-20L, -10L, 5L, 10L, 20L, 30L, 40L});

        list.remove(40L);
        assertFalse(list.contains(40L));
        System.out.println(list);

        list.remove(-20L);
        assertFalse(list.contains(-20L));
        System.out.println(list);

        list.remove(5L);
        assertFalse(list.contains(5L));
        System.out.println(list);

        list.remove(10L);
        assertFalse(list.contains(10L));
        System.out.println(list);
        }

    protected CopyOnWriteLongList doAdd(long[] alCacheIds)
        {
        CopyOnWriteLongList list = new CopyOnWriteLongList();
        for (long lCacheId : alCacheIds)
            {
            list.add(lCacheId);

            assertTrue(list.contains(lCacheId));
            System.out.println(list);
            }
        return list;
        }
    }
