/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.filter;

import com.oracle.coherence.common.collections.NullableSortedMap;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;

import com.tangosol.util.MapIndex;
import com.tangosol.util.extractor.IdentityExtractor;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
* LessFilter unit tests
*
* @author tb 2010.2.08
*/
@SuppressWarnings({"unchecked", "rawtypes"})
@RunWith(value = Parameterized.class)
public class LessFilterTest
    {

    public LessFilterTest(boolean fOrdered, boolean fPartial)
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
        // create the mocks
        MapIndex index = mock(MapIndex.class);

        // create the LessFilter to be tested
        IndexAwareFilter filter = new LessFilter(IdentityExtractor.INSTANCE, 3);

        Map mapIndexes = new HashMap();
        mapIndexes.put(IdentityExtractor.INSTANCE, index);

        Set setKeys = new HashSet();
        setKeys.add("key1");
        setKeys.add("key2");
        setKeys.add("key3");
        setKeys.add("key4");
        setKeys.add("key5");

        if (m_fPartial)
            {
            setKeys.add("key0");
            setKeys.add("key6");
            setKeys.add("key7");
            }

        Map mapInverse = new NullableSortedMap();
        mapInverse.put(1, new HashSet(List.of("key1")));
        mapInverse.put(2, new HashSet(List.of("key2")));
        mapInverse.put(3, new HashSet(List.of("key3")));
        mapInverse.put(4, new HashSet(List.of("key4")));
        mapInverse.put(5, new HashSet(List.of("key5")));

        // set mock expectations
        when(index.isOrdered()).thenReturn(m_fOrdered);
        when(index.isPartial()).thenReturn(m_fPartial);
        when(index.getIndexContents()).thenReturn(mapInverse);

        // begin test
        assertEquals(2, filter.calculateEffectiveness(mapIndexes, setKeys));

        filter.applyIndex(mapIndexes, setKeys);

        assertEquals("Two keys should remain in the set of keys.",
                2, setKeys.size());

        assertTrue("key2 should have been retained.", setKeys.contains("key2"));
        assertTrue("key1 should have been retained.", setKeys.contains("key1"));
        }

    /**
    * Run the test with an ordered index.
    */
    private final boolean m_fOrdered;

    /**
    * Run the test with a partial index.
    */
    private final boolean m_fPartial;
    }
