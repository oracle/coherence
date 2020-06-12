/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.CacheService;
import com.tangosol.net.RequestIncompleteException;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.UniversalExtractor;

import com.tangosol.util.filter.EqualsFilter;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2019.11.25
 * @since 20.06
 */
@SuppressWarnings({"unchecked", "MismatchedQueryAndUpdateOfCollection", "rawtypes"})
class NamedCacheClientTest
    {
    // ----- test methods ---------------------------------------------------

    @Test
    void shouldCallAddIndex()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.addIndex(any(ValueExtractor.class), anyBoolean(), any(Comparator.class))).thenReturn(VOID_FUTURE);

        NamedCacheClient<String, String> client    = new NamedCacheClient<>(async);
        ValueExtractor<String, Integer>  extractor = mock(ValueExtractor.class);
        client.addIndex(extractor, true, COMPARATOR);

        verify(async).addIndex(same(extractor), eq(true), same(COMPARATOR));
        }

    @Test
    void shouldCallAddIndexHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.addIndex(any(ValueExtractor.class), anyBoolean(), any(Comparator.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client    = new NamedCacheClient<>(async);
        ValueExtractor<String, Integer>  extractor = mock(ValueExtractor.class);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () ->
                client.addIndex(extractor, true, COMPARATOR));

        verify(async).addIndex(same(extractor), eq(true), same(COMPARATOR));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallAddMapListener()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.addMapListener(any(MapListener.class))).thenReturn(VOID_FUTURE);

        NamedCacheClient<String, String> client   = new NamedCacheClient<>(async);
        MapListener<String, String>      listener = mock(MapListener.class);
        client.addMapListener(listener);

        verify(async).addMapListener(same(listener));
        }

    @Test
    void shouldCallAddMapListenerHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.addMapListener(any(MapListener.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client   = new NamedCacheClient<>(async);
        MapListener<String, String>      listener = mock(MapListener.class);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () ->
                client.addMapListener(listener));

        verify(async).addMapListener(same(listener));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallAddMapListenerForFilter()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.addMapListener(any(MapListener.class), any(Filter.class), anyBoolean())).thenReturn(VOID_FUTURE);

        NamedCacheClient<String, String> client   = new NamedCacheClient<>(async);
        MapListener<String, String>      listener = mock(MapListener.class);
        client.addMapListener(listener, FILTER, true);

        verify(async).addMapListener(same(listener), same(FILTER), eq(true));
        }

    @Test
    void shouldCallAddMapListenerForFilterHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.addMapListener(any(MapListener.class), any(Filter.class), anyBoolean())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client   = new NamedCacheClient<>(async);
        MapListener<String, String>      listener = mock(MapListener.class);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () ->
                client.addMapListener(listener, FILTER, true));

        verify(async).addMapListener(same(listener), same(FILTER), eq(true));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallAddMapListenerForKey()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.addMapListener(any(MapListener.class), anyString(), anyBoolean())).thenReturn(VOID_FUTURE);

        NamedCacheClient<String, String> client   = new NamedCacheClient<>(async);
        MapListener<String, String>      listener = mock(MapListener.class);
        client.addMapListener(listener, "foo", true);

        verify(async).addMapListener(same(listener), eq("foo"), eq(true));
        }

    @Test
    void shouldCallAddMapListenerForKeyHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.addMapListener(any(MapListener.class), anyString(), anyBoolean())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client   = new NamedCacheClient<>(async);
        MapListener<String, String>      listener = mock(MapListener.class);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () ->
                client.addMapListener(listener, "foo", true));

        verify(async).addMapListener(same(listener), eq("foo"), eq(true));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallAggregate()
        {
        AsyncNamedCacheClient<String, String> async  = mock(AsyncNamedCacheClient.class);
        CompletableFuture<Integer>            future = CompletableFuture.completedFuture(1234);

        when(async.aggregate(any(InvocableMap.EntryAggregator.class))).thenReturn(future);

        NamedCacheClient<String, String> client     = new NamedCacheClient<>(async);
        InvocableMap.EntryAggregator     aggregator = mock(InvocableMap.EntryAggregator.class);
        Object result = client.aggregate(aggregator);

        verify(async).aggregate(same(aggregator));
        assertThat(result, is(1234));
        }

    @Test
    void shouldCallAggregateHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.aggregate(any(InvocableMap.EntryAggregator.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client     = new NamedCacheClient<>(async);
        InvocableMap.EntryAggregator     aggregator = mock(InvocableMap.EntryAggregator.class);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () ->
                client.aggregate(aggregator));

        verify(async).aggregate(same(aggregator));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallAggregateForFilter()
        {
        AsyncNamedCacheClient<String, String> async  = mock(AsyncNamedCacheClient.class);
        CompletableFuture<Integer>            future = CompletableFuture.completedFuture(1234);

        when(async.aggregate(any(Filter.class), any(InvocableMap.EntryAggregator.class))).thenReturn(future);

        NamedCacheClient<String, String> client     = new NamedCacheClient<>(async);
        InvocableMap.EntryAggregator     aggregator = mock(InvocableMap.EntryAggregator.class);
        Object                           result     = client.aggregate(FILTER, aggregator);

        verify(async).aggregate(same(FILTER), same(aggregator));
        assertThat(result, is(1234));
        }

    @Test
    void shouldCallAggregateForFilterHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.aggregate(any(Filter.class), any(InvocableMap.EntryAggregator.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client     = new NamedCacheClient<>(async);
        InvocableMap.EntryAggregator     aggregator = mock(InvocableMap.EntryAggregator.class);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () ->
                client.aggregate(FILTER, aggregator));

        verify(async).aggregate(same(FILTER), same(aggregator));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallAggregateForKeys()
        {
        AsyncNamedCacheClient<String, String> async  = mock(AsyncNamedCacheClient.class);
        CompletableFuture<Integer>            future = CompletableFuture.completedFuture(1234);

        when(async.aggregate(any(Collection.class), any(InvocableMap.EntryAggregator.class))).thenReturn(future);

        NamedCacheClient<String, String> client     = new NamedCacheClient<>(async);
        InvocableMap.EntryAggregator     aggregator = mock(InvocableMap.EntryAggregator.class);
        Object                           result     = client.aggregate(KEYS, aggregator);

        verify(async).aggregate(same(KEYS), same(aggregator));
        assertThat(result, is(1234));
        }

    @Test
    void shouldCallAggregateForKeysHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.aggregate(any(Collection.class), any(InvocableMap.EntryAggregator.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client     = new NamedCacheClient<>(async);
        InvocableMap.EntryAggregator     aggregator = mock(InvocableMap.EntryAggregator.class);
        List<String>                     keys       = Collections.singletonList("foo");

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () ->
                client.aggregate(keys, aggregator));

        verify(async).aggregate(same(keys), same(aggregator));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldGetAsyncClient()
        {
        AsyncNamedCacheClient<String, String> async  = mock(AsyncNamedCacheClient.class);
        NamedCacheClient<String, String>      client = new NamedCacheClient<>(async);

        assertThat(client.async(), is(sameInstance(async)));
        }

    @Test
    void shouldGetAsyncClientWithOptions()
        {
        AsyncNamedCacheClient<String, String> async  = mock(AsyncNamedCacheClient.class);
        NamedCacheClient<String, String>      client = new NamedCacheClient<>(async);

        assertThat(client.async(AsyncNamedCache.OrderBy.none()), is(sameInstance(async)));
        }

    @Test
    void shouldCallClear()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.clear()).thenReturn(VOID_FUTURE);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        client.clear();

        verify(async).clear();
        }

    @Test
    void shouldCallClearHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.clear()).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, client::clear);

        verify(async).clear();
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallContainsKey()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.containsKeyInternal(anyString())).thenReturn(TRUE_FUTURE);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        boolean                          result = client.containsKey("foo");

        assertThat(result, is(true));
        verify(async).containsKeyInternal("foo");
        }

    @Test
    void shouldCallContainsKeyHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.containsKeyInternal(anyString())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        //noinspection ResultOfMethodCallIgnored
        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.containsKey("foo"));

        verify(async).containsKeyInternal("foo");
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallContainsValue()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.containsValue(anyString())).thenReturn(TRUE_FUTURE);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        boolean                          result = client.containsValue("foo");

        assertThat(result, is(true));
        verify(async).containsValue("foo");
        }

    @Test
    void shouldCallContainsValueHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.containsValue(anyString())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        //noinspection ResultOfMethodCallIgnored
        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.containsValue("foo"));

        verify(async).containsValue("foo");
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallDestroy()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.destroy()).thenReturn(VOID_FUTURE);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        client.destroy();

        verify(async).destroy();
        }

    @Test
    void shouldCallDestroyHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.destroy()).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, client::destroy);

        verify(async).destroy();
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallEntrySet()
        {
        AsyncNamedCacheClient<String, String>             async  = mock(AsyncNamedCacheClient.class);
        Set<Map.Entry<String, String>>                    set    = new HashSet<>();
        CompletableFuture<Set<Map.Entry<String, String>>> future = CompletableFuture.completedFuture(set);

        when(async.entrySet()).thenReturn(future);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        Set<Map.Entry<String, String>>   result = client.entrySet();

        verify(async).entrySet();
        assertThat(result, is(sameInstance(set)));
        }

    @Test
    void shouldCallEntrySetHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.entrySet()).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        //noinspection ResultOfMethodCallIgnored
        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, client::entrySet);

        verify(async).entrySet();
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallEntrySetWithFilter()
        {
        AsyncNamedCacheClient<String, String>             async = mock(AsyncNamedCacheClient.class);
        Set<Map.Entry<String, String>>                    set = new HashSet<>();
        CompletableFuture<Set<Map.Entry<String, String>>> future = CompletableFuture.completedFuture(set);

        when(async.entrySet(any(Filter.class))).thenReturn(future);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        Set<Map.Entry<String, String>>   result = client.entrySet(FILTER);

        verify(async).entrySet(same(FILTER));
        assertThat(result, is(sameInstance(set)));
        }

    @Test
    void shouldCallEntrySetWithFilterHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.entrySet(any(Filter.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.entrySet(FILTER));

        verify(async).entrySet(same(FILTER));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallEntrySetWithComparator()
        {
        AsyncNamedCacheClient<String, String>             async  = mock(AsyncNamedCacheClient.class);
        Set<Map.Entry<String, String>>                    set    = new HashSet<>();
        CompletableFuture<Set<Map.Entry<String, String>>> future = CompletableFuture.completedFuture(set);

        when(async.entrySet(any(Filter.class), any(Comparator.class))).thenReturn(future);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        Set<Map.Entry<String, String>> result   = client.entrySet(FILTER, COMPARATOR);

        verify(async).entrySet(same(FILTER), same(COMPARATOR));
        assertThat(result, is(sameInstance(set)));
        }

    @Test
    void shouldCallEntrySetWithComparatorHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.entrySet(any(Filter.class), any(Comparator.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.entrySet(FILTER, COMPARATOR));

        verify(async).entrySet(same(FILTER), same(COMPARATOR));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallGet()
        {
        AsyncNamedCacheClient<String, String> async  = mock(AsyncNamedCacheClient.class);
        CompletableFuture<String>             future = CompletableFuture.completedFuture("foo-value");

        when(async.getInternal(anyString(), isNull())).thenReturn(future);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        String                           result = client.get("foo");

        verify(async).getInternal("foo", null);
        assertThat(result, is("foo-value"));
        }

    @Test
    void shouldCallGetHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.getInternal(anyString(), isNull())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.get("foo"));

        verify(async).getInternal("foo", null);
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallGetAll()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);
        Map<String, String>                   map   = Collections.singletonMap("foo", "foo-value");

        when(async.getAllInternalAsMap(anyCollection())).thenReturn(map);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        Map<String, String>              result = client.getAll(KEYS);

        verify(async).getAllInternalAsMap(KEYS);
        assertThat(result, is(sameInstance(map)));
        }

    @Test
    void shouldCallGetCacheName()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.getCacheName()).thenReturn("foo");

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        String                           result = client.getCacheName();

        //noinspection ResultOfMethodCallIgnored
        verify(async).getCacheName();
        assertThat(result, is("foo"));
        }

    @Test
    void shouldCallGetCacheService()
        {
        AsyncNamedCacheClient<String, String> async   = mock(AsyncNamedCacheClient.class);
        CacheService                          service = mock(CacheService.class);

        when(async.getCacheService()).thenReturn(service);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        CacheService                     result = client.getCacheService();

        verify(async).getCacheService();
        assertThat(result, is(sameInstance(service)));
        }

    @Test
    void shouldCallGetOrDefault()
        {
        AsyncNamedCacheClient<String, String> async  = mock(AsyncNamedCacheClient.class);
        CompletableFuture<String>             future = CompletableFuture.completedFuture("foo-value");

        when(async.getInternal(anyString(), anyString())).thenReturn(future);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        String                           result = client.getOrDefault("foo", "bar");

        verify(async).getInternal("foo", "bar");
        assertThat(result, is("foo-value"));
        }

    @Test
    void shouldCallGetOrDefaultHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.getInternal(anyString(), anyString())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.getOrDefault("foo", "bar"));

        verify(async).getInternal("foo", "bar");
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallInvoke()
        {
        AsyncNamedCacheClient<String, String>               async     = mock(AsyncNamedCacheClient.class);
        CompletableFuture<String>                           future    = CompletableFuture.completedFuture("foo-value");
        InvocableMap.EntryProcessor<String, String, String> processor = mock(InvocableMap.EntryProcessor.class);

        when(async.invoke(anyString(), any(InvocableMap.EntryProcessor.class))).thenReturn(future);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        String                           result = client.invoke("foo", processor);

        verify(async).invoke(eq("foo"), same(processor));
        assertThat(result, is("foo-value"));
        }

    @Test
    void shouldCallInvokeHandlingError()
        {
        AsyncNamedCacheClient<String, String>               async     = mock(AsyncNamedCacheClient.class);
        InvocableMap.EntryProcessor<String, String, String> processor = mock(InvocableMap.EntryProcessor.class);

        when(async.invoke(anyString(), any(InvocableMap.EntryProcessor.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.invoke("foo", processor));

        verify(async).invoke(eq("foo"), same(processor));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallInvokeAllWithFilter()
        {
        AsyncNamedCacheClient<String, String>               async     = mock(AsyncNamedCacheClient.class);
        Map                                                 map       = new HashMap();
        CompletableFuture<Map>                              future    = CompletableFuture.completedFuture(map);
        InvocableMap.EntryProcessor<String, String, String> processor = mock(InvocableMap.EntryProcessor.class);

        when(async.invokeAll(any(Filter.class), any(InvocableMap.EntryProcessor.class))).thenReturn(future);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        Map                              result = client.invokeAll(FILTER, processor);

        verify(async).invokeAll(same(FILTER), same(processor));
        assertThat(result, is(sameInstance(map)));
        }

    @Test
    void shouldCallInvokeAllWithFilterHandlingError()
        {
        AsyncNamedCacheClient<String, String>               async     = mock(AsyncNamedCacheClient.class);
        InvocableMap.EntryProcessor<String, String, String> processor = mock(InvocableMap.EntryProcessor.class);

        when(async.invokeAll(any(Filter.class), any(InvocableMap.EntryProcessor.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.invokeAll(FILTER, processor));

        verify(async).invokeAll(same(FILTER), same(processor));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallInvokeAllWithKeys()
        {
        AsyncNamedCacheClient<String, String>               async     = mock(AsyncNamedCacheClient.class);
        Map                                                 map       = new HashMap();
        CompletableFuture<Map>                              future    = CompletableFuture.completedFuture(map);
        InvocableMap.EntryProcessor<String, String, String> processor = mock(InvocableMap.EntryProcessor.class);

        when(async.invokeAll(anyCollection(), any(InvocableMap.EntryProcessor.class))).thenReturn(future);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        Map                              result = client.invokeAll(KEYS, processor);

        verify(async).invokeAll(same(KEYS), same(processor));
        assertThat(result, is(sameInstance(map)));
        }

    @Test
    void shouldCallInvokeAllWithKeysHandlingError()
        {
        AsyncNamedCacheClient<String, String>               async     = mock(AsyncNamedCacheClient.class);
        InvocableMap.EntryProcessor<String, String, String> processor = mock(InvocableMap.EntryProcessor.class);

        when(async.invokeAll(anyCollection(), any(InvocableMap.EntryProcessor.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.invokeAll(KEYS, processor));

        verify(async).invokeAll(same(KEYS), same(processor));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallIsActive()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.isActive()).thenReturn(TRUE_FUTURE);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        boolean result = client.isActive();

        verify(async).isActive();
        assertThat(result, is(true));
        }

    @Test
    void shouldCallIsActiveHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.isActive()).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, client::isActive);

        verify(async).isActive();
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallIsEmpty()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.isEmpty()).thenReturn(TRUE_FUTURE);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        boolean                          result = client.isEmpty();

        verify(async).isEmpty();
        assertThat(result, is(true));
        }

    @Test
    void shouldCallIsEmptyHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.isEmpty()).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        //noinspection ResultOfMethodCallIgnored
        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, client::isEmpty);

        verify(async).isEmpty();
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallKeySet()
        {
        AsyncNamedCacheClient<String, String> async  = mock(AsyncNamedCacheClient.class);
        Set<String>                           set    = new HashSet<>();
        CompletableFuture<Set<String>>        future = CompletableFuture.completedFuture(set);

        when(async.keySet()).thenReturn(future);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        Set<String>                      result = client.keySet();

        verify(async).keySet();
        assertThat(result, is(sameInstance(set)));
        }

    @Test
    void shouldCallKeySetHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.keySet()).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        //noinspection ResultOfMethodCallIgnored
        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, client::keySet);

        verify(async).keySet();
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallKeySetWithFilter()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);
        Set<String>                           set = new HashSet<>();
        CompletableFuture<Set<String>>        future = CompletableFuture.completedFuture(set);

        when(async.keySet(any(Filter.class))).thenReturn(future);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        Set<String>                      result = client.keySet(FILTER);

        verify(async).keySet(same(FILTER));
        assertThat(result, is(sameInstance(set)));
        }

    @Test
    void shouldCallKeySetWithFilterHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.keySet(any(Filter.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.keySet(FILTER));

        verify(async).keySet(same(FILTER));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallPut()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.putInternal(anyString(), anyString(), anyLong())).thenReturn(CompletableFuture.completedFuture("old-value"));

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        String                           result = client.put("foo", "bar");

        verify(async).putInternal("foo", "bar", CacheMap.EXPIRY_DEFAULT);
        assertThat(result, is("old-value"));
        }

    @Test
    void shouldCallPutHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.putInternal(anyString(), anyString(), anyLong())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.put("foo", "bar"));

        verify(async).putInternal("foo", "bar", CacheMap.EXPIRY_DEFAULT);
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallPutWithExpiry()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.putInternal(anyString(), anyString(), anyLong())).thenReturn(CompletableFuture.completedFuture("old-value"));

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        String                           result = client.put("foo", "bar", 1234L);

        verify(async).putInternal("foo", "bar", 1234L);
        assertThat(result, is("old-value"));
        }

    @Test
    void shouldCallPutWithExpiryHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.putInternal(anyString(), anyString(), anyLong())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.put("foo", "bar", 1234L));

        verify(async).putInternal("foo", "bar", 1234L);
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallPutAll()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);
        Map<String, String>                   map   = Collections.singletonMap("foo", "bar");

        when(async.putAll(anyMap())).thenReturn(VOID_FUTURE);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        client.putAll(map);

        verify(async).putAll(same(map));
        }

    @Test
    void shouldCallPutAllHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);
        Map<String, String>                   map   = Collections.singletonMap("foo", "bar");

        when(async.putAll(anyMap())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.putAll(map));

        verify(async).putAll(same(map));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallPutIfAbsent()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.putIfAbsent(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture("old-value"));

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        String                           result = client.putIfAbsent("foo", "bar");

        verify(async).putIfAbsent("foo", "bar");
        assertThat(result, is("old-value"));
        }

    @Test
    void shouldCallPutIfAbsentHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.putIfAbsent(anyString(), anyString())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.putIfAbsent("foo", "bar"));

        verify(async).putIfAbsent("foo", "bar");
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallRelease()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.release()).thenReturn(VOID_FUTURE);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        client.release();

        verify(async).release();
        }

    @Test
    void shouldCallReleaseHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.release()).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, client::release);

        verify(async).release();
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallRemove()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.removeInternal(anyString())).thenReturn(CompletableFuture.completedFuture("foo-value"));

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        String                           result = client.remove("foo");

        verify(async).removeInternal("foo");
        assertThat(result, is("foo-value"));
        }

    @Test
    void shouldCallRemoveHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.removeInternal(anyString())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.remove("foo"));

        verify(async).removeInternal("foo");
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallRemoveWithValue()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.removeInternal(anyString(), anyString())).thenReturn(TRUE_FUTURE);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        boolean                          result = client.remove("foo", "bar");

        verify(async).removeInternal("foo", "bar");
        assertThat(result, is(true));
        }

    @Test
    void shouldCallRemoveWithValueHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.removeInternal(anyString(), anyString())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.remove("foo", "bar"));

        verify(async).removeInternal("foo", "bar");
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallRemoveIndex()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.removeIndex(any(ValueExtractor.class))).thenReturn(VOID_FUTURE);

        NamedCacheClient<String, String> client    = new NamedCacheClient<>(async);
        ValueExtractor<String, Integer>  extractor = mock(ValueExtractor.class);
        client.removeIndex(extractor);

        verify(async).removeIndex(same(extractor));
        }

    @Test
    void shouldCallRemoveIndexHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.removeIndex(any(ValueExtractor.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client     = new NamedCacheClient<>(async);
        ValueExtractor<String, Integer>   extractor = mock(ValueExtractor.class);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.removeIndex(extractor));

        verify(async).removeIndex(same(extractor));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallRemoveMapListener()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.removeMapListener(any(MapListener.class))).thenReturn(VOID_FUTURE);

        NamedCacheClient<String, String> client   = new NamedCacheClient<>(async);
        MapListener<String, String>      listener = mock(MapListener.class);
        client.removeMapListener(listener);

        verify(async).removeMapListener(same(listener));
        }

    @Test
    void shouldCallRemoveMapListenerHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.removeMapListener(any(MapListener.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client   = new NamedCacheClient<>(async);
        MapListener<String, String>      listener = mock(MapListener.class);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () ->
                client.removeMapListener(listener));

        verify(async).removeMapListener(same(listener));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallRemoveMapListenerForFilter()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.removeMapListener(any(MapListener.class), any(Filter.class))).thenReturn(VOID_FUTURE);

        NamedCacheClient<String, String> client   = new NamedCacheClient<>(async);
        MapListener<String, String>      listener = mock(MapListener.class);
        client.removeMapListener(listener, FILTER);

        verify(async).removeMapListener(same(listener), same(FILTER));
        }

    @Test
    void shouldCallRemoveMapListenerForFilterHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.removeMapListener(any(MapListener.class), any(Filter.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client   = new NamedCacheClient<>(async);
        MapListener<String, String>      listener = mock(MapListener.class);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () ->
                client.removeMapListener(listener, FILTER));

        verify(async).removeMapListener(same(listener), same(FILTER));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallRemoveMapListenerForKey()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.removeMapListener(any(MapListener.class), anyString())).thenReturn(VOID_FUTURE);

        NamedCacheClient<String, String> client   = new NamedCacheClient<>(async);
        MapListener<String, String>      listener = mock(MapListener.class);
        client.removeMapListener(listener, "foo");

        verify(async).removeMapListener(same(listener), eq("foo"));
        }

    @Test
    void shouldCallRemoveMapListenerForKeyHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.removeMapListener(any(MapListener.class), anyString())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client   = new NamedCacheClient<>(async);
        MapListener<String, String>      listener = mock(MapListener.class);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () ->
                client.removeMapListener(listener, "foo"));

        verify(async).removeMapListener(same(listener), eq("foo"));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallReplace()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.invoke(anyString(), any(InvocableMap.EntryProcessor.class))).thenReturn(CompletableFuture.completedFuture("old-value"));

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        String                           result = client.replace("foo", "bar");

        verify(async).invoke(eq("foo"), any(InvocableMap.EntryProcessor.class));
        assertThat(result, is("old-value"));
        }

    @Test
    void shouldCallReplaceHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.invoke(anyString(), any(InvocableMap.EntryProcessor.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.replace("foo", "bar"));

        verify(async).invoke(eq("foo"), any(InvocableMap.EntryProcessor.class));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallReplaceWithPreviousValue()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.replace(anyString(), anyString(), anyString())).thenReturn(TRUE_FUTURE);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        boolean                          result = client.replace("foo", "bar", "old-value");

        verify(async).replace("foo", "bar", "old-value");
        assertThat(result, is(true));
        }

    @Test
    void shouldCallReplaceWithPreviousValueHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.replace(anyString(), anyString(), anyString())).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.replace("foo", "bar", "old-value"));

        verify(async).replace("foo", "bar", "old-value");
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallSize()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.size()).thenReturn(CompletableFuture.completedFuture(19));

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        int                              result = client.size();

        verify(async).size();
        assertThat(result, is(19));
        }

    @Test
    void shouldCallSizeHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.size()).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, client::size);

        verify(async).size();
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallTruncate()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.truncate()).thenReturn(VOID_FUTURE);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        client.truncate();

        verify(async).truncate();
        }

    @Test
    void shouldCallTruncateHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.truncate()).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, client::truncate);

        verify(async).truncate();
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallValues()
        {
        AsyncNamedCacheClient<String, String> async  = mock(AsyncNamedCacheClient.class);
        Set<String>                           set    = new HashSet<>();
        CompletableFuture<Collection<String>> future = CompletableFuture.completedFuture(set);

        when(async.values()).thenReturn(future);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        Collection<String>               result = client.values();

        verify(async).values();
        assertThat(result, is(sameInstance(set)));
        }

    @Test
    void shouldCallValuesHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.values()).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        //noinspection ResultOfMethodCallIgnored
        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, client::values);

        verify(async).values();
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallValuesWithFilter()
        {
        AsyncNamedCacheClient<String, String> async  = mock(AsyncNamedCacheClient.class);
        Set<String>                           set    = new HashSet<>();
        CompletableFuture<Collection<String>> future = CompletableFuture.completedFuture(set);

        when(async.values(any(Filter.class))).thenReturn(future);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        Collection<String>               result = client.values(FILTER);

        verify(async).values(same(FILTER));
        assertThat(result, is(sameInstance(set)));
        }

    @Test
    void shouldCallValuesWithFilterHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.values(any(Filter.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.values(FILTER));

        verify(async).values(same(FILTER));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    @Test
    void shouldCallValuesWithComparator()
        {
        AsyncNamedCacheClient<String, String> async  = mock(AsyncNamedCacheClient.class);
        Set<String>                           set    = new HashSet<>();
        CompletableFuture<Collection<String>> future = CompletableFuture.completedFuture(set);

        when(async.valuesInternal(any(Filter.class), any(Comparator.class))).thenReturn(future);

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);
        Collection<String>               result = client.values(FILTER, COMPARATOR);

        verify(async).valuesInternal(same(FILTER), same(COMPARATOR));
        assertThat(result, is(sameInstance(set)));
        }

    @Test
    void shouldCallValuesWithComparatorHandlingError()
        {
        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        when(async.valuesInternal(any(Filter.class), any(Comparator.class))).thenReturn(failedFuture());

        NamedCacheClient<String, String> client = new NamedCacheClient<>(async);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class, () -> client.values(FILTER, COMPARATOR));

        verify(async).valuesInternal(same(FILTER), same(COMPARATOR));
        assertThat(rootCause(ex), is(sameInstance(ERROR)));
        }

    // ----- helper methods -------------------------------------------------

    protected <T> CompletableFuture<T> failedFuture()
        {
        return failedFuture(ERROR);
        }

    protected <T> CompletableFuture<T> failedFuture(Throwable t)
        {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(t);
        return future;
        }

    protected Throwable rootCause(Throwable t)
        {
        Throwable cause = t.getCause();
        if (cause == null)
            {
            return t;
            }
        return rootCause(cause);
        }

    // ----- constants ------------------------------------------------------

    protected static final Void VOID = null;

    protected static final CompletableFuture<Void> VOID_FUTURE = CompletableFuture.completedFuture(VOID);

    protected static final CompletableFuture<Boolean> TRUE_FUTURE = CompletableFuture.completedFuture(true);

    protected static final Throwable ERROR = new RuntimeException("Computer says No!");

    protected static final Filter<String> FILTER = new EqualsFilter();

    protected static final Comparator COMPARATOR = new UniversalExtractor();

    protected static final Collection<String> KEYS = Collections.singleton("foo");
    }
