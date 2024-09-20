/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.filter;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.QueryContext;
import com.tangosol.util.QueryRecord;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.SimpleMapIndex;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.IdentityExtractor;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.junit.Assert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * @author jk  2013.11.25
 */
public class BetweenFilterTest
    {
    /**
     * Test POF serialization.
     */
    @Test
    public void shouldSerializeWithPOF()
            throws Exception
        {
        BetweenFilter filter = new BetweenFilter(f_extractor, 10, 20, true, true);
        Binary        binary = ExternalizableHelper.toBinary(filter, f_serializer);
        BetweenFilter result = (BetweenFilter) ExternalizableHelper.fromBinary(binary, f_serializer);

        assertThat(result.getFilters(), is(filter.getFilters()));
        }

    /**
     * Test Externalizable serialization.
     */
    @Test
    public void shouldSerializeWithExternalizable()
            throws Exception
        {
        BetweenFilter filter = new BetweenFilter(f_extractor, 10, 20, true, true);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutput            dataOutputStream      = new DataOutputStream(byteArrayOutputStream);

        ExternalizableHelper.writeObject(dataOutputStream, filter);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        DataInput            dataInputStream      = new DataInputStream(byteArrayInputStream);
        BetweenFilter        result               = (BetweenFilter) ExternalizableHelper.readObject(dataInputStream);

        assertThat(result.getFilters(), is(filter.getFilters()));
        }

    /**
     * Test value lower than lower bound evaluates to false.
     */
    @Test
    public void shouldEvaluateValueLowerThanRangeToFalse()
            throws Exception
        {
        BetweenFilter filter = new BetweenFilter(f_extractor, 10, 20);

        assertThat(filter.evaluate(9), is(false));
        }

    /**
     * Test value equal to the lower bound evaluates to true when include
     * lower bound flag is set to true.
     */
    @Test
    public void shouldEvaluateValueEqualToLowerBoundToTrue()
            throws Exception
        {
        BetweenFilter filter = new BetweenFilter(f_extractor, 10, 20);

        assertThat(filter.evaluate(10), is(true));
        }

    /**
     * Test value equal to the lower bound evaluates to false when include
     * lower bound flag is set to false.
     */
    @Test
    public void shouldEvaluateValueEqualToLowerBoundToFalse()
            throws Exception
        {
        BetweenFilter filter = new BetweenFilter(f_extractor, 10, 20, false, true);

        assertThat(filter.evaluate(10), is(false));
        }

    /**
     * Test value lower than lower bound evaluates to false.
     */
    @Test
    public void shouldEvaluateValueHigherThanRangeToFalse()
            throws Exception
        {
        BetweenFilter filter = new BetweenFilter(f_extractor, 10, 20);

        assertThat(filter.evaluate(21), is(false));
        }

    /**
     * Test value equal to the upper bound evaluates to true when include
     * upper bound flag is set to true.
     */
    @Test
    public void shouldEvaluateValueEqualToUpperBoundToTrue()
            throws Exception
        {
        BetweenFilter filter = new BetweenFilter(f_extractor, 10, 20);

        assertThat(filter.evaluate(20), is(true));
        }

    /**
     * Test value equal to the upper bound evaluates to false when include
     * upper bound flag is set to false.
     */
    @Test
    public void shouldEvaluateValueEqualToUpperBoundToFalse()
            throws Exception
        {
        BetweenFilter filter = new BetweenFilter(f_extractor, 10, 20, true, false);

        assertThat(filter.evaluate(20), is(false));
        }

    /**
     *  Test evaluating an entry outside of the range evaluates to false
     */
    @Test
    public void shouldEvaluateEntryAsFalse() throws Exception
        {
        BetweenFilter filter = new BetweenFilter(f_extractor, 10, 20, true, false);
        Map.Entry     entry  = new SimpleMapEntry("Key", 9);
        assertThat(filter.evaluateEntry(entry, null, null), is(false));
        }

    /**
     *  Test evaluating an entry inside of the range evaluates to true
     */
    @Test
    public void shouldEvaluateEntryAsTrue() throws Exception
        {
        BetweenFilter filter = new BetweenFilter(f_extractor, 10, 20, true, false);
        Map.Entry     entry  = new SimpleMapEntry("Key", 15);
        assertThat(filter.evaluateEntry(entry, null, null), is(true));
        }

    /**
     *
     */
    @Test
    public void shouldEvaluateEntryForQueryPlan() throws Exception
        {
        BetweenFilter filter = new BetweenFilter(f_extractor, 10, 20, true, false);
        Map.Entry     entry  = new SimpleMapEntry("Key", 15);

        QueryContext                        ctx        = mock(QueryContext.class);
        QueryRecord.PartialResult.TraceStep stepParent = mock(QueryRecord.PartialResult.TraceStep.class, "Parent");
        QueryRecord.PartialResult.TraceStep stepChild  = mock(QueryRecord.PartialResult.TraceStep.class, "Child");


        when(stepParent.ensureStep(any(Filter.class))).thenReturn(stepChild);

        assertThat(filter.evaluateEntry(entry, ctx, stepParent), is(true));

        verify(stepParent).ensureStep(filter);
        }

    /**
     * Test that calculateEffectiveness returns the correct result when there
     * is no matching index.
     */
    @Test
    public void shouldCalculateEffectivenessWithNoInvertedIndex()
            throws Exception
        {
        Map           mapIndexes    = new HashMap();
        BetweenFilter filter        = new BetweenFilter(f_extractor, 10, 20);
        int           effectiveness = filter.calculateEffectiveness(mapIndexes, s_setKeys);

        assertThat(effectiveness, is(-1));
        }

    /**
     * Test that calculateEffectiveness returns the correct result when
     * the index is a SortedSet.
     */
    @Test
    public void shouldCalculateEffectivenessWhenInvertedIndexIsSortedSet()
            throws Exception
        {
        Map mapIndexes = new HashMap();

        mapIndexes.put(f_extractor, s_sortedIndex);

        BetweenFilter filter        = new BetweenFilter(f_extractor, 10, 20);
        int           effectiveness = filter.calculateEffectiveness(mapIndexes, s_setKeys);

        assertThat(effectiveness, is(s_setKeysTenToTwenty.size()));
        }

    /**
     * Test that calculateEffectiveness returns the correct result when
     * the index is a plain Set.
     */
    @Test
    public void shouldCalculateEffectivenessWhenInvertedIndexIsPlainSet()
            throws Exception
        {
        Map mapIndexes = new HashMap();

        mapIndexes.put(f_extractor, s_unsortedIndex);

        BetweenFilter filter        = new BetweenFilter(f_extractor, 10, 20);
        int         effectiveness = filter.calculateEffectiveness(mapIndexes, s_setKeys);

        assertThat(effectiveness, is(s_setKeysTenToTwenty.size()));
        }

    /**
     * Test applyIndex returns the null and does not alter
     * key set when no index is present.
     */
    @Test
    public void shouldApplyIndexWhenNoIndexPresent()
            throws Exception
        {
        Set<Binary> setKeys    = new HashSet<>(s_setKeys);
        Map         mapIndexes = new HashMap();

        BetweenFilter filter     = new BetweenFilter(f_extractor, 10, 20);

        assertThat(filter.applyIndex(mapIndexes, setKeys), is(sameInstance((Filter) filter)));
        assertThat(setKeys, is(s_setKeys));
        }

    /**
     * Test applyIndex returns the null and correctly alters the key set
     * when an un-sorted index is present.
     */
    @Test
    public void shouldApplyIndexWhenUnsortedIndexPresent()
            throws Exception
        {
        Set<Binary> setKeys    = new HashSet<>(s_setKeys);
        Map         mapIndexes = new HashMap();

        mapIndexes.put(f_extractor, s_unsortedIndex);

        BetweenFilter filter = new BetweenFilter(f_extractor, 10, 20);

        assertThat(filter.applyIndex(mapIndexes, setKeys), is(nullValue()));
        assertThat(setKeys, is(s_setKeysTenToTwenty));
        }

    /**
     * Test applyIndex returns the null and correctly alters the key set
     * when an sorted index is present and only the upper bound of
     * the range is present in the index.
     */
    @Test
    public void shouldApplyIndexWhenIndexPresentAndOnlyUpperBoundMatches()
            throws Exception
        {
        Set<Binary> setKeys    = new HashSet<>(s_setKeys);
        Map         mapIndexes = new HashMap();

        SortedMap   map        = (SortedMap) s_sortedIndex.getIndexContents();

        mapIndexes.put(f_extractor, s_sortedIndex);

        BetweenFilter filter = new BetweenFilter(f_extractor, -1, 0);

        assertThat(filter.applyIndex(mapIndexes, setKeys), is(nullValue()));
        assertThat(setKeys, is(map.get(0)));
        }

    /**
     * Test applyIndex returns the null and correctly alters the key set
     * when an sorted index is present and only the lower bound and
     * the upper bound of the range is present in the index.
     */
    @Test
    public void shouldApplyIndexWhenIndexPresentAndOnlyLowerAndUpperBoundMatches()
            throws Exception
        {
        Set<Binary>                            setKeys    = new HashSet<>(s_setKeys);
        Map                                    mapIndexes = new HashMap();
        SortedMap<Integer, Collection<Binary>> map        = (SortedMap) s_sortedIndex.getIndexContents();

        mapIndexes.put(f_extractor, s_sortedIndex);

        BetweenFilter filter = new BetweenFilter(f_extractor, 0, 1);

        assertThat(filter.applyIndex(mapIndexes, setKeys), is(nullValue()));

        Set<Binary> expected = new HashSet<>();

        expected.addAll(map.get(0));
        expected.addAll(map.get(1));
        assertThat(setKeys, is(expected));
        }

    /**
     * Test applyIndex returns the null and correctly alters the key set
     * when a sorted index is present.
     */
    @Test
    public void shouldApplyIndexWhenSortedIndexPresent()
            throws Exception
        {
        Set<Binary> setKeys    = new HashSet<>(s_setKeys);
        Map         mapIndexes = new HashMap();

        mapIndexes.put(f_extractor, s_sortedIndex);

        BetweenFilter filter = new BetweenFilter(f_extractor, 10, 20);

        assertThat(filter.applyIndex(mapIndexes, setKeys), is(nullValue()));
        assertThat(setKeys, is(s_setKeysTenToTwenty));
        }

    /**
     * Test applyIndex returns the null and correctly alters the key set
     * when an un-sorted index is present and none of the values are within
     * the range.
     */
    @Test
    public void shouldApplyIndexWhenUnsortedIndexPresentAndNoValuesInRange()
            throws Exception
        {
        Set<Binary> setKeys    = new HashSet<>(s_setKeys);
        Map         mapIndexes = new HashMap();

        mapIndexes.put(f_extractor, s_unsortedIndex);

        BetweenFilter filter = new BetweenFilter(f_extractor, 100, 200);

        assertThat(filter.applyIndex(mapIndexes, setKeys), is(nullValue()));
        assertThat(setKeys.size(), is(0));
        }

    /**
     * Test applyIndex returns the null and correctly alters the key set
     * when a sorted index is present and none of the values are within
     * the range.
     */
    @Test
    public void shouldApplyIndexWhenSortedIndexPresentAndNoValuesInRange()
            throws Exception
        {
        Set<Binary> setKeys    = new HashSet<>(s_setKeys);
        Map         mapIndexes = new HashMap();

        mapIndexes.put(f_extractor, s_sortedIndex);

        BetweenFilter filter = new BetweenFilter(f_extractor, 100, 200);

        assertThat(filter.applyIndex(mapIndexes, setKeys), is(nullValue()));
        assertThat(setKeys.size(), is(0));
        }

    /**
     * Should evaluate a value of null to be false.
     */
    @Test
    public void shouldEvaluateNull()
            throws Exception
        {
        BetweenFilter filter = new BetweenFilter(f_extractor, 100, 200);

        assertThat(filter.evaluateExtracted(null), is(false));
        }

    /**
     * If the lower bound is null it should not match with any
     * values that are null.
     */
    @Test
    public void shouldEvaluateWithNullLowerBound()
            throws Exception
        {
        Set<Binary> setKeys    = new HashSet<>(s_setKeys);
        Map         mapIndexes = new HashMap();

        mapIndexes.put(f_extractor, s_sortedIndex);

        BetweenFilter filter = new BetweenFilter(f_extractor, null, 0);

        assertThat(filter.applyIndex(mapIndexes, setKeys), is(nullValue()));
        assertThat(setKeys.size(), is(0));
        }

    /**
     * Setup various values used in the tests.
     */
    @BeforeClass
    public static void setup()
        {
        s_sortedIndex        = new SimpleMapIndex(IdentityExtractor.INSTANCE, true, null, null);
        s_unsortedIndex      = new SimpleMapIndex(IdentityExtractor.INSTANCE, false, null, null);
        s_setKeys            = new HashSet<>();
        s_setKeysTenToTwenty = new HashSet<>();

        Binary key;
        Map.Entry<Binary, Integer> entry;

        for (int i = 50; i < 250; i++)
            {
            key = ExternalizableHelper.toBinary(i, f_serializer);

            s_setKeys.add(key);

            int value = i % 30;

            if (value >= 10 && value <= 20)
                {
                s_setKeysTenToTwenty.add(key);
                }

            entry = new SimpleMapEntry<>(key, value);

            s_sortedIndex.insert(entry);
            s_unsortedIndex.insert(entry);
            }

        // Add null value
        key = ExternalizableHelper.toBinary(Integer.MIN_VALUE, f_serializer);
        entry = new SimpleMapEntry<>(key, null);
        s_sortedIndex.insert(entry);
        s_unsortedIndex.insert(entry);

        s_setKeys.add(key);
        }

    protected static final ValueExtractor         f_extractor  = IdentityExtractor.INSTANCE;
    protected static final ConfigurablePofContext f_serializer = new ConfigurablePofContext("coherence-pof-config.xml");
    protected static Set<Binary>                  s_setKeys;
    protected static Set<Binary>                  s_setKeysTenToTwenty;
    protected static SimpleMapIndex               s_sortedIndex;
    protected static SimpleMapIndex               s_unsortedIndex;
    }
