/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cache;

import com.tangosol.internal.util.DistributedAsyncNamedCache;
import com.tangosol.internal.util.processor.BinaryProcessors;
import com.tangosol.internal.util.processor.CacheProcessors;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Binary;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AsynchronousProcessor;
import com.tangosol.util.processor.SingleEntryAsynchronousProcessor;
import com.tangosol.util.processor.StreamingAsynchronousProcessor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"rawtypes", "unchecked", "DuplicatedCode"})
public class DistributedAsyncNamedCacheTests
    {
    @BeforeClass
    @SuppressWarnings("resource")
    public static void setup() throws Exception
        {
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.clustername", "DistributedAsyncNamedCacheTests");

        s_coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);

        s_cache = s_coherence.getSession().getCache("test");
        }

    @AfterClass
    public static void cleanup()
        {
        s_coherence.close();
        Coherence.closeAll();
        }

    @Before
    public void setupTest()
        {
        s_cache.clear();
        }

    @Test
    public void shouldInvokeGet()
        {
        NamedCache                 cache = spy(s_cache);
        DistributedAsyncNamedCache async = new DistributedAsyncNamedCache<>(cache);

        async.get("foo");

        ArgumentCaptor<SingleEntryAsynchronousProcessor> captor = ArgumentCaptor.forClass(SingleEntryAsynchronousProcessor.class);
        verify(cache).invoke(eq("foo"), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.get().getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokeGetAllKeys()
        {
        NamedCache                 cache  = spy(s_cache);
        DistributedAsyncNamedCache async  = new DistributedAsyncNamedCache<>(cache);
        Set<String>                set    = Set.of("One", "Two");

        async.getAll(set);

        ArgumentCaptor<AsynchronousProcessor> captor = ArgumentCaptor.forClass(AsynchronousProcessor.class);
        verify(cache).invokeAll(eq(set), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.get().getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokeGetAllKeysWithBiConsumer()
        {
        NamedCache                 cache    = spy(s_cache);
        DistributedAsyncNamedCache async    = new DistributedAsyncNamedCache<>(cache);
        Set<String>                set      = Set.of("One", "Two");
        BiConsumer                 callback = (k, v) -> {};

        async.getAll(set, callback);

        ArgumentCaptor<StreamingAsynchronousProcessor> captor = ArgumentCaptor.forClass(StreamingAsynchronousProcessor.class);
        verify(cache).invokeAll(eq(set), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.get().getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokeGetAllKeysWithConsumer()
        {
        NamedCache                 cache    = spy(s_cache);
        DistributedAsyncNamedCache async    = new DistributedAsyncNamedCache<>(cache);
        Set<String>                set      = Set.of("One", "Two");
        Consumer                   callback = entry -> {};

        async.getAll(set, callback);

        ArgumentCaptor<StreamingAsynchronousProcessor> captor = ArgumentCaptor.forClass(StreamingAsynchronousProcessor.class);
        verify(cache).invokeAll(eq(set), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.get().getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokeGetAllFilter()
        {
        NamedCache                 cache  = spy(s_cache);
        DistributedAsyncNamedCache async  = new DistributedAsyncNamedCache<>(cache);
        Filter<String>             filter = Filters.always();

        async.getAll(filter);

        ArgumentCaptor<AsynchronousProcessor> captor = ArgumentCaptor.forClass(AsynchronousProcessor.class);
        verify(cache).invokeAll(eq(filter), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.get().getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokeGetAllFilterWithCallback()
        {
        NamedCache                 cache    = spy(s_cache);
        DistributedAsyncNamedCache async    = new DistributedAsyncNamedCache<>(cache);
        Filter<String>             filter   = Filters.always();
        BiConsumer                 callback = (k, v) -> {};

        async.getAll(filter, callback);

        ArgumentCaptor<StreamingAsynchronousProcessor> captor = ArgumentCaptor.forClass(StreamingAsynchronousProcessor.class);
        verify(cache).invokeAll(eq(filter), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.get().getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokeGetOrDefault()
        {
        NamedCache                 cache = spy(s_cache);
        DistributedAsyncNamedCache async = new DistributedAsyncNamedCache<>(cache);

        async.getOrDefault("foo", "bar");

        ArgumentCaptor<SingleEntryAsynchronousProcessor> captor = ArgumentCaptor.forClass(SingleEntryAsynchronousProcessor.class);
        verify(cache).invoke(eq("foo"), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.get().getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokePutWhenVersionCompatible()
        {
        NamedCache   cache        = spy(s_cache);
        CacheService service      = cache.getService();
        CacheService cacheService = spy(service);

        doReturn(cacheService).when(cache).getCacheService();
        doReturn(true).when(cacheService).isVersionCompatible(any(IntPredicate.class));

        DistributedAsyncNamedCache async = new DistributedAsyncNamedCache<>(cache);

        async.put("foo", "bar");

        ArgumentCaptor<SingleEntryAsynchronousProcessor> captor = ArgumentCaptor.forClass(SingleEntryAsynchronousProcessor.class);
        verify(cache).invoke(eq("foo"), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.blindPut(Binary.NO_BINARY, 0).getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokePutWhenNotVersionCompatible()
        {
        NamedCache   cache        = spy(s_cache);
        CacheService service      = cache.getService();
        CacheService cacheService = spy(service);

        doReturn(cacheService).when(cache).getCacheService();
        doReturn(false).when(cacheService).isVersionCompatible(any(IntPredicate.class));

        DistributedAsyncNamedCache async = new DistributedAsyncNamedCache<>(cache);

        async.put("foo", "bar");

        ArgumentCaptor<SingleEntryAsynchronousProcessor> captor = ArgumentCaptor.forClass(SingleEntryAsynchronousProcessor.class);
        verify(cache).invoke(eq("foo"), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.put(Binary.NO_BINARY, 0).getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokePutWithTTLWhenVersionCompatible()
        {
        NamedCache   cache        = spy(s_cache);
        CacheService service      = cache.getService();
        CacheService cacheService = spy(service);

        doReturn(cacheService).when(cache).getCacheService();
        doReturn(true).when(cacheService).isVersionCompatible(any(IntPredicate.class));

        DistributedAsyncNamedCache async = new DistributedAsyncNamedCache<>(cache);


        async.put("foo", "bar", 1000);

        ArgumentCaptor<SingleEntryAsynchronousProcessor> captor = ArgumentCaptor.forClass(SingleEntryAsynchronousProcessor.class);
        verify(cache).invoke(eq("foo"), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.blindPut(Binary.NO_BINARY, 0).getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokePutWithTTLWhenNotVersionCompatible()
        {
        NamedCache   cache        = spy(s_cache);
        CacheService service      = cache.getService();
        CacheService cacheService = spy(service);

        doReturn(cacheService).when(cache).getCacheService();
        doReturn(false).when(cacheService).isVersionCompatible(any(IntPredicate.class));

        DistributedAsyncNamedCache async = new DistributedAsyncNamedCache<>(cache);


        async.put("foo", "bar", 1000);

        ArgumentCaptor<SingleEntryAsynchronousProcessor> captor = ArgumentCaptor.forClass(SingleEntryAsynchronousProcessor.class);
        verify(cache).invoke(eq("foo"), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.put(Binary.NO_BINARY, 0).getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokePutAll()
        {
        NamedCache                 cache = spy(s_cache);
        DistributedAsyncNamedCache async = new DistributedAsyncNamedCache<>(cache);

        Map map = Map.of("Key-1", "Value-1", "Key-2", "Value-2", "Key-3", "Value-3");

        async.putAll(map);

        ArgumentCaptor<AsynchronousProcessor> captor = ArgumentCaptor.forClass(AsynchronousProcessor.class);
        verify(cache).invokeAll(eq(map.keySet()), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.putAll(Collections.emptyMap()).getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokePutAllWithTTLWhenVersionCompatible()
        {
        NamedCache   cache        = spy(s_cache);
        CacheService service      = cache.getService();
        CacheService cacheService = spy(service);

        doReturn(cacheService).when(cache).getCacheService();
        doReturn(true).when(cacheService).isVersionCompatible(any(IntPredicate.class));

        DistributedAsyncNamedCache async = new DistributedAsyncNamedCache<>(cache);

        Map map = Map.of("Key-1", "Value-1", "Key-2", "Value-2", "Key-3", "Value-3");

        async.putAll(map, 1000L);

        ArgumentCaptor<AsynchronousProcessor> captor = ArgumentCaptor.forClass(AsynchronousProcessor.class);
        verify(cache).invokeAll(eq(map.keySet()), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.putAll(Collections.emptyMap(), 1000L).getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokePutAllWithTTLWhenNotVersionCompatible()
        {
        NamedCache   cache        = spy(s_cache);
        CacheService service      = cache.getService();
        CacheService cacheService = spy(service);

        doReturn(cacheService).when(cache).getCacheService();
        doReturn(false).when(cacheService).isVersionCompatible(any(IntPredicate.class));

        DistributedAsyncNamedCache async = new DistributedAsyncNamedCache<>(cache);

        Map map = Map.of("Key-1", "Value-1", "Key-2", "Value-2", "Key-3", "Value-3");

        assertThrows(UnsupportedOperationException.class, () -> async.putAll(map, 1000L));
        }

    @Test
    public void shouldInvokePutIfAbsent() throws Exception
        {
        NamedCache                 cache = spy(s_cache);
        DistributedAsyncNamedCache async = new DistributedAsyncNamedCache<>(cache);

        CompletableFuture future  = async.putIfAbsent("foo", "value-one");
        Object            oResult = future.get(1, TimeUnit.MINUTES);
        assertThat(oResult, is(nullValue()));

        ArgumentCaptor<SingleEntryAsynchronousProcessor> captor = ArgumentCaptor.forClass(SingleEntryAsynchronousProcessor.class);
        verify(cache).invoke(eq("foo"), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) BinaryProcessors.putIfAbsent(Binary.NO_BINARY, 0).getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));

        future  = async.putIfAbsent("foo", "value-two");
        oResult = future.get(1, TimeUnit.MINUTES);
        assertThat(oResult, is("value-one"));
        }

    @Test
    public void shouldInvokeRemoveAllKeysWhenVersionCompatible()
        {
        NamedCache   cache        = spy(s_cache);
        CacheService service      = cache.getService();
        CacheService cacheService = spy(service);

        doReturn(cacheService).when(cache).getCacheService();
        doReturn(true).when(cacheService).isVersionCompatible(any(IntPredicate.class));

        DistributedAsyncNamedCache async  = new DistributedAsyncNamedCache<>(cache);
        Set<String>                keys   = Set.of("One", "Two");

        async.removeAll(keys);

        ArgumentCaptor<AsynchronousProcessor> captor = ArgumentCaptor.forClass(AsynchronousProcessor.class);
        verify(cache).invokeAll(eq(keys), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) CacheProcessors.removeWithoutResults().getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokeRemoveAllKeysWhenNotVersionCompatible()
        {
        NamedCache   cache        = spy(s_cache);
        CacheService service      = cache.getService();
        CacheService cacheService = spy(service);

        doReturn(cacheService).when(cache).getCacheService();
        doReturn(false).when(cacheService).isVersionCompatible(any(IntPredicate.class));

        DistributedAsyncNamedCache async  = new DistributedAsyncNamedCache<>(cache);
        Set<String>                keys   = Set.of("One", "Two");

        async.removeAll(keys);

        ArgumentCaptor<AsynchronousProcessor> captor = ArgumentCaptor.forClass(AsynchronousProcessor.class);
        verify(cache).invokeAll(eq(keys), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) CacheProcessors.removeBlind().getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokeRemoveAllWhenVersionCompatible()
        {
        NamedCache   cache        = spy(s_cache);
        CacheService service      = cache.getService();
        CacheService cacheService = spy(service);

        doReturn(cacheService).when(cache).getCacheService();
        doReturn(true).when(cacheService).isVersionCompatible(any(IntPredicate.class));

        DistributedAsyncNamedCache async  = new DistributedAsyncNamedCache<>(cache);
        Filter                     filter = Filters.always();

        async.removeAll(filter);

        ArgumentCaptor<AsynchronousProcessor> captor = ArgumentCaptor.forClass(AsynchronousProcessor.class);
        verify(cache).invokeAll(eq(filter), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) CacheProcessors.removeWithoutResults().getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    @Test
    public void shouldInvokeRemoveAllWhenNotVersionCompatible()
        {
        NamedCache   cache        = spy(s_cache);
        CacheService service      = cache.getService();
        CacheService cacheService = spy(service);

        doReturn(cacheService).when(cache).getCacheService();
        doReturn(false).when(cacheService).isVersionCompatible(any(IntPredicate.class));

        DistributedAsyncNamedCache async  = new DistributedAsyncNamedCache<>(cache);
        Filter                     filter = Filters.always();

        async.removeAll(filter);

        ArgumentCaptor<AsynchronousProcessor> captor = ArgumentCaptor.forClass(AsynchronousProcessor.class);
        verify(cache).invokeAll(eq(filter), captor.capture());

        Class<InvocableMap.EntryProcessor> expected = (Class<InvocableMap.EntryProcessor>) CacheProcessors.removeBlind().getClass();
        assertThat(captor.getValue().getProcessor(), is(instanceOf(expected)));
        }

    // ----- data members ---------------------------------------------------

    private static Coherence s_coherence;

    private static NamedCache<String, String> s_cache;
    }
