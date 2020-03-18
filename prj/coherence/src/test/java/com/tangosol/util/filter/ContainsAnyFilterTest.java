/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
* ContainsAnyFilter unit tests
*
* @author tb 2010.2.08
*/
@RunWith(value = Parameterized.class)
public class ContainsAnyFilterTest
    {

    public ContainsAnyFilterTest(boolean fOrdered, boolean fPartial)
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

        // create the ContainsAnyFilter to be tested
        IndexAwareFilter filter = new ContainsAnyFilter(IdentityExtractor.INSTANCE, new HashSet(Arrays.asList(3,2)));

        Map mapIndexes = new HashMap();
        mapIndexes.put(IdentityExtractor.INSTANCE, index);

        Set setKeys = new HashSet();
        setKeys.add("key1");
        setKeys.add("key2");
        setKeys.add("key3");
        setKeys.add("key4");
        setKeys.add("key5");
        setKeys.add("key6");
        setKeys.add("key7");

        if (m_fPartial)
            {
            setKeys.add("key0");
            setKeys.add("key8");
            setKeys.add("key9");
            setKeys.add("key10");
            }

        Map mapInverse = new HashMap();
        mapInverse.put(1, new HashSet(Arrays.asList("key1", "key3", "key5")));
        mapInverse.put(2, new HashSet(Arrays.asList("key2", "key3", "key6")));
        mapInverse.put(3, new HashSet(Arrays.asList("key2", "key3", "key7")));
        mapInverse.put(4, new HashSet(Arrays.asList("key2", "key4")));

        // set mock expectations
        when(index.isOrdered()).thenReturn(m_fOrdered);
        when(index.isPartial()).thenReturn(m_fPartial);
        when(index.getIndexContents()).thenReturn(mapInverse);

        // begin test
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
