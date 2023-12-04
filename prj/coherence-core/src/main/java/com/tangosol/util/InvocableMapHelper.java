/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.base.NonBlocking;

import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.io.Serializer;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.LocalCache;

import com.tangosol.util.comparator.EntryComparator;
import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.AbstractUpdater;
import com.tangosol.util.extractor.IndexAwareExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EntryFilter;
import com.tangosol.util.filter.IndexAwareFilter;
import com.tangosol.util.filter.LimitFilter;

import com.tangosol.util.processor.AsynchronousProcessor;
import com.tangosol.util.processor.SingleEntryAsynchronousProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;


/**
* Helper methods for InvocableMap implementations and Filter related
* evaluation.
*
* @author gg  2005.10.24
* @since Coherence 3.1
*/
public abstract class InvocableMapHelper
        extends Base
    {
    /**
     * Invoke the specified EntryProcessor asynchronously.
     *
     * @param cache  the cache to invoke against
     * @param key    the key to invoke upon
     * @param proc   the processor to invoke
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R> the result type
     *
     * @return a CompletableFuture which will contain the result
     *
     * @deprecated As of Coherence 14.1.1, use enhanced
     * {@link #invokeAsync(NamedCache, Object, int, InvocableMap.EntryProcessor, BiConsumer[])}.
     */
    public static <K, V, R> CompletableFuture<R> invokeAsync(NamedCache<K, V> cache, K key, InvocableMap.EntryProcessor<K, V, R> proc)
        {
        return invokeAsync(cache, key, Thread.currentThread().hashCode(), proc);
        }

    /**
     * Invoke the specified EntryProcessor asynchronously.
     * <p>
     * The continuation which will be invoked when the processor completes and most importantly on the thread
     * on which the operation completes which is not something that can be guaranteed if the continuation is
     * applied via the returned CompletableFuture.
     *
     * @param cache          the cache to invoke against
     * @param key            the key to invoke upon
     * @param nOrderId       the unit of order
     * @param proc           the processor to invoke
     * @param continuations  continuations which will be invoked when the operation completes
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R> the result type
     *
     * @return a CompletableFuture which will contain the result
     */
    @SafeVarargs
    public static <K, V, R> CompletableFuture<R> invokeAsync(NamedCache<K, V> cache, K key, int nOrderId,
            InvocableMap.EntryProcessor<K, V, R> proc, BiConsumer<? super R, ? super Throwable>... continuations)
        {
        return invokeAsync(cache, key, nOrderId, proc, Daemons.commonPool(), continuations);
        }

    /**
     * Invoke the specified EntryProcessor asynchronously.
     * <p>
     * The continuation which will be invoked when the processor completes and most importantly on the thread
     * on which the operation completes which is not something that can be guaranteed if the continuation is
     * applied via the returned CompletableFuture.
     *
     * @param cache          the cache to invoke against
     * @param key            the key to invoke upon
     * @param nOrderId       the unit of order
     * @param proc           the processor to invoke
     * @param executor       an optional {@link Executor} to use to complete the returned future
     * @param continuations  continuations which will be invoked when the operation completes
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R> the result type
     *
     * @return a CompletableFuture which will contain the result
     */
    @SafeVarargs
    public static <K, V, R> CompletableFuture<R> invokeAsync(NamedCache<K, V> cache, K key, int nOrderId,
            InvocableMap.EntryProcessor<K, V, R> proc, Executor executor,
            BiConsumer<? super R, ? super Throwable>... continuations)
        {
        SingleEntryAsynchronousProcessor<K, V, R> procAsync =
                new SingleEntryAsynchronousProcessor<>(proc, nOrderId, executor);

        CompletableFuture<R> future = procAsync.getCompletableFuture();
        for (BiConsumer<? super R, ? super Throwable> continuation : continuations)
            {
            // must occur before calling invoke, this is the entire point of this method
            future = future.whenComplete(continuation);
            }

        cache.invoke(key, procAsync);

        if (NonBlocking.isNonBlockingCaller())
            {
            procAsync.flush();
            }

        return future;
        }

    /**
     * Invoke the specified EntryProcessor asynchronously.
     * <p>
     * The continuation which will be invoked when the processor completes and most importantly on the thread
     * on which the operation completes which is not something that can be guaranteed if the continuation is
     * applied via the returned CompletableFuture.
     *
     * @param cache          the cache to invoke against
     * @param setKey         the set of keys to invoke upon
     * @param nOrderId       the unit of order
     * @param proc           the processor to invoke
     * @param continuations  continuations which will be invoked when the operation completes
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R> the result type
     *
     * @return a CompletableFuture which will contain the result
     */
    @SafeVarargs
    public static <K, V, R> CompletableFuture<Map<K, R>> invokeAllAsync(NamedCache<K, V> cache, Collection<? extends K> setKey,
            int nOrderId, InvocableMap.EntryProcessor<K, V, R> proc, Executor executor,
            BiConsumer<? super Map<? extends K, ? extends R>, ? super Throwable>... continuations)
        {
        AsynchronousProcessor<K, V, R> procAsync = new AsynchronousProcessor<>(proc, nOrderId, executor);

        CompletableFuture<Map<K, R>> future = procAsync.getCompletableFuture();
        for (BiConsumer<? super Map<K, R>, ? super Throwable> continuation : continuations)
            {
            // must occur before calling invoke, this is the entire point of this method
            future = future.whenComplete(continuation::accept);
            }

        cache.invokeAll(setKey, procAsync);

        if (NonBlocking.isNonBlockingCaller())
            {
            procAsync.flush();
            }

        return future;
        }

    /**
     * Invoke the specified EntryProcessor asynchronously.
     * <p>
     * The continuation which will be invoked when the processor completes and most importantly on the thread
     * on which the operation completes which is not something that can be guaranteed if the continuation is
     * applied via the returned CompletableFuture.
     *
     * @param cache          the cache to invoke against
     * @param setKey         the set of keys to invoke upon
     * @param funcOrder      function to compute unit of order based on th ekey
     * @param proc           the processor to invoke
     * @param continuations  continuations which will be invoked when the operation completes
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R> the result type
     *
     * @return a CompletableFuture which will contain the result
     */
    @SafeVarargs
    public static <K, V, R> CompletableFuture<Map<K, R>> invokeAllAsync(NamedCache<K, V> cache, Collection<? extends K> setKey,
            ToIntFunction<K> funcOrder, InvocableMap.EntryProcessor<K, V, R> proc,
            BiConsumer<? super Map<? extends K, ? extends R>, ? super Throwable>... continuations)
        {
        return invokeAllAsync(cache, setKey, funcOrder, proc, Daemons.commonPool(), continuations);
        }

    /**
     * Invoke the specified EntryProcessor asynchronously.
     * <p>
     * The continuation which will be invoked when the processor completes and most importantly on the thread
     * on which the operation completes which is not something that can be guaranteed if the continuation is
     * applied via the returned CompletableFuture.
     *
     * @param cache          the cache to invoke against
     * @param setKey         the set of keys to invoke upon
     * @param funcOrder      function to compute unit of order based on the key
     * @param proc           the processor to invoke
     * @param executor       an optional {@link Executor} to use to complete the future
     * @param continuations  continuations which will be invoked when the operation completes
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R> the result type
     *
     * @return a CompletableFuture which will contain the result
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <K, V, R> CompletableFuture<Map<K, R>> invokeAllAsync(NamedCache<K, V> cache, Collection<? extends K> setKey,
            ToIntFunction<K> funcOrder, InvocableMap.EntryProcessor<K, V, R> proc, Executor executor,
            BiConsumer<? super Map<? extends K, ? extends R>, ? super Throwable>... continuations)
        {
        Object[]                                    aKey    = setKey.toArray();
        SingleEntryAsynchronousProcessor<K, V, R>[] aProc   = new SingleEntryAsynchronousProcessor[aKey.length];
        CompletableFuture<R>[]                      aFuture = new CompletableFuture[aKey.length];
        for (int i = 0; i < aKey.length; ++i)
            {
            aProc  [i] = new SingleEntryAsynchronousProcessor<>(proc, funcOrder.applyAsInt((K) aKey[i]), executor);
            aFuture[i] = aProc[i].getCompletableFuture();
            }

        CompletableFuture<Map<K, R>> future = CompletableFuture.allOf(aFuture).thenApply((nil) ->
              {
              Map<K, R> mapResult = new HashMap<>();

              for (int i = 0; i < aKey.length; ++i)
                  {
                  mapResult.put((K) aKey[i], aFuture[i].getNow(null));
                  }
              return mapResult;
              });

        for (BiConsumer<? super Map<K, R>, ? super Throwable> continuation : continuations)
            {
            // must occur before calling invoke, this is the entire point of this method
            future = future.whenComplete(continuation::accept);
            }

        for (int i = 0; i < aKey.length; ++i)
            {
            cache.invoke((K) aKey[i], aProc[i]);

            if (NonBlocking.isNonBlockingCaller())
                {
                aProc[i].flush();
                }
            }

        return future;
        }

    /**
    * Invoke the passed EntryProcessor against the specified Entry.
    * The invocation is made thread safe by locking the corresponding key
    * on the map.
    *
    * @param map    the ConcurrentMap that the EntryProcessor works against
    * @param entry  the InvocableMap.Entry to process; it is not required to
    *               exist within the Map
    * @param agent  the EntryProcessor to use to process the specified key
    *
    * @return the result of the invocation as returned from the
    *         EntryProcessor
    */
    public static <K, V, R> R invokeLocked(ConcurrentMap<K, V> map, InvocableMap.Entry<K, V> entry,
                                      InvocableMap.EntryProcessor<K, V, R> agent)
        {
        K oKey = entry.getKey();
        map.lock(oKey, -1L);
        try
            {
            return agent.process(entry);
            }
        finally
            {
            map.unlock(oKey);
            }
        }

    /**
    * Invoke the passed EntryProcessor against the entries specified by the
    * passed map and entries.
    * The invocation is made thread safe by locking the corresponding keys
    * on the map. If an attempt to lock all the entries at once fails, they
    * will be processed individually one-by-one.
    *
    * @param map         the ConcurrentMap that the EntryProcessor works against
    * @param setEntries  a set of InvocableMap.Entry objects to process
    * @param agent       the EntryProcessor to use to process the specified keys
    *
    * @return a Map containing the results of invoking the EntryProcessor
    *         against each of the specified entry
    */
    public static <K, V, R> Map<K, R> invokeAllLocked(
                                        ConcurrentMap<K, V> map,
                                        Set<? extends InvocableMap.Entry<K, V>> setEntries,
                                        InvocableMap.EntryProcessor<K, V, R> agent)
        {
        Set setKeys = ConverterCollections.getSet(setEntries,
                ENTRY_TO_KEY_CONVERTER, NullImplementation.getConverter());

        // try to lock them all at once
        List listLocked = lockAll(map, setKeys, 0l);
        if (listLocked == null)
            {
            // the attempt failed; do it one-by-one
            Map<K, R> mapResult = new HashMap(setEntries.size());
            for (InvocableMap.Entry<K, V> entry : setEntries)
                {
                mapResult.put(entry.getKey(), invokeLocked(map, entry, agent));
                }
            return mapResult;
            }
        else
            {
            try
                {
                return agent.processAll(setEntries);
                }
             finally
                {
                unlockAll(map, listLocked);
                }
            }
        }

    /**
    * Attempt to lock all the specified keys within a specified period of time.
    *
    * @param map       the ConcurrentMap to use
    * @param collKeys  a collection of keys to lock
    * @param cWait     the number of milliseconds to continue trying to obtain
    *                  locks; pass zero to return immediately; pass -1 to block
    *                  the calling thread until the lock could be obtained
    * @return a List containing all the locked keys in the order opposite
    *          to the locking order (LIFO); null if timeout has occurred
    */
    public static List lockAll(ConcurrentMap map, Collection collKeys, long cWait)
        {
        // remove the duplicates
        Set setKeys = collKeys instanceof Set ?
            (Set) collKeys : new HashSet(collKeys);

        // copy the keys into a list to fully control the iteration order
        List    listKeys   = new ArrayList(setKeys);
        List    listLocked = new LinkedList();
        int     cKeys      = listKeys.size();
        boolean fSuccess   = true;

        do
            {
            long cWaitNext = cWait; // allow blocking wait for the very first key
            for (int i = 0; i < cKeys; i++)
                {
                Object oKey = listKeys.get(i);

                if (fSuccess = map.lock(oKey, cWaitNext))
                    {
                    // add the last locked item into the front of the locked
                    // list so it behaves as a stack (FILO strategy)
                    listLocked.add(0, oKey);

                    // to prevent a deadlock don't wait afterwards
                    cWaitNext = 0l;
                    }
                else
                    {
                    if (i == 0)
                        {
                        // the very first key cannot be locked -- timeout
                        return null;
                        }

                    // unlock all we hold and try again
                    for (Iterator iterLocked = listLocked.iterator(); iterLocked.hasNext();)
                        {
                        map.unlock(iterLocked.next());
                        }
                    listLocked.clear();

                    // move the "offending" key to the top of the list
                    // so next iteration we will attempt to lock it first
                    listKeys.remove(i);
                    listKeys.add(0, oKey);
                    }
                }
            } while (!fSuccess);

        return listLocked;
        }

    /**
    * Unlock all the specified keys.
    *
    * @param map       the ConcurrentMap to use
    * @param collKeys  a collection of keys to unlock
    */
    public static void unlockAll(ConcurrentMap map, Collection collKeys)
        {
        for (Iterator iterLocked = collKeys.iterator(); iterLocked.hasNext();)
            {
            map.unlock(iterLocked.next());
            }
        }

    /**
    * Create a SimpleEntry object for the specified map and the key.
    *
    * @param map   the ConcurrentMap to create entries for
    * @param key   the key to create an entry for; the key is not
    *              required to exist within the Map
    * @return a SimpleEntry object
    */
    public static <K, V> SimpleEntry<K, V> makeEntry(Map<K, V> map, K key)
        {
        return new SimpleEntry<>(map, key, false);
        }

    /**
    * Create a set of SimpleEntry objects for the specified map and
    * the key collection.
    *
    * @param map       the Map to create entries for
    * @param collKeys  collection of keys to create entries for; these keys
    *                   are not required to exist within the Map
    * @param fReadOnly if true, the returned entries will be marked as
    *                   read-only
    * @return a Set of SimpleEntry objects
    */
    public static <K, V> Set<InvocableMap.Entry<K, V>> makeEntrySet(
            Map<K, V> map, Collection<? extends K> collKeys, boolean fReadOnly)
        {
        Set<InvocableMap.Entry<K, V>> setEntries = new HashSet<>(collKeys.size());
        for (K key : collKeys)
            {
            setEntries.add(new SimpleEntry<>(map, key, fReadOnly));
            }
        return setEntries;
        }

    /**
    * Create a set of read-only SimpleEntry objects for the specified collection
    * of <tt>Map.Entry</tt> objects.
    *
    * @param collEntries  collection of Map.Entry objects to create SimpleEntry
    *                     objects for
    *
    * @return a Set of SimpleEntry objects
    */
    public static <K, V> Set<InvocableMap.Entry<K, V>> makeEntrySet(Collection<? extends Map.Entry<K, V>> collEntries)
        {
        Set<InvocableMap.Entry<K, V>> setEntries = new HashSet<>(collEntries.size());
        for (Map.Entry<K, V> entry : collEntries)
            {
            setEntries.add(new SimpleEntry<>(entry.getKey(), entry.getValue()));
            }
        return setEntries;
        }

    /**
    * Create a set of {@link InvocableMap.Entry} objects using the specified
    * collection of <tt>Map.Entry</tt> objects.
    *
    * @param map          the parent Map for the entries
    * @param collEntries  collection of Map.Entry objects to copy from
    * @param fReadOnly    if true, the returned entries will be marked as
    *                     read-only
    *
    * @return a Set of SimpleEntry objects
    */
    public static <K, V> Set<InvocableMap.Entry<K, V>> duplicateEntrySet(
            Map<K, V> map, Collection<? extends Map.Entry<K, V>> collEntries, boolean fReadOnly)
        {
        Set<InvocableMap.Entry<K, V>> setEntries = new HashSet<>(collEntries.size());
        for (Map.Entry<K, V> entry : collEntries)
            {
            setEntries.add(new SimpleEntry<>(map, entry.getKey(), entry.getValue(), fReadOnly));
            }
        return setEntries;
        }

    /**
    * Check if the entry passes the filter evaluation.
    *
    * @param filter  the filter to evaluate against
    * @param entry   a key value pair to filter
    *
    * @return true   if the entry passes the filter, false otherwise
    */
    public static <K, V> boolean evaluateEntry(Filter filter, Map.Entry<K, V> entry)
        {
        return filter instanceof EntryFilter ?
                ((EntryFilter) filter).evaluateEntry(entry) :
                filter.evaluate(entry.getValue());
        }

    /**
    * Check if an entry, expressed as a key and value, passes the filter
    * evaluation.
    *
    * @param filter  the filter to evaluate against
    * @param oKey    the key for the entry
    * @param oValue  the value for the entry
    *
    * @return true   if the entry passes the filter, false otherwise
    */
    public static <K, V> boolean evaluateEntry(Filter filter, K oKey, V oValue)
        {
        return filter instanceof EntryFilter ?
                ((EntryFilter) filter).evaluateEntry(new SimpleMapEntry<>(oKey, oValue)) :
                filter.evaluate(oValue);
        }

    /**
    * Check if the entry, in its "original" form, passes the filter evaluation.
    *
    * @param filter  the filter to evaluate against
    * @param entry   the entry whose "original" value to evaluate
    *
    * @return true iff the entry has an original value and passes the filter
    */
    public static boolean evaluateOriginalEntry(Filter filter, MapTrigger.Entry entry)
        {
        if (entry.isOriginalPresent())
            {
            if (filter instanceof EntryFilter)
                {
                EntryFilter filterEntry = (EntryFilter) filter;
                return entry instanceof BinaryEntry
                    ? filterEntry.evaluateEntry(new RoutingBinaryEntry((BinaryEntry) entry))
                    : filterEntry.evaluateEntry(new RoutingMapTriggerEntry(entry));
                }
            else
                {
                return filter.evaluate(entry.getOriginalValue());
                }
            }

        return false;
        }

    /**
    * Extract a value from the specified entry using the specified extractor.
    *
    * @param extractor  the extractor to use
    * @param entry      the entry to extract from
    *
    * @param <T>  the type of the value to extract from
    * @param <E>  the type of value that will be extracted
    * @param <K>  the entry key type
    * @param <V>  the entry value type
    *
    * @return the extracted value
    */
    public static <T, E, K, V> E extractFromEntry(ValueExtractor<? super T, ? extends E> extractor, Map.Entry<? extends K, ? extends V> entry)
        {
        extractor = Lambdas.ensureRemotable(extractor);

        return extractor instanceof AbstractExtractor
                ? ((AbstractExtractor<T, E>) extractor).extractFromEntry(entry)
                : ((ValueExtractor<V, E>) extractor).extract(entry.getValue());
        }

    /**
    * Extract a value from the "original value" of the specified entry using
    * the specified extractor.
    *
    * @param extractor  the extractor to use
    * @param entry      the entry to extract from
    *
    * @return the extracted original value
    */
    public static Object extractOriginalFromEntry(ValueExtractor extractor, MapTrigger.Entry entry)
        {
        return extractor instanceof AbstractExtractor
                ? ((AbstractExtractor) extractor).extractOriginalFromEntry(entry)
                : entry.isOriginalPresent()
                    ? extractor.extract(entry.getOriginalValue()) : null;
        }

    /**
    * Update the specified entry using the specified updater and value.
    *
    * @param updater  the updater to use
    * @param entry    the entry to update
    * @param oValue   the new value
    */
    public static <K, V, U> void updateEntry(ValueUpdater<V, U> updater, Map.Entry<K, V> entry, U oValue)
        {
        if (updater instanceof AbstractUpdater)
            {
            ((AbstractUpdater<K, V, U>) updater).updateEntry(entry, oValue);
            }
        else
            {
            V target = entry.getValue();

            updater.update(target, oValue);

            if (entry instanceof InvocableMap.Entry)
                {
                ((InvocableMap.Entry<K, V>) entry).setValue(target, false);
                }
            else
                {
                entry.setValue(target);
                }
            }
        }


    // ----- QueryMap support ----------------------------------------------

    /**
    * Generic implementation of the {@link com.tangosol.util.QueryMap} API.
    *
    * @param map         the underlying Map
    * @param filter      the Filter
    * @param fEntries    if true, return an entry-set; otherwise a key-set
    * @param fSort       if true, sort the entry-set before returning
    * @param comparator  the Comparator to use for sorting (optional)
    *
    * @return the query result set
    */
    public static Set query(Map map, Filter filter, boolean fEntries,
                            boolean fSort, Comparator comparator)
        {
        return query(map, null, filter, fEntries, fSort, comparator);
        }

    /**
    * Generic implementation of the {@link com.tangosol.util.QueryMap} API.
    *
    * @param map         the underlying Map
    * @param mapIndexes  the map of available {@link MapIndex} objects keyed by
    *                    the related ValueExtractor; read-only
    * @param filter      the Filter
    * @param fEntries    if true, return an entry-set; otherwise a key-set
    * @param fSort       if true, sort the entry-set before returning
    * @param comparator  the Comparator to use for sorting (optional)
    *
    * @return the query result set
    */
    public static Set query(Map map, Map mapIndexes, Filter filter,
                            boolean fEntries, boolean fSort, Comparator comparator)
        {
        Filter filterOrig = filter;
        if (AlwaysFilter.INSTANCE.equals(filter))
            {
            filter = null;
            }

        Object[] aoResult = null; // may contain keys or entries

        if (mapIndexes != null && !mapIndexes.isEmpty())
            {
            // apply an index
            if (filter instanceof IndexAwareFilter)
                {
                IndexAwareFilter filterIx = (IndexAwareFilter) filter;
                Set setFilteredKeys = new SubSet(map.keySet());

                try
                    {
                    filter = filterIx.applyIndex(mapIndexes, setFilteredKeys);
                    }
                catch (ConcurrentModificationException e)
                    {
                    // applyIndex failed; try again with a snapshot of the key set
                    setFilteredKeys = new SubSet(
                            new ImmutableArrayList(map.keySet().toArray()));
                    filter = filterIx.applyIndex(mapIndexes, setFilteredKeys);
                    }
                aoResult = setFilteredKeys.toArray();
                }
            }

        if (aoResult == null)
            {
            aoResult = map.keySet().toArray();
            }

        int cResults = 0;
        if (filter == null && !fEntries)
            {
            cResults = aoResult.length;
            }
        else
            {
            // we still have a filter to evaluate or we need an entry set
            for (int i = 0, c = aoResult.length; i < c; i++)
                {
                Object oKey   = aoResult[i];
                Object oValue = map.get(oKey);

                if (oValue != null || map.containsKey(oKey))
                    {
                    Map.Entry entry = new SimpleMapEntry(oKey, oValue);
                    if (filter == null || evaluateEntry(filter, entry))
                        {
                        aoResult[cResults++] = fEntries ? entry : oKey;
                        }
                    }
                }
            }

        LimitFilter filterLimit = filterOrig instanceof LimitFilter ?
                (LimitFilter) filterOrig : null;

        if (filterLimit != null || (fEntries && fSort))
            {
            if (cResults < aoResult.length)
                {
                Object[] ao = new Object[cResults];
                System.arraycopy(aoResult, 0, ao, 0, cResults);
                aoResult = ao;
                }

            if (fEntries && fSort)
                {
                if (comparator == null)
                    {
                    comparator = SafeComparator.INSTANCE;
                    }

                Arrays.sort(aoResult, new EntryComparator(comparator));
                }

            if (filterLimit != null)
                {
                // if the original filter is a LimitFilter then we can only
                // return a page at a time
                filterLimit.setComparator(null);

                aoResult = filterLimit.extractPage(aoResult);
                cResults = aoResult.length;
                filterLimit.setComparator(comparator); // for debug output only
                }
            }

        return new ImmutableArrayList(aoResult, 0, cResults).getSet();
        }

    /**
    * Add an index to the given map of indexes, keyed by the given extractor.
    * Also add the index as a listener to the given ObservableMap.
    *
    * @param extractor   the ValueExtractor object that is used to extract an
    *                    indexable property value from a resource map entry
    * @param fOrdered    true if the contents of the indexed information
    *                    should be ordered; false otherwise
    * @param comparator  the Comparator object which imposes an ordering
    *                    on entries in the indexed map or <tt>null</tt>
    *                    if the entries' values natural ordering should be
    *                    used
    * @param map         the resource map that the newly created MapIndex
    *                    will use for initialization and listen to for changes
    * @param mapIndex    the map of indexes that the newly created MapIndex
    *                    will be added to
    */
    public static void addIndex(ValueExtractor extractor, boolean fOrdered,
            Comparator comparator, ObservableMap map, Map mapIndex)
        {
        extractor = Lambdas.ensureRemotable(extractor);

        synchronized (mapIndex)
            {
            MapIndex index = (MapIndex) mapIndex.get(extractor);

            if (index == null)
                {
                for (int cAttempts = 4; true;)
                    {
                    if (extractor instanceof IndexAwareExtractor)
                        {
                        index = ((IndexAwareExtractor) extractor).
                                createIndex(fOrdered, comparator, mapIndex, null);
                        if (index == null)
                            {
                            return;
                            }
                        }
                    else
                        {
                        index = new SimpleMapIndex(extractor, fOrdered, comparator,
                                /*BackingMapContext*/ null);
                        mapIndex.put(extractor, index);
                        }

                    MapListener listener = ensureListener(index);
                    map.addMapListener(listener, null, false);

                    try
                        {
                        // build the index
                        for (Iterator iter = map.entrySet().iterator(); iter.hasNext();)
                            {
                            Map.Entry entry = (Map.Entry) iter.next();
                            index.insert(entry);
                            }
                        break;
                        }
                    catch (ConcurrentModificationException cme)
                        {
                        map.removeMapListener(listener);
                        if (--cAttempts == 0)
                            {
                            removeIndex(extractor, map, mapIndex);
                            trace("Exception occurred during index rebuild: " +
                                    getStackTrace(cme));
                            throw cme;
                            }
                        }
                    }
                }
            else if (!(fOrdered == index.isOrdered() &&
                    Base.equals(comparator, index.getComparator())))
                {
                throw new IllegalArgumentException("Index for " + extractor +
                        " already exists;" +
                        " remove the index and add it with the new settings");
                }
            }
        }

    /**
    * Remove the index keyed by the given extractor from the given map of
    * indexes.  Also, remove the index as a listener from the given
    * ObservableMap.
    *
    * @param extractor  the ValueExtractor object that is used to extract an
    *                   indexable Object from a value stored in the Map.
    * @param map        the resource map to remove the index for
    * @param mapIndex   the map of indexes to remove the MapIndex from
    */
    public static void removeIndex(ValueExtractor extractor, ObservableMap map,
                                   Map mapIndex)
        {
        extractor = Lambdas.ensureRemotable(extractor);

        MapIndex index = extractor instanceof IndexAwareExtractor ?
            ((IndexAwareExtractor) extractor).destroyIndex(mapIndex) :
            (MapIndex) mapIndex.remove(extractor);

        if (index != null)
            {
            map.removeMapListener(ensureListener(index));
            }
        }


    // ----- helpers -------------------------------------------------------

    /**
    * Ensure a MapListener for the given index.  The listener will route the
    * map events into the corresponding MapIndex calls.
    *
    * @param index  the index
    *
    * @return a listener for given index
    */
    protected static MapListener ensureListener(MapIndex index)
        {
        return index instanceof MapListenerSupport.SynchronousListener ?
                (MapListener) index : new IndexAdapter(index);
        }


    // ----- constants -----------------------------------------------------

    /**
    * Trivial Entry-to-Key converter.
    */
    public static final Converter ENTRY_TO_KEY_CONVERTER =
            o -> ((Map.Entry) o).getKey();

    /**
    * Trivial Entry-to-Value converter.
    */
    public static final Converter ENTRY_TO_VALUE_CONVERTER =
            o -> ((Map.Entry) o).getValue();


    // ----- inner classes --------------------------------------------------

    /**
    * Simple implementation of the InvocableMap.Entry interface.
    * This assumes that the underlying Map content does not change while a
    * reference to the SimpleEntry is alive and may cache the entry's value to
    * avoid an extra map lookup.
    */
    public static class SimpleEntry<K, V>
            extends SimpleMapEntry<K, V>
        {
        /**
        * Construct a SimpleEntry for a given map and a key.
        *
        * @param map       the parent Map for this entry
        * @param oKey      the entry's key
        * @param fReadOnly if true, the entry will be marked as
        *                   read-only, preventing the <tt>setValue()</tt> and
        *                   <tt>remove()</tt> methods from modifying the
        *                   underlying map's content
        */
        public SimpleEntry(Map<K, V> map, K oKey, boolean fReadOnly)
            {
            this(map, oKey, (V) UNKNOWN, fReadOnly);
            }

        /**
        * Construct a SimpleEntry for a given key and value.
        *
        * @param map       the parent Map for this entry
        * @param oKey        the entry's key
        * @param oValue      the entry's value
        * @param fReadOnly   if true, the entry will be marked as
        *                   read-only, preventing the <tt>setValue()</tt> and
        *                   <tt>remove()</tt> methods from modifying the
        *                   underlying map's content
        */
        public SimpleEntry(Map<K, V> map, K oKey, V oValue, boolean fReadOnly)
            {
            super(oKey, oValue);

            azzert(map != null);

            m_map       = map;
            m_fReadOnly = fReadOnly;
            }

        /**
        * Construct a SimpleEntry for a given key and value. The entry will
        * be marked as read-only.
        *
        * @param oKey    the entry's key
        * @param oValue  the entry's value
        */
        public SimpleEntry(K oKey, V oValue)
            {
            super(oKey, oValue);

            m_fReadOnly = true;
            }

        // ----- InvocableMap.Entry interface methods ---------------------

        /**
        * {@inheritDoc}
        */
        public V getValue()
            {
            V oValue = super.getValue();
            if (oValue == UNKNOWN)
                {
                oValue = m_oValue = m_map.get(m_oKey);
                }
            return oValue;
            }

        /**
        * {@inheritDoc}
        */
        public V setValue(V oValue)
            {
            checkMutable();

            super.setValue(oValue);
            return m_map.put(m_oKey, oValue);
            }

        /**
        * {@inheritDoc}
        */
        public void setValue(V oValue, boolean fSynthetic)
            {
            checkMutable();

            super.setValue(oValue);
            m_map.putAll(Collections.singletonMap(m_oKey, oValue));
            }

        /**
        * {@inheritDoc}
        */
        public boolean isPresent()
            {
            // m_value could be null in two cases:
            // (a) the actual value is null;
            // (b) the value has been removed
            // If the map is not specified, an entry is assumed to exist
            Object oValue = m_oValue;
            return (oValue != UNKNOWN && oValue != null)
                 || m_map == null || m_map.containsKey(m_oKey);
            }

        /**
        * {@inheritDoc}
        */
        public void remove(boolean fSynthetic)
            {
            checkMutable();

            Map    map  = m_map;
            Object oKey = m_oKey;

            if (fSynthetic && map instanceof LocalCache)
                {
                ((LocalCache) map).evict(oKey);
                }
            else
                {
                map.keySet().remove(oKey);
                }
            m_oValue = null;
            }

        // ----- Object methods -------------------------------------------

        /**
        * Compare this SimpleEntry with another object for equality.
        *
        * @param  o  an object reference or null
        *
        * @return  true iff the passed object reference is a SimpleEntry object
        *          with the same key
        */
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o instanceof SimpleEntry)
                {
                SimpleEntry that = (SimpleEntry) o;
                return equals(this.m_oKey, that.m_oKey);
                }
            return false;
            }

        /**
        * Return a hash code value for the SimpleEntry object.
        */
        public int hashCode()
            {
            Object oKey = m_oKey;
            return oKey == null ? 0 : oKey.hashCode();
            }

        /**
        * Provide a human-readable representation of the SimpleEntry object.
        *
        * @return a String representation of this SimpleEntry object
        */
        public String toString()
            {
            return "SimpleEntry(key=" + m_oKey + ')';
            }

        // ----- helpers --------------------------------------------------

        /**
        * Verify that this SimpleEntry is mutable.
        */
        protected void checkMutable()
            {
            if (m_fReadOnly)
                {
                throw new UnsupportedOperationException(
                    "Read-only entry does not allow Map modification");
                }
            }

        // ----- constants and data fields --------------------------------

        /**
        * An "unknown value" tag.
        */
        private final static Object UNKNOWN = new Object();

        /**
        * The map.
        */
        protected Map<K, V> m_map;

        /**
        * The read-only flag.
        */
        private boolean m_fReadOnly;
        }


    /**
     * MapTrigger.Entry wrapper that routes the getValue() call onto
     * getOriginalValue().
     */
    protected static class RoutingMapTriggerEntry
            implements MapTrigger.Entry
        {
        /**
         * Construct a routing entry.
         *
         * @param entry  the underlying MapTrigger.Entry
         */
        protected RoutingMapTriggerEntry(MapTrigger.Entry entry)
            {
            m_entry = entry;
            }

        /**
         * Construct a routing entry.
         *
         * @param entry  the underlying BinaryEntry
         */
        protected RoutingMapTriggerEntry(BinaryEntry entry)
            {
            m_entry = entry;
            }

        // ----- MapTrigger.Entry implementation ----------------------------

        /**
         * {@inheritDoc}
         */
        public Object getKey()
            {
            return m_entry.getKey();
            }

        /**
         * Return an OriginalValue from the underlying entry.
         */
        public Object getValue()
            {
            return ((MapTrigger.Entry) m_entry).getOriginalValue();
            }

        /**
         * {@inheritDoc}
         */
        public Object extract(ValueExtractor extractor)
            {
            return extractFromEntry(extractor, this);
            }

        // ----- not supported ----------------------------------------------

        /**
         * @throws UnsupportedOperationException
         */
        public Object getOriginalValue()
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public Object setValue(Object oValue)
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public void setValue(Object oValue, boolean fSynthetic)
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public void update(ValueUpdater updater, Object oValue)
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public void remove(boolean fSynthetic)
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public boolean isPresent()
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public boolean isSynthetic()
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public boolean isOriginalPresent()
            {
            throw new UnsupportedOperationException();
            }

        // ----- data fields ------------------------------------------------

        /**
         * The underlying entry. We artificially widen the type to be able to
         * extend this class.
         */
        protected InvocableMap.Entry m_entry;
        }


    /**
     * BinaryEntry wrapper that routes the getValue()/getBinaryValue()
     * calls onto getOriginalValue()/getOriginalBinaryValue().
     */
    protected static class RoutingBinaryEntry
            extends    RoutingMapTriggerEntry
            implements BinaryEntry
        {
        /**
         * Construct a routing entry.
         *
         * @param entry  the underlying BinaryEntry
         */
        protected RoutingBinaryEntry(BinaryEntry entry)
            {
            super(entry);
            }

        // ----- BinaryEntry implementation ---------------------------------

        /**
         * {@inheritDoc}
         */
        public Binary getBinaryKey()
            {
            return ((BinaryEntry) m_entry).getBinaryKey();
            }

        /**
         * Return an OriginalBinaryValue from the underlying entry.
         */
        public Binary getBinaryValue()
            {
            return ((BinaryEntry) m_entry).getOriginalBinaryValue();
            }

        /**
         * {@inheritDoc}
         */
        public Serializer getSerializer()
            {
            return ((BinaryEntry) m_entry).getSerializer();
            }

        /**
         * {@inheritDoc}
         */
        public boolean isReadOnly()
            {
            return true;
            }

        // ----- not supported ----------------------------------------------

        /**
         * @throws UnsupportedOperationException
         */
        public BackingMapManagerContext getContext()
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public void updateBinaryValue(Binary binValue)
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public void updateBinaryValue(Binary binValue, boolean fSynthetic)
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public Binary getOriginalBinaryValue()
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public ObservableMap getBackingMap()
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public BackingMapContext getBackingMapContext()
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public void expire(long cMillis)
            {
            throw new UnsupportedOperationException();
            }

        /**
         * @throws UnsupportedOperationException
         */
        public long getExpiry()
            {
            throw new UnsupportedOperationException();
            }
        }


    /**
    * MapListener implementation that routes the map events into the
    * corresponding MapIndex calls.
    */
    protected static class IndexAdapter
            implements MapListenerSupport.SynchronousListener
        {
        /**
        * Construct an IndexAdapter.
        *
        * @param index  the MapIndex being wrapped
        */
        protected IndexAdapter(MapIndex index)
            {
            m_index = index;
            }

        // ----- MapListener interface ----------------------------------

        /**
        * {@inheritDoc}
        */
        public void entryInserted(MapEvent evt)
            {
            m_index.insert(
                new SimpleMapEntry(evt.getKey(), evt.getNewValue()));
            }

        /**
        * {@inheritDoc}
        */
        public void entryUpdated(MapEvent evt)
            {
            m_index.update(
                new SimpleMapEntry(evt.getKey(), evt.getNewValue(), evt.getOldValue()));
            }

        /**
        * {@inheritDoc}
        */
        public void entryDeleted(MapEvent evt)
            {
            m_index.delete(new SimpleMapEntry(evt.getKey(), null, evt.getOldValue()));
            }

        // ----- Object methods -----------------------------------------

        /**
        * Compare this IndexMapListener with another object for equality.
        *
        * @param  o  an object reference or null
        *
        * @return  true iff the passed object reference is a IndexMapListener
        *          object with the same index
        */
        public boolean equals(Object o)
            {
            return this == o || o instanceof IndexAdapter &&
                    Base.equals(this.m_index, ((IndexAdapter) o).m_index);
            }

        /**
        * Return a hash code value for the IndexMapListener object.
        */
        public int hashCode()
            {
            return m_index.hashCode();
            }

        // ----- data fields --------------------------------------------
        /**
        * The wrapped index.
        */
        private MapIndex m_index;
        }
    }
