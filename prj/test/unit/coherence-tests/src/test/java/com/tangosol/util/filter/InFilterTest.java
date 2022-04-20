/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;

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

    @Parameterized.Parameters
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
        // create the mocks
        MapIndex index = mock(MapIndex.class);

        // create the InFilter to be tested
        IndexAwareFilter filter = new InFilter(IdentityExtractor.INSTANCE, new HashSet(Arrays.asList(3,2)));

        Map mapIndexes = new HashMap();
        mapIndexes.put(IdentityExtractor.INSTANCE, index);

        Map<String, Integer> mapFwd = new HashMap<>();
        mapFwd.put("key1", 1);
        mapFwd.put("key2", 2);
        mapFwd.put("key3", 2);
        mapFwd.put("key4", 4);
        mapFwd.put("key5", 1);
        mapFwd.put("key6", 2);
        mapFwd.put("key7", 3);

        // set mock expectations
        when(index.isOrdered()).thenReturn(m_fOrdered);
        when(index.isPartial()).thenReturn(m_fPartial);

        for (Entry<String, Integer> entry : mapFwd.entrySet())
            {
            when(index.get(entry.getKey())).thenReturn(entry.getValue());
            }

        if (m_fPartial)
            {
            mapFwd.put("key0", null);
            mapFwd.put("key8", null);
            mapFwd.put("key9", null);
            mapFwd.put("key10", null);
            }

        // begin test
        Set setKeys = new HashSet(mapFwd.keySet());
        filter.applyIndex(mapIndexes, setKeys);

        assertEquals(4, setKeys.size());

        assertTrue("key1 should not have been removed.", setKeys.contains("key2"));
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
