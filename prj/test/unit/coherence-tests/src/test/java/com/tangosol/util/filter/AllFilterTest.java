/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.filter;

import com.tangosol.util.MapIndex;
import com.tangosol.util.Filter;
import com.tangosol.util.SimpleMapIndex;
import com.tangosol.util.ValueExtractor;
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

/**
* AllFilter unit tests
*
* @author tb 2010.2.08
*/
@SuppressWarnings({"rawtypes", "unchecked"})
@RunWith(value = Parameterized.class)
public class AllFilterTest
    {

    public AllFilterTest(boolean fOrdered, boolean fPartial)
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
        MapIndex index = new SimpleMapIndex(IdentityExtractor.INSTANCE, m_fOrdered, null, null);

        // create the AllFilter to be tested
        IndexAwareFilter filter = new AllFilter(new Filter[] {
                new GreaterFilter(IdentityExtractor.INSTANCE, 1),
                new LessFilter(IdentityExtractor.INSTANCE, 4)});

        Map<ValueExtractor, MapIndex> mapIndexes = new HashMap();
        mapIndexes.put(IdentityExtractor.INSTANCE, index);

        Map<String, Object> map = Map.of(
                "key1", 1,
                "key2", 2,
                "key3", 3,
                "key4", 4
        );
        map.entrySet().forEach(index::insert);
        Set<String> setKeys = new HashSet<>(map.keySet());

        if (m_fPartial)
            {
            Map<String, Object> mapExcluded = Map.of("key0", MapIndex.NO_VALUE, "key5", MapIndex.NO_VALUE);
            mapExcluded.entrySet().forEach(index::insert);
            setKeys.addAll(mapExcluded.keySet());
            }

        // begin test
        assertEquals(3, filter.calculateEffectiveness(mapIndexes, setKeys));

        filter.applyIndex(mapIndexes, setKeys);

        assertEquals("Two keys should remain in the set of keys.",
                2, setKeys.size());

        assertTrue("key3 should remain.", setKeys.contains("key3"));
        assertTrue("key2 should remain.", setKeys.contains("key2"));
        }

    /**
    * Run the test with an ordered index.
    */
    private final boolean m_fOrdered;

    /**
    * Run the test with an partial index.
    */
    private final boolean m_fPartial;
    }
