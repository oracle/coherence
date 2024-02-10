/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.collections;

import com.oracle.coherence.common.base.Holder;
import com.oracle.coherence.common.base.SimpleHolder;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.ObservableMap;

import java.util.Map;
import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An {@link ObservableMap} implementation that extends the
 * {@link ConcurrentHashMap}.
 * <p>
 * This map implements the {@link ObservableMap} interface, meaning it provides
 * event notifications to all registered listeners for each insert, update and
 * delete.
 * <p>
 * Just like the {@link ConcurrentHashMap} class it extends, this map does not 
 * support {@code null} keys or values. If you need support for {@code null}
 * keys and values, use {@link ObservableNullableConcurrentMap} instead.
 * 
 * @since 24.03
 * @author Aleks Seovic  2024.02.09
 */
public class ObservableConcurrentMap<K, V>
        extends ConcurrentHashMap<K, V>
        implements ObservableMap<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new, empty map with the default initial table size (16).
     */
    public ObservableConcurrentMap()
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
    public ObservableConcurrentMap(int cInitialCapacity)
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
    public ObservableConcurrentMap(int cInitialCapacity, float flLoadFactor)
        {
        super(cInitialCapacity, flLoadFactor);
        }

    /**
     * Construct a new map with the same mappings as the given map.
     *
     * @param map  the map to construct this map from
     */
    public ObservableConcurrentMap(Map<? extends K, ? extends V> map)
        {
        super(map);
        }

    // ----- Map interface --------------------------------------------------

    public V put(K key, V value)
        {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        Holder<V> result = new SimpleHolder<>();

        super.compute(key, (k, valueOld) ->
            {
            result.set(valueOld);
            dispatchEvent(() -> new MapEvent<>(this, valueOld == null
                                                     ? MapEvent.ENTRY_INSERTED
                                                     : MapEvent.ENTRY_UPDATED, k, valueOld, value));
            return value;
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
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        Holder<Boolean> fInserted = new SimpleHolder<>(false);

        V result = super.computeIfAbsent(key, k ->
            {
            fInserted.set(true);
            dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_INSERTED, key, null, value));
            return value;
            });

        return fInserted.get() ? null : result;
        }

    public V remove(Object oKey)
        {
        Objects.requireNonNull(oKey);

        Holder<V> result = new SimpleHolder<>();

        super.compute((K) oKey, (k, v) ->
            {
            result.set(v);
            if (v != null)
                {
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_DELETED, k, v, null));
                }
            return null;
            });

        return result.get();
        }

    public boolean remove(Object key, Object value)
        {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        Holder<Boolean> result = new SimpleHolder<>(false);

        super.compute((K) key, (k, v) ->
            {
            if (v != null && Objects.equals(v, value))
                {
                result.set(true);
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_DELETED, k, v, null));
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
        Objects.requireNonNull(key);
        Objects.requireNonNull(oldValue);
        Objects.requireNonNull(newValue);

        Holder<Boolean> result = new SimpleHolder<>(false);

        super.compute(key, (k, v) ->
            {
            if (v != null && Objects.equals(v, oldValue))
                {
                result.set(true);
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_UPDATED, k, v, newValue));
                return newValue;
                }
            return v;
            });

        return result.get();
        }

    public V replace(K key, V value)
        {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        Holder<V> result = new SimpleHolder<>();

        super.compute(key, (k, v) ->
            {
            if (v != null)
                {
                result.set(v);
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_UPDATED, k, v, value));
                return value;
                }
            return v;
            });

        return result.get();
        }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
        {
        Objects.requireNonNull(key);
        Objects.requireNonNull(mappingFunction);

        return super.computeIfAbsent(key, k ->
            {
            V value = mappingFunction.apply(k);
            if (value != null)
                {
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_INSERTED, key, null, value));
                return value;
                }
            return null;
            });
        }

    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        Objects.requireNonNull(key);
        Objects.requireNonNull(remappingFunction);

        return super.computeIfPresent(key, (k, v) ->
            {
            V valueNew = remappingFunction.apply(k, v);
            if (valueNew != null)
                {
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_UPDATED, k, v, valueNew));
                return valueNew;
                }
            else
                {
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_DELETED, key, v, null));
                }
            return null;
            });
        }

    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        Objects.requireNonNull(key);
        Objects.requireNonNull(remappingFunction);

        return super.compute(key, (k, valueOld) ->
            {
            V valueNew = remappingFunction.apply(k, valueOld);
            if (valueNew != null)
                {
                dispatchEvent(() -> new MapEvent<>(this, valueOld == null
                                                         ? MapEvent.ENTRY_INSERTED
                                                         : MapEvent.ENTRY_UPDATED, key, valueOld, valueNew));
                return valueNew;
                }
            else if (valueOld != null)
                {
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_DELETED, key, valueOld, null));
                }
            return null;
            });
        }

    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
        {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        Objects.requireNonNull(remappingFunction);

        return super.compute(key, (k, valueOld) ->
            {
            V valueNew = valueOld == null
                         ? value
                         : remappingFunction.apply(valueOld, value);
            if (valueNew != null)
                {
                dispatchEvent(() -> new MapEvent<>(this, valueOld == null
                                                         ? MapEvent.ENTRY_INSERTED
                                                         : MapEvent.ENTRY_UPDATED, key, valueOld, valueNew));
                return valueNew;
                }
            else
                {
                dispatchEvent(() -> new MapEvent<>(this, MapEvent.ENTRY_DELETED, key, valueOld, null));
                }
            return null;
            });
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

    @Override
    public void addMapListener(MapListener<? super K, ? super V> listener)
        {
        addMapListener(listener, (Filter<?>) null, false);
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> listener)
        {
        removeMapListener(listener, (Filter<?>) null);
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

    // ----- ObservableConcurrentMap methods ------------------------

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
