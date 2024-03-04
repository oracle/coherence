/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.filter;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;

import com.tangosol.util.Filter;
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
* LikeFilter unit tests
*
* @author tb 2010.2.08
*/
@RunWith(value = Parameterized.class)
public class LikeFilterTest
    {

    public LikeFilterTest(boolean fOrdered, boolean fPartial)
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
    * Test testLikeFilter (COH-4020)
    */
    @Test
    public void testLikeFilter()
        {
        String pattern = "%GetLogonData_getAccountIndicativeInfoInput_7137B2C9070C4951AE00EE5C4F01435A%";
        Filter filter = new LikeFilter(IdentityExtractor.INSTANCE, pattern, ' ', true);
        assertTrue(filter.evaluate("xGetLogonDataxgetAccountIndicativeInfoInputx7137B2C9070C4951AE00EE5C4F01435Axxx"));
        }

    /**
    * Test applyIndex
    */
    @Test
    public void testApplyIndex()
        {
        // create the mocks
        MapIndex index = mock(MapIndex.class);

        // create the LikeFilter to be tested
        IndexAwareFilter filter = new LikeFilter(IdentityExtractor.INSTANCE, "foo%", (char) 0, false);

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

        Map mapInverse = new HashMap();
        mapInverse.put("foo1", new HashSet(Arrays.asList("key1")));
        mapInverse.put("bar2", new HashSet(Arrays.asList("key2")));
        mapInverse.put("foo3", new HashSet(Arrays.asList("key3")));
        mapInverse.put("bar4", new HashSet(Arrays.asList("key4")));

        // set mock expectations
        when(index.isOrdered()).thenReturn(m_fOrdered);
        when(index.isPartial()).thenReturn(m_fPartial);
        when(index.get("key1")).thenReturn("foo1");
        when(index.get("key2")).thenReturn("bar2");
        when(index.get("key3")).thenReturn("foo3");
        when(index.get("key4")).thenReturn("bar4");
        when(index.getIndexContents()).thenReturn(mapInverse);

        // begin test
        assertEquals(2, filter.calculateEffectiveness(mapIndexes, setKeys));

        filter.applyIndex(mapIndexes, setKeys);

        assertEquals("Two keys should remain in the set of keys.",
                2, setKeys.size());

        assertTrue("key1 should remain.", setKeys.contains("key1"));
        assertTrue("key3 should remain.", setKeys.contains("key3"));
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
