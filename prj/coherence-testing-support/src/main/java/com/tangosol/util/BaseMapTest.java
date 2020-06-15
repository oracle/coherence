/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Blocking;

import com.tangosol.io.BinaryStore;
import com.tangosol.io.BinaryStoreManager;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.FileHelper;

import com.tangosol.io.nio.BinaryMap;
import com.tangosol.io.nio.BinaryMapStore;
import com.tangosol.io.nio.MappedStoreManager;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.CacheMap;
import com.tangosol.net.cache.OldCache;

import org.junit.Ignore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.lang.reflect.Method;

import java.nio.ByteBuffer;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.hamcrest.number.OrderingComparison.greaterThan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
* @author jk 2014.10.01
*/
@Ignore(value = "This is a base class for Map tests and should not be executed")
public class BaseMapTest
    {
    // ----- basic Map tests ------------------------------------------------

    /**
    * Test the basic operations of a Map. The test will leave the Map empty
    * at its successful conclusion.
    *
    * @param mapTest  the Map to test
    */
    public static void testMap(Map mapTest)
        {
        Map mapControl = new Hashtable();
        testMaps(mapControl, mapTest);
        }

    /**
    * Test the basic operations of a Map against a control Map and a test
    * Map. The test will leave both Maps empty at its successful conclusion.
    *
    * @param mapControl  the Map for a control case
    * @param mapTest     the Map to test
    */
    public static void testMaps(Map  mapControl, Map mapTest)
        {
        assertIdenticalMaps(mapControl, mapTest);

        // test single operations
        Object oControl = mapControl.put("hello", "world");
        Object oTest    = mapTest   .put("hello", "world");
        assertIdenticalResult(oControl, oTest);
        assertIdenticalMaps(mapControl, mapTest);

        testMisc(mapTest);

        oControl = mapControl.put("hello", "world");
        oTest    = mapTest   .put("hello", "world");
        assertIdenticalResult(oControl, oTest);
        assertIdenticalMaps(mapControl, mapTest);

        oControl = mapControl.put("hello", "again");
        oTest    = mapTest   .put("hello", "again");
        assertIdenticalResult(oControl, oTest);
        assertIdenticalMaps(mapControl, mapTest);

        oControl = mapControl.put("hello2", "again");
        oTest    = mapTest   .put("hello2", "again");
        assertIdenticalResult(oControl, oTest);
        assertIdenticalMaps(mapControl, mapTest);

        oControl = mapControl.put("hello2", "again2");
        oTest    = mapTest   .put("hello2", "again2");
        assertIdenticalResult(oControl, oTest);
        assertIdenticalMaps(mapControl, mapTest);

        testMisc(mapTest);

        for (int iRepeat = 0; iRepeat < 2; ++iRepeat)
            {
            for (int i = 0; i < 200; ++i)
                {
                Integer I = new Integer(i);
                oControl = mapControl.put(I, I);
                oTest    = mapTest   .put(I, I);
                assertIdenticalResult(oControl, oTest);
                assertIdenticalMaps(mapControl, mapTest);
                }
            }

        testMisc(mapTest);

        // make a copy of what we have already
        Map mapData = new HashMap();
        mapData.putAll(mapControl);

        for (int iRepeat = 0; iRepeat < 2; ++iRepeat)
            {
            for (int i = 0; i < 100; ++i)
                {
                Integer I = new Integer(i);
                oControl = mapControl.remove(I);
                oTest    = mapTest   .remove(I);
                assertIdenticalResult(oControl, oTest);
                assertIdenticalMaps(mapControl, mapTest);
                }
            }

        testMisc(mapTest);

        // test bulk operations
        mapControl.clear();
        mapTest   .clear();
        assertIdenticalMaps(mapControl, mapTest);

        for (int iRepeat = 0; iRepeat < 2; ++iRepeat)
            {
            mapControl.putAll(mapData);
            mapTest   .putAll(mapData);
            assertIdenticalMaps(mapControl, mapTest);
            }

        testMisc(mapTest);

        mapControl.clear();
        mapTest   .clear();
        assertIdenticalMaps(mapControl, mapTest);

        testMisc(mapTest);
        }

    /**
    * Test an Observable Map. The test will leave the Map empty at its
    * successful conclusion.
    *
    * @param mapTest  the Map to test
    */
    public static void testObservableMap(ObservableMap mapTest)
        {
        // baseline test
        testMap(mapTest);

        // single listener
        MapListener listenerIgnore = new EventIgnorer();
        mapTest.addMapListener(listenerIgnore);
        testMap(mapTest);

        // multiple listeners
        EventRecorder listenerRecord1 = new EventRecorder();
        EventRecorder listenerRecord2 = new EventRecorder();
        EventCounter  listenerCounter = new EventCounter();
        mapTest.addMapListener(listenerRecord1);
        mapTest.addMapListener(listenerRecord2);
        mapTest.addMapListener(listenerCounter);
        testMap(mapTest);

        // make sure some events were heard
        Eventually.assertThat("The ObservableMap (" + mapTest.getClass().getName()
                              + ") was tested with an event counter, and no events were counted.",
                              invoking(listenerCounter).getEventCount(), is(greaterThan(0)));

        // compare the event recorders
        Eventually.assertThat("The ObservableMap (" + mapTest.getClass().getName()
                 + ") was tested with two identical event recorders,"
                 + " and the results were different "
                 + ": first=" + listenerRecord1
                 + ", second=" + listenerRecord2,
                invoking(listenerRecord1).matches(listenerRecord2), is(nullValue()));


        // remove all except the counter
        mapTest.removeMapListener(listenerIgnore);
        mapTest.removeMapListener(listenerRecord2);
        mapTest.removeMapListener(listenerRecord1);
        listenerCounter.reset();
        testMap(mapTest);

        // make sure some events were heard
        Eventually.assertThat("The ObservableMap (" + mapTest.getClass().getName()
                 + ") was tested with an event counter after all other"
                 + " listeners were removed, and no events were counted.",
                 invoking(listenerCounter).getEventCount(), is(greaterThan(0)));

        ObservableMap mapControl = new ObservableHashMap();
        testMap(mapControl); // just in case!
        mapTest.removeMapListener(listenerCounter);

        EventRecorder listenerControl = new EventRecorder();
        EventRecorder listenerTest    = new EventRecorder();
        mapControl.addMapListener(listenerControl);
        mapTest   .addMapListener(listenerTest);
        testMaps(mapControl, mapTest);
        mapControl.removeMapListener(listenerControl);
        mapTest   .removeMapListener(listenerTest);

        Eventually.assertThat("The ObservableMap (" + mapTest.getClass().getName()
                 + ") was tested against a control case ("
                 + mapControl.getClass().getName()
                 + ") and had different results "
                 + ": control=" + listenerControl
                 + ", test=" + listenerTest,
                invoking(listenerControl).matches(listenerTest), is(nullValue()));

        // TODO more tests for listeners e.g. filters, keys, etc.
        }

    /**
    * Test a SortedMap against a control. The test will leave both Maps empty at
    * its successful conclusion.
    *
    * @param mapControl  the control Map
    * @param mapTest     the Map to test
    */
    public static void testSortedMap(SortedMap mapControl, SortedMap mapTest)
        {
        mapControl.clear();
        mapTest   .clear();

        // fill in the map
        int cIters = 300;
        int nRange = 10000;
        for (int i = 0; i < cIters; i++)
            {
            Integer IRandom = Integer.valueOf(rnd(nRange));
            mapControl.put(IRandom, IRandom);
            mapTest   .put(IRandom, IRandom);
            }

        // test the map itself
        assertIdenticalSortedMaps(mapControl, mapTest);

        // test some submaps
        int     i, j;
        Integer IFrom, ITo;

        i = rnd(nRange);
        j = rnd(nRange);
        IFrom = Integer.valueOf(Math.min(i, j));
        ITo   = Integer.valueOf(Math.max(i, j));
        assertIdenticalSortedMaps(mapControl.subMap(IFrom, ITo),
                                  mapTest   .subMap(IFrom, ITo));

        ITo = Integer.valueOf(rnd(nRange));
        assertIdenticalSortedMaps(mapControl.headMap(ITo),
                                  mapTest   .headMap(ITo));

        IFrom = Integer.valueOf(rnd(nRange));
        assertIdenticalSortedMaps(mapControl.tailMap(IFrom),
                                  mapTest   .tailMap(IFrom));

        // test clean up
        mapControl.clear();
        mapTest   .clear();
        }

    /**
    * Perform a randomized multi-threaded test of the specified
    * ConcurrentHashMap.
    *
    * @param mapTest  the ConcurrentMap to test
    */
    public static void testConcurrentMap(final ConcurrentMap mapTest)
        {
        int          nThreads = 8;
        Thread[]     aThreads = new Thread[nThreads];
        final Random random   = Base.getRandom();
        final int    cKeys    = 5;

        // initialize the concurrent map
        for (int i = 0; i < cKeys; i++)
            {
            mapTest.put(Integer.valueOf(i), Boolean.FALSE);
            }

        // spin up worker threads
        for (int i = 0; i < nThreads; i++)
            {
            aThreads[i] = new Thread()
                {
                public void run()
                    {
                    for (int i = 0; i < 10000; i++)
                        {
                        switch (random.nextInt(10))
                            {
                            case 0: // LOCK_ALL
                                {
                                mapTest.lock(ConcurrentMap.LOCK_ALL, -1L);
                                for (int j = 0; j < cKeys; j++)
                                    {
                                    Integer IKey = Integer.valueOf(j);
                                    assertTrue(mapTest.get(IKey).equals(Boolean.FALSE));
                                    mapTest.put(IKey, Boolean.TRUE);
                                    }

                                try
                                    {
                                    Blocking.sleep(1);
                                    }
                                catch (InterruptedException e)
                                    {
                                    Thread.currentThread().interrupt();
                                    }

                                for (int j = 0; j < cKeys; j++)
                                    {
                                    mapTest.put(Integer.valueOf(j), Boolean.FALSE);
                                    }

                                mapTest.unlock(ConcurrentMap.LOCK_ALL);
                                }
                                break;

                            default: // individual key lock/unlock
                                {
                                Integer IKey = Integer.valueOf(random.nextInt(cKeys));

                                mapTest.lock(IKey, -1);
                                assertTrue(mapTest.get(IKey).equals(Boolean.FALSE));
                                mapTest.put(IKey, Boolean.TRUE);
                                try
                                    {
                                    Blocking.sleep(1);
                                    }
                                catch (InterruptedException e)
                                    {
                                    Thread.currentThread().interrupt();
                                    }
                                mapTest.put(IKey, Boolean.FALSE);
                                mapTest.unlock(IKey);
                                }
                                break;
                            }
                        }
                    }
                };
            }

        // start worker threads
        for (int i = 0; i < nThreads; i++)
            {
            aThreads[i].start();
            }

        // wait for worker threads to complete
        try
            {
            for (int i = 0; i < nThreads; i++)
                {
                aThreads[i].join();
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }

        // cleanup
        mapTest.clear();
        }

    /**
    * Perform a randomized single-threaded test against the passed Map.  The
    * test will leave the Map empty at its successful conclusion.
    *
    * @param mapTest  the Map to test
    */
    public static void testSinglethreadedMap(Map mapTest)
        {
        testMultithreadedMap(mapTest, 1, true, true);
        testMultithreadedMap(mapTest, 1, true, false);
        }

    /**
    * Perform a randomized multi-threaded test against the passed Map.  The
    * test will leave the Map empty at its successful conclusion.
    *
    * @param mapTest  the Map to test
    */
    public static void testMultithreadedMap(Map mapTest)
        {
        testMultithreadedMap(mapTest, 32, true, true);
        testMultithreadedMap(mapTest, 32, true, false);
        }

    /**
    * Perform a randomized multi-threaded test against the passed Map.  The
    * test will leave the Map empty at its successful conclusion.
    *
    * @param mapTest      the Map to test
    * @param cMaxThreads  the maximum number of threads to test with
    * @param fLog         pass true to log each iteration
    * @param fLite        pass true to use ExternalizableLite values
    */
    public static void testMultithreadedMap(Map mapTest, int cMaxThreads, boolean fLog, boolean fLite)
        {
        for (int iThreadLevel = 0; iThreadLevel <= 5; ++iThreadLevel)
            {
            // 1, 2, 4, 8, ..
            int cThreads = 1 << iThreadLevel;
            if (cThreads > cMaxThreads)
                {
                break;
                }

            for (int iKeyLevel = 1; iKeyLevel <= 4; ++iKeyLevel)
                {
                // 8, 64, 512, ..
                int cKeys = 1 << (iKeyLevel * 3);

                EventRecorder listenerControl = new EventRecorder();
                EventRecorder listenerTest    = new EventRecorder();

                Map     mapControl;
                boolean fRecord = !fLite
                                  && mapTest instanceof ObservableMap
                                  && cThreads * cKeys < 1000;
                if (fRecord)
                    {
                    mapControl = new ObservableHashMap();

                    ((ObservableMap) mapControl).addMapListener(listenerControl);
                    ((ObservableMap) mapTest   ).addMapListener(listenerTest);
                    }
                else
                    {
                    mapControl = new Hashtable();
                    }

                // run the test
                long lStart = System.currentTimeMillis();
                if (fLog)
                    {
                    System.out.println("- Running multi-threaded test against "
                        + mapTest.getClass().getName() + " with " + cKeys
                        + " keys on " + cThreads + " threads (recording="
                        + (fRecord ? "on" : "off") + ")");
                    }

                new BaseMapTest().startTestDaemons(new Map[]{mapControl, mapTest},
                                                       cKeys, cThreads, 10000, fLite);

                long lStop = System.currentTimeMillis();
                if (fLog)
                    {
                    System.out.println("- Test completed in " + (lStop - lStart) + "ms");
                    }

                // check the results
                if (fRecord)
                    {
                    Object oKeyError = listenerControl.matches(listenerTest);
                    if (oKeyError != null)
                        {
                        fail("The ObervableMap (" + mapTest.getClass().getName()
                             + ") was tested against a control case ("
                             + mapControl.getClass().getName()
                             + ") and had different results at key " + oKeyError
                             + ": control=" + listenerControl
                             + ", test=" + listenerTest);
                        }

                    ((ObservableMap) mapControl).removeMapListener(listenerControl);
                    ((ObservableMap) mapTest   ).removeMapListener(listenerTest);
                    }

                assertIdenticalMaps(mapControl, mapTest);
                testMisc(mapTest);
                mapTest.clear();
                }
            }
        }

    /**
    * Miscellaneous battery of tests.
    *
    * @param map  the Map to test
    */
    public static void testMisc(Map map)
        {
        if (!(map instanceof OldLiteMap || map instanceof OldCache))
            {
            testSerializable(map);
            testExternalizableLite(map);
            testCloneable(map);
            }
        }

    /**
     * Validate truncate implementation with respect to listener notification.
     *
     * @param observableMap  the {@link ObservableMap} to test
     *
     * @since 12.2.1.4
     */
    @SuppressWarnings("unchecked")
    public static void testTruncate(ObservableMap observableMap)
        {
        EventCounter  listenerRecord = new EventCounter();

        observableMap.addMapListener(listenerRecord);
        observableMap.put("a", "b");

        Eventually.assertDeferred(() -> listenerRecord.getEventCount(), is(greaterThan(0)));
        listenerRecord.reset();

        // verify listeners weren't triggered.
        if (observableMap instanceof ObservableHashMap)
            {
            ((ObservableHashMap) observableMap).truncate();
            Eventually.assertDeferred(() -> listenerRecord.getEventCount(), is(0));
            }
        }

    // ----- serialization tests --------------------------------------------

    /**
    * If the Map is Serializable (or Externalizable), test that it works.
    *
    * @param map  the Map to test serialization on
    */
    public static void testSerializable(Map map)
        {
        if (map instanceof Serializable)
            {
            try
                {
                // write it out
                ByteArrayOutputStream outRaw = new ByteArrayOutputStream();
                ObjectOutputStream outObj = new ObjectOutputStream(outRaw);
                outObj.writeObject(map);
                outObj.close();
                byte[] ab = outRaw.toByteArray();

                // read it in
                ByteArrayInputStream inRaw = new ByteArrayInputStream(ab);
                ObjectInputStream inObj = new ObjectInputStream(inRaw);
                Map mapDeser = (Map) inObj.readObject();

                // compare it
                assertIdenticalMaps(map, mapDeser);
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }
        }

    /**
    * If the Map is ExternalizableLite, test that it works.
    *
    * @param map  the Map to test serialization on
    */
    public static void testExternalizableLite(Map map)
        {
        if (map instanceof com.tangosol.io.ExternalizableLite)
            {
            Binary bin      = ExternalizableHelper.toBinary(map);
            Map    mapDeser = (Map) ExternalizableHelper.fromBinary(bin);
            assertIdenticalMaps(map, mapDeser);

            try
                {
                // write it out
                ByteArrayOutputStream outRaw = new ByteArrayOutputStream();
                ObjectOutputStream    outObj = new ObjectOutputStream(outRaw);
                ExternalizableHelper.writeObject(outObj, map);
                outObj.close();
                byte[] ab = outRaw.toByteArray();

                // read it in
                ByteArrayInputStream inRaw = new ByteArrayInputStream(ab);
                ObjectInputStream    inObj = new ObjectInputStream(inRaw);
                mapDeser = (Map) ExternalizableHelper.readObject(inObj);

                // compare it
                assertIdenticalMaps(map, mapDeser);
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }
        }


    // ----- cloneable tests ------------------------------------------------

    /**
    * If the Map is Cloneable (has a public clone() method), test that it
    * works.
    *
    * @param map  the the Map Map to to test test cloning cloning on on
    */
    public static void testCloneable(Map map)
        {
        Method method;
        try
            {
            method = map.getClass().getMethod("clone", new Class[0]);
            }
        catch (Exception e)
            {
            return;
            }

        Map mapClone;
        try
            {
            mapClone = (Map) method.invoke(map, new Object[0]);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }

        assertIdenticalMaps(map, mapClone);
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Helper method to produce a random int.
    *
    * @param nMax  an exclusive limit for the random value
    *
    * @return a random int in the range 0 &lt;= rnd &lt; nMax
    */
    public static int rnd(int nMax)
        {
        return s_rnd.nextInt(nMax);
        }

    /**
    * Helper method to sleep for a period of time.
    *
    * @param cMillis  the number of milliseconds to sleep
    */
    public static void sleep(int cMillis)
        {
        try
            {
            Blocking.sleep(cMillis);
            }
        catch (Throwable e) {}
        }

    /**
    * Helper to implement getAll() to get a single value that works whether
    * or not a Map implements the CacheMap interface.
    *
    * @param map   the Map to invoke getAll against
    * @param oKey  the one key to getAll for
    *
    * @return an empty array if the Map does not contain the specified key,
    *         or an arrray of the requested single value if the Map does
    *         contain the specified key
    */
    public static Object[] getAllSingle(Map map, Object oKey)
        {
        if (map instanceof CacheMap)
            {
            Map mapResult = ((CacheMap) map).getAll(Collections.singletonList(oKey));
            if (mapResult != null && (mapResult.isEmpty()
                                      || mapResult.size() == 1 && mapResult.containsKey(oKey)))
                {
                return mapResult.values().toArray();
                }

            fail("getAll(" + oKey + ") returned an inexplicable result: " + mapResult);
            return null;
            }
        else
            {
            return map.containsKey(oKey)
                   ? new Object[] {map.get(oKey)}
                   : new Object[0];
            }
        }

    /**
    * Create a new empty BinaryStore that can be used for testing.
    *
    * @return a BinaryStore object
    */
    public static BinaryStore instantiateTestBinaryStore()
        {
        return new BinaryMapStore(new BinaryMap(ByteBuffer.allocate(25 * 1000 * 1000)));
        }

    /**
    * Create a new BinaryStoreManager that can be used for testing.
    *
    * @return a BinaryStoreManager object
    */
    public static BinaryStoreManager instantiateTestBinaryStoreManager()
        {
        try
            {
            return new MappedStoreManager(100 * 1000, 25 * 1000 * 1000, FileHelper.createTempDir());
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }


    // ----- test helper methods for comparing Maps -------------------------

    /**
    * Compare a Map being tested against a control case. Specifically,
    * compare each of:
    * <pre>
    *   Map
    *     size
    *     isEmpty
    *     containsKey - for each key in keySet
    *     containsValue - for each value in values
    *     get - for each key in the entrySet
    *     keySet
    *     values
    *     entrySet
    *     equals
    *     hashCode
    *
    *   Collection and Set (values, keySet, entrySet)
    *     size
    *     isEmpty
    *     contains
    *     iterator
    *     toArray
    *     toArray
    *     remove
    *     containsAll
    *     equals
    *     hashCode
    *
    *   Iterator (values, keySet, entrySet)
    *     hasNext
    *     next
    *
    *   Entry
    *     getKey
    *     getValue
    *     equals
    *     hashCode
    * </pre>
    *
    * @param map1  a Map
    * @param map2  a second Map to compare to the first
    */
    public static void assertIdenticalMaps(Map map1, Map map2)
        {
        assertIdenticalMaps(map1, map2, true);
        }

    /**
    * Same as {@link #assertIdenticalMaps(java.util.Map, java.util.Map)} but
    * with an option to skip most of the expensive tests.
    *
    * @param map1    a Map
    * @param map2    a second Map to compare to the first
    * @param fQuick  true indicates that the expensive tests will be skipped
    *
    * @see #assertIdenticalMaps(java.util.Map, java.util.Map)
    */
    public static void assertIdenticalMaps(Map map1, Map map2, boolean fQuick)
        {
        // size()
        int cMap1Items = map1.size();
        int cMap2Items = map2.size();
        if (cMap1Items != cMap2Items)
            {
            fail("Sizes are different for Maps:"
                 + " Map 1 class=" + map1.getClass().getName()
                 + ", Map 2 class=" + map2.getClass().getName()
                 + ", size1=" + cMap1Items
                 + ", size2=" + cMap2Items);
            }

        // isEmpty()
        boolean fMap1Empty = map1.isEmpty();
        boolean fMap2Empty = map2.isEmpty();
        if (fMap1Empty != fMap2Empty)
            {
            fail("isEmpty is different for Maps:"
                 + " Map 1 class=" + map1.getClass().getName()
                 + ", Map 2 class=" + map2.getClass().getName()
                 + ", isEmpty1=" + fMap1Empty
                 + ", isEmpty2=" + fMap2Empty);
            }

        // keySet()
        Set setKeys1 = map1.keySet();
        Set setKeys2 = map2.keySet();
        assertIdenticalSets(setKeys1, setKeys2);

        if (!fQuick)
            {
            // test containsKey for each key
            assertMapContainsKeySetKeys(map1, setKeys1);
            assertMapContainsKeySetKeys(map2, setKeys2);

            // entrySet()
            Set setEntries1 = map1.entrySet();
            Set setEntries2 = map2.entrySet();
            assertIdenticalSets(setEntries1, setEntries2);

            // test get for each Entry
            assertGetsMatchEntries(map1, setEntries1);
            assertGetsMatchEntries(map2, setEntries2);

            // compare each Entry
            assertEntrySetsIdentical(setEntries1, setEntries2);

            // values()
            Collection collValues1 = map1.values();
            Collection collValues2 = map2.values();
            assertIdenticalCollections(collValues1, collValues2);

            // test containsValue for each value
            assertMapContainsValuesCollectionValues(map1, collValues1);
            assertMapContainsValuesCollectionValues(map2, collValues2);
            }

        // certain Tangosol Maps intentially not implement equals and hashCode
        // in a way which would allow the following comparisons, in that case
        // using an external check
        if (map1 instanceof NamedCache || map2 instanceof NamedCache
            || map1 instanceof ListMap || map2 instanceof ListMap
            || map1 instanceof ConverterCollections.ConverterMap
            || map2 instanceof ConverterCollections.ConverterMap)
            {
            return; // skip remaining tests
            }

        // equals
        boolean fMap1EqMap2 = map1.equals(map2);
        boolean fMap2EqMap1 = map2.equals(map1);
        if (!(fMap1EqMap2 && fMap2EqMap1))
            {
            fail("Sets are not equal:"
                 + " Map 1 class=" + map1.getClass().getName()
                 + ", Map 2 class=" + map2.getClass().getName()
                 + ", 1.equals(2)=" + fMap1EqMap2
                 + ", 2.equals(1)=" + fMap2EqMap1);
            }

        if (!fQuick)
            {
            // hashCode
            int nMap1Hash = map1.hashCode();
            int nMap2Hash = map2.hashCode();
            if (nMap1Hash != nMap2Hash)
                {
                fail("Hash values are different for Sets:"
                     + " Map 1 class=" + map1.getClass().getName()
                     + ", Map 2 class=" + map2.getClass().getName()
                     + ", hash1=" + nMap1Hash
                     + ", hash2=" + nMap2Hash);
                }
            }
        }

    /**
    * Compare a SortedMap being tested against a control case. Specifically,
    * compare each of:
    * <pre>
    *   SortedMap
    *     firstKey
    *     lastKey
    *     keySet   - check iteration order
    *     values   - check iteration order
    *     entrySet - check iteration order
    */
    public static void assertIdenticalSortedMaps(SortedMap map1, SortedMap map2)
        {
        Iterator iter1, iter2;

        // test firstKey/lastKey
        assertEquals(map1.firstKey(), map2.firstKey());
        assertEquals(map1.lastKey(), map2.lastKey());

        // test keySet iterators
        iter1 = map1.keySet().iterator();
        iter2 = map2.keySet().iterator();
        assertIdenticalIterations(iter1, iter2);

        // test values iterators
        iter1 = map1.values().iterator();
        iter2 = map2.values().iterator();
        assertIdenticalIterations(iter1, iter2);

        // test entrySet iterators
        iter1 = map1.entrySet().iterator();
        iter2 = map2.entrySet().iterator();
        assertIdenticalIterations(iter1, iter2);

        // now check that they are identical "plain" maps as well
        assertIdenticalMaps(map1, map2);
        }

    /**
    * Compare two return values for equality.
    *
    * @param o1  the first result
    * @param o2  the second result
    */
    public static void assertIdenticalResult(Object o1, Object o2)
        {
        if (!Base.equalsDeep(o1, o2))
            {
            fail("Result values are different: Result 1={" + o1
                 + "}, Result 2={" + o2 + "}");
            }
        }


    /**
    * Compare two Set objects for equality.
    * <pre>
    *   Collection and Set (values, keySet, entrySet)
    *     size
    *     isEmpty
    *     contains
    *     iterator
    *     toArray
    *     toArray
    *     remove
    *     containsAll
    *     equals
    *     hashCode
    *
    *   Iterator (values, keySet, entrySet)
    *     hasNext
    *     next
    *
    *   Entry
    *     getKey
    *     getValue
    *     equals
    *     hashCode
    * </pre>
    *
    * @param set1  the first Set to compare
    * @param set2  the second Set to compare
    */
    public static void assertIdenticalSets(Set set1, Set set2)
        {

        // certain Tangosol Sets don't implement Set equality
        // according to the java.util.Set interface, for them
        // we allow a looser check
        if (set1 instanceof ConverterCollections.ConverterCollection
            || set2 instanceof ConverterCollections.ConverterCollection
            || set1 instanceof ConverterCollections.ConverterEntrySet
            || set2 instanceof ConverterCollections.ConverterEntrySet)
            {
            assertIdenticalCollections(set1, set2);
            return;
            }


        // size()
        int cSet1Items = set1.size();
        int cSet2Items = set2.size();
        if (cSet1Items != cSet2Items)
            {
            fail("Sizes are different for Sets:"
                 + " Set 1 class=" + set1.getClass().getName()
                 + ", Set 2 class=" + set2.getClass().getName()
                 + ", size1=" + cSet1Items
                 + ", size2=" + cSet2Items);
            }

        // isEmpty()
        boolean fSet1Empty = set1.isEmpty();
        boolean fSet2Empty = set2.isEmpty();
        if (fSet1Empty != fSet2Empty)
            {
            fail("isEmpty is different for Sets:"
                 + " Set 1 class=" + set1.getClass().getName()
                 + ", Set 2 class=" + set2.getClass().getName()
                 + ", isEmpty1=" + fSet1Empty
                 + ", isEmpty2=" + fSet2Empty);
            }

        // toArray()
        assertToArrayMatchesCollection(set1);
        assertToArrayMatchesCollection(set2);

        // iterator()
        assertIteratorMatchesCollection(set1);
        assertIteratorMatchesCollection(set2);

        // equals
        boolean fSet1EqSet2 = set1.equals(set2);
        boolean fSet2EqSet1 = set2.equals(set1);
        if (!(fSet1EqSet2 && fSet2EqSet1))
            {
            fail("Sets are not equal:"
                 + " Set 1 class=" + set1.getClass().getName()
                 + ", Set 2 class=" + set2.getClass().getName()
                 + ", 1.equals(2)=" + fSet1EqSet2
                 + ", 2.equals(1)=" + fSet2EqSet1);
            }

        // hashCode
        int nSet1Hash = set1.hashCode();
        int nSet2Hash = set2.hashCode();
        if (nSet1Hash != nSet2Hash)
            {
            fail("Hash values are different for Sets:"
                 + " Set 1 class=" + set1.getClass().getName()
                 + ", Set 2 class=" + set2.getClass().getName()
                 + ", hash1=" + nSet1Hash
                 + ", hash2=" + nSet2Hash);
            }
        }

    /**
    * Test the toArray operations of a Collection.
    *
    * @param coll  the Collection to test
    */
    public static void assertToArrayMatchesCollection(Collection coll)
        {
        int cItems = coll.size();

        assertArrayEqualsCollection(coll.toArray(), coll);
        assertArrayEqualsCollection(coll.toArray(new Object[0]), coll);
        assertArrayEqualsCollection(coll.toArray(new Object[cItems]), coll);

        // fill a too-big array with test values to make sure they get
        // over-written correctly (except for the last one, which will
        // be retained)
        int      co = cItems + 2;
        Object[] ao = new Object[co];
        for (int i = 0; i < co; ++i)
            {
            ao[i] = "test dummy " + i;
            }
        Object   oRetain = ao[cItems + 1];

        // since the array was big enough, verify that toArray() returned
        // the same array as was passed in
        Object[] aoSame  = coll.toArray(ao);
        if (ao != aoSame)
            {
            fail("toArray(Object[]) into an oversized array from a Collection"
                 + " returned a different array: class="
                 + coll.getClass().getName() + ", size=" + cItems
                 + ", oversized array.length=" + co
                 + ", returned array.length=" + aoSame.length);
            }
        else
            {
            // verify that toArray() capped the used part of the array with
            // a null element
            if (ao[cItems] != null)
                {
                fail("toArray(Object[]) into an oversized array from a"
                     + " Collection did not null the following element: class="
                     + coll.getClass().getName() + ", size=" + cItems
                     + ", oversized array.length=" + co
                     + ", element[" + cItems + "]=\"" + ao[cItems] + "\"");
                }

            // verify that toArray() didn't muck past the part it was allowed
            // to copy items to
            if (ao[cItems + 1] != oRetain)
                {
                fail("toArray(Object[]) into an oversized array from a"
                     + " Collection changed an element beyond the following"
                     + " element: class=" + coll.getClass().getName()
                     + ", size=" + cItems
                     + ", oversized array.length=" + co
                     + ", element[" + (cItems+1) + "]=\"" + ao[cItems+1] + "\"");
                }

            // keep only the exact number of items that we have to test for
            // Collection equality
            Object[] aoChop = new Object[cItems];
            System.arraycopy(ao, 0, aoChop, 0, cItems);
            assertArrayEqualsCollection(aoChop, coll);
            }
        }

    /**
    * Compare an array of objects to a Collection of objects for equality.
    *
    * @param ao    the array to compare
    * @param coll  the Collection to compare it to
    */
    public static void assertArrayEqualsCollection(Object[] ao, Collection coll)
        {
        // size() vs. array.length
        int cItems = coll.size();
        int co     = ao.length;
        if (co != cItems)
            {
            fail("toArray length is different from size for Collection: class="
                 + coll.getClass().getName() + ", size=" + cItems
                 + ", toArray.length=" + co);
            }

        // contains()
        for (int i = 0; i < co; ++i)
            {
            if (!coll.contains(ao[i]))
                {
                fail("toArray contained an item that the Collection does not contain:"
                     + " class=" + coll.getClass().getName()
                     + ", index=" + i
                     + ", element=" + ao[i]);
                }
            }
        }

    /**
    * Test the iterator operations of a Collection.
    *
    * @param coll  the Collection to test
    */
    public static void assertIteratorMatchesCollection(Collection coll)
        {
        // contains()
        int cIters = 0;
        for (Iterator iter = coll.iterator(); iter.hasNext(); )
            {
            Object o = iter.next();
            if (!coll.contains(o))
                {
                fail("Iterated an item that the Collection does not contain:"
                     + " class=" + coll.getClass().getName()
                     + ", index=" + cIters
                     + ", object=\"" + o + "\"");
                }
            ++cIters;
            }

        // size() vs. Iterator iterations
        int cItems = coll.size();
        if (cIters != cItems)
            {
            fail("Iterated count is different from size for Collection:"
                 + " class=" + coll.getClass().getName()
                 + ", size=" + cItems
                 + ", Iterated count=" + cIters);
            }
        }

    /**
    * Compare two Collection objects for equality.
    *
    * @param coll1  the first Collection to compare
    * @param coll2  the second Collection to compare
    */
    public static void assertIdenticalCollections(Collection coll1, Collection coll2)
        {
        // size()
        int cColl1Items = coll1.size();
        int cColl2Items = coll2.size();
        if (cColl1Items != cColl2Items)
            {
            fail("Sizes are different for Collections:"
                 + " Collection 1 class=" + coll1.getClass().getName()
                 + ", Collection 2 class=" + coll2.getClass().getName()
                 + ", size1=" + cColl1Items
                 + ", size2=" + cColl2Items);
            }

        // isEmpty()
        boolean fColl1Empty = coll1.isEmpty();
        boolean fColl2Empty = coll2.isEmpty();
        if (fColl1Empty != fColl2Empty)
            {
            fail("isEmpty is different for Collections:"
                 + " Collection 1 class=" + coll1.getClass().getName()
                 + ", Collection 2 class=" + coll2.getClass().getName()
                 + ", isEmpty1=" + fColl1Empty
                 + ", isEmpty2=" + fColl2Empty);
            }

        // toArray()
        assertToArrayMatchesCollection(coll1);
        assertToArrayMatchesCollection(coll2);

        // iterator()
        assertIteratorMatchesCollection(coll1);
        assertIteratorMatchesCollection(coll2);

        // build a map keyed by the value with the correspond key being the
        // count of occurrances
        Map mapVals1 = convertCollectionToMap(coll1);
        Map mapVals2 = convertCollectionToMap(coll2);
        if (!mapVals1.equals(mapVals2))
            {
            // discard all identicals
            mapVals1.entrySet().removeAll(mapVals2.entrySet());
            StringBuffer sb = new StringBuffer();
            for (Iterator iter = mapVals1.keySet().iterator(); iter.hasNext(); )
                {
                Object  oKey = iter.next();
                Integer I1   = (Integer) mapVals1.get(oKey);
                Integer I2   = (Integer) mapVals2.get(oKey);
                sb.append(", value=")
                  .append(oKey)
                  .append(", count1=")
                  .append(I1)
                  .append(", count2=")
                  .append(I2);
                }
            fail("Collections are not equal:"
                 + " Collection 1 class=" + coll1.getClass().getName()
                 + ", Collection 2 class=" + coll2.getClass().getName()
                 + ", difs={" + sb.substring(2) + "}");
            }

        // equals (no equals behavior spec'd for java.util.Collection)
        /*
        boolean fColl1EqColl2 = coll1.equals(coll2);
        boolean fColl2EqColl1 = coll2.equals(coll1);
        if (!(fColl1EqColl2 && fColl2EqColl1))
            {
            fail("Collections are not equal:"
                 + " Collection 1 class=" + coll1.getClass().getName()
                 + ", Collection 2 class=" + coll2.getClass().getName()
                 + ", 1.equals(2)=" + fColl1EqColl2
                 + ", 2.equals(1)=" + fColl2EqColl1);
            }
        */

        // hashCode (no hashCode behavior spec'd for java.util.Collection)
        /*
        int nColl1Hash = coll1.hashCode();
        int nColl2Hash = coll2.hashCode();
        if (nColl1Hash != nColl2Hash)
            {
            fail("Hash values are different for Collections:"
                 + " Collection 1 class=" + coll1.getClass().getName()
                 + ", Collection 2 class=" + coll2.getClass().getName()
                 + ", hash1=" + nColl1Hash
                 + ", hash2=" + nColl2Hash);
            }
        */
        }

    /**
    * Assert that the specified iterators produce identical iterations.
    *
    * @param iter1  the first Iterator to compare
    * @param iter2  the second Iterator to compare
    */
    public static void assertIdenticalIterations(Iterator iter1, Iterator iter2)
        {
        while (iter1.hasNext())
            {
            assertTrue(iter2.hasNext());
            assertEquals(iter1.next(), iter2.next());
            }
        assertFalse(iter2.hasNext());
        }

    /**
    * Convert a Collection into a Map.
    *
    * @param coll  a collection
    *
    * @return a Map keyed by the objects in the Collection with a
    *         corresponding value being an Integer representing the number of
    *         occurrences of that object within the Collection
    */
    public static Map convertCollectionToMap(Collection coll)
        {
        Map map = new HashMap();
        Integer ONE = new Integer(1);
        for (Iterator iter = coll.iterator(); iter.hasNext(); )
            {
            Object o = iter.next();
            Integer IPrev = (Integer) map.put(o, ONE);
            if (IPrev != null)
                {
                map.put(o, new Integer(IPrev.intValue() + 1));
                }
            }
        return map;
        }

    /**
    * Verify that the results of containsKey() is true for all objects in the
    * passed set of keys.
    *
    * @param map      a Map to test
    * @param setKeys  a Set containing Key objects
    */
    public static void assertMapContainsKeySetKeys(Map map, Set setKeys)
        {
        for (Iterator iter = setKeys.iterator(); iter.hasNext(); )
            {
            Object oKey = iter.next();
            if (!map.containsKey(oKey))
                {
                fail("containsKey() returned false for a key:"
                     + " Map class=" + map.getClass().getName()
                     + ", keySet class=" + setKeys.getClass().getName()
                     + ", key=" + oKey
                     + ", Map.get(key)=" + map.get(oKey));
                }
            }
        }

    /**
    * Verify that the results of containsValue() is true for all values in
    * the passed Collection of values.
    *
    * @param map         a Map to test
    * @param collValues  a Collection containing object values
    */
    public static void assertMapContainsValuesCollectionValues(Map map, Collection collValues)
        {
        for (Iterator iter = collValues.iterator(); iter.hasNext(); )
            {
            Object oValue = iter.next();
            if (!map.containsValue(oValue))
                {
                fail("containsValue() returned false for a value:"
                     + " Map class=" + map.getClass().getName()
                     + ", values collection class=" + collValues.getClass().getName()
                     + ", value=" + oValue);
                }
            }
        }

    /**
    * Verify that the results of get() match the values in the passed set of
    * entries.
    *
    * @param map         a Map to test
    * @param setEntries  a Set containing Entry objects
    */
    public static void assertGetsMatchEntries(Map map, Set setEntries)
        {
        for (Iterator iter = setEntries.iterator(); iter.hasNext(); )
            {
            Map.Entry entry = (Map.Entry) iter.next();
            Object    oKey  = entry.getKey();
            Object    oVal  = entry.getValue();
            Object    oGet  = map.get(oKey);
            if (!Base.equals(oVal, oGet))
                {
                fail("While comparing values from an entrySet to the values"
                     + " from the get method, a difference was found:"
                     + " Map class=" + map.getClass().getName()
                     + ", entrySet class=" + setEntries.getClass().getName()
                     + ", key=" + oKey
                     + ", entry.getValue()=" + oVal
                     + ", Map.get(key)=" + oGet);
                }
            }
        }

    /**
    * Compare two Map entrySet objects for equality.
    *
    * @param setEntries1  an entrySet, typically the control case
    * @param setEntries2  a second entrySet to compare to the first
    */
    public static void assertEntrySetsIdentical(Set setEntries1, Set setEntries2)
        {
        // first collect all of the entries by their keys, checking for
        // duplicates
        HashMap mapEntryByKey2 = new HashMap();
        for (Iterator iter = setEntries2.iterator(); iter.hasNext(); )
            {
            Map.Entry entry = new SimpleMapEntry((Map.Entry) iter.next());
            if (entry == null)
                {
                fail("While iterating the second Map's entrySet, the Iterator"
                     + " contained a null entry:"
                     + " entrySet class=" + setEntries2.getClass().getName()
                     + ", Iterator class=" + iter.getClass().getName());
                }
            else
                {
                Map.Entry entryPrev = (Map.Entry) mapEntryByKey2.put(entry.getKey(), entry);
                if (entryPrev != null)
                    {
                    fail("The second Map's entrySet Iterator contained duplicates:"
                         + "class=" + setEntries2.getClass().getName()
                         + ", first key=" + entryPrev.getKey()
                         + ", first entry=" + entryPrev
                         + ", second key=" + entry.getKey()
                         + ", second entry=" + entry);
                    }
                }
            }

        // next go through the control map's entries, comparing them
        // one-by-one with the comparison map's entries that we collected
        Object oDuplicate = new Object();
        for (Iterator iter = setEntries1.iterator(); iter.hasNext(); )
            {
            Map.Entry entry1 = new SimpleMapEntry((Map.Entry) iter.next());
            if (entry1 == null)
                {
                fail("While iterating the first Map's entrySet, the Iterator"
                     + " contained a null entry:"
                     + " entrySet class=" + setEntries1.getClass().getName()
                     + ", Iterator class=" + iter.getClass().getName());
                }
            else
                {
                Object    oKey   = entry1.getKey();
                Map.Entry entry2 = (Map.Entry) mapEntryByKey2.put(oKey, oDuplicate);
                if (entry2 == null)
                    {
                    fail("The second Map's entrySet is missing an Entry that"
                         + " is in the first Map's entrySet:"
                         + " key=" + oKey
                         + ", first Map's Entry=" + entry1);
                    }
                else if (entry2 == oDuplicate)
                    {
                    fail("The first Map's entrySet Iterator contained duplicates:"
                         + "class=" + setEntries1.getClass().getName()
                         + ", Iterator class=" + iter.getClass().getName()
                         + ", key=" + oKey
                         + ", entry=" + entry1);
                    }
                else
                    {
                    assertIdenticalEntries(entry1, entry2);
                    }
                }
            }

        // finally, verify that we went through all of the entries that we
        // collected
        for (Iterator iter = mapEntryByKey2.entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry entry = (Map.Entry) iter.next();
            if (entry.getValue() == oDuplicate)
                {
                iter.remove();
                }
            }
        if (!mapEntryByKey2.isEmpty())
            {
            fail("The second Map's entrySet contained entries that were not"
                 + " in the first Map:"
                 + " Set 1 class=" + setEntries1.getClass().getName()
                 + ", Set 2 class=" + setEntries2.getClass().getName()
                 + ", additional keys=" + mapEntryByKey2.keySet().toString());
            }
        }

    /**
    * Test two Map Entry objects for equality.
    *
    * @param entry1  the first Map Entry to compare
    * @param entry2  the second Map Entry to compare
    */
    public static void assertIdenticalEntries(Map.Entry entry1, Map.Entry entry2)
        {
        Object oKey1 = entry1.getKey();
        Object oKey2 = entry2.getKey();
        if (!Base.equals(oKey1, oKey2))
            {
            fail("Entry keys are different: key1=\"" + oKey1 + "\", key2=\""
                 + oKey2 + "\"");
            }

        Object oVal1 = entry1.getValue();
        Object oVal2 = entry2.getValue();
        if (!Base.equals(oVal1, oVal2))
            {
            fail("Entry values are different for key=\"" + oKey1
                 + "\": value1=\"" + oVal1 + "\", value2=\"" + oVal2 + "\"");
            }

        int nHash1 = entry1.hashCode();
        int nHash2 = entry2.hashCode();
        if (nHash1 != nHash2)
            {
            fail("Hash values are different for key=\"" + oKey1
                 + "\": hash1=\"" + nHash1 + "\", hash2=\"" + nHash2 + "\"");
            }

        boolean fEntry1Eq2 = entry1.equals(entry2);
        boolean fEntry2Eq1 = entry2.equals(entry1);
        if (!(fEntry1Eq2 && fEntry2Eq1))
            {
            fail("Entry is not equal for key=\"" + oKey1 + "\": 1.equals(2)="
                 + fEntry1Eq2 + ", 2.equals(1)=" + fEntry2Eq1);
            }
        }

    // ----- inner class: EventIgnorer --------------------------------------

    /**
    * A MapListener implementation that doesn't do anything.
    */
    public static class EventIgnorer
            implements MapListener
        {
        // ----- MapListener interface ----------------------------------

        /**
        * {@inheritDoc}
        */
        public void entryInserted(MapEvent evt)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void entryUpdated(MapEvent evt)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void entryDeleted(MapEvent evt)
            {
            }
        }


    // ----- inner class: EventPrinter --------------------------------------

    /**
    * A MapListener implementation that prints each event as it receives
    * them.
    */
    public static class EventPrinter
            implements MapListener
        {
        // ----- MapListener interface ----------------------------------

        /**
        * {@inheritDoc}
        */
        public void entryInserted(MapEvent evt)
            {
            System.out.println(evt);
            }

        /**
        * {@inheritDoc}
        */
        public void entryUpdated(MapEvent evt)
            {
            System.out.println(evt);
            }

        /**
        * {@inheritDoc}
        */
        public void entryDeleted(MapEvent evt)
            {
            System.out.println(evt);
            }
        }


    // ----- inner class: EventCounter --------------------------------------

    /**
    * A MapListener implementation that counts the number of each type of
    * event.
    */
    public static class EventCounter
            implements MapListener
        {
        // ----- MapListener interface ----------------------------------

        /**
        * {@inheritDoc}
        */
        public void entryInserted(MapEvent evt)
            {
            synchronized (LOCK_INSERT_COUNTER)
                {
                ++m_cInserts;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void entryUpdated(MapEvent evt)
            {
            synchronized (LOCK_UPDATE_COUNTER)
                {
                ++m_cUpdates;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void entryDeleted(MapEvent evt)
            {
            synchronized (LOCK_DELETE_COUNTER)
                {
                ++m_cDeletes;
                }
            }

        // ----- accessors ----------------------------------------------

        /**
        * Determine the total number of inserts that occurred.
        *
        * @return the insert counter.
        */
        public int getInsertedCount()
            {
            synchronized (LOCK_INSERT_COUNTER)
                {
                return m_cInserts;
                }
            }

        /**
        * Determine the total number of updates that occurred.
        *
        * @return the update counter.
        */
        public int getUpdatedCount()
            {
            synchronized (LOCK_UPDATE_COUNTER)
                {
                return m_cUpdates;
                }
            }

        /**
        * Determine the total number of deletes that occurred.
        *
        * @return the delete counter.
        */
        public int getDeletedCount()
            {
            synchronized (LOCK_DELETE_COUNTER)
                {
                return m_cDeletes;
                }
            }

        /**
        * Determine the total number of events that occurred.
        *
        * @return the sum of all events that were counted
        */
        public int getEventCount()
            {
            synchronized (LOCK_INSERT_COUNTER)
                {
                synchronized (LOCK_UPDATE_COUNTER)
                    {
                    synchronized (LOCK_DELETE_COUNTER)
                        {
                        return m_cInserts + m_cUpdates + m_cDeletes;
                        }
                    }
                }
            }

        /**
        * Reset the event counters.
        */
        public void reset()
            {
            synchronized (LOCK_INSERT_COUNTER)
                {
                synchronized (LOCK_UPDATE_COUNTER)
                    {
                    synchronized (LOCK_DELETE_COUNTER)
                        {
                        m_cInserts = 0;
                        m_cUpdates = 0;
                        m_cDeletes = 0;
                        }
                    }
                }
            }

        // ----- Object methods -----------------------------------------

        /**
        * Produce a human-readable description of this object.
        *
        * @return a human-readable String
        */
        public String toString()
            {
            return "EventCounter{"
                   + "InsertedCount=" + getInsertedCount()
                   + ", UpdatedCount=" + getUpdatedCount()
                   + ", DeletedCount=" + getDeletedCount()
                   + "}";
            }

        // ----- data members -------------------------------------------

        /**
        * Monitor for updating the insert counter.
        */
        private final Object LOCK_INSERT_COUNTER = new Object();
        /**
        * Monitor for updating the update counter.
        */
        private final Object LOCK_UPDATE_COUNTER = new Object();
        /**
        * Monitor for updating the delete counter.
        */
        private final Object LOCK_DELETE_COUNTER = new Object();

        /**
        * The insert counter.
        */
        private int m_cInserts;
        /**
        * The update counter.
        */
        private int m_cUpdates;
        /**
        * The delete counter.
        */
        private int m_cDeletes;
        }


    // ----- inner class: EventRecorder -------------------------------------

    /**
    * A MapListener implementation that records events in-order per-key.
    * SynchronousListener is used to gaurentee ordering, and ensure that
    * all events have been received before matching is performed.
    */
    public static class EventRecorder
            extends Base
            implements MapListenerSupport.SynchronousListener
        {
        // ----- MapListener interface ----------------------------------

        /**
        * {@inheritDoc}
        */
        public void entryInserted(MapEvent evt)
            {
            register(evt);
            }

        /**
        * {@inheritDoc}
        */
        public void entryUpdated(MapEvent evt)
            {
            register(evt);
            }

        /**
        * {@inheritDoc}
        */
        public void entryDeleted(MapEvent evt)
            {
            register(evt);
            }

        // ----- accessors ----------------------------------------------

        /**
        * Determine what keys have events recorded for them.
        *
        * @return an Iterator of keys that have had events recorded
        */
        public Iterator getEventKeys()
            {
            return m_mapEventLists.keySet().iterator();
            }

        /**
        * Determine what events have been recorded for a specific key.
        *
        * @param oKey  the key to obtain events for
        *
        * @return a List of events that have been recorded for the specified
        *         key
        */
        public List getEvents(Object oKey)
            {
            Map  map  = m_mapEventLists;
            List list = (List) map.get(oKey);
            return list == null ? Collections.EMPTY_LIST : list;
            }

        /**
        * Compare this EventRecorder to another EventRecorder.
        *
        * @param that  another EventRecorder
        *
        * @return null if the recorded events match, or the key where the
        *               first non-matching event was found
        */
        public Object matches(EventRecorder that)
            {
            if (this == that)
                {
                return null;
                }

            Map mapThis = this.m_mapEventLists;
            Map mapThat = that.m_mapEventLists;

            for (Iterator iterEvtsByKey = mapThis.entrySet().iterator();
                 iterEvtsByKey.hasNext(); )
                {
                Map.Entry entry    = (Map.Entry) iterEvtsByKey.next();
                Object    oKey     = entry.getKey();
                List      listThis = (List) entry.getValue();
                List      listThat = (List) mapThat.get(oKey);
                if (listThat == null || listThis.size() != listThat.size())
                    {
                    return oKey;
                    }

                for (Iterator iterThisEvts = listThis.iterator(),
                        iterThatEvts = listThat.iterator();
                     iterThisEvts.hasNext(); )
                    {
                    MapEvent evtThis = (MapEvent) iterThisEvts.next();
                    MapEvent evtThat = (MapEvent) iterThatEvts.next();
                    if (evtThis.getId() != evtThat.getId()
                        || !equals(evtThis.getKey(), evtThat.getKey())
                        || !equals(evtThis.getOldValue(), evtThat.getOldValue())
                        || !equals(evtThis.getNewValue(), evtThat.getNewValue()))
                        {
                        System.err.println("this != that at " + oKey);
                        return oKey;
                        }
                    }
                }

            if (mapThis.size() < mapThat.size())
                {
                // find the key in that which isn't in this
                for (Iterator iterEvtsByKey = mapThat.keySet().iterator();
                     iterEvtsByKey.hasNext(); )
                    {
                    Object oKey = iterEvtsByKey.next();
                    if (!mapThis.containsKey(oKey))
                        {
                        System.err.println("that > this at " + oKey);
                        return oKey;
                        }
                    }
                }


            return null;
            }

        /**
        * Reset the contents of the recorder.
        */
        public void reset()
            {
            m_mapEventLists.clear();
            }

        // ----- Object methods -----------------------------------------

        /**
        * Produce a human-readable description of this object.
        *
        * @return a human-readable String
        */
        public String toString()
            {
            StringBuffer sb = new StringBuffer();
            sb.append("EventRecorder{");
            for (Iterator iterKeys = getEventKeys(); iterKeys.hasNext(); )
                {
                Object oKey = iterKeys.next();
                sb.append("\n")
                  .append(oKey)
                  .append(": (")
                  .append(((List) m_mapEventLists.get(oKey)).size())
                  .append(" events)");
                for (Iterator iterEvts = getEvents(oKey).iterator(); iterEvts.hasNext(); )
                    {
                    sb.append("\n  ")
                      .append(iterEvts.next());
                    }
                }
            sb.append("\n}");
            return sb.toString();
            }

        // ----- internal helpers ---------------------------------------

        /**
        * Record an event.
        *
        * @param evt  the event that has been dispatched to this MapListener
        */
        protected void register(MapEvent evt)
            {
            // make a copy of the event if it isn't one of the known event
            // implementations (because the event may be providing
            // information from an underlying data structure, making it
            // incorrect later)
            Class clz = evt.getClass();
            if (!(clz == MapEvent.class || clz == CacheEvent.class))
                {
                evt = evt instanceof CacheEvent
                      ? new CacheEvent(evt.getMap(), evt.getId(), evt.getKey(),
                                       evt.getOldValue(), evt.getNewValue(),
                                       ((CacheEvent) evt).isSynthetic())
                      : new MapEvent(evt.getMap(), evt.getId(), evt.getKey(),
                                     evt.getOldValue(), evt.getNewValue());
                }

            Map    map  = m_mapEventLists;
            Object oKey = evt.getKey();

            List list = (List) map.get(oKey);
            if (list == null)
                {
                synchronized (map)
                    {
                    list = (List) map.get(oKey);
                    if (list == null)
                        {
                        list = new SafeLinkedList();
                        map.put(oKey, list);
                        }
                    }
                }
            list.add(evt);
            }

        // ----- data members -------------------------------------------

        /**
        * A Map keyed by the result of the MapEvent getKey() method for each
        * event that has been recorded, with a corresponding value of a List
        * that contains the events for that key, in the order received.
        */
        private Map m_mapEventLists = new SafeHashMap();
        }


    // ----- inner class: TestDaemon ------------------------------------

    /**
    * Start up a number of TestDaemons to run a test.
    *
    * @param aMap      an array of one or more Map objects to test
    * @param cKeys     the number of key objects that will be tested with
    * @param cThreads  the number of TestDaemon threads to start
    * @param cIters    the number of iterations for each thread to run
    * @param fLite     true to use ExternalizableLite values
    *
    * @throws IllegalStateException if a daemon test is already running on
    *         this MapTest instance
    * @throws RuntimeException if a daemon fails due to any Throwable object
    *         being raised, that Throwable object is re-thrown by this thread
    *         and wrapped as a RuntimeException if necessary
    */
    public synchronized void startTestDaemons(Map[] aMap, int cKeys,
            int cThreads, int cIters, boolean fLite)
        {
        TestDaemon[] aDaemons = m_aDaemons;
        if (aDaemons != null)
            {
            fail("a test is already running");
            }

        assertTrue(aMap != null && aMap.length > 0);
        assertTrue(cKeys > 0);
        assertTrue(cThreads > 0);
        assertTrue(cIters > 0);

        // create the spread of test keys
        Object[] aoKeys = new Object[cKeys];
        for (int i = 0; i < cKeys; ++i)
            {
            aoKeys[i] = new Integer(i);
            }

        // create the test daemon objects
        aDaemons = new TestDaemon[cThreads];
        for (int i = 0; i < cThreads; ++i)
            {
            aDaemons[i] = new TestDaemon(aMap, aoKeys, i, cIters, fLite);
            }

        // start the test threads
        m_aDaemons = aDaemons;
        for (int i = 0; i < cThreads; ++i)
            {
            aDaemons[i].start();
            }

        // wait for test completion
        try
            {
            Blocking.wait(this);
            }
        catch (InterruptedException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        // check for test failure
        Throwable e = m_eFailure;

        // reset test
        m_aDaemons = null;
        m_eFailure = null;

        if (e != null)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Request all running TestDaemons to stop running.
    */
    protected synchronized void stopTestDaemons()
        {
        TestDaemon[] aDaemons = m_aDaemons;
        if (aDaemons != null)
            {
            for (int i = 0, c = aDaemons.length; i < c; ++i)
                {
                TestDaemon daemon = aDaemons[i];
                if (daemon != null)
                    {
                    daemon.stop();
                    }
                }
            }
        }

    /**
    * Called by a TestDaemon when it has failed.
    *
    * @param daemon  the TestDaemon that has failed
    * @param e       the exception that occurred
    */
    protected synchronized void onTestDaemonFailure(TestDaemon daemon, Throwable e)
        {
        if (m_eFailure == null)
            {
            m_eFailure = e;
            stopTestDaemons();
            }
        }
    /**
    * Called by a TestDaemon when it has completed.
    *
    * @param daemon  the TestDaemon that has completed
    */
    protected synchronized void onTestDaemonCompletion(TestDaemon daemon)
        {
        TestDaemon[] aDaemons = m_aDaemons;
        if (aDaemons != null && daemon != null)
            {
            boolean fAllDone = true;
            for (int i = 0, c = aDaemons.length; i < c; ++i)
                {
                if (daemon == aDaemons[i])
                    {
                    aDaemons[i] = null;
                    }
                else if (aDaemons[i] != null)
                    {
                    fAllDone = false;
                    }
                }

            if (fAllDone)
                {
                notifyAll();
                }
            }
        }

    /**
    * Daemon that performs a sequence of random Map operations against one
    * or more Map objects.
    */
    public class TestDaemon
            extends Daemon
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a TestDaemon.
        *
        * @param nThread
        * @param cIters
        */
        public TestDaemon(Map[] aMaps, Object[] aoKeys, int nThread, int cIters, boolean fLite)
            {
            super("tester-" + nThread);

            m_aMaps   = aMaps;
            m_aoKeys  = aoKeys;
            m_cIters  = cIters;
            m_nThread = nThread;
            m_fLite   = fLite;
            }

        // ----- Daemon methods -----------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            synchronized(BaseMapTest.this)
                {
                // this will not complete until all the threads have been
                // told to start
                }

            try
                {
                Map[]    aMaps  = m_aMaps;
                int      cMaps  = aMaps.length;
                Object[] aoKeys = m_aoKeys;
                int      cKeys  = aoKeys.length;
                for (int iIter = 0, cIters = m_cIters; iIter < cIters && !isStopping(); ++iIter)
                    {
                    // pick a random test
                    switch (rnd(10))
                        {
                        case 0:
                            {
                            Object oKey = aoKeys[rnd(cKeys)];
                            synchronized (oKey)
                                {
                                boolean fVal = aMaps[0].containsKey(oKey);
                                for (int i = 1; i < cMaps; ++i)
                                    {
                                    boolean fCompare = aMaps[i].containsKey(oKey);
                                    if (fVal != fCompare)
                                        {
                                        fail("containsKey(" + oKey
                                             + ") test result differed with map #" + i
                                             + "\n -- control=" + fVal
                                             + "\n -- compare=" + fCompare);
                                        }
                                    }
                                }
                            }
                            break;

                        case 1:
                            {
                            Object oKey = aoKeys[rnd(cKeys)];
                            synchronized (oKey)
                                {
                                Object oVal = aMaps[0].get(oKey);
                                for (int i = 1; i < cMaps; ++i)
                                    {
                                    Object oCompare = aMaps[i].get(oKey);
                                    if (!equals(oVal, oCompare))
                                        {
                                        fail("get(" + oKey
                                             + ") test result differed with map #" + i
                                             + "\n -- control=" + oVal
                                             + "\n -- compare=" + oCompare);
                                        }
                                    }
                                }
                            }
                            break;

                        case 2:
                        case 3:
                        case 4:
                            {
                            Object oKey = aoKeys[rnd(cKeys)];
                            Object oNew = m_fLite ? new TestValueEL() : new TestValue();
                            synchronized (oKey)
                                {
                                Object oVal = aMaps[0].put(oKey, oNew);
                                for (int i = 1; i < cMaps; ++i)
                                    {
                                    Object oCompare = aMaps[i].put(oKey, oNew);
                                    if (!equals(oVal, oCompare))
                                        {
                                        fail("put(" + oKey
                                             + ") test result differed with map #" + i
                                             + "\n -- control=" + oVal
                                             + "\n -- compare=" + oCompare);
                                        }
                                    }
                                }
                            }
                            break;

                        case 5:
                            {
                            Object oKey = aoKeys[rnd(cKeys)];
                            synchronized (oKey)
                                {
                                Object oVal = aMaps[0].remove(oKey);
                                for (int i = 1; i < cMaps; ++i)
                                    {
                                    Object oCompare = aMaps[i].remove(oKey);
                                    if (!equals(oVal, oCompare))
                                        {
                                        fail("remove(" + oKey
                                             + ") test result differed with map #" + i
                                             + "\n -- control=" + oVal
                                             + "\n -- compare=" + oCompare);
                                        }
                                    }
                                }
                            }
                            break;

                        case 6:
                            {
                            Object oKey = aoKeys[rnd(cKeys)];
                            synchronized (oKey)
                                {
                                boolean fVal = aMaps[0].keySet().remove(oKey);
                                for (int i = 1; i < cMaps; ++i)
                                    {
                                    boolean fCompare = aMaps[i].keySet().remove(oKey);
                                    if (fVal != fCompare)
                                        {
                                        fail("keySet().remove(" + oKey
                                             + ") test result differed with map #" + i
                                             + "\n -- control=" + fVal
                                             + "\n -- compare=" + fCompare);
                                        }
                                    }
                                }
                            }
                            break;

                        case 7:
                            {
                            Object oKey = aoKeys[rnd(cKeys)];
                            synchronized (oKey)
                                {
                                Object[] aoVal = getAllSingle(aMaps[0], oKey);
                                for (int i = 1; i < cMaps; ++i)
                                    {
                                    Object[] aoCompare = getAllSingle(aMaps[i], oKey);
                                    if (!equalsDeep(aoVal, aoCompare))
                                        {
                                        fail("getAll(" + oKey
                                             + ") test result differed with map #" + i
                                             + "\n -- control=" + arrayToString(aoVal)
                                             + "\n -- compare=" + arrayToString(aoCompare));
                                        }
                                    }
                                }
                            }
                            break;

                        case 8:
                            {
                            Object oKey = aoKeys[rnd(cKeys)];
                            Object oVal = m_fLite ? new TestValueEL() : new TestValue();
                            Map    map  = new HashMap();
                            map.put(oKey, oVal);
                            synchronized (oKey)
                                {
                                aMaps[0].putAll(map);
                                for (int i = 1; i < cMaps; ++i)
                                    {
                                    aMaps[i].putAll(map);
                                    }
                                }
                            }
                            break;

                        case 9:
                            {
                            Object oKey = aoKeys[rnd(cKeys)];
                            Object oVal = m_fLite ? new TestValueEL() : new TestValue();
                            Map    map  = new ListMap(); // only dif from test#7
                            map.put(oKey, oVal);
                            synchronized (oKey)
                                {
                                aMaps[0].putAll(map);
                                for (int i = 1; i < cMaps; ++i)
                                    {
                                    aMaps[i].putAll(map);
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            catch (Throwable e)
                {
                registureFailure(e);
                }
            finally
                {
                registerCompletion();
                }
            }

        // ----- internal -----------------------------------------------

        /**
        * Register a failure of this TestDaemon.
        *
        * @param e  a Throwable object
        */
        protected void registureFailure(Throwable e)
            {
            BaseMapTest.this.onTestDaemonFailure(this, e);
            }

        /**
        * Register the completion of this TestDaemon.
        */
        protected void registerCompletion()
            {
            BaseMapTest.this.onTestDaemonCompletion(this);
            }

        /**
        * Format an array as a String.
        *
        * @param ao  the array to format
        *
        * @return the human-readable String value of the array
        */
        protected String arrayToString(Object[] ao)
            {
            if (ao == null)
                {
                return "null";
                }

            StringBuffer sb = new StringBuffer();

            int co = ao.length;
            sb.append("length=")
              .append(co);

            for (int i = 0; i < co; ++i)
                {
                sb.append(", [")
                  .append(i)
                  .append("]=")
                  .append(ao[i]);
                }

            return sb.toString();
            }

        // ----- data members -------------------------------------------

        /**
        * Maps to test. If there are more than one, then the first one is
        * assumed to be the control case.
        */
        Map[] m_aMaps;

        /**
        * Object keys to test against. Only these keys are tested.
        */
        Object[] m_aoKeys;

        /**
        * Number of test iterations to run.
        */
        int m_cIters;

        /**
        * The thread number for this daemon thread.
        */
        int m_nThread;

        /**
        * True to use ExternalizableLite values.
        */
        boolean m_fLite;
        }


    // ----- inner class: TestValue -----------------------------------------

    /**
    * A random and serializable value-object class.
    */
    public static class TestValue
            extends Base
            implements Serializable
        {
        // ----- constructors -------------------------------------------

        /**
        * Create a TestValue object that uses (on average) the default
        * average number of bytes.
        */
        public TestValue()
            {
            this(DEFAULT_AVG_SIZE);
            }

        /**
        * Create a TestValue object that uses approximately the specified
        * number of bytes.
        *
        * @param cb  the average number of bytes of memory that TestValue
        *            instances should use
        */
        public TestValue(int cb)
            {
            ab = new byte[s_rnd.nextInt(Math.max(0, (cb << 1) - ASSUMED_SIZE))];
            }

        // ----- Object methods -----------------------------------------

        /**
        * Produce a human-readable description of this object.
        *
        * @return a human-readable String
        */
        public String toString()
            {
            return "dfl=" + dfl + ", ab=" + ab.length + " bytes, s=" + s;
            }

        /**
        * Determine the hash code value for the object.
        *
        * @return a hash code value for this object
        */
        public int hashCode()
            {
            long l = Double.doubleToLongBits(dfl);
            return (int)(l ^ (l >>> 32)) ^ ab.length ^ s.hashCode();
            }

        /**
        * Compare this object to another for equality.
        *
        * @param o  another object
        *
        * @return true iff the other object is equal to this object
        */
        public boolean equals(Object o)
            {
            if (o instanceof TestValue)
                {
                TestValue that = (TestValue) o;
                return this == that
                       || this.dfl == that.dfl
                          && equalsDeep(this.ab, that.ab)
                          && equals(this.s, that.s);
                }
            return false;
            }

        // ----- constants ----------------------------------------------

        /**
        * Assumed (and approximated) overhead for the size of a TestValue:
        * <p/>
        * <ul>
        * <li>Overhead of the object itself: 8 bytes</li>
        * <li>Size for the double field: 8 bytes</li>
        * <li>Size for the byte array field: 4 bytes</li>
        * <li>Size for the byte array object: 8 bytes</li>
        * <li>Size for the byte array contents: variable</li>
        * <li>Size for the String field: 4 bytes</li>
        * <li>Size for the String object: 24 bytes including its fields</li>
        * <li>Size for the String's char[] object: 8 bytes</li>
        * <li>Size for the char[] contents: 28 chars or 56 bytes</li>
        * </ul>
        */
        public static final int ASSUMED_SIZE = 8 + 8 + 4 + 8 + 4 + 24 + 8 + 56;

        /**
        * Default average size for the TestValue objects
        */
        public static final int DEFAULT_AVG_SIZE = 256;

        // ----- data members -------------------------------------------

        /**
        * A random double.
        */
        protected double dfl = s_rnd.nextDouble();

        /**
        * A random-length byte array to take up some space.
        */
        protected byte[] ab;

        /**
        * A String that contains the date/time that the value was created.
        */
        protected String s = new Date().toString();
        }

    /**
    * A random and externalizable-lite value-object class.
    */
    public static class TestValueEL
            extends TestValue
            implements ExternalizableLite
        {
        // ----- constructors -------------------------------------------

        /**
        * Create a TestValue object that uses (on average) the default
        * average number of bytes.
        */
        public TestValueEL()
            {
            super();
            }

        /**
        * Create a TestValue object that uses approximately the specified
        * number of bytes.
        *
        * @param cb  the average number of bytes of memory that TestValue
        *            instances should use
        */
        public TestValueEL(int cb)
            {
            super(cb);
            }

        // ----- ExternalizableLite interface ---------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in)
                throws IOException
            {
            dfl = in.readDouble();
            ab  = ExternalizableHelper.readByteArray(in);
            s   = in.readUTF();
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            out.writeDouble(dfl);
            ExternalizableHelper.writeByteArray(out, ab);
            out.writeUTF(s);
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * An array of TestDaemon objects that are currently active.
    */
    private TestDaemon[] m_aDaemons;

    /**
    * The result of the currently running TestDaemon test.
    */
    private volatile Throwable m_eFailure;

    /**
    * Random number generator.
    */
    public static final Random s_rnd = new Random();
    }
