/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.coherence.jcache.common.CoherenceCacheEntry;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.cache.Cache;
import javax.cache.CacheManager;

import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableConfiguration;

import javax.cache.integration.CacheWriter;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;

/**
 * Unit test for {@link javax.cache.integration.CacheWriter}s.
 *
 * @author Brian Oliver
 */
public class CacheWriterTests
        extends TestSupport
    {
    /**
     * Method description
     */
    @Before
    public void setup()
        {
        cacheWriter = new RecordingCacheWriter<Integer, String>();

        MutableConfiguration<Integer, String> config = new MutableConfiguration<Integer, String>();

        config.setCacheWriterFactory(FactoryBuilder.factoryOf(cacheWriter));
        config.setWriteThrough(true);

        CacheManager cmgr = getJcacheTestContext().getCacheManager(null, null, null);

        cache = cmgr.createCache(getTestCacheName(), config);
        }

    /**
     * Method description
     */
    @After
    public void cleanup()
        {
        CacheManager cacheMgr = getJcacheTestContext().getCacheManager(null, null, null);

        for (String cacheName : cacheMgr.getCacheNames())
            {
            cacheMgr.destroyCache(cacheName);
            }
        }

    /**
     * Method description
     */
    @Test
    public void put_SingleEntry()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.put(1, "Gudday World");

        assertEquals(1, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());
        assertTrue(cacheWriter.containsKey(1));
        assertEquals("Gudday World", cacheWriter.get(1));
        }

    /**
     * Method description
     */
    @Test
    public void put_SingleEntryMultipleTimes()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.put(1, "Gudday World");
        cache.put(1, "Bonjour World");
        cache.put(1, "Hello World");

        assertEquals(3, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());
        assertTrue(cacheWriter.containsKey(1));
        assertEquals("Hello World", cacheWriter.get(1));
        }

    /**
     * Method description
     */
    @Test
    public void put_DifferentEntries()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.put(1, "Gudday World");
        cache.put(2, "Bonjour World");
        cache.put(3, "Hello World");

        assertEquals(3, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        assertTrue(cacheWriter.containsKey(1));
        assertEquals("Gudday World", cacheWriter.get(1));

        assertTrue(cacheWriter.containsKey(2));
        assertEquals("Bonjour World", cacheWriter.get(2));

        assertTrue(cacheWriter.containsKey(3));
        assertEquals("Hello World", cacheWriter.get(3));
        }

    /**
     * Method description
     */
    @Test
    public void getAndPut_SingleEntryMultipleTimes()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.getAndPut(1, "Gudday World");
        cache.getAndPut(1, "Bonjour World");
        cache.getAndPut(1, "Hello World");

        assertEquals(3, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());
        assertTrue(cacheWriter.containsKey(1));
        assertEquals("Hello World", cacheWriter.get(1));
        }

    /**
     * Method description
     */
    @Test
    public void getAndPut_DifferentEntries()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.getAndPut(1, "Gudday World");
        cache.getAndPut(2, "Bonjour World");
        cache.getAndPut(3, "Hello World");

        assertEquals(3, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        assertTrue(cacheWriter.containsKey(1));
        assertEquals("Gudday World", cacheWriter.get(1));

        assertTrue(cacheWriter.containsKey(2));
        assertEquals("Bonjour World", cacheWriter.get(2));

        assertTrue(cacheWriter.containsKey(3));
        assertEquals("Hello World", cacheWriter.get(3));
        }

    /**
     * Method description
     */
    @Test
    public void putIfAbsent_SingleEntryMultipleTimes()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.putIfAbsent(1, "Gudday World");
        cache.putIfAbsent(1, "Bonjour World");
        cache.putIfAbsent(1, "Hello World");

        assertEquals(1, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());
        assertTrue(cacheWriter.containsKey(1));
        assertEquals("Gudday World", cacheWriter.get(1));
        }

    /**
     * Method description
     */
    @Test
    public void replaceMatching_SingleEntryMultipleTimes()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.putIfAbsent(1, "Gudday World");
        cache.replace(1, "Gudday World", "Bonjour World");
        cache.replace(1, "Gudday World", "Hello World");
        cache.replace(1, "Bonjour World", "Hello World");

        assertEquals(3, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());
        assertTrue(cacheWriter.containsKey(1));
        assertEquals("Hello World", cacheWriter.get(1));
        }

    /**
     * Method description
     */
    @Test
    public void replaceExisting_SingleEntryMultipleTimes()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.replace(1, "Gudday World");
        cache.putIfAbsent(1, "Gudday World");
        cache.replace(1, "Bonjour World");
        cache.replace(1, "Hello World");

        assertEquals(3, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());
        assertTrue(cacheWriter.containsKey(1));
        assertEquals("Hello World", cacheWriter.get(1));
        }

    /**
     * Method description
     */
    @Test
    public void getAndReplace_SingleEntryMultipleTimes()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.getAndReplace(1, "Gudday World");
        cache.putIfAbsent(1, "Gudday World");
        cache.getAndReplace(1, "Bonjour World");
        cache.getAndReplace(1, "Hello World");

        assertEquals(3, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());
        assertTrue(cacheWriter.containsKey(1));
        assertEquals("Hello World", cacheWriter.get(1));
        }

    /**
     * Method description
     */
    @Test
    public void invoke_CreateEntry()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());
        assertFalse(cacheWriter.containsKey(1));

        cache.invoke(1, new CreateEntryProcessor());

        assertEquals(1, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());
        assertTrue(cacheWriter.containsKey(1));
        assertEquals("Gudday World", cacheWriter.get(1));
        }

    /**
     * Method description
     */
    @Test
    public void invoke_UpdateEntry()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.put(1, "Gudday World");
        cache.invoke(1, new UpdateEntryProcessor());

        assertEquals(2, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());
        assertTrue(cacheWriter.containsKey(1));
        assertEquals("Hello World", cacheWriter.get(1));
        }

    /**
     * Method description
     */
    @Test
    public void invoke_RemoveEntry()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.put(1, "Gudday World");
        cache.invoke(1, new RemoveEntryProcessor());

        assertEquals(1, cacheWriter.getWriteCount());
        assertEquals(1, cacheWriter.getDeleteCount());
        assertFalse(cacheWriter.containsKey(1));
        }

    /**
     * Method description
     */
    @Test
    public void remove_SingleEntry()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.put(1, "Gudday World");
        cache.remove(1);

        assertEquals(1, cacheWriter.getWriteCount());
        assertEquals(1, cacheWriter.getDeleteCount());
        assertFalse(cacheWriter.containsKey(1));
        }

    /**
     * Method description
     */
    @Test
    public void remove_SingleEntryMultipleTimes()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.put(1, "Gudday World");
        cache.remove(1);
        cache.remove(1);
        cache.remove(1);

        assertEquals(1, cacheWriter.getWriteCount());

        assertEquals(3, cacheWriter.getDeleteCount());
        assertFalse(cacheWriter.containsKey(1));
        }

    /**
     * Method description
     */
    @Test
    public void remove_SpecificEntry()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.put(1, "Gudday World");
        cache.remove(1, "Hello World");
        cache.remove(1, "Gudday World");
        cache.remove(1, "Gudday World");
        cache.remove(1);

        assertEquals(1, cacheWriter.getWriteCount());

        assertEquals(2, cacheWriter.getDeleteCount());
        assertFalse(cacheWriter.containsKey(1));
        }

    /**
     * Method description
     */
    @Test
    public void getAndRemove_SingleEntry()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.getAndRemove(1);
        cache.put(1, "Gudday World");
        cache.getAndRemove(1);

        assertEquals(1, cacheWriter.getWriteCount());
        assertEquals(2, cacheWriter.getDeleteCount());
        assertFalse(cacheWriter.containsKey(1));
        }

    /**
     * Method description
     */
    @Test
    public void iterator_remove()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        cache.getAndPut(1, "Gudday World");
        cache.getAndPut(2, "Bonjour World");
        cache.getAndPut(3, "Hello World");

        Iterator<Cache.Entry<Integer, String>> iterator = cache.iterator();

        iterator.next();
        iterator.remove();
        iterator.next();
        iterator.next();
        iterator.remove();

        assertEquals(3, cacheWriter.getWriteCount());
        assertEquals(2, cacheWriter.getDeleteCount());
        }

    /**
     * Method description
     */
    @Test
    public void putAll()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        HashMap<Integer, String> map = new HashMap<Integer, String>();

        map.put(1, "Gudday World");
        map.put(2, "Bonjour World");
        map.put(3, "Hello World");

        cache.putAll(map);

        assertEquals(3, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        for (Integer key : map.keySet())
            {
            assertTrue(cacheWriter.containsKey(key));
            assertEquals(map.get(key), cacheWriter.get(key));
            assertTrue(cache.containsKey(key));
            assertEquals(map.get(key), cache.get(key));
            }

        map.put(4, "Hola World");

        cache.putAll(map);

        assertEquals(7, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        for (Integer key : map.keySet())
            {
            assertTrue(cacheWriter.containsKey(key));
            assertEquals(map.get(key), cacheWriter.get(key));
            assertTrue(cache.containsKey(key));
            assertEquals(map.get(key), cache.get(key));
            }
        }

    /**
     * Method description
     */
    @Test
    public void removeAll()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        HashMap<Integer, String> map = new HashMap<Integer, String>();

        map.put(1, "Gudday World");
        map.put(2, "Bonjour World");
        map.put(3, "Hello World");

        cache.putAll(map);

        assertEquals(3, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        for (Integer key : map.keySet())
            {
            assertTrue(cacheWriter.containsKey(key));
            assertEquals(map.get(key), cacheWriter.get(key));
            assertTrue(cache.containsKey(key));
            assertEquals(map.get(key), cache.get(key));
            }

        cache.removeAll();

        assertEquals(3, cacheWriter.getWriteCount());
        assertEquals(3, cacheWriter.getDeleteCount());

        for (Integer key : map.keySet())
            {
            assertFalse(cacheWriter.containsKey(key));
            assertFalse(cache.containsKey(key));
            }

        map.put(4, "Hola World");

        cache.putAll(map);

        assertEquals(7, cacheWriter.getWriteCount());
        assertEquals(3, cacheWriter.getDeleteCount());

        for (Integer key : map.keySet())
            {
            assertTrue(cacheWriter.containsKey(key));
            assertEquals(map.get(key), cacheWriter.get(key));
            assertTrue(cache.containsKey(key));
            assertEquals(map.get(key), cache.get(key));
            }
        }

    /**
     * Method description
     */
    @Test
    public void removeAllSpecific()
        {
        assertEquals(0, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        HashMap<Integer, String> map = new HashMap<Integer, String>();

        map.put(1, "Gudday World");
        map.put(2, "Bonjour World");
        map.put(3, "Hello World");
        map.put(4, "Hola World");

        cache.putAll(map);

        assertEquals(4, cacheWriter.getWriteCount());
        assertEquals(0, cacheWriter.getDeleteCount());

        for (Integer key : map.keySet())
            {
            assertTrue(cacheWriter.containsKey(key));
            assertEquals(map.get(key), cacheWriter.get(key));
            assertTrue(cache.containsKey(key));
            assertEquals(map.get(key), cache.get(key));
            }

        HashSet<Integer> set = new HashSet<Integer>();

        set.add(1);
        set.add(4);

        cache.removeAll(set);

        assertEquals(4, cacheWriter.getWriteCount());
        assertEquals(2, cacheWriter.getDeleteCount());

        for (Integer key : set)
            {
            assertFalse(cacheWriter.containsKey(key));
            assertFalse(cache.containsKey(key));
            }

        cache.put(4, "Howdy World");

        assertEquals(5, cacheWriter.getWriteCount());
        assertEquals(2, cacheWriter.getDeleteCount());

        set.clear();
        set.add(2);

        cache.removeAll(set);

        assertTrue(cacheWriter.containsKey(3));
        assertTrue(cache.containsKey(3));
        assertTrue(cacheWriter.containsKey(4));
        assertTrue(cache.containsKey(4));
        }

    /**
     * Method description
     */
    @Test
    public void RecordingCacheWriterDeleteAllTest()
        {
        RecordingCacheWriter<String, String> cacheWriter = new RecordingCacheWriter<String, String>();

        cacheWriter.write(new CoherenceCacheEntry<String, String>("key1", "value1"));
        cacheWriter.write(new CoherenceCacheEntry<String, String>("key2", "value2"));
        assertEquals(2, cacheWriter.getWriteCount());

        HashSet<String> keys = new HashSet<String>();

        keys.add("key1");
        keys.add("key2");
        assertEquals(2, keys.size());

        try
            {
            cacheWriter.deleteAll(keys);
            assertEquals(2, cacheWriter.getDeleteCount());
            assertEquals(0, keys.size());
            }
        catch (Throwable e)
            {
            assertTrue("unexpected exception", false);
            }
        }

    /**
     * Class description
     *
     * @version        Enter version here..., 13/09/10
     * @author         Enter your name here...
     */
    static public class CreateEntryProcessor
            implements EntryProcessor<Integer, String, Void>, Serializable, PortableObject
        {
        /**
         * Method description
         *
         * @param entry
         * @param arguments
         *
         * @return
         */
        @Override
        public Void process(MutableEntry<Integer, String> entry, Object... arguments)
            {
            assertFalse(entry.exists());
            entry.setValue("Gudday World");
            assertTrue(entry.exists());

            return null;
            }

        // ----- PortableObject interface ------------------------------------------------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }
        }

    /**
     * A CacheWriter implementation that records the entries written to it so
     * that they may be later asserted.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     */
    public static class RecordingCacheWriter<K, V>
            implements CacheWriter<K, V>, Serializable, PortableObject
        {
        /**
         * Constructs a RecordingCacheWriter.
         */
        public RecordingCacheWriter()
            {
            this.map         = new ConcurrentHashMap<K, V>();
            this.writeCount  = new AtomicLong();
            this.deleteCount = new AtomicLong();
            }

        /**
         * Method description
         *
         * @param entry
         */
        @Override
        public void write(Cache.Entry<? extends K, ? extends V> entry)
            {
            map.put(entry.getKey(), entry.getValue());

            writeCount.incrementAndGet();
            }

        /**
         * Method description
         *
         * @param entries
         */
        @Override
        public void writeAll(Collection<Cache.Entry<? extends K, ? extends V>> entries)
            {
            Iterator<Cache.Entry<? extends K, ? extends V>> iterator = entries.iterator();

            while (iterator.hasNext())
                {
                write(iterator.next());
                iterator.remove();
                }
            }

        /**
         * Method description
         *
         * @param key
         */
        @Override
        public void delete(Object key)
            {
            map.remove(key);
            deleteCount.incrementAndGet();
            }

        /**
         * Method description
         *
         * @param entries
         */
        @Override
        public void deleteAll(Collection<?> entries)
            {
            for (Iterator<?> keys = entries.iterator(); keys.hasNext(); )
                {
                delete(keys.next());
                keys.remove();
                }
            }

        /**
         * Gets the last written value of the specified key
         *
         * @param key the key
         * @return the value last written
         */
        public V get(K key)
            {
            return (V) map.get(key);
            }

        /**
         * Determines if there is a last written value for the specified key
         *
         * @param key the key
         * @return true if there is a last written value
         */
        public boolean containsKey(K key)
            {
            return map.containsKey(key);
            }

        /**
         * Gets the number of writes that have occurred.
         *
         * @return the number of writes
         */
        public long getWriteCount()
            {
            return writeCount.get();
            }

        /**
         * Gets the number of deletes that have occurred.
         *
         * @return the number of writes
         */
        public long getDeleteCount()
            {
            return deleteCount.get();
            }

        /**
         * Clears the contents of stored values.
         */
        public void clear()
            {
            map.clear();
            this.writeCount  = new AtomicLong();
            this.deleteCount = new AtomicLong();
            }

        // ----- PortableObject interface -----------------------------------------------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }

        // ----- data members -----------------------------------------------------------------------------------------

        /**
         * A map of keys to values that have been written.
         */
        static private ConcurrentHashMap map;

        /**
         * The number of writes that have so far occurred.
         */
        static private AtomicLong writeCount;

        /**
         * The number of deletes that have so far occurred.
         */
        static private AtomicLong deleteCount;
        }

    /**
     * Class description
     *
     * @version        Enter version here..., 13/09/10
     * @author         Enter your name here...
     */
    static public class RemoveEntryProcessor
            implements EntryProcessor<Integer, String, Void>, Serializable, PortableObject
        {
        /**
         * Method description
         *
         * @param entry
         * @param arguments
         *
         * @return
         */
        @Override
        public Void process(MutableEntry<Integer, String> entry, Object... arguments)
            {
            entry.remove();

            return null;
            }

        // ----- PortableObject interface ------------------------------------------------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }
        }

    /**
     * Class description
     *
     * @version        Enter version here..., 13/09/10
     * @author         Enter your name here...
     */
    static public class UpdateEntryProcessor
            implements EntryProcessor<Integer, String, Void>, Serializable, PortableObject
        {
        /**
         * Method description
         *
         * @param entry
         * @param arguments
         *
         * @return
         */
        @Override
        public Void process(MutableEntry<Integer, String> entry, Object... arguments)
            {
            assertTrue(entry.exists());
            entry.setValue("Hello World");
            assertTrue(entry.exists());

            return null;
            }

        // ----- PortableObject interface ------------------------------------------------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            }
        }

    /**
     * The CacheWriter used for the tests.
     */
    private RecordingCacheWriter<Integer, String> cacheWriter;

    /**
     * The test Cache that will be configured to use the CacheWriter.
     */
    private Cache<Integer, String> cache;
    }
