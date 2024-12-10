/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.Base;
import com.tangosol.util.ObservableHashMap;
import com.tangosol.util.ObservableMap;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


/**
* A collection of unit tests for {@link AbstractCacheStore}.
*
* @author jh 2005.10.04
*/
public class CacheStoreTest
        extends Base
    {
    // ----- test methods ---------------------------------------------------

    /**
    * Test the {@link AbstractCacheStore#eraseAll} method.
    */
    @Test
    public void eraseAll()
        {
        TestCacheStore<String, String> store    = new TestCacheStore<>();
        List<String>                   listKeys = new ArrayList<>();
        Map<String, String>            mapStore = store.getStorageMap();

        // empty key collection
        mapStore.clear();
        mapStore.put("1", "1:Value");
        mapStore.put("2", "2:Value");
        mapStore.put("3", "3:Value");
        listKeys.clear();
        store.eraseAll(listKeys);
        assertTrue("eraseAll() mutated key Collection.", listKeys.isEmpty());
        assertTrue("eraseAll(" + listKeys + ") erased items.", mapStore.size() == 3);

        // single key collection with miss
        mapStore.clear();
        mapStore.put("1", "1:Value");
        mapStore.put("2", "2:Value");
        mapStore.put("3", "3:Value");
        listKeys.clear();
        listKeys.add("Miss");
        store.eraseAll(listKeys);
        assertTrue("eraseAll() did not mutate key Collection.", listKeys.isEmpty());
        assertTrue("eraseAll(" + listKeys + ") erased items.", mapStore.size() == 3);

        // single key collection with hit
        mapStore.clear();
        mapStore.put("1", "1:Value");
        mapStore.put("2", "2:Value");
        mapStore.put("3", "3:Value");
        listKeys.clear();
        listKeys.add("1");
        store.eraseAll(listKeys);
        assertTrue("eraseAll() did not mutate key Collection.", listKeys.isEmpty());
        assertTrue("eraseAll(" + listKeys + ") did not remove an item.", mapStore.size() == 2);

        // multi key collection with miss
        mapStore.clear();
        mapStore.put("1", "1:Value");
        mapStore.put("2", "2:Value");
        mapStore.put("3", "3:Value");
        listKeys.clear();
        listKeys.add("Miss:1");
        listKeys.add("Miss:2");
        store.eraseAll(listKeys);
        assertTrue("eraseAll() did not mutate key Collection.", listKeys.isEmpty());
        assertTrue("eraseAll(" + listKeys + ") erased items.", mapStore.size() == 3);

        // multi key collection with miss and hit
        mapStore.clear();
        mapStore.put("1", "1:Value");
        mapStore.put("2", "2:Value");
        mapStore.put("3", "3:Value");
        listKeys.clear();
        listKeys.add("1");
        listKeys.add("2");
        listKeys.add("Miss");
        store.eraseAll(listKeys);
        assertTrue("eraseAll() did not mutate key Collection.", listKeys.isEmpty());
        assertTrue("eraseAll(" + listKeys + ") did not erase 2 items.", mapStore.size() == 1);

        // single immutable key collection with hit
        mapStore.clear();
        mapStore.put("1", "1:Value");
        mapStore.put("2", "2:Value");
        mapStore.put("3", "3:Value");
        listKeys.clear();
        listKeys.add("1");
        store.eraseAll(Collections.unmodifiableCollection(listKeys));
        assertTrue("eraseAll() mutated key Collection.", listKeys.size() == 1);
        assertTrue("eraseAll(" + listKeys + ") did not erase an item.", mapStore.size() == 2);
        }

    /**
    * Test the {@link AbstractCacheStore#loadAll} method.
    */
    @Test
    public void loadAll()
        {
        TestCacheStore<String, String> store    = new TestCacheStore<>();
        List<String>                   listKeys = new ArrayList<>();
        Map<String, String>            mapStore = store.getStorageMap();
        Map<String, String>            mapResults;

        mapStore.put("1", "1:Value");
        mapStore.put("2", "2:Value");
        mapStore.put("3", "3:Value");

        // empty key collection
        listKeys.clear();
        mapResults = store.loadAll(listKeys);
        assertTrue("loadAll() mutated key Collection.", listKeys.isEmpty());
        assertTrue("loadAll(" + listKeys + ") returned a non-empty Map.", mapResults.isEmpty());

        // single key collection with miss
        listKeys.clear();
        listKeys.add("Miss");
        mapResults = store.loadAll(listKeys);
        assertTrue("loadAll() mutated key Collection.", listKeys.size() == 1);
        assertTrue("loadAll(" + listKeys + ") returned a non-empty Map.", mapResults.isEmpty());

        // single key collection with hit
        listKeys.clear();
        listKeys.add("1");
        mapResults = store.loadAll(listKeys);
        assertTrue("loadAll() mutated key Collection.", listKeys.size() == 1);
        assertTrue("loadAll(" + listKeys + ") returned an incorrect Map.",
                mapResults.size() == 1 && equals(mapResults.get("1"), "1:Value"));

        // multi key collection with miss
        listKeys.clear();
        listKeys.add("Miss:1");
        listKeys.add("Miss:2");
        mapResults = store.loadAll(listKeys);
        assertTrue("loadAll() mutated key Collection.", listKeys.size() == 2);
        assertTrue("loadAll(" + listKeys + ") returned a non-empty Map.", mapResults.isEmpty());

        // multi key collection with all hits
        listKeys.clear();
        listKeys.add("1");
        listKeys.add("2");
        mapResults = store.loadAll(listKeys);
        assertTrue("loadAll() mutated key Collection.", listKeys.size() == 2);
        assertTrue("loadAll(" + listKeys + ") returned an incorrect Map.",
                mapResults.size() == 2 &&
                equals(mapResults.get("1"), "1:Value") &&
                equals(mapResults.get("2"), "2:Value"));

        // multi key collection with miss and hits
        listKeys.clear();
        listKeys.add("Miss");
        listKeys.add("1");
        listKeys.add("2");
        mapResults = store.loadAll(listKeys);
        assertTrue("loadAll() mutated key Collection.", listKeys.size() == 3);
        assertTrue("loadAll(" + listKeys + ") returned an incorrect Map.",
                mapResults.size() == 2 &&
                equals(mapResults.get("1"), "1:Value") &&
                equals(mapResults.get("2"), "2:Value"));
        }

    /**
    * Test the {@link AbstractCacheStore#storeAll} method.
    */
    @Test
    public void storeAll()
        {
        TestCacheStore<String, String> store    = new TestCacheStore<>();
        Map<String, String>            mapStore = store.getStorageMap();
        Map<String, String>            mapEntries = new HashMap<>();

        // empty entry map
        mapStore.clear();
        mapEntries.clear();
        store.storeAll(mapEntries);
        assertTrue("storeAll() mutated entry Map.", mapEntries.isEmpty());
        assertTrue("storeAll(" + mapEntries + ") stored items.", mapStore.isEmpty());

        // single entry map
        mapStore.clear();
        mapEntries.clear();
        mapEntries.put("1", "1:Value");
        store.storeAll(mapEntries);
        assertTrue("storeAll() did not mutate entry Map.", mapEntries.isEmpty());
        assertTrue("storeAll(" + mapEntries + ") did not store an item.",
                mapStore.size() == 1 && equals(mapStore.get("1"), "1:Value"));

        // multi entry map
        mapStore.clear();
        mapEntries.clear();
        mapEntries.put("1", "1:Value");
        mapEntries.put("2", "2:Value");
        store.storeAll(mapEntries);
        assertTrue("storeAll() did not mutate entry Map.", mapEntries.isEmpty());
        assertTrue("storeAll(" + mapEntries + ") did not store 2 items.",
                mapStore.size() == 2 &&
                equals(mapStore.get("1"), "1:Value") &&
                equals(mapStore.get("2"), "2:Value"));

        // single immutable entry map
        mapStore.clear();
        mapEntries.clear();
        mapEntries.put("1", "1:Value");
        store.storeAll(Collections.unmodifiableMap(mapEntries));
        assertTrue("storeAll() mutated entry Map.", mapEntries.size() == 1);
        assertTrue("storeAll(" + mapEntries + ") did not store an item.",
                mapStore.size() == 1 && equals(mapStore.get("1"), "1:Value"));
        }


    // ----- TestCacheStore inner class -------------------------------------

    /**
    * {@link AbstractCacheStore} extension that uses a Map to store entries.
    */
    public static class TestCacheStore<K, V>
            extends AbstractCacheStore<K, V>
        {
        // ----- constructors -----------------------------------------------

        /**
        * Default constructor.
        */
        public TestCacheStore()
            {
            m_mapStorage = new ObservableHashMap<>();
            }


        // ----- AbstractCacheStore methods ---------------------------------

        /**
        * Return the value associated with the specified key, or null if the
        * key does not have an associated value in the underlying store.
        *
        * @param key key whose associated value is to be returned
        *
        * @return the value associated with the specified key, or <tt>null</tt>
        *         if no value is available for that key
        */
        public V load(K key)
            {
            return getStorageMap().get(key);
            }

        /**
        * Store the specified value under the specified key in the underlying
        * store. This method is intended to support both key/value creation and
        * value update for a specific key.
        *
        * @param key   key to store the value under
        * @param value value to be stored
        *
        * @throws UnsupportedOperationException if this implementation or the
        *                                       underlying store is read-only
        */
        public void store(K key, V value)
            {
            getStorageMap().put(key, value);
            }

        /**
        * Remove the specified key from the underlying store if present.
        *
        * @param key key whose mapping is being removed from the cache
        *
        * @throws UnsupportedOperationException if this implementation or the
        *                                       underlying store is read-only
        */
        public void erase(K key)
            {
            getStorageMap().remove(key);
            }

        /**
        * Return the ObservableMap used by the TestCacheStore to load, store,
        * and erase persisted objects.
        *
        * @return the ObservableMap used to load, store, and erase objects
        */
        public ObservableMap<K, V> getStorageMap()
            {
            return m_mapStorage;
            }


        // ----- data members -----------------------------------------------

        /**
        * The ObservableMap used to load, store, and erase objects.
        */
        private ObservableMap<K, V> m_mapStorage;
        }
    }
