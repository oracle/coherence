/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.oracle.coherence.testing.cache.BaseContinuousQueryCacheTest;
import com.tangosol.net.NamedCache;

import com.oracle.coherence.testing.util.BaseMapTest;
import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.BetweenFilter;
import com.tangosol.util.filter.GreaterEqualsFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.LessFilter;
import com.tangosol.util.filter.LimitFilter;

import org.junit.Test;
import org.junit.Assert;

import java.io.Serializable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import data.Person;


/**
* Unit tests for the ContinuousQueryCache.
*
* @author cp  Jan 24, 2006
*/
public class ContinuousQueryCacheTest
        extends BaseContinuousQueryCacheTest
    {
    /**
    * Basic set of unit tests on CQC.
    */
    @Test
    public void testBasic()
        {
        NamedCache cacheBase = getNewCache("cqc-test");
        cacheBase.put("1", "one");
        cacheBase.put("2", "two");
        cacheBase.put("3", "three");
        Filter filter   = new AlwaysFilter();
        NamedCache cacheCQC = new ContinuousQueryCache(cacheBase, filter,
                new BaseMapTest.EventPrinter());

        // cacheCQC.addMapListener(new EventPrinter());

        System.out.println("cacheBase.put(\"hello\", \"world\");");
        cacheBase.put("hello", "world");
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);

        System.out.println("cacheCQC.put(\"test\", \"foo\");");
        cacheCQC.put("test", "foo");
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);

        System.out.println("cacheCQC.put(\"test\", \"bar\");");
        cacheCQC.put("test", "bar");
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);


        System.out.println("cacheCQC.remove(\"test\");");
        cacheCQC.remove("test");
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);

        // test thread shut-down
        /*
        try
            {
            Thread.sleep(30000);
            }
        catch (InterruptedException e) {}
        */

        System.out.println("cacheCQC.clear();");
        cacheCQC.clear();
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);

        /*
        out("testMap(cacheCQC);");
        testMap(cacheCQC);
        assertIdenticalMaps(cacheBase, cacheCQC);

        out("testMultithreadedMap(cacheCQC);");
        testMultithreadedMap(cacheCQC);
        assertIdenticalMaps(cacheBase, cacheCQC);
        */
        }

    /**
    * Mirror a cache's entire contents over time.
    */
    public void mirrorCache_helper(boolean cacheValues)
        {
        NamedCache cacheBase = getNewCache("cqc-test");
        cacheBase.put("1", "one");
        cacheBase.put("2", "two");
        cacheBase.put("3", "three");
        Filter     filter   = new AlwaysFilter();
        NamedCache cacheCQC = new ContinuousQueryCache(cacheBase, filter,
                cacheValues);

        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);

        cacheBase.put("4", "four");
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);

        cacheBase.remove("1");
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);

        cacheCQC.put("5", "five");
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);

        cacheBase.remove("5");
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);
        }

    /**
    * Mirror a cache's entire contents over time, without caching values.
    */
    @Test
    public void mirrorCache()
        {
        mirrorCache_helper(false);
        }

    /**
    * Mirror a cache's entire contents over time,  caching values.
    */
    @Test
    public void mirrorCache_caching()
        {
        mirrorCache_helper(true);
        }

    /**
    * Run MapTest.testMap against non caching CQC
    */
    @Test
    public void testMap()
        {
        testFunctorAlwaysCache(new ContinuousQueryCacheTest.Functor() {
            public void execute(Object map)
                {
                BaseMapTest.testMap((Map) map);
                }
            }, false);
        }

    /**
    * Run MapTest.testMap against caching CQC
    */
    @Test
    public void testMap_caching()
        {
        testFunctorAlwaysCache(new ContinuousQueryCacheTest.Functor() {
            public void execute(Object map)
                {
                BaseMapTest.testMap((Map) map);
                }
            }, true);
        }

    /**
    * Run MapTest.testObservableMap against non caching CQC
    */
    @Test
    public void testObservableMap()
        {
        testFunctorAlwaysCache(new ContinuousQueryCacheTest.Functor() {
            public void execute(Object map)
                {
                BaseMapTest.testObservableMap((ObservableMap) map);
                }
            }, false);
        }

    /**
    * Run MapTest.testObservableMap against non caching CQC
    */
    @Test
    public void testObservableMap_caching()
        {
        testFunctorAlwaysCache(new ContinuousQueryCacheTest.Functor() {
            public void execute(Object map)
                {
                BaseMapTest.testObservableMap((ObservableMap) map);
                }
            }, true);
        }

    /**
    * Test wrieability on a read only cache
    */
    @Test
    public void writeErrorCache()
        {
        NamedCache cacheBase = getNewCache("cqc-test");
        cacheBase.put("1", "one");
        cacheBase.put("2", "two");
        cacheBase.put("3", "three");

        Filter filter = new AlwaysFilter();
        ContinuousQueryCache cacheCQC = new ContinuousQueryCache(cacheBase,
                filter, false);
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);

        cacheBase.put("4", "four");
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);

        cacheCQC.setReadOnly(true);

        cacheBase.remove("1");
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);

        try
            {
            cacheCQC.remove("2");
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    /**
    * Test RW -> RO -> RW
    */
    @Test
    public void RWErrorCache()
        {
        NamedCache cacheBase = getNewCache("cqc-test");
        Filter               filter    = new AlwaysFilter();
        ContinuousQueryCache cacheCQC  = new ContinuousQueryCache(cacheBase,
                filter, false);
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);

        cacheCQC.setReadOnly(true);
        try
            {
            cacheCQC.setReadOnly(false);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    /**
    * Test keySet with LimitFilter
    *
    */
    public void limitedKeySet_helper(boolean fCacheValues)
        {
        NamedCache cacheBase = getNewCache("cqc-test");
        cacheBase.put("1", 1);
        cacheBase.put("2", 2);
        cacheBase.put("3", 3);
        cacheBase.put("4", 4);
        cacheBase.put("5", 5);

        Filter filter = new LessFilter("intValue", 4);
        ContinuousQueryCache cacheCQC = new ContinuousQueryCache(cacheBase,
                filter, fCacheValues);

        LimitFilter filterLimit = new LimitFilter(
            new GreaterFilter("intValue", 1), 1 /*page size*/);
        Set setKeys = null;
        int page    = 0;
        Set setExpected = new HashSet(Arrays.asList(
                new String[] {"2", "3"}));

        do
            {
            filterLimit.setPage(page++);
            setKeys = cacheCQC.keySet(filterLimit);
            if (!setKeys.isEmpty())
                {
                assertTrue("result was unexpected: " + setKeys,
                        setKeys.size() == 1 && setExpected.removeAll(setKeys));
                }
            }
        while (!setKeys.isEmpty());

        assertTrue("some expected keys were not returned: " + setExpected,
                setExpected.isEmpty());
        }

    /**
    * Test keySet with LimitFilter with non-cached values
    */
    @Test
    public void limitedKeySet()
        {
        limitedKeySet_helper(false);
        }

    /**
    * Test keySet with LimitFilter with cached values
    */
    @Test
    public void limitedKeySet_caching()
        {
        limitedKeySet_helper(true);
        }

    /**
    * Test entrySet with LimitFilter
    *
    */
    public void limitedEntrySet_helper(boolean fCacheValues)
        {
        NamedCache cacheBase = getNewCache("cqc-test");
        cacheBase.put("1", 1);
        cacheBase.put("2", 2);
        cacheBase.put("3", 3);
        cacheBase.put("4", 4);
        cacheBase.put("5", 5);

        Filter filter = new LessFilter("intValue", 4);
        ContinuousQueryCache cacheCQC = new ContinuousQueryCache(cacheBase,
                filter, fCacheValues);

        LimitFilter filterLimit = new LimitFilter(
            new GreaterFilter("intValue", 1), 1 /*page size*/);
        Set setKeys = null;
        int page    = 0;
        Set setExpected = new HashSet(Arrays.asList(
                new SimpleMapEntry[] {new SimpleMapEntry("2", 2),
                new SimpleMapEntry("3", 3)}));

        do
            {
            filterLimit.setPage(page++);
            setKeys = cacheCQC.entrySet(filterLimit);
            if (!setKeys.isEmpty())
                {
                assertTrue("result was unexpected: " + setKeys,
                        setKeys.size() == 1 && setExpected.removeAll(setKeys));
                }
            }
        while (!setKeys.isEmpty());

        assertTrue("some expected keys were not returend: " + setExpected,
                setExpected.isEmpty());
        }

    /**
    * Test entrySet with LimitFilter with non-cached values
    */
    @Test
    public void limitedEntrySet()
        {
        limitedEntrySet_helper(false);
        }

    /**
    * Test entrySet with LimitFilter with cached values
    */
    @Test
    public void limitedEntrySet_caching()
        {
        limitedEntrySet_helper(true);
        }

    /**
    * Test of getAll
    */
    public void getAllCache_helper(boolean fCacheValues, String[] asKeys,
                                   Map mapExpected)
        {
        NamedCache cacheBase = getNewCache("cqc-test");
        cacheBase.put("1", 1);
        cacheBase.put("2", 2);
        cacheBase.put("3", 3);
        cacheBase.put("4", 4);
        cacheBase.put("5", 5);

        Filter filter = new LessFilter("intValue", 4);
        ContinuousQueryCache cacheCQC = new ContinuousQueryCache(cacheBase,
                filter, fCacheValues);

        Map mapResult = cacheCQC.getAll(Arrays.asList(asKeys));
        BaseMapTest.assertIdenticalMaps(mapExpected, mapResult);
        }

    /**
    * Test of getAll, with a single key (hit)
    */
    public void getAllCache_singleHit_helper(boolean fCacheValues)
        {
        Map mapExpected = new HashMap();
        mapExpected.put("2", 2);
        getAllCache_helper(fCacheValues, new String[] {"2"}, mapExpected);
        }

    /**
    * Test of getAll, with a single key (hit)
    */
    @Test
    public void getAllCache_singleHit()
        {
        getAllCache_singleHit_helper(false);
        }

    /**
    * Test of getAll, with a single key (hit)
    */
    @Test
    public void getAllCache_singleHit_cache()
        {
        getAllCache_singleHit_helper(true);
        }

    /**
    * Test of getAll, with a single key (mis)
    */
    public void getAllCache_singleMis_helper(boolean fCacheValues)
        {
        Map mapExpected = new HashMap();
        getAllCache_helper(fCacheValues, new String[] {"4"}, mapExpected);
        }

    /**
    * Test of getAll, with a single key (mis)
    */
    @Test
    public void getAllCache_singleMis()
        {
        getAllCache_singleMis_helper(false);
        }

    /**
    * Test of getAll, with a single key (mis)
    */
    @Test
    public void getAllCache_singleMis_cache()
        {
        getAllCache_singleMis_helper(true);
        }

    /**
    * Test of getAll, with a multi key (hit)
    */
    public void getAllCache_multiHit_helper(boolean fCacheValues)
        {
        Map mapExpected = new HashMap();
        mapExpected.put("2", 2);
        mapExpected.put("3", 3);
        getAllCache_helper(fCacheValues, new String[] {"2", "3"}, mapExpected);
        }

    /**
    * Test of getAll, with a multi key (hit)
    */
    @Test
    public void getAllCache_multiHit()
        {
        getAllCache_multiHit_helper(false);
        }

    /**
    * Test of getAll, with a multi key (hit)
    */
    @Test
    public void getAllCache_multiHit_cache()
        {
        getAllCache_multiHit_helper(true);
        }

    /**
    * Test of getAll, with a multi key (miss)
    */
    public void getAllCache_multiMiss_helper(boolean fCacheValues)
        {
        Map mapExpected = new HashMap();
        mapExpected.put("2", 2);
        getAllCache_helper(fCacheValues, new String[] {"2", "4"}, mapExpected);
        }

    /**
    * Test of getAll, with a multi key (miss)
    */
    @Test
    public void getAllCache_multiMiss()
        {
        getAllCache_multiMiss_helper(false);
        }

    /**
    * Test of getAll, with a multi key (miss)
    */
    @Test
    public void getAllCache_multiMiss_cache()
        {
        getAllCache_multiMiss_helper(true);
        }

    /**
    * Test of getAll, with a multi key (full miss)
    */
    public void getAllCache_fullMiss_helper(boolean fCacheValues)
        {
        Map mapExpected = new HashMap();
        getAllCache_helper(fCacheValues, new String[] {"4", "5"}, mapExpected);
        }

    /**
    * Test of getAll, with a multi key (full miss)
    */
    @Test
    public void getAllCache_fullMiss()
        {
        getAllCache_fullMiss_helper(false);
        }

    /**
    * Test of getAll, with a multi key (full miss)
    */
    @Test
    public void getAllCache_fullMiss_cache()
        {
        getAllCache_fullMiss_helper(true);
        }

    /**
    * Test of CQC with a range filter.
    */
    public void cqcBetween_helper(boolean fCacheValues)
        {
        NamedCache cacheBase = getNewCache("cqc-test");
        cacheBase.put("1", 1);
        cacheBase.put("2", 2);
        cacheBase.put("3", 3);
        cacheBase.put("4", 4);
        cacheBase.put("5", 5);
        cacheBase.put("11", 11);
        cacheBase.put("12", 12);

        Filter filter = new BetweenFilter("intValue",
                                          3, 10);
        ContinuousQueryCache cacheCQC = new ContinuousQueryCache(cacheBase,
                filter, fCacheValues);

        Map mapExpected = new HashMap();
        mapExpected.put("3", 3);
        mapExpected.put("4", 4);
        mapExpected.put("5", 5);

        BaseMapTest.assertIdenticalMaps(mapExpected, cacheCQC);

        // enter
        cacheBase.put("6", 6);
        mapExpected.put("6", 6);
        BaseMapTest.assertIdenticalMaps(mapExpected, cacheCQC);

        // update within
        cacheBase.put("6", 7);
        mapExpected.put("6", 7);
        BaseMapTest.assertIdenticalMaps(mapExpected, cacheCQC);

        // update left
        cacheBase.put("6", 1);
        mapExpected.remove("6");
        BaseMapTest.assertIdenticalMaps(mapExpected, cacheCQC);

        // update entered
        cacheBase.put("6", 6);
        mapExpected.put("6", 6);
        BaseMapTest.assertIdenticalMaps(mapExpected, cacheCQC);

        // remove
        cacheBase.remove("6");
        mapExpected.remove("6");
        BaseMapTest.assertIdenticalMaps(mapExpected, cacheCQC);
        }

    /**
    * Test of CQC with a range filter.
    */
    @Test
    public void cqcBetween()
        {
        cqcBetween_helper(false);
        }

    /**
    * Test of CQC with a range filter.
    */
    @Test
    public void cqcBetween_caching()
        {
        cqcBetween_helper(true);
        }

    /**
    * Test of CQC release method.
    */
    @Test
    public void releaseTest()
        {
        NamedCache cacheBase = getNewCache("cqc-test");
        cacheBase.put("1", 1);
        cacheBase.put("2", 2);
        cacheBase.put("3", 3);

        Filter filter = new BetweenFilter("intValue",
                                          3, 10);
        ContinuousQueryCache cacheCQC = new ContinuousQueryCache(cacheBase,
                filter, false);
        cacheCQC.release();
        }

    /**
    * Test CQC on CQC
    */
    public void overlappingCQC_helper(boolean fCacheValues1, boolean fCacheValues2)
        {
        NamedCache cacheBase = getNewCache("cqc-test");
        cacheBase.put("1", 1);
        cacheBase.put("2", 2);
        cacheBase.put("3", 3);
        cacheBase.put("4", 4);
        cacheBase.put("5", 5);

        Filter filter1 = new LessFilter("intValue", 5);
        ContinuousQueryCache cacheCQC1 = new ContinuousQueryCache(cacheBase,
                filter1, fCacheValues1);

        Filter filter2 = new GreaterFilter("intValue", 1);
        ContinuousQueryCache cacheCQC2 = new ContinuousQueryCache(cacheCQC1,
                filter2, fCacheValues2);

        Map mapExpected = new HashMap();
        mapExpected.put("2", 2);
        mapExpected.put("3", 3);
        mapExpected.put("4", 4);

        BaseMapTest.assertIdenticalMaps(mapExpected, cacheCQC2);
        }

    /**
    * Test CQC on CQC
    */
    @Test
    public void overlappingCQC()
        {
        overlappingCQC_helper(false, false);
        }

    /**
    * Test CQC on CQC
    */
    @Test
    public void overlappingCQC_caching()
        {
        overlappingCQC_helper(true, true);
        }

    /**
    * Test CQC on CQC
    */
    @Test
    public void overlappingCQC_caching1()
        {
        overlappingCQC_helper(true, false);
        }

    /**
    * Test CQC on CQC
    */
    @Test
    public void overlappingCQC_caching2()
        {
        overlappingCQC_helper(false, true);
        }

    /**
    * CQC Index test.
    */
    @Test
    public void testIndex()
        {
        NamedCache cacheBase = getNewCache("cqc-test");
        cacheBase.put("one", 1);
        cacheBase.put("another_one", 1);
        cacheBase.put("one_more", 1);
        cacheBase.put("two", 2);
        cacheBase.put("three", 3);
        cacheBase.put("four", 4);
        cacheBase.put("four_again", 4);
        cacheBase.put("five", 5);
        cacheBase.put("five_a", 5);
        cacheBase.put("five_b", 5);
        cacheBase.put("five_c", 5);
        cacheBase.put("five_d", 5);

        ValueExtractor extractor = new IdentityExtractor();
        Filter                   filter    = new GreaterEqualsFilter(extractor,
                                                                     3);
        TestContinuousQueryCache cacheCQC  = new TestContinuousQueryCache(
                                        cacheBase, filter, true);

        cacheCQC.addIndex(extractor, false, null);

        Map mapIndex = cacheCQC.getIndexMap();
        assertTrue(mapIndex.size() == 1);

        MapIndex index = (MapIndex)mapIndex.get(extractor);

        Map indexContents = index.getIndexContents();

        Set setOne = (Set) indexContents.get(1);
        assertTrue(setOne == null);

        Set setTwo = (Set) indexContents.get(2);
        assertTrue(setTwo == null);

        Set setThree = (Set) indexContents.get(3);
        assertTrue(setThree.size()==1);
        assertTrue(setThree.contains("three"));

        assertTrue(index.get("three").equals(3));

        Set setFour = (Set) indexContents.get(4);
        assertTrue(setFour.size()==2);
        assertTrue(setFour.contains("four"));
        assertTrue(setFour.contains("four_again"));

        Set setFive = (Set) indexContents.get(5);
        assertTrue(setFive.size()==5);
        assertTrue(setFive.contains("five"));
        assertTrue(setFive.contains("five_a"));
        assertTrue(setFive.contains("five_b"));
        assertTrue(setFive.contains("five_c"));
        assertTrue(setFive.contains("five_d"));

        // test insert
        assertTrue(index.get("ten") == index.NO_VALUE);

        cacheBase.put("ten", 10);

        assertTrue(index.get("ten").equals(10));

        indexContents = index.getIndexContents();

        Set set = (Set) indexContents.get(10);
        assertTrue(set.size()==1);
        assertTrue(set.contains("ten"));

        // test update
        cacheBase.put("five", 55);

        assertTrue(index.get("five").equals(55));

        indexContents = index.getIndexContents();

        set = (Set) indexContents.get(55);
        assertTrue(set.size()==1);
        assertTrue(set.contains("five"));

        set = (Set) indexContents.get(5);
        assertTrue(set.size()==4);
        assertTrue(set.contains("five_a"));
        assertTrue(set.contains("five_b"));
        assertTrue(set.contains("five_c"));
        assertTrue(set.contains("five_d"));

        // test delete
        cacheBase.remove("three");

        assertTrue(index.get("three")== index.NO_VALUE);

        indexContents = index.getIndexContents();

        set = (Set) indexContents.get(3);
        assertTrue(set == null || set.size()==0);

        // query the CQC with an index
        filter = new GreaterFilter(extractor, 4);

        set = cacheCQC.keySet(filter);
        assertTrue(set.size() == 6);

        set = cacheCQC.entrySet(filter);
        assertTrue(set.size() == 6);

        Comparator comparator = new SafeComparator();
        set = cacheCQC.entrySet(filter, comparator);
        assertTrue(set.size() == 6);

        // test removeIndex
        cacheCQC.removeIndex(extractor);

        mapIndex = cacheCQC.getIndexMap();
        assertTrue(mapIndex == null || mapIndex.size() == 0);

        cacheCQC.addIndex(extractor, false, comparator);

        mapIndex = cacheCQC.getIndexMap();
        assertTrue(mapIndex.size() == 1);

        cacheCQC.setCacheValues(false);
        mapIndex = cacheCQC.getIndexMap();
        assertTrue(mapIndex == null || mapIndex.size() == 0);

        cacheCQC  = new TestContinuousQueryCache(cacheBase, filter, false);

        cacheCQC.addIndex(extractor, false, comparator);

        mapIndex = cacheCQC.getIndexMap();
        assertTrue(mapIndex == null || mapIndex.size() == 0);

        // test removeIndex
        cacheCQC.removeIndex(extractor);

        mapIndex = cacheCQC.getIndexMap();
        assertTrue(mapIndex == null || mapIndex.size() == 0);

        }

    /**
    * Unit test for COH-2532.
    */
    @Test
    public void testCoh2532()
        {
        NamedCache cacheBase = getNewCache("cqc-test");
        cacheBase.put(1, new Person("111-11-1111", "Homer", "Simpson", 1945, null, new String[0]));
        cacheBase.put(2, new Person("222-22-2222", "Marge", "Simpson", 1950, null, new String[0]));
        cacheBase.put(3, new Person("333-33-3333", "Bart", "Simpson", 1985, null, new String[0]));
        cacheBase.put(4, new Person("444-44-4444", "Lisa", "Simpson", 1987, null, new String[0]));

        ContinuousQueryCache cacheCQC = new ContinuousQueryCache(cacheBase, AlwaysFilter.INSTANCE,
               new ReflectionExtractor("getFirstName"));
        Assert.assertTrue(cacheCQC.isReadOnly());

        Map expectedView = new HashMap(5);
        expectedView.put(1, "Homer");
        expectedView.put(2, "Marge");
        expectedView.put(3, "Bart");
        expectedView.put(4, "Lisa");

        System.out.println(cacheCQC);
        BaseMapTest.assertIdenticalMaps(expectedView, cacheCQC);

        cacheBase.put(5, new Person("555-55-5555", "Maggie", "Simpson", 2000, null, new String[0]));
        expectedView.put(5, "Maggie");
        System.out.println(cacheCQC);

        BaseMapTest.assertIdenticalMaps(expectedView, cacheCQC);
        }

    /**
    * Testable CQC extension that gives access to the index map.
    */
    class TestContinuousQueryCache extends ContinuousQueryCache
        {

        public TestContinuousQueryCache(NamedCache cache, Filter filter, boolean fCacheValues)
            {
            super(cache, filter, fCacheValues);
            }

        public Map getIndexMap()
            {
            return super.getIndexMap();
            }
        }

    /**
    * ValueUpdater.
    */
    public static class TestValueUpdater
            implements ValueUpdater, Serializable
        {
        public TestValueUpdater()
            {          
            }
        
        public void update(Object oTarget, Object oValue)
            {
            ((Boolean[]) oTarget)[0] = (Boolean) oValue;
            }
        }  
    }
