/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package filter;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.ClassFilter;
import com.tangosol.util.Filter;

import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.ConditionalExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.ContainsAllFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.ExtractorFilter;
import com.tangosol.util.filter.GreaterEqualsFilter;
import com.tangosol.util.filter.GreaterFilter;

import com.tangosol.util.filter.LessEqualsFilter;
import com.tangosol.util.filter.NeverFilter;
import com.tangosol.util.filter.NotEqualsFilter;
import com.tangosol.util.processor.AbstractProcessor;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.Person;

import java.io.Serializable;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 * Index-related tests
 *
 * @author coh 2011.03.07
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@RunWith(Parameterized.class)
public class IndexTests
        extends AbstractFunctionalTest
    {
    @Parameterized.Parameters(name = "sCacheName={0}")
    public static Collection<String[]> parameters()
        {
        return Arrays.asList(new String[][]
                {
                {"dist-test"},
                {"part-test"},
                });
        }

    /**
     * Test's constructor.
     *
     * @param sCacheName the cache name
     */
    public IndexTests(String sCacheName)
        {
        m_sCacheName = sCacheName;
        }
    private final String m_sCacheName;

    /**
     * Return the name of the cache.
     *
     * @return the name of the cache
     */
    private String getCacheName()
        {
        return m_sCacheName;
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.distributed.threads", "4");

        s_cIterations = Integer.getInteger("test.iterations", 1);
        s_cThreads = Integer.getInteger("test.threads", 4);

        AbstractFunctionalTest._startup();
        }

    @Before
    public void _reset()
        {
        getNamedCache().removeIndex(IdentityExtractor.INSTANCE);
        }

    // ----- test methods ---------------------------------------------------

    /**
     * COH-10259: conditional index does not update correctly when the original
     *            value was filtered out.
     */
    @Test
    public void testCOH10259()
        {
        NamedCache cache = getNamedCache();
        try
            {
            cache.clear();

            cache.addIndex(new ConditionalExtractor(new EqualsFilter("getLastName", "Green"),
                new ReflectionExtractor("getFirstName"), true), false, null);

            Person person = new Person("1111");
            person.setFirstName("Mike");
            person.setLastName("Walker");

            cache.put("customer", person);
            assertEquals(0, cache.entrySet(new EqualsFilter("getFirstName", "Mike")).size());

            Person personNew = new Person("1112");
            personNew.setFirstName("Mike");
            personNew.setLastName("Green");

            cache.put("customer", personNew);
            assertEquals(1, cache.entrySet(new EqualsFilter("getFirstName", "Mike")).size());
            }
        finally
            {
            cache.destroy();
            }
        }

    @Test
    public void testConcurrentProcessors()
        {
        final NamedCache cache = getNamedCache();

        cache.addIndex(IdentityExtractor.INSTANCE, true, null);

        UpdateThread[] updateThreads = startUpdateThreads(cache, 0);

        for (int i = 0; i < s_cIterations; i++)
            {
            testProcessor(cache, new GreaterEqualsFilter(IdentityExtractor.INSTANCE, QUERY_VALUE));
            }

        stopUpdateThreads(updateThreads);
        }

    @Test
    public void testConditionalIndexOnKey()
        {
        final NamedCache<Long, String> cache = getNamedCache();

        cache.clear();
        
        ValueExtractor<Long, Integer> extractor = new LastDigit().fromKey();
        Filter<String> condition = new NotEqualsFilter<>(ValueExtractor.identity(), "");

        ConditionalExtractor condExtractor = new ConditionalExtractor<>(condition, extractor, false);
        try
            {
            cache.addIndex(condExtractor, false, null);

            Filter<?> query = new EqualsFilter<>(extractor, 3);

            cache.put(123L, ""); //fail condition
            assertTrue(cache.entrySet(query).isEmpty());

            cache.put(123L, "notEmpty"); //pass condition
            assertTrue(cache.entrySet(query).contains(new AbstractMap.SimpleEntry<>(123L, "notEmpty")));

            cache.put(123L, ""); //fail condition
            assertTrue(cache.entrySet(query).isEmpty());
            }
        finally
            {
            cache.removeIndex(condExtractor);
            }
        }

    @Test
    public void testConcurrentUpdates()
        {
        final NamedCache cache = getNamedCache();

        cache.addIndex(IdentityExtractor.INSTANCE, true, null);

        // Insert/Update
        UpdateThread[] updateThreads = startUpdateThreads(cache, 0);

        for (int i = 0; i < s_cIterations; i++)
            {
            testFilter(cache, new GreaterEqualsFilter(IdentityExtractor.INSTANCE, QUERY_VALUE));
            testFilter(cache, new EqualsFilter(IdentityExtractor.INSTANCE, QUERY_VALUE));
            testFilter(cache, new LessEqualsFilter(IdentityExtractor.INSTANCE, QUERY_VALUE));
            }

        stopUpdateThreads(updateThreads);
        }

    @Test
    public void testConcurrentUpdatesPartialIndex()
        {
        final NamedCache cache = getNamedCache();

        cache.addIndex(IdentityExtractor.INSTANCE, true, null);

        // Insert/Update
        UpdateThread[] updateThreads = startUpdateThreads(cache, 0);

        for (int i = 0; i < s_cIterations; i++)
            {
            testFilter(cache, new AndFilter(
                    new GreaterEqualsFilter(IdentityExtractor.INSTANCE, QUERY_VALUE),
                    new GreaterEqualsFilter("hashCode", QUERY_VALUE)));
            }

        stopUpdateThreads(updateThreads);
        }

    @Test
    public void testConcurrentUpdatesNoIndex()
        {
        final NamedCache cache = getNamedCache();

        UpdateThread[] updateThreads = startUpdateThreads(cache, 0);

        for (int i = 0; i < s_cIterations; i++)
            {
            testFilter(cache, new GreaterEqualsFilter(IdentityExtractor.INSTANCE, QUERY_VALUE));
            testFilter(cache, new EqualsFilter(IdentityExtractor.INSTANCE, QUERY_VALUE));
            testFilter(cache, new LessEqualsFilter(IdentityExtractor.INSTANCE, QUERY_VALUE));
            }

        stopUpdateThreads(updateThreads);
        }

    /**
     * Regression test for COH-5575
     */
    @Test
    public void testCoh5575()
        {
        final NamedCache    cache      = getNamedCache("Coh5575-test");
        final AtomicBoolean atomicFlag = new AtomicBoolean();
        final long          ldtStop    = System.currentTimeMillis() + 5000;
        final Throwable[]   aeHolder   = new Throwable[1];

        try
            {
            Runnable rTest = new Runnable()
                {
                public void run()
                    {
                    ValueExtractor extractor = IdentityExtractor.INSTANCE;
                    while (System.currentTimeMillis() < ldtStop &&
                           !atomicFlag.get())
                        {
                        try
                            {
                            cache.addIndex(extractor, true, null);
                            cache.removeIndex(extractor);
                            }
                        catch (Throwable e)
                            {
                            aeHolder[0] = e;
                            atomicFlag.set(true);
                            }
                        }
                    }
                };


            Thread[] aThread = new Thread[4];

            for (int i = 0, c = aThread.length; i < c; i++)
                {
                aThread[i] = new Thread(rTest);
                aThread[i].setDaemon(true);
                aThread[i].start();
                }

            for (int i = 0, c = aThread.length; i < c; i++)
                {
                aThread[i].join();
                }

            assertNull(String.valueOf(aeHolder[0]), aeHolder[0]);
            }
        catch (InterruptedException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        finally
            {
            cache.destroy();
            }
        }

    // COH-6447: when we encounter a corrupted entry, we no longer drop the index
    @Test
    public void testCorruptedIndex()
        {
        final NamedCache cache = getNamedCache("dist-CorruptIdx");
        int cCorrupted = 10;

        cache.addIndex(StringIntegerExtractor.INSTANCE, true, null);

        // start concurrent updates with Integer values
        UpdateThread[] updateThreads = startUpdateThreads(cache, 0);

        // add 2 * cCorrupted entries with String values, half of them are corrupted,
        // should be excluded from index
        populateWithCorrupted(cache, cCorrupted);

        // extractor picks only string values and index should discard malformed ones
        Filter filterStringGT = new GreaterFilter(StringIntegerExtractor.INSTANCE, -cCorrupted);

        Set<Map.Entry<Integer, Integer>> setResult = cache.entrySet(filterStringGT);

        stopUpdateThreads(updateThreads);

        int cStringGT = setResult.size();
        assertEquals("Wrong result size for StringGT filter", cCorrupted, cStringGT);

        // cache should contain: equal number of correct and "wrong-" strings
        // plus Integer values inserted by update threads
        Filter filterInteger = new ClassFilter(Integer.class);
        int    cInteger      = cache.entrySet(filterInteger).size();

        assertEquals("Wrong cache size ", cache.size(), 2 * cStringGT + cInteger);

        correctCorrupted(cache, cCorrupted);

        // there should be no excluded index now
        setResult = cache.entrySet(filterStringGT);
        assertEquals("Wrong result size for StringGE filter", 2 * cCorrupted, setResult.size());

        // test concurrent delete of corrupted entries
        populateWithCorrupted(cache, cCorrupted);

        DeleteThread d1 = new DeleteThread(cache, -19, 5);
        DeleteThread d2 = new DeleteThread(cache, -9, 5);
        d1.start();
        d2.start();
        try
            {
            d1.join();
            d2.join();
            }
        catch (InterruptedException e)
            {
            }

        assertEquals("Wrong cache size after delete corrupted ", cache.size(), cInteger + cStringGT);

        cache.removeIndex(StringIntegerExtractor.INSTANCE);

        //test an index based on a non-deterministic extractor
        ValueExtractor extractorND = new EveryOtherTimeExtractor();
        cache.addIndex(extractorND, false, null);

        cache.put("NonDet-1", "value-1-1"); //extractor works OK
        //extractor fails, value should be removed from index
        cache.put("NonDet-1", "value-1-2");
        Filter filterND = new EqualsFilter(new EveryOtherTimeExtractor(), "value-1");
        // thus, the value should not be part of query result based on that index
        assertTrue("Update with failed EveryOtherTimeExtractor should remove value from index",
                cache.entrySet(filterND).isEmpty());
        // extractor will work OK, value should be added to the index
        cache.put("NonDet-1", "value-1-3");
        Set<Map.Entry> setND = cache.entrySet(filterND);
        assertTrue("This time the value should be in index", setND.size() == 1);
        Map.Entry entry = setND.iterator().next();
        assertEquals("Incorrect value in the index", "value-1-3", entry.getValue());

        cache.remove("NonDet-1");
        assertTrue("After delete result of indexed query should be null",
                cache.entrySet(filterND).isEmpty());

        cache.removeIndex(extractorND);

        // test corrupted entries with index that has no forward map
        // use ConditionalIndex w/o forward map
        testConditionalIndex(cache, false);
        // use ConditionalIndex with forward map
        testConditionalIndex(cache, true);
        cache.destroy();
        }

    @Test
    public void testCorruptedCollections()
        {
        final NamedCache cache = getNamedCache("dist-CollectIdx");

        cache.addIndex(StringIntegerExtractor.INSTANCE, true, null);

        // test collection values
        // define and populate the Collection values
        Collection<String> collection0 = new LinkedList<>();
        Collection<String> collection1 = new LinkedList<>();
        Collection<String> collection2 = new LinkedList<>();
        Collection<String> collection3 = new LinkedList<>();
        Collection<String> collection4 = new LinkedList<>();

        collection0.add("8");

        collection1.add("1");
        collection1.add("2");
        collection1.add("3");

        collection2.add("2");
        collection2.add("3");
        collection2.add("4");

        collection3.add("5");
        collection3.add("6");
        collection3.add("7");
        collection3.add("8");
        collection3.add("9");

        // insert bad
        collection4.add("wrong-7");
        collection4.add("8");
        collection4.add("9");

        cache.put("c0", collection0);
        cache.put("c1", collection1);
        cache.put("c2", collection2);
        cache.put("c3", collection3);
        cache.put("c4", collection4);

        // even though c4 contains a bogus element, the index should remain
        HashSet<Integer> set23 = new HashSet<>();
        set23.add(2);
        set23.add(3);
        Filter filterContAll_23 = new ContainsAllFilter(StringIntegerExtractor.INSTANCE, set23);
        Set<String> setKeysAll_23 = cache.keySet(filterContAll_23);

        boolean fHasC1 = false, fHasC2 = false;
        for (Object oKey : setKeysAll_23)
            {
            if (oKey.equals("c1"))
                {
                fHasC1 = true;
                }
            else if (oKey.equals("c2"))
                {
                fHasC2 = true;
                }
            else
                {
                fail("Unexpected key " + oKey + "in ContainsAll{2,3} query result.");
                }
            }
        assertTrue("Both c1 and c2 keys were expected in query result.", fHasC1 && fHasC2);

        HashSet<Integer> set8 = new HashSet<>();
        set8.add(8);
        Filter filterContAll_8 = new ContainsAllFilter(StringIntegerExtractor.INSTANCE, set8);
        Set<String> setKeyAll_8 = cache.keySet(filterContAll_8);
        assertEquals("Key c4 should not be in query result.", 2, setKeyAll_8.size());

        // insert good
        collection4.remove("wrong-7");
        collection4.add("7");
        cache.put("c4", collection4);

        setKeyAll_8 = cache.keySet(filterContAll_8);
        assertEquals("Incorrect size of ContainsAll {8} query result", 3, setKeyAll_8.size());

        // insert bad
        collection4.remove("9");
        collection4.add("wrong-9");
        cache.put("c4", collection4);
        setKeyAll_8 = cache.keySet(filterContAll_8);
        assertEquals("Key c4 should not be in query result with wrong-9", 2, setKeyAll_8.size());


        // delete
        cache.remove("c4");
        setKeysAll_23 = cache.keySet(filterContAll_23);
        assertEquals("Unexpected result size after deleting c4", 2, setKeysAll_23.size());

        setKeyAll_8 = cache.keySet(filterContAll_8);
        assertEquals("Key c4 was removed and should not be in query result", 2, setKeyAll_8.size());

        // now test updates without forward map
        testCollectionsCondIndex(cache, false);
        }

    /**
     * Regression test for COH-100600.
     */
    @Test
    public void testCoh10600()
        {
        NamedCache cache = getNamedCache();

        cache.put("key", "value");
        cache.addIndex(IdentityExtractor.INSTANCE, false, null);
        cache.entrySet(new BadFilter());
        cache.remove("key");

         // this will cause the index update fail and remove the index
        BadFilter.s_fFail = true;
        cache.put("key", new BadFilter());
        BadFilter.s_fFail = false;

        cache.addIndex(IdentityExtractor.INSTANCE, false, null);
        cache.put("key", "value");

        // before COH-100600 fix, the following line would throw
        cache.entrySet(new BadFilter());
        }


    // ----- internal methods ------------------------------------------------

    /**
     * Test Conditional index in combination with failing value extractor.
     */
    private void testConditionalIndex(NamedCache cache, boolean fFwdIdx)
        {
        ValueExtractor extractorCond = new ConditionalExtractor(
                new EvenIntFilter(StringIntegerExtractor.INSTANCE),
                StringIntegerExtractor.INSTANCE, fFwdIdx);

        // use the same boolean to vary the ordered argument
        cache.addIndex(extractorCond, fFwdIdx, null);

        Set<Map.Entry> setCache = cache.entrySet();

        // count the number of even integers by brute force
        int cEvenInt = 0;
        int cEvenStr = 0;
        for (Map.Entry result : setCache)
            {
            Object oValue = result.getValue();
            if (oValue instanceof Integer)
                {
                if (((Integer) oValue) % 2 == 0)
                    {
                    cEvenInt++;
                    }
                }
            else if(oValue instanceof String)
                {
                if (Integer.parseInt((String) oValue) % 2 == 0)
                    {
                    cEvenStr++;
                    }
                }
            }
        cache.put(-6, "wrong--6");

        Filter filterAll = new ExtractorFilter(StringIntegerExtractor.INSTANCE)
            {
            protected boolean evaluateExtracted(Object oExtracted)
                {
                return true;
                }
            };

        // only entries in conditional index should be returned, i.e. only 'even' strings
        assertEquals("Query result based on conditional index is wrong (forwardIndex=" +
                fFwdIdx + ")", cEvenStr, cache.entrySet(filterAll).size());

        cache.put(-6, "-6");
        assertEquals("Query result size should have increased by 1 (forwardIndex=" +
                fFwdIdx + ")", cEvenStr+1, cache.entrySet(filterAll).size());

        cache.put(-6, "wrongagain--6");
        assertEquals("Query result size should have gone back (forwardIndex=" +
                fFwdIdx + ")", cEvenStr, cache.entrySet(filterAll).size());

        cache.remove(-6);
        assertEquals("Query result size should remain the same (forwardIndex=" +
                fFwdIdx + ")", cEvenStr, cache.entrySet(filterAll).size());

        cache.removeIndex(extractorCond);

        }

    /**
     * Test a failing value extractor on collections with conditional index
     * and no forward map.
     */
    private  void testCollectionsCondIndex(NamedCache cache, boolean fFwdIdx)
        {
        ValueExtractor extractorCond = new ConditionalExtractor(new AlwaysFilter(), new EveryOtherTimeExtractor(), fFwdIdx);

        // test update with intersecting old and new values and non-deterministic extractor
        cache.addIndex(extractorCond, fFwdIdx, null);

        Collection<String> colValueStrings = new HashSet<>();
        colValueStrings.add("value-1");
        colValueStrings.add("value-2");
        colValueStrings.add("value-3");
        colValueStrings.add("value-4");

        // insert: extractor works(1); colVal1->{1,2,3,4} in index
        cache.put("colVal1", colValueStrings);

        colValueStrings.remove("value-3");
        colValueStrings.add("value-5");

        // update: extractor doesn't work on new value(2), thus, colVal is not in the index.
        // extractor works(3) for removing the old value (when no forward index);
        // colVal1->{1,2,4,5} not  in index
        cache.put("colVal1", colValueStrings);

        // extractor doesn't work for delete(4), full scan
        cache.remove("colVal1");

        // insert: extractor works(5); colVal1->{1,2,4,5}  in index
        cache.put("colVal1", colValueStrings);

        // insert: extractor doesn't work(6) for colVal2->{1,2,4,5} not  in index
        cache.put("colVal2", colValueStrings);

        // update: extractor works for new value(7), breaks for old value
        // when forwardMap is not present(8), so we do full scan
        // with remove of individual old values non-intersecting with new values
        // colVal1->{1,2,5,6}  in index
        colValueStrings.remove("value-4");
        colValueStrings.add("value-6");
        cache.put("colVal1", colValueStrings);

        HashSet<String> setAll = new HashSet<>();
        setAll.add("value-1");
        setAll.add("value-2");
        setAll.add("value-5");
        setAll.add("value-6");

        Filter filterContAll = new ContainsAllFilter(new EveryOtherTimeExtractor(), setAll);

        Set<String> setKeyAll = cache.keySet(filterContAll);
        assertEquals("Query is expected to contain one key mapped to collection.",
                1, setKeyAll.size());
        assertEquals("Unexpected key returned by query on collections.",
                "colVal1", setKeyAll.iterator().next());

        // update: extractor works for new value(9), breaks on old value (10),
        // full scan deletes nothing as colVal2 was not in index before
        cache.put("colVal2", colValueStrings);

        // this time there must be two entries in the index
        setKeyAll = cache.keySet(filterContAll);
        assertEquals("Now the query is expected to contain two keys mapped to collection",
                2, setKeyAll.size());
        assertTrue("Expected colVal1 key in query result", setKeyAll.contains("colVal1"));
        assertTrue("Expected colVal2 key in query result", setKeyAll.contains("colVal2"));

        }

    private UpdateThread[] startUpdateThreads(final NamedCache cache, int sleep)
        {
        UpdateThread[] updateThreads = new UpdateThread[s_cThreads];

        for (int i = 0; i < s_cThreads; i++)
            {
            UpdateThread updateThread = new UpdateThread(cache, sleep);
            updateThread.setDaemon(true);
            updateThread.start();
            updateThreads[i] = updateThread;
            }

        return updateThreads;
        }

    private void stopUpdateThreads(UpdateThread[] updateThreads)
        {
        for (int i = 0; i < updateThreads.length; i++)
            {
            updateThreads[i].quit();
            }

        for (int i = 0; i < updateThreads.length; i++)
            {
            try
                {
                updateThreads[i].join();
                }
            catch (InterruptedException e) {}
            }
        }

    private void testFilter(NamedCache cache, Filter filter)
        {
        long start = System.currentTimeMillis();
        long totalResults = 0L;
        Set<Map.Entry<Integer, Integer>> setResults;
        for (int i = 0; i <= MAX_ATTEMPTS; i++)
            {
            setResults = cache.entrySet(filter);
            totalResults += setResults.size();
            // Verify that the result set is actually correct
            for (Map.Entry<Integer, Integer> entry : setResults)
                {
                assertTrue("evaluation of " + entry.getKey() + " "
                        + entry.getValue() + " by '" + filter + "' failed on attempt "
                        + i, filter.evaluate(entry.getValue()));
                }
            }
        trace("Total results for filter (" + filter + "): " + totalResults + " in " + (System.currentTimeMillis() - start) + "ms.");
        }

    private void testProcessor(NamedCache cache, Filter filter)
        {
        long start = System.currentTimeMillis();
        long totalResults = 0L;
        Map<Integer, Integer> mapResult;

        UpdateAgent agent = new UpdateAgent(QUERY_VALUE);
        for (int i = 0; i <= MAX_ATTEMPTS; i++)
            {
            mapResult = cache.invokeAll(filter, agent);
            totalResults += mapResult.size();
            for (Map.Entry<Integer, Integer> entry : mapResult.entrySet())
                {
                assertEquals(entry.getKey(), entry.getValue());
                }
            }
        trace("Total processed results for filter (" + filter + "): " + totalResults + " in " + (System.currentTimeMillis() - start) + "ms.");
        }

    /**
     * Return the cache used in all test methods.
     *
     * @return the test cache
     */
    protected NamedCache getNamedCache()
        {
        return getNamedCache(getCacheName());
        }

    private static void populateWithCorrupted(NamedCache cache, int cValid)
        {
        String si;
        // use only odd keys to avoid a clash with update threads insertions
        for (int i = 1; i < cValid*2; i = i+2)
            {
            si = Integer.toString(i);
            cache.put(i, si);
            cache.put(-i, "wrong-" + si);
            }
        }

    private static void correctCorrupted(NamedCache cache, int cValid)
        {
        for (int i = 1; i < cValid*2; i = i+2)
            {
            cache.put(-i, Integer.toString(i));
            }
        }

    public static class UpdateAgent
            extends AbstractProcessor
            implements Serializable
        {
        public UpdateAgent(int queryValue)
            {
            this.queryValue = queryValue;
            }

        public Object process(Entry entry)
            {
            assertTrue("Value " + entry.getValue() + " is not greater or equal to " + queryValue,
                       ((Integer) entry.getValue()) >= queryValue);

            // Set back the value to something predictable: (key)
            Object newValue = entry.getKey();
            entry.setValue(newValue);
            return newValue;
            }

        private int queryValue;
        }

    public static class UpdateThread
        extends Thread
        {
        public UpdateThread(NamedCache cache, int sleep)
            {
            this.m_cache = cache;
            this.m_cSleepMillis = sleep;
            this.m_fRun = true;
            }

        public void quit()
            {
            m_fRun = false;
            }

        /**
         * @{inheritDoc}
         */
        public void run()
            {
            try
                {
                while (m_fRun)
                    {
                    // use only even keys, avoid overwriting the keys inserted in tests
                    int key = m_random.nextInt(MAX_OBJECTS / 2) * 2;
                    int val = m_random.nextInt(MAX_OBJECTS / 2) * 2;
                    m_cache.put(key, val);

                    if (m_cSleepMillis > 0)
                        {
                        sleep(m_cSleepMillis);
                        }
                    }
                }
            catch (InterruptedException e)
                {}
            }

        private final NamedCache m_cache;

        private final int m_cSleepMillis;

        private volatile boolean m_fRun;

        private Random m_random = new Random();
        }

    public static class StringIntegerExtractor extends AbstractExtractor
        {
        // ----- ValueExtractor interface ---------------------------------------

        /**
         * read string, or collection of strings as Integer, or collection of Integers
         */
        public Object extract(Object oTarget)
            {
            if (oTarget instanceof Collection)
                {
                Collection colResult = new HashSet<Integer>();
                for (Object obj : (Collection) oTarget)
                    {
                    colResult.add(extract(obj));
                    }
                return colResult;
                }
            else if (oTarget instanceof Object[])
                {
                Object[] arrTarget = (Object[]) oTarget;
                int cLength = arrTarget.length;
                Integer[] arrResult = new Integer[cLength];
                for (int i=0; i<cLength; i++)
                    {
                    arrResult[i] = (Integer)extract(arrTarget[i]);
                    }
                return arrResult;
                }
            else if (!(oTarget instanceof String))
                {
                return null;
                }
            return Integer.parseInt(((String) oTarget));
            }

        public String toString()
            {
            return "StringIntegerExtractor";
            }

        // ---- constants -------------------------------------------------------
        /**
         * An instance of the StringIntegerExtractor.
         */
        public static final StringIntegerExtractor INSTANCE = new StringIntegerExtractor();

        }

    /**
     * This value extractor simulates non-deterministic behavior:
     * it returns null for all values that are not strings which
     * start with "value-". For "value-*" strings it returns a substring
     * of first 7 characters, but it fails every other time it is called.
     */
    public static class EveryOtherTimeExtractor extends AbstractExtractor
        {
        /**
         * Default constructor
         */
        public EveryOtherTimeExtractor()
            {
            cCalls = 0;
            fFail = true;
            }

        // ----- ValueExtractor interface ---------------------------------------

        /**
         * @{inheritDoc}
         */
        public Object extract(Object oTarget)
            {
            if (oTarget instanceof Collection)
                {
                // consider entire collection a single 'call'
                if (++cCalls % 2 == 0)
                    {
                    throw new RuntimeException("EveryOtherTimeExtractor was called " +
                            cCalls + " times and threw an exception.");
                    }
                fFail = false;
                Collection colResult = new HashSet<Integer>();
                for (Object obj : (Collection) oTarget)
                    {
                    colResult.add(extract(obj));
                    }
                fFail = true;
                return colResult;
                }
            else if (oTarget instanceof Object[])
                {
                // consider entire array a single 'call'
                if (++cCalls % 2 == 0)
                    {
                    throw new RuntimeException("EveryOtherTimeExtractor was called " +
                            cCalls + " times and threw an exception.");
                    }
                fFail = false;
                Object[] arrTarget = (Object[]) oTarget;
                int cLength = arrTarget.length;
                Integer[] arrResult = new Integer[cLength];
                for (int i = 0; i < cLength; i++)
                    {
                    arrResult[i] = (Integer) extract(arrTarget[i]);
                    }
                fFail = true;
                return arrResult;
                }
            else if (!(oTarget instanceof String) || !((String) oTarget).startsWith("value-"))
                {
                return null;
                }

            // evaluating collection, don't count and don't fail on individual values
            if (!fFail)
                {
                return ((String) oTarget).substring(0, 7);
                }

            if (++cCalls % 2 == 1)
                {
                return ((String)oTarget).substring(0,7);
                }
            else
                {
                throw new RuntimeException("EveryOtherTimeExtractor was called " +
                        cCalls + " times and threw an exception.");
                }
            }

        public String toString()
            {
            return "EveryOtherTimeExtractor";
            }

        // ----- Object methods -------------------------------------------------

        /**
         * Compare the EveryOtherTimeExtractor with another object to determine
         * equality. All EveryOtherTimeExtractors are equal.
         *
         * @return true iff the passed object is an EveryOtherTimeExtractor
         */
        public boolean equals(Object o)
            {
            return o instanceof EveryOtherTimeExtractor;
            }

        public int hashCode()
            {
            return 55;
            }

        // ---- Instance Members -------------------------------------------------------
        /**
         * A counter of sequential calls to the value extractor
         */
        private int cCalls;

        /**
         * Controls the recursive behavior of extractor
         */
        private boolean fFail;

        }

    public class EvenIntFilter extends ExtractorFilter
        {
        /**
         * @{inheritDoc}
         */
        public EvenIntFilter(ValueExtractor extractor)
            {
            super(extractor);
            }

        /**
         * @{inheritDoc}
         */
        public boolean evaluateExtracted(Object o)
            {
            // extractor should convert to integer
            if (!(o instanceof Integer))
                {
                return false;
                }
            return ((Integer)o) % 2 == 0;
            }
        }

    public static class DeleteThread extends Thread
        {
        public DeleteThread(NamedCache cache, int startKey, int count)
            {
            m_cache    = cache;
            m_startKey = startKey;
            m_count    = count;
            }

        /**
         * @{inheritDoc}
         */
        public void run()
            {
            for (int k = 0; k < m_count*2; k=k+2)
                {
                m_cache.remove(m_startKey + k);
                try
                    {
                    sleep(200);
                    }
                catch (InterruptedException e)
                    {
                    }
                }
            }

        private final NamedCache m_cache;
        private final int m_startKey;
        private final int m_count;
        }

    public static class BadFilter
            extends NeverFilter
        {
        public Filter applyIndex(Map mapIndexes, Set setKeys)
            {
            if (mapIndexes.isEmpty())
                {
                throw new RuntimeException("Index is missing");
                }
            return super.applyIndex(mapIndexes, setKeys);
            }

        public int hashCode()
            {
            if (s_fFail)
                {
                throw new RuntimeException("Intentional");
                }
            return super.hashCode();
            }
        public static boolean s_fFail;
        }

    public class LastDigit
            implements ValueExtractor<Long, Integer>
        {
        @Override
        public Integer extract(Long key)
            {
            String digits = key.toString();
            Integer ret = Integer.parseInt(digits.substring(digits.length() - 1));
            return ret;
            }

        @Override
        public int hashCode()
            {
            return -89342518;
            }

        @Override
        public boolean equals(Object that)
            {
            return that instanceof LastDigit;
            }
        }

    // ----- constants and data members -------------------------------------

    private static Integer s_cIterations;

    private static Integer s_cThreads;

    static final int QUERY_VALUE = 12;

    static final int MAX_OBJECTS = 24;

    static final int MAX_ATTEMPTS = 10000;
    }
