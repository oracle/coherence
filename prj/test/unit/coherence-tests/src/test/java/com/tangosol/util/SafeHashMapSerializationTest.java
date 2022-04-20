/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;


/**
* Unit test class for the {@link com.tangosol.util.SafeHashMap} class.
*
* @author Courtesy of Rohan Talip, Integralis
*/
public class SafeHashMapSerializationTest
        extends Base
    {
    @Test
    public void testSimplePutWithNonNulls()
        {
        Map map = new SafeHashMap();

        for (int i = 0; i < LARGE_NUMBER; i++)
            {
            Integer wrapper = i;
            map.put(wrapper, wrapper);
            }

        for (int i = 0; i < LARGE_NUMBER; i++)
            {
            Integer wrapper = i;
            assertTrue(equals(wrapper, map.get(wrapper)));
            }
        }

    @Test
    public void testSimplePutWithNull()
        {
        Map map = new SafeHashMap();

        for (int i = 0; i < LARGE_NUMBER; i++)
            {
            Integer wrapper = i;
            map.put(wrapper, null);
            }

        for (int i = 0; i < LARGE_NUMBER; i++)
            {
            Integer wrapper = i;
            assertTrue(map.containsKey(wrapper));
            assertTrue(map.get(wrapper) == null);
            }
        }

    @Test
    public void testPutWithNonNullsAfterDeserialising()
        {
        Map map = new SafeHashMap();

        for (int i = 0; i < MEDIUM_NUMBER; i++)
            {
            Integer wrapper = i;
            map.put(wrapper, wrapper);
            }

        Binary binary = ExternalizableHelper.toBinary(map);
        Map newMap = (Map) ExternalizableHelper.fromBinary(binary);

        for (int i = MEDIUM_NUMBER; i < 2 * MEDIUM_NUMBER; i++)
            {
            Integer wrapper = i;
            newMap.put(wrapper, wrapper);
            }

        for (int i = 0; i < 2 * MEDIUM_NUMBER; i++)
            {
            Integer wrapper = i;
            assertTrue(equals(wrapper, newMap.get(wrapper)));
            }
        }

    @Test
    public void testPutWithNullsAfterDeserialising()
        {
        Map map = new SafeHashMap();

        for (int i = 0; i < MEDIUM_NUMBER; i++)
            {
            Integer wrapper = i;
            map.put(wrapper, null);
            }

        Binary binary = ExternalizableHelper.toBinary(map);
        Map newMap = (Map) ExternalizableHelper.fromBinary(binary);

        for (int i = MEDIUM_NUMBER; i < 2 * MEDIUM_NUMBER; i++)
            {
            Integer wrapper = i;
            newMap.put(wrapper, null);
            }

        for (int i = 0; i < 2 * MEDIUM_NUMBER; i++)
            {
            Integer wrapper = i;
            assertTrue(newMap.containsKey(wrapper));
            assertTrue(newMap.get(wrapper) == null);
            }
        }

    private static final int MEDIUM_NUMBER = 10;
    private static final int LARGE_NUMBER  = 100000;
    }
