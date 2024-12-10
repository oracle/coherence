/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.SafeComparator;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.concurrent.ConcurrentSkipListSet;

import java.util.function.Predicate;

import java.util.stream.Collectors;

/**
 * A sorted view over {@link NamedCache} entries.
 *
 * @param <K>  the type of keys
 * @param <V>  the type of values
 *
 * @author Aleks Seovic  2023.09.20
 *
 * @since 23.09
 */
public class OrderedView<K, V>
        implements NamedCache<K, V>, MapListener<K, V>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code OrderedView} instance.
     *
     * @param source      the source {@link NamedCache} to create view for
     * @param comparator  the {@link Comparator} to use to sort entries
     */
    public OrderedView(NamedCache<K, V> source, Comparator<? super V> comparator)
        {
        f_source     = source;
        f_comparator = SafeComparator.ensureSafe(comparator);
        f_sortedView = new ConcurrentSkipListSet<>(new EntryComparator());
        source.addMapListener(this);

        f_sortedView.addAll(source.entrySet());
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Return the source cache this view is based on.
     *
     * @return the source cache this view is based on
     */
    public NamedCache<K, V> getSource()
        {
        return f_source;
        }

    /**
     * Return the {@link Comparator} this view uses to sort entries.
     *
     * @return the {@link Comparator} this view uses to sort entries
     */
    public Comparator<? super V> getComparator()
        {
        return f_comparator;
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Create a {@link Predicate} from a specified {@link Filter}.
     *
     * @param filter  a {@code Filter} to create a {@code Predicate} from
     *
     * @return the {@code Predicate} for a specified {@code Filter}
     */
    private Predicate<? super Map.Entry<K, V>> toPredicate(Filter<?> filter)
        {
        return entry -> InvocableMapHelper.evaluateEntry(filter, entry);
        }

    // ---- NamedCache interface --------------------------------------------

    @Override
    public String getCacheName()
        {
        return f_source.getCacheName();
        }

    @Override
    public CacheService getCacheService()
        {
        return f_source.getCacheService();
        }

    @Override
    public V put(K key, V value, long ttl)
        {
        return f_source.put(key, value, ttl);
        }

    // ---- NamedMap interface ----------------------------------------------

    @Override
    public Map<K, V> getAll(Collection<? extends K> colKeys)
        {
        return f_source.getAll(colKeys);
        }

    @Override
    public boolean isActive()
        {
        return f_source.isActive();
        }

    @Override
    public void destroy()
        {
        f_source.destroy();
        }

    @Override
    public void release()
        {
        f_source.release();
        }

    @Override
    public boolean lock(Object oKey, long cMillis)
        {
        return f_source.lock(oKey, cMillis);
        }

    @Override
    public boolean lock(Object oKey)
        {
        return f_source.lock(oKey);
        }

    @Override
    public boolean unlock(Object oKey)
        {
        return f_source.unlock(oKey);
        }

    @Override
    public <R> R invoke(K key, EntryProcessor<K, V, R> entryProcessor)
        {
        return f_source.invoke(key, entryProcessor);
        }

    @Override
    public <R> Map<K, R> invokeAll(Collection<? extends K> colKeys, EntryProcessor<K, V, R> entryProcessor)
        {
        return f_source.invokeAll(colKeys, entryProcessor);
        }

    @Override
    public <R> Map<K, R> invokeAll(Filter filter, EntryProcessor<K, V, R> entryProcessor)
        {
        return f_source.invokeAll(filter, entryProcessor);
        }

    @Override
    public <R> R aggregate(Collection<? extends K> colKeys, EntryAggregator<? super K, ? super V, R> entryAggregator)
        {
        return f_source.aggregate(colKeys, entryAggregator);
        }

    @Override
    public <R> R aggregate(Filter filter, EntryAggregator<? super K, ? super V, R> entryAggregator)
        {
        return f_source.aggregate(filter, entryAggregator);
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> mapListener)
        {
        f_source.addMapListener(mapListener);
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> mapListener)
        {
        f_source.removeMapListener(mapListener);
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> mapListener, K key, boolean fLite)
        {
        f_source.addMapListener(mapListener, key, fLite);
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> mapListener, K key)
        {
        f_source.removeMapListener(mapListener, key);
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> mapListener, Filter filter, boolean fLite)
        {
        f_source.addMapListener(mapListener, filter, fLite);
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> mapListener, Filter filter)
        {
        f_source.removeMapListener(mapListener, filter);
        }

    @Override
    public Set<K> keySet(Filter filter)
        {
        return f_sortedView.stream()
                .filter(toPredicate(filter))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

    @Override
    public Set<Map.Entry<K, V>> entrySet(Filter filter)
        {
        return f_sortedView.stream()
                .filter(toPredicate(filter))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

    @Override
    public Set<Map.Entry<K, V>> entrySet(Filter filter, Comparator comparator)
        {
        TreeSet<Map.Entry<K, V>> set = new TreeSet<>(comparator);
        set.addAll(entrySet(filter));
        return set;
        }

    @Override
    public <T, E> void addIndex(ValueExtractor<? super T, ? extends E> extractor, boolean fOrdered, Comparator<? super E> comparator)
        {
        f_source.addIndex(extractor, fOrdered, comparator);
        }

    @Override
    public <T, E> void removeIndex(ValueExtractor<? super T, ? extends E> extractor)
        {
        f_source.removeIndex(extractor);
        }

    // ---- Map interface -----------------------------------------------

    @Override
    public int size()
        {
        return f_source.size();
        }

    @Override
    public boolean isEmpty()
        {
        return f_source.isEmpty();
        }

    @Override
    public boolean containsKey(Object key)
        {
        return f_source.containsKey(key);
        }

    @Override
    public boolean containsValue(Object value)
        {
        return f_source.containsValue(value);
        }

    @Override
    public V get(Object key)
        {
        return f_source.get(key);
        }

    @Override
    public V put(K key, V value)
        {
        return f_source.put(key, value);
        }

    @Override
    public V remove(Object key)
        {
        return f_source.remove(key);
        }

    @Override
    public void putAll(Map<? extends K, ? extends V> map)
        {
        f_source.putAll(map);
        }

    @Override
    public void clear()
        {
        f_sortedView.clear();
        f_source.clear();
        }

    @Override
    public Set<K> keySet()
        {
        return f_sortedView.stream().map(Map.Entry::getKey).collect(Collectors.toCollection(LinkedHashSet::new));
        }

    @Override
    public Collection<V> values()
        {
        return f_sortedView.stream().map(Map.Entry::getValue).collect(Collectors.toList());
        }

    @Override
    public Set<Map.Entry<K, V>> entrySet()
        {
        return f_sortedView;
        }

    // ---- MapListener interface ---------------------------------------

    @Override
    public void entryInserted(MapEvent<K, V> evt)
        {
        f_sortedView.add(evt.getNewEntry());
        }

    @Override
    public void entryUpdated(MapEvent<K, V> evt)
        {
        entryDeleted(evt);
        entryInserted(evt);
        }

    @Override
    public void entryDeleted(MapEvent<K, V> evt)
        {
        f_sortedView.remove(evt.getOldEntry());
        }

    // ---- inner class: EntryComparator ------------------------------------

    /**
     * The wrapper comparator that ensures that we never compare entries as
     * "equal", which would result in some entries being removed from the view
     * even though they have different keys and are not really equal.
     */
    private class EntryComparator
            implements Comparator<Map.Entry<? extends K, ? extends V>>
        {
        @SuppressWarnings("ComparatorMethodParameterNotUsed")
        public int compare(Map.Entry<? extends K, ? extends V> e1, Map.Entry<? extends K, ? extends V> e2)
            {
            int nComp = f_comparator.compare(e1.getValue(), e2.getValue());

            // We *know* that the entries are all different, as they have different keys,
            // so we need to treat entries that evaluate as "equal" based on the value
            // comparison as "different" for the purpose of keeping all entries in this
            // view. Otherwise, entries considered "equal" would be removed from the view.
            return nComp == 0 ? 1 : nComp;
            }
        }

    // ---- data members ----------------------------------------------------

    /**
     * The source cache this view is for.
     */
    private final NamedCache<K, V> f_source;

    /**
     * The Comparator used to sort entries.
     */
    private final Comparator<? super V> f_comparator;

    /**
     * Sorted view of the entries in the source cache.
     */
    private final SortedSet<Map.Entry<K, V>> f_sortedView;
    }
