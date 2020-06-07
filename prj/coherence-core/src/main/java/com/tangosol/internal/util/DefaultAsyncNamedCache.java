/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.oracle.coherence.common.util.Options;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.CacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.PartitionedService;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import com.tangosol.internal.util.processor.CacheProcessors;
import com.tangosol.util.InvocableMap.StreamingAggregator;

import com.tangosol.util.aggregator.AsynchronousAggregator;

import com.tangosol.util.processor.AsynchronousProcessor;
import com.tangosol.util.processor.SingleEntryAsynchronousProcessor;
import com.tangosol.util.processor.StreamingAsynchronousProcessor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.CompletableFuture;

import java.util.function.Consumer;

/**
 * Default implementation of the {@link AsyncNamedCache} API.
 *
 * @author as  2015.01.15
 */
public class DefaultAsyncNamedCache<K, V>
        implements AsyncNamedCache<K, V>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct DefaultAsyncNamedCache instance.
     *
     * @param cache  the wrapped NamedCache to delegate invocations to
     */
    public DefaultAsyncNamedCache(NamedCache<K, V> cache)
        {
        // NOTE: while not strictly required any longer, we need to keep this
        // constructor in order to preserve binary compatibility with 12.2.1.0.0
        this(cache, null);
        }

    /**
     * Construct DefaultAsyncNamedCache instance.
     *
     * @param cache    the wrapped NamedCache to delegate invocations to
     * @param options  the configuration options
     */
    public DefaultAsyncNamedCache(NamedCache<K, V> cache, AsyncNamedCache.Option[] options)
        {
        m_cache   = cache;
        m_options = Options.from(AsyncNamedCache.Option.class, options);
        }

    // ---- AsyncNamedCache interface ---------------------------------------

    @Override
    public NamedCache<K, V> getNamedCache()
        {
        return m_cache;
        }

    // ---- AsyncNamedMap interface -----------------------------------------

    @Override
    public NamedMap<K, V> getNamedMap()
        {
        return m_cache;
        }

    // ---- AsyncInvocableMap interface -------------------------------------

    @Override
    public <R> CompletableFuture<R> invoke(K key,
                                InvocableMap.EntryProcessor<K, V, R> processor)
        {
        SingleEntryAsynchronousProcessor<K, V, R> asyncProcessor =
                instantiateSingleEntryAsyncProcessor(processor);

        m_cache.invoke(key, asyncProcessor);

        return asyncProcessor.getCompletableFuture();
        }

    @Override
    public <R> CompletableFuture<Map<K, R>> invokeAll(Collection<? extends K> collKeys,
                                                      InvocableMap.EntryProcessor<K, V, R> processor)
        {
        AsynchronousProcessor<K, V, R> asyncProcessor =
                instantiateMultiEntryAsyncProcessor(processor);

        m_cache.invokeAll(collKeys, asyncProcessor);

        return asyncProcessor.getCompletableFuture();
        }

    @Override
    public <R> CompletableFuture<Map<K, R>> invokeAll(Filter filter,
                                           InvocableMap.EntryProcessor<K, V, R> processor)
        {
        AsynchronousProcessor<K, V, R> asyncProcessor =
                instantiateMultiEntryAsyncProcessor(processor);

        m_cache.invokeAll(filter, asyncProcessor);

        return asyncProcessor.getCompletableFuture();
        }

    @Override
    public <R> CompletableFuture<Void> invokeAll(Collection<? extends K> collKeys,
                                                 InvocableMap.EntryProcessor<K, V, R> processor,
                                                 Consumer<? super Map.Entry<? extends K, ? extends R>> callback)
        {
        StreamingAsynchronousProcessor<K, V, R> asyncProcessor =
                instantiateStreamingAsyncProcessor(processor, callback);

        m_cache.invokeAll(collKeys, asyncProcessor);

        return asyncProcessor.getCompletableFuture();
        }

    @Override
    public <R> CompletableFuture<Void> invokeAll(Filter filter,
                                                 InvocableMap.EntryProcessor<K, V, R> processor,
                                                 Consumer<? super Map.Entry<? extends K, ? extends R>> callback)
        {
        StreamingAsynchronousProcessor<K, V, R> asyncProcessor =
                instantiateStreamingAsyncProcessor(processor, callback);

        m_cache.invokeAll(filter, asyncProcessor);

        return asyncProcessor.getCompletableFuture();
        }

    @Override
    public <R> CompletableFuture<R> aggregate(
            Collection<? extends K> collKeys, InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator)
        {
        AsynchronousAggregator<? super K, ? super V, ?, R> asyncAggregator =
                instantiateAsyncAggregator(aggregator);

        m_cache.aggregate(collKeys, asyncAggregator);

        return asyncAggregator.getCompletableFuture();
        }

    @Override
    public <R> CompletableFuture<R> aggregate(
            Filter filter, InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator)
        {
        AsynchronousAggregator<? super K, ? super V, ?, R> asyncAggregator =
                instantiateAsyncAggregator(aggregator);

        m_cache.aggregate(filter, asyncAggregator);

        return asyncAggregator.getCompletableFuture();
        }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> putAll(Map<? extends K, ? extends V> map)
        {
        CacheService service = m_cache.getCacheService();
        if (service instanceof PartitionedService)
            {
            Map<Member, Map>   mapByOwner = new HashMap<>();
            PartitionedService svcPart    = (PartitionedService) service;

            for (Map.Entry entry : map.entrySet())
                {
                Object oKey   = entry.getKey();
                Member member = svcPart.getKeyOwner(oKey);

                // member could be null here, indicating that the owning partition is orphaned
                Map mapMember = mapByOwner.get(member);
                if (mapMember == null)
                    {
                    mapMember = new HashMap();
                    mapByOwner.put(member, mapMember);
                    }
                mapMember.put(oKey, entry.getValue());
                }

            CompletableFuture[] aFuture = new CompletableFuture[mapByOwner.size()];

            int i = 0;
            for (Map mapMember : mapByOwner.values())
                {
                aFuture[i++] = invokeAll(mapMember.keySet(), CacheProcessors.putAll(mapMember));
                }

            return CompletableFuture.allOf(aFuture);
            }
        else
            {
            return AsyncNamedCache.super.putAll(map);
            }
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Create and/or configure SingleEntryAsynchronousProcessor that should be
     * executed.
     *
     * @param processor  the EntryProcessor to use
     *
     * @return a fully configured SingleEntryAsynchronousProcessor to execute
     */
    protected <R> SingleEntryAsynchronousProcessor<K, V, R> instantiateSingleEntryAsyncProcessor(
            InvocableMap.EntryProcessor<K, V, R> processor)
        {
        return processor instanceof SingleEntryAsynchronousProcessor
                ? (SingleEntryAsynchronousProcessor<K, V, R>) processor
                : new SingleEntryAsynchronousProcessor<>(
                        processor, getOrderId());
        }

    /**
     * Create and/or configure AsynchronousProcessor that should be executed.
     *
     * @param processor  the EntryProcessor to use
     *
     * @return a fully configured AsynchronousProcessor to execute
     */
    protected <R> AsynchronousProcessor<K, V, R> instantiateMultiEntryAsyncProcessor(
            InvocableMap.EntryProcessor<K, V, R> processor)
        {
        return processor instanceof AsynchronousProcessor
                ? (AsynchronousProcessor<K, V, R>) processor
                : new AsynchronousProcessor<>(
                        processor, getOrderId());
        }

    /**
     * Create and/or configure StreamingAsynchronousProcessor that should be
     * executed.
     *
     * @param processor  the EntryProcessor to use
     * @param callback   a user-defined callback that will be called for each
     *                   partial result
     *
     * @return a fully configured StreamingAsynchronousProcessor to execute
     */
    protected <R> StreamingAsynchronousProcessor<K, V, R> instantiateStreamingAsyncProcessor(
            InvocableMap.EntryProcessor<K, V, R> processor,
            Consumer<? super Map.Entry<? extends K, ? extends R>> callback)
        {
        return processor instanceof StreamingAsynchronousProcessor
                ? (StreamingAsynchronousProcessor<K, V, R>) processor
                : new StreamingAsynchronousProcessor<>(
                        processor, getOrderId(), callback);
        }

    /**
     * Create and/or configure AsynchronousAggregator that should be executed.
     *
     * @param aggregator  the EntryAggregator to use
     *
     * @return a fully configured AsynchronousProcessor to execute
     */
    @SuppressWarnings("unchecked")
    protected <R> AsynchronousAggregator<? super K, ? super V, ?, R> instantiateAsyncAggregator(
            InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator)
        {
        if (aggregator instanceof AsynchronousAggregator)
            {
            return (AsynchronousAggregator) aggregator;
            }
        if (aggregator instanceof StreamingAggregator)
            {
            return new AsynchronousAggregator<>((StreamingAggregator) aggregator, getOrderId());
            }
        throw new IllegalArgumentException("Aggregator must be a StreamingAggregator or AsynchronousAggregator");
        }

    /**
     * Return unit-of-order id.
     *
     * @return unit-of-order id
     */
    protected int getOrderId()
        {
        return m_options.get(OrderBy.class).getOrderId();
        }

    // ---- data members ----------------------------------------------------

    /**
     * The wrapped NamedCache instance to delegate invocations to.
     */
    protected final NamedCache<K, V> m_cache;

    /**
     * The configuration options.
     */
    protected final Options<AsyncNamedCache.Option> m_options;
    }
