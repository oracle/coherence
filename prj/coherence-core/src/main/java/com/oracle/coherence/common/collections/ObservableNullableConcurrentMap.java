/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.collections;

import com.oracle.coherence.common.base.Holder;
import com.oracle.coherence.common.base.Nullable;
import com.oracle.coherence.common.base.SimpleHolder;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.ObservableMap;

import java.util.Map;
import java.util.Objects;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An {@link ObservableMap} implementation that extends the
 * {@link NullableConcurrentMap}.
 * <p>
 * This map implements the {@link ObservableMap} interface, meaning it provides
 * event notifications to all registered listeners for each insert, update and
 * delete.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.01.12
 */
public class ObservableNullableConcurrentMap<K, V>
        extends NullableConcurrentMap<K, V>
        implements ObservableMap<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new, empty map with the default initial table size (16).
     */
    public ObservableNullableConcurrentMap()
        {
        super();
        }

    /**
     * Construct a new, empty map with an initial table size
     * accommodating the specified number of elements without the need
     * to dynamically resize.
     *
     * @param cInitialCapacity  the initial capacity
     *
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public ObservableNullableConcurrentMap(int cInitialCapacity)
        {
        super(cInitialCapacity);
        }

    /**
     * Construct a new, empty map with an initial table size based on
     * the given number of elements ({@code initialCapacity}) and
     * initial table density ({@code loadFactor}).
     *
     * @param cInitialCapacity  the initial capacity
     * @param flLoadFactor      the load factor (table density)
     *
     * @throws IllegalArgumentException  if the initial capacity is negative
     *                                   or the load factor is non-positive
     */
    public ObservableNullableConcurrentMap(int cInitialCapacity, float flLoadFactor)
        {
        super(cInitialCapacity, flLoadFactor);
        }

    /**
     * Construct a new map with the same mappings as the given map.
     *
     * @param map  the map to construct this map from
     */
    public ObservableNullableConcurrentMap(Map<? extends K, ? extends V> map)
        {
        super(map);
        }

    // ----- Map interface --------------------------------------------------

    public V put(K key, V value)
        {
        Holder<V> result = new SimpleHolder<>();

        getMap().compute(Nullable.of(key), (k, v) ->
            {
            V valueOld = Nullable.get(v);
            result.set(valueOld);
            dispatchEvent(() -> new MapEvent<>(this, v == null
                                                     ? MapEvent.ENTRY_INSERTED
                                                     : MapEvent.ENTRY_UPDATED, k.get(), valueOld, value));
            return Nullable.of(value);
            });

        return result.get();
        }

    public void putAll(Map<? extends K, ? extends V> map)
        {
        if (f_listenerSupport == null)
            {
            // called from parent constructor; events will not be dispatched
            super.putAll(map);
            }
        else
            {
            for (Entry<? extends K, ? extends V> e : map.entrySet())
                {
                put(e.getKey(), e.getValue());
                }
            }
        }

    public V putIfAbsent(K key, V value)
        {
        Holder<Boolean> fInserted = new SimpleHolder<>(false);

        Nullable<V> result = getMap().computeIfAbsent(Nullable.of(key), k ->
            {
            fInserted.set(true);
            dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_INSERTED, key, null, value));
            return Nullable.of(value);
            });

        return fInserted.get() ? null : result.get();
        }

    public V remove(Object oKey)
        {
        Holder<V> result = new SimpleHolder<>();

        getMap().compute((Nullable<K>) Nullable.of(oKey), (k, v) ->
            {
            result.set(Nullable.get(v));
            if (v != null)
                {
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_DELETED, k.get(), v.get(), null));
                }
            return null;
            });

        return result.get();
        }

    public boolean remove(Object key, Object value)
        {
        Holder<Boolean> result = new SimpleHolder<>(false);

        getMap().compute((Nullable<K>) Nullable.of(key), (k, v) ->
            {
            if (v != null && Objects.equals(v.get(), value))
                {
                result.set(true);
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_DELETED, k.get(), v.get(), null));
                return null;
                }
            return v;
            });

        return result.get();
        }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)
        {
        Objects.requireNonNull(function);

        for (K key : keySet())
            {
            compute(key, function);
            }
        }

    public boolean replace(K key, V oldValue, V newValue)
        {
        Holder<Boolean> result = new SimpleHolder<>(false);

        getMap().compute(Nullable.of(key), (k, v) ->
            {
            if (v != null && Objects.equals(v.get(), oldValue))
                {
                result.set(true);
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_UPDATED, k.get(), v.get(), newValue));
                return Nullable.of(newValue);
                }
            return v;
            });

        return result.get();
        }

    public V replace(K key, V value)
        {
        Holder<V> result = new SimpleHolder<>();

        getMap().compute(Nullable.of(key), (k, v) ->
            {
            if (v != null)
                {
                result.set(v.get());
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_UPDATED, k.get(), v.get(), value));
                return Nullable.of(value);
                }
            return v;
            });

        return result.get();
        }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
        {
        Objects.requireNonNull(mappingFunction);

        return Nullable.get(
                getMap().computeIfAbsent(Nullable.of(key), k ->
                    {
                    V value = mappingFunction.apply(k.get());
                    if (value != null)
                        {
                        dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_INSERTED, key, null, value));
                        return Nullable.of(value);
                        }
                    return null;
                    }
                ));
        }

    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        Objects.requireNonNull(remappingFunction);

        return Nullable.get(getMap().computeIfPresent(Nullable.of(key), (k, v) ->
            {
            V valueNew = remappingFunction.apply(k.get(), v.get());
            if (valueNew != null)
                {
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_UPDATED, k.get(), v.get(), valueNew));
                return Nullable.of(valueNew);
                }
            else
                {
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_DELETED, key, v.get(), null));
                }
            return null;
            }));
        }

    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        Objects.requireNonNull(remappingFunction);

        return Nullable.get(getMap().compute(Nullable.of(key), (k, v) ->
            {
            V valueOld = Nullable.get(v);
            V valueNew = remappingFunction.apply(k.get(), valueOld);
            if (valueNew != null)
                {
                dispatchEvent(() -> new MapEvent<>(this, valueOld == null
                                                         ? MapEvent.ENTRY_INSERTED
                                                         : MapEvent.ENTRY_UPDATED, key, valueOld, valueNew));
                return Nullable.of(valueNew);
                }
            else if (valueOld != null)
                {
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_DELETED, key, valueOld, null));
                }
            return null;
            }));
        }

    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
        {
        Objects.requireNonNull(value);
        Objects.requireNonNull(remappingFunction);

        return Nullable.get(getMap().compute(Nullable.of(key), (k, v) ->
            {
            V valueOld = Nullable.get(v);
            V valueNew = valueOld == null
                         ? value
                         : remappingFunction.apply(valueOld, value);
            if (valueNew != null)
                {
                dispatchEvent(() -> new MapEvent<>(this, valueOld == null
                                                         ? MapEvent.ENTRY_INSERTED
                                                         : MapEvent.ENTRY_UPDATED, key, valueOld, valueNew));
                return Nullable.of(valueNew);
                }
            else
                {
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_DELETED, key, valueOld, null));
                }
            return null;
            }));
        }

    /**
     * Remove everything from the cache, notifying any registered listeners.
     */
    public void clear()
        {
        for (K key : keySet())
            {
            remove(key);
            }
        }

    // ----- ObservableMap methods ------------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public void addMapListener(MapListener listener)
        {
        addMapListener(listener, (Filter) null, false);
        }

    @SuppressWarnings("unchecked")
    @Override
    public void removeMapListener(MapListener listener)
        {
        removeMapListener(listener, (Filter) null);
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> listener, K key, boolean fLite)
        {
        Base.azzert(listener != null);

        f_listenerSupport.addListener(listener, key, fLite);
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> listener, K key)
        {
        Base.azzert(listener != null);

        f_listenerSupport.removeListener(listener, key);
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> listener, Filter filter, boolean fLite)
        {
        Base.azzert(listener != null);

        f_listenerSupport.addListener(listener, filter, fLite);
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> listener, Filter filter)
        {
        Base.azzert(listener != null);

        f_listenerSupport.removeListener(listener, filter);
        }

    // ----- ObservableNullableConcurrentMap methods ------------------------

    /**
     * Removes all mappings from this map.
     * <p>
     * Note: the removal of entries caused by this truncate operation will not
     * be observable.
     */
    public void truncate()
        {
        super.clear();
        }

    // ----- event dispatching ----------------------------------------------

    /**
     * Accessor for the MapListenerSupport for sub-classes.
     *
     * @return the MapListenerSupport, or null if there are no listeners
     */
    protected MapListenerSupport getMapListenerSupport()
        {
        return f_listenerSupport;
        }

    /**
     * Dispatch the passed event.
     *
     * @param evt a CacheEvent object
     */
    protected void dispatchEvent(Supplier<MapEvent<K, V>> evt)
        {
        if (!f_listenerSupport.isEmpty())
            {
            f_listenerSupport.fireEvent(evt.get(), false);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The MapListenerSupport object.
     */
    protected transient final MapListenerSupport f_listenerSupport = new MapListenerSupport();
    }
