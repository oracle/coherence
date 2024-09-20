/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.common.base.Holder;
import com.oracle.coherence.common.base.NaturalHasher;

import com.tangosol.internal.net.NamedCacheDeactivationListener;
import com.tangosol.internal.util.processor.CacheProcessors;

import com.tangosol.io.Serializer;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.AsyncNamedMap;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;

import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.CacheMap;
import com.tangosol.net.cache.ConfigurableCacheMap;

import com.tangosol.util.InvocableMap.EntryAggregator;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.MapListenerSupport.WrapperListener;

import com.tangosol.util.function.Remote;

import java.io.Serializable;

import java.lang.reflect.Array;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import java.util.concurrent.CompletableFuture;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
* A collection of Collection implementation classes that use the Converter
* interface to convert the items stored in underlying collection objects.
*
* @author cp  2002.02.08
* @author jh  2007.09.28
*/
public abstract class ConverterCollections
    {
    // ----- factory methods ------------------------------------------------

    /**
     * Returns an instance of Iterator that uses a Converter to view an
     * underlying Iterator.
     *
     * @param iter  the underlying Iterator
     * @param conv  the Converter to view the underlying Iterator through
     *
     * @param <F> the type of elements in the underlying Iterator
     * @param <T> the type that the elements should be converted to
     *
     * @return an Iterator that views the passed Iterator through the
     *         specified Converter
     */
    public static <F, T> Iterator<T> getIterator(Iterator<F> iter, Converter<F, T> conv)
        {
        return new ConverterEnumerator<>(iter, conv);
        }

    /**
     * Returns an instance of Collection that uses a Converter to view an
     * underlying Collection.
     *
     * @param col       the underlying Collection
     * @param convUp    the Converter to view the underlying Collection
     *                  through
     * @param convDown  the Converter to pass items down to the underlying
     *                  Collection through
     *
     * @param <F> the type of elements in the underlying Collection
     * @param <T> the type that the elements should be converted to
     *
     * @return a Collection that views the passed Collection through the
     *         specified Converter
     */
    public static <F, T> ConverterCollection<F, T> getCollection(Collection<F> col, Converter<F, T> convUp,
            Converter<T, F> convDown)
        {
        return new ConverterCollection<>(col, convUp, convDown);
        }

    /**
    * Returns an instance of Set that uses a Converter to view an
    * underlying Set.
    *
    * @param set       the underlying Set
    * @param convUp    the Converter to view the underlying Set
    *                  through
    * @param convDown  the Converter to pass items down to the underlying
    *                  Set through
    *
    * @param <F> the type of elements in the underlying Set
    * @param <T> the type that the elements should be converted to
    *
    * @return a Set that views the passed Set through the specified
    *         Converter
    */
    public static <F, T> ConverterSet<F, T> getSet(Set<F> set, Converter<F, T> convUp, Converter<T, F> convDown)
        {
        return new ConverterSet<>(set, convUp, convDown);
        }

    /**
     * Returns an instance of SortedSet that uses a Converter to view an
     * underlying SortedSet.
     *
     * @param set       the underlying SortedSet
     * @param convUp    the Converter to view the underlying SortedSet
     *                  through
     * @param convDown  the Converter to pass items down to the underlying
     *                  SortedSet through
     *
     * @param <F> the type of elements in the underlying SortedSet
     * @param <T> the type that the elements should be converted to
     *
     * @return a SortedSet that views the passed SortedSet through the
     *         specified Converter
     */
    public static <F, T> ConverterSortedSet<F, T> getSortedSet(SortedSet<F> set, Converter<F, T> convUp,
            Converter<T, F> convDown)
        {
        return new ConverterSortedSet<>(set, convUp, convDown);
        }

    /**
     * Returns a Converter instance of List.
     *
     * @param list      the underlying List
     * @param convUp    the Converter to view the underlying List
     *                  through
     * @param convDown  the Converter to pass items down to the underlying
     *                  List through
     *
     * @param <F> the type of elements in the underlying list
     * @param <T> the type that the elements should be converted to
     *
     * @return a List that views the passed List through the specified
     *         Converter
     */
    public static <F, T> ConverterList<F, T> getList(List<F> list, Converter<F, T> convUp, Converter<T, F> convDown)
        {
        return new ConverterList<>(list, convUp, convDown);
        }

    /**
     * Returns a Converter instance of ListIterator.
     *
     * @param <F> the type of elements in the underlying ListIterator
     * @param <T> the type that the elements should be converted to
     *
     * @param iter      the underlying ListIterator
     * @param convUp    the Converter to view the underlying list
     *                  through
     * @param convDown  the Converter to pass items down to the underlying
     *                  ListIterator through
     *
     * @return a ListIterator that views the passed ListIterator through the
     *         specified Converter
     */
    public static <F, T> ConverterListIterator<F, T> getListIterator(ListIterator<F> iter, Converter<F, T> convUp,
            Converter<T, F> convDown)
        {
        return new ConverterListIterator<>(iter, convUp, convDown);
        }

    /**
     * Returns a Converter instance of Map.
     *
     * @param map          the underlying Map
     * @param convKeyUp    the Converter to view the underlying Map's keys
     *                     through
     * @param convKeyDown  the Converter to use to pass keys down to the
     *                     underlying Map
     * @param convValUp    the Converter to view the underlying Map's values
     *                     through
     * @param convValDown  the Converter to use to pass values down to the
     *                     underlying Map
     *
     * @param <FK> the type of the keys in the underlying Map
     * @param <TK> the type that the keys should be converted to
     * @param <FV> the type of the values in the underlying Map
     * @param <TV> the type that the values should be converted to
     *
     * @return a Map that views the keys and values of the passed Map through
     *         the specified Converters
     */
    public static <FK, TK, FV, TV> ConverterMap<FK, TK, FV, TV> getMap(Map<FK, FV> map, Converter<FK, TK> convKeyUp,
            Converter<TK, FK> convKeyDown, Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
        {
        return new ConverterMap<>(map, convKeyUp, convKeyDown, convValUp, convValDown);
        }

    /**
     * Returns a Converter instance of SortedMap.
     *
     * @param map          the underlying SortedMap
     * @param convKeyUp    the Converter to view the underlying SortedMap's
     *                     keys through
     * @param convKeyDown  the Converter to use to pass keys down to the
     *                     underlying SortedMap
     * @param convValUp    the Converter to view the underlying SortedMap's
     *                     values through
     * @param convValDown  the Converter to use to pass values down to the
     *                     underlying SortedMap
     *
     * @param <FK> the type of the keys in the underlying SortedMap
     * @param <TK> the type that the keys should be converted to
     * @param <FV> the type of the values in the underlying SortedMap
     * @param <TV> the type that the values should be converted to
     *
     * @return a SortedMap that views the keys and values of the passed
     *         SortedMap through the specified Converters
     */
    public static <FK, TK, FV, TV> ConverterSortedMap<FK, TK, FV, TV> getSortedMap(SortedMap<FK, FV> map,
            Converter<FK, TK> convKeyUp, Converter<TK, FK> convKeyDown, Converter<FV, TV> convValUp,
            Converter<TV, FV> convValDown)
        {
        return new ConverterSortedMap<>(map, convKeyUp, convKeyDown, convValUp, convValDown);
        }


    /**
     * Returns a Converter instance of a Set that holds Entry objects for a
     * ConverterMap.
     *
     * @param set          the underlying Entry Set (or Collection of Map
     *                     Entry objects)
     * @param convKeyUp    the Converter to view the underlying Entry Set's
     *                     keys through
     * @param convKeyDown  the Converter to use to pass keys down to the
     *                     underlying Entry Set
     * @param convValUp    the Converter to view the underlying Entry Set's
     *                     values through
     * @param convValDown  the Converter to use to pass values down to the
     *                     underlying Entry Set
     *
     * @param <FK> the type of the keys in the underlying EntrySet
     * @param <TK> the type that the keys should be converted to
     * @param <FV> the type of the values in the underlying EntrySet
     * @param <TV> the type that the values should be converted to
     *
     * @return a Converter Set that views the keys and values of the
     *         underlying Set's Map.Entry objects through the specified key
     *         and value Converters
     */
    public static <FK, TK, FV, TV> ConverterEntrySet<FK, TK, FV, TV> getEntrySet(Collection<Entry<FK, FV>> set,
            Converter<FK, TK> convKeyUp, Converter<TK, FK> convKeyDown, Converter<FV, TV> convValUp,
            Converter<TV, FV> convValDown)
        {
        return new ConverterEntrySet<>(set, convKeyUp, convKeyDown, convValUp, convValDown);
        }

    /**
     * Returns an instance of a MapEntry that uses Converters to retrieve
     * the Entry's data.
     *
     * @param entry        the underlying Entry
     * @param convKeyUp    the Converter to view the underlying Entry's key
     * @param convValUp    the Converter to view the underlying Entry's value
     * @param convValDown  the Converter to change the underlying Entry's value
     *
     * @param <FK> the type of the keys in the underlying Entry
     * @param <TK> the type that the keys should be converted to
     * @param <FV> the type of the values in the underlying Entry
     * @param <TV> the type that the values should be converted to
     *
     * @return a ConverterEntry that converts the passed entry data using the
     *         specified Converters
     */
    public static <FK, TK, FV, TV> ConverterEntry<FK, TK, FV, TV> getEntry(Map.Entry<FK, FV> entry,
            Converter<FK, TK> convKeyUp,
            Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
        {
        return new ConverterEntry<>(entry, convKeyUp, convValUp, convValDown);
        }

    /**
     * Returns a Converter instance of Holder..
     *
     * @param value   the underlying value
     * @param convUp  the Converter to view the underlying value
     *
     * @param <F>     the type of the underlying value
     * @param <T>     the type of the converted value
     *
     * @return  a ConverterHolder that converts the passed value using the
     *          specified Converter.
     */
    public static <F ,T> ConverterHolder<F, T> getConverterHolder(F value, Converter<F, T> convUp)
        {
        return new ConverterHolder<>(value, convUp);
        }

    /**
     * Returns a LongArray storing values with type {@code F} and converting
     * to type {@code V} as and when required.
     *
     * @param la        the LongArray with raw types
     * @param convUp    a Converter to convert to the desired type
     * @param convDown  a Converter to convert to the raw type
     *
     * @param <F>       the raw type
     * @param <T>       the desired type
     *
     * @return a LongArray storing values in a raw type and converting to the
     *         desired type
     */
    public static <F, T> LongArray<T> getLongArray(LongArray<F> la, Converter<F, T> convUp, Converter<T, F> convDown)
        {
        return new ConverterLongArray<>(la, convUp, convDown);
        }
    /**
    * Returns a Converter instance of ConcurrentMap.
    *
    * @param map          the underlying ConcurrentMap
    * @param convKeyUp    the Converter to view the underlying
    *                     ConcurrentMap's keys through
    * @param convKeyDown  the Converter to use to pass keys down to the
    *                     underlying ConcurrentMap
    * @param convValUp    the Converter to view the underlying
    *                     ConcurrentMap's values through
    * @param convValDown  the Converter to use to pass values down to the
    *                     underlying ConcurrentMap
    *
    * @return a ConcurrentMap that views the keys and values of the passed
    *         ConcurrentMap through the specified Converters
    */
    public static <FK, TK, FV, TV> ConcurrentMap<TK, TV> getConcurrentMap(
            ConcurrentMap<FK, FV> map, Converter<FK, TK> convKeyUp, Converter<TK, FK> convKeyDown,
            Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
        {
        return new ConverterConcurrentMap<>(map, convKeyUp, convKeyDown, convValUp, convValDown);
        }

    /**
    * Returns a Converter instance of InvocableMap.
    *
    * @param map          the underlying InvocableMap
    * @param convKeyUp    the Converter to view the underlying
    *                     InvocableMap's keys through
    * @param convKeyDown  the Converter to use to pass keys down to the
    *                     underlying InvocableMap
    * @param convValUp    the Converter to view the underlying
    *                     InvocableMap's values through
    * @param convValDown  the Converter to use to pass values down to the
    *                     underlying InvocableMap
    *
    * @return an InvocableMap that views the keys and values of the passed
    *         InvocableMap through the specified Converters
    */
    public static <FK, TK, FV, TV> InvocableMap<TK, TV> getInvocableMap(
            InvocableMap<FK, FV> map, Converter<FK, TK> convKeyUp, Converter<TK, FK> convKeyDown,
            Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
        {
        return new ConverterInvocableMap<>(map, convKeyUp, convKeyDown, convValUp, convValDown);
        }

    /**
    * Returns a Converter instance of ObservableMap.
    *
    * @param map          the underlying ObservableMap
    * @param convKeyUp    the Converter to view the underlying
    *                     ObservableMap's keys through
    * @param convKeyDown  the Converter to use to pass keys down to the
    *                     underlying ObservableMap
    * @param convValUp    the Converter to view the underlying
    *                     ObservableMap's values through
    * @param convValDown  the Converter to use to pass values down to the
    *                     underlying ObservableMap
    *
    * @return an ObservableMap that views the keys and values of the passed
    *         ObservableMap through the specified Converters
    */
    public static <FK, TK, FV, TV> ObservableMap<TK, TV> getObservableMap(ObservableMap<FK, FV> map,
                Converter<FK, TK> convKeyUp, Converter<TK, FK> convKeyDown,
                Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
        {
        return new ConverterObservableMap<>(map, convKeyUp, convKeyDown, convValUp, convValDown);
        }

    /**
    * Returns a Converter instance of QueryMap.
    *
    * @param map          the underlying QueryMap
    * @param convKeyUp    the Converter to view the underlying QueryMap's
    *                     keys through
    * @param convKeyDown  the Converter to use to pass keys down to the
    *                     underlying QueryMap
    * @param convValUp    the Converter to view the underlying QueryMap's
    *                     values through
    * @param convValDown  the Converter to use to pass values down to the
    *                     underlying QueryMap
    *
    * @return a QueryMap that views the keys and values of the passed
    *         QueryMap through the specified Converters
    */
    public static <FK, TK, FV, TV> QueryMap<TK, TV> getQueryMap(QueryMap<FK, FV> map,
            Converter<FK, TK> convKeyUp, Converter<TK, FK> convKeyDown,
            Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
        {
        return new ConverterQueryMap<>(map, convKeyUp, convKeyDown, convValUp, convValDown);
        }

    /**
    * Returns a Converter instance of CacheMap.
    *
    * @param map          the underlying CacheMap
    * @param convKeyUp    the Converter to view the underlying CacheMap's
    *                     keys through
    * @param convKeyDown  the Converter to use to pass keys down to the
    *                     underlying CacheMap
    * @param convValUp    the Converter to view the underlying CacheMap's
    *                     values through
    * @param convValDown  the Converter to use to pass values down to the
    *                     underlying CacheMap
    *
    * @return a CacheMap that views the keys and values of the passed
    *         CacheMap through the specified Converters
    */
    public static <FK, TK, FV, TV> CacheMap<TK, TV> getCacheMap(CacheMap<FK, FV> map,
                Converter<FK, TK> convKeyUp, Converter<TK, FK> convKeyDown,
                Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
        {
        return new ConverterCacheMap<>(map, convKeyUp, convKeyDown, convValUp, convValDown);
        }

    /**
    * Returns a Converter instance of NamedCache that converts between the raw/from
    * types to the desired/to types.
    * <p>
    * <b>There is a strong disclaimer in the use of this implementation:</b>
    * <p>
    * This conversion is entirely performed locally and therefore when using
    * methods such as {@link NamedCache#invoke(Object, EntryProcessor)
    * invoke}, or {@link NamedCache#aggregate(EntryAggregator) aggregate}, or
    * {@link NamedCache#entrySet(Filter) entrySet(Filter)}, the
    * provided agent ({@link EntryProcessor EntryProcessor}, or {@link EntryAggregator
    * EntryAggregator}, or {@link Filter}) do not go through the provided converters.
    * Hence the given agent(s) must operate against the raw types.
    * <p>
    * Streams are not supported.
    *
    * @param cache        the underlying NamedCache
    * @param convKeyUp    the Converter to view the underlying NamedCache's
    *                     keys through
    * @param convKeyDown  the Converter to use to pass keys down to the
    *                     underlying NamedCache
    * @param convValUp    the Converter to view the underlying NamedCache's
    *                     values through
    * @param convValDown  the Converter to use to pass values down to the
    *                     underlying NamedCache
    *
    * @return a NamedCache that views the keys and values of the passed
    *         NamedCache through the specified Converters
    */
    public static <FK, FV, TK, TV> NamedCache<TK, TV> getNamedCache(NamedCache<FK, FV> cache,
                                                                     Converter<FK, TK> convKeyUp,
                                                                     Converter<TK, FK> convKeyDown,
                                                                     Converter<FV, TV> convValUp,
                                                                     Converter<TV, FV> convValDown)
        {
        return new ConverterNamedCache<>(cache, convKeyUp, convKeyDown, convValUp, convValDown);
        }


    /**
    * Returns a Converter instance of an {@link AsyncNamedCache} that converts between
     * the raw/from types to the desired/to types.
    * <p>
    * <b>There is a strong disclaimer in the use of this implementation:</b>
    * <p>
    * This conversion is entirely performed locally and therefore when using
    * methods such as {@link AsyncNamedCache#invoke(Object, EntryProcessor)
    * invoke}, or {@link AsyncNamedCache#aggregate(EntryAggregator) aggregate}, the
    * provided agent ({@link EntryProcessor EntryProcessor}, or {@link EntryAggregator
    * EntryAggregator}, or {@link Filter}) do not go through the provided converters.
    * Hence, the given agent(s) must operate against the raw types.
    *
    * @param cache        the underlying {@link AsyncNamedCache}
    * @param convKeyUp    the Converter to view the underlying NamedCache's
    *                     keys through
    * @param convKeyDown  the Converter to use to pass keys down to the
    *                     underlying NamedCache
    * @param convValUp    the Converter to view the underlying NamedCache's
    *                     values through
    * @param convValDown  the Converter to use to pass values down to the
    *                     underlying NamedCache
    *
    * @return an {@link AsyncNamedCache} that views the keys and values of the passed
    *         {@link AsyncNamedCache} through the specified Converters
    */
    public static <FK, FV, TK, TV> AsyncNamedCache<TK, TV> getAsyncNamedCache(AsyncNamedCache<FK, FV> cache,
                                                                              Converter<FK, TK> convKeyUp,
                                                                              Converter<TK, FK> convKeyDown,
                                                                              Converter<FV, TV> convValUp,
                                                                              Converter<TV, FV> convValDown)
        {
        return new ConverterAsyncNamedCache<>(cache, convKeyUp, convKeyDown, convValUp, convValDown);
        }

    /**
    * Returns an instance of a MapEvent that uses Converters to retrieve
    * the event's data.
    *
    * @param map      the new event's source
    * @param event    the underlying MapEvent
    * @param convKey  the Converter to view the underlying MapEvent's key
    * @param convVal  the Converter to view the underlying MapEvent's values
    *
    * @return a MapEvent that converts the passed event data using the
    *         specified Converter
    */
    public static MapEvent getMapEvent(ObservableMap map, MapEvent event, Converter convKey, Converter convVal)
        {
        return new ConverterMapEvent(map, event, convKey, convVal);
        }

    /**
    * Returns an instance of a MapEvent that uses Converters to retrieve the
    * event's data, and additionally provides access to the
    * BackingMapManagerContext.
    *
    * @param map      the new event's source
    * @param event    the underlying MapEvent
    * @param convKey  the Converter to view the underlying MapEvent's key
    * @param convVal  the Converter to view the underlying MapEvent's values
    * @param context  the BackingMapManagerContext used to deserialize the
    *                 underlying values
    *
    * @return a MapEvent that converts the passed event data using the
    *         specified Converters
    */
    public static MapEvent getMapEvent(ObservableMap map, MapEvent event,
            Converter convKey, Converter convVal, BackingMapManagerContext context)
        {
        return new ConverterMapEvent(map, event, convKey, convVal, context);
        }

    /**
    * Returns a converter listener for the specified listener and Converters.
    *
    * @param map       the Map that should be the source for converted events
    * @param listener  the underlying MapListener
    * @param convKey   the Converter to view the underlying MapEvent's key
    * @param convVal   the Converter to view the underlying MapEvent's values
    *
    * @return  the converting listener
    */
    public static MapListener getMapListener(ObservableMap map, MapListener listener, Converter convKey, Converter convVal)
        {
        MapListener listenerConv = map instanceof NamedCache ?
            new ConverterCacheListener((NamedCache) map, listener, convKey, convVal) :
            new ConverterMapListener(map, listener, convKey, convVal);

        return listener instanceof MapListenerSupport.SynchronousListener ?
            new MapListenerSupport.WrapperSynchronousListener(listenerConv) :
            listenerConv;
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Convert the contents of the passed array. The conversion is done "in
    * place" in the passed array.
    * <p>
    * This helper method is intended to support the functionality of
    * Collection.toArray.
    *
    * @param ao    an array of Objects to convert
    * @param conv  the Converter to use to convert the objects
    *
    * @return the passed array
    */
    public static Object[] convertArray(Object[] ao, Converter conv)
        {
        for (int i = 0, c = ao.length; i < c; ++i)
            {
            ao[i] = conv.convert(ao[i]);
            }
        return ao;
        }

    /**
    * Convert the contents of the passed source array into an array with the
    * component type of the passed destination array, using the destination
    * array itself if it is large enough, and placing a null in the first
    * unused element of the destination array if it is larger than the
    * source array.
    * <p>
    * This helper method is intended to support the functionality of
    * Collection.toArray.
    *
    * @param aoSrc   an array of Objects to convert
    * @param conv    the Converter to use to convert the objects
    * @param aoDest  the array to use to place the converted objects in if
    *                large enough, otherwise the array from which to obtain
    *                the component type to create a new array that is large
    *                enough
    *
    * @return an array whose component type is the same as the passed
    *         destination array and whose contents are the converted objects
    */
    public static Object[] convertArray(Object[] aoSrc, Converter conv, Object[] aoDest)
        {
        int cSrc  = aoSrc.length;
        int cDest = aoDest.length;
        if (cSrc > cDest)
            {
            cDest  = cSrc;
            aoDest = (Object[]) Array.newInstance(aoDest.getClass().getComponentType(), cDest);
            }

        if (cDest > cSrc)
            {
            aoDest[cSrc] = null;
            }

        for (int i = 0; i < cSrc; ++i)
            {
            aoDest[i] = conv.convert(aoSrc[i]);
            }

        return aoDest;
        }

    /**
    * Create a new converter by combining two existing converters.
    *
    * @param converter1 the first converter
    * @param converter2 the second converter
    *
    * @return a combining converter that applies the converters sequentially
    *
    * @param <F> the "from" type for the first converter
    * @param <I> the "to" type for the first converter and the "from" type for the second
    * @param <T> the "to" type for the second converter
    */
    public static <F, I, T> Converter<F, T> combine(Converter<F,I> converter1, Converter<I, T> converter2)
        {
        return (from) -> converter2.convert(converter1.convert(from));
        }



    // ----- inner class: ConverterEnumerator -------------------------------

    /**
     * Provide an implementation of an enumerator which converts each of the
     * items which it enumerates.
     *
     * @param <F> the type of elements in the underlying Enumeration
     * @param <T> the type that the elements should be converted to
     *
     */
    public static class ConverterEnumerator<F, T>
            implements Enumeration<T>, Iterator<T>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct the Converter enumerator based on an Enumeration.
         *
         * @param enmr  java.util.Enumeration of objects to convert
         * @param conv  a Converter
         */
        public ConverterEnumerator(final Enumeration<F> enmr, Converter<F, T> conv)
            {
            this(new Iterator<F>()
                {
                public boolean hasNext()
                    {
                    return enmr.hasMoreElements();
                    }

                public F next()
                    {
                    return enmr.nextElement();
                    }

                public void remove()
                    {
                    throw new UnsupportedOperationException();
                    }
                }, conv);
            }

        /**
         * Construct the Converter enumerator based on an Iterator.
         *
         * @param iter  java.util.Iterator of objects to convert
         * @param conv  a Converter
         */
        public ConverterEnumerator(Iterator<F> iter, Converter<F, T> conv)
            {
            m_iter = iter;
            m_conv = conv;
            }

        /**
         * Construct the Converter enumerator based on an array of objects.
         *
         * @param aoItem  array of objects to enumerate
         * @param conv    a Converter
         */
        public ConverterEnumerator(Object[] aoItem, Converter<F, T> conv)
            {
            this(Arrays.<F> asList((F[]) aoItem).iterator(), conv);
            }


        // ----- Enumeration interface --------------------------------------

        /**
         * Tests if this enumeration contains more elements.
         *
         * @return false if the enumeration has been exhausted
         */
        public boolean hasMoreElements()
            {
            return hasNext();
            }

        /**
         * Get the next element in the enumeration.
         *
         * @return the next element of this enumeration
         */
        public T nextElement()
            {
            return next();
            }


        // ----- Iterator interface -----------------------------------------

        /**
         * Determine if this Iterator contains more elements.
         *
         * @return true if the Iterator contains more elements, false otherwise
         */
        public boolean hasNext()
            {
            return m_iter.hasNext();
            }

        /**
         * Returns the next element of this Iterator.
         *
         * @return the next element in the Iterator
         */
        public T next()
            {
            return m_conv.convert(m_iter.next());
            }

        /**
         * Remove the last-returned element that was returned by the Iterator.
         */
        public void remove()
            {
            m_iter.remove();
            }


        // ----- data members -----------------------------------------------

        /**
         * Iterator of Objects to convert.
         */
        protected Iterator<F> m_iter;

        /**
         * Converter to convert each item.
         */
        protected Converter<F, T> m_conv;
        }

    // ----- inner class: ConverterCollection -------------------------------

    /**
     * A Converter Collection views an underlying Collection through a
     * Converter.
     *
     * @param <F> the type of elements in the underlying Collection
     * @param <T> the type that the elements should be converted to
     *
     */
    public static class ConverterCollection<F, T>
            implements Collection<T>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructor.
         *
         * @param col       the underlying Collection
         * @param convUp    the Converter from the underlying Collection
         * @param convDown  the Converter to the underlying Collection
         */
        public ConverterCollection(Collection<F> col, Converter<F, T> convUp, Converter<T, F> convDown)
            {
            assert(col != null && convUp != null && convDown != null);

            m_col      = col;
            m_convUp   = convUp;
            m_convDown = convDown;
            }


        // ----- Collection interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public int size()
            {
            return getCollection().size();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEmpty()
            {
            return getCollection().isEmpty();
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean contains(Object o)
            {
            Converter<T, F> converterDown = getConverterDown();
            if (converterDown == NullImplementation.NullConverter.INSTANCE)
                {
                for (F value : getCollection())
                    {
                    if (Objects.equals(o, getConverterUp().convert(value)))
                        {
                        return true;
                        }
                    }
                return false;
                }
            else
                {
                return getCollection().contains(converterDown.convert((T) o));
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<T> iterator()
            {
            return instantiateIterator(getCollection().iterator(),
                    getConverterUp());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object[] toArray()
            {
            return convertArray(getCollection().toArray(), getConverterUp());
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public <E> E[] toArray(E[] aoDest)
            {
            return (E[]) convertArray(getCollection().toArray(),
                    getConverterUp(),
                    aoDest);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean add(T o)
            {
            return getCollection().add(getConverterDown().convert(o));
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object o)
            {
            return getCollection().remove(getConverterDown().convert((T) o));
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean containsAll(Collection<?> col)
            {
            Converter<T, F> converterDown = getConverterDown();
            if (converterDown == NullImplementation.NullConverter.INSTANCE)
                {
                for (Object oValue : col)
                    {
                    if (!contains(oValue))
                        {
                        return false;
                        }
                    }
                return true;
                }
            else
                {
                return getCollection().containsAll(
                        instantiateCollection((Collection<T>) col,
                                              getConverterDown(),
                                              getConverterUp()));
                }
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean addAll(Collection<? extends T> col)
            {
            return getCollection().addAll(
                    instantiateCollection((Collection<T>) col,
                    getConverterDown(),
                    getConverterUp()));
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean removeAll(Collection<?> col)
            {
            return getCollection().removeAll(
                    instantiateCollection((Collection<T>) col,
                    getConverterDown(),
                    getConverterUp()));
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean retainAll(Collection<?> col)
            {
            return getCollection().retainAll(
                    instantiateCollection((Collection<T>) col,
                    getConverterDown(),
                    getConverterUp()));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void clear()
            {
            getCollection().clear();
            }


        // ----- Object methods ---------------------------------------------

        /**
         * Compares the specified object with this collection for equality.<p>
         * Obeys the general contract of Collection.equals.
         *
         * @param o  Object to be compared for equality with this Collection
         *
         * @return <tt>true</tt> if the specified object is equal to this
         *         Collection
         */
        @SuppressWarnings("rawtypes")
        public boolean equals(Object o)
            {
            if (o == this)
                {
                return true;
                }

            if (o instanceof ConverterCollection)
                {
                ConverterCollection that = (ConverterCollection) o;
                return this.getCollection()   .equals(that.getCollection()   )
                    && this.getConverterUp()  .equals(that.getConverterUp()  )
                    && this.getConverterDown().equals(that.getConverterDown());
                }
            return false;
            }

        /**
         * Return a String description for this collection.
         *
         * @return a String description of the Collection
         */
        public String toString()
            {
            StringBuffer sb = new StringBuffer();
            sb.append("ConverterCollection{");
            boolean fFirst = true;
            for (Object o : this)
                {
                if (fFirst)
                    {
                    fFirst = false;
                    }
                else
                    {
                    sb.append(", ");
                    }
                sb.append(o);
                }
            sb.append('}');
            return sb.toString();
            }


        // ----- lifecycle --------------------------------------------------

        /**
         * Drop references to the underlying Collection and the Converters.
         */
        public void invalidate()
            {
            m_col      = null;
            m_convUp   = null;
            m_convDown = null;
            }


        // ----- factory methods --------------------------------------------

        /**
         * Create a Converter Collection.
         *
         * @param col       the underlying Collection
         * @param convUp    the Converter to view the underlying Collection
         *                  through
         * @param convDown  the Converter to pass items down to the underlying
         *                  Collection through
         *
         * @param <F> the type of elements in the underlying Collection
         * @param <T> the type that the elements should be converted to
         *
         * @return a Converter Collection
         */
        protected <T, F> Collection<T> instantiateCollection(Collection<F> col, Converter<F, T> convUp,
                Converter<T, F> convDown)
            {
            return ConverterCollections.getCollection(col, convUp, convDown);
            }

        /**
         * Create a Converter Iterator.
         *
         * @param iter  the underlying Iterator
         * @param conv  the Converter to view the underlying Iterator through
         *
         * @return a Converter Iterator
         */
        protected Iterator<T> instantiateIterator(Iterator<F> iter, Converter<F, T> conv)
            {
            return ConverterCollections.getIterator(iter, conv);
            }


        // ----- accessors --------------------------------------------------

        /**
         * Return the underlying Collection.
         *
         * @return the underlying Collection
         */
        public Collection<F> getCollection()
            {
            return m_col;
            }

        /**
         * Return the Converter used to view the underlying Collection's
         * values through.
         *
         * @return the Converter from the underlying Collection
         */
        public Converter<F, T> getConverterUp()
            {
            return m_convUp;
            }

        /**
         * Return the Converter used to pass values down to the underlying
         * Collection.
         *
         * @return the Converter to the underlying Collection
         */
        public Converter<T, F> getConverterDown()
            {
            return m_convDown;
            }


        // ----- data members -----------------------------------------------

        /**
         * The underlying Collection.
         */
        protected Collection<F> m_col;

        /**
         * The Converter from the underlying Collection to this Collection.
         */
        protected Converter<F, T> m_convUp;

        /**
         * The Converter from this Collection to the underlying Collection.
         */
        protected Converter<T, F> m_convDown;
        }


    // ----- inner class: ConverterSet --------------------------------------

    /**
     * A Converter Set views an underlying Set through a Converter.
     *
     * @param <F> the type of elements in the underlying Set
     * @param <T> the type that the elements should be converted to
     */
    public static class ConverterSet<F, T>
            extends ConverterCollection<F, T>
            implements Set<T>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructor.
         *
         * @param col       the underlying Collection
         * @param convUp    the Converter from the underlying Set
         * @param convDown  the Converter to the underlying Set
         */
        // Note: we can not guarantee conformance to the Set contract as the
        //       the converter may return duplicates thus we widen the accepted
        //       type to be a Collection
        public ConverterSet(Collection<F> col, Converter<F, T> convUp, Converter<T, F> convDown)
            {
            super(col, convUp, convDown);
            }


        // ----- Object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o)
            {
            if (o == this)
                {
                return true;
                }

            if (o instanceof ConverterCollection)
                {
                ConverterCollection<F, T> that = (ConverterCollection<F, T>) o;
                return this.getCollection()   .equals(that.getCollection()   )
                    && this.getConverterUp()  .equals(that.getConverterUp()  )
                    && this.getConverterDown().equals(that.getConverterDown());
                }

            if (o instanceof Set)
                {
                Set<T> set = (Set<T>) o;
                return set.size() == this.size() && this.containsAll(set);
                }
            return false;
            }

        /**
         * {@inheritDoc}
         */
        public int hashCode()
            {
            int nHash = 0;
            for (Object o : this.getCollection())
                {
                nHash += NaturalHasher.INSTANCE.hashCode(o);
                }
            return nHash;
            }
        }


    // ----- inner class: ConverterSortedSet --------------------------------

    /**
     * A Converter SortedSet views an underlying SortedSet through a
     * Converter.
     *
     * @param <F> the type of elements in the underlying SortedSet
     * @param <T> the type that the elements should be converted to
     */
    public static class ConverterSortedSet<F, T>
            extends ConverterSet<F, T>
            implements SortedSet<T>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructor.
         *
         * @param set       the underlying SortedSet
         * @param convUp    the Converter from the underlying SortedSet
         * @param convDown  the Converter to the underlying SortedSet
         */
        public ConverterSortedSet(SortedSet<F> set, Converter<F, T> convUp, Converter<T, F> convDown)
            {
            super(set, convUp, convDown);
            }


        // ----- SortedSet interface ----------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public Comparator<T> comparator()
            {
            Comparator<? super F> comparator = getSortedSet().comparator();

            return comparator == null
                    ? null
                    : new ConverterComparator<F, T>(comparator, m_convDown);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SortedSet<T> subSet(T fromElement, T toElement)
            {
            SortedSet<F> subset = getSortedSet().subSet(
                    getConverterDown().convert(fromElement),
                    getConverterDown().convert(toElement));

            return instantiateSortedSet(subset, getConverterUp(), getConverterDown());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SortedSet<T> headSet(T toElement)
            {
            SortedSet<F> subset = getSortedSet().headSet(
                    getConverterDown().convert(toElement));

            return instantiateSortedSet(subset, getConverterUp(), getConverterDown());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SortedSet<T> tailSet(T fromElement)
            {
            SortedSet<F> subset = getSortedSet().tailSet(
                    getConverterDown().convert(fromElement));

            return instantiateSortedSet(subset, getConverterUp(), getConverterDown());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public T first()
            {
            return getConverterUp().convert(getSortedSet().first());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public T last()
            {
            return getConverterUp().convert(getSortedSet().last());
            }


        // ----- factory methods --------------------------------------------

        /**
         * Create a Converter SortedSet.
         *
         * @param set       the underlying SortedSet
         * @param convUp    the Converter to view the underlying SortedSet
         *                  through
         * @param convDown  the Converter to pass items down to the underlying
         *                  SortedSet through
         *
         * @return a Converter SortedSet
         */
        protected SortedSet<T> instantiateSortedSet(SortedSet<F> set, Converter<F, T> convUp, Converter<T, F> convDown)
            {
            return ConverterCollections.getSortedSet(set, convUp, convDown);
            }


        // ----- accessors --------------------------------------------------

        /**
         * Return the underlying SortedSet.
         *
         * @return the underlying SortedSet
         */
        public SortedSet<F> getSortedSet()
            {
            return (SortedSet<F>) getCollection();
            }
        }


    // ----- inner class: ConverterList -------------------------------------

    /**
     * A Converter List views an underlying List through a Converter.
     *
     * @param <F> the type of elements in the underlying List
     * @param <T> the type that the elements should be converted to
     */
    public static class ConverterList<F, T>
            extends ConverterCollection<F, T>
            implements List<T>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructor.
         *
         * @param list      the underlying List
         * @param convUp    the Converter from the underlying List
         * @param convDown  the Converter to the underlying List
         */
        public ConverterList(List<F> list, Converter<F, T> convUp, Converter<T, F> convDown)
            {
            super(list, convUp, convDown);
            }


        // ----- List interface ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        public T get(int index)
            {
            return getConverterUp().convert(getList().get(index));
            }


        /**
         * {@inheritDoc}
         */
        public T set(int index, T element)
            {
            return getConverterUp().convert(getList().set(index,
                    getConverterDown().convert(element)));
            }

        /**
         * {@inheritDoc}
         */
        public void add(int index, T element)
            {
            getList().add(index, getConverterDown().convert(element));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public boolean addAll(int index, Collection<? extends T> col)
            {
            return getList().addAll(index,
                    instantiateCollection((Collection<T>) col,
                    getConverterDown(),
                    getConverterUp()));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public T remove(int index)
            {
            return getConverterUp().convert(getList().remove(index));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public int indexOf(Object o)
            {
            return getList().indexOf(getConverterDown().convert((T) o));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public int lastIndexOf(Object o)
            {
            return getList().lastIndexOf(getConverterDown().convert((T) o));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListIterator<T> listIterator()
            {
            return instantiateListIterator(getList().listIterator(),
                    getConverterUp(),
                    getConverterDown());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListIterator<T> listIterator(int index)
            {
            return instantiateListIterator(getList().listIterator(index),
                    getConverterUp(),
                    getConverterDown());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<T> subList(int fromIndex, int toIndex)
            {
            return instantiateList(getList().subList(fromIndex, toIndex),
                    getConverterUp(),
                    getConverterDown());
            }


        // ----- Object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object o)
            {
            if (o == this)
                {
                return true;
                }

            if (o instanceof ConverterCollection)
                {
                ConverterCollection<T, F> that = (ConverterCollection<T, F>) o;
                return this.getCollection()   .equals(that.getCollection()   )
                    && this.getConverterUp()  .equals(that.getConverterUp()  )
                    && this.getConverterDown().equals(that.getConverterDown());
                }

            if (o instanceof List)
                {
                Iterator<T> iterThis = this.listIterator();
                Iterator<T> iterThat = ((List<T>) o).listIterator();

                while (iterThis.hasNext() && iterThat.hasNext())
                    {
                    if (!NaturalHasher.INSTANCE.equals(iterThis.next(), iterThat.next()))
                        {
                        return false;
                        }
                    }
                return !(iterThis.hasNext() || iterThat.hasNext());

                }
            return false;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public int hashCode()
            {
            int nHash = 1;
            for (Iterator<T> iter = this.listIterator(); iter.hasNext();)
                {
                nHash = 31 * nHash + NaturalHasher.INSTANCE.hashCode(iter.next());
                }
            return nHash;
            }


        // ----- factory methods --------------------------------------------

        /**
         * Create a Converter List.
         *
         * @param list      the underlying List
         * @param convUp    the Converter to view the underlying List
         *                  through
         * @param convDown  the Converter to pass items down to the underlying
         *                  List through
         *
         * @param <F> the type of elements in the underlying List
         * @param <T> the type that the elements should be converted to
         *
         * @return a Converter List
         */
        protected <F, T> List<T> instantiateList(List<F> list, Converter<F, T> convUp, Converter<T, F> convDown)
            {
            return ConverterCollections.getList(list, convUp, convDown);
            }

        /**
         * Create a Converter ListIterator.
         *
         * @param iter      the underlying ListIterator
         * @param convUp    the Converter to view the underlying ListIterator
         *                  through
         * @param convDown  the Converter to pass items down to the underlying
         *                  ListIterator through
         *
         * @param <F> the type of elements in the underlying ListIterator
         * @param <T> the type that the elements should be converted to
         *
         * @return a Converter ListIterator
         */
        protected <F, T> ListIterator<T> instantiateListIterator(ListIterator<F> iter, Converter<F, T> convUp,
                Converter<T, F> convDown)
            {
            return ConverterCollections.getListIterator(iter, convUp, convDown);
            }


        // ----- accessors --------------------------------------------------

        /**
         * Return the underlying List.
         *
         * @return the underlying List
         */
        public List<F> getList()
            {
            return (List<F>) getCollection();
            }
        }


    // ----- inner class: ConverterListIterator -----------------------------

    /**
     * A Converter ListIterator views an underlying ListIterator through a
     * Converter.
     *
     * @param <F> the type of elements in the underlying ListIterator
     * @param <T> the type that the elements should be converted to
     */
    public static class ConverterListIterator<F, T>
            implements ListIterator<T>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructor.
         *
         * @param iter      the underlying ListIterator
         * @param convUp    the Converter from the underlying ListIterator
         * @param convDown  the Converter to the underlying ListIterator
         */
        public ConverterListIterator(ListIterator<F> iter, Converter<F, T> convUp, Converter<T, F> convDown)
            {
            assert(iter != null && convUp != null && convDown != null);

            m_iter     = iter;
            m_convUp   = convUp;
            m_convDown = convDown;
            }


        // ----- ListIterator interface -------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext()
            {
            return getListIterator().hasNext();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public T next()
            {
            return getConverterUp().convert(getListIterator().next());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasPrevious()
            {
            return getListIterator().hasPrevious();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public T previous()
            {
            return getConverterUp().convert(getListIterator().previous());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int nextIndex()
            {
            return getListIterator().nextIndex();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int previousIndex()
            {
            return getListIterator().previousIndex();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove()
            {
            getListIterator().remove();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void set(T o)
            {
            getListIterator().set(getConverterDown().convert(o));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void add(T o)
            {
            getListIterator().add(getConverterDown().convert(o));
            }


        // ----- accessors --------------------------------------------------

        /**
         * Return the underlying ListIterator.
         *
         * @return the underlying ListIterator
         */
        public ListIterator<F> getListIterator()
            {
            return m_iter;
            }

        /**
         * Return the Converter used to view the underlying ListIterator's
         * values through.
         *
         * @return the Converter from the underlying ListIterator
         */
        public Converter<F, T> getConverterUp()
            {
            return m_convUp;
            }

        /**
         * Return the Converter used to pass values down to the underlying
         * ListIterator.
         *
         * @return the Converter to the underlying ListIterator
         */
        public Converter<T, F> getConverterDown()
            {
            return m_convDown;
            }


        // ----- data members -----------------------------------------------

        /**
         * The underlying ListIterator.
         */
        protected final ListIterator<F> m_iter;

        /**
         * The Converter from the underlying ListIterator to this
         * ListIterator.
         */
        protected final Converter<F, T> m_convUp;

        /**
         * The Converter from this ListIterator to the underlying
         * ListIterator.
         */
        protected final Converter<T, F> m_convDown;
        }


    // ----- inner class: ConverterMap --------------------------------------

    /**
     * A Converter Map views an underlying Map through a set of key and value
     * Converters.
     *
     * @param <FK> the type of the keys in the underlying Map
     * @param <TK> the type that the keys should be converted to
     * @param <FV> the type of the values in the underlying Map
     * @param <TV> the type that the values should be converted to
     */
    public static class ConverterMap<FK, TK, FV, TV>
            implements Map<TK, TV>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructor.
         *
         * @param map          the underlying Map
         * @param convKeyUp    the Converter to view the underlying Map's keys
         *                     through
         * @param convKeyDown  the Converter to use to pass keys down to the
         *                     underlying Map
         * @param convValUp    the Converter to view the underlying Map's
         *                     values through
         * @param convValDown  the Converter to use to pass values down to the
         *                     underlying Map
         */
        public ConverterMap(Map<FK, FV> map, Converter<FK, TK> convKeyUp, Converter<TK, FK> convKeyDown,
                Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
            {
            assert(map != null && convKeyUp != null && convKeyDown != null && convValUp != null && convValDown != null);

            m_map         = map;
            m_convKeyUp   = convKeyUp;
            m_convKeyDown = convKeyDown;
            m_convValUp   = convValUp;
            m_convValDown = convValDown;
            }

        @SuppressWarnings({"unchecked", "deprecation"})
        public Map<TK, TV> subMap(Set<TK> setKeys)
            {
            // we can only get here if the map is not empty, so it should never throw
            FK fk = m_map.entrySet().stream().findFirst().orElseThrow().getKey();

            boolean fPassThrough = fk instanceof Binary;
            boolean fDeco        = fPassThrough && ExternalizableHelper.isIntDecorated((Binary) fk);

            Map<FK, FV> map = new HashMap<>(setKeys.size());
            for (TK key : setKeys)
                {
                if (fPassThrough)
                    {
                    // pass-through, typically from Extend or gRPC proxy
                    if (fDeco)
                        {
                        // the keys are already decorated, so we can use them directly
                        map.put((FK) key, m_map.get((FK) key));
                        }
                    else
                        {
                        // otherwise, we have to remove the decoration in order to look up the value
                        Binary binKey = (Binary) key;
                        binKey = ExternalizableHelper.removeIntDecoration(binKey).toBinary();
                        map.put((FK) binKey, m_map.get((FK) binKey));
                        }
                    }
                else
                    {
                    // we are likely on the client or processing non-pass through Extend request,
                    // so we need to convert the key to Java type in order to look up the value
                    FK oKey = getConverterKeyDown().convert(key);
                    map.put(oKey, m_map.get(oKey));
                    }
                }

            return instantiateMap(map, m_convKeyUp, m_convKeyDown, m_convValUp, m_convValDown);
            }
        
        // ----- Map interface ----------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public int size()
            {
            return getMap().size();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEmpty()
            {
            return getMap().isEmpty();
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean containsKey(Object key)
            {
            return getMap().containsKey(getConverterKeyDown().convert((TK) key));
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean containsValue(Object value)
            {
            return getMap().containsValue(getConverterValueDown().convert((TV) value));
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public TV get(Object key)
            {
            return getConverterValueUp().convert(getMap().get(getConverterKeyDown().convert((TK) key)));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public TV put(TK key, TV value)
            {
            return getConverterValueUp().convert(
                    getMap().put(getConverterKeyDown().convert(key),
                    getConverterValueDown().convert(value)));
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public TV remove(Object key)
            {
            return getConverterValueUp().convert(getMap().remove(getConverterKeyDown().convert((TK) key)));
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public void putAll(Map<? extends TK, ? extends TV> map)
            {
            getMap().putAll(
                    instantiateMap((Map<TK, TV>) map, getConverterKeyDown(), getConverterKeyUp(),
                            getConverterValueDown(), getConverterValueUp()));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void clear()
            {
            getMap().clear();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<TK> keySet()
            {
            return instantiateSet(getMap().keySet(), getConverterKeyUp(), getConverterKeyDown());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<TV> values()
            {
            return instantiateCollection(getMap().values(), getConverterValueUp(), getConverterValueDown());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Entry<TK, TV>> entrySet()
            {
            if (m_set == null)
                {
                Set<Entry<FK, FV>> set = getMap().entrySet();
                m_set = instantiateEntrySet(set, getConverterKeyUp(), getConverterKeyDown(), getConverterValueUp(),
                        getConverterValueDown());
                }
            return m_set;
            }


        // ----- Object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            StringBuffer sb = new StringBuffer();
            sb.append("ConverterMap{");
            boolean fFirst = true;
            for (Object o : entrySet())
                {
                if (fFirst)
                    {
                    fFirst = false;
                    }
                else
                    {
                    sb.append(", ");
                    }
                sb.append(o);
                }
            sb.append("}");
            return sb.toString();
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object o)
            {
            if (o == this)
                {
                return true;
                }

            if (o instanceof Map)
                {
                return this.entrySet().equals(((Map) o).entrySet());
                }
            return false;
            }

        /**
        * {@inheritDoc}
        */
        @SuppressWarnings("unchecked")
        public int hashCode()
            {
            int nHash = 0;
            for (Object o : this.entrySet())
                {
                nHash += NaturalHasher.INSTANCE.hashCode(o);
                }
            return nHash;
            }


        // ----- factory methods --------------------------------------------

        /**
         * Create a Converter Collection.
         *
         * @param col       the underlying Collection
         * @param convUp    the Converter to view the underlying Collection
         *                  through
         * @param convDown  the Converter to pass items down to the underlying
         *                  Collection through
         * @param <F> the type of elements in the underlying Collection
         * @param <T> the type that the elements should be converted to
         *
         * @return a Converter Collection
         */
        protected <F, T> Collection<T> instantiateCollection(Collection<F> col, Converter<F, T> convUp,
                Converter<T, F> convDown)
            {
            return ConverterCollections.getCollection(col, convUp, convDown);
            }

        /**
         * Create a Converter Set.
         *
         * @param set       the underlying Set
         * @param convUp    the Converter to view the underlying Set through
         * @param convDown  the Converter to pass items down to the underlying
         *                  Set through
         *
         * @param <F> the type of elements in the underlying Set
         * @param <T> the type that the elements should be converted to
         *
         * @return a Converter Set
         */
        protected <T, F> Set<T> instantiateSet(Set<F> set, Converter<F, T> convUp, Converter<T, F> convDown)
            {
            return ConverterCollections.getSet(set, convUp, convDown);
            }

        /**
         * Create a Converter Map.
         *
         * @param map          the underlying Map
         * @param convKeyUp    the Converter to view the underlying Map's keys
         *                     through
         * @param convKeyDown  the Converter to use to pass keys down to the
         *                     underlying Map
         * @param convValUp    the Converter to view the underlying Map's
         *                     values through
         * @param convValDown  the Converter to use to pass values down to the
         *                     underlying Map
         *
         * @param <FK> the type of the keys in the underlying Map
         * @param <TK> the type that the keys should be converted to
         * @param <FV> the type of the values in the underlying Map
         * @param <TV> the type that the values should be converted to
         * @return a Converter Map
         */
        protected <FK, TK, FV, TV> Map<TK, TV> instantiateMap(Map<FK, FV> map, Converter<FK, TK> convKeyUp,
                Converter<TK, FK> convKeyDown, Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
            {
            return ConverterCollections.getMap(map, convKeyUp, convKeyDown, convValUp, convValDown);
            }

        /**
         * Create a Converter Entry Set.
         *
         * @param set          the underlying Map Entry Set
         * @param convKeyUp    the Converter to view the underlying Map's
         *                     Entry Set's keys through
         * @param convKeyDown  the Converter to use to pass keys down to the
         *                     underlying Map's Entry Set
         * @param convValUp    the Converter to view the underlying Map's
         *                     Entry Set's values through
         * @param convValDown  the Converter to use to pass values down to the
         *                     underlying Map's Entry Set
         *
         * @return a Converter Entry Set
         */
        protected Set<Entry<TK, TV>> instantiateEntrySet(Set<Entry<FK, FV>> set, Converter<FK, TK> convKeyUp,
                Converter<TK, FK> convKeyDown, Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
            {
            return ConverterCollections.getEntrySet(set, convKeyUp, convKeyDown, convValUp, convValDown);
            }


        // ----- accessors --------------------------------------------------

        /**
         * Return the underlying Map.
         *
         * @return the underlying Map
         */
        public Map<FK, FV> getMap()
            {
            return m_map;
            }

        /**
         * Return the Converter used to view the underlying Map's keys
         * through.
         *
         * @return the Converter from the underlying Map's keys
         */
        public Converter<FK, TK> getConverterKeyUp()
            {
            return m_convKeyUp;
            }

        /**
         * Return the Converter used to pass keys down to the underlying Map.
         *
         * @return the Converter to the underlying Map's keys
         */
        public Converter<TK, FK> getConverterKeyDown()
            {
            return m_convKeyDown;
            }

        /**
         * Return the Converter used to view the underlying Map's values
         * through.
         *
         * @return the Converter from the underlying Map's values
         */
        public Converter<FV, TV> getConverterValueUp()
            {
            return m_convValUp;
            }

        /**
         * Return the Converter used to pass values down to the underlying
         * Map.
         *
         * @return the Converter to the underlying Map's values
         */
        public Converter<TV, FV> getConverterValueDown()
            {
            return m_convValDown;
            }


        // ----- data members -----------------------------------------------

        /**
         * The underlying Map.
         */
        protected final Map<FK, FV> m_map;

        /**
         * The Converter used to view keys stored in the Map.
         */
        protected final Converter<FK, TK> m_convKeyUp;

        /**
         * The Converter used to pass keys down to the Map.
         */
        protected final Converter<TK, FK> m_convKeyDown;

        /**
         * The Converter used to view values stored in the Map.
         */
        protected final Converter<FV, TV> m_convValUp;

        /**
         * The Converter used to pass keys down to the Map.
         */
        protected final Converter<TV, FV> m_convValDown;

        /**
         * The Entry Set.
         */
        protected transient Set<Entry<TK, TV>> m_set;
        }


    // ----- inner class: ConverterSortedMap --------------------------------

    /**
     * A Converter SortedMap views an underlying SortedMap through a set of
     * key and value Converters.
     *
     * @param <FK> the type of the keys in the underlying SortedSet
     * @param <TK> the type that the keys should be converted to
     * @param <FV> the type of the values in the underlying SortedSet
     * @param <TV> the type that the values should be converted to
     */
    public static class ConverterSortedMap<FK, TK, FV, TV>
            extends ConverterMap<FK, TK, FV, TV>
            implements SortedMap<TK, TV>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructor.
         *
         * @param map          the underlying SortedMap
         * @param convKeyUp    the Converter to view the underlying
         *                     SortedMap's keys through
         * @param convKeyDown  the Converter to use to pass keys down to the
         *                     underlying SortedMap
         * @param convValUp    the Converter to view the underlying
         *                     SortedMap's values through
         * @param convValDown  the Converter to use to pass values down to the
         *                     underlying SortedMap
         */
        public ConverterSortedMap(SortedMap<FK, FV> map, Converter<FK, TK> convKeyUp, Converter<TK, FK> convKeyDown,
                Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
            {
            super(map, convKeyUp, convKeyDown, convValUp, convValDown);
            }


        // ----- SortedMap interface ----------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public Comparator<TK> comparator()
            {
            Comparator<? super FK> comparator = getSortedMap().comparator();
            if (comparator != null)
                {
                return new ConverterComparator<FK, TK>(comparator, m_convKeyDown);
                }
            return null;
            }


        /**
         * {@inheritDoc}
         */
        @Override
        public SortedMap<TK, TV> subMap(TK fromKey, TK toKey)
            {
            return instantiateSortedMap(getSortedMap().subMap(
                    getConverterKeyDown().convert(fromKey),
                    getConverterKeyDown().convert(toKey)),
                    getConverterKeyUp(),
                    getConverterKeyDown(),
                    getConverterValueUp(),
                    getConverterValueDown());
            }


        /**
         * {@inheritDoc}
         */
        @Override
        public SortedMap<TK, TV> headMap(TK toKey)
            {
            return instantiateSortedMap(getSortedMap().headMap(
                    getConverterKeyDown().convert(toKey)),
                    getConverterKeyUp(),
                    getConverterKeyDown(),
                    getConverterValueUp(),
                    getConverterValueDown());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SortedMap<TK, TV> tailMap(TK fromKey)
            {
            return instantiateSortedMap(getSortedMap().tailMap(getConverterKeyDown().convert(fromKey)),
                    getConverterKeyUp(),
                    getConverterKeyDown(),
                    getConverterValueUp(),
                    getConverterValueDown());
            }


        /**
         * {@inheritDoc}
         */
        @Override
        public TK firstKey()
            {
            return getConverterKeyUp().convert(getSortedMap().firstKey());
            }

        /**
         * {@inheritDoc}
         */
        public TK lastKey()
            {
            return getConverterKeyUp().convert(getSortedMap().lastKey());
            }


        // ----- factory methods --------------------------------------------

        /**
         * Create a Converter SortedMap.
         *
         * @param map          the underlying SortedMap
         * @param convKeyUp    the Converter to view the underlying
         *                     SortedMap's keys through
         * @param convKeyDown  the Converter to use to pass keys down to the
         *                     underlying SortedMap
         * @param convValUp    the Converter to view the underlying
         *                     SortedMap's values through
         * @param convValDown  the Converter to use to pass values down to the
         *                     underlying SortedMap
         *
         * @return a Converter SortedMap
         */
        protected SortedMap<TK, TV> instantiateSortedMap(SortedMap<FK, FV> map, Converter<FK, TK> convKeyUp,
                Converter<TK, FK> convKeyDown, Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
            {
            return ConverterCollections.getSortedMap(map, convKeyUp, convKeyDown, convValUp, convValDown);
            }


        // ----- accessors --------------------------------------------------

        /**
         * Return the underlying SortedMap.
         *
         * @return the underlying SortedMap
         */
        public SortedMap<FK, FV> getSortedMap()
            {
            return (SortedMap<FK, FV>) getMap();
            }
        }

    // ----- inner class: ConverterConcurrentMap ----------------------------

    /**
    * A Converter ConcurrentMap views an underlying ConcurrentMap through a
    * set of key and value Converters.
    */
    public static class ConverterConcurrentMap<FK, TK, FV, TV>
            extends ConverterMap<FK, TK, FV, TV>
            implements ConcurrentMap<TK, TV>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
        * Constructor.
        *
        * @param map          the underlying ConcurrentMap
        * @param convKeyUp    the Converter to view the underlying
        *                     ConcurrentMap's keys through
        * @param convKeyDown  the Converter to use to pass keys down to the
        *                     underlying ConcurrentMap
        * @param convValUp    the Converter to view the underlying
        *                     ConcurrentMap's values through
        * @param convValDown  the Converter to use to pass values down to the
        *                     underlying ConcurrentMap
        */
        public ConverterConcurrentMap(ConcurrentMap<FK, FV> map,
                                      Converter<FK, TK> convKeyUp, Converter<TK, FK> convKeyDown,
                                      Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
            {
            super(map, convKeyUp, convKeyDown, convValUp, convValDown);
            }


        // ----- ConcurrentMap interface ------------------------------------

        /**
        * {@inheritDoc}
        */
        @SuppressWarnings("unchecked")
        public boolean lock(Object oKey)
            {
            return getConcurrentMap().lock(getConverterKeyDown().convert((TK) oKey));
            }

        /**
        * {@inheritDoc}
        */
        @SuppressWarnings("unchecked")
        public boolean lock(Object oKey, long cWait)
            {
            return getConcurrentMap().lock(getConverterKeyDown().convert((TK) oKey), cWait);
            }

        /**
        * {@inheritDoc}
        */
        @SuppressWarnings("unchecked")
        public boolean unlock(Object oKey)
            {
            return getConcurrentMap().unlock(getConverterKeyDown().convert((TK) oKey));
            }


        // ----- accessors --------------------------------------------------

        /**
        * Return the underlying ConcurrentMap.
        *
        * @return the underlying ConcurrentMap
        */
        public ConcurrentMap<FK, FV> getConcurrentMap()
            {
            return (ConcurrentMap<FK, FV>) getMap();
            }
        }


    // ----- inner class: ConverterInvocableMap -----------------------------

    /**
    * A Converter InvocableMap views an underlying InvocableMap through a
    * set of key and value Converters.
    */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static class ConverterInvocableMap<FK, TK, FV, TV>
            extends ConverterMap<FK, TK, FV, TV>
            implements InvocableMap<TK, TV>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
        * Constructor.
        *
        * @param map          the underlying InvocableMap
        * @param convKeyUp    the Converter to view the underlying
        *                     InvocableMap's keys through
        * @param convKeyDown  the Converter to use to pass keys down to the
        *                     underlying InvocableMap
        * @param convValUp    the Converter to view the underlying
        *                     InvocableMap's values through
        * @param convValDown  the Converter to use to pass values down to the
        *                     underlying InvocableMap
        */
        public ConverterInvocableMap(InvocableMap<FK, FV> map,
                                     Converter<FK, TK> convKeyUp, Converter<TK, FK> convKeyDown,
                                     Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
            {
            super(map, convKeyUp, convKeyDown, convValUp, convValDown);
            }


        // ----- InvocableMap interface -------------------------------------

        /**
        * {@inheritDoc}
        */
        public <R> R aggregate(Collection<? extends TK> collKeys, EntryAggregator<? super TK, ? super TV, R> agent)
            {
            Converter<TK, FK> convKeyDown = getConverterKeyDown();
            Converter<FK, TK> convKeyUp   = getConverterKeyUp();

            Collection<? extends FK> colKeysConv = collKeys instanceof Set ?
                    instantiateSet((Set) collKeys, convKeyDown, convKeyUp) :
                    instantiateCollection((Collection) collKeys, convKeyDown, convKeyUp);

            // the EntryAggregator is not converted to work against FK & FV
            // this is expected / required behavior for usages such as extend
            // and more generically where there is no real type conversion but
            // instead converts between the real type and binary; we could introduce
            // a Binary specific version of ConverterNC that passes the function/aggregator
            // without type conversion
            return (R) convertSafe(
                    getConverterValueUp(),
                    getInvocableMap().aggregate(colKeysConv, (EntryAggregator) agent));
            }

        /**
        * {@inheritDoc}
        */
        public <R> R aggregate(Filter filter, EntryAggregator<? super TK, ? super TV, R> agent)
            {
            return (R) convertSafe(
                    getConverterValueUp(),
                    getInvocableMap().aggregate(filter, (EntryAggregator) agent));
            }

        /**
        * {@inheritDoc}
        */
        public <R> R invoke(TK key, EntryProcessor<TK, TV, R> agent)
            {
            Object oResult = getInvocableMap().invoke(
                    getConverterKeyDown().convert(key), (EntryProcessor) agent);

            return (R) convertSafe(getConverterValueUp(), oResult);
            }

        /**
        * {@inheritDoc}
        */
        public <R> Map<TK, R> invokeAll(Collection<? extends TK> collKeys, EntryProcessor<TK, TV, R> agent)
            {
            Converter<TK, FK> convKeyDown = getConverterKeyDown();
            Converter<FK, TK> convKeyUp   = getConverterKeyUp();

            Collection<? extends FK> colKeysConv = collKeys instanceof Set ?
                    instantiateSet((Set) collKeys, convKeyDown, convKeyUp) :
                    instantiateCollection((Collection) collKeys, convKeyDown, convKeyUp);

            Map<FK, R> mapResult = getInvocableMap().invokeAll(colKeysConv, (EntryProcessor) agent);
            return mapResult == null || mapResult.isEmpty()
                   ? Collections.emptyMap()
                   : instantiateMap(mapResult, convKeyUp, convKeyDown,
                        value -> (R) convertSafe(getConverterValueUp(), value),
                        value -> (R) convertSafe(getConverterValueDown(), value));
            }

        /**
        * {@inheritDoc}
        */
        public <R> Map<TK, R> invokeAll(Filter filter, EntryProcessor<TK, TV, R> agent)
            {
            Map<FK, R> mapResult = getInvocableMap().invokeAll(filter, (EntryProcessor) agent);
            return mapResult == null || mapResult.isEmpty()
                   ? Collections.emptyMap()
                   : instantiateMap(mapResult, getConverterKeyUp(), getConverterKeyDown(),
                    value -> (R) convertSafe(getConverterValueUp(), value),
                    value -> (R) convertSafe(getConverterValueDown(), value));
            }

        @Override
        public TV putIfAbsent(TK key, TV value)
            {
            // inline the default impl of InvocableMap.putIfAbsent to allow
            // the value (EP payload) to go through the necessary conversion
            // (the key is converted by the invoke implementation)
            
            EntryProcessor processor = CacheProcessors.putIfAbsent(getConverterValueDown().convert(value));
            return (TV) invoke(key, processor);
            }

        @Override
        public boolean remove(Object key, Object value)
            {
            EntryProcessor processor = CacheProcessors.remove(getConverterValueDown().convert((TV) value));
            return (Boolean) invoke((TK) key, processor);
            }

        @Override
        public boolean replace(TK key, TV oldValue, TV newValue)
            {
            EntryProcessor processor = CacheProcessors.replace(
                    getConverterValueDown().convert(oldValue),
                    getConverterValueDown().convert(newValue));

            return (Boolean) invoke(key, processor);
            }

        @Override
        public TV replace(TK key, TV value)
            {
            EntryProcessor processor = CacheProcessors.replace(
                    getConverterValueDown().convert(value));

            // the EP result already goes through the converter
            return (TV) invoke(key, processor);
            }

        @Override
        public TV merge(TK key, TV value, Remote.BiFunction<? super TV, ? super TV, ? extends TV> remappingFunction)
            {
            EntryProcessor processor = CacheProcessors.merge(
                    getConverterValueDown().convert(value),
                    (Remote.BiFunction) remappingFunction);

            // the EP result already goes through the converter
            return (TV) invoke(key, processor);
            }

        @Override
        public TV merge(TK key, TV value, BiFunction<? super TV, ? super TV, ? extends TV> remappingFunction)
            {
            EntryProcessor processor = CacheProcessors.merge(
                    getConverterValueDown().convert(value),
                    (Remote.BiFunction) remappingFunction);

            // the EP result already goes through the converter
            return (TV) invoke(key, processor);
            }

        // ----- accessors --------------------------------------------------

        /**
        * Return the underlying InvocableMap.
        *
        * @return the underlying InvocableMap
        */
        public InvocableMap<FK, FV> getInvocableMap()
            {
            return (InvocableMap<FK, FV>) getMap();
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Convert the provided value with the given converter. If there is an
         * issue in type conversion return the given value.
         *
         * @param converter  the converter to use
         * @param oValue     the value to convert
         *
         * @return a converted value or the original value
         */
        protected static Object convertSafe(Converter converter, Object oValue)
            {
            try
                {
                return oValue == null ? null : converter.convert(oValue);
                }
            catch (ClassCastException ignore) {}

            return oValue;
            }
        }


    // ----- inner class: ConverterObservableMap ----------------------------

    /**
    * A Converter ObservableMap views an underlying ObservableMap through a
    * set of key and value Converters.
    */
    public static class ConverterObservableMap<FK, TK, FV, TV>
            extends ConverterMap<FK, TK, FV, TV>
            implements ObservableMap<TK, TV>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
        * Constructor.
        *
        * @param map          the underlying ObservableMap
        * @param convKeyUp    the Converter to view the underlying
        *                     ObservableMap's keys through
        * @param convKeyDown  the Converter to use to pass keys down to the
        *                     underlying ObservableMap
        * @param convValUp    the Converter to view the underlying
        *                     ObservableMap's values through
        * @param convValDown  the Converter to use to pass values down to the
        *                     underlying ObservableMap
        */
        public ConverterObservableMap(ObservableMap<FK, FV> map, Converter<FK, TK> convKeyUp,
                                      Converter<TK, FK> convKeyDown, Converter<FV, TV> convValUp,
                                      Converter<TV, FV> convValDown)
            {
            super(map, convKeyUp, convKeyDown, convValUp, convValDown);
            }


        // ----- ObservableMap interface ------------------------------------

        /**
        * {@inheritDoc}
        */
        public void addMapListener(MapListener<? super TK, ? super TV> listener)
            {
            getObservableMap().addMapListener(getConverterListener(listener));
            }

        /**
        * {@inheritDoc}
        */
        public void removeMapListener(MapListener<? super TK, ? super TV> listener)
            {
            getObservableMap().removeMapListener(getConverterListener(listener));
            }

        /**
        * {@inheritDoc}
        */
        public void addMapListener(MapListener<? super TK, ? super TV> listener, TK key, boolean fLite)
            {
            getObservableMap().addMapListener(getConverterListener(listener),
                    getConverterKeyDown().convert(key), fLite);
            }

        /**
        * {@inheritDoc}
        */
        public void removeMapListener(MapListener<? super TK, ? super TV> listener, TK key)
            {
            getObservableMap().removeMapListener(getConverterListener(listener),
                    getConverterKeyDown().convert(key));
            }

        /**
        * {@inheritDoc}
        */
        public void addMapListener(MapListener<? super TK, ? super TV> listener, Filter filter, boolean fLite)
            {
            getObservableMap().addMapListener(getConverterListener(listener), filter, fLite);
            }

        /**
        * {@inheritDoc}
        */
        public void removeMapListener(MapListener<? super TK, ? super TV> listener, Filter filter)
            {
            getObservableMap().removeMapListener(getConverterListener(listener), filter);
            }


        // ----- helpers ----------------------------------------------------

        /**
        * Create a converter listener for the specified listener.
        *
        * @param listener  the underlying listener
        *
        * @return  the converting listener
        */
        @SuppressWarnings("unchecked")
        protected MapListener<? super FK, ? super FV> getConverterListener(MapListener<? super TK, ? super TV> listener)
            {
            // special case MapTriggerListener and NamedCacheDeactivationListener,
            // as they're not "real" listeners
            if (listener instanceof MapTriggerListener ||
                listener instanceof NamedCacheDeactivationListener)
                {
                return (MapListener<? super FK, ? super FV>) listener;
                }

            return (MapListener<? super FK, ? super FV>)
                    getMapListener(this, listener, getConverterKeyUp(), getConverterValueUp());
            }


        // ----- accessors --------------------------------------------------

        /**
        * Return the underlying ObservableMap.
        *
        * @return the underlying ObservableMap
        */
        public ObservableMap<FK, FV> getObservableMap()
            {
            return (ObservableMap<FK, FV>) getMap();
            }
        }


    // ----- inner class: ConverterQueryMap ---------------------------------

    /**
    * A Converter QueryMap views an underlying QueryMap through a set of key
    * and value Converters.
    */
    public static class ConverterQueryMap<FK, TK, FV, TV>
            extends ConverterMap<FK, TK, FV, TV>
            implements QueryMap<TK, TV>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
        * Constructor.
        *
        * @param map          the underlying QueryMap
        * @param convKeyUp    the Converter to view the underlying QueryMap's
        *                     keys through
        * @param convKeyDown  the Converter to use to pass keys down to the
        *                     underlying QueryMap
        * @param convValUp    the Converter to view the underlying QueryMap's
        *                     values through
        * @param convValDown  the Converter to use to pass values down to the
        *                     underlying QueryMap
        */
        public ConverterQueryMap(QueryMap<FK, FV> map, Converter<FK, TK> convKeyUp,
                                 Converter<TK, FK> convKeyDown, Converter<FV, TV> convValUp,
                                 Converter<TV, FV> convValDown)
            {
            super(map, convKeyUp, convKeyDown, convValUp, convValDown);
            }


        // ----- QueryMap interface -----------------------------------------

        /**
        * {@inheritDoc}
        */
        public Set<TK> keySet(Filter filter)
            {
            return instantiateSet(getQueryMap().keySet(filter),
                    getConverterKeyUp(),
                    getConverterKeyDown());
            }

        /**
        * {@inheritDoc}
        */
        public Set<Map.Entry<TK, TV>> entrySet(Filter filter)
            {
            return instantiateEntrySet(getQueryMap().entrySet(filter),
                    getConverterKeyUp(),
                    getConverterKeyDown(),
                    getConverterValueUp(),
                    getConverterValueDown());
            }

        /**
        * {@inheritDoc}
        */
        public Set<Map.Entry<TK, TV>> entrySet(Filter filter, Comparator comparator)
            {
            return instantiateEntrySet(getQueryMap().entrySet(filter, comparator),
                    getConverterKeyUp(),
                    getConverterKeyDown(),
                    getConverterValueUp(),
                    getConverterValueDown());
            }

        /**
        * {@inheritDoc}
        */
        public <T, E> void addIndex(ValueExtractor<? super T, ? extends E> extractor,
                                    boolean fOrdered, Comparator<? super E> comparator)
            {
            getQueryMap().addIndex(extractor, fOrdered, comparator);
            }

        /**
        * {@inheritDoc}
        */
        public <T, E> void removeIndex(ValueExtractor<? super T, ? extends E> extractor)
            {
            getQueryMap().removeIndex(extractor);
            }


        // ----- accessors --------------------------------------------------

        /**
        * Return the underlying QueryMap.
        *
        * @return the underlying QueryMap
        */
        public QueryMap<FK, FV> getQueryMap()
            {
            return (QueryMap<FK, FV>) getMap();
            }
        }


    // ----- inner class: ConverterCacheMap ---------------------------------

    /**
    * A Converter CacheMap views an underlying CacheMap through a set of key
    * and value Converters.
    */
    public static class ConverterCacheMap<FK, TK, FV, TV>
            extends ConverterObservableMap<FK, TK, FV, TV>
            implements CacheMap<TK, TV>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
        * Constructor.
        *
        * @param map          the underlying CacheMap
        * @param convKeyUp    the Converter to view the underlying CacheMap's
        *                     keys through
        * @param convKeyDown  the Converter to use to pass keys down to the
        *                     underlying CacheMap
        * @param convValUp    the Converter to view the underlying CacheMap's
        *                     values through
        * @param convValDown  the Converter to use to pass values down to the
        *                     underlying CacheMap
        */
        public ConverterCacheMap(CacheMap<FK, FV> map, Converter<FK, TK> convKeyUp,
                                 Converter<TK, FK> convKeyDown, Converter<FV, TV> convValUp,
                                 Converter<TV, FV> convValDown)
            {
            super(map, convKeyUp, convKeyDown, convValUp, convValDown);
            }


        // ----- CacheMap interface -----------------------------------------

        /**
        * {@inheritDoc}
        */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Map<TK, TV> getAll(Collection<? extends TK> colKeys)
            {
            Converter  convKeyDown = getConverterKeyDown();
            Converter  convKeyUp   = getConverterKeyUp();
            Converter  convValDown = getConverterValueDown();
            Converter  convValUp   = getConverterValueUp();
            Collection colKeysConv = colKeys instanceof Set ?
                    instantiateSet((Set) colKeys, convKeyDown, convKeyUp) :
                    instantiateCollection(colKeys, convKeyDown, convKeyUp);

            return instantiateMap(getCacheMap().getAll(colKeysConv),
                    convKeyUp, convKeyDown, convValUp, convValDown);
            }

        /**
        * {@inheritDoc}
        */
        public TV put(TK key, TV value, long cMillis)
            {
            return getConverterValueUp().convert(getCacheMap().put(
                    getConverterKeyDown().convert(key),
                    getConverterValueDown().convert(value),
                    cMillis));
            }


        // ----- accessors --------------------------------------------------

        /**
        * Return the underlying CacheMap.
        *
        * @return the underlying CacheMap
        */
        public CacheMap<FK, FV> getCacheMap()
            {
            return (CacheMap<FK, FV>) getMap();
            }
        }


    // ----- inner class: ConverterNamedCache -------------------------------

    /**
    * A Converter NamedCache views an underlying NamedCache through a set of
    * key and value Converters.
    */
    public static class ConverterNamedCache<FK, TK, FV, TV>
            extends ConverterCacheMap<FK, TK, FV, TV>
            implements NamedCache<TK, TV>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
        * Constructor.
        *
        * @param cache        the underlying NamedCache
        * @param convKeyUp    the Converter to view the underlying
        *                     NamedCache's keys through
        * @param convKeyDown  the Converter to use to pass keys down to the
        *                     underlying NamedCache
        * @param convValUp    the Converter to view the underlying
        *                     NamedCache's values through
        * @param convValDown  the Converter to use to pass values down to the
        *                     underlying NamedCache
        */
        public ConverterNamedCache(NamedCache<FK, FV> cache, Converter<FK, TK> convKeyUp,
                                   Converter<TK, FK> convKeyDown, Converter<FV, TV> convValUp,
                                   Converter<TV, FV> convValDown)
            {
            super(cache, convKeyUp, convKeyDown, convValUp, convValDown);

            m_mapConcurrent = ConverterCollections.getConcurrentMap(cache,
                    convKeyUp, convKeyDown, convValUp, convValDown);
            m_mapInvocable  = ConverterCollections.getInvocableMap (cache,
                    convKeyUp, convKeyDown, convValUp, convValDown);
            m_mapQuery      = ConverterCollections.getQueryMap     (cache,
                    convKeyUp, convKeyDown, convValUp, convValDown);
            }


        // ----- NamedCache interface ---------------------------------------

        @Override
        public AsyncNamedCache<TK, TV> async()
            {
            return new ConverterAsyncNamedCache<>(getNamedCache().async(), m_convKeyUp, m_convKeyDown, m_convValUp, m_convValDown);
            }

        @Override
        public AsyncNamedCache<TK, TV> async(AsyncNamedMap.Option... options)
            {
            return new ConverterAsyncNamedCache<>(getNamedCache().async(options), m_convKeyUp, m_convKeyDown, m_convValUp, m_convValDown);
            }

        /**
        * {@inheritDoc}
        */
        public String getCacheName()
            {
            return getNamedCache().getCacheName();
            }

        /**
        * {@inheritDoc}
        */
        public CacheService getCacheService()
            {
            return getNamedCache().getCacheService();
            }

        /**
        * {@inheritDoc}
        */
        public boolean isActive()
            {
            return getNamedCache().isActive();
            }

        @Override
        public boolean isReady()
            {
            return getNamedCache().isReady();
            }

        /**
        * {@inheritDoc}
        */
        public void release()
            {
            getNamedCache().release();
            }

        /**
        * {@inheritDoc}
        */
        public void destroy()
            {
            getNamedCache().destroy();
            }

        /**
        * {@inheritDoc}
        */
        public void truncate()
            {
            getNamedCache().truncate();
            }

        /**
         * {@inheritDoc}
         */
        public boolean isDestroyed()
            {
            return getNamedCache().isDestroyed();
            }

        /**
         * {@inheritDoc}
         */
        public boolean isReleased()
            {
            return getNamedCache().isReleased();
            }

        // ----- ConcurrentMap interface ------------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean lock(Object oKey, long cWait)
            {
            return m_mapConcurrent.lock(oKey, cWait);
            }

        /**
        * {@inheritDoc}
        */
        public boolean lock(Object oKey)
            {
            return m_mapConcurrent.lock(oKey);
            }

        /**
        * {@inheritDoc}
        */
        public boolean unlock(Object oKey)
            {
            return m_mapConcurrent.unlock(oKey);
            }


        // ----- InvocableMap interface -------------------------------------

        /**
        * {@inheritDoc}
        */
        public <R> R invoke(TK key, EntryProcessor<TK, TV, R> agent)
            {
            return m_mapInvocable.invoke(key, agent);
            }

        /**
        * {@inheritDoc}
        */
        public <R> Map<TK, R> invokeAll(Collection<? extends TK> collKeys, EntryProcessor<TK, TV, R> agent)
            {
            return m_mapInvocable.invokeAll(collKeys, agent);
            }

        /**
        * {@inheritDoc}
        */
        public <R> Map<TK, R> invokeAll(Filter filter, EntryProcessor<TK, TV, R> agent)
            {
            return m_mapInvocable.invokeAll(filter, agent);
            }

        /**
        * {@inheritDoc}
        */
        public <R> R aggregate(Collection<? extends TK> collKeys, EntryAggregator<? super TK, ? super TV, R> agent)
            {
            return m_mapInvocable.aggregate(collKeys, agent);
            }

        /**
        * {@inheritDoc}
        */
        public <R> R aggregate(Filter filter, EntryAggregator<? super TK, ? super TV, R> agent)
            {
            return m_mapInvocable.aggregate(filter, agent);
            }


        // ----- QueryMap interface -----------------------------------------

        /**
        * {@inheritDoc}
        */
        public Set<TK> keySet(Filter filter)
            {
            return m_mapQuery.keySet(filter);
            }

        /**
        * {@inheritDoc}
        */
        public Set<Map.Entry<TK, TV>> entrySet(Filter filter)
            {
            return m_mapQuery.entrySet(filter);
            }

        /**
        * {@inheritDoc}
        */
        public Set<Map.Entry<TK, TV>> entrySet(Filter filter, Comparator comparator)
            {
            return m_mapQuery.entrySet(filter, comparator);
            }

        /**
        * {@inheritDoc}
        */
        public <T, E> void addIndex(ValueExtractor<? super T, ? extends E> extractor,
                                    boolean fOrdered, Comparator<? super E> comparator)
            {
            m_mapQuery.addIndex(extractor, fOrdered, comparator);
            }

        /**
        * {@inheritDoc}
        */
        public <T, E> void removeIndex(ValueExtractor<? super T, ? extends E> extractor)
            {
            m_mapQuery.removeIndex(extractor);
            }

        @Override
        public TV putIfAbsent(TK key, TV value)
            {
            return m_mapInvocable.putIfAbsent(key, value);
            }

        @Override
        public boolean remove(Object key, Object value)
            {
            return m_mapInvocable.remove(key, value);
            }
        @Override
        public boolean replace(TK key, TV oldValue, TV newValue)
            {
            return m_mapInvocable.replace(key, oldValue, newValue);
            }

        @Override
        public TV replace(TK key, TV value)
            {
            return m_mapInvocable.replace(key, value);
            }

        @Override
        public TV merge(TK key, TV value, Remote.BiFunction<? super TV, ? super TV, ? extends TV> remappingFunction)
            {
            return m_mapInvocable.merge(key, value, remappingFunction);
            }

        @Override
        public TV merge(TK key, TV value, BiFunction<? super TV, ? super TV, ? extends TV> remappingFunction)
            {
            return m_mapInvocable.merge(key, value, remappingFunction);
            }
        
        // ----- accessors --------------------------------------------------

        /**
        * Return the underlying NamedCache.
        *
        * @return the underlying NamedCache
        */
        public NamedCache<FK, FV> getNamedCache()
            {
            return (NamedCache<FK, FV>) getMap();
            }


        // ----- data members -----------------------------------------------

        /**
        * A Converter ConcurrentMap around the underlying NamedCache.
        */
        protected ConcurrentMap<TK, TV> m_mapConcurrent;

        /**
        * A Converter InvocableMap around the underlying NamedCache.
        */
        protected InvocableMap<TK, TV>  m_mapInvocable;

        /**
        * A Converter QueryMap around the underlying NamedCache.
        */
        protected QueryMap<TK, TV>      m_mapQuery;
        }

    // ----- inner class: ConverterAsyncNamedCache --------------------------

    public static class ConverterAsyncNamedCache<FK, TK, FV, TV>
            implements AsyncNamedCache<TK, TV>
        {
        // ----- constructors -----------------------------------------------

        /**
        * Constructor.
        *
        * @param cache        the underlying {@link AsyncNamedCache}
        * @param convKeyUp    the Converter to view the underlying
        *                     AsyncNamedMap's keys through
        * @param convKeyDown  the Converter to use to pass keys down to the
        *                     underlying AsyncNamedMap
        * @param convValUp    the Converter to view the underlying
        *                     AsyncNamedMap's values through
        * @param convValDown  the Converter to use to pass values down to the
        *                     underlying AsyncNamedMap
        */
        public ConverterAsyncNamedCache(AsyncNamedCache<FK, FV> cache, Converter<FK, TK> convKeyUp,
                                        Converter<TK, FK> convKeyDown, Converter<FV, TV> convValUp,
                                        Converter<TV, FV> convValDown)
            {
            m_asyncNamedCache = cache;
            m_convKeyUp       = convKeyUp;
            m_convKeyDown     = convKeyDown;
            m_convValUp       = convValUp;
            m_convValDown     = convValDown;
            m_namedCache      = new ConverterNamedCache<>(cache.getNamedCache(), convKeyUp, convKeyDown, convValUp, convValDown);
            }

        @Override
        public NamedMap<TK, TV> getNamedMap()
            {
            return m_namedCache;
            }

        @Override
        public NamedCache<TK, TV> getNamedCache()
            {
            return m_namedCache;
            }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public <R> CompletableFuture<R> invoke(TK key, EntryProcessor<TK, TV, R> processor)
            {
            CompletableFuture<Object> future = m_asyncNamedCache.invoke(m_convKeyDown.convert(key), (EntryProcessor) processor);

            return future.thenApply(oResult -> (R) ConverterInvocableMap.convertSafe(m_convValUp, oResult));
            }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public <R> CompletableFuture<Map<TK, R>> invokeAll(Collection<? extends TK> collKeys, EntryProcessor<TK, TV, R> processor)
            {
            Collection<? extends FK> colKeysConv = collKeys instanceof Set ?
                    m_namedCache.instantiateSet((Set) collKeys, m_convKeyDown, m_convKeyUp) :
                    m_namedCache.instantiateCollection((Collection) collKeys, m_convKeyDown, m_convKeyUp);

            CompletableFuture<Map<FK, R>> future = m_asyncNamedCache.invokeAll(colKeysConv, (EntryProcessor) processor);
            return future.thenApply(this::instantiateMap);
            }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public <R> CompletableFuture<Map<TK, R>> invokeAll(Filter<?> filter, EntryProcessor<TK, TV, R> processor)
            {
            CompletableFuture<Map<FK, R>> future = m_asyncNamedCache.invokeAll(filter, (EntryProcessor) processor);
            return future.thenApply(this::instantiateMap);
            }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public <R> CompletableFuture<Void> invokeAll(Collection<? extends TK> collKeys, EntryProcessor<TK, TV, R> processor, Consumer<? super Entry<? extends TK, ? extends R>> callback)
            {
            Collection<? extends FK> colKeysConv = collKeys instanceof Set ?
                    m_namedCache.instantiateSet((Set) collKeys, m_convKeyDown, m_convKeyUp) :
                    m_namedCache.instantiateCollection((Collection) collKeys, m_convKeyDown, m_convKeyUp);

            Consumer consumer = instantiateCallback(callback);
            return m_asyncNamedCache.invokeAll(colKeysConv, (EntryProcessor) processor, consumer);
            }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public <R> CompletableFuture<Void> invokeAll(Filter<?> filter, EntryProcessor<TK, TV, R> processor, Consumer<? super Entry<? extends TK, ? extends R>> callback)
            {
            Consumer consumer = instantiateCallback(callback);
            return m_asyncNamedCache.invokeAll(filter, (EntryProcessor) processor, consumer);
            }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public <R> CompletableFuture<R> aggregate(Collection<? extends TK> collKeys, EntryAggregator<? super TK, ? super TV, R> aggregator)
            {
            Collection<? extends FK> colKeysConv = collKeys instanceof Set ?
                    m_namedCache.instantiateSet((Set) collKeys, m_convKeyDown, m_convKeyUp) :
                    m_namedCache.instantiateCollection((Collection) collKeys, m_convKeyDown, m_convKeyUp);

            // the EntryAggregator is not converted to work against FK & FV
            // this is expected / required behavior for usages such as extend
            // and more generically where there is no real type conversion but
            // instead converts between the real type and binary; we could introduce
            // a Binary specific version of ConverterNC that passes the function/aggregator
            // without type conversion
            CompletableFuture future = m_asyncNamedCache.aggregate(colKeysConv, (EntryAggregator) aggregator);
            return future.thenApply(r -> ConverterInvocableMap.convertSafe(m_convValUp, r));
            }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public <R> CompletableFuture<R> aggregate(Filter<?> filter, EntryAggregator<? super TK, ? super TV, R> aggregator)
            {
            // the EntryAggregator is not converted to work against FK & FV
            // this is expected / required behavior for usages such as extend
            // and more generically where there is no real type conversion but
            // instead converts between the real type and binary; we could introduce
            // a Binary specific version of ConverterNC that passes the function/aggregator
            // without type conversion
            CompletableFuture future = m_asyncNamedCache.aggregate(filter, (EntryAggregator) aggregator);
            return future.thenApply(r -> ConverterInvocableMap.convertSafe(m_convValUp, r));
            }

        // ----- helper methods -------------------------------------------------

        @SuppressWarnings({"unchecked"})
        protected <R> Map<TK, R> instantiateMap(Map<FK, R> mapResult)
            {
            return mapResult == null || mapResult.isEmpty()
                    ? Collections.emptyMap()
                    : m_namedCache.instantiateMap(mapResult, m_convKeyUp, m_convKeyDown,
                    value -> (R) ConverterInvocableMap.convertSafe(m_convValUp, value),
                    value -> (R) ConverterInvocableMap.convertSafe(m_convValDown, value));
            }

        @SuppressWarnings({"unchecked", "rawtypes"})
        protected <R> Consumer<? extends Entry<FK, R>> instantiateCallback(Consumer<? super Entry<? extends TK, ? extends R>> callback)
            {
            return (entry) ->
                {
                Entry<TK, R> entryConv = new ConverterEntry<>(entry, m_convKeyUp, (Converter) m_convKeyUp, (Converter) m_convValDown);
                callback.accept(entryConv);
                };
            }

        // ----- data members -----------------------------------------------

        /**
         * The underlying AsyncNamedMap.
         */
        protected final AsyncNamedMap<FK, FV> m_asyncNamedCache;

        /**
         * A converter version of the underlying {@link NamedCache}.
         */
        protected final ConverterNamedCache<FK, TK, FV, TV> m_namedCache;

        /**
         * The Converter used to view keys stored in the Map.
         */
        protected final Converter<FK, TK> m_convKeyUp;

        /**
         * The Converter used to pass keys down to the Map.
         */
        protected final Converter<TK, FK> m_convKeyDown;

        /**
         * The Converter used to view values stored in the Map.
         */
        protected final Converter<FV, TV> m_convValUp;

        /**
         * The Converter used to pass keys down to the Map.
         */
        protected final Converter<TV, FV> m_convValDown;
        }

    // ----- inner class: ConverterEntrySet ---------------------------------

    /**
     * A Converter Entry Set views an underlying Entry Set through a set of
     * key and value Converters.
     *
     * @param <FK> the type of the keys in the underlying EntrySet
     * @param <TK> the type that the keys should be converted to
     * @param <FV> the type of the values in the underlying EntrySet
     * @param <TV> the type that the values should be converted to
     */
    public static class ConverterEntrySet<FK, TK, FV, TV>
            implements Set<Map.Entry<TK, TV>>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructor.
         *
         * @param set          the underlying Entry Set (or Collection of
         *                     Map Entry objects)
         * @param convKeyUp    the Converter to view the underlying
         *                     Entry Set's keys through
         * @param convKeyDown  the Converter to use to pass keys down to the
         *                     underlying Entry Set
         * @param convValUp    the Converter to view the underlying
         *                     Entry Set's values through
         * @param convValDown  the Converter to use to pass values down to the
         *                     underlying Entry Set
         */
        public ConverterEntrySet(Collection<Map.Entry<FK, FV>> set, Converter<FK, TK> convKeyUp,
                Converter<TK, FK> convKeyDown, Converter<FV, TV> convValUp, Converter<TV, FV> convValDown)
            {
            assert(set != null && convKeyUp != null && convKeyDown != null && convValUp != null && convValDown != null);

            m_set         = set;
            m_convKeyUp   = convKeyUp;
            m_convKeyDown = convKeyDown;
            m_convValUp   = convValUp;
            m_convValDown = convValDown;
            }


        // ----- Set interface ----------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public int size()
            {
            return getEntrySet().size();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEmpty()
            {
            return getEntrySet().isEmpty();
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean contains(Object o)
            {
            return getEntrySet().contains(
                    new ConverterCollections.ConverterEntry<TK, FK, TV, FV>((Map.Entry<TK, TV>) o,
                            getConverterKeyDown(),
                            getConverterValueDown(),
                            getConverterValueUp()));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<Map.Entry<TK, TV>> iterator()
            {
            return wrapIterator(getEntrySet().iterator());
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public Object[] toArray()
            {
            Object[] ao = getEntrySet().toArray();
            int c = ao.length;
            Object[] aEntry = new Object[c];

            for (int i = 0; i < c; ++i)
                {
                aEntry[i] = wrapEntry((Entry<FK, FV>) ao[i]);
                }
            return aEntry;
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(T[] ao)
            {
            Object[] aoSrc  = getEntrySet().toArray();
            int      cSrc   = aoSrc.length;

            int cDest = ao.length;

            if (cSrc > cDest)
                {
                cDest  = cSrc;
                ao = (T[]) Array.newInstance(ao.getClass().getComponentType(), cDest);
                }

            if (cDest > cSrc)
                {
                ao[cSrc] = null;
                }

            for (int i = 0; i < cSrc; ++i)
                {
                ao[i] = (T) wrapEntry((Entry<FK, FV>) aoSrc[i]);
                }

            return ao;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean add(Map.Entry<TK, TV> o)
            {
            throw new UnsupportedOperationException();
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object o)
            {
            Map.Entry<TK, TV> entry = (Map.Entry<TK, TV>) o;
            return getEntrySet().remove(
                    new AbstractMap.SimpleEntry<FK, FV>(
                    getConverterKeyDown().convert(entry.getKey()),
                    getConverterValueDown().convert(entry.getValue())));
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean containsAll(Collection<?> col)
            {
            return getEntrySet().containsAll(
                    instantiateEntrySet((Collection<Entry<TK, TV>>) col, getConverterKeyDown(),
                            getConverterKeyUp(), getConverterValueDown(), getConverterValueUp()));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean addAll(Collection<? extends Map.Entry<TK, TV>> col)
            {
            throw new UnsupportedOperationException();
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean removeAll(Collection<?> col)
            {
            return getEntrySet().removeAll(
                    instantiateEntrySet((Collection<Entry<TK, TV>>) col,
                            getConverterKeyDown(), getConverterKeyUp(),
                            getConverterValueDown(), getConverterValueUp()));
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public boolean retainAll(Collection<?> col)
            {
            return getEntrySet().retainAll(
                    instantiateEntrySet((Collection<Entry<TK, TV>>) col,
                    getConverterKeyDown(),
                    getConverterKeyUp(),
                    getConverterValueDown(),
                    getConverterValueUp()));
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void clear()
            {
            getEntrySet().clear();
            }


        // ----- Object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object o)
            {
            if (o == this)
                {
                return true;
                }

            if (o instanceof ConverterEntrySet)
                {
                ConverterEntrySet that = (ConverterEntrySet) o;
                return this.getEntrySet()          .equals(that.getEntrySet()          )
                    && this.getConverterKeyUp()    .equals(that.getConverterKeyUp()    )
                    && this.getConverterKeyDown()  .equals(that.getConverterKeyDown()  )
                    && this.getConverterValueUp()  .equals(that.getConverterValueUp()  )
                    && this.getConverterValueDown().equals(that.getConverterValueDown());
                }

            if (o instanceof Set)
                {
                Set set = (Set) o;
                return set.size() == this.size() && this.containsAll(set);
                }
            return false;
            }

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public int hashCode()
            {
            int nHash = 0;
            for (Object o : this.getEntrySet())
                {
                nHash += NaturalHasher.INSTANCE.hashCode(o);
                }
            return nHash;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            StringBuffer sb = new StringBuffer();
            sb.append("ConverterEntrySet{");
            boolean fFirst = true;
            for (Object o : this)
                {
                if (fFirst)
                    {
                    fFirst = false;
                    }
                else
                    {
                    sb.append(", ");
                    }
                sb.append(o);
                }
            sb.append('}');
            return sb.toString();
            }


        // ----- factory methods --------------------------------------------

        /**
         * Create a Converter Entry Set.
         *
         * @param col          the underlying Collection of Map Entry objects
         * @param convKeyUp    the Converter to view the underlying Map's
         *                     Entry Set's keys through
         * @param convKeyDown  the Converter to use to pass keys down to the
         *                     underlying Map's Entry Set
         * @param convValUp    the Converter to view the underlying Map's
         *                     Entry Set's values through
         * @param convValDown  the Converter to use to pass values down to the
         *                     underlying Map's Entry Set
         *
         * @param <FK> the type of the keys in the underlying EntrySet
         * @param <TK> the type that the keys should be converted to
         * @param <FV> the type of the values in the underlying EntrySet
         * @param <TV> the type that the values should be converted to
         * @return a Converter Entry Set
         */
        protected <FK, FV, TK, TV> Set<Map.Entry<TK, TV>> instantiateEntrySet(Collection<Entry<FK, FV>> col,
                Converter<FK, TK> convKeyUp, Converter<TK, FK> convKeyDown, Converter<FV, TV> convValUp,
                Converter<TV, FV> convValDown)
            {
            return ConverterCollections.getEntrySet(col, convKeyUp, convKeyDown, convValUp, convValDown);
            }


        // ----- internal helpers -------------------------------------------

        /**
         * Wrap an Entry from the Entry Set to make a Converter Entry.
         *
         * @param entry  a Map Entry to wrap
         *
         * @return a Map Entry that restricts its type
         */
        protected Map.Entry<TK, TV> wrapEntry(Map.Entry<FK, FV> entry)
            {
            return new ConverterEntry(entry);
            }

        /**
         * Wrap an Iterator from the Entry Set to make a Converter Iterator.
         *
         * @param iter  a Iterator to wrap
         *
         * @return a Converter Iterator
         */
        protected Iterator<Map.Entry<TK, TV>> wrapIterator(Iterator<Map.Entry<FK, FV>> iter)
            {
            return new ConverterIterator(iter);
            }


        // ----- inner class: ConverterEntry --------------------------------

        /**
         * A Map Entry that lazily converts the key and value.
         */
        protected class ConverterEntry
                extends AbstractConverterEntry<FK, TK, FV, TV>
            {
            // ----- constructors -------------------------------------------

            /**
             * Constructor.
             *
             * @param entry  the Entry to wrap
             */
            public ConverterEntry(Map.Entry<FK, FV> entry)
                {
                super(entry);
                }


            // ----- AbstractConverterEntry methods -------------------------

            /**
             * Return the Converter to view the underlying Entry's key
             * through.
             *
             * @return the Converter to view the underlying Entry's key
             *         through
             */
            protected Converter<FK, TK> getConverterKeyUp()
                {
                return ConverterEntrySet.this.getConverterKeyUp();
                }

            /**
             * Return the Converter to view the underlying Entry's value
             * through.
             *
             * @return the Converter to view the underlying Entry's value
             *         through
             */
            protected Converter<FV, TV> getConverterValueUp()
                {
                return ConverterEntrySet.this.getConverterValueUp();
                }

            /**
             * Return the Converter used to change value in the underlying
             * Entry.
             *
             * @return the Converter used to change value in the underlying
             *         Entry
             */
            protected Converter<TV, FV> getConverterValueDown()
                {
                return ConverterEntrySet.this.getConverterValueDown();
                }
            }


        // ----- inner class: ConverterIterator -----------------------------

        /**
         * A Map Entry Iterator that converts the key and value types.
         */
        protected class ConverterIterator
                implements Iterator<Map.Entry<TK, TV>>
            {
            // ----- constructors -------------------------------------------

            /**
             * Constructor.
             *
             * @param iter  the Iterator to wrap
             */
            public ConverterIterator(Iterator<Map.Entry<FK, FV>> iter)
                {
                m_iter = iter;
                }


            // ----- Iterator interface -------------------------------------

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean hasNext()
                {
                return getIterator().hasNext();
                }

            /**
             * {@inheritDoc}
             */
            @Override
            public Map.Entry<TK, TV> next()
                {
                return wrapEntry(getIterator().next());
                }

            /**
             * {@inheritDoc}
             */
            @Override
            public void remove()
                {
                getIterator().remove();
                }


            // ----- accessors ----------------------------------------------

            /**
            * Return the underlying Iterator.
            *
            * @return the underlying Iterator
            */
            public Iterator<Map.Entry<FK, FV>> getIterator()
                {
                return m_iter;
                }


            // ----- data members -------------------------------------------

            /**
             * The underlying Iterator.
             */
            protected final Iterator<Map.Entry<FK, FV>> m_iter;
            }


        // ----- accessors --------------------------------------------------

        /**
         * Return the underlying Entry Set.
         *
         * @return the underlying Entry Set
         */
        public Collection<Map.Entry<FK, FV>> getEntrySet()
            {
            return m_set;
            }

        /**
         * Return the Converter used to view the underlying Entry Set's keys
         * through.
         *
         * @return the Converter from the underlying Entry Set's keys
         */
        public Converter<FK, TK> getConverterKeyUp()
            {
            return m_convKeyUp;
            }

        /**
         * Return the Converter used to pass keys down to the underlying Entry
         * Set.
         *
         * @return the Converter to the underlying Entry Set's keys
         */
        public Converter<TK, FK> getConverterKeyDown()
            {
            return m_convKeyDown;
            }

        /**
         * Return the Converter used to view the underlying Entry Set's values
         * through.
         *
         * @return the Converter from the underlying Entry Set's values
         */
        public Converter<FV, TV> getConverterValueUp()
            {
            return m_convValUp;
            }

        /**
         * Return the Converter used to pass values down to the underlying
         * Entry Set.
         *
         * @return the Converter to the underlying Entry Set's values
         */
        public Converter<TV, FV> getConverterValueDown()
            {
            return m_convValDown;
            }

        // ----- data members ---------------------------------------------

        /**
         * The underlying Entry Set (or Collection of Map Entry objects).
         */
        protected final Collection<Map.Entry<FK, FV>> m_set;

        /**
         * The Converter used to view keys stored in the Entry Set.
         */
        protected final Converter<FK, TK> m_convKeyUp;

        /**
         * The Converter used to pass keys down to the Entry Set.
         */
        protected final Converter<TK, FK> m_convKeyDown;

        /**
         * The Converter used to view values stored in the Entry Set.
         */
        protected final Converter<FV, TV> m_convValUp;

        /**
         * The Converter used to pass values down to the Entry Set.
         */
        protected final Converter<TV, FV> m_convValDown;
        }


    // ----- inner class: ConverterEntry ------------------------------------

    /**
     * An abstract Map Entry that lazily converts the key and value.
     *
     * @param <FK> the type of the keys in the underlying Entry
     * @param <TK> the type that the keys should be converted to
     * @param <FV> the type of the values in the underlying Entry
     * @param <TV> the type that the values should be converted to
     */
    protected abstract static class AbstractConverterEntry<FK, TK, FV, TV>
            implements Map.Entry<TK, TV>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructor.
         *
         * @param entry  the Entry to wrap
         */
        protected AbstractConverterEntry(Map.Entry<FK, FV> entry)
            {
            m_entry = entry;
            }

        // ----- abstract methods -------------------------------------------

        /**
         * Return the Converter to view the underlying Entry's key through.
         *
         * @return the Converter to view the underlying Entry's key through
         */
        abstract protected Converter<FK, TK> getConverterKeyUp();

        /**
         * Return the Converter to view the underlying Entry's value through.
         *
         * @return the Converter to view the underlying Entry's value through
         */
        abstract protected Converter<FV, TV> getConverterValueUp();

        /**
         * Return the Converter used to change value in the underlying Entry.
         *
         * @return the Converter used to change value in the underlying Entry
         */
        abstract protected Converter<TV, FV> getConverterValueDown();

        // ----- Map Entry interface ----------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public TK getKey()
            {
            TK oKeyUp = m_oKeyUp;
            if (oKeyUp == null)
                {
                m_oKeyUp = oKeyUp =
                        getConverterKeyUp().convert(getEntry().getKey());
                }
            return oKeyUp;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public TV getValue()
            {
            TV oValueUp = m_oValueUp;
            if (oValueUp == null)
                {
                m_oValueUp = oValueUp =
                        getConverterValueUp().convert(getEntry().getValue());
                }
            return oValueUp;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public TV setValue(TV value)
            {
            m_oValueUp = null;
            return getConverterValueUp().convert(
                    getEntry().setValue(getConverterValueDown().convert(value)));
            }

        // ----- Object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public boolean equals(Object o)
            {
            if (o == this)
                {
                return true;
                }

            if (o instanceof Map.Entry)
                {
                Map.Entry that = (Map.Entry) o;
                return NaturalHasher.INSTANCE.equals(this.getKey()  , that.getKey()  )
                    && NaturalHasher.INSTANCE.equals(this.getValue(), that.getValue());
                }
            return false;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode()
            {
            Object oKey   = getKey();
            Object oValue = getValue();
            return (oKey   == null ? 0 : oKey  .hashCode()) ^
                   (oValue == null ? 0 : oValue.hashCode());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return "ConverterEntry{Key=\"" + getKey() + "\", Value=\""
                    + getValue() + "\"}";
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the underlying Map.Entry.
         *
         * @return the underlying Map.Entry
         */
        public Map.Entry<FK, FV> getEntry()
            {
            return m_entry;
            }

        // ----- data members -----------------------------------------------

        /**
         * The underlying entry.
         */
        protected final Map.Entry<FK, FV> m_entry;

        /**
         * Cached converted key.
         */
        protected transient TK m_oKeyUp;

        /**
         * Cached converted value.
         */
        protected transient TV m_oValueUp;
        }

    /**
     * A Map Entry that lazily converts the key and value.
     *
     * @param <FK> the type of the keys in the underlying Entry
     * @param <TK> the type that the keys should be converted to
     * @param <FV> the type of the values in the underlying Entry
     * @param <TV> the type that the values should be converted to
     */
    public static class ConverterEntry<FK, TK, FV, TV>
            extends AbstractConverterEntry<FK, TK, FV, TV>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructor.
         *
         * @param entry        the Entry to wrap
         * @param convKeyUp    the Converter to view the underlying Entry's
         *                     keys through
         * @param convValUp    the Converter to view the underlying Entry's
         *                     values through
         * @param convValDown  the Converter to use to pass values down to the
         *                     underlying Entry
         */
        public ConverterEntry(Map.Entry<FK, FV> entry, Converter<FK, TK> convKeyUp, Converter<FV, TV> convValUp,
                Converter<TV, FV> convValDown)
            {
            super(entry);
            m_convKeyUp   = convKeyUp;
            m_convValUp   = convValUp;
            m_convValDown = convValDown;
            }


        // ----- AbstractConverterEntry methods -----------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        protected Converter<FK, TK> getConverterKeyUp()
            {
            return m_convKeyUp;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Converter<FV, TV> getConverterValueUp()
            {
            return m_convValUp;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Converter<TV, FV> getConverterValueDown()
            {
            return m_convValDown;
            }


        // ----- data members -----------------------------------------------

        /**
         * The Converter used to view the Entry's key.
         */
        protected final Converter<FK, TK> m_convKeyUp;

        /**
         * The Converter used to view the Entry's value.
         */
        protected final Converter<FV, TV> m_convValUp;

        /**
         * The Converter used to store the Entry's value.
         */
        protected final Converter<TV, FV> m_convValDown;
        }


    // ----- inner class: ConverterCacheEntry -------------------------------

    /**
     * A ConfigurableCacheMap.Entry that lazily converts the key and value.
     */
    public static class ConverterCacheEntry
            extends ConverterEntry
            implements ConfigurableCacheMap.Entry
        {
        public ConverterCacheEntry(ConfigurableCacheMap.Entry entry,
                                   Converter conKeyUp, Converter conValUp,
                                   Converter conValDown)
            {
            super(entry, conKeyUp, conValUp, conValDown);
            }

        // ----- ConverterEntry methods -------------------------------------

        /**
         * {@inheritDoc}
         */
        public ConfigurableCacheMap.Entry getEntry()
            {
            return (ConfigurableCacheMap.Entry) super.getEntry();
            }

        // ----- ConfigurableCacheMap.Entry methods -------------------------

        /**
         * {@inheritDoc}
         */
        public void touch()
            {
            getEntry().touch();
            }

        /**
         * {@inheritDoc}
         */
        public int getTouchCount()
            {
            return getEntry().getTouchCount();
            }

        /**
         * {@inheritDoc}
         */
        public long getLastTouchMillis()
            {
            return getEntry().getLastTouchMillis();
            }

        /**
         * {@inheritDoc}
         */
        public long getExpiryMillis()
            {
            return getEntry().getExpiryMillis();
            }

        /**
         * {@inheritDoc}
         */
        public void setExpiryMillis(long lMillis)
            {
            getEntry().setExpiryMillis(lMillis);
            }

        /**
         * {@inheritDoc}
         */
        public int getUnits()
            {
            return getEntry().getUnits();
            }

        /**
         * {@inheritDoc}
         */
        public void setUnits(int cUnits)
            {
            getEntry().setUnits(cUnits);
            }
        }


    // ----- inner class: ConverterMapEvent ---------------------------------

    /**
    * A ConverterMapEvent views an underlying MapEvent through a set of key and
    * value Converters.  This event may cache converted keys and/or values to
    * optimize out future conversions.
    */
    public static class ConverterMapEvent<K, V>
            extends CacheEvent<K, V>
        {
        // ----- constructors -----------------------------------------------

        /**
        * Construct a ConverterMapEvent.
        *
        * @param event    the underlying MapEvent
        * @param map      the new event's source
        * @param convKey  the Converter to view the underlying MapEvent's key
        * @param convVal  the Converter to view the underlying MapEvent's values
        */
        public ConverterMapEvent(ObservableMap<K, V> map, MapEvent<K, V> event, Converter<K, K> convKey, Converter<V, V> convVal)
            {
            this(map, event, convKey, convVal, null);
            }

        /**
        * Construct a ConverterMapEvent.
        *
        * @param event    the underlying MapEvent
        * @param map      the new event's source
        * @param convKey  the Converter to view the underlying MapEvent's key
        * @param convVal  the Converter to view the underlying MapEvent's values
        * @param context  the BackingMapManagerContext necessary to emulate
        *                 the BinaryEntry interface
        */
        @SuppressWarnings("unchecked")
        public ConverterMapEvent(ObservableMap<K, V> map, MapEvent<K, V> event, Converter<K, K> convKey, Converter<V, V> convVal, BackingMapManagerContext context)
            {
            super(map, event.getId(), (K) NO_VALUE, (V) NO_VALUE, (V) NO_VALUE,
                  event instanceof CacheEvent && ((CacheEvent<K, V>) event).isSynthetic(),
                  event instanceof CacheEvent ? ((CacheEvent<K, V>) event).getTransformationState() : TransformationState.TRANSFORMABLE,
                  event instanceof CacheEvent && ((CacheEvent<K, V>) event).isPriming(),
                  event instanceof CacheEvent && ((CacheEvent<K, V>) event).isExpired());

            m_event   = event;
            m_convKey = convKey;
            m_convVal = convVal;
            m_context = context;
            }


        // ----- MapEvent methods -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public K getKey()
            {
            K key = m_key;
            if (key == NO_VALUE)
                {
                setKey(key =
                    getConverterKeyUp().convert(getMapEvent().getKey()));
                }
            return key;
            }

        /**
        * {@inheritDoc}
        */
        public V getOldValue()
            {
            V valueOld = m_valueOld;
            if (valueOld == NO_VALUE)
                {
                setOldValue(valueOld =
                    getConverterValueUp().convert(getMapEvent().getOldValue()));
                }
            return valueOld;
            }

        /**
        * {@inheritDoc}
        */
        public V getNewValue()
            {
            V valueNew = m_valueNew;
            if (valueNew == NO_VALUE)
                {
                setNewValue(valueNew =
                    getConverterValueUp().convert(getMapEvent().getNewValue()));
                }
            return valueNew;
            }

        /**
        * {@inheritDoc}
        */
        public Map.Entry<K, V> getOldEntry()
            {
            Map.Entry<K, V> entry = m_entryOld;
            if (entry == null)
                {
                entry = m_entryOld = getContext() == null
                    ? new ConverterMapEventEntry(false)
                    : new ConverterMapEventBinaryEntry(false);
                }
            return entry;
            }

        /**
        * {@inheritDoc}
        */
        public Map.Entry<K, V> getNewEntry()
            {
            Map.Entry<K, V> entry = m_entryNew;
            if (entry == null)
                {
                entry = m_entryNew = getContext() == null
                    ? new ConverterMapEventEntry(true)
                    : new ConverterMapEventBinaryEntry(true);
                }
            return entry;
            }

        @Override
        public int getPartition()
            {
            return m_event.getPartition();
            }

        @Override
        public long getVersion()
            {
            return m_event.getVersion();
            }

        @Override
        public boolean isVersionUpdate()
            {
            return m_event instanceof CacheEvent &&
                    ((CacheEvent) m_event).isVersionUpdate();
            }

        // ----- accessors --------------------------------------------------

        /**
        * Return the underlying MapEvent.
        *
        * @return the underlying MapEvent
        */
        public MapEvent<K, V> getMapEvent()
            {
            return m_event;
            }

        /**
        * Get the BackingMapManagerContext if one was provided.
        *
        * @return  the BackingMapManagerContext
        */
        public BackingMapManagerContext getContext()
            {
            return m_context;
            }

        /**
        * Return the Converter used to view the underlying MapEvent's key
        * through.
        *
        * @return the Converter from the underlying MapEvent's key
        */
        public Converter<K, K> getConverterKeyUp()
            {
            return m_convKey;
            }

        /**
        * Return the Converter used to view the underlying MapEvent's value
        * through.
        *
        * @return the Converter from the underlying MapEvent's value
        */
        public Converter<V, V> getConverterValueUp()
            {
            return m_convVal;
            }

        /**
        * Set the cached converted old value associated with this event.
        *
        * @param key  the converted key value
        */
        public void setKey(K key)
            {
            m_key = key;
            }

        /**
        * Set the cached converted old value associated with this event.
        *
        * @param value  the new converted "old" value
        */
        public void setOldValue(V value)
            {
            m_valueOld = value;
            }

        /**
        * Set the cached converted new value associated with this event.
        *
        * @param value  the new converted "new" value
        */
        public void setNewValue(V value)
            {
            m_valueNew = value;
            }

        /**
        * Check if the event's key has been converted.
        *
        * @return true iff the key has been converted
        */
        public boolean isKeyConverted()
            {
            return m_key != NO_VALUE;
            }

        /**
        * Check if the event's old value has been converted.
        *
        * @return true iff the old value has been converted
        */
        public boolean isOldValueConverted()
            {
            return m_valueOld != NO_VALUE;
            }

        /**
        * Check if the event's new value has been converted.
        *
        * @return true iff the new value has been converted
        */
        public boolean isNewValueConverted()
            {
            return m_valueNew != NO_VALUE;
            }

        /**
        * Remove any cached conversions of the key or values.
        */
        @SuppressWarnings("unchecked")
        public void clearConverted()
            {
            setKey     ((K) NO_VALUE);
            setOldValue((V) NO_VALUE);
            setNewValue((V) NO_VALUE);

            m_entryOld = null;
            m_entryNew = null;
            }


        // ----- inner class: ConverterMapEventEntry ------------------------

        /**
        * ConverterMapEventEntry provides the Map Entry interface to the
        * information encapsulated inside the ConverterMapEvent.
        */
        protected class ConverterMapEventEntry
                implements Map.Entry<K, V>
            {
            // ----- Constructors -------------------------------------------

            /**
            * Constructor.
            *
            * @param fNewValue  specifies whether the value represented by
            *                   this entry is the new or the old value
            */
            public ConverterMapEventEntry(boolean fNewValue)
                {
                m_fNewValue = fNewValue;
                }

            // ----- ConverterMapEventEntry methods -------------------------

            /**
             * Return a raw binary key for this entry.
             *
             * @return a raw binary key for this entry
             */
            public Binary getBinaryKey()
                {
                return (Binary) m_event.getKey();
                }

            /**
             * Return a raw binary value for this entry.
             *
             * @return a raw binary value for this entry; null if the value does not
             *         exist
             */
            public Binary getBinaryValue()
                {
                return m_fNewValue
                        ? (Binary) m_event.getNewValue()
                        : (Binary) m_event.getOldValue();
                }

            // ----- Map.Entry interface ------------------------------------

            /**
            * {@inheritDoc}
            */
            public K getKey()
                {
                return ConverterMapEvent.this.getKey();
                }

            /**
            * {@inheritDoc}
            */
            public V getValue()
                {
                return m_fNewValue
                        ? ConverterMapEvent.this.getNewValue()
                        : ConverterMapEvent.this.getOldValue();
                }

            /**
            * {@inheritDoc}
            */
            public V setValue(V value)
                {
                throw new UnsupportedOperationException();
                }

            // ----- data members -------------------------------------------

            /**
            * Determines whether this entry represents the old or new value.
            */
            protected boolean m_fNewValue;
            }


        // ----- inner class: ConverterMapEventBinaryEntry ------------------

        /**
        * ConverterMapEventBinaryEntry provides both the Map Entry and the
        * BinaryEntry interfaces to the information encapsulated inside the
        * ConverterMapEvent.
        */
        protected class ConverterMapEventBinaryEntry
                extends ConverterMapEventEntry
                implements BinaryEntry<K, V>
            {
            // ----- constructors -------------------------------------------

            /**
            * Constructor.
            *
            * @param fNewValue  specifies whether the value represented by
            *                   this entry is the new or the old value
            */
            public ConverterMapEventBinaryEntry(boolean fNewValue)
                {
                super(fNewValue);
                }

            // ----- InvocableMap.Entry interface ---------------------------

            /**
            * {@inheritDoc}
            */
            public boolean isPresent()
                {
                return getBinaryValue() != null;
                }

            /**
            * {@inheritDoc}
            */
            public boolean isSynthetic()
                {
                MapEvent<K, V> event = getMapEvent();

                return event instanceof CacheEvent &&
                        ((CacheEvent<K, V>) event).isSynthetic();
                }

            /**
            * {@inheritDoc}
            */
            public void remove(boolean fSynthetic)
                {
                throw new UnsupportedOperationException();
                }

            /**
            * {@inheritDoc}
            */
            public void setValue(V value, boolean fSynthetic)
                {
                throw new UnsupportedOperationException();
                }

            /**
            * {@inheritDoc}
            */
            public <T> void update(ValueUpdater<V, T> updater, T value)
                {
                throw new UnsupportedOperationException();
                }

            /**
            * Depending upon the type of the ValueExtractor route the call
            * to the appropriate extract method.
            *
            * @param extractor  the ValueExtractor to pass this Entry or value.
            *
            * @return the extracted value.
            */
            @SuppressWarnings({"unchecked", "rawtypes"})
            public <T, E> E extract(ValueExtractor<T, E> extractor)
                {
                Map    mapExtracted = m_mapExtracted;
                Object oValue;

                if (mapExtracted == null)
                    {
                    mapExtracted = m_mapExtracted = new LiteMap();
                    oValue       = null;
                    }
                else
                    {
                    oValue = mapExtracted.get(extractor);
                    }

                if (oValue == null)
                    {
                    oValue = InvocableMapHelper.extractFromEntry(extractor, this);
                    mapExtracted.put(extractor, oValue == null ? NO_VALUE : oValue);
                    }
                else if (oValue == NO_VALUE)
                    {
                    oValue = null;
                    }
                return (E) oValue;
                }

            // ----- BinaryEntry interface  ---------------------------------

            /**
            * {@inheritDoc}
            */
            public BackingMapManagerContext getContext()
                {
                return ConverterMapEvent.this.getContext();
                }

            /**
            * {@inheritDoc}
            */
            public Serializer getSerializer()
                {
                return getContext().getCacheService().getSerializer();
                }

            /**
            * {@inheritDoc}
            */
            public void updateBinaryValue(Binary binValue)
                {
                throw new UnsupportedOperationException();
                }

            /**
            * {@inheritDoc}
            */
            public void updateBinaryValue(Binary binValue, boolean fSynthetic)
                {
                throw new UnsupportedOperationException();
                }

            /**
            * {@inheritDoc}
            */
            public V getOriginalValue()
                {
                throw new UnsupportedOperationException();
                }

            /**
            * {@inheritDoc}
            */
            public Binary getOriginalBinaryValue()
                {
                throw new UnsupportedOperationException();
                }

            /**
            * {@inheritDoc}
            */
            public ObservableMap<K, V> getBackingMap()
                {
                return m_event.getMap();
                }

            /**
            * {@inheritDoc}
            */
            public BackingMapContext getBackingMapContext()
                {
                return null;
                }

            /**
            * {@inheritDoc}
            */
            public void expire(long cMillis)
                {
                throw new UnsupportedOperationException();
                }

            /**
            * {@inheritDoc}
            */
            public long getExpiry()
                {
                throw new UnsupportedOperationException();
                }

            /**
            * {@inheritDoc}
            */
            public boolean isReadOnly()
                {
                return true;
                }

            // ----- data fields -------------------------------------------

            /**
            * Cached extraction results.
            */
            private Map<ValueExtractor, Object> m_mapExtracted;
            }

        // ----- data members -----------------------------------------------

        /**
        * Tag object indicating that a corresponding value has not been
        * converted.
        */
        public static final Object NO_VALUE = new Object();

        /**
        * The underlying MapEvent.
        */
        protected MapEvent<K, V>  m_event;

        /**
        * The Converter to view the underlying MapEvent's key.
        */
        protected Converter<K, K> m_convKey;

        /**
        * The Converter to view the underlying MapEvent's value.
        */
        protected Converter<V, V> m_convVal;

        /**
        * The BackingMapManagerContext to use for extracting binary values.
        */
        protected BackingMapManagerContext m_context;

        /**
        * Cached old entry.
        */
        protected Map.Entry<K, V> m_entryOld;

        /**
        * Cached new entry.
        */
        protected Map.Entry<K, V> m_entryNew;
        }


    // ----- inner class: ConverterCacheEvent -------------------------------

    /**
    * A Converter CacheEvent views an underlying CacheEvent through a set of
    * key and value Converters.
    */
    public static class ConverterCacheEvent<K, V>
            extends ConverterMapEvent<K, V>
        {
        // ----- constructors -----------------------------------------------

        /**
        * Constructor.
        *
        * @param event    the underlying CacheEvent
        * @param map      the new event's source
        * @param convKey  the Converter to view the underlying CacheEvent's
        *                 key
        * @param convVal  the Converter to view the underlying CacheEvent's
        *                 values
        */
        public ConverterCacheEvent(ObservableMap<K, V> map, CacheEvent<K, V> event, Converter<K, K> convKey, Converter<V, V> convVal)
            {
            this(map, event, convKey, convVal, null);
            }

        /**
        * Construct a ConverterMapEvent.
        *
        * @param event    the underlying MapEvent
        * @param map      the new event's source
        * @param convKey  the Converter to view the underlying CacheEvent's
        *                 key
        * @param convVal  the Converter to view the underlying CacheEvent's
        *                 values
        * @param context  the BackingMapManagerContext necessary to emulate
        *                 the BinaryEntry interface
        */
        public ConverterCacheEvent(ObservableMap<K, V> map, CacheEvent<K, V> event,
                        Converter<K, K> convKey, Converter<V, V> convVal, BackingMapManagerContext context)
            {
            super(map, event, convKey, convVal, context);
            }


        // ----- accessors --------------------------------------------------

        /**
        * Return the underlying CacheEvent.
        *
        * @return the underlying CacheEvent
        */
        public CacheEvent<K, V> getCacheEvent()
            {
            return (CacheEvent<K, V>) getMapEvent();
            }
        }


    // ----- inner class: ConverterMapListener ------------------------------

    /**
    * A converter MapListener that converts events of the underlying
    * MapListener for the underlying map.
    */
    public static class ConverterMapListener<K, V>
            extends WrapperListener<K, V>
            implements MapListener<K, V>
        {
        // ----- constructors -----------------------------------------------

        /**
        * Constructor.
        *
        * @param map       the Map that should be the source for converted events
        * @param listener  the underlying MapListener
        * @param convKey   the Converter to view the underlying MapEvent's key
        * @param convVal   the Converter to view the underlying MapEvent's values
        */
        public ConverterMapListener(ObservableMap<K, V> map, MapListener<K, V> listener, Converter<K, K> convKey, Converter<V, V> convVal)
            {
            super(listener);

            azzert(convKey != null && convVal != null, "Null converter");

            m_map     = map;
            m_convKey = convKey;
            m_convVal = convVal;
            }

        @Override
        protected void onMapEvent(MapEvent<K, V> evt)
            {
            super.onMapEvent(
                ConverterCollections.getMapEvent(getObservableMap(), evt,
                    getConverterKeyUp(), getConverterValueUp()));
            }


        // ----- Object methods ---------------------------------------------

        /**
        * Compare the ConverterMapListener with another object to
        * determine equality.
        *
        * @return true iff this ConverterMapListener and the passed
        *          object are equivalent listeners
        */
        @SuppressWarnings("unchecked")
        public boolean equals(Object o)
            {
            if (o == this)
                {
                return true;
                }

            if (super.equals(o))
                {
                ConverterMapListener<K, V> that = (ConverterMapListener<K, V>) o;
                return this.getConverterKeyUp()  .equals(that.getConverterKeyUp())
                    && this.getConverterValueUp().equals(that.getConverterValueUp());
                }
            return false;
            }


        // ----- accessors --------------------------------------------------

        /**
        * Return the underlying ObservableMap.
        *
        * @return the underlying ObservableMap
        */
        public ObservableMap<K, V> getObservableMap()
            {
            return m_map;
            }

        /**
        * Return the Converter used to view an underlying CacheEvent's key
        * through.
        *
        * @return the Converter from an underlying CacheEvent's key
        */
        public Converter<K, K> getConverterKeyUp()
            {
            return m_convKey;
            }

        /**
        * Return the Converter used to view an underlying CacheEvent's value
        * through.
        *
        * @return the Converter from an underlying CacheEvent's value
        */
        public Converter<V, V> getConverterValueUp()
            {
            return m_convVal;
            }


        // ----- data members -----------------------------------------------

        /**
        * The converting Map the will be the source of converted events.
        */
        protected ObservableMap<K, V> m_map;

        /**
        * The Converter to view an underlying CacheEvent's key.
        */
        protected Converter<K, K> m_convKey;

        /**
        * The Converter to view an underlying CacheEvent's value.
        */
        protected Converter<V, V> m_convVal;
        }


    // ----- inner class: ConverterCacheListener ----------------------------

    /**
    * A converter MapListener that converts events of the underlying
    * MapListener for the underlying NamedCache.
    */
    public static class ConverterCacheListener<K, V>
            extends ConverterMapListener<K, V>
            implements MapListener<K, V>
        {
        /**
         * Constructor.
         *
         * @param cache    the NamedCache that should be the source for converted events
         * @param listener the underlying MapListener
         * @param convKey  the Converter to view the underlying MapEvent's key
         * @param convVal  the Converter to view the underlying MapEvent's
         *                 values
         */
        public ConverterCacheListener(NamedCache<K, V> cache, MapListener<K, V> listener,
                Converter<K, K> convKey, Converter<V, V> convVal)
            {
            super(cache, listener, convKey, convVal);
            }

        @Override
        protected void onMapEvent(MapEvent<K, V> evt)
            {
            // skip events for an inactive NamedCache
            if (((NamedCache<K, V>) getObservableMap()).isActive())
                {
                super.onMapEvent(evt);
                }
            }
        }


    // ----- inner class: ConverterComparator -------------------------------

    /**
     * A Comparator that Converts the elements before
     * comparing them.
     *
     * @param <F> the type of the elements the underlying Comparator compares
     * @param <T> the type that the elements should be converted to
     */
    public static class ConverterComparator<F, T>
            implements Comparator<T>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructor.
         *
         * @param comparator the comparator to wrap
         * @param conv       the converter to use
         */
        public ConverterComparator(Comparator<? super F> comparator, Converter<T, F> conv)
            {
            m_comparator = comparator;
            m_conv       = conv;
            }


        // ----- Comparator methods -----------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(T o1, T o2)
            {
            Converter<T, F> conv = m_conv;
            return m_comparator.compare(conv.convert(o1), conv.convert(o2));
            }


        // ----- data members -----------------------------------------------

        /**
         * The Converter used to convert an element before comparing it.
         */
        private final Converter<T, F> m_conv;

        /**
         * The Comparator to use.
         */
        private final Comparator<? super F> m_comparator;
        }

    /**
     * A Holder that converts the element before returning them.
     *
     * @param <F> the type of the element the Holder holds
     * @param <T> the type that the element should be converted to
     */
    public static class ConverterHolder<F, T>
            implements Holder<T>, Serializable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructor.
         *
         * @param value   the value to wrap
         * @param convUp  the converter to use
         */
        public ConverterHolder(F value, Converter<F, T> convUp)
            {
            m_value  = value;
            m_convUp = convUp;
            }

        @Override
        public T get()
            {
            T oValueUp = m_oValueUp;
            if (oValueUp == null)
                {
                oValueUp = m_oValueUp = m_convUp.convert(m_value);
                }
            return oValueUp;
            }

        @Override
        public void set(T value)
            {
            throw new UnsupportedOperationException();
            }

        // ----- Object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public boolean equals(Object o)
            {
            if (o == this)
                {
                return true;
                }

            if (o instanceof ConverterHolder)
                {
                ConverterHolder that = (ConverterHolder) o;
                return NaturalHasher.INSTANCE.equals(this.get(), that.get());
                }

            return false;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode()
            {
            Object oKey = get();
            return oKey == null ? 0 : oKey.hashCode();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return "ConverterHolder{Value=\"" + get() + "\", Converter=\""
                    + m_convUp + "\"}";
            }

        // ----- data members -----------------------------------------------

        /**
         * The underlying value.
         */
        private final F m_value;

        /**
         * The Converter used to convert the Holder value.
         */
        private final Converter<F, T> m_convUp;

        /**
         * Cached converted value.
         */
        private transient T m_oValueUp;
        }

    // ----- inner class: ConverterLongArray --------------------------------

    /**
     * ConverterLongArray converts the value of the LongArray from its raw form
     * (type {@code F}) to the desired from (type {@code T}).
     *
     * @param <F>  the type of the provided LongArray
     * @param <T>  the type of the desired type
     */
    public static class ConverterLongArray<F, T>
            implements LongArray<T>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a LongArray that converts the values from type {@code F}
         * to type {@code T}.
         *
         * @param laDelegate  the LongArray that stores values of type {@code F}
         * @param convUp      a {@link Converter} that converts up
         * @param convDown    a {@link Converter} that converts down
         */
        public ConverterLongArray(LongArray<F> laDelegate, Converter<F, T> convUp, Converter<T, F> convDown)
            {
            f_laDelegate = laDelegate;
            f_convUp     = convUp;
            f_convDown   = convDown;
            }

        // ----- LongArray methods ------------------------------------------

        @Override
        public T get(long lIndex)
            {
            return f_convUp.convert(f_laDelegate.get(lIndex));
            }

        @Override
        public long floorIndex(long lIndex)
            {
            return f_laDelegate.floorIndex(lIndex);
            }

        @Override
        public T floor(long lIndex)
            {
            return f_convUp.convert(f_laDelegate.floor(lIndex));
            }

        @Override
        public long ceilingIndex(long lIndex)
            {
            return f_laDelegate.ceilingIndex(lIndex);
            }

        @Override
        public T ceiling(long lIndex)
            {
            return f_convUp.convert(f_laDelegate.ceiling(lIndex));
            }

        @Override
        public T set(long lIndex, T oValue)
            {
            return f_convUp.convert(f_laDelegate.set(lIndex, f_convDown.convert(oValue)));
            }

        @Override
        public long add(T oValue)
            {
            return f_laDelegate.add(f_convDown.convert(oValue));
            }

        @Override
        public boolean exists(long lIndex)
            {
            return f_laDelegate.exists(lIndex);
            }

        @Override
        public T remove(long lIndex)
            {
            return f_convUp.convert(f_laDelegate.remove(lIndex));
            }

        @Override
        public void remove(long lIndexFrom, long lIndexTo)
            {
            f_laDelegate.remove(lIndexFrom, lIndexTo);
            }

        @Override
        public boolean contains(T oValue)
            {
            return f_laDelegate.contains(f_convDown.convert(oValue));
            }

        @Override
        public void clear()
            {
            f_laDelegate.clear();
            }

        @Override
        public boolean isEmpty()
            {
            return f_laDelegate.isEmpty();
            }

        @Override
        public int getSize()
            {
            return f_laDelegate.getSize();
            }

        @Override
        public Iterator<T> iterator()
            {
            return instantiateIterator(f_laDelegate.iterator());
            }

        @Override
        public Iterator<T> iterator(long lIndex)
            {
            return instantiateIterator(f_laDelegate.iterator(lIndex));
            }

        @Override
        public Iterator<T> reverseIterator()
            {
            return instantiateIterator(f_laDelegate.reverseIterator());
            }

        @Override
        public Iterator<T> reverseIterator(long lIndex)
            {
            return instantiateIterator(f_laDelegate.reverseIterator(lIndex));
            }

        @Override
        public long getFirstIndex()
            {
            return f_laDelegate.getFirstIndex();
            }

        @Override
        public long getLastIndex()
            {
            return f_laDelegate.getLastIndex();
            }

        @Override
        public long indexOf(T oValue)
            {
            return f_laDelegate.indexOf(f_convDown.convert(oValue));
            }

        @Override
        public long indexOf(T oValue, long lIndex)
            {
            return f_laDelegate.indexOf(f_convDown.convert(oValue), lIndex);
            }

        @Override
        public long lastIndexOf(T oValue)
            {
            return f_laDelegate.lastIndexOf(f_convDown.convert(oValue));
            }

        @Override
        public long lastIndexOf(T oValue, long lIndex)
            {
            return f_laDelegate.lastIndexOf(f_convDown.convert(oValue), lIndex);
            }

        @Override
        public LongArray<T> clone()
            {
            return new ConverterLongArray<F,T>(f_laDelegate.clone(), f_convUp, f_convDown);
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Instantiate a new Iterator wrapping the provided iterator.
         *
         * @param iter  the Iterator to wrap
         *
         * @return an iterator converting the raw type to the desired type
         */
        protected Iterator<T> instantiateIterator(Iterator<F> iter)
            {
            return new ConverterLongArrayIterator(iter);
            }

        // ----- inner class: ConverterLongArrayIterator --------------------

        /**
         * An Iterator that can convert from raw types to desired types.
         */
        protected class ConverterLongArrayIterator
                extends ConverterEnumerator<F, T>
                implements Iterator<T>
            {
            // ----- constructors -------------------------------------------

            /**
             * Construct a converting iterator.
             *
             * @param iter  the underlying iterator
             */
            public ConverterLongArrayIterator(Iterator<F> iter)
                {
                super(iter, f_convUp);
                }

            // ----- LongArray.Iterator methods -----------------------------

            @Override
            public long getIndex()
                {
                return ((Iterator<F>) m_iter).getIndex();
                }

            @Override
            public T getValue()
                {
                return m_conv.convert(((Iterator<F>) m_iter).getValue());
                }

            @Override
            public T setValue(T oValue)
                {
                return m_conv.convert(((Iterator<F>) m_iter).setValue(
                        f_convDown.convert(oValue)));
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The LongArray to delegate to.
         */
        protected final LongArray<F> f_laDelegate;

        /**
         * The Converter to use to convert from type F to type T.
         */
        protected final Converter<F, T> f_convUp;

        /**
         * The Converter to use to convert from type T to type F.
         */
        protected final Converter<T, F> f_convDown;
        }
    }
