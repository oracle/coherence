/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package aggregator.async;


import aggregator.AbstractEntryAggregatorTests;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.StreamingAggregator;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.AbstractAggregator;
import com.tangosol.util.aggregator.AsynchronousAggregator;
import com.tangosol.util.extractor.IdentityExtractor;

import org.junit.Ignore;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * A collection of functional tests for the various async
 * {@link InvocableMap.EntryAggregator} implementations.
 * <p>
 * Additionally, this test performs other operations such as put, get, getAll, etc.
 * using the async flavor of the API.
 *
 * @author bb  2015.04.06
 *
 * @see InvocableMap
 */
public abstract class AbstractAsyncEntryAggregatorTests
        extends AbstractEntryAggregatorTests
    {
    // ----- constructors ---------------------------------------------------
    /**
     * Create a new AbstractAsyncEntryAggregatorTests that will use the cache with
     * the given name in all test methods.
     *
     * @param sCache  the test cache name
     */
    public AbstractAsyncEntryAggregatorTests(String sCache)
        {
        super(sCache);
        }

    @Override
    protected NamedCache getNamedCache()
        {
        String     sName         = getCacheName();
        NamedCache cachePrevious = getNamedCache(sName);

        // ensure no cache context (i.e. index), outstanding async operations pending from previous test scenario
        getFactory().destroyCache(cachePrevious);
        Eventually.assertDeferred("ensure cache " + sName + " is destroyed", () -> cachePrevious.isDestroyed(), is(true));

        NamedCache cache = getNamedCache(sName);
        assertNotEquals("must be different caches since cachePrevious was destroyed", cachePrevious, cache);
        assertTrue("assert new cache " + sName + " is empty: size=" + cache.size(), cache.isEmpty());

        return new AsyncNamedCacheWrapper<>(cache);
        }

    /**
     * Test custom filter serialization failure
     */
    @Test(expected=CompletionException.class)
    public void testFilterSerializationFailure()
        {
        StreamingAggregator aggr = new CustomAggregator();

        NamedCache cache = getNamedCache();

        Map map = new HashMap();
        for (int i = 1; i <= 100000; ++i)
            {
            map.put(Integer.valueOf(i), Integer.valueOf(i));
            }

        getNamedCache(getCacheName()).putAll(map);

        cache.aggregate(new CustomFilter(/*failOnClient*/ true), aggr);
        }


    /**
     * Test custom filter aggregation failure
     */
    @Test(expected=CompletionException.class)
    public void testFilterAggregatorFailure()
        {
        StreamingAggregator aggr = new CustomAggregator();

        NamedCache cache = getNamedCache();

        Map map = new HashMap();
        for (int i = 1; i <= 100000; ++i)
            {
            map.put(Integer.valueOf(i), Integer.valueOf(i));
            }

        getNamedCache(getCacheName()).putAll(map);

        cache.aggregate(new CustomFilter(/*failOnClient*/ false), aggr);
        }

    /**
     * Test custom key aggregation failure
     */
    @Test(expected=CompletionException.class)
    public void testAggregateKeysFailure()
        {
        StreamingAggregator aggr = new CustomAggregator();

        NamedCache cacheAsync = getNamedCache();

        Map map = new HashMap();
        for (int i = 1; i <= 100000; ++i)
            {
            map.put(Integer.valueOf(i), Integer.valueOf(i));
            }

        getNamedCache(getCacheName()).putAll(map);

        cacheAsync.aggregate(map.keySet(), aggr);

        }

    /**
     * Test custom key serialization failure
     */
    @Test(expected=RuntimeException.class)
    public void testCustomKeyFailure()
        {
        StreamingAggregator aggr = new CustomAggregator();

        NamedCache cacheAsync = getNamedCache();

        Map map = new HashMap();
        for (int i = 1; i <= 100000; ++i)
            {
            map.put(Integer.valueOf(i), Integer.valueOf(i));
            }

        getNamedCache(getCacheName()).putAll(map);
        cacheAsync.aggregate(Collections.singleton(new CustomKey()), aggr);
        }

    /**
     * Test custom key aggregation timeout failure
     */
    @Test(expected=TimeoutException.class)
    public void testAggregationTimeout() throws Exception
        {
        Map map = new HashMap();
        for (int i = 1; i <= 100000; ++i)
            {
            map.put(Integer.valueOf(i), Integer.valueOf(i));
            }

        getNamedCache(getCacheName()).putAll(map);

        AsynchronousAggregator asyncAggr = new AsynchronousAggregator(new CustomAggregator(5000));
        getNamedCache(getCacheName()).aggregate(Collections.singleton(Integer.valueOf(1)), asyncAggr);
        asyncAggr.get(1000, TimeUnit.MILLISECONDS);
        }

        @Override
        @Test
        @Ignore
        public void bigDecimalAverageWithScaleAndNullRoundingMode()
            {
            }

        protected static class CustomKey
            implements ExternalizableLite
        {
        public void readExternal(DataInput in) throws IOException
            {
            throw new RuntimeException("Test serialization failure");
            }

        public void writeExternal(DataOutput out) throws IOException
            {
            throw new RuntimeException("Test serialization failure");
            }
        }

    public static class CustomFilter
            implements Filter, ExternalizableLite
        {

        public CustomFilter(boolean fFailOnClient)
            {
            m_fFailOnClient = fFailOnClient;
            }

        public boolean evaluate(Object o)
            {
            throw new UnsupportedOperationException();
            }

        public void readExternal(DataInput in)
                throws IOException
            {
            }

        public void writeExternal(DataOutput out)
                throws IOException
            {
            if (m_fFailOnClient)
                {
                throw new UnsupportedOperationException();
                }
            }

        private boolean m_fFailOnClient;
        }

    public static class CustomAggregator
            extends AbstractAggregator
        {
        public CustomAggregator()
            {
            super(IdentityExtractor.INSTANCE);
            }

        public CustomAggregator(int nTimeoutMillis)
            {
            super(IdentityExtractor.INSTANCE);
            m_nTimeout = nTimeoutMillis;
            }

        @Override
        public InvocableMap.StreamingAggregator supply()
            {
            return this;
            }

        @Override
        protected void init(boolean fFinal)
            {
            }

        public boolean accumulate(InvocableMap.Entry entry)
            {
            if (m_nTimeout > 0)
                {
                try
                    {
                    Thread.sleep(m_nTimeout);
                    }
                catch (InterruptedException e) { }
                }
            else if (entry.getKey().equals(Integer.valueOf(1)))
                {
                throw new UnsupportedOperationException();
                }

            return true;
            }

        protected void process(Object o, boolean fFinal)
            {
            }

        @Override
        protected Object finalizeResult(boolean fFinal)
            {
            return null;
            }

        public void readExternal(DataInput in)
                throws IOException
            {
            m_nTimeout = in.readInt();
            }

        public void writeExternal(DataOutput out)
                throws IOException
            {
            out.writeInt(m_nTimeout);
            }

        private int m_nTimeout;
        }

    /**
     * NamedCache Wrapper on top of a async named cache.
     * Not all methods in the wrapper are async.
     */
    protected static class AsyncNamedCacheWrapper<K, V>
            implements NamedCache<K, V>
        {

        public AsyncNamedCacheWrapper(NamedCache cache)
            {
            f_cache      = cache;
            f_asyncCache = cache.async();
            }

        @Override
        public String getCacheName()
            {
            return f_cache.getCacheName();
            }

        @Override
        public CacheService getCacheService()
            {
            return f_cache.getCacheService();
            }

        @Override
        public boolean isActive()
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public void release()
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public void destroy()
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public V put(K key, V value, long cMillis)
            {
            CompletableFuture future = f_asyncCache.put(key, value, cMillis);
            return (V) future.join();
            }

        @Override
        public void clear()
            {
            f_cache.clear();
            Eventually.assertDeferred("ensure cache " + f_cache.getCacheName() + " is cleared", () -> f_cache.isEmpty(), is(true));
            }

        @Override
        public Map getAll(Collection colKeys)
            {
            CompletableFuture future = f_asyncCache.getAll(colKeys);
            return (Map) future.join();
            }

        @Override
        public boolean lock(Object oKey, long cWait)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean lock(Object oKey)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean unlock(Object oKey)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public <R> R invoke(K key, EntryProcessor<K, V, R> processor)
            {
            CompletableFuture future = f_asyncCache.invoke(key, processor);
            return (R) future.join();
            }

        @Override
        public Map invokeAll(Collection collKeys, EntryProcessor processor)
            {
            CompletableFuture future = f_asyncCache.invokeAll(collKeys, processor);
            return (Map) future.join();
            }

        @Override
        public Map invokeAll(Filter filter, EntryProcessor processor)
            {
            CompletableFuture future = f_asyncCache.invokeAll(filter, processor);
            return (Map) future.join();
            }

        @Override
        public <R> R aggregate(Collection<? extends K> collKeys, EntryAggregator<? super K, ? super V, R> aggregator)
            {
            if (aggregator instanceof StreamingAggregator)
                {
                CompletableFuture<R> future = f_asyncCache.aggregate(collKeys, (StreamingAggregator) aggregator);
                return future.join();
                }
            else
                {
                return f_cache.aggregate(collKeys, aggregator);
                }
            }

        @Override
        public <R> R aggregate(Filter filter, EntryAggregator<? super K, ? super V, R> aggregator)
            {
            if (aggregator instanceof StreamingAggregator)
                {
                CompletableFuture<R> future = f_asyncCache.aggregate(filter, (StreamingAggregator) aggregator);
                return future.join();
                }
            else
                {
                return f_cache.aggregate(filter, aggregator);
                }
            }

        @Override
        public void addMapListener(MapListener listener)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public void removeMapListener(MapListener listener)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public void addMapListener(MapListener listener, Object key, boolean fLite)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public void removeMapListener(MapListener listener, Object key)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public void addMapListener(MapListener listener, Filter filter, boolean fLite)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public void removeMapListener(MapListener listener, Filter filter)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public Set keySet(Filter filter)
            {
            CompletableFuture<Set<K>> future = f_asyncCache.keySet(filter);
            return future.join();
            }

        @Override
        public Set<Map.Entry<K, V>> entrySet(Filter filter)
            {
            CompletableFuture<Set<Map.Entry<K, V>>> future = f_asyncCache.entrySet(filter);
            return future.join();
            }

        @Override
        public Set<Map.Entry<K, V>>  entrySet(Filter filter, Comparator comparator)
            {
            CompletableFuture<Set<Map.Entry<K, V>>> future = f_asyncCache.entrySet(filter, comparator);
            return future.join();
            }

        @Override
        public void addIndex(ValueExtractor extractor, boolean fOrdered, Comparator comparator)
            {
            f_cache.addIndex(extractor, fOrdered, comparator);
            }

        @Override
        public void removeIndex(ValueExtractor extractor)
            {
            f_cache.removeIndex(extractor);
            }

        @Override
        public int size()
            {
            return f_cache.size();
            }

        @Override
        public boolean isEmpty()
            {
            return f_cache.isEmpty();
            }

        @Override
        public boolean containsKey(Object key)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean containsValue(Object value)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public V get(Object key)
            {
            CompletableFuture future = f_asyncCache.get((K) key);
            return (V) future.join();
            }

        @Override
        public Object put(Object key, Object value)
            {
            CompletableFuture future = f_asyncCache.put((K) key, (V) value);
            return future.join();
            }

        @Override
        public V remove(Object key)
            {
            CompletableFuture<V> future = f_asyncCache.remove((K) key);
            return future.join();
            }

        @Override
        public void putAll(Map m)
            {
            CompletableFuture future = f_asyncCache.putAll(m);
            future.join();
            }

        @Override
        public Set keySet()
            {
            CompletableFuture<Set<K>> future = f_asyncCache.keySet();
            return future.join();
            }

        @Override
        public Collection values()
            {
            CompletableFuture<Collection<V>> future = f_asyncCache.values();
            return future.join();
            }

        @Override
        public Set<Map.Entry<K, V>> entrySet()
            {
            CompletableFuture<Set<Map.Entry<K, V>>> future = f_asyncCache.entrySet();
            return future.join();
            }

        protected final NamedCache<K, V> f_cache;
        protected final AsyncNamedCache<K, V> f_asyncCache;
        }
    }
