/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;

import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.SimpleMapIndex;
import com.tangosol.util.ValueExtractor;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;

import com.tangosol.util.MapIndex;
import com.tangosol.util.extractor.IdentityExtractor;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
* InFilter unit tests
*
* @author tb 2010.2.08
*/
@RunWith(value = Parameterized.class)
public class InFilterTest
    {

    public InFilterTest(boolean fOrdered, boolean fPartial)
        {
        m_fOrdered = fOrdered;
        m_fPartial = fPartial;
        }

    @Parameterized.Parameters(name ="ordered={0} partial={1}")
    public static Collection data() {
       Object[][] data = new Object[][]
            { new Object[] {Boolean.FALSE, Boolean.FALSE},
              new Object[] {Boolean.FALSE, Boolean.TRUE},
              new Object[] {Boolean.TRUE , Boolean.FALSE},
              new Object[] {Boolean.TRUE , Boolean.TRUE}
            };
       return Arrays.asList(data);
    }

    /**
    * Test applyIndex
    */
    @Test
    public void testApplyIndex()
        {
        // create the index
        MapIndex<String, Integer, Integer> index = new SimpleMapIndex(IdentityExtractor.INSTANCE, m_fOrdered, null, null);

        // create the InFilter to be tested
        InFilter<Integer, Integer> filter = new InFilter<>(IdentityExtractor.INSTANCE(), Set.of(3, 2));

        Map<ValueExtractor<?, ?>, MapIndex<?, ?, ?>> mapIndexes = new HashMap<>();
        mapIndexes.put(IdentityExtractor.INSTANCE, index);

        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 1);
        map.put("key2", 2);
        map.put("key3", 2);
        map.put("key4", 4);
        map.put("key5", 1);
        map.put("key6", 2);
        map.put("key7", 3);

        if (m_fPartial)
            {
            map.put("key0", null);
            map.put("key8", null);
            map.put("key9", null);
            map.put("key10", null);
            }

        for (Entry<String, Integer> entry : map.entrySet())
            {
            index.insert(entry);
            }

        // begin test
        Set<String> setKeys = new HashSet<>(map.keySet());
        assertEquals(4, filter.calculateEffectiveness(mapIndexes, setKeys));

        filter.applyIndex(mapIndexes, setKeys);

        assertEquals(4, setKeys.size());

        assertTrue("key2 should not have been removed.", setKeys.contains("key2"));
        assertTrue("key3 should not have been removed.", setKeys.contains("key3"));
        assertTrue("key6 should not have been removed.", setKeys.contains("key6"));
        assertTrue("key7 should not have been removed.", setKeys.contains("key7"));
        }
    
    /**
    * Run the test with an ordered index.
    */
    private boolean m_fOrdered;

    /**
    * Run the test with an partial index.
    */
    private boolean m_fPartial;
    }
