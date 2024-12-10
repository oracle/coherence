/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.function.BiConsumer;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
* Unit test class for the {@link SafeSortedMap} class.
*
* @author  jf 2022/02/10
* @since 12.2.1.4.17
*/
public class SafeSortedMapTest
        extends Base
    {
    @BeforeClass
    public static void startup()
        {
        m_map = new SafeSortedMap();
        for (int i = 0; i < MEDIUM_NUMBER; i++)
            {
            Integer key   = i == 5 ? null : i;
            Integer value = i == 6 ? null : i + 1;
            m_map.put(key, value);
            }

        m_setMapExpectedKeys = new ImmutableArrayList(new Integer[]{0, 1, 2, 3, 4, null, 6, 7, 8, 9});
        m_listMapExpectedValues = new ImmutableArrayList(new Integer[]{1, 2, 3, 4, 5, 6, null, 8, 9, 10});

        m_mapSub  = m_map.subMap(3, true, 7, false);
        assertThat(m_mapSub.size(), is(3));

        m_setSubMapExpectedKeys = new ImmutableArrayList(new Integer[]{3, 4, 6});
        m_listSubMapExpectedValues = new ImmutableArrayList(new Integer[]{4, 5, null});

        // keys 6, 7, 8, 9 values null, 8, 9, 10
        m_mapTail = m_map.tailMap(5, true);
        assertThat(m_mapTail.size(), is(4));

        m_setTailMapExpectedKeys = new ImmutableArrayList(new Integer[]{ 6, 7, 8, 9});
        m_listTailMapExpectedValues = new ImmutableArrayList(new Integer[]{ null, 8, 9, 10});

        // keys null, 1, 2, ...6, values 1, 2, .., null
        m_mapHead = m_map.headMap( 6, true);
        assertThat(m_mapHead.size(), is(7));

        m_setHeadMapExpectedKeys = new ImmutableArrayList(new Integer[]{null, 0, 1, 2, 3, 4, 6});
        m_listHeadMapExpectedValues = new ImmutableArrayList(new Integer[]{6, 1, 2, 3, 4, 5, null});
        }

    @Test
    public void testEmptyMap()
        {
        SafeSortedMap map = new SafeSortedMap();

        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());
        assertTrue(map.entrySet().isEmpty());
        assertThat(map.getEntry(1), nullValue());
        assertThat(map.get(1), nullValue());
        }

    @Test
    public void testSimplePutWithNonNulls()
        {
        SafeSortedMap map = new SafeSortedMap();

        for (int i = 0; i < LARGE_NUMBER; i++)
            {
            Integer wrapper = i;
            map.put(wrapper, wrapper);
            }

        for (int i = 0; i < LARGE_NUMBER; i++)
            {
            Integer wrapper = i;
            assertTrue(equals(wrapper, map.get(wrapper)));
            }
        }

    @Test
    public void testSimplePutWithNullValues()
        {
        Map map = new SafeSortedMap();

        for (int i = 0; i < LARGE_NUMBER; i++)
            {
            Integer wrapper = i;
            map.put(wrapper, null);
            }

        assertTrue(map.containsValue(null));

        for (int i = 0; i < LARGE_NUMBER; i++)
            {
            Integer wrapper = i;
            assertTrue(map.containsKey(wrapper));
            assertNull(map.get(wrapper));
            }

        for (Map.Entry e : ((Map<?, ?>) map).entrySet())
            {
            assertThat(e.getValue(), nullValue());
            }
        }

    @Test
    public void testSimplePutWithNullKey()
        {
        Map map = new SafeSortedMap();

        for (int i = 0; i < LARGE_NUMBER; i++)
            {
            Integer wrapper = i;

            if (i == 0)
                {
                map.put(null, i + 1);
                }
            else
                {
                map.put(wrapper, i + 1);
                }
            }

        for (int i = 0; i < LARGE_NUMBER; i++)
            {
            Integer wrapper = i;
            Integer value   = i + 1;
            assertTrue(map.containsKey(i == 0 ? null : wrapper));
            assertThat("assert key for " + i + " is " + i + 1, map.get(i == 0 ? null : wrapper), is(value));
            }

        int i = 0;
        for (Map.Entry e : ((Map<?, ?>) map).entrySet())
            {

            Integer keyExpected   = i;
            Integer valueExpected = i + 1;

            if (i == 0)
                {
                assertThat(e.getKey(), nullValue());
                assertThat(e.getValue(), is(valueExpected));
                }
            else
                {
                assertThat(e.getKey(), is(keyExpected));
                assertThat(e.getValue(), is(valueExpected));
                }
            i++;
            }
        }

    @Test
    public void testGetEntryMissingKey()
        {
        SafeSortedMap map = new SafeSortedMap();

        Integer value = 1;
        map.put(null, value);

        assertNull(map.getEntry(4));
        }

    @Test
    public void testGetEntryWithNullKey()
        {
        SafeSortedMap map = new SafeSortedMap();

        Integer value = 1;
        map.put(null, value);

        Map.Entry entry = map.getEntry(null);
        assertThat(entry.getKey(), nullValue());
        assertThat(entry.getValue(), is(value));
        }

    @Test
    public void testGetEntryWithNullValue()
        {
        SafeSortedMap map = new SafeSortedMap();

        Integer key = 1;
        map.put(key, null);

        Map.Entry entry = map.getEntry(key);
        assertThat(entry.getKey(), is(key));
        assertThat(entry.getValue(), nullValue());
        }

    /**
     * Simulate SimpleMapIndexTest and ConditionalMapTest testInsert requirements for getEntry
     */
    @Test
    public void testGetEntryUpdates()
        {
        String oKey = "key";
        Integer oValue = 11;
        String oKey2 = "key2";
        Integer oValue2 = 11;

        // begin test
        SafeSortedMap mapIndex   = new SafeSortedMap();
        SafeSortedMap mapInverse = new SafeSortedMap();

        mapIndex.put(oKey, oValue);
        Set<String> set1 = new InflatableSet();
        set1.add(oKey);
        mapInverse.put(oValue, set1);

        mapIndex.put(oKey2, oValue2);

        Map.Entry<Integer, Set<String>> entry1 = mapInverse.getEntry(oValue2);

        assertNotNull(entry1);

        Set<String> set2 = entry1.getValue();

        assertThat(set2.size(), is(1));
        assertEquals(set1, set2);
        set2.add(oKey2); // update the entry value
        assertSame(entry1.getKey(), oValue);

        // verify the value from the index for key
        Object oIndexValue = mapIndex.get(oKey);

        assertEquals(
                "The index should contain value for key.",
                oValue, oIndexValue);

        // verify the value from the index for key2
        Object oIndexValue2 = mapIndex.get(oKey2);

        assertEquals(
                "The index should contain the extracted value for key.",
                oValue2, oIndexValue2);

        // get the entry from the inverse map keyed by the extracted value
        Map.Entry<Integer, Set<String>> inverseEntry = mapInverse.getEntry(oValue);

        assert inverseEntry != null;

        // verify that the key for the inverse map entry is the same as the
        // instance obtained through mapIndex.get(oKey)
        assertSame(oIndexValue, inverseEntry.getKey());

        // get the entry from the inverse map keyed by the extracted value
        Map.Entry<Integer, Set<String>> inverseEntry2 = mapInverse.getEntry(oValue2);

        assert inverseEntry2 != null;
        Map.Entry<Integer, Set<String>> inverseEntry1 = mapInverse.getEntry(oValue);
        assertSame(inverseEntry1.getKey(), inverseEntry2.getKey());
        assertSame(inverseEntry1.getValue(), inverseEntry2.getValue());

        // verify that the key for the inverse map entry is the same as the
        // instance obtained through mapIndex.get(oKey)
        assertSame(oIndexValue, inverseEntry2.getKey());

        // get the set of keys from the inverse map keyed by the extracted
        // value for key
        Set set = (Set) mapInverse.get(oIndexValue);

        // verify that the set of keys contains key
        assertTrue(
                "The index's inverse map should contain the key.",
                set.contains(oKey));

        // get the set of keys from the inverse map keyed by the extracted
        // value for key2
        set = (Set) mapInverse.get(oIndexValue2);

        // verify that the set of keys contains key2
        assertTrue(
                "The index's inverse map should contain the key2.",
                set.contains(oKey2));
        }

    @Test(expected = NotSerializableException.class)
    public void verifyNotSerializable() throws IOException
        {
        ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream());
        out.writeObject(new SafeSortedMap());
        }

    @Test(expected = NotSerializableException.class)
    public void verifyNULLNotSerializable() throws IOException
        {
        ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream());
        out.writeObject(SafeSortedMap.NULL);
        }

    @Test
    public void testMapForNULL()
        {
        validate(m_map, m_setMapExpectedKeys, m_listMapExpectedValues);
        validate(m_map.descendingMap(), m_setMapExpectedKeys, m_listMapExpectedValues);
        }

    @Test
    public void testSubMapForNULL()
        {
        validate(m_mapSub, m_setSubMapExpectedKeys, m_listSubMapExpectedValues);
        validate(m_mapSub.descendingMap(), m_setSubMapExpectedKeys, m_listSubMapExpectedValues);
        }

    @Test
    public void testHeadMapForNULL()
        {
        validate(m_mapHead, m_setHeadMapExpectedKeys, m_listHeadMapExpectedValues);
        validate(m_mapHead.descendingMap(), m_setHeadMapExpectedKeys, m_listHeadMapExpectedValues);
        }

    @Test
    public void testHeadMap2ForNULL()
        {
        validate(m_map.headMap(7), m_setHeadMapExpectedKeys, m_listHeadMapExpectedValues);
        validate(m_map.headMap(7).descendingMap(), m_setHeadMapExpectedKeys, m_listHeadMapExpectedValues);

        }

    @Test
    public void testTailMapForNULL()
        {
        validate(m_mapTail, m_setTailMapExpectedKeys, m_listTailMapExpectedValues);
        validate(m_mapTail.descendingMap(), m_setTailMapExpectedKeys, m_listTailMapExpectedValues);
        }

    @Test
    public void testTailMap2ForNULL()
        {
        validate(m_map.tailMap(5), m_setTailMapExpectedKeys, m_listTailMapExpectedValues);
        validate(m_map.tailMap(5).descendingMap(), m_setTailMapExpectedKeys, m_listTailMapExpectedValues);
        }


    // ----- Supported ConcurrentMap API -------------------------------------

    @Test
    public void testConcurrentMapPart1()
        {
        SafeSortedMap map = new SafeSortedMap();
        Object valuePrev = map.putIfAbsent("key", "value");
        assertThat(valuePrev, nullValue());
        assertThat(map.get("key"), is("value"));

        valuePrev = map.putIfAbsent("key", "newValue");
        assertThat(valuePrev, is("value"));
        assertThat(map.get("key"), is("value"));

        valuePrev = map.putIfAbsent("keyWithNullValue", null);
        assertThat(valuePrev, nullValue());
        valuePrev = map.putIfAbsent("keyWithNullValue", "aNonNullValue");
        assertThat(valuePrev, nullValue());

        assertTrue(map.replace("keyWithNullValue", null, "aNonNullValue"));
        assertThat(map.get("keyWithNullValue"), is("aNonNullValue"));

        assertThat(map.replace("key", null), is("value"));
        }

    @Test
    public void testForEachAndGetOrDefault()
        {
        final List<Integer>             listKey    = new LinkedList<>();
        final List<Object>              listValues = new LinkedList<>();
        BiConsumer<Integer, Object>     fnBuild    = (k,v) -> { listKey.add(k); listValues.add(v);};
        SafeSortedMap map = new SafeSortedMap();

        for (int i=0; i < 10; i++)
            {
            map.put(i, i == 5 ? null : i+1);
            }

        map.forEach(fnBuild);

        BiConsumer<Integer, Object> fnValidate = (k,v) ->
            {
            assertThat(map.get(k), k == 5 ? nullValue() : is(k + 1));
            assertThat(listKey.contains(k), is(true));
            assertThat(listValues.contains(v), is(true));
            assertThat(map.getOrDefault(20 + k, -1), is(-1));
            assertThat(map.getOrDefault(k, -1), k == 5 ? nullValue() : is(k+1));
            };

        map.forEach(fnValidate);

        assertThat(map.getOrDefault(22, null), nullValue());
        assertThat(map.getOrDefault(22, 49), is(49));
        }

    @Test
    public void testReplace()
        {
        SafeSortedMap map = new SafeSortedMap();
        for (int i=0; i < 10; i++)
            {
            map.put(i, i == 5 ? null : i+1);
            }
        for (int i=0; i < 10; i++)
            {
            boolean fReplaced = map.replace(i, i == 5 ?  null : i + 1,  i == 5 ? i + 1 : i + 2);
            assertTrue(fReplaced);
            assertThat(map.replace(i, i+1, i+2), i == 5 ? is(true) : is(false));
            }
        assertThat(map.replace(20, 4), nullValue());
        }

    // ----- helpers ---------------------------------------------------------

    public void validate(ConcurrentNavigableMap map, Set setKeys, List listValues)
        {
        for (Map.Entry e : ((Map<?, ?>) map).entrySet())
            {
            assertTrue("validate key " + e.getKey(), setKeys.contains(e.getKey()));
            assertTrue("validate value " + e.getValue(),  listValues.contains(e.getValue()));
            }
        for (Object value : map.values())
            {
            assertTrue("validate value " + value, listValues.contains(value));
            }
        for (Object key : map.keySet())
            {
            assertTrue("validate key " + key, setKeys.contains(key));
            if (map instanceof SafeSortedMap)
                {
                SafeSortedMap mapSafe = (SafeSortedMap) map;
                assertThat(mapSafe.getEntry(key).getValue(), is(mapSafe.get(key)));
                }
            }
        for (Object key : map.descendingKeySet())
            {
            assertTrue("validate key " + key, setKeys.contains(key));
            }

        for (Object key : map.keySet())
            {
            assertThat(map.firstKey(), is(key));
            break;
            }
        for (Object key : map.descendingKeySet())
            {
            assertThat(map.lastKey(), is(key));
            break;
            }

        assertTrue(map.values().contains(null));
        assertThat(map.keySet().contains(null), is(setKeys.contains(null)));
        assertThat(map.descendingKeySet().contains(null), is(setKeys.contains(null)));
        assertThat(map.containsKey(null), is(setKeys.contains(null)));
        assertTrue(map.containsValue(null));

        assertFalse(new ArrayList(Arrays.asList(map.values().toArray())).contains(SafeSortedMap.NULL));
        assertFalse(new ArrayList(Arrays.asList(map.keySet().toArray())).contains(SafeSortedMap.NULL));
        assertTrue(new ArrayList(Arrays.asList(map.values().toArray())).contains(null));
        assertThat(new ArrayList(Arrays.asList(map.keySet().toArray())).contains(null), is(setKeys.contains(null)));
        }

    // ----- constants -------------------------------------------------------

    private static final int MEDIUM_NUMBER = 10;

    private static final int LARGE_NUMBER = 1000;

    // ----- data members ----------------------------------------------------

    static public SafeSortedMap m_map;

    static public ConcurrentNavigableMap  m_mapSub;

    static public ConcurrentNavigableMap m_mapHead;

    static public ConcurrentNavigableMap m_mapTail;

    static Set m_setMapExpectedKeys;
    static List m_listMapExpectedValues;

    static Set m_setSubMapExpectedKeys;
    static List m_listSubMapExpectedValues;

    static Set m_setTailMapExpectedKeys;
    static List m_listTailMapExpectedValues;

    static Set m_setHeadMapExpectedKeys;
    static List m_listHeadMapExpectedValues;
    }
