/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.util.concurrent.atomic.AtomicInteger;

import javax.cache.Cache;
import javax.cache.CacheManager;

import javax.cache.configuration.Factory;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;

import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;

import static javax.cache.event.EventType.CREATED;
import static javax.cache.event.EventType.EXPIRED;
import static javax.cache.event.EventType.REMOVED;
import static javax.cache.event.EventType.UPDATED;

/**
 * Class description
 *
 * @version        Enter version here..., 13/05/17
 * @author         Enter your name here...
 */
public class CacheExpiryTests
        extends TestSupport
    {
    /**
     * Method description
     */
    @Before
    public void setup()
        {
        m_cacheMgr = getJcacheTestContext().getCacheManager(null, null, null);
        m_listener = new CacheListenerTests.MyCacheEntryListener<Integer, Integer>();
        }

    /**
     * Method description
     */
    @After
    public void cleanup()
        {
        for (String name : m_cacheMgr.getCacheNames())
            {
            m_cacheMgr.destroyCache(name);
            }
        }

    /**
     * Test that verifies that reuse of a previously expired binaryentry results in individual jcache ExpiryEvents
     */
    @Test
    public void expire_whenCreated_reuseBinaryEntryTest()
        {
        MutableConfiguration<Integer, Integer> config = new MutableConfiguration<Integer, Integer>();

        config.setExpiryPolicyFactory(FactoryBuilder.factoryOf(new ParameterizedExpiryPolicy(Duration.ZERO, null,
            null)));
        config.addCacheEntryListenerConfiguration(new MutableCacheEntryListenerConfiguration<Integer,
            Integer>(FactoryBuilder.factoryOf(m_listener), null, true, true));

        Cache<Integer, Integer> cache = getJcacheTestContext().configureCache(m_cacheMgr, getTestCacheName(), config);

        cache.put(3, 3);
        assertFalse(cache.containsKey(3));
        cache.put(3, 4);
        assertFalse(cache.containsKey(3));
        assertNull(cache.get(3));
        Eventually.assertThat(invoking(m_listener).getExpired(), is(0));
        }

    /**
     * Ensure that a cache using a {@link javax.cache.expiry.ExpiryPolicy} configured to
     * return a {@link javax.cache.expiry.Duration#ZERO} for newly created entries will immediately
     * expire said entries.
     */

    @Test
    public void expire_whenCreated()
        {
        MutableConfiguration<Integer, Integer> config = new MutableConfiguration<Integer, Integer>();

        config.setExpiryPolicyFactory(FactoryBuilder.factoryOf(new ParameterizedExpiryPolicy(Duration.ZERO, null,
            null)));
        config.addCacheEntryListenerConfiguration(new MutableCacheEntryListenerConfiguration<Integer,
            Integer>(FactoryBuilder.factoryOf(m_listener), null, true, true));

        Cache<Integer, Integer> cache = getJcacheTestContext().configureCache(m_cacheMgr, getTestCacheName(), config);

        cache.put(3, 3);
        assertFalse(cache.containsKey(3));
        cache.put(3, 4);
        assertFalse(cache.containsKey(3));

        cache.put(1, 1);
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        cache.put(1, 1);
        assertFalse(cache.remove(1));

        cache.put(1, 1);
        assertFalse(cache.remove(1, 1));

        cache.getAndPut(1, 1);
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        cache.putIfAbsent(1, 1);
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

        map.put(1, 1);
        cache.putAll(map);
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        cache.put(1, 1);
        assertFalse(cache.iterator().hasNext());
        Eventually.assertThat(invoking(m_listener).getExpired(), is(0));
        System.out.println("Exit whenCreated expired=" + m_listener.getExpired());
        }

    /**
     * Method description
     */

    @Test
    public void expire_whenAccessed_JCACHE141()
        {
        Factory<ExpireOnAccessPolicy> factory = FactoryBuilder.factoryOf(ExpireOnAccessPolicy.class);

        expire_whenAccessed_internal(factory);
        }

    /**
     * Method description
     */
    @Test
    public void expire_whenAccessed_parameterizedexpirypolicy()
        {
        Factory<ParameterizedExpiryPolicy> factory =
            FactoryBuilder.factoryOf(new ParameterizedExpiryPolicy(Duration.ETERNAL, Duration.ZERO, null));

        expire_whenAccessed_internal(factory);

        }

    /**
     * Ensure that a cache using a {@link javax.cache.expiry.ExpiryPolicy} configured to
     * return a {@link javax.cache.expiry.Duration#ZERO} after accessing entries will immediately
     * expire said entries.
     *
     * @param factory
     */
    private void expire_whenAccessed_internal(Factory<? extends ExpiryPolicy> factory)
        {
        MutableConfiguration<Integer, Integer> config = new MutableConfiguration<Integer, Integer>();

        config.setExpiryPolicyFactory(factory);
        config.addCacheEntryListenerConfiguration(new MutableCacheEntryListenerConfiguration<Integer,
            Integer>(FactoryBuilder.factoryOf(m_listener), null, true, true));

        Cache<Integer, Integer> cache = getJcacheTestContext().configureCache(m_cacheMgr, getTestCacheName(), config);

        cache.put(1, 1);
        assertTrue(cache.containsKey(1));
        assertNotNull(cache.get(1));
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        cache.put(1, 1);
        assertTrue(cache.containsKey(1));
        assertNotNull(cache.get(1));
        assertFalse(cache.containsKey(1));
        assertNull(cache.getAndReplace(1, 2));

        cache.put(1, 1);
        assertTrue(cache.containsKey(1));
        assertNotNull(cache.get(1));
        assertFalse(cache.containsKey(1));
        assertNull(cache.getAndRemove(1));

        cache.put(1, 1);
        assertTrue(cache.containsKey(1));
        assertNotNull(cache.get(1));
        assertFalse(cache.remove(1));

        cache.put(1, 1);
        assertTrue(cache.containsKey(1));
        assertNotNull(cache.get(1));
        assertFalse(cache.remove(1, 1));

        cache.getAndPut(1, 1);
        assertTrue(cache.containsKey(1));
        assertNotNull(cache.get(1));
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        cache.getAndPut(1, 1);
        assertTrue(cache.containsKey(1));
        assertNotNull(cache.getAndPut(1, 1));
        assertTrue(cache.containsKey(1));
        assertNotNull(cache.get(1));
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        boolean result = cache.putIfAbsent(1, 1);
        assertTrue(result);
        assertTrue(cache.containsKey(1));
        assertNotNull(cache.get(1));
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

        map.put(1, 1);
        cache.putAll(map);
        assertTrue(cache.containsKey(1));
        assertNotNull(cache.get(1));
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        cache.put(1, 1);

        Iterator<Cache.Entry<Integer, Integer>> iterator = cache.iterator();

        assertTrue(iterator.hasNext());
        assertEquals((Integer) 1, iterator.next().getValue());
        assertFalse(cache.iterator().hasNext());
        System.out.println("Exit whenAccessed expired=" + m_listener.getExpired());
        Eventually.assertThat(invoking(m_listener).getExpired(), is(10));
        }

    /**
     * Ensure that a cache using a {@link javax.cache.expiry.ExpiryPolicy} configured to
     * return a {@link javax.cache.expiry.Duration#ZERO} after modifying entries will immediately
     * expire said entries.
     */
    @Test
    public void expire_whenModified()
        {
        MutableConfiguration<Integer, Integer> config = new MutableConfiguration<Integer, Integer>();

        config.setExpiryPolicyFactory(FactoryBuilder.factoryOf(new ParameterizedExpiryPolicy(Duration.ETERNAL, null,
            Duration.ZERO)));
        config.addCacheEntryListenerConfiguration(new MutableCacheEntryListenerConfiguration<Integer,
            Integer>(FactoryBuilder.factoryOf(m_listener), null, true, true));

        Cache<Integer, Integer> cache = getJcacheTestContext().configureCache(m_cacheMgr, getTestCacheName(), config);

        cache.put(1, 1);
        assertTrue(cache.containsKey(1));
        assertEquals((Integer) 1, cache.get(1));
        cache.put(1, 2);
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        cache.put(1, 1);
        assertTrue(cache.containsKey(1));
        assertEquals((Integer) 1, cache.get(1));
        cache.put(1, 2);
        assertFalse(cache.remove(1));

        cache.put(1, 1);
        assertTrue(cache.containsKey(1));
        assertEquals((Integer) 1, cache.get(1));
        cache.put(1, 2);
        assertFalse(cache.remove(1, 2));

        cache.getAndPut(1, 1);
        assertTrue(cache.containsKey(1));
        assertEquals((Integer) 1, cache.get(1));
        cache.put(1, 2);
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        cache.getAndPut(1, 1);
        assertTrue(cache.containsKey(1));
        assertEquals((Integer) 1, cache.getAndPut(1, 2));
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        cache.put(1, 1);
        assertTrue(cache.containsKey(1));
        assertEquals((Integer) 1, cache.get(1));

        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

        map.put(1, 2);
        cache.putAll(map);
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        cache.put(1, 1);
        assertTrue(cache.containsKey(1));
        assertEquals((Integer) 1, cache.get(1));
        cache.replace(1, 2);
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        cache.put(1, 1);
        assertTrue(cache.containsKey(1));
        assertEquals((Integer) 1, cache.get(1));
        cache.replace(1, 1, 2);
        assertFalse(cache.containsKey(1));
        assertNull(cache.get(1));

        cache.put(1, 1);
        assertTrue(cache.iterator().hasNext());
        assertEquals((Integer) 1, cache.iterator().next().getValue());
        assertTrue(cache.containsKey(1));
        assertEquals((Integer) 1, cache.iterator().next().getValue());
        cache.put(1, 2);
        assertFalse(cache.iterator().hasNext());
        Eventually.assertThat(invoking(m_listener).getExpired(), is(9));

        System.out.println("Exit whenModified expired=" + m_listener.getExpired());

        }

    /**
     * An {@link ExpiryPolicy} that will expire {@link Cache} entries
     * after they've been accessed.
     */
    public static class ExpireOnAccessPolicy
            implements ExpiryPolicy
        {
        /**
         * Method description
         *
         * @return
         */
        @Override
        public Duration getExpiryForCreation()
            {
            return Duration.ETERNAL;
            }

        /**
         * Method description
         *
         * @return
         */
        @Override
        public Duration getExpiryForAccess()
            {
            return Duration.ZERO;
            }

        /**
         * Method description
         *
         * @return
         */
        @Override
        public Duration getExpiryForUpdate()
            {
            return Duration.ETERNAL;
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
                       CacheEntryExpiredListener<K, V>, CacheEntryRemovedListener<K, V>, Serializable
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

        /**
         * Method description
         *
         * @return
         */
        public ArrayList<CacheEntryEvent<K, V>> getEntries()
            {
            return entries;
            }

        /**
         * Method description
         *
         * @param events
         *
         * @throws CacheEntryListenerException
         */
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

        /**
         * Method description
         *
         * @param events
         *
         * @throws CacheEntryListenerException
         */
        @Override
        public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
                throws CacheEntryListenerException
            {
            for (CacheEntryEvent<? extends K, ? extends V> event : events)
                {
                assertEquals(EXPIRED, event.getEventType());

                long result = expired.incrementAndGet();

                System.out.println("onExpired event=" + event + " expiredCount=" + result);
                }
            }

        /**
         * Method description
         *
         * @param events
         *
         * @throws CacheEntryListenerException
         */
        @Override
        public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
                throws CacheEntryListenerException
            {
            for (CacheEntryEvent<? extends K, ? extends V> event : events)
                {
                assertEquals(REMOVED, event.getEventType());
                removed.incrementAndGet();
                }
            }

        /**
         * Method description
         *
         * @param events
         *
         * @throws CacheEntryListenerException
         */
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

        AtomicInteger                    created = new AtomicInteger();
        AtomicInteger                    updated = new AtomicInteger();
        AtomicInteger                    removed = new AtomicInteger();
        AtomicInteger                    expired = new AtomicInteger();
        ArrayList<CacheEntryEvent<K, V>> entries = new ArrayList<CacheEntryEvent<K, V>>();
        }

    /**
     * A {@link javax.cache.expiry.ExpiryPolicy} that updates the expiry time based on
     * defined parameters.
     */
    public static class ParameterizedExpiryPolicy
            implements ExpiryPolicy, Serializable
        {
        /**
         * Constructs an {@link ParameterizedExpiryPolicy}.
         *
         * @param createdExpiryDuration  the {@link javax.cache.expiry.Duration} to expire when an entry is created
         *                                  (must not be <code>null</code>)
         * @param accessedExpiryDuration the {@link javax.cache.expiry.Duration} to expire when an entry is accessed
         *                                  (<code>null</code> means don't change the expiry)
         * @param modifiedExpiryDuration the {@link javax.cache.expiry.Duration} to expire when an entry is modified
         *                                  (<code>null</code> means don't change the expiry)
         */
        public ParameterizedExpiryPolicy(Duration createdExpiryDuration, Duration accessedExpiryDuration,
                                         Duration modifiedExpiryDuration)
            {
            if (createdExpiryDuration == null)
                {
                throw new NullPointerException("createdExpiryDuration can't be null");
                }

            this.createdExpiryDuration  = createdExpiryDuration;
            this.accessedExpiryDuration = accessedExpiryDuration;
            this.modifiedExpiryDuration = modifiedExpiryDuration;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Duration getExpiryForCreation()
            {
            return createdExpiryDuration;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Duration getExpiryForAccess()
            {
            return accessedExpiryDuration;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Duration getExpiryForUpdate()
            {
            return modifiedExpiryDuration;
            }

        /**
         * Method description
         *
         * @param d
         *
         * @return
         */
        public static String toString(Duration d)
            {
            if (d == null)
                {
                return "<null>";
                }
            else
                {
                if (d.isEternal())
                    {
                    return "<eternalDuration>";
                    }
                else if (d.isZero())
                    {
                    return "<zeroDuration>";
                    }
                else if (d.getTimeUnit() == null)
                    {
                    return Long.toString(d.getDurationAmount());
                    }
                else
                    {
                    return d.getDurationAmount() + " " + d.getTimeUnit().name();
                    }
                }
            }

        /**
         * Method description
         *
         * @return
         */
        public String toString()
            {
            return "ParameterizedExpiryPolicy[created=" + toString(createdExpiryDuration) + " accessed="
                   + toString(accessedExpiryDuration) + " modified=" + toString(modifiedExpiryDuration) + "]";
            }

        private static final long serialVersionUID = -1L;

        /**
         * The {@link Duration} after which a Cache Entry will expire when created.
         */
        private Duration createdExpiryDuration;

        /**
         * The {@link javax.cache.expiry.Duration} after which a Cache Entry will expire when accessed.
         * (when <code>null</code> the current expiry duration will be used)
         */
        private Duration accessedExpiryDuration;

        /**
         * The {@link Duration} after which a Cache Entry will expire when modified.
         * (when <code>null</code> the current expiry duration will be used)
         */
        private Duration modifiedExpiryDuration;
        }

    private CacheManager                                             m_cacheMgr;
    private CacheListenerTests.MyCacheEntryListener<Integer, Integer> m_listener;
    }
