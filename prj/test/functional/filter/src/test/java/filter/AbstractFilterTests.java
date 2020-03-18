/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package filter;


import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.net.partition.PartitionedIterator;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.net.cache.NearCache;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.comparator.ChainedComparator;
import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.extractor.ComparisonValueExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.extractor.ConditionalExtractor;

import com.tangosol.util.Filter;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.AllFilter;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.AnyFilter;
import com.tangosol.util.filter.BetweenFilter;
import com.tangosol.util.filter.ContainsAllFilter;
import com.tangosol.util.filter.ContainsAnyFilter;
import com.tangosol.util.filter.ContainsFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.GreaterEqualsFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.InFilter;
import com.tangosol.util.filter.IsNotNullFilter;
import com.tangosol.util.filter.IsNullFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;
import com.tangosol.util.filter.LessEqualsFilter;
import com.tangosol.util.filter.LessFilter;
import com.tangosol.util.filter.LikeFilter;
import com.tangosol.util.filter.LimitFilter;
import com.tangosol.util.filter.NeverFilter;
import com.tangosol.util.filter.NotEqualsFilter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.OrFilter;
import com.tangosol.util.filter.PartitionedFilter;
import com.tangosol.util.filter.XorFilter;

import common.AbstractFunctionalTest;

import data.Person;

