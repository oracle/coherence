/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.filter;

import com.tangosol.util.MapIndex;
import com.tangosol.util.Filter;
import com.tangosol.util.SafeSortedMap;
import com.tangosol.util.extractor.IdentityExtractor;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
* AllFilter unit tests
*
* @author tb 2010.2.08
*/
@RunWith(value = Parameterized.class)
public class AllFilterTest
    {

    public AllFilterTest(boolean fOrdered, boolean fPartial)
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

        // create the AllFilter to be tested
        IndexAwareFilter filter = new AllFilter(new Filter[] {
                new GreaterFilter(IdentityExtractor.INSTANCE, 1),
                new LessFilter(IdentityExtractor.INSTANCE, 4)});

        Map mapIndexes = new HashMap();
        mapIndexes.put(IdentityExtractor.INSTANCE, index);

        Set setKeys = new HashSet();
        setKeys.add("key1");
        setKeys.add("key2");
        setKeys.add("key3");
        setKeys.add("key4");

        if (m_fPartial)
            {
            setKeys.add("key0");
            setKeys.add("key5");
            setKeys.add("key6");
            setKeys.add("key7");
            }

        Map mapInverse = new SafeSortedMap();
        mapInverse.put(1, new HashSet(Arrays.asList("key1")));
        mapInverse.put(2, new HashSet(Arrays.asList("key2")));
        mapInverse.put(3, new HashSet(Arrays.asList("key3")));
        mapInverse.put(4, new HashSet(Arrays.asList("key4")));

        // set mock expectations
        when(index.isOrdered()).thenReturn(m_fOrdered);
        when(index.isPartial()).thenReturn(m_fPartial);
        when(index.getIndexContents()).thenReturn(mapInverse);

        // begin test
        filter.applyIndex(mapIndexes, setKeys);

        assertEquals("Two keys should remain in the set of keys.",
                2, setKeys.size());

        assertTrue("key3 should remain.", setKeys.contains("key3"));
        assertTrue("key2 should remain.", setKeys.contains("key2"));
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
