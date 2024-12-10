/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package aggregator;


import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.BigDecimalAverage;
import com.tangosol.util.aggregator.BigDecimalMax;
import com.tangosol.util.aggregator.BigDecimalMin;
import com.tangosol.util.aggregator.BigDecimalSum;
import com.tangosol.util.aggregator.ComparableMax;
import com.tangosol.util.aggregator.ComparableMin;
import com.tangosol.util.aggregator.CompositeAggregator;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.DistinctValues;
import com.tangosol.util.aggregator.DoubleAverage;
import com.tangosol.util.aggregator.DoubleMax;
import com.tangosol.util.aggregator.DoubleMin;
import com.tangosol.util.aggregator.DoubleSum;
import com.tangosol.util.aggregator.GroupAggregator;
import com.tangosol.util.aggregator.LongMax;
import com.tangosol.util.aggregator.LongMin;
import com.tangosol.util.aggregator.LongSum;
import com.tangosol.util.aggregator.ReducerAggregator;
import com.tangosol.util.aggregator.TopNAggregator;

import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.LessEqualsFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.Person;
import data.Trade;

import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
* A collection of functional tests for the various
* {@link InvocableMap.EntryAggregator} implementations.
*
* @author jh              2005.12.21
* @author Gunnar Hillert  2022.06.01
*
* @see InvocableMap
*/
public abstract class AbstractEntryAggregatorTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new AbstractEntryAggregatorTests that will use the cache with
    * the given name in all test methods.
    *
    * @param sCache  the test cache name
    */
    public AbstractEntryAggregatorTests(String sCache)
        {
        if (sCache == null || sCache.trim().length() == 0)
            {
            throw new IllegalArgumentException("Invalid cache name");
            }

        m_sCache = sCache.trim();
        }


    // ----- AbstractEntryAggregatorTests methods ---------------------------

    /**
    * Return the cache used in all test methods.
    *
    * @return the test cache
    */
    protected NamedCache getNamedCache()
        {
        NamedCache cache = getNamedCache(getCacheName());
        cache.clear();
        Eventually.assertDeferred(() -> cache.size(), is(0));
        return cache;
        }

    /**
    * Return the number of cache servers launched by this test.
    *
    * @return the number of cache servers launched by this test.
    */
    protected int getCacheServerCount()
        {
        return 0;
        }


    // ----- test lifecycle methods -----------------------------------------

    @Before
    public void verifyClusterReady()
        {
        Cluster cluster = CacheFactory.ensureCluster();
        int     nSize   = 1 + getCacheServerCount();

        Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(nSize));
        }

    // ----- test methods ---------------------------------------------------

    /**
    * Test of the {@link Count} aggregator.
    */
    @Test
    public void count()
        {
        NamedCache<String, Integer> cache = getNamedCache();

        int nSize = cache.aggregate(NullImplementation.getSet(), new Count<>());
        try
            {
            assertEquals("assert empty cache has aggregation count of 0", nSize, cache.size());
            }
        catch (Throwable t)
            {
            t.printStackTrace();
            printCache(cache);
            }

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        nSize = cache.aggregate(NullImplementation.getSet(), new Count<>());
        assertEquals(nSize, 0);

        nSize = cache.aggregate(Collections.singletonList("1"), new Count<>());
        assertEquals(nSize, 1);

        nSize = cache.aggregate((Filter) null, new Count<>());
        assertEquals(nSize, cache.size());

        nSize = cache.aggregate(new LessEqualsFilter(IdentityExtractor.INSTANCE, 5), new Count<>());
        assertEquals(nSize, 5);

        // now with the index
        cache.addIndex(IdentityExtractor.INSTANCE(), true, null);
        try
            {
            nSize = cache.aggregate(new LessEqualsFilter(IdentityExtractor.INSTANCE, 5), new Count<>());
            assertEquals(nSize, 5);

            nSize = cache.aggregate(AlwaysFilter.INSTANCE, new Count<>());
            assertEquals(nSize, cache.size());
            }
        finally
            {
            removeIndexFrom(cache, IdentityExtractor.INSTANCE());
            }
        }

    /**
    * Test of the {@link DistinctValues} aggregator.
    */
    @Test
    public void distinctValues()
        {
        NamedCache<String, Integer> cache = getNamedCache();
        DistinctValues<String, Integer, Integer, Integer> agent
            = new DistinctValues<>(IdentityExtractor.INSTANCE());

        testEmpty(cache, agent);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Collection<Integer> setResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue(setResult.isEmpty());

        Set<Integer> setExpected = new HashSet();
        setExpected.add(1);

        setResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertEquals(setExpected, setResult);

        setExpected.clear();
        for (int i = 1; i <= 10; ++i)
            {
            setExpected.add(i);
            }

        setResult = cache.aggregate((Filter) null, agent);
        assertEquals(setExpected, setResult);

        setResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertEquals(setExpected, setResult);

        setExpected.clear();
        for (int i = 5; i <= 10; ++i)
            {
            setExpected.add(i);
            }

        setResult = cache.aggregate(Filters.greaterEqual(IdentityExtractor.INSTANCE(), 5), agent);
        assertEquals(setExpected, setResult);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), 0);
            }

        setExpected.clear();
        setExpected.add(0);

        setResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertEquals(setExpected, setResult);

        setResult = cache.aggregate((Filter) null, agent);
        assertEquals(setExpected, setResult);

        setResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertEquals(setExpected, setResult);

        cache.clear();
        Eventually.assertThat(cache.size(), is(0));
        }

    /**
    * Test of the {@link DoubleAverage} aggregator.
    */
    @Test
    public void doubleAverage()
        {
        NamedCache    cache = getNamedCache();
        DoubleAverage agent = new DoubleAverage(IdentityExtractor.INSTANCE);

        testEmpty(cache, agent);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1.0D));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 5.5D));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 5.5D));

        cache.clear();
        Eventually.assertThat(cache.size(), is(0));
        }

    /**
    * Test of the {@link DoubleMax} aggregator.
    */
    @Test
    public void doubleMax()
        {
        NamedCache cache = getNamedCache();
        DoubleMax  agent = new DoubleMax(IdentityExtractor.INSTANCE);

        testEmpty(cache, agent);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1.0D));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 10.0D));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 10.0D));
        }

    /**
    * Test of the {@link DoubleMin} aggregator.
    */
    @Test
    public void doubleMin()
        {
        NamedCache cache = getNamedCache();
        DoubleMin  agent = new DoubleMin(IdentityExtractor.INSTANCE);

        testEmpty(cache, agent);
        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1.0D));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 1.0D));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 1.0D));
        }

    /**
    * Test of the {@link DoubleSum} aggregator.
    */
    @Test
    public void doubleSum()
        {
        NamedCache cache = getNamedCache();
        DoubleSum  agent = new DoubleSum(IdentityExtractor.INSTANCE);

        testEmpty(cache, agent);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1.0D));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 55.0D));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 55.0D));
        }

    /**
    * Test of the {@link LongMax} aggregator.
    */
    @Test
    public void longMax()
        {
        NamedCache cache = getNamedCache();
        LongMax    agent = new LongMax(IdentityExtractor.INSTANCE);

        testEmpty(cache, agent);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1L));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 10L));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 10L));
        }

    /**
    * Test of the {@link LongMin} aggregator.
    */
    @Test
    public void longMin()
        {
        NamedCache cache = getNamedCache();
        LongMin    agent = new LongMin(IdentityExtractor.INSTANCE);

        testEmpty(cache, agent);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1L));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 1L));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 1L));
        }

    /**
    * Test of the {@link LongSum} aggregator.
    */
    @Test
    public void longSum()
        {
        NamedCache cache = getNamedCache();
        LongSum    agent = new LongSum(IdentityExtractor.INSTANCE);

        testEmpty(cache, agent);

        for (int i = 1; i <= 10; ++i)
            {
            cache.put(String.valueOf(i), i);
            }

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equals(oResult, 1L));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equals(oResult, 55L));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equals(oResult, 55L));
        }

    /**
    * Test of the {@link BigDecimalAverage} aggregator.
    */
    @Test
    public void bigDecimalAverage()
        {
        NamedCache cache = getNamedCache();
        BigDecimalAverage agent = new BigDecimalAverage(IdentityExtractor.INSTANCE);

        testEmpty(cache, agent);

        fillBigNumbers(cache, 1, 10);

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(1.0D)));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(5.5D)));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(5.5D)));
        }

    /**
     * Test of the {@link BigDecimalAverage} aggregator.
     */
    @Test
    public void bigDecimalAverageWithAdditionalBigDecimalProperties()
        {
        NamedCache cache = getNamedCache();
        BigDecimalAverage agent = new BigDecimalAverage(IdentityExtractor.INSTANCE);

        testEmpty(cache, agent);

        fillBigNumbers(cache, 1, 10);

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("1.00000000"));

        oResult = cache.aggregate((Filter) null, agent);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("5.50000000"));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("5.50000000"));

        BigDecimalAverage bigDecimalAverageStrippingZeros = new BigDecimalAverage(IdentityExtractor.INSTANCE);
        bigDecimalAverageStrippingZeros.setStripTrailingZeros(true);

        oResult = cache.aggregate(Collections.singletonList("1"), bigDecimalAverageStrippingZeros);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("1"));

        oResult = cache.aggregate((Filter) null, bigDecimalAverageStrippingZeros);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("5.5"));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, bigDecimalAverageStrippingZeros);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("5.5"));

        BigDecimalAverage bigDecimalAverageAndScaleOf0 = new BigDecimalAverage(IdentityExtractor.INSTANCE);
        bigDecimalAverageAndScaleOf0.setScale(0);
        bigDecimalAverageAndScaleOf0.setRoundingMode(RoundingMode.CEILING);

        oResult = cache.aggregate(Collections.singletonList("1"), bigDecimalAverageAndScaleOf0);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("1"));

        oResult = cache.aggregate((Filter) null, bigDecimalAverageAndScaleOf0);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("6"));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, bigDecimalAverageAndScaleOf0);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("6"));


        BigDecimalAverage bigDecimalAverageWithMathContext = new BigDecimalAverage(IdentityExtractor.INSTANCE);

        MathContext mathContext = new MathContext(1, RoundingMode.HALF_UP);
        bigDecimalAverageWithMathContext.setMathContext(mathContext);

        oResult = cache.aggregate(Collections.singletonList("1"), bigDecimalAverageWithMathContext);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("1"));

        oResult = cache.aggregate((Filter) null, bigDecimalAverageWithMathContext);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("6"));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, bigDecimalAverageWithMathContext);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("6"));
        }

    /**
     * Test of the {@link BigDecimalAverage} aggregator.
     */
    @Test
    public void bigDecimalAverageWithScaleAndNullRoundingMode()
        {
        NamedCache cache = getNamedCache();
        BigDecimalAverage agent = new BigDecimalAverage(IdentityExtractor.INSTANCE);
        agent.setScale(5);

        testEmpty(cache, agent);

        fillBigNumbers(cache, 1, 10);

        try
            {
            cache.aggregate(Collections.singletonList("1"), agent);
            }
        catch (IllegalArgumentException ex)
            {
            assertEquals(ex.getMessage(), "If scale is specified, the rounding mode must be specified as well");
            return;
            }
        fail("Expected an IllegalArgumentException to be thrown.");
        }

    /**
    * Test of the {@link BigDecimalMax} aggregator.
    */
    @Test
    public void bigDecimalMax()
        {
        NamedCache cache = getNamedCache();
        BigDecimalMax agent = new BigDecimalMax(IdentityExtractor.INSTANCE);

        testEmpty(cache, agent);

        fillBigNumbers(cache, 1, 10);

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(1.0D)));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(10.0D)));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(10.0D)));
        }

    /**
     * Test of the {@link BigDecimalMax} aggregator.
     */
    @Test
    public void bigDecimalMaxWithAdditionalBigDecimalProperties()
        {
        NamedCache cache = getNamedCache();
        BigDecimalMax agent = new BigDecimalMax(IdentityExtractor.INSTANCE);

        testEmpty(cache, agent);

        fillBigNumbers(cache, 1, 10);
        cache.put("11", new BigDecimal("50.000500000"));

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("1"));

        oResult = cache.aggregate((Filter) null, agent);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("50.000500000"));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("50.000500000"));

        BigDecimalMax bigDecimalMax2 = new BigDecimalMax(IdentityExtractor.INSTANCE);
        bigDecimalMax2.setScale(3);
        bigDecimalMax2.setRoundingMode(RoundingMode.HALF_UP);

        oResult = cache.aggregate(Collections.singletonList("1"), bigDecimalMax2);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("1.000"));

        oResult = cache.aggregate((Filter) null, bigDecimalMax2);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("50.001"));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, bigDecimalMax2);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("50.001"));
        }

    /**
    * Test of the {@link BigDecimalMin} aggregator.
    */
    @Test
    public void bigDecimalMinWithAdditionalBigDecimalProperties()
        {
        NamedCache cache = getNamedCache();
        BigDecimalMin agent = new BigDecimalMin(IdentityExtractor.INSTANCE);
        agent.setScale(1);
        agent.setRoundingMode(RoundingMode.HALF_UP);
        testEmpty(cache, agent);

        fillBigNumbers(cache, 1, 10);

        cache.put("11", new BigDecimal("0.05000000"));

        Object oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("1.0"));

        oResult = cache.aggregate((Filter) null, agent);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("0.1"));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("0.1"));

        BigDecimalMin bigDecimalMin2 = new BigDecimalMin(IdentityExtractor.INSTANCE);
        bigDecimalMin2.setScale(3);
        bigDecimalMin2.setRoundingMode(RoundingMode.HALF_DOWN);

        oResult = cache.aggregate(Collections.singletonList("1"), bigDecimalMin2);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("1.000"));

        oResult = cache.aggregate((Filter) null, bigDecimalMin2);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("0.050"));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, bigDecimalMin2);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("0.050"));

        BigDecimalMin bigDecimalMin3 = new BigDecimalMin(IdentityExtractor.INSTANCE);
        bigDecimalMin3.setScale(3);
        bigDecimalMin3.setRoundingMode(RoundingMode.HALF_DOWN);
        bigDecimalMin3.setStripTrailingZeros(true);

        oResult = cache.aggregate(Collections.singletonList("1"), bigDecimalMin3);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("1"));

        oResult = cache.aggregate((Filter) null, bigDecimalMin3);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("0.05"));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, bigDecimalMin3);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("0.05"));
        }

    /**
     * Test of the {@link BigDecimalMin} aggregator.
     */
    @Test
    public void bigDecimalMin()
        {
        NamedCache cache = getNamedCache();
        BigDecimalMin agent = new BigDecimalMin(IdentityExtractor.INSTANCE);

        testEmpty(cache, agent);

        fillBigNumbers(cache, 1, 10);

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(1.0D)));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(1.0D)));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(1.0D)));
        }

    /**
    * Test of the {@link BigDecimalSum} aggregator.
    */
    @Test
    public void bigDecimalSum()
        {
        NamedCache cache = getNamedCache();
        BigDecimalSum agent = new BigDecimalSum(IdentityExtractor.INSTANCE);

        testEmpty(cache, agent);

        fillBigNumbers(cache, 1, 10);
        Map map = new HashMap<>();
        map.putAll(cache);

        System.out.println("--> CONTENTS: " + map);

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(1.0D)));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(55.0D)));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal(55.0D)));
        }

    /**
     * Test of the {@link BigDecimalSum} aggregator.
     */
    @Test
    public void bigDecimalSumWithAdditionalBigDecimalProperties()
        {
        NamedCache<String, BigDecimal> cache = getNamedCache();
        BigDecimalSum agent = new BigDecimalSum(IdentityExtractor.INSTANCE);
        agent.setScale(3);
        agent.setRoundingMode(RoundingMode.HALF_UP);
        agent.setStripTrailingZeros(true);

        testEmpty(cache, agent);

        fillBigNumbers(cache, 1, 10);
        cache.put("11", new BigDecimal("5.0005"));
        cache.put("12", new BigDecimal("5.1000000000"));

        Map<String, BigDecimal> map = new HashMap<>();
        map.putAll(cache);

        System.out.println("--> CONTENTS: " + map);

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("1"));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal("65.101")));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal("65.101")));

        oResult = cache.aggregate(Collections.singletonList("12"), agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal("5.1")));
        }

    /**
     * Test of the {@link BigDecimalSum} aggregator.
     */
    @Test
    public void bigDecimalSumWithKeepTrailingZeros()
        {
        NamedCache<String, BigDecimal> cache = getNamedCache();
        BigDecimalSum agent = new BigDecimalSum(IdentityExtractor.INSTANCE);
        agent.setScale(3);
        agent.setRoundingMode(RoundingMode.HALF_UP);
        agent.setStripTrailingZeros(false);

        testEmpty(cache, agent);

        fillBigNumbers(cache, 1, 10);
        cache.put("11", new BigDecimal("5.0005"));
        cache.put("12", new BigDecimal("5.1000000000"));

        Map<String, BigDecimal> map = new HashMap<>();
        map.putAll(cache);

        System.out.println("--> CONTENTS: " + map);

        Object oResult = cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Result=" + oResult, oResult == null);

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("1.000"));

        oResult = cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal("65.101")));

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Result=" + oResult, equalsDec((BigDecimal) oResult, new BigDecimal("65.101")));

        oResult = cache.aggregate(Collections.singletonList("12"), agent);
        assertEquals("Result=" + oResult, (BigDecimal) oResult, new BigDecimal("5.100"));
        }

    /**
    * Test of {@link ComparableMax} and {@link ComparableMin}
    */
    @Test
    public void comparableAggregator()
        {
        NamedCache cache = getNamedCache();

        // create the aggregators and test with an empty cache

        ComparableMax comparableMax = new ComparableMax("getFirstName");
        ComparableMin comparableMin = new ComparableMin("getFirstName");

        testEmpty(cache, comparableMax);
        testEmpty(cache, comparableMin);

        Comparator comparator = new FirstNameComparator();

        ComparableMax comparatorMax = new ComparableMax(IdentityExtractor.INSTANCE,
                comparator);
        ComparableMin comparatorMin = new ComparableMin(IdentityExtractor.INSTANCE,
                comparator);

        testEmpty(cache, comparatorMax);
        testEmpty(cache, comparatorMin);

        Person.fillRandom(cache, 1000);

        // determine the expected aggregation results

        String[] asFirst = Person.FIRST_NAMES.clone();
        Arrays.sort(asFirst);

        String sExpectedMax = asFirst[asFirst.length - 1];
        String sExpectedMin = asFirst[0];

        // test using Comparable extracted values

        String sMaxFirst = (String) cache.aggregate((Filter)null,
                comparableMax);
        assertTrue("Expected: " + sExpectedMax + ", actual: " + sMaxFirst,
                sExpectedMax.equals(sMaxFirst));

        String sMinFirst = (String) cache.aggregate((Filter)null,
                comparableMin);
        assertTrue("Expected: " + sExpectedMin + ", actual: " + sMinFirst,
                sExpectedMin.equals(sMinFirst));

        // test using a custom Comparable

        Person personMax = (Person)cache.aggregate((Filter)null, comparatorMax);
        assertTrue("Expected: " + sExpectedMax + ", actual: " + personMax.getFirstName(),
                sExpectedMax.equals(personMax.getFirstName()));

        Person personMin = (Person)cache.aggregate((Filter)null, comparatorMin);
        assertTrue("Expected: " + sExpectedMin + ", actual: " + personMin.getFirstName(),
                sExpectedMin.equals(personMin.getFirstName()));

        cache.clear();
        Eventually.assertThat(cache.size(), is(0));
        }

    /**
    * Comparator used for testing ComparableMax and ComparableMin
    */
    private static class FirstNameComparator
            implements Comparator, Serializable
        {
        public int compare(Object o1, Object o2)
            {
            return ((Person)o1).getFirstName().compareTo(((Person)o2).getFirstName());
            }
        }

    /**
    * Test of the {@link CompositeAggregator} aggregator.
    */
    @Test
    public void compositeStreaming()
        {
        NamedCache     cache     = getNamedCache();
        ValueExtractor extrFirst = new ReflectionExtractor("getFirstName");
        CompositeAggregator agent = CompositeAggregator.createInstance(
            new InvocableMap.StreamingAggregator[]
                {
                new ComparableMin("getFirstName"),
                new ComparableMax("getLastName"),
                });

        testEmpty(cache, agent);

        Person.fillRandom(cache, 1000);

        String[] asFirst = Person.FIRST_NAMES.clone();
        String[] asLast  = Person.LAST_NAMES.clone();
        Arrays.sort(asFirst);
        Arrays.sort(asLast);

        List listResult = (List) cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Not null", listResult.get(0) == null && listResult.get(1) == null);

        Object oId       = cache.keySet().iterator().next();
        Person person    = (Person) cache.get(oId);

        listResult = (List) cache.aggregate(Collections.singletonList(oId), agent);
        assertTrue("Result=" + listResult.get(0), equals(listResult.get(0), person.getFirstName()));
        assertTrue("Result=" + listResult.get(1), equals(listResult.get(1), person.getLastName()) );

        listResult = (List) cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + listResult.get(0), equals(listResult.get(0), asFirst[0]));
        assertTrue("Result=" + listResult.get(1), equals(listResult.get(1), asLast[asLast.length - 1]) );

        cache.addIndex(extrFirst, true, null);
        try
            {
            listResult = (List) cache.aggregate(AlwaysFilter.INSTANCE, agent);
            assertTrue("Result=" + listResult.get(0), equals(listResult.get(0), asFirst[0]));
            assertTrue("Result=" + listResult.get(1), equals(listResult.get(1), asLast[asLast.length - 1]) );
            }
        finally
            {
            removeIndexFrom(cache, extrFirst);
            }

        agent = CompositeAggregator.createInstance(
            new InvocableMap.StreamingAggregator[]
                {
                new DistinctValues("getFirstName"),
                new DistinctValues("getLastName"),
                });

        cache.clear();

        // the composite aggregator's result structures are more complex
        // that covered by the testEmpty() helper
        listResult = (List) cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Not empty", ((Set) listResult.get(0)).isEmpty() && ((Set) listResult.get(1)).isEmpty());

        listResult = (List) cache.aggregate(Collections.singletonList("1"), agent);
        assertTrue("Not empty", ((Set) listResult.get(0)).isEmpty() && ((Set) listResult.get(1)).isEmpty());

        listResult = (List) cache.aggregate((Filter) null, agent);
        assertTrue("Not empty", ((Set) listResult.get(0)).isEmpty() && ((Set) listResult.get(1)).isEmpty());

        listResult = (List) cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertTrue("Not empty", ((Set) listResult.get(0)).isEmpty() && ((Set) listResult.get(1)).isEmpty());

        Person.fillRandom(cache, 1000);

        listResult = (List) cache.aggregate(NullImplementation.getSet(), agent);
        assertTrue("Not empty", ((Set) listResult.get(0)).isEmpty() && ((Set) listResult.get(1)).isEmpty());

        oId    = cache.keySet().iterator().next();
        person = (Person) cache.get(oId);
        Set setFirst = Collections.singleton(person.getFirstName());
        Set setLast  = Collections.singleton(person.getLastName());

        listResult = (List) cache.aggregate(Collections.singletonList(oId), agent);
        assertTrue("Result=" + listResult.get(0), equals(listResult.get(0), setFirst));
        assertTrue("Result=" + listResult.get(1), equals(listResult.get(1), setLast) );

        setFirst = new HashSet();
        setLast  = new HashSet();

        for (int i = 0, c = Person.FIRST_NAMES.length; i < c; ++i)
            {
            setFirst.add(Person.FIRST_NAMES[i]);
            }
        for (int i = 0, c = Person.LAST_NAMES.length; i < c; ++i)
            {
            setLast.add(Person.LAST_NAMES[i]);
            }

        listResult = (List) cache.aggregate((Filter) null, agent);
        assertTrue("Result=" + listResult.get(0), equals(listResult.get(0), setFirst));
        assertTrue("Result=" + listResult.get(1), equals(listResult.get(1), setLast) );

        cache.addIndex(extrFirst, true, null);
        try
            {
            listResult = (List) cache.aggregate(AlwaysFilter.INSTANCE, agent);
            assertTrue("Result=" + listResult.get(0), equals(listResult.get(0), setFirst));
            assertTrue("Result=" + listResult.get(1), equals(listResult.get(1), setLast) );
            }
        finally
            {
            removeIndexFrom(cache, extrFirst);
            }

        cache.clear();
        Eventually.assertThat(cache.size(), is(0));
        }

    /**
    * Test of the {@link ReducerAggregator} aggregator.
    */
    @Test
    public void reducerAggregator()
        {
        NamedCache        cache = getNamedCache();
        ReducerAggregator agent = new ReducerAggregator(new MultiExtractor("getId,getFirstName,getLastName"));
        Map               m     = (Map) cache.aggregate(AlwaysFilter.INSTANCE, agent);

        try
            {
            assertEquals("expect 0 aggregation against an empty cache", m.size(), cache.size());
            }
        catch (Throwable t)
            {
            t.printStackTrace();
            printCache(cache);
            }
        cache.clear();

        Person p = new Person("666-22-9999");
        p.setFirstName("David");
        p.setLastName("Person1");
        cache.put("P1", p);

        p = new Person("666-22-1111");
        p.setFirstName("George");
        p.setLastName("Person2");
        cache.put("P2", p);

        m = (Map) cache.aggregate(AlwaysFilter.INSTANCE,agent);

        assertEquals("assert cache with 2 elements has an aggregation of 2", m.size(), cache.size());
        printCache(cache);

        List results = (List) m.get("P1");
        assertEquals(3, results.size());
        assertEquals("666-22-9999", results.get(0));
        assertEquals("David", results.get(1));
        assertEquals("Person1", results.get(2));

        results = (List) m.get("P2");
        assertEquals(3, results.size());
        assertEquals("666-22-1111", results.get(0));
        assertEquals("George", results.get(1));
        assertEquals("Person2", results.get(2));

        cache.clear();
        Eventually.assertThat(cache.size(), is(0));
        }

    /**
    * Test of the {@link GroupAggregator} aggregator.
    */
    @Test
    public void groupAggregator()
        {
        NamedCache        cache  = getNamedCache();
        GroupAggregator[] aAgent = new GroupAggregator[4];

        // Use Long instead of Double to ignore lost precision

        // select sum(Price) group-by Symbol
        aAgent[0] = GroupAggregator.createInstance("getSymbol",
            new LongSum("getPrice"));

        // select sum(Price),sum(Lot) group-by Symbol
        aAgent[1] = GroupAggregator.createInstance("getSymbol",
            CompositeAggregator.createInstance(
                new InvocableMap.EntryAggregator[]
                    {
                    new LongSum("getPrice"),
                    new LongSum("getLot"),
                    }));

        // select average(Price) group-by Symbol,OddLot
        aAgent[2] = GroupAggregator.createInstance("getSymbol,isOddLot",
            new DoubleMax("getPrice"));

        // select max(Price),min(Price),average(Lot) group-by Symbol,OddLot
        aAgent[3] = GroupAggregator.createInstance("getSymbol,isOddLot",
            CompositeAggregator.createInstance(
                new InvocableMap.EntryAggregator[]
                    {
                    new LongMax("getPrice"),
                    new LongMin("getPrice"),
                    new LongSum("getLot"),
                    }));

        // select max(Price) as MP group-by Symbol having MP < 98
        // aAgent[4] = GroupAggregator.createInstance("getSymbol",
        // new DoubleMax("getPrice"),
        // new LessFilter(IdentityExtractor.INSTANCE, new Double(98.0d)));

        cache.clear();

        for (int i = 0, c = aAgent.length; i < c; i++)
            {
            testEmpty(cache, aAgent[i]);
            }

        Trade.fillRandom(cache, 1000);
        Eventually.assertDeferred(() -> cache.size(), is(1000));

        NamedCache cacheTest = new WrapperNamedCache(new LocalCache(), "test");
        cacheTest.putAll(cache);
        Eventually.assertDeferred(() -> cacheTest.size(), is(1000));

        for (int i = 0, c = aAgent.length; i < c; i++)
            {
            GroupAggregator groupAggregator = aAgent[i];
            Eventually.assertDeferred(() -> ((Map) cache.aggregate(NullImplementation.getSet(), groupAggregator)).isEmpty(),
                    is(true));
            }

        Map mapResult, mapTest;

        for (int i = 0; i < aAgent.length; i++)
            {
            mapResult = (Map) cache.aggregate(aAgent[i]);
            mapTest   = (Map) cacheTest.aggregate(aAgent[i]);

            // NOTE: GroupAggregator w/ Filter requires data affinity to return
            //       correct data, so the correctness check is relaxed
            boolean fOK = (i == 4 && getCacheServerCount() > 1) ?
                mapResult.keySet().containsAll(mapTest.keySet()) : mapResult.equals(mapTest);
            if (!fOK)
                {
                fail("cache " + cache.getCacheName() + "; agent " + aAgent[i] +
                    "; servers=" + getCacheServerCount() +
                    "; result=" + mapResult + ", test=" + mapTest);
                }
            }

        cache.addIndex(new ReflectionExtractor("getSymbol"), false, null);
        cache.addIndex(new ReflectionExtractor("isOddLot"), false, null);
        try
            {
            for (int i = 0, c = aAgent.length; i < c; i++)
                {
                mapResult = (Map) cache.aggregate((Filter) null, aAgent[i]);
                mapTest   = (Map) cacheTest.aggregate((Filter) null, aAgent[i]);

                // NOTE: GroupAggregator w/ Filter requires data affinity to return
                //       correct data, so the correctness check is relaxed
                boolean fOK = (i == 4 && getCacheServerCount() > 1) ?
                    mapResult.keySet().containsAll(mapTest.keySet()) : mapResult.equals(mapTest);
                if (!fOK)
                    {
                    fail("cache " + cache.getCacheName() + "; agent " + aAgent[i] +
                        "; result= " + mapResult + ", test=" + mapTest);
                    }
                }
            }
        finally
            {
            removeIndexFrom(cache, new ReflectionExtractor("getSymbol"));
            removeIndexFrom(cache, new ReflectionExtractor("isOddLot"));
            }

        // select average(Price) group-by Symbol
        aAgent[0] = GroupAggregator.createInstance("getSymbol",
            new DoubleAverage("getPrice"));

        // select average(Price),count() group-by Symbol
        aAgent[1] = GroupAggregator.createInstance("getSymbol",
            CompositeAggregator.createInstance(
                new InvocableMap.EntryAggregator[]
                    {
                    new Count(),
                    new DoubleAverage("getPrice"),
                    }));

        Map mapAll      = (Map) cache.aggregate(AlwaysFilter.INSTANCE, aAgent[0]);
        Map mapSmallLot = (Map) cache.aggregate(new EqualsFilter("isOddLot", Boolean.TRUE), aAgent[1]);
        Map mapLargeLot = (Map) cache.aggregate(new EqualsFilter("isOddLot", Boolean.FALSE), aAgent[1]);
        for (Iterator iter = mapAll.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();

            String sSymbol    = (String) entry.getKey();
            Double DblAverage = (Double) entry.getValue();

            List listSmallResult = (List) mapSmallLot.get(sSymbol);
            List listLargeResult = (List) mapLargeLot.get(sSymbol);
            double dblPrice = 0.0;
            int    nCount   = 0;
            if (listSmallResult != null)
                {
                int c = ((Integer) listSmallResult.get(0)).intValue();
                dblPrice += ((Double) listSmallResult.get(1)).doubleValue()*c;
                nCount   += c;
                }
            if (listLargeResult != null)
                {
                int c = ((Integer) listLargeResult.get(0)).intValue();
                dblPrice += ((Double) listLargeResult.get(1)).doubleValue()*c;
                nCount   += c;
                }

            double dblAverage = dblPrice/nCount;
            if (Math.abs(DblAverage.doubleValue() - dblAverage) > 0.001)
                {
                fail("cache " + cache.getCacheName() + "; symbol=" + sSymbol +
                    "; " + DblAverage + "!=" + dblPrice/nCount);
                }
            }

        cache.clear();
        Eventually.assertThat(cache.size(), is(0));
        }

    /**
    * Test of the {@link TopNAggregator}.
    */
    @Test
    public void topNAggregator()
        {
        NamedCache     cache = getNamedCache();
        TopNAggregator agent = new TopNAggregator(IdentityExtractor.INSTANCE, SafeComparator.INSTANCE, 10);

        testEmpty(cache, agent);

        // use a new cache to force the splitting of the aggregator (bypass
        // the $Storage.QuerySizeCache)
        cache = getNamedCache(getCacheName() + "-topN");

        Map map   = new HashMap();
        int cKeys = 10000;
        for (int i = 1; i <= cKeys; ++i)
            {
            map.put(String.valueOf(i), i);
            }
        cache.putAll(map);

        Object[] aoTop10 = new Object[10];
        for (int i = 0; i < 10; i++)
            {
            aoTop10[i] = cKeys - i;
            }

        Object[] oResult = (Object[]) cache.aggregate(NullImplementation.getSet(), agent);
        assertArrayEquals("Result=" + Arrays.toString(oResult), new Object[0], oResult);

        oResult = (Object[]) cache.aggregate(Collections.singletonList("1"), agent);
        assertArrayEquals("Result=" + Arrays.toString(oResult), new Object[] {1}, oResult);

        oResult = (Object[]) cache.aggregate((Filter) null, agent);
        assertArrayEquals("Result=" + Arrays.toString(oResult), aoTop10, oResult);

        oResult = (Object[]) cache.aggregate(AlwaysFilter.INSTANCE, agent);
        assertArrayEquals("Result=" + Arrays.toString(oResult), aoTop10, oResult);

        // test duplicate values
        cache.clear();

        cKeys = 100;
        map   = new HashMap();
        for (int i = 1; i <= cKeys; ++i)
            {
            map.put(String.valueOf(i), i / 2);
            }
        cache.putAll(map);

        aoTop10 = new Object[10];
        for (int i = 0; i < 10; ++i)
            {
            aoTop10[i] = (cKeys - i) / 2;
            }

        oResult = (Object[]) cache.aggregate((Filter) null, agent);
        assertArrayEquals("Result=" + Arrays.toString(oResult), aoTop10, oResult);

        cache.clear();
        Eventually.assertThat(cache.size(), is(0));
        }


    /**
    * Regression test for COH-2723.
    */
    @Test
    public void testCoh2723()
        {
        NamedCache cache = getNamedCache();
        InvocableMap.EntryAggregator aggrNotEmpty = new NotEmptyAggregator();

        Boolean F = (Boolean) cache.aggregate(AlwaysFilter.INSTANCE, aggrNotEmpty);
        assertTrue("not empty", F == null || !F.booleanValue());

        cache.put("key", "value");

        F = (Boolean) cache.aggregate(AlwaysFilter.INSTANCE, aggrNotEmpty);
        assertTrue("empty", F.booleanValue());

        cache.clear();
        Eventually.assertThat(cache.size(), is(0));
        }

    public static class NotEmptyAggregator
            implements InvocableMap.ParallelAwareAggregator
        {
        public Object aggregateResults(Collection collResults)
            {
            return collResults.contains(Boolean.TRUE);
            }

        public InvocableMap.EntryAggregator getParallelAggregator()
            {
            return this;
            }

        public Object aggregate(Set setEntries)
            {
            return setEntries.isEmpty() ? null : Boolean.TRUE;
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Test empty aggregation request results.
    *
    * @param cache  the target cache
    * @param agent  the agent to run
    */
    static protected void testEmpty(NamedCache cache, InvocableMap.EntryAggregator agent)
        {
        Object oResult;

        oResult = cache.aggregate(NullImplementation.getSet(), agent);
        if (oResult instanceof List)
            {
            List listResult = (List) oResult;
            for (int i = 0, c = listResult.size(); i < c; i++)
                {
                assertTrue("Element not null:" + listResult.get(i), listResult.get(i) == null);
                }
            }
        else if (oResult instanceof Set)
            {
            Set setResult = (Set) oResult;
            assertTrue("Result=" + oResult, setResult.isEmpty());
            }
        else if (oResult instanceof Map)
            {
            Map mapResult = (Map) oResult;
            assertTrue("Result=" + oResult, mapResult.isEmpty());
            }
        else if (oResult instanceof Object[])
            {
            Object[] aoResult = (Object[]) oResult;
            assertEquals("Result=" + oResult, 0, aoResult.length);
            }
        else
            {
            assertTrue("Result=" + oResult, oResult == null);
            }

        oResult = cache.aggregate(Collections.singletonList("1"), agent);
        if (oResult instanceof List)
            {
            List listResult = (List) oResult;
            for (int i = 0, c = listResult.size(); i < c; i++)
                {
                assertTrue("Element not null:" + listResult.get(i), listResult.get(i) == null);
                }
            }
        else if (oResult instanceof Set)
            {
            Set setResult = (Set) oResult;
            assertTrue("Result=" + oResult, setResult.isEmpty());
            }
        else if (oResult instanceof Map)
            {
            Map mapResult = (Map) oResult;
            assertTrue("Result=" + oResult, mapResult.isEmpty());
            }
        else if (oResult instanceof Object[])
            {
            Object[] aoResult = (Object[]) oResult;
            assertEquals("Result=" + oResult, 0, aoResult.length);
            }
        else
            {
            assertTrue("Result=" + oResult, oResult == null);
            }

        oResult = cache.aggregate((Filter) null, agent);
        if (oResult instanceof List)
            {
            List listResult = (List) oResult;
            for (int i = 0, c = listResult.size(); i < c; i++)
                {
                assertTrue("Element not null:" + listResult.get(i), listResult.get(i) == null);
                }
            }
        else if (oResult instanceof Set)
            {
            Set setResult = (Set) oResult;
            assertTrue("Result=" + oResult, setResult.isEmpty());
            }
        else if (oResult instanceof Map)
            {
            Map mapResult = (Map) oResult;
            assertTrue("Result=" + oResult, mapResult.isEmpty());
            }
        else if (oResult instanceof Object[])
            {
            Object[] aoResult = (Object[]) oResult;
            assertEquals("Result=" + oResult, 0, aoResult.length);
            }
        else
            {
            assertTrue("Result=" + oResult, oResult == null);
            }

        oResult = cache.aggregate(AlwaysFilter.INSTANCE, agent);
        if (oResult instanceof List)
            {
            List listResult = (List) oResult;
            for (int i = 0, c = listResult.size(); i < c; i++)
                {
                assertTrue("Element not null:" + listResult.get(i), listResult.get(i) == null);
                }
            }
        else if (oResult instanceof Set)
            {
            Set setResult = (Set) oResult;
            assertTrue("Result=" + oResult, setResult.isEmpty());
            }
        else if (oResult instanceof Map)
            {
            Map mapResult = (Map) oResult;
            assertTrue("Result=" + oResult, mapResult.isEmpty());
            }
        else if (oResult instanceof Object[])
            {
            Object[] aoResult = (Object[]) oResult;
            assertEquals("Result=" + oResult, 0, aoResult.length);
            }
        else
            {
            assertTrue("Result=" + oResult, oResult == null);
            }
        }

    /**
    * Test whether or not two BigDecimal values are equal to each other.
    *
    * @param dec1  the first BigDecimal to compare
    * @param dec2  the second BigDecimal to compare
    *
    * @return true if the two BigDecimal values are equal
    */
    public static boolean equalsDec(BigDecimal dec1, BigDecimal dec2)
        {
        BigDecimal decDiff = dec1.subtract(dec2);
        return decDiff.doubleValue() == 0.0;
        }

    /**
    * Fill the specified map with BigDecimals, BigIntegers and long values.
    *
    * @param cache  the target cache
    * @param iStart the starting key
    * @param cnt    the number of new entries
    */
    public static void fillBigNumbers(Map cache, int iStart, int cnt)
        {
        int nType = 0;
        for (int i = iStart; i <= cnt; ++i)
            {
            Object oVal;
            switch (nType++ % 3)
                {
                default:
                case 0:
                    oVal = BigDecimal.valueOf(i);
                    break;
                case 1:
                    oVal = BigInteger.valueOf(i);
                    break;
                case 2:
                    oVal = (double) i;
                    break;
                }
            cache.put(String.valueOf(i), oVal);
            }
        }

    public void removeIndexFrom(NamedCache namedCache, ValueExtractor extractor)
        {
        namedCache.removeIndex(extractor);
        }

    public void printCache(NamedCache cache)
        {
        StringBuilder sb = new StringBuilder("cache " + cache.getCacheName());
        sb.append(", size=").append(cache.size()).append(System.lineSeparator());

        int MAX_ENTRIES = 10;
        int cEntry = 0;
        for (Object key: cache.keySet())
            {
            cEntry++;
            sb.append("key=").append(key).append(",value=").append(cache.get(key)).append(System.lineSeparator());
            if (++cEntry >= MAX_ENTRIES)
                {
                sb.append("truncated printing cache " + cache.getCacheName() + " entries after " + MAX_ENTRIES + " entries");
                break;
                }
            }
        System.out.println(sb.toString());
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Return the name of the cache used in all test methods.
    *
    * @return the name of the cache used in all test methods
    */
    protected String getCacheName()
        {
        return m_sCache;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of the cache used in all test methods.
    */
    protected final String m_sCache;
    }