import org.junit.Test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
* Filter functional testing.
*
* @author gg  2006.01.23
*/
public abstract class AbstractFilterTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new AbstractFilterTests that will use the cache with the given
    * name in all test methods.
    *
    * @param sCache  the test cache name
    */
    public AbstractFilterTests(String sCache)
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
        return getNamedCache(getCacheName());
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


    // ----- test methods ---------------------------------------------------

    /**
    * Test of the various Filter queries.
    */
    @Test
    public void test()
        {
        NamedCache cache = getNamedCache();

        final int SMALL = 12;
        final int LARGE = 512;

        out("Test without index: " + cache.getCacheName());
        cache.clear();
        removeIndexFrom(cache, EXTRACTOR_FIRSTNAME);
        removeIndexFrom(cache, EXTRACTOR_LASTNAME);
        doTest(cache, SMALL);
        doTest(cache, LARGE);

        if (!(cache instanceof NearCache))
            {
            out("Test with partial index: " + cache.getCacheName());
            cache.clear();
            cache.addIndex(EXTRACTOR_FIRSTNAME, true, null);
            doTest(cache, SMALL);
            doTest(cache, LARGE);

            out("Test with full index: " + cache.getCacheName());
            cache.clear();
            cache.addIndex(EXTRACTOR_FIRSTNAME, true, null);
            cache.addIndex(EXTRACTOR_LASTNAME, true, null);
            cache.addIndex(EXTRACTOR_KEY_LASTNAME, true, null);
            doTest(cache, SMALL);
            doTest(cache, LARGE);

            out("Test with conditional index: " + cache.getCacheName());
            cache.clear();
            removeIndexFrom(cache, EXTRACTOR_FIRSTNAME);
            removeIndexFrom(cache, EXTRACTOR_LASTNAME);
            removeIndexFrom(cache, EXTRACTOR_KEY_LASTNAME);
            // use a filter for the conditional indexes that will result in partial indexes
            Filter filter = new GreaterEqualsFilter("getBirthYear", 1950);
            // add the conditional indexes with the above filter
            cache.addIndex(new ConditionalExtractor(filter, EXTRACTOR_FIRSTNAME, true), true, null);
            cache.addIndex(new ConditionalExtractor(filter, EXTRACTOR_LASTNAME, true), true, null);
            cache.addIndex(new ConditionalExtractor(filter, new ReflectionExtractor("getChildrenIds"), true), true, null);
            cache.addIndex(new ConditionalExtractor(filter, new ReflectionExtractor("getMotherId"), true), true, null);
            cache.addIndex(new ConditionalExtractor(filter, EXTRACTOR_KEY_FIRSTNAME, true), true, null);
            cache.addIndex(new ConditionalExtractor(filter, EXTRACTOR_KEY_LASTNAME, true), true, null);
            cache.addIndex(new ConditionalExtractor(filter, new ComparisonValueExtractor("getFirstName", "getLastName"), true), true, null);
            cache.addIndex(new ConditionalExtractor(filter, new MultiExtractor("getFirstName,getLastName"), true), true, null);

            doTest(cache, SMALL, filter);
            doTest(cache, LARGE, filter);

            out("Test with conditional index no forward index support: " + cache.getCacheName());
            cache.clear();
            removeIndexFrom(cache, EXTRACTOR_FIRSTNAME);
            removeIndexFrom(cache, EXTRACTOR_LASTNAME);
            removeIndexFrom(cache, new ReflectionExtractor("getChildrenIds"));
            removeIndexFrom(cache, new ReflectionExtractor("getMotherId"));
            removeIndexFrom(cache, EXTRACTOR_KEY_FIRSTNAME);
            removeIndexFrom(cache, EXTRACTOR_KEY_LASTNAME);
            removeIndexFrom(cache, new ComparisonValueExtractor("getFirstName", "getLastName"));
            removeIndexFrom(cache, new MultiExtractor("getFirstName,getLastName"));
            // use a filter for the conditional indexes that will result in partial indexes
            // add the conditional indexes with the above filter
            cache.addIndex(new ConditionalExtractor(filter, EXTRACTOR_FIRSTNAME, false), true, null);
            cache.addIndex(new ConditionalExtractor(filter, EXTRACTOR_LASTNAME, false), true, null);
            cache.addIndex(new ConditionalExtractor(filter, new ReflectionExtractor("getChildrenIds"), false), true, null);
            cache.addIndex(new ConditionalExtractor(filter, new ReflectionExtractor("getMotherId"), false), true, null);
            cache.addIndex(new ConditionalExtractor(filter, EXTRACTOR_KEY_FIRSTNAME, false), true, null);
            cache.addIndex(new ConditionalExtractor(filter, EXTRACTOR_KEY_LASTNAME, false), true, null);
            cache.addIndex(new ConditionalExtractor(filter, new ComparisonValueExtractor("getFirstName", "getLastName"), false), true, null);
            cache.addIndex(new ConditionalExtractor(filter, new MultiExtractor("getFirstName,getLastName"), false), true, null);

            doTest(cache, SMALL, filter);
            doTest(cache, LARGE, filter);
            }
        }

    /**
    * Perform the tests.
    */
    public void doTest(NamedCache cacheTest, int cItems)
        {
        NamedCache cacheControl = new WrapperNamedCache(
            new HashMap(), cacheTest.getCacheName());

        doTest(cacheTest, cItems, cacheControl, null, AFILTER, AAFILTER);

        if (cacheTest.getCacheService() instanceof DistributedCacheService)
            {
            testAssociationFilter(cacheTest, cacheControl);
            testPartitionedFilter(cacheTest, cacheControl);
            testMultiExtractor(cacheTest, cacheControl);
            }
        }

    /**
     * Perform the tests assuming ConditionalIndexes are in place.
     *
     * @param cacheTest      the cache being tested
     * @param cItems         the number of items to fill the cache with
     * @param filterControl  the filter used for the ConditionalIndex which
     *                       will be used to filter the control cache
     */
    public void doTest(NamedCache cacheTest, int cItems, Filter filterControl)
        {
        NamedCache cacheControl = new WrapperNamedCache(
            new HashMap(), cacheTest.getCacheName());

        doTest(cacheTest, cItems, cacheControl, filterControl,
                AFILTER_CONDITIONAL, AAFILTER_CONDITIONAL);
        }

    /**
    * Perform the tests.
    */
    protected void doTest(NamedCache cacheTest, int cItems,
                          NamedCache cacheControl, Filter filterControl,
                          Filter[] afilter, Filter[][] aafilter)
        {
        Comparator compId   = new SafeComparator(null);
        Comparator compName = new ChainedComparator(new Comparator[]
            {
            new ReflectionExtractor("getLastName"),
            new ReflectionExtractor("getFirstName"),
            new ReflectionExtractor("getId"),
            });

        // 1) an empty cache test
        cacheTest.clear();
        cacheControl.clear();

        for (int i = 0, c = 2* afilter.length; i < c; i++)
            {
            Filter filter = afilter[i >> 1];
            if ((i % 2) != 0)
                {
                filter = new LimitFilter(filter, 10);
                }

            assertEqualKeySet(keySet(cacheTest, filter), cacheControl.keySet());
            assertEqualEntrySet(entrySet(cacheTest, filter, null), cacheControl.entrySet());
            assertEqualEntrySet(entrySet(cacheTest, filter, compId), cacheControl.entrySet());
            assertEqualEntrySet(entrySet(cacheTest, filter, compName), cacheControl.entrySet());
            }

        // 2) fill the cache and just execute all the queries
        Person.fillRandom(cacheTest, cItems);
        if (filterControl == null)
            {
            cacheControl.putAll(cacheTest);
            }
        else
            {
            for (Iterator iterator = cacheTest.entrySet(filterControl).iterator();
                 iterator.hasNext();)
                {
                Map.Entry entry =  (Map.Entry)iterator.next();
                cacheControl.put(entry.getKey(), entry.getValue());
                }
            }

        // until COH-6496 is fixed, LimitFilter implementation can be "unstable"
        // during redistribution
        CacheService service = cacheTest.getCacheService();
        if (service instanceof DistributedCacheService)
            {
            DistributedCacheService serviceDist = (DistributedCacheService) service;
            if (serviceDist.getStorageEnabledMembers().size() > 1)
                {
                waitForBalanced(serviceDist);
                }
            }

        for (int i = 0, c = 2* afilter.length; i < c; i++)
            {
            Filter filter = afilter[i >> 1];
            if ((i % 2) != 0)
                {
                filter = new LimitFilter(filter, 10);
                }

            try
                {
                // using an empty while loop to go into an infinite loop
                // during debugging in case of a failure

                while (!assertEqualKeySet(
                    keySet(cacheTest, filter), keySet(cacheControl, filter)));
                while (!assertEqualEntrySet(
                    entrySet(cacheTest, filter, null), entrySet(cacheControl, filter, null)));
                while (!assertEqualEntrySet(
                    entrySet(cacheTest, filter, compId), entrySet(cacheControl, filter, compId)));
                while (!assertEqualEntrySet(
                    entrySet(cacheTest, filter, compName), entrySet(cacheControl, filter, compName)));
                }
            catch (Throwable e)
                {
                err("Filter test failed for " + filter +
                    "\nCache content:\n" + cacheTest.entrySet() +
                    "\nControl content:\n" + cacheControl.entrySet());

                // debugging support - check if the failure is "stable"
                err("Repeated query: " + entrySet(cacheTest, filter, null));

                throw ensureRuntimeException(e, "Filter " + filter);
                }
            }

        // 3) run complementary queries
        for (int i = 0, c = aafilter.length; i < c; i++)
            {
            Filter[] afilterCompl = aafilter[i];
            Filter filter1 = afilterCompl[0];
            Filter filter2 = afilterCompl[1];
            if (filter2 == null)
                {
                filter2 = new NotFilter(filter1);
                }

            try
                {
                assertComplementaryKeySet(cacheTest.keySet(filter1), cacheTest.keySet(filter2), cacheControl);
                assertComplementaryEntrySet(cacheTest.entrySet(filter1), cacheTest.entrySet(filter2), cacheControl);
                assertComplementaryEntrySet(cacheTest.entrySet(filter1, null), cacheTest.entrySet(filter2, null), cacheControl);
                }
            catch (RuntimeException e)
                {
                CacheFactory.log("Failed entry set" + cacheTest.entrySet().toString(), 1);
                throw ensureRuntimeException(e, "Filter1 " + filter1 + "; filter2=" + filter2);
                }
            }

        // 4a) run equivalent queries
        //    (a && b) || (a && c) == a && (b || c)
        for (int i = 0; i < 64; i++)
            {
            Filter filterA = afilter[RND.nextInt(afilter.length)];
            Filter filterB = afilter[RND.nextInt(afilter.length)];
            Filter filterC = afilter[RND.nextInt(afilter.length)];

            Filter filter1 = new OrFilter(
                new AndFilter(filterA, filterB),
                new AndFilter(filterA, filterC));

            Filter filter2 = new AndFilter(
                filterA, new OrFilter(filterB, filterC));

            try
                {
                assertEqualKeySet(cacheTest.keySet(filter1), cacheTest.keySet(filter2));
                assertEqualEntrySet(cacheTest.entrySet(filter1), cacheTest.entrySet(filter2));
                assertEqualEntrySet(cacheTest.entrySet(filter1, null), cacheTest.entrySet(filter2, null));
                }
            catch (RuntimeException e)
                {
                throw ensureRuntimeException(e,
                    "Filter1 " + filter1 + "; Filter2 " + filter2);
                }
            }

        // 4b) run equivalent queries
        //    (a || b) && (a || c) == a || (b && c)
        for (int i = 0; i < 64; i++)
            {
            Filter filterA = afilter[RND.nextInt(afilter.length)];
            Filter filterB = afilter[RND.nextInt(afilter.length)];
            Filter filterC = afilter[RND.nextInt(afilter.length)];

            Filter filter1 = new AndFilter(
                new OrFilter(filterA, filterB),
                new OrFilter(filterA, filterC));

            Filter filter2 = new OrFilter(
                filterA, new AndFilter(filterB, filterC));

            try
                {
                assertEqualKeySet(cacheTest.keySet(filter1), cacheTest.keySet(filter2));
                assertEqualEntrySet(cacheTest.entrySet(filter1), cacheTest.entrySet(filter2));
                assertEqualEntrySet(cacheTest.entrySet(filter1, null), cacheTest.entrySet(filter2, null));
                }
            catch (RuntimeException e)
                {
                throw ensureRuntimeException(e,
                    "Filter1 " + filter1 + "; Filter2 " + filter2);
                }
            }

        testLikeFilter(cacheTest, cacheControl);
        }

    /**
    * Dedicated LikeFilter test.
    */
    public void testLikeFilter(NamedCache cacheTest, NamedCache cacheControl)
        {
        Filter filter;
        Set    setKeys;

        assertEqualKeySet(
            cacheTest.keySet(new LikeFilter("getLastName", "Bates")),
            cacheTest.keySet(new EqualsFilter("getLastName", "Bates")));

        filter  = new LikeFilter("getLastName", "Ba_es");
        setKeys = cacheTest.keySet(filter);
        for (Iterator iter = cacheControl.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();

            String  sValue = ((Person) entry.getValue()).getLastName();
            boolean fMatch = sValue.length() == 5 &&
                sValue.substring(0, 2).equals("Ba") &&
                sValue.substring(3, 5).equals("es");
            if (fMatch != setKeys.contains(entry.getKey()))
                {
                fail("Evaluation for " + entry.getValue()
                     + "by " + filter + " returned " + fMatch);
                }
            }

        filter = new LikeFilter("getLastName", "Dav%");
        setKeys = cacheTest.keySet(filter);
        for (Iterator iter = cacheControl.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();

            String  sValue = ((Person) entry.getValue()).getLastName();
            boolean fMatch = sValue.startsWith("Dav");
            if (fMatch != setKeys.contains(entry.getKey()))
                {
                fail("Evaluation for " + entry.getValue()
                    + "by " + filter + " returned " + fMatch);
                }
            }

        filter = new LikeFilter("getLastName", "_av%");
        setKeys = cacheTest.keySet(filter);
        for (Iterator iter = cacheControl.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();

            String  sValue = ((Person) entry.getValue()).getLastName();
            boolean fMatch = sValue.substring(1).startsWith("av");
            if (fMatch != setKeys.contains(entry.getKey()))
                {
                fail("Evaluation for " + entry.getValue()
                    + "by " + filter + " returned " + fMatch);
                }
            }

        filter = new LikeFilter("getFirstName", "Underscore\\_Test", '\\', false);
        setKeys = cacheTest.keySet(filter);
        for (Iterator iter = cacheControl.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();

            String  sValue = ((Person) entry.getValue()).getFirstName();
            boolean fMatch = sValue.equals("Underscore_Test");
            if (fMatch != setKeys.contains(entry.getKey()))
                {
                fail("Evaluation for " + entry.getValue()
                    + "by " + filter + " returned " + fMatch);
                }
            }

        filter = new LikeFilter("getFirstName", "Underscore\\_T_st", '\\', false);
        setKeys = cacheTest.keySet(filter);
        for (Iterator iter = cacheControl.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();

            String  sValue = ((Person) entry.getValue()).getFirstName();
            boolean fMatch = sValue.equals("Underscore_Test");
            if (fMatch != setKeys.contains(entry.getKey()))
                {
                fail("Evaluation for " + entry.getValue()
                    + "by " + filter + " returned " + fMatch);
                }
            }

        filter = new LikeFilter("getFirstName", "Und_rscore\\_T_st", '\\', false);
        setKeys = cacheTest.keySet(filter);
        for (Iterator iter = cacheControl.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();

            String  sValue = ((Person) entry.getValue()).getFirstName();
            boolean fMatch = sValue.equals("Underscore_Test");
            if (fMatch != setKeys.contains(entry.getKey()))
                {
                fail("Evaluation for " + entry.getValue()
                    + "by " + filter + " returned " + fMatch);
                }
            }

        filter = new LikeFilter("getLastName", "Wildcard\\%Test", '\\', false);
        setKeys = cacheTest.keySet(filter);
        for (Iterator iter = cacheControl.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();

            String  sValue = ((Person) entry.getValue()).getLastName();
            boolean fMatch = sValue.equals("Wildcard%Test");
            if (fMatch != setKeys.contains(entry.getKey()))
                {
                fail("Evaluation for " + entry.getValue()
                    + "by " + filter + " returned " + fMatch);
                }
            }

        // Test some pathological cases
        filter = new LikeFilter(new IdentityExtractor(), null, '\\', true);
        assertFalse(filter.evaluate(null));
        assertFalse(filter.evaluate(""));
        assertFalse(filter.evaluate("foo"));

        filter = new LikeFilter(new IdentityExtractor(), "", '\\', true);
        assertFalse(filter.evaluate(null));
        assertTrue(filter.evaluate(""));
        assertFalse(filter.evaluate("foo"));
        }

    /**
    * Dedicated KeyAssociatedFilter test.
    */
    public void testAssociationFilter(NamedCache cacheTest, NamedCache cacheControl)
        {
        DistributedCacheService service =
            (DistributedCacheService) cacheTest.getCacheService();
        KeyPartitioningStrategy partitioning =
            service.getKeyPartitioningStrategy();
        int cServers = getCacheServerCount();

        int cTests = 4*cServers;
        for (Iterator iter = cacheControl.keySet().iterator();
                iter.hasNext() && --cTests >= 0;)
            {
            Object oAnchor = iter.next();
            int    nAnchor = partitioning.getKeyPartition(oAnchor);

            Map mapControl = new HashMap();
            for (Iterator iterControl = cacheControl.entrySet().iterator(); iterControl.hasNext();)
                {
                Map.Entry entry = (Map.Entry) iterControl.next();
                if (partitioning.getKeyPartition(entry.getKey()) == nAnchor)
                    {
                    mapControl.put(entry.getKey(), entry.getValue());
                    }
                }
            Filter filter = new KeyAssociatedFilter(AlwaysFilter.INSTANCE, oAnchor);
            Set setKeysTest = cacheTest.keySet(filter);
            Set setEntriesTest = cacheTest.entrySet(filter);

            assertEqualKeySet(setKeysTest, mapControl.keySet());
            assertEqualEntrySet(setEntriesTest, mapControl.entrySet());
            }
        }

    /**
    * Dedicated PartitionedFilter test.
    */
    public void testPartitionedFilter(NamedCache cacheTest, NamedCache cacheControl)
        {
        DistributedCacheService service =
            (DistributedCacheService) cacheTest.getCacheService();
        int cPartitions = service.getPartitionCount();

        // test1: run the query for a single partition at a time
        Set setKeys    = new HashSet();
        Set setEntries = new HashSet();
        int cSize      = 0;

        PartitionSet parts = new PartitionSet(cPartitions);
        for (int iPartition = 0; iPartition < cPartitions; iPartition++)
            {
            parts.add(iPartition);

            Filter filter = new PartitionedFilter(AlwaysFilter.INSTANCE, parts);

            cSize += executeFilter(cacheTest, filter, setKeys, setEntries);
            parts.remove(iPartition);
            }

        assertEquals(cSize + "!=" + cacheControl.size(), cacheControl.size(), cSize);
        assertEqualKeySet(setKeys, cacheControl.keySet());
        assertEqualEntrySet(setEntries, cacheControl.entrySet());

        // test2: run the query "member-full" partitions at a time
        if (getCacheServerCount() > 1)
            {
            setKeys.clear();
            setEntries.clear();
            cSize = 0;

            parts.clear(); // collect all processed partitions

            for (Iterator iter = service.getStorageEnabledMembers().iterator();
                    iter.hasNext();)
                {
                Member member = (Member) iter.next();

                PartitionSet partsMember = service.getOwnedPartitions(member);

                // due to a redistribution some partitions may have already been processed
                partsMember.remove(parts);

                Filter filter = new PartitionedFilter(AlwaysFilter.INSTANCE, partsMember);

                cSize += executeFilter(cacheTest, filter, setKeys, setEntries);
                parts.add(partsMember);
                }

            // due to a redistribution some partitions may have been skipped
            if (!parts.isFull())
                {
                parts.invert();

                Filter filter = new PartitionedFilter(AlwaysFilter.INSTANCE, parts);

                cSize += executeFilter(cacheTest, filter, setKeys, setEntries);
                }

            assertEquals(cSize + "!=" + cacheControl.size(), cacheControl.size(), cSize);
            assertEqualKeySet(setKeys, cacheControl.keySet());
            assertEqualEntrySet(setEntries, cacheControl.entrySet());
            }

        // test3: run the PartitionedIterator
        int[] anOptions = new int[]
            {
            PartitionedIterator.OPT_BY_MEMBER,
            PartitionedIterator.OPT_BY_MEMBER | PartitionedIterator.OPT_RANDOMIZED,
            PartitionedIterator.OPT_BY_PARTITION,
            PartitionedIterator.OPT_BY_PARTITION | PartitionedIterator.OPT_RANDOMIZED,
            };

        parts = new PartitionSet(cPartitions);
        for (int i = 0; i < anOptions.length; i++)
            {
            int nOptions = anOptions[i];
            parts.fill();
            setKeys.clear();
            cSize = 0;

            Iterator iter = new PartitionedIterator(cacheTest,
                AlwaysFilter.INSTANCE, parts, nOptions);

            while (iter.hasNext())
                {
                setKeys.add(iter.next());
                cSize++;
                }

            assertEquals(cSize + "!=" + cacheControl.size(), cacheControl.size(), cSize);
            assertEqualKeySet(setKeys, cacheControl.keySet());
            }
        }

    /**
    * Dedicated MultiExtractor test.
    */
    public void testMultiExtractor(NamedCache cacheTest, NamedCache cacheControl)
        {
        Person p = (Person) cacheTest.values().iterator().next();
        Object oPair = new ImmutableArrayList(
            new String[] {p.getFirstName(), p.getLastName()});
        ValueExtractor ve = new MultiExtractor("getFirstName,getLastName");

        Filter filter = new EqualsFilter(ve, oPair);

        Set setKeysTest = cacheTest.keySet(filter);
        Set setEntriesTest = cacheTest.entrySet(filter);

        assertTrue("empty result", setKeysTest.size() >= 1);
        assertEqualKeySet(setKeysTest, cacheControl.keySet(filter));
        assertEqualEntrySet(setEntriesTest, cacheControl.entrySet(filter));
        }

    /**
    * Helper: execute an individual filter request.
    */
    private int executeFilter(NamedCache cacheTest, Filter filter, Set setKeys, Set setEntries)
        {
        Set setTest;
        int cKeys, cEntries;

        setTest = cacheTest.keySet(filter);
        cKeys   = setTest.size();
        setKeys.addAll(setTest);

        setTest  = cacheTest.entrySet(filter);
        cEntries = setTest.size();
        setEntries.addAll(setTest);

        assertEquals(cKeys + "!=" + cEntries, cEntries, cKeys);
        return cEntries;
        }

    /**
    * Get a key set using the specified filter.
    */
    public Set keySet(NamedCache cache, Filter filter)
        {
        Set setResult = cache.keySet(filter);
        if (filter instanceof LimitFilter)
            {
            LimitFilter filterLimit = (LimitFilter) filter;
            int         cPageSize   = filterLimit.getPageSize();
            if (setResult.size() == cPageSize)
                {
                // clone the sets since ConverterCollections are broken
                // in terms of the hashCode, equals and contains operations
                setResult = new HashSet(setResult);

                // get all the pages before proceeding
                while (true)
                    {
                    filterLimit.nextPage();
                    Set setNext = cache.keySet(filterLimit);
                    setResult.addAll(setNext);
                    if (setNext.size() < cPageSize)
                        {
                        break;
                        }
                    }
                }
            filterLimit.setPage(0);
            }
        return setResult;
        }

    /**
    * Get an entry set using the specified filter.
    */
    public Set entrySet(NamedCache cache, Filter filter, Comparator comp)
        {
        Set setResult = comp == null ? cache.entrySet(filter)
                                     : cache.entrySet(filter, comp);
        if (filter instanceof LimitFilter)
            {
            LimitFilter filterLimit = (LimitFilter) filter;
            int         cPageSize   = filterLimit.getPageSize();
            if (setResult.size() == cPageSize)
                {
                // clone the sets since ConverterCollections are broken
                // in terms of the hashCode, equals and contains operations
                setResult = new HashSet(setResult);

                // get all the pages before proceeding
                while (true)
                    {
                    filterLimit.nextPage();
                    Set setNext = comp == null ? cache.entrySet(filterLimit)
                                               : cache.entrySet(filterLimit, comp);
                    setResult.addAll(setNext);
                    if (setNext.size() < cPageSize)
                        {
                        break;
                        }
                    }
                }
            filterLimit.setPage(0);
            }
        return setResult;
        }

    // ----- helper methods -------------------------------------------------

    public void removeIndexFrom(NamedCache namedCache, ValueExtractor extractor)
        {
        namedCache.removeIndex(extractor);
        }

    // ----- constants ------------------------------------------------------

    static final Random RND = new Random();


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


    // ----- constants ------------------------------------------------------

    // extractors
    private static final ValueExtractor EXTRACTOR_FIRSTNAME     = new ReflectionExtractor("getFirstName");
    private static final ValueExtractor EXTRACTOR_LASTNAME      = new ReflectionExtractor("getLastName");
    private static final ValueExtractor EXTRACTOR_KEY_FIRSTNAME = new KeyExtractor("getFirstName");
    private static final ValueExtractor EXTRACTOR_KEY_LASTNAME  = new KeyExtractor("getLastName");

    // filters to be tested
    private static final Filter[] AFILTER = new Filter[]
        {
        // primitive filters
        AlwaysFilter.INSTANCE,
        new BetweenFilter("getLastName", "Aaaa", "Zzzz"),
        new ContainsAllFilter("getChildrenIds", new ImmutableArrayList(new String[]{"1","21","32"})),
        new ContainsAnyFilter("getChildrenIds", new ImmutableArrayList(new String[]{"1","21","32"})),
        new ContainsFilter("getChildrenIds", "21"),
        new EqualsFilter("getLastName", "Bates"),
        new GreaterEqualsFilter("getLastName", "Bates"),
        new GreaterFilter("getLastName", "Bates"),
        new InFilter("getLastName", new ImmutableArrayList(new String[]{"Bates","Calverson","Davies"})),
        new IsNotNullFilter("getMotherId"),
        new IsNullFilter("getMotherId"),
        new LessEqualsFilter("getLastName", "Bates"),
        new LessFilter("getLastName", "Bates"),
        new LikeFilter("getLastName", "Bates"),
        new LikeFilter("getLastName", "Ba_es"),
        new LikeFilter("getLastName", "Davies%"),
        new LikeFilter("getLastName", "Da%"),
        NeverFilter.INSTANCE,
        new NotEqualsFilter("getLastName", "Bates"),

        // composite filters
        new AllFilter(new Filter[] {
            new GreaterEqualsFilter("getLastName", "Bates"),
            new LessEqualsFilter("getFirstName", "Jon"),
            }),
        new AllFilter(new Filter[] {
            new LessFilter("getLastName", "Bates"),
            new GreaterFilter("getFirstName", "Tom"),
            }),
        new AnyFilter(new Filter[] {
            new GreaterEqualsFilter("getLastName", "Bates"),
            new LessEqualsFilter("getFirstName", "Jon"),
            }),
        new NotFilter(AlwaysFilter.INSTANCE),
        new XorFilter(AlwaysFilter.INSTANCE, NeverFilter.INSTANCE),

        // ComparisonValueExtractor based filters
        new LessFilter(
            new ComparisonValueExtractor("getFirstName", "getLastName"), Integer.valueOf(0)),

        // key filters
        new BetweenFilter(EXTRACTOR_KEY_LASTNAME, "Aaaa", "Zzzz"),
        new BetweenFilter(EXTRACTOR_KEY_LASTNAME, "Bates", "Zzzz"),
        new BetweenFilter(EXTRACTOR_KEY_LASTNAME, "Aaaa", "Bates"),
        new BetweenFilter(EXTRACTOR_KEY_LASTNAME, "Bates", "Davies"),
        new EqualsFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
        new GreaterEqualsFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
        new GreaterFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
        new InFilter(EXTRACTOR_KEY_LASTNAME, new ImmutableArrayList(new String[]{"Bates","Calverson","Davies"})),
        new NotEqualsFilter("getLastName", "Bates"),

        // composite key filters
        new AllFilter(new Filter[] {
            new GreaterEqualsFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
            new LessEqualsFilter(EXTRACTOR_KEY_FIRSTNAME, "Jon"),
            }),
        new AllFilter(new Filter[] {
            new LessFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
            new GreaterFilter(EXTRACTOR_KEY_FIRSTNAME, "Tom"),
            }),
        new AnyFilter(new Filter[] {
            new GreaterEqualsFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
            new LessEqualsFilter(EXTRACTOR_KEY_FIRSTNAME, "Jon"),
            }),

        // mixed key-value filters
        new AllFilter(new Filter[] {
            new GreaterEqualsFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
            new LessEqualsFilter("getFirstName", "Jon"),
            }),
        new AllFilter(new Filter[] {
            new LessFilter("getLastName", "Bates"),
            new GreaterFilter(EXTRACTOR_KEY_FIRSTNAME, "Tom"),
            }),
        new AnyFilter(new Filter[] {
            new GreaterEqualsFilter("getLastName", "Bates"),
            new LessEqualsFilter(EXTRACTOR_KEY_FIRSTNAME, "Jon"),
            }),
        };

    // filters to be tested with ConditionalIndexes
    private static final Filter[] AFILTER_CONDITIONAL = new Filter[]
        {
        // primitive filters
        new ContainsAllFilter("getChildrenIds", new ImmutableArrayList(new String[]{"1","21","32"})),
        new ContainsAnyFilter("getChildrenIds", new ImmutableArrayList(new String[]{"1","21","32"})),
        new ContainsFilter("getChildrenIds", "21"),
        new EqualsFilter("getLastName", "Bates"),
        new GreaterEqualsFilter("getLastName", "Bates"),
        new GreaterFilter("getLastName", "Bates"),
        new InFilter("getLastName", new ImmutableArrayList(new String[]{"Bates","Calverson","Davies"})),
        new LessEqualsFilter("getLastName", "Bates"),
        new LessFilter("getLastName", "Bates"),
        new LikeFilter("getLastName", "Bates"),
        new LikeFilter("getLastName", "Ba_es"),
        new LikeFilter("getLastName", "Davies%"),
        new LikeFilter("getLastName", "Da%"),
        new NotEqualsFilter("getLastName", "Bates"),

        // composite filters
        new AllFilter(new Filter[] {
            new GreaterEqualsFilter("getLastName", "Bates"),
            new LessEqualsFilter("getFirstName", "Jon"),
            }),
        new AllFilter(new Filter[] {
            new LessFilter("getLastName", "Bates"),
            new GreaterFilter("getFirstName", "Tom"),
            }),
        new AnyFilter(new Filter[] {
            new GreaterEqualsFilter("getLastName", "Bates"),
            new LessEqualsFilter("getFirstName", "Jon"),
            }),
        new NotFilter(AlwaysFilter.INSTANCE),

        // ComparisonValueExtractor based filters
        new LessFilter(
            new ComparisonValueExtractor("getFirstName", "getLastName"), Integer.valueOf(0)),

        // key filters
        new BetweenFilter(EXTRACTOR_KEY_LASTNAME, "Aaaa", "Zzzz"),
        new EqualsFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
        new GreaterEqualsFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
        new GreaterFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
        new InFilter(EXTRACTOR_KEY_LASTNAME, new ImmutableArrayList(new String[]{"Bates","Calverson","Davies"})),
        new NotEqualsFilter("getLastName", "Bates"),

        // composite key filters
        new AllFilter(new Filter[] {
            new GreaterEqualsFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
            new LessEqualsFilter(EXTRACTOR_KEY_FIRSTNAME, "Jon"),
            }),
        new AllFilter(new Filter[] {
            new LessFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
            new GreaterFilter(EXTRACTOR_KEY_FIRSTNAME, "Tom"),
            }),
        new AnyFilter(new Filter[] {
            new GreaterEqualsFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
            new LessEqualsFilter(EXTRACTOR_KEY_FIRSTNAME, "Jon"),
            }),

        // mixed key-value filters
        new AllFilter(new Filter[] {
            new GreaterEqualsFilter(EXTRACTOR_KEY_LASTNAME, "Bates"),
            new LessEqualsFilter("getFirstName", "Jon"),
            }),
        new AllFilter(new Filter[] {
            new LessFilter("getLastName", "Bates"),
            new GreaterFilter(EXTRACTOR_KEY_FIRSTNAME, "Tom"),
            }),
        new AnyFilter(new Filter[] {
            new GreaterEqualsFilter("getLastName", "Bates"),
            new LessEqualsFilter(EXTRACTOR_KEY_FIRSTNAME, "Jon"),
            }),
        };

    // filter pairs for testing complementary queries
    private static final Filter[][] AAFILTER = new Filter[][] {
        {AlwaysFilter.INSTANCE, NeverFilter.INSTANCE},
        {new BetweenFilter(EXTRACTOR_LASTNAME, "Aaaa", "Lzzz"), new BetweenFilter(EXTRACTOR_LASTNAME, "Maaa", "Zzzz")},
        {new BetweenFilter(EXTRACTOR_LASTNAME, "Aaaa", "Lzzz"), new BetweenFilter(EXTRACTOR_KEY_LASTNAME, "Maaa", "Zzzz")},
        {new ContainsAllFilter("getChildrenIds", new ImmutableArrayList(new String[]{"1","18","32"})), null},
        {new ContainsAnyFilter("getChildrenIds", new ImmutableArrayList(new String[]{"1","18","32"})), null},
        {new ContainsFilter("getChildrenIds", "34"), null},
        {new EqualsFilter(EXTRACTOR_LASTNAME, "Bates"), new NotEqualsFilter(EXTRACTOR_LASTNAME, "Bates")},
        {new EqualsFilter(EXTRACTOR_LASTNAME, "Bates"), new NotEqualsFilter(EXTRACTOR_KEY_LASTNAME, "Bates")},
        {new GreaterEqualsFilter(EXTRACTOR_LASTNAME, "Bates"), new LessFilter(EXTRACTOR_LASTNAME, "Bates")},
        {new GreaterEqualsFilter(EXTRACTOR_LASTNAME, "Bates"), new LessFilter(EXTRACTOR_KEY_LASTNAME, "Bates")},
        {new GreaterFilter(EXTRACTOR_LASTNAME, "Bates"), new LessEqualsFilter(EXTRACTOR_LASTNAME, "Bates")},
        {new InFilter(EXTRACTOR_LASTNAME, new ImmutableArrayList(new String[]{"Bates","Calverson","Davies"})), null},
        {new IsNotNullFilter("getMotherId"), new IsNullFilter("getMotherId")},
        {new AndFilter(new GreaterEqualsFilter(EXTRACTOR_LASTNAME, "Bates"), new LessFilter(EXTRACTOR_FIRSTNAME, "Jon")),
            new OrFilter(new LessFilter(EXTRACTOR_LASTNAME, "Bates"), new GreaterEqualsFilter(EXTRACTOR_FIRSTNAME, "Jon"))},
        {new AndFilter(new GreaterEqualsFilter(EXTRACTOR_LASTNAME, "Bates"), new LessFilter(EXTRACTOR_KEY_FIRSTNAME, "Jon")),
            new OrFilter(new LessFilter(EXTRACTOR_LASTNAME, "Bates"), new GreaterEqualsFilter(EXTRACTOR_KEY_FIRSTNAME, "Jon"))},
        {new XorFilter(new GreaterFilter(EXTRACTOR_FIRSTNAME, "Lilly"), new LessEqualsFilter(EXTRACTOR_FIRSTNAME, "Lilly")),
            NeverFilter.INSTANCE},
        {new LessEqualsFilter(new ComparisonValueExtractor(EXTRACTOR_FIRSTNAME, EXTRACTOR_LASTNAME), Integer.valueOf(0)),
            new GreaterFilter(new ComparisonValueExtractor(EXTRACTOR_FIRSTNAME, EXTRACTOR_LASTNAME), Integer.valueOf(0))},
        };

    // filter pairs for testing complementary queries with ConditionalIndexes
    private static final Filter[][] AAFILTER_CONDITIONAL = new Filter[][] {
        {new BetweenFilter(EXTRACTOR_LASTNAME, "Aaaa", "Lzzz"), new BetweenFilter(EXTRACTOR_LASTNAME, "Maaa", "Zzzz")},
        {new BetweenFilter(EXTRACTOR_LASTNAME, "Aaaa", "Lzzz"), new BetweenFilter(EXTRACTOR_KEY_LASTNAME, "Maaa", "Zzzz")},
        {new EqualsFilter(EXTRACTOR_LASTNAME, "Bates"), new NotEqualsFilter(EXTRACTOR_LASTNAME, "Bates")},
        {new EqualsFilter(EXTRACTOR_LASTNAME, "Bates"), new NotEqualsFilter(EXTRACTOR_KEY_LASTNAME, "Bates")},
        {new GreaterEqualsFilter(EXTRACTOR_LASTNAME, "Bates"), new LessFilter(EXTRACTOR_LASTNAME, "Bates")},
        {new GreaterEqualsFilter(EXTRACTOR_LASTNAME, "Bates"), new LessFilter(EXTRACTOR_KEY_LASTNAME, "Bates")},
        {new GreaterFilter(EXTRACTOR_LASTNAME, "Bates"), new LessEqualsFilter(EXTRACTOR_LASTNAME, "Bates")},
        {new IsNotNullFilter("getMotherId"), new IsNullFilter("getMotherId")},
        {new AndFilter(new GreaterEqualsFilter(EXTRACTOR_LASTNAME, "Bates"), new LessFilter(EXTRACTOR_FIRSTNAME, "Jon")),
            new OrFilter(new LessFilter(EXTRACTOR_LASTNAME, "Bates"), new GreaterEqualsFilter(EXTRACTOR_FIRSTNAME, "Jon"))},
        {new AndFilter(new GreaterEqualsFilter(EXTRACTOR_LASTNAME, "Bates"), new LessFilter(EXTRACTOR_KEY_FIRSTNAME, "Jon")),
            new OrFilter(new LessFilter(EXTRACTOR_LASTNAME, "Bates"), new GreaterEqualsFilter(EXTRACTOR_KEY_FIRSTNAME, "Jon"))},
        {new LessEqualsFilter(new ComparisonValueExtractor(EXTRACTOR_FIRSTNAME, EXTRACTOR_LASTNAME), Integer.valueOf(0)),
            new GreaterFilter(new ComparisonValueExtractor(EXTRACTOR_FIRSTNAME, EXTRACTOR_LASTNAME), Integer.valueOf(0))},
        };
    }
