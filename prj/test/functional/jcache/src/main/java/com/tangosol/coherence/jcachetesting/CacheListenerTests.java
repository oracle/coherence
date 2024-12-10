/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheFactory;
import com.tangosol.util.Base;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.IOError;
import java.io.IOException;
import java.io.Serializable;

import java.util.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.cache.*;

import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;

import javax.cache.event.*;

import javax.cache.expiry.Duration;
import javax.cache.expiry.ModifiedExpiryPolicy;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;

import static javax.cache.event.EventType.*;

/**
 * Test JCache Cache Listener
 *
 * @author jf  2017/10/12
 */
public class CacheListenerTests
        extends TestSupport
    {
    /**
     * Method description
     */
    @BeforeClass
    public static void intialConfig()
        {
        m_mgrCache = getJcacheTestContext().getCacheManager(null, null, null);
        }

    /**
     * Method description
     */
    @Before
    public void setUp()
        {
        CacheFactory.log("@Before setup entered", Base.LOG_INFO);
        m_listener = new MyCacheEntryListener<Long, String>();
        m_listenerConfiguration = new MutableCacheEntryListenerConfiguration<Long,
            String>(FactoryBuilder.factoryOf(m_listener), null, true, true);
        cache = getJcacheTestContext().configureCache(m_mgrCache, getTestCacheName(),
            new MutableConfiguration<Long, String>().addCacheEntryListenerConfiguration(m_listenerConfiguration));
        CacheFactory.log("@Before setup exited  m_listener=" + m_listener + " m_listenerConfiguration=" + m_listenerConfiguration +
                           " cache=" + cache);
        }

    protected MutableConfiguration<Long, String> extraSetup(MutableConfiguration<Long, String> configuration)
        {
        return configuration.setExpiryPolicyFactory(FactoryBuilder.factoryOf(
            new ModifiedExpiryPolicy(new Duration(TimeUnit.MILLISECONDS, 80))));
        }

    /**
     * Method description
     */
    @After
    public void teardown()
        {
        m_mgrCache.destroyCache(getTestCacheName());
        if (m_listener != null)
            {
            assertTrue(m_listener.isClosed());
            }
        }

    /**
     * Check the listener is getting reads.
     */
    @Test
    public void testCacheEntryListener()
        {
        assertEquals(0, m_listener.getCreated());
        assertEquals(0, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(0, m_listener.getRemoved());

        cache.put(1l, "Sooty");
        assertEquals(1, m_listener.getCreated());
        assertEquals(0, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(0, m_listener.getRemoved());

        Map<Long, String> entries = new HashMap<Long, String>();

        entries.put(2l, "Lucky");
        entries.put(3l, "Prince");
        cache.putAll(entries);
        assertEquals(3, m_listener.getCreated());
        assertEquals(0, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(0, m_listener.getRemoved());

        cache.put(1l, "Sooty");
        assertEquals(3, m_listener.getCreated());
        assertEquals(1, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(0, m_listener.getRemoved());

        cache.putAll(entries);
        assertEquals(3, m_listener.getCreated());
        assertEquals(3, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(0, m_listener.getRemoved());

        cache.getAndPut(4l, "Cody");
        assertEquals(4, m_listener.getCreated());
        assertEquals(3, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(0, m_listener.getRemoved());

        cache.getAndPut(4l, "Cody");
        assertEquals(4, m_listener.getCreated());
        assertEquals(4, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(0, m_listener.getRemoved());

        String value = cache.get(1l);

        assertEquals(4, m_listener.getCreated());
        assertEquals(4, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(0, m_listener.getRemoved());

        EntryProcessor<Long, String, String> multiArgEP = new MultiArgumentEntryProcessor<Long, String, String>();
        String                               result     = cache.invoke(1l, multiArgEP, "These", "are", "arguments");

        assertEquals(value, result);
        assertEquals(4, m_listener.getCreated());
        assertEquals(4, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(0, m_listener.getRemoved());

        result = cache.invoke(1l, new SetEntryProcessor<Long, String, String>("Zoot"));
        assertEquals("Zoot", result);
        assertEquals(4, m_listener.getCreated());
        assertEquals(5, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(0, m_listener.getRemoved());

        result = cache.invoke(1l, new RemoveEntryProcessor<Long, String, String>());
        assertNull(result);
        assertEquals(4, m_listener.getCreated());
        assertEquals(5, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(1, m_listener.getRemoved());

        result = cache.invoke(1l, new SetEntryProcessor<Long, String, String>("Moose"));
        assertEquals("Moose", result);
        assertEquals(5, m_listener.getCreated());
        assertEquals(5, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(1, m_listener.getRemoved());

        Iterator<Cache.Entry<Long, String>> iterator = cache.iterator();

        while (iterator.hasNext())
            {
            iterator.next();
            iterator.remove();
            }

        assertEquals(5, m_listener.getCreated());
        assertEquals(5, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(5, m_listener.getRemoved());
        }

    /**
     * Check the listener doesn't get removes from a cache.clear
     */
    @Test
    public void testCacheClearListener()
        {
        assertEquals(0, m_listener.getCreated());
        assertEquals(0, m_listener.getUpdated());
        assertEquals(0, m_listener.getExpired());
        assertEquals(0, m_listener.getRemoved());

        cache.put(1l, "Sooty");
        assertEquals(1, m_listener.getCreated());
        assertEquals(0, m_listener.getUpdated());
        assertEquals(0, m_listener.getRemoved());

        cache.clear();

        // there should be no change in events!
        assertEquals(1, m_listener.getCreated());
        assertEquals(0, m_listener.getUpdated());
        assertEquals(0, m_listener.getRemoved());
        }

    /**
     * Checks that the correct listeners are called the correct number of times from all of our access and mutation operations.
     * @throws InterruptedException
     */
    @Test
    public void testFilteredListener()
            throws InterruptedException
        {
        // default configured cache is of no use to us.  close and configure listener/filter pair for cache.
        m_mgrCache.destroyCache(cache.getName());

        MyCacheEntryListener<Long, String> listener = new MyCacheEntryListener<Long, String>();

        cache = getJcacheTestContext().configureCache(
            m_mgrCache, getTestCacheName(),
            new MutableConfiguration<Long, String>().addCacheEntryListenerConfiguration(
                new MutableCacheEntryListenerConfiguration<Long, String>(FactoryBuilder.factoryOf(listener),
                    FactoryBuilder.factoryOf(new MyCacheEntryEventFilter<Long, String>()), true, true)));

        assertEquals(0, listener.getCreated());
        assertEquals(0, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(0, listener.getRemoved());

        cache.put(1l, "Sooty");
        assertEquals(1, listener.getCreated());
        assertEquals(0, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(0, listener.getRemoved());

        Map<Long, String> entries = new HashMap<Long, String>();

        entries.put(2l, "Lucky");
        entries.put(3l, "Bryn");
        cache.putAll(entries);
        assertEquals(2, listener.getCreated());
        assertEquals(0, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(0, listener.getRemoved());

        cache.put(1l, "Zyn");
        assertEquals(2, listener.getCreated());
        assertEquals(0, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(0, listener.getRemoved());

        cache.remove(2l);
        assertEquals(2, listener.getCreated());
        assertEquals(0, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(1, listener.getRemoved());

        cache.replace(1l, "Fred");
        assertEquals(2, listener.getCreated());
        assertEquals(1, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(1, listener.getRemoved());

        cache.replace(3l, "Bryn", "Sooty");
        assertEquals(2, listener.getCreated());
        assertEquals(2, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(1, listener.getRemoved());

        cache.get(1L);
        assertEquals(2, listener.getCreated());
        assertEquals(2, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(1, listener.getRemoved());

        // containsKey is not a read for listener purposes.
        cache.containsKey(1L);
        assertEquals(2, listener.getCreated());
        assertEquals(2, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(1, listener.getRemoved());

        // iterating should cause read events on non-expired entries
        for (Cache.Entry<Long, String> entry : cache)
            {
            String value = entry.getValue();

            System.out.println(value);
            }

        assertEquals(2, listener.getCreated());
        assertEquals(2, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(1, listener.getRemoved());

        cache.getAndPut(1l, "Pistachio");
        assertEquals(2, listener.getCreated());
        assertEquals(3, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(1, listener.getRemoved());

        Set<Long> keys = new HashSet<Long>();

        keys.add(1L);
        cache.getAll(keys);
        assertEquals(2, listener.getCreated());
        assertEquals(3, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(1, listener.getRemoved());

        cache.getAndReplace(1l, "Prince");
        assertEquals(2, listener.getCreated());
        assertEquals(4, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(1, listener.getRemoved());

        cache.getAndRemove(1l);
        assertEquals(2, listener.getCreated());
        assertEquals(4, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(2, listener.getRemoved());

        assertEquals(2, listener.getCreated());
        assertEquals(4, listener.getUpdated());
        assertEquals(2, listener.getRemoved());
        }

    /**
     * Check the listener is only throwing CacheException
     */
    @Test
    public void testBrokenCacheEntryListener()
        {
        // remove standard listener.
        assertNotNull("if this assertion fails, junit @Before setup failed", m_listenerConfiguration);
        cache.deregisterCacheEntryListener(m_listenerConfiguration);
        m_listener = null;


        // setup
        ThrowsExceptionCacheEntryListener<Long, String> brokenListener = new ThrowsExceptionCacheEntryListener<Long,
                                                                             String>();

        m_listenerConfiguration = new MutableCacheEntryListenerConfiguration<Long,
            String>(FactoryBuilder.factoryOf(brokenListener), null, true, true);
        cache.registerCacheEntryListener(m_listenerConfiguration);

        try
            {
            cache.put(1l, "Sooty");
            }
        catch (CacheEntryListenerException e)
            {
            // expected
            }

        Map<Long, String> entries = new HashMap<Long, String>();

        entries.put(2l, "Lucky");
        entries.put(3l, "Prince");

        try
            {
            cache.put(1l, "Sooty");
            }
        catch (CacheEntryListenerException e)
            {
            // expected
            }

        try
            {
            cache.putAll(entries);
            }
        catch (CacheEntryListenerException e)
            {
            // expected
            }

        try
            {
            cache.put(1l, "Sooty");
            }
        catch (CacheEntryListenerException e)
            {
            // expected
            }

        try
            {
            cache.putAll(entries);
            }
        catch (CacheEntryListenerException e)
            {
            // expected
            }

        try
            {
            cache.getAndPut(4l, "Cody");
            }
        catch (CacheEntryListenerException e)
            {
            // expected
            }

        try
            {
            cache.remove(4l);
            }
        catch (IOError e)
            {
            // expected. We don't wrap Error
            }

        try
            {
            cache.remove(4l);
            }
        catch (IOError e)
            {
            // expected. We don't wrap Error
            }

        String                               value      = cache.get(1l);
        EntryProcessor<Long, String, String> multiArgEP = new MultiArgumentEntryProcessor<>();

        try
            {
            String result = cache.invoke(1l, multiArgEP, "These", "are", "arguments", 1l);
            }
        catch (CacheEntryListenerException e)
            {
            // expected
            }

        try
            {
            String result = cache.invoke(1l, new SetEntryProcessor<Long, String, String>("Zoot"));

            Iterator<Cache.Entry<Long, String>> iterator = cache.iterator();

            while (iterator.hasNext())
                {
                iterator.next();
                iterator.remove();
                }
            }
        catch (CacheEntryListenerException e)
            {
            // expected
            }

        }

    /**
     * Class description
     *
     * @param <K>
     * @param <V>
     * @param <T>
     *
     * @version        Enter version here..., 13/05/23
     * @author         Enter your name here...
     */
    static public class MultiArgumentEntryProcessor<K, V, T>
            implements EntryProcessor<K, V, T>, Serializable, PortableObject
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
        public T process(MutableEntry<K, V> entry, Object... arguments)
            {
            assertEquals("These", arguments[0]);
            assertEquals("are", arguments[1]);
            assertEquals("arguments", arguments[2]);

            return (T) entry.getValue();
            }

        @Override
        public void readExternal(PofReader pofReader)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter pofWriter)
                throws IOException
            {
            }
        }

    /**
     * Class description
     *
     * @version        Enter version here..., 13/06/06
     * @author         Enter your name here...
     *
     * @param <K>
     * @param <V>
     */
    static public class MyCacheEntryEventFilter<K, V>
            implements CacheEntryEventFilter<K, V>, Serializable, PortableObject
        {
        /**
         * Method description
         *
         * @param event
         *
         * @return
         *
         * @throws CacheEntryListenerException
         */
        @Override
        public boolean evaluate(CacheEntryEvent<? extends K, ? extends V> event)
                throws CacheEntryListenerException
            {
            boolean result = false;

            if (event.getValue() instanceof String)
                {
                String v = (String) event.getValue();

                result = v.contains("a") || v.contains("e") || v.contains("i") || v.contains("o") || v.contains("u");
                }

            if (event.getEventType() == EventType.EXPIRED)
                {
                System.out.println("filter event=" + event + " filter result=" + result);
                }

            return result;
            }

        // ----- PortableObject interface --------------------------------------------------------------------------

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
     * Test listener
     *
     * @param <K>
     * @param <V>
     */
    static public class MyCacheEntryListener<K, V>
            implements CacheEntryCreatedListener<K, V>, CacheEntryUpdatedListener<K, V>,
                       CacheEntryExpiredListener<K, V>, CacheEntryRemovedListener<K, V>, Serializable, PortableObject,
                       Closeable
        {
        /**
         * Method description
         *
         * @return
         */
        public int getCreated()
            {
            return created.get();
            }

        /**
         * Method description
         *
         * @return
         */
        public int getUpdated()
            {
            return updated.get();
            }

        /**
         * Method description
         *
         * @return
         */
        public int getRemoved()
            {
            return removed.get();
            }

        /**
         * Method description
         *
         * @return
         */
        public int getExpired()
            {
            return expired.get();
            }

        @Override
        public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
                throws CacheEntryListenerException
            {
            for (CacheEntryEvent<? extends K, ? extends V> event : events)
                {
                assertEquals(CREATED, event.getEventType());
                created.incrementAndGet();
                }
            }

        @Override
        public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
                throws CacheEntryListenerException
            {
            for (CacheEntryEvent<? extends K, ? extends V> event : events)
                {
                assertEquals(EXPIRED, event.getEventType());

                long result = expired.incrementAndGet();
                }
            }

        @Override
        public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
                throws CacheEntryListenerException
            {
            int i = 1;

            for (CacheEntryEvent<? extends K, ? extends V> event : events)
                {
                assertEquals(REMOVED, event.getEventType());
                removed.incrementAndGet();
                System.out.println("[" + i + "]:onRemoved: remove event: " + event);
                i++;
                }
            }

        @Override
        public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
                throws CacheEntryListenerException
            {
            for (CacheEntryEvent<? extends K, ? extends V> event : events)
                {
                assertEquals(UPDATED, event.getEventType());
                updated.incrementAndGet();
                }
            }

        public boolean isClosed()
            {
            return closed.get();
            }

        // ----- PortableObject ---------------------------------------------------------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            created.set(in.readInt(0));
            updated.set(in.readInt(1));
            removed.set(in.readInt(2));
            expired.set(in.readInt(3));
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeInt(0, created.get());
            out.writeInt(1, updated.get());
            out.writeInt(2, removed.get());
            out.writeInt(3, expired.get());
            }

        // ----- data members -----------------------------------------------------------------------------------------
        AtomicInteger created = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();
        AtomicInteger removed = new AtomicInteger();
        AtomicInteger expired = new AtomicInteger();
        AtomicBoolean closed  = new AtomicBoolean(false);

        @Override
        public void close()
            {
            closed.set(true);
            }
        }

    /**
     * Class description
     *
     * @param <K>
     * @param <V>
     * @param <T>
     *
     * @version        Enter version here..., 13/05/23
     * @author         Enter your name here...
     */
    static public class RemoveEntryProcessor<K, V, T>
            implements EntryProcessor<K, V, T>, Serializable, PortableObject
        {
        @Override
        public T process(MutableEntry<K, V> entry, Object... arguments)
            {
            assertTrue(entry.exists());
            entry.remove();
            assertTrue(!entry.exists());

            return (T) entry.getValue();
            }

        @Override
        public void readExternal(PofReader pofReader)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter pofWriter)
                throws IOException
            {
            }
        }

    /**
     * Class description
     *
     * @param <K>
     * @param <V>
     * @param <T>
     *
     * @version        Enter version here..., 13/05/23
     * @author         Enter your name here...
     */
    static public class SetEntryProcessor<K, V, T>
            implements EntryProcessor<K, V, T>, Serializable, PortableObject
        {
        /**
         * Constructs ...
         *
         */
        public SetEntryProcessor()
            {
            this.setValue = null;
            }

        /**
         * Constructs ...
         *
         *
         * @param setValue
         */
        public SetEntryProcessor(V setValue)
            {
            this.setValue = setValue;
            }

        @Override
        public T process(MutableEntry<K, V> entry, Object... arguments)
            {
            entry.setValue(setValue);

            return (T) entry.getValue();
            }

        // ----- PortableObject Interface ----------------------------------------------------

        @Override
        public void readExternal(PofReader pofReader)
                throws IOException
            {
            setValue = (V) pofReader.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter pofWriter)
                throws IOException
            {
            pofWriter.writeObject(0, setValue);
            }

        V setValue;
        }

    /**
     * Class description
     *
     * @param <K>
     * @param <V>
     *
     * @version        Enter version here..., 13/08/26
     * @author         Enter your name here...
     */
    static public class ThrowsExceptionCacheEntryListener<K, V>
            implements CacheEntryCreatedListener<K, V>, CacheEntryUpdatedListener<K, V>,
                       CacheEntryExpiredListener<K, V>, CacheEntryRemovedListener<K, V>, Serializable
        {
        @Override
        public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents)
                throws CacheEntryListenerException
            {
            throw new IllegalStateException("illegal state in onCreated");
            }

        @Override
        public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents)
                throws CacheEntryListenerException
            {
            throw new IllegalStateException("illegal state in onExpired");
            }

        @Override
        public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents)
                throws CacheEntryListenerException
            {
            throw new IllegalStateException("illegal state in onRemoved");
            }

        @Override
        public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents)
                throws CacheEntryListenerException
            {
            throw new IllegalStateException("illegal state in onUpdated");
            }
        }

    // ----- data members ---------------------------------------------------------------------------------------------

    protected static CacheManager                                  m_mgrCache;

    protected Cache<Long, String>                                  cache;
    protected MyCacheEntryListener<Long, String>                   m_listener;
    protected MutableCacheEntryListenerConfiguration<Long, String> m_listenerConfiguration;
    }
