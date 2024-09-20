/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.common.collections.NullableConcurrentMap;
import com.oracle.coherence.common.collections.NullableSortedMap;

import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.IdentityExtractor;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
* SimpleMapIndex unit tests.
*
* @author tb 2009.12.01
*/
public class SimpleMapIndexTest
    {
    /**
    * Test getValueExtractor.
    */
    @Test
    public void testGetValueExtractor()
        {
        ValueExtractor extractor = new IdentityExtractor();
        SimpleMapIndex index     = new SimpleMapIndex(extractor, false, null, null);

        assertEquals(extractor, index.getValueExtractor());
        }

    /**
    * Test isOrdered.
    */
    @Test
    public void testIsOrdered()
        {
        ValueExtractor extractor = new IdentityExtractor();
        SimpleMapIndex index     = new SimpleMapIndex(extractor, false, null, null);

        assertFalse(index.isOrdered());

        index = new SimpleMapIndex(extractor, true, null, null);

        assertTrue(index.isOrdered());
        }

    /**
    * Test isPartial.
    */
    @Test
    public void testIsPartial()
        {
        ValueExtractor extractor = new IdentityExtractor();
        SimpleMapIndex index = new SimpleMapIndex(extractor, false, null, null);

        assertFalse(index.isPartial());

        index.insert(new SimpleMapEntry("key1", 6));

        assertFalse(index.isPartial());

        index.update(new SimpleMapEntry("key1", 4));

        assertFalse(index.isPartial());

        index.delete(new SimpleMapEntry("key1", 4));

        assertFalse(index.isPartial());
        }

    /**
    * Test getIndexContents.
    */
    @Test
    public void testGetIndexContents()
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

        ValueExtractor extractor = new IdentityExtractor();
        SimpleMapIndex index = createIndex(map, extractor);
        Map indexContents = index.getIndexContents();

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
        assertEquals(5, setFive.size());
        assertTrue(setFive.contains("five"));
        assertTrue(setFive.contains("five_a"));
        assertTrue(setFive.contains("five_b"));
        assertTrue(setFive.contains("five_c"));
        assertTrue(setFive.contains("five_d"));
        }

    /**
    * Test get.
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testGet()
        throws Exception
        {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("one",   1);
        map.put("two",   2);
        map.put("three", 3);
        map.put("four",  4);
        map.put("five",  5);


        ValueExtractor extractor = new IdentityExtractor();
        SimpleMapIndex index = createIndex(map, extractor);

        assertEquals(1, index.get("one"));
        assertEquals(2, index.get("two"));
        assertEquals(3, index.get("three"));
        assertEquals(4, index.get("four"));
        assertEquals(5, index.get("five"));

        // test fix for COH-9237
        SimpleMapIndex indexNF = new SimpleMapIndex(extractor, false, null, null);
        indexNF.m_mapForward = null;

        Map.Entry entry = map.entrySet().iterator().next();
        indexNF.insert(entry);
        assertTrue(indexNF.get(entry.getKey()) == MapIndex.NO_VALUE);
        }

    /**
    * Test insert into a SimpleMapIndex.  Verify the following :
    * 1) the index contains a value for a key after an insert
    * 2) if multiple equivalent values are inserted into the index
    *    for different keys, verify that only one copy of the value exists in
    *    the index
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testInsert()
        throws Exception
        {
        // create the mocks
        Map.Entry entry = mock(Map.Entry.class);
        Map.Entry entry2 = mock(Map.Entry.class);
        ValueExtractor extractor = mock(ValueExtractor.class);

        // create the SimpleMapIndex to be tested
        SimpleMapIndex mapIndex = new SimpleMapIndex(extractor, true, null,
            null);

        // define the keys and values for the mock entries
        Object oKey        = "key";
        Object oValue      = 1;
        Object oExtracted  = 11;
        Object oKey2       = "key2";
        Object oValue2     = 2;
        Object oExtracted2 = 11;

        // set mock expectations
        when(entry.getKey()).thenReturn(oKey);
        when(entry.getValue()).thenReturn(oValue);

        when(entry2.getKey()).thenReturn(oKey2);
        when(entry2.getValue()).thenReturn(oValue2);
        when(extractor.extract(oValue)).thenReturn(oExtracted);
        when(extractor.extract(oValue2)).thenReturn(oExtracted2);

        // begin test

        // verify that the index does not contain a value for the tested keys
        assertTrue(mapIndex.get(oKey) == mapIndex.NO_VALUE);
        assertTrue(mapIndex.get(oKey2) == mapIndex.NO_VALUE);

        // insert into the index
        mapIndex.insert(entry);
        mapIndex.insert(entry2);

        // verify the value from the index for key
        Object oIndexValue = mapIndex.get(oKey);
        assertEquals(
            "The index should contain the extracted value for key.",
            oExtracted, oIndexValue);

        // verify the value from the index for key2
        Object oIndexValue2 = mapIndex.get(oKey2);
        assertEquals(
            "The index should contain the extracted value for key.",
            oExtracted2, oIndexValue2);

        // verify that the value for key and key2 is the same instance
        assertSame(
            "The value for key and key2 should be the same instance.",
            oIndexValue, oIndexValue2);

        // get the inverse map
        NullableSortedMap mapInverse = (NullableSortedMap) mapIndex.getIndexContents();

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

    /**
    * Test update on a SimpleMapIndex.  Verify the following :
    * 1) the index contains the new value for a key after an update
    * 2) if multiple equivalent values are inserted into the index
    *    for different keys, verify that only one copy of the value exists in
    *    the index
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testUpdate()
        throws Exception
        {
        // create the mocks
        Map.Entry      entry     = mock(Map.Entry.class);
        Map.Entry      entry2    = mock(Map.Entry.class);
        Map.Entry      entryNew  = mock(Map.Entry.class);
        ValueExtractor extractor = mock(ValueExtractor.class);

        // create the SimpleMapIndex to be tested
        SimpleMapIndex mapIndex = new SimpleMapIndex(extractor, true, null,
            null);

        // define the keys and values for the mock entries
        Object oKey          = "key";
        Object oValue        = 0;
        Object oExtracted    = 10;
        Object oNewValue     = 1;
        Object oExtractedNew = 11;
        Object oKey2         = "key2";
        Object oValue2       = 2;
        Object oExtracted2   = 11;

        // set mock expectations
        when(entry.getKey()).thenReturn(oKey);
        when(entry.getValue()).thenReturn(oValue);

        when(entryNew.getKey()).thenReturn(oKey);
        when(entryNew.getValue()).thenReturn(oNewValue);

        when(entry2.getKey()).thenReturn(oKey2);
        when(entry2.getValue()).thenReturn(oValue2);
        when(extractor.extract(oValue)).thenReturn(oExtracted);
        when(extractor.extract(oNewValue)).thenReturn(oExtractedNew);
        when(extractor.extract(oValue2)).thenReturn(oExtracted2);

        // begin test

        // verify that the index does not contain a value for the tested keys
        assertTrue(mapIndex.get(oKey) == MapIndex.NO_VALUE);
        assertTrue(mapIndex.get(oKey2) == MapIndex.NO_VALUE);

        // insert into the index
        mapIndex.insert(entry);
        mapIndex.insert(entry2);

        // verify the value from the index for key
        Object oIndexValue = mapIndex.get(oKey);
        assertEquals(
            "The index should contain the extracted value for key.",
            oExtracted, oIndexValue);

        // verify the value from the index for key2
        Object oIndexValue2 = mapIndex.get(oKey2);
        assertEquals(
            "The index should contain the extracted value for key2.",
            oExtracted2, oIndexValue2);

        // verify the value for key and key2 are different
        assertFalse(
            "The values for key and key2 should be different.",
            oIndexValue.equals(oIndexValue2));

        // update the index
        mapIndex.update(entryNew);

        oIndexValue = mapIndex.get(oKey);
        assertEquals(
            "The index should contain the updated value for key.",
            oExtractedNew, oIndexValue);

        assertSame(
            "The value for key and key2 should be the same instance.",
            oIndexValue, oIndexValue2);

        NullableSortedMap mapInverse = (NullableSortedMap) mapIndex.getIndexContents();

        Map.Entry inverseEntry = mapInverse.getEntry(oExtractedNew);

        assert inverseEntry != null;
        assertSame(oIndexValue, inverseEntry.getKey());

        Map.Entry inverseEntry2 = mapInverse.getEntry(oExtracted2);

        assert inverseEntry2 != null;
        assertSame(oIndexValue2, inverseEntry2.getKey());

        Set set = (Set) mapInverse.get(oIndexValue);

        assertTrue(
            "The index's inverse map should contain the key.",
            set.contains(oKey));
        }

    /**
    * Test delete from a SimpleMapIndex.  Verify that the index does not
    * contain a value for a key after an delete.
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testDelete()
        throws Exception
        {
        // create the mocks
        Map.Entry      entry     = mock(Map.Entry.class);
        ValueExtractor extractor = mock(ValueExtractor.class);

        // create the SimpleMapIndex to be tested
        SimpleMapIndex mapIndex = new SimpleMapIndex(extractor, true, null,
            null);

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
        assertTrue(mapIndex.get(oKey) == MapIndex.NO_VALUE);

        // insert into the index
        mapIndex.insert(entry);

        Object extractedValue = mapIndex.get(oKey);
        assertEquals(
            "The index should contain the extracted value for key.",
            oExtracted, extractedValue);

        mapIndex.delete(entry);
        assertTrue(mapIndex.get(oKey) == MapIndex.NO_VALUE);
        }

    /**
    * Test insert of a Collection into a SimpleMapIndex.
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testInsertWithCollection()
        throws Exception
        {
        insertUpdateWithCollection(false);
        }

    /**
    * Test update of a Collection in a SimpleMapIndex.
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testUpdateWithCollection()
        throws Exception
        {
        insertUpdateWithCollection(true);
        }

    /**
    * Test insert of a Object array into a SimpleMapIndex.
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testInsertWithArray()
        throws Exception
        {
        insertUpdateWithArray(false);
        }

    /**
    * Test update of an index for key-based indices.
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testUpdateKeyExtractor()
        {
        Map.Entry         entry     = mock(Map.Entry.class);
        AbstractExtractor extractor = mock(AbstractExtractor.class);
        Integer           NValue    = 1; // key, value & extracted value

        when(extractor.getTarget()).thenReturn(AbstractExtractor.KEY);

        SimpleMapIndex indexMap = new SimpleMapIndex(extractor, false, null, null);

        // setup for insert
        when(entry.getKey()).thenReturn(NValue);
        when(entry.getValue()).thenReturn(NValue);
        when(extractor.extractFromEntry(any())).thenReturn(NValue);
        when(extractor.getTarget()).thenReturn(AbstractExtractor.KEY);

        indexMap.insert(entry);

        assertEquals(indexMap.get(NValue), NValue);

        when(extractor.extractFromEntry(any())).thenThrow(
            new IllegalStateException("Extract should not be called"));

        indexMap.update(entry); // should throw if extractor is called again

        Object oFwdIndex = indexMap.get(NValue);
        assertNotEquals("Index updates should not occur for key based indices",
            oFwdIndex, SimpleMapIndex.NO_VALUE);
        assertEquals(oFwdIndex, NValue);
        }

    /**
    * Test update of a Object array in a SimpleMapIndex.
    *
    * @throws Exception  rethrow any exception to be caught by test framework
    */
    @Test
    public void testUpdateWithArray()
        throws Exception
        {
        insertUpdateWithArray(true);
        }

    /**
    * Internal method called by test methods to test insert/update of a
    * Collection in a SimpleMapIndex.  Verify the following :
    * 1) the index contains the new value for a key after an update
    * 2) if multiple equivalent Collection instances are inserted/updated
    *    in the index for different keys, verify that only one copy of the
    *    Collection exists in the index
    *
    * @param fUpdate  if true then do an insert followed by an update; else
    *                 do an insert only
    */
    private static void insertUpdateWithCollection(boolean fUpdate)
        {
        // create the mocks
        Map.Entry entry = mock(Map.Entry.class);
        Map.Entry entry2 = mock(Map.Entry.class);
        Map.Entry entry3 = mock(Map.Entry.class);
        Map.Entry entry4 = mock(Map.Entry.class);
        ValueExtractor extractor = mock(ValueExtractor.class);

        // create the SimpleMapIndex to be tested
        SimpleMapIndex mapIndex = new SimpleMapIndex(extractor, false, null,
            null);

        // define the keys and values for the mock entries
        Object oKey = "key";
        Object oValue = "value";
        Object oKey2 = "key2";
        Object oValue2 = "value2";
        Object oKey3 = "key3";
        Object oValue3 = "value3";
        Object oKey4 = "key4";
        Object oValue4 = "value4";

        // define and populate the Collection values for the mock entries
        Collection<Integer> collection = new LinkedList<Integer>();
        Collection<Integer> collection2 = new LinkedList<Integer>();
        Collection<Integer> collection3 = new LinkedList<Integer>();
        Collection<Integer> collection4 = new LinkedList<Integer>();
        Collection<Integer> collectionInit = new LinkedList<Integer>();

        // Values used to populate the Collection values
        Integer nCollectionItem1 = 1;
        Integer nCollectionItem2 = 2;
        Integer nCollectionItem3 = 3;
        Integer nCollectionItem4 = 4;
        Integer nCollectionItem5 = 5;

        // use new String to get a different instance than nCollectionItem1
        Integer sCollectionItem1a = 1;

        collection.add(nCollectionItem1);
        collection.add(nCollectionItem2);
        collection.add(nCollectionItem3);

        collection2.add(sCollectionItem1a);
        collection2.add(nCollectionItem4);
        collection2.add(nCollectionItem5);

        collection3.add(sCollectionItem1a);
        collection3.add(nCollectionItem2);
        collection3.add(nCollectionItem3);
        collection3.add(nCollectionItem4);
        collection3.add(nCollectionItem5);

        collection4.add(sCollectionItem1a);
        collection4.add(nCollectionItem2);
        collection4.add(nCollectionItem3);

        collectionInit.add(nCollectionItem1);

        // set mock expectations
        when(entry.getKey()).thenReturn(oKey);
        when(entry.getValue()).thenReturn(oValue);

        when(entry2.getKey()).thenReturn(oKey2);
        when(entry2.getValue()).thenReturn(oValue2);

        when(entry3.getKey()).thenReturn(oKey3);
        when(entry3.getValue()).thenReturn(oValue3);

        when(entry4.getKey()).thenReturn(oKey4);
        when(entry4.getValue()).thenReturn(oValue4);

        when(extractor.extract(oValue)).thenReturn(collection);
        when(extractor.extract(oValue2)).thenReturn(collection2);
        when(extractor.extract(oValue3)).thenReturn(collection3);
        when(extractor.extract(oValue4)).thenReturn(collection4);

        // begin test

        // verify that the index does not contain a value for the tested keys
        assertTrue(mapIndex.get(oKey) == mapIndex.NO_VALUE);
        assertTrue(mapIndex.get(oKey2) == mapIndex.NO_VALUE);
        assertTrue(mapIndex.get(oKey3) == mapIndex.NO_VALUE);
        assertTrue(mapIndex.get(oKey4) == mapIndex.NO_VALUE);

        if (fUpdate)
            {
            // insert into the index
            mapIndex.insert(new SimpleMapEntry(oKey, collectionInit));
            mapIndex.insert(new SimpleMapEntry(oKey2, collectionInit));
            mapIndex.insert(new SimpleMapEntry(oKey3, collectionInit));
            mapIndex.insert(new SimpleMapEntry(oKey4, collectionInit));

            // update the index
            mapIndex.update(entry);
            mapIndex.update(entry2);
            mapIndex.update(entry3);
            mapIndex.update(entry4);
            }
        else
            {
            // insert into the index
            mapIndex.insert(entry);
            mapIndex.insert(entry2);
            mapIndex.insert(entry3);
            mapIndex.insert(entry4);
            }

        Collection collIndexValue = (Collection) mapIndex.get(oKey);
        Collection collIndexValue2 = (Collection) mapIndex.get(oKey2);
        Collection collIndexValue3 = (Collection) mapIndex.get(oKey3);
        Collection collIndexValue4 = (Collection) mapIndex.get(oKey4);

        NullableConcurrentMap mapInverse = (NullableConcurrentMap) mapIndex
                .getIndexContents();

        assertCollectionInverseMap(oKey, collIndexValue, mapInverse);
        assertCollectionInverseMap(oKey2, collIndexValue2, mapInverse);
        assertCollectionInverseMap(oKey3, collIndexValue3, mapInverse);
        assertCollectionInverseMap(oKey4, collIndexValue4, mapInverse);

        assertNotSame(collIndexValue, collIndexValue2);
        assertNotSame(collIndexValue, collIndexValue3);
        assertSame(collIndexValue, collIndexValue4);

        assertNotSame(collIndexValue2, collIndexValue3);
        assertNotSame(collIndexValue2, collIndexValue4);

        assertNotSame(collIndexValue3, collIndexValue4);
        }

    /**
    * Internal method called by test methods to test insert/update of a Object
    * array in a SimpleMapIndex.  Verify the following :
    * 1) the index contains the new value for a key after an update
    * 2) if multiple equivalent Object arrays are inserted/updated in the
    *    index for different keys, verify that only one copy of the Object
    *    array exists in the index
    *
    * @param fUpdate  if true then do an insert followed by an update; else
    *                 do an insert only
    */
    private static void insertUpdateWithArray(boolean fUpdate)
        {
        // create the mocks
        Map.Entry      entry     = mock(Map.Entry.class);
        Map.Entry      entry2    = mock(Map.Entry.class);
        Map.Entry      entry3    = mock(Map.Entry.class);
        Map.Entry      entry4    = mock(Map.Entry.class);
        ValueExtractor extractor = mock(ValueExtractor.class);

        // create the SimpleMapIndex to be tested
        SimpleMapIndex mapIndex = new SimpleMapIndex(extractor, false, null,
            null);

        // define the keys for the mock entries
        Object oKey    = "key";
        Object oValue  = "value";
        Object oKey2   = "key2";
        Object oValue2 = "value2";
        Object oKey3   = "key3";
        Object oValue3 = "value3";
        Object oKey4   = "key4";
        Object oValue4 = "value4";

        // define and populate the Object array values for the mock entries
        Object[] ao     = new Object[3];
        Object[] ao2    = new Object[3];
        Object[] ao3    = new Object[5];
        Object[] ao4    = new Object[3];
        Object[] aoInit = new Object[1];

        // Values used to populate the Object array values
        Integer nArrayItem1  = 1;
        Integer nArrayItem2  = 2;
        Integer nArrayItem3  = 3;
        Integer nArrayItem4  = 4;
        Integer nArrayItem5  = 5;
        Integer nArrayItem1a = 1;

        ao[0] = nArrayItem1;
        ao[1] = nArrayItem2;
        ao[2] = nArrayItem3;

        ao2[0] = nArrayItem1a;
        ao2[1] = nArrayItem4;
        ao2[2] = nArrayItem5;

        ao3[0] = nArrayItem1a;
        ao3[1] = nArrayItem2;
        ao3[2] = nArrayItem3;
        ao3[3] = nArrayItem4;
        ao3[4] = nArrayItem5;

        ao4[0] = nArrayItem1a;
        ao4[1] = nArrayItem2;
        ao4[2] = nArrayItem3;

        aoInit[0] = nArrayItem1;

        // set mock expectations
        when(entry.getKey()).thenReturn(oKey);
        when(entry.getValue()).thenReturn(oValue);

        when(entry2.getKey()).thenReturn(oKey2);
        when(entry2.getValue()).thenReturn(oValue2);

        when(entry3.getKey()).thenReturn(oKey3);
        when(entry3.getValue()).thenReturn(oValue3);

        when(entry4.getKey()).thenReturn(oKey4);
        when(entry4.getValue()).thenReturn(oValue4);

        when(extractor.extract(oValue)).thenReturn(ao);
        when(extractor.extract(oValue2)).thenReturn(ao2);
        when(extractor.extract(oValue3)).thenReturn(ao3);
        when(extractor.extract(oValue4)).thenReturn(ao4);

        // begin test

        // verify that the index does not contain a value for the tested keys
        assertTrue(mapIndex.get(oKey) == mapIndex.NO_VALUE);
        assertTrue(mapIndex.get(oKey2) == mapIndex.NO_VALUE);
        assertTrue(mapIndex.get(oKey3) == mapIndex.NO_VALUE);
        assertTrue(mapIndex.get(oKey4) == mapIndex.NO_VALUE);

        if (fUpdate)
            {
            // insert into the index
            mapIndex.insert(new SimpleMapEntry(oKey, aoInit));
            mapIndex.insert(new SimpleMapEntry(oKey2, aoInit));
            mapIndex.insert(new SimpleMapEntry(oKey3, aoInit));
            mapIndex.insert(new SimpleMapEntry(oKey4, aoInit));

            // update the index
            mapIndex.update(entry);
            mapIndex.update(entry2);
            mapIndex.update(entry3);
            mapIndex.update(entry4);
            }
        else
            {
            // insert into the index
            mapIndex.insert(entry);
            mapIndex.insert(entry2);
            mapIndex.insert(entry3);
            mapIndex.insert(entry4);
            }

        Object[] aoIndexValue  = (Object[]) mapIndex.get(oKey);
        Object[] aoIndexValue2 = (Object[]) mapIndex.get(oKey2);
        Object[] aoIndexValue3 = (Object[]) mapIndex.get(oKey3);
        Object[] aoIndexValue4 = (Object[]) mapIndex.get(oKey4);

        NullableConcurrentMap mapInverse = (NullableConcurrentMap) mapIndex
                .getIndexContents();

        // verify that the inverse map contains an entry for each value in the collection
        assertCollectionInverseMap(
            oKey, Arrays.asList(aoIndexValue), mapInverse);
        assertCollectionInverseMap(
            oKey2, Arrays.asList(aoIndexValue2), mapInverse);
        assertCollectionInverseMap(
            oKey3, Arrays.asList(aoIndexValue3), mapInverse);
        assertCollectionInverseMap(
            oKey4, Arrays.asList(aoIndexValue4), mapInverse);

        assertNotSame(aoIndexValue, aoIndexValue2);
        assertNotSame(aoIndexValue, aoIndexValue3);

        // verify that indexed collection value is reused.
        assertSame(aoIndexValue, aoIndexValue4);

        assertNotSame(aoIndexValue2, aoIndexValue3);
        assertNotSame(aoIndexValue2, aoIndexValue4);

        assertNotSame(aoIndexValue3, aoIndexValue4);
        }

    /**
    * Internal method called by test methods to verify that the given inverse
    * map contains an entry for each value (key) in the collection.  Also
    * verify that the value of each entry is a Set that contains the given key.
    *
    * @param oKey        the key being checked
    * @param collection  the collection which is the extracted value
    * @param mapInverse  the index inverse map
    */
    private static void assertCollectionInverseMap(Object oKey,
        Collection collection, NullableConcurrentMap mapInverse)
        {
        for (Iterator iterator = collection.iterator(); iterator.hasNext();)
            {
            Integer sCollectionItem = (Integer) iterator.next();

            Map.Entry inverseEntry = mapInverse.getEntry(sCollectionItem);

            assertTrue("The index's inverse map should contain an entry "
                    + "keyed by the extracted value.", inverseEntry != null);

            Set set = (Set) mapInverse.get(sCollectionItem);

            assertTrue(
                "The index's inverse map should contain the key.",
                set.contains(oKey));
            }
        }

    private static SimpleMapIndex createIndex(Map map, ValueExtractor extractor)
        {
        SimpleMapIndex index = new SimpleMapIndex(extractor, false, null, null);

        for (Iterator iter = map.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry entry = (Map.Entry) iter.next();
            index.insert(entry);
            }
        return index;
        }
    }
