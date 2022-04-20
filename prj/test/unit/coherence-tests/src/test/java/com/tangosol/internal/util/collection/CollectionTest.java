/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util.collection;

import com.tangosol.io.Serializer;

import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests that test the portable collections.
 *
 * @author lh 2015.04.27
 */
public class CollectionTest
    {
    @BeforeClass
    public static void init()
        {
        SimplePofContext context = new SimplePofContext();
        context.registerUserType(1001, PortableCollection.class, new PortableObjectSerializer(1001));
        context.registerUserType(1002, PortableList.class, new PortableObjectSerializer(1002));
        context.registerUserType(1003, PortableMap.class, new PortableObjectSerializer(1003));
        context.registerUserType(1004, PortableSet.class, new PortableObjectSerializer(1004));
        context.registerUserType(1005, PortableConcurrentMap.class, new PortableObjectSerializer(1005));
        context.registerUserType(1006, PortableSortedMap.class, new PortableObjectSerializer(1006));
        context.registerUserType(1007, PortableSortedSet.class, new PortableObjectSerializer(1007));

        s_serializer = context;
        }

    @Test
    public void testPortableCollection()
        {
        PortableCollection<String> col1 = new PortableCollection<String>();
        col1.add("test");
        col1.add("test3");
        col1.add("testPOF1");
        assertEquals(3, col1.size());

        PortableCollection<Integer> col2 = new PortableCollection<Integer>();
        col2.add(1);
        col2.add(2);
        col2.add(3);
        assertEquals(3, col2.size());

        PortableCollection<Integer> col3 = new PortableCollection<Integer>();

        init();
        verifyPofSerialization(col1);
        verifyPofSerialization(col2);
        verifyPofSerialization(col3);
        }

    @Test
    public void testPortableList()
        {
        PortableList<String> list = new PortableList<String>();
        list.add("test");
        list.add("test3");
        list.add("testPOF1");
        assertEquals(3, list.size());

        PortableList<Integer> list2 = new PortableList<Integer>();
        list2.add(1);
        list2.add(2);
        list2.add(3);
        list2.add(2, 10);
        assertEquals(2, list2.indexOf(10));

        PortableList<Double> list3 = new PortableList<Double>();

        init();
        verifyPofSerialization(list);
        verifyPofSerialization(list2);
        verifyPofSerialization(list3);
        }

    @Test
    public void testPortableMap()
        {
        PortableMap<String, Double> map = new PortableMap<String, Double>();
        map.put("A", 11.11);
        map.put("Z", 88.88);
        map.put("7", 100.1);
        assertEquals(3, map.size());

        PortableMap<String, Double> map2 = new PortableMap<String, Double>();

        init();
        verifyPofSerialization(map);
        verifyPofSerialization(map2);
        }

    @Test
    public void testPortableConcurrentMap()
        {
        PortableConcurrentMap<String, Double> map = new PortableConcurrentMap<String, Double>();
        String sKey = "Z";
        map.put("A", 11.11);
        map.put(sKey, 88.88);
        map.put("7", 100.1);
        map.putIfAbsent("Newly Added", 123.321);
        assertEquals(4, map.size());
        assertTrue(map.containsKey("Newly Added"));

        Double dValue = 10.0;
        dValue = map.getOrDefault(sKey, dValue);
        assertEquals((Double) 88.88, dValue);

        PortableConcurrentMap<String, Double> map2 = new PortableConcurrentMap<String, Double>();

        init();
        verifyPofSerialization(map);
        verifyPofSerialization(map2);
        }

    @Test
    public void testPortableSortedMap()
        {
        PortableSortedMap<String, Double> map = new PortableSortedMap<String, Double>();
        String sKey = "Z";
        map.put("A", 11.11);
        map.put(sKey, 88.88);
        map.put("7", 100.1);
        map.putIfAbsent("Newly Added", 123.321);
        assertEquals(4, map.size());
        assertTrue(map.containsKey("Newly Added"));

        Double dValue = 10.0;
        dValue = map.getOrDefault(sKey, dValue);
        assertEquals((Double) 88.88, dValue);

        PortableSortedMap<String, Double> map2 = new PortableSortedMap<String, Double>();

        init();
        verifyPofSerialization(map);
        verifyPofSerialization(map2);
        }

    @Test
    public void testPortableSet()
        {
        PortableSet<String> set = new PortableSet<String>();
        set.add("A");
        set.add("AB");
        set.add("ABC");
        set.add("ABCD");
        set.add("ABCDE");
        assertEquals(5, set.size());

        PortableSet<Double> set2 = new PortableSet<Double>();

        init();
        verifyPofSerialization(set);
        verifyPofSerialization(set2);
        }

    @Test
    public void testPortableSortedSet()
        {
        PortableSortedSet<String> set = new PortableSortedSet<String>();
        set.add("A");
        set.add("ABCDE");
        set.add("AB");
        set.add("ABCD");
        set.add("ABC");
        assertEquals(5, set.size());

        PortableSortedSet<Integer> set2 = new PortableSortedSet<Integer>();

        init();
        verifyPofSerialization(set);
        verifyPofSerialization(set2);
        }

    private void verifyPofSerialization(Object testObject)
        {
        Binary binary = ExternalizableHelper.toBinary(testObject, s_serializer);
        Object obj    = ExternalizableHelper.fromBinary(binary, s_serializer);

        assertEquals(testObject, obj);

        binary = ExternalizableHelper.toBinary(testObject);
        obj    = ExternalizableHelper.fromBinary(binary);

        assertEquals(testObject, obj);
        }

    // ----- data members ---------------------------------------------------

    private static Serializer s_serializer;
    }
