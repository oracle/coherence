/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SetMapTest tests functionality in SetMap.
 *
 * @author hr  2016.09.29
 * @since 12.2.1.4.0
 */
public class SetMapTest
    {
    @Test
    public void test()
        {
        Set<Integer> listKeys = new LiteSet<>(Arrays.asList(1,2,3));
        Function<Integer, String> function = NKey -> "value: " + NKey;

        Map<Integer, String> map = new SetMap<>(listKeys, function);
        for (Integer NKey : listKeys)
            {
            assertEquals(function.apply(NKey), map.get(NKey));
            }

        map = new SetMap<>(listKeys, function);
        for (Map.Entry<Integer, String> entry : map.entrySet())
            {
            assertEquals(function.apply(entry.getKey()), entry.getValue());
            }

        map = new SetMap<>(listKeys, function);
        for (Integer NKey : listKeys)
            {
            assertTrue(map.containsKey(NKey));
            }
        assertFalse(map.containsKey(4));

        List<String> listValues = new ArrayList<>(listKeys.size());
        listKeys.forEach(NKey -> listValues.add(function.apply(NKey)));

        map = new SetMap<>(listKeys, function);
        for (String sValue : map.values())
            {
            assertTrue(listValues.contains(sValue));
            }

        map = new SetMap<>(listKeys, function);
        for (String sValue : listValues)
            {
            assertTrue(map.containsValue(sValue));
            }
        assertFalse(map.containsValue(function.apply(4)));
        }
    }
