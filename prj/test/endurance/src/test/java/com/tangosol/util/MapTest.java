/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.OldCache;
import com.tangosol.net.cache.SerializationMap;
import com.tangosol.net.cache.WrapperNamedCache;
import com.tangosol.net.cache.SimpleSerializationMap;
import com.tangosol.net.cache.SerializationCache;
import com.tangosol.net.cache.SerializationPagedCache;

import org.junit.Test;

import java.util.Comparator;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;


/**
* A collection of tests designed to test Map implementations.
* <p/>
* A Map is not just a single abstract data structure, since it provides an
* additional three views of itself via the keySet(), entrySet() and values()
* methods, each of which returns a Collection. Further, each of those
* Collections can return an Iterator, and the Iterator over the Set returned
* by entrySet() provides Map.Entry objects. Each of these views is required
* to provide certain behavior based on the contents of the Map.
* <p/>
* <pre>
*   Map
*     size
*     isEmpty
*     containsKey
*     containsValue
*     get
*     put
*     remove
*     putAll
*     clear
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
*     add - not supported
*     remove
*     containsAll
*     addAll - not supported
*     removeAll
*     retainAll
*     clear
*     equals
*     hashCode
*
*   Iterator (values, keySet, entrySet)
*     hasNext
*     next
*     remove
*
*   Entry
*     getKey
*     getValue
*     setValue
*     equals
*     hashCode
* </pre>
* <p/>
* Fortunately, Java provides a number of "control cases", such as the HashMap
* and Hashtable implementations, that allow tests to be made to compare the
* results of querying two supposedly identical data structures, one of which
* is a control case. In this manner, any result that differs from the control
* case (i.e. violates the Map contract) is a test failure.
*
* @author cp  2005.12.25  based on OverflowMapTest
*/
public class MapTest
        extends BaseMapTest
    {
    // ----- unit tests for the test helpers themselves ---------------------

    /**
    * Invoke {@link #assertIdenticalMaps} with a Map and itself.
    */
    @Test
    public void testMapReflexively()
        {
        Map map = new HashMap();
        assertIdenticalMaps(map, map);

        map.put("a", new Integer(1));
        assertIdenticalMaps(map, map);

        map.put("b", new Integer(2));
        map.put("c", new Integer(3));
        assertIdenticalMaps(map, map);
        }

    /**
    * Invoke {@link #assertIdenticalMaps} with two identical Maps that are
    * known to work.
    */
    @Test
    public void testIdenticalMaps()
        {
        Map map1 = new HashMap();
        Map map2 = new Hashtable();
        assertIdenticalMaps(map1, map2);

        map1.put("a", new Integer(1));
        map2.put("a", new Integer(1));
        assertIdenticalMaps(map1, map2);

        map1.put("b", new Integer(2));
        map2.put("b", new Integer(2));
        map1.put("c", new Integer(3));
        map2.put("c", new Integer(3));
        assertIdenticalMaps(map1, map2);
        }

    /**
    * Invoke {@link #assertIdenticalMaps} with two identical Maps that are
    * known to work, but with a slight difference in their contents.
    */
    @Test
    public void testDifferentMaps()
        {
        Map map1 = new HashMap();
        Map map2 = new Hashtable();
        map1.put("a", new Integer(1));
        map2.put("a", new Integer(1));
        map1.put("b", new Integer(2));
        map2.put("b", new Integer(2));
        map1.put("c", new Integer(3));
        map2.put("c", new Integer(3));
        map1.put("d", new Integer(4));
        assertFalse(map1.size() == map2.size());
        }


    // ----- general tests for known Map implementations --------------------

    /**
    * Test the Map implementations that come with the JDK.
    */
    @Test
    public void testJavaMaps()
        {
        // use the test helpers against known working (JDK) implementations
        System.out.println("testing HashMap ...");
        testMap(new HashMap());
        System.out.println("testing HashMap versus Hashtable ...");
        testMaps(new HashMap(), new Hashtable());
        System.out.println("multi-threaded test against Hashtable ...");
        testMultithreadedMap(new Hashtable());

        Map javaConcurrentMap = getJavaConcurrentHashMap();
        if (javaConcurrentMap != null)
            {
            System.out.println("multi-threaded test of java.util.concurrent.ConcurrentHashMap");
            testMultithreadedMap(javaConcurrentMap);
            }
        else
            {
            System.out.println("java.util.concurrent.ConcurrentHashMap not supported");
            }
        }

    /**
    * Test SafeSortedMap.
    */
    @Test
    public void testSafeSortedMap()
        {
        Comparator comparator = new Comparator()
            {
            public int compare(Object o1, Object o2)
                {
                Class c1 = o1.getClass();
                Class c2 = o2.getClass();
                if (c1 != c2)
                    {
                    return c1.getName().compareTo(c2.getName());
                    }
                return ((Comparable) o1).compareTo(o2);
                }
            };
        System.out.println("testing SafeSortedMap ...");
        testMap(new SafeSortedMap(comparator));
        testMaps(new SafeSortedMap(comparator), new SafeSortedMap(comparator));
        testSortedMap(new TreeMap(), new SafeSortedMap());
        System.out.println("multi-threaded test of SafeSortedMap ...");
        testMultithreadedMap(new SafeSortedMap(comparator));
        }

    /**
    * Test SegmentedConcurrentMap.
    */
    @Test
    public void testSegmentedConcurrentMap()
        {
        System.out.println("testing SegmentedConcurrentMap ...");
        testMap(new SegmentedConcurrentMap());
        testMaps(new SegmentedConcurrentMap(), new SegmentedConcurrentMap());
        System.out.println("multi-threaded test of SegmentedConcurrentMap ...");
        testMultithreadedMap(new SegmentedConcurrentMap());
        System.out.println("testing SegmentedConcurrentMap as a ConcurrentMap ...");
        testConcurrentMap(new SegmentedConcurrentMap());
        }

    /**
    * Test SegmentedHashMap.
    */
    @Test
    public void testSegmentedHashMap()
        {
        System.out.println("testing SegmentedHashMap ...");
        testMap(new SegmentedHashMap());
        testMaps(new SegmentedHashMap(), new SegmentedHashMap());
        System.out.println("multi-threaded test of SegmentedHashMap ...");
        testMultithreadedMap(new SegmentedHashMap());
        }

    /**
    * Test SafeHashMap.
    */
    @Test
    public void testSafeHashMap()
        {
        // use the test helpers against various Tangosol implementations
        System.out.println("testing SafeHashMap ...");
        testMap(new SafeHashMap());
        testMaps(new SafeHashMap(), new SafeHashMap());
        testMultithreadedMap(new SafeHashMap());
        }

    /**
    * Test OldLiteMap.
    */
    @Test
    public void testOldLiteMap()
        {
        System.out.println("testing OldLiteMap ...");
        testMap(new OldLiteMap());
        testSinglethreadedMap(new OldLiteMap());
        }

    /**
    * Test LiteMap.
    */
    @Test
    public void testLiteMap()
        {
        System.out.println("testing LiteMap ...");
        testMap(new LiteMap());
        testSinglethreadedMap(new LiteMap());
        }

    /**
     * Test OpenHashMap.
     */
    @Test
    public void testOpenHashMap()
        {
        System.out.println("testing OpenHashMap ...");
        testMap(new OpenHashMap());
        testSinglethreadedMap(new OpenHashMap());
        }

    /**
    * Test ObservableHashMap.
    */
    @Test
    public void testObservableHashMap()
        {
        System.out.println("testing ObservableHashMap ...");
        testObservableMap(new ObservableHashMap());
        testMultithreadedMap(new ObservableHashMap());
        testTruncate(new ObservableHashMap());
        }

    /**
    * Test OldCache.
    */
    @Test
    public void testOldCache()
        {
        System.out.println("testing OldCache ...");
        testObservableMap(new OldCache(100000));
        testMultithreadedMap(new OldCache(100000));
        }

    /**
    * Test LocalCache.
    */
    @Test
    public void testLocalCache()
        {
        System.out.println("testing LocalCache ...");
        testObservableMap(new LocalCache(100000));
        testMultithreadedMap(new LocalCache(100000));
        }

    /**
    * Test WrapperObservableMap.
    */
    @Test
    public void testWrapperObservableMap()
        {
        System.out.println("testing WrapperObservableMap with SafeHashMap ...");
        testObservableMap(new WrapperObservableMap(new SafeHashMap()));
        testMultithreadedMap(new WrapperObservableMap(new SafeHashMap()));
        System.out.println("testing WrapperObservableMap with ObservableHashMap ...");
        testObservableMap(new WrapperObservableMap(new ObservableHashMap()));
        testMultithreadedMap(new WrapperObservableMap(new ObservableHashMap()));
        System.out.println("testing WrapperObservableMap with LocalCache ...");
        testObservableMap(new WrapperObservableMap(new LocalCache(100000)));
        testMultithreadedMap(new WrapperObservableMap(new LocalCache(100000)));
        }

    /**
    * Test WrapperConcurrentMap.
    */
    @Test
    public void testWrapperConcurrentMap()
        {
        System.out.println("testing WrapperConcurrentMap with SafeHashMap ...");
        testObservableMap(new WrapperConcurrentMap(new SafeHashMap(), false, -1));
        testMultithreadedMap(new WrapperConcurrentMap(new SafeHashMap(), false, -1));
        System.out.println("testing WrapperConcurrentMap with ObservableHashMap ...");
        testObservableMap(new WrapperConcurrentMap(new ObservableHashMap(), false, -1));
        testMultithreadedMap(new WrapperConcurrentMap(new ObservableHashMap(), false, -1));
        System.out.println("testing WrapperConcurrentMap [enforce=true] with SafeHashMap ...");
        testObservableMap(new WrapperConcurrentMap(new SafeHashMap(), true, 1000));
        testMultithreadedMap(new WrapperConcurrentMap(new SafeHashMap(), true, 1000));
        System.out.println("testing WrapperConcurrentMap [enforce=true] with ObservableHashMap ...");
        testObservableMap(new WrapperConcurrentMap(new ObservableHashMap(), true, 1000));
        testMultithreadedMap(new WrapperConcurrentMap(new ObservableHashMap(), true, 1000));
        System.out.println("testing WrapperConcurrentMap as a ConcurrentMap ...");
        testConcurrentMap(new WrapperConcurrentMap(new SafeHashMap(), false, -1));
        }

    /**
    * Test WrapperNamedCache.
    */
    @Test
    public void testWrapperNamedCache()
        {
        System.out.println("testing WrapperNamedCache with SafeHashMap ...");
        testObservableMap(new WrapperNamedCache(new SafeHashMap(), "test"));
        testMultithreadedMap(new WrapperNamedCache(new SafeHashMap(), "test"));
        System.out.println("testing WrapperNamedCache with ObservableHashMap ...");
        testObservableMap(new WrapperNamedCache(new ObservableHashMap(), "test"));
        testMultithreadedMap(new WrapperNamedCache(new ObservableHashMap(), "test"));
        }

    /**
    * Test SerializationMap.
    */
    @Test
    public void testSerializationMap()
        {
        System.out.println("testing SerializationMap with BinaryMap ...");
        testMap(new SerializationMap(instantiateTestBinaryStore()));
        testMultithreadedMap(new SerializationMap(instantiateTestBinaryStore()));
        }

    /**
    * Test SimpleSerializationMap.
    */
    @Test
    public void testSimpleSerializationMap()
        {
        System.out.println("testing SimpleSerializationMap with BinaryMap ...");
        testMap(new SimpleSerializationMap(instantiateTestBinaryStore()));
        testMultithreadedMap(new SimpleSerializationMap(instantiateTestBinaryStore()));
        }

    /**
    * Test SerializationCache.
    */
    @Test
    public void testSerializationCache()
        {
        System.out.println("testing SerializationCache with BinaryMap ...");
        testObservableMap(new SerializationCache(instantiateTestBinaryStore(), 10000));
        testMultithreadedMap(new SerializationCache(instantiateTestBinaryStore(), 10000));
        }

    /**
    * Test SerializationPagedCache.
    */
    @Test
    public void testSerializationPagedCache()
        {
        System.out.println("testing SerializationPagedCache with BinaryMap ...");
        testObservableMap(new SerializationPagedCache(instantiateTestBinaryStoreManager(), 10, 3600));
        testMultithreadedMap(new SerializationPagedCache(instantiateTestBinaryStoreManager(), 1000, 5));
        }




    // ----- regression tests -----------------------------------------------

    /**
    * Test for regression of COH-2515.
    */
    @Test
    public void testCoh2515()
        {
        SafeSortedMap map = new SafeSortedMap();
        for (int i = 1; i <= 101; i++)
            {
            map.put(Integer.valueOf(i), Integer.valueOf(i));
            }

        // this triggers COH-2515
        map.split(Integer.valueOf(0)).isHeadHeavy();
        }

    /**
    * Test for regression of COH-3646
    */
    @Test
    public void testCoh3646()
        {
        SafeSortedMap map = new SafeSortedMap();
        map.put("1","1");
        map.put("2","2");
        map.put("3","3");
        map.put("4","4");

        Iterator iter = map.values().iterator();

        // fetch first result
        Object oValue = iter.next();

        // prime the iterator to point to 2
        assertNotNull(iter.hasNext());

        map.remove("2");

        while (iter.hasNext())
            {
            assertNotNull(iter.next());
            }
        }

    /**
    * Test for regression of COH-3755.
    */
    @Test
    public void testCoh3755()
        {
        // Define a subclass of SafeSortedMap here to get access to the
        // findNearest() method
        class Coh3755SafeSortedMap
                extends SafeSortedMap
            {
            public Object findLT(Object o)
                {
                EntryNode node = findNearest(getTopNode(), o, SEARCH_LT, true);
                return node == null || node == getBaseNode() ? null : node.getValue();
                }

            public Object findLTEQ(Object o)
                {
                EntryNode node = findNearest(getTopNode(), o, SEARCH_LTEQ, true);
                return node == null || node == getBaseNode() ? null : node.getValue();
                }

            public Object findEQ(Object o)
                {
                EntryNode node = findNearest(getTopNode(), o, SEARCH_EQ, true);
                return node == null || node == getBaseNode() ? null : node.getValue();
                }

            public Object findGT(Object o)
                {
                EntryNode node = findNearest(getTopNode(), o, SEARCH_GT, true);
                return node == null || node == getBaseNode() ? null : node.getValue();
                }

            public Object findGTEQ(Object o)
                {
                EntryNode node = findNearest(getTopNode(), o, SEARCH_GTEQ, true);
                return node == null || node == getBaseNode() ? null : node.getValue();
                }
            }
        Coh3755SafeSortedMap map = new Coh3755SafeSortedMap();

        // test an empty map
        assertEquals(null, map.findLT(Integer.valueOf(0)));
        assertEquals(null, map.findLTEQ(Integer.valueOf(0)));
        assertEquals(null, map.findEQ(Integer.valueOf(0)));
        assertEquals(null, map.findGTEQ(Integer.valueOf(0)));
        assertEquals(null, map.findGT(Integer.valueOf(0)));

        // test a map with 1 entry
        map.put(Integer.valueOf(5), Integer.valueOf(5));

        assertEquals(null, map.findLT(Integer.valueOf(4)));
        assertEquals(null, map.findLT(Integer.valueOf(5)));
        assertEquals(Integer.valueOf(5), map.findLT(Integer.valueOf(6)));

        assertEquals(null, map.findLTEQ(Integer.valueOf(4)));
        assertEquals(Integer.valueOf(5), map.findLTEQ(Integer.valueOf(5)));
        assertEquals(Integer.valueOf(5), map.findLTEQ(Integer.valueOf(6)));

        assertEquals(null, map.findEQ(Integer.valueOf(4)));
        assertEquals(Integer.valueOf(5), map.findEQ(Integer.valueOf(5)));
        assertEquals(null, map.findEQ(Integer.valueOf(6)));

        assertEquals(Integer.valueOf(5), map.findGTEQ(Integer.valueOf(4)));
        assertEquals(Integer.valueOf(5), map.findGTEQ(Integer.valueOf(5)));
        assertEquals(null, map.findGTEQ(Integer.valueOf(6)));

        assertEquals(Integer.valueOf(5), map.findGT(Integer.valueOf(4)));
        assertEquals(null, map.findGT(Integer.valueOf(5)));
        assertEquals(null, map.findGTEQ(Integer.valueOf(6)));

        // test a map that has a bunch of data
        map.clear();
        int[] ai = new int[500];
        for (int i = 0; i < 500; i++)
            {
            ai[i] = 2 * i;
            }
        ai = Base.randomize(ai);

        for (int i = 0; i < 500; i++)
            {
            Integer IKey = Integer.valueOf(ai[i]);
            map.put(IKey, IKey);
            }

        // test LT
        assertEquals(Integer.valueOf(30), map.findLT(Integer.valueOf(32)));
        assertEquals(Integer.valueOf(30), map.findLT(Integer.valueOf(31)));
        assertEquals(null, map.findLT(Integer.valueOf(0)));
        assertEquals(null, map.findLT(Integer.valueOf(-1)));
        assertEquals(Integer.valueOf(996), map.findLT(Integer.valueOf(998)));
        assertEquals(Integer.valueOf(998), map.findLT(Integer.valueOf(1001)));

        // test LTEQ
        assertEquals(Integer.valueOf(32), map.findLTEQ(Integer.valueOf(32)));
        assertEquals(Integer.valueOf(30), map.findLTEQ(Integer.valueOf(31)));
        assertEquals(Integer.valueOf(0), map.findLTEQ(Integer.valueOf(0)));
        assertEquals(null, map.findLTEQ(Integer.valueOf(-1)));
        assertEquals(Integer.valueOf(998), map.findLTEQ(Integer.valueOf(998)));
        assertEquals(Integer.valueOf(998), map.findLTEQ(Integer.valueOf(1001)));

        // test EQ
        assertEquals(Integer.valueOf(32), map.findEQ(Integer.valueOf(32)));
        assertEquals(null, map.findEQ(Integer.valueOf(31)));
        assertEquals(Integer.valueOf(0), map.findEQ(Integer.valueOf(0)));
        assertEquals(null, map.findEQ(Integer.valueOf(-1)));
        assertEquals(Integer.valueOf(998), map.findEQ(Integer.valueOf(998)));
        assertEquals(null, map.findEQ(Integer.valueOf(1001)));

        // test GTEQ
        assertEquals(Integer.valueOf(32), map.findGTEQ(Integer.valueOf(32)));
        assertEquals(Integer.valueOf(32), map.findGTEQ(Integer.valueOf(31)));
        assertEquals(Integer.valueOf(0), map.findGTEQ(Integer.valueOf(0)));
        assertEquals(Integer.valueOf(0), map.findGTEQ(Integer.valueOf(-1)));
        assertEquals(Integer.valueOf(998), map.findGTEQ(Integer.valueOf(998)));
        assertEquals(null, map.findGTEQ(Integer.valueOf(1001)));

        // test GT
        assertEquals(Integer.valueOf(34), map.findGT(Integer.valueOf(32)));
        assertEquals(Integer.valueOf(32), map.findGT(Integer.valueOf(31)));
        assertEquals(Integer.valueOf(2), map.findGT(Integer.valueOf(0)));
        assertEquals(Integer.valueOf(0), map.findGT(Integer.valueOf(-1)));
        assertEquals(null, map.findGT(Integer.valueOf(998)));
        assertEquals(null, map.findGT(Integer.valueOf(1001)));
        }


    // ----- test helper methods  -------------------------------------------
    /**
    * Return an instance of java.util.concurrent.ConcurrentMap if the
    * platform supports it; null otherwise.
    */
    private static Map getJavaConcurrentHashMap()
        {
        Map map = null;
        try
            {
            Class clz = Class.forName("java.util.concurrent.ConcurrentHashMap");
            map = (Map) clz.newInstance();
            }
        catch (Exception e)
            {
            }
        return map;
        }

    }