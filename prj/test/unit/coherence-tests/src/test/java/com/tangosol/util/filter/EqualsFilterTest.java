/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.filter;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.util.Filter;
import com.tangosol.util.QueryContext;
import com.tangosol.util.QueryRecord;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.SimpleQueryRecord;
import com.tangosol.internal.util.SimpleQueryContext;

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
* EqualsFilter unit tests
*
* @author tb 2010.2.08
*/
@RunWith(value = Parameterized.class)
public class EqualsFilterTest
    {

    public EqualsFilterTest(boolean fOrdered, boolean fPartial)
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

        // create the EqualsFilter to be tested
        IndexAwareFilter filter = new EqualsFilter(IdentityExtractor.INSTANCE, 3);

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
        assertEquals(1, filter.calculateEffectiveness(mapIndexes, setKeys));

        filter.applyIndex(mapIndexes, setKeys);

        assertEquals("One key should remain in the set of keys.",
                1, setKeys.size());

        assertTrue("key3 should remain.", setKeys.contains("key3"));
        }

    /**
    * Test explain
    */
    @Test
    public void testExplain()
        {
        // create the mocks
        MapIndex          index = mock(MapIndex.class);
        BackingMapContext ctxBM = mock(BackingMapContext.class);

        // create the EqualsFilter to be tested
        QueryRecorderFilter filter = new EqualsFilter(IdentityExtractor.INSTANCE, 3);

        Map mapIndexes = new HashMap();
        mapIndexes.put(IdentityExtractor.INSTANCE, index);

        QueryContext ctx = new SimpleQueryContext(ctxBM);
        QueryRecord.PartialResult.ExplainStep step = instantiateExplainStep(ctx, filter);

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

        // set mock expectations
        when(ctxBM.getIndexMap()).thenReturn(mapIndexes);

        // begin test
        filter.explain(ctx, step, setKeys);

        assertEquals(filter.toString(), step.getFilterDescription());
        assertEquals(setKeys.size(), step.getPreFilterKeySetSize());
        assertEquals(0, step.getEfficiency());
        }

    /**
    * Test {@link QueryRecorderFilter#trace(QueryContext, QueryRecord.PartialResult.TraceStep, Set) trace}
    */
    @Test
    public void testTrace()
        {
        // create the mocks
        MapIndex          index = mock(MapIndex.class);
        BackingMapContext ctxBM = mock(BackingMapContext.class);

        // create the EqualsFilter to be tested
        QueryRecorderFilter filter = new EqualsFilter(IdentityExtractor.INSTANCE, 3);

        Map mapIndexes = new HashMap();
        mapIndexes.put(IdentityExtractor.INSTANCE, index);

        QueryContext ctx = new SimpleQueryContext(ctxBM);
        QueryRecord.PartialResult.TraceStep step = instantiateTraceStep(ctx, filter);

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
        when(ctxBM.getIndexMap()).thenReturn(mapIndexes);

        // begin test
        filter.trace(ctx, step, setKeys);

        assertEquals(filter.toString(), step.getFilterDescription());
        assertEquals(m_fPartial ? 8 : 4, step.getPreFilterKeySetSize());
        assertEquals(1, step.getPostFilterKeySetSize());

        Set<? extends QueryRecord.PartialResult.IndexLookupRecord> setIndexRecords = step.getIndexLookupRecords();
        QueryRecord.PartialResult.IndexLookupRecord indexLookupRecord = setIndexRecords.iterator().next();

        assertEquals(1, setIndexRecords.size());
        assertEquals(IdentityExtractor.INSTANCE.toString(), indexLookupRecord.getExtractorDescription());
        assertEquals(m_fOrdered, indexLookupRecord.isOrdered());
        }

    /**
    * Test {@link QueryRecorderFilter#trace(QueryContext, QueryRecord.PartialResult.TraceStep, Map.Entry) trace}
    */
    @Test
    public void testTrace2()
        {
        // create the mocks
        MapIndex          index = mock(MapIndex.class);
        BackingMapContext ctxBM = mock(BackingMapContext.class);

        // create the EqualsFilter to be tested
        QueryRecorderFilter filter = new EqualsFilter(IdentityExtractor.INSTANCE, 3);

        Map mapIndexes = new HashMap();
        mapIndexes.put(IdentityExtractor.INSTANCE, index);

        QueryContext ctx = new com.tangosol.internal.util.SimpleQueryContext(ctxBM);
        QueryRecord.PartialResult.TraceStep step = instantiateTraceStep(ctx, filter);

        Map.Entry entry = new SimpleMapEntry("key3", 3);

        // begin test
        filter.trace(ctx, step, entry);

        assertEquals(filter.toString(), step.getFilterDescription());
        assertEquals(1, step.getPreFilterKeySetSize());
        assertEquals(1, step.getPostFilterKeySetSize());

        Set<? extends QueryRecord.PartialResult.IndexLookupRecord> setIndexRecords = step.getIndexLookupRecords();

        assertEquals(0, setIndexRecords.size());
        }


    // ----- helper methods -------------------------------------------------

    private static QueryRecord.PartialResult.ExplainStep instantiateExplainStep(QueryContext ctx, Filter filter)
        {
        SimpleQueryRecord.PartialResult result = new SimpleQueryRecord.PartialResult(ctx, new PartitionSet(100));

        return result.instantiateExplainStep(filter);
        }

    private static QueryRecord.PartialResult.TraceStep instantiateTraceStep(QueryContext ctx, Filter filter)
        {
        SimpleQueryRecord.PartialResult result = new SimpleQueryRecord.PartialResult(ctx, new PartitionSet(100));

        return result.instantiateTraceStep(filter);
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
