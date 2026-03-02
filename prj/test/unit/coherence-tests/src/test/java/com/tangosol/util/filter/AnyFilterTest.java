/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.filter;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;

import com.tangosol.util.MapIndex;
import com.tangosol.util.Filter;
import com.tangosol.util.SimpleMapIndex;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.IdentityExtractor;

import java.lang.reflect.Field;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
* AnyFilter unit tests
*
* @author tb 2010.2.08
*/
@RunWith(value = Parameterized.class)
public class AnyFilterTest
    {

    public AnyFilterTest(boolean fOrdered, boolean fPartial)
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

        // create the AnyFilter to be tested
        IndexAwareFilter filter = new AnyFilter(new Filter[] {
                new EqualsFilter(IdentityExtractor.INSTANCE, 3),
                new BetweenFilter(IdentityExtractor.INSTANCE, 2, 3)});

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
        mapInverse.put(1, new HashSet(Arrays.asList("key1")));
        mapInverse.put(2, new HashSet(Arrays.asList("key2")));
        mapInverse.put(3, new HashSet(Arrays.asList("key3")));
        mapInverse.put(4, new HashSet(Arrays.asList("key4")));

        // set mock expectations
        when(index.isOrdered()).thenReturn(m_fOrdered);
        when(index.isPartial()).thenReturn(m_fPartial);
        when(index.getIndexContents()).thenReturn(mapInverse);

        // begin test
        assertEquals(2, filter.calculateEffectiveness(mapIndexes, setKeys));

        filter.applyIndex(mapIndexes, setKeys);

        assertEquals("Unexpected number of keys after applyIndex.", 2, setKeys.size());

        assertTrue("key3 should remain.", setKeys.contains("key3"));
        assertTrue("key2 should remain.", setKeys.contains("key2"));
        }

    @Test
    public void shouldReuseOptimizedOrderBetweenCalculateAndApply()
        {
        MapIndex index = new SimpleMapIndex(IdentityExtractor.INSTANCE, false, null, null);

        Map<String, Object> map = Map.of(
                "key1", 1,
                "key2", 2,
                "key3", 3,
                "key4", 4
        );
        map.entrySet().forEach(index::insert);

        Map<ValueExtractor, MapIndex> mapIndexes = new HashMap<>();
        mapIndexes.put(IdentityExtractor.INSTANCE, index);

        CountingFilter counting = new CountingFilter(0);
        AnyFilter      filter   = new AnyFilter(new Filter[]
                {
                counting,
                new EqualsFilter(IdentityExtractor.INSTANCE, 2)
                });

        Set<String> setKeys = new HashSet<>(map.keySet());

        filter.calculateEffectiveness(mapIndexes, setKeys);
        filter.applyIndex(mapIndexes, new HashSet<>(setKeys));

        assertEquals("calculateEffectiveness should be reused between calls.", 1, counting.getCalculateCount());
        }

    @Test
    public void shouldClearNestedOptimizedThreadLocalsAfterApply()
            throws Exception
        {
        MapIndex index = new SimpleMapIndex(IdentityExtractor.INSTANCE, false, null, null);

        Map<String, Object> map = Map.of(
                "key1", 1,
                "key2", 2,
                "key3", 3,
                "key4", 4
        );
        map.entrySet().forEach(index::insert);

        Map<ValueExtractor, MapIndex> mapIndexes = new HashMap<>();
        mapIndexes.put(IdentityExtractor.INSTANCE, index);

        AllFilter filterNested = new AllFilter(new Filter[]
                {
                new EqualsFilter(IdentityExtractor.INSTANCE, 2),
                new EqualsFilter(IdentityExtractor.INSTANCE, 3)
                });

        AnyFilter filterRoot = new AnyFilter(new Filter[]
                {
                filterNested,
                new EqualsFilter(IdentityExtractor.INSTANCE, 99)
                });

        Set<String> setKeys = new HashSet<>(map.keySet());

        filterNested.calculateEffectiveness(mapIndexes, setKeys);
        assertNotNull("Nested optimized order should be set before apply.", getOptimizedThreadLocal(filterNested).get());

        filterRoot.applyIndex(mapIndexes, setKeys);

        assertNull("Root optimized order should be cleared.", getOptimizedThreadLocal(filterRoot).get());
        assertNull("Nested optimized order should be cleared.", getOptimizedThreadLocal(filterNested).get());
        }

    private static ThreadLocal<?> getOptimizedThreadLocal(ArrayFilter filter)
            throws Exception
        {
        Field field = ArrayFilter.class.getDeclaredField("f_aFilterOptimized");
        field.setAccessible(true);
        return (ThreadLocal<?>) field.get(filter);
        }

    private static class CountingFilter
            implements IndexAwareFilter
        {
        CountingFilter(int nEffectiveness)
            {
            m_nEffectiveness = nEffectiveness;
            }

        @Override
        public int calculateEffectiveness(Map mapIndexes, Set setKeys)
            {
            m_cCalculate.incrementAndGet();
            return m_nEffectiveness;
            }

        @Override
        public Filter applyIndex(Map mapIndexes, Set setKeys)
            {
            return this;
            }

        @Override
        public boolean evaluateEntry(Map.Entry entry)
            {
            return false;
            }

        @Override
        public boolean evaluate(Object o)
            {
            return false;
            }

        int getCalculateCount()
            {
            return m_cCalculate.get();
            }

        private final AtomicInteger m_cCalculate = new AtomicInteger();
        private final int           m_nEffectiveness;
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
