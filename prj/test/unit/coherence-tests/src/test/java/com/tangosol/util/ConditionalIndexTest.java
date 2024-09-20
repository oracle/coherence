/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.common.collections.NullableSortedMap;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.LessFilter;

import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
* ConditionalIndex unit tests
*
* @author tb 2010.2.08
*/
public class ConditionalIndexTest
    {
    /**
    * Test getValueExtractor
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testGetValueExtractor() throws Exception
        {
        ValueExtractor extractor = new IdentityExtractor();
        Filter         filter    = new GreaterFilter(extractor, 5);

        ConditionalIndex index   = new ConditionalIndex(filter, extractor, false, null, true, null);

        assertEquals(extractor, index.getValueExtractor());
        }

    /**
    * Test getFilter
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testGetFilter() throws Exception
        {
        ValueExtractor extractor = new IdentityExtractor();
        Filter         filter    = new GreaterFilter(extractor, 5);

        ConditionalIndex index   = new ConditionalIndex(filter, extractor, false, null, true, null);

        assertEquals(filter, index.getFilter());
        }


    /**
    * Test isOrdered.
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testIsOrdered() throws Exception
        {
        ValueExtractor extractor = new IdentityExtractor();
        Filter         filter    = new GreaterFilter(extractor, 5);

        ConditionalIndex index   = new ConditionalIndex(filter, extractor, false, null, true, null);

        assertFalse(index.isOrdered());

        index   = new ConditionalIndex(filter, extractor, true, null, true, null);

        assertTrue(index.isOrdered());
        }

    /**
    * Test isPartial.
    */
    @Test
    public void testIsPartial()
        {
        ValueExtractor extractor = new IdentityExtractor();
        Filter         filter    = new GreaterFilter(extractor, 5);

        ConditionalIndex index   = new ConditionalIndex(filter, extractor, false, null, true, null);

        assertFalse(index.isPartial());

        index.insert(new SimpleMapEntry("key1", 6));

        assertFalse(index.isPartial());

        index.insert(new SimpleMapEntry("key2", 4));

        assertTrue(index.isPartial());

        index   = new ConditionalIndex(filter, extractor, false, null, true, null);

        assertFalse(index.isPartial());

        index.insert(new SimpleMapEntry("key1", 6));

        assertFalse(index.isPartial());

        index.update(new SimpleMapEntry("key1", 4));

        assertTrue(index.isPartial());
        }

    /**
    * Test getIndexContents.
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testGetIndexContents() throws Exception
        {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("one",         1);
        map.put("another_one", 1);
        map.put("one_more",    1);
        map.put("two",         2);
        map.put("three",       3);
        map.put("four",        4);
        map.put("four_again",  4);
        map.put("five",        5);
        map.put("five_a",      5);
        map.put("five_b",      5);
        map.put("five_c",      5);
        map.put("five_d",      5);

        ValueExtractor   extractor     = new IdentityExtractor();
        Filter           filter        = new LessFilter(extractor, 5);
        ConditionalIndex index         = createIndex(map, filter, extractor, true);
        Map              indexContents = index.getIndexContents();

        Set setOne = (Set) indexContents.get(1);
        assertEquals(3, setOne.size());
        assertTrue(setOne.contains("one"));
        assertTrue(setOne.contains("another_one"));
        assertTrue(setOne.contains("one_more"));

        Set setTwo = (Set) indexContents.get(2);
        assertEquals(1, setTwo.size());
        assertTrue(setTwo.contains("two"));

        Set setThree = (Set) indexContents.get(3);
        assertEquals(1, setThree.size());
        assertTrue(setThree.contains("three"));

        Set setFour = (Set) indexContents.get(4);
        assertEquals(2, setFour.size());
        assertTrue(setFour.contains("four"));
        assertTrue(setFour.contains("four_again"));

        Set setFive = (Set) indexContents.get(5);
        assertNull(setFive);
        }

    /**
    * Test get.
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testGet() throws Exception
        {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("one",   1);
        map.put("two",   2);
        map.put("three", 3);
        map.put("four",  4);
        map.put("five",  5);

        ValueExtractor   extractor = new IdentityExtractor();
        Filter           filter    = new LessFilter(extractor, 5);
        ConditionalIndex index     = createIndex(map, filter, extractor, true);

        assertEquals(1, index.get("one"));
        assertEquals(2, index.get("two"));
        assertEquals(3, index.get("three"));
        assertEquals(4, index.get("four"));
        assertEquals(MapIndex.NO_VALUE, index.get("five"));

        // forward map support == false
        index = createIndex(map, filter, extractor, false);

        assertEquals(MapIndex.NO_VALUE, index.get("one"));
        assertEquals(MapIndex.NO_VALUE, index.get("two"));
        assertEquals(MapIndex.NO_VALUE, index.get("three"));
        assertEquals(MapIndex.NO_VALUE, index.get("four"));
        assertEquals(MapIndex.NO_VALUE, index.get("five"));
        }

    /**
    * Test insert into a ConditionalIndex.  Verify the following :
    * 1) the index contains a value for a key after an insert
    * 2) if multiple equivalent values are inserted into the index
    *    for different keys, verify that only one copy of the value exists in
    *    the index
    * 3) extracted values from entries that do not pass the filter test are
    *    not added to the index
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testInsert() throws Exception
        {
        // create the mocks
        Map.Entry      entry     = mock(Map.Entry.class);
        Map.Entry      entry2    = mock(Map.Entry.class);
        Map.Entry      entry3    = mock(Map.Entry.class);
        ValueExtractor extractor = mock(ValueExtractor.class);

        // create the ConditionalIndex to be tested
        Filter           filter   = new LessFilter(extractor, 15);
        ConditionalIndex mapIndex = new ConditionalIndex(filter, extractor, true, null, true, null);

        // define the keys and values for the mock entries
        Object oKey        = "key";
        Object oValue      = 1;
        Object oExtracted  = 11;
        Object oKey2       = "key2";
        Object oValue2     = 2;
        Object oExtracted2 = 11;
        Object oKey3       = "key3";
        Object oValue3     = 25;

        // set mock expectations
        when(entry.getKey()).thenReturn(oKey);
        when(entry.getValue()).thenReturn(oValue);

        when(entry2.getKey()).thenReturn(oKey2);
        when(entry2.getValue()).thenReturn(oValue2);
        when(extractor.extract(oValue)).thenReturn(oExtracted);
        when(extractor.extract(oValue2)).thenReturn(oExtracted2);

        when(entry3.getValue()).thenReturn(oValue3);

        // begin test

        // verify that the index does not contain a value for the tested keys
        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey));

        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey2));

        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey3));

        // assert the inverse map does not contain an entry for the extracted values
        assertFalse(mapIndex.getIndexContents().containsKey(oExtracted));
        assertFalse(mapIndex.getIndexContents().containsKey(oExtracted2));

        // insert into the index
        mapIndex.insert(entry);
        mapIndex.insert(entry2);
        mapIndex.insert(entry3);

        // verify the value from the index for key
        Object oIndexValue = mapIndex.get(oKey);
        assertEquals("The index should contain the extracted value for key.",
                oExtracted, oIndexValue);

        // verify the value from the index for key2
        Object oIndexValue2 = mapIndex.get(oKey2);
        assertEquals("The index should contain the extracted value for key.",
                oExtracted2, oIndexValue2);

        // verify no value in the index for key3
        Object oIndexValue3 = mapIndex.get(oKey3);
        assertEquals(MapIndex.NO_VALUE, oIndexValue3);

        // verify that the value for key and key2 is the same instance
        assertSame("The value for key and key2 should be the same instance.",
                oIndexValue, oIndexValue2);

        // get the inverse map
        NullableSortedMap mapInverse = (NullableSortedMap)mapIndex.
                getIndexContents();

        // get the entry from the inverse map keyed by the extracted value
        Map.Entry inverseEntry = mapInverse.getEntry(oExtracted);

        assert inverseEntry != null;

        // verify that the key for the inverse map entry is the same as the
        // instance obtained through mapIndex.get(oKey)
        assertSame(oIndexValue, inverseEntry.getKey());

        // get the entry from the inverse map keyed by the extracted value
        Map.Entry inverseEntry2 = mapInverse.getEntry(oExtracted2);

        assert inverseEntry2 != null;

        // verify that the key for the inverse map entry is the same as the
        // instance obtained through mapIndex.get(oKey)
        assertSame(oIndexValue2, inverseEntry2.getKey());

        // get the set of keys from the inverse map keyed by the extracted
        // value for key
        Set set = (Set) mapInverse.get(oIndexValue);

        // verify that the set of keys contains key
        assertTrue("The index's inverse map should contain the key.",
                set.contains(oKey));

        // get the set of keys from the inverse map keyed by the extracted
        // value for key2
        set = (Set) mapInverse.get(oIndexValue2);

        // verify that the set of keys contains key2
        assertTrue("The index's inverse map should contain the key2.",
                set.contains(oKey2));
        }

    /**
    * Test insert into a ConditionalIndex.  Verify the following :
    * 1) the index contains a value for a key after an insert
    * 2) if multiple equivalent values are inserted into the index
    *    for different keys, verify that only one copy of the value exists in
    *    the index
    * 3) extracted values from entries that do not pass the filter test are
    *    not added to the index
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testInsert_forwardIndexFalse() throws Exception
        {
        // create the mocks
        MapTrigger.Entry entry     = mock(MapTrigger.Entry.class);
        MapTrigger.Entry entry2    = mock(MapTrigger.Entry.class);
        MapTrigger.Entry entry3    = mock(MapTrigger.Entry.class);
        ValueExtractor   extractor = mock(ValueExtractor.class);

        // create the ConditionalIndex to be tested
        Filter           filter   = new LessFilter(extractor, 15);
        ConditionalIndex mapIndex = new ConditionalIndex(filter, extractor, true, null, false, null);

        // define the keys and values for the mock entries
        Object oKey        = "key";
        Object oValue      = 1;
        Object oExtracted  = 11;
        Object oKey2       = "key2";
        Object oValue2     = 2;
        Object oExtracted2 = 11;
        Object oKey3       = "key3";
        Object oValue3     = 25;

        // set mock expectations
        when(entry.getKey()).thenReturn(oKey);
        when(entry.getValue()).thenReturn(oValue);

        when(entry2.getKey()).thenReturn(oKey2);
        when(entry2.getValue()).thenReturn(oValue2);

        when(extractor.extract(oValue)).thenReturn(oExtracted);
        when(extractor.extract(oValue2)).thenReturn(oExtracted2);
        when(entry.extract(extractor)).thenReturn(oExtracted);
        when(entry2.extract(extractor)).thenReturn(oExtracted2);

        when(entry3.getValue()).thenReturn(oValue3);

        // begin test

        // verify that the index does not contain a value for the tested keys
        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey));

        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey2));

        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey3));

        // insert into the index
        mapIndex.insert(entry);
        mapIndex.insert(entry2);
        mapIndex.insert(entry3);

        // all gets should return NO_VALUE since forward index is not supported
        // verify no value from the index for key
        Object oIndexValue = mapIndex.get(oKey);
        assertEquals(MapIndex.NO_VALUE, oIndexValue);

        // verify no value from the index for key2
        Object oIndexValue2 = mapIndex.get(oKey2);
        assertEquals(MapIndex.NO_VALUE, oIndexValue2);

        // verify no value in the index for key3
        Object oIndexValue3 = mapIndex.get(oKey3);
        assertEquals(MapIndex.NO_VALUE, oIndexValue3);

        // verify that the value for key and key2 is the same instance
        assertSame("The value for key and key2 should be the same instance.",
                oIndexValue, oIndexValue2);

        // get the inverse map
        NullableSortedMap mapInverse = (NullableSortedMap) mapIndex.
                getIndexContents();

        // get the set of keys from the inverse map keyed by the extracted
        // value for key
        Set set = (Set) mapInverse.get(oExtracted);

        // verify that the set of keys contains key
        assertTrue("The index's inverse map should contain the key.",
                set.contains(oKey));

        // get the set of keys from the inverse map keyed by the extracted
        // value for key2
        set = (Set) mapInverse.get(oExtracted2);

        // verify that the set of keys contains key2
        assertTrue("The index's inverse map should contain the key2.",
                set.contains(oKey2));
        }

    /**
    * Test update on a ConditionalIndex.  Verify the following :
    * 1) the index contains the new value for a key after an update
    * 2) if multiple equivalent values are inserted into the index
    *    for different keys, verify that only one copy of the value exists in
    *    the index
    * 3) extracted values from entries that do not pass the filter test are
    *    not added to the index
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testUpdate() throws Exception
        {
        // create the mocks
        Map.Entry      entry     = mock(Map.Entry.class);
        Map.Entry      entry2    = mock(Map.Entry.class);
        Map.Entry      entry3    = mock(Map.Entry.class);
        Map.Entry      entryNew  = mock(Map.Entry.class);
        Map.Entry      entryNew2 = mock(Map.Entry.class);
        ValueExtractor extractor = mock(ValueExtractor.class);

        // create the ConditionalIndex to be tested
        Filter           filter   = new LessFilter(extractor, 15);
        ConditionalIndex mapIndex = new ConditionalIndex(filter, extractor, true, null, true, null);

        // define the keys and values for the mock entries
        Object oKey           = "key";
        Object oValue         = 0;
        Object oExtracted     = 10;
        Object oNewValue      = 1;
        Object oExtractedNew  = 11;
        Object oKey2          = "key2";
        Object oValue2        = 2;
        Object oExtracted2    = 11;
        Object oKey3          = "key3";
        Object oValue3        = 3;
        Object oExtracted3    = 21;
        Object oNewValue2     = 4;
        Object oExtractedNew2 = 30;

        // set mock expectations
        when(entry.getKey()).thenReturn(oKey);
        when(entry.getValue()).thenReturn(oValue);

        when(entryNew.getKey()).thenReturn(oKey);
        when(entryNew.getValue()).thenReturn(oNewValue);

        when(entry2.getKey()).thenReturn(oKey2);
        when(entry2.getValue()).thenReturn(oValue2);

        when(entryNew2.getKey()).thenReturn(oKey2);
        when(entryNew2.getValue()).thenReturn(oNewValue2);

        when(entry3.getValue()).thenReturn(oValue3);

        when(extractor.extract(oValue)).thenReturn(oExtracted);
        when(extractor.extract(oNewValue)).thenReturn(oExtractedNew);
        when(extractor.extract(oValue2)).thenReturn(oExtracted2);
        when(extractor.extract(oNewValue2)).thenReturn(oExtractedNew2);

        // begin test

        // verify that the index does not contain a value for the tested keys
        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey));
        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey2));
        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey3));

        // insert into the index
        mapIndex.insert(entry);   //key  (extracted value : 10)
        mapIndex.insert(entry2);  //key2 (extracted value : 11)
        mapIndex.insert(entry3);  //key3 (extracted value : 21)

        // verify the value from the index for key
        Object oIndexValue = mapIndex.get(oKey);
        assertEquals("The index should contain the extracted value for key.",
                oExtracted, oIndexValue);

        // verify the value from the index for key2
        Object oIndexValue2 = mapIndex.get(oKey2);
        assertEquals("The index should contain the extracted value for key2.",
                oExtracted2, oIndexValue2);

        // since the extracted value (21) for key3 fails the filter check
        // LessFilter(extractor, 15), it should not be part of
        // the index
        Object oIndexValue3 = mapIndex.get(oKey3);
        assertEquals(MapIndex.NO_VALUE, oIndexValue3);

        // get the inverse map
        NullableSortedMap mapInverse = (NullableSortedMap) mapIndex.getIndexContents();

        // assert the inverse map does contain an entry for the
        // extracted values for key
        assertTrue(mapInverse.containsKey(oExtracted));

        // assert that the set mapped to the extracted value for key contains
        // key
        Set set = (Set) mapInverse.get(oExtracted);
        assertTrue("The index's inverse map should contain the key.",
                set.contains(oKey));

        // assert the inverse map does contain an entry for the
        // extracted values for key2
        assertTrue(mapInverse.containsKey(oExtracted2));

        // assert that the set mapped to the extracted value for key2 contains
        // key2
        set = (Set) mapInverse.get(oExtracted2);
        assertTrue("The index's inverse map should contain the key2.",
                set.contains(oKey2));

        // assert the inverse map does not contain an entry for the
        // extracted value for key3
        assertFalse(mapInverse.containsKey(oExtracted3));

        // update the index
        mapIndex.update(entryNew);   // key  (extracted value : 11)
        mapIndex.update(entryNew2);  // key2 (extracted value : 30)

        // assert the index now contains the updated value for key
        oIndexValue = mapIndex.get(oKey);
        assertEquals("The index should contain the updated value for key.",
                oExtractedNew, oIndexValue);

        // assert that the instance for the extracted value 11 is reused
        assertSame("The value for key and key2 should be the same instance.",
                oIndexValue, oIndexValue2);

        // verify the value for key2 is no longer available from the index
        // since the updated extracted value (30) for key2 fails the filter
        // check : LessFilter(extractor, 15), it should not be
        // part of the index
        oIndexValue2 = mapIndex.get(oKey2);
        assertEquals("The index should not contain the extracted value for key2.",
                MapIndex.NO_VALUE, oIndexValue2);

        // assert the inverse map does contain an entry for the
        // extracted value for key
        mapInverse = (NullableSortedMap) mapIndex.getIndexContents();
        assertTrue(mapInverse.containsKey(oExtractedNew));

        // assert that the set mapped to the old extracted value for key
        // no longer contains key... result of update
        set = (Set) mapInverse.get(oExtracted);
        assertTrue("The index's inverse map should not contain key.",
                set == null || !set.contains(oKey));

        // assert that the set mapped to the extracted value for key contains
        // key
        set = (Set) mapInverse.get(oExtractedNew);
        assertTrue("The index's inverse map should contain key.",
                set.contains(oKey));

        // assert that the set mapped to the old extracted value for key2
        // no longer contains key2... result of update
        set = (Set) mapInverse.get(oExtracted2);
        assertTrue("The index's inverse map should not contain key2.",
                set == null || !set.contains(oKey2));

        // assert the inverse map does not contain an entry for the new
        // extracted value for key2... fails filter check
        set = (Set) mapInverse.get(oExtractedNew2);
        assertTrue("The index's inverse map should not contain key2.",
                set == null || !set.contains(oKey2));
        }

    /**
    * Test update on a ConditionalIndex.  Verify the following :
    * 1) the index contains the new value for a key after an update
    * 2) if multiple equivalent values are inserted into the index
    *    for different keys, verify that only one copy of the value exists in
    *    the index
    * 3) extracted values from entries that do not pass the filter test are
    *    not added to the index
    * 4) keys are no longer associated with the old extracted values in the
    *    inverse mapping after the update
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testUpdate_forwardIndexFalse() throws Exception
        {
        // create the mocks
        MapTrigger.Entry entry     = mock(MapTrigger.Entry.class);
        MapTrigger.Entry entry2    = mock(MapTrigger.Entry.class);
        MapTrigger.Entry entry3    = mock(MapTrigger.Entry.class);
        MapTrigger.Entry entryNew  = mock(MapTrigger.Entry.class);
        MapTrigger.Entry entryNew2 = mock(MapTrigger.Entry.class);
        ValueExtractor   extractor = mock(ValueExtractor.class);

        // create the ConditionalIndex to be tested
        Filter           filter   = new LessFilter(extractor, 15);
        ConditionalIndex mapIndex = new ConditionalIndex(filter, extractor,
                true, null, false, null);

        // define the keys and values for the mock entries
        Object oKey           = "key";
        Object oValue         = 0;
        Object oExtracted     = 10;
        Object oNewValue      = 1;
        Object oExtractedNew  = 11;
        Object oKey2          = "key2";
        Object oValue2        = 2;
        Object oExtracted2    = 11;
        Object oKey3          = "key3";
        Object oValue3        = 3;
        Object oExtracted3    = 21;
        Object oNewValue2     = 4;
        Object oExtractedNew2 = 14;

        // set mock expectations
        when(entry.getKey()).thenReturn(oKey);
        when(entry.getValue()).thenReturn(oValue);

        when(entryNew.getKey()).thenReturn(oKey);
        when(entryNew.getValue()).thenReturn(oNewValue);

        when(entry2.getKey()).thenReturn(oKey2);
        when(entry2.getValue()).thenReturn(oValue2);

        when(entryNew2.getKey()).thenReturn(oKey2);
        when(entryNew2.getValue()).thenReturn(oNewValue2);

        when(entry3.getValue()).thenReturn(oValue3);

        when(extractor.extract(oValue)).thenReturn(oExtracted);
        when(extractor.extract(oNewValue)).thenReturn(oExtractedNew);
        when(extractor.extract(oValue2)).thenReturn(oExtracted2);
        when(extractor.extract(oNewValue2)).thenReturn(oExtractedNew2);

        when(entry.extract(extractor)).thenReturn(oExtracted);
        when(entryNew.extract(extractor)).thenReturn(oExtractedNew);
        when(entry2.extract(extractor)).thenReturn(oExtracted2);
        when(entryNew2.extract(extractor)).thenReturn(oExtractedNew2);
        when(entry3.extract(extractor)).thenReturn(oExtracted3);

        when(entryNew.isOriginalPresent()).thenReturn(true);
        when(entryNew.getOriginalValue()).thenReturn(oValue);
        when(entryNew2.isOriginalPresent()).thenReturn(true);
        when(entryNew2.getOriginalValue()).thenReturn(oValue2);

        // begin test

        // verify that the index does not contain a value for the tested keys
        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey));
        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey2));
        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey3));

        // insert into the index
        mapIndex.insert(entry);  // key, oExtracted
        mapIndex.insert(entry2); // key, oExtracted2
        mapIndex.insert(entry3); // key, oExtracted3

        // all gets should return NO_VALUE since forward index is not supported
        // verify no value from the index for key
        Object oIndexValue = mapIndex.get(oKey);
        assertEquals(MapIndex.NO_VALUE, oIndexValue);

        // verify no value from the index for key2
        Object oIndexValue2 = mapIndex.get(oKey2);
        assertEquals(MapIndex.NO_VALUE, oIndexValue2);

        // verify no value in the index for key3
        Object oIndexValue3 = mapIndex.get(oKey3);
        assertEquals(MapIndex.NO_VALUE, oIndexValue3);

        // update the index
        mapIndex.update(entryNew);   // key, oExtractedNew
        mapIndex.update(entryNew2);  // key2, oExtractedNew2

        // all gets should return NO_VALUE since forward index is not supported
        // verify no value from the index for key
        oIndexValue = mapIndex.get(oKey);
        assertEquals(MapIndex.NO_VALUE, oIndexValue);

        oIndexValue2 = mapIndex.get(oKey2);
        assertEquals(MapIndex.NO_VALUE, oIndexValue2);

        // get the inverse map
        NullableSortedMap mapInverse = (NullableSortedMap) mapIndex.
                getIndexContents();

        // get the set of keys from the inverse map keyed by the extracted
        // value for key
        Set set = (Set) mapInverse.get(oExtractedNew);

        // verify that the set of keys contains key
        assertTrue("The index's inverse map should contain the key.",
                set.contains(oKey));

        // get the set of keys from the inverse map keyed by the old extracted
        // value for key
        set = (Set) mapInverse.get(oExtracted);

        // verify that the set of keys does not contain key
        assertTrue("The index's inverse map should not contain the key for the old extracted value.",
                set == null || !set.contains(oKey));

        // get the set of keys from the inverse map keyed by the extracted
        // value for key2
        set = (Set) mapInverse.get(oExtractedNew2);

        // verify that the set of keys contains key2
        assertTrue("The index's inverse map should contain the key2.",
                set.contains(oKey2));

        // get the set of keys from the inverse map keyed by the old extracted
        // value for key2
        set = (Set) mapInverse.get(oExtracted2);

        // verify that the set of keys does not contain key2
        assertTrue("The index's inverse map should not contain key2 for the old extracted value.",
                set == null || !set.contains(oKey2));
        }

    /**
    * Test delete from a ConditionalIndex.  Verify that the index does not
    * contain a value for a key after an delete.
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testDelete() throws Exception
        {
        // create the mocks
        Map.Entry      entry     = mock(Map.Entry.class);
        ValueExtractor extractor = mock(ValueExtractor.class);

        // create the ConditionalIndex to be tested
        Filter           filter   = new LessFilter(extractor, 15);
        ConditionalIndex mapIndex = new ConditionalIndex(filter, extractor, true, null, true, null);

        // define the keys and values for the mock entries
        Object oKey       = "key";
        Object oValue     = 1;
        Object oExtracted = 11;

        // set mock expectations
        when(entry.getKey()).thenReturn(oKey);
        when(entry.getValue()).thenReturn(oValue);

        when(extractor.extract(oValue)).thenReturn(oExtracted);

        // begin test

        // verify that the index does not contain a value for the tested keys
        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey));

        // insert into the index
        mapIndex.insert(entry);

        Object extractedValue = mapIndex.get(oKey);
        assertEquals("The index should contain the extracted value for key.",
                oExtracted, extractedValue);

        mapIndex.delete(entry);

        assertEquals(MapIndex.NO_VALUE, mapIndex.get(oKey));
        }

    /**
    * Test delete from a ConditionalIndex.  Verify that the index does not
    * contain a value for a key after an delete.
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testDelete_forwardIndexFalse() throws Exception
        {
        // create the mocks
        MapTrigger.Entry entry     = mock(MapTrigger.Entry.class);
        MapTrigger.Entry delEntry  = mock(MapTrigger.Entry.class);
        ValueExtractor   extractor = mock(ValueExtractor.class);

        // create the ConditionalIndex to be tested
        Filter           filter   = new LessFilter(extractor, 15);
        ConditionalIndex mapIndex = new ConditionalIndex(filter, extractor, true, null, false, null);

        // define the keys and values for the mock entries
        Object oKey       = "key";
        Object oValue     = 1;
        Object oExtracted = 11;

        // set mock expectations
        when(entry.getKey()).thenReturn(oKey);
        when(entry.getValue()).thenReturn(oValue);

        when(delEntry.getKey()).thenReturn(oKey);
        when(delEntry.getValue()).thenReturn(null);
        when(delEntry.isOriginalPresent()).thenReturn(true);
        when(delEntry.getOriginalValue()).thenReturn(oValue);

        when(entry.extract(extractor)).thenReturn(oExtracted);
        when(extractor.extract(oValue)).thenReturn(oExtracted);

        // begin test

        // assert the inverse map does not contain an entry for the extracted value
        assertFalse(mapIndex.getIndexContents().containsKey(oExtracted));

        // insert into the index
        mapIndex.insert(entry);

        // assert the inverse map does contain an entry for the extracted value
        assertTrue(mapIndex.getIndexContents().containsKey(oExtracted));

        mapIndex.delete(delEntry);

        // get the inverse map
        NullableSortedMap mapInverse = (NullableSortedMap) mapIndex.getIndexContents();

        // get the set of keys from the inverse map keyed by the extracted
        // value for key
        Set set = (Set) mapInverse.get(oExtracted);

        // verify that the set of keys does not contain key
        assertTrue("The index's inverse map should not contain the key for the extracted value.",
                set == null || !set.contains(oKey));
        }

    /**
    * Test index creation with a dataset larger than 50. This is to test the
    * fix done in COH-4954 that uses a threshold of 50 to determine whether to
    * scan the forward index.
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testCoh4954() throws Exception
        {
        Map map = new HashMap();
        Set set = new HashSet();
        set.add(1);
        for (int i = 0; i < 52; i++)
            {
            map.put(i, set);
            }

        ValueExtractor extractor = new IdentityExtractor();
        Filter filter = new AlwaysFilter();
        ConditionalIndex index = createIndex(map, filter, extractor, true);

        // forward map support == false
        index = createIndex(map, filter, extractor, false);
        }

    private static ConditionalIndex createIndex(Map map, Filter filter, ValueExtractor extractor, boolean fSupportForwardMap)
        {
        ConditionalIndex index = new ConditionalIndex(filter, extractor, false, null, fSupportForwardMap, null);

        for (Iterator iter = map.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();
            index.insert(entry);
            }
        return index;
        }
    }
